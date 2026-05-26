Paste the content below this line into a fresh Claude Code session on branch `refactor/remove-the-shackles`.

---

You are the dev agent for Sprint 33 — the first sub-sprint of Milestone M1 (DISCOVER + Intake) per the 2026-05-16 governance upgrade to the milestone framework (`docs/current/iteration_governance.md` §8).

Sprint 33 addresses the Alice bad case D1 dimension (UC-A vs UC-H DISCOVER mis-classification) by adding a soft-signal projection slot for REMOVED-listing + Ad Support topic disambiguation, plus DISCOVER `systemInstruction` principle-level teaching, plus a sibling `system_prompt.txt` teaching paragraph. This is `prompt_projection`-only; no Java decision-path branch, no classifier touch, no `INTAKE_UCS` change.

**Codex review:** deferred to M1 milestone-shared close per `iteration_governance.md` §4.3 default. You do NOT dispatch Codex at Sprint 33 close. Deliver-agent + human dispatch at M1 close against the cumulative Sprint 33 + Sprint 34 (+ 35 + 36 if shipped) commit range.

## 1. Loader

1. `AGENTS.md` (transitively loads `iteration_governance.md` §1 / §3 / §4 / §5 / §7 / **§8 NEW Milestone framework** + `doc_governance.md` + `agent_context_guide.md`).
2. `docs/milestone_objective.md` — the M1 milestone north star. Read for context on what M1's other sub-sprints will do and how Sprint 33's scope fits.
3. `docs/sprint_objective.md` — your Sprint 33 contract. §5 file table; §6 hard fences; §8 §7 stanza; §9 success metrics; §10 stop conditions.
4. `docs/sprints/sprint-031-handoff.md` §14 + `docs/sprints/sprint-031-fix-handoff.md` — Sprint 31 + fix-iteration #2 context on the soft-signal-projection + parameterised-invariance-test pattern that Sprint 33 mirrors.
5. `docs/sprints/sprint-032-handoff.md` §13 — Sprint 32 in-flight downgrade context on the Option β coverage gap. Sprint 33's DISCOVER signal does NOT depend on Option β; it's a different soft-signal source (intake-time `listing_context.status` + `topic_subject` observation, not intake-AMBIGUOUS routing alternates).
6. `docs/current/iteration_governance.md` §1.3 (LLM owns drift/topic-shift/escalation posture/response strategy — the classification decision belongs to the LLM), §1.7 (forbidden list — no keyword/regex/per-UC matrix), §3.2 Q3 (prompt_projection layer rationale), §4.1 (anti-hardcode kernel that Codex will walk at M1 close), §5.5/§5.6 (smoke demoted to observation; bad-case suite as new primary gate), §7 (sprint-objective stanza requirement).
7. `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` — the target bad case. Read the `closure_criterion` field carefully; Sprint 33 alone may not fully close Alice (D2 + D3 are Sprint 34 + Sprint 36 scope), so closure-criterion progress = IMPROVING is acceptable.
8. `eval_interactive/case_specs/bad_cases/_manifest.md` — the bad-case suite convention.
9. `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` — read the existing projection slot pattern (especially the Sprint 31 `alternate_candidate_use_cases` slot adjacent to lines 380-415). You add your new `discover_disambiguation_signals` slot in the same pattern.
10. `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` lines 411-431 — the existing DISCOVER `systemInstruction` building block with Sprint 7 §I0 weak-candidate cue. You extend it with one principle-level paragraph for ad-status UCs.
11. `server/src/main/resources/prompts/system_prompt.txt` lines 23-34 — the existing `already_called` (Sprint 23, lines 23-28) and `alternate_candidate_use_cases` (Sprint 31, lines 30-34) teaching paragraphs. Sprint 33 adds a sibling paragraph adjacent to these.
12. `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistry.java` (or wherever `getCandidateUcsForTopic` lives) — confirm the API; you'll consume it from `ContextProjectionBuilder` for the disambiguation candidate list.
13. `server/src/test/java/com/gumtree/csagent/service/runtime/IntakeAmbiguousCandidatesProjectionTest.java` — Sprint 31 projection regression test. Mirror its shape for `DiscoverDisambiguationSignalsProjectionTest`.
14. `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java` (post-fix-iteration #2 version) — Sprint 31 fix-iteration #2 parameterised invariance test. Mirror its shape for `AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest`.

## 2. Premise re-verification

Spot-check at session start (HEAD post-Sprint-32-close):

1. **`PhaseEvaluator.java:411-431` DISCOVER `systemInstruction`** — confirm it has the Sprint 7 §I0 weak-candidate cue (UC-F payment / UC-B advertising / UC-C messaging / UC-D account-login) and has NO UC-A vs UC-H disambiguation. If a prior agent added UC-A vs UC-H guidance, STOP and surface.
2. **`ContextProjectionBuilder.java` projection slots** — confirm via `grep -n 'projection.set\|projection.put' server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`. The list should include `candidate_use_cases`, `alternate_candidate_use_cases` (Sprint 31), `already_called`, `intake_state`. NO `discover_disambiguation_signals`.
3. **`UseCaseRegistry.getCandidateUcsForTopic(topic_subject)`** — confirm the API signature and that "Ad Support" → `[UC-A, UC-B, UC-FP, UC-H]` candidate list. Run a quick smoke check via the existing `RoutingResult` AMBIGUOUS path in `UseCaseRouter.java`.
4. **`BotSession.listingContext`** — confirm the `status` field is one of LIVE / PROCESSING / REMOVED / SUSPENDED / EXPIRED. Confirm via `grep -nE 'enum.*Status\|REMOVED' server/src/main/java/com/gumtree/csagent/`.
5. **`system_prompt.txt:23-34`** — confirm the two existing teaching paragraphs (already_called + alternate_candidate_use_cases). Sprint 33's new paragraph goes adjacent (probably lines 36+).
6. **Bad-case suite Alice case loads** — `cd eval_interactive && uv run python -c "from eval_interactive.case_spec.loader import load_case_specs; specs = load_case_specs('case_specs/bad_cases'); print([s.case_id for s in specs])"` should return `['alice_uc_a_uc_h_misclass']`.

If any premise drifts, STOP and surface in handoff §3.

## 3. The work

### 3.1 New projection slot `discover_disambiguation_signals`

Path: `ContextProjectionBuilder.java`, adjacent to the existing `alternate_candidate_use_cases` slot.

Shape: an `ObjectNode` (NOT array; the slot may carry multiple sub-fields in the future without schema breaks):

```json
"discover_disambiguation_signals": {
  "ad_status_observed": "REMOVED" | "SUSPENDED" | "EXPIRED" | null,
  "topic_subject_carries_multiple_candidate_ucs": true | false,
  "candidate_ucs_for_topic": ["UC-A", "UC-B", "UC-FP", "UC-H"] | []
}
```

Fire conditions (additive, all must hold for the slot to be "populated" — but the slot is ALWAYS present, schema-stable):

- `session.getListingContext()` is non-null AND `getStatus()` is one of `{REMOVED, SUSPENDED, EXPIRED}` (states where the listing is not visible to the user) — populates `ad_status_observed`.
- `session.getFormTopicSubject()` is non-null AND `useCaseRegistry.getCandidateUcsForTopic(topic).size() > 1` — populates `topic_subject_carries_multiple_candidate_ucs: true` and the candidate list.

Empty / null-default values when conditions don't fire. Schema-stable across turns per `ContextProjectionBuilder` convention.

### 3.2 DISCOVER `systemInstruction` extension at `PhaseEvaluator.java:411-431`

Add ONE paragraph at the end of the existing DISCOVER systemInstruction text. Suggested wording (you may refine; keep principle-level):

```
Additional disambiguation cue (read alongside the candidate-UC list and discover_disambiguation_signals projection): when the projection shows the user's listing is in a not-visible state (REMOVED / SUSPENDED / EXPIRED) AND the form topic_subject carries multiple plausible UCs (e.g., "Ad Support" → UC-A visibility, UC-FP ad-support deletion, UC-H appeal), the user's literal request is the disambiguation signal. A user asking "why can't I see my ad" or "what happened to my ad" or "where did my ad go" wants to KNOW the reason (UC-A FAQ-resolvable). A user asking "I want to appeal the removal" or "this removal was unfair" wants to APPEAL (UC-H intake). If the user's request is ambiguous between the two, ask ONE focused clarifying question on this turn before committing classify_use_case — for example: "Do you want to know the reason it was removed, or do you want to appeal the removal?" Do not commit UC-H purely on listing_context.status alone; the user's stated need is the disambiguation signal, not the listing's database row.
```

**NO regex on user content. NO if-else rule dump. Principle-level guidance.**

### 3.3 New `system_prompt.txt` teaching paragraph

Adjacent to the existing `already_called` + `alternate_candidate_use_cases` paragraphs (lines 23-34). Suggested wording (you may refine; keep mirror to Sprint 23 + Sprint 31 shape):

```
discover_disambiguation_signals (introduced 2026-05-16, Sprint 33):
when the projection contains discover_disambiguation_signals with
ad_status_observed populated AND topic_subject_carries_multiple_candidate_ucs
= true, the runtime is surfacing observable evidence that the user's
session is in a state where multiple UCs are plausibly responsive.
This slot is OBSERVABLE EVIDENCE the LLM may use to inform
classify_use_case (e.g., to ask one clarifying question before
committing), but the runtime does NOT enforce or branch on the
slot value. You own the read decision. Empty fields are the common
case (most sessions are unambiguous).
```

### 3.4 Regression test #1: `DiscoverDisambiguationSignalsProjectionTest`

Mirror `IntakeAmbiguousCandidatesProjectionTest` (Sprint 31) shape. 4-6 tests:

- T1: REMOVED listing + "Ad Support" topic → slot populated (`ad_status_observed: REMOVED`, candidate list contains UC-A/UC-FP/UC-H).
- T2: LIVE listing + "Ad Support" topic → slot fields empty (`ad_status_observed: null`).
- T3: REMOVED listing + single-candidate topic (e.g., topic that maps to UC-A only) → `topic_subject_carries_multiple_candidate_ucs: false`.
- T4: null listing_context (form without ad_id) → all slot fields empty.
- T5: Schema stability — slot is always present in projection, even when empty.
- T6 (optional): SUSPENDED + EXPIRED variants.

### 3.5 Regression test #2: `AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest`

Mirror the Sprint 31 fix-iteration #2 T8 parameterised invariance test shape. 4-6 variants × 5 invariance bars:

Variants:
- V1: REMOVED + Ad Support (signal fires; mock LLM commits to UC-A)
- V2: REMOVED + Ad Support (signal fires; mock LLM commits to UC-H)
- V3: LIVE + Ad Support (signal does not fire; mock LLM commits to UC-A)
- V4: REMOVED + single-candidate topic (signal partial; mock LLM commits)
- V5: null listing_context (signal does not fire; mock LLM commits)
- V6 (optional): SUSPENDED + Ad Support

Invariance bars per variant (these MUST be identical across V1-V6 to prove runtime non-enforcement):
1. `TerminalOutcome` reaches the mock LLM's stubbed terminal state (FINAL_ANSWER or whatever the mock returns).
2. `llmInvocation.invokeChat` call count is identical (verify via `verify(llmInvocation, times(N))`).
3. `toolDispatcher` interactions are identical (mock LLM stubs the same tool sequence across variants).
4. Final user-message text is identical (the mock LLM stubs the same canned response).
5. Projection slot value matches the per-variant expected value (the projection slot ITSELF varies per variant; the runtime DECISIONS do not).

A hypothetical future Java branch like `if discover_disambiguation_signals.ad_status_observed == "REMOVED" then force-clarify-question` would diverge on bars 1/2/3/4 between V1/V2 and V3/V4/V5; the test would fail. This is the §1.7 anti-hardcode proof.

## 4. Verification

### 4.1 Targeted test runs

```bash
mvn -q -pl server -Dtest=DiscoverDisambiguationSignalsProjectionTest test
mvn -q -pl server -Dtest=AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest test
```

Both SHALL pass cleanly.

### 4.2 Full server suite baseline preservation

```bash
mvn -q -pl server test
```

Expected: `Tests run: <917 + new test count>, Failures: 1, Errors: 0, Skipped: 2`. The +1 inherited failure is `SystemPromptUserRequestedTiebreakerTest` from the Sprint 24-era unauthored `system_prompt.txt:60` mod. Sprint 33's edit to `system_prompt.txt` is appending a new paragraph (not editing line 60); the inherited failure should persist unchanged. Surface any unexpected delta in handoff §4.

### 4.3 Bad-case suite Alice rerun (primary gate per §5.6)

```bash
cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/
```

Read the resulting `results.json` per-turn trace for `alice_uc_a_uc_h_misclass`. Manually evaluate against the case's `closure_criterion` field:

- (a) Bot routes to UC-A handling + grounded FAQ answer about removal reason / appeal path? → PASS (full closure unlikely from Sprint 33 alone since D2 intake prefill is Sprint 34 scope).
- (b) Bot asks ONE focused clarifying question on turn 2 and routes per user's answer? → PASS or IMPROVING (this is exactly what Sprint 33's DISCOVER teaching is designed to enable).
- (c) Bot acknowledges UC-H but gracefully escalates with reason=user_requested? → IMPROVING (better than dead loop).
- FAIL conditions: bot still enters 7-turn loop OR fabricates removal reason OR calls request_handover(intake_complete_for_uc_h) with empty fields.

Document the closure-criterion status in handoff §5 with the per-turn trace evidence. IMPROVING is acceptable for Sprint 33 close; FULL CLOSURE is the M1 milestone-close goal.

### 4.4 Smoke rerun (optional, observation only per §5.5)

```bash
cd eval_interactive && uv run eval-interactive run --path case_specs/smoke
```

NOT a gate at Sprint 33 close per §5.5 governance update. Run only if you want a sanity check. If you observe a > 10% regression beyond noise, surface in handoff §7 as informational (NOT as a Sprint 33 close blocker).

## 5. Stop conditions

See `docs/sprint_objective.md` §10 — 9 conditions. Specifically watch for:

- Premise drift on §4 / §2 items.
- Temptation to add a Java decision-path branch on `listing_context.status` (this IS the §1.7 forbidden pattern).
- Temptation to touch `RuntimeIntentClassifier` / `UseCaseRouter` / `DriftDetector` / `ClassifyUseCaseTool` / `INTAKE_UCS` / `escalation_reason` enum.
- Java test regression > inherited baseline.
- Bad-case suite Alice case shows FAIL (acceptable to remain at IMPROVING; FAIL means Sprint 33 alone definitely won't help, surface for M1 reassessment).

## 6. Files you stage and commit

Stage:

- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (new slot)
- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (DISCOVER systemInstruction extension)
- `server/src/main/resources/prompts/system_prompt.txt` (new teaching paragraph — APPEND; do NOT edit lines 23-34)
- `server/src/test/java/com/gumtree/csagent/service/runtime/DiscoverDisambiguationSignalsProjectionTest.java` (new)
- `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest.java` (new)
- `docs/sprints/sprint-033-handoff.md` (new; 12-section archive per §11 of `docs/sprint_objective.md`)

Do NOT stage:

- `docs/sprint_objective.md` (deliver-agent owned; deliver-agent archives at M1 milestone close)
- `docs/milestone_objective.md` (deliver-agent owned; stays stable across M1 sub-sprints)
- `docs/10-handoff.md` (deliver-agent owned)
- `docs/action_bank.md` (deliver-agent owned; flips happen at M1 milestone close per §4.3 default, NOT per sub-sprint)
- `docs/codex-findings.md` (Codex-owned; UNCHANGED at Sprint 33 close because Codex review is deferred to M1)
- `compact/sprint-033-*-prompt.md` (deliver-agent owned)
- `compact/M1-review-prompt.md` (deliver-agent will draft at M1 close)
- `eval_interactive/case_specs/bad_cases/` files (deliver-agent + human owned)
- Any working-tree mock-data files that pre-existed at session start (deliver-agent context — unrelated to Sprint 33)

Commit message shape (mirror Sprint 31 / Sprint 32):

```
sprint 33: DISCOVER UC-A/FP/H soft-signal + classification guidance (M1 sub-sprint 1)

First sub-sprint of Milestone M1 (DISCOVER + Intake) per
docs/milestone_objective.md. Addresses Alice bad case D1 dimension
(UC-A vs UC-H DISCOVER mis-classification) via:
- new ContextProjectionBuilder slot discover_disambiguation_signals
- PhaseEvaluator DISCOVER systemInstruction principle-level extension
  for ad-status UC disambiguation
- system_prompt.txt teaching paragraph (sibling to already_called,
  alternate_candidate_use_cases)

ZERO RuntimeIntentClassifier / UseCaseRouter / DriftDetector /
ClassifyUseCaseTool / INTAKE_UCS / escalation_reason enum change.
ZERO regex / keyword / per-UC matrix in Java. ZERO Tier-0 invariant.

New tests: DiscoverDisambiguationSignalsProjectionTest (N tests),
AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest
(M variants × 5 invariance bars per Sprint 31 fix-iteration #2 T8
precedent).

Bad-case suite Alice rerun: <PASS / FAIL / IMPROVING with evidence>.

Codex review deferred to M1 milestone-shared close per
iteration_governance.md §4.3 default.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

## 7. Handoff document

Write `docs/sprints/sprint-033-handoff.md` per the 12-section contract in `docs/sprint_objective.md` §11. Key sections:

- **§5 Bad-case suite rerun:** the primary signal; per-turn trace evidence; closure-criterion status with sub-bullet detail.
- **§7 Open questions for M1:** what should Sprint 34 / 35 / 36 focus on given Sprint 33's evidence.
- **§8 Anti-hardcode self-walk:** §4.1 nine-question kernel; expected verdict `approve`.
- **§12 Closure verdict placeholder:** leave for M1 milestone-shared decision (this sub-sprint doesn't close standalone).

## 8. Final check before commit

- [ ] §5 production code changes land in one commit; test files in same commit.
- [ ] No `RuntimeIntentClassifier` / `UseCaseRouter` / `DriftDetector` / `ClassifyUseCaseTool` files in the staged commit.
- [ ] No `IntakeFieldExtractor.java` / `INTAKE_UCS` / `escalation_reason` enum touch.
- [ ] No regex / keyword / per-UC matrix in production code or prompt.
- [ ] `system_prompt.txt` edit is APPEND only (existing lines 23-34 unchanged).
- [ ] Bad-case suite Alice case run produces a documented closure-criterion status in handoff §5.
- [ ] Java baseline preserved (917 + new test count / 1-inherited / 0 / 2).
- [ ] Anti-hardcode self-walk in handoff §8 with §4.1 nine questions.
- [ ] No deliver-agent-owned files in commit.
- [ ] No Codex review dispatch (deferred to M1 milestone close).

Commit and surface to the deliver-agent for M1 sub-sprint 2 (Sprint 34) planning.
