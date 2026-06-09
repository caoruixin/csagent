# QA Report — LLM Config Centralization

**Generated**: 2026-05-16 (local run)  
**Scope**: Phases 1–6 per verification plan; server module + runtime smoke.

## Summary Table (user-requested format)

### Phase 1: Compilation

**Status**: PASS  

**Details**: `mvn -pl server compile test-compile -q` completed with exit code 0.

### Phase 2: Unit Tests

**Status**: FAIL  

**Failures**:
- `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` — `ACTIVE-UC TIEBREAKER must be tagged with the Sprint 6 anchor` — `expected: <true> but was: <false>` (prompt text no longer contains the substring `"Sprint 6"` near the ACTIVE-UC block; section header `ACTIVE-UC TIEBREAKER:` remains).

**Fixes applied**: None (failure is driven by `prompts/system_prompt.txt` content vs golden test, not by LLM constructor/config beans).

**Totals**: Tests run: 917, Failures: 1, Errors: 0, Skipped: 2.

### Phase 3: Startup Verification

**Status**: PASS  

**Startup log line** (excerpt):

`LLM provider lineup: primary=deepseek[model=deepseek-v4-flash, base=https://api.deepseek.com/v1, key=present, thinking=disabled], fallback=kimi[model=kimi-k2.6, base=https://api.moonshot.ai/v1, key=present, thinking=disabled]`

**Checks**:
- model `deepseek-v4-flash` (not `deepseek-v4-pro`): ✓  
- Kimi key present: ✓  
- DeepSeek key present: ✓  
- Thinking disabled for both: ✓  

**Dotenv**: Startup used `mvn spring-boot:run -Dspring-boot.run.profiles=local` from `server/`; lineup shows `.env.local`-backed keys/model as expected on this machine.

### Phase 4: API Smoke Test

**Status**: PASS (with corrections to the scripted curl plan)

**Notes**:
- Documented API is `POST /v1/chat/sessions` with snake_case keys (`first_name`, `topic_subject`, …), not `POST /api/chat/session` (that path returns `No static resource`, HTTP 500).
- Smoke test used `http://127.0.0.1:8080` for reliable JSON responses.

**Sample**: Session creation + `POST /v1/chat/sessions/{id}/messages` with message `hi, I cannot see my advert` returned a normal assistant reply (not “Sorry, I'm a bit slow…” / deadline copy). `additional_data.latency_ms` ~4.8s.

**Logs**: `LLM [chat:request] provider=deepseek model=deepseek-v4-flash` with successful `LLM [chat:response]` lines (no timeout on primary).

### Phase 5: Hardcode Audit

**Status**: PASS (with documented allowances)

| Pattern | Result |
|--------|--------|
| `rg "deepseek-v4-pro" --type java --type py` | Matches: `LlmConfigValidatorTest.java` (explicit test setup), Python regression tests (`test_case_spec_*.py`) — acceptable as fixtures/tests, not production defaults. |
| `rg "DASHSCOPE_CHAT_MODEL"` | No matches in java/py/yaml. |
| `rg 'model.*=.*"kimi-k2' --type java` | No matches. |

**Remaining references in `server/src/main/java`**: comments only (e.g. `OpenAiCompatibleLlmClient`, `ChatController` javadoc) — **LOW** for “zero hardcode” policy.

### Phase 6: Browser Test

**Status**: SKIPPED  

**Details**: `curl` to `http://127.0.0.1:5173/` failed (connection error during this run). Earlier `lsof` had shown a node process on 5173; no interactive browser verification performed here.

---

## Issues Found

| # | Severity | Description | Status |
|---|----------|-------------|--------|
| 1 | MEDIUM | `SystemPromptUserRequestedTiebreakerTest` fails: `system_prompt.txt` no longer contains `"Sprint 6"` anchor required by Sprint 6 §G1 regression test. | OPEN |
| 2 | LOW | Test plan’s URLs/body shape (`/api/chat/session`, camelCase JSON) do not match `ChatController` (`/v1/chat/sessions`, snake_case). | DOC / OPEN |
| 3 | LOW | `deepseek-v4-pro` still appears in Java/Python **tests** and eval helpers by design; confirm team acceptance. | OPEN (policy) |

### Overall Verdict: **FAIL**

**Reason**: Phase 2 unit suite has 1 failing test (`917` run, `1` failure). LLM config centralization, compile, startup lineup, and runtime LLM smoke (Phase 1, 3, 4) behaved as expected on the test host.
