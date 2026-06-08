---
title: Sprint 087 / M-Auto-7 S-Y1.5 handoff — autoloop feedback-loop tightening
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: autoloop/autoloop/{config_validator.py, loop.py, meta_agent/{analyzer,proposer}.py, meta_agent/prompts/{analyze,propose}.txt}, autoloop/config.yaml
last_reviewed: 2026-06-09
supersedes: []
superseded_by: null
notes: >
  Pure autoloop/ INFRA patch (Layer §3.2 = infra). Tightens the
  auto-evolution meta-agent feedback loop so the S-Y2 pilot can search
  toward the CS4 entity-context gap instead of the pre-CS4 escalation
  cluster. Implements the §6 Option-B 5-fix scope of
  docs/solutions/2026-06-08-autoloop-mechanism-audit-and-feedback-loop-tightening.md
  (P0-A tier_breakdown passthrough + P0-B candidate-results landscape +
  P0-C pilot-target steering + P1 lessons opt-out + #5 forensic snapshot /
  4-layer hit-rate). NO agent semantic surface, NO scoring/sandbox change,
  NO re-bless (scoring_code_baseline_sha 0d86b08f… reproduces). Dev scope
  committed at 43cd9cf (autoloop/** only); docs/close artefacts bundled by
  the deliver-agent. Per-sub-sprint Codex (§4.3 trigger #3) REQUIRED before
  S-Y2 Part C — review prompt authored by the deliver-agent at close.
---

# Sprint 087 / M-Auto-7 S-Y1.5 — autoloop feedback-loop tightening

## §0 — Cold-start evidence table

| Item | Result |
|------|--------|
| **Goal** | Tighten the autoloop meta-agent feedback loop (P0-A/B/C + P1 + 2 supplements) so the S-Y2 pilot searches toward the CS4 entity-context gap, not the pre-CS4 escalation cluster. |
| **Layer (§3.2)** | `infra` (eval-framework auto-evolution loop's prompt builder + analyzer input-shaping + config schema). No agent semantic surface. §5.8 applies in spirit. |
| **Dev commit** | `43cd9cf` — 11 files, autoloop/** only (no docs; deliver-agent bundles close artefacts). |
| **autoloop pytest** | **324 → 331 (+7)**, full suite green (`uv run --extra dev pytest -q` → `331 passed`). |
| **Shadow firewall** | EXTENDED + shared via `analyzer._summarize_suite_cases`; `test_candidate_results_passthrough_filters_shadow_per_case` PROVEN load-bearing (FAILS when the shadow branch is disabled, PASSES when restored — §6). |
| **scoring_code_baseline_sha** | UNCHANGED — `0d86b08fc0309196d9fc49f4924fe446b36d6c04301c4bcb406ca12337dd149e` reproduces at this HEAD. **No re-bless.** |
| **`--dry-run -n 2` close checklist** | **PASS on all 4 items** (real meta-agent LLM; §5). Bonus: iter-1 picked `resolve_faq_grounded_answer.yaml` `$.procedure` (full on-gap). |
| **Forbidden files** | None touched: scoring/*, sandbox/*, program.md, config.yaml mutable_surface/baseline_dir, eval_interactive/**, server/**, data/**, docs/foundational/**. |
| **Codex (§4.3)** | REQUIRED (trigger #3); deliver-agent authors `compact/M-Auto-7-S-Y1.5-review-prompt.md`. Pending. |

## §1 — Diff per scope item

### P0-A — tier_breakdown passthrough (~28 LOC)

Both `proposer._build_user_input` (each `RECENT_ITERATIONS` row) and
`analyzer._build_user_input` (each `RECENT_ITERATIONS_SUMMARY` row) now
serialize `"tier_breakdown": (r.get("verdict") or {}).get("tier_breakdown", {})`
alongside the existing 5 string fields. The data already rode in the
record dict via `loop._record_to_dict_for_meta_agent` (loop.py:667); only
the projection was stripped. No new agent signal — pure passthrough of an
already-persisted, shadow-aggregate-only artefact.

- Files: `meta_agent/proposer.py`, `meta_agent/analyzer.py`,
  `prompts/propose.txt` (Inputs note), `prompts/analyze.txt` (Inputs note).
- Test: `test_recent_iterations_passthrough_includes_tier_breakdown`.

### P0-B — candidate-results passthrough + shadow-firewall extension (~95 LOC + prompt)

- `analyzer.analyze(...)` gains optional `recent_candidate_results`. It is
  sanitized INSIDE `analyze` via the new `summarize_candidate_results`
  BEFORE the prompt is built, so the firewall holds regardless of caller.
- `loop._read_recent_candidate_results` reads the last K candidate
  iterations' persisted `eval-results.json` (from `_persist_eval_traces`);
  K from new `meta_agent.recent_candidate_results_k` (default 3).
- The shadow firewall is EXTENDED, not weakened: `build_baseline_summary`
  and `summarize_candidate_results` now share `_summarize_suite_cases`,
  where the `suite_name == "shadow"` branch emits aggregate counts ONLY
  (`total_cases` / `passed_cases` / `failed_cases`). Per-case shadow detail
  (case_id, failure_tags, failure_shape) is never reached.
- `analyze.txt` adds a `CANDIDATE_RESULTS_SUMMARY` block instructing the
  LLM to label each failing case `BASELINE_FAILURE` vs
  `CANDIDATE_INTRODUCED_REGRESSION`.
- Files: `meta_agent/analyzer.py`, `loop.py`, `prompts/analyze.txt`,
  `config.yaml`.
- Test (HARD-required): `test_candidate_results_passthrough_filters_shadow_per_case`
  (firewall proof + `cs\d+s\d+` shadow-shape blacklist).

### P0-C — pilot-target steering (~90 LOC + prompt + config)

- `config.yaml:pilot` block (committed value in §4).
- New module `autoloop/autoloop/config_validator.py` (the sanctioned
  "tiny config_validator.py" of the contract; holds all pure pilot helpers
  so both `loop.py` and `cli.py` import it without the loop's heavy import
  cost):
  - `validate_pilot_config` — REJECTS (`PilotConfigError`) any
    `primary_targets` / `anti_kill_control` / `tier2_neighbors` case_id that
    collides with a shadow case_spec FILENAME stem under
    `eval_interactive/case_specs_shadow/`. Filenames only — `shadow_case_ids`
    uses `rglob("*.yaml")` + `.stem`, never opens file contents. Absent
    `pilot` block → `{}` → empty `PILOT_PRIMARY_TARGETS` (backward
    compatible). Called once per iteration at `loop.py` step 0.5 (a
    collision routes the iteration to `decision="error"` via the catch-all,
    surfacing the misconfig loudly).
  - `build_skill_phase_usecase_map` — lazy (NO cache) read of the 6 allowed
    Skill YAMLs' `applicable_phases` / `applicable_use_cases` only.
  - `render_pilot_input_blocks` — shared `PILOT_PRIMARY_TARGETS` +
    `SKILL_PHASE_USECASE_MAP` prompt blocks (rendered only when non-empty,
    so a pre-pilot prompt is byte-identical).
- `analyze.txt` — tags each `bad_cases_regressing` /
  `anchor_outcome_closure_criterion_fails` entry with `target_role` ∈
  {primary, anti_kill_control, tier2_neighbor, general}.
- `propose.txt` — (a) phase/UC selection bias toward skills whose declared
  phases/use-cases intersect the pilot hints (non-intersecting allowed with
  explicit rationale); (b) "NEVER edit a Skill you expect to regress an
  anti_kill_control entry"; (c) the literal labels-only directive under the
  `# Forbidden patterns (§1.7…)` section. **NO field-level steering** — the
  prompt never says "edit $.procedure not $.escalation_policy"; field
  correctness is MEASURED in #5, not prescribed.
- Files: `config.yaml`, `config_validator.py`, `loop.py`,
  `prompts/analyze.txt`, `prompts/propose.txt`.
- Tests: `test_pilot_primary_targets_block_serialized_in_propose_prompt`
  (block present + labels-only sentence asserted);
  `test_dry_run_with_pilot_config_threads_targets_and_picks_phase_correct_skill`.

### P1 — lessons opt-out (~8 LOC + 1 config line)

`config.yaml:lessons.enabled` (S-Y2 default **false**). When false,
`proposer.propose` swaps the lessons body for the placeholder
`<lessons disabled for this run — historical lessons may not reflect the
active pilot's targets>` (NOT empty). The file + compactor are untouched
(`loop._maybe_compact_lessons` still runs). **Scope note:** the opt-out is
applied to the PROPOSER only (the contract names `proposer.py:185-186`); the
analyzer still receives `LESSONS_MD` — see OQ-S87.3.

- Files: `meta_agent/proposer.py`, `config.yaml`.
- Test: `test_lessons_md_not_in_propose_user_input_when_disabled` (+ enabled
  control).

### #5 — per-exp pilot snapshot + 4-layer hit-rate audit (~75 LOC)

- `config_validator.build_pilot_snapshot` — forensic block: schema_version,
  active_sprint, the three case-id lists, phase/use-case hints,
  `lessons_enabled`, + 16-char `block_sha256` over the normalized JSON
  (computed once per iteration at step 0.5). Embedded in each
  `experiments.jsonl` row (`loop._build_record_dict`) AND dry-run
  `runs/<id>/hypothesis.json` (`loop._write_dry_run_artefacts`).
  Observation-only.
- `config_validator.{iteration_hits, compute_pilot_hit_rates}` — the
  4 independent layers: `phase_usecase_hit` (targeted skill's phases/UCs
  intersect pilot hints), `skill_hit` (== `resolve_faq_grounded_answer.yaml`),
  `field_family_hit` ({`$.procedure`, `$.grounding_instruction`,
  `$.critical_steps[*].desc`}, NOT `$.escalation_policy`), `full_on_gap_hit`
  (all three — the S-Y2 primary success metric) + venn-style
  `partial_hit_breakdown`. Surfaced in `autoloop report` (aggregate HTML
  section) and `autoloop audit` (per-iteration `pilot_iteration_hits` +
  running `pilot_hit_rate_aggregate`). No gate / baseline touched.
- Files: `config_validator.py`, `loop.py`, `cli.py`.
- Tests: `test_experiment_record_embeds_pilot_snapshot`,
  `test_experiment_record_embeds_lessons_enabled_flag`.

**Scope note on `cli.py`:** the terse §3.2 "confined to" list omitted
`cli.py`, but #5 explicitly requires extending `autoloop report` / `audit`,
which live there. The edit is minimal + additive (all hit-rate logic lives
in the fence-allowed `config_validator.py`; `cli.py` only imports + renders).
`cli.py` is not in the §3.2 hard-forbidden list. Flagged for Codex.

## §2 — Test evidence (autoloop pytest 324 → 331)

7 new tests, all green; ~24 prior autoloop tests unchanged and green;
full suite `331 passed in ~129s`.

| Test | Scope |
|------|-------|
| `test_meta_agent.py::test_recent_iterations_passthrough_includes_tier_breakdown` | P0-A (proposer + analyzer) |
| `test_meta_agent.py::test_candidate_results_passthrough_filters_shadow_per_case` | P0-B firewall (HARD) |
| `test_meta_agent.py::test_pilot_primary_targets_block_serialized_in_propose_prompt` | P0-C + labels-only sentence |
| `test_meta_agent.py::test_lessons_md_not_in_propose_user_input_when_disabled` | P1 placeholder-not-empty |
| `test_loop.py::test_experiment_record_embeds_pilot_snapshot` | #5 snapshot + block_sha256 |
| `test_loop.py::test_experiment_record_embeds_lessons_enabled_flag` | #5 lessons flag |
| `test_cli_smoke.py::test_dry_run_with_pilot_config_threads_targets_and_picks_phase_correct_skill` | P0-C end-to-end (dry-run) |

## §3 — Shadow-firewall proof (P0-B HARD gate)

The firewall lives in `analyzer._summarize_suite_cases` (the `shadow`
branch → aggregate counts only). Proof it is load-bearing (recorded
2026-06-09):

1. Temporarily replaced `if suite_name == "shadow":` with `if False:`
   (firewall disabled) → `test_candidate_results_passthrough_filters_shadow_per_case`
   **FAILED** at the `"shadow-secret-shape" not in user_prompt` assertion
   (shadow per-case detail leaked).
2. Reverted (byte-identical restore via backup) →  test **PASSED**.

The test mocks a candidate payload with shadow per-case content
(`cs59s01…`, `cs11s01…`, `SHADOW_SECRET_TAG`, `shadow-secret-shape`) and
asserts none reach the serialized analyzer prompt, plus a `cs\d+s\d+`
shadow-shape regex blacklist, while the shadow AGGREGATE (`"failed_cases": 1`)
and the non-shadow per-case detail (`cs_uc_a_no_ad_id_ad_specific`) DO survive.

## §4 — Committed `pilot` block

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

Plus `lessons.enabled: false` and `meta_agent.recent_candidate_results_k: 3`.
None of the 5 pilot case_ids collide with a shadow filename stem (validator
green). `mutable_surface`, `fitness.baseline_dir`, and
`scoring_code_baseline_sha` are byte-unchanged.

## §5 — `--dry-run -n 2` close checklist (real meta-agent LLM)

Run 2026-06-09 against the shipped S-Y2 pilot config via the real
meta-agent client (the full `run_one_iteration` dry-run path, client wrapped
to capture analyzer taxonomy + proposer output). Tree was clean/committed
(43cd9cf). **VERDICT: PASS — all 4 items.**

| # | Check | Result |
|---|-------|--------|
| 1 | rationale does NOT cite L-2026-05-31-004/005 | **PASS** — neither iter cites any L-…; `lessons disabled for this run` placeholder confirmed present in both propose prompts (P1 active). |
| 2 | analyzer tags ≥1 CS4 case `target_role: primary` | **PASS** — both iters tagged `cs_uc_a_no_ad_id_ad_specific` + `cs_uc_a_loaded_listing` as `target_role: primary`; `cs_uc_a_lookup_failed` + `cs_uc_fp_loaded_moderation` as `tier2_neighbor`. anti_kill_control `cs_uc_a_generic_policy_question` correctly absent from the regressing list (it passes). |
| 3 | ≥1 rationale explains UC-A/RESOLVE relevance (bonus: picks resolve_faq) | **PASS + BONUS** — iter-1 targeted `resolve_faq_grounded_answer.yaml` `$.procedure` (full on-gap), rationale about the UC-A ad-specific FAQ grounded-answer flow; iter-2 targeted `discover_triage.yaml` `$.grounding_instruction` (phase-correct UC-A classification), rationale about UC-A→intake misrouting. |
| 4 | no `after_value` contains a case_id literal / fixture key phrase | **PASS** — both after_values are generic CS prose (tool/field/UC-category vocabulary only); no `cs_uc_a_*` or fixture phrase. |

**Observation (not a checklist failure):** iter-1's candidate was DISCARDED
by the anti-hardcode detector (`Q1.if_then_decision_tree`) — its narrative
used a "when … the FAQ surface still applies. Run search_knowledge …"
shape the detector flags. This is the gate working as designed. S-Y1.5
scope is STEERING (where the proposer looks), which now works (iter-1 hit
the full on-gap target vs run-1's 3/3 off-gap on `$.escalation_policy`);
whether steered candidates clear the anti-hardcode + 5-layer gate is the
S-Y2 Part C question, measured by `full_on_gap_hit_rate` + actual keeps.

## §6 — Layer-classification + anti-hardcode stanza (§7)

- **Target failure layer:** `infra` (autoloop meta-agent prompt builder +
  analyzer input-shaping + config schema). No agent semantic surface.
- **Tier-0 invariant:** none added; all `program.md` §4 structural defenses
  (mutable-surface sandbox, 5-layer gate, shadow firewall, anti-hardcode
  detector) preserved.
- **Semantic hardcode:** none. Edits = (1) prompt INPUT-shape passthrough of
  already-existing artefacts (firewall EXTENDED, not weakened); (2)
  config-driven STEERING (skill phase/UC selection bias, not a rule against
  any utterance); (3) `lessons.enabled` flag; (4) forensic per-exp snapshot;
  (5) literal labels-only `propose.txt` sentence. Sunset: n/a.
- **Generalization coverage:** n/a (pure-infra; no CaseSpec touched).
  Validation = unit tests + `--dry-run -n 2` checklist; downstream metric =
  S-Y2 Part C `full_on_gap_hit_rate`.

## §7 — Codex review plan (§4.3 trigger #3, REQUIRED)

Per-sub-sprint Codex REQUIRED before S-Y2 Part C. Deliver-agent authors
`compact/M-Auto-7-S-Y1.5-review-prompt.md` at close. Focus:

- P0-B firewall: verify the test FAILS when the
  `analyzer._summarize_suite_cases` shadow branch is removed (proof in §3).
- P0-C: shadow-case-id exclusion in `validate_pilot_config` (filenames
  only); lazy `build_skill_phase_usecase_map` (no cache); `--dry-run -n 2`
  rationale spot-check (§5).
- P0-A: shadow-aggregate-only passthrough (`tier_breakdown` carries only
  Layer-4 aggregate metrics by the `evaluate(...)` default API surface).
- P1: placeholder-not-empty path.
- Scope: confirm rationale describes EDIT SHAPE, not case_id matching;
  note the additive `cli.py` edit (§1 #5 scope note).

## §8 — Open questions

- **OQ-S87.1 (L-006 consumed_by_proposer):** with `lessons.enabled:false`,
  the compactor still writes the next lesson (L-006) to `lessons.md` at the
  K=10 boundary, but the proposer does NOT consume it (placeholder). For the
  S-Y2 pilot this is intended (the May-31 lessons point off-gap). Open:
  when/whether to re-enable lessons post-pilot, and whether L-006+ authored
  during a disabled window should be quarantined or trusted on re-enable.
- **OQ-S87.2 (pilot schema versioning):** `pilot.schema_version: 1` is
  recorded in every `pilot_snapshot` but there is no migration/validation of
  the version yet. Open: the policy when the pilot schema evolves
  (reject-unknown vs best-effort-read) and whether `validate_pilot_config`
  should hard-check the version.
- **OQ-S87.3 (lessons opt-out scope = proposer only):** P1 disables lessons
  in the PROPOSER per the literal contract; the ANALYZER still receives
  `LESSONS_MD` ("use it to avoid re-reporting"). The dominant feedback
  vector was the proposer (run-1 rationales cited L-005), but the analyzer
  could still carry pre-CS4 bias. Open: extend the opt-out to the analyzer?
- **OQ-S87.4 (candidate-results availability):** `_read_recent_candidate_results`
  depends on `eval_traces_path` files surviving on disk; applier cleanup +
  forensic retention determine how many of the last K are actually
  readable. Open: confirm retention during the S-Y2 Part C run so the
  analyzer's candidate landscape is non-empty.
