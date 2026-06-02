---
title: Governance worked examples (Failure Brief + sprint stanza)
doc_tier: reference
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-06-02
review_cadence: every 3-5 sprints
supersedes: []
superseded_by: null
notes: >
  Extracted from docs/current/iteration_governance.md §2 (the
  cs_example_001 example brief) and §7.2 (the hypothetical Sprint 18
  worked example) on 2026-06-02 as part of the Layer A/B always-loaded
  split. Both are illustrative examples (hypothetical CaseSpec id), not
  contracts. Cite as "governance-examples (§2 example)" /
  "governance-examples (§7.2 example)".
---

# Governance worked examples

This reference doc receives the two worked examples that previously
lived inline in `iteration_governance.md`: the §2 Failure-Brief
example (`cs_example_001`) and the §7.2 hypothetical Sprint 18
sprint-objective stanza. Both were moved out of the always-loaded
file on 2026-06-02; the rule text and the templates they illustrate
stay in `iteration_governance.md` §2 and §7.

References to sections that stayed in the always-loaded Layer A read
"iteration_governance §X" (e.g., iteration_governance §1.3,
iteration_governance §1.5) for unambiguity.

## Example brief — `cs_example_001` (from iteration_governance §2)

`cs_example_001` is hypothetical; it is not a real CaseSpec id. It
illustrates the six-field Failure-Brief template in
iteration_governance §2.

**What happened?**
A UC-A user (listing visibility / paid Top Ad) says on turn 2 "and BTW
the replies to my buyer aren't coming through either, can you check?"
The bot continues to stamp `active_use_case=UC-A` and answers the
messaging complaint as if it were a follow-up about the listing.

**What should a good CS agent have done?**
Recognize the topic shift on turn 2, treat the messaging complaint as
a new issue, and reroute to UC-C (Replies / Messaging) — or, if
ambiguous, ask one clarifying question — instead of carrying UC-A
forward.

**Why does this matter?**
Carrying the prior UC through a topic shift corrupts intake, gives an
off-topic answer, and is exactly the failure mode the Constitution's
drift / topic-shift bullet (iteration_governance §1.3) says the LLM is
supposed to own.

**Is this a one-off or a pattern?**
Pattern. UC-A↔UC-C soft-shift is a documented cross-UC scenario
already partly covered by the Sprint 10 §L1 reroute pipeline. The
remaining question is whether the projection / signals reaching the
LLM are sufficient on a soft shift.

**Which layer is likely responsible?**
Most likely `prompt_projection` (the LLM did not see a sufficient
alternate-UC signal on turn 2) or `semantic_planner` (the LLM saw the
signal and still chose UC-A). The iteration_governance §3 checklist
disambiguates.

**What should NOT be done?**
Adding a regex on "replies" / "messages not coming through" to
`DriftDetector` to force the reroute. That is a keyword / regex
semantic hardcode and breaks the Constitution's Iteration rule
(iteration_governance §1.5). The fix lives in `prompt_projection`
(surface a soft alternate-UC signal) or `semantic_planner`, not in
Java pattern matching.

## Worked example — hypothetical Sprint 18 fix for `cs_example_001` (from iteration_governance §7.2)

The iteration_governance §2 brief for `cs_example_001` hypothesized
that the UC-A↔UC-C topic-shift failure lives in `prompt_projection`. A
hypothetical Sprint 18 takes the fix:

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** prompt_projection

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The
Constitution's drift / topic-shift bullet (§1.3 LLM owns) governs the
behaviour the projection enables; the projection itself sits inside
the Runtime's "trace and eval contract" responsibility (§1.4).

**Semantic hardcode:** No semantic hardcode introduced. The new
`alternate_candidate_use_cases` projected slot is a soft signal — a
list of UCs the existing `RuntimeIntentClassifier` already surfaces —
exposed to the LLM through the per-turn projection. The LLM owns
whether to act on it. No new keyword, regex, or per-UC matrix is
added to `DriftDetector` or the prompt.

**Generalization coverage:** target = UC-A↔UC-C drift (including
`cs_example_001` once promoted from a hypothetical brief).
Neighbor = UC-A↔UC-D, UC-A↔UC-F drift on the same projection.
Negative = UC-A single-issue follow-up that should stay in UC-A
(no false positive on the soft signal). Shadow = held-out UC-A↔UC-C
and UC-A↔UC-D drift traces, not visible to the dev agent. Counts
deferred to the G2 case-family sprint; this Sprint 18 stanza names
the required families.
```

A future deliver agent should be able to paste this template into a
new `docs/sprint_objective.md` and fill the four fields without
further interpretation. If filling a field requires guessing intent,
the sprint is not ready to start.
