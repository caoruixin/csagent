---
title: Sub-sprint C-2a — S-Auto-26 / Sprint 081 — R5 citation contract fix (ResolveArticleTool display_citation + SkillGuardrailDispatcher.handleMustCiteSource structural-shape rewrite + resolve_faq_grounded_answer.yaml citation-wording consistency)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file + docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md §4.5 (R5; updated re-scope) + compact/sprint-081-dev-prompt.md (self-contained executable view)
last_reviewed: 2026-06-06
review_cadence: per sub-sprint
supersedes: docs/sprints/sprint-080-objective.md
superseded_by: null
notes: >
  Sub-sprint C-2a of M-Auto-6. Re-scoped 2026-06-06 after dev pre-fix audit
  on the original C-2 (R5 + R6 bundle) STOPPED with zero file edits and
  found that the original prompt's anchors were wrong on both sides. The
  audit findings + the human re-scope direction (re-scope, stay in
  M-Auto-6; split into C-2a R5-only + C-2b R6-only; reuse existing
  `search_knowledge_eligible` for R6; do guardrail rewrite + skill
  wording for R5 — both within widened fences) are encoded directly in
  this contract; see §0 "Why this prompt looks different from the
  original C-2".

  C-2a ships only R5 — the citation contract fix. R6 lives in its own
  Sub-sprint C-2b (S-Auto-27 / Sprint 082) drafted as planning context
  in `compact/sprint-082-dev-prompt.md`; C-2b launches after C-2a
  dev-side close.

  Scope (citation contract change; semantic-touching at both the
  guardrail semantics surface and the skill-yaml citation-wording
  surface):
  - **R5 #1** — `ResolveArticleTool` returns an additive
    `display_citation` field: `source_url` when present and non-blank,
    else `article_id` fallback. Existing fields preserved.
  - **R5 #2** — `SkillGuardrailDispatcher.handleMustCiteSource` rewritten
    from literal `userMessage.contains(citeToken)` substring check
    (current literal-field-name behaviour at lines 357-368) to
    structural shape validation: PASS if the user-message contains a
    URL-shaped token (http:// or https:// prefix) OR an article_id-shaped
    token (Salesforce-style alphanumeric ID matching the existing
    `ka[0-9A-Za-z]+` pattern); REJECT otherwise. Empty / null /
    plain-English STILL rejected (grounding floor preserved).
  - **R5 #3** — `resolve_faq_grounded_answer.yaml` citation-wording
    consistency: skill `procedure` (:30) + `grounding_instruction` (:31)
    + line ~80 cite phrasing point at the new `display_citation` field
    via wording like "cite the display_citation token returned by
    resolve_article (URL when available, article_id otherwise)". The
    `cite_token_field: source_id` parameter at :44 is replaced or
    retired in step with the guardrail rewrite — the literal-substring
    semantics it was driving is gone.

  R5 is a **citation-guardrail contract change**, not the one-line
  yaml config swap the original C-2 prompt described. The dev audit
  confirmed the old prompt's "shape validation" assumption was wrong —
  the live guardrail does literal substring matching, and flipping
  `cite_token_field: source_id → display_citation` would have made the
  bot need to emit the literal text "display_citation" in its replies,
  which breaks the grounding floor and pre-existing tests.

  Per-sub-sprint Codex review REQUIRED. R5 is semantic-touching per
  `iteration_governance.md` §7: it touches both the LLM-facing
  citation contract (guardrail accept criteria) and the skill yaml
  citation wording (prompt_projection surface). Codex focus per §4.3
  must include the literal-substring → structural-shape semantics
  shift, the grounding-floor preservation evidence, and the no-new
  Tier-0-invariant verification.

  Forbidden: any LLM-based citation validation; any reranker or
  content-keyword match in the guardrail; any CaseSpec / eval scoring /
  baseline_dir / current_eval_baseline.md change; any
  `data/knowledge/knowledge_base_articles.json` change (C-2b's
  surface); any `IntakeFieldsRegistry` / `BudgetChecker` / `ControlKernel` /
  `AgentRunLoopImpl` / `ContextProjectionBuilder` / `UpdateIntakeFieldsTool` /
  `IntakeFieldsMerger` touch (those are A's / C-1's surfaces); any UI
  file (B's surface); any `eval_interactive/` file; autoloop 5-file
  SHA-locked scoring set; M-Auto-6 milestone-shared re-bless launch
  (deferred to M-Auto-6 close after C-2b lands).

  `baseline_dir` UNCHANGED (`m-auto-5-baseline-20260604-simfixed-stalledfix`);
  `docs/current_eval_baseline.md` UNCHANGED. NO outcome-evidence
  re-bless at this sub-sprint close — that runs at the M-Auto-6
  milestone close after C-2b lands.
---

# Sub-sprint C-2a — S-Auto-26 / Sprint 081 — R5 citation contract fix

## 0. Why this prompt looks different from the original C-2

The original C-2 prompt (R5 + R6 bundled; archived at
`compact/sprint-081-dev-prompt.md` pre-rewrite — superseded by this
contract) rested on **incorrect file anchors** that the dev pre-fix
audit caught BEFORE any code was written. The audit findings drive
this rewrite:

**Original R5 anchor errors** (caught by dev 2026-06-06):

1. The original prompt put the `must_cite_source` guardrail at
   `guardrails/MustCiteSource.java`. **No such file exists.** The
   guardrail lives at
   `SkillGuardrailDispatcher.java:340-384` (`handleMustCiteSource`) —
   a file the original prompt's fence FORBID editing.
2. The original prompt assumed the guardrail does URL/source_id
   **structural shape validation**. **It does not.** The live code is
   a literal `userMessage.contains(citeToken)` substring check, where
   `citeToken` defaults to `"source_id"` and is overridden by the yaml
   `cite_token_field` parameter (`SkillGuardrailDispatcher.java:357-368`).
   The guardrail checks whether the bot's reply literally contains the
   FIELD-NAME STRING (e.g. the literal text `"source_id"`), NOT the
   shape of any cited value. So flipping
   `cite_token_field: source_id → display_citation` would require the
   bot to emit the literal substring `"display_citation"` in its
   replies — which is absurd and would break the grounding floor.
3. The original prompt assumed `KbArticle` has `getCanonicalUrl()` +
   `getSourceId()`. **The real getters are `getSourceUrl()` +
   `getArticleId()`.** Schema columns are `source_url` + `article_id`
   per `V4__create_kb_articles.sql`.
4. The original prompt asserted the skill procedure / grounding_instruction
   was "out of scope". **But the wording at
   `resolve_faq_grounded_answer.yaml:30` ("cite the source_id") + :31
   ("cite the source_id in your user_message") + ~:80 is what
   actually steers the LLM** — `cite_token_field:44` is just a
   parameter to the literal-substring guardrail. So the one-line yaml
   flip would not have fixed c13 anyway.

This re-scope therefore widens C-2a's fence to include
`SkillGuardrailDispatcher.handleMustCiteSource` and the citation
wording in `resolve_faq_grounded_answer.yaml`, and reclassifies R5 as
a **citation-guardrail contract change** with full §7 stanza + Codex
focus on the literal→shape semantics shift + grounding-floor
preservation.

R6 (the corpus filter) is bounced to its own sub-sprint (C-2b /
S-Auto-27 / Sprint 082) because the dev audit also surfaced that the
original `bot_visible` plumbing was inert (ingestion silently drops
unknown JSON keys; the field would never reach the filter point), AND
that every article already carries a `search_knowledge_eligible: true`
field (218/218) — an ingested-but-unused governance flag whose name
matches R6's goal. C-2b plumbs the existing field through ingestion
+ entity + V17 migration + service + tool, instead of introducing a
parallel `bot_visible` mechanism.

This re-scope is **deliver-agent-authored after the dev STOP report**.
No code was changed in the audit; the dev's STOP was the right call.

## Class

**Layer (per `iteration_governance.md` §3.2):**

- **R5 #1** (ResolveArticleTool `display_citation` additive) →
  `infra` (tool result body shape change; structural derivation from
  existing article columns; zero semantic decision).
- **R5 #2** (`SkillGuardrailDispatcher.handleMustCiteSource` rewrite from
  literal-substring → structural URL/article_id shape validation) →
  `infra` (guardrail semantics rewrite). The current literal check is
  itself an `infra` choice — its replacement is also `infra`. Tier-0
  invariant: the grounding floor (require a cite token) is preserved;
  the accept criteria widens to two structural shapes; rejection of
  empty / null / plain-English is preserved.
- **R5 #3** (skill yaml citation wording at `:30 / :31 / ~:80` +
  `cite_token_field:44` retirement) → `prompt_projection`. This is a
  wording change on the LLM-facing surface and per
  `iteration_governance.md` §1.3 + §1.5 must not encode a per-UC
  matrix or a regex-on-message — and it does not (the wording points
  the LLM at a structurally-derived tool result field).

**§7 stanza requirement:** **REQUIRED** (R5 #2 + R5 #3 both touch
semantic-touching surfaces — guardrail semantics + skill yaml
wording). Full stanza in §7 below.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant.

- R5 #2's guardrail rewrite **preserves** the existing grounding
  floor: the contract is "the user-facing reply must contain a cite
  token" — that requirement is unchanged. The accept criteria widens
  from "the literal field-name string appears verbatim" to "a
  URL-shaped OR article_id-shaped token appears". The widening accepts
  citations the OLD code REJECTED iff the bot somehow cited a
  structural URL without also emitting the literal string "source_id" —
  i.e., the OLD code was failing for the right behaviour. So the
  rewrite **fixes a false-negative** in the existing grounding floor,
  not weakens it. Empty / null / plain-English replies STILL reject.
- R5 #1's additive `display_citation` field on the tool result is
  pure data; no Tier-0 surface touched.
- R5 #3's wording change directs the LLM at the new `display_citation`
  field; the skill procedure shape (search → resolve → answer →
  record) is unchanged.

**Semantic hardcode:** No semantic hardcode introduced.

- R5 #1's `display_citation` derives entirely from structured article
  columns (`source_url` if non-null and non-blank, else `article_id`);
  zero content scan, zero per-UC matrix.
- R5 #2's guardrail accept criteria is structural shape only:
  URL-shape = `startsWith("http://")` OR `startsWith("https://")`;
  article_id-shape = the existing Salesforce-style alphanumeric ID
  pattern (the dev audit nails the regex from the data; the
  illustrative pattern is `\b[kK][aA][0-9A-Za-z]{12,}\b` but the
  authoritative regex MUST be derived from the actual `article_id`
  values in `data/knowledge/knowledge_base_articles.json`). No
  keyword match on message text; no LLM call; no per-UC matrix.
- R5 #3's wording does NOT enumerate URLs, IDs, or per-UC fields —
  it points the LLM at the `display_citation` token returned by the
  tool.

## Goal

After this sub-sprint ships:

- **R5 #1 wiring**: `ResolveArticleTool` returns a `display_citation`
  field on every article result body: `source_url` when present and
  non-blank, else `article_id` fallback. Existing `article_id`,
  `source_url`, etc. preserved (additive only).
- **R5 #2 guardrail**: `SkillGuardrailDispatcher.handleMustCiteSource`
  accepts a user-facing reply iff it contains EITHER a URL-shaped
  cite token (`http://` / `https://` prefix anywhere in the message)
  OR an article_id-shaped cite token (the structural Salesforce ID
  pattern). Empty / null / plain-English rejects. The pre-existing
  test
  `SkillGuardrailDispatcherTest.mustCiteSource_doesNotFire_when_sourceIdPresent`
  (which inputs `"[source_id: kb-001]"`) continues to PASS because
  `kb-001` matches the article_id shape (or a back-compat shape; see
  §Scope #2 for the exact regex decision).
- **R5 #3 wording**: `resolve_faq_grounded_answer.yaml` procedure
  (:30), grounding_instruction (:31), and the line ~80 phrasing all
  point at the `display_citation` token returned by resolve_article.
  `cite_token_field:44` is either removed or repurposed (decision per
  the pre-fix audit; documented in handoff §1).

NOT a goal:

- Editing any other guardrail in `SkillGuardrailDispatcher.java`
  (only `handleMustCiteSource` lines 340-384 + helpers consumed only by
  it).
- Editing other skill yamls (only `resolve_faq_grounded_answer.yaml`).
- Editing `KbArticle.java` schema columns or repository (R5 #1 uses
  existing getters `getSourceUrl()` / `getArticleId()`).
- Editing `data/knowledge/knowledge_base_articles.json` (C-2b's
  surface — corpus eligibility filter).
- Editing `KnowledgeIngestionRunner` / `KnowledgeSearchService` /
  `KnowledgeHit` / `SearchKnowledgeTool` (C-2b's surface).
- Editing any UC routing / escalation posture / record_outcome guard /
  procedure flow / judge calibration / CaseSpec.
- Adding any new escalation_reason enum value.
- Adding any per-UC matrix anywhere.
- Adding any LLM-based citation validation.

## Scope (executable, #1–#5)

The dev prompt at `compact/sprint-081-dev-prompt.md` is the
self-contained executable view of this contract; sync invariant per
`prompt-artifact-rules.md` §9.3. The steps below are the canonical
version; the prompt mirrors them with cumulative context +
read-order wrappers added.

### #1 — R5 #1: `ResolveArticleTool` returns `display_citation`

**Anchors:**
- `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`
  (result body construction; verify the exact lines via pre-fix grep
  on the result-builder section).
- `server/src/main/java/com/gumtree/csagent/model/KbArticle.java`
  getters: `getSourceUrl()` returns `source_url` column;
  `getArticleId()` returns `article_id` column.

**Change:** add `display_citation` to the result body:
```java
String src = article.getSourceUrl();
String displayCitation = (src != null && !src.isBlank())
    ? src
    : article.getArticleId();
result.put("display_citation", displayCitation);
```

Existing fields (`article_id`, `source_url`, `title`, etc.) preserved.

**Hard fence:** purely additive; no field renamed; no existing field
dropped or changed.

### #2 — R5 #2: `SkillGuardrailDispatcher.handleMustCiteSource` structural-shape rewrite

**Anchor:** `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java:340-384`
(method body shown for reference in this contract since the current
shape is a small literal-substring check). Current behaviour: PASS iff
`userMessage.contains(citeToken)` where `citeToken` is the literal
value of the yaml `cite_token_field` param (default `"source_id"`).

**Pre-fix audit (MANDATORY before #2 implementation):**

- (a) Read `SkillGuardrailDispatcher.java:340-384` end-to-end and
  enumerate every input the method reads (`outcomeClass`,
  `g.parameters().get("outcome_class")`, `g.parameters().get("cite_token_field")`,
  `ctx.parsedUserMessage()`, `ctx.lastLlmRawResponse()`).
- (b) Read the existing tests pinning the method:
  - `SkillGuardrailDispatcherTest.mustCiteSource_doesNotFire_when_sourceIdPresent`
    (named in dev audit; verify exact path).
  - Any other test file matching `MustCiteSource*` or `SkillGuardrailDispatcher*`.
  - Enumerate every input shape the existing tests cover. Each one is
    a back-compat invariant that must remain GREEN after the rewrite.
- (c) Inspect the actual `article_id` values in
  `data/knowledge/knowledge_base_articles.json` and derive the strict
  article_id-shape regex from real data. Illustrative pattern:
  `\bka[0-9A-Za-z]+\b`. Authoritative: derive from data. The pattern
  must:
  - Match every existing `article_id` value in the corpus.
  - Match the back-compat test inputs (`"[source_id: kb-001]"` →
    decide whether the test should be updated to use a real article_id
    or whether the regex should be widened to a generic
    `\b[A-Za-z]+-?[A-Za-z0-9]+\b`-ish ID shape; document the choice).
  - Reject plain English (no false positives on natural-language
    replies).

**Change:** replace the literal-substring check at
`SkillGuardrailDispatcher.java:362-367` with a structural-shape
predicate:

```java
String userMessage = ctx.parsedUserMessage().orElse(null);
if (userMessage == null || userMessage.isBlank()) {
    userMessage = ctx.lastLlmRawResponse();
}
if (userMessage != null && containsAcceptableCiteToken(userMessage)) {
    return Optional.empty();
}
```

Where `containsAcceptableCiteToken` is a new private helper:

```java
private static boolean containsAcceptableCiteToken(String userMessage) {
    if (userMessage == null) return false;
    // URL-shape: http:// or https:// prefix anywhere
    if (USER_FACING_URL_PATTERN.matcher(userMessage).find()) return true;
    // article_id-shape: structural Salesforce-style ID (derived from
    // real corpus values in #2 pre-fix audit (c))
    if (ARTICLE_ID_PATTERN.matcher(userMessage).find()) return true;
    return false;
}
```

The two `Pattern` constants are private static final, defined at the
top of the class. The yaml `cite_token_field` parameter is no longer
consulted by the guardrail (the citation-token surface is now the
tool's `display_citation` field, not the yaml param). The yaml param
is either removed in #3 or kept as a documentation breadcrumb (see
#3 audit).

The reject branch (the trace map + `RejectVerdict` construction at
`:369-383`) is unchanged in shape; only the `cite_token_field` trace
key may be removed or renamed (per #3).

**Hard fence:**
- The widening MUST preserve all reject negatives that exist today:
  empty / null / plain English / random keyword strings still reject.
- The structural patterns MUST NOT use semantic / LLM-based
  validation, MUST NOT match on user-message keywords, MUST NOT enumerate
  any UC.
- The article_id pattern's regex MUST be derived from real corpus
  data, not invented.

### #3 — R5 #3: `resolve_faq_grounded_answer.yaml` citation-wording consistency

**Anchor:**
- `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml`
  lines `:30` (procedure), `:31` (grounding_instruction), `:44`
  (`cite_token_field`), `~:80` (cite phrasing in another section —
  pre-fix audit identifies the exact line).

**Pre-fix audit (MANDATORY before #3 implementation):**

- (a) Read the full yaml file end-to-end. Enumerate EVERY line that
  references `source_id` or citation token wording. Document each in
  handoff §1 with line number + the wording change planned.
- (b) Identify whether `cite_token_field` is consumed anywhere outside
  the guardrail (grep across `server/src/main` for the literal
  string `cite_token_field`). If consumed only by the guardrail (which
  the rewrite no longer reads), it can be REMOVED. If consumed
  elsewhere (e.g. a config-load smoke test or a different guardrail),
  the audit must surface it and the change adapts.

**Change:**

- Update `procedure` (:30) citation phrasing from "cite the source_id"
  to "cite the `display_citation` token returned by resolve_article
  (the article's source_url when available, otherwise its article_id)".
  Adapt grammar to match the surrounding sentence — minimum-edit
  diff.
- Update `grounding_instruction` (:31) similarly.
- Update the line ~80 cite phrasing similarly (exact wording derived
  from the audit).
- Either REMOVE `cite_token_field:44` (clean removal if no other
  consumer per audit (b)) OR rename it to `cite_token_source: display_citation`
  to make it explicit that the new authoritative source is the tool
  result field, not a literal-substring on a field name (declarative,
  not the old behavioural-driver; documented as a back-compat hint).
  Decision recorded in handoff §1.

**Hard fence:**
- ONLY the citation-token wording is touched. The skill's
  goal / role / objective / search-knowledge-must-precede /
  faq_miss escalation rule / non-citation paragraphs are byte-untouched.
- No procedure-step reordering, no new tool invocation rule, no UC
  routing change.
- The minimum-edit diff is captured in handoff §1.

### #4 — R5 tests for #1 + #2 + #3

**Anchors (new / extended test files):**

- `server/src/test/java/com/gumtree/csagent/service/tools/ResolveArticleToolTest.java`
  (existing or new; extend with `display_citation` tests).
- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcherTest.java`
  (existing; extend or replace the relevant `mustCiteSource*` tests
  with structural-shape coverage).
- Optionally a new
  `server/src/test/java/com/gumtree/csagent/service/runtime/skill/MustCiteSourceShapeTest.java`
  focused on the new helper if it gets its own class — design
  choice in pre-fix audit.

**R5 #1 tests:**
- Positive A — `source_url` non-null and non-blank →
  `display_citation` equals `source_url`.
- Positive B — `source_url` null → `display_citation` equals
  `article_id`.
- Positive C — `source_url` blank → `display_citation` equals
  `article_id`.
- Negative A — existing `article_id` field preserved on the result
  body.
- Negative B — existing `source_url` field preserved on the result
  body (if currently present).

**R5 #2 tests (the citation-guardrail rewrite — primary structural-shape
positives + the existing back-compat negatives):**

- Positive A — user_message contains a URL-shape (`https://help.gumtree.com/...`)
  → guardrail PASS (returns `Optional.empty()`).
- Positive B — user_message contains a URL-shape with http:// prefix
  → PASS.
- Positive C — user_message contains an article_id-shape token
  (`ka41r000000LIEEAA4`) → PASS.
- Positive D — user_message contains the legacy test shape
  `[source_id: kb-001]` → PASS (per back-compat — the article_id-shape
  regex MUST cover this OR the test MUST be updated to use a real
  article_id, with the decision documented).
- Negative A — user_message is empty → REJECT.
- Negative B — user_message is null (both `parsedUserMessage` and
  `lastLlmRawResponse`) → REJECT.
- Negative C — user_message contains only plain English (no URL, no
  article_id-shape) → REJECT.
- Negative D — user_message contains the literal string `"source_id"`
  WITHOUT a URL or actual article_id-shape (e.g. "I'll cite the
  source_id") → REJECT (the OLD literal-substring code would have
  PASSED this — confirms the literal-vs-structural shift).
- Negative E — outcome_class is not `resolve` / `resolved` → guardrail
  bows out as before (Sprint 39 contract preserved).
- Negative F — yaml `outcome_class` parameter disagrees → guardrail
  bows out as before.

**R5 #3 tests:**
- Yaml load smoke (config-load test if one exists; if not, a focused
  integration test that loads the yaml and asserts the citation-token
  wording / `cite_token_field` presence-or-removal matches the audit
  decision).

### #5 — Backend rebuild + integration smoke (no real-LLM run)

After #1-#4:
- `cd server && mvn -o -DskipTests package` → BUILD SUCCESS.
- Full `mvn -o test` → Java baseline `1327 / 1 / 0 / 2` preserved +
  R5 additions; sole pre-existing failure
  (`SystemPromptUserRequestedTiebreakerTest`, OQ-S41.5) preserved; no
  new failures.
- Document the rebuild + test summary in handoff §2.

No real-LLM smoke this sub-sprint. Outcome evidence is the M-Auto-6
milestone-shared re-bless after C-2b lands.

## Anti-误杀 invariants (HARD, non-negotiable)

1. **Grounding floor preserved.** The contract "user-facing reply
   must contain a cite token" is unchanged. Empty / null / plain
   English STILL reject.
2. **Structural validation only.** The new guardrail accept criteria
   is URL-shape OR article_id-shape; NO LLM-based validation, NO
   content-keyword match.
3. **Article_id pattern derived from real data.** Not invented;
   not relaxed beyond what the data demands.
4. **Skill yaml wording change is minimum-edit.** Only the
   citation-token phrasing is touched. Goal / role / objective /
   search-must-precede / faq_miss rule unchanged.
5. **`cite_token_field` retirement is consistent.** Either removed
   cleanly (no orphan consumer) or repurposed as a declarative
   `cite_token_source: display_citation` hint with no behavioural
   role.
6. **Additive `display_citation` only.** Existing tool result fields
   unchanged.
7. **No `data/knowledge/knowledge_base_articles.json` edit.** C-2b's
   surface; corpus governance lives there.
8. **No new `escalation_reason` enum value.**
9. **No new Tier-0 invariant.**
10. **`baseline_dir` UNCHANGED. `docs/current_eval_baseline.md`
    UNCHANGED.**

## Hard fences / STOP conditions

**Files allowed to edit** (fence):

- `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java`
  (**ONLY** `handleMustCiteSource` lines 340-384 + new private helper +
  the two `Pattern` constants at the top of the class + the
  `RejectVerdict` trace-map key referencing `cite_token_field` if
  that key is removed/renamed in step with #3)
- `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml`
  (**ONLY** the citation-token wording lines + `cite_token_field`
  parameter; pre-fix audit enumerates exact lines)
- `server/src/test/java/com/gumtree/csagent/service/tools/ResolveArticleToolTest.java`
  (extend or new)
- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcherTest.java`
  (extend or relevant `mustCiteSource*` tests rewritten)
- Optionally a new
  `server/src/test/java/com/gumtree/csagent/service/runtime/skill/MustCiteSourceShapeTest.java`
- `docs/sprints/sprint-081-handoff.md` (dev handoff)

**Files FORBIDDEN to edit**:

- Any OTHER guardrail handler in `SkillGuardrailDispatcher.java`
  (`handleFaqMiss`, `handleIntakeRequiredFields`, etc.).
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

- Pre-fix audit (b) on `SkillGuardrailDispatcher.handleMustCiteSource`
  surfaces an unexpected shape (e.g. it's also consumed by another
  outcome class beyond `resolve` / `resolved`, or it has unexpected
  callers) → STOP, surface to deliver-agent.
- Pre-fix audit (c) on the article_id-shape regex surfaces that the
  back-compat test inputs (`kb-001` style) CANNOT be represented by
  any narrow structural pattern that also excludes plain English →
  STOP, surface (decision is whether to update the test inputs to use
  real article_ids vs widen the regex; deliver-agent decides).
- Pre-fix audit on yaml `cite_token_field` consumers surfaces a
  consumer outside the guardrail → STOP (widens scope beyond C-2a;
  surface).
- Any new test in #4 fails in a way that suggests the grounding floor
  is weakened (a plain English reply PASSES the new guardrail) →
  STOP, anti-误杀 #1 violation.
- Any file outside the fence is touched → STOP, revert, re-launch.
- A second `must_cite_source` config-key consumer surfaces in
  `server/src/main` → STOP.

## Test / eval requirements

- All new tests in #4 GREEN.
- Existing Java baseline `1327 / 1 / 0 / 2` preserved + R5 additions
  (~+10-15 tests). Sole pre-existing failure (`OQ-S41.5`) preserved.
  Specifically, `SkillGuardrailDispatcherTest.mustCiteSource_doesNotFire_when_sourceIdPresent`
  either continues to PASS as-is OR is updated to use a real
  article_id (decision in pre-fix audit (c)); the structural-shape
  rewrite must not silently regress it.
- Eval pytest `553` UNCHANGED (no eval-side change).
- Autoloop pytest `324` UNCHANGED.
- Backend rebuild at #5 documented in handoff §2.
- **No real-LLM re-bless at this sub-sprint close.** Outcome evidence
  is the M-Auto-6 milestone-shared re-bless after C-2b lands.
- Mocked / unit tests cover the wiring; real-LLM evidence is the
  milestone re-bless (§5.7 separation).

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
appears verbatim" to "URL-shape OR article_id-shape token appears"
— a fix to a false-negative in the current literal check, not a
weakening of the floor. Empty / null / plain-English STILL reject.
R5 #1 is purely additive. R5 #3 changes wording on the
LLM-facing surface to point at the new `display_citation` field;
procedure shape preserved.

**Semantic hardcode:** No semantic hardcode introduced.
- R5 #1: `display_citation` derives entirely from structured article
  columns (`source_url` if non-null/non-blank else `article_id`); zero
  content scan; zero per-UC matrix.
- R5 #2: accept criteria is structural URL prefix (`http://`/`https://`)
  + Salesforce-style article_id regex derived from real corpus data
  (NOT invented). No LLM-based validation; no content-keyword match
  on the user message.
- R5 #3: wording points at the tool result field
  (`display_citation`), not at a hard-coded URL list or per-UC
  matrix. Minimum-edit diff; procedure / role / objective /
  search-must-precede / faq_miss rule untouched.

**Generalization coverage:** target / neighbor / negative / shadow case
counts: 1 / ~6 / ~10 / 0
- target: c13 (bot cites article_id when source_url is available).
- neighbor: every FAQ-grounded-resolve case that cites an article
  (R5 is universal — every `ResolveArticleTool` call gets
  `display_citation`; every guard fire uses the structural-shape
  check); URL-less articles (38 known) where `display_citation`
  falls back to `article_id`.
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

## Codex review plan (per `process/milestone-framework.md` §4.3)

**Per-sub-sprint Codex review REQUIRED** because R5 #2 touches the
LLM-facing citation contract (guardrail accept criteria semantics
change) and R5 #3 touches the LLM-facing skill yaml wording. Both
semantic-touching per `iteration_governance.md` §7 +
`process/milestone-framework.md` §4.3.

Codex prompt artifact: `compact/sprint-081-codex-review-prompt.md`
(deliver-agent authors at sub-sprint close, embeds §4.1 nine-question
kernel + §7 stanza + file-path fence + anti-误杀 invariants +
generalization coverage + the **literal-substring → structural-shape
semantics shift evidence + grounding-floor preservation evidence**
as a §3 focal point).

**Focus points for Codex** (per §4.3):

- **F1 (structural shape, not LLM-based)**: confirm R5 #2's accept
  criteria is purely structural (URL prefix + Salesforce-style
  article_id regex from data); confirm no LLM call, no content
  keyword, no per-UC matrix.
- **F2 (grounding-floor preservation)**: confirm the rewrite does
  NOT weaken the grounding floor. The new negatives (empty / null /
  plain English / `source_id`-literal-without-URL-or-real-ID) must
  reject; the new positives (URL + real article_id + back-compat
  test shape) must pass.
- **F3 (literal → shape semantics shift)**: confirm the OLD literal
  `userMessage.contains(citeToken)` is fully removed; the new
  structural predicate is implemented as the §Scope #2 design (two
  `Pattern` constants + the `containsAcceptableCiteToken` helper);
  the `cite_token_field` yaml param is no longer consumed by the
  guardrail.
- **F4 (skill yaml wording minimum-edit)**: confirm R5 #3's diff
  touches ONLY citation-token wording lines (`:30 procedure / :31
  grounding_instruction / ~:80 / :44 cite_token_field`); confirm
  goal / role / objective / search-must-precede / faq_miss rule
  byte-untouched.
- **F5 (article_id regex derived from data)**: confirm the regex
  matches every existing `article_id` value in the corpus AND
  rejects plain-English replies (no false positives observed in the
  test suite).
- Q1 / Q3 / Q5 / Q7 / Q8 / Q9 covered via the focal points above.

## Handoff requirements (dev authors `docs/sprints/sprint-081-handoff.md`)

§1 of the handoff must include:

- For each of #1-#3: file:line ranges + rationale + the test name(s)
  that gate it.
- **#2 pre-fix audit outcome (a) + (b) + (c)**:
  - (a) Every input the current method reads.
  - (b) Every existing `mustCiteSource*` test's input shape + whether
    it stays GREEN as-is or needs an update.
  - (c) The chosen `article_id`-shape regex + evidence it matches every
    existing `article_id` value + evidence it rejects plain English.
- **#3 pre-fix audit outcome**:
  - Every yaml line touched (line number + before/after wording).
  - Whether `cite_token_field` is REMOVED or REPURPOSED (decision +
    rationale).
  - Every grep hit for `cite_token_field` across
    `server/src/main` proving no orphan consumer (or if found, the
    handling).
- Java test results (full numeric: passed / failed / skipped / errors).
- Eval pytest / autoloop pytest results (UNCHANGED).
- STOP confirmations:
  - File fence respected; no forbidden file touched.
  - No `data/knowledge/knowledge_base_articles.json` edit.
  - No `KnowledgeIngestionRunner` / `KnowledgeSearchService` /
    `KnowledgeHit` / `SearchKnowledgeTool` edit.
  - No other guardrail handler touched.
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
   Safe partial; can ship independently.
2. **Commit 2 — R5 #2 pre-fix audit (a) + (b) + (c)**: pre-fix audit
   evidence captured in handoff §1 only (no code change). Optional
   if (a) + (b) + (c) are documented in the same commit as #2's code
   change.
3. **Commit 3 — R5 #2 (guardrail rewrite)**: `SkillGuardrailDispatcher`
   `handleMustCiteSource` rewrite + new private helper + two
   `Pattern` constants + `SkillGuardrailDispatcherTest` extensions or
   new `MustCiteSourceShapeTest`.
4. **Commit 4 — R5 #3 (skill yaml wording)**: `resolve_faq_grounded_answer.yaml`
   citation-token wording change + `cite_token_field` retirement +
   any yaml-load smoke test.
5. **Commit 5 — Backend rebuild + integration smoke evidence (#5)**:
   handoff only (no code change).
6. **Commit 6 — Dev handoff**: `docs/sprints/sprint-081-handoff.md`.

## Self-check checklist (dev completes before claiming done)

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
- [ ] No edit to `IntakeFieldsRegistry` / `SkillGuardrailDispatcher`
      handlers other than `handleMustCiteSource` / `BudgetChecker` /
      `ControlKernel` / `AgentRunLoopImpl` / `ContextProjectionBuilder` /
      `UpdateIntakeFieldsTool` / `IntakeFieldsMerger`.
- [ ] §7 stanza copied verbatim into handoff.
- [ ] `baseline_dir` UNCHANGED.
- [ ] `docs/current_eval_baseline.md` UNCHANGED.
- [ ] No outcome-evidence re-bless launched at this sub-sprint close.

When all checked: hand back to deliver-agent for close review +
per-sub-sprint Codex dispatch + Sub-sprint C-2b launch.
