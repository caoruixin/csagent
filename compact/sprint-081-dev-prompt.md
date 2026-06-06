# Sprint 081 / S-Auto-26 / M-Auto-6 — Sub-sprint C-2 — Dev Prompt (R5 ResolveArticleTool display_citation URL-preferred + R6 corpus bot_visible filter for (temp) template articles)

> **Planning-context artifact**: this dev prompt is drafted at the
> Sub-sprint A close + B open transition (2026-06-06) alongside
> Sub-sprint C-1's `sprint-080-dev-prompt.md` as the planned scope for
> the fourth sub-sprint of M-Auto-6. The CANONICAL active contract is
> `docs/sprint_objective.md`, which currently holds Sub-sprint B. At
> Sub-sprint B close + visual verification, deliver-agent may replace
> `sprint_objective.md` with this sub-sprint's contract (OR with C-1's
> contract; the parallel-launch policy lets either C-1 or C-2 take
> active contract first depending on dev availability).
>
> C-2 may run in parallel with C-1 per the human 2026-06-06 packaging
> decision. Independent surfaces (C-2 = citation UX + corpus retrieval
> governance; C-1 = intake/clarification runtime contract). When both
> are dev-launched in parallel, each lands on its own commit-range to
> keep causal attribution clean for the milestone-shared Codex review
> at M-Auto-6 close.

## Role identity

You are the **dev agent** for **Sprint 081 / S-Auto-26 / M-Auto-6
Sub-sprint C-2**.

Your one-sentence goal: ship R5 — `ResolveArticleTool` returns a
`display_citation` field that prefers the article's `canonical_url`
when present and falls back to `source_id` when absent; the skill yaml
`resolve_faq_grounded_answer.yaml` line 44 `cite_token_field` flips to
`display_citation`; the `must_cite_source` guardrail accepts EITHER a
URL OR a `source_id` so old traces and the ~38 URL-less articles are
not broken — plus R6, a data-field `bot_visible: false` flag on the
two known `(temp)` template articles in
`data/knowledge/knowledge_base_articles.json` plus a `SearchKnowledgeTool`
retrieval filter that hides `bot_visible=false` articles from the LLM
while retaining them in the corpus for human-CS access. Zero semantic
procedure / wording change.

## Read order (minimized)

1. `AGENTS.md` (auto-loaded via this prompt).
2. **This prompt** (full sub-sprint contract — do NOT read
   `docs/sprint_objective.md` for scope when this prompt is the active
   contract).
3. **Code anchors only as needed** (cited inline in §Scope below).

## Cumulative context (one-page)

- **M-Auto-5 CLOSED 2026-06-05** (Class A; baseline_dir =
  `m-auto-5-baseline-20260604-simfixed-stalledfix`).
- **M-Auto-6 ACTIVE**:
  - Sub-sprint A (R1.a + R2.a + R4.a) dev-side closed 2026-06-06;
    `baseline_dir` and `docs/current_eval_baseline.md` UNCHANGED until
    milestone close.
  - Sub-sprint B (R3.a + R3.b + R3.c admin observability) currently
    active OR closed by the time this sub-sprint launches.
  - Sub-sprint C-1 (R7 + R2.a#5-ext) may be running in parallel.
- **Source-of-truth proposal**:
  `docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md`
  §4.5 (R5) + §4.6 (R6) + §6.7 (Sub-sprint C-2 packaging).
- **Trigger cases**:
  - **R5 (c13)** — bot answers with `(Source: ka4P200000003sLIAQ)`
    even though the article has a `canonical_url`. Root cause:
    `resolve_faq_grounded_answer.yaml:44` configures
    `cite_token_field: source_id`; `ResolveArticleTool` returns
    `source_url` but the LLM never sees it as the canonical cite token.
  - **R6 (c7 / c12 / c17)** — bot surfaces a `(temp)` template article
    (`ka41r000000LIEJAA4` "(temp) Ad removed - By CS (general)" or
    `ka41r000000LIEEAA4` "(temp) NTD Ad Removed Information") as if it
    were user-facing content; the article contains `XXXXXXXXX`
    placeholders for CS-agent manual fill-in. The LLM ignores or
    misreads the placeholders, producing fake "your ad was removed
    because..." output.

## Class (layer classification)

**Layer (per `iteration_governance.md` §3.2):**

- **R5** → `infra` (tool result shape) + `prompt_projection` (skill yaml
  one-line `cite_token_field` config swap — contract-shaped, not
  procedure-shaped).
- **R6** → `infra` (data field + retrieval-tool filter).

**§7 stanza requirement:** **REQUIRED** (R5 touches the LLM-facing
citation contract via `cite_token_field`; R6 changes the corpus retrieval
surface visible to the LLM — both semantic-touching per
`iteration_governance.md` §7).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant.

- R5 adds a new tool-result field + a yaml config-key swap; the existing
  `must_cite_source` grounding guardrail's intent (require an attributable
  cite token) is preserved — only the preferred token format changes.
- R6 adds a data field + a retrieval filter; the existing corpus content
  is unchanged; retrieval surface to humans is unchanged.

**Semantic hardcode:** No semantic hardcode introduced.

- R5's `display_citation` field derives ENTIRELY from the article's
  structured fields (`canonical_url` if non-null/non-blank, else
  `source_id`); zero content matching of article body or user message.
- R5's yaml flip is a one-line config swap (`cite_token_field:
  display_citation`), NOT a procedure rewrite, NOT a wording change,
  NOT a per-UC matrix. The yaml change is the cleanest contract-shaped
  edit possible.
- R5's `must_cite_source` fallback adds a SECOND accepted token shape
  (either URL or source_id); zero keyword matching.
- R6 uses a `bot_visible: bool` data field driven by manual corpus
  curation. NOT a runtime title-keyword match on "(temp)" — that would
  be a forbidden §1.7 keyword shortcut. The 2 known `(temp)` articles
  are explicitly flagged in data; new articles default to `bot_visible:
  true` (no behavioural change for unflagged content).
- R6's `SearchKnowledgeTool` filter is a simple `article.bot_visible !=
  false` predicate — no semantic decision, no content scan.

## Goal

After this sub-sprint ships:

- **R5 wiring**: `ResolveArticleTool` returns a `display_citation` field
  on every article result: `canonical_url` if present and non-blank,
  else `source_id` as fallback;
  `resolve_faq_grounded_answer.yaml:44` `cite_token_field` reads
  `display_citation`; `must_cite_source` guardrail accepts either a URL
  shape or a `source_id` shape (back-compat with old traces + 38
  URL-less articles). LLM-facing replies cite URLs when available,
  source_ids otherwise — without breaking grounding contract.
- **R6 wiring**: the 2 known `(temp)` template articles in
  `data/knowledge/knowledge_base_articles.json` (`ka41r000000LIEJAA4` at
  row 273 and `ka41r000000LIEEAA4` at row 292) carry `"bot_visible":
  false`; `SearchKnowledgeTool` filters retrieval to exclude
  `bot_visible=false` hits; the trace records the filter decision so
  observability is preserved (a tool event indicates the article was
  available in corpus but suppressed from the LLM). Human-CS retrieval
  paths are unchanged.

NOT a goal:

- Rewriting `resolve_faq_grounded_answer.yaml` procedure / wording
  (anything beyond the single-line `cite_token_field` swap is out of
  scope — defer to autoloop).
- Adding NLP / keyword filters on article titles or bodies (R6 is a
  data-field flag; no runtime keyword inference).
- Deleting `(temp)` articles from corpus (humans may still need them).
- Auto-flagging more articles as `bot_visible: false` (the data-team
  decision is OUT of scope; this sub-sprint flags only the 2 known
  trigger cases).
- Modifying how `must_cite_source` checks grounding beyond accepting
  the second token shape.
- Any UI surface (R3.* is Sub-sprint B's surface).
- Any intake / DISCOVER / clarification path (Sub-sprint C-1's surface).

## Scope (executable, #1–#7)

### #1 — R5 step 1: `ResolveArticleTool` returns `display_citation`

**Anchor:** `server/src/main/java/.../tools/ResolveArticleTool.java`
lines 95-110 (where the tool builds its result body — verify the
exact line range; the proposal §4.5 cites this neighborhood).

**Change:** add a `display_citation` field to the tool's result body:

```java
String displayCitation = (article.getCanonicalUrl() != null
        && !article.getCanonicalUrl().isBlank())
    ? article.getCanonicalUrl()
    : article.getSourceId();
result.put("display_citation", displayCitation);
```

Preserve the existing `source_id` and `source_url` (or `canonical_url`
— verify the exact field name) fields on the result; `display_citation`
is ADDITIVE.

**Why:** c13 surface — the LLM cited `(Source: ka4P200000003sLIAQ)`
because that was the cite token the skill yaml told it to use. With
`display_citation` derived deterministically from the article structure,
the LLM has a single field to cite that automatically prefers URL.

### #2 — R5 step 2: skill yaml `cite_token_field` flip

**Anchor:** `server/src/main/resources/.../resolve_faq_grounded_answer.yaml`
line 44 (per the proposal §4.5 reference; verify the line is the
`cite_token_field` config row).

**Change:** flip the line from `cite_token_field: source_id` to
`cite_token_field: display_citation`.

**Hard fence:** this is the ONLY yaml change permitted in M-Auto-6
(per the milestone §1 + §6 yaml exception). NO other yaml line is
touched. Verify with `git diff` before commit.

**Why:** points the existing skill procedure at the new
`display_citation` field. No procedure / wording change — the cite
template stays identical, only the data field name changes.

### #3 — R5 step 3: `must_cite_source` guardrail fallback to source_id

**Anchor:** the `must_cite_source` guardrail implementation — likely
under `server/src/main/java/.../guardrails/` or similar (verify the
exact file via a pre-fix audit; if the guardrail validates citation
shape, that's the change point; if it just confirms presence of ANY
cite token, no fallback is needed).

**Change:** the guardrail must accept BOTH:
- A URL-shaped cite token (i.e. starts with `http://` or `https://`).
- A `source_id`-shaped cite token (alphanumeric Salesforce-style ID,
  matching the existing accept shape).

If the current guardrail only validates URL shape OR only validates
source_id shape, extend to accept both via a structural OR condition.

**Hard fence:** the structural validation MUST be STRICT — accept
exactly the two shapes (URL-prefixed string OR alphanumeric ID). NO
content-keyword matching, NO LLM-based validation. This is a
back-compat add only — old traces with `source_id` cites still pass,
new traces with URL cites pass too.

**Why:** ~38 URL-less articles exist; old traces have `source_id`
cites that should not retroactively break the grounding floor.

### #4 — R5 step 4: tests for #1 + #2 + #3

**Anchor (new / extended test files):**

- `server/src/test/java/.../tools/ResolveArticleToolTest.java` (existing
  or new) — extend with `display_citation` field tests.
- `server/src/test/java/.../guardrails/MustCiteSourceFallbackTest.java`
  (new or extend existing must_cite_source test).
- A grounding-integration test (if one exists) confirming the end-to-end
  flow with the new yaml token works.

**Tests** (positive + negative):

R5 #1:
- Positive A — article with non-null/non-blank `canonical_url` →
  `display_citation` equals the URL.
- Positive B — article with null `canonical_url` →
  `display_citation` equals `source_id`.
- Positive C — article with blank `canonical_url` (empty string) →
  `display_citation` equals `source_id`.
- Negative A — `source_id` field UNCHANGED on the result body (existing
  consumers still work).
- Negative B — `source_url` / `canonical_url` field UNCHANGED on the
  result body.

R5 #2:
- Yaml diff test (or a config-load smoke): the loaded skill config has
  `cite_token_field = "display_citation"`.

R5 #3:
- Positive A — guardrail accepts a URL-shaped cite token.
- Positive B — guardrail accepts a source_id-shaped cite token.
- Negative A — guardrail rejects a plain English phrase as a cite token
  (no semantic content allowed in place of a cite token).
- Negative B — guardrail rejects empty/null cite token (grounding floor
  preserved).

### #5 — R6 step 1: `bot_visible: false` data field on the 2 `(temp)` articles

**Anchor:** `data/knowledge/knowledge_base_articles.json` —
`ka41r000000LIEJAA4` near row 273 and `ka41r000000LIEEAA4` near row
292 (per proposal §4.6 citations; verify the exact rows).

**Change:** add `"bot_visible": false` as a top-level field to each
article's JSON object. Do NOT modify any other field on these
articles. Do NOT add the field to any other article (the default is
`true`).

Example post-change shape (illustrative):

```json
{
  "source_id": "ka41r000000LIEJAA4",
  "title": "(temp) Ad removed - By CS (general)",
  "description": "...XXXXXXXXX...",
  "canonical_url": null,
  "bot_visible": false,
  ...
}
```

**Why:** explicit data-field flag for retrieval filtering; data-driven
not keyword-driven (anti-误杀 §1.7).

### #6 — R6 step 2: `SearchKnowledgeTool` retrieval filter

**Anchor:** `server/src/main/java/.../tools/SearchKnowledgeTool.java`
(verify the exact path; the proposal §4.6 cites this tool as the
retrieval surface).

**Change:** add a filter step in the retrieval path that excludes
articles whose `bot_visible` field equals `false`. Articles without
the `bot_visible` field (the default state) are TREATED AS visible
(default `true` semantics).

Reference Java shape (illustrative):

```java
List<KnowledgeArticle> filtered = candidates.stream()
    .filter(a -> a.getBotVisible() == null || a.getBotVisible())
    .collect(Collectors.toList());
```

Trace observability: when the filter removes an article, emit a
structured log entry (NOT a user-facing trace event — backend log
only, INFO level) with: `tool_event_id, article_source_id,
filter_reason="bot_visible=false"`. This is for ops / corpus-curation
observability.

**Hard fence:** the filter MUST NOT use any signal other than the
`bot_visible` data field. No title-keyword check (no "startsWith
(temp)" or similar — that is a forbidden §1.7 shortcut). No body
inspection. No reranker influence.

### #7 — R6 step 3: tests for #5 + #6

**Anchor (new / extended test files):**

- `server/src/test/java/.../tools/SearchKnowledgeToolTest.java` (existing
  or new) — extend with `bot_visible` filter tests.

**Tests** (positive + negative):

R6 #5 — data verification:
- The 2 flagged articles have `bot_visible: false` after the JSON
  edit; ALL OTHER articles either omit the field (default true) or
  retain whatever they had.

R6 #6 — filter behaviour:
- Positive A — when a search query would return one of the 2 flagged
  articles, the filter removes it; the response does NOT include it.
- Positive B — when the same query also matches `bot_visible=true` or
  un-flagged articles, those are RETAINED in the response.
- Negative A — `bot_visible: true` articles are returned normally.
- Negative B — articles WITHOUT the `bot_visible` field are returned
  normally (default visible).
- Negative C — the corpus itself is unchanged (loading by `source_id`
  directly via `ResolveArticleTool` STILL surfaces the article — the
  filter is at the SEARCH / retrieval surface, not the resolve surface,
  so human-CS lookup paths continue working).

## Anti-误杀 invariants (HARD, non-negotiable)

1. **R5 `must_cite_source` fallback PRESERVES the grounding floor.**
   Empty / null cite tokens are STILL rejected; plain English phrases
   are STILL rejected. Only the structural shape allowed widens to
   accept URL OR source_id.
2. **R5 yaml change is ONE LINE.** The `cite_token_field` config key
   flip is the ONLY yaml edit. No procedure / wording / skill structure
   change anywhere in this sub-sprint.
3. **R5 `display_citation` derivation is STRUCTURAL.** Derived from
   `canonical_url` (if non-null and non-blank) vs `source_id` fallback;
   NO content scan of article body or title; NO inference; NO LLM call.
4. **R6 filter is DATA-FIELD driven.** The only signal is
   `article.bot_visible`. NO title-keyword match on "(temp)" or any
   other string; NO body inspection. New articles default visible
   (no behavioural change for unflagged content).
5. **R6 article retention.** The 2 flagged articles remain in the
   corpus JSON file; only the LLM-facing retrieval surface filters
   them. Human-CS direct-resolve paths (via `source_id` lookup) continue
   working.
6. **R6 filter observability.** When the filter removes an article,
   log structured evidence so corpus curation has a paper trail. This
   is INFO log only (not behavioural).
7. **No semantic procedure / wording change.** The only yaml line
   touched is `cite_token_field`. No bot prompt edit, no UC routing
   change, no escalation posture change, no judge calibration change.
8. **Anti-误杀 sentinels preserved.** Anchor uc_g_gdpr / uc_h_appeal /
   uc_i_payment / uc_j_safety + shadow cs38s* must stay at 0.000 stable
   at the milestone-shared re-bless. (Verified at close, not at this
   sub-sprint.)
9. **`baseline_dir` UNCHANGED. `docs/current_eval_baseline.md`
   UNCHANGED.** Both flip at milestone close.

## Hard fences / STOP conditions

**Files allowed to edit** (fence):

- `server/src/main/java/.../tools/ResolveArticleTool.java`
- `server/src/main/java/.../tools/SearchKnowledgeTool.java`
- `server/src/main/java/.../guardrails/<MustCiteSource>.java` (or
  wherever the guardrail lives; pre-fix audit identifies the file)
- `server/src/main/resources/.../resolve_faq_grounded_answer.yaml` —
  **ONLY** the `cite_token_field` config-key line
- `data/knowledge/knowledge_base_articles.json` — **ONLY** the 2
  flagged articles, **ONLY** the `bot_visible` field add
- `server/src/test/java/.../tools/ResolveArticleToolTest.java` (extend
  or new)
- `server/src/test/java/.../tools/SearchKnowledgeToolTest.java` (extend
  or new)
- `server/src/test/java/.../guardrails/MustCiteSourceFallbackTest.java`
  (new) or extension to the existing must_cite_source test
- `docs/sprints/sprint-081-handoff.md` (your dev handoff)

**Files FORBIDDEN to edit**:

- Any OTHER yaml file or any OTHER line of
  `resolve_faq_grounded_answer.yaml`.
- Any OTHER article in `knowledge_base_articles.json`.
- `IntakeFieldsRegistry.java`.
- `SkillGuardrailDispatcher.java`.
- `BudgetChecker.java`.
- `ControlKernel.java` (Sub-sprint C-1's surface).
- `AgentRunLoopImpl.java`.
- `ContextProjectionBuilder.java`.
- `FormContextIngestionService.java`.
- Any UI file (R3.* is Sub-sprint B's surface).
- Any `eval_interactive/` file (CaseSpecs / simulator / scoring /
  harness).
- Autoloop 5-file SHA-locked scoring set (fence-#13).
- `autoloop/config.yaml` `baseline_dir`.
- `docs/current_eval_baseline.md`.

**STOP conditions**:

- R5 #3 pre-fix audit surfaces that `must_cite_source` is implemented
  via an LLM-based check or some other unexpected shape → STOP and
  surface to deliver-agent.
- R6 #5 verification surfaces that the 2 `(temp)` articles already
  have OTHER governance flags (e.g. an existing `internal_only` field)
  that overlap with `bot_visible` semantics → STOP and surface (the
  data team may have a different convention).
- R6 retrieval filter affects MORE than the 2 flagged articles when
  tested → STOP (the default `bot_visible: true` semantics is broken).
- A test surfaces that the `cite_token_field` flip breaks an existing
  must-cite test because the grounding floor was tighter than expected
  → STOP (the fallback in #3 needs to be tightened or scope adjusted).
- Any forbidden file is touched → STOP, revert the touch, re-launch.

## Test / eval requirements

- All new tests in #4 + #7 GREEN.
- Existing test baselines unchanged: Java `1244 / 1 / 0 / 2` baseline +
  the new R5 + R6 tests (delta: +~10-12 tests). Sole pre-existing
  failure (`OQ-S41.5`) preserved.
- Eval pytest `553 passed, 0 failed` UNCHANGED (no eval-side change in
  this sub-sprint).
- Autoloop pytest UNCHANGED.
- **No real-LLM re-bless at this sub-sprint close.** Outcome evidence
  is the M-Auto-6 milestone-shared re-bless after C-1 + C-2 both land.
- Mocked-LLM tests for the citation-token surface (R5 yaml flip) are
  wiring evidence per §5.7; real evidence is the milestone re-bless.

## §7 Layer-classification + anti-hardcode stanza

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (R5 `ResolveArticleTool` returns
`display_citation` field; `must_cite_source` guardrail accepts URL OR
source_id fallback; R6 `bot_visible` data field + `SearchKnowledgeTool`
retrieval filter) + `prompt_projection` (R5 one-line
`cite_token_field` config-key swap in skill yaml — contract-shaped, not
procedure-shaped).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. R5
preserves the grounding floor (must_cite_source widens shape; does NOT
weaken the requirement to cite); R6 preserves corpus content (only
LLM-facing retrieval surface filters; human-CS direct-resolve paths
unchanged).

**Semantic hardcode:** No semantic hardcode introduced.
- R5's `display_citation` derives ENTIRELY from structured article
  fields (canonical_url presence → URL; absence → source_id fallback).
  Zero content scan, zero keyword match, zero per-UC matrix.
- R5's yaml flip is a ONE-LINE config-key swap (`cite_token_field:
  source_id` → `display_citation`). NOT a procedure rewrite, NOT a
  wording change.
- R5's `must_cite_source` fallback widens the accepted cite token
  shape from one to two (URL OR source_id). Structural validation
  only — no semantic / content / keyword check on the cite content.
- R6's filter uses ONLY the `article.bot_visible` data field. NOT a
  title-keyword match on "(temp)" or any other string. New articles
  default visible (no behavioural change for unflagged content).
- R6's data flag is applied to exactly 2 articles
  (`ka41r000000LIEJAA4` + `ka41r000000LIEEAA4`) per explicit corpus
  curation. NOT a runtime classification, NOT a content scan.

**Generalization coverage:** target / neighbor / negative / shadow case
counts: 4 / ~6 / ~8 / 0
- target: c13 (R5 cite token); c7 / c12 / c17 (R6 `(temp)` article
  surface).
- neighbor: every FAQ-grounded-resolve case that cites an article
  (R5 is universal); every search_knowledge call that might surface a
  `(temp)` article (R6 — 2 known articles).
- negative: R5 must_cite_source negatives (empty / null / plain English
  rejected); R5 source_id fallback preserved for URL-less articles
  (38 known); R6 default-visible articles unaffected; R6 human-CS
  direct-resolve unchanged (article still in corpus).
- shadow: not applicable (mocked-LLM tests are wiring evidence; real
  evidence is milestone-shared re-bless).
```

## Codex review plan (per `process/milestone-framework.md` §4.3)

**Per-sub-sprint Codex review REQUIRED** because R5 touches the
LLM-facing citation contract (`cite_token_field` is a prompt_projection
surface) and R6 touches the LLM-facing retrieval surface (search
results visible to the bot). Both are semantic-touching per
`iteration_governance.md` §7 + `process/milestone-framework.md` §4.3.

Codex prompt artifact: `compact/sprint-081-codex-review-prompt.md`
(deliver-agent authors at sub-sprint close, embeds §4.1 nine-question
kernel + §7 stanza + file-path fence + anti-误杀 invariants +
generalization coverage).

**Focus points for Codex** (per §4.3):

- Q1: confirm R5 `display_citation` derivation is structural
  (canonical_url-vs-source_id presence check) with NO content scan;
  confirm R6 filter uses ONLY `article.bot_visible` and NO title
  keyword.
- Q3: confirm R5 yaml change is ONE LINE (`cite_token_field` config
  swap) and NOT a procedure rewrite; confirm R6 filter point surfaces
  observability (log) when filtering.
- Q4: not applicable (no multi-turn state).
- Q5: confirm no semantic decision moved from LLM to Java (LLM still
  decides whether / how to cite; runtime now provides a better cite
  token; LLM still searches; runtime filters known-bad corpus entries
  but does NOT reshape results).
- Q7: confirm grounding floor preserved (`must_cite_source` still
  requires a cite token of one of two structural shapes; empty / plain
  English still rejected); confirm safety floor unchanged.
- Q8: confirm generalization coverage matches the §7 stanza counts.
- Q9: no temporary hardcode; all changes are durable.

## Handoff requirements (you author `docs/sprints/sprint-081-handoff.md`)

§1 of your handoff must include:

- For each of #1-#6: file:line ranges + rationale + the test name(s)
  that gate it.
- **#3 pre-fix audit outcome**: where `must_cite_source` lives + its
  current structural validation; confirm the fallback addition does
  NOT weaken the grounding floor.
- **#5 data verification**: the 2 article rows post-edit; a JSON-shape
  snapshot showing only the `bot_visible` field was added.
- **#6 retrieval evidence**: a search query result before/after the
  filter (mocked tool test) showing the 2 articles excluded.
- **R5 sample LLM-facing projection**: at least 2 sample tool result
  bodies (one with canonical_url present → URL cite; one URL-less →
  source_id fallback) showing the `display_citation` field correctly
  populated.
- Java test results (full numeric: passed / failed / skipped / errors).
- Eval pytest / autoloop pytest results (UNCHANGED).
- STOP confirmations:
  - File fence respected; no forbidden file touched.
  - `resolve_faq_grounded_answer.yaml` changed ONE LINE only
    (`cite_token_field`).
  - `knowledge_base_articles.json` changed ONLY 2 articles, ONLY
    `bot_visible` field added.
  - No semantic procedure / wording change.
  - No new `escalation_reason` enum value.
  - `IntakeFieldsRegistry` / `SkillGuardrailDispatcher` /
    `BudgetChecker` / `ControlKernel` / `AgentRunLoopImpl` /
    `ContextProjectionBuilder` UNCHANGED.
  - `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md`
    UNCHANGED.
  - No real-LLM outcome re-bless launched (deferred to milestone close).
- A clear "wiring evidence" vs "outcome evidence" separator per §5.7
  mocked-LLM gate.

## Commit discipline

Recommended commit split (per `prompt-artifact-rules.md` §9):

1. **Commit 1 — R5 #1 + #3 + #4 (server)**: `ResolveArticleTool`
   `display_citation` field + `must_cite_source` guardrail fallback +
   tests.
2. **Commit 2 — R5 #2 (yaml flip)**: `resolve_faq_grounded_answer.yaml`
   one-line config-key swap.
3. **Commit 3 — R6 #5 + #6 + #7**: `knowledge_base_articles.json`
   `bot_visible` flag on 2 articles + `SearchKnowledgeTool` filter +
   tests.
4. **Commit 4 — Dev handoff**: `docs/sprints/sprint-081-handoff.md`.

Each commit message follows the established convention (see sprint-077
+ sprint-078 commits as reference). All commits are by the dev agent;
deliver-agent commits its own close-archive artifacts separately at
sub-sprint close.

## Self-check checklist (complete BEFORE claiming done)

- [ ] Each of #1-#6 implemented with file:line ranges captured in
      handoff §1.
- [ ] All new tests in #4 + #7 GREEN.
- [ ] R5 `display_citation` derivation tests: canonical_url → URL;
      null/blank → source_id fallback.
- [ ] R5 yaml flip: `cite_token_field: display_citation` (one line,
      `git diff` confirms no other yaml change).
- [ ] R5 `must_cite_source` fallback: URL accepted; source_id accepted;
      empty / plain English rejected.
- [ ] R6 data flag: 2 articles (`ka41r000000LIEJAA4` +
      `ka41r000000LIEEAA4`) have `bot_visible: false`; no other article
      changed.
- [ ] R6 filter: bot_visible=false hides from LLM-facing search;
      bot_visible=true and default-visible articles returned normally;
      human-CS resolve path STILL surfaces the article.
- [ ] R6 observability: filter decision logged with article_source_id +
      filter_reason.
- [ ] Java baseline `1244 / 1 / 0 / 2` + new tests, no regressions.
- [ ] Eval pytest `553` unchanged.
- [ ] No file outside the file fence touched.
- [ ] No semantic procedure / wording change anywhere.
- [ ] `IntakeFieldsRegistry` / `SkillGuardrailDispatcher` /
      `BudgetChecker` / `ControlKernel` / `AgentRunLoopImpl` /
      `ContextProjectionBuilder` UNCHANGED.
- [ ] §7 stanza copied verbatim into the handoff.
- [ ] `baseline_dir` UNCHANGED.
- [ ] `docs/current_eval_baseline.md` UNCHANGED.
- [ ] No outcome-evidence re-bless launched at this sub-sprint close
      (the milestone-shared re-bless runs after C-1 + C-2 both land).

When all checked: hand back to deliver-agent for close review +
per-sub-sprint Codex dispatch + (if C-1 not yet closed) Sub-sprint C-1
status check before milestone close trigger.
