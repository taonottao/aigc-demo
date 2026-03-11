#!/usr/bin/env python3
import argparse
import pathlib
import re
import subprocess
from dataclasses import dataclass
from typing import Dict, List, Set, Tuple


@dataclass(frozen=True)
class TestPoint:
    module: str
    desc: str
    precondition: str
    expected: str
    test_type: str
    status: str
    test_time: str
    tester: str
    owner: str
    note: str


TARGET_HEADERS = [
    "功能模块",
    "测试点描述",
    "前置条件",
    "预期结果",
    "测试类型",
    "测试状态",
    "测试时间",
    "测试人",
    "负责人",
    "备注",
]


def run(cmd: List[str], cwd: pathlib.Path) -> str:
    p = subprocess.run(cmd, cwd=str(cwd), capture_output=True, text=True, check=False)
    return p.stdout if p.returncode == 0 else ""


def split_row(line: str) -> List[str]:
    return [c.strip() for c in line.strip().strip("|").split("|")]


def parse_existing_rows(output_path: pathlib.Path) -> Tuple[List[TestPoint], Set[Tuple[str, str]]]:
    if not output_path.exists():
        return [], set()

    lines = output_path.read_text(encoding="utf-8").splitlines()
    raw_headers: List[str] = []
    for i in range(len(lines) - 1):
        if lines[i].startswith("|") and "测试状态" in lines[i] and lines[i + 1].startswith("|"):
            raw_headers = split_row(lines[i])
            break

    if not raw_headers:
        return [], set()

    rows: List[TestPoint] = []
    keys: Set[Tuple[str, str]] = set()
    for line in lines:
        if not line.startswith("|") or "测试点描述" in line or line.startswith("|---"):
            continue
        cols = split_row(line)
        if len(cols) < 2:
            continue
        row_map: Dict[str, str] = {}
        for idx, h in enumerate(raw_headers):
            row_map[h] = cols[idx] if idx < len(cols) else ""
        for h in TARGET_HEADERS:
            row_map.setdefault(h, "")
        tp = TestPoint(
            module=row_map["功能模块"],
            desc=row_map["测试点描述"],
            precondition=row_map["前置条件"],
            expected=row_map["预期结果"],
            test_type=row_map["测试类型"],
            status=row_map["测试状态"] or "🔴 待测试",
            test_time=row_map["测试时间"],
            tester=row_map["测试人"],
            owner=row_map["负责人"],
            note=row_map["备注"],
        )
        if not tp.module or not tp.desc:
            continue
        rows.append(tp)
        keys.add((tp.module, tp.desc))
    return rows, keys


def is_frontend_file(path: pathlib.Path) -> bool:
    s = str(path)
    return "user-management-font/src/" in s and s.endswith((".js", ".ts", ".vue", ".jsx", ".tsx"))


def is_backend_file(path: pathlib.Path) -> bool:
    s = str(path)
    return "user-management-backend/src/main/java/" in s and s.endswith(".java")


def collect_scope_files(root: pathlib.Path, diff_range: str, module_filter: str) -> List[pathlib.Path]:
    files: List[pathlib.Path] = []
    if diff_range:
        out = run(["git", "diff", "--name-only", diff_range], root)
        for ln in out.splitlines():
            p = (root / ln.strip()).resolve()
            if p.exists() and p.is_file():
                files.append(p)
    else:
        for p in root.rglob("*"):
            if p.is_file() and (is_frontend_file(p) or is_backend_file(p)):
                files.append(p)

    if module_filter:
        files = [f for f in files if module_filter in str(f)]
    return files


def normalize_path_template(expr: str) -> str:
    def repl(m: re.Match) -> str:
        name = m.group(1).strip().lower()
        if name in {"suffix", "query", "search", "params"}:
            return ""
        if name in {"id", "userid", "orgid", "roleid"}:
            return "/{id}"
        return "/{param}"

    path = re.sub(r"\$\{([^}]+)\}", repl, expr)
    return path.replace("//", "/")


def extract_backend_points(files: List[pathlib.Path], owner: str, root: pathlib.Path) -> List[TestPoint]:
    points: List[TestPoint] = []
    mapping_re = re.compile(r"@(Get|Post|Put|Delete|Patch)Mapping(?:\(\s*\"([^\"]*)\"\s*\))?")
    prefix_re = re.compile(r"@RequestMapping\(\s*\"([^\"]*)\"\s*\)")
    ex_re = re.compile(r"throw new IllegalArgumentException\(\"([^\"]+)\"\)")

    for f in files:
        if not is_backend_file(f):
            continue
        text = f.read_text(encoding="utf-8", errors="ignore")

        if "Controller" in f.name:
            prefix = ""
            m_prefix = prefix_re.search(text)
            if m_prefix:
                prefix = m_prefix.group(1)
            for m in mapping_re.finditer(text):
                method = m.group(1).upper()
                sub = m.group(2) or ""
                path = f"{prefix}{sub}"
                if not path.startswith("/"):
                    path = "/" + path
                points.append(TestPoint(
                    module="后端接口",
                    desc=f"{method} {path} 接口可用性与返回结构校验",
                    precondition="后端服务已启动，数据库可连接",
                    expected="返回状态码符合约定，响应字段与业务语义正确",
                    test_type="后端自测",
                    status="🔴 待测试",
                    test_time="",
                    tester="",
                    owner=owner,
                    note=f"source={f.relative_to(root)}",
                ))

        for m in ex_re.finditer(text):
            msg = m.group(1)
            points.append(TestPoint(
                module="后端业务规则",
                desc=f"业务异常提示校验：{msg}",
                precondition="构造触发异常的输入条件",
                expected="返回明确错误信息且数据不被错误写入",
                test_type="后端自测",
                status="🔴 待测试",
                test_time="",
                tester="",
                owner=owner,
                note=f"source={f.relative_to(root)}",
            ))

    return points


def extract_frontend_points(files: List[pathlib.Path], owner: str, root: pathlib.Path) -> List[TestPoint]:
    points: List[TestPoint] = []
    direct_re = re.compile(r"\b(get|post|put|delete)\s*\(\s*['\"]([^'\"]+)['\"]")
    wrapped_re = re.compile(r"return\s+http\(\s*([`'\"])(.+?)\1\s*(?:,\s*\{(.*?)\})?\s*\)", re.DOTALL)
    method_re = re.compile(r"method\s*:\s*['\"](GET|POST|PUT|DELETE|PATCH)['\"]", re.IGNORECASE)

    for f in files:
        if not is_frontend_file(f):
            continue
        text = f.read_text(encoding="utf-8", errors="ignore")

        for m in direct_re.finditer(text):
            method = m.group(1).upper()
            path = m.group(2)
            points.append(TestPoint(
                module="前端交互",
                desc=f"前端调用 {method} {path} 的交互流程验证",
                precondition="前端页面可访问且网络请求可用",
                expected="请求参数正确，请求成功/失败都能正确反馈到页面",
                test_type="前端自测",
                status="🔴 待测试",
                test_time="",
                tester="",
                owner=owner,
                note=f"source={f.relative_to(root)}",
            ))

        for m in wrapped_re.finditer(text):
            path = normalize_path_template(m.group(2))
            opt = m.group(3) or ""
            mm = method_re.search(opt)
            method = mm.group(1).upper() if mm else "GET"
            points.append(TestPoint(
                module="前端交互",
                desc=f"前端调用 {method} {path} 的交互流程验证",
                precondition="前端页面可访问且网络请求可用",
                expected="请求参数正确，请求成功/失败都能正确反馈到页面",
                test_type="前端自测",
                status="🔴 待测试",
                test_time="",
                tester="",
                owner=owner,
                note=f"source={f.relative_to(root)}",
            ))

    return points


def build_integration_points(points: List[TestPoint], owner: str) -> List[TestPoint]:
    backend: Set[str] = set()
    frontend: Set[str] = set()
    be_re = re.compile(r"^(GET|POST|PUT|DELETE|PATCH)\s+([^\s]+)")
    fe_re = re.compile(r"前端调用\s+(GET|POST|PUT|DELETE|PATCH)\s+([^\s]+)")

    for p in points:
        m = be_re.search(p.desc)
        if m:
            backend.add(f"{m.group(1)} {m.group(2)}")
        m = fe_re.search(p.desc)
        if m:
            frontend.add(f"{m.group(1)} {m.group(2)}")

    out: List[TestPoint] = []
    for item in sorted(backend & frontend):
        out.append(TestPoint(
            module="前后端联调",
            desc=f"联调验证：{item}",
            precondition="前后端服务均可用，联调环境网络通畅",
            expected="前端请求与后端响应契约一致，页面结果正确",
            test_type="联调",
            status="🔴 待测试",
            test_time="",
            tester="",
            owner=owner,
            note="auto-overlap",
        ))
    return out


def normalize(points: List[TestPoint]) -> List[TestPoint]:
    out: List[TestPoint] = []
    seen: Set[Tuple[str, str]] = set()
    for p in points:
        k = (p.module, p.desc)
        if k in seen:
            continue
        seen.add(k)
        out.append(p)
    return out


def to_markdown(rows: List[TestPoint]) -> str:
    lines = [
        "# 功能测试清单",
        "",
        "| 功能模块 | 测试点描述 | 前置条件 | 预期结果 | 测试类型 | 测试状态 | 测试时间 | 测试人 | 负责人 | 备注 |",
        "|---|---|---|---|---|---|---|---|---|---|",
    ]
    for r in rows:
        lines.append("| " + " | ".join([
            r.module,
            r.desc,
            r.precondition,
            r.expected,
            r.test_type,
            r.status,
            r.test_time,
            r.tester,
            r.owner,
            r.note,
        ]) + " |")
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    ap = argparse.ArgumentParser(description="Generate/Update 功能测试清单")
    ap.add_argument("--root", required=True)
    ap.add_argument("--output", required=True)
    ap.add_argument("--diff", default="")
    ap.add_argument("--module", default="")
    ap.add_argument("--owner", default="待分配")
    args = ap.parse_args()

    root = pathlib.Path(args.root).resolve()
    output = pathlib.Path(args.output).resolve()
    output.parent.mkdir(parents=True, exist_ok=True)

    files = collect_scope_files(root, args.diff, args.module)
    points: List[TestPoint] = []
    points.extend(extract_backend_points(files, args.owner, root))
    points.extend(extract_frontend_points(files, args.owner, root))
    points.extend(build_integration_points(points, args.owner))
    points = normalize(points)

    existing_rows, existing_keys = parse_existing_rows(output)
    new_rows: List[TestPoint] = []
    for p in points:
        k = (p.module, p.desc)
        if k in existing_keys:
            continue
        new_rows.append(p)

    merged = existing_rows + new_rows
    output.write_text(to_markdown(merged), encoding="utf-8")

    print(f"output={output}")
    print(f"scanned_files={len(files)}")
    print(f"recognized_points={len(points)}")
    print(f"new_points={len(new_rows)}")


if __name__ == "__main__":
    main()
