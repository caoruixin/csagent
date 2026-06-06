# Sprint 082 / S-Auto-27 / M-Auto-6 Sub-sprint C-2b — Codex per-sub-sprint **targeted re-review** prompt (fix-iteration verification; verdict flip from APPROVE_S_AUTO_27_WITH_FIXES to APPROVE_S_AUTO_27)

## Role identity

You are the **Anti-Hardcode + Per-Sub-Sprint Review Agent** for **Sprint
082 / S-Auto-27 / M-Auto-6 Sub-sprint C-2b** (R6 corpus eligibility
filter), performing a **targeted re-review** after the prior pass
returned `APPROVE_S_AUTO_27_WITH_FIXES / blocking_count=2 /
decision=fix_required`.

The **substantive verdict is preserved verbatim** in
`docs/codex-findings.md` from the initial review at HEAD `3300b4a`
(your prior pass; committed at `1954cb6` for audit trail). Your §1
verdicts on R6 #1 / #2 / #4 / #5 / #6 / #7 all passed; §2 Q1 / Q2 /
Q3 / Q4 / Q5 / Q6 / Q7 / Q9 all passed; §3 F1 / F4 / F5 / F6 all
passed; §5 non-blocking observations remain valid; focused tests
returned 34/0/0/0. The two §4 blocking findings were both
Codex-tagged "infra hygiene gap, not executable semantic hardcode":

> **§4 #1 — Incomplete direct-resolve invariant coverage** —
> `ResolveArticleToolTest.java:239-272` pinned only
> `ka41r000000LIEJAA4`; the second required flagged ID
> `ka41r000000LIEEAA4` was absent.
>
> **§4 #2 — Forbidden-grep gate failure** —
> `V17__add_kb_search_knowledge_eligible.sql:4` introduced `(temp)`
> in a `server/src/main` comment, while F3 required zero newly
> introduced matches.

The deliver-agent dispatched a fix-iteration prompt
(`compact/sprint-082-fix-iteration-dev-prompt.md` at commit
`1954cb6`) scoped EXACTLY to those two fixes. The dev shipped 3
commits resolving both blockers:

- **`e6aad78`** — V17 SQL comment content-neutral rewrite (closes
  §4 #2). Line 4 wording `"(temp) CS-only template articles"` →
  `"CS-only placeholder template articles"`. SQL operation
  byte-unchanged.
- **`d27b824`** — second direct-resolve invariant test for
  `ka41r000000LIEEAA4` (closes §4 #1). New `@Test` method
  `execute_searchIneligibleArticle_stillResolvesByDirectId_secondTemplate`
  mirrors the first test structure (Option A per the fix-iteration
  prompt). Existing first test byte-untouched.
- **`4c8931f`** — handoff §3.1 fix-iteration addendum + corrected
  prior over-claims at handoff §3 R6 #8 and §6 fence.

Your task: verify the clean-tree gate, confirm the two fixes
resolve the prior blocking findings exactly, update the affected
§1 / §2 / §3 / §4 entries, and flip the verdict header to
`APPROVE_S_AUTO_27 / blocking_count=0`.

## What you DO NOT need to redo

The following PASS verdicts from the initial review remain valid
(the audited code in those scopes was not touched by the
fix-iteration commits):

- **§1 #1 R6 corpus JSON flip** (pass).
- **§1 #2 KbArticle entity field** (pass).
- **§1 #4 ingestion parser** (pass).
- **§1 #5 search-service filter** (pass).
- **§1 #6 KnowledgeHit propagation SKIPPED** (pass).
- **§1 #7 INFO log path α** (pass).
- **§1 Fence / cumulative range** (pass — and now extends to
  include the 3 fix-iteration commits per the new range below).
- **§2 Q1–Q7 + Q9** (pass).
- **§3 F1 / F4 / F5 / F6** (pass).
- **§5 NBO #1 / #2 / #3** (recorded; remain valid).

## What you DO need to do

### Step 1 — Verify the clean-tree gate

Run `git status --short`. Expected: zero `M ` / `A ` / `??` lines
(except `docs/codex-findings.md` if you are mid-edit on your own
verdict update; nothing else should appear). Untracked files
that were preempted at C-2b's pre-review stage (e.g.
`compact/framework-plan-v3.2-2026-06-06.md`) were committed at
`056fa5a` per the C-2a precedent and should NOT appear in
`git status` output.

If the working tree is dirty for any other reason: raise as a NEW
`out_of_scope_review` finding (do not auto-approve).

### Step 2 — Verify the new cumulative range

Confirm the cumulative range now extends from the original
audited four commits to include the 3 fix-iteration commits:

```
git log --oneline bb48aa0^..4c8931f
```

Expected: exactly 8 commits in this range:
- `bb48aa0` (R6 #2+#3 entity + V17 migration)
- `0594a25` (R6 #4 ingestion parser)
- `d21594f` (R6 #5+#7 search-service filter + INFO log)
- `977f9b8` (R6 #1+#8 JSON flip + first direct-resolve test)
- `7773c92` (initial dev handoff)
- `e6aad78` (fix-iteration: V17 SQL comment content-neutral rewrite)
- `d27b824` (fix-iteration: second direct-resolve test)
- `4c8931f` (fix-iteration: handoff §3.1 addendum)

The original audited range `bb48aa0^..7773c92` (5 commits) was
unchanged by the fix-iteration (those 5 commits remain
unamended); the cumulative final range becomes
`bb48aa0^..4c8931f` (8 commits) by appending the 3
fix-iteration commits.

If any original commit was rebased / amended / reordered, raise
as a NEW `scope_drift` blocking finding requiring substantive
re-review.

### Step 3 — Verify fix #1 (V17 SQL comment)

Confirm `V17__add_kb_search_knowledge_eligible.sql:4` no longer
contains the literal `(temp)` substring AND the SQL operation is
byte-unchanged.

```
grep -n "(temp)" server/src/main/resources/db/migration/V17__add_kb_search_knowledge_eligible.sql
# Expected: zero matches.

git show e6aad78 -- server/src/main/resources/db/migration/V17__add_kb_search_knowledge_eligible.sql
# Expected: comment text change only on line 4 (or surrounding comment
# lines if dev re-flowed); SQL ALTER TABLE statement byte-unchanged.
```

Also re-run the F3 forbidden-grep over `server/src/main`:

```
grep -rn "(temp)" server/src/main
# Expected: zero matches introduced in the new cumulative range.

grep -rn "ka41r000000LIEJAA4\|ka41r000000LIEEAA4" server/src/main
# Expected: zero matches (already zero in initial review; should
# remain zero — fix-iteration touched zero production source files).
```

### Step 4 — Verify fix #2 (second direct-resolve invariant test)

Confirm the new test method exists, covers `ka41r000000LIEEAA4`
end-to-end, and matches the first test's structure for the
direct-resolve invariant.

```
grep -n "execute_searchIneligibleArticle_stillResolvesByDirectId_secondTemplate\|ka41r000000LIEEAA4" \
  server/src/test/java/com/gumtree/csagent/service/tools/ResolveArticleToolTest.java

# Expected: method name found; ka41r000000LIEEAA4 found within the
# new test's KbArticle build + repository mock + assertion block.

git show d27b824 -- server/src/test/java/com/gumtree/csagent/service/tools/ResolveArticleToolTest.java
# Expected: exactly one new @Test method added; first test method
# (execute_searchIneligibleArticle_stillResolvesByDirectId)
# byte-untouched.
```

Verify the new test asserts:
- `result.isSuccess()` (the eligibility-false article still
  resolves via direct ID lookup).
- The returned `source_id` matches `ka41r000000LIEEAA4`.
- The returned `title` matches the article's actual title
  ("(temp) NTD Ad Removed Information" per the JSON corpus).
- The `safe_to_show` field is `true` (published + non-blank body;
  eligibility does NOT gate direct resolve).

This pins the F2 invariant for BOTH flagged article IDs, closing
the §4 #1 blocker.

### Step 5 — Verify focused tests pass

Re-run the focused test set with the new method included:

```
cd server && mvn -o -Dtest=ResolveArticleToolTest,KnowledgeSearchServiceTest,KnowledgeIngestionRunnerTest,KbArticleEligibilityCorpusTest test
```

Expected: all tests green; total count = prior focused (24) + 1
new (`ResolveArticleToolTest` now has 14 methods; up from 13 in
the initial review). The dev report claims focused four-suite
returned 23/0/0/0; verify this matches the suites you re-run.

Optionally run the full Java suite to verify the baseline
preservation:

```
mvn -o clean test
# Expected: 1348 / 1 / 0 / 2 (= prior 1347 + 1 new test;
# sole failure = inherited SystemPromptUserRequestedTiebreakerTest /
# OQ-S41.5).
```

The dev report claims the full suite is at `1348 / 1 / 0 / 2`;
verify.

### Step 6 — Update the §1 / §2 / §3 / §4 entries

Update the existing `docs/codex-findings.md` in place:

- **§1 #3 V17 migration verdict** — replace "fix required for
  forbidden-grep hygiene; schema operation passes" with a pure
  pass verdict, citing `e6aad78` and the new comment wording.
- **§1 #8 direct-resolve invariant verdict** — replace "fix
  required" with a pass verdict, citing `d27b824` and the new test
  method name + the now-correct handoff §3 R6 #8 + §6 fence
  evidence at `4c8931f`.
- **§2 Q8 generalization eval coverage** — replace "Fail pending
  targeted negative-control completion" with a clean pass,
  referencing the new second direct-resolve test.
- **§3 F2 direct-resolve invariant** — flip from fail to pass,
  citing the new test at `d27b824`.
- **§3 F3 forbidden-grep evidence** — flip from fail to pass,
  citing the V17 comment rewrite at `e6aad78` + the grep result
  on the new cumulative range.
- **§4 Blocking Findings** — replace the two prior blockers with:
  ```
  ## §4 Blocking Findings

  None on re-review. The prior two blocking findings were resolved
  by the fix-iteration commits e6aad78 (V17 SQL comment
  content-neutral rewrite; closes §4 #2) and d27b824 (second
  direct-resolve invariant test for ka41r000000LIEEAA4; closes §4
  #1). The handoff at 4c8931f added the §3.1 fix-iteration
  addendum and corrected the prior "both test-pinned" over-claims
  at §3 R6 #8 and §6 fence. Cumulative range now bb48aa0^..4c8931f
  (8 commits); audited range unchanged in code structure outside
  the two fix files.
  ```

### Step 7 — Flip the verdict header

Replace the top-of-file verdict header with:

```
## Sprint Review Decision
decision: pass
blocking_count: 0
final_verdict: APPROVE_S_AUTO_27
summary: <see template below>
```

Suggested summary template (adapt freely; cite the resolution
commits):

> Sprint 082 / S-Auto-27 / M-Auto-6 Sub-sprint C-2b is approved
> on targeted re-review. The cumulative range now extends to
> `bb48aa0^..4c8931f` (8 commits = original 5 + fix-iteration 3).
> The prior two §4 blocking findings were both Codex-tagged
> "infra hygiene gap, not executable semantic hardcode" and were
> resolved by `e6aad78` (V17 SQL comment content-neutral rewrite;
> closes §4 #2) and `d27b824` (second direct-resolve invariant
> test for `ka41r000000LIEEAA4`; closes §4 #1); `4c8931f` added
> the handoff §3.1 fix-iteration addendum + corrected the prior
> over-claims. Full Java suite `1348 / 1 / 0 / 2`; focused tests
> `23 / 0 / 0 / 2`; grep gates clean. Substantive verdicts at §1
> #1 / #2 / #4 / #5 / #6 / #7, §2 Q1–Q7 + Q9, §3 F1 / F4 / F5 /
> F6 stand from the initial review; §1 #3 / #8, §2 Q8, §3 F2 /
> F3 flip to pass. Verdict flipped to `APPROVE_S_AUTO_27 /
> blocking_count=0`.

### Step 8 — Preserve unchanged sections

DO NOT redo or rewrite:
- §1 verdicts on #1 / #2 / #4 / #5 / #6 / #7 / Fence + cumulative
  range (the cumulative range entry should be UPDATED to reflect
  the new 8-commit range, but the structural verdict is preserved).
- §2 Q1–Q7 + Q9 walkthrough.
- §3 F1 / F4 / F5 / F6 focal-point verdicts.
- §5 non-blocking observations.

The unchanged sections remain in `docs/codex-findings.md` as-is.

Optional (RECOMMENDED): append a one-line addendum to §5 noting
re-review verified the fix-iteration shape at HEAD `4c8931f` via
focused test re-run + grep gates.

## Constraints

1. **Do NOT edit any code.** Only `docs/codex-findings.md` may be
   written.
2. **Do NOT re-judge the substantive verdict** on the unchanged
   scopes. Only the entries explicitly impacted by the two fixes
   may be updated.
3. **Do NOT widen scope** to other sub-sprints or to non-C-2b
   parts of the cumulative range. The cumulative range is
   `bb48aa0^..4c8931f`.
4. **Cite anchors** (file:line / commit hash) for every claim.
5. **If you find a NEW blocking issue** (e.g. the fix-iteration
   commits inadvertently introduced a scope drift; the
   working tree is still dirty; the test fails when run; a grep
   gate fails): raise it as a blocking finding and do NOT
   auto-approve.

## Acceptance verdict shapes (re-review)

- **APPROVE_S_AUTO_27 / blocking_count=0**: clean-tree gate
  passes; both fixes verified per Steps 3-5; cumulative range
  unchanged in code structure outside the two fix files;
  substantive verdict stands on unchanged scopes. This is the
  expected outcome given the dev report. Issue the new verdict
  header + §4 update + targeted §1 / §2 / §3 entry updates +
  optional §5 addendum.
- **REJECT_S_AUTO_27 / out_of_scope_review (NEW)**: working tree
  still dirty for a different reason, or fix-iteration commits
  amended/rebased original audited range. Enumerate the new
  finding and require deliver-agent resolution.
- **REJECT_S_AUTO_27 / blocking_count > 0 (substantive)**: a
  fix-iteration commit introduced a NEW substantive issue (e.g. a
  forbidden production code change; a scope-drift test addition;
  a semantic hardcode sneaked in). Enumerate with anchored
  evidence + a one-line corrective direction.

When done: ensure `docs/codex-findings.md` carries the new
verdict header + updated §4 + targeted §1 / §2 / §3 entry updates
+ preserved unchanged §1 / §2 / §3 / §5 entries + (optional)
§5 addendum; commit nothing else; hand back to deliver-agent for
close + M-Auto-6 milestone-shared §9 real-LLM re-bless launch.
