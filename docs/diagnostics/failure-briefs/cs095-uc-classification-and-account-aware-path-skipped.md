# Failure Brief — cs095 UC classification mismatch and account-aware path skipped

> **Source case:** `cs_interactive_095`.
> **Source session:** `570Q5000008U5C9IAK`.
> **CaseSpec:** `eval_interactive/case_specs/smoke/cs_interactive_095.yaml`.
> **Approved L3 override:** Wave A2.1 reclassification (2026-04-30, legacy migration, `migrated_from_legacy: true`; classification block only — pins `primary_uc=UC-A, secondary_ucs=[UC-D, UC-K]`).
> **PRD / Eval classification (per human review):** UC-D (disagrees with CaseSpec override — see Ground-truth chain).
> **Runs:** `20260505-235231` (FAIL, composite=0.0, L1:no_stall + STALL:PLACEHOLDER_WITHOUT_FOLLOWUP, L3:tone=2.0). 2026-05-10 regressed further to TIMEOUT — Cluster C, deferred via `R-smoke-regression-investigation`.
> **Filed:** 2026-05-12 (Sprint 18 / G1, brief #6).

## Ground-truth chain

cs_095 has an approved L3 override (Wave A2.1, legacy migration). **The override carries only a classification block, no `expected` or `persona` block.** Wave A2.1 rationale: "Wrong email, no adverts showing -- UC-A with UC-D / UC-K fallback." Override pins UC-A primary.

**Classification disagreement surfaced by current review.** Human re-review classifies cs_095 as **UC-D primary** (account / email), citing the PRD and Eval classifications both as UC-D. The dominant user signal is "I have the wrong email address on my app account"; the "no adverts showing" signal is a *downstream consequence* of the email mismatch, not a UC-A Top-Ad-visibility query.

This is a **substantive eval_spec layer concern** — current CaseSpec override is misaligned with PRD/Eval classification. The Wave A2.1 override is `migrated_from_legacy: true` with empty `supporting_turn_numbers`; the migration may have inherited a UC-A classification from before PRD/Eval converged on UC-D. A Wave A5/A6 L3 re-review is warranted. Track in `docs/action_bank.md` as `R-cs095-uc-classification-l3-rereview` (eval_spec; potentially leads to a new approved override pinning UC-D primary).

**Either way, the bot's actual behaviour is informative.** The bot stamped `active_use_case=UC-A` (matching the current override) but gave an answer that *reads as UC-D* (steps to change the contact email). The bot's projection / planner is internally inconsistent — UC label says UC-A, content says UC-D. This adds an independent prompt_projection layer signal.

Other ground-truth facts:
- `outcome_class: resolve`, `should_escalate: false`, `allow_bot_resolution: 'true'`
- Expected tool sequence: `get_customer_context → search_knowledge → resolve_article → record_outcome`
- `grounding_mode: faq_source_backed`
- UC-A is on the phase 2 line 358 allowed list for `get_customer_context` (no CaseSpec/phase 2 conflict on that tool)

## What happened?

User entered via the Account Support form: `form_context.first_name="Trish"`, description "I think I have the wrong email address on my app account so am not getting messages to the app and it's telling me I have no adverts..  I cant see where it can be changed, can you help", empty ad_id, persona `frustration_level: high`, `drift_behavior: soft_shift`. The 2-turn transcript:

- **T0 user** (form): "I think I have the wrong email address on my app account so am not getting messages to the app and it's telling me I have no adverts..  I cant see where it can be changed, can you help"
- **T0 bot:** "Hi Trish! Hi Trish, you can update your contact email on the website by going to the user menu → My Details → Contact Details → Edit, then enter your new email and click Change Primary. You'll need to confirm via a link sent to that new address. Note that your login email stays the same as the one you registered with. Source: ka44J000000gKxqQAE"
- **T1 user:** "Thank you. Would the wrong email address on the app perhaps not allow syncing but it was working before"
- **T1 bot:** "I'm looking into this for you."

Tool sequence actually called: `search_knowledge → resolve_article → resolve_article` (3 calls). **`get_customer_context` — the first tool in the CaseSpec's expected sequence — was not called.**

Five stacked failures (in causal order, not severity order):

1. **UC routing / account-aware path skipped (root failure).** The bot never called `get_customer_context`, so it never verified whether an account with the user's email actually exists, whether ads are linked to that account, or whether the user is logged into the app with a different email. Instead the bot ran a generic FAQ search → returned a generic "how to change contact email" article. The user's *actual* problem (probable wrong-account login causing the "no adverts" signal) cannot be solved by generic FAQ steps; it requires an account-state check.

2. **Generic FAQ answer instead of account-aware investigation.** Even if `get_customer_context` was unavailable for some reason, the bot's T0 answer should have asked for the account-identifying email or ad_id before launching into the change-email procedure. The bot leapfrogged the discovery phase.

3. **Duplicated greeting in T0** ("Hi Trish! Hi Trish"). Projection / template rendering bug.

4. **Internal source ID exposed as user-visible citation** in T0 ("Source: ka44J000000gKxqQAE"). Sprint 14 §L0 introduced `canonical_url` and `safe_to_show` flags on resolved articles; the bot's user-facing answer is not gated to prefer `canonical_url` (or article title) over the internal SF `source_id`.

5. **STALL on T1 follow-up.** When the user asked the follow-up ("would the wrong email perhaps not allow syncing but it was working before"), the bot replied "I'm looking into this for you." and the session ended. L1 `no_stall` hard-failed with `STALL:PLACEHOLDER_WITHOUT_FOLLOWUP`, driving composite to 0.0 despite `correct_outcome=1.0` and `correct_uc=1.0`.

## What should a good CS agent have done?

The capability gap is **account-state-aware investigation** for symptom sets where the user describes an account / login / sync / visibility problem that may stem from a wrong-account or wrong-email login state, rather than a generic policy or configuration question.

Concretely:

- **Recognize account-state-flavored symptoms.** The combination "wrong email on app + no ads showing + no messages arriving" should anchor on UC-D account state, not on UC-A Top-Ad visibility or a generic "how to change email" FAQ. The bot's T0 trace shows it picked the generic policy path.

- **Verify account state before answering the surface symptom.** The CaseSpec's `expected_tool_sequence` puts `get_customer_context` first; phase 2 UC-D guidance reinforces this; the bot skipped it. Generic FAQ procedural steps returned without verifying state cannot solve a state mismatch.

- **Name the verified state back to the user in plain language.** Once state is known, the response should be tied to it (a specific account ↔ login email mismatch named explicitly) rather than a generic procedure. The current answer is content-portable to any user with any state — that's what makes it generic.

- **Sustain the conversation past T0.** When the user asks a follow-up sub-question on T1, any of {real answer tied to verified state, honest non-answer, escalate} is acceptable; a placeholder with no follow-up is L1 stall.

- **Surface choices.** Greeting rendered once; citations use `canonical_url` or article title rather than internal Salesforce IDs (Sprint 14 §L0 already paved this path).

Each is a separate sub-capability; together they define "a good CS agent on a UC-D-flavored account-state case".

## Why does this matter?

cs_095 is currently the most informative case in the smoke set on **multiple layers stacking on a single failure**. Per the human reviewer's analysis, three systemic UC-D-flavored causes compound:

1. **Knowledge base gap on UC-D account/email coverage.** UC-D is account/login-flavored; the FAQ corpus does not carry resolve-grade articles for "wrong email on app vs website" scenarios. Every FAQ search on this surface misses. cs_095 escaped a faqMissCount-driven escalation only because at least one resolve_article returned a tangentially-relevant article (the contact-email change steps), but that article doesn't solve the user's actual problem.

2. **`faqMissCount >= 2` threshold and timing.** The auto-search on turn 0 consumes one of the two allowed misses. The user's first explicit message exhausts the second. Combined with the KB gap above, this means UC-D account cases are structurally prone to faq_miss escalation on T1. The threshold is also checked *before* the form-description fallback contributes context, so even good form-supplied signals come too late to influence the early-stage tool routing.

3. **The account-aware path (`get_customer_context` first) is never taken.** Even though CaseSpec lists it as the first expected tool, the bot's runtime path skips straight to `search_knowledge` / `resolve_article`. This may be a projection issue (the bot's plan doesn't realize the UC requires identifier collection first) or a planner issue (the bot's UC-A interpretation deprioritizes account context retrieval).

The cs_095 failure is therefore not a single bug but a **layered failure across corpus, runtime config, and prompt/planning**. Any fix targeting one layer in isolation would leave the others producing the same surface on similar UC-D cases. This is the strongest evidence in the smoke set that the system needs **layered diagnosis before single-layer remediation** — exactly what `iteration_governance.md` §3 Fix Layer Classification was designed to enforce.

cs_095 also reveals the **first multi-turn skill_state failure** in the smoke set: even when the bot reaches *some* answer on T0, it cannot sustain into T1. The Cluster B briefs all fail at T0; cs_095 is the first case to fail past T0. G2 case-family construction should intentionally include multi-turn-followup cases on FAQ-resolve UCs to exercise this surface.

## Is this a one-off or a pattern?

`pattern` on multiple dimensions, but `under-observed` on each individually:

- **UC-D account/email-flavored cases with KB gap**: cs_095 is the canonical example in the smoke set. Other UC-D cases (cs_011 login-issue) had different shape (no FAQ resolve attempt). Real-traffic volume for "wrong email on account causing missing data" is likely substantial — UC-D-01 case_volume is significant per phase 2.
- **Bot skipping `get_customer_context` despite CaseSpec expectation**: also seen in cs_001 (UC-C) and cs_011 (UC-D), though those are confounded by phase 2 line 358 restricting the tool for those UCs. cs_095's UC-A allows the tool; the bot still skipped it — cleaner evidence of a projection / planner issue independent of the policy conflict.
- **PLACEHOLDER_WITHOUT_FOLLOWUP STALL**: cs_095 (05-05) is the only smoke set example. cs_014's 05-10 regression run shows the same surface. Under-sampled.
- **Duplicated greeting / internal SF ID citation**: only surfaced because cs_095 was the only smoke case to reach a "first grounded answer" state on T0.

## Which layer is likely responsible?

Five candidate layers, each addressing a distinct failure dimension. §3.2 first-match-wins resolves to one primary, but cs_095 is genuinely multi-layered:

- **`eval_spec` / `product_policy` (classification mismatch)** — current CaseSpec override pins UC-A; PRD and Eval classify as UC-D. Wave A5/A6 L3 re-review needed before downstream layer analysis is fully trustworthy.
- **`prompt_projection`** — bot stamped `active_use_case=UC-A` but produced a UC-D-flavored answer. Projection of UC label vs UC content is inconsistent. Also responsible for the duplicated greeting and the internal-ID citation choice.
- **`semantic_planner`** — bot skipped `get_customer_context` despite CaseSpec listing it first. Even with adequate projection, the planner chose a FAQ-first path. Also chose "I'm looking into this for you" placeholder on T1 instead of a real answer or honest non-answer.
- **`skill_state`** — T1 follow-up handling broken; session state after T0 did not enable a continued conversation.
- **`corpus`** — UC-D account/email FAQ coverage gap. Genuine corpus issue, not a runtime bug.

`runtime config` (faqMissCount threshold + timing) is a separate concern surfaced by the human reviewer. It's not in the §3 layer set (which covers semantic surfaces), but it is a real system-level issue. Treat as a `config governance` finding adjacent to the layer hypothesis.

## What should NOT be done?

- Do **not** add a regex / keyword on "wrong email" / "no adverts showing" → force-route to UC-D. Keyword chatbot regression.
- Do **not** lift the faqMissCount threshold blindly. The threshold exists to prevent unbounded retry loops; raising it without addressing the timing issue (checked before form-description fallback) just shifts the failure surface.
- Do **not** fill the UC-D corpus gap by writing fake FAQ articles that paraphrase the human reviewer's expected resolve path ("ask for email → check account → log-out advice"). The fix for the resolve path lives in the bot's planning / projection, not in the corpus. The corpus gap is real, but the right corpus addition is genuine help-center material on the "wrong email on app" scenario, written by content / product, not synthesized to make the bot pass.
- Do **not** add a Java guard requiring `get_customer_context` to be called before `search_knowledge` on UC-A or UC-D. Too rigid for the cases where the user explicitly asks a generic policy question that doesn't need account state.
- Do **not** edit cs_095's CaseSpec or override to lower the L1 stall hard check, change `outcome_class` to escalate, or accept the current bot's behaviour as PASS. The override deserves an L3 re-review for UC classification, but not a downgrade of the failure bar.

## Related observation — five R-items to track

cs_095 surfaces an unusually high number of distinct backlog items because the failure stacks across multiple layers:

1. **`R-cs095-uc-classification-l3-rereview`** (eval_spec). Wave A5/A6 L3 re-review: should `primary_uc` be UC-D (per PRD / Eval per human reviewer) rather than UC-A (per Wave A2.1 legacy migration)? This is the single most impactful follow-up; the rest of the layer analysis depends on the correct UC.

2. **`R-corpus-coverage-audit-per-uc`** (corpus, broadened — see cs_192 brief for the second case confirming this pattern). cs_095 surfaced a UC-D account/email FAQ coverage gap; cs_192 surfaces a potential UC-B free-items/giveaway gap. Per-UC corpus coverage audit (does each UC have resolve-grade articles for its common entry-point questions?). Content / product to write genuine help-center articles for confirmed gaps — NOT generator-synthesized articles.

3. **`R-faqMissCount-threshold-and-timing-review`** (runtime config / governance). Two-part: (a) is `>= 2` the right threshold given that auto-search burns one, and (b) the timing of the threshold check vs the form-description fallback. Belongs in the next config-governance sprint candidate alongside Sprint 15-style work, not a runtime semantic change.

4. **`R-l1-source-citation-quality-rubric`** (eval_spec). L1 `source_citation_present` check accepts internal SF IDs as valid citations. Should be tightened to require canonical_url OR article title.

5. **`R-g2-multi-turn-followup-case-family-design`** (deliver / eval governance, G2 input). G2 case-family construction should intentionally include multi-turn-followup cases on FAQ-resolve UCs to exercise the skill_state surface.

The first three are the substantive findings from the human reviewer's analysis. The last two were surfaced during this brief's drafting. All are independent of the bot-side L3-quality + L1-stall failures the brief identifies.
