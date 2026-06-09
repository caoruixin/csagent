# Sprint 081 / S-Auto-26 / M-Auto-6 Sub-sprint C-2a — Codex per-sub-sprint review prompt (Anti-Hardcode + citation-guardrail literal→shape semantics shift + skill yaml citation-wording minimum-edit + fence-expansion #2 scope call)

## Role identity

You are the **Anti-Hardcode + Per-Sub-Sprint Review Agent** for **Sprint
081 / S-Auto-26 / M-Auto-6 Sub-sprint C-2a** (R5 citation contract fix).

Your review covers the cumulative commit range
**`d4122c0^..5a0ab3d`** (4 commits = R5 #1 additive `display_citation` on
`ResolveArticleTool` + R5 #2 `SkillGuardrailDispatcher.handleMustCiteSource`
literal-substring → structural URL/article_id shape rewrite + R5 #3
`resolve_faq_grounded_answer.yaml` citation-wording consistency + dev
handoff). The `^..` notation is used so all 4 commits are included
(per the S-Auto-25 Codex non-blocking observation #1 lesson — the
`d4122c0..5a0ab3d` literal notation would exclude `d4122c0`).

This sub-sprint is **semantic-touching** on TWO surfaces: (a) the
LLM-facing citation guardrail at
`SkillGuardrailDispatcher.handleMustCiteSource:340-384` (R5 #2 rewrites
the semantics); (b) the LLM-facing skill citation-token wording at
`resolve_faq_grounded_answer.yaml:30 procedure / :31 grounding_instruction
/ ~:80 cite phrasing / :44 cite_token_field` (R5 #3 updates wording).
Per `iteration_governance.md` §7 + `process/milestone-framework.md`
§4.3, per-sub-sprint Codex review is **REQUIRED**.

## Read order (minimized)

1. `AGENTS.md` (auto-loaded via this prompt).
2. **This prompt** (self-contained per `prompt-artifact-rules.md` §9.1).
3. `docs/sprints/sprint-081-handoff.md` (dev handoff; produced AFTER
   this prompt was drafted — read it to ground the per-change verdict
   against shipped code; specifically read §1 pre-fix audit (a)/(b)/(c)
   outcomes + §3 fence confirmations + the two fence-expansion calls).
4. Code anchors only as needed (cited inline in §"Cumulative scope
   claim" below).

## Cumulative scope claim (what the cumulative range delivers)

### Commit 1 — `d4122c0` — R5 #1 `ResolveArticleTool` additive `display_citation`

Anchor: `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java:107-111`.

```java
// Approximate shape — verify against shipped diff:
String src = article.getSourceUrl();
String displayCitation = (src != null && !src.isBlank())
    ? src
    : article.getArticleId();
result.put("display_citation", displayCitation);
```

Purely additive. Existing fields (`article_id`, `source_url`, `title`,
etc.) preserved. Zero content scan; zero per-UC matrix.

### Commit 2 — `7b0665e` — R5 #2 `SkillGuardrailDispatcher.handleMustCiteSource` structural-shape rewrite

Anchor: `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java:340-384` (and the two new `Pattern` constants near the top of the class).

**Old behaviour (replaced):**
```java
String citeToken = "source_id";
Object citeField = g.parameters().get("cite_token_field");
if (citeField instanceof String s && !s.isBlank()) {
    citeToken = s.trim();
}
// ...
if (userMessage != null && userMessage.contains(citeToken)) {
    return Optional.empty();  // PASS if reply contains the LITERAL field-name string
}
```
This is a literal substring check on the field-NAME string (default
`"source_id"`), not a structural validation of any cited value.

**New behaviour (shipped):**
- Two private static final `Pattern` constants at the top of the class:
  - URL-shape: `https?://\S+`
  - article_id-shape: `\bka[A-Za-z0-9]{16}\b` (derived from the 218-row
    corpus per dev pre-fix audit (c); matches every Salesforce-style
    `ka...` article_id in `data/knowledge/knowledge_base_articles.json`;
    rejects plain English by the 16-char ka-prefixed structural
    requirement)
- New private helper `containsAcceptableCiteToken(userMessage)`:
  - PASS iff `USER_FACING_URL_PATTERN.matcher(userMessage).find()` OR
    `ARTICLE_ID_PATTERN.matcher(userMessage).find()`
  - REJECT otherwise (including empty / null / plain English)
- `handleMustCiteSource` body keeps the pre-outcome-class checks
  (`outcomeClass` null; normalized to `resolve` / `resolved`; yaml
  `outcome_class` agreement) unchanged. The `citeToken` extraction
  block + literal-substring check is fully removed.
- Trace key renamed `cite_token_field` → `cite_token_validator` with
  constant value `"structural_url_or_article_id"` (documents the new
  semantics in traces).
- Rejection message rewritten to point at the new `display_citation`
  field + the URL-shape OR article_id-shape acceptance.

**Semantic claim**: the rewrite **fixes a false-negative** in the
current literal-substring check. The OLD code REJECTED any reply that
cited a valid URL but did not also include the literal string
`"source_id"`. The NEW code PASSES such a reply (the URL alone
satisfies the structural shape). Empty / null / plain-English replies
STILL REJECT — the grounding floor (require a cite token) is
**preserved**.

### Commit 3 — `1624a7a` — R5 #3 skill yaml citation-wording consistency

Anchor: `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml`.

Minimum-edit diff (3 insertions / 4 deletions per dev report):
- `:30 procedure` — citation phrasing updated from "...source_id
  citation..." to point at the new `display_citation` token.
- `:31 grounding_instruction` — updated similarly.
- `~:80 critical-step cite phrasing` — updated to track.
- `:44 cite_token_field: source_id` — **REMOVED CLEANLY** (pre-fix
  audit (b) confirmed no orphan consumer in `server/src/main`; the
  guardrail no longer reads the param).

Goal / role / objective / search-must-precede-resolve / faq_miss
escalation rule paragraphs byte-untouched (verify via
`git show 1624a7a --stat` / `git diff 1624a7a^ 1624a7a -- '*.yaml'`).

### Two fence expansions (dev report + handoff §3)

**Expansion #1 — `ResolveFaqGuardrailsTest.java` — deliver-agent
PRE-APPROVED.** The contract's §Scope #2 STOP-condition (c) explicitly
named the choice ("update the test inputs to use a real article_id
(preferred) or widen the regex"); the dev chose the preferred path so
the narrow `ka[A-Za-z0-9]{16}` regex did not need widening. The two
`kb-001` user-message fixtures were updated to a real corpus article_id.
This was within the contract's fence (the contract listed test files
that "extend or new", which covers fixture updates).

**Expansion #2 — `PhaseEvaluatorResolveSkillIntegrationTest.java` —
NOT in the original fence; flagged for Codex review per dev handoff §3.**
The file holds golden `FAQ_PROCEDURE` / `FAQ_GROUNDING` constants that
mirror the yaml procedure / grounding text **verbatim**. The R5 #3
yaml wording change forced a mechanical (judgment-free) update to keep
the suite green. The dev surfaced it at build time (the #3 pre-fix
audit grepped main-source consumers, not test golden assertions — a
process gap, not a scope drift).

**Deliver-agent scope call on expansion #2 (post-ship 2026-06-06)**:
ACCEPTED as a non-blocking observation. Rationale:
- Identical category to expansion #1 (golden test mirrors the yaml;
  yaml changes in scope; test must update to track).
- Mechanically forced; no semantic decision made by the dev (the new
  constant values are byte-for-byte copies of the new yaml strings).
- No alternative without leaving the suite RED.
- The audit gap (grep main-source consumers, not test golden
  constants) is a process learning for future contract pre-fix audits;
  surface as a process recommendation, NOT a sub-sprint blocker.

Codex should still verify the expansion #2 diff is byte-mechanical
(constants updated to mirror the new yaml; no rubric / assertion shape
/ test scenario change).

### Commit 4 — `5a0ab3d` — dev handoff

`docs/sprints/sprint-081-handoff.md` (337 lines). §0 cold-start verdict;
§1 implementation evidence per R5 #1/#2/#3 with file:line + tests; §2
test/eval results (Java `1337 / 1 / 0 / 2`; +10 net R5 tests; sole
failure = inherited `SystemPromptUserRequestedTiebreakerTest` OQ-S41.5);
§3 STOP/fence confirmations including the two expansions; §4 commit
map.

## Embedded milestone context (for cross-reference)

This sub-sprint sits inside M-Auto-6 (Runtime substrate hygiene +
admin observability + intake/clarification contract + UX/corpus
governance). Predecessors already dev-side closed 2026-06-06:

- **Sub-sprint A (S-Auto-23 / Sprint 078; R1.a + R2.a + R4.a)** —
  `prompt_projection` + `infra` + `skill_state`. Shipped at
  `a873d18..af44903`; Codex `APPROVE_S_AUTO_23 / 0`.
- **Sub-sprint B (S-Auto-24 / Sprint 079; R3.a + R3.b + R3.c admin
  trace observability)** — `infra` UI-only; §7-EXEMPT. Shipped at
  `a26ec88..505aca3`; visual-verified.
- **Sub-sprint C-1 (S-Auto-25 / Sprint 080; R7 + R2.a#5-ext)** —
  `skill_state` + `infra` + `prompt_projection` (R7 new tool schema).
  Shipped at `be1e733..c031786`; Codex `APPROVE_S_AUTO_25 / 0`;
  capability-wiring Option-A fence-waiver ACCEPTED.

C-2a (this sub-sprint) and C-2b (S-Auto-27 / Sprint 082; R6 corpus
eligibility filter reusing `search_knowledge_eligible`; planning
context at `compact/sprint-082-dev-prompt.md`; launches sequentially
after C-2a Codex close) close out M-Auto-6 before the milestone-shared
§9 real-LLM re-bless.

Milestone class: runtime substrate-hygiene + UI/observability + intake
contract + UX/corpus governance + citation contract literal→shape
semantics fix. NOT a semantic milestone — no bot prompt procedure
rewrite, no UC routing change, no escalation posture decision, no
judge calibration, no CaseSpec rubric edit. C-2a's `handleMustCiteSource`
rewrite is a **grounding-floor false-negative correction** (the OLD
literal-substring check rejected URL-only replies; the NEW structural
predicate accepts them) AND C-2a's skill yaml wording change is a
minimum-edit citation-token reference (goal / role / objective /
search-must-precede / faq_miss byte-untouched).

## Embedded §7 stanza (from the C-2a sprint_objective.md)

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
  positive (`[source_id: kb-001]` updated to a real article_id)
  continues to PASS. R5 #3 wording change does not affect non-FAQ
  paths (skill scope unchanged).
- shadow: not applicable (mocked-LLM tests are wiring evidence; real
  evidence is milestone-shared re-bless after C-2b lands).
```

## Embedded file-path fence (from the C-2a contract)

**Files allowed to edit** (fence + the two expansions):
- `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java`
  (ONLY `handleMustCiteSource:340-384` + new private helper + two
  `Pattern` constants + trace-key rename + rejection message rewrite)
- `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml`
  (ONLY citation-token wording + `cite_token_field` removal)
- `server/src/test/java/com/gumtree/csagent/service/tools/ResolveArticleToolTest.java`
- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcherTest.java`
- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/ResolveFaqGuardrailsTest.java`
  (fence expansion #1 — pre-approved per pre-fix audit (c) STOP
  condition)
- `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorResolveSkillIntegrationTest.java`
  (fence expansion #2 — flagged for Codex scope review; deliver-agent
  ACCEPTED 2026-06-06 as non-blocking; mechanically forced byte-for-byte
  yaml mirror; the audit gap that allowed it to slip past pre-fix
  audit (b) is a process learning)
- `docs/sprints/sprint-081-handoff.md`

**Files FORBIDDEN to edit** (any touch = REJECT_S_AUTO_26):
- Any OTHER guardrail handler in `SkillGuardrailDispatcher.java`
  (handlers OTHER than `handleMustCiteSource`).
- Any OTHER yaml file (skills or otherwise).
- `KbArticle.java` / `KbArticleRepository.java`.
- `data/knowledge/knowledge_base_articles.json` (C-2b's surface).
- `KnowledgeIngestionRunner.java` / `KnowledgeSearchService.java` /
  `KnowledgeHit.java` / `SearchKnowledgeTool.java` (C-2b's surface).
- `IntakeFieldsRegistry.java` / `IntakeFieldsMerger.java` /
  `BudgetChecker.java` / `ControlKernel.java` / `AgentRunLoopImpl.java` /
  `ContextProjectionBuilder.java` / `UpdateIntakeFieldsTool.java`
  (A's / C-1's surfaces).
- Any UI file (B's surface).
- Any `eval_interactive/` file.
- Autoloop 5-file SHA-locked scoring set.
- `autoloop/config.yaml` `baseline_dir`.
- `docs/current_eval_baseline.md`.

## Embedded §4.1 nine-question anti-hardcode kernel (from anti-hardcode-review-kernel.md)

Walk each question against the cumulative range. Cite specific file:line
or commit hashes in your verdicts.

### Q1 — Semantic decision hardcode?

Did this PR encode a semantic decision (which UC, which next action,
how to phrase the reply) as a fixed rule (keyword / regex / if-else /
enum widening / per-UC matrix) instead of leaving it to the LLM?

For C-2a, examine:
- R5 #1: `display_citation` is structurally derived
  (`source_url` non-null/non-blank → URL else `article_id`); zero
  semantic decision; not a per-UC matrix.
- R5 #2: accept criteria is structural shape (URL regex +
  article_id regex). The URL regex `https?://\S+` is a transport-shape
  test, not a semantic test. The article_id regex `\bka[A-Za-z0-9]{16}\b`
  is a structural ID test derived from real corpus values, not a
  content scan, not a per-UC matrix.
- R5 #3: yaml wording points at the tool result field
  (`display_citation`); not a hardcoded URL list, not a per-UC
  enumeration.

Pass / fail with anchors.

### Q2 — Tier-0 invariant justification?

If a new Java guard / new Tier-0 invariant was added, is it justified
by `iteration_governance.md` §1.4 (Runtime ownership) + a current
Tier-0 in `runtime_freeze_and_risk_policy.md` §1 / §2?

For C-2a: no new Tier-0 invariant claimed. The `handleMustCiteSource`
rewrite preserves the existing grounding-floor contract (require a
cite token) and widens the accept criteria — this is a false-negative
correction to an existing guardrail, not a new Tier-0. Verify the
rewrite does not introduce a new Tier-0 by sneaking semantic logic
into the structural predicate (it should not — the predicate is
purely transport + ID-shape).

Pass / fail with anchors.

### Q3 — Could soft-signal projection suffice?

Could the change have been expressed as a soft signal in the prompt
projection (state, candidate list, diagnostic) instead of moving
ownership of a semantic decision into Java?

For C-2a: R5 #3 IS a prompt_projection wording change; R5 #1 adds a
soft-signal field (`display_citation`) on the tool result that the
LLM consumes. R5 #2's guardrail rewrite is a Runtime grounding-floor
check that cannot be projected (a soft signal cannot enforce a
grounding floor); the rewrite preserves the existing Runtime check
and only fixes its false-negative.

Pass / fail with anchors.

### Q4 — Eval/case text encoded?

Does any code or prompt or guardrail now match an exact CaseSpec
phrase, eval rubric phrase, or user-message keyword?

For C-2a: check the article_id regex pattern — confirm it does NOT
match an exact CaseSpec phrase or user-message keyword (it's a
structural pattern). The URL regex `https?://\S+` is a transport
pattern; the article_id regex `\bka[A-Za-z0-9]{16}\b` is a structural
ID pattern derived from real corpus data. Skill yaml wording change
at `:30 / :31 / ~:80` should not introduce CaseSpec-phrase mirroring
— verify against the dev handoff §1's before/after wording.

Pass / fail with anchors.

### Q5 — Semantic ownership moved LLM → Java?

Did a decision that was previously the LLM's (use case hypothesis,
next action, escalation posture, follow-up policy, natural-language
response wording) move into Java code or into a frozen prompt rule?

For C-2a: the LLM still decides WHEN to cite an article (skill
procedure tells it to call `resolve_article` then cite, but the LLM
decides what to write). The Runtime now provides a structurally-
derived `display_citation` token + accepts EITHER URL-shape OR
article_id-shape citation in the reply. No LLM-owned semantic
decision moved into Java.

Pass / fail with anchors.

### Q6 — Prompt if-else instead of principles?

Was a new prompt rule or skill yaml change an if-else / rule dump
instead of a principle?

For C-2a: R5 #3 wording change updates the citation-token reference
from `source_id` → `display_citation`. Verify the new wording is
declarative ("cite the `display_citation` token returned by
resolve_article") and NOT an if-else ("if `source_url` is non-null,
cite the URL; else cite the article_id" — that would be moving the
LLM's choice into a deterministic prompt rule). The dev handoff §1
should record the exact before/after wording.

Pass / fail with anchors.

### Q7 — Tool schema / safety / grounding floors preserved?

Are safety floor (Tier-0 invariants per §1.4) and grounding floor
(per `faq_grounding_contract.md`) intact?

For C-2a: the grounding floor is the central concern. The OLD
`handleMustCiteSource` literal-substring check enforced "the reply
contains the literal field-name string `source_id`" — a flawed
proxy for "the reply cites a real article". The NEW structural-shape
predicate enforces "the reply contains a URL-shape OR a Salesforce
article_id-shape token" — a more faithful proxy. Empty / null /
plain-English STILL reject. Verify against
`SkillGuardrailDispatcherTest` (extended) +
`ResolveFaqGuardrailsTest` (fixture-updated).

Pass / fail with anchors.

### Q8 — Generalization eval coverage?

Were target / neighbor / negative / shadow cases identified and
covered?

For C-2a: per the embedded §7 stanza, coverage is 1 / ~6 / ~10 / 0
(shadow N/A — mocked tests are wiring evidence; outcome at
milestone re-bless). Verify the negative coverage in
`SkillGuardrailDispatcherTest`:
- empty / null / plain English REJECT
- `source_id` literal substring WITHOUT a URL or real article_id
  REJECT (confirms the literal→shape semantics shift; the OLD code
  would have PASSED this)
- outcome_class != resolve bows out
- yaml `outcome_class` disagreement bows out
- URL-shape PASS / article_id-shape PASS / back-compat (real
  article_id replacing `kb-001`) PASS

Pass / fail with anchors.

### Q9 — Temporary hardcode / sunset plan?

If any temporary hardcode shipped (downgrade-to-signal pattern), is
the sunset trigger + target sprint named?

For C-2a: no temporary hardcode shipped. The article_id regex is a
durable structural pattern derived from corpus data; the URL regex
is a durable transport pattern; the yaml wording change is a
durable contract change; the `cite_token_field` removal is
permanent.

Pass / fail.

## Five focal points for this sub-sprint (semantic-touching surfaces)

In addition to the §4.1 walk above, write a verdict on each of these
five focal points with anchored evidence.

### F1 — Literal → structural-shape semantics shift (R5 #2)

The OLD `handleMustCiteSource` at `SkillGuardrailDispatcher.java:357-368`
did `userMessage.contains(citeToken)` where `citeToken` was the literal
value of the yaml `cite_token_field` param (default `"source_id"`).
The NEW code uses two structural `Pattern` constants + the
`containsAcceptableCiteToken` helper.

Verify:
- The OLD `citeToken` extraction block is fully removed (no dead code
  / no orphan param reference).
- The NEW patterns are private static final.
- The URL pattern is `https?://\S+` (or equivalent).
- The article_id pattern is `\bka[A-Za-z0-9]{16}\b` (or equivalent
  Salesforce-style structural pattern).
- The helper combines the two with `||`.
- Empty / null / plain-English reject (the existing reject path at
  `:369-383` is reached).
- The trace key rename `cite_token_field` → `cite_token_validator`
  with constant value `"structural_url_or_article_id"` is consistent
  with the new semantics.

### F2 — Grounding-floor preservation (no false-pass widening)

The widening from literal-substring to structural-shape **must not**
introduce a false positive. Specifically:
- Plain English replies (no URL, no article_id-shape) REJECT.
- Replies that mention `"source_id"` as plain text WITHOUT a URL or
  real article_id REJECT.
- Replies with a URL alone PASS (this was the OLD code's false
  negative — the rewrite explicitly fixes it).
- Replies with a real article_id PASS.

Verify against `SkillGuardrailDispatcherTest` extensions. Cite the
test names.

### F3 — Article_id regex derived from real corpus data

The contract REQUIRED the article_id regex to be derived from real
data, NOT invented. The dev shipped `\bka[A-Za-z0-9]{16}\b`.

Verify:
- Sample ~10 entries from `data/knowledge/knowledge_base_articles.json`
  and confirm every `article_id` matches `\bka[A-Za-z0-9]{16}\b`
  (the dominant Salesforce-style `ka...` prefix; 16 trailing
  alphanumeric chars).
- Confirm the regex is strict enough to reject plain English (it
  requires the `ka` literal prefix + 16 trailing alphanumerics — a
  shape unlikely to appear in natural-language replies).
- Confirm the dev pre-fix audit (c) outcome in handoff §1 cites the
  evidence (sample of real article_id values + plain-English negative
  test).
- Confirm the back-compat test in `ResolveFaqGuardrailsTest`
  (fence expansion #1) was updated to use a real corpus article_id
  instead of `kb-001` — the contract's pre-fix audit (c) STOP-condition
  preferred this path over widening the regex.

### F4 — Skill yaml minimum-edit + `cite_token_field` clean removal

The R5 #3 wording change must be minimum-edit:
- Goal / role / objective / search-must-precede-resolve / faq_miss
  escalation rule paragraphs byte-untouched.
- Citation-token references at `:30 procedure / :31 grounding_instruction
  / ~:80 cite phrasing` updated to point at `display_citation`.
- `cite_token_field:44` cleanly removed.

Verify via `git diff 1624a7a^ 1624a7a -- 'server/src/main/resources/skills/resolve_faq_grounded_answer.yaml'`
that:
- The diff is small (dev reports 3 ins / 4 del).
- No non-citation line touched.
- The `cite_token_field` removal does not break a non-guardrail
  consumer (dev pre-fix audit (b) grepped main-source consumers and
  confirmed none).

### F5 — Fence-expansion #2 scope call (`PhaseEvaluatorResolveSkillIntegrationTest`)

This file was NOT in the original C-2a fence; the dev surfaced it at
build time after the R5 #3 wording change forced a golden constant
update. Deliver-agent ACCEPTED the expansion as non-blocking
2026-06-06 per the "identical category / mechanically forced /
build-time discovery / process learning" rationale documented above.

Verify:
- The diff on `PhaseEvaluatorResolveSkillIntegrationTest.java` is
  byte-mechanical (constants updated to mirror the new yaml strings;
  no rubric / assertion shape / test scenario change).
- No new test logic, no new test scenario, no new assertion shape.
- The change is exactly what a yaml→test-constant byte-mirror would
  produce.

If the diff is byte-mechanical: accept as non-blocking observation.
If the diff includes new test logic or a scenario change: escalate
as a scope drift and downgrade to a blocking finding.

## Output format — write the verdict to `docs/codex-findings.md`

Replace the existing top-of-file verdict header with this format
(per `iteration_governance.md` §4.2):

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <int>
final_verdict: APPROVE_S_AUTO_26 | APPROVE_S_AUTO_26_WITH_FIXES | REJECT_S_AUTO_26 | NEEDS_HUMAN_ARCHITECTURE_DECISION
summary: <one paragraph>
```

Then write:

- **§1 Per-Change Verdicts** — one verdict per scope item (#1 / #2 /
  #3 / two fence expansions / golden-set churn if any).
- **§2 Nine-Question Kernel Walkthrough** — Q1–Q9 with anchored
  verdicts; final §4.1 aggregate verdict at the end.
- **§3 Five Focal-Point Verdicts** — F1 / F2 / F3 / F4 / F5 with
  anchored evidence.
- **§4 Blocking Findings** — None if all gates pass; otherwise
  enumerate.
- **§5 Non-Blocking Observations** — any items that surface but do
  not block (e.g. fence-expansion #2 audit-gap process recommendation;
  any other minor items).

## Constraints

1. **Do NOT edit any code.** This is a review-only pass. The only
   file you write is `docs/codex-findings.md`.
2. **Do NOT re-judge bad-case suite traces.** The bad-case suite
   primary gate (per `process/badcase-lifecycle.md` §5.6) is a
   human-judgment gate; you do not adjudicate it.
3. **Do NOT widen scope.** If you find scope drift, write it as a
   blocking finding; do NOT attempt to bundle it into your verdict.
4. **Cite anchors** (file:line / commit hash) for every claim. Do
   not assert without evidence.
5. **Audit the intended cumulative range** `d4122c0^..5a0ab3d` (4
   commits). If you see additional commits in scope, raise it as a
   blocking finding (`scope_drift`).
6. **Verify the working tree is clean** at HEAD before reviewing — if
   uncommitted changes exist, raise it as a `out_of_scope_review`
   finding.

## Acceptance verdict shapes

- **APPROVE_S_AUTO_26 / blocking_count=0**: all §4.1 questions pass,
  all 5 focal points pass, fence expansion #2 is byte-mechanical,
  grounding floor preserved, no semantic hardcode. Non-blocking
  observations (if any) are recorded in §5.
- **APPROVE_S_AUTO_26_WITH_FIXES / blocking_count > 0**: targeted
  small fixes needed (e.g. an additional test for an edge case the
  current coverage misses; a wording tweak for clarity in the
  rejection message). Enumerate each fix with anchored evidence
  + a one-line fix recommendation.
- **REJECT_S_AUTO_26 / blocking_count > 0**: a blocking finding
  (semantic hardcode introduced; grounding floor weakened; fence
  drift with semantic content; scope drift). Each finding has
  anchored evidence + a layer classification (per §3.2) + a
  one-line corrective direction.
- **NEEDS_HUMAN_ARCHITECTURE_DECISION**: review surfaced a
  decision the deliver-agent should make (e.g. the article_id regex
  needs to be wider than `\bka[A-Za-z0-9]{16}\b` to match a class
  of corpus values the dev's sample missed; the `cite_token_validator`
  trace key shape conflicts with a downstream consumer not surfaced
  by the audit). Surface as a human decision, not a unilateral
  Codex reject.

When done: ensure `docs/codex-findings.md` carries your verdict
header + body; commit nothing else; hand back to deliver-agent for
close + Sub-sprint C-2b launch.
