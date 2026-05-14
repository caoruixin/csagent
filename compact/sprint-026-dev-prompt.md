Paste the content below this line into a fresh Claude Code session on branch `design-v1-without-human-review`. Working tree at session-start carries deliver-agent-owned files + 3 pre-existing unrelated mods — see §11.

---

You are the **Sprint 26 dev agent**, running on the
`design-v1-without-human-review` branch. Sprint 26 is the
latency-decision sprint that consumes Sprint 25's per-LLM-call
instrumentation. Read the data; choose one of five decision outcomes;
ship a decision document; bundle the implementation only if the chosen
decision implies action. The decision IS the primary deliverable;
implementation is conditional.

## 1. Load order (cold-start canonical chain)

1. `AGENTS.md` (loads `docs/current/iteration_governance.md` §1 §3 §5 §7
   + `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md`).
2. `docs/sprint_objective.md` (Sprint 26 contract).
3. `docs/sprints/sprint-024-handoff.md` §"Outcome" + §3 (coalesce + honest-next-step).
4. `docs/sprints/sprint-025-handoff.md` §4 / §5 / §6 (synthetic baseline,
   worked-example comparison, methodology reconciliation).
5. `docs/action_bank.md` line 616 + line 652 (Sprint 25 close entries).

Do NOT load Sprint 18–23 archives unless analysis requires it.

## 2. Reproducibility rule (HARD)

Every quantitative claim (latency, percentile, pass-rate, cost) MUST
cite source path AND extraction command: `jq` filter, `python3 -c`,
`psql` heredoc, or literal phrase "manual eyeball over `<path>`". A
claim without both is not landable. Sprint 25 fix iteration commit
`c8b8c85` + `docs/sprints/sprint-025-fix-codex-review.md` pass set the
bar. Codex flags any unreproducible claim as BLOCKING.

## 3. Decision options

Land on exactly one of:

- **(A) Widen deadline budget.**
  `server/src/main/java/com/gumtree/csagent/controller/ChatController.java:41`
  (`USER_FACING_LLM_DEADLINE_MS = 30_000L`; callsites lines 74 + 109).
  Sister constants:
  `server/src/main/java/com/gumtree/csagent/service/llm/OpenAiCompatibleLlmClient.java:59–60`
  (`connectTimeout`, `readTimeout`) + line 115
  (`MIN_BUDGET_MS_FOR_NEXT_ATTEMPT`). Frontend axios:
  `ui/src/api/client.ts`.
- **(B) Revert to pre-`f2d4cb2` model.** Pre-`f2d4cb2` primary was
  `kimi-k2.6`. Touches `LlmClientConfig.java` bean wiring,
  `LlmProperties.java` defaults, `application-local.yml`
  `DEEPSEEK_MODEL` env override, `LlmConfigValidator.java` fatal-key
  flip.
- **(C) Accept current latency.** No code / config change. Explain why
  +3.3s p95 widening is acceptable given Sprint 24's coalesce UX.
- **(D) Change retry / backoff.** `OpenAiCompatibleLlmClient.java:130–215`
  (fixed 200ms sleep at 189 / 208; maxAttempts from
  `LlmCallContext.remainingAttempts()`) + `FallbackLlmClient.java`.
- **(E) Other.** Name (E) precisely; walk `iteration_governance.md`
  §3.2 to classify layer.

## 4. Decision criteria (handoff §6 must walk all five)

1. **UX with Sprint 24 coalesce.** The question is NOT "do users see
   a stuck bot on consecutive deadlines" — Sprint 24 fixed that. It's
   "is bot completion rate hurt by deadlines cutting calls short".
   Cite from `eval_interactive/results/20260514-111724/results.json`
   `llm_calls` `success==false` rows.
2. **Pass-rate signal.** The 4/14 drop at
   `docs/sprints/sprint-025-handoff.md:394` is **separate from** the
   +3.3s widening. Walk all three hypotheses: latency-driven,
   model-quality-driven, run-to-run variance. Cite evidence per
   hypothesis.
3. **Cost trade-offs.** Sketch wall-clock + provider-quota
   implications; if no usable cost data, say so.
4. **Reversibility.** Each option has different cost to undo.
5. **Downstream compounding.** Handover orchestrator at
   `docs/proposals/handover_orchestrator_design.md`;
   `R-cs040-uc-k-topic-subject-routing` at `docs/action_bank.md:614`.

## 5. "Don't conflate signals" (HARD)

+3.3s chat p95 widening at `sprint-025-handoff.md:354` is the LLM
latency signal. 4/14 pass-rate drop at line 394 is the bot
reliability signal. These are **different**. "Post-`f2d4cb2` is
worse, therefore revert" without evidence segregation is a §1.7
forbidden collapse. Codex flags BLOCKING.

## 6. Sprint 24 coalesce is in place

`BotSession.java:79–81` (`consecutive_deadline_count`) +
`PhaseEvaluator.java:684–799` (DEADLINE_EXCEEDED branch, reset hook,
threshold-gated honest next-step). Second consecutive deadline emits
*"I'm still having trouble responding in time. If you'd like, I can
connect you with a specialist, or you can try again in a few
minutes."*. UX question is post-Sprint-24, not pre.

## 7. "Accept" is a valid outcome

Decision (C) is the honest outcome if data doesn't support action.
"Latency-decision sprint" does NOT pre-suppose action.

## 8. No new instrumentation

If analysis surfaces a data gap, propose a follow-on R-item in
action_bank §5.2 — do NOT add instrumentation. Adding instrumentation
expands scope and inherits Sprint 25's bundle-or-defer dynamic.

## 9. Hard fences (NOT in scope)

1. No new instrumentation in `eval_interactive/`, `server/`, `ui/`.
2. No eval-spec edits — `eval_interactive/case_specs/*`,
   `eval_interactive/case_spec_overrides.yaml`,
   `eval_interactive/personas/*`, judge rubric, judge prompts.
3. No case-family edits — `eval_interactive/case_families/*`
   (Sprint 20 cascade fence).
4. No prompt edits — `server/src/main/resources/prompts/system_prompt.txt`.
5. No Sprint 24-landed code edits — `BotSession.consecutiveDeadlineCount`
   field; `PhaseEvaluator` reset hook + DEADLINE_EXCEEDED branch at
   lines 684–799.
6. No Sprint 25-landed code edits — `executor.py` `llm_calls`
   enrichment; `agent_client.py` `get_llm_calls`;
   `LlmSyntheticBaselineTest.java`.
7. No foundational doc edits — `docs/foundational/*`.
8. No governance doc edits — `docs/current/iteration_governance.md`,
   `doc_governance.md`, `agent_context_guide.md`.
9. No sprint archive edits — `docs/sprints/sprint-001-*` through
   `sprint-025-*.md`.
10. No Tier-0 invariant add / modify.
11. No eval rubric / CaseSpec widening to mask a real bot mistake
    (§1.7 forbidden).

## 10. §1.7 hard gate

- Do NOT widen the eval rubric or any CaseSpec to make a pass-rate
  signal look better.
- Do NOT collapse the +3.3s and 4/14 signals.
- Do NOT cite a number without source path + extraction command.
- Do NOT introduce a keyword / regex / if-else / enum / per-UC matrix
  for a soft semantic decision. None of (A)–(D) require one; if your
  proposed (E) implies one, the layer is `human_review_required`.

## 11. Working tree at session start

Expect deliver-agent-owned + pre-existing unrelated mods uncommitted:

- `docs/sprint_objective.md` (deliver-authored, Sprint 26).
- `compact/sprint-026-{dev,review}-prompt.md` (deliver-owned).
- `compact/sprint-deliver-orchestrator.md` (playbook; may be updated).
- `csagent_system_design_review.md` — pre-existing not-authored mod
  (Sprint 24 / 25 §8 flagged).
- `server/src/main/resources/prompts/system_prompt.txt` —
  pre-existing not-authored mod (source of inherited
  `SystemPromptUserRequestedTiebreakerTest` failure).
- `csagent-solution-_20260514.md` — pre-existing untracked.

Do NOT stage these; do NOT be surprised if the human bundles them at
commit time. If you `git add`, stage ONLY files you authored per §13;
avoid `git add -A` / `git add .`. Sprint 20
packaging-rollforward precedent: deliver-agent files in your commit
are not a blocker, but clean boundary is preferable.

## 12. 12-section handoff contract

Write `docs/sprints/sprint-026-handoff.md` matching Sprint 24 / 25:

1. Context Pack (1.1 docs, 1.2 code paths, 1.3 drift, 1.4 SoT, 1.5
   impl status, 1.6 risks).
2. Sprint-objective recap.
3. Premise re-verification (each of seven §3 points in objective: own
   check confirm or surface discrepancy).
4. Data analysis (latency, pass-rate, cost — each number with source
   + extraction command).
5. Hypothesis walk (three pass-rate hypotheses + latency hypothesis if
   relevant).
6. Decision + rationale (one of (A)–(E); walks five §5 criteria).
7. Open questions for human (n=0 / n=1 surface / n≥2 propose R-item).
8. Files changed (table; "none" if zero).
9. Layer-classification self-walk (§3 first-match-wins post-hoc).
10. Anti-hardcode self-walk (§4.1 9 questions).
11. Generalization-coverage table (target / neighbor / negative /
    shadow per §5).
12. Sprint-objective-met check (per-bullet against
    `docs/sprint_objective.md` §12). `closure_verdict` row populated
    post-Codex-review (placeholder OK; human + deliver-agent fill on
    close).

## 13. Deliverables (Files you author)

- `docs/sprints/sprint-026-handoff.md` (new) — 12-section.
- If bundled action: the config / code file(s) + a regression test.
- `docs/action_bank.md` §5.2 — append any new R-items the analysis
  proposes; close any R-items the decision resolves.

Do NOT author: `docs/codex-findings.md` (Codex writes it);
`docs/10-handoff.md` (deliver-agent refreshes at close);
`docs/sprints/sprint-026-objective.md` (deliver-agent archives at
close).

## 14. Stop conditions

- Any temptation to revert the model without evidence of (a) p95
  widening BEYOND the +3.3s shown AND (b) pass-rate drop being
  model-quality-attributable — STOP and reconsider.
- Any temptation to widen the deadline budget reflexively without
  citing the per-call `success==false`-flagged rate — STOP and
  reconsider.
- Any temptation to act on a quantitative claim that is not
  reproducible — STOP. Replace with the reproducible recipe or
  withdraw the claim.
- Any temptation to add instrumentation — STOP. Propose an R-item.
- Any temptation to touch Sprint 24 / 25-landed code — STOP. Hard
  fences #5 and #6.
- Any temptation to collapse the +3.3s and 4/14 signals — STOP.
- Any temptation to spawn another agent / sub-agent — STOP.

## 15. Commit-at-end heads-up

The human commits at session end on the local branch. Run your full
analysis, write the handoff + any in-scope code changes + the
action_bank append, and leave the working tree clean enough that the
human can review and commit.
