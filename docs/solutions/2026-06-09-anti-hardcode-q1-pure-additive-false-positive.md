---
title: anti_hardcode_check Q1 detector false-fires on pure-additive edits to fields whose baseline already encodes "when X → Y" prose (S-Y2 n=8 tranche RCA)
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-06-09
last_updated: 2026-06-10
mode: bad-case-driven
notes: >
  Path-2 RCA on the S-Y2 Part C tranche. 2026-06-09 draft was based on
  exp-70 alone (n=1). 2026-06-10 update: the n=8 tranche
  (exp-69..exp-76) completed; exp-76 hit Q1 with a BYTE-IDENTICAL
  matched_substring to exp-70 — confirming the false-positive is
  deterministic, not stochastic, on every pure-additive iteration on
  resolve_faq_grounded_answer.yaml `$.procedure`. Strengthens the
  recommendation: S-Y1.5b should land before any §3.4 fallback decision,
  because Q1 currently blocks the ONLY tranche candidates that ever wrote
  explicit CS4 entity-context narrative (exp-70 / exp-76 on the §Scope
  most-plausible surface) — without the patch, the question "can autoloop
  produce a candidate that flips the CS4 PRIMARIES" remains unanswered.
  Does NOT supersede / replace the 2026-06-08 substrate proposal.
---

# anti_hardcode_check Q1 detector false-fires on pure-additive

## 1. Executive summary (2026-06-10 — n=8 tranche complete)

**The problem (now confirmed deterministic, not stochastic)**: in the
S-Y2 Part C n=8 tranche, exp-70 and exp-76 BOTH hit Q1 false-positive
with **byte-identical matched_substring** from the pre-existing
baseline `$.procedure` text of `resolve_faq_grounded_answer.yaml`. Every
pure-additive iteration on this surface will trigger the same
false-positive because the trigger lies entirely in production text the
candidate's appended clause does not touch.

```
matched_substring (exp-70 and exp-76, identical):
"if available, otherwise its article_id) → record_outcome.
 only escalate via request_handover after a valid resolve attempt
 cannot complete (no viable hit, or res..."
```

The candidates' added clauses are legitimate CS4 narrative procedural
guidance (1478 chars on exp-70, 1441 chars on exp-76); the detector
misfires because it scans the entire normalized `after_value` instead
of the diff.

**The strategic stakes (2026-06-10 update)**: the n=8 tranche produced
4 candidates whose discards REACHED THE EVAL GATE (exp-69 / 71 / 72 /
75) — and per-case forensics (§3.5) confirm **none of them flipped a
CS4 PRIMARY target**. Their bad_cases improvements were on NEIGHBORS
(`cs_uc_a_lookup_failed`) and unrelated cases (cs001 / cs012 / cs015),
not the actual PRIMARIES (`cs_uc_a_no_ad_id_ad_specific` /
`cs_uc_a_loaded_listing`). The ONLY tranche candidates that wrote
explicit "listing-context anchor"/"ad-specific without identifier"
CS4 narrative — exp-70 and exp-76, both on
`resolve_faq.$.procedure` — were sandbox-killed before eval could
run. Without the Q1 patch, the question **"can the autoloop produce
a candidate that flips the CS4 PRIMARIES?"** remains unanswered.

**The fix**: change Q1 (and by symmetry Q2/Q4/Q5/Q6 — see §6.2)
detector scope from "scan full normalized after_value" to "scan full
normalized after_value, but suppress matches that appear in the
normalized before_value". Pure-additive candidates pay nothing for
baseline patterns that already shipped; rewrite candidates are
unaffected because new patterns they introduce are still flagged.

**Blast radius (verified, §3.4)**: exactly **1 of 24 mutable
(skill, field-class) slots** currently has baseline prose that
Q1-tripwires — `resolve_faq_grounded_answer.yaml $.procedure`. But
that ONE slot is the **single most-plausible CS4 entity-context fix
surface** per `sprint_objective.md` §Scope, AND it is the surface the
proposer has now landed on TWICE in 8 iterations with identical Q1
fate. So a 1/24 numeric blast radius blocks the ONLY narrative
candidate stream the autoloop has produced for the CS4 PRIMARIES
question.

**Recommended option**: **Option B** (baseline-whitelist filter) —
~15-20 LOC in `autoloop/autoloop/sandbox/anti_hardcode_check.py` +
2-3 new unit tests. Generalizes trivially to Q2/Q4/Q5/Q6. No prompt
changes, no config schema changes, no gate logic changes, no
re-bless. **2026-06-10 update — recommendation timing strengthened**:
S-Y1.5b should land BEFORE any §3.4 fallback decision. The n=8
tranche shows §3.4 fallback would prematurely conclude "autoloop can't
solve the PRIMARIES" when the actual finding is "autoloop produced 2
candidates whose primary-target capability we never measured."

**Relationship to 2026-06-08 substrate proposal**: that doc is
**implemented and not superseded**. It anticipated the "strong-pass
hit-rate + 0 keep → §3.4 fallback" path (its §11.1, §11.3, §6 Cons
all explicitly endorsed). What it didn't anticipate was that the
sandbox itself would systematically reject every pure-additive
candidate on the §Scope most-plausible surface for a false-positive
reason — that's a fresh framework defect from the M-Auto-1A v1
sandbox, surfaced only after S-Y1.5 brought the candidate stream
onto the right surface. This doc is a NARROW addendum, not a
revision.

## 2. Investigation scope

**Triggered by**: S-Y2 exp-70 (2026-06-09 ~00:11 UTC, branch
`autoloop/exp-70`).

**Method**: read `anti_hardcode_check.py` Q1 detector + normalization
+ entry point (`anti_hardcode_check()` at line 336);
reproduce the Q1 match against the post-normalization
`hypothesis.after_value`; verify the matched substring is contained
in `hypothesis.before_value` (i.e. the pre-existing baseline);
scan all 6 Skill YAML × 4 field-class mutable slots for the same
tripwire condition.

**Out of scope**: any change to gate logic, mutable surface, shadow
firewall, lessons system, pilot_snapshot block, the 4-layer
hit-rate metric, or the S-Y2 pilot's acceptance bar. None of those
are involved in this defect.

## 3. Current-state survey + root cause

### 3.1 The Q1 detector in HEAD

`autoloop/autoloop/sandbox/anti_hardcode_check.py`:

```python
# Lines 87-95 — when/whenever word-boundary normalization
_RE_WHEN_WORD_BOUNDARY = re.compile(r"\b(?:whenever|when)\b")
def _normalize(text, *, synonym_map_enabled):
    norm = text.lower()
    norm = re.sub(r"\s+", " ", norm)
    if synonym_map_enabled:
        norm = _RE_WHEN_WORD_BOUNDARY.sub("if", norm)
        for src, dst in _SYNONYM_MAP.items():
            norm = norm.replace(src, dst)
    return norm

# Lines 69-81 — _SYNONYM_MAP includes "->" → "→", "=>" → "→",
# "==>" → "→", " when "/" whenever " → " if "

# Lines 128-156 — Q1 regex patterns
_RE_Q1_IF_THEN    = re.compile(r"\bif\b[\s\S]{1,120}?\bthen\b[\s\S]{1,120}", re.IGNORECASE)
_RE_Q1_ARROW_TREE = re.compile(r"\bif\b[\s\S]{1,120}?→[\s\S]{1,120}",         re.IGNORECASE)

def _q1_if_then(text: str) -> str | None:
    m = _RE_Q1_IF_THEN.search(text) or _RE_Q1_ARROW_TREE.search(text)
    return _trim(m.group(0)) if m else None

# Line 353 — entry point scope
def anti_hardcode_check(hypothesis, *, config):
    ...
    text = hypothesis.after_value or ""
    # ... scans `text` (full normalized after_value), NOT a diff
```

**The contract the detector tries to enforce**: candidate must not
introduce "if X then Y" / "if X → Y" decision-tree narration into a
Skill YAML field — this is the §1.7 forbidden-list "using prompt as
an if-else rule dump" guard.

**The contract it actually enforces**: the **final post-edit field
text** must not contain such patterns. There's no allowance for
patterns that were already in production.

### 3.2 The pre-existing baseline that triggers Q1

`server/src/main/resources/skills/resolve_faq_grounded_answer.yaml`
HEAD `$.procedure`:

```yaml
procedure: "You are a helpful Gumtree customer support agent. Resolve
the user's issue using the provided tools. FAQ-path RESOLVE flow (S1):
the intended terminal sequence is search_knowledge -> resolve_article
-> grounded customer-facing answer (with a display_citation citation —
the article's source_url when available, otherwise its article_id) ->
record_outcome. Only escalate via request_handover after a valid
resolve attempt cannot complete (no viable hit, or resolve_article
could not produce a grounded answer)."
```

After `_normalize(synonym_map_enabled=true)`:

- `when available` → `if available` (word-boundary substitution)
- `-> record_outcome` → `→ record_outcome` (synonym map)
- Result: `"...if available, otherwise its article_id) → record_outcome. only escalate via request_handover after a valid resolve attempt cannot complete..."`

`_RE_Q1_ARROW_TREE` (`\bif\b[\s\S]{1,120}?→[\s\S]{1,120}`) matches:

- `\bif\b` ← "if available"
- `[\s\S]{1,120}?` ← " available, otherwise its article_id) " (lazy)
- `→` ← "→ record_outcome"
- `[\s\S]{1,120}` ← " record_outcome. only escalate via request_handover after a valid resolve attempt..."

**The matched substring is 100% baseline.** The candidate's append
contributes nothing to the match.

This was always going to fire from the moment `_SYNONYM_MAP` and
`_RE_WHEN_WORD_BOUNDARY` were added in Fix-C step 1 (S-Auto-5,
2026-05-28; see `synonym_map_enabled: true` decision in `config.yaml`).
It just stayed invisible because no prior iteration on
`resolve_faq.$.procedure` was a PURE-ADDITIVE that left the baseline
arrow-narrative intact:

- exp-66 hit `resolve_faq.$.escalation_policy` (different field, no
  baseline arrows).
- pre-S-Y1.5 iterations didn't target resolve_faq.procedure (off-gap).
- exp-71 hit `resolve_faq.$.critical_steps[1].desc` AS A REWRITE
  (before=503→after=1338, restructure) — the regex still scans the
  whole after_value, but `critical_steps[1].desc` doesn't have
  arrows in its baseline so no fire.

exp-70 is the first PURE-ADDITIVE on resolve_faq.procedure since the
synonym map was enabled.

### 3.3 The candidate's actual additive content

exp-70's append (1478 chars) — legitimate CS4 narrative guidance:

> Listing-context awareness: when the per-turn projection already
> carries a concrete listing reference (form_context or
> listing_context surfaces the customer's specific ad), treat the
> loaded listing as the case anchor and resolve from that listing's
> state alongside the grounded knowledge surface — answer the user's
> specific question against the listing data you already hold rather
> than asking the customer to restate an identifier the runtime has
> projected. Ad-specific questions without a listing reference: when
> the user's question concerns a specific ad's state but no listing
> reference is present in projected context and the user has not
> shared one, branch the resolve path on whether the question is
> answerable generically. ...

Does the appended text itself trigger Q1?
**No.** After normalization, the appended text contains `if` patterns
(several) and `→` arrows (zero — the candidate writes prose, not
arrow notation). `_RE_Q1_ARROW_TREE` needs both `if` AND `→`
within 120 chars. The append alone wouldn't match.

The match only fires because **the full `after_value = before + append`
straddles** the baseline's pre-existing `if available...→ record_outcome`
arrow-narrative.

### 3.4 Blast-radius scan (all 6 skills × 4 field-class slots + critical_steps)

Programmatic scan of HEAD applying the same normalization + Q1 regexes:

```
=== Q1 baseline-text matches ===
  resolve_faq_grounded_answer.yaml  procedure  ARROW_TREE
    "if available, otherwise its article_id) → record_outcome.
     only escalate via request_handover after a valid resolve attem"
Total: 1 (skill, field) slots already Q1-tripwired
```

**Exactly 1 of 24** mutable-surface slots (6 skills × {procedure,
grounding_instruction, escalation_policy, critical_steps[*].desc})
currently has baseline prose that Q1-false-fires. But that one slot is
**the most plausible CS4 entity-context fix surface** per
`sprint_objective.md` §Scope:

> "The CS4 entity-context guidance most plausibly lands in
> `resolve_faq_grounded_answer.yaml` `procedure`/`grounding_instruction`
> or an existing `critical_steps[*].desc`"

So the 1/24 blast radius is misleading by count; by **strategic
weight** it blocks the dominant CS4 fix surface for pure-additive
candidates. Rewrite candidates on the same field still have a path
through (if their rewrite removes the baseline arrow notation — but
that's a separate gating challenge and risks Tier-1 regression).

### 3.5 Q1's symmetry to Q2/Q4/Q5/Q6

Same defect class applies to all rule families that scan full
normalized `after_value`:

- Q2 (Tier-0 invariant invention) — would false-fire on baselines
  with `MUST always` / `MUST escalate` phrasing
- Q4 (raw eval phrase encoding) — case_id matching is content-
  level so the issue is theoretical (case_ids shouldn't be in
  baseline by construction) but the symmetry exists
- Q5 (semantic dimension assertion) — same as Q2
- Q6 (FLAG_FOR_CODEX heuristics) — same

The fix in §6 is structurally **rule-family agnostic** — it sits at
the detector entry point, not in the Q1 rule body. So fixing this
once fixes the defect class. No symmetric scan needed today (Q1 is
the only verified hit), but the fix proposal is one-and-done across
all rules.

## 4. Why the 2026-06-08 substrate proposal didn't anticipate this

The 2026-06-08 proposal (now implemented) covers:

| Anticipated scenario | Doc section |
|---|---|
| 0-keep + on-target hit-rate → §3.4 fallback | §11.1 + §11.3 + §6 Cons |
| Q1 detector legitimate hits on candidate-introduced hardcode | §10 fences + §11.3 (`PILOT_PRIMARY_TARGETS` confined to bookkeeping labels; anti-hardcode Q4 is still primary defense against case-id leakage) |
| Lack of hill-climbing memory ↔ strict gate | §5.5 P2-B (deferred as v2-grade design choice; not in S-Y1.5 scope) |
| Anti-hardcode Q1 detector **false-positive on baseline-text** matches | **NOT covered** — the proposal treated the sandbox as correct-by-construction and focused on prompt-builder + steering |

The implicit assumption in 2026-06-08 was: the sandbox is well-tuned
(per `S-Auto-5` calibration evidence and the 17-fixture suite). That
assumption holds for the rule-body regex patterns themselves, but
not for the **detector scope** (full after_value vs diff). The
distinction only matters once steering brings candidates onto skills
whose baseline prose already encodes structures Q1 watches for —
which is exactly what S-Y1.5 successfully did.

So this is **a defect surfaced by S-Y1.5's success**, not a defect in
S-Y1.5 itself.

## 5. n=8 tranche final bucketing (2026-06-10 — all 8 complete)

### 5.1 Per-exp verdict + bucket

| Exp | Target | full_on_gap? | Verdict | Bucket |
|---|---|---|---|---|
| exp-69 | discover_triage.$.grounding_instruction | partial (skill miss) | discard tier-1 anchor_outcome 8→5 | **gate-correct task hardness** |
| **exp-70** | resolve_faq.$.procedure | **full** | discard anti-hardcode Q1.if_then_decision_tree | **framework defect (this doc)** |
| exp-71 | resolve_faq.$.critical_steps[1].desc | full | discard tier-1 anchor_outcome 8→6 | gate-correct task hardness |
| exp-72 | resolve_faq.$.grounding_instruction | full | discard tier-2 critical_flow 3→4 | gate-correct task hardness |
| exp-73 | resolve_faq.$.escalation_policy | skill-only (wrong field) | discard tier-0 escalation_compliance on cs11s01 | **gate-correct off-target** (wrong field; predictably perturbs UC-D escalation) |
| exp-74 | discover_triage.$.critical_steps[0].desc | partial (skill miss) | discard tier-0 escalation_compliance on cs11s01 | gate-correct off-target |
| exp-75 | discover_triage.$.grounding_instruction | partial (skill miss) | discard tier-2 critical_flow UC-A 0→1 | gate-correct task hardness |
| **exp-76** | resolve_faq.$.procedure | **full** | discard anti-hardcode Q1.if_then_decision_tree | **framework defect (this doc) — identical to exp-70** |

**2 of 8 = framework defect (Q1 false-positive on the SAME baseline
substring, same target field, byte-identical matched substring).**
**6 of 8 = gate-correct discards** (either wrong-field/skill or
real task-hardness regressions).

### 5.2 The candidates' actual semantic coverage — and what eval revealed

Per `runs/<exp-N>/eval-results.json` for the 4 candidates that
REACHED EVAL (exp-69 / 71 / 72 / 75; the others were sandbox-killed):

| Exp | bad_cases flips | anchor_outcome flips | Net |
|---|---|---|---|
| exp-69 | +cs012, +cs015, +`cs_uc_a_lookup_failed` [NEIGHBOR], −iwzx_uc_k | −uc_e_promotion, −uc_fp_removed, −uc_g_gdpr | bad_cases +2; anchor_outcome −3 |
| exp-71 | +cs001, +`cs_uc_a_lookup_failed` [NEIGHBOR] | −uc_a_visibility, −uc_e_promotion, −uc_g_gdpr, +uc_i_payment | bad_cases +2; anchor_outcome −2 |
| exp-72 | +cs015, −`wmkb_uc_a_trader_flag_secondary_uc_h` [EXTEND] | (none) | net 0 + new UC-A tier2 |
| exp-75 | +`cs_uc_a_lookup_failed` [NEIGHBOR], −wmkb [EXTEND] | −uc_fp_removed, +uc_h_appeal | net 0 + new UC-A tier2 |

**Critical observation**: across all 4 eval-reached candidates, **zero
CS4 PRIMARIES flipped** (`cs_uc_a_no_ad_id_ad_specific` and
`cs_uc_a_loaded_listing` stay FAIL in every candidate). The bad_cases
+2 improvements on exp-69 / exp-71 come entirely from NEIGHBOR
(`cs_uc_a_lookup_failed`) + unrelated cases (cs001 / cs012 / cs015).

**What this means for the strategic question**:

1. **The autoloop's eval-reached candidates do not yet solve the
   PRIMARIES.** They fish in adjacent waters. If the strict
   anchor_outcome gate were relaxed, none of these 4 would qualify
   for S-Y2 PRIMARY SUCCESS anyway (sprint_objective.md §5.1.4
   requires PRIMARIES to flip; NEIGHBOR / unrelated improvements
   are not credit for SUCCESS).
2. **The 2 candidates that DID write explicit CS4-PRIMARY narrative
   (exp-70 / exp-76 on `resolve_faq.$.procedure`) never reached
   eval.** Their primary-target capability is therefore unknown.
3. **Q1 patch is the only path to resolve the unknown.** Either
   exp-70/exp-76 flip the PRIMARIES (→ S-Y2 SUCCESS likely) or
   they don't (→ §3.4 fallback with the question answered honestly,
   not prematurely concluded).

The 6 gate-correct discards are working as designed. The §3.4
hand-authored fallback path the 2026-06-08 proposal endorsed is
the right response for THEM. **This doc only addresses the 2 of 8
that are NOT working as designed AND happen to be the only
PRIMARIES-narrative candidates the autoloop has produced.**

### 5.3 Why NOT to open a "relax anchor_outcome strict-zero" proposal

A tempting follow-on would be: "exp-69 and exp-71 improve bad_cases by
+2; relax `anchor_outcome_max_drop_cases` from 0 to 2 so they pass and
go to §4.1 review." This would be a wrong-direction patch:

- exp-69 / exp-71 improve NEIGHBORS, not PRIMARIES (§5.2). Even if
  the autoloop kept them, the human §4.1 reviewer would not merge
  them as "the S-Y2 success candidate" because they don't flip
  primaries.
- Relaxing the gate burns human review cycles on candidates that
  don't solve the actual problem.
- The `anchor_outcome_max_drop_cases: 0` setting is a sound autoloop
  fitness invariant ("don't regress canonical scenarios"), not a
  bug. Loosening it is a separate v2-grade design decision belonging
  to a future milestone.
- The 2026-06-08 proposal already labelled this class of trade-off as
  P2-B deferred to v2 (§5.5 "0-keep + clean-checkout 没有 hill-climbing
  内存 — 这是 M-Auto-1A v1 的有意设计").

So this RCA stays narrow on Q1 false-positive. The "primaries not yet
flipped" finding is documented (§5.2) as STRATEGIC CONTEXT for
post-S-Y1.5b decisions, not as a problem to fix.

## 6. Design alternatives

### Option A — Pure diff-based scan (only check new chars vs baseline)

**Scope**: change `anti_hardcode_check()` to compute `before_value`
+ `after_value` diff (via `difflib.SequenceMatcher` or simple
string subtraction for additive case), normalize the diff alone,
and run all Q-rules against the diff text.

**Pros**:
- Conceptually cleanest. The detector explicitly scans only what
  the candidate changed.
- Generalizes naturally to all Q-rules.

**Cons**:
- difflib over normalized prose has edge cases (whitespace,
  token boundaries) that complicate the audit trail.
- A candidate that REWRITES while preserving a baseline if-then
  block would have that block in the diff (because the diff
  output includes "insert" + "delete" operations); to avoid this
  we'd need to special-case "this block is unchanged in net".
- The matched_substring reported to the user is the diff chunk,
  not the position in `after_value`; auditing in `runs/<id>/
  anti_hardcode_verdict.json` becomes less obvious.

### Option B (recommended) — Baseline-whitelist filter on matches

**Scope**: keep the detector scanning full normalized `after_value`,
but for each rule that matches, check whether the matched substring
is present in the normalized `before_value`. If yes, the match is
a baseline artifact — skip and continue scanning for further
matches. If no further match, return PASS.

**Implementation sketch** (~15-20 LOC):

```python
def _filter_matches(rule_fn, text_norm: str, before_norm: str) -> str | None:
    """Run `rule_fn` against text_norm; suppress matches present in
    before_norm. Returns the first NEW-content match, or None."""
    # Each rule_fn currently returns a single _trim()'d substring.
    # We re-implement at the regex level to iterate matches.
    ...

def anti_hardcode_check(hypothesis, *, config):
    text       = hypothesis.after_value or ""
    before     = hypothesis.before_value or ""
    text_norm  = _normalize(text,   synonym_map_enabled=cfg.synonym_map_enabled)
    before_norm = _normalize(before, synonym_map_enabled=cfg.synonym_map_enabled)
    # Q1 — iterate matches; suppress those present in before_norm.
    for regex in (_RE_Q1_IF_THEN, _RE_Q1_ARROW_TREE):
        for m in regex.finditer(text_norm):
            sub = m.group(0)
            if sub not in before_norm:
                return AntiHardcodeResult(
                    verdict="FAIL", rule_id="Q1.if_then_decision_tree",
                    matched_substring=_trim(sub),
                )
    # Q2/Q4/Q5/Q6 — same treatment.
    ...
```

**Pros**:
- Tiny implementation (~15-20 LOC for Q1; ~50 LOC across all rules).
- Semantically clean: "the candidate cannot ADD if-then narration
  that wasn't already there". Matches the spirit of anti-hardcode
  review (catch new hardcode, not retroactively fail production).
- Backwards-compatible: candidates whose `after_value` doesn't
  contain the baseline behave exactly as before.
- audit-friendly: the reported matched_substring is from
  `after_value` directly; users can read where in the field text
  the new hardcode lives.
- Naturally handles REWRITE: the rewritten field's new patterns
  (those not in baseline) still get caught.
- Works even when normalization differs between before/after (a
  candidate writes `if X → Y`, baseline has `when X -> Y` — both
  normalize to the same string, so the candidate's pattern is
  suppressed if and only if the equivalent appears in baseline).

**Cons**:
- "double-encoding" bypass: a candidate that copies a baseline
  if-then to a NEW position in the same field would not be
  flagged. **Risk: low** — current proposer LLM doesn't game the
  detector; this would require an adversarial proposer. If it
  ever happens, audit will catch it (the field still contains
  the pattern; reviewers see the doubled if-then).
- A baseline that already contains `if X → Y` essentially "grants
  license" to similar patterns later: if a candidate adds `if X → Z`
  with the same `X`, the substring `if X →` is in baseline, so the
  new arrow target `Z` only matters if the 120-char tail differs.
  In practice this means baseline-blessed prefixes can be reused.
  **Risk: low** — and arguably correct (prefix is already accepted
  by production).

### Option C — Tighten the regex (require additional hardcode markers)

**Scope**: extend `_RE_Q1_IF_THEN` / `_RE_Q1_ARROW_TREE` to require
co-occurrence of additional "hardcode signals" — e.g. case_id-like
tokens, raw eval phrases, enumeration brackets. Without those, the
narrative is presumed legitimate.

**Pros**:
- Doesn't change detector scope.
- Reduces false-positives on the rule itself.

**Cons**:
- The hardcode markers (case_ids, eval phrases) are already
  Q4's responsibility — duplicating them in Q1 changes the rule
  semantics, not the scope.
- Risk of over-tightening: a real if-then dump that doesn't happen
  to mention a case_id would slip through.
- Doesn't generalize to Q2/Q5/Q6 false-positive class.

### Option D — Disable synonym_map in S-Y2 only

**Scope**: set `anti_hardcode.synonym_map_enabled: false` in
`autoloop/config.yaml` for the duration of S-Y2.

**Pros**:
- Zero code change.
- Removes the `when` → `if` and `->` → `→` substitutions; the
  baseline's `when available...-> record_outcome` no longer
  normalizes to a Q1-matching pattern.

**Cons**:
- Re-opens the **Codex Axis B bypass** that synonym_map was added to
  close (S-Auto-5, 2026-05-28; see `config.yaml:362-369`):
  candidates can write `Whenever X => Y` to evade Q1.
- Trades a confirmed framework defect for an old known bypass.
- Doesn't fix the root cause; just hides it.

## 7. Recommended option + rationale

**Recommended: Option B (baseline-whitelist filter).**

1. **Smallest viable change**: ~15-20 LOC for Q1 alone; ~50 LOC
   covers Q1/Q2/Q4/Q5/Q6 uniformly. No prompt changes, no config
   schema changes, no gate logic changes, no re-bless of
   `m-auto-7-prepilot-baseline-20260608`.
2. **Semantically correct**: the anti-hardcode review's purpose is
   to catch what the candidate ADDS, not to retroactively fail
   patterns production has already accepted (which would otherwise
   require Codex to also fail the existing skill YAMLs — they
   don't, because production prose with `when X -> Y` is
   legitimate FAQ-flow notation).
3. **Audit-friendly**: matched substrings remain in
   `after_value` coordinates; existing
   `runs/<id>/anti_hardcode_verdict.json` schema unchanged.
4. **Generalizes uniformly**: the same predicate (`matched in
   before_norm`) applies to all Q-rules without per-rule changes.
5. **Low risk**: the only bypass concern (double-encoded baseline
   patterns) requires adversarial proposer behavior and remains
   visible to human / Codex review.

**Why not A**: the diff approach is more general but harder to
audit and over-engineers for the actual hit pattern observed
(exp-70 is pure-additive; the suppressed match is contiguous in
`after_value`).

**Why not C**: tightening Q1's signal goes the wrong direction —
risks under-detection of real new hardcode. Q4 already owns
case-id signals.

**Why not D**: trades a confirmed bug for a known bypass —
strictly worse.

## 8. Scope split + delivery priority

### S-Y1.5b (micro patch, ~0.5-1 d)

**§3.2 Layer**: `infra` (autoloop sandbox; eval-framework-adjacent —
§5.8 framework-defect priority precedent applies in spirit, since
the defect blocks legitimate on-gap candidates during an active
semantic pilot).

**§7 stanza**: §7-EXEMPT (pure-infra, sandbox scope refinement,
no semantic surface touched, no rule-body change, no normalization
change).

**Scope** (3 sentences):

1. Refactor `anti_hardcode_check()` to compute
   `before_norm = _normalize(hypothesis.before_value)` once at
   entry and pass it (or the regex `finditer` machinery) into
   each rule so matches contained in `before_norm` are suppressed.
2. Rewrite the Q1/Q2/Q4/Q5/Q6 rule helpers to iterate
   `finditer` over `text_norm` rather than returning the first
   match unconditionally, returning the FIRST match not in
   `before_norm` (or None).
3. Add 5 unit tests (one per affected rule) using fixtures that
   embed the matching pattern in the baseline + verify PASS, and
   one fixture that adds a NEW pattern not in baseline + verifies
   FAIL.

**Test/eval requirements**:

- `tests/test_anti_hardcode_check.py` — extend with:
  * `test_q1_arrow_tree_baseline_match_suppressed` — feed exp-70's
    actual `(before_value, after_value)` pair; assert PASS.
  * `test_q1_arrow_tree_new_match_still_caught` — baseline = simple
    text; after_value = baseline + new "if X → Y" clause; assert
    FAIL.
  * `test_q1_if_then_baseline_match_suppressed` — synthetic IF/THEN
    pattern in baseline; assert PASS on pure-additive.
  * `test_q2/q4/q5_baseline_match_suppressed` — mirror for the
    other rules (lightweight).
  * `test_double_encoding_bypass_still_visible_to_audit` — baseline
    has `if X → Y` once; after_value contains it twice; assert PASS
    + the test docstring documents the "double-encoding" trade-off
    + the audit field is unchanged.
- `tests/test_real_meta_agent_calibration.py` — the 17-fixture
  calibration suite (per `config.yaml:368`) must continue to
  produce 11 FAIL + 4 PASS + 2 FLAG outcomes (the calibration
  asserts no regression on the existing meta-agent samples).
- No re-bless needed.
- No §5.6 bad-case rerun needed.

**Per-sub-sprint Codex**: REQUIRED (per §4.3 — touches the sandbox
that enforces §1.7 forbidden list). Codex focus questions:
- Is "baseline-whitelist suppression" semantically equivalent to
  "the candidate did not add this pattern"? Yes (if normalize is
  the same function on both sides) — Codex verifies the
  normalization is byte-equivalent.
- Does the double-encoding bypass concern (a candidate that copies
  a baseline if-then into a new position) introduce a real attack
  vector? Calibration: the current proposer LLM does not game the
  detector; if a future proposer becomes adversarial, Q4 (case-id
  leak) + human / Codex review catch it.
- Are there other tests in `tests/test_anti_hardcode_check.py`
  that rely on the OLD behaviour (Q-rule fires on baseline
  patterns)? Verify none do.

**Owner-cadence note (2026-06-10 — n=8 tranche complete)**: with the
n=8 tranche closed and exp-70/exp-76 both Q1-blocked with byte-identical
matched_substring (§5.1), the timing question is no longer "land before
the in-flight tranche completes" but **"land before deciding §3.4
fallback"**. Two cadence options:

- **(a) S-Y1.5b lands NOW**, then run a SMALL re-tranche
  (e.g. `-n 3` targeted at `resolve_faq.$.procedure` with the
  same PILOT_PRIMARY_TARGETS) to re-judge the Q1-blocked candidate
  shape. ~0.5-1 d patch + ~1.5 h re-tranche = answer in ~half a day.
  **Strongly recommended** — this is the only path that actually
  answers the S-Y2 PRIMARIES question instead of preemptively
  concluding §3.4.
- **(b) Skip S-Y1.5b, declare §3.4 fallback now**. Hand-author the
  CS4 procedure. Q1 patch deferred to post-M-Auto-7 backlog. Saves
  ~0.5-1 d but **forfeits the answer to the pilot's core question**
  ("can autoloop produce a §1.7-acceptable candidate for CS4?").
  Acceptable only if the human is satisfied with §3.4 as the pilot
  outcome regardless of Q1.

(a) is the research-agent recommendation; (b) is deliver-agent
discretion.

## 9. Hard fences + non-goals

**Hard fences**:

- **No edits** to `autoloop/autoloop/scoring/*.py` (would trigger
  `gaming.scoring_code_drift.sha_changed`).
- **No edits** to `autoloop/autoloop/sandbox/yaml_diff_validator.py`,
  `applier.py`, or `content_validator.py` (only
  `anti_hardcode_check.py`).
- **No edits** to `autoloop/program.md` (locked).
- **No edits** to `autoloop/config.yaml` (the
  `anti_hardcode.synonym_map_enabled: true` setting stays — see
  Option D rejection).
- **No edits** to `eval_interactive/**`, `server/**`, `docs/foundational/**`,
  `docs/sprints/*`, `docs/milestones/*`.
- **No change to normalization** (`_normalize` keeps its current
  behavior; the fix is at detector scope, not normalization).
- **No change to rule-body regex patterns** (Q1/Q2/Q4/Q5/Q6 regex
  stay as-is; only the scope of what they scan changes).
- **Shadow firewall unaffected**: the fix lives entirely in the
  sandbox pre-eval; no eval result is read; no shadow data
  involved.

**Non-goals**:

- **Not** generalizing to a diff-based detector (Option A is more
  ambitious; defer to v2 if Option B proves insufficient).
- **Not** changing the gate's 5-layer lexicographic verdict, the
  pilot_snapshot block, the 4-layer hit-rate metric, or any
  observation in the 2026-06-08 substrate proposal.
- **Not** opening a v2-grade hill-climbing memory / partial-keep
  proposal — that's a separate question raised by the 3
  task-hardness discards in this tranche; premature at n=4.
- **Not** addressing the gate-correct task hardness discards
  (exp-69 / exp-71 / exp-72). They are working as designed and the
  2026-06-08 proposal's §3.4 fallback path is the right response.
- **Not** changing the `synonym_map_enabled: true` setting
  (Option D rejection).

## 10. Risk + compounding-effect analysis

### 10.1 Implementation risks

| Risk | Severity | Mitigation |
|---|---|---|
| Double-encoding bypass (candidate copies baseline if-then to a new position) | L | Adversarial-only; current proposer doesn't game; Q4 + human/Codex review catch real exploit attempts; audit field still shows the pattern |
| `_normalize` differing between before/after due to config drift | L | `synonym_map_enabled` is a single config read; pass it explicitly to both `_normalize` calls in the entry point |
| 17-fixture calibration suite (`test_real_meta_agent_calibration.py`) regresses | M | All 17 fixtures use synthetic before_value=""; the fix is no-op when before is empty. Add a smoke test that asserts the 11/4/2 outcome unchanged |
| Codex Axis B bypass (`Whenever ... =>`) re-introduced | None | Axis B was about normalization; this fix doesn't touch normalization |
| Generalization to Q2/Q4/Q5/Q6 introduces new false negatives in those rules | L | Each rule's existing positive test still passes when before_value is empty (which is the case for all existing fixtures) |

### 10.2 Non-implementation risks (not patching)

| Risk | Severity |
|---|---|
| S-Y2 remaining tranche (exp-73..76) blocked on Q1 false-positive when proposer picks resolve_faq.procedure again | **M** — proposer's S-Y1.5 steering biases toward this surface; expected hit rate on this slot is now non-zero per the pilot block |
| Future autoloop sub-sprints (S-B CS2-original; M-Auto-8 candidates) hit the same defect | **M** — defect is structural; will recur whenever a new pilot's target surface has baseline arrow-narrative |
| §3.4 fallback gets blamed for "autoloop can't even produce a candidate" when in fact it produced exactly one on-target candidate and the sandbox false-rejected it | L (cosmetic) — but matters for autoloop's credibility going into M-Auto-8 planning |

### 10.3 Compounding effects

- **Patch ordering**: must run on a clean tree (per the existing
  `R-autoloop-run-sweeps-dirty-index` carry-over). If landed
  mid-tranche, halt the in-flight autoloop, commit, restart.
- **Calibration ordering**: the 17-fixture calibration is the gate
  for the patch's correctness; run it before merging.
- **Codex review ordering**: per-sub-sprint Codex required even for
  a micro-patch like this, because it touches a §1.7 forbidden-list
  defense.
- **Post-patch validation**: if S-Y1.5b lands and S-Y2 has remaining
  `-n` budget, re-run exp-70 (same target_skill + target_field on
  the same pre-pilot baseline). Confirm it now passes Q1 and is
  evaluated against Tier-0/1/2/3 normally. If the original
  rationale leads to gate-correct discard, that's a clean
  task-hardness result; if it KEEPS, that's a S-Y2 PRIMARY SUCCESS
  outcome.

## 11. Observability / report implications

- **`runs/<exp-N>/anti_hardcode_verdict.json`** — schema unchanged.
- **`autoloop audit --experiment exp-N`** — show the
  matched_substring + a new field `matched_in_baseline: true|false`
  so reviewers can immediately distinguish "real hardcode" from
  "baseline artifact" (purely informational; this would close the
  feedback loop on the original exp-70 surprise).
- **`tests/test_anti_hardcode_check.py`** — adds 5 new tests + a
  baseline-fixture file. No removal of existing tests.
- **17-fixture calibration suite** — unchanged outcome
  (11 FAIL / 4 PASS / 2 FLAG) — explicit assertion in test.

## 12. Sources of truth used in this RCA (2026-06-10 update)

All paths HEAD-verified; n=8 tranche data verified post-tranche close
on `auto-loop-branch`:

- `autoloop/autoloop/sandbox/anti_hardcode_check.py` lines 69-95
  (`_SYNONYM_MAP`, `_RE_WHEN_WORD_BOUNDARY`, `_normalize`), 128-156
  (`_RE_Q1_IF_THEN`, `_RE_Q1_ARROW_TREE`, `_q1_if_then`), 336-355
  (`anti_hardcode_check` entry, `text = hypothesis.after_value`).
- `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml`
  HEAD `$.procedure` (the baseline text that triggers Q1 false-fire).
- `autoloop/results/experiments.jsonl` — exp-70 row
  (timestamp `2026-06-09T00:11:46+00:00`) AND exp-76 row
  (later in tranche; same target `resolve_faq.$.procedure`,
  byte-identical `matched_substring` and `rule_id` — see §1
  block-quote). Confirmed both have `matched_substring entirely in
  before_value (normalized) = True` via reproduction script.
- `autoloop/results/runs/exp-69/eval-results.json` +
  `exp-71/eval-results.json` +
  `exp-72/eval-results.json` +
  `exp-75/eval-results.json` — per-case flips that confirm "0
  PRIMARIES flipped, NEIGHBOR + unrelated improvements only" (§5.2
  table).
- `eval_interactive/results/m-auto-7-prepilot-baseline-20260608/bad_cases/aggregated.json`
  + `anchor_outcome/aggregated.json` — baseline per-case
  majority_passed signals for the diff above. Baseline bad_cases =
  8/17 passing; `cs_uc_a_no_ad_id_ad_specific` and
  `cs_uc_a_loaded_listing` are FAIL (the PRIMARIES we want flipped);
  `cs_uc_a_generic_policy_question` is PASS (the ANTI-KILL control
  to preserve).
- `autoloop/config.yaml:362-369` (`synonym_map_enabled: true`
  decision history; S-Auto-5 Fix-C step 2 2026-05-28).
- `docs/solutions/2026-06-08-autoloop-mechanism-audit-and-feedback-loop-tightening.md`
  (the substrate proposal this doc complements — particularly §5.5
  P2-B and §11.3 strong-pass + 0-keep path).
- `docs/sprint_objective.md` §Scope (CS4 entity-context guidance
  most plausibly lands in resolve_faq.procedure / grounding_instruction /
  critical_steps[*].desc).
- Blast-radius scan script (in this doc §3.4): reproduces the
  Q1 false-fire on the HEAD baseline of all 6 skill YAML × 4 field
  classes; exactly 1 slot triggers.

## 13. Open questions for deliver-agent (2026-06-10 update)

1. **S-Y1.5b before §3.4 fallback decision, or skip directly to §3.4?**
   The n=8 tranche reframed this question. Research agent strongly
   recommends **S-Y1.5b first** (§8 cadence option (a)) — without it,
   §3.4 fallback would be invoked while the autoloop's most-on-target
   candidate stream (exp-70 / exp-76, byte-identical Q1 fate) remains
   unevaluated. Going §3.4 directly answers a different question
   ("hand-author works") than the pilot's actual question ("can the
   autoloop produce a §4.1-acceptable candidate"). Deliver-agent
   discretion.
2. **Generalize to Q2/Q4/Q5/Q6 in the same patch, or Q1-only?**
   Research agent recommends generalizing (the abstraction
   `_filter_matches(rule_fn, text_norm, before_norm)` makes Q1-only
   and all-rules implementations very close in LOC; better to fix
   the rule-family symmetry once than carry a known structural
   defect on Q2-6). Deliver-agent may prefer Q1-only for minimal
   change surface.
3. **Add `matched_in_baseline` field to the audit verdict (§11)?**
   Research agent recommends yes — close the audit feedback loop
   for the next time someone reads exp-70-style verdicts. Trivial
   addition. Deliver-agent may defer to a separate observability
   ticket.
4. **(NEW 2026-06-10) Post-S-Y1.5b re-tranche size**: if (a) is
   chosen, what `-n` for the re-tranche? Research agent recommends
   **`-n 3` targeted at `resolve_faq_grounded_answer.yaml`** — large
   enough to give the proposer ≥ 1 retry on the same procedure
   surface after Q1 is fixed, small enough to bound at ~4.5 h. If
   the re-tranche produces 0 keeps but eval-reaches all 3, the
   PRIMARIES question is answered honestly; §3.4 fallback follows
   with the autoloop-capability question resolved.
5. **(NEW 2026-06-10) Document the "no PRIMARIES flipped" finding
   for M-Auto-7 close**: regardless of S-Y1.5b decision, the §5.2
   per-case forensic should land in the M-Auto-7 milestone-close
   handoff (deliver-agent's call: as part of S-Y2 handoff, or as a
   §0 observation in the milestone close package). Future
   targeted-pilot designs should know that the n=8 tranche
   improved NEIGHBORS but not PRIMARIES — that's a data point about
   M-Auto-1A v1 autoloop ceiling on this task class.

---

End of proposal (2026-06-10 update). Awaiting human + deliver-agent
decision on S-Y1.5b + re-tranche cadence vs direct §3.4 fallback.
No code changes have been made by the research agent.
