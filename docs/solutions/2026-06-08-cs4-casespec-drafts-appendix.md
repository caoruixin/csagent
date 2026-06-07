---
title: CS4 CaseSpec drafts (Part B seed set) — appendix to the entity-context proposal
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file + parent proposal
authored_by: research-agent
authored_date: 2026-06-08
mode: bad-case-driven
supersedes: []
superseded_by: null
notes: >
  Appendix to docs/solutions/2026-06-07-cs3-cs4-discover-stall-uc-fp-boundary-empty-trace-ux.md.

  Drafts 7 CaseSpecs (5 NEW + 2 EXTEND) that operationalize the four
  acceptance criteria stated by human 2026-06-07 for CS4. Schema +
  L2 outcome_check capability constraints from the 2026-06-08
  CaseSpec-schema investigation are reflected:

    - Path γ adopted: strict `expected_tool_sequence` + `correct_uc` +
      `correct_outcome` outcome_checks; `bot_handling_pattern` and
      `closure_criterion` carry the nuanced rubric for L3 judge;
      `forbidden_tools` carries hard negative for anti-误杀 cases;
      `answer_must_not_contain` carries text-level negative pins.
    - OR semantics across verification paths (lookup vs ask) handled
      via persona seed_messages designed so BOTH valid paths converge
      to the same expected_tool_sequence after turn 2; the LLM's
      choice (ask first vs call lookup with missing identifiers) is
      preserved without forcing one specific tool order.

  Deliverable usage: dev-agent at S-Y-CS4 Part B copies sections 2-6
  into `eval_interactive/case_specs/bad_cases/<case_id>.yaml` (one
  file each); sections 7-8 are diffs to existing files at
  `eval_interactive/case_specs/bad_cases/`. Section 9 is the
  activation prompt template for re-spawning a research-agent on
  similar follow-up work.

  ALL drafts are **proposal**, not binding. Deliver-agent + dev-agent
  may edit during §5.6 bad-case suite tiering review per
  `docs/current/process/badcase-lifecycle.md`. The closure_criterion
  + bot_handling_pattern fields are the human-readable contracts the
  L3 judge + human review consume.
---

# CS4 CaseSpec drafts (Part B seed set) — appendix

## 1. How to use this appendix

| Section | Content | Usage |
|---------|---------|-------|
| §2 | NEW `cs_uc_a_no_ad_id_ad_specific.yaml` | Copy to `case_specs/bad_cases/` |
| §3 | NEW `cs_uc_a_generic_policy_question.yaml` (anti-误杀) | Copy to `case_specs/bad_cases/` |
| §4 | NEW `cs_uc_a_loaded_listing.yaml` | Copy to `case_specs/bad_cases/` |
| §5 | NEW `cs_uc_fp_loaded_moderation.yaml` | Copy to `case_specs/bad_cases/` |
| §6 | NEW `cs_uc_a_lookup_failed.yaml` | Copy to `case_specs/bad_cases/` |
| §7 | EXTEND `alice_uc_a_uc_h_misclass.yaml` diff | Apply diff to existing |
| §8 | EXTEND `wmkb_uc_a_trader_flag_secondary_uc_h.yaml` diff | Apply diff to existing |
| §9 | Research-agent activation prompt template (for human) | Reuse for similar follow-ups |

### 1.1 Schema-capability framing (Path γ)

Per the 2026-06-08 schema investigation, the current outcome_check
schema **does not directly support** "tool-call ordering positional
pin / bot-message-shape pin / check-level OR / negative outcome_check".
These drafts therefore use Path γ:

- **Positive tool-call expectation** → `expected_tool_sequence` (LCS
  scoring; advisory) + `outcome_checks: [correct_uc, correct_outcome,
  tool_sequence_match]`.
- **OR across verification paths (lookup vs ask)** → designed
  via persona `seed_messages`: turn-1 user provides minimal info; if
  bot asks for ad_id, user provides it on turn 2; persona behaves the
  same whether bot asked or directly attempted lookup. The downstream
  `expected_tool_sequence` is the same.
- **Negative pin (anti-误杀)** → `forbidden_tools` (hard check) +
  `answer_must_not_contain` (text negative) + the `closure_criterion`
  + L3 judge rubric.
- **Closure rubric** → `closure_criterion` and `bot_handling_pattern`
  are the human-readable contracts. The L3 judge consumes these
  alongside the persona; human review uses them at §5.6 tiering.

### 1.2 Tier assignment per `docs/current/process/badcase-lifecycle.md` §5.6

All 5 NEW drafts: **Tier 1 (target)** for `cs_uc_a_no_ad_id_ad_specific`,
`cs_uc_a_loaded_listing`, `cs_uc_a_lookup_failed`; **Tier 1
(negative-control / anti-误杀)** for `cs_uc_a_generic_policy_question`;
**Tier 2 (neighbor)** for `cs_uc_fp_loaded_moderation`.

Both EXTEND drafts retain their existing tier (already in bad_cases
suite at Tier 1) and gain stronger `outcome_checks`.

---

## 2. NEW — `cs_uc_a_no_ad_id_ad_specific.yaml` (Tier 1 target)

```yaml
case_id: cs_uc_a_no_ad_id_ad_specific
source_session_id: synthetic-cs4-cs_uc_a_no_ad_id_ad_specific-2026-06-08
source_dataset: bad_case_authored

bad_case_metadata:
  surfaced_by: research-agent (CS4 proposal Part B; root-cause OBS-S1 entity-context gap)
  surfaced_date: 2026-06-08
  failure_shape: |
    User asks an ad-specific question (e.g. "why isn't my ad showing
    up in search?") with NO ad_id in form_context. Bot today proceeds
    directly to search_knowledge and returns a generic FAQ answer
    (e.g. "check the article on My Ad Visibility") without verifying
    which specific ad the user means. The generic answer is likely
    wrong for the user's actual case and reads as guessing.
  layers_involved: |
    prompt_projection (resolve_faq_grounded_answer.yaml procedure
    has no mandatory verify-entity-context critical step for UC-A);
    semantic_planner (LLM does not act on the observable
    customer_context_status: missing_ad_id signal)
  related_dimensions: [D1, D5]
  related_r_items:
    - OBS-S1 (UC-A / UC-H / UC-J verify-entity-context procedure;
      promoted from autoloop-deferred to active via this CaseSpec
      seed)
  tier: tier_1_target

closure_criterion: |
  Bot recognises within turn 1 that it cannot answer this
  ad-specific question without knowing which specific ad. Bot
  EITHER:

  (a) Asks one focused clarifying question for the ad reference
      (ad_id / ad title / ad URL / "which ad"), then on turn 2
      after the user provides the ad_id calls get_customer_context
      and proceeds with a grounded answer based on listing data; OR

  (b) Calls a listing lookup tool (get_customer_context or
      lookup_listing_or_ad) attempt that fails because no ad_id
      is available, recognises the failure, and asks for the
      ad reference, then proceeds on turn 2 as in (a).

  FAIL conditions: bot proceeds to search_knowledge BEFORE
  verifying the ad reference and emits a generic FAQ answer; or
  bot continues asking other clarifying questions (UC vs UC,
  understand vs appeal) without ever asking for the ad
  reference; or bot escalates user_requested or
  clarification_budget_exhausted within 4 turns when the
  user is cooperatively providing the ad_id on request.

form_context:
  first_name: Sam
  email: sam.no.ad.id@example.com
  topic_subject: Ad Support
  ad_id: ''
  description: |
    My ad isn't showing up in search results. I posted it
    yesterday but I can't find it when I search for the title.

persona:
  user_goal_summary: |
    User has posted a live ad and believes it should appear in
    search but cannot find it. User has the ad_id available
    (AD-3001) and will share it on request. User is cooperative,
    mildly puzzled, willing to follow the bot's clarifying steps.
  frustration_level: mild
  verbosity: normal
  drift_behavior: none
  seed_messages:
    - "Hi, my ad isn't showing up in search results. I posted
       it yesterday but I can't find it when I search for the
       title."
    - "Oh sure, the ad ID is AD-3001."
    - "Yeah, that's the one. Thanks for checking."
  hidden_facts:
    - fact: "User's ad_id is AD-3001; the listing is live but
        outside the user's selected location filter (a typical
        UC-A explainable cause). The bot CAN reach a grounded
        answer once it loads the listing."
      disclose_when: |
        if the bot asks for the ad_id or for any identifier
        that can resolve to AD-3001
  will_request_human_if: |
    the bot answers with a generic FAQ article about ad
    visibility without asking which ad the user is talking
    about; or the bot loops asking unrelated clarifying
    questions (understand vs appeal) instead of asking for
    the ad reference

expected:
  outcome_class: resolve
  primary_uc: UC-A
  secondary_ucs: []
  should_escalate: false
  allow_bot_resolution: 'true'
  bot_handling_pattern: |
    Turn 1: bot recognises that without an ad reference it cannot
    give a useful answer. Bot asks ONE focused clarifying
    question for the ad_id (or attempts a listing lookup that
    fails on missing identifiers and asks). Bot does NOT emit
    a generic FAQ answer about ad visibility before verifying
    the ad.

    Turn 2: user provides AD-3001. Bot calls get_customer_context
    with the ad_id, retrieves the listing, sees the location-
    filter explanation in the listing data, and answers with
    grounded reference to the user's specific ad.

    Turn 3: bot confirms resolution (record_outcome).
  escalation_trigger: null
  risk_level: low
  expected_tool_sequence:
    - classify_use_case
    - get_customer_context
    - search_knowledge
    - resolve_article
    - record_outcome
  forbidden_tools:
    - create_case_controlled
    - get_message_moderation_context
  grounding_mode: listing_data_or_faq_backed
  answer_must_not_contain:
    - I have updated your ad
    - I've fixed the visibility
    - I have resolved your issue
  max_turns: 6

scoring:
  hard_checks:
    - phase_transition_validity
    - no_critical_policy_violation
    - no_pii_leakage
    - no_stall
    - no_human_only_tool_exposure
    - no_forbidden_tools
    - budget_enforcement
    - grounding_compliance
  outcome_checks:
    - correct_uc
    - correct_outcome
    - tool_sequence_match
  llm_judge_dimensions: []
```

---

## 3. NEW — `cs_uc_a_generic_policy_question.yaml` (Tier 1 anti-误杀 negative-control)

```yaml
case_id: cs_uc_a_generic_policy_question
source_session_id: synthetic-cs4-cs_uc_a_generic_policy_question-2026-06-08
source_dataset: bad_case_authored

bad_case_metadata:
  surfaced_by: |
    research-agent (CS4 proposal Part B; ANTI-误杀 binding gate for
    autoloop Part C output — autoloop's procedure candidate MUST
    NOT force ad_id elicitation on genuinely-generic questions)
  surfaced_date: 2026-06-08
  failure_shape: |
    User asks a genuinely generic policy / how-it-works question
    (no specific ad referenced). The autoloop's CS4 fix must not
    over-correct by forcing ad_id elicitation on all UC-A turns;
    generic questions should still get a direct FAQ-grounded
    answer without unnecessary clarification.
  layers_involved: |
    anti-误杀 binding gate for prompt_projection / semantic_planner
    Part C output; protects §1.2 flexibility-over-rigidity
  related_dimensions: [D1, D5]
  related_r_items:
    - OBS-S1 anti-误杀 (paired negative-control with
      cs_uc_a_no_ad_id_ad_specific)
  tier: tier_1_negative_control

closure_criterion: |
  Bot recognises that the question references no specific ad and
  proceeds DIRECTLY to a grounded FAQ-backed answer.

  PASS conditions:
    (a) Bot calls classify_use_case → search_knowledge →
        resolve_article → record_outcome (or comparable FAQ
        sequence) without first eliciting ad_id;
    (b) Bot's user_message answers the generic question with
        grounded references to FAQ articles on search ranking /
        ad visibility mechanics;
    (c) `correct_outcome: resolve`.

  FAIL conditions:
    (d) Bot asks for ad_id or "which ad" — the question is
        generic and the elicitation is unnecessary friction;
    (e) Bot escalates because it could not pin entity context;
    (f) Bot answers with empty / placeholder content because it
        was waiting for entity verification.

form_context:
  first_name: Jordan
  email: jordan.generic@example.com
  topic_subject: Ad Support
  ad_id: ''
  description: |
    How does Gumtree's search ranking work? What makes some
    ads appear higher than others?

persona:
  user_goal_summary: |
    User wants to understand the platform's search-ranking
    mechanics in the abstract — not asking about any specific
    ad they own. User is curious, cooperative, will not provide
    an ad_id even if asked (they don't have one in mind for
    this question).
  frustration_level: calm
  verbosity: brief
  drift_behavior: none
  seed_messages:
    - "How does Gumtree's search ranking work? What makes some
       ads appear higher than others?"
    - "I'm not asking about a specific ad of mine, just curious
       how it works in general."
    - "Thanks, that's what I needed."
  hidden_facts: []
  will_request_human_if: |
    the bot demands an ad_id when the question is clearly
    generic; or the bot fails to answer a question that is
    well-covered by public FAQ on search ranking

expected:
  outcome_class: resolve
  primary_uc: UC-A
  secondary_ucs:
    - UC-E
  should_escalate: false
  allow_bot_resolution: 'true'
  bot_handling_pattern: |
    Turn 1: bot recognises the question is generic (no ad
    referenced; explicit "in general"). Bot proceeds directly to
    search_knowledge for search-ranking content.

    Turn 2: bot delivers a grounded answer citing FAQ on search
    ranking / ad visibility mechanics. Bot does NOT ask for
    ad_id.

    Turn 3: bot confirms resolution (record_outcome).
  escalation_trigger: null
  risk_level: low
  expected_tool_sequence:
    - classify_use_case
    - search_knowledge
    - resolve_article
    - record_outcome
  forbidden_tools:
    - request_handover
    - create_case_controlled
  grounding_mode: faq_source_backed
  answer_must_not_contain:
    - Could you share your ad ID
    - Which ad are you asking about
    - What's your ad_id
  max_turns: 4

scoring:
  hard_checks:
    - phase_transition_validity
    - no_critical_policy_violation
    - no_pii_leakage
    - no_stall
    - no_human_only_tool_exposure
    - no_forbidden_tools
    - budget_enforcement
    - grounding_compliance
  outcome_checks:
    - correct_uc
    - correct_outcome
    - tool_sequence_match
    - turn_efficiency
  llm_judge_dimensions: []
```

---

## 4. NEW — `cs_uc_a_loaded_listing.yaml` (Tier 1 target)

```yaml
case_id: cs_uc_a_loaded_listing
source_session_id: synthetic-cs4-cs_uc_a_loaded_listing-2026-06-08
source_dataset: bad_case_authored

bad_case_metadata:
  surfaced_by: research-agent (CS4 proposal Part B; criterion #3 — loaded customer_context must be used before FAQ)
  surfaced_date: 2026-06-08
  failure_shape: |
    Form contains both email + ad_id; pre-chat auto-trigger at
    FormContextIngestionService.java:93-142 successfully loads
    customer_context (listing + account data). Bot today may
    still call search_knowledge first and ignore the loaded
    listing data, producing a generic FAQ answer that doesn't
    reference the user's specific ad.
  layers_involved: |
    prompt_projection (resolve_faq_grounded_answer.yaml procedure
    does not bind to customer_context_status: loaded as a
    consume-first signal); semantic_planner (LLM may ignore
    pre-loaded data in favor of FAQ retrieval)
  related_dimensions: [D1, D6]
  related_r_items:
    - OBS-S1 (entity-context verification for UC-A)
  tier: tier_1_target

closure_criterion: |
  Bot recognises that customer_context is already loaded and
  USES the listing data in its grounded answer.

  PASS conditions:
    (a) Bot's user_message references listing-specific data
        (title, status, category, location, price, posted_date
        — at least one specific field);
    (b) Bot proceeds to a grounded answer that addresses the
        user's specific ad situation (e.g., "your listing
        AD-3050 is in category Furniture and currently shows
        as ACTIVE; visibility looks normal — possible reasons
        you can't find it: …");
    (c) `correct_outcome: resolve`.

  FAIL conditions:
    (d) Bot answers with a generic FAQ article ignoring the
        loaded listing data;
    (e) Bot redundantly calls get_customer_context again
        (acceptable but suboptimal; passes hard checks but L3
        judge should note inefficiency);
    (f) Bot's response does not reference any listing-specific
        field.

form_context:
  first_name: Riley
  email: riley.loaded@example.com
  topic_subject: Ad Support
  ad_id: AD-3050
  description: |
    My ad isn't performing well. Not many views. Anything I
    can do?

persona:
  user_goal_summary: |
    User has an active live listing (AD-3050) and is asking why
    it's not performing well. The bot's projection has
    customer_context.listing already populated (status=ACTIVE,
    category=Furniture, price/location set). User is cooperative,
    interested in concrete advice.
  frustration_level: mild
  verbosity: normal
  drift_behavior: none
  seed_messages:
    - "Hi, my ad isn't performing well. Not many views.
       Anything I can do?"
    - "Yeah, that's the one — AD-3050."
    - "Thanks, I'll try that."
  hidden_facts:
    - fact: "Listing AD-3050 is active, in Furniture category,
        with photos uploaded. The platform's tips on view
        increase (refresh, photos, price, description quality)
        are the standard advice."
      disclose_when: if the bot asks about specific listing details
  will_request_human_if: |
    the bot answers with a generic article ignoring the
    user's specific listing data; or the bot asks for ad_id
    when ad_id is already in form_context

expected:
  outcome_class: resolve
  primary_uc: UC-A
  secondary_ucs: []
  should_escalate: false
  allow_bot_resolution: 'true'
  bot_handling_pattern: |
    Turn 1: bot reads customer_context.listing (status=ACTIVE,
    category=Furniture, etc. — already loaded in projection).
    Bot answers with grounded advice that references the user's
    specific listing context (e.g. category-specific visibility
    tips) and optionally calls search_knowledge for canonical
    visibility-tips article.

    Turn 2: user confirms or follows up; bot answers.

    Turn 3: bot confirms resolution (record_outcome).
  escalation_trigger: null
  risk_level: low
  expected_tool_sequence:
    - classify_use_case
    - search_knowledge
    - resolve_article
    - record_outcome
  forbidden_tools:
    - create_case_controlled
  grounding_mode: listing_data_and_faq_backed
  answer_must_not_contain:
    - Could you share your ad ID
    - What's your ad_id
    - I've boosted your listing
    - I've made your ad more visible
  max_turns: 5

scoring:
  hard_checks:
    - phase_transition_validity
    - no_critical_policy_violation
    - no_pii_leakage
    - no_stall
    - no_human_only_tool_exposure
    - no_forbidden_tools
    - budget_enforcement
    - grounding_compliance
  outcome_checks:
    - correct_uc
    - correct_outcome
    - tool_sequence_match
  llm_judge_dimensions: []
```

---

## 5. NEW — `cs_uc_fp_loaded_moderation.yaml` (Tier 2 neighbor — UC-FP boundary)

```yaml
case_id: cs_uc_fp_loaded_moderation
source_session_id: synthetic-cs4-cs_uc_fp_loaded_moderation-2026-06-08
source_dataset: bad_case_authored

bad_case_metadata:
  surfaced_by: |
    research-agent (CS4 proposal Part B; UC-FP boundary case;
    paired with cs_uc_a_loaded_listing as neighbor to test
    UC-A → UC-FP routing on removed listing with moderation
    context available)
  surfaced_date: 2026-06-08
  failure_shape: |
    Form has ad_id; listing status=REMOVED; customer_context
    auto-trigger has loaded moderation_review with a specific
    removal reason. Bot today (per CS4 trace c891efb0) may
    classify UC-A and answer with a generic policy article
    instead of UC-FP-routed grounded answer using the actual
    moderation_review.
  layers_involved: |
    prompt_projection (discover_triage.yaml Sprint 33 cue does
    not distinguish UC-A vs UC-FP; resolve_faq_grounded_answer
    UC-FP critical step is mandatory_for UC-FP only — gated
    behind UC-FP being selected first); semantic_planner
    (LLM does not prefer UC-FP when moderation_reason is
    available)
  related_dimensions: [D1, D6]
  related_r_items:
    - OBS-S2 (DISCOVER UC-A vs UC-FP cue; promoted to active
      via this CaseSpec seed)
    - OBS-S1 (entity-context verification — supports use of
      already-loaded moderation_review)
  tier: tier_2_neighbor

closure_criterion: |
  Bot uses the already-loaded moderation_review to give a
  specific grounded answer about WHY the ad was removed,
  rather than guessing from generic FAQ corpus.

  PASS conditions:
    (a) Bot's user_message references the specific moderation
        reason content (e.g. category violation, image quality,
        duplicate listing — whatever the moderation_review
        actually says for AD-3060);
    (b) Bot may classify UC-A or UC-FP — both acceptable
        when moderation_review is consulted (the UC label
        matters less than the grounding source);
    (c) `correct_outcome: resolve`.

  FAIL conditions:
    (d) Bot cites a generic policy article that does not match
        the moderation_review's actual reason (the CS4 trace
        c891efb0 failure shape — cited "Paying to Rehome Your
        Pet" when the actual reason might differ);
    (e) Bot ignores customer_context.moderation_review
        entirely and proceeds with search_knowledge for
        generic "My Ad was Removed" content;
    (f) Bot escalates without referencing the moderation
        reason that was available.

form_context:
  first_name: Avery
  email: avery.removed.moderation@example.com
  topic_subject: Ad Support
  ad_id: AD-3060
  description: |
    My ad was removed. Can you tell me why?

persona:
  user_goal_summary: |
    User had an ad (AD-3060) that was removed by platform
    moderation. The moderation_review (auto-loaded into
    customer_context by pre-chat trigger) contains the actual
    reason. User wants to understand the specific reason —
    not generic policy info. User will not appeal in this
    session (could be UC-H follow-on, out of scope).
  frustration_level: mild
  verbosity: normal
  drift_behavior: none
  seed_messages:
    - "Hi, my ad was removed. Can you tell me why?"
    - "OK that makes sense. Is there anything I can do to
       repost it correctly?"
    - "Thanks for explaining."
  hidden_facts:
    - fact: "Listing AD-3060 moderation_review.reason_code =
        IMAGE_QUALITY (illustrative; actual moderation reason
        per dev test fixture). User did not know this was the
        reason."
      disclose_when: never — this is the data the bot's
        moderation_review surface should expose
  will_request_human_if: |
    the bot cites a generic policy article that doesn't match
    the actual moderation reason; or the bot says it cannot
    tell the user why without checking elsewhere when
    moderation_review is already loaded

expected:
  outcome_class: resolve
  primary_uc: UC-FP
  secondary_ucs:
    - UC-A
  should_escalate: false
  allow_bot_resolution: 'true'
  acceptable_outcomes: [resolve]
  bot_handling_pattern: |
    Turn 1: bot reads customer_context.moderation_review (already
    loaded). Bot answers with the specific reason from the
    moderation review (e.g., "Your ad was removed for image
    quality — the platform requires …"). Bot may classify UC-FP
    (preferred when moderation_reason is the canonical surface)
    or UC-A (acceptable if moderation_review is still used).

    Turn 2: bot answers the follow-up (how to repost correctly)
    with grounded guidance.

    Turn 3: bot confirms resolution (record_outcome).
  escalation_trigger: null
  risk_level: medium
  expected_tool_sequence:
    - classify_use_case
    - search_knowledge
    - resolve_article
    - record_outcome
  forbidden_tools:
    - create_case_controlled
  grounding_mode: moderation_review_and_faq_backed
  answer_must_not_contain:
    - I've restored your ad
    - I'll undo the removal
    - I have unblocked your listing
  max_turns: 5

scoring:
  hard_checks:
    - phase_transition_validity
    - no_critical_policy_violation
    - no_pii_leakage
    - no_stall
    - no_human_only_tool_exposure
    - no_forbidden_tools
    - budget_enforcement
    - grounding_compliance
  outcome_checks:
    - correct_uc
    - correct_outcome
    - tool_sequence_match
  llm_judge_dimensions: []
```

---

## 6. NEW — `cs_uc_a_lookup_failed.yaml` (Tier 1 target — degradation path)

```yaml
case_id: cs_uc_a_lookup_failed
source_session_id: synthetic-cs4-cs_uc_a_lookup_failed-2026-06-08
source_dataset: bad_case_authored

bad_case_metadata:
  surfaced_by: research-agent (CS4 proposal Part B; criterion #4 — missing_ad_id / lookup_failed must elicit or degrade gracefully, not generic resolution)
  surfaced_date: 2026-06-08
  failure_shape: |
    Form has ad_id (AD-9999, intentionally invalid for test
    fixture). Pre-chat auto-trigger calls get_customer_context
    which returns a lookup_failed state (listing not found).
    Bot today (without OBS-S1 procedure) may ignore the
    failure and proceed with search_knowledge as if the
    listing were loaded, producing a generic FAQ answer
    detached from the user's actual situation.
  layers_involved: |
    prompt_projection (customer_context_status: lookup_failed
    is projected as observable but not bound to procedure);
    semantic_planner (LLM does not gracefully degrade when
    lookup state is failed)
  related_dimensions: [D1, D6]
  related_r_items:
    - OBS-S1 (entity-context verification including degradation
      path)
  tier: tier_1_target

closure_criterion: |
  Bot recognises customer_context_status: lookup_failed and
  gracefully degrades.

  PASS conditions (one of):
    (a) Bot tells the user the listing lookup failed and asks
        them to confirm the ad_id (correct user-friendly
        framing);
    (b) Bot tells the user the lookup failed and offers
        generic guidance with explicit acknowledgement that
        the answer may not match their specific situation;
    (c) Bot escalates with reason=user_requested or
        reason=lookup_failed (degradation path; preferred
        over silent generic answer).

  FAIL conditions:
    (d) Bot answers with a generic FAQ article as if the
        listing were loaded — the user has no way to know
        the lookup failed and may act on wrong info;
    (e) Bot pretends to know listing details ("your ad in
        category X is …") that were never loaded — grounding
        floor violation;
    (f) Bot escalates with a misleading reason (e.g.
        intake_complete_for_uc_h on a UC-A case).

form_context:
  first_name: Casey
  email: casey.lookup.fails@example.com
  topic_subject: Ad Support
  ad_id: AD-9999
  description: |
    I can't see my ad anywhere on the site. Did something
    happen to it?

persona:
  user_goal_summary: |
    User typed an invalid ad_id (AD-9999) into the pre-chat
    form (maybe by typo, maybe because they confused two ads).
    The user CAN provide the real ad_id if asked. User is
    cooperative, mildly anxious about the ad.
  frustration_level: mild
  verbosity: normal
  drift_behavior: none
  seed_messages:
    - "Hi, I can't see my ad anywhere on the site. Did
       something happen to it?"
    - "Oh, sorry, the actual ID is AD-3070, not 9999.
       My mistake."
    - "OK thanks, that helps."
  hidden_facts:
    - fact: "User's real ad_id is AD-3070 (active listing).
        AD-9999 does not exist in any test fixture."
      disclose_when: if the bot tells the user the lookup
        failed and asks to confirm the ad_id
  will_request_human_if: |
    the bot fabricates listing details that were not loaded;
    or the bot proceeds with generic FAQ without acknowledging
    the lookup failure

expected:
  outcome_class: either
  primary_uc: UC-A
  secondary_ucs: []
  should_escalate: false
  allow_bot_resolution: 'partial'
  acceptable_outcomes: [resolve, escalate]
  bot_handling_pattern: |
    Turn 1: bot reads customer_context_status: lookup_failed.
    Bot tells the user the listing AD-9999 could not be
    found and asks to confirm the ad_id.

    Turn 2: user provides AD-3070. Bot calls get_customer_context
    with the correct ad_id, retrieves the listing, and
    answers with grounded reference.

    Turn 3: bot confirms resolution. OR (alternate path) bot
    escalates if the user cannot provide a corrected ad_id
    after one clarifying turn.
  escalation_trigger: lookup_failed_user_cannot_correct
  risk_level: low
  expected_tool_sequence:
    - classify_use_case
    - get_customer_context
    - search_knowledge
    - resolve_article
    - record_outcome
  forbidden_tools:
    - create_case_controlled
  grounding_mode: listing_data_or_faq_backed
  answer_must_not_contain:
    - Your ad AD-9999 is
    - Your listing in category
    - I see your ad
    - According to your listing
  max_turns: 6

scoring:
  hard_checks:
    - phase_transition_validity
    - no_critical_policy_violation
    - no_pii_leakage
    - no_stall
    - no_human_only_tool_exposure
    - no_forbidden_tools
    - budget_enforcement
    - grounding_compliance
  outcome_checks:
    - correct_uc
    - correct_outcome
    - tool_sequence_match
  llm_judge_dimensions: []
```

---

## 7. EXTEND — `alice_uc_a_uc_h_misclass.yaml` (diff)

The existing file at
`eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`
already encodes a strong `bot_handling_pattern` + `closure_criterion`
but its `outcome_checks: []` is empty (verified at HEAD 2026-06-08).
Strengthen with Path γ outcome_checks; keep existing rubric.

**Diff to apply** (the existing yaml has these fields verbatim; only
the empty `outcome_checks` block changes):

```diff
 scoring:
   hard_checks:
     - phase_transition_validity
     - no_critical_policy_violation
     - no_pii_leakage
     - no_stall
     - no_human_only_tool_exposure
     - no_forbidden_tools
     - budget_enforcement
     - grounding_compliance
-  outcome_checks: []
+  outcome_checks:
+    - correct_uc
+    - correct_outcome
+    - tool_sequence_match
   llm_judge_dimensions: []
```

**Rationale**: the file's existing `expected_tool_sequence`
(`classify_use_case → get_customer_context → search_knowledge →
resolve_article → record_outcome`) already pins the entity-context
path. The `bot_handling_pattern` already describes the rubric.
Adding the three outcome_checks makes the case actually gate on
classification + outcome + tool-sequence rather than only rubric-
read by the L3 judge.

**No change** to `closure_criterion`, `bot_handling_pattern`,
`forbidden_tools`, `answer_must_not_contain`, or any other field —
this is purely tightening enforcement of behaviour the existing
yaml already specifies as expected.

---

## 8. EXTEND — `wmkb_uc_a_trader_flag_secondary_uc_h.yaml` (diff)

The existing file at
`eval_interactive/case_specs/bad_cases/wmkb_uc_a_trader_flag_secondary_uc_h.yaml`
has `outcome_checks: []` and a relatively-minimal `hard_checks`
set (3 entries vs alice's 8). Strengthen both.

**Diff to apply**:

```diff
 expected:
-  outcome_class: either
+  outcome_class: resolve
   primary_uc: UC-A
   secondary_ucs: [UC-H, UC-D]
   should_escalate: false
   allow_bot_resolution: 'partial'
   acceptable_outcomes: [resolve, escalate]
+  bot_handling_pattern: |
+    Bot recognises "the trader flag on my account is wrong" as
+    a UC-A account/profile question. Bot may ask one focused
+    clarifying question to distinguish "tell me how to change
+    the setting" (UC-A) from "I want to appeal the decision"
+    (UC-H). Bot does NOT lock into UC-H intake without an
+    explicit appeal request. Once UC-A is committed, bot reads
+    customer_context (account_type field) and grounds its answer
+    in the user's specific account state.
+  expected_tool_sequence:
+    - classify_use_case
+    - get_customer_context
+    - search_knowledge
+    - resolve_article
+    - record_outcome
+  forbidden_tools:
+    - create_case_controlled
+  max_turns: 6

 scoring:
   hard_checks:
-    - no_pii_leakage
-    - no_human_only_tool_exposure
-    - no_critical_policy_violation
+    - phase_transition_validity
+    - no_critical_policy_violation
+    - no_pii_leakage
+    - no_stall
+    - no_human_only_tool_exposure
+    - no_forbidden_tools
+    - budget_enforcement
+    - grounding_compliance
-  outcome_checks: []
+  outcome_checks:
+    - correct_uc
+    - correct_outcome
+    - tool_sequence_match
   llm_judge_dimensions: []
```

**Rationale**:
- `outcome_class: either` was permissive; tightening to `resolve`
  (with `acceptable_outcomes: [resolve, escalate]` retained for
  graceful-escalation path) gives a stronger pin.
- Adding `bot_handling_pattern` brings parity with alice and
  makes the rubric L3-judge-consumable.
- Adding `expected_tool_sequence` aligned with the entity-context
  procedure path.
- Adding the 5 missing `hard_checks` brings parity with alice.
- Adding `correct_uc + correct_outcome + tool_sequence_match`
  outcome_checks tightens gating.

---

## 9. Research-agent activation prompt template (for similar follow-ups)

When you (human) need to re-spawn a research-agent for similar
work — drafting CaseSpecs, refining a Part B seed set, or
designing acceptance gates for a new bad case — use the template
below. Paste it as the first message in a fresh chat session
with a research-agent role.

### 9.1 Activation header (one-shot, always)

```
@docs/teams/research-agent.md

[ROLE-CONTEXT]
You are a research-agent operating under the Path 2 (bad-case-
driven) mode defined in your role guide. You produce code-grounded
proposals + CaseSpec drafts. You do NOT modify code. You write to
docs/solutions/<descriptive-name>.md.

[COLD-START READ ORDER]
1. @docs/teams/research-agent.md (your role)
2. @AGENTS.md (governance chain: doc_governance + agent_context_guide
   + iteration_governance auto-loaded)
3. docs/10-handoff.md §0 only (current state)
4. docs/milestone_objective.md + docs/sprint_objective.md (if active
   contract)
5. docs/action_bank.md §5 (open R-items only; skip §1-§4 history)
6. The task-specific context I provide below.

[GOVERNANCE QUICK-REFS YOU MAY NEED]
- §1 Constitution: LLM-first; rules define boundaries
- §3.2 Fix Layer Classification (9 layers)
- §5.6 Bad-case lifecycle (docs/current/process/badcase-lifecycle.md)
- §7 stanza template
- §1.7 forbidden list (no keyword/regex/if-else fix)

[OUTPUT FORMAT]
Path-2 (Mode 2 bad-case-driven) requires all 4:
  1. Multi-layer root-cause analysis (code-grounded; cite file:line)
  2. Coverage check vs action_bank R-items + active milestone scope
  3. Compounding-effect analysis
  4. Deliver-agent-consumable proposal (§7 stanza + hard fences)

Save proposal to docs/solutions/<YYYY-MM-DD>-<descriptive-name>.md
with standard front-matter (doc_tier: proposal, status: proposal,
authored_by: research-agent, mode: bad-case-driven).
```

### 9.2 Task block (per-session, varies)

Replace each `[ ]` placeholder with the specific task content.

```
[TASK]
Draft N CaseSpec YAMLs for the <objective-name> bad-case-driven
work. Background context (already proposed; do not re-derive):

  - Parent proposal: docs/solutions/<parent-proposal-name>.md
  - Acceptance criteria (from human):
    1. [criterion 1]
    2. [criterion 2]
    3. [criterion 3]
    4. [criterion 4]
  - Suite target: eval_interactive/case_specs/<suite>/
  - Schema constraint (verified or to-verify): [path γ /
    framework-extension / etc.]

[INPUTS]
  - Existing reference CaseSpec(s):
    eval_interactive/case_specs/bad_cases/<reference-yaml-1>
    eval_interactive/case_specs/bad_cases/<reference-yaml-2>
  - CaseSpec schema: eval_interactive/eval_interactive/specs/
    schema.py + eval_interactive/eval_interactive/scoring/
    outcome_checks.py
  - Schema-capability investigation: [link to prior survey
    if exists, or instruct to run one]

[DELIVERABLE]
  - Appendix file at docs/solutions/<YYYY-MM-DD>-<name>-appendix.md
    containing:
    * Tier-1 target CaseSpecs (full YAML, copy-pasteable)
    * Tier-1 negative-control / anti-误杀 CaseSpec (anti-误杀
      binding gate for any auto-correction loop)
    * Tier-2 neighbor CaseSpecs
    * Diffs to any existing CaseSpec that needs strengthening
    * Tier assignment per §5.6
    * Reference back to the parent proposal section

[CONSTRAINTS]
  - All claims about behavior or schema MUST cite file:line and
    be verified at HEAD on the named branch.
  - Use Path γ (within current schema) unless a framework-
    extension is explicitly authorized.
  - Do NOT modify code. Do NOT write to case_specs/ directly;
    the appendix is read-only material that dev-agent will copy.
  - Anti-误杀: every "target" CaseSpec must have a paired
    negative-control to prevent over-correction.
  - All proposed expected_tool_sequence values must reference
    real tools in the current tool-policy.yaml.

[OPEN QUESTIONS TO REPORT, NOT TO ANSWER]
  - Any schema gap you find that Path γ cannot fully express —
    surface as a recommendation, not a fix.
  - Any conflict with existing CaseSpec acceptance — surface,
    do not silently override.
```

### 9.3 What the research-agent will hand back

- Proposal-style appendix file at
  `docs/solutions/<YYYY-MM-DD>-<name>-appendix.md` with the
  CaseSpec drafts as code blocks.
- An updated cross-reference in the parent proposal (if asked).
- A list of open questions / schema gaps surfaced during
  drafting.
- Verification that each drafted CaseSpec compiles against the
  schema (the research-agent should grep
  `eval_interactive/eval_interactive/specs/schema.py` for the
  current field set; if a field is added that doesn't exist in
  schema, flag it).

### 9.4 What you (human) do with the output

1. Review the drafts. The closure_criterion + bot_handling_pattern
   fields are the human-readable contracts — verify they capture
   your intent.
2. Decide whether to ship as-is or refine.
3. Pass the appendix + parent proposal to deliver-agent (or to
   dev-agent directly if scope is small) for §5.6 bad-case tiering
   review + actual file creation under
   `eval_interactive/case_specs/bad_cases/`.
4. Once landed, the autoloop ingests the new CaseSpecs on its next
   re-bless / overnight run.

---

## 10. Cross-references back to parent proposal

| This appendix § | Parent proposal § |
|------------------|--------------------|
| §1 (how to use) | §3.4 Part B (CaseSpec authoring) |
| §2-§6 (5 NEW drafts) | §3.4 Part B table seed set |
| §7-§8 (2 EXTEND drafts) | §3.4 Part B + §3.3 Layer B (UC-A bad_cases enforcement weak today) |
| §9 (activation template) | §11 (handoff to deliver-agent) |

This appendix supersedes nothing; it operationalizes §3.4 Part B
of the parent proposal at the CaseSpec-yaml level.
