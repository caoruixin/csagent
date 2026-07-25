# csagent live E2E suite

This suite targets already-running services. It never starts, stops, or restarts
the backend, UI, PostgreSQL, or Redis.

## API checks

```bash
python3 -m venv e2e/.venv
e2e/.venv/bin/pip install -r e2e/requirements.txt
e2e/.venv/bin/pytest -c e2e/pytest.ini
```

The HTTP client always sets `trust_env=False`, so macOS proxy variables cannot
hijack `localhost`. Override the endpoint only when needed:

```bash
CSAGENT_API_URL=http://localhost:8080 e2e/.venv/bin/pytest -c e2e/pytest.ini
```

## Browser checks

```bash
npm --prefix e2e install
npm --prefix e2e exec playwright install chromium
npm --prefix e2e run test:ui
```

Screenshots and request/response/trace JSON are written to `e2e/artifacts/`.
If the runner cannot reach the user-managed local services, tests are marked
`UNVERIFIED`/skipped rather than pretending they passed.
