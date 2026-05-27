"""Anti-hardcode auto-check — S-Auto-4 real detector.

Deterministic regex + heuristic detector. NO LLM call; stdlib only
(`re` + `unicodedata` + a small synonym map). Replaces the S-Auto-3
placeholder body without changing the function signature.

The detector covers the structural / auto-tractable subset of the
§4.1 nine-question kernel:

- Q1 — semantic hardcode patterns (IF/WHEN ... THEN/ELSE; enumerated
  OR-keywords; `.contains(...)` / `.matches(...)` literals).
- Q2 — Tier-0 invariant invention (MUST / NEVER / Runtime MUST style
  assertions against §1.3 soft-semantic dimensions).
- Q4 — explicit `cs<id>`-like tokens; `session_id=` / `case_id=` /
  `iteration_id=` tokens.
- Q5 — LLM-ownership-shrinking language; "force the assistant to";
  standalone MUST/NEVER without Tier-0 context (FLAG_FOR_CODEX).

D2 discipline (`docs/sprint_objective.md` §"Hard fences"):
  Rules encode GENERIC structural shapes only. No specific eval
  CaseSpec phrasing, user utterance, expected assistant answer, or
  case-success/failure label may appear as a literal in the rule set.
  Detector self-discipline regression test in
  `tests/test_anti_hardcode_check.py` greps this file for the
  forbidden literals.
"""

from __future__ import annotations

import re
import unicodedata
from dataclasses import dataclass
from typing import Any, Literal, TYPE_CHECKING

if TYPE_CHECKING:
    from ..meta_agent.proposer import Hypothesis


# ---------------------------------------------------------------------
# Public types
# ---------------------------------------------------------------------


@dataclass
class AntiHardcodeResult:
    """The verdict of `anti_hardcode_check`.

    Fields:
        verdict — one of "PASS", "FAIL", "FLAG_FOR_CODEX".
        rule_id — the first matching rule id (e.g.
            "Q1.if_then_decision_tree"); None on PASS.
        matched_substring — offending excerpt for audit (≤200 chars);
            None on PASS.
        placeholder — S-Auto-3 placeholder marker. S-Auto-4 real
            detector always returns False.
    """

    verdict: Literal["PASS", "FAIL", "FLAG_FOR_CODEX"]
    rule_id: str | None
    matched_substring: str | None
    placeholder: bool = False


# ---------------------------------------------------------------------
# Normalization
# ---------------------------------------------------------------------


_SYNONYM_MAP = {
    # equality obfuscation
    " equals ": " = ",
    " equal ": " = ",
    " is equal to ": " = ",
    # arrow obfuscation (decision-tree narrations)
    "->": "→",
    "=>": "→",
    "==>": "→",
    # IF/WHEN/THEN equivalence — collapse to canonical tokens.
    " when ": " if ",
    " whenever ": " if ",
}


def _normalize(text: str, *, synonym_map_enabled: bool) -> str:
    """NFKC + lowercase + whitespace-collapse; optional synonym map.

    Synonyms are off by default (calibration evidence drives the
    decision to enable). When enabled they apply AFTER NFKC + lower
    + whitespace collapse so the keys are matched against the same
    surface the regex rules see.
    """
    norm = unicodedata.normalize("NFKC", text)
    norm = norm.lower()
    norm = re.sub(r"\s+", " ", norm)
    if synonym_map_enabled:
        for src, dst in _SYNONYM_MAP.items():
            norm = norm.replace(src, dst)
    return norm


# ---------------------------------------------------------------------
# Rule helpers — each rule returns (matched_substring, severity) or None.
# All rules read the normalized `text`.
# ---------------------------------------------------------------------


_FAIL = "FAIL"
_FLAG = "FLAG_FOR_CODEX"


def _trim(s: str, n: int = 200) -> str:
    s = s.strip()
    return s[:n]


# Q1 — semantic hardcode patterns -------------------------------------

# IF/THEN decision tree. Captures both single-line and multi-line
# decomposition: "if <X> then <Y>" with up to ~120 chars of <X> across
# newlines / bullets / dashes.
_RE_Q1_IF_THEN = re.compile(
    r"\bif\b[\s\S]{1,120}?\bthen\b[\s\S]{1,120}",
    re.IGNORECASE,
)
# Arrow decision tree (post-normalization arrows collapse to →).
_RE_Q1_ARROW_TREE = re.compile(
    r"\bif\b[\s\S]{1,120}?→[\s\S]{1,120}",
    re.IGNORECASE,
)
# Enumerated OR-keywords (≥3 quoted-or-bare tokens joined by "or").
# Matches: 'X' or 'Y' or 'Z'  or  "X" or "Y" or "Z"  or  X or Y or Z
_RE_Q1_OR_KEYWORDS = re.compile(
    r"""(?ix)
    (?:['"]?[\w-]{2,}['"]?\s+or\s+){2,}
    ['"]?[\w-]{2,}['"]?
    """,
    re.VERBOSE | re.IGNORECASE,
)
# `.contains(...)` / `.matches(...)` literal — code-style pattern
# enumeration inside prose.
_RE_Q1_CONTAINS_MATCHES = re.compile(
    r"\.\s*(?:contains|matches|startswith|endswith)\s*\(",
    re.IGNORECASE,
)


def _q1_if_then(text: str) -> str | None:
    m = _RE_Q1_IF_THEN.search(text) or _RE_Q1_ARROW_TREE.search(text)
    return _trim(m.group(0)) if m else None


def _q1_or_keywords(text: str) -> str | None:
    m = _RE_Q1_OR_KEYWORDS.search(text)
    return _trim(m.group(0)) if m else None


def _q1_contains_matches(text: str) -> str | None:
    m = _RE_Q1_CONTAINS_MATCHES.search(text)
    return _trim(m.group(0)) if m else None


# Q2 — Tier-0 invariant invention attempts ----------------------------

# §1.3 LLM-owned soft-semantic dimensions. Used as the "subject" set
# Q2 / Q5 watch for assertions against. These are GENERIC names from
# the constitution, not specific eval-CaseSpec content.
_SOFT_SEMANTIC_DIMENSIONS = (
    "user goal",
    "issue relation",
    "use case",
    "use case hypothesis",
    "drift",
    "topic shift",
    "next action",
    "escalation",
    "response strategy",
    "wording",
)

# Tier-0-like phrasing: MUST always / MUST never / MUST reject /
# MUST escalate / Runtime MUST ... — high-confidence FAIL when the
# subject is a soft-semantic dimension above.
_RE_Q2_MUST_ALWAYS = re.compile(
    r"\b(?:runtime|the runtime|the bot|the system|the assistant)\s+must\s+(?:always|never|reject|escalate|refuse|block)\b[\s\S]{0,80}",
    re.IGNORECASE,
)
_RE_Q2_TIER0_PHRASE = re.compile(
    r"\btier-?0\b|\binvariant\b|\bhard\s+gate\b",
    re.IGNORECASE,
)


def _q2_must_always(text: str) -> str | None:
    m = _RE_Q2_MUST_ALWAYS.search(text)
    if not m:
        return None
    snippet = m.group(0).lower()
    for dim in _SOFT_SEMANTIC_DIMENSIONS:
        if dim in snippet:
            return _trim(m.group(0))
    return None


def _q2_tier0_invention(text: str) -> str | None:
    m = _RE_Q2_TIER0_PHRASE.search(text)
    if not m:
        return None
    # Within ~80 chars of the Tier-0-like phrase, look for an
    # "add" / "new" / "introduce" verb that implies inventing one.
    start = max(0, m.start() - 80)
    end = min(len(text), m.end() + 80)
    window = text[start:end]
    if re.search(r"\b(?:add|new|introduce|require)\b", window):
        return _trim(window)
    return None


# Q4 — case_id / session_id / iteration_id literals --------------------

# `cs<NNN>` / `cs[a-z0-9_]+` token — explicit eval CaseSpec id leakage.
# Whitelist: bare "cs" inside a known generic English word (e.g.
# "discussion" contains "cs"? no — `\bcs` is a word boundary so the
# pattern requires "cs" as the start of an identifier; this avoids
# matching within ordinary words).
_RE_Q4_CASE_ID_TOKEN = re.compile(
    r"\bcs[0-9a-z_]{2,}\b",
    re.IGNORECASE,
)
# Explicit identifier assignment.
_RE_Q4_ID_ASSIGN = re.compile(
    r"\b(?:session_id|case_id|iteration_id|exp_id)\b\s*[:=]",
    re.IGNORECASE,
)


def _q4_case_id_token(text: str) -> str | None:
    m = _RE_Q4_CASE_ID_TOKEN.search(text)
    return _trim(m.group(0)) if m else None


def _q4_id_assign(text: str) -> str | None:
    m = _RE_Q4_ID_ASSIGN.search(text)
    if not m:
        return None
    start = max(0, m.start() - 20)
    end = min(len(text), m.end() + 40)
    return _trim(text[start:end])


# Q5 — LLM-ownership-shrinking language --------------------------------

_RE_Q5_FORCE_ASSISTANT = re.compile(
    r"\bforce\s+(?:the\s+)?(?:assistant|bot|llm|model)\s+to\b[\s\S]{0,80}",
    re.IGNORECASE,
)
_RE_Q5_BOT_MUST_ALWAYS = re.compile(
    r"\b(?:the\s+)?(?:bot|assistant|llm|model)\s+must\s+(?:always|never)\s+(?:answer|respond|say|reply|escalate|refuse|use|emit)\b[\s\S]{0,80}",
    re.IGNORECASE,
)
_RE_Q5_DO_NOT_CONSIDER = re.compile(
    r"\b(?:do\s+not|don['’]t|never)\s+(?:consider|evaluate|reason\s+about|account\s+for)\b[\s\S]{0,80}",
    re.IGNORECASE,
)
# Standalone MUST / NEVER (without Runtime-like subject) — FLAG only.
_RE_Q5_STANDALONE_MUST = re.compile(
    r"(?:^|\s)(?:must|never)\s+(?:always|escalate|refuse|reject|block|emit)\b[\s\S]{0,40}",
    re.IGNORECASE,
)


def _q5_force_assistant(text: str) -> str | None:
    m = _RE_Q5_FORCE_ASSISTANT.search(text)
    return _trim(m.group(0)) if m else None


def _q5_bot_must_always(text: str) -> str | None:
    m = _RE_Q5_BOT_MUST_ALWAYS.search(text)
    return _trim(m.group(0)) if m else None


def _q5_do_not_consider(text: str) -> str | None:
    m = _RE_Q5_DO_NOT_CONSIDER.search(text)
    if not m:
        return None
    snippet = m.group(0).lower()
    for dim in _SOFT_SEMANTIC_DIMENSIONS:
        if dim in snippet:
            return _trim(m.group(0))
    return None


def _q5_standalone_must(text: str) -> str | None:
    m = _RE_Q5_STANDALONE_MUST.search(text)
    if not m:
        return None
    # If the broader Q2/Q5 high-confidence patterns also matched,
    # they will rank above this rule by id (Q2 < Q5.) — this rule is
    # the borderline FLAG_FOR_CODEX path.
    return _trim(m.group(0))


# ---------------------------------------------------------------------
# Rule registry
# ---------------------------------------------------------------------

# Rule order: alphabetical by rule_id. Final verdict = first matching
# rule's severity (deterministic across runs). FAIL outranks
# FLAG_FOR_CODEX; PASS only when no rule matches.
_RULES: tuple[tuple[str, str, Any], ...] = (
    ("Q1.contains_or_matches_literal", _FAIL, _q1_contains_matches),
    ("Q1.enumerated_or_keywords", _FAIL, _q1_or_keywords),
    ("Q1.if_then_decision_tree", _FAIL, _q1_if_then),
    ("Q2.must_always_against_soft_dimension", _FAIL, _q2_must_always),
    ("Q2.tier0_invariant_invention_attempt", _FAIL, _q2_tier0_invention),
    ("Q4.case_id_literal", _FAIL, _q4_case_id_token),
    ("Q4.id_assignment_literal", _FAIL, _q4_id_assign),
    ("Q5.bot_must_always", _FAIL, _q5_bot_must_always),
    ("Q5.do_not_consider_soft_dimension", _FAIL, _q5_do_not_consider),
    ("Q5.force_assistant_to", _FAIL, _q5_force_assistant),
    ("Q5.standalone_must_borderline", _FLAG, _q5_standalone_must),
)


# ---------------------------------------------------------------------
# Public entrypoint — signature preserved from S-Auto-3.
# ---------------------------------------------------------------------


def anti_hardcode_check(
    hypothesis: "Hypothesis",
    *,
    config: dict[str, Any] | None = None,
) -> AntiHardcodeResult:
    """Detect §1.7-forbidden structural patterns in a propose diff.

    The function is deterministic: same input → same output. No LLM
    call; no I/O; no dependency outside `re` + `unicodedata` + the
    small `_SYNONYM_MAP` above.

    Rules are evaluated in id-sorted order. The first FAIL short-
    circuits (its `rule_id` + `matched_substring` are returned). If
    no FAIL fires but a FLAG_FOR_CODEX fires, the verdict is
    FLAG_FOR_CODEX — observation-only in v1 (the loop attaches the
    flag to the iteration record but does NOT discard).
    """
    text = hypothesis.after_value or ""
    if not text.strip():
        # An empty after_value is a content_validator concern, not
        # an anti-hardcode concern. Let the upstream validator catch.
        return AntiHardcodeResult(
            verdict="PASS",
            rule_id=None,
            matched_substring=None,
            placeholder=False,
        )

    anti_cfg = ((config or {}).get("anti_hardcode") or {})
    synonym_enabled = bool(anti_cfg.get("synonym_map_enabled", False))
    normalized = _normalize(text, synonym_map_enabled=synonym_enabled)

    flag_pending: tuple[str, str] | None = None
    for rule_id, severity, fn in _RULES:
        match = fn(normalized)
        if match is None:
            continue
        if severity == _FAIL:
            return AntiHardcodeResult(
                verdict="FAIL",
                rule_id=rule_id,
                matched_substring=match,
                placeholder=False,
            )
        if severity == _FLAG and flag_pending is None:
            flag_pending = (rule_id, match)

    if flag_pending is not None:
        return AntiHardcodeResult(
            verdict="FLAG_FOR_CODEX",
            rule_id=flag_pending[0],
            matched_substring=flag_pending[1],
            placeholder=False,
        )

    return AntiHardcodeResult(
        verdict="PASS",
        rule_id=None,
        matched_substring=None,
        placeholder=False,
    )
