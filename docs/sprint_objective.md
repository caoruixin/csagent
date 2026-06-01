---
title: Sprint 066 / S-Auto-11 — Eval-harness robustness + per-iter trace persistence + infra-error detection (M-Auto-3 sub-sprint 1 of ~4)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-01
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-065-objective.md]
superseded_by: null
notes: >
  M-Auto-3 / Sprint 066 / S-Auto-11. First sub-sprint of M-Auto-3 (Substrate-hygiene).
  **Layer**: `infra` + `eval_spec`/harness (eval-harness robustness + autoloop loop.py
  orchestration; NO customer-service-agent semantic decision change). **§7 stanza**:
  EXEMPT (harness/infra carve-out per §4.1; self-walked below for paper-trail).
  **Codex**: milestone-shared (default) at M-Auto-3 close UNLESS the OQ-S65.7/trace work
  requires editing the SHA-locked `eval_runner.py` (fence-#13) — then dev STOP-and-surfaces
  for a controlled override (human-authorized + SHA rebaseline) which upgrades S-Auto-11 to
  PER-SUB-SPRINT Codex per §4.3 trigger #3.

  This sub-sprint makes the autoloop's measurement trustworthy + observable BEFORE the
  substrate fixes (A1/A2/A3/B1) land in S-Auto-12..14, so the M-Auto-3 §11 unlock criteria
  can actually be measured. It carries forward the M-Auto-2 OQ-S65.7/8 (LLM-deadline
  degradation must not masquerade as a Tier-0 fitness regression) + the
  `R-overnight-eval-traces-not-persisted` observability prerequisite + Module D
  (user_simulator turn0 flake) + the post-`b351648` Java baseline re-establishment.

  **Substrate state inherited from M-Auto-2** (all in place at HEAD): OQ-S65.5 fixed
  (`eval_runner.run_suite` resolves `spec.path` absolute — the eval now actually runs);
  OQ-S65.6 fixed (`tier_evaluator._evaluate_layer0` is a delta, not absolute); scoring SHA
  `35305bd8…`; determinism config (`b351648`: bot/sim temp→0, LLM deadline 30→60s, eval
  concurrency 4→2). applier.py S-Auto-9 fence #20 fixes FINALIZED (do NOT touch).

  Dev session source-of-truth: `compact/sprint-066-dev-prompt.md` (self-contained per §9).
  Dev reads ONLY `AGENTS.md` (auto-loaded) + that prompt.
---

# Sprint 066 / S-Auto-11 — Eval-harness robustness + per-iter trace persistence + infra-error detection

## Class

- **Layer (primary)**: `infra` + `eval_spec`/harness — eval-harness robustness (`user_simulator` turn0) + autoloop `loop.py` orchestration (trace persistence + infra-error detection). No customer-service-agent semantic decision (UC routing / escalation posture / drift) is changed.
- **§7 stanza**: **EXEMPT** (harness/infra carve-out per `iteration_governance.md` §4.1 / §7). Self-walked below for paper-trail.
- **Codex review plan (§4.3)**: milestone-shared (default) at M-Auto-3 close UNLESS scope #3/#4 requires editing the SHA-locked `eval_runner.py` (fence-#13) → STOP-and-surface for a controlled override + per-sub-sprint Codex per §4.3 trigger #3.
- **Position in milestone**: 1st of ~4 (S-Auto-11 harness+observability+infra-error → S-Auto-12 A1 dedup → S-Auto-13 A2+A3 skill → S-Auto-14 B1 → optional S-Auto-15 buffer).

## Goal

Make the autoloop fitness measurement trustworthy + observable before the substrate fixes land: (a) the loop must mark LLM-deadline/`service_degraded`/failed-eval iterations as **infra-error**, never a Tier-0 fitness regression (OQ-S65.7/8); (b) per-iter eval traces must be persisted so §5.6 + the M-Auto-3 §11 measurement can read per-turn traces (`R-overnight-eval-traces-not-persisted`); (c) the `user_simulator` turn0 contract-violation flake must drop to 0 (Module D / `R-simulator-first-message-contract-violation-flake`); (d) the post-`b351648` Java test baseline must be re-established (the determinism config touched server Java).

**Acceptance**: Java baseline recorded; D1 turn0 flake `CONTRACT_VIOL_TURN0` 3/24 → 0 on a bad_cases rerun; per-iter eval traces persisted to `autoloop/results/runs/exp-<N>/` (verified by reading a per-turn trace back from a smoke iter); loop marks an injected/observed infra-error iteration as `error` not Tier-0 regression; all baselines preserved (or fence-#13 override recorded if taken).

## Scope (5 steps)

1. **Re-establish the Java test baseline** (FIRST — `b351648` edited `server/src/main/java`: `ChatController` `USER_FACING_LLM_DEADLINE_MS` 30000→60000; `LlmRequest` + `LlmInvocationService` temperature 0.3→0). Run the Java suite; record `Tests run/Failures/Errors/Skipped`. The pre-`b351648` baseline was `1183/1/0/2` (lone inherited `SystemPromptUserRequestedTiebreakerTest` failure per OQ-S41.5). If a determinism-edit-attributable test broke (e.g. an assertion on the old deadline/temperature constant), STOP-and-surface to deliver-agent + human (do NOT silently revert the determinism config — it is M-Auto-2-shipped substrate work).

2. **D1 — `user_simulator` turn0 robustness** (`eval_interactive/eval_interactive/simulator/user_simulator.py`, anchor ~:107-110 turn0 seed-vs-LLM branch + `_parse_simulator_response` + `_call_llm`):
   - turn0: when a case has no `persona.seed_messages`, bypass the LLM call and derive a deterministic first message from the case's seeded content (`form_context.description` or equivalent). The code already prefers `seed_messages` when present (~:107-110); extend the deterministic branch to cover the no-seed case.
   - strengthen `_parse_simulator_response`: on parse failure, retry up to N=3 with an explicit "your prior response was malformed; produce exactly this schema" follow-up message (the current single retry can repeat the same malformed shape).
   - target: `CONTRACT_VIOL_TURN0` 3/24 → 0 (cases cs015 p1, fg5q p2, fg5q p3 from the trace-dive).

3. **Per-iter eval trace persistence** (`R-overnight-eval-traces-not-persisted`; prefer `autoloop/autoloop/loop.py` orchestration — anchor ~L247-260 where it sets `CSAGENT_BACKEND_URL` around the eval + persists the row to `experiments.jsonl`):
   - persist the per-iter eval `results.json` (or a compacted form retaining `case_results[].per_turn_trace[]` + per-case verdicts, or a usable pointer to the `eval_interactive/results/<ts>/` dir) to `autoloop/results/runs/exp-<N>/eval-results.json` alongside the existing verdict.
   - PREFER reading/copying what `eval_runner` already produces (post-OQ-S65.5 the eval writes to `eval_interactive/results/<ts>/`) rather than editing the SHA-locked `eval_runner.py`. If a pointer/copy in `loop.py` suffices, no fence-#13 touch.
   - verify by reading a per-turn trace back from a smoke-iter's persisted artefact.

4. **OQ-S65.7/8 — infra-error detection** (`autoloop/autoloop/loop.py`):
   - detect per-iter infra-error signals: failed/empty suite eval (exit≠0 / missing `results.json`) AND high LLM-deadline / `service_degraded` prevalence (the `eval_interactive` Tier-0 `escalation_compliance` family coerces `service_degraded` on `LlmDeadlineExceededException` — see `eval_interactive/.../scoring/hard_checks.py` + `server/.../ChatController.java` `LlmDeadlineExceededException` give-up).
   - on detection, mark the iteration `infra-error` (a distinct outcome) and DO NOT score it as a Tier-0 fitness regression / discard. Persist the infra-error reason in the iter row.
   - prefer `loop.py` orchestration (reads `eval_runner`'s result) over editing the SHA-locked `eval_runner.py`. If `eval_runner.py` MUST change (e.g. to surface exit-code/empty-result honestly), STOP-and-surface for a fence-#13 controlled override (human-authorized + SHA rebaseline + per-sub-sprint Codex).

5. **Handoff + OQ ledger** (`docs/sprints/sprint-066-handoff.md`).

## Hard fences / STOP conditions

**In scope to edit**: `eval_interactive/eval_interactive/simulator/user_simulator.py` (D1); `autoloop/autoloop/loop.py` (trace persistence + infra-error detection); `autoloop/tests/**` (new tests). Read-only verification of `server/src/main/java` Java baseline.

**Fence (controlled-override only)**: the 4 SHA-locked scoring files `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py`. PREFER not to touch. If `eval_runner.py` must change for #3/#4, STOP-and-surface → human-authorized fence-#13 override + recompute & update `config.fitness.scoring_code_baseline_sha` + per-sub-sprint Codex per §4.3 #3.

**Hard-fenced (do NOT edit)**: `autoloop/autoloop/sandbox/applier.py` (S-Auto-9 fence #20 — FINALIZED), `sandbox/{anti_hardcode_check,content_validator}.py`, `autoloop/autoloop/{meta_agent,memory}/**`, `preflight.py`, `cli.py`; `server/src/main/java` semantic logic (A1/B1 are S-Auto-12/14, NOT here); `server/src/main/resources/skills/**` (A2/A3 are S-Auto-13); `eval_interactive/case_specs/**` (B1 sync is S-Auto-14); `docs/foundational/**`, `docs/current/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/teams/**`; prior sprint/milestone archives.

**STOP-and-surface conditions**: Java baseline shows a determinism-edit-attributable test regression; #3/#4 require editing a SHA-locked scoring file; any hard-fence touch needed; D1 turn0 change risks altering bot-facing behavior (it must only change the simulated USER's turn0). **No `git add -A`** — stage scope explicitly (honor `project_autoloop_dirty_index_hazard`; any `autoloop run` only on a clean committed tree). **Local-Mac only.**

## Test / eval requirements

- **Java baseline**: recorded at step #1 (this IS the deliverable, not "unchanged" — the prior `1183/1/0/2` may have shifted under `b351648`).
- **eval_interactive pytest**: `486 passed, 3 failed` preserved (D1 may add new tests for the turn0 deterministic branch + parse-retry).
- **autoloop pytest**: ≥`266 passed` preserved + new tests for loop.py trace-persistence + infra-error detection.
- **17-fixture detector sweep**: `31 passed` UNCHANGED.
- **scoring SHA**: held at `35305bd8…` UNLESS a fence-#13 override is taken (then re-baselined + recorded in handoff + `config.yaml`).
- **Smoke verification**: 1 autoloop iter (clean tree) reaches Step 9 AND its per-turn eval trace is readable from `autoloop/results/runs/exp-<N>/`; an infra-error-injected/observed iter is marked `error` not Tier-0 regression.
- **D1 target**: a `bad_cases` rerun shows `CONTRACT_VIOL_TURN0` 0/24 on the previously-flaking cases (cs015, fg5q).

## §7 stanza (EXEMPT — harness/infra carve-out; self-walked for paper-trail)

**Target failure layer:** `infra` + `eval_spec`/harness. No customer-service-agent semantic decision (UC hypothesis, drift, escalation posture, response strategy) is touched. D1 changes the simulated USER's turn0 generation; loop.py changes orchestration (trace persistence + infra-error classification). Per §4.1 / §7 these are EXEMPT (harness-robustness + infra carve-out). Self-walked here for paper-trail per the S-Auto-9 precedent.

**Tier-0 invariant:** none added. The infra-error classification is the Runtime's existing infra/persistence/trace responsibility (Constitution §1.4).

**Semantic hardcode:** none. D1's deterministic turn0 uses the case's own seeded content (no keyword/regex/enum). The infra-error detection keys on transport/exception signals (`service_degraded` prevalence, exit-code, missing results), not on semantic content.

**Generalization coverage:** target = the 3 turn0-flake cases (cs015 p1, fg5q p2/p3) + the OQ-S65.8 LLM-deadline degradation pattern; neighbor = any case whose turn0 lacks seed_messages; negative = cases WITH seed_messages unaffected; the infra-error path must not mask a REAL Tier-0 regression (only transport/deadline/empty-eval signals trigger it). Shadow = held-out (unaffected). Counts confirmed at handoff.

## Codex review plan (per §4.3)

Milestone-shared at M-Auto-3 close (default). Upgrades to PER-SUB-SPRINT REQUIRED IF scope #3/#4 takes a fence-#13 `eval_runner.py` controlled override (§4.3 trigger #3) — dev STOP-and-surfaces; deliver-agent authors the per-sub-sprint Codex prompt; verdict lands in `docs/codex-findings.md`.

## Handoff requirements

`docs/sprints/sprint-066-handoff.md` at close. Mandatory: §0 summary (scope, commits, final test counts incl. the re-established Java baseline); §1 Java baseline re-establishment (recorded counts + any determinism-attributable delta); §2 D1 turn0 (before/after + CONTRACT_VIOL_TURN0 evidence); §3 trace persistence (artefact path + a read-back sample); §4 OQ-S65.7/8 infra-error detection (mechanism + smoke evidence); §5 fence-#13 disposition (touched? if yes, override authorization + SHA rebaseline); §6 OQs surfaced; §7 self-check tick-off.

## Commit discipline

Multi-commit acceptable (D1 / loop.py / handoff). Commit message: `Sprint 066 / S-Auto-11 / M-Auto-3 — <description>` + standard deliver-agent footer. No `git add -A` — stage explicitly.

## Self-check (dev MUST verify before claiming done)

- [ ] Java baseline re-established + recorded (any determinism-attributable regression surfaced, not silently reverted).
- [ ] D1 turn0 deterministic branch + N=3 parse-retry implemented; CONTRACT_VIOL_TURN0 0/24 on cs015/fg5q.
- [ ] Per-iter eval traces persisted to `autoloop/results/runs/exp-<N>/`; a per-turn trace read back successfully.
- [ ] loop.py infra-error detection marks LLM-deadline/service_degraded/failed-eval iters as `error`, not Tier-0 regression.
- [ ] Preferred loop.py orchestration; if eval_runner.py (SHA-locked) edited → fence-#13 override authorized + SHA rebaselined + recorded.
- [ ] eval_interactive 486/3 + autoloop ≥266 (+ new tests) + 17-fixture 31 preserved.
- [ ] No edits to applier.py / sandbox / meta_agent / skills / case_specs / server semantic logic.
- [ ] Smoke iter on a CLEAN committed tree; no `git add -A`; local-Mac only.
- [ ] Handoff §0-§7 filled.
