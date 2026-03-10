---
name: commitandpush
description: Detect current git branch, generate a commit message from working tree changes, commit, and push to remote. Use when user asks to commit and push, auto-commit, generate commit message, or push current changes.
---

# Commit And Push

Perform exactly:
1. Detect current branch via `git branch --show-current`.
2. If repo has changes, pull latest code (`git pull`) first.
3. Generate commit message from changes and commit.
4. Push to remote via `git push`.
4. If no changes, do not run git commit/push; print "当前项目无变更".

## Command
```bash
bash /Users/smile/workspace/project/study/skills/commitandpush/scripts/commit_and_push.sh
```

## Notes
- Push requires network access and may require escalation/approval in sandboxed environments.
- Script uses `origin` and pushes the current branch with `-u` if needed.
