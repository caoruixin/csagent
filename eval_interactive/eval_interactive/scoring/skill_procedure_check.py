"""Sprint 43 (S-Eval-2, NEW Milestone M3-Eval sub-sprint 2) — Tier-2
``skill_procedure_followship`` extractor.

This module implements the Python eval-side half of the Tier-2 Critical-flow
layer landed in the M3-Eval milestone. It loads the per-Skill
``critical_steps`` declarations from
``server/src/main/resources/skills/*.yaml`` (LLM-visible single source of
truth per the proposal §5 decision 5) and evaluates each step's
``trace_check`` DSL expression against a session trace, returning per-step
PASS / FAIL / N/A.

§1.7 STRUCTURAL DEFENCE
=======================

The ``trace_check`` DSL grammar is **intentionally minimal**. It supports
exactly six primitives and rejects, **at parse time**, any expression
that would constitute a §1.7 semantic hardcode (keyword / regex /
message-content match). This is the milestone's primary structural
guarantee that S-Eval-3 ``critical_steps`` content cannot accidentally
encode an if-else / keyword matrix / regex on user or bot message
content via the back door of the DSL.

The six frozen primitives are:

1. ``accumulated_tool_results.<tool>`` — presence check; true iff the
   trace's ``accumulated_tool_results`` map contains an entry for the
   named tool.
2. ``tool_event_seq(<tool_a>) < tool_event_seq(<tool_b>)`` — order
   check; true iff ``tool_a`` was successfully dispatched before
   ``tool_b`` in this case's tool-call timeline.
3. ``intake_state.fields_collected.contains(<field>)`` — slot presence
   check; true iff the per-turn ``intake_state.fields_collected`` list
   contains the named field.
4. ``session.<flag>_present`` — flag boolean; true iff the trace's
   ``session`` object exposes the named flag with a truthy value.
5. ``any_of(<expr1>, <expr2>, ...)`` — combinator; true iff any inner
   expression is true.
6. ``all_of(<expr1>, <expr2>, ...)`` — combinator; true iff every inner
   expression is true.

Anything else — ``message.contains(...)``, ``user_message.matches(...)``,
``re.search(...)``, list-membership against message content, a
``keyword_match(...)`` primitive — fails parse with
``TraceCheckDSLSyntaxError``. The structural defence test
``tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py`` asserts
the rejection set.

If S-Eval-3 surfaces a need for a new primitive, deliver-agent + human
decide at S-Eval-3 planning whether to extend the DSL within S-Eval-3's
Codex review boundary, or to scope-shift. S-Eval-2 does NOT widen the
DSL pre-emptively.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Iterable, Literal, Mapping, Sequence

import yaml

# ---------------------------------------------------------------------------
# Public dataclasses
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class CriticalStep:
    """Eval-side mirror of the Java ``CriticalStep`` record (Sprint 43)."""

    id: str
    desc: str
    trace_check: str
    mandatory_for: tuple[str, ...]
    severity: Literal["mandatory", "advisory"]


@dataclass(frozen=True)
class Skill:
    """Minimum-viable eval-side Skill projection — only the fields the
    extractor needs. Loaded by :func:`load_skills_from_dir`.
    """

    name: str
    applicable_use_cases: tuple[str, ...]
    critical_steps: tuple[CriticalStep, ...]


@dataclass
class CriticalStepResult:
    """Per-step outcome returned by :meth:`SkillProcedureExtractor.extract`.

    ``severity`` mirrors S-Eval-1 (D-2.5) on
    :class:`~eval_interactive.scoring.outcome_checks.OutcomeCheckResult` /
    :class:`~eval_interactive.scoring.hard_checks.HardCheckResult`. Tier-2
    composite wiring (``composite.py``) reads ``severity`` to decide
    whether a FAIL flips ``case_passed``.
    """

    step_id: str
    desc: str
    outcome: Literal["PASS", "FAIL", "N/A"]
    severity: Literal["mandatory", "advisory"]
    detail: str = ""


# ---------------------------------------------------------------------------
# DSL — parser
# ---------------------------------------------------------------------------


class TraceCheckDSLSyntaxError(ValueError):
    """Raised by :func:`parse_trace_check` for any expression that is not a
    valid combination of the six frozen primitives.

    The error message names the rejected primitive so an S-Eval-3 dev
    sees immediately which §1.7 surface their ``trace_check`` tripped.
    """


# Reserved names — primitives, combinators, and the two field-namespaces
# the grammar permits. Anything that LOOKS like a primitive (``.contains``,
# ``.matches``, ``re.match``, etc.) but is not on this list fails parse.
_PRIMITIVE_PREFIXES = (
    "accumulated_tool_results.",
    "intake_state.fields_collected.contains(",
    "tool_event_seq(",
    "session.",
)
_COMBINATORS = ("any_of", "all_of")

# Hardcode-flavoured fragments that the parser blocks BY NAME so that an
# S-Eval-3 dev sees a clear error rather than a generic "syntax error".
# The blocklist is illustrative, not exhaustive — the structural defence
# is the positive grammar (only the six primitives are accepted), not
# this list. The list exists to produce specific error messages for the
# §5 negative-test set.
_HARDCODE_BLOCKLIST = {
    "re.match",
    "re.search",
    "re.fullmatch",
    "re.findall",
    "re.compile",
    "keyword_match",
    "keyword_in",
    "contains_keyword",
    ".matches(",
    ".startswith(",
    ".endswith(",
    "message.contains(",
    "user_message.contains(",
    "bot_message.contains(",
    "user_message.matches(",
    "bot_message.matches(",
    "user_intent in [",
    "intent in [",
    "user_message in [",
}


@dataclass
class _Token:
    kind: str  # "IDENT" | "NUMBER" | "PUNCT" | "STRING" | "EOF"
    value: str
    pos: int


_TOKEN_RE = re.compile(
    r"\s+"
    r"|(?P<STRING>'[^']*'|\"[^\"]*\")"
    r"|(?P<IDENT>[A-Za-z_][A-Za-z0-9_-]*)"
    r"|(?P<PUNCT>[().<,>])"
)


def _tokenize(source: str) -> list[_Token]:
    tokens: list[_Token] = []
    pos = 0
    while pos < len(source):
        m = _TOKEN_RE.match(source, pos)
        if m is None:
            raise TraceCheckDSLSyntaxError(
                f"unexpected character {source[pos]!r} at position {pos} in {source!r}"
            )
        if m.group("STRING"):
            tokens.append(_Token("STRING", m.group("STRING")[1:-1], pos))
        elif m.group("IDENT"):
            tokens.append(_Token("IDENT", m.group("IDENT"), pos))
        elif m.group("PUNCT"):
            tokens.append(_Token("PUNCT", m.group("PUNCT"), pos))
        # else: whitespace — skip
        pos = m.end()
    tokens.append(_Token("EOF", "", pos))
    return tokens


# ---------------------------------------------------------------------------
# AST nodes — narrow set, one per primitive.
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class _AccumulatedToolResult:
    tool_name: str


@dataclass(frozen=True)
class _ToolEventSeqOrder:
    earlier_tool: str
    later_tool: str


@dataclass(frozen=True)
class _IntakeFieldCollected:
    field_name: str


@dataclass(frozen=True)
class _SessionFlagPresent:
    flag_name: str  # the bit before "_present"


@dataclass(frozen=True)
class _AnyOf:
    children: tuple[Any, ...]


@dataclass(frozen=True)
class _AllOf:
    children: tuple[Any, ...]


_AstNode = (
    _AccumulatedToolResult
    | _ToolEventSeqOrder
    | _IntakeFieldCollected
    | _SessionFlagPresent
    | _AnyOf
    | _AllOf
)


def parse_trace_check(source: str) -> _AstNode:
    """Parse a ``trace_check`` expression into an internal AST.

    Raises :class:`TraceCheckDSLSyntaxError` for any syntactic surface
    that does not match the six frozen primitives — including, by
    construction, every §1.7 hardcode-flavoured shape.
    """

    if not isinstance(source, str) or not source.strip():
        raise TraceCheckDSLSyntaxError("trace_check expression is empty")

    # Pre-scan — fail fast with a NAMED error on common §1.7 hardcode
    # shapes. The positive grammar would reject these anyway, but a
    # named error helps S-Eval-3 devs (and Codex per-sub-sprint review)
    # identify the §1.7 surface tripped.
    stripped = source.strip()
    for forbidden in _HARDCODE_BLOCKLIST:
        if forbidden in stripped:
            raise TraceCheckDSLSyntaxError(
                f"§1.7 forbidden primitive {forbidden!r} in trace_check {source!r}; "
                "regex / keyword / message-content matching is structurally rejected. "
                "Use one of: accumulated_tool_results.<tool>, "
                "tool_event_seq(<a>) < tool_event_seq(<b>), "
                "intake_state.fields_collected.contains(<field>), "
                "session.<flag>_present, any_of(...), all_of(...)."
            )

    tokens = _tokenize(source)
    parser = _Parser(tokens, source)
    node = parser.parse_expr()
    parser.expect_eof()
    return node


class _Parser:
    def __init__(self, tokens: Sequence[_Token], source: str) -> None:
        self.tokens = list(tokens)
        self.pos = 0
        self.source = source

    def peek(self) -> _Token:
        return self.tokens[self.pos]

    def consume(self) -> _Token:
        tok = self.tokens[self.pos]
        self.pos += 1
        return tok

    def expect(self, kind: str, value: str | None = None) -> _Token:
        tok = self.peek()
        if tok.kind != kind or (value is not None and tok.value != value):
            raise TraceCheckDSLSyntaxError(
                f"expected {kind}{'' if value is None else f' {value!r}'} but got "
                f"{tok.kind} {tok.value!r} at position {tok.pos} in {self.source!r}"
            )
        return self.consume()

    def expect_eof(self) -> None:
        tok = self.peek()
        if tok.kind != "EOF":
            raise TraceCheckDSLSyntaxError(
                f"trailing tokens after expression at position {tok.pos} in "
                f"{self.source!r}: {tok.value!r}"
            )

    def parse_expr(self) -> _AstNode:
        tok = self.peek()
        if tok.kind != "IDENT":
            raise TraceCheckDSLSyntaxError(
                f"expected identifier at position {tok.pos} in {self.source!r}, "
                f"got {tok.kind} {tok.value!r}"
            )

        head = tok.value
        if head in _COMBINATORS:
            return self._parse_combinator(head)
        if head == "accumulated_tool_results":
            return self._parse_accumulated_tool_results()
        if head == "tool_event_seq":
            return self._parse_tool_event_seq_order()
        if head == "intake_state":
            return self._parse_intake_field_collected()
        if head == "session":
            return self._parse_session_flag()

        raise TraceCheckDSLSyntaxError(
            f"unknown primitive {head!r} at position {tok.pos} in {self.source!r}; "
            "valid primitives are accumulated_tool_results, tool_event_seq, "
            "intake_state, session, any_of, all_of"
        )

    def _parse_combinator(self, name: str) -> _AstNode:
        self.expect("IDENT", name)
        self.expect("PUNCT", "(")
        children: list[_AstNode] = []
        children.append(self.parse_expr())
        while self.peek().kind == "PUNCT" and self.peek().value == ",":
            self.consume()
            children.append(self.parse_expr())
        self.expect("PUNCT", ")")
        if not children:
            raise TraceCheckDSLSyntaxError(
                f"{name}(...) requires at least one inner expression in {self.source!r}"
            )
        if name == "any_of":
            return _AnyOf(tuple(children))
        return _AllOf(tuple(children))

    def _parse_accumulated_tool_results(self) -> _AccumulatedToolResult:
        self.expect("IDENT", "accumulated_tool_results")
        self.expect("PUNCT", ".")
        tool = self.expect("IDENT").value
        return _AccumulatedToolResult(tool)

    def _parse_tool_event_seq_order(self) -> _ToolEventSeqOrder:
        # Grammar: tool_event_seq(<a>) "<" tool_event_seq(<b>)
        earlier = self._parse_tool_event_seq_call()
        self.expect("PUNCT", "<")
        later = self._parse_tool_event_seq_call()
        return _ToolEventSeqOrder(earlier, later)

    def _parse_tool_event_seq_call(self) -> str:
        self.expect("IDENT", "tool_event_seq")
        self.expect("PUNCT", "(")
        tool = self.expect("IDENT").value
        self.expect("PUNCT", ")")
        return tool

    def _parse_intake_field_collected(self) -> _IntakeFieldCollected:
        self.expect("IDENT", "intake_state")
        self.expect("PUNCT", ".")
        self.expect("IDENT", "fields_collected")
        self.expect("PUNCT", ".")
        self.expect("IDENT", "contains")
        self.expect("PUNCT", "(")
        # Accept either a bareword identifier or a quoted string.
        tok = self.peek()
        if tok.kind == "IDENT":
            self.consume()
            field_name = tok.value
        elif tok.kind == "STRING":
            self.consume()
            field_name = tok.value
        else:
            raise TraceCheckDSLSyntaxError(
                f"intake_state.fields_collected.contains(...) requires a field "
                f"identifier or quoted string at position {tok.pos} in "
                f"{self.source!r}"
            )
        self.expect("PUNCT", ")")
        return _IntakeFieldCollected(field_name)

    def _parse_session_flag(self) -> _SessionFlagPresent:
        self.expect("IDENT", "session")
        self.expect("PUNCT", ".")
        ident = self.expect("IDENT").value
        if not ident.endswith("_present"):
            raise TraceCheckDSLSyntaxError(
                f"session.<flag>_present must end with '_present' suffix; got "
                f"session.{ident!r} in {self.source!r}"
            )
        return _SessionFlagPresent(ident[: -len("_present")])


# ---------------------------------------------------------------------------
# DSL — evaluator
# ---------------------------------------------------------------------------


def evaluate_trace_check(node: _AstNode, trace_view: "TraceView") -> bool:
    """Evaluate a parsed AST against a :class:`TraceView`. Pure function."""

    if isinstance(node, _AccumulatedToolResult):
        return node.tool_name in trace_view.accumulated_tool_results
    if isinstance(node, _ToolEventSeqOrder):
        earlier_seq = trace_view.first_seq_index_for(node.earlier_tool)
        later_seq = trace_view.first_seq_index_for(node.later_tool)
        if earlier_seq is None or later_seq is None:
            return False
        return earlier_seq < later_seq
    if isinstance(node, _IntakeFieldCollected):
        return node.field_name in trace_view.intake_fields_collected
    if isinstance(node, _SessionFlagPresent):
        # "<flag>_present" — re-attach suffix for lookup; the trace may
        # expose either the bare flag (truthy semantic) or the
        # ``<flag>_present`` key explicitly. Both are accepted.
        full_key = f"{node.flag_name}_present"
        if full_key in trace_view.session:
            return bool(trace_view.session[full_key])
        return bool(trace_view.session.get(node.flag_name))
    if isinstance(node, _AnyOf):
        return any(evaluate_trace_check(c, trace_view) for c in node.children)
    if isinstance(node, _AllOf):
        return all(evaluate_trace_check(c, trace_view) for c in node.children)
    raise TraceCheckDSLSyntaxError(f"unrecognised AST node: {type(node).__name__}")


# ---------------------------------------------------------------------------
# TraceView — adapts the eval harness's per-turn trace shape to the DSL.
# ---------------------------------------------------------------------------


@dataclass
class TraceView:
    """Adapter wrapping a list of per-turn trace dicts (as produced by the
    eval harness — ``case_results[].per_turn_trace[]``) so the DSL has a
    stable, narrow interface to read against.

    ``per_turn_trace`` is a sequence of dicts, each carrying at least
    ``tool_calls`` (list of dicts with ``tool_name``, ``success``,
    ``sequence_index``) and ``projection`` (dict with ``session``,
    ``accumulated_tool_results``, optionally an ``intake_state`` nested
    under ``session`` or alongside it).
    """

    per_turn_trace: Sequence[Mapping[str, Any]]

    # ---- Tool dispatch view --------------------------------------------

    @property
    def successful_tool_events(self) -> list[Mapping[str, Any]]:
        events: list[Mapping[str, Any]] = []
        for turn in self.per_turn_trace:
            for call in turn.get("tool_calls", []) or []:
                if call.get("success", False):
                    events.append(call)
        return events

    def first_seq_index_for(self, tool_name: str) -> int | None:
        """Lowest ``sequence_index`` for a successful dispatch of ``tool_name``."""
        best: int | None = None
        for call in self.successful_tool_events:
            if call.get("tool_name") != tool_name:
                continue
            seq = call.get("sequence_index")
            if seq is None:
                continue
            if best is None or seq < best:
                best = seq
        return best

    # ---- Accumulated-tool-results view (final-turn snapshot) -----------

    @property
    def accumulated_tool_results(self) -> Mapping[str, Any]:
        """Return the union of every turn's ``accumulated_tool_results`` map.

        S-Auto-19 (#3): the union runs unconditionally across ALL turns,
        presence-preserving. The previous read returned the final turn's ATR
        verbatim when it was non-empty and only unioned when the final turn
        omitted the key entirely. That bypassed earlier-turn tool results
        whenever a later turn carried a *partial* ATR — e.g. the projection
        evicts ``search_knowledge`` from the final turn's ATR once
        ``resolve_article`` is in flight, so a ``search_knowledge`` that ran
        on turn 1 was invisible to ``search-knowledge-before-faq-answer``
        even though it was unambiguously called. Unconditional union keeps
        the membership semantics of the DSL (``tool in ATR``) honest: a tool
        that appeared in any turn's ATR is present; success/failure of a
        genuine never-called case is preserved because no turn ever lists it.
        Empty map if no turn provides one.
        """
        if not self.per_turn_trace:
            return {}
        merged: dict[str, Any] = {}
        for turn in self.per_turn_trace:
            proj = turn.get("projection") or {}
            atr = proj.get("accumulated_tool_results") or {}
            if isinstance(atr, Mapping):
                merged.update(atr)
        return merged

    # ---- Session view --------------------------------------------------

    @property
    def session(self) -> Mapping[str, Any]:
        if not self.per_turn_trace:
            return {}
        last = self.per_turn_trace[-1]
        proj = last.get("projection") or {}
        return proj.get("session") or {}

    # ---- Intake-state view ---------------------------------------------

    @property
    def intake_fields_collected(self) -> Sequence[str]:
        if not self.per_turn_trace:
            return ()
        # Walk turns last → first so we see the most up-to-date snapshot;
        # ``intake_state`` may live either at the top level of the
        # projection or nested under ``session``.
        for turn in reversed(self.per_turn_trace):
            proj = turn.get("projection") or {}
            intake_state = proj.get("intake_state")
            if intake_state is None:
                session = proj.get("session") or {}
                intake_state = session.get("intake_state")
            if isinstance(intake_state, Mapping):
                fields = intake_state.get("fields_collected")
                # S-Auto-19 (#4): the backend emits ``fields_collected`` as
                # a dict/object (field-name -> value), not a list. The old
                # read only accepted a list/tuple, so the membership view
                # collapsed to ``()`` and ``*-intake-complete-before-handover``
                # could never pass on a real trace. Read the dict's KEYS as
                # the collected-field set when it is a Mapping; keep the
                # list/tuple path for fixtures and any legacy list shape.
                # This is a shape read, not a success-rule change: a field
                # only appears if the backend actually recorded it.
                if isinstance(fields, Mapping):
                    return tuple(fields.keys())
                if isinstance(fields, (list, tuple)):
                    return tuple(fields)
        return ()


# ---------------------------------------------------------------------------
# Skill loader — embedded minimal pyYAML pointer.
# ---------------------------------------------------------------------------


def load_skills_from_dir(skills_dir: str | Path) -> list[Skill]:
    """Load all ``*.yaml`` files under ``skills_dir`` as :class:`Skill`
    projections. Only the fields the extractor needs
    (``name``, ``applicable_use_cases``, ``critical_steps``) are
    materialised; everything else on the YAML is ignored on purpose.

    Empty / absent ``critical_steps:`` block → ``Skill.critical_steps``
    is the empty tuple (the S-Eval-2 default; S-Eval-3 populates).

    Note: Java-side ``SkillLoader`` is the authoritative validator. This
    Python loader is intentionally permissive (skips entries with
    missing fields with a warning) so a broken YAML still permits the
    rest of the suite to run; the Java loader catches schema breaks at
    Spring bootstrap.
    """
    p = Path(skills_dir)
    if not p.is_dir():
        raise FileNotFoundError(f"skills directory not found: {p}")
    skills: list[Skill] = []
    for yaml_path in sorted(p.glob("*.yaml")):
        with yaml_path.open() as f:
            raw = yaml.safe_load(f) or {}
        name = raw.get("name") or yaml_path.stem
        ucs = tuple(raw.get("applicable_use_cases") or [])
        raw_steps = raw.get("critical_steps") or []
        steps: list[CriticalStep] = []
        for raw_step in raw_steps:
            if not isinstance(raw_step, Mapping):
                continue
            sid = raw_step.get("id")
            desc = raw_step.get("desc")
            tc = raw_step.get("trace_check")
            mf = raw_step.get("mandatory_for") or []
            sev = (raw_step.get("severity") or "").strip().lower()
            if not (sid and desc and tc and mf and sev in ("mandatory", "advisory")):
                continue
            steps.append(
                CriticalStep(
                    id=str(sid),
                    desc=str(desc),
                    trace_check=str(tc),
                    mandatory_for=tuple(str(u) for u in mf),
                    severity=sev,  # type: ignore[arg-type]
                )
            )
        skills.append(
            Skill(
                name=str(name),
                applicable_use_cases=ucs,
                critical_steps=tuple(steps),
            )
        )
    return skills


# ---------------------------------------------------------------------------
# Extractor — public entry point.
# ---------------------------------------------------------------------------


@dataclass
class SkillProcedureExtractor:
    """Tier-2 ``skill_procedure_followship`` extractor.

    Caller hands in a trace (per-turn list) and the active Skill, gets
    back a list of :class:`CriticalStepResult` — one per declared step on
    the Skill. Composite scoring (``composite.py``) then converts the
    list into a Tier-2 gate verdict via the
    :func:`tier2_results_to_gate` adapter.

    Pre-parses each step's ``trace_check`` at construction time so a
    malformed DSL surfaces at extractor build (not at per-case
    evaluation).
    """

    skills_by_name: Mapping[str, Skill]
    _ast_cache: dict[tuple[str, str], _AstNode] = field(default_factory=dict)

    def __post_init__(self) -> None:
        for skill in self.skills_by_name.values():
            for step in skill.critical_steps:
                # Fail-fast: parse every step's trace_check at construction
                # so a bad expression doesn't lurk until a case happens to
                # match.
                self._ast_cache[(skill.name, step.id)] = parse_trace_check(step.trace_check)

    @classmethod
    def from_skills(cls, skills: Iterable[Skill]) -> "SkillProcedureExtractor":
        return cls(skills_by_name={s.name: s for s in skills})

    @classmethod
    def from_skills_dir(cls, skills_dir: str | Path) -> "SkillProcedureExtractor":
        return cls.from_skills(load_skills_from_dir(skills_dir))

    def extract(
        self,
        trace: Sequence[Mapping[str, Any]],
        active_skill: Skill | str | None,
        active_use_case: str | None,
    ) -> list[CriticalStepResult]:
        """Run every ``critical_step`` on ``active_skill`` against ``trace``.

        Returns one :class:`CriticalStepResult` per declared step. A step
        whose ``mandatory_for`` UC list does NOT contain ``active_use_case``
        is N/A for this case (no gate effect either direction).
        """

        skill = self._resolve_skill(active_skill)
        if skill is None or not skill.critical_steps:
            return []

        trace_view = TraceView(per_turn_trace=trace)
        results: list[CriticalStepResult] = []
        for step in skill.critical_steps:
            if active_use_case is None or active_use_case not in step.mandatory_for:
                results.append(
                    CriticalStepResult(
                        step_id=step.id,
                        desc=step.desc,
                        outcome="N/A",
                        severity=step.severity,
                        detail=(
                            f"step not applicable to active_use_case="
                            f"{active_use_case!r}; mandatory_for="
                            f"{list(step.mandatory_for)}"
                        ),
                    )
                )
                continue
            ast = self._ast_cache[(skill.name, step.id)]
            try:
                passed = evaluate_trace_check(ast, trace_view)
            except TraceCheckDSLSyntaxError as e:
                # Should not happen — parse-time validation in __post_init__
                # should have caught it. Defensive surface.
                results.append(
                    CriticalStepResult(
                        step_id=step.id,
                        desc=step.desc,
                        outcome="FAIL",
                        severity=step.severity,
                        detail=f"DSL evaluation error: {e}",
                    )
                )
                continue
            results.append(
                CriticalStepResult(
                    step_id=step.id,
                    desc=step.desc,
                    outcome="PASS" if passed else "FAIL",
                    severity=step.severity,
                    detail=step.trace_check,
                )
            )
        return results

    def _resolve_skill(self, active_skill: Skill | str | None) -> Skill | None:
        if active_skill is None:
            return None
        if isinstance(active_skill, Skill):
            return active_skill
        return self.skills_by_name.get(active_skill)


# ---------------------------------------------------------------------------
# Composite adapter — converts extractor results into a Tier-2 gate result.
# ---------------------------------------------------------------------------


@dataclass
class Tier2Result:
    """Aggregate outcome of the Tier-2 ``skill_procedure_followship`` check.

    Mirrors the S-Eval-1 (D-2.5) severity convention so ``composite.py``
    can wire Tier-2 alongside Tier-0 / Tier-1 / Tier-3 with a single
    severity-driven gate model.
    """

    passed: bool
    severity: Literal["critical", "advisory"]
    failed_step_ids: list[str] = field(default_factory=list)
    per_step: list[CriticalStepResult] = field(default_factory=list)
    detail: str = ""


def tier2_results_to_gate(results: Sequence[CriticalStepResult]) -> Tier2Result:
    """Reduce a list of per-step results into a single Tier-2 gate verdict.

    Gate rule:

    - Empty list (no applicable steps, OR Skill has no ``critical_steps``)
      → PASS / advisory (does NOT flip ``case_passed``).
    - Any ``mandatory`` step with outcome FAIL → FAIL / critical (flips
      ``case_passed``).
    - All applicable steps PASS or N/A → PASS / critical.
    - Otherwise (only ``advisory`` steps failed) → PASS / advisory.
    """
    if not results:
        return Tier2Result(passed=True, severity="advisory", detail="no applicable critical steps")
    mandatory_fails = [
        r for r in results if r.severity == "mandatory" and r.outcome == "FAIL"
    ]
    advisory_fails = [
        r for r in results if r.severity == "advisory" and r.outcome == "FAIL"
    ]
    if mandatory_fails:
        return Tier2Result(
            passed=False,
            severity="critical",
            failed_step_ids=[r.step_id for r in mandatory_fails],
            per_step=list(results),
            detail=f"{len(mandatory_fails)} mandatory critical step(s) failed",
        )
    if advisory_fails:
        return Tier2Result(
            passed=True,
            severity="advisory",
            failed_step_ids=[r.step_id for r in advisory_fails],
            per_step=list(results),
            detail=f"{len(advisory_fails)} advisory critical step(s) failed (does not gate)",
        )
    return Tier2Result(
        passed=True,
        severity="critical",
        per_step=list(results),
        detail="all applicable mandatory critical steps passed",
    )
