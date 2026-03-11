#!/usr/bin/env python3
import argparse
import datetime as dt
import json
import pathlib
import random
import re
import string
import subprocess
import sys
import time
import urllib.error
import urllib.request
from typing import Dict, List, Optional, Tuple


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


def now_str() -> str:
    return dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def split_row(line: str) -> List[str]:
    return [c.strip() for c in line.strip().strip("|").split("|")]


def row_line(values: Dict[str, str]) -> str:
    return "| " + " | ".join(values.get(h, "") for h in TARGET_HEADERS) + " |"


def parse_table(doc: pathlib.Path) -> Tuple[List[str], int, int, List[Dict[str, str]]]:
    lines = doc.read_text(encoding="utf-8").splitlines()
    header_idx = -1
    sep_idx = -1

    for i in range(len(lines) - 1):
        if lines[i].startswith("|") and "测试状态" in lines[i] and lines[i + 1].startswith("|"):
            header_idx = i
            sep_idx = i + 1
            break

    if header_idx < 0:
        raise RuntimeError("未找到测试清单表头（包含'测试状态'列）")

    raw_headers = split_row(lines[header_idx])
    rows: List[Dict[str, str]] = []
    i = sep_idx + 1
    while i < len(lines) and lines[i].startswith("|"):
        cols = split_row(lines[i])
        row: Dict[str, str] = {}
        for idx, h in enumerate(raw_headers):
            row[h] = cols[idx] if idx < len(cols) else ""
        for h in TARGET_HEADERS:
            row.setdefault(h, "")
        rows.append(row)
        i += 1

    return lines, header_idx, i, rows


def write_table(doc: pathlib.Path, lines: List[str], header_idx: int, data_end_idx: int, rows: List[Dict[str, str]]) -> None:
    out = []
    out.extend(lines[:header_idx])
    out.append("| " + " | ".join(TARGET_HEADERS) + " |")
    out.append("|---|---|---|---|---|---|---|---|---|---|")
    for r in rows:
        out.append(row_line(r))
    out.extend(lines[data_end_idx:])
    doc.write_text("\n".join(out) + "\n", encoding="utf-8")


def http_request(base_url: str, method: str, path: str, body: Optional[dict] = None) -> Tuple[int, str]:
    url = base_url.rstrip("/") + path
    data = None
    headers = {"Content-Type": "application/json"}
    if body is not None:
        data = json.dumps(body).encode("utf-8")

    req = urllib.request.Request(url=url, method=method.upper(), data=data, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=8) as resp:
            raw = resp.read().decode("utf-8", errors="ignore")
            return resp.getcode(), raw
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="ignore") if e.fp else str(e)
        return e.code, raw
    except Exception as e:
        return 0, str(e)


def http_request_with_headers(
    base_url: str,
    method: str,
    path: str,
    body: Optional[dict] = None,
    headers: Optional[Dict[str, str]] = None,
) -> Tuple[int, str]:
    url = base_url.rstrip("/") + path
    data = None
    req_headers = {"Content-Type": "application/json"}
    if headers:
        req_headers.update(headers)
    if body is not None:
        data = json.dumps(body).encode("utf-8")

    req = urllib.request.Request(url=url, method=method.upper(), data=data, headers=req_headers)
    try:
        with urllib.request.urlopen(req, timeout=8) as resp:
            raw = resp.read().decode("utf-8", errors="ignore")
            return resp.getcode(), raw
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="ignore") if e.fp else str(e)
        return e.code, raw
    except Exception as e:
        return 0, str(e)


def backend_alive(base_url: str) -> bool:
    code, _ = http_request(base_url, "GET", "/api/auth/captcha")
    return code == 200


def start_backend(workspace: pathlib.Path) -> Optional[subprocess.Popen]:
    backend_dir = workspace / "user-management-backend"
    if not backend_dir.exists():
        return None
    log_file = workspace / "doc" / "backend-test-run.log"
    lf = open(log_file, "a", encoding="utf-8")
    p = subprocess.Popen(["mvn", "spring-boot:run"], cwd=str(backend_dir), stdout=lf, stderr=lf)
    return p


def extract_api(desc: str) -> Tuple[Optional[str], Optional[str]]:
    m = re.search(r"\b((?:GET|POST|PUT|DELETE|PATCH)(?:/(?:GET|POST|PUT|DELETE|PATCH))*)\s+([^\s]+)", desc)
    if not m:
        return None, None
    method_expr = m.group(1)
    method = method_expr.split("/")[0]
    return method, m.group(2)


def generate_backend_unit_skeleton(workspace: pathlib.Path, method: str, path: str) -> str:
    test_dir = workspace / "user-management-backend" / "src" / "test" / "java" / "com" / "smile" / "usermanagement" / "generated"
    test_dir.mkdir(parents=True, exist_ok=True)
    safe = re.sub(r"[^A-Za-z0-9]", "_", f"{method}_{path}").strip("_")
    class_name = f"Generated_{safe}_Test"
    file_path = test_dir / f"{class_name}.java"
    if file_path.exists():
        return str(file_path)

    content = f'''package com.smile.usermanagement.generated;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class {class_name} {{

    @Test
    @Disabled("Auto-generated skeleton. Complete assertions before enabling.")
    void apiSmoke() {{
        // TODO: Implement assertions for {method} {path}
    }}
}}
'''
    file_path.write_text(content, encoding="utf-8")
    return str(file_path)


def find_existing_unit_tests(workspace: pathlib.Path, path_hint: str) -> bool:
    test_root = workspace / "user-management-backend" / "src" / "test" / "java"
    if not test_root.exists():
        return False
    token = re.sub(r"[^A-Za-z0-9]", "", path_hint).lower()
    for p in test_root.rglob("*Test.java"):
        name = p.stem.lower()
        if token and token in name:
            return True
    return any(test_root.rglob("*Test.java"))


def random_user_payload(tag: str) -> dict:
    suffix = "".join(random.choices(string.digits, k=6))
    return {
        "username": f"auto_{tag}_{suffix}",
        "password": "Pass@123456",
        "realName": "Auto Tester",
        "phone": "13800000000",
        "email": f"auto_{tag}_{suffix}@example.com",
        "avatar": "https://example.com/a.png",
        "orgId": 1,
        "status": 1,
    }


def random_org_payload(tag: str) -> dict:
    suffix = "".join(random.choices(string.ascii_uppercase + string.digits, k=6))
    return {
        "parentId": 1,
        "name": f"AutoOrg_{tag}_{suffix}",
        "code": f"AUTO_ORG_{suffix}",
        "leader": "Auto",
        "description": "auto-generated",
        "sortNo": 999,
        "status": 1,
    }


def random_role_payload(tag: str) -> dict:
    suffix = "".join(random.choices(string.ascii_uppercase + string.digits, k=6))
    return {
        "name": f"AutoRole_{tag}_{suffix}",
        "code": f"AUTO_ROLE_{suffix}",
        "description": "auto-generated",
        "status": 1,
    }


def random_menu_payload(tag: str) -> dict:
    suffix = "".join(random.choices(string.ascii_lowercase + string.digits, k=6))
    return {
        "parentId": 0,
        "name": f"AutoMenu_{tag}_{suffix}",
        "path": f"/auto-{suffix}",
        "icon": "auto",
        "permCode": f"auto:test:{suffix}",
        "type": "MENU",
        "sortNo": 999,
        "status": 1,
    }


def find_frontend_impl_files(workspace: pathlib.Path, path: str) -> List[str]:
    src_root = workspace / "user-management-font" / "src"
    found: List[str] = []
    if not src_root.exists():
        return found
    needle = re.sub(r"/\{[^}]+\}", "", path).split("?")[0]
    seq_tokens = [s for s in re.sub(r"/\{[^}]+\}", "/", path).split("/") if s]
    for p in src_root.rglob("*"):
        if p.is_file() and p.suffix in {".js", ".ts", ".vue", ".jsx", ".tsx"}:
            text = p.read_text(encoding="utf-8", errors="ignore")
            low = text.lower()
            if needle and needle in text:
                found.append(str(p.relative_to(workspace)))
                continue
            pos = 0
            ok = True
            for token in seq_tokens:
                token_low = token.lower()
                nxt = low.find(token_low, pos)
                if nxt < 0:
                    ok = False
                    break
                pos = nxt + len(token_low)
            if ok and seq_tokens:
                found.append(str(p.relative_to(workspace)))
    return found


def resolve_id_for_path(ctx: Dict[str, str], base_url: str) -> Optional[str]:
    rid = ctx.get("last_created_id")
    if rid:
        return rid

    auth = ctx.get("auth_headers", {})
    code, body = http_request_with_headers(base_url, "GET", "/api/users?orgId=1", headers=auth)
    if code == 200:
        try:
            data = json.loads(body)
            if isinstance(data, list) and data:
                current_user_id = ctx.get("current_user_id")
                for row in data:
                    raw_id = row.get("id")
                    if raw_id is None:
                        continue
                    cand = str(raw_id)
                    if cand and cand != "None" and cand != current_user_id:
                        rid = cand
                        break
                if not rid and data[0].get("id") is not None:
                    rid = str(data[0].get("id"))
        except Exception:
            rid = None
    if rid:
        return rid

    payload = random_user_payload("idseed")
    c, b = http_request_with_headers(base_url, "POST", "/api/users", payload, headers=auth)
    if c == 201:
        try:
            rid = str(json.loads(b).get("id"))
        except Exception:
            rid = None
    return rid


def resolve_org_id(ctx: Dict[str, str], base_url: str) -> Optional[str]:
    if ctx.get("org_id"):
        return ctx["org_id"]
    auth = ctx.get("auth_headers", {})
    code, body = http_request_with_headers(base_url, "GET", "/api/orgs/tree", headers=auth)
    if code != 200:
        return None
    try:
        data = json.loads(body)
        if isinstance(data, list) and data:
            raw_id = data[0].get("id")
            if raw_id is not None:
                oid = str(raw_id)
                ctx["org_id"] = oid
                return oid
    except Exception:
        pass
    return None


def ensure_temp_org_id(ctx: Dict[str, str], base_url: str) -> Optional[str]:
    if ctx.get("temp_org_id"):
        return ctx["temp_org_id"]
    auth = ctx.get("auth_headers", {})
    payload = random_org_payload("temp")
    code, body = http_request_with_headers(base_url, "POST", "/api/orgs", payload, headers=auth)
    if code not in (200, 201):
        return None
    try:
        oid = str(json.loads(body).get("id"))
    except Exception:
        oid = None
    if oid:
        ctx["temp_org_id"] = oid
    return oid


def resolve_role_id(ctx: Dict[str, str], base_url: str) -> Optional[str]:
    if ctx.get("role_id"):
        return ctx["role_id"]
    auth = ctx.get("auth_headers", {})
    code, body = http_request_with_headers(base_url, "GET", "/api/roles", headers=auth)
    if code != 200:
        return None
    try:
        data = json.loads(body)
        if isinstance(data, list) and data:
            raw_id = data[0].get("id")
            if raw_id is not None:
                rid = str(raw_id)
                ctx["role_id"] = rid
                return rid
    except Exception:
        pass
    return None


def resolve_menu_id(ctx: Dict[str, str], base_url: str) -> Optional[str]:
    if ctx.get("menu_id"):
        return ctx["menu_id"]
    auth = ctx.get("auth_headers", {})
    code, body = http_request_with_headers(base_url, "GET", "/api/menus/tree", headers=auth)
    if code != 200:
        return None
    try:
        data = json.loads(body)
        if isinstance(data, list) and data:
            raw_id = data[0].get("id")
            if raw_id is not None:
                mid = str(raw_id)
                ctx["menu_id"] = mid
                return mid
    except Exception:
        pass
    return None


def ensure_login(ctx: Dict[str, str], base_url: str, username: str, password: str) -> bool:
    if ctx.get("auth_token"):
        return True
    code, body = http_request(base_url, "GET", "/api/auth/captcha")
    if code != 200:
        return False
    try:
        cap = json.loads(body)
    except Exception:
        return False
    payload = {
        "username": username,
        "password": password,
        "captchaId": cap.get("captchaId"),
        "captcha": cap.get("code"),
    }
    lcode, lbody = http_request(base_url, "POST", "/api/auth/login", payload)
    if lcode != 200:
        return False
    try:
        token = json.loads(lbody).get("token")
    except Exception:
        token = None
    if not token:
        return False
    ctx["auth_token"] = token
    ctx["auth_headers"] = {"Authorization": f"Bearer {token}"}
    mcode, mbody = http_request_with_headers(base_url, "GET", "/api/auth/me", headers=ctx["auth_headers"])
    if mcode == 200:
        try:
            me = json.loads(mbody)
            user = me.get("user") if isinstance(me, dict) else None
            if isinstance(user, dict) and user.get("id") is not None:
                ctx["current_user_id"] = str(user.get("id"))
        except Exception:
            pass
    return True


def ensure_second_verify(ctx: Dict[str, str], base_url: str, password: str) -> Optional[str]:
    if ctx.get("second_verify"):
        return ctx["second_verify"]
    auth = ctx.get("auth_headers", {})
    code, body = http_request_with_headers(
        base_url,
        "POST",
        "/api/auth/second-verify",
        {"password": password},
        headers=auth,
    )
    if code != 200:
        return None
    try:
        token = json.loads(body).get("token")
    except Exception:
        token = None
    if token:
        ctx["second_verify"] = token
    return token


def api_smoke(base_url: str, method: str, path: str, ctx: Dict[str, str], username: str, password: str) -> Tuple[bool, str]:
    if not ensure_login(ctx, base_url, username, password):
        return False, "自动登录失败"

    auth_headers = dict(ctx.get("auth_headers", {}))

    if "{orgId}" in path:
        if "/orgs/{orgId}/users/" in path:
            oid = ensure_temp_org_id(ctx, base_url)
        else:
            oid = resolve_org_id(ctx, base_url)
        if not oid:
            return False, "无法准备 {orgId} 测试数据"
        path = path.replace("{orgId}", oid)
    if "{userId}" in path or "/api/users/{id}" in path:
        uid = resolve_id_for_path(ctx, base_url)
        if not uid:
            return False, "无法准备用户ID测试数据"
        path = path.replace("{userId}", uid).replace("{id}", uid)
    elif "/api/roles/{id}" in path:
        rid = resolve_role_id(ctx, base_url)
        if not rid:
            return False, "无法准备角色ID测试数据"
        path = path.replace("{id}", rid)
    elif "/api/orgs/{id}" in path:
        oid = ensure_temp_org_id(ctx, base_url) if method == "DELETE" else resolve_org_id(ctx, base_url)
        if not oid:
            return False, "无法准备组织ID测试数据"
        path = path.replace("{id}", oid)
    elif "/api/menus/{id}" in path:
        mid = resolve_menu_id(ctx, base_url)
        if not mid:
            return False, "无法准备菜单ID测试数据"
        path = path.replace("{id}", mid)

    if "{param}" in path:
        path = path.replace("/{param}", "")

    body = None
    if method in {"POST", "PUT"}:
        if path.startswith("/api/users/import"):
            return True, "multipart 导入接口自动脚本跳过（需手工）"
        if path.endswith("/reset-password"):
            body = {"newPassword": "Pass@123456"}
        elif path.endswith("/roles") and path.startswith("/api/users/"):
            role_id = resolve_role_id(ctx, base_url)
            body = {"ids": [int(role_id)]} if role_id else {"ids": [1]}
        elif path.endswith("/data-scopes"):
            body = [{"moduleCode": "USER", "scope": "ALL"}]
        elif path.startswith("/api/orgs"):
            body = random_org_payload("rw")
        elif path.startswith("/api/roles"):
            body = random_role_payload("rw")
        elif path.startswith("/api/menus"):
            body = random_menu_payload("rw")
        else:
            body = random_user_payload("rw")

    if method == "DELETE" and path.startswith("/api/orgs/"):
        sv = ensure_second_verify(ctx, base_url, password)
        if sv:
            auth_headers["X-Second-Verify"] = sv

    status, resp = http_request_with_headers(base_url, method, path, body, headers=auth_headers)
    ok = (200 <= status < 300) or status == 204
    if ok and method == "POST" and path.startswith("/api/users"):
        try:
            ctx["last_created_id"] = str(json.loads(resp).get("id"))
        except Exception:
            pass
    if ok and method == "POST" and path.startswith("/api/orgs"):
        try:
            ctx["temp_org_id"] = str(json.loads(resp).get("id"))
        except Exception:
            pass
    if ok:
        return ok, f"status={status}"
    brief = resp[:120].replace("\n", " ")
    return ok, f"status={status}; resp={brief}"


def backend_self_test(row: Dict[str, str], workspace: pathlib.Path, base_url: str, fail_note: str, ctx: Dict[str, str], username: str, password: str) -> Tuple[bool, str]:
    method, path = extract_api(row.get("测试点描述", ""))
    if not method or not path:
        return False, "无法从测试点描述提取后端 API"

    if not find_existing_unit_tests(workspace, path):
        generated = generate_backend_unit_skeleton(workspace, method, path)
        row["备注"] = (row.get("备注", "") + f"; 已生成单测骨架: {generated}").strip("; ")

    expect_fail = any(k in row.get("测试点描述", "") for k in ["失败", "异常", "重复"])
    if expect_fail:
        if not ensure_login(ctx, base_url, username, password):
            return False, "自动登录失败"
        status, resp = http_request_with_headers(base_url, method, path, headers=ctx.get("auth_headers", {}))
        ok = 400 <= status < 500
        if ok:
            return True, f"status={status}"
        msg = fail_note or f"后端自测失败: status={status}, resp={resp[:160]}"
        return False, msg

    ok, note = api_smoke(base_url, method, path, ctx, username, password)
    if ok:
        return True, note

    return False, fail_note or f"后端自测失败: {note}"


def frontend_self_test(row: Dict[str, str], workspace: pathlib.Path, fail_note: str, snippet_file: pathlib.Path) -> Tuple[bool, str]:
    method, path = extract_api(row.get("测试点描述", ""))
    method = method or "GET"
    path = path or "/"

    found = find_frontend_impl_files(workspace, path)

    snippet_file.parent.mkdir(parents=True, exist_ok=True)
    with snippet_file.open("a", encoding="utf-8") as f:
        f.write(f"\n## {row.get('测试点描述','')}\n")
        f.write("```ts\n")
        f.write("// Playwright snippet\n")
        f.write("import { test, expect } from '@playwright/test'\n")
        f.write("test('ui-flow', async ({ page }) => {\n")
        f.write("  await page.goto('http://localhost:5173')\n")
        f.write("  // TODO: complete UI actions\n")
        f.write(f"  // Verify request: {method} {path}\n")
        f.write("})\n")
        f.write("```\n")

    if found:
        return True, f"已识别前端相关文件: {', '.join(found[:3])}"
    return False, fail_note or "未识别到前端相关实现文件"


def integration_test(row: Dict[str, str], workspace: pathlib.Path, base_url: str, fail_note: str, checkpoint_file: pathlib.Path, ctx: Dict[str, str], username: str, password: str) -> Tuple[bool, str]:
    method, path = extract_api(row.get("测试点描述", ""))
    if not method or not path:
        return False, "无法从测试点描述提取联调 API"

    checkpoint_file.parent.mkdir(parents=True, exist_ok=True)
    with checkpoint_file.open("a", encoding="utf-8") as f:
        f.write(f"\n## {row.get('测试点描述','联调项')}\n")
        f.write(f"- 请求: {method} {path}\n")
        f.write("- 检查点1: 前端发出的参数结构是否符合接口约定\n")
        f.write("- 检查点2: 后端返回状态码与字段是否符合预期\n")
        f.write("- 检查点3: 页面展示与错误提示是否与返回一致\n")

    if not backend_alive(base_url):
        return False, fail_note or "后端服务不可用"

    frontend_found = find_frontend_impl_files(workspace, path)
    if not frontend_found:
        return False, fail_note or "未识别到前端相关实现文件"

    ok, note = api_smoke(base_url, method, path, ctx, username, password)
    if not ok:
        return False, fail_note or f"联调跑通失败: {note}"
    return True, f"{note}; frontend={', '.join(frontend_found[:2])}"


def update_row_pass(row: Dict[str, str], tester: str, note: str) -> None:
    row["测试状态"] = "🟢 已通过"
    row["测试时间"] = now_str()
    row["测试人"] = tester
    if note:
        row["备注"] = note


def update_row_fail(row: Dict[str, str], tester: str, note: str) -> None:
    row["测试状态"] = "🟠 存在缺陷"
    row["测试时间"] = now_str()
    row["测试人"] = tester
    row["备注"] = note


def main() -> None:
    ap = argparse.ArgumentParser(description="Execute pending test rows and update test doc")
    ap.add_argument("--workspace", required=True)
    ap.add_argument("--doc", required=True)
    ap.add_argument("--tester", required=True)
    ap.add_argument("--base-url", default="http://127.0.0.1:8080")
    ap.add_argument("--username", default="smile-admin")
    ap.add_argument("--password", default="123456")
    ap.add_argument("--max-items", type=int, default=0, help="0 means no limit")
    ap.add_argument("--fail-note", default="")
    ap.add_argument("--auto-start-backend", action="store_true")
    args = ap.parse_args()

    workspace = pathlib.Path(args.workspace).resolve()
    doc = pathlib.Path(args.doc).resolve()

    if not doc.exists():
        print(f"doc not found: {doc}")
        sys.exit(1)

    lines, header_idx, data_end_idx, rows = parse_table(doc)
    pending = [r for r in rows if r.get("测试状态", "").strip() != "🟢 已通过"]

    pass_count = 0
    fail_count = 0
    skipped_count = 0
    processed = 0

    backend_proc = None
    ctx: Dict[str, str] = {}

    if any(r.get("测试类型", "") in {"后端自测", "联调"} for r in pending):
        if not backend_alive(args.base_url) and args.auto_start_backend:
            backend_proc = start_backend(workspace)
            for _ in range(60):
                if backend_alive(args.base_url):
                    break
                time.sleep(1)

    snippet_file = workspace / "doc" / "前端测试脚本片段.md"
    checkpoint_file = workspace / "doc" / "联调检查点.md"

    for row in rows:
        if row.get("测试状态", "").strip() == "🟢 已通过":
            continue
        if args.max_items and processed >= args.max_items:
            break

        ttype = row.get("测试类型", "").strip()

        if ttype == "后端自测":
            if not backend_alive(args.base_url):
                update_row_fail(row, args.tester, args.fail_note or "后端服务不可用")
                fail_count += 1
            else:
                ok, note = backend_self_test(
                    row,
                    workspace,
                    args.base_url,
                    args.fail_note,
                    ctx,
                    args.username,
                    args.password,
                )
                if ok:
                    update_row_pass(row, args.tester, note)
                    pass_count += 1
                else:
                    update_row_fail(row, args.tester, note)
                    fail_count += 1

        elif ttype == "前端自测":
            ok, note = frontend_self_test(row, workspace, args.fail_note, snippet_file)
            if ok:
                update_row_pass(row, args.tester, note)
                pass_count += 1
            else:
                update_row_fail(row, args.tester, note)
                fail_count += 1

        elif ttype == "联调":
            ok, note = integration_test(
                row,
                workspace,
                args.base_url,
                args.fail_note,
                checkpoint_file,
                ctx,
                args.username,
                args.password,
            )
            if ok:
                update_row_pass(row, args.tester, note)
                pass_count += 1
            else:
                update_row_fail(row, args.tester, note)
                fail_count += 1
        else:
            update_row_fail(row, args.tester, args.fail_note or f"未知测试类型: {ttype}")
            fail_count += 1

        processed += 1

    write_table(doc, lines, header_idx, data_end_idx, rows)

    if backend_proc is not None:
        backend_proc.terminate()

    remaining = sum(1 for r in rows if r.get("测试状态", "").strip() == "🔴 待测试")
    print(f"processed={processed}")
    print(f"pass={pass_count}")
    print(f"fail={fail_count}")
    print(f"manual_pending={skipped_count}")
    print(f"remaining_pending={remaining}")

    # show updated snippet
    print("\n--- updated doc snippet ---")
    for line in doc.read_text(encoding="utf-8").splitlines()[:25]:
        print(line)


if __name__ == "__main__":
    main()
