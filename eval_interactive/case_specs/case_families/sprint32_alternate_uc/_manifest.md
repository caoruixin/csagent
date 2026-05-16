# Sprint 32 alternate_candidate_use_cases — local manifest

This manifest is local to the `sprint32_alternate_uc/` directory and is
appended (as a single family entry) to the Sprint 20 G2 root manifest at
`eval_interactive/case_specs/case_families/_manifest.yaml`. The flat
layout mirrors the Sprint 29 (R2) directive-probe precedent
(`sprint29_directive_probe/`) for one structural reason:

The eval-harness loader at
`eval_interactive/eval_interactive/case_spec/loader.py:144` globs
`*.yaml` directly under the passed path. Nested subdirectories
(`target/`, `neighbor/`, `negative/`) or a `_manifest.yaml` inside the
loaded directory would either (a) break the single-invocation pattern
or (b) require the loader to be pointed at each subdir separately. A
flat layout with a markdown manifest (this file) keeps the loader
contract intact and lets the dev rerun the whole family with one
command (see "Load + run" below).

The five visible CaseSpecs in this directory exercise the §7.2 worked
example shape from `docs/current/iteration_governance.md` — UC-A user
drifts to UC-C (or another observably-emitted alternate UC) on turn 2
— and validate the Sprint 31 `alternate_candidate_use_cases`
projection slot end-to-end at the eval surface. The Sprint 31
fix-iteration #2 strengthened T8 covers the runtime-side
non-enforcement proof (six slot variants × five invariance bars in
`AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest`);
this family covers the LLM-behaviour proof.

Two additional shadow CaseSpecs live at
`eval_interactive/case_specs_shadow/case_families/sprint32_alternate_uc/`
per `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md`. The dev
agent WROTE the shadow files but did NOT execute them in the dev
session (access boundary). The visible root manifest carries
`shadow_case_ids: "[REDACTED -- see case_specs_shadow/_manifest.yaml]"`;
the shadow manifest carries the real shadow ids.

Provenance: every case carries `source_dataset: case_family_authored`
and `source_session_id: synthetic-sprint32-<class>-<idx>` (mirrors the
Sprint 29 `synthetic-sprint29-<directive>-<idx>` pattern and the
Sprint 20 G2 `synthetic-sprint20-<family>-<class>-<idx>` pattern).

Load + run (single invocation; matches dev-prompt §4.3):

```
cd eval_interactive && \
  uv run eval-interactive run \
    --path case_specs/case_families/sprint32_alternate_uc/
```

Loader behaviour: `load_case_specs` globs `*.yaml` only and skips this
markdown file, so the run picks up exactly the five visible CaseSpecs
below.

## Cases

| case_id | class | primary_uc | expected outcome_class | shape |
|---|---|---|---|---|
| `cs32t01_uc_a_uc_c_drift` | **target** | UC-C (end) / UC-A (start) | resolve | The §7.2 worked example. UC-A visibility complaint on turn 1; UC-C messaging drift on turn 2; turn 3 explicitly prioritises UC-C. Expected reroute or focused clarifying question; the `alternate_candidate_use_cases` slot is the soft signal. |
| `cs32n01_uc_a_uc_fp_drift` | **neighbor** #1 | UC-FP (end) / UC-A (start) | resolve | UC-A visibility complaint on turn 1; UC-FP removal-explanation drift on turn 2. Grounded in the observed `cs_interactive_015` alternates `["UC-A","UC-FP","UC-H"]` shape (Sprint 31 reference smoke at `eval_interactive/results/20260516-024934/results.json`). |
| `cs32n02_uc_a_uc_b_drift` | **neighbor** #2 | UC-B (end) / UC-A (start) | resolve | UC-A visibility complaint on turn 1; UC-B posting-policy drift on turn 2. Grounded in the observed `cs_interactive_192` alternates `["UC-A","UC-B","UC-FP"]` shape (Sprint 31 reference smoke). |
| `cs32g01_uc_a_deepens_no_drift` | **negative** #1 | UC-A | resolve | UC-A visibility complaint on turn 1; UC-A Top Ad downgrade follow-up on turn 2 (same UC, deeper question). Even if the alternates slot is populated, the LLM should IGNORE it and stay in UC-A — the load-bearing eval-side proof of §1.7 non-enforcement at the LLM-behaviour layer. |
| `cs32g02_uc_a_explicit_stay` | **negative** #2 | UC-A | resolve | UC-A visibility complaint on turn 1; user EXPLICITLY confirms staying on the visibility issue on turn 2. Even if the alternates slot is populated, the LLM should IGNORE it. Second load-bearing non-enforcement proof. |

Shadow CaseSpec ids are redacted in this manifest and in the visible
root manifest; see `eval_interactive/case_specs_shadow/_manifest.yaml`.

## Authoring discipline self-check (per `docs/sprint_objective.md` §3 + §7)

- **No trace-specific text in `hidden_facts`.** Only `cs32t01`,
  `cs32n01`, and the two shadow cases have non-empty `hidden_facts`;
  every fact describes user-side observable state (missing inbox
  messages, listing details, removal-notification email), not bot
  transcript text.
- **No keyword / regex preconditions in `expected.*`.** Every
  `expected.*` field uses structured shape only (`primary_uc`,
  `should_escalate`, `escalation_trigger`, `expected_tool_sequence`,
  `forbidden_tools`, `grounding_mode`, `answer_must_not_contain`,
  `max_turns`). `bot_handling_pattern` is prose describing what the
  bot should do, mirroring the Sprint 20 / Sprint 29 precedents.
- **No CaseSpec id mirrored into runtime / prompt / judge code.** No
  code, prompt, or judge config edits this sprint.
- **No L3 override authored** for any of the five visible cases.
  Sprint 32 is `eval_spec`-only authoring; if a bot failure surfaces
  on the family rerun, the failure IS the finding.
- **No editing of existing case families.** Sprint 20 / Sprint 29
  cascade fence observed.
- **No `expected.escalation_trigger` value outside the 23-value
  canonical set** (per `PhaseEvaluator.java:39–63`). All five visible
  cases use `escalation_trigger: null` (resolve outcomes).
- **No semantic hardcode introduced.** The CaseSpec text is pure
  user-side observable state + expected-behaviour bars; the LLM owns
  the drift / topic-shift decision per Constitution §1.3.
- **Shadow access boundary observed.** The dev WROTE the two shadow
  CaseSpec files but did NOT execute them in the dev session;
  shadow path arguments never appeared in any run command.
