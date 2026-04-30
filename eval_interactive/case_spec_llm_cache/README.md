# Wave A6 LLM persona reviewer cache

One YAML file per `source_session_id`. Each file holds the rule_draft, llm_proposal, accepted_value, prompt hash, and acceptance_reason produced by `LlmPersonaReviewer.review` when the extractor builds that session's CaseSpec.

The extractor (`eval_interactive.case_spec.extractor.extract_case_specs`) writes these files on cache miss / `prompt_hash` mismatch / `--refresh-llm-session` and reads them on cache hit so regeneration is reproducible without hitting the DeepSeek API.

Files are regenerated automatically when the prompt template, rule_draft, or transcript turns change (the `prompt_hash` covers all three). Use `--refresh-llm-session SESSION_ID` (repeatable) to force a single re-call.

These files are committed to the repo and reviewed in PRs like any other source change. Diff review is the human gate on persona changes.

Wave A5 overrides in `eval_interactive/case_spec_overrides.yaml` always run AFTER L2 and have final word over any persona field this cache wrote.
