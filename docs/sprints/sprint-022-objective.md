---
title: Sprint 22 — phase 2 line 358 reconciliation + R-item closure (docs-only)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file (sprint objective until close, then archived to docs/sprints/sprint-022-objective.md)
last_reviewed: 2026-05-14
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  Sprint 22 is a narrow docs-only scope-correction sprint triggered by a
  Sprint 22 planning-turn premise-verification check. The check discovered
  that the `R-generator-get-customer-context-policy-mismatch` premise
  Sprint 21 carried forward — that phase 2 forbids `get_customer_context`
  for UC-C / UC-D / UC-F — was based on a UC-H-local prose annotation
  (phase 2 line 358, inside the UC-H-01 YAML block), not on the normative
  cross-UC matrix at phase 2 §2.10.1 line 1098. The cross-UC matrix
  explicitly permits UC-C / UC-D / UC-F. No policy widening is needed;
  what is needed is to reconcile the misleading UC-H-local prose,
  close the routed-to R-item premised on widening, and annotate the
  three affected briefs with correction notes. The substantive bot
  failures the briefs describe are unchanged; they were closed via
  separate dispositions in Sprint 21 (cs_001 approved override) or
  remain open under other R-items (cs_011 wording, cs_259 phase-plan).
---

# Sprint 22 — phase 2 line 358 reconciliation + R-item closure

## Goal

Reconcile the misleading phase 2 line 358 prose annotation with the
normative cross-UC allowlist at phase 2 §2.10.1 line 1098. Close the
`get_customer_context` policy-mismatch story in `docs/action_bank.md`
as not-an-actual-policy-mismatch. Annotate the three affected Failure
Briefs with correction notes (notes-style append, no rewrite). Open
one follow-on R-item capturing the residual behavioural question
(does the bot actually use `get_customer_context` when account-state
matters in UC-C / UC-D / UC-F?). No runtime change. No policy
widening. No CaseSpec / override / judge edits.

## Sprint class

**Docs-only governance / scope correction.** Stanza-exempt under
`docs/current/iteration_governance.md` §7 ("Pure infra, docs-only,
config-governance, and characterization-test sprints are **exempt**
and need not include the stanza."). Precedent: Sprint 15 (config
governance), Sprint 16 (docs + characterization tests), Sprint 17
(G0 docs-only governance bundle), Sprint 18 (G1 brief authoring).
See "Layer-classification + anti-hardcode stanza" section below for
the verbatim exemption declaration.

## Baseline

This is a docs-only governance sprint. No eval baseline applies.
`docs/current_eval_baseline.md` is not consulted by this sprint and
must not be edited.

## Implement only

Seven directives, in execution order:

1. **Reconcile phase 2 line 358 prose annotation.** File:
   `docs/foundational/phase2_domain_realization_spec.md`. Edit only
   the prose annotation on line 358 (inside the UC-H-01 YAML block).
   Reconciliation shape: **α-broader** — rewrite the parenthetical to
   stop enumerating allowed UCs at all, and instead cross-reference
   §2.10.1 (the cross-UC matrix). Suggested edit (dev agent may
   refine wording; intent must be preserved):

   Before:
   ```
     - get_customer_context (限 UC-A/UC-FP/UC-K，对 UC-H 不可，因此仅靠 user_message 提问收集)
   ```

   After (intent-preserving):
   ```
     - get_customer_context (UC-H 不可用 — 见 §2.10.1 cross-UC allowlist；UC-H 仅靠 user_message 提问收集标识符)
   ```

   Rationale: the original prose annotation tried to enumerate the
   allowed UCs inside a UC-H-local YAML block; the enumeration was
   incomplete (omitted UC-C / UC-D / UC-F, which §2.10.1 line 1098
   explicitly allows) and misled three brief authors and one
   Sprint-21 disposition. The cross-UC matrix at §2.10.1 is the
   source of truth; the UC-H-local annotation should restrict its
   claim to UC-H's own constraint ("UC-H cannot call this tool") and
   defer the cross-UC question to the matrix. Recommend the α-broader
   shape over an in-place annotation patch because the latter leaves
   another way for a future reader to mis-anchor.

   Side-effect: bump the doc's `last_reviewed` front-matter field to
   `2026-05-14`. Add a one-line `notes:` entry recording the
   reconciliation (cite "Sprint 22 — reconciled UC-H-01 line 358
   prose against §2.10.1 cross-UC matrix").

2. **Close `R-generator-get-customer-context-policy-mismatch` as
   not-an-actual-policy-mismatch.** File: `docs/action_bank.md` line
   396. The row already carries `status: done` from Sprint 21 (which
   reclassified it `eval_spec` → `product_policy` and routed to
   `R-phase2-uc-cdf-customer-context-policy-widen`). Sprint 22 must
   amend the description to reflect that the underlying policy
   mismatch never existed: §2.10.1 line 1098 (corroborated by
   `docs/customer_service_tool_spec_v0_2.yaml` line 260 and
   `docs/current/customer_service_tool_spec_v0_3.md` line 60)
   already permits UC-C / UC-D / UC-F. The Sprint 21 routing
   decision was based on a misreading of the UC-H-local prose at
   line 358 as the cross-UC rule. Append a one-paragraph correction
   note inline in the row (keep the existing `status: done` Sprint
   21 reference; add a "**Sprint 22 correction:**" addendum).

3. **Close `R-phase2-uc-cdf-customer-context-policy-widen` as
   premise-invalidated.** File: `docs/action_bank.md` line 559 (in
   §5.2 G1 surfaced backlog, "Sprint 21 routed-to R-items"
   sub-section). This is the **operational** closure target — the
   R-item Sprint 21 routed to that carried the widening premise
   forward. Mark `status: done — closed as premise-invalidated by
   Sprint 22`. One-paragraph explanation citing §2.10.1 line 1098 +
   the two corroborating policy surfaces (`customer_service_tool_spec_v0_2.yaml`
   line 260; `customer_service_tool_spec_v0_3.md` line 60). No
   widening was needed; the policy already permitted.

4. **Move both closed R-items to §6 Closed action index per Sprint
   21 close precedent if §6 has been the convention.** Review §6 to
   confirm convention; if Sprint 21 routed-to R-items are kept
   in-table in §5.2 with `status: done` markers (as line 396 is),
   apply the same convention to `R-phase2-uc-cdf-customer-context-policy-widen`
   in §5.2. Add a single Sprint 22 row to §6's archive table
   (`Sprint 22 | phase 2 line 358 reconciliation + R-item closure |
   closed (docs-only governance) | docs/sprints/sprint-022-*`).

5. **Add correction notes to the three affected briefs' Related
   observation sections.** Use the **notes-style appended block**,
   not a rewrite. Append a `> **Correction (Sprint 22, 2026-05-14):**`
   block immediately under the affected bullet point. Files and
   anchors:

   - `docs/diagnostics/failure-briefs/cs001-uc-c-template-escalate-on-faq-miss.md`
     line 67, bullet 2 ("CaseSpec ↔ phase 2 tool-policy conflict on
     `get_customer_context`"). The bullet claims "phase 2 §2.10 line
     358 restricts that tool to UC-A/UC-FP/UC-K (excluding UC-C)".
     Correction note must state that §2.10.1 line 1098 (the
     normative cross-UC matrix) explicitly permits UC-C; line 358
     was UC-H-local prose. No edit to the bullet text itself.

   - `docs/diagnostics/failure-briefs/cs011-uc-d-detailed-description-ignored-on-faq-miss.md`
     line 70 paragraph (the "systematic generator-vs-policy
     mismatch" paragraph). Correction note same shape; cite §2.10.1
     line 1098; note that the "systematic generator-vs-policy
     mismatch" hypothesis was invalidated — the generator was
     correct, the brief misread line 358.

   - `docs/diagnostics/failure-briefs/cs259-uc-f-sprint7-i0-violation-on-payment-question.md`
     line 100, bullet 1 ("Three confirmed instances across three
     UCs. The 'systematic generator-vs-policy mismatch' hypothesis
     from cs_011 is solid now."). Correction note same shape;
     state the hypothesis was invalidated by Sprint 22's
     premise-verification.

6. **Open one new follow-on R-item.** File: `docs/action_bank.md`
   §5.2. R-item name: **`R-uc-cdf-get-customer-context-bot-actual-usage`**.
   Layer hint: `prompt_projection` or `semantic_planner` (NOT
   `product_policy` — the policy already permits). Disposition:
   `proposed (Sprint 22 close 2026-05-14); deferred for future
   investigation sprint`. Source: cs_001 (UC-C) + cs_011 (UC-D) +
   cs_259 (UC-F). Description: the policy permits the bot to call
   `get_customer_context` for UC-C / UC-D / UC-F, but the three
   briefs documented that the bot did not call it on FAQ-miss
   shapes where account-state would plausibly matter. Whether the
   bot's per-turn projection surfaces the right signal to consider
   the tool, or whether the semantic planner systematically
   under-calls the tool, is an open behavioural question.

7. **Write Sprint 22 handoff and refresh `docs/10-handoff.md`.**
   Create `docs/sprints/sprint-022-handoff.md` (NEW). Refresh
   `docs/10-handoff.md` §1 lead to Sprint 22's close. Apply the
   12-section handoff doc contract, but several sections will be
   trivially short for a docs-only sprint (no eval, no anti-hardcode
   self-walk needed — no semantic surface touched).

## Do not implement

Hard fence. No edits allowed outside this list. If any temptation
arises to expand scope, the dev agent must stop and record the
temptation in `docs/action_bank.md` §5.2 as a deferred R-item
proposal rather than acting on it.

- **Other foundational docs** (`docs/foundational/*` except the
  line-358 surface in phase 2).
- **The constitution chain governance docs**
  (`docs/current/iteration_governance.md`,
  `docs/current/doc_governance.md`,
  `docs/current/agent_context_guide.md`).
- **Tier-0 invariants doc** (`docs/runtime_freeze_and_risk_policy.md`).
- **Any sprint archive** under `docs/sprints/sprint-001..021-*.md`.
- **Any failure brief NOT among the three named** in directive 5.
- **All runtime code** (`server/**`).
- **All prompts** (any system_instruction / prompt template surface).
- **All CaseSpecs, overrides, personas**.
- **The Sprint 20 case families**
  (`eval_interactive/case_specs/case_families/**`,
  `eval_interactive/case_specs_shadow/case_families/**`) — cascade
  fence inherited from Sprint 21.
- **All other `eval_interactive/**`** content (judge configs,
  rubrics, harness code).
- **FAQ corpus** (`data/faq/**`, `FAQ-knowledge_include_help_url.csv`).
- **The two corroborating policy surfaces** themselves
  (`docs/customer_service_tool_spec_v0_2.yaml`,
  `docs/current/customer_service_tool_spec_v0_3.md`). They already
  agree with §2.10.1; touching them risks introducing drift.
- **Any judge rubric or judge configuration**.
- **No new Tier-0 candidacy**.
- **No fix for the briefs' substantive failure modes** — those were
  addressed in Sprint 21 L3 batch (cs_001 approved override;
  cs_011 wording / cs_259 phase-plan tracked under separate R-items
  and other Sprint 19 backlog items).
- **No investigation of the new follow-on R-item**
  (`R-uc-cdf-get-customer-context-bot-actual-usage`) — open and
  defer only.
- **No `docs/codex-findings.md` content prepared by the dev agent**
  — Codex writes that in the review pass.
- **No widening of the policy**, since the allowlist already
  includes UC-C / UC-D / UC-F.

## Success metrics

This sprint is "success" if **all** of the following hold at close
commit:

1. `docs/foundational/phase2_domain_realization_spec.md` line 358
   prose annotation no longer enumerates allowed UCs; it
   cross-references §2.10.1 for the cross-UC allowlist; `last_reviewed`
   is `2026-05-14`; the surrounding UC-H-01 YAML block is otherwise
   byte-identical.
2. `docs/action_bank.md` line 396 row (`R-generator-get-customer-context-policy-mismatch`)
   carries a Sprint 22 correction addendum that cites §2.10.1 line
   1098 and the two corroborating policy surfaces.
3. `docs/action_bank.md` line 559 row (`R-phase2-uc-cdf-customer-context-policy-widen`)
   is marked closed-as-premise-invalidated with the same evidence
   citation.
4. The three failure briefs each carry a notes-style appended
   `> **Correction (Sprint 22, 2026-05-14):**` block under the
   correct anchor bullet; no original brief content has been
   rewritten or deleted.
5. A new R-item `R-uc-cdf-get-customer-context-bot-actual-usage` is
   open in `docs/action_bank.md` §5.2 with layer-hint
   `prompt_projection` or `semantic_planner`, disposition `proposed
   / deferred`, and source = cs_001 + cs_011 + cs_259.
6. `docs/sprints/sprint-022-handoff.md` exists; `docs/10-handoff.md`
   §1 leads with Sprint 22.
7. No file outside the "Implement only" list is modified.
8. No CaseSpec, override, judge config, prompt, or runtime code was
   touched.

Out of scope for this sprint's success metrics: any
target/neighbor/negative/shadow case metric (no eval was run); any
architecture-health metric (no semantic surface touched); the
behavioural question opened in directive 6 (deferred for future
investigation).

## Review rule

Codex review on this sprint should return `approve (exemption:
docs-only Sprint 15/16/G1 precedent)` after running the scope
checks listed in the review prompt. Per the per-PR §4.1 Anti-Hardcode
prompt: pure docs-only PRs are exempt and Codex returns `approve`
with the exemption named. The substantive review on a docs-only
sprint is the scope check (correct files touched, correct shape of
edit, no scope drift, follow-on R-item correctly layered). See
`compact/sprint-022-review-prompt.md` for the verbatim review prompt.

The §4.2 sprint-close header format applies regardless:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

## Layer-classification + anti-hardcode stanza — EXEMPT

Sprint 22 is docs-only governance / scope correction. It introduces
no prompt change, no runtime semantic decision change, no eval-spec
change, no judge-calibration change, no override / case-family / FAQ
content change, no Tier-0 candidacy. Per
`docs/current/iteration_governance.md` §7 ("Pure infra, docs-only,
config-governance, and characterization-test sprints are **exempt**
and need not include the stanza"), this sprint qualifies for the
exemption. Precedent: Sprint 15 (config governance), Sprint 16
(docs + characterization tests), Sprint 17 (G0 docs-only governance
bundle), Sprint 18 (G1 brief authoring). No §7 stanza is required;
no Tier-0 invariant is proposed; no semantic hardcode is introduced
(no semantic surface is touched).

The Sprint 21 §1.7-evidence-package gate does **not** apply to this
sprint, because Sprint 22 introduces no approved override, no
rubric edit, no judge-calibration change, and no quotation from
brief source-of-truth fields. The fix-iteration evidence convention
from Sprint 21 is therefore not in force; the dev agent must not
carry forward a Sprint 21-shaped §1.7 burden.

## Stop conditions

The dev agent must stop and surface to the human if any of these
fire during execution:

- Any temptation to touch a file outside the "Implement only" list.
- Any temptation to widen policy (i.e. propose adding UCs to the
  §2.10.1 matrix or to either v0.2.yaml / v0.3.md allowlist).
- Any discovery that §2.10.1 line 1098 actually does **not** permit
  UC-C / UC-D / UC-F (would invalidate the whole sprint premise).
- Any tempting brief edit that goes beyond a notes-style appended
  correction block (e.g. rewriting the brief author's analysis,
  retracting the Layer hypothesis, deleting the original bullet).
- Any temptation to investigate the follow-on R-item
  (`R-uc-cdf-get-customer-context-bot-actual-usage`) inline.

## References

- `docs/current/iteration_governance.md` §7 (stanza-exemption rules).
- `docs/current/doc_governance.md` §"Code ahead of docs" /
  §"Stale references" (governing the reconciliation and brief
  correction-note style).
- `docs/foundational/phase2_domain_realization_spec.md` line 358
  (UC-H-01 YAML block, prose to reconcile) + line 1098 (§2.10.1
  cross-UC matrix, source of truth).
- `docs/customer_service_tool_spec_v0_2.yaml` line 260 (corroborating
  policy surface).
- `docs/current/customer_service_tool_spec_v0_3.md` line 60
  (corroborating policy surface).
- `docs/action_bank.md` line 396 + line 559 (R-items to close).
- `docs/sprints/sprint-021-handoff.md` §3.7 (Sprint 21 routing
  decision based on the misread).
- Sprint 15 / 16 / 17 / 18 sprint_objective archives (stanza-exemption
  precedent).
