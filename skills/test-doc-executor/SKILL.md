---
name: test-doc-executor
description: Read doc/功能测试清单.md, process rows with status 🔴 待测试, execute or guide tests by type (后端自测/前端自测/联调), update row status and metadata in real time, and output pass/fail summary with updated document snippet.
---

# Test Doc Executor

Only execute pending test items from the checklist and update document status.

## Core Intent
- Read test document rows with `测试状态 = 🔴 待测试`.
- Execute or guide by `测试类型`:
  - `后端自测`: detect existing unit tests; if none, generate unit-test skeleton, then run API smoke verification.
  - `前端自测`: check component/API usage logic and generate Playwright/Cypress snippets.
  - `联调`: auto-verify the flow is runnable end-to-end by (1) ensuring frontend implementation references exist, and (2) calling the backend API successfully; also writes data-flow checkpoints.
- Update status in document in real time.

## State Update Rules
- Pass: set `🟢 已通过`, write `测试时间` + `测试人`.
- Fail: set `🟠 存在缺陷`, fill `备注` with error brief.
- Repeat until all pending rows are processed or manual stop condition occurs.

## Command
```bash
python3 /Users/smile/workspace/project/study/skills/test-doc-executor/scripts/execute_pending_tests.py \
  --workspace /Users/smile/workspace/project/study \
  --doc /Users/smile/workspace/project/study/doc/功能测试清单.md \
  --tester Codex
```

Optional:
```bash
# Manual stop after N items
python3 /Users/smile/workspace/project/study/skills/test-doc-executor/scripts/execute_pending_tests.py \
  --workspace /Users/smile/workspace/project/study \
  --doc /Users/smile/workspace/project/study/doc/功能测试清单.md \
  --tester 张三 \
  --max-items 5
```
