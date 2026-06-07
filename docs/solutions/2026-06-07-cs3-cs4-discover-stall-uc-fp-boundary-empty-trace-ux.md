---
title: CS3 DISCOVER placeholder stall + CS4 UC-A entity-context verification gap + CS2-new empty-trace UX + cross-UC continuity findings
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-06-07
mode: bad-case-driven
supersedes: []
superseded_by: null
notes: >
  Follow-up to docs/solutions/2026-06-07-cs1-default-resolved-cs2-user-role-projection-gap.md.
  Three additional cases surfaced by human 2026-06-07:

    - **CS2-new** (trace 587137c7-cd9…) — admin trace renders "No
      trace steps recorded" on a session that is "Bot Handling".
      Code-grounded finding: this is architectural reality (session
      created, no first user message processed yet), NOT a code
      defect. UX-only ambiguity. Captured here as §1 with a small
      observability suggestion; no sub-sprint warranted.
    - **CS3** (trace 33edc1eb-15d…) — DISCOVER turn 2 emits the
      auto-filled placeholder "I'm looking into this for you."
      (runtime injection at AgentRunLoopImpl.java:438-441 when LLM
      emits empty user_message + no tool_calls) → R2.a counter
      increments → turn 3 hits clarification budget cap before the
      LLM is invoked. User's polite "Thank you" never reaches the
      LLM; runtime force-escalates. §2.
    - **CS4** (trace c891efb0-481…) — Removed-kittens-ad case.
      **REFRAMED 2026-06-07 after follow-up human question**: the
      root cause is NOT primarily "UC-A vs UC-FP mis-classification"
      — it is the **upstream UC-A entity-context verification gap**.
      Even if the LLM had picked UC-FP, no `ad_id` was in form_context
      so the moderation_reason would still have been unavailable. The
      missing procedure step is "before answering about a specific
      ad, verify the entity context (ask for ad_id OR call
      get_customer_context / lookup_listing_or_ad)" — the OBS-S1
      backlog item, now promoted to active. UC-FP boundary cue
      (OBS-S2) is downstream of this and folded into the same
      sub-sprint. §3 + §4 reworked.

  Cross-UC continuity findings added at §10 (new): mid-session UC
  switching is runtime-owned (DriftDetector + RuntimeIntentClassifier
  + RerouteDecider; LLM cannot re-call classify_use_case in RESOLVE/
  CONFIRM). Customer_context payload persists across UC switches via
  state_inheritance.inherit on the destination skill. But there is
  NO stash-and-resume: a UC-A → UC-B → UC-A sequence does NOT
  restore UC-A's prior phase/state. D-full-issue-ledger remains
  deferred. Flagged as a separate design conversation for human
  triage (NOT folded into this proposal's sub-sprint scope).

  Compounding with prior proposal: CS3's R2.a counter behaviour
  intersects with CS1's "default-resolved on CLOSE" — both touch
  ControlKernel exit paths; the two sub-sprints should be carefully
  sequenced. See §5.
---

# CS3 stall + CS4 UC-A/UC-FP boundary + CS2-new empty-trace UX

## 0. Executive summary

Three additional bad cases surfaced 2026-06-07. Findings:

- **CS2-new** is **not a defect**. A session created via the demo
  flow lands in `BOT_HANDLING` immediately (`SessionManager.java:101`);
  no turn rows are written until the first user message is processed
  (the only exception is the hard-OOS create-time synthetic turn at
  `:231-262`). A user who lands at the greeting screen and never
  sends the first message leaves the session in `BOT_HANDLING` with
  zero `bot_turns` rows. `TraceViewer.tsx:72-74` correctly renders
  the empty state. The user experience is confusing because "Bot
  Handling" sounds active. A minor admin UX hint (§1) is the only
  recommended change; no sub-sprint warranted.

- **CS3** is a multi-layer DISCOVER stall pathology. Root cause:
  - `AgentRunLoopImpl.java:438-441` auto-fills the bot reply with
    `"I'm looking into this for you."` when the LLM emits empty
    `user_message` AND zero `tool_calls`. The LLM almost certainly
    emitted a null turn; runtime synthesised the placeholder.
  - The R2.a DISCOVER counter at `:455-457` increments the
    clarification round on this synthesised placeholder, even
    though no actual clarification was made.
  - `BudgetChecker.java:30-37` caps at 2 (`control-policy.yaml:2`).
    After turn 2, count=2.
  - Turn 3 hits the budget gate at `ControlKernel.java:288-318`
    BEFORE the LLM is invoked. Runtime force-escalates with the
    injected "I've reached the limit…" text. The user's "Thank you"
    on turn 3 was never seen by the LLM.
  - Two layers, two narrow fixes: (a) make the synthesised
    placeholder turn distinguishable from a real clarification so
    R2.a doesn't count it against the user; (b) discourage the LLM
    from emitting null turns by tightening the DISCOVER procedure.

- **CS4** is **primarily a UC-A entity-context verification gap**;
  the UC-A vs UC-FP boundary is a downstream symptom. **Reframed
  2026-06-07** after follow-up human question: "even when the
  agent picks UC-A, it still needs to know which specific ad
  the user is talking about". Root cause (multi-layer, upstream
  to downstream):
  - **Primary — `prompt_projection` / `semantic_planner`
    upstream gap**: `resolve_faq_grounded_answer.yaml` has NO
    UC-A critical step that mandates entity-context verification
    before `search_knowledge`. UC-A's `tools_required` (`:13-18`)
    lists `get_customer_context` (so the tool is reachable) but
    procedure / grounding_instruction (`:30-31`) is permissive,
    not prescriptive ("If tool data contains specific
    information…answer from that first" — presupposes data is
    there). DISCOVER procedure (`discover_triage.yaml`) doesn't
    instruct the LLM to ask for `ad_id` either. Result: when
    form_context has no `ad_id`, the auto-trigger at
    `FormContextIngestionService.java:93-142` runs
    `get_customer_context` with email only (no listing fetched)
    → `customer_context_status="missing_ad_id"` is projected as
    observable state but NOT enforcing. Bot proceeds to
    search_knowledge and answers from generic FAQ corpus.
  - **Secondary — UC-A vs UC-FP boundary cue gap**: even when
    the entity context IS available (ad_id present + moderation
    review fetched), `discover_triage.yaml:25-31` Sprint 33 cue
    distinguishes UC-A (understand) vs UC-H (appeal), but does
    NOT distinguish UC-A (generic ad-status) vs UC-FP (correct-
    deletion explanation with moderation_review). The UC-FP
    critical step (`:56-71`) is `mandatory_for: UC-FP` only,
    gating the moderation-context fetch behind UC-FP being
    already selected (chicken-and-egg).
  - **Tertiary — projection ergonomics**: `candidate_use_cases`
    (`ContextProjectionBuilder.java:499-508`) emits bare UC IDs
    without human-readable names — LLM has to remember from
    training what each UC means (also surfaces the CS2-original
    UC-G label hallucination sub-issue).
  - **Tertiary — no answer-repetition bucket**:
    `BudgetChecker.java` has no near-duplicate-answer-text
    bucket — CS4 turns 3-5 repeated the same "pets listing fee"
    answer verbatim and nothing caught it.
  - **Both PRIMARY + SECONDARY map to existing OBS-S1 + OBS-S2
    backlog** (autoloop semantic surfaces deferred post-M-Auto-6).
    OBS-S1's exact name is "UC-A / UC-H / UC-J verify-entity-
    context procedure" — that's the primary fix. CS4 is the
    evidence trigger to promote both. OBS-S1 is more
    consequential than OBS-S2 for CS4-shape cases: even if
    UC-FP were picked, no ad_id means no moderation_reason.

  See §10 for cross-UC continuity findings surfaced by the same
  follow-up human question (separate design conversation, not
  folded into the CS4 sub-sprint).

## 1. CS2-new (587137c7) — Empty trace UX

### 1.1 Current state (code-grounded; HEAD `auto-loop-branch`)

- `SessionManager.createSession()` at
  `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java:74-301`
  initialises every session with
  `handlingState="BOT_HANDLING"` and `currentPhase="INIT"`
  (`:101-102`).
- The session is persisted at `:220`. Only the **hard-OOS**
  routing outcome writes a synthetic turn at create-time
  (`:231-254`). Soft-OOS / ROUTED / AMBIGUOUS sessions persist
  WITHOUT any turn rows.
- The first user message is processed asynchronously via
  `POST /v1/chat/sessions/{id}/messages` →
  `SessionManager.processMessage()` →
  `ControlKernel.processMessage()` (`SessionManager.java:144-150`
  comment: "session creation must NEVER block on an LLM call.
  The user now lands in the chat box immediately with a static
  greeting; their first message exercises the normal processMessage
  path").
- Admin trace fetch: `GET /v1/demo/sessions/{id}/trace` at
  `DemoInspectionController.java:135-138` →
  `LocalEventStore.getSessionTraceWithLlmCalls()` at
  `:65-74` → returns `bot_turns` rows.
- UI render: `ui/src/components/admin/TraceViewer.tsx:72-74`:
  ```tsx
  {trace.steps.length === 0 && (
      <div style={{ color: 'var(--color-text-muted)', padding: 16 }}>
          No trace steps recorded.
      </div>
  )}
  ```

### 1.2 Why this surfaces

The most likely scenarios that produce an empty-trace
`BOT_HANDLING` session:

1. **Lazy user path**: user opens the demo flow, fills the
   pre-chat form, lands at the greeting screen, but **never sends
   the first message**. Session is in DB with `total_bot_turns=0`
   indefinitely.
2. **Session created via API without a follow-up message**:
   programmatic creation (e.g. eval simulator setup, manual
   smoke testing) that doesn't fire the first message.
3. **First-turn processing crashes between
   `session.setTotalBotTurns(+1)` at `ControlKernel.java:248` and
   the `recordRunResult()` / `recordTurn()` calls at
   `:568` / `:709`**. The agent reported this is theoretically
   possible but every known code path has try-catch wrapping.
   Treated as edge case; not the primary CS2-new shape.

### 1.3 Recommendation

**This is not a code defect**; the runtime is behaving as
designed. The UX confusion is real, however: "Bot Handling" with
no trace steps reads to an operator like "the bot is stuck" or
"the trace is missing". Two minor improvements (NOT a new
sub-sprint; queue as small UI/UX follow-on):

- (UX, P3) admin `SessionList.tsx` could derive a distinct
  display label like "Awaiting first message" when
  `handling_state == BOT_HANDLING` AND `total_bot_turns == 0`.
  Pure UI; no backend touch. Routed to admin observability
  backlog or `R-admin-trace-observability-…` series under
  S-Auto-29+.
- (UX, P3) `TraceViewer.tsx:72-74` empty-state copy could read
  "No turns yet — this session was created but the user has not
  sent their first message." Single-line copy change.

Recommend folding into the existing `R-admin-trace-
observability-session-list` (S-Auto-24 / R3.a, dev-side closed)
follow-on or adding `R-admin-empty-trace-zero-turn-affordance`
as a P3 backlog item. Do NOT block on this.

### 1.4 Layer classification

`infra` (admin observability). No semantic decision involved.
Pure cosmetic UI improvement. §7-EXEMPT (UI-only, no semantic
surface touched).

## 2. CS3 (33edc1eb) — DISCOVER placeholder stall + budget exhaustion

### 2.1 Current state (code-grounded; HEAD `auto-loop-branch`)

**Smoking gun #1** — runtime auto-fill of the placeholder:

`server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java:438-441`:

```java
if (calls == null || calls.isEmpty()) {
    String finalText = (userMsg == null || userMsg.isBlank())
            ? "I'm looking into this for you."
            : userMsg;
    ...
```

When the LLM emits a structured reply with empty `user_message`
(or null) AND empty `tool_calls`, the runtime synthesises the
literal placeholder `"I'm looking into this for you."` as the
bot's outgoing reply.

The same string is present at `PhaseEvaluator.java:715`,
`ControlKernel.java:458 / :704`, `ActionParser.java:72`, and as
an example in `templates.yaml:28`. It is a long-standing fallback
across multiple code paths — NOT a new regression.

**Smoking gun #2** — R2.a counter counts the synthesised
placeholder:

`AgentRunLoopImpl.java:442-457` (S-Auto-23 R2.a):

```java
// R2.a #3 — DISCOVER free-text clarification counter, wired on
// the LIVE AgentRunLoopImpl path (the legacy PhaseEvaluator:880
// +1 site is unreachable here, so session.clarificationCount
// otherwise stays 0 forever and the BudgetChecker clarification
// cap is never reachable — c3). STRUCTURAL CARDINALITY ONLY: no
// content / similarity / Jaccard heuristic.
boolean ucCommittedThisTurn =
        !Objects.equals(activeUc, session.getActiveUseCase());
if (isDiscoverFreeTextClarification(
        plan.phase(), false, ucCommittedThisTurn, userMsg)) {
    session.setClarificationCount(session.getClarificationCount() + 1);
}
```

The counter increments when phase=DISCOVER AND `hasToolCalls=
false` AND `ucCommittedThisTurn=false` AND `userMsg` is non-
blank (per `:1206-1209`). The synthesised
`"I'm looking into this for you."` placeholder satisfies all
four — so the counter increments **even though the bot didn't
actually clarify anything**.

**Smoking gun #3** — budget cap and force-escalate before LLM:

`server/src/main/resources/config/control-policy.yaml:1-2`:

```yaml
budgets:
  max-clarification-rounds: 2
```

`BudgetChecker.java:30-37`:

```java
public Optional<String> checkBudgets(BotSession session) {
    if (session.getClarificationCount() >= controlPolicy.getMaxClarificationRounds()) {
        log.info("Session {}: clarification budget exceeded …");
        return Optional.of("max-clarification-rounds");
    }
    ...
```

`ControlKernel.java:288-318` (excerpt):

```java
// Step 3: Check budgets — if exceeded, force ESCALATE
Optional<String> exceededBudget = budgetChecker.checkBudgets(session);
if (exceededBudget.isPresent()) {
    applyEscalationReason(session, mapBudgetToEscalationReason(
            exceededBudget.get(), session.getCurrentPhase(),
            session.getLastAction(), session.getActiveUseCase()));
    return forceEscalate(session, phaseBefore, userMessage, startTime,
            "I've reached the limit of what I can assist with on this topic. " +
            "Let me connect you with a human agent who can help further.");
}
```

So when turn 3 arrives, the budget check runs BEFORE the LLM is
invoked. The LLM never sees the user's "Thank you" — runtime
force-escalates with the injected text. The escalation reason
maps to `clarification_budget_exhausted` via the S-Auto-25
R2.a#5-ext extension at `ControlKernel.java:782-794`.

### 2.2 Trace replay (CS3 = trace 33edc1eb)

| Turn | User | Bot reply (LLM or runtime) | Phase | Tools | Clarif count |
|------|------|----------------------------|-------|-------|---------------|
| 1 | "my ad has been put on hold for the second time. Please can you restore it?" | "Do you want to know the reason it was put on hold, or do you want to appeal …?" (LLM, Sprint 33 disambiguation cue) | DISCOVER→DISCOVER | 0 | 1 |
| 2 | "I would like to know the reason it was put on hold and have it restored. I've posted the same ad twice and both times it's been put on hold." | "I'm looking into this for you." (**runtime auto-fill — LLM emitted empty user_message + empty tool_calls**) | DISCOVER→DISCOVER | 0 | 2 |
| 3 | "Thank you. I appreciate your help." | "I've reached the limit of what I can assist with on this topic. Let me connect you with a human agent who can help further." (**runtime force-escalate, LLM not invoked**) | DISCOVER→ESCALATE | `request_handover(clarification_budget_exhausted)` | 2 (capped) |

The user's clear answer on turn 2 (compound: "know the reason
and have it restored") gave the LLM enough to commit a UC
(UC-FP for the reason, UC-H for the appeal, or one focused
follow-up question). Instead the LLM emitted a null reply — and
runtime turned that into a counted clarification. The system
then locked the budget gate before the LLM had any opportunity
to recover on turn 3.

### 2.3 Root-cause analysis (multi-layer per §3.2)

**Layer A — `infra` (R2.a counter coverage gap)**: the counter
increments on a runtime-synthesised placeholder. The LLM's null
turn is a planning failure, but the runtime amplifies it by
counting it as if the bot had legitimately asked a clarifying
question. The fix surface: distinguish "LLM emitted a legitimate
clarification" from "LLM emitted nothing and runtime filled in
the placeholder", and only count the former. This is `infra`
(persistence + counter logic), not semantic.

**Layer B — `semantic_planner` (LLM null-turn pattern)**: the
LLM emitted empty `user_message` + empty `tool_calls`. This is
semantically a "stall" pattern. The DISCOVER procedure
(`discover_triage.yaml:19-22`) does not explicitly forbid null
turns or "wait" filler text. The `allow_interim_message: false`
flag at `:14` is metadata-only (not runtime-enforced).

**Layer C — `prompt_projection` (budget edge guidance gap)**:
when the LLM sees `budgets.clarification: {used: 1, max: 2}` on
turn 2, the prompt offers no explicit guidance: "you're at the
edge; if intent is clear, commit; if not, escalate; do NOT emit a
null/filler reply". `system_prompt.txt` and
`discover_triage.yaml` both miss this.

**Layer D — `infra` (budget gate fires before LLM on turn 3)**:
the design at `ControlKernel.java:288-318` checks budgets BEFORE
giving the LLM a turn. This is correct for legitimate budget
exhaustion (LLM has already used its rounds), but it means a
SINGLE bad null-turn can lock out the LLM from any recovery.
Tunable but not a defect per se.

### 2.4 Design alternatives + recommended option

**Option C3.A — Fix the R2.a counter gating** (recommended;
narrow, anti-误杀 preserved)

Change `AgentRunLoopImpl.java:438-458` so the counter ONLY
increments when the LLM emitted a non-trivial reply, NOT when
runtime synthesises the placeholder. Concretely:

```java
if (calls == null || calls.isEmpty()) {
    boolean llmEmittedNullTurn = (userMsg == null || userMsg.isBlank());
    String finalText = llmEmittedNullTurn
            ? "I'm looking into this for you."
            : userMsg;
    boolean ucCommittedThisTurn =
            !Objects.equals(activeUc, session.getActiveUseCase());
    if (!llmEmittedNullTurn && isDiscoverFreeTextClarification(
            plan.phase(), false, ucCommittedThisTurn, userMsg)) {
        session.setClarificationCount(session.getClarificationCount() + 1);
    }
    // Soft observability: when LLM null-turns, emit a trace
    // event ("discover_null_turn_synthesised") so the gap is
    // visible without forcing escalation.
    ...
```

Anti-误杀: today's legitimate clarification (LLM emits "Do you
want to know the reason or appeal?") still increments. Only
the runtime-synthesised placeholder is excluded.

Trade-offs:
- (+) Surgical; smallest change. Restores the counter's
  intended semantic ("the bot asked a clarifying question") by
  not counting a non-clarification as one.
- (+) Anti-误杀 preserved; characterization tests easy to write.
- (–) Does NOT fix the LLM null-turn pattern itself (covered by
  Option C3.B). The bot still wastes a turn on the placeholder
  — but the budget isn't burned for it.

**Option C3.B — Tighten DISCOVER procedure against null turns**
(complementary; soft `semantic_planner` cue)

Add ONE soft sentence to `discover_triage.yaml:20` procedure:
> "Every DISCOVER turn MUST either (a) call `classify_use_case`,
  or (b) ask one focused clarifying question (`user_message`
  ending with `?` or otherwise inviting a response). Do NOT emit
  an empty `user_message`, an empty `tool_calls` array, or a
  placeholder filler like 'I'm looking into this for you' /
  'Just checking on this' — that wastes a turn and burns the
  clarification budget without giving the user anything to
  respond to."

Trade-offs:
- (+) §1.3 / §1.5 compliant: it's a soft cue, not a Java guard.
  LLM owns the semantic decision.
- (+) Forward-looking: 6-month future LLMs will internalise this
  as good CS-agent behaviour.
- (–) Soft cue; can be ignored. C3.A's runtime gating is the
  hard backstop.

**Option C3.C — Raise the budget cap or add an off-ramp**

Bump `max-clarification-rounds` from 2 to 3, OR add a "recovery
turn" where after the budget cap fires once, the LLM is given
ONE explicit "summarise and close" turn before the runtime
force-escalates.

Trade-offs:
- (+) More user-friendly; rare cases of legitimate triple
  clarification don't auto-fail.
- (–) Treats the symptom (budget tight) without addressing the
  root cause (null-turn waste). Generalises poorly: the cap was
  set at 2 for a reason (Sprint 19 + later calibration).
- (–) Pushes a config tuning into the middle of a runtime
  semantic shape change. Out of scope here.

**Recommendation**: ship Option C3.A + C3.B together as a small
sub-sprint. Defer C3.C unless future bad-case evidence shows
the 2-round cap is too tight even after C3.A + C3.B land.

## 3. CS4 (c891efb0) — UC-A vs UC-FP boundary + missing moderation context + repetitive FAQ

### 3.1 Current state (code-grounded)

**UC-A vs UC-FP registry definitions**:
`server/src/main/resources/config/use-case-registry.yaml:2-43`:

```yaml
UC-A:
  name: "Ad Status & Visibility"
  topic-subjects: ["Ad Support"]
  risk-level: LOW
  allow-bot-resolution: true
  path: FAQ

UC-FP:
  name: "Correct Deletion Explanation"
  topic-subjects: ["Ad Support"]
  risk-level: MEDIUM
  allow-bot-resolution: true
  path: FAQ
```

Both map to `Ad Support` topic. Both are FAQ-path. UC-A is the
generic "explain ad status" surface; UC-FP is the specialised
"explain WHY a deletion happened" surface that requires consulting
the moderation context.

**Sprint 33 disambiguation cue** at
`server/src/main/resources/skills/discover_triage.yaml:25-31`
(inline in procedure):

> "A user asking why the ad is gone, what happened to it, or
  where it went is asking to UNDERSTAND the situation
  (FAQ-resolvable, classify toward the visibility / ad-status
  explanation UC). A user asking to appeal, contest, or reverse
  the removal is asking to ACT (the appeal UC, an intake path)."

This distinguishes UC-A (understand) from UC-H (appeal). **It
does NOT distinguish UC-A (generic status) from UC-FP (correct
moderation-reason explanation).**

**UC-FP critical step** (mandatory_for: UC-FP only) at
`discover_triage.yaml:56-71`:

```yaml
- id: removed-listing-route-with-moderation-context
  desc: |
    When the form context indicates a removed listing …
    the moderation context is the canonical reason metadata for
    deciding between a correct-deletion explanation (UC-FP) and
    an appeal intake (UC-H). Surface the moderation context in
    the trace before committing the use case so the route is
    anchored in the actual reason, not on the listing-status
    flag alone.
  trace_check: "accumulated_tool_results.get_moderation_review_context"
  mandatory_for:
    - UC-FP
```

This critical step fires only when the LLM has already committed
UC-FP — chicken-and-egg: the cue that should drive UC-FP
selection is gated behind UC-FP being already selected.

**RESOLVE UC-FP critical step** at
`server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:113-127`:

```yaml
- id: consult-moderation-context-on-removal-explanation
  desc: |
    For UC-FP (Correct Deletion Explanation), the moderation
    context is the canonical reason metadata … Without the
    moderation context, a UC-FP factual reply about the removal
    reason crosses the grounding floor …
  trace_check: "accumulated_tool_results.get_moderation_review_context"
  mandatory_for:
    - UC-FP
  severity: mandatory
```

Same `mandatory_for: [UC-FP]`. Once UC-A is committed, this
mandatory step is silently inapplicable; the LLM proceeds with
generic FAQ search.

**`candidate_use_cases` projection** (bare IDs only) at
`ContextProjectionBuilder.java:499-508`:

```java
if (skillRequiresContextKey(session, activeUc, "candidate_use_cases")) {
    ArrayNode candidateUcsNode = objectMapper.createArrayNode();
    if (session.getCandidateUseCases() != null) {
        for (String uc : session.getCandidateUseCases()) {
            if (uc != null && !uc.isBlank()) {
                candidateUcsNode.add(uc);
            }
        }
    }
    projection.set("candidate_use_cases", candidateUcsNode);
}
```

The LLM sees `["UC-A", "UC-FP", "UC-H", ...]` — bare IDs with
no human-readable names. To know UC-FP means "Correct Deletion
Explanation", the LLM relies on training memory + the procedure
text. The procedure text doesn't define UC-FP. UC-A "Ad Status
& Visibility" is the more familiar / generic-sounding label.

**`get_moderation_review_context` is RUNTIME-ONLY** — the bot
cannot call it directly. The tool fires automatically inside
specific skill flows. Per the Explore agent, the tool's
moderation_review data is included in the response when
listing.status is "removed"/"moderated".

**`get_customer_context` tool policy** at
`server/src/main/resources/config/tool-policy.yaml:8-10`:

```yaml
get_customer_context:
  type: AGENT_VISIBLE
  allowed-ucs: [UC-A, UC-C, UC-D, UC-F, UC-FP, UC-K]
```

`get_customer_context` IS allowed in UC-A. But:
- `GetCustomerContextTool.java:54-59` only adds `moderation_review`
  to the response when listing status is "removed"/"moderated"
  — and even then it's a top-level field, NOT projected by
  default into the per-turn projection.
- The sanitised listing struct at `:93-107` does NOT carry the
  moderation reason directly.
- The UC-A skill procedure does NOT instruct the bot to call
  `get_customer_context` before answering. There's no soft cue
  saying "for removed/suspended ad questions, fetch
  customer_context FIRST".

**BudgetChecker** has no near-duplicate-answer-text bucket
(`BudgetChecker.java:30-84`). Buckets cover:
`max-clarification-rounds`, `max-faq-miss`, `max-bot-turns-…`,
`max-repeated-same-action`, `max-total-bot-turns`. Repetition
detection is at the action level (same tool call repeated), not
at the response-text level. CS4 turns 3-5 emitted near-verbatim
"pets need listing fee" answers and nothing triggered.

### 3.2 Trace replay (CS4 = trace c891efb0)

| Turn | User | Bot action | Bot reply summary | Phase |
|------|------|------------|-------------------|-------|
| 1 | "I posted an ad for kittens I'm trying to rehome and it was removed. Can you tell me why?" | (DISCOVER clarifier per Sprint 33 cue) | "Do you want to know the reason it was removed, or would you like to appeal the removal?" | DISCOVER→DISCOVER |
| 2 | "I want to know why it was removed and what I can do to fix it." | `classify_use_case(UC-A)` → `search_knowledge` → `resolve_article(My Ad was Removed)` | Cites "Paying to Rehome Your Pet" policy; suggests user check email | DISCOVER→RESOLVE (UC-A committed) |
| 3 | "I checked my email and didn't find anything about the policy. Can you please tell me what policy I violated?" | `resolve_article(Paying to Rehome Your Pet)` + `search_knowledge` | Repeats "pets need listing fee" answer | RESOLVE→RESOLVE |
| 4 | "I'm not a breeder, I just want to find a good home for my kittens. I didn't know about any listing fee. Can you explain what I need to do?" | (no tool calls) | Repeats "pay the fee" answer | RESOLVE→RESOLVE |
| 5 | "I didn't know about this policy. I just want to find a good home for my kittens. Can you help me understand what I need to do?" | `resolve_article(Paying to Rehome Your Pet)` | Repeats "pay the fee" answer | RESOLVE→RESOLVE |
| 6 | "I'm frustrated. Can I speak to someone?" | `request_handover(user_requested)` | "let me connect you with a human agent right away" | RESOLVE→ESCALATE |

Three compounding gaps:

1. **UC mis-classification** (UC-A picked when UC-FP was the
   right route). The Sprint 33 disambiguation cue handled UC-A
   vs UC-H correctly but never offered UC-FP as a candidate
   framing.
2. **Moderation context never fetched**. The bot guessed the
   ad's removal reason (pets-need-fee) from the FAQ corpus
   instead of consulting the actual moderation reason. The
   guess might be wrong — the ad could have been removed for an
   image-quality issue, duplicate-listing, or unrelated policy.
   The user has no way to know.
3. **Repetitive non-progressing FAQ answers**. Turns 3-5
   restated the same policy. No runtime budget caught it
   because BudgetChecker only checks action-level repetition.

### 3.3 Root-cause analysis (multi-layer per §3.2)

**Reframed 2026-06-07** after follow-up human question. Ordered
from upstream (primary) to downstream (tertiary).

**Layer A (PRIMARY) — `semantic_planner` + `prompt_projection`
UC-A entity-context verification gap (OBS-S1 surface)**: there
is NO procedure step in `resolve_faq_grounded_answer.yaml` that
mandates UC-A to verify the user's specific ad before
delivering a knowledge-base answer. The skill `tools_required`
at `:13-18` lists `get_customer_context` (so the tool is
available), but:

- The procedure (`:30`) and grounding_instruction (`:31`) are
  permissive ("If tool data contains specific information about
  the user's case (account/ad/moderation), answer from that
  first") — presuppose data is there, don't enforce a fetch.
- The mandatory-for-UC-A critical step is only
  `search-knowledge-before-faq-answer` (`:52-65`,
  `mandatory_for: [UC-A, ...]`). No `verify-entity-context`
  step exists.
- The DISCOVER procedure (`discover_triage.yaml`) does not tell
  the LLM to ask for `ad_id` as a precondition. Confidence
  guidance mentions "ad ID, error message, action they tried"
  as confidence-supporting details, but as a threshold parameter
  — NOT as an elicitation directive.
- `FormContextIngestionService.java:93-142` auto-trigger fires
  `get_customer_context` pre-chat only when form has BOTH email
  AND ad_id (`:108-110`). CS4-shape sessions where the user
  types "I posted an ad for kittens" without providing the
  ad_id in form → listing context is never fetched → projection
  emits `customer_context_status: missing_ad_id` as an
  observable state, but it's purely observational; LLM may
  ignore it and proceed.
- `docs/proposals/uc_h_intake_lockin_after_uc_a_misclass.md`
  §4 ("Coverage check") already names this gap as
  "backlog 里没有专门针对 UC-A 的 verify entity-context
  R-item" — i.e. a known but uncovered gap.

This is OBS-S1's exact surface. Promoting it to an active
R-item is the primary CS4 fix.

**Layer B (SECONDARY) — `prompt_projection` UC-A vs UC-FP
boundary cue gap (OBS-S2 surface)**: the Sprint 33 cue language
("understand → visibility / ad-status explanation UC")
naturally maps to UC-A in the LLM's interpretation; UC-FP is
not contrastively named. This is a *cue completeness* gap in
`discover_triage.yaml:20-71`. Crucially, this layer is
SECONDARY because even if UC-FP had been picked, without
entity context (no ad_id, no listing fetch) the moderation
review would still have been unavailable — Layer A is the
binding constraint.

**Layer C (TERTIARY — projection ergonomics) — UC list as
bare IDs, no human names**: the LLM sees `["UC-A", "UC-FP",
"UC-H", …]` without descriptions
(`ContextProjectionBuilder.java:499-508`). UC-FP's existence is
essentially invisible at decision time. Same gap surfaced for
CS2-original (UC-G label hallucination).

**Layer D (TERTIARY — observability) — no answer-repetition
bucket**: when the bot's user_message text is near-duplicative
across turns (CS4 turns 3-5), there's no bucket in
`BudgetChecker.java:30-84` and no projection slot warning the
LLM. Not a primary driver; a backstop diagnostic for sessions
where the LLM falls into an answer-loop after Layer A fails.

**Layer E (PRE-EXISTING BACKLOG) — OBS-S1 + OBS-S2 already
exist in the M-Auto-6 backlog**. Per
`docs/action_bank.md` and `docs/sprints/sprint-078-handoff.md`:

- **OBS-S1**: "UC-A / UC-H / UC-J verify-entity-context
  procedure" — autoloop semantic sub-sprint. Deferred post-
  M-Auto-6. **THIS IS LAYER A**.
- **OBS-S2**: "DISCOVER disambiguation cue" — refining the
  Sprint 33 cue. Deferred post-M-Auto-6. **THIS IS LAYER B**.

CS4 is the exact evidence trigger to promote OBS-S1 + OBS-S2
to active R-items, with OBS-S1 as the primary lever.

### 3.4 Design alternatives + recommended option

**Option C4.A (REWRITTEN AGAIN 2026-06-07 after human directive
"treat entity-context verification as the primary fix; LLM-soft
procedure + prompt_projection support; do NOT implement a Java
hard guard that blocks search_knowledge globally") — Autoloop-
driven semantic optimization, NOT a hand-written critical step.**

**Critical reframing — why this is autoloop's job, not the
human's**:

`docs/action_bank.md` already names OBS-S1 as an "**Autoloop
semantic sub-sprint** targeting OBS-S1 (UC-A / UC-H / UC-J
verify-entity-context procedure)". The original design intent
was always that the procedural fix is discovered + proposed by
autoloop against a CaseSpec that pins the expected behaviour,
NOT hand-written by a research-agent or deliver-agent. The
human-side scope is:

1. Build the **prompt_projection infrastructure** the autoloop
   can reference (the projection slots autoloop will cite in
   its proposed procedure changes must already exist).
2. **Author the CaseSpecs** that operationalize the four
   acceptance criteria, with L2 outcome_checks that pin the
   tool-call ordering / message-shape expectations.
3. Run autoloop and **review the proposed procedure changes**
   under §4.1 anti-hardcode kernel + §1.7 forbidden-list.
4. **Merge + re-bless**.

This split honours the Constitution: §1.3 LLM ownership of
semantic decisions, §1.4 Runtime ownership of projection /
capability boundary, §1.5 no-keyword-fix iteration rule.

**Part A — Projection infrastructure** (human-directed; lands
FIRST):

A.1. **Extend `discover_disambiguation_signals`** at
`ContextProjectionBuilder.java`:

```json
"discover_disambiguation_signals": {
  "ad_status_observed": "REMOVED",
  "moderation_reason_available": true,
  "topic_subject_carries_multiple_candidate_ucs": true,
  "candidate_ucs_for_topic": ["UC-A", "UC-FP", "UC-H"]
}
```

The `moderation_reason_available` boolean is data-driven (set
true when `ListingLookupService` returned a non-null moderation
review). No keyword matching.

A.2. **`candidate_use_cases` with human-readable names**:

```json
"candidate_use_cases": [
  {"id": "UC-A", "name": "Ad Status & Visibility"},
  {"id": "UC-FP", "name": "Correct Deletion Explanation"},
  {"id": "UC-H", "name": "Ad Removal Appeal"}
]
```

Sourced from `UseCaseRegistryService.getUseCase(id).name()`.
This shared infrastructure also benefits CS2-original (round 1)
which depends on the same projection.

A.3. **`customer_context_status` is already in place** (Sprint
078 R4.a, `ContextProjectionBuilder.java:1383-1449`). No
infrastructure work needed — autoloop can reference it directly.

A.4. **Update `discover_triage.yaml` and
`resolve_faq_grounded_answer.yaml`** `soft_signal_via_projection`
and `required_context_keys` declarations to include the new
slots so the projection actually surfaces them on the relevant
phase / UC tuples. **NO procedure text change in Part A** —
that's autoloop's job in Part C.

**Part B — CaseSpec authoring** (human/research-directed; lands
SECOND):

Author CaseSpecs that operationalize the four acceptance
criteria. **Expected-behaviour framing (reframed 2026-06-07 at
human direction)**: the L2 outcome_check shape pins **"must
verify entity context: lookup if sufficient identifiers exist,
otherwise ask for ad_id"** — NOT "must call
`get_customer_context`". This framing is LLM-first: the LLM
chooses HOW to verify (call a lookup tool, ask the user, or
proceed if the context is already loaded), and the outcome_check
accepts any of these valid shapes. A `get_customer_context`-only
pin would reduce the LLM's autonomy and force a specific tool
when another path may be equally valid.

Suggested seed set (≥4, target + neighbor + negative-control
coverage):

| CaseSpec id | Shape | Pins (L2 outcome_check shape — "verify entity context") | Acceptance criterion |
|-------------|-------|---------------------------------------------------------|----------------------|
| `cs_uc_a_no_ad_id_specific_question` | form: no ad_id; open: "my ad isn't showing up in search" | Bot MUST verify entity context BEFORE `search_knowledge`. PASS if EITHER (a) first non-classify tool call is a listing lookup (`get_customer_context` or `lookup_listing_or_ad`), OR (b) first bot user_message ends with `?` AND references ad identification (asking for ad_id / ad title / ad URL / "which ad"). FAIL if `search_knowledge` precedes any verification attempt. | **#1 + #4** |
| `cs_uc_a_generic_policy_question` | form: no ad_id; open: "how does Gumtree's search ranking work? what makes ads appear higher?" | Bot is NOT required to elicit ad_id or call a listing lookup — the question is genuinely generic. PASS if `search_knowledge` runs directly and bot delivers a grounded generic answer. FAIL if bot demands ad_id when the question references no specific ad. | **#2 anti-误杀** |
| `cs_uc_a_loaded_listing` | form: ad_id + listing loaded (customer_context already populated by pre-chat auto-trigger); open: "why isn't my ad performing well?" | Bot MUST use the loaded customer_context BEFORE FAQ. PASS if bot's user_message references listing-specific data (title, status, category) OR the trace shows customer_context inspected before / instead of `search_knowledge`. FAIL if bot proceeds with generic FAQ without referencing the loaded listing. | **#3** |
| `cs_uc_fp_loaded_moderation` | form: ad_id + listing status=removed + moderation_review available; open: "why was my ad removed?" | Bot MUST use the available moderation context (verify-via-already-loaded path). PASS if bot's user_message references moderation_reason content OR the trace shows moderation_review consulted; UC-FP classification preferred over UC-A. FAIL if bot ignores moderation_review and cites a generic policy article. | **#3 (UC-FP boundary)** |
| `cs_uc_a_lookup_failed` | form: ad_id provided + lookup returns 404; open: "I can't see my ad anywhere" | Bot MUST verify-via-graceful-degradation. PASS if bot acknowledges lookup state AND offers a fallback (re-confirm ad_id / generic guidance / escalation); the LLM owns which fallback. FAIL if bot silently proceeds with generic FAQ as if the verification succeeded. | **#4 degradation path** |

Three notes on the "verify entity context" framing:

1. **The verb is "verify", not "call get_customer_context"**.
   Verification can succeed via: (a) calling a listing lookup
   tool, (b) asking the user for the ad reference, OR (c)
   reading already-loaded customer_context. The outcome_check
   accepts any of these three shapes per CaseSpec context.
2. **"Sufficient identifiers"** means whatever the listing
   lookup tool needs to succeed (today: ad_id + email per
   `GetCustomerContextTool`). When sufficient identifiers
   exist in form_context, the LLM SHOULD prefer the lookup
   path over asking the user (it's a better experience). When
   they don't, asking is the right path. The CaseSpec
   shape and the projected `customer_context_status` enum
   together make this distinction observable.
3. **Generic questions opt out**. The negative-control
   CaseSpec pins that genuinely-generic questions (no
   reference to a specific ad) do NOT require verification.
   This is the anti-误杀 binding gate for autoloop's Part C
   output — any procedure candidate that forces ad_id
   elicitation on the generic case fails this CaseSpec and
   is rejected.

Each CaseSpec goes through §5.6 bad-case suite tiering review;
human-side ground-truth label + judge rubric specified per
`docs/current/process/badcase-lifecycle.md`.

**Full YAML drafts for these CaseSpecs (5 NEW + 2 EXTEND diffs)
are in the companion appendix**:
[`docs/solutions/2026-06-08-cs4-casespec-drafts-appendix.md`](
2026-06-08-cs4-casespec-drafts-appendix.md). The appendix is
copy-pasteable into `eval_interactive/case_specs/bad_cases/`
at S-Y-CS4 Part B launch by the dev-agent. Schema-capability
constraints (only `correct_uc`, `correct_outcome`,
`tool_sequence_match` are reliable outcome_checks today; OR/
text-shape pins NOT supported) are reflected via Path γ: strict
`expected_tool_sequence` + `bot_handling_pattern` rubric +
`forbidden_tools` + `answer_must_not_contain` + L3 judge.

**Part C — Autoloop semantic optimization** (autoloop-driven;
lands THIRD):

C.1. **Baseline run** — execute the seed CaseSpec set against
the current `m-auto-6-baseline-…` (post-M-Auto-6 close):
expectation is that the 4 target CaseSpecs fail (target /
neighbor patterns) and the negative-control passes (anti-误杀
preserved on generic question).

C.2. **Autoloop semantic loop** — autoloop proposes skill
procedure modifications. The expected shape is roughly the
critical-step content the earlier proposal version drafted, but
**autoloop authors it, not the research-agent**:

- expected target: `resolve_faq_grounded_answer.yaml` procedure
  text OR a new critical step block;
- expected reference targets: `customer_context_status` (Part
  A.3 already present) + `moderation_reason_available` (Part
  A.1 new) + `candidate_use_cases[].name` (Part A.2 new);
- §1.7 forbidden-list anti-hardcode: no per-UC if-else cascade;
  no keyword check on user_message content; no enum widening;
  no Java guard.

C.3. **Human review** of every autoloop-proposed change per
§4.1 nine-question anti-hardcode kernel. Reject any candidate
that introduces semantic hardcode; iterate. The autoloop
output is treated as a **proposal**, not a binding decision
(per `docs/teams/research-agent.md` general guidance: "把
proposal 当作绑定决定 …" is the wrong shape).

C.4. **Merge accepted candidate**. Re-run the CaseSpec set —
expectation is that 4 target CaseSpecs pass while the
negative-control still passes.

**Part D — Re-bless + sub-sprint close** (human + deliver-
agent):

D.1. Mini re-bless against the curated bad-case suite + anchor
sentinel.

D.2. Codex per-sub-sprint review (§4.1 kernel — semantic
sub-sprint REQUIRES Codex review).

D.3. M-Auto-7 sub-sprint close.

**Pre-requisite spike (suggested for deliver-agent before
launching Part C)**: a 1-day spike to verify autoloop's current
capability to author skill-yaml procedure modifications (vs.
prompt-only candidates). M-Auto-3 / M-Auto-4 autoloop primarily
operated on prompt-side candidates; skill-yaml procedure as a
target surface needs confirmation. If autoloop today cannot
target skill-yaml procedure, Part C.0 inserts a small autoloop
tooling sprint to add the capability before Part C.1 launches.

**Trade-offs vs the (rejected) hand-written critical-step
approach**:

- (+) Honours `action_bank.md` original design intent for
  OBS-S1 (autoloop sub-sprint).
- (+) §1.3 / §1.7 cleaner: research-agent doesn't author the
  semantic decision text; autoloop discovers what the LLM
  needs and the human reviews it against the kernel.
- (+) Forward-looking: every future bad-case-pattern in the
  same shape (entity-context gap on a different UC family,
  cross-tool ordering, etc.) can follow the same template
  (CaseSpec → autoloop → review → merge), not "research-agent
  writes another critical step".
- (+) Acceptance criteria (#1-#4) translate directly to L2
  outcome_check shape — operationalizable.
- (–) Larger total surface: Part A (infra) ~2-3 days; Part B
  (CaseSpec authoring + tiering review) ~2-3 days; Part C
  (autoloop run + review iterations) ~3-5 days; Part D
  (re-bless + Codex) ~1-2 days. Total ~8-13 days.
- (–) Depends on autoloop capability (Part C.0 spike pending).
  If autoloop can't target skill yaml procedure today, this
  sub-sprint blocks on a tooling prerequisite.
- (–) Anti-误杀 still possible at autoloop output: the
  generic UC-A question case must remain passing post-merge.
  Human review at C.3 is the binding gate.

**Fallback alternative (NOT PREFERRED; documented for the case
autoloop cannot target skill yaml)**: an earlier version of
this proposal had a hand-written
`verify-entity-context-before-ad-specific-answer` critical step
authored directly into `resolve_faq_grounded_answer.yaml`. This
path is **not preferred** for OBS-S1-shape gaps because:

- The autoloop is the original `action_bank.md`-designated
  authority for OBS-S1 semantic procedure text;
- Hand-written procedure text encodes ONE research-agent's
  interpretation of the failure shape; autoloop discovers
  multiple candidates and is judged against the CaseSpec
  outcome rather than against the author's intuition;
- Future similar gaps (OBS-S6 / OBS-S7 / other entity-context-
  shaped surfaces) benefit from the same template (CaseSpec →
  autoloop → review) — hand-writing each one doesn't scale.

It remains a **valid fallback if autoloop cannot target skill
yaml procedure today** (Part C blocked) and the milestone
cannot wait for an autoloop tooling sprint. In that case:
research-agent / deliver-agent authors the critical step text,
runs the same CaseSpec set in Part B against the manual change,
human reviews under §4.1 + §1.7 kernel, merge + re-bless. The
acceptance criteria and CaseSpec L2 outcome_checks are
unchanged; only the AUTHOR of the procedure-text candidate
changes.

**Autoloop capability spike resolved 2026-06-07** — verdict
**READY** (see §3.5 below). The fallback is therefore not
expected to be exercised on CS4, but is preserved here for
audit trail and for similar future cases where the spike may
return NEEDS-TOOLING-SPRINT.

### 3.5 Autoloop capability spike — RESOLVED READY 2026-06-07

A code-grounded survey of `autoloop/` at HEAD on branch
`auto-loop-branch` confirms autoloop can today target skill
yaml procedure text without code changes. Key evidence
(citations verified at HEAD):

- **Mutable surface locked at 6 skill yaml files × 4 field
  paths** (`autoloop/config.yaml:12-30`,
  `autoloop/program.md §2 lines 40-78`):
  - Allowed files include `discover_triage.yaml`,
    `resolve_faq_grounded_answer.yaml`, and 4 other skill
    yamls — covering all surfaces in scope for CS4.
  - Allowed field paths: `$.procedure`,
    `$.grounding_instruction`, `$.escalation_policy`,
    `$.critical_steps[*].desc`.
- **Full pipeline implemented and live**:
  - Proposer at `autoloop/autoloop/meta_agent/proposer.py:44-72,
    92-94, 171-179` reads allowed skill yamls from disk and
    presents them inline to the LLM; `Hypothesis` dataclass
    represents a single skill-yaml field edit with
    `target_skill_file`, `target_field_path`, `before_value`,
    `after_value`.
  - Proposer prompt at
    `autoloop/autoloop/meta_agent/prompts/propose.txt:22-25`
    explicitly lists the 4 allowed field paths.
  - Applier at `autoloop/autoloop/sandbox/applier.py:358-378`
    parses the target yaml, writes the new field value,
    round-trips back to text; `:152, :168-171` writes to
    disk + git commits to an `autoloop/exp-N` branch.
  - Validator at
    `autoloop/autoloop/sandbox/yaml_diff_validator.py:66-72,
    111-117, 149-150` enforces the locked surface — diffs
    outside the 6 files × 4 fields are rejected before
    application.
- **Live production history**:
  `autoloop/results/experiments.jsonl` records 65 iterations
  across M-Auto-4 + M-Auto-5 with target field distribution:
  17 on `$.procedure`, 16 on `$.escalation_policy`, 15 on
  `$.grounding_instruction`, 14 on
  `$.critical_steps[N].desc`. Autoloop has actively
  mutated procedure text at production cadence for ~6 weeks.
- **Anti-hardcode discipline** at
  `autoloop/autoloop/sandbox/anti_hardcode_check.py:1-26`
  enforces forbidden patterns within the proposed text
  (Q1 IF/THEN trees, Q2 MUST/NEVER invention, Q4 case_id
  tokens, Q5 LLM-shrinking language) regardless of which
  allowed field is edited.

**Implication for CS4 Part C**: no autoloop tooling sprint
needed. Part C can launch as soon as Part A (projection infra)
and Part B (CaseSpec authoring) land. The pre-condition gate in
§9 step 5c is satisfied by this spike.

**Out-of-scope notes** (for future planning, NOT this
milestone):
- Autoloop's locked surface does NOT include `system_prompt.txt`,
  Java code, projection-shape changes, or tool definitions.
  Part A's projection infra MUST be human-authored — autoloop
  cannot extend `discover_disambiguation_signals` itself.
- Autoloop's locked surface does NOT include `tools_required`
  in skill yaml — only the soft-semantic fields above. If a
  future bad-case requires adding a tool to a skill, that
  remains human-directed.
- Surface widening (e.g. to `tools_required`, system_prompt,
  or Java) requires a new milestone + new `program.md` v2 per
  `program.md §8` governance — deferred until evidence
  justifies.

**Option C4.B — Prompt-side rule cascade only**

Add to `discover_triage.yaml` procedure: "Always pick UC-FP
when the user mentions 'removed' AND has provided an ad_id."

Trade-offs:
- (+) Tiny change.
- (–) Pure §1.7 forbidden territory: encoding keyword rules
  ("mentions removed") into prompt is exactly what the
  Constitution forbids. Also brittle — fails on "taken down",
  "suspended", "deleted", "deactivated", etc.

**Option C4.C — Java guard refusing UC-A commit on removed ad**

Block `classify_use_case(UC-A)` when `ad_status_observed=
REMOVED`, forcing re-classification toward UC-FP.

Trade-offs:
- (–) §1.5 forbidden: this is a soft semantic decision the
  LLM owns. A Java guard here would be a hard rule on a Tier
  question, not a Tier-0 invariant.
- (–) Anti-误杀 hazard: legitimate UC-A "I can't see my live
  ad in search" sessions might have a stale REMOVED flag.

**Option C4.D — Answer-repetition observability**

Add a near-duplicate-answer-text signal in the projection (e.g.
`answer_repeated_in_prior_turn: true` via simple text similarity)
so the LLM sees the repetition and can pivot.

Trade-offs:
- (+) Useful observability hook for many future cases.
- (–) Text similarity is fuzzy; risk of false positives /
  negatives. Better as a follow-on sub-sprint after OBS-S1 +
  OBS-S2 land.
- (–) Different layer (`prompt_projection` / observability),
  not the primary CS4 driver.

**Recommendation**: ship Option C4.A as a single sub-sprint
(promotes OBS-S1 + OBS-S2 with one extension — moderation
reason availability + UC names — that benefits CS2 (round 1)
also). Queue Option C4.D as a follow-on. Do NOT do C4.B or
C4.C.

## 4. Layer classification + §7 stanza pre-fills

### 4.1 CS3 sub-sprint (S-X-CS3) §7 stanza draft

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** infra (R2.a counter), prompt_projection
(DISCOVER procedure soft cue against null turns)

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The
existing §1.4 Runtime ownership of the clarification-counter
contract (a clarification round = a clarification ATTEMPT by
the LLM) is restored by excluding runtime-synthesised
placeholders from the counter.

**Semantic hardcode:** No semantic hardcode introduced. The
R2.a counter gating change is a data-flow correction (synthesised
placeholders no longer count as LLM clarifications). The
DISCOVER procedure addition is a soft cue per §1.3; no Java
guard on user_message content. No new keyword, regex, or enum
expansion.

**Generalization coverage:** target / neighbor / negative /
shadow case counts: <T>/<N>/<G>/<S> — to fill at sub-sprint
open. Target: CS3 trace 33edc1eb + a curated set of "LLM null-
turn" traces from the bad-case suite. Neighbor: legitimate
clarification turns continue to count (anti-误杀: the Sprint
33 cue's two-turn clarification on uc_h_appeal still increments
counter). Negative: RESOLVE-phase clarifications still increment
on the RESOLVE-intake bucket (S-Auto-25 R2.a#5-ext invariant
preserved). Shadow: held-out DISCOVER null-turn cases.
```

### 4.2 CS4 sub-sprint (S-Y-CS4 = OBS-S1 + OBS-S2 promotion via autoloop) §7 stanza draft (REWRITTEN AGAIN 2026-06-07 after human directive)

The CS4 sub-sprint has four parts (Part A infra, Part B
CaseSpec, Part C autoloop, Part D close). The §7 stanza below
covers the WHOLE sub-sprint. Multi-layer prospective because
the autoloop output (Part C) is not pre-decided; the stanza
states the human-fenced layer ownership and the autoloop's
allowed surface, not the specific procedural text.

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** multi-layer prospective.
  - Part A (human-directed): prompt_projection
    (moderation_reason_available signal + candidate-UC human-
    readable names + soft_signal_via_projection declarations
    in discover_triage.yaml and resolve_faq_grounded_answer.yaml).
  - Part B (human-directed): eval_spec
    (CaseSpec authoring per §5.6 lifecycle; L2 outcome_checks
    pin tool-call ordering + bot-message shape).
  - Part C (autoloop-driven, human-reviewed): semantic_planner
    (the autoloop proposes skill-yaml procedure changes the LLM
    will follow; human review enforces §4.1 + §1.7).
  - Part D (human-directed): eval_spec + governance close.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant.
The existing §1.4 Runtime ownership of the projection contract
covers the new soft-signal slots (Part A). The §1.3 LLM
ownership of semantic decisions covers the autoloop output
text (Part C). **NO Java guard refusing UC-A `search_knowledge`
without ad_id** per explicit 2026-06-07 human directive.

**Semantic hardcode:** No semantic hardcode introduced.
  - Part A: `moderation_reason_available` is data-derived
    (listing lookup returned a non-null moderation review);
    candidate-UC name comes from `UseCaseRegistryService`. No
    keyword matching on user_message.
  - Part B: CaseSpec L2 outcome_checks pin trace-shape (tool-
    call ordering + bot-message shape), NOT user_message
    keyword regex.
  - Part C: autoloop's allowed surface is the skill-yaml
    procedure text. The §4.1 nine-question kernel + §1.7
    forbidden-list block: no per-UC if-else cascade, no
    keyword check on user_message content, no enum widening,
    no Java guard. Human reviewer rejects any candidate that
    introduces semantic hardcode.

**Generalization coverage:** target / neighbor / negative /
shadow case counts: <T>/<N>/<G>/<S> — to fill at Part B
CaseSpec authoring open. Seed plan:
  - Target (T≥3): `cs_uc_a_no_ad_id_specific_question`,
    `cs_uc_a_loaded_listing`, `cs_uc_a_lookup_failed`.
  - Neighbor (N≥1): `cs_uc_fp_loaded_moderation` (UC-FP
    boundary case that benefits from same projection infra).
  - Negative anti-误杀 (G≥1): `cs_uc_a_generic_policy_question`
    (the bot must NOT be forced to elicit ad_id on genuinely-
    generic questions). Anti-误杀 binding gate for autoloop
    output.
  - Shadow (S≥2): held-out variants on the same UC-A
    entity-context-gap pattern + held-out generic-UC-A pattern.

Acceptance criteria (per 2026-06-07 human directive):
  #1 ad-specific questions do not receive generic FAQ answers
     without listing context;
  #2 generic policy questions are not forced to provide ad_id;
  #3 loaded customer_context is used before FAQ;
  #4 missing_ad_id leads to elicitation or lookup, not generic
     resolution.

These translate directly to Part B L2 outcome_checks (see
§3.4 Part B table) and the autoloop's success criterion in
Part C.
```

## 5. Hard fences + non-goals

### CS3 sub-sprint specifically

- Do NOT raise the `max-clarification-rounds` cap. The cap of 2
  is well-calibrated; the issue is the counter wrongly
  incrementing on a synthesised placeholder, not the cap
  itself.
- Do NOT add a Java guard refusing empty `user_message + empty
  tool_calls`. The DISCOVER procedure soft cue is enough; a
  hard guard would be brittle (legitimate "summary close" turns
  may have empty `user_message`).
- Do NOT touch `templates.yaml` or `PhaseEvaluator` placeholder
  strings — the same `"I'm looking into this for you."` is used
  in legitimate slow-LLM path and clarification-recovery
  contexts. The fix is the counter gating, not the placeholder.
- Do NOT touch the budget gate at `ControlKernel.java:288-318`
  — the budget check belongs before the LLM call (correct on
  legitimate budget exhaustion).

### CS4 sub-sprint specifically

- Do NOT add a per-UC if-else on user_message content ("if
  mentions removed → UC-FP").
- Do NOT block `classify_use_case(UC-A)` via Java guard when
  ad is removed — UC-FP preference is a soft LLM decision.
- Do NOT add `moderation_reason_text` (the actual reason string)
  to the per-turn projection — that would leak the explanation
  to the LLM bypassing the `get_customer_context` tool boundary.
  Only the boolean `moderation_reason_available` is projected.
- Do NOT add an answer-repetition bucket in this sub-sprint
  (Option C4.D); defer.
- Do NOT migrate UC IDs to new strings or rename UC-FP. The
  candidate-UC name field is additive only.

### Both sub-sprints

- No keyword / regex / if-else / enum expansion on a semantic
  surface (§1.5 / §1.7).
- No eval-side CaseSpec widening to mask the bot's behaviour
  on either trace (§5.4).
- No editing of `docs/sprints/*` or `docs/archive/*`.
- No editing of the system_prompt to encode case-specific
  fixes. CS3 fix lives in AgentRunLoopImpl + discover_triage.
  CS4 fix lives in projection + discover_triage + resolve skill.

## 6. Risk + compounding-effect analysis

### 6.1 CS3 risks

- **R-CS3-1 (medium)**: legitimate clarification turns where the
  LLM emits an empty `user_message` but does intend to ask
  (rare; but if so, the counter not incrementing means the
  budget never triggers, and the session could loop). Mitigation:
  the soft cue in C3.B explicitly discourages empty `user_message`,
  closing this loophole.
- **R-CS3-2 (low)**: characterization tests on legitimate
  clarification (Sprint 33 cue turn) must still increment the
  counter. Verify on uc_h_appeal / cs012 / other DISCOVER-
  clarification cases.
- **R-CS3-3 (low)**: the soft cue in `discover_triage.yaml`
  procedure (which is already very long) risks being ignored by
  the LLM. Mitigation: keep the cue short, position it near the
  existing Sprint 33 cue, and rely on the C3.A runtime backstop
  for the hard guarantee.

### 6.2 CS4 risks

- **R-CS4-1 (medium)**: the new `moderation_reason_available`
  signal could trigger UC-FP preference on edge cases where
  the listing has stale moderation data (e.g. the ad was
  re-published but the lookup carries old removal metadata).
  Mitigation: data-source review — ensure the
  `ListingLookupService` only sets the boolean true when the
  moderation review is CURRENT (not historical).
- **R-CS4-2 (low)**: the candidate-UC name field could subtly
  shift LLM behaviour on every UC classification, not just
  CS4-shape cases. Mitigation: this is the intended effect
  (better LLM context); verify on the full bad-case suite.
  Anti-误杀: a re-bless of the entire baseline is needed after
  C4.A lands.
- **R-CS4-3 (medium)**: the UC-FP RESOLVE skill change to
  surface `get_customer_context` as recommended could alter
  the tool-call profile on UC-FP cases (more get_customer_context
  calls). Mitigation: characterization tests on existing UC-FP
  cases; observability log on tool-call counts.

### 6.3 Compounding sequence — full picture (combining first proposal + this one)

| Order | Sub-sprint | What it fixes | What it doesn't mask |
|-------|------------|---------------|----------------------|
| 1 | **CS1 (S-A: default-resolved gate)** — from first proposal | Runtime stops false-crediting resolved on CLOSE without grounding | A larger fraction of bad cases now surface as honest failures, enabling honest measurement of all subsequent fixes |
| 2 | **CS3 (S-X-CS3)** — counter gating + DISCOVER null-turn cue | Synthesised placeholder no longer burns budget; LLM null-turn discouraged | Combined with CS1, sessions that today vacuously pass via the budget→escalate→Path B sequence will now correctly fail (forensic-evidence improvement) |
| 3 | **CS4 (S-Y-CS4 = OBS-S1+S2 promotion)** — moderation context projection + UC names | Bot picks UC-FP for moderation-aware removed-ad cases; consults canonical reason | After CS1+CS3, the resolved-rate on UC-FP-shape cases can be honestly measured |
| 4 | **CS2-original (S-B: user_role)** — from first proposal | Seller/buyer perspective slip closed | Final semantic-quality fix on the now-clean measurement floor |

This sequencing is critical:
- CS1 + CS3 are **measurement/trace-honesty** fixes. They must
  land BEFORE CS4 + CS2-original so the semantic deltas of the
  latter are not contaminated by false-credited or wrongly-
  budget-escalated sessions.
- CS4 (OBS-S1 + OBS-S2 promotion) is a **prompt_projection
  enrichment** that benefits BOTH CS2-original (user_role
  context) and CS4-shape cases. The candidate-UC name field
  is shared infrastructure.
- CS2-original (user_role) is the most semantic of the four
  and benefits most from a clean measurement floor.

**Incorrect orderings to avoid**:
- CS4 before CS1: the OBS-S1 procedure changes are measured
  against a baseline contaminated by false-resolved on CLOSE
  → cannot tell if UC-FP routing improved.
- CS3 before CS1: less harmful but still incorrect — CS3 alone
  surfaces some more honest failures but CS1's Path B still
  defaults stalled sessions to resolved on phase transitions.
- CS2-original (user_role) before CS4: the user_role slot
  benefits from the candidate-UC-names infrastructure;
  delivering them in the wrong order means duplicated work
  on the candidate_use_cases projection.

**Final recommended global ordering**: **CS1 → CS3 → CS4 → CS2-
original**, with mini re-bless between each. Or, if the
deliver-agent prefers to batch, **CS1+CS3** (measurement honesty,
both touch ControlKernel + AgentRunLoop) → mini re-bless →
**CS4** (OBS promotion, infrastructure for projection
enrichment) → mini re-bless → **CS2-original** (user_role on
the now-clean floor).

## 7. Observability / trace / report implications

### CS2-new (empty trace UX)

- Add `R-admin-empty-trace-zero-turn-affordance` to admin
  observability backlog. P3, small.

### CS3

- Add a soft observability trace event when the runtime
  synthesises the placeholder ("discover_null_turn_synthesised")
  so the gap is visible in the admin trace + eval traces
  without forcing escalation.
- Mid-sprint dashboard: percent of DISCOVER turns synthesised
  vs LLM-emitted; should drop after C3.B's soft cue lands.

### CS4

- The new `moderation_reason_available` slot should render in
  the admin "Projected Context" panel.
- A small dashboard / sweep: percent of removed-ad cases
  classified UC-FP vs UC-A; should rise after OBS-S1 + OBS-S2
  land.
- Per-case tool-call profile: % of UC-FP turns that called
  `get_customer_context`; aiming for high coverage on cases
  where `moderation_reason_available=true`.

## 8. Coverage check vs `action_bank.md` R-items + active scope

### 8.1 CS2-new

No existing R-item covers admin empty-trace affordance. Queue
as new P3 backlog item `R-admin-empty-trace-zero-turn-affordance`.

### 8.2 CS3

- **Adjacent**: `R-discover-clarification-counter-live-wireup`
  (R2.a; closed S-Auto-23). This sub-sprint is a FOLLOW-ON
  refinement to R2.a: the counter is correctly wired, but the
  gating predicate needs the null-turn exclusion. NOT a
  regression of R2.a; an extension.
- **Adjacent**: `R-slow-llm-placeholder-coalesce` (Sprint 19,
  partial). That R-item targets coalescing two consecutive
  deadline-exceeded placeholder events into one user-facing
  beat. CS3's placeholder source is `AgentRunLoopImpl.java:438-441`,
  a different code path. Related theme (placeholder-induced
  user-visible damage) but different fix surface.
- **New R-item**: `R-discover-null-turn-counter-anti误杀` —
  the C3.A + C3.B bundle.

### 8.3 CS4

- **OBS-S1** (UC-A / UC-H / UC-J verify-entity-context
  procedure) — deferred post-M-Auto-6 autoloop work. **CS4 is
  the evidence trigger to promote OBS-S1 to an active R-item.**
- **OBS-S2** (DISCOVER disambiguation cue refinement) —
  deferred post-M-Auto-6. **CS4 is the evidence trigger to
  promote OBS-S2 to an active R-item.**
- **Adjacent**: `R-uc-b-customer-context-policy-review` (Phase
  2 §2.10 policy question). Orthogonal — that's about adding
  UC-B to `get_customer_context` allow-list; CS4 is about
  CALLING `get_customer_context` from UC-FP correctly.
- **Adjacent**: `R-prompt-phase-plan-directive-followship`
  (general LLM directive non-fulfilment). UC-FP's mandatory
  step is a directive the LLM never sees because UC-A was
  picked instead — so this is a sibling failure of CS4 but
  the fix is upstream of directive enforcement (the LLM
  needs to PICK UC-FP first).
- **New combined R-item**: `R-moderation-context-projection-
  and-uc-fp-routing` — the C4.A bundle (promotes OBS-S1 +
  OBS-S2 with extensions for `moderation_reason_available`
  signal + candidate-UC names).

### 8.4 Active scope

M-Auto-6 dev-side complete; close pending. Neither CS3 nor
CS4 is a framework-defect priority brief (§5.8) — they don't
preempt M-Auto-6 close. Both join the post-M-Auto-6 candidate
pool with CS1 + CS2-original from the first proposal. Total:
4 sub-sprints (S-A, S-X-CS3, S-Y-CS4, S-B) likely under one
M-Auto-7 wrapper if deliver-agent chooses.

## 9. Summary table — what to do, in order

**Milestone ordering CONFIRMED by human 2026-06-07**:
**CS1 → CS3 → CS4 (entity-context primary, autoloop-driven) → CS2-original**.

All four sub-sprints belong to a single milestone wrapper
(provisionally M-Auto-7). CS4 is structurally different from
the others — see §3.4 reframing — so its sub-sprint shape is
multi-phase (A infra + B CaseSpec + C autoloop + D close).
The deliver-agent may further split CS4 into S-Y-CS4.A/B/C/D
sub-sub-sprints if preferred for cadence and Codex review
granularity.

| Step | Sub-sprint | Layer (§3.2) | Est. effort | Pre-req |
|------|------------|--------------|-------------|---------|
| 1 | **S-A (CS1 default-resolved gate)** — Path B `ControlKernel.java:575-579` gated through `isResolvedSuccessTerminal` | infra | 1-2 d | M-Auto-6 close (incl. S-Auto-28 unblock sequence + milestone re-bless + Codex §4.3) |
| 2 | Mini re-bless after S-A (measurement-honesty floor) | — | — | S-A close |
| 3 | **S-X-CS3 (R2.a counter gating + DISCOVER null-turn cue)** — `AgentRunLoopImpl.java:438-458` exclude runtime-synthesised placeholder from counter + `discover_triage.yaml:20` soft cue against null turns | infra + prompt_projection | 2-3 d | S-A close |
| 4 | Mini re-bless after S-X-CS3 | — | — | S-X-CS3 close |
| 5 | **S-Y-CS4 (OBS-S1 + OBS-S2 autoloop-driven promotion)** — see §3.4 Parts A/B/C/D | multi-layer (prompt_projection + eval_spec for human parts; semantic_planner via autoloop output) | 8-13 d (Part A 2-3 + Part B 2-3 + Part C 3-5 + Part D 1-2) | S-X-CS3 close (autoloop capability spike RESOLVED READY — see §3.5) |
| 5a | • S-Y-CS4 Part A — projection infra (moderation_reason_available + candidate-UC names + soft_signal_via_projection wiring) | prompt_projection | 2-3 d (subset of step 5) | S-X-CS3 close |
| 5b | • S-Y-CS4 Part B — CaseSpec authoring (≥4 target+neighbor+negative, L2 outcome_check shape per §3.4 Part B table; "verify entity context" framing) | eval_spec | 2-3 d | Part A close |
| 5c | • S-Y-CS4 Part C — autoloop semantic optimization + human review per §4.1 kernel | semantic_planner (autoloop-authored; human-reviewed) | 3-5 d | Part B close |
| 5d | • S-Y-CS4 Part D — mini re-bless + Codex per-sub-sprint review + sub-sprint close | governance | 1-2 d | Part C close |
| 6 | Mini re-bless after S-Y-CS4 | — | — | S-Y-CS4 close |
| 7 | **S-B (CS2-original user_role projection)** — `user_role` slot derived from listing-ownership match + DISCOVER + UC-A skill soft cues | prompt_projection | 3-5 d | S-Y-CS4 close (shares candidate-UC-name infra from Part A.2) |
| 8 | Full M-Auto-7 milestone re-bless | — | — | All sub-sprints closed |
| 9 | M-Auto-7 milestone close (Codex §4.3 milestone-shared review) | — | — | Re-bless paired-evidence complete |
| 10 | **Open R-full-issue-ledger-light research workstream** (independent of M-Auto-7; surfaces from §10.4 of this proposal) | research-only | TBD | CS1-4 close (per human directive 2026-06-07) |

**Autoloop capability spike — RESOLVED READY 2026-06-07** (see
§3.5 for full evidence). Autoloop's locked mutable surface
includes the skill yaml `$.procedure` field (and 3 other soft-
semantic fields) across the 6 in-scope skill yamls; the full
proposer → applier → validator pipeline is live and has been
running ~6 weeks across M-Auto-4 + M-Auto-5 with 65 iterations
(17 on `$.procedure` alone). Part C launches as soon as Part A
+ Part B land; no tooling pre-requisite. Anti-误杀 enforcement
is via the negative-control CaseSpec (`cs_uc_a_generic_policy_
question`) — autoloop candidates that force ad_id elicitation
on genuinely-generic questions fail this CaseSpec and are
rejected before merge.

**Why this ordering** (recap from §6.3 + human confirmation):
- S-A first: measurement honesty floor. Without it, every
  subsequent fix's eval delta is contaminated by false-credit
  on CLOSE-arm sessions.
- S-X-CS3 second: closes the R2.a counter false-positive +
  null-turn pattern; both shapes intersect with CS1's exit
  paths so completing S-A first avoids re-touching the same
  ControlKernel arms twice.
- S-Y-CS4 third: needs autoloop on a clean baseline. Also
  produces the projection infra (moderation_reason_available
  + candidate-UC names) that S-B depends on.
- S-B fourth: lands on the new candidate-UC-name infrastructure
  + the now-clean measurement floor. Highest-leverage semantic
  fix last.

**D-full-issue-ledger-light research workstream** (step 10):
per human directive 2026-06-07, cross-UC stash-and-resume is
NOT bundled into M-Auto-7. Open a separate research item AFTER
CS1-4 close. See §10.4 for the surfaced design question.
Deliver-agent files in `docs/action_bank.md §4` as
`D-full-issue-ledger-light` (NEW, deferred-pending-research).

## 10. Cross-UC continuity findings (surfaced 2026-06-07; SEPARATE design conversation, NOT folded into CS3/CS4 sub-sprint scope)

The same human follow-up that reframed CS4 also raised three
questions about cross-UC behaviour. These findings answer the
questions code-grounded and surface a separate architectural
question that the deliver-agent / human should decide
independently of the CS1–CS4 sub-sprints.

### 10.1 Q1 — Does the phase machine transition properly across UCs?

**YES, but transition authority is runtime-owned, not LLM-owned.**

- `ClassifyUseCaseTool` is exposed to the LLM ONLY in DISCOVER
  when `activeUseCase == null`
  (`ClassifyUseCaseTool.java:26-51, 100-206`). In RESOLVE /
  CONFIRM the tool is NOT in `allowedTools`; LLM cannot
  re-classify mid-session.
- Cross-UC transitions happen via runtime decision logic:
  - `DriftDetector.detect()` (`DriftDetector.java:55-94`) runs
    every turn, checks hard-shift keywords
    (GDPR / refund / scam / ad-removal / technical), can mutate
    `session.activeUseCase` BEFORE the LLM loop runs.
  - `RuntimeIntentClassifier.classify(...)` classifies intent
    relation (SAME_ISSUE / NEW_LOW_RISK_UC / NEW_HIGH_RISK_UC /
    HUMAN_REQUEST / etc.).
  - `RerouteDecider.decide(...)` (`RerouteDecider.java:49-137`)
    produces a `RerouteDecision` with action
    (CONTINUE_CURRENT / SOFT_SHIFT_TO_DISCOVER /
    RISK_SHIFT_TO_INTAKE / REBOUND_TO_RESOLVE /
    ESCALATE_IMMEDIATELY), targetUc, targetPhase.
  - `ControlKernel.applyRerouteDecision()`
    (`ControlKernel.java:600-660`) applies the decision to the
    session BEFORE the LLM gets its turn.

Implication: phase transitions on UC switch DO happen, but the
LLM cannot directly invoke "switch to UC-B"; it can only
implicitly trigger one via user-message content that
DriftDetector / IntentClassifier picks up.

### 10.2 Q2 — UC-A → UC-B → UC-A: can the original UC-A state be restored?

**NO. There is no stash-and-resume mechanism.**

- `BotSession` carries a SINGLE `activeUseCase` field (not a
  stack or list).
- `SkillStateBus.applyOnSkillSwitch(priorSkill, newSkill,
  session)` (`SkillStateBus.java:85-142`) on every skill switch
  applies the NEW skill's `state_inheritance.{reset, inherit}`.
  It does NOT back up the prior UC's state. Fields cleared
  during UC-B (via UC-B's `reset` list) are permanently gone;
  when UC-A is re-entered later, UC-A's inheritance is applied
  FRESH — there is no restore point.
- Concrete CS-A-A→B→A flow:
  1. UC-A is in RESOLVE.
  2. User message drifts to UC-B → `RerouteDecider` emits
     `SOFT_SHIFT_TO_DISCOVER`
     (`RerouteDecider.java:89-118` NEW_LOW_RISK_UC case) → UC-B
     starts at DISCOVER.
  3. UC-B is classified into RESOLVE.
  4. User drifts back to UC-A → again SOFT_SHIFT_TO_DISCOVER →
     UC-A re-enters at DISCOVER (NOT at the prior RESOLVE state).
- `prior_use_case_carry` projection slot
  (`ContextProjectionBuilder.java:46-60, 569-597`) does carry,
  within a 4-turn aging window, the prior UC + prior skill name
  + up to 3 source_ids — but ONLY as soft observability for the
  LLM. It does NOT restore phase or session state.
- `docs/action_bank.md §4` `D-full-issue-ledger` is the
  natural home for "stash-and-resume" / "multi-issue carry" —
  still DEFERRED. Sprint 11 explicitly carved this out: "do
  not reopen without a new objective doc and explicit
  acceptance criteria".

Implication: today's CS agent is single-issue-at-a-time. A real
multi-issue conversation (user mentions account problem, then
payment problem, then wants to return to account problem) loses
the original issue's RESOLVE progress on every soft shift.

### 10.3 Q3 — Does customer_context carry across UC switches?

**YES (payload-level), with caveats.**

- `BotSession.customerContext` is a session-scoped persisted
  field. UC switching does NOT auto-clear it.
- Both major RESOLVE skills declare `state_inheritance.inherit:
  [customer_context, accumulated_tool_results]`
  (`resolve_faq_grounded_answer.yaml:44-49`,
  `resolve_intake_collect_and_handover.yaml:35-42`). On skill
  switch, `customer_context` passes through unchanged.
- **Caveat #1 — `accumulated_tool_results` is per-turn
  ephemeral**: it's an in-memory variable within
  `AgentRunLoopImpl`, not a persisted BotSession field. The
  "carryover" mostly means "the LLM is told via the
  `already_called` projection slot that the prior turn called
  the tool". Re-calling the tool fetches fresh results; prior-
  call returned values are NOT persisted into the session for
  next-turn LLM consumption (apart from side-effects like
  `session.customerContext` being set).
- **Caveat #2 — projection visibility**: the per-turn
  projection emits `customer_context_status` enum (`missing_
  email / missing_ad_id / lookup_failed / lookup_skipped /
  loaded`) and the `ad_reference` struct as state-level
  signals (`ContextProjectionBuilder.java:1383-1449`). Whether
  the LLM also sees the full `customer_context` payload
  (listing.title, listing.status, listing.moderation_review,
  etc.) depends on the skill's `required_context_keys` /
  `soft_signal_via_projection` declarations. UC-A's
  resolve_faq_grounded_answer.yaml `:19-22` declares
  `required_context_keys: [form_context, customer_context,
  listing_context]`, so the payload IS surfaced once loaded.
- **Caveat #3 — non-carried fields**: `articlesShown`,
  `clarificationCount`, `faqMissCount`, `repeatedActionCount`
  are session-scoped, NOT reset on UC switch. They accumulate
  across UCs. Sometimes this is desired (one global budget),
  sometimes problematic (e.g. UC-A's articles still count
  toward UC-B's grounding floor). Not in scope for any current
  R-item; flag for a future review.

### 10.4 Surfaced design question — D-full-issue-ledger-light research workstream (CONFIRMED post-CS1-4)

**HUMAN DIRECTIVE 2026-06-07**: do not bundle cross-UC
stash-and-resume into M-Auto-7. Create a separate research
item for D-full-issue-ledger-light after CS1-4 close.

**Path** (confirmed):

1. Complete M-Auto-7 (CS1 → CS3 → CS4 → CS2-original) per §9.
2. AFTER CS2-original close, open a research-only workstream
   (Path α below).
3. Path α scope (proposed; deliver-agent + human refine at open
   time):
   - **Phase 1 — quantify the need**: survey real multi-issue
     traces from bad_cases + anchor_outcome + (if available)
     production traces. Identify sessions where a UC switch
     destroyed prior-UC state that would have been useful on
     return. Count: how often does it happen, what is lost
     each time, what is the user-impact shape (lost intake
     fields, lost RESOLVE progress, repeated FAQ search,
     etc.).
   - **Phase 2 — design (if Phase 1 evidence justifies)**:
     a thin `D-full-issue-ledger-light` proposal:
     - push/pop session-side stack of `(prior_uc, prior_phase,
       prior_skill_state_snapshot)` tuples on UC switch
     - restore phase + relevant inherited state on UC return
     - aging window / capacity limits to avoid unbounded growth
     - interaction with existing `prior_use_case_carry` soft
       signal (this slot remains complementary, not replaced)
   - **Phase 3 — bundle-or-defer decision**: human + deliver-
     agent decide whether to promote to active sub-sprint or
     return to deferred.
4. R-item name in `docs/action_bank.md §4`:
   `D-full-issue-ledger-light` (NEW, status: deferred-pending-
   research). Deliver-agent files this entry as part of the
   M-Auto-7 close handoff.

**Rationale for the defer**:

- M-Auto-7 (CS1-4) is bounded; bundling adds significant
  scope creep (BotSession schema change + SkillStateBus
  rework + phase-machine resume logic).
- Path α produces evidence to anchor the design; without it,
  we'd be designing for a hypothesised need.
- D-full-issue-ledger (the full version) was deferred by
  Sprint 11 with explicit "do not reopen without a new
  objective doc and explicit acceptance criteria". The
  research workstream IS that objective doc.

### 10.5 Coverage check vs `action_bank.md` R-items

- `D-full-issue-ledger` — deferred (Sprint 11 §4). The
  natural home for stash-and-resume; would need a new
  objective doc + acceptance criteria to reopen.
- `prior_use_case_carry` — landed (Sprint 41); operates as
  soft signal only; not a state-restoration mechanism.
- No current R-item names "phase/state preservation across UC
  switches" as an active item.

---

**Anchored evidence checklist (all verified at HEAD
`auto-loop-branch`)**:

CS2-new:
- `SessionManager.java:74-301` — createSession; `:101-102`
  default state; `:144-150` async-first-message comment;
  `:220` persist; `:231-262` hard-OOS synthetic turn
- `DemoInspectionController.java:135-138` — trace endpoint
- `LocalEventStore.java:65-74` — bot_turns query
- `TraceViewer.tsx:62-76` — empty-state render

CS3:
- `AgentRunLoopImpl.java:438-441` — placeholder auto-fill
- `AgentRunLoopImpl.java:442-457` — R2.a counter increment
- `AgentRunLoopImpl.java:1206-1209` — counter predicate
- `control-policy.yaml:1-2` — max-clarification-rounds: 2
- `BudgetChecker.java:30-37` — budget check
- `ControlKernel.java:288-318` — pre-LLM budget gate +
  injected escalation message
- `ControlKernel.java:782-794` — phase-aware re-map (S-Auto-25)
- `discover_triage.yaml:14` — `allow_interim_message: false`
  (metadata-only)
- `discover_triage.yaml:19-22` — DISCOVER procedure (no null-
  turn prohibition)
- `templates.yaml:28` — placeholder example
- `PhaseEvaluator.java:715`, `ControlKernel.java:458/704`,
  `ActionParser.java:72` — same placeholder string

CS4 (entity-context verify gap — PRIMARY):
- `resolve_faq_grounded_answer.yaml:13-18` — UC-A
  `tools_required` lists `get_customer_context` (reachable but
  not procedure-mandated)
- `resolve_faq_grounded_answer.yaml:30-31` — permissive
  grounding_instruction ("If tool data contains specific
  information…answer from that first") — presupposes data is
  there
- `resolve_faq_grounded_answer.yaml:52-65` — only
  `mandatory_for: UC-A` critical step today is
  `search-knowledge-before-faq-answer` — no `verify-entity-
  context` step exists
- `discover_triage.yaml` (full) — no instruction to ask for
  `ad_id` as a precondition
- `FormContextIngestionService.java:93-142` — auto-trigger
  conditions (line 98 allow-set; lines 109-110 require email +
  ad_id to actually fetch listing)
- `ContextProjectionBuilder.java:1383-1449` —
  `customer_context_status` enum + `ad_reference` struct
  (observable but not enforcing)
- `docs/proposals/uc_h_intake_lockin_after_uc_a_misclass.md`
  §4 — coverage gap explicitly named

CS4 (UC-A vs UC-FP boundary cue — SECONDARY):
- `use-case-registry.yaml:2-43` — UC-A + UC-FP definitions
- `discover_triage.yaml:20` — Sprint 33 cue (UC-A vs UC-H
  only)
- `discover_triage.yaml:56-71` — `removed-listing-route-with-
  moderation-context` (mandatory_for: UC-FP)
- `resolve_faq_grounded_answer.yaml:113-127` —
  `consult-moderation-context-on-removal-explanation`
  (mandatory_for: [UC-FP])
- `tool-policy.yaml:8-10` — get_customer_context allowed-ucs
- `GetCustomerContextTool.java:54-59` — moderation_review
  attached on removed/moderated listing
- `GetCustomerContextTool.java:93-107` — sanitised listing
  fields (no moderation_reason in listing struct)
- `ContextProjectionBuilder.java:499-508` — bare UC IDs (no
  human names)
- `BudgetChecker.java:30-84` — no near-duplicate-answer
  bucket
- `docs/sprints/sprint-078-objective.md` + `sprint-078-handoff.md`
  — OBS-S1 + OBS-S2 definitions (deferred to autoloop)

Autoloop capability spike (§3.5):
- `autoloop/config.yaml:12-30` — `mutable_surface` locks
  6 skill yaml files × 4 field paths
- `autoloop/program.md §2 lines 40-78` — locked-contract
  governance for the mutable surface
- `autoloop/autoloop/meta_agent/proposer.py:44-72, 92-94,
  171-179` — Hypothesis dataclass + skill-yaml read path
- `autoloop/autoloop/meta_agent/prompts/propose.txt:22-25` —
  allowed field paths in the LLM proposer prompt
- `autoloop/autoloop/sandbox/applier.py:358-378, 152, 168-171`
  — skill yaml mutation + on-disk write + git commit
- `autoloop/autoloop/sandbox/yaml_diff_validator.py:66-72,
  111-117, 149-150` — surface-lock enforcement
- `autoloop/autoloop/sandbox/anti_hardcode_check.py:1-26` —
  content-discipline detector (Q1/Q2/Q4/Q5)
- `autoloop/results/experiments.jsonl` — 65 production-mode
  iterations spanning M-Auto-4 + M-Auto-5; field-path
  distribution shows `$.procedure` as #1 target (17 of 65)

Cross-UC continuity (§10):
- `ClassifyUseCaseTool.java:26-51, 100-206` — tool whitelisted
  in DISCOVER + activeUseCase==null only; strong-prior guard
- `DriftDetector.java:55-94` — hard-shift keyword detection
- `RerouteDecider.java:49-137` — reroute decision matrix
- `ControlKernel.java:600-660` — applyRerouteDecision applies
  decision pre-LLM
- `SkillStateBus.java:85-142` — applyOnSkillSwitch (no backup;
  fresh inherit/reset per new skill)
- `BotSession.java` — single `activeUseCase` field, no stack
- `ContextProjectionBuilder.java:46-60, 569-597` —
  `prior_use_case_carry` slot (4-turn aging window, soft signal
  only)
- `resolve_faq_grounded_answer.yaml:44-49` +
  `resolve_intake_collect_and_handover.yaml:35-42` —
  `state_inheritance.inherit: [customer_context,
  accumulated_tool_results]`
- `docs/action_bank.md §4` — `D-full-issue-ledger` (deferred)
