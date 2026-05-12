# Failure Brief — cs192 UC-B mechanical escalate on resolvable giveaway question

> **Source case:** `cs_interactive_192` (UC-B primary, UC-K / UC-D / UC-B secondary, expected `resolve`, `should_escalate: false`).
> **Source session:** `570Q5000008rbcjIAA`.
> **CaseSpec:** `eval_interactive/case_specs/smoke/cs_interactive_192.yaml`.
> **Approved L3 override:** NONE (greped `case_spec_overrides.yaml`; no entry for this `source_session_id`).
> **Runs:** `20260505-235231` (FAIL, composite=0.0, L2_GATE:correct_outcome + L3:relevance/tone=2.0). 2026-05-10 regressed further to `CONTRACT_VIOLATION:active_use_case` — Cluster C, deferred via `R-smoke-regression-investigation`.
> **Filed:** 2026-05-13 (Sprint 18 / G1, brief #8).

## Ground-truth chain

cs_192 has no approved L3 override. The CaseSpec is rule-extracted L1 + L2 LLM persona review.

Ground truth: UC-B (Posting & Editing Guidance), `outcome_class: resolve`, `should_escalate: false`, `allow_bot_resolution: 'true'`, expected tool sequence `search_knowledge → resolve_article → record_outcome`, `grounding_mode: faq_source_backed`. Forbidden tools include `get_customer_context` (UC-B is not on the phase 2 line 358 allowed list — no CaseSpec/phase 2 conflict here; this matches the `R-generator-get-customer-context-policy-mismatch` exclusion pattern). `bot_handling_pattern: "Acknowledge the issue, run policy-mandated tools (...), and answer from the knowledge base. Do not escalate."`

The user's question — "I want to give away free items. Can I do this on your site? And if so then how do I do it?" — is a canonical UC-B posting-guidance question. Giving away free items is a common Gumtree posting pattern (e.g. "Freebies" listings), and the answer is a combination of (a) yes, free / £0 listings are allowed, (b) how to set the price field to £0 or free, (c) which categories accept giveaways. The CaseSpec correctly classifies this as resolvable.

The CaseSpec's `secondary_ucs: [UC-K, UC-D, UC-B]` has `UC-B` listed both as primary and secondary — likely a CaseSpec generator quirk (the L1 rule extractor may have appended UC-B from a different signal source). Not blocking, but flag for L3 review during G1 packaging (see Related observation).

The 2026-05-10 run regression (`CONTRACT_VIOLATION:active_use_case` — `active_use_case` was unstamped, a runtime contract violation that didn't exist in 2026-05-05) is Cluster C / deferred.

## What happened?

User entered via the Ad Support form: `form_context.first_name="Rita"`, description "I want to give away free items.  Can I do this on your site?  And if so then how do I do it?", empty ad_id, persona `frustration_level: mild`, `drift_behavior: soft_shift`. The 2-turn transcript:

- **T0 user** (form): "I want to give away free items.  Can I do this on your site?  And if so then how do I do it?"
- **T0 bot:** "Hi Rita! I'm having difficulty resolving this. Let me connect you with a specialist."
- **T1 user:** "OK. What items are allowed?"
- **T1 bot:** "This conversation has been transferred to a human agent. Please wait for them to respond."

Tool sequence: `search_knowledge × 2 → resolve_article × 2 → request_handover`. The bot ran a search-and-retry cycle (re-searching with different queries) before declaring faq_miss — more search effort than cs_001 (1 search + 3 resolves). But still no resolve-grade article surfaced, and the bot fell back to `escalation_reason=faq_miss_threshold_exceeded`.

The failure has two layers visible at the same surface:

1. **Outcome-level fail.** Expected `resolve`; actual `escalate`. L2 `correct_outcome=0.0` ("actual=escalate not in acceptable=['resolve']"). `correct_uc=1.0` (bot correctly stamped UC-B), `tool_sequence_match=0.667` (the search-retry burned an extra search and skipped `record_outcome`).

2. **Same mechanical-template surface seen in cs_001 / cs_011 / cs_038 / cs_040.** Bot's T0 is the same fixed template — "Hi {Name}! I'm having difficulty resolving this. Let me connect you with a specialist." — produced regardless of input. The bot's tool work (2 searches + 2 resolves) happened behind the scenes; the user-facing message gave zero acknowledgment of "free items" or "giveaway" or even "posting an ad". The single difference from cs_001's bot turn is that there is no duplicated greeting (single "Hi Rita!"), so the dup-greeting bug from cs_095 / cs_176 is not active here.

The L3 judge again flagged "misidentifies the user's name (no prior mention of 'Rita')" — but `form_context.first_name="Rita"`. Third confirmed instance of the form-context-trust judge calibration issue, after cs_038 (Paul) and cs_040 (Mo). Reinforces `R-l3-judge-form-context-trust-rubric`.

## What should a good CS agent have done?

The capability gap is **completing a real FAQ resolve attempt on a basic UC-B posting question, and either landing a resolve or admitting the gap honestly rather than masking it as "I'm having difficulty resolving this"**.

Concretely:

- **Acknowledge the user's specific question.** "Free items / giveaway listings" is the user's frame; the bot's T0 message acknowledges nothing. A real reply would name "free items" or "giveaway" or "no-charge listings" in its phrasing.

- **Make the FAQ retrieval work, or admit it didn't.** The bot ran 2 searches but no resolve-grade hit surfaced. Either the corpus does have UC-B giveaway content and the bot's queries didn't match it (search-quality issue), or the corpus doesn't have it (corpus gap). Either way, when search comes up empty on a basic UC-B question, the honest reply is "I couldn't find a specific article on giveaways — but in general, free / £0 listings are allowed; let me check with someone who knows the latest policy" rather than the fixed template.

- **Sustain the conversation past T0.** The user's T1 ("OK. What items are allowed?") is a clear follow-up — they pivoted to a tighter sub-question. The bot's T1 just confirmed the handoff with no engagement with the new question. Even if the handover is already in flight, the bot could relay the question into the handover payload so the human picks up with context.

- **Avoid the fixed-template fallback when search effort was non-trivial.** The bot did real work (2 searches + 2 resolves); the user-facing wording should reflect that, not collapse into a generic difficulty-resolving template.

## Why does this matter?

cs_192 is an outcome-level FAIL on a case that the CaseSpec, phase 2 UC-B policy, and the user's plain-language question all agree should be bot-resolvable. UC-B (Posting & Editing Guidance) is fundamentally bot-resolvable territory; "I want to give away free items, can I do this on your site?" is a canonical UC-B question with no policy ambiguity. The bot's template-escalate response without a substantive acknowledgment of the question or topic represents a containment-rate gap on the highest-volume Ad Support entry-point. Real-world UC-B traffic on giveaway / free-items posting likely accounts for a meaningful slice of Ad Support volume; any case in this shape currently failing outcome on the bot side is a real customer-experience and containment-rate cost.

cs_192 also surfaces a **potential UC-B corpus gap on giveaway / free-items posting**: the bot's 2 search_knowledge attempts found no resolve-grade hit. Whether this is a corpus gap (UC-B doesn't have a "free items / giveaways" article) or a retrieval-quality issue (article exists but bot's query didn't match) needs a corpus check during the next eval-governance sprint. Similar in shape to cs_095's UC-D account/email corpus gap (different UC, different topic), strengthening the case that **a corpus-coverage audit per UC is a real backlog item** for the eval-governance track.

## Is this a one-off or a pattern?

`pattern` on three independent dimensions:

1. **Mechanical-template surface on faq_miss escalation** — same template surface confirmed in cs_001, cs_002, cs_011, cs_014, cs_038, cs_040, and cs_015 (turn-0 same template phrasing). cs_192 shares this surface.

2. **L3 judge over-reaches on form_context first_name** — confirmed in cs_038 (Paul), cs_040 (Mo), and now cs_192 (Rita). Three instances reinforce `R-l3-judge-form-context-trust-rubric` is a systematic judge_calibration issue, not per-case.

3. **Potential UC-specific corpus gap on basic policy questions** — cs_095 (UC-D account/email) and cs_192 (UC-B free items / giveaway) both show "bot tries FAQ search, doesn't find a usable answer, falls back to faq_miss escalate." A corpus-coverage audit per UC is a recurring backlog candidate.

## Which layer is likely responsible?

Four candidate layers:

- **`prompt_projection`** (primary, shared with the mechanical-template pattern). Same template surface as cs_001 / cs_011 — the prompt likely directs the bot to a fixed-template fallback on faq_miss without distinguishing "no FAQ found but UC-B is generally bot-resolvable" from "no FAQ found and we should hand off." The 100% reliability of the template across cases confirms prompt-side, not LLM-decision-side variance.

- **`semantic_planner`** (secondary). Even with adequate projection, the LLM had `correct_outcome=resolve` as the expected destination per CaseSpec / phase 2 UC-B policy. The LLM chose to escalate instead. With 2 searches yielding no usable hit, the planner's options were (a) attempt a more general FAQ answer from priors, (b) honestly admit the gap, or (c) escalate. It chose escalate. This is a planner-side resolve-vs-escalate decision failure on UC-B.

- **`corpus`** (tertiary, conditional). If the FAQ corpus has UC-B giveaway / free-items content that the bot's queries didn't surface, this is a retrieval-quality issue (query formulation). If the corpus doesn't have such content, this is a corpus gap. Either way, a corpus check is warranted — see Related observation.

- **`eval_spec`** (judge calibration, ongoing). Same form-context-trust over-reach as cs_038 / cs_040, separate from the bot-side failure.

## What should NOT be done?

- Do **not** add a regex / keyword on "free items" / "give away" / "giveaway" → force-route to UC-B resolve template. Keyword chatbot regression Constitution §1.5.
- Do **not** add a Java guard forcing UC-B to never escalate on faq_miss. UC-B can legitimately faq_miss in genuine cases (e.g. an obscure policy question with no article); the right surface for "should we resolve harder or escalate" is the planner's judgment with adequate projection, not a hard runtime gate.
- Do **not** synthesize a fake UC-B "free items / giveaway" FAQ article to close the gap. Same point as cs_095: corpus additions should be genuine content-team material, not generated-to-pass entries.
- Do **not** edit cs_192's CaseSpec to accept `escalate` as a passing outcome. The CaseSpec correctly classifies this as resolvable; loosening it would mask both the bot template failure AND the corpus / retrieval question.
- Do **not** count "no prior mention of 'Rita'" against the bot. `form_context.first_name="Rita"`. Same as cs_038 / cs_040.

## Related observation — four R-items, all reinforcing existing pattern

cs_192 produces no genuinely new R-items, only reinforces existing ones:

1. **`R-l3-judge-form-context-trust-rubric`** (cs_038 + cs_040 + now cs_192). Three confirmed instances across UC-J / UC-K / UC-B — clearly systematic. Reinforces the judge_calibration backlog item; no new R-item needed.

2. **`R-faqMissCount-threshold-and-timing-review`** (cs_095 first surfaced; cs_192 reinforces). cs_192 ran 2 searches before hitting faq_miss — fits the "auto-search + 1 user message exhausts the threshold" pattern from cs_095's analysis. No new R-item; this is additional evidence for the existing one.

3. **`R-corpus-coverage-audit-per-uc`** (broadened from cs_095's `R-corpus-uc-d-account-faq-gap`). cs_095 surfaced a UC-D account/email corpus gap; cs_192 surfaces a *potential* UC-B free-items/giveaway corpus gap. Two UCs with similar shape suggests a per-UC corpus coverage audit (does each UC have resolve-grade articles for its common entry-point questions?) is warranted. **Replaces** the cs_095-specific UC-D R-item with a broadened version. cs_095's brief has been retroactively updated to reference this broader R-item.

4. **`R-cs192-secondary-ucs-duplicate-uc-b`** (eval_spec, low priority). The CaseSpec's `secondary_ucs: [UC-K, UC-D, UC-B]` lists UC-B both as primary and as secondary. Likely a generator quirk. Not blocking; flag for Wave A5/A6 L3 review during G1 packaging.
