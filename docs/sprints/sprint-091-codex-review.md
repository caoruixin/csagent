## Sub-sprint Review Decision — S-Y1.7
decision: pass
blocking_count: 0
summary: Commit `758503b6` implements an infra/eval-framework statistical gate, not a semantic runtime hardcode. The production gate branches on baseline pass-rate tier and posterior statistics, not keywords, UC enums, CaseSpec IDs, or visible eval text. Layer 0 is unchanged in the diff, exp-78’s anti-误杀 majority flip remains a discard, exp-66/72 are released by the configured statistical wiring rather than special cases, scope is `autoloop/**`, and the scoring SHA reproduces.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix for a semantic decision?
No semantic hardcode found. It adds statistical/eval-framework branching by measured pass-rate tier:
```diff
+    if pass_rate >= 0.999:
+        return TIER_S
+    if pass_rate <= 0.001:
+        return TIER_F
+    return TIER_N
```
The gate uses `(k,n)`, `P_regress`, BH-FDR, and count thresholds, not user text or UC-specific decision rules.

2. If yes to (1), is the change justified as protecting a current Tier-0 invariant?
N/A for semantic hardcode. It preserves the existing Tier-0 floor and adds the FS anti-误杀 floor as eval-gate protection, not as a new runtime Tier-0 invariant.

3. Could the same outcome be achieved by projecting a soft signal to the LLM instead of a hard branch in Java or the prompt?
N/A. This is not Java/prompt semantic routing; it is the autoloop survival gate. It also projects `classification=ambiguous` for non-discard knife edges rather than forcing every signal into a hard discard.

4. Does the change encode visible-eval case text, trace-specific phrasing, or a CaseSpec id into runtime, prompt, or judge config?
No executable gate logic does. Case IDs appear in oracle tests and data-driven reasons/metrics. Non-blocking note: production comments mention examples such as `wmkb`, but no branch consumes those strings:
```diff
+    A knife-edge increase (... exp-66's
+    wmkb at 0.797) gates nothing but marks the candidate AMBIGUOUS
```

5. Does the change move semantic ownership from the LLM to Java?
No. The LLM-owned surfaces in §1.3 are untouched. This changes scoring of fixed eval results and candidate survival, not runtime semantic planning.

6. Does the change add an if-else block to the prompt instead of principle-level or observable-state guidance?
No prompt changes.

7. Does the change preserve tool schema, capability / permission boundary, PII / safety floor, and grounding floor?
Yes. `server/**` and `eval_interactive/**` are untouched. Layer 0 has no body diff; the FS floor catches majority flips:
```diff
+            if (k / n) <= 0.5:
+                ctx.fs_hits.append(f"{cid}({k}/{n})")
```
`scoring_code_baseline_sha` also reproduces as `f2f983cc86e7d29e18d4659da3a317131eecd624df14a78ce26286f30d590d79`.

8. Does the PR ship generalization eval coverage — target, neighbor, negative, and shadow cases — and not only the target case?
Yes for this measurement-layer change: the oracle covers the 12 archived experiments across the named families, including tier0, anti-误杀 control, cross-case regression, C1, and C2:
```diff
+_DISCARDS = {67: ..., 78: (...), 69: (...)}
+_KEEPS = [66, 71, 72, 75, 77, 79]
```

9. If the change is temporary, does it carry an explicit rollback or sunset plan?
Not temporary. F5 knob confirmation is explicitly deferred to the first S-Y2 pilot run, but that is calibration follow-up, not a hardcode sunset.

Kernel Verdict: approve

R-S91.1: Keep the first S-Y2 pilot knob-confirmation gate from the handoff; the n=5 retrospective oracle certifies floors and wiring, not final statistical power.
