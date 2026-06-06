---
title: Sprint 081 / S-Auto-26 / M-Auto-6 Sub-sprint C-2a dev handoff — R5 citation-guardrail contract (display_citation + structural must_cite_source + skill wording)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: code (server/.../service/tools/ResolveArticleTool.java, service/runtime/skill/SkillGuardrailDispatcher.java, resources/skills/resolve_faq_grounded_answer.yaml)
last_reviewed: 2026-06-06
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  M-Auto-6 Sub-sprint C-2a. R5 (trigger c13): the bot cited an article_id even
  when the article had a source_url. Three additive/structural fixes: (#1)
  ResolveArticleTool result body now carries display_citation = source_url when
  present and non-blank, else article_id fallback; (#2) must_cite_source
  guardrail rewritten from a literal "source_id" substring check to a structural
  shape predicate accepting URL-shape OR article_id-shape tokens (grounding floor
  preserved — empty/null/plain-English still reject); (#3) resolve_faq_grounded
  _answer.yaml citation wording points at display_citation and cite_token_field
  removed cleanly (no orphan consumer). FENCE EXPANSIONS (2): ResolveFaqGuardrails
  Test.java (deliver-agent approved) + PhaseEvaluatorResolveSkillIntegrationTest
  .java (golden procedure/grounding text — mechanically forced by the approved
  R5 #3 wording change, judgment-free). NO outcome-evidence re-bless this
  sub-sprint; deferred to the M-Auto-6 milestone-shared re-bless after C-2b.
  baseline_dir + docs/current_eval_baseline.md UNCHANGED.
---

# Sprint 081 / S-Auto-26 / M-Auto-6 Sub-sprint C-2a — dev handoff

## §0 Cold-start summary + verdict

**Goal.** Ship R5 — a citation-guardrail contract change that lets the bot
cite either a URL or a structural article_id, keeping the grounding floor
intact. Trigger case **c13**: the bot answered with `(Source: ka4P200000003sLIAQ)`
even though the article had a `source_url`. Two root causes: (i) the LLM had no
signal the URL was preferred, and (ii) `ResolveArticleTool` exposed only
`article_id` + `source_url` separately, not a single preferred display token.

- **R5 #1** (`infra`) — `ResolveArticleTool` returns an additive
  `display_citation` field: `source_url` when present and non-blank, else
  `article_id` fallback.
- **R5 #2** (`infra`) — `SkillGuardrailDispatcher.handleMustCiteSource`
  rewritten from `userMessage.contains("source_id")` (a literal field-NAME
  substring check) to a structural shape predicate: accept iff the reply
  contains a URL-shape token (`http(s)://…`) OR an article_id-shape token
  (Salesforce-style `ka…` 18-char id, derived from the real corpus).
- **R5 #3** (`prompt_projection`) — `resolve_faq_grounded_answer.yaml`
  citation wording (procedure `:30`, grounding_instruction `:31`,
  critical-step desc `:79`) points at `display_citation`; `cite_token_field`
  removed cleanly.

Zero semantic procedure / wording change beyond the citation-token references.

**Verdict.** All of #1–#5 landed. **Java `mvn -o clean test` = 1337 run / 1 fail
/ 0 err / 2 skip.** The single failure is the inherited
`SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`
(OQ-S41.5) — the same documented pre-existing failure carried at the sub-sprint
launch baseline `1327 / 1 / 0 / 2`; it is unrelated to citations (it pins the
ACTIVE-UC TIEBREAKER system-prompt anchor, a surface this sub-sprint does not
touch). Net **+10 R5 tests, no regressions** (1327 + 10 = 1337). Backend
`mvn -o -DskipTests package` = **BUILD SUCCESS**. **No real-LLM outcome
re-bless** — deferred to the M-Auto-6 milestone-shared run after C-2b.
`baseline_dir` + `docs/current_eval_baseline.md` **UNCHANGED**.

---

## §1 Implementation

### #1 — R5 #1: `ResolveArticleTool` `display_citation`

- **File:line:** `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java:107-111`.
- **Change:** added `data.put("display_citation", !canonicalUrlMissing ? canonicalUrl : article.getArticleId());`
  immediately after the `source_url` / `canonical_url` / `canonical_url_missing`
  puts. Reuses the already-computed `canonicalUrl` (`= article.getSourceUrl()`,
  `:84`) and `canonicalUrlMissing` (`= canonicalUrl == null || canonicalUrl.isBlank()`,
  `:85`), so `display_citation` = `source_url` iff non-null and non-blank, else
  `article_id`. Purely additive — no existing field renamed, dropped, or reshaped.
- **Gating tests:** `ResolveArticleToolTest.java:160-225` —
  `execute_displayCitation_isSourceUrl_whenSourceUrlPresent`,
  `execute_displayCitation_fallsBackToArticleId_whenSourceUrlNull`,
  `execute_displayCitation_fallsBackToArticleId_whenSourceUrlBlank`,
  `execute_displayCitation_isAdditive_existingFieldsPreserved` (asserts
  `article_id` + `source_url` still populated as-was).

### #2 — R5 #2: `handleMustCiteSource` structural-shape rewrite

- **File:lines:** `SkillGuardrailDispatcher.java` — two `Pattern` constants
  `:96-110` (`USER_FACING_URL_PATTERN` `:99`, `ARTICLE_ID_PATTERN` `:107`);
  method body rewrite `:358-405`; new private helper
  `containsAcceptableCiteToken` `:407-412`; trace-key rename + reject-message
  rewrite `:382-405`. Added `import java.util.regex.Pattern;`.
- **What changed:** the `citeToken` extraction block (old `:357-361`, which read
  the default `"source_id"` and the optional `cite_token_field` override) is
  **removed**. The `userMessage` read order (`ctx.parsedUserMessage()` →
  `ctx.lastLlmRawResponse()`) is preserved. The old
  `userMessage.contains(citeToken)` literal substring check is replaced by
  `containsAcceptableCiteToken(userMessage)` (URL-shape OR article_id-shape;
  null-safe). The pre-outcome-class checks (`outcomeClass` null / normalize to
  `resolve`/`resolved` / yaml `outcome_class` agreement) are **byte-preserved**.
- **Trace + message:** trace key `cite_token_field` → `cite_token_validator`
  with constant value `"structural_url_or_article_id"`; reject message rewritten
  to name "either a source_url (http(s)://...) or a structural article_id" and
  "Cite the display_citation value returned by resolve_article".
- **Javadoc:** method javadoc (`:347-357`) + class-level S1 section (`:63-68`)
  updated for accuracy (literal `source_id` presence → "citation-token presence
  — URL-shape source_url or structural article_id"). No other handler touched.
- **Gating tests:** `SkillGuardrailDispatcherTest.java` —
  `mustCiteSource_doesNotFire_when_articleIdShapePresent` (renamed from the old
  `…_when_sourceIdPresent`, fixture updated to a real article_id),
  `…_when_httpsUrlPresent`, `…_when_httpUrlPresent`,
  `mustCiteSource_fires_when_emptyUserMessage`,
  `mustCiteSource_fires_when_nullUserMessage`,
  `mustCiteSource_fires_when_literalSourceIdWordButNoStructuralToken` (pins the
  literal→shape shift; the OLD code PASSED this),
  `mustCiteSource_bowsOut_when_yamlOutcomeClassDisagrees`. Plus the preserved
  `…_for_classEscalate` / `…_for_classAbandon` / `…_outside_resolve_faq_scope`
  / `…_handlesRESOLVEDLegacyAlias` / `…_fires_when_classResolve_andNoSourceId`
  (plain-English Negative C). Integration mirror in `ResolveFaqGuardrailsTest.java`.

### #3 — R5 #3: `resolve_faq_grounded_answer.yaml` citation wording

- **File:lines:** `resolve_faq_grounded_answer.yaml:30` (procedure), `:31`
  (grounding_instruction), `:43` (parameters: `cite_token_field` removed,
  `outcome_class: resolve` retained), `:79` (critical-step desc).
- **Before/after:**
  - `:30 procedure` — `…answer (with a source_id citation) -> record_outcome.`
    → `…answer (with a display_citation citation — the article's source_url when
    available, otherwise its article_id) -> record_outcome.`
  - `:31 grounding_instruction` — `…cite the source_id in your user_message.`
    → `…cite the display_citation token returned by resolve_article in your
    user_message (URL when available, article_id otherwise).`
  - `:43` — `cite_token_field: source_id` **REMOVED**; the `must_cite_source`
    parameters block keeps only `outcome_class: resolve`.
  - `:79 critical_steps[resolve-article-after-search-hit].desc` —
    `…depends on for the \`source_id\` citation.` →
    `…depends on for the \`display_citation\` citation.`
- **Minimum-edit confirmed** via `git diff` (3 insertions / 4 deletions; goal /
  role / objective / search-must-precede / faq_miss escalation rule
  byte-untouched).

---

## §1.1 Pre-fix audit outcomes

### #2 audit (a) — every input `handleMustCiteSource` reads

`outcomeClass` (arg); `g.parameters().get("outcome_class")`;
`g.parameters().get("cite_token_field")` (the now-removed override);
`ctx.parsedUserMessage()`; `ctx.lastLlmRawResponse()`. Sole caller is
`checkBeforeOutcomePersist`; the only outcome classes that reach the body are
`resolve` / `resolved` (legacy alias). No unexpected callers, no second outcome
class — **no STOP from (a)**. Old `RejectVerdict` trace keys (`:369-383`):
`predicate_name`, `decision_outcome`, `skill_name`, `reject_reason_label`,
`outcome_class_requested`, `cite_token_field` (→ renamed `cite_token_validator`),
`user_message_present`.

### #2 audit (b) — existing `mustCiteSource*` tests + `cite_token_field` consumers

`grep -rn cite_token_field server/src/main` → only the yaml `:44` and the
guardrail (`:358` read, `:375` trace). **No test asserts `cite_token_field`;
no other main-source consumer.** Decision: **REMOVE cleanly** (anti-误杀 #5
satisfied — no orphan consumer). `SkillLoaderTest` / `SkillTest` use
`must_cite_source` with empty / `outcome_class`-only parameters, so removal does
not break them.

Existing must_cite_source tests and their post-rewrite fate:

| Test file | Test | Old input | Post-rewrite |
|---|---|---|---|
| SkillGuardrailDispatcherTest | `…_doesNotFire_when_sourceIdPresent` | `[source_id: kb-001]` | **updated** input → real `ka…` id; renamed `…_when_articleIdShapePresent` (PASS) |
| SkillGuardrailDispatcherTest | `…_fires_when_classResolve_andNoSourceId` | plain English | PASS (still rejects — Negative C) |
| SkillGuardrailDispatcherTest | `…_for_classEscalate` / `…_for_classAbandon` / `…_outside_resolve_faq_scope` / `…_handlesRESOLVEDLegacyAlias` | — | PASS unchanged |
| ResolveFaqGuardrailsTest | `mustCiteSource_neighbor_passes_withSourceIdInUserMessage` | `[source_id: kb-001]` | **updated** → real `ka…` id (PASS) |
| ResolveFaqGuardrailsTest | `prematureResolve_neighbor_allows_resolveInClosePhase` | `[source_id: kb-001]` | **updated** → real `ka…` id (PASS) |
| ResolveFaqGuardrailsTest | `mustCiteSource_target_rejects_…withoutSourceId` | plain English | PASS unchanged |

The `viableHits()` helper's `"source_id":"kb-001"` is a **search-hit field**
read by the faq_miss handler (checks `hits` non-empty), NOT a user-facing
citation token through the structural check — left untouched.

### #2 audit (c) — article_id-shape regex derived from real corpus

`data/knowledge/knowledge_base_articles.json` has **218** article_ids;
**100% match `^ka`**, **100% exactly 18 chars** (`ka` + 16 alphanumerics).
Sample: `ka4P2000000021pIAA`, `ka44J000000CupTQAS`, `ka41r000000LIEEAA4`,
`ka4P200000003sLIAQ` (the c13 trigger id). Chosen pattern:
**`\bka[A-Za-z0-9]{16}\b`** — matches every corpus id, and the exact 18-char
length rejects plain English (e.g. "kangaroo" = 8 chars; the
`…_fires_when_literalSourceIdWordButNoStructuralToken` Negative confirms
"I'll cite the source_id for you." rejects). The wider candidate
`[A-Za-z]{2,}[0-9][A-Za-z0-9]+` was **rejected**: it would false-accept
plain-English alphanumerics like "iPhone12" / "Win10" (weakening the grounding
floor) AND still would not match `kb-001` (the hyphen). Because the narrow
pattern does not match the legacy fixture `kb-001`, the three `kb-001` user-
message fixtures were updated to a real corpus id `ka41r000000LIEEAA4` (audit-(b)
"preferred: update test inputs to real article_ids").

### #3 audit — yaml citation references

`grep -n source_id resolve_faq_grounded_answer.yaml` → exactly 4 lines
(`:30`, `:31`, `:44`, `:80` pre-edit). All four were the citation-token
references; all four changed per §1 #3. Zero `source_id` / `cite_token_field`
references remain in the yaml post-edit (verified).

---

## §2 Test / eval results

- **Java `mvn -o clean test`** = **1337 run / 1 fail / 0 err / 2 skip**.
  - Baseline at launch: `1327 / 1 / 0 / 2`. Net **+10** tests (R5 #1 +4 on
    `ResolveArticleToolTest`; R5 #2 +6 on `SkillGuardrailDispatcherTest`; the
    other touched test files updated fixtures/golden text in place, net 0).
  - **Sole failure = inherited OQ-S41.5**
    (`SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`),
    unrelated to citations. No new failures; the 7 FAQ golden-prompt assertions
    in `PhaseEvaluatorResolveSkillIntegrationTest` PASS after the golden
    `FAQ_PROCEDURE` + `FAQ_GROUNDING` constants were updated to mirror the new
    yaml verbatim.
- **Backend `mvn -o -DskipTests package`** = **BUILD SUCCESS** (jar built).
- **Eval pytest `553` / autoloop pytest `324`** — **UNCHANGED** (no
  `eval_interactive/` or `autoloop/` file touched; `git status` shows only the
  7 fenced code/test files + this handoff). Re-run not required (no eval-side
  surface touched per §5.7).

### Wiring evidence vs outcome evidence (§5.7 mocked-LLM gate)

This sub-sprint delivers **wiring evidence only**: JUnit tests assert the tool
result-body shape (`display_citation`), the guardrail accept/reject predicate
(structural URL/article_id shape), and the skill-yaml load wording. None of
these involve a real LLM choosing what to cite, so per §5.7 they are NOT
outcome evidence that the bot's c13 citation behaviour changed. **Outcome
evidence** (does the bot now cite the URL when one exists) is the M-Auto-6
milestone-shared real-LLM re-bless after C-2b lands. No re-bless launched here.

---

## §3 STOP / fence confirmations

- **File fence respected** with **two approved expansions** (both
  surface-pinning test files for the must_cite_source / yaml-procedure surface):
  1. `ResolveFaqGuardrailsTest.java` — **deliver-agent approved** (the narrow
     `ka…` pattern breaks its two `kb-001` user-message fixtures; updated to a
     real corpus id). Surfaced at audit (b) per the STOP condition.
  2. `PhaseEvaluatorResolveSkillIntegrationTest.java` — golden `FAQ_PROCEDURE` +
     `FAQ_GROUNDING` constants that pin the yaml procedure/grounding text
     verbatim. **Mechanically forced** by the approved R5 #3 wording change
     (judgment-free — the golden test must mirror the new yaml). Discovered at
     build time (the #3 audit grepped main-source consumers, not test golden
     assertions). Flagged for Codex scope review.
- Files touched (8): the 3 main files (`ResolveArticleTool.java`,
  `SkillGuardrailDispatcher.java`, `resolve_faq_grounded_answer.yaml`), 4 test
  files (`ResolveArticleToolTest`, `SkillGuardrailDispatcherTest`,
  `ResolveFaqGuardrailsTest`, `PhaseEvaluatorResolveSkillIntegrationTest`), and
  this handoff. **Nothing else.**
- **No** `data/knowledge/knowledge_base_articles.json` edit (C-2b's surface).
- **No** `KnowledgeIngestionRunner` / `KnowledgeSearchService` / `KnowledgeHit`
  / `SearchKnowledgeTool` edit (C-2b's surface).
- **No other guardrail handler** in `SkillGuardrailDispatcher.java` touched
  (`handleFaqMiss…` / `handleIntakeCompleteRequired` /
  `handlePrematureResolveOutcomeGuard` byte-untouched).
- **No other yaml** file or non-citation yaml line touched.
- **No** `KbArticle.java` / `KbArticleRepository.java` edit.
- **No** `IntakeFieldsRegistry` / `IntakeFieldsMerger` / `BudgetChecker` /
  `ControlKernel` / `AgentRunLoopImpl` / `ContextProjectionBuilder` /
  `UpdateIntakeFieldsTool` edit (A's / C-1's surfaces). **No** UI file (B's
  surface). **No** `eval_interactive/` file. **No** autoloop scoring-set file.
- **No** new `escalation_reason` enum value. **No** per-UC matrix. **No**
  LLM-based citation validation. **No** new Tier-0 invariant.
- `baseline_dir` **UNCHANGED**; `docs/current_eval_baseline.md` **UNCHANGED**.
- **No real-LLM outcome re-bless** launched (deferred to milestone close).

---

## §7 Layer-classification + anti-hardcode stanza

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

---

## §4 Commit map

1. **Commit 1 — R5 #1 (additive):** `ResolveArticleTool` `display_citation`
   field + `ResolveArticleToolTest` extensions.
2. **Commit 2 — R5 #2 (guardrail rewrite):** `SkillGuardrailDispatcher`
   `handleMustCiteSource` rewrite + helper + two `Pattern` constants +
   `SkillGuardrailDispatcherTest` extensions + `ResolveFaqGuardrailsTest`
   fixture updates (fence expansion #1).
3. **Commit 3 — R5 #3 (skill yaml wording):** `resolve_faq_grounded_answer.yaml`
   citation-token wording + `cite_token_field` removal +
   `PhaseEvaluatorResolveSkillIntegrationTest` golden-text update (fence
   expansion #2).
4. **Commit 4 — Dev handoff:** this file.
