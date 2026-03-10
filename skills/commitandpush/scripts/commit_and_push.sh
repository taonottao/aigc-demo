#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel 2>/dev/null || true)"
if [[ -z "$repo_root" ]]; then
  echo "[FAIL] not a git repo"
  exit 1
fi
cd "$repo_root"

branch="$(git branch --show-current)"
if [[ -z "$branch" ]]; then
  echo "[FAIL] cannot determine current branch (detached HEAD?)"
  exit 1
fi

changes="$(git status --porcelain)"
if [[ -z "$changes" ]]; then
  echo "当前项目无变更"
  exit 0
fi

# Pull latest before committing local changes.
# Prefer rebase to keep history linear; autostash avoids failing on dirty tree.
upstream="$(git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>/dev/null || true)"
if [[ -n "$upstream" ]]; then
  git pull --rebase --autostash
else
  git pull --rebase --autostash origin "$branch"
fi

# Refresh changes after pull/rebase.
changes="$(git status --porcelain)"

gen_commit_message() {
  local files count top_dirs type scope

  # Only consider tracked/renamed/added/modified/deleted paths from porcelain.
  files="$(printf '%s\n' "$changes" | sed -E 's/^..\s+//')"
  count="$(printf '%s\n' "$files" | sed '/^$/d' | wc -l | tr -d ' ')"
  top_dirs="$(printf '%s\n' "$files" | awk -F/ '{print $1}' | sed 's/^\.$/root/' | sort -u)"

  type="chore"
  if printf '%s\n' "$files" | rg -q '^(doc/|.*\.md$)'; then
    type="docs"
  fi
  if printf '%s\n' "$files" | rg -q '^user-management-(backend|font)/'; then
    type="feat"
  fi
  if printf '%s\n' "$files" | rg -q '^skills/'; then
    type="chore"
  fi

  scope="$(printf '%s\n' "$top_dirs" | head -n1)"
  if [[ "$(printf '%s\n' "$top_dirs" | wc -l | tr -d ' ')" -gt 1 ]]; then
    scope="multi"
  fi

  if [[ "$scope" == "multi" ]]; then
    echo "$type: update $count file(s)"
  else
    echo "$type($scope): update $count file(s)"
  fi
}

subject="$(gen_commit_message)"
body="$(printf '%s\n' "$changes" | sed -E 's/\s+$//' | head -n 200)"

# Stage everything and commit.
git add -A

if git diff --cached --quiet; then
  echo "当前项目无变更"
  exit 0
fi

git commit -m "$subject" -m "$body"

# Push current branch.
# -u ensures upstream is set on first push.
git push -u origin "$branch"

echo "[OK] pushed origin/$branch"
