#!/usr/bin/env bash
# Sprint 064 / S-Auto-9 / M-Auto-2 — OQ-S62.3 diagnostic Step 2a
#
# Pure-Python signal-trap probe to isolate whether the persistent 2-5 min
# OS-level kill targets ANY nohup'd python (background-process policy,
# App Nap, TAL, etc.) or is autoloop-specific (mvn child, resource pattern,
# memory profile).
#
# Outcomes:
#   1. Probe dies in 2-5 min, signal log shows GOT SIGNAL N → trappable kill;
#      identifies userspace killer (likely launchd/RunningBoard SIGTERM)
#   2. Probe dies in 2-5 min, signal log shows nothing → SIGKILL (untrappable)
#      from kernel or privileged sender; need dtrace next
#   3. Probe survives >15 min → kill is autoloop-specific, not nohup'd-python
#      generic; redirect diagnostic to autoloop resource profile
#
# Usage: bash /Users/caoruixin/projects/csagent-latest/scripts/sprint-064-signal-trap-probe.sh
# Logs to: /tmp/sprint-064-step2a.*

set -u
SCRATCH=/tmp/sprint-064-step2a
PROBE_PY=/tmp/sprint-064-signal-trap-probe.py

echo "===> Sprint 064 Step 2a: signal-trap probe <==="

# --- Cleanup any prior probe run + scratch files ---
pkill -f "signal-trap-probe" 2>/dev/null || true
sleep 1
rm -f ${SCRATCH}.* ${PROBE_PY}

# --- Write the probe Python (heredoc) ---
cat > ${PROBE_PY} <<'PYEOF'
"""Idle signal-trap probe. Traps every trappable signal; logs receipt with
timestamp; sleeps in 30s ticks. Designed to be killed by the same OS
mechanism that kills autoloop after 2-5 min."""
import signal, sys, os, time

LOG_PATH = "/tmp/sprint-064-step2a.signals.log"
PID = os.getpid()
LOG = open(LOG_PATH, "a", buffering=1)


def log(msg):
    line = f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] pid={PID} {msg}"
    print(line, flush=True)
    LOG.write(line + "\n")


def make_handler(sig_name, fatal=False):
    def handler(signum, _frame):
        log(f"GOT SIGNAL {signum} ({sig_name})")
        if fatal:
            log(f"...exiting on {sig_name}")
            sys.exit(0)
    return handler


# Trap everything that's trappable. SIGKILL and SIGSTOP cannot be trapped
# by design — if we die silently, the killer used SIGKILL.
TRAPPABLE = [
    ("SIGTERM", True),
    ("SIGHUP", True),
    ("SIGINT", True),
    ("SIGQUIT", True),
    ("SIGUSR1", False),
    ("SIGUSR2", False),
    ("SIGPIPE", False),
    ("SIGALRM", False),
    ("SIGCHLD", False),
    ("SIGTSTP", False),
    ("SIGCONT", False),
    ("SIGXCPU", False),
    ("SIGXFSZ", False),
    ("SIGVTALRM", False),
    ("SIGPROF", False),
    ("SIGWINCH", False),
    ("SIGIO", False),
    ("SIGSYS", False),
]

for name, fatal in TRAPPABLE:
    try:
        signal.signal(getattr(signal, name), make_handler(name, fatal=fatal))
    except (ValueError, OSError) as exc:
        log(f"could not trap {name}: {exc}")

log(f"probe started; trapping {len(TRAPPABLE)} signals; 30s ticks")
i = 0
while True:
    time.sleep(30)
    i += 1
    log(f"alive tick #{i} ({i * 30}s)")
PYEOF

# --- Record launch metadata ---
date "+%Y-%m-%d %H:%M:%S" > ${SCRATCH}-launch.ts
date +%s > ${SCRATCH}-launch.epoch

# --- Launch (same pattern as failing autoloop overnight: nohup + disown) ---
nohup python -u ${PROBE_PY} > ${SCRATCH}.stdout.log 2> ${SCRATCH}.stderr.log &
PROBE_PID=$!
echo $PROBE_PID > ${SCRATCH}.pid
disown $PROBE_PID 2>/dev/null || true

echo ""
echo "===> PROBE PID=$PROBE_PID launched at $(cat ${SCRATCH}-launch.ts) <==="
echo ""

# --- Capture launch-time snapshots ---
echo "----- ps at launch -----"
ps -o pri,nice,pid,ppid,user,command -p $PROBE_PID | tee ${SCRATCH}-ps-launch.log

echo ""
echo "----- parent shell ps -----"
SHELL_PPID=$(ps -o ppid= -p $PROBE_PID | tr -d ' ')
ps -o pri,nice,pid,user,command -p $SHELL_PPID | tee -a ${SCRATCH}-ps-launch.log

echo ""
echo "----- default nice (shell) -----"
nice | tee -a ${SCRATCH}-ps-launch.log

echo ""
echo "----- WorkloadOptimization defaults -----"
defaults read com.apple.WorkloadOptimization 2>&1 | head -20 | tee ${SCRATCH}-workload-defaults.log

echo ""
echo "----- system memory state -----"
sysctl vm.memory_pressure kern.willshutdown 2>&1 | tee ${SCRATCH}-sysctl.log
vm_stat | head -10 | tee -a ${SCRATCH}-sysctl.log

echo ""
echo "===> Watching for death (10s poll). Ctrl-C OK — death is file-based. <==="
echo ""

# --- Watch for death (polling) ---
TICKS=0
while kill -0 $PROBE_PID 2>/dev/null; do
    sleep 10
    TICKS=$((TICKS + 1))
    # Print a heartbeat every 60s so the user sees progress
    if [ $((TICKS % 6)) -eq 0 ]; then
        ELAPSED_NOW=$(( $(date +%s) - $(cat ${SCRATCH}-launch.epoch) ))
        echo "  [watcher] still alive at ${ELAPSED_NOW}s..."
    fi
done

date "+%Y-%m-%d %H:%M:%S" > ${SCRATCH}-death.ts
date +%s > ${SCRATCH}-death.epoch
ELAPSED=$(( $(cat ${SCRATCH}-death.epoch) - $(cat ${SCRATCH}-launch.epoch) ))

echo ""
echo "===> PROBE DEAD: PID=$PROBE_PID at $(cat ${SCRATCH}-death.ts) (elapsed=${ELAPSED}s) <==="
echo ""

# --- Summary ---
echo "----- final signal log (${SCRATCH}.signals.log) -----"
if [ -s ${SCRATCH}.signals.log ]; then
    cat ${SCRATCH}.signals.log
else
    echo "(empty — no signals trapped; likely SIGKILL or process exec'd before any signal)"
fi

echo ""
echo "----- stdout tail -----"
tail -30 ${SCRATCH}.stdout.log

echo ""
echo "----- stderr tail -----"
tail -30 ${SCRATCH}.stderr.log

echo ""
echo "===> DONE — paste the elapsed time + signal log lines back to Claude <==="
