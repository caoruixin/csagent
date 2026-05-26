Paste the content below this line into a fresh Codex session after the Sprint 36 dev commit lands. No PR will be opened; review the commit directly.

---

You are the Anti-Hardcode + Sprint-Close Review Agent for Sprint 36 — the first sub-sprint of Milestone M2-Skill (Skill Foundation + UC-Switching Continuity, LLM-led Policy-bounded) per `docs/milestone_objective.md`.

Sprint 36 is a **design-freeze sub-sprint** (Sprint-30-shape; docs-only; NO `server/` code). The dev produced `docs/proposals/skill_foundation_design.md` — an architectural decision doc that locks the hybrid framing (envelope + recommended order via prompt; terminal predicate via Java guard for Runtime-owned floor ONLY) for the M2-Skill foundation. The substantive review surface is the **proposed design semantics in the design doc**, NOT the Sprint 36 diff (which is docs-only).

Per `iteration_governance.md` §4.3, Sprint 36 fires per-sub-sprint Codex review per trigger #1 (possible new Tier-0 candidate — the design doc §8 surfaces two: S1 citation predicate + S2 intake-completeness predicate; deliver-agent + human pre-decided DEFER for both — Codex independently verifies the DEFER rationale is sound) + trigger #2 (§1.7 boundary discussion on hybrid framing — Codex verifies the proposed design does NOT smuggle per-UC-branch if-else into skill bodies).

**Human authorization at M2-Skill approval round (2026-05-17) governs the S1 predicate scope** per `docs/milestone_objective.md` §6 #4 (verbatim quote):

> "Accept the Skill-bounded exception to D-hard-citation-gate. This is not a generic Java grounding-citation gate. It is a narrow S1 terminal predicate that only applies when the bot attempts `record_outcome(class=resolve)` inside the `RESOLVE_FAQ` scope, and only checks citation presence as the minimum grounding-floor condition. The original deferral of a generic Java citation gate remains valid."

The design doc §4 (D3) MUST reproduce this authorization verbatim. Codex verifies the design doc D3 predicate scope matches exactly: (a) Phase = `RESOLVE_FAQ`; (b) Tool call = `record_outcome` with `class=resolve`; (c) Check = `source_id` citation present. Any expansion (citation on `class=escalate`, content-quality judgement, fan-out beyond RESOLVE_FAQ) is a BLOCKING finding.

## 1. Loader

1. `AGENTS.md` (transitively loads `iteration_governance.md` §1 / §3 / §4 / §5 / §7 / §8 + `doc_governance.md` + `agent_context_guide.md`).
2. `docs/milestone_objective.md` — the M2-Skill milestone north star. Read carefully: §1 5-sub-sprint layer breakdown + per-sub-sprint Codex review default; §2 goal (S1 grounding-floor predicate is the Codex M1 Finding 1 fix at the deeper layer); §6 hard fences #1-#19 (esp. #4 HARD FENCE INVERSION with human authorization quote — load-bearing for verifying Sprint 36 design doc §4 D3 reproduces verbatim); §10 stop conditions (esp. #1 Tier-0 candidate escalation path).
3. `docs/sprint_objective.md` — the Sprint 36 contract. §1 sub-sprint class (design-freeze; multi-layer prospective §7 stanza); §2 goal (D1-D6 sub-decisions); §3 non-goals; §4 premise check (10 items; dev refined premise #8); §5 file table + §5.1 design doc structure (8 sections); §6 hard fences (19 items); §8 §7 stanza (Track A + Track B prospective); §10 stop conditions.
4. `docs/sprints/sprint-036-handoff.md` — the dev's archive. Compare §9 against §5 of the objective; verify no scope creep. §1.2 + §1.3 + §5.4 surface the MATERIAL FINDING that S1+S2 partial predicates already shipped Sprint 6 §G2 / Sprint 7 §I2 / Sprint 11 §M1; Codex independently verifies this finding against `AgentRunLoopImpl.java` (load-bearing for the design doc's D3/D4 framing). §3 premise #8 refinement (`UseCaseDefinition.requiredIntakeFields` → `IntakeFieldsRegistry.intakeComplete`); Codex verifies the correction is correct.
5. `docs/proposals/skill_foundation_design.md` — **the substantive review surface.** Read end-to-end. §1 Purpose + relation to upstream; §2 D1 envelope shape; §3 D2 predicate shape; **§4 D3 S1 trigger + predicate** (must reproduce human authorization verbatim per §6 #4 of milestone objective); §5 D4 S2 trigger + predicate (must cite `IntakeFieldsRegistry.intakeComplete` per premise #8 refinement); §6 D5 UC-switching wide continuity invariant matrix (must be principle-level + registry-level intersection check, NOT per-UC-pair branch table); §7 D6 §4.1 nine-question walk-through; §8 Open questions + Tier-0 candidate write-up.
6. `docs/proposals/skill_orchestration_candidates.md` — Sprint 5 (F2) original proposal. Verify frontmatter EDIT carries `superseded_by: docs/proposals/skill_foundation_design.md` + `status: superseded`; body unchanged.
7. `docs/milestones/M1_objective.md` §12 closure verdict + `docs/sprints/M1-codex-review.md` Finding 1 — context for S1 citation predicate's anchor (Alice trace fabrication shape).
8. `docs/current/iteration_governance.md` — §1.3 (LLM-owned: next action / response strategy — verify design doc D1 envelope is teaching, not enforcement); §1.4 (Runtime-owned: grounding floor / capability — verify design doc D3/D4 predicates fit within this floor); §1.7 (Forbidden — verify design doc has NO per-UC-branch if-else); §3.2 Q6 (eval_spec layer for design-freeze sub-sprint); §4.1 nine-question kernel; §4.3 per-sub-sprint Codex triggers; §5/§5.5/§5.6 acceptance bars; §7 sprint-objective stanza; §8 milestone framework.
9. `docs/runtime_freeze_and_risk_policy.md` §1 / §2 — read the current Tier-0 invariant set BEFORE evaluating the design doc §8 Tier-0 candidate write-ups. Verify the design doc §8.3 + §8.4 DEFER rationale is sound (not premature locking; not unjustified deferral).
10. `compact/sprint-036-dev-prompt.md` — what the dev was authorized to do vs what landed.
11. `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` — **READ for verification of dev's MATERIAL FINDING.** Verify Sprint 6 §G2 `shouldRejectFaqMissHandover` (~lines 690-734), Sprint 7 §I2 `shouldRejectIncompleteIntakeHandover` (~lines 628-651), Sprint 11 §M1 `shouldRejectPrematureResolveOutcome` (~lines 745-759). Confirm dev's reading is correct.
12. `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` — verify the premise #8 refinement (`requiredFieldsFor`, `intakeComplete` helpers; map `REQUIRED_FIELDS_BY_UC`); confirm design doc D4 §5.2 cites the correct source.
13. `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` — verify `UseCaseDefinition` record has 6 fields (per dev §3 premise #8) and does NOT carry `requiredIntakeFields`; the design doc + dev handoff correctly flag the original premise #8 as a class-name mis-citation.

## 2. Scope-discipline gate (BLOCKING)

Verify the dev commit touches ONLY the following surfaces:

- `docs/proposals/skill_foundation_design.md` (NEW)
- `docs/proposals/skill_orchestration_candidates.md` (frontmatter EDIT only — `superseded_by` + `status: superseded`; body unchanged)
- `docs/sprints/sprint-036-handoff.md` (NEW)

Any of the following in the commit is a BLOCKING scope violation:

- ANY file under `server/src/main/**` (Sprint 36 must NOT touch production code)
- ANY file under `server/src/test/**` (Sprint 36 must NOT touch Java tests)
- `server/src/main/resources/prompts/system_prompt.txt` (no prompt edit; teaching paragraphs land in Sprint 37/38/39)
- ANY file under `eval_interactive/` (no eval surface edit)
- ANY file under `docs/foundational/` (no foundational doc edit)
- `docs/current/iteration_governance.md` or `doc_governance.md` or `agent_context_guide.md` (governance docs out of scope per constitution-discipline)
- `docs/runtime_freeze_and_risk_policy.md` (no Tier-0 edit; candidate write-up lives in `skill_foundation_design.md` §8, NOT in the foundational doc)
- ANY file under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-035-*` (no archive edit)
- ANY file under `docs/milestones/` (M1 archive immutable)
- `docs/sprint_objective.md` (deliver-agent-owned)
- `docs/milestone_objective.md` (deliver-agent-owned)
- `docs/10-handoff.md` (deliver-agent-owned)
- `docs/action_bank.md` (deliver-agent-owned at close)
- `docs/codex-findings.md` (you own this file)
- `compact/sprint-036-*-prompt.md` (deliver-agent-owned)

ALSO verify the frontmatter EDIT on `docs/proposals/skill_orchestration_candidates.md` is APPEND-ONLY (added `superseded_by` + flipped `status`; body unchanged). Run `git diff <pre-commit>..HEAD -- docs/proposals/skill_orchestration_candidates.md` and verify only frontmatter lines changed; body untouched.

Surface any scope-discipline failure as Finding #1 with the diff snippet quoted.

## 3. §4.1 Anti-Hardcode kernel walk (against the PROPOSED DESIGN in skill_foundation_design.md, NOT against the Sprint 36 diff)

Sprint 36's diff itself is docs-only and qualifies for §4.1 exemption (the dev's handoff §8 correctly notes this). The SUBSTANTIVE review is on the design doc's proposed semantics for D1-D6, which the dev walked in design doc §7. Codex INDEPENDENTLY walks the same nine questions against D1-D6 and verifies the dev's verdict.

1. **Q1 keyword / regex / if-else / enum / per-UC matrix for semantic decision?** Walk D1 (envelope), D2 (predicate), D3 (S1), D4 (S2), D5 (UC-switching). For each: verify no proposed surface adds a per-UC-branch decision. D5 is the highest-risk surface (continuity invariants could be expressed as per-UC-pair table); verify D5 §6 / design doc uses registry-level intersection check (`IntakeFieldsRegistry.canonicalFieldName` + `requiredFieldsFor` intersection), NOT a per-UC-pair branch table. Expected: NO per-UC-branch if-else in any skill body.
2. **Q2 Tier-0 justification?** Verify design doc §8.3 + §8.4 Tier-0 candidate write-ups are honest + the DEFER rationale is sound. The S1 citation predicate is narrow + Skill-bounded; deferring Tier-0 elevation pending Sprint 37 trace evidence is appropriate. The S2 intake-completeness predicate has been in production since Sprint 7; adding Tier-0 status now adds zero architectural protection beyond what's shipped. Expected: DEFER is the right answer for both; no premature Tier-0 codification.
3. **Q3 soft signal achievable?** D5 IS a soft signal (`prior_use_case_carry` projection slot, per Sprint 31/33 precedent). D3/D4 predicates protect Runtime-owned floor (§1.4 ownership); soft-signal alternative is OUT OF SCOPE for terminal predicates. Verify the design doc D2 §3 names the boundary correctly.
4. **Q4 eval text / CaseSpec id encoding into runtime/prompt/judge?** Verify design doc does NOT encode any visible-eval CaseSpec id or trace phrasing into the freeze contract. Design doc references "the Alice failure shape" + "Codex M1 Finding 1" in PROSE (analytical reference); does NOT feed runtime/prompt/judge. PASS if no specific CaseSpec id (e.g., `alice_uc_a_uc_h_misclass`) is named as a hardcoded reference for the predicate.
5. **Q5 semantic ownership shift LLM → Java?** Verify design doc D1 envelope is TEACHING (LLM owns recommended-order deviation per §1.3); D2/D3/D4 predicates enforce Runtime-owned §1.4 floor ONLY. The LLM retains all §1.3 ownership (UC hypothesis, next action, escalation posture, response strategy, customer language, per-step argument choice).
6. **Q6 prompt as if-else dump?** Verify the proposed envelope teaching paragraphs (Sprint 37/38/39 will ship in `system_prompt.txt`) are principle-level adjacent to existing Sprint 23 / 31 / 33 precedents. Design doc D1 §2 must name the principle-level shape, not propose per-UC-branch text.
7. **Q7 tool / capability / PII / grounding floor preserved?** Verify D3 predicate is a NARROW grounding-floor extension per human authorization (cited verbatim in design doc §4). It is NOT a generic citation gate (per `D-hard-citation-gate` deferral preserved). D4 predicate is the existing capability-floor pattern (mirror §E1). PII unchanged; safety unchanged.
8. **Q8 generalization coverage?** Sprint 36 itself N/A (design-freeze). Sprints 37/38/39 prospective coverage in design doc Track B + handoff §6.2. Verify Track B names: target = Alice multi-trace closure-criterion (a); neighbor = other FAQ-path UCs / intake UCs; negative = `record_outcome(class=escalate)` traces / legitimate intake-complete-with-fields-populated traces; shadow = held-out FAQ + intake traces. Verify the prospective coverage is internally consistent.
9. **Q9 rollback / sunset?** D1-D5 are permanent architectural surfaces; no sunset needed. The design freeze itself is the contract; if M2-Skill close evidence shows the foundation is wrong, M3 fold-back addresses.

Expected verdict on the §4.1 kernel walked against the PROPOSED DESIGN: **approve** (with the two Tier-0 candidate questions surfaced for human-review escalation; deliver-agent + human pre-decided DEFER per OQ 7.1/7.2; Codex confirms DEFER is sound).

## 4. §1.7 boundary check on the proposed design (BLOCKING if a violation is found)

§1.7 forbidden list applies to the PROPOSED DESIGN, not the diff. Specifically verify:

- **D1 envelope** does NOT encode "raw eval phrases into Java or prompt" — the envelope text proposed for `system_prompt.txt` (Sprint 37/38/39 ship) MUST be principle-level (sibling to Sprint 23 `already_called` / Sprint 31 `alternate_candidate_use_cases` / Sprint 33 `discover_disambiguation_signals` teaching paragraphs).
- **D3 S1 predicate** does NOT use "the prompt as an if-else rule dump" — the predicate is in Java (`RecordOutcomeTool` dispatch site adjacent to `shouldRejectPrematureResolveOutcome` Sprint 11 §M1); the envelope teaching paragraph mentions the recommended sequence as guidance only, NOT as an enforcement rule list.
- **D3 S1 predicate scope** is NARROW per human authorization — (a) Phase = `RESOLVE_FAQ`; (b) Tool call = `record_outcome` with `class=resolve`; (c) Check = `source_id` citation present. ANY expansion in the design doc (citation on `escalate`, content-quality judgement, fan-out beyond RESOLVE_FAQ) is a BLOCKING §1.7 + governance violation. Verify the design doc D3 explicit out-of-scope enumeration matches the authorization verbatim.
- **D4 S2 predicate** does NOT widen `escalation_reason` enum (`D-new-escalation-reason-enum` deferral preserved); the predicate downgrades to the existing canonical reason `incomplete_intake` (already in the enum per `EscalationReasonResolver.java:84-103`).
- **D5 UC-switching continuity invariants** are NOT per-UC-pair if-else. Verify D5 matrix is principle-level (one rule per dimension; per-UC variation enters only via registry-level intersection on intakeFields). A per-UC-pair branch table would be a §1.7 violation.

If ANY proposed design surface encodes a §1.7 violation, raise as a BLOCKING finding and recommend `fix_required` for design doc revision.

## 5. Hard-fence verification

- **Sprint 36 contract §6 hard fences (19 items):** verify each is honored in the diff. Especially:
  - #1 No per-UC-branch if-else in skill body
  - #2-#3 No `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md` edit
  - #14 No edits to sprint archives under `docs/sprints/sprint-001-*` through `sprint-035-*`
  - #15-#16 No `docs/foundational/` or `docs/milestones/` edit
  - #17-#18 No Tier-0 invariant added in `runtime_freeze_and_risk_policy.md`; Tier-0 candidate write-up lives in `skill_foundation_design.md` §8 ONLY
  - #19 No expansion of S1 predicate beyond human authorization
- **M2-Skill milestone-level hard fences (19 items per `docs/milestone_objective.md` §6):** verify the design doc honors each. Especially:
  - §6 #4 INVERSION scope-bound (S1 predicate fires ONLY within the verbatim-authorized scope; design doc §4 D3 reproduces verbatim)
  - §6 #5-#7 No classifier / `INTAKE_UCS` / `escalation_reason` enum touch
  - §6 #8 No Tier-0 invariant added without Sprint 36 freeze pre-authorization + human-review escalation (the design doc §8 surfaces candidates; deliver-agent + human DEFERRED both; no addition to `runtime_freeze_and_risk_policy.md` in Sprint 36 close)
- **MATERIAL FINDING verification:** independently verify the dev's §1.3 + §5.4 claim that S1+S2 partial predicates already ship in `AgentRunLoopImpl`. Read the cited line ranges (Sprint 6 §G2 ~690-734; Sprint 7 §I2 ~628-651; Sprint 11 §M1 ~745-759). Confirm dev's reading is correct. If dev mis-read, raise as BLOCKING (design doc D3/D4 framing depends on the finding).
- **Premise #8 refinement verification:** independently verify `UseCaseDefinition` record has NO `requiredIntakeFields` field; `IntakeFieldsRegistry` IS the actual source. Confirm design doc D4 §5.2 cites the correct source. If dev's correction itself is wrong, raise as BLOCKING.

## 6. Schema and reproducibility checks

- **Design doc structure:** verify 8 sections per `docs/sprint_objective.md` §5.1 baseline are all present + cover all 6 sub-decisions D1-D6. Verify §7 §4.1 walk-through is complete (all 9 questions answered). Verify §8 Tier-0 candidate write-ups for 7.1 + 7.2 carry explicit human-review escalation request.
- **Frontmatter edit on upstream proposal:** verify `docs/proposals/skill_orchestration_candidates.md` frontmatter carries `superseded_by: docs/proposals/skill_foundation_design.md` + `status: superseded`; body unchanged.
- **Handoff structure:** verify 12-section shape per `docs/sprint_objective.md` §11. §12 closure verdict placeholder (NOT filled by dev per `feedback_handoff_verdict_section_delegation.md`).
- **Reproducibility:** verify every claim in the design doc about current code shape cites file:line (e.g., `PhaseEvaluator.java:633-665`, `AgentRunLoopImpl.java:628-651`, etc.). Spot-check 3-5 cited file:line ranges; confirm they exist + match the design doc claim.

## 7. Validation runs (you re-execute)

From a clean checkout of the dev commit:

- **Java test baseline preservation:**

  ```bash
  cd server && mvn test -q
  ```

  Expected: `Tests run: 983, Failures: 1, Errors: 0, Skipped: 2` (or byte-identical to M1 close baseline). The 1 inherited failure is `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` attributed to dirty working-tree `system_prompt.txt` per M1 Codex review notes. Any new failure delta is a BLOCKING finding (Sprint 36 ships zero Java change).

- **No design-doc YAML loader check needed** — design doc is markdown prose, not a YAML CaseSpec. Skip schema validation.

- **No eval run required** at Sprint 36 close. Sprint 36 ships zero behaviour change.

## 8. Tier-0 candidate DEFER rationale review (Codex-substantive)

The design doc §8.3 + §8.4 surface two Tier-0 candidate questions:

- **OQ 7.1 — S1 citation predicate semantics → `runtime_freeze_and_risk_policy.md` §1/§2?**
- **OQ 7.2 — S2 intake-completeness predicate semantics → `runtime_freeze_and_risk_policy.md` §1/§2?**

Deliver-agent + human pre-decided DEFER for both at Sprint 36 close authorization. Codex INDEPENDENTLY verifies DEFER is sound:

- **For 7.1 (S1):** Is deferring Tier-0 codification reasonable given (a) the predicate is narrow + Skill-bounded per human authorization §6 #4; (b) Sprint 37 has not yet shipped — observed trace evidence is needed before deciding whether the predicate semantics warrant Tier-0 permanence; (c) the existing `D-hard-citation-gate` deferral preserved? Expected verdict: DEFER is sound. If Codex disagrees (e.g., the predicate IS Tier-0 territory even before Sprint 37 ships), surface as a Finding with rationale.
- **For 7.2 (S2):** Is deferring Tier-0 codification reasonable given (a) the predicate has been in production since Sprint 7 §I2 (`shouldRejectIncompleteIntakeHandover`); (b) adding Tier-0 status now adds zero new architectural protection beyond what's already shipped + tested; (c) the §E1 downgrade pattern is well-established? Expected verdict: DEFER is sound. If Codex disagrees, surface as a Finding.

If Codex CONFIRMS DEFER for both: note in the verdict header that Tier-0 elevation is correctly deferred + flag for re-evaluation at M2 close (after Sprint 37 ships + traces observed).

If Codex DISAGREES with either DEFER: raise as `out_of_scope_review` (the deferral decision is deliver-agent + human authority per `docs/milestone_objective.md` §10 stop condition #1; Codex's disagreement is informational input to the human's deliberation, not a Sprint 36 blocker on its own).

## 9. Deferred / non-blocking observations

- The 6 non-Tier-0 OQs (7.3 — S1 fail behaviour rejection; 7.4 — S2 Option A keep existing; 7.5 — D5.1 soft-signal projection; 7.6 — D1.1 no new envelope projection field; 7.7 — D5.2 cap=3/aging=4; 7.8 — premise #8 amend Sprint 38 contract) have dev-recommended defaults + deliver-agent + human agreement. Codex may note any disagreement as informational; not a blocking close gate (per-sub-sprint planning rounds for Sprint 37/38/39 carry the picks).
- The MATERIAL FINDING (S1+S2 partial predicates ship Sprint 6/7/11) is honest dev surfacing; not a Sprint 36 blocker. It changes Sprint 37 + 38 scoping (Sprint 38 becomes much lighter). Codex notes as informational; deliver-agent + human re-scope Sprint 38 contract at planning round.
- The premise #8 refinement (`UseCaseDefinition.requiredIntakeFields` → `IntakeFieldsRegistry`) is a mechanical class-name correction; not a Sprint 36 blocker. Deliver-agent bakes into Sprint 38 contract draft.

## 10. Output format (write to `docs/codex-findings.md`)

Replace the file content with the standard §4.2 sprint-close header:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>

## Review Evidence
<bullet list — review scope, commit range, what you re-ran, what passed, what was independently verified (esp. material finding + premise #8 refinement)>

## Blocking Findings (if any)
<numbered list; each entry quotes diff snippet OR design-doc paragraph + cited file:line>

## Anti-Hardcode Kernel
<nine-question walk against the PROPOSED DESIGN (D1-D6); each Q with one-line verdict>

## §1.7 Boundary Check
<the §4 design-doc boundary checks; pass/fail per surface>

## Hard-Fence Verification
<the §5 contract + milestone fences; pass/fail per fence; MATERIAL FINDING + premise #8 verification noted>

## Schema And Reproducibility Checks
<the §6 checks; design doc structure + frontmatter edit + handoff structure verified; cited file:line spot-checks reproduced>

## Validation Runs
<the §7 results; commands + output>

## Tier-0 Candidate DEFER Rationale Review
<the §8 walk; CONFIRM or DISAGREE per Tier-0 candidate (7.1 + 7.2); rationale>

## Deferred / Non-Blocking Notes
<the §9 items>
```

## 11. Expected verdict shape

If all gates pass + design doc honors human authorization + DEFER is sound for both Tier-0 candidates: **`decision: pass / blocking_count: 0`**. The cleanest outcome for a design-freeze sub-sprint where the dev shipped per spec is a single-pass close.

If §2 scope-discipline fails: **`decision: fix_required`** with the violating diff snippet quoted as Finding #1.

If the design doc D3 expands the S1 predicate beyond the human-authorized scope: **`decision: fix_required`** with the violating design-doc paragraph quoted (cite the verbatim authorization in `docs/milestone_objective.md` §6 #4 as the load-bearing reference).

If the MATERIAL FINDING (S1+S2 partial shipping) is wrong (dev mis-read the existing surface): **`decision: fix_required`** — design doc D3/D4 framing depends on the finding.

If the premise #8 refinement is wrong (dev's correction is itself incorrect): **`decision: fix_required`** — design doc D4 §5.2 must cite the correct source.

If a §1.7 violation is found in the proposed design (per §4): **`decision: fix_required`** — design doc must be revised to remove the violation OR scope must be split.

If Codex DISAGREES with the Tier-0 candidate DEFER decisions (per §8): **`decision: out_of_scope_review`** — the deferral is deliver-agent + human authority; Codex's disagreement is informational input.

If a substance concern is found that is NOT in scope for Sprint 36 (e.g., a critique of the M2-Skill milestone scope itself — the milestone is approved; the per-sub-sprint review evaluates the sub-sprint per the contract): **`decision: out_of_scope_review`** with the concern named and the milestone-contract reference cited.

## 12. Self-check before submitting

- [ ] §2 scope-discipline gate walked; every disallowed surface checked; frontmatter EDIT on upstream proposal verified APPEND-ONLY.
- [ ] §3 §4.1 nine-question kernel walked against the PROPOSED DESIGN (D1-D6), not against the Sprint 36 diff.
- [ ] §4 §1.7 boundary check walked against D1/D2/D3/D4/D5; verbatim authorization in design doc §4 (D3) verified.
- [ ] §5 hard-fence verification (Sprint 36 contract §6 19 items + M2-Skill §6 19 items + MATERIAL FINDING verification + premise #8 verification).
- [ ] §6 schema + reproducibility (design doc structure + frontmatter edit + handoff structure + cited file:line spot-checks).
- [ ] §7 Java baseline re-run from clean checkout.
- [ ] §8 Tier-0 candidate DEFER rationale review (independent verdict on 7.1 + 7.2 DEFER soundness).
- [ ] §9 deferred items noted as non-blocking.
- [ ] `docs/codex-findings.md` written per §10 format.
- [ ] Verdict per §11 expected shape.
