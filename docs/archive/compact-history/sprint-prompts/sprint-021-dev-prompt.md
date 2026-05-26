# Sprint 21 dev-agent prompt

Paste the content below this line into a fresh Claude Code session on branch `design-v1-without-human-review`. Dev agent has zero conversation context. The branch is shared with deliver-agent-owned files in the working tree — see "Working tree at start" below.

---

## Loader stanza

Read in order, then return a Context Pack (per `agent_context_guide.md`) and wait for human confirmation before writing anything:

1. `AGENTS.md` → `docs/current/doc_governance.md` → `docs/current/agent_context_guide.md` (task type = "Eval, governance") → `docs/current/iteration_governance.md` §1, §3, §4, §5, §7.
2. `docs/sprint_objective.md` (Sprint 21 — authoritative scope).
3. `docs/sprints/sprint-018-handoff.md` §5.1 (L3 override pipeline Method note: schema v2, `source_session_id` lookup).
4. `docs/sprints/sprint-020-handoff.md` §3.6 (cs095 family — the cascade you must NOT touch) and §12 (Sprint 20 backlog state).
5. The 8 briefs at `docs/diagnostics/failure-briefs/`: `cs001-...`, `cs011-...` (systematic R-item evidence), `cs038-...`, `cs040-...`, `cs095-...`, `cs176-...`, `cs192-...`, `cs259-...` (systematic R-item evidence).
6. `eval_interactive/case_spec_overrides.yaml` — internalise the schema v2 shape; cite line numbers when you append.

## What this sprint is

Sprint 21 processes 7 L3 R-items from `docs/action_bank.md` §5.2 (Wave A5/A6 L3 review batch). For each R-item, classify the L3 disposition as **approved / rejected / deferred** and produce the appropriate artefact. This is paper-only work on the eval_spec surface — no runtime change, no prompt change, no FAQ corpus change.

## The 7 R-items (in execution order)

Each R-item resolves to **approved** (write/update an entry in `case_spec_overrides.yaml`), **rejected** (the layer is not eval_spec; propose a new R-item targeting the right layer in handoff §12), or **deferred** (name the missing evidence).

1. **`R-cs001-escalation-trigger-l3-review`** — cs001 brief. Question: does `escalation_trigger: clarification_budget_exhausted` reconcile with `intake fields (none)`?
2. **`R-cs038-l3-review-intake-efficiency`** — cs038 brief. Question: should intake completion at T2 be the correct `turn_efficiency` target?
3. **`R-cs040-l3-review-intake-completion-semantics`** — cs040 brief. Question: should `escalation_reason=intake_complete_for_uc_k` + `intake_fields_collected=0` be a hard outcome fail? If this reads like Tier-0 territory (runtime evidence contract), flag `human_review_required` and stop — do NOT open a new Tier-0; the `R-escalation-reason-runtime-evidence-contract-review` candidate is out of Sprint 21 scope.
4. **`R-cs095-uc-classification-l3-rereview`** (highest impact) — cs095 brief. Question: PRD/Eval = UC-D primary vs Wave A2.1 legacy override = UC-A primary — which is ground truth? **Cascade rule (non-negotiable) — see below.**
5. **`R-cs176-escalation-reason-l3-review`** — cs176 brief. Question: is `user_requested` the right expected reason for UC-E refund demand?
6. **`R-cs192-secondary-ucs-duplicate-uc-b`** (low impact) — cs192 brief. Question: deduplicate UC-B in `secondary_ucs`?
7. **`R-generator-get-customer-context-policy-mismatch`** (systematic — spans cs_001 / cs_011 / cs_259). Sources: cs001 + cs011 + cs259 briefs. Question: the generator includes `get_customer_context` in `expected_tool_sequence` for UC-C / UC-D / UC-F despite phase 2 §2.10 line 358 restricting it. Approved → 3 coordinated per-case overrides (or 1 shared template if the schema supports it — check for precedent). Rejected → layer is `prompt_projection` or `system_prompt.txt` (bot's policy understanding is the root); propose new R-item.

## L3 override pipeline (paraphrased from `sprint-018-handoff.md` §5.1)

- Lookup is by **`source_session_id`** (NOT `case_id`).
- Each entry may declare zero or more of three blocks: `classification` (primary_uc, secondary_ucs); `expected` (subset of `expected.*` fields); `persona`.
- Approved entries REQUIRE `status: approved`, `source:`, `reviewer:`, `date:`, `confidence:`, `rationale:`. Use today's date (2026-05-13); reviewer = "human semantic review".
- `supporting_turn_numbers` may be empty ONLY when `migrated_from_legacy: true`. For new entries this sprint, populate it with the turn indices that support the disposition.

## §1.7 forbidden-line guardrail (READ TWICE)

§1.7 forbids "widening eval spec to accept a genuine bot mistake". For every approved override, in the handoff §3 disposition write-up:

- Quote the brief's "What happened?" + "What should a good CS agent have done?" fields.
- Show that the bot's behaviour on that `source_session_id` was **correct** (or the CaseSpec / rubric was the artefact that needed adjustment), NOT the other way around.
- If you cannot show that — disposition MUST be `rejected` or `deferred`, not `approved`. The right fix is at a different layer (commonly `prompt_projection` or `semantic_planner`); track as a new R-item.

## cs_095 cascade rule (NON-NEGOTIABLE)

The Sprint 20 cs095 case family at `eval_interactive/case_specs/case_families/cs095_uc_classification_account_aware/` and the matching `case_specs_shadow/case_families/cs095_uc_classification_account_aware/` was authored assuming UC-D primary. If your cs_095 L3 review reverses the UC primary in either direction, the family expected fields would need a refresh — **that refresh is NOT in Sprint 21 scope.** Propose a new R-item `R-cs095-family-refresh-post-l3-reversal` in handoff §12. Do NOT edit any file under those two paths.

## Files in scope

- `eval_interactive/case_spec_overrides.yaml` — append / update entries for approved overrides.
- `docs/sprints/sprint-021-handoff.md` (NEW) — 12-section handoff (below).
- `docs/10-handoff.md` — refresh to lead with Sprint 21.
- (Conditional, allowed only with §1.7 self-check pass) `eval_interactive/eval_interactive/case_spec/llm_persona_reviewer.py` — ONLY if a disposition classifies as `judge_calibration` AND the rubric edit does not introduce keyword / regex / if-else / per-UC matrix.

## Files NOT in scope (hard fence)

- `eval_interactive/case_specs/case_families/**` and `eval_interactive/case_specs_shadow/case_families/**` (cascade rule)
- `eval_interactive/case_specs/{smoke,anchor,promotion,exploration}/*` (overrides go in `case_spec_overrides.yaml`, NOT inline — inline edits are the §1.7 forbidden surface)
- `server/**`, `data/**`, `FAQ-knowledge_include_help_url.csv`, `eval_interactive/personas*.yaml`, `eval_interactive/data/**`
- `docs/sprints/**` other than the new Sprint 21 handoff; `docs/archive/**`; `docs/current/**`; `docs/foundational/**`
- `docs/runtime_freeze_and_risk_policy.md`, `AGENTS.md`, `CLAUDE.md`
- `docs/sprint_objective.md`, `docs/codex-findings.md` (deliver-agent owned)

## Working tree at start (commit-at-end heads-up)

Expect deliver-agent-owned files uncommitted at session start: `docs/sprint_objective.md` (Sprint 21 objective), `compact/sprint-021-dev-prompt.md` (this file), `compact/sprint-021-review-prompt.md`, `compact/sprint-deliver-orchestrator.md`. **Do not stage these yourself; do not be surprised when the human bundles them at commit time.** They are deliver-agent operational surface and roll forward at close. Prefer adding only the files you authored (the YAML, the new handoff, the refreshed `10-handoff.md`) rather than `git add -A`.

## Handoff doc contract (`docs/sprints/sprint-021-handoff.md`)

12 sections, adapted from Sprint 19/20 precedent:

1. **Context Pack** — what you read, source-of-truth choices, doc-status warnings, implementation status, top risks.
2. **Sprint-objective recap** — restate the 7 R-items and disposition options.
3. **Per-R-item disposition** — §3.1–§3.7, one per R-item. For each: brief citation, evidence walk, classification (approved/rejected/deferred), §1.7 self-check evidence (for approved), artefact (YAML diff for approved; new R-item proposal for rejected; reason for deferred).
4. **`case_spec_overrides.yaml` deltas** — table with line numbers.
5. **Cascade rule observance** — confirm no edit under `case_families/**`; list paths NOT touched as evidence.
6. **Files changed** — diff summary.
7. **Anti-hardcode self-walk (§4.1 9 questions)** — one-line answer per question, per approved override.
8. **Layer-classification self-walk (§3 questions)** — for each rejected R-item, defend the layer reclassification via §3.2.
9. **Generalization-coverage statement** — per approved override, name the Sprint 20 case family and assert consistency.
10. **Sprint-objective-met check** — walk the success-metrics list.
11. **Open questions for human**.
12. **Action-bank deltas + next recommended action** — rows to update in `docs/action_bank.md` §5.2; new R-items proposed (always include `R-cs095-family-refresh-post-l3-reversal` if the cs_095 disposition warrants); recommended next sprint.

## Stop conditions

Stop and surface to the human (do not commit) if:

- A disposition tempts you to widen a rubric / override to accept the bot's actual output when the bot was genuinely wrong → §1.7 forbidden line.
- A disposition tempts you to edit a Sprint 20-authored case-family file → cascade rule.
- A disposition surfaces a new Tier-0 candidate → flag `human_review_required`; do NOT open a new Tier-0.
- The `case_spec_overrides.yaml` schema disagrees materially with `sprint-018-handoff.md` §5.1.
- The briefs are not at `docs/diagnostics/failure-briefs/cs*.md`.

Do not run smoke / eval beyond a single-case sanity check that confirms a new override entry is parsed and applied. Commit on `design-v1-without-human-review` (one commit, two if cleaner). Do not push. Deliver agent picks up at sprint close.
