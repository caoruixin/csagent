---
title: Milestone M-Auto-3 — Substrate-hygiene (clean the autoloop fitness signal)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-01
review_cadence: per milestone
supersedes: [docs/milestones/M-Auto-2_objective.md]
superseded_by: null
notes: >
  Opened 2026-06-01 at M-Auto-2 close (Class C — In-flight downgrade). M-Auto-2
  established that the autoloop fitness signal is corrupted at two levels: the
  autoloop calc layer (OQ-S65.5 candidate-eval-never-ran + OQ-S65.6 Layer-0-absolute,
  both FIXED under fence-#13 overrides) and the customer-service AGENT RUNTIME layer
  (7 substrate bugs found in the bad_cases 24-case trace-dive — identical-retry-storm,
  paraphrase-storm, gating-race, escalation mis-stamp, etc.). The autoloop cannot
  produce a trustworthy first cherry-pick until the agent-runtime substrate is cleaned;
  M-Auto-3 cleans it.

  Source-of-truth proposal: `docs/solutions/runtime_substrate_hygiene_autoloop_signal_v1.md`
  (Mode-1 forward-looking; 4 modules A/B/C/D + fan-in causal cluster + §11 unlock
  criteria). Code anchors verified at HEAD `b351648` (19/20 MATCH; the lone drift
  confirms the out-of-scope B2/RC4 dead-code claim). The proposal is advisory, not
  binding; this milestone consumes the human-approved subset.

  **Human-approved scope cut (2026-06-01): A + B1 + D + OQ-S65.7/8 infra-error
  detection.** Module B2/B3 + Module C are explicitly DEFERRED to M-Auto-4+.

  **Causal model (proposal §2)**: the corruption is a fan-in, not a linear chain.
  Multiple independent "step-wasters" (identical-retry-storm 15/24, paraphrase-storm
  11/24, gating-race 4, clarification dead-code) run in PARALLEL into one downstream
  sink (max-steps/budget exhaustion → single-point reason resolver → mis-stamp). Fixing
  any upstream waster lowers exhaustion frequency; fixing the resolver makes residual
  exhaustions honest. Both are needed; they treat different diseases. Module A is the
  highest-leverage upstream cut (A1 dedup alone is 15/24); Module B1 is the single-point
  resolver honesty fix.

  **Ordering red lines (proposal §8.2)**: (1) Module A must precede any eval_spec
  acceptance change — never let an eval-side edit mask a storm symptom (§5.4). B1's
  minimal valid_reasons sync follows the B1 runtime fix, expecting the CORRECTED reason
  (NOT widening to accept a bot mistake). (2) A1 dedup MUST be hybrid (deterministic
  回挡 + already_called soft-signal upgrade) — soft-signal-alone was falsified over 12
  sprints (Sprint 19 chose soft-signal-first; Sprint 20 shipped already_called; storm
  persisted 15/24 at temp=0). (3) The §11 unlock-verification overnight runs at MILESTONE
  CLOSE, not as a sub-sprint.

  **§4 enum verdict (inherited from proposal §4 + the 2026-05-24 escalation proposal)**:
  do NOT add a new `escalation_reason` enum value (e.g. `max_steps_exhausted`). The enum
  is 23-value, three-mirror; reuse the existing `turn_budget_exhausted` catch-all when a
  max-steps exhaustion had viable hits. B1 thereby REMOVES a lossy heuristic
  (toolName-presence→faq label) — it net-LOWERS the hardcode surface.

  **Bundled-into-M-Auto-3 Codex (from the M-Auto-2 Class C lean close)**: the M-Auto-3
  milestone-shared close Codex range MUST be set to also cover the M-Auto-2 residual code
  that S-Auto-9's applier.py-only per-sub-sprint Codex did not review: the fence-#13
  `eval_runner.py` (OQ-S65.5) + `tier_evaluator.py` (OQ-S65.6) controlled overrides + the
  `b351648` server-Java determinism edits (`ChatController`/`LlmRequest`/`LlmInvocationService`).

  **§8.1 conformance**: 4 sub-sprints (S-Auto-11..S-Auto-14) within the 3-5 ceiling;
  margin = 1 (optional S-Auto-15 fix-iteration buffer).
---

# Milestone M-Auto-3 — Substrate-hygiene (clean the autoloop fitness signal)

## 1. Milestone class

**Multi-layer milestone, 4 coordinated sub-sprints (+ optional fix-iteration buffer).** Layer + §7-stanza + Codex breakdown:

| Sub-sprint | Layer (primary) | §7 stanza | Codex review |
|---|---|---|---|
| **S-Auto-11 / Sprint 066** — eval-harness robustness (D) + per-iter trace persistence + OQ-S65.7/8 infra-error detection | `infra` + `eval_spec`/harness (user_simulator turn0; autoloop `loop.py` orchestration; NO agent semantic decision change) | EXEMPT (harness/infra carve-out; self-walked for paper-trail) | Milestone-shared (default) UNLESS the OQ-S65.7/trace work requires editing the SHA-locked `eval_runner.py` (fence-#13) — then dev STOP-and-surfaces for a controlled override + per-sub-sprint Codex per §4.3 trigger #3 |
| **S-Auto-12 / Sprint 067** — A1 identical-retry-storm dedup (highest leverage 15/24) | `infra` (dispatcher idempotency 回挡, reuse existing `canonicalArgumentsHash`) + `prompt_projection` (`already_called` observation→binding soft signal) + trace annotation | REQUIRED | Milestone-shared (default) |
| **S-Auto-13 / Sprint 068** — A2 classify-first gating-race + A3 paraphrase-storm skill-layer discipline | `prompt_projection`/skill + config-governance (A2 `discover_triage.yaml` procedure; tool-policy alignment) + `semantic_planner`/`prompt_projection` (A3 grounding_instruction + projection echo) | REQUIRED | Milestone-shared (default) |
| **S-Auto-14 / Sprint 069** — B1 escalation-reason honesty + minimal eval `valid_reasons` sync | `infra` (`resolveMaxStepsReason` evidence-aware) + `eval_spec` (minimal case_spec valid_reasons sync) | REQUIRED | **PER-SUB-SPRINT REQUIRED** (§4.3 trigger #1 — touches Tier-0 candidate `R-escalation-reason-runtime-evidence-contract-review`; AND a §5.4-sensitive eval edit) |
| (Optional) **S-Auto-15 / Sprint 070** — fix-iteration buffer | TBD | TBD | TBD per §4.3 triggers |

## 2. Goal

Make the autoloop's per-iteration fitness signal measure **propose quality**, not the agent-runtime substrate bugs that currently make byte-identical inputs diverge into storms → max-steps → mis-stamped reasons. At M-Auto-3 close, a 3-pass `bad_cases` rerun (sim_temp=0 + bot_temp=0) shows the path-discipline and escalation-honesty bugs are bounded (proposal §11 thresholds), so the `tier1_bad_cases_regression_5_to_0` gate becomes a trustworthy fitness signal and the 0-keep result becomes *diagnosable* (propose-quality vs calc-bug vs narrow surface) rather than storm noise.

This is the **prerequisite** the M-Auto-2 Class C downgrade identified: M-Auto-2 fixed the autoloop CALC layer (OQ-S65.5/6); M-Auto-3 fixes the agent-RUNTIME layer + the loop's infra-error honesty (OQ-S65.7/8) + the eval-harness flake (D). After M-Auto-3, whether the first cherry-pick is achievable is a M-Auto-4 proposer-scope question, cleanly separated from substrate noise.

**What ships in `main` (auto-loop-branch) at M-Auto-3 close**:

- Agent runtime: A1 dispatcher dedup (idempotency 回挡 + projection soft-signal upgrade); B1 `resolveMaxStepsReason` evidence-aware (reuse `turn_budget_exhausted`, no new enum).
- Skills: A2 `discover_triage.yaml` classify-first procedure; A3 paraphrase grounding/projection (manual dev delivery — NOT autoloop-generated; does NOT widen the autoloop mutable surface).
- Eval harness: D1 `user_simulator` turn0 deterministic seed + parse-retry.
- Autoloop infra: per-iter eval trace persistence; OQ-S65.7/8 infra-error detection (loop marks LLM-deadline/service_degraded/failed-eval iterations as infra-error, not Tier-0 regression).
- Eval spec: minimal `valid_reasons` sync for the cases B1 re-stamps (the bot fix comes first; the case_spec expects the CORRECTED reason — NOT widening to accept a bug).

## 3. Sub-sprint sequence

### S-Auto-11 / Sprint 066 — Eval-harness robustness + per-iter trace persistence + infra-error detection (NEXT)

**Layer:** `infra` + `eval_spec`/harness. **§7 stanza:** EXEMPT (harness/infra carve-out). **Codex:** Milestone-shared default UNLESS SHA-locked `eval_runner.py` (fence-#13) must be edited (then STOP-and-surface + per-sub-sprint Codex per §4.3 trigger #3). **Est:** ~2-3 days.

**Scope:**
1. **Re-establish Java test baseline** (first task — `b351648` determinism config touched `server/src/main/java`). Run the Java suite; record `Tests run/Failures/Errors/Skipped`; if any determinism-edit-attributable test broke, STOP-and-surface.
2. **D1 — `user_simulator` turn0 robustness** (`eval_interactive/eval_interactive/simulator/user_simulator.py:107-110`): on turn0 with no `persona.seed_messages`, bypass the LLM call and derive a deterministic first message from `form_context`/seeded content; strengthen `_parse_simulator_response` with N=3 schema-reminder parse-retry. Target: `CONTRACT_VIOL_TURN0` 3/24 → 0.
3. **Per-iter eval trace persistence** (`R-overnight-eval-traces-not-persisted`; prefer `autoloop/autoloop/loop.py` orchestration — persist the eval_interactive `results.json` / per-turn traces or a usable pointer to `autoloop/results/runs/exp-<N>/`). So §5.6 trace review + §11 measurement can read per-turn traces.
4. **OQ-S65.7/8 — infra-error detection** (`autoloop/autoloop/loop.py`): detect per-iter LLM-deadline / `service_degraded` prevalence + failed/empty suite eval (exit≠0 / missing results.json) → mark the iteration `infra-error`, NOT a Tier-0 fitness regression / discard. Prefer loop.py orchestration over editing the SHA-locked `eval_runner.py`.
5. **Handoff** + OQ ledger.

### S-Auto-12 / Sprint 067 — A1 identical-retry-storm dedup — AFTER S-Auto-11

**Layer:** `infra` + `prompt_projection`. **§7 stanza:** REQUIRED. **Codex:** milestone-shared default. **Est:** ~2-3 days.

A1-x hybrid (proposal §3 Module A): in the tool dispatch path maintain a per-run `Set<(toolName, canonicalArgumentsHash)>` (reuse the existing `ContextProjectionBuilder.canonicalArgumentsHash`); on a duplicate `success==true` key, return the cached result + trace-annotate `deduplicated:true` + `original_at_step` + do NOT charge a step/budget; AND upgrade the `already_called` projection slot from observation-only to a binding soft signal (LLM told "already called these tools with these args this turn — don't repeat"). Hybrid is mandatory (red line #2). Negative-control: legitimate same-args retries after an external FAILURE (non-success result) are NOT deduped.

### S-Auto-13 / Sprint 068 — A2 classify-first + A3 paraphrase skill-layer discipline — AFTER S-Auto-12

**Layer:** `prompt_projection`/skill + config-governance + `semantic_planner`. **§7 stanza:** REQUIRED. **Codex:** milestone-shared default. **Est:** ~2-3 days.

- **A2 (gating-race)**: change `discover_triage.yaml` procedure to "classify first, then (in RESOLVE) search" — aligns the skill with the existing tool-policy (`classify_use_case:[ALL]`, `search_knowledge` requires a UC). Zero runtime code (skill-layer fix, per proposal §3 A2-A1). Delivered by manual dev — NOT autoloop-generated (does not widen the mutable surface).
- **A3 (paraphrase-storm)**: add to `$.grounding_instruction`/`$.procedure` "if a `search_knowledge` returns hits with `faq_miss=false`, don't search again this turn — draft from existing hits or escalate"; + projection echo of the prior search result so the LLM need not re-search to confirm. Soft-signal-first (Constitution §1.5); a hard cap is a last-resort backstop only.

### S-Auto-14 / Sprint 069 — B1 escalation-reason honesty + minimal eval sync — AFTER S-Auto-13

**Layer:** `infra` + `eval_spec`. **§7 stanza:** REQUIRED. **Codex:** **PER-SUB-SPRINT REQUIRED** (§4.3 trigger #1 Tier-0 candidate + §5.4-sensitive eval edit). **Est:** ~2-3 days.

- **B1**: change `PhaseEvaluator.resolveMaxStepsReason` so step 3 reads the most-recent `search_knowledge` result's `faq_miss` flag from `ToolEvent.resultData` instead of merely checking `te.toolName()`. When the turn had viable hits (`faq_miss=false`), fall back to the existing `turn_budget_exhausted` catch-all rather than `faq_miss_threshold_exceeded`. NO new enum (§4 verdict). This removes a lossy heuristic → net-lowers the hardcode surface.
- **Minimal eval sync**: for the specific cases that previously expected `faq_miss_threshold_exceeded` on a viable-hits exhaustion, update their `valid_reasons` to expect the now-correct `turn_budget_exhausted`. This follows the bot FIX (§5.4-compliant: spec expects CORRECT behavior, not widening to accept a mistake). Codex per-sub-sprint verifies this is not eval-side masking.

### (Optional) S-Auto-15 / Sprint 070 — Fix-iteration buffer

Reserved within the §8.5 ceiling if S-Auto-11..14 surface second-order issues.

## 4. Non-goals (explicit)

- M-Auto-3 does NOT implement Module B2 (clarification-budget revive / RC4), Module B3 (R5 turn-budget-conflation eval config + RC3 fallback-UC), or Module C (R7 classifier non-determinism bounding). All deferred to M-Auto-4+.
- M-Auto-3 does NOT add a new `escalation_reason` enum value (§4 verdict: reuse `turn_budget_exhausted`).
- M-Auto-3 does NOT add a new Tier-0 invariant (A1 dedup is the Runtime's existing idempotency responsibility per Constitution §1.4; if review argues for a Tier-0 elevation → `human_review_required`, do NOT self-invent).
- M-Auto-3 does NOT widen the autoloop mutable surface — A2/A3 skill fixes are delivered by manual dev, NOT autoloop-generated (Stage-2 entry remains a separate post-M-Auto-3 decision).
- M-Auto-3 does NOT use keyword/regex/if-else/enum-expansion to solve a semantic failure (Constitution §1.5/§1.7); A2/A3 are skill soft-fields + projection soft signals; A1 dedup is byte-identical idempotency (no semantic judgment).
- M-Auto-3 does NOT pursue cloud/remote-server execution (local-Mac per the standing direction).
- M-Auto-3 does NOT widen the per-iteration fitness suite beyond the existing 46 cases.

## 5. Milestone acceptance bar

**Hard gates (close PASS only if all clear):**

- [ ] **§11 unlock criteria** — 3-pass `bad_cases` rerun (sim_temp=0 + bot_temp=0): `IDENTICAL_RETRY` 15/24→≤2; `PARAPHRASE_STORM` 11/24→≤3; `GATING_RACE` 4→≤1; `ESCALATION_MISSTAMP` 5/24→≤1; `CONTRACT_VIOL_TURN0` 3/24→0; 8-flipping-case 3-pass `case_passed` range 2→≤1. (Measured via the per-iter trace persistence shipped in S-Auto-11.)
- [ ] **Java test baseline re-established (S-Auto-11) + preserved thereafter** — the post-`b351648` baseline is recorded at S-Auto-11 and held across S-Auto-12/14 (A1 + B1 are the agent-runtime edits; expect new targeted tests, no unexplained regression).
- [ ] **Python test baselines preserved** — eval_interactive `486/3` (S-Auto-11 D1 may add tests); autoloop pytest ≥266 (S-Auto-11 loop.py changes add tests); 17-fixture detector sweep `31`; scoring SHA held UNLESS a sub-sprint takes an authorized fence-#13 controlled override (then re-baselined + recorded).
- [ ] **Safety floor unchanged** (Tier-0 invariants).
- [ ] **Grounding floor unchanged** (per `faq_grounding_contract.md`).
- [ ] **Curated bad-case suite manual review pass (PRIMARY GATE per §5.6)** — deliver-agent + human run bad_cases at M-Auto-3 close; this is also the §11 measurement run.
- [ ] **Shadow regression-safety** — no NEW shadow regression beyond known fixtures.
- [ ] **Milestone-shared Codex review at M-Auto-3 close** — `pass / 0` (or `approve with downgrade-to-signal follow-up`) over the cumulative S-Auto-11..14 range **PLUS the bundled M-Auto-2 residual code** (fence-#13 OQ-S65.5/6 `eval_runner.py`+`tier_evaluator.py` + `b351648` server-Java determinism). Per `feedback_milestone_close_bad_case_before_codex`, §5.6 evidence is recorded before Codex dispatch.

**Observation-only (recorded; does not gate):** per-iter elapsed time; `new_semantic_hardcode_count` (target 0 — A1/B1 remove heuristics, A2/A3 are soft fields); `soft_signal_conversion_count` (A1 already_called upgrade + A3 projection echo count up); the first VALID overnight result on the cleaned substrate (does it yield a non-empty kept-slate? — informs M-Auto-4 proposer scope but does not gate M-Auto-3).

## 6. Hard fences (milestone-level)

M-Auto-3 is substrate-hygiene that DELIBERATELY edits surfaces M-Auto-2 fenced. The M-Auto-3 fence model:

- **In scope to edit** (the substrate-hygiene surfaces): `server/src/main/java` runtime dispatch + `PhaseEvaluator` (A1, B1); `server/src/main/resources/skills/*.yaml` LLM-soft fields + `discover_triage.yaml` procedure (A2/A3, manual dev); `server/src/main/resources/config/tool-policy.yaml` if A2 needs alignment; `eval_interactive/.../user_simulator.py` (D1); `autoloop/autoloop/loop.py` (trace persistence + infra-error detection); the specific `eval_interactive/case_specs/**` cases B1 re-stamps (minimal valid_reasons sync only).
- **Still fenced (controlled-override only, human-authorized + SHA rebaseline + per-sub-sprint Codex on touch)**: the 4 SHA-locked scoring files `tier_evaluator.py` / `eval_runner.py` / `baseline_loader.py` / `gaming.py`. Prefer NOT to touch; OQ-S65.7/trace work should live in `loop.py`. If `eval_runner.py` must change, it is a fence-#13 controlled override (precedent: OQ-S65.5/6).
- **Hard-fenced (do NOT edit)**: `autoloop/autoloop/sandbox/applier.py` (S-Auto-9 fence #20 fixes — FINALIZED); `autoloop/autoloop/sandbox/{anti_hardcode_check,content_validator}.py`; `autoloop/autoloop/meta_agent/**`, `memory/**`, `preflight.py`, `cli.py`; `docs/foundational/**`, `docs/current/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/teams/**`; prior sprint/milestone archives.
- **No new Tier-0 invariant**; **no new `escalation_reason` enum**; **no Stage-2 mutable-surface unlock**; **no `git add -A`** (stage scope explicitly; honor `project_autoloop_dirty_index_hazard` — any autoloop run on a clean committed tree).

## 7. R-items consumed / surfaced

**Consumed (status `proposed` → in-scope; flip to closed at M-Auto-3 close per resolution):**

- A1 ← `R-runtime-identical-tool-call-retry-storm` (successor to `R-runtime-orchestrator-tool-call-deduplication` write-side).
- A2 ← `R-runtime-tool-gating-race-uc-none`.
- A3 ← `R-runtime-paraphrase-storm-search-knowledge`.
- B1 ← `R-runtime-escalation-reason-misstamp-maxsteps-faq` (touches Tier-0 candidate `R-escalation-reason-runtime-evidence-contract-review`).
- D ← `R-simulator-first-message-contract-violation-flake`.
- Observability ← `R-overnight-eval-traces-not-persisted` + (downstream-enabled) `R-tier1-bad-cases-regression-5-to-0-attribution-unverified`.
- Infra-error detection ← OQ-S65.7/8 (carry-forward from M-Auto-2).
- Substrate hygiene (optional/early) ← `R-autoloop-run-sweeps-dirty-index`.

**Deferred (NOT consumed; M-Auto-4+):** `R-runtime-escalation-reason-turn-budget-conflated-with-intent` (B3/R5), `R-classifier-non-deterministic-uc-selection-at-temp-zero` (C/R7), `R-eval-interactive-judge-score-never-populated` (LOW priority).

**Surfaced (expected):** the first VALID overnight result on the cleaned substrate (kept-slate non-empty?) → M-Auto-4 proposer-scope input; any new bad case from §5.6 close review; any residual storm/mis-stamp not closed by A/B1.

## 8. Codex review plan (per §4.3)

**Default**: milestone-shared review at M-Auto-3 close over the cumulative S-Auto-11..14 range. **The range MUST be extended backward to include the M-Auto-2 residual code** (fence-#13 OQ-S65.5/6 + `b351648` server-Java determinism) bundled per the M-Auto-2 Class C lean-close decision.

**Per-sub-sprint triggers**: S-Auto-14 (B1) is per-sub-sprint REQUIRED (§4.3 #1 Tier-0 candidate + §5.4-sensitive eval edit). S-Auto-11 is per-sub-sprint REQUIRED ONLY IF it takes a fence-#13 `eval_runner.py` controlled override (§4.3 #3). S-Auto-12/13 are milestone-shared default unless they cross a §1.7 red line.

## 9. Estimated duration

~2-3 weeks (4 sub-sprints sequential at ~2-3 days each + close: §5.6 measurement + bundled Codex + close-out). Risk: B1 eval sync surfacing Codex §5.4 scrutiny could add a per-sub-sprint review round; the §11 measurement overnight could surface residual storms requiring an S-Auto-15 fix-iteration.

## 10. Stop conditions (milestone-level)

1. A1 dedup regresses a legitimate retry (external-failure retry mis-deduped) → halt; refine the success-only 回挡 condition.
2. B1 eval sync looks like eval-side masking to Codex (§5.4) → halt; reconsider whether the case_spec change tracks a real bot fix.
3. §11 measurement after S-Auto-14 still shows storms above thresholds → halt; deliver-agent + human decide S-Auto-15 fix-iteration vs partial-close.
4. Any sub-sprint needs to touch a hard-fenced surface beyond the §6 controlled-override carve-out → STOP-and-surface for authorization.
5. The first valid overnight (close-time, observation-only) still yields 0-keep → this is now DIAGNOSABLE (not a halt); route the propose-quality-vs-calc-vs-surface question to M-Auto-4.

## 11. Cross-milestone sequencing

- **Preceding**: M-Auto-2 (C — In-flight downgrade 2026-06-01) fixed the autoloop calc layer + reframed the binding constraint. M-Auto-1C (C 2026-05-30) resolved the substrate-execution kill.
- **Following (post-M-Auto-3)**: M-Auto-4 proposer-quality / tier1-attribution (once the fitness signal is clean, interpret the 0-keep); Module B2/B3 + Module C; Stage-2 mutable-surface unlock decision; M3-B Single Handover Orchestrator P0 remains in the candidate slate.
