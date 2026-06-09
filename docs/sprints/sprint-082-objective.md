---
title: Sub-sprint C-2b — S-Auto-27 / Sprint 082 — R6 corpus eligibility filter (reuse existing `search_knowledge_eligible` field) — ARCHIVED (dev-side closed; Codex-approved on targeted re-review; both prior P0 blockers resolved by fix-iteration; milestone evidence deferred)
doc_tier: sprint-archive
status: archived
implementation_status: partial
source_of_truth: this file (archived contract) + docs/sprints/sprint-082-handoff.md (dev handoff, including §3.1 fix-iteration addendum) + docs/codex-findings.md commit-at-close (Codex APPROVE_S_AUTO_27 on targeted re-review) + docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md §4.6 (R6; updated re-scope)
last_reviewed: 2026-06-06
review_cadence: archived
supersedes: docs/sprints/sprint-081-objective.md
superseded_by: null
notes: >
  Archived at S-Auto-27 dev-side close 2026-06-06 after Codex per-sub-sprint
  TARGETED re-review returned `APPROVE_S_AUTO_27 / blocking_count=0`. The
  initial Codex review (commit `1954cb6` audit trail) returned
  `APPROVE_S_AUTO_27_WITH_FIXES / blocking_count=2` on two infra-hygiene gaps
  (Codex §4 finding #1: only the first flagged article `ka41r000000LIEJAA4`
  had a direct-resolve invariant test, not `ka41r000000LIEEAA4` — F2/Q8
  evidence gap; §4 finding #2: V17 SQL line-4 carried a `(temp)` literal in
  the comment that tripped the F3 forbidden-grep gate, despite the SQL
  operation being content-neutral). Both findings were infra hygiene gaps,
  not executable semantic hardcode. Fix-iteration commits:
  - `e6aad78` — V17 SQL line-4 comment rewritten to content-neutral
    "CS-only placeholder template articles"; the SQL operation
    (`ADD COLUMN search_knowledge_eligible BOOLEAN NOT NULL DEFAULT TRUE`)
    byte-unchanged; `server/src/main` `(temp)` grep now clean.
  - `d27b824` — added `execute_searchIneligibleArticle_stillResolvesByDirectId_secondTemplate`
    in `ResolveArticleToolTest.java:275-303` pinning direct resolve on
    `ka41r000000LIEEAA4` with `safe_to_show=true`, mirroring the existing
    first-template test for `ka41r000000LIEJAA4` at `:239-272`. Both
    flagged articles now test-pinned for F2 direct-resolve invariant.
  - `4c8931f` — handoff §3.1 fix-iteration addendum + correction of the
    prior over-claim at R6 #8 + fence confirmation that the two fixes
    only touch the V17 SQL comment line + `ResolveArticleToolTest` + the
    handoff itself.
  The original 5 delivery commits (`bb48aa0` / `0594a25` / `d21594f` /
  `977f9b8` / `7773c92`) remain unamended and append-only. Codex's
  targeted re-review prompt at `a7c5b6f` asked Codex only to verify clean
  tree + audited range + the two fixes + reissue verdict header — Codex's
  flipped verdict updated `docs/codex-findings.md` to
  `APPROVE_S_AUTO_27 / blocking_count=0`. §1–§3 + §5 substantive content
  reflects the post-fix state (per-change verdicts PASS; §2 Q1–Q9 PASS
  aggregate `approve`; §3 F1–F6 PASS; 4 non-blocking observations
  recorded). **S-Auto-27 is dev-side closed / Codex-approved on targeted
  re-review / both prior P0 blockers resolved; milestone-level outcome
  evidence is deferred to the M-Auto-6 final re-bless** (paired-evidence
  at milestone close after C-2b lands — which is THIS sub-sprint;
  `baseline_dir` + `docs/current_eval_baseline.md` do NOT flip until then).

  Range-accounting clarification: the literal cumulative range
  `bb48aa0^..4c8931f` contains 11 Git commits, not 8: 5 original delivery
  + 3 fix-iteration = 8 substantive commits + 3 acknowledged audit/package
  commits (`056fa5a` aidazi framework v3.2 archive, `3300b4a` per-sub-sprint
  Codex review prompt, `1954cb6` initial Codex
  APPROVE_S_AUTO_27_WITH_FIXES verdict audit trail). The 3 audit/package
  commits introduce no C-2b production/test drift and are explicitly
  acknowledged in Codex's §5 #4 non-blocking observation. Subsequent
  audit/package commit `a7c5b6f` (targeted re-review prompt) lives
  outside this cumulative range.

  Status semantics (per human 2026-06-06 cadence + deliver-agent
  2026-06-06 close verdict):
  - Dev work complete: 5 delivery commits `bb48aa0` (R6 #2+#3 KbArticle +
    V17 migration) / `0594a25` (R6 #4 ingestion parser) / `d21594f`
    (R6 #5+#7 search filter + INFO log) / `977f9b8` (R6 #1+#8 JSON
    flip + first direct-resolve test) / `7773c92` (initial dev handoff)
    + 3 fix-iteration commits `e6aad78` / `d27b824` / `4c8931f` (above).
  - Java `mvn -o clean test` = `1348 / 1 / 0 / 2` (+10 net tests vs the
    1337/1/0/2 launch baseline; +1 new test from the second direct-resolve
    via `d27b824`; sole failure = inherited
    `SystemPromptUserRequestedTiebreakerTest`, OQ-S41.5; unrelated
    system-prompt surface, provably uncoupled — C-2b touched zero
    prompt files).
  - Focused tests passed `23 / 0 / 0 / 0` at Codex's targeted-re-review
    verification (`KnowledgeIngestionRunnerTest` /
    `KnowledgeSearchServiceTest` / `KbArticleEligibilityCorpusTest` /
    `ResolveArticleToolTest` for the four reused suite anchors). The
    initial review's broader 7-suite run was `34 / 0 / 0 / 0` per Codex
    §5 #3.
  - Codex per-sub-sprint review: `APPROVE_S_AUTO_27 / blocking_count=0`
    on targeted re-review. §1 per-change verdicts approve all of R6
    #1–#8 with explicit pass-on-re-review notes for #3 (V17 SQL comment)
    and #8 (second direct-resolve test). §2 §4.1 nine-question kernel
    Q1–Q9 PASS with anchored evidence; aggregate verdict `approve`.
    §3 six focal-point verdicts F1–F6 PASS: F1 data-field-only filter
    verified (only `KbArticle.isSearchKnowledgeEligible()`; no
    content/title/UC/reranker branch); F2 direct resolve invariant
    PASS on re-review (both `ka41r000000LIEJAA4` and
    `ka41r000000LIEEAA4` test-pinned for direct resolve with
    `safe_to_show=true`); F3 forbidden-grep clean on re-review (zero
    flagged-ID matches + zero `(temp)` matches under `server/src/main`
    post-fix); F4 back-compat default-true verified (entity builder
    default, V17 backfill, ingestion absent/null → `true`); F5 no
    parallel governance field (`bot_visible` absent); F6 pre-fix audit
    decisions PASS (`KnowledgeHit` zero-diff with documented SKIP
    rationale; INFO log path α with `SearchKnowledgeTool` byte-untouched).
    4 non-blocking observations recorded in §5: (1) pre-C-2b consumer
    sanity check passed (zero existing consumers of `search_knowledge_eligible`
    at `bb48aa0^` under `server/src/main`); (2) initial review HEAD was
    `3300b4a` not `7773c92` due to the 2 descendant audit/package commits
    `056fa5a` + `3300b4a` — outside the audited range, not scope drift;
    (3) focused verification passed (initial 34/0/0/0 + targeted re-review
    23/0/0/0; `git diff --check` clean); (4) targeted re-review verification
    passed + the 11-vs-8 range count corrected (acknowledged audit/package
    commits explain the delta; non-blocking).
  - Wiring evidence (mocked / unit / integration): 5 new Java tests on
    `KbArticleEligibilityCorpusTest` (corpus invariant — exactly 2
    flagged, both still published, no missing-field articles) +
    `KnowledgeIngestionRunnerTest` extensions (explicit-false /
    explicit-true / absent-default-true) + `KnowledgeSearchServiceTest`
    extensions (target eligibility filter, default-eligible, all-eligible)
    + `ResolveArticleToolTest` extensions (direct-resolve invariant on
    BOTH flagged IDs after fix-iteration). Flyway V17 migration applies
    cleanly in the test context. `(temp)` grep under `server/src/main`
    clean post-`e6aad78`. Article-ID literal grep under `server/src/main`
    clean.
  - Pre-fix audit design choices captured in handoff §1 (#1 no
    conflicting governance field; #2 primitive `boolean` +
    `@Builder.Default = true` so Lombok @Data generates
    `isSearchKnowledgeEligible()`; #3 V16 confirmed highest, V17
    chosen with `NOT NULL DEFAULT TRUE` mirroring V4 `is_published`
    pattern; #4 existing `published_status` parsing idiom mirrored
    via `path(...).asBoolean(true)`; #5 single Step-8 caller path
    confirmed; #6 KnowledgeHit SKIPPED — hits are post-filter
    eligible-by-construction; #7 path α chosen — INFO log at service
    layer, `SearchKnowledgeTool` byte-untouched).
  - `baseline_dir` NOT moved; `docs/current_eval_baseline.md`
    UNCHANGED. Both flip at M-Auto-6 milestone close after the
    milestone-shared re-bless launches.

  Outcome evidence DEFERRED. R6's falsifiable hypothesis (`(temp)`
  template article occurrences in `search_knowledge` results visible
  to LLM → 0 post-R6; direct resolve via
  `ResolveArticleTool.byArticleId` preserved for both flagged
  articles; grounding floor unchanged; anti-误杀 anchors stable) is
  observable at the M-Auto-6 milestone-shared re-bless. The wiring
  evidence is sufficient for sub-sprint close.

  Subsequent sub-sprints:
  - C-2b is the **last** M-Auto-6 sub-sprint; the milestone-shared
    §9 real-LLM re-bless launches NEXT after this close commit.
  - The M-Auto-6 milestone-shared Codex review prompt
    `compact/M-Auto-6-review-prompt.md` is authored at this close
    commit (covering A + B + C-1 + C-2a + C-2b cumulative range
    per `process/milestone-framework.md` §4.3); dispatched after
    the re-bless evidence is in.

  Forbidden (preserved for archival reference): any new parallel
  governance field (no `bot_visible` — re-scope decision: reuse
  `search_knowledge_eligible`); any hardcoded article_id in Java;
  any title-keyword filter on `(temp)` or other strings; any corpus
  article deletion; any OTHER article in `knowledge_base_articles.json`
  modified beyond the 2 flagged; any OTHER field on the 2 flagged
  articles beyond `search_knowledge_eligible`; any `ResolveArticleTool`
  filter touch (direct resolve MUST stay unfiltered — anti-误杀 #1);
  any skill yaml / `SkillGuardrailDispatcher` /
  `resolve_faq_grounded_answer.yaml` touch (C-2a's surface); any
  `IntakeFieldsRegistry` / `IntakeFieldsMerger` / `BudgetChecker` /
  `ControlKernel` / `AgentRunLoopImpl` / `ContextProjectionBuilder` /
  `UpdateIntakeFieldsTool` touch (A's / C-1's surfaces); any UI file
  (B's surface); any `eval_interactive/` file; autoloop 5-file
  SHA-locked scoring set; `baseline_dir` or
  `docs/current_eval_baseline.md` move.

  ORIGINAL CONTRACT NOTES BLOCK FOLLOWS — preserved verbatim
  for archive reference:

  Sub-sprint C-2b of M-Auto-6 (Runtime substrate hygiene + admin
  observability + intake/clarification contract + UX/corpus
  governance). Activated 2026-06-06 after Sub-sprint C-2a (R5
  citation contract fix) dev-side close — Codex `APPROVE_S_AUTO_26 /
  blocking_count=0` on targeted re-review. Sub-sprints A
  (R1.a + R2.a + R4.a), B (R3.a + R3.b + R3.c), C-1 (R7 +
  R2.a#5-ext), and C-2a (R5 citation contract fix) are all
  dev-side closed per their respective archives. The
  outcome-evidence re-bless waits for C-2b to land — single
  milestone-shared re-bless at M-Auto-6 close.

  C-2b is the **last sub-sprint before the M-Auto-6 milestone-shared
  §9 real-LLM re-bless**. After C-2b dev-side close + Codex
  APPROVE_S_AUTO_27, deliver-agent drafts the milestone-shared
  Codex review prompt (`compact/M-Auto-6-review-prompt.md`)
  covering A + B + C-1 + C-2a + C-2b cumulative range, and the
  milestone-shared re-bless launches.

  Scope (corpus governance via existing-field reuse; semantic-touching
  at the LLM-facing search retrieval surface):
  - **R6 #1** — `data/knowledge/knowledge_base_articles.json` flip
    `search_knowledge_eligible: true → false` on exactly 2 `(temp)`
    template articles: `ka41r000000LIEJAA4` (row ~272) +
    `ka41r000000LIEEAA4` (row ~291). ONLY these 2 articles; ONLY
    this field.
  - **R6 #2-#7** — plumb the field through:
    - `KbArticle.java` — new `searchKnowledgeEligible` column
      (boolean; default `true`).
    - `V17__add_kb_search_knowledge_eligible.sql` — new Flyway
      migration (V16 is highest existing) with `NOT NULL DEFAULT TRUE`.
    - `KnowledgeIngestionRunner.buildKbArticleFromJson` — parse
      the field from JSON; default `true` if absent / null.
    - `KnowledgeSearchService` — filter candidates by
      `searchKnowledgeEligible != false` at the candidates → hits
      assembly step + structured backend INFO log on filter
      decision (article_id + filter_reason).
    - `KnowledgeHit` — propagate the field (or skip per pre-fix
      audit design choice; document rationale).
    - `SearchKnowledgeTool` — INFO log only if path β chosen at
      pre-fix audit (#7); otherwise log emitted at
      `KnowledgeSearchService` layer.
  - **R6 #8 direct resolve invariant** — `ResolveArticleTool.byArticleId`
    STILL returns the 2 flagged articles. Filter is at search
    surface ONLY; human-CS access preserved.

  Per-sub-sprint Codex review REQUIRED. R6 changes the corpus
  retrieval surface visible to the LLM via the existing
  `search_knowledge_eligible` data field + service-layer filter.
  Data-field driven (NOT title-keyword; NOT hardcoded ID). Codex
  focus per §4.3 must include the data-field-only filter evidence,
  direct resolve invariant evidence, and forbidden-grep evidence
  (no hardcoded article-id strings in Java; no `(temp)` keyword
  filter).

  Forbidden: any new parallel governance field (no `bot_visible`
  per re-scope decision); any hardcoded article-id in Java; any
  title-keyword filter on `(temp)` or other strings; any corpus
  article deletion; any OTHER article in `knowledge_base_articles.json`
  modified beyond the 2 flagged; any OTHER field on the 2 flagged
  articles beyond `search_knowledge_eligible`; any
  `ResolveArticleTool` filter touch (direct resolve MUST stay
  unfiltered); any skill yaml / `SkillGuardrailDispatcher` /
  `resolve_faq_grounded_answer.yaml` touch (C-2a's surface);
  any `IntakeFieldsRegistry` / `IntakeFieldsMerger` /
  `BudgetChecker` / `ControlKernel` / `AgentRunLoopImpl` /
  `ContextProjectionBuilder` / `UpdateIntakeFieldsTool` touch
  (A's / C-1's surfaces); any UI file (B's surface); any
  `eval_interactive/` file; autoloop 5-file SHA-locked scoring
  set; `baseline_dir` or `docs/current_eval_baseline.md` move
  (both flip at M-Auto-6 milestone close after the
  milestone-shared re-bless).

  `baseline_dir` UNCHANGED (`m-auto-5-baseline-20260604-simfixed-stalledfix`);
  `docs/current_eval_baseline.md` UNCHANGED. NO outcome-evidence
  re-bless at this sub-sprint close — that runs at the M-Auto-6
  milestone close.
---

# Sub-sprint C-2b — S-Auto-27 / Sprint 082 — R6 corpus eligibility filter (ARCHIVED 2026-06-06)

> **Dev-side close status (2026-06-06):** S-Auto-27 is dev-side closed /
> Codex-approved on targeted re-review / both prior P0 blockers resolved
> by fix-iteration; milestone-level outcome evidence is deferred to the
> M-Auto-6 final re-bless.
>
> | Gate | Status | Evidence |
> |---|---|---|
> | Dev code shipped (5 delivery commits) | ✅ | `bb48aa0` (R6 #2+#3 KbArticle.searchKnowledgeEligible entity + V17 Flyway migration `NOT NULL DEFAULT TRUE`) / `0594a25` (R6 #4 `KnowledgeIngestionRunner.buildKbArticleFromJson` parses via existing `path(...).asBoolean(true)` `published_status` idiom) / `d21594f` (R6 #5+#7 `KnowledgeSearchService` filter step adjacent to existing `isPublished` Sprint-14 §L0 defense-in-depth pattern + INFO log path α at service layer) / `977f9b8` (R6 #1+#8 JSON flip on 2 `(temp)` template articles `ka41r000000LIEJAA4` + `ka41r000000LIEEAA4` + first direct-resolve invariant test on `ka41r000000LIEJAA4`) / `7773c92` (dev handoff). |
> | Fix-iteration commits (closing both initial Codex P0 blockers) | ✅ | `e6aad78` (V17 SQL line-4 comment rewritten to content-neutral "CS-only placeholder template articles"; SQL operation byte-unchanged; closes Codex §4 finding #2) / `d27b824` (second direct-resolve invariant test `execute_searchIneligibleArticle_stillResolvesByDirectId_secondTemplate` for `ka41r000000LIEEAA4` mirroring first-template at `:239-272`; closes Codex §4 finding #1) / `4c8931f` (handoff §3.1 fix-iteration addendum + over-claim correction at R6 #8). Original 5 delivery commits unamended and append-only. |
> | Initial Codex per-sub-sprint review (audit trail) | ✅ recorded | Commit `1954cb6`: `APPROVE_S_AUTO_27_WITH_FIXES / blocking_count=2`. §4 finding #1 = F2/Q8 direct-resolve invariant evidence gap (only first flagged article had a test); §4 finding #2 = F3 forbidden-grep tripped on V17 SQL line-4 `(temp)` literal (operation byte-unchanged). Both infra-hygiene gaps, NOT executable semantic hardcode. |
> | Targeted re-review (dispatched after fix-iteration) | ✅ `APPROVE_S_AUTO_27 / blocking_count=0` | Targeted prompt at `a7c5b6f` asked Codex to verify clean tree + cumulative range unchanged on the original 5 delivery + 3 fix-iteration commits + reissue verdict header + update §4 only with §1–§3 + §5 substantive content reflecting the post-fix state. Codex's flipped verdict updated `docs/codex-findings.md`: §1 per-change verdicts PASS with explicit pass-on-re-review notes for #3 (V17 SQL comment) and #8 (second direct-resolve test); §2 Q1–Q9 PASS aggregate `approve`; §3 F1–F6 PASS; §5 4 non-blocking observations recorded. |
> | Java unit + characterization tests | ✅ `1348 / 1 / 0 / 2` | +10 net tests vs the 1337/1/0/2 launch baseline (+5 delivery + 1 fix-iteration second direct-resolve from `d27b824` + 4 reused-by-extension); sole failure = inherited `SystemPromptUserRequestedTiebreakerTest` (OQ-S41.5, system-prompt content surface — provably uncoupled). |
> | Focused 4-suite test re-run (targeted re-review) | ✅ `23 / 0 / 0 / 0` | `mvn -o -Dtest=KnowledgeIngestionRunnerTest,KnowledgeSearchServiceTest,KbArticleEligibilityCorpusTest,ResolveArticleToolTest test` at HEAD `a7c5b6f`. The initial review's broader 7-suite run was `34 / 0 / 0 / 0` (Codex §5 #3). |
> | Backend rebuild + Flyway V17 application | ✅ | V17 migration applies cleanly; existing rows backfill to `true`; new column `NOT NULL DEFAULT TRUE`. |
> | `git diff --check` over `bb48aa0^..4c8931f` | ✅ clean | No whitespace defects. |
> | Forbidden-grep evidence | ✅ clean post-fix | `server/src/main` contains zero `(temp)` matches and zero literal article-ID strings (`ka41r000000LIEJAA4` / `ka41r000000LIEEAA4`) post-`e6aad78`. |
> | Pre-C-2b consumer sanity check | ✅ clean (Codex §5 #1) | At `bb48aa0^`, `git grep` found zero `search_knowledge_eligible` / `searchKnowledgeEligible` consumer references under `server/src/main`; the handoff's "ingested-but-unused on 218/218 articles" diagnosis is accurate. |
> | Cumulative-range accounting | ⚠️ acknowledged | Literal `bb48aa0^..4c8931f` contains 11 Git commits, not 8: 5 original delivery + 3 fix-iteration = 8 substantive + 3 acknowledged audit/package (`056fa5a` aidazi framework v3.2 archive; `3300b4a` per-sub-sprint Codex review prompt; `1954cb6` initial Codex APPROVE_S_AUTO_27_WITH_FIXES audit trail). The 3 audit/package commits introduce no C-2b production/test drift; explicitly acknowledged in Codex's §5 #4. Subsequent audit/package commit `a7c5b6f` (targeted re-review prompt) lives outside this cumulative range. |
> | **Milestone-shared §9 real-LLM re-bless (paired-evidence)** | ⏳ DEFERRED to M-Auto-6 close | Launches NEXT (deliver-agent + human action). `baseline_dir` and `docs/current_eval_baseline.md` NOT moved until then. R6 falsifiable hypothesis: `(temp)` template article occurrences in `search_knowledge` results visible to LLM → 0 post-R6; direct resolve preserved for both flagged articles; grounding floor unchanged; anti-误杀 anchors stable. |
>
> The body below is preserved verbatim as the archived contract that
> dev executed against. Do not edit — record-only.

# Sub-sprint C-2b — S-Auto-27 / Sprint 082 — R6 corpus eligibility filter

## Class

**Layer (per `iteration_governance.md` §3.2):**

- **R6** → `infra` across all sub-surfaces:
  - Corpus data flag flip (JSON; 2 articles only; existing field).
  - Schema migration (Flyway V17 adds `search_knowledge_eligible`
    column with `NOT NULL DEFAULT TRUE`).
  - Entity column on `KbArticle.java`.
  - Ingestion parses the field on `KnowledgeIngestionRunner`.
  - Search-time filter on `KnowledgeSearchService` (NOT on
    `ResolveArticleTool` — direct resolve stays unfiltered).
  - `KnowledgeHit` field propagation for observability (conditional
    on pre-fix audit design choice).
  - `SearchKnowledgeTool` structured INFO log on filter decision
    (conditional on pre-fix audit path α vs β decision).

**§7 stanza requirement:** **REQUIRED.** R6 changes the corpus
retrieval surface visible to the LLM. Although it's data-field
driven (NOT a content scan, NOT a title-keyword match), the surface
itself is LLM-facing. Full stanza in §7 below.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant.

- The corpus content itself is unchanged (the 2 articles remain in
  the JSON, in the database, retrievable via
  `ResolveArticleTool.byArticleId`).
- The search surface filter narrows what the LLM sees by ONE existing
  data field; the default (field missing) is visible (back-compat
  preserved).
- The grounding floor (`must_cite_source` from C-2a) is unaffected
  by this change.

**Semantic hardcode:** No semantic hardcode introduced.

- R6 uses ONLY the existing `search_knowledge_eligible` data field;
  zero content scan; zero per-UC matrix; zero title-keyword match
  on `(temp)` or any other string.
- The 2 article-id flips are corpus data edits (in the JSON), NOT
  hardcoded IDs in Java code.
- New articles default visible (no behavioural change for unflagged
  content).
- `KnowledgeSearchService` filter is a simple `eligible != false`
  predicate — no semantic decision.

## Goal

After this sub-sprint ships:

- **R6 #1 corpus data flip**: `ka41r000000LIEJAA4` (row ~272) and
  `ka41r000000LIEEAA4` (row ~291) in
  `data/knowledge/knowledge_base_articles.json` carry
  `"search_knowledge_eligible": false`. All other 216 articles
  retain their existing `search_knowledge_eligible: true` (no other
  JSON field on any article is touched).
- **R6 #2 entity column**: `KbArticle.java` carries a new
  `searchKnowledgeEligible` field (boolean, `@Column(name =
  "search_knowledge_eligible", nullable = false)`) with default
  `true` semantics in the entity AND the database.
- **R6 #3 schema migration**: a new Flyway migration
  `V17__add_kb_search_knowledge_eligible.sql` adds the column to
  `kb_articles` with `NOT NULL DEFAULT TRUE`. Migration runs
  cleanly on fresh DB + on existing DB with pre-existing rows
  (backfilled to `true`).
- **R6 #4 ingestion**: `KnowledgeIngestionRunner.buildKbArticleFromJson`
  parses the field from JSON (`searchKnowledgeEligible = json field
  if present else true`); persists to `KbArticle`. After re-ingestion
  the 2 flagged articles' rows in `kb_articles` have
  `search_knowledge_eligible = false`; all others have `true`.
- **R6 #5 search-time filter**: `KnowledgeSearchService` filters
  candidates by `searchKnowledgeEligible != false` (default-true
  semantics) at the result-assembly step. Articles with `false`
  are NOT returned to the calling tool. Articles with `true` or
  default are returned normally.
- **R6 #6 KnowledgeHit observability**: `KnowledgeHit` carries the
  `searchKnowledgeEligible` field so the trace and downstream code
  see the eligibility signal (or skip per pre-fix audit; document
  rationale).
- **R6 #7 SearchKnowledgeTool INFO log**: when the search service
  filters an article, a structured backend INFO log entry is
  emitted (at the search service or tool layer per pre-fix audit
  decision) with: `tool_event_id, article_id,
  filter_reason="search_knowledge_eligible=false"`. NOT a
  user-facing trace event.
- **R6 #8 direct resolve preserved**: `ResolveArticleTool` continues
  to surface the 2 `(temp)` articles when called directly by
  `article_id`. The filter is at the search surface ONLY.

NOT a goal:

- Adding any new `bot_visible` or other parallel governance field
  (re-scope decision: reuse `search_knowledge_eligible`, do NOT
  introduce a parallel mechanism).
- Hardcoding the 2 `article_id` values in Java code (forbidden §1.7
  shortcut).
- Adding a title-keyword filter (e.g. `startsWith("(temp)")`) —
  also forbidden §1.7.
- Deleting either article from the corpus / DB.
- Auto-flagging any OTHER article (data team decision; out of scope).
- Filtering `ResolveArticleTool` by eligibility (direct resolve must
  stay working — human-CS access is preserved).
- Modifying any other JSON field on the 2 flagged articles.
- Modifying any field on any other article in the JSON.
- Touching skill yaml / prompt wording / `must_cite_source`
  guardrail (C-2a's surface).
- Touching CaseSpec / eval scoring / `baseline_dir` /
  `current_eval_baseline.md`.

## Scope (executable, #1–#8)

The dev prompt at `compact/sprint-082-dev-prompt.md` is the
self-contained executable view of this contract; sync invariant per
`prompt-artifact-rules.md` §9.3. The steps below are the canonical
version; the prompt mirrors them with cumulative context +
read-order wrappers added.

### #1 — R6 #1: corpus data flip on 2 `(temp)` articles

**Anchor:** `data/knowledge/knowledge_base_articles.json`
- `ka41r000000LIEJAA4` near row 272.
- `ka41r000000LIEEAA4` near row 291.

PRE-FIX AUDIT before #1:
- Confirm both articles currently have `"search_knowledge_eligible":
  true`.
- Confirm no overlapping governance field with conflicting semantics
  (existing `eligible_uc_scope` is preserved; document any other
  potentially-conflicting field).

Change: flip the field on exactly the 2 articles to `false`. NO
other field on these 2 articles is touched. NO other article in
the JSON is touched. Verify via `git diff` shape: exactly 2 line
changes (one per article), each a `true → false` swap.

### #2 — R6 #2: `KbArticle` entity column

**Anchor:** `server/src/main/java/com/gumtree/csagent/model/KbArticle.java`

Add a new field + column declaration matching the existing boolean
column pattern (e.g. mirror `is_published`):

```java
@Column(name = "search_knowledge_eligible", nullable = false)
private boolean searchKnowledgeEligible = true;
```

Standard JavaBean getter / setter. ONLY the new field + accessors;
no other entity field touched.

### #3 — R6 #3: Flyway V17 migration

**Anchor:** `server/src/main/resources/db/migration/`

Create `V17__add_kb_search_knowledge_eligible.sql`:

```sql
ALTER TABLE kb_articles
    ADD COLUMN search_knowledge_eligible BOOLEAN NOT NULL DEFAULT TRUE;
```

The `DEFAULT TRUE` backfills existing rows; the `NOT NULL` matches
the entity field's primitive `boolean` type. ONE column addition;
no index change, no other constraint change, no other table
touched.

### #4 — R6 #4: `KnowledgeIngestionRunner` parses the field

**Anchor:** `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
(specifically `buildKbArticleFromJson`).

Parse the field with default-true on absent / null. Persist to
`KbArticle`. ONLY the new parsing step; no change to the existing
parsing of other fields; no content scan; no per-UC matrix; no
hardcoded article_id check.

### #5 — R6 #5: `KnowledgeSearchService` filters at the search surface

**Anchor:** `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`

Add a filter step at the candidates → hits assembly:

```java
List<KbArticle> filtered = candidates.stream()
    .filter(a -> a.isSearchKnowledgeEligible())
    .collect(Collectors.toList());
```

Adapt the stream / collection idiom to existing code style. Emit a
structured backend INFO log on each filter decision (decision: at
this layer OR delegate to #7 per audit). ONLY the eligibility
filter; no score change; no reranker influence; no content scan;
no title-keyword match.

### #6 — R6 #6: `KnowledgeHit` carries the eligibility field

**Anchor:** `server/src/main/java/com/gumtree/csagent/model/KnowledgeHit.java`

PRE-FIX AUDIT design choice: does `KnowledgeHit` represent
pre-filter candidates or post-filter results? If post-filter, the
field is always `true` and may be omitted; if pre-filter, the field
is informative. Decision documented in handoff §1.

### #7 — R6 #7: `SearchKnowledgeTool` INFO log

**Anchor:** `server/src/main/java/com/gumtree/csagent/service/tools/SearchKnowledgeTool.java`

PRE-FIX AUDIT design choice: log at the service layer (path α) OR
the tool layer (path β). If path α, `SearchKnowledgeTool` is
byte-untouched and the log emits at `KnowledgeSearchService`. If
path β, the tool emits the per-filtered-article INFO log.

Hard fence: log is INFO level, structured (`tool_event_id` if
available, `article_id`, `filter_reason="search_knowledge_eligible=false"`),
NOT a user-facing trace event (no `ToolEvent` shape change).

### #8 — R6 #8: tests + integration smoke

Tests cover:
- **JSON data verification**: 2 articles flipped; sample of others
  retain `true`; no other field on the 2 flagged articles changed
  (`git diff` shape OR JSON-shape diff test).
- **Ingestion (#4)**: re-ingestion with `false` / `true` / absent
  persists correctly (default `true`).
- **Search-service filter (#5)**: candidates list with 1 ineligible
  + 2 eligible returns hits excluding the ineligible; all-eligible
  unchanged; all-ineligible returns empty; default-true returned.
- **Direct resolve (R6 #8 invariant)**:
  `ResolveArticleTool.byArticleId("ka41r000000LIEJAA4")` returns
  the article; `ResolveArticleTool.byArticleId("ka41r000000LIEEAA4")`
  returns the article.
- **Backend rebuild**: `mvn -o -DskipTests package` BUILD SUCCESS;
  `mvn -o test` Flyway migration applies cleanly in test context;
  Java baseline `1337 / 1 / 0 / 2` preserved + R6 additions.
- **Forbidden absences (grep evidence)**:
  - `server/src/main` does NOT contain literal article-id strings
    `ka41r000000LIEJAA4` or `ka41r000000LIEEAA4` post-change.
  - `server/src/main` does NOT contain `(temp)` or
    `startsWith("(temp)")` patterns introduced by this sub-sprint.

## Anti-误杀 invariants (HARD, non-negotiable)

1. **Filter is at SEARCH surface only.**
   `ResolveArticleTool.byArticleId` continues to return the 2
   flagged articles. Human-CS access preserved.
2. **Data-field driven only.** Filter uses ONLY
   `KbArticle.isSearchKnowledgeEligible()`. NO title-keyword match
   on `(temp)`; NO body inspection; NO LLM call; NO content scan.
3. **No hardcoded article-id list in Java.** The 2 article-id flips
   are corpus data edits in the JSON ONLY.
4. **Default visible.** A `KbArticle` with no eligibility field set
   (default `true` from the entity / DB) returns normally.
5. **Corpus content unchanged.** The 2 articles remain in the JSON,
   in the database, retrievable via `ResolveArticleTool`. Only the
   LLM-facing search surface filters.
6. **No new parallel governance field.** Re-scope decision: reuse
   `search_knowledge_eligible`; do NOT introduce `bot_visible`.
7. **Observability preserved.** Filter decisions emit structured
   INFO log entries with `article_id` + `filter_reason`.
8. **No skill yaml / prompt wording change.** C-2a's surface;
   C-2b leaves yaml byte-untouched.
9. **No new `escalation_reason` enum value.**
10. **No new Tier-0 invariant.**
11. **`baseline_dir` UNCHANGED. `docs/current_eval_baseline.md`
    UNCHANGED.**

## Hard fences / STOP conditions

**Files allowed to edit** (fence):

- `data/knowledge/knowledge_base_articles.json` — ONLY the
  `search_knowledge_eligible` field on the 2 flagged articles;
  ONLY value flip `true → false`. NO other field; NO other
  article.
- `server/src/main/java/com/gumtree/csagent/model/KbArticle.java`
  (new field + getter / setter only).
- `server/src/main/java/com/gumtree/csagent/model/KnowledgeHit.java`
  (new field + getter / setter only IF pre-fix audit (#6) decides
  to propagate the field; OTHERWISE skip).
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
  (parse + persist the field only).
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
  (filter step + INFO log only).
- `server/src/main/java/com/gumtree/csagent/service/tools/SearchKnowledgeTool.java`
  (INFO log ONLY if path β chosen at #7; otherwise byte-untouched).
- `server/src/main/resources/db/migration/V17__add_kb_search_knowledge_eligible.sql`
  (new file; ONE column addition).
- `server/src/test/java/.../KnowledgeIngestionRunnerTest.java`
  (extend or new).
- `server/src/test/java/.../KnowledgeSearchServiceTest.java`
  (extend or new).
- `server/src/test/java/.../SearchKnowledgeToolTest.java`
  (extend or new).
- `server/src/test/java/.../ResolveArticleToolTest.java` (extend
  for direct-resolve invariant only).
- `docs/sprints/sprint-082-handoff.md` (dev handoff).

**Files FORBIDDEN to edit**:

- `KbArticleRepository.java` (no repository method change needed).
- `ResolveArticleTool.java` (must NOT filter at resolve surface; may
  be touched ONLY to add a test in `ResolveArticleToolTest` if
  needed — but no production code change).
- Any other article in `knowledge_base_articles.json`.
- Any other field on the 2 flagged articles.
- Any skill yaml or any line of any skill yaml (C-2a's surface).
- `SkillGuardrailDispatcher.java` (C-2a's surface).
- `resolve_faq_grounded_answer.yaml` (C-2a's surface).
- `IntakeFieldsRegistry.java` / `IntakeFieldsMerger.java` /
  `BudgetChecker.java` / `ControlKernel.java` / `AgentRunLoopImpl.java` /
  `ContextProjectionBuilder.java` / `UpdateIntakeFieldsTool.java`
  (A's / C-1's surfaces).
- Any UI file (B's surface).
- Any `eval_interactive/` file.
- Autoloop 5-file SHA-locked scoring set (fence-#13).
- `autoloop/config.yaml` `baseline_dir`.
- `docs/current_eval_baseline.md`.

**STOP conditions**:

- Pre-fix audit on the 2 articles surfaces an overlapping governance
  field with conflicting semantics → STOP, surface.
- Pre-fix audit on `KnowledgeIngestionRunner` surfaces an unexpected
  shape → STOP, surface.
- Pre-fix audit on `KnowledgeSearchService` surfaces a second caller
  path that should NOT filter → STOP, surface.
- V17 migration fails to apply (column already exists; conflict with
  another sub-sprint's migration) → STOP, surface.
- A test surfaces `ResolveArticleTool` being filtered when it shouldn't
  → STOP, anti-误杀 #1 violation.
- Grep evidence shows a hardcoded article_id string in Java → STOP,
  anti-误杀 #3 violation; revert.
- Grep evidence shows a title-keyword filter introduced → STOP,
  anti-误杀 #2 / §1.7 violation; revert.
- Any file outside the fence is touched → STOP, revert, re-launch.

## Test / eval requirements

- All new tests in #8 GREEN.
- Existing Java baseline `1337 / 1 / 0 / 2` (post-C-2a) preserved
  + R6 additions (~+12-20 tests including ingestion + service-filter
  + direct-resolve invariant + data-shape verification). Sole
  pre-existing failure (`OQ-S41.5`) preserved.
- Flyway migration V17 applies cleanly in the test context.
- Eval pytest `553` UNCHANGED (no eval-side change).
- Autoloop pytest `324` UNCHANGED.
- Backend rebuild + test summary in handoff §2.
- **No real-LLM re-bless at this sub-sprint close.** Outcome
  evidence is the M-Auto-6 milestone-shared re-bless after this
  sub-sprint closes.
- Wiring evidence vs outcome evidence separator per §5.7 mocked-LLM
  gate documented in handoff.

## §7 Layer-classification + anti-hardcode stanza

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` across all sub-surfaces — corpus
data flag flip (data); Flyway V17 schema migration (schema); KbArticle
column (entity); KnowledgeIngestionRunner field parsing (ingestion);
KnowledgeSearchService eligibility filter (service); KnowledgeHit
field propagation (model, conditional on pre-fix audit); structured
INFO log for filter observability.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant.
Corpus content is unchanged (articles remain in JSON + DB);
`ResolveArticleTool` direct resolve is unfiltered (human-CS access
preserved); only the LLM-facing search surface filters. Default
visible.

**Semantic hardcode:** No semantic hardcode introduced.
- Filter uses ONLY `KbArticle.isSearchKnowledgeEligible()`. No
  content scan; no title-keyword match on `(temp)`; no body
  inspection; no LLM call.
- The 2 article-id flips are corpus data edits in the JSON; ZERO
  hardcoded article-id strings in Java code.
- New articles default visible (no behavioural change for unflagged
  content).
- Reuses an EXISTING data field (`search_knowledge_eligible`,
  present on 218/218 articles), NOT a new parallel governance
  mechanism.

**Generalization coverage:** target / neighbor / negative / shadow
case counts: 3 / ~6 / ~8 / 0
- target: c7 / c12 / c17 (`(temp)` article surfaced unwrapped).
- neighbor: any search_knowledge call that would match the 2 flagged
  articles' content (the 2 known articles + zero false positives
  given the data-field-driven filter).
- negative: default-true articles unaffected; missing-field defaults
  to visible (back-compat); direct resolve via
  `ResolveArticleTool.byArticleId` unchanged for both flagged
  articles; hardcoded ID grep evidence NOT used; title-keyword grep
  evidence NOT used; no skill yaml / wording / `must_cite_source`
  guardrail touch.
- shadow: not applicable (mocked-LLM + ingestion + service tests are
  wiring evidence; real evidence is the M-Auto-6 milestone-shared
  re-bless after this sub-sprint closes).
```

## Codex review plan (per `process/milestone-framework.md` §4.3)

**Per-sub-sprint Codex review REQUIRED** because R6 changes the
corpus retrieval surface visible to the LLM (search-knowledge
results are LLM-facing). Although data-field driven (NOT title-keyword;
NOT content scan), the surface itself is semantic-touching per
`iteration_governance.md` §7 + `process/milestone-framework.md` §4.3.

Codex prompt artifact: `compact/sprint-082-codex-review-prompt.md`
(deliver-agent authors at sub-sprint close, embeds §4.1 nine-question
kernel + §7 stanza + file-path fence + anti-误杀 invariants +
generalization coverage + the **data-field-only filter evidence +
direct resolve invariant evidence + forbidden-grep evidence** as
§3 focal points).

**Focus points for Codex** (per §4.3):

- F1 (data-field driven): confirm filter uses ONLY
  `article.searchKnowledgeEligible` — no content scan; no
  title-keyword on `(temp)`; no per-UC matrix.
- F2 (direct resolve invariant): confirm `ResolveArticleTool.byArticleId`
  STILL returns both flagged articles (filter is at search surface
  only).
- F3 (forbidden-grep): grep `server/src/main` for the 2 article-id
  literals + `(temp)` patterns → ZERO hits in Java code post-change.
- F4 (back-compat default-true): articles without the field default
  to visible; ingestion default `true` when JSON absent.
- F5 (no parallel governance field): no `bot_visible` introduced;
  the JSON edit is ONLY on `search_knowledge_eligible`.
- Q1–Q9 covered via the focal points above.

## Handoff requirements (dev authors `docs/sprints/sprint-082-handoff.md`)

§1 of the handoff must include:

- For each of #1-#8: file:line ranges + rationale + the test name(s)
  that gate it.
- Pre-fix audit outcomes for #1 (no overlapping governance field),
  #2 (existing boolean column pattern), #3 (V16 is highest; V17
  chosen), #4 (existing JSON parsing idiom; ignored-unknown-keys
  pattern confirmed), #5 (single caller path; ResolveArticleTool
  bypasses), #6 (KnowledgeHit pre-filter vs post-filter design
  choice + decision), #7 (path α vs β for INFO log + decision).
- Java test results (full numeric: passed / failed / skipped /
  errors). Flyway V17 migration applied cleanly.
- Eval pytest / autoloop pytest results (UNCHANGED).
- STOP confirmations:
  - File fence respected; no forbidden file touched.
  - Only 2 JSON articles changed; only `search_knowledge_eligible`
    flipped on each.
  - No hardcoded article-id in Java (grep evidence).
  - No title-keyword filter (grep evidence).
  - No skill yaml / `SkillGuardrailDispatcher` / `must_cite_source`
    touch.
  - No `IntakeFieldsRegistry` / etc. touch.
  - `ResolveArticleTool.byArticleId` direct resolve still works on
    both flagged articles (test evidence).
  - No new `escalation_reason` enum value.
  - `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md`
    UNCHANGED.
  - No real-LLM outcome re-bless launched (deferred to milestone
    close).
- A clear "wiring evidence" vs "outcome evidence" separator per §5.7
  mocked-LLM gate.

## Commit discipline

Recommended commit split (per `prompt-artifact-rules.md` §9):

1. **Commit 1 — R6 #2 + #3 (entity + migration)**: `KbArticle`
   field + V17 SQL migration. Schema lands cleanly; existing rows
   backfill to `true`.
2. **Commit 2 — R6 #4 (ingestion parser)**: `KnowledgeIngestionRunner`
   parses the field; `KnowledgeIngestionRunnerTest` extensions.
3. **Commit 3 — R6 #5 + #6 + #7 (search service filter + hit +
   log)**: `KnowledgeSearchService` filter + INFO log;
   `KnowledgeHit` field (or skipped per audit); `SearchKnowledgeTool`
   log (if path β) or no change (if path α).
   `KnowledgeSearchServiceTest` + `SearchKnowledgeToolTest`
   extensions.
4. **Commit 4 — R6 #1 (JSON data flip)**: 2-article flip in
   `data/knowledge/knowledge_base_articles.json` +
   data-verification + direct-resolve invariant tests
   (`ResolveArticleToolTest`).
5. **Commit 5 — Dev handoff**: `docs/sprints/sprint-082-handoff.md`.

(The commit order matters: schema first so re-ingestion in test
contexts doesn't fail on a missing column; ingestion second so the
field is populated; service-filter third so the filter sees populated
data; JSON edit fourth so the 2 flagged articles flip on the live
corpus; handoff last.)

## Self-check checklist (dev completes before claiming done)

- [ ] Each of #1-#8 implemented with file:line ranges captured in
      handoff §1.
- [ ] Pre-fix audits for #1-#7 documented in handoff §1.
- [ ] R6 #1 JSON: ONLY 2 articles flipped; ONLY
      `search_knowledge_eligible` field touched; `git diff` shows
      exactly 2 line changes.
- [ ] R6 #2 entity: new field + getter / setter; default `true`;
      mirrors existing boolean column pattern.
- [ ] R6 #3 V17 migration: ONE column added with
      `NOT NULL DEFAULT TRUE`; migration applies cleanly.
- [ ] R6 #4 ingestion: field parsed from JSON; absent / null →
      `true`; persisted to `KbArticle`.
- [ ] R6 #5 search filter: filters `false`; preserves `true` and
      default; INFO log emitted on filter decision (or in #7 per
      audit).
- [ ] R6 #6 KnowledgeHit: field propagated (or skipped per audit
      with rationale documented).
- [ ] R6 #7 log: structured INFO with `article_id` +
      `filter_reason`; NOT a user-facing trace event.
- [ ] R6 #8 direct resolve: `ResolveArticleTool.byArticleId` STILL
      returns the 2 flagged articles (anti-误杀 #1 invariant).
- [ ] All new tests in #8 GREEN.
- [ ] Java baseline `1337 / 1 / 0 / 2` + R6 additions, no
      regressions.
- [ ] Eval pytest `553` unchanged.
- [ ] Autoloop pytest `324` unchanged.
- [ ] No file outside the file fence touched.
- [ ] No hardcoded article-id in Java (grep evidence).
- [ ] No title-keyword filter (grep evidence).
- [ ] No skill yaml / `SkillGuardrailDispatcher` /
      `resolve_faq_grounded_answer.yaml` touch.
- [ ] No `IntakeFieldsRegistry` / `BudgetChecker` / `ControlKernel` /
      `AgentRunLoopImpl` / `ContextProjectionBuilder` /
      `UpdateIntakeFieldsTool` / `IntakeFieldsMerger` touch.
- [ ] §7 stanza copied verbatim into handoff.
- [ ] `baseline_dir` UNCHANGED.
- [ ] `docs/current_eval_baseline.md` UNCHANGED.
- [ ] No outcome-evidence re-bless launched.

When all checked: hand back to deliver-agent for close review +
per-sub-sprint Codex dispatch + M-Auto-6 milestone-shared §9
real-LLM re-bless launch.
