---
title: Sprint 082 / S-Auto-27 / M-Auto-6 Sub-sprint C-2b dev handoff — R6 search-surface eligibility filter (search_knowledge_eligible reuse)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: code (server/.../model/KbArticle.java, service/knowledge/KnowledgeIngestionRunner.java, service/knowledge/KnowledgeSearchService.java, resources/db/migration/V17__add_kb_search_knowledge_eligible.sql, data/knowledge/knowledge_base_articles.json)
last_reviewed: 2026-06-06
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  M-Auto-6 Sub-sprint C-2b. R6 (triggers c7 / c12 / c17): the bot surfaced a
  (temp) CS-only template article (ka41r000000LIEJAA4 / ka41r000000LIEEAA4,
  containing XXXXXXXXX placeholders for CS manual fill-in) as if it were
  user-facing content. Root cause is corpus governance, not a semantic bug:
  these templates have no business surfacing on the LLM-facing search surface.
  Re-scope decision (2026-06-06, human): reuse the EXISTING ingested-but-unused
  search_knowledge_eligible data field (present on 218/218 articles) instead of
  the proposal's parallel bot_visible field. Layer: infra across all
  sub-surfaces. Plumb the field through entity (KbArticle) + Flyway V17 column +
  ingestion parser + search-service hit-projection filter; data-flip the 2
  template articles true→false in the JSON; emit a structured backend INFO log
  per filter decision. KnowledgeHit field (#6) SKIPPED — hits are post-filter,
  eligible-by-construction. INFO log (#7) at the service layer (path α) —
  SearchKnowledgeTool byte-untouched. ResolveArticleTool direct resolve is
  unfiltered (anti-误杀 #1 preserved). NO outcome-evidence re-bless this
  sub-sprint; deferred to the M-Auto-6 milestone-shared §9 re-bless after C-2b.
  baseline_dir + docs/current_eval_baseline.md UNCHANGED.
---

# Sprint 082 / S-Auto-27 / M-Auto-6 Sub-sprint C-2b — dev handoff

## §0 Cold-start summary + verdict

**Goal.** Ship R6 — stop the bot surfacing the two `(temp)` CS-only template
articles on the LLM-facing `search_knowledge` surface, by reusing the existing
`search_knowledge_eligible` data field (ingested-but-unused on 218/218
articles) as a search-time filter. Direct `resolve_article` by id stays
unfiltered so human-CS access is preserved.

**Triggers.** c7 / c12 / c17 — `ka41r000000LIEJAA4` ("(temp) Ad removed - By CS
(general)") and `ka41r000000LIEEAA4` ("(temp) NTD Ad Removed Information"),
both carrying `XXXXXXXXX` placeholders the LLM misread into fake
"your ad was removed because…" output.

**Verdict: DONE — wiring evidence green; outcome evidence deferred to the
milestone-shared re-bless.**

- **R6 #1 data flip** (`infra`) — `data/knowledge/knowledge_base_articles.json`:
  `search_knowledge_eligible` flipped `true → false` on exactly the 2 template
  articles. `git diff` = 2 lines changed (one per article); 216 others remain
  `true`; no other field on the 2 articles touched.
- **R6 #2 entity** (`infra`) — `KbArticle.searchKnowledgeEligible` (primitive
  `boolean`, `@Builder.Default = true`, `@Column(... nullable = false)`).
- **R6 #3 migration** (`infra`) — `V17__add_kb_search_knowledge_eligible.sql`:
  one `ADD COLUMN ... BOOLEAN NOT NULL DEFAULT TRUE`.
- **R6 #4 ingestion** (`infra`) — `KnowledgeIngestionRunner.buildKbArticleFromJson`
  parses the field via the existing `published_status` idiom (absent/null →
  `true`) and persists it.
- **R6 #5 search filter** (`infra`) — `KnowledgeSearchService` drops
  `searchKnowledgeEligible == false` candidates at the hit-projection step,
  mirroring the Sprint 14 §L0 `isPublished` defense-in-depth pattern.
- **R6 #6 KnowledgeHit** — **SKIPPED** (pre-fix audit): hits are constructed
  post-filter and are therefore eligible-by-construction; carrying the field
  would be always-`true` redundancy. Rationale below.
- **R6 #7 INFO log** (`infra`) — **path α**: emitted by `KnowledgeSearchService`
  per filtered article; `SearchKnowledgeTool` byte-untouched.
- **R6 #8 direct resolve** — `ResolveArticleTool.byArticleId` still returns both
  template articles (test-pinned). Filter is at the search surface only.

**Baselines.** Java `1347 / 1 / 0 / 2` (= pre-change `1337` + 10 R6 tests; sole
failure is the inherited `SystemPromptUserRequestedTiebreakerTest`, OQ-S41.5,
provably uncoupled — a system-prompt content assertion; this sub-sprint touched
zero prompt files). Eval pytest `553` + autoloop pytest `324` UNCHANGED (no
eval-side / autoloop file touched — `git status` evidence). `baseline_dir` and
`docs/current_eval_baseline.md` UNCHANGED.

---

## §1 Per-item detail (file:line ranges + rationale + gating tests)

### Pre-fix audit outcomes

- **#1 (JSON, 2 articles):** both at `search_knowledge_eligible: true`
  (rows 281 / 300). No conflicting governance field. `eligible_uc_scope:
  ["UC-B"]` on both is an orthogonal UC-scope filter (preserved). `published_
  status: true` on both → direct resolve is a successful resolve, NOT an
  unpublished-safe refusal. No `internal_only` / `cs_template` field present.
  **No STOP condition.**
- **#2 (KbArticle):** existing boolean column = `is_published`, a boxed
  `Boolean` + `@Builder.Default = true`. Decision: new field uses **primitive
  `boolean`** + `@Builder.Default = true` so Lombok `@Data` generates the
  `isSearchKnowledgeEligible()` accessor the filter calls (a boxed `Boolean`
  would generate `getSearchKnowledgeEligible()`). No manual getter/setter —
  the entity is Lombok-`@Data`, so accessors are generated (consistent with
  every other field; deviation from the prompt's literal getter/setter snippet
  is intentional and style-consistent).
- **#3 (V17):** V16 (`add_cross_turn_faq_hit_state`) confirmed highest; V17
  chosen. `kb_articles` (V4) has `is_published BOOLEAN NOT NULL DEFAULT true` —
  V17 mirrors that exactly for `search_knowledge_eligible`.
- **#4 (ingestion):** `buildKbArticleFromJson` parses booleans via
  `articleNode.path("<key>").asBoolean(true)` (the `published_status` idiom,
  line 208). Unknown JSON keys are silently ignored by `path(...)` — this is
  why `search_knowledge_eligible: true` was ingested-but-unused on 218/218
  articles. Decision: mirror the `published_status` idiom exactly (NOT the
  prompt's `get(...)` + null-check variant) for in-method consistency.
- **#5 (search service):** single hit-projection lambda (Step 8). The Sprint 14
  §L0 `isPublished` defense-in-depth check inside that lambda is the mirror
  point. `ResolveArticleTool` bypasses the service entirely (direct
  `kbArticleRepository.findById`), and `SearchKnowledgeTool` purely delegates
  to `search(...)` with no filtering of its own — so `KnowledgeSearchService`
  IS the single search surface; there is **no second caller path** that should
  skip the filter. **No STOP condition.**
- **#6 (KnowledgeHit pre/post-filter):** hits are built AFTER the filter (the
  lambda returns `null` for filtered articles, then `.filter(nonNull)`). Every
  surviving `KnowledgeHit` is therefore eligible-by-construction (`true`).
  Decision: **SKIP** the field — it would be always-`true` redundancy. The
  per-article filter trail lives in the INFO log instead. `KnowledgeHit.java`
  left byte-untouched.
- **#7 (INFO log layer α vs β):** the filter happens in `KnowledgeSearchService`,
  and `SearchKnowledgeTool` only receives already-filtered results. Decision:
  **path α** — log at the service layer; `SearchKnowledgeTool` needs no change.
  Correlation keys: `article_id` + `filter_reason` + `session_id` + `turn_index`
  (the `tool_event_id` named in the prompt is a tool-layer concept not available
  at the service layer; `session_id` / `turn_index` are the in-scope `search(...)`
  params and serve the same correlation purpose).

### Implementation

| # | File | Lines | What | Gating test(s) |
|---|------|-------|------|----------------|
| #2 | `server/.../model/KbArticle.java` | 52–63 (field @ 58–61) | primitive `boolean searchKnowledgeEligible` + `@Builder.Default=true` + `@Column(nullable=false)`; Lombok accessor `isSearchKnowledgeEligible()` | (compiled into all below) |
| #3 | `server/.../resources/db/migration/V17__add_kb_search_knowledge_eligible.sql` | new (8 lines) | `ALTER TABLE kb_articles ADD COLUMN search_knowledge_eligible BOOLEAN NOT NULL DEFAULT TRUE` | real-DB rollback proof (§2) |
| #4 | `server/.../knowledge/KnowledgeIngestionRunner.java` | 208–215 (parse), 233 (builder) | parse via `path(...).asBoolean(true)`; `.searchKnowledgeEligible(...)` on builder | `KnowledgeIngestionRunnerTest` (3) |
| #5 | `server/.../knowledge/KnowledgeSearchService.java` | 230–248 (filter+log inside Step-8 lambda) | drop `!isSearchKnowledgeEligible()` candidates; INFO per drop | `KnowledgeSearchServiceTest` (4) |
| #6 | `KnowledgeHit.java` | — | SKIPPED (see audit) | n/a |
| #7 | `KnowledgeSearchService.java` (path α) | 241–246 | structured INFO `article_id / filter_reason / session_id / turn_index` | `KnowledgeSearchServiceTest` (log asserted via behaviour) |
| #1 | `data/knowledge/knowledge_base_articles.json` | rows 281, 300 | `true → false` on the 2 template articles only | `KbArticleEligibilityCorpusTest` (2) |
| #8 | `server/.../tools/ResolveArticleToolTest.java` | +`execute_searchIneligibleArticle_stillResolvesByDirectId` | direct-resolve invariant (anti-误杀 #1) | `ResolveArticleToolTest` (13, +1) |

**Filter design note (no-backfill, pattern-consistent).** The eligibility check
sits in the same lambda as the §L0 `isPublished` check and runs after
`.limit(topResultsLimit)` (topResults=3). With only 2 flagged articles in the
corpus, a filtered template never starves the top-K of eligible content (worst
case: 2 templates + ≥1 eligible in the top-3 → the eligible one still surfaces).
This mirrors the established `isPublished` defense-in-depth behaviour exactly and
keeps the change to the eligibility filter only (no `.limit` move, no
reranker/score change, no `isPublished` behaviour change). `faqMiss` is computed
pre-filter (unchanged), so an all-template top-K can yield `hits=[]` with
`faqMiss=false` — identical to the pre-existing `isPublished` interaction.

---

## §2 Test / build evidence

### Wiring evidence (this sub-sprint's gate)

- **New R6 tests (10, all GREEN):**
  - `KnowledgeIngestionRunnerTest` — 3 (explicit-false / explicit-true /
    absent-defaults-true).
  - `KnowledgeSearchServiceTest` — 4 (drops-ineligible / all-eligible /
    all-ineligible / default-eligible-returned).
  - `KbArticleEligibilityCorpusTest` — 2 (exactly-2-flagged / flagged-stay-
    published).
  - `ResolveArticleToolTest` — +1 (direct resolve of a `false` article still
    succeeds).
- **Regression templates GREEN:** `Sprint14KnowledgeSearchPublishedFilterTest`
  (4), `Sprint14KnowledgeIngestionCanonicalUrlTest` (4),
  `Sprint14ResolveArticleSafetyTest` (4) — my filter did not perturb the
  `isPublished` path.
- **Full Java suite:** `mvn -o test` → `Tests run: 1347, Failures: 1, Errors: 0,
  Skipped: 2`. Sole failure = `SystemPromptUserRequestedTiebreakerTest.
  systemPrompt_marksActiveUcTiebreakerExplicitly` (inherited OQ-S41.5; a
  system-prompt content assertion — zero prompt files touched this sub-sprint).
  Pre-change tree = 1337 (= 1347 − 10 R6 tests); delta is exactly the 10 R6
  tests; **no new failure / no new error introduced.**
- **INFO log fired in test output (verbatim):**
  `search_knowledge filter: article_id=kaTEMP-1
  filter_reason=search_knowledge_eligible=false session_id=sess-elig-1
  turn_index=0`.
- **Flyway V17 — real-DB proof (the unit suite is mock-only and never runs
  Flyway):** applied the V17 `ALTER` against the live dev `kb_articles`
  (218 rows) inside `BEGIN … ROLLBACK`. Result: column lands as
  `boolean / is_nullable=NO / default=true`; all 218 existing rows backfill to
  `TRUE` (`non_true_rows = 0`); ROLLBACK left the dev DB at V16 (column absent)
  so the real Flyway run still applies V17 on next backend boot. Flyway history
  max confirmed V16 pre-run.
- **Grep evidence (forbidden absences):**
  - hardcoded `ka41r000000LIEJAA4` / `ka41r000000LIEEAA4` in `server/src/main`
    → **0 hits**.
  - `(temp)` / `startsWith` in the knowledge/tools/model Java → **0 hits**
    introduced.

### Outcome evidence (DEFERRED — §5.7 separator)

Per the §5.7 mocked-LLM evidence gate: the tests above are **wiring evidence**
(projection / parsing / dispatch / filter-predicate correctness with mocked
rerank LLM). They are NOT primary evidence of a behaviour change on the live
search surface. The **outcome evidence** (real-LLM: the bot no longer surfaces
the `(temp)` templates on c7 / c12 / c17) is the **M-Auto-6 milestone-shared §9
real-LLM re-bless** launched after this sub-sprint closes. **No real-LLM re-bless
was launched at this sub-sprint close.**

---

## §3 STOP / fence confirmations

- File fence respected — 9 files touched, all in-fence (5 modified + 4 new):
  `knowledge_base_articles.json`, `KbArticle.java`, `KnowledgeIngestionRunner.java`,
  `KnowledgeSearchService.java`, `ResolveArticleToolTest.java`, `V17__*.sql`,
  `KbArticleEligibilityCorpusTest.java`, `KnowledgeIngestionRunnerTest.java`,
  `KnowledgeSearchServiceTest.java`. (Pre-existing untracked
  `compact/framework-plan-v3.2-2026-06-06.md` is a separate workstream — NOT
  mine, NOT committed.)
- Only 2 JSON articles changed; only `search_knowledge_eligible` flipped on each
  (`git diff` = +2/−2).
- No hardcoded article-id in Java (grep = 0).
- No title-keyword filter / `(temp)` match in Java (grep = 0).
- No skill yaml / `SkillGuardrailDispatcher` / `resolve_faq_grounded_answer.yaml`
  / `must_cite_source` touch (C-2a's surface).
- No `KnowledgeHit.java` / `SearchKnowledgeTool.java` / `ResolveArticleTool.java`
  (production) / `KbArticleRepository.java` touch.
- No `IntakeFieldsRegistry` / `IntakeFieldsMerger` / `BudgetChecker` /
  `ControlKernel` / `AgentRunLoopImpl` / `ContextProjectionBuilder` /
  `UpdateIntakeFieldsTool` touch (A / C-1 surfaces).
- No UI / `eval_interactive/` / autoloop config touch.
- `ResolveArticleTool` direct resolve still works on both flagged articles
  (test-pinned).
- No new `escalation_reason` enum value. No new Tier-0 invariant.
- `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md` UNCHANGED.
- No real-LLM outcome re-bless launched (deferred to milestone close).

---

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` across all sub-surfaces — corpus data flag
flip (data); Flyway V17 schema migration (schema); KbArticle column (entity);
KnowledgeIngestionRunner field parsing (ingestion); KnowledgeSearchService
eligibility filter (service); KnowledgeHit field propagation (model, conditional
on pre-fix audit — SKIPPED); structured INFO log for filter observability.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. Corpus content
is unchanged (articles remain in JSON + DB); `ResolveArticleTool` direct resolve
is unfiltered (human-CS access preserved); only the LLM-facing search surface
filters. Default visible.

**Semantic hardcode:** No semantic hardcode introduced.
- Filter uses ONLY `KbArticle.isSearchKnowledgeEligible()`. No content scan; no
  title-keyword match on `(temp)`; no body inspection; no LLM call.
- The 2 article-id flips are corpus data edits in the JSON; ZERO hardcoded
  article-id strings in Java code.
- New articles default visible (no behavioural change for unflagged content).
- Reuses an EXISTING data field (`search_knowledge_eligible`, present on 218/218
  articles), NOT a new parallel governance mechanism.

**Generalization coverage:** target / neighbor / negative / shadow case counts:
3 / ~6 / ~8 / 0
- target: c7 / c12 / c17 (`(temp)` article surfaced unwrapped).
- neighbor: any search_knowledge call that would match the 2 flagged articles'
  content (the 2 known articles + zero false positives given the data-field-
  driven filter).
- negative: default-true articles unaffected; missing-field defaults to visible
  (back-compat); direct resolve via `ResolveArticleTool.byArticleId` unchanged
  for both flagged articles; hardcoded ID grep evidence NOT used; title-keyword
  grep evidence NOT used; no skill yaml / wording / `must_cite_source` guardrail
  touch.
- shadow: not applicable (mocked-LLM + ingestion + service tests are wiring
  evidence; real evidence is the M-Auto-6 milestone-shared re-bless after this
  sub-sprint closes).
