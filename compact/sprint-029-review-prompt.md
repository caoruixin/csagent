Paste the content below this line into a fresh Codex session after the dev's commits land. No PR will be opened; review the commit range against `main`.

---

# Sprint 29 review-agent prompt — (R2) probe follow-on (CaseSpec authoring + phase-derivation verification)

## 1. Loader stanza

Read in order before any verdict:

1. `AGENTS.md`.
2. `docs/current/doc_governance.md`.
3. `docs/current/agent_context_guide.md`.
4. `docs/current/iteration_governance.md` §1 + §3 + §4 + §5 + §7. **Sprint 29 is NOT exempt** (semantic-touching: `eval_spec` authoring).
5. `docs/sprint_objective.md` (Sprint 29 scope; particularly §6 / §7 / §9 / §11 / §12).
6. `docs/sprints/sprint-029-handoff.md` (dev handoff).
7. `docs/sprints/sprint-018-handoff.md` §5.1 + §8.7 + §8.8 (Method note, n=1 rule, conditional broadening).
8. `docs/sprints/sprint-020-handoff.md` §5 (G2 precedent).
9. `docs/sprints/sprint-027-handoff.md` §6 (probe scope).
10. `docs/sprints/sprint-028-handoff.md` §3 + §5 + §7 (per_turn_trace schema; 2 defensive shapes; named-but-not-opened follow-on).

## 2. Anti-Hardcode kernel

Run the §4.1 kernel verbatim as loaded from `iteration_governance.md`. **Sprint 29 is NOT exempt** (semantic-touching: `eval_spec` authoring).

Sprint-29 Q8 deviation: target = N≥3 cases across D485.2 / D564.7 / D616.2; neighbor + negative OPTIONAL (Sprint 20 G2 precedent; Sprint 29 is a probe); shadow NOT required (future structural sprint authors shadow). Accept deferral when called out in handoff §4 / §11.

## 3. Sprint-29 BLOCKING criteria

BLOCKING findings (use `decision: fix_required`):

### 3.1 Hard-fence violations (§9 of sprint_objective)

Any edit to: Sprint 20 case families (`cs001_*`..`cs259_*`, `manual_probe_*`); Sprint 23–28-landed code under `server/` or `eval_interactive/eval_interactive/`; `system_prompt.txt`; rubric widening; `docs/foundational/`, `docs/current/`, `docs/sprints/sprint-001-*`..`sprint-028-*`; Tier-0; deadline/model/retry/budget config; cs_040 UC-K→UC-C routing surface.

### 3.2 §1.7 violations in Track A authoring

- Trace-specific text in `hidden_facts` (copying bot transcript verbatim).
- Keyword/regex/if-else in `expected.*` fields.
- CaseSpec id mirrored into runtime/prompt/judge code.
- Rubric widening to accept a bot mistake (the failure IS the finding).
- L3 override authored for new cases.

### 3.3 Mocked-LLM as primary evidence

If Track A's directive-followship measurement uses mocked-LLM as primary evidence, flag BLOCKING. Mocked-LLM acceptable only as supporting coverage with explicit label. Real-LLM smoke is the evidence gate.

### 3.4 Reproducibility violations

Every quantitative claim must cite source path + extraction command. Unsourced numbers in §3 / §5 / §6 / §7 are BLOCKING.

### 3.5 Track B downgrade discipline

If Track B failed mid-sprint:
- §6.2/§6.3 must document failure mode + reproducible extraction + proposed follow-on R-item `R-per-turn-phase-transition-dump-for-smoke-harness`.
- §6 must NOT show retried verification with modified scope.
- R-item is **named only**, NOT opened.
- Track A's CaseSpecs must still land.

If Track B succeeded: §6.1 must answer all four Q's with reproducible extraction; no follow-on R-item proposed.

### 3.6 R-item discipline

`R-prompt-phase-plan-directive-followship` at `docs/action_bank.md:450` must be **updated**, NOT closed. Closure = BLOCKING.

### 3.7 cs_029 third defensive shape

Track B derivation must defensively skip a turn with NULL `phase_plan` — return "phase unknown" rather than throw. Crash/exception on cs_029 shape = BLOCKING.

### 3.8 Scope expansion

Diff includes files outside §9 = BLOCKING; identify the file. Per `feedback_out_of_scope_review_packaging_rollforward.md`: deliver-agent-owned files (`docs/sprint_objective.md`, `compact/sprint-029-*`) in the commit due to commit-at-end workflow are packaging-rollforward, NOT substantive blockers. Substantive scope-creep = BLOCKING; pure packaging = non-blocking.

## 4. Sprint-close header

Write at the top of `docs/codex-findings.md`:

```
## Sprint 29 Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

If `decision: pass`: substantive content sound; sprint may close.
If `decision: fix_required`: name each blocking finding with file + line + the §3 rule it violates.
If `decision: out_of_scope_review`: name the items broadened outside scope; for packaging-only out-of-scope (deliver-agent files in the commit), say so explicitly — those are non-blocking per the packaging-rollforward rule.

## 5. Deferral discipline

Out-of-scope concerns surfaced during review (dev observations → future R-items, directive-followship findings beyond the 3 target directives, cs_029 root-cause notes) belong in `docs/action_bank.md` as deferred — they are NOT blockers on Sprint 29.

Sprint 29 ships exactly: (i) N≥3 authored CaseSpecs probing D485.2 / D564.7 / D616.2; (ii) Track B (c) phase-derivation verdict; (iii) R-item disposition update at line 450. Anything else = deferral or scope-creep.
