#!/usr/bin/env python3
"""Sprint 064 / S-Auto-9 — reproduce applier._spawn_spring + _health_probe
in isolation (no LLM, no git, no exp branch), to find why the in-loop health
probe times out at 120s while a direct mvn boot is healthy in 6s.

Mimics applier.py EXACTLY:
  - _find_free_port(): bind 0, read back, close
  - _spawn_spring(): subprocess.Popen(mvn..., stdout=PIPE, stderr=STDOUT,
                     env=os.environ.copy(), start_new_session=True)  [POST-FIX]
  - _health_probe(): poll http://127.0.0.1:<port>/actuator/health every 2s,
                     WITHOUT reading proc.stdout (the autoloop never drains it)

Prints time-to-healthy or timeout, then drains the buffered mvn output so we
can see what mvn actually logged (and whether the pipe was full).
"""
import os
import socket
import subprocess
import sys
import time
import urllib.request

REPO = "/Users/caoruixin/projects/csagent-latest"
TIMEOUT = 120
INTERVAL = 2.0


def find_free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def probe_up(url: str) -> bool:
    try:
        req = urllib.request.Request(url)
        with urllib.request.urlopen(req, timeout=5.0) as resp:
            if resp.status != 200:
                return False
            data = resp.read().decode("utf-8", errors="replace")
            return '"status":"UP"' in data
    except Exception:
        return False


def main() -> int:
    port = find_free_port()
    url = f"http://127.0.0.1:{port}/actuator/health"
    print(f"[repro] free port: {port}")
    print(f"[repro] health url: {url}")

    cmd = [
        "mvn", "-q", "-pl", "server", "spring-boot:run",
        f"-Dspring-boot.run.arguments=--server.port={port}",
    ]
    print(f"[repro] spawning EXACTLY like applier (PIPE undrained, "
          f"start_new_session=True): {' '.join(cmd)}")
    t0 = time.monotonic()
    proc = subprocess.Popen(
        cmd,
        cwd=REPO,
        env=os.environ.copy(),
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        start_new_session=True,
    )
    print(f"[repro] mvn pid={proc.pid}")

    healthy_at = None
    deadline = time.monotonic() + TIMEOUT
    while time.monotonic() < deadline:
        if proc.poll() is not None:
            print(f"[repro] !! mvn exited prematurely rc={proc.returncode} "
                  f"at {time.monotonic()-t0:.1f}s")
            break
        if probe_up(url):
            healthy_at = time.monotonic() - t0
            print(f"[repro] *** HEALTHY at {healthy_at:.1f}s ***")
            break
        elapsed = time.monotonic() - t0
        print(f"\r[repro] {elapsed:.0f}s not-UP yet...", end="", flush=True)
        time.sleep(INTERVAL)
    print("")

    if healthy_at is None:
        print(f"[repro] === TIMEOUT: not healthy within {TIMEOUT}s "
              f"(matches the in-loop SpringStartupTimeoutError) ===")
    else:
        print(f"[repro] === healthy in {healthy_at:.1f}s "
              f"(does NOT reproduce the in-loop timeout) ===")

    # Teardown FIRST (kill its session group), THEN drain whatever was buffered.
    print(f"[repro] tearing down mvn pid={proc.pid} (own session/pgrp)")
    try:
        os.killpg(os.getpgid(proc.pid), 15)
    except Exception as e:
        print(f"[repro] killpg SIGTERM failed: {e}")
    time.sleep(3)
    try:
        os.killpg(os.getpgid(proc.pid), 9)
    except Exception:
        pass

    # Drain the buffered stdout now that mvn is dead (read won't block).
    try:
        buffered = proc.stdout.read() if proc.stdout else b""
    except Exception as e:
        buffered = b""
        print(f"[repro] drain failed: {e}")
    n = len(buffered)
    print(f"[repro] buffered mvn stdout bytes: {n} ({n/1024:.1f} KB)")
    print(f"[repro] pipe buffer was {'FULL (>=64KB) -> mvn was BLOCKED' if n >= 65536 else 'not full'}")
    print("[repro] ---- last 3000 bytes of buffered mvn output ----")
    tail = buffered[-3000:].decode("utf-8", errors="replace")
    print(tail)
    return 0


if __name__ == "__main__":
    sys.exit(main())
