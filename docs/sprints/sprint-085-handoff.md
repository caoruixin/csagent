---
title: Sprint 085 / S-Auto-30 handoff — CS3 DISCOVER synthesised-null-turn provenance gate
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java
last_reviewed: 2026-06-08
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  M-Auto-7 autoloop launch blocker #2. A runtime-synthesised DISCOVER
  placeholder (injected by ActionParser when the LLM emits an empty
  user_message + no tool_calls) was incrementing the R2.a clarification
  counter, burning the clarification budget and force-escalating the next
  turn before the LLM saw the user's reply (CS3 session 33edc1eb, turn 2).
  Fix = a structural provenance flag (ParsedAction.userMessageSynthesised)
  set only at the ActionParser null-turn fallback; the R2.a counter excludes
  synthesised turns. NOT a content heuristic — the "structural cardinality
  only" design is preserved. Plus one §1.3-soft discover_triage.yaml cue.
  §7-REQUIRED stanza in sprint_objective. Per-sub-sprint Codex EXEMPT (folds
  into the M-Auto-7 milestone-shared review). One staging note in §11 (the
  PhaseEvaluatorSkillIntegrationTest golden DISCOVER string required a
  sanctioned update for the authorized skill-content change — a test file,
  within the contract's "test file(s)" stage set). No real-LLM re-bless in
  S-X; counter-honesty verified at the pre-pilot re-bless (post-S-Y1, §12).
---

# Sprint 085 / S-Auto-30 — CS3 synthesised-null-turn provenance gate

## §0 — Cold-start evidence table

| Item | Result |
|------|--------|
| **Root cause (trace-confirmed)** | CS3 session `33edc1eb` (`eval_interactive/results/20260607-095759`, case `iwzx_uc_k_advert_on_hold_restore`). Turn-2 LLM raw = `{"user_message":"","tool_calls":[]}` (true null turn). `ActionParser.java:70-72` substitutes the placeholder `"I'm looking into this for you."` AT PARSE TIME, so `AgentRunLoopImpl:411` sees a NON-BLANK `userMsg`; the R2.a counter (`isDiscoverFreeTextClarification` requires only non-blank) incremented on the runtime placeholder (count 1→2). Turn-3 pre-LLM budget gate then force-escalated `clarification_budget_exhausted` before the LLM saw the user's "Thank you." |
| **Why the proposal's Option C3.A was inert** | C3.A keyed off `userMsg.isBlank()`, but `userMsg` is NEVER blank at the loop (ActionParser already substituted). The fix uses a STRUCTURAL PROVENANCE flag, not `userMsg.isBlank()` and not a content heuristic. |
| **Layer (§3.2)** | `infra` (R2.a counter provenance) + `prompt_projection` (complementary soft DISCOVER cue). |
| **Scope file 1 — `ParsedAction.java`** | Added `private boolean userMessageSynthesised;` (Lombok `@Data @Builder` → `isUserMessageSynthesised()` + builder method; primitive default `false`). All 24 call sites use the builder; no direct all-args constructor usage, so adding a field is safe. |
| **Scope file 2 — `ActionParser.java`** | `parse(...)` tracks `boolean synthesised` and sets it `true` ONLY at the `:70-72` null-turn fallback; builder now carries `.userMessageSynthesised(synthesised)`. `buildFallback()` (parse-failure handover apology) UNTOUCHED → its flag stays `false` (a different case). Placeholder string unchanged. |
| **Scope file 3 — `AgentRunLoopImpl.java:455-471`** | Counter increment now gated by `&& !action.isUserMessageSynthesised()`. Existing structural guards (`isDiscoverFreeTextClarification(plan.phase(), false, ucCommittedThisTurn, userMsg)`) intact. No content gate added (the `isClarificationMessage(finalText)` routing at the next line is UNCHANGED). |
| **Scope file 4 — trace diagnostic** | One-line `log.info` ("user_message_synthesised=true (runtime placeholder) … excluded from clarification counter") on synthesised turns, on the existing per-turn log surface (least-invasive existing mechanism; no per-turn structured diagnostic surface exists in the loop today). |
| **Scope file 5 — `discover_triage.yaml`** | ONE §1.3-soft sentence added near the Sprint-33 cue: every DISCOVER turn should call `classify_use_case` or ask one focused clarifying question; do not emit an empty `user_message` / empty `tool_calls` / placeholder filler. Soft cue only; no Java enforcement. |
| **Test #1** — ActionParser: blank `user_message` + no `tool_calls` → placeholder text AND `isUserMessageSynthesised()==true` (+ whitespace-blank variant; + 3 negative controls: genuine clarification, tool-call response, parse-failure handover all `false`). | PASS (`ActionParserTest`, +5) |
| **Test #2** — AgentRunLoop: synthesised placeholder turn → `clarificationCount` NOT incremented (stays 0). | PASS |
| **Test #3** — AgentRunLoop: genuine `?`-clarification (non-blank, not synthesised) → `clarificationCount` increments to 1 (anti-误杀). | PASS |
| **Test #4** — AgentRunLoop: non-synthesised non-blank declarative reply → structural counting unchanged (increments to 1, content-agnostic). | PASS |
| **Test #5** — CS3 replay shape (turn-1 genuine clarification → count 1; turn-2 null turn → count STAYS 1, not 2); `BudgetChecker(count=1, cap=2)` → `Optional.empty()` (no premature force-escalate; turn-3 reaches the LLM). | PASS |
| **Test #6** — anti-误杀: two genuine clarifications → count reaches cap=2; `BudgetChecker(count=2, cap=2)` → `Optional.of("max-clarification-rounds")` (still escalates; structural guard not weakened). | PASS |
| **New test class** | `server/.../service/runtime/AgentRunLoopSynthesisedNullTurnClarificationTest.java` (5 tests, #2-#6) + 5 tests added to `ActionParserTest` (#1). |
| **Focused gates** | `ActionParserTest` 20/0/0/0 · `AgentRunLoopSynthesisedNullTurnClarificationTest` 5/0/0/0 · `DiscoverClarificationCounterTest` 8/0/0/0 (regression, untouched) · `AgentRunLoopClarificationDetectionTest` 5/0/0/0 (regression, untouched) · `PhaseEvaluatorSkillIntegrationTest` 13/0/0/0 (golden updated, see §11). |
| **Full Java module suite** | `mvn -o test` → **1373 run / 1 failure / 0 errors / 2 skipped**. |
| **Delta vs post-S-A baseline `1363 / 1 / 0 / 2`** | **+10 run** = 5 new `ActionParserTest` + 5 new `AgentRunLoopSynthesisedNullTurnClarificationTest`. Failures/errors/skipped unchanged. **No new regression.** |
| **Sole failure** | `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` — the documented inherited OQ-S41.5 prompt tiebreaker test. Provably uncoupled: this sub-sprint touches no system-prompt tiebreaker code. |
| **Hard fences honored** | `control-policy.yaml` (`max-clarification-rounds: 2`) untouched; placeholder string untouched; pre-LLM budget gate (`ControlKernel.java:288-318`) untouched; no `isClarificationMessage`/content/keyword gate on the counter; RESOLVE-intake bucket untouched; no Java guard on empty `user_message`. |
| **§7 stanza** | §7-REQUIRED — present in `docs/sprint_objective.md`. |
| **Codex** | Per-sub-sprint EXEMPT; folds into M-Auto-7 milestone-shared close review (see §11). Codex NOT dispatched. |

## §11 — Codex deferral + open questions

**Codex (§4.3):** per-sub-sprint Codex review is DEFERRED to the M-Auto-7
milestone-shared close review. No §4.3 trigger fires: not a Tier-0 candidate
(adds no Tier-0 invariant); not §1.7-adjacent (the fix is a STRUCTURAL
provenance flag + a §1.3-soft cue — no keyword/regex/enum/per-UC matrix); not
a hard-fence violation; not a fix-iteration. Codex was NOT dispatched.

**OQ-S85.1 — staging note: golden snapshot test required a sanctioned update.**
The contract (§3.6) named the stage set as `ParsedAction.java`,
`ActionParser.java`, `AgentRunLoopImpl.java`, `discover_triage.yaml`, + test
file(s). The authorized soft-cue edit to `discover_triage.yaml` changed the
composed DISCOVER `systemInstruction`, which `PhaseEvaluatorSkillIntegrationTest`
pins to a GOLDEN string (`DISCOVER_SYSTEM_INSTRUCTION`). Three golden tests
failed transiently
(`discover_nullActiveUc_composesGoldenPhasePlan`,
`discover_anyActiveUc_…`, `discover_intakeUc_stillComposesGoldenPhasePlan`).
The test's own class doc explicitly authorizes this: *"Future Skill content
changes (Sprint 39+) MAY change these expectations."* The golden DISCOVER
string was updated to mirror the yaml cue verbatim; all 13 now pass. This is
a **test file**, so it is within the contract's "test file(s)" stage set — no
production/config file beyond the named four was touched. Flagged for
visibility, not a scope breach.

**OQ-S85.2 — counter-honesty effect is loop-local; the budget-gate consequence
is pinned by `BudgetChecker`, not by an end-to-end ControlKernel run.** The fix
lives entirely in the COUNT (`AgentRunLoopImpl`); the GATE
(`ControlKernel` step 3 → `BudgetChecker`) is unchanged. Tests #5/#6 drive the
real `AgentRunLoopImpl` for the count and assert the gate consequence with the
real `BudgetChecker` (count `>=` cap → `max-clarification-rounds`). The full
multi-turn ControlKernel orchestration ("turn-3 actually reaches the LLM
without a force-escalate") is the downstream consequence of count `<` cap,
pinned indirectly; it is exercised end-to-end at the §12 re-bless, not by a
mocked-LLM test (per §5.7 the mock controls the measured variable).

## §12 — Re-bless deferred to post-S-Y1 (no real-LLM evidence in S-X)

Per the sprint contract, **no real-LLM re-bless runs in S-X**. The change is
verified by Java/unit tests + diff inspection only. The behavioural effect —
that the CS3 DISCOVER stall no longer false-counts a clarification round and
no longer prematurely force-escalates — is an eval-evidence claim that, per
§5.7 (mocked-LLM tests cannot be primary evidence that a change altered LLM
behaviour, since the mock controls the measured variable), is confirmed only
at the **pre-pilot baseline re-bless after S-Y1** (batched re-bless cadence,
deliver-agent + human). The mocked-LLM tests above cover the provenance-flag
wiring and the counter exclusion only.

Expected re-bless signal (forensic, for the post-S-Y1 reviewer): on the CS3
anchor family (DISCOVER null-turn stalls), the `clarification_budget_exhausted`
force-escalate that fired on the turn AFTER a runtime placeholder should
disappear; genuine over-clarification escalations (cap honestly reached) must
remain. A re-bless that shows fewer DISCOVER `clarification_budget_exhausted`
escalations WITHOUT a rise in over-clarification stalls is the pass signal.
