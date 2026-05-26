Paste the content below this line into a fresh Codex session after the Sprint 32 dev commit lands. No PR will be opened; review the commit directly.

---

You are the Anti-Hardcode + Sprint-Close Review Agent for Sprint 32 (the case-family-authoring sprint that validates the Sprint 31 `alternate_candidate_use_cases` projection slot end-to-end on the §7.2 worked-example shape).

The Sprint 31 close cycle finished with Codex re-review `pass / 0` on commit `8d3e73b` (fix-iteration #2 strengthening T8). Sprint 32 picks up the OQ4-deferred case-family-authoring scope. This is `eval_spec`-only — the dev was authorized to author CaseSpecs and append manifests; NO production code, NO prompt, NO Java test, NO existing case-family edit.

## 1. Loader

1. `AGENTS.md` (transitively loads `iteration_governance.md` §1 / §3 / §4 / §5 / §7 + `doc_governance.md` + `agent_context_guide.md`).
2. `docs/sprint_objective.md` — the Sprint 32 contract. §6 file table; §7 hard fences; §9 §7 stanza; §10 success metrics; §12 stop conditions.
3. `docs/sprints/sprint-032-handoff.md` — the dev's archive. Compare §6 / §9 against §6 of the objective; verify no scope creep.
4. `docs/sprints/sprint-031-handoff.md` §14 + `docs/sprints/sprint-031-fix-handoff.md` — context on what Sprint 31 shipped + how the soft-signal posture was locked down by the Sprint 31 fix-iteration #2 T8 strengthening (load-bearing for the Sprint 32 negative-case design rationale).
5. `docs/current/iteration_governance.md` §1.7 (forbidden list, especially "widening eval spec to accept a genuine bot mistake"), §2 (Failure Brief Template — verify the source brief uses it), §3.2 (Q6 → `eval_spec` layer classification), §4 (your kernel), §5 (Eval Acceptance Rules — Sprint 32 ships no eval-mask), §7 (sprint-objective stanza requirement).
6. `compact/sprint-032-dev-prompt.md` — what the dev was authorized to do vs what landed.
7. `eval_interactive/case_specs/case_families/_manifest.yaml` — the root manifest. Verify the Sprint 32 entry is at the end and matches schema.
8. `eval_interactive/case_specs/case_families/sprint32_alternate_uc/` — the new family directory.
9. `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md` — you may read shadow content per the role boundary (Codex is allowed; the dev was not). Verify shadow files exist at the expected location.
10. `eval_interactive/case_specs_shadow/sprint32_alternate_uc/` — the new shadow directory.
11. `eval_interactive/case_specs_shadow/_manifest.yaml` — verify the Sprint 32 entry was appended with real shadow ids.
12. `docs/diagnostics/failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md` — the new source brief. Verify all six §2 template fields are filled and that "What should NOT be done" explicitly names a tempting regex/keyword fix and the §1.5 / §1.7 reason it is wrong.
13. `eval_interactive/results/<sprint-32-family-rerun-id>/results.json` — the dev's family rerun output. Re-run the per-case `alternate_candidate_use_cases` extraction recipe from handoff §5 to verify cited numbers reproduce.

## 2. Scope-discipline gate (BLOCKING)

Verify the dev commit touches ONLY the following surfaces:

- `docs/diagnostics/failure-briefs/sprint32-*.md` (new)
- `eval_interactive/case_specs/case_families/sprint32_alternate_uc/**` (new directory)
- `eval_interactive/case_specs_shadow/sprint32_alternate_uc/**` (new directory)
- `eval_interactive/case_specs/case_families/_manifest.yaml` (append-only)
- `eval_interactive/case_specs_shadow/_manifest.yaml` (append-only)
- `docs/sprints/sprint-032-handoff.md` (new)

Any of the following in the commit is a BLOCKING scope violation:

- ANY file under `server/src/main/**` (Sprint 32 must NOT touch production code)
- ANY file under `server/src/test/**` (Sprint 32 must NOT touch Java tests)
- `server/src/main/resources/prompts/system_prompt.txt` (no prompt edit)
- ANY file under `eval_interactive/eval_interactive/` (no harness/loader/simulator edit)
- ANY file under `eval_interactive/case_specs/smoke/` (no smoke CaseSpec edit)
- ANY file under `eval_interactive/case_specs/case_families/<not-sprint32>` (cascade fence — no edits to existing families)
- ANY file under `eval_interactive/case_specs_shadow/<not-sprint32>` (cascade fence — no edits to existing shadows)
- ANY file under `docs/foundational/`, `docs/current/`, or `docs/proposals/`
- ANY file under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-031-*` (no archive edit)
- `eval_interactive/case_spec_overrides.yaml` (no L3 override edit)
- `docs/runtime_freeze_and_risk_policy.md` (no Tier-0 edit)
- `docs/action_bank.md` (deliver-agent-owned at close; not staged by dev)
- `docs/sprint_objective.md` (deliver-agent-owned)
- `docs/10-handoff.md` (deliver-agent-owned)
- `docs/codex-findings.md` (you own this file)
- `compact/sprint-032-*-prompt.md` (deliver-agent-owned)

Surface any scope-discipline failure as Finding #1 with the diff snippet quoted.

## 3. §4.1 Anti-Hardcode kernel walk

Sprint 32 is semantic-touching on the `eval_spec` surface. Walk all nine questions:

1. **Q1 keyword / regex / if-else / per-UC matrix for semantic decision?** — verify no decision-routing logic is added to CaseSpec text. CaseSpec text describes user-side observable state (form context, seed messages, hidden facts) and expected behaviour bars; it does NOT encode bot-side decision logic. Per-UC matrices in the manifest entry (`target_case_ids`, `neighbor_case_ids`, etc.) are schema-required and acceptable.
2. **Q2 Tier-0 justification?** — N/A; Sprint 32 adds no Tier-0 invariant. `docs/runtime_freeze_and_risk_policy.md` untouched.
3. **Q3 soft signal achievable?** — N/A; Sprint 32 ships no decision-path code. The CaseSpec corpus VALIDATES whether the Sprint 31 soft signal is observable in the projection.
4. **Q4 eval text / CaseSpec id encoding?** — verify no Sprint 32 CaseSpec id is encoded into runtime, prompt, or judge config. The new CaseSpec ids exist only in: (a) the new YAML filenames, (b) the new family directory, (c) the root manifest entry, (d) the shadow manifest entry, (e) the handoff. Reject if any new id appears in `server/src/main/**` or `server/src/main/resources/prompts/system_prompt.txt`.
5. **Q5 semantic ownership shift?** — verify no production code touched. The Sprint 31 LLM-owns-the-decision posture for `alternate_candidate_use_cases` stays unchanged.
6. **Q6 prompt as if-else dump?** — N/A; no prompt edit.
7. **Q7 tool / capability / PII / grounding floor?** — verify CaseSpec text does NOT widen tool capability, PII boundary, or grounding floor expectations. The `expected.forbidden_tools`, `scoring.hard_checks.no_human_only_tool_exposure`, and `scoring.hard_checks.no_pii_leakage` entries SHALL match the Sprint 20 / Sprint 29 precedent shape.
8. **Q8 generalization coverage?** — verify the family ships ≥1 target + ≥2 neighbor + ≥2 negative + ≥2 shadow per `iteration_governance.md` §5.1. Count `*.yaml` files in `case_specs/case_families/sprint32_alternate_uc/` (expect ≥5 visible) and `case_specs_shadow/sprint32_alternate_uc/` (expect ≥2 shadow).
9. **Q9 rollback / sunset?** — N/A; Sprint 32 ships permanent eval-corpus additions. Rollback would be deletion of the new family directory + revert of the two manifest appends; no feature flag needed.

Expected verdict on the §4.1 kernel: **approve** (Sprint 32 is pure CaseSpec authoring; no semantic decision surface touched).

## 4. §1.7 "widening eval to accept a genuine bot mistake" check (BLOCKING)

Verify the negative CaseSpec design does NOT use a CaseSpec to mask a real bot mistake. The two negative cases each MUST:

- Have a populated `alternate_candidate_use_cases` slot (observable by the LLM).
- Expect the bot to STAY in the active UC.
- Not encode a `bot_handling_pattern` that papers over a known bot wrong-behavior shape.
- Not include a `secondary_ucs` entry that admits the bot's drift would be acceptable (the negative is the LLM-stays-put case; admitting drift acceptance would defeat the purpose).

If the dev's negative-case design appears to relax the rubric in a way that would accept a bot that DOES drift, raise as Finding under §1.7 forbidden list.

## 5. Hard-fence verification

- **Cascade fence:** verify NO file under `eval_interactive/case_specs/case_families/<existing-family>/` is touched. Existing families: `cs015_uc_fp_mis_route`, `cs001_uc_c_template_escalate`, `cs011_uc_d_description_ignored`, `cs038_uc_j_intake_redundancy`, `cs040_uc_k_disengaged_jargon`, `cs095_uc_classification_account_aware`, `cs176_uc_e_wrong_escalation_reason`, `cs192_uc_b_giveaway`, `cs259_uc_f_payment_question`, `manual_probe_uc_a_resolve_must`, `sprint29_directive_probe`. Run `git diff --stat 8d3e73b..HEAD -- eval_interactive/case_specs/case_families/` and verify no path matches these.
- **Existing-shadow fence:** verify NO file under `eval_interactive/case_specs_shadow/<existing-family>/` is touched.
- **Root manifest is APPEND-ONLY:** the `_manifest.yaml` edit at `eval_interactive/case_specs/case_families/_manifest.yaml` SHALL be a pure append of one new `- family_id: sprint32_alternate_uc` block at end-of-file. No existing entry edited. Verify by `git diff 8d3e73b..HEAD -- eval_interactive/case_specs/case_families/_manifest.yaml | head -40` showing only added lines.
- **Shadow manifest is APPEND-ONLY:** same check on `_manifest.yaml` in `case_specs_shadow/`.
- **§7.2 worked example untouched:** confirm `docs/current/iteration_governance.md` is absent from the diff range.
- **Sprint 31 fix-iteration #2 baseline preserved:** the strengthened T8 file should NOT appear in the Sprint 32 diff. Verify `git diff --stat 8d3e73b..HEAD -- server/src/test/` returns empty.

## 6. Schema and reproducibility checks

- **Schema validation:** load each new CaseSpec via `eval_interactive/eval_interactive/case_spec/loader.py:144` semantics. The loader globs `*.yaml`; each new visible + shadow YAML SHALL parse without error. Re-run:

  ```bash
  cd eval_interactive && uv run python -c "from eval_interactive.case_spec.loader import load_case_specs; specs = load_case_specs('case_specs/case_families/sprint32_alternate_uc'); print(len(specs))"
  ```

  Expected output: 5 (target + 2 neighbor + 2 negative). Shadow loader is a separate call against the shadow path.

- **Reproducibility:** every quantitative claim in the handoff (per-case composite_score, alternates list contents, smoke summary numbers) SHALL cite a source path + extraction recipe. Re-run each cited recipe and confirm bit-identical output. If the dev cites e.g. `cs_interactive_015 alts=["UC-A","UC-FP","UC-H"]`, re-extract from the cited `results.json` path and verify.

## 7. Validation runs (you re-execute)

From a clean checkout of the dev commit:

- **Java test baseline preservation:**

  ```bash
  mvn -q -pl server test
  ```

  Expected: `Tests run: 917, Failures: 1, Errors: 0, Skipped: 2` — byte-identical to Sprint 31 fix-iteration #2 close. Any delta is a BLOCKING finding (Sprint 32 must NOT change Java behaviour).

- **Sprint 32 family load + run:**

  ```bash
  cd eval_interactive && uv run eval-interactive run --path case_specs/case_families/sprint32_alternate_uc/
  ```

  Verify the run produces a `results.json` covering 5 cases (target + 2 neighbor + 2 negative; shadow excluded per access boundary). Re-extract per-case `alternate_candidate_use_cases` and confirm the slot is populated on target + neighbor cases (which use AMBIGUOUS intake) and may or may not be populated on negative cases (depending on the dev's intake design). If the dev claimed the slot is populated on the negatives, verify it.

- **(Optional, time-permitting) 14-case smoke regression check:**

  ```bash
  cd eval_interactive && uv run eval-interactive run --path case_specs/smoke
  ```

  Compare summary numbers to `eval_interactive/results/20260516-024934/results.json` (Sprint 31 reference). No-regression bar per `iteration_governance.md` §5.1. Sprint 32 makes no production change so the regression risk is LOW; this is a defensive check. Surface any large regression (composite drop > 10%) as a Finding for human + deliver-agent classification.

## 8. Deferred / non-blocking observations

- `R-llm-provider-latency-drift-2026-05-16` is still proposed in `docs/action_bank.md`; Sprint 32 explicitly does NOT consume. If you observe further latency widening in §7 reruns, surface as informational evidence for the R-item, not as a Sprint 32 blocker.
- The named-but-not-opened `R-sprint-31-case-family-authoring` informal R-item is closed by Sprint 32 substance. The deliver-agent will reconcile.
- The Sprint 31 OQ4 deferral is resolved by Sprint 32; verify the dev's handoff §10 layer-classification self-walk explicitly notes the OQ4 closure.

## 9. Output format (write to `docs/codex-findings.md`)

Replace the file content with the standard §4.2 sprint-close header:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>

## Review Evidence
<bullet list — review scope, commit range, what you re-ran, what passed>

## Blocking Findings (if any)
<numbered list; each entry quotes diff snippet + cited file:line>

## Anti-Hardcode Kernel
<nine-question walk; each Q with one-line verdict>

## Hard-Fence Verification
<the §5 checks; pass/fail per fence>

## Schema And Reproducibility Checks
<the §6 checks; cited recipes re-run, output noted>

## Validation Runs
<the §7 results; commands + output>

## Deferred / Non-Blocking Notes
<the §8 items>
```

## 10. Expected verdict shape

If all gates pass: **`decision: pass / blocking_count: 0`**. The cleanest outcome for an `eval_spec`-only sprint that ships per spec is a single-pass close.

If §2 scope-discipline fails: **`decision: fix_required`** with the violating diff snippet quoted as Finding #1. Examples that would trigger fix_required: any `server/src/main/**` file in the diff, edit to an existing case family, prompt edit, harness edit.

If a substance gap is found that is genuinely the dev's mistake (e.g., shadow ids not appended to shadow manifest; manifest entry missing required field): **`decision: fix_required`** with the missing artefact named.

If you find a substance concern that's NOT in scope for Sprint 32 (e.g., the §7.2 worked example wording is stale — per Sprint 31 OQ3 pre-pick the §7.2 fold-back is deferred to normal cadence; this is NOT a Sprint 32 close-gate failure): **`decision: out_of_scope_review`** with the concern named and the deferral rationale cited.

## 11. Self-check before submitting

- [ ] §2 scope-discipline gate walked, every disallowed surface checked.
- [ ] §3 §4.1 nine-question kernel walked.
- [ ] §4 §1.7 "widening eval to accept genuine bot mistake" check walked.
- [ ] §5 hard-fence verification (cascade, append-only, §7.2 untouched, Java test preserved).
- [ ] §6 schema validation re-run from clean checkout.
- [ ] §7 Java baseline + Sprint 32 family run re-run from clean checkout.
- [ ] §8 deferred items noted as non-blocking.
- [ ] `docs/codex-findings.md` written per §9 format.
- [ ] Verdict per §10 expected shape.
