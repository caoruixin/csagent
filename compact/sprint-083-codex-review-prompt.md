# Sprint 083 / S-Auto-28 / M-Auto-6 — per-sub-sprint Codex review prompt (R8 KnowledgeIngestionRunner reconcile path)

**Authored by deliver-agent at S-Auto-28 dev-side close, 2026-06-07.**
**Self-contained executable view per `prompt-artifact-rules.md`
§9.1/§9.2** — embeds the §4.1 nine-question anti-hardcode kernel
verbatim from `docs/current/anti-hardcode-review-kernel.md` + the full
sub-sprint §7 stanza + the file-path fence + the six focal points the
contract committed to. Dev outputs (the handoff at
`docs/sprints/sprint-083-handoff.md`) are NOT embedded because they
post-date this prompt's authoring schedule by definition — they are
referenced by path.

---

## 1. Role identity

You are the **Anti-Hardcode Per-Sub-Sprint Review Agent for
Sub-sprint S-Auto-28 (Sprint 083 / M-Auto-6 R8 KnowledgeIngestionRunner
reconcile path)**.

Your review covers the **audited cumulative commit range
`ba3defa^..78ae614`** (4 commits — see §5.1 for the per-commit
manifest). HEAD at dispatch time is `29425f4` (the most recent commit
is the parallel-workstream `compact/framework-plan-v4-*` precursor
preservation, mirroring the `056fa5a` / `57fad66` precedents from
S-Auto-26 / S-Auto-27 — outside the audited range, not scope drift; if
HEAD has advanced further, the additional commits are deliver-agent
artifact/audit commits also outside the audited range).

**Class** of this sub-sprint:

- Layer (per `iteration_governance.md` §3.2): **`infra`** —
  server-side knowledge data-application path. R8 fixes an insert-only
  loader (`KnowledgeIngestionRunner`) that could not apply R6's
  curation-flag change to existing DB rows, by adding a metadata-only
  `--reconcile` UPDATE mode.
- **§7 stanza requirement:** INCLUDED conservatively. R8 itself is
  pure infra and arguably §7-exempt, but its downstream effect is the
  same LLM-facing `search_knowledge` surface R6 (S-Auto-27) governs —
  R8 is what makes R6's already-shipped filter actually take effect on
  the live corpus. You may record the **infra-exemption explicitly**
  and `approve` if the change is what the contract describes.

**Context**: this sub-sprint is the M-Auto-6 **milestone-close
blocker** (per
`docs/diagnostics/failure-briefs/preflight-2026-06-07-kb-ingest-skip-existing-blocks-r6-flag.md`).
The §5.9 pre-flight runbook returned NO-GO at §0.3 on 2026-06-07
(`docs/diagnostics/2026-06-07-m-auto-6-preflight-verdict.md`) because
the populated dev DB's 2 `(temp)` template rows still read
`search_knowledge_eligible=true` after R6's JSON flip was committed —
the insert-only runner skips every already-present `article_id` and
never UPDATEs. The R6 close gates (`BEGIN…ROLLBACK` migration-mechanics
test + synthetic-article unit tests) did not catch this populated-DB
regression; the pre-flight runbook did, exactly as the §5.9 gate was
designed to. R8 is the structural fix.

---

## 2. Loader (minimal)

Read in this order:

1. `AGENTS.md` (auto-loaded; transitively loads the governance chain
   `doc_governance.md` → `agent_context_guide.md` →
   `iteration_governance.md`).
2. This prompt (you are reading it).
3. **Dev handoff** (dev output, post-dates this prompt's authoring;
   NOT embedded):
   - `docs/sprints/sprint-083-handoff.md` — §0 verdict summary, §1
     per-item file:line + pre-fix audit, §2 evidence (test counts +
     mock-interaction counts + INFO log samples + forbidden-grep), §3
     STOP / fence confirmations, §4 §7 stanza verbatim, §5 commit map.
4. **Code under review** (read for evidence, do NOT modify):
   - `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
     — the production change. Runner-entry gate at `:78-85`; mode
     comment at `:87-91`; `existingById` indexing at `:108-110`;
     `processArticles(...)` per-article loop at `:144-192`;
     `reconcileExisting(...)` at `:216-261`; reconcile INFO at
     `:258-259`; run-summary INFO at `:115-123`; `IngestionStats` record
     at `:270-274`.
   - `server/src/test/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionReconcileTest.java`
     — 9 new tests (mock-interaction discipline; F1 + F2 + #4(a) + #4(b)
     coverage).
   - `server/src/test/java/com/gumtree/csagent/service/knowledge/KnowledgeReconcileEndToEndTest.java`
     — 1 new test (#4(c) R6 end-to-end at mock level; F4 direct-resolve
     invariant + F5 R6 search-filter exclusion).
5. **Reference for the contract** (consult only when an audit point is
   unclear from this prompt + the handoff; NOT embedded):
   - `docs/sprint_objective.md` — canonical contract (this prompt
     mirrors it; the handoff §4 stanza is the contract's §7 stanza
     verbatim).
   - `docs/current/process/preflight-eval-checks.md` — the runbook R8
     #5 fixed (drift fixes + the §0.3 / A3 root-cause annotation in a
     separate docs-only commit `97d7801`).
   - `docs/diagnostics/failure-briefs/preflight-2026-06-07-kb-ingest-skip-existing-blocks-r6-flag.md`
     — the blocker brief that motivated the sub-sprint.
6. **R6 surface anchors** (consult to verify byte-unchanged claim):
   - `server/src/main/java/com/gumtree/csagent/model/KbArticle.java`
   - `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
   - `server/src/main/resources/db/migration/V17__add_kb_search_knowledge_eligible.sql`
   - `data/knowledge/knowledge_base_articles.json`

`docs/codex-findings.md` is your verdict destination per §6 below.
Replace its current content (which carries the C-2b
`APPROVE_S_AUTO_27` verdict; that verdict is preserved in git history
at `6431ab9`) with the §4.2 sprint-close header for S-Auto-28 + the
findings sections.

**Do not edit any code, any sub-sprint archive, any milestone archive,
or any other doc.** You may edit `docs/codex-findings.md` only.

---

## 3. Embedded sub-sprint contract (verbatim where relevant)

### 3.1 Goal (from contract §Goal)

After this sub-sprint ships:

- **R8 #1 reconcile method** — `KnowledgeIngestionRunner` gains a
  reconcile path that, for an `article_id` already present in
  `kb_articles`, UPDATEs only the mutable curation columns
  (`search_knowledge_eligible` / `is_published` / `uc_tags`) from JSON.
  Loads the existing entity, sets ONLY the 3 curation fields, calls
  `save()`. Content columns retain their DB values; chunks/embeddings
  are never recomputed.
- **R8 #2 `--reconcile` gate** — runs only under an explicit
  `--reconcile` argument. New ids fall through to the existing
  insert path (chunk + embed + save) under both modes. Plain
  `--ingest` (no `--reconcile`) is byte-for-byte unchanged. Canonical
  invocation is **standalone `--reconcile`**; `--ingest --reconcile`
  is non-canonical (parser-accepted but not pinned anywhere).
- **R8 #3 reconcile observability** — per-reconciled-row INFO line
  with `article_id` + changed fields `old → new` + end-of-run
  summary `articlesReconciled / articlesInsertedNew /
  articlesUnchanged`. INFO level; NOT a `ToolEvent`.
- **R8 #4 R6 end-to-end validated (mock level + planned real-DB
  post-close)** — reconcile flips both `(temp)` ids to `false`;
  `KnowledgeSearchService` excludes them; `ResolveArticleTool`
  direct-by-id returns both (anti-误杀 #1). Real-DB §0.3 evidence is
  sequenced AFTER Codex `APPROVE_S_AUTO_28` per the Definition-of-done
  (requires backend rebuild + mutates the shared dev DB).
- **R8 #5 runbook accuracy** — 5 drifts + the §0.3 / A3 annotation
  fixed in `docs/current/process/preflight-eval-checks.md` (separate
  docs-only commit `97d7801`).
- **R8 #6 handoff** — `docs/sprints/sprint-083-handoff.md`.

### 3.2 Anti-误杀 invariants (HARD, non-negotiable)

1. Direct resolve unaffected (`ResolveArticleTool.byArticleId`
   returns both `(temp)` articles before and after reconcile).
2. Reconcile is metadata-only (NEVER re-chunks, NEVER re-embeds,
   NEVER writes `kb_chunks`).
3. Content columns immutable under reconcile (`title` / `summary` /
   `description` / `source_url` / `url_category` / `token_count` on
   existing rows never overwritten).
4. Plain `--ingest` byte-for-byte unchanged (insert-only;
   skip-existing).
5. No prune (a DB row whose `article_id` is absent from JSON is never
   deleted).
6. No hardcoded article-id in Java (zero `ka41r…` / `(temp)` literals
   in `server/src/main`).
7. No manual SQL as evidence (the §0.3 DB state at the
   post-Codex-APPROVE Definition-of-done step is produced by the
   `--reconcile` run, NOT a hand `UPDATE`).
8. No R6-surface touch (`KnowledgeSearchService` / `KbArticle` /
   V17 / `knowledge_base_articles.json` byte-unchanged).
9. No new `escalation_reason` enum value. No new Tier-0 invariant.
10. `baseline_dir` UNCHANGED. `docs/current_eval_baseline.md`
    UNCHANGED.

### 3.3 File-path fence

**Files allowed to edit:**

- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
  (reconcile method + `--reconcile` gate + reconcile logging only).
- `server/src/test/java/.../KnowledgeIngestionRunnerTest.java` (extend
  / new reconcile tests).
- A new test class in the same test package (`KnowledgeIngestionReconcileTest.java`
  and/or `KnowledgeReconcileEndToEndTest.java`).
- `docs/current/process/preflight-eval-checks.md` (R8 #5 only;
  separate docs commit).
- `docs/sprints/sprint-083-handoff.md` (dev handoff).

**Files FORBIDDEN to edit:**

- `data/knowledge/knowledge_base_articles.json` (R6 already
  committed the flips).
- `server/.../model/KbArticle.java` (field already exists from R6).
- `server/.../resources/db/migration/V17__*.sql` (shipped immutable).
- `server/.../service/knowledge/KnowledgeSearchService.java` (R6
  filter — byte-unchanged).
- `server/.../service/tools/ResolveArticleTool.java` (direct resolve
  must stay unfiltered; only `ResolveArticleToolTest` may be touched
  for a regression assertion — no production change).
- `server/.../model/KnowledgeHit.java`.
- Any skill yaml / `SkillGuardrailDispatcher` /
  `resolve_faq_grounded_answer.yaml`.
- `IntakeFieldsRegistry` / `IntakeFieldsMerger` / `BudgetChecker` /
  `ControlKernel` / `AgentRunLoopImpl` / `ContextProjectionBuilder` /
  `UpdateIntakeFieldsTool`.
- Any UI file. Any `eval_interactive/` file. Autoloop 5-file
  SHA-locked scoring set. `autoloop/config.yaml` `baseline_dir`.
  `docs/current_eval_baseline.md`.

### 3.4 §7 stanza (verbatim from contract §3.7 / handoff §4)

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

---

## 4. Embedded §4.1 nine-question anti-hardcode kernel (verbatim from `docs/current/anti-hardcode-review-kernel.md`)

```text
You are the Anti-Hardcode Review Agent. The PR below proposes a change
to this repo. Your job is to decide whether the change introduces a
semantic hardcode — a keyword / regex / if-else / enum / per-UC matrix
that encodes a decision the LLM is supposed to own under
docs/current/iteration_governance.md §1.3 — and to issue a verdict.

Scope exemption: pure infra, docs-only, config-governance, and
characterization-test PRs are not subject to this review. If the PR
is purely one of those, return `approve` with a one-line note naming
the exemption.

For every other PR, walk these nine questions in order. For each
"yes" or each concern, paste the diff snippet and the reasoning.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix
   for a semantic decision (drift detection, escalation, UC
   selection, risk classification, follow-up, intake routing)?
2. If yes to (1), is the change justified as protecting a current
   Tier-0 invariant named in docs/runtime_freeze_and_risk_policy.md
   §1 / §2?
3. Could the same outcome be achieved by projecting a soft signal to
   the LLM (an additional projected slot, a candidate list, a
   diagnostic flag) instead of a hard branch in Java or the prompt?
4. Does the change encode visible-eval case text, trace-specific
   phrasing, or a CaseSpec id into runtime, prompt, or judge config?
5. Does the change move semantic ownership from the LLM to Java —
   that is, shrink what docs/current/iteration_governance.md §1.3
   says the LLM owns?
6. Does the change add an if-else block to the prompt instead of
   principle-level or observable-state guidance?
7. Does the change preserve tool schema, capability / permission
   boundary, PII / safety floor, and grounding floor?
8. Does the PR ship generalization eval coverage — target, neighbor,
   negative, and shadow cases — and not only the target case?
9. If the change is temporary, does it carry an explicit rollback or
   sunset plan (downgrade-to-signal trigger, retirement sprint id)?

Return exactly one verdict:

- `approve` — the change is not a semantic hardcode, or is justified
  as protecting a current Tier-0 invariant with adequate generalization
  coverage and a clear rollback if temporary.
- `approve with downgrade-to-signal follow-up` — the change is
  acceptable as an interim measure, but a follow-up sprint must
  convert it into a soft signal projected to the LLM. Name the
  trigger that should fire the conversion.
- `reject as semantic hardcode` — the change encodes a soft semantic
  decision the LLM should own; questions 1 and 2 fail, or questions
  5 / 6 fail, with no Tier-0 claim and no sunset plan.
- `needs human architecture decision` — the change crosses an
  unresolved governance question (new escalation reason enum value,
  new Tier-0 candidate, LLM-vs-Java boundary shift, new tool surface)
  and a human reviewer must decide before merge.

Do not rewrite the PR. Do not propose a code fix beyond naming the
layer in docs/current/iteration_governance.md §3 that the fix should
target.
```

---

## 5. Cumulative scope claim

### 5.1 Per-commit manifest (audited range `ba3defa^..78ae614` — 4 commits)

| # | Commit | Subject | Surface | In-fence? |
|---|---|---|---|---|
| 1 | `ba3defa` | R8 #1+#2+#3 KnowledgeIngestionRunner --reconcile metadata-only data-application + reconcile observability + `KnowledgeIngestionReconcileTest` (9 tests covering #4(a) + #4(b)) | server prod + test | ✅ — single prod file (`KnowledgeIngestionRunner.java`) + single new test file (`KnowledgeIngestionReconcileTest.java`) |
| 2 | `7ae61d2` | R8 #4(c) R6 end-to-end reconcile→search→resolve wiring test | server test | ✅ — single new test file (`KnowledgeReconcileEndToEndTest.java`) |
| 3 | `97d7801` | R8 #5 preflight-eval-checks.md drift fixes (5) + §0.3 / A3 root-cause annotation | docs | ✅ — docs-only commit per Commit discipline 3 of 4; runbook is in-fence |
| 4 | `78ae614` | R8 #6 dev handoff (`docs/sprints/sprint-083-handoff.md`) | docs | ✅ — single new handoff file; in-fence |

**Commit ordering** matches the contract's Commit discipline (§3.10 of
the dev prompt): #1+#2+#3 + #4(a/b) bundled in commit 1; #4(c) in
commit 2; #5 docs-only in commit 3; #6 handoff in commit 4.

### 5.2 Files touched outside the 4 audited commits

`git status --short` at dispatch time = clean. HEAD = `29425f4`
(parallel-workstream `compact/framework-plan-v4-*` preservation —
mirrors the `056fa5a` / `57fad66` precedents from C-2a / C-2b;
acknowledged as outside the audited range, not scope drift). If
`docs/codex-findings.md` is dirty at dispatch (it carries the prior
C-2b verdict), that is expected; you overwrite it with your verdict
per §6.

### 5.3 Baselines (per dev handoff §0 / §2)

- **Java full suite**: `1358 / 1 / 0 / 2` (= prior `1348` from
  S-Auto-27 close + exactly 10 new R8 tests; sole failure is the
  inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`,
  OQ-S41.5 — system-prompt content surface, provably uncoupled; this
  sub-sprint touched zero prompt files).
- **Focused suite** (`-Dtest=KnowledgeIngestionReconcileTest,KnowledgeReconcileEndToEndTest,KnowledgeIngestionRunnerTest,KnowledgeSearchServiceTest,ResolveArticleToolTest`):
  `31 / 0 / 0 / 0` (9 new reconcile + 1 new end-to-end + 3 pre-existing
  KnowledgeIngestionRunner + 4 pre-existing KnowledgeSearchService + 14
  pre-existing ResolveArticleTool).
- **Forbidden-grep**: `grep -rn "ka41r000000LIEJAA4\|ka41r000000LIEEAA4" server/src/main` → **0**; `grep -rn "(temp)" server/src/main` → **0** (handoff §2 evidence).
- **R6 surface byte-unchanged**: `KnowledgeSearchService.java`,
  `KbArticle.java`, `V17__*.sql`, `knowledge_base_articles.json`
  ABSENT from the diff (handoff §2 evidence + verifiable via
  `git diff --name-only ba3defa^..78ae614`).
- **Eval / autoloop pytest**: UNCHANGED by construction — `git diff
  --name-only ba3defa^..78ae614` shows zero files under `eval_interactive/`
  or `autoloop/`. (Eval pytest `553`; autoloop pytest `324`; UI vitest
  `10/0/0` — all unchanged by construction.)
- **`baseline_dir`** + **`docs/current_eval_baseline.md`**: UNCHANGED;
  flip at M-Auto-6 milestone close after the (now-unblocked)
  milestone-shared re-bless.

### 5.4 Real-DB §0.3 evidence is DEFERRED post-Codex by design

Per §5.7 (wiring-vs-outcome separator) and the contract's
Definition-of-done sequence, the live `--reconcile` run + §0.3 re-check
are produced AFTER Codex `APPROVE_S_AUTO_28` because they require a
backend rebuild and mutate the shared dev DB. **The dev-close gate is
the mock-level wiring evidence in §5.3 above**, NOT the real-DB §0.3
evidence. Your verdict on this sub-sprint judges the wiring evidence;
the real-DB outcome is the deliver-agent + human responsibility on the
post-APPROVE Definition-of-done sequence. (You may still recommend
specific assertions for the post-APPROVE sequence as non-blocking
observations.)

---

## 6. Output format — write your verdict to `docs/codex-findings.md` using this §4.2 sprint-close header verbatim

Replace `docs/codex-findings.md`'s current content (carrying the prior
C-2b `APPROVE_S_AUTO_27` verdict, preserved in git history at
`6431ab9`) with the §4.2 header for S-Auto-28 + the structured
findings:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph naming the sub-sprint-level finding. If
  `pass`, state the §4.1 kernel walk-through outcome + the six focal-point
  verdicts. If `fix_required`, name the P0/P1 findings + the layer per
  `iteration_governance.md` §3. If `out_of_scope_review`, name the
  scope drift.>
final_verdict: APPROVE_S_AUTO_28 | APPROVE_S_AUTO_28_WITH_FIXES | REJECT_S_AUTO_28 | OUT_OF_SCOPE_S_AUTO_28
```

Below the header, structure your findings in this order:

### §1 Per-change verdicts (R8 #1 through #6)

For each item, cite file:line for the production change OR the test
class + test method names; state pass / fix-required / reject.

### §2 §4.1 nine-question kernel walk-through

Walk Q1–Q9 once across the cumulative diff. For each `yes` or
concern, paste the diff snippet (file:line anchored) + the reasoning.
Aggregate verdict per the kernel's set. The contract claims this
sub-sprint is pure infra under the kernel's scope exemption — if you
agree, record the exemption explicitly and `approve` with a one-line
note naming the exemption (the kernel allows this short-circuit).

### §3 Six focal-point verdicts (F1–F6)

State each focal-point verdict (pass / fix-required) with anchored
evidence:

- **F1 (metadata-only):** reconcile NEVER calls `embeddingClient` /
  writes `kb_chunks` / overwrites content columns. **Crucial: verify
  via Mockito `verify(..., never())` mock-interaction counts, NOT just
  final-state assertion.** The handoff §2 claims `embedBatch` count =
  0 on existing reconcile (across multiple tests) and `saveAll` count
  = 0; confirm by reading the test method bodies in
  `KnowledgeIngestionReconcileTest.reconcile_doesNotReEmbedOrWriteChunks_onExistingRow`
  + the per-row reconcile tests. Also verify `embedBatch` count = 1 on
  the new-id-insert test (`reconcile_newId_takesInsertPath_andEmbeds`)
  — this proves the branch split is correct, not a blanket no-embed.
- **F2 (plain `--ingest` unchanged):** without `--reconcile`, existing
  rows are skipped + untouched (no `save`, no `embedBatch`, no
  `saveAll`). Cite the runner gate at
  `KnowledgeIngestionRunner.java:78-85` (entry widening) +
  `:161-174` (per-article branch) + the test
  `plainIngest_existingRow_skippedAndUntouched_noWriteNoEmbed`.
- **F3 (no manual SQL evidence):** the post-Codex Definition-of-done
  produces the §0.3 DB state by the `--reconcile` run, NOT a hand
  `UPDATE`. The handoff §2 / §3 commit to this; verify the sequence
  in the handoff §2 "Real-DB §0.3 evidence — PENDING" block is
  unambiguous (`mvn spring-boot:run -Dspring-boot.run.arguments=--reconcile`).
- **F4 (direct resolve invariant):** `ResolveArticleTool.byArticleId`
  returns both flagged articles post-reconcile. Cite the test
  `KnowledgeReconcileEndToEndTest.reconcileThenSearch_excludesBothTemplates_butDirectResolveStillReturnsThem`
  (the assertion that `ResolveArticleTool.byArticleId` succeeds on
  both `ka41r000000LIEJAA4` and `ka41r000000LIEEAA4` after reconcile).
- **F5 (forbidden-grep + R6 surface byte-unchanged):** zero `ka41r…` /
  `(temp)` literals introduced in `server/src/main` (re-run the grep
  yourself, do not just trust the handoff); R6 surface
  (`KnowledgeSearchService.java`, `KbArticle.java`, `V17__*.sql`,
  `knowledge_base_articles.json`) byte-unchanged (re-run `git diff
  --name-only ba3defa^..78ae614` yourself; expect the file list at
  §5.1 only).
- **F6 (no prune):** a JSON-absent DB row is never deleted. Confirm
  by reading `KnowledgeIngestionRunner.processArticles(...)` — the
  loop only iterates `articles` (the JSON list) and reconciles /
  inserts. Verify no `kbArticleRepository.delete*` call exists in the
  added code.

### §4 Blocking findings (or "None")

If any P0/P1 finding surfaces, name it + the layer per
`iteration_governance.md` §3 the fix should target. Otherwise:
"None."

### §5 Non-blocking observations

Process learnings, recommended post-APPROVE Definition-of-done
assertions, queued R-items for S-Auto-29+. NBOs do not block close.

---

## 7. Constraints

- You may NOT edit any code under `server/`, `ui/`, `eval/`,
  `eval_interactive/`, `autoloop/`, `data/`, or `scripts/`.
- You may NOT edit any sub-sprint archive
  (`docs/sprints/sprint-{078,079,080,081,082,083}-*.md`).
- You may NOT edit any milestone archive (`docs/milestones/M-Auto-*.md`).
- You may NOT edit `docs/sprint_objective.md`, `docs/milestone_objective.md`,
  `docs/10-handoff.md`, or `docs/action_bank.md`.
- You MAY edit `docs/codex-findings.md` (to write your verdict per §6
  output format).
- You may NOT re-judge any §5.6 bad-case manual verdict — the
  milestone-shared re-bless's bad-case verdict is a human +
  deliver-agent decision per `process/badcase-lifecycle.md` §5.6 (not
  in scope for this sub-sprint anyway since real-LLM re-bless is
  sequenced post-APPROVE).
- You may read code for evidence; you may NOT modify it.
- **Re-verify the forbidden-grep + R6-byte-unchanged claims yourself**
  rather than trusting only the handoff (the C-2b initial review
  failed on F2/Q8 evidence gap + F3 forbidden-grep gate exactly
  because the prior review trusted the handoff text rather than
  re-running the grep; same gate applies here).
- **Re-verify the mock-interaction counts yourself** by reading the
  test method bodies; the F1 verdict depends on this being mock-level
  verification, not assertion-of-final-state.

### Per-sub-sprint Codex trigger note

Per `process/milestone-framework.md` §4.3, per-sub-sprint Codex review
is REQUIRED for this sub-sprint because (a) the data-application path
gates R6's LLM-facing search surface; (b) S-Auto-28 is a milestone-close
blocker. The contract §3.8 records this as a deliberately conservative
call — you may explicitly invoke the kernel's scope exemption for pure
infra (§4 above) and `approve` with the exemption noted, if your audit
of the diff confirms the change is in fact pure infra per the contract's
claim.

---

## 8. Reading order (cold start, after this prompt)

1. This prompt (you are reading it).
2. `AGENTS.md` (auto-loaded).
3. `docs/sprints/sprint-083-handoff.md` (dev output).
4. `git log --oneline ba3defa^..78ae614` (audited range) +
   `git diff ba3defa^..78ae614 -- <surface>` for each F-point's
   anchored file:line.
5. `docs/current/anti-hardcode-review-kernel.md` for the canonical
   §4.1 kernel (the kernel embedded in §4 above is the canonical
   2026-05-25 version; the canonical-copy file is the source of truth
   if you suspect drift).
6. `docs/runtime_freeze_and_risk_policy.md` §1 / §2 for Tier-0
   invariant claims under Q2 of the kernel (the contract claims no
   Tier-0 invariant added).
7. `docs/sprint_objective.md` (canonical contract) for any audit point
   unclear from this prompt + the handoff.

When ready, write your verdict per §6 output format to
`docs/codex-findings.md`. Surface your verdict at the top of the file
with the §4.2 header verbatim.
