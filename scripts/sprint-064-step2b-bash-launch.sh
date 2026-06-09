#!/usr/bin/env bash
# Sprint 064 / S-Auto-9 / M-Auto-2 — OQ-S62.3 diagnostic Step 2b
#
# Hypothesis (from Step 2a):
#   The 2-5 min kill is autoloop-specific (Step 2a pure-python idle probe
#   survived 900s+). Sole differentiator at launch is NI=5 vs NI=0.
#   The NI=5 on the autoloop traces to zsh's default BG_NICE option, which
#   automatically applies `nice +5` to backgrounded (`&`) jobs.
#
# Test:
#   Launch autoloop from THIS bash script (bash has no BG_NICE). If the
#   autoloop boots with NI=0 AND survives past 5 min (or further, ideally
#   reaches Step 9 tier_evaluator), the BG_NICE hypothesis is confirmed.
#   If it boots NI=0 but still dies 2-5 min → BG_NICE was a red herring;
#   re-investigate memory / java-heap.
#
# Usage: bash /Users/caoruixin/projects/csagent-latest/scripts/sprint-064-step2b-bash-launch.sh
# Logs:  /tmp/sprint-064-step2b.*

set -u
SCRATCH=/tmp/sprint-064-step2b

echo "===> Sprint 064 Step 2b: bash-launched autoloop (BG_NICE hypothesis test) <==="

# --- 1) Stop the still-alive Step 2a probe (control evidence preserved in /tmp) ---
if [ -f /tmp/sprint-064-step2a.pid ]; then
    OLD_PROBE_PID=$(cat /tmp/sprint-064-step2a.pid)
    if kill -0 $OLD_PROBE_PID 2>/dev/null; then
        echo "[step2b] stopping still-alive step2a probe pid=$OLD_PROBE_PID (was control survivor)"
        kill -TERM $OLD_PROBE_PID 2>/dev/null || true
        sleep 2
        kill -KILL $OLD_PROBE_PID 2>/dev/null || true
    fi
fi
pkill -f "autoloop run" 2>/dev/null || true
sleep 2

# --- 2) Verify zsh BG_NICE setting (compare to what user's interactive shell did) ---
echo ""
echo "----- zsh BG_NICE status (interactive shell default) -----"
zsh -i -c 'setopt | grep -i nice' 2>&1 || true
echo "(BG_NICE = backgrounded jobs get nice +5; expected default-on in zsh)"

echo ""
echo "----- bash nice baseline (this script's parent) -----"
ps -o pri,nice,pid,user,command -p $$

# --- 3) Clean exp branches (autoloop creates exp-N on each run; clean up orphans) ---
echo ""
echo "----- pre-launch git cleanup -----"
cd /Users/caoruixin/projects/csagent-latest || exit 1
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "auto-loop-branch" ]; then
    echo "[step2b] switching from $CURRENT_BRANCH to auto-loop-branch"
    git switch auto-loop-branch
fi
for b in $(git branch --list "autoloop/exp-*" | sed 's/^[ *]*//'); do
    echo "[step2b] deleting orphan branch $b"
    git branch -D "$b" 2>&1 || true
done

# --- 4) Clear scratch + launch autoloop from THIS bash context ---
rm -f ${SCRATCH}.*
date "+%Y-%m-%d %H:%M:%S" > ${SCRATCH}-launch.ts
date +%s > ${SCRATCH}-launch.epoch

cd /Users/caoruixin/projects/csagent-latest/autoloop || exit 1
echo ""
echo "===> launching: python -u -m autoloop run --experiments 15 --auto-reboot"
echo "     (from bash; bash has no BG_NICE; expecting NI=0)"
echo ""

# nohup + & + disown — same pattern as Step 1, but parent is bash not zsh
nohup python -u -m autoloop run --experiments 15 --auto-reboot \
    > ${SCRATCH}.stdout.log 2> ${SCRATCH}.stderr.log &
AUTOLOOP_PID=$!
echo $AUTOLOOP_PID > ${SCRATCH}.pid
disown $AUTOLOOP_PID 2>/dev/null || true

echo "===> AUTOLOOP PID=$AUTOLOOP_PID launched at $(cat ${SCRATCH}-launch.ts) <==="

# Give it a beat for ps to settle
sleep 1

echo ""
echo "----- ps at launch (KEY: NI column) -----"
ps -o pri,nice,pid,ppid,user,command -p $AUTOLOOP_PID | tee ${SCRATCH}-ps-launch.log

NI_VALUE=$(ps -o nice= -p $AUTOLOOP_PID | tr -d ' ')
echo ""
if [ "$NI_VALUE" = "0" ]; then
    echo "✓ NI=$NI_VALUE — bash wrapper succeeded in keeping nice at 0"
elif [ "$NI_VALUE" = "5" ]; then
    echo "✗ NI=$NI_VALUE — still demoted even from bash; BG_NICE hypothesis FALSIFIED"
else
    echo "? NI=$NI_VALUE — unexpected; investigate"
fi

# --- 5) Watch for death ---
echo ""
echo "===> Watching for death (10s poll). Ctrl-C OK — death is file-based. <==="
echo "===> Expecting: if BG_NICE hypothesis holds, autoloop survives past 5 min."
echo "===> Killing manually after 15 min is FINE: pkill -f 'autoloop run'"
echo ""

TICKS=0
while kill -0 $AUTOLOOP_PID 2>/dev/null; do
    sleep 10
    TICKS=$((TICKS + 1))
    if [ $((TICKS % 6)) -eq 0 ]; then
        ELAPSED_NOW=$(( $(date +%s) - $(cat ${SCRATCH}-launch.epoch) ))
        LAST_STDOUT=$(tail -1 ${SCRATCH}.stdout.log 2>/dev/null || echo "(no stdout yet)")
        echo "  [watcher] still alive at ${ELAPSED_NOW}s — last stdout: ${LAST_STDOUT}"
    fi
done

date "+%Y-%m-%d %H:%M:%S" > ${SCRATCH}-death.ts
date +%s > ${SCRATCH}-death.epoch
ELAPSED=$(( $(cat ${SCRATCH}-death.epoch) - $(cat ${SCRATCH}-launch.epoch) ))

echo ""
echo "===> AUTOLOOP DEAD: PID=$AUTOLOOP_PID at $(cat ${SCRATCH}-death.ts) (elapsed=${ELAPSED}s) <==="
echo ""

echo "----- stdout tail (last 40 lines) -----"
tail -40 ${SCRATCH}.stdout.log

echo ""
echo "----- stderr tail (last 20 lines) -----"
tail -20 ${SCRATCH}.stderr.log

echo ""
echo "----- experiments.jsonl row count -----"
wc -l /Users/caoruixin/projects/csagent-latest/autoloop/results/experiments.jsonl

echo ""
echo "===> DONE — paste elapsed + last stdout + NI verdict back to Claude <==="
