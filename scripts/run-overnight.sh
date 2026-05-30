#!/usr/bin/env bash
# Bulletproof overnight runner for autoloop.
# Invoked via: screen -dmS autoloop-overnight bash /Users/caoruixin/projects/csagent-latest/scripts/run-overnight.sh
# Logs to: /Users/caoruixin/projects/csagent-latest/autoloop-overnight.log
# Captures everything (stdout + stderr) with timestamps.
#
# OQ-S62.3 (2026-05-30): prior overnight attempts died after ~2-5 min
# because macOS system sleep terminated the screen session
# (kIOMessageSystemWillSleep visible in pmset log). Fix: wrap the
# autoloop invocation with `caffeinate -d -i -s` which prevents
# idle / display / system sleep for the entire run.
# -d: prevent display sleep
# -i: prevent idle sleep
# -s: prevent system sleep (AC power only)
# -t 32400: 9-hour assertion (covers 5-9h overnight)

cd /Users/caoruixin/projects/csagent-latest || exit 1
LOG=/Users/caoruixin/projects/csagent-latest/autoloop-overnight.log
echo "[wrapper] START $(date -u +%Y-%m-%dT%H:%M:%SZ) pid=$$" > "$LOG"
echo "[wrapper] cwd=$(pwd)" >> "$LOG"
echo "[wrapper] python=$(which python) $(python --version 2>&1)" >> "$LOG"
echo "[wrapper] launching: caffeinate -d -i -s -t 32400 python -u -m autoloop run --experiments 15" >> "$LOG"
caffeinate -d -i -s -t 32400 python -u -m autoloop run --experiments 15 >> "$LOG" 2>&1
RC=$?
echo "[wrapper] DONE $(date -u +%Y-%m-%dT%H:%M:%SZ) rc=$RC" >> "$LOG"
