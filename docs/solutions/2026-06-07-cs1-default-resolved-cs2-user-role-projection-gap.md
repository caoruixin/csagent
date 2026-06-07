---
title: CS1 default-resolved on phase=CLOSE + CS2 user-role projection gap (seller/buyer perspective slip)
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
  Two new bad cases observed by human 2026-06-07. Both surface
  failure modes in DISCOVER-adjacent behaviour. CS1 is a Runtime
  §1.4 trace-contract gap (default-resolve when phase transitions
  to CLOSE without grounding evidence). CS2 is a §1.3 + §3.2 Q3
  prompt-projection gap (no user_role / seller-buyer signal in
  form_context or projection; LLM has to infer purely from message
  wording and slips mid-response). Independent fix surfaces but
  shared "DISCOVER fragility" theme. Recommended as TWO sub-sprints
  with CS1 first (measurement honesty before semantic fix).

  Filed AFTER M-Auto-6 dev-side closure (S-Auto-28 closed 2026-06-07,
  Definition-of-done unblock sequence in progress, milestone-shared
  re-bless still pending). These R-items are queued for action_bank
  §5.2; promotion to active sub-sprints is the deliver-agent's
  decision after M-Auto-6 closes.
---

# CS1 default-resolved on phase=CLOSE + CS2 user-role projection gap

## 1. Executive summary

Two real-session bad cases reveal two independent gaps in the runtime
contract and the per-turn projection:

- **CS1** (trace `3e4f0aad-af7…`) — bot stayed in DISCOVER → DISCOVER
  asking a clarifying question (0 tool calls, no UC commit, no
  grounded answer), yet the session terminated with
  `containment_outcome="resolved"`. Root cause: a runtime path at
  `ControlKernel.java:575-579` that defaults containment to
  `"resolved"` whenever the LLM transitions the phase to `CLOSE`
  without an explicit containment having been stamped — independent
  of whether a grounded answer was delivered. This is a §1.4
  Runtime trace-contract leak: the runtime credits a success the
  bot never demonstrated.
- **CS2** (trace `89f4ab98…`) — seller opens with "my buyer asked
  some questions about my ad … the buyer said when I told him to
  search for this ad … he can't find it — I want to investigate
  this problem". Bot's first response correctly asked the seller
  "is it published and visible on your end?" then slipped:
  "could you share the search terms or location your buyer used?"
  — treating the seller as a proxy for the buyer's search session
  rather than as the ad owner reporting a buyer's experience. Root
  cause: the form_context, the per-turn projection, the system
  prompt, and every skill yaml carry **no `user_role` / `is_seller`
  signal**. The LLM has to infer the user's role purely from message
  text and slips perspective mid-response. This is a §3.2 Q3
  `prompt_projection` gap.

The two fixes are independent on the file/surface level but interact
through the eval signal: CS1 alone would surface more cases as
`containment_incomplete_after_partial_answer` (instead of falsely
`resolved`), which is a measurement-honesty improvement that should
land BEFORE CS2 so the post-CS2 re-bless measures real semantic
delta on a clean honest floor. Recommended cadence: two small
sub-sprints, CS1 first, CS2 second, both narrow, both pure
`prompt_projection` + `infra` (no semantic hardcode).

## 2. Current-state survey (code-grounded; HEAD = `auto-loop-branch`)

### 2.1 Resolution-marking paths in the runtime

All writes to `BotSession.containmentOutcome` happen in
`server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`.
Four distinct paths exist today (all verified at HEAD):

1. **Path A — explicit escalation.** Lines 501-502 (and 1278-1279
   on a separate force-escalate path). When the agent loop ends via
   ESCALATE, containment is stamped `"escalated"`. Correct;
   anti-误杀 in `isResolvedSuccessTerminal`'s non-null guard.
2. **Path B — phase-transitions-to-CLOSE default.** Lines 575-579:
   ```java
   if ("CLOSE".equals(phaseAfter)) {
       session.setHandlingState("CLOSED");
       if (session.getContainmentOutcome() == null) {
           session.setContainmentOutcome("resolved");
       }
       eventEmitter.emitSessionClosed(session.getSessionId(),
               session.getContainmentOutcome());
   }
   ```
   This is the smoking gun for CS1: no grounding-evidence check,
   no disposition allow-list, no `articlesShown` requirement, no
   DISCOVER-only block. The comment at lines 572-574 names this as
   the "D16.D legacy evaluateClose state-setting mirror".
3. **Path C — `isResolvedSuccessTerminal`-gated stamp.** Lines 582-611
   (gate implementation at 1418-1469). Fires ONLY when `terminalOutcome
   == FINAL_ANSWER` AND `resolveDisposition ∈ {READY_TO_CONFIRM,
   ANSWERED_SUBTASK}` AND `articlesShown` is non-empty AND prior
   containment is null. This is the post-Sprint-075 ("S-Auto-20")
   anti-误杀-gated path that CORRECTLY excludes ungrounded answers,
   clarification turns, mid-resolution turns, and escalation. It is
   bypassed entirely when Path B fires first.
4. **Path D — `shouldVoidResolvedStamp` downgrade.** Lines 612-641
   (`shouldVoidResolvedStamp` at 1508-1520; `voidResolvedStamp` at
   1532-1535). Voids a prior `"resolved"` to
   `CONTAINMENT_INCOMPLETE_AFTER_PARTIAL_ANSWER` when a LATER turn
   hits `{MAX_STEPS, ERROR, DEADLINE_EXCEEDED, LLM_UNAVAILABLE}`.
   Does NOT cover the "Path B stamped resolved on a CLOSE
   transition that was itself unjustified" case — by the time
   Path B fires, the loop is exiting; there is no later turn to
   run the void check.

#### 2.1.1 Why Path B fires on CS1

The `phaseAfter` value is computed earlier in the same `process`
method (call it `evaluatePhase(...)` for orientation; lives in
`PhaseEvaluator.java` referenced from `ControlKernel.processMessage`).
The phase transition is **LLM-owned** (§1.3 semantic decision): the
LLM emits `next_phase` in its structured reply, and the runtime
applies the transition. So if the LLM at any turn (clarifier turn,
hallucinated "user confirmed" turn, simulator drop-out producing a
goodbye, etc.) sets `next_phase=CLOSE`, Path B stamps resolved.

This is **structurally** the same anti-误杀 hole the Sprint 075 +
Sprint 077 work already plugged for `isResolvedSuccessTerminal`
(`FINAL_ANSWER` + disposition + `articlesShown` gates) and
`shouldVoidResolvedStamp` (terminal-failure downgrade) — Path B is
the ONE remaining stamp site that has no grounding gate. The Sprint
074 D16.D `Code-comment` at 572-574 explicitly names this as a
"legacy" mirror — i.e. a port of a pre-S-Auto-20 evaluateClose
behaviour that was not re-examined when S-Auto-20 introduced the
gated path.

### 2.2 Eval-side default-resolved coverage gap

The eval composite gate at
`eval_interactive/eval_interactive/scoring/composite.py:285-303`
guards against vacuous-pass with:

```python
elif composite == 0.0 and not l2_results:
    case_passed = False
    verdict_reason = "no_l2_evidence_to_pass"
```

This OQ-S77 #3 gate fires ONLY when **both** `composite == 0.0` AND
`l2_results == []`. For CS1's CaseSpec shape, the case likely has
configured L2 outcome checks (`correct_outcome` etc.), so
`l2_results != []` even when every check scored 0 — the eval gate
does NOT trip, and the runtime-stamped `"resolved"` flows through
to `case_passed=true`. So this is NOT a complete eval-side backstop
for the Path B leak.

### 2.3 User-role / seller-buyer signal — comprehensive negative survey

Verified at HEAD; the listed files contain ZERO references to
`seller`, `buyer`, `ad_owner`, `advertiser`, `poster`, `user_role`,
`is_seller`, `posted_by`, `owner_email`:

- **`server/src/main/resources/prompts/system_prompt.txt`** —
  line 1: "You are a Gumtree customer service assistant. You help
  customers with their inquiries about ads, accounts, payments, and
  safety issues." Treats every user as a generic "customer".
  `grep -in 'seller\|buyer\|owner\|advertiser\|poster' system_prompt.txt`
  returns 0 matches.
- **`server/src/main/resources/skills/*.yaml`** —
  `grep -rin 'seller\|buyer\|ad_owner\|user_role\|posted_by' skills/`
  returns 0 matches across every skill yaml
  (`discover_triage.yaml`, `resolve_faq_grounded_answer.yaml`,
  `resolve_intake_collect_and_handover.yaml`, etc.).
- **`FormContextIngestionService.java:59-66`** — the form_context
  payload writes only `first_name`, `email`, `topic_subject`,
  `description`, and (optional) `ad_id`. No role field, no
  ownership inference.
- **`ContextProjectionBuilder.java`** — the projection emits
  `customer_context_status` enum (`missing_email / missing_ad_id /
  lookup_failed / lookup_skipped / loaded` at lines 1383-1401) and
  the `ad_reference` struct (`:1404-1449`), but NEITHER carries a
  role inference. The presence of `ad_id` + `email` is structurally
  signaled; whether the email's owner matches the ad's owner is
  not derivable from the projection.
- **`GetCustomerContextTool.java:79-105`** — the sanitised customer
  context returned to the LLM contains
  `account_status / account_type / creation_date / active_ads_count
  / total_ads_count / has_verified_email / has_verified_phone`
  plus listing fields (`ad_id / title / category / status / price /
  location / posted_date / is_featured`). Crucially the explicit
  comment at `:85-86` says "Intentionally omit: email, phone,
  full_name, address, etc." — meaning even when the tool runs, the
  bot can NEVER compare the form-context email to the ad's
  `posted_by` email. Role inference is structurally impossible.
- **`use-case-registry.yaml`** — the canonical UC list. Note that
  UC-G is "GDPR / Data Deletion" (HIGH risk, INTAKE path), while
  CS2's bot reasoning labelled its candidate UC as "UC-G (ad
  visibility/discoverability)". The ad-visibility UC is **UC-A**.
  The bot's reasoning text is internally inconsistent: it picked
  the WRONG UC id (UC-G) with a UC-A-shaped gloss. This is a
  separate but related sub-issue (see §3.2.4).

### 2.4 Recent commits touching prompt / projection surfaces

`git log --since="30 days ago" --oneline -- server/src/main/resources/prompts/
server/src/main/resources/skills/ server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
shows ~20 commits across M-Auto-6 (R1.a intake_fields schema,
R2.a DISCOVER counter, R4.a customer_context_status / ad_reference,
R5 citation-token wording, R7 update_intake_fields). **None of them
removed a seller/buyer framing** — confirming that the
user's "之前为 seller 工作的好好的" memory is most likely **LLM
training-data common-sense inference** that worked some fraction
of the time without an explicit signal. With no signal in the
projection, the behaviour is a per-draw lottery.

## 3. Root-cause analysis

### 3.1 CS1 — Default-resolved on phase=CLOSE

**Observed**: `containment_outcome="resolved"` on a trace whose last
visible turn is `DISCOVER → DISCOVER` with 0 tool calls and a
clarifying question.

**Expected**: containment should be `"resolved"` only when a
grounded answer was delivered (Path C's gate). For a session that
ended without resolution evidence — including a DISCOVER session
that never even committed a UC — containment should be one of:
- `"escalated"` (if an escalation occurred), or
- `CONTAINMENT_INCOMPLETE_AFTER_PARTIAL_ANSWER` (if a prior turn
  stamped resolved but the session then failed), or
- a new dedicated value like `"incomplete_no_resolution"` or
  `"discover_stalled"`, or simply `null` (blank, the eval gate then
  routes by `case_passed=false`).

**Why it happens**: `ControlKernel.java:575-579` Path B unconditionally
defaults to `"resolved"` on any `phaseAfter == "CLOSE"` transition
where `containmentOutcome` is null. The LLM-owned phase transition
to CLOSE can be triggered by:
- A turn where the LLM (incorrectly) decides the conversation is
  done and emits `next_phase=CLOSE` (e.g. hallucinates user
  confirmation; reads "thanks" as goodbye; closes after a
  clarifier).
- A simulator that drops out after a short conversation and the
  LLM closes in response.
- A `goal_achieved` simulator preemption that doesn't go through
  Path C because `articlesShown` is empty (DISCOVER never produced
  a grounded answer).

In every case, runtime credits the bot with a success that wasn't
demonstrated. This is a §1.4 violation: the trace-contract owner
(Runtime) must not credit a success that the substantive evidence
does not support.

**Layer (§3.2)**: `infra` — Runtime trace-contract / persistence
layer. Specifically the same family as Sprint 075 / S-Auto-20 (#1
`isResolvedSuccessTerminal` introduction) and Sprint 077 / S-Auto-22
(#4 `shouldVoidResolvedStamp`). NOT `semantic_planner` — the LLM is
not asked to set containment_outcome; the runtime stamps it.

### 3.2 CS2 — Seller/buyer perspective slip

**Observed**: bot's first response: "Could you confirm if the ad is
published and visible on your end? Also, could you share the search
terms or location your buyer used? This will help narrow down the
issue." The first sentence treats user as seller (ad owner);
the second sentence treats user as the buyer's search proxy.
Internally inconsistent.

**Expected**: a CS agent that recognises the user is the ad poster
reporting on behalf of a buyer should:
- frame all clarification through the seller's perspective ("Is
  your ad currently live? When did you post it? Have you tried
  searching for it yourself while logged out?"),
- treat the buyer as a third party whose information must be
  RELAYED through the seller, not solicited directly,
- not ask the seller to provide the buyer's exact search behaviour
  as if it were the seller's own.

**Why it happens** (multi-layer):

1. **Layer A — `prompt_projection` (§3.2 Q3, primary)**: there is
   NO `user_role` / `is_seller` / `viewer_perspective` slot in the
   per-turn projection. The form payload carries `email` + `ad_id`
   + `description` but the bot can neither:
   - structurally infer "this email owns this ad" (the runtime has
     `ListingLookupService`-derived state but exposes only
     `customer_context_status` enum + `ad_reference` struct —
     neither carries ownership match);
   - nor be told "the user is a Gumtree seller" generically (since
     the system prompt is role-agnostic).
   So when the user says "my buyer asked some questions about MY
   ad", the LLM has to infer the seller perspective purely from
   message wording. Inference works on some draws and slips on
   others.
2. **Layer B — `semantic_planner` (residual; secondary)**: even
   with a clear seller signal in the message ("my ad", "my buyer"),
   the LLM drifted mid-response. This is partly a semantic-planning
   weakness, but the right fix is NOT to add a Java guard or
   keyword rule — it is to (a) give the LLM a structural signal so
   the inference is no longer ambiguous, and (b) optionally
   reinforce the framing in the DISCOVER procedure ("when the user
   is the ad poster, ALL questions should be framed from their
   account perspective; never treat them as a proxy for a third
   party's search behaviour").
3. **Layer C — UC label hallucination (independent sub-issue)**: the
   bot's reasoning text says "UC-G (ad visibility/discoverability)".
   The actual UC-G in `use-case-registry.yaml:44-49` is "GDPR / Data
   Deletion"; the ad-visibility UC is **UC-A** at `:2-7`. The LLM
   gets the candidate UC list as bare IDs (no human-readable names
   on the per-turn projection — see
   `ContextProjectionBuilder.java:499-508`). When the LLM picks a
   UC, it must rely on training-data memory of "what UC-X means".
   This is a separate `prompt_projection` weakness (the UC list
   should carry human names alongside IDs) but is not the primary
   CS2 driver; the seller/buyer slip would occur even if the UC
   were correctly named UC-A. Noted as OBS-CS2.4 for future
   surfacing.

**Layer (§3.2 primary)**: `prompt_projection` (Q3) — the LLM made a
defensible choice given the impoverished projection; adding a
structural `user_role` signal is the right fix surface, not a
prompt-side keyword rule.

### 3.3 Why the two compound

CS1 and CS2 are independent on the fix surface (different files,
different layers) but interact through the eval signal:

- **Today** (both bugs live): a CS2-shaped session where the bot
  slips perspective but then transitions to CLOSE will be stamped
  `"resolved"` by Path B. The eval may even mark `case_passed=true`
  on a non-empty `l2_results`. So **CS1 (default-resolved) masks
  CS2 (perspective slip)** by crediting success on a session that
  produced a confused, perspective-slipped response without
  grounding.
- **After CS1 fix only**: CS2-shaped sessions will stop being
  credited as `"resolved"` and will surface in the bad-case suite
  as legitimate failures. This is the **measurement-honesty
  improvement** that should land FIRST so the post-CS2 re-bless
  measures real semantic delta.
- **After CS2 fix only (without CS1)**: the perspective slip rate
  on CS2-shape cases would decrease in the LLM's responses, but a
  fraction of slipped sessions would still be credited
  `"resolved"` by Path B, contaminating the measurement.

This is the same compounding shape as the M-Auto-5 framework-defect
priority (§5.8): infrastructure / trace-contract fixes precede
semantic fixes so the semantic fix can be honestly measured.

## 4. Design alternatives + trade-offs

### 4.1 CS1 — Three options

**Option C1.A — Tighten Path B to mirror Path C's gate** (recommended)

Replace the unconditional default-resolved at
`ControlKernel.java:575-579` with the same `isResolvedSuccessTerminal`
gate already used at Path C. Concretely:

```java
if ("CLOSE".equals(phaseAfter)) {
    session.setHandlingState("CLOSED");
    if (session.getContainmentOutcome() == null) {
        if (isResolvedSuccessTerminal(session, runResult)) {
            session.setContainmentOutcome("resolved");
        } else {
            // No grounded answer reached; leave containment null
            // so the eval gate routes case_passed by L2 evidence
            // rather than by the false-positive default.
            // (Alternative: set "incomplete_no_resolution" if a
            //  new enum value is desired.)
        }
    }
    eventEmitter.emitSessionClosed(session.getSessionId(),
            session.getContainmentOutcome());
}
```

Trade-offs:
- (+) Smallest, most targeted change. Pure §1.4 trace-contract
  tightening. Mirrors the gate Sprint 075 already validated.
- (+) Anti-误杀 preserved: legitimate `FINAL_ANSWER` + grounded
  resolutions still get `"resolved"` (Path C is unaffected; the
  CLOSE-arm becomes a no-op when Path C would have fired anyway,
  because Path C runs unconditionally next when the if-arm is
  taken — actually careful: today the `if (CLOSE) … else if
  (isResolvedSuccessTerminal) …` structure means Path C is ONLY
  reached when phase is not CLOSE; this rewrite needs to either
  preserve the if/else-if structure with the gate inlined into
  the CLOSE arm, OR collapse both arms into a single
  `isResolvedSuccessTerminal`-gated path that fires regardless of
  whether phase is CLOSE).
- (–) Some sessions today marked `"resolved"` (incorrectly) will
  flip to `null` containment, surfacing as
  `case_passed=false` on the eval. This is the intended
  honesty-restoration; consumers of `containment_outcome` should
  not be surprised because that's the existing semantics for
  cases that genuinely don't resolve.
- (–) Requires careful audit of `goal_achieved` simulator-preempted
  one-shots: today these go through Path B (LLM emits `next_phase=
  CLOSE` after the grounded answer) AND Path C's gate would also
  approve (FINAL_ANSWER + ANSWERED_SUBTASK + non-empty
  articlesShown). After the rewrite, the gate inside the CLOSE arm
  approves the same set, so the legitimate goal_achieved path is
  preserved. This needs to be confirmed by characterization tests.

**Option C1.B — Introduce a new `"incomplete_no_resolution"`
containment value** (defer to a fold-back sprint)

Same gate as C1.A but instead of leaving containment null on the
else branch, stamp a new explicit value like
`"incomplete_no_resolution"` or `"discover_stalled"`. This makes the
trace explicit about "the LLM closed but no grounded answer was
delivered".

Trade-offs:
- (+) Explicit trace; downstream consumers can branch on it.
- (–) Adds a new enum value that cross-cuts the eval-side
  `ESCALATION_TRIGGER_VALUES` / outcome-class mapping in
  `outcome_checks.py:34-38`. Needs coordinated migration. Same
  reason `D-new-escalation-reason-enum` is deferred — this should
  not be added in a narrow runtime sub-sprint.
- (–) Premature optimisation; the eval already routes
  `case_passed=false` on a null containment in the absence of L2
  evidence. Adding a new enum value without a consumer is
  governance churn.

**Option C1.C — Eval-side gate only**

Tighten the `composite.py:285-303` gate to additionally fail any
case whose runtime-stamped `containment_outcome="resolved"` lacks
any `articlesShown` evidence in the trace minimum.

Trade-offs:
- (+) Pure eval-side change; no runtime risk.
- (–) Treats the symptom (eval credits false success) without
  fixing the root cause (runtime stamps false success). The
  admin trace UI would still show the misleading `"resolved"`
  badge to operators, contaminating manual triage. Violates the
  doc-governance "code is truth" principle.
- (–) Same eval-side gate would need to learn 4 more "resolved
  is suspect" predicates for future Path-B-like surface
  additions. Doesn't generalise.

**Recommendation: Option C1.A.** Smallest, most targeted, mirrors
existing validated gate, runtime-fix-first per §1.4 trace-contract
ownership.

### 4.2 CS2 — Three options

**Option C2.A — Add a structural `user_role` projection slot
derived from form_context + listing-ownership match** (recommended)

Add a new per-turn projection slot, e.g.:

```yaml
user_role:
  inferred_role: "ad_owner" | "third_party_buyer" | "unknown"
  evidence:
    has_ad_id: bool
    email_owns_ad: bool | null  # null if listing not loaded
    self_reference_terms: bool  # message contains "my ad" / "my listing"
```

Implementation surfaces (estimated):
- `ListingLookupService` already loads the listing's `posted_by`
  email (used internally for moderation context). Surface an
  ownership-match boolean (form_context.email matches listing
  posted_by, case-folded, RFC 2606-safe) without leaking the raw
  email — same anti-PII discipline as the existing
  `customer_context_status` enum.
- `ContextProjectionBuilder` adds a new `user_role` projection slot
  emitted in the same `customer_context_status` projection block
  (around `:1383-1449`).
- `discover_triage.yaml` procedure adds a brief soft cue:
  "When `user_role.inferred_role == 'ad_owner'`, frame all
  clarifications from the seller's perspective; do not solicit
  third-party (buyer / lookup-er) behaviour directly. When it's
  `unknown`, ask one focused clarifying question to anchor the
  perspective before proceeding."
- `resolve_faq_grounded_answer.yaml` (UC-A's RESOLVE skill) inherits
  the slot and reinforces the framing.

Trade-offs:
- (+) Structural §3.2 Q3 fix — gives the LLM the inputs to make a
  correct semantic choice (the §1.2 primary principle: rules
  define boundaries, LLM owns semantic understanding). Forward-
  looking: future LLMs will use the signal more reliably; current
  LLMs no longer have to guess.
- (+) No keyword / regex / if-else / enum expansion on the
  semantic layer. The role is data-derived (email + ad_id +
  ownership match), not message-content-pattern-matched. §1.7
  compliant.
- (+) Composable with future cross-session memory work
  (`cross_session_memory_proposal.md` references user-historical
  context; `user_role` is a natural fit).
- (–) Requires plumbing through `ListingLookupService` + per-turn
  projection. Estimated 3-5 file edits + tests; not a one-line
  change. Comparable in size to R4.a (S-Auto-23 `customer_context_
  status` + `ad_reference`).
- (–) The "self_reference_terms" sub-signal risks drifting toward
  keyword matching if implemented as message-content regex. Should
  be left out of the v1 slot or implemented as a small lexical
  signal documented as observability-only, not used for branching.

**Option C2.B — Prompt-side reinforcement only**

Add to `system_prompt.txt` or `discover_triage.yaml` procedure:
"You are speaking to a Gumtree user who may be a seller (ad owner)
or a buyer. When the user references THEIR ad, treat them as the ad
owner; never solicit third-party (buyer) behaviour directly."

Trade-offs:
- (+) Smallest possible change; no runtime work.
- (–) Pure §1.5 forbidden-list territory: this is a "fix semantic
  failures by adding prompt rule" without addressing the
  underlying impoverished projection. The LLM still has to infer
  from message text; the prompt rule helps but doesn't structurally
  solve the problem.
- (–) The same instruction works for some draws and fails on
  others (the same draw variance the current state shows). Without
  a structural signal, the LLM remains stochastic on this surface.
- (–) Doesn't scale to future LLMs the same way C2.A does.

**Option C2.C — Bot self-disambiguates with a clarifying question
on every UC-A session involving an `ad_id`**

Add a soft hint to `discover_triage.yaml`: "On UC-A or UC-FP
sessions where the form carries an `ad_id`, ASK 'Just to clarify,
are you contacting us about your own ad, or someone else's ad?'
before classifying."

Trade-offs:
- (+) Defensive; doesn't depend on structural inference.
- (–) Conversationally clunky; degrades the user experience for
  the dominant case (user IS the ad owner, the question is
  obvious). Violates the §1.2 "flexibility / human touch" intent.
- (–) Doesn't help when the user opens with rich context like CS2
  ("my buyer asked … my ad …") that should be unambiguous.

**Recommendation: Option C2.A.** Forward-looking, structural,
§3.2 Q3-correct, composable with future work. Slightly larger fix
surface but the right shape; defers the C2.B prompt reinforcement
to a thin "use this slot when present" line in the existing skill
procedures (one or two sentences, not a rule cascade).

## 5. Recommended option + rationale

- **CS1: Option C1.A** — Tighten Path B at `ControlKernel.java:575-579`
  to gate the resolved-stamp through `isResolvedSuccessTerminal`.
  Land FIRST, before CS2.
- **CS2: Option C2.A** — Add a `user_role` projection slot derived
  from form_context + listing-ownership match, plumb through the
  DISCOVER + UC-A RESOLVE skills as a soft cue.

**Why this combination**:

1. CS1 is a Runtime §1.4 trace-contract gap — the right authority
   to fix it is the runtime, not the eval.
2. CS2 is a §3.2 Q3 prompt_projection gap — the right fix is to
   feed the LLM the structural signal it needs, not to add a
   prompt rule or a Java guard.
3. CS1 → CS2 sequencing aligns with the M-Auto-5 framework-defect
   priority precedent (§5.8): measurement-honesty before
   semantic-fix so the semantic delta is honestly measured.
4. Both fixes are LLM-first compatible (no keyword / regex /
   if-else / enum expansion on a semantic surface).
5. Both fixes are forward-looking: 6 months from now a smarter
   Gemini / DeepSeek / Kimi will use the same signals more
   reliably; the work doesn't become obsolete.

## 6. Scope split + delivery priority suggestion

Two narrow sub-sprints, sequential, both narrow enough to fit
inside M-Auto-7 or as M-Auto-6 follow-ons after milestone close.

### 6.1 Sub-sprint suggestion S-X (CS1) — Default-resolved gate

**R-item**: `R-controlkernel-default-resolved-on-close-anti误杀`

**Scope**:
1. `ControlKernel.java:575-579` rewrite — collapse the CLOSE-arm
   stamp through `isResolvedSuccessTerminal`. Either inline the
   gate into the CLOSE arm (preserving the existing if/else-if
   structure with the same gate logic) OR collapse both arms into a
   single `isResolvedSuccessTerminal`-gated path that runs
   regardless of `phaseAfter` (cleaner; verify it preserves the
   `handlingState=CLOSED` side-effect on every CLOSE transition).
2. Characterization tests pinning:
   - DISCOVER → DISCOVER → DISCOVER → CLOSE (0 tool calls, no
     UC) → containment is NOT `"resolved"` (either null or new
     value if C1.B is also adopted).
   - RESOLVE → FINAL_ANSWER + ANSWERED_SUBTASK + articlesShown
     non-empty + LLM emits CLOSE → containment IS `"resolved"`
     (preserves the goal_achieved one-shot anti-误杀).
   - RESOLVE → FINAL_ANSWER without articlesShown → CLOSE →
     containment is NOT `"resolved"`.
   - Escalation arm + CLOSE → containment stays `"escalated"`.
   - Sprint 077 Path D downgrade compatibility — `"resolved"` from
     a prior turn + this turn's CLOSE transition does not
     overwrite (it cannot, because Path C's gate also guards on
     non-null containment).
3. Eval-side observability: `containment_outcome=null` cases
   surface in trace JSON with an explicit `containment_reason`
   field (e.g. `"reason_no_grounding_evidence_on_close"`) so
   admin trace UI can render a distinct badge (R3.c
   informational guard rendering pattern is already in place).
   Bonus item; may defer if scope tightening is needed.

**Layer (§3.2)**: `infra` (Runtime trace-contract).

**§7 stanza pre-fill draft**: see §7.1 below.

**Hard fences**:
- No new enum value (defer Option C1.B's `"incomplete_no_resolution"`
  to a coordinated migration sprint).
- No `semantic_planner` edit (the LLM's CLOSE transition decision
  is unchanged; the runtime just stops false-crediting it).
- No keyword / regex / if-else dispatch on user_message content.
- No eval-side override that would mask the new honest signal
  (per §5.4).

**Estimated effort**: 1-2 day dev. Smaller than R5 or R6; comparable
to R2.a wireup.

### 6.2 Sub-sprint suggestion S-Y (CS2) — User-role projection slot

**R-item**: `R-user-role-projection-slot-from-listing-ownership`

**Scope**:
1. `ListingLookupService` surface an `email_owns_ad: boolean`
   helper that compares the form-context email to the listing's
   `posted_by` email (case-folded). Anti-PII: do NOT expose the
   raw posted_by email; only the boolean match.
2. `ContextProjectionBuilder` emit a new `user_role` slot:
   ```json
   "user_role": {
     "inferred_role": "ad_owner | unknown",
     "evidence": {
       "has_ad_id": true,
       "email_owns_ad": true | false | null
     }
   }
   ```
   Treat `email_owns_ad=true + has_ad_id=true` → `ad_owner`;
   otherwise → `unknown` (do NOT label as `third_party_buyer`
   without positive evidence; default to unknown to avoid false
   negatives).
3. `discover_triage.yaml` procedure adds ONE soft sentence (not a
   rule cascade): "When `user_role.inferred_role` is `ad_owner`,
   frame clarifications from the seller's perspective; treat any
   third party (buyer / friend / etc.) the user mentions as a
   reference, not as the source for direct clarification."
4. `resolve_faq_grounded_answer.yaml` (UC-A's primary RESOLVE
   skill) carries the same soft cue in its procedure block (or
   inherits via state_inheritance.soft_signal_via_projection).
5. Characterization tests + a small bad-case suite addition (e.g.
   `cs_seller_buyer_perspective_001`) covering:
   - Seller opens with "my ad" + matching email/ad_id → bot
     frames seller-side.
   - Buyer opens with someone else's ad_id → bot frames buyer-
     side OR asks one clarifying question.
   - Unknown / no ad_id → bot defaults to seller-frame on
     ambiguous "my ad" wording (current behaviour; preserved as
     anti-误杀).

**Layer (§3.2)**: `prompt_projection` (Q3, primary) + a thin
`semantic_planner`-adjacent soft cue in the skill yaml.

**§7 stanza pre-fill draft**: see §7.2 below.

**Hard fences**:
- No new Java guard on user_message content for seller/buyer
  intent classification.
- No regex on "my ad" / "my buyer" / "my listing" tokens for the
  role inference; the inference is data-derived only.
- No new escalation enum value (`seller_buyer_ambiguity` etc.).
- The `user_role.inferred_role` enum stays narrow:
  `ad_owner | unknown` (NOT `third_party_buyer` in v1).
- Anti-误杀: legitimate `ad_owner` sessions where the bot's
  inference is correct must not regress (verify on the curated
  bad-case suite per §5.6).

**Estimated effort**: 3-5 day dev (comparable to R4.a, smaller
than C-2a + C-2b combined). May fit as a single sub-sprint or
split into "S-Y.1 projection slot" + "S-Y.2 skill cue + bad-case
seed" depending on deliver-agent judgement.

### 6.3 Milestone placement

Both R-items are M-Auto-6 carry-overs at most (M-Auto-6 is dev-side
complete; not yet closed). They do not preempt M-Auto-6 close
(neither is a framework-defect under §5.8). Plausible placement:

- After M-Auto-6 milestone close → M-Auto-7 candidate (if
  M-Auto-7 is framed as "DISCOVER hygiene + projection
  enrichment").
- Or queue as standalone post-M-Auto-6 sub-sprints with their own
  small milestone wrapper.

Deliver-agent decision. Research-agent recommends batch both into
one milestone framing (e.g. "M-Auto-7: DISCOVER perspective +
trace-contract honesty") so the milestone-shared §9 re-bless
measures both deltas together; sub-sprints inside are still
sequential (CS1 first, CS2 second).

## 7. Layer classification + §7 stanza pre-fill

### 7.1 CS1 (S-X) §7 stanza draft

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** infra

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The
existing §1.4 Runtime trace-contract (containment_outcome owned by
runtime, must reflect actually-delivered grounding evidence) is
extended into the previously-unguarded CLOSE-transition arm.

**Semantic hardcode:** No semantic hardcode introduced. The change
narrows an existing default-stamp by reusing the
`isResolvedSuccessTerminal` gate (FINAL_ANSWER + grounded
disposition + non-empty articlesShown) already validated by Sprint
075 / S-Auto-20. No new keyword, regex, if-else on user_message
content, or enum expansion.

**Generalization coverage:** target / neighbor / negative / shadow
case counts: <T>/<N>/<G>/<S> — to be filled at sub-sprint open.
Target cases: CS1 trace `3e4f0aad-af7…` + curated DISCOVER-stalled
cases. Neighbor: the goal_achieved one-shot path (RESOLVE →
FINAL_ANSWER + ANSWERED_SUBTASK + articlesShown + CLOSE → still
stamps "resolved", anti-误杀). Negative: escalation arm stays
"escalated"; Path D downgrade unchanged. Shadow: held-out
DISCOVER-stalled traces.
```

### 7.2 CS2 (S-Y) §7 stanza draft

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** prompt_projection

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The
existing §1.4 Runtime ownership of capability boundary (the
projection is Runtime-owned; what it carries is Runtime-decided)
covers the new `user_role` slot. The new slot is OBSERVABLE
EVIDENCE for the LLM; the runtime does not branch on it.

**Semantic hardcode:** No semantic hardcode introduced. The
`user_role.inferred_role` is data-derived (form_context.email +
listing.posted_by ownership match), NOT message-content pattern-
matched. The skill yaml addition is a soft cue ("frame from
seller's perspective when slot says ad_owner") that the LLM owns
per §1.3; no Java guard enforces compliance. No new escalation
enum value.

**Generalization coverage:** target / neighbor / negative / shadow
case counts: <T>/<N>/<G>/<S> — to be filled at sub-sprint open.
Target cases: CS2 trace `89f4ab98…` + curated seller-side
perspective cases. Neighbor: third-party-buyer-perspective cases
(slot defaults to `unknown`; LLM still uses message wording).
Negative: cases where the user's role is genuinely ambiguous → bot
asks one clarifying question; does not auto-classify. Shadow:
held-out seller-vs-buyer perspective cases on UC-A / UC-FP.
```

## 8. Hard fences + non-goals

### Both sub-sprints (CS1 + CS2)

- No keyword / regex / if-else / enum expansion on a semantic
  surface (§1.5 / §1.7).
- No widening of eval CaseSpec to accept the bot's current
  behaviour (§5.4) — the bot's CS1 false-credit and CS2 slip are
  genuine mistakes, not eval-spec rubric errors.
- No editing of `docs/sprints/*` or `docs/archive/*`.
- No editing of the system_prompt to encode CS1 / CS2 fixes
  (CS1 is runtime; CS2 fix lives in projection + soft skill cue,
  not the system prompt's role description).

### CS1 (S-X) specifically

- Do NOT introduce a new `containment_outcome` enum value in this
  sub-sprint. The minimum viable fix is leave-null-on-no-grounding-
  evidence. Option C1.B is deferred.
- Do NOT touch the eval-side composite gate. CS1 is a Runtime
  fix; eval-side is the §5.4 backstop, not the primary lever.
- Do NOT touch `shouldVoidResolvedStamp` / `isResolvedSuccessTerminal`
  — they are already correctly anti-误杀-gated.

### CS2 (S-Y) specifically

- The `user_role` enum stays narrow: `ad_owner | unknown` in v1.
  Do NOT add `third_party_buyer` / `friend` / etc. without
  positive evidence on multiple bad cases first.
- Do NOT expose the raw `posted_by` email through the projection
  (privacy / §07-engineering-constraints PII posture).
- Do NOT add a message-content keyword detector for "my ad" /
  "my buyer" — the role inference is data-derived only.
- Do NOT enforce LLM compliance with the soft cue via a Java
  guard. The skill yaml addition is LLM-soft per §1.3.

## 9. Risk + compounding-effect analysis

### 9.1 CS1 risks

- **R-CS1-1 (medium)**: a session today legitimately marked
  `"resolved"` via Path B (FINAL_ANSWER + ANSWERED_SUBTASK +
  non-empty articlesShown + LLM emits CLOSE) must NOT regress to
  null. Mitigation: characterization tests on the goal_achieved
  one-shot path; spot-check the m-auto-5-baseline corpus for
  resolved-stamp counts before/after.
- **R-CS1-2 (low)**: downstream consumers of
  `containment_outcome=null` (admin UI, eval scoring,
  Salesforce-bound CRM bridge) may render incompletely. Verify
  Admin UI (`SessionList.tsx:145`) renders `'-'` on null correctly
  (it does per the Explore agent's findings); verify eval scoring
  treats null as case_passed=false absent L2 evidence (it does per
  `composite.py:285-303`).
- **R-CS1-3 (low)**: the resolved-stamp count drop after this fix
  will look like a regression in milestone pass-rate metrics.
  This is expected (measurement honesty); document in the
  milestone close evidence so the verdict reviewer interprets the
  drop correctly.

### 9.2 CS2 risks

- **R-CS2-1 (medium)**: false-positive `ad_owner` inference on
  sessions where the email-to-ad ownership match is incidental
  (e.g. shared family email, account hand-off). Mitigation:
  emit `email_owns_ad: null` whenever the listing lookup is
  uncertain; the LLM defaults to `unknown` in that case.
- **R-CS2-2 (medium)**: false-negative `unknown` on legitimate
  seller sessions where the listing lookup failed. Mitigation:
  `unknown` is the safe default; the LLM falls back to message-
  text inference and may slip occasionally — but this is the
  current behaviour, so it's no regression.
- **R-CS2-3 (low)**: drift between `customer_context_status` and
  `user_role` slots (both derived from listing lookup). Verify
  the two are computed from the same source so they cannot
  contradict; document the relationship in the projection comment.
- **R-CS2-4 (low)**: the UC-G label hallucination (§3.2.4) is
  NOT fixed by this sprint. The bot will still occasionally
  pick "UC-G" with an ad-visibility gloss. A separate
  `R-uc-id-projection-with-human-names` follow-on should add
  human-readable names to the candidate_use_cases projection.
  Queued as OBS-CS2.4.

### 9.3 Compounding sequence

**Correct order**: CS1 lands → re-measure baseline (resolved-stamp
count drops; some previously-passing cases now fail) → CS2 lands
→ re-measure baseline (perspective-slip rate decreases on
ad_owner cases). Both measured against the new honest floor.

**Incorrect order (CS2 first, CS1 deferred)**: CS2's perspective-
slip rate decreases on the LLM responses, BUT a fraction of slip
sessions still flow through to `containment_outcome="resolved"`
via Path B, so the measurement is contaminated. The semantic
delta looks smaller than it is. Avoid.

**Both sub-sprints' shared dependency**: the milestone-shared
real-LLM re-bless after CS1 + CS2 needs the M-Auto-6 simfixed-
stalledfix baseline as the prior reference (currently at
`m-auto-5-baseline-20260604-simfixed-stalledfix`; expected to
shift to `m-auto-6-baseline-…` after M-Auto-6 closes). Do not
launch CS1+CS2 before M-Auto-6 closes.

## 10. Observability / trace / report implications

### CS1

- Admin trace UI: `SessionList.tsx:145` outcome column today
  shows `s.outcome` text. After CS1, null containment cases will
  render `'-'` (default placeholder). Consider adding a
  `containment_reason: "no_grounding_evidence_on_close"`
  observability field so the trace viewer can show "incomplete
  (no resolution evidence)" instead of bare `'-'`. Bonus item;
  may defer.
- Eval `report.html`: cases that previously showed
  `containment_outcome=resolved` will shift to `containment_
  outcome=<empty>`. The R3 admin-trace cluster (S-Auto-24)
  already distinguishes informational from blocking events; the
  same pattern can render "incomplete" as a neutral state.
- Aggregate metrics: the per-case `containment_outcome` distribution
  will shift. Document the expected shift in the sub-sprint
  handoff so the milestone-close evidence reviewer knows the
  baseline change is expected.

### CS2

- The new `user_role` projection slot should be visible in the
  admin trace's "Projected Context" panel (already wired via
  the existing projection JSON dump). Verify the panel renders
  the new slot without truncation.
- Add a small dashboard / sweep diagnostic counting per-session
  `user_role.inferred_role` distribution across the bad-case
  suite. A first cut: % `ad_owner` vs % `unknown`. Helps
  calibrate whether the projection coverage is high enough to
  materially shift semantic behaviour.
- The LLM's reasoning text should ideally cite the
  `user_role.inferred_role` slot when framing perspective. Not
  enforced (it's the LLM's judgement per §1.3); observability
  only.

## 11. Coverage check vs `action_bank.md` R-items + active scope

### 11.1 Open / deferred R-items the new sub-sprints touch or adjoin

- **`R-aggregate-retains-per-attempt-composite-l2`** (M-Auto-5 close
  Codex non-blocking observation #3; eval observability). Not
  blocked by CS1/CS2; orthogonal eval framework gap.
- **`R-eval-interactive-judge-score-never-populated`** (LOW;
  chronic by-config). Orthogonal.
- **`R-runtime-escalation-reason-turn-budget-conflated-with-intent`**
  (B3/R5; deferred behind M-Auto-6). Orthogonal — that R-item is
  about escalation_reason; CS1 is about containment_outcome.
- **`R-classifier-non-deterministic-uc-selection-at-temp-zero`**
  (C/R7; deferred behind M-Auto-6). **Adjacent to CS2 §3.2.4**
  (the UC-G label hallucination sub-issue). Note in the CS2
  sub-sprint that UC-id projection enrichment is a downstream
  follow-on; don't bundle.
- **`R-uc-b-customer-context-policy-review`** (Phase 2 §2.10 policy
  question — whether `get_customer_context` should be allowed on
  UC-B). Orthogonal to CS2's user_role slot (the slot lives in the
  projection, not in tool policy).
- **`R-persona-goal-summary-scope-clarity`** (CONDITIONAL on G2
  pattern). Eval-side persona drift; not CS2's runtime perspective
  issue.
- **`R-corpus-coverage-audit-per-uc`** (per-UC corpus coverage).
  Orthogonal.

### 11.2 Gaps surfaced (NOT covered by any existing R-item)

- The Path B default-resolved leak (CS1) is **not in the action
  bank today**. The closest neighbour is the Sprint 077 / S-Auto-22
  `shouldVoidResolvedStamp` work, but that addressed terminal-
  failure downgrade, not phase-transition default-credit. CS1
  is a genuinely new R-item: `R-controlkernel-default-resolved-
  on-close-anti误杀`.
- The user_role / seller-buyer projection gap (CS2) is **not in
  the action bank today**. The closest neighbour is the R4.a
  `customer_context_status` + `ad_reference` work (S-Auto-23),
  but that was about the customer's account/listing lookup
  STATE, not about the user's ROLE in the conversation. CS2 is
  a genuinely new R-item: `R-user-role-projection-slot-from-
  listing-ownership`.

### 11.3 Reference to active milestone scope

M-Auto-6 (currently dev-side complete; close pending) does NOT
cover either gap. M-Auto-6's R8 unblock sequence and the
milestone-shared real-LLM re-bless gate the close; CS1 + CS2
should NOT be folded into M-Auto-6 retroactively. They are
post-M-Auto-6 candidates for M-Auto-7 (or a thin standalone
milestone wrapper).

## 12. Summary table — what to do, in order

| Step | Owner | Action | Gating | Output |
|------|-------|--------|--------|--------|
| 1 | Deliver-agent | Review this proposal; decide whether to accept S-X + S-Y framing | Human approval | Action-bank entry + sub-sprint plan |
| 2 | Deliver-agent | After M-Auto-6 milestone close, open S-X sub-sprint (CS1 default-resolved gate) | M-Auto-6 close + human direction | `docs/sprint_objective.md` for S-X |
| 3 | Dev-agent | Implement S-X per §6.1 + §7.1 | §5.6 bad-case suite green | S-X dev handoff |
| 4 | Review-agent | Codex per-sub-sprint review on S-X | §4.1 nine-question kernel | `APPROVE_S-X` |
| 5 | Human + deliver-agent | Mini re-bless after S-X to capture honest measurement floor | §5.6 + §5.7 | Updated baseline |
| 6 | Deliver-agent | Open S-Y sub-sprint (CS2 user_role projection) | S-X close + new baseline | `docs/sprint_objective.md` for S-Y |
| 7 | Dev-agent | Implement S-Y per §6.2 + §7.2 | §5.6 bad-case suite green | S-Y dev handoff |
| 8 | Review-agent | Codex per-sub-sprint review on S-Y | §4.1 nine-question kernel | `APPROVE_S-Y` |
| 9 | Human + deliver-agent | Full re-bless after S-Y to measure semantic delta | §5.6 + §5.7 | M-Auto-7 close evidence |
| 10 | Deliver-agent | Milestone-shared §9 Codex review + verdict per §4.2 / §4.3 | All gates green | M-Auto-7 close decision |

---

**Anchored evidence checklist (all verified at HEAD `auto-loop-branch`)**:

- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java:575-579` — Path B default-resolved
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java:1418-1469` — `isResolvedSuccessTerminal` gate
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java:1508-1535` — `shouldVoidResolvedStamp` + `voidResolvedStamp`
- `eval_interactive/eval_interactive/scoring/composite.py:285-303` — OQ-S77 #3 vacuous-pass gate
- `server/src/main/resources/prompts/system_prompt.txt:1, 20` — role-agnostic system prompt
- `server/src/main/java/com/gumtree/csagent/service/runtime/FormContextIngestionService.java:59-66` — form_context shape (no role field)
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:1383-1449` — `customer_context_status` + `ad_reference` (no `user_role`)
- `server/src/main/java/com/gumtree/csagent/service/tools/GetCustomerContextTool.java:79-105` — sanitised listing fields (no `posted_by`)
- `server/src/main/resources/config/use-case-registry.yaml:2-7 (UC-A), :44-49 (UC-G)` — UC ID-vs-name registry
- `server/src/main/resources/skills/discover_triage.yaml:19-21` — DISCOVER procedure (no role framing)
