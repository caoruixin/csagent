# Sprint 52 / M5 S3 — Projection audit (C1) + Skill-driven convergence (C2) — Dev Implementation Prompt

You are the dev agent for **Sprint 52 / S3**, the THIRD sub-sprint of **Milestone
M5 — Observability Coherence** and the milestone's **ONLY semantic surface (§7
REQUIRED)**. Goal: (C1) produce an auditable **field consumption matrix** for the
per-turn projection, then (C2) converge the projection to be **Skill-driven** where
M2 left it UC-driven — **without changing the semantic information the LLM can
see**. S2 (now closed) gave you per-invocation projections in the trace; you use
them as C1 evidence.

**HARD ORDER inside S3: deliver + get the C1 matrix reviewed BEFORE you change any
projection field in C2.** No field is deleted, renamed, or content-changed until
its matrix row shows no consumer would break. Read this prompt + the contracts in
§1 before writing any code.

**This is semantic-touching.** Convergence MUST be registry/Skill-driven — reading
`PhasePlan.requiredContextKeys()` / `Skill` declarations / the
`state_inheritance.soft_signal_via_projection` config. **No per-UC if-else, no
keyword / regex / enum** (§1.7). If a convergence cannot be done without a UC
branch, **STOP and surface** — do not introduce the branch.

## 1. Read order (cold start)

1. `AGENTS.md` (auto-loaded constitution chain). Do not re-read if in context.
2. `docs/solutions/observability_coherence_admin_trace_and_projection.md` —
   **READ §2.C (projection data-flow + line anchors), §4.C (C1/C2 design), §8 (#3
   fences).** This is the detail design.
3. `docs/sprint_objective.md` — the Sprint 52 contract (numbered scope C1 #1/#1a +
   C2 #2-#6 is binding; the §7 stanza is binding).
4. `docs/milestone_objective.md` §2 (#3 goal) + §3 (S3 paragraph) + §6 (S3/S4 hard
   fences) + §8 (Codex plan — S3 gets a per-sub-sprint Codex review at close).
5. `docs/sprints/sprint-051-handoff.md` §12.3 — **OQ-S51.2** (inherited): the S2
   eyeball found `BotTurn.projected_context` carries a PRE-EXISTING
   `mergeFaqGroundingIntoProjection` overlay that the raw per-invocation projection
   does not. You map this in C1 #1a; you do NOT auto-converge it.
6. `docs/10-handoff.md` §0 — baselines (Java `1165 / 1-inherited / 0 / 2`; Python
   `3 failed / 486 passed`).

## 2. The current projection map (verified — your C1 starting point, not the whole audit)

- `ContextProjectionBuilder` (`server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`):
  `buildProjection(...)` (~`:337`); two `build(...)` overloads (~`:733`, `:787`);
  `buildDriftAndTaskHistory(...)` (~`:1265`, the drift/task-history reconstruction
  consumer); `tool_schemas` set TWICE — a UC-driven base (~`:633`) THEN a
  Skill/plan-filtered set that overwrites it (~`:862`, the "MUST be filtered to
  plan.allowedTools" comment ~`:825`); `knowledge_hits` (~`:681`).
- `PhasePlan` (`model/PhasePlan.java`): `allowedTools()` + `requiredContextKeys()`
  are record components (~`:25-26`); `ToolDispatcher` enforces `allowedTools()`
  (~`:187-194`). `Skill` interface at `service/runtime/skill/Skill.java` — confirm
  in C1 exactly where `requiredContextKeys` is sourced (Skill vs PhasePlan).
- OQ-S51.2 overlay: `ControlKernel.mergeFaqGroundingIntoProjection(...)`
  (~`:2363`) is applied to `result.lastProjection()` (~`:2029`/`:2042`) before the
  `BotTurn.projected_context` single column is written — PRE-EXISTING, S2-untouched.
- `moderation_context`: per the proposal, a Skill declares it required but
  `ContextProjectionBuilder` never emits it — **confirm in C1** before acting.

This map is your starting point; the C1 matrix (§3 #1) is the full deliverable.

## 3. Items (per `docs/sprint_objective.md`)

### C1 — deliver + review FIRST
- **#1 Field consumption matrix** → `docs/diagnostics/m5-s3-projection-consumption-map.md`.
  Every projected field × {system_prompt explains / eval trace contract validator
  requires (`eval_interactive/.../trace/collector.py` — cite the exact check) /
  drift-task reconstruction depends (`buildDriftAndTaskHistory`) / a Skill declares
  / a real S2 trace shows the LLM using it}. **Gate for all C2 changes.**
- **#1a OQ-S51.2 row** — map the `BotTurn.projected_context` overlay vs raw
  per-invocation projection; name which is canonical for (eval trace contract) /
  (admin display) / (what the LLM saw); **recommend a disposition; human decides at
  C1 review. Do NOT change the overlay in S3.**

### C2 — each item gated by its #1 matrix row; registry/Skill-driven only
- **#2 Skill-declared context-key gating** — projection reads
  `PhasePlan.requiredContextKeys()` / the active `Skill` to decide slots (replace
  UC branching). No per-UC if-else.
- **#3 `moderation_context` gap** — emit it (if a consumer needs it) OR remove the
  stale declaration (if none does); pick the direction the C1 row justifies.
- **#4 Remove the computed-then-discarded UC-driven `tool_schemas` base** (~`:633`)
  in favour of the Skill-driven `plan.allowedTools()` filtered set — only if C1
  confirms nothing consumes the base before it is overwritten.
- **#5 `state_inheritance.soft_signal_via_projection` gating** — route soft signals
  (e.g. `prior_use_case_carry`) through the config path, not a UC branch; confirm
  the key + wiring in C1 first.
- **#6 Tests + real-LLM gate** — Java wiring/rendering tests (same slots projected,
  `moderation_context` coherent, `tool_schemas` ends Skill-filtered); the
  behaviour-risk gate is the **real-LLM bad-case rerun** (§6 below). Mocked-LLM =
  wiring only (§5.6 eval-evidence gate).

## 4. Hard fences (from `docs/sprint_objective.md` §"Hard fences" — DO NOT VIOLATE)

- **C1 BEFORE C2.** No field deleted/renamed/content-changed before its matrix row
  confirms no consumer (eval trace contract / `buildDriftAndTaskHistory` /
  admin-trace) would break. Ambiguous consumers → STOP and surface.
- **Registry/Skill-driven — no per-UC if-else / keyword / regex / enum** (§1.7). No
  semantic ownership moves LLM → Java.
- **No change to LLM-visible semantic information** — C2 may change which slot
  carries info or how it is gated, never whether the LLM can see a signal it could
  see before. The real-LLM rerun is the proof.
- No `escalation_reason` enum / tool-schema / PII / safety / grounding-floor change;
  no `composite.py` / eval-fixture / scoring change.
- Do NOT change the OQ-S51.2 overlay without explicit human decision at C1 review.
- Do NOT change the S2 `bot_turn_llm_calls` schema or `/trace` shape (read-only
  consume of S2's per-step projections).
- **STOP and surface** if a convergence needs a removed LLM-visible signal, or if
  the real-LLM rerun regresses the bad-case distribution (in-flight downgrade — do
  not force the change to close the symptom; that triggers a deliver-agent replan).

## 5. §7 stanza + §4.1 anti-hardcode self-walk (sub-sprint is §7 REQUIRED)

S3 is `prompt_projection` / semantic-touching. The §7 stanza in
`docs/sprint_objective.md` is binding — fill the dev handoff §3 with the §4.1
9-question kernel walk. **Expected: `approve`** IF convergence stays registry/Skill-
driven. Key: Q1 — no keyword/regex/enum/per-UC matrix added (you REPLACE UC
branching with Skill declarations, not add new branches); Q3 — the change keeps the
soft signal LLM-owned; Q5 — no semantic ownership moves LLM → Java; Q8 —
generalization coverage = the bad-case distribution holds on the real-LLM rerun.
**A per-sub-sprint Codex review runs at S3 close** (deliver-agent + human dispatch;
you do NOT dispatch Codex) — write the handoff so Codex can verify the §4.1 kernel
against your commit range.

## 6. Tests to run

- **Java**: `mvn test -B` — no NEW regression vs `1165 / 1-inherited / 0 / 2`
  (count grows with new S3 tests; inherited `SystemPromptUserRequestedTiebreakerTest`
  persists per OQ-S41.5).
- **Real-LLM bad-case rerun = the S3 evidence gate** (needs `make backend` + LLM
  keys): rerun `eval_interactive/case_specs/bad_cases/` and confirm the M4-close
  distribution holds — **PASS×5** (cs001/cs014/cs029/cs066/fg5q) + **IMPROVING×4**
  (alice/cs011/cs012/wmkb) + **FAIL×3** (cs015/cs095/iwzx) + **OOSR×0**. A
  regression = in-flight downgrade → STOP and surface. (Backend/keys unavailable →
  STOP and surface; mocked-LLM is NOT acceptable as the primary semantic-preservation
  evidence.)
- **Python**: `cd eval_interactive && uv run python -m pytest --tb=no -q` — no NEW
  regression vs `3 failed / 486 passed` (use `python -m pytest`, not the segfaulting
  console script; S3 should not touch eval-harness code).

## 7. Handoff + bundle

- Produce `docs/sprints/sprint-052-handoff.md`; **§12 reserved** (deliver-agent +
  human at close).
- Ship the C1 matrix as a committed artifact under `docs/diagnostics/`.
- Record: `git show --numstat`; the C1→C2 ordering evidence (matrix delivered
  before any field change); the real-LLM bad-case rerun results dir + per-case
  distribution; the §7 self-walk; any field whose convergence was STOPPED-and-
  surfaced rather than forced.
- Numbers from reproducible commands. **Stage ONLY S3 scope** (`server/src/main/java/**`,
  `docs/diagnostics/**` C1 matrix, new Java tests, NEW `sprint-052-handoff.md`); **no
  `git add -A`**; enumerate in the commit message.

## 8. Self-check (before claiming complete)

- [ ] Read the obs proposal §2.C/§4.C/§8 + the OQ-S51.2 handoff note?
- [ ] C1 matrix delivered to `docs/diagnostics/` AND reviewed by deliver-agent +
  human BEFORE any C2 field change?
- [ ] Every C2 change (#2-#5) gated by its matrix row (no consumer breaks)?
- [ ] Convergence is registry/Skill-driven — zero per-UC if-else / keyword / enum
  added?
- [ ] `moderation_context` resolved in the direction C1 justifies (#3)?
- [ ] `tool_schemas` discarded base removed only if C1 confirms no consumer (#4);
  ends with the Skill-filtered set?
- [ ] OQ-S51.2 overlay mapped (#1a) and NOT auto-changed (human decides)?
- [ ] Java: no NEW regression; new wiring/rendering tests PASS?
- [ ] **Real-LLM bad-case rerun holds the M4-close distribution** (PASS×5 /
  IMPROVING×4 / FAIL×3 / OOSR×0) — or STOP-surfaced?
- [ ] §7 stanza filled + §4.1 self-walk written for the S3-close Codex review?
- [ ] Handoff §1-§11 complete; §12 reserved; staged only S3-scope files?

If any checkbox is unchecked, surface to deliver-agent BEFORE declaring complete.
The close gates are: the C1 matrix (reviewed), the registry/Skill-driven invariant
(no per-UC hardcode), and the real-LLM bad-case distribution holding (no LLM-visible
semantic loss).
