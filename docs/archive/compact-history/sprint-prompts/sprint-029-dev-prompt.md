Paste the content below this line into a fresh Claude Code session on branch `design-v1-without-human-review`. Working tree at session-start carries deliver-agent-owned files + 3 pre-existing unrelated mods — see §11.

---

# Sprint 29 dev-agent prompt — (R2) probe follow-on (Track A CaseSpec authoring + Track B phase-derivation verification)

## 1. Loader stanza

Read in order before any plan or diff:

1. `AGENTS.md` (constitution chain).
2. `docs/current/doc_governance.md` (tier model, source-of-truth rules).
3. `docs/current/agent_context_guide.md` (per-task reading lists).
4. `docs/current/iteration_governance.md` §1 + §3 + §5 + §7.
5. `docs/sprint_objective.md` (Sprint 29 authoritative scope).
6. `docs/sprints/sprint-018-handoff.md` §5.1 Method note + §8.7 + §8.8.
7. `docs/sprints/sprint-020-handoff.md` §5 + `eval_interactive/case_specs/case_families/_manifest.yaml` (case-family precedent, `synthetic-sprint20-*` naming).
8. `docs/sprints/sprint-027-handoff.md` §6 + §11 (probe scope).
9. `docs/sprints/sprint-028-handoff.md` §3 + §5 + §7 (per_turn_trace schema, 2 defensive shapes, named-but-not-opened follow-on R-item).
10. Verify §4 premise items in `docs/sprint_objective.md` at session start; confirm, do NOT re-derive.

## 2. Sprint class

Sprint 29 is a **semantic-touching investigation+bundle, two-track** sprint. §7 stanza required; multi-layer prospective per-track.

- **Track A — `eval_spec`-layer CaseSpec authoring.** Allowed per §3.2 Q6 + Sprint 20 G2 precedent. NOT widening a rubric; authoring cases that exercise behaviour the runtime can and should perform.
- **Track B — `infra`-layer verification.** No code; post-hoc extraction on Track A's smoke output. If verification fails mid-sprint, downgrade fires (sprint_objective §7.2) — propose `R-per-turn-phase-transition-dump-for-smoke-harness` for a future sprint; do NOT retry.

## 3. Track A scope — author N≥3 CaseSpecs

Three target directives (D517.2 OUT of scope; tautology per Sprint 27 §6):

- **D485.2** CLOSE record_outcome-if-not-already-recorded. 1–2 cases reaching CLOSE (RESOLVE → CONFIRM → CLOSE path).
- **D564.7** INTAKE intake-complete handover on an INTAKE UC (UC-G/H/I/J/K). 1–2 cases where user supplies all required intake fields in 2–3 turns.
- **D616.2** RESOLVE FAQ premature-escalation prohibition. 1–2 cases on a FAQ-path UC with a known viable-hit article + mild user frustration.

Floor: N≥3 across all three directives (one per directive minimum). Suggested upper bound: 6 (2 per directive). Pick one structure:
- (i) `eval_interactive/case_specs/case_families/sprint29_directive_probe/` with subdirs per directive, OR
- (ii) three separate top-level case-families (one per directive).
Document rationale in handoff §4. Append manifest entries to existing `_manifest.yaml` (or create new manifest in the Sprint-29 dir; document choice).

Provenance per case:
- `source_dataset: case_family_authored`
- `source_session_id: synthetic-sprint29-<directive>-<idx>`

## 4. Track A authoring discipline (§1.7 HARD GATE — BLOCKING)

CaseSpecs MUST use **principled** `hidden_facts` + `expected_behaviors`. The directive shape is **structural**, not content; author cases that EXERCISE the precondition; do NOT bake directive text into the spec.

Hard rules (violation = STOP; review flags BLOCKING):

- No trace-specific text in `hidden_facts`. Hidden facts describe the user's situation.
- No keyword/regex preconditions in `expected.*`. Use structured fields (`primary_uc`, `should_escalate`, `escalation_trigger`, `expected_tool_sequence`, `forbidden_tools`, `grounding_mode`, `answer_must_not_contain`).
- No CaseSpec id mirrored into runtime/prompt/judge code.
- No rubric widening to accept a known bot mistake. **If the bot fails the authored case, the failure IS the finding.**
- No L3 override authored for the new cases.
- No editing of existing case families (`cs001_*`..`cs259_*`, `manual_probe_*`). Sprint 20 cascade fence stands.

## 5. Track A — Smoke run

For each authored case (or directory):

```
cd eval_interactive && uv run eval-interactive run --path <case_spec_path_or_dir>
```

Result at `eval_interactive/results/<run-id>/results.json`. Default **real-LLM** configuration; mocked-LLM is NOT permitted as primary evidence (Sprint 23 lesson). Mocked-LLM unit tests are supporting coverage only, with an explicit label in test file + handoff.

Per case, record in handoff §5.2 (reproducible extraction):
- Precondition fires (yes/no).
- Directive followed (yes/no).
- Bot-turn count via jq.
- Phase transitions observed via jq on `per_turn_trace[] | .phase_plan.phase`.
- Every count cited via jq/python on named file path.

## 6. Track B — (c) phase-derivation verification

After Track A smoke lands, extract per-turn `phase_plan.phase` deltas and verify:

- `phase_before[N] = phase_plan.phase[N-1]` (N ≥ 1)
- `phase_after[N] = phase_plan.phase[N]`
- For N == 0, `phase_before` is conventionally `DISCOVER`.

Answer four questions (handoff §6.1, reproducible jq):
1. Is `phase_plan` populated on every bot turn? (If any turn lands the **cs_029 third defensive shape** — populated trace, NULL `phase_plan` — derivation MUST defensively skip, NOT throw.)
2. Does the consumer's load-bearing case (D485.2 CLOSE entry) appear in Track A's corpus?
3. If yes, do `phase_plan.phase[N-1] == "CONFIRM"` and `phase_plan.phase[N] == "CLOSE"` correctly identify CLOSE entry?
4. Any other observed phase transitions where derivation could fail meaningfully for future consumers?

### 6.1 In-flight downgrade clause (Sprint 23 lesson — REQUIRED)

If Track B fails on first analysis pass (Q1–Q4 fails such that CLOSE-entry detection is corpus-undecidable on Track A's corpus):

- Handoff §6.2: document failure mode + reproducible extraction.
- Handoff §6.3: propose follow-on R-item `R-per-turn-phase-transition-dump-for-smoke-harness` to extend `per_turn_trace[i]` with `phase_before`, `phase_after`, `turn_index`.
- Do NOT retry Track B by re-running smoke with modified scope.
- Do NOT bundle the field extension into Track B. R-item is **proposed**, NOT **opened**.
- Track A still lands; Track B records the honest downgrade.

### 6.2 No code change in Track B

Verification only. No edits to `_build_per_turn_trace` or other Sprint 28-landed code.

## 7. cs_029 third defensive shape

cs_029 turn 1 (Sprint 28 smoke `eval_interactive/results/20260514-181257/results.json`) has populated trace + NULL `phase_plan`. Sprint 28 §5 documented only two empty-trace shapes (`cs_001` 500, `cs_259` CONTRACT_VIOLATION); cs_029 is a third, undocumented defensive shape — **populated trace, NULL phase_plan inside**.

Track B derivation MUST defensively skip a turn whose `phase_plan` is NULL — return "phase unknown" rather than throwing.

If Track A's authored corpus lands cs_029-shape on a load-bearing turn (e.g. CLOSE-entry turn has NULL `phase_plan`), Track B SHALL surface as failure under §6.1 and downgrade. Investigation of cs_029's root cause is a future R-item, NOT Sprint 29.

## 8. Hard fences (BLOCKING; 15 fences from sprint_objective §9)

1. No edits to Sprint 20 case families (`cs001_*`..`cs259_*`, `manual_probe_*`).
2. No edits to Sprint 23/24/25/26/27/28-landed code under `server/` or `eval_interactive/eval_interactive/`.
3. No prompt edits (`server/src/main/resources/prompts/system_prompt.txt`).
4. No eval-spec rubric widening.
5. No keyword/regex/if-else in CaseSpec `hidden_facts` / `expected.*`.
6. No new R-items opened unilaterally.
7. No foundational doc edits (`docs/foundational/`).
8. No governance doc edits (`docs/current/`).
9. No sprint archive edits (`docs/sprints/sprint-001-*` through `sprint-028-*.md`).
10. No Tier-0 changes.
11. No deadline/model/retry/budget config edits.
12. No mocked-LLM as primary evidence.
13. No cs_040 routing surface touched (Sprint 22 fence).
14. Every quantitative claim reproducible.
15. No retry on Track B failure.

## 9. Files in scope

Track A:
- `eval_interactive/case_specs/case_families/sprint29_directive_probe/` (or similar) — NEW directory + N≥3 CaseSpec YAMLs.
- `eval_interactive/case_specs/case_families/_manifest.yaml` — EDIT (append) OR new manifest in Sprint-29 dir.
- `eval_interactive/results/<run-id>/` — NEW (smoke output).

Track B: no file edits.

Handoff: `docs/sprints/sprint-029-handoff.md` NEW. `docs/action_bank.md` line-450 entry EDIT (update disposition; do NOT close).

## 10. Files NOT in scope

Everything else. If you want to touch a file outside §9, STOP and surface to the human.

## 11. Working tree at session start

Deliver-agent-owned files (do NOT stage yourself):
- `docs/sprint_objective.md` (Sprint 29 scope).
- `compact/sprint-029-dev-prompt.md` (this file) + `compact/sprint-029-review-prompt.md` + `compact/sprint-deliver-orchestrator.md`.

Plus 3 pre-existing unrelated mods (carried since Sprint 24; not your scope):
- ` M csagent_system_design_review.md`
- ` M server/src/main/resources/prompts/system_prompt.txt` (source of inherited `SystemPromptUserRequestedTiebreakerTest` failure)
- `?? csagent-solution-_20260514.md`

**Do not stage deliver-agent files; do not be surprised if the human bundles them at commit time.** Use `git add` on specific files under §9 only; avoid `git add -A` / `git add .`.

## 12. Handoff doc contract (12 sections; mirror Sprint 19/20/27)

Produce `docs/sprints/sprint-029-handoff.md`:

1. Context Pack.
2. Sprint-objective recap.
3. Premise re-verification (confirm §4 of sprint_objective).
4. Track A — CaseSpec authoring (files; per-directive rationale; §4 anti-hardcode self-check).
5. Track A — Smoke run results (per-case observation table; per-directive aggregate).
6. Track B — (c) phase-derivation verification (Q1–Q4; verdict; if failure: proposed follow-on R-item).
7. R-item disposition update (action_bank line 450).
8. Anti-hardcode self-walk (§4.1 nine questions).
9. Files changed.
10. Layer-classification self-walk (§3; per-track).
11. Open questions for human.
12. Closure verdict (placeholder; deliver-agent fills at close).

## 13. Stop conditions

- Premise drift — STOP.
- Track B mid-sprint failure — surface + propose follow-on R-item; do NOT retry.
- Rubric widening temptation — STOP. Failure IS the finding.
- Sprint 20 case-family edit — STOP.
- Trace-text in `hidden_facts` — STOP and redraft.
- cs_040 routing surface — STOP.
- Mocked-LLM as primary evidence — STOP.
- R-item closure — STOP. Update disposition only.
- Scope expansion — STOP and propose follow-on R-item.

## 14. Definition of done

- Track A: N≥3 CaseSpecs + smoke ran + per-case table in §5.2 + per-directive aggregate in §5.3 with reproducible extraction.
- Track B: Q1–Q4 answered in §6.1 with reproducible jq — either "derivation works; no follow-on R-item" OR "derivation fails; proposed R-item named".
- `R-prompt-phase-plan-directive-followship` updated at `docs/action_bank.md:450` (NOT closed).
- Anti-hardcode self-walk passes in §8.
- No fence in §8 violated.
- Codex sprint-close review will check §4 authoring discipline + Track B downgrade + reproducibility + mocked-LLM constraint.
