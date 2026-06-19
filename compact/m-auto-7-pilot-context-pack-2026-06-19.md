# M-Auto-7 pilot context pack (2026-06-19)

Transferable handoff for a new session. Branch `auto-loop-branch`, HEAD `d8404c86`
(clean tree). Covers: S-Auto-40 WP1-A (CLOSED) → M-Auto-7 pilot resume →
S-Auto-41 proposer steering → S-Auto-42 tier0 flaky defer rule.

---

## 1. 背景 (Background)

- Repo builds an LLM-first CS agent for a classifieds marketplace. Governance:
  `AGENTS.md` → `docs/current/iteration_governance.md` (§1 Constitution; §1.7
  forbidden incl. "don't widen eval spec to accept a real bot mistake"; §3
  fix-layer; §5 eval acceptance; **§5.4** eval_spec; **§5.7** mocked-LLM evidence
  gate — real-LLM is the only behaviour evidence).
- **Milestone M-Auto-7** = "autoloop readiness + CS4 entity-context pilot". The
  **autoloop** (`autoloop/`) is a meta-agent that proposes edits to **4 soft
  fields** (`$.procedure`, `$.grounding_instruction`, `$.escalation_policy`,
  `$.critical_steps[*].desc`) of **6 Skill YAMLs** (`server/src/main/resources/
  skills/{discover_triage,resolve_faq_grounded_answer,resolve_intake_collect_and_handover,confirm,escalate,terminal}.yaml`),
  scored by a **V3 noise-aware lexicographic fitness gate** (5 layers: tier0
  safety → tier1 outcome → tier2 critical-flow → tier3 improvement → tier4 shadow).
- **Two PRIMARY UC-A bad cases** (the pilot targets):
  - `cs_uc_a_no_ad_id_ad_specific` — resolve; bot must recognise an ad-specific Q
    is unanswerable without the ad reference, ask for it, then ground in listing.
  - `cs_uc_a_loaded_listing` — resolve; bot must consult the specific listing
    (tool-returned OR pre-loaded customer context) and **substantively use**
    listing-specific fields (status/category/price/location/posted_date).
- Eval framework: `eval_interactive/` (Python). Tests:
  `eval_interactive/.venv/bin/python -m pytest`; autoloop tests:
  `cd autoloop && .venv/bin/python -m pytest` (or `uv run python -m ...`).

## 2. 目标 (Goals)

1. **WP1-A (S-Auto-40, DONE):** encode the 2026-06-19 product decision —
   escalation-after-genuine-help is a VALID terminal when the user remains
   UNRESOLVED; resolve when SATISFIED — as a **declarative, condition-bound**
   conditional outcome acceptance for the 2 PRIMARY cases, **measurement-first**,
   **no bot/runtime/prompt/reason change**. (NOT an unconditional resolve-OR-escalate widen.)
2. **Resume M-Auto-7 pilot** from the **unchanged** canonical pre-pilot fitness
   baseline using the new measurement contract.
3. **S-Auto-41 (DONE):** fix proposer steering (proposer was OFF_TARGET).
4. **S-Auto-42 (DONE, safe core):** fix tier0 flake-sensitivity so near-coinflip
   shadow cases don't mis-attribute candidate regressions.

## 3. 已确认事实 (Confirmed facts)

**Measurement contract (WP1-A):**
- Simulator emits a same-call structured `user_state` ∈
  {`satisfied`,`unresolved_after_help`,`working`,`new_request`} in
  `eval_interactive/.../simulator/user_simulator.py::generate_next`; absent/invalid → None.
  Persisted per-turn (turn_id/produced_user_turn/user_state/goal_status/
  `signal_source="simulator_generate_next"`/`schema_version=1`) via
  `session_runner.run_session` → `SessionResult.user_state_signals` →
  `TraceData.user_state_signals` (attached in `batch/executor.py`) → results.json.
- The `bot_ended` break (session_runner ~196) precedes `generate_next` (~217) →
  escalation draws carry no terminal goal_status; the post-help UNRESOLVED signal
  is captured by the `generate_next` calls that DID run before the bot ended.
- Closure-quality structural marker = earliest bot turn with tool_calls +
  non-empty `source_ids` + grounded answer. Proves precondition only, NOT closure.
- Evaluator `eval_interactive/.../scoring/conditional_outcome.py` (generic, no
  case-id/free-text). Reduces signals to SATISFIED/UNRESOLVED(post-help)/UNKNOWN
  (goal_impossible kept separate, never UNRESOLVED; latest positive stance wins;
  no carry-forward/back-inference). `_check_correct_outcome` delegates to it when
  the CaseSpec carries `conditional_outcome_acceptance`. Verdicts: SATISFIED+resolve
  PASS · SATISFIED+escalate FAIL · UNRESOLVED+post-closure+escalate+ADJUDICATED PASS ·
  same w/o adjudication → CONDITIONAL_ELIGIBLE (score 0.0, never auto-PASS) ·
  UNRESOLVED+resolve FAIL(false-resolve) · UNKNOWN/neutral+escalate FAIL ·
  non-grounded+escalate FAIL.
- Adjudication registry `eval_interactive/case_specs/conditional_outcome_adjudications.yaml`:
  per-trace, keyed by `trace_id`(=session_id), matched on case_id +
  `closure_criterion_version` (sha256(closure_criterion)[:12]); only
  `verdict: accept_escalation` flips to PASS. cs_uc_a_loaded_listing closure
  version = `ba55899118d0`. Currently holds **4 `reject_escalation`** entries (the
  Phase-2 traces) — auditable record only, never flip to PASS.

**WP1-A Phase 2 (bounded real-LLM, DONE):** run dir
`eval_interactive/results/wp1a-phase2-measurement-20260619` (gitignored). 42/42
draws, **evidence floor MET**: SATISFIED=6, post-help UNRESOLVED=19, UNKNOWN/neutral
present, handover 6/6 retains state, **0** provenance + **0** alignment violations,
no leakage. Codex §4.1 = **approve** (in `docs/codex-findings.md`). The 4
CONDITIONAL_ELIGIBLE escalate traces (all `cs_uc_a_loaded_listing`) human-REJECTED
(closure not met: UC-B misclass / non-substantive listing use / budget-driven escalation).

**Pilot mechanics:**
- Pilot config = `autoloop/config.pilot-s-auto-38.yaml` (complete config; the
  global `autoloop/config.yaml` is stale at sha `f2f983cc` and NOT used by the pilot).
- `fitness.baseline_dir = eval_interactive/results/m-auto-7-prepilot-baseline-20260618-s_auto_38_split_full`
  (the unchanged pre-pilot FITNESS baseline; canonical `docs/current_eval_baseline.md`
  is a SEPARATE, frozen pointer).
- `fitness.scoring_code_baseline_sha = c35913067d4d…` (re-pinned this session from
  `7df8173c`; covers 6 files: `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming,aggregate,posterior}.py`).
- `fitness.tier_decision`: delta=0.10, p_regress=0.80, p_ambiguous_low=0.50, alpha_fdr=0.10, cross_case_count=2.
- `pilot.primary_targets = [cs_uc_a_no_ad_id_ad_specific, cs_uc_a_loaded_listing]`;
  `primary_targets_samples=11`; shadow/others n=5.
- Fitness gate uses **frozen baseline pass-counts** (k_b/n_b) and scores candidates live.
- Run cmds (require **clean tree** — dirty-index hazard; `caffeinate` for real runs):
  `cd autoloop && uv run python -m autoloop {preflight|dry-run|run} --config config.pilot-s-auto-38.yaml [-n N]`.
  Autoloop spawns its OWN backend on a free alt port (sets CSAGENT_BACKEND_URL).
- Standalone backend (for eval_interactive runs): `mvn -q -pl server spring-boot:run`
  (profile `local`; needs Postgres :5432 db `csagent` + Redis :6379; load env via
  `set -a; source .env.local; set +a`; health `/actuator/health`). Bot model
  deepseek-v4-flash; simulator moonshot-v1-32k temp 0.0. No proxy env for localhost.

**Pilot iterations run this session:**
- exp-87 DISCARD (tier1 TIER-S anti-误杀 flip; both PRIMARY 0; pre-steering, OFF_TARGET).
- exp-88/exp-89 DISCARD (steered, on_target; both regressed `cs38s01` tier0 originally,
  now correctly via real tier1 multi-case regression; both PRIMARY still 0 — UNPROVEN
  that grounding edits improve PRIMARY since short-circuited).
- exp-90 = **proposal-only dry-run, FROZEN** (do NOT apply/iterate without explicit
  go): edits `resolve_faq_grounded_answer.$.grounding_instruction`, on_target,
  anti-hardcode PASS, sandbox ACCEPT, all 6 disclosures, no leakage. Artefacts in
  `autoloop/results/runs/exp-90/` (gitignored).
- exp-82 = WITHDRAWN (do NOT revive).

**Mutation-surface analysis (read-only, `docs/diagnostics/autoloop-mutation-surface-scoping-2026-06-19.md`):**
- Runtime projects only the ACTIVE skill by (phase,useCase) (ContextProjectionBuilder ~L1071).
  Any `resolve_faq` field edit reaches only its FAQ UCs (UC-A/B/C/D/E/F/FP), never UC-G–K.
- `$.grounding_instruction` = narrowest legitimate semantic surface (escalation-free,
  grounding-dedicated). `$.procedure` = broadest (folds into system_instruction +
  conflates resolve-vs-escalate posture). `critical_steps[*].desc` gives **no runtime
  UC-scoping** (`mandatory_for` scopes only the tier2 EVAL, not projection); a NEW
  UC-A-only step is OUTSIDE the mutable surface (additions touch non-whitelisted paths).

**tier0 flake-sensitivity blocker (`docs/diagnostics/autoloop-tier0-flaky-posterior-data-blocker-2026-06-19.md`):**
- A per-check posterior needs per-check (k,n). NOT reliably persisted:
  `tier0_majority[check]` is a **bool only**; `failure_tags` **undercount** (exp-68
  cs11s01: 2/5 tags but tier0_majority=False); frozen baseline carries **no** per-check
  counts. A failure_tags posterior improperly CLEARED exp-68 (discard→KEEP) = widening.

## 4. 决策记录 (Decision record — user-authorized)

- **WP1-A CLOSED** at clean HEAD `052cc73b` (docs-only close record `0048b1a0`).
  Scope boundary (binding): validates **measurement + acceptance INFRASTRUCTURE only**;
  does NOT establish a conditional baseline and does NOT demonstrate improved bot behaviour.
- Closure-quality adjudication: **all 4 REJECTED** (recorded). `f3a4fff1` rationale =
  listing context not used substantively + budget-driven escalation (NOT "get_customer_context
  not called" — pre-loaded context is valid provenance).
- Auto-adjudication follow-up `R-conditional-adjudication-auto-triage` recorded (HELD):
  auto-reject clear cases; tool-returned AND pre-loaded context equally valid; require
  substantive listing-field use; human review only for borderline; never block the loop
  on conditional reviews that can't flip an already-false case_passed.
- **S-Auto-41 (proposer steering):** proposer-INPUT only; inject target PRIMARY +
  baseline evidence + 3 failure clusters; require 6 disclosures (causal_hypothesis,
  surface_rationale, blast_radius, escalation_preservation, expected_trace_change,
  no_benchmark_encoding); lightweight OFF_TARGET pre-check before eval; PREFER (not
  hard-code) `$.grounding_instruction`. Files: proposer.py, config_validator.py,
  prompts/propose.txt, config.pilot-s-auto-38.yaml `primary_target_steering` block.
- **S-Auto-42 = Option 1 (bool-only defer rule)** for tier0 flaky (NOT the posterior):
  deterministic Java gates + non-flaky tier0 = immediate DISCARD unchanged; a
  baseline-flagged FLAKY tier0-family violation is **DEFERRED → HOLD** (never KEEP,
  never auto-discard); otherwise-keep-eligible + deferred → `hold_inconclusive_flaky`.
  The 3-state posterior (CONFIRMED/CLEARED) + bounded re-sampling are **deferred** until
  per-check (k,n) is persisted on both candidate and a counts-carrying baseline.
- 3 observed bot-behaviour failure clusters (`docs/diagnostics/failure-clusters-uc-a-listing-2026-06-19.md`):
  UC-A→UC-B misclass; listing context ignored/superficial; budget-driven escalation
  before grounded resolution.

## 5. 当前任务 / 状态 (Current task / status)

- **All landed + committed on `auto-loop-branch`** (HEAD `d8404c86`, clean tree).
  Key commits: WP1-A `f64ad4b2`/`bebc2c31`/`8f569511`/`052cc73b`/`0048b1a0` + Codex
  `b6329041`; steering `5740b9c6`/`7f54b543`; findings/analysis `134f30cd`/`42bca1da`/
  `13cf57d8`; blocker `70595a26`; defer rule `ef100e73`; blocker-resolved `d8404c86`.
- S-Auto-42 validated: 4 defer unit tests + 34 golden tier_evaluator unchanged +
  zero-LLM oracle replay decisions PRESERVED (exp-67/68/73/74 + exp-88/89 all still
  discard; no KEEP-widening; oracle mechanism pins updated w/ justification) + scoring-SHA
  re-pin drift-clean. **412 autoloop tests green.**

## 6. 下一步 (Next steps — in order, all gated on human go)

1. Use the safe-core's first real evidence (e.g. an exp-90-style run, or replay) to
   **measure HOLD_INCONCLUSIVE_FLAKY frequency** → decide whether building the bounded
   confirmation re-sampling (+ the deferred posterior path, which needs per-check (k,n)
   persistence on both sides) is justified.
2. Only after that, **run exp-90 as ONE real iteration** (`autoloop run --config
   config.pilot-s-auto-38.yaml -n 1` on a clean committed tree, caffeinate) using the
   SAME frozen exp-90 proposal, so the result isolates the `$.grounding_instruction`
   surface hypothesis. Report keep/discard + PRIMARY movement + protected-case regression.

## 7. 注意事项 (Caveats / hard fences)

- **HELD (do not start without explicit go):** WP1-B, WP2, objective-alignment
  annotation, `R-conditional-adjudication-auto-triage`, the posterior/bounded-resampling
  path. **exp-82 stays WITHDRAWN.**
- **Frozen:** canonical pointer + `docs/current_eval_baseline.md`; conditional-acceptance
  semantics; bot/runtime/prompt/reason-enum/handover; the exp-90 proposal. **No re-bless,
  no baseline move.** scoring_code_baseline_sha re-pin ≠ re-bless (Sprint-091 precedent).
- Autoloop runs require a **clean committed tree** (per-exp commit sweeps the staged
  index). Real runs: `caffeinate`; a sleep-spanned LLM run is uncertifiable (kill+rerun).
- §5.7: mocked-LLM tests are wiring evidence only; never certify a behaviour/measurement
  change on them — real-LLM is the gate.
- **Pre-existing test failures (do NOT chase):** eval_interactive 6 —
  `test_s_eval_1_schema_and_scoring.py::TestBackwardCompatLoad::test_alice_bad_case_loads_unchanged`
  (stale 12-vs-17 bad_cases anchor) + 5× `test_rescore_s_auto_38_full_baseline.py::*`
  (`materialized` fixture `KeyError: form_context`). Proven pre-existing.
- Honest unresolved: (a) grounding edits are NOT shown to improve the PRIMARY (exp-88/89
  short-circuited pre-tier1); (b) whether the deferred posterior path is worth building
  depends on observed HOLD frequency; (c) the bounded confirmation re-sampling that would
  turn HOLD→KEEP/DISCARD is unbuilt (every flaky tier0 → HOLD until then).
- Deliver CAN dispatch Codex read-only: `codex exec --sandbox read-only -c
  model_reasoning_effort=high` (NOT xhigh); record verdict verbatim into `docs/codex-findings.md`.
