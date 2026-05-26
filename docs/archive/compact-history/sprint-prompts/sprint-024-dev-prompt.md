# Sprint 24 Dev Prompt — Slow-LLM Placeholder Coalesce + Coarse Latency Proxy (Two-Track)

Paste the content below this line into a fresh Claude Code session on branch `design-v1-without-human-review`. Working tree at session-start carries deliver-agent-owned files — see §13.

---

You are the dev agent on Sprint 24, a two-track sprint:

- **Track A:** deterministic UX repair on the cross-turn slow-LLM placeholder loop (`PhaseEvaluator.java` DEADLINE_EXCEEDED branch + new session-scope `consecutive_deadline_count` mirroring V12 `runtime_error_count`).
- **Track B:** honest coarse-proxy latency baseline (investigation-only, no code) + propose a follow-on instrumentation R-item.

UX-over-pass-rate framing: primary bar is the user-visible behaviour repair on the placeholder loop, not eval pass-rate movement.

## 1. Loader stanza — read in this order

1. `AGENTS.md`.
2. `docs/current/doc_governance.md`.
3. `docs/current/agent_context_guide.md` (Context Pack Prompt + the "Runtime, phase machine, drift" reading list; also "Eval, governance" list for Track B).
4. `docs/current/iteration_governance.md` §1, §3, §5, §7.
5. `docs/sprint_objective.md` — Sprint 24 scope (authoritative).
6. `docs/sprints/sprint-023-handoff.md` §3 / §4 (track-precedent walks) + §11 (R-items Sprint 24 closes).
7. `docs/sprints/sprint-019-handoff.md` §3 (cs_002/cs_011/cs_014/cs_038/cs_040/cs_066 per-case walks — placeholder-loop target set, behaviour-level) + §11.
8. Runtime files cited in `docs/sprint_objective.md` §2 (premise verification facts; do NOT re-verify, but DO read them once before editing).

Produce a Context Pack per `agent_context_guide.md` before editing any code.

## 2. UX-over-pass-rate framing

Primary success bar is the regression test's behaviour assertion: two consecutive `DEADLINE_EXCEEDED` outcomes emit 1 placeholder + 1 distinct honest message, NOT 2 identical placeholders. Pass-rate effects on the smoke set are secondary; do NOT optimise eval pass-rate at the expense of the UX repair shape.

## 3. Track A — narrow UX repair

### 3.1 Target set (re-derive)

Walk the placeholder-loop target set from `eval_interactive/results/20260510-134558/results.json`. Sprint 19 §3.7 named cs_002/cs_011/cs_014/cs_038/cs_040; Sprint 23 §4.1 re-derived (cs_011 has no placeholder — semantic_planner shape; cs_066 has the loop surface attributed differently in §3.6). Treat the *behaviour shape* (two consecutive `DEADLINE_EXCEEDED`) as target, NOT a CaseSpec list. Regression test is behaviour-level; do not encode CaseSpec ids in the runtime fix.

### 3.2 Implementation (single bundle)

1. **`BotSession.java`** — add `@Column(name = "consecutive_deadline_count", nullable = false)` Integer field with `@Builder.Default = 0`, mirroring `runtime_error_count` lines 75–77.

2. **`SessionManager.java`** — add `consecutiveDeadlineCount(0)` to the session builder, mirroring line 107.

3. **`PhaseEvaluator.java`**:
   - **Reset hook** in outcome-dispatch prologue (mirror lines 675–682 ERROR-counter reset shape): reset `consecutive_deadline_count` to 0 on any outcome that is NOT `DEADLINE_EXCEEDED`.
   - **Increment + threshold check** in `DEADLINE_EXCEEDED` branch (today lines 754–770): increment counter; if new value `< 2`, emit existing placeholder text; if `>= 2`, emit distinct honest next-step text. Mirror lines 738–752 (ERROR branch shape).
   - **Distinct honest next-step text** on 2nd consecutive deadline — constraints per `docs/sprint_objective.md` §3.1 (re-read; the §7 / §1.7 gates below restate them).
   - Transition tag remains `agent_deadline_exceeded` for both cases; bot does NOT auto-call `request_handover`.

4. **Database migration** — if persistence layer (Flyway / equivalent) requires schema migration for the new column, ship the SQL alongside.

5. **`LLM_UNAVAILABLE` branch** — leave unchanged. Track A scope is `DEADLINE_EXCEEDED` only; `LLM_UNAVAILABLE` coalesce would be scope expansion. If you believe it should coalesce too, defer to an action_bank R-item; do NOT bundle.

### 3.3 Track A regression test

Write a JUnit test under `server/src/test/java/.../service/runtime/` exercising the cross-turn deadline shape via the `PhaseEvaluator` surface. The test must assert:

- 1st `DEADLINE_EXCEEDED` outcome → user-facing message is the existing placeholder text.
- 2nd consecutive `DEADLINE_EXCEEDED` outcome → user-facing message is **NOT** the placeholder (inequality assertion) AND contains a next-step intent (one of "handover" / "retry" / "alternative" / equivalent; assert a property, not a literal LLM-relevant content match).
- Reset assertion: between two `DEADLINE_EXCEEDED` outcomes, a non-deadline outcome (e.g. `FINAL_ANSWER` or `CLARIFICATION_NEEDED`) resets the counter; a subsequent `DEADLINE_EXCEEDED` emits the existing placeholder again.

Run the full server unit-test suite (`mvn -pl server -am test` or equivalent). Must remain green.

## 4. Track B — coarse-proxy latency baseline (investigation-only)

### 4.1 Read-only data extraction

Read case-level `elapsed_ms` and total turns from `eval_interactive/results/20260505-235231/results.json` (pre-`f2d4cb2`) and `eval_interactive/results/20260510-134558/results.json` (post-`f2d4cb2`). Compute case-level p50 + p95 per run. Planning turn already extracted: pre p50 ≈ 10.4s / p95 ≈ 24.6s; post p50 ≈ 10.5s / p95 ≈ 27.6s (105 + 27 case-turns). Verify or accept with citation.

### 4.2 Output (handoff Track B section)

`docs/sprints/sprint-024-handoff.md` Track B section must contain ALL of:

1. **Coarse-proxy data table** — pre/post p50 + p95 with n (case-turn count) called out.

2. **Observed p95 increase** — report the ≈3-second p95 widening (≈24.6s → ≈27.6s) explicitly.

3. **No-per-call-claim paragraph** — verbatim in substance: *"Per-LLM-call latency is NOT in `eval_interactive/results/*/results.json` at the per-turn granularity needed to verify the Sprint 19 §3.7 hypothesis directly. Case-level `elapsed_ms` conflates LLM round-trip, tool dispatch, persistence, and retry overhead. The pre/post p95 widening is a signal that something changed; it is NOT evidence of per-LLM-call latency widening. A deadline-budget widening or model-revert decision made on this signal alone would not be justified."*

4. **Follow-on R-item proposal** — name `R-per-llm-call-latency-instrumentation` (or finalise a slug; deliver agent applies on close). Layer: `infra` / eval-harness. Scope: instrument per-turn LLM round-trip timing in `LlmInvocationService.invokeChat` OR surface the existing log-emitted timing into a structured field on results.json; collect baseline from a smoke rerun; compare pre / post `f2d4cb2`. **Prerequisite to any future deadline-budget or model-revert decision.**

### 4.3 Track B fences

READ-ONLY on `eval_interactive/results/**`. No instrumentation shipped this sprint (R-item proposes; future sprint builds). No deadline budget widen, no model config edit, no `prompt_projection` / eval-spec touch.

## 5. Scope fences

The six hard fences in `docs/sprint_objective.md` §6.2 apply to BOTH tracks. Re-read before editing.

## 6. Explicit out-of-scope items

Per `docs/sprint_objective.md` §6.1 / §10. Key items:

- **`cs_040` UC-K → UC-C routing** — Sprint 24 fixes the *placeholder shape* on cs_040 (and any case with the placeholder-loop surface); it does NOT touch routing. `R-cs040-uc-k-topic-subject-routing` (`docs/action_bank.md:614`) stays conditional.
- **`ChatController.java:125`** — different surface from the cross-turn loop; fires only when deadline propagates past `AgentRunLoopImpl`. Out-of-scope. If you find evidence it is user-visible on the loop shape, defer to an action_bank R-item; do not bundle.

## 7. §1.7 hard gate — honest next-step message

The 2nd-deadline message MUST: name what is happening; offer an actionable next step (handover offer / retry suggestion / alternative channel); NOT repeat the placeholder byte-for-byte; NOT branch on user content (no keyword/regex/if-else/enum/per-UC matrix; trigger is *event-shape* count of consecutive `DEADLINE_EXCEEDED` outcomes); NOT branch on UC (one message, uniformly applied at threshold); NOT contain visible-eval CaseSpec text or trace-specific phrasing.

These are §1.7 forbidden-list applications; a violation makes Track A reject-as-semantic-hardcode.

## 8. Handoff doc contract

Write `docs/sprints/sprint-024-handoff.md` with the 12-section shape (per Sprint 19 / Sprint 23 precedent):

1. Context Pack.
2. Sprint-objective recap.
3. Track A — implementation + regression test as evidence (behaviour-level; no per-CaseSpec walk).
4. Track B — coarse-proxy data table + no-per-call-claim paragraph + R-item proposal.
5. Files changed.
6. Layer-classification self-walk (per-track, per §3 of `iteration_governance.md`).
7. Anti-hardcode self-walk (§4.1, 9 questions, per-track).
8. Generalization-coverage table (per `docs/sprint_objective.md` §5).
9. Sprint-objective-met check — per-bullet PASS / PARTIAL / GAP against §§1, 3, 4, 5, 6, 8.
10. Open questions for human.
11. Action-bank deltas (proposed; deliver agent applies on close). Cover the four R-item rows in `docs/sprint_objective.md` §9.
12. Next recommended action.

§12 may use the standard "human + deliver agent will fill on close" placeholder for the closure_verdict row.

## 9. Files in scope (authorised)

- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
- `server/src/main/resources/db/migration/V*__*.sql` (new Flyway migration if needed)
- `server/src/test/java/.../service/runtime/` (new regression test file)
- `docs/sprints/sprint-024-handoff.md` (new file)
- READ-ONLY: `eval_interactive/results/20260505-235231/results.json`, `eval_interactive/results/20260510-134558/results.json`

## 10. Files NOT in scope

See `docs/sprint_objective.md` §10 for the full list (key items: `ChatController.java`, `system_prompt.txt`, `eval_interactive/case_specs/**`, `case_spec_overrides.yaml`, prompt_projection modules, `runtime_freeze_and_risk_policy.md`, model/budget config, `AlreadyCalledPromptConsumptionTest.java`, `docs/sprints/*`, `docs/archive/*`).

## 11. Stop conditions

STOP and surface to human if: premise facts in `docs/sprint_objective.md` §2 no longer hold; Track A's regression test genuinely requires touching a fenced file; you are tempted to widen deadline budget, change model config, edit `prompt_projection`, edit eval-spec, claim per-call latency from `elapsed_ms`, edit `ChatController.java`, or fix cs_040 routing; you discover a fact that contradicts the sprint scope and the right answer would be to narrow / split / defer.

## 12. Bundle policy

- **Track A bundles as a single commit at end** (commit-at-end workflow). All Track A files + handoff + R-item deltas in the close commit.
- **Track B is investigation-only.** No code; handoff Track B section is the deliverable.

## 13. Working tree at session start

Deliver-agent-owned files are uncommitted at session start: `docs/sprint_objective.md` (Sprint 24 objective), `compact/sprint-024-*.md`, `compact/sprint-deliver-orchestrator.md`. Pre-existing working-tree mods (`csagent_system_design_review.md`, `server/.../prompts/system_prompt.txt`) are also not yours. **Do not stage these; human bundles at commit time.** For your close-commit, stage ONLY files you authored under §9; avoid `git add -A` / `git add .`.

Now produce the Context Pack and proceed.
