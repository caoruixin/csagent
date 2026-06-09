# Dev prompt — Sprint 069 / S-Auto-13b / M-Auto-3 (self-contained per `iteration_governance.md` §9)

You are the **dev agent (Claude Code)** for **Sprint 069 / S-Auto-13b**, the fourth sub-sprint of milestone **M-Auto-3 — Substrate-hygiene (clean the autoloop fitness signal)** — the deterministic backstop follow-up to S-Auto-13's A3.

**One-line goal**: kill the RESOLVE paraphrase-storm that the S-Auto-13 soft signal could not (the model ignores it) — add a deterministic **`faq_miss`-state-aware same-turn re-search suppression** gate in the dispatch path: once a `search_knowledge` in the current turn returns a viable hit (`faq_miss=false`), subsequent same-turn `search_knowledge` re-searches are served from the prior hit + trace-annotated instead of re-executing. Keyed on the existing `faq_miss` flag, NOT query content. No keyword/regex/per-UC.

## Read order (minimal)
- `AGENTS.md` (auto-loaded — governance chain). Do NOT read other docs to start.
- THIS prompt — it embeds the full contract. Read code anchors below on demand.

## CRITICAL operational constraints (top-of-mind)
- **Branch**: `auto-loop-branch`. **Local-Mac only**.
- **Why this exists (OQ-S68.3)**: S-Auto-13 shipped the A3 SOFT layer (a `faq_miss`-keyed `grounding_instruction` in `resolve_faq_grounded_answer.yaml` + a projection echo in `ContextProjectionBuilder`). It is correct and FIRES (15-17 `search_reuse_instruction`/run) but `deepseek-v4-flash` ignores it — `PARAPHRASE_STORM` stayed **16/16/7** vs the §11 ≤3 target. Per the soft-signal-first rule the S-Auto-13 dev did NOT add a hard backstop and surfaced this. **The soft layer STAYS** (it's the correct first measure); you add the deterministic backstop BENEATH it. This is the SAME pattern as A1 (Sprint 19/20 soft-signal-alone falsified → S-Auto-12 hybrid 回挡).
- **Distinct from A1**: A1 (S-Auto-12) dedups BYTE-IDENTICAL repeats (same `canonicalArgumentsHash`). The paraphrase storm is NON-identical (different hash, same intent) — A1 can't catch it. Your gate keys on the `faq_miss` RESULT state, not arg identity. **Add it ALONGSIDE A1's `successfulDispatchCache`; do NOT modify A1 (FINALIZED).**
- **CORRECTED baselines (post-S-Auto-13)**: Java **`Tests run: 1192, Failures: 1, Errors: 0, Skipped: 2`** (S-Auto-13 RESOLVED OQ-S66.1 — only the inherited `SystemPromptUserRequestedTiebreakerTest` remains). eval_interactive **`495 passed, 8 failed`** — **OQ-S68.1**: 4 of the 8 are the 2026-06-01 `0323457` action_bank split (governance/lint tests reading `action_bank.md` rows that moved to `action_bank_archive.md`), NOT the agent and NOT yours to fix here. autoloop **`276`**. 17-fixture **`31`**. scoring SHA **`35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8`**.
- **Inherited substrate (do NOT re-fix / revert)**: S-Auto-11 (`loop.py`) + S-Auto-12 (A1 dedup) + S-Auto-13 (A2 classify-first; A3 soft layer; OQ-S66.1 goldens) FINALIZED. `b351648` determinism config. fence-#13 SHA untouched.
- **Clean-tree (`project_autoloop_dirty_index_hazard`)**: any `autoloop run` on a clean committed tree. Never `git add -A`.
- **Restart backend after server changes**: `mvn spring-boot:run` has NO hot-reload — the bad_cases measurement MUST run against a freshly-restarted `:8080` backend.
- **macOS proxy (`reference_macos_proxy_httpx_localhost`)**: localhost HTTP failures = the system proxy at `127.0.0.1:7890`; `eval_interactive` agent_client already disables it.

## Code anchors (verify on read; `AgentRunLoopImpl` unchanged since S-Auto-12)
- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` — the S-Auto-12 A1 region: per-run `Map<String,ToolEvent> successfulDispatchCache` decl ~:171; the byte-identical 回挡 ~:466-501 (computes `dedupKey`, checks cache, on hit builds `ToolEvent.deduplicated(...)` + `continue`s); cache-put (success-only, `putIfAbsent`) ~:534-535. **Add your `faq_miss`-state gate in this same dispatch region, alongside (not inside) the A1 cache logic.**
- `server/src/main/java/com/gumtree/csagent/model/ToolEvent.java` — S-Auto-12 added `deduplicated` / `originalAtStep` record fields + a `deduplicated(...)` factory + an 8-arg back-compat ctor. Add a distinct `paraphrase_suppressed` reason (a new field/factory, or reuse the annotation shape with a distinct flag) — do NOT overload `deduplicated`.
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java` — ~:1859-1862 flattens `deduplicated`/`original_at_step` onto the persisted `bot_turns.tool_calls` trace. Flatten your `paraphrase_suppressed`/`faq_hit_at_step` the same way.
- **faq_miss source**: `server/src/main/java/com/gumtree/csagent/model/KnowledgeSearchResult.java` + `service/tools/SearchKnowledgeTool.java` — the `faq_miss` flag that lands in the dispatched `search_knowledge` `ToolResult` / `ToolEvent.resultData` (the same flag B1/S-Auto-14 will read). Read the most-recent `search_knowledge` result's `faq_miss` from the dispatch result in `AgentRunLoopImpl`.

## Embedded contract

### Class
- **Layer**: `infra` (`faq_miss`-state-aware same-turn `search_knowledge` re-search suppression gate, alongside A1) + trace annotation. No agent semantic decision changed; the LLM still owns the FIRST search, which tool, what content.
- **§7 stanza**: **REQUIRED**. Self-walked below.
- **Codex**: **PER-SUB-SPRINT REQUIRED** (§4.3 #2 — deterministic backstop on the LLM-owned "whether to re-search" decision). You ship the code + handoff; the deliver-agent dispatches Codex at close.

### Scope (4 steps)

**1. The `faq_miss`-state gate** in `AgentRunLoopImpl` dispatch path, ALONGSIDE A1's `successfulDispatchCache` (do NOT modify A1's byte-identical logic). Track per run (`AgentRunLoop.run` = one turn) the **most-recent `search_knowledge` result's `faq_miss` state**. When a NEW `search_knowledge` dispatch occurs and the most-recent prior `search_knowledge` this run was `faq_miss=false` (viable hit): **suppress** — serve the prior viable-hit result (the LLM still sees the hit), do NOT re-execute, do NOT charge a step/budget (mirror A1). **Exception**: if the most-recent `search_knowledge` was `faq_miss=true` (no viable hit), ALLOW the re-search.

**2. Trace annotation**: `paraphrase_suppressed:true` + `faq_hit_at_step:<n>` on the `ToolEvent` (distinct from A1's `deduplicated`); flatten onto the `tool_calls` trace via `ControlKernel` (mirror the S-Auto-12 flatten). Load-bearing for report.html/admin trace (`project_observability_debt_pattern`).

**3. Negative controls + tests** (`server/src/test/**`): (a) FIRST search of a turn not suppressed; (b) re-search after `faq_miss=true` IS dispatched; (c) cross-turn/new-run search not suppressed; (d) a suppressed re-search returns the prior hit + writes the annotation + charges no step; (e) A1 coexistence (byte-identical → A1 path; paraphrase → new gate). **Validate against the bad_cases that no LEGITIMATELY-DISTINCT needed second search (a genuinely different sub-question after a viable hit) is wrongly suppressed — if one is, STOP-and-surface** (the gate may need to allow a small N of distinct searches rather than hard-suppress after the first hit).

**4. 3-pass `bad_cases` measurement + handoff** (`docs/sprints/sprint-069-handoff.md`), incl. whether OQ-S68.4 (the byte-identical re-search that escaped A1) is incidentally subsumed.

### Hard fences / STOP conditions
- **In scope**: `AgentRunLoopImpl.java` (new gate, alongside A1) + `ToolEvent.java` (distinct `paraphrase_suppressed` annotation) + `ControlKernel.java` (trace flatten) + `server/src/test/**`. Read-only: `KnowledgeSearchResult.java`/`SearchKnowledgeTool.java` (faq_miss source); bad_cases (measurement).
- **Hard-fenced (do NOT edit)**: A1's byte-identical `successfulDispatchCache` LOGIC (extend the dispatch site, don't alter A1); `PhaseEvaluator.resolveMaxStepsReason` (B1=S-Auto-14); `skills/**` (the S-Auto-13 A3 soft layer STAYS — do NOT remove it); `tool-policy.yaml`; `eval_interactive/case_specs/**` (B1); `user_simulator.py`; the 4 SHA-locked scoring files; `loop.py`/`applier.py`/sandbox/meta_agent/cli.py/preflight.py; `docs/foundational/**`, `docs/current/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/teams/**`; prior archives. Do NOT "fix" the 4 OQ-S68.1 eval split-failures here.
- **STOP-and-surface**: the gate wrongly suppresses a legitimately-distinct needed second search (→ maybe a small-N count cap instead of hard-suppress); landing it requires MODIFYING A1; it seems to need a new Tier-0 (`human_review_required`); any hard-fence touch. Do NOT remove the S-Auto-13 soft layer; do NOT revert S-Auto-11/12/13 or `b351648`. No `git add -A`; clean-tree for autoloop; restart backend; local-Mac only.

### Test / eval requirements (commands)
- **Java**: `mvn -q -pl server test` — no NEW failures beyond `1192/1/0/2`; `Tests run` rises by new gate/negative-control tests; `Failures` stays `1`.
- **eval_interactive pytest**: `cd eval_interactive && uv run python -m pytest --tb=no -q` → `495 passed, 8 failed` (the 8 unchanged — the 4 OQ-S68.1 split-failures are not yours; note if they're the same 8).
- **autoloop pytest**: `cd autoloop && uv run --extra dev pytest -q` → `276 passed`.
- **17-fixture sweep**: `cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py` → `31 passed`.
- **scoring SHA**: `cd autoloop && uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"` → `35305bd8...`.
- **A3 backstop measurement (deliverable)**: restart `:8080`, then 3× `cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1` (sim/bot temp=0/60s). Read persisted per-iter traces; report `PARAPHRASE_STORM` 16/16/7 → ≤3, `paraphrase_suppressed` annotations visible, negative controls intact.

### §7 stanza (REQUIRED)
- **Target failure layer**: `infra` (`faq_miss`-state-aware re-search suppression gate). No agent semantic decision changed.
- **Tier-0 invariant**: none added — extends the Runtime's existing idempotency/cardinality/budget responsibility (§1.4); NOT added to `runtime_freeze_and_risk_policy.md` §1/§2. Tier-0 elevation argument → `human_review_required`.
- **Semantic hardcode**: none. The gate is a STRUCTURAL state/cardinality backstop keyed on the EXISTING `faq_miss` result flag — NOT on query content/keyword/regex/enum/per-UC; no semantic judgment of what was searched. Justification: soft-signal-first satisfied — S-Auto-13 shipped the soft `faq_miss`-keyed instruction + echo, they FIRE (15-17/run), the model ignored them (`PARAPHRASE_STORM` 16/16/7) → same falsification→deterministic-backstop pattern as A1. The soft layer STAYS beneath. Net: removes wasted re-searches, adds no semantic rule.
- **Generalization coverage**: target = bad_cases `PARAPHRASE_STORM` (16/16/7); neighbor = anchor_outcome/shadow same shape; negative = first-search / `faq_miss=true` re-search / cross-turn / distinct-need (STOP-and-surface if wrongly suppressed); shadow = held-out.

### Codex review plan
**PER-SUB-SPRINT REQUIRED** (§4.3 #2). You ship code + handoff; the deliver-agent authors the Codex prompt at close (verifies: keyed on `faq_miss` not content; soft-first was tried; no legit-search suppression; A1 unmodified). This is separate from the milestone-shared close Codex.

### Handoff requirements (`docs/sprints/sprint-069-handoff.md`)
§0 summary (scope, commits, counts incl. Java `1192/1`+new tests); §1 the `faq_miss`-state gate mechanism (placement vs A1; most-recent-faq_miss tracking; suppression + no step/budget); §2 trace annotation before/after; §3 negative controls (first / faq_miss=true / cross-turn / distinct-need / A1 coexistence); §4 `PARAPHRASE_STORM` 3-pass before/after + OQ-S68.4 subsumption; §5 baselines + §7-stanza self-walk + fence disposition; §6 OQs surfaced; §7 self-check. Note per-sub-sprint Codex dispatch status.

### Commit discipline
Multi-commit acceptable. Message: `Sprint 069 / S-Auto-13b / M-Auto-3 — <description>` + footer `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`. No `git add -A`; stage explicitly. Do not push.

## Self-check (verify ALL before claiming done)
- [ ] `faq_miss`-state gate added in `AgentRunLoopImpl` ALONGSIDE A1 (A1 logic UNCHANGED); tracks most-recent `search_knowledge` `faq_miss` per run; suppresses same-turn re-search after `faq_miss=false`; serves prior hit; no step/budget charged.
- [ ] Exception: re-search after `faq_miss=true` is ALLOWED.
- [ ] Trace `paraphrase_suppressed:true` + `faq_hit_at_step` (distinct from A1's `deduplicated`); flattened onto `tool_calls`.
- [ ] Negative controls pass: first search not suppressed; `faq_miss=true` re-search dispatched; cross-turn not suppressed; distinct-need validated (STOP-and-surfaced if wrongly suppressed); A1 coexistence holds.
- [ ] `PARAPHRASE_STORM` 16/16/7 → ≤3 on a 3-pass bad_cases rerun (backend restarted); OQ-S68.4 subsumption checked.
- [ ] The S-Auto-13 A3 soft layer was NOT removed (stays beneath the backstop).
- [ ] Java no NEW failures beyond `1192/1/0/2` (+ new tests); did NOT touch the OQ-S68.1 eval split-failures.
- [ ] eval_interactive `495/8` + autoloop `276` + 17-fixture `31` + scoring SHA `35305bd8…` preserved.
- [ ] No edits to A1 logic / PhaseEvaluator / skills / case_specs / user_simulator / loop.py / scoring / applier.py / sandbox / meta_agent; no Tier-0 self-invented; no semantic-content keying.
- [ ] No `git add -A`; autoloop run on a clean committed tree; backend restarted for measurement; local-Mac only.
- [ ] Handoff §0-§7 filled; per-sub-sprint Codex dispatch status noted.
