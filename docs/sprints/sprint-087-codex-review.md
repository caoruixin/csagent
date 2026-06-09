# Codex findings - live scaffold

## S-Y1.5 (Sprint 087 / S-Auto-32) Review Decision
decision: pass
blocking_count: 0
final_verdict: APPROVE_S_Y1_5 / blocking_count=0 (kernel verdict: approve)
summary: Commit `43cd9cf` keeps the S-Y1.5 change inside the autoloop infra surface and does not introduce a semantic hardcode. The pilot case IDs are present in the meta-agent input as prioritization labels, not encoded into proposer `after_value`, runtime routing, scoring, sandbox, or judge config. The shadow firewall is preserved and load-bearing, the config validator rejects shadow filename-stem collisions, field-level steering is absent, `scoring_code_baseline_sha` still reproduces, and the autoloop suite is green at `331 passed`.

## Nine-Question Kernel Walk

1. **P1 concern reviewed - new steering labels, not a semantic hard branch.**
   The diff adds a pilot target surface and a `target_role` label set:

```diff
+pilot:
+  primary_targets: [cs_uc_a_no_ad_id_ad_specific, cs_uc_a_loaded_listing]
+  anti_kill_control: [cs_uc_a_generic_policy_question]
+  tier2_neighbors: [cs_uc_a_lookup_failed, cs_uc_fp_loaded_moderation]
+  phase_hint: [DISCOVER, RESOLVE]
+  use_case_hint: [UC-A]
```

```diff
+When PILOT_PRIMARY_TARGETS is present, tag every `bad_cases_regressing`
+and `anchor_outcome_closure_criterion_fails` entry with a `target_role`:
+`primary` if its case_id is in `primary_targets`, `anti_kill_control` if
+in `anti_kill_control`, `tier2_neighbor` if in `tier2_neighbors`,
+otherwise `general`.
```

   This is a config-driven search-prioritization surface for the autoloop meta-agent. It does not add Java routing, runtime escalation logic, use-case classification, drift detection, or a per-UC matrix that decides customer behavior.

2. **Tier-0 invariant: none added.**
   The sprint stanza and diff make no Tier-0 invariant claim. That is acceptable because the reviewed concern in Q1 is not a deterministic semantic decision. Existing safety, grounding, sandbox, anti-hardcode, and fitness gates are preserved.

3. **Soft-signal route used.**
   The outcome is achieved by projected signals: `tier_breakdown`, `CANDIDATE_RESULTS_SUMMARY`, `PILOT_PRIMARY_TARGETS`, and `SKILL_PHASE_USECASE_MAP`. The proposer still chooses the skill, field, and prose, and candidate acceptance remains with the existing sandbox / anti-hardcode / five-layer gate.

4. **P1 concern reviewed - CaseSpec IDs are present as prompt input labels.**
   The new prompt input intentionally includes pilot CaseSpec IDs. The diff also adds the required labels-only defense:

```diff
+6. The primary target case IDs in PILOT_PRIMARY_TARGETS are evaluation
+   bookkeeping labels. Use them ONLY to prioritize which failure cluster
+   to analyze. Do NOT mention, encode, paraphrase, or create rules around
+   these IDs or their literal fixture wording in any proposed
+   after_value.
```

   This satisfies the S-Y1.5 contract: IDs steer where the meta-agent looks, but are not encoded into Skill YAML output. The retained local post-patch dry-run artifact `autoloop/results/runs/exp-69/hypothesis.json` targets `resolve_faq_grounded_answer.yaml` / `$.grounding_instruction` and its rationale describes "UC-A primary failure shapes in RESOLVE-FAQ" and listing-context grounding, not text matching `cs_uc_a_no_ad_id_ad_specific`. Handoff section 5 records the same shape for the two-iteration close checklist.

5. **Semantic ownership does not move from LLM to Java.**
   `config_validator.py` validates config, renders prompt input blocks, and computes report/audit hit-rate observations. It does not decide customer intent, escalation posture, UC routing, follow-up, or response strategy.

6. **Prompt additions are steering guidance, not an if-else rule dump.**
   The new proposer prompt says:

```diff
+- Prefer targeting a Skill whose `applicable_phases` / `applicable_use_cases`
+  intersect the pilot's `phase_hint` / `use_case_hint`.
+- NEVER edit a Skill you expect to regress an `anti_kill_control` entry.
+This is selection guidance about WHERE to look, not a rule about any
+customer utterance - keep your `after_value` durable, generic CS prose.
```

   No prompt text prescribes a specific field such as "edit `$.procedure` not `$.escalation_policy`"; the only field-family logic is an observation-only report/audit metric.

7. **Schema, permission, PII/safety, and grounding boundaries preserved.**
   No tool schema, server runtime, PII/safety floor, grounding floor, sandbox, scoring, baseline, or mutable-surface boundary changes are in the reviewed range.

8. **Coverage appropriate for infra scope.**
   This sub-sprint ships autoloop unit/smoke coverage rather than a bad-case rerun. Verified evidence: `cd autoloop && uv run --extra dev pytest -q` -> `331 passed, 1 warning`. The new tests cover tier-breakdown passthrough, candidate-results shadow filtering, pilot prompt serialization, lessons placeholder, pilot snapshots, lessons flag, and dry-run pilot threading.

9. **No temporary hardcode requiring sunset.**
   No semantic hardcode is introduced, so no downgrade-to-signal trigger is required. The pilot card is already a signal projected to the meta-agent. Future lifecycle decisions for `lessons.enabled` and pilot schema versioning remain OQs, not blockers.

## F1-F6 Findings

**F1 - P0, pass: shadow firewall holds and is load-bearing.**
`analyzer.analyze(...)` sanitizes `recent_candidate_results` inside `analyze` before prompt construction:

```diff
+    candidate_summary = summarize_candidate_results(recent_candidate_results)
...
+        candidate_results_summary=candidate_summary,
```

The shared firewall is in `_summarize_suite_cases`:

```diff
+    if suite_name == "shadow":
+        passed = sum(1 for c in cases if c.get("case_passed") is True)
+        return {
+            "total_cases": len(cases),
+            "passed_cases": passed,
+            "failed_cases": len(cases) - passed,
+        }
```

I verified the full suite passes, then monkeypatched the summarizer in memory to bypass the shadow branch and invoked `test_candidate_results_passthrough_filters_shadow_per_case`; it failed as expected. No source file was edited for that mutation check.

**F2 - P1, pass: P0-C is steering, not a hardcode.**
`PILOT_PRIMARY_TARGETS` is rendered into meta-agent input, not into a proposed `after_value`. The proposer prompt includes the labels-only directive under the forbidden-patterns section. Local `exp-69` rationale and handoff section 5 describe edit shape and UC-A/RESOLVE relevance rather than matching a case-id literal. The local tree retains one post-patch pilot dry-run artifact (`exp-69`); the handoff records the two-iteration checklist, but both artifacts are not retained in `autoloop/results/runs/`.

**F3 - P0, pass: pilot shadow-case-id exclusion holds.**
`validate_pilot_config` returns `{}` when the `pilot` block is absent, so legacy runs render no `PILOT_PRIMARY_TARGETS`. Shadow exclusion reads filename stems only:

```python
for p in shadow_dir.rglob("*.yaml"):
    if p.name.startswith("_"):
        continue
    out.add(p.stem)
```

I also ran a temporary-directory check: absent pilot returned `{}`, and a `primary_targets` value colliding with `case_specs_shadow/shadow_case_a.yaml` was rejected with `PilotConfigError`.

**F4 - P1, pass: lazy map and no field-level steering.**
`build_skill_phase_usecase_map` has no cache and re-reads each allowed Skill YAML on every call with `yaml.safe_load(abs_path.read_text(...))`. The prompts steer by phase/use-case intersection only. The field-family constants in `config_validator.py` feed `iteration_hits` / `compute_pilot_hit_rates` for `report` / `audit`; they are not rendered into the proposer prompt and do not gate candidate acceptance.

**F5 - P0, pass: fences and no re-bless held.**
The reviewed range touches only:

```text
autoloop/autoloop/cli.py
autoloop/autoloop/config_validator.py
autoloop/autoloop/loop.py
autoloop/autoloop/meta_agent/analyzer.py
autoloop/autoloop/meta_agent/prompts/analyze.txt
autoloop/autoloop/meta_agent/prompts/propose.txt
autoloop/autoloop/meta_agent/proposer.py
autoloop/config.yaml
autoloop/tests/test_cli_smoke.py
autoloop/tests/test_loop.py
autoloop/tests/test_meta_agent.py
```

No forbidden paths were touched. `mutable_surface`, `fitness.baseline_dir`, and `scoring_code_baseline_sha` are byte-unchanged in the config diff. Independent recompute via `autoloop.scoring.gaming._compute_scoring_code_sha()` returned `0d86b08fc0309196d9fc49f4924fe446b36d6c04301c4bcb406ca12337dd149e`. The new `config_validator.py` and additive `cli.py` report/audit rendering are in scope for P0-C/#5 and do not alter gates.

**F6 - P1, pass with OQ noted: proposer lessons placeholder is non-empty and proposer-only.**
`proposer.py` defines a non-empty `_LESSONS_DISABLED_PLACEHOLDER`, and `proposer.propose(...)` swaps only the proposer `LESSONS_MD` body when `lessons.enabled` is false:

```diff
+    lessons_enabled = bool(lessons_cfg.get("enabled", True))
+    effective_lessons = lessons if lessons_enabled else _LESSONS_DISABLED_PLACEHOLDER
```

`analyzer.analyze(...)` still receives `lessons_md`; this matches the contract and OQ-S87.3. I do not treat it as blocking for S-Y1.5, but it remains worth watching in S-Y2 if analyzer summaries continue to cite stale pre-CS4 lessons.

## Non-Blocking Observations

**NBO-1 - P3, suggested layer: infra.**
The handoff records a real `--dry-run -n 2` checklist, but the working tree only retains one post-patch pilot dry-run artifact under `autoloop/results/runs/` (`exp-69`). If future reviews need to audit LLM dry-run rationale quality independently, preserve both dry-run artifacts or attach their captured prompts in the handoff bundle.

**NBO-2 - P3, suggested layer: infra.**
OQ-S87.3 is real but non-blocking: the analyzer still consumes `LESSONS_MD` while the proposer does not. If S-Y2 Part C shows analyzer taxonomy drift toward stale pre-CS4 lessons, the follow-up should target autoloop meta-agent input shaping rather than runtime customer-service logic.

**NBO-3 - P3, suggested layer: infra.**
The analyzer summary scrub pattern predates the new `cs_uc_a_*` pilot label shape and does not explicitly scrub that shape from `summary`. This is not a blocker because structured case IDs are intentionally present as labels and the proposer has the labels-only directive, but future hardening could extend the defensive summary scrub if rationales start echoing `cs_*` labels.
