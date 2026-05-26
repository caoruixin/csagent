"""Tests for `autoloop.sandbox.yaml_diff_validator`.

Fixture format: each `.diff` file under
`tests/fixtures/{valid,invalid}_diffs/` is itself a YAML document with
top-level keys:

    file_path: <repo-relative skill yaml path>
    expected_decision: ACCEPT | REJECT
    expected_reason_substring: <substring expected in result.reason>
    expected_changed_path_match: <optional; one path expected to be
                                  in accepted_paths or rejected_paths>
    before: <block scalar — the baseline YAML content>
    after: <block scalar — the proposed YAML content>

This format is convenient for human inspection (the file IS the
fixture) and avoids the parsing burden of unified-diff input.

Cross-Skill / cross-file diffs are the caller's responsibility per
the §3 fence #9 contract documented in `applier.py`. The
`test_cross_file_caller_rejects` test simulates a caller per-file
invocation and verifies that at least one of the two paths returns
REJECT.
"""

from __future__ import annotations

from pathlib import Path

import pytest
import yaml

from autoloop.sandbox import validate_skill_yaml_diff
from autoloop.sandbox.yaml_diff_validator import (
    ValidationResult,
    _diff_paths,
    _pattern_to_regex,
)

FIXTURES = Path(__file__).parent / "fixtures"


def _load_fixture(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def _all_fixtures(dirname: str) -> list[Path]:
    return sorted((FIXTURES / dirname).glob("*.diff"))


def _ids(paths: list[Path]) -> list[str]:
    return [p.stem for p in paths]


# --- Positive fixtures -----------------------------------------------


VALID_FIXTURES = _all_fixtures("valid_diffs")


@pytest.mark.parametrize("fixture_path", VALID_FIXTURES, ids=_ids(VALID_FIXTURES))
def test_positive_fixture_accepts(fixture_path: Path) -> None:
    fx = _load_fixture(fixture_path)
    result = validate_skill_yaml_diff(
        before_yaml=fx["before"],
        after_yaml=fx["after"],
        file_path=fx["file_path"],
    )
    assert result.decision == fx["expected_decision"], (
        f"{fixture_path.name}: expected {fx['expected_decision']} but got "
        f"{result.decision} (reason: {result.reason})"
    )
    if "expected_reason_substring" in fx:
        assert fx["expected_reason_substring"] in result.reason, (
            f"{fixture_path.name}: reason {result.reason!r} missing substring "
            f"{fx['expected_reason_substring']!r}"
        )
    if "expected_changed_path_match" in fx:
        # The accepted path may include an indexed form (e.g.
        # `$.critical_steps[0].desc` matches the `$.critical_steps[*].desc`
        # pattern); compare via the same matcher.
        pattern = fx["expected_changed_path_match"]
        regex = _pattern_to_regex(pattern)
        assert any(regex.match(p) for p in result.accepted_paths), (
            f"{fixture_path.name}: no accepted_paths matched {pattern}; "
            f"accepted={result.accepted_paths}"
        )


def test_positive_fixture_count_meets_contract() -> None:
    """The S-Auto-1 contract requires one positive fixture per allowed
    field class (4 classes). Catch regression if a fixture goes missing.
    """
    assert len(VALID_FIXTURES) >= 4, (
        f"expected >= 4 positive fixtures (one per allowed field class), "
        f"found {len(VALID_FIXTURES)}"
    )


# --- Negative fixtures -----------------------------------------------


INVALID_FIXTURES = _all_fixtures("invalid_diffs")


@pytest.mark.parametrize("fixture_path", INVALID_FIXTURES, ids=_ids(INVALID_FIXTURES))
def test_negative_fixture_rejects(fixture_path: Path) -> None:
    fx = _load_fixture(fixture_path)
    result = validate_skill_yaml_diff(
        before_yaml=fx["before"],
        after_yaml=fx["after"],
        file_path=fx["file_path"],
    )
    assert result.decision == "REJECT", (
        f"{fixture_path.name}: expected REJECT but got ACCEPT "
        f"(reason: {result.reason})"
    )
    if "expected_reason_substring" in fx:
        assert fx["expected_reason_substring"] in result.reason, (
            f"{fixture_path.name}: reason {result.reason!r} missing substring "
            f"{fx['expected_reason_substring']!r}"
        )


def test_negative_fixture_count_meets_contract() -> None:
    """S-Auto-1 contract requires >= 12 negative fixtures across the
    listed categories. Catch regression if fixtures go missing.
    """
    assert len(INVALID_FIXTURES) >= 12, (
        f"expected >= 12 negative fixtures, found {len(INVALID_FIXTURES)}"
    )


# --- Direct API tests (don't rely on fixtures) ------------------------


def test_diff_paths_empty_for_identical_dicts() -> None:
    assert _diff_paths({"a": 1, "b": 2}, {"a": 1, "b": 2}, "$") == []


def test_diff_paths_reports_scalar_change() -> None:
    assert _diff_paths({"a": 1}, {"a": 2}, "$") == ["$.a"]


def test_diff_paths_reports_added_key() -> None:
    paths = _diff_paths({"a": 1}, {"a": 1, "b": 2}, "$")
    assert paths == ["$.b"]


def test_diff_paths_reports_deleted_key() -> None:
    paths = _diff_paths({"a": 1, "b": 2}, {"a": 1}, "$")
    assert paths == ["$.b"]


def test_diff_paths_list_index_change() -> None:
    paths = _diff_paths({"xs": [1, 2, 3]}, {"xs": [1, 9, 3]}, "$")
    assert paths == ["$.xs[1]"]


def test_diff_paths_list_length_change_reports_extra_indices() -> None:
    paths = _diff_paths({"xs": [1, 2]}, {"xs": [1, 2, 3]}, "$")
    assert paths == ["$.xs[2]"]


def test_pattern_matcher_wildcard_index() -> None:
    rx = _pattern_to_regex("$.critical_steps[*].desc")
    assert rx.match("$.critical_steps[0].desc")
    assert rx.match("$.critical_steps[42].desc")
    assert not rx.match("$.critical_steps[0].trace_check")
    assert not rx.match("$.critical_steps[*].desc")  # literal `[*]` should not be path


def test_pattern_matcher_no_wildcard() -> None:
    rx = _pattern_to_regex("$.procedure")
    assert rx.match("$.procedure")
    assert not rx.match("$.procedure.nested")
    assert not rx.match("$.grounding_instruction")


# --- Direct semantic AST diff: byte differences with same AST -------


def test_byte_difference_with_same_ast_rejected_as_empty_diff() -> None:
    """A whitespace / formatting change that produces identical AST
    must REJECT with the empty-diff reason (per program.md §2 and
    `validate_skill_yaml_diff` step 6)."""
    before = (
        "name: discover_triage\n"
        "procedure: 'step one'\n"
    )
    after_whitespace = (
        "name:   discover_triage\n"
        "procedure: 'step one'\n"
    )
    r = validate_skill_yaml_diff(
        before_yaml=before,
        after_yaml=after_whitespace,
        file_path="server/src/main/resources/skills/discover_triage.yaml",
    )
    assert r.decision == "REJECT"
    assert "no whitelisted field changed" in r.reason


def test_procedure_edit_with_smuggled_mandatory_for_change_rejected() -> None:
    """Adversarial: a propose that LOOKS like a clean `procedure` edit
    but ALSO sneaks an extra `mandatory_for` entry. The validator
    must catch the smuggle because it diffs the AST, not the visible
    `procedure` line."""
    before = (
        "name: discover_triage\n"
        "procedure: 'step one'\n"
        "critical_steps:\n"
        "  - id: x\n"
        "    desc: 'd'\n"
        "    trace_check: 't'\n"
        "    mandatory_for: [UC-A]\n"
        "    severity: advisory\n"
    )
    after = (
        "name: discover_triage\n"
        "procedure: 'step one revised'\n"
        "critical_steps:\n"
        "  - id: x\n"
        "    desc: 'd'\n"
        "    trace_check: 't'\n"
        "    mandatory_for: [UC-A, UC-B]\n"
        "    severity: advisory\n"
    )
    r = validate_skill_yaml_diff(
        before_yaml=before,
        after_yaml=after,
        file_path="server/src/main/resources/skills/discover_triage.yaml",
    )
    assert r.decision == "REJECT"
    assert "mandatory_for" in r.reason


# --- Caller-layer responsibility: cross-file diff --------------------


def test_cross_file_caller_rejects() -> None:
    """Simulates a caller that received a bundle with two files. The
    validator API does not accept multi-file input; the applier
    (S-Auto-3) short-circuits before invoking the validator.

    This test simulates the per-file invocation that WOULD happen if
    the applier mistakenly looped instead of short-circuiting: at
    least one of the two file_paths is not in the mutable surface and
    the validator returns REJECT for it. This documents the contract
    for S-Auto-3 implementation: the cross-file fence is a caller
    invariant, but a defense-in-depth path through the validator
    still rejects out-of-surface files.
    """
    before_skill = "name: discover_triage\nprocedure: 'x'\n"
    after_skill = "name: discover_triage\nprocedure: 'y'\n"
    before_config = "feature: false\n"
    after_config = "feature: true\n"

    verdicts = [
        validate_skill_yaml_diff(
            before_yaml=before_skill,
            after_yaml=after_skill,
            file_path="server/src/main/resources/skills/discover_triage.yaml",
        ),
        validate_skill_yaml_diff(
            before_yaml=before_config,
            after_yaml=after_config,
            file_path="server/src/main/resources/config/runtime.yaml",
        ),
    ]
    decisions = [v.decision for v in verdicts]
    assert "REJECT" in decisions, (
        f"cross-file caller path failed: per-file verdicts={decisions}; "
        f"at least one out-of-surface file must REJECT"
    )
    out_of_surface_verdict = verdicts[1]
    assert out_of_surface_verdict.decision == "REJECT"
    assert "file outside mutable surface" in out_of_surface_verdict.reason


# --- ValidationResult shape ------------------------------------------


def test_validation_result_echoes_file_path() -> None:
    before = "name: x\nprocedure: 'a'\n"
    after = "name: x\nprocedure: 'b'\n"
    r: ValidationResult = validate_skill_yaml_diff(
        before_yaml=before,
        after_yaml=after,
        file_path="server/src/main/resources/skills/discover_triage.yaml",
    )
    assert r.file_path == "server/src/main/resources/skills/discover_triage.yaml"
    assert r.decision == "ACCEPT"
    assert r.accepted_paths == ["$.procedure"]
    assert r.rejected_paths == []
