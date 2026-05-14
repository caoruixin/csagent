---
title: Sprint 26 — Latency decision sprint (consumes Sprint 25 per-LLM-call instrumentation)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-13
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  Sprint 26 is the investigation+decision+conditional-bundle sprint that
  consumes Sprint 25's per-LLM-call latency instrumentation
  (`results.json` `llm_calls` field + `LlmSyntheticBaselineTest` synthetic
  baseline + worked-example DB-grounded comparison at
  `docs/sprints/sprint-025-handoff.md` §5.2). The deliverable is a
  documented decision drawn from (A) widen deadline budget, (B) revert to
  pre-`f2d4cb2` model, (C) accept current latency, (D) change retry /
  backoff, (E) other (surfaced during analysis). Implementation is
  conditional on which option the decision lands at; "accept" is a valid
  outcome if the data does not support action. Sprint 26 ships ZERO new
  instrumentation; ZERO eval-spec edits; ZERO case-family edits; ZERO
  prompt edits; ZERO edits to Sprint 24-landed or Sprint 25-landed code.
  Semantic-touching IF the chosen decision implies a runtime config /
  code change; pure-analysis if the chosen decision is (C) accept. The
  §7 stanza below is multi-layer prospective per-decision-outcome.
---

# Sprint 26 — Latency decision sprint

## 1. Goal

Read Sprint 25's instrumented per-LLM-call latency data + worked-example
pre/post-`f2d4cb2` DB comparison + synthetic baseline; produce a
documented decision drawn from the five options below; bundle the
implementation only if the decision implies action. The decision IS the
primary deliverable; the bundle is conditional.

## 2. Decision options

The dev agent must land on exactly one of:

- **(A) Widen the deadline budget.** Change
  `USER_FACING_LLM_DEADLINE_MS` in
  `server/src/main/java/com/gumtree/csagent/controller/ChatController.java:41`
  (currently 30_000L). If chosen, the sister constants in
  `OpenAiCompatibleLlmClient.java` (connect/read timeouts at lines
  59–60, `MIN_BUDGET_MS_FOR_NEXT_ATTEMPT` at line 115) must be
  re-evaluated against the new budget so the deadline-aware retry loop
  remains internally consistent. The frontend axios timeout
  (`ui/src/api/client.ts`, currently 60s) is the user-facing cap and
  may need adjustment.
- **(B) Revert to the pre-`f2d4cb2` model.** Pre-`f2d4cb2` primary was
  `kimi-k2.6` with `deepseek-v4-flash`-equivalent as fallback. Revert
  via `LlmClientConfig` bean wiring + `LlmProperties` (file path
  `server/src/main/java/com/gumtree/csagent/config/LlmProperties.java`)
  + the env override `DEEPSEEK_MODEL` /
  `application-local.yml:23` if the production target is the model
  string rather than the wiring.
- **(C) Accept the current latency.** No config / code change. The
  decision document explains why the +3.3s p95 widening is acceptable
  given the current UX (Sprint 24 coalesce + honest-next-step is in
  place; second consecutive deadline emits a distinct message offering
  handover, not a stuck-bot placeholder loop) and the cost / risk of
  alternatives.
- **(D) Change retry / backoff behaviour.** The retry surface lives in
  `OpenAiCompatibleLlmClient.java:130–215` (per-attempt
  deadline-aware retry loop, fixed 200ms sleep at lines 189 / 208,
  `maxAttempts` derived from `LlmCallContext.remainingAttempts()`) and
  `FallbackLlmClient.java` (cross-provider hedge). A retry / backoff
  change would re-tune the same constants `f2d4cb2` widened
  (`MIN_BUDGET_MS_FOR_NEXT_ATTEMPT`, the 200ms sleep, the maxAttempts
  cap).
- **(E) Other.** Reserved for an option the analysis surfaces that
  none of (A)–(D) captures. The dev must name (E) precisely and walk
  §3.2 of `iteration_governance.md` to classify the layer before any
  implementation.

## 3. Premise-verification statement

The deliver-agent ran the following premise checks before drafting
this objective; the dev agent should re-verify if any number is
load-bearing for the decision:

- **+3.3s chat p95 widening**: VERIFIED at
  `docs/sprints/sprint-025-handoff.md:354`. Reproducible via the
  `python3` + `psql` recipes at `docs/sprints/sprint-025-handoff.md`
  §5.2 lines 168–326. Source path:
  `eval_interactive/results/20260514-111724/results.json` (chat p95
  9982ms via the `python3 -c 'import json, statistics; ...'` extractor
  at handoff lines 178–186), and `llm_call_log` DB rows for pre /
  post comparison (psql heredoc at handoff lines 277–328).
- **4/14 pass-rate drop**: VERIFIED at
  `docs/sprints/sprint-025-handoff.md:394` — Sprint 25 §7 Q4 flags the
  drop "for visibility, not as a blocker" and explicitly notes "This
  is NOT a Sprint 25 regression — Sprint 25 ships zero semantic code
  changes". This signal is **distinct** from the +3.3s LLM-latency
  signal and Sprint 26 must NOT collapse them (see §6 hard rule).
- **Deadline budget config location**: VERIFIED at
  `server/src/main/java/com/gumtree/csagent/controller/ChatController.java:41`
  (`USER_FACING_LLM_DEADLINE_MS = 30_000L`; two callsites at lines 74
  and 109). NOT in `application.yml` / `application-local.yml`; NOT a
  `@Value` binding. Sister timeouts at
  `server/src/main/java/com/gumtree/csagent/service/llm/OpenAiCompatibleLlmClient.java:59–60`
  (`connectTimeout=3000`, `readTimeout=12000`) and line 115
  (`MIN_BUDGET_MS_FOR_NEXT_ATTEMPT = 3000L + 12000L + 200L = 15200L`).
- **Model config location**: VERIFIED at
  `server/src/main/java/com/gumtree/csagent/config/LlmProperties.java:25`
  (`deepseek-v4-pro` default) +
  `server/src/main/resources/application-local.yml:23`
  (`DEEPSEEK_MODEL` env override). The `f2d4cb2` commit message
  declares `.env.local` carries `DEEPSEEK_MODEL=deepseek-v4-flash`
  (uncommitted file; the verification dev should `cat .env.local`
  to confirm the current runtime model string before reasoning about
  the revert path).
- **Retry / backoff config location**: VERIFIED at
  `server/src/main/java/com/gumtree/csagent/service/llm/OpenAiCompatibleLlmClient.java:185–208`
  (fixed 200ms `sleepQuietly(200)` between retries; `maxAttempts`
  derived from `LlmCallContext.remainingAttempts()` at line 129) and
  `FallbackLlmClient.java` (cross-provider hedge). Sprint §C1 +
  Sprint 8.1 §M1 are the commit-message-cited prior contracts.
- **R-item shape**: No Sprint 26-specific R-item exists in
  `docs/action_bank.md` §5.2. The Sprint 25 close entry (line 616 +
  line 652) explicitly states: "The +3.3s widening decision (widen
  budget, revert model, accept, change retry/backoff) belongs to a
  future sprint that consumes Sprint 25's instrumentation". Sprint 26
  is that sprint; its scope is declared by this objective, not
  inherited from a prior R-item.
- **Sprint 24 coalesce + honest-next-step is in place**: VERIFIED at
  `server/src/main/java/com/gumtree/csagent/model/BotSession.java:79–81`
  (`consecutive_deadline_count` field) and
  `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:684–799`
  (`DEADLINE_EXCEEDED` branch with reset hook + threshold-gated
  distinct honest-next-step message). The user-visible UX of
  consecutive deadlines is no longer two identical placeholders.

If the dev's own re-verification of any of these surfaces a material
discrepancy, STOP and report (Sprint 22 / 23 / 24 / 25 planning-turn
premise-check pattern).

## 4. Reproducibility rule (hard requirement)

Every quantitative claim in the Sprint 26 decision document and
handoff — every latency number, every percentile, every pass-rate
claim, every cost / token estimate — MUST cite source path AND
extraction command (`jq` filter, `python3 -c`, `psql` query, or the
literal phrase "manual eyeball over <path>"). This is the same
reproducibility bar Sprint 25's fix iteration (commit `c8b8c85`)
landed and codex-fix-review at
`docs/sprints/sprint-025-fix-codex-review.md` confirmed pass on. It
applies even more strictly here because Sprint 26's whole deliverable
IS quantitative reasoning.

Memory reference:
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

## 5. Decision criteria

The dev must weigh at least the following dimensions in the analysis
and explicitly address each in the handoff §6 (decision-rationale
section):

1. **User-visible UX with Sprint 24 coalesce in place.** The
   deadline-widening question is no longer "do users see a stuck bot
   on consecutive deadlines" (Sprint 24 fixed that). The new question
   is "is the bot's actual problem-solving completion rate hurt by
   the deadline cutting calls short". Cite the rate from the smoke
   `results.json` `llm_calls` field — count of
   `success==false AND failureClass=="deadline_exceeded"` (or the
   equivalent failure-class string emitted by `LlmCallLogger`) per
   call total — and reason from that, not from the placeholder
   emission count.
2. **Pass-rate signal interpretation.** The 4/14 pass-rate drop noted
   in Sprint 25 §7 Q4 is a **separate signal** from the +3.3s
   LLM-latency widening. It may be latency-driven (calls cut short →
   bot gives up → case fails), model-quality-driven (newer model
   regresses on a semantic task), or run-to-run variance (Sprint 25
   ran ONE smoke; n=14 is small). The dev must walk all three
   hypotheses in handoff §5 and state which evidence (per-case
   transcript inspection, per-call `failureClass` distribution, prior
   smoke runs at `eval_interactive/results/*/`) supports which
   hypothesis. **Conflating them ("post-`f2d4cb2` is worse therefore
   revert") is a §1.7 forbidden line — do not act on a collapsed
   signal.**
3. **Cost trade-offs.** Widening the deadline budget costs
   wall-clock per request and may compound with downstream features
   (handover orchestrator, multi-turn flows). Model revert costs are
   different (provider quota, per-token pricing differences,
   prior-model intermittent `engine_overloaded_error` per
   `f2d4cb2` commit message). The handoff should sketch the cost
   side even if back-of-envelope; if no usable cost data exists, the
   handoff must say so explicitly, not invent numbers.
4. **Reversibility.** Each option has a different cost to undo. (A)
   and (D) are config constants and trivially reversible. (B) is
   bean-wiring and also reversible but touches the
   `LlmConfigValidator` startup contract. (C) is the no-change
   default and reversible by definition. The decision should weight
   reversibility as a tiebreaker, not the primary criterion.
5. **Compounding effects on downstream features.** The handover
   orchestrator design at
   `docs/proposals/handover_orchestrator_design.md` and the
   `R-cs040-uc-k-topic-subject-routing` n=1 conditional R-item at
   `docs/action_bank.md:614` could be downstream consumers of any
   deadline-budget widening. Flag if so.

## 6. Hard fences (eleven do-not-touch surfaces)

Sprint 26 MUST NOT:

1. **Add any new instrumentation.** Sprint 25 produced the data;
   Sprint 26 reads what exists. If the analysis surfaces a data gap,
   the dev proposes a follow-on R-item — does NOT add instrumentation
   in this sprint.
2. **Edit eval-spec surfaces** —
   `eval_interactive/case_specs/*`, `eval_interactive/case_spec_overrides.yaml`,
   `eval_interactive/personas/*`, judge rubric, judge prompts.
3. **Edit case-family surfaces** — `eval_interactive/case_families/*`
   (Sprint 20 cascade fence).
4. **Edit prompt surfaces** —
   `server/src/main/resources/prompts/system_prompt.txt` (Sprint 23
   teaching paragraph stands; no edit). The unauthored uncommitted
   `system_prompt.txt` working-tree mod inherited from Sprint 24 / 25
   is NOT this sprint's scope.
5. **Edit Sprint 24-landed code** —
   `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
   (`consecutiveDeadlineCount` field) +
   `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
   reset hook + DEADLINE_EXCEEDED threshold-gated branch.
6. **Edit Sprint 25-landed code** —
   `eval_interactive/eval_interactive/batch/executor.py` `llm_calls`
   enrichment; `eval_interactive/eval_interactive/simulator/agent_client.py`
   `get_llm_calls`; `server/src/test/java/com/gumtree/csagent/service/runtime/LlmSyntheticBaselineTest.java`.
7. **Edit foundational docs** — `docs/foundational/*`.
8. **Edit governance docs** — `docs/current/iteration_governance.md`,
   `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`.
9. **Edit sprint archives** —
   `docs/sprints/sprint-001-objective.md` through
   `docs/sprints/sprint-025-*.md`.
10. **Add or modify any Tier-0 invariant.** The
    `docs/runtime_freeze_and_risk_policy.md` Tier-0 set is not in
    scope.
11. **Widen the eval rubric or any CaseSpec** to mask a real bot
    mistake (§1.7 forbidden line).

## 7. §1.7 forbidden-line guardrail

- Do **not** widen the eval rubric or any CaseSpec to make a
  pass-rate signal look better. The 4/14 drop is a real signal; if
  the analysis concludes the drop is run-to-run variance, that is a
  conclusion to be defended with evidence, not papered over.
- Do **not** treat the +3.3s widening and the 4/14 pass-rate drop as
  the same signal. They are different. Conflating them and
  recommending revert without evidence segregation is an §1.7 forbidden
  collapse.
- Do **not** act on a quantitative claim that is not reproducible
  per §4 above. If a number cannot be cited with source + extraction
  command, it does not exist for purposes of this decision.

## 8. Bundle-or-defer policy (decision is primary, bundle conditional)

- The **decision document** (Sprint 26 handoff) is the primary
  deliverable. It exists whether or not the dev chooses to bundle an
  implementation.
- If the decision is **(C) accept**, the bundle is the decision
  document only. The handoff §6 explains the rationale; §11 names any
  deferred R-items (e.g. a follow-on instrumentation gap surfaced
  during analysis).
- If the decision is **(A) / (B) / (D) / (E)**, the bundle MAY
  include a config / code edit if it is **reversible**, **passes the
  §1.7 hard gate** (no semantic hardcode), and **comes with a
  regression test** that demonstrates the changed behaviour. If the
  edit cannot be paired with a regression test, the bundle is deferred
  to a follow-on sprint and Sprint 26 ships only the decision
  document + the deferred R-item proposal.

## 9. Files in scope per decision outcome (preliminary)

The dev refines this list in handoff §8 based on which decision is
chosen.

- Always:
  - `docs/sprint_objective.md` (this file — appended only if a fix
    iteration is required).
  - `docs/sprints/sprint-026-handoff.md` (new; 12-section contract per
    Sprint 24 / 25 precedent).
  - `docs/codex-findings.md` (Codex writes the sprint-close header
    here).
  - `docs/action_bank.md` §5.2 (append any new R-items the analysis
    proposes; close any R-items the decision resolves; note any
    n=1 open observations).
  - `docs/10-handoff.md` (refresh the lead to Sprint 26 at close).
- Decision (A) — widen deadline budget:
  - `server/src/main/java/com/gumtree/csagent/controller/ChatController.java`
    (the constant + any updated comment narrative).
  - Possibly
    `server/src/main/java/com/gumtree/csagent/service/llm/OpenAiCompatibleLlmClient.java`
    (sister constants `connectTimeout`, `readTimeout`,
    `MIN_BUDGET_MS_FOR_NEXT_ATTEMPT`).
  - Possibly `ui/src/api/client.ts` (axios timeout).
  - A new regression test file (e.g.
    `server/src/test/java/.../DeadlineBudgetTest.java`) demonstrating
    the new budget behaviour.
- Decision (B) — revert model:
  - `server/src/main/java/com/gumtree/csagent/config/LlmProperties.java`
    OR `server/src/main/java/com/gumtree/csagent/config/LlmClientConfig.java`
    (bean wiring) OR `.env.local` (uncommitted; out of git scope —
    the dev would document the runtime expectation, not commit the
    env file).
  - `server/src/main/java/com/gumtree/csagent/config/LlmConfigValidator.java`
    (fatal-key flip if wiring changes).
  - A new regression test file demonstrating the wiring + validator
    behaviour.
- Decision (C) — accept:
  - No code edits beyond the handoff and action_bank.
- Decision (D) — change retry / backoff:
  - `server/src/main/java/com/gumtree/csagent/service/llm/OpenAiCompatibleLlmClient.java`
    (the retry sleep + maxAttempts cap).
  - Possibly `FallbackLlmClient.java`.
  - A new regression test file.
- Decision (E) — other:
  - Out of scope to enumerate; the dev names the file set when (E)
    is declared.

## 10. Do not implement

- No new instrumentation in `eval_interactive/`, `server/`, or `ui/`.
- No eval-spec / case-family / prompt / judge edit.
- No edit to Sprint 24-landed code (`BotSession.consecutiveDeadlineCount`,
  `PhaseEvaluator` reset hook + DEADLINE_EXCEEDED branch).
- No edit to Sprint 25-landed code (`executor.py` `llm_calls`
  enrichment, `agent_client.py` `get_llm_calls`,
  `LlmSyntheticBaselineTest.java`).
- No Tier-0 invariant add / modify.
- No foundational / governance / archive doc edits.
- No rubric widening or CaseSpec edit to mask a real bot mistake.
- No decision without reproducible quantitative evidence per §4.
- No collapsing of latency + pass-rate signals per §7.

## 11. Layer-classification + anti-hardcode stanza (multi-layer prospective per-decision-outcome)

**Target failure layer:** multi-layer prospective; the dev agent walks
`iteration_governance.md` §3.2 first-match-wins per decision outcome
and identifies the post-hoc layer in handoff §9. The candidate set
gated by the chosen decision:

- Decision (A) widen deadline budget → **`infra`** (§3.2 Q1 / Q2 walk).
  The constant is in the request-handling controller; longer deadlines
  are semantic-adjacent because the longer wall-clock interacts with
  Sprint 24's coalesce trigger (the count of consecutive
  DEADLINE_EXCEEDED outcomes is reduced if fewer calls hit deadline).
  No §3.2 Q3 / Q5 / Q6 / Q7 surface is touched.
- Decision (B) revert model → **`infra`** + product-policy adjacent
  (§3.2 Q1 walk; the prior `f2d4cb2` flipped on a product-policy
  concern that the smaller `moonshot-v1-32k` "surfaces new
  instruction-following gaps we shouldn't be debugging right now",
  per the commit message). A revert would re-introduce that surface.
- Decision (C) accept current latency → **no layer change.** No Tier-0,
  no semantic hardcode, no decision artefact beyond the handoff +
  action_bank.
- Decision (D) change retry / backoff → **`infra`** (§3.2 Q1 walk; the
  retry surface is part of the Runtime-owned §1.4 "budget / timeout"
  surface).
- Decision (E) other → the dev declares the layer when (E) is named;
  if it falls outside §3.1's nine layers, the answer is
  `human_review_required` per §3.2 default tail.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The
`docs/runtime_freeze_and_risk_policy.md` §1 / §2 hard-invariant set is
not touched by any of (A)–(D); (E) would require the dev to walk §3.2
Q2 and the answer is `human_review_required` if a new Tier-0 candidate
emerges.

**Semantic hardcode:** No semantic hardcode introduced under any of
(A)–(D) — none of the candidate edits adds a keyword / regex / if-else
/ enum / per-UC matrix. All are config / wiring constants on the §1.4
Runtime-owned "tool schema, capability / permission boundary, PII and
safety floor, grounding floor, budget / timeout, idempotency,
persistence, trace and eval contract" surface. (E) would be walked
separately.

**Generalization coverage:**

- Target = the post-`f2d4cb2` smoke run
  `eval_interactive/results/20260514-111724/results.json` (the data
  the decision is read from). If the decision implies an edit, the
  bundle ships a regression test demonstrating the changed behaviour
  on the Sprint 24 / 25 code path it touches.
- Neighbor = the full `server/` JUnit suite remains green (modulo the
  pre-existing inherited `SystemPromptUserRequestedTiebreakerTest`
  failure inherited from the unauthored `system_prompt.txt`
  working-tree mod, per Sprint 24 §3.4 / Sprint 25 §11).
- Negative = the deferred-G2 surface; not in scope.
- Shadow = deferred-G2 surface; not in scope.

## 12. Success metrics

A successful Sprint 26 close requires all of:

- Decision is named explicitly in handoff §6 (one of (A)–(E)).
- Decision-rationale walks all five §5 dimensions and the §6 hard
  rules (UX with coalesce in place; latency / pass-rate signal
  segregation; cost; reversibility; downstream compounding).
- Every quantitative claim cites source path + extraction command per
  §4.
- If bundled action: a regression test demonstrates the changed
  behaviour, the §11 stanza's chosen branch holds post-hoc, and full
  `server/` JUnit suite remains green (modulo the documented
  pre-existing inherited failure).
- If no action: the "accept" rationale is explicit and any deferred
  R-items are named in action_bank §5.2.
- Codex sprint-close review at `docs/codex-findings.md` returns
  `decision: pass` OR Codex returns `decision: fix_required` /
  `out_of_scope_review` and the close-out is judged by the deliver
  agent's standard A / B / C / D classification.

## 13. Review-agent guidance (handed off to the §4.1 + §4.2 reviewer)

Codex runs the §4.1 anti-hardcode kernel from
`docs/current/iteration_governance.md` against this sprint and writes
the §4.2 sprint-close header to `docs/codex-findings.md`. Sprint 26 is
NOT exempt from §4.1 (it is semantic-touching if a runtime config /
code edit is bundled; pure-analysis-only if the decision is (C)
accept). The deliver-agent's review prompt enumerates the
sprint-specific deviations; reviewer follows that prompt.
