---
title: Sprint 067 / S-Auto-12 — A1 identical-retry-storm dedup (M-Auto-3 sub-sprint 2 of ~4)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-01
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-066-objective.md]
superseded_by: null
notes: >
  M-Auto-3 / Sprint 067 / S-Auto-12. Second sub-sprint of M-Auto-3 (Substrate-hygiene).
  **Layer**: `infra` (tool-dispatch idempotency 回挡) + `prompt_projection` (`already_called`
  observation→binding soft signal) + trace annotation. **§7 stanza**: REQUIRED (semantic-touching
  surface — it shapes how many tool steps the LLM spends; self-walked below). **Codex**:
  milestone-shared (default) at M-Auto-3 close — A1 adds NO Tier-0 and crosses NO §1.7 red line
  (justification embedded), so it stays milestone-shared UNLESS review/work argues for a Tier-0
  elevation of "per-turn tool-call idempotency" → then `human_review_required` (do NOT self-invent).

  This sub-sprint is the **highest-leverage upstream cut** in the proposal's fan-in causal model
  (`docs/solutions/runtime_substrate_hygiene_autoloop_signal_v1.md` §2.2): identical-retry-storm
  is the single largest step-waster (15/24 case-runs at bot_temp=0). It is delivered by manual dev
  (NOT autoloop-generated; does NOT widen the autoloop mutable surface).

  **Red line #2 (proposal §8.2): A1 MUST be hybrid** — deterministic idempotency 回挡 + `already_called`
  soft-signal upgrade. Soft-signal-alone was FALSIFIED over 12 sprints (Sprint 19 §4.3 chose
  soft-signal-first; Sprint 20 shipped the `already_called` slot; the storm persisted 15/24 at temp=0).
  The deterministic backstop is the Runtime's existing idempotency responsibility (Constitution §1.4),
  NOT a §1.5 keyword/regex hardcode.

  **Inherited substrate state at HEAD (do NOT re-fix / do NOT revert)**: S-Auto-11 shipped
  `loop.py` per-iter eval-trace persistence (#3) + infra-error detection (#4); fence-#13 untouched;
  scoring SHA `35305bd8…`. D1 `user_simulator` safe hardening shipped (CONTRACT_VIOL_TURN0 re-attributed
  bot-side per OQ-S66.2 — NOT this sub-sprint's scope). Determinism config (`b351648`: bot/sim temp→0,
  LLM deadline 30→60s, eval concurrency 4→2) is M-Auto-2-shipped — do NOT revert.

  **CORRECTED baselines (per OQ-S66.1/S66.3 from S-Auto-11 — the prompt's prior numbers were STALE)**:
  Java baseline is **`1183 / 10 / 0 / 2`** (the 9 over the old `1183/1/0/2` are PRE-EXISTING
  `PhaseEvaluator max_tool_steps` golden drift, NOT determinism-attributable, NOT yours to fix here);
  eval_interactive **`499 / 4`**; autoloop **`276`**; 17-fixture `31`; scoring SHA `35305bd8…`.

  Dev session source-of-truth: `compact/sprint-067-dev-prompt.md` (self-contained per §9).
  Dev reads ONLY `AGENTS.md` (auto-loaded) + that prompt; code anchors on demand.
---

# Sprint 067 / S-Auto-12 — A1 identical-retry-storm dedup

## Class

- **Layer (primary)**: `infra` (tool-dispatch idempotency 回挡 in the `AgentRunLoopImpl` inner loop / `ToolDispatcher`) + `prompt_projection` (upgrade the existing `already_called` slot from observation-only to a binding soft signal) + trace annotation. The change shapes *how many tool steps the LLM spends*; it does NOT add a UC-routing / drift / escalation-posture decision.
- **§7 stanza**: **REQUIRED** (semantic-touching: it constrains tool-call behavior the LLM owns). Self-walked below.
- **Codex review plan (§4.3)**: milestone-shared (default) at M-Auto-3 close. A1 adds NO Tier-0 invariant and crosses NO §1.7 red line (it reuses an existing order-insensitive hash for byte-identical idempotency, no keyword/regex/enum). UPGRADES to per-sub-sprint REQUIRED ONLY IF the work surfaces a Tier-0 elevation argument for "per-turn tool-call idempotency" (§4.3 trigger #1) → STOP-and-surface; do NOT self-invent a Tier-0.
- **Position in milestone**: 2nd of ~4 (S-Auto-11 ✅ → **S-Auto-12 A1 dedup** → S-Auto-13 A2+A3 skill → S-Auto-14 B1 → optional S-Auto-15 buffer).

## Goal

Cut the single largest source of execution-path divergence at bot_temp=0: byte-identical tool calls re-dispatched within a run (the `IDENTICAL_RETRY` storm, 15/24 case-runs in the bad_cases 24-case trace-dive). After A1, identical `(toolName, canonicalArgumentsHash)` calls that already succeeded this run are served from cache without charging a step/budget AND the LLM is told (binding soft signal) it has already made them — so the loop stops burning max-steps on repeats, lowering the downstream max-steps/budget-exhaustion frequency that mis-stamps escalation reasons.

**Acceptance**: on a 3-pass `bad_cases` rerun (sim_temp=0 + bot_temp=0 + 60s deadline + parallel=1, measured via the S-Auto-11 per-iter trace persistence), `IDENTICAL_RETRY` drops **15/24 → ≤2/24** with the negative controls intact (a normal single call is never deduped; a legitimate same-args retry after an external FAILURE is never deduped); all corrected baselines preserved (Java no NEW failures beyond the OQ-S66.1 10 + new A1 tests; eval_interactive `499/4`; autoloop `276`; 17-fixture `31`; scoring SHA `35305bd8…`).

## Scope (4 steps)

1. **Deterministic idempotency 回挡 (A1-x backstop)** in the tool-dispatch path (`server/src/main/java/.../runtime/AgentRunLoopImpl.java` inner dispatch loop ~:316/:449; and/or `ToolDispatcher.dispatch` ~:86-103). Maintain a per-run `Map<(toolName, canonicalArgumentsHash), cachedResult>` keyed by the **existing** `ContextProjectionBuilder.canonicalArgumentsHash` (~:1267, order-insensitive SHA-256 truncated). On a duplicate key whose prior result was `success==true`: return the cached result, **do NOT charge a step / budget**, and trace-annotate `deduplicated:true` + `original_at_step:<n>`. Only `success==true` results enter the cache (proposal §9 risk row).

2. **`already_called` soft-signal upgrade (A1-x hybrid half)** (`ContextProjectionBuilder` `buildAlreadyCalledNode` ~:1235-1251 + the slot ~:857/:182-187). Upgrade the slot from observation-only ("Slot is observability-only; no short-circuit on dispatch") to a **binding soft signal**: the projection explicitly instructs the LLM "you already called these tools with these args this run — do NOT repeat; draft from the existing result or take the next action." Hybrid is mandatory (red line #2). The LLM still owns *which* tool / *what* content; the soft signal only discourages byte-identical repeats.

3. **Negative controls + trace** — add Java tests proving: (a) a normal single call is not deduped; (b) a legitimate same-args retry after a non-`success` (external-failure) result is NOT deduped (it re-dispatches); (c) the dedup 回挡 returns the cached result + writes the `deduplicated`/`original_at_step` trace annotation + charges no step; (d) the `already_called` binding soft signal is projected. (Trace annotation is load-bearing for downstream report.html/admin trace per `project_observability_debt_pattern`.)

4. **Handoff + OQ ledger** (`docs/sprints/sprint-067-handoff.md`), including the 3-pass `bad_cases` `IDENTICAL_RETRY` before/after measurement.

## Hard fences / STOP conditions

**In scope to edit**: `server/src/main/java/.../runtime/AgentRunLoopImpl.java` (inner dispatch loop dedup) + `server/src/main/java/.../ToolDispatcher.java` (dispatch gating, if the 回挡 lives here) + `server/src/main/java/.../ContextProjectionBuilder.java` (`already_called` slot upgrade; reuse `canonicalArgumentsHash`) + trace annotation plumbing; `server/src/test/**` (new A1 + negative-control tests). Read-only: the bad_cases suite for the measurement.

**Hard-fenced (do NOT edit)**: `PhaseEvaluator.resolveMaxStepsReason` (B1 = S-Auto-14); `server/src/main/resources/skills/**` + `discover_triage.yaml` (A2/A3 = S-Auto-13); `server/src/main/resources/config/tool-policy.yaml` (A2 = S-Auto-13); `eval_interactive/case_specs/**` (B1 sync = S-Auto-14); `eval_interactive/.../user_simulator.py` (D1 FINALIZED in S-Auto-11); the 4 SHA-locked scoring files `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (fence-#13); `autoloop/autoloop/sandbox/applier.py` + `sandbox/{anti_hardcode_check,content_validator}.py`; `autoloop/autoloop/{meta_agent,memory}/**`, `preflight.py`, `cli.py`, `loop.py` (S-Auto-11 FINALIZED — A1 is server-side); `docs/foundational/**`, `docs/current/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/teams/**`; prior sprint/milestone archives.

**STOP-and-surface conditions**:
- A1 dedup regresses a legitimate retry (an external-failure non-`success` retry gets mis-deduped) → halt; refine the `success==true`-only 回挡 condition (proposal §10 stop condition #1).
- The design appears to require a NEW Tier-0 invariant (someone argues "per-turn tool-call idempotency" must be Tier-0) → `human_review_required`; do NOT self-invent a Tier-0 (milestone §6 / Non-goals).
- Any hard-fenced surface needs editing to land A1 → STOP-and-surface for authorization.
- Do NOT revert the `b351648` determinism config or the S-Auto-11 `loop.py`/D1 work.
- **No `git add -A`** — stage scope explicitly. Any `autoloop run` only on a clean committed tree (`project_autoloop_dirty_index_hazard`). **Local-Mac only.** Restart the backend after server changes (`mvn spring-boot:run` has no hot-reload) before the bad_cases measurement.

## Test / eval requirements

- **Java**: no NEW failures beyond the **OQ-S66.1 baseline `1183 / 10 / 0 / 2`** (`mvn -q -pl server test`); the 9 `PhaseEvaluator max_tool_steps` golden-drift failures are PRE-EXISTING and out of scope (do NOT "fix" them here). `Tests run` RISES by the new A1 + negative-control tests; `Failures` stays `10`. If a determinism/golden-unrelated test newly breaks → investigate (it is A1-attributable).
- **eval_interactive pytest**: `499 passed, 4 failed` preserved (A1 does not touch `eval_interactive`).
- **autoloop pytest**: `276 passed` preserved (A1 does not touch `autoloop`).
- **17-fixture detector sweep**: `31 passed`.
- **scoring SHA**: held at `35305bd8…` (A1 touches no scoring file).
- **A1 measurement (the deliverable)**: start a fresh `:8080` backend, then 3-pass `bad_cases` rerun (`cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1`, ×3 at sim_temp=0/bot_temp=0/60s deadline); read the persisted per-iter traces; report `IDENTICAL_RETRY` 15/24 → target ≤2/24, with the dedup `deduplicated:true` annotations visible in-trace and the negative controls intact.

## §7 stanza (REQUIRED)

**Target failure layer:** `infra` (A1 idempotency 回挡 in the dispatch path) + `prompt_projection` (`already_called` observation→binding soft signal). No UC-hypothesis / drift / escalation-posture / response-strategy decision is changed.

**Tier-0 invariant:** This sprint adds NO Tier-0 invariant. A1 dedup is an extension of the Runtime's existing idempotency / persistence responsibility (Constitution §1.4); it is NOT added to `docs/runtime_freeze_and_risk_policy.md` §1/§2. If review argues "per-turn tool-call idempotency" should be elevated to Tier-0 → `human_review_required` (do NOT self-invent).

**Semantic hardcode:** None introduced. The dedup key REUSES the existing `canonicalArgumentsHash` (order-insensitive, identical for all tools — no keyword / regex / enum / per-UC matrix). The 回挡 fires only on byte-identical `(toolName, args)` calls whose prior result was `success==true`; it makes no semantic judgment, and the LLM still owns which tool to call and what content. **Justification for the deterministic backstop (anti-hardcode):** soft-signal-first was the Sprint 19 §4.3 choice and the `already_called` slot shipped Sprint 20 (observation-only); 12 sprints later the storm is still 15/24 at temp=0 → soft-signal-ALONE is empirically falsified. The idempotency backstop is the §1.4 Runtime responsibility, not a §1.5 violation; hybrid (回挡 + soft-signal upgrade) is mandatory (red line #2). Net effect: this REMOVES wasted re-dispatches, it does not add a semantic rule.

**Generalization coverage:** target = the bad_cases `IDENTICAL_RETRY` subset (15/24 case-runs); neighbor = `anchor_outcome` + shadow cases of the same storm shape; negative = (a) a normal single call must NOT be deduped, (b) a legitimate same-args retry after an external FAILURE (non-`success`) must still re-dispatch; shadow = held-out (not read by dev). Counts confirmed at handoff via the 3-pass bad_cases rerun (IMPROVING evidence per §11: `IDENTICAL_RETRY` 15/24 → ≤2/24).

## Codex review plan (per §4.3)

Milestone-shared at M-Auto-3 close (default; cumulative S-Auto-11..14 range + bundled M-Auto-2 residual). UPGRADES to per-sub-sprint REQUIRED only if the work surfaces a Tier-0 elevation argument (§4.3 #1) — then STOP-and-surface; the deliver-agent authors the per-sub-sprint Codex prompt.

## Handoff requirements

`docs/sprints/sprint-067-handoff.md` at close. Mandatory: §0 summary (scope, commits, final test counts incl. Java `1183/10/0/2`+new tests); §1 A1 dedup mechanism (回挡 key + cache scope + step/budget non-charge + trace annotation); §2 `already_called` binding soft-signal upgrade (before/after projection); §3 negative controls (single-call + external-failure-retry NOT deduped); §4 `IDENTICAL_RETRY` 3-pass bad_cases before/after measurement; §5 baselines preserved + §7-stanza self-walk + fence disposition; §6 OQs surfaced (incl. any residual storm not closed by A1); §7 self-check tick-off.

## Commit discipline

Multi-commit acceptable (A1 runtime / projection / tests / handoff). Commit message: `Sprint 067 / S-Auto-12 / M-Auto-3 — <description>` + standard footer `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`. No `git add -A` — stage explicitly. Do not push.

## Self-check (dev MUST verify before claiming done)

- [ ] Deterministic 回挡 implemented (per-run `(toolName, canonicalArgumentsHash)` cache; `success==true`-only; no step/budget charged on hit; reuses existing hash).
- [ ] `already_called` slot upgraded observation-only → binding soft signal (hybrid; red line #2 satisfied).
- [ ] Trace annotation `deduplicated:true` + `original_at_step` written on every 回挡 hit.
- [ ] Negative controls pass: normal single call NOT deduped; external-failure (non-`success`) same-args retry re-dispatches.
- [ ] `IDENTICAL_RETRY` 15/24 → ≤2/24 on a 3-pass bad_cases rerun (backend restarted; measured via S-Auto-11 persisted traces).
- [ ] Java no NEW failures beyond the OQ-S66.1 baseline 10 (+ new A1/negative-control tests); did NOT touch the 9 golden-drift failures.
- [ ] eval_interactive `499/4` + autoloop `276` + 17-fixture `31` + scoring SHA `35305bd8…` preserved.
- [ ] No edits to PhaseEvaluator / skills / case_specs / user_simulator / loop.py / scoring files / applier.py / sandbox / meta_agent; no Tier-0 self-invented.
- [ ] No `git add -A`; any autoloop run on a clean committed tree; local-Mac only.
- [ ] Handoff §0-§7 filled.
