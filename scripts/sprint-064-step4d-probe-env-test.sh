#!/usr/bin/env bash
# Sprint 064 / S-Auto-9 / M-Auto-2 — health-probe env disambiguation (Step 4d)
#
# In-loop finding (step4 + spring-boot-<port>.log): Spring starts healthy in
# ~2.5s, but the autoloop's _health_probe (httpx) never connects (no request
# reaches the server for 120s -> SpringStartupTimeoutError). step4c's urllib
# probe connected fine. Hypothesis: an env proxy var that httpx honors for
# 127.0.0.1 but urllib bypasses for localhost.
#
# This runs in YOUR shell (the env the autoloop inherits) + the autoloop venv,
# and tests httpx vs urllib against a proper localhost server.
#
# Usage: bash /Users/caoruixin/projects/csagent-latest/scripts/sprint-064-step4d-probe-env-test.sh

set -u
cd /Users/caoruixin/projects/csagent-latest/autoloop || exit 1

echo "=== proxy env vars as this shell exports them ==="
env | grep -iE "proxy" || echo "(no *proxy* env vars in this shell)"
echo ""

echo "=== httpx vs urllib -> fresh localhost server, in autoloop venv ==="
uv run python - <<'PY'
import socket, threading, http.server, socketserver, time, urllib.request, os
import httpx

s = socket.socket(); s.bind(("127.0.0.1", 0)); port = s.getsockname()[1]; s.close()

class H(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        body = b'{"status":"UP"}'
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)
    def log_message(self, *a):
        pass

srv = socketserver.TCPServer(("127.0.0.1", port), H)
threading.Thread(target=srv.serve_forever, daemon=True).start()
time.sleep(0.5)
url = f"http://127.0.0.1:{port}/actuator/health"

proxy_env = {k: v for k, v in os.environ.items() if "proxy" in k.lower()}
print("proxy-related env httpx/urllib see:", proxy_env or "(none)")
print("test url:", url)

# EXACT replica of applier._probe_url_is_up httpx path
try:
    r = httpx.get(url, timeout=5.0)
    print(f"httpx(trust_env=default): status={r.status_code} body={r.text!r} -> UP? {r.json().get('status')=='UP'}")
except Exception as e:
    print(f"httpx(trust_env=default) FAILED: {type(e).__name__}: {e!r}")

# The proposed fix: httpx with trust_env=False (ignore env proxies for localhost)
try:
    with httpx.Client(trust_env=False) as c:
        r = c.get(url, timeout=5.0)
    print(f"httpx(trust_env=False): status={r.status_code} body={r.text!r} -> UP? {r.json().get('status')=='UP'}")
except Exception as e:
    print(f"httpx(trust_env=False) FAILED: {type(e).__name__}: {e!r}")

# urllib (what step4c used)
try:
    with urllib.request.urlopen(url, timeout=5.0) as resp:
        print(f"urllib: status={resp.status} body={resp.read().decode()!r}")
except Exception as e:
    print(f"urllib FAILED: {type(e).__name__}: {e!r}")
PY
echo ""
echo "=== INTERPRETATION ==="
echo "If httpx(default) FAILED but httpx(trust_env=False) + urllib succeed,"
echo "the health probe's httpx is being routed through a proxy for 127.0.0.1."
echo "Fix = make _probe_url_is_up use trust_env=False (localhost never needs a proxy)."
echo "===> DONE — paste this whole output back to Claude <==="
