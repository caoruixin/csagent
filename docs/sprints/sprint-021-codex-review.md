## Sprint Review Decision (Sprint 21 fix re-review)
decision: fix_required
blocking_count: 3
summary: Fix commit scope is otherwise clean (`HEAD^..HEAD` touches only `docs/sprints/sprint-021-handoff.md`; override YAML, case-family, judge, server/data, governance, and foundational surfaces are unchanged) and the cs_095 dimension-distinction paragraph remains byte-identical, but the three evidence-gap findings are not closed because each approved override still has a non-verbatim `What should a good CS agent have done?` quote: cs_001 at `docs/sprints/sprint-021-handoff.md:280` drops source `*way*` emphasis and the trailing colon from `docs/diagnostics/failure-briefs/cs001-uc-c-template-escalate-on-faq-miss.md:26`; cs_095 at `docs/sprints/sprint-021-handoff.md:452` drops source `**account-state-aware investigation**` emphasis from `docs/diagnostics/failure-briefs/cs095-uc-classification-and-account-aware-path-skipped.md:52`; cs_192 at `docs/sprints/sprint-021-handoff.md:608` drops source `**...**` emphasis from `docs/diagnostics/failure-briefs/cs192-uc-b-mechanical-escalate-on-resolvable-giveaway-question.md:43`.

## Findings

### 1. cs_001 approved override lacks the required `What happened?` + `What should` brief quotes

- path: `docs/sprints/sprint-021-handoff.md:270`
- severity: blocking
- citation: Sprint 21 section 1.7 blocking gate / approved override lacking required section 1.7 evidence
- layer: `eval_spec`
- diff snippet:

```diff
+**§1.7 self-check.** From the brief (line 14): *"The bot's actual
+`escalation_reason=faq_miss_threshold_exceeded` is more semantically
+accurate."* From the brief (line 16): *"The bot's outcome (escalate)
+IS correct."* The override aligns the CaseSpec's expected reason
+with the truthful phase 2 reason and the bot's actual correct
+behaviour on this specific dimension — the CaseSpec / generator was
+the artefact that needed adjustment, not the bot.
```

Reasoning: The review prompt makes the section 1.7 evidence package blocking per approved override: the dev agent must quote the brief's `What happened?` and `What should a good CS agent have done?` fields, then show the bot was correct on the approved dimension or that the CaseSpec/rubric was wrong. For cs_001, the handoff quotes only Ground-truth-chain lines 14 and 16. It does not quote the `What happened?` field at `docs/diagnostics/failure-briefs/cs001-uc-c-template-escalate-on-faq-miss.md:18`, nor the `What should a good CS agent have done?` field at `docs/diagnostics/failure-briefs/cs001-uc-c-template-escalate-on-faq-miss.md:24`. Because the approved YAML entry changes `expected.escalation_trigger` and `expected.bot_handling_pattern`, the missing field quotes leave the required section 1.7 evidence incomplete.

### 2. cs_095 approved override lacks the required `What happened?` + `What should` brief quotes

- path: `docs/sprints/sprint-021-handoff.md:431`
- severity: blocking
- citation: Sprint 21 section 1.7 blocking gate / approved override lacking required section 1.7 evidence
- layer: `eval_spec`
- diff snippet:

```diff
+**§1.7 self-check.** From the brief (line 19): *"The bot stamped
+`active_use_case=UC-A` (matching the current override) but gave an
+answer that reads as UC-D (steps to change the contact email)."*
+The bot's USER-FACING CONTENT on UC-D-primary was correct (it
+answered the UC-D question by giving email-change steps); the bot's
+UC STAMP was wrong (stamped UC-A per the override but produced UC-D
+content). The override was the artefact that needed adjustment, NOT
+the bot's content choice on this dimension.
```

Reasoning: The cs_095 disposition quotes only Ground-truth-chain line 19 and then argues the classification dimension is clean. It does not quote the brief's `What happened?` field at `docs/diagnostics/failure-briefs/cs095-uc-classification-and-account-aware-path-skipped.md:27`, nor the `What should a good CS agent have done?` field at `docs/diagnostics/failure-briefs/cs095-uc-classification-and-account-aware-path-skipped.md:50`. Those fields are especially load-bearing here because the brief also describes real bot failures (skipped account-state investigation, duplicated greeting, internal source ID leak, and stall). Without the required field quotes in the handoff, the approved UC-D classification override has not cleared the prompt's blocking section 1.7 evidence bar.

### 3. cs_192 approved override lacks the required `What happened?` + `What should` brief quotes

- path: `docs/sprints/sprint-021-handoff.md:576`
- severity: blocking
- citation: Sprint 21 section 1.7 blocking gate / approved override lacking required section 1.7 evidence
- layer: `eval_spec`
- diff snippet:

```diff
+**§1.7 self-check.** Zero-scoring-impact correction: the L2
+`correct_uc` check compares the bot's stamped UC to the union of
+primary and secondary; the dedupe leaves that union unchanged. The
+bot's separate UC-B template-escalate failure on this resolvable
+giveaway question is captured by the Sprint 20 cs192 case family and
+is NOT affected by this override. This is the §1.7-cleanest possible
+override — generator quirk corrected; no bot pass/fail change.
```

Reasoning: The cs_192 disposition explains why the secondary-UC dedupe is intended to be scoring-neutral, but it does not quote the approved override's source brief fields required by the review prompt: `What happened?` at `docs/diagnostics/failure-briefs/cs192-uc-b-mechanical-escalate-on-resolvable-giveaway-question.md:22` and `What should a good CS agent have done?` at `docs/diagnostics/failure-briefs/cs192-uc-b-mechanical-escalate-on-resolvable-giveaway-question.md:41`. Since cs_192 is recorded as an approved override in `eval_interactive/case_spec_overrides.yaml`, the missing field quotes are a blocking approved-override evidence gap even though no semantic hardcode or scoring impact was observed.

## Non-Blocking Checks

- Anti-hardcode kernel: no keyword, regex, if/else, enum, per-UC runtime matrix, prompt branch, or judge rubric edit was introduced in `HEAD^..HEAD`.
- Cascade rule: `git diff --name-only HEAD^..HEAD -- eval_interactive/case_specs/case_families eval_interactive/case_specs_shadow/case_families` returns no touched files.
- Judge rubric: `eval_interactive/eval_interactive/case_spec/llm_persona_reviewer.py` is untouched.
- Override schema sanity: `_load_case_spec_overrides` loads the edited YAML with `applied: 17`, `pending: 0`, and the three Sprint 21 source session ids present.
