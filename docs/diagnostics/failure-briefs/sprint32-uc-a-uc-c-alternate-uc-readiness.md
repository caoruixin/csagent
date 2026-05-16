# Failure Brief — UC-A↔UC-C drift / alternate-UC readiness (Sprint 32 case family source)

> **Source:** promoted from the §7.2 worked example in
> `docs/current/iteration_governance.md` (Sprint 18 governance landing).
> The §7.2 hypothesis was hand-authored as a worked example of the
> Failure Brief Template; this file promotes it into a real brief that
> anchors the Sprint 32 case family.
> **Sprint context:** Sprint 31 shipped the `alternate_candidate_use_cases`
> projection slot (Option β, commit `2c1fd41`) and strengthened the T8
> non-enforcement test to parameterise across six slot variants × five
> invariance bars (fix-iteration #2, commit `8d3e73b`). The runtime is
> proven non-enforcing on the slot; whether the LLM **reads** the soft
> signal on the §7.2 shape is the open eval question Sprint 32 exists to
> observe.
> **CaseSpec family target:** `cs32t01_uc_a_uc_c_drift` in
> `eval_interactive/case_specs/case_families/sprint32_alternate_uc/`.
> **Filed:** 2026-05-16 (Sprint 32 dev session).

## What happened?

A UC-A user (paid Top Ad / listing visibility complaint) opens the chat
with a vague ad-support description; on turn 2 or turn 3 the user shifts
topic — "and BTW the replies to my buyer aren't coming through either,
can you check?" — surfacing a distinct UC-C (Replies / Messaging) issue.
Pre-Sprint-31, the bot's per-turn projection carried only the active
`use_case=UC-A` and the bot answered the messaging complaint as if it
were a UC-A follow-up. Post-Sprint-31, the per-turn projection now
carries an `alternate_candidate_use_cases` slot. On the AMBIGUOUS-intake
shape the intake router emits the alternates list at turn 0, after which
the slot is observable to the LLM on every subsequent turn alongside
the active UC.

The Sprint 32 failure-shape question is: when the projection carries
`alternate_candidate_use_cases` with UC-C in the list and the user
seeds a UC-C drift on turn 2 or turn 3, does the LLM (a) reroute to
UC-C, (b) ask a focused clarifying question, or (c) continue stamping
`active_use_case=UC-A` and answer the messaging complaint inside the
UC-A frame?

The case-family corpus (target + neighbor + negative + shadow) ships
the eval surface that observes the answer.

## What should a good CS agent have done?

Recognize the topic shift on turn 2 or turn 3. Observe that the soft
signal in `alternate_candidate_use_cases` predicts a plausible UC-C
read of the new ask. Either:

1. *Reroute to UC-C* — flip `active_use_case` to UC-C, call the
   appropriate UC-C tools (`search_knowledge`, `resolve_article`,
   `record_outcome`), produce a grounded answer about message delivery /
   notification settings / inbox visibility, and continue the
   conversation as a UC-C issue.
2. *Ask one focused clarifying question* — "Got it — would you like me
   to look at the visibility issue first, or shall we start with the
   replies-not-coming-through issue?" — and route based on the user's
   reply.

Either path is acceptable. The forbidden shape is: continuing to stamp
UC-A through the drift and answering the messaging complaint as a UC-A
follow-up, ignoring the soft signal in the projection.

## Why does this matter?

Drift / topic-shift is **LLM-owned** per the Constitution
(`docs/current/iteration_governance.md` §1.3). The Constitution's
Iteration rule (§1.5) and forbidden-list (§1.7) explicitly carve out a
keyword / regex / per-UC matrix in `DriftDetector` as the wrong fix.
The Sprint 31 projection slot is the soft-signal alternative: the
runtime now surfaces the AMBIGUOUS-intake alternates list to the LLM
on every turn, and the LLM owns whether to act on it.

The §7.2 worked example is the canonical eval surface for this LLM
behaviour. Without a case-family corpus, the visible eval cannot
observe whether the LLM does in fact read the soft signal. The
Sprint 31 fix-iteration #2 strengthened T8 covers the **runtime**
non-enforcement proof (six slot variants × five invariance bars); the
Sprint 32 family covers the **LLM-behaviour** proof (target =
drift on the §7.2 shape; negative = soft-signal-observable-but-not-
acted-on shape).

Failure to reroute on the target shape is a containment-rate failure
(off-topic answer to the drift, original issue may also fall out of
scope) and a customer-experience failure (user has to re-state the
new issue, the bot looks unaware). The volume risk is moderate: the
four AMBIGUOUS-intake Sprint 31 smoke cases
(`cs_interactive_015 / 040 / 176 / 192`) confirm the AMBIGUOUS-intake
+ topic-shift shape is reachable in real traces.

## Is this a one-off or a pattern?

`pattern`. The drift / topic-shift shape covers any UC pair sharing
an intake topic-subject family. Observable evidence:

- **`cs_interactive_015`** alternates `["UC-A","UC-FP","UC-H"]` →
  UC-A↔UC-FP and UC-A↔UC-H are reachable drift pairs.
- **`cs_interactive_192`** alternates `["UC-A","UC-B","UC-FP"]` →
  UC-A↔UC-B and UC-A↔UC-FP are reachable drift pairs.
- **`cs_interactive_176`** alternates `["UC-E"]` (Sprint 31
  reference recorded `["UC-E","UC-K"]`; the 2026-05-16 dev re-extract
  shows `["UC-E"]` only) → UC-E↔X is reachable.
- **`cs_interactive_040`** alternates 12-element fallback set →
  the AMBIGUOUS-fallback shape itself is reachable when
  `session.getActiveUseCase()` is null at first projection.

UC-A↔UC-C is the §7.2 hypothetical pair; the dev SHALL author the
target CaseSpec to exercise this shape and observe whether the intake
router emits UC-C as an alternate on the authored intake context.
The Sprint 32 family neighbors cover at least two of the
**observably-emitted** UC pairs from the list above, so the family
generalizes beyond the hypothetical pair regardless of whether the
intake router emits UC-C for the target.

## Which layer is likely responsible?

`eval_spec` per `docs/current/iteration_governance.md` §3.2 Q6. The
runtime ships the soft signal (Sprint 31 §4.1–§4.5 — the AMBIGUOUS
intake snapshot + V14 Flyway migration + Hibernate DTO + projection
builder); the prompt ships the teaching paragraph (Sprint 31 §4.6);
the runtime non-enforcement is parametric-tested across six slot
variants × five invariance bars (Sprint 31 fix-iteration #2). The
eval surface — the CaseSpec corpus — is the only gap, and it is the
gap Sprint 32 closes.

Secondary layer: `prompt_projection` (the soft signal lives in the
per-turn projection). If the Sprint 32 smoke surfaces a shape gap
(e.g., the intake router does not emit UC-C in alternates on the
target shape), the deliver-agent registers a follow-on R-item rather
than the dev attempting a runtime fix in-sprint.

## What should NOT be done?

The tempting-but-wrong fix is to add a regex on `replies` / `messages
not coming through` / `buyer` (or a UC-C keyword bag) to
`DriftDetector` to force the reroute. This is a keyword / regex /
per-UC matrix semantic hardcode for a soft semantic decision the
Constitution explicitly assigns to the LLM (§1.3 LLM owns drift
detection; §1.5 Iteration rule; §1.7 forbidden list).

The Sprint 31 fix-iteration #2 already strengthened the runtime
non-enforcement proof to lock the Java side down across six slot
variants × five invariance bars. Adding a `DriftDetector` keyword
branch would (a) re-introduce a semantic hardcode the iteration
fence is designed to prevent, (b) break the parametric T8 invariance
bars on at least the `unrelated-UCs-only` and `single-alternate`
variants, and (c) shift LLM-owned semantic ownership back to Java —
the exact Constitution-§1.3 boundary inversion the §1.7 forbidden
list rules out.

If the Sprint 32 family reveals that the LLM does NOT read the soft
signal on the target shape, the correct next step is a
prompt-projection or semantic-planner sprint that increases the
soft-signal salience (e.g., a more pointed teaching paragraph, an
additional projected slot, a follow-up clarifying-question diagnostic),
not a Java keyword branch.
