# Sprint 082 / S-Auto-27 / M-Auto-6 — Sub-sprint C-2b — Dev Prompt (R6 corpus eligibility filter: plumb existing `search_knowledge_eligible` through KnowledgeIngestionRunner + KbArticle + V17 Flyway migration + KnowledgeSearchService + KnowledgeHit + SearchKnowledgeTool; flip 2 `(temp)` articles to false)

> **Active dev prompt** — Sub-sprint C-2b was promoted to the active
> contract at the M-Auto-6 Sub-sprint C-2a close (2026-06-06; Codex
> `APPROVE_S_AUTO_26 / blocking_count=0` on targeted re-review).
> The canonical contract is `docs/sprint_objective.md` (this prompt
> is its self-contained executable view per
> `docs/current/process/prompt-artifact-rules.md` §9).
>
> Sub-sprints A (R1.a + R2.a + R4.a; archive
> `docs/sprints/sprint-078-objective.md`), B (R3.a + R3.b + R3.c
> admin trace observability; archive
> `docs/sprints/sprint-079-objective.md`), C-1 (R7 + R2.a#5-ext
> intake/clarification runtime contract; archive
> `docs/sprints/sprint-080-objective.md`), and C-2a (R5 citation
> contract fix; archive `docs/sprints/sprint-081-objective.md`)
> are all dev-side closed. C-2b is **the last sub-sprint before
> the M-Auto-6 milestone-shared §9 real-LLM re-bless**. After
> C-2b dev-side close + Codex APPROVE_S_AUTO_27, deliver-agent
> drafts the milestone-shared Codex review prompt
> (`compact/M-Auto-6-review-prompt.md`) covering A + B + C-1 +
> C-2a + C-2b cumulative range, and the milestone-shared re-bless
> launches.

## Role identity

You are the **dev agent** for **Sprint 082 / S-Auto-27 / M-Auto-6
Sub-sprint C-2b**.

Your one-sentence goal: ship R6 — flip the existing ingested-but-unused
`search_knowledge_eligible` data field from `true` to `false` on the
two known `(temp)` template articles
(`ka41r000000LIEJAA4` near JSON row 272 + `ka41r000000LIEEAA4` near
row 291) in `data/knowledge/knowledge_base_articles.json`; plumb the
field through ingestion + entity + Flyway V17 migration + search
service + hit + tool so that `SearchKnowledgeTool` (the LLM-facing
search surface) filters out `search_knowledge_eligible=false`
articles while `ResolveArticleTool` (the direct-by-`article_id`
resolve surface) continues to surface them for human-CS use; emit a
structured backend INFO log on each filter decision for
corpus-curation observability. Zero hardcoded article-id list in
Java; zero title/body keyword filter; data-field driven only;
default (field missing) treated as visible.

## Read order (minimized)

1. `AGENTS.md` (auto-loaded via this prompt).
2. **This prompt** (full sub-sprint contract — do NOT read
   `docs/sprint_objective.md` for scope when this prompt is the active
   contract).
3. **Code anchors only as needed** (cited inline in §Scope below).
4. Before #1 (the JSON edit), run the §Scope #1 PRE-FIX AUDIT —
   confirm the two articles' `search_knowledge_eligible: true` state
   and that no other governance overlap (`eligible_uc_scope` etc.)
   conflicts with the flip.

## Cumulative context (one-page)

- **M-Auto-5 CLOSED 2026-06-05** (Class A;
  `baseline_dir = eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix`).
- **M-Auto-6 ACTIVE**:
  - Sub-sprint A (R1.a + R2.a + R4.a; archive
    `docs/sprints/sprint-078-objective.md`) DEV-SIDE CLOSED 2026-06-06.
  - Sub-sprint B (R3.a + R3.b + R3.c admin trace observability;
    archive `docs/sprints/sprint-079-objective.md`) DEV-SIDE CLOSED
    2026-06-06.
  - Sub-sprint C-1 (R7 + R2.a#5-ext; archive
    `docs/sprints/sprint-080-objective.md`) DEV-SIDE CLOSED
    2026-06-06; Codex `APPROVE_S_AUTO_25 / blocking_count=0`.
  - Sub-sprint C-2a (R5 citation contract fix; planned active at
    `compact/sprint-081-dev-prompt.md`) — current active when this
    prompt is the planning-context view.
  - **C-2b (this sub-sprint)** is the last sub-sprint before the
    M-Auto-6 milestone-shared §9 real-LLM re-bless.
- **Java baseline at sub-sprint launch**: `1327 / 1 / 0 / 2` (sole
  failure = inherited `SystemPromptUserRequestedTiebreakerTest`,
  OQ-S41.5); C-2a may add ~+10-15 tests (no regressions expected).
- **Python baselines**: eval_interactive pytest `553` (548 + 5
  inherited conda-python `test_corpus_lint.py` env-drift, NOT a
  regression); autoloop pytest `324`.
- **Source-of-truth proposal**:
  `docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md`
  §4.6 (R6; updated re-scope reflected here). The proposal predates
  the dev anchor audit and originally called for a new
  `bot_visible` field; the dev audit found the existing
  `search_knowledge_eligible` field already in the data (218/218
  articles) and the human re-scope decision (2026-06-06) is to reuse
  that field instead of introducing a parallel `bot_visible`
  mechanism.
- **Trigger cases**: **R6 (c7 / c12 / c17)** — the bot surfaces a
  `(temp)` template article (`ka41r000000LIEJAA4` "(temp) Ad removed
  - By CS (general)" or `ka41r000000LIEEAA4` "(temp) NTD Ad Removed
  Information") as if it were user-facing content; the article
  contains `XXXXXXXXX` placeholders for CS-agent manual fill-in. The
  LLM ignores or misreads the placeholders, producing fake "your ad
  was removed because..." output. Root cause: corpus governance —
  these articles are operationally CS-only templates that have no
  business surfacing to the bot via search. Data field
  `search_knowledge_eligible` already exists for exactly this
  purpose; the ingestion path silently drops it (the field is
  ingested-but-unused).

## Class (layer classification)

**Layer (per `iteration_governance.md` §3.2):**

- **R6** → `infra`. Five sub-surfaces, all `infra`:
  - Corpus data flag flip (JSON; 2 articles only; existing field).
  - Schema migration (Flyway V17 adds `search_knowledge_eligible`
    column with default `true`).
  - Entity column on `KbArticle.java`.
  - Ingestion parses the field on `KnowledgeIngestionRunner`.
  - Search-time filter on `KnowledgeSearchService` (NOT on
    `ResolveArticleTool` — direct resolve stays unfiltered).
  - `KnowledgeHit` carries the field for observability.
  - `SearchKnowledgeTool` emits structured INFO log on filter
    decision.

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
  on `(temp)`.
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
  `kb_articles` with `NOT NULL DEFAULT TRUE`. Migration runs cleanly
  on fresh DB + on existing DB with pre-existing rows (backfilled
  to `true`).
- **R6 #4 ingestion**: `KnowledgeIngestionRunner.buildKbArticleFromJson`
  parses the field from JSON (`searchKnowledgeEligible = json field
  if present else true`); persists to `KbArticle`. After re-ingestion
  the 2 flagged articles' rows in `kb_articles` have
  `search_knowledge_eligible = false`; all others have `true`.
- **R6 #5 search-time filter**: `KnowledgeSearchService` filters
  candidates by `searchKnowledgeEligible != false` (default-true
  semantics) at the result-assembly step. Articles with `false` are
  NOT returned to the calling tool. Articles with `true` or `null`
  (which the entity will default to `true`) are returned normally.
- **R6 #6 KnowledgeHit observability**: `KnowledgeHit` carries the
  `searchKnowledgeEligible` field so the trace and downstream code
  see the eligibility signal. (Design choice in pre-fix audit: if
  `KnowledgeHit` is constructed only from already-filtered articles,
  the field is always `true` post-filter and can be omitted; if it
  pre-filter shape, the field is informative. Document in handoff.)
- **R6 #7 SearchKnowledgeTool INFO log**: when the search service
  filters an article, `SearchKnowledgeTool` (or the search service
  itself — whichever is the right layer, decided in pre-fix audit)
  emits a structured backend INFO log:
  `tool_event_id, article_id, filter_reason="search_knowledge_eligible=false"`.
  NOT a user-facing trace event (no `ToolEvent` change); backend log
  only, for corpus-curation observability.
- **R6 #8 direct resolve preserved**: `ResolveArticleTool` continues
  to surface the 2 `(temp)` articles when called directly by
  `article_id` (e.g. a human CS agent in the admin UI). The filter
  is at the search surface ONLY.

NOT a goal:

- Adding any new `bot_visible` or other parallel governance field
  (the human re-scope decision is explicit: reuse
  `search_knowledge_eligible`, do NOT introduce a parallel
  mechanism).
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
- Touching skill yaml / prompt wording / `must_cite_source` guardrail
  (C-2a's surface).
- Touching CaseSpec / eval scoring / `baseline_dir` /
  `current_eval_baseline.md`.

## Scope (executable, #1–#8)

### #1 — R6 #1: corpus data flip on 2 `(temp)` articles

**Anchor:** `data/knowledge/knowledge_base_articles.json`
- `ka41r000000LIEJAA4` near row 272
- `ka41r000000LIEEAA4` near row 291

**PRE-FIX AUDIT (MANDATORY before #1):**

- Confirm both articles currently have `"search_knowledge_eligible":
  true` (per the dev anchor audit on the prior STOP).
- Confirm no overlapping governance field on these 2 articles. The
  data already shows `eligible_uc_scope: ["UC-B"]` for both — that's
  a UC-scope filter (not eligibility) and is preserved. Document any
  other potentially-conflicting field in handoff §1.
- Confirm the JSON object shape (keys / casing / null handling) on
  the 2 articles matches the rest of the corpus.

**Change:** flip the field on exactly the 2 articles:

```json
"search_knowledge_eligible": false,
```

(Replacing the existing `true` value on each of the 2 articles.) NO
other field on these 2 articles is touched. NO other article in the
JSON is touched.

Verify with `git diff data/knowledge/knowledge_base_articles.json`:
the diff should show ONLY 2 line changes (one per article), each a
`true → false` swap on the `search_knowledge_eligible` field.

### #2 — R6 #2: `KbArticle` entity column

**Anchor:** `server/src/main/java/com/gumtree/csagent/model/KbArticle.java`

**PRE-FIX AUDIT (MANDATORY before #2):**

- Read the file end-to-end. Confirm existing columns: `article_id`
  (PK), `title`, `summary`, `description`, `source_url`,
  `url_category`, `uc_tags` (text[]), `is_published`,
  `token_count`, `version`, `created_at`, `updated_at`.
- Confirm there is no pre-existing `search_knowledge_eligible` field
  or column.
- Identify the JPA / Hibernate annotation pattern used by the
  existing boolean column (`is_published`) and mirror it for the new
  column.

**Change:** add a new field + column declaration matching the
existing boolean pattern:

```java
@Column(name = "search_knowledge_eligible", nullable = false)
private boolean searchKnowledgeEligible = true;
```

Add the standard JavaBean getter / setter:

```java
public boolean isSearchKnowledgeEligible() {
    return searchKnowledgeEligible;
}

public void setSearchKnowledgeEligible(boolean searchKnowledgeEligible) {
    this.searchKnowledgeEligible = searchKnowledgeEligible;
}
```

**Hard fence:** ONLY the new field + accessors. No other entity
field touched; no other annotation changed.

### #3 — R6 #3: Flyway V17 migration

**Anchor:** `server/src/main/resources/db/migration/`

**PRE-FIX AUDIT (MANDATORY before #3):**

- Confirm the highest existing migration is V16
  (`V16__add_cross_turn_faq_hit_state.sql`). V17 is the next
  number.
- Read `V4__create_kb_articles.sql` to confirm the `kb_articles`
  table name and the existing column types.

**Change:** create
`server/src/main/resources/db/migration/V17__add_kb_search_knowledge_eligible.sql`:

```sql
ALTER TABLE kb_articles
    ADD COLUMN search_knowledge_eligible BOOLEAN NOT NULL DEFAULT TRUE;
```

The `DEFAULT TRUE` backfills existing rows; the `NOT NULL` matches
the entity field's primitive `boolean` type.

**Hard fence:** ONE column addition. No index change, no other
constraint change, no other table touched, no data backfill statement
(the column default handles existing rows; the JSON re-ingestion
handles the 2 flips).

### #4 — R6 #4: `KnowledgeIngestionRunner` parses the field

**Anchor:** `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
(specifically `buildKbArticleFromJson` method around line 196 per
the dev anchor audit; verify exact line range).

**PRE-FIX AUDIT (MANDATORY before #4):**

- Read `buildKbArticleFromJson` end-to-end. Identify the JSON parsing
  idiom for booleans (mirror the `is_published` parsing pattern if
  one exists).
- Confirm the method silently ignores unknown JSON keys (the
  pattern that explains why `search_knowledge_eligible: true` was
  ingested-but-unused for 218/218 articles).

**Change:** parse the field:

```java
JsonNode eligibleNode = articleNode.get("search_knowledge_eligible");
boolean searchKnowledgeEligible = (eligibleNode == null
        || eligibleNode.isNull())
    ? true
    : eligibleNode.asBoolean(true);
kbArticle.setSearchKnowledgeEligible(searchKnowledgeEligible);
```

Default `true` when the field is absent or null (back-compat for
articles that have never had the field).

**Hard fence:** ONLY the new parsing step. No change to the existing
parsing of other fields. No content scan; no per-UC matrix; no
hardcoded article_id check.

### #5 — R6 #5: `KnowledgeSearchService` filters at the search surface

**Anchor:** `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`

**PRE-FIX AUDIT (MANDATORY before #5):**

- Read the file end-to-end. Identify the candidates → hits assembly
  step (where the service returns the final list to the calling tool).
- Confirm the search service is the ONLY upstream caller of
  `SearchKnowledgeTool`; if there are direct callers of the search
  service that should NOT be filtered (e.g. an admin path), surface
  them.
- Confirm `ResolveArticleTool` does NOT go through this service (it
  resolves by `article_id` directly via the repository) — so the
  filter at this layer does not affect direct resolve.

**Change:** add a filter step at the candidates → hits assembly:

```java
List<KbArticle> filtered = candidates.stream()
    .filter(a -> a.isSearchKnowledgeEligible())
    .collect(Collectors.toList());
// continue assembly with filtered
```

(Adapt the stream / collection idiom to match the existing code
style.)

When the filter removes an article, log (decision: at this layer OR
delegate to #7 — pre-fix audit decides which layer is right):

```java
candidates.stream()
    .filter(a -> !a.isSearchKnowledgeEligible())
    .forEach(a -> log.info(
        "search_knowledge filter: article_id={} filter_reason=search_knowledge_eligible=false",
        a.getArticleId()));
```

**Hard fence:** ONLY the eligibility filter. No score change; no
reranker influence; no content scan; no title-keyword match; no
per-UC matrix.

### #6 — R6 #6: `KnowledgeHit` carries the eligibility field

**Anchor:** `server/src/main/java/com/gumtree/csagent/model/KnowledgeHit.java`

**PRE-FIX AUDIT (MANDATORY before #6):**

- Read the file end-to-end. Confirm existing fields:
  `source_id` / `article_id` / `title` / `snippet` /
  `canonical_url` / `canonical_url_missing` / `score` per the dev
  anchor audit (verify against the actual file).
- Decide: does `KnowledgeHit` represent pre-filter candidates or
  post-filter results? If post-filter, the field is always `true`
  and carrying it is informational redundancy — could be omitted.
  If pre-filter, the field is necessary so a downstream caller can
  see the filter outcome. Document the design choice in handoff §1.

**Change** (default path; adapt per audit):

Add `searchKnowledgeEligible` field to the model with the standard
getter / setter; populate it during the candidates → hits assembly in
`KnowledgeSearchService`.

**Hard fence:** if the audit concludes the field is redundant
(everything in `KnowledgeHit` post-filter is eligible-by-construction),
SKIP this step. Document the skip + rationale in handoff §1.

### #7 — R6 #7: `SearchKnowledgeTool` INFO log

**Anchor:** `server/src/main/java/com/gumtree/csagent/service/tools/SearchKnowledgeTool.java`

**PRE-FIX AUDIT (MANDATORY before #7):**

- Read the file end-to-end. Identify whether the tool already has
  per-result observability hooks (e.g. trace event emission for
  each hit). The INFO log should mirror the existing observability
  shape if one exists.
- Decide: log at the tool layer (per-search summary of filtered
  count + article_ids) OR at the search service layer (per-article
  filter event)? Either is acceptable; the latter aligns with the
  `KnowledgeSearchService` filter step. If the log is emitted by
  `KnowledgeSearchService` (#5), the tool layer may NOT need any
  change here. Document the decision in handoff §1.

**Change** (one of the two paths per audit):

Path α — log at the search service (preferred if the service is
where the filter happens; the tool then receives only filtered
results and the filter trail is in the service log). NO change to
`SearchKnowledgeTool`. Mark this step as COMPLETED-BY-#5 in handoff.

Path β — log at the tool (if the service returns pre-filter
candidates and the tool does the filter). Then the tool emits the
INFO log per filtered article.

**Hard fence:** the log is INFO level, structured, NOT a user-facing
trace event (no `ToolEvent` shape change). The log key set is
`tool_event_id` (when available), `article_id`,
`filter_reason="search_knowledge_eligible=false"`.

### #8 — R6 #8: tests + integration smoke

**Anchors (new / extended test files):**

- `server/src/test/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunnerTest.java`
  (or `KnowledgeIngestionRunnerEligibilityTest` if focused) —
  ingestion-side coverage.
- `server/src/test/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchServiceTest.java`
  — search-service-side filter coverage.
- `server/src/test/java/com/gumtree/csagent/service/tools/SearchKnowledgeToolTest.java`
  — tool-side end-to-end coverage (or focused on whichever layer
  ended up emitting the INFO log per path α/β decision).
- `server/src/test/java/com/gumtree/csagent/service/tools/ResolveArticleToolTest.java`
  (if not already extended in C-2a) — assert direct resolve of the
  2 `(temp)` articles still works.
- Data verification test (or integration test) confirming the JSON
  edit landed on exactly 2 articles.

**Tests:**

- **JSON data verification (#1)**:
  - `search_knowledge_eligible` is `false` on `ka41r000000LIEJAA4`
    and `ka41r000000LIEEAA4`.
  - `search_knowledge_eligible` is `true` on a sample of other
    articles (a dozen rows).
  - No other field on the 2 flagged articles changed (assert via
    `git diff` shape OR a JSON-shape diff test).

- **Ingestion (#4)**:
  - Re-ingestion of a sample JSON with `search_knowledge_eligible:
    false` persists `false` on the resulting `KbArticle`.
  - Re-ingestion with `search_knowledge_eligible: true` persists
    `true`.
  - Re-ingestion with the field absent defaults to `true`.

- **Search-service filter (#5)**:
  - Given a candidates list with 1 ineligible + 2 eligible articles,
    the returned hits exclude the ineligible.
  - Given a candidates list with all eligible, hits unchanged.
  - Given a candidates list with all ineligible, hits is empty.
  - Default-true behaviour: a `KbArticle` with default
    `searchKnowledgeEligible = true` is returned.

- **Direct resolve (#8 / R6 #8 invariant)**:
  - `ResolveArticleTool.byArticleId("ka41r000000LIEJAA4")` returns
    the article (filter is at search surface only).
  - `ResolveArticleTool.byArticleId("ka41r000000LIEEAA4")` returns
    the article.

- **Backend rebuild + Flyway migration**:
  - `mvn -o -DskipTests package` → BUILD SUCCESS.
  - `mvn -o test` → migration runs cleanly in the test context;
    Java baseline preserved + R6 additions.

- **Forbidden absences**:
  - Grep `server/src/main` for the literal strings
    `ka41r000000LIEJAA4` and `ka41r000000LIEEAA4` after the change —
    should return ZERO hits in Java code (data-field driven, NOT
    hardcoded ID).
  - Grep for `(temp)` or `startsWith("(temp)")` in Java code —
    should return ZERO hits introduced by this sub-sprint.

## Anti-误杀 invariants (HARD, non-negotiable)

1. **Filter is at SEARCH surface only.** `ResolveArticleTool.byArticleId`
   continues to return the 2 flagged articles. Human-CS access
   preserved.
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
6. **No new parallel governance field.** The human re-scope
   decision: reuse `search_knowledge_eligible`; do NOT introduce
   `bot_visible`.
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

- `data/knowledge/knowledge_base_articles.json` — **ONLY** the
  `search_knowledge_eligible` field on `ka41r000000LIEJAA4` (row
  ~272) and `ka41r000000LIEEAA4` (row ~291); **ONLY** value flip
  `true → false`. NO other field on these 2 articles. NO other
  article in the JSON.
- `server/src/main/java/com/gumtree/csagent/model/KbArticle.java`
  (new field + getter / setter only)
- `server/src/main/java/com/gumtree/csagent/model/KnowledgeHit.java`
  (new field + getter / setter only IF pre-fix audit (#6) decides
  to propagate the field; OTHERWISE skip)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
  (parse + persist the field only)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
  (filter step + INFO log only)
- `server/src/main/java/com/gumtree/csagent/service/tools/SearchKnowledgeTool.java`
  (INFO log ONLY if path β was chosen at #7; otherwise byte-untouched)
- `server/src/main/resources/db/migration/V17__add_kb_search_knowledge_eligible.sql`
  (new file; ONE column addition)
- `server/src/test/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunnerTest.java`
  (extend or new)
- `server/src/test/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchServiceTest.java`
  (extend or new)
- `server/src/test/java/com/gumtree/csagent/service/tools/SearchKnowledgeToolTest.java`
  (extend or new)
- `server/src/test/java/com/gumtree/csagent/service/tools/ResolveArticleToolTest.java`
  (extend for direct-resolve invariant only)
- Any new focused test class (e.g.
  `KbArticleEligibilityCharacterizationTest`) per the test design.
- `docs/sprints/sprint-082-handoff.md` (your dev handoff)

**Files FORBIDDEN to edit**:

- `KbArticleRepository.java` (no repository method change needed; the
  filter is at the service layer).
- `ResolveArticleTool.java` (must NOT filter at resolve surface;
  may be touched ONLY to add a test in a separate `ResolveArticleToolTest`
  if needed — but no production code change).
- Any other article in `knowledge_base_articles.json`.
- Any other field on the 2 flagged articles.
- Any other skill yaml or any line of any skill yaml (C-2a's
  surface).
- `SkillGuardrailDispatcher.java` (C-2a's surface).
- `resolve_faq_grounded_answer.yaml` (C-2a's surface).
- `IntakeFieldsRegistry.java` / `IntakeFieldsMerger.java` /
  `BudgetChecker.java` / `ControlKernel.java` / `AgentRunLoopImpl.java` /
  `ContextProjectionBuilder.java` / `UpdateIntakeFieldsTool.java`
  (A's / C-1's surfaces).
- Any UI file (B's surface).
- Any `eval_interactive/` file (CaseSpecs / simulator / scoring /
  harness).
- Autoloop 5-file SHA-locked scoring set (fence-#13).
- `autoloop/config.yaml` `baseline_dir`.
- `docs/current_eval_baseline.md`.

**STOP conditions**:

- Pre-fix audit on the 2 articles surfaces an overlapping governance
  field with conflicting semantics (e.g. an existing `internal_only`
  or `cs_template` field) → STOP, surface to deliver-agent (decide
  whether to use the discovered field or proceed with
  `search_knowledge_eligible`).
- Pre-fix audit on `KnowledgeIngestionRunner` surfaces that the
  builder takes an unexpected shape (e.g. delegates to a different
  builder class) → STOP, surface (the fence may need narrowing
  before proceeding).
- Pre-fix audit on `KnowledgeSearchService` surfaces a second caller
  path (e.g. an admin endpoint that should NOT filter) → STOP,
  surface (the filter may need to be conditional, or the second path
  may need a different signature).
- The V17 migration fails to apply (column already exists; conflict
  with another sub-sprint's migration) → STOP, surface.
- A test surfaces that `ResolveArticleTool` is being filtered when
  it shouldn't → STOP, anti-误杀 #1 violation.
- The grep evidence shows a hardcoded article_id string in Java →
  STOP, anti-误杀 #3 violation; revert.
- The grep evidence shows a title-keyword filter introduced → STOP,
  anti-误杀 #2 / §1.7 violation; revert.
- Any file outside the fence is touched → STOP, revert, re-launch.

## Test / eval requirements

- All new tests in #8 GREEN.
- Existing Java baseline `1327 / 1 / 0 / 2` (or whatever C-2a leaves
  it at — confirm at sub-sprint launch) preserved + R6 additions
  (~+12-20 tests including ingestion + service-filter + direct-resolve
  invariant + data-shape verification). Sole pre-existing failure
  (`OQ-S41.5`) preserved.
- Flyway migration V17 applies cleanly in the test context.
- Eval pytest `553` UNCHANGED (no eval-side change).
- Autoloop pytest `324` UNCHANGED.
- Backend rebuild + test summary in handoff §2.
- **No real-LLM re-bless at this sub-sprint close.** Outcome evidence
  is the M-Auto-6 milestone-shared re-bless after this sub-sprint
  closes.
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

## Handoff requirements (you author `docs/sprints/sprint-082-handoff.md`)

§1 of the handoff must include:

- For each of #1-#8: file:line ranges + rationale + the test name(s)
  that gate it.
- **Pre-fix audit outcomes** for #1 (no overlapping governance
  field), #2 (existing boolean column pattern), #3 (V16 is highest;
  V17 chosen), #4 (existing JSON parsing idiom; ignored-unknown-keys
  pattern confirmed), #5 (single caller path; ResolveArticleTool
  bypasses), #6 (KnowledgeHit pre-filter vs post-filter design
  choice + decision), #7 (path α vs β for INFO log + decision).
- Java test results (full numeric: passed / failed / skipped /
  errors). Flyway V17 migration applied cleanly.
- Eval pytest / autoloop pytest results (UNCHANGED — re-run not
  required since no eval-side touch; affirm via `git status`
  evidence).
- STOP confirmations:
  - File fence respected; no forbidden file touched.
  - Only 2 JSON articles changed; only `search_knowledge_eligible`
    flipped on each.
  - No hardcoded article-id in Java (grep evidence).
  - No title-keyword filter (grep evidence).
  - No skill yaml / `SkillGuardrailDispatcher` / `must_cite_source`
    touch.
  - No `IntakeFieldsRegistry` / etc. touch.
  - `ResolveArticleTool` direct resolve still works on the 2 flagged
    articles (test evidence).
  - No new `escalation_reason` enum value.
  - `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md`
    UNCHANGED.
  - No real-LLM outcome re-bless launched (deferred to milestone
    close).
- A clear "wiring evidence" vs "outcome evidence" separator per §5.7
  mocked-LLM gate.

## Commit discipline

Recommended commit split (per `prompt-artifact-rules.md` §9):

1. **Commit 1 — R6 #2 + #3 (entity + migration)**: `KbArticle` field
   + V17 SQL migration. Schema lands cleanly; existing rows backfill
   to `true`.
2. **Commit 2 — R6 #4 (ingestion parser)**: `KnowledgeIngestionRunner`
   parses the field; `KnowledgeIngestionRunnerTest` extensions.
3. **Commit 3 — R6 #5 + #6 + #7 (search service filter + hit +
   log)**: `KnowledgeSearchService` filter + INFO log; `KnowledgeHit`
   field (or skipped per audit); `SearchKnowledgeTool` log (if path
   β) or no change (if path α). `KnowledgeSearchServiceTest` +
   `SearchKnowledgeToolTest` extensions.
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

## Self-check checklist (complete BEFORE claiming done)

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
- [ ] Java baseline `1327 / 1 / 0 / 2` + R6 additions, no
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
- [ ] No outcome-evidence re-bless launched at this sub-sprint close.

When all checked: hand back to deliver-agent for close review +
per-sub-sprint Codex dispatch + M-Auto-6 milestone-shared §9 real-LLM
re-bless launch.
