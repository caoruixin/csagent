# Sprint 064 / S-Auto-9 / M-Auto-2 — Codex Per-Sub-Sprint Review Prompt

You are **Codex / Review Agent** for **Sprint 064 / S-Auto-9 / M-Auto-2 — Local-Mac OQ-S62.3 diagnostic + first overnight + first cherry-pick, sub-sprint 1 of 2-3 — Per-sub-sprint review per `iteration_governance.md` §4.3 trigger #3**. This sub-sprint underwent mid-sprint scope expansion via STOP-and-surface authorization (handoff §5 + §7); fence #20 (`autoloop/autoloop/sandbox/applier.py` controlled override on three narrow process-lifecycle / IO / connectivity infra fixes) was POST-HOC blessed at §8.3 in-place revision 2026-05-31, which triggers §4.3 trigger #3 → per-sub-sprint Codex REQUIRED at S-Auto-9 close (NOT deferrable to M-Auto-2 milestone-shared close).

**Cumulative scope claim**: review commits `875772f..HEAD` on `auto-loop-branch` (= 1 code-bearing commit: `e342d89` applier.py +35 −8 + handoff +294 + 7 diagnostic scripts under `scripts/sprint-064-*`). M-Auto-1C close-bundle commit `875772f` is the immediate predecessor and the established baseline.

This prompt is **self-contained per `iteration_governance.md` §9 invariant**. You do NOT need to read any repo doc beyond `AGENTS.md` (auto-loaded via constitution chain) + this prompt + the specific code anchors + the sub-sprint handoff named below. Do NOT edit code; do NOT re-judge bad-case manual review verdicts (none in scope for this sub-sprint); do NOT auto-PASS / auto-FAIL.

## Read order (minimal)

1. `AGENTS.md` is auto-loaded by Codex on session start; the constitution chain `doc_governance.md` → `agent_context_guide.md` → `iteration_governance.md` loads transitively. **Do NOT manually read** these.
2. This prompt.
3. **Sub-sprint handoff** (dev-authored; required for Axes A-J evidence): `docs/sprints/sprint-064-handoff.md` (294 lines; §0 summary table + §1 baseline reproduction + §2 per-hypothesis diagnostic evidence + §3 fix delivered + §4 smoke iter Step 9 verification + §5 NEW fence #20 controlled override + §6 OQs surfaced + §7 STOP-and-surface log + §8 M-Auto-2 close-readiness checklist).
4. **Code anchors** (read on demand during review):
   - `autoloop/autoloop/sandbox/applier.py` (focus on `_spawn_spring` ~lines 407-437 for OQ-S62.3 `start_new_session=True` + OQ-S64.1 stdout-to-log-file; `_probe_url_is_up` ~lines 473-485 for OQ-S64.2 `trust_env=False`).
   - `scripts/sprint-064-signal-trap-probe.sh` + `scripts/sprint-064-step2b-bash-launch.sh` + `scripts/sprint-064-step2c-dtrace-signal.sh` + `scripts/sprint-064-step4-smoke-iter.sh` + `scripts/sprint-064-step4b-spring-boot-timing.sh` + `scripts/sprint-064-step4c-spawn-repro.py` + `scripts/sprint-064-step4d-probe-env-test.sh` (7 diagnostic scripts under pure-infra carve-out; not load-bearing).
   - `docs/milestone_objective.md` (M-Auto-2 live; §6 17+3 fences post-§8.3 revision; fence #20 POST-HOC BLESSED entry).
   - `docs/milestones/M-Auto-1C_objective.md` (M-Auto-1C archived; §6 17+2 fences inherited; do NOT edit).

## Embedded milestone + sub-sprint context (verbatim from `docs/milestone_objective.md`)

### M-Auto-2 — Local-Mac OQ-S62.3 diagnostic + first overnight + first cherry-pick

**Milestone class** (per §1): multi-layer, 2-3 sub-sprints. S-Auto-9 = `infra` substrate-environment diagnostic + resolution + §7 EXEMPT (pure-infra carve-out self-walked); S-Auto-10 = `eval_spec` (first overnight + first cherry-pick on now-reliably-executable substrate). Codex review plan: **S-Auto-9 UPGRADED to PER-SUB-SPRINT per §4.3 trigger #3** (fence #20 post-hoc override; THIS REVIEW); S-Auto-10 milestone-shared default UNLESS cherry-pick candidate borderline-§5.3 surfaces.

**Goal** (per §2): finish what M-Auto-1C substrate-validated but didn't execute end-to-end. S-Auto-9 resolves OQ-S62.3 (the persistent "OS-level 2-5 min overnight kill" on local Mac) + retires M-Auto-1C §12.4 deferred gate #1 (live iter end-to-end through Step 9 with non-degenerate `tier_evaluator_verdict`); S-Auto-10 exercises the now-reliably-executable substrate end-to-end (first overnight + first cherry-pick).

**Sub-sprint S-Auto-9 / Sprint 064** (per §3, revised in-place per §8.3 2026-05-31):

> Sequence refinement 2026-05-31: original S-Auto-9 prompt anticipated fence #20 surface on `autoloop/autoloop/cli.py` or NEW files (e.g., a `launchd_wrapper.py` helper module) under the working hypothesis that OQ-S62.3 was an OS-level kill requiring alternate detachment mechanics. Mid-sub-sprint diagnostic investigation FALSIFIED sprint-063 §8's "OS-level kill" hypothesis: code read of `autoloop/autoloop/sandbox/applier.py` identified the kill as a **self-inflicted process-group suicide** (`_spawn_spring` did not give mvn its own session, so `_terminate_process`'s `os.killpg` targeted the autoloop itself). Dev surfaced via AskUserQuestion 2026-05-31 that the actual root-cause fix required editing `applier.py` (fence #18 FINALIZED at M-Auto-1C close). Human authorized fence #20 controlled override on the SAME file (S-Auto-7.2 fast-iteration precedent). Subsequently OQ-S64.1 (mvn stdout pipe deadlock) + OQ-S64.2 (macOS-system-proxy-routed health probe) were discovered during §4 smoke-iter verification + fixed under the same fence #20 authorization; each was narrated transparently to the human at the moment of application per user's fast-iteration preference. Cumulative 1-commit pattern landed (`e342d89` applier.py +35 −8 + handoff + 7 diagnostic scripts). Per §4.3 trigger #3 the fence #20 mid-sprint authorization UPGRADES Codex review plan from DEFAULT milestone-shared to **PER-SUB-SPRINT Codex review REQUIRED at S-Auto-9 close** — THIS REVIEW.

**Layer**: `infra` (substrate-environment diagnostic + resolution across one applier.py surface with three independent narrow fixes: process-group isolation + stdout-to-log-file + httpx proxy-bypass; structural; no semantic decision change; no projection / scoring semantic logic edit; no CaseSpec / judge change). **§7 stanza**: EXEMPT per pure-infra carve-out (self-walked for paper-trail; all three fixes are subprocess lifecycle / IO routing / connectivity plumbing; no semantic decision logic; no keyword / regex / enum / per-UC matrix).

### M-Auto-2 §6 17+3 hard fences (relevant subset embedded verbatim post-§8.3 revision 2026-05-31)

The M-Auto-2 §6 fence list extends M-Auto-1C §6 17+2 fences with ONE additional controlled override authorized at S-Auto-9 close. The relevant fences for S-Auto-9 review:

- **Fence #1**: No edits to `server/src/main/java/**`. (Verify cumulative diff empty.)
- **Fence #2**: No edits to `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`. M-Auto-1B S-Auto-7 controlled override on `loader.py` is FINALIZED.
- **Fence #3**: `server/src/main/resources/skills/*.yaml` writable EXACTLY ONCE via S-Auto-10 cherry-pick mechanism (NOT S-Auto-9).
- **Fence #13**: No modification of `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (locked at hash `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`).
- **Fence #14**: No hardcoding of specific eval case wording / user utterance / expected answer / case-status label in any detector rule or Skill YAML edit (D2 + §1.7).
- **Fence #15**: **No LLM call inside detector or content_validator OR applier surfaces** (per D1 regex-heuristic-only rule; M-Auto-1B/C baseline holds). The fence #20 fixes are subprocess / httpx plumbing, NOT LLM call additions — verify.
- **Fence #18 (M-Auto-1C era, FINALIZED outside fence #20 envelope)**: `autoloop/autoloop/sandbox/applier.py` byte-identical to M-Auto-1C S-Auto-7.2 close (post-OQ-S61.1 + OQ-S62.2 substrate fixes) **outside the narrow fence #20 envelope below**. S-Auto-9 authorized a follow-on controlled override per fence #20; fence #18's S-Auto-7.2-era surface otherwise remains in force.
- **Fence #19 (M-Auto-1C era, FINALIZED)**: `autoloop/autoloop/sandbox/content_validator.py` byte-identical to M-Auto-1C S-Auto-7.2 close (post-OQ-S62.1 rule 3 rewrite). S-Auto-9 MUST NOT edit. `autoloop/config.yaml` `length_overflow_absolute_ceiling` knob remains TUNABLE (currently 1200).
- **Fence #20 (NEW M-Auto-2; POST-HOC blessed at §8.3 in-place revision 2026-05-31)**: `autoloop/autoloop/sandbox/applier.py` writable in S-Auto-9 ONLY for THREE narrow process-lifecycle / IO / connectivity infra fixes (cumulative ~35 LOC at commit `e342d89`):
    - **OQ-S62.3** — `_spawn_spring` adds `start_new_session=True` to the `mvn` `subprocess.Popen` so mvn + its JVM descendants occupy their own session / process group; `_terminate_process`'s `os.killpg(os.getpgid(proc.pid), …)` therefore targets only mvn's subtree, eliminating the self-inflicted "OS-level 2-5 min kill" of the autoloop orchestrator that sprint-063 §8 had mis-attributed to jetsam / launchd / TAL / memory pressure.
    - **OQ-S64.1** — `_spawn_spring` routes mvn stdout to a per-port log file (`autoloop/results/spring-boot-<port>.log`) instead of an undrained `subprocess.PIPE` that would deadlock at the ~64KB macOS pipe buffer once the eval phase drove backend requests through the JVM.
    - **OQ-S64.2** — `_probe_url_is_up` wraps the httpx GET in `httpx.Client(trust_env=False)` so the localhost health probe is never routed through a macOS system proxy (httpx 0.28.1 honors the system proxy but ignores its localhost ExceptionsList; was producing a spurious 120 s `SpringStartupTimeoutError`).

    All three are structural plumbing on subprocess lifecycle / IO / connectivity; **no semantic decision logic**, **no keyword / regex / enum / per-UC matrix**, **no LLM call inside the validator surfaces** (fence #15 PRESERVED). Authorization: AskUserQuestion 2026-05-31 (mid-sprint STOP-and-surface; S-Auto-7.2 precedent for fast-iteration mode per user direction "可以我review后快速放宽通过"). S-Auto-10 + S-Auto-11 MUST NOT edit the fence #20 surface.

## §4.1 nine-question kernel (embedded verbatim from `docs/current/anti-hardcode-review-kernel.md`)

```text
You are the Anti-Hardcode Review Agent. The PR below proposes a change
to this repo. Your job is to decide whether the change introduces a
semantic hardcode — a keyword / regex / if-else / enum / per-UC matrix
that encodes a decision the LLM is supposed to own under
docs/current/iteration_governance.md §1.3 — and to issue a verdict.

Scope exemption: pure infra, docs-only, config-governance, and
characterization-test PRs are not subject to this review. If the PR
is purely one of those, return `approve` with a one-line note naming
the exemption.

For every other PR, walk these nine questions in order. For each
"yes" or each concern, paste the diff snippet and the reasoning.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix
   for a semantic decision (drift detection, escalation, UC
   selection, risk classification, follow-up, intake routing)?
2. If yes to (1), is the change justified as protecting a current
   Tier-0 invariant named in docs/runtime_freeze_and_risk_policy.md
   §1 / §2?
3. Could the same outcome be achieved by projecting a soft signal to
   the LLM (an additional projected slot, a candidate list, a
   diagnostic flag) instead of a hard branch in Java or the prompt?
4. Does the change encode visible-eval case text, trace-specific
   phrasing, or a CaseSpec id into runtime, prompt, or judge config?
5. Does the change move semantic ownership from the LLM to Java —
   that is, shrink what docs/current/iteration_governance.md §1.3
   says the LLM owns?
6. Does the change add an if-else block to the prompt instead of
   principle-level or observable-state guidance?
7. Does the change preserve tool schema, capability / permission
   boundary, PII / safety floor, and grounding floor?
8. Does the PR ship generalization eval coverage — target, neighbor,
   negative, and shadow cases — and not only the target case?
9. If the change is temporary, does it carry an explicit rollback or
   sunset plan (downgrade-to-signal trigger, retirement sprint id)?

Return exactly one verdict:

- `approve` — the change is not a semantic hardcode, or is justified
  as protecting a current Tier-0 invariant with adequate generalization
  coverage and a clear rollback if temporary.
- `approve with downgrade-to-signal follow-up` — the change is
  acceptable as an interim measure, but a follow-up sprint must
  convert it into a soft signal projected to the LLM. Name the
  trigger that should fire the conversion.
- `reject as semantic hardcode` — the change encodes a soft semantic
  decision the LLM should own; questions 1 and 2 fail, or questions
  5 / 6 fail, with no Tier-0 claim and no sunset plan.
- `needs human architecture decision` — the change crosses an
  unresolved governance question (new escalation reason enum value,
  new Tier-0 candidate, LLM-vs-Java boundary shift, new tool surface)
  and a human reviewer must decide before merge.

Do not rewrite the PR. Do not propose a code fix beyond naming the
layer in docs/current/iteration_governance.md §3 that the fix should
target.
```

## §4.1 Verdict set (use ONE)

- `approve` — no concerns; clean PASS on all 9 questions. The pure-infra carve-out applies if §4.1 walk is itself unnecessary; if you select this verdict, name the exemption explicitly in the summary.
- `approve with downgrade-to-signal follow-up` — kernel PASS but residual concerns warrant a non-blocking follow-up (e.g., narrow a rule, open an R-item, surface a calibration observation). Provide the trigger explicitly.
- `reject as semantic hardcode` — at least one §1.7 forbidden-list red line crossed; targeted fix-iteration required before sub-sprint can close.
- `needs human architecture decision` — kernel cannot be resolved without product / governance / Tier-0 decision; escalate.

## §4.2 Sprint-close header (write at TOP of `docs/codex-findings.md`)

```
## Sprint 064 / S-Auto-9 — Per-sub-sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph stating the verdict + key findings + §4.1 verdict>
```

## Verification axes (Codex walks each; report PASS / CONCERN / FAIL + per-axis evidence)

### Axis A — §4.1 nine-question kernel walk

Apply the kernel to the cumulative 1-commit S-Auto-9 scope (`875772f..HEAD`; code-bearing commit `e342d89`). Expected: PASS on all 9 (pure-infra plumbing on subprocess lifecycle, IO routing, and connectivity). The kernel's scope exemption ("pure infra, docs-only, config-governance, and characterization-test PRs are not subject to this review") arguably applies; nevertheless walk all 9 questions for paper-trail per the §7 stanza self-walk convention. If you find any structural semantic hardcode, FAIL Axis A with cited evidence.

Per-question expected reads:

- **Q1** PASS: no semantic keyword / regex / enum / per-UC matrix introduced. The 3 fixes are `subprocess.Popen` kwarg (`start_new_session=True`), stdout redirection to a log file, and an httpx client constructor kwarg (`trust_env=False`).
- **Q2** PASS: no new Tier-0 invariant claimed (handoff §0 + §7 stanza self-walk explicitly say none).
- **Q3** PASS: semantic ownership did not move from LLM to runtime; all three fixes are subprocess / IO / connectivity plumbing.
- **Q4** PASS: no visible-eval case text, CaseSpec id, expected answer, or case-status label encoded as runtime logic.
- **Q5** PASS: LLM-vs-runtime ownership split (§1.3 / §1.4) preserved; runtime owns subprocess lifecycle + IO routing (which it already did); the fixes don't shrink LLM ownership.
- **Q6** PASS: no prompt / template / Skill YAML / per-UC instruction matrix changed.
- **Q7** PASS: tool schema, capability boundary, PII / safety floor, grounding floor preserved; cumulative diff is zero-touch on Java / eval / Skill YAML / runtime semantic surfaces.
- **Q8** PASS: infra generalization coverage = the smoke iter through Step 9 (`exp-18`) confirms the substrate now reaches the tier_evaluator gate with a non-null 5-layer verdict; the 3 fixes individually verified via diagnostic scripts (signal-trap probe falsified the OS-kill hypothesis; step4d falsified the proxy-bypass hypothesis; the pipe-buffer hypothesis is a pre-emptive structural fix).
- **Q9** PASS: no temporary semantic rule added; all three fixes are durable structural plumbing.

### Axis B — Fence #20 controlled override verification

Fence #20 requires independent verification:

- **Fence #20 (`applier.py`)**: POST-HOC blessed at §8.3 in-place revision 2026-05-31 per dev mid-sprint AskUserQuestion authorization. Cumulative envelope = three named fixes in `e342d89`:
  - OQ-S62.3 — `_spawn_spring` adds `start_new_session=True`;
  - OQ-S64.1 — `_spawn_spring` replaces `subprocess.PIPE` with per-port log file (`autoloop/results/spring-boot-<port>.log`);
  - OQ-S64.2 — `_probe_url_is_up` wraps `httpx.get` in `httpx.Client(trust_env=False)`.
  
  Verify:
  - (a) Other `applier.py` lines byte-identical from M-Auto-1C close (`875772f`): run `git diff --stat 875772f..HEAD -- autoloop/autoloop/sandbox/applier.py`; expected ~35 LOC; the diff content is the named changes only, not collateral.
  - (b) Each fix stays plumbing-only — `_spawn_spring`'s `start_new_session` kwarg is a `subprocess.Popen` constructor option; the log-file redirect uses `open()` + `with`; the `httpx.Client(trust_env=False)` is a constructor kwarg. No semantic decision logic, no keyword / regex / enum / per-UC matrix, no LLM call.
  - (c) Fence #15 ("No LLM call inside detector or content_validator OR applier surfaces") REMAINS UNVIOLATED — the fence #20 fixes use `subprocess`, `os`, `pathlib.Path`, and `httpx` stdlib + library calls; no LLM invocation.
  - (d) No fence creep into other applier.py functions: `_spawn_spring` and `_probe_url_is_up` are the only functions touched; `_terminate_process` is unchanged but its behavior is now safe-by-construction because of the `start_new_session=True` in `_spawn_spring`.
  - (e) No fence creep into other autoloop modules: cumulative diff on `autoloop/autoloop/scoring/` + `autoloop/autoloop/sandbox/content_validator.py` + `autoloop/autoloop/sandbox/anti_hardcode_check.py` + `autoloop/autoloop/sandbox/gaming.py` + `autoloop/autoloop/loop.py` + `autoloop/autoloop/meta_agent/` + `autoloop/autoloop/memory/` + `autoloop/autoloop/preflight.py` + `autoloop/autoloop/cli.py` + `autoloop/config.yaml` is empty (Axis F audits this cumulatively).

### Axis C — Smoke iter through Step 9 (Gate #1 retirement evidence audit)

S-Auto-9 dev claims M-Auto-1C §12.4 deferred gate #1 RETIRED via smoke iter `exp-18`. Verify:

- Handoff §4 evidence table: iteration_id = `exp-18`; wall time ~71 s; reached Step 9 (`tier_evaluator.evaluate`); `experiments.jsonl` row written with non-null `verdict.layer_results` (5 layers); Layer 0 `tier0_safety` PASS; Layer 1 `tier1_outcome` FAIL (drives the discard with reason `tier1_bad_cases_regression_5_to_0`); Layers 2-4 `passed=None` with reason `not_evaluated_short_circuit_at_layer_1`.
- Independent reproduction (optional but recommended): run `scripts/sprint-064-step4-smoke-iter.sh 1` (or `python -m autoloop run --experiments 1`) with foreground :8080 backend ready to auto-reboot; verify a row is written to `autoloop/results/experiments.jsonl` (or one of the experiment-specific directories) with non-null `verdict.layer_results`. Codex may use `jq` to inspect the row structure.
- **Gate-retirement judgment**: the gate definition in M-Auto-1C §12.4 + M-Auto-2 §5 was "live iteration end-to-end through Step 9 with non-degenerate `tier_evaluator_verdict` (Layer 0-4 all non-degenerate)". The dev's framing: Layers 2-4 `passed=None` is the tier_evaluator's **designed** short-circuit (once Layer 1 fails, Layers 2-4 are skipped because the discard decision is already determined); the layer entries are present + non-null with explicit reasons; this constitutes a non-degenerate verdict structurally. The deliver-agent + human accepted this framing at S-Auto-9 close (AskUserQuestion 2026-05-31 recommended option: gate #1 RETIRED; alternative — require a keep-trajectory Layer 0-4 traversal demo before retirement — was declined per "conflates substrate readiness with proposal quality"). Codex audits internal consistency: does the tier_evaluator's short-circuit semantics match what `verdict.layer_results` claims for Layers 2-4? If yes, Axis C PASS. If you judge the short-circuit framing is inadequate and the gate genuinely requires a full Layer 0-4 traversal (i.e., the gate definition was meant to test all 5 layers run to completion, not just produce a 5-layer structured verdict), surface as Axis C CONCERN with a downgrade-to-signal follow-up trigger naming the S-Auto-10 expected keep-trajectory verification.

### Axis D — Test deltas + reproducibility

Independent reproduction expected:

```bash
cd autoloop && uv run --extra dev pytest -q
# Expected: 266 passed, 1 warning (UNCHANGED from M-Auto-1C close; S-Auto-9 added 0 new tests).

cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py
# Expected: 31 passed (17-fixture detector sweep UNCHANGED).

cd autoloop && uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"
# Expected: 22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9 (REASSERTED; drift silent).

cd eval_interactive && uv run python -m pytest --tb=no -q
# Expected: 486 passed, 3 failed (UNCHANGED env-specific per OQ-S47.3).

git diff --stat 875772f..HEAD -- server/ eval/src/main/java/
# Expected: empty (Java zero-touch).
```

If any reproduction returns a different count, CONCERN. The 1 warning in autoloop pytest is the documented baseline-missing-shadow assertion from `baseline_loader.py` (unchanged).

**No new autoloop tests authored at S-Auto-9** — handoff §0 explicitly says "autoloop pytest 266 PASS (UNCHANGED)". The three fence #20 fixes are validated via:
- (a) smoke iter `exp-18` reaching Step 9 (functional end-to-end);
- (b) diagnostic scripts (signal-trap probe falsifies OS-kill; step4d falsifies proxy-bypass);
- (c) code-level reasoning at the commit comments (each edit cites OQ id + failure mode).

Codex should judge whether the lack of dedicated unit tests for the three narrow plumbing fixes is acceptable for pure-infra carve-out scope. Precedent: S-Auto-7.2 added 11 new tests for its 3 substrate fixes (fence #18 + #19 envelopes); S-Auto-9's lighter unit-test posture is structurally explainable because the fixes are subprocess kwargs / IO redirects / httpx client constructor flags (less behavioral surface to test in isolation; functional smoke is the load-bearing verification). If you judge this CONCERN, surface as a downgrade-to-signal follow-up trigger (e.g., "S-Auto-10 should add a regression test asserting `_spawn_spring` passes `start_new_session=True` + `_probe_url_is_up` uses `trust_env=False`").

### Axis E — §4.3 trigger #3 timing + authorization paper trail

Verify the trigger upgrade timing + paper trail:

- Handoff §7 STOP-and-surface log: ONE event recorded. 2026-05-31 — dev raised AskUserQuestion when root-cause fix required editing `applier.py` (fence #18 FINALIZED at M-Auto-1C close, beyond the prompt's anticipated fence #20 envelope on `cli.py` / NEW files). Human authorized fence #20 controlled override (recommended option "Authorize fence #20 — apply start_new_session=True"). Subsequently OQ-S64.1 + OQ-S64.2 were discovered during §4 verification and applied under the same fence #20 authorization, narrated transparently to the human at the time of each application (per user's fast-iteration preference).
- Per dev's interpretation: a single AskUserQuestion authorization with subsequent transparent narration suffices for the same-file same-goal envelope. The handoff §5 + §7 explicitly capture this.
- Per `iteration_governance.md` §4.3 trigger #3: hard-fenced surface (applier.py was fence #18 FINALIZED at M-Auto-1C close) was re-touched. **Per-sub-sprint Codex required** (this review).
- Verify §8.3 in-place revision of `docs/milestone_objective.md` §6 fence list NOW includes fence #20 POST-HOC BLESSED entry (deliver-agent's S-Auto-9 close-bundle commit; verify the revised §6 fence #20 entry exists at `docs/milestone_objective.md` §6 + matches the actual fence touch).

If you judge the single-AskUserQuestion-plus-transparent-narration paper trail is inadequate for the three-fix envelope (e.g., OQ-S64.1 + OQ-S64.2 required separate authorizations rather than being subsumed under "same file, same goal" scope), surface as Axis E CONCERN. Per the deliver-agent + human joint judgment at S-Auto-9 close 2026-05-31 (recommended option): same-file same-goal subsumption accepted as legitimate; deliver-agent + human approved the §8.3 in-place revision capturing all three fixes within the fence #20 envelope.

### Axis F — Hard-fence cumulative verification

```bash
git diff --stat 875772f..HEAD -- \
  autoloop/autoloop/scoring/ \
  autoloop/autoloop/sandbox/anti_hardcode_check.py \
  autoloop/autoloop/sandbox/content_validator.py \
  autoloop/autoloop/sandbox/gaming.py \
  autoloop/autoloop/loop.py \
  autoloop/autoloop/meta_agent/ \
  autoloop/autoloop/memory/ \
  autoloop/autoloop/preflight.py \
  autoloop/autoloop/cli.py \
  autoloop/config.yaml \
  eval_interactive/ \
  server/ eval/ data/ db/ \
  server/src/main/resources/ \
  docs/foundational/ docs/runtime_freeze_and_risk_policy.md docs/current/
# Expected: empty.

git diff --stat 875772f..HEAD -- \
  docs/sprints/sprint-001-* docs/sprints/sprint-061-* docs/sprints/sprint-062-* docs/sprints/sprint-063-* \
  docs/milestones/M-Auto-1B_* docs/milestones/M-Auto-1C_*
# Expected: empty (sprint archives + closed milestone archives immutable).
```

If non-empty, FAIL Axis F with cited file paths + recommendation (fence creep needs immediate scope review or post-hoc revision).

### Axis G — Single-commit pattern + scope-clean

S-Auto-9 dev prompt recommended a multi-commit pattern acceptable (investigation + iteration-heavy; may produce multiple commits including diagnostic scripts + fix + handoff), with single-commit pattern preferred for the FIX commit itself. Actual: 1 cumulative commit (`e342d89`) bundling fix + diagnostic scripts + handoff. Verify scope-clean per-file:

- `autoloop/autoloop/sandbox/applier.py` (+35 −8): the three fence #20 named fixes only (`_spawn_spring` + `_probe_url_is_up`); verify no other applier.py functions touched.
- `docs/sprints/sprint-064-handoff.md` (+294): handoff §0-§8 per format.
- `scripts/sprint-064-{signal-trap-probe,step2b-bash-launch,step2c-dtrace-signal,step4,step4b-spring-boot-timing,step4c-spawn-repro,step4d-probe-env-test}.{sh,py}` (7 files): diagnostic scripts under pure-infra carve-out.

If any file boundary is unclear or includes unrelated edits, CONCERN.

### Axis H — Diagnostic scripts (pure-infra carve-out audit)

The 7 diagnostic scripts under `scripts/sprint-064-*` were authored under pure-infra carve-out (handoff §2 + dev prompt §"Diagnostic scripts"). Verify:

- Scripts are diagnostic / reproduction harnesses only (signal-trap probe, bash launcher, dtrace one-liner, smoke iter wrapper, spring boot timing, spawn repro, probe env test).
- No script imports or modifies the autoloop production code path (they invoke `python -m autoloop`, `mvn`, `httpx`, `urllib`, system tools; they do not patch `autoloop/autoloop/**`).
- Scripts are not load-bearing for the substrate; they are pure investigation harnesses.
- No semantic decision logic in script bodies; no keyword / regex / enum / per-UC matrix.

If any script contains load-bearing autoloop code, CONCERN. Disposition: per dev prompt's pure-infra carve-out, these scripts are paper-trail evidence + reproduction harnesses, not production code.

### Axis I — OQ-S64.3 disposition (~71s/iter observation)

S-Auto-9 surfaces OQ-S64.3 as observation: full iterations now run in ~71 s end-to-end, so a 15-iter overnight may complete in ~15-20 min rather than the planned 6-8 h budget. Disposition per deliver-agent + human AskUserQuestion 2026-05-31 (recommended option): keep ≥10 iter target (M-Auto-2 §5 hard gate); 6-8 h budget downgraded to observational; S-Auto-10 to exercise the new short-iter ergonomics + consider an optional second batch within same sub-sprint to exercise lessons compaction at K=20. Not a code defect; observation only.

Verify the disposition is internally consistent + appropriate scope-discipline (the substrate-fix work is done; the iter cost is a substrate-performance observation, not a defect). If you judge OQ-S64.3 should be formalized as an R-item (instead of carry-over to S-Auto-10), surface as Axis I non-blocking concern.

### Axis J — Gate #1 retirement judgment internal-consistency audit

S-Auto-9 dev flagged a judgment call (handoff §4 nuance paragraph): exp-18 short-circuited at Layer 1 (discard trajectory). Dev's framing: substrate produces genuine decision-driving verdicts (vs prior all-NULL); 5-layer verdict structure satisfies the gate definition; Layers 2-4 `passed=None` with explicit reason `not_evaluated_short_circuit_at_layer_1` are designed tier_evaluator semantics, NOT degeneracy. Deliver-agent + human accepted this framing at S-Auto-9 close (AskUserQuestion 2026-05-31 recommended option).

Codex audits internal consistency:

- Does tier_evaluator's documented behavior support the "designed short-circuit" framing? (Look at `autoloop/autoloop/scoring/tier_evaluator.py` — fence #13 LOCKED at hash `22548e20…` — for short-circuit semantics).
- Is the 5-layer verdict structure sufficient evidence that the substrate is no longer producing degenerate `verdict=NULL` outputs? (M-Auto-1B Goal #3 historical context: 0/13 historical experiments.jsonl rows had `verdict=NULL` due to substrate never reaching Step 9 — Step 9 was OS-killed mid-process.)
- If the gate definition was meant to verify all 5 layers run to completion (NOT just produce a 5-layer structured verdict with explicit short-circuit reasons at Layers 2-4), surface as Axis J CONCERN with a downgrade-to-signal follow-up trigger naming S-Auto-10 expected keep-trajectory verification.

Per the deliver-agent + human joint judgment at S-Auto-9 close 2026-05-31 (recommended option): gate #1 RETIRED accepted; a keep-trajectory Layer 0-4 traversal demo is S-Auto-10 territory (proposal quality), not S-Auto-9 (substrate readiness). Codex confirms internal consistency or flags concern.

### Optional Axis K — OQ-S62.3 root-cause framing (sprint-063 §8 falsification audit)

S-Auto-9 dev claims sprint-063 §8's hypotheses (caffeinate / screen / AC+lid / memory pressure / jetsam / launchd / TAL) are ALL FALSIFIED; the root cause was self-inflicted killpg suicide on `applier.py`. Verify:

- Handoff §2 per-hypothesis evidence: jetsam silent for the PID, signal-trap probe survived >900 s under identical launch (autoloop-workload-specific kill), bash launcher reproduced the kill (BG_NICE not cause), dtrace blocked by SIP (could not capture signal+sender), code-read of applier.py identified the killpg.
- The framing is internally consistent: process-group suicide explains the silent jetsam (kill from within the same process tree), the workload-specificity (only autoloop calls killpg via its own substrate), the timing (death at the 120 s health-probe timeout from OQ-S64.2 → ~3 min total after launch), the bash-parent-also-dies pattern under non-interactive bash launchers (shell shares the pgrp).
- The prior sprint-063 §8 hypothesis list was an investigation outcome; FALSIFICATION via S-Auto-9 diagnostic is the standard scientific protocol. Disposition: sprint-063 §8 hypotheses recorded historical; S-Auto-9 §2 supersedes.

If you judge the falsification is inadequate (e.g., dtrace was the highest-evidence-density probe and could not be run due to SIP; some other OS subsystem is still possible), surface as Axis K CONCERN. Per deliver-agent + human joint judgment at S-Auto-9 close: code-read identification of killpg + substrate fix + smoke iter exp-18 survival is sufficient evidence; alternate-OS-kill hypotheses are residual and not load-bearing for sub-sprint close.

## Constraints (Codex MUST respect)

- **No code edits**. Codex provides verdicts + evidence; deliver-agent + human + dev close any fixes.
- **No re-judging bad-case manual review verdicts** (none in scope for S-Auto-9; cs001 + wmkb reference signals are S-Auto-10 territory).
- **No re-judging Phase 2 §5.6 evidence** from M-Auto-1B/C (already closed; not in scope).
- **No mvn / pytest reproduction beyond what's claimed** (you may verify but don't extend scope).
- **No SIP-disabled dtrace investigation** (handoff §2 explicitly says dtrace `proc:::signal-send` blocked by SIP; high-risk / out-of-scope to enable).
- **Per-sub-sprint Codex review prompt is `compact/sprint-064-codex-review-prompt.md`** (this file); verdict lands in `docs/codex-findings.md` per §4.2 header convention; deliver-agent archives at S-Auto-9 close-bundle to `docs/sprints/sprint-064-codex-review.md` per existing convention.

## Output format

Write verdict at TOP of `docs/codex-findings.md` per §4.2 convention. Use the per-sub-sprint header format:

```
## Sprint 064 / S-Auto-9 — Per-sub-sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph stating the verdict + key findings + §4.1 verdict>

### Axis A - §4.1 Nine-question Kernel: PASS / CONCERN / FAIL
<evidence + 1-sentence per-question verdict>

### Axis B - Fence #20 Controlled Override: PASS / CONCERN / FAIL
<evidence per fix>

### Axis C - Smoke iter Step 9 evidence audit (Gate #1 retirement): PASS / CONCERN / FAIL
<5-layer verdict structure verification + designed short-circuit framing audit>

### Axis D - Test deltas + reproducibility: PASS / CONCERN / FAIL
<reproduce 4 counts + scoring SHA + Java zero-touch>

### Axis E - §4.3 trigger #3 timing + authorization paper trail: PASS / CONCERN / FAIL
<single-AskUserQuestion + transparent-narration audit; §8.3 in-place revision verification>

### Axis F - Hard-fence cumulative verification: PASS / CONCERN / FAIL
<git diff --stat output for all gated paths>

### Axis G - Single-commit pattern + scope-clean: PASS / CONCERN / FAIL
<per-file boundary>

### Axis H - Diagnostic scripts pure-infra carve-out audit: PASS / CONCERN / FAIL
<7 scripts non-load-bearing verification>

### Axis I - OQ-S64.3 (~71s/iter) disposition: PASS / CONCERN / FAIL
<internal-consistency audit>

### Axis J - Gate #1 retirement judgment internal-consistency audit: PASS / CONCERN / FAIL
<tier_evaluator short-circuit semantics audit>

### Optional Axis K - OQ-S62.3 root-cause framing (sprint-063 §8 falsification): PASS / CONCERN / FAIL (if surfaced)
<falsification audit>

### §4.1 Verdict

`approve` | `approve with downgrade-to-signal follow-up` | `reject as semantic hardcode` | `needs human architecture decision`

Follow-up trigger (if applicable): <name + 1-sentence justification>
```

## What this review DOES and DOES NOT cover

**DOES cover**:
- 1-commit cumulative scope `875772f..HEAD` on `auto-loop-branch` (= `e342d89`).
- Fence #20 controlled override (applier.py three narrow fixes; subprocess lifecycle / IO / connectivity plumbing).
- §4.3 trigger #3 fence #20 timing + authorization paper trail.
- Smoke iter Step 9 evidence (Gate #1 retirement judgment).
- OQ-S62.3 + OQ-S64.1 + OQ-S64.2 RESOLVED dispositions.
- OQ-S64.3 + OQ-zsh-BG_NICE OPEN carry-overs to S-Auto-10.
- Sprint-063 §8 hypothesis falsification audit.
- Diagnostic scripts pure-infra carve-out audit.

**DOES NOT cover**:
- S-Auto-10 overnight + cherry-pick mechanism (not yet executed).
- M-Auto-2 milestone-shared close verdict (Codex separately reviews at M-Auto-2 close over cumulative range).
- Phase 2 §5.6 bad-case + shadow evidence from M-Auto-1B/C (already closed).
- Bad-case manual review judgments (none in S-Auto-9 scope; cs001 + wmkb reference signals for S-Auto-10 are deliver-agent + human prior judgment).
- SIP-disabled deeper-OS investigation (out of scope; deliver-agent + human accepted code-read identification of killpg as sufficient root cause).
- Stale `autoloop/exp-*` branch cleanup (operational; carry-over from M-Auto-1C operational hygiene).

## Decision conditions

- **Verdict `pass / 0 / approve`** (or `approve` with pure-infra scope exemption invoked): all 10 (or 11) axes PASS + no §4.1 trigger needs follow-up. S-Auto-10 can dispatch immediately.
- **Verdict `pass / 0 / approve with downgrade-to-signal follow-up`**: axes PASS or PASS-WITH-CONCERN; non-blocking follow-up trigger named (e.g., S-Auto-10 add a regression test asserting `_spawn_spring` passes `start_new_session=True`; OR S-Auto-10 verify keep-trajectory Layer 0-4 traversal at overnight close; OR observability hygiene calibration). S-Auto-10 can dispatch with the recommendation incorporated into its dev prompt.
- **Verdict `fix_required / >0 / reject as semantic hardcode`**: at least one axis FAIL with §1.7 forbidden-list red line crossed. Targeted fix-iteration required BEFORE S-Auto-9 can close + S-Auto-10 dispatches. Examples: fence #20 changes encode semantic logic (Q1 / Q3 / Q5); applier.py changes add an LLM call (fence #15 violation); changes touch semantic decision boundaries.
- **Verdict `fix_required / >0` (non-hardcode)**: at least one axis FAIL on substrate / paper-trail / fence-creep concern (e.g., Axis B fence creep beyond named fixes; Axis E authorization inadequate; Axis F hard-fence non-empty). Targeted fix-iteration scope discussion with deliver-agent + human.
- **Verdict `out_of_scope_review`**: review broadens beyond sub-sprint scope. Surface to deliver-agent + human; potentially Class C close per `docs/current/deliver_close_taxonomy.md`.
- **Verdict `needs human architecture decision`**: kernel cannot be resolved without product / governance / Tier-0 decision. Escalate.

Begin review. Walk each axis in order. Write the verdict header at TOP of `docs/codex-findings.md` before the axis bodies. Cite evidence with `file:line` references for all PASS / CONCERN / FAIL claims.
