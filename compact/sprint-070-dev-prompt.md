# Sprint 070 / S-Auto-14 / M-Auto-3 — Dev Implementation Prompt

You are the **dev agent (Claude Code) for Sprint 070 / S-Auto-14 / M-Auto-3 (Substrate-hygiene, sub-sprint 5 of 5 — LAST before milestone close)**. One-line goal: **make `PhaseEvaluator.resolveMaxStepsReason` evidence-aware so a max-steps exit that had VIABLE FAQ hits is stamped `turn_budget_exhausted` (not mis-labelled `faq_miss_threshold_exceeded`), then minimally sync only the eval cases the bot fix re-stamps — with NO new escalation_reason enum.**

This prompt is **self-contained per `iteration_governance.md` §9**. Read ONLY `AGENTS.md` (auto-loaded via the constitution chain) + this prompt. Open the specific code anchors below on demand. Do NOT read other docs unless this prompt names them.

## Read order (minimal)

1. `AGENTS.md` (auto-loaded; constitution chain loads transitively). Do NOT manually read governance docs.
2. This prompt — your complete contract.
3. **Code anchors (open on demand):**
   - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:151-196` — `resolveMaxStepsReason` (the B1 target) + its Javadoc heuristic list. Step 3 is the `searchedKnowledge` presence check at `:183-194`; the catch-all `return "turn_budget_exhausted"` is at `:195`.
   - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:1169-1196` — the existing `faq_miss` parse pattern (`Boolean.TRUE.equals(data.get("faq_miss"))`) you reuse to read the flag off a `ToolEvent.resultData()`.
   - `server/src/main/java/com/gumtree/csagent/model/ToolEvent.java` — `toolName()`, `resultData()` (read-only).
   - `server/src/main/java/com/gumtree/csagent/model/KnowledgeSearchResult.java` + `service/tools/SearchKnowledgeTool.java` — confirm `faq_miss` lands on the dispatched `search_knowledge` result map (read-only; do NOT recompute it).
   - `server/src/test/**` — existing `PhaseEvaluator` / `resolveMaxStepsReason` tests (adjust goldens to the new contract).
   - `eval_interactive/case_specs/bad_cases/**` (cs001 / cs014 / cs095 / wmkb) + `eval_interactive/case_specs/promotion/cs_interactive_*.yaml` (the suite that pins `escalation_reason`) — measurement + the empirical sync target.

## Context (why this sub-sprint)

Module A (S-Auto-12 A1 dedup / S-Auto-13 A2 classify-first / S-Auto-13b A3 paraphrase backstop) cut the upstream step-wasters; B1 is the single-point **reason-resolver honesty** fix so the residual max-steps exhaustions are labelled truthfully (proposal §2 fan-in sink; `R-runtime-escalation-reason-misstamp-maxsteps-faq`). Today `resolveMaxStepsReason` step 3 returns `faq_miss_threshold_exceeded` whenever ANY `search_knowledge` ToolEvent is present — even when the last search returned a VIABLE hit (`faq_miss=false`). That mislabels "ran out of budget with a good answer in hand" as "missed the knowledge". B1 reads the most-recent `search_knowledge` result's `faq_miss` flag (the SAME flag the S-Auto-13b A3 gate reads) and falls through to the existing `turn_budget_exhausted` catch-all when the turn had viable hits.

**Inherited substrate at HEAD — FINALIZED, do NOT re-fix / revert:** S-Auto-11 (`loop.py`), S-Auto-12 (A1 `successfulDispatchCache`), S-Auto-13 (A2 `discover_triage` classify-first; A3 soft layer), S-Auto-13b (A3 `lastSearchKnowledgeViableHit` gate in `AgentRunLoopImpl`). `b351648` determinism config in place. fence-#13 scoring SHA `35305bd8…`.

**Baselines at HEAD (deliver-verified, post-S-Auto-13b):** Java **`1198 / 1 / 0 / 2`** (sole failure = inherited `SystemPromptUserRequestedTiebreakerTest`, OQ-S41.5 status quo — NOT yours); eval_interactive **`495 / 8`** (the 8 are the `0323457` action_bank split, OQ-S68.1 — NOT yours, do NOT fix here); autoloop `276`; 17-fixture `31`; scoring SHA `35305bd8…`.

## Class

- **Layer (primary)**: `infra` — `resolveMaxStepsReason` evidence-aware. **Secondary**: `eval_spec` — minimal `escalation_reason` sync on the cases the bot fix re-stamps. No agent semantic *decision* changes; B1 changes which canonical reason a max-steps exit is LABELLED with, using evidence the runtime already has.
- **§7 stanza**: REQUIRED (embedded below — walk it before you claim done).
- **Codex review plan (§4.3)**: PER-SUB-SPRINT REQUIRED (trigger #1 Tier-0-candidate surface `R-escalation-reason-runtime-evidence-contract-review` + §5.4-sensitive eval edit). The deliver-agent authors the Codex prompt at close; you just note dispatch status in the handoff.
- **Position**: 5th and LAST of M-Auto-3 (S-Auto-11 ✅ → 12 ✅ → 13 ✅-partial → 13b ✅ → **14** → M-Auto-3 close). Milestone at the §8.1 ceiling — any second-order issue → STOP-and-surface for a §8.5 split decision, NOT a self-added sub-sprint.

## Goal

Make `resolveMaxStepsReason` honest: when a `MAX_STEPS` exit had VIABLE FAQ hits (most-recent `search_knowledge` `faq_miss=false`), stamp the existing `turn_budget_exhausted` catch-all instead of `faq_miss_threshold_exceeded`. Then sync ONLY the eval cases whose expected reason no longer matches the now-correct bot output.

**Acceptance**: 3-pass `bad_cases` rerun (sim_temp=0 + bot_temp=0 + 60s + parallel=1, freshly-restarted backend, via the S-Auto-11 per-iter trace persistence) shows `ESCALATION_MISSTAMP` **5/24 → ≤1**, negative controls intact (genuine faq-miss exhaustion still `faq_miss_threshold_exceeded`; intake still `incomplete_intake`; clarification-stall still `clarification_budget_exhausted`). Baselines preserved: Java `1198/1/0/2` + new B1 tests; eval `495/8` modulo the documented minimal sync; autoloop `276`; 17-fixture `31`; scoring SHA `35305bd8…`.

## Scope (4 steps)

1. **B1 — evidence-aware `resolveMaxStepsReason`** (`PhaseEvaluator.java:173-196`). Replace the step-3 `searchedKnowledge` *presence* check (`:183-194`) with a most-recent-`search_knowledge`-`faq_miss` read: iterate `result.toolEvents()`, find the LAST `search_knowledge` event, read its `faq_miss` from `resultData()` (reuse `Boolean.TRUE.equals(data.get("faq_miss"))`, see `:1173`; guard null/malformed maps). If most-recent `search_knowledge` `faq_miss=false` (viable hit) → fall through to `return "turn_budget_exhausted"` (`:195`). If `faq_miss=true` (genuine miss) → keep `faq_miss_threshold_exceeded` (`:193`). Step-1 (`incomplete_intake`) + step-2 (`clarification_budget_exhausted`) branches UNCHANGED (precedence preserved). NO new enum value. Update the Javadoc heuristic list (`:160-171`).

2. **B1 tests** (`server/src/test/**`): (a) most-recent `faq_miss=false` → `turn_budget_exhausted` (the fix); (b) most-recent `faq_miss=true` → `faq_miss_threshold_exceeded` (negative control); (c) mixed turn early-miss-then-viable-hit → `turn_budget_exhausted` (most-recent wins); (d) INTAKE plan → `incomplete_intake`; (e) clarification-count>0 → `clarification_budget_exhausted` (precedence); (f) no `search_knowledge` → `turn_budget_exhausted`. Adjust any existing resolver goldens to the new contract (owned reversal, narrated — NOT regression-masking).

3. **Minimal eval `escalation_reason` sync (§5.4 — bot fix FIRST, then spec)**: restart `:8080`, rerun the affected cases, EMPIRICALLY identify the case_specs whose expected `escalation_reason` was `faq_miss_threshold_exceeded` on a VIABLE-HITS exhaustion that now correctly emits `turn_budget_exhausted`. Update ONLY those to `turn_budget_exhausted`. Do NOT touch genuine-miss cases (they correctly keep `faq_miss_threshold_exceeded`). Document EACH synced case (id, pre/post bot reason, the **three-part trace evidence** — (i) most-recent `search_knowledge` `faq_miss=false`, (ii) a viable hit was present, (iii) the turn then hit max tool steps / turn-budget exhaustion — and `eval_spec` classification). If a case would need widening to accept a bot MISTAKE → STOP-and-surface.

   **GUARDRAIL 1 — ORDERING IS ENFORCED**: do NOT edit ANY case_spec expectation before step 1 (the B1 bot fix) is landed AND the rerun shows the corrected bot reason. You may NOT change a case expectation first and then "prove" the bot fix against it — that inverts §5.4 and is a forbidden-list red line. Case `escalation_reason` expectations are touched ONLY in this step 3, strictly after steps 1-2; the three-part trace evidence is recorded before each eval edit.

4. **3-pass `bad_cases` measurement + handoff + OQ ledger** (`docs/sprints/sprint-070-handoff.md`): `ESCALATION_MISSTAMP` before/after; the synced-case §5.4 table; the OQ-S67.2 re-measurement (did upstream A1/A3 lower max-steps EXITS); confirm OQ-S69.1 / cross-turn NOT touched; M-Auto-3 close-readiness summary.

## Hard fences / STOP conditions

**GUARDRAIL 2 — B1 SCOPE IS EXACTLY `resolveMaxStepsReason` EVIDENCE-AWARENESS.** A single evidence-aware correction (read the most-recent `search_knowledge` `faq_miss` instead of mere tool presence) + the minimal post-fix eval sync (step 3). It does NOT expand to: a broader escalation refactor / any other change to the PhaseEvaluator state machine; broad case_spec adjustments (only the minimal empirical sync); `user_simulator.py`; the cross-turn paraphrase storm (OQ-S69.1); `tool-policy.yaml`; `skills/**`; a NEW `escalation_reason` enum value; or the A1/A3 dispatch-path gates. If the fix appears to need ANY of these → STOP-and-surface (do not self-expand).

**In scope to edit**: `PhaseEvaluator.java` (`resolveMaxStepsReason` + its Javadoc ONLY) + `server/src/test/**` (B1 tests) + the SPECIFIC `eval_interactive/case_specs/**` cases identified in step 3 (minimal `escalation_reason` sync ONLY; no rubric/judge/outcome-class edit). Read-only: `ToolEvent.java` / `KnowledgeSearchResult.java` / `SearchKnowledgeTool.java`; the suites for measurement.

**Hard-fenced (do NOT edit)**: the S-Auto-12 A1 `successfulDispatchCache` + S-Auto-13b A3 `lastSearchKnowledgeViableHit` gate in `AgentRunLoopImpl` (B1 reads `faq_miss` at RESOLVER time — do NOT touch the dispatch-path gates); `skills/**` (A2/A3 FINALIZED); `tool-policy.yaml`; `user_simulator.py`; the 4 SHA-locked scoring files; `autoloop/autoloop/loop.py` + `sandbox/**` + `meta_agent/**` + `cli.py` + `preflight.py`; `docs/foundational/**`, `docs/current/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/teams/**`; prior sprint/milestone archives. Do NOT touch the 4 OQ-S68.1 eval split-failures (separate housekeeping). Do NOT touch the cross-turn paraphrase storm (OQ-S69.1 — separate surface, carrier-decided at M-Auto-3 close).

**STOP-and-surface conditions**:
- B1 would need a NEW `escalation_reason` enum value → STOP (§4 verdict forbids it; reuse `turn_budget_exhausted`; if you truly believe a new value is needed → `human_review_required`, do NOT self-invent).
- The eval sync would need to widen a spec to accept a bot MISTAKE (e.g., bot escalates when it should resolve) → STOP (§5.4 red line).
- B1 appears to need a NEW Tier-0 invariant (the surface is the Tier-0-*candidate* `R-escalation-reason-runtime-evidence-contract-review`) → `human_review_required` (do NOT self-invent; this sub-sprint adds none).
- Landing B1 appears to need MODIFYING the A1/A3 dispatch-path gates → STOP (read `faq_miss` from `ToolEvent.resultData` at resolver time instead).
- Any hard-fenced surface needs editing; or S-Auto-11/12/13/13b or `b351648` would be reverted.
- **No `git add -A`** — stage explicitly. Any `autoloop run` only on a clean committed tree. **Local-Mac only.** Restart the backend (`mvn spring-boot:run`, no hot-reload) before the bad_cases measurement.

## Test / eval requirements

- **Java**: no NEW failures beyond `1198 / 1 / 0 / 2`; `Tests run` rises by the new B1 tests; `Failures` stays `1`.
- **eval_interactive pytest**: `495 / 8` preserved MODULO the minimal sync — synced cases PASS on the corrected reason; report the exact net delta; introduce no NEW failure beyond the synced set (the 8 OQ-S68.1 split-failures are not yours).
- **autoloop pytest** `276`; **17-fixture** `31`; **scoring SHA** `35305bd8…`.
- **B1 measurement (deliverable)**: restart `:8080`, 3-pass `bad_cases` (`cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1` ×3 at sim/bot temp=0/60s); read persisted per-iter traces; report `ESCALATION_MISSTAMP` 5/24 → ≤1 + per-case before/after reason + negative controls intact.

## §7 stanza (REQUIRED — walk before claiming done)

**Target failure layer:** `infra` (`resolveMaxStepsReason` evidence-aware) + `eval_spec` (minimal case_spec `escalation_reason` sync). No agent semantic *decision* changed; B1 changes which canonical reason a max-steps exit is LABELLED with, using `faq_miss` evidence the runtime already has.

**Tier-0 invariant:** NONE added. Stays `infra`; reuses the existing `turn_budget_exhausted` catch-all. Touches the Tier-0-*candidate* `R-escalation-reason-runtime-evidence-contract-review`; a Tier-0 elevation argument → `human_review_required` (do NOT self-invent). Not added to `runtime_freeze_and_risk_policy.md` §1/§2.

**Semantic hardcode:** None introduced; one REMOVED. B1 deletes the lossy `search_knowledge`-presence → faq-label heuristic and replaces it with a read of the EXISTING `faq_miss` flag + fall-through to the existing catch-all — net-LOWERS the hardcode surface. No new keyword/regex/enum/per-UC; no new `escalation_reason` value (§4 verdict). The eval sync expects the CORRECTED reason (§5.4 — not widening to accept a bug).

**Generalization coverage:** target = `ESCALATION_MISSTAMP` bad_cases (5/24; cs001/cs014/cs095/wmkb); neighbor = `promotion/` cases pinning `escalation_reason`; negative = genuine-miss / intake / clarification / mixed-turn-most-recent-wins; shadow = held-out. Counts confirmed at handoff via the 3-pass rerun (5→≤1) + the synced-case §5.4 evidence table.

## Codex review plan (per §4.3)

PER-SUB-SPRINT REQUIRED (trigger #1 + §5.4-sensitive). The deliver-agent authors `compact/sprint-070-codex-review-prompt.md` at close; you note dispatch status in the handoff (PENDING — deliver-agent at close).

## Handoff requirements

`docs/sprints/sprint-070-handoff.md`: §0 summary (scope, commits, counts incl. Java `1198/1`+new tests, eval `495/8`+sync delta); §1 the B1 resolver change (before/after; most-recent-`search_knowledge`-`faq_miss` read; precedence vs step-1/2 unchanged); §2 the minimal `escalation_reason` sync — per-case table (id, pre/post reason, `faq_miss=false` evidence, `eval_spec` class) proving §5.4 compliance; §3 negative controls; §4 `ESCALATION_MISSTAMP` 3-pass before/after + OQ-S67.2 re-measurement; §5 baselines + §7-stanza self-walk + fence disposition (incl. "OQ-S69.1 cross-turn NOT touched"); §6 OQs; §7 self-check. As the LAST sub-sprint, also summarize M-Auto-3 close-readiness (§11 gates met / split / open — incl. PARAPHRASE_STORM within-turn/cross-turn/total + OQ-S68.1 Python-baseline decision).

## Commit discipline

Multi-commit acceptable (B1+tests / eval sync / handoff). Message: `Sprint 070 / S-Auto-14 / M-Auto-3 — <description>` + footer `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`. No `git add -A`. Do not push.

## Self-check (verify before claiming done)

- [ ] `resolveMaxStepsReason` step 3 reads most-recent `search_knowledge` `faq_miss` (null/malformed guarded); `false`→`turn_budget_exhausted`, `true`→`faq_miss_threshold_exceeded`; step-1/2 UNCHANGED; Javadoc updated.
- [ ] NO new `escalation_reason` enum value; reuses `turn_budget_exhausted`.
- [ ] B1 tests cover fix + negatives (genuine miss / intake / clarification / mixed-turn most-recent / no-search).
- [ ] Eval `escalation_reason` sync MINIMAL + empirical (post-B1); only `faq_miss=false`-exhaustion cases; each justified §5.4; genuine-miss cases untouched; no case widened to accept a bot mistake.
- [ ] `ESCALATION_MISSTAMP` 5/24 → ≤1 (3-pass, backend restarted).
- [ ] A1/A3 gates, skills, tool-policy, user_simulator, loop.py, scoring, sandbox, meta_agent UNTOUCHED; b351648 not reverted; no Tier-0 self-invented; OQ-S69.1 + OQ-S68.1 NOT touched.
- [ ] Java no new failures beyond `1198/1/0/2` (+B1 tests); eval `495/8` modulo documented sync; autoloop `276`; 17-fixture `31`; scoring SHA `35305bd8…`.
- [ ] No `git add -A`; autoloop only on clean tree; backend restarted for measurement; local-Mac only.
- [ ] Handoff §0-§7 filled (incl. §2 §5.4 table + M-Auto-3 close-readiness); Codex dispatch status noted.
