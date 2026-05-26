Paste the content below this line into a fresh Claude Code session on branch `refactor/remove-the-shackles`.

---

You are the dev agent for the Sprint 31 fix iteration #2 (T8 non-enforcement strengthening). Codex returned `fix_required / blocking_count: 2` post-close. The deliver-agent + human have classified the findings:

- **Finding 1 (smoke regression)** → **out_of_scope_review** — accepted per §13 fix-iteration evidence (cold-start race + teaching paragraph both falsifiably REJECTED; external LLM provider drift dominates per +84% mean elapsed_ms widening). New R-item `R-llm-provider-latency-drift-2026-05-16` carries the investigation. **You do NOT touch this in fix-iteration #2.**
- **Finding 2 (T8 too weak)** → **fix_required** — you fix this one. Single file. Bounded.

This is fix-iteration #2 on Sprint 31. (Fix-iteration #1 was the §13 disambiguation walk; it landed at commit `de47635` and added no code.) The two commits already on the branch are `2c1fd41` (Sprint 31 ship) + `de47635` (§13 fix-iteration append). Your commit becomes the third.

## 1. Loader

1. `AGENTS.md` (transitively loads `iteration_governance.md` §1 §3 §5 §7 + `doc_governance.md` + `agent_context_guide.md`).
2. `docs/codex-findings.md` — both findings in full. Your scope is **Finding 2 only**. Finding 1 is classified out_of_scope; do not touch any latency / smoke / provider surface.
3. `docs/sprints/sprint-031-handoff.md` §4.7 (regression tests as shipped), §7.1 / §13 (fix-iteration #1 evidence), §12 (existing closure verdict — will be appended at fix-iteration #2 close).
4. `docs/sprint_objective.md` (the archived Sprint 31 objective at `docs/sprints/sprint-031-objective.md`) §6 last row + §9 §1.7-non-enforcement requirement.
5. `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java` — the existing T8 file, 173 lines, ONE test. **This is what you strengthen.**
6. `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest.java` — the Sprint 20 precedent T8 shape the new test should mirror. Read it before writing.

## 2. Premise re-verification

Spot-check at session start:

- `AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java` exists at the cited path; current line count is 173; the file contains exactly ONE `@Test` method (`populatedAlternateSlot_appearsInProjection_andRuntimeDoesNotShortCircuit`).
- `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest.java` exists and has a parameterized / multi-scenario structure.
- `grep -n "intakeAmbiguousCandidates" server/src/main/java/` returns only the field declaration on `BotSession.java`, the AMBIGUOUS-branch capture in `SessionManager.java`, and the projection emission in `ContextProjectionBuilder.java` (NO Java decision-path consumption). If any new consumer of the field has appeared since commit `de47635`, STOP — that would invalidate the non-enforcement guarantee Sprint 31 closed on.

## 3. The work — strengthen T8

The goal is to demonstrate **behaviour invariance** under varying slot values, not just that the slot appears in the projection on one happy-path scenario. Add parameterized tests so a future regression like `if alternate_candidate_use_cases.contains("UC-C") then short-circuit X` would fail the test suite.

### 3.1 New test scenarios (REQUIRED)

Add ≥ 4 new scenarios alongside the existing happy-path test. Acceptable structures:

- **Preferred:** convert the existing test to a JUnit 5 parameterized test (`@ParameterizedTest` + `@MethodSource` or `@ValueSource`) with the variants below.
- **Acceptable fallback:** keep the existing test and add ≥ 4 separate `@Test` methods.

Slot-value variants the test SHALL cover:

| variant | `intakeAmbiguousCandidates` field on BotSession | expected projection slot value |
|---------|--------------------------------------------------|---------------------------------|
| **V1 — null (ROUTED-style)** | `null` (Lombok default) | empty array |
| **V2 — empty array** | `new String[0]` | empty array |
| **V3 — single-element matching active** | `new String[]{"UC-A"}` (active = UC-A) | empty array (active filtered out) |
| **V4 — single-element alternate** | `new String[]{"UC-C"}` (active = UC-A) | `["UC-C"]` |
| **V5 — multi-element** | `new String[]{"UC-A", "UC-C", "UC-D"}` (active = UC-A) | `["UC-C", "UC-D"]` |
| **V6 — unrelated UCs only** | `new String[]{"UC-F", "UC-G"}` (active = UC-A) | `["UC-F", "UC-G"]` |

(Variants V1–V3 should produce empty projection slot; V4–V6 should produce non-empty. All variants should reach **identical** runtime outcomes — same `TerminalOutcome`, same LLM-mock invocation pattern, same tool-call count, same final answer text.)

The existing happy-path scenario covers (something close to) V4; you may collapse V4 into the parameterized version OR keep it as the existing canonical happy-path test if that's cleaner. Document the choice in the handoff.

### 3.2 Invariance assertion (REQUIRED)

For each variant, the test SHALL assert:

1. **Projection slot value** matches the expected (per the table above).
2. **TerminalOutcome** is identical across all variants (all reach `FINAL_ANSWER` per the LLM mock's stubbed no-tool-call response).
3. **`llmInvocation.invokeChat` call count** is identical across all variants (1 call each — the mock returns a final answer in one shot).
4. **`toolDispatcher` is NEVER invoked** across all variants (the mock returns no tool calls). Use `verifyNoInteractions(toolDispatcher)` after each run.
5. **Final user message** text is identical across all variants (the mock returns the same `userMessage` regardless of slot value).

If any variant produces a different outcome / tool-call count / message than the others, the runtime IS branching on the slot's value and the test correctly fails — which is the non-enforcement guarantee the §1.7 anti-hardcode posture requires.

### 3.3 Test naming + structure discipline

- Test method name(s) should make the invariance intent explicit. Suggested: `runtimeBehaviorIsInvariantToSlotValue` (parameterized) OR `behaviorUnchanged_*` per variant.
- Keep mock setup in `@BeforeEach`; don't duplicate per scenario. Only the `BotSession` field and the variant label change per case.
- The existing imports + `setUp()` should mostly carry through; you add the parameterized scenarios on top.

### 3.4 No other changes

- Do NOT touch any code under `server/src/main/java/`.
- Do NOT touch any other test file.
- Do NOT touch `system_prompt.txt`, the projection builder, the session manager, or any business-logic file.
- Do NOT author CaseSpecs.
- Do NOT touch `docs/action_bank.md`, `docs/proposals/`, `docs/sprints/sprint-031-handoff.md` (that's the deliver-agent's at close — see §6).
- Do NOT touch the §13 fix-iteration evidence on the handoff — it stays as the disambiguation record.

## 4. Acceptance

- The new tests compile and pass: `mvn -q -pl server -Dtest=AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest test` → all variants PASS.
- Full server suite still passes with the inherited 1-failure baseline (`SystemPromptUserRequestedTiebreakerTest` per Sprint 24+; Sprint 31 introduced no new perturbation): `mvn -q -pl server test` → `912 / 1-inherited / 0 / 2` or equivalent. Surface any delta in handoff §3.
- The strengthened test would detect a hypothetical Java branch on the slot's value. Mental check: imagine someone added `if (session.getIntakeAmbiguousCandidates() != null) { ... different behaviour ... }` in `AgentRunLoopImpl` — would your test catch it? If not, strengthen the assertions until it would.

## 5. Stop conditions

STOP and report (do NOT silently work around) when:

1. The premise check finds a new Java consumer of `intakeAmbiguousCandidates` (something has been added since `de47635`). The non-enforcement guarantee is broken upstream of your fix; surface in handoff §3 + §11.
2. You find yourself needing to change `AgentRunLoopImpl` or any other production code to make the new tests work. The whole point of the non-enforcement guarantee is that no production code consults the slot; if a test variant fails because of a production behaviour difference, that IS the regression Codex worried about — surface it.
3. You're tempted to widen scope (latency-drift investigation, CaseSpec authoring, other Codex findings). The classification was explicit: Finding 1 is out_of_scope; only Finding 2 in scope.
4. The full server suite shows a NEW regression (not the inherited 1-failure baseline). Investigate; do not silently adjust the test.

## 6. Handoff document

Write `docs/sprints/sprint-031-fix-handoff.md` (NEW file, mirroring the Sprint 20 fix-iteration `sprint-020-fix-handoff.md` shape — see that file for the structure precedent). Sections:

1. **Context** — what fix-iteration #2 is for; pointer to Codex Finding 2 + the deliver-agent classification.
2. **Premise re-verification** — confirm §2 spot-checks.
3. **Implementation walkthrough** — the parameterized test structure, the 6 variants, the 5 invariance assertions, the design choice (parameterized vs separate `@Test` methods).
4. **Verification** — the test command + literal output showing all variants PASS; the full suite output showing the inherited baseline is preserved.
5. **Anti-hardcode self-walk** — confirm the strengthened test would catch a hypothetical Java branch (the §4 mental check).
6. **Files changed** — exactly ONE file: the strengthened T8 test.
7. **Open questions for human** — none expected; if any, name them.
8. **Closure verdict placeholder** — leave for human + deliver-agent.

Length budget: 4–8K. Mirror Sprint 20 fix-handoff's terseness.

## 7. Working tree at session start

Expect uncommitted deliver-agent files:

- `docs/sprint_objective.md` (current — for whatever sprint is currently in flight after Sprint 31; treat as out-of-scope context).
- `docs/sprints/sprint-031-objective.md` (Sprint 31 archive, deliver-agent-owned).
- `docs/sprints/sprint-031-handoff.md` (Sprint 31 main handoff with §13 fix-iteration #1 already appended).
- `compact/sprint-031-fix-dev-prompt.md`, `compact/sprint-031-fix-review-prompt.md` (this prompt + the matching review prompt).
- Other deliver-agent / pre-existing unrelated mods.

**Do not stage these.** Stage only the strengthened test file + the new `docs/sprints/sprint-031-fix-handoff.md`. The human bundles deliver-agent files at commit time.

## 8. Final-commit run commands

1. `mvn -q -pl server -Dtest=AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest test` — all variants pass.
2. `mvn -q -pl server test` — full suite, inherited-baseline preserved.
3. The handoff §4 cites both outputs verbatim with reproducibility.
4. Commit message: `sprint 31 fix-iteration #2: strengthen T8 non-enforcement test (parameterized across 6 slot variants)` or equivalent.

If you hit a stop condition, write the handoff up to the stop point and mark later sections "not reached due to <stop condition>". Do not skip silently.
