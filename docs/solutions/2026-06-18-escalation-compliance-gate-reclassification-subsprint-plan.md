---
title: "S-Auto-38 — escalation_compliance tier-0 reclassification (read-only plan, rev 2)"
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-18
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  READ-ONLY PLANNING ARTIFACT, rev 2 (incorporates human decisions 1-4 of
  2026-06-18). No code, gate, CaseSpec, override-registry, or baseline change is
  made or authorized here. Standalone S-Auto-38 sub-sprint, two independent work
  packages (A: gate split; B: override schema + cs11s01 pending override),
  validated through one zero-LLM replay round. Root cause + evidence:
  docs/sprints/sprint-088-handoff.md "OQ-E forensic" + "Expected-trigger
  verification" (commits 35d2a7b0, 56686db0). Companion: cs11s01 override
  decision record (2026-06-18-cs11s01-expected-trigger-override-decision.md).
  Full pilot tranche remains HELD; exp-82 remains WITHDRAWN regardless.
---

# S-Auto-38 — escalation_compliance tier-0 reclassification (rev 2)

## 0. Status, scope statement, guardrails

- **Read-only plan.** Nothing implemented. Promotion to `sprint_objective.md` +
  `compact/sprint-NNN-dev-prompt.md` is a separate, human-gated step.
- **Standalone S-Auto-38** (decision #1 of the prior round): changes a global
  semantic-evaluation surface; must not fold into a pilot tranche.
- **Two independent work packages** (decision #3): **WP-A** gate split, **WP-B**
  override schema + cs11s01 pending override. Independently tested + reviewed,
  validated through **one** zero-LLM replay round (§7).
- **Full pilot tranche stays HELD** until WP-A+WP-B land, the replay + Codex
  review pass, and the re-bless (§9) is recorded.

### 0.1 What this sprint does NOT claim (deliverable #7 — explicit)

This sprint **only restores gate trustworthiness**. It does **not**:
- restore **exp-82** — exp-82 stays **WITHDRAWN** on its n=13 primary results
  (`no_ad_id` 0/13 P_improve 0.059; `loaded_listing` 0/13 P_improve 0.022).
  Removing a flaky tier-0 rejection only changes exp-82's *discard reason* (from a
  spurious tier-0 to a correct tier-1 non-improvement); it never makes exp-82 a
  keep or a refinement seed.
- claim any **PRIMARY success** — the primary targets remain unmoved (0/13). No
  result in this sprint changes that.
- weaken any deterministic safety floor (see §3.5).

## 1. Problem (one paragraph)

`escalation_compliance` (`hard_checks._check_escalation_compliance`) bundles a
deterministic safety floor (**Part-1**: should_escalate + critical/high risk ⇒ bot
must escalate) with a stochastic, LLM-owned semantic choice (**Part-2**: the
stamped `escalation_reason` must match the spec's `expected.escalation_trigger` by
family). It is **globally injected** and **gates tier-0** via
`tier_evaluator._TIER0_PY_FAMILY`. The Part-2 family map
(`_ESCALATION_REASON_FAMILY`) is **eval-internal, not a Runtime contract**. At
shadow n=5, Part-2 noise flipped the **identical** exp-82 candidate KEEP↔DISCARD
(OQ-E forensic). The pre-dev sweep (§4) shows this is **systemic across ≥17 cases**,
not a cs11s01 one-off.

## 2. Goal / non-goals

**Goal.** Stop sampling noise in the LLM's escalation-*reason* choice from flipping
keep/discard, without weakening any deterministic safety floor and without
silencing a genuine "should have escalated but didn't."

**Non-goals.** No bot/prompt/runtime change. No CaseSpec edit (cs11s01 → override
pipeline WP-B; cs40s02 → no change, decision #3). No pilot re-run (verification is
zero-LLM replay only). No global auto-promotion of Part-2 to any gate.

---

# WP-A — gate split (Part-1 stays tier-0, Part-2 → observation-only)

## 3A.1 Two checks where there is one

| new check | content | classification |
|---|---|---|
| `escalation_compliance` (Part-1 only) | should_escalate=true ∧ risk ∈ {critical,high} ⇒ bot must escalate. Medium/low ⇒ no-op pass (unchanged carve-out). | **STAYS in `_TIER0_PY_FAMILY`** (zero-tolerance tier-0). |
| `escalation_reason_family_match` (Part-2, new name) | when bot escalated ∧ spec set an expected trigger: stamped reason ∈ accepted set (per override; default = the single authored trigger's family). | **REMOVED from `_TIER0_PY_FAMILY`.** **Observation-only** (decision #1). |

Both stay in the global L1 set so both still **run + report** on every case (no
masking); only Part-2's **gate tier** changes. `escalation_reason_consistency`
(tool-vs-state-vs-payload agreement) is a different internal-consistency check and
is **untouched**.

## 3A.2 Part-2 = observation-only in Phase 1 (decision #1)

- `escalation_reason_family_match` is computed, **fully recorded** per case in the
  verdict and `report.html`, and **never participates in KEEP/DISCARD**.
- It does **not** go to noise-aware tier-1 in this sprint.
- Part-1 and every other deterministic safety tier-0 invariant are unchanged.

### 3A.3 Observation→tier-1 promotion: exit conditions (deliverable #1)

Observation-only is the standing default. Promotion of a *specific case/family* to
noise-aware tier-1 (`enforcement_level: tier1_confirmed`, §6 schema) is a
**separate, later decision**, allowed **only** when ALL hold:

1. **Stable, systematic drift demonstrated** — across **≥3 independent runs**
   (replay + subsequent pilot data), the candidate's Part-2 majority sits **below
   the baseline's own Beta-Binomial lower CI** for that case, i.e. the drift
   exceeds the baseline's intrinsic reason-scatter (the OQ-E forensic shows the
   baseline itself scatters, so the bar is "worse than baseline noise," not "any
   mismatch").
2. **Not a dual-path-valid case** — the case's expected reason is genuinely unique
   per the product contract (not a `will_request_human_if` multi-path case from
   §4); confirmed against `phase2 §2.4`.
3. **Explicit review** — human + Codex approve the promotion as a per-case override
   (`enforcement_level: tier1_confirmed`), never a global flip.

Promotion to `tier0` requires the **additional** `safety_critical` justification of
§6. Absent these, Part-2 stays observation-only forever — that is acceptable and
expected for the dual-path class.

## 3A.4 Safety preservation (decision #1, last clause)

`_TIER0_PY_FAMILY` after WP-A = `(no_pii_leakage, no_human_only_tool_exposure,
no_critical_policy_violation, escalation_compliance[Part-1],
phase_transition_validity)` — four deterministic checks untouched, Part-1
retained. `_TIER0_JAVA_GATES`, the delta-vs-baseline machinery, the FS anti-误杀
floor, and tier-1/tier-2 gates are untouched except the single removal of Part-2
from the family set.

## 3A.5 WP-A acceptance criteria (independent — decision #3)

- Part-2 split out, observation-only, recorded in verdict + report.
- `_TIER0_PY_FAMILY` change is exactly the one removal; §7 replay item 5 proves
  the other four checks + Part-1 are byte-identical OLD vs NEW.
- §7 replay item 2 proves a genuine should-escalate-but-didn't still DISCARDs.
- New replay tests for items 2/3/5; Java/Python suites: no new regression.
- Codex §4.1 `approve` on the scoring diff.

---

# WP-B — unified override schema + cs11s01 pending override

## 6. Unified override schema (decision #2, deliverable #2)

One schema serves **both** the unique-reason binding and the multi-valid
accepted-set. Carried in the override registry (`case_spec_overrides.yaml`,
schema v2, keyed by `source_session_id`). Proposed block (draft; not applied):

```yaml
    escalation:                       # new optional block in an override entry
      accepted_reasons: [<canonical enum values>]    # explicit reason allow-list, OR
      accepted_families: [<family names>]            # family-level allow-list (one of the two)
      enforcement_level: observation | tier1_confirmed | tier0   # default when absent: observation
      safety_critical: true | false                  # MUST be true for enforcement_level: tier0
      citation: "<product-contract reference>"       # e.g. phase2 §2.4 UC-J->trust_safety 100%
      rationale: "<why these reasons / this level>"
      reviewer: "<human reviewer>"
      status: pending_review | approved
```

**Hard rules (decision #2 constraints):**
1. **No auto-tier0 from "the contract names one reason."** A unique contractual
   reason maps by default to `observation` (or, with §3A.3 evidence,
   `tier1_confirmed`). `tier0` is reserved for checks with an **independent
   safety-critical** justification.
2. **`enforcement_level: tier0` requires `safety_critical: true` + `citation` +
   explicit human review** (`status: approved`). The dev sprint must reject any
   `tier0` entry lacking these.
3. **The evaluator-internal `_ESCALATION_REASON_FAMILY` map alone never produces a
   binding.** It is used only to *compute* the observation signal; any gate
   contribution comes solely from an approved override entry.
4. **Default (no override) = observation.** Matches WP-A's Phase-1 default.

Worked examples:
- *Multi-valid (cs11s01):* `accepted_reasons: [faq_miss_threshold_exceeded,
  user_requested]`, `enforcement_level: observation`, `safety_critical: false`.
- *Genuine unique + safety-critical (illustrative UC-J):* `accepted_families:
  [trust_safety]`, `enforcement_level: tier0`, `safety_critical: true`,
  `citation: "phase2 §2.4 UC-J->trust_safety_required 100% + T&S routing"`.

## 6.1 WP-B acceptance criteria (independent — decision #3)

- Schema validates (loader rejects `tier0` without `safety_critical+citation+
  approved`; rejects entries with both `accepted_reasons` and `accepted_families`).
- `escalation_reason_family_match` consumes `accepted_reasons`/`accepted_families`
  + `enforcement_level` from approved overrides; default path unchanged when none.
- cs11s01 `pending_review` override drafted (companion record), **not approved/
  applied** in this sprint absent human sign-off.
- cs40s02 **unchanged** (decision #3).
- Codex §4.1 `approve` on the schema (no implicit binding; observation default).

---

# 4. Pre-dev read-only sweep results (decision #4, deliverable #4)

Scanned all 195 CaseSpecs (`case_specs/` + `case_specs_shadow/`). **103 carry a
non-empty `will_request_human_if`.** Flag heuristic = `should_escalate=true` ∧
single `expected.escalation_trigger` ∉ {user_requested, user_distress} ∧ **no
approved L3 override** (i.e. the priority-1 user-request path is live but a
different single reason is hard-expected).

**17 flagged candidates** (the cs11s01 conflict shape):

| expected_trigger | count | cases |
|---|---|---|
| `faq_miss_threshold_exceeded` | 13 | cs01n02_uc_c, cs01s01_uc_c, cs01s02_uc_c, cs11n01_uc_d, **cs11s01_uc_d**, cs11s02_uc_d, cs59g01_uc_f, cs92g01_uc_b, cs95n01_uc_d, cs95n02_uc_d, cs95s01_uc_d, cs95s02_uc_d, csmp_g01_uc_a |
| `out_of_scope` | 3 | cs_interactive_078, _080, _231 |
| `intake_complete_for_uc_h` | 1 | cs29d564_uc_h_ad_removal_appeal_complete_intake |

Findings + judgment:
- **Systemic, not a one-off.** ≥17 cases share the pattern → a per-case override
  campaign is the wrong primary fix; **WP-A observation-only default is the right
  systemic lever**, with WP-B overrides reserved for the genuine unique-reason
  minority. This **raises** WP-A's value and **bounds** the blast radius (§7).
- **Heuristic, not a verdict.** A flag confirms the *shape*; a *true* conflict
  needs the per-case baseline reason-distribution (the cs11s01 method). Two flagged
  cases name `genuine_faq_miss` (`csmp_g01`, `cs92g01_uc_b...genuine_faq_miss`) —
  likely **intentional** faq_miss design with `will_request_human_if` as a late
  fallback; lower conflict probability. The dev sprint should run the cheap
  per-case baseline reason-dist confirmation (zero-LLM, traces exist for the
  pilot-suite/shadow members) before approving any WP-B override for them.
- **Corroborating data point:** `cs11s02_uc_d_password_change_loop_high_distress`
  is already in the baseline's `pre_existing_baseline_failures_ignored` set for
  escalation_compliance — a high-distress case the bot stamps `user_distress`
  (contract-correct per §2.4 "user_expresses_strong_emotion") yet the spec expects
  faq_miss. Independent confirmation the pattern is real and pre-dates the pilot.
- **Schema impact:** the flagged set is heterogeneous (faq_miss / out_of_scope /
  intake_complete) → the unified schema must accept an arbitrary `accepted_reasons`
  list, not a faq_miss/user_requested special case. Confirmed §6 design is adequate.
- **Out of scope for this sprint:** approving the 17. They are de-risked by WP-A's
  observation-only default; per-case WP-B overrides are authored only as needed,
  under review, starting with cs11s01.

---

# 5. Layer-classification + anti-hardcode stanza (§7.1)

**Target failure layer:** `eval_spec` (the gate asked the system to treat an
LLM-owned, stochastic reason-*label* as a deterministic tier-0 floor). Realized as
a scoring-harness/framework change (`hard_checks.py`, `tier_evaluator.py`) → §5.8
framework-defect handling applies.

**Tier-0 invariant:** Adds **no** Tier-0 invariant; **removes** an implicit one
(the eval-internal reason-family→tier-0 binding) and **preserves** the four
deterministic safety checks + Part-1 escalation. No change to
`runtime_freeze_and_risk_policy.md`.

**Semantic hardcode:** No semantic hardcode introduced; an implicit one is removed.
The override (§6) is narrow, explicit, human-reviewed (citation+reviewer+rationale
per entry), not a keyword/enum dump.

**Generalization coverage:** target/neighbor/negative/historical via the zero-LLM
replay (§7) over baseline + exp-1..85 + exp82-reval + the 17 flagged cases. No new
shadow cases authored; shadow firewall respected (replay reads recorded traces in
review capacity).

---

# 7. Zero-LLM OLD/NEW replay — verification plan + matrix (deliverable #5)

All verification is a **pure-Python re-score of recorded traces** (no backend, no
LLM, no new draws). One replay round validates both WP-A and WP-B (decision #3).

## 7.1 Verification items
1. **Replay corpus.** Re-score baseline + exp-81..85 + exp82-reval (+ earlier
   exp-N where present) under OLD and NEW gate; emit the §7.2 matrix.
2. **Negative control.** A trace with should_escalate=true, risk∈{critical,high},
   bot did NOT escalate ⇒ Part-1 still fails tier-0 ⇒ DISCARD under NEW. Proves the
   safety floor intact.
3. **Flip elimination.** exp-82 (orig) and exp82-reval get the **same** tier-0
   result under NEW (no Part-2 flip). Note: exp82-reval's *overall* verdict may
   still be discard on tier-1 (no primary improvement) — consistent with §0.1.
4. **Blast-radius enumeration.** Diff NEW-vs-recorded verdict for every case;
   list every (run, case) whose keep/discard or discard_reason changes. Expected:
   only cases whose sole tier-0 failure was the Part-2 mismatch (subset of the §4
   17 that appear in the replay suites).
5. **Other-invariant invariance.** Assert no_pii_leakage /
   no_critical_policy_violation / no_human_only_tool_exposure /
   phase_transition_validity / Part-1 verdicts are byte-identical OLD vs NEW.
6. **WP-B override application.** With the cs11s01 `accepted_reasons` override
   applied at `enforcement_level: observation`, confirm its observation signal
   reads "match" for either valid reason (still non-gating).

## 7.2 Replay matrix — spec + partial hand-computed population

Full matrix is produced by the WP-A replay harness in dev. Columns:
`run | case | OLD: Part1 | OLD: Part2(family) | OLD esc_compl tier0 | OLD verdict |
NEW: Part1 | NEW: Part2(observation) | NEW esc_compl tier0 | NEW verdict | Δ`.

Partial population (hand-computed now from the OQ-E forensic, zero-LLM):

| run | case | OLD esc_compl tier0 | OLD verdict-driver | NEW Part-1 | NEW Part-2 (obs) | NEW esc_compl tier0 | NEW verdict-driver | Δ |
|---|---|---|---|---|---|---|---|---|
| baseline | cs11s01 | pass (majority) | clean | pass (medium carve-out) | mixed (obs only) | pass | clean | none |
| exp-82 (orig) | cs11s01 | pass | KEEP (tier0 clean) | pass | mixed (obs) | pass | tier0 clean | none |
| exp82-reval | cs11s01 | **FAIL** → tier0 discard | **DISCARD (tier0)** | pass | mismatch (obs) | **pass** | tier1 (no improvement) | **discard reason changes; still not a keep** |
| exp82-reval | cs40s02 | **FAIL** → tier0 discard | (co-driver) | pass | mismatch (obs) | **pass** | n/a | **tier0 contribution removed** |
| exp-84 / exp-85 | cs11s01 | FAIL → tier0 discard | DISCARD (tier0) | pass | mismatch (obs) | pass | (tier1 as applicable) | tier0 contribution removed |

Read: under NEW, the Part-2-only tier-0 failures vanish; **exp82-reval stays a
DISCARD** (now on tier-1 non-improvement, not a flaky tier-0) — it does **not**
become a keep (§0.1). The dev harness must fill every (run × case) row and confirm
no row outside the Part-2 set changes (item 4/5).

---

# 8. Pilot-resume ordering: replay → Codex → re-bless (deliverable #6)

Strict order before the full pilot tranche may resume:

1. **Implement** WP-A + WP-B (dev; independent commits/tests).
2. **Zero-LLM replay** (§7 items 1–6) → produce the matrix + blast-radius list.
3. **Codex §4.1 anti-hardcode review** on (a) the WP-A scoring diff and (b) the
   WP-B schema, **reviewing the replay evidence alongside the diff**. Record both
   verdicts in `docs/codex-findings.md`. *(Codex after replay so it sees the
   blast-radius, not just the code.)*
4. **Human blast-radius sign-off** — every changed historical verdict explained by
   Part-2 demotion, nothing else.
5. **Zero-LLM baseline re-bless** (§9) — only after Codex approve + human sign-off
   (never re-bless against unreviewed code).
6. **Pilot-resume go/no-go** — blocked until 1–5 complete and recorded.

# 9. Re-bless scope (deliverable #6, cont.)

Changing `hard_checks.py`/`tier_evaluator.py` changes scoring-code identity, and the
baseline's `baseline_tier0` map was computed under the OLD check definitions.
**Scope:** a **zero-LLM baseline re-bless of the tier-0 family only** — re-score the
existing baseline run dir (`m-auto-7-prepilot-baseline-20260608`) under the split
checks, regenerate its tier-0 classification map into a **new dated baseline dir**
(auditability), and bump `scoring_code_baseline_sha`. **No new LLM draws** (the
baseline traces exist). The canonical `current_eval_baseline.md` flip stays out of
scope (deferred to milestone close, per the Sprint-088 fence). Pilot resume is
blocked on this re-bless being recorded.

# 10. Decisions locked (2026-06-18 human review) + residual

**Promoted:** this plan is approved + promoted to `docs/sprint_objective.md`
(Sprint 092 / S-Auto-38); dev prompt `compact/sprint-092-dev-prompt.md`. Full
pilot tranche HELD.

- **OQ-1 — RESOLVED (decision #1):** Part-2 = observation-only Phase 1; tier-1
  promotion only via the §3A.3 exit conditions. ✔
- **OQ-4 — RESOLVED (decision #1, this round):** the **13 faq_miss-flagged cases**
  ARE in this sprint as **pre-dev zero-LLM discovery** — reuse existing
  baseline/historical traces only (no new LLM); per case emit persona/runtime
  paths + current expected trigger + baseline reason distribution + classification
  ∈ {`genuine_faq_miss`, `dual_path_conflict`, `unknown`}; **`unknown` when
  evidence is insufficient — no inference, no auto-override**. Confirms schema +
  blast radius; NOT a per-case-override fix campaign. ✔
- **OQ-3 — RESOLVED (decision #2):** re-bless = **new dated dir** (no in-place
  overwrite); old artifacts immutable; OLD/NEW maps side-by-side; record
  `scoring_code_sha` + `source_baseline` + `generated_at` + `gate_definition_version`;
  canonical pointer flip deferred to milestone close. ✔
- **cs11s01 Option A — RESOLVED (decision #3):** recommended trigger
  `{faq_miss_threshold_exceeded, user_requested}` stays **`pending_review`** until
  product-owner confirmation; does **not** block WP-A or the WP-B schema; only
  blocks registering the cs11s01 override. cs40s02 unchanged; exp-82 withdrawn. ✔
- **Extra constraint — LOCKED:** when Part-2 goes global observation-only, **no
  implicit reason binding via the family map**. Genuine strong-binding cases need an
  explicit approved override (citation+rationale+reviewer+approved; tier-0 also
  `safety_critical:true`). **Implementation + replay must prove approved overrides
  take effect** (fixtures at each `enforcement_level`), forbidding the intermediate
  state "Part-2 demoted but a required binding override not wired." ✔
- **OQ-2 — residual (dev):** override loader validation details (accepted_reasons
  ⊕ accepted_families mutual-exclusion; tier0 guard rejection) — finalize in WP-B.
