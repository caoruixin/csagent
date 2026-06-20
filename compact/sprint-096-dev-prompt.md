# Dev prompt — Sprint 096 / S-Auto-44 (M-Auto-9): `D-new-escalation-reason-enum` migration

You are the **dev agent for Sprint 096 / S-Auto-44 (M-Auto-9)**. One-line goal: add
**exactly one** new canonical `escalation_reason` value for the narrow class of
bot-initiated handover below, wired **atomically** across every authoritative producer +
consumer, so the bot stops mislabeling these handovers `user_requested` and uses an honest
reason. This is the honest fix the **Sprint 095 / S-Auto-43 (WP1) BLOCK** identified
(`docs/sprints/sprint-095-handoff.md`). It is a **Runtime §1.4 vocabulary completion**
(human-authorized), **NOT** a semantic hardcode and **NOT** a generic fallback. It delivers
WP1's reason-honesty intent only; it does **NOT** solve the M-Auto-9 PRIMARY closure
question; **WP0/WP2 remain HELD**.

**Read order (minimal):** `AGENTS.md` (auto-loaded governance) + this prompt. Source-of-truth
= `docs/sprint_objective.md` (Sprint 096; fuller). Code anchors are read-on-demand.

## The narrow class the new reason names (binding — all 5 must hold)
1. user did **not** request a human; 2. issue is **in scope** (a V1 UC the bot engaged);
3. **no** higher-priority reason applies (not safety/scam, payment, GDPR/compliance,
identity, appeal, intake-complete, out-of-scope, actual service degradation, runtime error);
4. the bot made a **reasonable supported attempt** or **exhausted its resolution path**;
5. the issue is **unresolved** and the bot **initiates handover for human continuation**.
The value exists for THIS class only. **It must NOT become a generic fallback** (for "other",
for budget/turn exhaustion, for out-of-scope, or for service-degraded).

## Why this is needed (verified by WP1, read-only)
`escalation_reason` is LLM-supplied verbatim in `request_handover`
(`RequestHandoverTool.java:60`) and `user_requested` is priority-1/Tier-0 in
`EscalationReasonResolver` (`:84-113`), so the LLM's `user_requested` sticks. Walking all 23
canonical reasons, **none** honestly fits the §-class (`out_of_scope` is bound 100% to non-V1
intent `phase2…:564,582`; `service_degraded` is the forbidden catch-all). `confirm.yaml:20`
already reaches for the non-canonical `user_dissatisfied` (→ silently `service_degraded`) —
the gap is structural. The honest fix is a NEW low-priority bot-initiated reason.

## Reason NAME — review-first, do NOT pre-commit
Review the 23 existing values' snake_case convention (`turn_budget_exhausted`,
`appeal_requires_human`, `account_compliance`, …) + every consumer below, then propose a
name that precisely names the **bot-initiated, exhausted-resolution, unresolved, needs-human**
semantic — distinct from `service_degraded` (no degradation claim), `out_of_scope` (in scope),
and the budget-family; reads as a SEMANTIC (LLM-owned) reason, not a terminal-close. NOT a
generic "other"/"misc". Illustrative only (do NOT adopt unreviewed): `agent_unable_to_resolve`
/ `resolution_exhausted` / `unresolved_needs_human`. The name is a **Codex + human bless gate**
before finalizing.

## Atomic migration — the enum has 3 synced sources + the resolver, guarded by a sync test
`eval_interactive/tests/scoring/test_escalation_enum_sync.py` walks 3 sources and asserts the
"(23 values)" count. Going to **24** requires ALL of these in lockstep (or the test reds):
1. `docs/current/customer_service_tool_spec_v0_3.md` — the **Canonical `escalation_reason`
   enum (23 values)** bullet (add value; count → 24).
2. `server/.../service/runtime/PhaseEvaluator.java` `CANONICAL_ESCALATION_REASONS` (add).
3. `server/.../service/runtime/EscalationReasonResolver.java` `CANONICAL_REASONS` + `PRIORITY`
   (add at the §precedence low slot; decide the `user_dissatisfied`→? legacy `canonicalize`
   mapping + record it).
4. `server/.../service/tools/RequestHandoverTool.java` — ensure the value flows through;
   review (optional) whether to add canonical-set validation (must not break existing flows).
5. `server/.../service/runtime/ContextProjectionBuilder.java` (+ reason-vocab resource) —
   surface the value with teaching that reserves `user_requested` for an actual user request
   and uses the new reason ONLY for the §-class.
6. `server/src/main/resources/skills/confirm.yaml:20` — replace `user_dissatisfied` with the
   new canonical value.
7. `eval_interactive/eval_interactive/case_spec/schema.py` — `EscalationTrigger` Literal /
   `ESCALATION_TRIGGER_VALUES` + post-init validator + comments (add).
8. `eval_interactive/.../scoring/escalation_reason_match.py` (+ `hard_checks.py`,
   `case_spec/extractor.py`, `policy_table.py`, `case_outcome_resolver.py` as applicable) —
   put the value in the correct family for `escalation_reason_family_match` (observation-only
   since S-Auto-38); **no gating PASS change**.
9. Tests: `test_escalation_enum_sync.py` (23→24), `test_escalation_trigger_match.py`,
   `test_contract_validation.py` (update to 24 + new value).
10. `trace/collector.py` + any reason-enumerating serializer (round-trip the value).
11. backward-compat loaders (`baseline_loader.py` + any reason-enumerating loader) — old
    23-value artifacts load unchanged; **do NOT rewrite past evidence**.

## FROZEN — do NOT touch (STOP-and-surface if the fix seems to need them)
phase machine / `PhaseEvaluator` post-loop logic (beyond the `CANONICAL_ESCALATION_REASONS`
member add) / `ResolveDispositionEvaluator` / premature guard / RESOLVE→CONFIRM / `BotTurn.
sourceIds` / `record_outcome` / `isResolvedSuccessTerminal` / max-turn; handover **eligibility**
/ escalation **policy** (who/when — only the label vocabulary changes); `detectExplicitUser
Escalation` + the genuine-user `user_requested` path; `resolveMaxStepsReason` + budget-family
+ `TERMINAL_CLOSE_REASONS`; **CaseSpec/PRIMARY expectations** (no expected-reason/outcome
change to *require* the new value; **no PASS widening**); baseline / canonical pointer (frozen
until the §scoring-compat evidence is reviewed).

## Anti-误杀 fences (the value must NOT become a generic fallback)
- **No runtime auto-stamp / catch-all:** do NOT map arbitrary unknowns to it in `canonicalize`
  (unknowns still → `service_degraded`); do NOT default to it. It is **LLM-selected** (§1.3),
  surfaced via projection for the §-class only.
- **No user-message keyword/regex/content heuristic** to detect the class (WP1-forbidden).
- No CaseSpec ID / fixed utterance / ad ID / benchmark branch. No floor widening.

## Precedence (binding) + required tests
Place the value at a **low-priority** slot so each of these WINS over it whenever genuinely
present (it never displaces them): genuine `user_requested`/`user_distress`/`imminent_harm`;
`out_of_scope`; actual `service_degraded`/`runtime_error_threshold`; `trust_safety_required`;
`payment_dispute_detected`; `gdpr_intake`/`account_compliance`/`identity_verification_required`;
`appeal_requires_human`/`incorrect_deletion_appeal`; `intake_complete_for_uc_*`/
`incomplete_intake`/`tool_scope_blocked`; budget `turn_budget_exhausted`/
`faq_miss_threshold_exceeded`/`clarification_budget_exhausted`. Net = **lowest-priority
semantic reason**: surfaces only when nothing genuinely-higher (incl. real budget/failure)
applies. **Prove with `EscalationReasonResolver.resolve()` precedence tests for each pairing
(both orders)** + a one-paragraph rationale. Can't satisfy → STOP.

## Validation gates (record all in `docs/sprints/sprint-096-handoff.md`)
1. **Schema-consistency:** `test_escalation_enum_sync.py` green at 24; eval post-init validator
   accepts it; runtime+resolver sets include it.
2. **Characterization:** genuine user request → `user_requested`; §-class bot-initiated → the
   new value (not `user_requested`/`service_degraded`); + the §precedence tests.
3. **Backward-compat replay:** old 23-value traces+baselines load unchanged; no past reason
   rewritten; prove loaders unaffected.
4. **Zero-LLM scoring replay — no improper outcome / no PASS widening:** replay recorded
   bad_cases/anchor/shadow through the new scoring; prove no gated outcome changes; name the
   standing guards (below).
5. **Java + Python regression suites:** no new regression vs **re-measured** clean-tree
   baselines (Java `1394/1/0/2`; Python eval_interactive + autoloop). Re-measure + attribute.
6. **Bounded real-LLM validation (REQUIRED, §5.7):** projected vocabulary + model selection
   change → a bounded NON-pilot run (§-class cases + genuine-request controls + standing/
   precedence guards; §5.9 pre-flight GO; `caffeinate`) showing the model **uses the new reason
   for the §-class** and **does NOT over-use it** (no bleed onto user_requested/out_of_scope/
   service_degraded/budget). Decide under the S-Y1.7 **V3 noise-aware rule** (not a raw count)
   + a zero-LLM attribution cross-check.
7. **Scoring-SHA + baseline-compat plan:** record the new scoring SHA; analyze whether any
   existing canonical-baseline case's scored outcome changes. **Do NOT assume re-bless /
   canonical movement.** Additive + observation-only + no scored-outcome change → no re-bless
   (record §5.7 N/A). A scored-outcome change → **STOP** (human decides re-bless/baseline move).

## Standing guards (must stay green)
`anchor_uc_g_gdpr` · `anchor_uc_fp_removed` · `cs095_uc_d_email_recovery_misroute` · UC-J
scam/trust-safety (`cs38s01`) · UC-I payment · UC-G GDPR · explicit-human-request (genuine →
`user_requested`) · genuinely-unresolved escalate-after-help (escalation still occurs; only the
bot-initiated label changes). None may flip to the new reason; none may regress.

## STOP-and-surface (ship nothing dishonest; surface the cause; don't expand scope)
- the value can only fire via a generic fallback / runtime auto-stamp / content heuristic;
- the **real-LLM run (gate 6) shows over-use** (bleed) → do NOT ship; rework projection teaching;
- a **precedence slot is impossible** (gate /§6);
- **backward-compat** would require rewriting past evidence;
- **gate 7 compat evidence forces a re-bless / canonical move** (human decision — out of scope);
- a standing guard or safety/grounding floor regresses; or the name can't be made non-generic.

## §7 stanza
Target layer `prompt_projection` + `semantic_planner` (LLM selects the honest reason, §1.3);
the enum add is **Runtime §1.4 vocabulary completion**, human-authorized (cf.
[[feedback_capability_config_vs_semantic_fence]] — narrow lockstep schema edits + golden
enum-sync test updated). Adds **no Tier-0** (precedence floor preserved; new value low-priority,
cannot displace any safety/dispute/user reason). **No semantic hardcode** (vocabulary
completion; LLM owns the choice; no keyword/regex/per-UC matrix/content heuristic; NOT a generic
fallback). Coverage target/neighbor/negative/shadow = §-class bot-initiated (real-LLM+zero-LLM)
/ other-UC bot-initiated (no regression) / genuine user_requested + out_of_scope +
service_degraded + safety/payment/GDPR/dispute/intake + budget must NOT flip (§precedence +
zero-LLM replay + real-LLM no-over-use) / shadow held-out no regression.

## Rollback / commit discipline
Stage explicitly **by file** (NO `git add -A`). Separable commits: (a) enum add across the 3
sources + resolver/priority **+ the enum-sync test together** (else the sync test reds); (b)
projection + `confirm.yaml`; (c) eval schema + scoring; (d) tests. Tree green (suites +
enum-sync) at every boundary. Real-LLM only on a clean committed tree (`caffeinate`). No data/
eval artifacts committed. STOP mid-migration → revert to baseline (no half-applied enum).

## Codex review plan
Per-sub-sprint Codex **REQUIRED** (§4.3 — new enum value on the §1.7 "enum expansion" line). The
§4.1 kernel must confirm: human-authorized Runtime §1.4 vocabulary completion (not a semantic
hardcode); not a generic fallback (no auto-stamp/content heuristic); precedence preserves all
floors; no eval PASS widening. Verdict verbatim → `docs/codex-findings.md` (§4.2). No COMPLETE
close until `pass`.

## Required explicit records (objective + handoff)
- Delivers WP1's **reason-honesty intent only**; does not change whether/when handover occurs.
- Does **NOT** solve the M-Auto-9 PRIMARY closure / product-contract question.
- **WP0 and WP2 remain HELD** (charter §6).

## Self-check before close
- [ ] Name reviewed against conventions + consumers; non-generic; Codex+human blessed.
- [ ] Enum at 24 across all 3 sources + resolver; `test_escalation_enum_sync.py` green.
- [ ] §-class bot-initiated → new value; genuine request → `user_requested`; precedence tests pass.
- [ ] Backward-compat: old artifacts load unchanged; no past evidence rewritten.
- [ ] Zero-LLM scoring replay: no gated-outcome change, no PASS widening; standing guards green.
- [ ] Java + Python suites re-measured; no new regression.
- [ ] Bounded real-LLM: correct use for the §-class + NO over-use (V3 rule + zero-LLM cross-check).
- [ ] Scoring-SHA recorded; baseline-compat decided (no re-bless assumed; STOP if a scored
      outcome changes).
- [ ] Not a generic fallback; no frozen surface touched; no content heuristic.
- [ ] §4.1 Codex `pass`. WP1-intent-only + no-PRIMARY-closure + WP0/WP2-HELD restatements present.
- [ ] If any STOP fired → closed BLOCKED/STOPPED, COMPLETE gates not claimed.
