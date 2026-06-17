# Per-sub-sprint Codex review prompt — Sprint 091 / S-Auto-37 (M-Auto-7 S-Y1.7)

> Self-contained executable view. You receive context via this repo (docs +
> diff), NOT via any chat history. Do not edit code. Do not re-judge any human
> §5.6 bad-case verdict.

## 1. Role identity

You are the **Anti-Hardcode Review Agent** for **M-Auto-7 sub-sprint S-Y1.7**
(per-sub-sprint review, §4.3 trigger #3 — a new statistical decision surface on
the autoloop fitness gate that governs which `semantic_planner` skill-yaml
candidates survive to the §4.1 human gate). Issue a single §4.1 verdict and write
it to `docs/codex-findings.md` using the §4.2 header.

## 2. Scope under review (cumulative)

- **Single commit:** `758503b6` ("M-Auto-7 S-Y1.7 — noise-aware fitness gate (V3)").
- **Diff command:** `git show 758503b6 -- autoloop/` (or `git diff 758503b6^..758503b6`).
- **Ship artefacts (12 files, +1694/−543, all `autoloop/**` + `config.yaml`):**
  `tier_evaluator.py` (5-layer rewrite), new `posterior.py` (Beta-Binomial,
  dependency-free), `baseline_loader.py` (per-case k/n + stability_class
  plumbing), `aggregate.py` (`non_comparable_rate`), `preflight.py` (§5.9
  alert), `config.yaml` (the `fitness` knobs), and tests
  (`test_fitness_gate_oracle.py`, `test_posterior.py`, `test_preflight.py`,
  `test_tier_evaluator.py`).

## 3. What the change does (so you can judge intent vs surface)

Replaces the autoloop fitness gate's zero-tolerance majority-flip rule
(`anchor_outcome_max_drop_cases: 0` + raw tier2/shadow count gates) with a
stability-tiered, noise-aware statistical rule (the "V3" variant), motivated by a
measured ~92–95% false-discard rate for a behaviour-neutral candidate at n=3–5
(the M-Auto-4 measurement-reliability thesis). Lexicographic structure preserved:

- **Layer 0 (tier0 floor): UNCHANGED** delta logic.
- **TIER classification** per baseline case from existing `stability_thresholds`:
  TIER-S (baseline pass_rate==1.0) / TIER-F (==0.0) / TIER-N (else).
- **FS (new anti-误杀 floor):** a TIER-S case that **majority-flips** to fail →
  discard (NOT any-attempt-fail).
- **TIER-N:** Jeffreys Beta-Binomial posterior, TOST margin δ=0.10; per-case
  P_regress≥0.80 regressed / 0.50–0.80 ambiguous; cross-case discard if BH-FDR
  (α=0.10) flags ≥1 OR ≥2 cases at P_regress≥0.80.
- **Layer 2 (tier2) + Layer 4 (shadow): RETAINED, made noise-aware** (gate the
  count increase through the affected case's posterior / a shadow-aggregate
  posterior).
- Sample sizes: `samples_per_case` 3→5 (11 for pilot `primary_targets`);
  `anchor_outcome_max_drop_cases` removed; `scoring_code_baseline_sha` recomputed.

## 4. Inputs (read these; they are self-contained pointers, not chat)

- `docs/sprints/sprint-091-handoff.md` — the rule, final parameters, the C1/C2
  resolution, the F5 power-ceiling, the `scoring_code_baseline_sha` delta +
  no-re-bless rationale, the P0.6c instrumentation.
- `autoloop/tests/test_fitness_gate_oracle.py` — the zero-LLM acceptance oracle
  (the pinned V3 verdict matrix over the 12 archived experiments).
- `docs/solutions/p07-calibration/calibrate.py` (V5 reference implementation) +
  `p07-findings.md` (the P0.7 calibration evidence: F1 why any-fail TIER-S was
  rejected; F4 the exp-66/72 grounding; F5 the power ceiling).
- `docs/sprint_objective.md` (S-Y1.7 contract) — the normative rule spec + §7
  stanza + hard fences.

## 5. Focus checks (in addition to the kernel)

1. **No semantic hardcode / no CaseSpec-id leakage** — the rule is a
   stability-tiered statistic applied uniformly by baseline tier; confirm there
   is NO keyword/regex/enum/per-UC branch and **no CaseSpec id** in the gate
   logic. (Contrast: the S-Y1.5d Q4 finding was about a CaseSpec-id regex in a
   different file — confirm S-Y1.7 introduces none.)
2. **FS only STRENGTHENS the §5.4 anti-误杀 floor** — majority-flip TIER-S must
   never let a genuine anti-误杀 control regression pass; confirm exp-78's
   control (1.00→1/3) still discards and FS is not weaker than the prior floor.
3. **Layer 0 byte-for-byte unchanged** — confirm the tier0 delta floor logic is
   not altered.
4. **The oracle was not p-hacked** — confirm exp-66 lands `keep/ambiguous`
   (the wmkb P_regress=0.797 < 0.80 knife-edge) and exp-72 `keep` (shadow
   released) because the principled wiring decides, NOT because thresholds were
   tuned to a predetermined answer (§1.6).
5. **Scope fence + scoring SHA** — confirm only `autoloop/**` + `config.yaml`
   changed (no server/eval_interactive/CaseSpec/baseline), and that the recomputed
   `scoring_code_baseline_sha` reproduces from the new scoring file set.

## 6. Embedded §4.1 nine-question anti-hardcode kernel

```text
You are the Anti-Hardcode Review Agent. The PR below proposes a change
to this repo. Your job is to decide whether the change introduces a
semantic hardcode — a keyword / regex / if-else / enum / per-UC matrix
that encodes a decision the LLM is supposed to own under
docs/current/iteration_governance.md §1.3 — and to issue a verdict.

Scope exemption: pure infra, docs-only, config-governance, and
characterization-test PRs are not subject to this review. If the PR
is purely one of those, return `approve` with a one-line note naming
the exemption.

For every other PR, walk these nine questions in order. For each
"yes" or each concern, paste the diff snippet and the reasoning.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix
   for a semantic decision (drift detection, escalation, UC
   selection, risk classification, follow-up, intake routing)?
2. If yes to (1), is the change justified as protecting a current
   Tier-0 invariant named in docs/runtime_freeze_and_risk_policy.md
   §1 / §2?
3. Could the same outcome be achieved by projecting a soft signal to
   the LLM (an additional projected slot, a candidate list, a
   diagnostic flag) instead of a hard branch in Java or the prompt?
4. Does the change encode visible-eval case text, trace-specific
   phrasing, or a CaseSpec id into runtime, prompt, or judge config?
5. Does the change move semantic ownership from the LLM to Java —
   that is, shrink what docs/current/iteration_governance.md §1.3
   says the LLM owns?
6. Does the change add an if-else block to the prompt instead of
   principle-level or observable-state guidance?
7. Does the change preserve tool schema, capability / permission
   boundary, PII / safety floor, and grounding floor?
8. Does the PR ship generalization eval coverage — target, neighbor,
   negative, and shadow cases — and not only the target case?
9. If the change is temporary, does it carry an explicit rollback or
   sunset plan (downgrade-to-signal trigger, retirement sprint id)?

Return exactly one verdict:

- `approve`
- `approve with downgrade-to-signal follow-up`
- `reject as semantic hardcode`
- `needs human architecture decision`

Do not rewrite the PR. Do not propose a code fix beyond naming the
layer in docs/current/iteration_governance.md §3 that the fix should
target.
```

Note for Q8 in this context: "generalization coverage" = the 12-experiment
retrospective oracle (four families: RESOLVE-FAQ / DISCOVER / escalation_policy /
anti-误杀 control), NOT a per-CaseSpec family — this is a measurement-layer change.
The shadow firewall is preserved (per-case shadow detail is not exposed to the
meta-agent; the gate reads shadow only as a firewalled aggregate).

## 7. Output format — write to `docs/codex-findings.md`

Append a new section:

```
## Sub-sprint Review Decision — S-Y1.7
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

Then the §4.1 nine-question kernel walk (numbered, with diff snippets for any
concern) + a "Kernel Verdict" line with exactly one of the four verdicts. Record
any non-blocking observations as `OQ-S91.x` / `R-S91.x` items.

## 8. Constraints

- Do NOT edit any code or non-findings docs.
- Do NOT re-judge a human §5.6 bad-case verdict.
- This is `infra`/eval-framework scope; if you judge it a pure-infra exemption,
  say so explicitly and still answer focus checks 1–5 (the §1.7 / §5.4 surfaces
  are the reason it is NOT auto-exempt).
