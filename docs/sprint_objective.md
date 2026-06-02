---
title: Sprint 070 / S-Auto-14 — B1 escalation-reason honesty (resolveMaxStepsReason evidence-aware) + minimal eval valid_reasons sync (M-Auto-3 sub-sprint 5 of 5)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-02
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-069-objective.md]
superseded_by: null
notes: >
  M-Auto-3 / Sprint 070 / S-Auto-14. FIFTH and LAST sub-sprint of M-Auto-3 (Substrate-hygiene);
  the milestone is at the §8.1 5-sub-sprint ceiling (no buffer left). **Layer**: `infra`
  (`PhaseEvaluator.resolveMaxStepsReason` made evidence-aware) + `eval_spec` (minimal case_spec
  `escalation_reason` sync for the cases the bot fix re-stamps). **§7 stanza REQUIRED**. **Codex:
  PER-SUB-SPRINT REQUIRED** (§4.3 trigger #1 — touches the Tier-0 *candidate*
  `R-escalation-reason-runtime-evidence-contract-review`; AND a §5.4-sensitive eval edit; Codex
  verifies the eval sync tracks a real bot fix and is NOT eval-side masking of a bug).

  **Why this sub-sprint (the fan-in sink, proposal §2 + R-runtime-escalation-reason-misstamp-maxsteps-faq)**:
  Module A (A1 dedup / A2 classify-first / A3 paraphrase backstop) cut the upstream step-wasters; B1
  fixes the single-point reason resolver so the RESIDUAL max-steps exhaustions are stamped honestly.
  Today `resolveMaxStepsReason` (`server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:173-196`)
  step 3 returns `faq_miss_threshold_exceeded` whenever ANY `search_knowledge` ToolEvent is present
  (`te.toolName()` presence check at `:183-194`) — even when the last search returned a VIABLE hit
  (`faq_miss=false`). That mis-labels a turn-budget exhaustion as a knowledge-miss. B1 reads the
  most-recent `search_knowledge` result's `faq_miss` flag (the SAME flag the S-Auto-13b A3 gate reads;
  parse pattern already in this file at `:1173` `Boolean.TRUE.equals(data.get("faq_miss"))`) and, when
  the turn had viable hits, falls through to the EXISTING `turn_budget_exhausted` catch-all instead.

  **§4 enum verdict (inherited; do NOT violate)**: NO new `escalation_reason` enum value (e.g. no
  `max_steps_exhausted`). The enum is the canonical 23-value three-mirror set; reuse the existing
  `turn_budget_exhausted` catch-all. B1 thereby REMOVES a lossy heuristic (toolName-presence → faq
  label) — it net-LOWERS the hardcode surface, it does not add one. (`D-new-escalation-reason-enum`
  in `action_bank.md` §4 remains deferred/avoid.)

  **§5.4 red line (the eval sync)**: the bot fix lands FIRST; then ONLY the specific cases that
  previously expected `faq_miss_threshold_exceeded` on a VIABLE-HITS exhaustion get their expected
  `escalation_reason` updated to the now-correct `turn_budget_exhausted`. This is the spec expecting
  CORRECT behaviour — NOT widening the spec to accept a bot mistake. Cases where the last search was a
  genuine miss (`faq_miss=true`) MUST keep `faq_miss_threshold_exceeded` (the bot still correctly emits
  it). The case list is determined EMPIRICALLY post-B1 (rerun, observe which cases flip), NOT guessed.

  **OQ-S69.1 is OUT of scope (hard fence)**: S-Auto-14 does NOT touch the cross-turn paraphrase storm
  (OQ-S69.1, the S-Auto-13b residual). That is a separate `infra` surface (`BotSession`-scoped state),
  carrier-decided at M-Auto-3 close (accept / M-Auto-4 / §8.5 split). Do NOT "while I'm here" it.

  **Inherited substrate at HEAD (do NOT re-fix / revert)**: S-Auto-11 (`loop.py`) + S-Auto-12 (A1 hybrid
  dedup) + S-Auto-13 (A2 classify-first; A3 soft layer) + S-Auto-13b (A3 `faq_miss`-state backstop in
  `AgentRunLoopImpl`) are FINALIZED. `b351648` determinism config in place. fence-#13 scoring SHA
  `35305bd8…`.

  **CORRECTED baselines (post-S-Auto-13b, deliver-verified)**: Java **`1198 / 1 / 0 / 2`** (sole failure =
  inherited `SystemPromptUserRequestedTiebreakerTest`, OQ-S41.5 status quo); eval_interactive **`495 / 8`**
  (OQ-S68.1 — the 8 are the `0323457` action_bank split, NOT the agent; a quick test-fix or baseline-accept
  decision is still pending — do NOT treat those 8 as yours, and do NOT bundle that fix here); autoloop
  `276`; 17-fixture `31`; scoring SHA `35305bd8…`.

  Dev session source-of-truth: `compact/sprint-070-dev-prompt.md` (self-contained per §9). Dev reads ONLY
  `AGENTS.md` (auto-loaded) + that prompt; code anchors on demand.
---

# Sprint 070 / S-Auto-14 — B1 escalation-reason honesty + minimal eval valid_reasons sync

## Class

- **Layer (primary)**: `infra` — `PhaseEvaluator.resolveMaxStepsReason` made evidence-aware (read the most-recent `search_knowledge` result's `faq_miss` flag instead of mere tool presence). **Secondary**: `eval_spec` — a MINIMAL `escalation_reason` sync on the specific cases the bot fix re-stamps. No UC-routing / drift / escalation-posture *decision* changes; B1 changes which CANONICAL reason a max-steps exit is LABELLED with, using evidence the runtime already has.
- **§7 stanza**: **REQUIRED**. Self-walked below.
- **Codex review plan (§4.3)**: **PER-SUB-SPRINT REQUIRED** — trigger #1 (touches the Tier-0 *candidate* `R-escalation-reason-runtime-evidence-contract-review`; the change stays `infra` and adds NO Tier-0, but the surface is Tier-0-adjacent) AND a §5.4-sensitive eval edit (Codex verifies the valid_reasons sync tracks a real bot fix, not masking). The deliver-agent authors the Codex prompt at S-Auto-14 close; the verdict lands in `docs/codex-findings.md`.
- **Position in milestone**: 5th and LAST of 5 (S-Auto-11 ✅ → S-Auto-12 ✅ → S-Auto-13 ✅-partial → S-Auto-13b ✅ → **S-Auto-14 B1** → M-Auto-3 close). The §8.1 buffer is spent; any second-order issue → §8.5 split decision (deliver+human), NOT an in-milestone sub-sprint.

## Goal

Make `resolveMaxStepsReason` honest: when a `MAX_STEPS` exit had VIABLE FAQ hits (the most-recent `search_knowledge` returned `faq_miss=false`), stamp the existing `turn_budget_exhausted` catch-all instead of mis-labelling it `faq_miss_threshold_exceeded`. Then sync ONLY the eval cases whose expected reason no longer matches the (now-correct) bot output. After S-Auto-14, the autoloop's escalation-correctness signal stops conflating "ran out of budget with a good answer in hand" with "genuinely missed the knowledge".

**Acceptance**: on a 3-pass `bad_cases` rerun (sim_temp=0 + bot_temp=0 + 60s deadline + parallel=1, freshly-restarted backend, measured via the S-Auto-11 per-iter trace persistence) `ESCALATION_MISSTAMP` drops **5/24 → ≤1** with negative controls intact (a GENUINE faq-miss exhaustion still stamps `faq_miss_threshold_exceeded`; an INTAKE exhaustion still stamps `incomplete_intake`; a clarification-stall still stamps `clarification_budget_exhausted` — the step-1/step-2 branches are unchanged). Baselines preserved: Java `1198/1/0/2` + new B1 tests; eval_interactive `495/8` MODULO the minimal valid_reasons sync (the synced cases move from FAIL/mismatch to PASS on the corrected reason — net delta documented + each case justified §5.4); autoloop `276`; 17-fixture `31`; scoring SHA `35305bd8…`.

## Scope (4 steps)

1. **B1 — evidence-aware `resolveMaxStepsReason`** in `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:173-196`. Replace the step-3 `searchedKnowledge` *presence* check (`:183-194`) with a most-recent-`search_knowledge`-`faq_miss` read: iterate `result.toolEvents()`, find the LAST `search_knowledge` event, read its `faq_miss` from `ToolEvent.resultData()` (reuse the existing parse pattern `Boolean.TRUE.equals(data.get("faq_miss"))` — see this file `:1173`; guard for null/malformed result maps). Decision: if the most-recent `search_knowledge` had `faq_miss=false` (viable hit) → DO NOT return `faq_miss_threshold_exceeded`; fall through to the existing `return "turn_budget_exhausted"` catch-all (`:195`). If the most-recent `search_knowledge` had `faq_miss=true` (genuine miss) → keep `faq_miss_threshold_exceeded` (`:193`). The step-1 (`incomplete_intake`) and step-2 (`clarification_budget_exhausted`) branches are UNCHANGED. NO new enum value. Update the Javadoc heuristic list (`:160-171`) to reflect the evidence-aware step 3.

2. **B1 tests** (`server/src/test/**`): characterize the resolver directly. (a) most-recent `search_knowledge` `faq_miss=false` + max-steps → `turn_budget_exhausted` (the fix); (b) most-recent `search_knowledge` `faq_miss=true` + max-steps → `faq_miss_threshold_exceeded` (negative control — genuine miss preserved); (c) mixed turn: an early `faq_miss=true` then a later `faq_miss=false` viable hit → `turn_budget_exhausted` (most-recent wins); (d) INTAKE plan → `incomplete_intake` (step-1 unchanged); (e) clarification-count>0 → `clarification_budget_exhausted` (step-2 unchanged, precedence preserved); (f) no `search_knowledge` at all → `turn_budget_exhausted` (existing). Extend/adjust any existing `resolveMaxStepsReason` golden tests to the new contract (an owned contract reversal, narrated — NOT regression-masking).

3. **Minimal eval `escalation_reason` sync (§5.4 — bot fix FIRST, then spec)**: restart `:8080`, rerun the affected cases, and EMPIRICALLY identify the specific case_specs whose expected `escalation_reason` was `faq_miss_threshold_exceeded` on a VIABLE-HITS exhaustion that now (correctly) emits `turn_budget_exhausted`. Candidate surfaces: `eval_interactive/case_specs/bad_cases/**` (cs001 / cs014 / cs095 / wmkb trace-observed in `R-runtime-escalation-reason-misstamp-maxsteps-faq`) + `eval_interactive/case_specs/promotion/cs_interactive_*.yaml` (the suite that pins `escalation_reason`). Update ONLY those cases' expected reason to `turn_budget_exhausted`. **Do NOT** touch cases where the last search was a genuine miss (those correctly keep `faq_miss_threshold_exceeded`). Document EACH synced case in the handoff §2 table with: case id, pre/post bot reason, and the **three-part trace evidence** — (i) the most-recent `search_knowledge` returned `faq_miss=false`, (ii) a viable hit was present, (iii) the turn then hit max tool steps / turn-budget exhaustion — plus the layer classification (`eval_spec`). If a case looks like it would need widening to accept a bot MISTAKE → STOP-and-surface (do not sync it).

   **GUARDRAIL 1 — ORDERING IS ENFORCED**: do NOT edit ANY case_spec expectation before step 1 (the B1 bot fix) is landed AND the rerun shows the corrected bot reason. You may NOT change a case expectation first and then "prove" the bot fix against the changed expectation — that inverts §5.4 and is a forbidden-list red line. Case `escalation_reason` expectations are touched ONLY here in step 3, strictly after steps 1-2. The three-part trace evidence (i/ii/iii above) is RECORDED before each eval edit (per `feedback_milestone_close_bad_case_before_codex`).

4. **3-pass `bad_cases` measurement + handoff + OQ ledger** (`docs/sprints/sprint-070-handoff.md`): `ESCALATION_MISSTAMP` before/after; the synced-case table (§5.4 evidence per case); whether eliminating the storms upstream (A1/A3) materially lowered max-steps EXITS (re-measures OQ-S67.2); confirm OQ-S69.1 / cross-turn was NOT touched.

## Hard fences / STOP conditions

**GUARDRAIL 2 — B1 SCOPE IS EXACTLY `resolveMaxStepsReason` EVIDENCE-AWARENESS.** B1 is a single evidence-aware correction (read the most-recent `search_knowledge` `faq_miss` instead of mere tool presence) + the minimal post-fix eval sync in Scope step 3. It does NOT expand to: a broader escalation refactor or any change to the PhaseEvaluator state machine; broad case_spec adjustments (only the minimal empirical sync of step 3); `user_simulator.py`; the cross-turn paraphrase storm (OQ-S69.1); `tool-policy.yaml`; `skills/**`; a NEW `escalation_reason` enum value; or the A1/A3 dispatch-path gates. If the fix appears to need ANY of these → STOP-and-surface (do not self-expand the sub-sprint).

**In scope to edit**: `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (`resolveMaxStepsReason` + its Javadoc ONLY — do NOT touch other PhaseEvaluator methods) + `server/src/test/**` (new/adjusted B1 tests) + the SPECIFIC `eval_interactive/case_specs/**` cases identified empirically in step 3 (minimal `escalation_reason` sync ONLY — no rubric/judge/outcome-class change). Read-only: `ToolEvent.java` / `KnowledgeSearchResult.java` (faq_miss source); the bad_cases + promotion suites (measurement).

**Hard-fenced (do NOT edit)**: the S-Auto-12 A1 `successfulDispatchCache` + the S-Auto-13b A3 `lastSearchKnowledgeViableHit` gate in `AgentRunLoopImpl` (FINALIZED — B1 reads `faq_miss` at resolver time, it does NOT touch the dispatch-path gates); `server/src/main/resources/skills/**` (A2/A3 soft layer FINALIZED); `tool-policy.yaml`; `eval_interactive/.../user_simulator.py`; the 4 SHA-locked scoring files; `autoloop/autoloop/loop.py` + `sandbox/**` + `meta_agent/**` + `cli.py` + `preflight.py`; `docs/foundational/**`, `docs/current/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/teams/**`; prior sprint/milestone archives. Do NOT "fix" the 4 OQ-S68.1 eval split-failures here (separate housekeeping). Do NOT touch the cross-turn paraphrase storm (OQ-S69.1).

**STOP-and-surface conditions**:
- B1 would require a NEW `escalation_reason` enum value to express the corrected label → STOP (the §4 verdict forbids it; reuse `turn_budget_exhausted`). If you believe a new value is genuinely needed → `human_review_required`, do NOT self-invent.
- The eval sync would require widening a spec to accept a bot MISTAKE (e.g., the bot escalates when it should resolve) → STOP-and-surface (that is a §5.4 red line, not a sync).
- B1 appears to need a NEW Tier-0 invariant (the surface is the Tier-0-*candidate* `R-escalation-reason-runtime-evidence-contract-review`) → `human_review_required` (do NOT self-invent a Tier-0; this sub-sprint adds none).
- Landing B1 appears to require MODIFYING the A1/A3 dispatch-path gates → STOP (read `faq_miss` from `ToolEvent.resultData` at resolver time instead).
- Any hard-fenced surface needs editing. Do NOT revert S-Auto-11/12/13/13b or `b351648`.
- **No `git add -A`** — stage explicitly. Any `autoloop run` only on a clean committed tree. **Local-Mac only.** Restart the backend after server changes (`mvn spring-boot:run` has no hot-reload) before the bad_cases measurement.

## Test / eval requirements

- **Java**: no NEW failures beyond `1198 / 1 / 0 / 2` (the lone failure is the inherited tiebreaker); `Tests run` rises by the new B1 + negative-control tests; `Failures` stays `1`.
- **eval_interactive pytest**: `495 / 8` preserved MODULO the minimal `escalation_reason` sync — the synced cases should PASS on the corrected reason; report the exact net delta and confirm no NEW failure is introduced beyond the synced set (the 8 OQ-S68.1 split-failures are not yours).
- **autoloop pytest**: `276 passed`. **17-fixture detector sweep**: `31 passed`. **scoring SHA**: `35305bd8…`.
- **B1 measurement (deliverable)**: restart `:8080`, 3-pass `bad_cases` (`cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1`, ×3 at sim/bot temp=0/60s). Read the persisted per-iter traces; report `ESCALATION_MISSTAMP` 5/24 → ≤1, the per-case before/after escalation reason, negative controls intact.

## §7 stanza (REQUIRED)

**Target failure layer:** `infra` (`PhaseEvaluator.resolveMaxStepsReason` evidence-aware) + `eval_spec` (minimal case_spec `escalation_reason` sync for the cases the bot fix re-stamps). No agent semantic *decision* (UC hypothesis, drift, escalation posture, response strategy) is changed; B1 changes which canonical reason a max-steps exit is LABELLED with, using `faq_miss` evidence the runtime already has.

**Tier-0 invariant:** This sprint adds NO Tier-0 invariant. The change stays `infra` and reuses the existing `turn_budget_exhausted` catch-all. It TOUCHES the Tier-0-*candidate* surface `R-escalation-reason-runtime-evidence-contract-review`; if review argues for elevating "escalation reasons claiming session events require event evidence" to Tier-0 → `human_review_required` (do NOT self-invent). Not added to `docs/runtime_freeze_and_risk_policy.md` §1/§2.

**Semantic hardcode:** None introduced; one REMOVED. B1 deletes a lossy heuristic (`search_knowledge` toolName-presence → `faq_miss_threshold_exceeded` label) and replaces it with a read of the EXISTING `faq_miss` result flag, falling through to the existing catch-all — net-LOWERS the hardcode surface. No new keyword/regex/enum/per-UC matrix; no new `escalation_reason` enum value (§4 verdict). The eval sync expects the CORRECTED reason (§5.4 — not widening to accept a bug).

**Generalization coverage:** target = the `ESCALATION_MISSTAMP` bad_cases subset (5/24; cs001/cs014/cs095/wmkb trace-observed); neighbor = the `promotion/` cases that pin `escalation_reason`; negative = (a) genuine faq-miss exhaustion still `faq_miss_threshold_exceeded`, (b) INTAKE exhaustion still `incomplete_intake`, (c) clarification-stall still `clarification_budget_exhausted`, (d) mixed-turn most-recent-wins; shadow = held-out (not read by dev). Counts confirmed at handoff via the 3-pass bad_cases rerun (`ESCALATION_MISSTAMP` 5→≤1) + the synced-case §5.4 evidence table.

## Codex review plan (per §4.3)

**PER-SUB-SPRINT REQUIRED** (trigger #1 Tier-0-candidate surface + §5.4-sensitive eval edit). At S-Auto-14 close the deliver-agent authors `compact/sprint-070-codex-review-prompt.md` embedding the §4.1 nine-question kernel; Codex verifies: B1 reuses `turn_budget_exhausted` with NO new enum (§4 verdict); B1 reads existing `faq_miss` evidence and removes (not adds) a heuristic; the eval `escalation_reason` sync tracks the real bot fix and is NOT §5.4 masking (each synced case has trace evidence the last search was `faq_miss=false`); no Tier-0 self-invented; the A1/A3 gates + skills + scoring were not touched; OQ-S69.1 cross-turn was not touched. Verdict → `docs/codex-findings.md`. (Separate from the M-Auto-3 milestone-shared close Codex, which bundles the cumulative S-Auto-11..14 range + the M-Auto-2 residual.)

## Handoff requirements

`docs/sprints/sprint-070-handoff.md` at close. Mandatory: §0 summary (scope, commits, final counts incl. Java `1198/1`+new tests, eval `495/8`+sync delta); §1 the B1 resolver change (before/after logic; the most-recent-`search_knowledge`-`faq_miss` read; precedence vs step-1/step-2 unchanged); §2 the minimal eval `escalation_reason` sync — a per-case table (id, pre/post bot reason, `faq_miss=false` trace evidence, `eval_spec` classification) proving §5.4 compliance; §3 negative controls (genuine miss / intake / clarification / mixed-turn); §4 `ESCALATION_MISSTAMP` 3-pass bad_cases before/after + the OQ-S67.2 re-measurement (did upstream A1/A3 lower max-steps EXITS); §5 baselines + §7-stanza self-walk + fence disposition (incl. explicit "OQ-S69.1 cross-turn NOT touched"); §6 OQs surfaced; §7 self-check tick-off. (Codex is per-sub-sprint — note its dispatch status.) This is the LAST sub-sprint → the handoff also lists M-Auto-3 close-readiness (which §11 gates are met / split / open, incl. the PARAPHRASE_STORM within-turn/cross-turn/total decision + OQ-S68.1 Python-baseline decision).

## Commit discipline

Multi-commit acceptable (B1 + tests / eval sync / handoff). Commit message: `Sprint 070 / S-Auto-14 / M-Auto-3 — <description>` + standard footer `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`. No `git add -A` — stage explicitly. Do not push.

## Self-check (dev MUST verify before claiming done)

- [ ] `resolveMaxStepsReason` step 3 reads the most-recent `search_knowledge` `faq_miss` from `ToolEvent.resultData()` (null/malformed guarded); `faq_miss=false` → `turn_budget_exhausted`, `faq_miss=true` → `faq_miss_threshold_exceeded`; step-1/step-2 branches UNCHANGED; Javadoc updated.
- [ ] NO new `escalation_reason` enum value (§4 verdict); reuses existing `turn_budget_exhausted`.
- [ ] B1 tests cover the fix + negative controls (genuine miss / intake / clarification / mixed-turn most-recent-wins / no-search).
- [ ] Eval `escalation_reason` sync is MINIMAL + empirical (post-B1 rerun): only cases now correctly emitting `turn_budget_exhausted` on a `faq_miss=false` exhaustion; each justified in the handoff §2 table with trace evidence; NO case widened to accept a bot mistake (§5.4); genuine-miss cases untouched.
- [ ] `ESCALATION_MISSTAMP` 5/24 → ≤1 on a 3-pass bad_cases rerun (backend restarted; via S-Auto-11 persisted traces).
- [ ] A1/A3 dispatch-path gates, skills, tool-policy, user_simulator, loop.py, scoring, sandbox, meta_agent UNTOUCHED; b351648 not reverted; no Tier-0 self-invented; OQ-S69.1 cross-turn NOT touched; OQ-S68.1 split-failures NOT touched.
- [ ] Java no NEW failures beyond `1198/1/0/2` (+ new B1 tests); eval `495/8` modulo the documented sync; autoloop `276`; 17-fixture `31`; scoring SHA `35305bd8…`.
- [ ] No `git add -A`; any autoloop run on a clean committed tree; backend restarted for measurement; local-Mac only.
- [ ] Handoff §0-§7 filled (incl. §2 §5.4 evidence table + M-Auto-3 close-readiness); per-sub-sprint Codex dispatch status noted.
