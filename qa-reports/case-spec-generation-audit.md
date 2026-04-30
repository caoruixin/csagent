# CaseSpec generation audit

_Sessions audited: **367**_

## 570Q5000008kr6LIAQ (case_id=cs_interactive_001)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `badcase_turns.csv`
- turn count: `22`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 3, 'human_investigation': 6, 'handover_or_case': 0, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - A bit of both
  - I am confused i see you guys both of my email but when i log in i use [EMAIL]. it's really confused me
  - Thank you, so the details login i am having issues with is the [EMAIL]
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008vyxJIAQ (case_id=cs_interactive_002)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `badcase_turns.csv`
- turn count: `42`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 5, 'handover_or_case': 5, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - How long do I have to wait to sort this out? Its been this way since day 1.
  - So in order to receive notifications I HAVE to receive messages from you that I don't want and am not interested in??
  - I don't want to receive spam from you.
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008iyS5IAI (case_id=cs_interactive_003)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `badcase_turns.csv`
- turn count: `11`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 6, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - Courtney still not had any inquiries for my advert feel as if a totally wasted my money on this advert
  - Ok thanks
  - It under removal services
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q50000090OefIAE (case_id=cs_interactive_004)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `badcase_turns.csv`
- turn count: `26`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 15, 'user_requested_human': False}`
- representative user messages:
  - Yes. Icannot see my ad after logging in. It says I have no live ads
  - I cannot see my add after logging recently posted
  - yes [EMAIL]
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008RAwLIAW (case_id=cs_interactive_005)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `badcase_turns.csv`
- turn count: `24`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 6, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - Hi Nursan
  - Will they be put back on automatically or do I have to do it all manually again? I have paid for the ads to be featured too. I've also had a look at listing terms and there was nothing in the description or picture that would breach this.
  - You too bye
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008q1g9IAA (case_id=cs_interactive_006)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `50`
- transcript outcome: `escalate`
- transcript reason: visitor explicitly requested a human or manager
- evidence signal counts: `{'unresolved_user': 8, 'human_investigation': 4, 'handover_or_case': 3, 'identifier_context': 7, 'user_requested_human': True}`
- representative user messages:
  - Why are there no details or information on your website about how to raise a return and request a refund?
  - You have accepted that a customer has not received the item they paid for and instead of simply refunding your end, you have tried to disguise poor systems as a process that I must follow. No I don't need to wait. The proof of delivery is visible. You have even checked it. So you can't expect a customer to wait on your business.
  - If you are not in the UK then I suggest you call someone who is because right now you are representing Gumtree and the UK law and legislation applies.
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008O5htIAC (case_id=cs_interactive_007)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `badcase_turns.csv`
- turn count: `23`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - The ad id is [EMAIL]
  - I have 2 pending payment at my bank accoumt, i just promote once and i dont know why there is two payment
  - Thx
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kMiTIAU (case_id=cs_interactive_008)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `28`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update, investigation_needed
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 0, 'identifier_context': 13, 'user_requested_human': False}`
- representative user messages:
  - Hi how can I have an aggrement with Gumtree like business aggrement for posting as much as I want
  - Take care
  - Ok thanks
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008g8KjIAI (case_id=cs_interactive_009)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `badcase_turns.csv`
- turn count: `20`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - Hi can you please explain why when I go on to peoples ads for sale the last picture they post is not available or blank or a completely unrelated advert
  - No it's clear that you are either unable or unwilling to help,thanks for confirming that this service isn't working as it is supposed to
  - Certainly,do you have a complaint procedure for these issues as I don't recall saying I was 100 % sure of anything nevermind your advice would not work , merely stated that the only issue I had was with ads on your company website and didn't believe that my browser that works seemlessly on every other occasion I use it might not be the problem,as you stated from the beginning passing it back on to me
- outcome decision: partial_uc_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=intake_complete_for_uc_k
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008pLozIAE (case_id=cs_interactive_010)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `badcase_turns.csv`
- turn count: `28`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 5, 'handover_or_case': 1, 'identifier_context': 29, 'user_requested_human': False}`
- representative user messages:
  - Hi Monisha
  - I can't receieve that email, I no longer have access to any of the email address on my account
  - no, that is all thanks
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008NWIjIAO (case_id=cs_interactive_011)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `badcase_turns.csv`
- turn count: `28`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - Hi Jason
  - Is that defo my login email Jason ?
  - How do I do that ?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_requested
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008hx9tIAA (case_id=cs_interactive_012)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-FP`
- turns file: `badcase_turns.csv`
- turn count: `18`
- transcript outcome: `escalate`
- transcript reason: visitor explicitly requested a human or manager
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 4, 'user_requested_human': True}`
- representative user messages:
  - Hi Jason
  - Thank you and happy new year
  - Ok pls look in to this as soon as possible pls
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=user_requested_human,human_investigation_signals,handover_or_case_signals; reason=visitor explicitly requested a human or manager; trigger=user_requested
- classification override applied: **YES** -> primary=UC-FP, secondary=['UC-K']
- case-level override applied: **YES**
  - override source: approved Wave A5 semantic review (merged with Wave A2.1 UC-B reclassification map)
  - reviewer: human semantic review
  - date: 2026-04-30
  - confidence: `high`
  - supporting turns: `[5, 6, 8, 10, 12, 14]`
  - rationale: Late phone/human request is a failure-path signal. The bot should first retrieve safe account, listing, and moderation context, then explain the kitten ad deletion reason with policy-grounded next steps.
  - status: `approved`
  - case_id_hint: `cs_interactive_012`
  - changed expected fields:
    - `answer_must_not_contain`: `["I've fixed", 'I have sent you an email', 'I can restore/delete/ban this directly', "I'll resolve this for you"]` -> `[]`
    - `bot_handling_pattern`: `Acknowledge the issue, run policy-mandated tools (get_customer_context, search_knowledge, resolve_article, request_handover, record_outcome), collect required intake fields (none), and hand over with reason user_requested.` -> `Acknowledge the issue, run policy-mandated tools (get_customer_context, search_knowledge, resolve_article, record_outcome), and answer from the knowledge base. Do not escalate.`
    - `escalation_trigger`: `user_requested` -> `None`
    - `expected_tool_sequence`: `['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']` -> `['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']`
    - `outcome_class`: `escalate` -> `resolve`
    - `should_escalate`: `True` -> `False`
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_message_moderation_context']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008dPjVIAU (case_id=cs_interactive_013)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `badcase_turns.csv`
- turn count: `24`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 4, 'handover_or_case': 2, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - I am facing an issue with my Gumtree account. As soon as I post an ad, it gets removed immediately . I make sure my listings follow Gumtree’s policies and I am not posting anything prohibited. please let me know the reason why my ads are being deleted
  - Please review it and sort it please
  - But i am not a Trader , only selling my own household stuff
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008u9gjIAA (case_id=cs_interactive_014)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `badcase_turns.csv`
- turn count: `26`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 3, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - Ok thanks
  - I'd be happy enough if the contact email address was reverted to the original account address
  - Not that anyone will care of course 🤪😃
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008WmXxIAK (case_id=cs_interactive_015)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-FP`
- turns file: `badcase_turns.csv`
- turn count: `17`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 4, 'handover_or_case': 1, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - ok so how do I change it?
  - so I can't just change it?
  - you mean my ankles in the image???
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- classification override applied: **YES** -> primary=UC-FP, secondary=['UC-B', 'UC-K']
- case-level override applied: **YES**
  - override source: approved Wave A6 semantic re-review (supersedes Wave A2.1 UC-K legacy pin)
  - reviewer: human semantic review
  - date: 2026-04-30
  - confidence: `high`
  - supporting turns: `[5, 6, 7, 8, 9, 11, 13]`
  - rationale: The human agent gave clear policy reasons in turns 5-7 ("men only" wording, body parts in image, ad title flagged). The user's "ok so how do I change it?" / "so I can't just change it?" is the canonical UC-FP edit-and-repost request. Late escalation in turn 11 is failure-path evidence -- the bot's golden target is to retrieve safe customer/listing/moderation context, ground the answer in policy, and escalate only on explicit appeal/review/restoration/human-support requests, strong distress, or when a safe reason cannot be produced.
  - status: `approved`
  - case_id_hint: `cs_interactive_015`
  - changed expected fields:
    - `answer_must_not_contain`: `["I've fixed", 'I have sent you an email', 'I can restore/delete/ban this directly', "I'll resolve this for you"]` -> `[]`
    - `bot_handling_pattern`: `Acknowledge the issue, run policy-mandated tools (get_customer_context, search_knowledge, resolve_article, request_handover, record_outcome), collect required intake fields (none), and hand over with reason user_distress.` -> `Retrieve safe customer/listing/moderation context, explain why the ad was removed or flagged, clarify whether the user can edit/repost/change it, and escalate only if the reason cannot be produced, the user requests review/restoration/human support, or a human-only action is required.`
    - `escalation_trigger`: `user_distress` -> `None`
    - `expected_tool_sequence`: `['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']` -> `['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']`
    - `outcome_class`: `escalate` -> `resolve`
    - `should_escalate`: `True` -> `False`
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_message_moderation_context']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008bSlVIAU (case_id=cs_interactive_016)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `29`
- transcript outcome: `escalate`
- transcript reason: repeated unresolved/account-confusion signal(s) in visitor turns
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - Hello,Lerato.I would like to find an old transaction, from a bike I bought few years ago.Is that possible?Thanks
  - I m not sure which one I used in the past.They are both mine
  - No worries. Have a lovely new year 😊
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008pEh3IAE (case_id=cs_interactive_017)

- original primary_uc (HR): `OUT_OF_SCOPE_RATINGS_REVIEWS`
- final primary_uc: `UC-A`
- turns file: `badcase_turns.csv`
- turn count: `20`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 4, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Hi Courtney. Thanks you 😊
  - He was very angry it was not for sale anymore. I had literally just sold it
  - 🙏
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008t1riIAA (case_id=cs_interactive_018)

- original primary_uc (HR): `OUT_OF_SCOPE_RATINGS_REVIEWS`
- final primary_uc: `UC-A`
- turns file: `badcase_turns.csv`
- turn count: `25`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 5, 'handover_or_case': 0, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - Hello I’m writing to request the removal of a false review against my username. I never receive negative reviews and on ebay I have 100% positive reviews from over 500 transactions. I always make sure I’m polite and courteous. I can give you evidence as to why this particular reviewer is using false information.
  - No, looking forward to your email on that, and your response about the false review. Thanks
  - Yes please
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008qgjJIAQ (case_id=cs_interactive_019)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `28`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - Hi, i reported yesterday that the laptop i am selling. The person has received the laptop and is not giving me my money, instead its part of a scam.
  - Ye got it. Just waiting for the email
  - I am just hoping to either get paid or get the laptop back. I will repond to the email as soon as I receive it
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q50000090RcaIAE (case_id=cs_interactive_020)

- original primary_uc (HR): `OUT_OF_SCOPE_RATINGS_REVIEWS`
- final primary_uc: `UC-A`
- turns file: `badcase_turns.csv`
- turn count: `17`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - I don't know and can't them but my star rating is wrong
  - That's a bit unfair but not alot I can do about it
  - It's from years ago and doesn't reflect my current affairs
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kCnxIAE (case_id=cs_interactive_021)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `badcase_turns.csv`
- turn count: `22`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - cant replace my old one?
  - which email?
  - how will they be in touch?
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008zYjJIAU (case_id=cs_interactive_022)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `badcase_turns.csv`
- turn count: `22`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation, manager
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 2, 'handover_or_case': 6, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - Hi Monisha
  - I paid my invoice for my gumtree ads package. I put reference "Hassan Manzoor"
  - you too take care
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation, manager; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008hVf3IAE (case_id=cs_interactive_023)

- original primary_uc (HR): `UC-FP`
- final primary_uc: `UC-FP`
- turns file: `badcase_turns.csv`
- turn count: `21`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - my puppy ad has run out and i would like to repost it but am i only aloud 3 ads is this classed as my third or can i post another litter thanks
  - thanks talk soon
  - are you there
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008rjIXIAY (case_id=cs_interactive_024)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `badcase_turns.csv`
- turn count: `15`
- transcript outcome: `escalate`
- transcript reason: visitor explicitly requested a human or manager
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': True}`
- representative user messages:
  - Unfortunately I do not have an ad id , U think I have chatted with you early on today then I chatted with Jason and I have been trying to sort this all day . There is a problem with my email not being recognised but that is what I use all the time . There only other one I had was one was used to set up my phone but I do not use it . I advertise through the Gumtree app and not the account . Helen
  - I sent copies of chat with person who bought camera and money did not transfer and I sent copy of what he showed me on his phone. Helen
  - I only have first name Martin and date of conversation was Tuesday the 20th of January . Thank you Helen
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
  - escalation_trigger: HR='payment_dispute_detected' vs final='user_requested'
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008tUNRIA2 (case_id=cs_interactive_025)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `badcase_turns.csv`
- turn count: `33`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 2, 'handover_or_case': 2, 'identifier_context': 25, 'user_requested_human': False}`
- representative user messages:
  - I was just checking 'all ads' both active and inaccurate
  - Yes please delete everything else - I'm easily confused 👍
  - No thank you again
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008jM6DIAU (case_id=cs_interactive_026)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `badcase_turns.csv`
- turn count: `25`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 17, 'user_requested_human': False}`
- representative user messages:
  - Says there have been 2 replies to my ad but I’ve never received any
  - Ok. That’s not clear. Thanks bye.
  - If I look in my account I will loose the chat with you
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008oS9OIAU (case_id=cs_interactive_027)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `badcase_turns.csv`
- turn count: `22`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed, async_update, time_window
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 6, 'handover_or_case': 0, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - I few weeks back I was selling a fishing bivvy and a guy called Brian would not stop messaging me to buy it. I wouldn’t sell at the price he wanted. He became abusive and I also did to him. He has left a review of me negative as if I’ve sold him something and I haven’t I want his review removed please
  - Like I say he and I was both abusive to each other but I definitely did not sell him anything and his review makes out I haven’t and I’m a bad seller
  - Are you still there?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: investigation_needed, async_update, time_window; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008VtJlIAK (case_id=cs_interactive_028)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `badcase_turns.csv`
- turn count: `17`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 1, 'identifier_context': 2, 'user_requested_human': False}`
- representative user messages:
  - Ref 96385089
  - we had a outstaning invoice, and settled on Friday the 19.12.2025, still the account is not activated.
  - LIKEWISE
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_requested
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kDiPIAU (case_id=cs_interactive_029)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `badcase_turns.csv`
- turn count: `48`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): case_reference, escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 2, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - HI MY ACCOUNT OS
  - YOUR NO HELPING AT ALL
  - I HAVE ALREADY FOLLOWED YOUR SO CALLED PROCESS
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): case_reference, escalation; trigger=user_requested
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008uDdtIAE (case_id=cs_interactive_030)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `badcase_turns.csv`
- turn count: `18`
- transcript outcome: `escalate`
- transcript reason: visitor explicitly requested a human or manager
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 4, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': True}`
- representative user messages:
  - Hey Cortney
  - its a different category acount allowed via manager
  - should i speak to my acount manager for this
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=user_requested_human,human_investigation_signals; reason=visitor explicitly requested a human or manager; trigger=user_requested
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008oPmcIAE (case_id=cs_interactive_031)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `27`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - I posted 2 ads why have they been remy
  - Ok will wait for the email
  - Ok will your email me
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008Wf3RIAS (case_id=cs_interactive_032)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `20`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 6, 'handover_or_case': 1, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - Hi Courtney
  - understand you have you procedures
  - I had people contacting me
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008xTasIAE (case_id=cs_interactive_033)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `20`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - hi i was just talking to someone and the chat got cut off i basically had an account with gumtree but the tablet i was logged in on got broke and i lost all my details but i gave him the ad ID to take down the advert so i can put the advert on my new account and he said you may have some problems so contact us when you make the new advert
  - same to you enjoy the rest of your day bye thanks again
  - It is only a pleasure and take care!
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008LEZeIAO (case_id=cs_interactive_034)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `19`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 4, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - Hi not sure why my adds are removed
  - Can’t see it. Can only see the picture with removed. The add doesn’t open
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kyB3IAI (case_id=cs_interactive_035)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `19`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: paid_service_impact
- evidence signal counts: `{'unresolved_user': 3, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - becuase 2 minutues after i places the add i got calls saying you have a man and van service on gumtree?
  - im not here to explain myself, the add has 8 views and i paid a premium thats not good enough 2 of those views have been spam callers
  - i would like a copy
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008hOYjIAM (case_id=cs_interactive_036)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `26`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 3, 'human_investigation': 5, 'handover_or_case': 1, 'identifier_context': 1, 'user_requested_human': False}`
- representative user messages:
  - Hi i have questions
  - I wrote to the courier company many times but no response. Only today they wrote that they would try to deliver as soon as possible but they don't know when. So, according to the law, I am entitled to a refund for the delay in delivery and the courier company's failure to meet the delivery date. When buying it said for 2-4 days delivery today passes this day still no shipment
  - So tomorrow I wait for e-mail
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fIqnIAE (case_id=cs_interactive_037)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `23`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): case_reference
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 4, 'handover_or_case': 2, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - Hello, I was told false information about the condition of a car I bought on gumtree, what can I do about this? I have already reported the seller
  - I'm not sure how to find the ad ID. The title is: Ford KA, hatchback, 2013, manual, 1242 (cc), 3 doors
  - Thank you, my email address is [EMAIL]
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008TPw5IAG (case_id=cs_interactive_038)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `23`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): case_reference
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 3, 'handover_or_case': 1, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - There a scamer on gumtree
  - I paid for a parkside impact wrench £40 and no item
  - She has 18 item on gumtree I've contacted my bank and they said yes it a fraudulent transaction they seen it before
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008t4zFIAQ (case_id=cs_interactive_039)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `35`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 5, 'handover_or_case': 0, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - Hi Monisha
  - I posted an ad to warn other Gumtree users, it's "under review", it would help me and others if the ad is allowed so people know what she looks like and the serial numbers of her counterfeit notes, so they do not fall foul of her crimes too. https://www.gumtree.com/p/freebies/beware-of-this-fraudster-michelle-who-pays-with-counterfeit-notes/1509035262
  - OK, thank you, I will be reporting her crimes to the police and action fraud.
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008pMbOIAU (case_id=cs_interactive_040)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `21`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation, case_reference
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 8, 'handover_or_case': 5, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - There seems to be issues with my ad
  - Ok thank you
  - Is there a case reference
- outcome decision: partial_uc_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation, case_reference; trigger=intake_complete_for_uc_k
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008aQLxIAM (case_id=cs_interactive_041)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `19`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - Already discussed recent issue about search but agent closed chat while i was typing an answer. Very rude!
  - But maybe it's possible to keep old login email but redirect all communication to new one?
  - No. Buy
- outcome decision: partial_uc_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fHI1IAM (case_id=cs_interactive_042)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `17`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 4, 'handover_or_case': 1, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - No I don't see that anymore
  - I always had my number and my email but now it's only email
  - it wasnt like this before
- outcome decision: partial_uc_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008WJ2vIAG (case_id=cs_interactive_043)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `20`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): handover
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 3, 'user_requested_human': False}`
- representative user messages:
  - Standing by!
  - I can do that but it seems strange that one moment I get a sensible answer (this morning) and the next time I look the response to the url results in ten times as many results with the filter clearly not working as expected…
  - Ok. Thanks. I’ll drop you a note shortly.
- outcome decision: partial_uc_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): handover; trigger=intake_complete_for_uc_k
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008TVtRIAW (case_id=cs_interactive_044)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `32`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 15, 'user_requested_human': False}`
- representative user messages:
  - .
  - I have never been a trader, never registered as a trader and am very confused as to how and why this has been altered
  - Thank you I will await the response
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008b8I9IAI (case_id=cs_interactive_045)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `20`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed, async_update, time_window
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 5, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - 1508024733
  - I have been trying to put this ad up since yesterday as I am moving soon. This really isn’t quick enough for me if I’m honest. Gumtree has said that an email with an explanation has been sent but I have received nothing
  - There were three things I tried to put up
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008iiyPIAQ (case_id=cs_interactive_046)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `29`
- transcript outcome: `escalate`
- transcript reason: visitor explicitly requested a human or manager
- evidence signal counts: `{'unresolved_user': 3, 'human_investigation': 5, 'handover_or_case': 1, 'identifier_context': 11, 'user_requested_human': True}`
- representative user messages:
  - Hi have you got a telephone number to call as I need to speak to someone asap
  - No I have not been paid by the buyer
  - Ok kindly investigate asap thanks
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ZIgbIAG (case_id=cs_interactive_047)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `22`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 2, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - Having problems posting again
  - Have a nice weekend
  - Much appreciated
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008qnBJIAY (case_id=cs_interactive_048)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `18`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - sure hang on
  - sure
  - 685£
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008uTXNIA2 (case_id=cs_interactive_049)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `21`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - There’s a loading sign that never ends
  - Thank you have a good day
  - So what can I do for the app
- outcome decision: partial_uc_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=intake_complete_for_uc_k
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kGGTIA2 (case_id=cs_interactive_050)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `handover_turns.csv`
- turn count: `21`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 1, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - my messages are being sent to an old email address. Not the address which is registered for my account. And i lost a slae because of that
  - that is the best advice you can give me?
  - are you still there?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008k56XIAQ (case_id=cs_interactive_051)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `handover_turns.csv`
- turn count: `40`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 22, 'user_requested_human': False}`
- representative user messages:
  - Yes sorry couldn’t find ad number
  - Shall I just change contact email back to tiscali that would be easier
  - Bye … you to!
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008DtYLIA0 (case_id=cs_interactive_052)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `handover_turns.csv`
- turn count: `52`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 1, 'identifier_context': 22, 'user_requested_human': False}`
- representative user messages:
  - I messaged you guys weeks ago about photos on my adverts keep uploading blurred, compressed and poor quality. I was told that someone will get in contact with via email.
  - Just waiting for your email
  - Can I be sent a copy of this chat as well
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008zPZZIA2 (case_id=cs_interactive_053)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `handover_turns.csv`
- turn count: `38`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 3, 'handover_or_case': 3, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Hi - this is quite urgent.
  - It's like beyond a joke, that all the various options actually don't work and we've paid for a year of service
  - Unlike the rest of Gumtree ;-)
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_requested
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008mLwpIAE (case_id=cs_interactive_054)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `handover_turns.csv`
- turn count: `24`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): manager, escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 4, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - ive been trying to get in touch with someone responsible who can listen to my concern
  - Dear Gumtree Advertising Team, I am writing this with genuine regret and a great deal of anxiety, and I would like to sincerely apologise for having to make this request. When I agreed to the six-month advertising contract, I truly believed it would bring a reasonable and timely flow of relevant leads. I entered into the agreement with hope and good faith, trusting that the initial grace period before the first payment would allow enough time for leads and conversions to start coming through. Unfortunately, that has not happened. I have not received a single relevant or quality lead, and the £420 payment that has now been taken from my account has impacted me far more deeply than I ever expected. At this moment, I am left with just £42 in my bank account, and I am struggling to manage even basic expenses. I want to stress that I am not writing this to complain or place blame. I understand that advertising does not guarantee results, and I fully accept responsibility for signing up. However, the reality is that I simply cannot afford to continue paying for advertising that is not generating any meaningful enquiries or conversions. The financial strain has been overwhelming and honestly quite distressing. I was truly relying on the expectation that the first six weeks would begin to produce leads that could help cover the cost of the advertising. Sadly, that has not been the case, and continuing the contract is something I genuinely cannot sustain. I am very sorry to ask for special consideration, but I am humbly and politely requesting that the contract be cancelled. This request comes from a place of necessity rather than dissatisfaction. If required, I am more than willing to share my bank statement as proof of my financial situation. I would be deeply grateful if you could review my circumstances with compassion and understanding. Any flexibility or support you could offer would mean a great deal to me, and I would truly appreciate your kindness in this matter. Thank you very much for taking the time to read this. I sincerely apologise for any inconvenience this causes and appreciate your consideration more than I can express. Kind regards, Saif
  - not Muse or Tom as i have had a very bad experienced with both of them after i signed a contract 2 months ago,
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008TMmvIAG (case_id=cs_interactive_055)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-FP`
- turns file: `escalation_turns.csv`
- turn count: `18`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 10, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - Hi ive just creates and posted and paid £1.9@ for better coverage for 7 days and its saying i have no ada
  - So when will my ad be live im not happy
  - Ive paid for the advertising of my ad too
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=user_distress
- classification override applied: **YES** -> primary=UC-FP, secondary=['UC-K']
- case-level override applied: **YES**
  - override source: Wave A2.1 reclassification map
  - reviewer: legacy migration from extractor.py UC_B_RECLASSIFICATION_OVERRIDES (Wave A2.1)
  - date: 2026-04-30
  - confidence: `high`
  - supporting turns: `[]`
  - rationale: Paid promotion + temporary hold; UC-FP with UC-K fallback.
  - status: `approved`
  - migrated_from_legacy: `true`
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_message_moderation_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008SPtJIAW (case_id=cs_interactive_056)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `25`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 3, 'handover_or_case': 1, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - someone is on there way to me know
  - and has emailed me for address and i cannot send an email or access any other old emails
  - Ad Id 1507082823
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008Ye01IAC (case_id=cs_interactive_057)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `escalation_turns.csv`
- turn count: `42`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 1, 'identifier_context': 20, 'user_requested_human': False}`
- representative user messages:
  - Hello my add replies are not getting through to me and today l had a phishing attempt
  - l can send a screenshot of all this
  - all three
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008OowjIAC (case_id=cs_interactive_058)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `26`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 3, 'handover_or_case': 0, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - I see I have a message but I can’t open or see what the actual message
  - It’s frustrating to see a message is there but you can’t access it
  - Thank u
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008NOoDIAW (case_id=cs_interactive_059)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `35`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 3, 'human_investigation': 4, 'handover_or_case': 0, 'identifier_context': 21, 'user_requested_human': False}`
- representative user messages:
  - Hi I received an email about questions to help recover my account. I can't find the I for needed to answer them and I am annoyed as it is not my problem that my account got hacked it is gumtree and I need to sell some furniture but can't
  - Your company is so bad
  - I used this company to sell my home furniture and never had problems and then I don't use it for a while and come to find it's been hacked
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008uCQ5IAM (case_id=cs_interactive_060)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `37`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 3, 'human_investigation': 9, 'handover_or_case': 0, 'identifier_context': 42, 'user_requested_human': False}`
- representative user messages:
  - I am trying to post a flat to rent under the correct criteria but it continually keeps being removed and says I will receive an email which I haven’t done. I would really like to post this and have used my account for many years so not sure what the issue is and hoped you could help me with this
  - No will you email me directly?
  - Ok thanks for your help. I am not posting yet until I hear from you
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008i6JdIAI (case_id=cs_interactive_061)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `28`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 3, 'handover_or_case': 2, 'identifier_context': 23, 'user_requested_human': False}`
- representative user messages:
  - Hello Thato, thank you
  - It’s for all the ads, each time I try to reply to an ad it won’t let me. I am looking for a flat and unable to answer ads of other people. I didn’t post an ad myself
  - Exactly ok thanks
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008rbEXIAY (case_id=cs_interactive_062)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `18`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 4, 'handover_or_case': 1, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - Hi i have no adds at the moment i was just about to post one, but the last time i did i never got email notifications
  - Ok thats great, thanks
  - Yes there is no message in my junk or inbox
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008TzmfIAC (case_id=cs_interactive_063)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `26`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 13, 'user_requested_human': False}`
- representative user messages:
  - You just cut me off
  - Do both
  - Or just send me an email with hi on it and then you know it’s working?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: async_update; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008l7flIAA (case_id=cs_interactive_064)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `18`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - Hi I posted 3 ads yesterday I've had 9 replies but have not had a message or email to let me know.
  - Just received it
  - I advertised a Volvo 10 ton bottle jack and had 6 replies on the stats but had nothing. [EMAIL]
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008c8eHIAQ (case_id=cs_interactive_065)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `38`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 13, 'user_requested_human': False}`
- representative user messages:
  - your welcome
  - or indeed when they were sent
  - No you haven’t you have said they could be pending …and have not given an answer as to why their messages were deleted
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fBsXIAU (case_id=cs_interactive_066)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `badcase_turns.csv`
- turn count: `11`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 1, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - Should be [EMAIL]
  - Ok thank you
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008bQa1IAE (case_id=cs_interactive_067)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `badcase_turns.csv`
- turn count: `19`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 6, 'handover_or_case': 2, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Ad ID: 1508059319
  - You will contact me how??
  - Okay escalate this ASAP and let me know how will I now????
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kNQ2IAM (case_id=cs_interactive_068)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `badcase_turns.csv`
- turn count: `15`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - Hello Courtney, I have just advertised a keep fit bench for sale at £59. But for some reason it got posted twice. Ive paid to promote the advert through PayPal Pal. Can you make sure I've not paid twice for promotion
  - Goodnight
  - Thank you Courtney for your help
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008odJJIAY (case_id=cs_interactive_069)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `badcase_turns.csv`
- turn count: `30`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed, time_window, internal_team, async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 8, 'handover_or_case': 0, 'identifier_context': 15, 'user_requested_human': False}`
- representative user messages:
  - Hello Courtney, I assume Cortney is a typing error?
  - It also states Email Link No Longer Valid
  - It also states- Email Link No Longer Valod
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: investigation_needed, time_window, internal_team, async_update; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008U6zRIAS (case_id=cs_interactive_070)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `badcase_turns.csv`
- turn count: `10`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 3, 'user_requested_human': False}`
- representative user messages:
  - I was chatting to somrone and lost connection. I am retired and although I have a Gateway no. I do not pay tax and cannot do the form.
  - Exxactly what I have told you.
  - Ate you there Courtnry???
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: investigation_needed; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008t32HIAQ (case_id=cs_interactive_071)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `20`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed, async_update, time_window
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 4, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Hi Courtney, can I report a gumtree user please
  - Can you let me know once you've sent the email and I'll get the screenshots to you
  - If the gumtree user wanted £30 to begin with then the advert should have been put up at £30
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q50000091B97IAE (case_id=cs_interactive_072)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `badcase_turns.csv`
- turn count: `15`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - A want to delete my account immediately
  - Ok thanks for your help
  - A haven’t used it for 2 years or more and they ask silly questions like when my last post was ! A just want it deleted no big desp
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fN8wIAE (case_id=cs_interactive_073)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `badcase_turns.csv`
- turn count: `27`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - Ok but I keep losing connection is there a number i can ring to talk directly?
  - Well my connection keeps getting cut off and I really need a refund as my ad was not allowed
  - No response?
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008hVjtIAE (case_id=cs_interactive_074)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `26`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): manager, escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 3, 'handover_or_case': 2, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - Yes business and then I was asked for my bank details but I am worried that I may be scammed as not sure if the details given are safe
  - That is kind yes please
  - Oh I am sorry I posted before and someone messaged me saying I was deceiving them because I hadn't posted under business
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008SWpxIAG (case_id=cs_interactive_075)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `badcase_turns.csv`
- turn count: `15`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - my account is not working with premium as the direct debit didn't work
  - please dont tell me you will ask account manager to reach out as hes clearly not available and I been 4 days out of business
  - and my account manager is not answering via email, phone or WhatsApp at all for 4 days now
- outcome decision: partial_uc_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008NOeXIAW (case_id=cs_interactive_076)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `badcase_turns.csv`
- turn count: `31`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): manager
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 1, 'identifier_context': 27, 'user_requested_human': False}`
- representative user messages:
  - hi i was chatting with courtney but my internet went down
  - thats why i cant access it even though i had the log in details
  - ok thank you for your help
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008dsiHIAQ (case_id=cs_interactive_077)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `badcase_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation, manager
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 4, 'handover_or_case': 2, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - ok thanks
  - ok thank you may i have your e mail address pls
  - dont know what that is dont think so
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation, manager; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008n3bFIAQ (case_id=cs_interactive_078)

- original primary_uc (HR): `OUT_OF_SCOPE_PRO_CONTRACT`
- final primary_uc: `UC-A`
- turns file: `badcase_turns.csv`
- turn count: `30`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation, manager
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 4, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - no details ?
  - no
  - this problem is from day one
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
  - escalation_trigger: HR='user_requested' vs final='out_of_scope'
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008v7ZdIAI (case_id=cs_interactive_079)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `badcase_turns.csv`
- turn count: `19`
- transcript outcome: `escalate`
- transcript reason: visitor explicitly requested a human or manager
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 2, 'identifier_context': 2, 'user_requested_human': True}`
- representative user messages:
  - Hi - I have in contact with you for months now regarding taking down advertisements that are services posting in the community section.
  - or someone who can help me?
  - Can I speak to your manager
- outcome decision: partial_uc_transcript_escalation: transcript_indicated_outcome=escalate; flags=user_requested_human,human_investigation_signals,handover_or_case_signals; reason=visitor explicitly requested a human or manager; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008qfaLIAQ (case_id=cs_interactive_080)

- original primary_uc (HR): `OUT_OF_SCOPE_DELIVERY`
- final primary_uc: `UC-A`
- turns file: `badcase_turns.csv`
- turn count: `33`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation, handover
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 2, 'identifier_context': 3, 'user_requested_human': False}`
- representative user messages:
  - No problem, can you please advise?
  - Thank you. I hope you have a lovely day. Take care 🙂
  - No thank you, just please make sure you let me know as this will make deliveries a lot safer - and persuade your boss ;-) thanks, have a good day
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
  - escalation_trigger: HR='user_requested' vs final='out_of_scope'
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008za3ZIAQ (case_id=cs_interactive_081)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `badcase_turns.csv`
- turn count: `56`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 4, 'handover_or_case': 0, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - Why did Nuraan end the chat after just one minute?
  - Ok and once I receive a response that is not satisfactory, how do I escalate that complaint?
  - How do I make a complaint?
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008cVqzIAE (case_id=cs_interactive_082)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `17`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 3, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - I would like to advise you that I bought an iPhone 17 from your website. The phone was not authentic and I have reported this to the police . I paid your seller £825 and I am not happy. Please can you advise me on how I get my money back
  - How can I get my money back
  - This one Gumtree email address
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ynMDIAY (case_id=cs_interactive_083)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `badcase_turns.csv`
- turn count: `13`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - I was just speaking to yourself and you closed the chat without giving me the chance to respond
  - we can’t even view the review
  - it is a disgrace you allow false reviews on your platform
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008l0xdIAA (case_id=cs_interactive_084)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `19`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Is my add live please as l have not had a conformation either thank you
  - SEE THIS I PAID TO HAVE IT ON THE FRONT PAGE JUST THIS AFTERNOON
  - PLEASE EMAIL ME AS SOON AS POSSIBLE AND REMOVE THIS OFFENDING MESSAGE
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008vDTlIAM (case_id=cs_interactive_085)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `12`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): case_reference
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 4, 'handover_or_case': 1, 'identifier_context': 3, 'user_requested_human': False}`
- representative user messages:
  - I am reporting a fraudulent property advertisement listed on Gumtree. The advertiser falsely represented herself under a fake name and advertised a rental property in Islington with the intention to defraud me. I was led to believe the advertiser was called Kate Miller, which was later confirmed to be false. The transaction connected to this listing resulted in financial loss and has been reported to the police as fraud. The matter is logged with Action Fraud under crime reference number RF25120027504C. I am requesting that Gumtree investigate the account, preserve all related data (including IP logs and account details), and confirm the removal of the fraudulent listing.
  - £300 , one purchase was through western union the other was through money gram on two different cards as she led me to believe it was genuine 11th December 2025 and the second purchase was on the 15th December
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fp8PIAQ (case_id=cs_interactive_086)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `badcase_turns.csv`
- turn count: `17`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 2, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - You have taken away the call option from adverts not good
  - Shane really goodbye
  - From today my partially blind client can’t use your platform no more as there is no phone option but thanks for taking our complaint
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fvqXIAQ (case_id=cs_interactive_087)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `12`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 4, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - advert number 1508126982. Have sent £200 to them by paypal to secure purchase but seller now not responding to phone or email or messeges. The money has been recived their end but they have now 'dissapeared'.
  - thats it. Thanks.
  - ok. Would like the report this as a FRAUD via the gumtree ad. I have name of the person and email address. Also a phone number (all of which they are ignoring from myself.
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008syVdIAI (case_id=cs_interactive_088)

- original primary_uc (HR): `OUT_OF_SCOPE_RATINGS_REVIEWS`
- final primary_uc: `UC-A`
- turns file: `badcase_turns.csv`
- turn count: `19`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - Hi, ok, thanks.
  - They say I cancelled an offer, that is a lie!
  - No, I prefer you remove this fraudulent and harassing review
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008vCKnIAM (case_id=cs_interactive_089)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `11`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: paid_service_impact
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Hello. I’m reporting an urgent fraud. I paid a £1,000 deposit by bank transfer for this exact boat under a signed written agreement for a £3,500 sale. The seller then stopped responding. I obtained a County Court Judgment which remains unpaid. The seller has now re-listed the same boat, creating an immediate risk it will be sold again. I am applying for an injunction. I need the listing removed, the seller account restricted, and all account and listing data preserved for court and police disclosure.
  - I will do that now
  - This is the listing
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008tbwpIAA (case_id=cs_interactive_090)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `22`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 6, 'handover_or_case': 0, 'identifier_context': 2, 'user_requested_human': False}`
- representative user messages:
  - I reported an add a few days ago as it’s recently been active again. This person claimed to be fully insured but he ruined my floors cost me a lot of money. He’s not insured so the add is fraudulent
  - That’s all
  - His add still saying he is fully insured
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ghQXIAY (case_id=cs_interactive_091)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `badcase_turns.csv`
- turn count: `17`
- transcript outcome: `escalate`
- transcript reason: visitor explicitly requested a human or manager
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 0, 'identifier_context': 11, 'user_requested_human': True}`
- representative user messages:
  - I can’t put a telephone number on an ad.
  - Ok I don’t think the non telephone number will work and you will lose customers everywhere. I know if I want to purchase something on gumtree, I would like to speak to the person to get more information.
  - Yes so what do I do?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=user_requested_human,human_investigation_signals; reason=visitor explicitly requested a human or manager; trigger=user_requested
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fE8rIAE (case_id=cs_interactive_092)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `drift_control_turns.csv`
- turn count: `30`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 13, 'user_requested_human': False}`
- representative user messages:
  - Hi can you please confirm you are human
  - Thanks 👍
  - How will you can tact me BTW? After escalation
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_requested
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008eU7dIAE (case_id=cs_interactive_093)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `drift_control_turns.csv`
- turn count: `18`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - It won't let me view the ad it just goes straight to removed
  - Thank you. What was the issue?
  - No worries
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kGOXIA2 (case_id=cs_interactive_094)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `drift_control_turns.csv`
- turn count: `24`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): handover
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 1, 'identifier_context': 17, 'user_requested_human': False}`
- representative user messages:
  - It's like a new account. I'm confused
  - Ok, i'll try. Thank you
  - New i mean?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): handover; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008U5C9IAK (case_id=cs_interactive_095)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-A`
- turns file: `drift_control_turns.csv`
- turn count: `18`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - Thank you. Would the wrong email address on the app perhaps not allow syncing but it was working before
  - No I will try that. Thank you
  - Thanks I'll try that. And then will it stay as that address for next time?
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- classification override applied: **YES** -> primary=UC-A, secondary=['UC-D', 'UC-K']
- case-level override applied: **YES**
  - override source: Wave A2.1 reclassification map
  - reviewer: legacy migration from extractor.py UC_B_RECLASSIFICATION_OVERRIDES (Wave A2.1)
  - date: 2026-04-30
  - confidence: `high`
  - supporting turns: `[]`
  - rationale: Wrong email, no adverts showing -- UC-A with UC-D / UC-K fallback.
  - status: `approved`
  - migrated_from_legacy: `true`
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008t5H0IAI (case_id=cs_interactive_096)

- original primary_uc (HR): `OUT_OF_SCOPE_RATINGS_REVIEWS`
- final primary_uc: `UC-A`
- turns file: `drift_control_turns.csv`
- turn count: `22`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 4, 'handover_or_case': 1, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - No, that is the issue. The option popped up earlier but that was before the sale was completed.
  - Thank you. Shall we end chat now?
  - Perfect, thank you Candice.
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008w5YzIAI (case_id=cs_interactive_097)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `drift_control_turns.csv`
- turn count: `22`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - I already brought this issue to the attention of one of your colleagues who promised to escalate the matter and get back to me via email. I have still not heard from gumtree while the seller still holds on to my £150.
  - I'm not sure that I received this email.
  - Thank you. I'll look out for it.
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008f6fyIAA (case_id=cs_interactive_098)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `drift_control_turns.csv`
- turn count: `28`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - Hi, i was given an email address to send some screenshots before and after I uploaded them but I haven't had no response and I would like to get it sorted ASAP please
  - I'm not sure what troubleshooting is sorry
  - OK thanks
- outcome decision: partial_uc_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=intake_complete_for_uc_k
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008n3szIAA (case_id=cs_interactive_099)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `drift_control_turns.csv`
- turn count: `47`
- transcript outcome: `escalate`
- transcript reason: visitor explicitly requested a human or manager
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 5, 'handover_or_case': 4, 'identifier_context': 12, 'user_requested_human': True}`
- representative user messages:
  - I had a chat a couple of weeks ago about a seller called Ben, I was expecting the transcript to be emailed to me, but never saw anything
  - Just frustrating. I thought I'd asked for the copy of the chat with your agent to be emailed to me.
  - I await your email from GDPR dept
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008hHozIAE (case_id=cs_interactive_100)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `drift_control_turns.csv`
- turn count: `24`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 2, 'identifier_context': 17, 'user_requested_human': False}`
- representative user messages:
  - It has happened recently. It concerns the account I have which is registered under the email address [EMAIL] . I am receiving marketing emails from you to that address but I am not receiving emails as I try to change my forgotten password.
  - Can I download this conversation?
  - I have used a device which doesn't have the app on it. The resets have still failed due to no reset email arriving.
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fmDlIAI (case_id=cs_interactive_101)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `11`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation, case_reference
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 2, 'handover_or_case': 3, 'identifier_context': 1, 'user_requested_human': False}`
- representative user messages:
  - Good morning, I sold an item on Gumtree and posted it through Evri. The parcel seems to be lost. My question is, am I as the seller liable to refund the purchase? I did tell the buyer that I will use Standard delivery only, before he bought it. Maybe you can tell me what the right thing is to do.
  - The id is 1507513803, thank you!
  - The package was not received. I sent it on 14/12 and still shows it is with Evri
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008I9TFIA0 (case_id=cs_interactive_102)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `15`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): handover
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 1, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - Hello, I hope you are well. I require support to gain details from a purchase I made and the conversation with the buyer for insurance purposes. The ad and conversation has disappeared (I am assuming because of the age of the ad and the conversation) If this isn't something you can access easily and is classed as archived, I would like to request a subject to access request that includes all of my data under data protection GDPR laws in the UK.
  - Okay thank you, I will need this information for two of my Gumtree accounts - the account with the email [EMAIL] and [EMAIL]
  - Thank you so much
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008VO4gIAG (case_id=cs_interactive_103)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `24`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 2, 'handover_or_case': 3, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - It didn't give me any ad reference. The add was removed straight after posting
  - I see, even if I provide a different email address?
  - Can you confirm you have escalated this issue?
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008maHVIAY (case_id=cs_interactive_104)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 3, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - Thank you , [EMAIL]
  - Well please let me know asap as ive paid money too list it and I expect it too be up advertised for the full period i paid for
  - So where do I stand
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008iANFIA2 (case_id=cs_interactive_105)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `21`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 4, 'handover_or_case': 2, 'identifier_context': 19, 'user_requested_human': False}`
- representative user messages:
  - Why gumtree deleted my all ads
  - I just tried to post add but agin my ad deleted
  - Please resolve this issue now
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008duqXIAQ (case_id=cs_interactive_106)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `19`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed, internal_team, async_update, time_window
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 7, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - This has happened previously too, what is going on?
  - Happy new year
  - I dont have access to that number anymore but I have the same email
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008VIldIAG (case_id=cs_interactive_107)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `13`
- transcript outcome: `escalate`
- transcript reason: repeated unresolved/account-confusion signal(s) in visitor turns
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - I won't let me enter the ad as it's been removed but you have charged me over £10 and taken the money don't understand why it's been removed just trying to rehome my do
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008vzQLIAY (case_id=cs_interactive_108)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `27`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 1, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - I went through and noticed that the ad was duplicated
  - You too
  - Okay, thank you.
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008TtirIAC (case_id=cs_interactive_109)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `19`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 6, 'handover_or_case': 0, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - 1507275061
  - no that’s fine. Thanks Cortney
  - when can I expect your email?
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q50000090VmbIAE (case_id=cs_interactive_110)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `17`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed, internal_team, async_update, time_window
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 8, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - Hi Courtney I was disconnected from previous chat
  - All the best
  - What's the problem? its my phone I have full paper work from Apple
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008pLAfIAM (case_id=cs_interactive_111)

- original primary_uc (HR): `OUT_OF_SCOPE_PRO_CONTRACT`
- final primary_uc: `UC-A`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `20`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: identifier_confusion
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - 1508819706
  - ok how linked two accounts
  - I am not understand realy what you talking about I will investigate and fill complain to Ombudsmen , consider britch data policy act 1973 and 2010 I will write you officialy
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008wnoXIAQ (case_id=cs_interactive_112)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `23`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - Not the issue, I want to remove completely all my expired ads
  - you too
  - Thanks, I will
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008i0yzIAA (case_id=cs_interactive_113)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `28`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed, internal_team, async_update, time_window
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 10, 'handover_or_case': 0, 'identifier_context': 29, 'user_requested_human': False}`
- representative user messages:
  - YOU KEEP REMOVING MY ADDS THIS IS DISGUSTING BEHAVIOUR NO VILATION ARE BEEN DONE
  - well how can i keep my email address up to date if you stop use [EMAIL] was removed 2016
  - plus in the uk its against the law to stop people from updating there email address
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008g9GnIAI (case_id=cs_interactive_114)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 3, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - Ad ID: 1508292738
  - Okay thanks
  - Is it best to delete and sign up again?
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008sV8fIAE (case_id=cs_interactive_115)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `26`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 3, 'handover_or_case': 4, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - please help me
  - i don't need any refund
  - Alright, Thank You!
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fkdNIAQ (case_id=cs_interactive_116)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `28`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 8, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - Basically, when I view my ad, the call and messages function is in Grey and Customer cannot contact me directly via telephone call or Messages on my mobile, so what is the reason
  - It’s I’m possible For other two account holders We’re unable to make a call
  - When wil this be looked at? On a timely basis , Later today once I provide all info?
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008eJx0IAE (case_id=cs_interactive_117)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `23`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 3, 'handover_or_case': 1, 'identifier_context': 20, 'user_requested_human': False}`
- representative user messages:
  - Hello Bulelwa I hope you are well
  - I also have two spare rooms in my house, and I'm looking to rent them out to earn a bit of extra money. So I advertised one of these rooms online on gumtree today, and it was removed straight away with no explanation.
  - Thank you for trying to get it resolved because I don’t understand what the issue is
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008VrMoIAK (case_id=cs_interactive_118)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `intake_tool_contract_turns.csv`
- turn count: `34`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 3, 'identifier_context': 27, 'user_requested_human': False}`
- representative user messages:
  - I had issues with my ad
  - ok it’s been confusing
  - hopefully we can get this resolved
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008iDL7IAM (case_id=cs_interactive_119)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `handover_turns.csv`
- turn count: `13`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - Hi Cortney
  - Thanks. It's [EMAIL]. I can still access my old emails if it helps to verify me but I just can't send or receive any new emails.
  - OK, thank you
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q50000091um9IAA (case_id=cs_interactive_120)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `handover_turns.csv`
- turn count: `47`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - Cool
  - I just need my funds to order
  - Gumtree stuff it's cancelled
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008LordIAC (case_id=cs_interactive_121)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `handover_turns.csv`
- turn count: `32`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 5, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - Hi, I placed an ad a few weeks ago and paid £7.99 - I was not aware it was a subscription and was just charged another £7.99 - please note I have had no enquiries so was a bit frustrated to be charged again. I have deleted the ad, please can you confirm if I will be refunded for the most recent charge?
  - So what does this mean please?
  - Your auto renewal has been cancelled
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008VPqMIAW (case_id=cs_interactive_122)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `handover_turns.csv`
- turn count: `18`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): manager, escalation
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 0, 'handover_or_case': 3, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - I’m currently experiencing ongoing issues with the Gumtree app. Messages are still not working properly, and new adverts are now taking several hours to go live. In addition, most of my previously live adverts have been removed. These adverts were already live; I only edited the dates, after which they were taken down. Please see the attached screenshots for reference. I would appreciate your assistance in resolving this as soon as possible. Kind regards, Hakan
  - Ok. Thanks
  - Damien Silavant
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): manager, escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fJRtIAM (case_id=cs_interactive_123)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `handover_turns.csv`
- turn count: `22`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): handover
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 3, 'user_requested_human': False}`
- representative user messages:
  - You keep e mailing me to post this item but it has been posted today ?
  - Nat, I'm sorry about this. We can see that the parcel has been collected from you. Apologies for any inconvenience caused.
  - Nat I'm still here, just looking into this
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008Ro2XIAS (case_id=cs_interactive_124)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `handover_turns.csv`
- turn count: `33`
- transcript outcome: `escalate`
- transcript reason: visitor explicitly requested a human or manager
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 4, 'handover_or_case': 1, 'identifier_context': 3, 'user_requested_human': True}`
- representative user messages:
  - just checking the procedure re people making a purchase online.
  - 4. gumtree tranfers payment to me
  - thansk and bye now
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008b9KfIAI (case_id=cs_interactive_125)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `handover_turns.csv`
- turn count: `15`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - Please,is not fair to get the money from my account out of my approve,that is stealing!!!
  - Costumer service phone number??
  - I will pay when I want not when you want to get the money!!!
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008gVpCIAU (case_id=cs_interactive_126)

- original primary_uc (HR): `OUT_OF_SCOPE_DELIVERY`
- final primary_uc: `UC-A`
- turns file: `handover_turns.csv`
- turn count: `24`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 3, 'handover_or_case': 0, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - The seller agreed to sell me an ikea soderhamn sofa and chair. I hired a van and my son in law took a day off work to do so. Date arranged was 22.12. Seller contacted me on 19.22 to say he'd sold it to someone else. I had been in contact with him from 3.12, trying to buy the items, waiting through sales that had fallen through, keeping in regular contact as I really wanted to buy. I contacted gumtree around 27.12 but got no reply. Ad now deleted so I can't provide I'd number, sorry
  - I was so disappointed, especially as I'd tried so hard to get to buy. Items were perfect for me. Also, as you'll see from messages, I'd had an op on 3.12, so it really was a huge effort for me, which I conveyed in my message exchange with him
  - Sorry, meant reneging on our agreement I meant
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008sQ2XIAU (case_id=cs_interactive_127)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `handover_turns.csv`
- turn count: `28`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 13, 'user_requested_human': False}`
- representative user messages:
  - I recently posted a Afghani hand woven rug on gumtree.
  - Can you refund the amount he sent to me back to the customer.
  - 0415510234
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008TIZdIAO (case_id=cs_interactive_128)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `escalation_turns.csv`
- turn count: `25`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 16, 'user_requested_human': False}`
- representative user messages:
  - How do I find that while chatting?
  - Beautiful Stika Spruce Cutaway Dreadnought guitar in excellent condition. Lovingly maintained since I bought it a few years ago.. it comes with Excellent hard case and a new set of strings..I'm reluctantly selling asi don't play it enough to keep. Will take off £70 if case is not needed.
  - I description outlined in red saying my description cannot contain website links and to use website link field?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008OpkjIAC (case_id=cs_interactive_129)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `escalation_turns.csv`
- turn count: `9`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - I renewed an old ad a few days ago for a garage and paid £8.99 but can't see anything in live ads
  - Payment has been taken by Gumtree
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008wmKbIAI (case_id=cs_interactive_130)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-A`
- turns file: `escalation_turns.csv`
- turn count: `46`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): handover
- evidence signal counts: `{'unresolved_user': 4, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - Why did the last person end the chat
  - I don't understand why you didn't do this about 20 mins ago
  - Well thank you
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): handover; trigger=user_requested
- classification override applied: **YES** -> primary=UC-A, secondary=['UC-H', 'UC-D']
- case-level override applied: **YES**
  - override source: Wave A2.1 reclassification map
  - reviewer: legacy migration from extractor.py UC_B_RECLASSIFICATION_OVERRIDES (Wave A2.1)
  - date: 2026-04-30
  - confidence: `high`
  - supporting turns: `[]`
  - rationale: Trader flag wrong on account -- UC-A with UC-H / UC-D fallback.
  - status: `approved`
  - migrated_from_legacy: `true`
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fyMzIAI (case_id=cs_interactive_131)

- original primary_uc (HR): `UC-FP`
- final primary_uc: `UC-FP`
- turns file: `escalation_turns.csv`
- turn count: `12`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - 1508288194
  - please active my add it's my business paid add
  - it's my first add i have not created any violation so please check
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008q19tIAA (case_id=cs_interactive_132)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 4, 'handover_or_case': 2, 'identifier_context': 2, 'user_requested_human': False}`
- representative user messages:
  - I want to make a complaint about Candice - a rude and impolite Gumtree
  - Thank you. Must go now
  - Hoping it can be resolved ASAP, especially as a customer for over 10 years. Thanks
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ZBLlIAO (case_id=cs_interactive_133)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `25`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 13, 'user_requested_human': False}`
- representative user messages:
  - Hi I can't upload 2 photos to a new advert I was wondering if I send them to you can you up load for me
  - I'll leave chat and wait for your email.
  - Do you want me to send you the pictures
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008zV0fIAE (case_id=cs_interactive_134)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `17`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): handover
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 0, 'handover_or_case': 1, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - I have that email setting as my nominated email and it used to work. I tried to look into it and it shows two email addresses. I tried to delete the other one just in case but I'm not allowed to for some reason.
  - And I'm not sure it would change the error anyways
  - Ok, well at least I know now what to do when I send the next ad up. Thanks so much for your help!
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): handover; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008NMzJIAW (case_id=cs_interactive_135)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - sorry I can't find the AD id
  - Not right now. Thanks for your help
  - Brilliant, thanks so much
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008U8OXIA0 (case_id=cs_interactive_136)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 2, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - Thanks, can you tell me whether my reply on the web-site has been delivered to the potential purchaser?
  - Thanks, hopefully not too late if she/he still interested.
  - just about an hour ago
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kwQzIAI (case_id=cs_interactive_137)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `19`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation, case_reference
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 4, 'handover_or_case': 3, 'identifier_context': 1, 'user_requested_human': False}`
- representative user messages:
  - No, I just received messages from people asking for me to reply even though I have, and now I have tested this and tried sending messages to my husband via gumtree but they all did not go through
  - Nobody replied to any of my messages, but a few send reminders (2 listings in total) and I tested a third which also did not work
  - did you get it?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation, case_reference; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008tdAbIAI (case_id=cs_interactive_138)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `10`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: identifier_confusion
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - Tell me the Gumtree Email address where I can outline a problem
  - After a chat yesterday, it appears that I have two Gumtree accounts. One uses [EMAIL] and the other is gleck30@gmail .com The former address has not been in use for over ten years. My recently placed ad has received messages which were not sent to my email address. Is it possible for you to
  - simply sort out ONE account using the [EMAIL] and ensure that any ad messages will be duplicated by Email. I would prefer to continue with Gumtree instead of deleting the entire App..
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008SajtIAC (case_id=cs_interactive_139)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `26`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed, internal_team, async_update, time_window
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 10, 'handover_or_case': 0, 'identifier_context': 21, 'user_requested_human': False}`
- representative user messages:
  - Hi Cortney
  - this one i've not send any message yet to anyone from this
  - Sir is there any restriction with my new account also please clear
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: investigation_needed, internal_team, async_update, time_window; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008RsXZIA0 (case_id=cs_interactive_140)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `21`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): case_reference
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 5, 'handover_or_case': 1, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - I contacted you last week and you said it would be looked into. Why is all my ads being taken down?
  - I have had no payment history. You wont let me sell anything! No order ID. You wont let me sell anything! I put up 4 previous ads.
  - no. just this issue. Thanks
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): case_reference; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008pDOPIA2 (case_id=cs_interactive_141)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `17`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 2, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - I am trying to reply to ads for electricians, I am filling out the online form but when I press 'sedn' nothing happens
  - first attempt today
  - I cant send request, therefor I am NOT getting any replies.
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008cg85IAA (case_id=cs_interactive_142)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `27`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 1, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - I’m getting emails saying I have incoming messages that I haven’t replied to but there’s nothing in my inbox
  - No thanks, if you could try and sort asap please in case I have people interested in my ad
  - There’s no messages there at all
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008uyhdIAA (case_id=cs_interactive_143)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `25`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 15, 'user_requested_human': False}`
- representative user messages:
  - I am not receiving customer messages on my email?
  - Understood and out
  - Thanks for claryfying. Is there another way around it?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008sydhIAA (case_id=cs_interactive_144)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `18`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed, async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 0, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - It just says David at the top ?
  - [EMAIL], thank you !
  - Your chat will not let me copy and paste the message . It is from someone called Jordan
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: investigation_needed, async_update; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008rf6rIAA (case_id=cs_interactive_145)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `15`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 2, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - people don't get my massage
  - have a good day
  - anyway i will be waiting for ur reply thanks
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008xR9FIAU (case_id=cs_interactive_146)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `13`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed, async_update, time_window
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - Thankyou I’m trying to sell a drill and I had someone called Christopher ask if it was still available so i replied yes then he text saying who cares and also another person asked about stereo I’m selling can I deliver to India the thing is it’s stopping me from selling my items
  - Thankyou very much
  - The dates are today’s date on both items and the one for the drill is Christopher and the stereo is Santosh
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: investigation_needed, async_update, time_window; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q50000090FptIAE (case_id=cs_interactive_147)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `escalation_turns.csv`
- turn count: `16`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - How do I therefore place an ad?
  - The old address is closed so no messages will reach me.
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008b7xBIAQ (case_id=cs_interactive_148)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `escalation_turns.csv`
- turn count: `14`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 16, 'user_requested_human': False}`
- representative user messages:
  - I have changed my phone number and it is hard to log in
  - I don't understand what is AD ID
  - No account under [EMAIL]?? That's strange
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008NTUXIA4 (case_id=cs_interactive_149)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `escalation_turns.csv`
- turn count: `26`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 20, 'user_requested_human': False}`
- representative user messages:
  - when i sign in on the app its fine but it wont let me sign in on a browser i.e google chrome
  - thank you for your help sorry for bothering you have a good Christmas and a new year :)
  - yeah im signed in now thank you
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: async_update; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008dCKrIAM (case_id=cs_interactive_150)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `escalation_turns.csv`
- turn count: `21`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - ad id - 508088108
  - I cant access my acc
  - ok thank you
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008lUUHIA2 (case_id=cs_interactive_151)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `escalation_turns.csv`
- turn count: `44`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 3, 'handover_or_case': 2, 'identifier_context': 22, 'user_requested_human': False}`
- representative user messages:
  - Yes received link have tried it several times it keeps saying wrong email
  - i do not use the one above never have so not sure where that is from but its not me
  - Thank you for your help
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008uPn7IAE (case_id=cs_interactive_152)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `escalation_turns.csv`
- turn count: `25`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 19, 'user_requested_human': False}`
- representative user messages:
  - can you delete the account with the gmx.com and leave just the gmail please, it's very confusing and currently nothing works
  - I went through the link sent to me to open a new account
  - there is nothing in my gmail emai
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008q1D7IAI (case_id=cs_interactive_153)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `escalation_turns.csv`
- turn count: `34`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - you cancelled the link mprovided before I could create a new pass word
  - confusing.
  - ok thank you
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ijssIAA (case_id=cs_interactive_154)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `escalation_turns.csv`
- turn count: `48`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 4, 'handover_or_case': 0, 'identifier_context': 20, 'user_requested_human': False}`
- representative user messages:
  - I can’t get in to my account, when I reset password it fails saying there is a problem and try later. I logged on using Apple but that just opened a new account
  - [EMAIL]. Not sure about the Apple ID, it was all automated
  - Thanks for your help 👍
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ZrzJIAS (case_id=cs_interactive_155)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `escalation_turns.csv`
- turn count: `12`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 2, 'handover_or_case': 2, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - I’m trying to reset email but it’s not sending me the link [EMAIL]
  - Could you also check please if I have an account for a different email address [EMAIL]
  - Ok thanks, will you be sending me something so I can reset the password for the other account?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008qnmPIAQ (case_id=cs_interactive_156)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `escalation_turns.csv`
- turn count: `18`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - How can I get email messages for items I have for sale then pls
  - This is not helpful at all :(
  - Are you able to transfer my items for sale to my daviesnineteenseventy account
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008W3hJIAS (case_id=cs_interactive_157)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `escalation_turns.csv`
- turn count: `28`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 6, 'handover_or_case': 2, 'identifier_context': 21, 'user_requested_human': False}`
- representative user messages:
  - my ad was removed
  - really appreciate your help
  - I don’t want to look bad
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008dEg1IAE (case_id=cs_interactive_158)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `escalation_turns.csv`
- turn count: `21`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - Why has my account been restricted?
  - Ridiculous, can’t see any reason why this would be the case
  - I’ve posted adverts a number of times a few years ago
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008iy8jIAA (case_id=cs_interactive_159)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `escalation_turns.csv`
- turn count: `21`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - I am the landlord
  - Ok thanks
  - Hello are you there
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008g4qbIAA (case_id=cs_interactive_160)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `escalation_turns.csv`
- turn count: `22`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - I have tried to put a add on can you please let me know why there are deleting
  - Okay thank you
  - Try that one
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008NSYTIA4 (case_id=cs_interactive_161)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `escalation_turns.csv`
- turn count: `31`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 4, 'handover_or_case': 0, 'identifier_context': 19, 'user_requested_human': False}`
- representative user messages:
  - Great
  - But i do not understand the reason as the points on the email do not make sense to me…
  - and also email of my add removed
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008f6w5IAA (case_id=cs_interactive_162)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `escalation_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - I’ve just created an account and I’m trying to sell but it removes the adverts immediately
  - Okay how will they contact me
  - Are you still there?
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008oe7LIAQ (case_id=cs_interactive_163)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `escalation_turns.csv`
- turn count: `15`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 2, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - You have deleted my ad. I wanted to list it
  - Thank you hopefully ad will go live soon
  - Ok obviously problems at your end thank you
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008btQjIAI (case_id=cs_interactive_164)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 2, 'user_requested_human': False}`
- representative user messages:
  - ok, thanks
  - thanks a lot
  - ok, I didn't know that
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: async_update; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008dLpZIAU (case_id=cs_interactive_165)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `golden_turns.csv`
- turn count: `23`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - Using the App but when I select pictures from my gallery it does not upload and checking the FAQ pages I think they are all fine to use.
  - I will try that, thanks. Bye.
  - Will I lose my current ads if I do that?
- outcome decision: partial_uc_transcript_resolve: transcript evidence indicates resolution; reason=agent transcript contains resolution/answer confirmation and no unresolved user signal
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008WpyrIAC (case_id=cs_interactive_166)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - I need to make my ad live
  - Yes, they asked me to change my password, and I did
  - AD IS REMOVED
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: investigation_needed; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008xH0DIAU (case_id=cs_interactive_167)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - Hi Jason, i've used GT for a number of years but not of late,, tried to log in using my old stuff, but says wrong PW, ? click the link to change PW but the email never arrives? so thinking I'll start again with a new account it wont let me saying I already have an account, help?
  - signing out to do that, bye
  - ok, thanks
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: async_update; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008vITRIA2 (case_id=cs_interactive_168)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `badcase_turns.csv`
- turn count: `10`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): manager, escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 5, 'handover_or_case': 3, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - I want to come of the site deleted it but still on it why
  - Who is the account manager
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008TTI9IAO (case_id=cs_interactive_169)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `badcase_turns.csv`
- turn count: `14`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - Hello, it’s me again you ended the chat? Why did you end the chat I still had queries
  - I want to ask on the progress of my refund
  - Alright
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008gVUDIA2 (case_id=cs_interactive_170)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `badcase_turns.csv`
- turn count: `15`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Hi you have made it now harder for sellers taking the call feature away now sellers will feel it harder not always there for email email is not safe you can be spammed more with email than calling someone now sellers will not use the service as you get time waisters on email your not always there to get email sales will be lost then sellers will leave gumtree now and use other platforms
  - I will move to another platform that doesn't take my rights away and ine where I can use a telephone number as your a selling platform and there should be a call setting on a platform
  - Goodluck on losing a lot of customers everyone is not happy we won't use the service again until call option is returned
- outcome decision: partial_uc_transcript_resolve: transcript evidence indicates resolution; reason=agent transcript contains resolution/answer confirmation and no unresolved user signal
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008gTqbIAE (case_id=cs_interactive_171)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `14`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): handover
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 2, 'identifier_context': 3, 'user_requested_human': False}`
- representative user messages:
  - I'm just letting you know I'm not happy that you've taken the call feature away from me putting add in I will no longer use your service no more that's terrible nothing to fo with safety
  - If they don't your going to lose a lot of users I will never use gumtree thanks goodbye
  - You've just list a user and I will be encouraging online also for other people to stop using gumtree ebay started this to a few years ago and now they've lost lots of users sill move on your part
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008k3uMIAQ (case_id=cs_interactive_172)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `badcase_turns.csv`
- turn count: `10`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - Hello, I have just seen the 1 star review I have on my profile. This review came from a scammer. He advertised an item and I he sent bank details that did not match his name and had no images of the item, all were taken from online and I reported the matter to Gmtree but he has left a 1 star review on my profile which has negatively impacted my ability to sell items
  - Do you have an email address for an official complaint form to be filled out
  - Ok I’ll delete my account then and go else where if that’s how gumtree treats genuine folk reporting scammers and fraudsters
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kJHZIA2 (case_id=cs_interactive_173)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `drift_control_turns.csv`
- turn count: `26`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 5, 'human_investigation': 3, 'handover_or_case': 2, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - I would like my add reposted or a refund as it was only active for around 20 minutes . Thank you
  - Why does it need a specialist team can you explain why . The add was approved then removed . My payment was still taken .
  - Just issue the refund . Thanks
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008sDmrIAE (case_id=cs_interactive_174)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `drift_control_turns.csv`
- turn count: `30`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 16, 'user_requested_human': False}`
- representative user messages:
  - It says 55 replies and when i click on it no replies come up
  - First of all I am not using an app and never said I was full stop secondly what on earth is contact email and login email I have no idea what you're talking about
  - I will leave it like that for now. Do you save these conversations?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fMw1IAE (case_id=cs_interactive_175)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `drift_control_turns.csv`
- turn count: `23`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 2, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - ??
  - Strange, as I am also on care.com and I receive three replies an hour! Perhaps I should cancel my ad with Gumtree and get.a refund, please, as something is not right. Thank you in advance. ctb
  - Ah, sorry, let me try.
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008NMRRIA4 (case_id=cs_interactive_176)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `handover_turns.csv`
- turn count: `10`
- transcript outcome: `escalate`
- transcript reason: visitor explicitly requested a human or manager
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 2, 'user_requested_human': True}`
- representative user messages:
  - Give me my £50 back or put my business to the top like I paid for
  - That's good why is this so hard to do
  - What about giving a phone number to talk to someone
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=user_requested_human,unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=visitor explicitly requested a human or manager; trigger=user_requested
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
  - escalation_trigger: HR='clarification_budget_exhausted' vs final='user_requested'
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008aPw9IAE (case_id=cs_interactive_177)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `handover_turns.csv`
- turn count: `14`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): handover
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - With new account ill lose 3 good revs
  - At least you know about a problem with search, in the time being well have to switch to local facebook, hope that only temporarily
- outcome decision: partial_uc_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): handover; trigger=intake_complete_for_uc_k
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008pwJtIAI (case_id=cs_interactive_178)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `27`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - Sure, thanks
  - It does. Many thanks and have a good day
  - Thank you. Man, some people are so rude!
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008xcB7IAI (case_id=cs_interactive_179)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `25`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 13, 'user_requested_human': False}`
- representative user messages:
  - Could you contact the lady or unblock her as I can't unblock her I blocked her messages by mistake. The cat is black and white
  - I've got her email [EMAIL] but can't get hold of her it's so upset me as I would have had the cat and we agreed
  - No it says message has been sent but not received obviously
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008g16LIAQ (case_id=cs_interactive_180)

- original primary_uc (HR): `OUT_OF_SCOPE_RATINGS_REVIEWS`
- final primary_uc: `UC-A`
- turns file: `golden_turns.csv`
- turn count: `42`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 15, 'user_requested_human': False}`
- representative user messages:
  - I would like to edit or delete a review I left recently. Thanks.
  - Can you not delete the review from the seller's profile?
  - I no longer have the messages, sadly.
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008iJLhIAM (case_id=cs_interactive_181)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `19`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - An change removing phone numbers from the platform has been implemented. However, other articles state that one should not divulge address telephone number or email. I had a potential buyer interested in an add. Who requested to collect on Sunday. While I haven't given the address as yet. I replied to say. Come Sunday, if still available you are more than welcome to collect. My question is. If the buyer messages again in Sunday. If I cannot divulge a telephone number, email or address, how is buyer and seller meant to complete?
  - If a change like the one implemented has taken place. The help/selling page should have been updated on the same day to reflect this. As it is the first place that users would go to for a source of help. To do that would have taken minutes to do. While In know this is not your fault. I feel that this will generate lots of contact from people asking the same questions and feeling frustrated. However, on that note. I must thank you for your kindness and patience and taking your valuable time to courteously reply to my questions. It has been appreciated.
  - You are very welcome. Goodbye 😊
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q50000090NNdIAM (case_id=cs_interactive_182)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `28`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - I've already been on live chat with one of your colleagues today and he said to restart my phone and check app for updates but I have done that and no difference. I have also tried opening in web browser and again showing no replies from customers interested in my items being sold
  - Ok just seems odd they both haven't replied or read message and Gumtree saying I have unread messages but if all ok from what you can see then must be
  - I sent 3 days ago
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kyW1IAI (case_id=cs_interactive_183)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `17`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 2, 'user_requested_human': False}`
- representative user messages:
  - Okay thank you
  - Haha okay. Thank you for your help today. Have a good evening.
  - How long does that take?
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008G3WnIAK (case_id=cs_interactive_184)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `16`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - My adds are not attracting any interest at all and no replies to other adds either
  - messages
  - l don’t think my adds are visible and something wrong with access to
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008dwQvIAI (case_id=cs_interactive_185)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `20`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - Gumtree told me to clear cookies by clicking on the 4 dots when I should get More Tools but I don’t get more tools
  - Thanks. I will try that, Trevor
  - I had [EMAIL] but Microsoft blocked it so I am now using [EMAIL]
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008q1bJIAQ (case_id=cs_interactive_186)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `23`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - Why did my ad not go on and also was chatting on here before I only have this account
  - For the caravan
  - So what is the issue
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: async_update; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008NV89IAG (case_id=cs_interactive_187)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - I was rudely dsicomnwcyed form my previous agent called Narran m
  - Thankyou
  - This has been going on for a month and need to have this looked into
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: investigation_needed; trigger=user_requested
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008nJOIIA2 (case_id=cs_interactive_188)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `golden_turns.csv`
- turn count: `25`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update, time_window
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 0, 'identifier_context': 17, 'user_requested_human': False}`
- representative user messages:
  - I didn't choose that option...was never given any options.
  - Still nothing but the ad...no options etc
  - No nothing just the ad
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: async_update, time_window; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008uP2LIAU (case_id=cs_interactive_189)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `golden_turns.csv`
- turn count: `27`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: identifier_confusion
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 15, 'user_requested_human': False}`
- representative user messages:
  - Sorry i must have two accounts and i cannot log into the old one to take an add down
  - so i just have debsmc account now with nothing on too
  - old
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000009060DIAQ (case_id=cs_interactive_190)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-A`
- turns file: `golden_turns.csv`
- turn count: `34`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: paid_service_impact
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - Let me check
  - Paid Role – Wellness Session Support (Urgent)
  - Finally which job category gets the most views
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- classification override applied: **YES** -> primary=UC-A, secondary=['UC-K']
- case-level override applied: **YES**
  - override source: Wave A2.1 reclassification map
  - reviewer: legacy migration from extractor.py UC_B_RECLASSIFICATION_OVERRIDES (Wave A2.1)
  - date: 2026-04-30
  - confidence: `high`
  - supporting turns: `[]`
  - rationale: "Where is my ad" -- UC-A with UC-K fallback.
  - status: `approved`
  - migrated_from_legacy: `true`
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008w24rIAA (case_id=cs_interactive_191)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-FP`
- turns file: `golden_turns.csv`
- turn count: `19`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - https://www.gumtree.com/p/sports-massage-services/diamond-star-massage/1509201102
  - My ad id
  - Here’s is my ad id
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- classification override applied: **YES** -> primary=UC-FP, secondary=['UC-F']
- case-level override applied: **YES**
  - override source: Wave A2.1 reclassification map
  - reviewer: legacy migration from extractor.py UC_B_RECLASSIFICATION_OVERRIDES (Wave A2.1)
  - date: 2026-04-30
  - confidence: `high`
  - supporting turns: `[]`
  - rationale: Cancel auto-renewal -- UC-FP with UC-F fallback.
  - status: `approved`
  - migrated_from_legacy: `true`
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_message_moderation_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008rbcjIAA (case_id=cs_interactive_192)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-B`
- turns file: `golden_turns.csv`
- turn count: `29`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - OK. What items are allowed?
  - OK. Thanks once again.
  - Before we end. Do I need to post photos as well?
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008c5TWIAY (case_id=cs_interactive_193)

- original primary_uc (HR): `UC-A`
- final primary_uc: `UC-A`
- turns file: `golden_turns.csv`
- turn count: `32`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 23, 'user_requested_human': False}`
- representative user messages:
  - GER_01436904
  - gerdun123@googlemail
  - What email was used? I saw this
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: investigation_needed; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008YlntIAC (case_id=cs_interactive_194)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `golden_turns.csv`
- turn count: `42`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: paid_service_impact
- evidence signal counts: `{'unresolved_user': 3, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 26, 'user_requested_human': False}`
- representative user messages:
  - My postcode is en76jj
  - I got charged 24. 99 on my Amex card from gumtree
  - I bumped up many days ago
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008SYYPIA4 (case_id=cs_interactive_195)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `33`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - That's OK, I'm keen on contacting Anna & ideally speaking to her re this advert, but am not getting any response to my messages
  - OK thanks for your time
  - Can you text her ?
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kj8nIAA (case_id=cs_interactive_196)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `30`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - Good morning, the above ID is just one of many for which I always leave a review, good or bad, my issue is that NONE of the reviews I leave are published against the buyer or potential buyer's account
  - I will monitor things, have a good day.
  - I will monitor this, how long doe it take to have a buyers reives left updated?
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008flO9IAI (case_id=cs_interactive_197)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `35`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Hello Monisha
  - Sure take your time
  - i have just sent this message
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fvYoIAI (case_id=cs_interactive_198)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `20`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - Hello Carmen
  - Carmen sorry
  - and my phone number is not in site i ask for pout plz
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008xYibIAE (case_id=cs_interactive_199)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `20`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - i see my last message about a week ago
  - are we still talking
  - this does not make sense. if i click on 'reply to message' nothing happens, if i look at messages in website no message from Denver exists
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q50000091nMTIAY (case_id=cs_interactive_200)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `23`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - Basically I need to contact the seller of the last item i bought. I have unfortunately deleted the message trail :( can you help me retrieve it please?
  - You too !
  - Yes 👍 much appreciated thank you!!
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: async_update; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fDKrIAM (case_id=cs_interactive_201)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `21`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Hola
  - Ok thanks anyway
  - Even though I’ve sold on there so wouldn’t be possible
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008LvbNIAS (case_id=cs_interactive_202)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `17`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - I contacted you last week about my account and still no response this is annoying
  - I did to the hotmail and it's still not been sorted
  - No just sort my account so I can use
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008zgAbIAI (case_id=cs_interactive_203)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `25`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - Hi Carmen, I have been missold a car on gumtree how do I report this?
  - Car was purchased via telephone and we paid cash.
  - Ah ok send the email it says ok
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008uM1FIAU (case_id=cs_interactive_204)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `29`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - I am having trouble contacting a seller
  - Today but have messaged them previously to no avail.
  - Ad id: 1508896497
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008uNuzIAE (case_id=cs_interactive_205)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `18`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: confusion
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - Neither
  - Not sure which is linked to my account
  - Il just find another seller.
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008gm0PIAQ (case_id=cs_interactive_206)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `golden_turns.csv`
- turn count: `21`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - Hi there I have messaged about an ad and planning on going to see dog but the ad has now expired
  - No worries thanks anyway
  - No worries
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008XMjVIAW (case_id=cs_interactive_207)

- original primary_uc (HR): `UC-A`
- final primary_uc: `UC-A`
- turns file: `badcase_turns.csv`
- turn count: `33`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 33, 'user_requested_human': False}`
- representative user messages:
  - The ad which disappeared is for a budgie and cage, only posted 2 days ago. Which I paid for
  - You have not asked me which account I want to use moving forward because I only have one account!!!!!!
  - I ALSO TOLD YOU THAT IS HOW I KNOW IT IS LINKED TO THAT EMAIL ADDRESS. YOU ARE NOT LISTENING TO A WORD I HAVE SAID
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kjFGIAY (case_id=cs_interactive_208)

- original primary_uc (HR): `UC-FP`
- final primary_uc: `UC-FP`
- turns file: `badcase_turns.csv`
- turn count: `24`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 3, 'user_requested_human': False}`
- representative user messages:
  - Hi can u plz connect me with Monisha thanks
  - You're welcome and same to you, take care :)
  - Take care
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008vFCDIA2 (case_id=cs_interactive_209)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `25`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): handover
- evidence signal counts: `{'unresolved_user': 12, 'human_investigation': 0, 'handover_or_case': 1, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - Never used the app for sold items needing delivery, how does it work, how do I know if payment has been made etc
  - Just to be clear - sellers cannot receive their payout through PayPal. While buyers can pay using methods like Visa, Mastercard, Apple Pay, Google Pay and PayPal, the payout to the seller is always sent directly to the seller’s verified UK bank account via our payment partner. If you need more information you are welcome to look through our Help pages here https://www.gumtree.com/info/safety.
  - Bye bye
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008by5UIAQ (case_id=cs_interactive_210)

- original primary_uc (HR): `OUT_OF_SCOPE_RATINGS_REVIEWS`
- final primary_uc: `UC-A`
- turns file: `badcase_turns.csv`
- turn count: `22`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 6, 'handover_or_case': 2, 'identifier_context': 1, 'user_requested_human': False}`
- representative user messages:
  - 4th December
  - Feel this will put buyers off
  - This is the second time this has happen seems quite unfair as I recently had a buyer not turning up after I waited in all day, but never left them a bad review
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008NOD7IAO (case_id=cs_interactive_211)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `badcase_turns.csv`
- turn count: `28`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 2, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - I have been trying to delete my account since 13/11/25 as I no longer use it. Nearly a month on; I’ve had 2 emails asking me to clarify details however I am unable to confirm payments, posts, ID numbers. I have replied to this on both occasions, to which I have been ignored. I’ve emailed and contacted on the chat numerous times which I get told my case will be escalated; but I receive no reply. What is going on? Why can’t you delete my account? Only one email should be on an account so that should clarify which to remove.
  - I’ll await a reply
  - Is it 30 days Monday to Friday?
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kN3RIAU (case_id=cs_interactive_212)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `badcase_turns.csv`
- turn count: `38`
- transcript outcome: `escalate`
- transcript reason: repeated unresolved/account-confusion signal(s) in visitor turns
- evidence signal counts: `{'unresolved_user': 5, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 16, 'user_requested_human': False}`
- representative user messages:
  - Hello, my name is Thato. Thanks so much for reaching out! Please allow me a moment to review your request so I can find the best solution for you.
  - I am not sure but truth be told you should have done the right thing from tthe beginning
  - Please enjoy the rest of your evening
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008u7qDIAQ (case_id=cs_interactive_213)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `40`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 6, 'handover_or_case': 0, 'identifier_context': 16, 'user_requested_human': False}`
- representative user messages:
  - Do you have a record of my last conversation
  - I believe my item was sold on your platform and was scammed and have spoken to police and been advised Gumtree should refund the amount it was sold for
  - I have proof of pakaging and posting, buyers address
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008hKEzIAM (case_id=cs_interactive_214)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `badcase_turns.csv`
- turn count: `24`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - I do not want to be contacted by message I want Phone.
  - I won't use messaging service .
  - can I put my number in the AD, or will it get rejected?
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008gdA1IAI (case_id=cs_interactive_215)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `badcase_turns.csv`
- turn count: `18`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - Where can I find the number
  - Phone number please
  - Complaints department please
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008eVzlIAE (case_id=cs_interactive_216)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `17`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - Someone left a negative review, i dont know how or why and I can't get rid of it. Can you help?
  - Oh man...its clearly fraudulent.
  - Who done it? Because I dont recall selling anything!!
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008cdi5IAA (case_id=cs_interactive_217)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `drift_control_turns.csv`
- turn count: `18`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - No error message. I just go on gumtree and it's on my account already
  - This has been going on for months though
  - I just browse
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: investigation_needed; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008P2mpIAC (case_id=cs_interactive_218)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `drift_control_turns.csv`
- turn count: `19`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): handover
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 4, 'handover_or_case': 1, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - Thanks got scammed by callum on my messages
  - [28/11, 17:56] Harmik: Hi its Harmik [28/11, 17:56] Harmik: In regards the dual controls [28/11, 18:23] Callum: Hi mate, how are you? [28/11, 18:24] Callum: I'll send details over just now and post tomorrow,I ha e also hand written you instructions for fitment to your 207 aswell. [28/11, 18:25] Callum: Name: Callum Clark Bank: Monzo Bank Account number: 83367413 Sort code: 04-00-03 [28/11, 18:25] Callum: Total amount due is £59.35. [28/11, 19:13] Harmik: Ok thank you [28/11, 19:14] Harmik: Thanks mate [28/11, 19:26] Callum: Give me a message on here once it's through mate, I'll post it before 1pm tomorrow. Should be with you Monday [28/11, 19:28] Harmik: Will do Thanks [29/11, 09:29] Harmik: Payment made. [29/11, 13:12] Harmik: Can let me know when you posted the dual controls please [29/11, 13:19] Callum: I'll call you back my friend [01/12, 13:26] Harmik: Hiya mate did you manage to send the parcel today ? [01/12, 13:35] Callum: I'll call you back [01/12, 13:36] Harmik: Hi callum will be in work soon. If i dont answer just call please Thanks [01/12, 13:38] Harmik: Just text 🙄 I meant [01/12, 17:05] Harmik: Hi Callum is the car part been sent? Thanks [01/12, 18:52] Harmik: Please reply [01/12, 19:11] Callum: Hi mate. Not posted today. Post tomorrow, been soo busy up untill right this second. Will messge you tomorrow lunch time.. [02/12, 07:30] Harmik: Ok thank you [02/12, 18:37] Harmik: Mate seriously are you actually gonna send me the stuff ? Its been 3 days since I paid [02/12, 18:40] Harmik: You were gonna text me today ? [02/12, 18:42] Callum: Call you back. Yes posted today. [02/12, 18:42] Harmik: Cheers thanks [02/12, 18:43] Harmik: You have reference number? Thank you [03/12, 12:59] Harmik: You have a reference number? [03/12, 12:59] Harmik: When can i expect the parcel ? [04/12, 16:05] Harmik: Why you not answering my calls ? [04/12, 16:06] Harmik: If you send the parcel on Tuesday? I would have got it by now [05/12, 09:54] Callum: I'm away on a training course in. Germany this week mate. My mum has posted your parcel, but has not given me a tracking number. But I can assure you your parcel is on its way. [05/12, 09:55] Harmik: Ok thanks. Will let you know when I have received the parcel [08/12, 14:41] Harmik: I havnt received the parcel. Can you please tell me whats happening? [08/12, 14:44] Harmik: Thanks for scamming me mate [08/12, 14:54] Callum: No scam. I'm still in Germany. I'll phone my mum tonight and see what's going on. [09/12, 16:57] Harmik: Did you speak to your mum ? [11/12, 14:19] Harmik: Hi what's happening with the parcel ?
  - Gumtree got nothing to do with it ?
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008t4kkIAA (case_id=cs_interactive_219)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `drift_control_turns.csv`
- turn count: `32`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 1, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - It says payment failed
  - Not happening!!!
  - I think it's a technical issue at your end
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ppBxIAI (case_id=cs_interactive_220)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `drift_control_turns.csv`
- turn count: `34`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 4, 'handover_or_case': 3, 'identifier_context': 22, 'user_requested_human': False}`
- representative user messages:
  - Yes, I have.
  - With a different browser, the reset email has not arrived.
  - I can see chats for a 2017 advert - cannot find the ID for that.
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fG5qIAE (case_id=cs_interactive_221)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-FP`
- turns file: `drift_control_turns.csv`
- turn count: `24`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 13, 'user_requested_human': False}`
- representative user messages:
  - hello, i just placed an ad on gumtree which has the following ad id... 1508254218, it did not let me publish the ad with my phone number like it always has done please can you tell me why this is
  - and you
  - ok thank you for your help
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=faq_miss_threshold_exceeded
- classification override applied: **YES** -> primary=UC-FP, secondary=['UC-K', 'UC-C']
- case-level override applied: **YES**
  - override source: Wave A2.1 reclassification map
  - reviewer: legacy migration from extractor.py UC_B_RECLASSIFICATION_OVERRIDES (Wave A2.1)
  - date: 2026-04-30
  - confidence: `high`
  - supporting turns: `[]`
  - rationale: Ad breaking rules; phone rejected -- UC-FP with UC-K / UC-C fallback.
  - status: `approved`
  - migrated_from_legacy: `true`
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_message_moderation_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008L6DtIAK (case_id=cs_interactive_222)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `drift_control_turns.csv`
- turn count: `56`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): case_reference
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 5, 'handover_or_case': 1, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - Hi nurran
  - Ditto
  - 👌
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008t5NRIAY (case_id=cs_interactive_223)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `drift_control_turns.csv`
- turn count: `18`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - I'm trying to advertise my car for sale. It keeps saying car not road registered and it is. That's
  - It's not showing the ad now as i deleted it. But now i try to re list it still keeps saying not road registered. I don't even know what thst means
  - Oh leave it I'll advertise is somewhere else. I'm not messing about with sending messages. If you can't fix it it doesn't matter. I don't know what not road registered means or why its even an option
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008oarhIAA (case_id=cs_interactive_224)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-B`
- turns file: `drift_control_turns.csv`
- turn count: `26`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 4, 'handover_or_case': 1, 'identifier_context': 15, 'user_requested_human': False}`
- representative user messages:
  - I already have one ad on my account
  - sorry the email address is [EMAIL]
  - On the mobile app, I can upload photos but when I press Post, I get an error saying “something went wrong, please try again later.” I was advised to use a browser instead, but now on my laptop the page refreshes every time I try to upload photos, so I can’t complete the advert at all. This has been happening for the last couple of days.
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008noezIAA (case_id=cs_interactive_225)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `drift_control_turns.csv`
- turn count: `21`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 13, 'user_requested_human': False}`
- representative user messages:
  - [EMAIL] cant remember last time i logged in, maybe 9 months ago when i bought a car seat
  - i hope i haven't confused u.
  - i need to go out to pick my grandson up. can i discuss this later perhaps
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008NaSnIAK (case_id=cs_interactive_226)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `handover_turns.csv`
- turn count: `9`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): manager, escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 2, 'identifier_context': 2, 'user_requested_human': False}`
- representative user messages:
  - Please sort it out asap we already pay for that and our ads are not bumping up. I sent query yesterday as well but its still same
  - can you ask them to contact us on in this email [EMAIL] or call us on 07720 887859 we been trying to contact but its not working out.
  - Please sort it out asap we are really appreciate
- outcome decision: partial_uc_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): manager, escalation; trigger=intake_complete_for_uc_k
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fwY6IAI (case_id=cs_interactive_227)

- original primary_uc (HR): `UC-FP`
- final primary_uc: `UC-FP`
- turns file: `handover_turns.csv`
- turn count: `34`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 1, 'identifier_context': 24, 'user_requested_human': False}`
- representative user messages:
  - I am working on this account for the first time, so please allow me to do so. I have never worked on this account before.
  - Ok thank you appreciate
  - No thank you
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008btIgIAI (case_id=cs_interactive_228)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `handover_turns.csv`
- turn count: `31`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 25, 'user_requested_human': False}`
- representative user messages:
  - Ad ID: 1451657969
  - It says:Please can you provide (1) your registered email address on Gumtree.co.uk and (2) provide 2 of the 4 of the following information below to confirm you as the correct user: · Creation date of account; · Payments made on ads for insertion fees, features, etc.; · The order ID related to those payments (not the method of payment used); or · Number of Ads posted in the last 12 months.
  - Okay, thank you
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008Sh2DIAS (case_id=cs_interactive_229)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `handover_turns.csv`
- turn count: `18`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - I'm trying to post a job advert.
  - When I get to the time to pay I click payment and it just gets saved.
  - But I had this job ad running before, so I wanted the same ad
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fOplIAE (case_id=cs_interactive_230)

- original primary_uc (HR): `OUT_OF_SCOPE_RATINGS_REVIEWS`
- final primary_uc: `UC-A`
- turns file: `escalation_turns.csv`
- turn count: `23`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update, time_window
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - Today after 2pm I left the feedback, it was only feedback I left
  - Name of the buyer is Tan
  - He bought baby / toddler toys
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008f2hBIAQ (case_id=cs_interactive_231)

- original primary_uc (HR): `OUT_OF_SCOPE_RATINGS_REVIEWS`
- final primary_uc: `UC-A`
- turns file: `escalation_turns.csv`
- turn count: `6`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - Let me quickly tell you why I'm getting in touch. I have a one star review from about six years ago. The man in questions - Lee - replied to my add offereing a free TV. He asked some questions, took my telephone number and said he'd get back to me. By the time he called back, someone else had taken the TV. I had told him it was first come first serve. He was extremely abusive on the phone and left the bad review. Can this review be removed? I should have reported him at the time.
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
  - escalation_trigger: HR='user_requested' vs final='out_of_scope'
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008Lqi9IAC (case_id=cs_interactive_232)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `escalation_turns.csv`
- turn count: `54`
- transcript outcome: `escalate`
- transcript reason: visitor explicitly requested a human or manager
- evidence signal counts: `{'unresolved_user': 5, 'human_investigation': 1, 'handover_or_case': 2, 'identifier_context': 23, 'user_requested_human': True}`
- representative user messages:
  - Hello?
  - I've bumped my ad up for it to only be on the 1st page for less then a day. Now I've paid for it to be featured for 17 days but it isn't showing?
  - Separating them into brands makes it more difficult to sell
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008QNFhIAO (case_id=cs_interactive_233)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `11`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - Help me reset my password
  - I press forgot password but the email with the reset link doe not come
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008g81NIAQ (case_id=cs_interactive_234)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `18`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed, time_window
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - Euphemia4
  - Sorry not shore what that was about
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: investigation_needed, time_window; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fCNBIA2 (case_id=cs_interactive_235)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `22`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: still_unable
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - I need an email resent to reactivate my account
  - yes can't log in
  - no that's it many thanks
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008qmYbIAI (case_id=cs_interactive_236)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `13`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - September 2024
  - okay let me try 1 min
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008xNVRIA2 (case_id=cs_interactive_237)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `golden_turns.csv`
- turn count: `13`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 2, 'user_requested_human': False}`
- representative user messages:
  - Oxygen concentrator seventy pounds
  - At least its stopped raining 🌧 🙃
  - Yes thank you.
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008q5tRIAQ (case_id=cs_interactive_238)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `golden_turns.csv`
- turn count: `28`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - This is the first carer required job posting I have made. When checking for any replies to the job advert I come up as it is me liking the job!
  - No, that's all for now. Thanks again, cheers
  - Perfect, thankyou!
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008wU9BIAU (case_id=cs_interactive_239)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `golden_turns.csv`
- turn count: `25`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - I am new to this
  - Do you take a percentage commision?
  - Or via you like ebay?
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008xVG5IAM (case_id=cs_interactive_240)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `golden_turns.csv`
- turn count: `18`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 2, 'user_requested_human': False}`
- representative user messages:
  - Are you human
  - Ok, I llmrelist them
  - fridge magnet,and small item, boxing programs are light,
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fETpIAM (case_id=cs_interactive_241)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `golden_turns.csv`
- turn count: `17`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - Hello Nuraan
  - bye thanks
  - ok will thank you
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fjscIAA (case_id=cs_interactive_242)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `golden_turns.csv`
- turn count: `28`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 5, 'handover_or_case': 0, 'identifier_context': 19, 'user_requested_human': False}`
- representative user messages:
  - The option to use " Phone Only " is not there when I try to list an addI am away from my computer all day.
  - Please cotacting me by email on [EMAIL] Thanks for your help, Thierry
  - Is this a recent change to contact details?
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008bFgDIAU (case_id=cs_interactive_243)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `golden_turns.csv`
- turn count: `15`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - i paid for this ad id 1508016323
  - this was the best pacakge i bought but still i am not on front page
  - are you there
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008caqfIAA (case_id=cs_interactive_244)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `10`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 2, 'user_requested_human': False}`
- representative user messages:
  - Put the wrong sale price for dog.
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- classification override applied: **YES** -> primary=UC-D, secondary=[]
- case-level override applied: **YES**
  - override source: Wave A2.1 reclassification map
  - reviewer: legacy migration from extractor.py UC_B_RECLASSIFICATION_OVERRIDES (Wave A2.1)
  - date: 2026-04-30
  - confidence: `high`
  - supporting turns: `[]`
  - rationale: Wrong price on specific ad -- agent asked for ad ID; UC-D.
  - status: `approved`
  - migrated_from_legacy: `true`
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kKC1IAM (case_id=cs_interactive_245)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-B`
- turns file: `golden_turns.csv`
- turn count: `23`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - hi,my teephone numbers are missing from my ads????the only way of contact now is email-is this normal now
  - ok thanks and goodbye
  - yes did that and no change
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008uy1iIAA (case_id=cs_interactive_246)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-B`
- turns file: `golden_turns.csv`
- turn count: `17`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: still_unable
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Hi Jason, is it possible to change my location for the series of adverts I am currently positing? For example, whilst my home address is Blandford, Dorset the items are actually at my father-in-law's address which is Bournemouth, Dorset.
  - Understood, but according to your earlier statement that means I cannot post with the correct location i.e. Bournemouth. Is this correct or can I change it somehow?
  - Okay, I missed the fact that you could enter the location when posting the ad. Thanks for your help.
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fyDJIAY (case_id=cs_interactive_247)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-B`
- turns file: `golden_turns.csv`
- turn count: `10`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: internal_team
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 0, 'user_requested_human': False}`
- representative user messages:
  - What does that mean. Where should phone number be
  - So.....there is no point posting for the time being. When will I know when functioning correctly and where will the phone number be allowed to be placed?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: internal_team; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008zX8vIAE (case_id=cs_interactive_248)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `golden_turns.csv`
- turn count: `26`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: still_unable, paid_service_impact
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 16, 'user_requested_human': False}`
- representative user messages:
  - No i usd phone number
  - Ok will do ive still got 2 more problem ive over payed 2 times for urgent which i didn,t need so can please have refund
  - Yes going to post new add thank you very much jason
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008IwKHIA0 (case_id=cs_interactive_249)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-FP`
- turns file: `golden_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 3, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - Hi there
  - I need urgent help.please I paid for a 30-Day Booster Package, but my job advert expired immediately due to a system issue. I did not intend to expire the ad, and I have just paid for it today. Please restore my advert or reactivate it, as I should still have 30 days remaining.
  - All good thanks
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- classification override applied: **YES** -> primary=UC-FP, secondary=['UC-K']
- case-level override applied: **YES**
  - override source: Wave A2.1 reclassification map
  - reviewer: legacy migration from extractor.py UC_B_RECLASSIFICATION_OVERRIDES (Wave A2.1)
  - date: 2026-04-30
  - confidence: `high`
  - supporting turns: `[]`
  - rationale: Paid 30-day Booster expired -- UC-FP with UC-K fallback.
  - status: `approved`
  - migrated_from_legacy: `true`
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_message_moderation_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008iwZxIAI (case_id=cs_interactive_250)

- original primary_uc (HR): `UC-B`
- final primary_uc: `UC-K`
- turns file: `golden_turns.csv`
- turn count: `12`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Hi I just uploaded my advert. Edit it with the mother and the kittens please Restain the advert many thanks
  - Okay thank you
  - We’ve added the kittens mother to the picture can you please remove this issue because we are trying to sell the kittens
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=either, policy_should_escalate=True
- classification override applied: **YES** -> primary=UC-K, secondary=['UC-FP', 'UC-B']
- case-level override applied: **YES**
  - override source: Wave A2.1 reclassification map
  - reviewer: legacy migration from extractor.py UC_B_RECLASSIFICATION_OVERRIDES (Wave A2.1)
  - date: 2026-04-30
  - confidence: `high`
  - supporting turns: `[]`
  - rationale: Advert on hold 2nd time, please restore -- UC-K with UC-FP / UC-B fallback.
  - status: `approved`
  - migrated_from_legacy: `true`
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ScvNIAS (case_id=cs_interactive_251)

- original primary_uc (HR): `UC-FP`
- final primary_uc: `UC-FP`
- turns file: `golden_turns.csv`
- turn count: `16`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - Hi why is my add not live yet
  - Ok thanks
  - I did it there now
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q50000090KPlIAM (case_id=cs_interactive_252)

- original primary_uc (HR): `UC-FP`
- final primary_uc: `UC-FP`
- turns file: `golden_turns.csv`
- turn count: `14`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - Repost car ad
  - Okay, let me know when you have my acc
  - ??????
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008iwmrIAA (case_id=cs_interactive_253)

- original primary_uc (HR): `UC-FP`
- final primary_uc: `UC-FP`
- turns file: `golden_turns.csv`
- turn count: `9`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 1, 'user_requested_human': False}`
- representative user messages:
  - is here any other expert i can tal; with
  - okok
  - i complain organist those accounts before to you and you take no action
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: investigation_needed; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008a2ebIAA (case_id=cs_interactive_254)

- original primary_uc (HR): `UC-FP`
- final primary_uc: `UC-FP`
- turns file: `golden_turns.csv`
- turn count: `10`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): manager
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 2, 'handover_or_case': 1, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - Why was if account banned?
  - please help me unbanned if account after ill paid for ad posting..
  - please help me unbanned id my account reactive it?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): manager; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008rKnBIAU (case_id=cs_interactive_255)

- original primary_uc (HR): `UC-FP`
- final primary_uc: `UC-FP`
- turns file: `golden_turns.csv`
- turn count: `11`
- transcript outcome: `escalate`
- transcript reason: repeated unresolved/account-confusion signal(s) in visitor turns
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 15, 'user_requested_human': False}`
- representative user messages:
  - Hi I have paid twice now to put my add up and apparently it doesn’t meet the rules but I have followed them all can you help me to sort this
  - What account? I am very confused
  - Ok I have a separate email that I don’t use anymore maybe that has an old account how will I find out what other accounts I have linked? Or are they all linked to my email?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals; reason=repeated unresolved/account-confusion signal(s) in visitor turns; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008cma5IAA (case_id=cs_interactive_256)

- original primary_uc (HR): `UC-A`
- final primary_uc: `UC-A`
- turns file: `golden_turns.csv`
- turn count: `35`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: still_unable
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - My Ad ID: 1506986492, I can't find my ad when I click on "double room for rent Kilburn"
  - Thanks happy new year to you
  - Ok thanks.
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008pzXtIAI (case_id=cs_interactive_257)

- original primary_uc (HR): `UC-A`
- final primary_uc: `UC-A`
- turns file: `golden_turns.csv`
- turn count: `18`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: still_unable
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - I havent used gumtree for few years
  - I dont know cant access gum tree to answer that question
  - waiting for new link to arrive from my email address
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q500000920WbIAI (case_id=cs_interactive_258)

- original primary_uc (HR): `UC-A`
- final primary_uc: `UC-A`
- turns file: `golden_turns.csv`
- turn count: `33`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: paid_service_impact
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - Can’t find my advertisement
  - I am following a link from confirmation of payment !!
  - Trying that now one aec
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008OqaLIAS (case_id=cs_interactive_259)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `golden_turns.csv`
- turn count: `9`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: paid_service_impact
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - How do I receive the payment when I sell an item
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008zXorIAE (case_id=cs_interactive_260)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `golden_turns.csv`
- turn count: `19`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - i cannot see my ad, not even inactive
  - i was not provided an ad id but it was all saved, i went through the payment process
  - Freelance Beauty Therapist / Nail Technician
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008J5sDIAS (case_id=cs_interactive_261)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `golden_turns.csv`
- turn count: `15`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: paid_service_impact
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - Hi . i try to post the ad on but your website does not taking the payment please help
  - Payment process started. Please do not go back or close the browser until payment has been made. Total payment: £17.92 Paying with PayPal [EMAIL] PayPal Choose another way to pay
  - hi , thank you its okey now cheers
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kJr3IAE (case_id=cs_interactive_262)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `golden_turns.csv`
- turn count: `19`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: explicit_unresolved
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - I want my add active
  - Two days go I post my add. But not working
  - But gumtree not send me any email and ID
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008JGajIAG (case_id=cs_interactive_263)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `golden_turns.csv`
- turn count: `10`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: paid_service_impact
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - Hello why have my post for a free French bull dog been removed
  - You have took my payment and instantly took it down ????
  - Dave [EMAIL]
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kwlxIAA (case_id=cs_interactive_264)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `10`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 3, 'user_requested_human': False}`
- representative user messages:
  - Drum kit in Glasgow £20 won't reply to messages sent does this thing work????
  - It's been two days
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008TsBhIAK (case_id=cs_interactive_265)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `13`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 2, 'user_requested_human': False}`
- representative user messages:
  - Yesterday
  - It is friday
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008DC8QIAW (case_id=cs_interactive_266)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `11`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - Hello,
  - Ok thank you, was just making sure
  - 1506744687
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008Wf8HIAS (case_id=cs_interactive_267)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `20`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - Hi item ad number 1505576985
  - Yes it's happened to me in the past where I'm waiting for an email notification about messages but sometimes goes in spam folder and it gets missed
  - OK how do I contact the person as I've messaged a few times since last Friday. It's obviously gone in their spam folder too 🙈
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: async_update; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008aTLRIA2 (case_id=cs_interactive_268)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `24`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - No problem
  - You too. Thanks again
  - Very welcome nuraan
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008bpoXIAQ (case_id=cs_interactive_269)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `golden_turns.csv`
- turn count: `12`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 2, 'user_requested_human': False}`
- representative user messages:
  - Hi i bought a car from gumtree a year ago would you still have the information toyota verso YR61ZLY
  - Ok thanks
  - I believe the person who sold it didn't declare its category n
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008oMIVIA2 (case_id=cs_interactive_270)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `34`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Hey my names tony slinger my email is [EMAIL]
  - Thanks all.helps ill wait reply thanks sorry bother you bye
  - Ok so im All ok no restrictioms At all just wait reply
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008hJArIAM (case_id=cs_interactive_271)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `13`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: still_unable
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 1, 'user_requested_human': False}`
- representative user messages:
  - I've been trying to contact a person called Peter about a white male kitten but I can't get any response at all
  - Glad you can see i even messaged off my hubby phone to but just cant seem to get a reply and I am very interested in giving the kitten a very good life my son has fallen in love with the picture and he lives animals I just didn't know if there was any other way of contacting him
  - Oh I don't know ive messaged a few times, it said online yesterday but he never replied
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008VLjVIAW (case_id=cs_interactive_272)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `25`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - Will do give me a minute
  - Thank you for checking
  - No the error message came later to my email in the spam folder
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: investigation_needed; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008l1SHIAY (case_id=cs_interactive_273)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `17`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - Saying the email address is not recognised
  - A couch I have bought off gumtree too
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q50000091n4jIAA (case_id=cs_interactive_274)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `golden_turns.csv`
- turn count: `16`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: still_unable
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - 2 days I uploaded an ad for a P.A.Equipment I cannot find it now?
  - Ok. Thank you
  - I expect the email would have been [EMAIL]
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008yFijIAE (case_id=cs_interactive_275)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `10`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 13, 'user_requested_human': False}`
- representative user messages:
  - What do you mean is no longer valid? When I am editing my ad I can select from the 3 email addresses on my account stored, but I want them 2 of them removed and 1 new one added, surely that's possible
  - It's not really notifications I wanted, it was a way for customers to contact me on my ad instead of using my mobile number
  - So there is no way at all to edit one of the existing ones, not even from your side?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: async_update; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008lXK5IAM (case_id=cs_interactive_276)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `13`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - My app shows notifications, but when I log on there is no message there
  - OK thanks
  - I've seen your test message
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008l4DFIAY (case_id=cs_interactive_277)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `10`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - yes I need to change my email address
  - that is crazy ! shit website !
  - I want to change my email address
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ZR0jIAG (case_id=cs_interactive_278)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `60`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: still_unable
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 44, 'user_requested_human': False}`
- representative user messages:
  - Yes, I am logged in online and have opened the message section but there is no message there.
  - So I am now logged on but I can't access the message.
  - OK. Thank you very much.
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008v73NIAQ (case_id=cs_interactive_279)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `19`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - Unable to complete my Ad also change my bank details
  - My bank account is no longer with Trustee Savings Bank
  - No error messages
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ehhZIAQ (case_id=cs_interactive_280)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `27`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - I was told to get in touch as l have been having problems with your app it has a broken link and it is affecting me why is this please
  - due to this others too have had this happen it’s all on trust piolet
  - deleted my gumtree account
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008uxOzIAI (case_id=cs_interactive_281)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `13`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 2, 'user_requested_human': False}`
- representative user messages:
  - Sending messages to both advertisers, getting no response…how can I confirm that my messages are getting through?
  - great input, appreciated and thanks again. Cheerio.
  - ok thanks for that, we can do no more, c`est la vie.
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008mIMDIA2 (case_id=cs_interactive_282)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `13`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - [EMAIL] was my original and very old email
  - Okay, seems fairly unhelp when I have 47 messages I needed to keep but okay. Thanks.
  - and one i can rarely access anymore
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008i4hfIAA (case_id=cs_interactive_283)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `25`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: still_unable
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - veiwers cant message me on my advertisement
  - i might try that thanks
  - ive done the bump up, but strange no messages after ive had 173 veiws
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008zS61IAE (case_id=cs_interactive_284)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `27`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - Just trying to contact regarding the car been there for 50+ days the number I tried to call and it says its not recognised is there an email address with the account I can send email to?
  - The advertiser has entered the wrong phone number so you either help them or should remove the ad for them
  - Never mind you obviously can't help
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008zM0bIAE (case_id=cs_interactive_285)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `10`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - They should be going to [EMAIL] Currently the are going to my sign in email [EMAIL]
  - Probably best to change my login email then. Agree?
  - Ok will do thanks
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008jYc2IAE (case_id=cs_interactive_286)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `16`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - I received that thank you,
  - I’m good, thankyou.
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ZQO1IAO (case_id=cs_interactive_287)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `14`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - Just now few mins ago
  - Nope ok it didn’t send it was for chairs
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008qkyDIAQ (case_id=cs_interactive_288)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `11`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 3, 'user_requested_human': False}`
- representative user messages:
  - thank u take ur time
  - nor i get it replies [EMAIL]
  - people don't get my massage
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: investigation_needed; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008lhPtIAI (case_id=cs_interactive_289)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `17`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Hello thanks
  - Please view this ad: Double Mezzanine Semi-Studio - Holland Road, Holland Park/Shepherds Bush [POSTCODE] - All Bills Inc Price:£240pw https://www.gumtree.com/p/flats-houses/double-mezzanine-semi-studio-holland-road-holland-parkshepherds-bush-w14-8bb-all-bills-inc/1475855411?utm_source=unknown&utm_campaign=socialbuttons&utm_content=app_ios&utm_medium=Social
  - I am looking for a flat in London so maybe the issue is in the housing section
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008OpO9IAK (case_id=cs_interactive_290)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `21`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: paid_service_impact, explicit_unresolved, still_unable
- evidence signal counts: `{'unresolved_user': 4, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - Hi, good morning, my ad is not showing on featured page nor under the main search words
  - I will edit it to crystal glass antique because I paid a renewal fee only yesterday so its still got a few days left, but still there is issues of not receiving messages - is there a reason for this?
  - Appreciated
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008U2kXIAS (case_id=cs_interactive_291)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `24`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - It show I’ve got 3 messages then when I go to messages it says “something went wrong “ error code 500
  - Can’t see other messages I have replied to yours
  - Yes it says hi in the emails
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008RpD7IAK (case_id=cs_interactive_292)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `16`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Hi just received a message on my account but when I try and go on it’s saying error 500
  - normally be on home screen and nothing on there
  - That would
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008vJcPIAU (case_id=cs_interactive_293)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `19`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Keep getting on hotmail I have messages on gumtree tree but there’s nothing there
  - No iv only one account
  - Mobile app
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008IVtZIAW (case_id=cs_interactive_294)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `golden_turns.csv`
- turn count: `33`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: paid_service_impact
- evidence signal counts: `{'unresolved_user': 3, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - I have not got my money
  - It was delivered on 1st of December
  - 28th of November
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008frwbIAA (case_id=cs_interactive_295)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `27`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: internal_team, async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 13, 'user_requested_human': False}`
- representative user messages:
  - There is an ad for a room in portswood and the messages are on my name but it’s not my ad
  - I’ll do now bye
  - Ok thank you
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: internal_team, async_update; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008Rq2jIAC (case_id=cs_interactive_296)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `30`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - Hi. I’ve had a couple if emails from you saying I have unanswered messages - I have checked and I’ve only got one conversation and no new messages.
  - Well I definitely won’t be paying for your services again - I’m so angry that I have missed a sale!
  - Well you need to sort your system out then! That’s ridiculous.
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008puWbIAI (case_id=cs_interactive_297)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `15`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - Oh I don’t know what it is it’s for the Dogue de Bordeaux that’s £350 in market deeping in Cambridgeshire
  - Oh
  - Okay so what can I do now as very interested
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008G4npIAC (case_id=cs_interactive_298)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `31`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): handover
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 1, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - Not happy with customer care an agent is blocking my password who l tried to get help with this afternoon
  - a serious complaint about what happened to me with that THATO
  - l need to go l am very upset please raise
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=handover_or_case_signals; reason=agent transcript includes handover/case signal(s): handover; trigger=user_requested
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008xbIHIAY (case_id=cs_interactive_299)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `17`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - Fingers crossed and thanks
  - Let’s give it a go
  - Yes…..based in Glasgow Scotland and just returned from Southern Ireland
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008XSs9IAG (case_id=cs_interactive_300)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `16`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - i have got to find out that there are a fwe accounts related to me
  - Iput it in my name David denton
  - yes & i do not recall putting that name on my account. i have placed an add for a bike & i can not find it
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008p1DZIAY (case_id=cs_interactive_301)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `25`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: still_unable
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 17, 'user_requested_human': False}`
- representative user messages:
  - I cannot reply to emails received on my phone.
  - I receive an email with a new message. I can read the message but cannot reply to it as I am told there are no messages in my Inbox. I have to log on with laptop to reply which is not convenient.
  - ok I'll try
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008nFAzIAM (case_id=cs_interactive_302)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `20`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - Hello Jason
  - I am divorcing, and my phone is at the police station. I saved my password on the phone, but can't access it, now.
  - I don't know my password
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008raNJIAY (case_id=cs_interactive_303)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `15`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: time_window
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - i need to reset my paword please
  - thankyou
  - are you there ?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: time_window; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008xOT7IAM (case_id=cs_interactive_304)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `28`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 14, 'user_requested_human': False}`
- representative user messages:
  - Is it possible to call me on [PHONE]?
  - I cant login in to my account
  - You should be able to find all accounts linked to my self
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008aXAXIA2 (case_id=cs_interactive_305)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `14`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 1, 'user_requested_human': False}`
- representative user messages:
  - All three that I tried complied with the required criteria.
  - No why should I change my modus, I'd rather not belong to Gumtree.
  - At every where a password is required my passwords are accepted apart from Gumtree.
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008q90zIAA (case_id=cs_interactive_306)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `27`
- transcript outcome: `escalate`
- transcript reason: repeated unresolved/account-confusion signal(s) in visitor turns
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 28, 'user_requested_human': False}`
- representative user messages:
  - It wont let me use my saved password or email
  - Yes but my contact email is google
  - ok i'll try that. thanks
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals; reason=repeated unresolved/account-confusion signal(s) in visitor turns; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008hysLIAQ (case_id=cs_interactive_307)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `18`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: still_unable
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - Hi I can’t to update my adv
  - Wrong password but I did changed still not happen
  - Website
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q500000908rdIAA (case_id=cs_interactive_308)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `19`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 13, 'user_requested_human': False}`
- representative user messages:
  - Yes, I just signed in again. I still can’t see my ads. My account shows my name.
  - No thank you, thanks for your help bye
  - Ok, that worked, thank you.
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008eYBFIA2 (case_id=cs_interactive_309)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `37`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: paid_service_impact
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 23, 'user_requested_human': False}`
- representative user messages:
  - Is there an account linked to my email [EMAIL]?
  - Is any money owed on the account?
  - How can I do that when I don’t receive the link????
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008pNVpIAM (case_id=cs_interactive_310)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `13`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Please send me a password reset to [EMAIL]. It won’t let me in and I’m not receiving the reset email
  - No email received again
  - Oh my god. What a faff
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008la8HIAQ (case_id=cs_interactive_311)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `13`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - Having issue receiving email password reset or logging in
  - https://www.gumtree.com/p/business-for-sale/window-cleaning-rounds-for-sale/1506802397
  - Here’s one of my adverts
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ftWzIAI (case_id=cs_interactive_312)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `badcase_turns.csv`
- turn count: `28`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 3, 'human_investigation': 3, 'handover_or_case': 1, 'identifier_context': 27, 'user_requested_human': False}`
- representative user messages:
  - Hi, i need to change the email address on my account but both email addresses are locked and cant be changed, one of them i dont even recognise. it id not the login email address but the contact address
  - [EMAIL] is fine. Many thanks
  - what email address will i get the confirmation of deletion?
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=gdpr_intake
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008NZ6vIAG (case_id=cs_interactive_313)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `badcase_turns.csv`
- turn count: `15`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 2, 'identifier_context': 1, 'user_requested_human': False}`
- representative user messages:
  - just signed up and trying to post my add and all filed in and then doesnt let me post and pay
  - ok thansk
  - doesnrt work still ???
- outcome decision: partial_uc_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=user_distress
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008YoU1IAK (case_id=cs_interactive_314)

- original primary_uc (HR): `UC-K`
- final primary_uc: `UC-K`
- turns file: `badcase_turns.csv`
- turn count: `40`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: explicit_unresolved
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - Of I enter 'Blackheath' in the location for example it doesn't show anything other than surrounding areas. I know there is always item for sale in Blackheath. Same for 'Welling', 'Bexlyheath', etc.
  - Yits not working for me other than from your links.
  - I think it might be the radius but it's not something I've changed. Thanks for your help.
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=either, policy_should_escalate=True
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'answer_grounded', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['resolve_article', 'search_knowledge'] vs policy=['get_message_moderation_context', 'get_moderation_review_context', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008STIbIAO (case_id=cs_interactive_315)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `badcase_turns.csv`
- turn count: `22`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 2, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - Hi Candice - great to reconnect!
  - if we disconnect, pls do email it across if you can - many thanks again!
  - got it thanks - where can those charges be found pls?
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q50000091ATCIA2 (case_id=cs_interactive_316)

- original primary_uc (HR): `OUT_OF_SCOPE_PRO_CONTRACT`
- final primary_uc: `UC-A`
- turns file: `badcase_turns.csv`
- turn count: `25`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation, manager
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 9, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - i have been trying to call gumtree for over a year now
  - no thank you very much for your time
  - it is of the utmost importance
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008yDgvIAE (case_id=cs_interactive_317)

- original primary_uc (HR): `OUT_OF_SCOPE_RATINGS_REVIEWS`
- final primary_uc: `UC-A`
- turns file: `badcase_turns.csv`
- turn count: `15`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: investigation_needed
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 3, 'user_requested_human': False}`
- representative user messages:
  - Hi Candice
  - As you are unable to help. Yes thank you
  - I can understand if this was many months ago but all the information is on my account in my messages
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008zcDRIAY (case_id=cs_interactive_318)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `badcase_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 3, 'handover_or_case': 1, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - I bought a TV from a guy. His discretion was incorrect. He stated it was a 65" , it was 55". He stated the brightness needed turned up. Wrong, the back light is broken. Basically he sold me a non working product. What are my rights or what can I do. I contacted him and he said, no refunds.
  - Cheers Jason. Have a good day
  - Appreciate that
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ebCLIAY (case_id=cs_interactive_319)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 1, 'identifier_context': 1, 'user_requested_human': False}`
- representative user messages:
  - 1508059319 1508056756 1508039994 1508039626
  - ok I will drop gumtree, will move over to ebay I guess. I wasted my time posting Ads not great staff and great systems Gumtree has!!!!!!
  - ok blessing tochodumum
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008p5qfIAA (case_id=cs_interactive_320)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `drift_control_turns.csv`
- turn count: `24`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): handover
- evidence signal counts: `{'unresolved_user': 4, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 2, 'user_requested_human': False}`
- representative user messages:
  - One of my item was sold on 15th. Buyer has paid for it but this is my first selling experience with gumtree.
  - Okay so you would get paid once the item has been delivered to the buyer. The buyer would confirm everything is okay and then that would be released to your method of payment
  - How would I get paid after I sell the item
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): handover; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008O2gnIAC (case_id=cs_interactive_321)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `drift_control_turns.csv`
- turn count: `13`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 7, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - My add is not showing and I have paid twice buy mistake. £19 AND £21 pounds
  - Yes i have paid twice because I thought that's why it was not showing my add could I have a refund please
  - Thanks have a lovely day take care 🙂
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008EinlIAC (case_id=cs_interactive_322)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `handover_turns.csv`
- turn count: `15`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 1, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - Hi, I haven received my money yet. The item was delivered on 26 Nov
  - Ok, thank you
  - Will they join the chat, via email, phone call?
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com', 'ad ID is REDACTED_AD_ID']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008J9uDIAS (case_id=cs_interactive_323)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `handover_turns.csv`
- turn count: `50`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 26, 'user_requested_human': False}`
- representative user messages:
  - Hi Candice. I need your help.
  - Thanks ☺️
  - Have a great rest of your day
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008hL6EIAU (case_id=cs_interactive_324)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `escalation_turns.csv`
- turn count: `24`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Can you help
  - Okay got it. Have a good day
  - Although I have to start fresh and lose my history
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008yucDIAQ (case_id=cs_interactive_325)

- original primary_uc (HR): `UC-G`
- final primary_uc: `UC-G`
- turns file: `escalation_turns.csv`
- turn count: `18`
- transcript outcome: `escalate`
- transcript reason: visitor explicitly requested a human or manager
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 6, 'handover_or_case': 1, 'identifier_context': 14, 'user_requested_human': True}`
- representative user messages:
  - Hi, I’m looking to ensure my full account is deleted . I’m having issues with the process and need it actioned asap please .
  - No thanks, have a nice evening .
  - Do you have a reference number ? I would like to ensure this happens and the correct information is captured.
- outcome decision: mandatory_policy_escalation: UC-G must hand over; trigger=user_requested
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008n3D3IAI (case_id=cs_interactive_326)

- original primary_uc (HR): `UC-E`
- final primary_uc: `UC-E`
- turns file: `golden_turns.csv`
- turn count: `11`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - Erm property Sussex road southport merseyside Terry says still available how do I go about viewing through younir him on k in a min
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: async_update; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008jyBWIAY (case_id=cs_interactive_327)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `golden_turns.csv`
- turn count: `10`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - I sent a link to my son following posting to gumtree and add states sold.
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008kmRdIAI (case_id=cs_interactive_328)

- original primary_uc (HR): `OUT_OF_SCOPE_RATINGS_REVIEWS`
- final primary_uc: `UC-A`
- turns file: `golden_turns.csv`
- turn count: `21`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 2, 'user_requested_human': False}`
- representative user messages:
  - I left feedback for a buyer, item collected this morning. Am I able to change or delete it ?
  - You too 🌺
  - Thank you again ;)
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000009228bIAA (case_id=cs_interactive_329)

- original primary_uc (HR): `UC-FP`
- final primary_uc: `UC-FP`
- turns file: `golden_turns.csv`
- turn count: `13`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Hello, all of my post are being removed saying I am breaking posting policies and that I will receive an email explaining, but I never get the email?
  - I’ve reposted and it seems to be on there ty
  - Hello?
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008tXBdIAM (case_id=cs_interactive_330)

- original primary_uc (HR): `UC-I`
- final primary_uc: `UC-I`
- turns file: `golden_turns.csv`
- turn count: `13`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: frustration, paid_service_impact
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Hi dear whi my post was cancelled ?
  - Ok I didn't know that, I'm sorry, can I have the money back that I paid for the ad?
  - I'm so disappointed I pay 14 pounds for the ad and then you delete my ad
- outcome decision: mandatory_policy_escalation: UC-I must hand over; trigger=payment_dispute_detected
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008qcEHIAY (case_id=cs_interactive_331)

- original primary_uc (HR): `UC-A`
- final primary_uc: `UC-A`
- turns file: `golden_turns.csv`
- turn count: `13`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Furnished room to rent in bs14
  - Ad ID: 1508751621
  - Oke
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008afT3IAI (case_id=cs_interactive_332)

- original primary_uc (HR): `UC-A`
- final primary_uc: `UC-A`
- turns file: `golden_turns.csv`
- turn count: `15`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 12, 'user_requested_human': False}`
- representative user messages:
  - Where do I find that if I can't find my ad?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008u8KrIAI (case_id=cs_interactive_333)

- original primary_uc (HR): `UC-A`
- final primary_uc: `UC-A`
- turns file: `golden_turns.csv`
- turn count: `12`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - My ads - comes up with you have no ads
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008mJ6zIAE (case_id=cs_interactive_334)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `12`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 11, 'user_requested_human': False}`
- representative user messages:
  - Hi I've posted an advert on Gumtree someone has replied to me about my ad and is interested but wants to do a payment plan
  - I think the app
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008UD87IAG (case_id=cs_interactive_335)

- original primary_uc (HR): `OUT_OF_SCOPE_DELIVERY`
- final primary_uc: `UC-A`
- turns file: `golden_turns.csv`
- turn count: `14`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - Hi, I just realised a part of the item I sold and collected by buyer miss a piece that I'm in possession of. Cannot find a way to contact buyer as chat was already deleted before I realised. I would like to contact the buyer that possibly can collect the missing item
  - No my personal mobile number no
  - 1507661693 maybe
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008iIO1IAM (case_id=cs_interactive_336)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `28`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: frustration
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - I updated my adds to let potential buyers know item is still available. However, when I updated the ads a couple of days ago. It no longer displays my number to buyers.
  - While I know this is not your fault and is in no way a criticism against you. Gumtree users are going to be annoyed be annoyed by this. And I feel that page should have been updated to reflect the change you expressed
  - I use gumtree for local people. It is not an item that would be posted so affords local collection
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ntpxIAA (case_id=cs_interactive_337)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `14`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - Are u human
  - Internet
  - It just still at a green box with COMPLETE or similar on
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008gd0LIAQ (case_id=cs_interactive_338)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `11`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Mail box, app, web
  - Where is my existing massages?
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008sECfIAM (case_id=cs_interactive_339)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `10`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 4, 'user_requested_human': False}`
- representative user messages:
  - I tried to explain this issue to your colleague but they cut me out. I replied to some ads in Gumtree and my reply has been posted on Bark as a public ad with my phone.
  - Can I send you the screenshot?
  - There is not an ID
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: async_update; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008QWinIAG (case_id=cs_interactive_340)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `15`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - How do I do this
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008gaC9IAI (case_id=cs_interactive_341)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): handover
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 1, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - i have using gumtree over 10 years now
  - when i try to post used to give me two option email and phone number i prefer contact with phone number but there is no phone number section no more only email
  - even now i checked there is a post whic is poster 5 hours ago and still see the phone option
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): handover; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ylx7IAA (case_id=cs_interactive_342)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Do I need an account to contact the seller
  - Done then. thank oyu
  - Does it cost to have an accoutn
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: async_update; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008qew1IAA (case_id=cs_interactive_343)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `11`
- transcript outcome: `resolve`
- transcript reason: agent transcript contains resolution/answer confirmation and no unresolved user signal
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Applicant is Conor Thompson his email is [EMAIL] I have emailed him but no responce. the mobile number sent has only 10 digits instead of 11 are you able to get me the correct phone no
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008U9dxIAC (case_id=cs_interactive_344)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `11`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 3, 'user_requested_human': False}`
- representative user messages:
  - I can’t access my messages. I’m getting error code 500. Can you help.
  - Ok I will thanks
  - Yes I’m just trying to clear my cache
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fzvlIAA (case_id=cs_interactive_345)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `14`
- transcript outcome: `escalate`
- transcript reason: visitor explicitly requested a human or manager
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': True}`
- representative user messages:
  - Hi I tried to download the app for buy &sale items but I couldn’t do that when I go to App Store to download it does not work it says open when I click that it goes to items actually I want to download the app and sale item please help me on that
  - Thank you not to worry I am not good at on line working
  - Yes I am actually is easier for me to speak to someone rather then writing could you please do it that way?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=user_requested_human; reason=visitor explicitly requested a human or manager; trigger=user_requested
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008jSGTIA2 (case_id=cs_interactive_346)

- original primary_uc (HR): `UC-C`
- final primary_uc: `UC-C`
- turns file: `golden_turns.csv`
- turn count: `14`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - Hello Carmen. Hope you’re well. I’m in touch because I’m constantly receiving spam notifications even though they’re turned off in app. I don’t want to have to turn them off in my phone settings because I’d still like to receive message notifications when buying/selling. Please help.
  - Okay thanks. Take care.
  - Settings for all notifications except messages are already turned off yet I’m still receiving spam notifications from you.
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_moderation_review_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008k8h7IAA (case_id=cs_interactive_347)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `13`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: time_window
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - Last time in the end of 2024
  - Ok thank you for your advice
  - You are able to send me email with resetting link ?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: time_window; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008gkeXIAQ (case_id=cs_interactive_348)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: repeated unresolved/account-confusion signal(s) in visitor turns
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - I can log into my account
  - Not sure
  - It's says that ive been logged out and to contact customer service
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals; reason=repeated unresolved/account-confusion signal(s) in visitor turns; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008uR8zIAE (case_id=cs_interactive_349)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `11`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - Your password reset system is not recognising the link from your reset email
  - I'm using the app on a mobile
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008le0bIAA (case_id=cs_interactive_350)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `10`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: time_window
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 7, 'user_requested_human': False}`
- representative user messages:
  - [EMAIL] been trying to enter the acoount ithink ientered it wrong to many time and block me
  - Ok thankyou
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=human_investigation_signals; reason=agent required investigation or async follow-up: time_window; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q50000090Qa1IAE (case_id=cs_interactive_351)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `10`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - i can not sign in ,i have tryed password reset but still NO,,,,email address is,,,[EMAIL]
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008nzc2IAA (case_id=cs_interactive_352)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `17`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 9, 'user_requested_human': False}`
- representative user messages:
  - Are you there?
  - Yes, and answered it by clicking through
  - This is the first time I have tired to use it to message someone
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008xcpRIAQ (case_id=cs_interactive_353)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `golden_turns.csv`
- turn count: `12`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 3, 'user_requested_human': False}`
- representative user messages:
  - Hello I did but then it wouldn’t allow me too.
  - Sorry I can’t remember
  - Yes I did
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008g2SDIAY (case_id=cs_interactive_354)

- original primary_uc (HR): `UC-H`
- final primary_uc: `UC-H`
- turns file: `badcase_turns.csv`
- turn count: `16`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 1, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - Person called Tom left the negative review for no reason.
  - Ok please resolve it asap. I will be grateful
  - He's already been in touch with you
- outcome decision: mandatory_policy_escalation: UC-H must hand over; trigger=appeal_requires_human
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'ask_user', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008w4g9IAA (case_id=cs_interactive_355)

- original primary_uc (HR): `OUT_OF_SCOPE_RATINGS_REVIEWS`
- final primary_uc: `UC-A`
- turns file: `badcase_turns.csv`
- turn count: `14`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - Please review my chat with Cortney on 24th January
  - Thanks - I’ll use the email address
  - I understand the 30 day policy but it can be overridden
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008mJ0XIAU (case_id=cs_interactive_356)

- original primary_uc (HR): `UC-FP`
- final primary_uc: `UC-FP`
- turns file: `golden_turns.csv`
- turn count: `24`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 13, 'user_requested_human': False}`
- representative user messages:
  - Why is my ADD not posted under f
  - Il will look 4 it and get nack
  - Ju as t put my ADD under faschunds ,4 sale in South Australia like it should be
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008LBBxIAO (case_id=cs_interactive_357)

- original primary_uc (HR): `OUT_OF_SCOPE_RATINGS_REVIEWS`
- final primary_uc: `UC-A`
- turns file: `golden_turns.csv`
- turn count: `21`
- transcript outcome: `escalate`
- transcript reason: agent required investigation or async follow-up: async_update
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 1, 'user_requested_human': False}`
- representative user messages:
  - Hi sir
  - No problem
  - Ok thank you
- outcome decision: mandatory_policy_escalation: UC-A must hand over; trigger=out_of_scope
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled', 'get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008rhv5IAA (case_id=cs_interactive_358)

- original primary_uc (HR): `UC-A`
- final primary_uc: `UC-A`
- turns file: `golden_turns.csv`
- turn count: `22`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 10, 'user_requested_human': False}`
- representative user messages:
  - Hello I have a car advert on here. Today I had a message that said someone was interested. It asked me to click on a link and log into something which I didn't. The message has now disappeared. Was it spam ???
  - I think so. I wasn't at home so I didn't have much time to look or think. I opened the link which then asked me to log in. That's when I got suspicious and came out of it. The message or notification has now gone I can't find it. Do you know what it was
  - And you thanks 👍
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008vsK1IAI (case_id=cs_interactive_359)

- original primary_uc (HR): `UC-A`
- final primary_uc: `UC-A`
- turns file: `golden_turns.csv`
- turn count: `19`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: still_unable
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 15, 'user_requested_human': False}`
- representative user messages:
  - I cannot access my sd?
  - I've manage to repost it ...thanks for no help really.
  - My details tell me nothing
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008WDITIA4 (case_id=cs_interactive_360)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `golden_turns.csv`
- turn count: `31`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: paid_service_impact
- evidence signal counts: `{'unresolved_user': 3, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 8, 'user_requested_human': False}`
- representative user messages:
  - I was charged £3.75 and £2.25 twice yesterday for my 2 framed horse prints that I put on Gumtree for sale and 2 more payments from meta platform for £1.50 & £2.00
  - Yes that's it thank you.
  - 3 live ads in thought I have only 2 the 2 horse racing framed prints.
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
  - outcome_class: HR='escalate' vs final='resolve'
  - should_escalate: HR=True vs final=False
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008ZmLKIA0 (case_id=cs_interactive_361)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `golden_turns.csv`
- turn count: `19`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - Is gumtree not free to post for sale anymore?
  - Ive not added features and still not giving options to post
  - Ill try thanks never had issue before
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008pGm5IAE (case_id=cs_interactive_362)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `golden_turns.csv`
- turn count: `36`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: paid_service_impact
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 16, 'user_requested_human': False}`
- representative user messages:
  - I looked at add to add to it was still up then went back it was gone I must can mistake
  - Yes I just looked seen it I can show u payment in my bank
  - could u pls put job id Contract
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008WufBIAS (case_id=cs_interactive_363)

- original primary_uc (HR): `UC-F`
- final primary_uc: `UC-F`
- turns file: `golden_turns.csv`
- turn count: `9`
- transcript outcome: `unclear`
- transcript reason: unresolved user signal(s) present without clear handover evidence: paid_service_impact
- evidence signal counts: `{'unresolved_user': 1, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 1, 'user_requested_human': False}`
- representative user messages:
  - Yes, 1502436179 and 1502436282 I would like to re advertise these, however you want to charge me £4.99 for each item.
  - Thank you Candice for your time, may I wish you a Very Happy Christmas
  - Can you please remove the £4.99 for each item please?
- outcome decision: policy_default: transcript evidence unclear or insufficient; policy_outcome=resolve, policy_should_escalate=False
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008pyjtIAA (case_id=cs_interactive_364)

- original primary_uc (HR): `UC-A`
- final primary_uc: `UC-A`
- turns file: `badcase_turns.csv`
- turn count: `43`
- transcript outcome: `escalate`
- transcript reason: agent transcript includes handover/case signal(s): escalation
- evidence signal counts: `{'unresolved_user': 6, 'human_investigation': 4, 'handover_or_case': 1, 'identifier_context': 16, 'user_requested_human': False}`
- representative user messages:
  - Hi there, I paid to bump up my ad this morning and since then only one other person's ad has been bumped up so my ad should be second in the listing not way down
  - As only one other ad has been refreshed/bumped up today i reasonably expect it to now be second on the listing - otherwise what have I paid for? It's now no higher than before it was bumped up but, to reiterate, only one persons ad has been updated since
  - I expect to get whats been paid for, simple as that.
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals,handover_or_case_signals; reason=agent transcript includes handover/case signal(s): escalation; trigger=clarification_budget_exhausted
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008iF1xIAE (case_id=cs_interactive_365)

- original primary_uc (HR): `UC-J`
- final primary_uc: `UC-J`
- turns file: `badcase_turns.csv`
- turn count: `21`
- transcript outcome: `unclear`
- transcript reason: no strong transcript outcome signal detected
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 0, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': False}`
- representative user messages:
  - Will no longer use your platform I'll move to Facebook tiktok where they allow you a telephone number this is a disaster for buying and selling
  - I've had enough of this rubbish I'll go to better platforms where customer service is customer service
  - Users don't often get emails when they do theres no reply people are struggling to sell things the last 3 days
- outcome decision: mandatory_policy_escalation: UC-J must hand over; trigger=trust_safety_required
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['fixed_script_library', 'create_case_controlled', 'request_handover', 'record_outcome'] vs final=['create_case_controlled', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['get_customer_context', 'resolve_article', 'search_knowledge'] vs policy=['get_customer_context', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_customer_account', 'lookup_listing_or_ad', 'resolve_article', 'search_knowledge']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008fpLJIAY (case_id=cs_interactive_366)

- original primary_uc (HR): `UC-A`
- final primary_uc: `UC-A`
- turns file: `badcase_turns.csv`
- turn count: `25`
- transcript outcome: `escalate`
- transcript reason: visitor explicitly requested a human or manager
- evidence signal counts: `{'unresolved_user': 0, 'human_investigation': 2, 'handover_or_case': 0, 'identifier_context': 6, 'user_requested_human': True}`
- representative user messages:
  - Well here's some feedback: getting quite a lot of views and no responses so I would say not a successful trial. There appears to have been no warning or announcement of that by the way?
  - Utterly, utterly stupid.
  - What? How on earth are you supposed to arrange collection/pick up of items safely?
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=user_requested_human,human_investigation_signals; reason=visitor explicitly requested a human or manager; trigger=user_requested
- policy-vs-HR mismatches:
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'lookup_customer_account']
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`

## 570Q5000008mcZRIAY (case_id=cs_interactive_367)

- original primary_uc (HR): `UC-D`
- final primary_uc: `UC-D`
- turns file: `badcase_turns.csv`
- turn count: `31`
- transcript outcome: `escalate`
- transcript reason: unresolved user signal(s) combined with agent investigation/follow-up language
- evidence signal counts: `{'unresolved_user': 2, 'human_investigation': 1, 'handover_or_case': 0, 'identifier_context': 5, 'user_requested_human': False}`
- representative user messages:
  - Hi I don’t seem to get any phone calls as I think my phone number is not showing
  - I have paid so much money for the add it’s not worth it if I am not getting any business
  - It takes longer to respond and reply here texts , easier to connect directly
- outcome decision: faq_transcript_escalation: transcript_indicated_outcome=escalate; flags=unresolved_user_signals,human_investigation_signals; reason=unresolved user signal(s) combined with agent investigation/follow-up language; trigger=faq_miss_threshold_exceeded
- policy-vs-HR mismatches:
  - expected_tool_sequence: HR=['get_customer_context', 'search_knowledge', 'resolve_article', 'answer_grounded', 'record_outcome'] vs final=['get_customer_context', 'search_knowledge', 'resolve_article', 'request_handover', 'record_outcome']
  - forbidden_tools: HR(non-human-only)=['create_case_controlled'] vs policy=['create_case_controlled', 'get_message_moderation_context', 'get_moderation_review_context', 'lookup_listing_or_ad']
  - outcome_class: HR='resolve' vs final='escalate'
  - should_escalate: HR=False vs final=True
- dropped hidden_facts (already in form_context): ['email is customer@example.com']
- llm_cache_hit: `False`
- llm_offline_fallback: `True`
- llm_acceptance_reason: `n/a`
- llm_confidence: `n/a`
