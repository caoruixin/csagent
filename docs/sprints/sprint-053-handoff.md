---
title: Sprint 53 / M5 S4 — Skill-declaration audit (Phase A) + context-key gating (#2) + soft-signal gating (#5) — dev handoff
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (sub-sprint dev archive); ContextProjectionBuilder.java + Sprint53SkillDeclarationGatingTest.java + docs/diagnostics/m5-s4-skill-declaration-audit.md (code)
last_reviewed: 2026-05-25
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sub-sprint dev-authored handoff. FOURTH sub-sprint of Milestone M5 —
  Observability Coherence; the milestone's highest-risk sub-sprint (§7
  REQUIRED; per-sub-sprint Codex at S4 close). Phase A (Skill-declaration
  audit) DELIVERED + reviewed via human AskUserQuestion BEFORE any Phase
  B gating change. Phase B landed: #2 candidate_use_cases gating + #5
  three soft-signal gates (alternate_candidate_use_cases /
  discover_disambiguation_signals / prior_use_case_carry), all
  registry/Skill-driven, no per-UC if-else, defensive-emit default for
  unmapped (phase, UC). #1a YAML edits: ZERO (audit concluded
  declarations are intentional as-is for the 4 gateable slots). #4
  (CONDITIONAL C3 + knowledge_hits) DEFERRED to S5/follow-on milestone
  per default. Real-LLM bad-case rerun: initial run during upstream
  LLM provider degradation (deepseek primary + kimi fallback both
  exhausted; 128 transport errors 09:24-12:10 CST) was STOP-AND-
  SURFACED; the provider recovered later in the session and a
  recovered-LLM re-run (12:15 CST) reproduces the M4-close
  distribution shape — REGRESSION-SAFE PASS (10/12 bot_ended cleanly;
  2/12 hit documented `R-bad-case-parallel-session-establishment-flakiness`
  and BOTH cleared on 2× iso rerun). Phase B gating empirically
  verified working in both directions (DISCOVER soft signals present
  + prior_use_case_carry absent; RESOLVE opposite) across both backend
  builds. Shadow rerun STOP-AND-SURFACED per dev-blind contract.
  §12 reserved for deliver-agent + human at sub-sprint close.
---

# Sprint 53 / M5 S4 — Skill-declaration audit (Phase A) + context-key gating (#2) + soft-signal gating (#5) dev handoff

## 1. Goal and outcome

S4 completes the M5 #3 Skill-driven projection convergence S3 began.
S3 landed C2 #3 (`moderation_context` strip) + C2 #4 (Skill-registry-
driven `tool_schemas` base) and STOP-surfaced **#2 (Skill-declared
context-key gating)** + **#5 (`soft_signal_via_projection` gating)**
because the S3 C1 matrix flagged the Skill declarations INCOMPLETE —
gating on them as-is would drop LLM-visible context.

S4 ran the audit-before-gating discipline that S3's C1 established. **Phase A** produced a Skill-declaration completeness audit
(`docs/diagnostics/m5-s4-skill-declaration-audit.md`, 462 lines)
classifying every candidate slot × Skill row against actual NEED vs
declared coverage and routing each slot to one of three outcomes:
GATE / KEEP UNCONDITIONAL / OUT OF SCOPE. The audit was reviewed by
the human via `AskUserQuestion` 2026-05-25 BEFORE any Phase-B code
edit; the human's directive was "Land Phase B per audit (4-slot gate;
no YAML edit)" — option 1 (Recommended).

**Phase B** landed the registry/Skill-driven gating per the audit:

- **#2 — `candidate_use_cases` context-key gating** in
  `ContextProjectionBuilder.buildProjection(...)` (lines wrapping
  `:411-419`). Reads `Skill.requiredContextKeys()` via a new private
  helper `skillRequiresContextKey(session, activeUc, key)`. Defensive
  default = emit when the registry is unavailable / phase is null /
  no Skill maps the tuple. Per audit §3.E, only `discover_triage`
  declares + needs.
- **#5 — Soft-signal gating** for the three slots:
  `alternate_candidate_use_cases` (`:432-441`),
  `discover_disambiguation_signals` (`:457`), and
  `prior_use_case_carry` (`:473-475`). Reads
  `Skill.stateInheritance().softSignalViaProjection()` via a new
  private helper `skillDeclaresSoftSignal(session, activeUc, slot)`.
  Same defensive-emit default. Per audit §3.G/§3.H/§3.I, only
  `discover_triage` declares the first two; `resolve_faq_grounded_answer`
  + `resolve_intake_collect_and_handover` declare
  `prior_use_case_carry`.

The four KEEP-UNCONDITIONAL slots flagged by the audit (`form_context`,
`customer_context`, `listing_context`, `conversation_history`) stay
unchanged — gating any of them would risk DROPPING an LLM-visible
signal the bad-case suite depends on (alice UC-A↔UC-H,
UC-FP appeal flows, UC-A↔UC-C drift continuity); the spec's
"all-Skills-need / INHERIT-RISK → keep unconditional" exception
applies. `intake_state` stays OUT OF SCOPE — the existing
`IntakeFieldsRegistry` gate is canonical (and Tier-2 LOAD-BEARING).

**#1a (complete the declarations) result: ZERO YAML edits.** The
audit confirmed declarations are intentional as-is for the four
GATE-routed slots; for the KEEP-UNCONDITIONAL slots, adding
declarations would not change Phase-B behaviour.

**#4 (CONDITIONAL C3 + OQ-S52.4 `knowledge_hits`): DEFERRED per
default** (sprint contract §"Scope — #4"). Not expanded into S4 to
keep risk-bounded.

LLM-visible semantic impact of LANDED changes: **the four gated
slots emit ONLY for declaring Skills + unmapped-tuple fallback
(defensive default)**. Empirically verified against the 5
bad-case sessions that completed turns on the S4 build (§6.2): every
RESOLVE turn (resolve_faq + resolve_intake) saw the three DISCOVER-
only slots ABSENT (gated correctly), and the `prior_use_case_carry`
slot PRESENT (declared by RESOLVE Skills, gate emits). One sampled
DISCOVER turn (cs011) had all three DISCOVER soft signals PRESENT
and `prior_use_case_carry` ABSENT (gated — `discover_triage` doesn't
declare). The runtime behaviour matches the Phase-A audit's predicted
declaration coverage exactly.

## 2. Scope (per `docs/sprint_objective.md` #1-#5)

### Phase A — #1 Skill-declaration completeness audit (DELIVERED + reviewed)

Authored `docs/diagnostics/m5-s4-skill-declaration-audit.md` (462 lines).
Covers the **6 production Skills** × the **9 candidate slots** identified
in the S3 C1 matrix §3.E/§3.L (6 context-key + 3 soft-signal). Each cell
records four columns: DECL (declared in YAML?), EMIT (emitted today?),
NEED (does the Skill genuinely use the slot per its
procedure / critical_steps / grounding?), RISK if gated. Each slot is
routed to GATE / KEEP UNCONDITIONAL / OUT OF SCOPE / STOP-AND-SURFACE.
Source-of-truth citations:

- Skill declarations → `server/src/main/resources/skills/*.yaml`
- Validator allowed-slot names →
  `SkillLoader.java:65-69` (`VALID_PROJECTION_SLOTS`)
- Projection-layer emission anchors →
  `ContextProjectionBuilder.java:411-475` (the 4 gated slots) +
  `:636-674` (the 4 unconditional context slots)
- S3 C1 cross-references → `docs/diagnostics/m5-s3-projection-consumption-map.md`
  §3.E (soft signals) / §3.L (context blocks) / §4 (C2 disposition)

The audit reached a CONSERVATIVE conclusion: **4 slots GATE-routed
cleanly**, **4 slots KEEP-UNCONDITIONAL** (dropping any would risk
LLM-visible signal loss the bad-case suite depends on), **1 slot
(`intake_state`) OUT OF SCOPE** (already registry-gated). **Zero
STOP-AND-SURFACE** — all 9 slots resolved cleanly.

The audit was committed as a diagnostic artifact under
`docs/diagnostics/` BEFORE any Phase-B code edit.

### Phase A — #1a Complete the declarations — ZERO YAML EDITS

The audit concluded declarations are intentional as-is for the four
GATE-routed slots (`candidate_use_cases` → discover_triage only;
`alternate_candidate_use_cases` / `discover_disambiguation_signals` →
discover_triage only; `prior_use_case_carry` → resolve_faq +
resolve_intake). For the KEEP-UNCONDITIONAL slots, adding declarations
to non-declaring Skills would not change Phase-B behaviour (we are not
gating them) and would add YAML churn for zero behaviour change. Per
the sprint contract: *"If a slot is needed by ALL Skills → keep it
unconditional, don't gate (record in the matrix)."* The audit records
this for each of the four kept-unconditional slots.

**Footprint of #1a: zero YAML edits.**

### Phase A review — `AskUserQuestion` 2026-05-25

Surfaced the audit's conservative outcome (GATE 4, KEEP UNCONDITIONAL
4, OUT OF SCOPE 1) via `AskUserQuestion` BEFORE any Phase-B code edit.
Four options offered: (a) land Phase B per audit, (b) add declarations
to non-gating slots first, (c) land only the 3 soft-signal gates
(skip #2), (d) STOP S4 as Phase-A-only partial close. Human chose
**(a) — Land Phase B per audit (4-slot gate; no YAML edit)
(Recommended)**.

### Phase B — #2 Skill-declared context-key gating (LANDED)

`ContextProjectionBuilder.java` (numstat `+148/-26`):

- Wrapped `candidate_use_cases` emission (formerly `:411-419`,
  unconditional) with `if (skillRequiresContextKey(session, activeUc,
  "candidate_use_cases"))`.
- New private helper `skillRequiresContextKey(BotSession, String, String)`
  (placed after `resolveProjectedToolNames` — the S3 C2 #4 helper —
  to keep registry-driven helpers grouped). Reads
  `skillRegistry.select(currentPhase, activeUc).map(Skill::requiredContextKeys)`
  and tests for membership. Defaults to `true` (emit) when
  `skillRegistry == null`, `session == null`, `currentPhase == null`,
  OR `select(...).isEmpty()` (no Skill maps the tuple). This is the
  same defensive-default pattern S3 C2 #4 used for `resolveProjectedToolNames`'s
  `getVisibleToolsForUc(...)` fallback — preserves pre-S4 behaviour for
  legacy/unmapped sessions.

§1.7 boundary verified: no per-UC if-else, no keyword/regex/enum
branch added. The gate reads registry data (the YAML declaration);
the defensive fallback is a single uniform `true`.

### Phase B — #5 Skill-soft-signal gating (LANDED)

Same file, same pattern:

- Wrapped `alternate_candidate_use_cases` emission (formerly
  `:432-441`, unconditional) with `if (skillDeclaresSoftSignal(...,
  "alternate_candidate_use_cases"))`.
- Wrapped `discover_disambiguation_signals` emission (formerly `:457`,
  unconditional) with `if (skillDeclaresSoftSignal(...,
  "discover_disambiguation_signals"))`. Helper is invoked BEFORE the
  expensive `buildDiscoverDisambiguationSignalsNode(session)` call so
  the computation is skipped entirely when gated out.
- Wrapped `prior_use_case_carry` emission (formerly `:473-475`,
  unconditional) with `if (skillDeclaresSoftSignal(...,
  "prior_use_case_carry"))`. Same skip-build optimization as above.
- New private helper `skillDeclaresSoftSignal(BotSession, String,
  String)`. Reads
  `skillRegistry.select(...).map(s -> s.stateInheritance().softSignalViaProjection())`
  and tests for membership. Same defensive-default behaviour as
  `skillRequiresContextKey`.

§1.7 boundary verified: same as #2.

### #4 — CONDITIONAL DEFERRED

Per the sprint contract default (§"Scope — #4 (CONDITIONAL — decided at
S4 close)"), the original-S4 C3 (dedup/denoise) + OQ-S52.4
(`knowledge_hits` canonicalization) are deferred to keep S4 risk-
bounded. The Phase B convergence + the audit consumed the available
budget; C3 + `knowledge_hits` belong in S5 or a separate
"projection hygiene" milestone per the contract's §8.5 footnote.

### #5 — Tests + the mandatory evidence gates

**Java wiring/rendering tests** (LANDED, 11 NEW tests):

NEW `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint53SkillDeclarationGatingTest.java`
(528 lines, 11 tests, all PASS):

| Test | Audit row | Purpose |
|---|---|---|
| `discoverTriage_receivesCandidateUseCases_alternate_disambig` | §3.E/§3.G/§3.H | **POSITIVE**: discover_triage emits all 3 DISCOVER slots |
| `resolveFaq_receivesPriorUseCaseCarry` | §3.I | POSITIVE: resolve_faq emits `prior_use_case_carry` |
| `resolveIntake_receivesPriorUseCaseCarry` | §3.I | POSITIVE: resolve_intake emits `prior_use_case_carry` |
| `confirm_doesNotReceive_anyGatedSlot` | §3.E/§3.G/§3.H/§3.I | **NEGATIVE**: confirm doesn't get any of 4 |
| `escalate_doesNotReceive_anyGatedSlot` | same | NEGATIVE: escalate doesn't get any of 4 |
| `terminal_doesNotReceive_anyGatedSlot` | same | NEGATIVE: terminal doesn't get any of 4 |
| `resolveFaq_doesNotReceive_discoverOnlySoftSignals` | §3.E/§3.G/§3.H | NEGATIVE: resolve_faq doesn't get DISCOVER-only slots, DOES get `prior_use_case_carry` |
| `unmappedTuple_receivesAllGatedSlots_defensiveDefault` | §6.A defensive default | **DEFENSIVE**: unmapped (phase, UC) emits all 4 — preserves pre-S4 |
| `nullPhase_receivesAllGatedSlots_defensiveDefault` | same | DEFENSIVE: null phase emits all 4 |
| `confirmSkill_stillReceivesFormContext_whenSessionHasIt` | §4.B (KEEP-UNCONDITIONAL) | **KEEP-UNCONDITIONAL**: confirm still gets form/customer/listing/conversation context (data-gated, not Skill-gated) |
| `productionSkillYamls_matchAuditDeclarationCoverage` | §3.E/§3.G/§3.H/§3.I | **PRODUCTION YAML guard**: loads real Skill YAMLs from classpath + asserts every declaration cell matches the audit's NEED column |

The NEGATIVE control proves the audit's central claim: a Skill that
needs a slot still receives it (no dropped signal). The DEFENSIVE
control proves the gate cannot regress legacy/unmapped sessions. The
KEEP-UNCONDITIONAL control proves the 4 audit-deferred slots are
unaffected by the gate. The PRODUCTION YAML guard is the regression
guard against future stale declarations.

**Mocked-LLM covers wiring ONLY** (per §5.6 / sprint contract).
Behaviour-risk gates: §6.3 below.

## 3. §4.1 anti-hardcode self-walk (per §7 stanza + §4.3 per-sub-sprint Codex review at S4 close)

The S4 deliver-agent + human dispatch a per-sub-sprint Codex review
at close per `milestone_objective.md` §8 + `iteration_governance.md`
§4.3 (S4 is semantic-touching + the highest-risk M5 sub-sprint —
same per-sub-sprint trigger as S3). Self-walk below is the dev-agent
input for that review.

1. **Does the PR add a keyword / regex / if-else / enum / per-UC
   matrix for a semantic decision?** No. Phase B adds two private
   helpers that read **registry data** (Skill YAML declarations) via
   `skillRegistry.select(phase, uc).map(skill -> skill.requiredContextKeys() / skill.stateInheritance().softSignalViaProjection())`.
   The gating decision is "does this Skill declare this slot?" —
   registry membership, not a UC/keyword branch. The defensive
   fallback for unmapped tuples is a single uniform `true`, not a UC
   matrix.
2. **Tier-0 invariant justification?** N/A — no new Tier-0 invariant
   added. Projection is inside Runtime's "trace and eval contract"
   (§1.4); the LLM owns soft-signal interpretation (§1.3). The
   constitution's "prompt_projection owns whether the LLM has the
   inputs to make a correct semantic choice" governs.
3. **Could the same outcome be achieved by projecting a soft signal?**
   The change IS the convergence to a registry/Skill-declaration-driven
   projection (S3's M2-correct steady state extended to context-key +
   soft-signal slots). The declarations themselves are registry data,
   LLM-soft (the LLM owns whether to act on the projected signals).
4. **Encode visible-eval case text / trace-specific phrasing /
   CaseSpec id into runtime, prompt, or judge config?** No. Zero
   touch to `eval_interactive/case_specs/**`, `composite.py`, judge
   fixtures, or prompts.
5. **Does the change move semantic ownership from LLM to Java?** No.
   The opposite: moves projection-shape decisions from
   unconditional/UC-driven Java code to the Skill registry (YAML
   declarations the LLM operates within). The LLM still owns
   classification, escalation, response strategy, and soft-signal
   reads.
6. **Add an if-else block to the prompt instead of principle-level
   guidance?** No prompt change.
7. **Preserve tool schema, capability / permission boundary, PII /
   safety floor, and grounding floor?** Yes.
   - `tool_schemas`: untouched (S3 C2 #4 already converged).
   - PII redaction: untouched.
   - Grounding floor (FAQ-grounding overlay path):
     UNTOUCHED per OQ-S51.2 disposition (a) (S3 close).
   - Tier-0 safety hard_checks schema unchanged.
   - S2 surface (`bot_turn_llm_calls`, `/trace`, `AgentRunLoopImpl`,
     `TraceWriter`, `mergeFaqGroundingIntoProjection`): UNTOUCHED.
8. **Ship generalization eval coverage — target, neighbor, negative,
   shadow?** Java wiring coverage shipped (11 tests cover POSITIVE
   [3 tests] + NEGATIVE [4 tests] + DEFENSIVE [2 tests] +
   KEEP-UNCONDITIONAL [1 test] + PRODUCTION YAML guard [1 test]). The
   **NEGATIVE control** is the key contract: a Skill that NEEDS a
   slot still receives it (and a Skill that does NOT declare a slot
   does NOT receive it). The real-LLM bad-case rerun + shadow rerun
   are STOP-AND-SURFACED to deliver-agent — see §6.3.
9. **If temporary, rollback / sunset plan?** Not temporary. Phase B
   is the M2-correct steady-state convergence per the C1 (S3 matrix)
   + Phase-A (this sub-sprint) audit chain. The four GATE-routed
   slots' declarations are intentional and match per-Skill NEED.

**Expected Codex verdict at S4 close:** `approve` for the code/
anti-hardcode axis. The real-LLM rerun is STOP-AND-SURFACED to
deliver-agent (§6.3 below); Codex re-review on the rerun verdict
follows the same close-package-timing pattern S3 used (per memory
`feedback_milestone_close_bad_case_before_codex`).

## 4. Hard fences honoured

Per `docs/sprint_objective.md` "Hard fences / STOP conditions":

- ✅ **AUDIT BEFORE GATING** — Phase-A audit
  `docs/diagnostics/m5-s4-skill-declaration-audit.md` (462 lines)
  delivered AND reviewed via `AskUserQuestion` BEFORE any Phase-B
  code edit. Human chose option (a) "Land Phase B per audit"
  2026-05-25.
- ✅ **Registry/Skill-driven — no per-UC if-else** — Phase B gates
  read `skillRegistry.select(...).map(Skill::requiredContextKeys / stateInheritance().softSignalViaProjection())`;
  no keyword/regex/enum/per-UC matrix added. The defensive fallback
  for unmapped tuples is a single uniform `true`.
- ✅ **No change to LLM-visible semantic information** — gating
  changes WHETHER a slot is projected for a Skill (per declaration);
  it does NOT change WHETHER the LLM can see a semantic signal it
  could see before. Empirically verified on the 5 bad-case sessions
  that completed turns on the S4 build (§6.2):
  - DISCOVER turn (cs011 turn 1): all 3 DISCOVER soft signals
    PRESENT (`candidate_use_cases`, `alternate_candidate_use_cases`,
    `discover_disambiguation_signals`); `prior_use_case_carry`
    ABSENT (discover_triage doesn't declare). ✓
  - RESOLVE turns (alice, cs001, cs014, cs066, cs095 — last-turn
    projections): all 3 DISCOVER soft signals ABSENT;
    `prior_use_case_carry` PRESENT (resolve_faq + resolve_intake
    declare). ✓
- ✅ **No `escalation_reason` enum / tool-schema / PII / safety /
  grounding-floor change** — none touched. Zero edits under
  `eval_interactive/case_specs/**`, `composite.py`, eval fixtures.
- ✅ **OQ-S51.2 FAQ-grounding overlay UNTOUCHED** — explicit
  preservation per S3 close human option-(a) disposition.
- ✅ **S2 `bot_turn_llm_calls` schema + `/trace` endpoint shape
  UNTOUCHED** — Phase B is read-only consume of S2's per-step
  projections (in the empirical-verification §6.2 below).
- ✅ **#4 CONDITIONAL DEFERRED — not expanded into S4** — per
  default (C3 dedup + OQ-S52.4 `knowledge_hits` canonicalization
  routed to S5 / future milestone).
- ⚠️ **STOP-and-surface for the real-LLM rerun** — see §6.3. The
  rerun was attempted on the S4 build but is NOT interpretable as
  Phase-B evidence due to documented upstream LLM provider transport
  failure (deepseek primary + kimi fallback both exhausted /
  fallback-failed; 128 transport errors logged since 09:24 CST on
  the S4 backend). The 5 cases that DID complete turns showed the
  Phase-B gating working byte-correctly. The 7 contract_violations
  are categorically upstream of Phase B (the bot returned its
  generic "Sorry, I'm having trouble reaching the assistant right
  now" fallback in every contract-violation case).
- ⚠️ **STOP-and-surface for the shadow rerun** — per the
  dev-blind contract (sprint contract §"Test/eval requirements"),
  the shadow rerun is deliver-agent / human run only. Dev does NOT
  read shadow case content / results.

## 5. Files touched + footprint

Numstat (per `git diff --numstat` at sprint close, against HEAD `49d48b1`):

| File | Insertions | Deletions | Kind |
|---|---|---|---|
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | 148 | 26 | Phase B #2 + #5 gates + 2 helpers |
| `docs/diagnostics/m5-s4-skill-declaration-audit.md` | 462 | 0 | NEW (Phase-A deliverable) |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint53SkillDeclarationGatingTest.java` | 528 | 0 | NEW (11 wiring/rendering tests + 1 production-YAML guard) |
| `docs/sprints/sprint-053-handoff.md` | (this file) | 0 | NEW (dev handoff) |

S4 scope only: **zero YAML edits** (audit concluded #1a = zero). No
edits under `eval_interactive/`, `data/`, `ui/`, `config/**`,
`composite.py`, or any other surface beyond the four named in the
sprint-contract Commit-discipline §.

## 6. Baselines + gates

### 6.1 Java suite

`mvn test -B` against HEAD `49d48b1` + S4 changes → **`Tests run:
1183, Failures: 1, Errors: 0, Skipped: 2`**.

- **`+11` vs the `1172` baseline** = the 11 new `Sprint53SkillDeclarationGatingTest`
  tests (all PASS).
- The lone failure is the **inherited
  `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`**
  (OQ-S41.5 STATUS QUO, persists from before Sprint 24). **No NEW
  Java regression.**

### 6.2 Empirical Phase-B gating verification (the 5 cases that completed turns)

The five cases that completed turns on the S4 build (alice, cs001,
cs014, cs066, cs095) had their LAST-turn projections inspected via
the `/v1/demo/sessions/{id}/trace` endpoint (S2-added per-step
projection in `bot_turn_llm_calls.projection`). Per-case verdict:

| Case | Last-turn phase | active_uc | Skill (per `skillRegistry.select`) | `candidate_use_cases` | `alternate_candidate_use_cases` | `discover_disambiguation_signals` | `prior_use_case_carry` |
|---|---|---|---|---|---|---|---|
| alice | RESOLVE | UC-A | resolve_faq_grounded_answer | **absent** ✓ | **absent** ✓ | **absent** ✓ | **present** ✓ |
| cs001 | RESOLVE | UC-C | resolve_faq_grounded_answer | absent ✓ | absent ✓ | absent ✓ | present ✓ |
| cs014 | RESOLVE | UC-C | resolve_faq_grounded_answer | absent ✓ | absent ✓ | absent ✓ | present ✓ |
| cs066 | RESOLVE | UC-K | resolve_intake_collect_and_handover | absent ✓ | absent ✓ | absent ✓ | present ✓ |
| cs095 | RESOLVE | UC-C | resolve_faq_grounded_answer | absent ✓ | absent ✓ | absent ✓ | present ✓ |

Additionally one DISCOVER turn was sampled (cs011 turn 1 via httpx
probe of `/v1/demo/sessions/{id}/trace`):

| Case/turn | Phase | active_uc | Skill | `candidate_use_cases` | `alternate_candidate_use_cases` | `discover_disambiguation_signals` | `prior_use_case_carry` |
|---|---|---|---|---|---|---|---|
| cs011 turn 1 | DISCOVER | null (pre-classification) | discover_triage (wildcard match) | **present (empty array)** ✓ | **present (empty array)** ✓ | **present (sub-fields null)** ✓ | **absent** ✓ |

The runtime behaviour matches the Phase-A audit's predicted
declaration coverage **exactly**. The gating is byte-correct and
preserves all LLM-visible signals per the audit's NEED column.

### 6.3 Real-LLM bad-case rerun — REGRESSION-SAFE PASS (recovered-LLM re-run)

**Initial real-LLM rerun on the S4 build (09:20 CST) was
STOP-AND-SURFACED due to upstream LLM provider transport failure
(deepseek primary + kimi fallback both exhausted; 128 transport
errors logged since 09:24 CST). The provider recovered later in
the session; a re-run on the healthy provider (12:15 CST onward,
results `20260525-s4-main-run-recovered-llm/`) reproduces the
M4-close distribution shape and is the Phase-B evidence gate
recorded here. The original-degraded run and its iso-reruns are
preserved for forensic comparison; the verdict below uses the
recovered-LLM run.**

**Setup**: backend restarted on the S4 build at 09:19 CST
(`make backend`; PID 87647 logging to `/tmp/s4-backend.log`); javap
confirmed `skillRequiresContextKey` + `skillDeclaresSoftSignal`
compiled into `target/classes`; bot LLM provider lineup logged at
startup: `primary=deepseek[model=deepseek-v4-flash], fallback=kimi[model=kimi-k2.6]`.

**Initial main run on degraded LLM provider**
(`results/20260525-s4-main-run/`, 09:20 CST,
`cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1`):

| Case | M4-close baseline | S4 initial run | Disposition |
|---|---|---|---|
| alice | IMPROVING | bot_ended turns=2 (HUMAN_REVIEW, composite=0.5) | within baseline shape |
| cs001 | PASS | loop_detected turns=3 | within baseline shape |
| cs011 | IMPROVING | **CONTRACT_VIOLATION (active_use_case missing)** | upstream LLM failure |
| cs012 | IMPROVING | **CONTRACT_VIOLATION** | upstream LLM failure |
| cs014 | PASS | loop_detected turns=2 | within baseline shape |
| cs015 | FAIL | **CONTRACT_VIOLATION** | upstream LLM failure |
| cs029 | PASS | **CONTRACT_VIOLATION** | upstream LLM failure |
| cs066 | PASS | loop_detected turns=2 | within baseline shape |
| cs095 | FAIL | loop_detected turns=2 | within baseline shape |
| fg5q | PASS | **CONTRACT_VIOLATION** | upstream LLM failure |
| iwzx | FAIL | **CONTRACT_VIOLATION** | upstream LLM failure |
| wmkb | IMPROVING | **CONTRACT_VIOLATION** | upstream LLM failure |

**Isolated reruns** of all 7 CONTRACT_VIOLATION cases (`parallel=1`,
`results/20260525-s4-iso-rerun-*`, 09:24-09:37 CST on the still-
degraded provider): **all 7 still CONTRACT_VIOLATION**, NOT clearing
on isolated rerun. cs011 was re-attempted **3× more** in isolation
— all 3 still CONTRACT_VIOLATION (results
`20260525-024143`, `20260525-024146`, `20260525-024148`). This is
**inconsistent with the documented intermittent
`R-bad-case-parallel-session-establishment-flakiness` shape** (S3
close had 2/12 contract violations at parallel=1, both cleared on
1-2× isolated rerun) — consistent with a categorically-different
upstream failure mode (LLM transport vs session-establishment timing).

**Recovered-LLM re-run**
(`results/20260525-s4-main-run-recovered-llm/`, 12:15 CST, after the
upstream LLM provider recovered — deepseek probe at 12:14 CST
returned `200 OK` content=`READY`):

| Case | M4-close baseline | S4 recovered-LLM | Disposition |
|---|---|---|---|
| alice | IMPROVING | bot_ended turns=2 composite=0.5 | matches baseline ✓ |
| cs001 | PASS | bot_ended turns=4 composite=0.0 | matches baseline ✓ |
| cs011 | IMPROVING | bot_ended turns=3 composite=0.0 | matches baseline ✓ (cleared upstream flake) |
| cs012 | IMPROVING | bot_ended turns=2 composite=0.5 | matches baseline ✓ (cleared) |
| cs014 | PASS | bot_ended turns=2 composite=0.0 | matches baseline ✓ |
| cs015 | FAIL | **contract_violation** → iso-rerun #2 bot_ended turns=2 composite=0.5 | session-establishment flake, cleared 2× iso ✓ |
| cs029 | PASS | bot_ended turns=2 composite=0.0 | matches baseline ✓ (cleared) |
| cs066 | PASS | bot_ended turns=3 composite=0.0 | matches baseline ✓ |
| cs095 | FAIL | bot_ended turns=3 composite=0.5 | matches baseline ✓ |
| fg5q | PASS | **contract_violation** → iso-rerun #2 bot_ended turns=2 composite=0.5 | session-establishment flake, cleared 2× iso ✓ (S3-close also flaked on fg5q) |
| iwzx | FAIL | bot_ended turns=6 composite=0.0 | matches baseline ✓ |
| wmkb | IMPROVING | bot_ended turns=2 composite=0.5 | matches baseline ✓ (cleared) |

**Iso-rerun verdicts** for the two recovered-LLM flakes
(`results/20260525-s4-recovered-iso-*`):

- cs015: attempt #1 CONTRACT_VIOLATION → **attempt #2 bot_ended
  turns=2 composite=0.5** ✓ cleared.
- fg5q: attempt #1 CONTRACT_VIOLATION → **attempt #2 bot_ended
  turns=2 composite=0.5** ✓ cleared.

Both clear on 2× isolated rerun — **EXACTLY the documented
`R-bad-case-parallel-session-establishment-flakiness` shape** that
S3 close encountered (S3: fg5q + iwzx; S4 recovered-LLM: cs015 +
fg5q). The `fg5q` flake is now a recurrent observation across S3 +
S4 — recommend the existing R-item priority bump (OQ-S53.1 below).

**Distribution verdict: HOLDS the M4-close baseline shape**
(PASS×5 + IMPROVING×4 + FAIL×3 + OOSR×0) when the two
session-establishment flakes are isolated per the documented flake
disposition. **The decisive signal — outcome class — is stable**
across all 12 cases: zero resolve↔escalate flips; every case reaches
its baseline-shape terminal disposition on the run (10) or on a
2× iso rerun (2).

**Phase B gating runtime-verification across the recovered-LLM
run** (last-turn projection per case via the S2 `bot_turn_llm_calls.projection`
column on the trace endpoint):

| Case | Last-turn phase | active_uc | Skill | `candidate_use_cases` | `alternate_candidate_use_cases` | `discover_disambiguation_signals` | `prior_use_case_carry` |
|---|---|---|---|---|---|---|---|
| alice | RESOLVE | UC-A | resolve_faq | absent ✓ | absent ✓ | absent ✓ | EMIT ✓ |
| cs001 | RESOLVE | UC-C | resolve_faq | absent ✓ | absent ✓ | absent ✓ | EMIT ✓ |
| cs011 | RESOLVE | UC-D | resolve_faq | absent ✓ | absent ✓ | absent ✓ | EMIT ✓ |
| cs012 | RESOLVE | UC-A | resolve_faq | absent ✓ | absent ✓ | absent ✓ | EMIT ✓ |
| cs029 | RESOLVE | UC-D | resolve_faq | absent ✓ | absent ✓ | absent ✓ | EMIT ✓ |
| cs066 | RESOLVE | UC-K | resolve_intake | absent ✓ | absent ✓ | absent ✓ | EMIT ✓ |
| cs095 | RESOLVE | UC-C | resolve_faq | absent ✓ | absent ✓ | absent ✓ | EMIT ✓ |
| iwzx | RESOLVE | UC-H | resolve_intake | absent ✓ | absent ✓ | absent ✓ | EMIT ✓ |
| wmkb | RESOLVE | UC-D | resolve_faq | absent ✓ | absent ✓ | absent ✓ | EMIT ✓ |

Every cell matches the Phase-A audit's NEED column exactly: the 3
DISCOVER-only soft signals are gated out for RESOLVE Skills (per
§3.E/§3.G/§3.H); `prior_use_case_carry` emits for resolve_faq +
resolve_intake (per §3.I). Combined with the earlier degraded-LLM
sample of a DISCOVER turn (cs011 turn 1 via the httpx probe of
the trace endpoint showed all 3 DISCOVER soft signals PRESENT +
`prior_use_case_carry` ABSENT for discover_triage), the gating is
empirically verified working byte-correctly in BOTH directions
across BOTH backend builds.

**No Phase-B-attributable regression observed**: the recovered-LLM
distribution reproduces M4-close, outcome class is stable, the 2
flakes match the documented session-establishment shape (clear on
iso), and the gating empirically matches the audit's predicted
declaration coverage across all 9 cases that captured a last-turn
projection.

**Original-degraded-run analysis (preserved for forensic comparison)**:
backend log evidence (`/tmp/s4-backend.log`):

- 128 errors matching `chat:exhausted|fallback-failed|fallback-skipped-deadline`
  since 09:24 CST.
- Concrete failure modes logged: `LLM [chat:fallback-skipped-deadline-exceeded]
  primary=deepseek fallback=kimi remaining_budget_ms=-172337
  retry_decision=fallback_skipped_deadline_exceeded failure_class=RuntimeException`;
  `LLM [chat:fallback-failed] both primary=deepseek and fallback=kimi
  failed`; `LLM API call failed after retry (provider=kimi)`;
  `LLM [chat:request] provider=kimi ... LLM [chat:exhausted] provider=kimi
  after 1 attempts ... retry_decision=exhausted`; `failure_class=connect_timeout`.
- Bot's response on every CONTRACT_VIOLATION turn (sampled via the
  S2 `/trace` endpoint on cs011 session
  `69f4b6ff-b3f3-4894-8ce3-5f2e8575d1e8`): `"Sorry, I'm having
  trouble reaching the assistant right now. Please try again in a
  moment."` — the runtime's standard LLM-unavailable fallback
  message. The LLM was never reached; the bot cannot have committed
  a UC.
- Note: S3-close rerun used **moonshot-v1-32k as bot model**
  (`sprint-052-handoff.md §12.2`); the S4-build backend default config
  is **deepseek-v4-flash primary + kimi-k2.6 fallback**. Provider
  lineup drifted between S3 close and now — an external infrastructure
  shift outside Sprint 53 scope.

**Phase B causality ruled out**:

1. The Phase B code path is invoked during LLM-input projection
   construction (per-turn). The "Sorry, I'm having trouble" fallback
   is returned BEFORE Phase B's gating code runs (the LLM call itself
   failed at transport-level).
2. The 5 cases that DID complete turns empirically verified Phase B
   gating works correctly (§6.2 above).
3. Per Sprint 31 §13 + Sprint 32 Codex re-run evidence: external LLM
   provider drift is a documented confounding source that
   `iteration_governance.md §5.5` formalized as the rationale for
   demoting the smoke composite_score to observation — "a metric that
   cannot be reliably attributed to sprint-side causes cannot gate
   sprint close". The same principle applies here to the bad-case
   distribution: the 7 CONTRACT_VIOLATIONs cannot be attributed to
   Phase B because the LLM transport never landed.

**Verdict (recovered-LLM rerun)**: **Regression-safe PASS**. The
recovered-LLM rerun reproduces the M4-close distribution shape;
the 2 flakes match the documented `R-bad-case-parallel-session-establishment-flakiness`
pattern (clear on 2× iso); Phase B gating is empirically verified
working in both directions; outcome class is stable across all 12
cases; no Phase-B-attributable degradation observed. The
human-judgment §5.6 review can proceed against the recovered-LLM
data at deliver-agent S4 close.

The Phase B Java tests (§6.1) + the empirical §6.2 + the audit are
the dev-side evidence the gate is byte-correct. The recovered-LLM
rerun (this §6.3) + the runtime Phase-B verification table above
are the eval-evidence that the gate is semantic-preserving on the
bad-case distribution.

### 6.4 Shadow rerun — STOP-AND-SURFACED to deliver-agent (dev-blind)

Per the sprint contract: *"the held-out `case_specs_shadow/` set is
**dev-blind** — you do NOT read shadow case content or results;
STOP-surface the shadow rerun to the deliver-agent at close
(deliver-agent / human run + read it), exactly as the bad-case
eyeball pattern."*

The shadow rerun is **STOP-AND-SURFACED to deliver-agent + human at
S4 close**. The dev session did NOT read shadow content / results.
Deliver-agent / human run the shadow set on the same healthy-LLM
backend used for the re-run of §6.3, with the same disambiguation
protocol if upstream failure recurs.

### 6.5 Python

`cd eval_interactive && uv run python -m pytest tests/ --tb=no -q` →
**`3 failed, 486 passed`**. **UNCHANGED vs S3-close baseline** (S4
did not touch eval-harness Python).

The 3 failures are env-specific per OQ-S47.3:
`tests/regression/test_case_spec_overrides.py::test_v2_schema_loads_cleanly`,
`test_smoke_review_report_tracks_smoke_set_and_overrides`, and
`tests/regression/test_corpus_lint.py::test_full_corpus_lints_clean_with_smoke_subset_flag`.

### 6.6 Tier-0 safety floor + grounding floor

UNTOUCHED. No edits to `FaqGroundingDiagnostics`,
`SourceEvidenceLineage`, `FaqOutputClassifier`, the
`mergeFaqGroundingIntoProjection` overlay, PII redaction, or any
Tier-0 hard_check schema.

### 6.7 Codex §4.1 anti-hardcode kernel

§3 self-walk above. Expected `approve` for the code axis at S4
close per `milestone_objective.md` §8 + §4.3 per-sub-sprint review
trigger.

## 7. R-items consumed / surfaced

**Consumed**: No R-items consumed in S4 (M5 scope; OQ-S52.1 / OQ-S52.2
were sprint-objective-tracked open questions consumed by S4 design,
not standalone R-items).

**Surfaced** (handoff-only; deliver-agent decides whether to open
R-items at S4 close):

- **OQ-S53.1** — `R-bad-case-parallel-session-establishment-flakiness`
  PRIORITY-BUMP-CONSIDERATION: at S4 the documented flake shape
  presented as a 7/12 CONTRACT_VIOLATION rate that DID NOT clear on
  isolated rerun, distinguishing it from the prior S3-close
  presentation (2/12 cleared 1-2× iso). Root-caused to upstream LLM
  transport failure (not the flake shape). Recommend recording this
  data point on the R-item if it remains open.
- **OQ-S53.2** — Bot LLM provider lineup drifted from S3-close
  (`moonshot-v1-32k` primary) to today's config
  (`deepseek-v4-flash` primary + `kimi-k2.6` fallback). External
  infrastructure change; not in Sprint 53 scope but worth recording
  as an observability data point (consistent with
  `iteration_governance.md §5.5` external-provider-drift confounding
  source).
- **OQ-S53.3** — C3 dedup/denoise + OQ-S52.4 `knowledge_hits`
  canonicalization remain deferred (per #4 CONDITIONAL default).
  Deliver-agent + human decide at S4 close whether S5 picks them up
  or whether they spin into a separate "projection hygiene"
  milestone per §8.5 footnote.

## 8. OQ (open questions)

- **OQ-S53.1 / OQ-S53.2 / OQ-S53.3** — see §7 above.
- **OQ-S52.1 / OQ-S52.2** — CONSUMED by S4 (Phase B #2 + #5
  landed); the audit + Phase B answers the "is the convergence
  shape correct?" question. The "does the rerun confirm
  semantic-preservation?" question is STOP-AND-SURFACED via §6.3.
- **OQ-S52.4** (`knowledge_hits` dual-path) — DEFERRED to OQ-S53.3
  per #4 CONDITIONAL default.

## 9. Backend / dev-sandbox context

- Backend at `http://localhost:8080/actuator/health` UP at S4 close,
  running on the S4 build (HEAD `49d48b1` + uncommitted S4 Java
  diff; `javap` confirmed `skillRequiresContextKey` +
  `skillDeclaresSoftSignal` in `target/classes` on both backend
  invocations).
- Backend invocations during the dev session:
  - **PID 87647** (09:19 → 12:10 CST): initial S4-build backend
    used for the initial bad-case rerun (§6.3 initial run +
    iso-reruns). LLM provider lineup
    `primary=deepseek[model=deepseek-v4-flash], fallback=kimi[model=kimi-k2.6]`
    per startup log; degraded during 09:24-12:10 CST per §6.3
    forensic evidence. Backend exited (mvn process terminated by
    signal 137 — likely OOM under repeated LLM-retry storms;
    forensic context for the deliver-agent's R-item bump
    consideration).
  - **PID 94828** (~12:11 CST → ongoing at S4 close): fresh S4-build
    backend (re-launched by user before the recovered-LLM rerun;
    same `target/classes`; `javap` re-verified). Used for the
    recovered-LLM rerun + iso reruns in §6.3.
- LLM provider HEALTH at S4 close: recovered (direct deepseek
  probe at 12:14 CST returned 200 OK content=`READY`; bad-case
  rerun on the recovered provider reproduces the M4-close
  distribution shape). The Sprint 31 §13 / §5.5 external-provider-
  drift confounding source pattern was active during the 09:24-12:10
  window; the deliver-agent's bad-case re-run at close should
  re-verify provider health before drawing distribution conclusions.

## 10. Self-check vs sprint-contract closing checklist

- [x] Phase-A audit matrix delivered + reviewed by deliver-agent +
  human BEFORE any Phase-B gating? YES — `docs/diagnostics/m5-s4-skill-declaration-audit.md`
  + `AskUserQuestion` 2026-05-25 chose option (a).
- [x] Declarations COMPLETED (#1a) so gating drops no LLM-visible
  signal? YES — audit concluded zero YAML edits needed for the
  4 GATE-routed slots (declarations match NEED); the 4 KEEP-UNCONDITIONAL
  slots stay unconditional to preserve INHERIT-RISK / dropped-signal
  guards (per audit §4.B).
- [x] #2 + #3 gating is registry/Skill-driven — zero per-UC if-else
  / keyword / enum added? YES (§3 self-walk Q1 + Q5).
- [x] NEGATIVE control proven: a Skill that needs a slot still
  receives it (no dropped signal)? YES — Java NEGATIVE control tests
  (4 cases in `Sprint53SkillDeclarationGatingTest`) +
  `productionSkillYamls_matchAuditDeclarationCoverage` proves the
  YAML declarations match audit. Empirically: §6.2 verifies the
  gating's runtime behaviour matches the audit's predicted
  declaration coverage exactly for both DISCOVER (cs011 turn 1) and
  RESOLVE (5 completed cases).
- [x] #4 (C3 + `knowledge_hits`) deferred (not expanded into S4)?
  YES — per default.
- [x] Java: no NEW regression; new wiring tests PASS? YES — `1183 /
  1-inherited / 0 / 2` (+11 new Sprint53 tests; lone failure is
  OQ-S41.5).
- [x] **Real-LLM bad-case rerun holds the M4-close distribution**
  (PASS×5 / IMPROVING×4 / FAIL×3 / OOSR×0)? YES — recovered-LLM
  rerun at 12:15 CST after provider recovered reproduces the
  M4-close distribution shape; 10/12 cases bot_ended on the main
  run, 2/12 hit documented `R-bad-case-parallel-session-establishment-flakiness`
  (cs015 + fg5q) and BOTH cleared on 2× iso rerun. Outcome class
  stable; no Phase-B-attributable degradation. The original-degraded
  rerun and its iso-reruns are preserved for forensic comparison
  (see §6.3 + §11 result dirs).
- [STOP-SURFACED] **Shadow rerun STOP-surfaced to deliver-agent**
  (dev-blind)? YES — per §6.4 + the dev-blind contract.
- [x] §7 stanza filled + §4.1 self-walk written for the S4-close
  Codex review? YES — `docs/sprint_objective.md` §7 stanza already
  binding; §3 of this handoff is the per-sub-sprint self-walk.
- [x] Handoff §1-§11 complete; §12 reserved; staged only S4-scope
  files? YES — §12 reserved for deliver-agent at close.

## 11. Commit / bundling note

S4 dev work is UNCOMMITTED in the working tree at handoff
(commit-at-end per `feedback_commit_at_end_bundles_deliver_artefacts.md`).
The dev staged-scope ONLY per `sprint_objective.md` "Commit
discipline":

- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint53SkillDeclarationGatingTest.java`
- `docs/diagnostics/m5-s4-skill-declaration-audit.md`
- `docs/sprints/sprint-053-handoff.md`

**No YAML edits** (#1a result; per audit). **No `git add -A`**.
Deliver-agent close-bundle files (objective archive, `10-handoff.md`
§0 + §1 refresh, `action_bank.md` §6 Sprint 53 row, the
per-sub-sprint `compact/sprint-053-codex-review-prompt.md`, and the
real-LLM rerun + shadow rerun results) are bundled by the human at
close per the established pattern.

**Result dirs for deliver-agent review** (under
`eval_interactive/results/`):

Primary (the §6.3 verdict source):

- `20260525-s4-main-run-recovered-llm/` — 12-case main run on the
  S4 build with healthy LLM provider (12:15 CST). 10/12 cases
  bot_ended cleanly; 2/12 (cs015 + fg5q) hit the documented
  session-establishment flake.
- `20260525-s4-recovered-iso-20260525-042046/` (cs015 #2 PASS) +
  `20260525-s4-recovered-iso-20260525-042122/` (fg5q #2 PASS) —
  the iso reruns that cleared the 2 flakes on attempt 2.
- `20260525-s4-recovered-iso-20260525-042035/` (cs015 #1 CONTRACT)
  + `20260525-s4-recovered-iso-20260525-042116/` (fg5q #1 CONTRACT)
  — preserved as the flake-shape evidence (CONTRACT on attempt 1,
  PASS on attempt 2 — the documented intermittent shape).

Forensic / preserved for comparison (the original-degraded run that
prompted the initial STOP-and-surface):

- `20260525-s4-main-run/` — initial parallel=1 run on the
  S4 build during upstream LLM provider degradation (09:20 CST).
  5/12 completed turns (alice, cs001, cs014, cs066, cs095);
  7/12 CONTRACT_VIOLATION categorically upstream of Phase B (bot
  returned its LLM-unavailable fallback message).
- `20260525-s4-iso-rerun-20260525-023652/` (cs011) →
  `20260525-s4-iso-rerun-20260525-023710/` (wmkb) — single-case
  iso reruns of the 7 degraded-LLM CONTRACT_VIOLATION cases. All 7
  still CONTRACT_VIOLATION during the LLM degradation window —
  distinguished from the post-recovery session-establishment flake
  (which clears on iso).
- `20260525-s4-iso-rerun-20260525-024143/`,
  `20260525-024146/`, `20260525-024148/` — three additional iso
  reruns of cs011 during LLM degradation (all CONTRACT_VIOLATION;
  consistent failure under upstream outage).

Backend log preserved at `/tmp/s4-backend.log` (128 LLM
transport-failure entries during the 09:24-12:10 CST degradation
window; available for deliver-agent / Codex independent verification).
The recovered-LLM rerun ran against a different backend invocation
(PID 94828; same S4 target/classes, healthy LLM lineup) per §9.

## 12. Sub-sprint close verdict (deliver-agent + human)

**Verdict: S4 (Sprint 53) = A — Clean PASS** (pending the combined Codex
`pass / 0`). This close ALSO closes **Milestone M5 — Observability
Coherence** (human decision 2026-05-25: close M5 at S4; the deferred #4
C3-dedup + `knowledge_hits` canonicalization routes to a separate
"projection hygiene" milestone candidate, NOT an S5). Recorded jointly by
the deliver-agent + human 2026-05-25.

### 12.1 Deliver-agent independent verification (not dev self-report)

- **Footprint / fences** — `git diff --numstat HEAD` = `ContextProjectionBuilder.java`
  `+148/-26` only; audit + test + handoff untracked; **zero YAML edits**
  (confirms #1a = 0); `git status --porcelain` clean on `skills/`,
  `eval_interactive/`, `data/`, `ui/`, `config/`, `composite.py`.
- **Code axis (§1.7 + null-safety)** — both helpers gate on registry-data
  membership (`Skill.requiredContextKeys()` / `stateInheritance().softSignalViaProjection()`)
  with a single uniform defensive `true` fallback for null-registry / null-session
  / null-phase / empty-`select`; no per-UC if-else / keyword / enum. Null-safe by
  construction: `StateInheritance`'s compact constructor normalises
  `softSignalViaProjection` to `List.of()` and `Skill` normalises
  `stateInheritance` to `EMPTY` / `requiredContextKeys` to `List.of()`, so the
  no-null-guard on `skillDeclaresSoftSignal`'s terminal `.contains(...)` cannot
  NPE.
- **Java suite** — reproduced `mvn test -B` → `Tests run: 1183, Failures: 1,
  Errors: 0, Skipped: 2` (+11 new `Sprint53SkillDeclarationGatingTest`; lone
  failure = inherited `SystemPromptUserRequestedTiebreakerTest`, OQ-S41.5 STATUS
  QUO). **No NEW regression.**
- **Backend on S4 build** — `javap -p target/classes …ContextProjectionBuilder`
  shows both `skillRequiresContextKey` + `skillDeclaresSoftSignal`; health UP.

### 12.2 §5.6 bad-case + shadow regression-safety gate — PASS (joint; human concurred)

Recorded in `eval_interactive/case_specs/bad_cases/_manifest.md` →
"M5 milestone close + S4 (Sprint 53)" section.

- **Bad-case (recovered-LLM)** — deliver-agent corroborated the dev's §6.3
  distribution from `results/20260525-s4-main-run-recovered-llm/results.json`:
  10/12 `bot_ended`, 2/12 (cs015 + fg5q) the session-establishment flake, both
  **cleared on iso #2** (`…-recovered-iso-…042046` / `…042122`, composite 0.5).
  Distribution reproduces M4-close exactly (PASS×5 / IMPROVING×4 / FAIL×3 /
  OOSR×0); outcome class stable.
- **Shadow (NEW mandatory S4 gate; deliver-agent ran, dev-blind)** — 22 held-out
  cases at parallel=1, `results/20260525-094611/`. **19/22 reached the bot with
  healthy multi-turn outcomes** (mean_outcome 0.711, mean_turns 3.1). The 3
  outcome-0 cases are ALL session-establishment failures where the bot was never
  reached (`turns_traced=0`, `elapsed_ms=0`): cs32s02 (contract → cleared on iso)
  + cs59s01/cs59s02 (deterministic HTTP-400 on empty-form session-create — a
  pre-existing shadow-fixture issue, reproduced on iso). **No S4-attributable
  regression** — the gating runs only inside a bot turn. Programmatic 0/22 is the
  empty-scoring-schema artifact (observation-only per §5.5/§5.6).
- **Provider context** — the dev's initial 09:20 run was STOP-and-surfaced under
  upstream bot-LLM degradation; the recovered-LLM run (~12:14 onward) is the
  evidence gate. Deliver-agent re-verified provider health on shadow case 1
  (`cs01s01` `bot_ended`, 2 turns) before trusting the distribution.

### 12.3 Codex review plan (combined, per human 2026-05-25)

ONE combined Codex review at M5 close (human chose "one combined review"):
the §4.1 nine-question kernel + §4.2 header over the **cumulative M5 commit
range `84ae017..<S4 dev commit>`**, S4-focused (the semantic surface), noting
S1/S2 are §7-exempt observation and S3 already passed per-sub-sprint
(`docs/sprints/sprint-052-codex-review.md`). Serves BOTH the mandated
per-sub-sprint S4 review (sprint objective §"Codex review plan") AND the §8
milestone-shared review. Archived to `docs/sprints/sprint-053-codex-review.md`
+ `docs/milestones/M5_codex-review.md`. **Dispatch discipline honoured** (memory
`feedback_milestone_close_bad_case_before_codex`): the bad-case + shadow rerun
evidence is recorded into `_manifest.md` + this §12 BEFORE Codex; the dev scope
is committed by the human BEFORE dispatch. Self-walk input for Codex = §3 of
this handoff. Expected verdict: `approve` (code axis).

### 12.4 R-items / dispositions surfaced (deliver-agent → action_bank at M5 close)

- **OQ-S53.1** — `R-bad-case-parallel-session-establishment-flakiness`: S4 adds
  data points (bad-case cs015/fg5q cleared on 2× iso; shadow cs32s02 cleared on
  iso). Keep open (already PRIORITY-BUMPED at S3 close).
- **NEW R-item candidate** — `R-shadow-fixture-empty-form-session-create-400`
  (cs59s01/cs59s02 deterministic 400 on empty `form_context`; eval-harness /
  fixture; low priority; 2 shadow cases cannot establish a session). Surfaced by
  running shadow dev-blind. Upstream of all bot behaviour.
- **OQ-S53.2** — bot-LLM provider drift (`moonshot-v1-32k` at S3 close →
  `deepseek-v4-flash`+`kimi-k2.6` at S4): observation-only, §5.5 confounding
  source.
- **OQ-S53.3 → RESOLVED** — #4 (C3 dedup/denoise + `knowledge_hits`
  canonicalization) routes to a separate "projection hygiene" milestone candidate
  (M6+), per human 2026-05-25. NOT an S5; M5 closes at 4 sub-sprints (within the
  §8.5 ceiling).
- **Downgrade eligibility** — cs001/cs014/cs029/cs066 now at 3 consecutive
  regression-safe closes (M3/M4/M5); downgrade DEFERRED to a future
  bad-case-driven milestone close (M5 is coherence, not bad-case-anchored).

### 12.5 Packaging / commit note

Dev staged S4 scope ONLY (4 files per §11; no `git add -A`; zero YAML). The
deliver-agent close-bundle (this §12 finalize; `_manifest.md` M5-close section;
`compact/sprint-053-codex-review-prompt.md`; and — AFTER the Codex pass — the M5
milestone-close artefacts: archive `sprint_objective.md`→`sprint-053-objective.md`
+ `milestone_objective.md`→`milestones/M5_objective.md` + `codex-findings.md`→
`milestones/M5_codex-review.md`, reset live scaffolds, `10-handoff.md` §0/§1/§2,
`action_bank.md` §6 row + §6.5 M5 closed-milestone row + R-item flips,
`sprint_objective.md`/`milestone_objective.md` reset to next-TBD) is bundled by
the human per the established commit-at-end pattern.

**Packaging note (A-with-packaging-note per `deliver_close_taxonomy.md`):** at
human direction (2026-05-25), the S4 pre-Codex commit ALSO bundles a
human-authored **governance-doc condensation** that was in the working tree,
distinct from S4's `prompt_projection` scope: `docs/current/iteration_governance.md`
(−146/+30 — condenses the intro, §4.1, §5.5/§5.6, §8.2; trims verbose evidence
to sprint-archive pointers) + NEW `compact/anti-hardcode-review-kernel.md` (the
§4.1 nine-question kernel EXTRACTED to a single canonical compact file, which
the doc now links to). Deliver-agent verified the extraction is **content-faithful**
— the kernel file holds the nine questions + scope exemption + four verdicts +
the "do not rewrite" footer byte-identical to the removed §4.1 block; the other
condensations preserve the normative rules (gates, human-judgment-gate principle,
milestone framework) and only trim narrative/evidence history. This is a
docs-only / config-governance change (§4.1-exempt) but rides in the Codex-reviewed
range; the Codex prompt §2 flags it for a content-preservation confirmation on the
constitution-tier doc. Scope-mix is a deliberate packaging choice (roll forward),
not a dev scope violation.
