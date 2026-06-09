Paste the content below this line into a fresh Claude Code session on branch `refactor/remove-the-shackles`.

---

# Sprint 37 dev prompt (Claude Code session) — NEW M2 sub-sprint 1 = Skill Registry + state-across-Skill design freeze

**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`
**Sub-sprint type:** docs-only design freeze (NO `server/` code; NO test code)
**Codex review:** per-sub-sprint at Sprint 37 close

You are dev-agent (Claude Code) for **NEW Sprint 37**, the FIRST sub-sprint of NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization). Your task is to produce ONE architectural decision doc that locks 10 design decisions covering all 4 downstream implementation sub-sprints (Sprints 38/39/40/41).

> **Important orientation:** there was an OLD Sprint 37 contract in `docs/sprint_objective.md` and an OLD Sprint 37 dev prompt at this file path (`compact/sprint-037-dev-prompt.md`) drafted for OLD M2-Skill framing (S1 implementation per minimum-surface incrementalism). The OLD framing was wholesale superseded on 2026-05-17 per human direction. NEW Sprint 37 is fundamentally different: it's a **docs-only design freeze** for a **Skill Registry abstraction** (first-class externalized Skill definitions). Read the NEW contract at `docs/sprint_objective.md` FIRST; do NOT proceed from memory of the OLD prompt's framing.

---

## 1. Read order on session start

Read these in order before doing anything else. Cite each file in your handoff §1 Context Pack as you read it.

1. `AGENTS.md` (auto-loaded transitively: `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md`).
2. `docs/sprint_objective.md` — NEW Sprint 37 contract (the binding scope for this sub-sprint). Pay attention to §2 Goal (10 design decisions a-j), §3 Non-goals, §6 hard fences, §7 bundle policy, §10 stop conditions.
3. `docs/milestone_objective.md` — NEW M2 contract (context). Pay attention to §2 Goal, §3 Sub-sprint sequence (your sub-sprint is the first row), §5 acceptance bar recalibration (Alice + interactive eval = OBSERVATION, NOT gate), §6 hard fences.
4. `docs/proposals/skill_orchestration_candidates.md` — Sprint 5 (F2) original Skill orchestration proposal. This is the architectural ancestor of NEW M2. Status `proposal` / `not_started`. Read for: F2 framing (Skill as parametrized PhasePlan); S1/S2/S3/S4/S5 candidate enumeration (M2 ships S1+S2; S3/S4/S5 deferred to M3-C); the original tool whitelist / recommended order / terminal predicate vocabulary.
5. `docs/proposals/skill_foundation_design.md` — OLD Sprint 36 design freeze (historical reference). This is what was superseded. Read for: what the OLD framing decided (reuse PhasePlan + add teaching paragraph + add adjacent predicate); the OLD §4.1 walk-through and Tier-0 candidate enumeration (the freeze decided NOT to add Tier-0); the OLD §6 #4 verbatim authorization quote for S1 bounded inversion of `D-hard-citation-gate` (CARRIED FORWARD into NEW M2 §6 #4; you'll reference it in design decision (g) for S1 `must_cite_source` Skill guardrail).
6. `docs/sprints/sprint-036-handoff.md` — OLD Sprint 36 dev handoff (historical context for what the dev concluded about current code shape — §1.2 has verified line-numbers and §1.3 has the "MATERIAL FINDING" about Sprint 11 §M1 predicate that Sprint 37 design decision (g) will absorb).
7. Code shape verification (grep each + cite line numbers in handoff §3):
   - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` — grep for `systemInstruction` / `groundingInstruction` / `escalationPolicy` per phase branch; grep for `INTAKE_UCS` and `escalation_reason` enum; cite line numbers.
   - `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` — grep for `shouldReject` methods (FaqMissHandover, IncompleteIntakeHandover, PrematureResolveOutcome); grep for `FAQ_PATH_UCS`; cite line numbers.
   - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` — find the existing projection slots from M1 (Sprint 32 `alternate_candidate_use_cases`, Sprint 33 `discover_disambiguation_signals`); cite line numbers and the slot construction pattern.
   - `server/src/main/resources/prompts/system_prompt.txt` — total line count; line numbers for Sprint 23 `already_called` paragraph, Sprint 31 `alternate_candidate_use_cases` paragraph, Sprint 33 `discover_disambiguation_signals` paragraph.
   - `server/src/main/java/com/gumtree/csagent/service/tools/RecordOutcomeTool.java` — grep `execute` method + `normalizeOutcomeClass`; cite line numbers (Sprint 39 will need this for S1 `must_cite_source` integration; Sprint 37 design decision (g) maps the integration).
   - `server/src/main/java/com/gumtree/csagent/service/runtime/EscalationReasonResolver.java` — grep current downgrade family (Sprint 6 `faq_miss_threshold_exceeded` precedent); cite line numbers (Sprint 39 will need this for S2 `intake_complete_required` integration; Sprint 37 design decision (g) maps the integration).
8. Memory feedbacks (load all from `.claude/agent-memory/sprint-deliver-orchestrator/` — these are deliver-agent feedbacks but relevant patterns for dev too):
   - `feedback_constitution_discipline_vs_planning_anticipation.md` — most relevant for Sprint 37; do NOT silently edit governance docs even if the design freeze "logically suggests" an edit.
   - `feedback_commit_at_end_bundles_deliver_artefacts.md` — dev does NOT stage deliver-agent-owned files; Sprint 37 dev commit bundles only `docs/proposals/skill_registry_design.md` + possible Tier-0 candidate write-up + `docs/sprints/sprint-037-handoff.md`.
   - `feedback_multi_layer_prospective_stanza.md` — Sprint 37 §7 stanza is single-track multi-layer PROSPECTIVE covering Sprints 38/39/40/41.
   - `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every code-citation in design doc + handoff cites file path + line numbers (extraction recipe).

---

## 2. Task — produce `docs/proposals/skill_registry_design.md` locking 10 design decisions

Sprint 37 §2 Goal in the contract enumerates 10 decisions (a)-(j). Read the contract §2 verbatim; you implement that. Section structure of the design doc:

- **Frontmatter** (doc_governance.md schema): `doc_tier: proposal`, `status: proposal`, `implementation_status: not_started`, `source_of_truth: this file`, `last_reviewed: <Sprint 37 commit date>`, `review_cadence: per milestone`, `supersedes: [docs/proposals/skill_foundation_design.md, docs/proposals/skill_orchestration_candidates.md]`, `superseded_by: null`, `notes: > <what this doc decides; relationship to OLD M2-Skill freeze; relationship to Sprint 5 F2 original>`.
- **§1 Background + context**: what M2 is doing; why Sprint 37 freeze is the load-bearing decision; the M2 §5 acceptance recalibration (Alice + eval = observation; functional review + tests = gate); the user-provided research-agent proposal (quote relevant parts; see §3 below).
- **§2 Design decision (a)** — Skill data model
- **§3 Design decision (b)** — Skill Registry shape
- **§4 Design decision (c)** — PhaseEvaluator-as-Skill-Selector integration
- **§5 Design decision (d)** — `procedure` vs `guardrails` responsibility split
- **§6 Design decision (e)** — Retroactive migration mapping for ALL 6 phase YAMLs
- **§7 Design decision (f)** — Retroactive migration mapping for Sprint 23/31/33 teaching paragraphs
- **§8 Design decision (g)** — Retroactive migration mapping for Sprint 6/7/11 predicates
- **§9 Design decision (h)** — Unified Skill terminal-predicate dispatcher design
- **§10 Design decision (i)** — Session-level state model + per-Skill `state_inheritance` semantics
- **§11 Design decision (j)** — §4.1 anti-hardcode kernel walk-through on the proposed design
- **§12 Tier-0 candidate enumeration** — each candidate named; qualification verdict; (if qualified) proposed addition text; (if rejected) reasoning. If NO candidates, state so explicitly.
- **§13 Downstream sub-sprint references** — for each design decision (a)-(j), name which downstream sub-sprint (38/39/40/41) implements it. This lets future dev sessions trace the freeze authority cleanly.

For each design decision section (§2-§11), follow this internal structure:

- **Decision statement** — what this freeze locks (1-3 sentences; declarative).
- **Rationale** — why this decision over alternatives (2-5 sentences; references constraints + Constitution + the research-agent proposal).
- **Alternatives considered + why rejected** — 1-3 alternatives that were on the table; for each, why it's not chosen. (At minimum, the OLD Sprint 36 freeze framing is one alternative for each relevant decision — namely "do NOT do this; reuse existing PhasePlan + add teaching paragraph". Document why NEW M2 rejects that for each relevant decision.)
- **§1.7 boundary check** — explicit "this decision does NOT introduce per-UC-branch if-else because <reason>". If the decision is far from §1.7 (e.g., (b) Skill Registry shape is infrastructure, not semantic), state "§1.7 not implicated".
- **§1.3 / §1.4 boundary check** — explicit "this decision preserves LLM-owned semantic ownership per §1.3 because <reason>" + "this decision preserves Runtime-owned floor per §1.4 because <reason>". If the decision is far from §1.3/§1.4 (e.g., (b) infrastructure), state "§1.3/§1.4 not implicated".
- **Downstream sub-sprint reference** — which sub-sprint (38/39/40/41) implements this decision.

---

## 3. User-provided research-agent proposal (load-bearing architectural input)

The human has done research-agent work prior to NEW M2 approval. The proposal is the conceptual foundation for the Skill Registry abstraction. Quote relevant parts in §1 Background of the design doc; reference throughout §2-§11 design decisions.

**Three-tier abstraction** (research-agent's framing):

- **Tools** = atomic capabilities (already present in `service/tools/`; no change).
- **Skills** = configurable LLM-driven workflow descriptions (NEW; what Sprint 37 freezes; each Skill names tools + recommended order + guardrails).
- **Workflows** = deterministic code-orchestrated multi-step (NOT what M2 is doing; legacy `resolveFaq` path is workflow-style; M2 explicitly chooses Skill-as-Prompt over Workflow per Constitution §1.5).

**Four orchestration patterns** the research-agent enumerated:

1. Pure ReAct — LLM autonomous, no flow guidance (too unpredictable for production CS).
2. Plan-then-Execute — LLM generates plan, executes (good for complex multi-step, not the M2 fit).
3. **Skill-as-Prompt** (the M2 fit) — Phase/scenario → inject corresponding Skill Prompt → LLM autonomously drives tool calls per Skill description. Flexible but bounded.
4. Hardcoded Workflow — Java/Python explicit orchestration (legacy pattern; M2 moves AWAY from this).

**Why Skill-as-Prompt for CS use case** (research-agent's rationale):

- Customer-service problems by type have standard handling flows.
- LLM instruction-following is good enough (Claude 4 series) to follow Skill `procedure` reliably while exercising judgment within the envelope.
- Skill can be independently iterated (YAML edit + reload, no Java recompile).
- Deterministic boundaries preserved as hard fallback: tool whitelist (PhasePlan.allowedTools), safety guards (ControlKernel), terminal predicates (Skill `guardrails`).

**Architecture diagram** (research-agent's framing; adapt verbatim or paraphrase in §1):

```
┌─────────────────────────────────────────┐
│              ControlKernel              │
│  (safety, budget, phase transitions)    │
├─────────────────────────────────────────┤
│            PhaseEvaluator               │
│  (Skill Selector: phase + UC → Skill)   │
├─────────────────────────────────────────┤
│           Skill Registry  ← NEW         │
│  ┌─────────────────────────────────┐    │
│  │ skill: resolve_faq_grounded_*   │    │
│  │ tools: [search_knowledge, …]    │    │
│  │ procedure: |                    │    │
│  │   1. search_knowledge first     │    │
│  │   2. if found, resolve_article  │    │
│  │   3. synthesize + cite          │    │
│  │   4. record_outcome             │    │
│  │ guardrails:                     │    │
│  │   must_cite_source: true        │    │
│  │   max_tool_steps: 4             │    │
│  └─────────────────────────────────┘    │
├─────────────────────────────────────────┤
│            AgentRunLoop                 │
│  (LLM ↔ Tool loop, Skill-guided exec)   │
├─────────────────────────────────────────┤
│         ToolDispatcher + Tools          │
│  (atomic capability layer, unchanged)   │
└─────────────────────────────────────────┘
```

**Branch logic handling** (research-agent's three-level framing; references §1.3 / §1.4 split):

- Level 1 — LLM-soft branch in Skill `procedure` text (LLM judges based on `procedure` guidance; principle-level, NOT per-UC-pair if-else per §1.7).
- Level 2 — Deterministic hard branch in PhaseEvaluator / ControlKernel (safety-critical; not LLM-owned).
- Level 3 — Hybrid (your architecture is already here): PhasePlan constrains tool whitelist (hard), Skill `procedure` guides flow (soft), ControlKernel safety guards (hard).

**Key research-agent insight**: "your architecture only needs the last step — extract the flow description strings scattered in PhaseEvaluator into independent, versionable Skill definition files. This change is a natural extension within your current PhasePlan framework; it does not require core refactor."

NEW M2 takes this insight and adds RETROACTIVE migration of Sprint 23/31/33 teaching paragraphs + Sprint 6/7/11 predicates into the Skill abstraction (the research-agent proposal didn't address these; the human noticed the gap; M2 §3 covers them).

---

## 4. Walking §4.1 nine-question anti-hardcode kernel on the PROPOSED design (Decision (j))

This is the most important section of the design doc. Walk all 9 questions in `iteration_governance.md` §4.1; for each, document the verdict on the PROPOSED design:

1. **Does the proposed design add keyword / regex / if-else / enum / per-UC matrix for a semantic decision?** Walk per Skill component: (i) Skill data model fields (a) — any field that names a per-UC enum? Are `applicable_use_cases` and `applicable_phases` enums acceptable (those are CAPABILITY scope, NOT semantic decision per §1.3)? (ii) Skill `procedure` text (f) (g) — is the migrated content principle-level, or does it carry per-UC-branch if-else?  (iii) Skill `guardrails` declarations (d) (g) (h) — is the DSL declarative principle-level, or does it carry per-UC enum?  (iv) Skill `state_inheritance` (i) — is the declaration principle-level, or per-UC-pair table? Each must be ANSWERED with verdict + reasoning.
2. **If yes to (1), is the change justified as protecting Tier-0?** Default: NO (per M2 §6 #8 default; Tier-0 candidates surfaced per (j) but NOT pre-decided here). If (1) is no, mark (2) "N/A".
3. **Could the same outcome be achieved by projecting a soft signal to the LLM?** This applies to e.g. design decision (i) state inheritance — could `state_inheritance` decisions be entirely LLM-soft via projection slot instead of Skill-declared? If yes, document why the chosen path (declarative inheritance + projection slot for soft signal) is better than pure-projection.
4. **Does the proposed design encode visible-eval case text, trace-specific phrasing, or CaseSpec id?** Should be NO — design freeze is generic architectural decisions, not case-bound.
5. **Does the proposed design move semantic ownership from LLM to Java?** Walk per decision: (d) `procedure` vs `guardrails` split — `procedure` is LLM-soft, `guardrails` is Runtime-floor only; verify no LLM-owned next-action / response-strategy moves into `guardrails`. (g) Sprint 6/7/11 predicate migration — these were ALREADY Runtime-floor in M1 (Sprint 6 = FAQ-miss handover refusal protects grounding floor; Sprint 7 = incomplete-intake handover refusal protects capability floor; Sprint 11 = premature-resolve refusal protects grounding floor); migration preserves Runtime-floor ownership, not a shift.
6. **Does the proposed design add an if-else block to the prompt instead of principle-level guidance?** Walk per decision: (f) Sprint 23/31/33 teaching paragraph migration — these are CURRENTLY principle-level in system_prompt.txt; migration preserves principle-level form in Skill `procedure`.
7. **Does the proposed design preserve tool schema, capability boundary, PII / safety floor, grounding floor?** Walk per layer: tool schema unchanged (existing tools); capability boundary preserved (PhasePlan.allowedTools composed from Skill `tools_required`); PII / safety floor preserved (ControlKernel unchanged); grounding floor preserved (Sprint 11 + NEW S1 `must_cite_source` enforce; bounded per §6 #4).
8. **Does the proposed design ship generalization eval coverage?** Sprint 37 is docs-only; per Sprint 37 §8 stanza, generalization coverage is PROSPECTIVE: Sprint 38 covers 4 phase Skills behavioural equivalence; Sprint 39 covers 2 phase Skills + 3 migrated predicates + 2 new predicates; etc.
9. **If the design is temporary, does it carry rollback or sunset plan?** The Skill abstraction is NOT temporary; it's a long-term architectural shift. Document "permanent change; no sunset". (Individual Skill content can be tuned per M3-Skill-Tuning candidate if needed; that's tuning not abstraction rollback.)

Issue a verdict at the end of (j): `approve` (no anti-hardcode violation in proposed design; Constitution §1.3 / §1.4 / §1.7 preserved); OR `concerns` (specific paragraph or decision needs deliver-agent + human re-decision; surface in handoff §7 OQ); OR `reject as semantic hardcode` (the proposed design fundamentally violates §1.5 / §1.7 — STOP per §10 condition #3).

---

## 5. Tier-0 candidate enumeration (Decision (j) + §12)

At minimum these candidates should be evaluated and verdict given (qualified or rejected, with reasoning):

- **C1 — Skill tool-whitelist enforcement is unconditional.** Statement: "The tool whitelist composed from `skill.tools_required` is enforced by PhasePlan.allowedTools; an LLM tool call outside the whitelist is rejected unconditionally regardless of any soft `procedure` guidance." Qualification: this is an existing runtime guarantee from PhasePlan.allowedTools (Tier-0 candidate ONLY if formalizing it as Tier-0 is load-bearing for downstream confidence; usually NOT — keep as §1.4 implicit guarantee).
- **C2 — Skill terminal predicate refusal is non-overridable by LLM.** Statement: "A guardrail refusal (e.g., `must_cite_source` rejecting `record_outcome(class=resolve)`) cannot be overridden by LLM retry within the same Skill invocation; LLM must either satisfy the guardrail OR escalate." Qualification: this is the predicate enforcement semantics; Tier-0 candidate if it's a new runtime guarantee (probably YES — non-overridability of guardrails IS a structural runtime property worth Tier-0 status).
- **C3 — Skill `state_inheritance` is enforced at session-state-bus boundary.** Statement: "When PhaseEvaluator selects a new Skill on UC switch, SkillStateBus applies the new Skill's `state_inheritance` declaration unconditionally; the LLM cannot 'inherit' state the new Skill declares as `reset`." Qualification: this is the Sprint 41 state-bus enforcement; Tier-0 candidate if new (probably evaluate per (i) decision shape).
- **C4 — S1 `must_cite_source` guardrail is bounded per M2 §6 #4 verbatim authorization.** Statement: "S1 `must_cite_source` fires ONLY on Phase=RESOLVE_FAQ AND tool call=`record_outcome(class=resolve)` AND check=`source_id` citation present; expansion requires new human authorization." Qualification: **NOT a Tier-0 candidate** per §6 #4 bounded inversion (it's a Runtime-floor predicate authorized by human, not a structural Tier-0 invariant; Tier-0 deferral preserved per `D-hard-citation-gate`).
- **C5 — S2 `intake_complete_required` guardrail is bounded to Sprint 7 §I2 precedent.** Statement: "S2 `intake_complete_required` fires ONLY when `requiredIntakeFields` per M1 Sprint 34 extractor is incompletely populated AND tool call=`request_handover` AND `escalation_reason=intake_complete_for_uc_X`." Qualification: similar to Sprint 7 §I2 (already in production; not Tier-0); migrating to Skill guardrails does NOT change its Tier-0 status.

You may identify additional candidates. For each, document candidate statement + qualification verdict + (if qualified) proposed `runtime_freeze_and_risk_policy.md` §1/§2 addition text + (if rejected) reasoning.

If any candidate is QUALIFIED, file a NEW write-up file at `docs/proposals/tier0_candidate_<name>.md` (per Sprint 37 §5 row 2) with full elaboration; surface in handoff §6 + §7 OQ for deliver-agent + human evaluation at Sprint 37 close. Do NOT edit `runtime_freeze_and_risk_policy.md` in Sprint 37 dev commit (per §6 #2 fence + §10 condition #7).

---

## 6. Hard fences reminder (from Sprint 37 contract §6)

You MUST NOT in Sprint 37 dev commit:

1. Write `server/` code (no Java, no test, no YAML Skill file, no prompt edit).
2. Edit `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md` (Tier-0 candidate → separate candidate write-up file).
3. Edit other docs under `docs/foundational/` or `docs/current/` (other than your own handoff).
4. Edit sprint archives under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-036-*`.
5. Edit milestone archives under `docs/milestones/`.
6. Edit `docs/milestone_objective.md` or `docs/sprint_objective.md`.
7. Edit `docs/action_bank.md` or `docs/codex-findings.md`.
8. Edit `docs/proposals/skill_foundation_design.md` (OLD Sprint 36 freeze) — supersede pattern is deliver-agent at M2 approval bundle, separate from Sprint 37 dev commit.
9. Edit `docs/proposals/skill_orchestration_candidates.md` (Sprint 5 F2 original) — chained supersede is deliver-agent.
10. Edit `eval_interactive/case_specs/case_families/`, `eval_interactive/case_specs_shadow/`, `eval_interactive/eval_interactive/`, `eval_interactive/case_specs/bad_cases/`, `eval_interactive/case_spec_overrides.yaml`.
11. Introduce per-UC-branch if-else in the PROPOSED Skill data model OR `procedure` text OR `guardrails` declarations OR `state_inheritance` declarations (§1.7).
12. Pre-decide Sprint 38 / 39 / 40 / 41 implementation specifics beyond what (a)-(j) decides.
13. Delete OR relocate `docs/proposals/skill_foundation_design.md` OR `docs/proposals/skill_orchestration_candidates.md` (supersede pattern only).

---

## 7. Stop conditions (per Sprint 37 contract §10)

STOP and surface in handoff §7 OQ (do NOT silently work around) when:

1. **Premise drift** on §4 items (any of 10 premises changed since 2026-05-17).
2. **Tier-0 candidate surfaced** — file candidate write-up file; do NOT edit governance docs.
3. **§1.7 forbidden-list violation** cannot be cleanly resolved in proposed design.
4. **§1.3 boundary violation** in proposed design (Skill predicate enforces LLM-owned decision).
5. **§1.4 boundary mismatch** in proposed design (Skill predicate doesn't correspond to Runtime-owned floor).
6. **Tempted to write `server/` code** — Sprint 37 is docs-only by design.
7. **Tempted to edit governance docs** — file candidate write-up file instead.
8. **Tempted to apply supersede pattern** to `skill_foundation_design.md` — deliver-agent owns at M2 approval bundle (per default; surface in OQ if you prefer to apply in Sprint 37 commit).
9. **Tempted to draft Sprint 38+ contracts** — deliver-agent owns at planning round.
10. **Tempted to pre-decide Sprint 38+ implementation specifics** beyond (a)-(j).
11. **Tempted to delete OR relocate** `skill_foundation_design.md` OR `skill_orchestration_candidates.md`.
12. **Tempted to modify** `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` — Alice is observation not gate at M2.

---

## 8. Bundle policy

**Single dev commit** with ONLY these files:

- `docs/proposals/skill_registry_design.md` (NEW; the design freeze doc).
- (CONDITIONAL) `docs/proposals/tier0_candidate_<name>.md` (NEW; only if §4.1 walk surfaces qualified Tier-0 candidate; one file per candidate).
- `docs/sprints/sprint-037-handoff.md` (NEW; 12-section dev archive per Sprint 31-36 shape — see Sprint 37 contract §11).

You do NOT stage:
- `docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md` — deliver-agent-owned per `feedback_commit_at_end_bundles_deliver_artefacts.md`.
- `compact/sprint-037-*.md` — deliver-agent-owned.
- Anything under `server/` (Sprint 37 is docs-only).
- Anything under `eval_interactive/` (Sprint 37 is docs-only).

Commit message format (suggestion):

```
sprint 37: Skill Registry + state-across-Skill design freeze (NEW M2 sub-sprint 1; supersedes OLD M2-Skill Sprint 36 freeze)

- docs/proposals/skill_registry_design.md: 10 design decisions locked
  (a) Skill data model; (b) SkillRegistry shape; (c) PhaseEvaluator-as-
  Selector integration; (d) procedure vs guardrails responsibility split;
  (e) retroactive migration mapping for all 6 phase YAMLs; (f) migration
  mapping for Sprint 23/31/33 teaching paragraphs; (g) migration mapping
  for Sprint 6/7/11 predicates; (h) unified Skill terminal-predicate
  dispatcher design; (i) session-level state model + per-Skill
  state_inheritance semantics; (j) §4.1 anti-hardcode kernel walk.
- Tier-0 candidates: <if any: name and reference candidate write-up files;
  if none: state "no qualified Tier-0 candidates surfaced">.
- docs/sprints/sprint-037-handoff.md: 12-section dev archive.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

---

## 9. Java baseline note

Sprint 37 is docs-only: NO Java test run; NO smoke run; NO bad-case run; NO interactive eval run. The Java baseline (post-M1-close 983 + inherited `SystemPromptUserRequestedTiebreakerTest` failure attributed to dirty `system_prompt.txt` working-tree state) is UNCHANGED by Sprint 37.

If your `git status` at session start shows uncommitted working-tree changes from prior deliver-agent session (most likely: `docs/milestone_objective.md`, `docs/sprint_objective.md` modified per M2 approval bundle), surface in handoff §3 (premise check); the deliver-agent has bundled those for you, you should NOT stage them in your dev commit.

---

## 10. Handoff document structure (12 sections per Sprint 31-36 shape; per Sprint 37 contract §11)

Write `docs/sprints/sprint-037-handoff.md` with these 12 sections:

1. **Context Pack** — files read at session start; key citations.
2. **Sub-sprint-objective recap** — Sprint 37 design freeze; 10 design decisions; docs-only.
3. **Premise re-verification** — §4 spot-check; 10 premises.
4. **Design doc walkthrough** — high-level summary of each decision (a)-(j).
5. **§4.1 anti-hardcode walk-through summary** — verdict per question.
6. **Tier-0 candidate enumeration outcome** — candidates surfaced; verdicts; write-up file paths if any.
7. **Open questions for deliver-agent + human**.
8. **Anti-hardcode self-walk on Sprint 37 itself** — expected `approve`.
9. **Files changed** — table.
10. **Layer-classification self-walk** — docs/design only + PROSPECTIVE.
11. **§5 Eval Acceptance bars (adapted)** — design doc shipped; §4.1 walk done; Tier-0 candidates surfaced if any; constitution-compliance verified.
12. **Closure verdict placeholder** — leave for deliver-agent + human + Codex.

---

## 11. M2 milestone context recap (quick reference)

- M2 = NEW Skill Registry Abstraction milestone (supersedes OLD M2-Skill mid-flight).
- 5 sub-sprints: S37 (this one, design freeze) → S38 (SkillRegistry core + 4 simpler phases) → S39 (RESOLVE_FAQ + RESOLVE_INTAKE + Sprint 6/7/11 predicate migration + S1/S2 new predicates + unified dispatcher) → S40 (Sprint 23/31/33 teaching extraction + orchestration shell cleanup) → S41 (UC switch + state preservation).
- M2 acceptance bar recalibrated 2026-05-17: bad-case suite (Alice) + interactive eval are OBSERVATION, not gate. Primary gate = functional review + Java tests + Sprint 37 freeze decisions honored across implementation sub-sprints.
- Sprint 37 freeze is the load-bearing decision; the design doc you ship constraint-binds Sprints 38/39/40/41.

Go.
