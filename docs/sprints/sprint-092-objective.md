---
title: "Sprint 092 / S-Auto-38 — escalation_compliance tier-0 reclassification (inserted blocker)"
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-18
review_cadence: per sprint
supersedes: docs/sprints/sprint-088-objective.md
superseded_by: null
notes: >
  INSERTED BLOCKER (§5.8 framework-defect: scoring harness). Promotes the
  human-approved plan
  docs/solutions/2026-06-18-escalation-compliance-gate-reclassification-subsprint-plan.md
  (rev 2) + the companion override decision
  docs/solutions/2026-06-18-cs11s01-expected-trigger-override-decision.md. Root
  cause + forensic evidence: docs/sprints/sprint-088-handoff.md (OQ-E + expected-
  trigger verification; commits 35d2a7b0 / 56686db0). The CS4 pilot (Sprint 088 /
  S-Auto-33) is HELD — its contract is snapshot at docs/sprints/sprint-088-
  objective.md and is re-promoted VERBATIM when this blocker closes + the binding
  gates (§Binding gates) pass. FULL PILOT TRANCHE STAYS HELD throughout; no real-
  LLM run beyond zero-LLM replay; exp-82 stays WITHDRAWN; no PRIMARY-success claim.
---

# Sprint 092 / S-Auto-38 — escalation_compliance tier-0 reclassification

> Detailed design, schema, replay matrix, sweep results, and verification items
> live in the approved plan (rev 2). This contract is the authoritative scope +
> gate set; the plan is the reference. Read the plan §3A/§6/§7/§8/§9 before dev.

## Class

| Field | Value |
|---|---|
| **Layer (§3.2)** | `eval_spec` — the gate treated an LLM-owned, stochastic `escalation_reason` *label* as a deterministic tier-0 floor. Realized as a scoring-harness/framework change (§5.8). |
| **§7 stanza** | **REQUIRED** (semantic-evaluation surface) — see §7. |
| **Per-sub-sprint Codex (§4.3 / §4.1)** | **REQUIRED** — anti-hardcode kernel on the WP-A scoring diff AND the WP-B schema; reviewed against the replay evidence (a binding gate). |

## Goal

Stop sampling noise in the LLM's escalation-**reason** choice from flipping
KEEP↔DISCARD, **without** weakening any deterministic safety floor and **without**
silencing a genuine "should-escalate-but-didn't." Split the globally-injected
`escalation_compliance` into Part-1 (behaviour; stays zero-tolerance tier-0) and
Part-2 (reason-family match; **observation-only**, removed from
`_TIER0_PY_FAMILY`).

### What this sprint does NOT do (binding)

- Does **not** restore **exp-82** — it stays WITHDRAWN on its n=13 primaries
  (P_improve 0.059 / 0.022). Removing a flaky tier-0 rejection only changes a
  *discard reason*; it never makes exp-82 keep-worthy.
- Does **not** claim any **PRIMARY success** (primaries remain 0/13).
- Does **not** weaken `no_pii_leakage`, `no_critical_policy_violation`,
  `no_human_only_tool_exposure`, `phase_transition_validity`, or Part-1 escalation.
- Does **not** start the full pilot tranche or any real-LLM run.

## Scope — two independent work packages (separate commits)

### WP-A — gate split (commit boundary: scoring harness only)
- Split `hard_checks._check_escalation_compliance` → Part-1 (`escalation_compliance`)
  + Part-2 (`escalation_reason_family_match`, new name). Both stay in the global L1
  set (still run + report); only Part-2's gate tier changes.
- Remove `escalation_reason_family_match` from `tier_evaluator._TIER0_PY_FAMILY`;
  Part-2 = **observation-only** (recorded in verdict + report.html, never gates).
- Touches only `eval_interactive/.../hard_checks.py` + `autoloop/.../tier_evaluator.py`
  (+ tests). **No** CaseSpec, no override-registry, no schema in this commit.
- Acceptance: plan §3A.5.

### WP-B — unified override schema + cs11s01 pending override (separate commit)
- Implement the unified override `escalation:` block (plan §6): `accepted_reasons`
  / `accepted_families`, `enforcement_level: observation|tier1_confirmed|tier0`,
  `safety_critical`, `citation`, `rationale`, `reviewer`, `status`. Loader rejects
  `tier0` without `safety_critical:true + citation + approved`; the
  `escalation_reason_family_match` check consumes approved overrides.
- Draft the cs11s01 override as `status: pending_review` (companion record) —
  **do NOT enter it into the active registry** until the product owner approves
  (cs11s01 Option A pending; §Open decisions). cs40s02 **unchanged**.
- Touches override-registry schema/loader + the Part-2 check's override consumption
  (+ tests). **No** gate-tier change here (that is WP-A).
- Acceptance: plan §6.1.

> Commit-boundary rule: WP-A and WP-B are independently testable + reviewable.
> WP-A may land first; WP-B does not block WP-A. Neither bundles the other's files.

## Pre-dev zero-LLM discovery (decision #4 — REQUIRED before implementation)

Per-case confirmation of the **13 faq_miss-flagged** cases (plan §4), reusing ONLY
existing baseline / historical run traces (**no new LLM eval**). Per case output:
persona/runtime-allowed escalation paths; current expected trigger; baseline
reason distribution; classification ∈ {`genuine_faq_miss`, `dual_path_conflict`,
`unknown`}. **`unknown` when evidence is insufficient — no inference, no auto-
override.** This discovery confirms the override schema + replay blast radius; it
is **not** a per-case-override fix campaign (the systemic fix is WP-A observation-
only). Recorded in the handoff.

## Binding gates before the pilot tranche may resume (strict order)

1. **Pre-dev zero-LLM sweep** (above) recorded.
2. **WP-A + WP-B implemented** (independent commits/tests).
3. **Zero-LLM OLD/NEW replay** (plan §7) — matrix + blast-radius + negative-control
   (genuine should-escalate-but-didn't still DISCARDs) + flip-elimination + other-
   invariant byte-identical + **approved-override-takes-effect proof** (§Extra).
4. **Codex §4.1** on the WP-A diff + WP-B schema, reviewed against the replay
   evidence; verdict → `docs/codex-findings.md` (§4.2 header).
5. **Human blast-radius sign-off** — every changed historical verdict explained by
   Part-2 demotion, nothing else.
6. **New dated re-bless** (§Re-bless) — only after (4)+(5).
7. **Pilot-resume go/no-go** — blocked until 1–6 complete + recorded; resume =
   re-promote `sprint-088-objective.md` verbatim.

## Extra implementation constraint (decision — no intermediate gap)

When Part-2 goes global observation-only, **no reason binding may be retained
implicitly via the evaluator family map.** Any case whose product contract
genuinely requires a strong reason binding must use an **explicit approved
override** (citation + rationale + reviewer + `status: approved`; tier-0
additionally requires `safety_critical: true`). **Implementation + replay must
prove these approved overrides actually take effect** — explicitly forbidding the
intermediate state "Part-2 globally demoted, but a required binding override is
not yet wired." (At sprint start there are zero approved binding overrides; the
proof is: a fixture/approved test override at each `enforcement_level` is honoured
in the replay.)

## Re-bless (decision #2 — new dated directory; no in-place overwrite)

Write a **new dated baseline dir** (re-score the existing
`m-auto-7-prepilot-baseline-20260608` traces under the split checks; **zero-LLM**).
Old artifacts stay immutable; OLD and NEW tier-0 maps are side-by-side comparable.
Record in the new dir's metadata: `scoring_code_sha`, `source_baseline`,
`generated_at`, and a `gate_definition_version`. The canonical
`current_eval_baseline.md` / `config.fitness.baseline_dir` pointer flip stays
**deferred to milestone close** (separate human decision).

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `eval_spec` (gate config treated a stochastic LLM-owned
reason label as a deterministic floor); realized as a scoring-harness change (§5.8).

**Tier-0 invariant:** Adds **no** Tier-0 invariant; **removes** an implicit one
(the eval-internal reason-family→tier-0 binding) and **preserves** the four
deterministic safety tier-0 checks + Part-1 escalation. No change to
`runtime_freeze_and_risk_policy.md`.

**Semantic hardcode:** No semantic hardcode introduced; an implicit one is removed.
The override (WP-B) is narrow, explicit, human-reviewed (citation/reviewer/rationale
per entry), not a keyword/enum dump.

**Generalization coverage:** target / neighbor / negative / shadow via the zero-LLM
replay over baseline + exp-1..85 + exp82-reval + the 17 flagged cases (plan §4/§7).
No new shadow cases; shadow firewall respected (replay reads recorded traces in a
review capacity).

## Test / eval requirements

- **All verification is zero-LLM replay** of recorded traces (no backend, no LLM,
  no new draws). Mocked-LLM is not primary evidence (§5.7) — but here the evidence
  IS the deterministic re-score of real recorded traces, which is the appropriate
  primary evidence for a scoring-logic change.
- Java + Python suites: no new regression; new replay tests for plan §7 items 2/3/5
  + the approved-override-takes-effect proof.
- Deterministic safety floors byte-identical OLD vs NEW (plan §7 item 5).

## Hard fences / STOP conditions

- **No bot / prompt / runtime change** (`server/src/main/java/**` + skill YAMLs
  byte-identical — this is a scoring-surface sprint).
- **No CaseSpec edit.** cs11s01 → WP-B `pending_review` override only (not
  registered until PO approval). cs40s02 byte-identical.
- **No new dated-dir canonical flip** (`current_eval_baseline.md` /
  `config.fitness.baseline_dir` unchanged; deferred to milestone close).
- **No deterministic-safety-check weakening** (the four + Part-1 preserved + proven
  byte-identical).
- **No implicit reason binding** via the family map (Extra constraint).
- **No real-LLM run; full pilot tranche stays HELD.**
- **STOP** + escalate if: the replay shows any verdict change outside the Part-2
  demotion set (plan §7 item 4); any deterministic safety check changes; a `tier0`
  override lacks `safety_critical+citation+approved`; or an approved override fails
  to take effect in the replay.

## Codex review plan (§4.3)

Per-sub-sprint Codex REQUIRED. Nine-question anti-hardcode kernel on (a) the WP-A
scoring diff and (b) the WP-B schema, reviewed alongside the replay evidence;
verdict to `docs/codex-findings.md` (§4.2 header) before the re-bless + pilot
resume. Prompt drafted by deliver at WP close.

## Handoff requirements

`docs/sprints/sprint-092-handoff.md` records: the pre-dev zero-LLM sweep table (13
cases, classified, `unknown` where insufficient); the WP-A + WP-B diffs + commit
shas (independent); the OLD/NEW replay matrix + blast-radius list + the negative-
control / flip-elimination / other-invariant / approved-override-takes-effect
results; the Codex verdicts; the new dated re-bless dir + its metadata; the pilot-
resume go/no-go; and an explicit restatement that exp-82 stays withdrawn + no
PRIMARY success is claimed.

## Commit discipline

Stage explicitly by file (NO `git add -A`). WP-A and WP-B are separate commits with
disjoint file sets. Re-bless result dirs are gitignored (data). Deliver close-bundle
artefacts bundled by the human at close.

## Open decisions (carried; non-blocking for WP-A)

- **cs11s01 Option A** `{faq_miss_threshold_exceeded, user_requested}` is the
  recommended trigger but stays `pending_review` until the **product owner**
  confirms the FAQ-miss-first path is in-scope. Does **not** block WP-A or the WP-B
  schema; only blocks registering the cs11s01 override.
