# Dev prompt — Sprint 090 (S-Y1.5c session) / S-Auto-36 (M-Auto-7 S-Y1.5c)

你是 **dev agent for Sprint 090 / S-Auto-36 (M-Auto-7 S-Y1.5c)** — the
severity-calibration half of the S-Y1.5b/c combo. S-Y1.5b (scope fix,
S-Auto-35) is already LANDED in HEAD; you are implementing S-Y1.5c only.

**One-line goal:** calibrate the autoloop anti-hardcode sandbox detector's
*severity* — keep FAIL only on rules whose surface form is unambiguous
§1.7 evidence; demote the semantic-judgment rules to FLAG_FOR_CODEX (which
routes to the per-sub-sprint Codex Kernel review, where that judgment
constitutionally belongs); add a `severity_overrides` reversibility hatch.
Pure autoloop-infra; **no re-bless**.

## Read order (minimal)

1. `AGENTS.md` (auto-loaded — governance chain).
2. **This prompt** — the self-contained contract is embedded below; you do
   not need any other doc to execute.
3. Code anchors named inline below (read those specific files/lines when
   you reach the relevant scope item).

Full story (read only if you want it): the binding implementation plan is
`docs/solutions/2026-06-11-sy15bc-combo-implementation-plan.md`; the RCA is
`docs/solutions/2026-06-11-anti-hardcode-detector-vs-kernel-calibration.md`.

---

## Class

| Field | Value |
|---|---|
| **Layer (§3.2)** | `infra` — autoloop propose-stage sandbox detector severity calibration (eval-framework-adjacent) |
| **§7 stanza** | **REQUIRED** — this changes what the propose-stage filter treats as a §1.7 violation. Stanza embedded below; copy it into your handoff section. |
| **Per-sub-sprint Codex (§4.3)** | **REQUIRED** — one combined nine-question walk covers the cumulative S-Y1.5b + S-Y1.5c diff. Runs AFTER your dev close, BEFORE the downstream S-Y2 Part C re-tranche. |

## Goal

The S-Auto-4 detector currently FAILs (= pre-eval **discards** the
candidate) on eleven rules, several of which fire on shapes that are
legitimate principle-level narrative / few-shot teaching, not hardcode.
During the in-flight CS4 pilot this blocked on-target candidates from ever
reaching eval (exp-70/exp-76 on `resolve_faq_grounded_answer.yaml
$.procedure`). S-Y1.5b (LANDED) stopped baseline text from re-triggering;
S-Y1.5c (this work) fixes the *severity*: FAIL means "definitely a
hardcode — discard"; FLAG_FOR_CODEX means "borderline — let Codex judge
under the Kernel." Restore the semantic gate to where the Constitution
puts it (§4.1/§4.3 Codex review), keep the cheap syntactic FAILs that are
surface == intent.

## Code anchors

- `autoloop/autoloop/sandbox/anti_hardcode_check.py`:
  * `_FAIL = "FAIL"` line **114**; `_FLAG = "FLAG_FOR_CODEX"` line **115**.
  * `_RULES` registry — lines **353-365** (the 11 entries; the severity is
    the middle element of each tuple). **This is the primary edit.**
  * `anti_hardcode_check()` entry — line **373**; `anti_cfg` read line
    **401**; `synonym_enabled` line **402**; `normalized` line **403**;
    `before_norm` lines **409-411**; the rule loop lines **414-434**
    (`for rule_id, severity, fn in _RULES:` — currently uses the registry
    `severity` directly; you will route it through `_effective_severity`).
  * `AntiHardcodeResult` dataclass lines **44-61**; the FLAG_FOR_CODEX
    return path lines **428-434**.
  * **DO NOT CHANGE**: any rule-body regex (`_RE_Q1_*` 166-189, `_RE_Q2_*`
    227-234, `_RE_Q4_*` 274-282, `_RE_Q5_*` 302-318), `_normalize`
    (90-105), `_SYNONYM_MAP` (69-81), `_first_new_match` (123-158), each
    rule helper's predicate.
- `autoloop/config.yaml` — `anti_hardcode:` block lines **381-406**
  (`enabled` 385, `synonym_map_enabled` 401, `flag_for_codex_rate_warn_threshold`
  406). The stale "11 FAIL + 4 PASS + 2 FLAG" calibration narrative is the
  comment at line **394**. `scoring_code_baseline_sha` (the no-re-bless
  proof) is line **340** — must NOT change.
- `autoloop/autoloop/meta_agent/prompts/propose.txt` — "# Forbidden
  patterns (§1.7 …)" list lines **32-63** (SIX items in HEAD; item #6 =
  PILOT_PRIMARY_TARGETS bookkeeping). "# Pilot target steering" begins line
  **65**. You insert a new "# Acceptable patterns" section BETWEEN them.
- `autoloop/autoloop/loop.py` — FLAG surfacing path you must NOT break:
  `anti_hardcode_flag_for_codex = True` line **271**; persisted to the
  experiments row lines **610-611**; written to
  `runs/<id>/anti_hardcode_verdict.json` lines **808-813**.
- Tests:
  * `autoloop/tests/test_anti_hardcode_check.py` — holds the individual
    per-pattern `assert res.verdict == "FAIL"` tests (the real assertions
    the demote flips) + where your new tests go.
  * `autoloop/tests/test_real_meta_agent_calibration.py` — asserts FLAG
    rate < 0.25 + 0-FP-on-clean over the THREE clean real samples
    (exp-2/3/4). **These assertions stay green** (clean prose never
    matches, so the demote can't touch them); only its module docstring's
    "11/4/2" narrative needs the doc-hygiene update.

## Scope (4 items, in order)

### Item 1 — `_RULES` registry severity changes (anti_hardcode_check.py:353-365)

Flip the middle tuple element from `_FAIL` to `_FLAG` for exactly these
**six** registry entries:

| rule_id (registry) | FAIL → FLAG | helper |
|---|---|---|
| `Q1.enumerated_or_keywords` | `_FAIL` → `_FLAG` | `_q1_or_keywords` |
| `Q1.if_then_decision_tree` | `_FAIL` → `_FLAG` | `_q1_if_then` |
| `Q2.must_always_against_soft_dimension` | `_FAIL` → `_FLAG` | `_q2_must_always` |
| `Q5.bot_must_always` | `_FAIL` → `_FLAG` | `_q5_bot_must_always` |
| `Q5.do_not_consider_soft_dimension` | `_FAIL` → `_FLAG` | `_q5_do_not_consider` |
| `Q5.force_assistant_to` | `_FAIL` → `_FLAG` | `_q5_force_assistant` |

**Keep FAIL** (do NOT touch): `Q1.contains_or_matches_literal`,
`Q2.tier0_invariant_invention_attempt`, `Q4.case_id_literal`,
`Q4.id_assignment_literal`.
**Already FLAG** (unchanged): `Q5.standalone_must_borderline`.

**`Q1.arrow_tree` note (correction 2026-06-11):** the arrow form
(`if X → Y`) is NOT a separate registry entry — the arrow regex
`_RE_Q1_ARROW_TREE` runs inside `_q1_if_then`, which the registry
registers under the single id `Q1.if_then_decision_tree`. Flipping that
one entry demotes BOTH the if/then and arrow forms together. **Do NOT mint
a `Q1.arrow_tree` rule_id or split the helper.** A dedicated unit test
(below) asserts the arrow form demotes via the shared id.

Final FAIL set = 4 rules; final FLAG set = 7 rules; total 11 (unchanged
count).

### Item 2 — `severity_overrides` config + `_effective_severity` helper

Add to the EXISTING `anti_hardcode:` block in `autoloop/config.yaml`
(nested under it, NOT top-level):

```yaml
  # S-Y1.5c — per-rule severity overrides. Empty default. Each key is a
  # rule_id from anti_hardcode_check.py _RULES; value is "FAIL" or
  # "FLAG_FOR_CODEX". Overrides the _RULES default. Reversibility hatch:
  # if Codex review at S-Y2 close shows a demoted rule was real hardcode
  # that slipped, re-promote here without a code change.
  severity_overrides: {}
```

In `anti_hardcode_check.py`, add a helper near `_RULES`:

```python
def _effective_severity(rule_id: str, default: str, config: dict | None) -> str:
    """Effective severity for a rule, honoring config overrides.

    Order: anti_hardcode.severity_overrides[rule_id] > _RULES default.
    Only "FAIL" / "FLAG_FOR_CODEX" are valid; any other value is ignored
    (treated as no override) so a config typo cannot disable a rule.
    """
    overrides = ((config or {}).get("anti_hardcode") or {}).get(
        "severity_overrides"
    ) or {}
    candidate = overrides.get(rule_id)
    if candidate in ("FAIL", "FLAG_FOR_CODEX"):
        return candidate
    return default
```

In the rule loop (`anti_hardcode_check.py:414-434`) compute
`eff = _effective_severity(rule_id, severity, config)` per rule and branch
on `eff` instead of the raw registry `severity`. Empty override map →
byte-identical to the registry default. Preserve the existing
FLAG_FOR_CODEX "remember-first-flag, keep scanning for a FAIL" semantics
(a later FAIL still outranks an earlier FLAG).

### Item 3 — propose.txt "# Acceptable patterns" clarification

Insert this new section AFTER the "# Forbidden patterns" list (after line
63) and BEFORE "# Pilot target steering" (line 65):

```text
# Acceptable patterns (NOT forbidden — these are legitimate)

The §1.7 forbidden list above targets RULE DUMPS — decision trees
that bind the LLM's semantic choice on dimensions §1.3 says the LLM
owns. The following shapes are NOT rule dumps and are acceptable in
your `after_value`:

1. **Few-shot illustrative examples** that teach a CLASS of
   behavior: `(e.g., "how do I receive payment", "how does payout
   work")` teaches the LLM what a FAQ-shaped question looks like;
   the LLM still judges whether any given customer turn matches.
2. **Principle-level narrative** with observable-state conditioning:
   "When the projection shows a listing context, anchor the answer
   on that listing's state" describes a principle + observable
   signal; the LLM judges applicability.
3. **Sequence narration** describing tool flow: "search_knowledge ->
   resolve_article -> grounded answer -> record_outcome" describes
   the FAQ-path workflow contract; not a rule dump.
4. **Explicit Tool / enum surface legibility**: making
   Runtime-owned schemas (escalation_reason enum,
   intake_state.intake_complete) legible to the LLM via narrative
   that uses the schema's literal names — this is necessary
   projection equivalence, not hardcode.

The autoloop sandbox detector now FLAGs (does NOT FAIL) Q1.if_then,
Q1.arrow_tree, Q1.enumerated_or_keywords, Q2.must_always, and Q5.*
patterns — these flags route to Codex per-sub-sprint review under
the Kernel 9-question gate (`docs/current/anti-hardcode-review-kernel.md`).
A FLAG is not a discard; it is a signal that Codex must judge.

Q4.case_id_literal (raw `cs<id>` tokens) and Q4.id_assignment_literal
(`session_id:` / `case_id:` etc.) and Q1.contains_or_matches_literal
(`.contains(...)` / `.matches(...)`) and Q2.tier0_invariant_invention_attempt
remain FAIL because their surface form == constitutional intent.
```

This must NOT weaken the forbidden list above it — it adds positive
guidance only.

### Item 4 — demoted-fixture re-baseline (per-fixture, NOT bulk)

**Code reality (read carefully — the plan's "17-fixture exact-count"
framing is inaccurate):** there is no single "11 FAIL / 4 PASS / 2 FLAG"
assertion. The assertions the demote flips are the **individual
per-pattern `assert res.verdict == "FAIL"` tests** in
`test_anti_hardcode_check.py` — e.g. `test_q1_if_then_decision_tree_rejected`,
`test_q1_or_keyword_enumeration_rejected`,
`test_q2_runtime_must_always_on_soft_dimension_rejected`, and the Q5.*
rejection tests.

1. Run the suite once after Items 1-3.
2. **Grep `test_anti_hardcode_check.py` for every `== "FAIL"`** and triage
   each against the Item 1 table:
   - rule_id in the demoted six → change expected verdict to
     `FLAG_FOR_CODEX`, and add a one-line Kernel rationale comment, e.g.
     `# Q3: equivalent soft-signal projection exists — flag, do not fail`
     or `# Q6: principle-level narrative — flag, do not fail`.
   - rule_id in the kept four (Q1.contains/matches, Q2.tier0, Q4.case_id,
     Q4.id_assign) → expected verdict stays `FAIL`. Do not touch.
   Do not assume the named list is exhaustive — the grep is authoritative.
3. **Do not bulk-rewrite.** Each flipped test gets its own rationale line;
   the commit message enumerates each flipped test + its Kernel rationale.
4. Doc-hygiene: update the "11 FAIL + 4 PASS + 2 FLAG" narrative in the
   `test_real_meta_agent_calibration.py` module docstring AND the
   `config.yaml:394` comment to the new approximate distribution
   (~2 FAIL / 4 PASS / 11 FLAG). These are narrative, not gates.

**STOP + escalate to human** if: any test EXPECTED to stay FAIL changes
verdict (or vice versa); the registry change is then wrong for that
pattern.

## Test / eval requirements

Add to `autoloop/tests/test_anti_hardcode_check.py`:

- `test_severity_overrides_promote_demoted_rule_back_to_FAIL` — set
  `anti_hardcode.severity_overrides: {"Q1.if_then_decision_tree": "FAIL"}`;
  feed a `(before_value, after_value)` pair with a NEW Q1.if_then pattern;
  assert verdict == `FAIL`, rule_id == `Q1.if_then_decision_tree`.
- `test_severity_overrides_demote_fail_to_flag` — set
  `anti_hardcode.severity_overrides: {"Q4.case_id_literal": "FLAG_FOR_CODEX"}`;
  feed an `after_value` with a literal `cs011`; assert verdict ==
  `FLAG_FOR_CODEX`. (Verifies the override MACHINERY both directions; the
  DEFAULT Q4 behavior is asserted separately below + is HARD-FENCED.)
- `test_default_severity_demotes_q1_if_then_to_flag` — default config; NEW
  `if X then Y` pattern; assert verdict == `FLAG_FOR_CODEX`.
- `test_default_severity_demotes_q1_arrow_tree_to_flag` — default config;
  NEW `if X → Y` (arrow) pattern; assert verdict == `FLAG_FOR_CODEX` AND
  rule_id == `Q1.if_then_decision_tree` (proves the arrow form demotes via
  the shared registry id — correction #1).
- `test_default_severity_keeps_q4_case_id_as_fail` — default config;
  `after_value` containing `cs_uc_a_no_ad_id`; assert verdict == `FAIL`.
- `test_default_severity_keeps_q1_contains_matches_as_fail` — default
  config; `after_value` containing `.contains("refund")`; assert verdict
  == `FAIL`.
- `test_default_severity_keeps_q2_tier0_invention_as_fail` — default
  config; `after_value` with "add new tier-0 invariant"; assert verdict ==
  `FAIL`.
- `test_propose_txt_contains_acceptable_patterns_section` — assert the
  propose.txt file contains the literal substring
  `Few-shot illustrative examples` (defensive — the clarification must not
  silently disappear). Put it wherever the propose.txt is already read in
  tests, or a new `test_propose_prompt.py`.

Update existing tests:

- Flip the demoted per-pattern FAIL tests → FLAG_FOR_CODEX per Item 4.
- Re-verify any test asserting the `anti_hardcode_flag_for_codex` flag
  attaches to the iteration record (loop wiring) still holds — more
  iterations now FLAG.

Suite-level:

- Full `autoloop` pytest **green** (baseline 336 post-S-Y1.5b + your net
  delta).
- `test_real_meta_agent_calibration.py` three assertions stay green
  (untouched mechanically; docstring narrative updated only).
- `scoring_code_baseline_sha` (config.yaml:340) **unchanged** — prove it
  (`gaming.scoring_code_drift.sha_changed` must not fire). **No re-bless.**
- Mocked/unit only — per §5.7 this is NOT behaviour-change evidence; the
  real-LLM evidence is the downstream S-Y2 Part C `-n 3` tranche.

## Pre-close sanity check (§3.4 — 1-2 dry-run iterations)

After committing your dev scope (clean tree — the autoloop sweeps the
staged index, so never run it dirty), run:

```bash
cd autoloop && uv run python -m autoloop run --dry-run -n 2
```

Then read `runs/exp-<N>/hypothesis.json` for each dry-run iteration and
confirm:

- `after_value` contains NO raw eval CaseSpec ids (Q4 still bites).
- `rationale` does NOT gratuitously use "the bot must always X" / "the
  runtime must never Y" against §1.3 soft dimensions.
- `after_value` reads as principle-level narrative or few-shot teaching —
  NOT if-then rule dumps with quoted user_message keywords.

If ANY iteration shows the proposer over-correcting toward real rule dumps
(Q4 leakage; gratuitous always/must/never; case_id literals; per-UC
if-else lattices), **STOP and tune the propose.txt clarification** — the
proposer must not infer "FLAG is acceptable so I'll write anything." If
all look normal, proceed. Record the 1-2 `hypothesis.json` snapshots + a
short per-iteration checklist verdict in your handoff section.

## Hard fences / STOP

- Edit **only**: `autoloop/autoloop/sandbox/anti_hardcode_check.py`,
  `autoloop/config.yaml` (severity_overrides + comment hygiene),
  `autoloop/autoloop/meta_agent/prompts/propose.txt`,
  `autoloop/tests/test_anti_hardcode_check.py`,
  `autoloop/tests/test_real_meta_agent_calibration.py` (docstring only),
  and at most one new `autoloop/tests/test_propose_prompt.py` for the
  substring test.
- **No §1.7 constitution change** — `docs/current/iteration_governance.md`
  byte-identical.
- **No Kernel change** — `docs/current/anti-hardcode-review-kernel.md`
  byte-identical.
- **No rule-body regex / `_normalize` / `_first_new_match` / predicate
  change** — severity is the ONLY behavioral change.
- **No scoring change** — `autoloop/autoloop/scoring/*.py` byte-identical;
  `scoring_code_baseline_sha` reproduces → **no re-bless**.
- **No CaseSpec / fixtures / baseline change** (`eval_interactive/**`),
  **no Skill YAML change** (`server/src/main/resources/skills/**`), **no
  mutable-surface block change** (`config.yaml:mutable_surface`), **no
  `baseline_dir` / `current_eval_baseline.md` flip**, **no `server/**`
  Java change** (Java baseline `1383/1/0/2` stays).
- **Q4 default stays FAIL**: the override map MAY demote Q4 per-experiment
  (forensic), but the DEFAULT `_RULES` registry keeps `Q4.case_id_literal`
  + `Q4.id_assignment_literal` as `_FAIL`.
- **Shadow firewall unaffected** — the detector reads only `before_value`
  + `after_value` strings; no eval-result reads, no shadow data.
- **propose.txt clarification must NOT silently disappear** — the
  substring unit test enforces this.
- **STOP + escalate** if: a kept-FAIL test changes verdict (or a
  demoted-FLAG test stays FAIL); `scoring_code_baseline_sha` changes; the
  fix needs a file outside the allowed set; or the dry-run shows proposer
  over-correction.

## §7 Layer-classification + anti-hardcode stanza (copy into handoff)

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (autoloop sandbox detector severity
calibration; eval-framework-adjacent — §5.8 framework-defect priority
applies in spirit because the propose-stage filter was blocking on-target
candidates from reaching eval for false-positive reasons during an
in-flight semantic pilot).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. The demote
does NOT change what §1.7 forbids; it changes how the propose-stage filter
expresses uncertainty about borderline semantic patterns (FLAG_FOR_CODEX =
"Codex must judge" vs FAIL = "definitely a hardcode"). The semantic gate is
restored to per-sub-sprint Codex review (§4.3), where it lived
constitutionally before the detector's S-Auto-4 over-tightening. Structural
defenses retained: Q4.case_id_literal + Q4.id_assignment_literal stay FAIL
(§1.7 raw-eval-phrase / identifier-binding); Q2.tier0_invariant_invention_attempt
stays FAIL (§3.2 Tier-0 minting authorization); Q1.contains_or_matches_literal
stays FAIL (code-style enumeration, surface == intent); shadow firewall,
mutable-surface YAML sandbox, and 5-layer lexicographic fitness gate all
unaffected; per-sub-sprint Codex review (§4.3) IS the semantic-judgment
binding gate. Reversibility: `anti_hardcode.severity_overrides` re-promotes
any demoted rule to FAIL per-experiment without a code change.

**Semantic hardcode:** No semantic hardcode introduced. This patch removes
false-positive hardcode rejections that were blocking legitimate
principle-level narrative + few-shot illustrative teaching (e.g. exp-70 /
exp-76 on `resolve_faq.$.procedure`). The propose.txt clarification adds
positive guidance without weakening the §1.7 forbidden list.

**Generalization coverage:** target / neighbor / negative / shadow case
counts: n/a — pure-infra sub-sprint touching only the detector severity
registry + `_effective_severity` helper + propose.txt clarification +
`severity_overrides` config schema + the two test files. No agent semantic
surface touched. Validation = unit tests + per-pattern demoted-fixture
re-baseline + §3.4 pre-close 1-2 dry-run sanity check.
```

## Codex review plan (§4.3, REQUIRED — combo)

One §4.3 nine-question Kernel walk covers the cumulative S-Y1.5b +
S-Y1.5c diff. Codex writes TWO §4.2 stanzas to `docs/codex-findings.md`
(one for b, one for c). S-Y1.5c focus questions Codex will apply:

1. Severity-demote scope correctness — FAIL set reduced to ONLY
   surface==intent rules (Q4.case_id, Q4.id_assign, Q2.tier0,
   Q1.contains/matches)? Demoted rules genuinely semantic-judgment per
   Kernel Q3/Q6?
2. Demoted-fixture per-fixture review — each FAIL→FLAG test's Kernel
   rationale justified? ≥1 unjustified demote ⇒ Codex FAIL.
3. Q4 default unchanged — default registry still FAILs `cs011` /
   `cs_uc_a_no_ad_id` + `session_id=` style.
4. propose.txt clarification opens no §1.7 bypass (framing real rule
   dumps as "few-shot examples")?
5. FLAG_FOR_CODEX surfacing preserved end-to-end (`anti_hardcode_verdict`
   + `anti_hardcode_flag_for_codex` through experiments row +
   `runs/<id>/anti_hardcode_verdict.json`). Without surfacing, the demote
   is a §1.7 bypass.
6. Shadow firewall unaffected.
7. `scoring_code_baseline_sha` stable (no scoring-file edit).
8. Reversibility actually reversible — override `{Q1.if_then_decision_tree:
   "FAIL"}` returns FAIL on a NEW Q1.if_then.

## Handoff (single combo file — `docs/sprints/sprint-090-handoff.md`)

The S-Y1.5b scope-fix section is ALREADY in this file (S-Auto-35). **Do
NOT create a separate handoff and do NOT rewrite the b section.** Append a
clearly-delimited `## S-Y1.5c — severity calibration` section recording:
Class; Goal; diff summary (the 4-5 changed files + commit SHA range); the
per-rule severity table (incl. the `Q1.arrow_tree` row + its
covered-by-`Q1.if_then_decision_tree` note); the demoted-fixture
FAIL→FLAG test-update table + per-test Kernel rationale; the
`test_real_meta_agent_calibration.py` docstring + config.yaml comment
hygiene fix; §3.4 dry-run sanity evidence; the §7 stanza (above); the
`severity_overrides` reversibility example; the no-re-bless / no-scoring /
no-§1.7 / no-Kernel proof; and the observability follow-up R-item ("Track
Codex `reject` rate on FLAG_FOR_CODEX patterns at S-Y2 close; if > 30%,
reconsider demote scope at M-Auto-8" — observability R-item, not a §5.X
gate). The two §4.2 Codex stanzas are written by Codex into
`docs/codex-findings.md` at combo close.

## Commit discipline

Stage explicitly by file (NO `git add -A`). The dev commit is `autoloop/**`
only (detector + config + propose.txt + tests). The handoff section and
any `docs/` close artefacts are deliver-agent / human close artefacts,
bundled at close — NOT part of your dev commit. Commit the dev scope
**before** the §3.4 dry-run so the tree is clean (the autoloop sweeps the
staged index; never run it dirty). Do not run a non-dry-run autoloop.

## Self-check (tick before close)

- [ ] `_RULES`: exactly the six listed entries flipped `_FAIL`→`_FLAG`;
      the four kept-FAIL + the one already-FLAG untouched; count still 11.
- [ ] No new `Q1.arrow_tree` registry id; arrow form demotes via
      `Q1.if_then_decision_tree` (proven by unit test).
- [ ] `severity_overrides: {}` added under `anti_hardcode:`;
      `_effective_severity` honored in the rule loop; empty map = no-op.
- [ ] propose.txt "# Acceptable patterns" inserted between the forbidden
      list and "# Pilot target steering"; substring test added.
- [ ] Rule-body regexes / `_normalize` / `_first_new_match` / predicates
      byte-identical; severity is the only behavioral change.
- [ ] Demoted per-pattern FAIL tests flipped to FLAG with Kernel
      rationale; kept-FAIL tests unchanged; `== "FAIL"` grep triaged
      exhaustively.
- [ ] 8 new tests added (overrides both directions, default demote incl.
      arrow, three kept-FAIL defaults, propose.txt substring); full
      autoloop pytest green.
- [ ] `test_real_meta_agent_calibration.py` assertions green; its docstring
      + config.yaml:394 comment updated to ~2/4/11 (doc-hygiene).
- [ ] `scoring_code_baseline_sha` unchanged (proof recorded); no re-bless.
- [ ] Only the allowed files touched; Java / Skill YAML / CaseSpec /
      baseline / shadow all untouched.
- [ ] §3.4 dry-run sanity: 1-2 `hypothesis.json` snapshots clean; checklist
      verdict recorded; no proposer over-correction.
- [ ] Handoff: S-Y1.5c section APPENDED to `sprint-090-handoff.md` (b
      section untouched); §7 stanza + reversibility example + observability
      R-item present; commit is `autoloop/**` only.
