#!/usr/bin/env python3
"""Launch autoloop overnight as a fully-detached POSIX session.

Doesn't use screen (macOS built-in screen 4.00.03 has reliability issues
in our environment per S-Auto-8 OQ). Uses subprocess.Popen with
start_new_session=True (calls setsid() in child before exec) and
wraps with `caffeinate -d -i -s -t 32400` to prevent macOS sleep.

The launched python autoloop becomes its own POSIX session leader
with PPID=1 (re-parented to launchd/init when this launcher exits).
"""
import os
import subprocess
import sys
from pathlib import Path

REPO = Path("/Users/caoruixin/projects/csagent-latest")
LOG = REPO / "autoloop-overnight.log"
N_EXPERIMENTS = sys.argv[1] if len(sys.argv) > 1 else "15"

# Open the log file before fork so the child inherits the fd
log_f = open(LOG, "w")
log_f.write(f"[launcher] START pid={os.getpid()} experiments={N_EXPERIMENTS}\n")
log_f.flush()

# Spawn: caffeinate wrapping python -u -m autoloop
# caffeinate -d/-i/-s holds power assertions while child runs
cmd = [
    "/usr/bin/caffeinate",
    "-d", "-i", "-s",
    "-t", "32400",
    "python", "-u", "-m", "autoloop", "run",
    "--experiments", N_EXPERIMENTS,
]

p = subprocess.Popen(
    cmd,
    cwd=str(REPO),
    stdout=log_f,
    stderr=subprocess.STDOUT,
    stdin=subprocess.DEVNULL,
    start_new_session=True,  # POSIX setsid() in child
    close_fds=True,
)

# Print PID + exit launcher cleanly; the child detaches and runs alone
print(f"LAUNCHED autoloop pid={p.pid} caffeinate-wrapped log={LOG}")
print(f"Monitor: tail -f {LOG}")
print(f"Check rows: wc -l {REPO}/autoloop/results/experiments.jsonl")
print(f"Kill if needed: kill {p.pid}")
log_f.close()
