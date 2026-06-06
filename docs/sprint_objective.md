---
title: Sub-sprint S-Auto-28 / Sprint 083 — M-Auto-6 pre-flight-blocker fix — R8 KnowledgeIngestionRunner reconcile path for mutable curation fields
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file (active sub-sprint contract) + docs/milestone_objective.md (M-Auto-6 acceptance bar) + docs/diagnostics/failure-briefs/preflight-2026-06-07-kb-ingest-skip-existing-blocks-r6-flag.md (the blocker brief this sub-sprint closes) + docs/diagnostics/2026-06-07-m-auto-6-preflight-verdict.md (NO-GO verdict) + code (server/.../service/knowledge/KnowledgeIngestionRunner.java)
last_reviewed: 2026-06-07
review_cadence: replaced when next sub-sprint or milestone is promoted
supersedes: [docs/sprints/sprint-082-objective.md]
superseded_by: null
notes: >
  PROMOTED 2026-06-07 ahead of the M-Auto-6 milestone-shared §9 real-LLM
  re-bless because that re-bless is currently **NO-GO**. The prior
  sprint_objective placeholder said "do NOT promote a next sub-sprint until
  the milestone-shared re-bless launches"; that guard is overridden here for
  exactly one reason: the re-bless **cannot launch** — the M-Auto-6
  pre-flight (§5.9) returned NO-GO at §0.3. S-Auto-28 is the prerequisite
  that unblocks it, so it is promoted ahead as a milestone-close BLOCKER.

  Blocker root cause (full brief:
  docs/diagnostics/failure-briefs/preflight-2026-06-07-kb-ingest-skip-existing-blocks-r6-flag.md):
  R6 (S-Auto-27) shipped a corpus-curation flag
  (`search_knowledge_eligible: true → false` on 2 `(temp)` template
  articles) but `KnowledgeIngestionRunner` is **insert-only** — it skips
  every `article_id` already present in `kb_articles`
  (`KnowledgeIngestionRunner.java:84-105`) and never UPDATEs. On the
  populated dev DB the 2 templates remain `true` (all 218 rows `true`), so
  R6's filter is a no-op and R6 outcome evidence cannot be produced. The R6
  dev-side close never caught this: its "real-DB proof" was a
  `BEGIN … ROLLBACK` migration-mechanics test only and its unit tests use
  synthetic articles (`sprint-082-handoff.md:183-196`).

  S-Auto-28 / R8 adds a `--reconcile` data-application mode that UPDATEs the
  mutable curation columns of already-present articles from the JSON
  source-of-truth WITHOUT re-embedding or rewriting content. Human direction
  2026-06-07: the fix MUST be structural (the runner reconciles existing
  rows) — a manual `UPDATE` of the 2 rows is explicitly forbidden as the
  remediation, and manual SQL may NOT be used as close evidence.

  Layer: `infra` (server-side data-application path). The change introduces
  no prompt, routing, escalation, eval-spec, or judge change. Its downstream
  effect IS the same LLM-facing search surface R6 governs, so the §7 stanza
  is included (mirroring the S-Auto-27 conservative call), but no semantic
  decision logic is added. Does NOT trigger the §5.8 eval-framework freeze
  (this is a server-side infra defect, not an eval-framework defect).

  `baseline_dir` (`m-auto-5-baseline-20260604-simfixed-stalledfix`) and
  `docs/current_eval_baseline.md` UNCHANGED throughout — they do not move at
  this sub-sprint close. They flip only at M-Auto-6 milestone close after the
  (now-unblocked) milestone-shared re-bless lands.

  Sequencing after S-Auto-28 ships: rebuild backend → run `--reconcile` →
  re-run §0.3 (expect exactly 2 `(temp)` rows `false`, 216 `true`) → §0.3
  PASS → resume the M-Auto-6 pre-flight from Step 1 (bad_cases smoke) → the
  milestone-shared §9 real-LLM re-bless launches.
---

# Sub-sprint S-Auto-28 / Sprint 083 — R8 KnowledgeIngestionRunner reconcile path

> **Status:** active contract (promoted 2026-06-07). Closes the M-Auto-6
> pre-flight blocker filed at
> `docs/diagnostics/failure-briefs/preflight-2026-06-07-kb-ingest-skip-existing-blocks-r6-flag.md`.
> Gates the M-Auto-6 milestone-shared re-bless (currently NO-GO at §0.3).

## Class

**Layer (per `iteration_governance.md` §3.2):**

- **R8** → `infra` — server-side knowledge data-application path. The
  defect is persistence/data-wiring (an insert-only loader cannot apply a
  curation-flag change to an existing row); the fix is a reconcile/UPDATE
  path. No semantic decision, no prompt, no routing, no eval-spec change.

**§7 stanza requirement:** **INCLUDED (conservative).** R8 itself is pure
infra and is arguably §7-exempt, but its downstream effect is the same
LLM-facing `search_knowledge` surface that R6 (S-Auto-27) governs — R8 is
what makes R6's already-shipped filter actually take effect on the live
corpus. Mirroring the S-Auto-27 call, the full §7 stanza is included below
so Codex can verify scope discipline. Codex may record the infra-exemption
explicitly and `approve`.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. It changes
how DB rows are synced from a committed JSON source-of-truth; it adds no
kernel-level guarantee.

**Semantic hardcode:** No semantic hardcode introduced. The reconcile path
is generic over every JSON article (it iterates the same article list the
insert path does); it contains ZERO hardcoded `article_id`, ZERO
title-keyword match, ZERO content scan. The 2 `(temp)` IDs are touched only
as data already committed in the JSON by R6 — R8 does not re-edit the JSON.

## Goal

After this sub-sprint ships:

- **R8 #1 reconcile method** — `KnowledgeIngestionRunner` gains a reconcile
  path that, for an `article_id` **already present** in `kb_articles`,
  UPDATEs only the **mutable curation columns** from the JSON
  source-of-truth:
  - `search_knowledge_eligible`
  - `is_published`
  - `uc_tags`
  It does NOT update content columns (`title`, `summary`,
  `description`/`content_plain`, `source_url`/canonical Help-Site URL,
  `url_category`, `token_count`) and does NOT touch `kb_chunks` or call the
  embedding client. Design: **load the existing entity → set only the 3
  curation fields from JSON → `save()`** (content columns retain their DB
  values; chunks/embeddings are never recomputed). The runner does NOT
  rebuild a fresh `KbArticle` from JSON for existing rows.
- **R8 #2 `--reconcile` gate** — the reconcile behaviour runs only under an
  explicit `--reconcile` application argument. Under `--reconcile`, an
  article **not** present in the DB still falls through to the existing
  insert path (chunk + embed + save). Under plain `--ingest` (no
  `--reconcile`), behaviour is **byte-for-byte unchanged**: insert-only;
  existing rows skipped and untouched.
- **R8 #3 reconcile observability** — a structured INFO summary at the end
  of a reconcile run reports counts (`articlesReconciled`,
  `articlesInsertedNew`, `articlesUnchanged`) and a per-reconciled-row INFO
  line naming the `article_id` + which curation fields changed
  (`old → new`). Mirrors the existing ingestion logging idiom; NOT a
  user-facing trace event.
- **R8 #4 R6 end-to-end validated on the live dev DB** — after `--reconcile`
  on the populated `csagent` DB, exactly the 2 `(temp)` rows
  (`ka41r000000LIEJAA4`, `ka41r000000LIEEAA4`) read
  `search_knowledge_eligible = false` and the other 216 read `true`;
  `SearchKnowledgeTool` no longer returns the 2 templates; `ResolveArticleTool`
  direct-by-id still returns them (anti-误杀 #1); the §5.9 §0.3 pre-flight
  check PASSES.
- **R8 #5 runbook accuracy** — the 5 drifts caught at the 2026-06-07
  pre-flight are corrected in `docs/current/process/preflight-eval-checks.md`
  (separate docs commit), and the §3 A3 root-cause annotation from the
  blocker brief is added per that file's own §6 maintenance rule.

NOT a goal:

- Re-editing `data/knowledge/knowledge_base_articles.json` (R6 already
  committed the 2 flips; the JSON is the correct source-of-truth — R8 only
  fixes the application path).
- Re-embedding or re-chunking any existing article (reconcile is
  metadata-only).
- Overwriting content columns (`title` / `summary` / `description` /
  `source_url` / `url_category`) on any existing row.
- Deleting any DB row whose `article_id` is absent from the JSON (no
  "prune" semantics; out of scope).
- Changing the R6 search-time filter, the `KbArticle` entity, the V17
  migration, or `ResolveArticleTool` (all shipped + correct).
- Any prompt / skill yaml / routing / escalation / eval-spec / judge change.
- Moving `baseline_dir` or `docs/current_eval_baseline.md`.
- Using a manual SQL `UPDATE` as the close evidence (forbidden — the
  reconcile path itself must produce the DB state).

## Scope (executable, #1–#6)

The self-contained executable dev prompt is authored separately at
`compact/sprint-083-dev-prompt.md` (deliver-agent, per
`prompt-artifact-rules.md` §9.3 sync invariant). The steps below are
canonical.

### #1 — R8 #1: reconcile method (existing-row, metadata-only)

**Anchor:** `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
(`run()` loop at 66-126; existing skip-existing branch at 84-105;
`buildKbArticleFromJson` at 196-239).

PRE-FIX AUDIT before #1:
- Confirm `KbArticleRepository.save()` on a managed/existing-id entity
  performs a full-row UPDATE (so the load-then-modify approach is required
  to avoid clobbering content columns — building fresh from JSON would
  overwrite content).
- Confirm the mutable curation columns are exactly
  `search_knowledge_eligible` (primitive `boolean`), `is_published`
  (`Boolean`), `uc_tags` (`String[]`); confirm content columns to protect.
- Confirm no JPA cascade re-touches `kb_chunks` on an article save (chunks
  are a separate repository write; a bare `kbArticleRepository.save` must
  not cascade to chunks).

Change: add a private `reconcileExisting(JsonNode articleNode, KbArticle
existing)` that loads the existing entity (already in hand from the
existing-ids lookup, or `findById`), sets ONLY the 3 curation fields parsed
from JSON (reuse the existing `path(...).asBoolean(true)` /
`extractUcTagsStatic` idioms), updates `updatedAt`, and `save()`s. It MUST
NOT call `chunkingService` or `embeddingClient`, MUST NOT write `kb_chunks`,
and MUST NOT modify content columns.

### #2 — R8 #2: `--reconcile` application-arg gate

**Anchor:** `KnowledgeIngestionRunner.run()` (the `--ingest` gate at 68).

Change: read a `reconcile` flag from `ApplicationArguments` alongside the
existing `ingest` flag. In the per-article loop:
- if `existingIds.contains(articleId)`:
  - if `reconcile` → call `reconcileExisting(...)`;
  - else → `articlesSkipped++` (unchanged insert-only behaviour).
- else → existing insert path (`processArticle`), under both modes.

**Canonical invocation form (deliver-agent decision 2026-06-07):**
standalone `--reconcile`. Rationale: plain `--ingest` keeps insert-only
semantics; `--reconcile` signals metadata-only update semantics; the two
flags name different modes, not the same mode with a flag stack.
`--ingest --reconcile` as a combo form is non-canonical (do NOT pin it,
do NOT recommend it, do NOT write any test asserting it). At the parser
level the combo is technically accepted because either flag opens the
runner gate, but documentation and tests pin standalone `--reconcile`
only.

Plain `--ingest` (without `--reconcile`) keeps the existing
skip-existing behaviour exactly. The runner-entry gate widens to
`if (!ingest && !reconcile) return;` so `--reconcile` alone opens the
runner; branching inside the per-article loop on `reconcile` (true ⇒
metadata-only update on existing; insert+embed on new). Pin the
canonical form in one unit test, in the dev handoff §1, and in the
Definition-of-done step 2.

### #3 — R8 #3: reconcile observability

Emit per-reconciled-row structured INFO
(`article_id`, changed fields with `old → new`) and an end-of-run summary
(`articlesReconciled` / `articlesInsertedNew` / `articlesUnchanged`).
Mirror the existing ingestion `log.info` idiom (lines 73-125). INFO level;
NOT a `ToolEvent`; NOT user-facing.

### #4 — R8 #4: tests

**(a) populated-DB reconcile regression** (the gap the R6 unit tests
missed — they only exercise the empty-table insert path):
- Seed an existing `kb_articles` row with `search_knowledge_eligible=true`;
  JSON/source declares `false`; run reconcile; assert the row is now
  `false`. Likewise assert `is_published` and `uc_tags` reconcile.

**(b) negative / anti-误杀 tests:**
- **No re-embed on existing reconcile**: `embeddingClient` is NEVER invoked
  and `kbChunkRepository` is NEVER written when reconciling an existing
  article (verify via mock interaction counts).
- **Content columns preserved**: after reconcile, `title` / `summary` /
  `description` / `source_url` / `url_category` on the existing row are
  byte-identical to their pre-reconcile DB values (NOT overwritten from
  JSON even if JSON differs).
- **New id still inserts**: an `article_id` absent from the DB, under
  `--reconcile`, takes the insert path (chunk + embed + save).
- **Plain `--ingest` unchanged**: without `--reconcile`, an existing row is
  skipped and unchanged; `embeddingClient`/`kbChunkRepository` not invoked
  for it.

**(c) R6 end-to-end smoke** (integration / wiring-level where mock-feasible;
real-DB step belongs to the §"Definition of done" sequence): after
reconcile, the 2 `(temp)` ids are `false`; `KnowledgeSearchService` /
`SearchKnowledgeTool` exclude them from search hits; `ResolveArticleTool`
direct-by-id still returns both (anti-误杀 #1).

### #5 — R8 #5: runbook drift fixes (separate docs commit)

**Anchor:** `docs/current/process/preflight-eval-checks.md`. Fix the 5
drifts recorded in the 2026-06-07 verdict
(`docs/diagnostics/2026-06-07-m-auto-6-preflight-verdict.md` §"Drift caught"):
1. DB name `csagent_dev` → `csagent` in §0.2 / §0.3 SQL snippets.
2. Smoke command form `python -m eval_interactive.cli run` → `python -m
   eval_interactive run` (Steps 1/2 + Appendix).
3. Full re-bless command → `cd autoloop && uv run python
   scripts/rebless_baseline.py --n 9 --out-dir
   ../eval_interactive/results/m-auto-N-baseline-shared-$(date +%Y%m%d)/`
   (the script requires cwd=autoloop + `uv run`; its own usage uses `--n 5`).
4. §0.1 detection note: `ps aux | grep '[s]pring-boot'` does NOT match the
   forked `java -cp …` server JVM; detect by port (`lsof -ti:8080`).
5. Add the §3 / A3 root-cause annotation from the blocker brief
   (insert-only runner ⇒ re-ingestion alone never flips an existing-row
   curation flag; remediation = `--reconcile`, not re-run `--ingest`), per
   `preflight-eval-checks.md` §6.

### #6 — R8 #6: dev handoff

`docs/sprints/sprint-083-handoff.md` per the handoff requirements section.

## Anti-误杀 invariants (HARD, non-negotiable)

1. **Direct resolve unaffected.** `ResolveArticleTool.byArticleId` returns
   both `(temp)` articles before and after reconcile.
2. **Reconcile is metadata-only.** It NEVER re-chunks, NEVER re-embeds,
   NEVER writes `kb_chunks` for an existing article.
3. **Content columns immutable under reconcile.** `title` / `summary` /
   `description` / `source_url` / `url_category` on existing rows are never
   overwritten.
4. **Plain `--ingest` is byte-for-byte unchanged.** Insert-only; existing
   rows skipped and untouched.
5. **No prune.** A DB row whose `article_id` is absent from the JSON is
   never deleted.
6. **No hardcoded article-id in Java.** Reconcile is generic over the JSON
   article list; zero `(temp)` / `ka41r…` literals introduced.
7. **No manual SQL as evidence.** The DB state at §0.3 must be produced by
   the `--reconcile` run, not a hand `UPDATE`.
8. **No R6-surface touch.** `KnowledgeSearchService` filter, `KbArticle`
   entity, V17 migration, and the JSON corpus stay byte-unchanged.
9. **No new `escalation_reason` enum value. No new Tier-0 invariant.**
10. **`baseline_dir` UNCHANGED. `docs/current_eval_baseline.md` UNCHANGED.**

## Hard fences / STOP conditions

**Files allowed to edit:**

- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
  (reconcile method + `--reconcile` gate + reconcile logging only).
- `server/src/test/java/.../KnowledgeIngestionRunnerTest.java` (extend /
  new reconcile tests).
- A new test class if cleaner (e.g.
  `KnowledgeIngestionReconcileTest.java`) under the same test package.
- `docs/current/process/preflight-eval-checks.md` (R8 #5 drift fixes +
  A3 annotation only — separate commit).
- `docs/sprints/sprint-083-handoff.md` (dev handoff).
- `compact/sprint-083-dev-prompt.md` (deliver-agent-authored prompt
  artifact; not dev-edited).

**Files FORBIDDEN to edit:**

- `data/knowledge/knowledge_base_articles.json` (R6 already committed the
  flips; correct source-of-truth — do not re-touch).
- `server/.../model/KbArticle.java` (field already exists from R6).
- `server/.../resources/db/migration/V17__*.sql` (shipped; immutable).
- `server/.../service/knowledge/KnowledgeSearchService.java` (R6 filter —
  byte-unchanged).
- `server/.../service/tools/ResolveArticleTool.java` (direct resolve must
  stay unfiltered; touch only `ResolveArticleToolTest` if a regression
  assertion is needed — no production change).
- `server/.../model/KnowledgeHit.java`.
- Any skill yaml / `SkillGuardrailDispatcher` / `resolve_faq_grounded_answer.yaml`.
- `IntakeFieldsRegistry` / `IntakeFieldsMerger` / `BudgetChecker` /
  `ControlKernel` / `AgentRunLoopImpl` / `ContextProjectionBuilder` /
  `UpdateIntakeFieldsTool`.
- Any UI file. Any `eval_interactive/` file. Autoloop 5-file SHA-locked
  scoring set. `autoloop/config.yaml` `baseline_dir`.
  `docs/current_eval_baseline.md`.

**STOP conditions:**

- Pre-fix audit finds `kbArticleRepository.save` cascades to `kb_chunks` →
  STOP, surface (reconcile must not touch chunks).
- Reconcile would require re-embedding to update a curation field → STOP,
  surface (design contradiction).
- Any embedding-client or chunk-repository invocation observed in a
  reconcile-existing test → STOP, anti-误杀 #2 violation.
- A content column changes on an existing row after reconcile → STOP,
  anti-误杀 #3 violation.
- Plain `--ingest` behaviour changes for existing rows → STOP, anti-误杀 #4
  violation.
- Grep shows a hardcoded `ka41r…` / `(temp)` literal in `server/src/main`
  → STOP, anti-误杀 #6 violation; revert.
- Any file outside the fence is touched → STOP, revert, re-launch.

## Test / eval requirements

- All new reconcile tests (R8 #4 a/b/c) GREEN.
- Java baseline preserved: re-measure at launch per
  `[[feedback_remeasure_baselines_prompts_stale]]` (S-Auto-27 close
  recorded `1348 / 1 / 0 / 2` with the sole inherited failure
  `SystemPromptUserRequestedTiebreakerTest` / OQ-S41.5). Net delta should
  be exactly the new reconcile tests; no new failure / error.
- Eval pytest `553` UNCHANGED; autoloop pytest `324` UNCHANGED (no
  eval-side / autoloop change). Note env caveat
  `[[reference_eval_pytest_corpus_lint_conda_python]]` if the reader sees
  491/12 — that is conda-python drift, not a regression.
- Flyway unchanged (no new migration; V17 already applied).
- Backend rebuild + test summary in handoff §2.
- **No real-LLM re-bless at this sub-sprint close.** Outcome evidence is the
  M-Auto-6 milestone-shared re-bless, which resumes AFTER §0.3 passes.
- Wiring-evidence vs outcome-evidence separator per §5.7 documented in the
  handoff.

## §7 Layer-classification + anti-hardcode stanza

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

## Codex review plan (per `process/milestone-framework.md` §4.3)

**Per-sub-sprint Codex review REQUIRED** (conservative — the data-application
path gates R6's LLM-facing search surface, and S-Auto-28 is a milestone-close
blocker). Codex prompt artifact: `compact/sprint-083-codex-review-prompt.md`
(deliver-agent authors at sub-sprint close; embeds §4.1 nine-question kernel
+ the §7 stanza + the file-path fence + anti-误杀 invariants + the focal
points below). Codex may record the infra-exemption and `approve`.

**Focus points for Codex:**

- F1 (metadata-only): reconcile NEVER calls `embeddingClient` / writes
  `kb_chunks` / overwrites content columns — verified by mock-interaction
  tests, not just by assertion.
- F2 (plain `--ingest` unchanged): existing rows skipped + untouched without
  `--reconcile`.
- F3 (no manual SQL evidence): the §0.3 DB state is produced by `--reconcile`,
  evidenced by the reconcile INFO summary, not a hand `UPDATE`.
- F4 (direct resolve invariant): `ResolveArticleTool.byArticleId` returns
  both flagged articles post-reconcile.
- F5 (forbidden-grep): zero `ka41r…` / `(temp)` literals introduced in
  `server/src/main`; R6 surface (search service / entity / V17 / JSON)
  byte-unchanged.
- F6 (no prune): a JSON-absent DB row is not deleted.

## Handoff requirements (dev authors `docs/sprints/sprint-083-handoff.md`)

§1 must include, for each of #1–#6: file:line ranges + rationale + the gating
test name(s); the pre-fix audit outcomes (save-cascade behaviour; mutable vs
content columns; chunk-cascade absence). §2 must include: full Java numeric
results + the reconcile mock-interaction evidence (embeddingClient invocation
count = 0 on existing reconcile); eval/autoloop pytest (UNCHANGED); the
real-DB §0.3 evidence (psql rows: 2 `false` / 216 `true`) produced by the
`--reconcile` run with the reconcile INFO summary quoted; and the explicit
wiring-vs-outcome separator (§5.7). STOP confirmations: fence respected; no
re-embed; content columns preserved; plain `--ingest` unchanged; no manual
SQL as evidence; no hardcoded id; direct resolve preserved; `baseline_dir`
and `current_eval_baseline.md` UNCHANGED; no outcome re-bless launched.

## Commit discipline (per `prompt-artifact-rules.md` §9)

1. **Commit 1 — R8 #1 + #2 + #3** (reconcile method + `--reconcile` gate +
   logging) with the reconcile unit/negative tests (#4 a/b).
2. **Commit 2 — R8 #4(c)** R6 end-to-end wiring test (search exclusion +
   direct-resolve invariant).
3. **Commit 3 — R8 #5** runbook drift fixes + A3 annotation
   (`docs/current/process/preflight-eval-checks.md`) — docs-only.
4. **Commit 4 — R8 #6** dev handoff (`docs/sprints/sprint-083-handoff.md`).

## Self-check checklist (dev completes before claiming done)

- [ ] #1 reconcile method: load-existing → set 3 curation fields → save;
      no `chunkingService` / `embeddingClient` / `kbChunkRepository` call.
- [ ] #2 `--reconcile` gate: existing-row UPDATE only under `--reconcile`;
      plain `--ingest` skip-existing byte-unchanged; insert path for new ids
      under both modes.
- [ ] #3 reconcile INFO logging (per-row changed-fields + run summary).
- [ ] #4(a) populated-DB reconcile regression GREEN (true→false; is_published;
      uc_tags).
- [ ] #4(b) negatives GREEN: no re-embed; content columns preserved; new id
      inserts; plain `--ingest` untouched.
- [ ] #4(c) R6 end-to-end GREEN: search excludes the 2 templates; direct
      resolve returns both.
- [ ] No hardcoded article-id / `(temp)` literal in `server/src/main` (grep).
- [ ] R6 surface (search service / KbArticle / V17 / JSON) byte-unchanged.
- [ ] #5 runbook 5 drifts fixed + A3 annotation added (docs-only commit).
- [ ] Java baseline re-measured + delta = new tests only; eval pytest 553 /
      autoloop 324 unchanged.
- [ ] §7 stanza copied verbatim into handoff.
- [ ] `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md` UNCHANGED.
- [ ] No outcome-evidence re-bless launched.

## Definition of done (close + unblock sequence)

After dev-side close + Codex `APPROVE_S_AUTO_28`:

1. **Rebuild backend** (no hot-reload — `[[feedback_restart_backend_before_eyeball]]`):
   restart so the new reconcile code is on the classpath.
2. **Run reconcile** against the live dev DB using the canonical
   standalone `--reconcile` invocation pinned in §Scope #2:
   `cd <repo> && mvn -o -pl server spring-boot:run -Dspring-boot.run.arguments=--reconcile`.
   Confirm the reconcile INFO summary (`articlesReconciled=2`).
3. **Re-run §0.3** (`docs/current/process/preflight-eval-checks.md`):
   `psql -d csagent -c "SELECT article_id, search_knowledge_eligible FROM
   kb_articles WHERE article_id IN ('ka41r000000LIEJAA4','ka41r000000LIEEAA4');"`
   → expect both `false`; `SELECT search_knowledge_eligible, count(*) …` →
   2 `false` / 216 `true`.
4. **§0.3 PASS → resume** the M-Auto-6 pre-flight from Step 1 (bad_cases
   smoke) → Step 2 (anchor_outcome anti-误杀 sentinel) → Step 3 anomaly scan
   → §4 verdict. On GO, the milestone-shared §9 real-LLM re-bless launches
   (deliver-agent + human).
5. **`baseline_dir` and `docs/current_eval_baseline.md` remain UNCHANGED**
   through this entire sequence; they move only at M-Auto-6 milestone close
   after the milestone-shared re-bless lands.

Active milestone contract: `docs/milestone_objective.md`.
Cold-start state: `docs/10-handoff.md` §0.
