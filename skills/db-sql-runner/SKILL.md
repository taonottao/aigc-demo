---
name: db-sql-runner
description: Auto-load local MySQL or PostgreSQL connection settings from a local config file, then run SQL for query and write operations. Use when the user asks to connect a DB and execute SQL tasks such as SELECT, INSERT, UPDATE, DELETE, schema checks, or data fixes without repeatedly re-entering credentials.
---

# DB SQL Runner

Use this skill to run SQL against MySQL/PostgreSQL safely and repeatably.
Default behavior is auto-reading local config from `study/local-config/database.json`.

## Workflow
1. Load local DB config from `study/local-config/database.json`.
2. Optionally override specific fields using CLI flags:
- `--engine`, `--host`, `--port`, `--user`, `--password`, `--database`
3. Collect SQL input:
- One statement via `--sql`, or multi-line SQL via `--sql-file`
4. Decide mode:
- Read-only query: `--mode query`
- Write operation: `--mode execute` and require `--allow-write`
5. Execute script:
- `python3 scripts/db_sql_exec.py ...`
6. Return result summary:
- For query: row count + table/json/csv output
- For execute: affected rows + commit status

## Command Patterns

```bash
# Query (uses local-config/database.json automatically)
python3 scripts/db_sql_exec.py \
  --mode query \
  --sql "SELECT id, email FROM users LIMIT 20" \
  --output table
```

```bash
# Write operation (with explicit write flag)
python3 scripts/db_sql_exec.py \
  --mode execute \
  --allow-write \
  --sql "UPDATE users SET active = 0 WHERE last_login_at < NOW() - INTERVAL '365 days'"
```

## Safety Rules
- Confirm with user before running `--mode execute` on production.
- Use `--mode query` first to preview target rows before update/delete.
- Require explicit `--allow-write` for non-read SQL.
- Never log raw credentials in responses.
- If dependency import fails, ask to install only the missing driver:
  - MySQL: `pymysql`
  - PostgreSQL: `psycopg` (or `psycopg2`)

## References
- Read `references/sql-safety-and-usage.md` for operational guidance and examples.
