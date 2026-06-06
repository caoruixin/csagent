## Sprint Review Decision
decision: out_of_scope_review
blocking_count: 1
final_verdict: REJECT_S_AUTO_26
summary: The intended four-commit range `d4122c0^..5a0ab3d` substantively passes the anti-hardcode kernel and all five C-2a focal checks: the citation field is additive, the guardrail now validates structural URL/article-ID shapes while preserving its bounded resolve-only floor, the YAML edit is citation-only, and both fence expansions are mechanical test maintenance. However, the review contract requires a clean working tree at HEAD, and pre-review `git status --short` reported the unrelated untracked file `compact/context-pack-governance-framework-extraction-2026-06-06.md`; therefore this review must return `out_of_scope_review` and cannot issue `APPROVE_S_AUTO_26` until the cleanliness gate is restored and rechecked.

## §1 Per-Change Verdicts

**#1 — R5 #1 additive `display_citation`: pass.** Commit `d4122c0` adds only `display_citation`, selecting the already-computed non-blank `source_url` or falling back to `article_id`, while existing `source_id`, `article_id`, `source_url`, and `canonical_url` fields remain present (`ResolveArticleTool.java:84-111`). Focused tests pin URL preference, null/blank fallback, and additive preservation (`ResolveArticleToolTest.java:161-224`).

**#2 — R5 #2 structural `must_cite_source` rewrite: pass.** Commit `7b0665e` fully removes the `cite_token_field` extraction and literal `contains(citeToken)` check. It adds private static final URL/article-ID patterns (`SkillGuardrailDispatcher.java:97-108`), preserves the outcome-class bounds (`:358-374`), combines both shapes through the null-safe helper (`:375-411`), and records `cite_token_validator=structural_url_or_article_id` on rejection (`:382-397`). No other handler changed in the cumulative diff.

**#3 — R5 #3 skill YAML wording: pass.** Commit `1624a7a` changes only the three citation references and removes `cite_token_field`; objective, escalation policy, search-first paragraph structure, and FAQ-miss rule remain otherwise intact (`resolve_faq_grounded_answer.yaml:29-43`, `:74-83`). The commit diff is the claimed minimum edit: 3 insertions / 4 deletions in the YAML.

**Fence expansion #1 — `ResolveFaqGuardrailsTest`: pass.** Commit `7b0665e` replaces only the two legacy `kb-001` user-message fixtures with the real corpus ID constant `ka41r000000LIEEAA4`; guardrail scenarios and assertion shapes are unchanged (`ResolveFaqGuardrailsTest.java:128-137`, `:164-171`; corpus anchor `knowledge_base_articles.json:291`).

**Fence expansion #2 — `PhaseEvaluatorResolveSkillIntegrationTest`: pass, accepted non-blocking expansion.** Commit `1624a7a` changes only `FAQ_PROCEDURE` and `FAQ_GROUNDING` string fragments to mirror the new YAML text (`PhaseEvaluatorResolveSkillIntegrationTest.java:105-135`). No test logic, scenario, rubric, or assertion shape changed. The focused integration test passed.

**Golden-set / forbidden-surface churn: none.** `git diff --name-status d4122c0^..5a0ab3d` contains exactly the eight claimed files; no other YAML, `eval_interactive/`, autoloop scoring set, baseline, corpus, UI, or C-2b file changed. The range contains exactly commits `d4122c0`, `7b0665e`, `1624a7a`, and `5a0ab3d`.

## §2 Nine-Question Kernel Walkthrough

**Q1 — Semantic decision hardcode? Pass.** `display_citation` is derived from structured article columns (`ResolveArticleTool.java:84-111`), and the guardrail patterns validate transport/identifier shapes rather than use case, next action, response wording, or content keywords (`SkillGuardrailDispatcher.java:97-108`, `:407-411`). The YAML points at the returned token without a per-UC matrix (`resolve_faq_grounded_answer.yaml:30-31`, `:79`).

**Q2 — Tier-0 invariant justification? Pass / no new Tier-0.** The existing resolve-only guard remains bounded by normalized `resolve`/`resolved` plus YAML `outcome_class` agreement (`SkillGuardrailDispatcher.java:358-374`). The replacement predicate changes only citation-token presence validation (`:375-411`); it adds no routing, escalation, safety, or semantic branch.

**Q3 — Could soft-signal projection suffice? Pass.** R5 #1 supplies the LLM-facing soft signal directly (`ResolveArticleTool.java:107-111`), and R5 #3 instructs use of it (`resolve_faq_grounded_answer.yaml:30-31`). R5 #2 remains the already-existing Runtime grounding-floor enforcement point and only corrects its literal-substring false negative (`SkillGuardrailDispatcher.java:375-397`).

**Q4 — Eval/case text encoded? Pass.** The patterns are generic URL and Salesforce-ID shapes (`SkillGuardrailDispatcher.java:97-108`), not CaseSpec text or a user-message keyword. Corpus samples at `knowledge_base_articles.json:15`, `:34`, `:53`, `:74`, `:95`, `:116`, `:135`, `:154`, `:173`, and `:192` all have the same structural ID shape.

**Q5 — Semantic ownership moved LLM → Java? Pass.** Java exposes a preferred citation token and checks only token shape (`ResolveArticleTool.java:107-111`; `SkillGuardrailDispatcher.java:407-411`). It does not choose the article, decide when to cite, judge relevance/correctness, or compose customer-facing text (`SkillGuardrailDispatcher.java:344-356`).

**Q6 — Prompt if-else instead of principles? Pass.** The YAML tells the LLM to cite the `display_citation` token returned by `resolve_article`; it does not instruct the LLM to inspect fields and implement fallback logic itself (`resolve_faq_grounded_answer.yaml:30-31`). The parenthetical description explains the tool-field contract and is mirrored byte-for-byte in the golden constants (`PhaseEvaluatorResolveSkillIntegrationTest.java:105-135`).

**Q7 — Tool schema / safety / grounding floors preserved? Pass.** Existing tool-result fields are preserved (`ResolveArticleTool.java:95-115`). Empty, null, plain-English, and literal `source_id`-only replies reject, while URL and real article-ID shapes pass (`SkillGuardrailDispatcherTest.java:278-373`). Outcome-class and skill-scope boundaries remain covered (`:375-448`).

**Q8 — Generalization eval coverage? Pass for wiring evidence.** Target/neighbor/negative coverage includes URL preference and URL-less fallback (`ResolveArticleToolTest.java:161-224`), HTTPS/HTTP/article-ID positives (`SkillGuardrailDispatcherTest.java:297-330`), empty/null/plain-English/literal-field-name negatives (`:278-290`, `:332-373`), and outcome/scope boundaries (`:375-448`). Shadow/outcome evidence remains correctly deferred per handoff `5a0ab3d`.

**Q9 — Temporary hardcode / sunset plan? Pass / not applicable.** The URL and article-ID shapes are durable structural validators (`SkillGuardrailDispatcher.java:97-108`), `display_citation` is a durable tool-result contract (`ResolveArticleTool.java:107-111`), and `cite_token_field` is permanently removed from the live YAML (`resolve_faq_grounded_answer.yaml:40-43`).

**§4.1 aggregate verdict:** `approve` for the audited commit range. Sprint-close approval is withheld solely by the dirty-working-tree gate.

## §3 Five Focal-Point Verdicts

**F1 — Literal → structural-shape semantics shift: pass.** The old extraction block and literal substring check are absent. The shipped private static final patterns are `https?://\S+` and `\bka[A-Za-z0-9]{16}\b` (`SkillGuardrailDispatcher.java:97-108`); the helper joins them with OR semantics and is null-safe (`:407-411`). Rejections use the renamed trace key/value and updated hint (`:382-397`).

**F2 — Grounding-floor preservation: pass.** `mustCiteSource_fires_when_classResolve_andNoSourceId`, `mustCiteSource_fires_when_emptyUserMessage`, `mustCiteSource_fires_when_nullUserMessage`, and `mustCiteSource_fires_when_literalSourceIdWordButNoStructuralToken` reject (`SkillGuardrailDispatcherTest.java:278-290`, `:332-373`). `mustCiteSource_doesNotFire_when_articleIdShapePresent`, `...httpsUrlPresent`, and `...httpUrlPresent` pass (`:297-330`). Focused review run: `67` tests, `0` failures/errors/skips.

**F3 — Article-ID regex derived from real corpus: pass.** Review-time corpus audit found `218/218` IDs match `^ka[A-Za-z0-9]{16}$`; ten anchored samples appear at `knowledge_base_articles.json:15-192`. The exact prefix plus 16 alphanumerics rejects ordinary plain English, pinned by the literal-`source_id` negative (`SkillGuardrailDispatcherTest.java:358-373`). Handoff audit (c) records the full-corpus derivation in commit `5a0ab3d`, and expansion #1 uses a real corpus ID (`ResolveFaqGuardrailsTest.java:128-137`, `:164-171`; corpus `:291`).

**F4 — Skill YAML minimum-edit + clean removal: pass.** Commit `1624a7a` changes the citation references at YAML lines `30`, `31`, and `79`, and removes only `cite_token_field` from the guardrail parameters (`resolve_faq_grounded_answer.yaml:29-43`, `:74-83`). Repository search finds no live main/test consumer of `cite_token_field`; the only live trace key is the replacement at `SkillGuardrailDispatcher.java:388`.

**F5 — Fence expansion #2 scope call: pass / non-blocking.** The `1624a7a` diff changes only the two static golden strings (`PhaseEvaluatorResolveSkillIntegrationTest.java:105-135`) to mirror YAML lines `30-31`; no assertion, scenario, or test method changed. `PhaseEvaluatorResolveSkillIntegrationTest` passed all `14` focused tests.

## §4 Blocking Findings

1. **`out_of_scope_review` — working tree was not clean before review.** At HEAD `6e55d79`, pre-review and pre-write `git status --short` reported `?? compact/context-pack-governance-framework-extraction-2026-06-06.md`. The review contract explicitly requires a clean HEAD and requires an `out_of_scope_review` finding when uncommitted changes exist. Layer classification: `infra` / review-environment hygiene. Corrective direction: place that unrelated file into its intended committed or removed state, then rerun the clean-tree check and reissue the sprint-close decision; do not change the audited C-2a range.

## §5 Non-Blocking Observations

1. **Expansion #2 pre-fix audit gap.** The change is byte-mechanical and acceptable, but future YAML wording audits should search test golden constants as well as main-source consumers; commit `1624a7a` demonstrates that `PhaseEvaluatorResolveSkillIntegrationTest.java:105-135` is a load-bearing mirror.

2. **Current FAQ grounding doc is historically stale relative to the existing Sprint-39 gate.** `docs/current/faq_grounding_contract.md:8-10` and `:92-99` still describe citation handling as non-blocking, while the pre-existing `must_cite_source` Runtime rejection path is live at `SkillGuardrailDispatcher.java:358-397`. C-2a did not introduce this drift and should not fold it into this review-only sub-sprint, but a later docs-reconciliation task should clarify the bounded resolve-only exception.

3. **Focused verification passed.** Review-time command `mvn -o -Dtest=ResolveArticleToolTest,SkillGuardrailDispatcherTest,ResolveFaqGuardrailsTest,PhaseEvaluatorResolveSkillIntegrationTest test` completed with `67 / 0 / 0 / 0`; `git diff --check d4122c0^..5a0ab3d` was clean.
