Paste the content below this line into a fresh Codex session after the dev's commit lands. No PR will be opened; review the commit range.

---

# Sprint 31 review prompt — alternate_candidate_use_cases projection slot (Option β)

You are the review agent (Codex). Single track, single layer (`prompt_projection`). Sprint 31 is **semantic-touching** (touches `system_prompt.txt` + projection surface). The §4.1 Anti-Hardcode kernel is **REQUIRED** — no exemption. Codex review is **REQUIRED** at close.

The dev's commit lands on branch `refactor/remove-the-shackles` (or whatever branch the human points you at).

## 1. Loader stanza

Read in order before any verdict:

1. `AGENTS.md`.
2. `docs/current/doc_governance.md`.
3. `docs/current/iteration_governance.md` §1 / §3 / §4.1 / §4.2 / §5 / §7.
4. `docs/sprint_objective.md` — Sprint 31 scope, end to end.
5. `docs/proposals/alternate_uc_signal_data_source_design.md` — the design freeze; §5 code-paths table is what Sprint 31 shipped verbatim.
6. `docs/sprints/sprint-031-handoff.md` — the dev's handoff.
7. Precedents: `docs/sprints/sprint-020-handoff.md` + Sprint 20 fix-iteration handoff (the `already_called` ship pattern Sprint 31 mirrors); `docs/sprints/sprint-023-handoff.md` (the `system_prompt.txt` teaching-paragraph ship pattern Sprint 31 mirrors).
8. `compact/sprint-031-dev-prompt.md` — what was authorized vs what landed.

## 2. §4.1 kernel — REQUIRED, walk verbatim

Sprint 31 is semantic-touching (`prompt_projection`-layer + `system_prompt.txt` edit). Run the §4.1 nine-question kernel verbatim against the dev's commit range. Focus areas:

- **Q1 (keyword/regex/if-else/per-UC matrix?)** — verify NO new pattern-based code in the data-source pipeline (`SessionManager.java` AMBIGUOUS-branch edit is a literal preservation of `routingResult.ambiguousCandidates()`, not a synthesis). Also verify the `system_prompt.txt` teaching paragraph is NOT an if-else dump per UC.
- **Q2 (Tier-0 justification?)** — N/A (no new Tier-0 added per stanza).
- **Q3 (soft signal achievable?)** — verify YES (already the design's recommendation; check that the implementation preserves the soft posture).
- **Q4 (eval-text encoding?)** — verify NO Cas eSpec id / trace text encoded into the slot or the teaching paragraph.
- **Q5 (semantic ownership shift?)** — verify NO. The LLM still owns the drift / topic-shift semantic decision per Constitution §1.3. The Java change is data preservation (capturing what's already produced); the projection change is surface (publishing existing state); the prompt change is teaching (informing the LLM about a new observable signal). No semantic decision moves from LLM to Java.
- **Q6 (prompt as if-else dump?)** — verify the new teaching paragraph is principle-level + observable-state guidance per the `already_called` paragraph pattern (Sprint 23 §3), NOT an if-else per UC.
- **Q7 (tool / capability / PII / grounding preserved?)** — verify YES.
- **Q8 (generalization coverage?)** — verify target / neighbor / negative / shadow per `docs/sprint_objective.md` §9. Target = UC-A ↔ UC-C drift family (Java regression-test fixture). Neighbor = other UC pairs in same intake topic-subject family. Negative = single-issue ROUTED session. Shadow = deferred to Sprint 31+1 (CaseSpec-authoring sprint per OQ4 pre-pick) — verify the handoff §6 explicitly defers shadow with rationale.
- **Q9 (rollback / sunset?)** — verify the change is permanent (no feature-flag scaffolding), which is acceptable since the slot is additive + soft-signal + reversible by reverting the migration + the projection block.

## 3. Sprint-31-specific deviations to verify

### 3.1 Bundled-implementation expectations (NOT scope drift)

The bundle is one implementation commit landing 7–8 files per `docs/sprint_objective.md` §6 table:

1. `BotSession.java` — new field addition.
2. `db/migration/V14__intake_ambiguous_candidates.sql` — new Flyway migration.
3. `SessionManager.java` — two edits (AMBIGUOUS-branch capture + builder-line null-default).
4. `ContextProjectionBuilder.java` — new projection slot.
5. `system_prompt.txt` — new teaching paragraph (sibling to `already_called` per OQ2 pre-pick).
6. `IntakeAmbiguousCandidatesProjectionTest.java` (or similar) — new regression test file with 5–8 tests.

A bundle in this exact shape is **not scope drift**. Do not flag.

### 3.2 Hard-fence violations (BLOCKING)

`docs/sprint_objective.md` §7 lists 15 hard fences. Any violation is BLOCKING. Specifically check:

1. **No edits to `RuntimeIntentClassifier.java` / `IntentClassification.java`** — Option α NOT taken.
2. **No edits to `DriftResult.java` / `DriftDetector.java`** — Option δ NOT taken.
3. **No edits to `UseCaseRouter.java`** — OQ1 pre-pick is binding.
4. **No edits to `ClassifyUseCaseTool.java`** — LLM-facing tool stays unchanged.
5. **No new CaseSpecs / no edits to existing case families** — Sprint 20 / 29 cascade fence; OQ4 pre-pick defers case-family work.
6. **No edits to foundational docs / governance docs / sprint archives**.
7. **No Tier-0 changes** in `docs/runtime_freeze_and_risk_policy.md`.
8. **No deadline / model / retry / budget config edits**.
9. **No editing of `R-prompt-phase-plan-directive-followship`** at `docs/action_bank.md:450`.
10. **No new projection slots beyond `alternate_candidate_use_cases`** — opportunistic-field expansion is BLOCKING.

### 3.3 Anti-hardcode posture check (BLOCKING)

The runtime MUST NOT branch on the slot's value. Cross-check the integration test (T8 in dev-prompt §3.7 — non-enforcement evidence mirror of `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest`): it should assert the runtime's reroute / phase / escalation decisions do NOT change as a function of the slot's contents. If the test is missing OR is too weak to demonstrate non-enforcement, that is a `fix_required` blocker.

If the dev added a Java check that gates any decision on the slot's value (e.g. "if alternate_candidate_use_cases.contains('UC-C') then trigger drift detector"), that is a §1.7 violation and BLOCKING.

### 3.4 Schema-backwards-compat (BLOCKING)

`alternate_candidate_use_cases` is additive — no existing projection slot may be renamed, removed, or have its shape changed. Verify with:

```bash
# Existing projection keys preserved
jq '.case_results[0].per_turn_trace[0].projection | keys' \
  eval_interactive/results/<sprint-31-run-id>/results.json

# Sprint 28 reference keys
jq '.case_results[0].per_turn_trace[0].projection | keys' \
  eval_interactive/results/20260514-181257/results.json
```

The Sprint 31 keys set MUST be a superset of the Sprint 28 keys set (with `alternate_candidate_use_cases` as the only new key).

### 3.5 Smoke regression check (BLOCKING)

Per `iteration_governance.md` §5.1: no regression on `composite_score` / pass-rate / safety / grounding / wrong-containment / over-escalation floors vs the Sprint 28 reference. Cross-check by running the handoff §5 / §11 jq extractions against the dev's smoke `results.json`; confirm output matches.

The slot's pass-rate IS NOT a success metric (Sprint 31 is a projection-surface ship, not a behaviour-change ship — the LLM may or may not act on the new slot in 14-case smoke).

### 3.6 Reproducibility (BLOCKING)

Every quantitative claim in the handoff cites source path + extraction recipe + literal output. Cross-check ≥ 1 number per cited table; re-run the recipe against the named `results.json` and confirm output matches.

### 3.7 Premise re-verification check (NON-BLOCKING)

Handoff §3 should re-state `docs/sprint_objective.md` §4's premise item (`SessionManager.java:185–189` AMBIGUOUS-branch discards `routingResult.ambiguousCandidates()`) with the session-start re-verification. If skipped but code is correct, flag non-blocking.

### 3.8 OQ pre-pick adherence (NON-BLOCKING)

Verify the dev's commit honours all 5 OQ pre-picks from `docs/sprint_objective.md` §5:

- OQ1 (ROUTED-null): no `setIntakeAmbiguousCandidates` call in the ROUTED branch of `SessionManager.java`.
- OQ2 (sibling teaching placement): new paragraph is adjacent to `already_called` lines 23–28 of `system_prompt.txt`, NOT inside the DISCOVER-phase block.
- OQ3 (no §7.2 edit): `iteration_governance.md` §7.2 unchanged; handoff §1.3 flags the drift for the fold-back agent.
- OQ4 (no CaseSpecs): no new files under `eval_interactive/case_specs/`.
- OQ5 (no `R-uc-cdf` interaction): no edit to `R-uc-cdf-get-customer-context-bot-actual-usage` at action_bank.md.

A deviation from any OQ pre-pick without an explicit handoff rationale is a `fix_required` blocker on the OQ.

## 4. Packaging rollforward

The commit may bundle deliver-agent files (`docs/sprint_objective.md`, `compact/sprint-031-*-prompt.md`, `docs/10-handoff.md`) + Sprint 30 close artefacts (`docs/sprints/sprint-030-objective.md`, `docs/sprints/sprint-030-handoff.md` §12 fill) + pre-existing unrelated working-tree mods.

If the only out-of-scope content is path-based packaging artefacts AND substantive findings close per your own evidence, this is **A-with-packaging-note** territory — `out_of_scope_review` with `blocking_count: 1` on path grounds is acceptable. Classify and report; do not block substance. Per `feedback_out_of_scope_review_packaging_rollforward.md`. Content-based blockers (anything failing §3.2 / §3.3 / §3.4 / §3.5 / §3.6) → `fix_required` instead.

## 5. §4.2 sprint-close header

At the top of the dev's `docs/codex-findings.md` (or wherever Codex publishes — defer to recent precedent at `docs/sprints/sprint-028-codex-review.md` shape):

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

Per-finding write-ups in the body. Mirror `docs/sprints/sprint-028-codex-review.md` shape.

## 6. Deferral rule

Valid-but-out-of-scope concerns go to `docs/action_bank.md` as deferred items, NOT blocker findings. Do not edit `docs/action_bank.md` yourself — that's the deliver-agent's close-turn action. If the handoff §7 already names the concern, acknowledge; do not list as a finding.

## 7. Final reminders

- Read the commit-range diff start to end; verify the bundle.
- One verdict, one header. Substance over ceremony.
- More than 8 findings on a single-track `prompt_projection` ship is over-review; trim to the most load-bearing.
- The §1.7 forbidden list is canonical for what Sprint 31 must NOT contain. Confirm before `pass`.
- The non-enforcement integration test (T8) is load-bearing for the anti-hardcode posture; verify it exists AND is strong enough to demonstrate the runtime ignores the slot's value.
