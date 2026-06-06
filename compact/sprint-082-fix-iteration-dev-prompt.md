# Sprint 082 / S-Auto-27 / M-Auto-6 Sub-sprint C-2b — FIX-ITERATION dev prompt (two targeted P0 fixes per Codex `APPROVE_S_AUTO_27_WITH_FIXES / blocking_count=2`)

> **Active fix-iteration dev prompt** — targeted P0 fixes only. The
> Codex per-sub-sprint review at the original C-2b dispatch
> (`compact/sprint-082-codex-review-prompt.md` at commit `3300b4a`)
> returned `decision: fix_required / blocking_count: 2 /
> final_verdict: APPROVE_S_AUTO_27_WITH_FIXES`. The substantive
> verdict was strong: §1 #1/#2/#4/#5/#6/#7 PASS; §2 Q1-Q7+Q9 PASS;
> §3 F1+F4+F5+F6 PASS; focused tests 34/0/0/0. Only two narrow
> blockers — both Codex-tagged as "infra hygiene gap, not executable
> semantic hardcode". This prompt closes them.

## Role identity

You are the **dev agent** for the C-2b fix-iteration. Scope is
EXACTLY the two Codex blockers — nothing else. No new test scenarios,
no new code paths, no scope drift.

## Read order (minimized)

1. `AGENTS.md` (auto-loaded).
2. **This prompt** (full fix scope; do NOT re-read
   `docs/sprint_objective.md` for scope when this prompt is the
   active fix-iteration contract).
3. **`docs/codex-findings.md` §4 Blocking Findings** (the
   authoritative source of the two fixes; Codex's `APPROVE_S_AUTO_27_WITH_FIXES`
   verdict is at the top of the file).
4. The two source files cited inline below.

## The two fixes

### Fix #1 — `V17` SQL comment forbidden-grep hygiene

**Anchor:** `server/src/main/resources/db/migration/V17__add_kb_search_knowledge_eligible.sql:4`.

Current content (committed at `bb48aa0`):
```sql
-- R6 (Sub-sprint C-2b) — corpus-curation flag for the LLM-facing
-- search_knowledge surface. NOT NULL DEFAULT TRUE backfills every
-- existing row to visible; the JSON re-ingestion flips the two
-- (temp) CS-only template articles to FALSE. Direct resolve_article
-- by id is unaffected (no filter at the resolve surface).
ALTER TABLE kb_articles
    ADD COLUMN search_knowledge_eligible BOOLEAN NOT NULL DEFAULT TRUE;
```

The literal substring `(temp)` on line 4 trips the contract's F3
forbidden-grep gate. Codex §4 finding #2 corrective direction:
"replace that comment phrase with content-neutral wording; do not
change the SQL operation".

**Change:** rewrite line 4 (and re-flow the surrounding comment if
needed) to remove the `(temp)` literal. Suggested wording (you may
choose any equivalent that does not contain `(temp)`):

> ... the JSON re-ingestion flips the two CS-only placeholder
> template articles to FALSE. ...

OR

> ... the JSON re-ingestion flips the two CS-only template articles
> (operationally human-CS-only, with placeholder text) to FALSE. ...

OR any other phrasing that conveys the same meaning without the
`(temp)` substring. Do NOT change:
- The SQL operation (`ALTER TABLE kb_articles ADD COLUMN ...`).
- The semantics described in the comment.
- Any other line of the file.

**Verification grep after the fix:**
```
grep -rn "(temp)" server/src/main 2>&1
```
Expected: zero matches introduced by C-2b. (Pre-existing matches
elsewhere in `server/src/main` from prior sprints, if any, are
acceptable — the contract's gate was specifically "no newly
introduced matches in C-2b's commit range".)

### Fix #2 — Direct-resolve invariant test gap

**Anchor:** `server/src/test/java/com/gumtree/csagent/service/tools/ResolveArticleToolTest.java:239-272`
(method `execute_searchIneligibleArticle_stillResolvesByDirectId`).

Current state: the test pins direct-resolve for `ka41r000000LIEJAA4`
only. Codex §4 finding #1: the second flagged ID
`ka41r000000LIEEAA4` is required by the contract's F2 invariant +
the handoff's claim "both articles test-pinned" but is absent from
the test file.

**Change:** add a second direct-resolve invariant covering
`ka41r000000LIEEAA4`. Two acceptable shapes (your choice; pick the
cleaner one):

**Option A — Add a second mirror test (simpler, no refactor):**

Append a second `@Test` method after the existing
`execute_searchIneligibleArticle_stillResolvesByDirectId`, mirroring
the structure but with `ka41r000000LIEEAA4` + its actual JSON title
"(temp) NTD Ad Removed Information" + an appropriate description
stub. Example shape:

```java
@Test
void execute_searchIneligibleArticle_stillResolvesByDirectId_secondTemplate() {
    // Same anti-误杀 #1 invariant as above, pinned for the second
    // flagged article so both eligibility=false rows in the corpus
    // are covered by the direct-resolve characterization.
    KbArticle csTemplate = KbArticle.builder()
            .articleId("ka41r000000LIEEAA4")
            .title("(temp) NTD Ad Removed Information")
            .description("The 'Rights' owner of the item you're advertising ...")
            .sourceUrl(null)
            .ucTags(new String[]{"UC-B"})
            .isPublished(true)
            .searchKnowledgeEligible(false)
            .build();
    when(kbArticleRepository.findById("ka41r000000LIEEAA4"))
            .thenReturn(Optional.of(csTemplate));

    ToolResult result = tool.execute(buildSession(),
            Map.of("source_id", "ka41r000000LIEEAA4"));

    assertTrue(result.isSuccess(),
            "a search_knowledge_eligible=false (but published) article must still "
                    + "resolve via resolve_article — the R6 filter is at the search surface only");
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) result.getData();
    assertEquals("ka41r000000LIEEAA4", data.get("source_id"));
    assertEquals("(temp) NTD Ad Removed Information", data.get("title"));
    assertEquals(Boolean.TRUE, data.get("safe_to_show"),
            "published + non-blank body → safe_to_show true; eligibility does not gate direct resolve");
}
```

**Option B — Parameterize over both IDs (more elegant, light
refactor):**

Convert the existing `@Test` to a JUnit 5 `@ParameterizedTest` with
`@CsvSource` (or similar) listing both `(articleId, title)` pairs;
the test body uses the parameters to build the `KbArticle` + assert.
Either shape is acceptable per Codex; Option A is the lower-risk
choice and matches the existing test-naming convention in the file.

**Hard fence:**
- ONLY this test file is touched in fix #2.
- No production code change.
- No change to the existing first test (preserves the
  `ka41r000000LIEJAA4` characterization byte-for-byte).
- No new test scenarios beyond the second flagged ID's direct
  resolve. NO test for the search-surface filter (that's
  `KnowledgeSearchServiceTest`'s scope). NO test for the
  unpublished case (out of R6 scope).

**Note on the test title comment**: the test method's existing
comment block uses `(temp)` to describe the article type. The
comment text is in test code (not in `server/src/main`), so it
does NOT trigger the F3 forbidden-grep gate (which scoped to
`server/src/main`). Codex confirmed this scoping in §4 finding #2:
"V17 introduces `(temp)` in a `server/src/main` comment". You may
keep or rewrite the test comments; the grep gate is about main
source only.

## File fence (fix-iteration ONLY)

**Files allowed to edit**:
- `server/src/main/resources/db/migration/V17__add_kb_search_knowledge_eligible.sql`
  (fix #1 comment rewrite only; SQL operation unchanged).
- `server/src/test/java/com/gumtree/csagent/service/tools/ResolveArticleToolTest.java`
  (fix #2 second direct-resolve test; the existing first test
  byte-untouched).
- `docs/sprints/sprint-082-handoff.md` — append a §3.x or §6
  addendum noting the fix-iteration shape + the two corrected
  evidence rows.

**Files FORBIDDEN to edit** (any touch = scope drift; STOP):
- Any other `server/src/main` or `server/src/test` file.
- Any other migration SQL.
- Any other yaml / json / corpus / scoring / autoloop /
  eval_interactive file.
- Any docs file other than the handoff addendum.

## Verification + handoff requirements

After the two fixes:

1. **Run focused tests** to confirm the new test passes + the
   existing tests still pass + the V17 migration still applies:
   ```
   cd server && mvn -o -Dtest=ResolveArticleToolTest,KnowledgeSearchServiceTest,KnowledgeIngestionRunnerTest,KbArticleEligibilityCorpusTest test
   ```
   Expected: all tests green; total count = previous + 1 (or
   parameterization yields +1 method count).
2. **Run full Java test** to confirm baseline:
   ```
   mvn -o clean test
   ```
   Expected: `1348 / 1 / 0 / 2` (previous `1347/1/0/2` + 1 new
   test; sole failure = inherited `SystemPromptUserRequestedTiebreakerTest`).
3. **Re-verify the forbidden-grep gate**:
   ```
   grep -rn "(temp)" server/src/main 2>&1
   grep -rn "ka41r000000LIEJAA4\|ka41r000000LIEEAA4" server/src/main 2>&1
   ```
   Expected: zero hits on both.
4. **Handoff §3 addendum** in `docs/sprints/sprint-082-handoff.md`
   — append a "Fix-iteration §3.x" section:
   - V17 SQL comment rewritten (before/after wording).
   - Second direct-resolve test added (method name; ID covered).
   - Java baseline post-fix (`1348/1/0/2` expected).
   - Re-verified grep evidence (zero `(temp)` + zero hardcoded
     article-id in `server/src/main`).
   - Correct the handoff's prior over-claim about "both
     article-pinned" (line 68-69 + line 227-228 per Codex finding
     anchors) if needed.

## Commit discipline

Recommended commit split:

1. **Commit 1 — V17 SQL comment rewrite (fix #1)**:
   `V17__add_kb_search_knowledge_eligible.sql` comment-only edit.
2. **Commit 2 — Second direct-resolve invariant test (fix #2)**:
   `ResolveArticleToolTest.java` test addition.
3. **Commit 3 — Handoff addendum**: append the fix-iteration §3.x
   to `docs/sprints/sprint-082-handoff.md`.

Each commit message references the Codex finding it closes (e.g.
"Sprint 082 / S-Auto-27 fix-iteration — V17 SQL comment
content-neutral rewrite (closes Codex §4 finding #2)" and "Sprint
082 / S-Auto-27 fix-iteration — second direct-resolve invariant
test for ka41r000000LIEEAA4 (closes Codex §4 finding #1)").

## Self-check before claiming done

- [ ] Fix #1: V17 SQL comment no longer contains `(temp)` literal;
      SQL operation unchanged; only the comment text edited.
- [ ] Fix #2: second direct-resolve test added (or first test
      parameterized); covers `ka41r000000LIEEAA4` end-to-end (build
      KbArticle stub + repository mock + tool.execute + assert
      success + assert title + assert safe_to_show).
- [ ] `mvn -o clean test` = `1348 / 1 / 0 / 2`; +1 net test; sole
      failure = inherited OQ-S41.5.
- [ ] `grep -rn "(temp)" server/src/main` = 0 hits.
- [ ] `grep -rn "ka41r000000LIEJAA4\|ka41r000000LIEEAA4" server/src/main`
      = 0 hits.
- [ ] No file outside the fix-iteration fence touched.
- [ ] Handoff §3 addendum written.
- [ ] No scope drift (no new test scenarios beyond the second
      flagged ID's direct resolve; no production code change; no
      other migration / yaml / json file touched).

When all checked: hand back to deliver-agent for targeted re-review
+ close cascade + M-Auto-6 milestone-shared §9 real-LLM re-bless
launch.
