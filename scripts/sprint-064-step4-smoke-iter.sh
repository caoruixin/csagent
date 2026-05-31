#!/usr/bin/env bash
# Sprint 064 / S-Auto-9 / M-Auto-2 — OQ-S62.3 fix verification (Step 4)
#
# Verifies the start_new_session=True fix in applier.py:_spawn_spring.
# Before the fix: autoloop killpg'd its own process group at Spring
# teardown (~2-3 min), so no iteration ever completed. After the fix:
# mvn+JVM are isolated in their own session; killpg targets only them;
# the autoloop survives teardown and completes the iteration.
#
# Success = autoloop SURVIVES past the historic 2-3 min kill window AND
# writes a new experiments.jsonl row. Gate #1 retirement = that row (or a
# subsequent one) carries non-null verdict.layer_results (reached Step 9
# tier_evaluator.evaluate).
#
# Usage: bash /Users/caoruixin/projects/csagent-latest/scripts/sprint-064-step4-smoke-iter.sh [N]
#   N = experiment count (default 1). Use 3 if iter-1 discards early and you
#       want more chances at a Step-9 verdict row.
# Logs:  /tmp/sprint-064-step4.*

set -u
SCRATCH=/tmp/sprint-064-step4
N_EXP="${1:-1}"
JSONL=/Users/caoruixin/projects/csagent-latest/autoloop/results/experiments.jsonl

echo "===> Sprint 064 Step 4: smoke iter (experiments=$N_EXP) — OQ-S62.3 fix verification <==="
echo ""

# --- Cleanup ---
pkill -f "autoloop run" 2>/dev/null || true
sleep 2
rm -f ${SCRATCH}.*

# --- Git cleanup ---
cd /Users/caoruixin/projects/csagent-latest || exit 1
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "auto-loop-branch" ]; then
    echo "[step4] switching from $CURRENT_BRANCH to auto-loop-branch"
    git switch auto-loop-branch
fi
for b in $(git branch --list "autoloop/exp-*" | sed 's/^[ *]*//'); do
    echo "[step4] deleting orphan branch $b"
    git branch -D "$b" 2>&1 || true
done

# --- Record baseline row count ---
BASELINE_ROWS=$(wc -l < "$JSONL" 2>/dev/null | tr -d ' ')
echo "[step4] experiments.jsonl baseline rows: $BASELINE_ROWS"

# --- Launch from bash (NI=0) ---
date "+%Y-%m-%d %H:%M:%S" > ${SCRATCH}-launch.ts
date +%s > ${SCRATCH}-launch.epoch
cd /Users/caoruixin/projects/csagent-latest/autoloop || exit 1

echo ""
echo "===> launching: python -u -m autoloop run --experiments $N_EXP --auto-reboot"
nohup python -u -m autoloop run --experiments "$N_EXP" --auto-reboot \
    > ${SCRATCH}.stdout.log 2> ${SCRATCH}.stderr.log &
AUTOLOOP_PID=$!
echo $AUTOLOOP_PID > ${SCRATCH}.pid
disown $AUTOLOOP_PID 2>/dev/null || true
echo "===> AUTOLOOP PID=$AUTOLOOP_PID at $(cat ${SCRATCH}-launch.ts) <==="
sleep 1
ps -o pri,nice,pid,ppid,user,command -p $AUTOLOOP_PID | tee ${SCRATCH}-ps-launch.log

# --- Watch: success is SURVIVAL past 2-3 min + a new row ---
echo ""
echo "===> Monitoring. Historic kill window was 2-5 min; surviving past that = fix works. <==="
echo "===> Full iter through Step 9 takes ~12-25 min. 35-min cap. Do NOT Ctrl-C. <==="
echo ""

TICKS=0
MAX_TICKS=300   # 300 * 10s = 50 min (room for up to ~3 iters; stops early on success)
PASSED_KILL_WINDOW=0
LAST_SEEN_ROWS=$BASELINE_ROWS
STEP9_SUCCESS=0
while true; do
    sleep 10
    TICKS=$((TICKS + 1))
    ELAPSED_NOW=$(( $(date +%s) - $(cat ${SCRATCH}-launch.epoch) ))
    NOW_ROWS=$(wc -l < "$JSONL" 2>/dev/null | tr -d ' ')

    ALIVE=0
    kill -0 $AUTOLOOP_PID 2>/dev/null && ALIVE=1

    # Milestone: survived past 300s (5 min) — beyond the historic kill window
    if [ "$PASSED_KILL_WINDOW" -eq 0 ] && [ "$ELAPSED_NOW" -ge 300 ]; then
        PASSED_KILL_WINDOW=1
        echo "  *** [${ELAPSED_NOW}s] SURVIVED past the 5-min historic kill window — killpg fix is working ***"
    fi

    # Heartbeat each 60s
    if [ $((TICKS % 6)) -eq 0 ]; then
        LAST=$(tail -1 ${SCRATCH}.stdout.log 2>/dev/null || echo "(none)")
        echo "  [watcher] ${ELAPSED_NOW}s alive=$ALIVE rows=$NOW_ROWS — last: ${LAST}"
    fi

    # New row written? Check if it reached Step 9 (non-null verdict.layer_results).
    if [ "$NOW_ROWS" -gt "$LAST_SEEN_ROWS" ]; then
        LAST_SEEN_ROWS=$NOW_ROWS
        echo ""
        echo "  *** [${ELAPSED_NOW}s] NEW experiments.jsonl row (now ${NOW_ROWS}) ***"
        HAS_VERDICT=$(tail -1 "$JSONL" | python3 -c '
import json, sys
try:
    row = json.loads(sys.stdin.read())
    v = row.get("verdict") or {}
    lr = v.get("layer_results")
    print("YES" if lr else "NO")
except Exception:
    print("NO")
' 2>/dev/null)
        DECISION=$(tail -1 "$JSONL" | python3 -c 'import json,sys; print(json.loads(sys.stdin.read()).get("decision"))' 2>/dev/null)
        echo "      row decision=$DECISION  reached_Step9(verdict.layer_results)=$HAS_VERDICT"
        if [ "$HAS_VERDICT" = "YES" ]; then
            STEP9_SUCCESS=1
            echo ""
            echo "  *** [${ELAPSED_NOW}s] STEP 9 REACHED — non-null verdict.layer_results. SUCCESS. ***"
            echo "  *** Stopping autoloop + Spring (goal met; no need to run remaining iters). ***"
            kill -TERM $AUTOLOOP_PID 2>/dev/null || true
            sleep 2
            kill -KILL $AUTOLOOP_PID 2>/dev/null || true
            pkill -f "spring-boot:run" 2>/dev/null || true
            break
        fi
    fi

    # Process exited?
    if [ "$ALIVE" -eq 0 ]; then
        echo ""
        echo "===> autoloop process exited at ${ELAPSED_NOW}s <==="
        break
    fi

    # Cap
    if [ $TICKS -ge $MAX_TICKS ]; then
        echo ""
        echo "===> 50-min cap reached; stopping autoloop <==="
        kill -TERM $AUTOLOOP_PID 2>/dev/null || true
        sleep 3
        kill -KILL $AUTOLOOP_PID 2>/dev/null || true
        pkill -f "spring-boot:run" 2>/dev/null || true
        break
    fi
done

date "+%Y-%m-%d %H:%M:%S" > ${SCRATCH}-end.ts
date +%s > ${SCRATCH}-end.epoch
ELAPSED=$(( $(cat ${SCRATCH}-end.epoch) - $(cat ${SCRATCH}-launch.epoch) ))
FINAL_ROWS=$(wc -l < "$JSONL" 2>/dev/null | tr -d ' ')

echo ""
echo "============================================================"
echo "STEP 4 RESULT"
echo "============================================================"
echo "elapsed: ${ELAPSED}s"
echo "survived past 5-min kill window (killpg fix): $( [ "$PASSED_KILL_WINDOW" -eq 1 ] && echo YES || echo NO )"
echo "reached Step 9 (non-null verdict.layer_results): $( [ "$STEP9_SUCCESS" -eq 1 ] && echo 'YES — GATE #1 RETIRED' || echo 'NO' )"
echo "rows: ${BASELINE_ROWS} -> ${FINAL_ROWS} (delta $(( FINAL_ROWS - BASELINE_ROWS )))"
echo ""
echo "----- stdout (full) -----"
cat ${SCRATCH}.stdout.log
echo ""
echo "----- stderr tail -----"
tail -20 ${SCRATCH}.stderr.log
echo ""
echo "----- last experiments.jsonl row: decision + verdict.layer_results -----"
tail -1 "$JSONL" | python3 -c '
import json, sys
row = json.loads(sys.stdin.read())
print("iteration_id:", row.get("iteration_id"))
print("decision:", row.get("decision"))
print("discard_reason:", row.get("discard_reason"))
print("error:", (row.get("error") or "")[:200])
v = row.get("verdict")
if v is None:
    print("verdict: NULL (did not reach Step 9 tier_evaluator)")
else:
    lr = v.get("layer_results")
    print("verdict.layer_results NULL?:", lr is None)
    if lr is not None:
        try:
            print("layer_results keys:", list(lr.keys()) if isinstance(lr, dict) else type(lr).__name__)
            print(json.dumps(lr, indent=2)[:1500])
        except Exception as e:
            print("layer_results repr:", repr(lr)[:1500])
' 2>&1
echo ""
echo "===> DONE — paste the STEP 4 RESULT block back to Claude <==="
