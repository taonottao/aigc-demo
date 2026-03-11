---
name: test-doc-generator
description: Scan workspace frontend/backend code changes (or specified module/diff) and generate/update doc/功能测试清单.md by filling missing function points into a standardized test-case checklist. Use when users ask to generate test docs, update test checklist from implementation, initialize test status, or summarize new test points.
---

# Test Doc Generator

Generate/update markdown test checklist only. Do not run regression or service startup tasks.

## Core Intent
- Scan current project frontend/backend code changes or specified module.
- If no scope is given, compare current implementation with existing checklist and append missing function points.
- Create/update standardized `doc/功能测试清单.md`.
- Initialize all newly recognized points with `🔴 待测试`.
- Return summary of newly added test points count.

## Execution Logic
1. Code awareness:
- Analyze workspace or `--diff` range.
- Support `--module` filter.
2. Feature extraction:
- Backend API endpoints
- Frontend UI/API interaction logic
- Business rules (validation/exception)
3. Document generation:
- Create/update markdown checklist with required 10 columns (aligned with existing doc):
  - 功能模块
  - 测试点描述
  - 前置条件
  - 预期结果
  - 测试类型（前端自测/后端自测/联调）
  - 测试状态（默认 `🔴 待测试`）
  - 测试时间（新项默认空）
  - 测试人（新项默认空）
  - 负责人
  - 备注
4. Status initialization:
- Ensure all new points are `🔴 待测试`.
5. Output feedback:
- Print `new_points=<count>` summary.

## Command
```bash
python3 /Users/smile/workspace/project/study/skills/test-doc-generator/scripts/generate_feature_test_checklist.py \
  --root /Users/smile/workspace/project/study \
  --output /Users/smile/workspace/project/study/doc/功能测试清单.md \
  --owner 待分配
```

Optional scope:
```bash
python3 /Users/smile/workspace/project/study/skills/test-doc-generator/scripts/generate_feature_test_checklist.py \
  --root /Users/smile/workspace/project/study \
  --output /Users/smile/workspace/project/study/doc/功能测试清单.md \
  --diff "HEAD~1..HEAD" \
  --module user-management-backend \
  --owner 张三
```
