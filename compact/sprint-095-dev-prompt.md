# Dev prompt — Sprint 095 / S-Auto-43 (M-Auto-9 WP1): `user_requested` escalation-reason honesty

You are the **dev agent for Sprint 095 / S-Auto-43 (M-Auto-9), WP1 only**. One-line
goal: when the **bot itself initiates handover**, stop the LLM from labeling the
escalation `escalation_reason=user_requested` (the user never asked for a human) and
have it use an **accurate bot-initiated reason from the existing approved vocabulary**.
**Reason-label HONESTY only — do NOT change whether or when handover occurs, any phase
transition, or any outcome.** This is the first dev sub-sprint of the APPROVED M-Auto-9
runtime-closure milestone; **it does NOT solve the PRIMARY closure / product-contract
question** (charter §6).

**Read order (minimal):** `AGENTS.md` (auto-loaded governance chain) + this prompt.
Source-of-truth contract = `docs/sprint_objective.md` (Sprint 095; same content,
fuller). Everything you need is embedded below; the paths are read-on-demand anchors.

## The defect (verified read-only — confirm, don't re-derive)
- `escalation_reason` is **LLM-supplied** in the `request_handover` args:
  `server/src/main/java/com/gumtree/csagent/service/tools/RequestHandoverTool.java:60`
  reads it verbatim (`:87`/`:102` persist). On bot-initiated escalations the LLM
  supplies `user_requested` though the user never asked for a human.
- `user_requested` is **priority 1** (Tier-0) in `EscalationReasonResolver.PRIORITY`
  (`server/.../service/runtime/EscalationReasonResolver.java:84-113`) → once supplied
  it **wins precedence** and never gets overwritten by a runtime fallback. Mislabel
  sticks.
- `confirm.yaml:20` (`server/src/main/resources/skills/confirm.yaml`) prescribes
  `reason 'user_dissatisfied'` for the not-satisfied handover, but `user_dissatisfied`
  is **NOT** in the canonical 23-value enum (`EscalationReasonResolver.CANONICAL_REASONS`,
  `:51-75`) → it canonicalizes to `service_degraded`, and there is **no guidance**
  reserving `user_requested` for an actual user request.
- This is the **one actionable defect** WP1 owns. The **budget-family** runtime
  mislabel (`turn_budget_exhausted`/`faq_miss_threshold_exceeded`/
  `clarification_budget_exhausted` via `resolveMaxStepsReason`) is a **separate vector
  — out of scope, frozen**.
- Evidence: OQ-S93.1 brief `docs/diagnostics/failure-briefs/oq-s93.1-confirm-record-vs-handover.md`
  §3.2/§3.3 → 13/22 PRIMARY escalations = **2 genuine user-accepted + 6 bot-initiated
  `user_requested` mislabel + 5 budget-family**. exp-90 also carries ≥1 `user_requested`
  on `loaded_listing`.

## Allowed surfaces (layer = `prompt_projection` + `semantic_planner`)
1. `server/src/main/resources/skills/confirm.yaml` — replace the non-canonical
   `user_dissatisfied` with a **valid canonical** bot-initiated reason; tighten the
   procedure / `escalation_policy` so the LLM reserves `user_requested` for an actual
   user request and uses a bot-initiated reason when the bot decides to escalate.
2. `server/.../service/runtime/ContextProjectionBuilder.java` (+ any reason-vocabulary
   template it renders) — surface the approved escalation-reason vocabulary to the LLM
   with the `user_requested`-is-for-actual-user-requests distinction explicit.
   Projection/teaching text only; **no runtime branch on user-message content**.
3. Other `server/src/main/resources/skills/*.yaml` — **only if** they prescribe
   `user_requested`/a non-canonical reason for a bot-initiated handover; same minimal
   correction.
4. Java tests under `server/src/test/java/...`.

The reason label is **LLM-supplied, not a runtime stamp** → the fix is projection/skill
teaching, never a Java guard.

## FROZEN — do NOT touch (STOP-and-surface if the fix seems to need any of these)
phase-machine transitions; `PhaseEvaluator` (incl. `mapFinalAnswer`); `ResolveDisposition
Evaluator`; premature-resolve guard; RESOLVE→CONFIRM promotion; `BotTurn.sourceIds` /
`priorGroundedResolveAnswerDelivered` (WP0); `record_outcome` semantics; `isResolved
SuccessTerminal`; max-turn behaviour; handover **eligibility** / escalation **policy**;
**runtime fallback reason stamping** — `EscalationReasonResolver` precedence table +
`canonicalize` + `resolveMaxStepsReason` + the budget-family reasons; `detectExplicit
UserEscalation` (the runtime genuine-user-request path — leave intact); CaseSpec /
PRIMARY expectations; eval-spec widening; baseline / scoring / canonical pointer /
re-bless; WP0 / WP2 / companion persona / objective-alignment annotation; the autoloop
pilot; `discover_triage.$.procedure`. **No user-message keyword/regex/content heuristic,
no CaseSpec IDs, no fixed utterances, no ad IDs, no benchmark branch (§1.5/§1.7). NO new
`escalation_reason` enum value** (that is the deferred `D-new-escalation-reason-enum`,
`action_bank.md` §4 — needs its own migration; STOP-and-surface, do not invent one).

## Validation contract (do in order; evidence → `docs/sprints/sprint-095-handoff.md`)
1. **Characterize before editing** (read-only): classify each escalation into exactly
   one of {genuine user-requested; bot-initiated mislabeled `user_requested`;
   runtime-owned budget-family fallback (unchanged)}. Primary set =
   `autoloop/results/runs/exp-90/eval-results.json` (present, 4.4M; the two PRIMARY
   case_results). Corroborate with the OQ-S93.1 brief §3.3 counts — the raw Sprint-093
   dirs `eval_interactive/results/2026-06-18-*` are **gitignored / may be absent**; use
   the brief's recorded classification as the documented Sprint-093 evidence.
2. **Add Java characterization tests** proving: genuine request → `user_requested`;
   bot-initiated handover → approved **non**-user-requested canonical reason; handover
   **decision** + **phase transition** + `containment_outcome` unchanged; the §"standing
   guards" precedence holds (a real `trust_safety_required`/`payment_dispute_detected`/
   `gdpr_intake` is NOT displaced by the new low-priority bot default, and the new
   default does NOT displace a genuine `user_requested`). No CaseSpec IDs / fixed
   utterances / ad IDs / benchmark branches.
3. **Run the Java suite — no new regression.** Documented post-Sprint-093 baseline
   `1394 / 1 / 0 / 2` (sole failure = inherited, provably-uncoupled `SystemPromptUser
   RequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`, OQ-S41.5).
   **Re-measure on a clean tree at start + attribute any delta** — prompts have carried
   stale baselines; your new tests add to the run total; the gate is "no *new*
   regression vs the re-measured baseline."
4. **Zero-LLM attribution replay** over exp-90 (+ the documented Sprint-093
   classification): existing bot-initiated `user_requested` mislabels are **corrected**
   by the new vocabulary/skill mapping; genuine user-requested cases stay
   `user_requested`; **no outcome / phase / handover-decision change**.
5. **No real-LLM run required** unless implementation reveals behaviour beyond
   reason-label projection (→ STOP-and-surface). Certifiable WP1 claim = wiring +
   historical-mislabel attribution + no-regression (zero-LLM + Java). The "live LLM now
   picks the honest reason" claim is an LLM-behaviour change that §5.7 would gate on a
   real-LLM run; WP1 does **not** claim it (deferred to the WP2 companion gate).
6. **§4.1 anti-hardcode kernel** — per-sub-sprint Codex REQUIRED (confirm.yaml +
   projection are a semantic surface, §4.3). Verdict verbatim → `docs/codex-findings.md`
   (§4.2). No close until `pass`.

## STOP-and-surface (surface real cause as OQ, don't expand scope)
- **Existing-reason honesty gate (binding — the two ONLY acceptable outcomes):** if an
  existing approved enum value **accurately describes** the bot-initiated handover →
  use it, **complete WP1**. If **no** existing value is **semantically honest enough**
  → **STOP and surface the vocabulary gap** under `D-new-escalation-reason-enum`
  (`action_bank.md` §4); **WP1 is BLOCKED, not complete** (do not declare it done). Do
  **not** add a new enum and do **not** ship a knowingly inaccurate catch-all (e.g.
  `service_degraded`) just to replace `user_requested` — swapping one dishonest label
  for another is not the fix. There is **no** "honesty floor" that justifies an
  inaccurate label.
- The honesty win can be shown **only** by the LLM changing its choice (not pinnable by
  wiring/zero-LLM/Java) → STOP (real-LLM territory).
- Any **frozen** surface would have to move (e.g. a runtime stamp, not the LLM arg, is
  the source; or the resolver precedence would need editing) → no longer WP1; STOP.
- A standing guard regresses; or `exp-90/eval-results.json` is absent/unreadable.

## Standing guards (must stay green)
`anchor_uc_g_gdpr` · `anchor_uc_fp_removed` · `cs095_uc_d_email_recovery_misroute` ·
UC-J scam/trust-safety precedence (`cs38s01`) · UC-I payment · UC-G GDPR ·
explicit-human-request (genuine request still → `user_requested`) ·
genuinely-unresolved escalate-after-help (escalation still happens; only the bot-
initiated label changes). Core safety argument: any honest bot-initiated reason you'd
use is **low-priority** (tier-3+, e.g. `service_degraded` is priority 30) and **cannot**
displace safety/dispute reasons (priorities 0–16); steering off `user_requested`
(priority 1) can only *reduce* false Tier-0 reasons — verify this explicitly. (Low
priority is necessary, NOT sufficient — it does not make a value *honest*; that is the
existing-reason honesty gate above.)

## §7 stanza
Target layer `semantic_planner` (LLM escalation-reason choice, §1.3) + `prompt_projection`
(approved vocabulary + confirm.yaml text); adds **no Tier-0** (no Java guard; resolver +
`detectExplicitUserEscalation` unchanged); **no semantic hardcode** (teaching text +
reuse an existing canonical reason; no keyword/regex/enum-expansion/per-UC matrix/
case-id/user-content check); coverage target/neighbor/negative/shadow = bot-initiated
mislabel traces (exp-90 + Sprint-093 doc) / other-UC bot-initiated escalations
(no regression) / genuine user-request + §standing-guards precedence / held-out N/A for
a label-only zero-LLM+Java change (no case family, no real-LLM run; deferred to WP2).

## Rollback / commit discipline
Stage explicitly **by file** (NO `git add -A`). `confirm.yaml`/skill edits and
`ContextProjectionBuilder` edits = separable, independently revertible commits; tree
green (no new regression) at every commit boundary; no data/eval artifacts committed
(gitignored). If STOP fires after a partial edit, revert it to the documented baseline
before handing back.

## Required explicit records (objective + handoff)
- WP1 improves **reason-label honesty only**; it does not change whether/when handover
  occurs, nor any phase/outcome.
- WP1 **does not solve** the M-Auto-9 PRIMARY closure / product-contract question.
- **WP0 and WP2 remain HELD** (charter §6).

## Self-check before close
- [ ] Three-class defect attribution recorded before any edit.
- [ ] Tests: genuine→`user_requested`; bot-initiated→approved non-user reason; handover
      decision + phase + outcome unchanged; standing-guard precedence holds; no
      case-id/utterance/ad-id/benchmark branch.
- [ ] Java suite re-measured; no new regression vs clean-tree baseline.
- [ ] Zero-LLM replay: mislabels corrected, genuine preserved, no outcome/phase/handover
      change.
- [ ] No frozen surface touched; no new enum value; no user-message heuristic.
- [ ] Existing-reason honesty gate resolved one of two ways: an existing reason is
      accurate → COMPLETE; OR no honest value exists → WP1 **BLOCKED** + vocabulary-gap
      OQ (not declared done). No knowingly-inaccurate catch-all shipped.
- [ ] §4.1 Codex `pass` in `docs/codex-findings.md`.
- [ ] Handoff records the chosen bot-initiated reason + why it is honest within the
      existing enum; WP1-only + WP0/WP2-HELD restatements present.
