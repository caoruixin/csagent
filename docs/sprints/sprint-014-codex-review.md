## Sprint Review Decision

decision: pass
blocking_count: 0
summary: Sprint 14.1 closes the previous persistence blocker: `SourceEvidenceLineage`, `FaqOutputClass`, and `FaqGroundingDiagnostics` are computed before `BotTurn` save and persisted as snake_case fields under `bot_turns.projected_context.faq_grounding`. Citation extraction remains passive and non-blocking, and the diff stays narrow with no hard citation gate, DB migration, broad framework, corpus change, CaseSpec/judge/prompt/routing change, or escalation-enum change.

## Blocking Sprint Failures

None.

## Non-Blocking Notes

- severity: P2
- target doc/code/test: `docs/10-handoff.md` §11/§12 and `docs/action_bank.md` §3
- blocks current sprint goal: no
- exact minimal fix, if any: After this Codex pass, clean up the few pre-review phrases that still read as premature closure, such as "Sprint 14 closed" / "combined Sprint 14 + 14.1 objective is met", and update the older §12 test-count line from 854 to the Sprint 14.1 count of 859. The current phase headers correctly say Sprint 14.1 is awaiting Codex re-review, so this is documentation polish rather than a blocker.

- severity: P2
- target doc/code/test: `docs/faq_grounding_contract.md` §1 and §5
- blocks current sprint goal: no
- exact minimal fix, if any: Optional clarity only: add the durable trace surface to the §1 term table alongside the `BotSession` transient slots. §5 already accurately describes `bot_turns.projected_context.faq_grounding` as the durable surface.

## Regression Risks

- severity: P2
- target doc/code/test: `server/src/test/java/com/gumtree/csagent/integration/Sprint141FaqGroundingTracePersistenceTest.java`
- blocks current sprint goal: no
- exact minimal fix, if any: Optional hardening only: add a true repository save/reload test if a future DB-backed trace regression appears. The current test captures the exact `BotTurn.projectedContext` value handed to `BotTurnRepository.save(...)`, and `projected_context` is already the durable JSONB column.

- severity: P2
- target doc/code/test: `mvn -pl server test -Dtest='Sprint14*,Sprint141*'`, focused Sprint 6/7/8/10/11/12/13 regression command, and `mvn -pl server test`
- blocks current sprint goal: no
- exact minimal fix, if any: None. I reran the focused Sprint 14/14.1 suite (38/0/0/0), the named regression guard suite (269/0/0/0), and the full server test suite (859/0/0/0); the existing FAQ S1 guard and Sprint 6 ReadTimeout no-retry closure remain green.

## Recommended Next Phase

Script / Policy Config Governance Sprint
