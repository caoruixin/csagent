Paste the content below this line into a fresh Codex session after the dev's commits land. No PR will be opened; review the commit range against `main` on branch `design-v1-without-human-review`.

---

You are the **Sprint 26 review agent (Codex)**. Sprint 26 is the
latency-decision sprint that consumes Sprint 25's per-LLM-call
instrumentation. The dev agent has produced a decision document
(handoff) + optional bundled implementation. Review the commit range
and write the sprint-close header to `docs/codex-findings.md`.

## 1. Load order

1. `AGENTS.md` (loads `iteration_governance.md` §1 §3 §4 §5 §7 +
   `doc_governance.md` + `agent_context_guide.md`).
2. `docs/sprint_objective.md` (Sprint 26 contract; §11 stanza is
   multi-layer prospective per-decision-outcome; §6 has eleven hard
   fences).
3. `docs/sprints/sprint-026-handoff.md`.
4. Dev's commit range: `git log --oneline c689274..HEAD` (Sprint 25
   close = `c689274`); `git diff c689274..HEAD`.

## 2. Run §4.1 Anti-Hardcode kernel

Run the §4.1 kernel verbatim as loaded from `iteration_governance.md`.
Sprint 26 is **NOT exempt** if the dev bundled an implementation
under (A) / (B) / (D) / (E); Sprint 26 IS exempt only if decision is
(C) accept (no code / config change). State exemption status
explicitly.

Sprint-specific deviations:

- **Q8 (target/neighbor/negative/shadow):** Sprint 26 defers
  negative + shadow to G2 — accept when called out in handoff §11.
- **Q9 (sunset plan):** (A) / (D) config constants; (B) bean-wiring;
  (C) no-change. Accept "reversible by config edit" as sufficient.

## 3. Sprint-26-specific checks (in addition to §4.1)

- **Decision is named explicitly.** The handoff §6 MUST land on
  exactly one of (A) widen budget / (B) revert model / (C) accept /
  (D) change retry-backoff / (E) other. If the handoff is ambiguous
  or hedges between options, that is BLOCKING.
- **Reproducibility bar.** Every quantitative claim (latency,
  percentile, pass-rate, cost) MUST cite source path AND extraction
  command. A claim without both is BLOCKING. This is the bar Sprint
  25's fix iteration (`c8b8c85` →
  `docs/sprints/sprint-025-fix-codex-review.md` pass) established.
- **Signal-conflation check.** The +3.3s chat p95 widening
  (`sprint-025-handoff.md:354`) and the 4/14 pass-rate drop
  (`sprint-025-handoff.md:394`) are SEPARATE signals. If the
  handoff's rationale collapses them ("post-`f2d4cb2` is worse
  therefore revert") without evidence segregation, that is BLOCKING
  per §1.7.
- **Eleven hard-fence violations are BLOCKING.** Sprint 26 must not
  touch:
  1. Any new instrumentation in `eval_interactive/`, `server/`, or
     `ui/` beyond the decision-implied edit.
  2. Eval-spec surfaces (`eval_interactive/case_specs/*`,
     `eval_interactive/case_spec_overrides.yaml`, `personas/*`, judge
     rubric).
  3. Case-family surfaces (`eval_interactive/case_families/*`).
  4. Prompt surfaces (`server/src/main/resources/prompts/system_prompt.txt`).
  5. Sprint 24-landed code (`BotSession.consecutiveDeadlineCount`,
     `PhaseEvaluator` reset hook + DEADLINE_EXCEEDED branch).
  6. Sprint 25-landed code (`executor.py` `llm_calls`, `agent_client.py`
     `get_llm_calls`, `LlmSyntheticBaselineTest.java`).
  7. `docs/foundational/*`.
  8. `docs/current/iteration_governance.md` / `doc_governance.md` /
     `agent_context_guide.md`.
  9. `docs/sprints/sprint-001-*` through `docs/sprints/sprint-025-*.md`.
  10. Any Tier-0 invariant add / modify.
  11. Eval rubric / CaseSpec widening to mask a real bot mistake.
- **"Accept" is valid.** If decision is (C), the handoff is the only
  deliverable. Do NOT flag the absence of a code change as
  scope-incomplete; (C) was named as a valid outcome.
- **If bundled action:** a regression test demonstrating the changed
  behaviour is required. If missing, that is BLOCKING.

## 4. Packaging-rollforward rule

If your only blocking finding is path-based (deliver-agent-owned
files like `docs/sprint_objective.md`, `compact/sprint-026-*.md`,
`compact/sprint-deliver-orchestrator.md` bundled into the dev's
commit), and the substantive findings closed per your own evidence,
classify the substantive verdict honestly and note the packaging
artefact for the deliver-agent's close handoff to address. The
human's classification will be A-with-packaging-note (Sprint 20
precedent). Do NOT split this into a separate re-review round on
packaging grounds alone.

## 5. Sprint-close header (write to `docs/codex-findings.md`)

Write the §4.2 sprint-close header at the top of
`docs/codex-findings.md`:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

Below the header, list findings (blocking + non-blocking) with file
paths and line citations. For each finding, name the §3 fix-layer
classification it points to (`infra` / `prompt_projection` /
`semantic_planner` / etc.) — do not propose a code fix beyond naming
the layer.

## 6. Deferral-to-action_bank rule

Out-of-scope concerns the diff surfaces (issues genuinely outside
Sprint 26's eleven hard fences but not in the dev's handoff) go to
**non-blocking** findings with a recommendation that the deliver
agent / human append them to `docs/action_bank.md` §5.2 as deferred
R-items. Do NOT escalate them to BLOCKING.

## 7. Verdict set

Per §4.1, exactly one verdict on the diff:

- `approve` — clean, or justified Tier-0 protection with
  generalization coverage.
- `approve with downgrade-to-signal follow-up` — interim acceptable;
  name the trigger.
- `reject as semantic hardcode` — Q1 + Q2 fail or Q5 / Q6 fail with
  no Tier-0 claim.
- `needs human architecture decision` — boundary question (new
  Tier-0 candidate, LLM-vs-Java shift, new tool surface).
