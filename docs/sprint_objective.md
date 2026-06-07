---
title: Sprint 085 / S-Auto-30 (M-Auto-7 S-X) — CS3 DISCOVER synthesised-null-turn provenance gate
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-08
review_cadence: per sprint
supersedes: docs/sprints/sprint-084-objective.md
superseded_by: null
notes: >
  Second autoloop launch blocker of M-Auto-7. Root cause TRACE-CONFIRMED
  against CS3 session 33edc1eb (eval_interactive/results/20260607-095759):
  turn-2 LLM emitted user_message="" + no tool_calls; ActionParser.java:70-72
  substitutes the placeholder "I'm looking into this for you." AT PARSE TIME,
  so AgentRunLoopImpl sees a NON-BLANK userMsg and the R2.a clarification
  counter increments on the runtime-synthesised placeholder (count 1->2 →
  turn-3 force-escalate clarification_budget_exhausted before the LLM sees
  the user's "Thank you"). The proposal's Option C3.A (userMsg.isBlank()
  guard) is CONFIRMED INERT — userMsg is never blank at the loop. Fix =
  structural provenance flag (NOT a content heuristic; keeps the R2.a
  "structural cardinality only" design). Source: CS3/CS4 proposal §2 +
  §8.2 (mechanism corrected per trace).
---

# Sprint 085 / S-Auto-30 — CS3 synthesised-null-turn provenance gate

## Class

- **Layer (§3.2):** infra (R2.a counter provenance) + prompt_projection
  (complementary soft DISCOVER cue).
- **§7 stanza:** REQUIRED (the soft DISCOVER cue touches a skill-yaml
  semantic surface). Stanza below.
- **Milestone role:** Phase 1 autoloop launch blocker #2.

## Goal

A runtime-synthesised DISCOVER placeholder (injected by ActionParser
when the LLM null-turns) must NOT be counted as a clarification round
and must not burn the clarification budget. Mark the synthesised
placeholder with a structural provenance flag and exclude it from the
R2.a counter; genuine LLM-authored clarifications keep their existing
structural counting (anti-误杀).

## Scope

1. **`ParsedAction.java`** — add `private boolean userMessageSynthesised;`
   (Lombok `@Data @Builder` → `isUserMessageSynthesised()` getter, builder
   `.userMessageSynthesised(...)`; primitive default false).
2. **`ActionParser.java:70-73`** — when the null-turn fallback fires
   (`toolCalls.isEmpty() && userMessage.isBlank()`), continue substituting
   `"I'm looking into this for you."` AND set the builder flag
   `userMessageSynthesised=true`. The normal path and the parse-failure
   `buildFallback()` (:120-130) leave it false — the flag marks ONLY the
   null-turn placeholder, not the handover apology.
3. **`AgentRunLoopImpl.java:455-457`** — exclude the synthesised placeholder
   from the counter: add `&& !action.isUserMessageSynthesised()` to the
   increment condition. Keep the existing structural guards intact
   (`isDiscoverFreeTextClarification(plan.phase(), false, ucCommittedThisTurn,
   userMsg)` — DISCOVER + no tools + no UC commit). Do NOT add any content
   check (no `isClarificationMessage` gate — that would convert the
   structural counter into a content heuristic; explicitly out of scope per
   the human decision 2026-06-08).
4. **Trace event** — surface `user_message_synthesised=true` (or a
   `discover_null_turn_synthesised` per-turn diagnostic) when the flag is
   set, via the least-invasive existing per-turn trace/diagnostic surface,
   so eval/admin can see the null-turn without it forcing escalation.
5. **`discover_triage.yaml`** — add ONE §1.3-soft cue (near the existing
   Sprint-33 cue) discouraging null/filler turns: every DISCOVER turn should
   either call `classify_use_case` or ask one focused clarifying question;
   do not emit an empty `user_message` / empty `tool_calls` / placeholder
   filler. Complementary only — the provenance flag is the hard backstop;
   do NOT rely on the cue as the sole fix.

## Hard fences / STOP conditions

- Do NOT raise `max-clarification-rounds` (`control-policy.yaml:1-2`); the
  cap of 2 is correct — the bug is the false count, not the cap.
- Do NOT touch the shared placeholder string in `templates.yaml` /
  `PhaseEvaluator` / `ControlKernel` (used on legitimate slow-LLM paths).
- Do NOT touch the pre-LLM budget gate at `ControlKernel.java:288-318`.
- Do NOT add an `isClarificationMessage` / any content/keyword check to the
  counter (structural provenance only — human decision 2026-06-08).
- Do NOT change the RESOLVE-intake counter bucket (S-Auto-25 R2.a#5-ext).
- No Java guard on empty `user_message` (the soft cue is the LLM-side lever).
- STOP + surface an OQ if excluding synthesised turns regresses a genuine
  over-clarification session (test #6) — do not weaken the structural guard.

## Test / eval requirements (all 6 mandatory)

1. blank `user_message` + no tool_calls → ActionParser synthesises the
   placeholder AND `userMessageSynthesised=true` (ActionParser unit test).
2. that synthesised placeholder turn → `clarificationCount` does NOT
   increment (AgentRunLoop test).
3. genuine clarification question (non-blank, `?`-shaped) → `clarificationCount`
   increments (anti-误杀).
4. non-synthesised non-blank reply → existing structural counting behavior
   unchanged (`userMessageSynthesised=false` → counts as before).
5. CS3 anchor `33edc1eb` shape (3-turn DISCOVER stall) → placeholder no
   longer pushes count to 2; no premature `clarification_budget_exhausted`
   force-escalate; turn-3 user message reaches the LLM.
6. anti-误杀: a genuinely over-clarifying session still hits the cap of 2
   and escalates as before.
- Focused Java suite: no new regression vs the post-S-A baseline. Report
  counts. **No real-LLM re-bless in S-X** — Java/unit + diff verification
  only; the pre-pilot baseline re-bless runs after S-Y1 (batched cadence).

## §7 stanza

**Target failure layer:** infra (R2.a counter provenance) + prompt_projection
(soft DISCOVER cue).
**Tier-0 invariant:** none added. Restores the §1.4 Runtime clarification-
counter contract (a clarification round = an LLM-authored clarification
attempt) by excluding runtime-synthesised placeholders via a structural
provenance flag.
**Semantic hardcode:** none. The flag is structural provenance (was the
reply LLM-authored or runtime-synthesised), NOT a content/keyword/similarity
heuristic — the R2.a "structural cardinality only" design is preserved. The
discover_triage.yaml cue is a §1.3-soft sentence, no Java enforcement.
**Generalization coverage:** T/N/G/S = 1 / 1 / 2 / ≥1 — target = CS3 trace
33edc1eb; neighbor = legitimate `?`-clarification still counts; negative =
non-synthesised non-blank reply unchanged + RESOLVE-intake bucket unchanged;
shadow = held-out DISCOVER null-turn traces.

## Codex review plan (§4.3)

Per-sub-sprint Codex deferred to the M-Auto-7 milestone-shared close review.
No §4.3 trigger fires: not Tier-0; not §1.7-adjacent (structural provenance
flag + §1.3-soft cue, no keyword/regex/enum); not hard-fence; not
fix-iteration. Dev does NOT dispatch Codex.

## Handoff requirements

Dev authors `docs/sprints/sprint-085-handoff.md`: §0 evidence (6 tests
green; Java suite counts); §11 Codex deferral + any OQs; §12 note that the
counter-honesty effect is verified at the pre-pilot re-bless (post-S-Y1),
not in S-X.

## Commit discipline

Stage only authorized files: `ParsedAction.java`, `ActionParser.java`,
`AgentRunLoopImpl.java`, `discover_triage.yaml`, + the test file(s). No
`git add -A`. New commit per fix; no `--amend` across a failed hook.
