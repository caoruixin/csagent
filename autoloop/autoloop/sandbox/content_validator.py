"""Pre-sandbox content validator.

Lightweight structural integrity checks that run BEFORE the YAML
diff sandbox, so content-broken hypotheses discard earlier (cheaper
than rejecting in sandbox). Three families:

- **Placeholder syntax integrity** — tokens like `{TOKEN}`, `${TOKEN}`,
  or `<TOKEN>` that already appear in `before_value` must survive
  intact in `after_value`. Tokens not present in `before_value` are
  ignored (no false positive on plain prose).
- **Length sanity** — zero-length / 5x-overflow / 0.1x-underflow
  guard against rambling expansions or accidental near-deletions.
  All ratios + floors are config-driven.
- **Forbidden-token deny-list** — small curated set of literals
  (e.g. `<<SYSTEM>>`, `<<USER>>`) that should never appear in any
  edit. Configurable via `config.content_validator.deny_list`.

D3 discipline: validator ships ONLY the 3 generic placeholder
shapes by default; Salesforce-specific or locale-specific token
shapes are opt-in via `config.content_validator.custom_token_shapes`.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Any, Literal, Pattern, TYPE_CHECKING

if TYPE_CHECKING:
    from ..meta_agent.proposer import Hypothesis


@dataclass
class ContentValidationResult:
    """The verdict of `validate_content`."""

    verdict: Literal["PASS", "FAIL"]
    rule_id: str | None
    detail: str | None


# Generic placeholder token shapes (D3 — these are the default; any
# additional shapes must come via config.custom_token_shapes).
# Each shape is a regex with a single capture group naming the token
# body — so we can compare presence-in-before vs presence-in-after by
# extracted name.
_GENERIC_TOKEN_SHAPES: tuple[tuple[str, Pattern[str]], ...] = (
    ("curly", re.compile(r"\{([A-Z][A-Z0-9_]{1,40})\}")),
    ("dollar_curly", re.compile(r"\$\{([A-Z][A-Z0-9_]{1,40})\}")),
    ("angle_internal", re.compile(r"<([A-Z][A-Z0-9_]{1,40})>")),
)


def _compile_custom_shapes(
    shapes: list[str] | None,
) -> tuple[tuple[str, Pattern[str]], ...]:
    """Compile user-provided regex token shapes.

    Each string is treated as a raw regex with at least one capture
    group. A malformed pattern is silently dropped (v1 contract:
    config is human-curated; bad regex is a config error caught at
    review, not at validation-time).
    """
    out: list[tuple[str, Pattern[str]]] = []
    if not shapes:
        return tuple()
    for i, s in enumerate(shapes):
        try:
            out.append((f"custom_{i}", re.compile(s)))
        except re.error:
            continue
    return tuple(out)


def _extract_tokens(
    text: str, shapes: tuple[tuple[str, Pattern[str]], ...]
) -> set[tuple[str, str]]:
    """Return {(shape_name, token_body), ...} found in `text`."""
    found: set[tuple[str, str]] = set()
    for shape_name, pat in shapes:
        for m in pat.finditer(text):
            if m.groups():
                found.add((shape_name, m.group(1)))
            else:
                found.add((shape_name, m.group(0)))
    return found


def validate_content(
    hypothesis: "Hypothesis",
    *,
    config: dict[str, Any] | None = None,
) -> ContentValidationResult:
    """Structural integrity check on the proposed `after_value`.

    Rule evaluation order (first match wins):

    1. zero_length              — `after_value.strip()` is empty.
    2. deny_list                — `after_value` contains a deny-list token.
    3. length_overflow          — `after_value` > 5x `before_value`.
    4. length_underflow         — `after_value` < 0.1x `before_value` AND
                                  `before_value` is "substantial" (>= floor).
    5. placeholder_corrupted    — a placeholder token present in
                                  `before_value` is missing or malformed
                                  in `after_value`.

    Returns PASS when no rule fires.
    """
    before = hypothesis.before_value or ""
    after = hypothesis.after_value or ""
    cv_cfg = ((config or {}).get("content_validator") or {})

    # ---- rule 1: zero_length ----------------------------------------
    if not after.strip():
        return ContentValidationResult(
            verdict="FAIL",
            rule_id="content_validator.zero_length",
            detail="after_value is empty or whitespace-only",
        )

    # ---- rule 2: deny_list ------------------------------------------
    deny_list = cv_cfg.get("deny_list") or []
    for token in deny_list:
        if not isinstance(token, str) or not token:
            continue
        if token in after:
            return ContentValidationResult(
                verdict="FAIL",
                rule_id="content_validator.deny_list",
                detail=f"after_value contains forbidden token {token!r}",
            )

    # ---- rule 3: length_overflow ------------------------------------
    overflow_ratio = float(cv_cfg.get("length_overflow_ratio", 5.0))
    before_len = len(before)
    after_len = len(after)
    if before_len > 0 and after_len > overflow_ratio * before_len:
        return ContentValidationResult(
            verdict="FAIL",
            rule_id="content_validator.length_overflow",
            detail=(
                f"after_value length {after_len} exceeds "
                f"{overflow_ratio:.1f}x before_value length {before_len}"
            ),
        )

    # ---- rule 4: length_underflow (with floor) ----------------------
    underflow_ratio = float(cv_cfg.get("length_underflow_ratio", 0.1))
    underflow_min_before = int(cv_cfg.get("length_underflow_min_before", 50))
    if (
        before_len >= underflow_min_before
        and after_len < underflow_ratio * before_len
    ):
        return ContentValidationResult(
            verdict="FAIL",
            rule_id="content_validator.length_underflow",
            detail=(
                f"after_value length {after_len} below "
                f"{underflow_ratio:.2f}x before_value length {before_len} "
                f"(before length floor {underflow_min_before})"
            ),
        )

    # ---- rule 5: placeholder_corrupted ------------------------------
    custom_shapes = _compile_custom_shapes(cv_cfg.get("custom_token_shapes"))
    all_shapes = _GENERIC_TOKEN_SHAPES + custom_shapes
    before_tokens = _extract_tokens(before, all_shapes)
    if before_tokens:
        after_tokens = _extract_tokens(after, all_shapes)
        missing = before_tokens - after_tokens
        if missing:
            shape_name, token_body = sorted(missing)[0]
            return ContentValidationResult(
                verdict="FAIL",
                rule_id="content_validator.placeholder_corrupted",
                detail=(
                    f"placeholder token {token_body!r} (shape={shape_name}) "
                    f"present in before_value is missing or malformed in "
                    f"after_value"
                ),
            )

    return ContentValidationResult(
        verdict="PASS",
        rule_id=None,
        detail=None,
    )
