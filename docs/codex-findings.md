## Sub-sprint Review Decision — S-Auto-39 (Sprint 093, RESOLVE→CONFIRM/CLOSE deadlock corrective)
decision: pass
blocking_count: 0
summary: Commit 033abaee implements the RESOLVE→CONFIRM repair as a structural `skill_state` transition keyed on prior persisted turn evidence (`phase_after == RESOLVE` plus non-empty `source_ids`) and the existing `ANSWERED_SUBTASK` disposition; it does not inspect the new user message, add per-case/runtime UC exceptions, expand enums, weaken `shouldRejectPrematureResolveOutcome`, or lower `record_outcome`. The Sprint 9 §O1 retry exclusion is narrowly limited to premature-guard-only `record_outcome` rejection with prior grounding, while genuine record failures, slot-request turns, first grounded answers, and explicit escalation/distress paths remain outside the promotion.

Reviewed: runtime diff `033abaee` (`PhaseEvaluator.java`, `ControlKernel.java`, `BotSession.java`) + `Sprint93ResolveConfirmDeadlockTest.java`, against the Step-0 attribution (`docs/diagnostics/sprint-093-step0-escalation-attribution.md`), the §4 bounded real-LLM run, and the §5 byte-identity cross-check (`docs/sprints/sprint-093-handoff.md`). Read-only `codex exec` (model_reasoning_effort=high); verdict recorded verbatim below.

Kernel verdict: `approve`.

Per-question (§4.1 nine-question kernel):
- Q1 Pass: no new keyword/regex/per-UC semantic decision; the new branch is structural phase/history state plus existing disposition.
- Q2 N/A: no Tier-0 exception is needed; the change preserves the premature guard and trace-contract intent.
- Q3 Pass: projecting a soft signal would not repair the Java phase-machine deadlock; this belongs in `skill_state`.
- Q4 Pass: no visible eval phrase, CaseSpec id, or trace-specific text is encoded into runtime.
- Q5 Pass: LLM ownership is not shrunk; Java only permits a confirmable phase after a prior grounded RESOLVE answer.
- Q6 Pass: no prompt if-else or prompt change is introduced.
- Q7 Pass: tool schema, grounding floor, and premature guard are preserved; genuine `record_outcome` failures still retry.
- Q8 Pass: tests cover target deadlock, first-answer negative, DISCOVER-search negative, slot-request anti-误杀, genuine failure retry, guard truth table, and handoff reports neighbor/genuine-escalation validation.
- Q9 Pass: not framed as temporary; no rollback/sunset required.

Note (non-blocking, recorded for the human/deliver): the dev handoff §5 surfaces OQ-S93.1 — the structural deadlock is repaired (CONFIRM reachable) but `record_outcome(resolve)` lands in 0/42 bounded-run draws (resolved reached only via the grounding terminal stamp), with a downstream CONFIRM-phase record-vs-handover issue and a `user_requested` reason-mislabel. This is a follow-up scope item, not a defect in this diff. Pilot stays HELD.

---

## Sub-sprint Review Decision — S-Auto-38 (Sprint 092, escalation_compliance tier-0 split)
decision: pass
blocking_count: 0
summary: Kernel verdict `approve`: WP-A removes the implicit eval-internal escalation-reason-family-to-tier-0 binding while preserving the deterministic escalation behaviour floor, and WP-B adds a narrow, explicit, human-reviewable override schema without adding active registry entries or changing runtime/tool surfaces.

Reviewed: WP-A `57cd93c5` (gate split) + WP-B `8ed65cc6` (override schema), against the zero-LLM replay evidence (`analysis/out/replay_s_auto_38_output.txt`). Prompt: `compact/sprint-092-codex-review-prompt.md`. Read-only `codex exec` (model_reasoning_effort=high); verdict recorded verbatim below.

Kernel verdict: `approve`.

Justification: WP-A keeps `escalation_compliance` as Part-1 in `_TIER0_PY_FAMILY`, moves Part-2 to `escalation_reason_family_match` with advisory default severity, and the replay evidence shows 43/43 tier-0 flips are exactly `PART2_DEMOTION`, with 0 regressions and the high-risk should-escalate negative control still discarded. WP-B’s override path is scoped to eval scoring, requires `accepted_reasons` XOR `accepted_families`, restricts enforcement levels, and rejects tier-0 re-elevation unless approved, safety-critical, and cited. No prompt if-else, visible-eval phrase pin, runtime tool-schema change, PII-floor weakening, or grounding-floor weakening found.

Per-question concerns: none blocking. The only semantic enum/allow-list surface is the explicit eval override schema, not a runtime hardcode; currently there are zero approved escalation bindings in the active registry.

---

## Sub-sprint Review Decision — S-Y1.7
decision: pass
blocking_count: 0
summary: Commit `758503b6` implements an infra/eval-framework statistical gate, not a semantic runtime hardcode. The production gate branches on baseline pass-rate tier and posterior statistics, not keywords, UC enums, CaseSpec IDs, or visible eval text. Layer 0 is unchanged in the diff, exp-78’s anti-误杀 majority flip remains a discard, exp-66/72 are released by the configured statistical wiring rather than special cases, scope is `autoloop/**`, and the scoring SHA reproduces.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix for a semantic decision?
No semantic hardcode found. It adds statistical/eval-framework branching by measured pass-rate tier:
```diff
+    if pass_rate >= 0.999:
+        return TIER_S
+    if pass_rate <= 0.001:
+        return TIER_F
+    return TIER_N
```
The gate uses `(k,n)`, `P_regress`, BH-FDR, and count thresholds, not user text or UC-specific decision rules.

2. If yes to (1), is the change justified as protecting a current Tier-0 invariant?
N/A for semantic hardcode. It preserves the existing Tier-0 floor and adds the FS anti-误杀 floor as eval-gate protection, not as a new runtime Tier-0 invariant.

3. Could the same outcome be achieved by projecting a soft signal to the LLM instead of a hard branch in Java or the prompt?
N/A. This is not Java/prompt semantic routing; it is the autoloop survival gate. It also projects `classification=ambiguous` for non-discard knife edges rather than forcing every signal into a hard discard.

4. Does the change encode visible-eval case text, trace-specific phrasing, or a CaseSpec id into runtime, prompt, or judge config?
No executable gate logic does. Case IDs appear in oracle tests and data-driven reasons/metrics. Non-blocking note: production comments mention examples such as `wmkb`, but no branch consumes those strings:
```diff
+    A knife-edge increase (... exp-66's
+    wmkb at 0.797) gates nothing but marks the candidate AMBIGUOUS
```

5. Does the change move semantic ownership from the LLM to Java?
No. The LLM-owned surfaces in §1.3 are untouched. This changes scoring of fixed eval results and candidate survival, not runtime semantic planning.

6. Does the change add an if-else block to the prompt instead of principle-level or observable-state guidance?
No prompt changes.

7. Does the change preserve tool schema, capability / permission boundary, PII / safety floor, and grounding floor?
Yes. `server/**` and `eval_interactive/**` are untouched. Layer 0 has no body diff; the FS floor catches majority flips:
```diff
+            if (k / n) <= 0.5:
+                ctx.fs_hits.append(f"{cid}({k}/{n})")
```
`scoring_code_baseline_sha` also reproduces as `f2f983cc86e7d29e18d4659da3a317131eecd624df14a78ce26286f30d590d79`.

8. Does the PR ship generalization eval coverage — target, neighbor, negative, and shadow cases — and not only the target case?
Yes for this measurement-layer change: the oracle covers the 12 archived experiments across the named families, including tier0, anti-误杀 control, cross-case regression, C1, and C2:
```diff
+_DISCARDS = {67: ..., 78: (...), 69: (...)}
+_KEEPS = [66, 71, 72, 75, 77, 79]
```

9. If the change is temporary, does it carry an explicit rollback or sunset plan?
Not temporary. F5 knob confirmation is explicitly deferred to the first S-Y2 pilot run, but that is calibration follow-up, not a hardcode sunset.

Kernel Verdict: approve

R-S91.1: Keep the first S-Y2 pilot knob-confirmation gate from the handoff; the n=5 retrospective oracle certifies floors and wiring, not final statistical power.


## Sub-sprint Review Decision — S-Y1.5b
decision: pass
blocking_count: 0
summary: The prior standalone §4.1 `approve` is re-confirmed under the combined b+c lens. S-Y1.5c changes only registry severity, config override selection, proposer guidance, and tests; it does not change `_first_new_match`, `before_norm`, normalization, rule regexes, predicates, or suppression ordering. OQ-S90.1, OQ-S90.2, and OQ-S90.3 remain non-blocking and unchanged.

## Sub-sprint Review Decision — S-Y1.5c
decision: pass
blocking_count: 0
summary: R-S90.5 and R-S90.6 are resolved by S-Y1.5d attempt-2 (`708f8cb3`). Independent probes confirm ordinary `CSAT` / `csagent` / `css` / `csv` prose returns PASS while real numeric, underscore, shadow, and `csmp_*` CaseSpec ids remain default `FAIL Q4.case_id_literal`; the 452-id corpus scan reports 0 misses, Q4 severity remains `_FAIL`, and scoring/config/propose surfaces are unchanged.

## Sub-sprint Review Decision — S-Y1.5d
decision: pass
blocking_count: 0
summary: Targeted §4.3 re-review passes. The corrected `\bcs[a-z0-9_]*[0-9_][a-z0-9_]*\b` regex resolves the ordinary-word false positives without weakening the Q4 hard-discard: all requested real-id probes and all 452 corpus cs-prefixed CaseSpec ids fail Q4, with 0 corpus misses and 0 real CaseSpec-id literals in detector source. `Q4.case_id_literal` remains `_FAIL`; no other rule, normalization, override, propose, config, or scoring surface changed; the configured scoring SHA reproduces exactly.

## Blocking Finding

### R-S90.5 — S-Y1.5c's retained FAIL set is not limited to unambiguous §1.7 surfaces

**RESOLVED-by-S-Y1.5d-attempt-2:** R-S90.5 and R-S90.6 are resolved by commit `708f8cb3`. Independent verification confirms `CSAT`, `csagent`, `css`, and `csv` clean prose now PASS; numeric, underscore, shadow, and `csmp_*` CaseSpec ids still FAIL `Q4.case_id_literal`; the root-level Python `os.walk` scan found 452 distinct cs-prefixed CaseSpec ids with 0 regex misses; detector source contains 0 real corpus IDs; and the scoring baseline SHA remains `0d86b08fc0309196d9fc49f4924fe446b36d6c04301c4bcb406ca12337dd149e`. The historical R-S90.5/R-S90.6 finding text below is retained for the record.

```python
_RE_Q4_CASE_ID_TOKEN = re.compile(
    r"\bcs[0-9a-z_]{2,}\b",
    re.IGNORECASE,
)
...
("Q4.case_id_literal", _FAIL, _q4_case_id_token),
```

The regex at `autoloop/autoloop/sandbox/anti_hardcode_check.py:274` accepts any
word beginning with `cs` plus two alphanumeric/underscore characters. Independent
review probes returned:

```text
FAIL  Q4.case_id_literal  Use CSAT feedback as an aggregate quality signal.
FAIL  Q4.case_id_literal  The csagent should provide grounded answers.
```

`CSAT` is ordinary customer-service language and `csagent` is the project name;
neither is an eval CaseSpec id. This contradicts S-Y1.5c's load-bearing claim
that every remaining automatic FAIL has a surface form equal to constitutional
intent. The regex predates c, but c explicitly reviews and retains this rule as
one of only four supposedly unambiguous FAIL rules, so the defect is in scope for
the severity-calibration close.

This does **not** justify demoting actual Q4 ids: independent probes confirm
`cs011`, `cs_uc_a_no_ad_id`, `session_id=abc`, and `case_id: 42` all still
correctly return default FAIL, and focus question 3 hard-fences that behavior.
Required fix layer: `infra`. Human/deliver must decide the correction while
preserving default FAIL for actual Q4 identifiers.

### R-S90.6 — S-Y1.5d makes existing `csmp_*` CaseSpec ids PASS

Checklist item 2 fails. The new discriminator assumes all real CaseSpec ids
begin `cs<digit|_>`, but the current corpus already contains `cs<letter>...`
ids. For example:

```yaml
# eval_interactive/case_specs/case_families/manual_probe_uc_a_resolve_must/negative/csmp_g01_uc_a_genuine_faq_miss_obscure.yaml
case_id: csmp_g01_uc_a_genuine_faq_miss_obscure

# eval_interactive/case_specs_shadow/case_families/manual_probe_uc_a_resolve_must/csmp_s01_uc_a_search_hits_with_ad_id_mismatch.yaml
case_id: csmp_s01_uc_a_search_hits_with_ad_id_mismatch
```

Independent targeted probes against `40f8c07` returned:

```text
PASS  None  csmp_g01_uc_a_genuine_faq_miss_obscure
PASS  None  csmp_s01_uc_a_search_hits_with_ad_id_mismatch
```

These are real visible/shadow CaseSpec ids, not hypothetical future ids. The
delta therefore weakens the §1.7 Q4 hard-discard while fixing the false-positive
class. The new tests cover only `cs<digit|_>` ids, so focused `45 passed` and
full-suite `346 passed, 1 pre-existing warning` do not detect the regression.
Scoring remains untouched and the configured SHA still reproduces exactly; that
does not offset the Q4 false-negative. Required fix layer: `infra`.

## S-Y1.5b Combined-Lens Reconfirmation

- Reviewed half: commit `9c62a86` inside cumulative range
  `9c62a86^..75b302c -- autoloop/`.
- Prior per-PR verdict: `approve`.
- Combined-lens result: unchanged. The c-only detector diff does not touch
  `_first_new_match`, `before_norm`, `_normalize`, `_SYNONYM_MAP`,
  `_RE_WHEN_WORD_BOUNDARY`, any `_RE_*` rule body, or
  `_SOFT_SEMANTIC_DIMENSIONS`.
- S-Y1.5c changes the verdict of a new demoted-rule match from FAIL to FLAG, but
  it does not change whether b suppresses a baseline-present match. Baseline
  matches remain suppressed before severity is consulted.
- Per-PR §4.1 verdict: **`approve`**.

### Preserved Non-Blocking Observations

#### OQ-S90.1 — greedy trailing spans still false-positive on adjacent appends

The unchanged Q1/Q2/Q5 trailing captures can include newly appended context, so
the after-match span may no longer be a substring of `before_norm`. This remains
a non-blocking `infra` follow-up triggered by an observed recurrence. C does not
interact with the suppression calculation.

#### OQ-S90.2 — substring suppression admits duplicate/context-substring bypasses

```python
if before_norm and m.group(0) in before_norm:
    continue
```

The accepted v1 trade-off remains: a newly duplicated pattern, or a new match
whose text is merely contained inside baseline context, can be suppressed. The
complete candidate remains visible to human/Codex review. C changes only the
severity applied after an unsuppressed match is found.

#### OQ-S90.3 — no live Q6 detector rule

The registry still contains 11 Q1/Q2/Q4/Q5 rules and no Q6 rule. Suppression is
uniform across all registered rules; c adds no Q6 surface.

## S-Y1.5b §4.1 Nine-Question Kernel

1. **No.** B adds baseline-membership suppression and match iteration inside an
   infra governance detector; it adds no customer-semantic drift, escalation,
   UC-selection, follow-up, or routing decision.
2. **N/A.** No new semantic decision or Tier-0 invariant is added.
3. **No.** Projecting a signal to the customer-facing LLM would not correct a
   propose-stage detector false-positive. The candidate remains subject to the
   unchanged detector rules and human/Codex review.
4. **No.** Exp-70 text appears only in tests. No CaseSpec id or trace-specific
   phrase is added to detector rules or runtime.
5. **No.** No semantic ownership moves from the LLM to Java/runtime.
6. **No.** B changes no prompt.
7. **Yes, preserved.** Tool schema, capability/permission boundaries, PII/safety,
   grounding, runtime, and scoring surfaces are untouched.
8. **Yes, with the prior non-blocking limitation.** Coverage includes the real
   target pair, synthetic neighbor families, new-pattern negative controls, the
   accepted duplicate bypass, calibration, and full-suite regression. No shadow
   data is read.
9. **N/A / observed.** B is not marked temporary. OQ-S90.1 and OQ-S90.2 name the
   triggers for an `infra` follow-up.

### S-Y1.5b Kernel Verdict

`approve`

## S-Y1.5c Focused Review

### C1. Severity-demote scope

The final registry has the contracted 11 entries:

- Default FAIL: `Q1.contains_or_matches_literal`,
  `Q2.tier0_invariant_invention_attempt`, `Q4.case_id_literal`,
  `Q4.id_assignment_literal`.
- Demoted to FLAG: `Q1.enumerated_or_keywords`,
  `Q1.if_then_decision_tree` (including arrow form),
  `Q2.must_always_against_soft_dimension`, `Q5.bot_must_always`,
  `Q5.do_not_consider_soft_dimension`, `Q5.force_assistant_to`.
- Already FLAG: `Q5.standalone_must_borderline`.

The six demoted rule classes genuinely require semantic judgment:

- Q1 OR lists can be few-shot illustration or keyword routing.
- Q1 if/then and arrow forms can be principle/sequence narration or rule dumps.
- Q2 must/always checks a soft-dimension substring but cannot determine whether
  the surrounding statement protects a Runtime-owned floor.
- Q5 force/must/do-not-consider forms require the object and ownership boundary
  to determine whether they are forbidden.

The scope still fails overall because retained `Q4.case_id_literal` is broader
than actual Q4 identifiers; see R-S90.5.

### C2. Demoted-fixture per-fixture review

All 12 actual FAIL→FLAG assertion flips carry a Q3/Q6 rationale and remain
**caught as FLAG**, not silently changed to PASS:

- `Q1.if_then_decision_tree`: the basic if/then fixture, NFKC fixture,
  multi-line fixture, synonym-arrow fixture, Axis-B `Whenever =>` fixture,
  multi-line `When =>` fixture, and new-arrow suppression negative control.
- `Q1.enumerated_or_keywords`: the OR-keyword fixture.
- `Q2.must_always_against_soft_dimension`: the runtime-must fixture.
- Q5: force-assistant, bot-must-always, and do-not-consider fixtures.

The rationales are justified at the **rule-class** level: the included fixtures
are intentionally hard examples of one side of an ambiguous surface, while a
deterministic surface matcher cannot adjudicate all matches without semantic
context. `test_fail_carries_matched_substring` was re-pointed to a retained FAIL
fixture; it is not a thirteenth FAIL→FLAG flip. No unjustified demotion was
found.

### C3. Q4 default behavior

Default Q4 remains FAIL and overrides may intentionally demote it:

```text
cs011             -> FAIL / Q4.case_id_literal
cs_uc_a_no_ad_id  -> FAIL / Q4.case_id_literal
session_id=abc    -> FAIL / Q4.id_assignment_literal
case_id: 42       -> FAIL / Q4.id_assignment_literal
```

This passes the explicit default-unchanged hard fence, while R-S90.5 separately
blocks the claim that the Q4 case-id matcher is unambiguous.

### C4. `propose.txt` compatibility

The new section does not delete or weaken the preceding forbidden list. It
defines acceptable examples by two semantic conditions: they teach a class and
the LLM still judges applicability. Principle-level guidance is conditioned on
observable state; tool/enum literals are limited to Runtime-owned schema
legibility. It adds no prompt if-else block.

The distinction is not machine-verifiable. A probe shaped as a real routing rule
but framed as comma-separated “examples” returned PASS because it does not match
the OR-list regex. That candidate would still violate forbidden item 4 and must
be rejected by Codex if kept; see OQ-S90.6.

### C5. FLAG_FOR_CODEX surfacing

The unchanged end-to-end path is intact:

```python
result.anti_hardcode_verdict = ah_verdict
...
result.anti_hardcode_flag_for_codex = True
```

- `_build_record_dict()` persists both `anti_hardcode_verdict` and
  `anti_hardcode_flag_for_codex` to the normal `experiments.jsonl` row.
- Dry-run artifacts persist `runs/<id>/anti_hardcode_verdict.json` plus
  `dry_run_record.json`.
- `autoloop audit` reads the experiments row and surfaces
  `anti_hardcode_flag_for_codex`.
- `test_anti_hardcode_flag_for_codex_does_not_discard` verifies a FLAG continues
  and is persisted.

The demotion therefore does not silently erase matches.

### C6. Shadow firewall, scoring SHA, reversibility, and evidence

- Neither b nor c reads or routes shadow data; the detector reads only
  `hypothesis.before_value` and `hypothesis.after_value`.
- No file under `autoloop/autoloop/scoring/` changed.
- `_compute_scoring_code_sha()` independently reproduced
  `0d86b08fc0309196d9fc49f4924fe446b36d6c04301c4bcb406ca12337dd149e`.
- `test_severity_overrides_promote_demoted_rule_back_to_FAIL` sets
  `Q1.if_then_decision_tree: FAIL` and a new Q1 pattern returns FAIL.
- Invalid override values cannot disable a rule; only exact `FAIL` and
  `FLAG_FOR_CODEX` values are accepted.
- Exp-77 supports the handoff's PROCEED statement: it is a clean, on-target,
  principle-level dry-run snapshot and anti-hardcode PASSes. Exp-78 timed out
  before the detector and carries no detector evidence. This review does not
  re-judge the human/deliver verdict.
- Reviewer verification: focused detector/proposer/calibration/wire suite
  **51 passed**; full autoloop suite **344 passed, 1 pre-existing warning**.

### Non-Blocking C Observations

#### OQ-S90.6 — “few-shot” framing remains a semantic, not syntactic, boundary

A candidate such as an escalation rule written as comma-separated “examples”
can return PASS rather than FLAG. The new prompt still forbids keyword routing,
and final Codex review remains the binding semantic gate, so this is not a
second blocker. Trigger an `infra`-layer follow-up if a real PASS/keep candidate
uses few-shot framing to prescribe routing or action instead of teaching a
class.

#### OQ-S90.7 — retained FAIL rules still have meta-negation false positives

Reviewer probes showed that negated meta-guidance such as “Do not introduce a
new Tier-0 invariant” and “Avoid adding `message.contains(...)` checks” returns
FAIL. Those phrases are not useful Skill guidance and the behavior predates c,
so this remains non-blocking. Revisit in the `infra` layer if an actual
in-scope candidate is discarded on a negated anti-hardcode statement.

#### R-S90.4 — FLAG reject-rate observability remains appropriate

Track the Codex reject rate on FLAG_FOR_CODEX candidates at S-Y2 close. The
handoff's `>30%` trigger remains a reasonable signal that too much judgment was
moved downstream.

## S-Y1.5c §4.1 Nine-Question Kernel

1. **Concern.** C adds no customer-routing regex or per-UC matrix, but its central
   severity decision retains an overbroad regex-backed automatic classification:

   ```python
   r"\bcs[0-9a-z_]{2,}\b"
   ```

   Treating ordinary `CSAT`/`csagent` tokens as definite CaseSpec leakage is not
   a surface-equals-intent decision.
2. **No.** C adds no Tier-0 invariant. The retained
   `Q2.tier0_invariant_invention_attempt` is an enforcement rule, not a new
   runtime invariant.
3. **Yes, and mostly implemented correctly.** The six ambiguous rule classes are
   projected as FLAG_FOR_CODEX instead of hard-discarded. For actual Q4 ids, a
   soft signal is insufficient because raw eval-id leakage remains forbidden;
   the architecture question is how to preserve that hard fence without
   classifying ordinary `cs...` terms as ids.
4. **No new eval leakage.** C adds no visible-eval CaseSpec id or trace phrase to
   runtime, prompt, or judge config. Its prompt examples are generic. Q4 actual-id
   defaults remain FAIL.
5. **No.** C does not move customer-semantic ownership from the LLM to Java. It
   moves ambiguous anti-hardcode adjudication from a deterministic pre-eval
   discard to Codex/human review.
6. **No, with OQ-S90.6.** The prompt adds principle-level categories and
   observable-state guidance, not an if-else block. Few-shot framing can still
   conceal a rule dump and therefore depends on final semantic review.
7. **Yes, preserved.** Tool schema, capability/permission, PII/safety,
   grounding, mutable Skill surface, scoring, and shadow boundaries are
   unchanged.
8. **Not fully.** Target demotion, retained-Fail, override, prompt-presence,
   calibration, wire, and full-suite coverage pass. However, the retained Q4
   FAIL rule lacks ordinary `cs...` negative controls; the missing class is
   demonstrated by the `CSAT`/`csagent` probes and is the blocking gap.
9. **Partially.** `severity_overrides` is reversible for a whole rule and the
   Q1 re-promotion test passes. It cannot resolve R-S90.5 without also demoting
   actual Q4 ids, which conflicts with the default-Q4 hard fence; human/deliver
   must choose the `infra`-layer correction.

### S-Y1.5c Kernel Verdict

`needs human architecture decision`
