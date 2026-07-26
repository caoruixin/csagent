---
title: "Sprint 105 — §4.1 Codex anti-hardcode review (verbatim transcript)"
doc_tier: sprint-archive
status: current
implementation_status: historical
source_of_truth: this file (verbatim Codex output)
last_reviewed: 2026-07-26
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Read-only `codex exec` run against compact/sprint-105-review-prompt.md on
  branch sprint-105-eval-verdict @ a2af44cb, model_reasoning_effort=high,
  2026-07-26. Verdict pass / blocking_count 0 / kernel `approve`; the same
  verdict block is recorded in docs/codex-findings.md. Reproduced verbatim
  and unsoftened, including the tool-call trace and the harness's own
  `codex_models_manager` cache-TTL warnings (benign; unrelated to the review).
---

# Sprint 105 — §4.1 Codex review, verbatim

Dispatched under explicit human instruction 2026-07-26, which overrides the
dev prompt §4 fence ("do not author your own review prompt and do not
dispatch Codex"). Nothing below is edited.

```text

exec
/bin/zsh -lc "rg -n \"_ADVISORY_DIMENSIONS|premature_finish|stall_quality|judge\\(\" eval_interactive/eval_interactive/scoring/llm_judge.py" in /Users/caoruixin/projects/csagent-wt-105
 succeeded in 0ms:
61:    ``iteration_governance.md`` §5.6 + milestone §5). ``premature_finish``
62:    and ``stall_quality`` keep their existing default-``"critical"``
102:    _ADVISORY_DIMENSIONS = frozenset({
114:        "premature_finish",
115:        "stall_quality",
149:    def judge(
164:            "premature_finish": lambda: self._judge_premature_finish(trace, transcript),
165:            "stall_quality": lambda: self._judge_stall_quality(trace, transcript),
385:    def _judge_premature_finish(self, trace: TraceData, transcript: list[dict]) -> JudgeResult:
406:        return self._call_llm("premature_finish", prompt)
408:    def _judge_stall_quality(self, trace: TraceData, transcript: list[dict]) -> JudgeResult:
427:        return self._call_llm("stall_quality", prompt)
438:        dims, critical for ``premature_finish`` / ``stall_quality``)
520:        (``premature_finish`` / ``stall_quality``) keeps the legacy
524:        return "advisory" if dimension in cls._ADVISORY_DIMENSIONS else "critical"

2026-07-26T03:09:31.421595Z ERROR codex_models_manager::manager: failed to renew cache TTL: missing field `supports_reasoning_summaries` at line 88 column 5
exec
/bin/zsh -lc "sed -n '216,248p' eval_interactive/eval_interactive/scoring/outcome_checks.py" in /Users/caoruixin/projects/csagent-wt-105
 succeeded in 0ms:
          and an ``escalation_reason``. Otherwise the fallback scores
          0.5 (partial credit so the outcome still beats a true
          mismatch but does not pass the L2 mandatory gate).
        - primary=escalate, actual=resolve: bot must have produced a
          source-cited answer turn (``trace.turns`` containing a
          turn with non-empty ``source_ids``). Otherwise 0.5.

        Falls back to the strict ``outcome_class`` match when
        ``acceptable_outcomes`` is empty, preserving legacy behaviour.

        S-Auto-40 (WP1-A): when the CaseSpec carries a declarative
        ``conditional_outcome_acceptance`` block, the verdict is delegated to
        the generic conditional-outcome evaluator instead. That evaluator
        reads the Phase-1 ``user_state`` signal and a closure-quality marker
        and returns PASS only for a SATISFIED→resolve path or an
        adjudicated UNRESOLVED→escalate path; a positively-UNRESOLVED
        escalation without an adjudication artifact scores 0.0
        (``CONDITIONAL_ELIGIBLE`` / ``REVIEW_REQUIRED``), so it never
        auto-passes the mandatory gate. This is a §5.4 product-authorized
        ``eval_spec`` acceptance, NOT an unconditional ``resolve OR escalate``
        widen.
        """
        if getattr(case_spec, "conditional_outcome_acceptance", None):
            from eval_interactive.scoring.conditional_outcome import (
                evaluate_conditional_outcome,
            )

            cr = evaluate_conditional_outcome(case_spec, trace)
            return OutcomeCheckResult("correct_outcome", cr.score, cr.detail)

        expected = case_spec.expected.outcome_class.lower()
        raw_outcome = trace.session_state.containment_outcome.lower()
        actual = _OUTCOME_MAP.get(raw_outcome, raw_outcome)

2026-07-26T03:09:34.523148Z ERROR codex_models_manager::manager: failed to renew cache TTL: missing field `supports_reasoning_summaries` at line 88 column 5
codex
The reroute claim is substantiated in code: `correct_outcome` only reads `user_state` through `conditional_outcome_acceptance`, so the post-handoff move from `server/**` to `eval_spec` is grounded. I’m finalizing the review text now.
exec
/bin/zsh -lc 'rg -l "conditional_outcome_acceptance" eval_interactive/case_specs | wc -l' in /Users/caoruixin/projects/csagent-wt-105
 succeeded in 0ms:
      14

2026-07-26T03:09:41.118897Z ERROR codex_models_manager::manager: failed to renew cache TTL: missing field `supports_reasoning_summaries` at line 88 column 5
codex
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
hook: Stop
hook: Stop Completed
tokens used
99,779
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
