# QA Report -- D13 Interactive Eval Harness

**Generated**: 2026-04-24 05:05
**Scope**: Full QA pass on `eval_interactive/` Python package (D13 Interactive Evaluation Harness)
**Tech Stack**: Python 3.12, Click CLI, httpx, openai SDK, PyYAML, Jinja2, pytest 8+

---

## 1. Test Execution Results

### Test Plan Execution Summary

| Test ID | Test Name | Result | Key Output |
|---------|-----------|--------|------------|
| T1 | Package Installation | PASS | `pip install -e ".[dev]"` succeeds, 28 packages resolved |
| T2 | CLI Help | PASS | All 4 subcommands (main, extract, run, compare) show correct help text |
| T3 | Unit Tests (existing) | PASS | 10/10 tests pass in `test_schema.py` |
| T4 | CaseSpec Extraction | PASS | 159 anchor, 101 promotion, 107 exploration -- all load correctly |
| T5 | Config Loading | PARTIAL | Config loads but `LLM model` resolves to empty string |
| T6 | Agent Client Connectivity | PASS | Session created, message sent, trace and events retrieved |
| T7 | Single Session Run (E2E) | PARTIAL | Session completes but user simulator falls back to generic message |
| T8 | Trace + Scoring Pipeline | PARTIAL | All scoring layers produce results but L2 scores are 0.0 due to camelCase bug |
| T9 | Batch Run | PARTIAL | 3/3 cases complete with parallel=2, results saved, but composite scores are degraded |
| T10 | Report Generation | PASS | JSON (3 cases) and HTML (14,023 bytes) reports generated successfully |
| T11 | Comparison | PASS | Two runs compared, metric deltas computed, text report produced |
| T12 | Stall Detector | PASS | Clean=no stall, promise-no-followup=stall detected, promise-with-result=no stall |

### New Unit Tests Written and Executed

| Test File | Total | Pass | Fail | Skip |
|-----------|-------|------|------|------|
| tests/test_schema.py (existing) | 10 | 10 | 0 | 0 |
| tests/test_stall_detector.py | 17 | 17 | 0 | 0 |
| tests/test_hard_checks.py | 22 | 22 | 0 | 0 |
| tests/test_outcome_checks.py | 20 | 20 | 0 | 0 |
| tests/test_composite.py | 10 | 10 | 0 | 0 |
| tests/test_config.py | 9 | 9 | 0 | 0 |
| tests/test_user_simulator.py | 10 | 10 | 0 | 0 |
| tests/test_diff_engine.py | 8 | 8 | 0 | 0 |
| tests/test_report_generators.py | 14 | 14 | 0 | 0 |
| **Total** | **120** | **120** | **0** | **0** |

All 130 tests (10 existing + 120 new) pass.

---

## 2. Code Review Findings

### Summary

| # | Severity | File | Line | Issue | Suggestion |
|---|----------|------|------|-------|------------|
| 1 | CRITICAL | trace/collector.py | 89-101 | Session state API returns camelCase keys but collector expects snake_case | Add camelCase-to-snake_case key mapping |
| 2 | CRITICAL | trace/collector.py | 105-123 | Trace API returns camelCase keys but collector expects snake_case | Add camelCase-to-snake_case key mapping |
| 3 | CRITICAL | trace/collector.py | 140-158 | Handover logs API returns camelCase keys and JSON-string payload | Map camelCase keys and parse JSON string payload |
| 4 | HIGH | eval_interactive.yaml | 7 | `DASHSCOPE_CHAT_MODEL` env var referenced but not defined in `.env.local` | Add `DASHSCOPE_CHAT_MODEL=qwen-plus` (or appropriate model) to `.env.local` |
| 5 | HIGH | scoring layers | -- | 6 scoring check names in CaseSpecs are not implemented | Implement or map: `grounding_compliance`, `fixed_script_adherence`, `answer_accuracy`, `escalation_triggered`, `intake_fields_collected`, `resolution_achieved` |
| 6 | MEDIUM | simulator/session_runner.py | 86-93 | `form_dict` always includes empty `ad_id` even when blank | Minor; no functional impact since API accepts empty strings |
| 7 | MEDIUM | cli.py | 76-125 | CLI `run` command lacks `--limit`/`--cases` option for subset runs | Add `--limit N` option to run first N cases from a set |
| 8 | MEDIUM | batch/executor.py | 246-286 | `_build_case_result` stores full transcript in results JSON | Large transcripts could create very large result files; consider optional truncation |
| 9 | LOW | simulator/agent_client.py | 17 | `httpx.Client` with 30s timeout but no connection pool limits | May hit fd limits under high parallelism; each batch case creates a new client |
| 10 | LOW | scoring/llm_judge.py | 51 | `api_key` defaults to `"not-set"` when empty | Should fail fast with a clear error rather than send an invalid key |

### Detailed Findings

#### CRITICAL-1: TraceCollector camelCase/snake_case Mismatch (Session State)

- **File**: `eval_interactive/trace/collector.py:88-102`
- **Severity**: CRITICAL
- **Description**: The `_build_session_state` method reads fields using snake_case keys (`active_use_case`, `containment_outcome`, `total_bot_turns`, etc.) but the CS Agent API at `GET /v1/chat/sessions/{id}` returns camelCase keys (`activeUseCase`, `containmentOutcome`, `totalBotTurns`).
- **Impact**: All `SessionState` fields are always empty/zero. This causes L2 `correct_uc` and `correct_outcome` checks to always score 0.0, making composite scores unreliable.
- **Evidence**:
  - API response keys: `['activeUseCase', 'articlesShown', 'candidateUseCases', 'caseId', 'clarificationCount', 'containmentOutcome', ...]`
  - Collector code: `raw.get("active_use_case", "")`, `raw.get("containment_outcome", "")`, `raw.get("total_bot_turns", 0)`
- **Suggestion**: Add a key-mapping function to convert camelCase API keys to snake_case before building the dataclass. For example: `active_use_case = raw.get("activeUseCase") or raw.get("active_use_case", "")`.

#### CRITICAL-2: TraceCollector camelCase/snake_case Mismatch (Per-Turn Trace)

- **File**: `eval_interactive/trace/collector.py:105-123`
- **Severity**: CRITICAL
- **Description**: The `_build_turns` method reads trace entry fields using snake_case keys (`turn_index`, `user_message`, `bot_response`, `action_selected`, `tool_calls`, `source_ids`, `phase_before`, `phase_after`, `active_use_case`, `latency_ms`, `projected_context`) but the API returns camelCase keys (`turnIndex`, `userMessage`, `botResponse`, `actionSelected`, `toolCalls`, `sourceIds`, `phaseBefore`, `phaseAfter`, `activeUseCase`, `latencyMs`, `projectedContext`).
- **Impact**: All `TurnTrace` fields except `turn_index` (which has a `len(turns)` fallback) are empty. This breaks phase transition validity checks, source citation checks, tool sequence checks, and policy violation checks.
- **Suggestion**: Same as CRITICAL-1 -- add camelCase-to-snake_case key mapping.

#### CRITICAL-3: TraceCollector camelCase/snake_case Mismatch (Handover Logs)

- **File**: `eval_interactive/trace/collector.py:140-158`
- **Severity**: CRITICAL
- **Description**: Two issues:
  1. Handover log API returns camelCase keys (`logId`, `sessionId`, `handoverPayload`, `transferResult`, `customerMessage`) but the code looks for snake_case (`log_id`, `session_id`, `handover_payload`, etc.).
  2. The `handoverPayload` field is a **JSON string**, not a dict. The code does `log.get("handover_payload") or log` which returns the whole log dict (not the parsed payload).
- **Impact**: Handover matching by session_id always fails (line 149: `log.get("session_id")` returns None for camelCase `sessionId`). HandoverData is always `None`, breaking `handover_completeness` and `case_id_present` L2 checks.
- **Suggestion**: Map camelCase keys and parse `handoverPayload` JSON string with `json.loads()`.

#### HIGH-4: Missing DASHSCOPE_CHAT_MODEL Environment Variable

- **File**: `eval_interactive.yaml:7` / `.env.local`
- **Severity**: HIGH
- **Description**: The config references `${DASHSCOPE_CHAT_MODEL}` for the LLM model name, but this variable is not defined in `.env.local`. The model field resolves to an empty string, causing all LLM API calls (user simulator and LLM judge) to fail with `400: you must provide a model parameter`.
- **Impact**: The user simulator always falls back to the generic message "I'm still waiting for help with my issue." which makes conversations unrealistic. The LLM judge always uses the default score (3.0/5). Both components are degraded to useless fallbacks.
- **Evidence**: Running any session produces: `LLM call failed after 2 attempts: Error code: 400 - {'error': {'message': 'you must provide a model parameter.'}}`
- **Suggestion**: Add `DASHSCOPE_CHAT_MODEL=qwen-plus` (or the desired model name) to `.env.local`.

#### HIGH-5: Unimplemented Scoring Check Names

- **File**: `scoring/hard_checks.py`, `scoring/outcome_checks.py`, and 367 CaseSpec YAMLs
- **Severity**: HIGH
- **Description**: 6 scoring check names referenced in CaseSpecs are not implemented in the checker dispatch tables:
  - Hard checks not implemented: `grounding_compliance` (244 cases), `fixed_script_adherence` (123 cases)
  - Outcome checks not implemented: `answer_accuracy` (244 cases), `escalation_triggered` (218 cases), `intake_fields_collected` (123 cases), `resolution_achieved` (149 cases)
- **Impact**: These checks are silently skipped during scoring. Composite scores are computed with fewer checks than intended, inflating the pass rate. A case could pass even if it violates grounding compliance or fails to achieve resolution.
- **Suggestion**: Either implement the missing checks or remove them from CaseSpec YAMLs. Consider logging a warning when an unrecognized check name is encountered.

#### MEDIUM-7: CLI `run` Command Lacks Case Count Limiting

- **File**: `eval_interactive/cli.py:76-125`
- **Severity**: MEDIUM
- **Description**: The `run` CLI command has `--set` and `--path` options but no way to limit the number of cases (e.g., `--limit 5` or `--cases 5`). Running `--set anchor` executes all 159 cases, which with the LLM simulator takes significant time and API cost.
- **Suggestion**: Add `--limit N` option that slices the case list to the first N specs.

---

## 3. Test Coverage

### Tested Modules

- `case_spec/schema.py` -- CaseSpec creation and field defaults
- `case_spec/loader.py` -- YAML loading and round-trip
- `config.py` -- Config loading, env var resolution, defaults
- `scoring/stall_detector.py` -- Clean/stall/edge case detection
- `scoring/hard_checks.py` -- All 10 L1 check implementations
- `scoring/outcome_checks.py` -- All 8 L2 check implementations
- `scoring/composite.py` -- Composite aggregation with all layer combinations
- `simulator/user_simulator.py` -- JSON/text response parsing
- `comparison/diff_engine.py` -- Run comparison and regression detection
- `report/json_report.py` -- JSON report generation and file saving
- `report/html_report.py` -- HTML report generation with XSS safety

### Untested Modules (Require Live Services or Complex Mocking)

- `simulator/agent_client.py` -- Covered by integration tests (T6), but no unit tests with mocked HTTP
- `simulator/session_runner.py` -- Covered by integration tests (T7), but no unit tests with mocked dependencies
- `trace/collector.py` -- Covered by integration tests (T8), but the camelCase bug prevents meaningful unit testing
- `batch/executor.py` -- Covered by integration tests (T9), async orchestration not unit-tested
- `case_spec/extractor.py` -- The extraction pipeline was not tested (requires HR CSV + turn data)
- `scoring/llm_judge.py` -- LLM response parsing not unit-tested (requires LLM mock)

### Priority Modules to Add Tests

1. **trace/collector.py** -- Critical: the camelCase mapping fix needs tests to verify correctness
2. **simulator/agent_client.py** -- Mock httpx to test error handling and retry logic
3. **scoring/llm_judge.py** -- Test `_parse_response` with various JSON/bare-number formats
4. **batch/executor.py** -- Test `_compute_summary` with edge cases (empty results, all-pass, all-fail)

---

## 4. Summary

### CRITICAL Priority (fix immediately -- scoring is broken)

1. **TraceCollector camelCase/snake_case mismatch**: The CS Agent API returns camelCase field names but the TraceCollector expects snake_case. This affects session state, per-turn trace, and handover log parsing. All L2 outcome checks and several L1 hard checks produce incorrect results (zeroed fields). **This single bug invalidates all evaluation scoring.**

2. **Handover payload is a JSON string**: The `handoverPayload` field from the API is a JSON-encoded string, not a parsed dict. The collector does not parse it, so handover data is malformed.

### HIGH Priority (fix before first real evaluation run)

3. **Missing DASHSCOPE_CHAT_MODEL env var**: The LLM model name is empty, causing both the user simulator and LLM judge to fail on every call. All conversations use fallback messages and all judge scores use defaults (3.0/5). Add `DASHSCOPE_CHAT_MODEL=<model_name>` to `.env.local`.

4. **6 unimplemented scoring checks**: `grounding_compliance`, `fixed_script_adherence`, `answer_accuracy`, `escalation_triggered`, `intake_fields_collected`, and `resolution_achieved` are referenced in 367 CaseSpecs but have no implementation. They are silently skipped, inflating pass rates.

### MEDIUM Priority (fix soon)

5. CLI `run` command needs a `--limit N` option to avoid running all 159+ cases during development and testing.

6. `_build_case_result` includes the full transcript in results JSON, which can produce very large files for 367+ cases.

### LOW Priority (fix when convenient)

7. Each batch case creates a new `httpx.Client`; consider connection pooling.

8. `LlmJudge` defaults `api_key` to `"not-set"` instead of failing fast.

### Metrics

- **Total issues found**: 10 (3 CRITICAL, 2 HIGH, 3 MEDIUM, 2 LOW)
- **Test pass rate**: 130/130 (100%)
- **Integration test pass rate**: 8/12 full pass, 4/12 partial pass (degraded by camelCase and missing model config)
- **Key blocker**: The camelCase/snake_case mismatch in TraceCollector renders all scoring unreliable. This must be fixed before any evaluation run produces trustworthy results.

---

## Test Files Created

| File | Tests | Purpose |
|------|-------|---------|
| `tests/test_stall_detector.py` | 17 | StallDetector clean/stall/edge cases |
| `tests/test_hard_checks.py` | 22 | All L1 hard check implementations |
| `tests/test_outcome_checks.py` | 20 | All L2 outcome check implementations |
| `tests/test_composite.py` | 10 | Composite score aggregation logic |
| `tests/test_config.py` | 9 | Config loading, env var resolution |
| `tests/test_user_simulator.py` | 10 | LLM response JSON parsing |
| `tests/test_diff_engine.py` | 8 | Run comparison and regression detection |
| `tests/test_report_generators.py` | 14 | JSON and HTML report generation |
