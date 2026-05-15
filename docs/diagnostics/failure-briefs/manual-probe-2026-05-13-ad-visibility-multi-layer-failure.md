# Failure Brief — manual-probe 2026-05-13 ad-visibility multi-layer failure

> **Source:** Manual probe trace, captured 2026-05-13 ~18:05 BST.
> **Trace ID:** `4a2f3680-02a8-4d13-8f0b-f99d7c249b57` (session_id).
> **CaseSpec:** N/A (manual probe; no rule-extracted CaseSpec, no L3 override). Ground truth derived from the bot's own RESOLVE phase `system_instruction` and from phase 2 UC-A policy.
> **Run:** single-session manual probe, not part of the smoke set.
> **Filed:** 2026-05-13 (Sprint 18 / G1, brief #10 — first manual-probe brief; brings G1 total to 9 smoke + 1 manual-probe = 10 briefs).
> **Note:** Per Q1 = framing C answer (2026-05-13), this brief is intentionally separate from the 9 smoke-set briefs so G2 case-family construction can distinguish smoke-set anchors from manual-probe observations.

## Ground-truth chain

This is a **manual probe trace**, not a generated CaseSpec. There is no rule-extracted L1 spec, no L2 LLM persona review, no L3 approved override. The trace was captured today against the current dev runtime by entering the chat with form fields `ad_id=ad-1003`, `email=ee@eee.com`, `first_name=eee`, `description=x`, `topic_subject=Ad Support`, listing context `ad_id=AD-1001` (note form/listing ad_id mismatch).

Ground truth here is derived from two sources that ARE authoritative:

1. **The bot's own RESOLVE phase `system_instruction`** (visible in the trace's `phase_plan` field on Turn 2):
   > "FAQ-path RESOLVE flow (S1): the intended terminal sequence is search_knowledge -> resolve_article -> grounded customer-facing answer (with a source_id citation) -> record_outcome. Only escalate via request_handover after a valid resolve attempt cannot complete (no viable hit, or resolve_article could not produce a grounded answer)."
   >
   > "If tool data contains specific information about the user's case (account/ad/moderation), answer from that first. For policy/process explanations, you MUST call search_knowledge first if accumulated_tool_results.search_knowledge is empty; do not produce a factual customer-facing answer without grounded knowledge evidence. **After search_knowledge returns a viable hit, you MUST call resolve_article for the top hit before answering the customer; cite the source_id in your user_message.** If search_knowledge returns no viable hit, request_handover with escalation_reason='faq_miss_threshold_exceeded' is allowed."

2. **Phase 2 UC-A (Listing visibility / Top Ad)** is `allow_bot_resolution`-true and faq-resolvable.

The trace shows the bot violated *both* clauses of the RESOLVE `system_instruction` MUST clause: (a) it did not call `resolve_article` for the top hit, and (b) it called `request_handover(faq_miss_threshold_exceeded)` despite `search_knowledge` returning three viable hits (all with score=5 and `faq_miss: false`).

## What happened?

The 2-turn trace:

- **T1 user** (form-supplied): "why I can't see my advert"
- **T1 bot:** "Hi eee, I understand you're having trouble seeing your advert. Could you please provide the ad ID or title so I can look into this further?" (DISCOVER → DISCOVER, classified as needing more evidence)
- **T2 user:** "ad-1003"
- **T2 bot:** "I'm having difficulty resolving this. Let me connect you with a specialist." (DISCOVER → ESCALATE; `active_use_case=UC-A` committed at 0.90)

Bot's T1 response was reasonable (clarification before commit). Bot's T2 response stacks multiple failures:

1. **`faq_miss_threshold_exceeded` self-stamped despite `faq_miss: false`.** The trace's `accumulated_tool_results.search_knowledge` block on T2 shows three viable hits (all score=5):
   - `ka4P200000004ZtIAI` — "I Can't Find My Ad"
   - `ka4P2000000060bIAA` — "Where Is My Ad?"
   - `ka41r000000LIEJAA4` — "(temp) Ad removed - By CS (general)"
   plus the explicit flags `faq_miss: false`, `answer_miss: false`, `retrieval_miss: false`, `retrieved_but_unresolved: true`. The first two hits are direct, relevant UC-A answers. The bot stamped `escalation_reason=faq_miss_threshold_exceeded` anyway — **a direct semantic contradiction between the bot's claim and the runtime data the bot received in its own projection**.

2. **RESOLVE phase MUST-call-resolve_article violated.** The `system_instruction` for RESOLVE is explicit: "After search_knowledge returns a viable hit, you MUST call resolve_article for the top hit before answering the customer." Three viable hits returned; bot called zero `resolve_article`. The bot's tool sequence: `classify_use_case, get_customer_context, search_knowledge × 3, request_handover`.

3. **Runtime orchestrator executed `search_knowledge` three times for one LLM request.** Per the user's analysis of the trace, the T2 LLM Raw Response shows exactly one `search_knowledge` tool_call requested:
   ```
   "tool_calls": [{
     "name": "search_knowledge",
     "arguments": {"query": "why can't I see my advert", "uc_tags": ["UC-A", "UC-B"]}
   }]
   ```
   The trace shows three `search_knowledge` executions, with identical params (`query="why can't I see my advert"`, `uc_tags=["UC-A","UC-B"]`) and identical return payloads (same 3 source_ids, same scores). Each call took ~1146–1275 ms. Plausible root causes (per user's analysis): (a) phase transition DISCOVER → RESOLVE triggered RESOLVE init logic that re-queued the pending call; (b) the Turn 1 failed `search_knowledge` call (failed with "Tool 'search_knowledge' is not allowed for use case 'none'" because no UC was committed yet) got replayed once tools were unlocked post-classify_use_case; (c) the LLM's new T2 request. The de-duplication on `accumulated_tool_results` merged the result to one entry, but the execution layer did not de-duplicate the calls themselves — three real API round-trips were made.

4. **ad_id mismatch detected internally but not surfaced to the user.** The T1 LLM reasoning explicitly notes: "The form context has ad_id 'ad-1003' but the listing context shows ad_id 'AD-1001'." The bot asked the user to confirm the ad ID. The user replied "ad-1003" (the form value). The bot then called `get_customer_context` with `ad_id=ad-1003` but the returned listing context still shows `AD-1001`. The bot did not relay this mismatch to the user, who was left with an unaddressed data inconsistency about their own ad's identifier.

5. **Same mechanical-template surface as Cluster B.** T2's user-facing message — "I'm having difficulty resolving this. Let me connect you with a specialist." — is the same template seen in cs_001 / cs_011 / cs_038 / cs_040 / cs_015 / cs_192 / cs_259.

## What should a good CS agent have done?

The capability gap is **runtime-evidence-bound escalation reasons, explicit phase-plan compliance, and state-aware acknowledgment of data inconsistencies** the bot already detected internally.

Concretely:

- **Run the RESOLVE sequence as the system_instruction specifies.** Three viable search hits were returned; the bot must call `resolve_article` on the top hit (`ka4P200000004ZtIAI` — "I Can't Find My Ad") and produce a grounded customer-facing answer citing the article. The MUST clause is explicit in the prompt.

- **Stamp escalation_reason against actual session evidence.** `faq_miss_threshold_exceeded` is allowed *only* when search returns no viable hit. Three viable hits is the opposite of that condition. If the bot genuinely couldn't produce an answer for some other reason (e.g. ambiguity, ad_id mismatch unresolvable), the right reason is a different family — possibly `user_requested` if the user later demands handover, or a more specific reason if one fits. The current behavior is a runtime-contract violation (same root as cs_040 `intake_complete_for_uc_k` and cs_176 `faq_miss_threshold_exceeded` without search evidence).

- **Surface the ad_id mismatch back to the user.** The bot saw `form.ad_id=ad-1003` vs `listing.ad_id=AD-1001` on T1; the LLM reasoning even named it. A real reply would have said "I have your ad ID as ad-1003 but I'm seeing AD-1001 in our system for an iPhone 15 Pro listing — does that sound right, or is one of these wrong?" instead of just "could you please provide the ad ID or title". The bot detected state inconsistency and asked for clarification, but didn't share the actual signal it detected.

- **Avoid the fixed-template fallback.** The bot did real tool work (classify + get_customer_context + 3× search returning 3 hits). The user-facing message should reflect that, not collapse into the same generic-difficulty template that cases with zero useful tool work (cs_001) produced.

## Why does this matter?

This trace is the **highest-density runtime-evidence-contract violation observed in the G1 corpus**. It shows three simultaneous gaps that have been observed separately in smoke briefs but appear together here:

- The self-stamped `faq_miss_threshold_exceeded` despite `faq_miss: false` is **more egregious than cs_176** (where the bot stamped faq_miss without confirmed search evidence either way) because the runtime data the bot received explicitly says `faq_miss: false`. The contradiction is direct and on the same projection. Three instances now confirm `R-escalation-reason-runtime-evidence-contract-review` (cs_040 + cs_176 + this trace) as systematic across three UCs and three reasons.

- The RESOLVE phase MUST-call-resolve_article violation is the **second observed instance of the bot ignoring an explicit phase-plan directive** (cs_259's Sprint 7 §I0 violation was the first). Per the user's prior decision, this is NOT yet an R-item — more controlled testing across shapes is needed before opening a backlog item on planner instruction-following. The second instance does strengthen the observation but does not cross the bar.

- The orchestrator duplicate-execution finding is **the first runtime / infrastructure-level bug surfaced in the G1 corpus**, distinct from all the prompt_projection / semantic_planner findings in the smoke briefs. It is independently actionable (orchestrator de-dup is a clear engineering task) and has a measurable cost (3× ~1.2s ≈ 2.5s wasted latency per occurrence, plus 3× the backend search load).

- The ad_id mismatch finding hints at a UC-A / state-aware-investigation gap similar to cs_095's "name the verified state back to the user" capability, but applied to a different surface (form-vs-listing data inconsistency) on a different UC.

## Is this a one-off or a pattern?

`pattern` reinforcement on three dimensions, `single-instance` on two:

- **Self-stamped escalation_reason without runtime evidence** (cs_040 + cs_176 + this trace): pattern, 3 instances across 3 UCs and 2+ reason names.
- **Bot ignores explicit phase-plan directive** (cs_259 §I0 + this trace's RESOLVE MUST clause): 2 observed instances, **still below the user's bar for opening an R-item** — needs controlled multi-shape testing.
- **Mechanical-template surface on faq_miss escalation** (multiple smoke cases + this trace): pattern.
- **Runtime orchestrator duplicate tool execution** (this trace only): single-instance, but the failure shape (1 LLM request → 3 identical executions, deterministic input/output) suggests an orchestrator-code bug rather than an LLM-side stochastic issue; replication should be straightforward. NEW R-item.
- **ad_id form-vs-listing mismatch** (this trace only): single-instance; whether this is a common shape in production traffic or a probe-specific artifact needs production data. Not opening an R-item on n=1.

## Which layer is likely responsible?

Three layers stack here:

- **`semantic_planner`** (primary, two distinct failures). (a) The LLM ignored the RESOLVE `system_instruction` MUST clause to call resolve_article on viable hits. (b) The LLM stamped `faq_miss_threshold_exceeded` despite `faq_miss: false` in its own projection. Both are planner decisions made against the evidence the planner had access to.

- **`infra` / runtime orchestrator** (NEW, distinct from the smoke briefs). The orchestrator executed `search_knowledge` three times for one LLM request, with identical params and identical results. This is a deterministic orchestration-code bug — not a semantic failure. It belongs in the §3 `infra` layer per `iteration_governance.md` §3.2 Q1, not in the semantic layers. NEW R-item `R-runtime-orchestrator-tool-call-deduplication`.

- **`prompt_projection`** (secondary). The mechanical-template surface ("I'm having difficulty resolving this. Let me connect you with a specialist.") is the same Cluster B pattern; same projection root.

## What should NOT be done?

- Do **not** add a Java guard refusing `request_handover(faq_miss_threshold_exceeded)` when `accumulated_tool_results.search_knowledge.faq_miss == false`. This is exactly the right shape of fix in principle (evidence-bound reason gate), but it should go through the `R-escalation-reason-runtime-evidence-contract-review` runtime-contract review, not be patched per-case. Scoping the contract carefully matters — `user_distress` is already gated by Sprint 4 §E1, `user_requested` is subjective and shouldn't be gated, etc.
- Do **not** patch the orchestrator dup-execution issue by adding a per-call hash deduplication in the orchestrator without first investigating root cause. The user's three hypotheses (phase-transition re-trigger, failed-call replay, LLM new request — all summing to 3) are plausible; the right fix depends on which (or which combination) is happening. Probably worth a focused infra investigation sprint candidate.
- Do **not** open `R-prompt-phase-plan-directive-followship` from this trace alone — per the user's prior decision, controlled multi-shape testing is the gating evidence requirement, and this trace adds a second observation but does not constitute the testing the user asked for.
- Do **not** treat the ad_id mismatch as a CaseSpec quirk. The trace is real-world; the form / listing data store inconsistency is a production data issue, not a CaseSpec issue.
- Do **not** add a regex on "why can't I see my advert" → UC-A response template. Keyword chatbot regression.

## Related observation — one new R-item, two reinforcements, one open observation strengthened

1. **`R-runtime-orchestrator-tool-call-deduplication`** (NEW, `infra` layer per §3.2 Q1; orchestrator de-dup / idempotency). Per user's Q3=OK answer, opened as a new backlog item. Recommended scope: investigate root cause among the three hypotheses (phase-transition re-trigger, Turn 1 failed-call replay, LLM new request) before deciding remediation; document the orchestrator's intended de-duplication / idempotency contract; add a regression test that fires `search_knowledge` once in a similar trace and asserts ≤1 execution.

2. **`R-escalation-reason-runtime-evidence-contract-review`** (reinforced; now 3 instances across cs_040 / cs_176 / this trace). The runtime-contract gap is no longer "potentially systematic" — three instances across three UCs and two reason names (intake_complete_for_uc_k, faq_miss_threshold_exceeded ×2) make it solidly systematic. Strengthens the Tier-0 candidate case.

3. **Open observation — bot ignores explicit phase-plan directives** (cs_259 Sprint 7 §I0 + this trace's RESOLVE MUST-call clause). Two observed instances, still below the user's bar for opening an R-item. Awaits controlled multi-shape testing during G2 case-family construction or a small targeted probe sprint.

4. **`R-ad-id-form-vs-listing-data-consistency`** (single-instance; not opened on n=1). The form-supplied `ad_id=ad-1003` vs listing context `ad_id=AD-1001` mismatch is a real-world data-source inconsistency. Whether this is a common shape in production traffic or a probe-specific artifact needs production data before opening a backlog item. Note in this brief; flag for monitoring during real-traffic observation but not yet a tracked item.
