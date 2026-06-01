# Dev prompt — Sprint 068 / S-Auto-13 / M-Auto-3 (self-contained per `iteration_governance.md` §9)

You are the **dev agent (Claude Code)** for **Sprint 068 / S-Auto-13**, the third sub-sprint of milestone **M-Auto-3 — Substrate-hygiene (clean the autoloop fitness signal)**.

**One-line goal**: close the two remaining upstream step-wasters at the skill/projection layer — the DISCOVER **gating-race** (A2: make `discover_triage` classify-first, matching the existing tool-policy) and the RESOLVE **paraphrase-storm** (A3: a soft `faq_miss`-keyed grounding instruction + projection echo so the LLM stops re-searching after a viable hit). Both are manual skill-layer fixes (no runtime semantic-decision logic). As an in-scope consequence of editing both drift-affected skills, reconcile the OQ-S66.1 `max_tool_steps` goldens.

## Read order (minimal)
- `AGENTS.md` (auto-loaded — governance chain). Do NOT read other docs to start.
- THIS prompt — it embeds the full contract. Read code anchors below on demand.

## CRITICAL operational constraints (top-of-mind)
- **Branch**: `auto-loop-branch`. **Local-Mac only**.
- **CORRECTED baselines (post-S-Auto-12)**: Java **`Tests run: 1186, Failures: 10, Errors: 0, Skipped: 2`** — the 10 = the OQ-S66.1 pre-existing `max_tool_steps` golden drift (9: `resolve_faq_grounded_answer.yaml` 6 vs golden 4; `discover_triage.yaml` 3 vs golden 2) + the inherited `SystemPromptUserRequestedTiebreakerTest` (1). **S-Auto-13 is the place to clear the 9** (step #3 below). eval_interactive **`499 passed, 4 failed`** (4 pre-existing). autoloop **`276 passed`**. 17-fixture **`31`**. scoring SHA **`35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8`**.
- **Inherited substrate (at HEAD; do NOT re-fix / revert)**: S-Auto-11 (`autoloop/autoloop/loop.py` trace persistence + infra-error detection — FINALIZED) + S-Auto-12 (A1 hybrid dedup in `AgentRunLoopImpl`/`ToolEvent`/`ControlKernel`/`system_prompt.txt` — FINALIZED; do NOT touch). `b351648` determinism config in place (bot/sim temp→0, LLM deadline 60s). fence-#13 scoring files untouched.
- **Manual delivery**: A2/A3 edit autoloop-mutable skill fields (`$.procedure` / `$.grounding_instruction`) BY HAND. This does NOT widen the autoloop mutable surface (Stage-2 is a separate post-M-Auto-3 decision). Do NOT route these through the autoloop.
- **Clean-tree discipline (`project_autoloop_dirty_index_hazard`)**: any `autoloop run` only on a clean committed tree. Never `git add -A`; stage explicitly.
- **Restart backend after skill/server changes**: `mvn spring-boot:run` has NO hot-reload — the bad_cases measurement MUST run against a freshly-restarted `:8080` backend.
- **macOS proxy (`reference_macos_proxy_httpx_localhost`)**: localhost HTTP failures = the system proxy at `127.0.0.1:7890`; `eval_interactive` agent_client already disables it.

## Code anchors (verified at HEAD; note `service/tools/` NOT `service/runtime/` — the proposal's paths were stale)
- **A2 gating-race**:
  - `server/src/main/resources/skills/discover_triage.yaml` — `tool_calls.allowed` lists both `search_knowledge` + `classify_use_case`; `$.procedure` (the big block) carries the Sprint-7 §I0 weak-candidate cue instructing `search_knowledge` THEN `classify_use_case`; the `behaviors` list has `faq-uc-search-before-commit` with `trace_check: "tool_event_seq(search_knowledge) < tool_event_seq(classify_use_case)"` (REWARDS search-before-classify — the contradiction); `max_tool_steps: 3`.
  - `server/src/main/resources/config/tool-policy.yaml` — `search_knowledge.allowed-ucs: [UC-A..UC-FP]` (NO `none`/DISCOVER); `classify_use_case.allowed-ucs: [ALL]`. The asymmetry: a search in DISCOVER (activeUseCase=none) is rejected.
  - `server/src/main/java/com/gumtree/csagent/service/tools/ToolDispatcher.java:97-101` — reads `session.getActiveUseCase()` (`:97`), calls `policyEnforcer.isToolAllowed(...)` (`:98`), rejects "Tool '%s' is not allowed for use case '%s'" (`:101`, `none` when null).
  - `server/src/main/java/com/gumtree/csagent/service/tools/ToolPolicyEnforcer.java:65` — `isToolAllowed`; `:71` the `ALL` short-circuit.
- **A3 paraphrase-storm**:
  - `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml` — `$.grounding_instruction` (~:31, already says "you MUST call search_knowledge first if accumulated_tool_results.search_knowledge is empty"); `$.escalation_policy` (~:32 references `faq_miss_threshold_exceeded`); `max_tool_steps: 6`.
  - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` — existing search-echo / anti-re-search snippet ~:738 ("Do NOT call search_knowledge again."); `accumulated_tool_results` echo ~:847. Coordinate A3's echo with these (extend, don't duplicate).
  - `faq_miss` flag origin: `server/src/main/java/com/gumtree/csagent/model/KnowledgeSearchResult.java` + `service/tools/SearchKnowledgeTool.java`.

**Distinction from S-Auto-12 A1**: A1 deduped BYTE-IDENTICAL repeats (same `canonicalArgumentsHash`). A3 targets NON-identical PARAPHRASE repeats (different hash, same intent) — A1 cannot catch these, so A3 is genuinely needed.

## Embedded contract

### Class
- **Layer (primary)**: `prompt_projection`/skill (A2 procedure; A3 grounding_instruction + projection echo) + config-governance (tool-policy alignment context) + `semantic_planner` (A3 — LLM owns whether to re-search). No new runtime decision logic.
- **§7 stanza**: **REQUIRED** (semantic-touching). Self-walked below.
- **Codex**: milestone-shared (default). UPGRADES to per-sub-sprint ONLY if you add an A3 hard-cap backstop or the OQ-S66.1 golden reconciliation raises a §1.7/§5.4 concern → STOP-and-surface.

### Scope (4 steps)

**1. A2 — `discover_triage.yaml` classify-first.** Edit `$.procedure` so DISCOVER classifies FIRST (do not instruct `search_knowledge` before `classify_use_case` while activeUseCase=none); the weak-candidate FAQ path gathers enough to classify toward the right FAQ-path UC, then RESOLVE runs the grounded search. Reconcile the `faq-uc-search-before-commit` behavior (its trace_check rewards search<classify — the contradiction that drives the gating-race). Align with the existing `tool-policy.yaml` (`classify=[ALL]`, `search` needs a UC). **PREFER NOT to edit `tool-policy.yaml`** (this is A2-A1, skill-only); if you think the policy must change instead (A2-A2), **STOP-and-surface** — it is a different trade-off. Manual dev.

**2. A3 — `resolve_faq_grounded_answer.yaml` paraphrase discipline** + projection echo. Add to `$.grounding_instruction`: "after a `search_knowledge` returns a viable hit (`faq_miss=false`), do NOT re-search this turn — draft from existing hits via `resolve_article`, or escalate; a fresh search is only warranted if the prior result was `faq_miss=true` or the query is materially different." Ensure `ContextProjectionBuilder` echoes the prior search result (coordinate with ~:738/:847) so the LLM need not re-search to confirm. **Soft-signal-first (§1.5)** — do NOT add a hard search-count cap as the primary fix; if soft-signal + echo are insufficient, **STOP-and-surface** before adding an A3-cap.

**3. OQ-S66.1 `max_tool_steps` golden reconciliation (in-scope consequence).** You are editing both drift-affected skills — reconcile their goldens (the 9 pre-existing `PhaseEvaluator` failures: `PhaseEvaluatorPlanTest` ×2 + `PhaseEvaluatorResolveSkillIntegrationTest` ×7; golden asserts 4/2, shipped 6/3). Pick the **shipped YAML value as source of truth** (re-derive if A2/A3 changed the step count) and update the `PhaseEvaluator` golden TESTS in `server/src/test/**` to match. Target Java `Failures: 10 → 1` (the tiebreaker remains). **STOP-and-surface** if a golden encodes an intended cap (more than a stale-value sync). (You may edit the golden TEST expectations — NOT `PhaseEvaluator.resolveMaxStepsReason` logic, which is B1/S-Auto-14.)

**4. 3-pass `bad_cases` measurement + handoff** → `docs/sprints/sprint-068-handoff.md`.

### Hard fences / STOP conditions
- **In scope**: `discover_triage.yaml` (A2), `resolve_faq_grounded_answer.yaml` (A3), `ContextProjectionBuilder.java` (A3 echo, only if existing insufficient), `server/src/test/**` (OQ-S66.1 goldens + new A2/A3 tests). `tool-policy.yaml` only if A2 truly requires (STOP-and-surface first).
- **Hard-fenced (do NOT edit)**: `PhaseEvaluator.resolveMaxStepsReason` LOGIC (B1=S-Auto-14; golden TEST expectations are OK for OQ-S66.1); `AgentRunLoopImpl`/`ToolEvent`/`ControlKernel`/`system_prompt.txt` (S-Auto-12 FINALIZED); `eval_interactive/case_specs/**` (B1=S-Auto-14); `user_simulator.py` (D1 done); the 4 SHA-locked scoring files; `loop.py` (S-Auto-11) + `applier.py` + sandbox + meta_agent + cli.py + preflight.py; `docs/foundational/**`, `docs/current/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/teams/**`; prior archives.
- **STOP-and-surface**: A2/A3 risk changing UC-routing/escalation/drift semantics beyond the storm fix (those are the LLM's per §1.3 — only re-order/soft-guide); the OQ-S66.1 golden is more than a stale-value sync; A3 needs a hard cap; A2 needs `tool-policy.yaml`; any hard-fence touch. Do NOT revert S-Auto-11/12 or `b351648`. No `git add -A`; clean-tree for autoloop; restart backend before measurement; local-Mac only.

### Test / eval requirements (commands)
- **Java**: `mvn -q -pl server test` — from `1186/10/0/2`, REDUCE failures via the golden reconciliation → target `Failures: 1` + new A2/A3 tests (`Tests run` rises). No red tests left (if A2/A3 shift a skill's `max_tool_steps`, update its golden same-sub-sprint).
- **eval_interactive pytest**: `cd eval_interactive && uv run python -m pytest --tb=no -q` → `499 passed, 4 failed` (unchanged).
- **autoloop pytest**: `cd autoloop && uv run --extra dev pytest -q` → `276 passed`.
- **17-fixture sweep**: `cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py` → `31 passed`.
- **scoring SHA**: `cd autoloop && uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"` → `35305bd8...`.
- **A2/A3 measurement (deliverable)**: restart `:8080` backend, then 3× `cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1` (sim/bot temp=0/60s). Read persisted per-iter traces; report `GATING_RACE` 4→≤1 (DISCOVER search "not allowed for use case 'none'" rejections) + `PARAPHRASE_STORM` 11/24→≤3 (non-identical re-searches after a `faq_miss=false` hit), negative controls intact.

### §7 stanza (REQUIRED)
- **Target failure layer**: `prompt_projection`/skill (A2 procedure; A3 grounding_instruction + projection echo) + `semantic_planner` (A3) + config-governance (tool-policy alignment). No runtime semantic-decision logic added.
- **Tier-0 invariant**: none added.
- **Semantic hardcode**: none. A2 RE-ORDERS the skill procedure to match the EXISTING tool-policy (removes a self-contradiction; no keyword/regex/enum/per-UC matrix; LLM still owns the UC choice §1.3). A3 is a soft `faq_miss`-keyed grounding instruction + projection echo (LLM owns whether to re-search; soft-signal-first §1.5); any hard cap is a structural cardinality backstop, last-resort, STOP-and-surface first. OQ-S66.1 aligns a stale TEST expectation to shipped behavior. Net: net-LOWERS the hardcode surface (removes a contradicting cue).
- **Generalization coverage**: target = `GATING_RACE` (4) + `PARAPHRASE_STORM` (11/24); neighbor = anchor_outcome/shadow same shape; negative = distinct RESOLVE searches not suppressed, `faq_miss=true` re-search allowed, DISCOVER clarify-then-classify not broken; shadow = held-out.

### Codex review plan
Milestone-shared at M-Auto-3 close (default). PER-SUB-SPRINT only if an A3 hard-cap is added or the OQ-S66.1 golden reconciliation raises §1.7/§5.4 concern — STOP-and-surface.

### Handoff requirements (`docs/sprints/sprint-068-handoff.md`)
§0 summary (scope, commits, final counts incl. post-reconciliation Java baseline); §1 A2 (before/after procedure + `faq-uc-search-before-commit` disposition + GATING_RACE evidence); §2 A3 (grounding_instruction + projection echo before/after + PARAPHRASE_STORM evidence + no-hard-cap confirmation or STOP record); §3 negative controls; §4 OQ-S66.1 reconciliation (goldens + shipped values + Java before/after); §5 baselines + §7-stanza self-walk + fence disposition; §6 OQs surfaced; §7 self-check tick-off.

### Commit discipline
Multi-commit acceptable. Message: `Sprint 068 / S-Auto-13 / M-Auto-3 — <description>` + footer `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`. No `git add -A`; stage explicitly. Do not push.

## Self-check (verify ALL before claiming done)
- [ ] A2: `discover_triage.yaml` classifies-first; `faq-uc-search-before-commit` search-before-classify reward reconciled; aligned with tool-policy (NOT edited, or STOP-and-surfaced); `GATING_RACE` 4→≤1.
- [ ] A3: `resolve_faq_grounded_answer.yaml` grounding_instruction + projection echo discourage re-search after `faq_miss=false`; soft-signal-first (NO hard cap, or STOP-and-surfaced); `PARAPHRASE_STORM` 11/24→≤3.
- [ ] Negative controls: distinct RESOLVE searches not suppressed; `faq_miss=true` re-search allowed; DISCOVER clarify-then-classify not broken.
- [ ] OQ-S66.1: discover_triage + resolve_faq `max_tool_steps` goldens reconciled to shipped values; Java `Failures 10 → 1`; no red tests left.
- [ ] eval_interactive `499/4` + autoloop `276` + 17-fixture `31` + scoring SHA `35305bd8…` preserved.
- [ ] No edits to PhaseEvaluator resolver logic / AgentRunLoopImpl / ToolEvent / system_prompt / case_specs / user_simulator / loop.py / scoring / applier.py / sandbox / meta_agent; A2/A3 manual; no Tier-0; no semantic hardcode (no A3 hard-cap without surfacing).
- [ ] No `git add -A`; autoloop run on a clean committed tree; backend restarted for measurement; local-Mac only.
- [ ] Handoff §0-§7 filled.
