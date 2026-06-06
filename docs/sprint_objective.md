---
title: Sub-sprint C-2 — S-Auto-26 / Sprint 081 — R5 ResolveArticleTool display_citation URL-preferred + R6 corpus bot_visible filter for (temp) template articles
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file + docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md §4.5 (R5) + §4.6 (R6) + §6.7 (Sub-sprint C-2 packaging) + compact/sprint-081-dev-prompt.md (self-contained executable view)
last_reviewed: 2026-06-06
review_cadence: per sub-sprint
supersedes: docs/sprints/sprint-080-objective.md
superseded_by: null
notes: >
  Sub-sprint C-2 of M-Auto-6 (Runtime substrate hygiene + admin
  observability + intake/clarification contract + UX/corpus governance).
  Activated 2026-06-06 after Sub-sprint C-1 dev-side close (Codex
  `APPROVE_S_AUTO_25 / blocking_count=0`; capability-wiring Option-A
  fence-waiver accepted). Sub-sprint A (R1.a + R2.a + R4.a) is
  dev-side closed and smoke-verified per
  `docs/sprints/sprint-078-objective.md`. Sub-sprint B (R3.a + R3.b +
  R3.c) is dev-side closed and visual-verified per
  `docs/sprints/sprint-079-objective.md`. Sub-sprint C-1 (R7 +
  R2.a#5-ext) is dev-side closed and Codex-approved per
  `docs/sprints/sprint-080-objective.md`. The outcome-evidence re-bless
  waits for C-2 to land — single milestone-shared re-bless at
  M-Auto-6 close.

  Scope (citation UX + corpus governance; semantic-touching at the
  citation contract surface via R5 yaml flip):
  - R5 (proposal §4.5) — `ResolveArticleTool` returns an additive
    `display_citation` field (canonical_url if present and non-blank,
    else source_id fallback); one-line
    `resolve_faq_grounded_answer.yaml:44` `cite_token_field` swap
    (`source_id` → `display_citation`); `must_cite_source` guardrail
    fallback to accept EITHER a URL shape OR a source_id shape
    (back-compat with old traces + 38 URL-less articles).
  - R6 (proposal §4.6) — `bot_visible: false` data-field flag on the
    2 known `(temp)` template articles in
    `data/knowledge/knowledge_base_articles.json`
    (`ka41r000000LIEJAA4` "(temp) Ad removed - By CS (general)" +
    `ka41r000000LIEEAA4` "(temp) NTD Ad Removed Information");
    `SearchKnowledgeTool` retrieval filter excludes
    `bot_visible=false` hits from the LLM-facing surface while
    retaining the articles in corpus for human-CS direct-resolve;
    structured INFO log on filter decision.

  Per-sub-sprint Codex review REQUIRED (R5 touches the LLM-facing
  citation contract via `cite_token_field`; R6 touches the
  LLM-facing retrieval surface — both semantic-touching per
  `iteration_governance.md` §7).

  After C-2 dev-side close, the M-Auto-6 milestone-shared §9 real-LLM
  re-bless launches. Paired-evidence against
  `m-auto-5-baseline-20260604-simfixed-stalledfix`; `baseline_dir`
  and `docs/current_eval_baseline.md` flip at milestone close.

  Forbidden: any semantic procedure / wording change; ANY OTHER yaml
  edit beyond the single `cite_token_field` line; ANY OTHER article
  in `knowledge_base_articles.json` beyond the 2 flagged ones; ANY
  OTHER field on the 2 flagged articles beyond `bot_visible`; any
  new `escalation_reason` enum value; any title-keyword or content
  scan in the R6 filter (data-field driven ONLY); any LLM-based
  citation validation in the R5 guardrail (structural-shape ONLY); any
  `IntakeFieldsRegistry` / `SkillGuardrailDispatcher` / `BudgetChecker` /
  `ControlKernel` / `AgentRunLoopImpl` / `ContextProjectionBuilder`
  touch (those are C-1's / earlier surfaces); any UI file (B's
  surface); any `eval_interactive/` file; autoloop 5-file SHA-locked
  scoring set; `baseline_dir` or `docs/current_eval_baseline.md` move
  (both flip at M-Auto-6 milestone close).

  `baseline_dir` UNCHANGED (`m-auto-5-baseline-20260604-simfixed-stalledfix`);
  `docs/current_eval_baseline.md` UNCHANGED. NO outcome-evidence
  re-bless at this sub-sprint close — that runs at the M-Auto-6
  milestone close after C-2 lands.
---

# Sub-sprint C-2 — S-Auto-26 / Sprint 081 — R5 + R6

## Class

**Layer (per `iteration_governance.md` §3.2):**

- **R5** → `infra` (tool result shape addition; `must_cite_source`
  guardrail structural fallback) + `prompt_projection` (one-line skill
  yaml `cite_token_field` config-key swap — contract-shaped, not
  procedure-shaped).
- **R6** → `infra` (data field + retrieval-tool filter).

**§7 stanza requirement:** **REQUIRED** (R5 touches the LLM-facing
citation contract via `cite_token_field`; R6 changes the corpus
retrieval surface visible to the LLM — both semantic-touching per
`iteration_governance.md` §7). Full stanza in §7 below.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant.

- R5 adds a new tool-result field + a yaml config-key swap; the
  existing `must_cite_source` grounding guardrail's intent (require an
  attributable cite token) is preserved — only the preferred token
  format changes and the accepted token shape widens (URL OR
  source_id).
- R6 adds a data field + a retrieval filter; the existing corpus
  content is unchanged; retrieval surface to humans is unchanged.

**Semantic hardcode:** No semantic hardcode introduced.

- R5's `display_citation` derives ENTIRELY from structured article
  fields (`canonical_url` if non-null and non-blank, else `source_id`);
  zero content matching of article body or user message.
- R5's yaml flip is a one-line config swap (`cite_token_field:
  display_citation`), NOT a procedure rewrite, NOT a wording change.
- R5's `must_cite_source` fallback adds a SECOND accepted structural
  shape (URL OR source_id); zero keyword matching.
- R6 uses a `bot_visible: bool` data field driven by manual corpus
  curation. NOT a runtime title-keyword match on "(temp)" — that
  would be a forbidden §1.7 keyword shortcut. The 2 known `(temp)`
  articles are explicitly flagged in data; new articles default to
  `bot_visible: true` (no behavioural change for unflagged content).
- R6's `SearchKnowledgeTool` filter is a simple `article.bot_visible !=
  false` predicate — no semantic decision, no content scan.

## Goal

After this sub-sprint ships:

- **R5 wiring**: `ResolveArticleTool` returns a `display_citation`
  field on every article result: `canonical_url` if present and
  non-blank, else `source_id` as fallback;
  `resolve_faq_grounded_answer.yaml:44` `cite_token_field` reads
  `display_citation`; `must_cite_source` guardrail accepts either a URL
  shape or a `source_id` shape (back-compat with old traces + 38
  URL-less articles). LLM-facing replies cite URLs when available,
  source_ids otherwise — without breaking grounding contract.
- **R6 wiring**: the 2 known `(temp)` template articles in
  `data/knowledge/knowledge_base_articles.json`
  (`ka41r000000LIEJAA4` near row 273 and `ka41r000000LIEEAA4` near
  row 292) carry `"bot_visible": false`; `SearchKnowledgeTool`
  filters retrieval to exclude `bot_visible=false` hits; the trace
  records the filter decision (INFO log only — backend log, not
  user-facing trace event) so observability is preserved. Human-CS
  retrieval paths are unchanged.

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
  the second structural token shape.
- Any UI surface (R3.* is Sub-sprint B's surface).
- Any intake / DISCOVER / clarification path (Sub-sprint C-1's
  surface).

## Scope (executable, #1–#7)

The dev prompt at `compact/sprint-081-dev-prompt.md` is the
self-contained executable view of this contract; sync invariant per
`prompt-artifact-rules.md` §9.3. The scope steps below are the
canonical version; the prompt mirrors them with one-page cumulative
context + read-order wrappers added.

### #1 — R5 step 1: `ResolveArticleTool` returns `display_citation`

**Anchor:** `server/src/main/java/.../tools/ResolveArticleTool.java`
(neighborhood per proposal §4.5; verify exact lines via pre-fix grep).

**Change:** add a `display_citation` field to the tool's result body
derived from `canonical_url` (when non-null and non-blank) or
`source_id` fallback. Preserve existing `source_id` and `canonical_url`
fields on the result — `display_citation` is ADDITIVE.

### #2 — R5 step 2: skill yaml `cite_token_field` flip

**Anchor:** `server/src/main/resources/.../resolve_faq_grounded_answer.yaml`
line 44 (per proposal §4.5; verify the line is the
`cite_token_field` config row via pre-fix grep).

**Change:** flip the line from `cite_token_field: source_id` to
`cite_token_field: display_citation`. This is the ONLY yaml line
touched in C-2 — verify with `git diff` before commit.

### #3 — R5 step 3: `must_cite_source` guardrail fallback

**Anchor:** the `must_cite_source` guardrail implementation (pre-fix
audit identifies the file; likely under
`server/src/main/java/.../guardrails/` or in the
`SkillGuardrailDispatcher` family).

**Change:** the guardrail must accept BOTH:

- A URL-shaped cite token (`http://` / `https://` prefix).
- A `source_id`-shaped cite token (alphanumeric Salesforce-style ID,
  matching the existing accept shape).

Structural OR condition; STRICT (no content-keyword match, no
LLM-based validation, no plain-English acceptance). Back-compat add
only.

**STOP condition:** if the guardrail is implemented via an LLM-based
check or some other unexpected shape (a real-traffic check, not the
expected structural validator), STOP and surface to deliver-agent.

### #4 — R5 step 4: tests for #1 + #2 + #3

Extend or add `ResolveArticleToolTest`,
`MustCiteSourceFallbackTest` (or extend existing must_cite_source
test). Cover R5 positives (URL → URL cite; null/blank → source_id
fallback; result body field-preservation negatives), R5 yaml flip
config-load smoke, R5 #3 positives (URL accepted; source_id
accepted) + negatives (plain English rejected; empty/null rejected
— grounding floor preserved).

### #5 — R6 step 1: `bot_visible: false` data field on the 2 `(temp)` articles

**Anchor:** `data/knowledge/knowledge_base_articles.json` —
`ka41r000000LIEJAA4` near row 273 and `ka41r000000LIEEAA4` near row
292 (per proposal §4.6; verify exact rows via pre-fix grep).

**Change:** add `"bot_visible": false` as a top-level field to each
article's JSON object. Do NOT modify any other field on these
articles. Do NOT add the field to any other article (default `true`).

**STOP condition:** if the verification surfaces that these 2
articles already carry OTHER governance flags (e.g. an existing
`internal_only` field) with overlapping semantics, STOP and surface
to deliver-agent (data-team convention check).

### #6 — R6 step 2: `SearchKnowledgeTool` retrieval filter

**Anchor:** `server/src/main/java/.../tools/SearchKnowledgeTool.java`
(verify exact path; proposal §4.6 cites this tool).

**Change:** add a filter step that excludes articles whose
`bot_visible` field equals `false`. Articles without the field
(default state) treated as visible. Filter step is structurally a
simple predicate (`article.bot_visible == null ||
article.bot_visible`).

**Observability:** when the filter removes an article, emit a
structured backend INFO log entry with `tool_event_id,
article_source_id, filter_reason="bot_visible=false"`. NOT a
user-facing trace event.

**STOP condition:** if the filter affects MORE than the 2 flagged
articles when tested (default-`true` semantics broken), STOP and
surface to deliver-agent.

### #7 — R6 step 3: tests for #5 + #6

Extend or add `SearchKnowledgeToolTest`. Cover data verification (2
flagged articles only; default unchanged), filter behaviour
(positives: bot_visible=false hits removed; bot_visible=true and
unflagged returned; negatives: default visible articles unaffected;
human-CS direct-resolve via `ResolveArticleTool` STILL surfaces the
flagged articles — filter is at search surface, not resolve surface).

## Anti-误杀 invariants (HARD, non-negotiable)

1. **R5 `must_cite_source` fallback PRESERVES the grounding floor.**
   Empty / null cite tokens STILL rejected; plain English phrases
   STILL rejected. Only structural shape widens to accept URL OR
   source_id.
2. **R5 yaml change is ONE LINE.** `cite_token_field` flip ONLY. No
   procedure / wording / skill structure change.
3. **R5 `display_citation` derivation is STRUCTURAL.** From
   `canonical_url` (non-null, non-blank) vs `source_id` fallback;
   NO content scan; NO inference; NO LLM call.
4. **R6 filter is DATA-FIELD driven.** Only signal is
   `article.bot_visible`. NO title-keyword match on "(temp)" or any
   string; NO body inspection. New articles default visible.
5. **R6 article retention.** The 2 flagged articles remain in the
   corpus JSON; only the LLM-facing retrieval surface filters them.
   Human-CS direct-resolve paths continue working.
6. **R6 filter observability.** When the filter removes an article,
   INFO-log structured evidence. NOT behavioural — corpus-curation
   paper trail only.
7. **No semantic procedure / wording change.** Only yaml line touched
   is `cite_token_field`. No bot prompt edit, no UC routing change,
   no escalation posture change, no judge calibration change.
8. **No new `escalation_reason` enum value.**
9. **`baseline_dir` UNCHANGED. `docs/current_eval_baseline.md`
   UNCHANGED.** Both flip at M-Auto-6 milestone close.
10. **No new Tier-0 invariant.**

## Hard fences / STOP conditions

**Files allowed to edit** (fence):

- `server/src/main/java/.../tools/ResolveArticleTool.java`
- `server/src/main/java/.../tools/SearchKnowledgeTool.java`
- `server/src/main/java/.../guardrails/<must_cite_source impl>.java`
  (or wherever the guardrail lives; pre-fix audit identifies the
  file)
- `server/src/main/resources/.../resolve_faq_grounded_answer.yaml` —
  **ONLY** the `cite_token_field` config-key line
- `data/knowledge/knowledge_base_articles.json` — **ONLY** the 2
  flagged articles, **ONLY** the `bot_visible` field add
- `server/src/test/java/.../tools/ResolveArticleToolTest.java`
  (extend or new)
- `server/src/test/java/.../tools/SearchKnowledgeToolTest.java`
  (extend or new)
- `server/src/test/java/.../guardrails/MustCiteSourceFallbackTest.java`
  (new or extension)
- `docs/sprints/sprint-081-handoff.md` (dev handoff)

**Files FORBIDDEN to edit**:

- Any OTHER yaml file or any OTHER line of
  `resolve_faq_grounded_answer.yaml`.
- Any OTHER article in `knowledge_base_articles.json`.
- `IntakeFieldsRegistry.java`.
- `SkillGuardrailDispatcher.java` reject-logic (unless it IS the
  `must_cite_source` host file; in that case ONLY the fallback
  branch).
- `BudgetChecker.java`.
- `ControlKernel.java` (Sub-sprint C-1's surface).
- `AgentRunLoopImpl.java`.
- `ContextProjectionBuilder.java`.
- `FormContextIngestionService.java`.
- `UpdateIntakeFieldsTool.java` (C-1's surface).
- `IntakeFieldsMerger.java` (C-1's surface).
- Any UI file (B's surface).
- Any `eval_interactive/` file (CaseSpecs / simulator / scoring /
  harness).
- Autoloop 5-file SHA-locked scoring set (fence-#13).
- `autoloop/config.yaml` `baseline_dir`.
- `docs/current_eval_baseline.md`.

**STOP conditions**:

- R5 #3 pre-fix audit surfaces that `must_cite_source` is implemented
  via an LLM-based check or some other unexpected shape → STOP and
  surface.
- R6 #5 verification surfaces existing OTHER governance flags on the
  2 `(temp)` articles overlapping with `bot_visible` semantics →
  STOP and surface.
- R6 retrieval filter affects MORE than the 2 flagged articles when
  tested → STOP (default `bot_visible: true` semantics broken).
- A test surfaces that the `cite_token_field` flip breaks an existing
  must-cite test because the grounding floor was tighter than
  expected → STOP (the fallback in #3 needs to be tightened or
  scope adjusted).
- Any forbidden file is touched → STOP, revert, re-launch.

## Test / eval requirements

- All new tests in #4 + #7 GREEN.
- Existing Java baseline `1327 / 1 / 0 / 2` preserved + R5 + R6
  additions (~+10-12 tests). Sole pre-existing failure (`OQ-S41.5`)
  preserved.
- Eval pytest `553` UNCHANGED (no eval-side change in this
  sub-sprint).
- Autoloop pytest UNCHANGED.
- **No real-LLM re-bless at this sub-sprint close.** Outcome evidence
  is the M-Auto-6 milestone-shared re-bless after C-2 lands.
- Mocked-LLM tests for the citation-token surface (R5 yaml flip) are
  wiring evidence per §5.7; real evidence is the milestone re-bless.

## §7 Layer-classification + anti-hardcode stanza

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (R5 `ResolveArticleTool` returns
`display_citation` field; `must_cite_source` guardrail accepts URL OR
source_id fallback; R6 `bot_visible` data field +
`SearchKnowledgeTool` retrieval filter) + `prompt_projection` (R5
one-line `cite_token_field` config-key swap in skill yaml —
contract-shaped, not procedure-shaped).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. R5
preserves the grounding floor (must_cite_source widens shape; does
NOT weaken the requirement to cite); R6 preserves corpus content
(only LLM-facing retrieval surface filters; human-CS direct-resolve
paths unchanged).

**Semantic hardcode:** No semantic hardcode introduced.
- R5's `display_citation` derives ENTIRELY from structured article
  fields (canonical_url presence → URL; absence → source_id
  fallback). Zero content scan, zero keyword match, zero per-UC
  matrix.
- R5's yaml flip is a ONE-LINE config-key swap
  (`cite_token_field: source_id` → `display_citation`). NOT a
  procedure rewrite, NOT a wording change.
- R5's `must_cite_source` fallback widens the accepted cite token
  shape from one to two (URL OR source_id). Structural validation
  only — no semantic / content / keyword check.
- R6's filter uses ONLY the `article.bot_visible` data field. NOT a
  title-keyword match on "(temp)" or any other string. New articles
  default visible (no behavioural change for unflagged content).
- R6's data flag is applied to exactly 2 articles
  (`ka41r000000LIEJAA4` + `ka41r000000LIEEAA4`) per explicit corpus
  curation. NOT a runtime classification, NOT a content scan.

**Generalization coverage:** target / neighbor / negative / shadow
case counts: 4 / ~6 / ~8 / 0
- target: c13 (R5 cite token); c7 / c12 / c17 (R6 `(temp)` article
  surface).
- neighbor: every FAQ-grounded-resolve case that cites an article
  (R5 is universal); every search_knowledge call that might surface
  a `(temp)` article (R6 — 2 known articles).
- negative: R5 must_cite_source negatives (empty / null / plain
  English rejected); R5 source_id fallback preserved for URL-less
  articles (38 known); R6 default-visible articles unaffected; R6
  human-CS direct-resolve unchanged (article still in corpus).
- shadow: not applicable (mocked-LLM tests are wiring evidence; real
  evidence is milestone-shared re-bless).
```

## Codex review plan (per `process/milestone-framework.md` §4.3)

**Per-sub-sprint Codex review REQUIRED** because R5 touches the
LLM-facing citation contract (`cite_token_field` is a
prompt_projection surface) and R6 touches the LLM-facing retrieval
surface (search results visible to the bot). Both are
semantic-touching per `iteration_governance.md` §7 +
`process/milestone-framework.md` §4.3.

Codex prompt artifact: `compact/sprint-081-codex-review-prompt.md`
(deliver-agent authors at sub-sprint close, embeds §4.1
nine-question kernel + §7 stanza + file-path fence + anti-误杀
invariants + generalization coverage).

**Focus points for Codex** (per §4.3):

- Q1: confirm R5 `display_citation` derivation is structural
  (canonical_url-vs-source_id presence check) with NO content scan;
  confirm R6 filter uses ONLY `article.bot_visible` and NO title
  keyword.
- Q3: confirm R5 yaml change is ONE LINE (`cite_token_field` config
  swap) and NOT a procedure rewrite; confirm R6 filter point
  surfaces observability (log) when filtering.
- Q4: not applicable (no multi-turn state).
- Q5: confirm no semantic decision moved from LLM to Java (LLM still
  decides whether / how to cite; runtime now provides a better cite
  token; LLM still searches; runtime filters known-bad corpus
  entries but does NOT reshape results).
- Q7: confirm grounding floor preserved (`must_cite_source` still
  requires a cite token of one of two structural shapes; empty /
  plain English still rejected); confirm safety floor unchanged.
- Q8: confirm generalization coverage matches the §7 stanza counts.
- Q9: no temporary hardcode; all changes are durable.

## Handoff requirements (dev authors `docs/sprints/sprint-081-handoff.md`)

§1 of the handoff must include:

- For each of #1-#6: file:line ranges + rationale + the test name(s)
  that gate it.
- **#3 pre-fix audit outcome**: where `must_cite_source` lives + its
  current structural validation; confirm the fallback addition does
  NOT weaken the grounding floor.
- **#5 data verification**: the 2 article rows post-edit; a
  JSON-shape snapshot showing only the `bot_visible` field was added.
- **#6 retrieval evidence**: a search query result before/after the
  filter (mocked tool test) showing the 2 articles excluded.
- **R5 sample LLM-facing projection**: at least 2 sample tool result
  bodies (one with canonical_url present → URL cite; one URL-less →
  source_id fallback) showing the `display_citation` field correctly
  populated.
- Java test results (full numeric: passed / failed / skipped /
  errors).
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
  - No real-LLM outcome re-bless launched (deferred to milestone
    close).
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

## Self-check checklist (dev completes before claiming done)

- [ ] Each of #1-#6 implemented with file:line ranges captured in
      handoff §1.
- [ ] All new tests in #4 + #7 GREEN.
- [ ] R5 `display_citation` derivation tests: canonical_url → URL;
      null/blank → source_id fallback.
- [ ] R5 yaml flip: `cite_token_field: display_citation` (one line,
      `git diff` confirms no other yaml change).
- [ ] R5 `must_cite_source` fallback: URL accepted; source_id
      accepted; empty / plain English rejected.
- [ ] R6 data flag: 2 articles (`ka41r000000LIEJAA4` +
      `ka41r000000LIEEAA4`) have `bot_visible: false`; no other
      article changed.
- [ ] R6 filter: bot_visible=false hides from LLM-facing search;
      bot_visible=true and default-visible articles returned
      normally; human-CS resolve path STILL surfaces the article.
- [ ] R6 observability: filter decision logged with article_source_id
      + filter_reason.
- [ ] Java baseline `1327 / 1 / 0 / 2` + new tests, no regressions.
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
      (the milestone-shared re-bless runs after C-2 lands).

When all checked: hand back to deliver-agent for close review +
per-sub-sprint Codex dispatch + M-Auto-6 milestone close trigger.
