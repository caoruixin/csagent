---
title: Milestone M-Auto-6 — Runtime substrate hygiene at intake + DISCOVER surfaces + admin observability + intake/clarification contract + UX/corpus governance
doc_tier: current-runtime
status: current
implementation_status: partial
source_of_truth: this file + docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md (research-agent input; expanded post-ship to c1-c17)
last_reviewed: 2026-06-06
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  Opened 2026-06-05 at M-Auto-5 close (Class A — APPROVE_WITH_NON_BLOCKING_OBSERVATIONS,
  baseline_dir moved to m-auto-5-baseline-20260604-simfixed-stalledfix). M-Auto-5
  established that the eval measurement floor is honest across all four
  columns. M-Auto-6 attacks the BOT-RUNTIME + admin-observability + intake/clarification
  contract + UX/corpus-governance hygiene gaps that prevent the honest measurement
  from converging into a stable autoloop signal.

  Source-of-truth proposal:
  `docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md`
  (Path-2 bad-case-driven, research-agent 2026-06-05; expanded post-ship of
  Sub-sprint A on 2026-06-06 from 9 traces / 4 R-items to **17 traces (c1-c17) /
  8 R-items / 7 OBS-items**). Code anchors verified at HEAD `021a86a` per the
  proposal §4 (R1/R2/R4 surfaces) + at S-Auto-23 commits a873d18..af44903
  (post-ship R-items R2.a#5-ext / R7 surfaced from c14).

  **Cadence revision (human 2026-06-06)** — post-ship Sub-sprint A
  smoke evaluation surfaced 2 new runtime R-items (R2.a#5-ext + R7) and
  expanded R3 to three sub-items (3.a + 3.b + 3.c). New packaging:

  - **Sub-sprint A = R1.a + R2.a + R4.a** (S-Auto-23 / Sprint 078) —
    **DEV-SIDE CLOSED 2026-06-06**. Code shipped at commits
    `a873d18 / 840a5e2 / 247da11 / af44903`; Codex APPROVE_S_AUTO_23 at
    `62b4d7b`; P2 smoke wiring evidence at
    `eval_interactive/results/20260605-105339/` (R1 schema + R1 #2 per-UC
    projection + R2 #3 live-path counter + R2 #4 DISCOVER-only budgets +
    R4 #6/#7 enum/block every turn; anti-误杀 floor preserved). Archived
    contract: `docs/sprints/sprint-078-objective.md`.
    **S-Auto-23 is dev-side closed / smoke-verified / Codex-approved;
    milestone-level outcome evidence is deferred to the M-Auto-6 final
    re-bless.** `baseline_dir` and `docs/current_eval_baseline.md`
    UNCHANGED.
  - **Sub-sprint B = R3.a + R3.b + R3.c** (S-Auto-24 / Sprint 079) —
    CURRENT active contract in `docs/sprint_objective.md`. UI-only admin
    trace observability: 3.a session-list `handling_state` completeness,
    3.b A1-dedup ToolEvent folding with preserved expand-to-audit, **3.c
    NEW** — informational guard rejection badge distinct from blocking
    runtime errors (c2/c10/c17 surface). Merges with audit Cluster B.1 +
    C.2.
  - **Sub-sprint C-1 = R7 + R2.a#5-ext** (S-Auto-25 / Sprint 080) —
    PRIMARY, drafted as planning context in
    `compact/sprint-080-dev-prompt.md`. Intake/clarification runtime
    contract bundle: **R7** new `update_intake_fields` no-op-side-effect
    tool so the LLM can accumulate intake fields across turns without
    triggering the handover validator (c14 surfaced; R7 is the runtime
    enabler for OBS-S6); **R2.a#5-ext** narrow phase-aware re-map
    extension to cover RESOLVE + intake-UC + free-text action (c14
    label-mislabel surface; anti-误杀 #12 spirit preserved — RESOLVE
    non-intake + any-phase tool-call repeats stay
    `turn_budget_exhausted`).
  - **Sub-sprint C-2a = R5 citation contract fix** (S-Auto-26 /
    Sprint 081) — PRIMARY, planned at `compact/sprint-081-dev-prompt.md`.
    **Re-scoped 2026-06-06** after a dev pre-fix audit on the original
    C-2 (R5 + R6 bundle) STOPPED with zero file edits and found that
    the original anchors were wrong on both sides. Re-scoped scope:
    (a) `ResolveArticleTool` returns an additive `display_citation`
    field (`source_url` if non-null/non-blank, else `article_id`
    fallback); (b) `SkillGuardrailDispatcher.handleMustCiteSource`
    (`:340-384`) rewritten from the live literal
    `userMessage.contains(citeToken)` substring check to a structural
    shape predicate (URL-shape OR Salesforce-style article_id-shape
    derived from real corpus data); (c) `resolve_faq_grounded_answer.yaml`
    citation-token wording at `:30 / :31 / ~:80 / :44 cite_token_field`
    points at the new `display_citation` token. Per-sub-sprint Codex
    REQUIRED with explicit focus on the literal→shape semantics shift
    + grounding-floor preservation.
  - **Sub-sprint C-2b = R6 corpus eligibility filter** (S-Auto-27 /
    Sprint 082) — PRIMARY, planned at `compact/sprint-082-dev-prompt.md`.
    Re-scoped 2026-06-06: reuse the **existing** `search_knowledge_eligible`
    data field (already on 218/218 corpus articles; ingested-but-unused
    per dev audit) instead of introducing a parallel `bot_visible`
    mechanism. Flip the field from `true → false` on
    `ka41r000000LIEJAA4` (row ~272) + `ka41r000000LIEEAA4` (row ~291);
    plumb through `KnowledgeIngestionRunner.buildKbArticleFromJson` →
    new `KbArticle.searchKnowledgeEligible` column → V17 Flyway
    migration (V16 is highest existing) → `KnowledgeSearchService`
    eligibility filter → `KnowledgeHit` field propagation →
    `SearchKnowledgeTool` structured INFO log on filter decision.
    Direct resolve via `ResolveArticleTool.byArticleId` stays
    unfiltered (human-CS access preserved). Per-sub-sprint Codex
    REQUIRED.
  - **Sequential cadence (post-rewrite)**: C-2b launches after C-2a
    dev-side close. Independent surfaces, but sequencing keeps
    causal attribution clean for the milestone-shared Codex review.
  - **Milestone-shared §9 real-LLM re-bless** runs AFTER A + B + C-1 +
    C-2a + C-2b all land — single milestone-level evidence run, not
    per-sub-sprint.

  **Single bundled C explicitly rejected (2026-06-06)** by human, on
  the basis that (1) R7 + R2.a#5-ext form one coherent
  intake/clarification runtime contract that benefits from causal
  isolation; (2) R7 is the OBS-S6 enabler — high priority, should not be
  blocked by R5/R6 UX work; (3) small sub-sprints aid causal attribution
  + anti-误杀 review.

  **C-2 split into C-2a + C-2b (2026-06-06 re-scope, post dev STOP)**:
  the original C-2 prompt rested on wrong file anchors caught by the
  dev pre-fix audit. R5's `must_cite_source` lives in the FORBIDDEN
  `SkillGuardrailDispatcher.handleMustCiteSource` and does literal
  substring matching (not shape validation); R6's `bot_visible`
  cannot reach the filter point because ingestion silently drops
  unknown JSON keys, and the corpus already carries
  `search_knowledge_eligible` for exactly this purpose. C-2a re-scopes
  R5 as a citation-guardrail contract change with widened fence; C-2b
  re-scopes R6 to reuse `search_knowledge_eligible` with full
  ingestion+entity+migration+service plumbing. Both retain `§7
  REQUIRED + per-sub-sprint Codex REQUIRED`.

  Audit Cluster B.2 (`primary_uc` vs `active_use_case` authority) +
  Cluster C.1 (cs59s 400) + `R-aggregate-retains-per-attempt-composite-l2`
  remain queued for S-Auto-27+ (post-M-Auto-6) pending the milestone
  re-bless evidence. M-Auto-4 S-Auto-18 stays deferred to M-Auto-7+.

  **Causal model (proposal §5)**: the honest measurement floor from
  M-Auto-5 still surfaces ~draw-noise on specific UC surfaces. Post
  Sub-sprint A:
  - **Intake-UC handover** (UC-G / UC-H / UC-I / UC-J / UC-K) first-call
    rejection-and-retry → ELIMINATED by R1.a schema declaration (live
    in build per c11/c15/c16 first-call success in smoke).
  - **DISCOVER clarification depth** → counter now lives on the live
    path (R2.a #3); budget projection emitted DISCOVER-only (R2.a #4);
    phase-aware re-map labels DISCOVER + free-text correctly (R2.a #5).
  - **UC-A entity-premise signal** → `customer_context_status` +
    `ad_reference` surfaced every turn (R4.a); the c7 semantic loop
    closure waits on OBS-S1 (autoloop after milestone close).
  - **Residual c14 RESOLVE-intake clarification mislabel** + **c14
    intake partial-stash gap** → R2.a#5-ext + R7 in Sub-sprint C-1.
  - **c13 citation source_id-vs-URL** → R5 citation contract fix in
    Sub-sprint C-2a (additive `display_citation` +
    `handleMustCiteSource` literal→shape rewrite + skill yaml
    wording).
  - **c7/c12/c17 `(temp)` corpus surface** → R6 corpus eligibility
    filter in Sub-sprint C-2b (reuse existing
    `search_knowledge_eligible` field; plumb through ingestion +
    entity + V17 migration + service + tool).
  - **c2/c10/c17 admin guard-vs-error confusion** → R3.c in Sub-sprint B.
  - **c7 admin missing-case + c8 dedup UI** → R3.a + R3.b in
    Sub-sprint B.

  All remaining R-items are LIVE-path wiring or UI/observability
  improvements. None changes semantic surfaces (yaml / prompt / CaseSpec
  / judge calibration). The 7 semantic OBS-items (OBS-S1..S7) are
  explicitly deferred to autoloop AFTER M-Auto-6 close; OBS-S6 is
  additionally gated on R7 ship.

  M-Auto-4's S-Auto-18 (escalation-family tier reshape) stays deferred
  behind M-Auto-6 (to M-Auto-7+).
---

# Milestone M-Auto-6 — Runtime substrate hygiene + admin observability + intake/clarification contract + UX/corpus governance

## 1. Milestone class

**Class:** runtime substrate-hygiene + UI/observability + intake contract
+ UX/corpus governance milestone (Tier-1 mechanical wiring +
prompt_projection contract completion + UI-only display additions + new
no-side-effect tool + data field driven retrieval filter + citation
contract literal→shape semantics fix). NOT a semantic milestone — no
bot prompt procedure rewrite, no UC routing, no escalation posture
decision, no judge calibration, no CaseSpec rubric edit. R5 (C-2a)
rewrites `SkillGuardrailDispatcher.handleMustCiteSource` from a literal
substring check to a structural URL-or-article_id shape predicate and
updates the citation-token wording in `resolve_faq_grounded_answer.yaml`
to point at the new `display_citation` field; the rewrite **preserves
the grounding floor** (empty / null / plain-English STILL reject) and
the wording change is minimum-edit (citation-token references only,
procedure / role / objective untouched).

**§7 stanza requirement:** REQUIRED at the milestone level + at each
semantic-touching sub-sprint. Layer matrix:

| Sub-sprint | Layer mix | §7 stanza required? | Rationale |
|---|---|---|---|
| A (R1.a + R2.a + R4.a) | `prompt_projection` + `infra` + `skill_state` | ✅ REQUIRED | LLM-facing projection surface touched (R1 schema + R4 enum) — semantic-touching per §7. Shipped at S-Auto-23 close. |
| B (R3.a + R3.b + R3.c) | `infra` (observability) | ❌ EXEMPT | UI-only display; zero semantic surface; ≤ ~10 LOC diagnostic logging is non-behavioural. |
| C-1 (R7 + R2.a#5-ext) | `skill_state` + `infra` + `prompt_projection` (R7 tool schema) | ✅ REQUIRED | R7 adds a new tool name (LLM-facing projection surface); R2.a#5-ext extends the existing phase-aware re-map (control-plane labeling). Semantic-touching per §7. Shipped at S-Auto-25 close (`APPROVE_S_AUTO_25 / blocking_count=0`). |
| C-2a (R5 citation contract fix) | `infra` (ResolveArticleTool result + handleMustCiteSource semantics rewrite) + `prompt_projection` (skill yaml citation wording at `:30/:31/~:80/:44`) | ✅ REQUIRED | R5 #2 rewrites the LLM-facing citation guardrail from literal substring to structural URL/article_id shape predicate; R5 #3 changes the LLM-facing skill wording to point at the new `display_citation` field. Semantic-touching per §7. Re-scoped 2026-06-06 from the original "1-line `cite_token_field` config swap" after the dev pre-fix audit caught the wrong-anchor STOP. |
| C-2b (R6 corpus eligibility filter) | `infra` (data + entity + V17 migration + ingestion + service filter + tool log) | ✅ REQUIRED | R6 changes the LLM-facing search retrieval surface via a data-field eligibility filter. Reuses existing `search_knowledge_eligible` (ingested-but-unused on 218/218 articles per dev audit) — NOT a new parallel `bot_visible` mechanism. Direct resolve unfiltered (human-CS access preserved). Semantic-touching per §7. Re-scoped 2026-06-06. |

**Layer breakdown** (per `iteration_governance.md` §3 + the proposal §2):

| R-item | Layer | Sub-sprint | Status | Reason |
|---|---|---|---|---|
| R1.a — `request_handover` intake-fields schema declaration | `prompt_projection` | A | ✅ SHIPPED | Projected schema lacked the field validator expects; required-fields source already exists in `IntakeFieldsRegistry`. Zero new semantic surface; zero new per-UC matrix (registry iterated as-is). |
| R2.a — DISCOVER clarification counter live-path wiring + budget projection + phase-aware mapping | `infra` + `skill_state` | A | ✅ SHIPPED | Dead-code counter wired on live path; DISCOVER-only budget projection; `max-repeated-same-action` re-maps to `clarification_budget_exhausted` only when phase=DISCOVER + free-text action (anti-误杀 #12). |
| R4.a — entity-premise projection slot (`customer_context_status` enum + `ad_reference` block) | `prompt_projection` | A | ✅ SHIPPED (enabler; semantic loop waits on OBS-S1) | Projects already-known runtime facts (form_context state, lookup tool result) as structured boolean/enum; zero content matching; priority order `missing_email > missing_ad_id > lookup_failed > lookup_skipped > loaded`; `missing` ≠ `skipped`. |
| R3.a — admin trace observability: session-list handling_state completeness | `infra` (observability) | B | ⏳ PENDING | Renders existing `BotSession.HandlingState` values; addresses c7 admin-missing-case symptom. |
| R3.b — `TraceViewer` dedup ToolEvent UI folding (expand preserves audit) | `infra` (observability) | B | ⏳ PENDING | Renders existing `ToolEvent.deduplicated` flag; addresses c8 0ms-event readability. |
| R3.c — informational guard rejection badge distinct from blocking error | `infra` (observability) | B | ⏳ PENDING (NEW post-ship) | UI display distinction over guard rejection events; addresses c2/c10/c17 record_outcome confusion. SAFER default for unrecognised codes = blocking. |
| R7 — `update_intake_fields` no-side-effect tool (intake partial-stash mechanism) | `skill_state` + `infra` + `prompt_projection` (new tool schema) | C-1 | ⏳ PENDING (NEW post-ship from c14) | Lets LLM accumulate intake fields across turns without triggering the handover validator; runtime enabler for OBS-S6. |
| R2.a#5-ext — phase-aware re-map narrow extension to RESOLVE + intake-UC + free-text | `infra` | C-1 | ⏳ PENDING (NEW post-ship from c14) | Surface c14 RESOLVE-phase intake clarification mislabel; preserves anti-误杀 #12 spirit (RESOLVE non-intake + any tool-call repeat stay `turn_budget_exhausted`). |
| R5 — citation contract fix: additive `display_citation` on ResolveArticleTool + `SkillGuardrailDispatcher.handleMustCiteSource` literal→shape rewrite + skill yaml citation wording at `:30/:31/~:80/:44` | `infra` + `prompt_projection` | C-2a | ⏳ PENDING (re-scoped 2026-06-06) | c13 surface: bot cites `article_id` even when article has `source_url`. Re-scoped from "1-line `cite_token_field` swap" after dev anchor audit caught the literal-substring guardrail at the forbidden `SkillGuardrailDispatcher.java:340-384` + the actual LLM-steering wording at `resolve_faq_grounded_answer.yaml:30-31`. Grounding floor preserved (empty / null / plain-English STILL reject); accept criteria widens from "literal field-name substring" to "URL-shape OR Salesforce article_id-shape". |
| R6 — corpus eligibility filter: reuse existing `search_knowledge_eligible` data field + full plumbing (V17 migration + ingestion + entity + search service + hit + tool) | `infra` (data + entity + migration + service + tool) | C-2b | ⏳ PENDING (re-scoped 2026-06-06) | c7/c12/c17 surface: `(temp)` template articles (`ka41r000000LIEJAA4` + `ka41r000000LIEEAA4`) reach LLM unwrapped. Re-scoped from "new `bot_visible` data field" after dev audit found the existing `search_knowledge_eligible` field on 218/218 articles (ingested-but-unused). Data field driven, NOT title-keyword matched. Article retained for human CS use; direct resolve via `ResolveArticleTool.byArticleId` unfiltered. |

## 2. Goal

Make the post-M-Auto-5 honest measurement floor converge to a STABLE
autoloop signal at the **intake-UC handover + DISCOVER clarification +
UC-A entity-premise + intake partial-stash + admin observability +
citation + corpus surface** by:

1. Eliminating the LIVE-path runtime wiring defects (R1.a + R2.a + R4.a
   shipped at Sub-sprint A; R7 + R2.a#5-ext to ship at Sub-sprint C-1)
   that inject draw-noise independent of bot semantic competence.
2. Surfacing structured premise signals (R4.a; depends on OBS-S1
   autoloop after milestone close) so the LLM has the observable state
   it needs to decide whether to challenge unverified premises.
3. Cleaning the admin trace observability surface (R3.a + R3.b + R3.c
   at Sub-sprint B) so manual bad-case triage is not blocked by display
   artefacts.
4. Improving citation UX + filtering known-bad corpus content (R5 + R6
   at Sub-sprint C-2) so the LLM is not surfacing `(temp)` template
   articles unwrapped, and human-facing answers cite URLs rather than
   internal source IDs when both are available.

**Why this milestone exists**: M-Auto-5 closed the eval-honesty floor
(0/414 vacuous-pass + 10 F→P paired-evidence + near-coinflip ELIMINATED).
The post-close baseline `m-auto-5-baseline-20260604-simfixed-stalledfix`
shows 6/12 `reducible-flaky` cases in `bad_cases` — material residual
noise that prevents autoloop semantic optimization from making clean
fitness signals on intake + DISCOVER skill surfaces. Sub-sprint A landed
R1.a + R2.a + R4.a (proposal §5.3 primary residual source); Sub-sprint
B + C-1 + C-2 address the post-ship surfaces surfaced by c10-c17
trace evaluation. The falsifiable prediction (§5 below) tests the
attribution at the MILESTONE-shared re-bless after all four sub-sprints
land — NOT per-sub-sprint.

NOT a goal: bot-capability optimization on any UC surface. Semantic
optimization is explicitly deferred to autoloop AFTER M-Auto-6:
- OBS-S1 (UC-A / UC-H / UC-J verify-entity-context procedure step) —
  blocked by R4.a ship (already done).
- OBS-S2 (DISCOVER ad_status disambiguation cue too conservative).
- OBS-S3 (FAQ fabrication at c8 turn 6 / `D-faq-grounded-resolve-bypass`).
- OBS-S4 (record_outcome timing — n=3 across c2/c10/c17; guard is
  CORRECT, LLM timing question).
- OBS-S5 (cross-turn rank-1 first refinement — M-Auto-3 §11 floor).
- OBS-S6 (intake procedures too literal — gated on R7 ship at C-1).
- OBS-S7 (escalate-without-summary at c10).

## 3. Sub-sprint sequence

Sequence updated 2026-06-06 after S-Auto-25 dev-side close + the
post-C-1 C-2 re-scope (R5 + R6 originally bundled as Sub-sprint C-2;
split into C-2a R5-only + C-2b R6-only after the dev pre-fix audit on
C-2 STOPPED with wrong anchors caught — see §0 notes block). Sequential
cadence: A (done) → B (done) → C-1 (done) → C-2a (current planned
active) → C-2b (next). No re-bless between sub-sprints — outcome
evidence runs once at milestone close.

### Sub-sprint A — S-Auto-23 / Sprint 078 — R1.a + R2.a + R4.a — **DEV-SIDE CLOSED 2026-06-06**

R1.a (per proposal §6.1) + R2.a (per proposal §6.2) + R4.a (per
proposal §6.4). All three share the `ContextProjectionBuilder.java`
edit context.

> **S-Auto-23 is dev-side closed / smoke-verified / Codex-approved;
> milestone-level outcome evidence is deferred to the M-Auto-6 final
> re-bless.**

Status:
- Code shipped: commits `a873d18 / 840a5e2 / 247da11 / af44903`.
- Java baseline preserved: `1244 / 1 / 0 / 2` (sole failure = inherited
  OQ-S41.5).
- Codex per-sub-sprint review: `APPROVE_S_AUTO_23 / blocking_count=0`
  at commit `62b4d7b` (`docs/codex-findings.md`).
- P2 smoke wiring evidence: `eval_interactive/results/20260605-105339/`
  (11 cases, n=1, parallel=1; R1 schema + R1 #2 per-UC projection + R2
  #3 live-path counter + R2 #4 DISCOVER-only budgets + R4 #6/#7 enum
  every turn; anti-误杀 floor preserved).
- `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md` UNCHANGED.

Archived contract: `docs/sprints/sprint-078-objective.md`.

### Sub-sprint B — S-Auto-24 / Sprint 079 — R3.a + R3.b + R3.c admin trace observability — **DEV-SIDE CLOSED 2026-06-06**

R3.a + R3.b + R3.c (per proposal §6.3, post-ship expanded). UI-only
admin trace observability:
- R3.a: `SessionList.tsx` StatusBadge covers the COMPLETE
  `BotSession.HandlingState` enum (addresses c7 admin-missing-case).
- R3.b: `TraceViewer.tsx` folds A1-dedup'd ToolEvent records by default
  with click-to-expand audit preservation (addresses c8 0ms event
  readability).
- R3.c: `TraceViewer.tsx` informational guard rejection badge distinct
  from blocking runtime errors; SAFER default for unrecognised codes =
  blocking (addresses c2/c10/c17 record_outcome confusion).
- Plus terminal handling_state header + diagnostic logging for
  handling_state transitions.

> **S-Auto-24 is dev-side closed / visual-verified / §7-EXEMPT (no
> per-sub-sprint Codex required); milestone-level outcome evidence is
> deferred to the M-Auto-6 final re-bless.**

Status:
- Code shipped: commits `a26ec88..505aca3`.
- UI vitest: `10 passed / 0 failed / 0 skipped`; UI `tsc -b` exit 0;
  `vite build` SUCCESS.
- Java baseline preserved: `1297 / 1 / 0 / 2` (post-S-Auto-23 baseline;
  sole failure = inherited OQ-S41.5; no new Java tests added — #5
  diagnostic logging non-behavioural).
- Visual verification by deliver-agent on session `e82c8da3`
  confirmed R3.a / R3.b / R3.c / terminal-state header / #5 logging.
- `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md` UNCHANGED.

Archived contract: `docs/sprints/sprint-079-objective.md`.

### Sub-sprint C-1 — S-Auto-25 / Sprint 080 — R7 + R2.a#5-ext — intake/clarification runtime contract — **DEV-SIDE CLOSED 2026-06-06**

R7 + R2.a#5-ext (per proposal §4.7 + §4.8 + §6.7). Intake/clarification
runtime contract bundle:
- R7: new `update_intake_fields(fields={...})` tool that writes
  `session.intakeFields` via the existing `persistInlineIntakeFields`
  merge path but does NOT trigger the handover validator. LLM can
  accumulate partial intake state across turns without "send-or-stall"
  dilemma. Runtime enabler for OBS-S6 (LLM inferring intake fields from
  free text — gated until R7 ships).
- R2.a#5-ext: narrow extension of the phase-aware re-map
  (`ControlKernel.mapBudgetToEscalationReason`) to cover RESOLVE +
  `IntakeFieldsRegistry.isIntakeUseCase(activeUseCase)` + free-text
  action. Anti-误杀 #12 spirit preserved: RESOLVE non-intake UC + any
  phase + any tool-call repeat remain `turn_budget_exhausted`.

> **S-Auto-25 is dev-side closed / Codex-approved / capability-wiring
> fence-waiver accepted; milestone-level outcome evidence is deferred
> to the M-Auto-6 final re-bless.**

Status:
- Code shipped: commits `be1e733..c031786` (intended cumulative range
  `be1e733^..c031786` — 4 commits).
- Java baseline `1327 / 1 / 0 / 2` (+30 net tests vs the 1297/1/0/2
  launch baseline; sole failure = inherited OQ-S41.5, verified
  pre-existing by `git stash`).
- Codex per-sub-sprint review: `APPROVE_S_AUTO_25 / blocking_count=0`.
  §1 / §2 / §3 all PASS. 5 non-blocking observations recorded.
- Capability-wiring Option-A fence-waiver ACCEPTED (deliver-agent
  2026-06-06): `tool-policy.yaml` +6 lines (AGENT_VISIBLE intake-UC
  entry); `resolve_intake_collect_and_handover.yaml` +4 lines
  (`tools_required` add); `SkillLoader.VALID_TOOL_NAMES` +1 string +
  1 comment. ZERO procedure / objective / grounding / escalation /
  wording / enum / CaseSpec edit.
- `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md`
  UNCHANGED.

Archived contract: `docs/sprints/sprint-080-objective.md`.

### Sub-sprint C-2a — S-Auto-26 / Sprint 081 — R5 citation contract fix — **CURRENT ACTIVE (re-scoped 2026-06-06)**

R5 citation contract fix (per proposal §4.5; re-scoped after the dev
pre-fix audit on the original C-2 STOPPED with wrong anchors caught).

**Why re-scoped**: the original C-2 prompt assumed the
`must_cite_source` guardrail lived at
`guardrails/MustCiteSource.java` (no such file) + did URL/source_id
structural shape validation (it does not — it does literal
`userMessage.contains(citeToken)` substring matching). The dev STOP
caught this BEFORE any code was written. The human re-scope
direction (2026-06-06): re-scope, stay in M-Auto-6, split C-2 into
C-2a (R5) + C-2b (R6), use widened fences with reclassified §7,
DO NOT do safe-partial.

Scope:
- R5 #1: `ResolveArticleTool` returns an additive `display_citation`
  field on the result body (`source_url` if non-null/non-blank, else
  `article_id` fallback).
- R5 #2: rewrite `SkillGuardrailDispatcher.handleMustCiteSource`
  (`:340-384`) from the literal `userMessage.contains(citeToken)`
  substring check to a structural shape predicate: PASS iff the user
  message contains a URL-shape token (`http://`/`https://` prefix) OR
  an article_id-shape token (Salesforce-style regex derived from real
  corpus data). Empty / null / plain-English STILL reject —
  grounding floor preserved.
- R5 #3: update `resolve_faq_grounded_answer.yaml` citation-token
  wording at `:30 procedure / :31 grounding_instruction / ~:80 cite
  phrasing / :44 cite_token_field` to point at the new
  `display_citation` field. Minimum-edit diff; goal / role /
  objective / search-must-precede / faq_miss escalation rule
  byte-untouched.

Dev prompt: `compact/sprint-081-dev-prompt.md` (rewritten 2026-06-06
to encode the corrected anchors + reclassified §7). Active sprint
contract: `docs/sprint_objective.md` (after C-1 close + this
re-scope commit). Per-sub-sprint Codex review REQUIRED with explicit
focus on the literal→shape semantics shift + grounding-floor
preservation evidence.

### Sub-sprint C-2b — S-Auto-27 / Sprint 082 — R6 corpus eligibility filter (PLANNING CONTEXT; launches after C-2a dev-side close)

R6 corpus eligibility filter (per proposal §4.6; re-scoped after the
dev pre-fix audit on the original C-2 STOPPED with wrong anchors
caught). Re-scoped to reuse the existing `search_knowledge_eligible`
field that already lives on 218/218 corpus articles (ingested-but-unused
per the dev anchor audit), instead of introducing a parallel new
`bot_visible` mechanism per the human re-scope decision (2026-06-06).

Scope:
- R6 #1: flip `search_knowledge_eligible: true → false` on
  `ka41r000000LIEJAA4` (row ~272) + `ka41r000000LIEEAA4` (row ~291)
  in `data/knowledge/knowledge_base_articles.json`. ONLY these 2
  articles; ONLY this field.
- R6 #2-#7: plumb the field through:
  - `KbArticle.java` — new `searchKnowledgeEligible` column
    (boolean, default `true`).
  - `V17__add_kb_search_knowledge_eligible.sql` — new Flyway
    migration (V16 is highest existing) with `NOT NULL DEFAULT TRUE`.
  - `KnowledgeIngestionRunner.buildKbArticleFromJson` — parse the
    field; default `true` if absent.
  - `KnowledgeSearchService` — filter candidates by
    `searchKnowledgeEligible != false` at the candidates → hits
    assembly step.
  - `KnowledgeHit` — propagate the field (or skip per pre-fix audit
    design choice).
  - `SearchKnowledgeTool` — structured backend INFO log on filter
    decision (article_id + filter_reason).
- R6 #8: direct resolve invariant — `ResolveArticleTool.byArticleId`
  STILL returns the 2 flagged articles (filter is at search surface
  ONLY; human-CS access preserved).

Forbidden: any new parallel governance field (no `bot_visible`); any
hardcoded article_id in Java; any title-keyword filter; any corpus
article deletion.

Dev prompt: `compact/sprint-082-dev-prompt.md` (planning context;
becomes active sprint contract when C-2a closes). Per-sub-sprint
Codex review REQUIRED (R6 changes corpus retrieval surface visible to
the LLM via data field + service filter).

**Sequential cadence**: C-2b launches AFTER C-2a dev-side close +
Codex approval. Independent code surfaces but sequenced for clean
causal attribution.

### Milestone close — milestone-shared §9 real-LLM re-bless

After A + B + C-1 + C-2a + C-2b all land, deliver-agent + human launch ONE
milestone-shared real-LLM re-bless (`autoloop/scripts/rebless_baseline.py
--n 9 --out-dir eval_interactive/results/m-auto-6-baseline-shared-YYYYMMDD/`).
Paired evidence against `m-auto-5-baseline-20260604-simfixed-stalledfix`.

Routes:

- **(a) Prediction holds (§5)**: M-Auto-6 closes (Class A or A-with-NBO);
  `baseline_dir` + `docs/current_eval_baseline.md` flip; S-Auto-28+
  Cluster B.2 / C.1 / `R-aggregate-retains-per-attempt-composite-l2`
  opens in M-Auto-7 planning OR autoloop semantic sub-sprint (OBS-S1 /
  OBS-S2 / OBS-S6 / OBS-S7) opens.
- **(b) Prediction fails**: pause autoloop semantic opens; open a
  re-diagnosis sub-sprint to identify the unaddressed residual noise
  source. Do NOT add semantic hardcodes to "force" stability; do NOT
  widen R1/R2/R4/R5/R6/R7 with content heuristics.
- **(c) Anti-误杀 violation**: immediate revert + diagnose; potential
  downgrade to Class C.

Decision is human-gated.

### S-Auto-28+ — TBD (post-M-Auto-6)

Per the M-Auto-6 audit-cluster routing + the M-Auto-5 Codex
non-blocking observation #3 R-item. Candidates:
- Cluster B.2 (`primary_uc` vs `active_use_case` authority — needs
  research-agent decision sub-sprint).
- Cluster C.1 (cs59s session_create 400 — research-first short).
- `R-aggregate-retains-per-attempt-composite-l2` (from M-Auto-5 Codex
  observation #3).
- Autoloop semantic sub-sprint targeting OBS-S1 / OBS-S2 / OBS-S6
  (OBS-S6 only after R7 ships) / OBS-S7.

May be deferred to M-Auto-7.

## 4. Non-goals (explicit)

- **No semantic procedure edits**. No bot prompt rewrite, no skill yaml
  procedure-step / wording rewrite, no UC routing cue, no escalation
  posture decision, no judge calibration, no CaseSpec rubric. OBS-S1
  (UC-A / UC-H / UC-J verify-entity procedure step), OBS-S2 (DISCOVER
  disambiguation), OBS-S3 (FAQ fidelity / c8 turn 6 fabrication),
  OBS-S6 (intake too literal — additionally gated on R7), OBS-S7
  (escalate-without-summary) are ALL deferred to autoloop AFTER M-Auto-6
  close.
  - **Exception**: R5 (Sub-sprint C-2a) updates the citation-token
    wording in `resolve_faq_grounded_answer.yaml` at `:30 procedure
    + :31 grounding_instruction + ~:80 cite phrasing + :44
    cite_token_field` to point at the new `display_citation` field
    returned by `ResolveArticleTool`. This is a **citation-token
    reference change**, minimum-edit diff (goal / role / objective /
    search-must-precede / faq_miss escalation rule byte-untouched).
    The contemporaneous `SkillGuardrailDispatcher.handleMustCiteSource`
    rewrite at `:340-384` is a guardrail semantics fix (literal
    substring → structural URL/article_id shape predicate) — a
    grounding-floor false-negative correction, not a procedure edit.
    Both permitted per the milestone class §1.
- **No `IntakeFieldsRegistry.java:53-67` field-definition edits.** R1.a
  projects the existing required-fields list as-is; if any UC's
  required-fields set is itself wrong, that's a separate `eval_spec`
  question outside M-Auto-6.
- **No new `escalation_reason` enum value.** Per `D-new-escalation-reason-enum`
  deferred (action_bank §4) + M-Auto-3 §4 verdict. R2.a + R2.a#5-ext
  re-map `max-repeated-same-action` to the existing
  `clarification_budget_exhausted` value, no enum widening.
- **No identical-clarification cross-turn semantic dedup**. Cardinality
  budget only. M-Auto-3 §11 anti-误杀 rank-1 floor preserved (per
  `R-runtime-paraphrase-storm-search-knowledge` close + proposal §5.2).
- **No `SkillGuardrailDispatcher` reject-logic edits** EXCEPT the
  scoped `handleMustCiteSource` rewrite at `:340-384` in C-2a.
  Validator remains the last line of defence on every other handler;
  R1.a only adds the LLM-facing contract; R7 adds a NEW no-side-effect
  tool that does NOT bypass the validator on the handover path. The
  C-2a `handleMustCiteSource` rewrite **preserves** the grounding-floor
  contract (empty / null / plain-English STILL reject) and widens the
  accept criteria from "literal field-name substring" to "URL-shape OR
  Salesforce-style article_id-shape" — a fix to a false-negative in the
  current literal check, not a weakening.
- **No `record_outcome` premature guard edits** (OBS-S4 by-design).
  R3.c distinguishes informational guard rejections from blocking
  errors at the UI display surface ONLY; the guard at
  `ResolveDispositionEvaluator.java:160-186` is unchanged.
- **No corpus article deletion.** R6 (C-2b) flips the existing
  `search_knowledge_eligible` data field from `true → false` on the
  2 `(temp)` template articles; the articles remain in corpus + DB +
  retrievable via `ResolveArticleTool.byArticleId` for human CS use;
  only the LLM-facing search-knowledge surface is filtered.
- **`must_cite_source` guardrail logic IS rewritten in C-2a** —
  literal `userMessage.contains(citeToken)` substring check →
  structural URL-shape OR article_id-shape predicate. The grounding
  floor (require a cite token) is **preserved**; the rewrite fixes a
  false-negative (the OLD code rejected URL-only replies that didn't
  also literally contain the string `"source_id"`). Empty / null /
  plain-English STILL reject. Article_id-shape regex derived from
  real corpus data (`data/knowledge/knowledge_base_articles.json`),
  NOT invented. No new parallel guardrail or grounding rule.
- **No simulator / eval-framework / scoring SHA / autoloop 5-file set
  edits.** Fence-#13 SHA respected.
- **No M-Auto-4 S-Auto-18 (escalation-family tier reshape) work.**
  Deferred to M-Auto-7+.
- **No per-sub-sprint outcome-evidence re-bless.** A's smoke is wiring
  evidence; B is UI-only (visual verification only); C-1 / C-2 wait for
  the milestone-shared re-bless. `baseline_dir` and
  `docs/current_eval_baseline.md` do not flip until the milestone-shared
  re-bless lands at close.

## 5. Milestone acceptance bar

**Falsifiable hypothesis** (proposal §5.3 + post-ship c10-c17 expansion):
R1+R2+R4 (shipped at A) + R7 + R2.a#5-ext + R5 + R6 + R3.* are
collectively the dominant residual noise + UX + observability gap on
intake / DISCOVER / UC-A entity-premise / citation / corpus / admin
surfaces after M-Auto-5. If true, after all four sub-sprints ship and a
**single milestone-shared real-LLM re-bless** runs on the corrected
runtime, the following should observably move:

| Signal | Pre-M-Auto-6 (simfixed-stalledfix) | Post-milestone prediction | Falsifier |
|---|---|---|---|
| `bad_cases` `reducible-flaky` count | 6/12 | ≤ 2/12 | Stays > 2/12 and remaining flaky cases hit intake + DISCOVER + UC-A entity surfaces → not the residual noise source; re-diagnose. |
| anchor uc_f_billing pass_rate | 0.89 (reducible-flaky) | Toward stable ~1.00 | Drifts ≤ 0.78 → R1 was not the residual noise here. |
| anchor uc_fp_removed pass_rate | 0.89 (reducible-flaky) | Toward stable ~1.00 | Same. |
| Intake-UC first-call rejection rate (UC-G / UC-H / UC-I / UC-J / UC-K) | High pre-A (c1/c5/c6 pattern) | 0 post-A | A smoke evidence: c11/c15/c16 first-call success observed; milestone re-bless confirms at corpus scale. |
| DISCOVER clarification cap-hit count | 0 (counter never incremented) | > 0 on c3/c9-like cases | A smoke evidence: counter increments live (uc_k_tech / cs012 / iwzx). |
| `clarification_budget_exhausted` escalation_reason occurrences | 0 (mapping fall-through stamps `turn_budget_exhausted`) | > 0 on DISCOVER free-text repeats AND C-1-after RESOLVE-intake free-text repeats | Stays 0 → R2.a #5 or R2.a#5-ext mapping wiring did not take. |
| `turn_budget_exhausted` occurrences attributable to RESOLVE-intake clarification | n=1 known (c14) + characterization at milestone re-bless | 0 post-R2.a#5-ext | R2.a#5-ext mapping check. |
| c14-class intake partial-stash behaviour (UC-J `report_target` / `report_type` / `description` accumulated across turns without first-send rejection) | Cannot accumulate without handover triggering validator (c14 pattern) | LLM uses `update_intake_fields` to accumulate; handover only when all required fields collected | R7 wiring check. Mocked tests in C-1 suite cover schema + tool dispatch; real-LLM behaviour depends on LLM adoption — wiring evidence is the close gate. |
| Citation token in user-facing replies | `(Source: ka4P200000003sLIAQ)`-style source_id | `(Source: <canonical_url>)` when article has URL; `source_id` fallback for the 38 URL-less articles | R5 wiring check. |
| `(temp)` template article occurrences in `search_knowledge` results visible to LLM | 2 known articles surface in c7/c12/c17 | 0 post-R6 | R6 retrieval filter check; corpus content itself unchanged. |
| Admin trace UI: c7 admin missing case + c8 dedup confusion + c2/c10/c17 informational-vs-blocking | All present pre-B | Resolved at UI surface via R3.a + R3.b + R3.c; underlying trace data unchanged | Visual verification at B close; no eval impact. |
| Anti-误杀 (persistent high-risk cases) | 0.000 stable on anchor uc_g_gdpr / uc_h_appeal / uc_i_payment / uc_j_safety + shadow cs38s* | UNCHANGED at 0.000 stable | Any rise (an artifact mis-passes) → reject the change as masking. |

**Hard close gates** (unchanged from `iteration_governance.md` §5.5 +
`process/badcase-lifecycle.md` §5.6):

- Codex §4.1 nine-question kernel pass at milestone close (per-sub-sprint
  Codex reviews REQUIRED for A + C-1 + C-2 — semantic-touching; OPTIONAL
  for B — UI-only; the milestone-shared Codex review at close is the
  formal close gate).
- Java test suite no new regression vs the documented inherited baseline
  (`1244 / 1 / 0 / 2` post-S-Auto-23). Each sub-sprint may add new
  tests; total failure count must not flip.
- Safety floor unchanged (anti-误杀 listed above).
- Grounding floor unchanged (R5 fallback to `source_id` preserves
  grounding contract).
- Curated bad-case suite manual review (primary gate) at milestone
  close — paired-evidence review against
  `m-auto-5-baseline-20260604-simfixed-stalledfix`.

**Re-bless cadence (revised 2026-06-06)**:
- Sub-sprint A: NO outcome-evidence re-bless (P2 smoke wiring evidence
  only; outcome deferred to milestone close).
- Sub-sprint B: NO re-bless (UI-only; visual verification at sub-sprint
  close).
- Sub-sprint C-1 + C-2: NO per-sub-sprint re-bless (mocked-LLM tests +
  per-sub-sprint Codex are the gates; outcome deferred to milestone
  close).
- Milestone close: ONE real-LLM re-bless (`--n 9` per the
  S-Auto-21/22 / M-Auto-5 close protocol; multi-suite) against
  `eval_interactive/results/m-auto-6-baseline-shared-YYYYMMDD/`.
  `baseline_dir` flip + `docs/current_eval_baseline.md` flip happen at
  close, paired-evidence-reviewed.

## 6. Hard fences (milestone-level)

1. NO bot semantic / prompt rewrite / UC-hypothesis / escalation
   posture / skill yaml procedure-step / CaseSpec edit anywhere in
   M-Auto-6. **Exception**: R5 (C-2a) updates the citation-token
   wording at `resolve_faq_grounded_answer.yaml:30 procedure + :31
   grounding_instruction + ~:80 cite phrasing + :44 cite_token_field`
   to point at the new `display_citation` field — minimum-edit diff;
   goal / role / objective / search-must-precede / faq_miss escalation
   rule byte-untouched. Permitted per §1 milestone class.
2. NO `IntakeFieldsRegistry` content changes; R1.a projects existing
   contract as-is.
3. NO new `escalation_reason` enum values; R2.a + R2.a#5-ext re-map
   to existing `clarification_budget_exhausted`.
4. NO identical-clarification cross-turn content/semantic dedup; R2.a
   is cardinality-only.
5. NO `SkillGuardrailDispatcher` reject-logic edits EXCEPT C-2a's
   `handleMustCiteSource` rewrite at `:340-384` (literal substring
   → structural URL/article_id shape; grounding floor preserved).
   R1.a only adds LLM-facing schema slot; R7 adds a NEW tool that
   does NOT bypass the validator on the handover path (handover
   validator semantics preserved).
6. NO `ResolveDispositionEvaluator` reject-logic edits. R3.c is a UI
   display distinction over CORRECT §1.4 guard rejections, not a guard
   behaviour change.
7. NO corpus article deletion. R6 (C-2b) flips the existing
   `search_knowledge_eligible` data field on 2 `(temp)` articles;
   articles retained in corpus + DB for human CS use; direct resolve
   via `ResolveArticleTool.byArticleId` unfiltered.
8. NO `must_cite_source` guardrail logic change OTHER than the C-2a
   `handleMustCiteSource` rewrite (literal substring → structural
   URL/article_id shape; grounding floor preserved). Old traces +
   URL-less articles preserved via the article_id-shape predicate.
   The article_id-shape regex MUST be derived from real corpus data
   (NOT invented).
9. NO simulator / eval-framework / scoring SHA / autoloop 5-file set
   edits. Fence-#13 SHA respected.
10. NO M-Auto-4 S-Auto-18 work (deferred to M-Auto-7+).
11. NO autoloop semantic optimization sub-sprint until M-Auto-6 close
    + milestone re-bless evidence reviewed. OBS-S6 additionally gated
    on R7 ship (which lands in C-1).
12. NO per-sub-sprint outcome-evidence re-bless. Outcome evidence
    consolidates at the milestone-shared re-bless after A + B + C-1 +
    C-2 land. `baseline_dir` + `docs/current_eval_baseline.md` do NOT
    flip until milestone close.
13. NO `BotSession.HandlingState` enum value additions (R3.a renders
    existing values only).

## 7. R-items consumed / surfaced

**Consumed at milestone open + post-ship expansion** (queued from M-Auto-5
close + the 2026-06-05 / 2026-06-06 proposal):

- **Sub-sprint A (S-Auto-23 / Sprint 078) — DEV-SIDE CLOSED 2026-06-06:**
  - `R-request-handover-intake-fields-schema-projection` (R1.a; proposal §4.1; layer `prompt_projection`) — **dev-side closed; milestone evidence deferred**.
  - `R-discover-clarification-counter-live-wireup` (R2.a; proposal §4.2; layer `infra` + `skill_state`; merged M-Auto-6 Cluster C.3 "generic clarifier branch wasting opening turn") — **dev-side closed; milestone evidence deferred**.
  - `R-entity-premise-projection-slot` (R4.a; proposal §4.4; layer `prompt_projection`; pairs with OBS-S1 deferred to autoloop) — **dev-side closed; milestone evidence deferred**.

- **Sub-sprint B (S-Auto-24 / Sprint 079) — CURRENT ACTIVE:**
  - `R-admin-trace-observability-session-list` (R3.a; proposal §4.3a; layer `infra` observability; merges with Cluster B.1 per_turn_trace truncation).
  - `R-admin-trace-observability-dedup-toolevent-folding` (R3.b; proposal §4.3b; layer `infra` observability; merges with Cluster C.2 46f5b2e9 500 + double-send live UI).
  - `R-admin-trace-observability-informational-guard-badge` (R3.c; proposal §4.3c; layer `infra` observability; NEW post-ship from c2/c10/c17).

- **Sub-sprint C-1 (S-Auto-25 / Sprint 080) — DEV-SIDE CLOSED 2026-06-06:**
  - `R-intake-partial-stash-update-tool` (R7; proposal §4.8; layer `skill_state` + `infra` + `prompt_projection` for new tool schema; NEW post-ship from c14; runtime enabler for OBS-S6) — **dev-side closed; Codex `APPROVE_S_AUTO_25 / blocking_count=0`; capability-wiring Option-A fence-waiver accepted; milestone evidence deferred**.
  - `R-resolve-intake-clarification-budget-mapping-extension` (R2.a#5-ext; proposal §4.7; layer `infra`; NEW post-ship from c14; preserves anti-误杀 #12 spirit) — **dev-side closed; milestone evidence deferred**.

- **Sub-sprint C-2a (S-Auto-26 / Sprint 081) — CURRENT ACTIVE (re-scoped 2026-06-06):**
  - `R-citation-display-token-url-preferred` (R5; proposal §4.5; re-scoped 2026-06-06 from "1-line `cite_token_field` swap" to citation contract change after the dev pre-fix audit caught wrong anchors). New layer mix: `infra` (ResolveArticleTool result body + `SkillGuardrailDispatcher.handleMustCiteSource` literal→shape rewrite) + `prompt_projection` (skill yaml citation-token wording at `:30/:31/~:80/:44`). Partial overlap with `R-canonical-url-corpus-curation` per action_bank §5 (R5 treats "article has URL but unused"; that R-item treats "article has no URL at all" — complementary).

- **Sub-sprint C-2b (S-Auto-27 / Sprint 082) — PLANNING CONTEXT (launches after C-2a dev-side close; re-scoped 2026-06-06):**
  - `R-corpus-search-knowledge-eligible-retrieval-filter` (R6; proposal §4.6; re-scoped 2026-06-06 from "new `bot_visible` data field" to "reuse existing `search_knowledge_eligible` field" after the dev anchor audit found the field already on 218/218 corpus articles, ingested-but-unused). Layer: `infra` (data + entity + V17 Flyway migration + ingestion + search service filter + hit + tool log). NOTE: the R-item was previously named `R-corpus-bot-visible-retrieval-filter`; renamed at the re-scope to reflect the field-name choice.

**Queued for S-Auto-28+ (post-M-Auto-6):**

- `R-aggregate-retains-per-attempt-composite-l2` (from M-Auto-5 Codex non-blocking observation #3).
- M-Auto-6 Cluster B.2 (`primary_uc` vs `active_use_case` mismatch; authority undecided; needs research-agent decision sub-sprint).
- M-Auto-6 Cluster C.1 (cs59s session_create 400; research-first short).
- Autoloop semantic sub-sprint targeting OBS-S1 (UC-A / UC-H / UC-J verify-entity-context procedure) / OBS-S2 (DISCOVER disambiguation cue) / **OBS-S6** (intake too literal — gated on R7 ship) / OBS-S7 (escalate-without-summary). OBS-S3 / OBS-S4 routed via `D-faq-grounded-resolve-bypass` (action_bank §4) reassessment.

**Status carry-forward**: M3-B P0; M-Auto-1A carry-overs (7);
`R-runtime-escalation-reason-turn-budget-conflated-with-intent` (B3/R5);
`R-classifier-non-deterministic-uc-selection-at-temp-zero` (C/R7) — note: this is an OLD R-item code from M-Auto-4-era naming, unrelated to the new R7 (intake partial-stash) in this milestone;
`R-autoloop-run-sweeps-dirty-index` standing hazard;
`R-eval-interactive-judge-score-never-populated` (chronic LOW). M-Auto-4
PAUSED.

## 8. Codex review plan (per §4.3)

**Per-sub-sprint Codex reviews:**

- **Sub-sprint A (S-Auto-23)**: REQUIRED — done. Verdict at commit
  `62b4d7b`: `APPROVE_S_AUTO_23 / blocking_count=0`. R1.a + R4.a touched
  the LLM-facing projection surface; R2.a's #4 budgets projection +
  #5 phase-aware mapping are control-plane labeling. Five non-blocking
  observations recorded.
- **Sub-sprint B (S-Auto-24)**: OPTIONAL (UI-only / §7-EXEMPT per
  proposal §6.5 + §7.2). Visual verification by deliver + human is the
  primary acceptance evidence; the milestone-shared Codex at M-Auto-6
  close covers B cumulatively.
- **Sub-sprint C-1 (S-Auto-25)**: REQUIRED — done. Verdict at the
  S-Auto-25 close commit: `APPROVE_S_AUTO_25 / blocking_count=0`. R7
  adds a new LLM-facing tool name (projection surface); R2.a#5-ext
  extends the existing phase-aware re-map (control-plane labeling).
  Five non-blocking observations recorded; capability-wiring Option-A
  fence-waiver accepted (F2 verdict).
- **Sub-sprint C-2a (S-Auto-26)**: REQUIRED — R5 #2 rewrites the
  citation guardrail semantics (literal substring → structural shape);
  R5 #3 updates the LLM-facing skill yaml citation-token wording.
  Both semantic-touching per `iteration_governance.md` §7 +
  `process/milestone-framework.md` §4.3. Codex focus must include the
  literal→shape semantics shift + grounding-floor preservation
  evidence + article_id-shape regex derived-from-data evidence.
- **Sub-sprint C-2b (S-Auto-27)**: REQUIRED — R6 changes the corpus
  retrieval surface visible to the LLM via the existing
  `search_knowledge_eligible` data field + service-layer filter.
  Data-field driven (NOT title-keyword; NOT hardcoded ID). Codex
  focus must include the data-field-only filter evidence, direct
  resolve invariant evidence, and forbidden-grep evidence (no
  hardcoded article-id strings in Java; no `(temp)` keyword filter).

All per-sub-sprint dev prompts and Codex prompts must be self-contained
per `prompt-artifact-rules.md` §9.1-§9.6 and embed:

- Full §4.1 nine-question kernel (NOT a reference).
- Full §7 stanza when REQUIRED (Target failure layer / Tier-0 invariant
  / Semantic hardcode / Generalization coverage).
- Anti-误杀 invariants (anchor uc_g/h/i/j/safety + shadow cs38s* must
  stay at 0.000 stable; for R6: the 2 `(temp)` articles flagged
  bot-invisible must not surface to LLM via search, but human-CS
  retrieval surfaces remain unchanged).
- File-path fence enumerating allowed edit surface for the sub-sprint.

**Milestone-shared Codex review** at M-Auto-6 close covers the cumulative
commit range over A + B + C-1 + C-2a + C-2b per
`iteration_governance.md` §4.3. The milestone-shared review is the
formal close gate.

## 9. Estimated duration

**Sub-sprint A (S-Auto-23 / Sprint 078; R1.a + R2.a + R4.a)** — SHIPPED:
delivered in 1 dev session + Codex + smoke. ~280 LOC (server) + ~160
test LOC across 5 new test classes; 53 new mocked-LLM characterization
tests. Real-LLM PRE-fix characterization absorbed into the dev session;
HUMAN-LAUNCHED outcome re-bless DEFERRED to the milestone-shared run.

**Sub-sprint B (S-Auto-24 / Sprint 079; R3.a + R3.b + R3.c)**: ~1-1.5
dev sessions per the proposal §6.5 + post-ship R3.c addition (~120 LOC
UI + ~80 test LOC; 2 main UI files: `SessionList.tsx`, `TraceViewer.tsx`,
plus UI test files + ≤ ~10 LOC `SessionManager.java` diagnostic
logging). Visual verification — no re-bless.

**Sub-sprint C-1 (S-Auto-25 / Sprint 080; R7 + R2.a#5-ext)** —
SHIPPED: delivered in 1 dev session + Codex + capability-wiring
fence-waiver decision. +30 net Java tests (path-β `IntakeFieldsMerger`
extraction + `UpdateIntakeFieldsTool` + dispatch smoke +
`MapBudgetToClarificationLabelTest` extension). Per-sub-sprint Codex
review delivered `APPROVE_S_AUTO_25 / blocking_count=0`.

**Sub-sprint C-2a (S-Auto-26 / Sprint 081; R5 citation contract fix)**:
~1-1.5 dev sessions per the post-rewrite scope (~140 LOC, ~3 files:
`ResolveArticleTool.java` additive `display_citation` +
`SkillGuardrailDispatcher.handleMustCiteSource` rewrite + two `Pattern`
constants + new `containsAcceptableCiteToken` helper +
`resolve_faq_grounded_answer.yaml` citation-token wording at `:30/:31/~:80/:44`
+ extensions to `ResolveArticleToolTest` and `SkillGuardrailDispatcherTest`).
Per-sub-sprint Codex review ~half a session with explicit focus on
the literal→shape semantics shift + grounding-floor preservation +
article_id-shape regex derived-from-data evidence.

**Sub-sprint C-2b (S-Auto-27 / Sprint 082; R6 corpus eligibility
filter)**: ~1-1.5 dev sessions per the post-rewrite scope (~180 LOC,
~6 files: `data/knowledge/knowledge_base_articles.json` 2-article flip
+ `KbArticle.java` new column + `V17` Flyway migration + new column +
`KnowledgeIngestionRunner` parser + `KnowledgeSearchService` filter +
INFO log + optional `KnowledgeHit` field + optional `SearchKnowledgeTool`
log + extensions to `KnowledgeIngestionRunnerTest` /
`KnowledgeSearchServiceTest` / `SearchKnowledgeToolTest` /
`ResolveArticleToolTest` direct-resolve invariant). Per-sub-sprint
Codex review ~half a session. Launches sequentially after C-2a
dev-side close.

**Milestone close run**: ONE milestone-shared real-LLM re-bless
(`autoloop/scripts/rebless_baseline.py --n 9`, multi-suite — bad_cases
+ anchor_outcome + shadow), ~30-60 minutes wall-time depending on
provider latency. Plus paired-evidence review.

**M-Auto-6 total**: best case (route (a)) = ~6-8 dev sessions across
A (done) + B (done) + C-1 (done) + C-2a + C-2b + milestone re-bless +
close. Worst case (route (b) re-diagnosis at milestone close) = +1
sub-sprint. C-2a and C-2b are sequenced (not parallel) per the
2026-06-06 re-scope to keep causal attribution clean for the
milestone-shared Codex review.

## 10. Stop conditions (milestone-level)

- **Milestone-shared re-bless evidence contradicts the falsifiable
  hypothesis AND diagnosis converges on a non-runtime cause** (e.g.
  remaining residual flake is genuine bot semantic ambiguity, not
  wiring): downgrade M-Auto-6 to Class C — In-flight downgrade; route
  remaining residual to autoloop semantic milestone.
- **Codex per-sub-sprint review surfaces blocking findings that cannot
  converge in ≤ 2 fix-iterations**: stop the milestone, human review
  required.
- **Anti-误杀 violation** (any persistent high-risk case mis-passes due
  to an R1/R2/R4/R5/R6/R7/R2.a#5-ext/R3.* change): immediate revert +
  diagnose; potential downgrade.
- **Eval CaseSpec misalignment with R2.a / R2.a#5-ext mapping wider
  than ~3 affected cases**: triage as `eval_spec` question; may require
  an interim spec-sync sub-sprint.
- **R7 `update_intake_fields` adoption rate at milestone re-bless = 0**
  (LLM never calls the new tool despite being declared in the schema):
  surface as OBS-S6-Pre observation; do not block close — R7 is the
  enabler, adoption is the LLM-side autoloop concern (OBS-S6 after
  close); however, the wiring evidence (schema declared + tool dispatch
  works + validator unchanged) must still be confirmed.
- **R6 retrieval filter false-positive rate > 0** (the filter blocks
  any non-`(temp)` article that should remain bot-visible): immediate
  diagnose; data field allow-list must remain explicit.

## 11. Cross-milestone sequencing

- M-Auto-5 (closed 2026-06-05) — eval-honesty floor LIVE; baseline_dir
  flipped to `m-auto-5-baseline-20260604-simfixed-stalledfix`.
- **M-Auto-6 (this milestone, ACTIVE)** — runtime substrate hygiene +
  admin observability + intake/clarification contract + UX/corpus
  governance. Sub-sprints A + B + C-1 dev-side closed 2026-06-06;
  C-2a current (re-scoped from original C-2 R5+R6 bundle after the
  dev pre-fix audit STOP); C-2b drafted as planning context (launches
  after C-2a dev-side close); milestone-shared re-bless at close.
- M-Auto-7+ — candidate: autoloop semantic optimization on cleaned
  surfaces (OBS-S1 UC-A/H/J verify-entity-context / OBS-S2 DISCOVER
  disambiguation cue / **OBS-S6** intake too literal — gated on R7 ship
  in M-Auto-6 C-1 / OBS-S7 escalate-without-summary) OR M-Auto-4
  S-Auto-18 resumption (escalation-family tier reshape) OR M3-B Single
  Handover Orchestrator P0. Decision deferred to M-Auto-6 close.
