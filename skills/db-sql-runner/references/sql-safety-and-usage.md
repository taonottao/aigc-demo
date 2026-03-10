# SQL Safety And Usage

## Inputs Checklist
- Confirm local config exists: `study/local-config/database.json`.
- Confirm DB engine in config: `mysql` or `postgresql` (or `postgres`).
- Confirm host/port and database name in config.
- Confirm username/password in config are valid.
- Confirm whether task is read-only or write operation.

## Recommended Execution Order
1. Run a `SELECT` preview to confirm target rows.
2. If write is needed, run `--mode execute --allow-write`.
3. Re-run verification query and report before/after counts.

## Output Modes
- `table`: readable terminal table.
- `json`: machine-friendly result.
- `csv`: export-like result.

## Examples
```bash
python3 scripts/db_sql_exec.py \
  --mode query \
  --sql "SELECT COUNT(*) AS c FROM orders" \
  --output table
```

```bash
python3 scripts/db_sql_exec.py \
  --mode execute \
  --allow-write \
  --sql "DELETE FROM sessions WHERE expires_at < NOW()"
```

## Config Format
```json
{
  "engine": "postgresql",
  "host": "localhost",
  "port": 5432,
  "user": "smile",
  "password": "123456",
  "database": "postgres"
}
```
