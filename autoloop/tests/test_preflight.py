"""Tests for `autoloop.preflight` — Sprint 061 / S-Auto-7.1.

All subprocess + socket invocations are mocked. No live network call,
no live `lsof` invocation, no live `git status`. The auto-reboot
helper's signal sequence is verified via `os.kill` patching.

CLI integration tests use the same subprocess-based pattern as
`test_cli_smoke.py` for the preflight subcommand surface (verifying
`--help` exits 0 + the new subcommand + flags are registered).
"""

from __future__ import annotations

import os
import signal
import socket
import subprocess
import sys
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from autoloop import preflight
from autoloop.preflight import (
    PreflightResult,
    auto_reboot_foreground_backend,
    check_baseline_dir_loads,
    check_clean_working_tree,
    check_foreground_backend,
    check_meta_llm_api_key,
    check_postgres_reachable,
    check_redis_reachable,
    format_report,
    run_preflight,
)


REPO_ROOT = Path(__file__).resolve().parents[2]


# --- check_foreground_backend ----------------------------------------


def test_foreground_backend_no_listener_returns_ok():
    fake_proc = MagicMock(stdout="", returncode=1)
    with patch.object(subprocess, "run", return_value=fake_proc):
        r = check_foreground_backend({})
    assert r.status == "ok"
    assert r.name == "foreground_backend"


def test_foreground_backend_listener_returns_fail_with_pid():
    lsof_output = (
        "COMMAND   PID    USER   FD   TYPE             DEVICE SIZE/OFF NODE NAME\n"
        "java    8613 caoruixin  100u  IPv6 0xabc      0t0  TCP *:8080 (LISTEN)\n"
    )
    fake_proc = MagicMock(stdout=lsof_output, returncode=0)
    with patch.object(subprocess, "run", return_value=fake_proc):
        r = check_foreground_backend({})
    assert r.status == "fail"
    assert r.details["pids"] == [8613]
    assert "8613" in r.remediation


def test_foreground_backend_lsof_missing_returns_warn():
    with patch.object(subprocess, "run", side_effect=FileNotFoundError()):
        r = check_foreground_backend({})
    assert r.status == "warn"


def test_foreground_backend_honors_config_port_override():
    fake_proc = MagicMock(stdout="", returncode=1)
    seen_cmd: list[list[str]] = []

    def _capture(cmd, **_kwargs):
        seen_cmd.append(cmd)
        return fake_proc

    with patch.object(subprocess, "run", side_effect=_capture):
        check_foreground_backend({"preflight": {"backend_port": 9090}})
    assert seen_cmd
    assert ":9090" in " ".join(seen_cmd[0])


# --- check_postgres_reachable / check_redis_reachable ----------------


def test_postgres_reachable_ok_when_socket_opens():
    fake_sock = MagicMock()
    fake_sock.__enter__ = MagicMock(return_value=fake_sock)
    fake_sock.__exit__ = MagicMock(return_value=None)
    with patch.object(socket, "create_connection", return_value=fake_sock):
        r = check_postgres_reachable({})
    assert r.status == "ok"
    assert "postgres" in r.message


def test_postgres_reachable_fail_when_socket_refused():
    with patch.object(socket, "create_connection", side_effect=ConnectionRefusedError("refused")):
        r = check_postgres_reachable({})
    assert r.status == "fail"
    assert "remediation" in (r.remediation or "").lower() or r.remediation


def test_redis_reachable_fail_when_socket_timeout():
    with patch.object(socket, "create_connection", side_effect=socket.timeout()):
        r = check_redis_reachable({})
    assert r.status == "fail"
    assert r.details["port"] == 6379


# --- check_meta_llm_api_key ------------------------------------------


def test_meta_llm_api_key_present_returns_ok(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("AUTOLOOP_META_LLM_API_KEY", "sk-xyz")
    r = check_meta_llm_api_key({})
    assert r.status == "ok"


def test_meta_llm_api_key_absent_returns_fail(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.delenv("AUTOLOOP_META_LLM_API_KEY", raising=False)
    r = check_meta_llm_api_key({})
    assert r.status == "fail"
    assert "AUTOLOOP_META_LLM_API_KEY" in r.message


# --- check_clean_working_tree ----------------------------------------


def test_clean_working_tree_empty_porcelain_is_ok():
    fake_proc = MagicMock(stdout="", returncode=0, stderr="")
    with patch.object(subprocess, "run", return_value=fake_proc):
        r = check_clean_working_tree({})
    assert r.status == "ok"


def test_dirty_working_tree_is_warn_not_fail():
    fake_proc = MagicMock(
        stdout=" M autoloop/cli.py\n?? new_file.py\n",
        returncode=0,
        stderr="",
    )
    with patch.object(subprocess, "run", return_value=fake_proc):
        r = check_clean_working_tree({})
    assert r.status == "warn"
    assert r.details["porcelain_line_count"] == 2


# --- check_baseline_dir_loads ----------------------------------------


def test_baseline_dir_loads_ok_against_blessed_dir():
    """Sample the real blessed baseline_dir from S-Auto-7 — the
    pre-flight is exactly the consumer this check is designed for.
    """
    import yaml

    with (REPO_ROOT / "autoloop" / "config.yaml").open("r", encoding="utf-8") as f:
        cfg = yaml.safe_load(f)
    r = check_baseline_dir_loads(cfg, repo_root=REPO_ROOT)
    assert r.status == "ok"
    assert "baseline snapshot loaded" in r.message


def test_baseline_dir_loads_fail_when_missing():
    cfg = {
        "fitness": {
            "baseline_dir": "nonexistent/path/blah",
            "suites": [{"name": "bad_cases"}],
        }
    }
    r = check_baseline_dir_loads(cfg, repo_root=REPO_ROOT)
    assert r.status == "fail"


def test_baseline_dir_loads_fail_when_unset():
    cfg = {"fitness": {}}
    r = check_baseline_dir_loads(cfg)
    assert r.status == "fail"
    assert "not set" in r.message


# --- auto_reboot_foreground_backend ----------------------------------


def test_auto_reboot_sigterm_then_process_gone():
    """SIGTERM lands → process disappears before the timeout."""
    kill_calls: list[tuple[int, int]] = []

    def fake_kill(pid: int, sig: int) -> None:
        kill_calls.append((pid, sig))
        if sig == 0 and len(kill_calls) > 1:
            # After SIGTERM, _pid_alive probe should report dead.
            raise ProcessLookupError()

    with patch.object(os, "kill", side_effect=fake_kill):
        ok = auto_reboot_foreground_backend(1234, sigterm_timeout_seconds=0.5)
    assert ok is True
    assert kill_calls[0] == (1234, signal.SIGTERM)


def test_auto_reboot_sigkill_after_sigterm_ignored():
    """SIGTERM is ignored (process still alive) → SIGKILL is issued."""
    state = {"alive": True, "sigkill_seen": False}

    def fake_kill(pid: int, sig: int) -> None:
        if sig == signal.SIGTERM:
            return
        if sig == signal.SIGKILL:
            state["alive"] = False
            state["sigkill_seen"] = True
            return
        if sig == 0:
            if not state["alive"]:
                raise ProcessLookupError()
            return

    with patch.object(os, "kill", side_effect=fake_kill):
        ok = auto_reboot_foreground_backend(4321, sigterm_timeout_seconds=0.3)
    assert ok is True
    assert state["sigkill_seen"] is True


def test_auto_reboot_pid_already_gone_returns_true():
    def fake_kill(pid: int, sig: int) -> None:
        raise ProcessLookupError()

    with patch.object(os, "kill", side_effect=fake_kill):
        ok = auto_reboot_foreground_backend(9999, sigterm_timeout_seconds=0.1)
    assert ok is True


def test_auto_reboot_invalid_pid_returns_false():
    assert auto_reboot_foreground_backend(0) is False
    assert auto_reboot_foreground_backend(-1) is False


# --- run_preflight aggregator ----------------------------------------


def test_run_preflight_returns_six_results(monkeypatch: pytest.MonkeyPatch):
    """Aggregator runs all 6 checks and returns the list — with
    every external dep mocked so the test is hermetic.
    """
    monkeypatch.setenv("AUTOLOOP_META_LLM_API_KEY", "x")

    fake_lsof = MagicMock(stdout="", returncode=1)
    fake_git = MagicMock(stdout="", returncode=0, stderr="")
    fake_sock = MagicMock()
    fake_sock.__enter__ = MagicMock(return_value=fake_sock)
    fake_sock.__exit__ = MagicMock(return_value=None)

    def fake_subprocess_run(cmd, **_kwargs):
        if cmd and cmd[0] == "lsof":
            return fake_lsof
        if cmd and cmd[0] == "git":
            return fake_git
        return MagicMock(stdout="", returncode=0, stderr="")

    cfg = {
        "fitness": {
            "baseline_dir": "nonexistent",
            "suites": [{"name": "bad_cases"}],
        },
    }
    with patch.object(subprocess, "run", side_effect=fake_subprocess_run), \
         patch.object(socket, "create_connection", return_value=fake_sock):
        results = run_preflight(cfg)

    assert len(results) == 6
    names = [r.name for r in results]
    assert names == [
        "foreground_backend",
        "postgres_reachable",
        "redis_reachable",
        "meta_llm_api_key",
        "clean_working_tree",
        "baseline_dir_loads",
    ]


# --- format_report ---------------------------------------------------


def test_format_report_renders_summary():
    results = [
        PreflightResult(name="a", status="ok", message="fine"),
        PreflightResult(name="b", status="warn", message="meh", remediation="fix b"),
        PreflightResult(name="c", status="fail", message="broken", remediation="fix c"),
    ]
    out = format_report(results)
    assert "ok=1 warn=1 fail=1" in out
    assert "fix b" in out
    assert "fix c" in out
    # `ok` results never print remediation lines.
    assert "fix a" not in out


# --- CLI integration -------------------------------------------------


def _run_cli(args: list[str]) -> subprocess.CompletedProcess:
    return subprocess.run(
        [sys.executable, "-m", "autoloop", *args],
        cwd=str(REPO_ROOT),
        capture_output=True,
        text=True,
    )


def test_cli_preflight_subcommand_help_exits_zero():
    res = _run_cli(["preflight", "--help"])
    assert res.returncode == 0
    assert "preflight" in res.stdout.lower()


def test_cli_top_level_help_lists_preflight():
    res = _run_cli(["--help"])
    assert res.returncode == 0
    assert "preflight" in res.stdout


def test_cli_run_help_advertises_new_flags():
    res = _run_cli(["run", "--help"])
    assert res.returncode == 0
    assert "--auto-reboot" in res.stdout
    assert "--skip-preflight" in res.stdout


def test_cli_run_skip_preflight_bypasses_gate(monkeypatch: pytest.MonkeyPatch):
    """--skip-preflight + --dry-run should NOT invoke preflight.run_preflight
    (dry-run already short-circuits pre-flight; with --skip-preflight on
    a live run the gate prints a warning and proceeds). This test
    verifies the gate logic via the in-process helper.
    """
    from autoloop import cli

    cfg: dict = {}
    called = {"n": 0}

    def fake_run_preflight(*_a, **_kw):
        called["n"] += 1
        return []

    monkeypatch.setattr(cli._preflight, "run_preflight", fake_run_preflight)
    rc = cli._run_preflight_gate(
        cfg, REPO_ROOT, auto_reboot=False, skip_preflight=True
    )
    assert rc == 0
    assert called["n"] == 0


def test_cli_run_preflight_gate_refuses_on_fail(monkeypatch: pytest.MonkeyPatch):
    from autoloop import cli

    def fake_run_preflight(*_a, **_kw):
        return [
            PreflightResult(
                name="meta_llm_api_key",
                status="fail",
                message="missing",
                remediation="export it",
            )
        ]

    monkeypatch.setattr(cli._preflight, "run_preflight", fake_run_preflight)
    rc = cli._run_preflight_gate({}, REPO_ROOT, auto_reboot=False, skip_preflight=False)
    assert rc != 0


def test_cli_run_preflight_gate_auto_reboot_kills_foreground(
    monkeypatch: pytest.MonkeyPatch,
):
    """--auto-reboot invokes auto_reboot_foreground_backend on the
    foreground_backend fail, then re-runs pre-flight; the second
    pass returns clean so the gate proceeds.
    """
    from autoloop import cli

    sequence = [
        [
            PreflightResult(
                name="foreground_backend",
                status="fail",
                message="listener on :8080",
                remediation="kill it",
                details={"port": 8080, "pids": [8613]},
            )
        ],
        [],  # second pass — clean
    ]
    calls = {"reboot": []}

    def fake_run_preflight(*_a, **_kw):
        return sequence.pop(0)

    def fake_reboot(pid: int, **_kw) -> bool:
        calls["reboot"].append(pid)
        return True

    monkeypatch.setattr(cli._preflight, "run_preflight", fake_run_preflight)
    monkeypatch.setattr(
        cli._preflight, "auto_reboot_foreground_backend", fake_reboot
    )
    rc = cli._run_preflight_gate({}, REPO_ROOT, auto_reboot=True, skip_preflight=False)
    assert rc == 0
    assert calls["reboot"] == [8613]


def test_cli_run_preflight_gate_auto_reboot_does_not_recover_other_fails(
    monkeypatch: pytest.MonkeyPatch,
):
    from autoloop import cli

    def fake_run_preflight(*_a, **_kw):
        return [
            PreflightResult(
                name="postgres_reachable",
                status="fail",
                message="refused",
                remediation="start it",
            )
        ]

    monkeypatch.setattr(cli._preflight, "run_preflight", fake_run_preflight)
    rc = cli._run_preflight_gate({}, REPO_ROOT, auto_reboot=True, skip_preflight=False)
    assert rc != 0
