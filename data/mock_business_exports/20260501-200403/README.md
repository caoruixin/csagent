# Mock Business Data Export

Generated at: 2026-05-01T12:04:03.899344+00:00

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
psql postgresql://postgres:***@localhost:5432/csagent -c "select case_id, use_case_id, contact_email, ad_id, queue_name from mock_cases order by created_at desc limit 20;"
```
