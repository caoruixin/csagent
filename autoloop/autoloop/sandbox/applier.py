"""Applier — writes a hypothesis to a Skill YAML on `autoloop/exp-N`
branch and brings up a backend on an alternate port for eval to hit.

S-Auto-3 deliverable. The S-Auto-1 skeleton documented the
single-file fence + the validator contract; this module fills in
the git logic, the YAML field patch, the free-port discovery, the
Spring spawn, and the health-probe.

Hard fences enforced here:

- A `Hypothesis` whose `target_skill_file` is NOT one of the six
  allowed Skill YAMLs is rejected BEFORE any git operation runs.
  Cross-file edits (multiple files touched in one bundle) are not
  representable in the `Hypothesis` dataclass — one file per
  hypothesis — so the cross-file fence is enforced by typing.
- After patching the YAML, the applier re-invokes
  `yaml_diff_validator.validate_skill_yaml_diff(...)` against the
  on-disk result as a sanity check. If the validator rejects the
  on-disk diff (e.g. the field path resolved unexpectedly), the
  applier raises BEFORE committing.
- The applier never touches `main`. It always runs on an
  `autoloop/exp-N` branch.
- The applier never invokes `eval-interactive`. That is the
  eval_runner's job; mixing concerns here would muddle the surface.

The applier returns an `AppliedExperiment` whose `backend_process`
is the live Spring subprocess. The caller MUST call `cleanup()` in
a `finally` block to avoid orphaned backend processes — `loop.py`
does this.
"""

from __future__ import annotations

import os
import re
import shutil
import socket
import subprocess
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any, TYPE_CHECKING

import yaml

from .yaml_diff_validator import validate_skill_yaml_diff

if TYPE_CHECKING:
    from ..meta_agent.proposer import Hypothesis


_REPO_ROOT = Path(__file__).resolve().parents[3]


class ApplyError(Exception):
    """Base class for applier errors."""


class CrossFileError(ApplyError):
    """Raised when a hypothesis targets a non-allowed Skill YAML."""


class BeforeValueMismatchError(ApplyError):
    """Raised when on-disk current value disagrees with hypothesis.before_value."""


class SpringStartupTimeoutError(ApplyError):
    """Raised when the health-probe never succeeds within the timeout."""


class SanityCheckRejectError(ApplyError):
    """Raised when the on-disk YAML diff fails the validator post-patch."""


@dataclass
class AppliedExperiment:
    """The handle the caller cleans up after the iteration finishes.

    Fields:
        iteration_id — "exp-1", "exp-2", ...
        branch_name — full git branch name (e.g. "autoloop/exp-1").
        commit_sha — the SHA of the autoloop commit (one commit per
            iteration).
        skill_file_path — repo-absolute path to the Skill YAML.
        backend_port — TCP port the alt-Spring is listening on.
        backend_process — live subprocess; cleanup() terminates it.
        original_branch — the branch the caller started on, so
            cleanup can switch back.
    """

    iteration_id: str
    branch_name: str
    commit_sha: str
    skill_file_path: Path
    backend_port: int
    backend_process: subprocess.Popen | None
    original_branch: str = ""


def apply(
    hypothesis: "Hypothesis",
    *,
    iteration_id: str,
    config: dict[str, Any],
    repo_root: Path | None = None,
    health_probe_timeout_seconds: int = 120,
    health_probe_interval_seconds: float = 2.0,
) -> AppliedExperiment:
    """Patch the target Skill YAML on a fresh `autoloop/<iteration_id>`
    branch, commit, then bring up a backend on a free alt port.

    Returns an AppliedExperiment with the live subprocess handle.
    Callers MUST call `cleanup(...)` in a finally block.
    """
    surface_cfg = (config or {}).get("mutable_surface") or {}
    allowed_files: list[str] = surface_cfg.get("allowed_skill_files") or []
    allowed_paths: list[str] = surface_cfg.get("allowed_field_paths") or []
    root = Path(repo_root) if repo_root else _REPO_ROOT

    if hypothesis.target_skill_file not in allowed_files:
        raise CrossFileError(
            f"hypothesis targets non-allowed file: {hypothesis.target_skill_file}"
        )

    skill_path = root / hypothesis.target_skill_file
    if not skill_path.exists():
        raise ApplyError(f"target skill file missing on disk: {skill_path}")

    # --- 1. Record current branch, then create exp-N branch.
    original_branch = _git_current_branch(root)
    branch_name = f"autoloop/{iteration_id}"
    _git_create_branch(root, branch_name)

    try:
        # --- 2. Read current YAML, verify before_value, patch, write back.
        before_yaml = skill_path.read_text(encoding="utf-8")
        current_field_value = _read_field_from_yaml(
            before_yaml, hypothesis.target_field_path
        )
        if current_field_value != hypothesis.before_value:
            raise BeforeValueMismatchError(
                f"before_value mismatch on {hypothesis.target_field_path}: "
                f"on-disk len={len(current_field_value)} "
                f"hypothesis len={len(hypothesis.before_value)}"
            )

        after_yaml = _write_field_to_yaml(
            before_yaml,
            hypothesis.target_field_path,
            hypothesis.after_value,
        )
        skill_path.write_text(after_yaml, encoding="utf-8")

        # --- 3. Sanity-check: the on-disk diff must still pass validator.
        verdict = validate_skill_yaml_diff(
            before_yaml=before_yaml,
            after_yaml=after_yaml,
            file_path=hypothesis.target_skill_file,
            allowed_skill_files=allowed_files,
            allowed_field_paths=allowed_paths,
        )
        if verdict.decision != "ACCEPT":
            raise SanityCheckRejectError(
                f"post-patch sanity check failed: {verdict.reason}"
            )

        # --- 4. git add + commit.
        _git_add(root, hypothesis.target_skill_file)
        commit_msg = _build_commit_message(hypothesis, iteration_id)
        _git_commit(root, commit_msg)
        commit_sha = _git_head_sha(root)

        # --- 5. Find a free port.
        backend_port = _find_free_port()

        # --- 6. Spawn Spring on the alt port + health-probe.
        backend_process = _spawn_spring(root, backend_port)
        try:
            _health_probe(
                port=backend_port,
                timeout_seconds=health_probe_timeout_seconds,
                interval_seconds=health_probe_interval_seconds,
                proc=backend_process,
            )
        except SpringStartupTimeoutError:
            _terminate_process(backend_process)
            raise

        return AppliedExperiment(
            iteration_id=iteration_id,
            branch_name=branch_name,
            commit_sha=commit_sha,
            skill_file_path=skill_path,
            backend_port=backend_port,
            backend_process=backend_process,
            original_branch=original_branch,
        )
    except Exception:
        # On failure post branch-creation, switch back to original
        # branch but keep the autoloop/exp-N branch in place so the
        # human can inspect what went wrong.
        try:
            _git_checkout(root, original_branch)
        except Exception:
            pass
        raise


def cleanup(applied: AppliedExperiment, *, repo_root: Path | None = None) -> None:
    """Tear down a running experiment: kill backend, switch branch back.

    The autoloop/exp-N branch and its commit are intentionally NOT
    deleted — they are the audit trail for `autoloop apply` to
    cherry-pick from later. Branch hygiene (deleting old discard
    branches) is a future-milestone concern.
    """
    if applied.backend_process is not None:
        _terminate_process(applied.backend_process)

    root = Path(repo_root) if repo_root else _REPO_ROOT
    if applied.original_branch:
        try:
            _git_checkout(root, applied.original_branch)
        except Exception:
            # Best-effort. If the human had uncommitted state on the
            # original branch, the checkout might fail; that is a
            # human-facing surface, not a loop-failing condition.
            pass


# --- git operations --------------------------------------------------


def _git_current_branch(root: Path) -> str:
    out = _run_git(root, ["rev-parse", "--abbrev-ref", "HEAD"])
    return out.strip()


def _git_create_branch(root: Path, branch_name: str) -> None:
    # `git switch -c` if available; fall back to `checkout -b`.
    try:
        _run_git(root, ["switch", "-c", branch_name])
    except subprocess.CalledProcessError:
        _run_git(root, ["checkout", "-b", branch_name])


def _git_checkout(root: Path, branch_name: str) -> None:
    try:
        _run_git(root, ["switch", branch_name])
    except subprocess.CalledProcessError:
        _run_git(root, ["checkout", branch_name])


def _git_add(root: Path, file_path: str) -> None:
    _run_git(root, ["add", file_path])


def _git_commit(root: Path, message: str) -> None:
    _run_git(root, ["commit", "-m", message])


def _git_head_sha(root: Path) -> str:
    return _run_git(root, ["rev-parse", "HEAD"]).strip()


def _run_git(root: Path, args: list[str]) -> str:
    proc = subprocess.run(
        ["git", *args],
        cwd=str(root),
        capture_output=True,
        text=True,
        check=True,
    )
    return proc.stdout


# --- YAML field read/write ------------------------------------------


_PATH_TOKEN = re.compile(r"\.([^.\[]+)|\[(\d+)\]")


def _parse_field_path(path: str) -> list[Any]:
    """Parse a JSONPath-ish string into a list of (str | int) tokens.

    `$.procedure` -> ["procedure"]
    `$.critical_steps[2].desc` -> ["critical_steps", 2, "desc"]
    """
    if not path.startswith("$"):
        raise ApplyError(f"target_field_path must start with '$': {path}")
    tokens: list[Any] = []
    for m in _PATH_TOKEN.finditer(path):
        key, idx = m.group(1), m.group(2)
        if key is not None:
            tokens.append(key)
        elif idx is not None:
            tokens.append(int(idx))
    return tokens


def _read_field_from_yaml(yaml_text: str, path: str) -> str:
    """Read the field at `path` from `yaml_text` and return its string
    representation.

    Strings are returned verbatim. Non-strings raise ApplyError —
    the four allowed field classes are all string-typed.
    """
    parsed = yaml.safe_load(yaml_text)
    tokens = _parse_field_path(path)
    cursor: Any = parsed
    for tok in tokens:
        if isinstance(tok, str):
            if not isinstance(cursor, dict) or tok not in cursor:
                raise ApplyError(f"field path {path} not found in YAML")
            cursor = cursor[tok]
        else:
            if not isinstance(cursor, list) or tok >= len(cursor):
                raise ApplyError(f"field path {path} index out of range")
            cursor = cursor[tok]
    if cursor is None:
        return ""
    if not isinstance(cursor, str):
        raise ApplyError(
            f"field path {path} resolved to non-string ({type(cursor).__name__}); "
            "only string fields are mutable in v1"
        )
    return cursor


def _write_field_to_yaml(yaml_text: str, path: str, new_value: str) -> str:
    """Set the field at `path` to `new_value` and round-trip the YAML.

    Uses `yaml.safe_load` + `yaml.safe_dump` — comments are NOT
    preserved. v1 Skill YAMLs are comment-light; comment-preservation
    is a future-milestone concern.
    """
    parsed = yaml.safe_load(yaml_text)
    tokens = _parse_field_path(path)
    cursor: Any = parsed
    for tok in tokens[:-1]:
        if isinstance(tok, str):
            cursor = cursor[tok]
        else:
            cursor = cursor[tok]
    last = tokens[-1]
    if isinstance(last, str):
        cursor[last] = new_value
    else:
        cursor[last] = new_value
    return yaml.safe_dump(parsed, sort_keys=False, allow_unicode=True, width=10000)


# --- Port discovery + Spring spawn + health probe -------------------


def _find_free_port() -> int:
    """Ask the OS for a free TCP port by binding to 0 and reading back."""
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def _spawn_spring(root: Path, port: int) -> subprocess.Popen:
    """Spawn `mvn spring-boot:run` on the given port.

    The server's pom.xml lives at `server/pom.xml`. We invoke from
    repo root with `-pl server` so the multi-module build picks the
    server module.
    """
    cmd = [
        "mvn",
        "-q",
        "-pl",
        "server",
        "-am",
        "spring-boot:run",
        f"-Dspring-boot.run.arguments=--server.port={port}",
    ]
    return subprocess.Popen(
        cmd,
        cwd=str(root),
        env=os.environ.copy(),
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )


def _health_probe(
    *,
    port: int,
    timeout_seconds: int,
    interval_seconds: float,
    proc: subprocess.Popen,
) -> None:
    """Poll http://localhost:<port>/actuator/health every interval until
    200 + {"status": "UP"} or timeout.

    httpx is the canonical client for `eval_interactive`; we use it
    here too. If the import fails (minimal CI env), fall back to
    stdlib urllib for the probe.
    """
    deadline = time.monotonic() + timeout_seconds
    url = f"http://127.0.0.1:{port}/actuator/health"

    while time.monotonic() < deadline:
        # Cheap subprocess-alive check: if the spawn already exited,
        # the health-probe will never succeed.
        if proc.poll() is not None:
            raise SpringStartupTimeoutError(
                f"Spring subprocess exited prematurely (rc={proc.returncode}) "
                f"on port {port}"
            )
        if _probe_url_is_up(url):
            return
        time.sleep(interval_seconds)

    raise SpringStartupTimeoutError(
        f"Spring did not become healthy on port {port} within {timeout_seconds}s"
    )


def _probe_url_is_up(url: str) -> bool:
    try:
        import httpx  # type: ignore

        try:
            r = httpx.get(url, timeout=5.0)
            if r.status_code != 200:
                return False
            try:
                payload = r.json()
            except ValueError:
                payload = {}
            return payload.get("status") == "UP"
        except httpx.HTTPError:
            return False
    except ImportError:
        # Fallback: stdlib urllib.
        import json as _json
        import urllib.request as _ur

        try:
            req = _ur.Request(url)
            with _ur.urlopen(req, timeout=5.0) as resp:
                if resp.status != 200:
                    return False
                data = resp.read().decode("utf-8", errors="replace")
                try:
                    payload = _json.loads(data)
                except _json.JSONDecodeError:
                    payload = {}
                return payload.get("status") == "UP"
        except Exception:
            return False


def _terminate_process(proc: subprocess.Popen) -> None:
    """Kill the Spring subprocess + its descendants.

    `mvn spring-boot:run` typically spawns the JVM as a child of mvn;
    a naive `proc.terminate()` leaves the JVM alive. We use a
    process-group kill on POSIX.
    """
    if proc.poll() is not None:
        return
    try:
        if hasattr(os, "killpg"):
            os.killpg(os.getpgid(proc.pid), 15)  # SIGTERM
        else:
            proc.terminate()
        try:
            proc.wait(timeout=15)
        except subprocess.TimeoutExpired:
            if hasattr(os, "killpg"):
                os.killpg(os.getpgid(proc.pid), 9)  # SIGKILL
            else:
                proc.kill()
            proc.wait(timeout=10)
    except (ProcessLookupError, PermissionError, OSError):
        pass


def _build_commit_message(hypothesis: "Hypothesis", iteration_id: str) -> str:
    rationale = hypothesis.rationale.strip().replace("\n", " ")
    rationale_short = rationale[:80]
    return f"autoloop {iteration_id}: {rationale_short}"


# --- Compatibility -------------------------------------------------


def apply_proposal(*args, **kwargs):  # pragma: no cover - removed in S-Auto-3
    """Removed in S-Auto-3. The S-Auto-1 placeholder is replaced by
    `apply(...)` above; any code still calling `apply_proposal` is
    a stale import path.
    """
    raise NotImplementedError(
        "apply_proposal was the S-Auto-1 placeholder. Use apply(hypothesis, ...)."
    )


# Keep an unused but useful import for callers that want to confirm
# the binary exists; some CI environments mask mvn as a missing tool.
def has_maven() -> bool:
    return shutil.which("mvn") is not None
