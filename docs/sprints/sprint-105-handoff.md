---
title: Sprint 105 handoff — eval verdict: Loop C, L3 collapse, D1/D2/D3 ladder
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: eval_interactive/eval_interactive/scoring/
last_reviewed: 2026-07-26
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 105 (WS-4 + Loop C). Branch sprint-105-eval-verdict, worktree
  ../csagent-wt-105, based on perf-replan-2026-07 @ 82f8a137. Scoring-layer
  only; no server/** change. Evidence is deterministic re-scoring of eight
  already-recorded runs plus a corpus-wide structural sweep.
---

# Sprint 105 handoff

Branch `sprint-105-eval-verdict`; commits `291b1023`, `a6d43fae`,
`2220ed54`, `d48057d6`.

**Base-commit discrepancy.** The contract §0.1 states the tip of
`perf-replan-2026-07` is `d7d84f86` and that `git log --oneline -1` "must
read d7d84f86". It actually reads **`82f8a137`** ("docs: four parallel
sub-sprint contracts…") — the commit that introduced the contract itself,
a descendant of `d7d84f86`. Sprint 104's worktree is on the same commit. I
branched from the real tip.

---

## 1. Item 1 feasibility finding — what `results.json` does and does not carry

**Verdict: feasible for the layer that has the defect; not feasible for the
layers below it.** No fallback to fixtures was needed, and no live run was
requested.

### Carried, and sufficient to replay `compute_composite` exactly

`l1_results[]`, `l2_results[]`, `l3_results[]`, `tier2_result` (including
`per_step[]`), `stall_detected` / `stall_failure_tag`, `case_passed`,
`composite_score`, `outcome_score`, `judge_score`, `failure_tags`,
`containment_outcome`, `active_use_case`, `escalation_reason`,
`total_turns`, `stop_reason`, `case_passed_authority`, `user_state_signals`,
`transcript[]` (`role` / `message` / `turn_index` / `source`), `llm_calls[]`,
and `per_turn_trace[]`.

These are precisely `compute_composite`'s inputs, which is why the Loop C
defect — which lives in that function — is re-scorable offline.

### NOT carried (recorded in code as `rescore.REHYDRATION_GAPS`)

| Owner | Missing field | Consequence |
|---|---|---|
| `TraceData` | `events` | Absent entirely. Any L1 check reading session events cannot be recomputed. |
| `TraceData` | `handover` (`log_id`, `handover_payload`, `transfer_result`) | Absent entirely. L2 `handover_completeness` cannot be recomputed; its recorded score is reused. |
| `SessionState` | `candidate_use_cases`, `clarification_count`, `faq_miss_count`, `form_context`, `customer_context`, `articles_shown`, `current_phase`, `total_bot_turns` | 8 of 12 fields dropped; only `containment_outcome` / `active_use_case` / `escalation_reason` survive. |
| `TurnTrace` | `turn_index`, `user_message`, `bot_response`, `source_ids`, `phase_before`, `phase_after`, `active_use_case`, `latency_ms` | `_build_per_turn_trace` keeps only `tool_calls` / `phase_plan` / `projection` — **8 of 10 fields dropped**. Bot text is recoverable from `transcript[]`; the phase pair and `source_ids` are not. |
| `HardCheckResult` / `OutcomeCheckResult` | `severity` | Dropped by the pre-Sprint-105 serialiser. **This one is load-bearing** — `composite.py` filters advisory results out of both the L1 gate and the outcome mean. |

The severity gap is not recoverable from the check name: `no_forbidden_tools`,
`user_requested_escalation` and `escalation_reason_family_match` all choose
severity at runtime. **Sprint 105 now persists it** (`d48057d6`), so runs
recorded from here on are fully re-scorable.

### Why the gap did not invalidate this sprint's evidence

Rather than assume the severity default was harmless, `rescore.py` proves it
per-case. `legacy_verdict()` is a deliberately frozen, standalone copy of the
pre-Sprint-105 arithmetic that **does not import `composite.py`** — an oracle
that shares the implementation it verifies cannot detect a change in it. Every
case is checked against it before being quoted.

**Result: 17 of 17 cases across all 8 recorded runs reproduce their recorded
`outcome_score`, `judge_score`, `composite` and `case_passed` exactly.** The
severity gap is empirically inert on this substrate.

---

## 2. The Loop C diagnosis, and why the alternative was rejected

### What the contract offered

The contract §3.2 asked me to choose between (a) `judge_score = 0.0` on no
gating L3 being the defect, and (b) the `advisory` classification of the three
dims being correct but the composite's treatment of "no gating dims" being
wrong.

**These are the same claim**, and both are (a). I settled on (a). But the
naive form of (a) is wrong, and I rejected it — see below.

### The measurement that decided it

A corpus-wide sweep of all 486 loadable specs
(`scratchpad/sweep_ceiling.py`, reproduced from
`case_spec.loader.load_case_spec` + `LlmJudge.ALL_DIMENSIONS`):

```
judge-dim configuration across the corpus:
   advisory dims only       430
   no L3 dims at all         56
specs configuring premature_finish or stall_quality:  0
```

**Zero of 486 specs configure either gating dimension.** Therefore
`judge_score` was structurally `0.0` and `composite = 0.5 * outcome + 0.5 * 0
≤ 0.5` for **every case in the corpus** — against a 0.7 pass bar.

```
structurally CANNOT reach 0.7 (old rules): 486 / 486
structurally CANNOT reach 0.7 (new rules):  45 / 486
```

**This is materially worse than the contract and the re-plan state.** Both
describe the problem as bad_cases plus the `promotion/` and `anchor/` specs.
It is the entire corpus: `anchor` 159, `exploration` 107, `promotion` 101,
`case_families` 49, `probe` 25, `bad_cases` 19, `smoke` 14, `anchor_outcome`
12. `passed_cases`, `task_success_rate` and `mean_composite_score` have been
pinned at zero corpus-wide, not merely depressed. The autoloop was optimising
a constant.

The 45 that still cannot reach 0.7 are exactly the specs with **no L2
`outcome_checks` and no L3 dims** — `anchor_outcome` 12, `bad_cases` 8,
`probe/option_beta_coverage` 25. These configure no programmatic scoring
signal at all, and 20 of them are `human_review` authority by design. A spec
that declines to be scored should not be given a programmatic pass; this is
correct, not residual.

### The fix, and the alternative I rejected

The naive form of (a) is *renormalise the composite onto the outcome term*
whenever no gating dim exists. **I implemented that first, measured it, and
backed it out.**

On the one recorded session whose verdict this sprint moves
(`cs_interactive_185` @ `20260725-124324`), naive renormalisation scored a
flat **1.0**. But the advisory judge had recorded `groundedness = 2.0` with
this reasoning:

> "In Turn 2, the bot properly grounded its password-reset instructions with a
> canonical URL. However, in Turn 3 the bot made multiple definitive factual
> claims about account recovery (e.g., Gumtree cannot manually verify accounts,
> the login email cannot be changed, a new account is required) but only
> referenced a vague 'help article' without surfacing a URL or human-readable
> title to the user."

That is a real, specific, correct quality deduction — precisely the
ID-alone-citation failure pattern the S-Eval-5 groundedness rubric was
tightened to catch. **Renormalising it away throws a real measurement on the
floor and calls the result "unmeasured".** That is less discriminating, which
is the wrong direction under §5.4.

**The shipped rule: advisory dims are a fallback signal, never a dilutant.**

| L3 state | `judge_basis` | composite |
|---|---|---|
| ≥1 gating dim measured | `gating` | `0.5*outcome + 0.5*judge` — **unchanged**, advisory dims excluded exactly as S-Eval-5 specified |
| no gating dim, ≥1 advisory dim measured | `advisory_fallback` | `0.5*outcome + 0.5*judge(advisory)` |
| no usable L3 at all | `none` | `outcome` (renormalised), or `0.0` with no gating L2 |

S-Eval-5's intent is preserved exactly where it applies: advisory dims never
dilute a mean that already contains a gating dim. S-Eval-5 simply did not
contemplate the case where they are the *only* signal — which is the whole
corpus.

Under this rule the moving session scores **0.8667, not 1.0**. The deduction
survives.

### What was NOT done

- **The 0.7 bar was not lowered.** Pinned by
  `test_the_pass_threshold_was_not_lowered`.
- **No dimension was promoted to gating.** Pinned by
  `test_the_two_gating_l3_dims_are_the_only_non_advisory_ones`.
- **No `bad_cases` `llm_judge_dimensions` field was edited**, though the
  contract permitted it. The 11 bad_cases that carry L2 checks now reach a
  ceiling of 1.0 through renormalisation without any spec edit, and their
  authority is `human_review` regardless — editing them would have changed
  the corpus under Sprint 106 for no gain.
- **Renormalisation is one-directional.** It never applies to a missing
  *outcome* term. With no gating L2 the ceiling is 0.5 even with a perfect
  judge, so a pass still requires L2 evidence and OQ-S77 #3 is intact.
  Pinned by `test_renormalisation_never_applies_to_a_missing_outcome_term`
  and `test_oq_s77_no_evidence_gate_survives_renormalisation`.

---

## 3. Per-session before/after, with transcript justification

All 8 recorded runs, 17 cases. Fidelity 17/17.

| Run | Case | was | now | composite | tier | direction |
|---|---|---|---|---|---|---|
| ws5-draw1 | cs_interactive_179 | FAIL | FAIL | 0.0000 → 0.0000 | D2 | unchanged |
| ws5-draw1 | cs_interactive_185 | FAIL | FAIL | 0.0000 → 0.0000 | D2 | unchanged |
| ws5-draw2 | cs_interactive_179 | FAIL | FAIL | 0.0000 → 0.0000 | D2 | unchanged |
| ws5-draw2 | cs_interactive_185 | FAIL | FAIL | 0.0000 → 0.0000 | D3 | unchanged |
| ws5-draw3 | cs_interactive_179 | FAIL | FAIL | 0.0000 → 0.0000 | UNKNOWN | unchanged |
| **ws5-draw3** | **cs_interactive_185** | **FAIL** | **PASS** | **0.5000 → 0.8667** | **D1** | **looser (+0.3667)** |
| s103-draw1 | cs_interactive_179 | FAIL | FAIL | 0.0000 → 0.0000 | D2 | unchanged |
| s103-draw1 | cs_interactive_185 | FAIL | FAIL | 0.0000 → 0.0000 | D3 | unchanged |
| s103-draw2 | cs_interactive_179 | FAIL | FAIL | 0.0000 → 0.0000 | D2 | unchanged |
| s103-draw2 | cs_interactive_185 | FAIL | FAIL | 0.0000 → 0.0000 | D2 | unchanged ⚠ |
| s103-draw3 | cs_interactive_179 | FAIL | FAIL | 0.0000 → 0.0000 | D2 | unchanged |
| s103-draw3 | cs_interactive_185 | FAIL | FAIL | 0.0000 → 0.0000 | D3 | unchanged |
| s103-negctl | cs_interactive_030 | FAIL | FAIL | 0.0000 → 0.0000 | D2 | unchanged |
| s103-negctl | cs_interactive_155 | FAIL | FAIL | 0.0000 → 0.0000 | D3 | unchanged |
| s103-neighbors | cs_interactive_170 | FAIL | FAIL | 0.0000 → 0.0000 | UNKNOWN | unchanged |
| s103-neighbors | cs_interactive_263 | FAIL | FAIL | 0.0000 → 0.0000 | D3 | unchanged |
| defect1-verify | cs_interactive_208 | FAIL | FAIL | 0.0000 → 0.0000 | D3 | unchanged |

⚠ = `20260725-165354` carries **1 L3 fallback call** (`relevance`,
`parse_failure`) and is flagged **NOT QUOTABLE AS EVIDENCE** by the item-3
diagnostic. It is listed for completeness; nothing is claimed from it.

### The one rise — `cs_interactive_185` @ `20260725-124324` (§7.2)

Exactly one verdict moved in the entire substrate, because it is the only
recorded case with `case_passed = True` (every other case fails a hard gate,
which zeroes the composite before any of this arithmetic runs).

Harness facts: **zero L1 failures** (all 15 checks pass, including
`no_premature_escalation` and `source_citation_present`), `correct_uc = 1.0`,
`correct_outcome = 1.0` (`actual=resolve in acceptable=['resolve']`), Tier-2
`passed=True severity=critical`, `containment_outcome = resolved`,
`stop_reason = goal_impossible`.

Transcript justification:

- **Turn 2** — user reveals they are locked out and cannot edit two live ads.
  The bot retrieves and delivers a concrete, grounded next step with a
  clickable canonical URL:
  > "you can reset your password by clicking 'Login/Register' on the Gumtree
  > homepage, then 'Forgot your password?' … You'll receive a reset email —
  > check your spam folder if it doesn't arrive. Here's the article for
  > reference: `https://help.gumtree.com/s/technical-issues?cat=Account_Issues&article=Changing-Your-Password`"

  Tool trace confirms it: `search_knowledge` → `resolve_article` on that turn.

- **Turn 3** — user asks the bot to manually verify the account or resend to
  a different address. The bot correctly refuses, states the policy limit
  honestly, and offers a human:
  > "Unfortunately, Gumtree cannot manually verify accounts or resend password
  > reset links to a different email. … the login email cannot be changed …
  > I'm sorry I can't do more here — would you like me to transfer you to a
  > human agent who may be able to help further?"

  This is correct: per the re-plan §1.5, `GumtreeApiService` exposes four
  getters and `SalesforceService` only `createCase` + `requestHandover`. **No
  tool can unlock an account.** The ask is genuinely outside the bot's
  capability, and the bot said so instead of stalling or inventing.

**Why the rise is defensible:** the bot retrieved, cited a real URL, gave an
actionable next step, did not over-escalate, and escalated only when the
remaining ask was provably impossible for it. That is a session I can defend
on reading it.

**Why it is 0.8667 and not 1.0:** the advisory `groundedness = 2.0` deduction
quoted in §2 is carried into the score. The turn-3 claims were correct policy
but cited only as "our help article" with no URL — a real citation-quality
defect, and it costs the case 0.133. I consider that the right size for the
flaw.

**Residual concern I am recording rather than hiding:** the `resolved`
containment stamp is generous. The user's actual goal — regain account access
— was *not* achieved, and the session ends with them asking "Can I speak to
someone?". Calling that `resolved` is a **runtime stamping** question, not a
scoring one, and it is out of scope here (`server/**` is Sprint 104's). It is
listed in §7.

### Negative controls — neither rose (§7.3)

Both were chosen by reading transcripts, not by taking the run labelled
`negctl`.

**NC-1 `cs_interactive_155` @ `20260725-165835`.** Tool trace across all
turns: `[[], [], ['request_handover']]` — **zero retrieval calls, ever.** At
turn 2 the bot asked the user to confirm their email; the user had already
given it at turn 1, gave it again at turn 3 with "I've checked spam and
promotions folders — nothing", and the bot replied:
> "I've reached the limit of what I can assist with on this topic. Let me
> connect you with a human agent who can help further."

L1 fails: `no_premature_escalation`, `source_citation_present`.
**0.0000 → 0.0000. Tier D3.**

**NC-2 `cs_interactive_185` @ `20260725-165045`.** Tool trace:
`[[], [], ['request_handover']]` — again zero retrieval. Sharper than NC-1:
the bot *solicited* the information and then discarded it. Turn 2:
> "To edit the contact number on one of your live ads, I'll need the ad ID or
> the title of the ad you want to update. Could you please provide that?"

Turn 3, user: *"The ad is 'Samsung Galaxy S23 Ultra - Unlocked - Excellent
Condition'."* Bot: *"I've reached the limit of what I can assist with…"*

L1 fail: `no_premature_escalation` ("handed over at turn 3 with no
search_"). **0.0000 → 0.0000. Tier D3.**

Both are genuinely wrong sessions and both remain FAIL, gated at L1 before
any composite arithmetic runs. Additionally pinned synthetically by
`test_negative_control_premature_handover_does_not_start_passing` and
`test_negative_control_wrong_uc_does_not_start_passing`.

### L3 fallback diagnostic per run (§7.4)

| Run | fallback / total | quotable |
|---|---|---|
| ws5-draw1 | 0/6 | yes |
| ws5-draw2 | 0/6 | yes |
| ws5-draw3 | 0/3 | yes ← the one rise |
| s103-draw1 | 0/6 | yes |
| s103-draw2 | **1/6** | **no** |
| s103-draw3 | 0/6 | yes |
| s103-negctl | 0/6 | yes |
| s103-neighbors | 0/3 | yes |
| defect1-verify-208 | 0/3 | yes |

Every run quoted above as evidence reports **0** fallbacks. The single
degraded dimension was found by the new diagnostic, not known in advance.

---

## 4. The ladder definition

`eval_interactive/eval_interactive/scoring/containment_ladder.py`. First
match wins.

| Tier | Trace facts that produce it |
|---|---|
| **UNKNOWN** | No per-turn trace recorded, **or** no `containment_outcome` stamped. An unmeasured session is never charged to the bot as a failure. |
| **D1** | `containment_outcome == resolved` **and** no `request_handover` **and** a grounded artifact surfaced. Fully automated closure. |
| **D2** | Retrieval attempted **and** a grounded artifact surfaced **and** — if handed over — `handover_completeness == 1.0`. Also: `resolved` with no grounded artifact (deflection claimed without retrieval evidence). |
| **D3** | Everything else: handover with no retrieval attempt; retrieval that surfaced nothing; grounded work whose handover payload was incomplete; abandoned. |

Input facts, all already recorded:

| Fact | Source |
|---|---|
| `attempted_retrieval` | any `search_knowledge` / `resolve_article` in `per_turn_trace[].tool_calls[].tool_name` |
| `grounded_artifact` | `search_knowledge` with ≥1 hit and no `faq_miss`/`retrieval_miss`, **or** `resolve_article` success carrying `source_url`/`canonical_url` |
| `citation_check_passed` | L1 `source_citation_present` |
| `handover_requested` / `handover_turn` | first `request_handover` tool call and its turn index |
| `handover_payload_complete` | L2 `handover_completeness >= 1.0` |
| `containment_outcome`, `total_turns` | as recorded |

**Anti-hardcode:** the ladder never receives the transcript. Every input is a
tool-call or structured tool-result fact. That is a structural guarantee, not
a convention — pinned by
`test_tier_is_derived_from_trace_facts_not_bot_wording`.

**Reproducibility:** live and offline paths funnel through one extractor
(`extract_facts_from_turn_dicts`) over the identical `tool_calls` payload, so
a tier computed during a run and one recomputed from that run's
`results.json` agree by construction — which is what lets a human spot-check
verdict and the harness verdict disagree *visibly*.

**Migration:** additive. `containment_outcome` keeps its three values and its
meaning; no recorded baseline is re-mapped. The new `containment_tier` /
`containment_tier_detail` case fields and the run-level
`containment_tier_breakdown` are absent from older runs and default to
`UNKNOWN` when re-scored, which is honest — those runs were not tiered.

---

## 5. Test numbers

### `eval_interactive` pytest

| | failed | passed | skipped |
|---|---|---|---|
| Baseline (before first edit) | **15** | 763 | 5 |
| After | **14** | 786 | 5 |
| After, with `data/eval_datasets` symlinked in (2026-07-26) | **13** | 787 | 5 |

**The contract's stated baseline (`764 passed, 14 failed, 5 skipped`) was
wrong for this worktree** — the worktree is missing a data tree the primary
checkout has. Measured attribution, via `pytest -q --tb=line`:

| count | cause | owner |
|---|---|---|
| 13 | missing `data/human_review_annotations_2026-04-22_golden.csv` | `data/**` (Sprint 107) — absent from the primary checkout too |
| 1 | missing `data/eval_datasets/badcase_turns.csv` | **worktree-only**; `.gitignore:8 data/*` means `git worktree add` does not carry `data/` |
| 1 | stale Skill-count assertion | **mine — item 5, fixed** |

**Correction, 2026-07-26 — the attribution above is the second measurement,
and it reverses what I first wrote.** I originally reported 12 from the golden
CSV and 2 from `badcase_turns.csv`, and criticised the contract's "13 of the 14
come from the golden CSV" on that basis (§8 item 3). **The contract's split was
right and mine was wrong.** After symlinking
`data/eval_datasets → /Users/caoruixin/projects/csagent/data/eval_datasets` the
suite reports `13 failed, 787 passed, 5 skipped`, and all 13 survivors die on
the golden CSV: 8 in `tests/regression/test_case_spec_overrides.py`, 4 in
`tests/regression/test_case_spec_persona_snapshots.py`, and 1 in
`tests/test_transcript_evidence.py::test_cs_interactive_004_account_login_guidance_resolves_without_async_false_positive`
— which I had mis-bucketed under `badcase_turns.csv` because both files are
absent here and I attributed by module rather than by traceback.
`badcase_turns.csv` is worth exactly **one** failure
(`test_transcript_evidence.py::test_cs_interactive_001_like_evidence_detects_unresolved_investigation`),
and it **is present in the primary checkout** — so that failure is a worktree
artefact, not a repo-wide one. The golden CSV is genuinely absent everywhere,
so those 13 are environmental in any checkout.

Restated against a data-complete checkout: baseline **14 failed / 764 passed /
5 skipped** → after **13 / 787 / 5**. The delta below is unaffected.

Delta: −1 failure (item 5), +23 passes (22 new tests in
`test_sprint_105_loop_c_and_ladder.py`, plus the item-5 test flipping green).
**Self-introduced failures: 0.**

Two pre-existing tests asserted the defect and were flipped, with the old
assertion preserved verbatim in the docstring per §6:
`test_composite.py::TestComputeComposite::test_all_pass_no_judge` and
`test_s_eval_5_l3_repositioning.py::…::test_all_advisory_l3_yields_zero_judge_score`
(renamed `…test_all_advisory_l3_used_as_fallback_signal`).

### `server` mvn

`cd server && mvn -o test` → `Tests run: 1493, Failures: 1, Errors: 0,
Skipped: 2` **before and after — byte-identical**, as required. The one
failure is the documented inherited
`SystemPromptUserRequestedTiebreakerTest`. `git diff HEAD -- server/` is
empty.

### Corpus lint

`488 spec(s), Errors: 226, Warnings: 51` before and after — unchanged, as
expected for a sprint that edits no spec. (Sprint 106 is moving these under
me in a parallel worktree; drift there is not mine.)

---

## 6. What the new verdict still cannot see (§7.5)

**The ladder measures whether a correct next step was *delivered*, not
whether the advice was *good*.** Two answers can both cite a real, correctly
retrieved article and still differ materially in quality — the e2e findings
recorded exactly that: two different groundings for the same question, one of
them worse advice, both legitimately grounded. **My ladder places both at D2
and cannot distinguish them.** Answer quality is the L3 judge's job and no
trace-derived tier can substitute for it. This is stated in the module
docstring so the next reader does not over-claim it.

Also invisible to the tier:

- **Whether the user's goal was actually met.** D1 keys on the `resolved`
  stamp plus grounding, not on the customer's own resolution stance. The
  moving session in §3 is the live example: tiered **D1**, yet the user ends
  by asking for a human. `user_state_signals` is recorded and could feed a
  future refinement; I did not wire it, because doing so would change what
  the tier means, and the contract asked for a trace-derived tier.
- **Whether the handover payload's *contents* are correct**, only whether
  `handover_completeness` scored 1.0 — a completeness check, not an accuracy
  check.
- **Turn-level ordering quality.** A bot that retrieves at turn 5 after four
  wasted turns and one that retrieves at turn 1 both reach D2. `handover_turn`
  is recorded in the detail string but does not move the tier.
- **The `resolved` stamp's own trustworthiness.** The ladder consumes it; it
  cannot audit it.

And on the composite fix specifically: **`judge_basis == "advisory_fallback"`
means the verdict rests on dims that S-Eval-5 deliberately declined to gate
on.** That is better than resting on nothing, which was the prior state, but
it is not the same as a spec that configures `premature_finish` /
`stall_quality`. See §7 item 1.

---

## 7. Real defects found but out of contract — listed, not fixed

1. **`premature_finish`'s rubric credits every handover, so the dim cannot
   detect a careless one.** Its rubric reads *"5 = the conversation ended
   naturally after the issue was fully resolved **or properly escalated**"*,
   with no test of what "properly" requires.

   **Measured, 2026-07-26** (human-authorised follow-up; enabled the two
   critical dims as an always-on floor and re-judged all 17 recorded
   sessions live against the judge provider, 34 calls, 0 fallbacks):

   | session | `premature_finish` | what actually happened |
   |---|---|---|
   | `cs_interactive_155` @ negctl | **5.0** | zero retrieval, handed over after receiving the user's email |
   | `cs_interactive_185` @ 165045 | **5.0** | zero retrieval, handed over after receiving the ad title it asked for |
   | `cs_interactive_179` @ ws5-draw3 | **1.0** | HTTP-500 transport failure — the bot never ended anything |
   | `cs_interactive_170` @ neighbors | **1.0** | HTTP-500 transport failure |

   It scores 5.0 on exactly the sessions it exists to catch, and 1.0 on
   sessions the bot did not end. Tightening the rubric to require evidence
   of a genuine attempt fixes it (both negative controls → stable 1.0 over
   3 draws; both infra failures → 5.0; the one good session → stable 5.0;
   one genuinely gray case flips `[1,1,5]`). **That change was measured and
   then reverted, not shipped** — see §8 item 8 for why.

2. **The corpus configures neither gating L3 dim** — 0 of 486. This is a
   real observation, but the inference "therefore premature handover is
   unmeasured" is **wrong**, and I originally reported it that way. The L1
   hard check `no_premature_escalation` (landed by WS-1) already catches
   this failure mode deterministically: it is what fails on both negative
   controls, and it hard-zeroes the composite through the L1 gate. A stable
   deterministic check is a better guard here than a noisy LLM dim. The
   corpus's omission is, on this failure mode, costing nothing.

3. **`data/human_review_annotations_2026-04-22_golden.csv` is missing from
   every checkout, primary included**, costing **13** test failures that no
   sprint in this series owns. (Corrected 2026-07-26: I first wrote this item
   about `badcase_turns.csv` and put the count at 2. That file is present in
   the primary checkout and costs exactly 1 failure, which is a
   worktree-only artefact of `.gitignore:8 data/*` — see §5.) Both files are
   gitignored, so restoring them is a local-env task, not a code change; but
   13 permanently-red tests is baseline rot, and the decision of whether to
   regenerate the golden CSV or make those tests skip on its absence is
   unowned.

4. **Two of 17 recorded sessions died on a backend HTTP 500**
   (`cs_interactive_170` @ `s103-neighbors`, `cs_interactive_179` @
   `ws5-draw3`) — `BotTransportError`, empty `per_turn_trace`, blank
   containment. ~12% of the recorded substrate is unmeasured for infra
   reasons. The ladder now reports these as UNKNOWN rather than D3, but the
   underlying 500s are unexplained.

5. **`results.json` drops 8 of 10 `TurnTrace` fields and both `events` and
   `handover` entirely** (§1 table). I fixed only the `severity` gap, because
   that is the one that blocks composite re-scoring. Full L1/L2 offline
   re-scoring remains impossible; closing it means widening
   `_build_per_turn_trace` and serialising `TraceData.handover`.

6. **`contract_warnings` is absent from some case-result rows.** The
   timeout / transport-error paths build their rows without it, so a consumer
   doing `case["contract_warnings"]` raises `KeyError` (I hit this). The
   schema is documented as uniform across populated / placeholder / error
   paths; it is not.

7. **Spec loading emits five copies of
   `"Expected.escalation_trigger is None with should_escalate=true; family-match
   scoring will be treated as advisory for this case."` on every corpus load**,
   with no case id attached. Unactionable as written.

8. **The `resolved` containment stamp on `cs_interactive_185` @ ws5-draw3 is
   generous** — the user's goal was not met and they close by asking for a
   human. Runtime stamping, `server/**`, Sprint 104's path.

---

## 8. Where I think this contract is wrong

1. **It understates Loop C by two orders of scope.** The contract says the
   defect is "worse than bad_cases only" and names the `promotion/` and
   `anchor/` specs. Measured: **all 486 specs**, every bucket. The pass rate
   was not depressed — it was *pinned at zero corpus-wide*. The contract's
   own framing would have led me to fix it for a subset and report success.

2. **Its two offered diagnoses are the same diagnosis, and the fix it implies
   is wrong.** §3.2 (a) and (b) both say the composite's handling of "no
   gating dims" is the defect. More importantly, the obvious implementation
   of that — renormalise onto the outcome term — discards a real advisory
   measurement and would have scored the one moving session a flat 1.0,
   throwing away a correct `groundedness = 2.0` deduction. The contract's P0
   ("only more discriminating, never more permissive") is what caught this;
   its §3.2 framing did not.

3. ~~**Its baseline was wrong and its attribution was wrong.**~~
   **WITHDRAWN 2026-07-26 — the contract was right and I was wrong.** I
   claimed the split was "12 from the golden CSV and 2 from a second missing
   file". Re-measured by traceback rather than by module: **13 from the golden
   CSV and 1 from `badcase_turns.csv`**, exactly as the contract stated. What
   survives of the complaint is only the total: the contract's `764 passed /
   14 failed` is the *primary checkout's* number, and this worktree measures
   `763 / 15` because `.gitignore:8 data/*` keeps `git worktree add` from
   carrying `data/`. That is a worktree-provisioning gap, not a contract
   error — a contract that states the primary baseline is stating the right
   thing. Full correction in §5; §7 item 3 corrected to match.

   The lesson is the mirror of item 8's: I attributed 15 failures to two
   missing files by looking at which module they lived in, then used that
   attribution to correct a contract that had measured it properly. Attribute
   from the traceback, and re-measure before contradicting a stated baseline.

4. **Its named target session does not exist.** §1 and re-plan §1.3 both say
   "the sharpest instance: `promotion/cs_interactive_179` under drift reached
   `correct_uc = 1.0`, `correct_outcome = 1.0`, `containment_outcome =
   resolved`, zero L1 failures". Across all 12 recorded draws of that case,
   **`cs_interactive_179` never once reaches that profile** — its best is
   `correct_uc=1.0, correct_outcome=0.0, escalated`. The session with exactly
   the described profile is **`cs_interactive_185` @ `ws5-goalstatus-draw3`**.
   The case ids were swapped somewhere upstream, and the re-plan carries the
   error too. Anyone re-deriving the finding from `cs_interactive_179` will
   fail to reproduce it.

5. **The base commit in §0.1 is stale** (`d7d84f86` vs the actual
   `82f8a137`), and the instruction "must read d7d84f86" would have made a
   careful agent stop.

6. **§5's generalization-coverage line (`5 / 19 / 2 / 0`) is not achievable
   as written.** The 19 `bad_cases` are named as neighbours "all currently
   structurally 0", but **no recorded run in the substrate contains a single
   bad_case** — all 8 runs are `promotion/` and `anchor/` sessions. I
   substituted the corpus-wide structural sweep (§2), which covers all 19 plus
   the other 467, and is stronger evidence for P1 than re-scoring 19 sessions
   would have been. But the contract's stated coverage plan could not be
   executed against the substrate it points at.

7. **Minor:** §7.4 says "a run with a non-zero fallback count may not be
   quoted as evidence", while §3.3 asks only that a *wholly* collapsed run be
   unusable. I implemented both as separate flags (`evidence_clean` vs
   `verdict_usable`) because they are different bars, but the contract reads
   as though they are one.

8. **Where *I* was wrong — the always-on quality floor, built and reverted
   2026-07-26.** My own §7 originally called "turn the two gating dims on"
   the highest-value follow-up, and it was authorised on that framing. I
   built it (an always-on `_FLOOR_DIMENSIONS` in `llm_judge.judge()`, so no
   spec file was touched and Sprint 106 was not disturbed), measured it, and
   reverted it. Three findings, in the order they landed:

   - **The premise was false.** `premature_finish` scored **5.0** on both
     negative controls — the sessions it exists to catch — because its
     rubric credits any handover as a proper ending (§7 item 1). Turning the
     dim on as-written would have measured nothing and cost ~972 extra judge
     calls per full-corpus run.
   - **Fixing the rubric works but is not free.** A tightened rubric gives
     both negative controls a stable 1.0 across 3 draws and correctly stops
     mis-firing on the two HTTP-500 sessions. But one genuinely gray case
     flips `[1, 1, 5]`, so the dim is a reasonable *observation* and a poor
     *gate* — §3.2's tail rule would classify that as
     `judge_calibration`.
   - **Enabling the floor silently makes the ruler LOOSER.** Once a gating
     dim always exists, S-Eval-5's rule that advisory dims drop out of the
     mean stops being a tie-breaker and becomes a permanent deletion:
     `groundedness` / `relevance` / `tone_appropriateness` would never reach
     the composite again. Measured on the one moving session, that discarded
     the real `groundedness = 2.0` deduction and took it **0.8667 →
     0.9500**. Repairing *that* means reversing S-Eval-5's exclusion rule
     outright, which breaks eight existing tests that encode it.

   Three coupled semantic reversals, to enable a dim whose failure mode is
   **already caught deterministically at L1** by `no_premature_escalation`,
   is a bad trade. Reverted; the findings are recorded above instead.

   The general lesson for this contract series: "the corpus does not
   configure X" is an observation about configuration, not evidence that X
   is unmeasured. I asserted the latter from the former without checking
   whether another layer already covered it. It did.

---

## 9. Self-check

- [x] Worktree is `../csagent-wt-105` on `sprint-105-eval-verdict`; nothing
      written outside owned paths; `git diff HEAD -- server/` is **empty**.
- [x] Both baselines recorded before the first edit (pytest 15/763/5, mvn
      1493/1/0/2); self-introduced failures **0**; `mvn -o test` byte-identical
      after.
- [x] Backend never restarted; no bot session launched; no `make ingest`; no
      Postgres touched. All evidence is offline re-scoring plus static corpus
      analysis.
- [x] The one score that rose has quoted transcript justification (§3), and
      the advisory deduction against it is carried into the score rather than
      discarded.
- [x] Two negative controls read and confirmed genuinely wrong; both remain
      0.0000 (§3).
- [x] L3 fallback diagnostic reports **0** for every run quoted as evidence;
      the one run with 1 fallback is flagged and nothing is claimed from it.
- [x] The 0.7 bar was not lowered; no dimension was promoted to gating. Both
      pinned by tests.
- [x] Autoloop not resumed; no Codex dispatched; no merge to
      `perf-replan-2026-07` or `main`; no other sprint's paths touched
      (`case_specs/**` unedited — lint identical at 488/226/51).
- [x] No run artifacts committed; every commit staged by explicit path.

---

## 10. Post-handoff follow-ups (2026-07-26, separate session)

Written after the handoff above was committed, in the same worktree, on the
same branch. Nothing here changes a verdict, a bar, or a score.

**Environment.** `data/eval_datasets` was symlinked to the primary
checkout's copy (`.gitignore:8 data/*` keeps `git worktree add` from carrying
the tree). Not a repo change; it is what made the §5 re-measurement possible.

**Corrections (commit `d34483e7`).** §5 attribution table, §7 item 3 and §8
item 3. Re-measured by traceback rather than by module: the baseline failures
split **13 (golden CSV) + 1 (`badcase_turns.csv`, worktree-only)**, not
12 + 2, so the contract's "13 of the 14 come from the golden CSV" was right
and the §8 complaint against it is withdrawn.

**§7 items 6 + 7 closed (commit `050a98c0`).** Both were in this sprint's
owned path and cost ~30 lines together.

- All four placeholder row builders now emit `contract_warnings` (empty), so
  the `KeyError` is gone from the rows that matter — the two HTTP-500 rows of
  §7 item 4 are exactly where it fired. The contract-violation row also now
  serialises `severity` on its synthetic L1 entry, which the populated path
  has always carried and `composite.py` treats as load-bearing.
- `Expected` gained a diagnostic-only `spec_context` label filled by the
  loader, so the five un-attributed advisory warnings now name
  `anchor_outcome_uc_{g,h,i,j,k}`. Pinned so it can never round-trip into a
  written spec YAML (that would churn the corpus under Sprint 106).
- Evidence: eval_interactive pytest **13 failed / 796 passed / 5 skipped**
  (from 13/787/5 post-symlink; +9 = the new tests, no regression). Corpus
  lint unchanged at **488 / 226 / 51**. No `server/**`, `case_specs/**`,
  `eval/**` or `data/**` change.

**§7 items 4 + 8 filed as failure briefs** under
`docs/diagnostics/failure-briefs/`:

- `sprint-105-2026-07-26-backend-500-unmeasured-sessions.md`. Adds to §7
  item 4: the two 500s are on **different backend builds** (the WS-6-A
  re-route commit landed between the `ws5-*` and `s103-*` runs), both fired
  on the turn where the customer changes the subject, and no backend log
  survives. **§5.8 does not preempt** — the layer is `infra` but the scope is
  the bot backend, not the eval framework.
- `sprint-105-2026-07-26-resolved-stamp-vs-authoritative-user-state.md`.
  **Re-routes §7 item 8 from `server/**` to `eval_spec`.**
  `ControlKernel.java:1558-1567` documents that the runtime never observes
  the simulator-side `user_state` / `goal_impossible` signals, so it cannot
  be the fix site. The M-Auto-9 product decision already rules that
  `resolved` + authoritative UNRESOLVED user_state ⇒ FAIL; the reason it does
  not bite here is that `outcome_checks.py:238` gates that path behind
  `conditional_outcome_acceptance`, which only **13 of 488 specs** declare.
  The moving session in §3 is the first case where the hole reaches the new
  ladder's top tier.

### 10.1 Independent-substrate validation — Sprint 104's six runs

The evidence in §2 and §3 comes entirely from runs this sprint also used to
find the defect. Sprint 104 recorded six further runs
(`csagent-wt-104/eval_interactive/results/20260725-18{2254,2528,2742,3008,3105,3438}`,
**11 sessions**) that Sprint 105 never saw. Re-scored offline with the new
`rescore` command, no bot call, no judge call:

```
cases                                        11
fidelity (reproduce their recorded verdict)  11/11
verdicts moved                                0
judge term raised from a structural 0.0      10/11
judge_basis: advisory_fallback 10 | none 1
containment tiers: D2 7 | D3 3 | UNKNOWN 1
```

**Zero verdicts moved on a substrate the sprint never saw.** The judge term
rose on 10 of 11 cases — from the structural `0.0` to a real advisory-based
measurement between `0.4000` and `1.0000` — and every one of those 11 cases
stayed `FAIL` at `composite 0.0000`, because an L1 hard check or an L2 gate
bites before the composite arithmetic runs
(`L1:escalation_reason_consistency`, `L2_GATE:correct_uc`,
`L2_GATE:correct_outcome`).

The sharpest single data point for P0: `cs_interactive_155` @ `s104-negctl`
now scores `judge = 1.0000` on advisory dims and **still fails**. A perfect
advisory judge cannot buy a pass past a failed gate — which is the property
§2's "advisory dims are a fallback signal, never a dilutant" rule was meant
to have, tested here against behaviour this sprint had no hand in.

It also corroborates §2's corpus-wide claim from a second angle: 10 of 11
cases resolve to `judge_basis = advisory_fallback`, i.e. their specs
configure no gating dimension either.

Two byproducts, both folded back above:

- **A third HTTP 500** — `cs_interactive_030` @ `20260725-183008`
  (`s104-negctl`), on a third backend build. The brief was widened to 3 of 27
  sessions and gained the fact that Sprint 104's rerun of the same case
  succeeded, so the fault is transient rather than input-deterministic.
- **The `OQ-S105.user-state-not-consumed` measurement**, run over all 27
  recorded sessions: exactly **1** carries `containment_outcome = "resolved"`,
  and its final `user_state` is `unresolved_after_help`. One opportunity, one
  miss.

**Remaining §7 items (1, 2, 3, 5) routed to the ledger, not committed.** A
drafted `docs/action_bank.md` §5.2 section is held in the session scratchpad
rather than written, because four worktrees appending to one 195 KB file
would conflict; the intended write is one wave-wide section by the deliver
agent at merge. It records, per item, the §5.8 judgement for the
framework-scoped `infra` item (`results.json` field loss is an R-item, **not**
a preempting brief — it corrupts no measurement already taken), the
`premature_finish` rubric OQ, the unowned golden-CSV decision, and a new
`OQ-S105.user-state-not-consumed` paired with the existing
`OQ-S77.goal-impossible-resolved-evidence`.
