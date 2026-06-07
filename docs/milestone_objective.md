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
    **DEV-SIDE CLOSED 2026-06-06**. Intake/clarification runtime
    contract bundle: **R7** new `update_intake_fields` no-op-side-effect
    tool (LLM can accumulate intake fields across turns without
    triggering the handover validator; runtime enabler for OBS-S6);
    **R2.a#5-ext** narrow phase-aware re-map extension to cover
    RESOLVE + intake-UC + free-text action (anti-误杀 #12 spirit
    preserved — RESOLVE non-intake + any-phase tool-call repeats
    stay `turn_budget_exhausted`). Codex per-sub-sprint
    `APPROVE_S_AUTO_25 / blocking_count=0`; capability-wiring
    Option-A fence-waiver ACCEPTED (byte-narrow tool-policy.yaml +
    skill yaml `tools_required` + SkillLoader VALID_TOOL_NAMES
    edits). Archive `docs/sprints/sprint-080-{objective,handoff}.md`.
  - **Sub-sprint C-2a = R5 citation contract fix** (S-Auto-26 /
    Sprint 081) — **DEV-SIDE CLOSED 2026-06-06**. Code shipped at
    commits `d4122c0..5a0ab3d` (intended cumulative range
    `d4122c0^..5a0ab3d` — 4 commits): R5 #1 ResolveArticleTool
    additive `display_citation` field; R5 #2 SkillGuardrailDispatcher
    `handleMustCiteSource` literal→structural-shape rewrite via
    `Pattern` constants at `:97-108` + `containsAcceptableCiteToken`
    helper + trace key rename `cite_token_field` →
    `cite_token_validator`; R5 #3 `resolve_faq_grounded_answer.yaml`
    citation-token wording at `:30/:31/~:80` + clean `cite_token_field`
    removal. Java baseline `1337 / 1 / 0 / 2` (+10 net tests). Two
    fence expansions Codex-accepted (#1 ResolveFaqGuardrailsTest
    fixture update; #2 PhaseEvaluatorResolveSkillIntegrationTest
    byte-mechanical golden mirror). Codex per-sub-sprint
    `APPROVE_S_AUTO_26 / blocking_count=0` on targeted re-review
    (substantive verdict PASS on initial review at HEAD `6e55d79`;
    procedural REJECT on clean-tree gate resolved by `8a7cb66`;
    substantive REJECT preserved at `221432d` for audit trail;
    targeted re-review prompt at `54b8729` flipped verdict header +
    §4 only with §1–§5 PASS preserved verbatim). Archive
    `docs/sprints/sprint-081-{objective,handoff}.md`. **Outcome
    evidence deferred to M-Auto-6 milestone-shared re-bless.**
  - **Sub-sprint C-2b = R6 corpus eligibility filter** (S-Auto-27 /
    Sprint 082) — **DEV-SIDE CLOSED 2026-06-06**. Code shipped at
    delivery commits `bb48aa0..7773c92` (5 commits: R6 #2+#3
    `KbArticle.searchKnowledgeEligible` entity + V17 Flyway migration
    `NOT NULL DEFAULT TRUE`; R6 #4 `KnowledgeIngestionRunner`
    ingestion parser via the existing `path(...).asBoolean(true)`
    `published_status` idiom; R6 #5+#7 `KnowledgeSearchService`
    filter step adjacent to Sprint-14 §L0 `isPublished`
    defense-in-depth pattern + INFO log path α at service layer;
    R6 #1+#8 JSON flip on `ka41r000000LIEJAA4` + `ka41r000000LIEEAA4`
    + first direct-resolve invariant test; dev handoff) + 3
    fix-iteration commits closing both initial Codex P0 blockers:
    `e6aad78` V17 SQL line-4 comment content-neutral rewrite
    (closes Codex §4 #2 forbidden-grep gate; SQL operation
    byte-unchanged); `d27b824` second direct-resolve invariant test
    for `ka41r000000LIEEAA4` (closes Codex §4 #1 F2/Q8 evidence
    gap); `4c8931f` handoff §3.1 fix-iteration addendum + over-claim
    correction. Range `bb48aa0^..4c8931f` contains 11 Git commits
    total: 8 substantive + 3 acknowledged audit/package
    (`056fa5a` aidazi framework v3.2 archive; `3300b4a`
    per-sub-sprint Codex review prompt; `1954cb6` initial Codex
    `APPROVE_S_AUTO_27_WITH_FIXES` audit trail). Original 5
    delivery commits unamended and append-only. Java baseline
    `1348 / 1 / 0 / 2` (+10 net tests; sole failure = inherited
    `SystemPromptUserRequestedTiebreakerTest` OQ-S41.5, provably
    uncoupled — system-prompt surface, C-2b touched zero prompt
    files). Pre-fix audit decisions captured in handoff §1:
    KnowledgeHit (#6) SKIPPED (post-filter eligible-by-construction);
    INFO log (#7) path α at service layer (`SearchKnowledgeTool`
    byte-untouched). Codex per-sub-sprint `APPROVE_S_AUTO_27 /
    blocking_count=0` on targeted re-review. Archive
    `docs/sprints/sprint-082-{objective,handoff}.md`. **Outcome
    evidence deferred to M-Auto-6 milestone-shared re-bless.**
  - **Milestone-shared §9 real-LLM re-bless** launches NEXT — single
    milestone-level evidence run after C-2b dev-side close, paired
    against `m-auto-5-baseline-20260604-simfixed-stalledfix`.

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
| C-2a (R5 citation contract fix) | `infra` (ResolveArticleTool result + handleMustCiteSource semantics rewrite) + `prompt_projection` (skill yaml citation wording at `:30/:31/~:80/:44`) | ✅ REQUIRED — done; Codex `APPROVE_S_AUTO_26 / blocking_count=0` | R5 #2 rewrote LLM-facing citation guardrail from literal substring to structural URL/article_id shape predicate; R5 #3 changed LLM-facing skill wording to point at new `display_citation` field. Shipped at S-Auto-26 close. |
| C-2b (R6 corpus eligibility filter) | `infra` (data + entity + V17 migration + ingestion + service filter + tool log) | ✅ REQUIRED — done; Codex `APPROVE_S_AUTO_27 / blocking_count=0` on targeted re-review (both prior P0 blockers resolved by fix-iteration) | R6 changed LLM-facing search retrieval surface via existing `search_knowledge_eligible` data field. Direct resolve unfiltered (human-CS access preserved). Shipped at S-Auto-27 close. |
| **S-Auto-28 (R8 KnowledgeIngestionRunner `--reconcile` data-application path)** | `infra` (server-side knowledge data-application path; standalone `--reconcile` metadata-only mode UPDATEs 3 mutable curation columns from JSON on existing rows; plain `--ingest` byte-for-byte unchanged) | ✅ INCLUDED CONSERVATIVELY — done; Codex `APPROVE_S_AUTO_28 / blocking_count=0` under §4.1 pure-infra scope exemption | R8 is the **M-Auto-6 milestone-close BLOCKER fix** — without it the populated dev DB cannot reflect R6's committed JSON flip (insert-only runner skips existing IDs). R8 itself is pure infra and Codex invoked the §4.1 scope exemption + recorded `approve`. Downstream surface is R6's LLM-facing search; no semantic decision added. Shipped at S-Auto-28 close 2026-06-07. |

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
| R5 — citation contract fix: additive `display_citation` on ResolveArticleTool + `SkillGuardrailDispatcher.handleMustCiteSource` literal→shape rewrite + skill yaml citation wording at `:30/:31/~:80/:44` | `infra` + `prompt_projection` | C-2a | ✅ SHIPPED (dev-side closed; Codex `APPROVE_S_AUTO_26 / blocking_count=0` on targeted re-review) | c13 surface fix: bot now cites URLs when article has `source_url`; article_id fallback preserved for URL-less articles. Grounding floor preserved (empty / null / plain-English STILL reject); accept criteria widened from "literal field-name substring" to "URL-shape OR Salesforce article_id-shape". |
| R6 — corpus eligibility filter: reuse existing `search_knowledge_eligible` data field + full plumbing (V17 migration + ingestion + entity + search service + hit + tool) | `infra` (data + entity + migration + service + tool) | C-2b | ✅ SHIPPED (dev-side closed; Codex `APPROVE_S_AUTO_27 / blocking_count=0` on targeted re-review; both prior P0 blockers resolved by fix-iteration) | c7/c12/c17 surface fix: `(temp)` template articles filtered from LLM-facing search. `KnowledgeHit` SKIPPED (post-filter eligible-by-construction); INFO log path α at service layer; `SearchKnowledgeTool` byte-untouched. Direct resolve unfiltered (anti-误杀 #1 preserved — both flagged articles test-pinned for direct resolve). |
| R8 — `KnowledgeIngestionRunner` `--reconcile` metadata-only data-application path (M-Auto-6 milestone-close blocker fix; reconcile UPDATEs 3 mutable curation columns `search_knowledge_eligible` / `is_published` / `uc_tags` on existing rows from JSON; standalone `--reconcile` canonical; plain `--ingest` byte-unchanged) | `infra` (server-side knowledge data-application path) | S-Auto-28 | ✅ SHIPPED (dev-side closed 2026-06-07; Codex `APPROVE_S_AUTO_28 / blocking_count=0` under §4.1 pure-infra scope exemption; F1-F6 PASS; 3 NBOs incl. standalone entry-gate test gap queued as `R-standalone-reconcile-entry-gate-test` for S-Auto-29+) | Pre-flight §0.3 surface fix: R6's committed JSON flip cannot land on populated dev DB without an UPDATE path — runner was insert-only. R8 adds metadata-only reconcile mode; load existing → set 3 curation fields from JSON → save. No re-chunk / no re-embed (mock-interaction counts verified); content columns immutable; direct resolve unfiltered (anti-误杀 #1); no prune. Mock-level wiring evidence at dev close; real-DB §0.3 evidence sequenced post-Codex per §5.7. |

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

Sequence at C-2b dev-side close 2026-06-06: A + B + C-1 + C-2a + C-2b
ALL DEV-SIDE CLOSED. Sequential cadence: A (done) → B (done) → C-1
(done) → C-2a (done) → C-2b (done) → **milestone-shared §9 real-LLM
re-bless launches NEXT**. No re-bless ran between sub-sprints —
outcome evidence runs once at milestone close, paired against
`m-auto-5-baseline-20260604-simfixed-stalledfix`. `baseline_dir` +
`docs/current_eval_baseline.md` flip at the milestone close decision.

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

### Sub-sprint C-2a — S-Auto-26 / Sprint 081 — R5 citation contract fix — **DEV-SIDE CLOSED 2026-06-06**

R5 citation contract fix (per proposal §4.5; re-scoped 2026-06-06
after the dev pre-fix audit on the original C-2 STOPPED with wrong
anchors caught; the dev STOP caught it BEFORE any code was written —
the original prompt's anchors `guardrails/MustCiteSource.java` and
its "shape validation" premise were both wrong; the live guardrail
lives at `SkillGuardrailDispatcher.java:340-384` doing literal
substring matching).

> **S-Auto-26 is dev-side closed / Codex-approved on targeted
> re-review / fence-expansion #2 byte-mechanical accepted;
> milestone-level outcome evidence is deferred to the M-Auto-6
> final re-bless.**

Status:
- Code shipped: commits `d4122c0..5a0ab3d` (intended cumulative range
  `d4122c0^..5a0ab3d` — 4 commits: R5 #1 ResolveArticleTool
  additive `display_citation`; R5 #2 SkillGuardrailDispatcher
  literal→structural-shape rewrite + ResolveFaqGuardrailsTest
  fixture update; R5 #3 skill yaml citation-token wording +
  `cite_token_field` clean removal +
  PhaseEvaluatorResolveSkillIntegrationTest golden mirror update;
  dev handoff).
- Java baseline preserved: `1337 / 1 / 0 / 2` (+10 net tests; sole
  failure = inherited OQ-S41.5).
- Focused tests at Codex review-time: `67 / 0 / 0 / 0`
  (`ResolveArticleToolTest` + `SkillGuardrailDispatcherTest` +
  `ResolveFaqGuardrailsTest` +
  `PhaseEvaluatorResolveSkillIntegrationTest`).
- Two fence expansions Codex-accepted: #1 ResolveFaqGuardrailsTest
  `kb-001` fixtures → real corpus `ka41r000000LIEEAA4`
  (pre-approved per pre-fix audit (c) STOP-condition preferred
  path); #2 PhaseEvaluatorResolveSkillIntegrationTest golden
  constants byte-mirror updated (NOT in original fence; discovered
  at build time; mechanically forced byte-mirror; Codex F5
  confirmed byte-mechanical).
- Codex per-sub-sprint review: `APPROVE_S_AUTO_26 /
  blocking_count=0` on targeted re-review. §1 per-change verdicts
  approve all of R5 #1 + #2 + #3 + both fence expansions. §2 §4.1
  Q1–Q9 PASS aggregate `approve`. §3 F1–F5 PASS (literal→structural-shape
  semantics shift; grounding-floor preservation; article_id regex
  derived-from-218/218 IDs; yaml minimum-edit; fence-expansion #2
  byte-mechanical). 3 non-blocking observations recorded. Procedural
  REJECT on clean-tree gate resolved by `8a7cb66`; substantive
  REJECT preserved at `221432d` for audit trail; targeted re-review
  prompt at `54b8729` flipped verdict header + §4 only with §1–§5
  PASS preserved verbatim.
- `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md`
  UNCHANGED.

Archived contract: `docs/sprints/sprint-081-objective.md`.

### Sub-sprint C-2b — S-Auto-27 / Sprint 082 — R6 corpus eligibility filter — **DEV-SIDE CLOSED 2026-06-06**

R6 corpus eligibility filter (per proposal §4.6; re-scoped 2026-06-06
to reuse the existing `search_knowledge_eligible` field that already
lives on 218/218 corpus articles, ingested-but-unused per the dev
anchor audit — instead of introducing a parallel new `bot_visible`
mechanism per the human re-scope decision).

> **S-Auto-27 is dev-side closed / Codex-approved on targeted
> re-review / both prior P0 blockers resolved by fix-iteration;
> milestone-level outcome evidence is deferred to the M-Auto-6
> final re-bless.**

Status:
- Code shipped: 5 delivery commits `bb48aa0..7773c92` (R6 #2+#3
  KbArticle entity + V17 Flyway migration; R6 #4 ingestion parser
  via existing `published_status` idiom; R6 #5+#7
  KnowledgeSearchService filter step + INFO log path α at service
  layer; R6 #1+#8 JSON flip + first direct-resolve invariant test;
  dev handoff) + 3 fix-iteration commits closing both initial
  Codex P0 blockers: `e6aad78` V17 SQL line-4 comment content-neutral
  rewrite (closes Codex §4 #2 F3 forbidden-grep gate; SQL operation
  byte-unchanged); `d27b824` second direct-resolve invariant test
  `execute_searchIneligibleArticle_stillResolvesByDirectId_secondTemplate`
  for `ka41r000000LIEEAA4` at `ResolveArticleToolTest:275-303`
  mirroring first-template at `:239-272` (closes Codex §4 #1 F2/Q8
  evidence gap); `4c8931f` handoff §3.1 fix-iteration addendum +
  over-claim correction at R6 #8.
- Cumulative range `bb48aa0^..4c8931f` contains 11 Git commits:
  8 substantive (5 delivery + 3 fix-iteration) + 3 acknowledged
  audit/package (`056fa5a` aidazi framework v3.2 archive;
  `3300b4a` per-sub-sprint Codex review prompt; `1954cb6` initial
  Codex `APPROVE_S_AUTO_27_WITH_FIXES` audit trail). Original 5
  delivery commits unamended and append-only.
- Java baseline preserved: `1348 / 1 / 0 / 2` (+10 net tests vs the
  1337/1/0/2 launch baseline; +1 from `d27b824` second direct-resolve;
  sole failure = inherited OQ-S41.5).
- Focused tests at Codex targeted re-review time: `23 / 0 / 0 / 0`
  (`KnowledgeIngestionRunnerTest` + `KnowledgeSearchServiceTest` +
  `KbArticleEligibilityCorpusTest` + `ResolveArticleToolTest`).
  Initial review's broader 7-suite run was `34 / 0 / 0 / 0`.
- Pre-fix audit design choices: #1 no conflicting governance field;
  #2 primitive `boolean` + `@Builder.Default=true` so Lombok @Data
  generates `isSearchKnowledgeEligible()`; #3 V16 confirmed highest,
  V17 chosen with `NOT NULL DEFAULT TRUE` mirroring V4 `is_published`
  pattern; #4 existing `published_status` parsing idiom mirrored via
  `path(...).asBoolean(true)`; #5 single Step-8 caller path confirmed;
  #6 KnowledgeHit SKIPPED — hits are post-filter
  eligible-by-construction; #7 path α chosen — INFO log at service
  layer, `SearchKnowledgeTool` byte-untouched.
- Codex per-sub-sprint review: `APPROVE_S_AUTO_27 / blocking_count=0`
  on targeted re-review. §1 per-change verdicts PASS with explicit
  pass-on-re-review notes for #3 (V17 SQL comment) and #8 (second
  direct-resolve test). §2 §4.1 Q1–Q9 PASS aggregate `approve`. §3
  F1–F6 PASS (data-field-only filter; direct resolve invariant on
  BOTH flagged IDs; forbidden-grep clean post-fix; back-compat
  default-true; no parallel governance field; pre-fix audit
  decisions). 4 non-blocking observations recorded.
- Forbidden-grep evidence: `server/src/main` contains zero `(temp)`
  matches and zero literal article-ID strings post-`e6aad78`.
- `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md`
  UNCHANGED.

Archived contract: `docs/sprints/sprint-082-objective.md`.

### Sub-sprint S-Auto-28 — Sprint 083 — R8 KnowledgeIngestionRunner `--reconcile` data-application path — **DEV-SIDE CLOSED 2026-06-07** (M-Auto-6 milestone-close BLOCKER fix)

R8 KnowledgeIngestionRunner `--reconcile` metadata-only data-application
path. Promoted 2026-06-07 ahead of the milestone-shared re-bless because
the §5.9 pre-flight runbook (`docs/current/process/preflight-eval-checks.md`)
returned **NO-GO at §0.3** on 2026-06-07 (recorded at
`docs/diagnostics/2026-06-07-m-auto-6-preflight-verdict.md`): the populated
dev DB's 2 `(temp)` template rows still read `search_knowledge_eligible=true`
after R6's JSON flip was committed, because the insert-only runner
(`KnowledgeIngestionRunner.run()` at `:66-126` skip-existing branch at
`:84-105`) skips every already-present `article_id` and never UPDATEs. R6's
close gates (`BEGIN…ROLLBACK` migration-mechanics test + synthetic-article
unit tests) did not catch this populated-DB regression; the pre-flight
runbook did, exactly as the §5.9 gate was designed to. R8 is the structural
fix per the blocker brief at
`docs/diagnostics/failure-briefs/preflight-2026-06-07-kb-ingest-skip-existing-blocks-r6-flag.md`.

> **S-Auto-28 is dev-side closed / Codex-approved under §4.1 pure-infra
> scope exemption / M-Auto-6 milestone-close blocker FIXED at code level;
> real-DB §0.3 evidence sequenced post-close per §5.7.**

Status:
- Code shipped: 4 commits `ba3defa..78ae614` (R8 #1+#2+#3
  `KnowledgeIngestionRunner` `--reconcile` metadata-only data-application +
  reconcile observability + `KnowledgeIngestionReconcileTest` 9 tests
  covering #4(a) + #4(b); R8 #4(c) `KnowledgeReconcileEndToEndTest` R6
  end-to-end wiring at mock level; R8 #5 `preflight-eval-checks.md` 5 drift
  fixes + §0.3 / A3 root-cause annotation; R8 #6 dev handoff).
- Java baseline preserved: `1358 / 1 / 0 / 2` (+10 net tests vs the
  1348/1/0/2 launch baseline; sole failure = inherited OQ-S41.5
  `SystemPromptUserRequestedTiebreakerTest`, provably uncoupled — this
  sub-sprint touched zero prompt files).
- Focused 5-suite tests at Codex review time: `31 / 0 / 0 / 0`.
- Canonical invocation pinned: standalone `--reconcile`. Runner-entry gate
  widened to `if (!ingest && !reconcile) return;` at `:78-85`; per-article
  branch on `reconcile` at `:161-174`. `--ingest --reconcile` non-canonical
  (parser-accepted but NOT pinned, NOT tested). Plain `--ingest` is
  byte-for-byte unchanged (insert-only skip-existing).
- Pre-fix audit (3 STOPs cleared BEFORE any code): `save()` is full-row
  UPDATE on existing-id entity (no `@DynamicUpdate` → load-then-modify
  mandatory); mutable curation columns = `searchKnowledgeEligible` /
  `isPublished` / `ucTags`; no JPA cascade to `kb_chunks` (chunks referenced
  via plain `String articleId`, no `@ManyToOne`/`@OneToMany`).
- Mock-interaction evidence (F1 / anti-误杀 #2 — the key gate): on existing
  reconcile, `embeddingClient.embedBatch` count = **0**, `kbChunkRepository.saveAll`
  count = **0** (verified via `verify(..., never())`); on new-id insert under
  `--reconcile`, `embedBatch` count = **1** (proves branch split correct,
  NOT blanket no-embed).
- Codex per-sub-sprint review: `APPROVE_S_AUTO_28 / blocking_count=0` under
  §4.1 pure-infra scope exemption. §1 per-change verdicts PASS for R8 #1-#6;
  §2 Q1-Q9 PASS with scope-exemption invoked + aggregate `approve`; §3 F1-F6
  focal-point verdicts PASS — F1 metadata-only (Codex independently verified
  mock-interaction counts), F2 plain `--ingest` unchanged, F3 no manual SQL
  evidence (post-Codex sequence pinned), F4 direct resolve invariant
  (`ResolveArticleTool` direct-by-id returns both flagged IDs post-reconcile),
  F5 forbidden-grep + R6 surface byte-unchanged (Codex re-ran `git grep` at
  `78ae614` and confirmed blob-hash identity for `KnowledgeSearchService.java`,
  `KbArticle.java`, V17, `knowledge_base_articles.json`,
  `docs/current_eval_baseline.md`, `autoloop/config.yaml`), F6 no prune
  (`processArticles(...)` only iterates JSON + reconciles/skips/inserts; no
  `kbArticleRepository.delete*` exists). 3 non-blocking observations: (NBO #1)
  standalone entry-gate test gap — no test directly supplies standalone
  `--reconcile` through `ApplicationArguments` to `run()`; queued as
  `R-standalone-reconcile-entry-gate-test` for S-Auto-29+ infra-test pickup;
  (NBO #2) post-APPROVE evidence assertions reminder; (NBO #3) independent
  review verification passed at `feb3419` (focused tests + diff-check clean).
- Real-DB §0.3 evidence DEFERRED post-Codex per §5.7 wiring-vs-outcome
  separator + the contract's Definition-of-done sequence. The dev-close
  gate was mock-level wiring evidence (10 new tests + mock-interaction
  counts); the live `--reconcile` run + §0.3 re-check are produced AFTER
  Codex APPROVE_S_AUTO_28 because they require a backend rebuild and mutate
  the shared dev DB. Anti-误杀 #7 forbids manual SQL UPDATE as evidence —
  the §0.3 DB state MUST be produced by the `--reconcile` run.
- `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md` UNCHANGED.

Archived contract: `docs/sprints/sprint-083-objective.md`.

### Milestone close — milestone-shared §9 real-LLM re-bless

After A + B + C-1 + C-2a + C-2b + **S-Auto-28** all land + the S-Auto-28
Definition-of-done unblock sequence completes (rebuild backend + standalone
`--reconcile` + §0.3 re-check), deliver-agent + human resume the M-Auto-6
pre-flight from Step 1 (bad_cases smoke per
`docs/current/process/preflight-eval-checks.md` §2 Step 1) → Step 2
(anchor_outcome anti-误杀 sentinel) → §4 verdict; on GO, launch ONE
milestone-shared real-LLM re-bless (`cd autoloop && uv run python
scripts/rebless_baseline.py --n 9 --out-dir
../eval_interactive/results/m-auto-6-baseline-shared-YYYYMMDD/`).
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

### S-Auto-29+ — TBD (post-M-Auto-6)

Per the M-Auto-6 audit-cluster routing + the M-Auto-5 Codex
non-blocking observation #3 R-item + the S-Auto-28 Codex NBO #1
queue. Candidates:
- `R-standalone-reconcile-entry-gate-test` (NEW from S-Auto-28
  Codex NBO #1; infra-test pickup; pin standalone `--reconcile`
  through `ApplicationArguments` to `run()` so a gate regression
  fails before the shared DB step).
- Cluster B.2 (`primary_uc` vs `active_use_case` authority — needs
  research-agent decision sub-sprint).
- Cluster C.1 (cs59s session_create 400 — research-first short).
- `R-aggregate-retains-per-attempt-composite-l2` (from M-Auto-5 Codex
  observation #3).
- Autoloop semantic sub-sprint targeting OBS-S1 / OBS-S2 / OBS-S6
  (OBS-S6 only after R7 ships, now landed at C-1) / OBS-S7.

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
| Anti-误杀 (persistent high-risk cases) | Intake anchors uc_g_gdpr / uc_h_appeal / uc_i_payment / uc_j_safety: safety-of-pass (reframed 2026-06-07 post-R7; pre-R7 this read "0.000 stable"). Shadow cs38s*: still 0.000 stable floor (not reframed). | Intake anchors: no UNSAFE pass — every PASS has genuine required fields (customer/session-sourced) + request_handover / controlled case creation where applicable + safe escalation (containment=escalated). Shadow cs38s*: UNCHANGED at 0.000 stable. | Intake anchors: reject only an unsafe pass — empty-handover, self-resolve of the sensitive issue, missing/fabricated fields, or superficial tier-2 field-presence; a bare rise off 0.000 is permissible if verified as genuine-intake-then-escalate. Shadow cs38s*: any rise (an artifact mis-passes) → reject as masking. |

**Anti-误杀 reframe (2026-06-07, post-R7).** R7's `update_intake_fields`
(S-Auto-25) changed the intake-state projection surface the tier-2
`*-intake-complete-before-handover` step reads, so the intake anchors can
now legitimately PASS when the bot genuinely collects the required fields
and then escalates. The absolute 0.000 floor for these
intake-complete-before-handover steps was partly a pre-R7 projection
artifact; the safety tripwire remains the unsafe-pass invariant.
Therefore the `m-auto-5-baseline-20260604-simfixed-stalledfix` numbers
for the G/H/I/J/K intake anchors are **pre-R7 — comparable only for
unsafe-pass detection, NOT as an absolute 0.000 floor.** This reframe
covers ONLY the uc_g / uc_h / uc_i / uc_j intake anchors; **shadow
cs38s* retains its 0.000 stable floor** unless separately diagnosed and
reframed. The targeted n=9 diagnostic `diag-anchor4-20260607-083632`
confirms every off-0.000 pass is genuine-intake-then-escalate
(HARD=0 / SOFT=0; 0/36 self-resolve); see
`docs/diagnostics/2026-06-07-m-auto-6-anchor-overpass-diagnostic.md`.

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

- **Sub-sprint B (S-Auto-24 / Sprint 079) — DEV-SIDE CLOSED 2026-06-06 (visual-verified; §7-EXEMPT):**
  - `R-admin-trace-observability-session-list` (R3.a; proposal §4.3a; layer `infra` observability) — **dev-side closed; visual-verified; milestone evidence deferred**.
  - `R-admin-trace-observability-dedup-toolevent-folding` (R3.b; proposal §4.3b; layer `infra` observability) — **dev-side closed; visual-verified; milestone evidence deferred**.
  - `R-admin-trace-observability-informational-guard-badge` (R3.c; proposal §4.3c; layer `infra` observability) — **dev-side closed; visual-verified; milestone evidence deferred**.

- **Sub-sprint C-1 (S-Auto-25 / Sprint 080) — DEV-SIDE CLOSED 2026-06-06:**
  - `R-intake-partial-stash-update-tool` (R7; proposal §4.8; layer `skill_state` + `infra` + `prompt_projection` for new tool schema; NEW post-ship from c14; runtime enabler for OBS-S6) — **dev-side closed; Codex `APPROVE_S_AUTO_25 / blocking_count=0`; capability-wiring Option-A fence-waiver accepted; milestone evidence deferred**.
  - `R-resolve-intake-clarification-budget-mapping-extension` (R2.a#5-ext; proposal §4.7; layer `infra`; NEW post-ship from c14; preserves anti-误杀 #12 spirit) — **dev-side closed; milestone evidence deferred**.

- **Sub-sprint C-2a (S-Auto-26 / Sprint 081) — DEV-SIDE CLOSED 2026-06-06:**
  - `R-citation-display-token-url-preferred` (R5; proposal §4.5; re-scoped 2026-06-06 from "1-line `cite_token_field` swap" to citation contract change after the dev pre-fix audit caught wrong anchors). New layer mix: `infra` (ResolveArticleTool result body + `SkillGuardrailDispatcher.handleMustCiteSource` literal→shape rewrite) + `prompt_projection` (skill yaml citation-token wording at `:30/:31/~:80/:44`). Partial overlap with `R-canonical-url-corpus-curation` per action_bank §5 (R5 treats "article has URL but unused"; that R-item treats "article has no URL at all" — complementary). **Dev-side closed; Codex `APPROVE_S_AUTO_26 / blocking_count=0` on targeted re-review; both fence expansions accepted; milestone evidence deferred**.

- **Sub-sprint C-2b (S-Auto-27 / Sprint 082) — DEV-SIDE CLOSED 2026-06-06:**
  - `R-corpus-search-knowledge-eligible-retrieval-filter` (R6; proposal §4.6; re-scoped 2026-06-06 from "new `bot_visible` data field" to "reuse existing `search_knowledge_eligible` field" after the dev anchor audit found the field already on 218/218 corpus articles, ingested-but-unused). Layer: `infra` (data + entity + V17 Flyway migration + ingestion + search service filter + hit + tool log). NOTE: the R-item was previously named `R-corpus-bot-visible-retrieval-filter`; renamed at the re-scope to reflect the field-name choice. **Dev-side closed; Codex `APPROVE_S_AUTO_27 / blocking_count=0` on targeted re-review; both prior P0 blockers resolved by fix-iteration (`e6aad78` V17 SQL comment content-neutral + `d27b824` second direct-resolve test for `ka41r000000LIEEAA4` + `4c8931f` handoff addendum); KnowledgeHit (#6) SKIPPED + INFO log (#7) path α decisions accepted; milestone evidence deferred**.

- **Sub-sprint S-Auto-28 (Sprint 083) — DEV-SIDE CLOSED 2026-06-07** (M-Auto-6 milestone-close BLOCKER fix):
  - `R-knowledge-ingestion-reconcile-data-application` (R8; NEW 2026-06-07 from the §5.9 pre-flight NO-GO at §0.3; closes blocker brief `preflight-2026-06-07-kb-ingest-skip-existing-blocks-r6-flag.md`). Layer: `infra` (server-side knowledge data-application path; `KnowledgeIngestionRunner` gains a standalone `--reconcile` metadata-only mode that UPDATEs the 3 mutable curation columns `search_knowledge_eligible` / `is_published` / `uc_tags` of existing rows from JSON; plain `--ingest` byte-for-byte unchanged). **Dev-side closed; Codex `APPROVE_S_AUTO_28 / blocking_count=0` under §4.1 pure-infra scope exemption; F1-F6 all PASS; mock-interaction counts pin no-re-embed + no-chunk-write + content-column preservation + plain-`--ingest`-unchanged + new-id-insert-still-embeds + direct-resolve invariant + no-prune; 3 NBOs (#1 standalone entry-gate test gap queued as `R-standalone-reconcile-entry-gate-test`; #2 post-APPROVE evidence assertions; #3 independent review verification 31/0/0/0 + diff-check clean); real-DB §0.3 evidence sequenced post-Codex per §5.7**.

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
- **Sub-sprint C-2a (S-Auto-26)**: REQUIRED — done. Verdict at the
  S-Auto-26 close commit: `APPROVE_S_AUTO_26 / blocking_count=0` on
  targeted re-review. §1 per-change verdicts PASS; §2 Q1–Q9 PASS;
  §3 F1–F5 PASS (literal→structural-shape semantics shift verified;
  grounding-floor preservation; article_id regex derived from
  218/218 corpus IDs; yaml minimum-edit; fence-expansion #2
  byte-mechanical). 3 non-blocking observations recorded. Procedural
  REJECT on clean-tree gate resolved by `8a7cb66`; substantive REJECT
  preserved at `221432d` for audit trail; targeted re-review prompt
  at `54b8729` flipped verdict header + §4 only with §1–§5 PASS
  preserved verbatim.
- **Sub-sprint C-2b (S-Auto-27)**: REQUIRED — done. Verdict at the
  S-Auto-27 close commit: `APPROVE_S_AUTO_27 / blocking_count=0` on
  targeted re-review. Initial review returned `APPROVE_S_AUTO_27_WITH_FIXES
  / blocking_count=2` (commit `1954cb6` audit trail) on two
  infra-hygiene gaps: F2/Q8 second direct-resolve test for
  `ka41r000000LIEEAA4` missing + F3 V17 SQL line-4 `(temp)` literal
  in the comment tripping the forbidden-grep gate. Both resolved by
  fix-iteration commits `e6aad78` (V17 SQL content-neutral rewrite;
  SQL operation byte-unchanged) + `d27b824` (second direct-resolve
  test at `ResolveArticleToolTest:275-303`) + `4c8931f` (handoff
  §3.1 addendum + over-claim correction). Targeted re-review prompt
  at `a7c5b6f` verified clean tree + cumulative range; Codex's
  flipped verdict: §1 per-change verdicts PASS with explicit
  pass-on-re-review notes for #3 and #8; §2 Q1–Q9 PASS aggregate
  `approve`; §3 F1–F6 PASS (data-field-only filter; both flagged
  IDs direct-resolve test-pinned; forbidden-grep clean post-fix;
  back-compat default-true; no parallel governance field; pre-fix
  audit decisions PASS). 4 non-blocking observations recorded.

All per-sub-sprint dev prompts and Codex prompts must be self-contained
per `prompt-artifact-rules.md` §9.1-§9.6 and embed:

- Full §4.1 nine-question kernel (NOT a reference).
- Full §7 stanza when REQUIRED (Target failure layer / Tier-0 invariant
  / Semantic hardcode / Generalization coverage).
- Anti-误杀 invariants (intake anchors uc_g/h/i/j: no UNSAFE pass —
  genuine intake + handover/case + safe escalation; reject empty-handover
  / self-resolve / superficial-tier2; reframed 2026-06-07 post-R7, see
  §5. Shadow cs38s* must still stay at 0.000 stable. For R6: the 2
  `(temp)` articles flagged bot-invisible must not surface to LLM via
  search, but human-CS retrieval surfaces remain unchanged).
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

**Sub-sprint C-2a (S-Auto-26 / Sprint 081; R5 citation contract fix)** —
SHIPPED: delivered in 1 dev session + Codex per-sub-sprint targeted
re-review. 4 commits (`d4122c0..5a0ab3d`). +10 net Java tests across
`ResolveArticleToolTest` + `SkillGuardrailDispatcherTest` +
`ResolveFaqGuardrailsTest` (fixture update; fence expansion #1) +
`PhaseEvaluatorResolveSkillIntegrationTest` (byte-mechanical golden
mirror; fence expansion #2). Per-sub-sprint Codex review delivered
`APPROVE_S_AUTO_26 / blocking_count=0`.

**Sub-sprint C-2b (S-Auto-27 / Sprint 082; R6 corpus eligibility
filter)** — SHIPPED: delivered in 1 dev session + initial Codex
review + fix-iteration + targeted re-review. 5 delivery commits
(`bb48aa0..7773c92`) + 3 fix-iteration commits (`e6aad78` + `d27b824`
+ `4c8931f`) + 3 audit/package commits (`056fa5a` + `3300b4a` +
`1954cb6`) = 11 Git commits in cumulative range `bb48aa0^..4c8931f`.
+10 net Java tests across `KbArticleEligibilityCorpusTest` (new) +
`KnowledgeIngestionRunnerTest` extensions + `KnowledgeSearchServiceTest`
extensions + `ResolveArticleToolTest` extensions (both flagged IDs
direct-resolve test-pinned post-fix-iteration). Per-sub-sprint Codex
targeted re-review delivered `APPROVE_S_AUTO_27 / blocking_count=0`
after the two initial infra-hygiene gaps were resolved.

**Milestone close run**: ONE milestone-shared real-LLM re-bless
(`autoloop/scripts/rebless_baseline.py --n 9`, multi-suite — bad_cases
+ anchor_outcome + shadow), ~30-60 minutes wall-time depending on
provider latency. Plus paired-evidence review.

**M-Auto-6 total**: A (done) + B (done) + C-1 (done) + C-2a (done) +
C-2b (done) — all 5 sub-sprints dev-side closed 2026-06-06. Remaining
work: milestone-shared §9 real-LLM re-bless (NEXT human-launched
action) + paired-evidence review + milestone-shared Codex review at
`compact/M-Auto-6-review-prompt.md` + close decision per §5 routes.
Worst case (route (b) re-diagnosis at milestone close) = +1
fix-iteration sub-sprint.

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
- **M-Auto-6 (this milestone, ACTIVE; dev-side complete, milestone
  close pending)** — runtime substrate hygiene + admin observability
  + intake/clarification contract + UX/corpus governance. Sub-sprints
  A + B + C-1 + C-2a + C-2b ALL dev-side closed 2026-06-06; **S-Auto-28
  (R8 KnowledgeIngestionRunner `--reconcile` data-application path;
  M-Auto-6 milestone-close BLOCKER fix surfaced by the §5.9 pre-flight
  NO-GO at §0.3) dev-side closed 2026-06-07**. S-Auto-28
  Definition-of-done **unblock sequence** is the immediate NEXT action
  (rebuild backend + standalone `--reconcile` + §0.3 re-check; anti-误杀
  #7 forbids manual SQL UPDATE as evidence). On §0.3 PASS: resume the
  M-Auto-6 pre-flight from Step 1 (bad_cases smoke per
  `docs/current/process/preflight-eval-checks.md`) → Step 2
  (anchor_outcome anti-误杀 sentinel) → §4 verdict; on GO, the
  milestone-shared §9 real-LLM re-bless launches; milestone-shared
  Codex review at `compact/M-Auto-6-review-prompt.md` follows; close
  decision per §5 routes (a) / (b) / (c) gated on the re-bless evidence
  + Codex verdict.
- M-Auto-7+ — candidate: autoloop semantic optimization on cleaned
  surfaces (OBS-S1 UC-A/H/J verify-entity-context / OBS-S2 DISCOVER
  disambiguation cue / **OBS-S6** intake too literal — gated on R7 ship
  in M-Auto-6 C-1 / OBS-S7 escalate-without-summary) OR M-Auto-4
  S-Auto-18 resumption (escalation-family tier reshape) OR M3-B Single
  Handover Orchestrator P0. Decision deferred to M-Auto-6 close.
