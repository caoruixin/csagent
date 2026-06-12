---
title: Anti-hardcode detector calibration vs Kernel — demote syntactic regex from FAIL to FLAG_FOR_CODEX, restore Codex semantic review as the binding gate
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-06-11
mode: forward-looking
notes: >
  Path-1 forward-looking research on the anti-hardcode rule
  architecture. Triggered by a human observation during M-Auto-7 S-Y2
  Part C: the autoloop sandbox's Q1.if_then_decision_tree detector
  fires on candidate prose that the production skill YAMLs already
  encode legitimately as few-shot teaching, while Codex review (the
  Kernel 9-question semantic gate) would not reject those same
  patterns. Survey of production skill YAMLs confirms: blessed prose
  contains illustrative examples, principle-level if/when narratives,
  and per-UC routing matrices that are semantically legitimate per
  Kernel Q3/Q6 but indistinguishable to the regex from "if-else rule
  dump." This proposal demotes Q1/Q2/Q5 (semantic-judgment-required
  patterns) from FAIL to FLAG_FOR_CODEX, keeps Q4 (case_id literal
  leakage) as FAIL, and routes the semantic judgment back to per-PR /
  per-sub-sprint Codex review where the Kernel already lives.

  Sibling doc: 2026-06-09-anti-hardcode-q1-pure-additive-false-positive.md
  fixed the detector's SCOPE (full after_value vs diff-aware). This
  doc addresses the detector's SEVERITY (FAIL vs FLAG_FOR_CODEX for
  semantic patterns). The two are orthogonal and complementary.
---

# Anti-hardcode detector vs Kernel — demote semantic Q-rules to FLAG_FOR_CODEX

## 1. Executive summary

**The problem (calibration, not bug)**: the anti-hardcode rule
machinery has three layers — the constitutional principle
(§1.7), the Kernel 9-question semantic review (Codex), and the
autoloop sandbox detector (deterministic regex). The detector is
implemented as a **syntactic proxy** for the semantic principle. It
inevitably mis-classifies in both directions on the kind of prose
that lives in real skill YAMLs:

- **Over-fires** on principle-level narrative with `if X → Y` or `if X
  then Y` shape — including legitimate few-shot teaching like
  *"when the user's question is anchored to a specific listing,
  resolve from that listing's state alongside the grounded knowledge
  surface."* Two of the S-Y2 tranche's most-targeted candidates
  (exp-70 / exp-76 on `resolve_faq.$.procedure`) were killed for this
  reason.
- **Under-fires** on semantically equivalent hardcode that happens to
  use a non-matching surface form: production
  `confirm.yaml:$.procedure` ships `(e.g., 'thanks', 'that helps',
  'yes')` — three quoted keyword examples comma-joined — and Q1
  PASSES it; the same content written as `'thanks' or 'that helps' or
  'yes'` would FAIL Q1.or_keywords. Same semantic content, opposite
  verdict.

**The actual constitutional answer**: §1.7 forbids "**using prompt as
an if-else rule dump**" and "adding UC-specific hard rules for
**soft semantic decisions**." Neither prohibits illustrative examples
or principle-level narrative that lets the LLM judge applicability.
The Kernel 9-question review (§4.1) operationalizes this distinction
correctly through **Q3** ("could the same outcome be achieved by
projecting a soft signal to the LLM... instead of a hard branch") and
**Q6** ("if-else block vs principle-level or observable-state
guidance").

**The recommended fix**: demote the SEMANTIC-judgment rules
(Q1.if_then_decision_tree, Q1.arrow_tree, Q1.or_keywords,
Q1.contains_matches, Q2.must_always, Q2.tier0_invention,
Q5.bot_must_always, Q5.do_not_consider, Q5.force_assistant,
Q5.standalone_must) **from FAIL to FLAG_FOR_CODEX**. Keep Q4
(`case_id_literal` / `id_assignment_literal`) as FAIL — that is the
ONE rule whose surface form is unambiguous (raw eval CaseSpec id in
`after_value` IS the §1.7 "encoding raw eval phrases" prohibition,
no semantic interpretation needed). The semantic gate is restored to
its proper home: per-sub-sprint / milestone-shared Codex review using
the Kernel.

**Relationship to existing solutions**:
- `2026-06-08-autoloop-mechanism-audit-and-feedback-loop-tightening.md`
  (implemented) — unrelated; addressed prompt-builder + steering. Not
  superseded.
- `2026-06-09-anti-hardcode-q1-pure-additive-false-positive.md` (status
  proposal) — addresses detector SCOPE (full after_value vs diff-aware
  baseline suppression). Complementary, not superseded; this doc
  addresses detector SEVERITY which is orthogonal. Either or both can
  land.

**Implementation scope**: ~30 LOC in
`autoloop/autoloop/sandbox/anti_hardcode_check.py` (`_RULES` registry
severity changes + a config-driven severity-override map for
reversibility) + ~50 LOC of test updates + the 17-fixture calibration
re-baseline (currently 11 FAIL / 4 PASS / 2 FLAG → after demote
~2 FAIL / 4 PASS / 11 FLAG). No prompt changes, no gate logic changes,
no re-bless.

## 2. Investigation scope + method

**Triggered by**: human review question during M-Auto-7 S-Y2 Part C
(2026-06-11): "anti-hardcode 这个规则是写在哪里的... 在 prompt、 skill
YAML 里面，它有的时候需要一些 few shot 的，相当于它的一些教学式案例
... 现在的规则是否合理呢？"

**Method**: (a) trace all three layers of the rule machinery
(constitution / kernel / detector) to their source-of-truth files;
(b) read the kernel's 9 questions verbatim and identify the semantic
distinction it operationalizes; (c) scan all six skill YAMLs' four
mutable-field-class slots for production-blessed prose that matches
the patterns the detector watches for; (d) cross-reference the
production prose against §1.7 + Kernel Q3/Q6 to classify each
match as "would Codex review approve under Kernel?" vs "would Codex
reject?"; (e) verify the detector's actual regex behavior against the
production text via a reproducible Python script.

**Out of scope**: any change to §1.7 itself, the 9-question Kernel,
the constitutional layer, the 5-layer fitness gate, the mutable
surface, the shadow firewall, the lessons system, the pilot
steering, the 4-layer hit-rate metric. The defect addressed here is
ONLY the autoloop sandbox detector's FAIL severity on Q-rules whose
correct verdict is "semantic review required."

## 3. Current-state survey

### 3.1 Three-layer rule architecture

| Layer | File | Verdict produced by | How |
|---|---|---|---|
| **Constitutional principle** | `docs/current/iteration_governance.md` §1.7 | Doctrine (not a verdict) | Human-written governance text |
| **Codex review kernel** (§4.1) | `docs/current/anti-hardcode-review-kernel.md` | `approve` / `approve with downgrade-to-signal follow-up` / `reject as semantic hardcode` / `needs human architecture decision` | LLM (Codex) semantic walk through 9 questions |
| **Autoloop sandbox detector** | `autoloop/autoloop/sandbox/anti_hardcode_check.py` | `PASS` / `FAIL` / `FLAG_FOR_CODEX` | Deterministic regex (no LLM) |

Layer 1 governs. Layer 2 is the binding semantic gate per `§4.1`. Layer
3 was added in S-Auto-4 (2026-05-27) as a propose-stage cheap
filter so the autoloop's hill-climbing doesn't waste eval budget on
obvious hardcode patterns. Layer 3's PASS/FAIL was always intended as
a proxy for Layer 2, not a replacement.

### 3.2 What §1.7 actually forbids (verbatim)

From `docs/current/iteration_governance.md` §1.7:

> - encoding raw eval phrases into Java or prompt
> - **adding UC-specific hard rules for soft semantic decisions**
> - widening eval spec to accept a genuine bot mistake
> - optimizing visible eval at the cost of shadow/generalization
> - **using prompt as an if-else rule dump**

The two bolded items are the ones the detector tries to enforce. The
operative phrases are:

- "**rule dump**" — a decision tree poured into the prompt as a
  substitute for LLM semantic judgment, NOT any if-then sentence.
- "**hard rules for soft semantic decisions**" — rules that bind the
  LLM's choice on dimensions §1.3 says the LLM owns (user goal,
  issue relation, use case hypothesis, drift, next action, escalation
  posture, response strategy, wording). Principle-level narrative or
  illustrative examples that LET the LLM judge are not bound by §1.7.

### 3.3 Kernel Q3/Q6 — the operative semantic distinction (verbatim)

From `docs/current/anti-hardcode-review-kernel.md`:

> **Q3.** Could the same outcome be achieved by **projecting a soft
> signal to the LLM** (an additional projected slot, a candidate
> list, a diagnostic flag) instead of a hard branch in Java or the
> prompt?
>
> **Q6.** Does the change add an **if-else block** to the prompt
> instead of **principle-level or observable-state guidance**?

Q3 is the **substitutability test**: if the soft-signal-projection
alternative exists and was not taken, the change is a hardcode. If the
projection alternative doesn't exist or is structurally equivalent to
the narrative, the change is teaching.

Q6 is the **shape test**: prompt prose that reads as "principle X
applies in observable-state Y" is acceptable; prose that reads as
"`if user_message contains 'X' then call tool Y`" is not.

Together they capture the constitutional intent precisely. A regex
cannot.

### 3.4 Production-blessed prose audit — what currently ships

Programmatic scan of HEAD's six skill YAMLs × four mutable
field-classes, looking for shapes the Q-rules watch for. The skill
files have all been through Codex review (§4.1) and human merge
across multiple sprints; this is "blessed prose."

| Skill / field | Pattern | Production text (excerpt) | Kernel verdict | Detector verdict |
|---|---|---|---|---|
| `discover_triage.$.procedure` | 8 quoted user-utterance few-shot examples | `"how do I X"`, `"what items are allowed"`, `"how do I receive payment"`, `"how do I get paid when I sell"`, `"how does payout work"` | Approve (Q3: candidate_use_cases projection covers; the literals teach the LLM the SHAPE of FAQ questions, not bind to literal text) | PASS (Q1.or_keywords needs " or " separator; production uses commas) |
| `discover_triage.$.procedure` | per-UC routing matrix in prose | `"payment / sale-proceeds questions point to UC-F; how-to-post and general advertising questions to UC-B; messaging to UC-C; account / login to UC-D"` | Approve with downgrade-to-signal follow-up (Q3: candidate_use_cases_named projection covers; the narrative makes the projection's options legible) | PASS (no "then" / "→" / " or " joins — uses semicolons) |
| `discover_triage.$.procedure` | if/when chains describing classify_use_case threshold | `"When the user's intent is clear (or you can infer it with a supporting detail), call classify_use_case... Otherwise ask one clear clarifying question..."` | Approve (Q6: principle-level guidance about classify_use_case confidence threshold) | PASS (after normalization "when→if", no "then"/"→" within 120 chars) |
| `resolve_faq.$.procedure` | sequence narration with arrow notation | `"FAQ-path RESOLVE flow (S1): the intended terminal sequence is search_knowledge -> resolve_article -> grounded customer-facing answer ... -> record_outcome. Only escalate via request_handover after a valid resolve attempt cannot complete..."` | Approve (Q3: no soft-signal substitute — this IS the FAQ tool-sequence contract; Q6: principle-level workflow guidance) | **FAIL Q1.arrow_tree** (post-normalization "when→if" + "->→→" produces `"if available, otherwise its article_id) → record_outcome"`) — already documented in 2026-06-09 sibling doc |
| `resolve_faq.$.escalation_policy` | explicit `if (a) ... priority 1 ... (b) ... priority 2 ...` matrix with quoted escalation_reasons | `"if (a) the user explicitly requests a human (use escalation_reason='user_requested', priority 1), (b) ..."` | Approve (Q3: escalation_reason enum is Runtime-owned schema, not LLM judgment; making the enum legible to the LLM is necessary projection equivalent) | PASS (after normalization, has "if (a)" but no "then" / "→" within 120 chars) |
| `resolve_faq.$.grounding_instruction` | two distinct `If ...` narrative segments | `"If tool data contains specific information about the user's case..."`, `"If search_knowledge returns no viable hit..."` | Approve (Q6: principle-level conditioning, not decision-tree dump) | PASS (no "then" / "→" in either fragment) |
| `resolve_intake.$.procedure` | e.g. JSON shape example | `(e.g. {"intake_fields": {"field_a": "value_a", ...}})` | Approve (illustrative — teaches the JSON shape the LLM must produce) | PASS (no Q-rule trigger) |
| **`confirm.$.procedure`** | **3-token quoted keyword example for resolution-acknowledgment** | `(e.g., 'thanks', 'that helps', 'yes')` and `(e.g., 'no', 'still not working', 'I need more help')` | Approve (Q6: illustrative few-shot teaching of resolved-vs-not-resolved POSTURE; Q3: no projected slot for "user posture" — narrative IS the teaching surface) | **PASS** (Q1.or_keywords needs " or "; production uses commas) |

**Critical finding**: the same `confirm.yaml` content rewritten as
`(e.g., 'thanks' or 'that helps' or 'yes')` would **FAIL** Q1.or_keywords
— identical semantic intent, opposite detector verdict. This is the
clearest single demonstration of detector brittleness.

### 3.5 Detector regex inventory + intended target

| Rule id | Regex / predicate (paraphrased) | Intended target (§1.7 mapping) | Failure mode |
|---|---|---|---|
| Q1.if_then_decision_tree | `\bif\b ... \bthen\b` within 120 chars | "if-else rule dump" | **Over-fires** on principle-level if/when narrative |
| Q1.arrow_tree | `\bif\b ... → ... ` within 120 chars (after `->/=>/==>` → `→` normalization) | "if-else rule dump" with arrow shorthand | **Over-fires** on sequence narration ("search → resolve → record") and on baseline text with arrows |
| Q1.or_keywords | ≥3 quoted-or-bare tokens joined by " or " | UC-specific keyword enumeration | **Under-fires** when comma-joined; **over-fires** on illustrative few-shot with " or " separator |
| Q1.contains_matches | `.contains(` / `.matches(` / `.startswith(` / `.endswith(` literal | code-style enumeration in prose | Generally correct (code-style is rare in legit prose); rare false positive |
| Q2.must_always | `runtime\|the bot\|the system\|the assistant must (always\|never\|reject\|escalate\|refuse\|block)` near a §1.3 soft-semantic dimension | Tier-0 invariant invention against soft dimensions | **Reasonable signal but semantic judgment required** — there are legitimate "the bot must always" statements about Runtime-owned dimensions (PII, safety) |
| Q2.tier0_invention | `tier-0\|invariant\|hard gate` within 80 chars of `add\|new\|introduce\|require` | Explicit Tier-0 mint | Generally correct (this is a meta-discussion pattern); low false positive |
| Q4.case_id_literal | `cs[0-9a-z_]{2,}` word-boundary | Raw eval CaseSpec id leakage | **Correctly correct** — §1.7 "encoding raw eval phrases" is unambiguous |
| Q4.id_assign | `session_id\|case_id\|iteration_id\|exp_id` followed by `:` / `=` | Identifier binding | Generally correct (low false positive); semantic-judgment-light |
| Q5.bot_must_always | `the bot must (always\|never\|treat)` | LLM-ownership shrink | **Semantic judgment required** — there are legitimate "the bot must always" statements (e.g., safety floor) |
| Q5.do_not_consider | `do not consider X` where X ∈ soft dimensions | LLM-ownership shrink | Semantic judgment required |
| Q5.force_assistant | `force the assistant to / require the assistant to ...` | LLM-ownership shrink | Semantic judgment required |
| Q5.standalone_must | borderline MUST near soft dimension | Catch-all | Already FLAG_FOR_CODEX (correctly demoted) |

**The pattern**: Q4.* and Q2.tier0_invention can be correctly judged
by regex (the surface form IS the offense). Q1.if_then / Q1.arrow /
Q1.or_keywords / Q2.must_always / Q5.* require semantic judgment about
whether the pattern denies LLM ownership of a §1.3 soft dimension.

### 3.6 The mismatch (one diagram)

```
   Constitutional principle (§1.7)         "rule dump" / "hard rules
                                           for soft semantic decisions"
                |
                |  intent: forbid LLM-ownership-denying patterns
                v
   Kernel 9-question review (§4.1)         Q3 (soft-signal substitutability)
                                           Q6 (if-else block vs principle-level)
                                           — SEMANTIC judgment by Codex
                |
                |  intended proxy
                v
   Autoloop sandbox detector               Q1.if_then  /  Q1.arrow_tree
                                           Q1.or_keywords / Q5.bot_must_always
                                           — SYNTACTIC regex (no LLM)

      ^                                                                ^
      |                                                                |
      +---  match form, miss semantic intent (over- and under-fires) ---+

      Q4 (case_id literal) is the exception — surface == semantic intent.
```

The Q4 surface form IS the §1.7 prohibition (a literal CaseSpec id in
the prompt body is the offense; nothing semantic to interpret). Every
other Q-rule's surface form is a NECESSARY-but-INSUFFICIENT condition
for the §1.7 prohibition.

## 4. Gap analysis

### 4.1 The detector's actual role in the loop

`autoloop/loop.py:226-238` (the anti-hardcode call site):

```python
# --- 4. Anti-hardcode (S-Auto-4 real detector).
ah_verdict = _anti_hardcode_check_fn(hypothesis, config=config)
result.anti_hardcode_verdict = ah_verdict
if ah_verdict.verdict == "FAIL":
    result.decision = "discard"
    result.discard_reason = f"anti_hardcode_rejected:{ah_verdict.rule_id}"
    _persist_iteration(result, config, root, dry_run=dry_run, applied=None)
    return _finalize(result, started)
if ah_verdict.verdict == "FLAG_FOR_CODEX":
    # Observation-only: continue the iteration; surface the
    # flag for Codex / human review via the iteration record.
    result.anti_hardcode_flag_for_codex = True
```

Today: FAIL kills the iteration before eval; FLAG_FOR_CODEX is
observation-only (continues to eval; flag persisted to record). After
demote: Q1/Q2/Q5 will produce FLAG_FOR_CODEX → iteration proceeds to
eval; the candidate either passes the 5-layer gate (becomes KEEP, then
human + Codex §4.1 semantic review at sub-sprint close decides) or
fails the gate (genuine task hardness).

### 4.2 Why Codex review can / should do this judgment

`docs/current/iteration_governance.md` §4.3 already mandates Codex
per-sub-sprint review when autoloop output is the §1.7 binding gate
(trigger #2). M-Auto-7 S-Y2 explicitly invokes this — see
`docs/sprint_objective.md` §Codex review plan:

> Per-sub-sprint Codex REQUIRED (trigger #2 — autoloop output is the
> §1.7 binding gate). Review the merged skill-yaml diff under the
> §4.1 nine-question kernel; verdict to `docs/codex-findings.md`
> (§4.2 header).

So for the targeted pilot, Codex IS the binding semantic gate. The
detector's role degenerates to "cheap filter to discard obvious
hardcode before eval burn." For ambiguous patterns where semantic
judgment is required, the detector should defer to Codex, not
preempt it.

### 4.3 What the detector still adds (Q4-only world)

After demote, the detector retains real value at Q4:

- Q4.case_id_literal — catches `cs011 / cs015 / cs_uc_a_no_ad_id /
  cs59s*` and similar in the candidate's `after_value`. The §1.7
  "encoding raw eval phrases into prompt" prohibition is the
  literal surface form, no semantic interpretation. Keeping Q4 as
  FAIL is structurally correct.
- Q4.id_assign — catches `session_id=` / `case_id=` patterns.
  Identifier-binding language. Same as above.
- Q2.tier0_invention — catches `add tier-0` / `new invariant` /
  `introduce hard gate` patterns. Tier-0 minting requires explicit
  authorization (§3.2). Surface form is generally unambiguous.

So Q4.* and Q2.tier0_invention remain FAIL. Everything else moves to
FLAG_FOR_CODEX.

## 5. Design alternatives

### Option A — Demote Q1.* / Q2.must_always / Q5.* to FLAG_FOR_CODEX (recommended)

**Scope**:

- Change rule severities in
  `autoloop/autoloop/sandbox/anti_hardcode_check.py` `_RULES` registry:
  - Q1.contains_or_matches_literal: FAIL (keep — surface == intent)
  - Q1.enumerated_or_keywords: **FAIL → FLAG_FOR_CODEX**
  - Q1.if_then_decision_tree: **FAIL → FLAG_FOR_CODEX**
  - Q2.must_always_against_soft_dimension: **FAIL → FLAG_FOR_CODEX**
  - Q2.tier0_invariant_invention_attempt: FAIL (keep)
  - Q4.case_id_literal: FAIL (keep)
  - Q4.id_assignment_literal: FAIL (keep)
  - Q5.bot_must_always: **FAIL → FLAG_FOR_CODEX**
  - Q5.do_not_consider_soft_dimension: **FAIL → FLAG_FOR_CODEX**
  - Q5.force_assistant_to: **FAIL → FLAG_FOR_CODEX**
  - Q5.standalone_must_borderline: FLAG_FOR_CODEX (already)
- Add `anti_hardcode.severity_overrides:` config map so the demote is
  reversible per-experiment without code change.
- Re-baseline `tests/test_real_meta_agent_calibration.py` 17-fixture
  suite (current 11 FAIL / 4 PASS / 2 FLAG → after demote ~2 FAIL / 4
  PASS / 11 FLAG; the FAIL→FLAG cases are well-known and should be
  reviewed individually as part of the patch).
- Update `tests/test_anti_hardcode_check.py` — about 8 tests change
  their expected verdict; 4-5 new tests added to cover the new
  configurable severity path.

**Pros**:
- Smallest code change.
- Restores semantic-gate ownership to Codex (where the Kernel already
  lives).
- Preserves Q4.* as cheap structural defense against raw eval phrase
  leakage (the only ground-truth syntactic anti-hardcode signal).
- Reversible via config — if Codex review at S-Y2 close shows
  FLAG_FOR_CODEX patterns are slipping through unreviewed, the
  override map can re-promote any subset to FAIL without a code
  change.
- No prompt changes, no gate logic changes, no re-bless.
- Compatible with the 2026-06-09 sibling doc (baseline-whitelist
  scope fix) — they address orthogonal axes.

**Cons**:
- More candidates reach eval → autoloop iteration time increases by
  ~the eval cost of false-positive-formerly-FAIL candidates (~90
  min/iter for real-LLM eval). Mitigated because most demoted
  patterns are syntactically rare in candidates (the proposer is
  steered by `propose.txt`'s own §1.7 mapping toward narrative form).
- A genuinely hardcode-y candidate might pass eval (because the gate
  is about regression, not §1.7) and then be reviewed by Codex. If
  Codex review is skipped or perfunctory, hardcode could slip
  through. **Mitigation**: §4.3 makes per-sub-sprint Codex REQUIRED
  for trigger-#2 sub-sprints (S-Y2 already in this regime); deliver-
  agent's close-out checklist must surface FLAG_FOR_CODEX patterns
  to Codex (proposal §11 observability).
- Loop reads "FAIL" as discardable evidence today; FLAG_FOR_CODEX
  history shows lower-frequency patterns (current 17-fixture is
  2/17 FLAG). The flag stream becomes meaningfully larger after
  demote and may need an audit-output redesign (manageable; §11).

### Option B — Add few-shot whitelist to Q1.if_then / Q1.or_keywords

**Scope**:

- Extend the Q1 regex predicates to recognize illustrative-example
  context windows (`(e.g., …)`, `for example, …`, `such as "…"`,
  `including "…", "…", and "…"`) and skip Q1 matches inside those
  windows.
- ~40 LOC of regex + predicate logic, ~6 new tests.

**Pros**:
- Narrower change. Keeps Q1.* as FAIL for "real" if-then.
- Targeted fix for the confirm.yaml pattern (`(e.g., 'thanks', …)`)
  surface.

**Cons**:
- **Doesn't address the deeper mismatch**. Even with whitelist, the
  detector still false-fires on `resolve_faq.procedure`'s
  arrow-notation sequence narrative (which is not a few-shot
  example).
- Whitelist creates a new bypass surface: candidates that gaming the
  detector by wrapping if-then dumps in `(e.g., …)` framing would
  pass while still being rule dumps semantically.
- Adds ad-hoc complexity to a layer whose principled answer is
  "defer to semantic review."
- Less reversible than Option A.

### Option C — Replace detector with on-demand Codex call at propose time

**Scope**:

- Add `meta_agent/anti_hardcode_codex_review.py`: at propose time,
  send `(before_value, after_value, target_skill_file,
  target_field_path)` to a Codex API call with the 9-question Kernel
  as system prompt. Use Codex's verdict directly.
- Remove the regex detector entirely (or keep as `FLAG_FOR_CODEX`
  fallback when Codex is unavailable).

**Pros**:
- Principled. The semantic gate is literally the semantic gate.
- Eliminates the syntactic/semantic mismatch.

**Cons**:
- Adds an LLM API call per iteration (~$0.01-0.03; ~5-15s latency).
  Over the n=8 tranche that's ~$0.10-0.25 and ~40-120s. Tolerable
  but real.
- Codex API availability becomes a loop liveness dependency.
- v2-grade change: requires new code surface, prompt engineering,
  Codex-availability fallback, and a calibration sub-sprint to
  confirm Codex verdict stability.
- Defer to M-Auto-8+.

### Option D — Status quo

Keep Q1.* / Q2.must_always / Q5.* as FAIL. Accept that S-Y2 burns
two on-target candidates (exp-70 / exp-76) per false-positive class.
Accept that production prose has been calibrated to AVOID specific
syntactic forms rather than to encode legitimate teaching.

**Rejected** because:
- Codifies a writing-style restriction (don't use "then" / "→" / "or
  X or Y or Z") that has no constitutional basis.
- Preserves the brittle proxy that makes Q1's verdict
  surface-form-dependent (see §3.4 confirm.yaml comma-vs-or example).
- Future targeted pilots on `resolve_faq.procedure` or similar
  surfaces will continue to hit the same false-positive class.

## 6. Recommended option + rationale

**Recommended: Option A** (demote Q1.* / Q2.must_always / Q5.* to
FLAG_FOR_CODEX; keep Q4.* and Q2.tier0_invention as FAIL).

1. **Smallest principled change**: ~30 LOC + config knob + test
   updates. No prompt, no gate, no re-bless.
2. **Constitutionally correct**: restores semantic-gate ownership to
   Codex review (where the Kernel already lives at §4.1). The
   detector becomes "cheap structural defense against ground-truth
   syntactic offenses" (Q4 raw eval phrase, Q2 tier-0 minting) +
   "diagnostic signal for borderline patterns" (the demoted set).
3. **Reversible**: config-driven severity overrides let any subset of
   demoted rules be re-promoted to FAIL without a code change. If a
   M-Auto-8+ review shows FLAG_FOR_CODEX is too lax for some
   pattern, the override flips.
4. **Compatible with the 2026-06-09 sibling doc**: that doc fixes
   detector SCOPE (full after_value → diff-aware baseline-whitelist);
   this doc fixes detector SEVERITY (FAIL on semantic patterns →
   FLAG_FOR_CODEX). Both can land independently; landing both gives
   the cleanest end-state.
5. **Respects §4.3 per-sub-sprint Codex requirement**: for trigger-#2
   sub-sprints (autoloop output as §1.7 binding gate), Codex review
   is mandatory. The flag stream FROM Q1/Q2/Q5 patterns flows into
   that review naturally.

**Why not B**: too narrow + creates a new bypass surface. The few-shot
whitelist treats one symptom (`(e.g., …)` framing) and leaves the
deeper mismatch in place.

**Why not C**: principled but v2-grade. The cost-of-Codex-call is
manageable but the new code surface + Codex availability + fallback
design + calibration sub-sprint takes this out of in-flight scope.
Reasonable post-M-Auto-7 milestone candidate.

**Why not D**: the cost of doing nothing keeps growing — every
targeted pilot on a surface with baseline arrow notation re-hits the
same wall, and the proposer's writing style is being implicitly
shaped by detector regex shape rather than by §1.7 principle.

## 7. Scope split + delivery priority

### S-Y1.5c (micro patch, ~0.5-1 d, **deliver-agent decision**)

**§3.2 Layer**: `infra` (autoloop sandbox; eval-framework-adjacent).

**§7 stanza**: REQUIRED — semantic-touching at the rule-severity level
(touches what counts as §1.7 violation in the propose-stage filter).
See §8 stanza draft.

**Scope (3 items)**:

1. `_RULES` registry severity changes per §5 Option A list. Add
   `anti_hardcode.severity_overrides: dict[rule_id, "FAIL" | "FLAG_FOR_CODEX"]`
   config schema (default empty; if a rule_id appears, override the
   registry default for that rule only).
2. Re-baseline 17-fixture calibration suite + update
   `tests/test_anti_hardcode_check.py` expectations.
3. Update `propose.txt` "# Forbidden patterns (§1.7 Constitution —
   Forbidden)" section to clarify the LEGITIMATE shapes — add one
   sentence: *"Few-shot illustrative examples and principle-level
   narrative with quoted user-utterance shapes (e.g. `(e.g., 'X',
   'Y')`) ARE acceptable when they teach a class of behavior and the
   LLM judges applicability. The detector's PASS/FAIL/FLAG verdict is
   not authoritative for these; Codex §4.1 review is."*

**Test/eval requirements**:

- `tests/test_anti_hardcode_check.py` — re-baseline existing expected
  verdicts; add tests for `severity_overrides` config knob (Q1 stays
  FAIL when override sets Q1 to FAIL; Q4 stays FAIL by default).
- `tests/test_real_meta_agent_calibration.py` — 17-fixture suite
  re-baseline. Each FAIL→FLAG change must be reviewed individually
  in the test file's commit message (i.e., "this Q1.if_then pattern
  is legitimate principle-level guidance per Kernel Q6").
- `tests/test_loop_anti_hardcode_wire.py` — ensure FLAG_FOR_CODEX
  paths still attach the flag to iteration record (existing
  behavior; just more rules now produce FLAG).
- No re-bless.
- No §5.6 bad-case rerun (sandbox is pre-eval; no agent semantic
  surface touched).

**Per-sub-sprint Codex (§4.3)**: REQUIRED — this change shifts what
the autoloop ENFORCES at propose-stage. Codex focus questions:

1. Is the rule-severity demote scope correct (Q4 stays FAIL — the
   raw-eval-phrase defense; Q2.tier0_invention stays FAIL; everything
   else FLAG_FOR_CODEX)?
2. Does the 17-fixture re-baseline reflect a reasonable judgment for
   each FAIL→FLAG case, OR did the patch demote a rule that genuinely
   needed FAIL severity? (Codex reads each re-baselined fixture's
   verdict change and judges per Kernel Q3/Q6.)
3. Is the `propose.txt` few-shot clarification compatible with
   §1.7 + Kernel Q3/Q6? (Codex confirms it teaches the proposer
   correctly without opening a §1.7 bypass.)
4. Does the sprint close-out process for trigger-#2 sub-sprints
   actually surface FLAG_FOR_CODEX patterns to Codex review? If not,
   the demote risks letting hardcode slip past the §4.1 gate.

### Optional concurrent: land the 2026-06-09 sibling doc

The 2026-06-09 Q1 baseline-whitelist scope fix is orthogonal and
complementary. Landing both in the same patch is recommended:

- Scope fix (2026-06-09): detector reads `before_value` and suppresses
  matches already in baseline.
- Severity fix (this doc): demote semantic-judgment rules from FAIL to
  FLAG_FOR_CODEX.

After both: Q1.if_then on resolve_faq.procedure produces neither FAIL
(baseline-suppressed) nor noise (would only flag NEW patterns). And
NEW patterns that do trigger Q1 are FLAG_FOR_CODEX, not FAIL.

## 8. §7 Layer-classification + anti-hardcode stanza

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (autoloop sandbox detector rule
severity calibration; eval-framework-adjacent — §5.8 framework-defect
priority precedent applies in spirit).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. The
demote does NOT change what §1.7 forbids; it only changes how the
autoloop sandbox's propose-stage filter expresses its uncertainty
about borderline semantic patterns (FLAG_FOR_CODEX = "Codex must
judge" vs FAIL = "definitely a hardcode"). The semantic gate moves
back to per-sub-sprint Codex review (§4.3), which is where it lived
constitutionally before the detector's S-Auto-4 over-tightening.

**Semantic hardcode:** No semantic hardcode introduced. This patch is
the OPPOSITE — it removes false-positive hardcode rejections that
were blocking legitimate principle-level narrative + few-shot
illustrative teaching. The structural defenses retained:
- Q4.case_id_literal stays FAIL (§1.7 "encoding raw eval phrases").
- Q4.id_assignment_literal stays FAIL.
- Q2.tier0_invariant_invention_attempt stays FAIL.
- The shadow firewall is unaffected.
- The mutable surface YAML-diff sandbox is unaffected.
- The 5-layer lexicographic fitness gate is unaffected.
- Per-sub-sprint Codex review (§4.3) becomes the semantic-judgment
  binding gate, which is what §4.1 Kernel already specifies.

Reversibility: `anti_hardcode.severity_overrides` config map lets
any demoted rule be re-promoted to FAIL per-experiment without code
change. If post-S-Y1.5c Codex review at S-Y2 close shows a specific
FLAG_FOR_CODEX pattern is leaking real hardcode through, the
override flips that rule back to FAIL.

**Generalization coverage:** target / neighbor / negative / shadow
case counts: n/a — pure-infra sub-sprint touching only
`autoloop/autoloop/sandbox/anti_hardcode_check.py` +
`autoloop/autoloop/meta_agent/prompts/propose.txt` (one-sentence
clarification) + `autoloop/config.yaml` (severity_overrides schema) +
`autoloop/tests/test_anti_hardcode_check.py` +
`autoloop/tests/test_real_meta_agent_calibration.py`. No agent
semantic surface touched. Validation = unit tests + 17-fixture
re-baseline + S-Y1.5c-close mini smoke run on dry-run propose
generation (assert that the previous exp-70 / exp-76 candidate-shape
no longer produces FAIL).
```

## 9. Hard fences + non-goals

**S-Y1.5c hard fences**:

- **No edits** to `docs/current/iteration_governance.md` §1.7 (the
  constitutional principle is unchanged).
- **No edits** to `docs/current/anti-hardcode-review-kernel.md` (the
  9-question Codex review prompt is unchanged).
- **No edits** to `autoloop/autoloop/scoring/*.py` (no scoring code
  drift).
- **No edits** to `autoloop/autoloop/sandbox/yaml_diff_validator.py`,
  `applier.py`, `content_validator.py` (only `anti_hardcode_check.py`).
- **No edits** to `autoloop/program.md` (locked).
- **No edits** to the mutable surface, the 5-layer gate, the shadow
  firewall, the pilot_snapshot block, the lessons system, or the
  4-layer hit-rate metric.
- **No edits** to `eval_interactive/**` / `server/**` /
  `docs/foundational/**` / `docs/sprints/*` / `docs/milestones/*`.
- **Q4.case_id_literal MUST remain FAIL severity** (the only
  surface-form-matches-semantic-intent rule; the literal §1.7
  "encoding raw eval phrases" defense).
- **Q2.tier0_invariant_invention_attempt MUST remain FAIL severity**
  (Tier-0 minting requires explicit governance authorization per
  §3.2; surface form is unambiguous).
- **Per-sub-sprint Codex review for trigger-#2 sub-sprints MUST
  continue to consume FLAG_FOR_CODEX patterns from the iteration
  record** — the deliver-agent's close-out checklist must explicitly
  surface flags to Codex. If this surfacing is missing, the demote
  becomes a §1.7 bypass.

**Non-goals**:

- **Not changing what §1.7 forbids**. The constitutional principle is
  unchanged; the demote only changes how the autoloop sandbox
  expresses uncertainty about borderline patterns.
- **Not replacing the regex detector with a Codex call** (that's
  Option C; v2-grade; defer to M-Auto-8+).
- **Not introducing a few-shot whitelist** (that's Option B; rejected
  in favor of A).
- **Not removing the detector entirely**. Q4.* + Q2.tier0_invention
  retain real value at the propose-stage cheap-filter level.
- **Not modifying the Kernel 9-question review**.
- **Not changing the per-sub-sprint Codex review trigger conditions**
  (§4.3 unchanged).
- **Not addressing the gate-correct task-hardness discards from the
  S-Y2 tranche** (exp-69 / 71 / 72 / 75 etc.; those are anticipated
  per 2026-06-08 §5.5 / §11.1 and §3.4 hand-authored fallback path).
- **Not opening hill-climbing memory** (v2-grade).

## 10. Risk + compounding-effect analysis

### 10.1 Implementation risks

| Risk | Severity | Mitigation |
|---|---|---|
| Codex per-sub-sprint review at S-Y2 close doesn't surface FLAG_FOR_CODEX patterns → hardcode slips through | **H** | deliver-agent close-out checklist explicitly lists FLAG_FOR_CODEX iterations for Codex; §4.3 already requires Codex consume the §1.7 binding gate; reproducible test in `tests/test_loop_anti_hardcode_wire.py` asserting flag attaches to record |
| 17-fixture re-baseline is wrong — some demoted FAIL fixtures are genuinely §1.7 hardcode that the regex correctly caught | M | per-fixture commit-message review; Codex per-sub-sprint focus question #2; config-override fallback to re-promote any specific rule |
| Loop runtime grows because more candidates reach eval | L | quantitative cost: at S-Y2's ~90 min/iter × ~25% of iterations being formerly-FAIL Q1.* shape = ~22 min/iter additional in worst case. Acceptable for the calibration restoration |
| Proposer LLM starts generating MORE if-then patterns because the FAIL-pressure is removed → eval gate burns | M | `propose.txt` clarification sentence keeps the §1.7 norm visible at propose time; gate-correct discards remain the structural defense; if pattern surfaces, severity_overrides can re-promote |
| Q4 false-positive on legitimate prose containing `cs_xxx` as a non-eval token (e.g. `cs` as a literature abbreviation in CS-domain prose) | L | Q4 already uses word-boundary matching + the test suite includes a smoke test for this edge case; if it surfaces, Q4 can also be re-baselined |
| Config schema change (`anti_hardcode.severity_overrides`) breaks existing per-experiment configs | L | new field is optional + empty-default; existing configs are byte-compatible |

### 10.2 Non-implementation risks (not patching)

| Risk | Severity |
|---|---|
| Future targeted pilots on `resolve_faq.procedure` / similar arrow-notation surfaces continue to lose candidates to false-positive Q1 | **M** — recurs every pilot on such surface; ~2 false-positives in S-Y2 n=8 alone |
| Proposer writing-style continues to be implicitly shaped by detector regex (avoid "then" / "→" / " or " separators) rather than §1.7 principle, creating a divergence between candidate prose and production-blessed prose | M — already happening; production uses arrows + parens-with-commas while candidates that use same forms are killed |
| New autoloop contributors mis-read the rule machinery: "the autoloop detector FAILs on if-then, so if-then is the §1.7 line" → over-restrictive review culture | L (cultural) |
| §1.7 "encoding raw eval phrases" defense (Q4) becomes the ONLY surviving detector rule — perceived weakening even though Q4 is the only rule that's semantically grounded | L (cosmetic — Codex review remains the binding gate) |

### 10.3 Compounding effects

- **Land 2026-06-09 sibling doc first OR together**: that doc's
  baseline-whitelist scope fix is necessary to make the demote
  behave correctly on resolve_faq.procedure. Without scope fix,
  demoting Q1.arrow_tree to FLAG_FOR_CODEX still surfaces a flag on
  the baseline text → noisy Codex review queue.
  Recommended landing order: **scope fix (2026-06-09) → severity fix
  (this doc)**.
- **deliver-agent close-out checklist update is REQUIRED**: the
  FLAG_FOR_CODEX patterns need explicit surfacing to Codex review at
  per-sub-sprint / milestone close. Without this, the demote risks
  letting hardcode slip past §4.1. Add a one-line check to the close
  template: "Iterations with `anti_hardcode_flag_for_codex=True`
  reviewed by Codex per Kernel Q3/Q6 — verdict recorded in §4.2
  header."
- **propose.txt clarification + detector demote are co-required**: if
  the detector demotes but `propose.txt` still implies that any
  if-then triggers FAIL, the proposer will avoid teaching shapes that
  ARE legitimate. The clarification sentence keeps the proposer's
  prior accurate.
- **17-fixture re-baseline is a forensic record**: each fixture's
  verdict change should be commit-message-justified per Kernel Q3/Q6.
  Don't bulk-rewrite without per-fixture review.

## 11. Observability / report implications

- **`autoloop audit --experiment exp-N`** — add a per-rule
  `severity_used` field next to the verdict so reviewers can see
  whether a flag came from a "default severity" or an "override
  severity." This makes post-hoc audit of demote correctness easy.
- **deliver-agent close-out checklist** — add a section: "FLAG_FOR_CODEX
  iterations" listing each iteration where `anti_hardcode_flag_for_codex
  = True` + the rule_id + the matched_substring (post-scope-fix:
  guaranteed to be a NEW pattern in the candidate, not baseline). Codex
  reviews these against Kernel Q3/Q6 at sprint close.
- **`docs/codex-findings.md` schema** — no schema change. The §4.2
  header already covers the per-PR verdict surface; the FLAG_FOR_CODEX
  iterations are reviewed individually within the per-sub-sprint Codex
  pass that §4.3 already requires.
- **17-fixture calibration test as live signal** — the suite outcome
  (currently 11/4/2) becomes a CALIBRATION INVARIANT: re-baselined to
  ~2/4/11 at S-Y1.5c close. Future detector edits MUST not change this
  outcome without an explicit calibration sub-sprint. The test asserts
  the calibrated outcome.
- **`runs/<exp-N>/anti_hardcode_verdict.json`** — schema unchanged;
  new field `severity_overrides_applied: list[rule_id]` records which
  overrides were active (forensic; helps audit reproduce the verdict).
- **(Optional, non-blocking) Codex-review metric**: post-S-Y1.5c, track
  the FLAG_FOR_CODEX → Codex-verdict (`approve` vs `reject as semantic
  hardcode`) distribution. If Codex `reject` rate on FLAG_FOR_CODEX
  patterns is high (> 30%), that's a signal the detector is correctly
  flagging real hardcode at high precision — argument for keeping the
  detector. If reject rate is low (< 10%), detector is mostly
  noise — argument for moving toward Option C in a future milestone.

## 12. Sources of truth used in this RCA

- `docs/current/iteration_governance.md` §1.7 (verbatim quoted in §3.2)
  + §1.3 (LLM-owned soft dimensions) + §3.2 (Tier-0 minting
  authorization) + §4.1 (Kernel anchor) + §4.3 (per-sub-sprint Codex
  trigger conditions).
- `docs/current/anti-hardcode-review-kernel.md` (full 9-question
  Kernel; Q3 + Q6 verbatim quoted in §3.3).
- `autoloop/autoloop/sandbox/anti_hardcode_check.py` —
  - lines 69-95 (`_SYNONYM_MAP`, `_RE_WHEN_WORD_BOUNDARY`, `_normalize`)
  - lines 128-167 (Q1 regex constants + `_q1_*` helpers)
  - lines 169-265 (Q2/Q4/Q5 regex constants + helpers)
  - lines 290-322 (`_RULES` registry — the file the demote edits)
  - lines 336-360 (`anti_hardcode_check` entry point)
  - confirmed presence of `_first_new_match` + `before_norm` =
    2026-06-09 scope fix is already landed (commit indicates
    "S-Auto-35").
- `autoloop/autoloop/loop.py` lines 226-238 (the discard-on-FAIL +
  continue-on-FLAG_FOR_CODEX call site).
- `autoloop/autoloop/meta_agent/prompts/propose.txt` lines 36-49
  (current "Forbidden patterns (§1.7 Constitution — Forbidden)"
  section; the patch adds one clarifying sentence).
- `autoloop/config.yaml` lines 349-376 (existing `anti_hardcode:`
  config block; the patch adds `severity_overrides:` sub-key).
- Production skill YAMLs (all 6 files) — programmatic scan + manual
  reading of `discover_triage.yaml`, `resolve_faq_grounded_answer.yaml`,
  `resolve_intake_collect_and_handover.yaml`, `confirm.yaml`,
  `escalate.yaml`, `terminal.yaml`. The §3.4 audit table is
  reproducible via the scan script.
- `docs/sprint_objective.md` §Codex review plan (per-sub-sprint Codex
  REQUIRED for S-Y2 — trigger-#2 binding gate).
- `docs/solutions/2026-06-09-anti-hardcode-q1-pure-additive-false-positive.md`
  (the sibling SCOPE fix; complementary not superseded).
- `docs/solutions/2026-06-08-autoloop-mechanism-audit-and-feedback-loop-tightening.md`
  (the implemented substrate proposal; unrelated to this doc but
  cited as governance precedent for infra-layer §1.7-protecting
  changes).

## 13. Open questions for deliver-agent

1. **Land as S-Y1.5c standalone, OR bundle with 2026-06-09 sibling
   doc as S-Y1.5b/c combo?** Research agent recommends bundle — both
   are tiny patches to the same file (`anti_hardcode_check.py`), share
   the same test surface (`test_anti_hardcode_check.py` +
   `test_real_meta_agent_calibration.py`), and have a natural landing
   order (scope fix → severity fix). Deliver-agent may prefer to
   land scope fix first + ship S-Y2 close + open severity fix
   post-M-Auto-7.
2. **17-fixture re-baseline review depth — bulk vs per-fixture?**
   Research agent recommends per-fixture (each FAIL→FLAG case
   reviewed against Kernel Q3/Q6 in commit message). Deliver-agent
   may prefer bulk if time-pressured for S-Y2 close.
3. **Codex per-sub-sprint close-out checklist update — is the
   "FLAG_FOR_CODEX iterations" section a hard requirement or a soft
   guideline?** Research agent recommends hard — required field in
   the close template — because without it the demote silently
   becomes a §1.7 bypass. Deliver-agent's call.
4. **Severity override config schema location — `anti_hardcode.severity_overrides`
   vs a dedicated top-level block?** Research agent recommends nested
   (less surface, reads as a refinement of existing
   `anti_hardcode:` block). Deliver-agent may prefer top-level if
   visibility matters more than nesting.
5. **Does the propose.txt clarification need calibration evidence
   before merge?** Research agent recommends running a 1-2 iteration
   dry-run with the clarification to confirm the proposer doesn't
   over-correct and start generating actual hardcode. Deliver-agent
   may prefer ship-and-watch.
6. **Post-merge: should the metric "Codex reject rate on FLAG_FOR_CODEX
   patterns" be tracked as a §5.X gate condition** (e.g., "if Codex
   rejects > 30% of FLAG_FOR_CODEX patterns, the demote is too
   aggressive; reconsider rule severity at next milestone")?
   Research agent recommends as an observability follow-up R-item,
   not a §5.X gate. Deliver-agent's call.

---

End of proposal (Path-1 forward-looking). Awaiting human +
deliver-agent decision on whether to bundle with the 2026-06-09
sibling scope fix, and on the open questions above. Research
agent's recommendation: **Option A** (demote Q1.* / Q2.must_always
/ Q5.* to FLAG_FOR_CODEX; keep Q4.* and Q2.tier0_invention as
FAIL; add `anti_hardcode.severity_overrides` reversibility config).
No code changes have been made by the research agent.
