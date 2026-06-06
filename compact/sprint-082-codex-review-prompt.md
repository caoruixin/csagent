# Sprint 082 / S-Auto-27 / M-Auto-6 Sub-sprint C-2b — Codex per-sub-sprint review prompt (Anti-Hardcode + data-field-only filter + direct-resolve invariant + forbidden-grep evidence + pre-fix audit decisions: #6 skip + #7 path α)

## Role identity

You are the **Anti-Hardcode + Per-Sub-Sprint Review Agent** for **Sprint
082 / S-Auto-27 / M-Auto-6 Sub-sprint C-2b** (R6 corpus eligibility
filter reusing existing `search_knowledge_eligible` field).

Your review covers the cumulative commit range
**`bb48aa0^..7773c92`** (5 commits = R6 #2+#3 KbArticle entity field +
V17 Flyway migration + R6 #4 ingestion parser + R6 #5+#7 search-service
filter + INFO log + R6 #1+#8 JSON flip + direct-resolve invariant tests
+ dev handoff). The `^..` notation includes `bb48aa0` per the S-Auto-25
Codex non-blocking observation #1 lesson.

This sub-sprint is **semantic-touching** at the LLM-facing
search-knowledge retrieval surface (R6 narrows the corpus visible to
the LLM by data-field eligibility filter). Per
`iteration_governance.md` §7 + `process/milestone-framework.md` §4.3,
per-sub-sprint Codex review is **REQUIRED**.

**C-2b is the LAST sub-sprint before the M-Auto-6 milestone-shared §9
real-LLM re-bless.** After your verdict + close, deliver-agent drafts
the milestone-shared Codex review prompt
(`compact/M-Auto-6-review-prompt.md`) covering A + B + C-1 + C-2a +
C-2b cumulative range, and the milestone-shared re-bless launches.

## Read order (minimized)

1. `AGENTS.md` (auto-loaded via this prompt).
2. **This prompt** (self-contained per `prompt-artifact-rules.md` §9.1).
3. `docs/sprints/sprint-082-handoff.md` (dev handoff; produced AFTER
   this prompt was drafted — read it to ground per-change verdicts
   against shipped code; specifically read §1 pre-fix audit outcomes
   for #1–#7 + §3 STOP / fence confirmations + the two pre-fix audit
   design choices flagged at #6 / #7).
4. Code anchors only as needed (cited inline in §"Cumulative scope
   claim" below).

## Cumulative scope claim (what the cumulative range delivers)

### Commit 1 — `bb48aa0` — R6 #2 + #3 KbArticle entity field + V17 Flyway migration

**Anchors:**
- `server/src/main/java/com/gumtree/csagent/model/KbArticle.java` —
  new `searchKnowledgeEligible` field with `@Column(name =
  "search_knowledge_eligible", nullable = false)` mirroring the
  existing `is_published` boolean column pattern; Lombok-generated
  `isSearchKnowledgeEligible()` getter via `@Builder.Default = true`.
- `server/src/main/resources/db/migration/V17__add_kb_search_knowledge_eligible.sql` —
  new migration `ALTER TABLE kb_articles ADD COLUMN
  search_knowledge_eligible BOOLEAN NOT NULL DEFAULT TRUE`.

Schema lands cleanly; existing rows backfill to `true` per the
`DEFAULT TRUE`. V16 was the highest pre-existing migration; V17 is
the next number.

**Real-DB verification (per dev report)**: applied against live
`kb_articles` table (218 rows) in a rolled-back transaction; lands
as `boolean / NOT NULL / default true`; all 218 rows backfilled to
`TRUE`; dev DB left at V16 (rollback successful). Verify this claim
matches the handoff §2 evidence + the V17 SQL diff at commit `bb48aa0`.

### Commit 2 — `0594a25` — R6 #4 ingestion parser

**Anchor:** `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
(specifically `buildKbArticleFromJson` method).

Parses the JSON field via the existing `published_status` idiom
(`path(...).asBoolean(true)`); default `true` when absent or null —
back-compat preserved (the field has been silently dropped on 218/218
articles pre-C-2b, so all post-ingestion rows must default to visible
unless explicitly flipped).

Verify:
- The parsing mirrors the existing `published_status` idiom (no
  unexpected change to JSON parsing).
- Default-`true` semantics when the field is absent or null.
- No other field touched by this change.
- No content scan; no per-UC matrix; no hardcoded article_id check.

### Commit 3 — `d21594f` — R6 #5 + #7 search-service filter + INFO log (path α)

**Anchor:** `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`.

The filter is placed **inside the existing Sprint-14 §L0
`isPublished` defense-in-depth lambda** — the candidates → hits
assembly's existing eligibility-style predicate is extended with the
new structural condition (`a -> a.isPublished() &&
a.isSearchKnowledgeEligible()` or equivalent shape). Verify the diff
mirrors the `isPublished` pattern and does not introduce a new code
path.

**Path α** chosen at the #7 design choice: structured backend INFO
log is emitted at the **service** layer (per filtered-out article:
`tool_event_id` if available, `article_id`,
`filter_reason="search_knowledge_eligible=false"`).
`SearchKnowledgeTool.java` is **byte-untouched** (reduces blast
radius; the tool receives only post-filter results).

Verify:
- Filter predicate uses ONLY `KbArticle.isSearchKnowledgeEligible()`.
- No score / reranker / sort-order change.
- No content scan, no title-keyword on `(temp)`, no body inspection.
- INFO log is structured + at INFO level (not WARN/ERROR; not a
  user-facing trace event; no `ToolEvent` shape change).
- `SearchKnowledgeTool.java` shows zero diff in this commit range.

### Commit 4 — `977f9b8` — R6 #1 + #8 JSON 2-article flip + direct-resolve invariant test

**Anchors:**
- `data/knowledge/knowledge_base_articles.json` — `git diff` shape
  EXACTLY `+2 / -2` (one line change per flagged article; both
  `search_knowledge_eligible` field flips from `true → false`).
  Flagged articles: `ka41r000000LIEJAA4` (near row ~272) +
  `ka41r000000LIEEAA4` (near row ~291). NO other article touched. NO
  other field on the 2 flagged articles touched.
- `server/src/test/java/.../tools/ResolveArticleToolTest.java` —
  extended with the direct-resolve invariant: calling
  `ResolveArticleTool.byArticleId(<flagged_id>)` STILL returns the
  article (filter is at the search surface ONLY; human-CS access
  preserved).

**Pre-fix audit #6 outcome**: `KnowledgeHit` field **SKIPPED** per
the dev's design choice (hits are post-filter, so the eligibility
field would always be `true` and carrying it is redundant —
informational redundancy with no observability value).
`KnowledgeHit.java` shows zero diff in this commit range.

### Commit 5 — `7773c92` — dev handoff

`docs/sprints/sprint-082-handoff.md` (270 lines). §0 cold-start
verdict; §1 implementation per #1–#7 with file:line ranges + pre-fix
audit outcomes; §2 test/eval results (Java `1347 / 1 / 0 / 2`; +10
R6 tests all green; sole failure = inherited
`SystemPromptUserRequestedTiebreakerTest` OQ-S41.5 — uncoupled, zero
prompt files touched); §3 STOP/fence confirmations; §7 §7 stanza
verbatim.

## Embedded milestone context

This sub-sprint sits inside M-Auto-6. Predecessors already dev-side
closed 2026-06-06:

- **Sub-sprint A (S-Auto-23 / Sprint 078; R1.a + R2.a + R4.a)** —
  Codex `APPROVE_S_AUTO_23 / 0`.
- **Sub-sprint B (S-Auto-24 / Sprint 079; R3.a + R3.b + R3.c)** —
  §7-EXEMPT visual-verified.
- **Sub-sprint C-1 (S-Auto-25 / Sprint 080; R7 + R2.a#5-ext)** —
  Codex `APPROVE_S_AUTO_25 / 0`; capability-wiring Option-A
  fence-waiver ACCEPTED.
- **Sub-sprint C-2a (S-Auto-26 / Sprint 081; R5 citation contract
  fix)** — Codex `APPROVE_S_AUTO_26 / 0` on targeted re-review
  (substantive verdict PASS on initial review; procedural REJECT on
  clean-tree gate resolved by `8a7cb66`; targeted re-review at
  `54b8729` flipped header + §4 only; §1 / §2 / §3 / §5 preserved
  verbatim).

C-2b (this sub-sprint) is the **last sub-sprint before milestone
close**. After your verdict + close, the milestone-shared §9
re-bless launches.

Milestone class: runtime substrate-hygiene + UI/observability +
intake contract + UX/corpus governance + citation contract
literal→shape semantics fix + corpus eligibility filter (reuse
existing data field). NOT a semantic milestone — no bot prompt
procedure rewrite; no UC routing change; no escalation posture
decision; no judge calibration; no CaseSpec rubric edit. C-2b is
pure infra (corpus data + ingestion + entity + V17 migration +
search-service filter + log).

## Embedded §7 stanza (from the C-2b sprint_objective.md)

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` across all sub-surfaces — corpus
data flag flip (data); Flyway V17 schema migration (schema);
KbArticle column (entity); KnowledgeIngestionRunner field parsing
(ingestion); KnowledgeSearchService eligibility filter (service);
KnowledgeHit field propagation (model, conditional on pre-fix audit
— SKIPPED per #6 design choice); structured INFO log for filter
observability (path α: at service layer; SearchKnowledgeTool
byte-untouched).

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

## Embedded file-path fence (from the C-2b contract)

**Files allowed to edit**:

- `data/knowledge/knowledge_base_articles.json` — ONLY the
  `search_knowledge_eligible` field on the 2 flagged articles; ONLY
  value flip `true → false`. NO other field; NO other article.
- `server/src/main/java/com/gumtree/csagent/model/KbArticle.java`
  (new field + getter / setter only).
- `server/src/main/java/com/gumtree/csagent/model/KnowledgeHit.java`
  (new field + getter / setter only IF pre-fix audit (#6) decides
  to propagate the field; **OTHERWISE skip — the dev report says
  SKIPPED**).
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
  (parse + persist the field only).
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
  (filter step + INFO log only).
- `server/src/main/java/com/gumtree/csagent/service/tools/SearchKnowledgeTool.java`
  (INFO log ONLY if path β chosen at #7; **otherwise byte-untouched
  — the dev report says path α; byte-untouched**).
- `server/src/main/resources/db/migration/V17__add_kb_search_knowledge_eligible.sql`
  (new file; ONE column addition).
- `server/src/test/java/.../KnowledgeIngestionRunnerTest.java`,
  `KnowledgeSearchServiceTest.java`, `SearchKnowledgeToolTest.java`
  (or focused new test classes; dev report names
  `KbArticleEligibilityCorpusTest.java`).
- `server/src/test/java/.../ResolveArticleToolTest.java` (extend
  for direct-resolve invariant only).
- `docs/sprints/sprint-082-handoff.md` (dev handoff).

**Files FORBIDDEN to edit** (any touch = REJECT_S_AUTO_27):

- `KbArticleRepository.java`.
- `ResolveArticleTool.java` (must NOT filter at resolve surface; may
  be touched ONLY in `ResolveArticleToolTest` for the direct-resolve
  invariant; production code byte-untouched).
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
- Autoloop 5-file SHA-locked scoring set.
- `autoloop/config.yaml` `baseline_dir`.
- `docs/current_eval_baseline.md`.

## Embedded §4.1 nine-question anti-hardcode kernel

Walk each question against the cumulative range. Cite specific
file:line or commit hashes in your verdicts.

### Q1 — Semantic decision hardcode?

Did this PR encode a semantic decision (which article should be
surfaced, which UC, etc.) as a fixed rule (keyword / regex / if-else /
enum widening / per-UC matrix) instead of leaving it to data?

For C-2b, examine:
- The filter predicate at `KnowledgeSearchService` — must be
  structurally `eligible != false` (or `eligible == true`) ONLY.
- No title-keyword `startsWith("(temp)")` or similar.
- No hardcoded article_id list in Java.
- No per-UC matrix.
- The 2 article flips are in DATA (JSON), not in Java code.

Pass / fail with anchors.

### Q2 — Tier-0 invariant justification?

For C-2b: no new Tier-0 invariant claimed. Corpus content unchanged;
direct resolve unfiltered; only LLM-facing search surface filters;
default visible. Verify no Tier-0 surface was sneaked in.

Pass / fail with anchors.

### Q3 — Could soft-signal projection suffice?

For C-2b: the filter is a Runtime corpus-governance decision (the
2 `(temp)` articles are operationally CS-only templates with
placeholder text the LLM cannot meaningfully use). A soft signal
in the prompt cannot fix the corpus surface; the wrong-shape
articles must be filtered at the retrieval surface itself.
Alternative (soft signal) would have asked the LLM to recognize
"this article looks like a `(temp)` template" — which is exactly
the §1.7 forbidden "encode eval/CaseSpec text" pattern.

Verify the Runtime-vs-LLM ownership boundary is correctly placed
(Runtime owns the eligibility data field; LLM still owns "which
returned article best answers the user").

Pass / fail with anchors.

### Q4 — Eval/case text encoded?

For C-2b: verify the filter does NOT match on user-message text,
CaseSpec phrases, article title keyword `(temp)`, or article body
content. The ONLY signal must be `KbArticle.searchKnowledgeEligible`.

Pass / fail with anchors.

### Q5 — Semantic ownership moved LLM → Java?

For C-2b: the LLM still decides what to search for, what to cite,
and how to phrase the reply. Runtime narrows the available corpus
by ONE data-field eligibility test. No LLM-owned semantic decision
moved into Java.

Pass / fail with anchors.

### Q6 — Prompt if-else instead of principles?

For C-2b: no prompt / skill yaml change. The skill procedure +
grounding instruction byte-untouched.

Pass / fail with anchors (or "N/A — no prompt edit").

### Q7 — Tool schema / safety / grounding floors preserved?

For C-2b: the `must_cite_source` grounding floor (from C-2a) is
unaffected (C-2b doesn't touch the guardrail). The search-knowledge
tool schema is unchanged (path α — `SearchKnowledgeTool`
byte-untouched). Safety floor (Tier-0 invariants per §1.4)
unchanged.

Pass / fail with anchors.

### Q8 — Generalization eval coverage?

For C-2b: per the embedded §7 stanza, coverage is 3 / ~6 / ~8 / 0
(shadow N/A — wiring evidence; outcome at milestone re-bless).
Verify:
- target (c7 / c12 / c17): each flagged article would no longer
  surface to the LLM via search_knowledge.
- neighbor: 0 false positives — other articles with similar
  content but `search_knowledge_eligible: true` continue to
  surface.
- negative: default-true articles unaffected; missing-field
  defaults to visible; direct resolve via
  `ResolveArticleTool.byArticleId` unchanged.

Pass / fail with anchors.

### Q9 — Temporary hardcode / sunset plan?

For C-2b: no temporary hardcode shipped. The eligibility filter is
a durable structural test; the 2 article flips are durable corpus
edits; the V17 migration is permanent.

Pass / fail.

## Five focal points for this sub-sprint

In addition to the §4.1 walk above, write a verdict on each of
these five focal points with anchored evidence.

### F1 — Data-field-only filter

The filter predicate at `KnowledgeSearchService` must use ONLY
`KbArticle.isSearchKnowledgeEligible()`. No content scan; no
title-keyword on `(temp)` or any string; no body inspection; no
LLM call; no per-UC matrix.

Verify by reading the diff at commit `d21594f`. Confirm the
filter is byte-narrow and structurally adjacent to the existing
`isPublished` defense-in-depth lambda.

### F2 — Direct resolve invariant

`ResolveArticleTool.byArticleId("ka41r000000LIEJAA4")` and
`ResolveArticleTool.byArticleId("ka41r000000LIEEAA4")` MUST still
return the articles. Filter is at the SEARCH surface ONLY;
direct-lookup paths (human-CS access) are preserved.

Verify by reading the diff at commit `977f9b8` for
`ResolveArticleToolTest.java`; confirm at least one test pins each
flagged article's direct-resolve invariant.

### F3 — Forbidden-grep evidence

Grep `server/src/main` for:
- Literal `ka41r000000LIEJAA4` → expected 0 hits.
- Literal `ka41r000000LIEEAA4` → expected 0 hits.
- `(temp)` or `startsWith("(temp)")` → expected 0 hits introduced
  by this sub-sprint (a pre-existing match elsewhere is fine; new
  matches in C-2b's file fence is a §1.7 violation).

Per the dev report, all greps return 0. Re-verify.

### F4 — Back-compat default-true

Verify the chain default-`true` semantics:
- The entity field defaults to `true` (Lombok `@Builder.Default =
  true`).
- The V17 migration `DEFAULT TRUE` backfills existing rows.
- The ingestion parser at commit `0594a25` defaults `true` when
  the JSON field is absent or null.
- The filter predicate treats `null` / missing as visible (NOT
  rejected).

This ensures the 216 unflagged articles remain visible; new
articles default visible; old data without the field remains
visible.

### F5 — No parallel governance field

The contract REQUIRED reuse of the existing `search_knowledge_eligible`
field, NOT introduction of a new `bot_visible` field. Verify the
diff shows NO new field name `bot_visible` (or similar parallel
mechanism) anywhere.

### F6 (BONUS) — Pre-fix audit design decisions

Two decisions were anticipated by the contract as pre-fix audit
outcomes and the dev report selected:
- **#6 SKIPPED** (`KnowledgeHit` field not propagated): the dev's
  rationale is "hits are post-filter, eligible-by-construction".
  Verify `KnowledgeHit.java` shows zero diff in this commit range.
- **#7 path α** (log at service layer; `SearchKnowledgeTool`
  byte-untouched): verify `SearchKnowledgeTool.java` shows zero
  diff in this commit range AND the INFO log emits at
  `KnowledgeSearchService` per the contract's path-α shape.

Both decisions are CONTRACT-ALLOWED. Verify the rationale is
documented in handoff §1 (per the contract's audit-output
requirement).

## Process learnings from S-Auto-26 NBO #1

The S-Auto-26 Codex review surfaced a process learning: "future
yaml-wording pre-fix audits should grep test golden constants in
addition to main-source consumers". C-2b does NOT have a yaml
wording change, so the specific audit gap doesn't apply. But the
analogous principle for C-2b: did the dev's pre-fix audits cover
all consumers of `search_knowledge_eligible`? In particular:

- The dev report claims `KnowledgeIngestionRunner.buildKbArticleFromJson`
  silently dropped the field pre-C-2b (218/218 articles ingested-but-unused).
  Verify by grepping `server/src/main` for `search_knowledge_eligible`
  / `searchKnowledgeEligible` BEFORE the C-2b commits and confirming
  zero references existed pre-C-2b.

This is a SANITY CHECK, not a blocking finding. Surface as a
non-blocking observation if you find any pre-C-2b reference the
dev missed.

## Output format — write the verdict to `docs/codex-findings.md`

Replace the existing top-of-file verdict header in
`docs/codex-findings.md` (currently the S-Auto-26 `APPROVE_S_AUTO_26`
verdict) with the new format (per `iteration_governance.md` §4.2):

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <int>
final_verdict: APPROVE_S_AUTO_27 | APPROVE_S_AUTO_27_WITH_FIXES | REJECT_S_AUTO_27 | NEEDS_HUMAN_ARCHITECTURE_DECISION
summary: <one paragraph>
```

Then write:

- **§1 Per-Change Verdicts** — one verdict per scope item (#1 / #2 /
  #3 / #4 / #5 / #6 SKIPPED + rationale / #7 path α + rationale / #8
  direct-resolve invariant / fence expansions if any).
- **§2 Nine-Question Kernel Walkthrough** — Q1–Q9 with anchored
  verdicts; final §4.1 aggregate verdict at the end.
- **§3 Five Focal-Point Verdicts** — F1 / F2 / F3 / F4 / F5 + F6
  with anchored evidence.
- **§4 Blocking Findings** — None if all gates pass; otherwise
  enumerate.
- **§5 Non-Blocking Observations** — any items that surface but do
  not block (e.g. the S-Auto-26 NBO #1 analogue sanity-check; any
  other minor items).

## Constraints

1. **Do NOT edit any code.** This is a review-only pass. The only
   file you write is `docs/codex-findings.md`.
2. **Do NOT re-judge bad-case suite traces.** The bad-case suite
   primary gate is a human-judgment gate; you do not adjudicate it.
3. **Do NOT widen scope.** If you find scope drift, write it as a
   blocking finding; do NOT attempt to bundle it into your verdict.
4. **Cite anchors** (file:line / commit hash) for every claim. Do
   not assert without evidence.
5. **Audit the intended cumulative range** `bb48aa0^..7773c92` (5
   commits). If you see additional commits in scope, raise it as a
   blocking finding (`scope_drift`).
6. **Verify the working tree is clean** at HEAD before reviewing — if
   uncommitted changes exist (other than your own write to
   `docs/codex-findings.md`), raise it as a `out_of_scope_review`
   finding. (The previously-untracked
   `compact/framework-plan-v3.2-2026-06-06.md` was committed
   preemptively at `056fa5a` to avoid the C-2a clean-tree gate
   regression; verify the working tree is clean at HEAD `7773c92`
   pre-review.)

## Acceptance verdict shapes

- **APPROVE_S_AUTO_27 / blocking_count=0**: all §4.1 questions pass,
  all 6 focal points pass, pre-fix audit decisions (#6 SKIPPED,
  #7 path α) are contract-allowed + documented in handoff §1,
  data-field-only filter verified, direct-resolve invariant
  verified, forbidden-grep evidence clean, V17 migration sound,
  back-compat default-true preserved. Non-blocking observations
  (if any) recorded in §5.
- **APPROVE_S_AUTO_27_WITH_FIXES / blocking_count > 0**: targeted
  small fixes needed. Enumerate each with anchored evidence + a
  one-line fix recommendation.
- **REJECT_S_AUTO_27 / blocking_count > 0**: a blocking finding
  (semantic hardcode introduced; title-keyword filter found; direct
  resolve broken; hardcoded article_id in Java; default-true
  back-compat broken; fence drift). Each finding has anchored
  evidence + a layer classification + a one-line corrective
  direction.
- **NEEDS_HUMAN_ARCHITECTURE_DECISION**: review surfaced a
  decision the deliver-agent should make. Surface as a human
  decision, not a unilateral Codex reject.

When done: ensure `docs/codex-findings.md` carries your verdict
header + body; commit nothing else; hand back to deliver-agent for
close + M-Auto-6 milestone-shared §9 real-LLM re-bless launch.
