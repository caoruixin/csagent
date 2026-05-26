Paste the content below this line into a fresh Codex session at Sprint 53 / M5 S4 close. This is a **combined review** (human decision 2026-05-25): it serves BOTH the mandated **per-sub-sprint S4 Codex review** (`docs/sprint_objective.md` §"Codex review plan" — S4 is the highest-risk M5 semantic surface) AND the **§8 milestone-shared M5 review**, in one dispatch over the cumulative M5 commit range. No PR will be opened; review the commit range directly.

**Commit-timing note (deliver-agent → human, read before dispatch):** at prompt-draft time the S4 dev work was uncommitted in the working tree (HEAD `60df77b`). Before dispatching Codex, the human commits the S4 dev-scope files (`ContextProjectionBuilder.java`, `Sprint53SkillDeclarationGatingTest.java`, `docs/diagnostics/m5-s4-skill-declaration-audit.md`, `docs/sprints/sprint-053-handoff.md`) PLUS the deliver-agent's pre-Codex evidence files (`eval_interactive/case_specs/bad_cases/_manifest.md` M5-close section, and this prompt) as the pre-Codex commit, so the range is well-defined. The M5 milestone-close bundle (objective + codex-findings archives, 10-handoff, action_bank, milestone reset) lands AFTER this review. So at review time the range is `84ae017..<S4-pre-codex-commit>` and the close bundle is not yet committed. This is the same dispatch-discipline pattern S3 used (record evidence + commit BEFORE Codex per memory `feedback_milestone_close_bad_case_before_codex`).

**Deliver-agent evidence note (read before dispatch):** the **real-LLM bad-case rerun AND the shadow rerun** (the two mandatory S4 evidence gates per the contract + `iteration_governance.md` §5.6) are the **deliver-agent's** gates, run at S4 close — NOT Codex's. The deliver-agent recorded both distributions in `docs/sprints/sprint-053-handoff.md` §12 + the bad-case `_manifest.md` "M5 milestone close + S4 (Sprint 53)" section. **Codex does NOT re-run the bot, does NOT run the shadow set, and does NOT re-judge per-case bad-case verdicts** (§5.6 human-judgment gate). Codex verifies the LANDED code's anti-hardcode + scope discipline and confirms the recorded evidence is consistent with the semantic-preservation claim.

---

You are the Anti-Hardcode + Sub-Sprint-Close + Milestone-Close Review Agent for **Sprint 53 / M5 S4 — Skill-declaration audit (Phase A) + context-key gating (#2) + soft-signal gating (#5)** per `docs/sprint_objective.md`, reviewed at the close of **Milestone M5 — Observability Coherence**.

**Scope structure (combined review):** The substantive review is **S4** (the highest-risk M5 sub-sprint and, with S3, one of only two semantic surfaces in M5). The milestone-shared dimension is light: **S1 (Sprint 50)** is eval-harness display + a foundational phase5 fold-back (§7-exempt, no bot code); **S2 (Sprint 51)** is observation-only per-invocation trace persistence (§7-exempt, no loop-behaviour change); **S3 (Sprint 52)** already passed a per-sub-sprint review (`docs/sprints/sprint-052-codex-review.md`, `pass / 0`). Confirm S1/S2/S3 are present in the cumulative range and that S4 did not smuggle changes into their surfaces; do not re-litigate S3's already-reviewed diff.

**What landed in S4 (the review focus):** a two-phase, audit-gated projection convergence completing the M5 #3 work S3 began.

- **Phase A — Skill-declaration completeness audit** (`docs/diagnostics/m5-s4-skill-declaration-audit.md`, 462 lines): every candidate slot × the 6 production Skills, classified DECL / EMIT-today / NEED / RISK-if-gated, routed to GATE / KEEP-UNCONDITIONAL / OUT-OF-SCOPE. Delivered AND human-reviewed (`AskUserQuestion`, 2026-05-25; human chose "Land Phase B per audit; 4-slot gate; no YAML edit") BEFORE any Phase-B code edit. Outcome: 4 slots GATE-routed, 4 KEEP-UNCONDITIONAL, 1 (`intake_state`) OUT-OF-SCOPE; **zero STOP-AND-SURFACE; zero YAML edits** (#1a concluded declarations are intentional as-is for the 4 gated slots).
- **Phase B #2 — context-key gating**: `candidate_use_cases` emission wrapped in `skillRequiresContextKey(session, activeUc, "candidate_use_cases")`, reading `Skill.requiredContextKeys()` (only `discover_triage` declares + needs it).
- **Phase B #5 — soft-signal gating**: the three soft-signal slots (`alternate_candidate_use_cases`, `discover_disambiguation_signals`, `prior_use_case_carry`) wrapped in `skillDeclaresSoftSignal(...)`, reading `Skill.stateInheritance().softSignalViaProjection()` (DISCOVER-only for the first two; resolve_faq + resolve_intake for `prior_use_case_carry`).
- Two NEW private helpers, both defaulting to `true` (emit) for null-registry / null-session / null-phase / unmapped-tuple — defensive pre-S4 preservation. 11 NEW tests (`Sprint53SkillDeclarationGatingTest`).

**What did NOT land (deferred per the contract's CONDITIONAL #4 default):** C3 dedup/denoise + OQ-S52.4 `knowledge_hits` canonicalization → routed to a separate "projection hygiene" milestone candidate (human decision 2026-05-25). NOT in scope for this review; confirm they were not partially smuggled in.

---

## 1. Loader

Read in this order on a fresh Codex session. Do NOT skip.

1. **`AGENTS.md`** (transitively loads `doc_governance.md` + `agent_context_guide.md` + `iteration_governance.md` §1 / §1.3 / §1.4 / §1.7 / §3.2 / §4.1 / §4.2 / §4.3 / §5 / §5.5 / §5.6 / §7 / §8).
2. **`docs/milestone_objective.md`** — M5 north star. §1 (S3/S4 = the §7-REQUIRED semantic surfaces; S1/S2 display/observation), §2 (#3 goal — Skill-driven projection WITHOUT changing LLM-visible semantic information), §3 (S4 paragraph: Phase A audit BEFORE Phase B gating; #4 CONDITIONAL defaults to DEFER; shadow + bad-case reruns mandatory), §5 (regression-safety acceptance bar), §6 (S3/S4 hard fences), §8 (milestone-shared review default + per-sub-sprint S4 trigger this prompt fulfils).
3. **`docs/sprint_objective.md`** — the Sprint 53 contract. The AUDIT-BEFORE-GATING hard fence, the §7 stanza (binding), the "registry/Skill-driven — no per-UC if-else" fence, the "no change to LLM-visible semantic information" fence, the "real-LLM + shadow rerun mandatory; mocked covers wiring only" clause, the CONDITIONAL-#4-default-defer clause.
4. **`docs/sprints/sprint-053-handoff.md`** — the dev handoff. §1 (goal/outcome), §2 (per-item scope; Phase A + Phase B), §3 (the dev's own §4.1 self-walk — re-verify, do not echo), §4 (hard fences honoured), §5 (footprint numstat), §6 (baselines; §6.2 empirical gating verification; §6.3 the recovered-LLM bad-case rerun + STOP-surface of the degraded run; §6.4 shadow STOP-surfaced to deliver-agent), §7 (R-items), §12 (deliver-agent close verdict + the bad-case + shadow rerun results, filled before dispatch).
5. **`docs/diagnostics/m5-s4-skill-declaration-audit.md`** — the Phase-A deliverable (LOAD-BEARING). This is the justification for every Phase-B gating decision. Verify: (a) §3.E routes `candidate_use_cases` → GATE on `discover_triage` only (the audit's NEED column matches the DECL column → gating drops no signal); (b) §3.G/§3.H route `alternate_candidate_use_cases` + `discover_disambiguation_signals` → GATE on `discover_triage` only; (c) §3.I routes `prior_use_case_carry` → GATE on resolve_faq + resolve_intake; (d) §4.B the 4 KEEP-UNCONDITIONAL slots (`form_context`, `customer_context`, `listing_context`, `conversation_history`) stay unconditional because the audit shows dropping them risks an LLM-visible signal the bad-case suite depends on (INHERIT-RISK exception); (e) `intake_state` OUT-OF-SCOPE (the existing `IntakeFieldsRegistry` gate is canonical). **The headline Phase-A claim to verify: every GATE-routed slot's DECL coverage == its NEED column (no dropped signal), which is why #1a = zero YAML is correct, not a shortcut.**
6. **`docs/current/iteration_governance.md`** — §1.3 LLM-owned (soft-signal interpretation stays LLM-owned); §1.4 Runtime-owned (projection is inside "trace and eval contract"; gating which slots project per Skill declaration is a §1.4-internal source-of-truth move, not a §1.3 shift); §1.7 forbidden-list; §3.2 layer classification (S4 = `prompt_projection` Q3); §4.1 nine-question kernel (re-walk at §3); §4.2 header; §5.5/§5.6 (why programmatic pass rate is observation-only — relevant to reading the shadow + bad-case evidence); §7 stanza.
7. **`docs/runtime_freeze_and_risk_policy.md`** §1 + §2 — verify S4 ADDED no Tier-0 invariant and removed no safety floor.
8. **Code (read on demand during §3/§4):**
   - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` — the four gated emissions (~`:411`/`:439`/`:478`/`:503`) + the two NEW helpers `skillRequiresContextKey` + `skillDeclaresSoftSignal` (~`:1446+`). **The load-bearing diff.**
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` + `StateInheritance.java` — confirm `requiredContextKeys()` / `stateInheritance().softSignalViaProjection()` are non-null by record invariant (the helpers rely on this for null-safety).
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java` — `select(phase, useCase)` (the registry source the gates read).
   - `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint53SkillDeclarationGatingTest.java` (11 tests).

---

## 2. Scope claim

`git show <S4-pre-codex-commit> --stat` (or `git diff 60df77b..<commit> --stat` for the S4 delta). Expected S4 dev scope EXACTLY: `ContextProjectionBuilder.java` (+148/-26), NEW `docs/diagnostics/m5-s4-skill-declaration-audit.md`, NEW `server/src/test/java/.../Sprint53SkillDeclarationGatingTest.java`, `docs/sprints/sprint-053-handoff.md` (§12 deliver-agent-filled), `eval_interactive/case_specs/bad_cases/_manifest.md` (deliver-agent M5-close evidence section), this prompt. Verify the S4 delta:

- **ZERO YAML edits** — `git diff 60df77b..<commit> -- server/src/main/resources/skills/` is EMPTY (the #1a audit conclusion; the gate reads existing declarations, adds none).
- **No `eval_interactive/case_specs/**` / `composite.py` / scoring / eval-fixture touch** beyond the `bad_cases/_manifest.md` evidence ledger (deliver-agent-owned §5.6 record, not a scoring change). No `eval/**` / `data/**` touch.
- **No `ui/**` touch** (S4 is server-side projection logic only).
- **No `system_prompt.txt` edit**; no Skill `critical_steps[].desc` / `procedure` edit.
- **No S2 surface touch** — `bot_turn_llm_calls`, the `/trace` endpoint, `AgentRunLoopImpl`, `TraceWriter`, `ControlKernel.recordRunResult` / `mergeFaqGroundingIntoProjection` are UNCHANGED (the OQ-S51.2 overlay is explicitly NOT touched).
- **No `escalation_reason` enum / tool-schema-definition / PII / safety / grounding-floor change.**
- **CONDITIONAL #4 did NOT land** — no C3 dedup/denoise; no `knowledge_hits` canonicalization. Deferred, not partially landed.
- **Milestone-shared spot-check** — S1 (`9ef9d1e`), S2 (`cf9c120`), S3 (`49d48b1`), docs-archive (`60df77b`) are in the cumulative range `84ae017..<commit>`; S4 did not edit S1's eval-harness display files, S2's trace surface, or S3's `resolve_faq_grounded_answer.yaml` / `tool_schemas` helper.

**In-range governance condensation (expected; bundled at human direction 2026-05-25 — NOT an S4 semantic-scope item):** the S4 pre-Codex commit ALSO contains a human-authored **docs / config-governance** condensation of `docs/current/iteration_governance.md` (−146/+30) + a NEW `compact/anti-hardcode-review-kernel.md` (the §4.1 nine-question kernel extracted to a single canonical compact file, which the doc now links to). This is **§4.1-EXEMPT** (docs-only / config-governance) and is **not** part of S4's `prompt_projection` review. Treat it as a packaging-note bundle (A-with-packaging-note per `deliver_close_taxonomy.md`), NOT a scope violation. The ONE thing to confirm (because it is a constitution-tier doc): the condensation is **content-preserving** — the extracted kernel holds the nine questions + scope-exemption + four verdicts byte-identical to the removed §4.1 block, and the §5.5/§5.6/§8 condensations did not alter any normative rule (the close gates, the §5.6 human-judgment-gate principle, the §8 milestone framework, the §7 stanza all remain). If you find a dropped or altered normative rule, surface it as a finding; otherwise note it approved-as-docs-condensation and move on. Do NOT re-walk the nine-question kernel against this docs change (it is exempt).

---

## 3. §4.1 nine-question anti-hardcode kernel walk (S4 focus)

Walk each of the nine against the S4 diff. **The two gating helpers are the load-bearing items.** For each "yes"/concern, paste the diff snippet (cite path:line) + reasoning.

- **Q1 (semantic hardcode added?)** — The central question. Do `skillRequiresContextKey` + `skillDeclaresSoftSignal` gate on REGISTRY DATA (`skillRegistry.select(phase, uc).map(Skill::requiredContextKeys / stateInheritance().softSignalViaProjection())` membership) or do they encode a per-UC if-else / keyword / regex / enum? Confirm: the gating decision is "does the selected Skill declare this slot?" — registry membership, NOT a UC branch. The only non-registry branch is the single uniform `return true` for null-registry / null-session / null-phase / empty-`select` (unmapped tuples) — defensive pre-S4 preservation, not a UC matrix. There must be no `if (uc.equals("UC-X"))`-style branch and no slot-name routed by UC literal.
- **Q2 (Tier-0 invariant protection?)** — Expected NO new Tier-0. Projection is inside Runtime's "trace and eval contract" (§1.4); the gate adds no kernel guard.
- **Q3 (soft signal vs hard branch?)** — The change moves projection-shape decisions from unconditional/UC-driven Java to the Skill registry (YAML declarations). The soft signals stay LLM-owned (§1.3): gating decides WHETHER a slot is projected for a Skill; the LLM still decides whether to act on it. Confirm no NEW hard branch over an LLM-owned decision.
- **Q4 (eval-phrase / CaseSpec-id encoded?)** — Walk for any visible-eval case_id / trace phrasing / CaseSpec id in the diff. Expected none (the helpers read `phase` + `activeUseCase` + slot-name strings generically; the slot-name literals are projection-field names, not eval phrases).
- **Q5 (LLM ownership shrunk?)** — Does S4 shrink what §1.3 says the LLM owns? Expected NO — the opposite: it converges projection shape to Skill-registry declarations the LLM operates within; the bot's classification / escalation / response-strategy / soft-signal reads are untouched.
- **Q6 (prompt if-else added?)** — `system_prompt.txt` UNTOUCHED — verify empty diff.
- **Q7 (tool schema / capability / PII / grounding floor preserved?)** — `tool_schemas` untouched (S3 C2 #4 already converged it); PII redaction untouched; grounding floor (`mergeFaqGroundingIntoProjection`) UNTOUCHED; Tier-0 hard_checks schema unchanged.
- **Q8 (generalization coverage?)** — `Sprint53SkillDeclarationGatingTest` (11 tests): POSITIVE (discover_triage emits all 3 DISCOVER slots; resolve_faq + resolve_intake emit `prior_use_case_carry`), **NEGATIVE — the key contract** (confirm/escalate/terminal receive none of the 4; resolve_faq does NOT get the DISCOVER-only slots but DOES get `prior_use_case_carry` — i.e. a Skill that NEEDS a slot still receives it = no dropped signal), DEFENSIVE (unmapped tuple + null phase emit all 4 = pre-S4 preserved), KEEP-UNCONDITIONAL (confirm still gets form/customer/listing/conversation context — data-gated not Skill-gated), and the **PRODUCTION-YAML guard** `productionSkillYamls_matchAuditDeclarationCoverage` (loads real Skill YAMLs + asserts every declaration cell matches the audit's NEED column — the regression guard against future stale declarations). **The real-LLM bad-case rerun + the shadow rerun are the semantic-preservation gates** (deliver-agent-run; §5 below + handoff §12 / §6.3 + the `_manifest.md` M5-close section) — the wiring tests alone are NOT sufficient evidence.
- **Q9 (rollback / sunset?)** — Not temporary: Phase B is the M2-correct steady-state convergence the C1 (S3) + Phase-A (S4) audit chain justifies. `git revert <commit>` reverses both gates + the helpers.

---

## 4. S4 hard-fence walk (`sprint_objective.md` §"Hard fences" + `milestone_objective.md` §6 S3/S4)

Walk each + verify NO violation:

1. **AUDIT BEFORE GATING** (the headline fence) — the Phase-A audit `docs/diagnostics/m5-s4-skill-declaration-audit.md` was delivered AND human-reviewed (`AskUserQuestion`, 2026-05-25, option (a)) BEFORE any Phase-B code edit. Verify the audit exists, resolves every candidate slot to GATE / KEEP-UNCONDITIONAL / OUT-OF-SCOPE with no unresolved gap, and that each GATE-routed slot's NEED column == its DECL coverage (so gating drops no LLM-visible signal). This is the S4 analog of S3's C1-before-C2.
2. **Registry/Skill-driven — no per-UC if-else** — re-confirm via §3 Q1.
3. **No change to LLM-visible semantic information** — gating changes WHICH slots project per Skill, never WHETHER the LLM can see a signal it could see before for the cases that exercise that Skill. The proof is the real-LLM bad-case rerun + the shadow rerun (§5). The NEGATIVE control test (a Skill that needs a slot still gets it) is the wiring-level proof.
4. **#1a = zero YAML** — confirm the audit's conclusion that the 4 GATE-routed slots' declarations are intentional/complete as-is (so adding declarations would be churn-for-zero-behaviour) and the 4 KEEP-UNCONDITIONAL slots are NOT gated (adding declarations there would not change Phase-B behaviour). Verify no YAML was edited.
5. **OQ-S51.2 FAQ-grounding overlay UNTOUCHED**; **S2 `bot_turn_llm_calls` / `/trace` surface UNTOUCHED.**
6. **No `escalation_reason` enum / tool-schema / PII / safety / grounding-floor change**; no scoring / eval-fixture change.
7. **CONDITIONAL #4 DEFERRED** — confirm C3 + `knowledge_hits` were not expanded into S4.

The headline fences are **AUDIT-BEFORE-GATING** + **registry/Skill-driven (no per-UC if-else)** + **no dropped LLM-visible signal** (the NEGATIVE control).

---

## 5. Real-LLM bad-case + shadow rerun verification (deliver-agent evidence; Codex confirms consistency)

The deliver-agent ran BOTH mandatory S4 gates at close and recorded them in `docs/sprints/sprint-053-handoff.md` §12 + the bad-case `_manifest.md` "M5 milestone close + S4 (Sprint 53)" section. **Codex does NOT re-run the bot or the shadow set.** Codex verifies:

(a) **Bad-case (recovered-LLM)** — the recorded distribution is consistent with the claim that S4's gating preserved LLM-visible semantics: the M4-close baseline HOLDS (PASS×5 [cs001, cs014, cs029, cs066, fg5q] + IMPROVING×4 [alice, cs011, cs012, wmkb] + FAIL×3 [cs015, cs095, iwzx] + OOSR×0), with no PASS→FAIL flip attributable to the gating. The two contract_violations on the main run (cs015 + fg5q) cleared on 2× iso (the documented `R-bad-case-parallel-session-establishment-flakiness` shape). NOTE the provider context: the dev's initial 09:20 run was during an upstream bot-LLM degradation and was correctly STOP-and-surfaced (per §5.5 external-provider-drift confounding); the recovered-LLM run (~12:14+) is the evidence gate. A PASS→FAIL flip attributable to the gating would be a P0 in-flight downgrade.
(b) **Shadow (NEW S4 gate, deliver-agent ran dev-blind)** — 22 held-out cases: 19/22 reached the bot with healthy multi-turn outcomes (mean_outcome 0.711, mean_turns 3.1); the 3 outcome-0 cases are ALL session-establishment failures where the bot was never reached (`turns_traced=0`, `elapsed_ms=0`) — cs32s02 (contract → cleared on iso) + cs59s01/cs59s02 (deterministic HTTP-400 on empty-form session-create, a pre-existing shadow-fixture issue). Confirm this is consistent with no S4 regression (the gating runs only inside a bot turn; the 3 zero-outcome cases never reached one). Programmatic 0/22 is the empty-scoring-schema artifact, observation-only per §5.5/§5.6 (the same convention the bad-case suite uses).
(c) **Empirical gating verification** — handoff §6.2/§6.3 record per-case last-turn projections showing the 3 DISCOVER-only soft signals gated OUT for RESOLVE Skills and `prior_use_case_carry` emitted for resolve_faq + resolve_intake (+ one DISCOVER turn with all 3 DISCOVER signals present + `prior_use_case_carry` absent). Confirm this matches the Phase-A audit's NEED column.

If the recorded reruns show a regression attributable to the gating, or the audit's NEED==DECL claims do not hold against the code, surface as a P0 finding.

---

## 6. OQ disposition

For each, return a 1-3 sentence assessment + recommended disposition (`closed` / `closed-with-followup` / `surface-as-r-item` / `defer-to-projection-hygiene-milestone` / `human-architecture-decision`):

1. **OQ-S52.1 (#2) + OQ-S52.2 (#5)** — the S3-STOP-surfaced items, now LANDED in S4 Phase B gated by the Phase-A audit. Confirm the convergence is the correct shape (registry-driven, audit-gated, NEGATIVE-control-proven) and recommend `closed`.
2. **OQ-S52.4 / OQ-S53.3 (#4 — C3 dedup + `knowledge_hits` canonicalization)** — DEFERRED per the contract's CONDITIONAL default to a separate "projection hygiene" milestone candidate (human decision 2026-05-25). Confirm the deferral is sound scope-discipline (not a smuggled gap) and recommend `defer-to-projection-hygiene-milestone`.
3. **OQ-S53.1** — `R-bad-case-parallel-session-establishment-flakiness`: S4 adds data points (bad-case cs015/fg5q + shadow cs32s02, all cleared on iso). Recommend keep-open (already priority-bumped).
4. **NEW — shadow empty-form session-create HTTP-400** (cs59s01/cs59s02, deterministic): recommend whether a low-priority R-item (`R-shadow-fixture-empty-form-session-create-400`) is warranted (eval-harness/fixture; upstream of all bot behaviour).
5. **OQ-S53.2** — bot-LLM provider drift (moonshot → deepseek/kimi): confirm observation-only per §5.5.

---

## 7. Output format

Write your verdict to the top of `docs/codex-findings.md` (delete-and-add supersession; the deliver-agent archives the live file to `docs/sprints/sprint-053-codex-review.md` AND `docs/milestones/M5_codex-review.md` after this review). Use the §4.2 4-line header:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph — S4 verdict: AUDIT-before-GATING discipline + §4.1 kernel (two registry/Skill-driven gating helpers, null-safe, defensive-emit default) + §1.7 compliance + NEGATIVE-control no-dropped-signal + hard-fence honor + bad-case + shadow rerun consistency + #4 correctly deferred; AND the milestone-shared confirmation that S1/S2 are §7-exempt observation and S3's already-reviewed surface was not disturbed>
```

Then below: (1) §3 nine-question kernel results (the two gating helpers are load-bearing); (2) §4 hard-fence walk (AUDIT-before-GATING + registry-driven + no-dropped-signal are the headline); (3) §5 bad-case + shadow rerun + empirical-gating consistency; (4) §6 OQ disposition table (5 rows); (5) reproducibility spot-check of the dev's cited numbers (`1183 / 1 / 0 / 2` Java; the `+148/-26` footprint numstat; the audit's 4-GATE / 4-KEEP / 1-OOS routing; zero YAML) per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`; (6) the milestone-shared confirmation (S1/S2 §7-exempt + observation; S3 already passed `sprint-052-codex-review.md`; cumulative range coherent); (7) optional Codex-surfaced new findings (P0-P3 with cite); (8) a 1-2 paragraph judgment on whether S4 ships as one coherent audit-gated Skill-driven convergence (Phase-A-gated, registry-driven, NEGATIVE-control-proven, #4 correctly deferred) — and whether M5 as a whole (S1 display + S2 trace + S3+S4 projection convergence) closes as a coherent observability-coherence milestone with no semantic regression.

## 8. Anti-pre-decision discipline

Per `feedback_constitution_discipline_vs_planning_anticipation.md`: surface findings + evidence; do NOT pre-decide M5-blocking vs projection-hygiene-milestone-deferral routing — deliver-agent + human + Codex decide jointly. The §4.2 header IS your binding verdict on whether S4 + M5 can close PASS. Do NOT edit code; do NOT propose fixes beyond naming the `iteration_governance.md` §3 layer. Do NOT edit sprint archives, the objective archive, or the milestone archive the deliver-agent lands at close.
