# Sprint 24 Review Prompt — Slow-LLM Placeholder Coalesce + Coarse Latency Proxy

Paste the content below this line into a fresh Codex session after the dev's commits land. No PR will be opened; review the commit range.

---

You are the Anti-Hardcode Review Agent on Sprint 24, a two-track sprint:

- **Track A:** narrow `infra` UX repair — session-scope `consecutive_deadline_count` on `BotSession` + `PhaseEvaluator` DEADLINE_EXCEEDED branch + distinct honest next-step on 2nd consecutive deadline + regression test.
- **Track B:** investigation-only coarse-proxy latency baseline + follow-on R-item proposal. No code shipped on Track B.

UX-over-pass-rate framing: primary success bar is the user-visible behaviour repair, not pass-rate movement.

## 1. Loader stanza

Read in this order:

1. `AGENTS.md`.
2. `docs/current/doc_governance.md`.
3. `docs/current/agent_context_guide.md` (Context Pack Prompt).
4. `docs/current/iteration_governance.md` §1, §3, §4.1, §4.2, §5, §7.
5. `docs/sprint_objective.md` — Sprint 24 scope (authoritative; the six hard fences in §6.2 and the §10 NOT-in-scope list are load-bearing for this review).
6. `docs/sprints/sprint-024-handoff.md` — dev's handoff.
7. The dev's commit range on `design-v1-without-human-review` (no PR).
8. `docs/action_bank.md:612-614` (the three R-items Sprint 24 closes / reframes).

## 2. §4.1 kernel — execution

Run the §4.1 kernel verbatim as loaded from `iteration_governance.md`. Sprint 24 is NOT exempt (semantic-touching). Sprint-24-specific deviations:

- **Q1:** Track A introduces a *threshold check* on a session-scope event-shape counter (`consecutive_deadline_count >= 2`). This is NOT a semantic hardcode — trigger is event shape, not user content. Verify dev did NOT add keyword/regex/if-else on user words or UC.
- **Q8:** Track A is behaviour-level (regression test), not CaseSpec-id-level. Accept when handoff §8 shows: target = regression test passes; neighbor = full server suite green; negative = reset-behaviour assertion; shadow = deferred to G2. Track B is exempt from §5.1 (diagnostic).

Other 7 questions apply normally.

## 3. Sprint-24-specific context layer

### 3.1 Track A bundled-fix expectations

Dev must have shipped: `BotSession.java` new `consecutive_deadline_count` field (mirror `runtime_error_count` lines 75–77); `SessionManager.java` init at line 107 mirror; `PhaseEvaluator.java` reset hook in outcome-dispatch prologue (mirror lines 675–682 ERROR-counter reset) + increment + threshold check + distinct honest message in DEADLINE_EXCEEDED branch (today lines 754–770); database migration if needed; regression test asserting (1) 1st deadline → existing placeholder text, (2) 2nd consecutive deadline → distinct honest next-step text (inequality + next-step intent), (3) reset behaviour. Missing piece = fix_required.

### 3.2 Track B investigation-only expectations

Handoff Track B section must contain ALL of: coarse-proxy data table (pre/post p50 + p95 with case-turn n); observed p95 increase reported (≈24.6s → ≈27.6s); explicit **no-per-call-claim paragraph**; follow-on R-item proposal (`R-per-llm-call-latency-instrumentation` or equivalent), `infra` / eval-harness, flagged as prerequisite to any future deadline-budget or model-revert decision. Missing piece = fix_required. If Track B shipped code (any commit touching runtime / eval / config), fix_required.

### 3.3 §1.7 guardrails

The 2nd-deadline message MUST: name what's happening; offer an actionable next step; NOT repeat the placeholder byte-for-byte; NOT branch on user content (no regex/keyword/if-else/enum/per-UC matrix); NOT contain visible-eval CaseSpec text. Keyword/regex/per-UC branch = **reject as semantic hardcode**. Merely repeating the placeholder (no differentiation) = fix_required (UX bar not met).

### 3.4 Hard fences + out-of-scope checks

Six hard fences in `docs/sprint_objective.md` §6.2 apply verbatim; any violation in diff or handoff = fix_required. `ChatController.java:125` and `cs_040` UC-K→UC-C routing are out-of-scope (§6.1 / §10); if dev edited either OR silently merged surfaces, fix_required. If handoff fails to name `ChatController.java:125` as out-of-scope, informational only. If handoff conflates the two emission surfaces, fix_required.

### 3.5 Track B per-call claim violation (blocking)

If handoff represents coarse-proxy data (`elapsed_ms`, total turns) as **per-LLM-call latency evidence**, fix_required (fence #6). Handoff must use language like *"signal, not evidence"* / *"case-level `elapsed_ms` conflates round-trip with dispatch / persistence / retry"* / *"per-call latency requires future instrumentation"*. Absence of this honesty paragraph = fix_required.

### 3.6 Generalization coverage

Track A coverage in handoff §8 must follow `sprint_objective.md` §5.1 — target / neighbor / negative / shadow. Track B is diagnostic (no §5.1 bars).

## 4. Packaging-rollforward rule

Deliver-agent-owned files in the close commit are NOT scope drift. If the commit bundles `docs/sprint_objective.md`, `compact/sprint-024-*.md`, `compact/sprint-deliver-orchestrator.md`, `csagent_system_design_review.md`, or `server/.../prompts/system_prompt.txt` (last two were pre-existing working-tree mods NOT from Sprint 24 dev), do NOT classify as scope drift. Substantive surface is runtime / test diff + handoff. If the only blocking finding would be a packaging observation while substantive findings closed, decision = `pass` not `out_of_scope_review`; state packaging observation as informational.

## 5. Deferral rule

Out-of-scope concerns go to `docs/action_bank.md` via deliver agent on close, NOT to blocking findings. NOT blocking: `LLM_UNAVAILABLE` not getting the same coalesce treatment; per-LLM-call latency instrumentation deferred to the proposed follow-on R-item.

## 6. Sprint-close header

Write to `docs/codex-findings.md` with §4.2 format:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

Then per-finding detail. Decision applies to Sprint 24 as a whole; tracks not graded independently. If Track A passes but Track B violates fence #6, sprint decision = fix_required (single blocking finding on Track B).
