# Dev prompt — Sprint 067 / S-Auto-12 / M-Auto-3 (self-contained per `iteration_governance.md` §9)

You are the **dev agent (Claude Code)** for **Sprint 067 / S-Auto-12**, the second sub-sprint of milestone **M-Auto-3 — Substrate-hygiene (clean the autoloop fitness signal)**.

**One-line goal**: cut the single largest execution-path-divergence source at bot_temp=0 — the identical-retry storm (15/24 case-runs) — by giving the tool dispatcher a **hybrid** dedup: a deterministic per-run idempotency 回挡 for byte-identical successful calls **plus** an upgrade of the existing `already_called` projection slot from observation-only to a **binding soft signal**. Net effect: the loop stops burning max-steps on byte-identical repeats.

## Read order (minimal)
- `AGENTS.md` (auto-loaded — governance chain: Constitution, doc_governance, agent_context_guide, iteration_governance). Do NOT read other docs to start.
- THIS prompt — it embeds the full contract. Read the code anchors below on demand during work.

## CRITICAL operational constraints (top-of-mind)
- **Branch**: `auto-loop-branch`. **Local-Mac only** — NO cloud / remote framing.
- **CORRECTED baselines (the older prompts were STALE — verified by S-Auto-11, OQ-S66.1/S66.3)**: Java is **`Tests run: 1183, Failures: 10, Errors: 0, Skipped: 2`** — the 9 failures over the old `1183/1/0/2` are PRE-EXISTING `PhaseEvaluator max_tool_steps` golden drift (`resolve_faq_grounded_answer.yaml` 6 vs golden 4; `discover_triage.yaml` 3 vs golden 2), **NOT determinism-attributable and NOT yours to fix here** (they live on hard-fenced skills/test-golden surfaces). eval_interactive **`499 passed, 4 failed`** (4 pre-existing). autoloop pytest **`276 passed`**. 17-fixture **`31`**. scoring SHA **`35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8`**.
- **Inherited substrate (at HEAD; do NOT re-fix / do NOT revert)**: S-Auto-11 shipped `autoloop/autoloop/loop.py` per-iter eval-trace persistence (#3) + infra-error detection (#4) — FINALIZED, do NOT touch `loop.py`. D1 `user_simulator.py` safe hardening shipped (its `CONTRACT_VIOL_TURN0` target was re-attributed BOT-SIDE per OQ-S66.2 — that bot-side fix is NOT this sub-sprint). The `b351648` determinism config (bot/sim temp→0, `ChatController` LLM deadline 30→60s, eval concurrency 4→2) is M-Auto-2-shipped — do NOT revert. fence-#13 scoring files untouched.
- **Clean-tree discipline (`project_autoloop_dirty_index_hazard`)**: any `autoloop run` only on a clean committed tree (commit/stash first). Never `git add -A`; stage scope files explicitly.
- **Restart backend after server changes**: `mvn spring-boot:run` has NO hot-reload. This sub-sprint EDITS server Java, so the bad_cases measurement MUST run against a freshly-restarted `:8080` backend.
- **macOS proxy note (`reference_macos_proxy_httpx_localhost`)**: the Mac runs a system proxy at `127.0.0.1:7890`; if a localhost HTTP call fails it is the proxy. `eval_interactive` agent_client already disables the proxy.

## Code anchors (verified at `b351648`; server Java is UNCHANGED since — S-Auto-11 touched only `user_simulator.py`/`loop.py`/tests — so these hold at HEAD, but confirm on read)
- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`: outer step loop `:179`; **inner per-response dispatch loop `:316`**; unconditional **`toolDispatcher.dispatch(...)` `:449` (NO dedup today)**; `already_called` slot is observation-only `:182-187` (comment "Slot is observability-only; no short-circuit on dispatch"); classify-commit early return `:520-535`; max_steps exit `:554`.
- `server/src/main/java/.../ContextProjectionBuilder.java`: `already_called` slot `:857`; **`buildAlreadyCalledNode(priorToolEvents)` `:1235-1251`** (emits `{tool, arguments_hash, at_step}`, `success==true` only, cross-step); **`canonicalArgumentsHash(args)` `:1267`** (order-insensitive SHA-256, truncated 16 hex — **this is the dedup key, already implemented**).
- `server/src/main/java/.../ToolDispatcher.java`: dispatch gating `:86-103` (`session.getActiveUseCase()` `:97`); canonical 23-enum `:35-58`.

**Key fact (proposal §1.2)**: the dedup key (`canonicalArgumentsHash`) AND the soft-signal slot (`already_called`) already exist in code. What is missing is (a) using the hash at dispatch as an idempotency 回挡, and (b) upgrading the soft signal from observation to binding.

## Embedded contract

### Class
- **Layer (primary)**: `infra` (tool-dispatch idempotency 回挡) + `prompt_projection` (`already_called` observation→binding soft signal) + trace annotation. No UC-routing / drift / escalation-posture / response-strategy decision changes.
- **§7 stanza**: **REQUIRED** (semantic-touching — it constrains how many tool steps the LLM spends). Self-walked below.
- **Codex**: milestone-shared (default) at M-Auto-3 close. A1 adds NO Tier-0 and crosses NO §1.7 red line. UPGRADES to per-sub-sprint REQUIRED only if the work surfaces a Tier-0 elevation argument for "per-turn tool-call idempotency" (§4.3 #1) → STOP-and-surface; do NOT self-invent a Tier-0.

### Red line #2 (proposal §8.2 — MANDATORY): A1 MUST be hybrid.
Deterministic 回挡 ALONE is insufficient (a stubborn LLM can re-emit across steps and still burn steps before the cache helps); soft-signal ALONE is empirically FALSIFIED (Sprint 19 chose soft-signal-first; Sprint 20 shipped `already_called`; the storm persisted 15/24 at temp=0 twelve sprints later). **Ship BOTH halves.**

### Scope (4 steps)

**1. Deterministic idempotency 回挡 (backstop half).** In the inner dispatch path (`AgentRunLoopImpl` `:316`/`:449`, and/or `ToolDispatcher.dispatch` `:86-103`) maintain a per-run `Map<(toolName, canonicalArgumentsHash) → cachedResult>` keyed by the EXISTING `ContextProjectionBuilder.canonicalArgumentsHash` (`:1267`). On a duplicate key whose prior result was `success==true`: return the cached result, **do NOT charge a step / budget**, and trace-annotate `deduplicated:true` + `original_at_step:<n>`. **Only `success==true` results enter the cache** (so an external-failure result is never cached → legitimate retries still re-dispatch).

**2. `already_called` soft-signal upgrade (hybrid half).** Upgrade the slot (`ContextProjectionBuilder` `buildAlreadyCalledNode` `:1235-1251` + slot `:857`/`AgentRunLoopImpl:182-187`) from observation-only to a **binding soft signal**: the projection explicitly tells the LLM "you already called these tools with these args this run — do NOT repeat; draft from the existing result or take the next action." The LLM still owns WHICH tool / WHAT content; the signal only discourages byte-identical repeats.

**3. Negative controls + trace tests** (`server/src/test/**`): (a) a normal single call is NOT deduped; (b) a same-args retry after a non-`success` (external-failure) result is NOT deduped (re-dispatches); (c) the 回挡 returns the cached result + writes `deduplicated`/`original_at_step` + charges no step; (d) the `already_called` binding soft signal is projected. (Trace annotation is load-bearing for downstream report.html/admin trace — `project_observability_debt_pattern`.)

**4. Handoff + OQ ledger** → `docs/sprints/sprint-067-handoff.md`, including the 3-pass `bad_cases` `IDENTICAL_RETRY` before/after.

### Hard fences / STOP conditions
- **In scope**: `AgentRunLoopImpl.java` (inner dispatch dedup) + `ToolDispatcher.java` (if the 回挡 lives there) + `ContextProjectionBuilder.java` (`already_called` upgrade; reuse `canonicalArgumentsHash`) + trace plumbing; `server/src/test/**` (new tests). Read-only: bad_cases suite (measurement).
- **Hard-fenced (do NOT edit)**: `PhaseEvaluator.java` `resolveMaxStepsReason` (B1 = S-Auto-14); `server/src/main/resources/skills/**` + `discover_triage.yaml` + `config/tool-policy.yaml` (A2/A3 = S-Auto-13); `eval_interactive/case_specs/**` (B1 sync = S-Auto-14); `eval_interactive/.../user_simulator.py` (D1 FINALIZED); the 4 SHA-locked scoring files `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py`; `autoloop/autoloop/loop.py` (S-Auto-11 FINALIZED) + `sandbox/applier.py` + `sandbox/{anti_hardcode_check,content_validator}.py` + `meta_agent`/`memory`/`preflight.py`/`cli.py`; `docs/foundational/**`, `docs/current/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/teams/**`; prior sprint/milestone archives.
- **STOP-and-surface**: external-failure retry gets mis-deduped (refine `success==true`-only condition); design seems to need a NEW Tier-0 (`human_review_required` — do NOT self-invent); any hard-fence touch needed; do NOT revert `b351648` / S-Auto-11 work. No `git add -A`; clean-tree for autoloop; local-Mac only.

### Test / eval requirements (commands)
- **Java**: `mvn -q -pl server test` — **no NEW failures beyond `1183 / 10 / 0 / 2`** (the 9 golden-drift failures are PRE-EXISTING; leave them). `Tests run` rises by your new tests; `Failures` stays `10`.
- **eval_interactive pytest**: `cd eval_interactive && uv run python -m pytest --tb=no -q` → `499 passed, 4 failed` (unchanged; A1 is server-side).
- **autoloop pytest**: `cd autoloop && uv run --extra dev pytest -q` → `276 passed`.
- **17-fixture sweep**: `cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py` → `31 passed`.
- **scoring SHA**: `cd autoloop && uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"` → `35305bd8...` (held; you touch no scoring file).
- **A1 measurement (deliverable)**: restart `:8080` backend (`mvn spring-boot:run` or the repo run script), then 3× `cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1` (sim_temp=0/bot_temp=0/60s deadline). Read the persisted per-iter traces (`autoloop/results/runs/exp-<N>/eval-results.json` shape from S-Auto-11, or the `eval_interactive/results/<ts>/` source); report `IDENTICAL_RETRY` 15/24 → target **≤2/24**, `deduplicated:true` annotations visible, negative controls intact.

### §7 stanza (REQUIRED)
- **Target failure layer**: `infra` (A1 idempotency 回挡) + `prompt_projection` (`already_called` binding soft signal). No agent semantic decision changed.
- **Tier-0 invariant**: none added — A1 dedup extends the Runtime's existing idempotency/persistence responsibility (Constitution §1.4); NOT added to `runtime_freeze_and_risk_policy.md` §1/§2. Tier-0 elevation argument → `human_review_required` (do NOT self-invent).
- **Semantic hardcode**: none. Dedup key reuses the existing order-insensitive `canonicalArgumentsHash` (all tools, no keyword/regex/enum/per-UC matrix); 回挡 fires only on byte-identical `success==true` calls and makes no semantic judgment. Justification for the deterministic backstop: soft-signal-first (Sprint 19) shipped (Sprint 20 `already_called`) and the storm persisted 15/24 at temp=0 → soft-signal-alone is falsified; the idempotency backstop is §1.4 Runtime responsibility, not a §1.5 violation; hybrid mandatory. Net: this REMOVES wasted re-dispatches, not adds a rule.
- **Generalization coverage**: target = bad_cases `IDENTICAL_RETRY` subset (15/24); neighbor = anchor_outcome + shadow same storm shape; negative = (a) normal single call not deduped, (b) external-failure same-args retry still re-dispatches; shadow = held-out. Counts confirmed at handoff (3-pass bad_cases: IDENTICAL_RETRY 15/24→≤2/24).

### Codex review plan
Milestone-shared at M-Auto-3 close (default). PER-SUB-SPRINT REQUIRED only if a Tier-0 elevation argument surfaces (§4.3 #1) — STOP-and-surface; deliver-agent authors the prompt.

### Handoff requirements (`docs/sprints/sprint-067-handoff.md`)
§0 summary (scope, commits, final counts incl. Java `1183/10/0/2`+new tests); §1 A1 dedup mechanism (key + cache scope + step/budget non-charge + trace annotation); §2 `already_called` binding soft-signal upgrade (before/after projection); §3 negative controls (single-call + external-failure-retry NOT deduped); §4 `IDENTICAL_RETRY` 3-pass bad_cases before/after; §5 baselines preserved + §7-stanza self-walk + fence disposition; §6 OQs surfaced (any residual storm); §7 self-check tick-off.

### Commit discipline
Multi-commit acceptable. Message: `Sprint 067 / S-Auto-12 / M-Auto-3 — <description>` + footer `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`. No `git add -A`; stage explicitly. Do not push.

## Self-check (verify ALL before claiming done)
- [ ] Deterministic 回挡 implemented (per-run `(toolName, canonicalArgumentsHash)` cache; `success==true`-only; no step/budget on hit; reuses existing hash).
- [ ] `already_called` upgraded observation-only → binding soft signal (HYBRID; red line #2).
- [ ] Trace annotation `deduplicated:true` + `original_at_step` on every 回挡 hit.
- [ ] Negative controls pass: normal single call NOT deduped; external-failure (non-`success`) same-args retry re-dispatches.
- [ ] `IDENTICAL_RETRY` 15/24 → ≤2/24 on a 3-pass bad_cases rerun (backend restarted; via S-Auto-11 persisted traces).
- [ ] Java no NEW failures beyond the OQ-S66.1 baseline `10` (+ new tests); did NOT touch the 9 golden-drift failures.
- [ ] eval_interactive `499/4` + autoloop `276` + 17-fixture `31` + scoring SHA `35305bd8…` preserved.
- [ ] No edits to PhaseEvaluator / skills / case_specs / user_simulator / loop.py / scoring / applier.py / sandbox / meta_agent; no Tier-0 self-invented.
- [ ] No `git add -A`; any autoloop run on a clean committed tree; local-Mac only.
- [ ] Handoff §0-§7 filled.
