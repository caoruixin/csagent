# Dev prompt — Sprint 097 / S-Auto-45 (M-Auto-9 WP2): satisfiable UC-A entity-context companion

You are the **dev agent for Sprint 097 / S-Auto-45 (M-Auto-9 WP2)**. One-line goal: author
**one** cooperative, **visible** UC-A entity-context companion CaseSpec and prove that a
**recorded SATISFIED → `record_outcome(resolve)` → CONFIRM → CLOSE** terminal **can land
end-to-end** on a genuinely-satisfiable flow — closing M-Auto-9's evidence gap — **without**
any closure-forcing edit to the frozen phase machine, and while the two existing PRIMARY
personas stay unchanged and correctly **escalate-after-help**. This is **eval_spec** companion
work; it intends **no** runtime/prompt/phase-machine change. **WP0 stays HELD.**

**Read order (minimal):** `AGENTS.md` (auto-loaded governance) + this prompt. Source-of-truth =
`docs/sprint_objective.md` (Sprint 097; fuller). Gating product decision (read it):
`docs/current/m-auto-9-escalate-after-help-product-decision.md`. Code/CaseSpec anchors below are
read-on-demand.

**Launch precondition:** the human blesses the §"Acceptance criteria" resolvability/ground
truth before you author the persona. If they are not yet blessed, STOP and request the bless.

## Why this sub-sprint exists
M-Auto-7's pilot closed NO-KEEP because neither PRIMARY (`cs_uc_a_no_ad_id_ad_specific`,
`cs_uc_a_loaded_listing`) reaches a recorded SATISFIED+resolve even when grounding is correct —
the personas declare `goal_status=impossible` after a reasonable answer (unsatisfiable by
construction). The charter (`docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md`
§6) makes "no phase-machine change" a first-class valid outcome and asks for a **satisfiable
companion** to prove resolve *can* land. The product decision (recorded) confirms:
escalate-after-genuine-help is a valid terminal; `goal_impossible` alone is **not** an
auto-hard-fail; false-resolve is judged on authoritative `user_state` + closure evidence.

## What to build (Scope)

### #1 — One cooperative, VISIBLE companion CaseSpec
NEW bad-case CaseSpec under `eval_interactive/case_specs/bad_cases/` (flat layout; add a row to
`_manifest.md`), mirroring `cs_uc_a_loaded_listing.yaml`'s **flow** (`classify_use_case →
get_customer_context → search_knowledge → resolve_article → record_outcome(resolve)` →
CONFIRM → CLOSE). **VISIBLE** (development/contract anchor, NOT shadow). Proposed `case_id`:
**`cs_uc_a_loaded_listing_cooperative`** (name not pre-committed; human-bless). Carries
`bad_case_metadata` + `closure_criterion` + a `conditional_outcome_acceptance` block
(`satisfied_outcome: resolve`). It differs from the PRIMARY **primarily in the persona
acceptance contract** — same UC-A entity-context situation, NOT an unrelated easy case.

### #2 — Acceptance criteria (human-blessed at launch)
- **Resolvable ground truth (proposed):** a LIVE listing (AD-2002-class) whose genuinely-correct
  resolution is *substantive, listing-grounded* visibility advice — the bot consults
  `get_customer_context`, references ≥1 specific listing field (status/category/price/location/
  posted_date), and gives category-specific advice grounded in that listing (optionally + the
  canonical visibility-tips article).
- **Persona becomes SATISFIED only when** all hold: (a) bot used the specific listing context
  (≥1 field, not a generic mention), (b) advice is substantive + relevant to the listing's
  category/state, (c) bot did not re-ask for an ad_id already in `form_context`. Then its
  `user_state` series → SATISFIED and the bot records resolve.
- **Persona stays UNRESOLVED when** any of: generic FAQ advice / superficial listing mention /
  missing context use / incomplete resolution / re-asking an already-present ad_id. **No
  unconditional satisfaction** — a wrong/shallow answer must NOT be accepted.

### #3 — Authoring path + ownership
Eval **companion** (new CaseSpec + persona), **NOT** a CaseSpec widen of the two PRIMARY or
their terminals. Deliver+human bless the resolvability; Codex §4.1 reviews anti-hardcode.

### #4 — Visible + anti-hardcode protections
VISIBLE. **Retain held-out neighbors** so you cannot special-case the companion: the two PRIMARY
+ the negative control `cs_uc_a_generic_policy_question` + Tier-2 neighbor
`cs_uc_fp_loaded_moderation` + the held-out shadow suite stay in the run and must not regress.
No case-id / fixed-utterance / per-UC / user-message-content branch to make it pass.

### #5 — Expected RESOLVE→CONFIRM→CLOSE trace contract
Tool sequence `classify_use_case → get_customer_context → search_knowledge → resolve_article →
record_outcome(resolve)`; phase reaches **CONFIRM then CLOSE**; `user_state` shows **SATISFIED**
at/after the grounded answer; `escalation_reason = none`; `correct_outcome: resolve`. Pin as
Java/integration characterization where feasible (satisfiable grounded flow reaching
record→CONFIRM via the **existing** Sprint-093 transition + WP1 reason honesty, **no
premature-guard relax**).

### #6 — Success under the V3 noise-aware rule (NOT raw pass counts)
Decide acceptance under the **S-Y1.7 V3 stability-tiered noise-aware rule** (tier0 floor +
TIER-S majority-flip anti-误杀 floor + TIER-N Beta-Binomial/δ/BH-count) — the small-n
majority-flip gate had a ~92–95% false-discard rate, so a raw ×N count cannot certify closure.
SUCCESS = the companion reaches a recorded SATISFIED+resolve→CONFIRM/CLOSE at a rate clearing
the V3 rule, **and** the 2 PRIMARY do not regress (still escalate-after-help, never forced
resolve), **and** the §7 guards hold. A zero-LLM attribution cross-check must distinguish a
**genuine** resolve from the `user_requested` mislabel + the budget family.

### #7 — Standing safety + cross-UC guards (held green)
The three named exp-90 blast-radius guards **`anchor_uc_g_gdpr`**, **`anchor_uc_fp_removed`**,
**`cs095_uc_d_email_recovery_misroute`**; plus safety UC-J (`cs38s01` scam) / UC-I payment /
UC-G GDPR must NOT be promoted into a forced/false resolve; premature-resolve guard +
`escalation_policy` precedence intact; safety + grounding floors held.

## Hard fences / frozen surfaces
- Do NOT modify/widen `cs_uc_a_no_ad_id_ad_specific.yaml` / `cs_uc_a_loaded_listing.yaml` or
  their expected outcomes/reasons/bars.
- No phase-machine/runtime change intended: `ResolveDispositionEvaluator`,
  `isResolvedSuccessTerminal`, premature-resolve guard, RESOLVE→CONFIRM promotion,
  `BotTurn.sourceIds`, `record_outcome`, max-turn, `EscalationReasonResolver` precedence — frozen.
  **If a runtime change proves necessary → STOP (see below).**
- WP0 HELD (no `source_ids`/promotion change). No `user_state` manipulation/back-inference
  (the simulator `user_state` is the Phase-1 ground truth; `conditional_outcome.py` constraint).
- No `goal_impossible`-based auto-hard-fail (per the recorded product decision).
- No semantic hardcode (§1.5/§1.7); no benchmark-shaped easy case.
- No re-bless / canonical-pointer move assumed — the bounded run is a VALIDATION run. Adding a
  new visible companion does not change existing baseline cases' scored outcomes; canonical
  baseline + pointer stay frozen.

## Test / eval requirements
1. Companion compiles against `eval_interactive/eval_interactive/case_spec/schema.py`;
   `_manifest.md` row added; `conditional_outcome_acceptance` parses.
2. Java/Python suites — no new regression vs re-measured clean-tree baselines (Java `1422/1/0/2`;
   eval_interactive + autoloop). Re-measure at start; attribute deltas.
3. Bounded NON-pilot real-LLM run (§5.7 + §5.9 GO; `caffeinate`): companion ×11 + 2 PRIMARY ×11 +
   negative control ×5 + Tier-2 neighbor ×5 + the §7 guards. Show the companion reaches a
   recorded SATISFIED+resolve→CONFIRM/CLOSE; the 2 PRIMARY do not regress; §7 guards green;
   decide under the V3 rule (#6) + a zero-LLM attribution cross-check.
4. Zero-LLM attribution cross-check — genuine resolve vs `user_requested` mislabel + budget
   family; confirm `user_state`-driven acceptance, not back-inference.
5. Anti-误杀 proof — held-out neighbors / negative control / shadow green; no special-casing.

## Code / CaseSpec anchors (read-on-demand)
- `eval_interactive/case_specs/bad_cases/cs_uc_a_loaded_listing.yaml` — the PRIMARY to mirror
  (flow, `conditional_outcome_acceptance`, `closure_criterion`). Do NOT edit it.
- `eval_interactive/case_specs/bad_cases/_manifest.md` — tier definitions + lifecycle ledger row.
- `eval_interactive/eval_interactive/case_spec/schema.py` — CaseSpec schema.
- `eval_interactive/eval_interactive/scoring/conditional_outcome.py` — the declarative
  SATISFIED/UNRESOLVED/UNKNOWN acceptance (reads `trace.user_state_signals`; never back-inferred;
  CONDITIONAL_ELIGIBLE never auto-PASS).
- `eval_interactive/eval_interactive/scoring/hard_checks.py:787-800` — `goal_impossible`
  exclusion (the recorded product decision keeps it).
- V3 gate: the S-Y1.7 fitness rule (`autoloop/...` tier_decision / `config.yaml`) — for the
  success decision basis.

## Stop-and-surface (close BLOCKED, not COMPLETE)
STOP if the companion can pass **only** through: phase forcing / premature-guard relax /
max_turns raise / lowering `record_outcome`; OR a PRIMARY CaseSpec widen; OR `user_state`
manipulation/back-inference; OR benchmark-specific/case-id/per-UC/content logic. ALSO STOP if:
a **genuinely-satisfiable** companion **still cannot reach a recorded resolve** without a runtime
change (surfaces a real closure defect → a separate sub-sprint; do NOT force it); the companion
can't be satisfiable without being trivially easy (loses control value); the §7 guards or
safety/grounding floors regress; or compatibility evidence forces a re-bless decision (human's).

## Handoff requirements
Write `docs/sprints/sprint-097-handoff.md`: the companion CaseSpec + bless record; the §5 gate
evidence (bounded real-LLM run + V3 decision + zero-LLM cross-check); the stop-condition check;
restate that WP2 proves resolve-can-land (or surfaces a real closure defect), does NOT modify the
PRIMARY, intends no runtime change, and WP0 stays HELD. Record the §4.1 Codex verdict pointer.

## Commit discipline
Eval-side, staged explicitly by file (no `git add -A`): (a) companion CaseSpec + `_manifest.md`;
(b) characterization tests; (c) handoff. Tree green at each boundary. Real-LLM only on a clean
committed tree (`caffeinate`). No data/artifact files committed.

## Self-check before declaring done
- [ ] Companion authored (visible), compiles, `_manifest.md` row added; name human-blessed.
- [ ] Acceptance criteria human-blessed; persona SATISFIED only on genuinely-correct grounded
      help; shallow/generic/incomplete stays UNRESOLVED.
- [ ] Two PRIMARY CaseSpecs + bars UNCHANGED; no frozen runtime surface touched; WP0 HELD.
- [ ] Bounded real-LLM run: companion reaches recorded SATISFIED+resolve→CONFIRM/CLOSE under the
      V3 rule; 2 PRIMARY not regressed; §7 standing guards green; zero-LLM cross-check confirms
      genuine resolve.
- [ ] No special-casing / case-id / content heuristic; `user_state`-driven acceptance.
- [ ] Java/Python suites no new regression (deltas attributed).
- [ ] §4.1 Codex `pass` recorded verbatim in `docs/codex-findings.md`.
- [ ] If any STOP fired → closed BLOCKED with the gate decision recorded; COMPLETE not claimed.
