---
title: Performance priority re-plan — eval-contamination first, then architecture
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file (for the priority ordering); cited code/doc paths for every evidence claim
last_reviewed: 2026-07-25
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Rev 2 (2026-07-25) REPLACES rev 1 of this same file. Rev 1 concluded "autoloop
  is low-yield, go fix the agent (FAQ-miss wording + UC routing)". A deeper
  investigation (CaseSpec/scoring review, simulator review, phase/loop review,
  plus a first-principles read of BRD.md + PRD_biz_part.md) showed rev 1 was
  half wrong: the two headline symptoms are primarily caused by the EVAL
  system teaching the agent the wrong behaviour, and by two written-but-stale
  business rules — not by agent wording. Rev 1's WS0 (measurement power) and
  WS4 (projection hygiene) survive; rev 1's WS2/WS3 framing is superseded.
  Three product decisions were taken by the product owner on 2026-07-25 and
  are recorded in §2. This proposal does not itself authorize a sprint; it is
  input to the human + deliver-agent scoping process (AGENTS.md).
---

# Performance priority re-plan (2026-07, rev 2)

## 0. Verdict

The product owner's two felt symptoms — *"things it should finish itself, it
doesn't; things it shouldn't rush to a human on, it escalates immediately"* —
are **the same mechanism seen from two sides**, and that mechanism lives in the
**eval system**, not in the agent's wording.

Concretely: the CaseSpec generator treated *"a human agent took this over"* in
historical transcripts as *"the bot should escalate"*, producing **158 specs
that are simultaneously `should_escalate: true` and `allow_bot_resolution:
'true'`**; and the scoring layer **explicitly refuses to report
over-escalation** while **paying full marks for a 10-character handover
summary**. An agent optimized against this target is being trained to escalate.

Two further causes are written into the governing business docs and were
stale rather than wrong-at-the-time. Both were resolved by product decision on
2026-07-25 (§2).

Separately, the architecture question ("is the phase machine too rigid / is the
agent loop still needed / should we degrade to an agentic workflow") has a
clear code answer: **the capability ceiling is the tool surface, not the phase
machine**, and **the loop's freedom is allocated to the wrong place**. See §4.

## 1. Diagnosis

### 1.1 Loop A — "should have finished it itself, didn't"

| # | Cause | Evidence (verified verbatim) |
|---|---|---|
| A1 | **The generator treats "a human took over" as "the bot should escalate."** The corpus is by definition sessions that reached a human agent, so the human almost always said "let me look into this" / "support team" — the regex fires, `has_hard_escalation_evidence` becomes true, and an FAQ-resolvable UC is stamped `escalate`. | `eval_interactive/eval_interactive/case_spec/case_outcome_resolver.py:196-206`; `transcript_evidence.py:146-208`. Yield: **158 specs** with `should_escalate: true` AND `allow_bot_resolution: 'true'` (anchor 74 / promotion 64 / smoke 6 / case_families 14) |
| A2 | **The linter explicitly waives that contradiction**, and is not wired into CI. | `case_spec/linter.py:221-233` carve-out returns `[]` for (FAQ-resolvable UC + expected escalate + `request_handover` in sequence); `cli.py` has no lint command |
| A3 | **UC-K is architecturally forbidden from consulting the knowledge base.** | `PhaseEvaluator.java:1017` verbatim: `// Intake UCs (UC-G/H/I/J/K) must NEVER invoke knowledge search`. `use-case-registry.yaml` UC-K = `path: INTAKE` + `allow-bot-resolution: false`, but `phase2_domain_realization_spec.md:493` says `partial`. Three hardcoded sets disagree (`case_outcome_resolver.py:23` excludes UC-K; `hard_checks.py:227` and `PhaseEvaluator.java:35` include it) |
| A4 | **Listing/ad status query was out of MVP scope by design.** | `PRD_biz_part.md:228` places it in Phase 1.x, "需 listing/用户 API 与权限评审". **Resolved by decision D1 (§2)** |
| A5 | **The grounding hard gate silently no-ops on exactly the cases that need it.** | `hard_checks.py:1036`: `if grounding_mode != "faq_source_backed": return PASS`. Six bad_cases whose entire purpose is "must be grounded in the user's own listing data" use non-enum values (`listing_data_and_faq_backed`, …); `schema.py:174` constrains the enum only in a comment and `loader.py:80` does not validate |

### 1.2 Loop B — "shouldn't have escalated, did"

| # | Cause | Evidence (verified verbatim) |
|---|---|---|
| B1 | **The BRD mandates escalate-on-frustration.** | `BRD.md:126-138`: "The bot must escalate after: 2 failed intent recognitions, OR customer requests an agent, OR **customer indicates frustration**, OR … out-of-scope." **Resolved by decision D2 (§2)** |
| B2 | **L1 deliberately does not report over-escalation.** | `hard_checks.py:548-550` verbatim: "Over-escalation … is **intentionally NOT reported here**"; `:553` returns pass whenever `should_escalate` is false. No L1 check can fail on a premature escalation |
| B3 | **`correct_outcome` pays full marks for premature handover, asymmetrically.** | `outcome_checks.py:268-286`: expected `resolve`, actual `escalate` ⇒ if handover `summary >= 10 chars` and a reason exists ⇒ **score 1.0**, clearing the mandatory L2 gate. Reverse direction `:288-300`: expected `escalate`, actual `resolve` ⇒ requires cited `source_ids` for 1.0, else 0.5 ⇒ gate fails. **Escalating costs 10 characters; resolving costs real retrieval** |
| B4 | **`escalation_timing` rewards escalating earlier.** | `outcome_checks.py:436-439`: `esc_turn <= max_turns//2` ⇒ 1.0, decaying after. A turn-0 handover scores best |
| B5 | **The one L3 dimension that could catch it grades it as equivalent, and is disabled everywhere it matters.** | `llm_judge.py:346`: "5 = … fully resolved **or properly escalated**". Not configured on any of the 19 bad_cases |
| B6 | **Sharpest single point: `cs012` fails L1 when it behaves correctly.** | Global critical regex `hard_checks.py:255` `\b(please\s+)?call\s+me\b` fires on that case's seed message #2 and demands escalation within one turn; the case's own `closure_criterion:27-30` demands policy explanation first. Correct behaviour ⇒ L1 FAIL; wrong behaviour ⇒ L1 pass |

### 1.3 Loop C — the programmatic verdict is structurally always 0

All 19 bad_cases set `llm_judge_dimensions: []` ⇒ `judge_score = 0` ⇒
`composite = 0.5 * outcome ≤ 0.5`, against a PASS threshold of `>= 0.7`
(`executor.py:413`). Masked today by `case_passed_authority: "human_review"`,
but **any reader treating composite as a trend is misled — including the
autoloop, which has been optimizing exactly this number.**

**Confirmed live on 2026-07-25, and it is worse than "bad_cases only."** The
WS-5 validation ran 15 real sessions over five `promotion/` and `anchor/`
specs — i.e. **programmatic-authority** specs, not human-review ones — and
every one scored `composite = 0.000` in all three arms including the
pre-change baseline. The sharpest instance is
`promotion/cs_interactive_185` (draw 3, run `20260725-124324`):
`correct_uc = 1.0`, `correct_outcome = 1.0`, `containment_outcome = resolved`,
zero L1 failures — and still `FAIL`, because its three L3 dimensions are all
`advisory`, so `judge_score = 0` and the composite cannot exceed 0.5. A case
where the bot did the right thing cannot pass. The pass rate on these specs
is not measuring the agent at all.

> **Correction (Sprint 105, 2026-07-26).** This paragraph previously named
> `cs_interactive_179` as the sharpest instance. Re-scoring all eight
> recorded runs shows `cs_interactive_179` **never** reaches that profile in
> any of its 12 recorded draws — its best is
> `correct_uc = 1.0, correct_outcome = 0.0, escalated`. The session with the
> profile described here is `cs_interactive_185` @ `20260725-124324`, which
> is also the single case in the whole recorded substrate with
> `case_passed = True`, and therefore the only one whose verdict the Sprint
> 105 fix moves (0.5000 → 0.8667, FAIL → PASS). The Sprint 105 contract
> inherited the same swapped id. See `docs/sprints/sprint-105-handoff.md` §3.
>
> **Scope of the defect (same re-measurement).** "Worse than bad_cases only"
> understates it: **0 of 486 specs configure either gating L3 dimension**
> (`premature_finish` / `stall_quality`) — 430 declare only the three
> advisory dims and 56 declare none — so `composite >= 0.7` was unreachable
> for the *entire* corpus, not just the `promotion/` and `anchor/` specs.
> `passed_cases` and `task_success_rate` were pinned at zero corpus-wide.

A second, independent degradation was found the same day: because the judge
had been sharing the simulator's config, pointing it at a model that rejects
an explicit `temperature` made **every L3 dimension 400 twice and fall back
to `_DEFAULT_SCORE = 3.0`** — the whole L3 layer collapsing to a constant
behind a single log line (`scoring/llm_judge.py`). Fixed in WS-5, but it
shows the L3 layer can silently become a constant without any gate noticing.

### 1.4 Simulator — the suspicion needs redirecting

- **"The persona is never satisfiable" is not supported.** The satisfiable
  companion case reports `satisfied` 11/11; M-Auto-11 recorded 44/44 draws
  ending `goal_achieved`. The team has in fact been treating the simulator's
  *early* satisfaction as a bug ("Gate E preempt").
- **The real, undocumented defect:** the field that terminates the session,
  `goal_status` (`achieved`/`impossible`), **has no rubric anywhere in the
  system prompt**, while `user_state` — which has a careful 12-line rubric —
  **has no terminating power**. `user_simulator.py:21-71` vs
  `session_runner.py:259-280`. *The best-specified signal cannot end the
  session; the session-ending signal is unspecified.*
- **Intent switching is not being tested at all.** `persona.drift_behavior` is
  non-`none` in **344 of 486 specs** but is **declared without ever being
  explained to the model**. Precisely: the field itself never reaches the
  prompt, and the only thing that does is a bare token —
  `extractor.py:1144` appends the literal string `Drift: hard_shift.` to
  `user_goal_summary`, which *is* rendered. The WS-5 baseline arm proves that
  token did nothing: 0 intent shifts across 10 signals. `seed_messages[1:]`
  is genuinely never injected, and should stay that way — those are reactive
  turns from the historical agent transcript and are non-sequiturs without
  the agent turns they answered.
- **Drift coverage is stratified in a way that matters for test selection.**
  `exploration/` is 106/107 `hard_shift` but all 106 are intake UCs stamped
  `escalate`; `anchor/` is 120 `soft_shift` and **zero** `hard_shift`;
  `smoke/` and `bad_cases/` are zero `hard_shift` by construction
  (`smoke_curator._is_safe_for_smoke` excludes drift). So resolve-class
  intent-switching coverage lands almost entirely on `promotion/`.
- Simulator and L3 judge share one model and one provider
  (`llm_judge.py:84-85`); error correlation is not isolated.

### 1.5 Architecture — the ceiling is the tool surface, and the freedom is misplaced

- **`GumtreeApiService` exposes exactly four methods, all getters**
  (`getAccountByEmail`, `getListingByAdId`, `getModerationReview`,
  `getMessageModerationHistory`); `SalesforceService` has only `createCase` +
  `requestHandover`. **No tool can restore an ad, refund, unlock an account, or
  reset a password.** Dismantling the phase machine adds zero capability.
- **Two of six phases are unreachable**: `SessionManager.java:321-350` returns
  canned text for `phase ∈ {CLOSE, ESCALATE}` before `ControlKernel` runs, so
  `escalate.yaml` / `terminal.yaml` never execute — already the cause of 119
  mis-scored eval cases (`sprint-049-handoff.md:149`).
- **Four phases have ≤2 available tools with the order fixed in prose.** Only
  RESOLVE-FAQ retains real planning latitude, and measurement shows **44/44
  draws used one search per turn** against a 6-step budget.
- **The loop's latitude has never been used for planning — only for spinning**,
  which is why three deterministic backstops exist
  (`AgentRunLoopImpl.java:613-768`); the code comments concede four times that
  soft signals were empirically falsified.
- **The one decision that genuinely needs an LLM — intent switching — is done
  by ~5 regexes and 24 keywords** (`DriftDetector.java:63-90`,
  `RuntimeIntentClassifier.java`), and **the LLM has no tool to change the UC
  outside DISCOVER** (`classify_use_case` appears only in
  `discover_triage.yaml:9`); `CONFIRM→DISCOVER` is a dead edge.
- This is precisely the **V1.1 "hybrid orchestration / lightweight procedures"**
  already registered as deferred in `phase0_normative_freeze.md:82`. Changing
  the phase machine is a normative-freeze change and needs a `§0.6` deviation
  entry.

## 2. Product decisions (taken 2026-07-25)

**D1 — Read-only user data status query is promoted into current scope.**
Previously `PRD_biz_part.md:228` Phase 1.x. UC-A (and equivalent read-only
status paths) must consult entity context and ground the answer in the user's
own data before answering.

**D2 — "Customer indicates frustration ⇒ must escalate" is revised to
"de-escalate and keep solving first."** When the user expresses frustration but
the underlying ask is still explanation- or query-class, acknowledge and
continue solving; escalate only on an explicit human request or a second
failure. High-risk topics (fraud, safety, legal) keep immediate escalation.
Requires a `phase0_normative_freeze.md §0.6` deviation entry and a BRD
write-back.

**D3 — Eval verdict moves to the D1/D2/D3 containment ladder.** Business
success is already defined as a three-tier ladder in `PRD_biz_part.md:66-67`
(**D2 = "first resolution / material deflection: the user gets the correct next
step — status explanation, link, form" at 45–55%**), but eval carries only the
flat `resolved / escalated / abandoned`
(`eval_interactive/eval_interactive/trace/models.py:31`) and grades pass/fail
on a binary resolve-vs-escalate comparison. Consequently *"correct handover
with full context"* (a business success) and *"careless premature handover"* (a
real failure) **score identically today**.

## 3. Workstreams

### WS-1 — Stop gating on a contaminated target (P0, prerequisite for everything)

Until this lands, **no pass-rate number is trustworthy**.

1. Fix the `correct_outcome` asymmetry (`outcome_checks.py:268-300`): the
   expected-resolve-but-escalated branch must require evidence that resolution
   was *attempted* (a retrieval/citation turn), matching the rigor of the
   reverse branch — not a 10-character string.
2. Add an L1 hard check `no_premature_escalation`: when
   `should_escalate: false`, a handover with no prior `search_knowledge` /
   `resolve_article` attempt is a FAIL. This fills the hole B2 leaves open.
3. Fix `escalation_timing` (`outcome_checks.py:436-439`): score highest for
   escalating *after* a genuine attempt, not earliest.
4. Validate `grounding_mode` (`schema.py:174` / `loader.py`) and fold the six
   listing-grounded values into the legal enum so `hard_checks.py:1036`
   actually runs.
5. Resolve the `cs012` contradiction: the global `user_requested_escalation`
   regex (`hard_checks.py:996-1025`) must distinguish "asking for an
   explanation or a callback" from "demanding a human", consistent with D2.
6. Wire the linter into the CLI and remove the R1 carve-out
   (`linter.py:221-233`) so the 158 contradictory specs become visible.

### WS-2 — Rewrite CaseSpec expectations against the product principle (P0)

**Expectation is wrong — flip to `resolve`:**
`cs011_uc_c_faq_miss_not_distress` (its own `hidden_facts` say the mail is in
the spam folder — textbook self-serve — yet `acceptable_outcomes: [escalate]`
single-valued), `cs001_uc_c_mechanical_template_escalate` (decouple the
quality complaint from the outcome), `cs_uc_a_lookup_failed` (its
`closure_criterion:51-52` literally prefers escalation over a generic answer,
while the user is holding the correct ad id).

**Signal diluted to zero — tighten:** `wmkb`, `cs095`, `iwzx` all carry
`acceptable_outcomes: [resolve, escalate]`, so both answers score 1.0. Convert
to the two-stage `conditional_outcome_acceptance` + `require_closure_precondition`
mechanism: require the explanation/query half to be answered, then permit
handover for the data-modification half.

**Structurally unwinnable — restructure:** `cs_uc_a_loaded_listing` (the goal
"make my live ad perform better" is unsatisfiable in chat). Either make the
persona satisfiable as its `_resolvable` companion does, or explicitly demote
it to "grounding-only, outcome not scored".

**Bulk:** the 158 contradictory specs need per-UC review. Start with
`anchor` (74) + `smoke` (6) — they are **programmatic gates**
(`executor.py:611`), so they steer optimization harder than bad_cases do.
Representative: `anchor/cs_interactive_193.yaml` ("My ad is live but I can't
find it on my home page" — a pure UC-A status query stamped `escalate`).

### WS-3 — Land the three product decisions (P0)

1. **D1**: add a mandatory critical step keyed on `get_customer_context` for
   UC-A in `resolve_faq_grounded_answer.yaml`, mirroring the existing UC-FP
   `consult-moderation-context-on-removal-explanation` (UC-A has no equivalent
   today, `:52-143`).
2. **A3/UC-K**: change UC-K in `use-case-registry.yaml` from `path: INTAKE` /
   `false` to the documented `partial` so it can reach `resolveFaq`; reconcile
   the three divergent hardcoded sets.
3. **D2**: revise frustration handling in `risk-keywords.yaml` and the
   associated prompt; register the `§0.6` deviation and write back to the BRD.

### WS-4 — Implement the D1/D2/D3 ladder in eval (P1)

Extend `containment_outcome` beyond the flat three values so that
"explained, then handed over with context" and "handed over at turn 0" are
scored differently, and so a human spot-check verdict is reproducible by the
harness.

### WS-5 — Fix the simulator's real defects (P1, cheap, high leverage)

1. Write a rubric for `goal_status` (`user_simulator.py:21-71`) to the same
   standard as the existing `user_state` rubric. This is the most plausible
   channel for "pressured into escalation" and the cheapest fix in the plan.
2. Inject `persona.drift_behavior` — 344/486 specs declare it and it is
   currently dead, meaning **intent switching has never been evaluated**.
3. Separate the simulator model/provider from the judge (`llm_judge.py:84-85`).

### WS-6 — Move the latitude to where it belongs (P2; needs WS-5 first to verify)

- **Contract**: demote INTAKE and CONFIRM to deterministic workflow plus a
  single structured LLM call (extraction / classification); cut RESOLVE-FAQ's
  budget from 6 to 3 (measured max 2); delete the unreachable `escalate.yaml`
  and `terminal.yaml` and the eval mis-scoring they cause.
- **Expand**: give the RESOLVE/CONFIRM skills `classify_use_case` (or a new
  `propose_reroute` tool) and restore the `CONFIRM→DISCOVER` edge, so the LLM
  actually owns drift — the exact surface where
  `failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md` ruled that
  "add another regex" is the wrong fix.
- **Framing**: the question is not "autonomous or not". It is that latitude was
  granted in five fixed-procedure phases (then fenced back in with
  deterministic backstops) and withheld at the single point that needs
  judgement. Target shape = deterministic workflow skeleton + two bounded loops
  (RESOLVE-FAQ retrieval chain; intent-switch decision) = the registered V1.1
  hybrid orchestration.

### WS-7 — Layer the scale-up evaluation (P2)

Answering "should scale testing keep using an LLM simulator": **layer it, don't
choose.**
- **Small-batch human spot-check** stays the primary gate (bad_cases already
  run as `human_review`).
- **Mid-batch semantic regression** uses the LLM simulator — but only after
  WS-5, or it cannot see intent switching and its termination signal is
  unspecified.
- **Large-batch deterministic regression** revives the `eval/` Java replay
  harness: **601 recorded real sessions, zero LLM calls on the user side**
  (`make eval-full`). Caveats: unchanged since 2026-04-27 (~2 months stale) and
  `data/human_review_annotations_2026-04-22_golden.csv` is missing, so human
  ground truth is lost. **Reviving it is far cheaper than writing a new
  simulator**, and it is structurally immune to simulator bias.
- Real data is plentiful: `data/eval_datasets/` (7 prepared datasets),
  `data/filtered/` (6,835 filtered real sessions), and
  `docs/case-data-stat.md` (120,367 cases) for volume weighting.

## 4. Sequencing

1. **WS-1 + WS-3** in parallel — make the ruler accurate and land the decisions.
2. **WS-2** — rewrite expectations against the corrected ruler; `anchor`/`smoke`
   before `bad_cases`.
3. **WS-5** — fix the simulator; intent switching becomes measurable for the
   first time.
4. **WS-4** — three-tier verdict.
5. **WS-6** — architecture, verified by WS-5's drift coverage.
6. **WS-7** — layered scale-up; revive the Java replay harness.

**Autoloop**: do not resume iterations before WS-1 and WS-2 land. Its objective
function is the contaminated one, and the composite it reads is structurally 0
on the bad-case suite. Afterwards, reuse its comparison/gating infrastructure
as the regression harness for WS-1..WS-6 rather than as a discovery engine.

## 5. What not to do

- Do not fix Loop A or Loop B with keyword/regex/enum rules — both are soft
  semantic decisions owned by the LLM (Constitution §1.5, §1.7).
- Do not widen a CaseSpec to accept genuinely wrong bot behaviour (§5.4). The
  WS-2 rewrites go the other way: they *remove* specs that accept
  over-escalation.
- Do not trust any pass-rate, composite, or autoloop verdict measured before
  WS-1 lands.
- Do not dismantle the phase machine to "give the agent more room": the ceiling
  is the tool surface (§1.5), and the phase machine is a normative freeze
  requiring a `§0.6` deviation entry.
