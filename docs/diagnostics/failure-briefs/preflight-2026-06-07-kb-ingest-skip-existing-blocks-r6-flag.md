# Framework-Defect Brief — preflight-2026-06-07 KB-ingest skip-existing blocks R6 curation flag

> **Brief class:** infra / deployment-path defect (per `iteration_governance.md`
> §2 — surfaced by the §5.9 pre-flight gate, not by a bot trace). The R6
> corpus-curation data flip (`search_knowledge_eligible: true → false` on the
> 2 `(temp)` template articles) cannot land in any already-populated
> `kb_articles` table via the documented "JSON re-ingestion" path.
> **Source artefact:** §0.3 data-flag spot-check of the M-Auto-6 pre-flight
> (`docs/current/process/preflight-eval-checks.md` §0.3), run 2026-06-07
> against the live dev DB (`csagent`) immediately after a clean backend
> restart that applied Flyway V17.
> **Filed:** 2026-06-07 (M-Auto-6 milestone-shared re-bless pre-flight; held
> NO-GO).
> **Layer (§3):** `infra` — server-side knowledge ingestion / data-application
> path (`KnowledgeIngestionRunner`). NOT a bot semantic defect, NOT an
> eval-framework defect.
> **Blocking scope:** blocks **R6 outcome evidence** and therefore the
> M-Auto-6 milestone-shared re-bless. This is an `infra` server-side defect,
> so it does NOT trigger the §5.8 eval-framework global semantic-sub-sprint
> freeze; it gates this specific milestone close because R6's outcome cannot
> be measured until the flag can be applied to the live corpus.

## What happened?

At M-Auto-6 pre-flight (§0.3), after a clean backend restart that applied
Flyway V17 (`V16 → V17 add_kb_search_knowledge_eligible`, confirmed in the
boot log), both R6 template articles read `search_knowledge_eligible = true`
in the DB, and **all 218 `kb_articles` rows read `true`**:

```
article_id          | search_knowledge_eligible | title
--------------------+---------------------------+-------------------------------------
ka41r000000LIEJAA4  | t                         | (temp) Ad removed - By CS (general)
ka41r000000LIEEAA4  | t                         | (temp) NTD Ad Removed Information

search_knowledge_eligible | count
--------------------------+-------
t                         | 218
```

The R6 design (per `V17__add_kb_search_knowledge_eligible.sql` and
`sprint-082-handoff.md`) is: V17 backfills every existing row to
`NOT NULL DEFAULT TRUE`, and the **JSON re-ingestion flips the 2 template
articles to FALSE**. The JSON source of truth is correct — exactly 2
articles carry `search_knowledge_eligible: false`
(`data/knowledge/knowledge_base_articles.json`, the 2 `(temp)` IDs above).

But the re-ingestion never flips them, because
**`KnowledgeIngestionRunner` skips every article already present in the
DB**:

- `KnowledgeIngestionRunner.java:85-88` — collects all existing
  `article_id`s into `existingIds`.
- `KnowledgeIngestionRunner.java:102-105` — `if (existingIds.contains(articleId)) { articlesSkipped++; continue; }`.
- `KnowledgeIngestionRunner.java:139` — `kbArticleRepository.save(article)` runs
  **only for non-skipped (new) articles**. There is no UPDATE path for
  existing rows.

The 2 template articles already exist in the populated dev corpus, so an
`--ingest` run skips them and the flag stays `true`. The runner is an
insert-only loader, not an upsert/reconcile.

The R6 dev-side close never exercised this. Per `sprint-082-handoff.md`
§2, the "real-DB proof" was a `BEGIN … ROLLBACK` migration-mechanics test
only (proved the column lands + backfills to TRUE, then rolled back to
V16), and **outcome evidence was explicitly DEFERRED** to this
milestone-shared re-bless (`sprint-082-handoff.md:183-196`). The R6 unit
tests (`KnowledgeIngestionRunnerTest`, `KnowledgeSearchServiceTest`,
`KbArticleEligibilityCorpusTest`) all use synthetic in-memory articles
(`kaTEMP-1`), so none exercise the insert-only skip against a populated
table.

## What should a good ingestion / data-application path have done?

When a curation flag changes in the source JSON (`search_knowledge_eligible`,
and structurally also `is_published`, `uc_tags`, `eligible_uc_scope`), the
data-application path should **reconcile the existing DB row to the new
flag value** — not silently skip it because the `article_id` already
exists. Concretely, a correct path would, for an article already in the
DB whose immutable content is unchanged, UPDATE the mutable curation
columns from the JSON (without re-chunking / re-embedding unchanged
content), so the live corpus matches the committed source of truth.

The skip-existing optimization is correct for the *content/embedding*
pipeline (don't re-embed unchanged articles), but it incorrectly also
blocks *metadata* reconciliation.

## Why does this matter?

R6's entire observable behaviour depends on at least one corpus article
being `search_knowledge_eligible = false` so the `KnowledgeSearchService`
Step-8 filter (`KnowledgeSearchService.java:230-248`) has something to
drop. With all 218 rows `true`, the filter is a no-op and R6 is
indistinguishable from absent.

If the M-Auto-6 `--n 9` re-bless had launched against this DB state:

- R6 would appear **non-functional** (filter never fires) — a false
  R6-regression read on the milestone's own falsifiable hypothesis.
- Pre-flight anomalies **A1 / A2 / A3** (`docs/current/process/preflight-eval-checks.md`
  §3) would fire spuriously (`search_knowledge hits=[]` interpretation,
  zero filter-decision INFO entries, `(temp)` body leakage), each a
  documented NO-GO — discovered only *after* burning multi-suite `--n 9`
  real-LLM credit. This is precisely the spend the §5.9 pre-flight exists
  to prevent.

It also raises a **production-deployment question**: the same
skip-existing logic means a corpus-curation flag could never be flipped
on an already-deployed article in production either. R6 as shipped has no
working data-application path for its own central data edit.

This bears on Constitution §1.6 ("Eval is evidence, not authority … must
not regress … architecture health"): blessing an R6 baseline measured
against a corpus where the flag never applied would record a verdict that
does not reflect R6's actual behaviour.

## Is this a one-off or a pattern?

**Pattern (structural).** It is deterministic and applies to **every**
curation-flag change on **every** already-ingested article — not just the
2 R6 templates. `is_published`, `uc_tags`, and any future per-article
curation column read by `buildKbArticleFromJson` share the same fate: a
change in JSON never reaches an existing DB row. Reproduces on any
populated `kb_articles` table.

## Which layer is likely responsible?

**`infra`** — server-side knowledge data-application path. Site:
`server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
(`run()` skip-existing at 84-105; `processArticle` insert-only at
131-186). Per §3.2 decision questions: not semantic (§1.3 — the LLM is
given a corpus; the corpus is wrong), not `eval_spec` (the CaseSpec is
fine), not `java_guard` (no Tier-0 invariant). It is persistence / data
wiring → `infra`.

## What should NOT be done?

- **Hand-`UPDATE` the 2 rows in the DB to make the pre-flight pass and
  proceed.** This was offered and explicitly declined (human direction
  2026-06-07). It closes the symptom on these 2 rows for this one run but
  leaves the deployment path broken: the next curation-flag change — and
  production — hits the same wall. The fix must be structural (the runner
  reconciles existing rows), not a per-incident manual patch.
- **`TRUNCATE` + full re-ingest as the standing procedure.** This would
  re-chunk and re-embed all 218 articles (218 embedding-API calls) every
  time a single curation flag changes. Acceptable as a one-off escape
  hatch, wrong as the deployment contract for a metadata edit.
- **Promote skip-existing to "always overwrite on conflict" naively.**
  That would re-embed unchanged content on every ingest and risk
  clobbering DB-side state the JSON does not own. The reconcile must be
  scoped to the mutable curation columns and must not re-embed unchanged
  content.

## Routing decision

Open an `infra` sub-sprint (deliver-agent to scope into
`docs/sprint_objective.md`). Candidate scope:

1. Add a reconcile path to `KnowledgeIngestionRunner` (or a companion
   idempotent reconcile step) that, for an already-present `article_id`
   whose content is unchanged, UPDATEs the mutable curation columns
   (`search_knowledge_eligible`, `is_published`, `uc_tags`, …) from JSON
   without re-chunking / re-embedding. Gate behind an explicit arg
   (e.g. `--reconcile`) so plain `--ingest` keeps its insert-only
   semantics.
2. Anti-误杀 counter-tests against a **populated** table (the current
   unit tests only cover the empty-table insert path): existing row with
   flipped flag is reconciled to `false`; unchanged-content article is
   NOT re-embedded; a content change still re-chunks; `is_published`
   behaviour preserved.
3. After ship: re-run §0.3 on the live dev DB (expect exactly the 2
   `(temp)` rows `false`, 216 rows `true`), then resume the M-Auto-6
   pre-flight from Step 1 (bad_cases smoke).

## Pre-flight checklist contribution (§5.9)

The cheapest read-only check that catches this is the existing §0.3 SQL
spot-check (it caught it today). What is missing is the **root-cause
attribution** in §3: anomaly A3 currently disposes "Data flip didn't land
OR ingestion didn't re-read JSON OR cache is stale." Proposed A3 / §0.3
annotation:

> If §0.3 shows the targeted rows still at the default value AND the rows
> pre-existed the migration, the cause is `KnowledgeIngestionRunner`
> skip-existing (insert-only loader; no UPDATE path for existing rows) —
> NOT a stale JSON commit or cache. Re-ingestion alone will never flip an
> existing-row curation flag. Remediation = reconcile path, not re-run
> `--ingest`.

(Pending addition to `docs/current/process/preflight-eval-checks.md` §3 /
§0.3 per the §6 maintenance rule — held for human confirmation.)

## Provenance

- Investigation: 2026-06-07, M-Auto-6 milestone-shared re-bless pre-flight.
  §0.1 backend restart (stale PID 39786 started 2026-06-05, pre-M-Auto-6;
  fresh boot applied V17) and §0.2 Flyway both PASS; §0.3 NO-GO.
- DB state captured from live `csagent` DB after the fresh boot (218 rows,
  all `t`; both `(temp)` IDs `t`).
- Code: `KnowledgeIngestionRunner.java:68` (`--ingest` gate), `:84-105`
  (skip-existing), `:139` (insert-only save), `:208-215` / `:233` (R6
  parse + builder).
- R6 dev-side close: `docs/sprints/sprint-082-handoff.md` §1 (#4 ingestion),
  §2 (real-DB ROLLBACK proof + DEFERRED outcome evidence).
- Pre-flight verdict record: `docs/diagnostics/2026-06-07-m-auto-6-preflight-verdict.md`.
- Human direction 2026-06-07: hold NO-GO, file this brief, do not
  hand-patch the DB, resolve the data-application path before the re-bless.
