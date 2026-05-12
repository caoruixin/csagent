# Sprint 18 Handoff — Human-led Failure Portfolio (G1)

Date: 2026-05-13
Branch: `design-v1-without-human-review`
Sprint class: docs-only governance (no runtime, no test, no prompt,
no CaseSpec, no FAQ corpus, no judge, no eval harness change).

## 0. Context Pack

**Relevant docs**

- `docs/sprint_objective.md` (current-runtime; status: current) — the
  Sprint 18 G1 objective; source of the five G1 deliverables.
- `docs/current/iteration_governance.md` (current-runtime; status:
  current) — Sprint 17 G0 bundle; §2 Failure Brief Template is the
  template the 10 briefs follow; §3 Fix Layer Classification is the
  decision tree the briefs use to name the responsible layer; §3.2 Q6
  is the eval_spec gate the Method note's CaseSpec L3 check sits
  before.
- `docs/current/doc_governance.md` (durable-connective; status:
  current) — tier model, status enums; G1 briefs use no front matter
  (per §2 template, briefs are diagnostic write-ups whose source-of-
  truth is the cited trace + CaseSpec / override / phase 2 path).
- `docs/current/agent_context_guide.md` (durable-connective; status:
  current) — per-task reading lists; the Method note's manual-probe
  ground-truth chain references phase 2 UC policy and trace
  `phase_plan.system_instruction` per its eval / governance and
  privacy / PII / trace reading lists.
- `docs/proposals/interactive_case_spec_generation_plan.md` (proposal;
  status: partial — Wave A5 / A6 / A6.6 landed) — the 3-layer
  CaseSpec pipeline (L1 rule extraction / L2 LLM persona / L3
  approved overrides) the Method note's L3 check uses.
- `docs/foundational/phase2_domain_realization_spec.md` (foundational;
  status: current) — UC policy + tool-policy lines; §2.10 line 358
  restricts `get_customer_context` to UC-A / UC-FP / UC-K (cited
  systematically across cs_001 / cs_011 / cs_259 briefs); §UC-FP-01
  escalation_conditions cited in cs_015; §UC-K intake-complete
  policy cited in cs_040; §UC-E escalation conditions cited in
  cs_176.
- `docs/foundational/phase5_evaluation_design.md` (foundational;
  status: current) — line 951 `no_human_only_tool_exposure` Wave
  B1.2 check; relevant to several briefs that touch escalation
  reason validation.
- `eval_interactive/case_spec_overrides.yaml` (current-runtime;
  status: current) — Wave A5 / A6 approved overrides; the L3
  ground-truth source for cs_015, cs_011, cs_095. Schema v2; keyed
  by `source_session_id`.
- `eval_interactive/results/20260505-235231/results.json` and
  `.../20260510-134558/results.json` — the two smoke runs the 9
  smoke briefs trace through.
- `docs/diagnostics/codex-findings.md` (pre-Sprint-17 stale path) —
  staged for deletion in this commit; supersession to
  `docs/codex-findings.md` was already landed by Sprint 17 close
  (`ac778bc`).

**Relevant code paths** — none read with intent to edit. G1 is
docs-only; the briefs cite code paths only to point at the modules
that own the layer the brief names (e.g. `RuntimeIntentClassifier`,
`DriftDetector`, `PhaseEvaluator`). No edit was made to any code
path.

**Doc status warnings**

- `docs/diagnostics/codex-findings.md` was a Sprint 16 review file at
  the wrong path; superseded by `docs/codex-findings.md` (Sprint 17
  G0 review) committed in `ac778bc`. The deletion of the stale path
  is staged in this commit as housekeeping; no review history is
  lost (`docs/sprints/sprint-016-codex-review.md` retains the Sprint
  16 review archive).
- The 10 briefs reference CaseSpec / phase 2 / runtime artifacts at
  current paths. Brief authors verified each cited path against the
  working tree at brief filing time; subsequent fold-backs or moves
  may shift line numbers, in which case the brief's section reference
  is the load-bearing claim (e.g. "phase 2 §2.10 tool-policy
  restriction"), not the literal line number.

**Source-of-truth decision** — for G1 deliverables, the authoritative
artifacts are: the 10 brief files (for failure-shape evidence and
layer hypothesis), `docs/action_bank.md` §5.2 (for the R-item
ledger), and this handoff (for the Method note, the
layer-classification self-walk, and the conditional-broadening
record). The `docs/sprint_objective.md` is normative for scope.

**Implementation status** — `implemented`. G1 deliverables are
file-on-disk artefacts; there is no runtime behaviour to verify.

**Risks before drafting**

1. Brief drift from the §2 template — mitigated by mechanically
   verifying that each brief carries all 6 required headers (`What
   happened?`, `What should a good CS agent have done?`, `Why does
   this matter?`, `Is this a one-off or a pattern?`, `Which layer is
   likely responsible?`, `What should NOT be done?`).
2. CaseSpec-is-authority anchoring on a CaseSpec that has no L3
   override — mitigated by the Method note's L3 check, which forces
   every brief to declare its L3 status before treating the CaseSpec
   `expected.*` fields as authoritative.
3. Layer over-attribution to `java_guard` (the historical default
   when something feels broken) — mitigated by the §3.2 first-match-
   wins walk in every brief and an explicit §3.3 "Why no Java guard
   by default" check on the brief's named layer.
4. R-item proliferation: every brief surfacing a unique per-case
   R-item leads to a noisy backlog. Mitigated by the conditional-
   broadening rule (when 2 instances confirm a pattern, broaden the
   per-case R-item to a systematic R-item and retroactively rename
   in earlier briefs); see §6 of this handoff.
5. Confusing G1 scope with G3+ remediation: it is tempting to slip a
   fix proposal into the brief's What should NOT be done? field.
   Mitigated by the §1.5 Iteration rule cross-reference in every
   brief and by keeping the brief's posture to "name the layer, name
   the wrong fix, do not specify the right fix".

## 1. Exact actions implemented

- **G1.1** — filed 10 Failure Briefs under
  `docs/diagnostics/failure-briefs/`. Filename convention α:
  `<case_id>-<uc>-<slug>.md` for smoke briefs;
  `manual-probe-<date>-<slug>.md` for the manual probe. Each brief
  carries header metadata (source case, source_session_id, CaseSpec
  path, approved L3 override status, runs, filed date), the 6
  required `iteration_governance.md` §2 fields, and (where applicable)
  a Ground-truth chain preamble and / or a Related observation tail.
- **G1.2** — appended new §5.2 `G1 surfaced backlog` sub-section to
  `docs/action_bank.md` with 18 R-items (1 Tier-0 candidate, 5
  systematic, 9 per-case L3 / governance, 1 G2 input, 1 new infra, 1
  external/regression discovery) and 2 open observations (not opened
  on n=1 evidence). R-item ids are stable kebab-case; sources cite
  the brief file(s) that surface each R-item.
- **G1.3** — updated `docs/action_bank.md` §5.1 G1 row from
  `deferred — next governance sprint` to `done — 10 briefs filed (9
  smoke + 1 manual-probe); see docs/diagnostics/failure-briefs/`.
  G2 row unchanged.
- **G1.4** — Method note documented in §5 of this handoff (CaseSpec
  L3 override check via `eval_interactive/case_spec_overrides.yaml`
  keyed by `source_session_id`; manual-probe ground-truth derivation
  chain from phase 2 policy + `phase_plan.system_instruction`).
- **G1.5** — archived this sprint's objective verbatim as
  `docs/sprints/sprint-018-g1-failure-portfolio-objective.md` and
  refreshed `docs/10-handoff.md` to lead with Sprint 18 G1 (Sprint
  17 G0 demoted to "Preceding sprint" per the Sprint 17 close
  precedent).

## 2. Files changed

- `docs/sprint_objective.md` — replaced Sprint 17 G0 content with
  Sprint 18 G1 objective. Prospective tense ("This sprint will / does
  X") per the human's Q2 packaging decision (option (b)).
- `docs/sprints/sprint-018-g1-failure-portfolio-objective.md` — NEW,
  verbatim archive copy of `docs/sprint_objective.md`.
- `docs/sprints/sprint-018-handoff.md` — NEW (this file).
- `docs/diagnostics/failure-briefs/` — NEW directory with 10 brief
  files (953 lines total; sizes range 69–125 lines per brief).
- `docs/action_bank.md` — new §5.2 sub-section (18 R-items + 2 open
  observations) and §5.1 G1 row marked `done`.
- `docs/10-handoff.md` — current-phase paragraph rewritten to lead
  with Sprint 18 G1; Sprint 17 G0 demoted to preceding-sprint
  paragraph (the Sprint 17 detail paragraphs below the lead are
  preserved).
- `docs/diagnostics/codex-findings.md` — DELETED (stale pre-Sprint-17
  path; superseded by `docs/codex-findings.md` in `ac778bc`).

No file under `server/`, `ui/`, `eval/`, `eval_interactive/`,
`data/`, `scripts/`, `docs/foundational/`, `docs/current/`, or
`docs/sprints/sprint-001..017-*` is modified.

## 3. Tests run

No code was touched. `mvn -pl server test` and
`pytest eval_interactive/tests/` were not re-run. No new test was
added.

## 4. Brief catalogue

The 10 briefs sit at `docs/diagnostics/failure-briefs/`:

| # | brief | source case / session | primary layer | secondary layer | cluster |
|---|-------|----------------------|---------------|-----------------|---------|
| 1 | `cs015-uc-fp-mis-route-and-premature-escalate.md` | `cs_interactive_015` / `570Q5000008WmXxIAK` | `prompt_projection` | `semantic_planner` | A |
| 2 | `cs001-uc-c-template-escalate-on-faq-miss.md` | `cs_interactive_001` / `570Q5000008kr6LIAQ` | `semantic_planner` | `prompt_projection` | B-1 |
| 3 | `cs011-uc-d-detailed-description-ignored-on-faq-miss.md` | `cs_interactive_011` / Sprint 4 §E1 override | `semantic_planner` | `prompt_projection` | B-1 |
| 4 | `cs038-uc-j-intake-redundancy-and-jargon-framing.md` | `cs_interactive_038` | `semantic_planner` | `judge_calibration` | B-2 |
| 5 | `cs040-uc-k-disengaged-jargon-intake-false-complete.md` | `cs_interactive_040` | `semantic_planner` | `prompt_projection` | B-3 |
| 6 | `cs095-uc-classification-and-account-aware-path-skipped.md` | `cs_interactive_095` / Wave A2.1 legacy override | `prompt_projection` | `semantic_planner` + `eval_spec` | A |
| 7 | `cs176-uc-e-wrong-escalation-reason-family.md` | `cs_interactive_176` | `semantic_planner` | — | A |
| 8 | `cs192-uc-b-mechanical-escalate-on-resolvable-giveaway-question.md` | `cs_interactive_192` | `semantic_planner` | `prompt_projection` | A |
| 9 | `cs259-uc-f-sprint7-i0-violation-on-payment-question.md` | `cs_interactive_259` | `semantic_planner` | `prompt_projection` | A |
| 10 | `manual-probe-2026-05-13-ad-visibility-multi-layer-failure.md` | manual probe / session `4a2f3680-02a8-4d13-8f0b-f99d7c249b57` | multi-layer (infra + `semantic_planner` + `prompt_projection`) | — | — |

Cluster taxonomy (used for grouping; not a contract):

- **Cluster A** — persistent failures (both smoke runs FAIL): cs_015,
  cs_095, cs_176, cs_192, cs_259. cs_192 is the intersection of A
  and B-style mechanical surface but is filed as Cluster A per the
  outcome.
- **Cluster B** — PASS by outcome but mechanical surface (L3
  relevance / tone ≤ 2.0): cs_001 (B-1 disengaged-template canonical),
  cs_011 (B-1 user-detail-ignored extreme), cs_038 (B-2
  engaged-but-mechanical), cs_040 (B-3 disengaged + jargon hybrid).
  cs_002 / cs_014 / cs_066 are not separately briefed; they are
  neighbor candidates for G2 case-family construction.
- **Cluster C** — regression-only (only fail in 2026-05-10 run):
  cs_002, cs_011, cs_014, cs_038, cs_040, cs_066. Not separately
  briefed; surfaced via `R-smoke-regression-investigation`.

## 5. Method note for G1 brief authoring

The 10 briefs depend on two ground-truth derivation techniques. They
are documented here (G1.4) so the next governance sprint inherits the
practice without re-deriving it.

### 5.1 CaseSpec L3 override check

Generated CaseSpecs at `eval_interactive/case_specs/smoke/` follow
the 3-layer pipeline documented in
`docs/proposals/interactive_case_spec_generation_plan.md`:

- **L1** — deterministic rule extraction from the source transcript.
  Produces the initial `classification.*`, `expected.*`, persona, and
  pattern fields. No human review.
- **L2** — bounded LLM persona reviewer (DeepSeek v4 Pro, persona
  fields only). Refines persona fields; does not touch
  `classification` or `expected`.
- **L3** — human-approved overrides at
  `eval_interactive/case_spec_overrides.yaml` (schema v2), keyed by
  `source_session_id` (NOT `case_id`). Overrides may carry
  `classification` / `expected` / `persona` blocks. Approved overrides
  with `migrated_from_legacy: true` are allowed empty
  `supporting_turn_numbers`.

Before filing a Failure Brief on a generated CaseSpec:

1. Grep `eval_interactive/case_spec_overrides.yaml` for the
   `source_session_id` from the trace / CaseSpec.
2. **If an approved override exists** — the override is the
   authoritative ground truth. Anchor the brief's expected behaviour
   on the override's `classification.*` / `expected.*` blocks. Cite
   the override file + line range in the Ground-truth chain preamble.
   `eval_spec` is **ruled out** as a candidate layer for the outcome
   failure (the human-reviewed override is the contract; widening the
   override to accept the bot's actual output would violate
   `iteration_governance.md` §5.4).
3. **If no approved override exists** — the CaseSpec is L1 + L2 only.
   The `expected.*` and `classification.*` fields are rule-extracted
   defaults, not human-reviewed. The brief should flag whether the
   CaseSpec itself needs L3 triage as a separate `eval_spec` candidate
   per `iteration_governance.md` §3.2 Q6. Use a per-case R-item
   (`R-csNNN-...-l3-review` or similar) to track the L3 ask.

Briefs that cite a Wave A5 / A6 override as authority in G1:

- cs_015 — Wave A6 semantic re-review, 2026-04-30 (override lines
  53–98).
- cs_011 — Sprint 4 §E1 override on escalation_trigger.
- cs_095 — Wave A2.1 legacy classification-only override (note:
  human review during cs_095 brief authoring identified a substantive
  classification mismatch between the override and the PRD/Eval
  expected UC; the L3 re-review R-item is
  `R-cs095-uc-classification-l3-rereview`).

Briefs that have no L3 override and flagged the CaseSpec for L3
triage: cs_001 (escalation_trigger inconsistency), cs_038 (intake
turn_efficiency), cs_040 (intake_complete + 0 fields), cs_176
(escalation_reason family), cs_192 (secondary_ucs duplication),
cs_259 (no specific L3 flag — outcome is correct, surface is
mechanical violation of Sprint 7 §I0).

### 5.2 Manual-probe ground-truth derivation chain

Manual-probe traces have no CaseSpec — no L1 extraction, no L2 review,
no L3 override exists. Ground truth must come from authoritative
authoring sources that are checked into the repo / observable in the
trace.

Two sources are authoritative for derived expected behaviour:

1. **Phase 2 UC policy** (`docs/foundational/phase2_domain_realization_spec.md`).
   For the manual probe's UC-A failure: phase 2 §UC-A states
   `allow_bot_resolution=true`, faq-resolvable. The bot's
   `request_handover(faq_miss_threshold_exceeded)` on a UC-A trace
   with viable FAQ hits is checkable against this policy.
2. **The bot's own `phase_plan.system_instruction`** observable in
   the trace at the failing turn. For the manual probe, the RESOLVE
   phase `system_instruction` (turn 2) includes a MUST clause: "After
   search_knowledge returns a viable hit, you MUST call resolve_article
   for the top hit before answering the customer; cite the source_id
   in your user_message." The bot's actual tool sequence
   (`search_knowledge × 3`, no `resolve_article`) is checkable
   against this MUST.

The brief's Ground-truth chain preamble must enumerate both sources
and quote the load-bearing clauses, so a reader who has never seen
the trace can verify the failure against authoritative authoring
intent rather than against the brief author's recollection.

This technique extends to future production-capture traces where no
CaseSpec exists. The technique deliberately does **not** invent a
new ground-truth source: phase 2 + `system_instruction` are both
human-authored for the agent to consume; both pre-exist the trace;
neither requires the brief author to make a judgment call.

### 5.3 Why this Method note lives here, not in iteration_governance.md §2

The Method note documents G1-specific brief authoring practice. The
human's Q3 packaging decision (option (c), 2026-05-13) chose to
defer the note to the handoff rather than appending it to
`iteration_governance.md` §2 mid-sprint. Rationale:

- The note is observable in two briefs already (cs_015 cites the
  override; manual-probe cites the system_instruction), so the
  practice is documented by example.
- The §2 fold-back cadence is "every 3–5 sprints" (per the
  governance doc front matter); folding the note into §2 should
  happen on that cadence, not as a G1 side effect.
- If G2 / G3+ brief authoring re-uses the technique without
  modification, the §2 fold-back is justified at the next cadence
  pass. If the technique drifts, the handoff record protects against
  premature constitution-level commitment.

## 6. Layer-classification self-walk (§3 questions)

Each brief names exactly one primary layer (and optional secondary
candidates). The §3.2 first-match-wins walk produces those layer
choices as follows:

- **cs_015** — Q1 no infra crash, Q2 no current Tier-0 invariant on
  UC routing, Q3 projection at session start lacks a strong-prior
  signal for UC-FP empty-`ad_id` deletion-question shape →
  `prompt_projection`. Q4 not applicable (single-turn intake).
  Secondary `semantic_planner` if projection turns out adequate.
  Q6 ruled out because the Wave A6 override is the ground truth.
- **cs_001** — Q1 no infra crash, Q2 no Tier-0, Q3 projection seems
  adequate (UC-C anchored; FAQ search ran), Q4 single-turn (the
  template-escalate happens on turn 0), Q5 the LLM chose a fixed
  template instead of an acknowledged escalation message →
  `semantic_planner`. Secondary `prompt_projection` if the prompt's
  template fallback path is suppressing the acknowledgment.
- **cs_011** — Q1 no infra crash, Q2 no Tier-0, Q3 projection has
  detailed `form_context.description` (user's six-line problem
  statement), Q5 the LLM ignored the description and produced the
  same generic template as cs_001 → `semantic_planner`. Secondary
  `prompt_projection` if the description is not surfaced to the
  user_message-generation prompt.
- **cs_038** — Q1 no infra crash, Q2 no Tier-0, Q3 projection has
  `form_context.first_name="Paul"`, Q5 the LLM asked for first_name
  again on turn 0 despite Paul being in context →
  `semantic_planner`. Secondary `judge_calibration` because the L3
  judge over-reaches by criticizing the form-supplied first_name as
  "without confirmation" (this is the basis of
  `R-l3-judge-form-context-trust-rubric`).
- **cs_040** — Q1 no infra, Q2 no Tier-0, Q3 projection has
  `form_context` Mo + description (jargon-heavy), Q5 the LLM
  classified intake as complete despite collecting zero intake fields
  and stamped `escalation_reason=intake_complete_for_uc_k` →
  `semantic_planner`. Secondary `prompt_projection` if the intake
  policy text in the prompt is not specific enough about
  "complete = all required fields collected". This is the brief that
  surfaced the first Tier-0 candidate
  (`R-intake-complete-runtime-contract-review`, later broadened to
  `R-escalation-reason-runtime-evidence-contract-review`).
- **cs_095** — Q1 no infra, Q2 no Tier-0, Q3 the projection
  classifies as UC-A (per the Wave A2.1 legacy override) but the
  human review of the trace + PRD identifies UC-D as the correct UC
  (account / email recovery), Q5 the LLM operated correctly within
  UC-A's playbook but answered the wrong UC's question →
  `prompt_projection` (primary; the UC classification itself is the
  upstream projection error). Secondary `semantic_planner` if the
  projection includes both candidates and the LLM picked the wrong
  one. Secondary `eval_spec` because the override's UC-A
  classification is itself under L3 re-review
  (`R-cs095-uc-classification-l3-rereview`).
- **cs_176** — Q1 no infra, Q2 no Tier-0, Q3 projection adequate, Q5
  the LLM stamped `escalation_reason=faq_miss_threshold_exceeded` on
  a UC-E refund-demand trace whose expected reason is
  `user_requested` per `iteration_governance.md` §3.2 escalation
  rubric → `semantic_planner`.
- **cs_192** — Q1 no infra, Q2 no Tier-0, Q3 projection seems
  adequate (UC-B anchored), Q5 the LLM template-escalated on turn 0
  for a giveaway / "free items" question that has resolvable FAQ
  content → `semantic_planner`. Secondary `prompt_projection` if the
  faqMissCount threshold is checked before the FAQ search returned.
- **cs_259** — Q1 no infra, Q2 a candidate Tier-0 lives here: the
  bot violated the Sprint 7 §I0 weak-candidate cue (an explicit
  phase-plan `system_instruction` directive). However, Sprint 7 §I0
  is currently held in prompt, not in runtime Java, so the violation
  is `semantic_planner`. If the human elects to elevate Sprint 7 §I0
  to a current Tier-0 invariant in `docs/runtime_freeze_and_risk_policy.md`,
  this re-classifies to `java_guard`; until then, `semantic_planner`
  per Q5.
- **manual-probe** — Q1 yes: the runtime orchestrator executed
  `search_knowledge` three times for one LLM request (`infra` layer
  per §3.2 Q1 — this is the first-matching question and produces
  `R-runtime-orchestrator-tool-call-deduplication`). The brief
  separately walks Q2–Q7 for the other failures in the same trace
  (`semantic_planner` for the `faq_miss_threshold_exceeded`
  self-stamp despite `faq_miss=false`; `semantic_planner` for the
  RESOLVE MUST-call-resolve_article violation; `prompt_projection`
  for whether the bot saw the system_instruction MUST clause
  adequately). The brief is multi-layer by design.

§3.3 "Why no Java guard by default" check passed on every brief: no
brief named `java_guard` as primary. cs_259's escalation conditional
to `java_guard` is contingent on a future Tier-0 promotion that has
not happened.

## 7. Anti-hardcode self-walk (§4.1 prompt)

The Sprint 17 G0 §4.1 Anti-Hardcode Review Prompt walks 9 questions
on a per-PR basis. G1 has no PR-level semantic change (the briefs
are docs); the prompt's scope exemption applies: pure docs-only PRs
are exempt and return `approve` with the exemption named.

Verdict on the G1 packaging commit: **approve (exemption: docs-only
governance PR; no semantic change introduced).**

For thoroughness, the 9 questions answered as if the briefs were a
semantic-change PR:

1. Keyword/regex/if-else/enum/per-UC matrix added for a semantic
   decision? — No. Briefs describe failure shapes; no runtime code
   is added.
2. Tier-0 invariant justification? — Not required (Q1 = no).
3. Soft signal projection alternative? — Not required (Q1 = no).
4. Visible-eval case text encoded into runtime / prompt / judge? —
   No. Briefs cite case ids in their own headers but no `cs_NNN`
   string is written into runtime / prompt / judge.
5. Semantic ownership moved from LLM to Java? — No.
6. Prompt grew an if-else block? — No (no prompt edit).
7. Tool schema, capability / permission, PII / safety floor,
   grounding floor preserved? — Yes (no change).
8. Generalization eval coverage (target / neighbor / negative /
   shadow)? — Not required for docs-only governance (G2 will
   produce these).
9. Temporary change → sunset plan? — Not applicable (no change).

The 18 R-items themselves are tracked in `docs/action_bank.md` §5.2.
Each R-item is a future scope candidate; none is a runtime change
this sprint introduced.

## 8. R-item summary (full table in `docs/action_bank.md` §5.2)

### 8.1 Tier-0 candidate

- `R-escalation-reason-runtime-evidence-contract-review` — 3
  instances (cs_040 + cs_176 + manual-probe). Solidly systematic.
  Should the runtime require that escalation_reasons claiming session
  events (e.g. `intake_complete_for_uc_*`, `faq_miss_threshold_exceeded`,
  `clarification_budget_exhausted`) carry corresponding event
  evidence? Scope: evidence-claiming subset only (not `user_requested` /
  `user_distress` which have separate contracts).

### 8.2 Systematic (≥2 instances confirmed)

- `R-generator-get-customer-context-policy-mismatch` — cs_001 (UC-C)
  + cs_011 (UC-D) + cs_259 (UC-F). CaseSpec generator includes
  `get_customer_context` in `expected_tool_sequence` regardless of
  phase 2 §2.10 line 358 UC restriction.
- `R-l3-judge-form-context-trust-rubric` — cs_038 (Paul) + cs_040
  (Mo) + cs_192 (Rita). L3 judge over-reaches by criticizing
  form-supplied `first_name` as "without confirmation".
- `R-corpus-coverage-audit-per-uc` — cs_095 + cs_192 + cs_259. Per-UC
  FAQ corpus coverage audit. No generator-synthesized articles (per
  §8.2 anti-hardcode rule in `compact/sprint-deliver-orchestrator.md`).
- `R-faqMissCount-threshold-and-timing-review` — cs_095 + cs_192 +
  cs_259. Two-part: (a) `>= 2` threshold given auto-search burns one;
  (b) threshold-check timing vs form-description fallback.
- `R-duplicated-greeting-projection-fix` — cs_095 ("Hi Trish! Hi
  Trish") + cs_176 ("Hi Gary! Hi Gary"). Single
  projection / template-rendering bug rendering greeting twice.

### 8.3 Per-case L3 / governance

- `R-uc-b-customer-context-policy-review` (cs_015 Related observation)
- `R-cs001-escalation-trigger-l3-review` (cs_001)
- `R-cs038-l3-review-intake-efficiency` (cs_038)
- `R-cs040-l3-review-intake-completion-semantics` (cs_040)
- `R-persona-goal-summary-scope-clarity` (cs_040, **conditional** —
  only open if G2 case-family work shows the same scope-broadening
  pattern on other personas)
- `R-cs095-uc-classification-l3-rereview` (cs_095; **most impactful
  cs_095 follow-up**)
- `R-l1-source-citation-quality-rubric` (cs_095)
- `R-cs176-escalation-reason-l3-review` (cs_176)
- `R-cs192-secondary-ucs-duplicate-uc-b` (cs_192; low priority)

### 8.4 G2 input / future-input

- `R-g2-multi-turn-followup-case-family-design` (cs_095) — G2
  case-family construction should intentionally include multi-turn
  followup cases on FAQ-resolve UCs to exercise the `skill_state`
  surface.

### 8.5 New infra (from manual-probe)

- `R-runtime-orchestrator-tool-call-deduplication` (manual-probe) —
  `infra` layer per §3.2 Q1. 1 LLM request → 3 identical
  `search_knowledge` executions. Investigate root cause among 3
  hypotheses; document orchestrator's intended de-dup / idempotency
  contract; add regression test.

### 8.6 External / regression discovery

- `R-smoke-regression-investigation` — 2026-05-05 → 2026-05-10 smoke
  drop (64% → 21%). 6 cases regressed (cs_002, cs_011, cs_014,
  cs_038, cs_040, cs_066). Sprints 14 / 14.1 / 15 / 16 all declared
  no-runtime-semantic-change; trace evidence may contradict. **P1,
  must resolve before G2** otherwise G2 case-family construction has
  no clean baseline.

### 8.7 Open observations (NOT opened as R-items)

- **Bot ignores explicit phase-plan directives** — observed in cs_259
  (Sprint 7 §I0 violation) and manual-probe (RESOLVE MUST-
  call-resolve_article violation). n=2 opportunistic; user (2026-05-13)
  said controlled multi-shape testing needed across UC-B / UC-C /
  UC-D / UC-F empty-form shapes before opening
  `R-prompt-phase-plan-directive-followship`.
- **ad_id form-vs-listing data consistency** — manual-probe (form
  ad-1003 vs listing AD-1001). n=1; needs production data to know if
  this is a common shape.

### 8.8 Conditional-broadening record

Three R-items were broadened during G1 brief authoring after a 2nd
instance confirmed the pattern. The broadening is retroactive — the
earlier brief was updated to cite the broadened R-item.

| Earlier per-case id | Broadened id | Instances |
|---------------------|--------------|-----------|
| `R-cs001-uc-c-customer-context-policy-conflict` | `R-generator-get-customer-context-policy-mismatch` | cs_001 + cs_011 + cs_259 |
| `R-intake-complete-runtime-contract-review` | `R-escalation-reason-runtime-evidence-contract-review` | cs_040 + cs_176 + manual-probe |
| `R-corpus-uc-d-account-faq-gap` | `R-corpus-coverage-audit-per-uc` | cs_095 + cs_192 + cs_259 |

The rule: 2 instances → broaden to systematic and rename retroactively.
n=1 → keep per-case. The rule is recorded in
`compact/sprint-deliver-orchestrator.md` §4.5 and is reinforced here
for the next G1-style sprint.

## 9. Was Sprint 18 G1 objective met?

Per the success metrics in `docs/sprint_objective.md`:

- ✓ 10 brief files at `docs/diagnostics/failure-briefs/`, each with
  the 6 required §2 fields, each naming exactly one primary layer
  from §3.1.
- ✓ Each brief with a Wave A5 / A6 override (cs_015, cs_011, cs_095)
  cites the override and rules out `eval_spec` for the outcome
  failure (cs_095 has a secondary `eval_spec` candidate via the L3
  re-review R-item, but `eval_spec` is not the primary outcome-failure
  layer — the failure shape is upstream UC mis-classification).
- ✓ Each brief without an L3 override flags the CaseSpec for L3
  triage (cs_001 / cs_038 / cs_040 / cs_176 / cs_192) or explicitly
  documents why no L3 ask is opened (cs_259).
- ✓ The manual-probe brief derives ground truth from phase 2 UC-A
  policy + the RESOLVE phase `system_instruction` MUST clause.
- ✓ `docs/action_bank.md` §5.2 lists all 18 R-items + 2 open
  observations.
- ✓ `docs/action_bank.md` §5.1 G1 row marked `done`; G2 row
  `deferred`.
- ✓ This handoff includes the Method note (§5) and the
  layer-classification self-walk (§6).
- ✓ No code touched; no test re-run.
- ✓ No file under `server/`, `ui/`, `eval/`, `eval_interactive/`,
  `data/`, `scripts/`, `docs/foundational/`, `docs/current/`, or
  `docs/sprints/sprint-001..017-*` was modified.

## 10. Open questions for human

1. **R-smoke-regression-investigation prioritization.** The 64% → 21%
   smoke drop sits between G1 close and G2 start. Recommended
   sequencing: P1 regression investigation as the next sprint (small,
   focused), then G2 case-family construction with a clean baseline.
   Alternative: parallel-track a small probe sprint on phase-plan-
   directive-followship (UC-B / UC-C / UC-D / UC-F empty-form shapes)
   to inform whether `R-prompt-phase-plan-directive-followship` opens
   as an R-item.
2. **Wave A5 / A6 L3 review batch sizing.** Six per-case L3 R-items
   (`R-cs001-...`, `R-cs038-...`, `R-cs040-...`, `R-cs095-...`,
   `R-cs176-...`, `R-cs192-...`) plus the systematic generator
   R-item (`R-generator-get-customer-context-policy-mismatch`) cluster
   naturally into one L3 review sprint, but the briefs do not pre-
   suppose sizing. Open: batch all 7 into a single L3 sprint, or
   prioritize the `R-cs095-uc-classification-l3-rereview` and the
   generator-policy mismatch first (largest impact).
3. **Tier-0 promotion for `R-escalation-reason-runtime-evidence-contract-review`.**
   3 instances make this a strong Tier-0 candidate, but per §3.2 Q2,
   a new Tier-0 invariant requires a human decision via
   `human_review_required`. Open: does the human want to open a
   Tier-0 candidacy discussion now, or wait until G2 / G3+ case
   families confirm the contract scope?
4. **Manual-probe brief filing frequency.** Manual probes are
   one-shot traces; should G2 / G3+ adopt a regular cadence (e.g.
   one manual probe per sprint, picked by the human) to keep the
   real-traffic shadow channel populated, or is manual probing a
   reactive technique only?

## 11. Next recommended action

Recommended next sprint: **Sprint 19 — smoke regression
investigation** (`R-smoke-regression-investigation`). Narrow scope:
identify whether Sprints 14 / 14.1 / 15 / 16 introduced an
unintended runtime change, or whether the regression is driven by an
external factor (judge calibration drift, KB content change, eval
harness change). Outcome should clarify the 64% → 21% drop and
restore a clean baseline before G2 case-family construction.

After Sprint 19: **Sprint 20 — G2 Interactive Case Family + Shadow
Split**, using G1's 10 briefs as input. The conditional-broadening
record (§8.8) and the open observations (§8.7) inform G2's neighbor /
negative case selection.

Wave A5 / A6 L3 review batch can run in parallel with Sprint 19
since the two surfaces are disjoint (L3 review touches CaseSpec
overrides and judge rubric calibration; smoke regression touches
runtime / corpus / harness).
