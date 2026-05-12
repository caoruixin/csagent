# Failure Brief — cs259 UC-F Sprint 7 §I0 violation on payment question

> **Source case:** `cs_interactive_259` (UC-F primary, no secondary, expected `resolve`, `should_escalate: false`).
> **Source session:** `570Q5000008OqaLIAS`.
> **CaseSpec:** `eval_interactive/case_specs/smoke/cs_interactive_259.yaml`.
> **Approved L3 override:** NONE.
> **Runs:** `20260505-235231` (FAIL, composite=0.0, L2_GATE:correct_outcome + L2:tool_sequence_match=0.25 + L3:relevance/tone=2.0). 2026-05-10 regressed further (empty escalation_reason, source_citation_present + trace_minimum fails) — Cluster C, deferred via `R-smoke-regression-investigation`.
> **Filed:** 2026-05-13 (Sprint 18 / G1, brief #9, last smoke-set brief).

## Ground-truth chain

cs_259 has no approved L3 override. The CaseSpec is rule-extracted L1 + L2 LLM persona review.

Ground truth: UC-F (Payment / sale proceeds), `outcome_class: resolve`, `should_escalate: false`, `allow_bot_resolution: 'true'`, expected tool sequence `get_customer_context → search_knowledge → resolve_article → record_outcome`, `grounding_mode: faq_source_backed`, `risk_level: medium`, `bot_handling_pattern: "Acknowledge the issue, run policy-mandated tools (...), and answer from the knowledge base. Do not escalate."`

**CaseSpec ↔ phase 2 conflict on `get_customer_context` (third instance, after cs_001 UC-C and cs_011 UC-D).** CaseSpec lists `get_customer_context` as the first tool in `expected_tool_sequence`, but phase 2 §2.10 line 358 restricts it to UC-A / UC-FP / UC-K — UC-F is excluded. The bot's actual skipping of the tool is consistent with phase 2 but inconsistent with the CaseSpec. Reinforces the existing systematic `R-generator-get-customer-context-policy-mismatch` — now confirmed across three UCs (UC-C, UC-D, UC-F), strongly suggesting the CaseSpec generator unconditionally includes `get_customer_context` regardless of phase 2's per-UC allowance list.

**Empty form context is structurally relevant.** `form_context.topic_subject=UNKNOWN`, empty `description`, empty `ad_id`. This is the only smoke case where the form provided no disambiguation signal — the user landed in chat without selecting a topic. This shape activates **Sprint 7 §I0 weak-candidate cue** (see "What happened?").

The 2026-05-10 run regression (empty escalation_reason, trace_minimum + source_citation_present fails) is Cluster C / deferred.

## What happened?

User entered with empty form context: `form_context.first_name="Lorraine"`, `topic_subject=UNKNOWN`, empty `description`, empty `ad_id`, persona `frustration_level: high`. The 2-turn transcript:

- **T0 bot** (session_create, no user form description to react to): "Hi Lorraine! Thank you for reaching out. I'd like to help you with your inquiry. Could you tell me a bit more about what you need help with?"
- **T1 user:** "How do I receive the payment when I sell an item"
- **T1 bot:** "I'm having difficulty resolving this. Let me connect you with a specialist."

Tool sequence: `search_knowledge → classify_use_case → request_handover` (`tool_sequence_match=0.25`, lcs=1/4 vs the CaseSpec's `[get_customer_context, search_knowledge, resolve_article, record_outcome]`). The bot ran 1 search → presumably no viable hit / faq_miss → committed UC-F via classify_use_case → escalated. **No `resolve_article` call**. Outcome: `escalate` with `faq_miss_threshold_exceeded` (expected `resolve` with `escalation_trigger: null`). L3 relevance=2.0, tone=2.0.

**The bot violated Sprint 7 §I0 weak-candidate cue.** The DISCOVER phase `system_instruction` (visible in the bot's phase plan, e.g. the manual probe trace from 2026-05-13) contains an explicit rule:

> "Sprint 7 §I0 weak-candidate cue: when candidate_use_cases is empty or weak AND the form context is empty / UNKNOWN topic AND the current user message is clearly FAQ-shaped ('how do I X', 'can I Y', 'what items are allowed') OR is payment / sale-proceeds-shaped ('how do I receive payment', 'how do I get paid when I sell', 'how does payout work'), **do NOT request_handover with faq_miss_threshold_exceeded after a single user turn.** Instead, gather enough evidence to classify toward the right FAQ-path UC: call search_knowledge with the user's question as the query, then call classify_use_case with the most plausible UC (payment / sale-proceeds questions point to UC-F; ...). Once classified, RESOLVE will run the grounded resolve sequence."

cs_259 matches all three preconditions:

- `candidate_use_cases`: weak (empty form context = no prior signal).
- form context: UNKNOWN topic, empty description.
- user message: "How do I receive the payment when I sell an item" — **literally one of the §I0 example phrases** ("how do I receive payment", "how do I get paid when I sell").

The bot followed the first two §I0 steps (search_knowledge + classify_use_case → UC-F committed at 0.90 confidence per the result data). Then **violated the prohibition** by calling `request_handover(faq_miss_threshold_exceeded)` instead of letting RESOLVE run a grounded resolve sequence (which §I0 explicitly says RESOLVE will do).

## What should a good CS agent have done?

The capability gap is **honouring explicit phase-plan instructions when their preconditions match**, combined with **resolving payment / sale-proceeds questions at the FAQ-grounded resolve path rather than short-circuiting to escalation**.

Concretely:

- **Honour Sprint 7 §I0.** When the preconditions match (weak candidates, empty form, FAQ/payment-shaped user message), the bot must not `request_handover(faq_miss_threshold_exceeded)` on a single user turn. The instruction is explicit and in the prompt.

- **Run the RESOLVE sequence after classification.** Once UC-F is committed at 0.90 confidence, the bot should transition to RESOLVE and attempt a grounded answer via `search_knowledge` (with a UC-F-tagged query) → `resolve_article` on viable hits → grounded customer-facing answer with source citation. If the search genuinely returns no viable hit, the right response is either an honest non-answer ("I couldn't find a specific article on payment timing — let me check with someone who knows the latest payout policy") or a payment-context-aware clarification, not the fixed template.

- **Acknowledge the user's specific question.** "How do I receive the payment" is a concrete, common UC-F question. Even if the corpus is gappy, the bot can name the user's frame back ("you're asking about receiving payment when an item sells"). The current "I'm having difficulty resolving this" template gives no acknowledgment.

- **Honour the UC-F policy expectation.** UC-F is `allow_bot_resolution: 'true'`, `should_escalate: false`. The system design says UC-F is bot-resolvable. The bot's escalation contradicts the system's own classification.

## Why does this matter?

UC-F (Payment / sale proceeds) is a medium-risk, high-volume category — receiving payment is one of the most frequent questions a seller has on any marketplace. cs_259's specific question ("how do I receive the payment when I sell an item") is the most basic possible UC-F entry-point. A bot that escalates this with a template message is failing the simplest UC-F case shape, which suggests UC-F containment-rate is likely substantially below target.

cs_259 also demonstrates a **single instance of the bot ignoring an explicit Sprint 7 §I0 phase-plan directive**. The §I0 cue exists precisely to prevent the failure mode cs_259 exhibits, yet the bot called `request_handover(faq_miss_threshold_exceeded)` after a single user turn — exactly what §I0 forbids. Whether this is a systematic planner instruction-following gap or a one-off interaction with the UC-F + empty-form shape needs more evidence before triggering a remediation path; in either case it's worth flagging that "adding clearer instructions" is not guaranteed to fix the shape — the existing instruction was clear and was ignored. See Related observation #5 for the open-question framing.

This brief also shows the systematic CaseSpec generator quirk affecting a third UC. cs_001 (UC-C), cs_011 (UC-D), cs_259 (UC-F) all have `expected_tool_sequence` starting with `get_customer_context` despite phase 2 line 358 excluding their respective UCs. Three cases across three UCs makes the systematic conclusion solid.

## Is this a one-off or a pattern?

`pattern` on multiple dimensions:

1. **Sprint 7 §I0 violation on FAQ-shaped / payment-shaped single-turn questions.** cs_259 is the only smoke case with `topic_subject=UNKNOWN` + empty description, so the §I0 precondition is only satisfied here in the smoke set. n=1 evidence — see Related observation #5 for the testing-required framing.

2. **Mechanical-template surface on faq_miss escalation.** Same template phrasing ("I'm having difficulty resolving this. Let me connect you with a specialist.") as cs_001 / cs_002 / cs_011 / cs_014 / cs_038 / cs_040 / cs_015 / cs_192. cs_259 is the first case where the template fires on T1 (not T0) because the empty form deferred classification to the user's first message.

3. **CaseSpec ↔ phase 2 `get_customer_context` systematic mismatch.** cs_001 (UC-C), cs_011 (UC-D), cs_259 (UC-F). Three UCs confirmed.

## Which layer is likely responsible?

Four candidate layers:

- **`prompt_projection` / `semantic_planner`** (primary, shared root). The bot has explicit Sprint 7 §I0 guidance in its DISCOVER phase system_instruction; the bot still chose to `request_handover(faq_miss_threshold_exceeded)` after a single user turn. Either the projection isn't surfacing §I0 effectively at decision time (prompt_projection) or the planner ignored the directive in this specific shape (semantic_planner). **Single-case evidence** — more test runs targeting different §I0-precondition shapes would help distinguish "the planner ignores phase-plan directives generally" from "the planner missed this specific shape" (see Related observation #5).

- **`corpus`** (secondary, conditional). UC-F payment / sale-proceeds may have FAQ corpus gaps. The bot's 1 search_knowledge call presumably returned no viable hit; whether the corpus actually carries "how do I receive payment" content or whether the bot's query missed existing content needs a corpus check. Reinforces `R-corpus-coverage-audit-per-uc` from cs_095 / cs_192.

- **`eval_spec`** (tertiary). CaseSpec ↔ phase 2 `get_customer_context` systematic mismatch is now confirmed across three UCs (cs_001 / cs_011 / cs_259). Reinforces `R-generator-get-customer-context-policy-mismatch` — this is no longer "potentially systematic", it is systematic.

- **`runtime config`** (faqMissCount threshold). cs_259's faq_miss_threshold_exceeded fires after 1 search; this is the same threshold + timing concern cs_095 surfaced. Reinforces `R-faqMissCount-threshold-and-timing-review`.

## What should NOT be done?

- Do **not** add a regex / keyword on "how do I receive payment" / "how do I get paid" / similar phrases → force-route to UC-F resolve template. Keyword chatbot regression Constitution §1.5.
- Do **not** rush to add another / clearer Sprint 7 §I0-style instruction to the prompt without testing whether more text helps. The existing instruction is clear and was ignored in cs_259; more text may or may not change the planner's behaviour — that needs validation, not assumption.
- Do **not** add a Java guard refusing `request_handover(faq_miss_threshold_exceeded)` on UC-F. Too rigid; UC-F can legitimately faq_miss on edge cases (obscure tax / refund policy questions); the runtime contract should be that the reason matches actual session evidence (per `R-escalation-reason-runtime-evidence-contract-review`), not that UC-F can never escalate.
- Do **not** synthesize fake UC-F payment FAQ articles to close the corpus gap. Same point as cs_095 / cs_192.
- Do **not** edit cs_259's CaseSpec to accept `escalate` as a passing outcome. UC-F is `allow_bot_resolution: 'true'` per phase 2; the CaseSpec correctly classifies as resolvable.

## Related observation — no new R-items; cs_259 reinforces four existing items + one open observation

cs_259 produces no new R-items. It reinforces existing ones across multiple briefs and surfaces one open observation that needs more evidence before becoming an R-item.

1. **`R-generator-get-customer-context-policy-mismatch`** (cs_001 UC-C + cs_011 UC-D + now cs_259 UC-F). Three confirmed instances across three UCs. The "systematic generator-vs-policy mismatch" hypothesis from cs_011 is solid now.

2. **`R-corpus-coverage-audit-per-uc`** (cs_095 UC-D + cs_192 UC-B + now cs_259 UC-F). Third UC where the bot's FAQ search comes up empty on a basic policy question. Per-UC corpus coverage audit is increasingly load-bearing for the eval-governance track.

3. **`R-faqMissCount-threshold-and-timing-review`** (cs_095 first, cs_192 reinforced, cs_259 reinforced). cs_259 fits the "1 search + 1 user turn = faq_miss" shape exactly.

4. **`R-l3-judge-form-context-trust-rubric`** — no new evidence here (cs_259's persona is named "Lorraine" via `form_context.first_name`; L3 judge in cs_259 didn't specifically flag name confirmation, so this case doesn't reinforce the issue but doesn't contradict it either).

5. **Open observation (not yet an R-item)**: cs_259's bot violated the explicit Sprint 7 §I0 phase-plan instruction despite the cue being in the DISCOVER `system_instruction` text. Whether this is a systematic "planner ignores phase-plan directives" issue or a one-off (e.g. UC-F + empty-form interaction) needs more evidence before opening a backlog item. **More test runs targeting different shapes that activate Sprint 7 §I0 conditions** (empty form + FAQ-shaped messages across UC-B / UC-C / UC-D / UC-F) are needed before deciding whether to open a `R-prompt-phase-plan-directive-followship` item. This testing should be part of G2 case-family construction or a small targeted probe sprint, not committed to as an R-item on n=1 evidence.
