# Sprint 29 (R2) directive-probe — local manifest

This manifest is local to the `sprint29_directive_probe/` directory and is
deliberately **not** appended to the Sprint 20 G2 root manifest at
`eval_interactive/case_specs/case_families/_manifest.yaml`. Two reasons:

1. The Sprint 29 (R2) probe shape is a different artefact from the
   Sprint 20 G2 case-family shape (target / neighbor / negative / shadow);
   appending probe entries to the G2 manifest would dilute that schema.
2. The Sprint 28 smoke loader (`load_case_specs` in
   `eval_interactive/eval_interactive/case_spec/loader.py:144`) globs
   `*.yaml` directly under the passed path. Layout choices that nest
   subdirectories (`d485_2/`, `d564_7/`, `d616_2/`) or place a
   `_manifest.yaml` inside the loaded directory either (a) break the
   single-invocation smoke pattern or (b) require the loader to be
   pointed at each subdir separately. A flat layout with a markdown
   manifest (this file) keeps the loader contract intact.

The four CaseSpecs are hand-authored per `docs/sprint_objective.md` §6.1
to exercise the precondition of a named directive (D485.2 / D564.7 /
D616.2). The observation per case is **precondition fires (yes/no)** and
**directive followed (yes/no)** — recorded in
`docs/sprints/sprint-029-handoff.md` §5.2.

Provenance: every case carries `source_dataset: case_family_authored` and
`source_session_id: synthetic-sprint29-<directive>-<idx>` (mirrors the
Sprint 20 G2 `synthetic-sprint20-<family>-<class>-<idx>` pattern).

Load + run (single invocation; matches dev-prompt §5):

```
cd eval_interactive && \
  uv run eval-interactive run \
    --path case_specs/case_families/sprint29_directive_probe/
```

Loader behaviour: `load_case_specs` globs `*.yaml` only and skips this
markdown file, so the run picks up exactly the four CaseSpecs below.

## Cases

| case_id | directive | primary_uc | expected outcome_class | precondition shape |
|---|---|---|---|---|
| `cs29d485_uc_b_delete_ad_resolution` | **D485.2** CLOSE record_outcome-if-not-already-recorded | UC-B | resolve | Resolve-path UC-B (posting / editing) with a clear FAQ ask and a user-satisfaction signal in turn 2. Bot is expected to reach RESOLVE → CONFIRM → CLOSE so the D485.2 directive can be observed. |
| `cs29d485_uc_c_email_notifications_resolution` | **D485.2** CLOSE record_outcome-if-not-already-recorded | UC-C | resolve | Resolve-path UC-C (messaging settings) with a clear FAQ ask and a user-satisfaction signal in turn 2. Authored as a second D485.2 case so Track B has more than one chance to observe the CONFIRM → CLOSE transition (the load-bearing case for Track B Q3 per sprint_objective §7.1). |
| `cs29d564_uc_h_ad_removal_appeal_complete_intake` | **D564.7** INTAKE intake-complete handover | UC-H | escalate | INTAKE UC-H (ad removal appeal). Form context supplies `ad_id` (AD-29447) and registered `email`; the user's first message supplies `stated_reason_or_context`. All three UC-H required intake fields per `IntakeFieldsRegistry.java:59` (`ad_id_or_listing_url` / `registered_email` / `stated_reason_or_context`) are observable inside two bot turns, so the bot is expected to call `request_handover(intake_complete_for_uc_h)`. |
| `cs29d616_uc_c_message_delivery_mild_frustration` | **D616.2** RESOLVE FAQ only-escalate-after-valid-resolve-attempt | UC-C | resolve | Resolve-path UC-C (messaging delivery) with a viable FAQ hit reachable ("Sending & Receiving Messages" surface) and mild user frustration that could tempt premature `request_handover(faq_miss_threshold_exceeded)`. D616.2 prohibits short-circuiting to handover before a valid resolve attempt. The `hidden_fact` captures user-side observable state (six months of prior success; user can see own sent messages); no bot trace text encoded. |

## Authoring discipline self-check (per `docs/sprint_objective.md` §6.2)

- **No trace-specific text in `hidden_facts`.** Only
  `cs29d564_uc_h_…` and `cs29d616_uc_c_…` have non-empty `hidden_facts`;
  both describe the user's situation (deletion-notification absence; six
  months of prior success), not bot transcript text.
- **No keyword / regex preconditions in `expected.*`.** Every
  `expected.*` field uses structured shape only (`primary_uc`,
  `should_escalate`, `escalation_trigger`, `expected_tool_sequence`,
  `forbidden_tools`, `grounding_mode`, `answer_must_not_contain`,
  `max_turns`). `bot_handling_pattern` is prose describing what the bot
  should do, mirroring the Sprint 20 cs011 / cs038 precedents.
- **No CaseSpec id mirrored into runtime / prompt / judge code.** No
  code, prompt, or judge config edits this sprint.
- **No L3 override authored** for any of the four cases. Sprint 29 is a
  probe; if a bot failure surfaces, the failure IS the finding.
- **No editing of existing case families.** Sprint 20 cascade fence
  observed.
- **No `expected.escalation_trigger` value outside the 23-value canonical
  set** (per `PhaseEvaluator.java:39–63`). Triggers used:
  `intake_complete_for_uc_h` (one case), `null` (three cases).
