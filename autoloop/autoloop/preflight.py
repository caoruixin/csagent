"""autoloop pre-flight env check — Sprint 061 / S-Auto-7.1.

Deterministic substrate-level discovery of the dev environment that the
auto-loop's `run` subcommand depends on. Pre-flight runs FIRST when
`python -m autoloop run` is invoked, BEFORE `loop.run_iterations()`.

Six checks (run in sequence, all returned as a list of
`PreflightResult`):

    1. check_foreground_backend  — lsof on the bot's HTTP port; a
       foreground Spring backend listening on :8080 contends with
       the per-iter alt-port Spring spawn the applier brings up
       (OQ-S60.7 root-cause workaround). Surfaces the conflict PID
       so `--auto-reboot` can kill it.
    2. check_postgres_reachable  — TCP probe localhost:5432.
    3. check_redis_reachable     — TCP probe localhost:6379.
    4. check_meta_llm_api_key    — `AUTOLOOP_META_LLM_API_KEY` is
       set in `os.environ` and non-empty (OQ-S60.10 surface).
    5. check_clean_working_tree  — `git status --porcelain`; non-
       empty surfaces as `warn`, NOT `fail`.
    6. check_baseline_dir_loads  — `baseline_loader.load(...)`
       against `config.fitness.baseline_dir`.

The pre-flight tool is DEV-ONLY. Production reboot semantics are
NOT in scope.

Stdlib-only: subprocess + socket + pathlib + dataclasses + os +
signal + typing. No new heavy deps. No LLM call.
"""

from __future__ import annotations

import os
import signal
import socket
import subprocess
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Literal, Optional


# --- Public dataclass ------------------------------------------------


Status = Literal["ok", "warn", "fail"]


@dataclass
class PreflightResult:
    """One check's outcome. `remediation` is shown to the operator
    on fail; `details` is free-form structured context (e.g. the
    conflict PID for the foreground-backend check).
    """

    name: str
    status: Status
    message: str
    remediation: Optional[str] = None
    details: Optional[dict[str, Any]] = None


# --- Defaults --------------------------------------------------------

DEFAULT_BACKEND_PORT = 8080
DEFAULT_POSTGRES_HOST = "localhost"
DEFAULT_POSTGRES_PORT = 5432
DEFAULT_REDIS_HOST = "localhost"
DEFAULT_REDIS_PORT = 6379
DEFAULT_TCP_TIMEOUT_SECONDS = 2.0
DEFAULT_LSOF_TIMEOUT_SECONDS = 5.0
DEFAULT_GIT_TIMEOUT_SECONDS = 5.0
DEFAULT_API_KEY_ENV = "AUTOLOOP_META_LLM_API_KEY"


def _preflight_cfg(config: dict[str, Any]) -> dict[str, Any]:
    """Optional `preflight:` block in config.yaml (none ships in
    M-Auto-1B; defaults kick in). Future milestones may surface
    per-env overrides here without touching this module.
    """
    return (config or {}).get("preflight") or {}


# --- Check #1: foreground backend on :8080 ---------------------------


def check_foreground_backend(config: dict[str, Any]) -> PreflightResult:
    """Detect a foreground Spring backend listening on the bot's
    HTTP port. Returns `fail` with the conflict PID(s) on hit.

    The applier brings up an alt-port Spring per-iter; a foreground
    :8080 backend contends for Flyway / Redis / classpath state per
    Sprint 060 OQ-S60.7. The recommended path is to stop the
    foreground backend before the loop runs (manually or via
    `--auto-reboot`).
    """
    port = int(_preflight_cfg(config).get("backend_port", DEFAULT_BACKEND_PORT))
    name = "foreground_backend"

    try:
        proc = subprocess.run(
            ["lsof", "-nP", "-i", f":{port}", "-sTCP:LISTEN"],
            capture_output=True,
            text=True,
            timeout=DEFAULT_LSOF_TIMEOUT_SECONDS,
        )
    except FileNotFoundError:
        return PreflightResult(
            name=name,
            status="warn",
            message="`lsof` not on PATH; cannot probe :%d listener" % port,
            remediation="Install lsof (macOS ships it by default); pre-flight cannot verify port state",
        )
    except subprocess.TimeoutExpired:
        return PreflightResult(
            name=name,
            status="warn",
            message="`lsof -i :%d` timed out" % port,
            remediation="Check lsof responsiveness; pre-flight cannot verify port state",
        )

    pids = _parse_lsof_pids(proc.stdout)
    if not pids:
        return PreflightResult(
            name=name,
            status="ok",
            message="no listener on :%d (alt-port Spring spawn unobstructed)" % port,
        )

    return PreflightResult(
        name=name,
        status="fail",
        message=(
            "foreground listener on :%d (pids=%s) contends with applier alt-port Spring spawn"
            % (port, ",".join(str(p) for p in pids))
        ),
        remediation=(
            "Stop the foreground backend with `kill %s` "
            "or re-run with `--auto-reboot`" % " ".join(str(p) for p in pids)
        ),
        details={"port": port, "pids": pids},
    )


def _parse_lsof_pids(lsof_stdout: str) -> list[int]:
    """Extract PIDs from `lsof -nP -i :<port> -sTCP:LISTEN` output.

    Header is column-name; subsequent rows have COMMAND PID USER ...
    PID is column index 1 (0-indexed). Lines that don't parse cleanly
    are skipped — pre-flight prefers under-reporting to false-fail.
    """
    pids: list[int] = []
    for line in lsof_stdout.splitlines():
        parts = line.split()
        if len(parts) < 2:
            continue
        if parts[0] == "COMMAND":
            continue
        try:
            pid = int(parts[1])
        except ValueError:
            continue
        if pid not in pids:
            pids.append(pid)
    return pids


# --- Check #2: PostgreSQL TCP reachable ------------------------------


def check_postgres_reachable(config: dict[str, Any]) -> PreflightResult:
    cfg = _preflight_cfg(config)
    host = str(cfg.get("postgres_host", DEFAULT_POSTGRES_HOST))
    port = int(cfg.get("postgres_port", DEFAULT_POSTGRES_PORT))
    timeout = float(cfg.get("tcp_timeout_seconds", DEFAULT_TCP_TIMEOUT_SECONDS))

    return _probe_tcp(
        name="postgres_reachable",
        host=host,
        port=port,
        timeout=timeout,
        ok_message="postgres reachable at %s:%d" % (host, port),
        fail_remediation=(
            "Start PostgreSQL (e.g. `brew services start postgresql`); "
            "the per-iter Spring spawn requires it for Flyway migrations"
        ),
    )


# --- Check #3: Redis TCP reachable -----------------------------------


def check_redis_reachable(config: dict[str, Any]) -> PreflightResult:
    cfg = _preflight_cfg(config)
    host = str(cfg.get("redis_host", DEFAULT_REDIS_HOST))
    port = int(cfg.get("redis_port", DEFAULT_REDIS_PORT))
    timeout = float(cfg.get("tcp_timeout_seconds", DEFAULT_TCP_TIMEOUT_SECONDS))

    return _probe_tcp(
        name="redis_reachable",
        host=host,
        port=port,
        timeout=timeout,
        ok_message="redis reachable at %s:%d" % (host, port),
        fail_remediation=(
            "Start Redis (e.g. `brew services start redis`); "
            "the per-iter Spring spawn requires it for session state"
        ),
    )


def _probe_tcp(
    *,
    name: str,
    host: str,
    port: int,
    timeout: float,
    ok_message: str,
    fail_remediation: str,
) -> PreflightResult:
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return PreflightResult(name=name, status="ok", message=ok_message)
    except (OSError, socket.timeout) as e:
        return PreflightResult(
            name=name,
            status="fail",
            message="cannot reach %s:%d (%s)" % (host, port, e),
            remediation=fail_remediation,
            details={"host": host, "port": port},
        )


# --- Check #4: meta-LLM API key set ----------------------------------


def check_meta_llm_api_key(config: dict[str, Any]) -> PreflightResult:
    meta = (config or {}).get("meta_agent") or {}
    env_name = str(meta.get("api_key_env", DEFAULT_API_KEY_ENV))
    val = os.environ.get(env_name, "")
    if val:
        return PreflightResult(
            name="meta_llm_api_key",
            status="ok",
            message="%s set in env (length=%d)" % (env_name, len(val)),
        )
    return PreflightResult(
        name="meta_llm_api_key",
        status="fail",
        message="%s is unset or empty in os.environ" % env_name,
        remediation=(
            "Ensure `autoloop/.env.local` declares %s, then either "
            "rely on the CLI's dotenv auto-load OR `set -a; "
            "source autoloop/.env.local; set +a` before invoking the loop"
            % env_name
        ),
        details={"env_name": env_name},
    )


# --- Check #5: working tree cleanliness ------------------------------


def check_clean_working_tree(config: dict[str, Any], *, cwd: Optional[Path] = None) -> PreflightResult:
    """`git status --porcelain`; non-empty surfaces as `warn` (NOT
    `fail`) because dev may have intentional in-flight state.
    """
    name = "clean_working_tree"
    try:
        proc = subprocess.run(
            ["git", "status", "--porcelain"],
            cwd=str(cwd) if cwd else None,
            capture_output=True,
            text=True,
            timeout=DEFAULT_GIT_TIMEOUT_SECONDS,
        )
    except FileNotFoundError:
        return PreflightResult(
            name=name,
            status="warn",
            message="`git` not on PATH; cannot verify working tree state",
        )
    except subprocess.TimeoutExpired:
        return PreflightResult(
            name=name,
            status="warn",
            message="`git status` timed out",
        )

    if proc.returncode != 0:
        return PreflightResult(
            name=name,
            status="warn",
            message="`git status` exited %d: %s" % (proc.returncode, (proc.stderr or "").strip()),
        )

    porcelain = (proc.stdout or "").strip()
    if not porcelain:
        return PreflightResult(
            name=name,
            status="ok",
            message="working tree clean",
        )

    n_lines = len(porcelain.splitlines())
    return PreflightResult(
        name=name,
        status="warn",
        message="working tree has %d uncommitted change(s)" % n_lines,
        remediation="Commit or stash before running the loop (warning only; not a hard fail)",
        details={"porcelain_line_count": n_lines},
    )


# --- Check #6: baseline_dir loads cleanly ----------------------------


def check_baseline_dir_loads(
    config: dict[str, Any], *, repo_root: Optional[Path] = None
) -> PreflightResult:
    """Invoke `baseline_loader.load(baseline_dir, config=config)`;
    surface BaselineLoadError or non-empty warnings as `fail`.

    Lazy-imports baseline_loader so a missing scoring/ surface
    cannot crash the pre-flight at import time.
    """
    name = "baseline_dir_loads"
    fitness = (config or {}).get("fitness") or {}
    rel = fitness.get("baseline_dir")
    if not rel:
        return PreflightResult(
            name=name,
            status="fail",
            message="config.fitness.baseline_dir is not set",
            remediation=(
                "Bless a baseline_dir per the M-Auto-1B contract "
                "(see autoloop/config.yaml `fitness.baseline_dir` comments)"
            ),
        )

    baseline_dir = Path(rel)
    if not baseline_dir.is_absolute() and repo_root is not None:
        baseline_dir = repo_root / baseline_dir

    try:
        from autoloop.scoring import baseline_loader
    except ImportError as e:
        return PreflightResult(
            name=name,
            status="fail",
            message="cannot import baseline_loader: %s" % e,
            remediation="Repair the autoloop.scoring import surface",
        )

    try:
        snapshot = baseline_loader.load(baseline_dir, config=config)
    except baseline_loader.BaselineLoadError as e:
        return PreflightResult(
            name=name,
            status="fail",
            message="baseline_loader.load failed: %s" % e,
            remediation=(
                "Verify config.fitness.baseline_dir points at the blessed "
                "staging directory and that per-suite results.json files exist"
            ),
            details={"baseline_dir": str(baseline_dir)},
        )

    if snapshot.warnings:
        return PreflightResult(
            name=name,
            status="fail",
            message=(
                "baseline_loader surfaced %d warning(s): %s"
                % (len(snapshot.warnings), "; ".join(snapshot.warnings[:3]))
            ),
            remediation=(
                "Re-bless the baseline_dir (per-suite results.json missing); "
                "see Sprint 060 / S-Auto-7 handoff for the blessing procedure"
            ),
            details={"baseline_dir": str(baseline_dir)},
        )

    return PreflightResult(
        name=name,
        status="ok",
        message=(
            "baseline snapshot loaded (run_id=%s, %d suite(s))"
            % (snapshot.baseline_run_id, len(snapshot.snapshots))
        ),
        details={"baseline_dir": str(baseline_dir)},
    )


# --- Aggregator ------------------------------------------------------


def run_preflight(
    config: dict[str, Any], *, repo_root: Optional[Path] = None
) -> list[PreflightResult]:
    """Run all six checks in sequence and return their results.
    Callers decide whether `fail` blocks the loop (the CLI default
    refuses to start; `--skip-preflight` is the dev escape hatch).
    """
    cwd = repo_root if repo_root else None
    return [
        check_foreground_backend(config),
        check_postgres_reachable(config),
        check_redis_reachable(config),
        check_meta_llm_api_key(config),
        check_clean_working_tree(config, cwd=cwd),
        check_baseline_dir_loads(config, repo_root=repo_root),
    ]


# --- Auto-reboot helper ----------------------------------------------


def auto_reboot_foreground_backend(
    pid: int,
    *,
    sigterm_timeout_seconds: float = 5.0,
) -> bool:
    """Stop the foreground :8080 backend pid. SIGTERM with timeout
    escalation to SIGKILL (OQ-S61.2 default).

    Returns True if the process is gone after the sequence; False if
    the process is still alive after SIGKILL (caller treats as fatal).
    Dev-only ergonomics; NOT a production reboot mechanism.
    """
    if pid <= 0:
        return False

    try:
        os.kill(pid, signal.SIGTERM)
    except ProcessLookupError:
        return True
    except PermissionError:
        return False

    deadline = time.monotonic() + sigterm_timeout_seconds
    while time.monotonic() < deadline:
        if not _pid_alive(pid):
            return True
        time.sleep(0.2)

    try:
        os.kill(pid, signal.SIGKILL)
    except ProcessLookupError:
        return True
    except PermissionError:
        return False

    # Brief grace for SIGKILL to land.
    for _ in range(10):
        if not _pid_alive(pid):
            return True
        time.sleep(0.1)
    return not _pid_alive(pid)


def _pid_alive(pid: int) -> bool:
    try:
        os.kill(pid, 0)
    except ProcessLookupError:
        return False
    except PermissionError:
        return True
    return True


# --- Stdout formatting -----------------------------------------------


def format_report(results: list[PreflightResult]) -> str:
    """Compact, scannable report. The CLI prints this to stdout
    alongside iteration progress (NOT a separate log file).
    """
    lines: list[str] = []
    lines.append("[preflight] %d check(s):" % len(results))
    for r in results:
        prefix = {"ok": "OK  ", "warn": "WARN", "fail": "FAIL"}.get(r.status, "????")
        lines.append("[preflight] %s %s — %s" % (prefix, r.name, r.message))
        if r.remediation and r.status != "ok":
            lines.append("[preflight]      remediation: %s" % r.remediation)
    n_fail = sum(1 for r in results if r.status == "fail")
    n_warn = sum(1 for r in results if r.status == "warn")
    n_ok = sum(1 for r in results if r.status == "ok")
    lines.append(
        "[preflight] summary: ok=%d warn=%d fail=%d" % (n_ok, n_warn, n_fail)
    )
    return "\n".join(lines)
