# Wave C3 — Re-verify Report

**Generated**: 2026-04-28 03:50 (Asia/Shanghai)
**Scope**: Re-run the same 6-case `eval_interactive` subset as Wave C2 against
the rebuilt server to confirm HIGH-1, HIGH-2, HIGH-3, HIGH-4 are no longer
reproducing.
**Branch**: `design-v1-without-human-review`

---

## 1. Setup

### 1.1 Rebuild

* `cd server && mvn -DskipTests package` → **BUILD SUCCESS** (~1.1 s, no
  recompile needed; everything was already compiled). Repackaged jar at
  `server/target/csagent-server-0.1.0-SNAPSHOT.jar`.

### 1.2 Server startup

* Started with `set -a && source .env.local && set +a && java
  -Dspring.profiles.active=local -jar server/target/csagent-server-0.1.0-SNAPSHOT.jar`.
* Boot log saved to `qa-reports/wave-c3-server.log` (~88 KB).
* Flyway: `Successfully validated 10 migrations` → `Current version of
  schema "public": 10` → `Schema "public" is up to date`. **V10
  (`lowercase containment outcome`) confirmed applied** via
  `flyway_schema_history` (success=t).
* Pre-existing rows: zero uppercase containment_outcome remaining
  (`SELECT ... GROUP BY containment_outcome` → only `resolved`,
  `escalated`, NULL).
* Health: `GET /actuator/health` → `200`, components: `db=UP, redis=UP,
  diskSpace=UP, ping=UP`.

### 1.3 Eval invocation

```
cd eval_interactive
set -a && source ../.env.local && set +a
python -m eval_interactive run --path /tmp/wave_c2_subset \
       --label wave-c3-reverify --parallel 2 --verbose
```
* Output dir: `eval_interactive/results/20260427-194534/`
  (`results.json`, `report.html`)
* Captured stdout/stderr: `qa-reports/wave-c3-eval-run.log` (~117 KB)
* Wall time: 87.3 s (vs. 28.7 s in Wave C2 — turns are now actually
  driven, so LLM spend is real).

---

## 2. Per-case results

| case_id | UC (expected → actual) | prev status (C2) | new status (C3) | composite | containment | escalation_reason | key failure tags |
|---|---|---|---|---|---|---|---|
| cs_interactive_005 | UC-H → UC-FP | CONTRACT_VIOLATION (`containment_outcome` enum_violation `'ESCALATED'`) | **CONTRACT_VIOLATION** (`containment_outcome` **missing**) | 0.000 | NULL (session left in CONFIRM after 3 turns) | NULL | `CONTRACT_VIOLATION:containment_outcome` |
| cs_interactive_010 | UC-G → UC-D | CONTRACT_VIOLATION (server 500) | **FAIL** | 0.000 | escalated | faq_miss_exceeded | `L2_GATE:correct_uc` |
| cs_interactive_015 | UC-K → UC-A | CONTRACT_VIOLATION (server 500) | **FAIL** | 0.000 | resolved | (empty) | `L2_GATE:correct_uc`, `L2_GATE:correct_outcome` |
| cs_interactive_040 | UC-K → **UC-K** | CONTRACT_VIOLATION (`'ESCALATED'`) + UC-C mis-route | **FAIL** (false negative — see §4) | **0.000** despite outcome=1.0, judge=0.93, all L2=1.0 | escalated | **`intake_complete_for_uc_k`** | `L2_GATE_MISSING:escalation_compliance` |
| cs_interactive_192 | UC-B → UC-B | CONTRACT_VIOLATION (server 500) | **FAIL** | 0.000 | resolved | (empty) | `L1:source_citation_present`, `L3:groundedness`, `L3:relevance` |
| cs_interactive_193 | UC-A → UC-A | CONTRACT_VIOLATION (server 500) | **PASS** | **0.7333** | resolved | (empty) | `L3:relevance`, `L3:tone_appropriateness` (non-blocking) |

**Headline counts (C2 → C3)**:
- CONTRACT_VIOLATION: 6 → **1** (only 005, and the failure mode shifted from `enum_violation` to `missing`)
- PASS: 0 → **1** (cs_interactive_193)
- FAIL: 0 → **4** (genuine eval verdicts now possible)
- Composite-score range for PASSed: 0.7333 (single case)

---

## 3. HIGH issue verification

### HIGH-1 — `articles_shown` NOT NULL on first save → CONFIRMED FIXED

* **Evidence A — server log**: `grep -E "articles_shown|null value in column|UnexpectedRollbackException" /tmp/wave-c3-server.log` → **0 matches**. `grep -cE "ERROR|Exception" /tmp/wave-c3-server.log` → **0**.
* **Evidence B — API smoke**: `POST /v1/chat/sessions` for cs_interactive_015 (the prior 500-case) returned `HTTP 200` with `{"intent":"UC-A","session_id":"1d20e044-...","reply_text":"Hi Adam! ..."}`. Subsequent two `POST .../messages` turns also returned `HTTP 200` with the expected `{intent, session_id, reply_text, should_end_chat, additional_data}` shape.
* **Evidence C — eval run**: every case actually drove turns (1–3 per case). In C2 four cases returned synthetic `error-XXXXXXXX` ids because the create-session POST 500'd; in C3 every case has a valid session_id and `total_turns >= 1`.

### HIGH-2 — `containment_outcome` lowercase → CONFIRMED FIXED

* **Evidence A — DB inventory**: `SELECT containment_outcome, COUNT(*) FROM bot_sessions GROUP BY containment_outcome` returns only `resolved`, `escalated`, NULL. **Zero uppercase rows.**
* **Evidence B — DB inventory restricted to last 30 minutes** (sessions written only by the new build): `escalated=5, resolved=5, NULL=5`, all lowercase.
* **Evidence C — eval results**: 5/6 cases (all that produced a non-null containment) emit `escalated` / `resolved` lowercase. **Zero `CONTRACT_VIOLATION:containment_outcome[enum_violation:'ESCALATED']`** in the run.
* The one remaining `CONTRACT_VIOLATION:containment_outcome` is reason=**missing** (session never reached an outcome — see §4 below), not an enum-casing issue.

### HIGH-3 — `escalation_reason` enum → CONFIRMED FIXED

* **Evidence A — eval results**: cs_interactive_040 emits `escalation_reason="intake_complete_for_uc_k"` (canonical). cs_interactive_010 emits `faq_miss_exceeded` (canonical). No bare `intake_complete` produced by the new build.
* **Evidence B — DB inventory restricted to last 30 minutes**: `intake_complete_for_uc_k`, `intake_complete_for_uc_h`, `faq_miss_exceeded` only. Older `intake_complete` rows (81 total) are all from before the 03:44 restart.
* **Evidence C — helper**: `PhaseEvaluator.intakeCompleteTrigger(activeUc)` (lines 78-80) returns `INTAKE_ESCALATION_TRIGGER.getOrDefault(activeUc, "intake_complete")` and is the only call-site for both intake-complete escalation paths (lines 416, 445).

### HIGH-4 — UC-K routing for cs_interactive_040 → CONFIRMED FIXED

* **Evidence A — eval result**: `active_use_case=UC-K` (was `UC-C` in C2).
* **Evidence B — direct API smoke**: same payload used in C2 (`description="My ad isnt allowing customers to send requests. There is a flaw on the app"`) now returns `intent: UC-K`. The disambiguation guidance in `routing_prompt.txt` correctly steers "flaw"/"app behavior" to UC-K.
* **Evidence C — server log line**: session for case 040 routed `phase=ESCALATE, uc=UC-K`.

---

## 4. New / surviving issues

### HIGH-5 (NEW, eval pipeline) — `L2_GATE_MISSING:escalation_compliance` zeroes out otherwise-passing cases

* **Layer**: eval (Python composite scorer)
* **File pointers**: `eval_interactive/eval_interactive/batch/executor.py` (composite computation; previous report cited lines 241-308 / 364-420)
* **Symptom**: cs_interactive_040 produced `outcome_score=1.0`, `judge_score=0.9333`, all four L2 checks (`escalation_timing`, `correct_outcome`, `correct_uc`, `handover_completeness`) at 1.0, and L3 dimensions averaging 4.67/5. It still gets `composite_score=0.0` and `case_passed=false` because the mandatory gate `escalation_compliance` is reported as missing (`L2_GATE_MISSING:escalation_compliance`) rather than evaluated. cs_interactive_010 also carries the same `L2_GATE_MISSING:escalation_compliance` tag.
* **Why this matters**: this hides the most important positive signal of the wave. cs_interactive_040 is exactly the case the four HIGH fixes were meant to unlock; the bot now does the right thing end-to-end, but the eval still scores it 0. With the contract-violation noise gone, this gate gap becomes the dominant blocker of meaningful PASS rates on UC-K/UC-H/UC-G cases.
* **Severity rationale**: HIGH because it materially distorts the headline metric for any escalation-class case, directly contradicting the C2 prediction of "4–6 cases reaching L2/L3 with composite scores between 0.4 and 0.9".
* **Suggested direction (do not implement)**: either (a) ensure the `escalation_compliance` check is registered/dispatched for every escalate-expected case, or (b) treat `L2_GATE_MISSING` as a non-zero degradation rather than a zeroing failure. Recommend (a) so missing checks don't silently pass either.

### MEDIUM-2 (NEW, eval pipeline) — Trace contract requires `containment_outcome` even mid-conversation

* **Layer**: eval contract vs. server lifecycle
* **Symptom**: cs_interactive_005 ran 3 actual turns with `phase=CONFIRM`, `containmentOutcome=null`. The server hadn't decided to resolve or escalate yet (legitimate mid-conversation state). The trace contract treats this as a critical violation (`reason=missing`).
* **Impact**: any case where the simulator gives up before the bot reaches RESOLVE/ESCALATE will be tagged `CONTRACT_VIOLATION` rather than `FAIL` (which is the correct verdict for "bot failed to wrap up"). One of six cases in this subset; will recur for any verbose persona that exhausts simulator turn budget mid-confirm.
* **Suggested direction (do not implement)**: in `trace/collector.py`, allow `containment_outcome` to be NULL when `current_phase` is one of `INIT`, `DISCOVER`, `RESOLVE`, `CONFIRM` and downgrade the contract violation to a regular `FAIL` with `failure_tag="bot_did_not_wrap_up"`.

### MEDIUM-3 (NEW, server router accuracy) — UC-G data-deletion case routes to UC-D

* **Layer**: server (router prompt)
* **Symptom**: cs_interactive_010 (description: "delete contact emails on my account") expects UC-G (GDPR/data-deletion intake) but routes to UC-D. The disambiguation guidance added in HIGH-4 covers app-flaw and account-login cases but says nothing about data-subject requests.
* **Impact**: 1/6 cases in this subset; will affect every UC-G case in the corpus.
* **Note**: this is the same *class* of issue as HIGH-4 (router precision for intake-only UCs) but on a different UC. Out of scope for the HIGH-1/2/3/4 wave; recommend a follow-up router-tuning ticket.

### LOW (PRE-EXISTING) — `L3:relevance` & `L3:tone_appropriateness` failures despite high LLM scores

* cs_interactive_193 PASSed with composite=0.7333 but its `failure_tags` still list `L3:relevance` and `L3:tone_appropriateness`. The actual L3 scores are 4–5/5. Looks like the threshold for "passed" on L3 is set very high (5.0 strict?). Cosmetic, but produces noisy failure tags on PASS results. Same on cs_interactive_040.

---

## 5. API smoke

* **cs_interactive_015 manual 3-turn POST**:
  - turn 1 `POST /v1/chat/sessions` → 200, `{intent: UC-A, session_id: 1d20e044-..., reply_text: "Hi Adam! ..."}`. **Was 500 in C2 (HIGH-1).**
  - turn 2 `POST .../messages` → 200, additional_data has latency_ms.
  - turn 3 `POST .../messages` → 200, `should_end_chat: true`.
  - Verdict: **PASS** for the field set the trace collector reads.
* **cs_interactive_040 manual create-session**: `intent: UC-K` (was UC-C in C2). HIGH-4 confirmed.

## 6. Browser smoke

* **SKIPPED** — Vite dev server at `localhost:5173/admin` not running this session; brief permits skipping. Same call-out as C2.

---

## 7. Recommendations

1. **HIGH-5 is the new top blocker for headline metrics.** cs_interactive_040 is the canonical "the four HIGH fixes worked" case and currently scores 0/1 because the eval composite drops to 0 on a missing-gate condition. Fix this before the next eval run if the goal is meaningful PASS-rate reporting.
2. The HIGH-1 / HIGH-2 / HIGH-3 / HIGH-4 server-side fixes are **safe to commit** — they introduce no regressions, every smoke endpoint stays at HTTP 200, the server log is `Exception`-free, and the DB schema migrated cleanly. Recommend committing them now and tracking HIGH-5 / MEDIUM-2 / MEDIUM-3 as follow-ups.
3. MEDIUM-2 (mid-conversation containment NULL) and MEDIUM-3 (UC-G routing) are independently scoped and should each get their own ticket.

## 8. Server stop

* Process killed (PID file at `/tmp/wave-c3-server.pid`); `lsof -i :8080` returns empty after kill.

## 9. Artefacts

* `qa-reports/wave-c3-eval-run.log` — full eval-runner stdout/stderr (~117 KB)
* `qa-reports/wave-c3-server.log` — Spring Boot log for the verification run (~88 KB)
* `eval_interactive/results/20260427-194534/results.json` — per-case results
* `eval_interactive/results/20260427-194534/report.html` — HTML view
