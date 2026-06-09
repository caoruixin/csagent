# Sprint 081 / S-Auto-26 / M-Auto-6 — Sub-sprint C-2a — Dev Prompt (R5 citation contract fix: ResolveArticleTool display_citation + SkillGuardrailDispatcher.handleMustCiteSource structural-shape rewrite + resolve_faq_grounded_answer.yaml citation-wording consistency)

> **Active dev prompt — RE-SCOPED 2026-06-06** after a previous dev
> pre-fix audit on the original C-2 prompt (R5+R6 bundled) STOPPED
> with zero file edits and found that the original anchors were wrong
> on both sides:
>
> - The original R5 prompt put the `must_cite_source` guardrail at
>   `guardrails/MustCiteSource.java` — **no such file**. The
>   guardrail lives at
>   `SkillGuardrailDispatcher.java:340-384` (`handleMustCiteSource`),
>   which the original prompt's fence FORBID editing. And the
>   guardrail does a literal `userMessage.contains(citeToken)`
>   substring check — NOT structural URL/source_id shape validation
>   as the original prompt assumed. So flipping
>   `cite_token_field: source_id → display_citation` would have made
>   the bot need to emit the literal text `"display_citation"` in its
>   replies, breaking the grounding floor + existing tests + not
>   even fixing c13 (the LLM citation behaviour is driven by the
>   skill yaml procedure/grounding_instruction wording at `:30-31`,
>   not by `cite_token_field:44`).
>
> - The original R6 prompt assumed editing only
>   `data/knowledge/knowledge_base_articles.json` would propagate to a
>   `SearchKnowledgeTool` filter. **It would not.**
>   `KnowledgeIngestionRunner.buildKbArticleFromJson` silently drops
>   unknown JSON keys; `KbArticle` has no `bot_visible` column; the
>   ingested-but-unused governance flag `search_knowledge_eligible`
>   (present on 218/218 articles) is the real reuse path — full
>   plumbing through ingestion + entity + V17 migration + service +
>   tool is required.
>
> The dev STOP was the right call. This re-scoped prompt SPLITS the
> old C-2 into two narrower sub-sprints with corrected anchors:
>
> - **C-2a (this prompt; S-Auto-26 / Sprint 081)** = R5 citation
>   contract fix with widened fence + reclassified §7.
> - **C-2b (S-Auto-27 / Sprint 082; planning context at
>   `compact/sprint-082-dev-prompt.md`)** = R6 corpus eligibility
>   filter reusing the existing `search_knowledge_eligible` field,
>   plumbing through `KnowledgeIngestionRunner` + `KbArticle` + V17
>   Flyway migration + `KnowledgeSearchService` + `KnowledgeHit` +
>   `SearchKnowledgeTool`. Launches after C-2a dev-side close.
>
> The canonical contract is `docs/sprint_objective.md` (this prompt
> is its self-contained executable view per
> `docs/current/process/prompt-artifact-rules.md` §9). M-Auto-6 close
> waits for C-2a + C-2b both dev-side closed.

## Role identity

You are the **dev agent** for **Sprint 081 / S-Auto-26 / M-Auto-6
Sub-sprint C-2a**.

Your one-sentence goal: ship R5 — a citation-guardrail contract change
that lets the bot cite either a URL or a structural article_id and
keeps the grounding floor intact — by (a) adding an additive
`display_citation` field on `ResolveArticleTool`'s result body
(`source_url` when present and non-blank, else `article_id` fallback);
(b) rewriting `SkillGuardrailDispatcher.handleMustCiteSource` from the
current literal `userMessage.contains(citeToken)` substring check to a
structural shape predicate that accepts URL-shape OR article_id-shape
tokens; (c) updating `resolve_faq_grounded_answer.yaml` citation-token
wording (`procedure :30 + grounding_instruction :31 + ~:80 cite phrasing
+ cite_token_field :44`) to point the LLM at the new
`display_citation` field. Zero semantic procedure / wording change
beyond the citation-token references. Grounding floor preserved (empty
/ null / plain-English STILL reject).

## Read order (minimized)

1. `AGENTS.md` (auto-loaded via this prompt).
2. **This prompt** (full sub-sprint contract — do NOT read
   `docs/sprint_objective.md` for scope when this prompt is the active
   contract).
3. **Code anchors only as needed** (cited inline in §Scope below).
4. Before #2 (the guardrail rewrite), follow the §Scope #2 PRE-FIX
   AUDIT (a) + (b) + (c) — read
   `SkillGuardrailDispatcher.java:340-384`,
   `SkillGuardrailDispatcherTest` (and any `MustCiteSource*` test),
   and `data/knowledge/knowledge_base_articles.json` for real
   article_id values. Before #3 (the yaml wording), follow the §Scope
   #3 PRE-FIX AUDIT — read the full yaml file end-to-end + grep
   `cite_token_field` across `server/src/main`.

## Cumulative context (one-page)

- **M-Auto-5 CLOSED 2026-06-05** (Class A; `baseline_dir =
  eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix`).
- **M-Auto-6 ACTIVE**:
  - Sub-sprint A (R1.a + R2.a + R4.a; archive
    `docs/sprints/sprint-078-objective.md`) DEV-SIDE CLOSED 2026-06-06.
  - Sub-sprint B (R3.a + R3.b + R3.c admin trace observability;
    archive `docs/sprints/sprint-079-objective.md`) DEV-SIDE CLOSED
    2026-06-06.
  - Sub-sprint C-1 (R7 + R2.a#5-ext; archive
    `docs/sprints/sprint-080-objective.md`) DEV-SIDE CLOSED
    2026-06-06; Codex `APPROVE_S_AUTO_25 / blocking_count=0`;
    capability-wiring Option-A fence-waiver accepted.
  - **C-2a (this sub-sprint)** is CURRENT ACTIVE. C-2b (R6 corpus
    eligibility) launches after C-2a dev-side close.
- **Java baseline at sub-sprint launch**: `1327 / 1 / 0 / 2` (sole
  failure = inherited `SystemPromptUserRequestedTiebreakerTest`,
  OQ-S41.5; verified pre-existing on clean main).
- **Python baselines**: eval_interactive pytest `553` (548 + 5
  inherited conda-python `test_corpus_lint.py` env-drift, NOT a
  regression); autoloop pytest `324`.
- **Source-of-truth proposal**:
  `docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md`
  §4.5 (R5; updated re-scope reflected in this prompt). The proposal
  text predates the dev anchor audit and uses some outdated anchors;
  this prompt's anchors are authoritative for C-2a.
- **Trigger case**: **R5 (c13)** — bot answers with
  `(Source: ka4P200000003sLIAQ)` even though the article has a
  `source_url`. Root cause is two-fold per the dev audit:
  (i) the LLM has no signal that the URL is preferred (the skill
  procedure / grounding_instruction at `:30-31` says "cite the
  source_id"), and (ii) `ResolveArticleTool`'s result body has only
  `article_id` + `source_url` as separate fields, not a single
  preferred display token. R5 fixes both surfaces.

## Class (layer classification)

**Layer (per `iteration_governance.md` §3.2):**

- **R5 #1** (additive `display_citation` on tool result body) →
  `infra` (structural derivation from `source_url` vs `article_id`).
- **R5 #2** (`handleMustCiteSource` rewrite) → `infra` (guardrail
  semantics; replaces literal-substring with structural shape).
- **R5 #3** (skill yaml citation wording + `cite_token_field`
  retirement) → `prompt_projection` (LLM-facing wording).

**§7 stanza requirement:** **REQUIRED.** R5 #2 + R5 #3 both touch
semantic-touching surfaces. Full stanza embedded in §7 below.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant.

- R5 #2's rewrite **preserves** the grounding-floor contract (require
  a cite token) and widens the accept criteria from "literal
  field-name string appears verbatim in the reply" to "URL-shape OR
  article_id-shape token appears in the reply". The widening
  **fixes a false-negative** in the current literal check (the OLD
  code rejected any reply that cited a URL without also writing the
  literal string "source_id"). The new code STILL rejects empty /
  null / plain-English replies — the grounding floor is intact.
- R5 #1 is purely additive.
- R5 #3 is a wording change pointing at the new `display_citation`
  field; procedure shape / goal / role / objective / search-must-
  precede / faq_miss escalation rule are untouched.

**Semantic hardcode:** No semantic hardcode introduced.

- R5 #1's `display_citation` derives entirely from structured article
  columns (`source_url` if non-null and non-blank, else `article_id`);
  zero content scan; zero per-UC matrix.
- R5 #2's accept criteria is structural URL prefix
  (`http://` / `https://`) + Salesforce-style article_id regex
  **derived from real corpus data** in `data/knowledge/knowledge_base_articles.json`
  (NOT invented). No LLM call; no content-keyword match; no per-UC
  matrix.
- R5 #3's wording points at the tool result field
  (`display_citation`), not at a hard-coded URL list or per-UC
  matrix. Minimum-edit diff — procedure / role / objective /
  search-must-precede / faq_miss rule untouched.

## Goal

After this sub-sprint ships:

- **R5 #1 wiring**: `ResolveArticleTool` returns a `display_citation`
  field on every article result body: `source_url` when present and
  non-blank, else `article_id` fallback. Existing fields preserved.
- **R5 #2 guardrail**: `SkillGuardrailDispatcher.handleMustCiteSource`
  accepts a user-facing reply iff it contains EITHER a URL-shape
  token (`http://` / `https://` prefix anywhere in the reply) OR an
  article_id-shape token (the structural Salesforce ID regex derived
  from corpus data). Empty / null / plain-English REJECT. The
  pre-existing test
  `SkillGuardrailDispatcherTest.mustCiteSource_doesNotFire_when_sourceIdPresent`
  (input `"[source_id: kb-001]"`) either continues to PASS as-is
  (because `kb-001` matches the article_id-shape regex) OR is updated
  to use a real article_id from the corpus — decision in pre-fix
  audit (c).
- **R5 #3 wording**: `resolve_faq_grounded_answer.yaml` procedure
  (:30), grounding_instruction (:31), and the line ~80 cite phrasing
  all point at the `display_citation` token returned by
  resolve_article. `cite_token_field:44` is either REMOVED cleanly
  (preferred if pre-fix audit (b) finds no other consumer) or
  REPURPOSED as a declarative `cite_token_source: display_citation`
  hint with no behavioural role.

NOT a goal:

- Editing any other guardrail handler in
  `SkillGuardrailDispatcher.java` (only `handleMustCiteSource` lines
  340-384 + the new private helper + the two `Pattern` constants).
- Editing any other yaml file or any non-citation yaml line.
- Editing `KbArticle.java` schema columns or repository.
- Editing `data/knowledge/knowledge_base_articles.json` (C-2b's
  surface).
- Editing `KnowledgeIngestionRunner` / `KnowledgeSearchService` /
  `KnowledgeHit` / `SearchKnowledgeTool` (C-2b's surface).
- Adding any new escalation_reason enum value.
- Adding any per-UC matrix anywhere.
- Adding any LLM-based citation validation.
- Touching C-1's surface (`UpdateIntakeFieldsTool`,
  `IntakeFieldsMerger`, `ControlKernel.mapBudgetToEscalationReason`).
- Touching A's surface (`IntakeFieldsRegistry`,
  `ContextProjectionBuilder`).
- Touching B's surface (any UI file).

## Scope (executable, #1–#5)

### #1 — R5 #1: `ResolveArticleTool` returns `display_citation`

**Anchors:**
- `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`
  — result body construction. Pre-fix grep for the existing
  `result.put("article_id", ...)` line to anchor the insertion site.
- `server/src/main/java/com/gumtree/csagent/model/KbArticle.java`
  getters: `getSourceUrl()` returns `source_url` column;
  `getArticleId()` returns `article_id` column. (Schema columns
  enumerated at `KbArticle.java` `@Column` blocks: `article_id`,
  `title`, `summary`, `description`, `source_url`, `url_category`,
  `uc_tags`, `is_published`, `token_count`, `version`, `created_at`,
  `updated_at`.)

**Change:** add `display_citation` to the result body:

```java
String src = article.getSourceUrl();
String displayCitation = (src != null && !src.isBlank())
    ? src
    : article.getArticleId();
result.put("display_citation", displayCitation);
```

Insert near the existing `article_id` / `source_url` puts. Existing
fields preserved (no rename, no drop, no shape change).

**Hard fence:** purely additive; no field renamed; no existing field
dropped or changed.

### #2 — R5 #2: `SkillGuardrailDispatcher.handleMustCiteSource` structural-shape rewrite

**Anchor:** `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java:340-384`
(`handleMustCiteSource` method body).

**Current behaviour** (for reference; this is what the rewrite
replaces):

```java
String citeToken = "source_id";
Object citeField = g.parameters().get("cite_token_field");
if (citeField instanceof String s && !s.isBlank()) {
    citeToken = s.trim();
}
String userMessage = ctx.parsedUserMessage().orElse(null);
if (userMessage == null || userMessage.isBlank()) {
    userMessage = ctx.lastLlmRawResponse();
}
if (userMessage != null && userMessage.contains(citeToken)) {
    return Optional.empty();  // PASS — bot's reply contains the literal field-name string
}
// REJECT (build trace + RejectVerdict at :369-383)
```

This is a literal-substring check on the field-NAME string (default
`"source_id"`), not a structural validation of any cited value.

**PRE-FIX AUDIT (MANDATORY before #2 implementation):**

- (a) Read `SkillGuardrailDispatcher.java:340-384` end-to-end.
  Enumerate every input the method reads:
  `outcomeClass`, `g.parameters().get("outcome_class")`,
  `g.parameters().get("cite_token_field")`,
  `ctx.parsedUserMessage()`, `ctx.lastLlmRawResponse()`. Identify the
  `RejectVerdict` trace keys at `:369-383` (especially the
  `cite_token_field` trace key — that key is renamed or removed in
  step with #3).
- (b) Read every existing test pinning the method. Start with
  `SkillGuardrailDispatcherTest.mustCiteSource_doesNotFire_when_sourceIdPresent`
  (named in dev audit) and grep for any other `mustCiteSource*` or
  `MustCiteSource*` test under `server/src/test/java`. Enumerate
  every input shape covered by existing tests — each is a
  back-compat invariant. Document the inputs and the planned
  pass/fail status post-rewrite.
- (c) Inspect actual `article_id` values in
  `data/knowledge/knowledge_base_articles.json` (sample ~10 entries)
  and derive the strict article_id-shape regex from real data. Two
  candidate patterns:
  - Narrow: `\b[kK][aA][0-9A-Za-z]+\b` — matches Salesforce-style
    `ka...` IDs (the dominant shape in the corpus per the dev audit).
  - Wider: `\b[A-Za-z]{2,}[0-9][A-Za-z0-9]+\b` — also matches
    back-compat test shapes like `kb-001`.
  - Decision: pick the NARROWEST pattern that matches every existing
    `article_id` in the corpus AND rejects plain English. If the
    back-compat test shape `kb-001` does not fit, decide between
    updating the test input to use a real `article_id` (preferred) or
    widening the regex (only if no false-positive on plain English).
    Record the decision + evidence in handoff §1.

**Change:** replace the literal-substring check with a structural-shape
predicate.

1. Add two private static final `Pattern` constants at the top of
   `SkillGuardrailDispatcher` (next to the other constants like
   `S1_CITATION_PRESENCE_REQUIRED`):

```java
private static final java.util.regex.Pattern USER_FACING_URL_PATTERN =
    java.util.regex.Pattern.compile("https?://\\S+");
private static final java.util.regex.Pattern ARTICLE_ID_PATTERN =
    java.util.regex.Pattern.compile("<derived in pre-fix audit (c)>");
```

2. Add the helper:

```java
private static boolean containsAcceptableCiteToken(String userMessage) {
    if (userMessage == null) return false;
    if (USER_FACING_URL_PATTERN.matcher(userMessage).find()) return true;
    if (ARTICLE_ID_PATTERN.matcher(userMessage).find()) return true;
    return false;
}
```

3. Rewrite the relevant branch of `handleMustCiteSource`. The
   pre-outcome-class checks (lines 342-356 — `outcomeClass` null,
   normalized to `resolve`/`resolved`, yaml `outcome_class` agreement)
   are preserved unchanged. The reading of `userMessage` from
   `ctx.parsedUserMessage()` / `ctx.lastLlmRawResponse()` is
   preserved. Replace lines 357-368:

```java
// citeToken extraction block (lines 357-361) — REMOVED.
String userMessage = ctx.parsedUserMessage().orElse(null);
if (userMessage == null || userMessage.isBlank()) {
    userMessage = ctx.lastLlmRawResponse();
}
if (userMessage != null && containsAcceptableCiteToken(userMessage)) {
    return Optional.empty();
}
```

4. Update the `RejectVerdict` construction (lines 369-383) — the
   trace key `cite_token_field` and the rejection message string both
   reference the old `citeToken` variable. Replace with:
   - Trace key: rename to `cite_token_validator` with a constant value
     `"structural_url_or_article_id"` (so traces stay structured;
     value documents the new semantics).
   - Rejection message: rewrite to "The user-facing message must
     include a citation — either a `source_url` (http(s)://...) or a
     structural `article_id` — from a successful resolve_article call
     before record_outcome with class=resolve can persist. Cite the
     `display_citation` value returned by resolve_article in your
     user_message."

**Hard fence:**
- The widening MUST preserve every reject negative that exists today:
  empty / null / plain English / random keyword strings still reject.
- The structural patterns MUST NOT use semantic / LLM-based
  validation, MUST NOT match on user-message keywords, MUST NOT
  enumerate any UC.
- The article_id pattern's regex MUST be derived from real corpus
  data, not invented.
- ONLY `handleMustCiteSource` + the two new `Pattern` constants +
  the new helper are touched. Every other guardrail handler in the
  class is byte-untouched.

### #3 — R5 #3: `resolve_faq_grounded_answer.yaml` citation-wording consistency

**Anchor:**
- `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml`
  lines `:30` (procedure), `:31` (grounding_instruction), `:44`
  (`cite_token_field: source_id`), `~:80` (cite phrasing — exact line
  identified in pre-fix audit).

**Current relevant content** (for reference):

- `:30 procedure` — "...search_knowledge → resolve_article → grounded
  customer-facing answer (with a source_id citation) → record_outcome.
  ..."
- `:31 grounding_instruction` — "...After search_knowledge returns a
  viable hit, you MUST call resolve_article for the top hit before
  answering the customer; cite the source_id in your user_message.
  ..."
- `:44` — `cite_token_field: source_id` (under a parameters block for
  the `must_cite_source` guardrail).
- `~:80` — references a `source_id` citation requirement in another
  context (audit identifies the line and surrounding sentence).

**PRE-FIX AUDIT (MANDATORY before #3 implementation):**

- (a) Read the full yaml file end-to-end. Enumerate EVERY line that
  references `source_id` or any citation token wording. Document each
  in handoff §1 with the line number + the before/after wording
  planned.
- (b) Grep across `server/src/main` for the literal string
  `cite_token_field`:
  ```
  grep -rn "cite_token_field" server/src/main
  ```
  If `cite_token_field` is consumed only by the guardrail (which the
  rewrite no longer reads), it can be REMOVED cleanly. If consumed
  elsewhere (config-load smoke test, alternate guardrail), the audit
  must surface the consumer and the change adapts (either keep the
  yaml key with a `display_citation` value as a back-compat hint, or
  also update the consumer).

**Change:**

- Update `procedure` (:30) — replace "with a source_id citation" with
  "with a `display_citation` citation (the article's source_url when
  available, otherwise its article_id)". Adapt grammar to match the
  surrounding sentence; minimum-edit diff.
- Update `grounding_instruction` (:31) — replace "cite the source_id
  in your user_message" with "cite the `display_citation` token
  returned by resolve_article in your user_message (URL when
  available, article_id otherwise)". Minimum-edit diff.
- Update the line ~80 cite phrasing similarly (exact wording per the
  audit).
- Either REMOVE `cite_token_field:44` cleanly (preferred if pre-fix
  audit (b) finds no other consumer) OR rename it to
  `cite_token_source: display_citation` (declarative breadcrumb;
  no behavioural role). Document the decision + rationale in handoff
  §1.

**Hard fence:**
- ONLY the citation-token wording is touched. The skill's goal /
  role / objective / search-must-precede-resolve / faq_miss escalation
  rule / non-citation paragraphs are byte-untouched.
- No procedure-step reordering, no new tool invocation rule, no UC
  routing change.
- Minimum-edit diff captured in handoff §1 via `git diff` excerpt.

### #4 — R5 tests for #1 + #2 + #3

**Anchors (new / extended test files):**

- `server/src/test/java/com/gumtree/csagent/service/tools/ResolveArticleToolTest.java`
  — extend with `display_citation` tests (or create if missing —
  pre-fix grep confirms).
- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcherTest.java`
  — extend or REWRITE the relevant `mustCiteSource*` tests for the
  structural-shape predicate.
- Optionally a new
  `server/src/test/java/com/gumtree/csagent/service/runtime/skill/MustCiteSourceShapeTest.java`
  focused on the new helper if it gets its own class (design choice
  per pre-fix audit).

**R5 #1 tests:**

- Positive A — `KbArticle` with `source_url` non-null and non-blank
  → `display_citation` equals the `source_url`.
- Positive B — `KbArticle` with `source_url == null` →
  `display_citation` equals the `article_id`.
- Positive C — `KbArticle` with `source_url` blank (`""` or `"   "`)
  → `display_citation` equals the `article_id`.
- Negative A — existing `article_id` field preserved on the result
  body (assert the existing key remains populated as-was).
- Negative B — existing `source_url` field preserved on the result
  body (assert the existing key remains populated as-was when
  source_url is non-null).

**R5 #2 tests (the citation-guardrail rewrite — structural-shape
positives + the existing back-compat negatives):**

- Positive A — user_message contains a URL-shape
  (`https://help.gumtree.com/...`) → guardrail PASS (returns
  `Optional.empty()`).
- Positive B — user_message contains a URL-shape with `http://`
  prefix → PASS.
- Positive C — user_message contains an article_id-shape token
  (`ka41r000000LIEEAA4`) → PASS.
- Positive D — user_message contains the legacy test shape
  `[source_id: kb-001]` → PASS (per back-compat — either the
  article_id-shape regex covers it OR the test is updated to use a
  real article_id; decision documented in pre-fix audit (c)).
- Negative A — user_message is empty (`""`) → REJECT.
- Negative B — user_message is null (both `parsedUserMessage` and
  `lastLlmRawResponse`) → REJECT.
- Negative C — user_message contains only plain English (no URL, no
  article_id-shape, e.g. "I can help with your ad removal.") →
  REJECT.
- Negative D — user_message contains the literal string `"source_id"`
  WITHOUT a URL or an actual article_id-shape (e.g. "I'll cite the
  source_id for you.") → REJECT. The OLD literal-substring code would
  have PASSED this — this assertion confirms the literal→shape
  semantics shift. Mark this test with an explanatory comment.
- Negative E — outcome_class is not `resolve` / `resolved` → guardrail
  bows out as before (Sprint 39 contract preserved; returns
  `Optional.empty()` because the handler doesn't apply).
- Negative F — yaml `outcome_class` parameter disagrees (e.g.
  `parameters.outcome_class = "escalate"`) → guardrail bows out as
  before.

**R5 #3 tests:**

- Yaml load smoke (if `SkillLoader` has a yaml-load test infrastructure
  visible to the project; otherwise add a focused integration test
  that loads the yaml via `SkillLoader` and asserts the citation-token
  wording in `procedure` / `grounding_instruction` references
  `display_citation` and that `cite_token_field` is removed-or-renamed
  per the audit decision).

### #5 — Backend rebuild + integration smoke (no real-LLM run)

After #1-#4:

- `cd server && mvn -o -DskipTests package` → BUILD SUCCESS.
- `mvn -o test` from `server/` → Java baseline `1327 / 1 / 0 / 2`
  preserved + R5 additions; sole pre-existing failure
  (`SystemPromptUserRequestedTiebreakerTest`, OQ-S41.5) preserved; no
  new failures.
- Document the rebuild output + test summary in handoff §2.

No real-LLM smoke this sub-sprint. Outcome evidence is the M-Auto-6
milestone-shared re-bless after C-2b lands.

## Anti-误杀 invariants (HARD, non-negotiable)

1. **Grounding floor preserved.** The contract "user-facing reply
   must contain a cite token" is unchanged. Empty / null / plain
   English STILL reject.
2. **Structural validation only.** Accept criteria is URL-shape OR
   article_id-shape; NO LLM-based validation; NO content-keyword
   match.
3. **Article_id pattern derived from real data.** Not invented; not
   relaxed beyond what corpus + back-compat demand.
4. **Skill yaml wording change is minimum-edit.** Only the
   citation-token phrasing is touched. Goal / role / objective /
   search-must-precede / faq_miss rule unchanged.
5. **`cite_token_field` retirement is consistent.** Either removed
   cleanly (no orphan consumer) or repurposed as a declarative
   `cite_token_source: display_citation` hint with no behavioural
   role. Decision evidenced in audit (b).
6. **Additive `display_citation` only.** Existing tool result fields
   unchanged.
7. **No `data/knowledge/knowledge_base_articles.json` edit.** C-2b's
   surface.
8. **No new `escalation_reason` enum value.**
9. **No new Tier-0 invariant.**
10. **`baseline_dir` UNCHANGED. `docs/current_eval_baseline.md`
    UNCHANGED.**

## Hard fences / STOP conditions

**Files allowed to edit** (fence):

- `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java`
  (**ONLY** `handleMustCiteSource` lines 340-384 + new private helper
  + the two `Pattern` constants near the top of the class + the
  `RejectVerdict` trace-map key rename and rejection message
  rewrite in step with the rewrite)
- `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml`
  (**ONLY** the citation-token wording lines per pre-fix audit (a) +
  `cite_token_field` parameter per audit (b))
- `server/src/test/java/com/gumtree/csagent/service/tools/ResolveArticleToolTest.java`
  (extend or new)
- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcherTest.java`
  (extend or relevant `mustCiteSource*` tests rewritten)
- Optionally
  `server/src/test/java/com/gumtree/csagent/service/runtime/skill/MustCiteSourceShapeTest.java`
- `docs/sprints/sprint-081-handoff.md` (dev handoff)

**Files FORBIDDEN to edit**:

- Any OTHER guardrail handler in `SkillGuardrailDispatcher.java`
  (`handleFaqMiss`, `handleIntakeRequiredFields`, `handleProgressiveResolve`,
  etc.).
- Any OTHER yaml file (skills or otherwise).
- `KbArticle.java` (schema column already provides `getSourceUrl()` +
  `getArticleId()`; no new column needed).
- `KbArticleRepository.java`.
- `data/knowledge/knowledge_base_articles.json` (C-2b's surface).
- `KnowledgeIngestionRunner.java` / `KnowledgeSearchService.java` /
  `KnowledgeHit.java` / `SearchKnowledgeTool.java` (C-2b's surface).
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

- Pre-fix audit (a) on `handleMustCiteSource` surfaces an unexpected
  shape (e.g. the method is also consumed by another outcome class
  beyond `resolve` / `resolved`, or it has unexpected callers) → STOP,
  surface to deliver-agent.
- Pre-fix audit (b) on existing `mustCiteSource*` tests surfaces a
  test input shape that CANNOT be represented by any narrow
  structural pattern that also excludes plain English → STOP, surface
  (deliver-agent decides whether to update the test inputs to use real
  article_ids vs widen the regex to a different shape).
- Pre-fix audit (c) on the article_id-shape regex surfaces that real
  corpus values include shapes the narrow `[kK][aA]...` regex misses
  → STOP, surface (decide whether to broaden the regex or rely on a
  generic Salesforce-style ID pattern).
- Pre-fix audit on yaml `cite_token_field` consumers surfaces a
  consumer outside the guardrail → STOP (widens scope beyond C-2a;
  surface).
- Any new test in #4 fails in a way that suggests the grounding floor
  is weakened (a plain English reply PASSES the new guardrail) →
  STOP, anti-误杀 #1 violation.
- Any file outside the fence is touched → STOP, revert, re-launch.
- A second `must_cite_source` config-key consumer surfaces in
  `server/src/main` (other than the guardrail) → STOP.

## Test / eval requirements

- All new tests in #4 GREEN.
- Existing Java baseline `1327 / 1 / 0 / 2` preserved + R5 additions
  (~+10-15 tests). Sole pre-existing failure (`OQ-S41.5`) preserved.
- `SkillGuardrailDispatcherTest.mustCiteSource_doesNotFire_when_sourceIdPresent`
  either continues to PASS as-is OR is updated to use a real
  article_id (decision in pre-fix audit (c)); the structural-shape
  rewrite must not silently regress it.
- Eval pytest `553` UNCHANGED (no eval-side change).
- Autoloop pytest `324` UNCHANGED.
- Backend rebuild + test run documented in handoff §2.
- **No real-LLM re-bless at this sub-sprint close.** Outcome evidence
  is the M-Auto-6 milestone-shared re-bless after C-2b lands.
- Wiring evidence vs outcome evidence separator per §5.7 mocked-LLM
  gate documented in handoff.

## §7 Layer-classification + anti-hardcode stanza

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (R5 #1 ResolveArticleTool result
body shape; R5 #2 SkillGuardrailDispatcher.handleMustCiteSource
semantics rewrite from literal-substring to structural URL/article_id
shape validation) + `prompt_projection` (R5 #3 skill yaml citation
wording at procedure / grounding_instruction / ~:80 + cite_token_field
retirement).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. R5 #2
preserves the existing grounding-floor contract (require a cite
token) and widens the accept criteria from "literal field-name string
appears verbatim" to "URL-shape OR article_id-shape token appears" —
a fix to a false-negative in the current literal check, not a
weakening of the floor. Empty / null / plain-English STILL reject.
R5 #1 is purely additive. R5 #3 changes wording on the LLM-facing
surface to point at the new `display_citation` field; procedure shape
preserved.

**Semantic hardcode:** No semantic hardcode introduced.
- R5 #1: `display_citation` derives entirely from structured article
  columns (`source_url` if non-null/non-blank else `article_id`);
  zero content scan; zero per-UC matrix.
- R5 #2: accept criteria is structural URL prefix (`http://`/`https://`)
  + Salesforce-style article_id regex derived from real corpus data
  (NOT invented). No LLM-based validation; no content-keyword match
  on the user message.
- R5 #3: wording points at the tool result field (`display_citation`),
  not at a hard-coded URL list or per-UC matrix. Minimum-edit diff;
  procedure / role / objective / search-must-precede / faq_miss rule
  untouched.

**Generalization coverage:** target / neighbor / negative / shadow case
counts: 1 / ~6 / ~10 / 0
- target: c13 (bot cites article_id when source_url is available).
- neighbor: every FAQ-grounded-resolve case that cites an article
  (R5 is universal); URL-less articles (38 known) where
  `display_citation` falls back to `article_id`.
- negative: R5 #2 guardrail rejects empty / null / plain English;
  rejects "I'll cite the source_id" (literal-substring false positive
  on the OLD code) — confirms the literal→shape semantics shift;
  rejects outcome_class != resolve (Sprint 39 contract preserved);
  rejects yaml `outcome_class` parameter disagreement; the back-compat
  positive (`[source_id: kb-001]` or its updated form) continues to
  PASS. R5 #3 wording change does not affect non-FAQ paths (skill
  scope unchanged).
- shadow: not applicable (mocked-LLM tests are wiring evidence; real
  evidence is milestone-shared re-bless after C-2b lands).
```

## Handoff requirements (you author `docs/sprints/sprint-081-handoff.md`)

§1 of the handoff must include:

- For each of #1-#3: file:line ranges + rationale + the test name(s)
  that gate it.
- **#2 pre-fix audit outcome (a) + (b) + (c)**:
  - (a) Every input the current method reads.
  - (b) Every existing `mustCiteSource*` test's input shape +
    whether it stays GREEN as-is or needs an update + rationale.
  - (c) The chosen `article_id`-shape regex + evidence it matches
    every existing `article_id` value in the corpus (sample a dozen
    rows) + evidence it rejects plain English (cite a Negative C-style
    test input).
- **#3 pre-fix audit outcome**:
  - Every yaml line touched (line number + before/after wording).
  - Whether `cite_token_field` is REMOVED or REPURPOSED (decision +
    rationale + grep evidence of no-orphan-consumer).
- Java test results (full numeric: passed / failed / skipped /
  errors).
- Eval pytest / autoloop pytest results (UNCHANGED — re-run not
  required since no eval-side touch; affirm via `git status` evidence).
- STOP confirmations:
  - File fence respected; no forbidden file touched.
  - No `data/knowledge/knowledge_base_articles.json` edit.
  - No `KnowledgeIngestionRunner` / `KnowledgeSearchService` /
    `KnowledgeHit` / `SearchKnowledgeTool` edit.
  - No other guardrail handler in `SkillGuardrailDispatcher.java`
    touched.
  - No other yaml file or non-citation yaml line touched.
  - No new `escalation_reason` enum value.
  - `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md`
    UNCHANGED.
  - No real-LLM outcome re-bless launched (deferred to milestone
    close).
- A clear "wiring evidence" vs "outcome evidence" separator per §5.7
  mocked-LLM gate.

## Commit discipline

Recommended commit split (per `prompt-artifact-rules.md` §9):

1. **Commit 1 — R5 #1 (additive)**: `ResolveArticleTool`
   `display_citation` field + `ResolveArticleToolTest` extensions.
2. **Commit 2 — R5 #2 (guardrail rewrite)**: `SkillGuardrailDispatcher`
   `handleMustCiteSource` rewrite + new private helper + two `Pattern`
   constants + `SkillGuardrailDispatcherTest` extensions (or new
   `MustCiteSourceShapeTest`).
3. **Commit 3 — R5 #3 (skill yaml wording)**:
   `resolve_faq_grounded_answer.yaml` citation-token wording change +
   `cite_token_field` retirement + any yaml-load smoke test.
4. **Commit 4 — Dev handoff**: `docs/sprints/sprint-081-handoff.md`
   (the pre-fix audit (a)/(b)/(c) evidence can be in this commit or
   inlined in commit 2's body — your choice).

## Self-check checklist (complete BEFORE claiming done)

- [ ] Each of #1-#3 implemented with file:line ranges captured in
      handoff §1.
- [ ] All new tests in #4 GREEN.
- [ ] Pre-fix audit (a) + (b) + (c) documented in handoff §1.
- [ ] R5 #1 `display_citation`: source_url non-null/non-blank → URL;
      null/blank → article_id fallback. Existing fields preserved.
- [ ] R5 #2 guardrail accept: URL-shape positives; article_id-shape
      positives; back-compat (`[source_id: kb-001]` or updated)
      positive.
- [ ] R5 #2 guardrail reject: empty; null; plain English;
      `source_id`-literal-without-URL-or-ID; outcome_class !=
      `resolve`/`resolved`; yaml `outcome_class` disagreement.
- [ ] R5 #3 yaml diff: ONLY citation-token wording + `cite_token_field`
      lines; `git diff` confirms goal / role / objective /
      search-must-precede / faq_miss rule byte-untouched.
- [ ] `cite_token_field` decision (REMOVE or REPURPOSE) documented
      with no-orphan-consumer evidence.
- [ ] Java baseline `1327 / 1 / 0 / 2` + R5 additions, no
      regressions.
- [ ] Eval pytest `553` unchanged.
- [ ] Autoloop pytest `324` unchanged.
- [ ] No file outside the file fence touched.
- [ ] No semantic procedure / wording change beyond the citation
      token references in #3.
- [ ] No edit to `data/knowledge/knowledge_base_articles.json` or
      C-2b's plumbing surfaces.
- [ ] No edit to `IntakeFieldsRegistry` / other guardrail handlers /
      `BudgetChecker` / `ControlKernel` / `AgentRunLoopImpl` /
      `ContextProjectionBuilder` / `UpdateIntakeFieldsTool` /
      `IntakeFieldsMerger`.
- [ ] §7 stanza copied verbatim into handoff.
- [ ] `baseline_dir` UNCHANGED.
- [ ] `docs/current_eval_baseline.md` UNCHANGED.
- [ ] No outcome-evidence re-bless launched at this sub-sprint close.

When all checked: hand back to deliver-agent for close review +
per-sub-sprint Codex dispatch + Sub-sprint C-2b launch.
