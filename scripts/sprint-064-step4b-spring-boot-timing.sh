#!/usr/bin/env bash
# Sprint 064 / S-Auto-9 / M-Auto-2 — Spring-health-timeout disambiguation (Step 4b)
#
# After the killpg fix (OQ-S62.3), the smoke iter survives teardown but the
# iteration errors with SpringStartupTimeoutError (Spring not healthy in 120s).
# This test disambiguates the cause by running the EXACT applier mvn command
# with stdout going to a FILE (no pipe-buffer deadlock):
#
#   - healthy in <120s, log > 64KB  -> applier's undrained subprocess.PIPE is
#       deadlocking mvn at ~64KB; fix = drain stdout (or DEVNULL/file)
#   - healthy in <120s, log < 64KB  -> 120s was just slightly short / flaky
#   - healthy only after >120s       -> genuinely slow cold boot; bump timeout
#   - never healthy / mvn exits      -> real boot failure; read the log
#
# Usage: bash /Users/caoruixin/projects/csagent-latest/scripts/sprint-064-step4b-spring-boot-timing.sh
# Logs:  /tmp/sprint-064-step4b.*

set -u
SCRATCH=/tmp/sprint-064-step4b
MVN_LOG=${SCRATCH}-mvn.log
REPO=/Users/caoruixin/projects/csagent-latest

echo "===> Sprint 064 Step 4b: Spring boot timing (file output, no pipe block) <==="

# --- Cleanup any prior spring-boot:run + free :8080 conflicts ---
pkill -f "spring-boot:run" 2>/dev/null || true
sleep 2
rm -f ${SCRATCH}.* ${MVN_LOG}

# --- Pick a free port (same mechanism as applier._find_free_port) ---
PORT=$(python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1",0)); print(s.getsockname()[1]); s.close()')
echo "[step4b] chosen alt port: $PORT"

cd "$REPO" || exit 1

# --- Spawn the EXACT applier command, stdout+stderr -> FILE ---
date +%s > ${SCRATCH}-launch.epoch
echo "[step4b] launching: mvn -q -pl server spring-boot:run -Dspring-boot.run.arguments=--server.port=$PORT"
nohup mvn -q -pl server spring-boot:run \
    "-Dspring-boot.run.arguments=--server.port=$PORT" \
    > "$MVN_LOG" 2>&1 &
MVN_PID=$!
echo $MVN_PID > ${SCRATCH}-mvn.pid
echo "[step4b] mvn pid=$MVN_PID; log=$MVN_LOG"

# --- Poll /actuator/health, timing to UP (cap 240s) ---
URL="http://127.0.0.1:$PORT/actuator/health"
DEADLINE=$(( $(date +%s) + 240 ))
HEALTHY_AT=""
while [ "$(date +%s)" -lt "$DEADLINE" ]; do
    # mvn still alive?
    if ! kill -0 $MVN_PID 2>/dev/null; then
        echo "[step4b] mvn exited prematurely; see log"
        break
    fi
    BODY=$(curl -sf --max-time 5 "$URL" 2>/dev/null || true)
    if echo "$BODY" | grep -q '"status":"UP"'; then
        HEALTHY_AT=$(( $(date +%s) - $(cat ${SCRATCH}-launch.epoch) ))
        break
    fi
    NOW_ELAPSED=$(( $(date +%s) - $(cat ${SCRATCH}-launch.epoch) ))
    LOG_BYTES=$(wc -c < "$MVN_LOG" 2>/dev/null | tr -d ' ')
    printf "\r  [poll] %ss elapsed, log=%s bytes, health not-UP yet   " "$NOW_ELAPSED" "$LOG_BYTES"
    sleep 3
done
echo ""

# --- Capture log size at health/timeout ---
LOG_BYTES=$(wc -c < "$MVN_LOG" 2>/dev/null | tr -d ' ')
PIPE_BUF_NOTE="(macOS default pipe buffer ≈ 65536 bytes / 64KB)"

echo ""
echo "============================================================"
echo "STEP 4b RESULT"
echo "============================================================"
if [ -n "$HEALTHY_AT" ]; then
    echo "RESULT: Spring became healthy at ${HEALTHY_AT}s (port $PORT)"
else
    echo "RESULT: Spring did NOT become healthy within the 240s cap (port $PORT)"
fi
echo "mvn log size at this point: ${LOG_BYTES} bytes  ${PIPE_BUF_NOTE}"
echo ""
if [ -n "$HEALTHY_AT" ] && [ "$LOG_BYTES" -gt 65536 ]; then
    echo ">>> DIAGNOSIS: log (${LOG_BYTES}B) EXCEEDS 64KB pipe buffer AND boot succeeded"
    echo ">>> to a FILE. The applier's undrained subprocess.PIPE would deadlock mvn"
    echo ">>> around 64KB -> Spring hangs -> 120s health timeout. PIPE-BLOCK CONFIRMED."
elif [ -n "$HEALTHY_AT" ] && [ "$HEALTHY_AT" -gt 110 ]; then
    echo ">>> DIAGNOSIS: boot took ${HEALTHY_AT}s, close to/over the 120s default."
    echo ">>> Slow cold boot; bumping health_probe_timeout would help (log under 64KB)."
elif [ -n "$HEALTHY_AT" ]; then
    echo ">>> DIAGNOSIS: boot healthy in ${HEALTHY_AT}s with log ${LOG_BYTES}B (< 64KB)."
    echo ">>> Neither pipe-block nor slow-boot clearly indicated; 120s timeout was"
    echo ">>> borderline/flaky. Inspect why the in-loop probe differs."
else
    echo ">>> DIAGNOSIS: Spring never healthy even to a file. Real boot failure —"
    echo ">>> read the log tail below."
fi

echo ""
echo "----- mvn log tail (last 40 lines) -----"
tail -40 "$MVN_LOG"

# --- Teardown: kill mvn + its java child + any spring-boot:run ---
echo ""
echo "[step4b] tearing down mvn pid=$MVN_PID + children"
pkill -TERM -P $MVN_PID 2>/dev/null || true
kill -TERM $MVN_PID 2>/dev/null || true
sleep 3
pkill -KILL -f "spring-boot:run" 2>/dev/null || true
pkill -KILL -f "server.port=$PORT" 2>/dev/null || true
echo ""
echo "===> DONE — paste the STEP 4b RESULT block + DIAGNOSIS back to Claude <==="
