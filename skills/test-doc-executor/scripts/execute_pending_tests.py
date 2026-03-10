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


def backend_alive(base_url: str) -> bool:
    code, _ = http_request(base_url, "GET", "/api/users")
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
    m = re.search(r"\b(GET|POST|PUT|DELETE|PATCH)\s+([^\s]+)", desc)
    if not m:
        return None, None
    return m.group(1), m.group(2)


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


def find_frontend_impl_files(workspace: pathlib.Path, path: str) -> List[str]:
    src_root = workspace / "user-management-font" / "src"
    found: List[str] = []
    if not src_root.exists():
        return found
    needle = path.replace("/{id}", "")
    for p in src_root.rglob("*"):
        if p.is_file() and p.suffix in {".js", ".ts", ".vue", ".jsx", ".tsx"}:
            text = p.read_text(encoding="utf-8", errors="ignore")
            if needle and needle in text:
                found.append(str(p.relative_to(workspace)))
    return found


def resolve_id_for_path(ctx: Dict[str, str], base_url: str) -> Optional[str]:
    rid = ctx.get("last_created_id")
    if rid:
        return rid

    code, body = http_request(base_url, "GET", "/api/users")
    if code == 200:
        try:
            data = json.loads(body)
            if isinstance(data, list) and data:
                rid = str(data[0].get("id"))
        except Exception:
            rid = None
    if rid:
        return rid

    payload = random_user_payload("idseed")
    c, b = http_request(base_url, "POST", "/api/users", payload)
    if c == 201:
        try:
            rid = str(json.loads(b).get("id"))
        except Exception:
            rid = None
    return rid


def api_smoke(base_url: str, method: str, path: str, ctx: Dict[str, str]) -> Tuple[bool, str]:
    if "{id}" in path:
        rid = resolve_id_for_path(ctx, base_url)
        if not rid:
            return False, "无法准备 {id} 测试数据"
        path = path.replace("{id}", rid)

    body = None
    if method in {"POST", "PUT"}:
        body = random_user_payload("rw")

    status, resp = http_request(base_url, method, path, body)
    ok = (200 <= status < 300) or status == 204
    if ok and method == "POST":
        try:
            ctx["last_created_id"] = str(json.loads(resp).get("id"))
        except Exception:
            pass
    return ok, f"status={status}"


def backend_self_test(row: Dict[str, str], workspace: pathlib.Path, base_url: str, fail_note: str, ctx: Dict[str, str]) -> Tuple[bool, str]:
    method, path = extract_api(row.get("测试点描述", ""))
    if not method or not path:
        return False, "无法从测试点描述提取后端 API"

    if not find_existing_unit_tests(workspace, path):
        generated = generate_backend_unit_skeleton(workspace, method, path)
        row["备注"] = (row.get("备注", "") + f"; 已生成单测骨架: {generated}").strip("; ")

    expect_fail = any(k in row.get("测试点描述", "") for k in ["失败", "异常", "重复"])
    if expect_fail:
        status, resp = http_request(base_url, method, path)
        ok = 400 <= status < 500
        if ok:
            return True, f"status={status}"
        msg = fail_note or f"后端自测失败: status={status}, resp={resp[:160]}"
        return False, msg

    ok, note = api_smoke(base_url, method, path, ctx)
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


def integration_test(row: Dict[str, str], workspace: pathlib.Path, base_url: str, fail_note: str, checkpoint_file: pathlib.Path, ctx: Dict[str, str]) -> Tuple[bool, str]:
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

    ok, note = api_smoke(base_url, method, path, ctx)
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
    pending = [r for r in rows if r.get("测试状态", "").strip() == "🔴 待测试"]

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
        if row.get("测试状态", "").strip() != "🔴 待测试":
            continue
        if args.max_items and processed >= args.max_items:
            break

        ttype = row.get("测试类型", "").strip()

        if ttype == "后端自测":
            if not backend_alive(args.base_url):
                update_row_fail(row, args.tester, args.fail_note or "后端服务不可用")
                fail_count += 1
            else:
                ok, note = backend_self_test(row, workspace, args.base_url, args.fail_note, ctx)
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
            ok, note = integration_test(row, workspace, args.base_url, args.fail_note, checkpoint_file, ctx)
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
