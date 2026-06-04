---
title: Sprint 076 / S-Auto-21 — Simulator role-inversion fix + bad-case suite re-render + baseline re-bless (M-Auto-5 sub-sprint 3)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-04
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-075-objective.md]
superseded_by: null
notes: >
  Corrective sub-sprint #2 of M-Auto-5, opened 2026-06-04 in response to the
  diagnostic `docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md`.
  S-Auto-19 (eval reads) and S-Auto-20 (runtime stamp + loop_detected) corrected
  the OUTPUT-column verdict pipeline. S-Auto-21 corrects the INPUT column: the
  customer simulator at `eval_interactive/.../simulator/user_simulator.py:187`
  inverts roles when sending the transcript to the chat-completions API
  (transcript `role="user"` — the simulated customer's own prior outputs — is
  sent as `assistant`; the bot is sent as `user`). From turn ~3+ this conditions
  the simulator LLM to behave as the support agent. Lower-bound contamination
  across `eval_interactive/results/**` is 1,017 / 7,071 sessions (14.4 %); the
  defect emerged 2026-05-29 → 2026-06-04. Every M-Auto-5 baseline so far —
  `m-auto-5-baseline-20260604`, the still-running `m-auto-5-baseline-20260605` —
  is contaminated on the input side and not certifiable. This sub-sprint fixes
  the simulator + adds a multi-turn contract test + re-renders the curated
  bad-case suite + ships the authoritative M-Auto-5 baseline on the simulator-
  fixed corpus. Clusters B and C from the audit are OUT of scope and routed to
  M-Auto-6 (B+C joint, with 2× research-agent dispatch). A.6 (bot self-diagnoses
  sim drift; runtime ignores) is an OQ carry, not fixed here. NO bot / runtime /
  prompt / routing / UC-hypothesis / escalation-posture / skill-soft-field /
  CaseSpec-rubric edit. The still-running `m-auto-5-baseline-20260605` is NOT
  killed — it is retained as FORENSIC data (do not consume its stability
  classifications as input to any downstream semantic sprint). Dev session
  source-of-truth: compact/sprint-076-dev-prompt.md.
---

# Sprint 076 / S-Auto-21 — Simulator role-inversion fix + suite re-render + M-Auto-5 re-bless

## Class

- **Layer (primary)**: `infra` — eval simulator correctness
  (`eval_interactive/eval_interactive/simulator/user_simulator.py` + simulator
  system prompt + the simulator contract tests). §1.4 (eval contract / persistence)
  ground; NOT §1.3 LLM-semantic ground. NO bot / runtime / prompt / routing /
  UC-hypothesis / escalation-posture / skill-soft-field / CaseSpec-rubric edit.
- **§7 stanza**: §7-EXEMPT (characterization / measurement-infra). Stanza
  **included below** for rigor because audit-quantified contamination
  (14.4 %) reaches every gate-contributing signal computed across the last
  week.
- **Codex review plan (§4.3)**: PER-SUB-SPRINT RECOMMENDED; folds into the
  M-Auto-5 milestone-shared close. NOT a fence-#13 SHA trigger (the autoloop
  5-file scoring set is untouched). Codex focus (below): no semantic-side
  rubric widening / drift-guard keyword set is declarative measurement (not
  routing semantic) / anti-误杀 (guard cannot loop a legitimate customer turn
  to failure) / §5.7 (mock contract test covers wiring only; real-LLM
  re-rendered traces are evidence of fix).
- **Position**: M-Auto-5 sub-sprint 3 of 3 (S-Auto-19 → S-Auto-20 →
  **S-Auto-21**). After this sub-sprint M-Auto-5 is close-ready. M-Auto-4's
  S-Auto-18 (bounded §5 escalation-family tiers) is **deferred to M-Auto-7
  or later**, behind M-Auto-6 (Clusters B + C of the audit).

## Goal

Stop the customer simulator from drifting into agent voice from turn ~3
onward, restore the input column of every bad-case + anchor + shadow trace
to genuine customer voice, and re-bless the authoritative M-Auto-5 baseline
on the simulator-fixed corpus so the M-Auto-5 north-star (honest per-case
verdicts) is finally achievable end-to-end.

**This sub-sprint does NOT change the bot's customer-service ability.** Every
edit is in the eval simulator harness (one Python file + its prompt + its
tests + a re-render of the suite). The bot, runtime, prompts, CaseSpecs, and
the autoloop scoring SHA-locked set are untouched.

## Scope (executable, #1-#7)

### #1 — Root-cause fix: invert the role map in `user_simulator.py:187`

`generate_next` (lines 177-198) currently emits to the OpenAI
chat-completions API:

```python
for turn in transcript:
    role = "user" if turn.get("role") == "user" else "assistant"
    messages.append({"role": role, "content": turn.get("message", "")})
```

Transcript `role="user"` is the **simulated customer's own prior output**;
transcript `role="bot"` is the **CS agent under test**. Under the
chat-completions protocol, `assistant` means "what *this* model previously
said" — so the current code conditions the simulator LLM, from turn 2-3+,
that *it* is the helper. The fix is to invert the map: simulator's own
prior outputs map to `assistant`, bot replies map to `user`.

Concretely:

```python
for turn in transcript:
    role = "assistant" if turn.get("role") == "user" else "user"
    messages.append({"role": role, "content": turn.get("message", "")})
```

Also fix the latest-bot-reply fallback on line 191-192 — the inserted
`{"role": "assistant", "content": bot_reply}` is the **bot's** reply, so it
must be `"role": "user"` after the inversion. Verify by walking through a
3-turn transcript and confirming that the simulator LLM's view is: prior
customer turns = its own past assistant outputs; bot turns = the other party
(user). Cite the chosen lines in the handoff.

### #2 — Per-turn persona re-anchor

The persona is anchored only at position-0 system message
(`user_simulator.py:21-49`); nothing re-injects it across N rounds of
chat-completions conditioning. Add a short re-anchor user-role message
appended **before** the existing terminal `"Based on the conversation
above, generate your next customer response as JSON."` prompt, of the
form:

> Continuing as the customer (NOT the support agent). Your goal:
> {persona.goal_summary}. Stay in customer voice.

Re-anchor lives in code (not Jinja) so it always fires; persona slot
references the same `case_spec.persona.goal_summary` already used at
position 0. One short line; do not duplicate the full system prompt.

### #3 — Negative-form rules in the simulator system prompt

The Jinja template at `_SYSTEM_PROMPT_TEMPLATE` (lines 21-49) has no
"you are NEVER the support agent" rule. Add a negative-form `Forbidden`
block to the template, explicitly listing the behaviours the audit §2.3
catalogued:

- do NOT apologize or say "I apologize", "I'm sorry to hear", "Thanks for
  your patience";
- do NOT say "let me check", "let me look into", "let me connect you",
  "let me escalate";
- do NOT cite sources (no `(source: …)` token; no `https://help.gumtree.com/…`
  URLs);
- do NOT refer to "our support team", "our technical team", "our team";
- do NOT provide solutions, instructions, or policy statements;
- do NOT repeat the bot's previous turn verbatim.

Block is declarative + framed as customer-side behaviour ("a real customer
NEVER..."). Keep it tight (≤ 8 bullets).

### #4 — Post-generation customer-voice guard with retry

`_MAX_SIMULATOR_ATTEMPTS=3` currently retries on JSON parsing failures
only (`_try_parse_simulator_response`). Add a customer-voice **drift
detector** that runs after a successful parse and treats drift as a parse
failure for retry purposes. A drift is any of:

- **D1 keyword hit**: the message matches the audit §2.4 keyword set
  (case-insensitive, substring): `"I apologize"`, `"i'm sorry to hear"`,
  `"thanks for your patience"`, `"thanks for the details"`,
  `"let me check"`, `"let me look into"`, `"let me connect"`,
  `"let me escalate"`, `"our support team"`, `"our technical team"`,
  `"our team"`, `"(source:"`, `"https://help.gumtree.com"`.
- **D2 verbatim regurgitation**: the message overlaps ≥ 80 % of the
  immediately-prior bot turn at the token level (audit §2.5 subtype 2).
  Implement as: tokenize on whitespace, compute Jaccard or contiguous
  N-gram overlap (8-gram), threshold 0.8.
- **D3 system-prompt leakage**: the message contains a substring of the
  fixed parts of the system prompt template (audit §2.5 subtype 3, e.g.
  the literal `"Based on the conversation above, generate your next
  customer response as JSON."`). A small set of literal probes is fine.

When drift is detected:

1. Log a `simulator_drift_detected` event (include which detector fired,
   the message snippet, the attempt index).
2. Retry within the existing `_MAX_SIMULATOR_ATTEMPTS=3` budget, with a
   corrective reminder appended to the message list (analogous to the
   existing `_PARSE_RETRY_INSTRUCTION`, but worded for drift: "Your
   previous response drifted into the support agent's voice. Respond as
   the customer.").
3. After 3 attempts all drift, **emit a `simulator_drift_blocked`
   diagnostic into the session trace** and end the session with
   `stop_reason="simulator_drift_blocked"`. **Do NOT silently accept a
   drifted turn into the transcript.**

Anti-误杀 (HARD): if a customer COULD legitimately say a phrase that
matches a keyword (e.g. a frustrated customer typing "I apologize for
the bad screenshot"), the retry-then-end path is the right escape.
Endless-loop is forbidden; the 3-attempt budget guarantees termination.

### #5 — Multi-turn contract test (`eval_interactive/tests/test_user_simulator.py`)

Audit §7 (third bullet) explicitly calls out that the existing test file
covers JSON parsing only — there is no `generate_next` multi-turn test.
Add at minimum:

- **T1 role-map invariant**: build a 4-turn transcript (alternating
  user/bot), mock the underlying LLM call, assert the messages list
  passed to the LLM has the inverted role map (simulator's prior
  customer turns = `assistant`, bot replies = `user`).
- **T2 persona re-anchor present**: assert the re-anchor message
  appears immediately before the terminal "generate your next customer
  response as JSON." prompt, and contains a substring of the case_spec
  persona.goal_summary.
- **T3 negative-form rules present**: assert the rendered system prompt
  contains the negative-form block (a few representative substrings).
- **T4 drift guard retries on keyword hit**: mock the LLM to return
  `{"message": "I apologize for the confusion. Let me check..."}` on
  the first call and a clean customer turn on the second; assert
  `generate_next` returns the second call's content and a
  `simulator_drift_detected` log/diagnostic was raised.
- **T5 drift guard retries on verbatim regurgitation**: mock the LLM
  to return the immediately-prior bot reply verbatim on the first
  call; assert retry fires.
- **T6 drift guard retries on system-prompt leakage**: mock the LLM
  to return the literal "Based on the conversation above..." string;
  assert retry fires.
- **T7 drift guard end-of-session after 3 failed attempts**: mock all
  3 attempts to drift; assert a `simulator_drift_blocked` diagnostic
  is emitted and the session terminates with
  `stop_reason="simulator_drift_blocked"`. Anti-误杀: the bot under
  test gets no opportunity to be scored on a drifted turn.

**§5.7 acknowledgement**: T1-T7 are mock-LLM tests covering wiring,
prompt-shape, and retry control flow. They are NOT primary evidence
that the simulator is fixed — that evidence comes from #6 (real-LLM
re-rendered traces).

### #6 — Bad-case suite re-render (real-LLM evidence per §5.7)

Re-run the curated bad-case suite (`eval_interactive/case_specs/bad_cases/`)
end-to-end with the simulator-fixed code. Persist to
`eval_interactive/results/m-auto-5-baseline-YYYYMMDD-simfixed/bad_cases/`.

Acceptance evidence (write into handoff):

- **6a per-turn audit table**: for every session, every `role="user"`
  turn at turn-index ≥ 1, run the audit §2.4 keyword sweep. Expected:
  0 contaminated turns (vs the prior 14.4 % session-level / 1,424
  user-turn contamination across `eval_interactive/results/**`). Any
  residual: enumerate + classify (legitimate customer phrase that
  triggers a keyword vs. drift the guard missed).
- **6b regurgitation + leakage spot-check**: for 3 random sessions per
  audit §6 step 1 (random within bad_cases), confirm no user turn is
  byte-identical to the prior bot turn and no user turn contains the
  fixed parts of the system prompt template.
- **6c same-case-cross-time**: pick one case that appears in both
  `m-auto-4-baseline-20260604/bad_cases/` and the new re-render;
  diff turn-by-turn at a high level (length, presence of citation
  tokens, presence of customer-voice markers). Confirm the new render
  is qualitatively customer-voiced.
- **6d `simulator_drift_blocked` audit**: count sessions that ended
  with that stop_reason. Each must be inspected — they are either
  (a) a genuine customer turn that hit a keyword (anti-误杀 surface,
  bug in #4's keyword set, refine), or (b) a model genuinely unable
  to maintain customer voice for this persona (rare; flag to human).

### #7 — M-Auto-5 authoritative re-bless (HUMAN-GATED)

After #1-#6 pass and the bad-case suite re-render is reviewed:

- 7a **dev STOPS** before launching the full multi-suite real-LLM
  re-bless. Leave it re-bless-ready: backend healthy (no rebuild
  needed — bot code untouched), clean tree, the exact command (incl.
  output dir `m-auto-5-baseline-YYYYMMDD-simfixed`) ready in the
  handoff.
- 7b **HUMAN launches** the re-bless overnight (or batch). Mac awake
  (per `feedback_long_llm_run_no_sleep`); clean tree (per
  `project_autoloop_dirty_index_hazard`).
- 7c **DELIVER + HUMAN review** the re-bless: confirm the verdict-
  distribution shift vs `m-auto-4-baseline-20260604` is explainable
  by (i) S-Auto-19 eval reads, (ii) S-Auto-20 runtime stamp,
  (iii) S-Auto-21 simulator-clean traces — no unexplained flips.
- 7d **Pointer move** `baseline_dir` from `m-auto-4-baseline-20260604`
  to `m-auto-5-baseline-YYYYMMDD-simfixed`; OLD baselines RETAINED
  (`m-auto-4-baseline-20260604`, `m-auto-5-baseline-20260604`, the
  still-running `m-auto-5-baseline-20260605`). The 20260605 dir is
  explicitly **forensic-only** — do not consume its stability
  classifications as input to any downstream semantic sprint.
- 7e **`docs/current_eval_baseline.md` flip**: deliver-agent updates
  `implementation_status` from `partial` (set during this sub-sprint
  open) back to `current` when the pointer move is committed.

## Anti-误杀 invariants (HARD, non-negotiable)

1. **Guard cannot loop a legitimate customer turn to indefinite
   failure.** 3-attempt budget + `simulator_drift_blocked` escape are
   mandatory; remove either = regression.
2. **No bot / runtime / prompt / routing / UC-hypothesis / escalation /
   skill / CaseSpec edit anywhere.** Entire scope is the eval simulator
   harness.
3. **No rubric widening (§1.7 / §5.4).** This sub-sprint does not
   change what counts as success for the bot; it changes who is
   producing the input column.
4. **Drift guard keyword set is declarative measurement, not a routing
   semantic rule.** Codex §4.1 question 4 will ask; the answer is the
   keyword set is the audit's first-party catalogue of agent-voice
   markers (apology / "let me check" / source citation / `help.gumtree.com`
   URL / "our team"). Same character as "no `noreply@gumtree.com` PII"
   in S-Auto-19's relaxation (first-party, declarative, scoped to the
   measurement surface).
5. **A.6 NOT in scope.** The audit §2.6 observation (bot's own LLM
   reasoning identifies a turn as "the user's message is a bot apology,
   not a new user query" and the runtime ignores the signal) is an OQ
   carry: **OQ-S76.A6 — bot self-diagnosis of simulator drift unused by
   runtime**. If the simulator fix here is complete, A.6 may dissolve;
   if a residual contamination tail persists post-S-Auto-21, A.6 may
   become a M-Auto-6 candidate. Decision deferred.

## Hard fences / STOP conditions

- **No edit outside `eval_interactive/eval_interactive/simulator/` +
  `eval_interactive/tests/test_user_simulator.py`** except the re-render
  output dir and (at close, by deliver) `docs/current_eval_baseline.md`
  + `docs/sprint_objective.md` + `docs/10-handoff.md` housekeeping.
- **Do NOT kill `m-auto-5-baseline-20260605`** (still running) — it is
  forensic data.
- **Do NOT launch the full M-Auto-5 re-bless yourself** — human-gated
  (#7b).
- **Do NOT launch the held S-Auto-17 validation overnight**; it
  requires the simfixed baseline as its certifiable input.
- **Do NOT open S-Auto-18 or any M-Auto-4 work**; it is deferred behind
  M-Auto-6 (audit Clusters B + C).
- **Do NOT touch Clusters B and C from the audit.** They are routed
  to M-Auto-6 (B.1 trace truncation; B.2 primary_uc vs active_use_case
  authority; C.1 cs59s 400; C.2 46f5b2e9 500; C.3 generic clarifier
  branch).
- **No CaseSpec, no `case_spec_overrides.yaml`, no FAQ, no prompt
  template, no judge config edit.**
- **No autoloop 5-file scoring SHA-locked set edit.**
- All eval / re-render only on a clean committed tree (per
  `project_autoloop_dirty_index_hazard`).

## Test / eval requirements

- `eval_interactive/tests/test_user_simulator.py`: T1-T7 (above) green.
- `eval_interactive` pytest: no regression vs current `524 passed`
  (S-Auto-20 baseline) under `uv run pytest`. New tests increment, not
  replace.
- Java suite: untouched (this sub-sprint is Python-only). Confirm
  `1232 / 1 / 0 / 2` unchanged.
- autoloop pytest: untouched. Confirm `324` unchanged.
- Bad-case suite re-render (#6) on the simfixed simulator: audit §2.4
  keyword sweep contamination → 0 (or fully-enumerated residual with
  classification).

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` — eval simulator correctness
(`eval_interactive/.../simulator/user_simulator.py` + system prompt +
contract test file). NO `semantic_planner` / `prompt_projection` /
`skill_state` / `eval_spec` / `judge_calibration` / `product_policy` /
`java_guard` edit.

**Tier-0 invariant:** adds none; preserves all Tier-0 families at
current strictness. The simulator drift guard records a measurement
disposition the *simulator* reached; it changes no bot decision and
adds no runtime invariant.

**Semantic hardcode:** none. The drift-guard keyword set is the audit's
first-party catalogue of agent-voice markers (declarative measurement),
not a content-matching routing semantic rule. The role-map inversion is
a single-line API protocol fix; the per-turn persona re-anchor is a
short additional message in the simulator's own message list. The
negative-form block is the existing system-prompt template extended
with prohibitions on customer-side behaviour. None of these are
keyword/regex/enum routing of the bot, escalation, UC, or any §1.3
LLM-owned decision.

**Generalization coverage:** characterization / measurement-infra
sub-sprint — evidence is (a) #5 contract tests (mock, wiring), (b) #6
bad-case re-render keyword sweep (real-LLM, 0 contamination expected),
(c) #6c same-case-cross-time qualitative diff, (d) #6d
`simulator_drift_blocked` audit. NOT target/neighbor/negative/shadow
case-family counts. L4 shadow firewall unchanged.

## Codex review plan (§4.3)

PER-SUB-SPRINT RECOMMENDED; folds into M-Auto-5 milestone-shared close.
NOT a fence-#13 SHA trigger.

Codex focus (in priority order):

1. **No semantic-side rubric widening (§1.7 / §5.4)**. Confirm no
   CaseSpec, no judge config, no `eval_spec` edit; the simulator change
   does not widen what counts as bot success.
2. **Drift-guard keyword set is declarative measurement, not routing
   semantic** (§4.1 question 4 / §1.7).
3. **Anti-误杀**: guard cannot loop a legitimate customer turn; the
   3-attempt budget + `simulator_drift_blocked` escape are in place;
   #6d audit shows the escape is rare and inspected.
4. **§5.7 evidence gate**: mock tests cover wiring; real-LLM
   re-rendered traces are the gate on whether the simulator is fixed.
5. **No bot / runtime / prompt edit anywhere** (verify the diff is
   strictly within `eval_interactive/eval_interactive/simulator/` +
   `eval_interactive/tests/test_user_simulator.py` + the re-render
   output dir).
6. **A.6 carry**: confirm A.6 (bot self-diagnoses sim drift; runtime
   ignores) is an OQ, not a stealth scope expansion into runtime.

## Handoff requirements (dev authors `docs/sprints/sprint-076-handoff.md`)

MUST include:

- **§0 cold-start summary** (the standard table: sub-sprint, commits,
  test counts, what shipped, what NOT shipped).
- **§1 the role-map diff** (`user_simulator.py:187` and the line-191
  fallback fix) with before/after; the chosen per-turn re-anchor text;
  the negative-form block diff in `_SYSTEM_PROMPT_TEMPLATE`.
- **§2 drift-guard implementation**: detector code references; keyword
  set in full; verbatim-regurgitation algorithm + threshold choice;
  system-prompt-leakage probe set; retry-and-escape control flow.
- **§3 contract tests T1-T7**: file:line for each; one paragraph per
  test on what it asserts and how mocks are constructed.
- **§4 bad-case re-render evidence**:
  - 4a keyword-sweep table (per session × turn): contamination count
    before (from `m-auto-4-baseline-20260604` or
    `m-auto-5-baseline-20260604` baseline of the same suite) and after
    (simfixed). Goal: 0 (or fully classified residual).
  - 4b regurgitation + system-prompt-leakage spot-check (3 sessions).
  - 4c same-case-cross-time diff (1 case).
  - 4d `simulator_drift_blocked` session list + classification (none
    expected; if any, enumerate each).
- **§5 §5.7 self-classification**: which evidence is mock (T1-T7) and
  which is real-LLM (the #6 re-render).
- **§6 re-bless-ready command**: the exact command + output dir name +
  preconditions (clean tree confirmed; Mac awake instruction; how to
  monitor; abort criteria).
- **§7 explicit STOP confirmations**: `baseline_dir` NOT moved (still
  `m-auto-4-baseline-20260604`); the full M-Auto-5 re-bless NOT run by
  dev (left re-bless-ready for human); `m-auto-5-baseline-20260605`
  NOT killed (forensic); S-Auto-17 overnight NOT launched; S-Auto-18
  NOT opened; M-Auto-6 (B + C) NOT touched.
- **§8 OQ carry**: OQ-S76.A6 (bot self-diagnoses sim drift, runtime
  ignores) — left for M-Auto-6 candidate decision.

## Commit discipline

- Stage **only** authorized files (NOT `git add -A`):
  - `eval_interactive/eval_interactive/simulator/user_simulator.py`
  - `eval_interactive/tests/test_user_simulator.py`
  - `docs/sprints/sprint-076-handoff.md` (when written)
  - Any new fixture data the contract tests require (in a single
    fixture subdir, named clearly).
- One commit per substantive step where practical (#1 role map, #2/#3
  prompt edits, #4 drift guard, #5 contract tests). The re-render
  output (`m-auto-5-baseline-YYYYMMDD-simfixed/`) is gitignored —
  reference the dir in the handoff, do not commit it.
- Run any eval / re-render only on a clean committed tree.
- Deliver-owned docs (this sprint_objective, the dev prompt, the
  milestone_objective update, the 10-handoff update) are committed
  separately by the deliver-agent + human.

## Self-check checklist (dev completes before claiming done)

- [ ] #1 role-map inversion applied at `user_simulator.py:187` AND the
      line-191 latest-bot-reply fallback; verified by walking a 4-turn
      transcript end-to-end in the handoff.
- [ ] #2 per-turn persona re-anchor message present before the
      terminal "generate ... JSON" prompt; references
      `case_spec.persona.goal_summary`.
- [ ] #3 negative-form block added to `_SYSTEM_PROMPT_TEMPLATE`;
      covers ≥ 6 prohibitions from the audit §2.3 catalogue.
- [ ] #4 drift guard (D1 keywords + D2 verbatim regurgitation + D3
      system-prompt leakage) implemented; 3-attempt retry budget in
      place; `simulator_drift_blocked` escape end-of-session present;
      anti-误杀 path verified by T7.
- [ ] #5 T1-T7 green; `eval_interactive` pytest no regression vs 524.
- [ ] #6 bad-case suite re-rendered (simfixed dir on disk); audit §2.4
      keyword sweep contamination = 0 or fully classified; 6b/6c/6d
      evidence in handoff.
- [ ] Java: no test touched (Python-only sub-sprint); confirmed
      `1232 / 1 / 0 / 2`.
- [ ] autoloop pytest `324` unchanged.
- [ ] `baseline_dir` NOT moved.
- [ ] `m-auto-5-baseline-20260605` NOT killed (forensic).
- [ ] Full M-Auto-5 re-bless NOT run (left re-bless-ready).
- [ ] S-Auto-17 overnight NOT launched; S-Auto-18 NOT opened.
- [ ] M-Auto-6 (audit Clusters B + C) NOT touched.
- [ ] Handoff written per §Handoff requirements above.
