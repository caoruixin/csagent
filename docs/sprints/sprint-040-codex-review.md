---
title: Sprint 40 Codex per-sub-sprint review archive — Teaching extraction (NEW M2 sub-sprint 4)
doc_tier: sprint-archive
status: archived
implementation_status: historical
source_of_truth: this file
last_reviewed: 2026-05-18
review_cadence: never
supersedes: []
superseded_by: null
notes: >
  Verbatim archive of `docs/codex-findings.md` as written by Codex per
  `iteration_governance.md` §4.3 trigger #3 (system_prompt.txt structural
  change — LLM input contract surface) at Sprint 40 close 2026-05-18.
  Codex was dispatched from `compact/sprint-040-review-prompt.md` (the
  deliver-agent's per-sub-sprint review prompt) against the Sprint 40 dev
  commit `9130abc` (parent `2f412b6`; branch `refactor/remove-the-shackles`).
  Verdict: **decision: pass / blocking_count: 0** on first pass (single
  round). Codex independently re-walked the §4.1 9-question kernel + §1.7
  boundary check + all 33 Sprint 40 §6 + 22 M2 §6 hard fences + reproducibility
  checks + validation runs + Tier-0 candidate reaffirmation + 5 OQ
  independent verdicts + 2 contract-drift item classifications. All OQ
  verdicts aligned with the deliver-agent + human pre-decisions (which were
  withheld from the review prompt per the deliver-agent's choice to surface
  OQs without pre-loaded dispositions). Codex's 3 non-blocking observations
  were recorded for housekeeping: (a) deliver-agent's review-prompt per-file
  numstat estimates differed from actuals (continuation of the Sprint 39
  `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` lapse);
  (b) the dev handoff's "byte-for-byte" framing for the migration is more
  precisely "formatting-normalized content equivalence" (bullet markers
  removed + first post-colon "The" became "the"); (c) M2 close fold-back
  queue gains the Sprint 37 design doc §7.8 typo (OQ-S40.5 routed to M2
  close per Sprint 38 OQ-S38.1 precedent). Live `docs/codex-findings.md`
  resets to scaffold after this archive lands per
  `feedback_packaging_codex_findings_supersession.md` delete-and-add
  pattern.
---

# Sprint 40 Codex per-sub-sprint review (archived 2026-05-18)

**Codex run context.** Dispatched by the human from `compact/sprint-040-review-prompt.md` at Sprint 40 close 2026-05-18. Review scope: Sprint 40 dev commit `9130abc` (parent `2f412b6`; branch `refactor/remove-the-shackles`). Trigger: `iteration_governance.md` §4.3 trigger #3 (system_prompt.txt structural change — LLM input contract surface). Output written to `docs/codex-findings.md` per §11 of the review prompt + the scaffold convention.

## Sprint Review Decision
decision: pass
blocking_count: 0
summary: Sprint 40 passes. The dev commit `9130abc` is a prompt-side content-relocation slice only: Sprint 31/Sprint 33/DISCOVER teaching moved from `system_prompt.txt` into the DISCOVER Skill procedure, Sprint 23 `already_called` and the `request_handover` decision tree remain in the shell, no Sprint 41 state-bus or runtime semantic surface was touched, and Java validation shows only the documented inherited `SystemPromptUserRequestedTiebreakerTest` failure.

## Review Evidence
- Reviewed commit range `2f412b6..9130abc`; `git diff --stat` shows exactly 5 files and `639 insertions(+), 25 deletions(-)`.
- Loaded the constitution chain, M2 milestone objective, Sprint 40 objective, Sprint 40 handoff, Sprint 37 design freeze §7, Sprint 37/38/39 archives, runtime freeze policy, FAQ grounding contract, and Sprint 40 compact prompts.
- Spot-checked `system_prompt.txt`, `discover_triage.yaml`, both touched tests, and all cited hard-fenced Java/YAML surfaces.
- Server tree was clean before validation; repo-level dirty files are pre-existing deliver-agent docs/compact artifacts, so review uses the committed range for dev-scope verification.

## Blocking Findings
None.

## Anti-Hardcode Kernel (per `iteration_governance.md` §4.1)
- Q1: PASS — no new keyword/regex/if-else/enum/per-UC matrix for an LLM-owned semantic decision; migrated text is principle-level Skill procedure teaching.
- Q2: PASS/N/A — no Tier-0 invariant added; `docs/runtime_freeze_and_risk_policy.md` is unchanged.
- Q3: PASS — no new hard branch appears; existing projection slots remain LLM-soft signals.
- Q4: PASS — scan found no Alice/source-session/CaseSpec/visible-eval text in runtime prompt, Skill YAML, or tests.
- Q5: PASS — semantic ownership stays with the LLM; no Java runtime decision was added.
- Q6: PASS — no prompt if-else dump was introduced; the preserved `request_handover` tree remains the Sprint 37-approved situation-to-enum universal block.
- Q7: PASS — tool schema, `PhasePlan.allowedTools`, PII/safety floor, and Sprint 39 grounding floor are unchanged.
- Q8: PASS — target coverage is the 11-test `SkillTeachingMigrationIntegrationTest`; neighbor/negative coverage from Sprint 38/39 and M1 tests remains green.
- Q9: PASS/N/A — no temporary kill switch or sunset surface was added; revert of `9130abc` restores the relocated content.

## §1.7 Boundary Check
- Raw eval phrase encoding: PASS — no Alice, bad-case, source-session id, or visible-eval phrase added to prompt/YAML/runtime.
- UC-specific hard rules for soft semantic decisions: PASS — no per-UC-pair branch table added; existing DISCOVER examples predate Sprint 40.
- Eval-spec widening: PASS — no `eval_interactive/` edits.
- Visible-eval optimization at shadow/generalization cost: PASS — no eval metric or bad-case result is used as a gate; M2 §5 recalibration respected.
- Prompt as if-else dump: PASS — shell is 80 lines with universal mechanics + decision tree; DISCOVER Skill procedure is principle-level.

## Hard-Fence Verification
- Scope discipline: PASS — only `system_prompt.txt`, `discover_triage.yaml`, `SkillTeachingMigrationIntegrationTest.java`, `PhaseEvaluatorSkillIntegrationTest.java`, and `docs/sprints/sprint-040-handoff.md` changed.
- Sprint 40 §6 #1/#2: PASS — `system_prompt.txt:23-28` is byte-identical to pre-Sprint-40 and pre `54-101` equals post `33-80`.
- Sprint 40 §6 #3/#4: PASS — no Sprint 31/33 teaching was copied into RESOLVE Skills; the other five Skill YAMLs are unchanged.
- Sprint 40 §6 #5-#13: PASS — `PhaseEvaluator.java`, `AgentRunLoopImpl.java`, `SkillGuardrailDispatcher.java`, `ContextProjectionBuilder.java`, M1 intake surfaces, and Sprint 11/11.1/12 frozen surfaces are unchanged; no `SkillStateBus.java` exists.
- Sprint 40 §6 #14-#28: PASS — no Tier-0/governance/foundational/freeze/eval/Alice/override edits in the dev commit; Java composition tests are not mocked-LLM behavior evidence.
- Sprint 40 §6 #29-#33: PASS — no per-UC branch in `discover_triage.yaml`, no customer-facing language prescription, M1/Sprint 38/Sprint 39 tests pass, and Sprint 41 specifics are not pre-decided.
- M2 §6 #1-#22: PASS — no per-UC Skill body, no new predicate, no classifier/drift/router/tool touch, no `INTAKE_UCS` or enum widening, no Tier-0 policy edit, no old design deletion, and M1 functional surfaces are preserved.

## Schema And Reproducibility Checks
- PASS — line counts reproduced: `system_prompt.txt` 80, `discover_triage.yaml` 30, new test 264, `PhaseEvaluatorSkillIntegrationTest.java` 400, handoff 312.
- PASS — unchanged claims reproduced with empty `git diff 2f412b6..9130abc -- <path>` for the hard-fenced Java files and the other five Skill YAMLs.
- PASS — `docs/proposals/skill_registry_design.md:2015` contains the Sprint 31/Sprint 33 typo; implementation used the correct pre-Sprint-40 line mapping.
- NON-BLOCKING NOTE — per-file `git diff --numstat` is `312/0`, `2/23`, `1/1`, `60/1`, `264/0`; this differs from some review-prompt per-file numstat estimates but not from the total stat or touched-file list.
- NON-BLOCKING NOTE — migrated Sprint 31/33 content is semantically equivalent but not literally byte-identical after whitespace-only normalization because bullet markers were removed and the first post-colon `The` became `the`; this does not change the LLM-soft teaching content.

## Validation Runs
- Full suite: `cd server && mvn clean test -q 2>&1 | tail -25` reported `Tests run: 1105, Failures: 1, Errors: 0, Skipped: 2`; sole failure is `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53`.
- New Sprint 40 tests: `SkillTeachingMigrationIntegrationTest` 11/11 PASS.
- Existing Sprint 38 golden test: `PhaseEvaluatorSkillIntegrationTest` 13/13 PASS.
- Sprint 39 tests: `PhaseEvaluatorResolveSkillIntegrationTest` 14/14, `SkillGuardrailDispatcherTest` 24/24, `ResolveFaqGuardrailsTest` 11/11, `ResolveIntakeGuardrailsTest` 11/11 PASS.
- Sprint 38 SkillRegistry-core tests: `SkillTest` 9/9, `SkillRegistryTest` 11/11, `SkillLoaderTest` 18/18 PASS.
- M1 functional surface: `Sprint71PartialIntakePersistenceTest` 14/14 PASS.
- Inherited baseline failure: `SystemPromptUserRequestedTiebreakerTest` 6 tests / 1 expected failure.

## Tier-0 Candidate Independent Verification
- C1: REAFFIRM rejected-as-new-candidate; Sprint 40 does not affect tool-whitelist enforcement.
- C2: REAFFIRM qualified-defer; Sprint 40 ships no new dispatcher/non-overridability evidence.
- C3: REAFFIRM qualified-defer; Sprint 40 ships no state-bus boundary enforcement.
- C4/C5: REAFFIRM not candidates; Sprint 40 adds no S1/S2 predicate surface.

## OQ Independent Verification
- OQ-S40.1: AGREE WITH DEV — the `PhaseEvaluatorSkillIntegrationTest.java` golden update is an in-scope behavioral-equivalence/test-preservation edit under the Sprint 39 precedent; no fix iteration.
- OQ-S40.2: AGREE WITH DEV — combining the Skill envelope paragraph and DISCOVER pointer respects design §7.3 at content level; no refactor required.
- OQ-S40.3: STATUS QUO — inherited `SystemPromptUserRequestedTiebreakerTest` failure persists and is not a Sprint 40 blocker.
- OQ-S40.4: AGREE WITH DEV — no Sprint 23 cosmetic rewording was required; existing text is already cross-tool/cross-Skill framed.
- OQ-S40.5: ROUTE TO M2 CLOSE FOLD-BACK — fix the design-doc typo separately; Sprint 40 implemented the correct mapping.
- Drift item 1: CONTENT-EQUIVALENCE PRESERVED — 80 lines misses the 60-75 observation target but preserves load-bearing content; no fix required.
- Drift item 2: REAFFIRM PRECEDENT — behavioral-equivalence test edits do not count as scope creep when they are necessary to keep existing golden tests aligned.

## Deferred / Non-Blocking Notes
- The exact-byte claim for migrated paragraphs should be described as formatting-normalized content equivalence in future handoffs/prompts.
- M2 close should fold back the Sprint 37 design typo and the existing template-vs-legacy wording notes as a separate governance commit.
- The inherited `SystemPromptUserRequestedTiebreakerTest` literal `Sprint 6` anchor remains a known housekeeping item outside Sprint 40 scope.
