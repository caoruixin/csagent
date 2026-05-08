## Sprint Review Decision

decision: pass
blocking_count: 0
summary: Sprint 15 meets the exact M0 / M1 / M2 objective in a behaviour-preserving way: DriftDetector risk keywords are YAML-backed with parity coverage, script template version metadata is pinned against the approved docs, and knowledge retrieval / rerank thresholds are config-visible with unchanged defaults and diagnostics. The diff stays inside config-governance scope and avoids prompt, routing, judge, corpus, CaseSpec, hard citation gate, hot reload, dashboard, or broad runtime work.

## Blocking Sprint Failures

None.

## Non-Blocking Notes

- severity: P2
- target doc/code/test: `docs/10-handoff.md` §16.3
- blocks current sprint goal: no
- exact minimal fix, if any: Optional documentation polish only: the focused command `mvn -pl server test -Dtest='Sprint15*,DriftDetectorTest,RerankServiceTest'` reports 55 tests locally (25 Sprint 15 + 18 `DriftDetectorTest` + 12 `RerankServiceTest`), while the handoff says 42. Update the count if this doc is touched again; the command is green and this does not affect Sprint 15 acceptance.

- severity: P2
- target doc/code/test: `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint15RiskKeywordsConfigTest.java` and `server/src/test/java/com/gumtree/csagent/service/guardrails/Sprint15ScriptLibraryConsistencyTest.java`
- blocks current sprint goal: no
- exact minimal fix, if any: Optional hardening only: if future governance wants parser-level duplicate YAML mapping-key detection, enable strict duplicate detection in the YAML `ObjectMapper` used by the config tests. Current tests do cover duplicate risk keywords, required fields, empty groups, version parity, required template IDs, and required variables, so Sprint 15's behaviour-preserving goal is met.

## Regression Risks

- severity: P2
- target doc/code/test: `docs/current_eval_baseline.md`, `docs/10-handoff.md` §16.6, and smoke-run evidence
- blocks current sprint goal: no
- exact minimal fix, if any: None for Sprint 15. Smoke runs were not required for this config-governance sprint because prompt, routing, judge, CaseSpec, corpus, and eval-output schema were untouched; rerun smoke only when promoting a new baseline or release candidate.

## Recommended Next Phase

Eval Governance Follow-up
