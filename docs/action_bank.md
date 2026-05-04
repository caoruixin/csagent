# Action Bank

Date: 2026-05-04 (post Sprint 1)

## Status

### A1. Deterministic EscalationReasonResolver — **DONE** (2026-05-04)

Implemented in `EscalationReasonResolver` (Spring service) + integrated
into `ControlKernel` and `PhaseEvaluator`. Full precedence table over
the canonical 23-value enum. Explicit user / callback request beats
budget reasons. Tool call, session state, and handover payload now
agree (gate `escalation_reason_consistency` passes 0/14 across all
three follow-up runs).

Target case status:

- `cs_interactive_002`: still fails (needs distress detector — see
  Next Action #1 in handoff).
- `cs_interactive_014`: still fails (same as above).
- `cs_interactive_029`: consistency gate now passes; failure mode is
  a separate `CONTRACT_VIOLATION:active_use_case` because topic is
  UNKNOWN and the explicit-user detection forces escalation before
  any UC commits. Sprint A1 contract is still met (the semantic
  reason is now `user_requested`, not the budget reason).

### A2. UC-K technical regression routing — **DONE** (2026-05-04)

Implemented in `UseCaseRouter.matchUcKTechnicalRegression`. Pre-LLM
deterministic override that fires only on regression keywords
(disappeared / used to / greyed out / app crashes / error when /
"not getting THE option"). UC-E generic FAQ ("how do contact options
work?") is preserved.

Target case status:

- `cs_interactive_066`: **passes consistently in all three runs**
  (composite 0.82–0.89), `active_use_case=UC-K`,
  `escalation_reason=intake_complete_for_uc_k`.

Negative-regression coverage: cs_095 stays on UC-A / UC-D LLM
classification (not over-routed to UC-K).

### A3. Server-side handover payload assembler — **DONE** (2026-05-04)

Implemented in `HandoverPayloadAssembler` (Spring service). Replaces
inline payload building in `SessionManager.recordHandover`.

Required-field coverage:

- user issue ✅ (`user_issue` field)
- detected UC ✅ (`primary_use_case` + label inside summary)
- escalation reason ✅ (canonicalised via resolver)
- collected identifiers ✅ (`identifiers_collected`)
- relevant tools / sources / status checks ✅
  (`status_checks_performed`, `knowledge_tools_invoked`, `articles_shown`)
- partial answer or clear blocker ✅
  (`partial_answer_or_blocker` derived from tool digest + reason hint)
- unresolved question ✅ (`unresolved_question`, last user message
  with form-description fallback)
- issue-specific summary ✅ — assembler always quotes content tokens
  from the form description (cannot collapse to "User needs help.")

Also added: `recordHandover` is now invoked when the auto-search
path itself escalates at session-create time (cs_192-style cases).

Target case status:

- `cs_interactive_066`: handover_completeness=1.0, summary mentions
  "phone", "contact", "option" tokens.
- `cs_interactive_095`: assembler runs, but the bot's outcome
  mismatch (escalate vs expected resolve) is a routing problem, not
  a payload problem — out of A3 scope.
- `cs_interactive_259`: handover persists; correct_uc / correct_outcome
  failures are routing problems, out of A3 scope.

## Carry-over deferrals (unchanged)

- D1. Full trace / transcript alignment gate
- D2. Turn-0 factual claim classifier
- D3. Full service-outcome taxonomy
- D4. Large production smoke case expansion
- D5. Tool error / timeout cases
- D6. Full semantic groundedness classifier
- D7. Complete GDPR / moderation / payment / scam / out-of-scope eval suite

## New deferred items emerging from Sprint 1

- D8. **Distress / frustration detector.** Resolver precedence already
  prefers `user_distress` over budget reasons; runtime needs a signal
  source. Multi-turn frustration / ALL-CAPS / repeated-complaint
  detection. Required to flip cs_002 / cs_014.
- D9. **Pre-LLM routing bias for messaging / login keywords.** UC-B /
  UC-C / UC-F flips between runs on cs_001 / cs_002 / cs_014. A small
  deterministic rule matching account / login / notification keywords
  would stabilise routing.
- D10. **Soft-OOS + early-escalation interaction.** When the user
  requests a callback while the topic is UNKNOWN, the session escapes
  before any UC is committed and the trace contract fails on
  `active_use_case`. Either commit a default UC for soft-OOS or defer
  forced escalation until after the first DISCOVER turn.

## Rule (carry-over)

If a finding is not directly related to A1, A2, A3, or one of the
explicitly named deferred items above, do not implement it without an
updated sprint scope.
