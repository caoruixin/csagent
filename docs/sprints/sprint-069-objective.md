---
title: Sprint 069 / S-Auto-13b — A3 paraphrase-storm deterministic backstop (faq_miss-state-aware same-turn re-search suppression) (M-Auto-3 sub-sprint 4 of 5)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-02
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-068-objective.md]
superseded_by: null
notes: >
  M-Auto-3 / Sprint 069 / S-Auto-13b. Fourth sub-sprint of M-Auto-3 (Substrate-hygiene), the
  fix-iteration follow-up to S-Auto-13's A3. **Layer**: `infra` (tool-dispatch `faq_miss`-state-aware
  same-turn re-search suppression). **§7 stanza REQUIRED**. **Codex: PER-SUB-SPRINT REQUIRED** (§4.3
  trigger #2 — a deterministic backstop on the LLM-owned "whether to re-search" decision; Codex must
  verify the structural-cardinality justification, that it is keyed on the existing `faq_miss` flag and
  NOT on query content, and that soft-signal-first was genuinely tried first).

  **Why this sub-sprint exists (OQ-S68.3)**: S-Auto-13 shipped the A3 soft layer (a `faq_miss`-keyed
  `grounding_instruction` + a projection echo of the prior search result). It is correct and demonstrably
  FIRES — 15-17 `search_reuse_instruction`/run reach the LLM per-step before each re-search — but
  `deepseek-v4-flash` ignores the soft signal: `PARAPHRASE_STORM` stayed 16/16/7 vs the §11 ≤3 target.
  Per the S-Auto-13 contract's soft-signal-first discipline (Constitution §1.5), the dev did NOT add a
  hard backstop and surfaced OQ-S68.3 for this decision. S-Auto-13b adds the deterministic backstop the
  proposal anticipated (Module A3-cap), but as a **`faq_miss`-state-aware gate** (more targeted than a raw
  count cap): once a turn already has a viable hit, further same-turn re-search is genuinely redundant.

  **This is the SAME justification pattern as A1 (S-Auto-12)**: soft-signal-first was tried and empirically
  falsified, so a deterministic backstop that is the Runtime's existing idempotency/cardinality
  responsibility (Constitution §1.4) is warranted — NOT a §1.5 keyword/regex/enum hardcode. The A3 soft
  layer SHIPPED in S-Auto-13 STAYS (it is the correct first measure); S-Auto-13b adds the backstop beneath it.

  **Distinct from A1**: A1 dedups BYTE-IDENTICAL repeats (same `canonicalArgumentsHash`). A3's storm is
  NON-identical paraphrase re-searches (different hash, same intent) that A1 cannot catch. S-Auto-13b's
  gate keys on the `faq_miss` RESULT state, not on argument identity — so it catches paraphrases. It is
  ADDED ALONGSIDE A1's `successfulDispatchCache` in `AgentRunLoopImpl`; it does NOT modify A1 (A1 is FINALIZED).

  **Inherited substrate at HEAD (do NOT re-fix / revert)**: S-Auto-11 (`loop.py`) + S-Auto-12 (A1 hybrid
  dedup) + S-Auto-13 (A2 `discover_triage` classify-first; A3 soft layer in `resolve_faq_grounded_answer`
  + `ContextProjectionBuilder` echo; OQ-S66.1 goldens reconciled) are FINALIZED. `b351648` determinism
  config in place. fence-#13 scoring SHA `35305bd8…`.

  **CORRECTED baselines (post-S-Auto-13)**: Java **`1192 / 1 / 0 / 2`** (OQ-S66.1 resolved; only the
  inherited tiebreaker remains); eval_interactive **`495 / 8`** (OQ-S68.1 — +4 from the `0323457`
  action_bank split, NOT the agent; a quick test-fix or baseline-accept decision is pending — do NOT treat
  the 4 split-failures as yours); autoloop `276`; 17-fixture `31`; scoring SHA `35305bd8…`.

  Dev session source-of-truth: `compact/sprint-069-dev-prompt.md` (self-contained per §9). Dev reads ONLY
  `AGENTS.md` (auto-loaded) + that prompt; code anchors on demand.
---

# Sprint 069 / S-Auto-13b — A3 paraphrase-storm deterministic backstop

## Class

- **Layer (primary)**: `infra` — a `faq_miss`-state-aware same-turn `search_knowledge` re-search suppression gate in the tool-dispatch path (`AgentRunLoopImpl`), added ALONGSIDE the S-Auto-12 A1 `successfulDispatchCache` (NOT modifying A1) + trace annotation. No UC-routing / drift / escalation-posture decision changes; the LLM still owns whether to search the FIRST time, which tool, and what content.
- **§7 stanza**: **REQUIRED**. Self-walked below.
- **Codex review plan (§4.3)**: **PER-SUB-SPRINT REQUIRED** (trigger #2 — a deterministic backstop on the LLM-owned "whether to re-search" decision is §1.7-adjacent; Codex verifies the structural-cardinality justification + keyed-on-`faq_miss`-not-content + soft-signal-first-was-tried). The deliver-agent authors the Codex prompt at S-Auto-13b close; the verdict lands in `docs/codex-findings.md`.
- **Position in milestone**: 4th of 5 (S-Auto-11 ✅ → S-Auto-12 ✅ → S-Auto-13 ✅-partial → **S-Auto-13b A3 backstop** → S-Auto-14 B1 → M-Auto-3 close). The optional fix-iteration buffer is consumed by this sub-sprint; M-Auto-3 is at the §8.1 5-sub-sprint ceiling.

## Goal

Close the A3 paraphrase-storm to the §11 bar deterministically, since the soft signal (correct, and firing) is ignored by the model. After S-Auto-13b, once a `search_knowledge` in the current turn returns a viable hit (`faq_miss=false`), subsequent same-turn `search_knowledge` re-searches are served from the prior viable hit + trace-annotated instead of re-executing — so the loop stops burning steps re-confirming an answer it already has.

**Acceptance**: on a 3-pass `bad_cases` rerun (sim_temp=0 + bot_temp=0 + 60s deadline + parallel=1, freshly-restarted backend, measured via the S-Auto-11 per-iter trace persistence) `PARAPHRASE_STORM` drops **16/16/7 → ≤3** with negative controls intact (the FIRST search of a turn is never suppressed; a re-search after `faq_miss=true` IS allowed; a search in a different turn/run is not suppressed; a legitimately-distinct needed second search is not wrongly suppressed — STOP-and-surface if it is). Baselines preserved: Java `1192/1/0/2` + new gate tests; eval_interactive `495/8` (the 8 unchanged — the 4 split-failures are OQ-S68.1, not yours); autoloop `276`; 17-fixture `31`; scoring SHA `35305bd8…`.

## Scope (4 steps)

1. **`faq_miss`-state-aware re-search suppression gate** in `server/src/main/java/.../service/runtime/AgentRunLoopImpl.java`, in the dispatch path ALONGSIDE the S-Auto-12 A1 `successfulDispatchCache` (~:171 decl / ~:466-501 回挡 / ~:534-535 cache-put — confirm on read; do NOT modify A1's byte-identical logic). Track, per run (`AgentRunLoop.run` = one turn), the **most-recent `search_knowledge` result's `faq_miss` state** (read it from the dispatched `search_knowledge` `ToolResult` / the value that lands in `ToolEvent.resultData` — the same `faq_miss` flag B1 will read; source `server/src/main/java/.../model/KnowledgeSearchResult.java` + `service/tools/SearchKnowledgeTool.java`). When a NEW `search_knowledge` dispatch occurs and the most-recent prior `search_knowledge` in this run was `faq_miss=false` (viable hit): suppress it — serve the prior viable-hit result (so the LLM still sees the hit), do NOT re-execute the search, do NOT charge a step/budget (mirror A1). **Exception**: if the most-recent `search_knowledge` was `faq_miss=true` (no viable hit), DO allow the re-search (the LLM legitimately needs to try again).

2. **Trace annotation**: annotate each suppressed re-search distinctly from A1 — e.g. `paraphrase_suppressed:true` + `faq_hit_at_step:<n>` on the `ToolEvent` (reuse the S-Auto-12 `ToolEvent` annotation pattern; a distinct reason from `deduplicated`), and flatten onto the persisted `tool_calls` trace via `ControlKernel` (mirror the S-Auto-12 `deduplicated`/`original_at_step` flatten). Load-bearing for downstream report.html/admin trace (`project_observability_debt_pattern`).

3. **Negative controls + tests** (`server/src/test/**`): (a) the FIRST `search_knowledge` of a turn is never suppressed; (b) a re-search after a `faq_miss=true` result IS dispatched (not suppressed); (c) a `search_knowledge` in a NEW run/turn is not suppressed (per-run scope); (d) a suppressed re-search returns the prior viable hit + writes the `paraphrase_suppressed`/`faq_hit_at_step` annotation + charges no step; (e) coexistence with A1 (a byte-identical re-search still hits A1's path; a paraphrase re-search hits the new gate). Validate against the bad_cases that no LEGITIMATELY-DISTINCT needed second search (a genuinely different sub-question after a viable hit) is wrongly suppressed — if one is, STOP-and-surface (the gate may need to allow N>1 distinct searches, i.e. fall back to a count-style cap).

4. **3-pass `bad_cases` measurement + handoff + OQ ledger** (`docs/sprints/sprint-069-handoff.md`), including `PARAPHRASE_STORM` before/after + whether OQ-S68.4 (the byte-identical re-search that escaped A1) is incidentally subsumed.

## Hard fences / STOP conditions

**In scope to edit**: `server/src/main/java/.../service/runtime/AgentRunLoopImpl.java` (the new `faq_miss`-state gate, alongside A1) + `server/src/main/java/.../model/ToolEvent.java` (annotation fields, if the existing `deduplicated`/`originalAtStep` pattern needs a distinct `paraphrase_suppressed` reason) + `server/src/main/java/.../service/runtime/ControlKernel.java` (trace flatten) + `server/src/test/**` (new tests). Read-only: `KnowledgeSearchResult.java` / `SearchKnowledgeTool.java` (faq_miss source); the bad_cases suite (measurement).

**Hard-fenced (do NOT edit)**: the S-Auto-12 A1 byte-identical `successfulDispatchCache` LOGIC (extend the dispatch site with the new gate, do NOT alter A1's behavior); `PhaseEvaluator.resolveMaxStepsReason` (B1 = S-Auto-14); `server/src/main/resources/skills/**` (A2/A3 soft layer FINALIZED in S-Auto-13 — the soft layer STAYS, do NOT remove it); `tool-policy.yaml`; `eval_interactive/case_specs/**` (B1 = S-Auto-14); `eval_interactive/.../user_simulator.py`; the 4 SHA-locked scoring files; `autoloop/autoloop/loop.py` + `sandbox/applier.py` + sandbox + meta_agent + cli.py + preflight.py; `docs/foundational/**`, `docs/current/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/teams/**`; prior sprint/milestone archives. Do NOT "fix" the 4 OQ-S68.1 eval_interactive split-failures here (separate housekeeping; not server-side).

**STOP-and-surface conditions**:
- The gate wrongly suppresses a legitimately-distinct needed second search after a viable hit (a genuinely different sub-question) → halt; the gate may need to allow a small N of distinct searches (count-style cap) rather than hard-suppress after the first viable hit. Surface the trade-off.
- Landing the gate appears to require MODIFYING A1's logic (vs adding alongside) → STOP-and-surface.
- The design seems to need a new Tier-0 invariant → `human_review_required` (do NOT self-invent).
- Any hard-fenced surface needs editing. Do NOT remove the S-Auto-13 A3 soft layer (it stays beneath the backstop). Do NOT revert S-Auto-11/12/13 or `b351648`.
- **No `git add -A`** — stage explicitly. Any `autoloop run` only on a clean committed tree. **Local-Mac only.** Restart the backend after server changes (`mvn spring-boot:run` has no hot-reload) before the bad_cases measurement.

## Test / eval requirements

- **Java**: no NEW failures beyond `1192 / 1 / 0 / 2` (the lone failure is the inherited tiebreaker); `Tests run` rises by the new gate + negative-control tests; `Failures` stays `1`.
- **eval_interactive pytest**: `495 / 8` preserved (the 8 unchanged — the 4 OQ-S68.1 split-failures are not yours; A3 backstop is server-side). If you can cheaply confirm the 8 are the same 8, note it.
- **autoloop pytest**: `276 passed`.
- **17-fixture detector sweep**: `31 passed`.
- **scoring SHA**: held at `35305bd8…`.
- **A3 backstop measurement (deliverable)**: restart `:8080` backend, 3-pass `bad_cases` (`cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1`, ×3 at sim/bot temp=0/60s). Read the persisted per-iter traces; report `PARAPHRASE_STORM` 16/16/7 → ≤3, the `paraphrase_suppressed` annotations visible in-trace, negative controls intact.

## §7 stanza (REQUIRED)

**Target failure layer:** `infra` (a `faq_miss`-state-aware same-turn `search_knowledge` re-search suppression gate in the dispatch path). No agent semantic decision (UC hypothesis, drift, escalation posture, response strategy) is changed; the LLM still owns whether to search first, which tool, and what content.

**Tier-0 invariant:** This sprint adds NO Tier-0 invariant. The gate extends the Runtime's existing idempotency / cardinality / budget responsibility (Constitution §1.4); it is NOT added to `docs/runtime_freeze_and_risk_policy.md` §1/§2. If review argues for elevating "per-turn search cardinality after a viable hit" to Tier-0 → `human_review_required` (do NOT self-invent).

**Semantic hardcode:** None introduced. The gate is a STRUCTURAL state/cardinality backstop keyed on the EXISTING `faq_miss` result flag — NOT on query content, keyword, regex, enum, or per-UC matrix; it makes no semantic judgment about what the LLM searched for. **Justification for the deterministic backstop (anti-hardcode):** soft-signal-first was satisfied — S-Auto-13 shipped the `faq_miss`-keyed soft `grounding_instruction` + projection echo, they demonstrably FIRE (15-17/run), and the model empirically ignored them (`PARAPHRASE_STORM` 16/16/7). This is the SAME falsification → deterministic-backstop pattern as A1 (Sprint 19/20 soft-signal-alone → S-Auto-12 hybrid 回挡). The soft layer STAYS beneath the backstop. Net effect: REMOVES wasted re-searches, adds no semantic rule.

**Generalization coverage:** target = the bad_cases `PARAPHRASE_STORM` subset (16/16/7); neighbor = `anchor_outcome` / shadow same-storm shape; negative = (a) first search of a turn not suppressed, (b) re-search after `faq_miss=true` allowed, (c) cross-turn/new-run search not suppressed, (d) a legitimately-distinct needed second search not wrongly suppressed (STOP-and-surface if so); shadow = held-out (not read by dev). Counts confirmed at handoff via the 3-pass bad_cases rerun (`PARAPHRASE_STORM` 16→≤3).

## Codex review plan (per §4.3)

**PER-SUB-SPRINT REQUIRED** (trigger #2). At S-Auto-13b close the deliver-agent authors `compact/sprint-069-S-Auto-13b-codex-prompt.md` (or the per-sub-sprint equivalent) embedding the §4.1 nine-question kernel; Codex verifies: the gate is keyed on `faq_miss` state not query content (no semantic hardcode); soft-signal-first was genuinely tried in S-Auto-13 (the falsification evidence); the gate does not suppress legitimate distinct searches; A1 was not modified. Verdict → `docs/codex-findings.md`. (This sub-sprint's Codex is SEPARATE from the milestone-shared close Codex; the milestone close still bundles the cumulative range.)

## Handoff requirements

`docs/sprints/sprint-069-handoff.md` at close. Mandatory: §0 summary (scope, commits, final counts incl. Java `1192/1`+new tests); §1 the `faq_miss`-state gate mechanism (where it sits relative to A1; the most-recent-faq_miss tracking; suppression + step/budget non-charge); §2 trace annotation (`paraphrase_suppressed`/`faq_hit_at_step` before/after); §3 negative controls (first search / faq_miss=true re-search / cross-turn / distinct-need / A1 coexistence); §4 `PARAPHRASE_STORM` 3-pass bad_cases before/after + OQ-S68.4 subsumption note; §5 baselines + §7-stanza self-walk + fence disposition; §6 OQs surfaced; §7 self-check tick-off. (Codex is per-sub-sprint — note its dispatch status.)

## Commit discipline

Multi-commit acceptable (gate / trace / tests / handoff). Commit message: `Sprint 069 / S-Auto-13b / M-Auto-3 — <description>` + standard footer `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`. No `git add -A` — stage explicitly. Do not push.

## Self-check (dev MUST verify before claiming done)

- [ ] `faq_miss`-state gate added in `AgentRunLoopImpl` ALONGSIDE A1 (A1 byte-identical logic UNCHANGED); tracks most-recent `search_knowledge` `faq_miss` per run; suppresses same-turn re-search after `faq_miss=false`; serves prior hit; no step/budget charged.
- [ ] Exception correct: re-search after `faq_miss=true` is ALLOWED (not suppressed).
- [ ] Trace annotation `paraphrase_suppressed:true` + `faq_hit_at_step` on every suppression (distinct from A1's `deduplicated`); flattened onto `tool_calls` trace.
- [ ] Negative controls pass: first search not suppressed; `faq_miss=true` re-search dispatched; cross-turn search not suppressed; distinct-need second search validated (STOP-and-surfaced if wrongly suppressed); A1 coexistence holds.
- [ ] `PARAPHRASE_STORM` 16/16/7 → ≤3 on a 3-pass bad_cases rerun (backend restarted; via S-Auto-11 persisted traces); OQ-S68.4 subsumption checked.
- [ ] The S-Auto-13 A3 soft layer (grounding_instruction + projection echo) was NOT removed (stays beneath the backstop).
- [ ] Java no NEW failures beyond `1192/1/0/2` (+ new gate tests); did NOT touch the OQ-S68.1 eval split-failures.
- [ ] eval_interactive `495/8` + autoloop `276` + 17-fixture `31` + scoring SHA `35305bd8…` preserved.
- [ ] No edits to A1 logic / PhaseEvaluator resolver / skills / case_specs / user_simulator / loop.py / scoring / applier.py / sandbox / meta_agent; no Tier-0 self-invented; no semantic-content keying.
- [ ] No `git add -A`; any autoloop run on a clean committed tree; backend restarted for measurement; local-Mac only.
- [ ] Handoff §0-§7 filled; per-sub-sprint Codex dispatch status noted.
