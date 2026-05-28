# Codex review prompt — Milestone M-Auto-1A — Auto-Evolution Build — Milestone-shared Anti-Hardcode review (§4.3 default)

You are the **Milestone-Shared Anti-Hardcode Review Agent for Milestone M-Auto-1A — Auto-Evolution Build**. This is the **milestone-shared Codex pass** dispatched per `docs/current/iteration_governance.md` §4.3 default at M-Auto-1A close. It evaluates the cumulative commit range covering all four sub-sprints (S-Auto-1 through S-Auto-4).

A separate **per-sub-sprint Codex review** of S-Auto-4 alone (per §4.3 trigger #2) has ALREADY been completed and archived at `docs/codex-findings.md` (verdict: `pass / 0` — `approve with downgrade-to-signal follow-up`; cumulative scope of that pass was `cf0127f..1feef1f` for the S-Auto-4 dev commit only). Your milestone-shared pass MUST NOT re-litigate the per-sub-sprint findings — instead, cross-check S-Auto-1/2/3 (which were Codex-deferred to this milestone-shared close per §4.3 default) and sanity-check that the four sub-sprints in cumulative composition do not introduce inter-sub-sprint interactions Codex's per-sub-sprint pass could not see.

The cumulative commit range under review:

```
1fb2062 (exclusive)..b6b627b (inclusive)  — 9 commits

6b8087d  M-Auto-1A opening: milestone objective + Sprint 54 / S-Auto-1 contract + dev prompt
85fc409  Sprint 54 / S-Auto-1 — autoloop subsystem scaffold + YAML diff sandbox
48892d2  S-autoloop-1-2  (S-Auto-1 close + S-Auto-2 planning round)
eb55322  Sprint 55 / S-Auto-2 — four-tier fitness evaluator + shadow runner + baseline loader
d9e4086  Sprint 55 / S-Auto-2 close + Sprint 56 / S-Auto-3 planning round + meta-agent config
cf0127f  Sprint 56 / S-Auto-3 close + Sprint 57 / S-Auto-4 planning round + anti-hardcode kernel contract
         (NB: S-Auto-3 dev work + close artefacts were bundled into this single commit per the
         human's close commit cadence; the dev scope of S-Auto-3 is the autoloop/{loop,memory,
         meta_agent}.py + applier.py rewrite + cli.py wiring + the meta_agent prompt files + 64
         new tests — verify via `docs/sprints/sprint-056-handoff.md` and `git diff d9e4086..cf0127f`.)
1feef1f  Sprint 57 / S-Auto-4 — anti-hardcode kernel + content validator + gaming checks
         (per-sub-sprint Codex archived at docs/codex-findings.md; verdict pass / 0)
8f9b754  Sprint 57 / S-Auto-4 close + M-Auto-1A milestone-close preparation
         (deliver-agent close-bundle: 10-handoff §0+§1 refresh + milestone_objective §6.2 close-
         state + action_bank §6 Sprint 57 row + sprint_objective archive + scoring_code_baseline_sha
         first-fill (LITERAL git commit "1feef1f"; subsequently corrected — see b6b627b))
b6b627b  Sprint 57 / S-Auto-4 close fix-up — scoring_code_baseline_sha content-hash correction
         (deliver-agent post-Codex fix-up: corrected scoring_code_baseline_sha to the SHA-256
         content hash 5177b674... computed via gaming._compute_scoring_code_sha(); deliver-agent
         disposition note appended to docs/codex-findings.md; R-S57-anti-hardcode-whenever-arrow-
         synonym-bypass opened in docs/action_bank.md §5 as the contract-expected M-Auto-1B
         calibration target from per-sub-sprint Codex Axis B finding.)
```

The previous milestone (M5 — Observability Coherence) closed at commit `c9390dc` (A — Clean PASS 2026-05-25). All Java + eval-interactive Python baselines from M5 close are the inherited baselines this milestone-shared review compares against.

---

## Read order (minimal)

Read only:

1. `AGENTS.md` (auto-loaded via constitution chain — pulls `doc_governance.md` + `agent_context_guide.md` + `iteration_governance.md`).
2. This prompt (self-contained executable view per `iteration_governance.md` §9 invariant).
3. **The per-sub-sprint Codex archive at `docs/codex-findings.md`** — your milestone-shared pass starts from those findings and extends them; you do NOT re-litigate Axes A-E for S-Auto-4 alone, but you DO confirm the S-Auto-4 verdict still holds when the cumulative S-Auto-1..S-Auto-3 surfaces are taken into account.
4. **The four sub-sprint handoffs** (the dev's own close artefacts):
   - `docs/sprints/sprint-054-handoff.md` — S-Auto-1: mutable-surface contract + YAML diff sandbox
   - `docs/sprints/sprint-055-handoff.md` — S-Auto-2: four-tier fitness evaluator + shadow runner + baseline loader
   - `docs/sprints/sprint-056-handoff.md` — S-Auto-3: loop orchestrator + meta-agent + 3-layer memory + applier real impl + cli wiring
   - `docs/sprints/sprint-057-handoff.md` — S-Auto-4: anti-hardcode kernel + content validator + gaming checks

You may sample (NOT embed) the following code paths for verification:
- `autoloop/autoloop/sandbox/yaml_diff_validator.py` — S-Auto-1: mutable-surface whitelist sandbox
- `autoloop/autoloop/scoring/tier_evaluator.py` + `eval_runner.py` + `baseline_loader.py` — S-Auto-2: 5-layer lexicographic fitness with shadow firewall
- `autoloop/autoloop/loop.py` — S-Auto-3: orchestrator state machine
- `autoloop/autoloop/meta_agent/{analyzer,proposer,lessons_compactor,llm_client}.py` + `prompts/{analyze,propose,compact}.txt` — S-Auto-3: meta-agent + prompts (this is where the §1.7 forbidden-list is TAUGHT to the LLM rather than ENCODED in code)
- `autoloop/autoloop/memory/{experiments_log,iterations_index,lessons_log}.py` — S-Auto-3: 3-layer memory with shadow firewall regression test
- `autoloop/autoloop/sandbox/applier.py` — S-Auto-3: git + Spring alt-port spawn + health-probe + cleanup
- `autoloop/autoloop/sandbox/anti_hardcode_check.py` + `content_validator.py` + `autoloop/autoloop/scoring/gaming.py` — S-Auto-4: the structural §1.7 guard (already independently verified per-sub-sprint; sanity-check at cumulative level)
- `autoloop/autoloop/cli.py` — S-Auto-3+S-Auto-4: 5 subcommands wired + audit rendering extension
- `autoloop/config.yaml` — cumulative: fitness + meta_agent + lessons + content_validator + anti_hardcode + gaming blocks
- `autoloop/program.md` — cumulative: §4 forbidden-by-construction status table with rows 1/2/3/4/5/6 all DELIVERED
- `docs/milestone_objective.md` — full milestone definition + §6.1 OQ-S56.1 disposition + §6.2 close-state
- `docs/action_bank.md` §5 — open R-items including the NEW `R-S57-anti-hardcode-whenever-arrow-synonym-bypass` opened at S-Auto-4 close fix-up

---

## Embedded milestone class + Codex review plan (from `docs/milestone_objective.md` §1 + §8)

| Sub-sprint | Layer (primary) | §7 stanza | Codex review |
|---|---|---|---|
| S-Auto-1 / Sprint 54 — Mutable-surface contract + YAML diff sandbox | `infra` | REQUIRED | **Milestone-shared (this review)** |
| S-Auto-2 — Four-tier fitness evaluator + shadow runner | `eval_spec` | REQUIRED | **Milestone-shared (this review)** |
| S-Auto-3 — Loop orchestrator + meta-agent + 3-layer memory + applier real impl + cli wiring | `infra` | REQUIRED | **Milestone-shared (this review)** |
| S-Auto-4 — Anti-hardcode kernel + content validator + gaming checks | `eval_spec` | REQUIRED | **Per-sub-sprint COMPLETE (verdict `pass / 0`); this milestone-shared pass cross-checks the cumulative composition** |

---

## Embedded milestone goal (from `docs/milestone_objective.md` §2)

Stand up the full **build-time substrate** for a Skill-YAML-only auto-evolution loop so that, at M-Auto-1A close, a single command can:

1. Take ONE meta-agent-proposed hypothesis (a single Skill YAML edit on a white-listed LLM-soft field).
2. Validate it through `autoloop/sandbox/` — reject anything that touches structural fields, trace_check, mandatory_for, guardrails, state_inheritance, tools_required, applicable_use_cases, or crosses multiple files / multiple Skills.
3. Run it through `autoloop/sandbox/anti_hardcode_check.py` — auto-reject if it encodes if-else patterns, keyword lists, eval-case-id references, or LLM-ownership-shrinking language.
4. Apply, mvn-compile, Spring-restart on an alternate port, run the four-tier eval, and produce a lexicographic 5-layer verdict (Tier-0 → Tier-1 → Tier-2 → improvement-threshold → shadow regression).
5. Write the verdict + diff + raw eval artefacts + sanitized failure taxonomy to three persistent memory layers.

**Dataset scope (v1)**: per-iteration fitness = `bad_cases` (12) + `anchor_outcome` (12) + `shadow` (23) = 47 cases. The `anchor` 159-case suite is intentionally NOT used as a per-iteration fitness signal in v1 (legacy `expected_tool_sequence` etc. authored pre-§5.6).

**Layer 0 scope (v1)**: v1 auto-loop Layer 0 = Python `hard_checks` Tier-0 family. The `eval/src/main/java/com/gumtree/csagent/eval/` Java module (legacy `GateEvaluator` with 11 historical "Java replay" gates) is **legacy v1-demo-era code, superseded by eval_interactive** at commit `1b71fa4` (last touched at `e247c56`, pre-Sprint-1); zero callers in any current eval flow. Sprint 55 schema-check disposition: governance-hygiene R-item `R-eval-java-module-retirement` opened for M-Auto-2+ cleanup.

**Crucially M-Auto-1A ships NO Skill YAML edits and NO cherry-picks to main.** Dry-run mode + a single live iteration that PRODUCES a verdict (regardless of keep/discard) are the close-gate evidence. The first overnight batch + first human review of kept candidates + first cherry-pick to main are M-Auto-1B territory.

---

## Embedded milestone §6 hard fences (verbatim from `docs/milestone_objective.md`)

1. **No edits** to any file under `server/src/main/java/**`. The Runtime side stays byte-identical to M5-close `c9390dc`.
2. **No edits** to any file under `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`. The Evaluator side stays byte-identical.
3. **No edits** to any file under `server/src/main/resources/{skills,prompts,scripts,config,mock}/**`.
4. **No edits** to `data/**`, `db/migration/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/iteration_governance.md`.
5. **No edits** to sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-053-*` or any prior milestone archive under `docs/milestones/`.
6. **No `git add -A`** by the dev agent; deliver-agent close-bundle artefacts bundled by the human at close.
7. **No cherry-pick to main** of any `autoloop/exp-N` branch in M-Auto-1A.
8. **No new Tier-0 invariant**. C2/C3 DEFER continues per M2-close verdict.
9. **No cross-file diff** by meta-agent ever (sandbox enforces). Single-Skill, single-field-class diff per iteration.
10. **No shadow-set leakage to meta-agent**. Shadow result fed back to loop is aggregate `{shadow_regression_detected: yes|no, drop_pct: <float>}` only. Per-case shadow failures NEVER reach `meta_agent/proposer.py`. Structural firewall enforced in `autoloop/scoring/tier_evaluator.py` (separate API surface for loop vs. human audit).
11. **No mutation of `eval_interactive/results/` schema**. Auto-loop produces parallel output under `autoloop/results/` only.
12. **No editing of `docs/codex-findings.md` during M-Auto-1A sub-sprint execution** (per-sub-sprint Codex at S-Auto-4 close already wrote findings; milestone-shared Codex extends them).

### 6.1 OQ-S56.1 BLESSED-IN-SCOPE disposition (verbatim from `docs/milestone_objective.md` §6.1; pre-blessed before your review)

S-Auto-3 surfaced **OQ-S56.1**: one out-of-standard-scope edit to `eval_interactive/eval_interactive.yaml` (literal `bot.base_url: http://localhost:8080` → env-var indirection `bot.base_url: ${CSAGENT_BACKEND_URL}` + 7-line comment block). Deliver-agent + human disposition 2026-05-27 at S-Auto-3 close: **BLESSED as in-scope**.

Reasoning (summary; full text in `docs/milestone_objective.md` §6.1):
1. The edited file is `eval_interactive/eval_interactive.yaml` — a **sibling of the inner `eval_interactive/eval_interactive/` directory**, NOT a child of it. §6 item 2 fence covers `eval_interactive/eval_interactive/**` (inner Python module) + `eval_interactive/case_specs/**` + `eval_interactive/case_specs_shadow/**`; the top-level YAML is NOT covered by the letter of the fence.
2. Required by §5 "Live iteration end-to-end" — alt-port routing for the eval-interactive subprocess.
3. Structurally safe (loop sets `CSAGENT_BACKEND_URL`; `eval_runner.py` sets fallback default; no semantic logic change).
4. §6 item 2 fence still applies in full to inner module + case_specs + case_specs_shadow (verified byte-identical).

**Codex MUST NOT re-litigate this disposition.** The blessing was made at S-Auto-3 close to give the milestone-shared review a stable hard-fence list. Your role is to verify the disposition is HONOURED — i.e., the diff at `eval_interactive/eval_interactive.yaml` is byte-identical to the 9-line OQ-S56.1 diff and no further out-of-fence eval-interactive surface edits exist across the cumulative range.

### 6.2 S-Auto-4 close-state + remaining M-Auto-1A milestone-close gates (from `docs/milestone_objective.md` §6.2)

All 4 sub-sprints CLOSED at this point. Gates SATISFIED at S-Auto-4 close:
- Java test baseline `1183 / 1-inherited / 0 / 2` UNCHANGED from M5 close `c9390dc`.
- Python test baselines: eval_interactive `486 passed, 3 failed` UNCHANGED; autoloop subsystem grown 48 → 83 → 147 → 216 across S-Auto-1/2/3/4.
- Anti-hardcode auto-check regression test calibration table: 11/11 forbidden FAIL + 4/4 clean PASS + 2/2 borderline FLAG (FLAG_FOR_CODEX rate 11.76%, well under 25%).
- Sandbox structural-reject coverage from S-Auto-1.

Gates PENDING at milestone close (parallel to your milestone-shared review):
1. Bad-case suite manual review (§5.6 primary; `parallel=1`).
2. Shadow regression-safety rerun (§5 NEW M5-parity gate).
3. Live-iter smoke (§5 + OQ-S56.5; needs `AUTOLOOP_META_LLM_API_KEY` + built `server/` jar).
4. **This milestone-shared Codex review** (`pass` or `approve with downgrade-to-signal follow-up`).

**`config.fitness.scoring_code_baseline_sha`**: deliver-agent backfilled `"5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c"` at S-Auto-4 close-bundle fix-up (the actual SHA-256 content hash from `gaming._compute_scoring_code_sha()` at commit `1feef1f`). First-fill of literal `"1feef1f"` (git commit SHA) was a representation mismatch flagged by per-sub-sprint Codex; resolved at fix-up commit `b6b627b`. Re-verified `_check_scoring_code_drift(config=config)` returns `[]` (silent in steady state).

---

## Embedded §4.1 nine-question anti-hardcode kernel (verbatim from `docs/current/anti-hardcode-review-kernel.md`)

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

- `approve` — the change is not a semantic hardcode, or is justified
  as protecting a current Tier-0 invariant with adequate generalization
  coverage and a clear rollback if temporary.
- `approve with downgrade-to-signal follow-up` — the change is
  acceptable as an interim measure, but a follow-up sprint must
  convert it into a soft signal projected to the LLM. Name the
  trigger that should fire the conversion.
- `reject as semantic hardcode` — the change encodes a soft semantic
  decision the LLM should own; questions 1 and 2 fail, or questions
  5 / 6 fail, with no Tier-0 claim and no sunset plan.
- `needs human architecture decision` — the change crosses an
  unresolved governance question (new escalation reason enum value,
  new Tier-0 candidate, LLM-vs-Java boundary shift, new tool surface)
  and a human reviewer must decide before merge.

Do not rewrite the PR. Do not propose a code fix beyond naming the
layer in docs/current/iteration_governance.md §3 that the fix should
target.
```

---

## Milestone-shared verification axes

The per-sub-sprint Codex pass already walked Axes A–E for S-Auto-4 in isolation. Your milestone-shared pass extends to the cumulative range with these axes:

### Axis M1 — §4.1 nine-question kernel walk (cumulative)

Walk Q1–Q9 against the cumulative S-Auto-1..S-Auto-4 range. Special attention to:
- **Q1 / Q5 / Q6**: does the `autoloop/sandbox/anti_hardcode_check.py` regex / heuristic set itself encode §1.7-violating decision logic? (The defender must not be a hardcode pump.) Per-sub-sprint Codex Axis A confirmed this for S-Auto-4 in isolation; re-confirm at cumulative level by spot-checking the rule definitions did not drift between S-Auto-1 sandbox white-list and S-Auto-4 detector.
- **Q2**: does any auto-loop code path attempt to create a new Tier-0 invariant? Special attention to S-Auto-3 `loop.py` orchestrator (it could theoretically encode runtime invariants in its state machine guards) and S-Auto-2 `tier_evaluator.py` Layer 0 (the Python `hard_checks` family is the inherited Tier-0 floor; ensure auto-loop does not extend it).
- **Q4**: does the meta-agent `propose.txt` / `analyze.txt` / `compact.txt` prompt leak any eval `case_id` / `session_id` / known eval phrase that could prejudice the meta-agent's outputs? Read the three prompt files in full; they SHOULD teach the §1.7 forbidden-list verbatim (per `iteration_governance.md` §1.7) without naming a specific eval case.
- **Q8**: cumulative generalization coverage — target + neighbor + negative + shadow case families across S-Auto-1 sandbox + S-Auto-4 detector. The 11/11 forbidden + 4/4 clean + 2/2 borderline calibration table in `sprint-057-handoff.md` §4 + S-Auto-1 sandbox 12+ structural reject fixtures + S-Auto-2 lexicographic verdict 5-layer coverage = the cumulative coverage claim.

### Axis M2 — Hard-fence verification (§6 + §6.1)

Independently run:

```
git diff --stat 1fb2062..b6b627b -- \
  server/ eval/src/main/java/ eval_interactive/eval_interactive/ \
  eval_interactive/case_specs/ eval_interactive/case_specs_shadow/ \
  data/ db/migration/ server/src/main/resources/ \
  docs/foundational/ docs/runtime_freeze_and_risk_policy.md \
  docs/current/iteration_governance.md
```

Expected output: EMPTY. Every listed path is byte-identical between M5 close `c9390dc` and M-Auto-1A close `b6b627b`.

Then independently verify the **OQ-S56.1 blessed surface** at `eval_interactive/eval_interactive.yaml` shows ONLY the 9-line OQ-S56.1 diff (env-var indirection + comment block):

```
git diff 1fb2062..b6b627b -- eval_interactive/eval_interactive.yaml
```

If any further out-of-fence eval-interactive edit exists, FLAG. The OQ-S56.1 blessing was scoped to that single diff; any second-surface edit invalidates the disposition.

Verify §6 item 5 sprint archive integrity:

```
git diff --stat 1fb2062..b6b627b -- docs/sprints/sprint-001-* docs/sprints/sprint-053-*
```

Should return EMPTY (the carve-out for sprints 054+ is allowed because they are M-Auto-1A's own sub-sprint archives).

### Axis M3 — Sandbox + detector + gaming cumulative consistency

Three independent structural defences ship across M-Auto-1A: S-Auto-1 sandbox white-list (rejects diffs touching structural Skill YAML fields), S-Auto-4 anti-hardcode detector (rejects soft-semantic hardcodes in mutable-surface text), S-Auto-2 + S-Auto-3 shadow firewall (per-case shadow info never reaches the loop or proposer). Verify these defences COMPOSE correctly:

1. **Sandbox + detector composition**: a hypothesis with a structural Skill YAML field target (e.g., `target_field_path: $.applicable_use_cases`) MUST be rejected by S-Auto-1 sandbox (NOT by S-Auto-4 detector — the detector operates on `after_value` text, the sandbox operates on field-path). Confirm by reading `autoloop/autoloop/loop.py` state machine to verify the sandbox check (step 3) precedes the anti-hardcode check (step 4).
2. **Detector + content_validator ordering**: content_validator runs at step 2.5 (pre-sandbox; structural integrity); detector runs at step 4 (post-sandbox; semantic guard). Confirm a content-broken hypothesis discards earliest, before sandbox + detector ever fire.
3. **Shadow firewall holds end-to-end**: the firewall is implemented as a separate API surface in `tier_evaluator.evaluate(...)` (default `audit=False` returns aggregate-only Layer 4 metrics; `audit=True` opens the per-case path for human audit only). Verify the `experiments_log` JSON-roundtrip regression test in `autoloop/tests/test_memory.py` (`test_experiments_log_shadow_firewall_no_per_case_key`) + the loop-level test in `autoloop/tests/test_loop.py` (`test_shadow_firewall_holds_in_experiments_log`) + the CLI test in `autoloop/tests/test_cli_integration.py` (`test_cli_audit_default_respects_shadow_firewall`) all PASS. Confirm by grep + spot-read.

### Axis M4 — Meta-agent prompt §1.7 boundary check

Read `autoloop/autoloop/meta_agent/prompts/{analyze.txt, propose.txt, compact.txt}` in full (60 + 108 + 49 LOC). Verify:

1. The prompts **TEACH** the LLM the §1.7 forbidden-list verbatim — i.e., the prompts reference the rule, not the implementations of the rule. Concretely, the propose.txt should include the forbidden-list as STRINGS the LLM reads, not as Python code that the LLM is asked to emulate.
2. The prompts **do not name specific eval `case_id`s** (`cs011`, `cs015`, etc.) or specific user utterances or specific expected assistant answers. The S-Auto-4 detector (the post-LLM gate) has its own D2 regression test; the prompts are the PRE-LLM contract and have a different (but related) discipline.
3. The prompts **do not enumerate keyword lists** (per §1.7 "no keyword / regex / if-else / enum / per-UC matrix as a soft semantic decision rule" — applies to prompts too via §1.7).

A prompt-level §1.7 violation here would be more subtle than a detector violation, because the harm path is via the LLM's emitted hypotheses (which the detector might catch — or might not, per the Axis B Codex-authored synonym bypass). The milestone-shared review extends the per-sub-sprint review by including this prompt-level check.

### Axis M5 — `program.md` row 1–6 status table coherence

Read `autoloop/program.md` §4 (forbidden-by-construction table). At M-Auto-1A close, the expected state is:

- Row 1 (sandbox white-list) — DELIVERED (S-Auto-1)
- Row 2 (4-tier lexicographic fitness) — DELIVERED (S-Auto-2)
- Row 3 (anti-hardcode auto-check) — DELIVERED (S-Auto-4; per S-Auto-4 contract)
- Row 4 (shadow regression gate) — DELIVERED (S-Auto-2)
- Row 5 (shadow result firewall) — DELIVERED (S-Auto-2)
- Row 6 (no main-branch cherry-pick by the loop itself) — DELIVERED Hybrid per OQ-S55.1 (S-Auto-3)

If any row is in a `DEFERRED` or `PARTIAL` state, FLAG — the milestone is not closure-ready.

### Axis M6 — Reproducibility

Per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`, every numeric claim in the deliver-agent's M-Auto-1A close package must be reproducible. Spot-check:

- `cd autoloop && uv run --extra dev pytest -q` → MUST reproduce `216 passed, 1 warning`.
- `cd eval_interactive && uv run python -m pytest --tb=no -q` → MUST reproduce `486 passed, 3 failed`.
- `mvn test -B -pl server` (if you choose to reproduce Java baseline) → MUST reproduce `Tests run: 1183, Failures: 1, Errors: 0, Skipped: 2`; the only failure inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`.

If any baseline diverges from the M5 close baseline (other than the autoloop pytest growth 0 → 216), FLAG.

### Axis M7 — Adversarial spot-check at cumulative level

Per-sub-sprint Codex Axis B already authored ≥3 adversarials for S-Auto-4 detector and found 1 bypass (`Whenever ... =>`). Your milestone-shared pass MAY (but is not required to) construct an additional adversarial that tests the **CUMULATIVE** defence chain — e.g., a hypothesis whose `after_value` is detector-clean but whose `target_field_path` is sandbox-clean-but-semantically-questionable. The expected outcome is that the cumulative chain catches the hypothesis at SOME stage (sandbox if structurally wrong, detector if textually forbidden, content_validator if integrity-broken). If the cumulative chain has a hole the per-sub-sprint review could not see, FLAG and recommend a fix-iteration sub-sprint.

Note: the EXISTING bypass `Whenever ... =>` is recorded as M-Auto-1B target via `R-S57-anti-hardcode-whenever-arrow-synonym-bypass` in `docs/action_bank.md` §5. Codex's milestone-shared pass should NOT re-find this; check for NEW cumulative-level holes.

### Axis M8 — Bad-case suite + shadow regression respect (human-judgment gate)

Per `iteration_governance.md` §5.6, bad-case suite manual review is a HUMAN-JUDGMENT gate; Codex MUST NOT auto-PASS / auto-FAIL. Codex's role here is to:

1. Verify the deliver-agent + human manual review verdicts are present in the milestone close artefacts (will be in `docs/10-handoff.md` §1 close paragraph at milestone close; if absent at the time of your review, FLAG as evidence-pending rather than re-judging).
2. Verify internal consistency: if the deliver-agent records "PASS×5 + IMPROVING×4 + FAIL×3" matching M5 close distribution, the cumulative range MUST NOT have any `server/` / Java / eval_interactive Python touch that could explain a deviation (you already verified this in Axis M2).
3. Verify the shadow regression rerun produced no new failure beyond `R-shadow-fixture-empty-form-session-create-400`.
4. Verify the live-iter smoke is recorded as either PASS-with-evidence OR explicitly marked blocked / waived-by-human (NOT silently skipped). M-Auto-1A's live-iter close-gate accepts ANY decision (keep or discard) so long as the FULL pipeline executes; if the gate is `blocked` per OQ-S56.5 prerequisite (missing `AUTOLOOP_META_LLM_API_KEY` or built `server/` jar), the milestone close must explicitly acknowledge the waiver — silent skip is FAIL.

---

## Output format (per `iteration_governance.md` §4.2 sprint-close header, lifted to milestone level)

Append your findings to `docs/codex-findings.md` (DO NOT overwrite the per-sub-sprint S-Auto-4 archive that already exists at the top; append below the deliver-agent disposition note). Use:

```
## Milestone M-Auto-1A — Auto-Evolution Build — Milestone-Shared Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph naming the verdict + the axes that were dispositive>

## Review Evidence
- <reproducibility commands run + their outputs>
- <commit range walked + a one-line per-commit note>

## 1. Nine-Question Anti-Hardcode Kernel Results (cumulative)
1. Q1 ... <walk>
2. Q2 ... <walk>
...
9. Q9 ... <walk>

## 2. Hard-Fence Walk (cumulative; §6 + §6.1)
<axis M2 findings>

## 3. Sandbox + Detector + Gaming Cumulative Consistency
<axis M3 findings>

## 4. Meta-Agent Prompt §1.7 Boundary Check
<axis M4 findings>

## 5. program.md Status Table Coherence
<axis M5 findings>

## 6. Reproducibility Spot-Checks
<axis M6 findings>

## 7. Cumulative Adversarial Spot-Check
<axis M7 findings; if no NEW hole found beyond the M-Auto-1B-tracked Whenever-arrow bypass, say so>

## 8. Bad-Case Suite + Shadow + Live-Iter Smoke Evidence Audit
<axis M8 findings; explicitly classify the live-iter smoke result as PASS-with-evidence / explicitly-blocked-or-waived / silent-skip-FAIL>

## Verdict
<one of: approve | approve with downgrade-to-signal follow-up | reject as semantic hardcode | needs human architecture decision>

## Notes for M-Auto-1B planning round
<observations to inform M-Auto-1B kick-off; e.g., calibration data points worth tracking, R-items to consume in early M-Auto-1B sub-sprints>
```

---

## Constraints

- **Do NOT edit code.** Codex's role is to read + reason + return a verdict.
- **Do NOT re-litigate the per-sub-sprint Codex Axes A–E for S-Auto-4 alone** — those are concluded. Your milestone-shared pass extends them to the cumulative range; cross-check, do not duplicate.
- **Do NOT re-judge §5.6 bad-case suite human-judgment verdicts.** Verify presence + internal consistency (Axis M8).
- **Do NOT propose new Tier-0 invariants.** Q2 detects attempts to invent Tier-0; Codex's role is to flag, not bless.
- **Do NOT broaden scope beyond M-Auto-1A.** The cumulative range is `1fb2062..b6b627b` (exclusive of `1fb2062`). M-Auto-1B planning notes are welcome (in your "Notes for M-Auto-1B planning round" section) but NOT in-scope for the verdict.
- **The verdict set is exactly the four options listed in the §4.1 kernel.** No verdict invention.
- **Codex MUST return `pass` (or `approve with downgrade-to-signal follow-up`) BEFORE M-Auto-1A milestone close can complete.** `reject` triggers a fix-iteration sub-sprint extending M-Auto-1A. `needs human architecture decision` halts and surfaces to deliver-agent + human.

Cumulative scope claim: `1fb2062..b6b627b` covers all four sub-sprints of M-Auto-1A plus the milestone-opening commit and the close-bundle fix-up; deliver-agent reproduced `216 pytest PASS` at HEAD `b6b627b` 2026-05-27; per-sub-sprint Codex returned `pass / 0` for S-Auto-4 at intermediate commit `1feef1f`.
