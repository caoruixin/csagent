# Codex findings (LIVE)

This is the **live** Codex review ledger. Per-sub-sprint §4.1 verdicts and the
sprint-/milestone-close §4.2 headers accumulate here during the active milestone,
then are archived to `docs/milestones/M<N>_codex-review.md` at milestone close and
this file is reset to this scaffold.

**Conventions:**
- Per-PR / per-sub-sprint verdict (§4.1): `approve` | `approve with downgrade-to-signal
  follow-up` | `reject as semantic hardcode` | `needs human architecture decision`.
- Sprint-/milestone-close header (§4.2):
  ```
  ## (Sprint|Milestone) Review Decision — <id>
  decision: pass | fix_required | out_of_scope_review
  blocking_count: <number>
  summary: <one paragraph>
  ```

**Last archived:** M-Auto-11 → `docs/milestones/M-Auto-11_codex-review.md` (2026-06-22;
WP1 TARGET CaseSpec anti-hardcode review `approve`/0; milestone-close `pass`/0 — single
additive characterization CaseSpec, no semantic hardcode / oracle-weakening / forbidden
surface). Prior: M-Auto-10 → `docs/milestones/M-Auto-10_codex-review.md`; M-Auto-9 →
`docs/milestones/M-Auto-9_codex-review.md`.

---

## Sprint 105 (perf-replan wave) — §4.1 per-sub-sprint review — 2026-07-26

Branch `sprint-105-eval-verdict` @ `a2af44cb`; prompt
`compact/sprint-105-review-prompt.md`; dispatched read-only via `codex exec`
(`model_reasoning_effort=high`). Full transcript archived at
`docs/sprints/sprint-105-codex-review.md`. Recorded verbatim below.

**Dispatch note:** the Sprint 105 dev prompt §4 forbids the dev agent from
authoring its own review prompt or dispatching Codex. Both were done here
under explicit human instruction (2026-07-26), which is the recorded override.

```
## Sub-sprint Review Decision
decision: pass
blocking_count: 0
summary: I exempt `291b1023`, `d48057d6`, and `050a98c0` under the kernel’s scope clause as stale-test, instrumentation, and schema-diagnostic work; I reviewed `2220ed54` and `a6d43fae` on the merits. Those two commits repair eval measurement without moving runtime semantic ownership: `2220ed54` stops treating absent L3 signal as failed L3 signal while preserving the `0.7` bar, the gating-dimension set, and the OQ-S77 no-evidence gate, and `a6d43fae` adds a D1/D2/D3 ladder derived from recorded trace facts rather than transcript wording. The reverted always-on quality floor was the right call because it would have made the ruler looser by permanently discarding advisory deductions once a gating dim always existed, and the handoff’s corrected claims are now materially accurate, including the reroute of the `resolved`/`user_state` hole to `eval_spec` rather than `server/**`.

1. Yes, but non-blocking and eval-only: `a6d43fae` adds ordered branches such as `if resolved and not f.handover_requested and f.grounded_artifact: ... -> D1` / `if f.attempted_retrieval and f.grounded_artifact: ... -> D2`, and `2220ed54` adds `elif measured_advisory_l3: judge_basis = "advisory_fallback"`; these branch on recorded trace facts and measured-signal presence, not keywords, regexes, per-UC matrices, or bot text.
2. No: those branches are not justified as protecting a current Tier-0 invariant, and they do not pretend to be; they sit in `eval_spec`/`infra` territory under §3, not `java_guard`, so no Tier-0 expansion is being smuggled in.
3. No: a soft-signal projection to the LLM would not fix an offline scoring defect; `rescore` and the composite/ladder logic need deterministic replay over persisted traces, not new runtime or prompt inputs.
4. No: no runtime/prompt/judge path encodes visible-eval text, trace-specific phrasing, or CaseSpec ids; the ladder never receives transcript text, and `050a98c0`’s `spec_context` is diagnostic-only and pinned not to round-trip into written specs.
5. No: `git diff HEAD~5 -- server/` is empty, and nothing here reassigns user-goal, UC, escalation posture, or wording from the LLM to Java; the new logic measures already-produced traces after the fact.
6. No: no prompt if-else block was added; the new branching is confined to Python scoring code (`composite.py`, `containment_ladder.py`) plus metadata on judge fallbacks.
7. Yes: tool schema, capability boundary, safety floor, and grounding floor are preserved; there is no `server/**` change, the moved case still carries the advisory groundedness deduction (`0.8667`, not `1.0`), and the no-evidence gate remains intact.
8. Yes, within the available surface: the PR ships target, neighbor, and negative coverage via 17 rescored sessions, focused synthetic guard tests, and an independent 11-session substrate with zero verdict moves; shadow is explicitly unavailable by contract rather than silently omitted.
9. N/A: this is not a temporary allowlist or sunset patch, so no rollback trigger is required.

F1 — Yes: the new ruler is more discriminating overall, not broadly more permissive; exactly 1 of 17 in-sprint sessions moved, that rise is transcript-defensible, and 0 of 11 independent Sprint 104 sessions moved even though 10/11 gained a real advisory judge term.

F2 — Yes: S-Eval-5 was extended into a case it did not contemplate rather than weakened where it already applied; `compute_composite` still uses gating dims exclusively when any measured gating dim exists, and the flipped tests preserve the prior assertions verbatim in their docstrings.

F3 — Yes: the D1/D2/D3 tier is derived from trace facts, not bot wording; `containment_ladder.py` reads `tool_calls`, grounded-artifact facts, `source_citation_present`, `handover_completeness`, and turn indices, and `test_tier_is_derived_from_trace_facts_not_bot_wording` pins that shape.

F4 — No: nothing lowered a bar; `test_the_pass_threshold_was_not_lowered`, `test_the_two_gating_l3_dims_are_the_only_non_advisory_ones`, and `test_oq_s77_no_evidence_gate_survives_renormalisation` explicitly pin the threshold, gating-dimension set, and zero-evidence refusal.

kernel verdict: approve
```

---

_(no other active findings)_
