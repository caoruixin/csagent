---
title: "No active dev sub-sprint — Sprint 095 / S-Auto-43 (M-Auto-9 WP1) CLOSED BLOCKED (escalation-reason vocabulary gap)"
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-20
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  There is NO active dev sub-sprint. The first M-Auto-9 dev sub-sprint — Sprint 095 /
  S-Auto-43, WP1 `user_requested` escalation-reason honesty — was launched and closed
  **BLOCKED** at the §6 existing-reason honesty gate: no member of the canonical
  23-value `escalation_reason` enum honestly labels the bot-initiated
  "exhausted-resolution / unresolved / needs-human" handover, and the contract forbade
  shipping a knowingly-inaccurate catch-all (`service_degraded`) or adding a new enum
  value. NO code was shipped (tree returned to the documented baseline). BLOCKED is one
  of the two contract-sanctioned terminals — WP1 is NOT declared complete; the defect is
  NOT fixed. The honest fix is the deferred `D-new-escalation-reason-enum` migration (a
  new low-priority bot-initiated reason), which is its own objective and is NOT yet
  scoped or launched. Active milestone = M-Auto-9 (design APPROVED; charter
  docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md). WP0
  (source_ids/promotion) and WP2 (CONFIRM record-vs-handover on satisfiable flows) remain
  HELD under charter §6. Archived contract: docs/sprints/sprint-095-objective.md; dev
  handoff: docs/sprints/sprint-095-handoff.md.
---

# Current sub-sprint state — none active

## Status

**No active dev sub-sprint.** Sprint 095 / S-Auto-43 (M-Auto-9 WP1 — `user_requested`
escalation-reason honesty) is **CLOSED — BLOCKED** (escalation-reason vocabulary gap).

## What closed (Sprint 095 / S-Auto-43, WP1 — BLOCKED)

WP1 set out to stop the LLM mislabeling bot-initiated handovers `user_requested` and
have it use an accurate bot-initiated reason from the **existing** approved 23-value
`escalation_reason` enum. The dev verified the defect read-only (LLM supplies
`user_requested` verbatim in `request_handover`; it is priority-1/Tier-0 in
`EscalationReasonResolver`, so it sticks) and did the three-class attribution (genuine
user-requested / bot-initiated mislabel / runtime budget-family).

The **§6 existing-reason honesty gate fired**: every bot-initiated mislabel carries one
semantic — *"the bot exhausted its grounded help on an in-scope, non-intake issue; the
problem is unresolved; a human must take over."* Walking all 23 canonical reasons, **none
is honest**: the user-signal trio needs a user signal that is absent; the
safety/dispute/compliance/intake values are UC-bound to risk categories not present;
`out_of_scope` is bound 100% to non-V1 intent (`phase2_domain_realization_spec.md:564,582`)
so it would falsely reclassify an in-scope issue; `service_degraded` is the §6-forbidden
inaccurate catch-all. Per the binding rule (an honest existing reason **or** a recorded
BLOCK — never a knowingly-inaccurate label), the dev shipped **no code** and closed
**BLOCKED**. The gap is independently corroborated by the pre-existing `confirm.yaml:20`
which already prescribes the non-canonical `user_dissatisfied` (→ silently
`service_degraded`): the skill author also reached for a bot-initiated reason that does
not exist canonically.

- Archived contract: `docs/sprints/sprint-095-objective.md`.
- Dev handoff (attribution + 23-reason honesty table + vocabulary-gap analysis):
  `docs/sprints/sprint-095-handoff.md`.
- No semantic surface changed → §4.1 Codex N/A; no eval rerun (no behaviour change).

## What this does and does NOT mean

- **The `user_requested` mislabel is NOT fixed.** WP1 is BLOCKED, not complete. The
  pre-existing dishonest `user_dissatisfied → service_degraded` stand-in also remains.
- **WP1 never bore on the M-Auto-9 PRIMARY closure / product-contract question** (the
  unsatisfiable-persona problem; making `record_outcome(resolve)` land). Unchanged.
- **WP0 and WP2 remain HELD** under charter §6.

## Next action

**Human decision: open the `D-new-escalation-reason-enum` migration as its own
objective** (recommended by the WP1 handoff §10). The honest fix is a dedicated
low-priority bot-initiated reason (semantics: *the agent exhausted its grounded
resolution and a human must take over*), wired in lockstep across
`EscalationReasonResolver` (enum + Tier-3 priority slot) + `ContextProjectionBuilder` +
the eval-side `EscalationTrigger` (`eval_interactive/.../case_spec/schema.py`) + the
scoring layer (`escalation_reason_family_match`) + `confirm.yaml` + projection teaching
that reserves `user_requested` for genuine requests. This cross-cut is exactly why it is
a coordinated migration (`action_bank.md` §4 `D-new-escalation-reason-enum`), not a WP1
label tweak. **It is NOT yet scoped or launched** — the deliver-agent scopes it (with the
§7 stanza + the anti-误杀 / precedence guards) only on human go-ahead.

The migration is the honest path to the WP1 objective; it does **not** by itself solve
the PRIMARY closure question. M-Auto-9 WP0/WP2 and all M-Auto-7 holds stay in force.

## Holds (in force)

No exp-91; no mutable-surface change; no WP1-B; no WP2; no objective-alignment
annotation; no posterior/bounded re-sampling; no re-bless; no baseline movement; no
canonical-pointer change. exp-82 stays WITHDRAWN. WP0 (source_ids/promotion) HELD. No new
`escalation_reason` enum value is added until the `D-new-escalation-reason-enum` migration
is scoped + approved.
