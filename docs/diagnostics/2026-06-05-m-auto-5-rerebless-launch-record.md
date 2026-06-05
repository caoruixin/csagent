---
title: 2026-06-05 M-Auto-5 authoritative re-re-bless launch record (corrected framework; evidence generation #2; NOT M-Auto-5 close)
doc_tier: diagnostic
status: diagnostic
implementation_status: historical
source_of_truth: this file (launch record only — outcome lives in `eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix/`)
last_reviewed: 2026-06-05
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sibling to `docs/diagnostics/2026-06-05-m-auto-5-rebless-launch-record.md`
  (the S-Auto-21 simfixed re-bless launch record). The two are NOT siblings in
  the M-Auto-5 close path — the prior launch record's run is the simfixed
  forensic baseline; this launch record's run is the authoritative re-re-bless
  on the S-Auto-22 corrected framework.

  Single-purpose record. Captures the exact launch conditions for the
  authoritative re-re-bless of the M-Auto-5 multi-suite corpus on the
  corrected framework (S-Auto-22 + bookkeeping). This is the evidence the
  M-Auto-5 milestone-close paired-evidence review reads.

  This is NOT the M-Auto-5 close. M-Auto-5 close = §5.9 pre-flight sweep
  validation (the OQ-S77 sweep returns ZERO) + paired-evidence review +
  milestone-shared Codex + `baseline_dir` pointer move + archive sweep; it
  happens AFTER this run completes and the validation + reviews pass. The
  pointer is NOT moved by the act of running this; the deliver-agent moves
  it only after the close-evidence reviews classify the run as acceptable.

  Differs from the prior (2026-06-05 simfixed) launch record in three
  material ways: (1) HEAD includes S-Auto-22 dev work + deliver bookkeeping;
  (2) backend MUST be rebuilt because `server/` was edited (#4 runtime stamp
  downgrade); (3) the §5.9 pre-flight sweep on the output is the OQ-S77
  validation gate, not just an interpretation aid.
---

# 2026-06-05 — M-Auto-5 authoritative re-re-bless launch record (corrected framework)

## 1. Framing — what this is and what it is not

**This is**: the human-launched authoritative multi-suite re-bless of the
M-Auto-5 corpus on the **S-Auto-22 corrected framework**. It generates the
authoritative baseline + the §5.9 pre-flight sweep validation gate
evidence that the M-Auto-5 milestone-close review reads.

**This is not**:
- The M-Auto-5 milestone close (that's paired-evidence + Codex + pointer
  move + archive sweep AFTER this run).
- A re-render of the S-Auto-21 simfixed run (that completed 2026-06-05,
  retained on disk at `eval_interactive/results/m-auto-5-baseline-20260604-
  simfixed/` as **forensic-only** per the 2026-06-05 simfixed launch
  record §9).
- A bot-capability evaluation (M-Auto-5 corrects measurement; bot is
  byte-identical to the S-Auto-21 build modulo S-Auto-22 #4 trace-contract
  persistence, which is a server-side trace persistence correction, not a
  decision change).

## 2. Launch parameters

| Field | Value |
|---|---|
| Branch | `auto-loop-branch` |
| HEAD SHA at launch | the latest deliver-bookkeeping commit (after the S-Auto-22 close bundle; HEAD will be reported in the post-run §9 along with the dev's `2ea65de` commit being included by ancestry). MUST include `2ea65de` (S-Auto-22 dev) in HEAD's ancestry. |
| Working tree | MUST be clean before launch (see §3 preconditions). |
| Bot code base | INCLUDES S-Auto-22 #4 runtime trace-contract change (`server/.../runtime/ControlKernel.java` + `server/.../model/BotSession.java`) → the existing `45618df`/`1bc77c1`/`9e4907e`/`e5b0d27`/`0bc37e2` build from the S-Auto-21 simfixed run is **STALE**. Backend MUST be rebuilt before launch (see §3.3). |
| Eval scoring base | INCLUDES S-Auto-22 #1+#3 composite.py override block + #2 hard_checks.py terminal-failure check. autoloop 5-file scoring SHA-locked set untouched. |
| Output dir | `eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix` (fresh dated dir mirroring the simfixed naming; old baselines RETAINED). |
| Sampling | `--n 7` (matches the S-Auto-21 simfixed run; deterministic per-attempt at `simulator_temperature=0.0`; the script may write up to 9 attempt dirs a0-a8 with internal retries, observed on the prior run). |
| `baseline_dir` config | UNCHANGED — stays `m-auto-4-baseline-20260604` until post-review pointer move. |

### Exact command

```bash
cd autoloop && uv run python scripts/rebless_baseline.py \
    --n 7 \
    --out-dir ../eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix
```

Source: copied verbatim from `docs/sprints/sprint-077-handoff.md` §6
(the dev-authored re-re-bless-ready command, identical to the S-Auto-21
launch command modulo the output dir).

## 3. Preconditions (verify each before launch)

1. **Clean working tree** — `git status` shows no staged or unstaged
   changes. Per `project_autoloop_dirty_index_hazard`, a dirty index
   poisons per-exp commits. Any in-progress edits must be committed or
   stashed before launch.
2. **HEAD ancestry includes `2ea65de`** — the S-Auto-22 dev commit. The
   deliver bookkeeping commit that opens this launch record sits on top.
3. **Backend REBUILT and booted fresh** — S-Auto-22 #4 modified `server/`
   (`ControlKernel.java` + `BotSession.java`). The existing backend (last
   rebuilt for the S-Auto-21 simfixed run) is stale and would not exhibit
   the void/downgrade behavior. Run:

   ```bash
   # Build (no tests; trust the dev's mvn test 1244/1/0/2 from handoff)
   cd server && mvn -o -DskipTests package
   # Stop any existing backend on :8080 (mvn spring-boot:run process)
   # then boot fresh:
   cd server && mvn -o spring-boot:run -Dspring-boot.run.profiles=local
   # in another shell:
   curl -s localhost:8080/actuator/health   # expect status:UP (db/redis/disk/ping all UP)
   ```

   The §1 framing note "bot is byte-identical to the S-Auto-21 build modulo
   trace-contract persistence" means decision behavior is unchanged; what
   changed is what gets persisted on terminal-failure shapes (a stamp gets
   voided rather than left stale). Without rebuilding, the re-re-bless
   would NOT exhibit Fix #4 and would not be the corrected-framework
   authoritative baseline.
4. **Postgres + Redis up** — Postgres at `localhost:5432`; Redis at
   `localhost:6379`. Bot LLM creds in repo-root `.env.local`; simulator
   LLM creds in `autoloop/.env.local`.
5. **Mac kept awake** — per `feedback_long_llm_run_no_sleep`. Wrap with
   `caffeinate -dimsu`:

   ```bash
   caffeinate -dimsu &   # PID for cleanup later
   # ...launch re-re-bless...
   ```
6. **Disk space** — multi-suite n=7 produces ~3 × 7-9 × per-case JSON =
   hundreds of MB. Confirm `eval_interactive/results/` has headroom.

## 4. Forensic-baseline policy (cumulative)

Four pre-existing baseline dirs MUST be retained and treated as
forensic-only — do NOT consume their stability classifications, pass
rates, or judge scores as input to any downstream sprint:

- `eval_interactive/results/m-auto-4-baseline-20260604/` — the live
  `baseline_dir`. Retained as the pre-fix comparator.
- `eval_interactive/results/m-auto-5-baseline-20260604/` — the
  S-Auto-19 re-bless scratch. Buggy simulator + S-Auto-19 stamp inert.
- `eval_interactive/results/m-auto-5-baseline-20260605/` — the running
  forensic from 2026-06-04. Buggy simulator.
- `eval_interactive/results/m-auto-5-baseline-20260604-simfixed/` —
  the S-Auto-21 simfixed run (2026-06-05). Clean simulator + S-Auto-20
  stamp + S-Auto-21 simfix, BUT exposed the OQ-S77 stall-not-gated
  gap. Per §9 of the prior launch record: forensic-only.

The new dir produced by this launch (`m-auto-5-baseline-20260604-
simfixed-stalledfix/`) is the **authoritative target**. The
`baseline_dir` config pointer is moved to it ONLY after:

1. The §5.9 pre-flight sweep returns ZERO matches (§5 below).
2. Paired-evidence review against the bad-case suite passes.
3. Milestone-shared Codex (§4.1) passes (with the two S-Auto-22
   deviations carrying for explicit Codex verdict).

## 5. §5.9 pre-flight sweep — the OQ-S77 validation gate (post-run, BEFORE paired-evidence)

The vacuous-pass + terminal-failure fingerprint sweep is described in
`docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md` §6
step 6. Run it on the new output dir AFTER the re-re-bless completes
and BEFORE the paired-evidence review.

Reference implementation (Python; copy into a scratch script or run
inline):

```python
import json, os
OUT = "eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix"
attempts_dir = f"{OUT}/_rebless_scratch/_attempts"
TERMINAL_FAILURE = {"loop_detected", "goal_impossible", "error",
                    "contract_violation", "max_turns_exceeded"}
hits = []
for a in sorted(os.listdir(attempts_dir)):
    for suite in ("bad_cases", "anchor_outcome", "shadow"):
        f = f"{attempts_dir}/{a}/{suite}/results.json"
        if not os.path.exists(f): continue
        data = json.load(open(f))
        for c in (data.get("case_results") or []):
            comp = c.get("composite_score", 0) or 0
            l2 = c.get("l2_results") or []
            tags = c.get("failure_tags") or []
            sr = (c.get("stop_reason") or "").lower()
            if (c.get("case_passed") and comp == 0 and not l2 and
                (any(t.startswith("STALL:") for t in tags) or sr in TERMINAL_FAILURE)):
                hits.append((suite, a, c.get("case_id"), sr, tags))
print(f"Total hits: {len(hits)}")
for h in hits[:20]:
    print(" ", h)
```

**Validation gate**:

- **Expected ZERO hits**. The S-Auto-22 fixes (#1 + #2 + #3) close the
  fingerprint at the gate; a zero count proves the gate gap is closed
  at corpus scale, not just on a synthetic fixture.
- **A non-zero count** means the structural fix didn't cover some
  configuration the corpus exercises. Halt; do NOT proceed to paired-
  evidence review. Open S-Auto-23 fix-iteration; root-cause the missed
  configuration; re-fix; re-re-re-bless. (The prior simfixed run
  showed 12 hits on the same fingerprint, so we have a concrete
  before/after benchmark.)
- The dev's deterministic re-score on the simfixed scratch (handoff
  §5) showed 12 vacuous draws + 1 correct extra all flip to FAIL,
  matching expectations, so we expect this sweep to return ZERO on
  the real-LLM re-re-blessed corpus.

If ZERO: OQ-S77.stall-not-gated is officially **CLOSED**; framework-
defect priority (§5.8) lifts; proceed to paired-evidence review.

## 6. Expected anomalies (interpret, do not treat as gaming)

Same as the 2026-06-05 simfixed launch record §5, with two updates:

- **`suspect_baseline_manipulation` will likely fire** — same reason
  as the simfixed run (the S-Auto-22 corrections shift the verdict
  distribution further), now also reflecting the vacuous-pass fixes
  removing 12 false-pass draws. Read as **"the S-Auto-22 corrected
  measurement expected distribution shift"**, not gaming.
- **Some F→P flips from the simfixed run will SOFTEN** — cs095 stable
  1.00 → ~0.56 reducible-flaky; uc_a_visibility stable 1.00 → ~0.89
  (still PASS); uc_fp_removed stable 0.89 → ~0.67 (still PASS). The
  dev's deterministic re-score predicts these exactly. The verdict-
  shift comparison vs `m-auto-4-baseline-20260604` should match
  `dev_handoff §5 table` modulo small real-LLM variance.
- **csmp_s01 a-attempt may flip to FAIL on a `loop_detected + resolved
  + composite>0` draw** — the one correct extra the dev's re-score
  identified. csmp_s01 was already majority-FAIL 0.375; expect ~0.25
  on the re-re-bless.
- **Judge layer still excluded from paired-evidence review** per
  OQ-S76.judge-zero resolution; canonical signal = composite + outcome
  + L1 + L2 `failure_tags`. Close write-up cites the four-run table at
  `action_bank.md` `R-eval-interactive-judge-score-never-populated`.
- **Shadow's cs59s* errors will persist** — 18/198 errors (9.6%)
  expected, entirely Cluster C.1 known issue (cs59s01/02 session_create
  400). Not a regression; M-Auto-6 candidate.
- **Persistent failures preserved (no over-correction)**: anchor
  uc_g/h/i/j stable 0.000; shadow cs38s scam+harassment stable 0.000;
  cs15s*, cs76s*, cs92s*, cs32s* etc. unchanged from m-auto-4.

## 7. Post-run procedure (do NOT move the pointer until §5 sweep + paired-evidence + Codex all pass)

1. **Confirm the run completed** — check
   `out-dir/_rebless_report.json` for the run-level summary; per-suite
   `out-dir/<suite>/aggregated.json` for completeness. If the run was
   sleep-spanned or aborted, kill + re-launch on a clean tree, do NOT
   bless a partial run.
2. **§5.9 pre-flight sweep** (§5 above) — the OQ-S77 validation gate.
   Expected ZERO. If non-zero, open S-Auto-23.
3. **If sweep passes**: Deliver+human paired-evidence review (§5.6
   primary gate) on the bad-case suite. Verdict-distribution shift vs
   `m-auto-4-baseline-20260604` must be explainable by the four
   columns (eval-read S-Auto-19 / runtime-stamp S-Auto-20 / simulator
   S-Auto-21 / vacuous-pass-gate S-Auto-22); no unexplained verdict
   flip. The dev's handoff §5 table is the prior reference for the
   F→P-flip magnitudes; small real-LLM variance is expected.
4. **Milestone-shared Codex review** (§4.1 nine-question anti-hardcode
   kernel) over the M-Auto-5 cumulative commit range (S-Auto-19
   through S-Auto-22 + all deliver bookkeeping). The two S-Auto-22
   deviations carry into the review explicitly for Codex's verdict.
   Deliver authors `compact/M-Auto-5-review-prompt.md` (self-contained
   per prompt-artifact-rules §9.1/§9.2) when the §5 sweep + paired-
   evidence both pass.
5. **If all gates pass**: deliver moves `config.fitness.baseline_dir`
   from `m-auto-4-baseline-20260604` to `m-auto-5-baseline-20260604-
   simfixed-stalledfix`; flips `docs/current_eval_baseline.md`
   `implementation_status` to `current`; executes standard milestone-
   close archive sweep (objective + codex-review →
   `docs/milestones/M-Auto-5_*`; reset placeholders; §1 truncation per
   `doc_governance.md` retention rule; §2 archive index row;
   `action_bank.md` §7.1 retention sweep).
6. **If either review fails**: do NOT move the pointer. Classify per
   deliver-agent role §Milestone close (fix-iteration / out-of-scope /
   in-flight downgrade); open the appropriate sub-sprint.

## 8. Explicit non-gating follow-ups (NOT preconditions for this re-re-bless)

Confirmed cumulative through 2026-06-05; none gate the launch:

- **Audit Cluster B + C** — routed to M-Auto-6 (parallel research-
  agent dispatch after M-Auto-5 close).
- **OQ-S76.A6** — reassess at M-Auto-5 close; reasonable bet to
  dissolve given S-Auto-21 simulator clean + S-Auto-22 gate clean.
- **OQ-S76.drift-stop** — label fidelity only; NOT a blocker.
- **OQ-S76.judge-zero** — resolved 2026-06-05 (route c, collapsed
  into chronic `R-eval-interactive-judge-score-never-populated`).
- **OQ-S77.stall-detector-window** — new from S-Auto-22 dev;
  stall_detector window-tuning question; M-Auto-6 or later candidate;
  NOT a re-re-bless blocker (Fix #1 scoping handles it on this corpus).
- **OQ-S77.goal-impossible-resolved-evidence** — new from S-Auto-22
  dev; `eval_spec` deferred consistent with S-Auto-20's own
  goal_impossible deferral; future eval-spec sub-sprint.
- **S-Auto-18 (M-Auto-4 §5 escalation-family tiers)** — remains
  deferred behind M-Auto-6.

## 9. Outcome classification (filled in AFTER the run completes)

To be appended by the deliver-agent after the human-launched re-re-
bless completes + the §5.9 sweep runs + the paired-evidence + Codex
reviews progress. Sections to include (per the 2026-06-05 simfixed
launch record §9 pattern):

- §9.1 What this run PROVES (positive evidence) — incl. the §5
  sweep ZERO match.
- §9.2 What this run REVEALED (negative evidence) — if any.
- §9.3 Outcome classification (authoritative vs forensic).
- §9.4 Routing (close vs fix-iteration).
- §9.5 Pointer move + status flip record (if authoritative).

Until §9 is filled, the run is provisionally treated as "evidence
generation in progress; pointer NOT moved".
