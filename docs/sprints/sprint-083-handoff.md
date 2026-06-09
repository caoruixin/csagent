---
title: Sprint 083 / S-Auto-28 / M-Auto-6 — R8 KnowledgeIngestionRunner --reconcile data-application path (pre-flight blocker fix)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: code (server/.../service/knowledge/KnowledgeIngestionRunner.java) + docs/current/process/preflight-eval-checks.md (R8 #5 runbook fixes)
last_reviewed: 2026-06-07
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  M-Auto-6 milestone-close BLOCKER sub-sprint. Closes
  docs/diagnostics/failure-briefs/preflight-2026-06-07-kb-ingest-skip-existing-blocks-r6-flag.md:
  KnowledgeIngestionRunner was insert-only, so R6's committed
  search_knowledge_eligible: true→false flip on the 2 (temp) templates could
  never land on the already-populated dev corpus (M-Auto-6 pre-flight NO-GO at
  §0.3). R8 adds a metadata-only --reconcile mode that UPDATEs the mutable
  curation columns (search_knowledge_eligible / is_published / uc_tags) of
  existing rows from the JSON source-of-truth WITHOUT re-chunking / re-embedding
  / overwriting content columns. Layer: infra (server-side data-application).
  No prompt / routing / escalation / eval-spec / judge change. §7 stanza
  INCLUDED conservatively (mirrors S-Auto-27) because R8 is what makes R6's
  already-shipped LLM-facing search filter take effect on the live corpus;
  Codex may record the infra-exemption and approve.

  Dev-close gate = wiring evidence (10 new mock-level Java tests + Java baseline
  preserved). Outcome / real-DB §0.3 evidence is PRODUCED IN the Definition-of-
  done sequence (post-Codex APPROVE_S_AUTO_28: rebuild backend → run --reconcile
  → re-run §0.3), NOT at this dev close — it requires a backend rebuild and
  mutates the shared dev DB. baseline_dir + docs/current_eval_baseline.md
  UNCHANGED. No real-LLM re-bless launched.
---

# Sprint 083 / S-Auto-28 / M-Auto-6 — R8 dev handoff

## §0 Cold-start summary + verdict

**Goal.** Add a metadata-only `--reconcile` data-application mode to
`KnowledgeIngestionRunner` so R6's `search_knowledge_eligible: true → false`
flip on the 2 `(temp)` template articles actually lands on the populated dev
`kb_articles` table, unblocking the M-Auto-6 milestone-shared re-bless
(currently NO-GO at §0.3).

**Root cause (recap).** The runner was insert-only: it skips every
`article_id` already present in the DB and has no UPDATE path. On the
populated corpus the 2 templates stayed `true` (all 218 rows `true`), so R6's
search filter had nothing to drop and R6 read as non-functional. Re-running
`--ingest` can never flip an existing-row curation flag.

**Verdict: DONE (dev-side) — wiring evidence green; real-DB §0.3 + outcome
evidence sequenced post-Codex per the Definition-of-done.**

- **R8 #1 reconcile method** (`infra`) — `reconcileExisting(...)`: loads the
  existing managed entity, sets ONLY the 3 mutable curation fields from JSON,
  bumps `updatedAt`, and `save()`s — content columns and the embedding
  pipeline untouched. Saves (and counts "reconciled") ONLY when a curation
  column actually changed.
- **R8 #2 `--reconcile` gate** (`infra`) — runner-entry gate widened to
  `if (!ingest && !reconcile) return;`; per-article loop branches on
  `reconcile`. Standalone `--reconcile` is the canonical metadata-only entry;
  plain `--ingest` is byte-for-byte unchanged (insert-only, skip-existing).
- **R8 #3 reconcile observability** (`infra`) — per-reconciled-row INFO line
  (`article_id` + changed fields `old -> new`) + an end-of-run summary
  (`articlesReconciled=… articlesInsertedNew=… articlesUnchanged=…`).
- **R8 #4 tests** — 10 new mock-level Java tests (9 reconcile/negative + 1
  R6 end-to-end wiring), all GREEN; mock-interaction counts prove no re-embed.
- **R8 #5 runbook accuracy** — 5 drifts fixed in
  `docs/current/process/preflight-eval-checks.md` + the §0.3 / A3 root-cause
  annotation (separate docs commit).
- **R8 #6 handoff** — this file.

**Baselines.** Java full suite `1358 / 1 / 0 / 2` (= prior `1348` + exactly 10
new R8 tests; sole failure is the inherited
`SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`,
OQ-S41.5 — a system-prompt content assertion; this sub-sprint touched zero
prompt files). Eval pytest `553` + autoloop pytest `324` UNCHANGED by
construction (no `eval_interactive/` or `autoloop/` file touched —
`git status` evidence; see §2). `baseline_dir`
(`m-auto-5-baseline-20260604-simfixed-stalledfix`) and
`docs/current_eval_baseline.md` UNCHANGED.

---

## §1 Per-item detail (file:line ranges + rationale + gating tests)

### Pre-fix audit outcomes (completed BEFORE any code — no STOP fired)

- **`save()` is a full-row UPDATE on an existing-id entity.** `KbArticle`
  (`server/.../model/KbArticle.java:16-22`) is a plain `@Data @Entity` with NO
  `@DynamicUpdate`, so `JpaRepository.save()` on a managed/existing-id entity
  merges and Hibernate issues an all-columns UPDATE. **Consequence:** building
  a fresh `KbArticle` from JSON and saving it WOULD clobber the content
  columns → the load-then-modify approach is mandatory. Confirmed.
- **Mutable curation columns** are exactly `searchKnowledgeEligible`
  (primitive `boolean`, `KbArticle.java:58-60`), `isPublished` (`Boolean`,
  `:47-49`), `ucTags` (`String[]`, `:43-45`). **Content columns to protect:**
  `title` (`:28-29`), `summary` (`:31-32`), `description` (`:34-35`),
  `sourceUrl` (`:37-38`), `urlCategory` (`:40-41`), `tokenCount` (`:62-63`)
  (plus entity-managed `version` / `createdAt`). Confirmed.
- **No JPA cascade to `kb_chunks` on an article save.** `KbArticle` has NO
  `@OneToMany`/relationship mapping to `KbChunk`; `KbChunk`
  (`server/.../model/KbChunk.java:28-29`) references the article only via a
  plain `String articleId` column (no `@ManyToOne`). So
  `kbArticleRepository.save(article)` cannot cascade to chunks; chunks are
  written ONLY via `kbChunkRepository.saveAll(...)` in `processArticle`
  (`KnowledgeIngestionRunner.java:330`). Confirmed → no STOP (anti-误杀 #2
  structurally safe).
- **Canonical invocation form.** Standalone `--reconcile` is the metadata-only
  update entry; plain `--ingest` keeps insert-only semantics. The two flags
  name different modes, not the same mode with a flag stack. `--ingest
  --reconcile` is non-canonical (NOT pinned, NOT recommended, NO test asserts
  it); at the parser level the combo is technically accepted because either
  flag opens the runner gate, but docs + tests pin standalone `--reconcile`
  only. Pinned in `KnowledgeIngestionReconcileTest` (`processArticles(..., true)`
  / `(..., false)` cases) and here.

### Implementation

| # | File | Lines (verified) | What | Gating test(s) |
|---|------|------------------|------|----------------|
| #1 | `server/.../service/knowledge/KnowledgeIngestionRunner.java` | `reconcileExisting` @ 216–261; parse idioms reused @ 224 (`search_knowledge_eligible` `asBoolean(true)`), 232 (`published_status` `asBoolean(true)`), 241–245 (`extractUcTagsStatic` + CSV fallback) | load existing → set ONLY 3 curation fields → bump `updatedAt` → `save()`; save+count only when a field changed | `KnowledgeIngestionReconcileTest` (9) |
| #2 | same file | gate @ 80–85 (`if (!ingest && !reconcile) return;`); `existingById` map @ 108–110; per-article branch @ 161–174 (in `processArticles` @ 144–192) | `--reconcile` arg read alongside `--ingest`; existing+reconcile → `reconcileExisting`, existing+ingest → skip (unchanged), new → `processArticle` (both modes) | `KnowledgeIngestionReconcileTest.reconcile_newId_takesInsertPath_andEmbeds`, `.plainIngest_existingRow_skippedAndUntouched_noWriteNoEmbed` |
| #3 | same file | per-row INFO @ 258–259; run-summary INFO @ 115–123 (`articlesReconciled=… articlesInsertedNew=… articlesUnchanged=…` @ 117–118); `IngestionStats` record @ 270–274 | structured INFO per reconciled row + end-of-run counts; mirrors the existing ingestion `log.info` idiom; INFO level, NOT a `ToolEvent` | `processArticles_reconcile_countsChangedVsUnchangedVsInserted` (counts); INFO line observed in test output (§2) |
| #4 | `server/.../service/knowledge/KnowledgeIngestionReconcileTest.java` (new, 9 tests); `server/.../service/knowledge/KnowledgeReconcileEndToEndTest.java` (new, 1 test) | new files | (a) populated-DB regression; (b) negatives w/ mock counts; (c) R6 end-to-end | the 10 tests below |
| #5 | `docs/current/process/preflight-eval-checks.md` | §0.1 (port-detect note), §0.2/§0.3 (DB name), Steps 1/2 + Appendix (smoke cmd), Appendix + front-matter (full re-bless cmd), §0.3 + A3 (root-cause annotation), §7 history row | 5 drift fixes + A3 annotation (separate docs commit) | n/a (docs) |
| #6 | `docs/sprints/sprint-083-handoff.md` | this file | dev handoff | n/a |

**Rationale, per item:**

- **#1 — load-then-modify, conditional save.** The pre-fix audit shows a fresh
  build-from-JSON would clobber content (full-row UPDATE), so reconcile mutates
  the *existing* managed entity. It reuses `buildKbArticleFromJson`'s parse
  idioms inline (not the whole builder) per the contract's "do NOT rebuild from
  JSON for existing rows." `uc_tags` mirrors the insert path EXACTLY
  (`extractUcTagsStatic` + CSV-mapping fallback) so reconcile never clobbers a
  CSV-derived tag set on an article whose JSON omits `uc_tags`. The method
  saves (and bumps `updatedAt`) ONLY when ≥1 curation column actually changed —
  this is what makes `articlesReconciled` report the count of rows whose
  curation *moved* (expected exactly 2 on the live corpus), and avoids 216
  no-op writes. Gating: `KnowledgeIngestionReconcileTest`.
- **#2 — dual-flag gate, shared loop.** The per-article loop was extracted into
  package-private `processArticles(...)` so the insert / skip / reconcile
  branches are unit-testable with mocked repos WITHOUT reading the on-disk
  corpus. `existingById` is now a `Map<String,KbArticle>` (was a `Set<String>`)
  so reconcile has the entity in hand (no per-row `findById`); `--ingest` still
  only needs `containsKey`. Plain `--ingest`: existing rows hit the `else`
  branch → counted unchanged, no write, no chunk/embed — byte-for-byte
  unchanged. Gating: the new-id-insert + plain-ingest-skip tests.
- **#3 — observability.** Per-row INFO names the `article_id` and each changed
  field as `old -> new`; the run summary prints the 3 counts in the
  `articlesReconciled=…` shape the §0.3 evidence quotes. INFO level, service-
  log idiom, never a user-facing trace event.
- **#4 — tests.** See §2.
- **#5 — runbook.** The 5 drifts were caught at the 2026-06-07 NO-GO pre-flight
  (`docs/diagnostics/2026-06-07-m-auto-6-preflight-verdict.md` §"Drift caught");
  the A3 / §0.3 root-cause annotation is the §5.9 "cheapest read-only check"
  contribution from the blocker brief.

---

## §2 Test / build evidence

### Wiring evidence (this sub-sprint's dev-close gate)

**New R8 tests (10, all GREEN):**

`KnowledgeIngestionReconcileTest` (9):
- `reconcile_flipsSearchKnowledgeEligible_trueToFalse_onExistingRow` — #4(a)
  target: the R6 `true→false` flip lands on an existing row; `save(existing)`
  called once; `embeddingClient.embedBatch` / `kbChunkRepository.saveAll`
  never.
- `reconcile_appliesIsPublishedChange_onExistingRow` — #4(a) neighbor.
- `reconcile_appliesUcTagsChange_onExistingRow` — #4(a) neighbor.
- `reconcile_doesNotReEmbedOrWriteChunks_onExistingRow` — #4(b) anti-误杀 #2:
  `embeddingClient.embedBatch` / `embed` never; `kbChunkRepository.saveAll` /
  `save` never (mock-interaction counts).
- `reconcile_preservesContentColumns_evenWhenJsonDiffers` — #4(b) anti-误杀 #3:
  JSON seeds DIFFERENT title/summary/description/source_url/url_category/
  token_estimate; post-reconcile the row's content columns are byte-identical
  to their pre-reconcile values (only the curation flag moved).
- `reconcile_noOpWhenAlreadyInSync_returnsFalse_noSave` — an in-sync row
  issues no write and is counted "unchanged" (supports `articlesReconciled=2`).
- `reconcile_newId_takesInsertPath_andEmbeds` — #4(b): a new id under
  `--reconcile` inserts; `embedBatch` invoked once, `saveAll` once, `save` once.
- `plainIngest_existingRow_skippedAndUntouched_noWriteNoEmbed` — #4(b)
  anti-误杀 #4: without `--reconcile`, an existing row whose JSON declares a
  DIFFERENT flag is skipped + untouched; `save` / `embedBatch` / `saveAll`
  never; the entity's `searchKnowledgeEligible` stays `true`.
- `processArticles_reconcile_countsChangedVsUnchangedVsInserted` — #3 counts:
  1 changed / 1 unchanged / 1 inserted; `embedBatch` invoked exactly once
  (the new article only — never for the 2 existing).

`KnowledgeReconcileEndToEndTest` (1) — #4(c) R6 end-to-end (mock-level):
- `reconcileThenSearch_excludesBothTemplates_butDirectResolveStillReturnsThem`:
  reconcile flips BOTH `(temp)` ids `true→false`; `KnowledgeSearchService.search`
  then drops both from hits while the eligible article surfaces;
  `ResolveArticleTool` direct-by-id STILL returns both templates (anti-误杀 #1).

**Mock-interaction evidence (F1 / anti-误杀 #2 — the key gate):**
On an existing-row reconcile, `embeddingClient.embedBatch` invocation count =
**0** and `kbChunkRepository.saveAll` write count = **0** (verified via
`verify(..., never())` in `reconcile_doesNotReEmbedOrWriteChunks_onExistingRow`
and asserted again in the per-row reconcile tests). The insert path
(`reconcile_newId_takesInsertPath_andEmbeds`) shows `embedBatch` count = **1**
for a NEW id, proving the branch split is correct (not a blanket no-embed).

**Reconcile INFO log fired in test output (verbatim samples):**
```
Knowledge reconcile: article_id=kaX curation updated: search_knowledge_eligible true -> false
Knowledge reconcile: article_id=kaX curation updated: uc_tags [UC-A] -> [UC-B, UC-C]
Knowledge reconcile: article_id=ka41r000000LIEJAA4 curation updated: search_knowledge_eligible true -> false
Knowledge reconcile: article_id=ka41r000000LIEEAA4 curation updated: search_knowledge_eligible true -> false
search_knowledge filter: article_id=ka41r000000LIEJAA4 filter_reason=search_knowledge_eligible=false session_id=sess-e2e turn_index=0
search_knowledge filter: article_id=ka41r000000LIEEAA4 filter_reason=search_knowledge_eligible=false session_id=sess-e2e turn_index=0
```

**Focused run** (`-Dtest=KnowledgeIngestionReconcileTest,KnowledgeReconcileEndToEndTest,KnowledgeIngestionRunnerTest,KnowledgeSearchServiceTest,ResolveArticleToolTest`):
`Tests run: 31, Failures: 0, Errors: 0, Skipped: 0` (9 + 1 new + 3 + 4 + 14).

**Full Java suite** (`mvn -o test`): `Tests run: 1358, Failures: 1, Errors: 0,
Skipped: 2`. Pre-change tree = `1348` (S-Auto-27 close); delta = exactly the
10 new R8 tests; **no new failure / no new error**. The sole failure is the
inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`
(OQ-S41.5) — a system-prompt content assertion, provably uncoupled (this
sub-sprint touched zero prompt files).

**Forbidden-grep gates (zero hits, cited):**
- `grep -rn "ka41r000000LIEJAA4\|ka41r000000LIEEAA4" server/src/main` → **0**
  (article-id literals live only in test + corpus JSON).
- `grep -rn "(temp)" server/src/main` → **0**.
- `grep -n "startsWith" .../KnowledgeIngestionRunner.java` → **0**.

**R6 surface byte-unchanged** (`git diff --name-only HEAD`): the changed set is
`KnowledgeIngestionRunner.java` + the 2 new test files + the runbook doc.
`KnowledgeSearchService.java`, `KbArticle.java`, `V17__*.sql`, and
`data/knowledge/knowledge_base_articles.json` are ABSENT from the diff →
byte-unchanged (anti-误杀 #8).

**Eval / autoloop pytest UNCHANGED by construction.** `git status` shows zero
files touched under `eval_interactive/` or `autoloop/`, so eval pytest `553`
and autoloop pytest `324` are unchanged by construction (not re-run — per
`[[reference_eval_pytest_corpus_lint_conda_python]]` the conda-python drift
would surface `491/12`, which is env noise, not a regression). Flyway unchanged
(no new migration; V17 already on disk + applied).

### Real-DB §0.3 evidence — PENDING the Definition-of-done (post-Codex)

Per §5.7 (wiring-vs-outcome separator) and the contract's Definition-of-done
sequence, the live `--reconcile` run + §0.3 re-check are produced AFTER
dev-side close + Codex `APPROVE_S_AUTO_28` — they require a backend REBUILD
(no hot-reload, `[[feedback_restart_backend_before_eyeball]]`) and mutate the
shared dev DB, so they are NOT run at this dev close. The dev-close gate is the
mock-level wiring evidence above. The exact post-close sequence (deliver-agent
+ human):

```bash
# 1. Rebuild + restart backend (detect prior by PORT per the §0.1 drift fix):
lsof -ti:8080 | xargs -r kill -9
mvn -o -pl server spring-boot:run -Dspring-boot.run.arguments=--reconcile \
    > /tmp/csagent-reconcile.log 2>&1 &
# Expect the reconcile INFO summary:  articlesReconciled=2 articlesInsertedNew=0 articlesUnchanged=216
# 2. Re-run §0.3:
psql -d csagent -c "SELECT article_id, search_knowledge_eligible FROM kb_articles WHERE article_id IN ('ka41r000000LIEJAA4','ka41r000000LIEEAA4');"
psql -d csagent -c "SELECT search_knowledge_eligible, count(*) FROM kb_articles GROUP BY 1;"
# Expect: both flagged IDs false; count → 2 false / 216 true.
```

The §0.3 DB state MUST be produced by this `--reconcile` run (anti-误杀 #7 /
F3) — a manual SQL `UPDATE` is forbidden as evidence. On §0.3 PASS the M-Auto-6
pre-flight resumes from Step 1 (bad_cases smoke).

---

## §3 STOP / fence confirmations

- **File fence respected** — touched: `KnowledgeIngestionRunner.java` (in-fence),
  `KnowledgeIngestionReconcileTest.java` (new, in-fence),
  `KnowledgeReconcileEndToEndTest.java` (new, in-fence),
  `docs/current/process/preflight-eval-checks.md` (in-fence, separate commit),
  `docs/sprints/sprint-083-handoff.md` (this file). No forbidden file touched.
  (The working tree also shows `compact/framework-plan-v4-*` changes — those
  are the SEPARATE aidazi framework workstream, NOT mine, NOT staged in any R8
  commit.)
- **No re-embed** — `embeddingClient.embedBatch` mock count = 0 on existing
  reconcile; `kbChunkRepository.saveAll` count = 0 (anti-误杀 #2).
- **Content columns preserved** — test seeds differing JSON content; the row's
  title/summary/description/source_url/url_category/token_count are unchanged
  post-reconcile (anti-误杀 #3).
- **Plain `--ingest` unchanged** — existing row skipped + untouched, no write,
  no chunk/embed; the JSON flag flip is NOT applied (anti-误杀 #4).
- **No manual SQL as evidence** — the §0.3 state is sequenced to be produced by
  the `--reconcile` run (post-Codex); no hand `UPDATE` used (anti-误杀 #7).
- **No hardcoded id** — grep = 0 for `ka41r…` / `(temp)` in `server/src/main`
  (anti-误杀 #6).
- **Direct resolve preserved** — `KnowledgeReconcileEndToEndTest` asserts both
  `(temp)` ids resolve by id post-reconcile (anti-误杀 #1).
- **No prune** — reconcile only iterates JSON articles and updates/inserts; a
  JSON-absent DB row is never touched/deleted (anti-误杀 #5; no delete call
  exists in the runner).
- **R6 surface byte-unchanged** — search service / `KbArticle` / V17 / JSON
  absent from the diff (anti-误杀 #8).
- **No new `escalation_reason` enum value. No new Tier-0 invariant.**
- **`baseline_dir` UNCHANGED; `docs/current_eval_baseline.md` UNCHANGED.**
- **No real-LLM outcome re-bless launched** (sequenced to M-Auto-6
  milestone-shared re-bless after §0.3 PASS).

---

## §4 Layer-classification + anti-hardcode stanza

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` — server-side knowledge data-application
path (`KnowledgeIngestionRunner`). The defect is an insert-only loader that
cannot apply a curation-flag change to an existing DB row; the fix is a
metadata-only `--reconcile` UPDATE path. No prompt / routing / escalation /
eval-spec / judge change.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. It syncs DB
rows to a committed JSON source-of-truth; it adds no kernel-level guarantee.
`ResolveArticleTool` direct resolve stays unfiltered (anti-误杀 #1); plain
`--ingest` is unchanged.

**Semantic hardcode:** No semantic hardcode introduced. The reconcile path
iterates the same JSON article list the insert path does; ZERO hardcoded
`article_id`, ZERO title-keyword match, ZERO content scan. Reconcile updates
ONLY the mutable curation columns (`search_knowledge_eligible`,
`is_published`, `uc_tags`); content columns and embeddings are untouched.

**Generalization coverage:** target / neighbor / negative / shadow case
counts: 1 / ~2 / ~5 / 0
- target: the 2 `(temp)` articles' DB rows reconcile `true → false` (the R6
  data-application that previously could not land).
- neighbor: `is_published` / `uc_tags` reconcile on an existing row (same
  mechanism, other mutable curation columns).
- negative: existing reconcile does NOT re-embed / NOT write kb_chunks / NOT
  overwrite content columns; new id still inserts+embeds; plain `--ingest`
  leaves existing rows untouched. No hardcoded-id grep hit; no R6-surface
  touch.
- shadow: not applicable (reconcile tests + the §0.3 real-DB check are
  wiring/data evidence; the R6 outcome evidence is the M-Auto-6
  milestone-shared re-bless that resumes after §0.3 PASS).
```

---

## §5 Commit map (per `prompt-artifact-rules.md` §9)

1. **Commit 1 — R8 #1 + #2 + #3** — `KnowledgeIngestionRunner.java` +
   `KnowledgeIngestionReconcileTest.java` (#4 a/b).
2. **Commit 2 — R8 #4(c)** — `KnowledgeReconcileEndToEndTest.java`.
3. **Commit 3 — R8 #5** — `docs/current/process/preflight-eval-checks.md`
   (docs-only).
4. **Commit 4 — R8 #6** — this handoff.

The `compact/framework-plan-v4-*` working-tree changes are deliberately
EXCLUDED from all four commits (separate workstream; files staged by name).
