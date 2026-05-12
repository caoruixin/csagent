# Failure Brief — cs015 UC-FP mis-route and premature escalate

> **Source case:** `cs_interactive_015` (UC-FP primary, secondary UC-B / UC-K, expected `resolve`, `should_escalate: false`).
> **Source session:** `570Q5000008WmXxIAK`.
> **CaseSpec:** `eval_interactive/case_specs/smoke/cs_interactive_015.yaml`.
> **Approved override:** `eval_interactive/case_spec_overrides.yaml` lines 53–98.
> **Runs:** `20260505-235231` (FAIL, composite=0.0, `active_use_case=UC-A`) and `20260510-134558` (FAIL, composite=0.0, `active_use_case=UC-B`).
> **Filed:** 2026-05-12 (Sprint 18 / G1, brief #1).

## Ground-truth chain

The cs_015 CaseSpec is *not* a raw auto-generation; it has been semantically re-reviewed and is anchored by an approved override:

- **Wave A6 semantic re-review**, 2026-04-30, reviewer = human semantic review, confidence = high, supporting_turn_numbers = [5, 6, 7, 8, 9, 11, 13] (`eval_interactive/case_spec_overrides.yaml` lines 53–98).
- Override pins `primary_uc=UC-FP`, `secondary_ucs=[UC-B, UC-K]`, `outcome_class=resolve`, `should_escalate=false`, `expected_tool_sequence=[get_customer_context, search_knowledge, resolve_article, record_outcome]`, with `bot_handling_pattern` requiring the two-stage retrieve-context-then-escalate-only-on-trigger flow.
- The override's rationale explicitly classifies the source-transcript late escalation (turn 11) as **failure-path evidence, not golden behavior**. The CaseSpec generation pipeline upgrade is documented in `docs/proposals/interactive_case_spec_generation_plan.md` (Wave A5 / A6 / A6.6) and the human-reviewer step is the L3 stage of that pipeline.

This brief therefore measures the bot's actual behaviour against a **human-reviewed, approved ground truth**, not against a raw rule-extracted expectation. Layer classification below excludes `eval_spec` for the cs_015 outcome failure: §3.2 Q6 (eval CaseSpec asking the system to do something it cannot / should not) is already disproved by the Wave A6 review.

## What happened?

User opened the chat from the Ad Support form with description "Hi - can you tell me what happened to my ad?" (no `ad_id` supplied). The ground-truth context is that the ad ("Male Massage and Waxing, by a Male") was correctly removed under policy for "men only" wording and visible body parts in the image — a canonical UC-FP-01 (Correct Deletion / Compliance Takedown Explanation) case where the bot is expected to retrieve customer/listing/moderation context, explain the policy reason, and clarify whether the user can edit/repost.

In both eval runs the bot mis-routed (`active_use_case=UC-A` on 2026-05-05; `active_use_case=UC-B` on 2026-05-10), never called `get_customer_context`, ran `search_knowledge` + 3× `resolve_article` against an FAQ corpus that does not carry the per-ad takedown reason, and template-escalated on turn 0 with "Hi Adam! I'm having difficulty resolving this. Let me connect you with a specialist." The follow-up turn ("ok so how do I change it?") — the canonical UC-FP edit-and-repost signal — was met only with the standard handover message. Outcome was `escalate` (expected `resolve`); composite score 0.0 in both runs.

## What should a good CS agent have done?

Follow the two-stage flow encoded in the CaseSpec `bot_handling_pattern` and reinforced by the override rationale and UC-FP-01 phase 2 strategy:

1. *Stage 1 — explain.* Recognize "what happened to my ad?" as UC-FP. Call `get_customer_context` (and the UC-FP-specific `get_moderation_review_context` if available) to retrieve the actual takedown reason. Combine that with a `search_knowledge` / `resolve_article` lookup against community standards / ad policy / reposting guide. Answer the user with a concrete reason ("your ad was removed because the wording targeted a single gender and the image contained visible body parts — these violate community standards X / Y") plus the edit/repost path.
2. *Stage 2 — escalate only on the listed triggers.* `user_requests_human`, `faq_miss_ge_2`, `clarification_budget_exhausted`, `user_requests_appeal_or_review`, `user_expresses_strong_emotion`, or `issue_classified_as_incorrect_deletion` (phase 2 §UC-FP-01 escalation_conditions). The seed message "ok so how do I change it?" is an *edit-and-repost* request and should keep the case in resolve, not flip it to escalate.

Even if the takedown reason cannot be safely surfaced (e.g. internal TMX detail), the bot should explain *what category of reason* applied and the standard reposting guidance — that is itself a resolve, per phase 2 §UC-FP-01 forbidden-behaviors list which carves out only internal moderation tooling screenshots, not the policy explanation.

## Why does this matter?

cs_015 is high-volume territory: UC-FP-01 accounts for ~17.3% of cases per phase 2 (case_volume 20,875), the highest single-UC share. A bot that mis-routes the canonical "why was my ad deleted?" question to UC-A (listing visibility) or UC-B (posting guidance) and then template-escalates is failing the highest-traffic UC on the busiest entry point ("Ad Support" form). This is a containment-rate failure and a customer-experience failure simultaneously: the user gets no answer to a question the system is explicitly designed to answer, then sits in a human-agent queue for a question a script could have closed.

This case is also the cleanest test of the Constitution's §1.2 LLM-owns clause (use case hypothesis, response strategy): the projection at session start (form_subject "Ad Support" + description "Hi - can you tell me what happened to my ad?") contains enough signal for the LLM to anchor on UC-FP. If the system is choosing UC-A or UC-B instead, the planner-side or projection-side disambiguation is broken.

## Is this a one-off or a pattern?

`pattern`. Two failure modes are layered here:

1. *Mis-routing on `Ad Support` form when description is "ad deleted/removed/taken down" shape* — confirmed across both smoke runs for cs_015 (UC-A on 05-05, UC-B on 05-10). The `active_use_case` is unstable across runs, indicating no anchored UC hypothesis. UC-FP being the largest single-UC share by case_volume means the population of similar real cases is large.

2. *Template-escalate on turn 0 when FAQ search returns no specific resolve-grade hit* — same surface as cs_001 / cs_002 / cs_011 / cs_014 / cs_038 / cs_040 (Cluster B briefs, separately filed). The bot never tries to assemble customer / moderation context as an alternative grounding source before declaring `faq_miss_threshold_exceeded`. This is its own pattern documented in those briefs; cs_015 is one of the cases where that pattern produces an *outcome* failure (resolve → escalate) rather than only an L3 quality failure.

## Which layer is likely responsible?

Two candidate layers; §3.2 first-match-wins resolves to one but both may be live:

- **`prompt_projection`** (primary). The projection at session start (form_subject "Ad Support" + description "Hi - can you tell me what happened to my ad?" + empty `ad_id`) likely does not surface a strong-prior signal for UC-FP. The Sprint 10 §L1 reroute and the UC-FP anchoring may be unwired for empty-ad_id deletion-question shape, or weighted lower than UC-A / UC-B. The mis-route landing on UC-A in one run and UC-B in another (different wrong UCs across runs) is the tell — the LLM is choosing among options that look comparable because the disambiguation signal is too soft.

- **`semantic_planner`** (secondary). Even with adequate projection, the LLM could be defaulting to UC-A / UC-B because they read as "general ad question" instead of "compliance-takedown explanation". If projection turns out adequate after inspecting turn-0 `projected_context`, this becomes the primary candidate.

`eval_spec` is **ruled out** by the Ground-truth chain above. `product_policy` does not apply to the cs_015 failure itself (see Related observation for the separate UC-B / `get_customer_context` policy question).

## What should NOT be done?

- Do **not** add a regex / keyword on "what happened to my ad?" / "why was my ad deleted?" / "ad removed" to force-route to UC-FP in `RuntimeIntentClassifier` or `DriftDetector`. That is the canonical keyword-chatbot regression the Constitution's §1.5 Iteration rule forbids. The right move is a soft signal in the projection (e.g. surface a "deletion/takedown" hint derived from form-subject + description semantics) for the LLM to weigh.

- Do **not** edit cs_015's CaseSpec or override to lower the bar (e.g. accept `escalate` as a passing outcome). The override is *the* approved Wave A6 ground truth; loosening it to make the bot pass would mask a genuine bot bug per `iteration_governance.md` §5 eval-override rule.

- Do **not** add a new `escalation_reason` enum value (e.g. `ad_deletion_unknown_reason_fallback`) to legitimize the current template-escalate-on-faq-miss surface. The enum is frozen per Sprint 13 / runtime freeze policy.

- Do **not** lift the phase 2 `get_customer_context` restriction on UC-B *as part of cs_015 work*. That restriction is its own product-policy decision (see Related observation) and belongs in a separate review, not bundled into the cs_015 fix.

- Do **not** add a Java guard forcing `get_customer_context` to be called before `request_handover` on UC-FP. It would break legitimate fast-escalation paths (e.g. user-requested human on turn 0, distress signal).

## Related observation — separate phase 2 policy question

Reviewing cs_015 surfaced a tangential phase 2 §2.10 tool-policy question worth tracking on its own: `get_customer_context` is restricted to UC-A / UC-FP / UC-K (phase 2 line 358), excluding UC-B (Posting & Editing Guidance). When cs_015's bot mis-routes to UC-B (as in the 2026-05-10 run), this restriction removes the bot's path to ad-specific context — but the restriction is **not** what causes cs_015's failure: the failure starts at the UC mis-route, which happens *before* tool policy applies. The phase 2 design intent for this restriction (privacy / scope minimization?) is unclear and may be under-scoped: UC-B posting-and-editing guidance often benefits from knowing the user's actual ad state. This is its own `product_policy` decision and **belongs in a separate brief / product review**, tracked in `docs/action_bank.md` as `R-uc-b-customer-context-policy-review`. It is not in cs_015's remediation scope.

Phase 2 line 592 also defines a separate `get_moderation_review_context` tool (runtime_only, gumshoe, v0.2 addition). It is **not** subsumed by `get_customer_context`; the two retrieve different data. Both being unavailable to UC-B compounds the recovery gap on a UC-B mis-route, but again this is a phase 2 question, not a cs_015 question.
