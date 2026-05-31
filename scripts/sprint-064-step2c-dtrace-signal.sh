#!/usr/bin/env bash
# Sprint 064 / S-Auto-9 / M-Auto-2 — OQ-S62.3 diagnostic Step 2c (Hypothesis 3)
#
# Established by Step 2a/2b:
#   - kill is autoloop-workload-specific (idle python survives 900s+)
#   - NOT nice-based (dies at NI=0)
#   - invisible in RunningBoard/jetsam logs for the specific PID
#   - takes the bash parent down too
#   - fires ~2-3 min after [run] starting, during iter-1 work (LLM/git/mvn)
#
# This step uses dtrace proc:::signal-send to capture WHO sends WHAT signal
# to the autoloop python (and its children/parent). dtrace requires sudo.
#
# Captures every SIGHUP(1)/SIGABRT(6)/SIGKILL(9)/SIGTERM(15)/SIGSEGV(11) send
# system-wide with sender execname+pid+uid and target fname+pid, to
# /tmp/sprint-064-step2c-dtrace.log. After the autoloop dies, we grep that
# log for the autoloop PID + bash PID to see the killer.
#
# Usage: bash /Users/caoruixin/projects/csagent-latest/scripts/sprint-064-step2c-dtrace-signal.sh
#   (will prompt for sudo password for dtrace)
# Logs:  /tmp/sprint-064-step2c.*

set -u
SCRATCH=/tmp/sprint-064-step2c
DTRACE_LOG=${SCRATCH}-dtrace.log

echo "===> Sprint 064 Step 2c: dtrace signal-send capture (Hypothesis 3) <==="
echo ""

# --- 0) Prime sudo up front (so the password prompt isn't mid-run) ---
echo "----- priming sudo (dtrace needs root) -----"
sudo -v || { echo "sudo failed; aborting"; exit 1; }

# --- 1) Cleanup prior runs ---
pkill -f "autoloop run" 2>/dev/null || true
sudo pkill dtrace 2>/dev/null || true
sleep 2
rm -f ${SCRATCH}.* ${DTRACE_LOG}

# --- 2) Git cleanup ---
cd /Users/caoruixin/projects/csagent-latest || exit 1
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "auto-loop-branch" ]; then
    echo "[step2c] switching from $CURRENT_BRANCH to auto-loop-branch"
    git switch auto-loop-branch
fi
for b in $(git branch --list "autoloop/exp-*" | sed 's/^[ *]*//'); do
    git branch -D "$b" 2>&1 || true
done

# --- 3) Start dtrace in background BEFORE launching autoloop ---
# proc:::signal-send args: args[1] = psinfo_t* (target), args[2] = signal number
# built-ins execname/pid/uid = the SENDER
echo ""
echo "----- starting dtrace signal-send capture -----"
sudo dtrace -q -n '
proc:::signal-send
/ args[2] == 1 || args[2] == 6 || args[2] == 9 || args[2] == 11 || args[2] == 15 /
{
    printf("%Y sig=%d SENDER[execname=%s pid=%d uid=%d ppid=%d] -> TARGET[fname=%s pid=%d]\n",
        walltimestamp, args[2], execname, pid, uid, ppid,
        args[1]->pr_fname, args[1]->pr_pid);
}
' > ${DTRACE_LOG} 2>${SCRATCH}-dtrace.err &
DTRACE_SHELL_PID=$!
# Give dtrace a moment to compile + attach
sleep 5
echo "[step2c] dtrace running (wrapper pid=$DTRACE_SHELL_PID); log=${DTRACE_LOG}"
if ! grep -q "" ${DTRACE_LOG} 2>/dev/null && [ -s ${SCRATCH}-dtrace.err ]; then
    echo "[step2c] dtrace stderr:"; cat ${SCRATCH}-dtrace.err
fi

# --- 4) Launch autoloop from bash (NI=0) ---
date "+%Y-%m-%d %H:%M:%S" > ${SCRATCH}-launch.ts
date +%s > ${SCRATCH}-launch.epoch
cd /Users/caoruixin/projects/csagent-latest/autoloop || exit 1

echo ""
echo "===> launching autoloop (experiments=15, auto-reboot) <==="
nohup python -u -m autoloop run --experiments 15 --auto-reboot \
    > ${SCRATCH}.stdout.log 2> ${SCRATCH}.stderr.log &
AUTOLOOP_PID=$!
echo $AUTOLOOP_PID > ${SCRATCH}.pid
disown $AUTOLOOP_PID 2>/dev/null || true
BASH_PID=$$
echo "===> AUTOLOOP PID=$AUTOLOOP_PID (bash parent PID=$BASH_PID) at $(cat ${SCRATCH}-launch.ts) <==="
echo "$AUTOLOOP_PID $BASH_PID" > ${SCRATCH}-pids.txt

sleep 1
ps -o pri,nice,pid,ppid,user,command -p $AUTOLOOP_PID | tee ${SCRATCH}-ps-launch.log

# --- 5) Watch for death ---
echo ""
echo "===> Watching for death (10s poll). Do NOT Ctrl-C (would kill this bash + dtrace). <==="
echo "===> If it survives 12 min, this script auto-stops it + dtrace. <==="
echo ""

TICKS=0
MAX_TICKS=72   # 72 * 10s = 12 min cap
while kill -0 $AUTOLOOP_PID 2>/dev/null; do
    sleep 10
    TICKS=$((TICKS + 1))
    if [ $((TICKS % 6)) -eq 0 ]; then
        ELAPSED_NOW=$(( $(date +%s) - $(cat ${SCRATCH}-launch.epoch) ))
        LAST_STDOUT=$(tail -1 ${SCRATCH}.stdout.log 2>/dev/null || echo "(none)")
        echo "  [watcher] alive at ${ELAPSED_NOW}s — last stdout: ${LAST_STDOUT}"
    fi
    if [ $TICKS -ge $MAX_TICKS ]; then
        echo "  [watcher] 12 min cap reached — stopping autoloop manually"
        kill -TERM $AUTOLOOP_PID 2>/dev/null || true
        break
    fi
done

date "+%Y-%m-%d %H:%M:%S" > ${SCRATCH}-death.ts
date +%s > ${SCRATCH}-death.epoch
ELAPSED=$(( $(cat ${SCRATCH}-death.epoch) - $(cat ${SCRATCH}-launch.epoch) ))
echo ""
echo "===> AUTOLOOP gone: PID=$AUTOLOOP_PID at $(cat ${SCRATCH}-death.ts) (elapsed=${ELAPSED}s) <==="

# --- 6) Stop dtrace + flush ---
sleep 2
sudo pkill dtrace 2>/dev/null || true
sleep 2

echo ""
echo "============================================================"
echo "DTRACE SIGNAL-SEND LOG — entries targeting our PIDs"
echo "============================================================"
AUTOLOOP_PID=$(cat ${SCRATCH}.pid)
echo "(autoloop pid=$AUTOLOOP_PID, bash pid=$BASH_PID)"
echo ""
echo "----- signals TARGETING autoloop pid=$AUTOLOOP_PID -----"
grep "pid=$AUTOLOOP_PID\]" ${DTRACE_LOG} 2>/dev/null || echo "(none — autoloop PID not a signal target)"
echo ""
echo "----- signals TARGETING bash pid=$BASH_PID -----"
grep "pid=$BASH_PID\]" ${DTRACE_LOG} 2>/dev/null || echo "(none)"
echo ""
echo "----- ALL SIGKILL(9)/SIGTERM(15) in capture window -----"
grep -E "sig=9 |sig=15 " ${DTRACE_LOG} 2>/dev/null | tail -40 || echo "(none captured)"
echo ""
echo "----- total dtrace lines captured -----"
wc -l ${DTRACE_LOG} 2>/dev/null
echo ""
echo "----- autoloop stdout tail -----"
tail -15 ${SCRATCH}.stdout.log
echo ""
echo "===> DONE — paste the 'TARGETING autoloop' + 'ALL SIGKILL/SIGTERM' sections back to Claude <==="
