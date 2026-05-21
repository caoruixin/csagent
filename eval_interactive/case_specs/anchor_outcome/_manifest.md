# anchor_outcome suite manifest

Outcome-only anchor suite created in Sprint 42 / S-Eval-1 (NEW M3-Eval
sub-sprint 1, 2026-05-21). Each case declares ONLY `outcome_class` +
`persona.user_goal_summary` + `closure_criterion`. `expected_tool_sequence`,
`forbidden_tools`, `bot_handling_pattern`, and `escalation_trigger` are
intentionally omitted (or set to None) so the suite anchors on the user's
goal rather than the bot's procedural path.

The suite is the **second human-judgment surface** beside the curated
bad-case suite at `eval_interactive/case_specs/bad_cases/` per
`docs/current/iteration_governance.md` §5.5 (S-Eval-1 append).

## Per-UC coverage (12 cases, one per canonical UC)

| Case ID | UC | should_escalate | source_session_id provenance |
|---|---|---|---|
| `anchor_outcome_uc_a_visibility` | UC-A | false | reused from smoke `cs_interactive_095` (UC-A email-visibility confusion) |
| `anchor_outcome_uc_b_posting` | UC-B | false | reused from smoke `cs_interactive_192` (UC-B giveaway / free-items posting) |
| `anchor_outcome_uc_c_messaging` | UC-C | false | reused from smoke `cs_interactive_002` (UC-C message notifications) |
| `anchor_outcome_uc_d_login` | UC-D | false | reused from smoke `cs_interactive_011` (UC-D login issue) |
| `anchor_outcome_uc_e_promotion` | UC-E | false | reused from smoke `cs_interactive_176` (UC-E paid Top Ad placement) |
| `anchor_outcome_uc_f_billing` | UC-F | false | reused from smoke `cs_interactive_259` (UC-F billing / payment question) |
| `anchor_outcome_uc_fp_removed` | UC-FP | false | reused from smoke `cs_interactive_015` (UC-FP ad removed) |
| `anchor_outcome_uc_g_gdpr` | UC-G | true | synthetic (UC-G has zero existing coverage in smoke + anchor + family fixtures at HEAD `6ceae7c`; flagged `source_dataset: synthetic`) |
| `anchor_outcome_uc_h_appeal` | UC-H | true | reused from `case_families/sprint29_directive_probe/cs29d564_uc_h_ad_removal_appeal_complete_intake` (UC-H synthetic appeal-with-prefill intake) |
| `anchor_outcome_uc_i_payment` | UC-I | true | reused from smoke `cs_interactive_036` (UC-I delivery / payment dispute) |
| `anchor_outcome_uc_j_safety` | UC-J | true | reused from smoke `cs_interactive_038` (UC-J safety report intake) |
| `anchor_outcome_uc_k_tech` | UC-K | true | reused from smoke `cs_interactive_066` (UC-K technical issue intake) |

12 cases, within the 10-15 target range. 11 cases reuse real or
representative source sessions (smoke + family); 1 case (UC-G) is
synthetic because UC-G has zero existing coverage across smoke / anchor /
case-family fixtures at HEAD. The UC-G synthetic provenance is flagged
in handoff §6 as an observation for M3-Eval (relevant to S-Eval-3 and
S-Eval-4 scope decisions).

## Schema discipline

- **No `expected.expected_tool_sequence`**: omitted on every case.
- **No `expected.forbidden_tools`**: omitted on every case.
- **No `expected.bot_handling_pattern`**: omitted on every case (loader
  passes through None; schema accepts None per S-Eval-1 D-1.1).
- **No `expected.escalation_trigger`**: omitted on every case. Cases
  with `should_escalate=true` (UC-G/H/I/J/K) deliberately omit the
  trigger to exercise the S-Eval-1 D-1.2 schema relaxation; the scorer
  treats the missing trigger as advisory per the §5.5 anchor_outcome
  text.
- **`scoring.hard_checks: []`, `scoring.outcome_checks: []`,
  `scoring.llm_judge_dimensions: []`**: empty arrays on every case.
  Per S-Eval-1 D-2.4 + composite.py change, an empty
  `scoring.outcome_checks` opts the case out of the mandatory-L2
  gate; per S-Eval-1 D-2.x, the Tier-3 advisory dims (`tool_sequence_match`,
  `no_forbidden_tools`, `escalation_reason_consistency` when trigger
  absent) do not contribute to the gate.

## Use

```
cd eval_interactive
uv run eval-interactive run --path case_specs/anchor_outcome/
```

At sprint or milestone close, deliver-agent + human read each case's
per-turn trace and judge PASS / FAIL / IMPROVING **qualitatively**
against `closure_criterion`. This is a **human-judgment gate**, not a
programmatic gate (mirroring the bad-case suite contract in
`iteration_governance.md` §5.6).
