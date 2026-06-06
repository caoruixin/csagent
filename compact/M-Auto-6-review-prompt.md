# M-Auto-6 milestone-shared Codex review prompt

**Authored by deliver-agent at C-2b dev-side close, 2026-06-06; updated
at S-Auto-28 dev-side close, 2026-06-07 to add the 6th sub-sprint
(R8 KnowledgeIngestionRunner `--reconcile` data-application path;
M-Auto-6 milestone-close BLOCKER fix surfaced by the §5.9 pre-flight
NO-GO at §0.3).**
**Dispatched after the S-Auto-28 Definition-of-done unblock sequence
completes + the M-Auto-6 milestone-shared §9 real-LLM re-bless evidence
is in.**

This prompt is the **self-contained executable view** of the
milestone-shared Codex review per `process/milestone-framework.md`
§4.3 + `prompt-artifact-rules.md` §9.1/§9.2. The milestone contract
context is embedded verbatim from `docs/milestone_objective.md`; the
§4.1 nine-question kernel is embedded verbatim from
`docs/current/anti-hardcode-review-kernel.md`. The dev handoffs (one
per sub-sprint) are NOT embedded — they are dev outputs produced
before this review and are referenced by path under "Loader".

---

## 1. Role identity

You are the **Anti-Hardcode + Milestone-Close Review Agent for
Milestone M-Auto-6**. Your review covers the cumulative commit range
`6236941..HEAD` (52+ commits at dispatch time — exact commit count
includes the C-2b close-cascade commit, the S-Auto-28 launch + 4 dev
commits + per-sub-sprint Codex review prompt + S-Auto-28 close-cascade
commit, and the subsequent re-bless launch commit) across the **six**
sub-sprints A + B + C-1 + C-2a + C-2b + **S-Auto-28** that comprise
M-Auto-6. The milestone is "Runtime substrate hygiene
at intake + DISCOVER surfaces + admin observability + intake/clarification
contract + UX/corpus governance" — a Tier-1 mechanical wiring +
prompt_projection contract completion + UI-only display additions +
new no-side-effect tool + data field driven retrieval filter +
citation contract literal→shape semantics fix milestone.

**Your verdict is the formal milestone close gate per
`iteration_governance.md` §5.5.** Per-sub-sprint Codex reviews have
already been delivered for A (`APPROVE_S_AUTO_23 / blocking_count=0`
at `62b4d7b`), C-1 (`APPROVE_S_AUTO_25 / blocking_count=0`), C-2a
(`APPROVE_S_AUTO_26 / blocking_count=0` on targeted re-review at the
S-Auto-26 close commit), C-2b (`APPROVE_S_AUTO_27 / blocking_count=0`
on targeted re-review at the S-Auto-27 close commit), and **S-Auto-28**
(`APPROVE_S_AUTO_28 / blocking_count=0` under §4.1 pure-infra scope
exemption at the S-Auto-28 close commit). B was §7-EXEMPT and
visual-verified — no per-sub-sprint
Codex required per `process/milestone-framework.md` §4.3. Your
milestone-shared review covers the cumulative commit range and is
expected to either (i) cumulatively confirm the per-sub-sprint
verdicts under the §4.1 kernel, or (ii) surface cross-sprint
hardcode / regression risks that per-sub-sprint reviews could not
detect.

---

## 2. Loader (minimal)

Read in this order:

1. `AGENTS.md` (auto-loaded; transitively loads the governance chain
   `doc_governance.md` → `agent_context_guide.md` →
   `iteration_governance.md`).
2. This prompt (you are reading it).
3. The per-sub-sprint dev handoffs (dev outputs; NOT embedded; required
   for verifying claims per sub-sprint):
   - `docs/sprints/sprint-078-handoff.md` (S-Auto-23 / A)
   - `docs/sprints/sprint-079-handoff.md` (S-Auto-24 / B)
   - `docs/sprints/sprint-080-handoff.md` (S-Auto-25 / C-1)
   - `docs/sprints/sprint-081-handoff.md` (S-Auto-26 / C-2a)
   - `docs/sprints/sprint-082-handoff.md` (S-Auto-27 / C-2b; includes
     §3.1 fix-iteration addendum)
   - `docs/sprints/sprint-083-handoff.md` (S-Auto-28; R8
     KnowledgeIngestionRunner `--reconcile` data-application path
     fixing the M-Auto-6 milestone-close blocker)

The archived per-sub-sprint **objectives** (dev-side-closed banners
with the gate evidence tables) live alongside the handoffs at
`docs/sprints/sprint-{078,079,080,081,082}-objective.md`. They are
deliver-agent outputs and are referenced for the per-sub-sprint
acceptance status they record.

`docs/codex-findings.md` at dispatch time will be replaced by your
milestone-shared verdict per §4.2 sprint-close header format below;
the per-sub-sprint Codex verdicts are preserved in git history at
the respective per-sub-sprint close commits.

**Do not edit any code or any sub-sprint archive.** Do not re-judge
any §5.6 bad-case manual verdict. Read code for evidence; do not
modify it.

---

## 3. Embedded milestone context (verbatim from `docs/milestone_objective.md`)

### 3.1 Milestone class

**Class:** runtime substrate-hygiene + UI/observability + intake
contract + UX/corpus governance milestone (Tier-1 mechanical wiring +
prompt_projection contract completion + UI-only display additions +
new no-side-effect tool + data field driven retrieval filter +
citation contract literal→shape semantics fix). NOT a semantic
milestone — no bot prompt procedure rewrite, no UC routing, no
escalation posture decision, no judge calibration, no CaseSpec rubric
edit.

R5 (C-2a) rewrites `SkillGuardrailDispatcher.handleMustCiteSource`
from a literal substring check to a structural URL-or-article_id
shape predicate and updates the citation-token wording in
`resolve_faq_grounded_answer.yaml` to point at the new
`display_citation` field; the rewrite **preserves the grounding
floor** (empty / null / plain-English STILL reject) and the wording
change is minimum-edit (citation-token references only, procedure /
role / objective untouched).

**§7 stanza requirement:** REQUIRED at the milestone level + at each
semantic-touching sub-sprint. Layer matrix (post-S-Auto-28 dev-side
close):

| Sub-sprint | Layer mix | §7 stanza required? | Status |
|---|---|---|---|
| A (R1.a + R2.a + R4.a) | `prompt_projection` + `infra` + `skill_state` | ✅ REQUIRED — done; Codex `APPROVE_S_AUTO_23 / blocking_count=0` | DEV-SIDE CLOSED 2026-06-06 |
| B (R3.a + R3.b + R3.c) | `infra` (observability) | ❌ EXEMPT (UI-only; zero semantic surface) | DEV-SIDE CLOSED 2026-06-06 (visual-verified) |
| C-1 (R7 + R2.a#5-ext) | `skill_state` + `infra` + `prompt_projection` (R7 tool schema) | ✅ REQUIRED — done; Codex `APPROVE_S_AUTO_25 / blocking_count=0` (capability-wiring Option-A fence-waiver ACCEPTED) | DEV-SIDE CLOSED 2026-06-06 |
| C-2a (R5 citation contract fix) | `infra` (ResolveArticleTool result + handleMustCiteSource semantics rewrite) + `prompt_projection` (skill yaml citation wording at `:30/:31/~:80/:44`) | ✅ REQUIRED — done; Codex `APPROVE_S_AUTO_26 / blocking_count=0` on targeted re-review | DEV-SIDE CLOSED 2026-06-06 |
| C-2b (R6 corpus eligibility filter) | `infra` (data + entity + V17 migration + ingestion + service filter + tool log) | ✅ REQUIRED — done; Codex `APPROVE_S_AUTO_27 / blocking_count=0` on targeted re-review (both prior P0 blockers resolved by fix-iteration) | DEV-SIDE CLOSED 2026-06-06 |
| **S-Auto-28 (R8 KnowledgeIngestionRunner `--reconcile` data-application path)** | `infra` (server-side knowledge data-application path; standalone `--reconcile` metadata-only mode UPDATEs 3 mutable curation columns on existing rows from JSON; plain `--ingest` byte-unchanged) | ✅ INCLUDED CONSERVATIVELY — done; Codex `APPROVE_S_AUTO_28 / blocking_count=0` **under §4.1 pure-infra scope exemption** (R8 itself is pure infra; downstream surface gates R6's LLM-facing search; Codex invoked the kernel scope exemption and approved) | DEV-SIDE CLOSED 2026-06-07 (M-Auto-6 milestone-close BLOCKER fix) |

### 3.2 Goal

Make the post-M-Auto-5 honest measurement floor converge to a STABLE
autoloop signal at the **intake-UC handover + DISCOVER clarification +
UC-A entity-premise + intake partial-stash + admin observability +
citation + corpus surface** by:

1. Eliminating the LIVE-path runtime wiring defects (R1.a + R2.a + R4.a
   in A; R7 + R2.a#5-ext in C-1) that inject draw-noise independent of
   bot semantic competence.
2. Surfacing structured premise signals (R4.a; depends on OBS-S1
   autoloop after milestone close) so the LLM has the observable state
   it needs to decide whether to challenge unverified premises.
3. Cleaning the admin trace observability surface (R3.a + R3.b + R3.c
   at B) so manual bad-case triage is not blocked by display artefacts.
4. Improving citation UX + filtering known-bad corpus content (R5 at
   C-2a + R6 at C-2b) so the LLM is not surfacing `(temp)` template
   articles unwrapped, and human-facing answers cite URLs rather than
   internal source IDs when both are available.

NOT a goal: bot-capability optimization on any UC surface. Semantic
optimization is explicitly deferred to autoloop AFTER M-Auto-6
(OBS-S1..S7).

### 3.3 Sub-sprint sequence + per-sub-sprint scope summary

**Sub-sprint A — S-Auto-23 / Sprint 078 — R1.a + R2.a + R4.a — DEV-SIDE CLOSED 2026-06-06.**
Code: `a873d18..af44903`. R1.a `request_handover` schema `intake_fields`
slot + per-active-UC required-fields projection iterated from
`IntakeFieldsRegistry`; R2.a #3 DISCOVER counter live-path wiring with
4 AND-guarded structural criteria + #4 `budgets.clarification`
DISCOVER-only projection + #5 phase-aware `mapBudgetToEscalationReason`
re-map; R4.a `customer_context_status` enum + `ad_reference` block. 53
mocked-LLM characterization tests. P2 smoke wiring evidence at
`eval_interactive/results/20260605-105339/`. Codex `APPROVE_S_AUTO_23 /
blocking_count=0` at `62b4d7b`.

**Sub-sprint B — S-Auto-24 / Sprint 079 — R3.a + R3.b + R3.c — DEV-SIDE CLOSED 2026-06-06 (UI-only; §7-EXEMPT).**
Code: `a26ec88..505aca3`. R3.a `SessionList.tsx` StatusBadge full
`BotSession.HandlingState` coverage; R3.b `TraceViewer.tsx` A1-dedup
ToolEvent folding with click-to-expand audit; R3.c
`data-rejection-kind="informational"|"blocking"|"ok"` categoriser +
yellow badge over 4 SkillGuardrailDispatcher constant strings with
SAFER unrecognised-code default = blocking; #4 terminal `handling_state`
+ `escalation_reason` header; #5 ~7 LOC `SessionManager.java`
handling_state-transition INFO logging (non-behavioural; the ONE
permitted backend touch). 10 new UI vitest tests; 0 new Java tests.
Visual verification by deliver-agent confirmed all five items.
Per-sub-sprint Codex SKIPPED per §7-EXEMPT.

**Sub-sprint C-1 — S-Auto-25 / Sprint 080 — R7 + R2.a#5-ext — DEV-SIDE CLOSED 2026-06-06.**
Code: `be1e733..c031786`. R7 path-β `IntakeFieldsMerger.merge(session,
incoming, objectMapper)` extraction with 9 characterization tests
pinning byte-equivalence on the `request_handover` persist path; R7
`UpdateIntakeFieldsTool` no-side-effect tool with free-form
`string → string` map arguments, structural validation only, merges
via the shared helper, emits standard `ToolEvent`; R7
`ContextProjectionBuilder` `tool_schemas` registration with description
referencing `required_intake_fields_for_active_uc`; R2.a#5-ext
`ControlKernel.mapBudgetToEscalationReason` 4-arg overload firing on
`phase=RESOLVE AND IntakeFieldsRegistry.isIntakeUseCase(activeUseCase)
AND isFreeTextActionKey(lastAction)` with anti-误杀 #12 spirit
preserved and no new enum. +30 net Java tests. Capability-wiring
**Option-A fence-waiver ACCEPTED** (deliver-agent 2026-06-06): byte-narrow
`tool-policy.yaml` +6 lines + `resolve_intake_collect_and_handover.yaml`
+4 lines + `SkillLoader.VALID_TOOL_NAMES` +1 string. Codex
`APPROVE_S_AUTO_25 / blocking_count=0`; 5 non-blocking observations.

**Sub-sprint C-2a — S-Auto-26 / Sprint 081 — R5 citation contract fix — DEV-SIDE CLOSED 2026-06-06.**
Code: `d4122c0..5a0ab3d` (intended cumulative range `d4122c0^..5a0ab3d`
— 4 commits). R5 #1 `ResolveArticleTool` additive `display_citation`
field on result body (`source_url` if non-null/non-blank, else
`article_id` fallback; existing fields preserved); R5 #2
`SkillGuardrailDispatcher.handleMustCiteSource` (`:340-384`) rewritten
from literal `userMessage.contains(citeToken)` substring check to
structural shape predicate via two private static final `Pattern`
constants at `:97-108` (URL `https?://\S+` + article_id
`\bka[A-Za-z0-9]{16}\b` derived from real corpus data) + new
`containsAcceptableCiteToken` helper at `:407-411` + trace key rename
`cite_token_field` → `cite_token_validator` with value
`"structural_url_or_article_id"` at `:382-397` (grounding floor
preserved — empty / null / plain-English STILL reject); R5 #3
`resolve_faq_grounded_answer.yaml` citation-token wording at `:30
procedure / :31 grounding_instruction / ~:79 cite phrasing` updated to
point at `display_citation`; `cite_token_field:44` REMOVED CLEANLY.
+10 net Java tests. Two fence expansions both Codex-accepted (#1
ResolveFaqGuardrailsTest fixture update; #2
PhaseEvaluatorResolveSkillIntegrationTest byte-mechanical golden
mirror). Codex `APPROVE_S_AUTO_26 / blocking_count=0` on targeted
re-review (procedural REJECT on clean-tree gate resolved by `8a7cb66`;
substantive verdict PASS throughout; targeted re-review prompt at
`54b8729` flipped verdict header + §4 only with §1–§5 PASS preserved
verbatim; close commit `6570ec2`).

**Sub-sprint C-2b — S-Auto-27 / Sprint 082 — R6 corpus eligibility filter — DEV-SIDE CLOSED 2026-06-06.**
5 delivery commits `bb48aa0..7773c92` + 3 fix-iteration commits
`e6aad78` + `d27b824` + `4c8931f`. R6 #1 JSON flip
`search_knowledge_eligible: true → false` on exactly 2 `(temp)` template
articles `ka41r000000LIEJAA4` + `ka41r000000LIEEAA4` (216 others retain
`true`; no other field touched). R6 #2
`KbArticle.searchKnowledgeEligible` primitive `boolean` +
`@Builder.Default=true` + `@Column(... nullable=false)`. R6 #3
`V17__add_kb_search_knowledge_eligible.sql` ONE `ADD COLUMN ... BOOLEAN
NOT NULL DEFAULT TRUE` mirroring existing V4 `is_published` pattern;
content-neutral comment post-`e6aad78`. R6 #4
`KnowledgeIngestionRunner.buildKbArticleFromJson` parses via existing
`path("search_knowledge_eligible").asBoolean(true)` `published_status`
idiom. R6 #5 `KnowledgeSearchService:223-246` adjacent eligibility
check inside the existing Step-8 published-defense lambda; operational
predicate is only `!article.isSearchKnowledgeEligible()`; score, rerank,
ordering, result-limit placement, hit construction unchanged. R6 #6
`KnowledgeHit.java` **SKIPPED** (post-filter eligible-by-construction;
rationale documented). R6 #7 INFO log path α at
`KnowledgeSearchService:240-245` per filtered article;
`SearchKnowledgeTool.java` byte-untouched. R6 #8 direct resolve
invariant preserved at `ResolveArticleTool.java:66-81`; both flagged IDs
test-pinned at `ResolveArticleToolTest:239-272` + `:275-303` (the second
test added by fix-iteration `d27b824`). +10 net Java tests across
`KbArticleEligibilityCorpusTest` (NEW) + `KnowledgeIngestionRunnerTest`
+ `KnowledgeSearchServiceTest` + `ResolveArticleToolTest`. Range
`bb48aa0^..4c8931f` contains 11 Git commits (8 substantive + 3
acknowledged audit/package). Codex `APPROVE_S_AUTO_27 /
blocking_count=0` on targeted re-review (initial Codex verdict at
`1954cb6` was `APPROVE_S_AUTO_27_WITH_FIXES / blocking_count=2` on two
infra-hygiene gaps — both closed by fix-iteration; targeted re-review
prompt at `a7c5b6f` flipped verdict header + §4 only with §1–§3 + §5
substantive content reflecting post-fix state).

**Sub-sprint S-Auto-28 — Sprint 083 — R8 KnowledgeIngestionRunner `--reconcile` data-application path — DEV-SIDE CLOSED 2026-06-07 (M-Auto-6 milestone-close BLOCKER fix).**
Promoted 2026-06-07 ahead of the milestone-shared re-bless because the
§5.9 pre-flight runbook returned **NO-GO at §0.3** on 2026-06-07
(recorded at `docs/diagnostics/2026-06-07-m-auto-6-preflight-verdict.md`):
the populated dev DB's 2 `(temp)` template rows still read
`search_knowledge_eligible=true` after R6's JSON flip was committed,
because the insert-only `KnowledgeIngestionRunner` skips every
already-present `article_id` and never UPDATEs. R6's close gates
(`BEGIN…ROLLBACK` migration-mechanics test + synthetic-article unit
tests) did not catch this populated-DB regression; the pre-flight
runbook did, exactly as the §5.9 gate was designed to. R8 is the
structural fix per the blocker brief at
`docs/diagnostics/failure-briefs/preflight-2026-06-07-kb-ingest-skip-existing-blocks-r6-flag.md`.
Code: 4 commits `ba3defa..78ae614` (R8 #1+#2+#3 metadata-only
reconcile + observability + `KnowledgeIngestionReconcileTest` 9 tests
covering #4(a)+#4(b); R8 #4(c) `KnowledgeReconcileEndToEndTest` R6
end-to-end wiring at mock level; R8 #5 5 runbook drift fixes + §0.3/A3
root-cause annotation — docs-only separate commit `97d7801`; R8 #6
dev handoff). Canonical invocation pinned to **standalone
`--reconcile`** (deliver-agent decision 2026-06-07; rationale: plain
`--ingest` keeps insert-only semantics, `--reconcile` signals
metadata-only update semantics, the two flags name different modes
not the same mode with a flag stack; `--ingest --reconcile` combo
non-canonical). Runner-entry gate widened to
`if (!ingest && !reconcile) return;` at
`KnowledgeIngestionRunner.java:78-85`; per-article branch on
`reconcile` at `:161-174`; `reconcileExisting(...)` at `:216-261`
loads the existing managed `KbArticle`, sets ONLY the 3 mutable
curation columns (`searchKnowledgeEligible` / `isPublished` /
`ucTags`) from JSON via the existing `path(...).asBoolean(true)` /
`extractUcTagsStatic` idioms, bumps `updatedAt`, calls `save()`;
saves+counts ONLY when ≥1 curation column actually changed (so
`articlesReconciled` reports rows whose curation MOVED — expected 2
on the live corpus). Content columns (`title` / `summary` /
`description` / `source_url` / `url_category` / `token_count`) and
the embedding pipeline (`chunkingService` / `embeddingClient` /
`kbChunkRepository`) NEVER touched on reconcile. Plain `--ingest`
byte-for-byte unchanged (insert-only skip-existing). New IDs under
`--reconcile` still fall through to insert+embed. Per-row INFO at
`:258-259` + end-of-run summary INFO at `:115-123`. Pre-fix audit (3
STOPs cleared BEFORE any code): `save()` is full-row UPDATE on
existing-id entity (no `@DynamicUpdate` → load-then-modify mandatory);
mutable curation columns confirmed; no JPA cascade to `kb_chunks`
(chunks referenced via plain `String articleId`, no
`@ManyToOne`/`@OneToMany`). Java baseline `1358/1/0/2` (+10 net tests
across `KnowledgeIngestionReconcileTest` 9 tests + `KnowledgeReconcileEndToEndTest`
1 test; sole failure = inherited OQ-S41.5, provably uncoupled).
Focused 5-suite re-review tests `31/0/0/0`. Per-sub-sprint Codex
`APPROVE_S_AUTO_28 / blocking_count=0` **under §4.1 pure-infra scope
exemption**: §1 R8 #1-#6 PASS; §2 Q1-Q9 PASS aggregate `approve`; §3
F1-F6 PASS (Codex independently verified mock-interaction counts via
`verify(..., never())`; re-ran `git grep` returning 0
`ka41r…`/`(temp)` under `server/src/main`; confirmed before/after
blob hashes IDENTICAL for `KnowledgeSearchService.java`,
`KbArticle.java`, V17, `knowledge_base_articles.json`,
`docs/current_eval_baseline.md`, `autoloop/config.yaml`). 3
non-blocking observations: NBO #1 standalone entry-gate test gap —
queued as `R-standalone-reconcile-entry-gate-test` for S-Auto-29+
infra-test pickup; NBO #2 post-APPROVE evidence assertions reminder
for the deliver-agent + human Definition-of-done unblock sequence;
NBO #3 independent review verification passed.
**Real-DB §0.3 evidence DEFERRED post-Codex per §5.7** (live
`--reconcile` run + §0.3 re-check require backend rebuild + mutate
shared dev DB; sequenced as the Definition-of-done unblock action).
Anti-误杀 #7 forbids manual SQL UPDATE as the §0.3 evidence — the DB
state MUST be produced by the `--reconcile` run.

### 3.4 Non-goals (explicit)

- **No semantic procedure edits.** No bot prompt rewrite, no skill
  yaml procedure-step / wording rewrite, no UC routing cue, no
  escalation posture decision, no judge calibration, no CaseSpec
  rubric. OBS-S1..S7 are ALL deferred to autoloop AFTER M-Auto-6.
  - **Exception:** R5 (C-2a) updates the citation-token wording in
    `resolve_faq_grounded_answer.yaml` at `:30 procedure + :31
    grounding_instruction + ~:80 cite phrasing + :44 cite_token_field`
    to point at the new `display_citation` field returned by
    `ResolveArticleTool`. This is a citation-token reference change,
    minimum-edit diff (goal / role / objective / search-must-precede /
    faq_miss escalation rule byte-untouched). The contemporaneous
    `SkillGuardrailDispatcher.handleMustCiteSource` rewrite is a
    guardrail semantics fix (literal substring → structural
    URL/article_id shape predicate) — a grounding-floor false-negative
    correction, not a procedure edit. Both permitted per the milestone
    class §1.
- **No `IntakeFieldsRegistry.java:53-67` field-definition edits.** R1.a
  projects the existing required-fields list as-is.
- **No new `escalation_reason` enum value.** R2.a + R2.a#5-ext re-map
  `max-repeated-same-action` to the existing
  `clarification_budget_exhausted` value, no enum widening.
- **No identical-clarification cross-turn semantic dedup.** R2.a is
  cardinality-only. M-Auto-3 §11 anti-误杀 rank-1 floor preserved.
- **No `SkillGuardrailDispatcher` reject-logic edits** EXCEPT the
  scoped `handleMustCiteSource` rewrite at `:340-384` in C-2a.
  Validator remains the last line of defence on every other handler;
  R1.a only adds the LLM-facing contract; R7 adds a NEW no-side-effect
  tool that does NOT bypass the validator on the handover path.
- **No `record_outcome` premature guard edits** (OBS-S4 by-design). R3.c
  distinguishes informational guard rejections from blocking errors at
  the UI display surface ONLY; the guard at
  `ResolveDispositionEvaluator.java:160-186` is unchanged.
- **No corpus article deletion.** R6 (C-2b) flips the existing
  `search_knowledge_eligible` data field on 2 `(temp)` articles; the
  articles remain in corpus + DB + retrievable via
  `ResolveArticleTool.byArticleId` for human CS use; only the
  LLM-facing search-knowledge surface is filtered.
- **No `must_cite_source` guardrail logic change OTHER than the C-2a
  `handleMustCiteSource` rewrite** (literal substring → structural
  URL/article_id shape; grounding floor preserved). The article_id-shape
  regex MUST be derived from real corpus data (NOT invented).
- **No simulator / eval-framework / scoring SHA / autoloop 5-file set
  edits.** Fence-#13 SHA respected.
- **No M-Auto-4 S-Auto-18 (escalation-family tier reshape) work.**
- **No per-sub-sprint outcome-evidence re-bless.** Outcome evidence
  consolidates at the milestone-shared re-bless after A + B + C-1 +
  C-2a + C-2b land. `baseline_dir` + `docs/current_eval_baseline.md` do
  NOT flip until milestone close (THIS review).

### 3.5 Milestone acceptance bar (falsifiable hypothesis)

R1+R2+R4 (shipped at A) + R7 + R2.a#5-ext + R5 + R6 + R3.* are
collectively the dominant residual noise + UX + observability gap on
intake / DISCOVER / UC-A entity-premise / citation / corpus / admin
surfaces after M-Auto-5. If true, after the milestone-shared real-LLM
re-bless runs on the corrected runtime, the following should observably
move (paired-evidence against
`m-auto-5-baseline-20260604-simfixed-stalledfix`):

| Signal | Pre-M-Auto-6 baseline | Post-milestone prediction | Falsifier |
|---|---|---|---|
| `bad_cases` `reducible-flaky` count | 6/12 | ≤ 2/12 | Stays > 2/12 and remaining flaky cases hit intake + DISCOVER + UC-A entity surfaces → re-diagnose. |
| anchor uc_f_billing pass_rate | 0.89 (reducible-flaky) | Toward stable ~1.00 | Drifts ≤ 0.78 → R1 was not the residual noise here. |
| anchor uc_fp_removed pass_rate | 0.89 (reducible-flaky) | Toward stable ~1.00 | Same. |
| Intake-UC first-call rejection rate (UC-G / UC-H / UC-I / UC-J / UC-K) | High pre-A (c1/c5/c6 pattern) | 0 post-A | Milestone re-bless confirms at corpus scale. |
| DISCOVER clarification cap-hit count | 0 (counter never incremented) | > 0 on c3/c9-like cases | Counter increments live (A smoke evidence). |
| `clarification_budget_exhausted` escalation_reason occurrences | 0 (mapping fall-through stamps `turn_budget_exhausted`) | > 0 on DISCOVER free-text repeats AND C-1-after RESOLVE-intake free-text repeats | Stays 0 → R2.a #5 or R2.a#5-ext mapping wiring did not take. |
| `turn_budget_exhausted` occurrences attributable to RESOLVE-intake clarification | n=1 known (c14) | 0 post-R2.a#5-ext | R2.a#5-ext mapping check. |
| c14-class intake partial-stash behaviour | Cannot accumulate without handover triggering validator | LLM uses `update_intake_fields` to accumulate; handover only when all required fields collected | R7 wiring check. Real-LLM adoption depends on LLM — wiring evidence (schema declared + tool dispatch works + validator unchanged) is the close gate. |
| Citation token in user-facing replies | `(Source: ka4P200000003sLIAQ)`-style source_id | `(Source: <canonical_url>)` when article has URL; `source_id` fallback for the 38 URL-less articles | R5 wiring check. |
| `(temp)` template article occurrences in `search_knowledge` results visible to LLM | 2 known articles surface in c7/c12/c17 | 0 post-R6 | R6 retrieval filter check; corpus content itself unchanged. |
| Admin trace UI: c7 admin missing case + c8 dedup confusion + c2/c10/c17 informational-vs-blocking | All present pre-B | Resolved at UI surface via R3.a + R3.b + R3.c; underlying trace data unchanged | Visual verification at B close; no eval impact. |
| Anti-误杀 (persistent high-risk cases) | 0.000 stable on anchor uc_g_gdpr / uc_h_appeal / uc_i_payment / uc_j_safety + shadow cs38s* | UNCHANGED at 0.000 stable | Any rise (an artifact mis-passes) → reject the change as masking. |

**Hard close gates** (per `iteration_governance.md` §5.5 +
`process/badcase-lifecycle.md` §5.6):

- Codex §4.1 nine-question kernel pass at milestone close (THIS review).
- Java test suite no new regression vs the documented inherited
  baseline. Java baseline at C-2b dev-side close: `1348/1/0/2` (sole
  failure = inherited OQ-S41.5, provably uncoupled).
- Safety floor unchanged (anti-误杀 listed above).
- Grounding floor unchanged (R5 guardrail rewrite preserves the
  grounding contract: empty / null / plain-English STILL reject; URL or
  article_id shape PASS).
- Curated bad-case suite manual review (primary gate) at milestone
  close — paired-evidence review against
  `m-auto-5-baseline-20260604-simfixed-stalledfix`. **NOTE:** this is a
  human + deliver-agent gate; Codex does NOT re-judge the bad-case
  verdicts.

### 3.6 Hard fences (milestone-level)

1. NO bot semantic / prompt rewrite / UC-hypothesis / escalation
   posture / skill yaml procedure-step / CaseSpec edit anywhere in
   M-Auto-6. **Exception**: R5 (C-2a) updates the citation-token
   wording at `resolve_faq_grounded_answer.yaml:30 procedure + :31
   grounding_instruction + ~:80 cite phrasing + :44 cite_token_field`
   to point at the new `display_citation` field — minimum-edit diff.
2. NO `IntakeFieldsRegistry` content changes; R1.a projects existing
   contract as-is.
3. NO new `escalation_reason` enum values; R2.a + R2.a#5-ext re-map to
   existing `clarification_budget_exhausted`.
4. NO identical-clarification cross-turn content/semantic dedup; R2.a
   is cardinality-only.
5. NO `SkillGuardrailDispatcher` reject-logic edits EXCEPT C-2a's
   `handleMustCiteSource` rewrite at `:340-384` (literal substring →
   structural URL/article_id shape; grounding floor preserved).
6. NO `ResolveDispositionEvaluator` reject-logic edits. R3.c is a UI
   display distinction over CORRECT §1.4 guard rejections.
7. NO corpus article deletion. R6 (C-2b) flips the existing
   `search_knowledge_eligible` data field on 2 `(temp)` articles;
   articles retained in corpus + DB for human CS use; direct resolve
   via `ResolveArticleTool.byArticleId` unfiltered.
8. NO `must_cite_source` guardrail logic change OTHER than the C-2a
   `handleMustCiteSource` rewrite. The article_id-shape regex MUST be
   derived from real corpus data (NOT invented).
9. NO simulator / eval-framework / scoring SHA / autoloop 5-file set
   edits. Fence-#13 SHA respected.
10. NO M-Auto-4 S-Auto-18 work.
11. NO autoloop semantic optimization sub-sprint until M-Auto-6 close.
    OBS-S6 additionally gated on R7 ship (which lands in C-1).
12. NO per-sub-sprint outcome-evidence re-bless. `baseline_dir` +
    `docs/current_eval_baseline.md` do NOT flip until milestone close.
13. NO `BotSession.HandlingState` enum value additions (R3.a renders
    existing values only).

---

## 4. Embedded §4.1 nine-question anti-hardcode kernel (verbatim from `docs/current/anti-hardcode-review-kernel.md`)

```text
You are the Anti-Hardcode Review Agent. The PR below proposes a change
to this repo. Your job is to decide whether the change introduces a
semantic hardcode — a keyword / regex / if-else / enum / per-UC matrix
that encodes a decision the LLM is supposed to own under
docs/current/iteration_governance.md §1.3 — and to issue a verdict.

Scope exemption: pure infra, docs-only, config-governance, and
characterization-test PRs are not subject to this review. If the PR
is purely one of those, return `approve` with a one-line note naming
the exemption.

For every other PR, walk these nine questions in order. For each
"yes" or each concern, paste the diff snippet and the reasoning.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix
   for a semantic decision (drift detection, escalation, UC
   selection, risk classification, follow-up, intake routing)?
2. If yes to (1), is the change justified as protecting a current
   Tier-0 invariant named in docs/runtime_freeze_and_risk_policy.md
   §1 / §2?
3. Could the same outcome be achieved by projecting a soft signal to
   the LLM (an additional projected slot, a candidate list, a
   diagnostic flag) instead of a hard branch in Java or the prompt?
4. Does the change encode visible-eval case text, trace-specific
   phrasing, or a CaseSpec id into runtime, prompt, or judge config?
5. Does the change move semantic ownership from the LLM to Java —
   that is, shrink what docs/current/iteration_governance.md §1.3
   says the LLM owns?
6. Does the change add an if-else block to the prompt instead of
   principle-level or observable-state guidance?
7. Does the change preserve tool schema, capability / permission
   boundary, PII / safety floor, and grounding floor?
8. Does the PR ship generalization eval coverage — target, neighbor,
   negative, and shadow cases — and not only the target case?
9. If the change is temporary, does it carry an explicit rollback or
   sunset plan (downgrade-to-signal trigger, retirement sprint id)?

Return exactly one verdict:

- `approve` — the change is not a semantic hardcode, or is justified
  as protecting a current Tier-0 invariant with adequate generalization
  coverage and a clear rollback if temporary.
- `approve with downgrade-to-signal follow-up` — the change is
  acceptable as an interim measure, but a follow-up sprint must
  convert it into a soft signal projected to the LLM. Name the
  trigger that should fire the conversion.
- `reject as semantic hardcode` — the change encodes a soft semantic
  decision the LLM should own; questions 1 and 2 fail, or questions
  5 / 6 fail, with no Tier-0 claim and no sunset plan.
- `needs human architecture decision` — the change crosses an
  unresolved governance question (new escalation reason enum value,
  new Tier-0 candidate, LLM-vs-Java boundary shift, new tool surface)
  and a human reviewer must decide before merge.

Do not rewrite the PR. Do not propose a code fix beyond naming the
layer in docs/current/iteration_governance.md §3 that the fix should
target.
```

---

## 5. Cumulative scope claim (the milestone diff Codex is reviewing)

**Cumulative commit range:** `6236941..HEAD` at dispatch time. `6236941`
is the M-Auto-5 milestone close commit; HEAD is the M-Auto-6 close
commit. Expected commit count: ~46-48 (43 commits from the start of
M-Auto-6 contract work at `a79be7c` through the C-2b dev-side close +
the close-cascade commit being committed alongside this prompt, plus
4-5 contract / planning / proposal commits prior to A's code work).

### 5.1 Per-sub-sprint commit ranges (substantive code + handoffs)

| Sub-sprint | Commit range | Substantive count | Notes |
|---|---|---|---|
| Pre-A (contracts + proposal) | `6578403..af44903`-anchor | 4-5 | Includes M-Auto-6 open `a79be7c`, contract status flips, proposal record, cadence transition `390e9d8`. |
| A — Sprint 078 / S-Auto-23 | `a873d18..af44903` | 4 dev + 1 handoff + 1 Codex finding `62b4d7b` | R1.a + R2.a + R4.a + dev handoff. |
| B — Sprint 079 / S-Auto-24 | `a26ec88..505aca3` | code commits + dev handoff | UI-only; §7-EXEMPT. |
| C-1 — Sprint 080 / S-Auto-25 | `be1e733..c031786` (intended `be1e733^..c031786`) | 4 dev + dev handoff + Codex finding | R7 + R2.a#5-ext + dev handoff. |
| C-2a — Sprint 081 / S-Auto-26 | `d4122c0..5a0ab3d` (intended `d4122c0^..5a0ab3d`) + `8a7cb66` (out-of-scope context-pack archive) + `221432d` (substantive REJECT audit) + `54b8729` (targeted re-review prompt) + close commit `6570ec2` | 4 dev + 1 handoff + 4 procedural | R5 #1 + R5 #2 + R5 #3 + dev handoff. |
| C-2b — Sprint 082 / S-Auto-27 | `bb48aa0..7773c92` (5 delivery) + `e6aad78` + `d27b824` + `4c8931f` (3 fix-iteration) + `056fa5a` + `3300b4a` + `1954cb6` + `a7c5b6f` + close commit | 5 delivery + 3 fix-iteration + 4 audit/package | R6 #1–#8 + dev handoff + fix-iteration addendum. |
| **S-Auto-28 — Sprint 083** | `ba3defa..78ae614` (4 commits: R8 #1+#2+#3 + #4(a/b) at `ba3defa`; R8 #4(c) at `7ae61d2`; R8 #5 docs-only at `97d7801`; R8 #6 handoff at `78ae614`) + `0d15c69` launch + `29425f4` framework precursor + `feb3419` per-sub-sprint review prompt + close commit | 4 substantive + 4 audit/package | R8 KnowledgeIngestionRunner `--reconcile` metadata-only data-application path (M-Auto-6 milestone-close BLOCKER fix). |

Per-sub-sprint Codex verdicts:
- A: `APPROVE_S_AUTO_23 / blocking_count=0` at `62b4d7b`.
- B: SKIPPED per §7-EXEMPT; visual verification by deliver-agent.
- C-1: `APPROVE_S_AUTO_25 / blocking_count=0`; capability-wiring
  Option-A fence-waiver ACCEPTED.
- C-2a: `APPROVE_S_AUTO_26 / blocking_count=0` on targeted re-review;
  procedural REJECT on clean-tree gate resolved by `8a7cb66`;
  substantive verdict PASS throughout (§1 / §2 / §3 / §5 verbatim).
- C-2b: `APPROVE_S_AUTO_27 / blocking_count=0` on targeted re-review;
  initial verdict at `1954cb6` was `APPROVE_S_AUTO_27_WITH_FIXES /
  blocking_count=2` on two infra-hygiene gaps (Codex §4 #1 F2/Q8
  second direct-resolve test missing for `ka41r000000LIEEAA4` + Codex
  §4 #2 V17 SQL line-4 `(temp)` literal in comment tripping F3
  forbidden-grep gate; SQL operation byte-unchanged); both closed by
  fix-iteration `e6aad78` + `d27b824` + `4c8931f`.
- **S-Auto-28**: `APPROVE_S_AUTO_28 / blocking_count=0` under §4.1
  pure-infra scope exemption. §1 R8 #1-#6 PASS; §2 Q1-Q9 PASS aggregate
  `approve` (scope-exemption invoked); §3 F1-F6 PASS (Codex independently
  verified mock-interaction counts + re-ran `git grep` returning 0
  `ka41r…`/`(temp)` under `server/src/main` + confirmed before/after
  blob hashes IDENTICAL for `KnowledgeSearchService.java`,
  `KbArticle.java`, V17, `knowledge_base_articles.json`,
  `docs/current_eval_baseline.md`, `autoloop/config.yaml`). 3 NBOs (#1
  standalone entry-gate test gap queued as
  `R-standalone-reconcile-entry-gate-test`; #2 post-APPROVE evidence
  assertions reminder; #3 independent review verification 31/0/0/0 +
  diff-check clean).

### 5.2 Java baseline progression

- M-Auto-5 close baseline: not directly recorded for M-Auto-6 review
  purposes; the relevant pre-M-Auto-6 baseline is whatever Java state
  existed before A. The A launch baseline was implicitly the
  pre-existing inherited count (sole failure = inherited
  `SystemPromptUserRequestedTiebreakerTest` OQ-S41.5, provably
  uncoupled — system-prompt content surface; none of the M-Auto-6
  sub-sprints touched prompt files).
- A close: 53 new mocked-LLM characterization tests.
- B close: 10 new UI vitest tests; 0 new Java tests (UI-only;
  diagnostic logging non-behavioural).
- C-1 close: `1327/1/0/2` (+30 net Java tests).
- C-2a close: `1337/1/0/2` (+10 net Java tests).
- C-2b close: `1348/1/0/2` (+10 net Java tests).
- **S-Auto-28 close: `1358/1/0/2`** (+10 net Java tests across
  `KnowledgeIngestionReconcileTest` 9 + `KnowledgeReconcileEndToEndTest`
  1).
- **Java baseline at this milestone-shared review: `1358/1/0/2`**;
  sole failure = inherited OQ-S41.5.
- UI vitest baseline at this milestone-shared review:
  `10 passed / 0 failed / 0 skipped` (vitest + @testing-library/react +
  jsdom; harness bootstrap committed at `a26ec88` per OQ-S79.1).
- Python pytest baselines UNCHANGED: autoloop `324`; eval_interactive
  `553`; 17-fixture `31`.

### 5.3 Milestone-shared real-LLM re-bless evidence (dispatched-time placeholder)

At dispatch time the milestone-shared real-LLM re-bless will have run
against `m-auto-5-baseline-20260604-simfixed-stalledfix` and produced
results at `eval_interactive/results/m-auto-6-baseline-shared-YYYYMMDD/`
(`--n 9`, multi-suite — bad_cases + anchor_outcome + shadow). The
paired-evidence comparison against the M-Auto-5 baseline is the
**primary §5.6 acceptance gate** and is judged by human + deliver-agent
PRIOR to dispatching this Codex review. The deliver-agent will record
the bad-case verdict + the falsifiable-hypothesis row-by-row
disposition in `docs/sprints/sprint-082-handoff.md` §3.2 or a
dedicated milestone-close handoff appendix before this review is sent.

**Codex does NOT re-judge the §5.6 bad-case verdicts.** Codex reviews
the cumulative commit diff for semantic-hardcode + regression risks
under the §4.1 kernel. The bad-case verdict and the falsifiable
hypothesis outcomes are inputs to your verdict's scope-and-evidence
audit, not outputs of your review.

---

## 6. Output format — write your verdict to `docs/codex-findings.md` using this §4.2 sprint-close header verbatim

Per `iteration_governance.md` §4.2, write the following header verbatim
to the **top** of `docs/codex-findings.md`, replacing any per-sub-sprint
content that lives there at dispatch time (the per-sub-sprint Codex
verdicts are preserved in git history at the respective close commits;
they are NOT lost by your overwrite):

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph naming the milestone-level finding and the §5
  acceptance-bar disposition. If `pass`, state the cumulative kernel
  walk-through outcome and the milestone-close route (a / b / c per
  `docs/milestone_objective.md` §5). If `fix_required`, name the P0/P1
  findings + the layer per `iteration_governance.md` §3. If
  `out_of_scope_review`, name the scope drift.>
final_verdict: APPROVE_M_AUTO_6 | APPROVE_M_AUTO_6_WITH_NON_BLOCKING_OBSERVATIONS | APPROVE_M_AUTO_6_WITH_FIXES | REJECT_M_AUTO_6
```

Below the header, structure your findings in this order:

### §1 Cumulative §4.1 kernel walk-through (across the milestone diff)

Walk Q1–Q9 once cumulatively across the cumulative commit range. For
each `yes` or each concern, paste the diff snippet (file:line range
anchored) and the reasoning. Aggregate verdict (`approve` / `approve
with downgrade-to-signal follow-up` / `reject as semantic hardcode` /
`needs human architecture decision`) per the kernel's verdict set.
Cumulative pass requires per-sub-sprint pass already preserved + no
cross-sprint interaction surfacing a new hardcode.

### §2 Per-sub-sprint verdict reconciliation

For each sub-sprint (A / B / C-1 / C-2a / C-2b) cite the per-sub-sprint
Codex verdict (or the §7-EXEMPT visual-verification status for B), and
confirm or rebut at the milestone-shared level. State if any
per-sub-sprint NBO becomes blocking under cross-sprint interaction.

### §3 Milestone-level focal-point verdicts

State at least these focal-point verdicts (numbered F1–F6, anchored to
the §3.1 milestone-class concerns):

- **F1** — Grounding floor preserved (R5 guardrail rewrite + R6 corpus
  filter don't introduce false-positive over the grounding contract).
- **F2** — Anti-误杀 floor preserved (anchor uc_g_gdpr / uc_h_appeal /
  uc_i_payment / uc_j_safety + shadow cs38s* unchanged; verified at the
  milestone-shared re-bless).
- **F3** — Cross-sub-sprint hardcode-introduction check (no
  cross-sprint combination produces a semantic hardcode that any single
  per-sub-sprint review missed).
- **F4** — Capability-wiring fence-waiver acceptance (C-1) does not
  introduce capability drift over the cumulative range.
- **F5** — Citation contract (C-2a) preserves both the grounding floor
  AND the URL-less-article path (38 articles without `source_url`
  still cite article_id via the structural-shape predicate).
- **F6** — Corpus eligibility filter (C-2b) is data-field-driven only
  (no content scan; no title-keyword; no hardcoded article_id in Java)
  AND direct resolve preserved for both flagged IDs (anti-误杀 #1).

### §4 Blocking findings (or "None")

If any P0/P1 finding surfaces, name it + the layer per
`iteration_governance.md` §3 the fix should target. Otherwise: "None."

### §5 Non-blocking observations

Process learnings, cross-sprint observations, queued R-items for
S-Auto-28+. NBOs do not block the milestone close.

---

## 7. Constraints

- You may NOT edit any code under `server/`, `ui/`, `eval/`, `eval_interactive/`,
  `autoloop/`, `data/`, or `scripts/`.
- You may NOT edit any sub-sprint archive
  (`docs/sprints/sprint-{078,079,080,081,082}-*.md`).
- You may NOT edit any milestone archive (`docs/milestones/M-Auto-*.md`).
- You may NOT edit `docs/sprint_objective.md` or
  `docs/milestone_objective.md`.
- You MAY edit `docs/codex-findings.md` (to write your verdict per §6
  output format).
- You may NOT re-judge any §5.6 bad-case manual verdict. The
  paired-evidence bad-case verdict is a human + deliver-agent decision
  per `process/badcase-lifecycle.md` §5.6; you take it as input to
  your scope/evidence audit, not as something you adjudicate.
- You may read code for evidence; you may NOT modify it.
- Anti-误杀 invariants are HARD: any rise in persistent-high-risk-case
  pass-rate (anchor uc_g_gdpr / uc_h_appeal / uc_i_payment /
  uc_j_safety + shadow cs38s* away from 0.000 stable) caused by an
  M-Auto-6 change is a `reject` trigger, not an `approve with
  observations`. The bad-case manual verdict will confirm this state
  before dispatch.

### Per-sub-sprint Codex review trigger list (for reference; embedded per `process/milestone-framework.md` §4.3 OPTIONAL clause)

Per `process/milestone-framework.md` §4.3, per-sub-sprint Codex review
is REQUIRED when:

- Sub-sprint touches a semantic surface (prompt, runtime semantic
  decision, eval spec, judge calibration, new keyword/regex/enum
  influencing routing or escalation).
- Sub-sprint introduces a new tool schema (LLM-facing).
- Sub-sprint changes a guardrail / safety / grounding floor.
- Sub-sprint touches a capability boundary (tool-policy.yaml,
  SkillLoader.VALID_TOOL_NAMES, skill tools_required).

Per-sub-sprint Codex review is OPTIONAL (covered cumulatively at
milestone close) when:

- Sub-sprint is UI-only with no semantic surface touched (B).
- Sub-sprint is pure infra / docs-only / config-governance /
  characterization-test (per §4.1 scope exemption).
- Sub-sprint adds only diagnostic logging that is non-behavioural and
  ≤ ~10 LOC (B's #5).

In M-Auto-6 the trigger fired for A + C-1 + C-2a + C-2b (per-sub-sprint
Codex required and delivered); B was §7-EXEMPT and Codex SKIPPED.

---

## 8. Reading order (cold start, after this prompt)

1. This prompt (you are reading it).
2. `AGENTS.md` (auto-loaded; transitively loads governance chain).
3. The five sub-sprint dev handoffs at
   `docs/sprints/sprint-{078,079,080,081,082}-handoff.md`.
4. The five sub-sprint archived objectives at
   `docs/sprints/sprint-{078,079,080,081,082}-objective.md` (for the
   dev-side-close banner tables that record the per-sub-sprint gate
   evidence).
5. The dispatch-time milestone-shared re-bless results at
   `eval_interactive/results/m-auto-6-baseline-shared-YYYYMMDD/`
   (provided as a path in the dispatch message; the human +
   deliver-agent will have already recorded the bad-case verdict +
   paired-evidence disposition in a milestone-close handoff appendix).
6. The cumulative diff via `git log --oneline 6236941..HEAD` +
   `git diff 6236941..HEAD -- <surface>` for surfaces named in §3 /
   §4 / §6 above.
7. `docs/runtime_freeze_and_risk_policy.md` §1 / §2 for Tier-0
   invariant claims under Q2 of the §4.1 kernel.
8. `docs/current/anti-hardcode-review-kernel.md` for the canonical
   §4.1 kernel (the kernel embedded in §4 above is the canonical
   2026-05-25 version; the canonical-copy file is the source of truth
   if you suspect drift).

When ready, write your verdict per §6 output format to
`docs/codex-findings.md`. Surface your verdict at the top of the file
with the §4.2 header verbatim.
