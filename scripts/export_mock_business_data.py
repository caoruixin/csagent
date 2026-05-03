#!/usr/bin/env python3
"""
Export local mock business data as a reviewable graph.

This script intentionally excludes evaluation datasets. It combines:
- file-backed mock fixtures under server/src/main/resources/mock
- PostgreSQL mock business tables such as mock_cases and mock_handover_log
- optionally, runtime tables related to mock case/handover sessions

It uses the local psql CLI instead of a Python PostgreSQL driver.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlsplit, urlunsplit


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_FIXTURE_ROOT = ROOT / "server" / "src" / "main" / "resources" / "mock"
DEFAULT_EXPORT_ROOT = ROOT / "data" / "mock_business_exports"

MOCK_TABLES = ["mock_cases", "mock_handover_log"]
RUNTIME_TABLES = [
    "bot_sessions",
    "bot_turns",
    "bot_events",
    "session_outcomes",
    "llm_call_log",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Export mock business data, schema, and relationships for review."
    )
    parser.add_argument(
        "--db-url",
        default=default_db_url(),
        help="PostgreSQL URL. Defaults to env DATABASE_URL, then local csagent defaults.",
    )
    parser.add_argument(
        "--fixture-root",
        default=str(DEFAULT_FIXTURE_ROOT),
        help="Mock fixture directory containing accounts/listings/etc.",
    )
    parser.add_argument(
        "--output",
        default=None,
        help="Output directory. Defaults to data/mock_business_exports/<timestamp>.",
    )
    parser.add_argument(
        "--runtime",
        choices=["none", "related", "all"],
        default="related",
        help=(
            "Runtime table export scope. 'related' exports sessions referenced by "
            "mock cases/handover logs; 'all' exports all runtime rows; 'none' skips them."
        ),
    )
    parser.add_argument(
        "--pretty",
        action="store_true",
        help="Pretty-print JSON output. Larger, but easier to review in git or an editor.",
    )
    return parser.parse_args()


def default_db_url() -> str:
    if os.getenv("DATABASE_URL"):
        return os.environ["DATABASE_URL"]

    host = os.getenv("DB_HOST", "localhost")
    port = os.getenv("DB_PORT", "5432")
    name = os.getenv("DB_NAME", "csagent")
    user = os.getenv("DB_USERNAME", "postgres")
    password = os.getenv("DB_PASSWORD", "postgres")
    return f"postgresql://{user}:{password}@{host}:{port}/{name}"


def redact_db_url(db_url: str) -> str:
    parsed = urlsplit(db_url)
    if not parsed.password:
        return db_url
    username = parsed.username or ""
    host = parsed.hostname or ""
    port = f":{parsed.port}" if parsed.port else ""
    netloc = f"{username}:***@{host}{port}" if username else f"{host}{port}"
    return urlunsplit((parsed.scheme, netloc, parsed.path, parsed.query, parsed.fragment))


def run_psql(db_url: str, sql: str) -> str:
    cmd = [
        "psql",
        db_url,
        "-X",
        "--set",
        "ON_ERROR_STOP=1",
        "--tuples-only",
        "--no-align",
        "--command",
        sql,
    ]
    result = subprocess.run(cmd, text=True, capture_output=True, check=False)
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or result.stdout.strip())
    return result.stdout.strip()


def psql_json(db_url: str, sql: str):
    output = run_psql(db_url, sql)
    if not output:
        return None
    return json.loads(output)


def table_exists(db_url: str, table: str) -> bool:
    sql = """
        select exists (
            select 1
            from information_schema.tables
            where table_schema = 'public'
              and table_name = %s
        )::text;
    """ % sql_literal(table)
    return run_psql(db_url, sql) == "true"


def sql_literal(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def load_fixtures(fixture_root: Path) -> dict:
    fixtures: dict[str, dict[str, object]] = {}
    if not fixture_root.exists():
        return fixtures

    for category_dir in sorted(p for p in fixture_root.iterdir() if p.is_dir()):
        category = category_dir.name
        fixtures[category] = {}
        for json_file in sorted(category_dir.glob("*.json")):
            with json_file.open("r", encoding="utf-8") as handle:
                fixtures[category][json_file.stem] = json.load(handle)

    return fixtures


def copy_fixtures(fixture_root: Path, output_dir: Path) -> None:
    target = output_dir / "fixtures"
    if target.exists():
        shutil.rmtree(target)
    if fixture_root.exists():
        shutil.copytree(fixture_root, target)


def export_schema(db_url: str, tables: list[str]) -> dict:
    existing = [table for table in tables if table_exists(db_url, table)]
    if not existing:
        return {"tables": {}, "foreign_keys": []}

    table_list = ", ".join(sql_literal(table) for table in existing)
    columns_sql = f"""
        select coalesce(jsonb_object_agg(table_name, columns order by table_name), '{{}}'::jsonb)::text
        from (
            select
                table_name,
                jsonb_agg(
                    jsonb_build_object(
                        'column_name', column_name,
                        'data_type', data_type,
                        'udt_name', udt_name,
                        'is_nullable', is_nullable,
                        'column_default', column_default
                    )
                    order by ordinal_position
                ) as columns
            from information_schema.columns
            where table_schema = 'public'
              and table_name in ({table_list})
            group by table_name
        ) c;
    """
    fk_sql = f"""
        select coalesce(jsonb_agg(row_to_json(fks) order by source_table, source_column), '[]'::jsonb)::text
        from (
            select
                tc.table_name as source_table,
                kcu.column_name as source_column,
                ccu.table_name as target_table,
                ccu.column_name as target_column,
                tc.constraint_name
            from information_schema.table_constraints tc
            join information_schema.key_column_usage kcu
              on tc.constraint_name = kcu.constraint_name
             and tc.table_schema = kcu.table_schema
            join information_schema.constraint_column_usage ccu
              on ccu.constraint_name = tc.constraint_name
             and ccu.table_schema = tc.table_schema
            where tc.constraint_type = 'FOREIGN KEY'
              and tc.table_schema = 'public'
              and tc.table_name in ({table_list})
        ) fks;
    """
    return {
        "tables": psql_json(db_url, columns_sql) or {},
        "foreign_keys": psql_json(db_url, fk_sql) or [],
    }


def export_table(db_url: str, table: str, where_sql: str | None = None) -> list[dict]:
    if not table_exists(db_url, table):
        return []
    where_clause = f" where {where_sql}" if where_sql else ""
    sql = f"""
        select coalesce(jsonb_agg(to_jsonb(t)), '[]'::jsonb)::text
        from (select * from {table}{where_clause}) t;
    """
    return psql_json(db_url, sql) or []


def get_related_session_ids(mock_data: dict[str, list[dict]]) -> set[str]:
    session_ids: set[str] = set()
    for row in mock_data.get("mock_cases", []):
        if row.get("session_id"):
            session_ids.add(str(row["session_id"]))
    for row in mock_data.get("mock_handover_log", []):
        if row.get("session_id"):
            session_ids.add(str(row["session_id"]))
    return session_ids


def export_postgres_data(db_url: str, runtime_scope: str) -> dict[str, list[dict]]:
    data = {table: export_table(db_url, table) for table in MOCK_TABLES}

    if runtime_scope == "none":
        return data

    related_session_ids = get_related_session_ids(data)
    session_filter = None
    if runtime_scope == "related":
        if not related_session_ids:
            for table in RUNTIME_TABLES:
                data[table] = []
            return data
        quoted_ids = ", ".join(sql_literal(session_id) for session_id in sorted(related_session_ids))
        session_filter = f"session_id in ({quoted_ids})"

    for table in RUNTIME_TABLES:
        data[table] = export_table(db_url, table, session_filter)
    return data


def build_relationships(fixtures: dict, postgres_data: dict[str, list[dict]]) -> dict:
    relationships = {
        "accounts_by_email": {},
        "listings_by_ad_id": {},
        "cases_by_case_id": {},
        "cases_by_contact_email": defaultdict(list),
        "cases_by_ad_id": defaultdict(list),
        "cases_by_session_id": defaultdict(list),
        "handover_logs_by_session_id": defaultdict(list),
        "sessions_by_session_id": {},
    }

    for key, account in fixtures.get("accounts", {}).items():
        email = account.get("email")
        if email:
            relationships["accounts_by_email"][email] = {
                "fixture_key": key,
                "user_id": account.get("user_id"),
                "account_status": account.get("account_status"),
            }

    for key, listing in fixtures.get("listings", {}).items():
        ad_id = listing.get("ad_id")
        if ad_id:
            relationships["listings_by_ad_id"][ad_id] = {
                "fixture_key": key,
                "title": listing.get("title"),
                "status": listing.get("status"),
                "seller_email": listing.get("seller_email"),
                "removal_reason": listing.get("removal_reason"),
            }

    for case in postgres_data.get("mock_cases", []):
        case_id = case.get("case_id")
        if case_id:
            relationships["cases_by_case_id"][case_id] = {
                "use_case_id": case.get("use_case_id"),
                "contact_email": case.get("contact_email"),
                "ad_id": case.get("ad_id"),
                "session_id": case.get("session_id"),
                "queue_name": case.get("queue_name"),
            }
        if case.get("contact_email"):
            relationships["cases_by_contact_email"][case["contact_email"]].append(case_id)
        if case.get("ad_id"):
            relationships["cases_by_ad_id"][case["ad_id"]].append(case_id)
        if case.get("session_id"):
            relationships["cases_by_session_id"][case["session_id"]].append(case_id)

    for log in postgres_data.get("mock_handover_log", []):
        if log.get("session_id"):
            relationships["handover_logs_by_session_id"][log["session_id"]].append(log.get("log_id"))

    for session in postgres_data.get("bot_sessions", []):
        session_id = session.get("session_id")
        if session_id:
            relationships["sessions_by_session_id"][session_id] = {
                "case_id": session.get("case_id"),
                "active_use_case": session.get("active_use_case"),
                "handling_state": session.get("handling_state"),
                "form_topic_subject": session.get("form_topic_subject"),
                "created_at": session.get("created_at"),
            }

    return normalize_defaultdicts(relationships)


def normalize_defaultdicts(value):
    if isinstance(value, defaultdict):
        return {key: normalize_defaultdicts(inner) for key, inner in value.items()}
    if isinstance(value, dict):
        return {key: normalize_defaultdicts(inner) for key, inner in value.items()}
    if isinstance(value, list):
        return [normalize_defaultdicts(item) for item in value]
    return value


def write_json(path: Path, data, pretty: bool) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        if pretty:
            json.dump(data, handle, indent=2, sort_keys=True)
        else:
            json.dump(data, handle, separators=(",", ":"))
        handle.write("\n")


def write_readme(output_dir: Path, manifest: dict) -> None:
    readme = f"""# Mock Business Data Export

Generated at: {manifest["generated_at"]}

This export intentionally excludes evaluation datasets under `data/eval_datasets`.

Files:
- `manifest.json` - export metadata and row counts
- `schema.json` - PostgreSQL table columns and foreign keys
- `fixtures.json` - file-backed mock accounts, listings, moderation reviews, and message moderation data
- `postgres_data.json` - exported PostgreSQL rows
- `relationships.json` - indexes that connect accounts, adverts, cases, handovers, and sessions
- `mock_business_snapshot.json` - all of the above in one JSON document
- `fixtures/` - copied source JSON fixtures

Primary relationships:
- `accounts.email` -> `listings.seller_email`
- `accounts.email` -> `mock_cases.contact_email`
- `listings.ad_id` -> `mock_cases.ad_id`
- `mock_cases.session_id` -> `bot_sessions.session_id`
- `mock_handover_log.session_id` -> `bot_sessions.session_id`

To inspect mocked adverts from the running backend:

```bash
curl http://localhost:8080/v1/demo/mock-data/listings
```

To inspect seeded mock cases directly:

```bash
psql {manifest["db_url"]} -c "select case_id, use_case_id, contact_email, ad_id, queue_name from mock_cases order by created_at desc limit 20;"
```
"""
    (output_dir / "README.md").write_text(readme, encoding="utf-8")


def build_manifest(
    output_dir: Path,
    db_url: str,
    fixture_root: Path,
    fixtures: dict,
    postgres_data: dict,
) -> dict:
    return {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "output_dir": str(output_dir),
        "db_url": redact_db_url(db_url),
        "fixture_root": str(fixture_root),
        "fixture_counts": {
            category: len(items)
            for category, items in sorted(fixtures.items())
        },
        "postgres_counts": {
            table: len(rows)
            for table, rows in sorted(postgres_data.items())
        },
        "excluded": [
            "data/eval_datasets",
            "eval",
            "eval_interactive",
        ],
    }


def main() -> int:
    args = parse_args()
    timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    output_dir = Path(args.output) if args.output else DEFAULT_EXPORT_ROOT / timestamp
    output_dir.mkdir(parents=True, exist_ok=True)

    fixture_root = Path(args.fixture_root)
    fixtures = load_fixtures(fixture_root)
    copy_fixtures(fixture_root, output_dir)

    postgres_data = export_postgres_data(args.db_url, args.runtime)
    schema = export_schema(args.db_url, MOCK_TABLES + RUNTIME_TABLES)
    relationships = build_relationships(fixtures, postgres_data)
    manifest = build_manifest(output_dir, args.db_url, fixture_root, fixtures, postgres_data)

    snapshot = {
        "manifest": manifest,
        "schema": schema,
        "fixtures": fixtures,
        "postgres_data": postgres_data,
        "relationships": relationships,
    }

    write_json(output_dir / "manifest.json", manifest, args.pretty)
    write_json(output_dir / "schema.json", schema, args.pretty)
    write_json(output_dir / "fixtures.json", fixtures, args.pretty)
    write_json(output_dir / "postgres_data.json", postgres_data, args.pretty)
    write_json(output_dir / "relationships.json", relationships, args.pretty)
    write_json(output_dir / "mock_business_snapshot.json", snapshot, args.pretty)
    write_readme(output_dir, manifest)

    print(f"Exported mock business data to: {output_dir}")
    print("Counts:")
    for category, count in manifest["fixture_counts"].items():
        print(f"  fixture:{category}: {count}")
    for table, count in manifest["postgres_counts"].items():
        print(f"  postgres:{table}: {count}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
