#!/usr/bin/env python3
"""Run SQL against MySQL/PostgreSQL from CLI inputs."""

from __future__ import annotations

import argparse
import csv
import json
import pathlib
import re
import sys
from typing import Iterable


READ_ONLY_PREFIXES = ("select", "with", "show", "describe", "desc", "explain")
DEFAULT_CONFIG_PATH = pathlib.Path(__file__).resolve().parents[3] / "local-config" / "database.json"
REQUIRED_CONN_FIELDS = ("engine", "host", "port", "user", "password", "database")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Execute SQL on MySQL/PostgreSQL with explicit write safety checks."
    )
    parser.add_argument("--config", default=str(DEFAULT_CONFIG_PATH))
    parser.add_argument("--engine", choices=("mysql", "postgres", "postgresql"))
    parser.add_argument("--host")
    parser.add_argument("--port", type=int)
    parser.add_argument("--user")
    parser.add_argument("--password")
    parser.add_argument("--database")
    parser.add_argument("--mode", choices=("query", "execute"), required=True)
    parser.add_argument("--sql", help="SQL statement text")
    parser.add_argument("--sql-file", help="Path to SQL file")
    parser.add_argument("--allow-write", action="store_true")
    parser.add_argument("--output", choices=("table", "json", "csv"), default="table")
    return parser.parse_args()


def load_db_config(config_path: str) -> dict:
    path = pathlib.Path(config_path).expanduser()
    if not path.exists():
        raise ValueError(f"Config file not found: {path}")
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ValueError(f"Invalid config JSON: {path}") from exc
    if not isinstance(data, dict):
        raise ValueError("Config JSON must be an object.")
    return data


def merge_conn_config(args: argparse.Namespace, config: dict) -> dict:
    conn = dict(config)
    for field in REQUIRED_CONN_FIELDS:
        cli_value = getattr(args, field)
        if cli_value is not None:
            conn[field] = cli_value

    if conn.get("engine") == "postgresql":
        conn["engine"] = "postgres"

    missing = [field for field in REQUIRED_CONN_FIELDS if not conn.get(field)]
    if missing:
        missing_csv = ", ".join(missing)
        raise ValueError(f"Missing required connection field(s): {missing_csv}")

    try:
        conn["port"] = int(conn["port"])
    except (TypeError, ValueError) as exc:
        raise ValueError("Connection field 'port' must be an integer.") from exc

    if conn["engine"] not in ("mysql", "postgres"):
        raise ValueError("Connection field 'engine' must be mysql, postgres, or postgresql.")

    return conn


def load_sql(args: argparse.Namespace) -> str:
    if bool(args.sql) == bool(args.sql_file):
        raise ValueError("Provide exactly one of --sql or --sql-file.")
    if args.sql:
        sql = args.sql
    else:
        sql = pathlib.Path(args.sql_file).read_text(encoding="utf-8")
    sql = sql.strip()
    if not sql:
        raise ValueError("SQL is empty.")
    return sql


def first_keyword(sql: str) -> str:
    cleaned = re.sub(r"^\s*(--.*?$|/\*.*?\*/\s*)*", "", sql, flags=re.S | re.M)
    match = re.match(r"([a-zA-Z]+)", cleaned.strip())
    return match.group(1).lower() if match else ""


def should_be_read_only(sql: str) -> bool:
    return first_keyword(sql) in READ_ONLY_PREFIXES


def connect_mysql(conn_cfg: dict):
    try:
        import pymysql  # type: ignore
    except ImportError as exc:
        raise RuntimeError("Missing dependency: pymysql") from exc

    return pymysql.connect(
        host=conn_cfg["host"],
        port=conn_cfg["port"],
        user=conn_cfg["user"],
        password=conn_cfg["password"],
        database=conn_cfg["database"],
        charset="utf8mb4",
        cursorclass=pymysql.cursors.Cursor,
        autocommit=False,
    )


def connect_postgres(conn_cfg: dict):
    try:
        import psycopg  # type: ignore

        return psycopg.connect(
            host=conn_cfg["host"],
            port=conn_cfg["port"],
            user=conn_cfg["user"],
            password=conn_cfg["password"],
            dbname=conn_cfg["database"],
            autocommit=False,
        )
    except ImportError:
        pass

    try:
        import psycopg2  # type: ignore
    except ImportError as exc:
        raise RuntimeError("Missing dependency: psycopg (or psycopg2)") from exc

    return psycopg2.connect(
        host=conn_cfg["host"],
        port=conn_cfg["port"],
        user=conn_cfg["user"],
        password=conn_cfg["password"],
        dbname=conn_cfg["database"],
    )


def print_table(columns: Iterable[str], rows: list[tuple]) -> None:
    cols = list(columns)
    widths = [len(str(c)) for c in cols]
    for row in rows:
        for i, value in enumerate(row):
            widths[i] = max(widths[i], len("" if value is None else str(value)))

    header = " | ".join(str(col).ljust(widths[i]) for i, col in enumerate(cols))
    sep = "-+-".join("-" * w for w in widths)
    print(header)
    print(sep)
    for row in rows:
        print(
            " | ".join(
                ("" if value is None else str(value)).ljust(widths[i])
                for i, value in enumerate(row)
            )
        )


def print_json(columns: Iterable[str], rows: list[tuple]) -> None:
    cols = list(columns)
    data = [{cols[i]: row[i] for i in range(len(cols))} for row in rows]
    print(json.dumps(data, ensure_ascii=False, indent=2, default=str))


def print_csv(columns: Iterable[str], rows: list[tuple]) -> None:
    writer = csv.writer(sys.stdout)
    writer.writerow(list(columns))
    writer.writerows(rows)


def main() -> int:
    args = parse_args()
    try:
        config = load_db_config(args.config)
        conn_cfg = merge_conn_config(args, config)
        sql = load_sql(args)
    except Exception as exc:
        print(f"Input error: {exc}", file=sys.stderr)
        return 2

    is_read_only = should_be_read_only(sql)
    if args.mode == "query" and not is_read_only:
        print(
            "Safety check failed: query mode only accepts read-only SQL.",
            file=sys.stderr,
        )
        return 2
    if args.mode == "execute" and not args.allow_write and not is_read_only:
        print(
            "Safety check failed: pass --allow-write for write operations.",
            file=sys.stderr,
        )
        return 2

    try:
        conn = (
            connect_mysql(conn_cfg)
            if conn_cfg["engine"] == "mysql"
            else connect_postgres(conn_cfg)
        )
    except Exception as exc:
        print(f"Connection error: {exc}", file=sys.stderr)
        return 1

    try:
        with conn.cursor() as cur:
            cur.execute(sql)

            if args.mode == "query":
                rows = list(cur.fetchall())
                columns = [item[0] for item in cur.description] if cur.description else []
                print(f"Rows: {len(rows)}")
                if args.output == "table":
                    print_table(columns, rows)
                elif args.output == "json":
                    print_json(columns, rows)
                else:
                    print_csv(columns, rows)
                conn.rollback()
                print("Transaction rolled back (read-only mode).")
            else:
                affected = cur.rowcount
                conn.commit()
                print(f"Execute success. Affected rows: {affected}")
    except Exception as exc:
        conn.rollback()
        print(f"SQL execution error: {exc}", file=sys.stderr)
        return 1
    finally:
        conn.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
