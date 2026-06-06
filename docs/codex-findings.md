## Sprint Review Decision
decision: pass
blocking_count: 0
final_verdict: APPROVE_S_AUTO_27
summary: Sprint 082 / S-Auto-27 / M-Auto-6 Sub-sprint C-2b is approved on targeted re-review. The prior two §4 blocking findings, both infra hygiene gaps rather than executable semantic hardcode, were resolved by `e6aad78` (V17 SQL comment content-neutral rewrite) and `d27b824` (second direct-resolve invariant test for `ka41r000000LIEEAA4`); `4c8931f` added the handoff §3.1 fix-iteration addendum and corrected the prior over-claims. The original five delivery commits remain unamended and append-only. The range `bb48aa0^..4c8931f` contains 11 Git commits total: 8 delivery/fix/handoff commits plus the 3 acknowledged audit/package commits `056fa5a`, `3300b4a`, and `1954cb6`. Focused tests passed `23 / 0 / 0 / 0`; full Java suite matched `1348 / 1 / 0 / 2`; grep gates are clean. Verdict flipped to `APPROVE_S_AUTO_27 / blocking_count=0`.

## §1 Per-Change Verdicts

**#1 — R6 corpus JSON flip: pass.** Commit `977f9b8` changes exactly two JSON lines (`+2/-2`), flipping only `search_knowledge_eligible` for `ka41r000000LIEJAA4` and `ka41r000000LIEEAA4` (`data/knowledge/knowledge_base_articles.json:272-300`). Review-time structured audit found `218` articles, exactly those two at `false`, both still `published_status=true`, and no article missing the eligibility field. `KbArticleEligibilityCorpusTest.java:29-74` pins the exact-two and remain-published corpus invariants.

**#2 — `KbArticle` entity field: pass.** Commit `bb48aa0` adds primitive `boolean searchKnowledgeEligible`, `@Builder.Default=true`, and the non-null `search_knowledge_eligible` column only (`KbArticle.java:51-60`). Lombok `@Data` supplies `isSearchKnowledgeEligible()`.

**#3 — V17 migration: pass on targeted re-review.** V17 remains the next migration after V16 and its sole operation is `ADD COLUMN search_knowledge_eligible BOOLEAN NOT NULL DEFAULT TRUE` (`V17__add_kb_search_knowledge_eligible.sql:6-7`), matching the existing non-null/default-true published-column shape (`V4__create_kb_articles.sql:9`). Commit `e6aad78` changes only line 4's comment to the content-neutral “CS-only placeholder template articles”; the SQL operation is byte-unchanged and the `server/src/main` `(temp)` grep is now clean.

**#4 — ingestion parser: pass.** Commit `0594a25` mirrors the existing `published_status` idiom with `path("search_knowledge_eligible").asBoolean(true)` and passes the value into the builder (`KnowledgeIngestionRunner.java:205-214`, `:224-238`). Explicit false, explicit true, and absent/default-true are pinned (`KnowledgeIngestionRunnerTest.java:30-100`). No other mapped field changed in the commit.

**#5 — search-service filter: pass.** Commit `d21594f` adds one adjacent check inside the existing Step-8 published-defense lambda; the operational predicate is only `!article.isSearchKnowledgeEligible()` (`KnowledgeSearchService.java:223-246`). Score, rerank, ordering, result-limit placement, and hit construction remain unchanged (`:247-259`).

**#6 — `KnowledgeHit` propagation SKIPPED: pass.** `KnowledgeHit.java` has zero diff in the range. The contract-allowed rationale is documented: hits are constructed after the filter and are eligible-by-construction (`sprint-082-handoff.md:114-119`).

**#7 — INFO log path α: pass.** The service emits an INFO log per filtered article with `article_id`, fixed structural `filter_reason`, `session_id`, and `turn_index` (`KnowledgeSearchService.java:240-245`). `SearchKnowledgeTool.java` is byte-untouched; the path-α rationale is documented (`sprint-082-handoff.md:120-126`).

**#8 — direct-resolve invariant: pass on targeted re-review.** Production direct resolve remains unfiltered and reads directly by ID, gating only unpublished articles (`ResolveArticleTool.java:66-81`). The original test pins `ka41r000000LIEJAA4` (`ResolveArticleToolTest.java:239-272`), and commit `d27b824` adds the mirror `execute_searchIneligibleArticle_stillResolvesByDirectId_secondTemplate` test for `ka41r000000LIEEAA4`, asserting success, returned source ID, actual corpus title, and `safe_to_show=true` (`ResolveArticleToolTest.java:275-303`). Commit `4c8931f` records the fix and corrects the prior over-claim (`sprint-082-handoff.md:68-71`, `:229-231`, `:277-327`).

**Fence / cumulative range: pass, with range-accounting clarification.** The original audited five commits `bb48aa0` through `7773c92` remain unamended and append-only. The targeted fix commits are exactly `e6aad78`, `d27b824`, and `4c8931f`, touching only V17's comment, `ResolveArticleToolTest`, and the authorized handoff. The literal range `bb48aa0^..4c8931f` contains 11 Git commits, not 8, because it also includes the acknowledged audit/package commits `056fa5a`, `3300b4a`, and `1954cb6`; none changes C-2b production/test behavior. Forbidden production, skill, UI, eval, autoloop, and baseline paths remain untouched, and `git diff --check bb48aa0^..4c8931f` is clean.

## §2 Nine-Question Kernel Walkthrough

**Q1 — Semantic decision hardcode? Pass.** The runtime decision is driven only by `KbArticle.isSearchKnowledgeEligible()` (`KnowledgeSearchService.java:240-246`); the two decisions live in corpus data (`knowledge_base_articles.json:281`, `:300`). No Java article-ID list, title/body scan, or per-UC matrix exists.

**Q2 — Tier-0 invariant justification? Pass / no new Tier-0.** The articles remain in the corpus and published (`knowledge_base_articles.json:272-300`), production direct resolve does not inspect eligibility (`ResolveArticleTool.java:66-81`), and only search hit projection filters (`KnowledgeSearchService.java:223-259`).

**Q3 — Could soft-signal projection suffice? Pass.** Corpus eligibility is a Runtime-owned retrieval boundary. Asking the LLM to infer template status from title/body text would encode the forbidden content heuristic; the shipped implementation instead consumes a structured data field (`KbArticle.java:58-60`; `KnowledgeSearchService.java:240-246`).

**Q4 — Eval/case text encoded? Pass for operational behavior.** The predicate does not inspect query, title, body, CaseSpec text, or article ID (`KnowledgeSearchService.java:240-246`). The prior V17 comment-literal hygiene failure was not executable semantic matching and is resolved by `e6aad78`.

**Q5 — Semantic ownership moved LLM → Java? Pass.** Java narrows the available corpus by one structural eligibility flag; it does not choose the query, relevance, citation, response strategy, or wording (`KnowledgeSearchService.java:193-259`).

**Q6 — Prompt if-else instead of principles? N/A — no prompt or skill edit.** The cumulative name-status diff contains no YAML or prompt-runtime file.

**Q7 — Tool schema / safety / grounding floors preserved? Pass.** `SearchKnowledgeTool.java`, `ResolveArticleTool.java`, `SkillGuardrailDispatcher.java`, and skill YAML have zero diff. Path α changes no tool result shape (`SearchKnowledgeTool.java:52-74`), and production direct resolve's published-safety floor remains unchanged (`ResolveArticleTool.java:74-81`).

**Q8 — Generalization eval coverage? Pass on targeted re-review.** Search target/neighbor/default-visible behavior is covered (`KnowledgeSearchServiceTest.java:57-138`), ingestion default-visible behavior is covered (`KnowledgeIngestionRunnerTest.java:30-100`), and the corpus test covers both flags/remain-published (`KbArticleEligibilityCorpusTest.java:29-74`). Direct-resolve negative controls now pin both flagged IDs: `ka41r000000LIEJAA4` (`ResolveArticleToolTest.java:239-272`) and, via `d27b824`, `ka41r000000LIEEAA4` (`:275-303`).

**Q9 — Temporary hardcode / sunset plan? Pass.** The eligibility column, parser, and predicate are durable structural mechanisms (`V17__add_kb_search_knowledge_eligible.sql:6-7`; `KnowledgeIngestionRunner.java:213-214`; `KnowledgeSearchService.java:240-246`); no temporary branch shipped.

**§4.1 aggregate verdict:** `approve`. No executable semantic hardcode exists; Q8 and the explicit F3 acceptance gate are green after `d27b824` and `e6aad78`.

## §3 Five Focal-Point Verdicts

**F1 — Data-field-only filter: pass.** Commit `d21594f` adds only the eligibility check and INFO log immediately after the existing `isPublished` check (`KnowledgeSearchService.java:223-246`). No content, title, ID, UC, reranker, score, or LLM branch was added.

**F2 — Direct resolve invariant: pass on targeted re-review.** Production behavior remains preserved (`ResolveArticleTool.java:66-81`). Commit `d27b824` adds the required second-ID mirror test; both `ka41r000000LIEJAA4` and `ka41r000000LIEEAA4` now assert successful direct resolve and `safe_to_show=true` (`ResolveArticleToolTest.java:239-303`).

**F3 — Forbidden-grep evidence: pass on targeted re-review.** Commit `e6aad78` replaces the V17 line-4 `(temp)` literal with content-neutral wording while leaving the SQL operation byte-unchanged (`V17__add_kb_search_knowledge_eligible.sql:4-7`). Review-time `rg` found zero `(temp)` and zero flagged-ID matches under `server/src/main`; no executable title/content filter exists.

**F4 — Back-compat default-true: pass.** Entity builder default is true (`KbArticle.java:58-60`), V17 backfills/defaults true (`V17__add_kb_search_knowledge_eligible.sql:6-7`), ingestion absent/null semantics use `asBoolean(true)` (`KnowledgeIngestionRunner.java:213-214`), and the primitive/non-null field means search rejects only explicit false (`KnowledgeSearchService.java:240-246`). Focused tests pin absent/default-visible and all-eligible/default-eligible behavior (`KnowledgeIngestionRunnerTest.java:78-100`; `KnowledgeSearchServiceTest.java:83-97`, `:115-138`).

**F5 — No parallel governance field: pass.** The cumulative implementation diff introduces no `bot_visible`/`botVisible`; it reuses `search_knowledge_eligible` across data, entity, ingestion, migration, and service.

**F6 — Pre-fix audit decisions: pass.** `KnowledgeHit.java` and `SearchKnowledgeTool.java` have zero diff. The SKIPPED and path-α rationales are explicitly recorded (`sprint-082-handoff.md:114-126`), and the service-layer INFO log is present (`KnowledgeSearchService.java:240-245`).

## §4 Blocking Findings

None on re-review. The prior two blocking findings were resolved by the fix-iteration commits `e6aad78` (V17 SQL comment content-neutral rewrite; closes §4 #2) and `d27b824` (second direct-resolve invariant test for `ka41r000000LIEEAA4`; closes §4 #1). The handoff at `4c8931f` added the §3.1 fix-iteration addendum and corrected the prior “both test-pinned” over-claims at R6 #8 and the fence confirmation. The original audited five commits remain unamended; outside the two fix files, the fix iteration changes only the authorized handoff.

## §5 Non-Blocking Observations

1. **Pre-C-2b consumer sanity check passed.** `git grep` at `bb48aa0^` finds zero `search_knowledge_eligible` / `searchKnowledgeEligible` references under `server/src/main`; the handoff's ingested-but-unused diagnosis is accurate (`sprint-082-handoff.md:101-106`).

2. **Clean tree passed, but review HEAD differs from the prompt expectation.** The pre-review working tree was clean. HEAD was `3300b4a`, not `7773c92`; the two descendant commits add only the separately named framework-plan archive (`056fa5a`) and this review prompt (`3300b4a`). They are outside the explicitly audited range, and `7773c92` is an ancestor, so this is not cumulative-range scope drift.

3. **Focused verification passed.** Review-time command `mvn -o -Dtest=KnowledgeIngestionRunnerTest,KnowledgeSearchServiceTest,KbArticleEligibilityCorpusTest,ResolveArticleToolTest,Sprint14KnowledgeSearchPublishedFilterTest,Sprint14KnowledgeIngestionCanonicalUrlTest,Sprint14ResolveArticleSafetyTest test` completed with `34 / 0 / 0 / 0`. `git diff --check bb48aa0^..7773c92` was clean.

4. **Targeted re-review verification passed; range count corrected.** At review HEAD `a7c5b6f`, the working tree was clean before this findings edit. The requested focused four-suite run completed `23 / 0 / 0 / 0`; `mvn -o clean test` completed `1348 / 1 / 0 / 2` with only inherited `SystemPromptUserRequestedTiebreakerTest`; both `server/src/main` grep gates returned zero matches. The command `git log bb48aa0^..4c8931f` contains 11 commits total, not the prompt-stated 8: 8 delivery/fix/handoff commits plus acknowledged audit/package commits `056fa5a`, `3300b4a`, and `1954cb6`. This accounting mismatch is non-blocking because those three commits are known review-process artifacts and introduce no C-2b production/test drift.
