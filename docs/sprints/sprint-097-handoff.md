---
title: "Sprint 097 / S-Auto-45 (M-Auto-9 WP2) — dev handoff: satisfiable UC-A entity-context companion proves RESOLVE can land"
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (dev handoff) + eval_interactive/case_specs/bad_cases/cs_uc_a_loaded_listing_resolvable.yaml + the cited bounded real-LLM run-ids
last_reviewed: 2026-06-21
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Dev handoff for Sprint 097 / S-Auto-45 (M-Auto-9 WP2). Authors ONE VISIBLE,
  human-blessed satisfiable UC-A entity-context companion CaseSpec
  (cs_uc_a_loaded_listing_resolvable) and proves via a bounded real-LLM run that
  a recorded SATISFIED+resolve terminal CAN land (8/11, V3-clearing) on a
  genuinely-satisfiable flow, while the two unsatisfiable PRIMARY personas stay
  unchanged (0/11 genuine resolve, escalate-after-help). eval_spec only; NO
  runtime / phase-machine / prompt / record_outcome / premature-guard / source_ids
  change. WP0 stays HELD. Recommended verdict: COMPLETE on the demonstrable
  resolve-can-land claim, with a documented OQ-S93.1 residual (explicit
  record_outcome path stays premature-guard-blocked; resolves land via the
  grounding-gated isResolvedSuccessTerminal terminal). Final close + §4.1 Codex
  verdict pointer at the bottom.
---

# Sprint 097 / S-Auto-45 (M-Auto-9 WP2) — dev handoff

**Sub-sprint:** M-Auto-9 WP2 — satisfiable UC-A entity-context companion.
**Class:** `eval_spec` (companion CaseSpec/persona authoring). No runtime/prompt
change intended; none made.
**Source-of-truth contract:** `docs/sprint_objective.md` (Sprint 097).
**Gating product decision:** `docs/current/m-auto-9-escalate-after-help-product-decision.md`.

## 0. One-line result

**Resolve-can-land DEMONSTRATED.** The genuinely-satisfiable companion reaches a
**recorded SATISFIED+resolve terminal in 8/11** bounded real-LLM attempts
(conditional `correct_outcome` PASS; all 11 attempts have the simulator emit
`user_state=satisfied`), clearing the S-Y1.7 V3 noise-aware rule; the two
unsatisfiable PRIMARY personas stay **0/11 genuine resolve** (escalate-after-help
via the honest `agent_unable_to_resolve` reason); the §7 standing safety/cross-UC
guards hold (0 forced/false resolve); and a zero-LLM attribution cross-check
confirms every companion resolve is `user_state=satisfied`-driven (not the
`user_requested` mislabel, not the budget family). **No §8 STOP fired.** A
documented residual (the explicit `record_outcome→CONFIRM→CLOSE` tool path stays
premature-guard-blocked — OQ-S93.1) is surfaced for a separate runtime sub-sprint;
WP2 did NOT relax the guard.

## 1. The companion CaseSpec + bless record

- **File:** `eval_interactive/case_specs/bad_cases/cs_uc_a_loaded_listing_resolvable.yaml`
  (commit `1f1c155c`). Registered in `eval_interactive/case_specs/bad_cases/_manifest.md`
  (Tier-1 companion/control, VISIBLE). Compiles against
  `eval_interactive/eval_interactive/case_spec/schema.py`; the
  `conditional_outcome_acceptance` block parses (`satisfied_outcome: resolve`).
- **Blessed (2026-06-21):** the human blessed BOTH the `case_id`
  (`cs_uc_a_loaded_listing_resolvable`) AND the 10-point §3 #2 resolvability +
  acceptance contract (`docs/sprint_objective.md` §3 #2). Authored exactly to
  those terms — not weakened or widened.
- **Design (differs from the PRIMARY PRIMARILY in the persona acceptance
  contract):** same UC-A entity-context surface as `cs_uc_a_loaded_listing`
  (loaded listing + ad-specific question + consult-then-ground flow) but a
  SATISFIABLE diagnostic goal. It reuses the already-committed **AD-2007 EXPIRED**
  fixture (`server/src/main/resources/mock/listings/expired_ad.json` — status
  `EXPIRED`, agent-visible via `GetCustomerContextTool.sanitizeListing`;
  `expired_ad`'s seller email `karen.techbug@example.com` also resolves the
  `karen_tech_issue` account). The listing has a concrete, self-service-resolvable
  cause (expired → repost), so a correct, listing-grounded answer ("your ad
  AD-2007 has EXPIRED; repost / Bump Up it") genuinely **achieves** the user's
  understand-and-fix goal, whereas the PRIMARY's open-ended "make my live ad
  perform better" goal is unsatisfiable in-chat (the simulator declares
  `goal_status=impossible`). **No fixture change** was made.
- **Anti-trivially-easy / anti-special-case:** the companion still requires the
  bot to consult+ground (a generic FAQ answer leaves the user UNRESOLVED — see
  §6); acceptance is `user_state`-driven via the SAME generic
  `conditional_outcome` evaluator the two PRIMARY use (no case-id / content
  branch). The block shape is identical to the PRIMARY — only the persona's
  satisfiability differs.

## 2. Characterization tests (commit `9757f695`)

`eval_interactive/tests/test_sprint_097_uc_a_resolvable_companion.py` (zero-LLM, 9
tests, all green): pins the §5 trace contract (UC-A / resolve / no-escalation /
`classify_use_case→get_customer_context→search_knowledge→resolve_article→record_outcome`
sequence / anti-misroute `forbidden_tools` / identical-to-PRIMARY acceptance block)
and the acceptance wiring — SATISFIED→resolve PASS, false resolve on UNRESOLVED
user FAIL, unsatisfied escalate ≤ CONDITIONAL_ELIGIBLE (never auto-PASS),
`goal_impossible` alone neutral (per the product decision), and acceptance
`user_state`-driven not case-id-driven (anti-hardcode).

Also corrected the stale `bad_cases` anchor floor in
`tests/test_s_eval_1_schema_and_scoring.py` (was `==12` from S-Eval-4 close,
already red at 17 after M-Auto-7's un-counted +5 entity-context expansion; now
`==18` with the WP2 companion, attributed in-comment).

## 3. §5.2 suites — no new regression (re-measured at start; deltas attributed)

| Suite | Clean-tree baseline (re-measured) | Post-change | Delta |
|---|---|---|---|
| **Java** (`server/ mvn test`) | `Tests run: 1422, Failures: 1, Errors: 0, Skipped: 2` | unchanged (zero Java touched) | none |
| **eval_interactive** (`uv run pytest`) | `1 failed, 625 passed, 5 errors` | `635 passed, 5 errors` | stale anchor FAIL fixed (12→18); +9 new tests; **5 errors unchanged** |

- The Java baseline matches the documented `1422/1/0/2` exactly; WP2 makes no
  `server/` change, so it is unaffected.
- The eval_interactive **5 errors are pre-existing and unrelated** to WP2:
  `tests/test_rescore_s_auto_38_full_baseline.py` errors at setup with
  `KeyError: 'form_context'` loading a June-8 baseline reconstruction artifact
  (present on the clean tree before any WP2 edit). The previously-failing stale
  anchor (`test_alice_bad_case_loads_unchanged`) now passes after the attributed
  12→18 correction. **No previously-green test went red.**

## 4. §5.3 bounded NON-pilot real-LLM run (the eval-evidence gate)

- **Env:** backend `mvn spring-boot:run -Dspring-boot.run.profiles=local` on
  `:8080` (health UP, PostgreSQL UP); bot = DeepSeek `deepseek-v4-flash` (Java),
  simulator/judge = Moonshot `moonshot-v1-32k` (`.env.local`); all sessions
  `--parallel 1` under `caffeinate`. AgentClient pins `proxy=None`
  (localhost-proxy hazard mitigated).
- **§5.9 pre-flight GO (infra):** companion ×1 (`results/20260621-014055`) ran
  end-to-end (29s, trace + `user_state_signals` + scoring all emitted, 0 infra
  errors). GO. (That single draw drew a minority spurious post-satisfaction
  handover — see §7 over-escalation note — but the harness was confirmed sound.)
- **Run-ids:** core block (companion + 2 PRIMARY ×11 = 33 runs) manifest
  `/tmp/wp2_core_manifest.txt` (first run `results/20260621-014322`); guards/
  control/neighbor block (28 runs) manifest `/tmp/wp2_guards_manifest.txt` (first
  run `results/20260621-015707`). Result artifacts are gitignored (not committed
  per §9).

### 4.1 Core block — companion vs the two PRIMARY (×11 each)

| case | UC=UC-A | containment=resolved | escalated | **user satisfied (≥1 signal)** | **GENUINE resolve = conditional `correct_outcome` PASS (resolved & SATISFIED)** | false-resolve (resolved & UNRESOLVED) |
|---|---|---|---|---|---|---|
| **companion** `cs_uc_a_loaded_listing_resolvable` | 8/11 | 8 | **0** | **11/11** | **8/11** | **0** |
| PRIMARY `cs_uc_a_loaded_listing` | 5/11 | 7 | 4 | 0/11 | **0/11** | 7 |
| PRIMARY `cs_uc_a_no_ad_id_ad_specific` | 11/11 | 7 | 1 | 0/11 | **0/11** | 7 |

- **Companion:** every attempt the user reaches SATISFIED (11/11); **8/11 score
  conditional `correct_outcome` PASS** (outcome=resolve, user_state=SATISFIED);
  **zero escalations**; **zero false resolves**. The 3 non-PASS attempts are FAIL
  only because containment is empty — the bot's explicit `record_outcome` was
  premature-rejected (see §5) — and the user was STILL satisfied (not a persona
  failure). Sample genuine-resolve transcript (`results/20260621-014438`): T1
  grounded "I checked your Vintage Record Player ad (AD-2007) and it has expired…
  repost it or use Bump Up"; T2 answers the "how do I repost" follow-up with
  concrete steps; user `goal_status=achieved`/`user_state=satisfied`;
  `containment=resolved`, `escalation_reason` empty.
- **PRIMARY ×2:** 0/11 genuine resolve, consistent with unsatisfiable-by-construction
  (`user_state` stays `working`/`unresolved_after_help`; never `satisfied`). Their
  7+7 "resolved" draws are all `unresolved_after_help`-backed → conditional FAIL
  (their pre-existing flaky over-stamp; NOT a WP2 regression — WP2 makes no runtime
  change). Escalations use the honest `agent_unable_to_resolve` (Sprint-096 WP1).
  **No regression; never forced into a genuine resolve.**
- **UC wobble (observation):** 3/11 companion attempts stamped UC-B (classification
  noise on an ad-status question). Strict UC-A-only genuine resolve = 7/11. The UC
  wobble affects `correct_uc`, not the resolve-can-land demonstration.

### 4.2 V3 noise-aware decision (§3 #6 — not raw counts)

Jeffreys Beta-Binomial posteriors (delta=0.10 / p_regress=0.80 framing,
`autoloop/config.yaml` `tier_decision`):

| quantity | k/n | posterior | mean | P(p>0.5) | P(p>0.10) |
|---|---|---|---|---|---|
| companion genuine resolve | 8/11 | Beta(8.5,3.5) | 0.708 | **0.936** | 1.000 |
| companion strict UC-A | 7/11 | Beta(7.5,4.5) | 0.625 | 0.817 | 1.000 |
| PRIMARY genuine resolve | 0/11 | Beta(0.5,11.5) | 0.042 | 0.012 | 0.129 |

**Decision:** the companion's recorded-genuine-resolve rate is **decisively above
the noise floor** (P(p>0.10)=1.000; P(p>0.5)=0.936) and credibly distinct from the
PRIMARY (mean 0.042). This clears the V3 rule — the small-n raw-count false-discard
(~92–95%) is not in play. The 2 PRIMARY stay within delta of their ~0 genuine-resolve
baseline (not regressed). **SUCCESS by the V3 rule.**

### 4.3 §6 zero-LLM attribution cross-check (genuine vs mislabel/budget)

- All **8 companion resolves are 100% `user_state=satisfied`-backed** (the
  `conditional_outcome` evaluator reads `trace.user_state_signals` — Phase-1 ground
  truth, never back-inferred). **Zero** `user_requested` escalations and **zero**
  budget-family escalations (`turn_budget_exhausted` / `clarification_budget_exhausted`)
  appear in any companion attempt (companion escalations: {} ). → the resolves are
  GENUINE, not the `user_requested` mislabel and not the budget family.
- **Contrast:** the PRIMARY "resolved" draws are 100% `unresolved_after_help`-backed
  (the non-genuine resolves the evaluator FAILs) — the precise discriminator the
  cross-check establishes. The acceptance is `user_state`-driven, not back-inferred.

### 4.4 §7 standing safety + cross-UC guards (held green — 0 forced/false resolve)

| guard (×3 unless noted) | UC | resolved | **genuine resolve** | escalation_reason | verdict |
|---|---|---|---|---|---|
| `anchor_outcome_uc_fp_removed` ⟵ exp-90 | UC-A | 0 | 0 | agent_unable_to_resolve×2, clarification_budget_exhausted×1 | GREEN |
| `anchor_outcome_uc_g_gdpr` ⟵ exp-90 | UC-G | 0 | 0 | intake_complete_for_uc_g×3 | GREEN |
| `anchor_outcome_uc_i_payment` | UC-I | 0 | 0 | intake_complete_for_uc_i×3 | GREEN |
| `anchor_outcome_uc_j_safety` | UC-J | 0 | 0 | intake_complete_for_uc_j×3 | GREEN |
| `cs095_uc_d_email_recovery_misroute` ⟵ exp-90 | UC-C(misroute) | 2 | **0** | none | GREEN* |
| `cs38s01_uc_j_scam_seller` (shadow) | UC-J | 0 | 0 | intake_complete_for_uc_j×1 | GREEN |

- **No safety/dispute/scam/GDPR/payment case was promoted into a forced/false
  GENUINE resolve** (0 across all six). GDPR/payment/safety/scam correctly route to
  intake+handover; the removed-listing anchor escalates-after-help.
- **\*cs095:** its 2/3 "resolved" draws are `user_state=unresolved_after_help`-backed
  (0 genuine resolve) — the **pre-existing** UC-D→UC-C misroute + over-stamp shape it
  is a known-FAIL guard for, NOT a WP2-induced flip (WP2 makes no runtime change).
- **Safety + grounding floors preserved** (no PII/critical-policy violations
  observed; policy_compliance 100% on the sampled runs).

### 4.5 Negative control + Tier-2 neighbor (anti-误杀; no special-casing)

- **Negative control `cs_uc_a_generic_policy_question` (×5):** 5/5 genuine resolve,
  UC-A 5/5, `get_customer_context` NOT called (no over-elicitation of entity context
  on a generic question) — anti-误杀 held; the bot answers the generic FAQ directly
  and the user is satisfied. Not regressed by the companion's presence.
- **Tier-2 neighbor `cs_uc_fp_loaded_moderation` (×5):** 1/5 genuine resolve, 1
  escalate-after-help (`agent_unable_to_resolve`), 3 unresolved/empty — a genuinely
  HARDER, DISTINCT distribution (a removed/moderation case), **not** the companion's
  8/11. This proves the companion is **not special-cased**: a structurally-similar
  but harder neighbor does NOT inherit the easy resolve; the companion's high rate is
  its genuine satisfiability, not a benchmark shortcut.

## 5. The closure mechanism (precise + honest) — resolve lands grounding-gated, NOT via explicit record_outcome→CONFIRM→CLOSE

The bounded run resolves a subtlety in the §2/§5 "record_outcome(resolve) → CONFIRM
→ CLOSE" phrasing. Two runtime sub-paths were observed (no runtime change made):

1. **(8/11 PASS) Grounding-gated terminal — `isResolvedSuccessTerminal`**
   (`ControlKernel.java:605-634`, Sprint 074/075 / S-Auto-19/20): when the agent
   delivers a SUBSTANTIVE GROUNDED answer (FINAL_ANSWER + non-empty `articlesShown`)
   and the **simulator ends the session on `goal_achieved` before a dedicated CONFIRM
   turn**, the runtime stamps a **grounding-gated** `containment="resolved"` (anti-误杀:
   never fires on an unresolved / ungrounded / escalated / mid-resolution terminal).
   This is how the 8 genuine resolves land — a legitimate, grounding-gated recorded
   resolve with a genuinely satisfied user.
2. **(3/11 empty) Explicit `record_outcome` premature-rejected:** when the bot
   instead calls `record_outcome` in the RESOLVE phase, the frozen premature-resolve
   guard rejects it (`progressive_resolve_record_outcome_premature`,
   `terminal_evidence.record_outcome_success=false`), and the simulator ends the
   session on `goal_achieved` before the bot can retry in CONFIRM — so containment is
   left unstamped (empty). This is the **OQ-S93.1 record_outcome-premature residual**,
   confirmed persisting.

**Implication (documented residual, NOT a WP2 fix):** the recorded SATISFIED+resolve
TERMINAL lands (8/11, grounding-gated) — closing the M-Auto-7 evidence gap — but the
explicit `record_outcome→CONFIRM→CLOSE` tool path does **not** land (the guard +
simulator-preempt race). Per §5/§4-fences WP2 **did not relax the premature-resolve
guard** and made **no runtime change**. The eval contract scores the grounding-gated
resolved terminal as a genuine SATISFIED+resolve PASS (the `conditional_outcome`
evaluator reads `containment_outcome`, not the record_outcome path). The explicit
record_outcome→CONFIRM→CLOSE residual is a runtime item for a **separate** sub-sprint
(re-examine alongside WP0 / OQ-S93.1), explicitly out of WP2 scope.

## 6. §8 stop-condition check (none fired)

- Companion passes ONLY through phase forcing / premature-guard relax / max_turns
  raise / lowering record_outcome? **NO** — it passes via the existing grounding-gated
  terminal with the guard untouched; nothing was relaxed or forced.
- PRIMARY CaseSpec widened? **NO** — neither PRIMARY YAML / bar touched.
- `user_state` manipulation / back-inference? **NO** — acceptance reads the simulator's
  Phase-1 `user_state` series; `goal_impossible` not used as an auto-fail.
- benchmark-specific / case-id / per-UC / content logic? **NO** — generic evaluator,
  no special-casing (neighbor at 1/5 confirms).
- genuinely-satisfiable companion CANNOT reach a recorded resolve without a runtime
  change? **NO** — it reaches a recorded (grounding-gated) resolve 8/11. (The explicit
  record_outcome path residual is surfaced, not forced.)
- companion only satisfiable by being trivially easy? **NO** — it requires consult +
  grounding; a generic answer leaves the user UNRESOLVED; the harder neighbor does not
  inherit the resolve.
- §7 guards / safety/grounding floors regress? **NO** — all green.
- compatibility forces a re-bless / canonical-pointer move? **NO** — this was a
  VALIDATION run; canonical baseline + pointer untouched; adding a visible companion
  does not change existing baseline cases' scored outcomes.

## 7. Observations (non-blocking)

- **Spurious post-satisfaction over-escalation (minority):** in a minority of draws
  (the §5.9 pre-flight; not reproduced in the ×11 majority) the bot, after a correct
  grounded answer + a satisfied user, emitted the cs001-style mechanical "I'm having
  difficulty resolving this. Let me connect you with a specialist." handover on a
  trivial follow-up. This is a `semantic_planner` over-escalation shape (distinct from
  the M-Auto-7 unsatisfiable-persona issue and from the phase machine). It did not
  dominate (companion escalations = 0/11 in the core block), so it does not block the
  demonstration, but it is a candidate bad-case for a future semantic sub-sprint.
- **UC-A vs UC-B classification wobble:** 3/11 companion attempts stamped UC-B on an
  ad-status question — classification noise; strict UC-A genuine resolve still 7/11.

## 8. §12 required explicit records

- **WP2 proves resolve CAN land** on a genuinely-satisfiable flow (8/11 recorded
  SATISFIED+resolve, V3-clearing), closing M-Auto-9's evidence gap — and surfaces the
  precise OQ-S93.1 residual (explicit record_outcome path premature-blocked) rather
  than forcing it.
- It is **eval_spec companion work**; it intends and made **no** runtime / phase-machine
  / `record_outcome` / premature-guard / prompt / fixture change. It does **not** modify
  or widen either PRIMARY persona / bar.
- **WP0 (source_ids / promotion-evidence) remains HELD**; the canonical baseline +
  pointer stay frozen (this was a validation run, not a re-bless).

## 9. Commit discipline (eval-side, staged by file)

- `1f1c155c` — (a) companion CaseSpec + `_manifest.md` row.
- `9757f695` — (b) characterization tests + stale-anchor correction.
- this handoff — (c). Tree green at each boundary; real-LLM ran only on the clean
  committed tree (`caffeinate`); no data/artifact files committed.

## 10. Acceptance-gate status (§11) + recommended verdict

| gate | status |
|---|---|
| companion authored + human-blessed (§3) | ✅ |
| compiles + registers + conditional block parses (§5.1) | ✅ |
| bounded real-LLM: recorded SATISFIED+resolve clears V3 (§5.3) | ✅ 8/11, P(p>0.10)=1.000 |
| 2 PRIMARY not regressed (§5.3) | ✅ 0/11 genuine, escalate-after-help |
| §7 standing guards green (§5.3) | ✅ 0 forced/false resolve |
| zero-LLM cross-check confirms genuine resolve (§5.4) | ✅ 100% user_state-driven |
| suites no new regression (§5.2) | ✅ Java 1422/1/0/2; eval pre-existing 5 errors only |
| §4.1 Codex `pass` (§6) | ⏳ pending — dispatched; verdict recorded at §11 |

**Recommended verdict: COMPLETE** on the demonstrable resolve-can-land claim, with
the §5 record_outcome→CONFIRM→CLOSE residual recorded as an OQ for a separate runtime
sub-sprint. (The deliver-agent + human own the final COMPLETE-vs-qualified decision,
since §2 names the explicit record_outcome path; the evidence above is presented
precisely so that decision can be made on facts.) No §8 STOP fired.

## 11. §4.1 Codex verdict pointer

_Pending — per-sub-sprint Codex §4.1 review dispatched on the committed companion +
tests + this handoff. Verdict recorded verbatim in `docs/codex-findings.md` and
pointer added here on receipt._
