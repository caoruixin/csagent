## Sprint Review Decision
decision: pass
blocking_count: 0
summary: The cumulative §4.1 kernel walk across `6236941..d315323`, including the docs-only close package, passes with no semantic hardcode or cross-sprint ownership shift. Route-(b) attribution holds: R5, R6, and R2.a/R2.a#5-ext are correctly exonerated for the five regressed cases, and route-(c) is correctly ruled out by the HARD=0 safety audit and 0/36 anchor self-resolve result. The A6 change from a vacuous absolute 0.000 pass-rate floor to a scoped safety-of-pass invariant is a tightening of the safety floor because every unsafe, empty-handover, self-resolve, or superficial-tier2 pass remains a reject. Close route verdict: approve route-(b) accept-with-known-regression as recorded.
final_verdict: APPROVE_M_AUTO_6_WITH_NON_BLOCKING_OBSERVATIONS

### §1 Cumulative §4.1 kernel walk-through (across the milestone diff)

**Q1 — Does the milestone add a semantic keyword / regex / if-else / enum / per-UC matrix? No.** The cumulative range adds conditionals and two regexes, but none decides drift, UC selection, escalation posture, follow-up, risk classification, or response strategy. The relevant executable snippets are structural/runtime-owned:

```java
// ContextProjectionBuilder.java:473-478
if (IntakeFieldsRegistry.isIntakeUseCase(activeUc)) {
    for (String f : IntakeFieldsRegistry.requiredFieldsFor(activeUc)) {
        requiredForUc.add(f);
    }
}

// SkillGuardrailDispatcher.java:99-108
Pattern.compile("https?://\\S+");
Pattern.compile("\\bka[A-Za-z0-9]{16}\\b");

// KnowledgeSearchService.java:240-245
if (article != null && !article.isSearchKnowledgeEligible()) {
    return null;
}
```

R1/R4 project existing observable state; R2 counts structural cardinality and accurately labels an already-fired Runtime budget; R7 persists LLM-supplied fields; R5 enforces the existing grounding-floor citation-shape contract; R6/R8 consume curation data fields. No semantic decision matrix is replicated in the milestone code.

**Q2 — Tier-0 justification? N/A.** Q1 is no. No new Tier-0 invariant or escalation-reason enum value was introduced. Existing Runtime-owned safety, grounding, budget, capability, and persistence boundaries remain the justification for the deterministic checks.

**Q3 — Could a soft signal replace a hard semantic branch? Pass.** Where the LLM needs information, the milestone ships soft/structured surfaces: `required_intake_fields_for_active_uc`, `budgets.clarification`, `customer_context_status`, `ad_reference`, `display_citation`, and the LLM-invoked `update_intake_fields` tool. R2 label mapping, R5 citation presence, and R6 corpus eligibility remain Runtime-owned contract enforcement, not substitute semantic planning.

**Q4 — Visible-eval or trace-specific text encoded? No.** Independent grep found no CaseSpec IDs or regressed-case text in runtime/prompt/judge config, and zero flagged article IDs or `(temp)` literals under `server/src/main`. The two eligibility decisions live only in corpus data. The close package changes docs only.

**Q5 — Semantic ownership moved LLM → Java? No.** Java does not choose the UC, article relevance, escalation posture, or answer. The LLM chooses when and how to use projected state and `update_intake_fields`; Runtime only validates/persists structural contracts.

**Q6 — Prompt if-else added? No.** The only procedure wording change is the C-2a minimum-edit citation-token substitution:

```yaml
# resolve_faq_grounded_answer.yaml:30-31
... grounded customer-facing answer (with a display_citation citation ...)
... cite the display_citation token returned by resolve_article ...
```

No conditional prompt decision tree or semantic procedure rewrite was added.

**Q7 — Tool schema, capability boundary, PII/safety, and grounding floor preserved? Yes.** The new R7 tool is scoped to intake UCs through `tool-policy.yaml` and the intake skill; handover completeness remains guarded. R5 rejects null/empty/plain-English and accepts URL/article-ID shapes; all 218 corpus IDs match the article-ID predicate and 38 URL-less articles retain the fallback path. R6 filters search only; direct resolve remains available. Safety evidence is HARD=0 with no self-resolve.

**Q8 — Generalization coverage adequate? Yes.** Per-sub-sprint target/neighbor/negative coverage is preserved, both flagged R6 IDs have direct-resolve tests, R8 covers neighbor curation fields and no-re-embed/content-preservation negatives, and the milestone-shared run covers bad_cases, anchor_outcome, and shadow. Review-time focused verification passed 148 Java tests; UI verification passed 10 vitests plus production build.

**Q9 — Temporary hardcode requiring sunset? No.** No temporary semantic branch shipped. The follow-up ledger concerns known semantic/infra residuals, not downgrade of an interim hardcode.

**§4.1 aggregate verdict: `approve`.** Per-sub-sprint approvals remain valid; no cross-sprint interaction or close-package edit introduces semantic hardcode.

### §2 Per-sub-sprint verdict reconciliation

**A / S-Auto-23 — confirmed `APPROVE_S_AUTO_23 / blocking_count=0`.** R1/R2/R4 remain registry-driven, structural, and projection-first. No A observation becomes blocking cumulatively.

**B / S-Auto-24 — confirmed §7-EXEMPT, visual-verified.** UI display mappings and diagnostic logging do not alter runtime semantics. The UI informational-rejection allow-list affects display only and defaults unknown failures to blocking.

**C-1 / S-Auto-25 — confirmed `APPROVE_S_AUTO_25 / blocking_count=0`.** The Option-A capability-wiring waiver remains confined to registering/scoping a no-side-effect LLM-invoked tool; cumulative evidence shows adoption without capability drift.

**C-2a / S-Auto-26 — confirmed `APPROVE_S_AUTO_26 / blocking_count=0`.** The citation literal-to-shape rewrite preserves the grounding floor and URL-less fallback; no cumulative interaction turns it into semantic routing.

**C-2b / S-Auto-27 — confirmed `APPROVE_S_AUTO_27 / blocking_count=0`.** Both prior blockers remain closed. The filter is data-field-only, both flagged IDs remain directly resolvable, and forbidden runtime greps are clean.

**S-Auto-28 — confirmed `APPROVE_S_AUTO_28 / blocking_count=0` under the pure-infra exemption.** The generic reconcile path made R6 effective on the populated DB without content scanning, re-embedding, pruning, or changing plain `--ingest`.

No per-sub-sprint non-blocking observation becomes blocking under cumulative interaction.

### §3 Milestone-level focal-point verdicts

**F1 — PASS: grounding floor preserved.** R5 replaces the false-positive-prone literal `source_id` substring check with structural URL/article-ID presence while retaining reject behavior for null, empty, plain-English, and literal-field-name-only replies. R6 removes only explicitly ineligible search candidates and does not weaken resolve/published guards.

**F2 — PASS: anti-误杀 floor preserved under safety-of-pass.** The close evidence records HARD=0 across 36 anchor attempts plus 18 cs38s* attempts and 0/36 anchor self-resolve. Spot-sampled representative traces for uc_g/uc_h/uc_i/uc_j and cs38s01/cs38s02 all end `containment=escalated`; PASS representatives show genuine intake plus `update_intake_fields`, `request_handover`, and controlled case creation where applicable. No unsafe PASS was found.

**F3 — PASS: no cross-sub-sprint semantic hardcode.** R1/R4/R7 combine into structured intake/entity observability; R5/R6/R8 combine into citation/corpus contract plumbing. Neither combination creates a Java-owned semantic choice. The three close-decision commits are docs-only and add no executable hardcode.

**F4 — PASS: capability-wiring fence waiver remains bounded.** `update_intake_fields` is LLM-invoked, free-form, no-side-effect beyond persistence, scoped to intake UCs, and cannot bypass the separate `request_handover` completeness guard.

**F5 — PASS: citation contract preserves grounding and URL-less articles.** Corpus audit confirms 38 URL-less articles and 218/218 IDs matching `^ka[A-Za-z0-9]{16}$`; `display_citation` falls back to article ID. All six resolved articles cited in Cluster 1 are URL-bearing, so R5's fallback path was not exercised and cannot explain the resolve-vs-escalate regression.

**F6 — PASS: corpus eligibility + reconcile are data-field-driven.** Runtime contains no flagged ID, title-keyword, or content-scan predicate. Exactly two corpus rows are `search_knowledge_eligible=false`, both remain published, direct resolve is test-pinned for both, and §0.3 records `articlesReconciled=2` with DB state 2 false / 216 true.

**F7 — PASS: A6 is a tightening, not a weakening.** The pre-reframe rule rejected any movement from a 0.000 floor without inspecting whether the movement was safe; it could therefore reward a vacuous failure-to-complete-intake artifact. The scoped post-reframe rule rejects every unsafe, empty-handover, self-resolve, fabricated-field, or superficial-tier2 pass while permitting only genuine intake followed by safe escalation. Scope remains limited to uc_g/uc_h/uc_i/uc_j plus cs38s01/cs38s02; all other cases retain prior treatment.

**F8 — PASS: route-(b) attribution holds.** R5 is exonerated because all six Cluster-1 resolved articles are URL-bearing; R6 is exonerated because searches returned non-empty hits; R2.a/R2.a#5-ext are exonerated for Cluster 2 because `cs32s02` attempts 3-6 and `cs015` attempt 4 are explicitly invalid `infra_error` sessions where turn-flow never executed. Cluster 1 is the pre-existing resolve-vs-escalate semantic failure shape; Clusters 2/3 are session-start infra flake. No clean M-Auto-6 code regression was isolated, and route-(c) is ruled out by the safety audit.

### §4 Blocking findings

None.

### §5 Non-blocking observations

1. **R5 citation validator remains presence-only by contract.** Any structural URL/article ID can satisfy it; it does not prove the cited token came from the selected `resolve_article` result. This is consistent with the existing bounded grounding-floor authorization and does not explain the known regressions, but it remains a future grounding-strengthening opportunity rather than a semantic-hardcode fix.

2. **R2 DISCOVER cardinality follow-up remains valid.** The route-(b) ledger notes minimal/placeholder DISCOVER replies can contribute to premature clarification-budget exits. Keep the queued CS3 fix at `infra` plus soft projection guidance; do not add content/keyword-based clarification detection.

3. **Known close follow-ups remain correctly non-blocking.** Preserve the UC-FP entity-premise/resolve-vs-escalate OQ, empty-trace/session-start infra brief, standalone `--reconcile` entry-gate test, stale FAQ-grounding-doc reconciliation, and baseline-pointer decision in the M-Auto-7/deliver ledger.

4. **Dirty-tree waiver expanded at review time.** In addition to the dispatch-listed files, `compact/M-Auto-6-review-prompt.md` was modified and `compact/baseline-flip-m-auto-6-proposed.md` was untracked. Both remain outside runtime/eval/baseline-pointer review surfaces and do not affect this verdict.

5. **Independent verification.** Focused Java review suite: `148 / 0 / 0 / 0`. UI: `10 passed / 0 failed / 0 skipped`; `npm run build` succeeded. `git diff --check 6236941..d315323` reports only pre-existing whitespace in compact/sprint-archive artifacts, not production code or this verdict.
