# Sprint 53 / M5 S4 — Skill-declaration audit + context-key gating (#2) + soft-signal gating (#5) — Dev Implementation Prompt

You are the dev agent for **Sprint 53 / S4**, the FOURTH sub-sprint of **Milestone
M5 — Observability Coherence** and its **highest-risk** one. S4 completes the
Skill-driven projection convergence S3 began: S3 landed C2 #3 + #4 and
STOP-surfaced **#2 (Skill-declared context-key gating)** + **#5
(soft_signal_via_projection gating)** to S4 because the C1 matrix found the Skill
declarations INCOMPLETE — gating on them as-is would **drop LLM-visible context**.

**HARD ORDER: deliver + get the Phase-A Skill-declaration AUDIT reviewed BEFORE
any Phase-B gating change.** No slot is gated until its audit row confirms the
Skill declaration is complete and gating won't drop a signal the LLM relies on.
This is the S3-C1 discipline, applied to the higher-risk context/soft-signal
surface. Read this prompt + the contracts in §1 before writing any code.

**This is semantic-touching + the highest-risk M5 sub-sprint.** Gating MUST be
registry/Skill-driven (read `PhasePlan.requiredContextKeys()` +
`Skill.stateInheritance().softSignalViaProjection()`). No per-UC if-else /
keyword / enum (§1.7). **BOTH a real-LLM bad-case rerun AND a shadow rerun are
MANDATORY evidence gates** (mocked-LLM covers wiring only). If a slot can't be
gated without dropping an LLM-visible signal, **STOP and surface** — do not gate.

## 1. Read order (cold start)

1. `AGENTS.md` (auto-loaded constitution chain).
2. `docs/solutions/observability_coherence_admin_trace_and_projection.md` — §2.C
   / §4.C (C2/C3) / §8 (#3 fences).
3. `docs/sprint_objective.md` — the Sprint 53 contract (Phase A #1/#1a → Phase B
   #2/#3; #4 CONDITIONAL/deferred; the §7 stanza; the hard fences).
4. `docs/sprints/sprint-052-handoff.md` §2 (#2 + #5 STOP-surface rationale) +
   §7 (OQ-S52.1/2/4) + `docs/diagnostics/m5-s3-projection-consumption-map.md`
   (the S3 C1 matrix — your audit's starting point; it already flags the
   declaration gaps).
5. `docs/milestone_objective.md` §2 (#3 goal) + §3 (S4 paragraph) + §6 (S3/S4
   fences) + §8 (per-sub-sprint Codex at S4 close).
6. `docs/10-handoff.md` §0 — baselines (Java `1172 / 1-inherited / 0 / 2`;
   Python `3 failed / 486 passed`).

## 2. The current projection map (verified — your Phase-A starting point)

- Context slots emitted by `ContextProjectionBuilder` (e.g. `form_context`,
  `customer_context`, `listing_context`) — emitted unconditionally / via legacy
  UC logic today.
- `PhasePlan.requiredContextKeys()` is SET from `skill.requiredContextKeys()`
  (`PhaseEvaluator.java:455`) but has **no projection-layer reader** today
  (grep confirms) — declared-but-not-consumed. The declarations are INCOMPLETE
  (S3 C1: `discover_triage` lacks `customer_context`; only FAQ declares
  `listing_context`).
- Soft-signal slots: `alternate_candidate_use_cases`
  (`ContextProjectionBuilder.java:441`), `discover_disambiguation_signals`
  (`:457`), `prior_use_case_carry` (`:475`) — built via legacy Sprint-31/33/41
  UC/phase logic.
- `Skill` record (`service/runtime/skill/Skill.java`) carries
  `requiredContextKeys` (List) + `stateInheritance` (StateInheritance, with
  `softSignalViaProjection` posture). These are the gates #2/#5 will read.

## 3. Items (per `docs/sprint_objective.md`)

### Phase A — deliver + review FIRST
- **#1 Skill-declaration completeness audit** → `docs/diagnostics/m5-s4-skill-declaration-audit.md`.
  Per Skill × slot: declared / emitted-today / actually-needed / GAP. **Gate for
  all Phase-B gating.** Builds on the S3 C1 matrix.
- **#1a Complete the declarations** — add missing `requiredContextKeys` /
  `softSignalViaProjection` declarations to the Skill YAMLs so the declaration
  set is the complete, intentional gate (registry data). If a slot is needed by
  ALL Skills → keep it unconditional, don't gate (record in the matrix).

### Phase B — gated by the Phase-A audit; registry/Skill-driven only
- **#2 Context-key gating** — `ContextProjectionBuilder` reads
  `PhasePlan.requiredContextKeys()` to decide context-slot emission. No per-UC
  branch.
- **#3 Soft-signal gating** — route the 3 soft-signal slots through
  `Skill.stateInheritance().softSignalViaProjection()`. Soft signal stays
  LLM-owned.
- **#4 CONDITIONAL (default DEFER)** — C3 dedup/denoise + OQ-S52.4
  `knowledge_hits` canonicalization. Do NOT let it expand S4; defer to S5/future
  milestone unless Phase A+B land with budget to spare (deliver-agent decides at
  close).
- **#5 Tests + mandatory gates** — Java wiring tests + the real-LLM bad-case
  rerun + the **shadow rerun** (both mandatory).

## 4. Hard fences (from `docs/sprint_objective.md` — DO NOT VIOLATE)

- **AUDIT BEFORE GATING.** No slot gated before its Phase-A row confirms the
  declaration is complete + gating drops no LLM-visible signal. Unresolved gap →
  STOP and surface.
- **Registry/Skill-driven — no per-UC if-else / keyword / enum** (§1.7). No
  semantic ownership moves LLM → Java.
- **No change to LLM-visible semantic information** — gating changes *whether a
  slot is projected for a Skill*, never *whether the LLM can see* a signal it
  could see before. Real-LLM + shadow reruns are the proof.
- No `escalation_reason` enum / tool-schema-def / PII / safety / grounding-floor
  change; no `composite.py` / eval-fixture / scoring change; OQ-S51.2 overlay +
  S2 `bot_turn_llm_calls`/`/trace` UNTOUCHED.
- **STOP and surface** if a gating needs a dropped LLM-visible signal, or if the
  real-LLM / shadow rerun regresses the bad-case distribution (in-flight
  downgrade — revert the gate, surface; do not force it).

## 5. §7 stanza + §4.1 self-walk (sub-sprint is §7 REQUIRED)

Fill the dev handoff §3 with the §4.1 9-question kernel walk. **Expected
`approve`** IF gating stays registry/Skill-driven + the audit completes the
declarations first. Key: Q1 — completing declarations (#1a) is registry data,
not a hardcode; gating reads declarations, adds no UC branch; Q5 — no semantic
ownership moves LLM → Java (the LLM still owns UC/action/escalation); Q8 —
coverage incl. the NEGATIVE control "a Skill that NEEDS a slot still gets it"
(the dropped-signal risk) + the mandatory shadow rerun. A per-sub-sprint Codex
review runs at S4 close (deliver-agent + human dispatch).

## 6. Tests to run

- **Java**: `mvn test -B` — no NEW regression vs `1172 / 1-inherited / 0 / 2`.
- **Real-LLM bad-case rerun (MANDATORY)**: `make backend` (restart on the S4
  build — no hot-reload; `javap` the new gating method to confirm the build) →
  `cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1`
  → distribution must hold (PASS×5 / IMPROVING×4 / FAIL×3 / OOSR×0; isolate
  session-establishment flakes). **Use `python -m pytest`, not the segfaulting
  console script.**
- **Shadow rerun (MANDATORY)**: the held-out `case_specs_shadow/` set is
  **dev-blind** — you do NOT read shadow case content or results; STOP-surface
  the shadow rerun to the deliver-agent at close (deliver-agent / human run +
  read it), exactly as the bad-case eyeball pattern. Record the STOP-surface in
  the handoff.
- **Python**: `cd eval_interactive && uv run python -m pytest --tb=no -q` — no
  NEW regression vs `3 failed / 486 passed`.

## 7. Handoff + bundle

- Produce `docs/sprints/sprint-053-handoff.md`; **§12 reserved**.
- Ship the Phase-A audit matrix under `docs/diagnostics/`.
- Record: `git show --numstat`; Phase-A→Phase-B ordering evidence; the real-LLM
  rerun results (+ the shadow rerun STOP-surface for the deliver-agent); the §7
  self-walk; any slot STOPPED-and-surfaced rather than gated.
- **Stage ONLY S4 scope** (`server/src/main/java/**`,
  `server/src/main/resources/skills/*.yaml`, `docs/diagnostics/**` audit, new
  tests, NEW `sprint-053-handoff.md`); **no `git add -A`**.

## 8. Self-check (before claiming complete)

- [ ] Phase-A audit matrix delivered + reviewed by deliver-agent + human BEFORE
  any Phase-B gating?
- [ ] Declarations COMPLETED (#1a) so gating drops no LLM-visible signal?
- [ ] #2 + #3 gating is registry/Skill-driven — zero per-UC if-else / keyword /
  enum added?
- [ ] NEGATIVE control proven: a Skill that needs a slot still receives it (no
  dropped signal)?
- [ ] #4 (C3 + knowledge_hits) deferred (not expanded into S4)?
- [ ] Java: no NEW regression; new wiring tests PASS?
- [ ] **Real-LLM bad-case rerun holds the M4-close distribution** (or
  STOP-surfaced)?
- [ ] **Shadow rerun STOP-surfaced to the deliver-agent** (dev-blind)?
- [ ] §7 stanza + §4.1 self-walk written for the S4-close Codex review?
- [ ] Handoff §1-§11 complete; §12 reserved; staged only S4-scope files?

If any checkbox is unchecked, surface to the deliver-agent BEFORE declaring
complete. The close gates are: the Phase-A audit (reviewed), the registry/Skill-
driven invariant (no per-UC hardcode + no dropped signal), and BOTH the real-LLM
bad-case distribution holding AND the shadow rerun showing no regression.
