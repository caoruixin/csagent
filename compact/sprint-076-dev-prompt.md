# Dev Prompt — Sprint 076 / S-Auto-21 (M-Auto-5 corrective sub-sprint #2)

> Self-contained executable view of `docs/sprint_objective.md` (prompt-artifact-rules §9).
> Paste into a fresh dev session. You need NO other doc except `AGENTS.md`
> (auto-loaded) + this prompt. Code anchors in §11 are read on demand.

## 1. Role identity

You are the **dev agent for Sprint 076 / S-Auto-21**, the corrective sub-sprint #2
of Milestone **M-Auto-5 — Eval Verdict Correctness + Trace-Contract Honesty**.

**One-line goal:** stop the customer simulator from drifting into agent voice from
turn ~3 onward; re-render the curated bad-case suite on the fixed simulator; leave
the M-Auto-5 baseline re-bless-ready (HUMAN-GATED). MEASUREMENT/INFRA only: NO bot
/ runtime / prompt / routing / UC-hypothesis / escalation / skill / CaseSpec edit.
The simulator change is a §1.4 eval-contract / measurement correction, not a
semantic change.

## 2. Read order (minimal)

1. `AGENTS.md` — governance (Constitution §1, §1.5/§1.7 anti-hardcode, §5.4
   no-eval-side-override-of-a-real-bug, **§5.7 real-LLM evidence gate**, §7
   stanza).
2. **This prompt** — the full contract.
3. On demand, the §11 code anchors.

Do NOT read `docs/sprints/*` or `docs/archive/*`. The audit document
(`docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md`) is
**input artifact** — you may read it for cross-reference, but every binding
fact this prompt needs is embedded below.

## 3. WHY (read before coding)

The customer simulator that drives every multi-turn eval case has a
**role-inversion defect** at `eval_interactive/eval_interactive/simulator/
user_simulator.py:187` that, from turn ~3 onward, conditions the simulator
LLM to behave as the **support agent**, not the customer. Across
`eval_interactive/results/**` (7,071 sessions in the last week),
**1,017 sessions (14.4 %)** contain at least one contaminated user turn
where the "customer" apologizes / says "let me check" / cites sources /
writes `help.gumtree.com` URLs / regurgitates the bot's prior turn
verbatim. Three observed sub-types:

1. **agent-voice drift** — apologetic / solution-oriented prose in
   customer voice.
2. **verbatim regurgitation** — the simulator emits the bot's prior turn
   word-for-word.
3. **system-prompt leakage** — the simulator emits its own meta-
   instruction "Based on the conversation above, generate your next
   customer response as JSON." as the customer turn.

All three have the same root cause: in `generate_next` (lines 177-198),
the transcript-to-API role map is inverted. Under the OpenAI
chat-completions protocol, `assistant` means "what *this* model
previously said". Today's code sends the simulated **customer's** prior
turns as `assistant` (so the simulator LLM sees them as its own past
utterances) and sends the **bot's** turns as `user` (the other party).
After 2-3 turns the in-context pattern says: "A `user` asks for help; an
`assistant` (me) provides help; repeat." The position-0 system message
(persona: "you are a customer contacting Gumtree support…") is a single
short instruction against N rounds of assistant-as-helper conditioning,
and the terminal "generate your next customer response as JSON" reminder
at line 197 does not undo the inversion.

The `_MAX_SIMULATOR_ATTEMPTS=3` retry mechanism (`_call_llm`) handles
JSON parse failures only — there is no post-generation customer-voice
guard. The contract test file `eval_interactive/tests/test_user_simulator.py`
covers JSON parsing only — there is no `generate_next` multi-turn test.

**Consequence**: every autoloop iteration, pass-rate metric, bad-case
suite rerun, and judge-driven scoring run produced in the last week is
computed on partially-fictional conversations where the bot was
penalised for failing to handle bot-voice "customer" turns. The
M-Auto-5 baselines `m-auto-5-baseline-20260604` and the still-running
`m-auto-5-baseline-20260605` are **not certifiable** until this
sub-sprint ships and the suite is re-rendered.

## 4. Scope

### Step 0 — context confirmation (read-only)

Open `eval_interactive/eval_interactive/simulator/user_simulator.py` and
verify the §3 narrative against the live code: the role map at line 187,
the latest-bot-reply fallback at line 191-192, the system prompt
template at lines 21-49, the `_MAX_SIMULATOR_ATTEMPTS=3` constant. Open
`eval_interactive/tests/test_user_simulator.py` and confirm it covers
JSON parsing only (no `generate_next` multi-turn test). Write nothing
yet; this step is mental verification of the contract you are about to
implement.

### #1 — Root-cause fix: invert the role map (`user_simulator.py:187` + 191-192)

Current:

```python
for turn in transcript:
    role = "user" if turn.get("role") == "user" else "assistant"
    messages.append({"role": role, "content": turn.get("message", "")})

if not transcript or transcript[-1].get("role") != "bot":
    messages.append({"role": "assistant", "content": bot_reply})
```

Target:

```python
for turn in transcript:
    role = "assistant" if turn.get("role") == "user" else "user"
    messages.append({"role": role, "content": turn.get("message", "")})

if not transcript or transcript[-1].get("role") != "bot":
    messages.append({"role": "user", "content": bot_reply})
```

Rationale: transcript `role="user"` is the **simulated customer's own
prior output** → from the simulator LLM's POV it is `assistant` (its own
past replies). Transcript `role="bot"` is the **CS agent under test** →
from the simulator LLM's POV it is `user` (the other party). The
latest-bot-reply fallback inserts the bot's reply, so it must be `user`,
not `assistant`.

Walk a hypothetical 4-turn transcript end-to-end in your handoff §1.
Show the messages list pre-fix and post-fix.

### #2 — Per-turn persona re-anchor

Audit §2.3: "persona is anchored only at message position 0; nothing
re-injects it turn-by-turn." Add a short user-role message **immediately
before** the existing terminal "Based on the conversation above,
generate your next customer response as JSON." prompt, of the form:

> Continuing as the customer (NOT the support agent). Your goal:
> {persona.goal_summary}. Stay in customer voice.

Implement in code (not in the Jinja template). It references the same
`case_spec.persona.goal_summary` already passed to the system prompt.
One short message; do not duplicate the full system prompt.

### #3 — Negative-form rules in the simulator system prompt

Extend the Jinja `_SYSTEM_PROMPT_TEMPLATE` (lines 21-49) with a
`Forbidden` block. The audit §2.3 catalogue:

```
Forbidden — you are NEVER the support agent. A real customer NEVER:
- apologizes for the support agent or says "I apologize", "I'm sorry to hear", "Thanks for your patience", "Thanks for the details";
- says "let me check", "let me look into it", "let me connect you", "let me escalate";
- cites sources (e.g. `(source: kaXXXXXX)`) or writes help.gumtree.com URLs;
- refers to "our support team", "our technical team", "our team";
- provides solutions, instructions, or policy statements;
- repeats the bot's previous turn verbatim;
- emits the meta-instruction "Based on the conversation above, generate your next customer response as JSON.".
```

Keep ≤ 8 bullets. Frame as customer-side behaviour, not as "don't say
keyword X".

### #4 — Post-generation customer-voice guard with retry

`_call_llm` currently retries on JSON parse failures only. Add a
customer-voice drift detector that runs after a successful parse and
treats drift as a parse failure for retry purposes.

**Detector** (after `_try_parse_simulator_response` returns a dict):

- **D1 keyword hit** (case-insensitive substring) — message contains
  any of:
  ```
  ["i apologize", "i'm sorry to hear", "thanks for your patience",
   "thanks for the details", "let me check", "let me look into",
   "let me connect", "let me escalate", "our support team",
   "our technical team", "our team", "(source:",
   "https://help.gumtree.com"]
  ```
- **D2 verbatim regurgitation** — message and the immediately-prior
  bot turn share an 8-gram overlap with Jaccard similarity ≥ 0.8 (over
  whitespace-tokenized 8-grams). If the prior turn is shorter than 8
  tokens, fall back to whole-message exact-match.
- **D3 system-prompt leakage** — message contains any literal probe
  from `["Based on the conversation above", "generate your next
  customer response as JSON"]`.

**Retry control flow**:

1. On drift detection, log a `simulator_drift_detected` event (include
   detector D1/D2/D3, the message snippet, attempt index).
2. Retry within the existing `_MAX_SIMULATOR_ATTEMPTS=3` budget. Append
   a corrective reminder to the messages list:
   > Your previous response drifted into the support agent's voice.
   > Respond as the customer (do NOT apologize, do NOT cite sources, do
   > NOT propose solutions). Respond with the JSON contract.
3. After 3 attempts all drift, emit a `simulator_drift_blocked`
   diagnostic and end the session with
   `stop_reason="simulator_drift_blocked"`. **Do NOT silently accept a
   drifted turn into the transcript.**

Anti-误杀 (HARD): the 3-attempt budget guarantees termination. A
legitimate customer turn that triggers D1 (e.g. a frustrated customer
typing "I apologize for the bad screenshot") will hit the retry, fail
to escape on first attempt, and end on `simulator_drift_blocked` — that
session is then reviewed at handoff §4d. Endless-loop is forbidden.

The drift event + blocked-stop_reason must be observable in the
session trace (whatever the existing session_runner trace API supports
— do not invent a new trace surface).

### #5 — Multi-turn contract test (`eval_interactive/tests/test_user_simulator.py`)

Add the following tests (mock the underlying LLM via the existing test
patterns in this file):

- **T1 role-map invariant** — build a 4-turn transcript (alternating
  user/bot), call `generate_next`, assert the messages list passed to
  the LLM has the inverted role map (customer prior turns = `assistant`,
  bot replies = `user`).
- **T2 persona re-anchor present** — assert the re-anchor message
  appears immediately before the terminal "generate ... JSON" prompt,
  and contains a substring of `case_spec.persona.goal_summary`.
- **T3 negative-form rules present** — assert the rendered system
  prompt contains ≥ 3 representative substrings from the §3 block (e.g.
  "do NOT apologize", "do NOT cite sources", "do NOT repeat the bot's
  previous turn verbatim").
- **T4 drift guard retries on D1 keyword hit** — mock LLM call 1 to
  return `"I apologize for the confusion. Let me check..."`; mock call
  2 to return a clean customer turn; assert `generate_next` returns
  call 2's content and `simulator_drift_detected` is logged.
- **T5 drift guard retries on D2 verbatim regurgitation** — mock LLM
  call 1 to return the immediately-prior bot reply verbatim (or near-
  verbatim above the 8-gram Jaccard threshold); mock call 2 clean;
  assert retry fires.
- **T6 drift guard retries on D3 system-prompt leakage** — mock LLM
  call 1 to return `"Based on the conversation above, generate your
  next customer response as JSON."`; mock call 2 clean; assert retry
  fires.
- **T7 drift guard end-of-session after 3 failed attempts** — mock all
  3 attempts to drift (D1); assert a `simulator_drift_blocked`
  diagnostic is emitted and the simulator end-of-session is signalled
  with `stop_reason="simulator_drift_blocked"`. The bot under test
  must not be scored on a drifted turn.

**§5.7 acknowledgement**: T1-T7 are mock-LLM tests covering wiring,
prompt-shape, and retry control flow. They are NOT primary evidence
that the simulator is fixed — that evidence comes from #6 (real-LLM
re-rendered traces).

### #6 — Bad-case suite re-render (real-LLM evidence per §5.7)

Re-run the curated bad-case suite (`eval_interactive/case_specs/bad_cases/`)
end-to-end with the simulator-fixed code. Persist to
`eval_interactive/results/m-auto-5-baseline-YYYYMMDD-simfixed/bad_cases/`.
Use the same harness invocation pattern S-Auto-19 used (see S-Auto-19
handoff in `docs/sprints/sprint-074-handoff.md` if you need the exact
command — ONLY for the command shape, not for any other content).

Evidence (write into handoff §4):

- **6a per-turn audit table**: for every session, every `role="user"`
  turn at turn-index ≥ 1, run the §4 D1 keyword sweep. Goal: 0
  contaminated turns. Any residual → enumerate + classify (legitimate
  customer phrase that triggers a keyword, vs drift the guard missed).
- **6b regurgitation + leakage spot-check**: for 3 random sessions,
  confirm no user turn is byte-identical to the prior bot turn and no
  user turn contains the D3 probe set.
- **6c same-case-cross-time**: pick one case present in both
  `m-auto-4-baseline-20260604/bad_cases/` and the new re-render; diff
  turn-by-turn at a high level (length, citation tokens, customer-voice
  markers).
- **6d `simulator_drift_blocked` audit**: count sessions that ended
  with that stop_reason. Each is inspected — (a) keyword false-positive
  (refine D1) or (b) genuine inability to maintain customer voice
  (flag to human).

### #7 — M-Auto-5 authoritative re-bless (HUMAN-GATED — dev STOPS here)

Leave re-bless-ready (do NOT execute):

- backend healthy (bot code is untouched in this sub-sprint — no rebuild
  needed; confirm `curl` health check or equivalent);
- clean committed tree (per `project_autoloop_dirty_index_hazard`);
- the exact command in handoff §6 with the output dir
  `m-auto-5-baseline-YYYYMMDD-simfixed`;
- preconditions (Mac awake per `feedback_long_llm_run_no_sleep`).

**HUMAN launches the re-bless; deliver-agent moves the `baseline_dir`
pointer and flips `docs/current_eval_baseline.md` status field.** Not
this sub-sprint's dev session.

## 5. Anti-误杀 invariants (HARD)

1. Guard cannot loop a legitimate customer turn to indefinite failure
   (3-attempt budget + `simulator_drift_blocked` escape mandatory).
2. NO bot / runtime / prompt / routing / UC-hypothesis / escalation /
   skill / CaseSpec edit anywhere.
3. NO rubric widening (§1.7 / §5.4); this sub-sprint does not change
   what counts as success for the bot.
4. Drift-guard keyword set is declarative measurement (the audit's
   first-party catalogue of agent-voice markers), not a routing
   semantic.
5. **A.6 NOT in scope.** Bot self-diagnoses sim drift in its own
   reasoning field (`audit §2.6`), runtime ignores. Track as OQ-S76.A6.
   If S-Auto-21 ships clean, A.6 may dissolve; otherwise M-Auto-6
   candidate.

## 6. Hard fences / STOP conditions

- Edit ONLY: `eval_interactive/eval_interactive/simulator/user_simulator.py`
  and `eval_interactive/tests/test_user_simulator.py`. No other code path.
- DO NOT kill `m-auto-5-baseline-20260605` (still running) — it is
  forensic data; let it complete.
- DO NOT launch the full M-Auto-5 re-bless yourself — human-gated (§7).
- DO NOT launch the held S-Auto-17 validation overnight.
- DO NOT open S-Auto-18 or any M-Auto-4 work.
- DO NOT touch audit Clusters B and C (routed to M-Auto-6).
- DO NOT edit any CaseSpec, `case_spec_overrides.yaml`, FAQ, prompt
  template (other than the simulator's own system prompt), judge config.
- DO NOT touch the autoloop 5-file scoring SHA-locked set.
- All eval / re-render only on a clean committed tree.

## 7. Test / eval requirements

- `eval_interactive/tests/test_user_simulator.py`: T1-T7 green.
- `eval_interactive` pytest under `uv run pytest`: no regression vs
  `524 passed` (S-Auto-20 baseline). New tests increment.
- Java: untouched. Confirm `1232 / 1 / 0 / 2` unchanged.
- autoloop pytest: untouched. Confirm `324` unchanged.
- Bad-case re-render keyword sweep contamination = 0 (or fully
  classified residual).

## 8. §7 Layer-classification + anti-hardcode stanza (embedded)

**Target failure layer:** `infra` — eval simulator correctness
(`eval_interactive/.../simulator/user_simulator.py` + the system prompt
+ the contract test file). NO `semantic_planner` / `prompt_projection` /
`skill_state` / `eval_spec` / `judge_calibration` / `product_policy` /
`java_guard` edit.

**Tier-0 invariant:** adds none; preserves all Tier-0 families at
current strictness.

**Semantic hardcode:** none. The drift-guard keyword set is the audit's
first-party catalogue of agent-voice markers (declarative measurement,
same character as the S-Auto-19 `noreply@gumtree.com` PII relaxation),
not a content-matching routing semantic rule on the bot side.

**Generalization coverage:** measurement-infra sub-sprint — evidence is
(a) T1-T7 contract tests (mock, wiring), (b) #6a keyword-sweep on real-
LLM re-rendered bad-case traces (0 contamination expected), (c) #6c
same-case-cross-time qualitative diff, (d) #6d `simulator_drift_blocked`
audit. NOT target/neighbor/negative/shadow case-family counts.

## 9. Codex review plan (§4.3, embedded)

PER-SUB-SPRINT RECOMMENDED; folds into M-Auto-5 milestone-shared close.
NOT a fence-#13 SHA trigger.

Codex focus (in priority order):

1. No semantic-side rubric widening / no bot or runtime edit.
2. Drift-guard keyword set is declarative measurement, not routing
   semantic.
3. Anti-误杀: guard cannot loop a legitimate customer turn; the
   3-attempt budget + `simulator_drift_blocked` escape are in place;
   #6d audit shows the escape is rare and inspected.
4. §5.7 evidence gate: mock tests cover wiring; real-LLM re-rendered
   traces are the gate on whether the simulator is fixed.
5. A.6 (bot self-diagnoses sim drift; runtime ignores) is OQ-S76.A6
   carry, not a stealth scope expansion.

## 10. Handoff requirements (write `docs/sprints/sprint-076-handoff.md`)

MUST include:

- §0 cold-start summary (sub-sprint, commits, test counts, what
  shipped / what NOT shipped).
- §1 role-map diff (file:line before/after) + the 4-turn transcript
  walkthrough; the persona re-anchor text; the negative-form block
  diff.
- §2 drift-guard implementation (detector code refs, keyword set in
  full, regurgitation algorithm + threshold, leakage probe set,
  retry-and-escape control flow).
- §3 T1-T7 file:line + one paragraph per test on assertion + mock
  construction.
- §4 bad-case re-render evidence (4a keyword-sweep table; 4b
  spot-check; 4c same-case-cross-time; 4d
  `simulator_drift_blocked` audit).
- §5 §5.7 self-classification (mock vs real-LLM evidence).
- §6 re-bless-ready command (exact command, output dir, preconditions,
  monitor/abort criteria).
- §7 STOP confirmations (`baseline_dir` NOT moved;
  `m-auto-5-baseline-20260605` NOT killed; full re-bless NOT run;
  S-Auto-17 overnight / S-Auto-18 NOT touched; B+C NOT touched).
- §8 OQ-S76.A6 (bot self-diagnoses sim drift; runtime ignores).

## 11. Code anchors (on demand)

- `eval_interactive/eval_interactive/simulator/user_simulator.py:21-49`
  — system prompt template (#3).
- `eval_interactive/eval_interactive/simulator/user_simulator.py:177-198`
  — `generate_next` message assembly (#1 role map at :187; #1 fallback
  at :191-192; #2 re-anchor inserted before :195-198).
- `eval_interactive/eval_interactive/simulator/user_simulator.py:202+`
  — `_call_llm` retry mechanism (#4 drift detector hooks here).
- `eval_interactive/tests/test_user_simulator.py` — extend with T1-T7
  (#5).
- `eval_interactive/eval_interactive/simulator/session_runner.py:182-192`
  — transcript role labels (verify-only; the inversion is downstream;
  do NOT edit this file).
- `eval_interactive/case_specs/bad_cases/` — re-render corpus (#6).

## 12. Commit discipline (embedded)

- `git add` only authorized files (no `-A`):
  `eval_interactive/eval_interactive/simulator/user_simulator.py`,
  `eval_interactive/tests/test_user_simulator.py`,
  `docs/sprints/sprint-076-handoff.md`.
- One commit per substantive step where practical (#1 role map, #2/#3
  prompt edits, #4 drift guard, #5 contract tests).
- The re-render output (`m-auto-5-baseline-YYYYMMDD-simfixed/`) is
  gitignored — reference in handoff, do not commit.
- Run eval / re-render only on a clean committed tree.
- Deliver-agent + human commit the deliver-owned docs separately.

## 13. Self-check checklist

- [ ] Role-map inversion at `user_simulator.py:187` + line-191 fallback;
      4-turn transcript walkthrough in handoff §1.
- [ ] Per-turn persona re-anchor message present before the terminal
      "generate ... JSON" prompt.
- [ ] Negative-form block added to `_SYSTEM_PROMPT_TEMPLATE` (≥ 6
      prohibitions).
- [ ] Drift guard (D1 + D2 + D3) implemented; 3-attempt retry budget;
      `simulator_drift_blocked` escape end-of-session.
- [ ] T1-T7 green; eval pytest no regression vs 524.
- [ ] Bad-case suite re-rendered; §4a keyword sweep contamination = 0
      or fully classified; §4b/§4c/§4d evidence in handoff.
- [ ] Java `1232 / 1 / 0 / 2` unchanged; autoloop pytest `324` unchanged.
- [ ] `baseline_dir` NOT moved; `m-auto-5-baseline-20260605` NOT killed.
- [ ] Full M-Auto-5 re-bless NOT run (re-bless-ready in handoff §6).
- [ ] S-Auto-17 overnight NOT launched; S-Auto-18 NOT opened; M-Auto-6
      (audit Clusters B + C) NOT touched.
- [ ] Handoff written per §10.
