Paste the content below this line into a fresh Codex session at Sprint 52 / M5 S3 close. This is a **per-sub-sprint Codex review** (locked human-confirmed addition per `docs/milestone_objective.md` §8 + `iteration_governance.md` §4.3 — S3 is the milestone's only semantic surface and is coupled to the eval trace contract, so it is reviewed in isolation BEFORE any S4 builds on it). No PR will be opened; review the S3 commit range directly. A milestone-shared review still runs at M5 close over the cumulative range.

**Commit-timing note (deliver-agent → human, read before dispatch):** at prompt-draft time the S3 dev work was uncommitted in the working tree (HEAD `cf9c120`). Before dispatching Codex, the human commits the S3 dev-scope files as a dev commit (`sprint 52 / M5 S3: projection audit C1 + Skill-driven convergence C2 (#3 moderation_context strip + #4 tool_schemas Skill-registry base)`) so the range is well-defined. The deliver-agent close bundle (objective archive + 10-handoff + action_bank row + this review's archive) lands AFTER this review. So at review time the range is `cf9c120..<S3-dev-commit>` and the close bundle is not yet committed.

**Deliver-agent evidence note (read before dispatch):** the **real-LLM bad-case rerun** (the C2 semantic-preservation evidence gate per the S3 contract + `iteration_governance.md` §5.6) is the **deliver-agent's** gate, run at S3 close — NOT Codex's. By dispatch time the deliver-agent will have recorded the rerun result (distribution vs the M4-close baseline) in `docs/sprints/sprint-052-handoff.md` §12 and/or the bad-case `_manifest.md`. Codex verifies the **landed code's** anti-hardcode + scope discipline; Codex does NOT re-run the bot and does NOT re-judge per-case bad-case verdicts (§5.6 human-judgment gate).

---

You are the Anti-Hardcode + Sub-Sprint-Close Review Agent for **Sprint 52 / M5 S3 — Projection audit (C1) + Skill-driven convergence (C2)** per `docs/sprint_objective.md`. S3 is the THIRD sub-sprint of **Milestone M5 — Observability Coherence** and the milestone's **ONLY semantic surface** (§7 REQUIRED; the other M5 sub-sprints S1/S2 are display/observation). This is why S3 gets a per-sub-sprint review (vs the M5 milestone-shared default).

**What landed in S3 (the review scope):** the dev delivered the **C1 field consumption matrix** (`docs/diagnostics/m5-s3-projection-consumption-map.md`), did a human-reviewed C1 gate (`AskUserQuestion`, 2026-05-25; human directive "Land #3 + #4 only; STOP-surface #2 + #5"), then landed exactly TWO C2 convergences:

- **C2 #3** — stripped the stale `moderation_context` entry from `resolve_faq_grounded_answer.yaml`'s `required_context_keys` (the C1 matrix established zero runtime consumer; the projection never emitted a slot keyed off this declaration).
- **C2 #4** — replaced the UC-driven `tool_schemas` base in `ContextProjectionBuilder.buildProjection(...)` with a Skill-registry-derived equivalent (`SkillRegistry.select(phase, uc).map(Skill::toolsRequired)`), with a single defensive fallback to the pre-M2 `ToolPolicyEnforcer.getVisibleToolsForUc(uc)` for tuples the registry does not map.

**What did NOT land (STOP-surfaced to S4 per the C1 matrix HIGH-risk analysis + human directive):** C2 #2 (Skill-declared context-key gating) → OQ-S52.1; C2 #5 (`state_inheritance.soft_signal_via_projection` gating) → OQ-S52.2. These are NOT in scope for this review (no code landed); note only that they were correctly STOP-surfaced (not smuggled in).

---

## 1. Loader

Read in this order on a fresh Codex session. Do NOT skip.

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `agent_context_guide.md` + `iteration_governance.md` §1 / §1.3 / §1.4 / §1.7 / §3.2 / §4.1 / §4.2 / §4.3 / §5 / §5.6 / §7 / §8).
2. **`docs/milestone_objective.md`** — M5 north star. Especially §1 (S3 = the only §7-REQUIRED semantic surface), §2 (#3 goal — Skill-driven projection WITHOUT changing LLM-visible semantic information), §3 (S3 paragraph; "no projection field deleted/changed before the C1 map confirms no consumer"), §6 (S3/S4 hard fences), §8 (the per-sub-sprint S3 Codex review this prompt fulfils).
3. **`docs/sprint_objective.md`** — the Sprint 52 contract. The C1-before-C2 hard ordering, the §7 stanza (binding), the "registry/Skill-driven — no per-UC if-else" fence, and the "real-LLM rerun is the evidence gate; mocked covers wiring only" clause.
4. **`docs/sprints/sprint-052-handoff.md`** — the dev handoff. §2 (per-item scope), §3 (the dev's own §4.1 self-walk — re-verify, do not echo), §4 (hard fences honoured), §5 (footprint numstat), §6 (baselines + the real-LLM rerun STOP-surface), §7 (OQ-S52.1/2/3/4), §12 (deliver-agent close verdict + the rerun result, filled before dispatch).
5. **`docs/diagnostics/m5-s3-projection-consumption-map.md`** — the C1 deliverable (LOAD-BEARING). This is the justification for every C2 decision. Verify: (a) §3.R / §4-row-#3 establish `moderation_context` has zero consumer (system_prompt / eval-trace-contract / eval-scoring / `buildDriftAndTaskHistory` / other-Skill-decl / sampled-LLM-trace all show no read) → the #3 strip is safe; (b) §3.K / §4-row-#4 establish the `buildProjection(...)` base IS consumed by legacy callers (`PhaseEvaluator.evaluateResolveFaq*`, `ControlKernel.recordTurn`) → #4 must REPLACE (registry-driven), not delete; (c) §4 rows #2 + #5 justify the HIGH-risk STOP-surface.
6. **`docs/current/iteration_governance.md`** — §1.3 LLM-owned; §1.4 Runtime-owned (note: the tool whitelist / `tools_required` is explicitly Runtime-owned, so consolidating it at a single Skill-registry source is a §1.4-consistent move, not a §1.3 LLM-ownership shift); §1.7 forbidden-list; §3.2 layer classification; §4.1 nine-question kernel (re-walk at §3 below); §4.2 header; §7 stanza.
7. **`docs/runtime_freeze_and_risk_policy.md`** §1 + §2 — verify S3 ADDED no Tier-0 invariant and removed no safety floor.
8. **Code at HEAD `<S3-dev-commit>`** (read on demand during §3/§4):
   - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` — the C2 #4 `tool_schemas` base + the NEW `resolveProjectedToolNames(BotSession, String)` helper (~`:1360+`). **The load-bearing diff.**
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java` — `select(String phase, String useCase)` (~`:65`) — confirm the helper's registry source.
   - `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml` — the C2 #3 strip.
   - `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint52ProjectionSkillDrivenTest.java` (7 tests) + the `PhaseEvaluatorResolveSkillIntegrationTest.java` golden update (4→3 `requiredContextKeys`).

---

## 2. Scope claim

`git show <S3-dev-commit> --stat`. Expected EXACTLY: `ContextProjectionBuilder.java` (+59/-13), `resolve_faq_grounded_answer.yaml` (-1), `PhaseEvaluatorResolveSkillIntegrationTest.java` (golden +1/-2), NEW `Sprint52ProjectionSkillDrivenTest.java`, NEW `docs/diagnostics/m5-s3-projection-consumption-map.md`, NEW `docs/sprints/sprint-052-handoff.md`. Verify:

- **No `eval_interactive/**` / `eval/**` / `data/**` / `composite.py` / eval-fixture touch** (`git diff --stat <range> -- eval_interactive/ eval/ data/` empty).
- **No `ui/**` touch** (S3 is server-side projection logic only).
- **No `system_prompt.txt` edit**; no Skill `critical_steps[].desc` / `procedure` edit; no other Skill YAML edit beyond the single `moderation_context` line.
- **No S2 surface touch** — `bot_turn_llm_calls` schema, the `/trace` endpoint, `AgentRunLoopImpl`, `TraceWriter`, `ControlKernel.recordRunResult`/`mergeFaqGroundingIntoProjection` are UNCHANGED (S3 consumes S2's per-step projections read-only as C1 evidence; the OQ-S51.2 overlay is explicitly NOT touched).
- **C2 #2 + #5 did NOT land** — confirm no `requiredContextKeys`-driven projection gating (#2) and no `soft_signal_via_projection` gating (#5) code is present; they are STOP-surfaced, not implemented.

---

## 3. §4.1 nine-question anti-hardcode kernel walk

Walk each of the nine questions against the S3 diff. **C2 #4 (`resolveProjectedToolNames`) is the load-bearing item.** For each "yes"/concern, paste the diff snippet (cite path:line) + reasoning.

- **Q1 (semantic hardcode added?)** — The central question. Is `resolveProjectedToolNames` registry/Skill-driven with a SINGLE defensive fallback, or does it encode a per-UC if-else / keyword / regex / enum? Confirm: it calls `skillRegistry.select(phase, activeUseCase)` and returns `Skill.toolsRequired()` when matched; the only non-registry branch is the single `toolPolicyEnforcer.getVisibleToolsForUc(activeUseCase)` fallback for UNMAPPED tuples (mirroring pre-M2 behaviour) + the null-guards. There must be no `if (uc.equals("UC-A"))`-style branch, no hardcoded tool-name literal that routes by UC, no message-content match. C2 #3 is a declaration REMOVAL — confirm it cannot add a hardcode.
- **Q2 (Tier-0 invariant protection?)** — Expected NO new Tier-0. C2 #4 narrows/redirects a projection source; C2 #3 removes a dangling declaration. Neither adds a guard.
- **Q3 (soft signal vs hard branch?)** — C2 #4 moves tool-schema projection from a UC-driven Java palette to Skill-registry declarations (registry data). Confirm it does not introduce a NEW hard branch over a decision the LLM owns. (Note §1.4: `tools_required` is the Runtime-owned tool whitelist; consolidating it at the Skill registry is a §1.4-internal source-of-truth move, not a §1.3 shift.)
- **Q4 (eval-phrase / CaseSpec-id encoded?)** — Walk for any visible-eval case_id / trace phrasing / CaseSpec id in the diff. Expected none (the change reads `phase` + `activeUseCase` generically).
- **Q5 (LLM ownership shrunk?)** — Does S3 shrink what §1.3 says the LLM owns? Expected NO — the opposite: it removes a Java UC palette in favour of Skill-registry declarations the LLM operates within. The bot's semantic decisions (UC hypothesis, next action, escalation) are untouched.
- **Q6 (prompt if-else added?)** — `system_prompt.txt` UNTOUCHED — verify empty diff.
- **Q7 (tool schema / capability / PII / grounding floor preserved?)** — Verify: for MAPPED Skills the projected `tool_schemas` set equals the post-`build(...)` `:845-863` plan-filtered overwrite (the run-loop steady state — the dev claims byte-identical; spot-check that `Skill.toolsRequired()` == `plan.allowedTools()` for at least the FAQ Skill via the golden `allowedTools()` list, which is UNCHANGED in the test diff). PII redaction + grounding floor (the `mergeFaqGroundingIntoProjection` overlay) UNTOUCHED.
- **Q8 (generalization coverage?)** — `Sprint52ProjectionSkillDrivenTest` (7 tests): target = Skill-matched uses `toolsRequired` not the UC palette (Mockito `never()` on `getVisibleToolsForUc`); neighbor = the steady-state overwrite no-op; negative = Skill-unmatched falls back, null-uc-no-wildcard emits empty (no spurious projection); the #3 guards (no `moderation_context` emit + YAML decl absent). The **real-LLM bad-case rerun is the semantic-preservation gate** (deliver-agent-run; see §5 + handoff §12) — the wiring tests alone are NOT sufficient evidence that LLM-visible semantics held for the legacy `buildProjection(...)` paths.
- **Q9 (rollback / sunset?)** — C2 #3 + #4 are the M2-correct steady state (durable; N/A). `git revert <S3-dev-commit>` reverses both. #2 + #5 STOP-surfaced to S4 (the documented continuation).

---

## 4. S3 hard-fence walk (`sprint_objective.md` §"Hard fences" + `milestone_objective.md` §6 S3/S4)

Walk each + verify NO violation:

1. **C1 BEFORE C2** — the C1 matrix `docs/diagnostics/m5-s3-projection-consumption-map.md` was committed AND reviewed (`AskUserQuestion` at C1 review, 2026-05-25) BEFORE any C2 field change. Verify the matrix exists and that #3 + #4 each cite a matrix row that confirms consumer-safety.
2. **Registry/Skill-driven — no per-UC if-else** — re-confirm via §3 Q1.
3. **No field deleted/changed before C1 confirms no consumer** — #3 strips a declaration the matrix shows has zero consumer; #4 REPLACES (not deletes) the base because the matrix shows legacy callers consume it. Confirm no OTHER projection field was changed.
4. **No change to LLM-visible semantic information for the run-loop primary path** — the run-loop `build(...)` `:845-863` plan-filtered overwrite makes the run-loop tool_schemas byte-identical; the cold-edge legacy `buildProjection(...)` paths see the Skill-narrowed palette (the real-LLM rerun is the proof — verify the deliver-agent recorded it in §12).
5. **OQ-S51.2 FAQ-grounding overlay UNTOUCHED** — `mergeFaqGroundingIntoProjection` not in the diff.
6. **No `escalation_reason` enum / tool-schema definition / PII / safety / grounding-floor change**; no scoring / eval-fixture change.
7. **STOP-and-surface honored for #2 + #5** — confirm they are deferred (OQ-S52.1/2), not partially landed.

The headline fence is **C1-before-C2** + **registry/Skill-driven (no per-UC if-else)**.

---

## 5. Real-LLM rerun + C1 matrix verification (deliver-agent evidence, Codex confirms consistency)

The deliver-agent runs the real-LLM bad-case rerun at S3 close (the §5.6 evidence gate) and records the distribution in `docs/sprints/sprint-052-handoff.md` §12 / the bad-case `_manifest.md`. **Codex does NOT re-run the bot.** Codex verifies:

(a) The deliver-agent's recorded rerun distribution is consistent with the claim that C2 #3 + #4 preserved LLM-visible semantics — i.e., the M4-close baseline (PASS×5 [cs001, cs014, cs029, cs066, fg5q] + IMPROVING×4 [alice, cs011, cs012, wmkb] + FAIL×3 [cs015, cs095, iwzx] + OOSR×0) HOLDS, with no PASS→FAIL flip attributable to the projection change. A PASS→FAIL flip on the rerun is a P0 in-flight downgrade.
(b) The C1 matrix's two load-bearing claims hold against the code: the #3 "zero consumer" claim (spot-check there is no `moderation_context` read in `system_prompt.txt` / the eval trace contract / `buildDriftAndTaskHistory` / another Skill YAML — note the `get_message_moderation_context` TOOL + `BotSession.moderationContext` session field are a SEPARATE concept from the stripped `required_context_keys` projection declaration; confirm the strip does not affect those) and the #4 "base IS consumed by legacy callers" claim (`grep -rn '\.buildProjection(' server/src/main/java` shows the `PhaseEvaluator` + `ControlKernel.recordTurn` callers).

If the recorded rerun shows a regression, or the matrix claims do not hold against the code, surface as a P0 finding.

---

## 6. OQ disposition

For each, return a 1-3 sentence assessment + recommended disposition (`closed` / `closed-with-followup` / `surface-as-r-item` / `defer-to-S4` / `human-architecture-decision`):

1. **OQ-S52.1** — C2 #2 (Skill-declared context-key gating) STOP-surfaced: the matrix flagged non-declaring-Skill omissions (`discover_triage` does not declare `customer_context`; only `resolve_faq_grounded_answer` declares `listing_context`) as not-provably-intentional; gating without a Skill-declaration audit + real-LLM rerun risks dropping LLM-visible signals. Confirm the STOP was correct (not over-cautious) and recommend whether #2 belongs in S4 or a follow-on milestone.
2. **OQ-S52.2** — C2 #5 (`soft_signal_via_projection` gating) STOP-surfaced: correct convergence shape (registry-driven) but the §5.6 real-LLM rerun on DISCOVER→RESOLVE bad cases (alice/cs011/cs012/wmkb) was the gating evidence. Recommend S4 vs defer.
3. **OQ-S52.3** — OQ-S51.2 (FAQ-grounding overlay) disposition: human chose option (a) leave-as-is + note at S3. Confirm this is consistent (the overlay is benign + pre-existing; no scorer reads `projection.faq_grounding`) and whether the three future dispositions in matrix §6 warrant an R-item.
4. **OQ-S52.4** — `knowledge_hits` / `accumulated_tool_results` dual knowledge-projection path (M5 §7 flagged): matrix §3.N confirms `knowledge_hits` is NOT dead (legacy `PhaseEvaluator.evaluateResolveFaq*` pass non-null hits). Recommend disposition (likely defer-to-S4 or a future M5+ milestone for the canonical-path question).

---

## 7. Output format

Write your verdict to the top of `docs/codex-findings.md` (delete-and-add supersession; the deliver-agent archives the live file to `docs/sprints/sprint-052-codex-review.md` after this review). Use the §4.2 4-line header:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph — S3 verdict: C1-before-C2 discipline + §4.1 kernel (C2 #4 registry/Skill-driven, C2 #3 declaration-removal) + §1.7 compliance + hard-fence honor + real-LLM-rerun-consistency + #2/#5 correctly STOP-surfaced + OQ disposition>
```

Then below: (1) §3 nine-question kernel results (C2 #4 is load-bearing); (2) §4 hard-fence walk (C1-before-C2 + registry-driven are the headline); (3) §5 real-LLM-rerun + C1-matrix consistency; (4) §6 OQ disposition table (4 rows); (5) reproducibility spot-check of the dev's cited numbers (1172/1/0/2 Java; the footprint numstat; the golden 4→3 keys) per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`; (6) optional Codex-surfaced new findings (P0-P3 with cite); (7) a 1-2 paragraph judgment on whether S3 ships as one coherent Skill-driven convergence (C1-gated, registry-driven, with the high-risk #2/#5 correctly deferred) or has inconsistency / orphaned wiring (e.g. is the `getVisibleToolsForUc` fallback genuinely defensive-for-unmapped-tuples, or does it mask a registry-coverage gap that should have been a STOP-surface?).

## 8. Anti-pre-decision discipline

Per `feedback_constitution_discipline_vs_planning_anticipation.md`: surface findings + evidence; do NOT pre-decide M5-blocking vs S4-deferral routing — deliver-agent + human + Codex decide jointly. The §4.2 header IS your binding verdict on whether S3 can close PASS. Do NOT edit code; do NOT propose fixes beyond naming the `iteration_governance.md` §3 layer. Do NOT edit sprint archives or the objective archive the deliver-agent lands at close.
