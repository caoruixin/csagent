"""Tests for the applier's mvn subprocess invocation shape.

Pinned by S-Auto-7.2 (Sprint 062, M-Auto-1C) to prevent OQ-S61.1
regression: the root `csagent-parent` pom has `<packaging>pom</packaging>`
with no `mainClass`, so any reactor selection that brings the parent in
will fail `spring-boot:run` before reaching the server submodule.

These tests do NOT spawn a real mvn process; they capture the argv
constructed by `_spawn_spring` via a `subprocess.Popen` patch and
assert on the command shape.
"""

from __future__ import annotations

import re
from pathlib import Path
from unittest.mock import MagicMock, patch

from autoloop.sandbox import applier as _applier


def _capture_spawn_argv(tmp_path: Path) -> list[str]:
    """Invoke `_spawn_spring` with Popen patched; return the argv."""
    captured: dict[str, list[str]] = {}

    def fake_popen(cmd, **_kwargs):  # type: ignore[no-untyped-def]
        captured["cmd"] = list(cmd)
        return MagicMock()

    with patch.object(_applier.subprocess, "Popen", side_effect=fake_popen):
        _applier._spawn_spring(tmp_path, 18080)

    return captured["cmd"]


def test_spawn_spring_does_not_include_dash_am(tmp_path: Path):
    """OQ-S61.1 regression guard.

    `-am` (`--also-make`) would pull `csagent-parent` (packaging=pom,
    no mainClass) into the reactor and fail `spring-boot:run` on the
    parent before the server submodule is touched.
    """
    cmd = _capture_spawn_argv(tmp_path)
    assert "-am" not in cmd, (
        "applier._spawn_spring must not pass -am: it would bring "
        "csagent-parent (packaging=pom, no mainClass) into the "
        "reactor and break spring-boot:run (OQ-S61.1)."
    )
    assert "--also-make" not in cmd


def test_spawn_spring_command_shape_pl_server_only(tmp_path: Path):
    """Lock the post-fix command shape: -pl server, no -am, with
    spring-boot:run goal and a --server.port system property."""
    cmd = _capture_spawn_argv(tmp_path)
    assert cmd[0] == "mvn"
    assert "-q" in cmd
    pl_idx = cmd.index("-pl")
    assert cmd[pl_idx + 1] == "server"
    assert "spring-boot:run" in cmd
    port_args = [a for a in cmd if a.startswith("-Dspring-boot.run.arguments=")]
    assert len(port_args) == 1
    assert re.search(r"--server\.port=\d+", port_args[0])
