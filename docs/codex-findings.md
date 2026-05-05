## Sprint Review Decision

decision: fix_required
blocking_count: 1
summary: I0 and I1 are implemented within the requested scope, and the diff does not add S3, S5, CaseSpec churn, judge calibration, or a fourth Sprint 7 action. I2 is not complete: `intake_state` is projected, but partial intake fields are not maintained across normal clarification turns, so the bot cannot reliably ask only for missing UC-K fields or close the cs066 stall target.

## Blocking Sprint Failures

- severity: P1
- target case or test: I2 / cs_interactive_066 UC-K intake-state orchestration
- blocks current sprint goal: yes
- evidence: `ContextProjectionBuilder` derives `intake_state.fields_collected`, `fields_remaining`, and `intake_complete` only from `session.getIntakeFields()`. The only production Sprint 7 write to `session.intakeFields` is `AgentRunLoopImpl.persistInlineIntakeFields`, and it runs only when the LLM emits `request_handover.arguments.intake_fields`. A normal intake clarification turn with no tool calls does not persist any field from the user's answer, so the next projection can still show all required fields as remaining. The focused tests pre-seed `session.intakeFields` or persist fields from a handover call; they do not cover a multi-turn cs066 path where the user supplies one required field, the session updates it, and the next bot turn asks only for the other missing field.
- exact minimal fix: In the existing RESOLVE_INTAKE path, before building the next `intake_state` projection, merge newly supplied required-field values from the current intake user turn and form context into `session.intakeFields` using `IntakeFieldsRegistry` aliases. For the cs066 UC-K anchor, seed `repro_steps_or_error_message` from the form/current issue text when present and capture `platform` from the user's reply. Add a focused test proving that after one UC-K field is supplied on a clarification turn, the next projection removes it from `fields_remaining` and the bot asks only for the remaining required field; keep this inside `PhaseEvaluator` / `AgentRunLoop` with no new skill runtime framework.

## Non-Blocking Notes

- severity: P2
- target case or test: I0 / cs_interactive_259 candidate_use_cases projection
- blocks current sprint goal: no
- exact minimal fix, if any: None for current Sprint 7 scope. `candidate_use_cases` is projected from existing `session.candidateUseCases`, the DISCOVER cue is anchored to the empty-form payment / sale-proceeds FAQ shape, and the Sprint 6 S1 FAQ-grounded-resolve guard remains covered by focused tests.

- severity: P2
- target case or test: I1 / cs_interactive_015 UC-FP vs UC-A tiebreaker
- blocks current sprint goal: no
- exact minimal fix, if any: None for current Sprint 7 scope. The routing prompt now receives a narrow `routing_context` from existing session moderation/listing/account state and preserves cs095/cs014/cs066 negative prompt guards; clean runtime measurement is still blocked by upstream auth contamination.

- severity: P2
- target case or test: scope discipline
- blocks current sprint goal: no
- exact minimal fix, if any: None. The latest `HEAD~1..HEAD` code diff is limited to the three Sprint 7 surfaces plus focused tests/constructor updates; no CaseSpec files, eval tests, ReadTimeout retry, S3 no-prior-search runtime guard, S5 Tier-2 runtime guard, judge calibration, broad prompt rewrite, or broad routing taxonomy rewrite were added.

## Regression Risks

- severity: P2
- target case or test: contaminated Sprint 7 targeted/smoke evals
- blocks current sprint goal: no
- exact minimal fix, if any: Rotate or repair the upstream Kimi credential before promoting any post-Sprint-7 smoke baseline. The latest smoke (`eval_interactive/results/20260505-210359/results.json`) and targeted cs015/cs259 runs show `CONTRACT_VIOLATION:active_use_case` only on cases that hit upstream 401 before a routing/classify turn; this is not clean evidence of a Sprint 7 routing regression.

- severity: P2
- target case or test: regression guard test coverage
- blocks current sprint goal: no
- exact minimal fix, if any: None beyond the I2 blocker fix. Targeted verification run passed 52 tests covering the Sprint 7 focused suites plus S1, cs014, cs176 explicit-human-help, and cs002 reconciliation guards; `AgentRunLoopIntakeIntegrationTest` also passed.

## Recommended Next Sprint Actions

- After the I2 blocker is fixed and Sprint 7 closes, rerun targeted cs259/cs015/cs066 and two clean smoke runs with valid LLM credentials before promoting a new canonical baseline.
