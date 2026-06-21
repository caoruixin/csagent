# M-Auto-9 milestone-shared anti-hardcode + close review prompt

You are the **Anti-Hardcode + Milestone-Close Review Agent for Milestone M-Auto-9**
(runtime/orchestration closure). Review the **cumulative M-Auto-9 implementation
range** below as a whole and issue a milestone-level verdict. Both sub-sprints already
passed per-sub-sprint §4.1 Codex (`approve`/`pass`/0); this milestone-shared pass
re-walks the union + confirms the carried residuals are honestly carried, not silently
resolved.

## Loader (minimal)
- `AGENTS.md` (auto-loaded governance) + this prompt.
- Sub-sprint handoffs (dev artifacts, read these): `docs/sprints/sprint-096-handoff.md`
  (WP1), `docs/sprints/sprint-097-handoff.md` (WP2).
- Product decision: `docs/current/m-auto-9-escalate-after-help-product-decision.md`.
- Per-sub-sprint Codex verdicts already recorded: `docs/codex-findings.md` (S-Auto-44,
  S-Auto-45).

## Embedded milestone context (M-Auto-9)
- **Identity:** runtime/orchestration closure DESIGN milestone (charter
  `docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md`, design
  `APPROVE` 2026-06-20). It became a 2-WP **implementation** milestone for WP1+WP2; the
  charter blesses "**no phase-machine change is a first-class valid outcome**."
- **Goal:** make bot-initiated unresolved handovers honestly labeled (WP1); record the
  product/eval decision (escalate-after-genuine-help is a valid terminal; `goal_impossible`
  alone is not an auto-hard-fail); and **prove a recorded SATISFIED+resolve terminal CAN
  land** on a genuinely-satisfiable flow (WP2) — without forcing the frozen phase machine.
- **Layer breakdown + §7:** WP1 = `prompt_projection`/`semantic_planner` + a human-authorized
  **Runtime §1.4 vocabulary** completion (§7 stanza present). WP2 = `eval_spec` companion
  authoring (§7 stanza present). Both §7 REQUIRED.
- **Non-goals / HELD:** no phase-machine / `record_outcome` / premature-resolve-guard /
  `source_ids` / max-turn change; the two PRIMARY CaseSpecs + bars unchanged; **WP0
  (source_ids/promotion) HELD**; canonical baseline + pointer frozen (no re-bless).
- **Acceptance bar (met):** WP1 §7.6 bounded real-LLM PASS no-over-use; WP2 8/11 recorded
  SATISFIED+resolve V3-clearing with 2 PRIMARY 0/11 (no regression) + §7 guards green; the
  §5.6 curated bad-case suite manual review is the deliver+human PRIMARY gate (do NOT
  re-judge it — see Constraints).

## Cumulative scope claim (review this range)
**WP1 — Sprint 096 / S-Auto-44 (`agent_unable_to_resolve` migration):** the code range is
`git diff ad2191b5^..a9d52eec`. Key commits: `ad2191b5` (enum add across the 3 synced
sources `customer_service_tool_spec_v0_3.md` ↔ eval `EscalationTrigger` ↔
`PhaseEvaluator.CANONICAL_ESCALATION_REASONS`, + `ToolDispatcher`, + `EscalationReasonResolver`
set & PRIORITY **50** strictly-lowest, + enum-sync test 23→24); `266e803b` (projection
teaching in `system_prompt.txt` + `ContextProjectionBuilder` + `confirm.yaml:20`
`user_dissatisfied`→`agent_unable_to_resolve`); `34f7eb23` (eval scoring family `bot_limit`,
**observation-only**); `f800618c` (precedence + characterization tests). Per-sub-sprint
Codex: `approve`/`pass`/0.

**WP2 — Sprint 097 / S-Auto-45 (satisfiable companion):** the code range is
`git diff 1f1c155c^..ae958c7f`. Key commits: `1f1c155c` (NEW visible companion CaseSpec
`cs_uc_a_loaded_listing_resolvable` + `_manifest.md` row; uses the already-committed AD-2007
EXPIRED fixture, no fixture change); `9757f695` (zero-LLM characterization tests + an
in-scope stale `bad_cases` anchor-count correction 12→18). Per-sub-sprint Codex: `approve`/`pass`/0.

**Deliver docs (docs-only):** product decision doc; the Sprint 096 close + WP2 launch-readiness
+ this close. Not code; exempt from the kernel.

## Milestone-level questions (beyond the per-PR passes)
1. Re-walk the §4.1 nine-question kernel (below) over the **union** of WP1+WP2 code. Does the
   cumulative change introduce any semantic hardcode the per-PR reviews missed at the seam?
2. **Consistency with the S-Auto-38 escalation-scoring split** (Sprint 092: Part-1 escalate-vs-don't
   = tier-0; Part-2 `escalation_reason_family_match` = observation-only): does WP1's new value +
   `bot_limit` family entry stay observation-only (no new tier-0 reason-label gate)? Confirm.
3. **Consistency with the S-Auto-39 RESOLVE→CONFIRM transition** (Sprint 093): WP2 exercises the
   closure path but makes no runtime change — confirm WP2 did not relax the premature-resolve
   guard or alter the phase machine.
4. **Residual honesty:** confirm the OQ-S93.1 residual (explicit `record_outcome→CONFIRM→CLOSE`
   premature-blocked; resolves land via grounding-gated `isResolvedSuccessTerminal`) is carried
   as an OQ, not silently resolved or papered over; and that WP0 stays HELD.
5. Confirm no PRIMARY CaseSpec / expected outcome / bar was widened, and the canonical baseline +
   pointer were not moved.

## Embedded §4.1 nine-question kernel
```text
You are the Anti-Hardcode Review Agent. The PR below proposes a change to this repo. Your job is
to decide whether the change introduces a semantic hardcode — a keyword / regex / if-else / enum /
per-UC matrix that encodes a decision the LLM is supposed to own under iteration_governance.md
§1.3 — and to issue a verdict.

Scope exemption: pure infra, docs-only, config-governance, and characterization-test PRs are not
subject to this review. If the PR is purely one of those, return `approve` with a one-line note
naming the exemption.

For every other PR, walk these nine questions in order. For each "yes" or each concern, paste the
diff snippet and the reasoning.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix for a semantic decision
   (drift detection, escalation, UC selection, risk classification, follow-up, intake routing)?
2. If yes to (1), is the change justified as protecting a current Tier-0 invariant named in
   docs/runtime_freeze_and_risk_policy.md §1 / §2?
3. Could the same outcome be achieved by projecting a soft signal to the LLM (an additional
   projected slot, a candidate list, a diagnostic flag) instead of a hard branch in Java or the prompt?
4. Does the change encode visible-eval case text, trace-specific phrasing, or a CaseSpec id into
   runtime, prompt, or judge config?
5. Does the change move semantic ownership from the LLM to Java — that is, shrink what
   iteration_governance.md §1.3 says the LLM owns?
6. Does the change add an if-else block to the prompt instead of principle-level or observable-state guidance?
7. Does the change preserve tool schema, capability / permission boundary, PII / safety floor, and grounding floor?
8. Does the PR ship generalization eval coverage — target, neighbor, negative, and shadow cases — and not only the target case?
9. If the change is temporary, does it carry an explicit rollback or sunset plan (downgrade-to-signal trigger, retirement sprint id)?

Return exactly one verdict: `approve` | `approve with downgrade-to-signal follow-up` |
`reject as semantic hardcode` | `needs human architecture decision`.
Do not rewrite the PR. Do not propose a code fix beyond naming the layer in
iteration_governance.md §3 the fix should target.
```

## Carried M-Auto-7 OQs (context; lower-relevance to M-Auto-9 surfaces)
These were carried for a future milestone-shared Codex; the M-Auto-7 registry pilot closed
NO-KEEP without one. Most relevant to M-Auto-9's escalation/closure surfaces: **S-Auto-38**
(escalation-scoring split, `docs/sprints/sprint-092-codex-review.md`) and **S-Auto-39**
(RESOLVE→CONFIRM, `docs/sprints/sprint-093-codex-review.md`) — confirm WP1/WP2 are consistent with
both (questions 2-3). Lower-relevance (autoloop/CS4-readiness, already per-sub-sprint `pass`/0):
OQ-S84.1/.2, OQ-S85.1/.2, OQ-S86a.1, OQ-086b.1/.2, OQ-S87.1/.2/.3/.4, S-Y1.5 NBO-1/3. Note them
as carried; you need not re-adjudicate them here.

## Output format (write to `docs/codex-findings.md`, top, verbatim)
Use the §4.2 sprint-close header:
```
## Milestone Review Decision — M-Auto-9
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```
Then the milestone-level per-question findings (1-5 above) + the kernel verdict for the WP1 and
WP2 code ranges.

## Constraints
- You do NOT edit code or any repo file (read-only `codex exec`). The deliver agent records your
  verdict verbatim.
- You do NOT re-judge the §5.6 curated bad-case human verdict (that is the deliver+human PRIMARY
  gate; you may reference it).
- Cite concrete diff snippets / paths for any concern. If you find a genuine blocking regression,
  say so plainly (the deliver agent will STOP and surface it).
