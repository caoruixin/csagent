## Sprint Review Decision
decision: pass
blocking_count: 0
final_verdict: APPROVE_WITH_NON_BLOCKING_OBSERVATIONS
summary: M-Auto-5 may close: the cumulative range corrects eval/runtime measurement and trace-contract surfaces without moving LLM-owned semantic decisions into Java or the bot prompt. The only keyword/enum/branch additions are measurement-side gates or simulator-input hygiene, with paired false-fail counter-evidence and no P->F majority flips in the authoritative stalledfix baseline. Non-blocking observations remain for M-Auto-6 or later: stall-detector window tuning, goal_impossible positive-evidence policy, Cluster C.1 session-create 400s, and judge-layer wiring.

## §1 Cumulative Scope Assessment

**S-Auto-19 / Sprint 074:** approve. The five eval-read corrections are trace reads, not success-rule rewrites: ATR now unions all turns, intake reads dict keys, source citations accumulate across the session, `trace_minimum` reads terminal disposition, and the PII relaxation is limited to first-party/system or RFC 2606 documentation email addresses. Runtime edits in `ControlKernel` stamp trace contract fields and answer-turn `sourceIds`; they do not change routing, UC choice, escalation posture, or customer wording.

**S-Auto-20 / Sprint 075:** approve. `ControlKernel.isResolvedSuccessTerminal` broadens the resolved stamp to `FINAL_ANSWER` plus `READY_TO_CONFIRM|ANSWERED_SUBTASK` plus grounding, which records a disposition already reached by the bot rather than causing that disposition. Removing `loop_detected` from valid blank-containment terminals is an eval framework correction: a looped session can no longer pass because the gate had no evidence.

**S-Auto-21 / Sprint 076:** approve. The role-map inversion fix is clearly input-column measurement repair in the customer simulator. The D1 keyword, D2 Jaccard, and D3 leakage guard are hard branches, but they operate only on simulator output to prevent fabricated support-agent customer turns from entering eval traces; they do not encode a bot semantic decision, runtime routing rule, or CaseSpec expected-behaviour override. Real-LLM evidence in the handoff reports 0/40 contamination on the focused bad-case re-render and 852->0 contamination across the pre-fix corpus sweep.

**S-Auto-22 / Sprint 077:** approve. The STALL, terminal-failure, zero-evidence, and runtime void/downgrade changes are eval/trace-contract false-positive gates. The two documented deviations are accepted: `stall_result.detected` is scoped to `composite == 0` to avoid known detector false positives with scored recovery, and `goal_impossible` is excluded from the terminal-failure set while zero-evidence `goal_impossible` passes are still caught by the structural no-L2/no-composite rule. No code or config pointer change is required before close.

## §2 Nine-Question Kernel Walkthrough

**Q1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix for a semantic decision?** Yes, it adds keyword/enum/branch machinery, but I do not classify those additions as bot semantic decisions. Simulator guard snippet: `user_simulator.py:87-111` adds `_DRIFT_KEYWORDS`, `_DRIFT_LEAKAGE_PROBES`, and `_REGURGITATION_JACCARD_THRESHOLD`; `user_simulator.py:410-447` retries or raises `SimulatorDriftError` when the simulated customer drifts into support-agent voice. Eval gate snippets: `composite.py:262-303` flips only otherwise-passing cases with `stall_result.detected and composite == 0.0` or `composite == 0.0 and not l2_results`; `hard_checks.py:763-810` enumerates valid and terminal-failure stop reasons. These are measurement/trace gates over evaluator artifacts, not user-goal, UC, escalation, follow-up, or intake-routing decisions.

```python
# eval_interactive/eval_interactive/scoring/composite.py:262-303
if case_passed:
    if stall_result.detected and composite == 0.0:
        case_passed = False
        verdict_reason = "stall_promoted"
    elif composite == 0.0 and not l2_results:
        case_passed = False
        verdict_reason = "no_l2_evidence_to_pass"
```

**Q2. If yes to Q1, is the change justified as protecting a current Tier-0 invariant?** Runtime changes that touch Java are justified by the runtime-owned trace/eval contract under governance §1.4, not by creating a new Tier-0 semantic invariant. The relevant Java snippets are `ControlKernel.java:572-602` and `ControlKernel.java:1446-1458`: resolved stamps are recorded only for grounded success terminals, and stale resolved stamps are voided only for runtime-observable failure terminals. The simulator/eval Python branches are `infra`/`eval_spec` measurement gates; they do not need a new Tier-0 claim because they do not decide customer semantics.

```java
// server/.../ControlKernel.java:1446-1458
if (!"resolved".equals(session.getContainmentOutcome())) return false;
TerminalOutcome t = runResult.terminalOutcome();
return t == MAX_STEPS || t == ERROR
        || t == DEADLINE_EXCEEDED || t == LLM_UNAVAILABLE;
```

**Q3. Could the same outcome be achieved by projecting a soft signal to the LLM?** No for the reviewed fixes. These are scoring, trace, and simulator-input correctness issues. Projecting "maybe this trace is stale" to the bot would not make an eval verdict honest, and projecting simulator drift to the simulator after contamination has already entered the transcript would score the bot against fabricated input. The proper §3 layer is `infra` for eval framework and trace persistence, with a deferred `eval_spec` question for `goal_impossible` positive-evidence policy.

**Q4. Does the change encode visible-eval case text, trace-specific phrasing, or a CaseSpec id into runtime, prompt, or judge config?** No runtime, bot prompt, or judge config encodes visible CaseSpec ids or target case prose. I found case ids only in comments/docs/test evidence, for example `composite.py:275` references cs095 a4 as rationale for scoping, and `hard_checks.py:797-799` references cs32s02 as counter-evidence. The only phrase catalog is the simulator's customer-voice drift catalog at `user_simulator.py:87-100`; it is a measurement guard for support-agent voice contamination, not a runtime/prompt rule for customer semantics.

**Q5. Does the change move semantic ownership from the LLM to Java?** No. Java ownership remains trace contract and persistence: `isResolvedSuccessTerminal` uses terminal outcome, resolve disposition, grounding presence, and blank containment to record `"resolved"` (`ControlKernel.java:1356-1406`), while `shouldVoidResolvedStamp` voids only stale `"resolved"` under runtime failure terminals (`ControlKernel.java:1446-1458`). There is no Java rule for user goal, issue relation, UC hypothesis, drift/topic shift, next action, escalation posture, response strategy, or customer wording.

**Q6. Does the change add an if-else block to the prompt instead of principle-level or observable-state guidance?** The bot system prompt is not changed. The simulator system prompt adds a negative-form `Forbidden` block at `user_simulator.py:48-55`; this is evaluator persona anchoring, not bot prompt logic. It is acceptable because the simulator's role is to produce customer turns, and the audit showed support-agent voice contamination was a measurement defect.

**Q7. Does the change preserve tool schema, capability/permission boundary, PII/safety floor, and grounding floor?** Yes. No tool schema or capability boundary changes appear in the cumulative diff. The PII change is narrow (`hard_checks.py:248-271` allowlists `noreply@gumtree.com` plus `example.com|org|net`; phone patterns remain strict), and source citation remains fail-closed for never-grounded substantive answers (`hard_checks.py:1041-1068`). High-risk anchor and shadow cases remain 0.000 in the stalledfix report.

**Q8. Does the PR ship generalization eval coverage and not only target cases?** Yes. The range includes paired characterization and counter-tests for the read/gate changes, simulator contract tests T1-T7, S-Auto-21 real-LLM bad-case re-render evidence, S-Auto-22 deterministic re-score, and the authoritative stalledfix corpus across bad_cases, anchor_outcome, and shadow. I independently verified the m-auto-4 -> stalledfix majority flips: bad_cases 5 F->P / 0 P->F, anchor_outcome 3 F->P / 0 P->F, shadow 2 F->P / 0 P->F.

**Q9. If temporary, does it carry rollback or sunset plan?** No temporary semantic hardcode is shipped. The deferred items are recorded as open questions, not hidden temporary branches: `OQ-S77.stall-detector-window`, `OQ-S77.goal-impossible-resolved-evidence`, Cluster C.1 session-create 400s, and chronic judge wiring. No downgrade-to-signal follow-up is required for M-Auto-5 close; the future work should target `infra` or `eval_spec` per the open-question layer.

## §3 Specific-Focus-Points Verdicts

**§6.1 S-Auto-22 Deviation #1 - STALL promotion scoped to `composite==0`: verified, accepted.** I concur with the deliver-agent's stance. The actual code gates on typed `stall_result.detected` and `composite == 0.0` (`composite.py:262-284`), not string prefixes or case ids. The simfixed aggregate preserves cs095 a4 as `case_passed=true`, `failure_reason=bot_ended`, and `failure_tags=["STALL:PLACEHOLDER_WITHOUT_FOLLOWUP"]`; the handoff records the missing full-transcript detail that it recovered via escalation outside the detector window. The authoritative stalledfix report has cs095 at 0.5555555556, exactly 5/9. A blanket stall promotion would have converted that known scored-recovery draw to false fail and lost a tracked F->P flip, so the scoped rule is not stealth weakening.

**§6.2 S-Auto-22 Deviation #2 - terminal-failure override excludes `goal_impossible`: verified, accepted.** I concur. The code preserves S-Auto-20's `goal_impossible` ambiguity in `_VALID_TERMINAL_STOP_REASONS` (`hard_checks.py:748-768`) and excludes it from `_TERMINAL_FAILURE_STOP_REASONS` with explicit rationale (`hard_checks.py:789-802`). In the simfixed shadow aggregate, cs32s02 attempts a0/a2/a4 were passing `goal_impossible` draws; that is sufficient anti-false-fail evidence for not blanket-failing positive-evidence `goal_impossible`. In stalledfix, zero-evidence `goal_impossible` attempts are still failed by `VERDICT_OVERRIDE:no_l2_evidence_to_pass`, while positive-evidence `goal_impossible` remains a future `eval_spec` policy question.

**§6.3 §5.9 pre-flight sweep - OQ-S77 closure validity: sufficient for M-Auto-5.** The deliver-agent's exact 414-draw fingerprint uses fields not retained in the compact aggregate attempt rows (`composite_score` and `l2_results` are absent there), so I could not independently recompute the exact predicate from the public aggregate alone. I did verify the visible missed-gate symptom across 414 aggregate attempts: no passing summarized attempt carries `STALL:*`, `loop_detected`, `error`, `contract_violation`, or `max_turns_exceeded`; the only passing terminal-failure-shaped rows are two `goal_impossible` positive-evidence deferrals. This is sufficient to close OQ-S77.stall-not-gated and lift §5.8 framework-defect priority for that brief. Scenarios outside the fingerprint remain, but they are the documented follow-ups, not M-Auto-5 blockers.

**§6.4 Paired-evidence verdict shift: verified, explainable.** I compared `_rebless_report.json` for `m-auto-4-baseline-20260604` and `m-auto-5-baseline-20260604-simfixed-stalledfix`. The majority flips match the prompt exactly: bad_cases alice/cs012/cs015/cs066/cs095 F->P; anchor uc_a_visibility/uc_f_billing/uc_fp_removed F->P; shadow cs01s01/cs11s01 F->P; no P->F flips. The shifts are consistent with the four measurement columns and do not require a bot-capability claim.

**§6.5 Persistent high-risk failures preserved: sufficient.** The stalledfix report keeps anchor uc_g_gdpr, uc_h_appeal, uc_i_payment, and uc_j_safety at pass_rate 0.000. Shadow cs38s01 and cs38s02 remain 0.000; cs15s02, cs92s02, and cs32s01 also remain 0.000 in the available ids, with corresponding renamed ids for the other listed surfaces. I see no evidence that the eval gate, simulator, or runtime changes over-corrected a genuine high-risk failure into PASS.

**§6.6 Near-coinflip re-emergence: verified as corrected measurement, not regression.** Stalledfix has near-coinflip bad_cases cs001 at 0.444 and cs095 at 0.556, plus shadow cs01s02 at 0.444; anchor has none. cs095 exactly matches the deterministic 5/9 expectation after vacuous-pass removal. The re-emergence exposes genuine stochastic ambiguity that the simfixed false-positive gate previously masked.

**§6.7 Cluster C.1 cs59s* `session_create_failed:400`: defer to M-Auto-6.** The stalledfix shadow aggregate shows cs59s01 and cs59s02 as `comparable=false`, `aggregate_status=infra_error`, `valid_attempts=0`, with all 18 attempts failing before session creation. This predates the in-scope eval/runtime/simulator paths and is not a regression introduced by M-Auto-5. It should not block close.

**§6.8 Judge layer excluded per OQ-S76.judge-zero: sound.** The stalledfix scratch result shows `judge_score=0.0` for every bad_cases, anchor_outcome, and shadow case. `docs/action_bank.md` records the chronic by-configuration cause and the four-run evidence. Excluding judge from paired evidence is sound for this milestone because the canonical signals - composite, outcome, L1, and L2/failure tags - are populated and moved with the measurement fixes.

**§6.9 baseline_dir not moved before verdict: verified.** `autoloop/config.yaml:134` still points to `eval_interactive/results/m-auto-4-baseline-20260604`, and `docs/current_eval_baseline.md` is not flipped by this review. The deliver-side STOP is intact.

## §4 Blocking Findings

None.

## §5 Non-Blocking Observations

1. **OQ-S77.stall-detector-window should remain visible in M-Auto-6/future planning.** The current close decision relies on `composite == 0` scoping to avoid known out-of-window false positives. That is acceptable for M-Auto-5, but detector calibration is still an `infra`/possible `eval_spec` quality issue.

2. **OQ-S77.goal-impossible-resolved-evidence is a real eval-spec question, not a close blocker.** The current gate correctly fails zero-evidence `goal_impossible` passes and preserves positive-evidence ambiguity. A future sprint should decide whether positive-evidence `resolved + goal_impossible` is a hard fail, per CaseSpec expectations rather than a blanket stop-reason rule.

3. **The exact §5.9 414-draw predicate is not reproducible from compact aggregate attempts alone.** The aggregate attempt rows omit `composite_score` and `l2_results`; the deliver sweep may have used richer run-local data. For future close reviews, preserve per-attempt composite/L2 fields in the authoritative aggregate or include the sweep output artifact.

4. **Targeted pytest verification did not run cleanly in this review environment.** `uv run --project eval_interactive pytest eval_interactive/tests/test_oq_s77_false_positive_gates.py eval_interactive/tests/test_user_simulator.py` exited with code 139 and no output. I did not use that as evidence; the verdict rests on code/diff review, handoffs, and recorded baseline artifacts.

5. **Cluster C.1 and judge wiring remain forward work.** cs59s01/cs59s02 session-create 400s and chronic judge-score zeroing are both outside M-Auto-5's verdict-correctness close bar, but both should stay out of any future semantic sprint's evidence until resolved or explicitly excluded.
