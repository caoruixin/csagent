## Sprint Review Decision
decision: pass
blocking_count: 0
summary: S-Auto-28 is approved under the §4.1 pure-infra scope exemption. The cumulative range `ba3defa^..78ae614` adds no semantic hardcode, Tier-0 invariant, LLM-vs-Java ownership shift, prompt branch, tool-schema change, or forbidden R6-surface edit. R8 #1-#6 and focal points F1-F6 pass: reconcile is metadata-only, plain `--ingest` preserves skip-existing behavior, post-APPROVE evidence is explicitly produced by the standalone `--reconcile` run rather than manual SQL, direct resolve remains available, independent forbidden-grep/blob-hash checks are clean, and no prune path exists.
final_verdict: APPROVE_S_AUTO_28

## §1 Per-Change Verdicts

**R8 #1 — reconcile method: pass.** `reconcileExisting(...)` reads desired values and mutates only `searchKnowledgeEligible`, `isPublished`, `ucTags`, and the row timestamp before saving the existing entity (`KnowledgeIngestionRunner.java:216-260`). No content-field setter, chunking call, embedding call, or chunk-repository write exists in this method. Target and neighbor coverage is pinned by `reconcile_flipsSearchKnowledgeEligible_trueToFalse_onExistingRow`, `reconcile_appliesIsPublishedChange_onExistingRow`, and `reconcile_appliesUcTagsChange_onExistingRow` (`KnowledgeIngestionReconcileTest.java:75-153`).

**R8 #2 — `--reconcile` gate and branch split: pass.** The entry gate requires `--ingest` or `--reconcile`, with reconcile selecting reconcile mode (`KnowledgeIngestionRunner.java:78-92`). Existing rows reconcile only when the mode boolean is true; plain ingest skips them; new rows take the existing insert path under either mode (`:144-191`). `reconcile_newId_takesInsertPath_andEmbeds` and `plainIngest_existingRow_skippedAndUntouched_noWriteNoEmbed` pin the branch split (`KnowledgeIngestionReconcileTest.java:256-315`).

**R8 #3 — reconcile observability: pass.** Reconciled rows emit INFO with `article_id` and old/new field values (`KnowledgeIngestionRunner.java:256-260`); reconcile runs emit `articlesReconciled`, `articlesInsertedNew`, and `articlesUnchanged` at INFO (`:114-124`). `processArticles_reconcile_countsChangedVsUnchangedVsInserted` pins the three counters (`KnowledgeIngestionReconcileTest.java:317-355`).

**R8 #4 — R6 wiring validation: pass.** The nine reconcile/negative tests cover target, neighbor, and negative behavior (`KnowledgeIngestionReconcileTest.java:75-355`). `reconcileThenSearch_excludesBothTemplates_butDirectResolveStillReturnsThem` flips both flagged test entities, excludes both from search, and successfully resolves both by direct ID (`KnowledgeReconcileEndToEndTest.java:69-116`). Review-time focused verification passed `31 / 0 / 0 / 0`.

**R8 #5 — runbook accuracy: pass.** The docs-only commit corrects port-based backend detection, DB name, smoke commands, full re-bless invocation, and the §0.3/A3 insert-only-runner root-cause/remediation guidance (`docs/current/process/preflight-eval-checks.md:80-135`, `:165-199`, `:220-231`, `:331-359`).

**R8 #6 — dev handoff: pass.** The handoff records implementation, mock-interaction evidence, test counts, fence confirmations, §7 stanza, commit map, and an unambiguous post-APPROVE real-DB sequence (`docs/sprints/sprint-083-handoff.md:38-79`, `:166-269`, `:272-354`).

## §2 §4.1 Nine-Question Kernel Walk-Through

**Scope exemption and aggregate verdict: `approve`.** The cumulative executable change is pure `infra`: a persistence/data-application mode in `KnowledgeIngestionRunner`. The remaining changes are tests, a runbook, and a handoff. No semantic decision surface is changed.

**Q1 — semantic keyword/regex/if-else/enum/matrix? No.** The new branch distinguishes explicit ingestion modes and row existence (`KnowledgeIngestionRunner.java:161-178`); it does not decide drift, escalation, UC, risk, follow-up, or routing.

**Q2 — Tier-0 justification? N/A.** Q1 is no, and the diff adds no Tier-0 invariant.

**Q3 — soft-signal alternative? N/A.** Applying committed curation metadata to persistence is an infra operation, not an LLM semantic choice.

**Q4 — visible-eval or trace-specific text in runtime? No.** Independent grep found zero flagged article IDs and zero `(temp)` literals in `server/src/main`; the production path iterates generic JSON articles.

**Q5 — LLM ownership moved to Java? No.** The change only syncs persistence fields; it adds no semantic selection or response decision.

**Q6 — prompt if-else? No.** No prompt or skill file is in the audited range.

**Q7 — schema/capability/safety/grounding preserved? Yes.** No tool schema, permission, PII/safety, grounding, search-service, or direct-resolve production file changed.

**Q8 — generalization coverage? Yes for this infra change.** Target eligibility flip, neighbor curation fields, and negative controls for no embed/chunk write, content preservation, no-op, new insert, and plain ingest are covered (`KnowledgeIngestionReconcileTest.java:75-355`). The mock-level search/direct-resolve wiring case passes (`KnowledgeReconcileEndToEndTest.java:69-116`). Shadow is not applicable to the infra wiring change; real-DB and real-LLM outcome evidence remains in the post-APPROVE milestone-close sequence.

**Q9 — temporary branch requiring sunset? No.** The reconcile mode is a durable generic data-application mechanism.

## §3 Six Focal-Point Verdicts

**F1 — metadata-only: pass.** `reconcileExisting(...)` contains no `embeddingClient`, `kbChunkRepository`, or content setter call (`KnowledgeIngestionRunner.java:216-260`). `reconcile_doesNotReEmbedOrWriteChunks_onExistingRow` verifies `embedBatch`, `embed`, `saveAll`, and `save` are never called on the embedding/chunk mocks (`KnowledgeIngestionReconcileTest.java:157-180`). The target flip also verifies `embedBatch` and `saveAll` never run (`:75-102`), while `reconcile_newId_takesInsertPath_andEmbeds` verifies exactly one `embedBatch`, `saveAll`, and article `save` for a new ID (`:256-283`).

**F2 — plain `--ingest` unchanged: pass.** The entry widening preserves ingest mode when reconcile is absent (`KnowledgeIngestionRunner.java:78-92`); existing rows in plain ingest increment unchanged and continue without writes (`:161-174`). `plainIngest_existingRow_skippedAndUntouched_noWriteNoEmbed` verifies no article save, no embed batch, no chunk saveAll, and no curation flip (`KnowledgeIngestionReconcileTest.java:285-315`).

**F3 — no manual SQL evidence: pass.** The handoff marks real-DB §0.3 evidence pending post-Codex and specifies `mvn -o -pl server spring-boot:run -Dspring-boot.run.arguments=--reconcile` before the read-only psql checks; it explicitly forbids manual SQL `UPDATE` as evidence (`docs/sprints/sprint-083-handoff.md:246-269`).

**F4 — direct resolve invariant: pass.** After both entities reconcile to ineligible and are excluded from search, direct `ResolveArticleTool` calls for both flagged IDs return success (`KnowledgeReconcileEndToEndTest.java:76-115`). `ResolveArticleTool.java` is absent from the cumulative diff.

**F5 — forbidden grep and R6 surface unchanged: pass.** Review-time `git grep` at `78ae614` returned zero flagged-ID or `(temp)` matches under `server/src/main`. Before/after blob hashes are identical for `KnowledgeSearchService.java`, `KbArticle.java`, V17, `knowledge_base_articles.json`, `docs/current_eval_baseline.md`, and `autoloop/config.yaml`. The audited name-status list contains exactly the five claimed in-fence files.

**F6 — no prune: pass.** `processArticles(...)` iterates only the JSON article list and reconciles, skips, or inserts each item (`KnowledgeIngestionRunner.java:144-191`). No `kbArticleRepository.delete*` call exists in the runner or added diff.

## §4 Blocking Findings

None.

## §5 Non-Blocking Observations

1. **Standalone entry-gate test gap.** The tests call `processArticles(..., true)` and prove reconcile branch behavior, but no test directly supplies standalone `--reconcile` through `ApplicationArguments` to `run()`. The gate implementation is correct by inspection (`KnowledgeIngestionRunner.java:78-92`), and the mandatory post-APPROVE live command will verify it before milestone close. A future infra-test change should pin standalone `--reconcile` at the runner entrypoint so a gate regression fails before the shared DB step.

2. **Post-APPROVE evidence assertions.** Preserve the handoff's required sequence and capture the reconcile INFO summary plus read-only §0.3 results showing `articlesReconciled=2`, `articlesInsertedNew=0`, `articlesUnchanged=216`, both flagged rows false, and the remaining 216 true. Do not use a manual SQL `UPDATE`.

3. **Independent review verification.** `mvn -o -pl server -Dtest=KnowledgeIngestionReconcileTest,KnowledgeReconcileEndToEndTest,KnowledgeIngestionRunnerTest,KnowledgeSearchServiceTest,ResolveArticleToolTest test` passed `31 / 0 / 0 / 0`; `git diff --check ba3defa^..78ae614` was clean. Review HEAD was `feb3419`; descendants after `78ae614` touch only the acknowledged compact framework/review-prompt artifacts and are outside the audited range.
