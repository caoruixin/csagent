---
title: Sub-sprint C-2b — S-Auto-27 / Sprint 082 — R6 corpus eligibility filter (reuse existing `search_knowledge_eligible` field; plumb through KnowledgeIngestionRunner + KbArticle + V17 Flyway migration + KnowledgeSearchService + KnowledgeHit + SearchKnowledgeTool; flip 2 `(temp)` articles to false)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file + docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md §4.6 (R6; updated re-scope) + compact/sprint-082-dev-prompt.md (self-contained executable view)
last_reviewed: 2026-06-06
review_cadence: per sub-sprint
supersedes: docs/sprints/sprint-081-objective.md
superseded_by: null
notes: >
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
