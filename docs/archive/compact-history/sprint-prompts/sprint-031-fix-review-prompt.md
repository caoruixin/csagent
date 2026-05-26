Paste the content below this line into a fresh Codex session after the fix-iteration #2 commit lands. No PR will be opened; review the fix commit only.

---

You are the Anti-Hardcode + Sprint-Close Review Agent for the Sprint 31 fix iteration #2 (T8 non-enforcement strengthening). This re-review judges:

- Closure of your prior **Finding 2** (T8 too weak), and
- Adherence to the deliver-agent classification: **Finding 1 is out_of_scope_review (accepted)**, NOT addressed in this fix iteration.

The two commits already on the branch are `2c1fd41` (Sprint 31 ship) + `de47635` (§13 fix-iteration #1 evidence append). The fix-iteration #2 commit is the third. Review **only that third commit**.

## 1. Loader

1. `AGENTS.md`.
2. `docs/current/iteration_governance.md` §1 / §3 / §4.1 / §4.2 / §5 / §7.
3. `docs/codex-findings.md` — your prior verdict (`fix_required`, 2 blockers). This re-review judges closure of Finding 2 + acceptance of Finding 1 as out_of_scope.
4. `docs/sprints/sprint-031-objective.md` (archived Sprint 31 objective) §9 (non-enforcement requirement) + §10 (success metrics) + §1.7 anti-hardcode posture.
5. `docs/sprints/sprint-031-handoff.md` §4.7 / §12 / §13 (Sprint 31 ship + fix-iteration #1 evidence).
6. `docs/sprints/sprint-031-fix-handoff.md` (NEW — the dev's fix-iteration #2 handoff).
7. `compact/sprint-031-fix-dev-prompt.md` — what the dev was authorized to do vs what landed.

## 2. §4.1 kernel

Sprint 31 is semantic-touching (`prompt_projection` + `system_prompt.txt` edit). The fix iteration #2 commit touches a **test file only**, no production code. The §4.1 nine-question kernel applies in slim form:

- **Q1 keyword/regex/if-else/per-UC matrix?** — verify the new test scenarios do NOT add per-UC pattern-based decision logic. Slot-value variants in fixtures are acceptable; pattern matching IN the production code under test is NOT introduced (production code unchanged).
- **Q2 Tier-0 justification?** — N/A.
- **Q3 soft signal achievable?** — N/A (no production change).
- **Q4 eval-text encoding?** — verify no CaseSpec id / trace-specific text in the test scenarios.
- **Q5 semantic ownership shift?** — verify NO production code touched.
- **Q6 prompt as if-else dump?** — N/A.
- **Q7 tool / capability / PII / grounding preserved?** — verify YES.
- **Q8 generalization coverage?** — verify the new test scenarios cover the variant table from `compact/sprint-031-fix-dev-prompt.md` §3.1 (≥ 4 new variants; preferably 6 covering null / empty / single-active / single-alternate / multi / unrelated). Confirm the test asserts behaviour INVARIANCE across variants (TerminalOutcome, llm call count, tool-call count, final message text all identical).
- **Q9 rollback / sunset?** — test-only change; trivially reversible.

Expected verdict on the §4.1 kernel: `approve` (the fix-iteration #2 is a pure test-strengthening change; no semantic surface touched in production code).

## 3. Finding 2 closure check (BLOCKING gate)

The strengthened test SHALL demonstrate that the runtime's outputs are invariant under varying slot values. Verify by re-running:

```bash
mvn -q -pl server -Dtest=AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest test
```

The test SHALL pass with ≥ 5 scenario variants exercised. Cross-check the test file contains:

- A `@ParameterizedTest` annotation OR ≥ 4 new `@Test` methods.
- Slot-value variants covering at minimum: null / empty / single-element-matching-active / multi-element / unrelated-UCs.
- Per-variant assertions on: `TerminalOutcome` equality, `llmInvocation.invokeChat` call count equality (or via `verify(llmInvocation, times(N))`), `verifyNoInteractions(toolDispatcher)` (or equivalent zero-tool-call assertion), final user-message text equality, projection-slot value matching the expected per the variant.

**Mental check (BLOCKING):** mentally insert a hypothetical Java branch in `AgentRunLoopImpl` like `if (session.getIntakeAmbiguousCandidates() != null && session.getIntakeAmbiguousCandidates().length > 0) { skipResolve(); }`. Would the strengthened test catch this? If not, the test is still too weak; flag as `fix_required` again.

If the strengthened test would catch the hypothetical regression, Finding 2 is **closed**. Sprint 31 fix iteration #2 verdict on Finding 2: `closed`.

## 4. Finding 1 out_of_scope_review check (NON-BLOCKING acceptance)

The deliver-agent + human have classified Finding 1 (smoke regression) as **out_of_scope_review** per:

- §13 fix-iteration #1 disambiguation evidence (both Sprint-31-cause hypotheses falsifiably REJECTED).
- New R-item `R-llm-provider-latency-drift-2026-05-16` opened to carry the investigation.
- Constitution §1.6 ("Eval is evidence, not authority") — a regression provably not caused by the sprint's changes does not fire the close gate.
- Precedent `feedback_out_of_scope_review_packaging_rollforward.md`.

Verify the fix iteration #2 commit does NOT include any latency/provider/smoke surface change. If the dev silently added a latency-mitigation hack, that's scope drift and BLOCKING. Otherwise, Finding 1 is accepted as out_of_scope and stays carried by the new R-item.

## 5. Scope discipline (BLOCKING)

The fix iteration #2 commit SHALL touch:

- Exactly ONE production-adjacent file: `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java`.
- Exactly ONE NEW docs file: `docs/sprints/sprint-031-fix-handoff.md`.

Anything else in the commit's diff is scope drift. Specifically forbidden:

- No edits to any other test file.
- No edits to any production code (`src/main/`).
- No edits to `system_prompt.txt`, `BotSession.java`, `SessionManager.java`, `ContextProjectionBuilder.java`, or any other Sprint 31 file.
- No edits to `docs/sprints/sprint-031-handoff.md` (the main Sprint 31 handoff). The §13 fix-iteration #1 record is immutable.
- No edits to `docs/action_bank.md` (deliver-agent's at sprint-close).
- No new CaseSpecs.
- No edits to other Sprint 31-related artefacts.

## 6. §4.2 sprint-close header

At the top of `docs/codex-findings.md` (overwrite or append per existing convention):

```
## Sprint Review Decision (fix-iteration #2)
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph: Finding 2 closure verdict + Finding 1 accepted-as-out_of_scope verification>
```

Expected verdict: `pass / blocking_count: 0` if the strengthened test passes the §3 mental check AND the §5 scope discipline holds AND the §4 out_of_scope_review classification holds.

## 7. Final reminders

- Read the fix-iteration #2 commit diff start to end; verify the bundle is test + handoff only.
- The §3 mental check is the single load-bearing gate for Finding 2 closure. If the strengthened test would NOT catch a hypothetical Java branch on the slot, the fix is still too weak; flag again. No third fix iteration is desired; the dev was instructed to over-cover rather than under-cover.
- More than 3 findings on a test-only fix is over-review.
- Do NOT re-litigate Finding 1; it has been classified out_of_scope_review by the deliver-agent + human. If you disagree with the classification, raise it as a follow-on governance concern, NOT a Sprint 31 blocker.
