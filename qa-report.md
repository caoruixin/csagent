# QA Report: LLM Interaction Detail (Admin TraceViewer)

**Generated**: 2026-04-25  
**Scope**: Frontend-only changes — `TraceStep` types, `mapTrace` in `client.ts`, `TraceViewer.tsx` (LlmDetailPanel + metadata).  
**Tech stack**: React + TypeScript (Vite), ESLint (flat config), no new backend changes tested.

---

## Test cases

| ID | Description | Status | Details |
|----|-------------|--------|---------|
| P1-001 | TypeScript: `npx tsc --noEmit` in `ui/` | **PASS** | Exit code 0. |
| P1-002 | ESLint: `src/components/admin/TraceViewer.tsx`, `src/types/index.ts`, `src/api/client.ts` | **FAIL** | 3 errors (see below). `index.ts` reported no issues. |
| P1-003 | All planned `data-testid` attributes present in `TraceViewer.tsx` | **PASS** | Found: `llm-reasoning`, `projected-context-toggle`, `llm-raw-response-toggle`, `action-params-toggle`, `llm-detail-btn`. |
| P2-001 | Production build: `npm run build` | **PASS** | `tsc -b && vite build` completed; assets emitted. |
| P3-001 | `TraceStep` includes 8 new optional fields | **PASS** | `projected_context`, `llm_raw_response`, `action_parameters`, `source_ids`, `phase_before`, `phase_after`, `active_use_case`, `latency_ms` — all optional. |
| P3-002 | `mapTrace` accepts camelCase and snake_case from API | **PASS** | Uses `??` fallbacks (e.g. `t.llmRawResponse ?? t.llm_raw_response`). |
| P3-003 | JSON string fields use `tryParse` | **PASS** | `projected_context` and `action_parameters` parsed when `typeof ... === 'string'`. |
| P3-004 | `LlmDetailPanel` tolerates missing/empty data | **PASS** | Handles no LLM, parse errors, empty projected context, optional tool data. **Note**: `action-params-toggle` is **not** rendered when `action_parameters` is empty/undefined (conditional block). |
| P3-005 | `parseLlmRawResponse` edge cases | **PASS** | `null`/empty → no error; valid JSON → parsed; invalid JSON → `parseError: true`, raw shown in raw section. |
| P3-006 | `getReasoningText` behavior | **PASS** | Returns `null` when parse error, non-object, or no `reasoning` key; normalizes non-string `reasoning` via `JSON.stringify`. |
| P3-007 | Collapsible sections default collapsed | **PASS** | `useState(false)` for projected, raw, and action toggles. |
| P3-008 | `data-testid` names match test plan | **PASS** | Matches specified IDs. |
| P4-001 | API smoke: `GET /v1/demo/sessions` | **PASS** | Backend responded with JSON session list (localhost:8080). |
| P4-002 | API smoke: trace payload includes new BotTurn-style fields | **SKIP** | `GET .../trace` returned `[]` for sampled session; could not validate live field presence. |

---

## Summary

| Metric | Count |
|--------|-------|
| Total test cases | 14 |
| Passed | 12 |
| Failed | 1 |
| Skipped | 1 |

---

## Issues (severity)

### 1. ESLint errors on targeted files (lint fails)

- **Severity**: **Major** (breaks `npx eslint ...` in CI or pre-commit if enforced).
- **Details**:
  - `client.ts:133` — `@typescript-eslint/no-explicit-any` on `turns: any[]` in `mapTrace` (touches changed code).
  - `client.ts:204` — `no-explicit-any` in `getPerUCMetrics` map callback (unchanged feature area, but file is in lint scope).
  - `TraceViewer.tsx:18` — `react-hooks/set-state-in-effect`: synchronous `setLoading(true)` inside `useEffect` body.
- **Suggestion**: Replace `any` with a typed DTO or `unknown` + narrow; adjust loading pattern per React guidance (e.g. derive loading from request promise / key) or document eslint-disable with rationale if intentional.

### 2. `action-params-toggle` only when action parameters are non-empty

- **Severity**: **Minor** (E2E or automation that always expects the toggle in DOM will not find it on turns without `action_parameters`).
- **File**: `TraceViewer.tsx` — section wrapped in `{!isEmptyRecord(step.action_parameters) && (...)}`.
- **Suggestion**: If tests require a stable selector, add an always-present wrapper with a test id, or document that the toggle is conditional.

### 3. Live trace empty for sample session

- **Severity**: **Cosmetic** for this QA run (environment/data), not a code defect.
- **Details**: Could not end-to-end verify API returns `llmRawResponse`, `projectedContext`, etc., because trace array was empty.

---

## Code review notes (no issue logged)

- Metadata bar: phase label, `active_use_case` badge, `latency_ms`, `source_ids` count — consistent optional chaining.
- `llm-detail-btn` uses `e.stopPropagation()` to avoid toggling the step card — appropriate.
- `LlmDetailPanel` uses light blue container (`#F0F9FF` / `#BAE6FD`) for reasoning area as specified.

---

## Quick reference: `data-testid` line map

| testid | Approx. line |
|--------|----------------|
| `llm-reasoning` | 124 |
| `projected-context-toggle` | 161 |
| `llm-raw-response-toggle` | 187 |
| `action-params-toggle` | 210 (conditional) |
| `llm-detail-btn` | 379 |
