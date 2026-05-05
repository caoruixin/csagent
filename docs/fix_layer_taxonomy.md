# Fix Layer Taxonomy — Sprint 5 (F0)

Date: 2026-05-05
Sprint: Prompt / Context Projection and Fix-Layer Diagnostic Sprint 5
Status: diagnostic — no code, prompt, spec, or runtime changes proposed in this file beyond classification

## Purpose

Classify the remaining post-Sprint-4 smoke failures into the correct primary
fix layer so the next implementation sprint stops defaulting to a Java guard
patch when the actual root cause is a prompt / context surface gap, a
skill-orchestration gap, an infra/runtime gap, a case-spec gap, or an
upstream judge volatility issue.

## Layers

| Layer | Definition |
|---|---|
| `java_guard` | A deterministic invariant the Java runtime MUST guarantee (e.g. resolver precedence, override-pipeline integrity, escalation_reason_consistency). Implemented in `ControlKernel`, `EscalationReasonResolver`, `UseCaseRouter`, `PhaseEvaluator`, `HandoverPayloadAssembler`, `SessionManager`. |
| `prompt_context_projection` | The runtime is permissive enough but the LLM lacks state, prior, phase goal, allowed-tool cue, grounding cue, or handover cue in the projected JSON / system prompt. Fix lives in `system_prompt.txt`, `routing_prompt.txt`, `ContextProjectionBuilder.buildProjection`, or `PhaseEvaluator.plan().systemInstruction/groundingInstruction/escalationPolicy`. |
| `skill_orchestration` | A repeated multi-tool / multi-step flow that the LLM has to re-derive every turn. Fix is to introduce a deterministic plan template or skill (FAQ-grounded-answer, soft-OOS-clarify-then-decide, intake-collect-and-handover) so the LLM stops re-inventing the orchestration. |
| `case_spec_eval` | The CaseSpec, override pipeline, judge dimension, or scoring dimension is the gap. The runtime behaviour is acceptable; the spec / scoring expects something else. Fix lives in `eval_interactive/case_spec_overrides.yaml`, the smoke curator, or `eval_interactive/case_specs/...`. |
| `infra_runtime` | The runtime cannot complete a session for non-semantic reasons — Kimi `session_create_failed: ReadTimeout`, transport flake, port collision, persistence error. Fix lives in eval client timeout config, Kimi client retry / pre-warm, async pre-fetch, or DB pool config. |
| `judge_calibration` | The L3 LLM judge (`relevance`, `tone_appropriateness`, sometimes `groundedness`) flips on identical bot output. Fix lives in judge prompt, judge model, judge ensembling, or judge-dimension scoring weight. |
| `product_policy_gap` | The bot is being asked to do something the V1 product surface does not yet cover (e.g. Top-Ad refund flow, Replies-Messaging email-sync repair). Fix is a Phase 2 / 3 spec change, not a runtime / prompt / skill change. |
| `unknown_needs_human_review` | Cause is not yet evidence-backed; needs a transcript walkthrough before any fix layer is committed. |

The first non-trivial multi-layer choice is `prompt_context_projection` vs.
`skill_orchestration`: prompt fixes nudge the LLM with one new sentence;
skill fixes wrap a deterministic state machine around a recurring
multi-turn / multi-tool flow. Pick prompt when one missing cue would have
flipped the decision; pick skill when the LLM is being asked to thread
together 3+ tools / 2+ turns under a recognisable trigger condition (see
F2).

## Evidence — what's currently failing

All evidence is anchored to these two smoke runs:

- post-Sprint-4 canonical: `eval_interactive/results/20260504-221916/results.json` (8/14, mean composite 0.4915)
- nondeterminism reference: `eval_interactive/results/20260504-223153/results.json` (6/14, mean composite 0.3615)

Reference also: `docs/current_eval_baseline.md`, `docs/10-handoff.md` §9
"Remaining P0 / P1 blockers", `docs/codex-findings.md` (Sprint 4.1 review,
no blocking, three P2 documentation notes only).

## Classifications

### cs_interactive_192 — turn-0 grounding loss + ReadTimeout double failure mode

| Field | Value |
|---|---|
| Case id | `cs_interactive_192` |
| Observed failure | r1 ERROR `session_create_failed: ReadTimeout` (no transcript). r2 FAIL composite 0.000 with `L1:source_citation_present`, `L2:tool_sequence_match`. UC-B FAQ ("can I give away free items?"). The bot answered fluently but did not cite a `source_id`, and the run did not call `search_knowledge` on the very first user turn. |
| Evidence path | `eval_interactive/results/20260504-221916/results.json` (cs_interactive_192 ERROR row). `eval_interactive/results/20260504-223153/results.json` (cs_interactive_192 r2 row, tags `L1:source_citation_present`, `L2:tool_sequence_match`). Spec: `eval_interactive/case_specs/smoke/cs_interactive_192.yaml`. |
| Primary layer | `infra_runtime` (the dominant smoke-side failure mode is the ReadTimeout — 5 unique cases across the two Sprint 4 runs; this case sees it on r1) |
| Secondary layer | `prompt_context_projection` (the r2 grounding failure is an LLM behaviour: it answered without first running `search_knowledge` / `resolve_article`. The grounding instruction string in `PhaseEvaluator.plan()` for the FAQ RESOLVE branch only says "cite knowledge source IDs from `search_knowledge` results" — it does not enforce "search before answering" on the very first user turn when the user message is itself the FAQ question.) |
| Why primary | The ReadTimeout completely prevents the bot from running in r1 — no other layer can be evaluated. Once that is unblocked, the secondary `source_citation_present` flake becomes the next thing to look at. |
| Why other layers should not be fixed first | A prompt fix is wasted work if the session never starts; a skill fix would not survive a session_create error; a CaseSpec fix is inappropriate (the case correctly expects a grounded answer). |
| Recommended minimal next action | (1) Widen eval-client timeout to 120s OR pre-warm the first Kimi call OR async pre-fetch FAQ snapshots OR accept-and-retry on ReadTimeout. (2) Once unblocked, evaluate whether the FAQ RESOLVE grounding instruction needs a "search before answering on turn 1 if no `search_knowledge` result is in `accumulated_tool_results`" cue (see F1). |
| Confidence | high (infra_runtime is corroborated by ReadTimeouts across cs_002 / cs_014 / cs_015 / cs_192 / cs_259 in r1 / r2) |

### cs_interactive_259 — soft-OOS UNKNOWN topic, payment FAQ, contract violation / wrong outcome

| Field | Value |
|---|---|
| Case id | `cs_interactive_259` |
| Observed failure | r1 `CONTRACT_VIOLATION:active_use_case` (no UC ever committed before escalation, total_turns=0 stop_reason=contract_violation). r2 `L2_GATE:correct_outcome`, `L2:correct_outcome`, `L2:tool_sequence_match` — the bot escalated UC-F with `faq_miss_threshold_exceeded` after a single user turn instead of running the search → resolve_article → record_outcome FAQ flow. Form topic was UNKNOWN with empty description; the user turn 1 is "How do I receive the payment when I sell an item". |
| Evidence path | `eval_interactive/results/20260504-221916/results.json` (cs_interactive_259 r1 row). `eval_interactive/results/20260504-223153/results.json` (cs_interactive_259 r2 row). Spec: `eval_interactive/case_specs/smoke/cs_interactive_259.yaml`. |
| Primary layer | `skill_orchestration` (the UC-F payment FAQ resolve flow is a deterministic multi-tool sequence: `get_customer_context → search_knowledge → resolve_article → record_outcome`. The bot does not stitch these together when the form context is UNKNOWN — it shortcuts to "I'm having difficulty resolving this" + handover. The FAQ-grounded-resolve plan template is the right shape of fix.) |
| Secondary layer | `prompt_context_projection` (the DISCOVER → RESOLVE transition is silent about "if topic is UNKNOWN but user turn 1 contains a clear FAQ-shaped question, classify and search before escalating"). Tertiary: `infra_runtime` (cs_259 also hit ReadTimeout in earlier sprints, but not in r1/r2 of Sprint 4). |
| Why primary | r2 demonstrates the bot got past session-create and still skipped the entire FAQ flow. r1 demonstrates the contract-violation path triggered when the bot escalates with no UC committed. Both shapes are upstream of a missing skill template that says "for UNKNOWN-topic + FAQ-shaped first user turn, classify_use_case → search_knowledge → resolve_article → answer with citation". |
| Why other layers should not be fixed first | A `java_guard` for "must call search_knowledge before escalating" would fight the legitimate fast-escalation paths (user_distress, user_requested, hard-OOS, GDPR). A `case_spec_eval` widening would mask the real gap — the bot really should resolve this question. A `judge_calibration` fix would not change the L2 gate. |
| Recommended minimal next action | F2 §S1 "FAQ-grounded-resolve skill" candidate. Trigger when DISCOVER ends with a low-confidence FAQ-shaped intent and no `search_knowledge` result in `accumulated_tool_results`. |
| Confidence | high (the r2 transcript shows 1 user turn, no search, immediate handover; the spec is unambiguous that this should resolve) |

### cs_interactive_176 — UC-E feature-explanation form, payment-dispute escalation reason picked

| Field | Value |
|---|---|
| Case id | `cs_interactive_176` |
| Observed failure | r1 `L1:escalation_compliance` cross-family fail — bot stamped `payment_dispute_detected` for a UC-E "why isn't my paid ad at the top" form context; spec expects `user_requested` (or family-equivalent FAQ-miss). r2 worse — bot picked UC-I (`active_use_case=UC-I`, reason `service_degraded`). |
| Evidence path | `eval_interactive/results/20260504-221916/results.json` (cs_interactive_176 r1 row, tags `L1:escalation_compliance`, `L3:relevance`, `L3:tone_appropriateness`). `eval_interactive/results/20260504-223153/results.json` (cs_interactive_176 r2 row, tags `L1:escalation_compliance`, `L2_GATE:correct_uc`, `L2:correct_uc`). Spec: `eval_interactive/case_specs/smoke/cs_interactive_176.yaml`. |
| Primary layer | `prompt_context_projection` (`request_handover` enum has 23 values; the bot LLM matched the literal phrase "give me my £50 back" to `payment_dispute_detected`. The system prompt's `request_handover` decision tree puts payment-dispute under "Payment dispute / chargeback / billing issue → `payment_dispute_detected`". For a UC-E feature-explanation context where the user's turn-2 message contains "£50 back", the LLM has no cue that the dispute literal is a *trigger* category, not a *content* keyword match. The prompt currently lacks: (a) "use the active_use_case as a tiebreaker — if active_use_case is UC-E, prefer FAQ-family triggers over policy/safety triggers unless the policy signal is unambiguous", (b) "do not pick payment_dispute_detected for advertising fee inquiries — those are UC-E, not UC-FP".) |
| Secondary layer | `skill_orchestration` (a UC-E feature-explanation skill could have committed UC-E and run the FAQ search before the user's frustrated payment-back demand arrived). Tertiary: `case_spec_eval` (the spec lists secondary UCs `[UC-B, UC-K]` but not UC-FP / UC-FP refund family — defensible if the design wants Top-Ad-refund routed away from UC-FP). |
| Why primary | The runtime did not stamp `payment_dispute_detected` from a deterministic gate — the LLM picked it from the enum based on prompt cues. Fixing this in `applyEscalationReason` would require a runtime taxonomy ("if active_use_case = UC-E, downgrade payment_dispute_detected to faq_miss") which is exactly the *bot_limit family* widening codex flagged in Sprint 4 as a runtime over-fit. The minimal, evidence-backed fix is a prompt cue. |
| Why other layers should not be fixed first | (a) `java_guard`: would replicate the §E1 pattern of downgrading a Tier-0 reason — but `payment_dispute_detected` is a Tier-2 policy reason, not a distress reason. The pattern does not transfer cleanly. (b) `case_spec_eval`: would mask a genuine bot mistake (the persona is asking for a refund for a paid feature; the right answer is UC-E feature-explanation + UC-K / UC-FP intake handover, not payment-dispute escalation). (c) `skill_orchestration`: helpful but a skill for "UC-E feature-explanation" would be premature without first verifying that a one-line prompt cue is insufficient. |
| Recommended minimal next action | F1 candidate C1 "active_use_case-aware request_handover decision tree" — add one paragraph to `system_prompt.txt` in the `request_handover` section that says: "Before picking a Tier-2 policy reason (`payment_dispute_detected`, `appeal_requires_human`, `incorrect_deletion_appeal`, `account_compliance`), check `session.active_use_case`. If active_use_case ∈ {UC-A, UC-B, UC-E} (Ad / FAQ / advertising-fee feature-explanation), prefer `faq_miss_threshold_exceeded` or `intake_complete_for_uc_k` unless the user explicitly invokes a chargeback / billing / GDPR / appeal path." |
| Confidence | medium-high (the symptom is reproducible across both Sprint 4 runs; the proposed fix is one prompt paragraph; if the prompt fix doesn't stick, escalate to F2 §S2 "soft-OOS / advertising-fee feature-explanation" skill) |

### cs_interactive_066 — UC-K turn_budget cross-family variance

| Field | Value |
|---|---|
| Case id | `cs_interactive_066` |
| Observed failure | r1 PASS (UC-K / `intake_complete_for_uc_k` / 0.867). r2 FAIL composite 0.000 — `L1:escalation_compliance` cross-family on `turn_budget_exhausted` vs spec `intake_complete_for_uc_k`. UC-K contract preserved (active_use_case=UC-K in both runs). 5-turn transcript on r2 — bot kept asking intake clarifications instead of completing intake within the UC-K turn budget. |
| Evidence path | `eval_interactive/results/20260504-221916/results.json` (r1 PASS row). `eval_interactive/results/20260504-223153/results.json` (r2 FAIL row, `escalation_reason=turn_budget_exhausted`, total_turns=5, transcript shows two clarifying questions in a row before turn budget hits). Spec: `eval_interactive/case_specs/smoke/cs_interactive_066.yaml` (`max_turns=10`, intake fields `[platform, repro_steps_or_error_message]`). |
| Primary layer | `skill_orchestration` (UC-K intake is a deterministic flow — collect platform, repro_steps_or_error_message, then `request_handover(intake_complete_for_uc_k)`. The bot LLM keeps asking one extra clarifying question per turn, exhausts the budget, and the resolver legitimately stamps `turn_budget_exhausted`. A deterministic intake plan template would commit each field as it's collected and trigger handover the moment the field set is complete.) |
| Secondary layer | `prompt_context_projection` (the intake `systemInstruction` in `PhaseEvaluator.buildIntakeSystemInstruction` does not currently surface "you have already collected fields X, Y; the only remaining field is Z" — the LLM has to re-derive this from `conversation_history` every turn, and on r2 it asked a question outside the required field list). Tertiary: `java_guard` (one could harden the resolver to prefer `intake_complete_for_uc_k` over `turn_budget_exhausted` when ≥ 80% of intake fields are committed — but that is exactly the L1-family conflation Sprint 4 §E1 pushed back on). |
| Why primary | r1 PASS shows the runtime / spec / override are all in agreement when the LLM happens to finish intake. r2 FAIL is upstream LLM intake-completion variance. Hardening the runtime in either direction (force-completion or extending the budget) is a workaround; the structural fix is a UC-K intake skill that makes "ask only the missing field" deterministic. |
| Why other layers should not be fixed first | (a) `case_spec_eval`: r1 already passes — the spec is correct. (b) `judge_calibration`: r2 fails L1, not an L3 judge. (c) `infra_runtime`: not a session_create failure. (d) `java_guard`: would re-introduce the bot_limit / intake_complete family conflation Codex pushed back on in Sprint 3.1. |
| Recommended minimal next action | F2 §S3 "UC-K (and UC-G/H/I/J) intake-collect-and-handover skill" candidate. As a smaller-scope alternative, F1 could enrich the intake `systemInstruction` with the explicit list of "fields collected so far" / "fields remaining" — but the underlying re-derivation cost makes the skill route the durable answer. |
| Confidence | medium-high (the variance is reproducible — see Sprint 3 r2, Sprint 4 r2 — and the fix layer is consistent across both runs) |

### cs_interactive_011 — variance reference (currently stable PASS post-Sprint-4-§E1)

| Field | Value |
|---|---|
| Case id | `cs_interactive_011` |
| Observed failure | r1 PASS (UC-D / `faq_miss_threshold_exceeded` / 0.800). r2 PASS (same). Carried in this taxonomy as **closure evidence** that the §E1 gate + §E3 supporting override resolved the cs011 cross-family flake. |
| Evidence path | `eval_interactive/results/20260504-221916/results.json` cs_interactive_011 row. `eval_interactive/results/20260504-223153/results.json` cs_interactive_011 row. Override: `eval_interactive/case_spec_overrides.yaml` (570Q5000008NWIjIAO). Java contract: `Cs001LlmDistressGateIntegrationTest` (cs014-shape calm seed → downgraded to faq_miss case). |
| Primary layer | `java_guard` (closed) — §E1 gate in `ControlKernel.applyEscalationReason`. Listed here as a *historical anchor*: the cs011 prior failure mode (LLM-supplied `user_distress` without B1 hit) is exactly the pattern that Sprint 4 closed via runtime guard, not via prompt or skill. |
| Why primary | The §E1 gate intentionally lives in Java because "B1 deterministic detector must earn `user_distress`" is an invariant the LLM cannot be trusted to maintain. This is a true `java_guard` concern. |
| Why other layers should not be fixed first | (n/a — closed) |
| Recommended minimal next action | none — keep `Cs001LlmDistressGateIntegrationTest` green; if cs011 ever regresses, reopen as `java_guard` first. |
| Confidence | high |

### cs_interactive_001 — variance reference (stable PASS post-Sprint-4-§E1)

| Field | Value |
|---|---|
| Case id | `cs_interactive_001` |
| Observed failure | Both Sprint 4 runs PASS at composite 0.786 (UC-C / `faq_miss_threshold_exceeded`). Failure tags only contain L3:relevance / L3:tone_appropriateness — judge volatility, not a runtime / spec gap. |
| Evidence path | both Sprint 4 results files. |
| Primary layer | `judge_calibration` (residual L3 tags). Historically a `java_guard` (§E1 gate) closure. |
| Why primary | The escalation reason / UC / outcome are all green; the only failure tags are L3 dimensions which `docs/current_eval_baseline.md` explicitly defers (D15). |
| Why other layers should not be fixed first | The runtime / prompt / skill / case-spec layers are all green for cs001 — there is no remaining defect on those layers. |
| Recommended minimal next action | none in the next implementation sprint. Defer to a future judge-calibration sprint. |
| Confidence | high |

### cs_interactive_002 — variance reference

| Field | Value |
|---|---|
| Case id | `cs_interactive_002` |
| Observed failure | r1 PASS (UC-C / `user_distress` / 0.786). r2 ERROR `session_create_failed: ReadTimeout`. |
| Evidence path | both Sprint 4 results files. |
| Primary layer | `infra_runtime` (the only Sprint 4 failure mode for cs002 is the ReadTimeout shared with cs014 / cs015 / cs192 / cs259). The B1 contract is pinned by `Cs002AlreadyEscalatedDistressReconcileIntegrationTest`. |
| Why primary | r1 demonstrates the runtime / spec are aligned when the session starts; r2 fails before runtime logic runs at all. |
| Why other layers should not be fixed first | Same as cs192 — fixing prompt / skill / spec while the session never starts is wasted work. |
| Recommended minimal next action | Same as cs192 — F1 / F2 from `docs/current_eval_baseline.md` "Recommended next sprint direction": eval-client timeout / Kimi pre-warm / async pre-fetch / accept-and-retry. |
| Confidence | high |

### cs_interactive_014 — variance reference

| Field | Value |
|---|---|
| Case id | `cs_interactive_014` |
| Observed failure | Both Sprint 4 runs ERROR `session_create_failed: ReadTimeout`. The Java route/loop contract is pinned by `Cs014RouteAndLoopHandoverIntegrationTest` and the §C2 strong-prior carry-forward. |
| Evidence path | both Sprint 4 results files. |
| Primary layer | `infra_runtime` |
| Secondary layer | (none — the prior `prompt_context_projection` / `java_guard` gaps for cs014 are closed by Sprint 3 §C2) |
| Why primary | Same as cs002 / cs192. |
| Why other layers should not be fixed first | Same. |
| Recommended minimal next action | Same as cs002 / cs192. |
| Confidence | high |

### cs_interactive_015 — UC-FP rejected-ad edit / repost flow

| Field | Value |
|---|---|
| Case id | `cs_interactive_015` |
| Observed failure | r1 FAIL composite 0.000, `L2_GATE:correct_uc`, `L2:correct_uc`, `L2:tool_sequence_match` — bot routed UC-A (Ad Status & Visibility) instead of UC-FP (Posting Policies / rejected-ad reason / edit-or-repost). Transcript: 2 turns, bot greeted with "could you provide your ad ID" then asked a clarification, never got to the FAQ. r2 ERROR `session_create_failed: ReadTimeout`. |
| Evidence path | both Sprint 4 results files. Spec: `eval_interactive/case_specs/smoke/cs_interactive_015.yaml`. |
| Primary layer | `prompt_context_projection` / `skill_orchestration` (joint — the bot's UC-A vs UC-FP routing call was made on a turn-0 form description "Hi - can you tell me what happened to my ad?" without seeing the persona's hidden_fact "ad title was 'Male Massage and Waxing, by a Male'". UC-A vs UC-FP disambiguation is exactly what `routing_prompt.txt` already lists as "Why was my ad deleted, ad on hold, policy violation reason, appeal a removal → UC-FP". The prompt is correct; the bot did not pick UC-FP because the form description does not contain those words. The skill-orchestration view: the rejected-ad / appeal-or-edit flow needs `get_customer_context` → `search_knowledge` early so the LLM has the moderation reason before it commits a UC.) |
| Secondary layer | `infra_runtime` (r2 ReadTimeout — same shared mode). |
| Why primary | The persona reveals "ad title was X" only when the bot asks for details; on turn 1 the bot has no signal to distinguish UC-A from UC-FP. The right structural fix is to (a) auto-trigger `get_customer_context` (which would surface the moderation status) — already in the V1 design but currently the LLM is choosing UC-A before that happens, and (b) update the routing-prompt to prefer UC-FP over UC-A when description is "what happened to my ad" (matches the routing_prompt rule already, but is not surfaced as a tiebreaker rule). |
| Why other layers should not be fixed first | A `java_guard` for UC-A vs UC-FP would be content-keyword brittle. A `case_spec_eval` widening would mask the genuine routing miss. An `infra_runtime` fix unblocks r2 but does not address r1. |
| Recommended minimal next action | F1 candidate C2 "routing prompt UC-FP tiebreaker" (one-line change: when `topic_subject = Ad Support` and `description` contains "what happened to my ad" / "removed" / "on hold" / "rejected", prefer UC-FP over UC-A). Plus: ensure `get_customer_context` runs before classify_use_case on form-context-with-email cases (already auto-triggered per Phase 3 §3.1.4 but the order needs verification — see F1 §3). |
| Confidence | medium |

### cs_interactive_095 — UC-A email-sync product gap

| Field | Value |
|---|---|
| Case id | `cs_interactive_095` |
| Observed failure | Both Sprint 4 runs FAIL composite 0.000 with `L2_GATE:correct_outcome`, `L2:correct_outcome` — bot escalated (`faq_miss_threshold_exceeded`) when spec expects resolve. The persona wants to change the email address tied to the app account so messages sync; the bot routes UC-A correctly but cannot resolve the email-change request from FAQ. |
| Evidence path | both Sprint 4 results files. Spec: `eval_interactive/case_specs/smoke/cs_interactive_095.yaml` (`outcome_class=resolve`, `expected_tool_sequence=[get_customer_context, search_knowledge, resolve_article, record_outcome]`). |
| Primary layer | `product_policy_gap` (the V1 FAQ surface does not include a self-service "change my account email" article. `search_knowledge` on "wrong email on app account" does not return a resolve-able article — the spec asks for a resolution that the knowledge base cannot ground. The bot's escalate is honest given the actual knowledge surface; the spec is asking for a product capability that V1 does not have.) |
| Secondary layer | `case_spec_eval` (the override / spec authors should reconsider whether cs095 should expect resolve given the FAQ surface, OR whether to widen the UC-A FAQ corpus to cover email-sync). Tertiary: `judge_calibration` (L3:relevance / L3:tone_appropriateness flap). |
| Why primary | The bot is doing the structurally-correct thing (route UC-A, search FAQ, find no useful article, escalate). Forcing resolve via prompt or skill would require fabricating an answer that the knowledge base does not support — a direct violation of the system prompt's "Never fabricate facts; ground answers in retrieved knowledge or context" rule. |
| Why other layers should not be fixed first | (a) `prompt_context_projection`: the prompt is already telling the LLM the right thing — escalate when faq_miss. (b) `skill_orchestration`: a UC-A skill could not invent an article. (c) `java_guard`: forcing `outcome_class=resolve` would be a regression. |
| Recommended minimal next action | A non-Sprint-5 spec-side decision: either (a) add a UC-A "change app account email" FAQ article + override, or (b) update the spec to `outcome_class=escalate` with `escalation_trigger=account_compliance` or `intake_complete_for_uc_h` (depending on which UC-H/D path the email-change actually belongs to). Defer: this is a spec / Phase 2 product question, not a Sprint 5 diagnostic. |
| Confidence | medium-high |

### Kimi `session_create_failed: ReadTimeout` family — cs014 / cs002 / cs015 / cs192 / cs259

| Field | Value |
|---|---|
| Case ids | cs_014 / cs_002 / cs_015 / cs_192 / cs_259 (varying r1/r2 incidence) |
| Observed failure | `ERROR:session_create_failed: ReadTimeout('timed out'). Backend at http://localhost:8080 unreachable or returned an error during POST /v1/chat/sessions.` Total turns 0, stop_reason=error. |
| Evidence path | both Sprint 4 results files. `docs/10-handoff.md` §9 P1 #1, `docs/current_eval_baseline.md` "Known current blocker patterns" #1. |
| Primary layer | `infra_runtime` |
| Secondary layer | none — the failure is upstream of any LLM / spec / skill surface. |
| Why primary | The auto-search path runs 4–6 chained Kimi LLM calls during session create. Each Kimi call takes 8–15s on the historical endpoint, so total auto-search latency exceeds the 60s eval-client timeout. The runtime + spec + prompt + skill surfaces all become irrelevant when the session never starts. |
| Why other layers should not be fixed first | The error tag is a session-create timeout, not a model behaviour. No prompt / spec / skill change would help. |
| Recommended minimal next action | (1) widen eval-client timeout to 120s OR (2) pre-warm Kimi connection on a /healthz hit OR (3) async pre-fetch FAQ snapshots so session-create doesn't block on Kimi calls OR (4) accept-and-retry on ReadTimeout. Pick one in the next implementation sprint (`docs/current_eval_baseline.md` §F1 candidate). |
| Confidence | high (the single dominant smoke-side failure mode in both Sprint 4 runs; 5 unique cases across r1/r2) |

### L3 relevance / tone_appropriateness — judge volatility

| Field | Value |
|---|---|
| Case ids | cs_001, cs_002, cs_011, cs_038, cs_066 (r2 only), cs_095, cs_176 (varying) |
| Observed failure | L3:relevance and / or L3:tone_appropriateness flap across runs on identical bot output. Even passing cases (cs_001, cs_002, cs_011) carry these tags. |
| Evidence path | both Sprint 4 results files. `docs/current_eval_baseline.md` "Known nondeterminism" L3 bullet. `docs/action_bank.md` D15 (deferred). |
| Primary layer | `judge_calibration` |
| Why primary | The judge flips on the same bot output across nondeterminism re-runs. No structural change in the bot, runtime, prompt, skill, or spec would explain the per-run variation. |
| Why other layers should not be fixed first | Per `docs/action_bank.md` D15 and `docs/current_eval_baseline.md` "L3 judge volatility", this is explicitly out of scope for the next narrow runtime sprint. |
| Recommended minimal next action | none in Sprint 6. Defer to a future judge-calibration sprint. |
| Confidence | high |

## Layer count summary (post-Sprint-4 smoke)

| Layer | Count of distinct cases primarily here |
|---|---:|
| `infra_runtime` | 5 (cs_002 / cs_014 / cs_015 / cs_192 / cs_259 in their ReadTimeout incidence; cs_192 / cs_259 also have a non-infra layer when the session does start) |
| `skill_orchestration` | 2 (cs_259 r2 FAQ-flow shortcut, cs_066 r2 intake-completion variance) |
| `prompt_context_projection` | 2 (cs_176 reason-tree, cs_015 UC-A-vs-UC-FP routing tiebreaker) |
| `product_policy_gap` | 1 (cs_095 email-sync) |
| `judge_calibration` | many (carried as L3 tag noise on most cases including passing ones) |
| `java_guard` | 0 new — Sprint 4 §E1 / §E3 closed the recent ones; cs_011 / cs_001 are listed only as historical anchors |
| `case_spec_eval` | 0 new (all approved-override paths already pinned by `test_smoke_yaml_matches_override_pipeline_output`) |
| `unknown_needs_human_review` | 0 |

## Implications for the next implementation sprint

- The marginal `java_guard` per-case payoff is now low. Sprint 4 closed the
  last high-confidence runtime invariant gap (`user_distress` LLM
  over-claim). Continuing to add Java guards for the remaining failures
  would either re-conflate L1 escalation_compliance families
  (cs_066 turn_budget vs intake_complete) or impose runtime taxonomies
  on LLM enum picks (cs_176 payment_dispute_detected) that prompts can
  handle more cleanly.
- The largest single-fix lift is `infra_runtime` — one timeout / pre-warm /
  retry change unblocks 5 cases.
- After infra is unblocked, the next two highest-confidence fixes are
  `prompt_context_projection` (cs_176 reason-tree, cs_015 routing
  tiebreaker — small, scoped, evidence-backed) and `skill_orchestration`
  (cs_259 FAQ-grounded-resolve, cs_066 UC-K intake-collect-and-handover —
  larger, but each unblocks a recurring failure family).
- `case_spec_eval` and `judge_calibration` should remain deferred per
  `docs/current_eval_baseline.md` and `docs/action_bank.md`.
- `product_policy_gap` (cs_095) is a spec / Phase 2 question, not a Sprint 6
  candidate.
