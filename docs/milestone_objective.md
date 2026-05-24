---
title: Milestone M5 — Observability Coherence
doc_tier: current-runtime
status: current
implementation_status: partial
source_of_truth: this file
last_reviewed: 2026-05-24
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  Approved by human 2026-05-24. Fifth milestone under the
  iteration_governance.md §8 framework; second cleanup/coherence-flavored
  milestone (after M4-Eval-Cleanup) — regression-safety acceptance bar, not
  bad-case-closure-anchored. Path 1 research-driven, consuming
  docs/solutions/observability_coherence_admin_trace_and_projection.md (the
  whole-solution proposal) + docs/solutions/eval_report_coherence_proposal.md
  (the S1 detail proposal). Human scope decisions 2026-05-24: (1) full M5 = all
  three observability problems; (2) S1 = Alternative B (remove legacy Phase-5
  dashboard rendering + foundational fold-back), keeping the dashboard's
  aggregates in results.json (display-only removal) and keeping + reframing the
  per-UC rollup; (3) add a per-sub-sprint Codex review at S3 close. Hard
  ordering constraint: S2 before S3
  (per-invocation projection must be visible in the trace before the projection
  audit can judge which fields are load-bearing). S1 is independent (eval-harness
  Python; zero server) and sequenced first as the fast, zero-bot-risk win.
---

# Milestone objective — M5 Observability Coherence

**One-paragraph thesis.** Three observability surfaces — the eval `report.html`,
the admin trace's per-turn LLM Raw Response, and the LLM Projected Context — have
fallen behind the agent's architecture. M2 (Skill Registry abstraction) and
M3-Eval (the four-tier evaluation pyramid) reshaped the scoring / runtime / plan
layers, but the display layer and the trace layer did not follow. The result is a
compounding observability debt: a human opening `report.html` on a human-judgment
suite sees a misleading "0/12 passed"; a human opening the admin trace for a
multi-step turn sees only the LAST of N LLM invocations; and the per-turn
projection still carries the pre-M2 UC-driven field set plus duplication and
noise. M5 brings all three back into coherence. It changes *display and audit*
surfaces — **not bot behaviour** — with the single exception of S3's projection
convergence, which must change *what is projected* without changing *what
semantic information is available to the LLM* (verified by a real-LLM bad-case
rerun showing no regression).

## 1. Milestone class (layer breakdown + §7 coverage)

| Sub-sprint | Problem | Fix layer (§3.1) | §7 stanza | Semantic-touching? |
|---|---|---|---|---|
| **S1** Eval Report Coherence | #1 | `infra` (eval-harness display) + foundational docs fold-back | **exempt** (display + docs-only) | No |
| **S2** Per-invocation trace | #2 | `infra` (observability / trace contract, §1.4 Runtime-owned) | **exempt** (pure observation; carries a clarifying stanza because it touches server) | No |
| **S3** Projection audit + Skill-driven convergence | #3 | `prompt_projection` (§3.2 Q3) | **REQUIRED** | **Yes** |
| **S4** (conditional) Projection dedup / denoise | #3 | `prompt_projection` | **REQUIRED** | **Yes** |

The milestone is semantic-touching **only via S3 (+S4)**. S1 and S2 are display /
observation and add no semantic decision. S3/S4 carry the §7 stanza and are gated
by a real-LLM bad-case rerun (mocked-LLM tests cover wiring only — §5.6 eval
evidence gate).

## 2. Goal (architectural outcome, user/observer-facing)

- **#1 / S1**: a human opening `report.html` sees the M3-Eval four-tier verdict
  (Tier-0 safety / Tier-1 outcome / Tier-2 critical-flow / Tier-3 advisory) and a
  prominent `suite_authority` badge (human_review vs programmatic), **not** a
  pre-M3 7-metric dashboard whose "0/12 passed" headline contradicts the
  human-judgment verdict in `_manifest.md`.
- **#2 / S2**: a human opening the admin trace for a multi-step turn (FAQ Skill,
  `max_tool_steps: 4`) sees **every** LLM invocation with its **own** projected
  context + raw response + tool calls + latency — not just the last invocation's
  (the current `BotTurn` single-column overwrite).
- **#3 / S3**: the projection面 is audited against (a) the eval trace contract
  validator, (b) drift/task-history reconstruction, (c) admin-trace rendering,
  and (d) Skill declarations — then converged to be **Skill-driven** where M2 left
  it UC-driven, **without** changing the semantic information available to the
  LLM. Convergence is registry/Skill-driven (no per-UC if-else).

## 3. Sub-sprint sequence (preliminary; deliver-agent refines per round)

> Hard ordering: **S1 independent (sequenced first)** → **S2 before S3** → S3 →
> S4 (conditional on S3 findings). S1 touches the eval-harness Python + a
> foundational doc; S2/S3/S4 touch server Java (+ UI for S2). No two sub-sprints
> share an edited surface, so a §8.5 "scope crosses unrelated surface" split is
> not triggered.

### S1 — Eval Report Coherence (Sprint 50) — `infra` + docs fold-back — §7-exempt
Re-align `report.html` with the M3-Eval four-tier pyramid and the M4
`case_passed_authority` annotation by building a four-tier verdict surface
(Tier-0/1/2/3 sections + `suite_authority` header + per-case tier-labelled
badges + a `_render_tier2` that surfaces `tier2_result`). Per the human's
**Alternative B** choice, the legacy Phase-5 §6.9 7-metric dashboard *rendering*
is removed (its computed aggregates stay in `results.json` for backward-compat —
display stripped, data preserved) and `phase5_evaluation_design.md §6.9` gets a
minimal, intentional foundational fold-back marking it superseded by the
four-tier pyramid. Zero `server/` touch, zero scoring change, zero fixture
change; consumes `R-eval-report-observability`. **Depends on:** nothing.

### S2 — Per-invocation trace (B2 full persistence) (Sprint 51) — `infra` — §7-exempt
Promote per-invocation LLM data to a first-class persisted observation so the
admin trace can show every loop step. Today `AgentRunLoopImpl` overwrites
`lastLlmRawResponse`/`lastProjection` each step (`:221`/`:189`) and
`TraceWriter.recordTurn` persists only the final one into `BotTurn`'s single
columns; the per-call `llm_call_log` exists but truncates to 500 chars and
carries no per-call projection. S2 persists per-step {raw response, that step's
projection, tool_calls, latency, step_index, callType} (new table or
`BotTurn.llm_calls` jsonb), nests them in the trace endpoint, and renders an
"Invocation 1..N" list in the UI — **observation-only: no change to the loop's
control flow, LLM call count, timing, or termination semantics.** **Depends on:**
nothing (but is the prerequisite tool for S3).

### S3 — Projection audit (C1) + Skill-driven convergence (C2) (Sprint 52) — `prompt_projection` — §7 REQUIRED
First produce a **field consumption matrix** (C1, zero-risk diagnostic): each
projection field × {system_prompt explains it / eval trace contract requires it /
drift-task reconstruction depends on it / a Skill declares it / a real trace shows
the LLM using it}. Then converge (C2): make the projection **read Skill
declarations** — `skill.requiredContextKeys()` decides which context slots to
project (which also forces the `moderation_context` coherence question: the FAQ
Skill declares it required but `ContextProjectionBuilder` never emits it),
`state_inheritance.soft_signal_via_projection` gates soft signals like
`prior_use_case_carry`, and the computed-then-discarded UC-driven `tool_schemas`
base computation is removed in favour of the Skill-driven `plan.allowedTools()`.
**No projection field is deleted or changed before the C1 map confirms no
consumer depends on it.** **Depends on:** S2 (per-call projection visibility).

### S4 (conditional) — Projection dedup / denoise (C3) (Sprint 53) — `prompt_projection` — §7 REQUIRED
Only if the S3 C1 consumption-map justifies it: separate "projected for the eval
trace contract" from "projected for LLM decisions," merge duplicated values
(`session.*` vs `budget_state`/`phase_plan`), and evaluate the `task_summary`
natural-language restatement, the ~15 always-null §N0 slots, and the
`drift_history`/`task_history` per-turn re-parse for their actual value to the
LLM. Highest-risk sub-sprint; **shadow + bad-case rerun mandatory**; batched
small-step convergence. **Depends on:** S3 C1 map + C2 landed. **Decision to run
S4 is taken at S3 close**, based on the C1 findings (per the human's "S4 可选").

## 4. Non-goals (explicit)

- Not implementing the three N/A Phase-5 metrics (`correct_tool_invocation_rate`
  / `escalation_correctness_rate` / `grounded_final_answer_rate`; N/A since
  pre-M3).
- Not refactoring the `ControlKernel` / `PhaseEvaluator` phase machine — M5 reads
  their products (the `PhasePlan`, the per-turn state) only.
- Not changing the LLM provider / model / temperature / deadline.
- Not changing `composite.py` scoring weights or the `>= 0.7` summary threshold
  (S1 is display-only).
- Not coupling the Tier-2 HTML display to the §5.6 human-judgment gate (the
  bad-case gate stays human-review-of-trace regardless of rendering).
- Not changing bot behaviour, LLM call count, or call timing (S2 observation-only).
- Not deleting/altering any projection field before the C1 consumption-map (S3).
- Not resolving `R-bad-case-metadata-field-name-canonicalize` or
  `R-case-families-manifest-cs095-smoke-vs-anchor-orphan` (orthogonal R-items).
- **Deferred to a later milestone**: UC-G/H/I/J bad-case seeding; the Single
  Handover Orchestrator P0 release-gate blocker (still the standing P0 — M5 does
  not displace it, it is a parallel observability cleanup).

## 5. Milestone acceptance bar (regression-safety + capability-demonstrated)

This is a cleanup/coherence milestone (cf. M4-Eval-Cleanup) — its acceptance bar
is **regression-safety, not bad-case closure**. No bad case is expected to flip
to PASS as a *result* of M5; instead, the bad-case suite is the **regression
guard** for S3's projection convergence (the only behaviour-risk surface).

**Hard gates (all must pass at milestone close):**

1. **Bad-case suite distribution holds** vs the M4 close baseline, on a real-LLM
   rerun at milestone close: **PASS×5** (cs001, cs014, cs029, cs066, fg5q) +
   **IMPROVING×4** (alice, cs011, cs012, wmkb) + **FAIL×3** (cs015, cs095, iwzx)
   + **OOSR×0**. This is the §5.6 primary human-judgment gate, used here as a
   regression-safety bar — S3/S4 must not regress it. (Deliver-agent confirms the
   `core` vs `scope-relevant` run set against `bad_cases/_manifest.md` §5.6.1 at
   S3/close planning; the full 12-case suite is the M4-close regression baseline.)
2. **Java baseline** `1163 / 1-inherited / 0 / 2` — no NEW regression after S2
   (B2 is observation-only; the test count may grow with new S2 tests).
3. **Python baseline** — no NEW regression (`3 failed, 460 passed` via
   `uv run python -m pytest` at HEAD `84ae017`).
4. **Tier-0 safety floor + grounding floor unchanged.**
5. **Codex §4.1 anti-hardcode kernel** pass over the cumulative range (S1/S2
   trivially approve as display/observation; S3/S4 are the substantive review).

**Capability-demonstrated (the milestone's actual deliverable, verified at close):**

- (a) `report.html` re-rendered from the M4 run `results/20260523-075141`
  displays the four-tier verdict + `suite_authority`; no misleading "0/12"
  headline.
- (b) admin trace for a multi-step FAQ turn shows N invocations, each with its
  own projected context + raw response.
- (c) S3 ships the field consumption-map + Skill-driven gating, with the real-LLM
  bad-case rerun confirming no change to LLM-visible semantic availability.

## 6. Hard fences (milestone level)

**S1**: display-only (HTML) + one intentional foundational fold-back; no scoring
/ threshold change; no fixture change; no `server/` touch; no regeneration of
historical `results.json`; the phase5 §6.9 fold-back preserves content (marks
superseded + pointer per `doc_governance.md`, does not delete or rewrite the rest
of phase5).

**S2**: observation-only — no change to `AgentRunLoopImpl` loop condition, LLM
call count, timing, or termination semantics; no semantic change to existing
`bot_turns` columns (new table or new jsonb column only; backward-compat);
routing/rerank calls labelled by `callType`, not混入 chat invocations; the eval
harness `get_llm_calls` contract on the existing `/llm-calls` endpoint is not
broken (it is in use).

**S3 / S4**: **no projection field deleted or changed before the C1 consumption-
map confirms it is not consumed by** (a) the eval trace contract validator
(`trace/collector.py`), (b) drift/task-history reconstruction
(`ContextProjectionBuilder.buildDriftAndTaskHistory`), or (c) admin-trace
rendering; convergence is **registry/Skill-driven — no per-UC if-else** (§1.7);
no `escalation_reason` enum / tool schema / Tier-0 floor change smuggled in;
**real-LLM rerun is the evidence gate** (mocked-LLM covers projection
rendering/wiring only).

## 7. R-items consumed / surfaced

- **Consumed**: `R-eval-report-observability` (S1) — both proposals recommend
  **elevating it low → medium-high** given the human-cold-read cost; M5 schedules it.
- **Expected to surface** (record as R-items or fold into the owning sub-sprint):
  the `moderation_context` declared-but-never-emitted contract gap (S3 C1); the
  `knowledge_hits` dual-path "which is the M3 canonical projection path" coherence
  question (S3 C1); possibly an `llm_call_log` 500-char truncation review when S2
  lands full per-invocation persistence.
- **NOT bundled** (stay separate): `R-bad-case-metadata-field-name-canonicalize`,
  `R-case-families-manifest-cs095-smoke-vs-anchor-orphan`.

## 8. Codex review plan (§4.3)

**Default: milestone-shared review at M5 close** over the cumulative commit range
(S1 + S2 + S3 [+ S4]). S1 (eval-harness display + a foundational fold-back) and S2
(observation-only infra) carry no semantic surface — Codex verifies S1's fold-back
is minimal/correct per `doc_governance.md` and S2 is genuinely observation-only
(no loop behaviour change); the anti-hardcode kernel trivially approves both.

**Locked addition (human-confirmed 2026-05-24; deliver-agent discretion per §4.3):**
a **per-sub-sprint Codex review at S3 close**, before S4 builds on it — S3 is the
only semantic surface and is coupled to the eval trace contract; verifying it in
isolation de-risks the milestone. A targeted `compact/sprint-052-codex-review-prompt.md`
is generated at S3 close in addition to the milestone-shared review at M5 close.

## 9. Estimated milestone duration (informational, not a gate)

S1 ≈ 3-4 dev days (four-tier render + dashboard removal + phase5 fold-back +
tests). S2 ≈ 3-5 dev days (server + DB migration + trace endpoint + UI). S3 ≈ 3-5
dev days (C1 audit + C2 convergence + real-LLM rerun). S4 (conditional) ≈ 2-4 dev
days. Total ≈ 2-4 calendar weeks if S4 runs; ≈ 1.5-3 weeks if S3's C1 map does not
justify S4.
