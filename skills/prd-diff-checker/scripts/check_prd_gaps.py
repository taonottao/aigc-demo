#!/usr/bin/env python3
import argparse
import pathlib
import re
import subprocess
from dataclasses import dataclass
from typing import List, Optional, Sequence, Set, Tuple


@dataclass(frozen=True)
class ReqItem:
    section: str
    text: str
    line_no: int


@dataclass(frozen=True)
class Evidence:
    kind: str
    value: str


@dataclass(frozen=True)
class ImplSignals:
    backend_endpoints: Set[str]
    fe_api_paths: Set[str]
    schema_text: str
    user_entity_text: str
    backend_text: str
    frontend_text: str


def run(cmd: Sequence[str], cwd: pathlib.Path) -> str:
    p = subprocess.run(list(cmd), cwd=str(cwd), capture_output=True, text=True, check=False)
    return p.stdout if p.returncode == 0 else ""


def read_text(p: pathlib.Path) -> str:
    if not p.exists() or not p.is_file():
        return ""
    return p.read_text(encoding="utf-8", errors="ignore")


def should_scan_file(p: pathlib.Path) -> bool:
    s = str(p)
    if any(part in s for part in ["/node_modules/", "/dist/", "/target/", "/.git/", "/.idea/"]):
        return False
    return p.suffix in {".java", ".js", ".ts", ".vue"}


def collect_scope_files(root: pathlib.Path, diff_range: str) -> Optional[Set[pathlib.Path]]:
    if not diff_range:
        return None
    out = run(["git", "diff", "--name-only", diff_range], root)
    files: Set[pathlib.Path] = set()
    for ln in out.splitlines():
        p = (root / ln.strip()).resolve()
        if p.exists() and p.is_file():
            files.add(p)
    return files


def parse_prd(prd_path: pathlib.Path) -> List[ReqItem]:
    lines = prd_path.read_text(encoding="utf-8").splitlines()
    items: List[ReqItem] = []
    cur_section = ""

    def add(text: str, line_no: int) -> None:
        t = text.strip()
        if not t:
            return
        items.append(ReqItem(section=cur_section or "(root)", text=t, line_no=line_no))

    for i, line in enumerate(lines, start=1):
        m = re.match(r"^(#{2,4})\s+(.*)$", line)
        if m:
            level = len(m.group(1))
            title = m.group(2).strip()
            if level <= 3:
                cur_section = title
            continue

        if re.match(r"^\s*[-*]\s+", line):
            add(re.sub(r"^\s*[-*]\s+", "", line), i)
            continue

    # de-dupe by (section,text)
    seen: Set[Tuple[str, str]] = set()
    out: List[ReqItem] = []
    for it in items:
        k = (it.section, it.text)
        if k in seen:
            continue
        seen.add(k)
        out.append(it)
    return out


def scan_backend_endpoints(root: pathlib.Path, scope: Optional[Set[pathlib.Path]]) -> Set[str]:
    base = root / "user-management-backend" / "src" / "main" / "java"
    if not base.exists():
        return set()

    endpoints: Set[str] = set()
    req_map_re = re.compile(r'@RequestMapping\(\s*"([^"]+)"\s*\)')
    map_re = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\(\s*"([^"]*)"\s*\))?')

    for p in base.rglob("*.java"):
        if scope is not None and p.resolve() not in scope:
            continue
        if "Controller" not in p.name:
            continue
        text = read_text(p)
        prefix = ""
        m = req_map_re.search(text)
        if m:
            prefix = m.group(1)
        for mm in map_re.finditer(text):
            method = mm.group(1).upper()
            sub = mm.group(2) or ""
            path = f"{prefix}{sub}"
            if not path.startswith("/"):
                path = "/" + path
            endpoints.add(f"{method} {path}")
    return endpoints


def scan_frontend_api_paths(root: pathlib.Path, scope: Optional[Set[pathlib.Path]]) -> Set[str]:
    src = root / "user-management-font" / "src"
    if not src.exists():
        return set()
    api_paths: Set[str] = set()
    http_re = re.compile(r"/api/[a-zA-Z0-9_\-/{}}$`]+")
    for p in src.rglob("*"):
        if not p.is_file():
            continue
        if scope is not None and p.resolve() not in scope:
            continue
        if not should_scan_file(p):
            continue
        text = read_text(p)
        for m in http_re.finditer(text):
            api_paths.add(m.group(0).replace("`", ""))
    return api_paths


def build_impl_signals(root: pathlib.Path, scope: Optional[Set[pathlib.Path]]) -> ImplSignals:
    backend_eps = scan_backend_endpoints(root, scope)
    fe_api_paths = scan_frontend_api_paths(root, scope)

    schema_text = read_text(root / "user-management-backend" / "src" / "main" / "resources" / "schema.sql")
    user_entity_text = read_text(
        root
        / "user-management-backend"
        / "src"
        / "main"
        / "java"
        / "com"
        / "smile"
        / "usermanagement"
        / "entity"
        / "User.java"
    )

    backend_text = ""
    backend_dir = root / "user-management-backend" / "src" / "main"
    if backend_dir.exists():
        for p in backend_dir.rglob("*"):
            if p.is_file() and should_scan_file(p) and (scope is None or p.resolve() in scope):
                backend_text += "\n" + read_text(p)

    frontend_text = ""
    fe_dir = root / "user-management-font" / "src"
    if fe_dir.exists():
        for p in fe_dir.rglob("*"):
            if p.is_file() and should_scan_file(p) and (scope is None or p.resolve() in scope):
                frontend_text += "\n" + read_text(p)

    return ImplSignals(
        backend_endpoints=backend_eps,
        fe_api_paths=fe_api_paths,
        schema_text=schema_text,
        user_entity_text=user_entity_text,
        backend_text=backend_text,
        frontend_text=frontend_text,
    )


def classify_module(section: str) -> str:
    s = section.strip()
    if s.startswith("1.") or "用户管理" in s:
        return "user"
    if s.startswith("2.") or "组织管理" in s:
        return "org"
    if s.startswith("3.") or "角色管理" in s:
        return "role"
    if s.startswith("4.") or "权限管理" in s:
        return "perm"
    if s.startswith("5.") or "系统支撑" in s:
        return "system"
    return "other"


def has_any(text: str, needles: Sequence[str]) -> bool:
    t = text.lower()
    return any(n.lower() in t for n in needles)


def requirement_status(req: ReqItem, sig: ImplSignals) -> Tuple[str, List[Evidence]]:
    text = req.text.strip()
    mod = classify_module(req.section)
    ev: List[Evidence] = []

    def add(kind: str, value: str) -> None:
        if value:
            ev.append(Evidence(kind=kind, value=value))

    def add_endpoint_evidence(path_contains: str) -> None:
        for ep in sorted(sig.backend_endpoints):
            if path_contains in ep:
                add("backend", ep)
        for p in sorted(sig.fe_api_paths):
            if path_contains in p:
                add("frontend", p)

    if mod == "user":
        add_endpoint_evidence("/api/users")
        if "增删改查" in text:
            need = {
                "GET /api/users",
                "GET /api/users/{id}",
                "POST /api/users",
                "PUT /api/users/{id}",
                "DELETE /api/users/{id}",
            }
            return ("FOUND" if need.issubset(sig.backend_endpoints) else ("PARTIAL" if ev else "MISSING")), ev[:6]

        if any(k in text for k in ["启用", "禁用"]):
            ok = has_any(sig.user_entity_text, ["status"]) and has_any(sig.schema_text, ["status"]) and (
                "PUT /api/users/{id}" in sig.backend_endpoints
            )
            return ("FOUND" if ok else ("PARTIAL" if ev else "MISSING")), (ev + [Evidence("schema", "sys_user.status")] if ok else ev)[:6]

        if "批量" in text or "导入" in text or "导出" in text:
            return ("PARTIAL" if has_any(sig.backend_text + sig.frontend_text, ["import", "export"]) else "MISSING"), []

        if "加密密码" in text or "BCrypt" in text:
            return ("FOUND" if has_any(sig.backend_text, ["BCrypt", "PasswordEncoder"]) else "MISSING"), (
                [Evidence("backend", "BCrypt/PasswordEncoder usage")] if has_any(sig.backend_text, ["BCrypt", "PasswordEncoder"]) else []
            )

        field_map = {
            "姓名": ["realName", "real_name"],
            "手机号": ["phone"],
            "邮箱": ["email"],
            "头像": ["avatar"],
            "密码": ["password"],
            "用户名": ["username"],
        }
        for cn, needles in field_map.items():
            if cn in text:
                ok = has_any(sig.user_entity_text, needles) and has_any(sig.schema_text, needles)
                return ("FOUND" if ok else "MISSING"), ev[:6]

        if "组织" in text:
            ok = has_any(sig.user_entity_text, ["orgId", "org_id"]) and has_any(sig.schema_text, ["org_id"])
            return ("FOUND" if ok else ("PARTIAL" if has_any(sig.schema_text, ["org_id"]) else "MISSING")), ev[:6]

        if "角色" in text or "个人中心" in text or "重置" in text:
            return "MISSING", []

        return ("PARTIAL" if ev else "MISSING"), ev[:6]

    if mod == "org":
        add_endpoint_evidence("/api/org")
        add_endpoint_evidence("/api/organizations")
        if ev:
            return "FOUND", ev[:6]
        if has_any(sig.schema_text, ["create table", "sys_org"]):
            return "PARTIAL", [Evidence("schema", "sys_org table exists; API/UI missing")]
        return "MISSING", []

    if mod in {"role", "perm"}:
        add_endpoint_evidence("/api/roles")
        add_endpoint_evidence("/api/permissions")
        add_endpoint_evidence("/api/menus")
        if ev:
            return "PARTIAL", ev[:6]
        return "MISSING", []

    if mod == "system":
        if "登录" in text or "JWT" in text or "验证码" in text:
            has_fe_login = has_any(sig.frontend_text, ["LoginView", "/login"])
            has_be_auth = has_any(sig.backend_text, ["jwt", "spring-security", "PreAuthorize"]) or any(
                any(x in ep for x in ["/api/login", "/api/auth", "/login"]) for ep in sig.backend_endpoints
            )
            if has_fe_login and not has_be_auth:
                return "PARTIAL", [Evidence("frontend", "LoginView exists; backend auth missing")]
            if has_be_auth:
                return "PARTIAL", [Evidence("backend", "auth/security hints found")]
            return "MISSING", []

        if "PreAuthorize" in text or "权限校验" in text:
            return ("PARTIAL" if has_any(sig.backend_text, ["PreAuthorize", "EnableMethodSecurity"]) else "MISSING"), []

        if "日志" in text:
            return ("PARTIAL" if has_any(sig.backend_text, ["log", "audit"]) else "MISSING"), []

        if "BCrypt" in text:
            return ("FOUND" if has_any(sig.backend_text, ["BCrypt", "PasswordEncoder"]) else "MISSING"), []

        return "MISSING", []

    m = re.search(r"/api/[a-zA-Z0-9_\-/{}}]+", text)
    if m:
        add_endpoint_evidence(m.group(0))
        return ("FOUND" if ev else "MISSING"), ev[:6]

    return "MISSING", []


def to_md(prd_path: pathlib.Path, items: List[Tuple[ReqItem, str, List[Evidence]]]) -> str:
    total = len(items)
    found = sum(1 for _, s, _ in items if s == "FOUND")
    partial = sum(1 for _, s, _ in items if s == "PARTIAL")
    missing = sum(1 for _, s, _ in items if s == "MISSING")

    def ev_str(evs: List[Evidence]) -> str:
        return "; ".join([f"{e.kind}:{e.value}" for e in evs]) if evs else ""

    lines: List[str] = []
    lines.append("# 需求差异报告")
    lines.append("")
    lines.append(f"- PRD: {prd_path}")
    lines.append(f"- Total: {total}")
    lines.append(f"- FOUND: {found}")
    lines.append(f"- PARTIAL: {partial}")
    lines.append(f"- MISSING: {missing}")
    lines.append("")
    lines.append("| PRD模块 | 需求点 | 状态 | 证据 | PRD行号 |")
    lines.append("|---|---|---|---|---|")
    for req, status, evs in items:
        lines.append(f"| {req.section} | {req.text} | {status} | {ev_str(evs)} | {req.line_no} |")
    lines.append("")
    lines.append("## Notes")
    lines.append("- Evidence-based and heuristic; treat PARTIAL/MISSING as a triage queue for deeper review.")
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    ap = argparse.ArgumentParser(description="Check gaps between PRD and implementation")
    ap.add_argument("--root", required=True)
    ap.add_argument("--prd", required=True)
    ap.add_argument("--output", required=True)
    ap.add_argument("--diff", default="")
    ap.add_argument("--module", default="", help="filter by PRD section keyword")
    args = ap.parse_args()

    root = pathlib.Path(args.root).resolve()
    prd = pathlib.Path(args.prd).resolve()
    output = pathlib.Path(args.output).resolve()
    output.parent.mkdir(parents=True, exist_ok=True)

    scope = collect_scope_files(root, args.diff)
    reqs = parse_prd(prd)
    if args.module:
        reqs = [r for r in reqs if args.module in r.section or args.module in r.text]

    sig = build_impl_signals(root, scope)
    results: List[Tuple[ReqItem, str, List[Evidence]]] = []
    for r in reqs:
        status, evs = requirement_status(r, sig)
        results.append((r, status, evs))

    output.write_text(to_md(prd, results), encoding="utf-8")
    print(f"output={output}")
    print(f"total={len(results)}")
    print(f"found={sum(1 for _, s, _ in results if s == 'FOUND')}")
    print(f"partial={sum(1 for _, s, _ in results if s == 'PARTIAL')}")
    print(f"missing={sum(1 for _, s, _ in results if s == 'MISSING')}")


if __name__ == "__main__":
    main()
