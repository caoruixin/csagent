---
title: "Sprint 095 / S-Auto-43 (M-Auto-9 WP1) — user_requested escalation-reason honesty"
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-20
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  First dev sub-sprint of the APPROVED M-Auto-9 runtime-closure design milestone
  (charter docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md
  §6 WP1). SCOPED, NOT LAUNCHED — this file + compact/sprint-095-dev-prompt.md are
  the deliver contract; the dev agent is not spawned until the human pastes the dev
  prompt. Scope is narrowly the `user_requested` escalation-reason mislabel on
  bot-initiated handover: reason-label HONESTY only. It does NOT change whether or
  when handover occurs, and it does NOT solve the M-Auto-9 PRIMARY closure /
  product-contract question (the unsatisfiable-persona problem). WP0 (source_ids /
  promotion-evidence) and WP2 (CONFIRM record-vs-handover on satisfiable flows)
  remain HELD under charter §6. Layer = prompt_projection / semantic_planner; no
  frozen runtime surface; no new enum value; no real-LLM run required (zero-LLM
  attribution replay + Java characterization is the evidence gate, charter §6 +
  §5.7). Parent milestone north-star = the M-Auto-9 charter proposal doc (a formal
  docs/milestone_objective.md promotion to M-Auto-9 is a recommended follow-up, not
  done in the scoping session).
---

# Sprint 095 / S-Auto-43 (M-Auto-9 WP1) — `user_requested` escalation-reason honesty

## 0. Status

**SCOPED — awaiting human launch.** This is the deliver-agent contract for the
first M-Auto-9 dev sub-sprint. The dev agent has **not** been spawned. Launch =
the human pastes `compact/sprint-095-dev-prompt.md` into a fresh dev session.
`implementation_status: not_started`.

- **Parent milestone:** M-Auto-9 (runtime/orchestration closure DESIGN milestone),
  design FINAL verdict `APPROVE` 2026-06-20. North-star =
  `docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md`
  (charter §6 "Work-package decomposition", WP1).
- **Sprint-ID assignment (history-checked):** last archived dev sub-sprint =
  Sprint 094 / S-Auto-40 (`docs/sprints/sprint-094-handoff.md`). The pilot-closure
  steps **S-Auto-41** (proposer steering + mutation-surface scoping) and
  **S-Auto-42** (bool-only flaky-tier0 defer rule) ran *without* consuming a
  `Sprint NNN` number — they are recorded only in `docs/action_bank.md` §5, not as
  `sprint-NNN` archives. So the next free `Sprint NNN` is **095** and the next free
  `S-Auto-N` is **43**. There is no `sprint-095`/`sprint-096` archive, no
  `compact/sprint-095*` artifact, and no `S-Auto-43+` reference anywhere in the live
  docs or git history → **Sprint 095 / S-Auto-43** is unambiguous. (The pairing gap
  — 094=S-Auto-40 then 095=S-Auto-43 — is expected, because S-Auto-41/42 never took
  Sprint numbers.)

## 1. Exact objective

Correct the **escalation-reason label** the bot supplies when it **initiates
handover itself**, so the trace stops claiming the customer asked for a human when
they did not.

Required behavioural distinction (the whole of the change):

- `user_requested` (and the other Tier-0 user-signal reasons `user_distress` /
  `imminent_harm`) may be used **only** when the corresponding user signal is
  genuinely present — i.e. the customer actually requested human support / expressed
  distress.
- When the **bot independently decides to escalate** (e.g. it has exhausted grounded
  help and chooses `request_handover`, or the CONFIRM-phase persona stays
  unsatisfied), it must label the handover with an **accurate bot-initiated reason
  drawn from the existing approved vocabulary** (the canonical 23-value
  `escalation_reason` enum), not `user_requested`.

**This is a reason-label honesty change ONLY. It must not change whether or when
handover occurs**, must not change any phase transition, and must not change any
outcome (`containment_outcome`).

### 1.1 The defect (verified, read-only)

- The `escalation_reason` arg is **LLM-supplied** in the `request_handover` tool
  call (`server/.../service/tools/RequestHandoverTool.java:60` reads it verbatim;
  `:87`/`:102` persist it). On bot-initiated escalations the LLM supplies
  `user_requested` even though the user never asked for a human (OQ-S93.1 brief
  §3.2/§3.3: **6/13 PRIMARY escalations** are bot-initiated `user_requested`
  mislabels; exp-90 also shows ≥1 `user_requested` on `loaded_listing`).
- `user_requested` is **priority 1** (Tier-0) in
  `EscalationReasonResolver.PRIORITY` (`EscalationReasonResolver.java:84-113`), so
  once the LLM supplies it, it **wins precedence** and is never overwritten by a
  later runtime budget-family fallback. The mislabel therefore sticks.
- The CONFIRM skill (`server/src/main/resources/skills/confirm.yaml:20`) already
  *intends* a non-user reason for the not-satisfied handover — it prescribes
  `reason 'user_dissatisfied'` — but `user_dissatisfied` is **not** a member of the
  canonical 23-value enum (`EscalationReasonResolver.CANONICAL_REASONS`,
  `:51-75`), so it canonicalizes to `service_degraded` and gives the LLM no clean,
  valid bot-initiated reason to reach for. There is no projection/skill guidance
  that tells the LLM to **reserve** `user_requested` for an actual user request.
- The `user_requested` mislabel is the **one clearly-actionable defect** the
  OQ-S93.1 research and the M-Auto-9 design review both isolated as WP1. It is
  distinct from the **budget-family** runtime mislabel (`turn_budget_exhausted` /
  `faq_miss_threshold_exceeded` / `clarification_budget_exhausted` set by
  `resolveMaxStepsReason` on loop/budget exhaustion — 5/13 escalations; tracked
  separately, **out of scope** here).

### 1.2 What WP1 explicitly does NOT do

- It does **not** solve the M-Auto-9 PRIMARY closure / product-contract objective
  (whether the two PRIMARY personas are resolvable; whether escalate-after-help is
  the correct terminal; making `record_outcome(resolve)` land). Those personas are
  unsatisfiable-by-construction; WP1 changes none of that. Charter §6: WP1 "improves
  reason-label correctness; it does NOT by itself solve the PRIMARY closure blocker."
- It does **not** reduce, increase, or re-time any escalation. The same handovers
  happen on the same turns; only the *label* on bot-initiated ones changes.
- It does **not** touch the runtime fallback reason-stamping vector
  (`resolveMaxStepsReason`, the budget-family reasons) — that is WP-separate and
  frozen here.

## 2. Allowed files / surfaces

| Surface | Path | Allowed change |
|---|---|---|
| CONFIRM skill procedure + escalation-reason vocabulary | `server/src/main/resources/skills/confirm.yaml` | Replace the non-canonical `user_dissatisfied` literal with a **valid canonical** bot-initiated reason; tighten the procedure / `escalation_policy` text so the LLM reserves `user_requested` for an actual user request and uses a bot-initiated reason when the bot itself decides to escalate. |
| Per-turn projection escalation-reason vocabulary | `server/.../service/runtime/ContextProjectionBuilder.java` (+ any reason-vocabulary template/resource it renders) | Surface the approved escalation-reason vocabulary to the LLM with the `user_requested`-is-for-actual-user-requests distinction made explicit. Projection/teaching text only — no runtime branch on user-message content. |
| Other CONFIRM/RESOLVE skills that prescribe a handover reason | other `server/src/main/resources/skills/*.yaml` **only if** they prescribe `user_requested` (or a non-canonical reason) for a bot-initiated handover | Same correction, minimally, where the same mislabel pattern is prescribed. Do not broaden beyond the reason label. |
| Java characterization tests | `server/src/test/java/.../*` (new/extended tests) | Add the §5 characterization + regression tests. |

Layer (§3.2): **`prompt_projection`** (the vocabulary/teaching surfaced to the LLM) +
**`semantic_planner`** (the LLM's reason choice; reason labeling is LLM-owned per
§1.3). The reason label is LLM-supplied, **not** a runtime stamp, so the fix lives in
projection/skill teaching, never in a Java guard.

## 3. Frozen files / surfaces (do NOT touch)

All of the following are out of scope and must remain byte-unchanged in behaviour:

- phase-machine transitions; `PhaseEvaluator` (incl. `mapFinalAnswer`,
  `interpretRunResult`); `ResolveDispositionEvaluator`; the premature-resolve guard
  (`shouldRejectPrematureResolveOutcome`); RESOLVE→CONFIRM promotion.
- `BotTurn.sourceIds` / `priorGroundedResolveAnswerDelivered` (WP0 territory — HELD).
- `record_outcome` semantics or requirements; `isResolvedSuccessTerminal`.
- max-turn behaviour; handover **eligibility** / escalation **policy** (who/when may
  escalate); **runtime fallback reason stamping** — `EscalationReasonResolver`
  precedence table + `canonicalize`, `resolveMaxStepsReason`, and the budget-family
  reasons. WP1 supplies the LLM a better reason; it does **not** change the resolver
  or the runtime stamp.
- `detectExplicitUserEscalation` / the runtime path that sets `user_requested` for a
  genuine user request before the budget check (protects the explicit-human-request
  guard — leave intact).
- CaseSpec / PRIMARY expectation changes; any eval-spec widening; baseline, scoring,
  canonical pointer, re-bless.
- WP0, WP2, the satisfiable companion persona, objective-alignment annotation.
- the autoloop pilot (stays CLOSED — NO KEEP); `discover_triage.$.procedure` lever.
- any claim that WP1 solves the PRIMARY closure objective.
- **user-message keyword/regex/content heuristics**, CaseSpec IDs, fixed utterances,
  ad IDs, or any benchmark-specific branch (§1.5 / §1.7).
- **No new `escalation_reason` enum value.** Adding/renaming a value is the deferred
  `D-new-escalation-reason-enum` item (`action_bank.md` §4) and requires a
  coordinated migration via its own objective — it is **not** in WP1. The fix must
  reuse an existing canonical reason. (See §6 stop-and-surface.)

## 4. Layer-classification + anti-hardcode stanza (§7.1)

**Target failure layer:** `semantic_planner` (the LLM's escalation-reason choice,
LLM-owned per §1.3) + `prompt_projection` (the approved vocabulary surfaced to the
LLM and the `confirm.yaml` reason text). Multi-layer prospective per §7's variant:
the actionable defect is single-layer (LLM-supplied reason label), but the corrective
surface spans projection + skill teaching.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. It adds **no Java
guard**; the existing `EscalationReasonResolver` precedence and the
`detectExplicitUserEscalation` runtime path are unchanged.

**Semantic hardcode:** No semantic hardcode introduced. The change is
projection/skill teaching text plus reserving an **existing** canonical reason; no
keyword / regex / if-else / enum expansion / per-UC matrix / case-id / user-message
content check.

**Generalization coverage:** target / neighbor / negative / shadow =
`bot-initiated user_requested mislabel traces (exp-90 + documented Sprint-093
classification, ≥2 PRIMARY shapes)` / `other-UC bot-initiated escalations (no
reason/handover regression)` / `genuine user-request cases (must STILL produce
user_requested) + the standing precedence guards in §8` / `held-out — N/A for a
label-only zero-LLM + Java change; no case family is built and no real-LLM run is
required (charter §6 + §5.7); deferred to the WP2 real-LLM companion gate`.

## 5. Required validation contract

The dev agent must do all of the following, in order. Evidence goes in the handoff
`docs/sprints/sprint-095-handoff.md`.

1. **Characterize the current defect before editing** (read-only attribution over
   the cited evidence; classify each escalation into exactly one of):
   - genuine **user-requested** handover (user actually asked for a human);
   - **bot-initiated** handover mislabeled `user_requested`;
   - **runtime-owned fallback** reasons (budget-family `resolveMaxStepsReason`) — must
     remain unchanged.
   Primary evidence = `autoloop/results/runs/exp-90/eval-results.json` (present
   locally, 4.4M; the two PRIMARY case_results). Corroborating = the OQ-S93.1 brief
   §3.3 counts (2 genuine + 6 bot-initiated mislabel + 5 budget-family) — the raw
   Sprint-093 bounded-run dirs (`eval_interactive/results/2026-06-18-*`) are
   **gitignored and may be absent**; treat the brief's recorded classification as the
   documented Sprint-093 evidence, do not assume the dirs are present.

2. **Add tests proving** (Java characterization; no CaseSpec IDs / fixed utterances /
   ad IDs / benchmark branches introduced):
   - a genuine user request still resolves to `user_requested`;
   - a bot-initiated handover carries an **approved non-user-requested** reason
     (canonical enum member; not `user_requested`/`user_distress`/`imminent_harm`
     absent the user signal);
   - handover **decisions** and **phase transitions** are unchanged (same handover,
     same turn, same phase, same `containment_outcome`);
   - the standing precedence guards in §8 hold (a legitimate higher-priority safety
     reason — e.g. UC-J `trust_safety_required`, UC-I `payment_dispute_detected`,
     UC-G `gdpr_intake` — is **not** displaced by the new bot-initiated default, and
     the new default does not displace a genuine `user_requested`).

3. **Run the relevant Java characterization/regression suite — no new failures**
   against the documented baseline. Documented post-Sprint-093 baseline =
   **`1394 / 1 / 0 / 2`** (sole failure = the inherited, provably-uncoupled
   `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`,
   OQ-S41.5). **Re-measure at sprint start and attribute any delta** — do not trust
   the stated number blindly (prompts have carried stale baselines); the gate is "no
   *new* regression vs the re-measured clean-tree baseline," and your new tests add to
   the run total.

4. **Zero-LLM attribution replay** over the cited Sprint-093 / exp-90 evidence
   showing, deterministically (no LLM call):
   - existing bot-initiated `user_requested` mislabels are **corrected** under the
     new vocabulary/skill mapping (the reason the wiring would now prescribe is a
     bot-initiated reason, not `user_requested`);
   - genuine user-requested cases remain correctly labeled `user_requested`;
   - **no outcome, phase, or handover-decision change** is introduced by the replay.

5. **Real-LLM run is NOT required** for this label-only sub-sprint **unless**
   implementation reveals behaviour beyond reason-label projection (then STOP and
   surface — see §6). Rationale: charter §6 ("No real-LLM run is required to certify a
   label change"). **Honesty note on §5.7:** the change touches prompt-projection /
   skill text the LLM consumes, so the *certifiable* WP1 claim is the **wiring +
   historical-mislabel attribution + no-regression** (zero-LLM replay + Java tests).
   The stronger claim "the live LLM now reliably picks the honest reason" is an
   LLM-behaviour change that, per §5.7, would need a real-LLM run; WP1 does **not**
   claim it — it is deferred to the WP2 real-LLM companion gate. If the dev concludes
   the honesty win can be demonstrated *only* by the LLM changing its choice (i.e. it
   cannot be pinned by the wiring at all), that is the §6 stop-and-surface trigger.

6. **Apply the existing §4.1 anti-hardcode review kernel** (per-sub-sprint Codex
   REQUIRED — confirm.yaml + projection are a semantic surface; §4.3 trigger). Record
   the verdict verbatim in `docs/codex-findings.md` (§4.2). No close until `pass`.

## 6. Stop-and-surface conditions

STOP and surface (do not work around) if any of these arise:

- **Existing-reason honesty gate (binding — supersedes any "ship the floor anyway"
  reading).** Resolve the bot-initiated reason as follows, and these are the *only*
  two acceptable outcomes:
  - **If an existing approved `escalation_reason`** (a member of the canonical
    23-value enum) **accurately describes** the bot-initiated handover → use it and
    **complete WP1**.
  - **If no existing enum value is semantically honest enough** for the bot-initiated
    handover → **STOP and surface the vocabulary gap** under the deferred new-reason
    OQ (`D-new-escalation-reason-enum`, `action_bank.md` §4). In that case **WP1 is
    BLOCKED, not complete — do not declare it done** (see §9 / §10).
  - **WP1 must not add a new enum value**, and **must not ship a knowingly inaccurate
    catch-all** (e.g. `service_degraded`) merely to replace `user_requested`. Swapping
    one dishonest label for another dishonest label is **not** the honesty fix. There
    is no "honesty floor" that justifies an inaccurate label: an honest existing reason
    or a recorded BLOCK are the two — and only — acceptable results.
- **The change cannot be certified without a real-LLM run** (§5.5 trigger): the
  honesty win depends solely on the LLM changing its choice and cannot be pinned by
  the wiring / zero-LLM replay / Java tests.
- **Any frozen surface would have to move** to make the label honest (e.g. it turns
  out a runtime stamp, not the LLM arg, is producing `user_requested`, or the resolver
  precedence would need editing). That is no longer WP1.
- **A standing guard (§8) regresses** in the zero-LLM replay or Java tests.
- The required evidence (`exp-90/eval-results.json`) is absent or unreadable.

Follow the wrong-layer-fix discipline: ship the safe in-scope honesty part if any
exists, surface the real cause as an OQ, and do not silently expand scope.

## 7. Rollback requirements

- Single-purpose, separable commits; stage explicitly **by file** (no `git add -A`).
  The `confirm.yaml`/skill edits and the `ContextProjectionBuilder` edits should be
  separable so either can be reverted independently.
- Each commit must leave the Java suite at "no new regression vs the re-measured
  baseline" (the tree is green at every commit boundary).
- No data/artifact files committed (bounded-run/eval artifacts stay gitignored).
- If STOP-and-surface fires after a partial edit, `git revert`/reset the partial edit
  so the tree returns to the documented baseline before handing back; do not leave a
  half-applied vocabulary change.

## 8. Standing guards that must remain green

- `anchor_uc_g_gdpr`
- `anchor_uc_fp_removed`
- `cs095_uc_d_email_recovery_misroute`
- UC-J scam / trust-and-safety precedence (e.g. `cs38s01`)
- UC-I payment precedence
- UC-G GDPR precedence
- explicit-human-request behaviour (a genuine user request still produces
  `user_requested`)
- genuinely-unresolved escalate-after-help behaviour (the escalation still happens;
  only its label changes when the bot — not the user — initiated it)

These are precedence/blast-radius guards: any honest bot-initiated reason WP1 would use
is a **low-priority** value (tier-3+, e.g. `service_degraded` is priority 30) that
**cannot** displace the high-priority safety/dispute reasons (priorities 0–16), so
steering the LLM off `user_requested` (priority 1) can only *reduce* false Tier-0
reasons — verify this property explicitly, it is the core safety argument for WP1. (This
precedence fact does **not** make any given value *honest* — honesty is judged by the §6
existing-reason honesty gate; a value being low-priority is necessary, not sufficient.)

## 9. Acceptance gates (close checklist)

- [ ] Defect characterized (§5.1) before any edit; three-class attribution recorded.
- [ ] Tests added (§5.2): genuine-request→`user_requested`; bot-initiated→approved
      non-user reason; handover decision + phase + outcome unchanged; §8 precedence
      guards hold. No CaseSpec IDs / fixed utterances / ad IDs / benchmark branches.
- [ ] Java suite re-measured + no new regression vs the clean-tree baseline (§5.3).
- [ ] Zero-LLM attribution replay passes (§5.4): mislabels corrected, genuine cases
      preserved, no outcome/phase/handover-decision change.
- [ ] No frozen surface (§3) touched; no new enum value; no user-message heuristic.
- [ ] §4.1 anti-hardcode kernel `pass` recorded in `docs/codex-findings.md`.
- [ ] Handoff `docs/sprints/sprint-095-handoff.md` records: three-class attribution,
      the chosen bot-initiated reason + why it is honest within the existing enum,
      test list, re-measured baseline + delta attribution, zero-LLM replay matrix,
      Codex verdict, and the explicit restatements in §10.

**Two terminal outcomes only.** WP1 closes as **COMPLETE** only when an existing
approved reason honestly labels the bot-initiated handover and every gate above is met.
If the §6 existing-reason honesty gate fires (no existing value is honest enough), WP1
closes as **BLOCKED**: the deliver-agent archives this contract and records the
vocabulary-gap OQ under `D-new-escalation-reason-enum`; the gates above are explicitly
**not** claimed. A knowingly-inaccurate catch-all is never a valid COMPLETE.

## 10. Required explicit records (deliverable)

The objective + handoff must state, verbatim in spirit:

- **WP1 improves semantic / reason-label honesty only.** It does not change whether
  or when handover occurs, nor any phase transition or outcome.
- **WP1 does not solve the M-Auto-9 PRIMARY closure / product-contract question**
  (the unsatisfiable-persona problem; making `record_outcome(resolve)` land).
- **WP0** (`source_ids` / promotion-evidence) and **WP2** (CONFIRM record-vs-handover
  on satisfiable flows) **remain HELD** under charter §6 conditions.

## 11. Evidence and closeout artifacts

- `docs/sprints/sprint-095-handoff.md` — dev handoff (per §9 / §10).
- `docs/codex-findings.md` — §4.1 per-sub-sprint Codex verdict.
- On close, deliver-agent archives this file → `docs/sprints/sprint-095-objective.md`
  (milestone-framework §8.3); `compact/sprint-095-dev-prompt.md` stays in place as the
  historical executable view; `action_bank.md` §5 `R-oq-s93.1-confirm-record-vs-handover`
  status updated (WP1 closed; WP0/WP2 still HELD).

## 12. Codex review plan

Per-sub-sprint Codex **REQUIRED** (§4.3 trigger #3 — the escalation-reason vocabulary
in `confirm.yaml`/projection sits on the §1.7 semantic surface). The kernel walks the
nine questions over the `confirm.yaml`/skill + `ContextProjectionBuilder` edits + the
new tests, against the §8 blast-radius. Verdict → `docs/codex-findings.md` (§4.2). No
close until `pass`.
