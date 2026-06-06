## Sprint Review Decision
decision: fix_required
blocking_count: 2
final_verdict: APPROVE_S_AUTO_27_WITH_FIXES
summary: The cumulative five-commit range `bb48aa0^..7773c92` is structurally sound and introduces no operational semantic hardcode: the search filter uses only `KbArticle.isSearchKnowledgeEligible()`, default-visible behavior is preserved across entity/migration/ingestion, the two corpus flips are exact, and production direct resolve remains unfiltered. Two narrow acceptance gaps block close: the required direct-resolve invariant is test-pinned for only one of the two flagged article IDs, while the handoff claims both; and the required `server/src/main` forbidden grep finds a newly introduced `(temp)` literal in the V17 migration comment. Remove that literal and pin direct resolve for `ka41r000000LIEEAA4`, then targeted re-review can approve.

## §1 Per-Change Verdicts

**#1 — R6 corpus JSON flip: pass.** Commit `977f9b8` changes exactly two JSON lines (`+2/-2`), flipping only `search_knowledge_eligible` for `ka41r000000LIEJAA4` and `ka41r000000LIEEAA4` (`data/knowledge/knowledge_base_articles.json:272-300`). Review-time structured audit found `218` articles, exactly those two at `false`, both still `published_status=true`, and no article missing the eligibility field. `KbArticleEligibilityCorpusTest.java:29-74` pins the exact-two and remain-published corpus invariants.

**#2 — `KbArticle` entity field: pass.** Commit `bb48aa0` adds primitive `boolean searchKnowledgeEligible`, `@Builder.Default=true`, and the non-null `search_knowledge_eligible` column only (`KbArticle.java:51-60`). Lombok `@Data` supplies `isSearchKnowledgeEligible()`.

**#3 — V17 migration: fix required for forbidden-grep hygiene; schema operation passes.** V17 is the next migration after V16 and its sole operation is `ADD COLUMN search_knowledge_eligible BOOLEAN NOT NULL DEFAULT TRUE` (`V17__add_kb_search_knowledge_eligible.sql:6-7`), matching the existing non-null/default-true published-column shape (`V4__create_kb_articles.sql:9`). However, V17 introduces `(temp)` in a `server/src/main` comment (`V17__add_kb_search_knowledge_eligible.sql:4`), failing the prompt's explicit F3 zero-new-match gate. Fix: replace that comment wording with content-neutral language; do not change the SQL operation.

**#4 — ingestion parser: pass.** Commit `0594a25` mirrors the existing `published_status` idiom with `path("search_knowledge_eligible").asBoolean(true)` and passes the value into the builder (`KnowledgeIngestionRunner.java:205-214`, `:224-238`). Explicit false, explicit true, and absent/default-true are pinned (`KnowledgeIngestionRunnerTest.java:30-100`). No other mapped field changed in the commit.

**#5 — search-service filter: pass.** Commit `d21594f` adds one adjacent check inside the existing Step-8 published-defense lambda; the operational predicate is only `!article.isSearchKnowledgeEligible()` (`KnowledgeSearchService.java:223-246`). Score, rerank, ordering, result-limit placement, and hit construction remain unchanged (`:247-259`).

**#6 — `KnowledgeHit` propagation SKIPPED: pass.** `KnowledgeHit.java` has zero diff in the range. The contract-allowed rationale is documented: hits are constructed after the filter and are eligible-by-construction (`sprint-082-handoff.md:114-119`).

**#7 — INFO log path α: pass.** The service emits an INFO log per filtered article with `article_id`, fixed structural `filter_reason`, `session_id`, and `turn_index` (`KnowledgeSearchService.java:240-245`). `SearchKnowledgeTool.java` is byte-untouched; the path-α rationale is documented (`sprint-082-handoff.md:120-126`).

**#8 — direct-resolve invariant: fix required.** Production direct resolve remains unfiltered and reads directly by ID, gating only unpublished articles (`ResolveArticleTool.java:66-81`). Commit `977f9b8` pins successful direct resolve for `ka41r000000LIEJAA4` only (`ResolveArticleToolTest.java:239-272`); there is no `ka41r000000LIEEAA4` occurrence in that test file. This contradicts the handoff's “both ... test-pinned” claims (`sprint-082-handoff.md:68-69`, `:227-228`) and fails the explicit F2 requirement. Fix: parameterize or add a second direct-resolve test covering `ka41r000000LIEEAA4`.

**Fence / cumulative range: pass.** `git rev-list --count bb48aa0^..7773c92` returns `5`, exactly commits `bb48aa0`, `0594a25`, `d21594f`, `977f9b8`, and `7773c92`. The ten cumulative touched files are the nine authorized implementation/test files plus the authorized handoff; forbidden production, skill, UI, eval, autoloop, and baseline paths have zero diff. `git diff --check` is clean.

## §2 Nine-Question Kernel Walkthrough

**Q1 — Semantic decision hardcode? Pass.** The runtime decision is driven only by `KbArticle.isSearchKnowledgeEligible()` (`KnowledgeSearchService.java:240-246`); the two decisions live in corpus data (`knowledge_base_articles.json:281`, `:300`). No Java article-ID list, title/body scan, or per-UC matrix exists.

**Q2 — Tier-0 invariant justification? Pass / no new Tier-0.** The articles remain in the corpus and published (`knowledge_base_articles.json:272-300`), production direct resolve does not inspect eligibility (`ResolveArticleTool.java:66-81`), and only search hit projection filters (`KnowledgeSearchService.java:223-259`).

**Q3 — Could soft-signal projection suffice? Pass.** Corpus eligibility is a Runtime-owned retrieval boundary. Asking the LLM to infer template status from title/body text would encode the forbidden content heuristic; the shipped implementation instead consumes a structured data field (`KbArticle.java:58-60`; `KnowledgeSearchService.java:240-246`).

**Q4 — Eval/case text encoded? Pass for operational behavior.** The predicate does not inspect query, title, body, CaseSpec text, or article ID (`KnowledgeSearchService.java:240-246`). The V17 comment literal is an F3 hygiene failure, not executable semantic matching.

**Q5 — Semantic ownership moved LLM → Java? Pass.** Java narrows the available corpus by one structural eligibility flag; it does not choose the query, relevance, citation, response strategy, or wording (`KnowledgeSearchService.java:193-259`).

**Q6 — Prompt if-else instead of principles? N/A — no prompt or skill edit.** The cumulative name-status diff contains no YAML or prompt-runtime file.

**Q7 — Tool schema / safety / grounding floors preserved? Pass.** `SearchKnowledgeTool.java`, `ResolveArticleTool.java`, `SkillGuardrailDispatcher.java`, and skill YAML have zero diff. Path α changes no tool result shape (`SearchKnowledgeTool.java:52-74`), and production direct resolve's published-safety floor remains unchanged (`ResolveArticleTool.java:74-81`).

**Q8 — Generalization eval coverage? Fail pending targeted negative-control completion.** Search target/neighbor/default-visible behavior is covered (`KnowledgeSearchServiceTest.java:57-138`), ingestion default-visible behavior is covered (`KnowledgeIngestionRunnerTest.java:30-100`), and the corpus test covers both flags/remain-published (`KbArticleEligibilityCorpusTest.java:29-74`). However, the required direct-resolve negative control is pinned for only one flagged ID (`ResolveArticleToolTest.java:239-272`), not both.

**Q9 — Temporary hardcode / sunset plan? Pass.** The eligibility column, parser, and predicate are durable structural mechanisms (`V17__add_kb_search_knowledge_eligible.sql:6-7`; `KnowledgeIngestionRunner.java:213-214`; `KnowledgeSearchService.java:240-246`); no temporary branch shipped.

**§4.1 aggregate verdict:** `approve with targeted fixes`. No executable semantic hardcode exists, but Q8 and the explicit F3 acceptance gate must be corrected before close.

## §3 Five Focal-Point Verdicts

**F1 — Data-field-only filter: pass.** Commit `d21594f` adds only the eligibility check and INFO log immediately after the existing `isPublished` check (`KnowledgeSearchService.java:223-246`). No content, title, ID, UC, reranker, score, or LLM branch was added.

**F2 — Direct resolve invariant: fail.** Production behavior is preserved (`ResolveArticleTool.java:66-81`), but the required per-flagged-ID test evidence is incomplete: only `ka41r000000LIEJAA4` is pinned (`ResolveArticleToolTest.java:239-272`); `ka41r000000LIEEAA4` is absent.

**F3 — Forbidden-grep evidence: fail.** Both flagged ID literals return zero hits in `server/src/main`, and no executable title/content filter exists. Nevertheless, cumulative added-line grep finds one new `(temp)` hit at `V17__add_kb_search_knowledge_eligible.sql:4`, contrary to the explicit zero-new-match requirement.

**F4 — Back-compat default-true: pass.** Entity builder default is true (`KbArticle.java:58-60`), V17 backfills/defaults true (`V17__add_kb_search_knowledge_eligible.sql:6-7`), ingestion absent/null semantics use `asBoolean(true)` (`KnowledgeIngestionRunner.java:213-214`), and the primitive/non-null field means search rejects only explicit false (`KnowledgeSearchService.java:240-246`). Focused tests pin absent/default-visible and all-eligible/default-eligible behavior (`KnowledgeIngestionRunnerTest.java:78-100`; `KnowledgeSearchServiceTest.java:83-97`, `:115-138`).

**F5 — No parallel governance field: pass.** The cumulative implementation diff introduces no `bot_visible`/`botVisible`; it reuses `search_knowledge_eligible` across data, entity, ingestion, migration, and service.

**F6 — Pre-fix audit decisions: pass.** `KnowledgeHit.java` and `SearchKnowledgeTool.java` have zero diff. The SKIPPED and path-α rationales are explicitly recorded (`sprint-082-handoff.md:114-126`), and the service-layer INFO log is present (`KnowledgeSearchService.java:240-245`).

## §4 Blocking Findings

1. **Incomplete direct-resolve invariant coverage (`infra` characterization-test gap).** `ResolveArticleToolTest.java:239-272` pins only `ka41r000000LIEJAA4`; the second required flagged ID is absent, despite the handoff claiming both are test-pinned (`sprint-082-handoff.md:68-69`, `:227-228`). Corrective direction: parameterize the test over both IDs or add a second equivalent case for `ka41r000000LIEEAA4`, then correct the handoff evidence if needed.

2. **Forbidden-grep gate failure (`infra` hygiene gap, not executable semantic hardcode).** The cumulative range adds `(temp)` at `V17__add_kb_search_knowledge_eligible.sql:4`, while F3 requires zero newly introduced `server/src/main` matches. Corrective direction: replace the comment phrase with content-neutral wording and leave the migration SQL unchanged.

## §5 Non-Blocking Observations

1. **Pre-C-2b consumer sanity check passed.** `git grep` at `bb48aa0^` finds zero `search_knowledge_eligible` / `searchKnowledgeEligible` references under `server/src/main`; the handoff's ingested-but-unused diagnosis is accurate (`sprint-082-handoff.md:101-106`).

2. **Clean tree passed, but review HEAD differs from the prompt expectation.** The pre-review working tree was clean. HEAD was `3300b4a`, not `7773c92`; the two descendant commits add only the separately named framework-plan archive (`056fa5a`) and this review prompt (`3300b4a`). They are outside the explicitly audited range, and `7773c92` is an ancestor, so this is not cumulative-range scope drift.

3. **Focused verification passed.** Review-time command `mvn -o -Dtest=KnowledgeIngestionRunnerTest,KnowledgeSearchServiceTest,KbArticleEligibilityCorpusTest,ResolveArticleToolTest,Sprint14KnowledgeSearchPublishedFilterTest,Sprint14KnowledgeIngestionCanonicalUrlTest,Sprint14ResolveArticleSafetyTest test` completed with `34 / 0 / 0 / 0`. `git diff --check bb48aa0^..7773c92` was clean.
