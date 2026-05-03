# Codex Findings: Interactive Eval Review

Date: 2026-05-03

Primary reference: `eval_interactive/results/20260501-215807/results.json`

This review focuses on why the current interactive smoke run is failing, which parts of the proposed Phase 3 plan are valid, and what minimal changes should be made before spending more time on prompt tuning or KB authoring.

## Executive Summary

The proposed Phase 3 plan needs one correction before execution: the referenced run is not a `1/14` pass baseline. `eval_interactive/results/20260501-215807/results.json` reports:

| Metric | Actual value |
| --- | ---: |
| Total cases | 14 |
| Passed cases | 0 |
| Failed cases | 14 |
| Task success rate | 0.0 |
| Mean composite score | 0.0 |
| Mean outcome score | 0.6452 |
| Mean judge score | 0.7048 |
| Escalation correctness | 0.7143 |
| Policy compliance rate | 1.0 |
| Mean turns to resolution | 1.86 |

The earlier `1/14` and `0.069` composite baseline appears to describe a different run. The current referenced run also contains `ERROR:session_create_failed` on `cs_interactive_066`, so the "hold session_create_failed at 0" guard is already failing.

The KB field correction is valid: `public.kb_articles` uses `uc_tags`, not `uc_mappings`, and the mapping report shows 218/218 articles mapped. The primary issue is not article-to-UC tag coverage. The immediate blockers are runtime correctness, scoring hygiene, routing, exact escalation reasons, and missing or weak content for a few smoke-relevant topics.

Recommended sequencing:

1. Tier 0: stabilize eval/runtime gates.
2. Tier 1: deterministic routing and escalation-reason resolver.
3. Tier 2: resolve-before-escalate behavior for FAQ UCs.
4. Tier 3: targeted KB article updates/drafts.
5. Tier 4: prompt few-shots only for residual ambiguity.

I would not start with prompt few-shots or KB articles before Tier 0/Tier 1. They would produce mixed signal because several cases are currently zeroed by source-citation, handover, timeout, and exact-enum gates.

## Baseline Failure Inventory

From `20260501-215807`:

| Case | Expected UC | Actual UC | Outcome | Reason | Main blockers |
| --- | --- | --- | --- | --- | --- |
| `cs_interactive_001` | UC-C | UC-C | escalated | `turn_budget_exhausted` | source citation, reason mismatch, missing handover |
| `cs_interactive_002` | UC-C | UC-I | escalated | `turn_budget_exhausted` | source citation, reason mismatch, wrong UC, missing handover |
| `cs_interactive_004` | UC-D | UC-E | escalated | `user_requested` | source citation, wrong UC, wrong outcome |
| `cs_interactive_011` | UC-D | UC-D | escalated | `user_distress` | source citation, reason mismatch, missing handover |
| `cs_interactive_014` | UC-C | UC-C | escalated | `user_requested` | source citation, reason mismatch, missing handover |
| `cs_interactive_015` | UC-FP | UC-A | resolved | none | source citation, wrong UC |
| `cs_interactive_029` | UC-C | UC-F | escalated | `turn_budget_exhausted` | source citation, reason mismatch, wrong UC, missing handover |
| `cs_interactive_036` | UC-I | empty | escalated | `out_of_scope` | invalid transition, reason mismatch, wrong UC, incomplete handover |
| `cs_interactive_038` | UC-J | UC-J | escalated | `intake_complete_for_uc_j` | reason mismatch |
| `cs_interactive_040` | UC-K | UC-K | escalated | `turn_budget_exhausted` | reason mismatch |
| `cs_interactive_066` | UC-E | empty | empty | empty | `session_create_failed` timeout |
| `cs_interactive_095` | UC-A | UC-D | escalated | `turn_budget_exhausted` | wrong UC, wrong outcome |
| `cs_interactive_192` | UC-B | UC-B | resolved | none | source citation only |
| `cs_interactive_259` | UC-F | UC-F | escalated | `turn_budget_exhausted` | source citation, wrong outcome |

The most important implication: a simple enum fix cannot unlock six cases by itself. Several enum-mismatch cases also fail L1 source citation and/or L2 handover completeness.

## 1. Correctness Bugs

### 1.1 Baseline status is misreported

The supplied status says `1/14` pass and mean composite `0.069`, but the referenced result file reports `0/14` and `0.0`. This matters because it changes the interpretation of Phase 3 progress and guardrails. `cs_interactive_040`, which likely passed in an earlier run, regressed to `turn_budget_exhausted` in the referenced run.

Minimal fix:

- Use one canonical baseline run in all Phase 3 planning.
- Add a small summary verifier that prints total/pass/composite/error counts before any plan is approved.

### 1.2 Session creation performs expensive agent work

`SessionManager.createSession()` can trigger FAQ auto-search/control-kernel work during session creation. In the current run, `cs_interactive_066` fails during `POST /v1/chat/sessions` with a read timeout.

This is a product and eval correctness problem. Session creation should be cheap, deterministic, and resilient. Resolution work should happen after the user sends the first actual message.

Minimal fix:

- Remove LLM/KB/control-kernel execution from `createSession()`.
- Return a static, low-risk greeting or no bot message.
- Move any auto-search/resolution to the first user turn.
- Add an eval case that asserts session creation returns within the timeout and cannot call external/LLM tools.

### 1.3 Source citation gate is too broad

`source_citation_present` currently flags any user-facing bot message without `source_ids` when `grounding_mode=faq_source_backed`. This catches messages such as "I'm looking into this for you" and clarifying questions.

That is likely a rubric/scoring bug. Clarifying, acknowledgement, and handover messages are not factual KB answers and should not require article citations.

Minimal fix:

- Require citations only for substantive FAQ answers that make factual claims or recommend policy steps.
- Exempt acknowledgement, clarification, progress, and pure handover turns.
- Add test fixtures for "clarifying question without source" and "factual answer without source".

### 1.4 Handover completeness is missing on escalation paths

Several escalated FAQ cases fail `L2_GATE_MISSING:handover_completeness`. The runtime appears able to record handover data, but not all escalation paths reliably surface a complete handover object to the eval trace.

Minimal fix:

- Guarantee a single runtime-owned handover event for every `ESCALATE` outcome.
- Required fields: `session_id`, `primary_use_case`, `summary`, `escalation_reason`, `total_bot_turns`.
- Add tests for FAQ escalation, fixed-script escalation, hard out-of-scope escalation, and budget escalation.

### 1.5 Escalation reason selection is non-deterministic

The prompt instructs the LLM to choose a canonical enum, but exact reason correctness is a hard gate. Current examples:

- `cs_interactive_038`: expected `trust_safety_required`, actual `intake_complete_for_uc_j`.
- `cs_interactive_040`: expected `intake_complete_for_uc_k`, actual `turn_budget_exhausted`.
- `cs_interactive_001`: expected `clarification_budget_exhausted`, actual `turn_budget_exhausted`.

Minimal fix:

- Add a server-side escalation-reason resolver.
- Give it precedence rules that are tested independently of LLM output.
- Let the LLM provide evidence/intake summary, not the final canonical enum in cases where the runtime can decide.

### 1.6 Routing prompt contradicts use-case registry semantics

`routing_prompt.txt` maps "Can't log in", "password reset", "locked out", and "wrong email on account" to UC-A, described as account/login. The KB mapping report says UC-A is "Ad Status & Visibility"; UC-D is "Account & Login".

This can cause systematic UC-A/UC-D errors and confuses downstream tool policy.

Minimal fix:

- Align routing prompt labels with the registry and eval definitions.
- Add routing regression tests for login, password reset, wrong email, ad not visible, app sync, and contact email.

### 1.7 Hard out-of-scope routing runs before semantic dispute detection

`cs_interactive_036` expects UC-I payment dispute intake but is routed/escalated as hard out-of-scope because the topic subject is Delivery. The case text includes refund/payment/legal-dispute signals that should override generic Delivery OOS handling.

Minimal fix:

- In routing, evaluate high-confidence in-scope payment dispute, trust/safety, and refund signals before hard topic-subject OOS.
- For Delivery plus refund/charge/dispute/legal liability, route to UC-I.
- Keep plain courier/logistics delivery questions out of scope.

### 1.8 Runtime-only case creation is assigned to the agent

The plan logic tells the LLM to call `create_case_controlled` before handover for UC-H/UC-J/UC-K. This is fragile because case creation is a side effect and the tool is intended to be runtime-controlled.

Minimal fix:

- Runtime should create the case deterministically once the fixed-script intake is complete.
- The LLM should not be responsible for ordering side-effect tool calls before handover.
- Add duplicate-case and missing-case tests.

### 1.9 `turn_budget_exhausted` masks semantic failures

Several cases end with `turn_budget_exhausted` even when a more specific outcome is expected. This makes failures hard to diagnose and can produce poor customer experience.

Minimal fix:

- If the runtime reaches the final allowed turn after having enough classification/intake evidence, emit the semantic reason, not generic budget exhaustion.
- Reserve `turn_budget_exhausted` for true loop exhaustion where no better reason applies.

## 2. Missing Eval Cases

The current smoke set is useful, but it does not isolate several important failure modes. Add targeted evals before scaling Phase 3.

### 2.1 Runtime and infrastructure cases

- Session creation must not call LLM/KB tools.
- Session creation must return inside timeout under normal backend load.
- Tool timeout should produce a controlled fallback, not `session_create_failed`.
- No run should contain `ERROR`, `CONTRACT_VIOLATION`, `session_create_failed`, or `max_steps_exceeded`.

### 2.2 Scoring hygiene cases

- Clarifying question without `source_ids` should pass source-citation gate.
- Acknowledgement/progress message without `source_ids` should pass.
- Factual FAQ answer without `source_ids` should fail.
- Factual FAQ answer with irrelevant `source_ids` should fail or receive a lower citation-quality score.
- Handover-only response should not require KB citation.

### 2.3 Escalation reason precedence cases

Add a matrix covering:

- Explicit human request beats general distress.
- Trust/safety urgency beats intake completion.
- Payment dispute beats generic Delivery OOS.
- Clarification budget exhaustion beats generic turn budget exhaustion.
- Intake completion for UC-H/UC-J/UC-K is used only when no stronger safety/dispute trigger exists.

### 2.4 Routing ambiguity cases

Add one-turn and multi-turn cases for:

- Login/password/wrong email -> UC-D.
- Ad visibility/moderation status -> UC-A.
- Reply/message delivery -> UC-C.
- Contact email vs login email vs app notification sync -> UC-D or UC-A depending product decision.
- Phone number visibility/contact option -> UC-E or UC-B depending product decision.
- Seller payment guidance -> UC-F.
- Deletion appeal vs deletion explanation -> UC-H vs UC-FP.
- Delivery scam vs delivery logistics vs delivery refund dispute -> UC-J/UC-E vs OOS vs UC-I.

### 2.5 Fixed-script and case-creation cases

- UC-H, UC-J, UC-K intake completes required fields before handover.
- Runtime creates a case exactly once before handover.
- Handover summary contains the collected fixed-script fields.
- Safety/fraud case can escalate early when risk is high, without forcing full intake.

### 2.6 Tool policy cases

- Agent cannot expose or call human-only tools for refunds, account mutations, moderation enforcement, GDPR deletion, or payment actions.
- Agent-visible tools are constrained by active UC.
- Misrouting should not unlock unsafe tools.
- Tool errors should not be represented as successful customer-facing actions.

### 2.7 Knowledge retrieval cases

- Retrieval should find existing article coverage for known KB-backed topics.
- Retrieval should fail closed when active UC filter hides relevant articles.
- Multi-UC articles should be returned for all intended UCs.
- Missing smoke-relevant topics should be tracked as content gaps, not mapping gaps.

## 3. Weak Rubric Dimensions

### 3.1 Composite score is too opaque for development

The all-or-nothing L1/L2 gate is appropriate for release blocking, but it hides partial progress during development. In this run, many cases have non-zero outcome and judge scores but composite `0.0`.

Recommended change:

- Keep release PASS strict.
- Add a diagnostic score that reports independent buckets: infra, contract, routing, outcome, handover, citation, LLM quality.

### 3.2 Source citation metric checks presence, not citation quality

The current source gate appears to check whether sources exist, not whether the cited sources actually support the answer.

Recommended change:

- Split into `citation_required`, `citation_present`, and `citation_supports_answer`.
- Use retrieval/article IDs plus answer spans for support checks where possible.

### 3.3 Groundedness can reward non-answers

Generic escalation messages can receive high groundedness because they make no factual claims. That is logically defensible but weak as a customer-service measure.

Recommended change:

- Add an "answer usefulness" or "actionability" dimension separate from groundedness.
- For resolvable FAQ cases, penalize generic escalation even if it is grounded.

### 3.4 Handover completeness is binary and shallow

The current gate checks required fields but does not measure whether the handover is useful to a human agent.

Recommended change:

- Add handover quality dimensions: concise issue summary, customer objective, identifiers collected, attempted resolution, reason for escalation, no unnecessary PII, and next action.

### 3.5 Policy compliance is too narrow

Policy compliance is `1.0` in a run where the agent misroutes payment/dispute/safety cases, times out at session creation, and escalates incorrectly. The metric is probably catching only severe violations.

Recommended change:

- Add policy-specific checks for payment/refund disclaimers, fraud/safety escalation urgency, privacy minimization, human availability, and prohibited promises.

### 3.6 No customer-effort metric

The rubric does not directly measure whether the user had to repeat themselves, got a clear next step, or was blocked from human escalation.

Recommended change:

- Add dimensions for repeated-information burden, directness, next-step clarity, and escalation availability.

### 3.7 Latency and resilience are not first-class quality dimensions

`cs_interactive_066` failed with a session creation timeout, but the summary does not elevate latency as a product-quality metric.

Recommended change:

- Track session create latency, first response latency, tool latency, timeout fallbacks, and retry behavior.

## 4. Agent Design Problems

### 4.1 LLM is responsible for deterministic control decisions

Exact UC, exact escalation reason, fixed-script completion, and side-effect ordering are currently too dependent on model behavior. These are better handled by deterministic runtime logic.

Recommended design:

- LLM handles language understanding, summarization, and user-facing phrasing.
- Runtime owns routing constraints, phase transitions, canonical escalation reasons, handover creation, and case creation.

### 4.2 Session lifecycle mixes initialization and resolution

The system appears to do resolution work during session creation. That creates timeout risk and makes the first visible bot turn hard to reason about.

Recommended design:

- `createSession()` only initializes state.
- First user turn enters routing/discover/resolution.
- Any proactive greeting should be static and not tool-backed.

### 4.3 Routing, prompt, and KB labels are not synchronized

Routing prompt, KB mapping, and case specs do not consistently describe the same UC semantics. The UC-A/UC-D contradiction is the clearest example.

Recommended design:

- Generate routing prompt examples from one authoritative UC registry.
- Add a CI check that catches prompt examples pointing to the wrong UC.

### 4.4 Hard OOS is too coarse

Topic subject "Delivery" is treated as handover/out-of-scope before the runtime considers whether the actual issue is a payment dispute, fraud/scam, or product-safety issue.

Recommended design:

- Use topic subject as a prior, not a hard override.
- Apply semantic overrides for high-risk or in-scope exception patterns.

### 4.5 Fixed-script intake conflicts with safety escalation

The prompt says trust/safety urgency beats `intake_complete_for_uc_j`, but runtime plans can still steer UC-J toward intake-complete escalation.

Recommended design:

- Build explicit precedence into runtime reason resolver.
- Allow early safety escalation when required fields are incomplete but risk is high.

### 4.6 Tool loop does not reliably convert retrieval into an answer

Some cases retrieve or classify but end in `turn_budget_exhausted` rather than a final answer. This suggests the loop lacks a deterministic post-tool answer fallback.

Recommended design:

- After successful KB retrieval, force an answer generation step.
- If answer generation fails, escalate with a specific recoverable reason and complete handover.

## 5. Tool-Use Risks

### 5.1 Misrouting changes tool access

Tool policy depends on active UC. If routing is wrong, the agent may lose access to needed tools or gain access to irrelevant tools.

Risk:

- Correct answer is impossible after a wrong UC filter.
- Unsafe tools may become available if future policies expand.

Minimal mitigation:

- Add routing confidence and re-route checks before tool execution.
- Keep human-only and sensitive tools inaccessible regardless of UC.

### 5.2 Search is filtered by active UC

UC-filtered retrieval is good for precision, but a wrong active UC can hide the right article. This is likely affecting several FAQ cases.

Minimal mitigation:

- When confidence is low or retrieval returns weak results, allow a controlled cross-UC retrieval probe.
- Do not expose cross-UC results unless they pass relevance checks.

### 5.3 Side-effect tools should not be LLM-ordered

Case creation, handover, refunds, enforcement, account mutation, GDPR actions, and follow-up emails are side effects. The LLM should not decide their ordering or execution.

Minimal mitigation:

- Runtime owns side-effect execution.
- LLM can propose a summary and fields, but runtime validates and executes.

### 5.4 Tool timeouts can become user-visible failures

The `session_create_failed` timeout shows that tool/runtime latency can kill a case before the agent responds.

Minimal mitigation:

- Add timeout budgets per phase.
- Use controlled fallback responses for read-only tool failure.
- Never run slow tools during session creation.

### 5.5 Citation metadata can satisfy the system without helping the user

The eval checks `source_ids`, but the customer needs clear, relevant guidance. A hidden or irrelevant source ID can pass a shallow metric while still producing poor support.

Minimal mitigation:

- Tie source IDs to answer claims.
- Prefer visible article references or traceable article titles in the answer where product UX allows it.

## 6. Customer Service Policy Gaps

### 6.1 Human escalation availability

BRD expectations say the customer should be able to request a human at any point. Current behavior sometimes escalates for the wrong reason and sometimes fails before session creation.

Policy gap:

- The runtime needs deterministic "human requested" detection and must not block the user behind extra turns when they explicitly ask for an agent.

### 6.2 Payment, refund, and dispute boundaries

Payment/refund issues are high-stakes and often require specialist or human handling. The bot should give safe general guidance but not promise refunds, adjudicate disputes, or imply Gumtree controls third-party payments.

Policy gap:

- The system needs clearer boundaries between UC-F payment guidance, UC-I payment dispute intake, Delivery OOS, and fraud/safety reports.

### 6.3 Trust and safety urgency

Fraud, scams, dangerous goods, threats, and urgent safety issues should not be reduced to generic intake completion.

Policy gap:

- `trust_safety_required` should override normal intake-complete reasons when risk is clear.
- The handover should include safety-relevant details already collected.

### 6.4 Phone contact and privacy

Users may ask why phone contact is unavailable, hidden, or unsafe. The KB has related safety material, but the product policy needs a direct answer path.

Policy gap:

- Clarify when phone numbers are visible, when sellers choose contact options, and when messaging is preferred for safety/privacy.

### 6.5 Login email, contact email, and messaging email

Existing articles distinguish login email, account/contact email, and communications, but this is not surfaced reliably in the agent behavior.

Policy gap:

- Define a canonical answer for customers seeing multiple emails or missing replies.
- Specify when the bot can answer from KB vs when account lookup or human handover is needed.

### 6.6 Seller payment guidance

There is existing safe-payment content, but seller-specific "how should I get paid" guidance is not clearly routed to UC-F and may be filtered out.

Policy gap:

- Provide a seller-payment policy answer that avoids guaranteeing safety, discourages risky off-platform behavior, and recommends inspect/meet/safe-payment practices.

### 6.7 Out-of-scope Delivery exceptions

Delivery as logistics may be out of scope, but delivery scams and delivery-related payment disputes are not the same as courier support.

Policy gap:

- Define exception handling for Delivery plus scam, refund, chargeback, missing item, legal threat, or seller/buyer dispute signals.

## 7. Recommended Minimal Fixes

### Fix 0: Establish a clean baseline

Scope:

- Use `20260501-215807` as the baseline or rerun once and record the new canonical baseline.
- Add a script/check that reports pass count, mean composite, error count, and guard failures.

Acceptance criteria:

- Baseline summary is reproducible.
- Planning documents no longer mix results from different runs.

### Fix 1: Make session creation deterministic and cheap

Scope:

- Remove LLM/KB/control-kernel calls from `createSession()`.
- Return only initialized session state and static greeting behavior.

Acceptance criteria:

- `cs_interactive_066` no longer fails during session creation.
- New eval asserts no tool/LLM work on `POST /v1/chat/sessions`.

### Fix 2: Repair scoring gates that create false negatives

Scope:

- Update source citation gate to apply only to substantive FAQ answers.
- Ensure handover completeness is emitted for every escalation.

Acceptance criteria:

- `cs_interactive_192` should pass if its only blocker remains source citation false positive.
- FAQ escalation cases no longer fail `L2_GATE_MISSING:handover_completeness`.

### Fix 3: Add server-side escalation reason resolver

Scope:

- Implement deterministic reason precedence:
  - explicit human request -> `user_requested`
  - safety/fraud urgency -> `trust_safety_required`
  - payment/refund dispute -> `payment_dispute_detected`
  - clarification exhausted -> `clarification_budget_exhausted`
  - fixed-script completion -> `intake_complete_for_uc_h/j/k`
  - true loop exhaustion -> `turn_budget_exhausted`
  - plain OOS -> `out_of_scope`

Acceptance criteria:

- `cs_interactive_001`, `011`, `014`, `038`, and `040` use expected canonical reasons when their routing/outcome is otherwise correct.

### Fix 4: Align routing and UC semantics

Scope:

- Fix UC-A/UC-D prompt mismatch.
- Add semantic overrides for Delivery plus dispute/fraud signals.
- Add targeted routing examples for UC-FP deletion compliance, UC-F payment guidance, UC-C replies, UC-E phone/contact options, and UC-A app/ad visibility sync.

Acceptance criteria:

- `cs_interactive_002`, `004`, `015`, `029`, `036`, `095`, and `259` route to expected UCs or produce a documented product-policy exception.

### Fix 5: Runtime-owned case creation and handover

Scope:

- For UC-H/UC-J/UC-K, runtime creates a case exactly once after fixed-script intake or urgent safety trigger.
- Runtime then records complete handover.

Acceptance criteria:

- No missing or duplicate case creation.
- Handover includes collected fields and next action.

### Fix 6: Force answer after successful KB retrieval

Scope:

- For FAQ UCs, successful retrieval should lead to a concise answer with supporting source IDs.
- If retrieval is weak, ask one clarification or escalate with a specific reason.

Acceptance criteria:

- `cs_interactive_004`, `095`, `192`, and `259` do not end as generic `turn_budget_exhausted` after tool use.

### Fix 7: Author or update only the minimum KB content

Do this after Fixes 1-6, otherwise article changes will be hard to evaluate.

Minimum content work:

- Phone/contact option article or update for UC-E/UC-B.
- Email/app/contact/login distinction article or update for UC-D/UC-A.
- Seller receiving payment guidance for UC-F, or retag/update existing safe-payment content so UC-F retrieval can find it.
- Delivery/refund dispute note only if product policy wants a user-facing article; it will not unblock fixed-script-only UC-I cases without routing/intake changes.

Acceptance criteria:

- Retrieval probes find the intended article for each smoke-relevant query.
- Article IDs are returned under the expected UC filters.

## Suggested Phase 3 Execution Order

I recommend a modified ROI sequence:

1. Tier 0 substrate fix: baseline, session creation, citation gate, handover completeness.
2. Smoke.
3. Tier 1 deterministic routing/reason resolver.
4. Smoke.
5. Tier 2 resolve-before-escalate loop improvements.
6. Smoke.
7. Tier 3 minimal KB articles/retagging.
8. Smoke.
9. Tier 4 prompt few-shots only for remaining ambiguous failures.

Expected result:

- Tier 0 + Tier 1 should make `5-6/14` plausible if the eval gates are repaired.
- Tier 2 + Tier 3 are needed to reach `6-8/14`.
- Prompt-only T1 is not recommended because exact enum, handover, case creation, and timeout behavior are runtime responsibilities.
