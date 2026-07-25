---
title: Sprint 106 handoff — CaseSpec expectation rewrite for promotion/ + case_families/
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: eval_interactive/case_specs/{promotion,case_families}/**
last_reviewed: 2026-07-26
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sub-sprint 106 of the 2026-07 performance re-plan, WS-2 continuation.
  Adjudicates the 64 promotion/ and 17 case_families/ R1 violations left by
  WS-2 (74de32c3), plus the 3 case_families R2 errors. Data-only: no runtime
  code, no Python package, no test file touched. Branch
  sprint-106-casespec-promotion, worktree ../csagent-wt-106.
---

# Sprint 106 handoff

**Branch** `sprint-106-casespec-promotion` — 6 commits, 82 files, all under
`eval_interactive/case_specs/`.

## 0. Headline

81 R1-violating specs were adjudicated individually against the customer's
actual ask under the 2026-07-25 product principle. **The corpus stopped
teaching escalation on 26 specs (14 flips + 12 two-stage), and 53 specs were
confirmed as correctly escalating and recorded as such.** The 3 R2 errors in
`case_families/` were diagnosed and fixed; R2 across the whole repo is now
17 → 14.

The keep rate is high — 53 of 81 — and that is the finding, not caution. Two
distinct reasons, neither of which was visible before the specs were read one
by one:

1. **`promotion/`'s UC-A block is a review-removal population, not a
   read-only-status-query population.** 12 of its 20 UC-A specs ask to remove,
   edit or retract a rating or review. That is a write, and the knowledge
   corpus has no article on review removal at all — so the bot can neither
   perform the action nor ground a refusal. `promotion/` UC-A is not the D1
   population the re-plan is aimed at.
2. **`case_families/`'s R1 violations are mostly designed negative controls.**
   11 of its 17 are the family members whose entire purpose is to be the
   legitimate exception inside a resolve-policy UC. Flipping them would have
   deleted the discriminating member of each family.

Both of these are cases where flipping would have been the §5.4 failure mode
running in the opposite direction — widening the corpus to accept an answer the
bot cannot ground, or an action it cannot take.

## 1. Per-spec decision table

`flip` = expectation changed escalate → resolve. `two-stage` = converted to
`conditional_outcome_acceptance` (explanation half required, then handover may
be accepted). `keep` = no expectation change, adjudication recorded in-file.
`refer` = referred to the product owner, expectation untouched.

Keep reasons: **K1** data modification · **K2** high-risk (fraud / safety /
legal) · **K3** explicit human demand.

### 1.1 `promotion/` — 64 specs

| spec | UC | the customer's ask | decision | reason |
|---|---|---|---|---|
| `cs_interactive_096` | UC-A | the Rate Buyer option disappeared after the sale - how can the review still be left | **flip** | The Reviews article documents exactly when and where the rating control appears. |
| `cs_interactive_328` | UC-A | delete a review left for a buyer; also why does reposting always cost money; also repost the pet ads | **two-stage** | Mixed: the reposting-charge question is FAQ explanation; review deletion and reposting are writes. |
| `cs_interactive_017` | UC-A | remove a 1-star feedback left after the bike was already sold | **keep** | Removing a rating is a data modification; the FAQ corpus has no review-removal article. — K1 data modification |
| `cs_interactive_018` | UC-A | remove a false review and read the message thread as evidence | **keep** | Review removal plus an evidence investigation across another user's message thread. — K1 data modification |
| `cs_interactive_020` | UC-A | adjust an incorrect star rating from years ago | **keep** | Adjusting a stored rating is a data modification; no grounded removal policy exists. — K1 data modification |
| `cs_interactive_078` | UC-A | make the Company and Finance tabs stop throwing an authentication error | **keep** | A reproducible authentication defect on a Pro account; fixing it is platform-side. — K1 data modification |
| `cs_interactive_080` | UC-A | change the default delivery address to an Evri collection point for all future orders | **keep** | Changing a stored delivery default is a data modification. — K1 data modification |
| `cs_interactive_088` | UC-A | remove a 1-star review the user calls fraudulent | **keep** | Review removal; the fraud allegation also needs human adjudication. — K1 data modification |
| `cs_interactive_111` | UC-A | explain why ad 1508819706 was removed, dispute a linked email, threaten an Ombudsman complaint | **keep** | The user opens a formal legal/regulatory complaint; D2 keeps legal topics escalating. — K2 high-risk (legal) |
| `cs_interactive_126` | UC-A | investigate a seller who reneged on an agreed sale and take action against them | **keep** | Enforcement action against another member's account. — K1 data modification |
| `cs_interactive_130` | UC-A | publish a car ad free as a private seller despite the account being flagged Trade | **keep** | Reclassifying the account and waiving the trade fee are both writes. — K1 data modification |
| `cs_interactive_180` | UC-A | edit or delete a review the user left 14 days ago | **keep** | Deleting a stored review is a data modification. — K1 data modification |
| `cs_interactive_210` | UC-A | investigate and remove a negative review the user says is inaccurate | **keep** | Review removal plus an investigation into what the other party did. — K1 data modification |
| `cs_interactive_230` | UC-A | remove feedback the user left for a buyer earlier the same day | **keep** | Retracting a submitted review is a data modification. — K1 data modification |
| `cs_interactive_231` | UC-A | remove a 1-star review from six years ago | **keep** | Review removal, far outside any self-serve path. — K1 data modification |
| `cs_interactive_316` | UC-A | cancel the monthly payments to Gumtree, and be phoned back | **keep** | Cancelling a recurring billing agreement is a write, and the user demands a call back. — K1 data modification + K3 explicit human demand |
| `cs_interactive_317` | UC-A | remove a review from a non-buyer, then escalate it as a formal policy complaint | **keep** | The user explicitly asks for the case to be escalated as a policy complaint. — K1 data modification + K3 explicit human demand |
| `cs_interactive_335` | UC-A | put the seller back in touch with a buyer after the chat was deleted | **keep** | Requires releasing another member's contact route from deleted data. — K1 data modification |
| `cs_interactive_355` | UC-A | override the 30-day review policy and file a formal complaint about a previous outcome | **keep** | The user asks to override policy and explicitly asks how to file a complaint. — K1 data modification + K3 explicit human demand |
| `cs_interactive_357` | UC-A | remove a negative review left after the user declined a low offer | **keep** | Review removal; no grounded removal policy. — K1 data modification |
| `cs_interactive_003` | UC-C | why has the house-clearance ad received no inquiries | **flip** | Explanation of ad visibility plus a read-only status check on the user's own ad (D1). |
| `cs_interactive_058` | UC-C | a message cannot be opened, and can a deleted ad be brought back | **flip** | The deleted-ad question is answered verbatim by the knowledge corpus. |
| `cs_interactive_094` | UC-C | having accidentally made a second account, should the old one be deleted and will history be lost | **flip** | Purely advisory: the user asks what to do, not for the platform to do it. |
| `cs_interactive_134` | UC-C | route ad-reply notifications to the Gmail address instead of the Hotmail login email | **flip** | The spec's own transcript ends with the user thanking the agent for the answer. |
| `cs_interactive_143` | UC-C | why customer messages are not arriving at the account email, and can they be redirected | **flip** | The spec's own hidden_facts contains the explanation. |
| `cs_interactive_267` | UC-C | how to reach a seller whose replies appear to be going to spam | **flip** | The user's own message supplies the mechanism; the answer is notification guidance. |
| `cs_interactive_051` | UC-C | the review link goes nowhere; sold/unsold not visible on iPad; consolidate two accounts | **two-stage** | Mixed: the review flow and the iPad view are explanation; account consolidation is a write. |
| `cs_interactive_060` | UC-C | why does the rental ad keep being removed with no email, and get it posted | **two-stage** | Mixed: the hold reason is read-only moderation status (D1); clearing the hold is a write. |
| `cs_interactive_175` | UC-C | where are the replies to the new ad, and otherwise cancel it and refund | **two-stage** | Mixed: locating replies is explanation/read-only; the refund is a write. |
| `cs_interactive_027` | UC-C | remove a negative review from a person the user never sold to | **keep** | Review removal plus adjudication of a hostile exchange between two members. — K1 data modification |
| `cs_interactive_052` | UC-C | fix ad photos that upload blurred and compressed, already reported weeks ago | **keep** | An open image-pipeline defect already awaiting an engineering callback. — K1 data modification |
| `cs_interactive_122` | UC-C | messages broken, ads slow to go live, and most previously live ads removed after a date edit | **keep** | A multi-ad state defect requiring restoration the bot cannot perform. — K1 data modification |
| `cs_interactive_137` | UC-C | fix outgoing messages that show as sent but are never delivered | **keep** | A reproduced message-delivery defect; the remedy is a platform fix. — K1 data modification |
| `cs_interactive_200` | UC-C | retrieve a deleted message trail so the buyer can be contacted again | **keep** | Recovery of deleted conversation data plus third-party contact. — K1 data modification |
| `cs_interactive_203` | UC-C | report having been missold a car with undisclosed Category S damage | **keep** | A misselling allegation with legal exposure; D2 keeps fraud/legal escalating immediately. — K2 high-risk (fraud / legal) |
| `cs_interactive_275` | UC-C | delete two locked email addresses from account settings and add a new one | **keep** | Editing stored account emails is a data modification the user is explicitly blocked from. — K1 data modification |
| `cs_interactive_350` | UC-D | account locked after too many failed login attempts | **flip** | A standard temporary lockout with documented self-serve recovery. |
| `cs_interactive_092` | UC-D | restore a star rating that reset to zero, and explain why live ads get no enquiries | **two-stage** | Mixed: the enquiries question is explanation; restoring a rating is a write. |
| `cs_interactive_154` | UC-D | regain access after a failing password reset, delete a duplicate Apple account, and find the missing price field | **two-stage** | Mixed: reset guidance and the price-field question are explanation; deleting an account is a write. |
| `cs_interactive_166` | UC-D | make a twice-removed ad live again | **keep** | Reinstating a removed ad is a write. — K1 data modification |
| `cs_interactive_225` | UC-D | regain account access after the reset fails and the email is flagged a security threat | **keep** | A security-layer block on the account; clearing it is platform-side. — K1 data modification |
| `cs_interactive_302` | UC-D | change the phone number and email on an account the user can no longer log into | **keep** | Credential change without account access; identity verification is required. — K1 data modification + K2 high-risk (identity) |
| `cs_interactive_306` | UC-D | log in when a web application firewall blocks the request | **keep** | A WAF block with a specific incident id; only the platform can clear it. — K1 data modification |
| `cs_interactive_223` | UC-E | what does the 'car not road registered' error mean and how is the listing completed | **flip** | The user explicitly asks what the validation error means - explanation-class. |
| `cs_interactive_091` | UC-E | put a telephone number on an ad, and fix the wrong email on expired ads | **two-stage** | Mixed: the phone-number rule is policy explanation; correcting stored ad emails is a write. |
| `cs_interactive_086` | UC-E | restore the telephone-call option on adverts for a partially blind user | **refer** | Whether to restore a withdrawn platform feature is an undecided product decision. |
| `cs_interactive_320` | UC-F | how does a first-time seller get paid after an item sells with delivery enabled | **flip** | A pure how-does-it-work question about the payout flow. |
| `cs_interactive_361` | UC-F | why is posting no longer free - the flow keeps asking for payment | **flip** | The spec's own hidden_facts states the cause. |
| `cs_interactive_129` | UC-F | a renewed ad paid for at £8.99 is not visible in live ads - find it or refund | **two-stage** | Mixed: ad-state lookup is read-only (D1); the refund is a write. |
| `cs_interactive_260` | UC-F | the ad is invisible and shows unpaid although the payment process was completed | **two-stage** | Mixed: ad/payment state is read-only (D1); reconciling the payment is a write. |
| `cs_interactive_007` | UC-F | refund a duplicate £9.99 promotion charge | **keep** | A refund is a financial write. — K1 data modification |
| `cs_interactive_022` | UC-F | apply a paid invoice to the account and send a confirmation email | **keep** | Reconciling a payment against an account is a financial write. — K1 data modification |
| `cs_interactive_028` | UC-F | activate the account after an outstanding invoice was settled | **keep** | Activating an account is a state write. — K1 data modification |
| `cs_interactive_077` | UC-F | stop the ad charges and switch the account from business to personal | **keep** | Reclassifying an account and stopping charges are both writes. — K1 data modification |
| `cs_interactive_125` | UC-F | stop recurring monthly debits and speak to a person by phone | **keep** | Cancelling a recurring mandate is a write, and the user demands a phone agent. — K1 data modification + K3 explicit human demand |
| `cs_interactive_219` | UC-F | make a failing card payment go through to bump an ad before the 30th | **keep** | A payment that will not process; the bot has no payment tool. — K1 data modification |
| `cs_interactive_229` | UC-F | a job ad cannot be paid for - clicking payment only saves the ad | **keep** | A blocked checkout; completing it is a platform action. — K1 data modification |
| `cs_interactive_070` | UC-F | complete secure-payment onboarding as a retired non-taxpayer who cannot submit the tax form | **refer** | Whether a non-taxpayer can be onboarded is an undecided product/policy question. |
| `cs_interactive_023` | UC-FP | does reposting an expired puppy ad count against the three-ad limit | **flip** | A pure posting-policy question with no platform action requested. |
| `cs_interactive_255` | UC-FP | which other accounts are linked to this user, and get the twice-paid kitten ad posted | **two-stage** | Mixed: the account-linkage question is read-only (D1); clearing the rejection is a write. |
| `cs_interactive_131` | UC-FP | activate a paid business ad that is not live | **keep** | Activating a held listing is a moderation write. — K1 data modification |
| `cs_interactive_227` | UC-FP | do not deactivate the business account and grant permission to post | **keep** | Account-status decision and posting permission are both platform writes. — K1 data modification |
| `cs_interactive_253` | UC-FP | take enforcement action against other accounts breaking the posting policy | **keep** | Enforcement against third-party accounts. — K1 data modification |
| `cs_interactive_254` | UC-FP | unban and reactivate an account restricted for policy violations | **keep** | Lifting a ban is a moderation write. — K1 data modification |

### 1.2 `case_families/` — 17 specs

| spec | UC | the customer's ask | decision | reason |
|---|---|---|---|---|
| `csmp_g01_uc_a_genuine_faq_miss_obscure` | UC-A | why an ad ranks higher in Welsh-language search than English | **keep** | Designed negative control: a genuine FAQ miss on per-language ranking. — K1 out-of-corpus edge |
| `csmp_g02_uc_a_user_requested_specialist` | UC-A | asks to be connected to a specialist about listing visibility | **keep** | Designed negative control: explicit human request from the first turn. — K3 explicit human demand |
| `cs92g01_uc_b_obscure_policy_genuine_faq_miss` | UC-B | whether live crickets for reptile food are allowed under livestock rules | **keep** | Designed negative control: a genuine FAQ miss on a niche policy edge. — K1 out-of-corpus policy edge |
| `cs92g02_uc_b_explicit_user_requested` | UC-B | asks to be connected to a person for help posting an ad | **keep** | Designed negative control: explicit human request from the first turn. — K3 explicit human demand |
| `cs01n02_uc_c_messaging_general` | UC-C | listings show interest counts but no messages - is this normal or does someone need to look | **two-stage** | Mixed by design: verify and explain first, escalate only if it survives that. |
| `cs01g01_uc_c_user_requested_handover` | UC-C | explicitly asks for a human about a messaging issue | **keep** | Designed negative control: explicit human request from the first turn. — K3 explicit human demand |
| `cs01g02_uc_c_strong_distress_signal` | UC-C | high-distress messaging complaint demanding a real person | **keep** | Designed negative control; kept on the explicit demand, not on the distress. — K3 explicit human demand |
| `cs95n01_uc_d_app_login_email_mismatch` | UC-D | the app and website are signed into different accounts - which is primary and how to consolidate | **flip** | Read-only account-state verification plus an explanation; the spec ships the cause. |
| `cs95n02_uc_d_no_ads_visible_state` | UC-D | the profile shows zero ads although three were posted this week and were live yesterday | **flip** | A read-only status query on the user's own ads - the D1 poster case. |
| `cs11n01_uc_d_2fa_loop_verbose` | UC-D | 2FA SMS codes are always rejected despite being entered immediately | **two-stage** | Mixed by design: the spec asks for 'either resolve grounded or hand over honestly'. |
| `cs11g01_uc_d_brief_user_requested` | UC-D | explicitly asks for a human to fix an account issue | **keep** | Designed negative control: explicit human request from the first turn. — K3 explicit human demand |
| `cs11g02_uc_d_explicit_distress` | UC-D | reports a suspected account takeover | **keep** | Designed negative control; kept on the security risk, not the distress phrasing. — K2 high-risk (account compromise) |
| `cs76g02_uc_e_clarification_first_turn` | UC-E | a vague complaint that a paid featured ad 'isn't right' | **keep** | Designed negative control on clarification-before-outcome; R2 field corrected. — K3 explicit human demand (post-clarification) |
| `cs76n01_uc_e_refund_demand` | UC-E | refund £30 for a Premium Listing that never delivered | **keep** | A refund is a financial write; R2 field corrected. — K1 data modification |
| `cs76n02_uc_e_fulfillment_demand` | UC-E | deliver the missing 18 days of a Top Ad or give the days back | **keep** | Fulfilment or remediation of a paid service is a write; R2 field corrected. — K1 data modification |
| `cs59g01_uc_f_obscure_payment_taxation` | UC-F | whether Gumtree sale income must be reported to HMRC, and VAT treatment | **keep** | Designed negative control: a genuine FAQ miss on regulated tax advice. — K1 out-of-corpus + regulated advice |
| `cs59g02_uc_f_user_requested_first_turn` | UC-F | asks for a human on a payment problem in the opening turn | **keep** | Designed negative control: explicit human request from the first turn. — K3 explicit human demand |

## 2. Decision mix and why it looks like this

| decision | promotion | case_families | total |
|---|---:|---:|---:|
| flip → resolve | 12 | 2 | **14** |
| two-stage | 10 | 2 | **12** |
| keep escalate | 40 | 13 | **53** |
| refer to product owner | 2 | 0 | **2** |
| **total** | **64** | **17** | **81** |

WS-2 flipped 51 of 80 on `anchor/` + `smoke/` (64%). This pass flips or
converts 26 of 81 (32%). The contract predicted the keep rate here would be
higher, and it is, for the two structural reasons in §0.

**Where the flips came from.** The strongest evidence class is a spec that
ships its own answer and then demands a handover anyway — the clearest possible
proof that the `escalate` label was generator noise rather than judgement:

| spec | the evidence, in the spec or the corpus |
|---|---|
| `cs_interactive_361` | `hidden_facts`: "Your ad was posted in the Services category, not the For Sale category" — the answer to "is gumtree not free to post anymore" |
| `cs_interactive_143` | `hidden_facts`: the account's contact email "is no longer functional due to platform changes" — the answer to "why am I not receiving customer messages" |
| `cs_interactive_134` | the persona's final seed message is "Ok, well at least I know now what to do ... Thanks so much for your help!" — the script records the customer satisfied by an explanation |
| `cs_interactive_058` | KB `My Ad was Removed` (ka44J000000gKv5QAE) answers "can you bring up an ad after you deleted it" verbatim, including the Repost route |
| `cs_interactive_096` | KB `Reviews` (ka4P2000000065RIAQ) documents where the Rate Buyer/Seller control lives and the 5-day publication rule |
| `cs_interactive_223` | user says "I don't know what not road registered means" — a request for a definition the platform owns |
| `cs_interactive_350` | user states the cause ("ientered it wrong to many time and block me") and closes with "Ok thankyou" |
| `cs95n02` / `cs95n01` | the specs' own goals say the bot "must verify state via `get_customer_context`" and "verify the state and explain how to consolidate" — the D1 case, graded as its opposite |

The remaining flips are explanation-class on their face: `003` (why no
enquiries, ad id supplied for a D1 lookup), `023` (does reposting count against
the 3-ad limit), `094` (should I delete the duplicate account), `267` (messages
going to spam), `320` (how do I get paid after a sale).

**Why 53 keeps.** Distribution of keep reasons (a spec may carry two; the
leading reason is counted first and co-citations are listed separately):

- **K1, the platform must write or act — 40.** Refunds; review and rating
  removal; ad or account restore / activate / unban; stored credential and
  billing changes; enforcement against third-party accounts; and platform
  defects whose only remedy is an engineering fix (`052`, `078`, `122`, `137`,
  `219`, `229`).
- **K1, genuine out-of-corpus FAQ miss — 3.** `cs59g01` (HMRC tax reporting),
  `cs92g01` (live crickets under livestock rules), `csmp_g01` (per-language
  search ranking). All three are `case_families` negative controls whose stated
  purpose is that an honest search followed by `faq_miss` IS the correct
  behaviour. Flipping them would force the bot to invent a policy answer — the
  grounding failure they exist to catch. (Grouped under K1 in the per-spec
  table; they are not data modification and are broken out here.)
- **K2 high-risk — 4.** `111` (Ombudsman + Data Protection Act complaint),
  `203` (Category S misselling — consumer law), `302` (credential change
  without account access — the account-takeover shape), `cs11g02` (suspected
  account compromise). `302` is co-cited with K1.
- **K3 explicit human demand — 11.** Seven are `case_families` members built
  precisely to test that trigger (`cs01g01`, `cs01g02`, `cs11g01`, `cs59g02`,
  `cs92g02`, `csmp_g02`, `cs76g02`); four are co-cited with K1 on `promotion`
  specs (`125`, `316`, `317`, `355`).

*Correction to a commit message:* commit `a4240e68` ("record 40 promotion specs
as deliberate escalate keeps") states "K1 35 / K2 3". The correct promotion-only
tally is **K1-leading 38, K2-bearing 3, K3 co-cited 4**. The numbers above and
the per-spec table in §1 are authoritative.

Decision D2 revised "customer indicates frustration ⇒ must escalate" to
"de-escalate and keep solving first". **Frustration alone was not accepted as a
keep reason anywhere in this sprint.** Two `case_families` negative controls
were kept but had their rationale explicitly *narrowed* in-file, because their
authored justification is now the weaker of two grounds:

- `cs01g02_uc_c_strong_distress_signal` — kept on the explicit demand
  ("GET ME A REAL PERSON"), not on the ALL-CAPS distress framing.
- `cs11g02_uc_d_explicit_distress` — kept on the security risk
  ("SOMEONE CHANGED MY EMAIL WITHOUT ME"), not on the distress framing.

If D2 is ever tightened further, those two are the first specs to re-examine.

**Why two-stage rather than a permissive list.** A plain
`acceptable_outcomes: [resolve, escalate]` scores 1.0 for both terminals
(`_check_correct_outcome` takes the acceptable-set branch first), i.e. it
measures nothing on the axis this workstream exists to fix — WS-2 found exactly
that on three specs. The declarative block gives a real signal:

| bot behaviour | verdict |
|---|---|
| explanation delivered, customer satisfied, outcome `resolve` | **PASS** |
| grounded help delivered, customer still positively UNRESOLVED, then handover | **CONDITIONAL_ELIGIBLE** (0.0, review-required) |
| handover with **no** closure-qualified grounded-help marker | **FAIL** |

`require_closure_precondition` is satisfied structurally — a bot turn carrying
tool calls AND non-empty `source_ids` AND a non-empty answer
(`conditional_outcome.py:113-132`) — so it does not depend on these specs
having a `closure_criterion` field, which they do not.

`allow_bot_resolution` was deliberately left at the UC policy value rather than
set to `'partial'` (as WS-2 did on the UC-K spec `iwzx`): linter R2 compares
that field to the policy row, so `'partial'` on a UC-A/C/D/E/F/FP spec would
have traded an R1 error for an R2 one.

## 3. Before / after linter counts

Command, run from `eval_interactive/`, for every bucket:

```
uv run eval-interactive lint --path case_specs/<bucket> --summary-only --exit-zero
```

Counted with the linter and the loader throughout. **Not** with `grep`: WS-2
wrote a rationale block into every file it changed and that block quotes the
strings `should_escalate: true` and `allow_bot_resolution: 'true'`, so a text
search reports the pre-WS-2 number and is wrong. This sprint's notes do the
same.

### 3.1 Per bucket

| bucket | errors before | errors after | Δ |
|---|---:|---:|---:|
| `promotion/` (101 specs) | 64 | **42** | −22 |
| `case_families/` (50 specs) | 22 | **15** | −7 |
| `anchor/` (159 specs) | 20 | 20 | 0 |
| `smoke/` (14 specs) | 3 | 3 | 0 |
| `bad_cases/` (19 specs) | 16 | 16 | 0 |
| `exploration/` (107 specs) | 0 | 0 | 0 |

The four buckets outside this sprint's scope are byte-unchanged — confirmed by
`git status`, which shows 82 changed files all under
`case_specs/{promotion,case_families}/`.

### 3.2 Per rule, for the two buckets in scope

| bucket | rule | before | after | Δ |
|---|---|---:|---:|---:|
| `promotion/` | R1 `uc_outcome_consistent` | 64 | **42** | −22 |
| `promotion/` | R11 `hidden_fact_not_duplicating_form` (warn) | 1 | 1 | 0 |
| `case_families/` | R1 `uc_outcome_consistent` | 17 | **13** | −4 |
| `case_families/` | R2 `uc_allow_bot_resolution_consistent` | 3 | **0** | −3 |
| `case_families/` | R0 `legacy_field_name` | 1 | 1 | 0 |
| `case_families/` | R9 `bot_handling_pattern_required` | 1 | 1 | 0 |
| `case_families/` | R10 `intake_fields_present_for_intake_uc` | 1 | 1 | 0 |
| `case_families/` | R8 `user_goal_summary_neutral` (warn) | 5 | 5 | 0 |

The R1 arithmetic reconciles exactly to the decision table: promotion
64 − 12 flips − 10 two-stage = 42 (40 keeps + 2 referrals); case_families
17 − 2 flips − 2 two-stage = 13.

### 3.3 The residual R1 count is deliberate, and R1 cannot say so

**42 + 13 = 55 R1 errors remain in the two buckets, and every one is a
reviewed, deliberate keep or referral.** R1 is a strict equality check of
`expected.outcome_class` against the UC policy row
(`case_spec/linter.py:236-249`); it has no waiver, suppression or
"adjudicated" state. A spec that genuinely must escalate inside a
resolve-policy UC is therefore permanently an R1 error.

The practical consequence: **the R1 count can no longer be read as a backlog.**
After this sprint, a reader cannot distinguish "not yet reviewed" from
"reviewed and deliberately kept" from the count alone — only from the
`expectation_revision_note` now present in every one of the 81 files. This is
recorded as a real defect in §7.

## 4. `cs_interactive_185` — flagged for Sprint 104

**Verdict: `primary_uc` is CORRECT as UC-D. Nothing changed but an
`expectation_revision_note`.**

Sprint 103 §6.6 flagged that "the spec expects UC-D (Account & Login), but its
scripted drift in every draw is toward editing live ads (UC-A/UC-B territory)".
Adjudicated on the spec's own evidence, the pin holds and the premise does not:

1. **Every authored field is Account & Login.** `topic_subject` is "Account
   Support"; the form description is a browser cookie-clearing instruction for
   regaining access; `user_goal_summary` opens "User wants to access his
   Gumtree account"; all three `seed_messages` are one thread (the
   4-dots/More-tools path, "Trying to get access to my account", the old
   address blocked by Microsoft). There is no authored intent to edit, post or
   manage an advert anywhere in the spec.
2. **The only ad-related content is one `hidden_facts` entry** — "The user has
   two live ads posted on their Gumtree account" — whose `disclose_when` is
   "when the bot asks about account activity or ad status". That is a reply the
   persona gives when questioned, not a goal it pursues.
3. **"In every draw" is contradicted by Sprint 103's own evidence table**
   (`sprint-103-handoff.md` §5.2). Draw 2 ran `DISCOVER; DISCOVER→RESOLVE[UC-D];
   RESOLVE→ESCALATE[UC-D]` and draw 3 ran `DISCOVER×2; DISCOVER→ESCALATE[UC-D]`
   — UC-D throughout. Only draw 1 ended UC-A, and `propose_reroute` never fired
   on this case in any draw. 2 of 3 draws agree with the pin. Sprint 103 itself
   hedged the finding as "arguably wrong".

`drift_behavior` was also left alone. `hard_shift` is arguably overstated —
both of the persona's threads (the cookie/browser block, the Microsoft-blocked
email) sit inside UC-D, which is a soft shift. It was not changed for two
reasons. First, the single UC-A excursion is better explained by a documented
eval-framework defect than by this spec: per replan rev 2 §1.4 the
`persona.drift_behavior` field never reaches the model, and the only thing
rendered is the bare token `Drift: hard_shift.` appended to `user_goal_summary`
(`extractor.py:1144`) — the simulator is told to shift without being told to
what, and the two-live-ads hidden fact is the only other material available to
shift toward. That is `infra` under §3.2 question 1, and out of this contract.
Second, retuning the class would silently remove this case from the drift
population Sprint 104 is measuring right now, on a premise its own evidence
does not support — and changing a spec to match observed bot behaviour is the
§5.4 failure mode this workstream exists to undo.

**For Sprint 104: `cs_interactive_185` is byte-unchanged apart from a comment
block. No drift measurement taken against it needs re-attribution.**

Sprint 104 should however note §5 below: 10 `promotion/` specs now carry a
two-stage block, which changes how their escalate path scores.

## 5. `case_spec_overrides.yaml` — swept, zero conflicts, nothing touched

**The file is unchanged.** Method: parse all 17 override entries, collect their
`source_session_id`s, and cross-reference against the `source_session_id` of
every one of the 81 specs adjudicated.

- 17 overrides, 17 unique sessions, all `status: approved`.
- **Exactly one** intersects this sprint's scope: session
  `570Q5000008wmKbIAI` → `cs_interactive_130`.
- That entry carries a **`classification` block only** — `primary_uc: UC-A`,
  `secondary_ucs: [UC-H, UC-D]` — which is *identical to what the spec already
  declares*. It has no `expected` block, so it cannot re-assert an
  escalate-shaped expectation.
- `cs_interactive_130` is a **keep**. Its outcome was not changed.

**The WS-2 hazard does not arise this sprint.** WS-2 found approved L3
overrides pinning escalate-shaped `expected` blocks on two flipped sessions,
which would have re-asserted at the next regeneration and emitted an
`escalation_trigger` on a `should_escalate: false` spec — a latent
`Expected.__post_init__` load failure. Here, **none of the 14 flips and none of
the 12 two-stage conversions has an override entry of any kind.** No approval
was deleted, modified, or needed to be.

**Test files touched under `eval_interactive/tests/**`: none.** The
override-regression test `tests/regression/test_case_spec_overrides.py` and all
count-anchor tests were carved out to this sprint but required no change,
because no override and no corpus count moved. Nothing was contended with
Sprint 105, so no yield was necessary.

## 6. Specs referred to the product owner, not decided

Per contract §5, where the answer turns on a product decision nobody has taken,
the dev agent does not take it. Both keep their current expectation; both are
marked in-file; both remain R1 errors.

| spec | UC | the undecided question |
|---|---|---|
| `cs_interactive_070` | UC-F | A retired non-taxpayer with a Gateway account cannot complete the mandatory secure-payment onboarding form. **Does an exception route exist for non-taxpayers?** Until that is answered there is no correct bot behaviour to encode — this is compliance, not eval. |
| `cs_interactive_086` | UC-E | A carer argues that withdrawing the call option from adverts excludes blind and disabled users. **Will the feature return, or is there an accessibility route to offer?** The complaint also carries a discrimination dimension. |

`cs_interactive_091` (UC-E) raises the *same* feature withdrawal but only asks
"so what do I do?" — a policy explanation the bot can give. It was converted to
two-stage rather than referred. If the product owner rules on `086`, `091`
should be re-read at the same time.

## 7. Real defects found, out of contract, not fixed

1. **The generator will re-create every contradiction on the next
   regeneration.** This is the load-bearing one. Nothing upstream changed:
   `case_spec/case_outcome_resolver.py:196-206` and
   `transcript_evidence.py:146-208` still read "a human agent took this session
   over" as "the bot should escalate", and the corpus is by definition sessions
   that reached a human. WS-2 rewrote 80 specs, this sprint rewrote 81 more, and
   **a regeneration would undo all 161.** The 26 flips and conversions are
   therefore currently protected only by the fact that nobody re-runs the
   extractor. Either the resolver needs the fix, or the rewritten expectations
   need to be pinned as approved overrides in `case_spec_overrides.yaml`. This
   should be a named workstream, not a footnote.

2. **R1 cannot record an adjudicated exception** (§3.3). 55 deliberate keeps
   are indistinguishable from 55 unreviewed specs in the count. Suggested shape:
   a per-spec `lint_waiver: {rule: R1, reason: ..., reviewed: <date>}` that the
   linter downgrades to a warning, so the error count returns to being a
   backlog measure. Not attempted here — it is a change to the Python package,
   which is Sprint 105's path.

3. **Two-stage on a programmatic bucket needs an adjudication route.**
   `promotion/` and `case_families/` both resolve to `case_passed_authority:
   "programmatic"` (`batch/executor.py:48-70`); only `bad_cases` and
   `anchor_outcome` are human-judgment suites. So on the 12 specs converted
   here, escalate-after-grounded-help lands `CONDITIONAL_ELIGIBLE` and scores
   0.0 until a per-trace entry exists in
   `case_specs/conditional_outcome_adjudications.yaml`. This is *deliberate* —
   it refuses to auto-pay 1.0 for a handover, which is the whole point — but it
   means those 12 specs can only reach PASS via the resolve path unless someone
   adjudicates. WS-2 avoided this by converting only `bad_cases` specs. **Sprint
   104 and the deliver agent need to know this before reading any pass-rate on
   `promotion/`.**

4. **`cs_interactive_253`'s `user_goal_summary` says "Facebook" where it means
   Gumtree** — "User wants Facebook to enforce posting policies against accounts
   that are violating them". A generator hallucination in persona text that is
   rendered to the simulator. Recorded in the file's note; not corrected,
   because persona text was outside this sprint's remit and correcting it would
   change what the simulator is told mid-flight while Sprint 104 measures.

5. **Sprint 103's §6.6 finding on `cs_interactive_185` is contradicted by its
   own §5.2 table** (§4 above). Not a code defect, but a handoff claim that
   would have caused a wrong edit had it been taken at face value.

6. **Five specs repo-wide have `should_escalate: true` with a null
   `escalation_trigger`** (R3, surfaced as loader warnings during the
   full-corpus load). Pre-existing, none in this sprint's scope, none created
   here — a flip sets both fields together, so it cannot produce this shape.

7. **`case_families/` carries 3 non-R1/R2 errors this sprint did not touch**,
   because the contract scoped item 2 to R1 + R2: 1 R0 legacy-field and 1 R9
   missing `bot_handling_pattern` (both on `_manifest.yaml`), and 1 R10
   intake-fields error on `cs40g02_uc_k_explicit_user_requested`. Plus 5 R8
   warnings.

## 8. Where I think this contract is wrong

1. **"Use the two-stage mechanism exactly as WS-2 did" does not transfer
   cleanly, and the contract does not say so.** WS-2 applied
   `conditional_outcome_acceptance` to `bad_cases/` specs — a human-review
   authority suite where the adjudication step already exists as process.
   `promotion/` is programmatic. Following the instruction literally therefore
   introduces a scoring state (`CONDITIONAL_ELIGIBLE`) that has no owner in
   this bucket. I followed it, kept the count modest (10 of 64), and documented
   the consequence in §7.3 — but a contract that names a mechanism should name
   the bucket-authority precondition with it.

2. **The contract's headline framing — "resolve-class intent-switching
   coverage lands almost entirely on `promotion/`" — set up an expectation that
   the UC-A block would be D1 material.** It is not; it is a review-removal
   population (§0.1). The re-plan's §1.4 reasoning about `hard_shift` counts is
   about *drift* coverage, which is a different axis from *resolvability*. A
   bucket can be the right place to measure intent-switching and still be the
   wrong place to look for read-only status queries. Worth separating those two
   claims before the next bucket is scoped.

3. **The pytest baseline in §6 was reproducible only after copying a
   gitignored directory.** `data/*` is gitignored in full, so `git worktree
   add` does not carry `data/eval_datasets/badcase_turns.csv`, and a fresh
   worktree reports **15 failed / 763 passed**, not the contract's 14/764. The
   extra failure is an artifact of the worktree, not of anyone's code. The
   contract already tells us to copy `.env.local` for exactly this reason; it
   should tell us to copy `data/` too, or the next four parallel sub-sprints
   will each rediscover it. (Sprint 103 §7.1 separately corrected the
   contract's claim that all 14 come from the missing golden CSV — 13 do, and 1
   is the stale Skill count. That correction is confirmed here and is still not
   reflected in the contract text.)

4. **Minor:** the contract says the worktree tip must read `d7d84f86`. The tip
   of `perf-replan-2026-07` is `82f8a137` — the commit that added the contract
   itself. Sprints 104 and 105 both branched from `82f8a137`; this worktree
   matches them.

## 9. Verification

| gate | baseline | after | verdict |
|---|---|---|---|
| `uv run pytest -q` (from `eval_interactive/`) | 14 failed, 764 passed, 5 skipped | **14 failed, 764 passed, 5 skipped** | unchanged |
| full-corpus load (`load_case_spec` over 486 specs) | — | **486 OK, 0 failed** | pass |
| `mvn -o test` (from `server/`) | Tests run: 1493, Failures: 1 | Tests run: 1493, Failures: 1, Errors: 0, Skipped: 2 | **unchanged** |
| files changed outside owned paths | — | **0** | pass |

Baseline pytest attribution, re-measured rather than trusted: 13 of the 14
failures come from the missing `data/human_review_annotations_2026-04-22_golden.csv`
(Sprint 107's scope; the file does not exist in the primary checkout either),
and 1 is `test_load_all_six_production_skills_populated_critical_steps`
asserting `expected 6 production Skill YAMLs, found 7` — the stale count that
is Sprint 105's item 5. Neither is this sprint's.

**Zero `Expected.__post_init__` failures**, which is the specific failure mode
§5 guards against: a flip sets `should_escalate: false` and
`escalation_trigger: null` together, so the rejected combination cannot be
produced, and no override re-asserts one.

## 10. Self-check

- [x] Worktree `../csagent-wt-106` on `sprint-106-casespec-promotion`; all 82
      changed files under `case_specs/{promotion,case_families}/`; `server/**`,
      `eval_interactive/eval_interactive/**`, `eval/**`, `data/**`, `Makefile`,
      `autoloop/**`, `e2e/**` diffs all empty.
- [x] All 64 `promotion/` + 17 `case_families/` R1 specs adjudicated
      individually; decision table complete, no blank reasons.
- [x] Counted with the linter and the loader, never with `grep` (§3).
- [x] No `acceptable_outcomes` list gained `escalate`; no permissive
      `[resolve, escalate]` introduced anywhere — the two-stage block was used
      precisely to avoid that.
- [x] Full-corpus load succeeds: 486 specs, 0 failures.
- [x] `case_spec_overrides.yaml` cross-referenced by `source_session_id` for
      every changed spec; 1 intersection found, classification-only, on a keep;
      file unchanged, no approval deleted.
- [x] `cs_interactive_185` adjudicated and flagged for Sprint 104 (§4).
- [x] Test files touched under `eval_interactive/tests/**`: **none**; nothing
      contended with Sprint 105, so no yield needed (§5).
- [x] No bot session run; no backend restart; autoloop not resumed; no Codex
      dispatched; no merge to `perf-replan-2026-07` or `main`.
