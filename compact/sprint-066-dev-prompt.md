# Dev prompt — Sprint 066 / S-Auto-11 / M-Auto-3 (self-contained per `iteration_governance.md` §9)

You are the **dev agent (Claude Code)** for **Sprint 066 / S-Auto-11**, the first sub-sprint of milestone **M-Auto-3 — Substrate-hygiene (clean the autoloop fitness signal)**.

**One-line goal**: make the autoloop's fitness measurement trustworthy + observable BEFORE the substrate fixes land — (a) the loop marks LLM-deadline/`service_degraded`/failed-eval iterations as **infra-error** (not Tier-0 regression), (b) per-iter eval traces are persisted for §5.6/§11 review, (c) `user_simulator` turn0 contract-violation flake → 0, (d) the post-`b351648` Java baseline is re-established.

## Read order (minimal)
- `AGENTS.md` (auto-loaded — governance chain: Constitution, doc_governance, agent_context_guide, iteration_governance). Do NOT read other docs to start.
- THIS prompt — it embeds the full contract. Read code anchors on demand during work.

## CRITICAL operational constraints (top-of-mind)
- **Branch**: `auto-loop-branch`. **Local-Mac only** — NO cloud / remote framing.
- **Clean-tree discipline (`project_autoloop_dirty_index_hazard`)**: `autoloop run` per-exp git commit sweeps the WHOLE staged index onto the exp branch then reverts on checkout — so **only run `autoloop run` on a clean, committed tree** (commit/stash first). Never `git add -A`; stage scope files explicitly.
- **Inherited substrate state (all at HEAD, do not re-fix)**: OQ-S65.5 FIXED (`eval_runner.run_suite` resolves `spec.path` absolute — the candidate eval now actually runs); OQ-S65.6 FIXED (`tier_evaluator._evaluate_layer0` is a delta, not an absolute floor); `config.fitness.scoring_code_baseline_sha = 35305bd8...`; determinism config from `b351648` (bot/sim temp→0, LLM deadline 30→60s in `ChatController`, eval concurrency 4→2). `applier.py` S-Auto-9 fence #20 fixes (`start_new_session=True` + per-port mvn log + `trust_env=False` probe) are FINALIZED — **do NOT touch applier.py**.
- **macOS proxy note** (`reference_macos_proxy_httpx_localhost`): the Mac runs a system proxy at `127.0.0.1:7890`; if a localhost HTTP call fails, it's the proxy. `eval_interactive` agent_client already disables the proxy; the autoloop health probe already uses `trust_env=False`.
- **Restart backend after server changes**: `mvn spring-boot:run` has no hot-reload. (S-Auto-11 does NOT edit server runtime, but the D1 bad_cases verification needs a running :8080 backend — start it fresh.)

## Embedded contract

### Class
- **Layer (primary)**: `infra` + `eval_spec`/harness — `user_simulator` turn0 + autoloop `loop.py` orchestration (trace persistence + infra-error classification). **No customer-service-agent semantic decision is changed** (no UC routing / drift / escalation posture / response strategy).
- **§7 stanza**: **EXEMPT** (harness/infra carve-out per §4.1). Self-walked below for paper-trail.
- **Codex**: milestone-shared (default) at M-Auto-3 close. UPGRADES to per-sub-sprint REQUIRED IF you must edit the SHA-locked `eval_runner.py` (fence-#13) for scope #3/#4 → STOP-and-surface first.

### Scope (5 steps)

**1. Re-establish the Java test baseline (FIRST).** `b351648` (M-Auto-2 determinism config) edited `server/src/main/java`: `ChatController` `USER_FACING_LLM_DEADLINE_MS` 30000→60000; `LlmRequest` + `LlmInvocationService` temperature 0.3→0. Run the project's Java test suite (the repo's standard invocation, e.g. `mvn -q -pl server test` from repo root, or full `mvn test`); record `Tests run / Failures / Errors / Skipped`. The pre-`b351648` baseline was `1183 / 1 / 0 / 2` (the lone failure is the inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` per OQ-S41.5 — STATUS QUO). If a determinism-edit-attributable test broke (e.g. an assertion on the old deadline/temperature constant), **STOP-and-surface** to deliver-agent + human — do NOT silently revert the determinism config (it is M-Auto-2-shipped substrate work; the right fix is usually to update the test's expectation, but that is a human call).

**2. D1 — `user_simulator` turn0 robustness.** File `eval_interactive/eval_interactive/simulator/user_simulator.py` (anchor ~:107-110 = the turn0 seed-vs-LLM branch; also `_parse_simulator_response` + `_call_llm`).
- turn0: the code already prefers `persona.seed_messages` when present. Extend the deterministic branch so that when a case has NO `seed_messages`, turn0 ALSO bypasses the LLM call and derives a deterministic first message from the case's seeded content (`form_context.description` or equivalent) instead of falling through to the moonshot LLM call (the source of the turn0 flake).
- strengthen `_parse_simulator_response`: on parse failure, retry up to **N=3** with an explicit "your prior response was malformed; produce exactly this schema: …" follow-up (the current single retry can repeat the same malformed shape).
- Target: `CONTRACT_VIOL_TURN0` 3/24 → 0 (the flaking cases are `cs015` p1, `fg5q` p2, `fg5q` p3).

**3. Per-iter eval trace persistence** (`R-overnight-eval-traces-not-persisted`). PREFER `autoloop/autoloop/loop.py` orchestration (anchor ~L247-260 = where it sets `CSAGENT_BACKEND_URL` around the eval + persists the iter row to `experiments.jsonl`).
- Persist the per-iter eval `results.json` (or a compacted form retaining `case_results[].per_turn_trace[]` + per-case verdicts, or a usable pointer to the `eval_interactive/results/<ts>/` dir the eval already writes) to `autoloop/results/runs/exp-<N>/eval-results.json` alongside the existing verdict.
- PREFER reading/copying what `eval_runner` already produces (post-OQ-S65.5 the eval writes to `eval_interactive/results/<ts>/`) over editing the SHA-locked `eval_runner.py`. If a loop.py pointer/copy suffices → no fence-#13 touch.
- Verify by reading a per-turn trace back from a smoke-iter's persisted artefact.

**4. OQ-S65.7/8 — infra-error detection** (`autoloop/autoloop/loop.py`).
- Detect per-iter infra-error signals: failed/empty suite eval (exit≠0 / missing `results.json`) AND high LLM-deadline / `service_degraded` prevalence. (Context: the `eval_interactive` Tier-0 `escalation_compliance` check coerces `service_degraded` when the bot hits `LlmDeadlineExceededException` — see `eval_interactive/eval_interactive/scoring/hard_checks.py` `_ESCALATION_REASON_FAMILY` + `server/.../controller/ChatController.java` deadline give-up.)
- On detection, mark the iteration **`infra-error`** (a distinct outcome) and DO NOT score it as a Tier-0 fitness regression / discard. Persist the infra-error reason in the iter row.
- PREFER `loop.py` orchestration (reads `eval_runner`'s result). If `eval_runner.py` (SHA-locked) MUST change to surface exit-code/empty-result honestly → **STOP-and-surface** for a human-authorized fence-#13 controlled override (recompute + update `config.fitness.scoring_code_baseline_sha`; this upgrades S-Auto-11 to per-sub-sprint Codex per §4.3 #3).

**5. Handoff + OQ ledger** → `docs/sprints/sprint-066-handoff.md`.

### Hard fences / STOP conditions
- **In scope**: `eval_interactive/eval_interactive/simulator/user_simulator.py` (D1); `autoloop/autoloop/loop.py` (#3/#4); `autoloop/tests/**` (new tests). Read-only verification of `server/src/main/java` (step #1).
- **Fence (controlled-override only)**: the 4 SHA-locked scoring files `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py`. PREFER not to touch; if `eval_runner.py` must change → STOP-and-surface (fence-#13 override + SHA rebaseline + per-sub-sprint Codex).
- **Hard-fenced (do NOT edit)**: `autoloop/autoloop/sandbox/applier.py` (S-Auto-9 fence #20 FINALIZED), `sandbox/{anti_hardcode_check,content_validator}.py`, `autoloop/autoloop/{meta_agent,memory}/**`, `preflight.py`, `cli.py`; `server/src/main/java` semantic logic (A1/B1 = later sub-sprints); `server/src/main/resources/skills/**` (A2/A3 = S-Auto-13); `eval_interactive/case_specs/**` (B1 sync = S-Auto-14); `docs/foundational/**`, `docs/current/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/teams/**`; prior sprint/milestone archives.
- **STOP-and-surface**: Java determinism-edit test regression; need to edit a SHA-locked scoring file; any hard-fence touch; if a D1 change risks altering the BOT's behavior (it must only change the simulated USER's turn0). No `git add -A`. Local-Mac only. `autoloop run` only on a clean committed tree.

### Test / eval requirements (commands)
- **Java baseline (step #1 deliverable)**: `mvn -q -pl server test` (or the repo's standard invocation) — record `Tests run/Failures/Errors/Skipped`.
- **eval_interactive pytest**: `cd eval_interactive && uv run python -m pytest --tb=no -q` → expect `486 passed, 3 failed` (D1 may ADD tests).
- **autoloop pytest**: `cd autoloop && uv run --extra dev pytest -q` → expect ≥`266 passed` (+ new loop.py tests).
- **17-fixture detector sweep**: `cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py` → `31 passed`.
- **scoring SHA**: `cd autoloop && uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"` → `35305bd8...` (held UNLESS a fence-#13 override is taken — then re-baseline + record).
- **D1 verification**: start a fresh :8080 backend (`mvn spring-boot:run` or the repo's run script), then `cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1` and confirm `CONTRACT_VIOL_TURN0` 0/24 on cs015/fg5q.
- **Smoke autoloop iter (CLEAN committed tree!)**: `cd autoloop && python -m autoloop run --experiments 1 --auto-reboot` (or the S-Auto-9 wrapper `scripts/sprint-064-step4-smoke-iter.sh 1`); confirm Step 9 reached AND the per-turn eval trace is readable from `autoloop/results/runs/exp-<N>/`; confirm an infra-error iter is marked `error` not Tier-0 regression.

### §7 stanza (EXEMPT — harness/infra carve-out; self-walked)
- **Target failure layer**: `infra` + `eval_spec`/harness. No agent semantic decision touched. Per §4.1/§7 EXEMPT.
- **Tier-0 invariant**: none added (infra-error classification is the Runtime's existing infra/persistence/trace responsibility, Constitution §1.4).
- **Semantic hardcode**: none. D1 deterministic turn0 uses the case's own seeded content (no keyword/regex/enum); infra-error detection keys on transport/exception signals (`service_degraded` prevalence, exit-code, missing results), not semantic content.
- **Generalization coverage**: target = 3 turn0-flake cases (cs015 p1, fg5q p2/p3) + OQ-S65.8 LLM-deadline pattern; neighbor = any no-seed-messages turn0; negative = cases WITH seed_messages unaffected, and the infra-error path must NOT mask a REAL Tier-0 regression (only transport/deadline/empty-eval signals trigger it); shadow = held-out.

### Codex review plan
Milestone-shared at M-Auto-3 close (default). PER-SUB-SPRINT REQUIRED only if you take a fence-#13 `eval_runner.py` controlled override (§4.3 #3) — STOP-and-surface; deliver-agent authors the prompt.

### Handoff requirements (`docs/sprints/sprint-066-handoff.md`)
§0 summary (scope, commits, final test counts incl. re-established Java baseline); §1 Java baseline (counts + any determinism-attributable delta); §2 D1 (before/after + CONTRACT_VIOL_TURN0 evidence); §3 trace persistence (artefact path + read-back sample); §4 OQ-S65.7/8 infra-error detection (mechanism + smoke evidence); §5 fence-#13 disposition (touched? override authorization + SHA rebaseline if yes); §6 OQs surfaced; §7 self-check tick-off.

### Commit discipline
Multi-commit acceptable. Message: `Sprint 066 / S-Auto-11 / M-Auto-3 — <description>` + footer `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`. No `git add -A`; stage explicitly. Do not push.

## Self-check (verify ALL before claiming done)
- [ ] Java baseline re-established + recorded; any determinism-attributable regression SURFACED (not silently reverted).
- [ ] D1 turn0 deterministic no-seed branch + N=3 parse-retry; `CONTRACT_VIOL_TURN0` 0/24 on cs015/fg5q.
- [ ] Per-iter eval traces persisted to `autoloop/results/runs/exp-<N>/`; a per-turn trace read back.
- [ ] loop.py infra-error detection marks LLM-deadline/service_degraded/failed-eval iters `error`, not Tier-0 regression.
- [ ] Preferred loop.py orchestration; if `eval_runner.py` edited → fence-#13 override authorized + SHA rebaselined + recorded + per-sub-sprint Codex.
- [ ] eval_interactive `486/3` + autoloop ≥`266` (+ new tests) + 17-fixture `31` preserved.
- [ ] No edits to applier.py / sandbox / meta_agent / skills / case_specs / server semantic logic.
- [ ] Smoke iter on a CLEAN committed tree; no `git add -A`; local-Mac only.
- [ ] Handoff §0-§7 filled.
