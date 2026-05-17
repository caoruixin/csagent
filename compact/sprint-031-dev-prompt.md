Paste the content below this line into a fresh Claude Code session on branch `refactor/remove-the-shackles` (or whatever branch the human points you at). Working tree at session-start carries deliver-agent-owned files + the Sprint 30 close-window outputs — see §10.

---

# Sprint 31 dev prompt — alternate_candidate_use_cases projection slot (Option β implementation)

You are the dev agent. Single track, single layer (`prompt_projection`). Authoritative scope: `docs/sprint_objective.md`. Ships the Option β design frozen at `docs/proposals/alternate_uc_signal_data_source_design.md` (Sprint 30 close, 2026-05-16). Sprint 31 ships **code** (Java + Flyway + prompt + regression test); 5–8 files modified per the §6 file table.

## 1. Loader stanza

Read in order before any code:

1. `AGENTS.md` (transitively loads `iteration_governance.md` §1 §3 §5 §7 + `doc_governance.md` + `agent_context_guide.md`).
2. `docs/sprint_objective.md` — Sprint 31 scope. §4 premise check + §5 pre-picked defaults are authoritative; §6 file table is what Sprint 31 ships verbatim.
3. `docs/proposals/alternate_uc_signal_data_source_design.md` — the design freeze. §5 code-paths table is the source of truth for what Sprint 31 implements. §6 §7 stanza pre-fill is what Sprint 31 uses for its handoff §10 self-walk.
4. `docs/sprints/sprint-030-handoff.md` — Sprint 30 dev's recommendation walk-through; §4–§7 explain why Option β over α / γ / δ / ε.
5. `docs/sprints/sprint-020-handoff.md` §5 + Sprint 20 fix iteration handoff — the cleanest implementation precedent for an additive soft-signal projection slot + prompt teaching. Sprint 31 mirrors this shape verbatim (the `already_called` ship pattern).
6. `docs/sprints/sprint-023-handoff.md` §3 + §5 — Sprint 23 added the system_prompt.txt teaching paragraph for `already_called`; Sprint 31 mirrors the teaching paragraph shape.
7. Run a fresh Context Pack (per `agent_context_guide.md`) before any code.

## 2. Premise re-verification (mandatory — do not start writing code without this)

The §4 of `docs/sprint_objective.md` carries one load-bearing premise item that Sprint 31 MUST re-verify at session start:

- **`SessionManager.java:185–189` AMBIGUOUS branch currently discards `routingResult.ambiguousCandidates()`**. Spot-check by reading `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java:180–195` (one read; one paragraph in handoff §3). If `RoutingResult.ambiguousCandidates()` has been renamed, removed, or restructured between 2026-05-16 and dev session start, STOP and surface drift in handoff §1.6 + §3 + §11; do NOT code under a false premise.

Additional spot-checks (lighter; just `grep` / `head` is fine):

- `BotSession.java` line 53–54 still has `candidateUseCases: String[]` shape (mirror target for the new field).
- `ContextProjectionBuilder.java` lines 380–394 still emit `candidate_use_cases` (adjacent placement for the new slot).
- `system_prompt.txt` lines 23–28 still hold the `already_called` teaching paragraph (sibling placement target per OQ2 pre-pick).
- Latest Flyway version is V13 (Sprint 24); the new migration is V14.

## 3. The work — 5–8 file changes per §6 file table

Implement the §6 table verbatim. The dev MAY name the regression-test file differently from `IntakeAmbiguousCandidatesProjectionTest.java` if a more conventional name fits the existing test-package layout; document the choice in handoff §4. Otherwise, no deviations from the §6 table.

Specific implementation notes per row:

### 3.1 `BotSession.java` field addition

Mirror the existing `candidateUseCases` field's annotations:

```java
@Column(name = "intake_ambiguous_candidates", columnDefinition = "text[]")
private String[] intakeAmbiguousCandidates;
```

Adjacent placement (line 53–54 area, just below or above `candidateUseCases`). Lombok `@Data` handles accessors.

### 3.2 Flyway migration

Path: `server/src/main/resources/db/migration/V14__intake_ambiguous_candidates.sql` (next version after V13 per premise check).

```sql
ALTER TABLE bot_sessions ADD COLUMN intake_ambiguous_candidates text[];
```

No backfill required; null is the valid "no snapshot" state.

### 3.3 `SessionManager.java` AMBIGUOUS-branch edit

At `SessionManager.java:185–189` (AMBIGUOUS branch of the routing switch), BEFORE the `setCurrentPhase("DISCOVER")` call, assign:

```java
List<String> ambiguousCandidates = routingResult.ambiguousCandidates();
session.setIntakeAmbiguousCandidates(
    ambiguousCandidates == null
        ? null
        : ambiguousCandidates.toArray(new String[0])
);
```

(Use the existing `ambiguousCandidates()` accessor on `RoutingResult` — verified at premise check.)

### 3.4 `SessionManager.java` ROUTED-branch (OQ1 pre-pick: leave null)

Per `docs/sprint_objective.md` §5 OQ1: leave `intakeAmbiguousCandidates` null on the ROUTED branch (lines 255–259). Lombok default is null; no explicit assignment required. Document the OQ1 pre-pick adherence in handoff §4.

### 3.5 `ContextProjectionBuilder.java` projection slot

Adjacent to the existing `candidate_use_cases` block (lines 380–394). Build an `ArrayNode` from `session.getIntakeAmbiguousCandidates()` filtered to exclude the active UC:

```java
ArrayNode alternateCandidateUcsNode = objectMapper.createArrayNode();
String activeUc = session.getActiveUseCase();
if (session.getIntakeAmbiguousCandidates() != null) {
    for (String uc : session.getIntakeAmbiguousCandidates()) {
        if (uc != null && !uc.isBlank() && !uc.equals(activeUc)) {
            alternateCandidateUcsNode.add(uc);
        }
    }
}
projection.set("alternate_candidate_use_cases", alternateCandidateUcsNode);
```

Schema-stable across turns: emit empty array (not absent / not null) when no snapshot OR when the filter removes the only entry. Adjacent placement keeps the related slots grouped.

### 3.6 `system_prompt.txt` teaching paragraph

Per OQ2 pre-pick: sibling placement adjacent to the `already_called` teaching at lines 23–28. Mirror the `already_called` paragraph's shape (3–5 sentences, no if-else dump, explicit soft-signal posture, names the slot + `accumulated_tool_results`-style provenance pointer + the "LLM owns the read" line). Suggested content shape (the dev SHOULD draft from first principles; this is a sketch):

> Alternate candidate use cases (the `alternate_candidate_use_cases` projection slot):
> The per-turn projection may include an `alternate_candidate_use_cases` array. This is the list of use cases the intake router considered plausible for the session's topic-subject family at session creation, minus the currently active use case. Each entry names a UC the user's intake message was AMBIGUOUS between; the routing layer chose `active_use_case` and surfaced the rest here as observable evidence.
> - When the user's later turn surfaces evidence that the active UC is the wrong fit (the user starts talking about a different concern that maps to one of the alternates), the slot tells you which alternates the router already considered plausible. You own the decision of whether to ask a clarifying question, propose a reroute, or stay on the active UC.
> - An empty `alternate_candidate_use_cases` array means either (a) the intake routed deterministically to one UC (no alternates considered), or (b) the active UC is the only surviving candidate after filtering. Proceed normally.
> - The runtime does not gate any tool or phase on this slot; it is soft signal you may or may not act on.

The dev SHALL refine this draft against the existing `already_called` paragraph's style. One paragraph, no if-else dump per UC.

### 3.7 Regression test file

New file (suggested name `IntakeAmbiguousCandidatesProjectionTest.java`, under `server/src/test/java/com/gumtree/csagent/service/runtime/`). 5–8 tests mirroring `AlreadyCalledProjectionTest` shape:

- T1: AMBIGUOUS intake assigns the candidates array onto `BotSession.intakeAmbiguousCandidates`.
- T2: ROUTED intake leaves `BotSession.intakeAmbiguousCandidates` null.
- T3: `ContextProjectionBuilder` emits `alternate_candidate_use_cases` as an array minus the active UC.
- T4: Empty snapshot (null `intakeAmbiguousCandidates`) → projection emits empty array.
- T5: Snapshot containing only the active UC → projection emits empty array (after filter).
- T6: Snapshot of 3 UCs, one of which is active → projection emits 2-element array.
- T7: (optional) Schema stability — `alternate_candidate_use_cases` key always present in projection JSON.
- T8: (non-enforcement evidence) Integration test mirroring Sprint 20 `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest`: the runtime's reroute / phase / escalation decisions do NOT branch on the slot's value. The slot is observable, the runtime ignores it.

Tests 1–7 are unit-level; test 8 is integration. Either land both in the same file or split per existing test-package convention; document the choice in handoff §4.

## 4. Reproducibility rule

Every number in the handoff cites source path + extraction recipe + literal output. No number without method. Per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

## 5. Scope discipline — only the §6 file table, no opportunistic fields

Sprint 31 ships exactly the §6 table. Do NOT:

- Add additional projection slots beyond `alternate_candidate_use_cases`.
- Refactor any of the existing slots (`candidate_use_cases`, `already_called`, etc.).
- Touch `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` — these are Option α / δ surfaces explicitly NOT taken.
- Author CaseSpecs — OQ4 pre-pick defers to Sprint 31+1.

If the implementation surfaces a need outside the table (e.g. "ContextProjectionBuilder needs a new helper method"), name it in handoff §7 as a proposed follow-on R-item, NOT a Sprint 31 ship.

## 6. Smoke rerun discipline

At sprint close:

```
cd eval_interactive && uv run eval-interactive run --set smoke --label sprint-31-alt-uc-slot
```

Bot: local Spring Boot at `http://localhost:8080` (verify with `curl -sS -o /dev/null -w "HTTP %{http_code}\n" http://localhost:8080/v1/demo/sessions` → `HTTP 200`).

Acceptance per `docs/sprint_objective.md` §10:

- No regression on `composite_score` / pass-rate / safety / grounding / wrong-containment / over-escalation floors vs the Sprint 28 reference (`eval_interactive/results/20260514-181257/results.json`).
- The new `alternate_candidate_use_cases` slot appears in `case_results[].per_turn_trace[].projection` on cases that hit the AMBIGUOUS intake branch. The dev SHALL identify ≥ 1 such case in handoff §5; if zero such cases exist in the 14-case set, the dev SHALL note this honestly — the slot is observable-only this sprint and the Sprint 31+1 case-family sprint will author cases that exercise AMBIGUOUS intake deliberately.

Extraction recipes for the handoff §5:

```bash
# Confirm the new slot is present in projection on cases that hit AMBIGUOUS intake
jq -r '
  .case_results[]
  | select(.per_turn_trace[]?.projection.alternate_candidate_use_cases != null)
  | "\(.case_id): turns=\(.per_turn_trace | length), alts=\([.per_turn_trace[].projection.alternate_candidate_use_cases // []] | flatten | unique)"
' eval_interactive/results/<run-id>/results.json
```

```bash
# Confirm schema-stability: key present on every populated turn
jq '[.case_results[].per_turn_trace[] | .projection | has("alternate_candidate_use_cases")] | group_by(.) | map({k: .[0], n: length})' \
  eval_interactive/results/<run-id>/results.json
```

## 7. Hard fences (the 15 forbidden surfaces)

Per `docs/sprint_objective.md` §7 — quick summary:

1. No edits to `RuntimeIntentClassifier.java`.
2. No edits to `IntentClassification.java`.
3. No edits to `DriftResult.java` / `DriftDetector.java`.
4. No edits to `UseCaseRouter.java`.
5. No edits to `ClassifyUseCaseTool.java`.
6. No new CaseSpecs.
7. No edits to existing case families (Sprint 20 / 29 cascade fence).
8. No edits to foundational docs.
9. No edits to governance docs; `iteration_governance.md` §7.2 fold-back deferred to normal cadence per OQ3 pre-pick.
10. No edits to sprint archives.
11. No Tier-0 changes.
12. No deadline / model / retry / budget config edits.
13. Every quantitative claim reproducible.
14. No mocked-LLM as primary evidence for prompt-causal claims (Sprint 31's smoke confirms the slot is present; it does NOT need to prove the LLM acted on it).
15. No editing of `R-prompt-phase-plan-directive-followship` at `docs/action_bank.md:450`.

§1.7 hard gate: no semantic hardcode introduced. The runtime does NOT gate any tool dispatch, phase transition, or escalation decision on the slot's value (the non-enforcement integration test T8 in §3.7 verifies this). If the dev finds themselves adding a Java branch on the slot's value, STOP — there is no small-exception path.

## 8. 12-section handoff doc contract

Write `docs/sprints/sprint-031-handoff.md` per `docs/sprint_objective.md` §11. The 12 sections are: (1) Context Pack, (2) Sprint-objective recap, (3) Premise re-verification, (4) Implementation walkthrough, (5) Worked-example smoke rerun, (6) Generalization coverage table, (7) Open questions for human, (8) Anti-hardcode self-walk, (9) Files changed, (10) Layer-classification self-walk, (11) §5 Eval Acceptance bars, (12) Closure verdict placeholder (deliver-agent owned).

## 9. Stop conditions

STOP and report (do NOT silently work around) when:

1. Premise re-verification fails on `SessionManager.java:185–189` or `RoutingResult.ambiguousCandidates()`.
2. Java test regression on the inherited `SystemPromptUserRequestedTiebreakerTest` baseline shifts due to Sprint 31's `system_prompt.txt` edit. Investigate, surface, do not silently adjust.
3. Smoke regression on `composite_score` / safety / grounding / wrong-containment / over-escalation floors.
4. Flyway migration fails on a fresh DB or breaks idempotency on an existing DB.
5. Tempted to widen scope beyond §6 table (add other projection slots, refactor existing slots, touch hard-fenced files).
6. Tempted to extend `UseCaseRouter` for ROUTED-branch alternates — OQ1 pre-pick is binding.
7. Tempted to author CaseSpecs — OQ4 pre-pick defers to Sprint 31+1.
8. Tempted to edit `iteration_governance.md` §7.2 in place — OQ3 pre-pick defers to normal cadence.
9. Tempted to add a runtime gate on the slot's value — §1.7 forbidden; the integration non-enforcement test must hold.

## 10. Working tree at session start (commit-at-end pattern)

Expect uncommitted files at session start:

- `docs/sprint_objective.md` (Sprint 31 contract; deliver-agent-owned).
- `compact/sprint-031-dev-prompt.md`, `compact/sprint-031-review-prompt.md` (deliver-agent-owned).
- `docs/sprints/sprint-030-objective.md` (Sprint 30 archive; deliver-agent-owned).
- `docs/sprints/sprint-030-handoff.md` (Sprint 30 dev handoff with §12 closure verdict filled by deliver-agent at Sprint 30 close).
- `docs/sprints/sprint-029-objective.md` + `docs/sprints/sprint-029-handoff.md` (Sprint 29 archives; deliver-agent-owned).
- `compact/sprint-029-dev-prompt.md` + `compact/sprint-029-review-prompt.md` + `compact/sprint-030-dev-prompt.md` + `compact/sprint-030-review-prompt.md` (deliver-agent-owned planning prompts).
- `docs/proposals/alternate_uc_signal_data_source_design.md` (Sprint 30 design doc; **read this as Sprint 31's source of truth for §6**).
- `docs/10-handoff.md` (deliver-agent §1 lead refresh).
- `docs/action_bank.md` (modified — Sprint 30 close-window R-item registration for `R-alternate-uc-signal-data-source`).
- `csagent_system_design_review.md`, `eval_interactive/case_specs/case_families/sprint29_directive_probe/` — pre-existing unrelated mods or Sprint 29 untracked artefacts. Sprint 31 does NOT touch these.

**Do not stage these.** Stage only your authored §3 files (the 7-row file table) for your final commit. Do not run `git add -A` / `git add .`. The human bundles deliver-agent files at commit time per the commit-at-end pattern.

**Working-tree mod on `system_prompt.txt`:** there is a pre-existing untracked mod on `server/src/main/resources/prompts/system_prompt.txt` (inherited from Sprint 24 onwards per the inherited-failure baseline). Sprint 31's own edit to `system_prompt.txt` adds the teaching paragraph; the pre-existing mod is NOT Sprint 31's. The dev SHALL be careful when staging — stage only the Sprint 31 lines (the new teaching paragraph). If `git diff` shows both Sprint 31's addition AND the pre-existing mod, use `git add -p` to stage only the Sprint 31 hunks. Document the situation in handoff §9 (files changed).

## 11. Final-commit run commands

1. `mvn -q -pl server test` — zero new regressions from current HEAD; new `IntakeAmbiguousCandidatesProjectionTest` (5–8 tests) all pass; inherited failure baseline (`SystemPromptUserRequestedTiebreakerTest`) re-verified — flag if perturbed.
2. `cd eval_interactive && uv run pytest` — zero regressions.
3. `cd eval_interactive && uv run eval-interactive run --set smoke --label sprint-31-alt-uc-slot` — smoke rerun for §10 acceptance verification.
4. The handoff §5 + §11 cite `eval_interactive/results/<run-id>/results.json` for the smoke evidence; jq commands in §6 of this prompt run cleanly against that file.
5. The dev's commit message names the Sprint 31 scope ("sprint 31: alternate_candidate_use_cases projection slot (Option β)"). The deliver-agent bundles supplementary files at close.

If you hit a stop condition, write the handoff up to the stop point and mark later sections "not reached due to <stop condition>". Do not skip silently.
