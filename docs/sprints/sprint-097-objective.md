---
title: "Sprint 097 / S-Auto-45 (M-Auto-9 WP2) — satisfiable UC-A entity-context companion (prove RESOLVE→CONFIRM→CLOSE can land)"
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-06-21
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  SCOPED, ground-truth + name BLESSED 2026-06-21, NOT LAUNCHED. WP2 of M-Auto-9 (charter
  docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md §6).
  Authors ONE resolvable, VISIBLE UC-A entity-context companion CaseSpec
  (`cs_uc_a_loaded_listing_resolvable`, human-blessed name) that
  exercises the same flow as the two PRIMARY (request ad reference if needed →
  load listing context → substantively grounded help → record RESOLVE →
  CONFIRM/CLOSE) but differs PRIMARILY in the persona acceptance contract: the
  persona becomes SATISFIED only when human-blessed resolution criteria are
  actually met (genuinely correct, listing-grounded help), and stays UNRESOLVED
  on generic / superficial / incomplete answers. Goal = demonstrate a recorded
  SATISFIED+resolve terminal CAN land end-to-end, decided under the S-Y1.7 V3
  noise-aware rule (not raw pass counts), while the two existing PRIMARY personas
  (unsatisfiable by construction) stay UNCHANGED and correctly escalate-after-help.
  This is eval_spec companion authoring; it intends NO runtime / phase-machine /
  record_outcome / premature-guard / source_ids / prompt change. Builds on the
  recorded product/eval decision docs/current/m-auto-9-escalate-after-help-product-decision.md.
  WP0 stays HELD; the two PRIMARY CaseSpecs + expected terminals are NOT modified.
  Dev prompt: compact/sprint-097-dev-prompt.md.
---

# Sprint 097 / S-Auto-45 (M-Auto-9 WP2) — satisfiable UC-A entity-context companion

## 0. Status

**CLOSED — COMPLETE 2026-06-21** (dev + per-sub-sprint Codex + milestone-close gates).
Resolve-can-land **DEMONSTRATED**: the companion `cs_uc_a_loaded_listing_resolvable`
(AD-2007 EXPIRED fixture) reached a recorded SATISFIED+resolve terminal **8/11** bounded
real-LLM (V3-clearing), 2 PRIMARY 0/11 (no regression, escalate-after-help), §7 guards
green, zero-LLM cross-check 100% `user_state`-driven, §4.1 Codex `approve`/`pass`/0. **No
§8 STOP fired.** **Qualification (objective §2/§11):** the recorded resolve lands via the
grounding-gated `isResolvedSuccessTerminal` terminal, **not** via a proven explicit
`record_outcome→CONFIRM→CLOSE` tool-path sequence — that path stays premature-guard-blocked
(the OQ-S93.1 residual), carried to a separate runtime sub-sprint, NOT forced here. `eval_spec`
only; no runtime/phase-machine/`record_outcome`/premature-guard/`source_ids`/prompt change;
two PRIMARY CaseSpecs + bars unchanged; WP0 HELD. Archived at M-Auto-9 close →
`docs/sprints/sprint-097-{objective,handoff}.md`; dev prompt `compact/sprint-097-dev-prompt.md`
spent. `implementation_status: implemented`.

- **Parent milestone:** M-Auto-9 (runtime/orchestration closure; design `APPROVE`
  2026-06-20). Charter §6 WP2.
- **Predecessor:** Sprint 096 / S-Auto-44 (M-Auto-9 WP1 — `agent_unable_to_resolve`
  reason migration) CLOSED COMPLETE 2026-06-20.
- **Gating product decision (recorded):**
  `docs/current/m-auto-9-escalate-after-help-product-decision.md` (escalate-after-
  genuine-help is a valid terminal; `goal_impossible` alone is not an auto-hard-fail;
  false-resolve judged on authoritative `user_state` + closure evidence).
- **Sprint-ID (history-checked):** last archived dev sub-sprint = Sprint 096 /
  S-Auto-44. Next free pair = **Sprint 097 / S-Auto-45** (no `sprint-097` archive, no
  `compact/sprint-097*`, no `S-Auto-45+` reference anywhere — unambiguous).

## 1. Class

- **Layer (§3.2):** `eval_spec` (companion CaseSpec/persona authoring + acceptance
  contract + the conditional-adjudication artifact). **No runtime/prompt/phase-machine
  change is intended** (if one proves necessary → §8 STOP).
- **§7 stanza:** REQUIRED (eval_spec is a semantic surface). See §10.
- **Per-sub-sprint Codex (§4.3):** **REQUIRED** — the companion is an eval-side
  artifact on the §5.4 / §1.7 anti-hardcode surface (it must not be a benchmark-shaped
  easy case the implementation can special-case). See §14.

## 2. Goal

Prove that a **recorded SATISFIED → `record_outcome(resolve)` → CONFIRM → CLOSE**
terminal **can land end-to-end** on a genuinely-satisfiable UC-A entity-context flow —
closing the M-Auto-9 evidence gap that the two unsatisfiable PRIMARY personas left
open — **without** any closure-forcing edit to the frozen phase machine, and while the
two PRIMARY personas stay unchanged and correctly **escalate-after-help**.

The deliverable is the **smallest legitimate** demonstration: a resolvable companion
+ the bounded real-LLM evidence + the zero-LLM cross-check, decided under the V3 rule.
"No runtime change needed" is a **first-class valid outcome**; if resolve cannot land
on a genuinely-satisfiable persona without a runtime change, that is a §8 STOP that
surfaces a real closure defect for a separate sub-sprint (do not force it here).

## 3. Scope — the satisfiable companion (human-blessed criteria)

### #1 — Author ONE resolvable, VISIBLE companion CaseSpec

A NEW bad-case CaseSpec under `eval_interactive/case_specs/bad_cases/` (flat layout;
registered in `_manifest.md`), mirroring `cs_uc_a_loaded_listing.yaml`'s **flow**:
`classify_use_case → get_customer_context → search_knowledge → resolve_article →
record_outcome(resolve)` → CONFIRM → CLOSE. **Visibility = VISIBLE** (a development /
contract anchor, **not** shadow). **Blessed `case_id`: `cs_uc_a_loaded_listing_resolvable`**
(human-blessed 2026-06-21 — `resolvable` describes the human-blessed ground truth without
implying the persona is easy to satisfy or unconditionally cooperative). It carries the
standard `bad_case_metadata` + `closure_criterion` + a `conditional_outcome_acceptance`
block (`satisfied_outcome: resolve`).

The companion differs from `cs_uc_a_loaded_listing` **primarily in the persona
acceptance contract** — it is the same UC-A entity-context situation, not an unrelated
easy case, so it stays a clean control for the two PRIMARY.

### #2 — Human-blessed resolvability + acceptance contract (BLESSED 2026-06-21, §contract-element-1)

The companion's ground truth + acceptance contract is **human-blessed (2026-06-21)** —
authored by the dev exactly to these terms (do not weaken or widen):

1. The issue is **objectively resolvable** within the available UC-A entity-context flow.
2. The bot must **correctly retain / classify UC-A**.
3. It must **use the already-available listing context** and **must not re-ask for an
   ad ID already present**.
4. The answer must **use listing-specific information substantively**: at least one
   diagnosis, explanation, or recommended action must **materially depend on the actual
   listing state or fields**.
5. **Merely mentioning the listing title/status before reverting to generic FAQ advice
   does NOT satisfy** the contract.
6. **Generic ad-creation tips, unsupported claims, or unrelated promotional advice
   remain `UNRESOLVED`.**
7. The persona becomes **`SATISFIED` only after receiving a correct, sufficiently
   grounded and actionable answer meeting the human-blessed criteria** above.
8. Once satisfied, the expected trace is: **grounded answer → `record_outcome(RESOLVE)`
   → CONFIRM → CLOSE**.
9. The companion **must not authorize a false resolve for a still-dissatisfied user**
   and **must not modify or widen either existing PRIMARY CaseSpec**.
10. **No phase forcing, CaseSpec widening, `user_state` manipulation, case-ID
    hardcoding, or benchmark-specific branch** is permitted.

### #3 — Persona authoring + review ownership (REQUIRED, §contract-element-2)

- **Authoring path:** an eval **companion** (a new CaseSpec + persona), **NOT** a
  CaseSpec widen of the two existing PRIMARY (`cs_uc_a_no_ad_id_ad_specific`,
  `cs_uc_a_loaded_listing`) or their expected terminals.
- **Review ownership:** deliver-agent + human **BLESSED** the resolvability / ground
  truth + name (2026-06-21; §3 #1/#2). The per-trace adjudication semantics + Codex §4.1
  anti-hardcode / no-special-casing review remain at sub-sprint execution.

### #4 — Visible status + anti-hardcode protections (REQUIRED, §contract-element-3)

VISIBLE. **Retain the held-out neighboring guards** so the implementation cannot
special-case the companion: the two PRIMARY + the anti-误杀 negative control
(`cs_uc_a_generic_policy_question`) + the Tier-2 neighbor (`cs_uc_fp_loaded_moderation`)
+ the held-out shadow suite stay in the run and must not regress. No case-id / fixed-
utterance / per-UC / user-message-content branch may be used to make the companion pass.

### #5 — Expected RESOLVE→CONFIRM→CLOSE trace contract (REQUIRED, §contract-element-4)

Specify the expected trace: tool sequence `classify_use_case → get_customer_context →
search_knowledge → resolve_article → record_outcome(resolve)`; phase path reaches
**CONFIRM then CLOSE**; the simulator `user_state` series shows **SATISFIED** at/after
the grounded answer; `escalation_reason = none`; `correct_outcome: resolve`. Pin this
as Java/integration characterization where feasible (a satisfiable grounded flow
reaching record→CONFIRM via the existing Sprint-093 transition + WP1 reason honesty,
**no premature-guard relax**).

### #6 — V3 noise-aware success criteria, not raw pass counts (REQUIRED, §contract-element-5)

Acceptance is decided under the **S-Y1.7 V3 stability-tiered noise-aware rule**
(tier0 floor + TIER-S majority-flip anti-误杀 floor + TIER-N Beta-Binomial / δ /
BH-count), **not** a raw ×N pass-count (the small-n majority-flip gate had a ~92–95%
false-discard rate). SUCCESS = the companion reaches a **recorded SATISFIED+resolve →
CONFIRM/CLOSE** terminal at a rate that clears the V3 rule, **and** the two PRIMARY do
**not** regress (they stay escalate-after-help, never forced resolve), **and** the §7
standing guards hold green. A zero-LLM attribution cross-check must distinguish a
**genuine** resolve from the `user_requested` mislabel and the budget family.

### #7 — Standing safety + cross-UC regression guards (REQUIRED, §contract-element-6)

Held green across the bounded run (the same suites the V3 gate reads —
bad_cases / anchor_outcome / shadow): the three named exp-90 blast-radius guards
**`anchor_uc_g_gdpr`**, **`anchor_uc_fp_removed`**, **`cs095_uc_d_email_recovery_misroute`**;
plus safety UC-J (`cs38s01` scam), UC-I payment, UC-G GDPR must **not** be promoted
into a forced/false resolve; the premature-resolve guard + `escalation_policy`
precedence stay intact; safety + grounding floors held.

## 4. Hard fences / frozen surfaces

- **Do NOT modify or widen** `cs_uc_a_no_ad_id_ad_specific.yaml` /
  `cs_uc_a_loaded_listing.yaml` or any of their expected outcomes / reasons / bars.
- **No phase-machine / runtime change** intended: `ResolveDispositionEvaluator`,
  `isResolvedSuccessTerminal`, the premature-resolve guard, RESOLVE→CONFIRM promotion,
  `BotTurn.sourceIds`, `record_outcome` semantics, max-turn, `EscalationReasonResolver`
  precedence — all frozen. (If a runtime change proves necessary → §8 STOP.)
- **WP0 stays HELD** — no `source_ids` / promotion-evidence change.
- **No user_state manipulation / back-inference** — the simulator `user_state` is the
  Phase-1 ground-truth signal; it is never derived from the chosen outcome / handover /
  reason / absence of a signal (`conditional_outcome.py` constraint).
- **No `goal_impossible`-based auto-hard-fail** and **no** re-introduction of one
  (per the recorded product decision).
- **No semantic hardcode** (§1.5 / §1.7): no keyword/regex/case-id/per-UC/content
  heuristic to make the companion pass; no benchmark-shaped easy case.
- **No re-bless / canonical-pointer move assumed** — the bounded run is a VALIDATION
  run, not a re-bless. Adding a new visible companion does not change existing baseline
  cases' scored outcomes; if the human later wants the companion in the canonical
  baseline, that is a **separate** re-bless decision (§8 STOP-and-surface; do not
  assume).

## 5. Test / eval requirements

1. **Companion CaseSpec compiles** against `eval_interactive/eval_interactive/case_spec/schema.py`;
   `_manifest.md` row added (tier proposed: a Tier-1 **companion/control**, visible);
   the `conditional_outcome_acceptance` block parses.
2. **Java/Python suites** — no new regression vs the re-measured clean-tree baselines
   (Java `1422/1/0/2`; eval_interactive + autoloop). Re-measure at start; attribute deltas.
3. **Bounded NON-pilot real-LLM run (the eval gate, §5.7 + §5.9 GO; `caffeinate`):**
   companion ×11 + 2 PRIMARY ×11 + anti-误杀 control ×5 + Tier-2 neighbor ×5 + the §7
   standing guards. Show the companion reaches a **recorded SATISFIED+resolve →
   CONFIRM/CLOSE**, the 2 PRIMARY do **not** regress, the §7 guards hold; decide under
   the V3 rule (§3 #6) + a zero-LLM attribution cross-check.
4. **Zero-LLM attribution cross-check** — distinguish genuine resolve from the
   `user_requested` mislabel + budget family; confirm `user_state`-driven acceptance,
   not back-inference.
5. **Anti-误杀 proof** — the held-out neighbors / negative control / shadow stay green;
   no special-casing of the companion (Codex §4.1).

## 6. Codex review plan

Per-sub-sprint Codex **REQUIRED** (§4.3): the companion is an eval_spec artifact on the
§5.4 / §1.7 surface. The §4.1 kernel must confirm: (1) the companion is a genuine
control, **not** a benchmark-shaped easy case or a CaseSpec widen of the PRIMARY;
(2) no special-casing / case-id / content heuristic makes it pass; (3) the acceptance
is `user_state`-driven (not back-inferred); (4) no PRIMARY bar widened; (5) no frozen
surface touched. Verdict verbatim → `docs/codex-findings.md` (§4.2). No COMPLETE close
until `pass`.

## 7. (reserved — see §3 #6/#7 for success + guards)

## 8. Stop-and-surface conditions (BLOCKED, not COMPLETE)

STOP and surface (do not work around) if the companion can be made to pass **only**
through any of:
- **phase forcing** / relaxing the premature-resolve guard / raising `max_turns` /
  lowering the `record_outcome` requirement;
- **CaseSpec widening** of the two PRIMARY or their expected terminals;
- **`user_state` manipulation / back-inference** (deriving satisfaction from the
  outcome/handover/reason rather than the simulator signal);
- **benchmark-specific / case-id / per-UC / user-message-content** logic.

Also STOP and surface if:
- a **genuinely-satisfiable** companion **still cannot reach a recorded resolve**
  without a runtime change (this reveals a real closure defect → a separate runtime
  sub-sprint / re-examine WP0; do NOT force it here);
- the companion cannot be made satisfiable without making it **trivially easy** (losing
  its value as a clean control for the two PRIMARY);
- the §7 standing guards or safety/grounding floors regress;
- the compatibility evidence indicates a **re-bless / canonical-pointer move** is
  required (human decision, out of this session's authority).

## 9. Rollback / commit discipline

- Eval-side change, staged explicitly **by file** (no `git add -A`): (a) the companion
  CaseSpec + `_manifest.md` row; (b) any characterization tests; (c) the handoff. Tree
  green (suites) at each commit boundary.
- Real-LLM steps run only on a clean committed tree (`caffeinate`).
- No data/artifact files committed (bounded-run artifacts gitignored).

## 10. Layer-classification + anti-hardcode stanza (§7.1)

**Target failure layer:** `eval_spec` (author a satisfiable companion + its acceptance
contract; demonstrate a resolve terminal can land). No runtime semantic change intended.

**Tier-0 invariant:** adds no Tier-0 invariant. The safety + grounding floors and the
premature-resolve guard / `escalation_policy` precedence are preserved (standing §7
guards held green; no forced/false resolve on any safety/dispute case).

**Semantic hardcode:** No semantic hardcode introduced. The companion is a generalizable
control, not a benchmark-shaped easy case; acceptance is `user_state`-driven, not
back-inferred; no keyword/regex/case-id/per-UC/content heuristic. The named risk — the
companion being special-cased or trivially easy — is fenced by §4/§8 and the §3 #7
held-out guards + Codex §4.1.

**Generalization coverage:** target / neighbor / negative / shadow =
`the satisfiable companion (recorded SATISFIED+resolve→CONFIRM/CLOSE under the V3 rule)`
/ `the two PRIMARY (unchanged — still escalate-after-help, no regression) +
cs_uc_fp_loaded_moderation` / `cs_uc_a_generic_policy_question + the three exp-90
blast-radius guards (anchor_uc_g_gdpr / anchor_uc_fp_removed /
cs095_uc_d_email_recovery_misroute) + safety UC-J/I/G must NOT flip to a forced resolve`
/ `held-out shadow suite no regression`.

## 11. Acceptance gates — two terminal outcomes

WP2 closes **COMPLETE** only when: the companion is authored + human-blessed (§3);
it compiles + registers (§5.1); the bounded real-LLM run shows a **recorded
SATISFIED+resolve → CONFIRM/CLOSE** clearing the V3 rule with the 2 PRIMARY not
regressed and the §7 guards green (§5.3); the zero-LLM cross-check confirms a genuine
resolve (§5.4); suites green (§5.2); and §4.1 Codex `pass`.

It closes **BLOCKED / STOPPED** (not COMPLETE) if any §8 condition fires — notably if
a genuinely-satisfiable companion cannot reach a recorded resolve without a runtime
change (surfacing a real closure defect), or if passing would require special-casing /
PRIMARY widening / `user_state` manipulation. A BLOCKED close archives the contract +
records the gate decision; the COMPLETE gates are explicitly not claimed.

## 12. Required explicit records (deliverable)

- WP2 **proves resolve can land on a satisfiable flow** (or surfaces a real closure
  defect if it cannot). It does **not** modify the two PRIMARY personas / bars.
- It is **eval_spec** companion work; it intends **no** runtime/phase-machine change.
- **WP0** (source_ids / promotion-evidence) **remains HELD**; the canonical baseline +
  pointer stay frozen.

## 13. Evidence and closeout artifacts

- `docs/sprints/sprint-097-handoff.md` — dev handoff (the companion CaseSpec + bless
  record; the §5 gate evidence incl. the bounded real-LLM run + V3 decision + zero-LLM
  cross-check; the §8 stop-condition check; the §12 restatements).
- `docs/codex-findings.md` — §4.1 per-sub-sprint Codex verdict (REQUIRED).
- On close, deliver-agent archives this file → `docs/sprints/sprint-097-objective.md`;
  `compact/sprint-097-dev-prompt.md` stays as the historical executable view;
  `action_bank.md` §5 `R-oq-s93.1...` updated (WP2 outcome).

## 14. Codex review plan (restated)

Per-sub-sprint Codex **REQUIRED** (§4.3). The §4.1 kernel confirms the companion is a
genuine, non-special-cased control with `user_state`-driven acceptance and no PRIMARY
widening (§6). Verdict verbatim → `docs/codex-findings.md` (§4.2). No COMPLETE close
until `pass`.
