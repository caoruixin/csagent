Paste the content below this line into a fresh Claude Code session on branch `design-v1-without-human-review`. Working tree at session-start carries deliver-agent-owned files — see §11.

---

# Sprint 25 dev prompt — Per-LLM-call latency instrumentation

You are the **dev agent** for Sprint 25. The sprint implements
`R-per-llm-call-latency-instrumentation` per Sprint 24 §4.5
acceptance bar. Layer: `infra` / eval-harness. Single-track,
single-R-item, bundled (Java + Python may both touch). Sprint 25
PRODUCES the instrument; it does NOT act on the data.

## 1. Loader stanza — load in this order on a cold start

1. `AGENTS.md` (repo constitution chain entry).
2. `docs/current/doc_governance.md` (tier model + source-of-truth rules).
3. `docs/current/agent_context_guide.md` (per-task reading lists +
   Context Pack Prompt).
4. `docs/current/iteration_governance.md` §1 (Constitution), §3
   (Fix Layer Classification), §5 (Eval Acceptance Rules), §7
   (sprint-objective stanza).
5. `docs/sprint_objective.md` (this sprint's authoritative scope —
   read end-to-end before any edit).
6. `docs/sprints/sprint-024-handoff.md` §4 (Track B coarse-proxy
   investigation), §4.3 (`LlmInvocationService.java:116` timing
   emit reference — note line drift, see §3 below), §4.5 (the
   R-item proposal you are implementing), §10 (open questions
   including Q1 methodology), §11 (action-bank deltas).
7. `docs/action_bank.md:616` (the
   `R-per-llm-call-latency-instrumentation` row — full disposition
   text).

Produce the Context Pack per `agent_context_guide.md` BEFORE any
edit.

## 2. Reproducibility rule — bake this in from minute one

Per
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_deliver_agent_cited_numbers_must_be_reproducible.md`
(the deliver-agent's own discipline; you inherit it for Sprint 25):

Every quantitative claim in your handoff (latency numbers,
percentiles, sample counts, overhead deltas) MUST cite:

1. The exact source path.
2. The exact extraction command (literal `jq` / `psql -c "..."` /
   `python -c "..."` / "manual eyeball" if no scripted extraction).
3. The aggregation window if multiple plausible ones exist (per-
   case, per-turn, per-LLM-call).
4. The derivation of any n-value.

A claim missing 1–4 is methodologically unsupported; Codex blocks
on it. Sprint 25's whole deliverable is reproducible methodology,
so the bar is non-negotiable for your own numbers too.

## 3. Premise verification before any code edit

Re-verify these facts in your context pack (deliver agent did
this at planning turn; you re-check because the working tree
may have moved):

- `LlmInvocationService.invokeChat` at
  `server/src/main/java/com/gumtree/csagent/service/runtime/LlmInvocationService.java`
  92–160. `elapsed` at line 110, log emit at 111, `llmCallLogger.log(...)`
  at 114. (Sprint 24 §4.3 cites `:116`; actual is 111/114 — minor
  drift, not material.)
- `LlmCallLogger.log` / `logFailure` at
  `server/src/main/java/com/gumtree/csagent/service/observability/LlmCallLogger.java:37/71`,
  both persist `latency_ms` to `llm_call_log`.
- `V9__create_llm_call_log.sql` defines the table with
  `latency_ms INT NOT NULL` + index on `(session_id, turn_index)`.
  Per-LLM-call latency is ALREADY in the DB; gap is surfacing it
  into `results.json`.
- `results.json` schema (per
  `eval_interactive/results/20260514-080835/results.json`):
  top-level `run_id`/`label`/`timestamp`/`elapsed_ms`/`summary`/`case_results`;
  per-case includes `transcript` of `{turn_index, role, message, source}`
  with no per-turn duration. Writer at
  `eval_interactive/eval_interactive/batch/executor.py` `_build_case_result`
  (line 282).

If anything above has drifted, STOP and report.

## 4. Scope — walk both options before committing

Per `docs/sprint_objective.md` §4. Walk BOTH before picking one;
record the rejected alternative + justification in handoff §3.

- **Option A — Java-side:** capture per-call duration in
  `LlmInvocationService.invokeChat` and thread it through
  `AgentRunLoop`/`SessionRunner`/`TraceCollector` to `results.json`.
- **Option B — Writer-side:** surface the existing
  `LlmCallLogger`-emitted timing (already in `llm_call_log` per V9)
  into a per-turn field on `results.json`. Eval-harness change at
  `eval_interactive/eval_interactive/batch/executor.py` `_build_case_result`.
  May require a new DB read path or bot-side endpoint exposing
  `llm_call_log` rows by session.

Pick the option with lower blast radius and clearer reproducibility.

## 5. Synthetic baseline design

Per `docs/sprint_objective.md` §5. Constraints:

- Fixed prompt (single deterministic prompt; not a runtime prompt).
- No tool dispatch.
- No persistence overhead (or measure and subtract the delta).
- Reproducible: exact command + source path; n ≥ 30 per percentile.
- Implementation location is your choice (JUnit benchmark / Python
  script / Python module). Record + justify in handoff §4.

## 6. Pre-`f2d4cb2` comparison conditional

Per `docs/sprint_objective.md` §6. Verify whether the local DB
retains pre-`f2d4cb2` `llm_call_log` rows. If yes, compare. If
no, current-model baseline is new ground truth (absolute, not
delta). Record the path taken + verification evidence in handoff
§5.

## 7. Methodology reconciliation (Sprint 24 §10 Q1)

Per `docs/sprint_objective.md` §7. Investigate where the planning-
turn numbers (pre p95≈24.6s / post p95≈27.6s / n="105+27 case-
turns") came from. Walk hypotheses 1–4 in §7. Record the
determined source (or honest "source unknown / cannot be
reconstructed") in handoff §6. This is NOT a blocker for the
sprint — it is methodology honesty.

## 8. Files in scope (preliminary; refines during your option walk)

See `docs/sprint_objective.md` §8 for the preliminary per-option
file list. The actual files you touch are recorded in handoff §8.

## 9. Hard fences — these are blocking Codex findings if violated

Per `docs/sprint_objective.md` §12. Violating ANY of these means
Codex will block:

1. No deadline-budget widening (no edits to deadline / timeout
   config in `application.yml` / `application*.properties` / `LlmProperties.java` /
   anywhere else).
2. No model config change / model revert (no edits to
   `LlmProperties.java` model name fields or
   `application*.yml` model selection).
3. No `prompt_projection` work (no new projected slot, no signal
   surface change, no prompt edit).
4. No eval-spec edits (no `eval_interactive/case_specs/**`, no
   `case_spec_overrides.yaml`, no personas, no judge rubric).
5. No Tier-0 changes.
6. No edits to Sprint 24-landed code:
   `BotSession.consecutiveDeadlineCount` (line 81 area);
   `SessionManager.consecutiveDeadlineCount(0)` builder line (108);
   `PhaseEvaluator` reset hook (684–693); `PhaseEvaluator`
   `DEADLINE_EXCEEDED` branch (765–803); `PhaseEvaluator`
   `LLM_UNAVAILABLE` branch (804–817);
   `V13__add_consecutive_deadline_count.sql`;
   `Sprint24DeadlinePlaceholderCoalesceTest.java`.
7. No edits to Sprint 23-landed `system_prompt.txt` teaching
   paragraph.
8. No edits to `AlreadyCalledPromptConsumptionTest.java`.
9. Every quantitative claim in the handoff cites source path +
   extraction command per §2 above. Unsupported quantitative
   claims = blocking Codex finding.
10. The smoke rerun MUST use real LLM calls (not mocked). Per
   `.claude/agent-memory/sprint-deliver-orchestrator/feedback_mocked_llm_cannot_prove_prompt_causal_change.md`:
   a mock controls the variable being measured; mocked-LLM
   latency measures the mock, not the LLM.

§1.7 hard gate: no semantic surface touched; no rubric widening;
no Sprint 24-landed code modified. If you find yourself wanting
to touch any of these to fix correctly, STOP and add a new
R-item to `docs/action_bank.md` §5.2 (deferred) and report via
handoff §7.

## 10. Handoff doc contract — 12 sections, write to `docs/sprints/sprint-025-handoff.md`

1. **Context Pack** (relevant docs + code paths + doc-status
   warnings + source-of-truth decision + implementation-status +
   risks).
2. **Sprint-objective recap.**
3. **Option chosen** (A or B) + rejected alternative + justification.
4. **Synthetic baseline design** (location + command + sample
   output + n).
5. **Pre-`f2d4cb2` comparison path** (delta vs absolute) + DB
   verification evidence.
6. **Methodology reconciliation** (Sprint 24 §10 Q1 — determined
   source or honest "unknown").
7. **Open questions for human** (anything that needed a decision
   and didn't have one; honest unknowns; deferred R-items added
   to action_bank §5.2).
8. **Files changed** (path + change + line range).
9. **Layer-classification self-walk** (§3.2 first-match-wins on
   your bundle; should resolve to `infra`).
10. **Anti-hardcode self-walk** (§4.1 nine questions; should pass
    cleanly because no semantic decision moves).
11. **Generalization-coverage table** (target / neighbor /
    negative / shadow per `docs/sprint_objective.md` §11).
12. **Sprint-objective-met check** (per-bullet PASS / PARTIAL /
    GAP against `docs/sprint_objective.md` §9 success metrics).
    The closure_verdict row is a placeholder; the deliver agent
    fills it on close per
    `.claude/agent-memory/sprint-deliver-orchestrator/feedback_handoff_verdict_section_delegation.md`.

## 11. Working tree at session start — DO NOT stage these yourself

Per
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_commit_at_end_bundles_deliver_artefacts.md`,
your working tree at session start carries uncommitted
deliver-agent-owned files: `docs/sprint_objective.md`,
`compact/sprint-025-dev-prompt.md`, `compact/sprint-025-review-prompt.md`,
possibly `compact/sprint-deliver-orchestrator.md`, plus pre-existing
mods (`csagent_system_design_review.md`,
`server/src/main/resources/prompts/system_prompt.txt`) which are
NOT yours.

**Do not stage these yourself.** Stage only files YOU authored.
Avoid `git add -A` / `git add .` for your commit. If the human
bundles deliver-agent files at commit time that is OK.

## 12. Stop conditions

STOP and report instead of improvising if:

- You feel tempted to widen the deadline budget or revert the model.
- You feel tempted to act on the latency data (this is a future
  sprint).
- You need to touch Sprint 24-landed code to instrument correctly
  (the sprint shape is wrong; the deliver agent will rescope).
- You need to touch eval-spec / `case_specs/` / personas / judge
  rubric.
- You need to touch a Tier-0 surface.
- You produce a quantitative claim that you cannot back with
  source path + extraction command.
- Premise verification finds the cited file paths / line ranges
  have drifted materially.
- You find yourself wanting to spawn another Agent via tool.

In each STOP case, report via handoff §7 with the gap, the
proposed deferred R-item, and the recommendation to the deliver
agent.

## 13. Sprint close

When done, the deliver agent reads handoff §12 (Sprint-objective-
met check), Codex's review at `docs/codex-findings.md`, and
classifies A / B / C / D per
`docs/current/iteration_governance.md` §4.2 + deliver-agent role
brief §4. You do not run the close yourself.
