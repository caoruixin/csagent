Paste the content below this line into a fresh Claude Code session at the repo root. This is the dev-agent brief for **Sprint 45 — Bad-case suite expansion + trial milestone-close dry-run** (S-Eval-4; FOURTH sub-sprint of Milestone M3-Eval Coarse-to-Fine Evaluation Architecture).

---

You are Claude Code doing the **S-Eval-4 implementation** for Sprint 45 of NEW Milestone M3-Eval. This is a **DATA + CALIBRATION** sub-sprint — you ship 10-12 new bad-case YAMLs + a trial milestone-close manual review dry-run, NOT code change. You do NOT touch Tier-0 / Tier-1 / Tier-2 / Tier-3 scoring code (those are S-Eval-1 / S-Eval-2 / S-Eval-3 territory).

The S-Eval-3 (Sprint 44) close left S-Eval-4 UNBLOCKED per `iteration_governance.md` §4.3 trigger #2 satisfied. **Codex review for S-Eval-4 is deferred to M3-Eval milestone-shared close** per §4.3 default (no per-sub-sprint trigger fires for S-Eval-4). Your handoff §12 closure verdict stays EMPTY at dev close per `feedback_handoff_verdict_section_delegation.md`; deliver-agent + human fill at close.

## 1. Read order (cold-start; load in this order)

1. `AGENTS.md` (auto-loads `doc_governance.md` + `agent_context_guide.md` + `iteration_governance.md` §1 / §3 / §4 / §5 / §7 / **§5.6** / **§5.6.1-§5.6.3**).
2. `docs/milestone_objective.md` — M3-Eval north star; ESPECIALLY §3 S-Eval-4 row + §5 milestone acceptance bar (bad-case suite manual review is PRIMARY GATE per §5.6) + §6 hard fences (15 milestone-level items) + §8 Codex review plan.
3. `docs/sprint_objective.md` — S-Eval-4 contract (this is the live contract; you implement against §5 Files in scope + §6 Files NOT in scope + §8 §7-stanza + §10 STOP signals).
4. `docs/solutions/m3_eval_milestone_proposal.md` §6 S-Eval-4 (scope detail) + §4 decision 3 (bad-case selection criteria) + §5.6 (curated bad-case suite philosophy).
5. `docs/sprints/sprint-044-handoff.md` §12 closure verdict — LOAD-BEARING carry-overs: OQ-S44.1 executor wiring (carry to S-Eval-4 or S-Eval-5); OQ-S44.3 UC-FP moderation calibration (potential fix-iteration trigger); OQ-S44.5 real-LLM run posture; **OQ-S44.6 Python baseline runner discipline (you MUST use `uv run python -m pytest`, NOT `uv run pytest`)**.
6. `docs/sprints/sprint-042-handoff.md` §12.7 — per-UC anchor distribution observation (origin of UC-G/H/I/J thin-corpus priority for S-Eval-4 bad-case selection).
7. `eval_interactive/case_specs/bad_cases/_manifest.md` + `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` — Alice precedent for bad-case YAML schema + `bad_case_metadata` block + `closure_criterion` wording.
8. `eval_interactive/case_spec_overrides.yaml` — the 17 approved entries (your source pool; read-only).
9. `iteration_governance.md` §5.6 / §5.6.1 / §5.6.2 / §5.6.3 — bad-case suite governance + tiering + per-milestone selection + lifecycle downgrade.

## 2. Implementation actions

### 2.1 Pre-flight: source pool verification

Verify the 17 approved entries in `eval_interactive/case_spec_overrides.yaml` at HEAD:

```bash
cd eval_interactive && grep -c "^  status: approved$" case_spec_overrides.yaml
```

Expected: `17` (NOT 29 as proposal §4 mistakenly stated; deliver-agent reconciled at S-Eval-1 launch 2026-05-20). If the count is NOT 17, STOP per §10 #3 and surface.

Verify per-UC distribution in the 17 entries:

```bash
grep -B1 "^  status: approved$" case_spec_overrides.yaml | grep "active_use_case" | sort | uniq -c
```

Record the distribution in handoff §9 for S-Eval-5 + M3-Eval close calibration input.

### 2.2 Bad-case selection

Select **10-12 entries** from the 17 per these priorities (per `docs/sprint_objective.md` §2.1):

1. **UC-G / UC-H / UC-I / UC-J priority first** (thin-corpus UCs from S-Eval-1 §12.7 carry-over; preserved through S-Eval-2 + S-Eval-3 §12.7). Over-represent these in the 10-12 selections compared to the source-pool distribution.
2. **D1-D4 dimension coverage**: D1 mis-classification, D2 intake prefill gap, D3 lock-in escape, D4 stated_reason circularity. Prefer entries spanning all 4 dimensions. If a dimension is undersampled in the 17, STOP per §10 #3 and surface (deliver-agent + human decide among the 3 fallback paths: (a) accept narrower coverage; (b) supplement with non-override-sourced REAL sessions; (c) defer to follow-on milestone with R-item).
3. **Three highest-volume UC families**: UC-FP, UC-G, UC-H.
4. **Strongest semantic divergence from pre-override CaseSpec** (read the override `notes:` field; pick entries where the divergence is qualitatively largest).

Record the selection rationale in handoff §4 (per-case decision: why this entry, which dimensions/UCs it covers, why it was picked over alternates).

### 2.3 Bad-case YAML authoring

For each selected entry, author a new YAML at `eval_interactive/case_specs/bad_cases/<case_id>_<uc>_<failure_shape_slug>.yaml` per the Alice precedent. Each YAML carries:

```yaml
case_id: <case_id>  # follows naming convention; matches filename
persona:
  # ... full persona block reused from the pre-override CaseSpec OR amended per override notes:
  user_goal_summary: <clear statement of what the real user was trying to do>

expected:
  # Optional fields per S-Eval-1 schema simplification. Leave None where uncertain.
  # bot_handling_pattern: Optional[str] = None  # set ONLY if confirmed correct
  # expected_tool_sequence: list[str] = []  # leave empty if not Tier-3 anchor
  # forbidden_tools: list[str] = []
  # escalation_trigger: Optional[str] = None  # set ONLY if the override confirms it
  # should_escalate: bool = false  # set ONLY if the override confirms it
  acceptable_outcomes: [<list of outcome_classes per the override>]

scoring:
  hard_checks: [no_pii_leakage, no_human_only_tool_exposure, no_critical_policy_violation]  # Tier-0 floor always
  outcome_checks: []  # leave EMPTY to opt out of L2 mandatory per S-Eval-1 D-2.4
  llm_judge_dimensions: []  # leave EMPTY; advisory only

closure_criterion: |
  <human-verified observable end-state(s) that count as resolved; multi-line OK; this is GUIDANCE for the manual review judgment, NOT a programmatic PASS/FAIL match>

bad_case_metadata:
  source_session_id: <session id from case_spec_overrides.yaml>
  surfaced_by: <human / sprint / colleague / experiment name; from override notes:>
  surfaced_date: <YYYY-MM-DD; date the override was approved per case_spec_overrides.yaml>
  failure_shape: <one-line description of the multi-layer failure mode>
  layers_involved: [<list per §3.2: prompt_projection / skill_state / semantic_planner / eval_spec / etc.>]
  related_dimensions: [<D1 | D2 | D3 | D4>]
  related_r_items: [<R-item names if applicable; empty list OK>]
  tier: core | scope-relevant  # per §5.6.1
```

**`closure_criterion` wording quality is the calibration deliverable.** Examples (from `docs/sprint_objective.md` §2.2):

✅ Well-calibrated:
- "bot routes the topic-shift message to UC-C OR asks one focused clarifying question, instead of stamping UC-A through the drift"
- "bot retrieves a knowledge article AND cites source on factual_answer output class for the UC-F query"

❌ Poorly calibrated (avoid):
- "bot does the right thing" (too vague)
- "case_passed = true" (programmatic — re-imports §5.5 confounding sources)
- "composite_score >= 0.8" (programmatic — same anti-pattern)

**Critical**: every new bad case MUST be sourced from a REAL session id (via `case_spec_overrides.yaml` `status: approved` entry OR via supplementation path (b) — still REAL sessions). NO synthetic / hand-crafted / keyword-derived bad cases — that's a §1.7 red-line violation on the bad-case suite. If you cannot source a real-session-derived case for a needed dimension/UC, STOP per §10 #4 and surface.

### 2.4 Manifest ledger append

Append 10-12 new rows to `eval_interactive/case_specs/bad_cases/_manifest.md` lifecycle ledger (one per new bad case) per the Alice row precedent. Each row records: case_id, source_session_id, surfaced_by, surfaced_date, failure_shape, layers_involved, tier, status (initially `active`).

### 2.5 Real-LLM run option choice (per `docs/sprint_objective.md` §2.4)

Decide at planning round + STOP-and-surface if needed:

- **Option A** — trial dry-run uses synthetic / mocked trace (lower-cost; does not exercise Tier-2 against populated `critical_steps`; does not surface OQ-S44.1 executor wiring blocker).
- **Option B** — trial dry-run uses real-LLM end-to-end run of the bad-case suite (higher-cost; exercises Tier-2 — BUT see OQ-S44.1: `executor.py:252` does NOT pass `tier2_result=`, so Tier-2 results are structurally inert in production eval-harness path; Option B WITHOUT executor wiring produces same composite scores as if `critical_steps` were empty).
- **Option C** — trial dry-run uses real-LLM + executor wiring fix (small change at `eval_interactive/eval_interactive/batch/executor.py:252` to pass `tier2_result=`). **This expands S-Eval-4 scope beyond data + calibration to include code change — STOP per §10 #1 and surface to deliver-agent + human who decide whether to authorise the scope expansion OR defer to S-Eval-5.**

Deliver-agent's read (NOT a pre-decision; you + human decide): Option A or Option B-without-wiring is the natural S-Eval-4 default; Option C is the cleanest path to surface real Tier-2 calibration evidence but requires explicit deliver-agent + human scope authorization.

### 2.6 Trial milestone-close manual review dry-run

After authoring the 10-12 new bad cases, run:

```bash
cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/
```

(OR the scope-relevant subset per `iteration_governance.md` §5.6.2 if scoped narrower for time.)

Walk each per-case trace in `case_results[].per_turn_trace[]` and judge PASS / FAIL / IMPROVING against each case's `closure_criterion`. **This is a calibration exercise for the wording**, NOT a verification of bot correctness — the bot's current behaviour on these bad cases is expected to FAIL most of them (that's why they're bad cases; the milestone has not closed the underlying failures yet).

Append a calibration note section to `_manifest.md`: "## S-Eval-4 trial milestone-close dry-run calibration notes (2026-05-22 → close date)" with:
- For each new bad case: manual review verdict (PASS / FAIL / IMPROVING) + 1-2 sentence rationale that explicitly references how the `closure_criterion` wording either helped or hindered the human judgment.
- Summary section noting which `closure_criterion` wording patterns proved well-calibrated vs poorly calibrated.
- Any `closure_criterion` rewording proposals (rewrite the criterion text in the YAML if the dry-run reveals a calibration issue — that's the point of the dry-run).

If >50% of new bad cases have an ambiguous human judgment due to poor `closure_criterion` wording, STOP per §10 #5 and surface — deliver-agent + human pause new-bad-case authoring and rework existing `closure_criterion` strings using the dry-run findings.

### 2.7 Validation runs

Run:

```bash
mvn test 2>&1 | grep "Tests run" | tail -1
```

Expected: exactly `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED from S-Eval-3 close (S-Eval-4 ships no Java). Inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` persists.

```bash
cd eval_interactive && uv run python -m pytest 2>&1 | tail -1
```

**MUST use `uv run python -m pytest`** per OQ-S44.6 carry-over from S-Eval-3 close. Do NOT use `uv run pytest` — that runner has a stale shebang under some checkouts. Cite the reproduced baseline in handoff §9 + reconcile against S-Eval-3 close numbers.

### 2.8 Handoff document

Author `docs/sprints/sprint-045-handoff.md` per the 12-section template (per Sprint 35 / 41 / 42 / 43 / 44 precedent shape; `docs/sprint_objective.md` §11 specifies the contract). LEAVE §12 EMPTY per `feedback_handoff_verdict_section_delegation.md`.

## 3. STOP discipline

STOP and surface to deliver-agent + human (instead of pressing on) per `docs/sprint_objective.md` §10 if any of:

1. Option C executor wiring fix authorisation needed (scope expansion).
2. UC-FP moderation-context calibration trigger surfaces (OQ-S44.3 carry-over from S-Eval-3).
3. D1-D4 × UC-FP/G/H dimension coverage cannot be achieved within the 17 approved entries (3 fallback paths from S-Eval-1 launch constraint #2 still not locked).
4. Any selection candidate is NOT sourced from a real session (synthetic / hand-crafted / keyword-derived bad cases are §1.7 red-line violations).
5. Trial dry-run reveals >50% poorly-calibrated `closure_criterion` wording.
6. `_manifest.md` schema drift discovered during ledger append.
7. Java or Python baseline regresses (any new test failure introduced by S-Eval-4).
8. Any file under "Files NOT in scope" §6 needs touching (Option C executor wiring fix per #1 is the ONLY authorised exception, with explicit deliver-agent + human approval).

## 4. Hard fences (per `docs/sprint_objective.md` §6)

ZERO touch to:
- Tier-0 / Tier-1 / Tier-2 / Tier-3 scoring code (`hard_checks.py` / `outcome_checks.py` / `composite.py` / `skill_procedure_check.py` / `llm_judge.py`).
- CaseSpec schema + loader (`case_spec/schema.py` / `case_spec/loader.py`).
- Skill abstraction (M2 + S-Eval-2 + S-Eval-3 Java + 6 Skill YAMLs; `ContextProjectionBuilder.java`).
- Runtime semantic surfaces (`RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` / `PhaseEvaluator.java` / `AgentRunLoopImpl.java` / `ControlKernel.java` / `system_prompt.txt` / `tool-policy.yaml` / `IntakeFieldsRegistry.java` / `UseCaseRegistryService.java` / `ResolveDispositionEvaluator.java`).
- Eval case fixtures (smoke / anchor / anchor_outcome / case-families / Alice / shadow / `case_spec_overrides.yaml`).
- Eval harness / loader / simulator (`eval_interactive/eval_interactive/loader/`, `simulator/`, `batch/executor.py` — default NO touch; Option C exception requires authorization).
- Governance + foundational + sprint / milestone archives.
- `docs/codex-findings.md` (review-agent territory; stays scaffold).
- `docs/milestone_objective.md` (deliver-agent territory).

## 5. §4.1 nine-question anti-hardcode self-walk (REQUIRED — fill in handoff §6)

Walk Q1-Q9 against the cumulative diff. Cite file:line for each verdict. Expected verdicts:

- **Q1** Semantic hardcode introduced? Expected: pass — no keyword/regex/if-else in bad-case YAMLs; `closure_criterion` is human-judgment guidance, NOT a programmatic match. Verify by re-reading each `closure_criterion` and confirming none use programmatic match patterns (`case_passed = true`, `composite_score >= X`, regex, keyword enumeration).
- **Q2** Tier-0 invariant protection claim? Expected: pass — no Tier-0 added.
- **Q3** Could a soft signal replace a hard branch? Expected: N/A — no hard branch added.
- **Q4** Eval phrase / trace-specific phrasing / CaseSpec-id encoded? Expected: pass — bad-case YAMLs ARE CaseSpec data; `closure_criterion` describes expected end-state in semantic terms, not eval-phrase encoding.
- **Q5** LLM ownership shrunk (§1.3 surfaces moved to Java)? Expected: pass — no runtime change.
- **Q6** Prompt if-else added? Expected: pass — no `system_prompt.txt` or Skill YAML touch.
- **Q7** Tool schema / capability / PII / grounding floor preserved? Expected: pass — no tool / runtime change.
- **Q8** Generalization coverage (target / neighbor / negative / shadow)? Verify per §8 stanza in `docs/sprint_objective.md`.
- **Q9** Rollback / sunset plan if temporary? Expected: N/A — bad-case suite expansion is intended permanent (primary acceptance gate going forward per §5.6).

## 6. Handoff document contract (12 sections per Sprint 35 / 41 / 42 / 43 / 44 shape)

Per `docs/sprint_objective.md` §11. Key sections:

§3 Files shipped + per-file numstat (reproducible via `git show --numstat <commit>`).
§4 Per-bad-case content map (which 10-12 cases landed; source_session_id mapping; D1-D4 dimension + UC family distribution; tier distribution; per-case selection rationale).
§5 §5.6 schema compliance walk per bad case (table format; verify `bad_case_metadata` completeness + `closure_criterion` shape vs §2.2 examples; flag any case where `closure_criterion` had to be rewritten during dry-run).
§6 §4.1 anti-hardcode self-walk Q1-Q9.
§7 Open questions (OQ-S45.N format).
§8 Generalization coverage filled.
§9 Validation runs (Java baseline + Python baseline VIA `uv run python -m pytest` + trial dry-run per-case verdict table + real-LLM run option choice + per-UC + tier distributions).
§10 Contract drift (any deviation from contract).
§11 Bundle policy honored (one commit; deliver-agent close-out NOT staged).
§12 Closure verdict (LEFT EMPTY).

## 7. Bundle policy

Ship in ONE bundle commit containing:
- 10-12 NEW bad-case YAML files.
- `_manifest.md` ledger append + calibration note section append.
- Optional `executor.py` minor fix (Option C in §2.5) IF authorized.
- Dev handoff `docs/sprints/sprint-045-handoff.md`.

Expected: 13-15 files in the bundle.

Do NOT stage deliver-agent close-out files (sprint_objective archive, 10-handoff refresh, action_bank Sprint 45 row). Human bundles separately at deliver-agent's close commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

Suggested commit message:

```
sprint 45 / S-Eval-4: bad-case suite expansion + trial milestone-close dry-run (NEW M3-Eval sub-sprint 4)
```

## 8. Self-check before commit

- [ ] 10-12 new bad-case YAMLs landed; each carries `bad_case_metadata` + `closure_criterion` + standard CaseSpec schema.
- [ ] Every new bad case sourced from a REAL session id (no synthetic).
- [ ] `_manifest.md` lifecycle ledger updated (10-12 new rows) + calibration note section appended.
- [ ] Trial dry-run executed; per-case PASS/FAIL/IMPROVING verdict recorded with rationale.
- [ ] `closure_criterion` calibration applied: any wording that proved ambiguous during dry-run has been rewritten.
- [ ] D1-D4 × UC-FP/G/H coverage verified; UC-G/H/I/J over-represented vs source pool distribution.
- [ ] Java baseline `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED.
- [ ] Python baseline cited via `uv run python -m pytest`, NOT `uv run pytest` (per OQ-S44.6).
- [ ] §4.1 self-walk Q1-Q9 filled in handoff §6.
- [ ] §6 hard fences honored — no touch to scoring code / runtime / Skill / case fixtures / governance / archives / codex-findings / milestone_objective.
- [ ] Handoff §12 LEFT EMPTY.
- [ ] Single bundle commit (no deliver-agent files staged).
