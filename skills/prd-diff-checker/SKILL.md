---
name: prd-diff-checker
description: Compare implementation in this workspace (frontend/backend code) against the product requirements document doc/prd.md and produce a gap report of missing/partial/implemented features with evidence. Use when user asks to check PRD gaps, requirements vs code differences, or scope coverage.
---

# PRD Diff Checker

Generate a requirements gap report between `doc/prd.md` and current code.

## What It Does
- Parses `doc/prd.md` into discrete requirement points (headings, bullets, and table rows).
- Scans backend (Java controllers/endpoints) and frontend (routes/views/api calls).
- Marks each requirement as:
  - `FOUND` (evidence exists in code)
  - `PARTIAL` (some hints exist but missing core pieces)
  - `MISSING` (no implementation evidence)
- Writes a markdown report to `doc/需求差异报告.md` with evidence references.

## Command
```bash
python3 /Users/smile/workspace/project/study/skills/prd-diff-checker/scripts/check_prd_gaps.py \
  --root /Users/smile/workspace/project/study \
  --prd /Users/smile/workspace/project/study/doc/prd.md \
  --output /Users/smile/workspace/project/study/doc/需求差异报告.md
```

Optional scope:
```bash
# Only analyze changed files in a diff range
python3 /Users/smile/workspace/project/study/skills/prd-diff-checker/scripts/check_prd_gaps.py \
  --root /Users/smile/workspace/project/study \
  --prd /Users/smile/workspace/project/study/doc/prd.md \
  --output /Users/smile/workspace/project/study/doc/需求差异报告.md \
  --diff "HEAD~1..HEAD"

# Only focus on a module keyword/path
python3 /Users/smile/workspace/project/study/skills/prd-diff-checker/scripts/check_prd_gaps.py \
  --root /Users/smile/workspace/project/study \
  --prd /Users/smile/workspace/project/study/doc/prd.md \
  --output /Users/smile/workspace/project/study/doc/需求差异报告.md \
  --module "用户管理"
```

## Notes
- This is a heuristic checker: it relies on evidence in code (endpoints, routes, API calls, keywords). Treat `PARTIAL/MISSING` as a triage queue.
