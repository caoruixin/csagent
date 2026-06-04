---
title: Sprint 076 / S-Auto-21 dev handoff — customer-simulator role-inversion fix (M-Auto-5)
doc_tier: sprint-archive
status: current
implementation_status: partial
source_of_truth: eval_interactive/eval_interactive/simulator/user_simulator.py
last_reviewed: 2026-06-04
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  MEASUREMENT/INFRA sub-sprint. Code (#1-#5) shipped + committed; bad-case
  re-render (#6) run with real-LLM evidence (0 contamination). The M-Auto-5
  authoritative re-bless (#7) is HUMAN-GATED and left re-bless-ready (§6).
  In-fence reconciliation: the literal stop_reason="simulator_drift_blocked"
  is owned by session_runner (outside the edit fence) — see OQ-S76.drift-stop
  in §8.
---

# Sprint 076 / S-Auto-21 — customer-simulator role-inversion fix

## §0 Cold-start summary

**Sub-sprint:** S-Auto-21 (corrective #2 of M-Auto-5 — Eval Verdict
Correctness + Trace-Contract Honesty). **MEASUREMENT/INFRA only**: edits are
confined to the eval customer-simulator; NO bot / runtime / prompt / routing /
UC-hypothesis / escalation / skill / CaseSpec edit.

**What shipped (committed):**

| Commit | Scope |
|--------|-------|
| `5471f9e` | #1-#4 `user_simulator.py`: invert role map + fallback, per-turn persona re-anchor, negative-form Forbidden block, customer-voice drift guard (D1/D2/D3 + 3-attempt retry + `SimulatorDriftError` escape) |
| `ba47ec6` | #5 `test_user_simulator.py`: T1-T7 contract tests + drift-detector unit coverage |

**Test gates:**

- eval_interactive `uv run pytest`: **538 passed** (524 S-Auto-20 baseline + 14
  new). No regression.
- autoloop `uv run pytest`: **324 passed** (unchanged).
- Java: **untouched by construction** — `git diff` for this sub-sprint is two
  Python files under `eval_interactive/`; zero `server/` files changed, so the
  documented `1232 / 1 / 0 / 2` baseline cannot move. Not re-run (a full
  `mvn test` on unchanged source yields no information).

**What was run (dev, #6):** bad-case suite re-render on the fixed simulator,
real-LLM (moonshot simulator → localhost bot). Output:
`eval_interactive/results/20260604-152103/` (label
`m-auto-5-baseline-20260604-simfixed`). **Contamination = 0** across all 12
cases / 40 customer turns (§4). The drift guard never fired (0 detected, 0
blocked) — the role-map fix removed the contamination at its source.

**What did NOT ship / was NOT run:**

- The literal `stop_reason="simulator_drift_blocked"` (owned by
  `session_runner.py`, outside the edit fence) — implemented as the in-fence
  equivalent `SimulatorDriftError` + log; see §8 OQ-S76.drift-stop.
- The M-Auto-5 authoritative re-bless (#7) — HUMAN-GATED; re-bless-ready in §6.
- `m-auto-5-baseline-20260605` was NOT killed (no live process found; its
  results dir is untouched). S-Auto-17 overnight NOT launched; S-Auto-18 NOT
  opened; audit Clusters B + C (M-Auto-6) NOT touched.

---

## §1 Role map + re-anchor + negative-form block

### #1 role-map inversion (`user_simulator.py`)

`generate_next` transcript → chat-completions role map, before/after:

```python
# BEFORE (:187 pre-fix — role-inverted)
for turn in transcript:
    role = "user" if turn.get("role") == "user" else "assistant"
    messages.append({"role": role, "content": turn.get("message", "")})
if not transcript or transcript[-1].get("role") != "bot":
    messages.append({"role": "assistant", "content": bot_reply})

# AFTER (:327 + :333 post-fix — role from the SIMULATOR LLM's POV)
for turn in transcript:
    role = "assistant" if turn.get("role") == "user" else "user"
    messages.append({"role": role, "content": turn.get("message", "")})
if not transcript or transcript[-1].get("role") != "bot":
    messages.append({"role": "user", "content": bot_reply})
```

Rationale: under the OpenAI chat protocol `assistant` = "what *this* model
previously said". The simulated **customer's** own prior turns
(transcript `role="user"`) are the simulator LLM's own past output →
`assistant`. The **bot** under test (`role="bot"`) is the other party →
`user`. Pre-fix sent it inverted, so after 2-3 rounds the in-context pattern
read "a `user` asks for help; an `assistant` (me) provides help" and the
"customer" started answering as the support agent.

**4-turn walkthrough.** Transcript
`[user:u1, bot:b1, user:u2, bot:b2]`, `bot_reply="b2"`:

| msg # | PRE-FIX role | POST-FIX role | content |
|-------|--------------|---------------|---------|
| 0 | system | system | persona prompt |
| 1 | user | **assistant** | u1 (customer) |
| 2 | assistant | **user** | b1 (bot) |
| 3 | user | **assistant** | u2 (customer) |
| 4 | assistant | **user** | b2 (bot) |
| 5 | — (last turn is bot, no fallback) | — | — |
| 6 | user | user | re-anchor (#2) |
| 7 | user | user | "Based on the conversation above…" |

Pre-fix the simulator saw its own customer turns as the *other party*'s and
the bot's as its own → it learned to continue the bot. Post-fix the customer
turns are correctly its own past `assistant` output.

### #2 per-turn persona re-anchor (`user_simulator.py:340-348`)

A short user-role message inserted immediately before the terminal generation
prompt:

> Continuing as the customer (NOT the support agent). Your goal:
> {case_spec.persona.goal_summary}. Stay in customer voice.

(`persona.goal_summary` is the property alias of `user_goal_summary`,
`case_spec/schema.py:127`.) Position-0 persona is one short instruction against
N rounds of conversation; this re-states role + goal every turn.

### #3 negative-form Forbidden block (`_SYSTEM_PROMPT_TEMPLATE:42-55`)

Added a 7-bullet `Forbidden` block (customer-side framing, "do NOT …"):

```
Forbidden -- you are the customer, NEVER the support agent. As the customer you:
- do NOT apologize to or for the agent (no "I apologize", "I'm sorry to hear", "thanks for your patience", "thanks for the details");
- do NOT offer to help, check, look into, connect, or escalate ("let me check", "let me look into it", "let me connect you", "let me escalate");
- do NOT cite sources (e.g. "(source: kaXXXXXX)") or write help.gumtree.com URLs;
- do NOT speak for "our support team", "our technical team", or "our team";
- do NOT provide solutions, instructions, or policy statements;
- do NOT repeat the bot's previous turn verbatim;
- do NOT emit the meta-instruction "Based on the conversation above, generate your next customer response as JSON.".
```

---

## §2 Drift-guard implementation (#4)

All in `user_simulator.py`. The detector runs after a successful JSON parse and
treats drift like a parse failure for retry purposes, sharing the single
`_MAX_SIMULATOR_ATTEMPTS = 3` budget.

**Detectors** (`_detect_customer_voice_drift`, :183; order D3→D1→D2, any one hit
suffices):

- **D1 keyword** (case-insensitive substring), 13 markers:
  `["i apologize", "i'm sorry to hear", "thanks for your patience",
  "thanks for the details", "let me check", "let me look into",
  "let me connect", "let me escalate", "our support team",
  "our technical team", "our team", "(source:", "https://help.gumtree.com"]`.
- **D2 verbatim regurgitation** (`_is_regurgitation`, :161): 8-gram Jaccard
  over whitespace tokens of the message vs the immediately-prior bot turn;
  drift when `jaccard ≥ 0.8`. When the prior bot turn is `< 8` tokens (no
  8-grams), fall back to whole-message exact match.
- **D3 system-prompt leakage**: literal probes
  `["based on the conversation above", "generate your next customer response as json"]`.

**Retry + escape control flow** (`_call_llm`, :356):

1. Parse failure → existing schema-reminder retry (unchanged).
2. Parsed-but-drifted → log `simulator_drift_detected detector=… attempt=…`
   (:416), append `_DRIFT_RETRY_INSTRUCTION` ("Your previous response drifted
   into the support agent's voice… Respond with the JSON contract."), retry.
3. **Budget exhausted** (:437-447): re-evaluate the last raw response once. If
   it still drifts → log `simulator_drift_blocked detector=… after 3 attempts`
   and **raise `SimulatorDriftError(detector, snippet, attempts)`** — the
   drifted turn is **never** returned into the transcript. A clean-but-
   unparseable last response still falls back to the lenient raw-text parse; a
   pure transport failure still yields the canned line. **Invariant: `_call_llm`
   never returns a message the detector flags.**

**Anti-误杀 / termination:** the 3-attempt budget guarantees termination (no
infinite loop). A legitimate customer turn that trips D1 (e.g. "I apologize for
the blurry screenshot") retries and, failing to escape, ends on the block path
for human review (§4d) — it is never looped indefinitely, and it is never
silently accepted.

**In-fence reconciliation (READ THIS).** The contract asked the blocked session
to surface `stop_reason="simulator_drift_blocked"`. `stop_reason` is set
**exclusively** in `session_runner.run_session` (`simulator/session_runner.py`),
which is **outside this sub-sprint's edit fence** (§6 fences + §11 "do NOT edit
this file"). The executor's generic handler (`batch/executor.py:279`) maps any
raise from `generate_next` to `stop_reason="error"`. `error` is **not** in
`hard_checks._VALID_TERMINAL_STOP_REASONS` (`{goal_achieved, goal_impossible,
max_turns_exceeded}`, `scoring/hard_checks.py:763`), so a blank-containment
errored session **fails `trace_minimum`** rather than vacuous-passing — i.e. the
correct outcome (a contaminated session is NOT scored as a pass). I therefore
shipped the in-fence equivalent (raise + `simulator_drift_blocked` log + carry
`detector/snippet/attempts`) and surfaced the literal-string plumbing as
OQ-S76.drift-stop (§8) — a mechanical one-line catch in `session_runner` once a
follow-up sub-sprint opens that fence.

---

## §3 Contract tests T1-T7 (`tests/test_user_simulator.py`)

Mock LLM via the existing `_FakeClient`/`_simulator_with` pattern; `_full_case`
provides every field `_SYSTEM_PROMPT_TEMPLATE` reads. Per §5.7 these are
wiring/shape/control-flow tests, **not** primary fix evidence (that is §4).

| Test | file:line | Assertion (mock construction) |
|------|-----------|-------------------------------|
| T1 role-map invariant | :279 | 4-turn transcript → sent messages have customer turns as `assistant`, bot turns as `user`; (`:291`) latest-bot-reply fallback is `user`-role. One clean mock reply. |
| T2 re-anchor present | :305 | `sent[-2]` is a `user` re-anchor containing "Continuing as the customer" + the `persona.goal_summary` substring; `sent[-1]` is the generation prompt. |
| T3 negative-form rules | :323 | rendered system prompt (`sent[0]`) contains "do NOT apologize", "do NOT cite sources", "do NOT repeat the bot's previous turn verbatim". |
| T4 D1 keyword retry | :342 | mock #1 "I apologize…Let me check…", #2 clean → returns #2; `simulator_drift_detected` logged; `_DRIFT_RETRY_INSTRUCTION` injected. |
| T5 D2 regurgitation retry | :361 | mock #1 = verbatim ≥8-token prior bot turn, #2 clean → retry fires; `detector=D2` logged. |
| T6 D3 leakage retry | :378 | mock #1 = "Based on the conversation above…", #2 clean → retry; `detector=D3` logged. |
| T7 3× drift blocks | :393 | mock = 3× D1 drift → `pytest.raises(SimulatorDriftError)` (detector="D1", attempts=3); `simulator_drift_blocked` logged; exactly 3 LLM calls; drifted turn never returned. |
| TestDriftDetector | :408 | unit coverage: clean/D1/D3/D2-Jaccard/short-prior exact-match/no-prior-skip. |

---

## §4 Bad-case re-render evidence (real-LLM, §5.7)

Run: `cd eval_interactive && eval-interactive run --set bad_cases --label
m-auto-5-baseline-20260604-simfixed`. Simulator deterministic
(`simulator_temperature=0.0`, OQ-S65.8), so a single draw is reproducible.
Output `eval_interactive/results/20260604-152103/` (gitignored). 12 cases, 40
customer turns, 365s wall. (Programmatic 0/12 PASS is the expected
human-judgment-suite informational verdict per §5.6 — NOT a contamination
signal; bot pass-rate is out of S-Auto-21 scope. Observation: `mean_judge=0.0`
across the suite — pre-existing judge surface, tracked separately, not this
sub-sprint.)

### §4a D1 keyword sweep (goal: 0)

Swept every `role="user"` turn at `turn_index ≥ 1` with the live `_DRIFT_KEYWORDS`
catalogue (`/tmp/s76_sweep.py` imports the module constants for exact parity).

```
total user turns swept: 40
contaminated turns:     0   (goal: 0)
```

Per-case (all `D1_hits = 0`): alice(3), cs001(4), cs011(2), cs012(8),
cs014(1), cs015(1), cs029(2), cs066(2), cs095(6), fg5q(2), iwzx(4), wmkb(5).
**VERDICT: PASS — 0 contamination, no residual to classify.**

### §4b regurgitation + leakage spot-check (3 random sessions)

cs011 / cs095 / cs001: `byte_identical_to_prior_bot = 0`, `D3_leak = 0` each.

### §4c same-case cross-time (OLD buggy sim vs NEW fixed sim)

OLD side = `results/m-auto-5-baseline-20260604/_rebless_scratch/bad_cases/
results.json` (same 12 cases, rendered on the **buggy** simulator). NEW =
this re-render. D1-contaminated customer turns:

| | OLD (buggy) | NEW (fixed) |
|---|---|---|
| alice_uc_a_uc_h_misclass | **1** (+ a `(source:` token in a customer turn) | 0 |
| cs001_uc_c_mechanical_template_escalate | **1** | 0 |
| cs012_uc_fp_late_phone_failure_path | **1** | 0 |
| other 9 cases | 0 | 0 |
| **total D1-contaminated user turns** | **3** | **0** |

Concrete contrast (`alice`, turn 3, CUSTOMER role):

- **OLD (buggy):** *"I understand your frustration, Alice. Unfortunately, I
  don't have access to the specific reason… The article 'My Ad was Removed'
  (source: ka44J000000gKv5QAE) explains… I'd recommend contacting our customer
  support team who can look into the exact reason for you."* — a verbatim
  support-agent reply (apology + source citation + "our customer support team")
  emitted **as the customer**.
- **NEW (fixed):** *"I've checked the posting rules, but I'm still not sure what
  the issue is. Can you please tell me the specific reason my ad was removed?"*
  — genuine customer voice.

(Turn counts/char totals differ OLD↔NEW because the fixed simulator drives a
different, correct conversation — expected, not a regression.)

Corroboration of the audit's 14.4 % finding: a sweep of pre-fix raw runs under
`results/2026*/` found **852** D1-contaminated customer turns; the fixed run has
0.

### §4d `simulator_drift_blocked` audit

`simulator_drift_detected = 0`, `simulator_drift_blocked = 0` in the run log.
The guard never had to fire: the role-map fix (#1) plus the re-anchor (#2) and
Forbidden block (#3) kept every turn in customer voice, so there were no
false-positives to refine and no genuine-inability sessions to escalate.

---

## §5 §5.7 self-classification

- **Mock-LLM (NOT primary evidence):** T1-T7 + TestDriftDetector. They prove the
  role map, re-anchor, prompt shape, and retry/escape control flow are wired
  correctly. The mock controls the measured variable, so they cannot show the
  simulator is *behaviourally* fixed.
- **Real-LLM (the evidence gate):** §4 — the bad-case re-render rendered with
  the real moonshot simulator against the real bot. 0/40 contamination, the
  OLD→NEW cross-time contrast (3→0), and the 852→0 corpus corroboration are the
  evidence that the fix works in practice.

---

## §6 M-Auto-5 authoritative re-bless — RE-BLESS-READY (HUMAN-GATED, NOT run)

This sub-sprint runs only the focused bad-case re-render (#6). The authoritative
multi-suite re-bless (#7) is human-launched.

**Preconditions (human):**

1. `git status` clean on `auto-loop-branch` at `ba47ec6` (or later).
2. Boot the backend fresh (no hot-reload — `feedback_restart_backend_before_eyeball`):
   `cd server && mvn -o spring-boot:run -Dspring-boot.run.profiles=local`
   (Postgres + Redis must be up; bot LLM creds in repo-root `.env.local`).
   Confirm `curl -s localhost:8080/actuator/health` → `status:UP`. Bot code is
   untouched this sub-sprint, so no rebuild semantics changed.
3. Keep the Mac AWAKE for the full run (`caffeinate -dimsu …` or wrap the
   command). A sleep-spanned run is uncertifiable — kill + re-run
   (`feedback_long_llm_run_no_sleep`).

**Exact command (fresh dated dir; old baseline RETAINED, pointer NOT moved):**

```bash
cd autoloop && uv run python scripts/rebless_baseline.py \
    --n 7 \
    --out-dir ../eval_interactive/results/m-auto-5-baseline-20260604-simfixed
```

(`--n` per the captured S-Auto-17 draw depth; `--n 5` tool default, `--n 3`
matches live `config.fitness.samples_per_case` — human picks `n`. Use today's
date if it differs.)

**Monitor / abort:** watch `<out-dir>/_rebless_report.json`; the run writes
`<out-dir>/<suite>/aggregated.json` per suite and does NOT move
`config.fitness.baseline_dir` (still `m-auto-4-baseline-20260604`). Expect
`suspect_baseline_manipulation` to fire — the corrected measurement shifts the
verdict distribution; that is the intended effect, not gaming. Moving the
`baseline_dir` pointer + flipping `docs/current_eval_baseline.md` status is a
separate **deliver-agent** action, not this dev session.

---

## §7 STOP confirmations

- [x] `config.fitness.baseline_dir` pointer NOT moved (no config edit).
- [x] `m-auto-5-baseline-20260605` NOT killed — no live process existed; its
      results dir is untouched.
- [x] Full M-Auto-5 authoritative re-bless NOT run (re-bless-ready, §6).
- [x] S-Auto-17 validation overnight NOT launched.
- [x] S-Auto-18 / any M-Auto-4 work NOT opened.
- [x] Audit Clusters B + C (M-Auto-6) NOT touched.
- [x] No CaseSpec / `case_spec_overrides.yaml` / FAQ / judge / prompt-template
      (other than the simulator's own system prompt) / autoloop 5-file scoring
      set edited. `git diff` = `user_simulator.py` + `test_user_simulator.py`.
- [x] Dev backend booted for #6 was stopped afterward (:8080 free).

---

## §8 Open questions

- **OQ-S76.drift-stop** — surface the literal
  `stop_reason="simulator_drift_blocked"`. `SimulatorDriftError` carries
  `detector/snippet/attempts`; a future sub-sprint that opens the
  `session_runner` fence can add a one-line `try/except SimulatorDriftError`
  around the `generate_next` call (`session_runner.py:217`) to set
  `result.stop_reason="simulator_drift_blocked"`, plus an executor branch
  (`executor.py`) to record it distinctly from `error`. Today the raise is
  recorded as `stop_reason="error"`, which already fails `trace_minimum`
  correctly (so no scoring vacuous-pass) — this OQ is for label fidelity in
  reports/audits, not correctness.
- **OQ-S76.A6** (carry from the dev prompt §5.5) — the bot self-diagnoses
  simulator drift in its own reasoning field (audit §2.6) and the runtime
  ignores it. NOT in scope here. If S-Auto-21 ships clean (it did — 0
  contamination), A.6 may dissolve; otherwise it is an M-Auto-6 candidate.
- **Observation (not an OQ for this sub-sprint):** `mean_judge=0.0` across the
  re-rendered bad-case suite — a judge/observability surface unrelated to the
  simulator fix; flagged for the deliver-agent.
