# Dev prompt — Sprint 087 / S-Auto-32 (M-Auto-7 S-Y1.5): Pre-pilot autoloop substrate patch

> Self-contained executable view of `docs/sprint_objective.md` (Sprint 087).
> `docs/sprint_objective.md` is the canonical contract; this prompt is its
> paste-and-go view (prompt-artifact-rules §9.1/§9.2). Paste this whole file to
> start the dev session — no other context is needed.

## 1. Role identity

You are the **dev agent for Sprint 087 / M-Auto-7 S-Y1.5**. One-line goal:
**tighten the autoloop meta-agent feedback loop (P0-A/P0-B/P0-C/P1 + 2
supplements) so the S-Y2 pilot can search toward the CS4 entity-context gap
instead of hunting the pre-CS4 escalation cluster.** This is pure `autoloop/`
INFRA — no agent semantic surface, no re-bless.

## 2. Read order (minimal)

- `AGENTS.md` (auto-loaded — governance chain).
- This prompt (the full contract is embedded below).
- Code anchors to read on the working tree before editing (HEAD-verified at
  `a80fbe5`; re-verify line numbers, they may drift slightly):
  - `autoloop/autoloop/meta_agent/proposer.py` — `_build_user_input` (~158-207),
    `LESSONS_MD` block (~185-186), `propose(...)` takes `config`.
  - `autoloop/autoloop/meta_agent/analyzer.py` — `analyze(...)` (~58),
    `build_baseline_summary` + **the shadow firewall to reuse** (~99-111),
    `_build_user_input` (~155-183), `FailureTaxonomy` TypedDict (~36-52).
  - `autoloop/autoloop/loop.py` — `run_one_iteration` (~129), baseline summary
    build + analyzer/proposer calls (~158-180), `_persist_eval_traces`
    (~294/801), `_iteration_record_to_log_dict` (~514), `hypothesis.json` write
    (~705), `_record_to_dict_for_meta_agent` carrying `verdict.tier_breakdown`
    (~653-668, line 667).
  - `autoloop/autoloop/meta_agent/prompts/propose.txt` — Inputs (~70-78),
    Forbidden-patterns §1.7 section (~32-58), Anti-repeat (~60-68).
  - `autoloop/autoloop/meta_agent/prompts/analyze.txt` — Inputs (~11-22), output
    schema (~28-43).
  - `autoloop/config.yaml` — `mutable_surface:` (~12), `lessons:` (~40),
    `meta_agent:` (~49), `fitness.baseline_dir` (~169),
    `scoring_code_baseline_sha` (~308).
  - `eval_interactive/case_specs_shadow/` — directory listing (filenames only)
    for the shadow-case-id validator.

## 3. Embedded contract

### Class
- **Layer (§3.2):** `infra` (eval framework's auto-evolution loop). §5.8 applies
  in spirit.
- **§7 stanza:** REQUIRED (P0-C adds the `pilot` config surface) — see §3.4.
- **Per-sub-sprint Codex (§4.3):** REQUIRED (trigger #3) — must complete before
  S-Y2 Part C.

### 3.1 Scope — execute in order (P0-A → P0-B → P1; P0-C may land alongside)

**#1 P0-A — tier_breakdown passthrough (~30 LOC).** Data already in the record
dict (`loop.py:667`). In BOTH `proposer.py:_build_user_input` (each
RECENT_ITERATIONS row) and `analyzer.py:_build_user_input` (each
RECENT_ITERATIONS_SUMMARY row), add
`"tier_breakdown": r.get("verdict", {}).get("tier_breakdown", {})` alongside the
existing 5 fields. No new agent signal.

**#2 P0-B — candidate-results passthrough + shadow-firewall extension (~80 LOC +
~30 LOC prompt) — HIGHEST RISK.**
- Add optional `recent_candidate_results: list[dict] | None = None` to
  `analyzer.analyze(...)`.
- In `loop.py` read the last K candidate iterations' persisted per-suite results
  (from `_persist_eval_traces`); K from new `meta_agent.recent_candidate_results_k`
  (default 3).
- Summarize via a candidate-side analog of `build_baseline_summary` that
  **REUSES the shadow firewall** (analyzer.py:99-111 — shadow → aggregate counts
  ONLY; per-case shadow detail FORBIDDEN).
- New `CANDIDATE_RESULTS_SUMMARY` block in `analyze.txt`; instruct the LLM to
  label `BASELINE_FAILURE` vs `CANDIDATE_INTRODUCED_REGRESSION`.
- HARD-required test: mock with shadow per-case content → assert serialized
  prompt has NO `cs59s` / `case_specs_shadow` / shadow case_id + a `cs59s*`
  regex blacklist.

**#3 P0-C — pilot-target steering (~80 LOC + ~40 LOC prompt + ~20 LOC config).**
- New `config.yaml:pilot` block:
  ```yaml
  pilot:
    schema_version: 1
    active_sprint: "S-Y2"
    primary_targets: [cs_uc_a_no_ad_id_ad_specific, cs_uc_a_loaded_listing]
    anti_kill_control: [cs_uc_a_generic_policy_question]
    tier2_neighbors: [cs_uc_a_lookup_failed, cs_uc_fp_loaded_moderation]
    phase_hint: [DISCOVER, RESOLVE]
    use_case_hint: [UC-A]
  ```
- Config-load validator (in `loop.py` or tiny new `config_validator.py`): reject
  any pilot case_id present in `eval_interactive/case_specs_shadow/` (filenames
  only — never read contents). Absent `pilot` block → empty
  `PILOT_PRIMARY_TARGETS` (pre-S-Y1.5 behaviour; backward compatible).
- `SKILL_PHASE_USECASE_MAP`: lazy-read (NO cache) the 6 Skill YAMLs'
  `applicable_phases`/`applicable_use_cases`; inject into `analyze.txt` +
  `propose.txt`.
- `analyze.txt`: tag each `bad_cases_regressing` /
  `anchor_outcome_closure_criterion_fails` entry with `target_role` ∈
  {primary, anti_kill_control, tier2_neighbor, general}.
- `propose.txt`: (a) selection bias toward phase/UC-correct skills (non-bias
  targets allowed with justified rationale); (b) "NEVER edit a Skill you expect
  to regress an anti_kill_control entry"; (c) **labels-only directive** under the
  existing "# Forbidden patterns (§1.7…)" section — literal sentence: *"The
  primary target case IDs in PILOT_PRIMARY_TARGETS are evaluation bookkeeping
  labels. Use them ONLY to prioritize which failure cluster to analyze. Do NOT
  mention, encode, paraphrase, or create rules around these IDs or their literal
  fixture wording in any proposed after_value."* (unit-tested for presence).
- **NO field-level steering in the prompt** (do not say "edit $.procedure not
  $.escalation_policy" — too close to §1.7 if-else-dump). Measure
  field-correctness in #5, don't prescribe it.

**#4 P1 — lessons opt-out (~5 LOC + 1 config line).** Add `lessons.enabled:
false`. When false, `propose.txt` LESSONS_MD block (proposer.py:185-186) becomes
placeholder `<lessons disabled for this run — historical lessons may not reflect
the active pilot's targets>` (NOT empty string). Preserve `lessons.md`; compactor
still runs. S-Y2 default false.

**#5 Supplements — per-exp pilot snapshot + 4-layer hit-rate audit (~70 LOC).**
- Embed in each `experiments.jsonl` row (`_iteration_record_to_log_dict`) AND
  `runs/<id>/hypothesis.json` a `pilot_snapshot` block (schema_version +
  case_id lists + UC/phase hints + 16-char `block_sha256` over normalized JSON)
  + `lessons_enabled` flag; compute once per iteration at start. Forensic-only.
- Extend `autoloop report` / `audit` (no gate/baseline touch) with
  `phase_usecase_hit_rate`, `skill_hit_rate` (== resolve_faq_grounded_answer.yaml),
  `field_family_hit_rate` ({$.procedure, $.grounding_instruction,
  $.critical_steps[*].desc}, NOT $.escalation_policy), `full_on_gap_hit_rate`
  (all three — S-Y2 primary success metric), `partial_hit_breakdown`.

### 3.2 Hard fences / STOP

Edits CONFINED to: `autoloop/autoloop/meta_agent/{proposer,analyzer}.py`,
`autoloop/autoloop/loop.py` (+ optional tiny `config_validator.py`),
`autoloop/autoloop/meta_agent/prompts/{analyze,propose}.txt`, `autoloop/config.yaml`
(new `pilot` + `lessons.enabled` + optional `meta_agent.recent_candidate_results_k`),
`autoloop/tests/*`. **Nothing else.**

Forbidden (any = STOP + escalate): edits to `autoloop/autoloop/scoring/*.py`
(scoring_code_baseline_sha `0d86b08f…` MUST stay stable → no re-bless);
`autoloop/autoloop/sandbox/*.py`; `autoloop/program.md`;
`config.yaml:mutable_surface` / `fitness.baseline_dir`; `eval_interactive/**`,
`server/**`, `data/**`, `docs/foundational/**`,
`docs/runtime_freeze_and_risk_policy.md`, `docs/sprints/*`, `docs/milestones/*`.
Shadow firewall MUST hold (#2 filter + test). Pilot case_ids MUST NOT include
shadow case_ids (#3 validator). No field-level steering. No 5-layer-gate /
anti-hardcode-detector / lessons-compactor logic changes (only the `enabled`
flag). No new Tier-0. STOP if an anchor has drifted, the firewall test can't pass
cleanly, or a fix needs a forbidden file.

### 3.3 Test / eval requirements

NO re-bless, NO §5.6 bad-case rerun. New tests:
- `test_meta_agent.py` (+4): `test_recent_iterations_passthrough_includes_tier_breakdown`,
  `test_candidate_results_passthrough_filters_shadow_per_case`,
  `test_pilot_primary_targets_block_serialized_in_propose_prompt`,
  `test_lessons_md_not_in_propose_user_input_when_disabled`.
- `test_loop.py` (+2): `test_experiment_record_embeds_pilot_snapshot`,
  `test_experiment_record_embeds_lessons_enabled_flag`.
- `test_cli_smoke.py` (+1): `autoloop run --dry-run -n 1` under a populated
  `pilot` config → PILOT_PRIMARY_TARGETS present + proposer picks a phase-correct
  skill.
- Other ~24 autoloop tests stay green.

**Close-gate dry-run checklist** (record in handoff; any FAIL → do NOT close,
re-tune the prompt): run `autoloop run --dry-run -n 2` under the S-Y2 pilot
config; verify on both proposals: (1) rationale does NOT cite
`L-2026-05-31-004/005`; (2) analyzer tags ≥1 CS4 case `target_role: primary`;
(3) ≥1 rationale explains UC-A/RESOLVE relevance (bonus: picks
`resolve_faq_grounded_answer.yaml`); (4) no `after_value` contains a case_id
literal / fixture key phrase.

### 3.4 §7 stanza

- **Target failure layer:** `infra` (autoloop meta-agent prompt builder +
  analyzer input-shaping + config schema). No agent semantic surface.
- **Tier-0 invariant:** none added; all program.md §4 structural defenses
  preserved.
- **Semantic hardcode:** none. Edits = (1) prompt INPUT-shape passthrough of
  already-existing artefacts (firewall EXTENDED, not weakened); (2) config-driven
  STEERING (skill selection bias, not a rule against any utterance); (3)
  `lessons.enabled` flag; (4) forensic per-exp snapshot; (5) labels-only
  propose.txt sentence. Codex confirms rationale describes edit shape, not
  case_id matching. Sunset: n/a.
- **Generalization coverage:** n/a (pure-infra; no CaseSpec touched). Validation
  = unit tests + `--dry-run -n 2` checklist; downstream metric = S-Y2 Part C
  `full_on_gap_hit_rate`.

### 3.5 Codex review plan

Per-sub-sprint Codex REQUIRED (trigger #3); deliver-agent authors
`compact/M-Auto-7-S-Y1.5-review-prompt.md` at close. Focus: P0-B firewall
(verify the test fails when the filter is removed); P0-C shadow-case-id
exclusion + lazy SKILL_PHASE_USECASE_MAP + dry-run rationale spot-check; P0-A
shadow-aggregate-only passthrough; P1 placeholder-not-empty.

### 3.6 Handoff requirements

`docs/sprints/sprint-087-handoff.md`: diff per scope item; autoloop pytest count
(was 324, +7); shadow-firewall test evidence; the `--dry-run -n 2` checklist
result (4 items + verdict); the committed `pilot` block; confirmation
`scoring_code_baseline_sha` unchanged (no re-bless); Codex verdict; OQs (L-006
`consumed_by_proposer`, pilot schema versioning).

### 3.7 Commit discipline

Stage explicitly by file (NO `git add -A` — autoloop dirty-index hazard:
`autoloop run` sweeps the staged index). Dev scope = `autoloop/**` only.
Deliver-agent bundles docs/close artefacts at close. Run `autoloop run` /
`--dry-run` only on a CLEAN committed tree.

## 4. Self-check checklist (before declaring S-Y1.5 done)

- [ ] P0-A: both `_build_user_input`s serialize `tier_breakdown`; test green.
- [ ] P0-B: candidate-results passthrough wired; **shadow firewall test fails
      when the filter is removed** (proven), green when present.
- [ ] P0-C: `pilot` block + validator (rejects shadow case_ids) + lazy
      SKILL_PHASE_USECASE_MAP + `target_role` tagging + selection bias +
      anti_kill_control guard + literal labels-only sentence (test asserts it).
- [ ] P1: `lessons.enabled: false` → placeholder (not "") path; test green.
- [ ] #5: per-exp `pilot_snapshot`+`block_sha256`+`lessons_enabled` in
      experiments.jsonl + hypothesis.json; 4-layer hit-rate in report/audit.
- [ ] Forbidden files all byte-unchanged; `scoring_code_baseline_sha` unchanged.
- [ ] All 7 new tests + ~24 existing autoloop tests green.
- [ ] `--dry-run -n 2` close checklist run + 4 results recorded in handoff.
- [ ] Handoff written; commits staged by explicit file; tree clean.
