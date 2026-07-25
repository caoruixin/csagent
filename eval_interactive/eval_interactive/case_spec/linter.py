"""CaseSpec linter (Wave A2.2).

Detects policy / spec inconsistencies in CaseSpec YAML files. Read-only;
never modifies any spec on disk.

Designed to run on BOTH the new (post-Wave-A1.1) dataclass schema AND
legacy specs that still use the old field layout (``goal_summary``,
no ``allow_bot_resolution`` / ``bot_handling_pattern``). Legacy specs
emit an ``R0 legacy_field_name`` warning instead of crashing.

CLI:
    python -m eval_interactive.eval_interactive.case_spec.linter <path>

Exit code is non-zero if any rule of severity ``error`` fires.
"""

from __future__ import annotations

import argparse
import re
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Iterable, Literal, Optional

import yaml

from .policy_table import (
    _TOOL_ALLOWED_UCS,
    get_policy,
    list_human_only_tools,
)
from .schema import ESCALATION_TRIGGER_VALUES


Severity = Literal["error", "warning"]


# ---------------------------------------------------------------------------
# Public dataclasses
# ---------------------------------------------------------------------------


@dataclass
class LintViolation:
    """A single rule violation against a single CaseSpec."""

    case_id: str
    rule: str
    severity: Severity
    message: str
    field_path: Optional[str] = None


@dataclass
class _LintableSpec:
    """Best-effort parsed spec used by the linter.

    Holds the raw YAML dict plus convenience accessors. We deliberately do
    NOT instantiate the strict ``schema.CaseSpec`` because legacy specs
    are missing fields that ``Expected.__post_init__`` would reject.
    """

    case_id: str
    path: Path
    raw: dict
    legacy: bool = False  # True if uses pre-A1.1 field layout
    parse_error: Optional[str] = None
    extra_violations: list[LintViolation] = field(default_factory=list)


# ---------------------------------------------------------------------------
# Spec loading (tolerant)
# ---------------------------------------------------------------------------


def _load_spec_dict(path: Path) -> _LintableSpec:
    """Load a spec YAML in a way that tolerates the legacy schema."""

    try:
        with open(path, "r", encoding="utf-8") as fh:
            raw = yaml.safe_load(fh)
    except yaml.YAMLError as exc:  # pragma: no cover - defensive
        return _LintableSpec(
            case_id=path.stem,
            path=path,
            raw={},
            parse_error=f"YAML parse error: {exc}",
        )

    if not isinstance(raw, dict):
        return _LintableSpec(
            case_id=path.stem,
            path=path,
            raw={},
            parse_error="Top-level YAML is not a mapping",
        )

    case_id = raw.get("case_id", path.stem)
    persona = raw.get("persona", {}) or {}

    # Legacy = uses old field name ``goal_summary`` instead of
    # ``user_goal_summary``, OR is missing the new required fields.
    legacy = (
        "goal_summary" in persona
        or "user_goal_summary" not in persona
        or "allow_bot_resolution" not in (raw.get("expected") or {})
        or "bot_handling_pattern" not in (raw.get("expected") or {})
    )

    return _LintableSpec(case_id=case_id, path=path, raw=raw, legacy=legacy)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


_BOT_EXPECTATION_PATTERNS: tuple[re.Pattern[str], ...] = (
    re.compile(r"expects?\s+bot\s+to\s+resolve", re.IGNORECASE),
    re.compile(r"expects?\s+bot\s+to\s+escalate", re.IGNORECASE),
    re.compile(r"wants?\s+the\s+bot\s+to", re.IGNORECASE),
    re.compile(r"expects?\s+the\s+bot\s+to", re.IGNORECASE),
)

_RESOLVE_STYLE_PHRASES: tuple[str, ...] = (
    "I've fixed",
    "I have fixed",
    "I have sent",
    "I've sent",
    "I can restore",
    "I have restored",
    "I've restored",
    "I have deleted",
    "I've deleted",
    "I can delete",
    "I can ban",
)


def _normalise_uc(uc: str) -> str:
    """Strip the ``-NN`` suffix variant of a use case id."""

    key = (uc or "").strip()
    if key.count("-") >= 2:
        head, mid, *_ = key.split("-")
        key = f"{head}-{mid}"
    return key


def _allowed_ucs_for_tool(tool: str) -> Optional[frozenset[str]]:
    """Return policy-reachable UC set for ``tool`` (or None if unknown)."""

    return _TOOL_ALLOWED_UCS.get(tool)


def _tool_allowed_for_uc(tool: str, uc: str) -> bool:
    allowed = _allowed_ucs_for_tool(tool)
    if allowed is None:
        return False
    if "ALL" in allowed:
        return True
    return _normalise_uc(uc) in allowed


# ---------------------------------------------------------------------------
# Per-rule implementations
# ---------------------------------------------------------------------------


def _r0_legacy_field_name(spec: _LintableSpec) -> list[LintViolation]:
    if not spec.legacy:
        return []
    return [
        LintViolation(
            case_id=spec.case_id,
            rule="R0 legacy_field_name",
            severity="warning",
            message="spec uses pre-A1.1 schema; regenerate",
            field_path="persona.goal_summary | expected.allow_bot_resolution | expected.bot_handling_pattern",
        )
    ]


def _r1_uc_outcome_consistent(spec: _LintableSpec) -> list[LintViolation]:
    expected = spec.raw.get("expected") or {}
    primary_uc = expected.get("primary_uc")
    outcome = expected.get("outcome_class")
    if not primary_uc or outcome is None:
        return []
    try:
        policy = get_policy(primary_uc)
    except KeyError:
        return [
            LintViolation(
                case_id=spec.case_id,
                rule="R1 uc_outcome_consistent",
                severity="error",
                message=f"primary_uc {primary_uc!r} has no policy entry",
                field_path="expected.primary_uc",
            )
        ]
    if policy.outcome_class == "either":
        if outcome not in ("resolve", "escalate"):
            return [
                LintViolation(
                    case_id=spec.case_id,
                    rule="R1 uc_outcome_consistent",
                    severity="error",
                    message=(
                        f"primary_uc={primary_uc} policy=either; outcome_class "
                        f"must be resolve or escalate, got {outcome!r}"
                    ),
                    field_path="expected.outcome_class",
                )
            ]
        return []
    # WS-1 item 6 (replan §3 WS-1.6, closing hole §1.1 A2). A Wave-A4
    # carve-out used to sit here and return [] for the exact combination
    # (FAQ-resolvable UC + outcome escalate + should_escalate + a
    # ``request_handover`` in the sequence), on the rationale that transcript
    # evidence may legitimately turn a resolvable UC into a handover.
    #
    # That rationale is what silenced the corpus's single largest
    # contradiction. The corpus is by construction sessions that reached a
    # human, so the generator's escalation-evidence regex fires on nearly
    # every transcript (``case_outcome_resolver.py:196-206``,
    # ``transcript_evidence.py:146-208``) and stamps FAQ-resolvable UCs as
    # ``escalate`` — while the same specs keep ``allow_bot_resolution:
    # 'true'``. The carve-out made every one of those invisible, and the
    # linter was never wired into CI, so nothing ever surfaced them.
    #
    # The carve-out is removed. R1 now reports the contradiction. Per the
    # WS-1 scope this sub-sprint only makes them VISIBLE; rewriting the
    # affected specs is WS-2's job (replan §3 WS-2 "Bulk").
    if outcome != policy.outcome_class:
        return [
            LintViolation(
                case_id=spec.case_id,
                rule="R1 uc_outcome_consistent",
                severity="error",
                message=(
                    f"outcome_class={outcome!r} does not match policy for "
                    f"{primary_uc} (expected {policy.outcome_class!r})"
                ),
                field_path="expected.outcome_class",
            )
        ]
    return []


def _r2_uc_allow_bot_resolution_consistent(spec: _LintableSpec) -> list[LintViolation]:
    expected = spec.raw.get("expected") or {}
    primary_uc = expected.get("primary_uc")
    if not primary_uc:
        return []
    if "allow_bot_resolution" not in expected:
        # Legacy specs do not have this field; covered by R0.
        return []
    try:
        policy = get_policy(primary_uc)
    except KeyError:
        return []
    actual = expected.get("allow_bot_resolution")
    if actual != policy.allow_bot_resolution:
        return [
            LintViolation(
                case_id=spec.case_id,
                rule="R2 uc_allow_bot_resolution_consistent",
                severity="error",
                message=(
                    f"allow_bot_resolution={actual!r} != policy "
                    f"{policy.allow_bot_resolution!r} for {primary_uc}"
                ),
                field_path="expected.allow_bot_resolution",
            )
        ]
    return []


def _r3_escalation_trigger_required(spec: _LintableSpec) -> list[LintViolation]:
    expected = spec.raw.get("expected") or {}
    if not expected.get("should_escalate"):
        return []
    trigger = expected.get("escalation_trigger")
    if trigger is None or (isinstance(trigger, str) and not trigger.strip()):
        return [
            LintViolation(
                case_id=spec.case_id,
                rule="R3 escalation_trigger_required",
                severity="error",
                message="should_escalate=true but escalation_trigger is empty",
                field_path="expected.escalation_trigger",
            )
        ]
    if trigger not in ESCALATION_TRIGGER_VALUES:
        return [
            LintViolation(
                case_id=spec.case_id,
                rule="R3 escalation_trigger_required",
                severity="error",
                message=(
                    f"escalation_trigger={trigger!r} is not in canonical enum"
                ),
                field_path="expected.escalation_trigger",
            )
        ]
    return []


def _r4_escalation_trigger_forbidden(spec: _LintableSpec) -> list[LintViolation]:
    expected = spec.raw.get("expected") or {}
    if expected.get("should_escalate"):
        return []
    trigger = expected.get("escalation_trigger")
    if trigger is None:
        return []
    if isinstance(trigger, str) and not trigger.strip():
        return []
    return [
        LintViolation(
            case_id=spec.case_id,
            rule="R4 escalation_trigger_forbidden",
            severity="error",
            message=(
                f"should_escalate=false but escalation_trigger={trigger!r} is set"
            ),
            field_path="expected.escalation_trigger",
        )
    ]


def _r5_no_human_only_tool_in_forbidden(spec: _LintableSpec) -> list[LintViolation]:
    expected = spec.raw.get("expected") or {}
    forbidden = expected.get("forbidden_tools") or []
    human_only = set(list_human_only_tools())
    bad = [t for t in forbidden if t in human_only]
    if not bad:
        return []
    return [
        LintViolation(
            case_id=spec.case_id,
            rule="R5 no_human_only_tool_in_forbidden",
            severity="warning",
            message=(
                f"forbidden_tools includes human-only tools {bad!r}; these are "
                "global L1 concerns, drop them from per-case forbidden_tools"
            ),
            field_path="expected.forbidden_tools",
        )
    ]


def _r6_tool_sequence_within_uc_allowance(spec: _LintableSpec) -> list[LintViolation]:
    expected = spec.raw.get("expected") or {}
    primary_uc = expected.get("primary_uc")
    seq = expected.get("expected_tool_sequence") or []
    if not primary_uc:
        return []
    violations: list[LintViolation] = []
    for tool in seq:
        if _allowed_ucs_for_tool(tool) is None:
            # Tools that are not in tool-policy.yaml at all (e.g.
            # ``answer_grounded`` legacy pseudo-tool). Skip silently --
            # not the linter's concern; the scoring layer can flag it.
            continue
        if not _tool_allowed_for_uc(tool, primary_uc):
            violations.append(
                LintViolation(
                    case_id=spec.case_id,
                    rule="R6 tool_sequence_within_uc_allowance",
                    severity="error",
                    message=(
                        f"tool {tool!r} not allowed for primary_uc {primary_uc} "
                        f"per tool-policy.yaml"
                    ),
                    field_path="expected.expected_tool_sequence",
                )
            )
    return violations


def _r7_forbidden_tool_actually_reachable(spec: _LintableSpec) -> list[LintViolation]:
    expected = spec.raw.get("expected") or {}
    forbidden = expected.get("forbidden_tools") or []
    human_only = set(list_human_only_tools())
    violations: list[LintViolation] = []
    for tool in forbidden:
        if tool in human_only:
            # R5 already covered.
            continue
        allowed = _allowed_ucs_for_tool(tool)
        if allowed is None:
            violations.append(
                LintViolation(
                    case_id=spec.case_id,
                    rule="R7 forbidden_tool_actually_reachable",
                    severity="warning",
                    message=(
                        f"forbidden_tool {tool!r} is not policy-reachable by the "
                        "bot in any UC; remove it as irrelevant noise"
                    ),
                    field_path="expected.forbidden_tools",
                )
            )
    return violations


def _r8_user_goal_summary_neutral(spec: _LintableSpec) -> list[LintViolation]:
    persona = spec.raw.get("persona") or {}
    # Inspect both new and legacy field names so we still catch the
    # "Expects bot to..." anti-pattern in legacy specs.
    text = persona.get("user_goal_summary") or persona.get("goal_summary") or ""
    if not isinstance(text, str) or not text:
        return []
    for pattern in _BOT_EXPECTATION_PATTERNS:
        if pattern.search(text):
            field_name = (
                "persona.user_goal_summary"
                if "user_goal_summary" in persona
                else "persona.goal_summary"
            )
            return [
                LintViolation(
                    case_id=spec.case_id,
                    rule="R8 user_goal_summary_neutral",
                    severity="warning",
                    message=(
                        "user_goal_summary contains a bot-side expectation "
                        f"({pattern.pattern!r}); rewrite as a user goal"
                    ),
                    field_path=field_name,
                )
            ]
    return []


def _r9_bot_handling_pattern_required(spec: _LintableSpec) -> list[LintViolation]:
    expected = spec.raw.get("expected") or {}
    pattern = expected.get("bot_handling_pattern")
    if isinstance(pattern, str) and pattern.strip():
        return []
    # Legacy specs don't have this field at all -- still report as error.
    return [
        LintViolation(
            case_id=spec.case_id,
            rule="R9 bot_handling_pattern_required",
            severity="error",
            message="bot_handling_pattern is missing or empty",
            field_path="expected.bot_handling_pattern",
        )
    ]


_INTAKE_UCS = {"UC-G", "UC-H", "UC-I", "UC-J", "UC-K"}


def _r10_intake_fields_present_for_intake_uc(
    spec: _LintableSpec,
) -> list[LintViolation]:
    expected = spec.raw.get("expected") or {}
    primary_uc = _normalise_uc(expected.get("primary_uc") or "")
    outcome = expected.get("outcome_class")
    if primary_uc not in _INTAKE_UCS or outcome != "escalate":
        return []
    scoring = spec.raw.get("scoring") or {}
    outcome_checks = scoring.get("outcome_checks") or []
    has_intake_check = "intake_fields_collected" in outcome_checks
    has_dedicated_field = bool(expected.get("required_intake_fields")) or bool(
        expected.get("intake_fields")
    )
    if has_intake_check or has_dedicated_field:
        return []
    return [
        LintViolation(
            case_id=spec.case_id,
            rule="R10 intake_fields_present_for_intake_uc",
            severity="error",
            message=(
                f"primary_uc={primary_uc} with outcome=escalate but spec does not "
                "reference intake fields (scoring.outcome_checks lacks "
                "intake_fields_collected and no required_intake_fields field)"
            ),
            field_path="scoring.outcome_checks",
        )
    ]


def _r11_hidden_fact_not_duplicating_form(spec: _LintableSpec) -> list[LintViolation]:
    persona = spec.raw.get("persona") or {}
    form = spec.raw.get("form_context") or {}
    form_values: list[str] = []
    for key in ("first_name", "email", "topic_subject", "ad_id", "description"):
        v = form.get(key)
        if isinstance(v, str) and v.strip():
            form_values.append(v.strip().lower())
    violations: list[LintViolation] = []
    for hf in persona.get("hidden_facts") or []:
        if not isinstance(hf, dict):
            continue
        fact_text = (hf.get("fact") or "").strip().lower()
        if not fact_text:
            continue
        for fv in form_values:
            if fv and fv in fact_text:
                violations.append(
                    LintViolation(
                        case_id=spec.case_id,
                        rule="R11 hidden_fact_not_duplicating_form",
                        severity="warning",
                        message=(
                            f"hidden_fact {hf.get('fact')!r} duplicates a "
                            f"form_context value ({fv!r}); redundant"
                        ),
                        field_path="persona.hidden_facts",
                    )
                )
                break
    return violations


def _r12_answer_must_not_contain_nonempty_for_escalate(
    spec: _LintableSpec,
) -> list[LintViolation]:
    expected = spec.raw.get("expected") or {}
    if expected.get("outcome_class") != "escalate":
        return []
    forbidden_phrases = expected.get("answer_must_not_contain") or []
    joined = " ".join(p for p in forbidden_phrases if isinstance(p, str)).lower()
    if any(rp.lower() in joined for rp in _RESOLVE_STYLE_PHRASES):
        return []
    return [
        LintViolation(
            case_id=spec.case_id,
            rule="R12 answer_must_not_contain_nonempty_for_escalate",
            severity="warning",
            message=(
                "outcome_class=escalate but answer_must_not_contain has no "
                "resolve-style phrase (e.g. \"I've fixed\", \"I have sent\")"
            ),
            field_path="expected.answer_must_not_contain",
        )
    ]


_PER_SPEC_RULES = (
    _r0_legacy_field_name,
    _r1_uc_outcome_consistent,
    _r2_uc_allow_bot_resolution_consistent,
    _r3_escalation_trigger_required,
    _r4_escalation_trigger_forbidden,
    _r5_no_human_only_tool_in_forbidden,
    _r6_tool_sequence_within_uc_allowance,
    _r7_forbidden_tool_actually_reachable,
    _r8_user_goal_summary_neutral,
    _r9_bot_handling_pattern_required,
    _r10_intake_fields_present_for_intake_uc,
    _r11_hidden_fact_not_duplicating_form,
    _r12_answer_must_not_contain_nonempty_for_escalate,
)


# ---------------------------------------------------------------------------
# Public per-spec API
# ---------------------------------------------------------------------------


def lint_case_spec(spec: Any) -> list[LintViolation]:
    """Run all per-spec lint rules against ``spec``.

    Accepts either:
    * a ``_LintableSpec`` (used by the CLI / file helper),
    * a raw dict (the parsed YAML body), or
    * a ``schema.CaseSpec`` dataclass instance — converted to dict via
      ``dataclasses.asdict``.
    """

    if isinstance(spec, _LintableSpec):
        lintable = spec
    elif isinstance(spec, dict):
        lintable = _LintableSpec(
            case_id=spec.get("case_id", "<unknown>"),
            path=Path(""),
            raw=spec,
            legacy=(
                "goal_summary" in (spec.get("persona") or {})
                or "user_goal_summary" not in (spec.get("persona") or {})
            ),
        )
    else:
        # Treat as a CaseSpec dataclass — best-effort serialise.
        from dataclasses import asdict, is_dataclass

        if not is_dataclass(spec):
            raise TypeError(f"Unsupported spec type: {type(spec)!r}")
        raw = asdict(spec)
        lintable = _LintableSpec(
            case_id=raw.get("case_id", "<unknown>"),
            path=Path(""),
            raw=raw,
            legacy=False,
        )

    out: list[LintViolation] = []
    if lintable.parse_error:
        out.append(
            LintViolation(
                case_id=lintable.case_id,
                rule="R-parse parse_error",
                severity="error",
                message=lintable.parse_error,
                field_path=None,
            )
        )
        return out

    for rule_fn in _PER_SPEC_RULES:
        try:
            out.extend(rule_fn(lintable))
        except Exception as exc:  # pragma: no cover - defensive
            out.append(
                LintViolation(
                    case_id=lintable.case_id,
                    rule=f"{rule_fn.__name__} (rule_error)",
                    severity="error",
                    message=f"linter rule crashed: {exc!r}",
                )
            )
    out.extend(lintable.extra_violations)
    return out


def lint_spec_file(path: Path) -> list[LintViolation]:
    """Load a YAML spec from ``path`` and lint it."""

    spec = _load_spec_dict(Path(path))
    return lint_case_spec(spec)


# ---------------------------------------------------------------------------
# Cross-spec rule(s)
# ---------------------------------------------------------------------------


def _r13_source_session_no_duplication(
    specs: Iterable[_LintableSpec],
    subset_pairs: Optional[frozenset[tuple[str, str]]] = None,
) -> list[LintViolation]:
    """Detect a source_session_id appearing in multiple specs.

    ``subset_pairs`` allows the caller to declare that one bucket is a
    SUBSET of another (e.g. ``smoke`` is a subset of ``anchor`` /
    ``promotion`` / ``exploration``), in which case duplicate
    source_session_ids that span ONLY ``(subset, parent)`` pairs are
    expected and not flagged. Each pair is the ``(subset_bucket,
    parent_bucket)`` tuple of directory names as derived from the spec's
    ``path.parent.name``. Specs with no parent directory name (e.g.
    ad-hoc files) are treated as in their own bucket.
    """

    subset_pairs = subset_pairs or frozenset()
    # Index: session_id -> list of (case_id, bucket_name).
    by_session: dict[str, list[tuple[str, str]]] = defaultdict(list)
    for s in specs:
        sid = (s.raw.get("source_session_id") or "").strip()
        if not sid:
            continue
        bucket = s.path.parent.name if s.path else ""
        by_session[sid].append((s.case_id, bucket))

    out: list[LintViolation] = []
    for sid, entries in by_session.items():
        if len(entries) <= 1:
            continue
        # Are all duplicate cross-bucket pairs explained by subset_pairs?
        buckets = sorted({b for _, b in entries})
        all_pairs_allowed = True
        if len(buckets) > 1 and subset_pairs:
            for i in range(len(buckets)):
                for j in range(len(buckets)):
                    if i == j:
                        continue
                    pair = (buckets[i], buckets[j])
                    if pair in subset_pairs or (buckets[j], buckets[i]) in subset_pairs:
                        continue
                    # This bucket pair is NOT allowed -> we must flag.
                    all_pairs_allowed = False
                    break
                if not all_pairs_allowed:
                    break
        else:
            all_pairs_allowed = False

        # Even if cross-bucket is allowed, two cases in the SAME bucket
        # sharing a session_id is still a real duplication bug.
        per_bucket: dict[str, list[str]] = defaultdict(list)
        for cid, bucket in entries:
            per_bucket[bucket].append(cid)
        same_bucket_dups = [
            (b, ids) for b, ids in per_bucket.items() if len(ids) > 1
        ]

        if all_pairs_allowed and not same_bucket_dups:
            continue

        case_ids = [cid for cid, _ in entries]
        for cid, _ in entries:
            out.append(
                LintViolation(
                    case_id=cid,
                    rule="R13 source_session_no_duplication",
                    severity="error",
                    message=(
                        f"source_session_id={sid!r} appears in multiple specs: "
                        f"{sorted(case_ids)!r}"
                    ),
                    field_path="source_session_id",
                )
            )
    return out


# ---------------------------------------------------------------------------
# Directory walker + report
# ---------------------------------------------------------------------------


def _collect_spec_files(root: Path) -> list[Path]:
    if root.is_file():
        return [root]
    files: list[Path] = []
    for ext in ("*.yaml", "*.yml"):
        files.extend(sorted(root.rglob(ext)))
    return files


def _format_markdown_report(
    root: Path,
    file_violations: dict[Path, list[LintViolation]],
    cross_spec_violations: list[LintViolation],
    spec_count: int,
) -> str:
    total_errors = 0
    total_warnings = 0
    rule_counter: dict[str, int] = defaultdict(int)
    for vs in file_violations.values():
        for v in vs:
            if v.severity == "error":
                total_errors += 1
            else:
                total_warnings += 1
            rule_counter[v.rule] += 1
    for v in cross_spec_violations:
        if v.severity == "error":
            total_errors += 1
        else:
            total_warnings += 1
        rule_counter[v.rule] += 1

    lines: list[str] = []
    lines.append(f"# CaseSpec Lint Report")
    lines.append("")
    lines.append(f"- Root: `{root}`")
    lines.append(f"- Specs scanned: **{spec_count}**")
    lines.append(f"- Errors: **{total_errors}**")
    lines.append(f"- Warnings: **{total_warnings}**")
    lines.append("")
    lines.append("## Rule frequency")
    lines.append("")
    if rule_counter:
        lines.append("| Rule | Count |")
        lines.append("| --- | ---: |")
        for rule, count in sorted(
            rule_counter.items(), key=lambda kv: (-kv[1], kv[0])
        ):
            lines.append(f"| {rule} | {count} |")
    else:
        lines.append("_no violations_")
    lines.append("")

    if cross_spec_violations:
        lines.append("## Cross-spec violations")
        lines.append("")
        for v in cross_spec_violations:
            lines.append(
                f"- **{v.severity.upper()}** `{v.rule}` "
                f"(case_id=`{v.case_id}`, field=`{v.field_path}`): {v.message}"
            )
        lines.append("")

    lines.append("## Per-case violations")
    lines.append("")

    # Group by case_id for stable ordering.
    by_case: dict[str, list[tuple[Path, LintViolation]]] = defaultdict(list)
    for path, vs in file_violations.items():
        for v in vs:
            by_case[v.case_id].append((path, v))

    if not by_case:
        lines.append("_no per-case violations_")
        return "\n".join(lines) + "\n"

    for case_id in sorted(by_case.keys()):
        entries = by_case[case_id]
        path = entries[0][0]
        lines.append(f"### {case_id}")
        lines.append(f"- File: `{path}`")
        lines.append("")
        for _, v in entries:
            field_str = f" `{v.field_path}`" if v.field_path else ""
            lines.append(
                f"  - **{v.severity.upper()}** `{v.rule}`{field_str}: {v.message}"
            )
        lines.append("")

    return "\n".join(lines) + "\n"


def lint_directory(
    root: Path,
    subset_pairs: Optional[frozenset[tuple[str, str]]] = None,
) -> tuple[dict[Path, list[LintViolation]], list[LintViolation], int]:
    """Lint every spec under ``root`` (recursive). Returns

    ``(per_file_violations, cross_spec_violations, total_specs)``.

    ``subset_pairs`` -- optional set of ``(subset_bucket, parent_bucket)``
    pairs indicating that the subset bucket is allowed to share
    ``source_session_id`` values with its parent bucket(s). See
    ``_r13_source_session_no_duplication`` for the exact semantics.
    """

    files = _collect_spec_files(root)
    per_file: dict[Path, list[LintViolation]] = {}
    lintables: list[_LintableSpec] = []
    for path in files:
        spec = _load_spec_dict(path)
        lintables.append(spec)
        per_file[path] = lint_case_spec(spec)
    cross = _r13_source_session_no_duplication(lintables, subset_pairs=subset_pairs)
    return per_file, cross, len(lintables)


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def _has_errors(
    per_file: dict[Path, list[LintViolation]],
    cross_spec: list[LintViolation],
) -> bool:
    for vs in per_file.values():
        if any(v.severity == "error" for v in vs):
            return True
    return any(v.severity == "error" for v in cross_spec)


def _parse_subset_of(value: Optional[str]) -> frozenset[tuple[str, str]]:
    """Parse the ``--subset-of`` CLI argument.

    Format: comma-separated ``subset:parent`` tokens, e.g.
    ``smoke:anchor,smoke:promotion,smoke:exploration``. Whitespace
    around tokens is tolerated. Returns an empty set when ``value`` is
    None or blank.
    """

    if not value:
        return frozenset()
    pairs: list[tuple[str, str]] = []
    for raw in value.split(","):
        token = raw.strip()
        if not token:
            continue
        if ":" not in token:
            raise ValueError(
                f"--subset-of token {token!r} is not in 'subset:parent' form"
            )
        subset, parent = token.split(":", 1)
        subset = subset.strip()
        parent = parent.strip()
        if not subset or not parent:
            raise ValueError(
                f"--subset-of token {token!r} has an empty subset or parent name"
            )
        pairs.append((subset, parent))
    return frozenset(pairs)


def main(argv: Optional[list[str]] = None) -> int:
    parser = argparse.ArgumentParser(
        prog="python -m eval_interactive.eval_interactive.case_spec.linter",
        description="Lint CaseSpec YAML files for policy/spec inconsistencies.",
    )
    parser.add_argument(
        "path",
        type=str,
        help="File or directory to lint (recursive for directories).",
    )
    parser.add_argument(
        "--out",
        type=str,
        default=None,
        help="Write the markdown report to this path instead of stdout.",
    )
    parser.add_argument(
        "--subset-of",
        dest="subset_of",
        type=str,
        default=None,
        help=(
            "Comma-separated 'subset:parent' bucket pairs (directory names) "
            "that are allowed to share source_session_id values without "
            "tripping R13 source_session_no_duplication. Example: "
            "'smoke:anchor,smoke:promotion,smoke:exploration'."
        ),
    )
    args = parser.parse_args(argv)

    try:
        subset_pairs = _parse_subset_of(args.subset_of)
    except ValueError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2

    root = Path(args.path)
    if not root.exists():
        print(f"error: path does not exist: {root}", file=sys.stderr)
        return 2

    per_file, cross, total = lint_directory(root, subset_pairs=subset_pairs)
    report = _format_markdown_report(root, per_file, cross, total)

    if args.out:
        out_path = Path(args.out)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(report, encoding="utf-8")
        print(f"wrote report to {out_path}")
    else:
        print(report)

    return 1 if _has_errors(per_file, cross) else 0


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main())
