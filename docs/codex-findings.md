## Sprint Review Decision

decision: fix_required
blocking_count: 1
summary: L0 is substantially met, and the L1/L2 extraction/diagnostic logic is implemented as passive, non-blocking code with focused Maven regressions green. Sprint 14 does not yet meet the citation observability goal because the new lineage and grounding diagnostics are stamped only onto non-persisted `@Transient` session fields after the `BotTurn` is saved, so they are not observable at trace / BotTurn / event level.

## Blocking Sprint Failures

- severity: P1
- target doc/code/test: `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java:1977`, `server/src/main/java/com/gumtree/csagent/model/BotSession.java:276`, `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:455`, `docs/faq_grounding_contract.md:92`
- blocks current sprint goal: yes
- evidence: `recordRunResult` saves the `BotTurn` before calling `stampFaqGroundingObservability`, and the L1/L2 fields it stamps live only on JPA `@Transient` `BotSession` properties. `ContextProjectionBuilder` does not emit a `faq_grounding` / lineage object, `ChatSessionResponse.additional_data` only returns `latency_ms`, and the contract claims trace surfaces such as `projection.faq_grounding`; after a normal save/reload path the fields are gone, leaving `retrieved_source_ids`, `resolved_source_ids`, `cited_source_ids`, and the soft diagnostics computed but not durably observable.
- exact minimal fix: Compute `SourceEvidenceLineage` and `FaqGroundingDiagnostics` before saving the turn and persist the snake_case fields into an existing trace surface, such as `bot_turns.projected_context.faq_grounding` or a narrow `BotEvent` payload; add a focused persistence/trace test that reloads the saved turn and asserts retrieved-only, resolved-only, cited-by-id, cited-by-url, and missing-citation cases are visible. Do not add a hard citation gate, DB migration, new runtime framework, or broad S1 rewrite.

## Non-Blocking Notes

- severity: P2
- target doc/code/test: `qa-reports/faq-kb-lineage-and-url-audit.md`, `scripts/build_knowledge_base.py`, `data/knowledge/knowledge_base_articles.json`
- blocks current sprint goal: no
- exact minimal fix, if any: None for Sprint 14. The CSV -> JSON -> ingest -> DB -> service -> tool chain is specifically documented, and a local recount matches the report's 218 total articles, 218 published, 180 with `source_url`, 38 missing canonical URLs, and 0 unsafe-to-show articles.

- severity: P2
- target doc/code/test: `server/src/main/java/com/gumtree/csagent/repository/KbChunkRepository.java:37`, `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java:203`, `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java:86`
- blocks current sprint goal: no
- exact minimal fix, if any: Optional hardening only: if `safe_to_show` is intended to include a non-blank body at search time, mirror the `ResolveArticleTool` body check in the published-only ANN SQL or post-fetch projection and add one stale-chunk test. Current corpus has 0 empty-body unsafe articles, and ingestion does not create chunks for blank bodies, so this is not a Sprint 14 blocker.

## Regression Risks

- severity: P2
- target doc/code/test: `mvn -pl server test -Dtest='Sprint14*'` and focused Sprint 6/7/8/10/11/12/13 regression command
- blocks current sprint goal: no
- exact minimal fix, if any: None. I reran both Maven suites locally: Sprint 14 focused tests passed 33/0/0/0, and the named regression guard suite passed 269/0/0/0, covering the existing FAQ S1 guard, Sprint 6 ReadTimeout no-retry closure tests, and focused cs014/cs066/cs095/cs002/cs029/cs176 guards.

- severity: P2
- target doc/code/test: `pytest eval_interactive/tests/`
- blocks current sprint goal: no
- exact minimal fix, if any: Re-run in the repo's intended Python environment if a validation-only closure needs the Python eval suite. The handoff records 294/0, but in this local shell `/Users/caoruixin/miniconda3/bin/pytest` exited with code 139 and `python3 -m pytest` had no pytest module, so I could not independently verify that line.

## Recommended Next Phase

Narrow Runtime Fix
