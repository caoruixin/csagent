---
title: "Sprint 096 / S-Auto-44 (M-Auto-9) — D-new-escalation-reason-enum migration (bot-initiated unresolved-handover reason)"
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-20
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  SCOPED, NOT LAUNCHED. The coordinated D-new-escalation-reason-enum migration that the
  Sprint 095 / S-Auto-43 (WP1) BLOCK identified as the honest fix. It adds ONE new
  canonical escalation_reason value for the narrow class "bot-initiated, in-scope,
  unresolved, no higher-priority reason applies, handover for human continuation", wired
  atomically across all authoritative producers + consumers (runtime enum + resolver +
  handover tool + projection + confirm.yaml + eval EscalationTrigger + scoring +
  serialization + backward-compat loading). This is a Runtime §1.4 capability/vocabulary
  completion (human-authorized via the §4 D-new-escalation-reason-enum migration path),
  NOT a semantic hardcode: the LLM still owns when to select it (§1.3); it MUST NOT become
  a generic runtime fallback. Delivers WP1's reason-honesty intent only; it does NOT solve
  the M-Auto-9 PRIMARY closure / product-contract question; WP0/WP2 remain HELD. Touches
  the previously-frozen EscalationReasonResolver + canonical enum under explicit human
  authorization; the phase machine / record_outcome / premature guard / source_ids /
  CaseSpec PRIMARY expectations / canonical baseline pointer stay frozen. Requires a
  bounded real-LLM validation (projected vocabulary + model-selected reason change) and a
  scoring-SHA + baseline-compatibility plan; re-bless / canonical movement is NOT assumed —
  decided only on compatibility evidence. The reason NAME is NOT pre-committed (review
  naming conventions + consumers first). Dev prompt: compact/sprint-096-dev-prompt.md.
---

# Sprint 096 / S-Auto-44 (M-Auto-9) — `D-new-escalation-reason-enum` migration

## 0. Status

**SCOPED — awaiting human launch.** Deliver contract for the migration dev sub-sprint;
the dev agent is **not** spawned until the human pastes
`compact/sprint-096-dev-prompt.md`. `implementation_status: not_started`.

- **Parent milestone:** M-Auto-9 (runtime/orchestration closure; design `APPROVE` 2026-06-20).
- **Predecessor:** Sprint 095 / S-Auto-43 (WP1) CLOSED **BLOCKED** at the §6 existing-reason
  honesty gate — no canonical reason honestly labels the bot-initiated unresolved handover
  (`docs/sprints/sprint-095-handoff.md`). This migration is the human-authorized honest fix
  (`action_bank.md` §4 `D-new-escalation-reason-enum`, TRIGGERED 2026-06-20).
- **Sprint-ID (history-checked):** last archived dev sub-sprint = Sprint 095 / S-Auto-43
  (`docs/sprints/sprint-095-{objective,handoff}.md`). Next free pair = **Sprint 096 /
  S-Auto-44** (no `sprint-096` archive, no `compact/sprint-096*`, no `S-Auto-44+` reference
  anywhere — unambiguous).

## 1. Exact objective

Add **exactly one** new canonical `escalation_reason` value that honestly labels a single
narrow class of bot-initiated handover (§2), and wire it **atomically** across every
authoritative producer and consumer (§3) so the enum stays internally consistent. After
the migration, when the bot initiates handover for that class, it labels the handover with
the new (honest) reason instead of the dishonest `user_requested` / `service_degraded`.

This **delivers WP1's reason-label honesty intent**. It is still **reason-label honesty
only**: it must not change whether or when handover occurs, nor any phase transition or
outcome. It **does not** solve the M-Auto-9 PRIMARY closure / product-contract question.

## 2. The narrow class the new reason names (binding)

The new reason applies to a handover where **all** of the following hold:

1. the user did **not** request a human;
2. the issue remains **in scope** (a V1-covered use case the bot engaged);
3. **no** safety, compliance, payment, service-failure, or other **higher-priority** reason
   applies (not scam/trust-safety, payment dispute, GDPR/compliance, identity, appeal,
   intake-complete, out-of-scope, actual service degradation, runtime error);
4. the bot has made a **reasonable supported attempt** or **exhausted its available
   resolution path**;
5. the issue remains **unresolved** and the bot **initiates handover for human
   continuation**.

The new value exists to give the LLM an honest option for **this** class — nothing wider.
It **must not become a generic fallback** (§4) for "anything else", for budget/turn
exhaustion (those are the existing runtime terminal-close reasons), or for out-of-scope /
service-degraded cases.

## 3. Allowed surfaces — the atomic migration (all in lockstep)

The enum currently has **three synced sources of truth** plus the resolver, all guarded by
`eval_interactive/tests/scoring/test_escalation_enum_sync.py` (it walks the three and
asserts they match the "(23 values)" doc bullet). The migration makes it **24** and MUST
update every site so that test (and the runtime/eval suites) pass:

| # | Surface | Path | Change |
|---|---|---|---|
| 1 | Canonical doc enum | `docs/current/customer_service_tool_spec_v0_3.md` (the **Canonical `escalation_reason` enum (23 values)** bullet) | add the value; update the count to 24 |
| 2 | Runtime canonical set | `server/.../service/runtime/PhaseEvaluator.java` `CANONICAL_ESCALATION_REASONS` | add the value |
| 3 | Resolver set + **precedence** | `server/.../service/runtime/EscalationReasonResolver.java` `CANONICAL_REASONS` + `PRIORITY` (and `canonicalize` if a legacy literal should map to it) | add the value at a **low-priority slot** per §6; decide the `user_dissatisfied` legacy mapping (record the decision) |
| 4 | Handover-tool argument | `server/.../service/tools/RequestHandoverTool.java` | ensure the new value flows through (`escalation_reason` arg → payload/trace); review whether to add explicit canonical-set validation (optional; must not break existing flows) |
| 5 | Projection vocabulary | `server/.../service/runtime/ContextProjectionBuilder.java` (+ any reason-vocabulary resource) | surface the new value in the per-turn reason vocabulary with teaching that reserves `user_requested` for an actual user request and uses the new reason **only** for the §2 class |
| 6 | CONFIRM skill | `server/src/main/resources/skills/confirm.yaml` (the `:20` not-satisfied handover reason) | replace the non-canonical `user_dissatisfied` with the new canonical value |
| 7 | Eval enum (consumer) | `eval_interactive/eval_interactive/case_spec/schema.py` `EscalationTrigger` Literal / `ESCALATION_TRIGGER_VALUES` + the post-init validator + comments | add the value |
| 8 | Eval scoring | `eval_interactive/.../scoring/escalation_reason_match.py` (+ `hard_checks.py`, `case_spec/extractor.py`, `policy_table.py`, `case_outcome_resolver.py` as applicable) | place the new value in the correct family for `escalation_reason_family_match` (observation-only since S-Auto-38) + any policy table; **no gating PASS change** |
| 9 | Enum-sync + trigger tests | `eval_interactive/tests/scoring/test_escalation_enum_sync.py` (the 23→24 regex + 3-source walk), `test_escalation_trigger_match.py`, `test_contract_validation.py` | update to expect 24 and the new value |
| 10 | Serialization / trace | `trace/collector.py` + any trace serializer that enumerates reasons | accept + round-trip the new value |
| 11 | Backward-compat loading | baseline/trace loaders (`baseline_loader.py` and any reason-enumerating loader) | old artifacts (23-value) load unchanged; **past evidence is not rewritten** |

New Java + Python characterization/precedence tests live under the respective `test`
trees.

## 4. Frozen surfaces + anti-误杀 fences (do NOT touch / do NOT do)

**Frozen runtime/eval surfaces** (unchanged):
- phase-machine transitions; `PhaseEvaluator` post-loop interpretation (other than the
  `CANONICAL_ESCALATION_REASONS` member add); `ResolveDispositionEvaluator`; the
  premature-resolve guard; RESOLVE→CONFIRM promotion; `BotTurn.sourceIds`;
  `record_outcome` semantics; `isResolvedSuccessTerminal`; max-turn behaviour.
- **handover eligibility / escalation policy** — *who/when* may escalate is unchanged; the
  migration changes only the *label vocabulary*.
- `detectExplicitUserEscalation` and the runtime path that stamps `user_requested` for a
  genuine user request (leave intact — genuine requests still get `user_requested`).
- the existing runtime budget-family stamping (`resolveMaxStepsReason`) and the
  `TERMINAL_CLOSE_REASONS` set — the new reason is **not** a terminal-close reason and must
  not alter budget-family behaviour.
- **CaseSpec / PRIMARY expectations**: no CaseSpec expected-outcome or expected-reason is
  changed to *require* the new value, and no PASS condition is widened (the new value is
  added to the *vocabulary*, not to any case's accept set). Any case that should newly
  *expect* the new reason is a **separate** `eval_spec` decision, out of this migration.
- **baseline / canonical pointer**: frozen until the §7.7 compatibility evidence is
  reviewed; this session does not move them.

**Anti-误杀 fences (the value must not become a generic fallback):**
- **No runtime auto-stamp / generic catch-all.** Do NOT make `canonicalize` map arbitrary
  unknowns to the new value (unknowns still → `service_degraded`); do NOT have the runtime
  *default* to it. It is an **LLM-selected** reason (§1.3), surfaced via projection for the
  §2 class only.
- **No user-message keyword/regex/content heuristic** to detect the class (that is the
  WP1-forbidden vector and would reintroduce a semantic hardcode).
- **No CaseSpec ID / fixed utterance / ad ID / benchmark branch.**
- **No widening** of any safety/grounding floor; no relaxation of the premature guard.

## 5. Reason name — review-first, NOT pre-committed

Do **not** pre-commit a spelling. Before wiring, review (a) the naming convention of the
existing 23 values (snake_case; mostly `<subject>_<state>` / `<category>_<action>` — e.g.
`turn_budget_exhausted`, `appeal_requires_human`, `account_compliance`) and (b) every
consumer in §3. Propose a name that:
- names the **bot-initiated, exhausted-resolution, unresolved, needs-human** semantic
  precisely (not a generic "other"/"misc"/"fallback");
- is distinct from `service_degraded` (no system degradation claim), `out_of_scope` (the
  issue is in scope), and the budget-family (not a turn/clarification cap);
- reads as a **semantic** escalation reason (LLM-owned), not a terminal-close reason.

Illustrative-only candidates (do **not** adopt without review/bless):
`agent_unable_to_resolve`, `resolution_exhausted`, `unresolved_needs_human`. The chosen
name is a **Codex + human review gate** before the wiring is finalized.

## 6. Precedence (binding constraint + required tests)

Place the new value at a **low-priority slot** such that, whenever genuinely present, **each
of the following wins over it** (the new reason never displaces them):
- genuine `user_requested` (and `user_distress` / `imminent_harm`);
- `out_of_scope`;
- actual `service_degraded` (and `runtime_error_threshold`);
- safety/scam (`trust_safety_required`), payment (`payment_dispute_detected`),
  GDPR/compliance (`gdpr_intake` / `account_compliance` / `identity_verification_required`),
  dispute/appeal (`appeal_requires_human` / `incorrect_deletion_appeal`);
- intake-specific (`intake_complete_for_uc_*` / `incomplete_intake` / `tool_scope_blocked`);
- genuine budget/failure termination (`turn_budget_exhausted` /
  `faq_miss_threshold_exceeded` / `clarification_budget_exhausted`).

Net: the new value is the **lowest-priority semantic reason** — it surfaces only when
nothing genuinely-higher (including a real budget/failure termination) applies. The dev
proposes the exact `PRIORITY` slot consistent with this and **proves it with precedence
tests** (`EscalationReasonResolver.resolve(existing, candidate)` for each pairing above, in
both orders), with a one-paragraph rationale. If the constraint cannot be satisfied without
displacing some reason → STOP (§8).

## 7. Required validation gates

All gates must be recorded in `docs/sprints/sprint-096-handoff.md`.

1. **Producer/consumer schema-consistency tests** — `test_escalation_enum_sync.py` (3
   sources + the doc count) passes at 24; the eval post-init validator accepts the value;
   runtime `CANONICAL_ESCALATION_REASONS` + resolver `CANONICAL_REASONS` include it.
2. **Genuine-user-request vs bot-initiated-handover characterization** — Java + Python
   tests proving a genuine user request still resolves to `user_requested`, and a
   bot-initiated §2-class handover resolves to the new value (not `user_requested` /
   `service_degraded`). Plus the §6 precedence tests.
3. **Historical trace / backward-compat replay without rewriting past evidence** — old
   (23-value) traces + baselines load unchanged; no historical reason is rewritten; the new
   value simply does not appear in old artifacts. Prove the loaders are unaffected.
4. **Zero-LLM scoring replay — no improper outcome or PASS widening** — replay the recorded
   bad_cases / anchor / shadow traces through the new scoring code; prove no case's gated
   outcome changes and no PASS is widened (the new value is observation-only in
   `escalation_reason_family_match`, non-gating). Name the §8-guard cases explicitly.
5. **Java + Python regression suites** — no new regression vs the re-measured clean-tree
   baselines (Java documented `1394/1/0/2`; Python `eval_interactive` + autoloop suites).
   **Re-measure at start and attribute deltas** (your new tests add to the totals).
6. **Bounded real-LLM validation (required, §5.7)** — because the projected vocabulary and
   the model-selected reason behaviour change, a bounded NON-pilot real-LLM run (the §2-class
   bot-initiated cases + genuine-user-request controls + the §8 precedence/standing guards;
   §5.9 pre-flight GO; `caffeinate`) must show the model **uses the new reason for the §2
   class** and **does NOT over-use it** (no bleed onto `user_requested` / `out_of_scope` /
   `service_degraded` / budget cases). Decide under the S-Y1.7 V3 noise-aware rule (not a
   raw count), with a zero-LLM attribution cross-check.
7. **Scoring-SHA + baseline-compatibility plan** — record the new scoring-code SHA and
   analyze whether any existing canonical-baseline case's scored outcome changes. **Do NOT
   assume re-bless or canonical-pointer movement is required.** If the evidence shows the
   schema/scoring change is additive + observation-only with **no** scored-outcome change on
   the existing baseline → no re-bless (record the §5.7 N/A reasoning). If it shows a scored
   outcome would change → STOP and surface (human decides re-bless / baseline movement).

## 8. Stop-and-surface conditions

STOP and surface (do not work around) if:
- the new value can only be made to fire for the §2 class via a **generic fallback** /
  runtime auto-stamp / user-message content heuristic (it would stop being honest/narrow);
- the **real-LLM validation (§7.6) shows the model over-uses** the new reason as a catch-all
  (bleed onto out_of_scope / service_degraded / budget / genuine user_requested) — do NOT
  ship; the projection teaching needs rework;
- a **precedence test (§6) cannot be satisfied** — the value would displace a
  higher-priority reason at every viable slot;
- **backward-compat loading** would require rewriting past evidence, or old baselines cannot
  load alongside the 24-value schema;
- the **§7.7 compatibility evidence indicates a re-bless / canonical-pointer move is
  required** (that is a human decision, out of this scoping/dev session's authority);
- any **§8 standing guard** or safety/grounding floor regresses;
- the naming review (§5) cannot land a precise, non-generic name.

## 9. Rollback requirements

- Single logical migration, but **separable commits** staged explicitly **by file** (no
  `git add -A`): (a) the enum add across the 3 sources + resolver/priority; (b) projection +
  `confirm.yaml` teaching; (c) eval schema + scoring; (d) tests. The tree must be green
  (suites + enum-sync) at every commit boundary — because the enum-sync test spans sources,
  the enum-add commit (a) must update all three sources + the test together to stay green.
- Real-LLM steps run only on a clean committed tree (`caffeinate`; keep the Mac awake).
- No data/artifact files committed (bounded-run artifacts gitignored).
- If a STOP fires mid-migration, revert to the documented baseline (the partial enum-add
  must not be left half-applied — it would red the enum-sync test).

## 10. Layer-classification + anti-hardcode stanza (§7.1)

**Target failure layer:** `prompt_projection` + `semantic_planner` (the LLM selects the
honest reason from the surfaced vocabulary; reason labeling is LLM-owned, §1.3). The enum
addition itself is a **Runtime §1.4 capability/vocabulary** change (the canonical reason
vocabulary is Runtime-owned), human-authorized via the `D-new-escalation-reason-enum`
migration path — analogous to registering a new tool-capability (cf.
[[feedback_capability_config_vs_semantic_fence]]): narrow schema edits in lockstep across
the synced sources, with the golden enum-sync test updated.

**Tier-0 invariant:** adds no Tier-0 invariant. The resolver precedence **floor** is
preserved (the new value is low-priority and cannot displace any safety/dispute/user-signal
reason); the safety + grounding floors are unchanged.

**Semantic hardcode:** No semantic hardcode introduced. The new enum value is a vocabulary
completion that gives the LLM an honest option; the LLM owns *when* to use it. There is **no**
keyword / regex / if-else / per-UC matrix / user-message content heuristic, and the value is
**not** wired as a generic runtime fallback (no `canonicalize` auto-mapping, no default
stamp). The one named risk — the value becoming a catch-all — is fenced by §4 and gated by
the §7.6 real-LLM no-over-use check.

**Generalization coverage:** target / neighbor / negative / shadow = `§2-class bot-initiated
handovers (real-LLM + zero-LLM characterization)` / `other-UC bot-initiated escalations (no
reason/handover regression)` / `genuine user_requested + out_of_scope + service_degraded +
safety/payment/GDPR/dispute/intake + budget cases must NOT flip to the new reason (§6
precedence + §7.4 zero-LLM replay + §7.6 real-LLM no-over-use)` / `held-out shadow suite no
regression (§7.4)`.

## 11. Acceptance gates — two terminal outcomes

WP closes **COMPLETE** only when: the value is wired atomically across §3 (enum-sync green
at 24); §5 name reviewed + blessed; §6 precedence proven; §7 gates 1–7 all met (real-LLM
shows correct use + no over-use; backward-compat clean; no PASS widening; suites green;
scoring-SHA/baseline-compat resolved without an undecided re-bless); §4.1 Codex `pass`.

It closes **BLOCKED / STOPPED** (not COMPLETE) if any §8 condition fires — e.g. the model
over-uses the reason, a precedence slot is impossible, or compatibility evidence forces a
re-bless decision that is the human's to make. A BLOCKED close archives the contract +
records the gate decision; the COMPLETE gates are explicitly not claimed.

## 12. Required explicit records (deliverable)

- This migration **delivers WP1's reason-honesty intent only.** It does not change whether
  or when handover occurs.
- It **does not solve the M-Auto-9 PRIMARY closure / product-contract question** (the
  unsatisfiable-persona problem; making `record_outcome(resolve)` land).
- **WP0** (source_ids / promotion-evidence) and **WP2** (CONFIRM record-vs-handover on
  satisfiable flows) **remain HELD** under charter §6.

## 13. Evidence and closeout artifacts

- `docs/sprints/sprint-096-handoff.md` — dev handoff (name decision + rationale; the atomic
  diff map across §3; §6 precedence proof; the §7.1–§7.7 gate evidence incl. the bounded
  real-LLM run + zero-LLM cross-check; the scoring-SHA/baseline-compat decision; the §12
  restatements).
- `docs/codex-findings.md` — §4.1 per-sub-sprint Codex verdict (REQUIRED).
- On close, deliver-agent archives this file → `docs/sprints/sprint-096-objective.md`;
  `compact/sprint-096-dev-prompt.md` stays as the historical executable view;
  `action_bank.md` §4 `D-new-escalation-reason-enum` + §5 `R-oq-s93.1...` updated.

## 14. Codex review plan

Per-sub-sprint Codex **REQUIRED** (§4.3 trigger #2/#3 — a new `escalation_reason` enum value
sits exactly on the §1.7 "enum expansion" line). The §4.1 kernel must confirm: (1) the value
is a **human-authorized Runtime §1.4 vocabulary completion**, not a semantic hardcode; (2) it
is **not** a generic fallback (no auto-stamp, no content heuristic); (3) the precedence slot
preserves all floors; (4) no eval PASS widening. Verdict verbatim → `docs/codex-findings.md`
(§4.2). No COMPLETE close until `pass`.
