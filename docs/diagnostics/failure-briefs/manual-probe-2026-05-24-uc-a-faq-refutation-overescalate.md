# Failure Brief — manual-probe 2026-05-24 UC-A FAQ-refutation over-escalate

> **Source:** Manual probe trace, captured 2026-05-24 ~20:24 BST (driven during the M5 S2 per-invocation-trace eyeball).
> **Trace ID:** `33369b62-b5a…` (session_id; partial — full trace in the admin trace + the human's paste).
> **CaseSpec:** N/A (manual probe; no rule-extracted CaseSpec, no L3 override).
> **Run:** single-session manual probe; not part of any eval set.
> **Filed:** 2026-05-24 by deliver-agent + human (per `iteration_governance.md` §2 — human labels expected behaviour; deliver-agent labels layer hypothesis + do-not-do list).
> **Routing decision (2026-05-24):** does NOT fit M5 (observability-only). Routes to a future **semantic-planner / escalation-evidence-contract / moderation-context** milestone. Research pass before encoding (Path 2).

## ⚠️ Coverage check — this is a RE-SURFACING, not a novel case

This trace is a **4th instance** of the documented systematic pattern, not a new failure shape. Strong overlap with:

- **`manual-probe-2026-05-13-ad-visibility-multi-layer-failure.md`** — near-identical shape: UC-A "can't see advert" → `request_handover(faq_miss_threshold_exceeded)` stamped while `search_knowledge` returned viable hits with `faq_miss: false`; the SAME three source_ids (`ka4P200000004ZtIAI` "I Can't Find My Ad", `ka4P…bIAA` "Where Is My Ad?", `ka41r000000LIEJAA4` "(temp) Ad removed - By CS"); the SAME mechanical template ("I'm having difficulty resolving this. Let me connect you with a specialist.").
- **`R-escalation-reason-runtime-evidence-contract-review`** (`docs/action_bank.md` §5) — now **4 instances** across UCs/reasons: cs040 (`intake_complete_for_uc_k`) + cs176 (`faq_miss_threshold_exceeded`) + the 2026-05-13 probe + this. The prior brief already called this "solidly systematic … strengthens the Tier-0 candidate case."
- **`R-runtime-orchestrator-tool-call-deduplication`** (`docs/action_bank.md` §5) — possibly reinforced (T3 ran `search_knowledge` twice with identical hits), though here it may be agentic-loop re-search after a failed `record_outcome` rather than the 2026-05-13 "3× identical for ONE LLM request" orchestrator bug — **research must disambiguate**.

## What happened?

3-turn UC-A session:

- **T1** "hi I can't find my advert" → bot asks a clarifying question (DISCOVER→DISCOVER). Reasonable.
- **T2** "it's an advert I've posted last week." → bot classifies UC-A, `search_knowledge` (3 hits, `faq_miss: false`), `resolve_article(ka4P200000004ZtIAI)`, grounded expiry/policy answer citing the source (DISCOVER→RESOLVE). **Correct FAQ handling.**
- **T3** "but I can see it yesterday" → the bot's first invocation reasons "it may have been removed by our moderation team … Let me check the details" + `search_knowledge`. The loop then runs: `get_customer_context` (account_found=false / listing_found=false — probe ad not in store), `search_knowledge` (same 3 hits, `faq_miss: false`), **`record_outcome(resolve)` → ERROR** (`[REDACTED_TOKEN]`), `search_knowledge` again (same 3 hits), `request_handover(faq_miss_threshold_exceeded)`. Final output: the mechanical "I'm having difficulty resolving this. Let me connect you with a specialist." (RESOLVE→ESCALATE).

## What should a good CS agent have done?

The user's "I could see it yesterday" **refutes expiry** (the ad is ~6 days old) and points to a **recent moderation removal**. A good agent should pivot: ground a moderation-removal answer on the hits it already had (`ka41r000000LIEJAA4` "(temp) Ad removed" + `ka4P…bIAA` "Where Is My Ad?" moderation-process article) — explain removal reasons, check My Gumtree > Manage My Ads for inactive/removed status + removal emails — and/or ask for the ad ID to check status. **Resolve or progress; do not escalate on `faq_miss_threshold_exceeded` when `faq_miss: false`.**

## Why does this matter?

Over-escalation / wrong-containment of a resolvable FAQ-path issue, handed to a specialist. Violates §1.3 (escalation posture is LLM-owned; here escalation fired on a *mechanical* threshold contradicted by the runtime evidence the bot received). Reinforces the §5.1 over-escalation acceptance bar and the `R-escalation-reason-runtime-evidence-contract-review` Tier-0-candidate gap (now 4 instances).

## Is this a one-off or a pattern?

**Pattern** — 4th instance of `faq_miss_threshold_exceeded`-despite-`faq_miss:false` over-escalation. NEW wrinkles vs the 2026-05-13 probe: (a) a **`record_outcome(resolve)` runtime ERROR** mid-turn (the 2026-05-13 trace had no record_outcome error); (b) the **FAQ-refutation pivot** — the user supplied new info ("saw it yesterday") that should re-route to moderation grounding, which the bot named in its own reasoning but did not act on.

## Which layer is likely responsible?

Same stack as the 2026-05-13 brief; research to disambiguate:

- **`semantic_planner`** (primary): escalated despite viable hits + its own moderation reasoning; did not pivot to moderation grounding on the "saw it yesterday" signal.
- **`prompt_projection`** (secondary, and the M5/S3 tie-in): the FAQ Skill declares `moderation_context` as a `required_context_key` but `ContextProjectionBuilder` **never emits it** (the gap S3's C1 audit will document) — so the bot likely could not *see* moderation status to ground a confident answer. Also the mechanical-template surface.
- **`infra` / `skill_state`**: the `record_outcome(resolve)` ERROR; and the faq_miss counter / threshold logic — **why did `faq_miss_threshold_exceeded` fire when every search reported `faq_miss: false`?** (the central anomaly, shared with 2026-05-13).

## What should NOT be done?

- Do **not** add a Java guard refusing `request_handover(faq_miss_threshold_exceeded)` when `faq_miss==false` as a per-case patch — route it through `R-escalation-reason-runtime-evidence-contract-review` (the runtime-evidence escalation-contract review; possible Tier-0, needs human decision per §3.2 Q2 / `human_review_required`).
- Do **not** add a regex on "saw it yesterday" / "can't find advert" → moderation. Keyword-chatbot regression (§1.7).
- Do **not** globally lower the faq_miss threshold as a hack.
- The `moderation_context` projection fix is a **future semantic milestone**, not M5 (observability-only); M5/S3 only *audits + documents* the gap.

## Open questions for the research pass (scoped — this is reinforcement, not from-scratch)

1. **Root cause of `faq_miss_threshold_exceeded` firing despite `faq_miss: false`** — is the reason LLM-stamped (semantic_planner) or runtime-stamped via a counter? Does the counter increment on the `record_outcome` error or repeated searches rather than on actual faq_miss?
2. **The `record_outcome(resolve)` ERROR** — real infra/tool bug, or a redaction artifact? Does it trigger a fallback escalation path?
3. **Dup `search_knowledge`** — same `R-runtime-orchestrator-tool-call-deduplication` bug as 2026-05-13, or legitimate agentic-loop re-search after the failed `record_outcome`?
4. **`moderation_context` projection gap** as a contributing root cause (ties to S3 C1) — would projecting it have let the bot ground instead of escalate?
5. **Promotion assessment**: do 4 instances now cross the bar to scope `R-escalation-reason-runtime-evidence-contract-review` (+ the moderation-context projection) into the next semantic milestone candidate?

Related: [[manual-probe-2026-05-13-ad-visibility-multi-layer-failure]] · [[project-faq-overescalate-maxsteps-misstamp]].

## Update 2026-05-24 (session d4b90e96 + Explore investigation) — root cause refined to RUNTIME budget gates

A second manual probe (session `d4b90e96-afcb-4412-aa7f-7bae46e41616`, model `deepseek-v4-flash`, captured ~21:56) reproduced a WORSE sibling of the same pattern, and a code investigation pinned the mechanism. This **refines the layer hypothesis: primarily `infra`/runtime, NOT `semantic_planner`** (the LLM does not self-stamp the reason — turn 4 had "No LLM call for this turn").

**Session d4b90e96:** the bot over-clarified in DISCOVER for 2 turns ("can't see my ad" → "i can see yesterday" → re-asked the SAME why-or-appeal disambiguation), never committed a UC or searched; on turn 4 ("ad-1001") the runtime force-escalated with `turn_budget_exhausted` and no LLM call.

**Two runtime force-escalation vectors (both unguarded by the Sprint 39 guardrail):**

| Vector | Where | When | Reason stamped | This brief's instances |
|---|---|---|---|---|
| **Pre-LLM session-budget gate** | `BudgetChecker.checkBudgets()` (`BudgetChecker.java:32-81`) called in `ControlKernel.processMessage()` (~`:279`); reason map ~`:663`/`:296` | BEFORE the LLM runs, on `clarificationCount`/`totalBotTurns`/`faqMissCount`/`repeatedActionCount` limits (`control-policy.yaml` `max-clarification-rounds: 2`) | `clarification_budget_exhausted` / `turn_budget_exhausted` (catch-all) / `faq_miss_threshold_exceeded` | **d4b90e96** (clarificationCount≥2 → force-escalate, no LLM call) |
| **Post-loop MAX_STEPS heuristic** | `PhaseEvaluator.resolveMaxStepsReason()` (`PhaseEvaluator.java:173-196`) | AFTER the RESOLVE agentic loop hits `maxToolSteps` | heuristic: `incomplete_intake` > `clarification_budget` > `faq_miss_threshold_exceeded` > `turn_budget_exhausted` fallback | **33369b62 / 2026-05-13** (RESOLVE loop exhausted → `faq_miss_threshold_exceeded` despite `faq_miss:false`) |

**Reason mis-labeling:** even the reason NAME is imprecise — d4b90e96 was a *clarification*-budget exhaustion but surfaced as the `turn_budget_exhausted` catch-all; the RESOLVE-loop vector stamps `faq_miss_threshold_exceeded` when `faq_miss:false`. The runtime mislabels WHY it escalated.

**Sprint 39 guardrail gap (confirmed):** `SkillGuardrailDispatcher.checkBeforeDispatch()` is **tool-dispatch-scoped** — it only rejects LLM-issued `request_handover(reason)` calls. It covers NEITHER the pre-LLM `BudgetChecker` nor the post-loop `resolveMaxStepsReason` runtime vectors. (Matches memory `project-faq-overescalate-maxsteps-misstamp`: "Sprint 39 only closed the LLM vector.")

**No S2 regression:** the budget/escalation logic (`BudgetChecker`, `PhaseEvaluator.resolveMaxStepsReason`) is NOT in any Sprint 51 / M5 S2-modified file; S2's `ControlKernel`/`AgentRunLoopImpl` edits are observation-only and the budget check at `ControlKernel.java:~279` is unchanged. The worse behavior is pre-existing runtime + a weaker-model (`deepseek-v4-flash`) over-clarification accelerant, not S2.

**Solution direction (for the research proposal / future semantic milestone — NOT M5):**
1. **Reason precision (low-risk `infra`):** order the specific budget checks before the `turn_budget_exhausted` catch-all so the stamped reason is semantically correct; never stamp `faq_miss_threshold_exceeded` when `faq_miss:false`.
2. **Architectural crux (human decision):** the runtime *mechanically force-escalates* on budget exhaustion — but Constitution §1.3 says **escalation posture is LLM-owned**, while §1.4 says **budgets are Runtime-owned**. The LLM-first fix is to *project budget pressure as a soft signal* and let the LLM own escalate-vs-continue, rather than a runtime hard gate. This §1.3-vs-§1.4 boundary call needs the human + the evidence-contract review (possible Tier-0); do NOT patch per-reason in Java without it (per this brief's "what NOT to do").
3. **Over-clarification (`semantic_planner`/model + guidance):** the model re-asked the same disambiguation instead of committing on added signal; the DISCOVER guidance (`discover_triage.yaml`, confidence ≥0.5 commit) is soft/LLM-owned. Sharper commit-on-added-signal guidance and/or a stronger model — not a hard rule.
