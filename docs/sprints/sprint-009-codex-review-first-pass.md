## Sprint Review Decision

decision: fix_required
blocking_count: 1
summary: O0 and O1 are implemented, and the main O2 trace-result path now persists bounded result_data / result_summary with green Sprint 9 server coverage. O2 is not fully closed because failed-tool error surfaces and generic result maps are not sanitized enough to satisfy the sprint's secrets / sensitive-PII protection requirement.

## Blocking Sprint Failures

- severity: P1
- target case, tool, UI path, or test: O2 `bot_turns.tool_calls` sanitization for failed tool rows and generic `result_data`
- blocks current sprint goal: yes
- evidence: `ControlKernel.recordRunResult` persists `te.errorMessage()` directly as `error_message`, and `ToolCallTraceSanitizer.summarize(...)` builds failed-tool `result_summary` from the raw error string. The generic sanitizer copies arbitrary map keys and only redacts email-shaped strings, so a failed tool error or future tool result containing a phone number, postcode, token, password, API key, authorization header, or other secret-bearing key can still be written to the trace and rendered by TraceViewer. Existing Sprint 9 tests cover email redaction for a successful `resolve_article` title, but not failed-tool `error_message` / `result_summary` redaction or secret-key map redaction.
- exact minimal fix: Sanitize `error_message` before persistence and before summary generation, and extend `ToolCallTraceSanitizer.sanitizeAny` to redact sensitive key names plus phone/postcode/token-like values, preferably by reusing the existing PII redaction helper. Add focused tests proving failed-tool emails/phones/secrets are redacted in `error_message` and `result_summary`, and that generic result maps redact `password` / `token` / `secret` / `api_key` / `authorization` fields.

## Non-Blocking Notes

- severity: P2
- target case, tool, UI path, or test: `RequestHandoverTool.deriveFallbackSummary`
- blocks current sprint goal: no
- exact minimal fix, if any: Either include the form description as the method comment claims, or narrow the comment to the implemented sources: active UC, topic, escalation reason, and optional `current_user_message`.

## Regression Risks

- severity: P2
- target case, tool, UI path, or test: post-fix validation sweep
- blocks current sprint goal: no
- exact minimal fix, if any: After the O2 sanitizer fix, rerun `mvn -pl server test`, `python -m pytest -p no:capture eval_interactive/tests/test_agent_client_session_create_timeout.py`, and `ui/./node_modules/.bin/tsc --noEmit`; the latest existing server reports are green at 753 / 0 / 0 / 0 and the local ReadTimeout regression plus UI typecheck passed during this review.

## Recommended Next Phase

Re-run validation
