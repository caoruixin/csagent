---
title: Sprint 068 / S-Auto-13 — A2 classify-first gating-race + A3 paraphrase-storm skill-layer discipline (M-Auto-3 sub-sprint 3 of ~4)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-01
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-067-objective.md]
superseded_by: null
notes: >
  M-Auto-3 / Sprint 068 / S-Auto-13. Third sub-sprint of M-Auto-3 (Substrate-hygiene).
  **Layer**: `prompt_projection`/skill (A2 discover_triage procedure; A3 grounding_instruction
  + projection echo) + config-governance (tool-policy alignment context) + `semantic_planner`
  (A3 — LLM owns whether to re-search). **§7 stanza REQUIRED** (semantic-touching: skill soft-fields
  + projection soft signals shape UC routing + search discipline). **Codex**: milestone-shared (default)
  at M-Auto-3 close — A2/A3 are skill soft-fields + projection (no Tier-0, no §1.7 red line). UPGRADES
  to per-sub-sprint ONLY if an A3 hard-cap or the OQ-S66.1 golden reconciliation raises a §1.7 concern →
  STOP-and-surface.

  Both fixes are delivered by MANUAL dev (NOT autoloop-generated) per milestone §4 non-goal — editing
  `$.procedure` / `$.grounding_instruction` manually does NOT widen the autoloop mutable surface (Stage-2
  remains a separate post-M-Auto-3 decision). After the skill edits, the autoloop baseline_dir may need
  alignment at M-Auto-3 close (proposal §9 risk row — not this sub-sprint).

  **A2 (gating-race, proposal §3 A2-A1)**: the root cause is two configs contradicting each other, NOT the
  dispatcher. `discover_triage.yaml` instructs/rewards search-before-classify in the weak-candidate FAQ
  path (Sprint-7 §I0 procedure cue + the `faq-uc-search-before-commit` behavior whose trace_check rewards
  `tool_event_seq(search_knowledge) < tool_event_seq(classify_use_case)`), but `tool-policy.yaml` gates
  `search_knowledge` to `[UC-A..UC-FP]` (NO `none`/DISCOVER) while `classify_use_case` is `[ALL]` — so a
  search in DISCOVER (activeUseCase=none) is rejected "Tool 'search_knowledge' is not allowed for use case
  'none'" (`ToolDispatcher.java:97-101` → `ToolPolicyEnforcer.isToolAllowed:65`), wasting a step. A2 makes
  the skill tell the truth (classify FIRST, then search in RESOLVE), aligning the skill with the existing
  tool-policy. Zero runtime code.

  **A3 (paraphrase-storm, proposal §3 A3)**: distinct from S-Auto-12 A1 — A1 deduped BYTE-IDENTICAL repeats;
  A3 targets NON-identical paraphrase repeats (different `canonicalArgumentsHash`, same intent) that A1's
  hash cannot catch. Fix: a soft grounding instruction (keyed on the EXISTING `faq_miss` flag) + a projection
  echo of the prior search result so the LLM need not re-search to confirm. Soft-signal-first (§1.5); a hard
  cap is a last-resort backstop only (STOP-and-surface before adding).

  **OQ-S66.1 reconciliation (in-scope consequence)**: S-Auto-13 edits BOTH `discover_triage.yaml` (A2) and
  `resolve_faq_grounded_answer.yaml` (A3) — exactly the two skills whose `max_tool_steps` goldens drifted
  (the 9 pre-existing `PhaseEvaluator` failures in the Java `1186/10/0/2` baseline). Reconcile the goldens
  here (pick the shipped YAML value as source of truth; update the `PhaseEvaluator` golden tests to match),
  aiming to take the Java baseline from 10 failures toward 1 (the inherited tiebreaker). STOP-and-surface if
  the entanglement is larger than a golden-value sync.

  **Inherited substrate at HEAD (do NOT re-fix / do NOT revert)**: S-Auto-11 (`loop.py` trace persistence +
  infra-error detection) + S-Auto-12 (A1 hybrid dedup in `AgentRunLoopImpl`/`ToolEvent`/`ControlKernel`/
  `system_prompt.txt`) are FINALIZED. `b351648` determinism config in place. fence-#13 scoring SHA `35305bd8…`.

  **CORRECTED baselines (post-S-Auto-12)**: Java `1186/10/0/2` (S-Auto-12 +3 A1 tests; the 10 are the
  OQ-S66.1 golden drift (9) + tiebreaker (1) — S-Auto-13 aims to clear the 9); eval_interactive `499/4`;
  autoloop `276`; 17-fixture `31`; scoring SHA `35305bd8…`.

  Dev session source-of-truth: `compact/sprint-068-dev-prompt.md` (self-contained per §9). Dev reads ONLY
  `AGENTS.md` (auto-loaded) + that prompt; code anchors on demand.
---

# Sprint 068 / S-Auto-13 — A2 classify-first gating-race + A3 paraphrase-storm skill-layer discipline

## Class

- **Layer (primary)**: `prompt_projection`/skill (A2 `discover_triage.yaml` procedure; A3 `resolve_faq_grounded_answer.yaml` `grounding_instruction` + a `ContextProjectionBuilder` projection echo) + config-governance (tool-policy *alignment context* — the fix matches the skill to the existing policy, ideally without editing the policy) + `semantic_planner` (A3 — the LLM owns whether to re-search). No new runtime decision logic.
- **§7 stanza**: **REQUIRED** (semantic-touching). Self-walked below.
- **Codex review plan (§4.3)**: milestone-shared (default) at M-Auto-3 close. A2/A3 are skill soft-fields + a projection echo — no Tier-0, no §1.7 keyword/regex/enum hardcode. UPGRADES to per-sub-sprint ONLY IF an A3 hard-cap backstop or the OQ-S66.1 golden reconciliation crosses a §1.7 line → STOP-and-surface.
- **Position in milestone**: 3rd of ~4 (S-Auto-11 ✅ → S-Auto-12 ✅ → **S-Auto-13 A2+A3** → S-Auto-14 B1 → optional S-Auto-15 buffer).

## Goal

Close the two remaining upstream step-wasters in the proposal's fan-in model so the autoloop fitness signal stops measuring substrate noise: the DISCOVER **gating-race** (A2, `GATING_RACE` 4→≤1) and the RESOLVE **paraphrase-storm** (A3, `PARAPHRASE_STORM` 11/24→≤3). Both are skill-layer / projection soft-signal fixes (no runtime semantic decision changes), delivered by manual dev. As an in-scope consequence of editing both drift-affected skills, reconcile the OQ-S66.1 `max_tool_steps` goldens (take the Java baseline from 10 failures toward 1).

**Acceptance**: on a 3-pass `bad_cases` rerun (sim_temp=0 + bot_temp=0 + 60s deadline + parallel=1, freshly-restarted backend, measured via the S-Auto-11 per-iter trace persistence): `GATING_RACE` 4→≤1 (DISCOVER `search_knowledge` "not allowed for use case 'none'" rejections drop); `PARAPHRASE_STORM` 11/24→≤3 (non-identical re-searches after a `faq_miss=false` hit drop); negative controls intact (legitimate distinct RESOLVE searches not suppressed; legitimate re-search after `faq_miss=true` still allowed; classify-first does not break a legitimate DISCOVER clarify-then-classify flow). Java baseline ideally `1186→ (failures 10→1)` via the OQ-S66.1 golden reconciliation (at minimum no NEW failures); eval `499/4`, autoloop `276`, 17-fixture `31`, scoring SHA `35305bd8…` preserved.

## Scope (4 steps)

1. **A2 — `discover_triage.yaml` classify-first** (`server/src/main/resources/skills/discover_triage.yaml`). Edit `$.procedure` so DISCOVER classifies FIRST (do not instruct `search_knowledge` before `classify_use_case` while activeUseCase=none) — the weak-candidate FAQ path should gather enough to classify toward the right FAQ-path UC, then let RESOLVE run the grounded search. Reconcile the `faq-uc-search-before-commit` behavior (currently rewards `tool_event_seq(search_knowledge) < tool_event_seq(classify_use_case)`, which is exactly what drives the LLM into the `search`-in-`none` gating-race). Align the skill with the existing `tool-policy.yaml` (`classify_use_case:[ALL]`, `search_knowledge` requires a UC). PREFER NOT to edit `tool-policy.yaml` (A2-A1 is skill-only); if you believe the policy must change instead, STOP-and-surface (that is A2-A2, a different trade-off). Manual dev — NOT autoloop-generated.

2. **A3 — `resolve_faq_grounded_answer.yaml` paraphrase discipline** (`server/src/main/resources/skills/resolve_faq_grounded_answer.yaml` `$.grounding_instruction` ~:31) + projection echo (`server/src/main/java/.../service/runtime/ContextProjectionBuilder.java`, coordinate with the existing search-echo at ~:738 "Do NOT call search_knowledge again" + `accumulated_tool_results` echo ~:847). Add: "after a `search_knowledge` returns a viable hit (`faq_miss=false`), do NOT re-search this turn — draft from the existing hits via `resolve_article`, or escalate; a fresh search is only warranted if the prior result was `faq_miss=true` or the query is materially different." Ensure the projection echoes the prior search result so the LLM need not re-search to confirm. **Soft-signal-first (§1.5)**: do NOT add a hard search-count cap as the primary fix; if soft-signal + echo prove insufficient, STOP-and-surface before adding an A3-cap backstop. Distinct from S-Auto-12 A1 (which only dedups byte-identical repeats; A3 covers paraphrase repeats A1's hash misses).

3. **OQ-S66.1 `max_tool_steps` golden reconciliation (in-scope consequence)**. Since this sub-sprint edits both `discover_triage.yaml` and `resolve_faq_grounded_answer.yaml`, reconcile their `max_tool_steps` goldens — the 9 pre-existing `PhaseEvaluator` failures (`PhaseEvaluatorPlanTest` ×2 + `PhaseEvaluatorResolveSkillIntegrationTest` ×7; golden asserts 4/2, shipped is 6/3). Pick the **shipped YAML value as source of truth** (post-A2/A3, re-derive if A2/A3 changed the needed step count) and update the `PhaseEvaluator` golden tests in `server/src/test/**` to match. Target: Java failures `10 → 1` (the inherited `SystemPromptUserRequestedTiebreakerTest` remains, OQ-S41.5 STATUS QUO). STOP-and-surface if the golden encodes an intended cap the team wants enforced (i.e. it is more than a stale-value sync).

4. **3-pass `bad_cases` measurement + handoff + OQ ledger** (`docs/sprints/sprint-068-handoff.md`), including `GATING_RACE` + `PARAPHRASE_STORM` before/after.

## Hard fences / STOP conditions

**In scope to edit**: `server/src/main/resources/skills/discover_triage.yaml` (A2) + `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml` (A3) + `server/src/main/java/.../service/runtime/ContextProjectionBuilder.java` (A3 projection echo, only if the existing echo is insufficient) + `server/src/test/**` (OQ-S66.1 golden reconciliation + any new A2/A3 tests). `server/src/main/resources/config/tool-policy.yaml` only if A2 genuinely requires it (PREFER not — STOP-and-surface first). Read-only: the bad_cases suite (measurement).

**Hard-fenced (do NOT edit)**: `PhaseEvaluator.resolveMaxStepsReason` *logic* (B1 = S-Auto-14; you may update its golden TEST expectations for OQ-S66.1, NOT the resolver code); `AgentRunLoopImpl` / `ToolEvent` / `ControlKernel` / `system_prompt.txt` (S-Auto-12 A1 FINALIZED); `eval_interactive/case_specs/**` (B1 sync = S-Auto-14); `eval_interactive/.../user_simulator.py` (D1 FINALIZED); the 4 SHA-locked scoring files; `autoloop/autoloop/loop.py` (S-Auto-11 FINALIZED) + `sandbox/applier.py` + `sandbox/{anti_hardcode_check,content_validator}.py` + `meta_agent`/`memory`/`preflight.py`/`cli.py`; `docs/foundational/**`, `docs/current/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/teams/**`; prior sprint/milestone archives.

**STOP-and-surface conditions**:
- A2/A3 risk altering UC-routing / escalation / drift semantics beyond the storm fix (these are the LLM's by §1.3 — your edits must only re-order/soft-guide, not hardcode a UC decision).
- The OQ-S66.1 golden reconciliation is more than a stale-value sync (the golden encodes an intended cap).
- Soft-signal + projection echo prove insufficient for A3 and you are tempted to add a hard search-count cap (surface first — it is a backstop, not the first choice).
- A2 seems to require editing `tool-policy.yaml` (that is A2-A2, a different trade-off).
- Any hard-fenced surface needs editing. Do NOT revert S-Auto-11/12 or the `b351648` determinism config.
- **No `git add -A`** — stage scope explicitly. Any `autoloop run` only on a clean committed tree. **Local-Mac only.** Restart the backend after skill/server changes (`mvn spring-boot:run` has no hot-reload) before the bad_cases measurement.

## Test / eval requirements

- **Java**: from the S-Auto-12 baseline `1186/10/0/2`, S-Auto-13 should REDUCE failures via the OQ-S66.1 golden reconciliation — target `Failures: 1` (the tiebreaker) + any new A2/A3 tests (`Tests run` rises). At minimum: no NEW non-reconciled failures. If A2/A3 change a skill's `max_tool_steps`, the golden MUST be updated to the shipped value in the same sub-sprint (no red tests left).
- **eval_interactive pytest**: `499 passed, 4 failed` preserved (A2/A3 are server/skill-side).
- **autoloop pytest**: `276 passed`.
- **17-fixture detector sweep**: `31 passed`.
- **scoring SHA**: held at `35305bd8…`.
- **A2/A3 measurement (deliverable)**: restart `:8080` backend, 3-pass `bad_cases` (`cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1`, ×3 at sim/bot temp=0/60s). Read the persisted per-iter traces; report `GATING_RACE` 4→≤1 (DISCOVER search "not allowed for use case 'none'" rejections) + `PARAPHRASE_STORM` 11/24→≤3 (non-identical re-searches after a `faq_miss=false` hit), negative controls intact.

## §7 stanza (REQUIRED)

**Target failure layer:** `prompt_projection`/skill (A2 `discover_triage` procedure; A3 `grounding_instruction` + projection echo) + `semantic_planner` (A3 — LLM owns whether to re-search) + config-governance (tool-policy alignment context). No runtime semantic-decision logic is added.

**Tier-0 invariant:** This sprint adds NO Tier-0 invariant. A2 aligns a skill with the existing tool-policy; A3 is a soft grounding signal + projection echo. Neither touches `docs/runtime_freeze_and_risk_policy.md` §1/§2.

**Semantic hardcode:** None introduced. A2 RE-ORDERS the `discover_triage` procedure to match the EXISTING `tool-policy.yaml` (removing a self-contradiction) — no keyword/regex/enum/per-UC matrix added; the LLM still owns the UC choice (§1.3). A3 adds a soft `grounding_instruction` keyed on the EXISTING `faq_miss` flag + a projection echo of prior search results — the LLM still owns whether to re-search; soft-signal-first per §1.5. Any A3 hard-cap is a structural cardinality limit (not a semantic rule) and a last-resort backstop only (STOP-and-surface before adding). The OQ-S66.1 golden reconciliation aligns a stale TEST expectation to shipped behavior (not a runtime rule). Net effect: removes wasted DISCOVER/RESOLVE steps + a self-contradicting skill cue — net-lowers, not raises, the hardcode surface.

**Generalization coverage:** target = bad_cases `GATING_RACE` (4) + `PARAPHRASE_STORM` (11/24) subsets; neighbor = `anchor_outcome` / shadow same-storm shape; negative = (a) legitimate distinct RESOLVE searches (different queries) NOT suppressed, (b) legitimate re-search after `faq_miss=true` still allowed, (c) classify-first does NOT break a legitimate DISCOVER clarify-then-classify flow; shadow = held-out (not read by dev). Counts confirmed at handoff via the 3-pass bad_cases rerun.

## Codex review plan (per §4.3)

Milestone-shared at M-Auto-3 close (default; cumulative S-Auto-11..14 + bundled M-Auto-2 residual). UPGRADES to per-sub-sprint ONLY IF an A3 hard-cap backstop is added or the OQ-S66.1 golden reconciliation raises a §1.7 / §5.4 concern (a test-expectation change that could look like masking) — then STOP-and-surface; the deliver-agent authors the per-sub-sprint Codex prompt.

## Handoff requirements

`docs/sprints/sprint-068-handoff.md` at close. Mandatory: §0 summary (scope, commits, final counts incl. the post-reconciliation Java baseline); §1 A2 classify-first (before/after procedure + the `faq-uc-search-before-commit` behavior disposition + GATING_RACE evidence); §2 A3 paraphrase discipline (grounding_instruction + projection echo before/after + PARAPHRASE_STORM evidence + confirmation no hard-cap was added, or STOP-and-surface record if it was); §3 negative controls (distinct RESOLVE searches + faq_miss=true re-search + DISCOVER clarify-then-classify); §4 OQ-S66.1 golden reconciliation (which goldens, shipped values chosen, Java before/after); §5 baselines + §7-stanza self-walk + fence disposition; §6 OQs surfaced; §7 self-check tick-off.

## Commit discipline

Multi-commit acceptable (A2 / A3 / golden reconciliation / handoff). Commit message: `Sprint 068 / S-Auto-13 / M-Auto-3 — <description>` + standard footer `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`. No `git add -A` — stage explicitly. Do not push.

## Self-check (dev MUST verify before claiming done)

- [ ] A2: `discover_triage.yaml` procedure classifies-first; the `faq-uc-search-before-commit` search-before-classify reward reconciled; aligned with existing tool-policy (tool-policy NOT edited, or STOP-and-surfaced); `GATING_RACE` 4→≤1 on a 3-pass rerun.
- [ ] A3: `resolve_faq_grounded_answer.yaml` grounding_instruction + projection echo discourage re-search after a `faq_miss=false` hit; soft-signal-first (NO hard cap, or STOP-and-surfaced); `PARAPHRASE_STORM` 11/24→≤3.
- [ ] Negative controls: distinct RESOLVE searches not suppressed; `faq_miss=true` re-search allowed; DISCOVER clarify-then-classify not broken.
- [ ] OQ-S66.1: discover_triage + resolve_faq `max_tool_steps` goldens reconciled to shipped values; Java failures `10 → 1` (tiebreaker only) + any new tests; no red tests left.
- [ ] eval_interactive `499/4` + autoloop `276` + 17-fixture `31` + scoring SHA `35305bd8…` preserved.
- [ ] No edits to PhaseEvaluator resolver logic / AgentRunLoopImpl / ToolEvent / system_prompt / case_specs / user_simulator / loop.py / scoring / applier.py / sandbox / meta_agent; A2/A3 manual (not autoloop-generated); no Tier-0; no semantic hardcode (no A3 hard-cap without surfacing).
- [ ] No `git add -A`; any autoloop run on a clean committed tree; backend restarted for measurement; local-Mac only.
- [ ] Handoff §0-§7 filled.
