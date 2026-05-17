"""Wave A6.1 shadow audit: ask DeepSeek v4 to review CaseSpec persona fields.

This is a SHADOW AUDIT script. It does NOT modify any production extractor
code, any CaseSpec YAML, the override file, or any test. Its only outputs
are two report files in qa-reports/ that the maintainer reviews before we
wire the LLM layer into ``extractor.py`` in waves A6.2/A6.3.

Wave A6.2 refactor: prompt, validators, and DeepSeek client now live in
``eval_interactive.case_spec.llm_persona_reviewer`` -- this script imports
those names so the production extractor and the shadow audit always use
the same canonical prompt template (sha256 surfaced in the run report).

Pipeline per session:
    1. Load every CaseSpec YAML under
       ``eval_interactive/case_specs/{anchor,promotion,exploration,smoke}/``.
       Deduplicate by ``source_session_id`` (smoke specs duplicate anchor
       specs).
    2. For each unique spec, look up the source turns by ``source_dataset``
       (table mirrors ``llm_persona_reviewer.SOURCE_DATASET_TURNS_FILE``)
       and ``source_session_id``. Skip cleanly if no transcript turns are
       found.
    3. Build the prompt via ``llm_persona_reviewer.render_user_prompt``.
    4. Call DeepSeek (model from ``DEFAULT_DEEPSEEK_MODEL`` / ``DEEPSEEK_MODEL`` env)
       with temperature=0 and ``response_format={"type":"json_object"}``.
       Retry on 5xx / network errors with exponential backoff (max 3
       retries).
    5. Validate the response strictly using
       ``llm_persona_reviewer.validate_response``.
       Any failure is recorded but does not crash the run.
    6. Write side-by-side reports to:
         - qa-reports/llm-persona-review.md   (human-readable)
         - qa-reports/llm-persona-review.yaml (machine-readable)

Concurrency is capped at 5 simultaneous DeepSeek calls. A 100ms jitter
sits between launches. Total calls are hard-capped at 500.

CLI:
    python -m eval_interactive.scripts.llm_review_specs \\
        --limit 3                          # dry-run on first N sessions
        --session-id 570Q5000008hx9tIAA    # single-session run
        --out-md qa-reports/llm-persona-review.md
        --out-yaml qa-reports/llm-persona-review.yaml
        --dry-run                          # render prompt, skip API call
"""

from __future__ import annotations

import argparse
import asyncio
import dataclasses
import datetime as _dt
import hashlib
import json
import logging
import os
import random
import re
import sys
import time
from collections import Counter
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Iterable

import httpx
import yaml

# ---------------------------------------------------------------------------
# Module wiring -- importable as both "python -m eval_interactive.scripts..."
# and as a plain script. Mirrors ``regenerate_case_specs.py``.
# ---------------------------------------------------------------------------

_REPO_ROOT = Path(__file__).resolve().parents[2]
if str(_REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(_REPO_ROOT))


# ---------------------------------------------------------------------------
# Static config -- prompt, validators, and source-dataset map come from the
# canonical reviewer module.
# ---------------------------------------------------------------------------

from eval_interactive.case_spec.llm_persona_reviewer import (  # noqa: E402
    ALLOWED_RESPONSE_KEYS,
    BOT_EXPECTATION_PATTERNS,
    DEFAULT_DEEPSEEK_MODEL,
    PROMPT_SYSTEM,
    PROMPT_TEMPLATE_SHA256,
    PersonaDraft,
    SOURCE_DATASET_TURNS_FILE,
    USER_GOAL_SUMMARY_MAX_CHARS,
    all_messages as _all_messages_shared,
    render_user_prompt as _render_user_prompt_shared,
    shared_identifier as _shared_identifier,
    validate_response as _validate_response_shared,
    verbatim_in_visitor as _verbatim_in_visitor,
    visitor_messages as _visitor_messages_shared,
    word_overlap as _word_overlap_shared,
)

LOG = logging.getLogger("llm_review_specs")

DEEPSEEK_ENDPOINT = (
    os.environ.get("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1") + "/chat/completions"
)
DEEPSEEK_MODEL = DEFAULT_DEEPSEEK_MODEL  # back-compat alias

CASE_SET_DIRS = ("anchor", "promotion", "exploration", "smoke")
DEFAULT_CASE_SPEC_ROOT = _REPO_ROOT / "eval_interactive" / "case_specs"
DEFAULT_TURNS_DIR = _REPO_ROOT / "data" / "eval_datasets"
DEFAULT_OUT_MD = _REPO_ROOT / "qa-reports" / "llm-persona-review.md"
DEFAULT_OUT_YAML = _REPO_ROOT / "qa-reports" / "llm-persona-review.yaml"

CALL_BUDGET = 500       # hard cap (sanity guard)
CONCURRENCY = 5
JITTER_SECONDS = 0.1
HTTP_TIMEOUT = 90.0
MAX_RETRIES = 3


# ---------------------------------------------------------------------------
# Prompt template -- the PROMPT_SYSTEM constant and per-session renderer
# come from llm_persona_reviewer (the canonical source of truth).
# ---------------------------------------------------------------------------


def render_user_prompt(
    *,
    spec: dict[str, Any],
    turns: list[dict[str, str]],
    turns_filename: str,
) -> str:
    """Render the per-session user-prompt block by adapting a CaseSpec dict
    into the structured arguments expected by the shared renderer."""
    expected = spec.get("expected", {}) or {}
    persona = spec.get("persona", {}) or {}
    form = spec.get("form_context", {}) or {}

    drift_type = persona.get("drift_behavior", "none") or "none"
    frustration_level = persona.get("frustration_level", "none") or "none"
    has_frustration = frustration_level != "none"

    rule_seeds = list(persona.get("seed_messages") or [])
    rule_hidden = [
        {
            "fact": (hf.get("fact") or "").strip(),
            "disclose_when": (hf.get("disclose_when") or "").strip(),
        }
        for hf in (persona.get("hidden_facts") or [])
        if isinstance(hf, dict)
    ]
    rule_draft = PersonaDraft(
        seed_messages=rule_seeds,
        user_goal_summary=persona.get("user_goal_summary", "") or "",
        hidden_facts=rule_hidden,
        verbosity=persona.get("verbosity", "normal") or "normal",
    )

    return _render_user_prompt_shared(
        source_session_id=str(spec.get("source_session_id", "")),
        source_dataset=str(spec.get("source_dataset", "")),
        primary_uc=str(expected.get("primary_uc", "")),
        secondary_ucs=list(expected.get("secondary_ucs") or []),
        drift_type=drift_type,
        has_frustration=has_frustration,
        frustration_type=frustration_level,
        topic_subject=str(form.get("topic_subject", "") or ""),
        description=str(form.get("description", "") or ""),
        rule_draft=rule_draft,
        turns=turns,
        turns_filename=turns_filename,
    )


# ---------------------------------------------------------------------------
# Data classes
# ---------------------------------------------------------------------------

@dataclass
class SessionWork:
    """One spec's work item."""
    case_id: str
    source_session_id: str
    source_dataset: str
    primary_uc: str
    spec_path: Path
    spec_doc: dict[str, Any]
    turns: list[dict[str, str]]
    turns_filename: str


@dataclass
class SessionResult:
    """Per-session audit outcome."""
    case_id: str
    source_session_id: str
    source_dataset: str
    primary_uc: str
    spec_path: Path
    rule_draft: dict[str, Any]
    llm_proposal: dict[str, Any] | None = None
    llm_confidence: str = ""
    llm_rationale: str = ""
    validation_status: str = "ok"
    validation_notes: list[str] = field(default_factory=list)
    diff_summary: dict[str, bool] = field(default_factory=lambda: {
        "seed_messages_changed": False,
        "user_goal_summary_changed": False,
        "hidden_facts_changed": False,
    })
    prompt_hash: str = ""
    error: str | None = None
    skipped_reason: str | None = None  # e.g. "no_transcript"


# ---------------------------------------------------------------------------
# Spec / transcript loading
# ---------------------------------------------------------------------------

def _load_yaml(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as f:
        data = yaml.safe_load(f) or {}
    if not isinstance(data, dict):
        raise ValueError(f"{path}: expected dict at top level")
    return data


def discover_specs(case_spec_root: Path) -> list[Path]:
    paths: list[Path] = []
    for sub in CASE_SET_DIRS:
        d = case_spec_root / sub
        if not d.is_dir():
            continue
        for p in sorted(d.glob("cs_interactive_*.yaml")):
            paths.append(p)
    return paths


def dedup_by_session(spec_paths: list[Path]) -> list[Path]:
    """Return one spec path per source_session_id.

    Smoke specs are mirrors of anchor/promotion/exploration specs. We
    prefer the non-smoke copy when present; otherwise we keep the smoke
    one. Within non-smoke buckets, the first encountered wins (anchor
    sorted before promotion before exploration).
    """
    bucket_priority = {"anchor": 0, "promotion": 1, "exploration": 2, "smoke": 3}
    chosen: dict[str, tuple[int, Path]] = {}
    for p in spec_paths:
        try:
            doc = _load_yaml(p)
        except Exception as e:  # pragma: no cover -- defensive
            LOG.warning("Failed to load %s: %s", p, e)
            continue
        sid = (doc.get("source_session_id") or "").strip()
        if not sid:
            continue
        bucket = p.parent.name
        prio = bucket_priority.get(bucket, 99)
        existing = chosen.get(sid)
        if existing is None or prio < existing[0]:
            chosen[sid] = (prio, p)
    return [pair[1] for pair in chosen.values()]


def load_turns_for_session(
    *,
    turns_dir: Path,
    source_dataset: str,
    session_id: str,
) -> tuple[list[dict[str, str]], str]:
    """Return (sorted-turns, filename). Empty list if not found."""
    filename = SOURCE_DATASET_TURNS_FILE.get(source_dataset.strip(), "")
    if not filename:
        return [], ""
    path = turns_dir / filename
    if not path.exists():
        return [], filename
    turns: list[dict[str, str]] = []
    import csv as _csv
    with path.open("r", encoding="utf-8") as f:
        reader = _csv.DictReader(f)
        for row in reader:
            if (row.get("conversation_id") or "").strip() == session_id:
                turns.append(row)

    def _seq_key(r: dict[str, str]) -> int:
        try:
            return int((r.get("sequence") or "0").strip())
        except ValueError:
            return 0

    turns.sort(key=_seq_key)
    return turns, filename


# ---------------------------------------------------------------------------
# Validation -- thin shim over llm_persona_reviewer.validate_response so the
# shadow audit script keeps a tuple-returning API but doesn't duplicate any
# rules.
# ---------------------------------------------------------------------------

_WS_RE = re.compile(r"\s+")


def _normalize(s: str) -> str:
    return _WS_RE.sub(" ", s).strip().lower()


def validate_response(
    raw: dict[str, Any],
    rule_draft: dict[str, Any],
    turns: list[dict[str, str]],
) -> tuple[dict[str, Any], str, list[str]]:
    """Wrap :func:`llm_persona_reviewer.validate_response` so the audit
    script keeps the historical (proposal, status, notes) tuple."""
    rd_seeds = list(rule_draft.get("seed_messages") or [])
    rd_facts = [
        dict(hf) for hf in (rule_draft.get("hidden_facts") or []) if isinstance(hf, dict)
    ]
    pd = PersonaDraft(
        seed_messages=rd_seeds,
        user_goal_summary=rule_draft.get("user_goal_summary", "") or "",
        hidden_facts=rd_facts,
        verbosity="normal",
    )
    outcome = _validate_response_shared(raw, pd, turns)
    # The shadow-audit YAML uses "schema_error" for length failures; remap
    # here so historical reports stay consistent.
    status = outcome.status
    if status == "user_goal_summary_too_long":
        status = "schema_error"
    return outcome.proposal, status, list(outcome.notes)


# ---------------------------------------------------------------------------
# Diff helpers
# ---------------------------------------------------------------------------

def _seeds_changed(a: list[str], b: list[str]) -> bool:
    return [s.strip() for s in a] != [s.strip() for s in b]


def _ugs_changed(a: str, b: str) -> bool:
    return (a or "").strip() != (b or "").strip()


def _hidden_changed(a: list[dict[str, str]], b: list[dict[str, str]]) -> bool:
    def _key(items: list[dict[str, str]]) -> list[tuple[str, str]]:
        return sorted(((i.get("fact","").strip(), i.get("disclose_when","").strip())
                       for i in items))
    return _key(a) != _key(b)


def _string_diff_ratio(a: str, b: str) -> float:
    """Crude diff ratio: fraction of words that differ."""
    aw = set(_normalize(a).split())
    bw = set(_normalize(b).split())
    if not aw and not bw:
        return 0.0
    union = aw | bw
    inter = aw & bw
    if not union:
        return 0.0
    return 1.0 - (len(inter) / len(union))


def _seeds_substantive(rule: list[str], llm: list[str]) -> bool:
    """Heuristic: at least 2 of the 3 seeds materially differ.

    Compared by case+whitespace-normalised string equality.
    """
    rn = [_normalize(s) for s in rule]
    ln = [_normalize(s) for s in llm]
    n = max(len(rn), len(ln))
    if n == 0:
        return False
    same = 0
    for i in range(min(len(rn), len(ln))):
        if rn[i] == ln[i]:
            same += 1
    diff = n - same
    return diff >= 2


# ---------------------------------------------------------------------------
# DeepSeek client
# ---------------------------------------------------------------------------

class DeepSeekClient:
    def __init__(self, api_key: str, *, dry_run: bool = False) -> None:
        self._api_key = api_key
        self._dry_run = dry_run
        self._client: httpx.AsyncClient | None = None

    async def __aenter__(self) -> "DeepSeekClient":
        self._client = httpx.AsyncClient(timeout=HTTP_TIMEOUT)
        return self

    async def __aexit__(self, *exc: Any) -> None:
        if self._client is not None:
            await self._client.aclose()

    async def call(self, *, system: str, user: str) -> dict[str, Any]:
        if self._dry_run:
            return {
                "seed_messages": [],
                "user_goal_summary": "",
                "hidden_facts": [],
                "llm_confidence": "low",
                "llm_rationale": "(dry-run; no DeepSeek call made)",
            }
        assert self._client is not None
        body = {
            "model": DEEPSEEK_MODEL,
            "messages": [
                {"role": "system", "content": system},
                {"role": "user", "content": user},
            ],
            "temperature": 0,
            "response_format": {"type": "json_object"},
        }
        # Never log the auth header.
        headers = {
            "Authorization": f"Bearer {self._api_key}",
            "Content-Type": "application/json",
        }
        last_exc: Exception | None = None
        for attempt in range(MAX_RETRIES + 1):
            try:
                resp = await self._client.post(
                    DEEPSEEK_ENDPOINT, headers=headers, json=body
                )
            except (httpx.RequestError, httpx.TransportError) as e:
                last_exc = e
                LOG.warning("DeepSeek request error attempt %d: %s", attempt + 1, e)
            else:
                if resp.status_code >= 500:
                    LOG.warning(
                        "DeepSeek 5xx attempt %d (status=%s)",
                        attempt + 1, resp.status_code,
                    )
                elif resp.status_code != 200:
                    # Non-retryable HTTP error
                    snippet = resp.text[:300] if resp.text else ""
                    raise RuntimeError(
                        f"DeepSeek non-200 response: {resp.status_code} {snippet}"
                    )
                else:
                    data = resp.json()
                    content = (
                        data.get("choices", [{}])[0]
                        .get("message", {})
                        .get("content", "")
                    )
                    try:
                        return json.loads(content)
                    except json.JSONDecodeError as e:
                        raise RuntimeError(
                            f"DeepSeek returned non-JSON content: {content[:300]}"
                        ) from e
            # Backoff before retry (skip after final attempt)
            if attempt < MAX_RETRIES:
                delay = (2 ** attempt) + random.random()
                await asyncio.sleep(delay)
        raise RuntimeError(f"DeepSeek call failed after {MAX_RETRIES + 1} attempts: {last_exc}")


# ---------------------------------------------------------------------------
# Audit pipeline
# ---------------------------------------------------------------------------

PROMPT_TEMPLATE_SHA256 = hashlib.sha256(PROMPT_SYSTEM.encode("utf-8")).hexdigest()


async def _audit_one(
    work: SessionWork,
    client: DeepSeekClient,
    semaphore: asyncio.Semaphore,
) -> SessionResult:
    rule_draft = {
        "seed_messages": list((work.spec_doc.get("persona") or {}).get("seed_messages") or []),
        "user_goal_summary": (work.spec_doc.get("persona") or {}).get("user_goal_summary", ""),
        "hidden_facts": [
            dict(hf) for hf in
            ((work.spec_doc.get("persona") or {}).get("hidden_facts") or [])
            if isinstance(hf, dict)
        ],
    }
    user_prompt = render_user_prompt(
        spec=work.spec_doc, turns=work.turns, turns_filename=work.turns_filename
    )
    prompt_hash = hashlib.sha256(
        (PROMPT_SYSTEM + user_prompt).encode("utf-8")
    ).hexdigest()

    result = SessionResult(
        case_id=work.case_id,
        source_session_id=work.source_session_id,
        source_dataset=work.source_dataset,
        primary_uc=work.primary_uc,
        spec_path=work.spec_path,
        rule_draft=rule_draft,
        prompt_hash=prompt_hash,
    )

    try:
        async with semaphore:
            await asyncio.sleep(JITTER_SECONDS * random.random())
            raw_response = await client.call(system=PROMPT_SYSTEM, user=user_prompt)
    except Exception as e:
        result.error = f"{type(e).__name__}: {e}"
        result.validation_status = "other"
        result.validation_notes.append(f"api_error: {result.error}")
        return result

    proposal, status, notes = validate_response(raw_response, rule_draft, work.turns)
    result.llm_proposal = {
        "seed_messages": proposal.get("seed_messages", []),
        "user_goal_summary": proposal.get("user_goal_summary", ""),
        "hidden_facts": proposal.get("hidden_facts", []),
    }
    result.llm_confidence = proposal.get("llm_confidence", "")
    result.llm_rationale = proposal.get("llm_rationale", "")
    result.validation_status = status
    result.validation_notes = notes
    result.diff_summary = {
        "seed_messages_changed": _seeds_changed(
            rule_draft["seed_messages"], result.llm_proposal["seed_messages"]
        ),
        "user_goal_summary_changed": _ugs_changed(
            rule_draft["user_goal_summary"], result.llm_proposal["user_goal_summary"]
        ),
        "hidden_facts_changed": _hidden_changed(
            rule_draft["hidden_facts"], result.llm_proposal["hidden_facts"]
        ),
    }
    return result


async def run_audit(
    works: list[SessionWork],
    *,
    api_key: str,
    dry_run: bool,
) -> list[SessionResult]:
    semaphore = asyncio.Semaphore(CONCURRENCY)
    async with DeepSeekClient(api_key, dry_run=dry_run) as client:
        tasks = [_audit_one(w, client, semaphore) for w in works]
        return await asyncio.gather(*tasks)


# ---------------------------------------------------------------------------
# Reports
# ---------------------------------------------------------------------------

def _fmt_seeds(seeds: list[str]) -> str:
    if not seeds:
        return "  (none)"
    return "\n".join(f'  - "{s}"' for s in seeds)


def _fmt_hidden(hidden: list[dict[str, str]]) -> str:
    if not hidden:
        return "  (none)"
    return "\n".join(
        f'  - fact: "{hf.get("fact","")}"  | disclose_when: "{hf.get("disclose_when","")}"'
        for hf in hidden
    )


def write_reports(
    *,
    out_md: Path,
    out_yaml: Path,
    results: list[SessionResult],
    skipped_no_transcript: list[str],
    timestamp: str,
    calls_made: int,
    dry_run: bool,
) -> None:
    out_md.parent.mkdir(parents=True, exist_ok=True)
    out_yaml.parent.mkdir(parents=True, exist_ok=True)

    # ---------- Aggregate stats ----------
    seed_changed = sum(1 for r in results if r.diff_summary["seed_messages_changed"])
    ugs_changed = sum(1 for r in results if r.diff_summary["user_goal_summary_changed"])
    hidden_changed = sum(1 for r in results if r.diff_summary["hidden_facts_changed"])

    confidence_dist = Counter(r.llm_confidence for r in results if r.llm_confidence)
    validation_dist = Counter(r.validation_status for r in results)

    # "Notable" = >=2 of 3 seed slots differ OR user_goal_summary diff >= 0.8
    notable: list[SessionResult] = []
    for r in results:
        if r.error or r.skipped_reason:
            continue
        rule_seeds = r.rule_draft.get("seed_messages") or []
        prop_seeds = (r.llm_proposal or {}).get("seed_messages") or []
        seeds_diff = _seeds_substantive(rule_seeds, prop_seeds)
        ugs_diff = _string_diff_ratio(
            r.rule_draft.get("user_goal_summary", "") or "",
            (r.llm_proposal or {}).get("user_goal_summary", "") or "",
        )
        if seeds_diff or ugs_diff >= 0.8:
            notable.append(r)
    # Stable sort: highest-confidence first, then by case_id
    confidence_rank = {"high": 0, "medium": 1, "low": 2, "": 3}
    notable.sort(key=lambda r: (confidence_rank.get(r.llm_confidence, 3), r.case_id))
    notable_top = notable[:15]

    # ---------- Markdown report ----------
    md_lines: list[str] = []
    md_lines.append("# LLM persona review (Wave A6.1 shadow audit)")
    md_lines.append("")
    md_lines.append(f"- run_timestamp: `{timestamp}`")
    md_lines.append(f"- model: `{DEEPSEEK_MODEL}`")
    md_lines.append(f"- prompt_template_sha256: `{PROMPT_TEMPLATE_SHA256}`")
    md_lines.append(f"- sessions_audited: **{len(results)}**")
    md_lines.append(f"- calls_made: **{calls_made}**")
    md_lines.append(f"- calls_skipped_no_transcript: **{len(skipped_no_transcript)}**")
    md_lines.append(f"- dry_run: `{dry_run}`")
    md_lines.append("")
    md_lines.append("## Summary by field changed")
    md_lines.append("")
    md_lines.append("| Field | sessions LLM would change |")
    md_lines.append("|-------|---------------------------|")
    md_lines.append(f"| seed_messages     | {seed_changed} |")
    md_lines.append(f"| user_goal_summary | {ugs_changed} |")
    md_lines.append(f"| hidden_facts      | {hidden_changed} |")
    md_lines.append("")
    md_lines.append("## Confidence distribution")
    md_lines.append("")
    if confidence_dist:
        md_lines.append("| Confidence | Count |")
        md_lines.append("|------------|-------|")
        for level in ("high", "medium", "low"):
            md_lines.append(f"| {level} | {confidence_dist.get(level, 0)} |")
    else:
        md_lines.append("(no confidence values returned)")
    md_lines.append("")
    md_lines.append("## Validation failure breakdown")
    md_lines.append("")
    md_lines.append("| Status | Count |")
    md_lines.append("|--------|-------|")
    for status, n in sorted(validation_dist.items(), key=lambda kv: (-kv[1], kv[0])):
        md_lines.append(f"| {status or '(empty)'} | {n} |")
    md_lines.append("")
    if skipped_no_transcript:
        md_lines.append("## Sessions skipped (no transcript matched)")
        md_lines.append("")
        for sid in skipped_no_transcript:
            md_lines.append(f"- `{sid}`")
        md_lines.append("")

    # cs_interactive_012 dedicated section
    md_lines.append("## cs_interactive_012 — full before/after")
    md_lines.append("")
    cs012 = next(
        (r for r in results if r.source_session_id == "570Q5000008hx9tIAA"),
        None,
    )
    if cs012 is None:
        md_lines.append("_Session not found in this run._")
    else:
        md_lines.append(f"- spec path: `{cs012.spec_path.relative_to(_REPO_ROOT)}`")
        md_lines.append(f"- llm_confidence: `{cs012.llm_confidence}`")
        md_lines.append(f"- validation_status: `{cs012.validation_status}`")
        if cs012.validation_notes:
            md_lines.append("- validation_notes:")
            for note in cs012.validation_notes:
                md_lines.append(f"  - {note}")
        md_lines.append("")
        md_lines.append("**rule_draft seed_messages:**")
        md_lines.append(_fmt_seeds(cs012.rule_draft.get("seed_messages") or []))
        md_lines.append("")
        md_lines.append("**llm_proposal seed_messages:**")
        md_lines.append(_fmt_seeds((cs012.llm_proposal or {}).get("seed_messages") or []))
        md_lines.append("")
        md_lines.append(
            f"**rule_draft user_goal_summary:** {cs012.rule_draft.get('user_goal_summary','')!r}"
        )
        md_lines.append(
            f"**llm_proposal user_goal_summary:** {(cs012.llm_proposal or {}).get('user_goal_summary','')!r}"
        )
        md_lines.append("")
        md_lines.append("**rule_draft hidden_facts:**")
        md_lines.append(_fmt_hidden(cs012.rule_draft.get("hidden_facts") or []))
        md_lines.append("")
        md_lines.append("**llm_proposal hidden_facts:**")
        md_lines.append(_fmt_hidden((cs012.llm_proposal or {}).get("hidden_facts") or []))
        md_lines.append("")
        md_lines.append(f"**llm_rationale:** {cs012.llm_rationale!r}")
        md_lines.append("")

    # Notable proposed changes (top 15)
    md_lines.append("## Notable proposed changes (top 15)")
    md_lines.append("")
    if not notable_top:
        md_lines.append("_No sessions met the substantive-change heuristic._")
    else:
        for r in notable_top:
            md_lines.append(
                f"### {r.case_id}  (session=`{r.source_session_id}`, "
                f"UC=`{r.primary_uc}`, dataset=`{r.source_dataset}`)"
            )
            md_lines.append("")
            md_lines.append(f"- llm_confidence: `{r.llm_confidence}`")
            md_lines.append(f"- validation_status: `{r.validation_status}`")
            if r.validation_notes:
                md_lines.append("- validation_notes:")
                for note in r.validation_notes:
                    md_lines.append(f"  - {note}")
            md_lines.append("")
            md_lines.append("**rule_draft.seed_messages:**")
            md_lines.append(_fmt_seeds(r.rule_draft.get("seed_messages") or []))
            md_lines.append("")
            md_lines.append("**llm_proposal.seed_messages:**")
            md_lines.append(_fmt_seeds((r.llm_proposal or {}).get("seed_messages") or []))
            md_lines.append("")
            md_lines.append(
                f"**rule_draft.user_goal_summary:** {r.rule_draft.get('user_goal_summary','')!r}"
            )
            md_lines.append(
                f"**llm_proposal.user_goal_summary:** {(r.llm_proposal or {}).get('user_goal_summary','')!r}"
            )
            md_lines.append("")
            md_lines.append(f"**llm_rationale:** {r.llm_rationale!r}")
            md_lines.append("")

    out_md.write_text("\n".join(md_lines), encoding="utf-8")

    # ---------- YAML companion ----------
    yaml_doc: dict[str, Any] = {
        "run_metadata": {
            "timestamp": timestamp,
            "model": DEEPSEEK_MODEL,
            "prompt_template_sha256": PROMPT_TEMPLATE_SHA256,
            "sessions_audited": len(results),
            "calls_made": calls_made,
            "calls_skipped_no_transcript": skipped_no_transcript,
            "dry_run": dry_run,
        },
        "summary": {
            "seed_messages_changed": seed_changed,
            "user_goal_summary_changed": ugs_changed,
            "hidden_facts_changed": hidden_changed,
            "confidence_distribution": dict(confidence_dist),
            "validation_status_distribution": dict(validation_dist),
        },
        "sessions": [
            {
                "source_session_id": r.source_session_id,
                "case_id_hint": r.case_id,
                "primary_uc": r.primary_uc,
                "source_dataset": r.source_dataset,
                "spec_path": str(r.spec_path.relative_to(_REPO_ROOT)),
                "prompt_hash": r.prompt_hash,
                "rule_draft": r.rule_draft,
                "llm_proposal": r.llm_proposal,
                "llm_confidence": r.llm_confidence,
                "llm_rationale": r.llm_rationale,
                "validation_status": r.validation_status,
                "validation_notes": r.validation_notes,
                "diff_summary": r.diff_summary,
                "error": r.error,
                "skipped_reason": r.skipped_reason,
            }
            for r in results
        ],
    }
    with out_yaml.open("w", encoding="utf-8") as f:
        yaml.dump(yaml_doc, f, sort_keys=False, allow_unicode=True, width=120)


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument(
        "--case-spec-root", type=Path, default=DEFAULT_CASE_SPEC_ROOT,
        help="Root containing anchor/promotion/exploration/smoke subdirs.",
    )
    p.add_argument(
        "--turns-dir", type=Path, default=DEFAULT_TURNS_DIR,
        help="Directory containing per-source turns CSV files.",
    )
    p.add_argument("--out-md", type=Path, default=DEFAULT_OUT_MD)
    p.add_argument("--out-yaml", type=Path, default=DEFAULT_OUT_YAML)
    p.add_argument("--limit", type=int, default=None,
                   help="Dry-run on the first N unique sessions.")
    p.add_argument("--session-id", type=str, default=None,
                   help="Run for a single source_session_id.")
    p.add_argument(
        "--dry-run", action="store_true",
        help="Render the prompt and skip the DeepSeek call. "
             "Prints the first session's prompt to stdout for debugging.",
    )
    p.add_argument("--verbose", action="store_true")
    return p.parse_args(argv)


def build_works(
    spec_paths: list[Path],
    *,
    turns_dir: Path,
) -> tuple[list[SessionWork], list[str]]:
    works: list[SessionWork] = []
    skipped: list[str] = []
    for sp in spec_paths:
        try:
            doc = _load_yaml(sp)
        except Exception as e:
            LOG.warning("Failed to load %s: %s", sp, e)
            continue
        sid = (doc.get("source_session_id") or "").strip()
        sds = (doc.get("source_dataset") or "").strip()
        if not sid or not sds:
            LOG.warning("Spec %s missing source_session_id/source_dataset; skipping", sp)
            continue
        turns, fname = load_turns_for_session(
            turns_dir=turns_dir, source_dataset=sds, session_id=sid,
        )
        if not turns:
            LOG.warning(
                "No transcript turns for session=%s dataset=%s (file=%s); skipping",
                sid, sds, fname or "(unknown)",
            )
            skipped.append(sid)
            continue
        works.append(SessionWork(
            case_id=str(doc.get("case_id", "")),
            source_session_id=sid,
            source_dataset=sds,
            primary_uc=str(((doc.get("expected") or {}).get("primary_uc") or "")),
            spec_path=sp,
            spec_doc=doc,
            turns=turns,
            turns_filename=fname,
        ))
    return works, skipped


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s [%(levelname)s] %(message)s",
    )

    api_key = os.environ.get("DEEPSEEK_API_KEY", "")
    if not api_key and not args.dry_run:
        LOG.error("DEEPSEEK_API_KEY is not set. Use --dry-run for prompt debugging.")
        return 2

    spec_paths = discover_specs(args.case_spec_root)
    LOG.info("Discovered %d spec files under %s.", len(spec_paths), args.case_spec_root)
    deduped = dedup_by_session(spec_paths)
    LOG.info("Deduplicated to %d unique source_session_ids.", len(deduped))

    works, skipped_no_transcript = build_works(deduped, turns_dir=args.turns_dir)
    LOG.info(
        "Prepared %d work items; %d sessions skipped (no transcript).",
        len(works), len(skipped_no_transcript),
    )

    if args.session_id:
        works = [w for w in works if w.source_session_id == args.session_id]
        if not works:
            LOG.error("No work item matches --session-id=%s.", args.session_id)
            return 3
        LOG.info("Filtered to single session: %s", args.session_id)

    if args.limit is not None and args.limit >= 0:
        works = works[: args.limit]
        LOG.info("Applied --limit; %d work items remain.", len(works))

    if len(works) > CALL_BUDGET:
        LOG.error(
            "Refusing to run: %d work items exceeds the safety cap of %d.",
            len(works), CALL_BUDGET,
        )
        return 4

    if args.dry_run and works:
        first = works[0]
        prompt = render_user_prompt(
            spec=first.spec_doc, turns=first.turns, turns_filename=first.turns_filename
        )
        sys.stdout.write("------ DRY RUN: rendered user prompt for first session ------\n")
        sys.stdout.write(f"# session: {first.source_session_id}\n")
        sys.stdout.write(f"# case_id: {first.case_id}\n")
        sys.stdout.write("------ system ------\n")
        sys.stdout.write(PROMPT_SYSTEM)
        sys.stdout.write("\n------ user ------\n")
        sys.stdout.write(prompt)
        sys.stdout.write("\n------ end ------\n")

    started = time.time()
    timestamp = _dt.datetime.now(_dt.timezone.utc).isoformat()

    if args.dry_run:
        # In dry-run we still produce a report (rule_draft only, llm_proposal=null)
        # so the maintainer can inspect the pipeline shape end-to-end.
        results: list[SessionResult] = []
        for w in works:
            rule_draft = {
                "seed_messages": list(
                    (w.spec_doc.get("persona") or {}).get("seed_messages") or []
                ),
                "user_goal_summary": (w.spec_doc.get("persona") or {}).get(
                    "user_goal_summary", ""
                ),
                "hidden_facts": [
                    dict(hf) for hf in
                    ((w.spec_doc.get("persona") or {}).get("hidden_facts") or [])
                    if isinstance(hf, dict)
                ],
            }
            user_prompt = render_user_prompt(
                spec=w.spec_doc, turns=w.turns, turns_filename=w.turns_filename
            )
            ph = hashlib.sha256(
                (PROMPT_SYSTEM + user_prompt).encode("utf-8")
            ).hexdigest()
            results.append(SessionResult(
                case_id=w.case_id,
                source_session_id=w.source_session_id,
                source_dataset=w.source_dataset,
                primary_uc=w.primary_uc,
                spec_path=w.spec_path,
                rule_draft=rule_draft,
                llm_proposal=None,
                llm_confidence="",
                llm_rationale="",
                validation_status="dry_run",
                validation_notes=["dry_run: no DeepSeek call made"],
                prompt_hash=ph,
            ))
        calls_made = 0
    else:
        results = asyncio.run(run_audit(works, api_key=api_key, dry_run=False))
        calls_made = sum(1 for r in results if r.error is None)

    write_reports(
        out_md=args.out_md,
        out_yaml=args.out_yaml,
        results=results,
        skipped_no_transcript=skipped_no_transcript,
        timestamp=timestamp,
        calls_made=calls_made,
        dry_run=args.dry_run,
    )
    LOG.info(
        "Wrote %s and %s in %.1fs.",
        args.out_md, args.out_yaml, time.time() - started,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
