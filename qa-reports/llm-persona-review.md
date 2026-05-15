# LLM persona review (Wave A6.1 shadow audit)

- run_timestamp: `2026-04-30T06:10:37.167698+00:00`
- model: `deepseek-v4-pro`
- prompt_template_sha256: `61617a17cfd68a14ef0a7496724c2141e0896b6e0b92c34394668f858e9c0364`
- sessions_audited: **1**
- calls_made: **0**
- calls_skipped_no_transcript: **0**
- dry_run: `True`

## Summary by field changed

| Field | sessions LLM would change |
|-------|---------------------------|
| seed_messages     | 0 |
| user_goal_summary | 0 |
| hidden_facts      | 0 |

## Confidence distribution

(no confidence values returned)

## Validation failure breakdown

| Status | Count |
|--------|-------|
| dry_run | 1 |

## cs_interactive_012 — full before/after

- spec path: `eval_interactive/case_specs/anchor/cs_interactive_012.yaml`
- llm_confidence: ``
- validation_status: `dry_run`
- validation_notes:
  - dry_run: no DeepSeek call made

**rule_draft seed_messages:**
  - "Hi Jason"
  - "Thank you and happy new year"
  - "Ok pls look in to this as soon as possible pls"

**llm_proposal seed_messages:**
  (none)

**rule_draft user_goal_summary:** 'User reports Ad Support: Kitten. Drift: soft_shift.'
**llm_proposal user_goal_summary:** ''

**rule_draft hidden_facts:**
  (none)

**llm_proposal hidden_facts:**
  (none)

**llm_rationale:** ''

## Notable proposed changes (top 15)

### cs_interactive_012  (session=`570Q5000008hx9tIAA`, UC=`UC-FP`, dataset=`badcase`)

- llm_confidence: ``
- validation_status: `dry_run`
- validation_notes:
  - dry_run: no DeepSeek call made

**rule_draft.seed_messages:**
  - "Hi Jason"
  - "Thank you and happy new year"
  - "Ok pls look in to this as soon as possible pls"

**llm_proposal.seed_messages:**
  (none)

**rule_draft.user_goal_summary:** 'User reports Ad Support: Kitten. Drift: soft_shift.'
**llm_proposal.user_goal_summary:** ''

**llm_rationale:** ''
