# Codex review prompt — Milestone M-Auto-1C — Auto-Evolution Calibration Continuation — Milestone-shared Anti-Hardcode review at Class C in-flight-downgrade close

You are the **Milestone-Shared Anti-Hardcode Review Agent for Milestone M-Auto-1C — Auto-Evolution Calibration Continuation**. This is the **milestone-shared Codex pass** dispatched per `docs/current/iteration_governance.md` §4.3 default at M-Auto-1C close. M-Auto-1C is being **CLOSED WITH AN INCOMPLETE ACCEPTANCE BAR AS CLASS C IN-FLIGHT DOWNGRADE** per the human-locked joint AskUserQuestion 2026-05-30 (close now; OQ-S62.3 + first overnight + first cherry-pick → M-Auto-2). M-Auto-1C is the §8.5 continuation of M-Auto-1B (2 sub-sprints: S-Auto-7.2 applier fix + S-Auto-8 first overnight). Your verdict feeds the deliver-agent + human's milestone-close classification (expected C — In-flight downgrade per §12.1 hard-gate tally 8 PASS + 3 FAIL + 2 N/A; sub-sprint S-Auto-7.2 itself is Class A — Clean close at sub-sprint level; sub-sprint S-Auto-8 is PARTIAL Class C-style carryover at sub-sprint level rolling into the milestone verdict).

**One per-sub-sprint Codex review has ALREADY been completed** and is an input to this milestone-shared pass (do NOT re-litigate the per-sub-sprint findings):

1. **S-Auto-7.2 / Sprint 062** per-sub-sprint review (§4.3 trigger #3 — fence #19 `content_validator.py` rule 3 length_overflow post-hoc controlled override authorized at STOP-2 mid-sprint 2026-05-30) returned `decision: pass / blocking_count: 0` 2026-05-30 with §4.1 verdict `approve with downgrade-to-signal follow-up`. Single non-blocking Axis E concern (stale pre-expansion wording at 3 doc locations) was addressed at S-Auto-7.2 close-bundle commit `583e5a3` (milestone_objective.md locations fixed in-place per §8.3 deliver-agent-owned revision; handoff line 490 acknowledged-as-immutable per `doc_governance.md` "sprint archives never edited" rule). Archived at `docs/sprints/sprint-062-codex-review.md`; prompt at `compact/sprint-062-codex-review-prompt.md`.

The cumulative commit range under review for THIS milestone-shared pass:

```
586f138 (exclusive)..<M-Auto-1C-close-bundle-sha> (inclusive)

07eab09  Sprint 062 / S-Auto-7.2 / M-Auto-1C — applier.py:370-378 mvn module-selection fix + 2 tests (OQ-S61.1 RESOLVED)
121ecca  Sprint 062 / S-Auto-7.2 / M-Auto-1C — initial handoff (superseded by 7183c20 content; preserved for paper-trail)
7183c20  Sprint 062 / S-Auto-7.2 / M-Auto-1C — OQ-S62.1 content_validator length_overflow rewrite + OQ-S62.2 _git_create_branch idempotency + 9 tests + 506-line handoff revision (fence #19 post-hoc controlled override)
583e5a3  Sprint 062 / S-Auto-7.2 / M-Auto-1C close-bundle — §3 + §6 in-place revision per §8.3 + per-sub-sprint Codex review prompt
42e254b  Sprint 062 / S-Auto-7.2 / M-Auto-1C close-bundle — archive sub-sprint + S-Auto-8 contract + dev prompt
2653aac  Sprint 063 / S-Auto-8 / M-Auto-1C — cv ceiling 1000→1200 + overnight wrapper scripts
4ffb86c  Sprint 063 / S-Auto-8 / M-Auto-1C — handoff (partial; OQ-S62.3 expansion blocks overnight)
<close-prep>  Sprint 063 / S-Auto-8 / M-Auto-1C close-prep — pre-Codex bundle (manifest + action_bank R-item dispositions + this prompt + milestone_objective §12 placeholder + M-Auto-2 milestone_objective + S-Auto-9 sprint_objective + S-Auto-9 dev prompt)
                              (this is the commit that prepares for your review; final SHA will be the commit human creates BEFORE dispatching this prompt to you)
```

7 agent-loop commits + 1 close-prep commit + 1 close-bundle commit (post your verdict) in the cumulative range; 2 sub-sprints (S-Auto-7.2 already per-sub-sprint reviewed + S-Auto-8 first milestone-shared exposure); 1 per-sub-sprint Codex review already complete (S-Auto-7.2 per §4.3 trigger #3 fence #19 upgrade); this milestone-shared pass is the first Codex exposure for S-Auto-8 + the close-prep bundle.

The previous milestone (M-Auto-1B — Auto-Evolution Calibration) closed at commit `586f138` (A-with-acceptance-bar-revision sub-classified `approve with downgrade-to-signal follow-up` 2026-05-30; 5/15 hard gates explicitly inherited to M-Auto-1C per §8.5 split decision). All Java + eval-interactive Python baselines from M-Auto-1B close are the inherited baselines this milestone-shared review compares against:

- Java `Tests run: 1183, Failures: 1, Errors: 0, Skipped: 2` (the lone inherited `SystemPromptUserRequestedTiebreakerTest` failure since Sprint 24-era working-tree mod is documented OQ-S41.5 STATUS QUO).
- eval_interactive Python `486 passed, 3 failed` (3 env-specific per OQ-S47.3 STATUS QUO).
- autoloop pytest `255 PASS, 1 warning` at S-Auto-7.1 close baseline → 266 PASS at S-Auto-7.2 close (+11 new tests: 2 applier mvn invocation + 6 cv absolute_ceiling + 3 git branch idempotency).
- 17-fixture anti_hardcode detector sweep `31 PASS` UNCHANGED.
- `scoring_code_baseline_sha` `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9` REASSERTED at S-Auto-7.2 close (zero touch to `autoloop/autoloop/scoring/`).

---

## Read order (minimal)

Read only:

1. `AGENTS.md` (auto-loaded via constitution chain — pulls `doc_governance.md` + `agent_context_guide.md` + `iteration_governance.md`).
2. This prompt (self-contained executable view per `iteration_governance.md` §9 invariant).
3. **The two sub-sprint handoffs** (the dev's own close artefacts):
   - `docs/sprints/sprint-062-handoff.md` — S-Auto-7.2: applier mvn fix (OQ-S61.1) + content_validator length_overflow rewrite with new `length_overflow_absolute_ceiling: 1000` knob (OQ-S62.1) + `_git_create_branch` idempotency via new `_branch_exists` helper (OQ-S62.2); 506-line dev handoff (already per-sub-sprint reviewed; sample for cross-reference only)
   - `docs/sprints/sprint-063-handoff.md` — S-Auto-8: first overnight blocked by OQ-S62.3 expansion across 9 launch attempts; substrate validated through Step 6 mvn spawn; baseline drift envelope 0/46 case_passed drift; R-S58 propose-distribution scan 0/13 historical Cf chars; cv ceiling 1000→1200 (single exp-13 data point); 4 OQs surfaced (OQ-S62.3 expansion + OQ-eval-judge-disabled + OQ-cv-ceiling-calibration-thin-evidence + OQ-prompt-doc-sandbox-gaming-path-typo); STOP-and-surface invoked at session end with overnight 0/15 + §5.6 manual review + cherry-pick deferred to M-Auto-2
4. **The S-Auto-7.2 per-sub-sprint Codex archive** (input-only; for cross-reference if you need to understand what was already verified):
   - `docs/sprints/sprint-062-codex-review.md` — S-Auto-7.2 per-sub-sprint verdict `pass / 0` `approve with downgrade-to-signal follow-up` 2026-05-30; 9 axes A-I all PASS or PASS-with-CONCERN; single Axis E concern addressed in S-Auto-7.2 close-bundle commit `583e5a3`
   - `compact/sprint-062-codex-review-prompt.md` — S-Auto-7.2 per-sub-sprint review prompt (Axis A-I; verdict)

You may sample (NOT embed) the following code paths for verification:

- `autoloop/autoloop/sandbox/applier.py` — S-Auto-7.2 OQ-S61.1 mvn `-am` removal at lines 370-378 (Codex Axis C reproduced 2026-05-30 Spring `/actuator/health: UP` in 6s on port 19998) + OQ-S62.2 `_git_create_branch` idempotency via new `_branch_exists` helper (`git show-ref --verify --quiet refs/heads/<name>`); verify the fence #18 envelope captures both fixes within infra plumbing-repair scope (no semantic decision logic).
- `autoloop/autoloop/sandbox/content_validator.py` — S-Auto-7.2 OQ-S62.1 rule 3 rewrite `overflow_cap = max(overflow_ratio * before_len, float(absolute_ceiling))`; fence #19 post-hoc controlled override per §8.3 in-place revision (S-Auto-7.2 close-bundle commit `583e5a3` ratified the fence); rule count UNCHANGED at 5 cv rules; verify D3 invariant ("No LLM call inside detector or content_validator" per D1 regex-heuristic-only rule) REMAINS UNCHANGED (the rule 3 rewrite is a threshold knob refinement, not an LLM call addition).
- `autoloop/config.yaml` — S-Auto-7.2 added `content_validator.length_overflow_absolute_ceiling: 1000` knob + S-Auto-8 bumped to `1200` (post-exp-13 single-data-point calibration; surfaced as OQ-cv-ceiling-calibration-thin-evidence ledger-only).
- `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` — fence #13 byte-identical to M-Auto-1B close `22548e20…`; verify zero touch across M-Auto-1C cumulative range (S-Auto-7.2 + S-Auto-8).
- `autoloop/autoloop/sandbox/anti_hardcode_check.py` — fence; verify zero touch across M-Auto-1C cumulative range (rule count UNCHANGED at 11; detector self-discipline 3 regression tests UNCHANGED PASS).
- `autoloop/autoloop/loop.py`, `autoloop/autoloop/meta_agent/`, `autoloop/autoloop/memory/`, `autoloop/autoloop/preflight.py`, `autoloop/autoloop/cli.py` — fences; verify zero touch across M-Auto-1C cumulative range.
- `scripts/run-overnight.sh` + `scripts/launch-overnight.py` — NEW at S-Auto-8 (overnight wrapper attempts; abandoned mid-sub-sprint per OQ-S62.3 expansion investigation; not load-bearing — substrate works without them).
- `eval_interactive/eval_interactive/`, `eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`, `server/src/main/java/`, `eval/src/main/java/`, `data/`, `db/`, `server/src/main/resources/{prompts,scripts,config,mock,skills}/` — verify zero touch across M-Auto-1C cumulative range (no cherry-pick landed; skills/ untouched).
- `docs/milestone_objective.md` — milestone live (will be archived AFTER your verdict to `docs/milestones/M-Auto-1C_objective.md`); read §1 milestone class + §2 goal + §3 sub-sprint sequence + §5 acceptance bar (13 hard gates) + §6 hard fences (17+2 items including the new fence #19 post-hoc controlled override) + §7 R-items + §10 stop conditions + §12 closure verdict (full Class C in-flight downgrade rationale with §12.1-§12.9 subsections; verify against acceptance-bar tally + sub-sprint dispositions + OQ-S62.3 expansion summary + R-item flips + joint AskUserQuestion decisions).
- `docs/action_bank.md` §5.2 — R-S58 CLOSED-AS-THEORETICAL-ONLY annotation (2026-05-30 at M-Auto-1C close; 0/13 historical Cf chars; reopen condition documented); R-eval-interactive-judge-score-never-populated annotation update (S-Auto-8 §3 baseline 6× evidence; LOW priority M-Auto-2+ unchanged).
- `docs/action_bank.md` §6 Sprint 063 row + §6.5 M-Auto-1C closed-milestone index row (close-prep commit landed).
- `eval_interactive/case_specs/bad_cases/_manifest.md` "M-Auto-1C close" section — §5.6 PRIMARY GATE close-day rerun evidence (deliver-agent ran 3 suites against foreground :8080 backend; bad_cases parallel=1 + anchor_outcome parallel=4 + shadow parallel=4; per-case PASS / FAIL / IMPROVING joint judgment recorded).

---

## Embedded milestone class + Codex review plan (from `docs/milestone_objective.md` §1 + §8)

| Sub-sprint | Layer (primary) | §7 stanza | Codex review |
|---|---|---|---|
| S-Auto-7.2 / Sprint 062 — applier mvn fix (OQ-S61.1) + post-hoc OQ-S62.1 cv length_overflow + OQ-S62.2 `_git_create_branch` idempotency | `infra` (substrate plumbing repair across 3 surfaces) | EXEMPT (pure-infra carve-out self-walked) | **Per-sub-sprint COMPLETE** (verdict `pass / 0` `approve with downgrade-to-signal follow-up` 2026-05-30 per §4.3 trigger #3 fence #19 upgrade; this milestone-shared pass cross-checks cumulative composition only) |
| S-Auto-8 / Sprint 063 — first overnight batch + first human review + first cherry-pick to main (BLOCKED by OQ-S62.3 expansion) | `eval_spec` (per-iteration fitness verdict consumption + manual review + cherry-pick) | REQUIRED (semantic-touching via cherry-pick if it lands; cherry-pick did NOT land) | **Milestone-shared (this review)** — no §4.3 per-sub-sprint trigger fired in S-Auto-8 (cherry-pick did NOT land per OQ-S62.3 block; no §1.7 borderline candidate surfaced) |

The §4.3 trigger #4 (prior sub-sprint fix_required outcome) did NOT apply at S-Auto-8 open because S-Auto-7.2 closed `pass / 0 / approve with downgrade-to-signal follow-up`, not `fix_required`. The S-Auto-7.2 Axis E concern was a docs-only stale-wording fix, addressed in the S-Auto-7.2 close-bundle and not requiring per-sub-sprint re-review of S-Auto-8.

---

## Embedded milestone goal (from `docs/milestone_objective.md` §2)

Finish what M-Auto-1B substrate-fixed but didn't validate end-to-end. At M-Auto-1C close, the M-Auto-1A/B substrate was supposed to be demonstrated to:

1. ✗ **Smoke iter through Step 9 with non-degenerate `tier_evaluator_verdict` (Layer 0-4 all non-degenerate)**: NOT MET. S-Auto-7.2 retired OQ-S61.1 + OQ-S62.1 + OQ-S62.2 at substrate layer + Codex Axis C reproduced Spring UP in 6s; structural-equivalent evidence at S-Auto-7.2 close. S-Auto-8 attempted standard-path smoke iter via raw shell per OQ-S62.3 workaround; exp-13 SURVIVED 17 min but cv-discarded at Step 2.5 (1089 chars; led to cv ceiling 1000→1200 bump); subsequent overnight attempts reached Step 6 mvn spawn but were killed mid-Step 7 by OQ-S62.3 expansion. No experiments.jsonl row carries `verdict.layer_results` non-null. **Deferred to M-Auto-2 S-Auto-9 + S-Auto-10**.
2. ✗ **Pre-batch baseline drift envelope established (≥2 rerun)**: PARTIAL via 2 baseline reruns per suite at S-Auto-8 §3 (run-IDs `20260530-073918/082909/074858/094858/075127/095127`); per-suite case_passed drift 0/46 across all 3 suites; gate <10/34 PASS at the drift-envelope discipline level. **Achieved without overnight; deferred overnight kick-off to M-Auto-2**.
3. ✗ **First overnight batch executed**: NOT MET. 0 of 15 iterations completed across 6+ launch attempts spanning every escape route (Claude Code Bash, user iTerm, Python wrapper with `start_new_session=True`, GNU screen detached, caffeinate-wrapped with all 3 sleep-prevention assertions, AC power, 7.5GB free RAM). All died at 2-5 min mark after Step 6 mvn spawn. No matching kernel jetsam log entries. **Deferred to M-Auto-2 S-Auto-10**.
4. ✗ **First human review of overnight kept candidates**: NOT MET (no kept candidates). **Deferred to M-Auto-2 S-Auto-10 close**.
5. ✗ **First cherry-pick decision via AskUserQuestion**: NOT MET. **Deferred to M-Auto-2 S-Auto-10 close**.
6. ✓ **Final R-S58 disposition**: MET — CLOSED-AS-THEORETICAL-ONLY based on 0/13 historical Cf-char observations in `autoloop/results/experiments.jsonl` baseline scan (S-Auto-8 §5 evidence; reopen condition documented).

**Acceptance bar reality**: 8/13 hard gates met cleanly + 3/13 FAIL (live iter end-to-end + first overnight + first cherry-pick) + 2/13 N/A (manual review + apply Hybrid because no candidates) (`docs/milestone_objective.md` §12.1 hard-gate tally). All 4 unmet/N/A semantic gates trace to OQ-S62.3 expansion blocking overnight execution at OS-level kill.

**Crucially M-Auto-1C ships NO Skill YAML edits and NO cherry-picks to main.** Cherry-pick was planned for S-Auto-8 (allowed EXACTLY ONCE per §6 fence #7) but never executed because OQ-S62.3 expansion blocked overnight; no `server/src/main/resources/skills/*.yaml` touched.

---

## Embedded milestone §6 hard fences (verbatim from `docs/milestone_objective.md` §6 — 17+2 with S-Auto-7.2 post-hoc fence #19 controlled override)

All 17+2 fences. Your hard-fence walk uses this list as the canonical reference:

1. **No edits** to any file under `server/src/main/java/**`. Runtime byte-identical to M-Auto-1B close on the agent-loop scope.
2. **No edits** to `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`. The loader.py controlled override from M-Auto-1B S-Auto-7 is FINALIZED; subsequent sub-sprints MUST NOT edit it.
3. **Cherry-pick exception (UNCHANGED FROM M-Auto-1B)**: `server/src/main/resources/skills/*.yaml` files are **conditionally writable EXACTLY ONCE** in M-Auto-1C — via the S-Auto-8 cherry-pick mechanism only. (DID NOT EXERCISE — no cherry-pick landed.)
4. **No edits** to `data/**`, `db/migration/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/iteration_governance.md`, `docs/teams/**`.
5. **No edits** to sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-061-*` or any prior milestone archive under `docs/milestones/` (including M-Auto-1B archives).
6. **No `git add -A`** by the dev agent. Stage only S-Auto-N scope files explicitly.
7. **Cherry-pick to main is ALLOWED EXACTLY ONCE** during S-Auto-8 — for ONE human-approved kept candidate via `python -m autoloop apply --experiment exp-<N>` Hybrid mode. (DID NOT EXERCISE.)
8. **No new Tier-0 invariant**. C2/C3 DEFER continues per M2-close verdict.
9. **No cross-file diff** by meta-agent ever (sandbox enforces).
10. **No shadow-set leakage to meta-agent**. Aggregate `{shadow_regression_detected, drop_pct}` only.
11. **No mutation of `eval_interactive/results/` schema**.
12. **No editing of `docs/codex-findings.md` during M-Auto-1C execution**. The live scaffold receives content at M-Auto-1C close (this milestone-shared Codex writes).
13. **No modification of `autoloop/autoloop/scoring/` baseline files** locked against M-Auto-1B S-Auto-7 close content hash `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`.
14. **No hardcoding of specific eval case wording / user utterance / expected answer / case-status label** in any anti-hardcode detector rule or cherry-pick Skill YAML edit.
15. **No LLM call inside detector or content_validator** (per D1 regex-heuristic-only rule).
16. **No promotion of `gaming.observation_only_in_v1`** from `true` to `false` in M-Auto-1C.
17. **At MOST 1 cherry-pick** during S-Auto-8.
18. **CONTROLLED OVERRIDE on `autoloop/autoloop/sandbox/applier.py`**: conditionally writable in **S-Auto-7.2 ONLY** for the OQ-S61.1 mvn module-selection fix at lines 370-378 + the OQ-S62.2 `_git_create_branch` idempotency fix (post-hoc within same fence envelope). Cumulative S-Auto-7.2 fence #18 envelope on applier.py = ~50 LOC across commits `07eab09` (mvn fix) + `7183c20` (branch idempotency). S-Auto-8 MUST NOT edit `applier.py` (verify zero S-Auto-8 touch).
19. **NEW (M-Auto-1C-only POST-HOC controlled override per §8.3 in-place revision 2026-05-30)**: `autoloop/autoloop/sandbox/content_validator.py` rule 3 length_overflow scope is conditionally writable in **S-Auto-7.2 ONLY** for the OQ-S62.1 fix (rewrite to `overflow_cap = max(overflow_ratio * before_len, float(absolute_ceiling))` with new tunable knob `length_overflow_absolute_ceiling: 1000` in `autoloop/config.yaml`). Authorization: STOP-2 mid-sprint AskUserQuestion 2026-05-30. S-Auto-8 MUST NOT edit `content_validator.py` (verify zero S-Auto-8 touch; S-Auto-8 only bumped the ceiling KNOB in `autoloop/config.yaml` from 1000 → 1200, not the rule itself).

### 6.1 OQ-S56.1 disposition (inherited UNCHANGED)

The blessing for `eval_interactive/eval_interactive.yaml` env-var indirection carries forward UNCHANGED.

---

## Embedded §4.1 nine-question anti-hardcode kernel (verbatim from `docs/current/anti-hardcode-review-kernel.md`)

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

---

## Verification axes (M-Auto-1C-shared — 9 axes; walk in order)

### Axis Mc1 — §4.1 nine-question kernel walk (cumulative)

Walk all 9 questions on the cumulative `586f138..<close-bundle>` agent-loop scope, treating S-Auto-7.2 per-sub-sprint findings as input (do NOT re-litigate them). Focus weight on:

- **S-Auto-8 commits** `2653aac` (cv ceiling 1000→1200; `autoloop/config.yaml` config-governance; expected EXEMPT) + `4ffb86c` (handoff; docs-only; expected EXEMPT).
- **Wrapper scripts** `scripts/run-overnight.sh` + `scripts/launch-overnight.py` (NEW at S-Auto-8; expected pure-infra carve-out; abandoned mid-sub-sprint per OQ-S62.3 expansion; verify no semantic logic encoded).
- **Cumulative composition** of S-Auto-7.2 (already-reviewed PASS) + S-Auto-8 → does any cumulative §1.7 boundary cross emerge that wasn't visible at the per-sub-sprint pass?

Expected verdict: `approve` with scope-exemption note (S-Auto-8 is pure-infra + config-governance + docs-only execution attempt; no semantic decision encoded).

### Axis Mc2 — Cumulative §1.7 boundary check on the cv ceiling 1000→1200 bump

The S-Auto-8 dev session bumped `autoloop/config.yaml` `content_validator.length_overflow_absolute_ceiling` from 1000 → 1200 after the exp-13 smoke iter produced a 1089-char `after_value` on a 143-char `before_value` field (`escalate.yaml $.procedure`). This is a config-governance knob refinement, NOT a rule rewrite (fence #19 governs the rule itself; that rewrite landed in S-Auto-7.2 + was per-sub-sprint reviewed).

Verify:

1. The bump is a single-knob tuning under the §8.3 in-place revision controlled override pattern (no NEW fence override required; the knob lives in the `content_validator:` config block established at S-Auto-7.2).
2. The single-data-point calibration is documented as `OQ-cv-ceiling-calibration-thin-evidence` in `docs/sprints/sprint-063-handoff.md` §8 (ledger-only acknowledgment; not auto-promoted to R-item).
3. The bump does NOT cross §1.7 (it's a numeric threshold; not a keyword / regex / if-else / enum / per-UC matrix).

Expected: PASS with observation that future overnight evidence may surface further calibration adjustments (the OQ-cv-ceiling-calibration-thin-evidence is a forward-planning note, not a blocker).

### Axis Mc3 — Sub-sprint dispositions table verification

Verify against `docs/milestone_objective.md` §12.2:

| Sub-sprint | Verdict | Class | Codex |
|---|---|---|---|
| S-Auto-7.2 / Sprint 062 | CLOSED 2026-05-30 | A — Clean close at sub-sprint level | `pass / 0` `approve with downgrade-to-signal follow-up` per-sub-sprint per §4.3 trigger #3 |
| S-Auto-8 / Sprint 063 | CLOSED 2026-05-30 with STOP-and-surface | PARTIAL at sub-sprint level (Class C-style carryover) | Milestone-shared (this review) |

Verify the dispositions are coherent (S-Auto-7.2 substrate scope fully landed; S-Auto-8 substrate validation through Step 6 + drift envelope + R-S58 baseline + cv tuning landed but overnight blocked + cherry-pick deferred; both dispositions accurate vs handoff content).

### Axis Mc4 — Acceptance bar audit (13 hard gates from §5; 4 unmet items explicitly inherited to M-Auto-2 per §12.4)

Walk the 13 gates from `docs/milestone_objective.md` §5 + verify the §12.1 tally:

- Gates 1-3 PASS (Tier-0 safety floor + Java baseline + Python baseline UNCHANGED — verify via `git diff --stat 586f138..<close-bundle> -- server/src/main/java/ eval/src/main/java/ eval_interactive/eval_interactive/`).
- Gate 4 FAIL (live iter through Step 9 NOT met) — verify M-Auto-2 inheritance per §12.4 + §12.9 sub-sprint sequence (S-Auto-9 + S-Auto-10).
- Gate 5 PASS (drift envelope ≥2 reruns; 0/46 case_passed drift) — verify via S-Auto-8 §3 run-IDs.
- Gate 6 FAIL (overnight ≥10 iters NOT met; 0/15) — verify M-Auto-2 inheritance.
- Gate 7 N/A (no kept candidates because overnight blocked) — verify M-Auto-2 inheritance.
- Gate 8 N/A (no candidates) — verify M-Auto-2 inheritance.
- Gate 9 PASS (§5.6 close-day rerun by deliver-agent at close-bundle prep; results in `_manifest.md` M-Auto-1C close section).
- Gate 10 PASS (shadow rerun consistent with M-Auto-1B baseline envelope).
- Gate 11 PASS (OQ-S61.1 RESOLVED via S-Auto-7.2 commit `07eab09` + Codex Axis C reproduction).
- Gate 12 PASS (R-S58 CLOSED-AS-THEORETICAL-ONLY; 0/13 historical Cf chars; reopen condition documented).
- Gate 13 PENDING (this milestone-shared Codex review).

Verify that the §12 closure verdict accurately reports 8 PASS + 3 FAIL + 2 N/A (excluding gate 13 pending) and that the 4 unmet/N/A gates have a clear M-Auto-2 inheritance path.

### Axis Mc5 — Cumulative hard-fence verification (against §6 17+2 fences)

Walk all 17+2 fences against `git diff --stat 586f138..<close-bundle>` for the cumulative range. Spot-check commands:

```bash
# Java zero-touch
git diff --stat 586f138..HEAD -- server/src/main/java/ eval/src/main/java/
# Expected: empty

# Eval-interactive byte-identical
git diff --stat 586f138..HEAD -- eval_interactive/eval_interactive/ eval_interactive/case_specs/ eval_interactive/case_specs_shadow/
# Expected: empty (manifest update at close-prep is _manifest.md only; expected as deliver-agent close-bundle update per §5.6 cadence)

# Skill YAML zero-touch (no cherry-pick landed)
git diff --stat 586f138..HEAD -- server/src/main/resources/skills/
# Expected: empty

# Other server resources zero-touch
git diff --stat 586f138..HEAD -- server/src/main/resources/prompts/ server/src/main/resources/scripts/ server/src/main/resources/config/ server/src/main/resources/mock/
# Expected: empty

# Data + db zero-touch
git diff --stat 586f138..HEAD -- data/ db/
# Expected: empty

# Scoring fence #13 byte-identical
git diff --stat 586f138..HEAD -- autoloop/autoloop/scoring/
# Expected: empty (zero touch across M-Auto-1C cumulative range)

# Verify scoring SHA still 22548e20…
cd autoloop && uv run python -c "from autoloop.scoring.gaming import _check_scoring_code_drift; from autoloop.config import load; cfg = load('config.yaml'); print(_check_scoring_code_drift(cfg))"
# Expected: []

# Fence #18 envelope on applier.py: only S-Auto-7.2 commits touched it
git diff --stat 586f138..HEAD -- autoloop/autoloop/sandbox/applier.py
# Expected: shows the S-Auto-7.2 modifications (07eab09 mvn + 7183c20 idempotency); S-Auto-8 zero touch

# Fence #19 envelope on content_validator.py: only S-Auto-7.2 commit touched it
git diff --stat 586f138..HEAD -- autoloop/autoloop/sandbox/content_validator.py
# Expected: shows the S-Auto-7.2 OQ-S62.1 rule 3 rewrite (7183c20); S-Auto-8 zero touch

# Other autoloop sandbox files zero-touch
git diff --stat 586f138..HEAD -- autoloop/autoloop/sandbox/anti_hardcode_check.py autoloop/autoloop/sandbox/gaming.py
# Expected: empty (gaming.py actually lives in scoring/ per OQ-prompt-doc-sandbox-gaming-path-typo; this command will correctly show no sandbox/gaming.py exists)

# Other autoloop fences zero-touch
git diff --stat 586f138..HEAD -- autoloop/autoloop/loop.py autoloop/autoloop/meta_agent/ autoloop/autoloop/memory/ autoloop/autoloop/preflight.py autoloop/autoloop/cli.py
# Expected: empty

# Sprint archive zero-touch
git diff --stat 586f138..HEAD -- docs/sprints/sprint-001-*.md docs/sprints/sprint-061-*.md docs/milestones/
# Expected: empty (new docs/sprints/sprint-062-*.md + sprint-063-*.md are NEW additions per the close-bundle, not edits to existing sprint archives)

# Docs governance zero-touch
git diff --stat 586f138..HEAD -- docs/foundational/ docs/runtime_freeze_and_risk_policy.md docs/current/ docs/teams/
# Expected: empty
```

All 17+2 fences expected to PASS cumulative.

### Axis Mc6 — §5.6 bad-case manual review + close-day rerun verification

The §5.6 PRIMARY GATE close-day rerun was executed by the deliver-agent at M-Auto-1C close-prep against the foreground :8080 backend. Verify:

1. `eval_interactive/case_specs/bad_cases/_manifest.md` carries a NEW "M-Auto-1C close" section documenting: (a) per-suite run-IDs + parallel settings + elapsed times; (b) per-case PASS / FAIL / IMPROVING joint judgment between deliver-agent + human; (c) suite-aggregate PRIMARY GATE verdict (PASS at suite level expected).
2. The close-day rerun PASS verdict is consistent with the S-Auto-8 §3 pre-batch baseline drift envelope (which already showed 0/46 case_passed drift across 2 reruns each); the close-day rerun is the THIRD independent reference point.
3. The bot is byte-identical on agent-loop scope (zero Skill YAML edit because no cherry-pick landed); any observed drift between close-day rerun and prior baselines is attributable to LLM-provider variance, not bot-code regression.

### Axis Mc7 — OQ-S62.3 expansion investigation evidence walk (NEW; this is the critical close-decision axis)

The M-Auto-1C close verdict (Class C in-flight downgrade) is anchored on the OQ-S62.3 expansion blocking overnight execution. Verify:

1. **OQ-S62.3 investigation scope was thorough**: 9 launch attempts spanning Claude Code Bash + user iTerm + Python wrapper with `subprocess.Popen(start_new_session=True)` + GNU screen detached (with + without caffeinate) + caffeinate-wrapped with all 3 sleep-prevention assertions confirmed via `pmset -g assertions` + AC power + lid open + free memory tested at 108MB (OOM expected) AND 7.5GB (no pressure). `docs/sprints/sprint-063-handoff.md` §8 documents all 9 attempts in a table.
2. **Substrate-fix verification is complete through Step 6**: every overnight launch attempt reached Step 6 mvn spawn successfully + Spring boot was observed alive at alt ports across multiple attempts; the autoloop substrate code (post-S-Auto-7.2 fixes) is NOT the proximate cause.
3. **No matching kernel jetsam log entries**: dev confirmed via `log show --predicate 'eventMessage CONTAINS "memorystatus"'` or equivalent that memorystatus kill subsystem is silent for the killed PIDs.
4. **Decision is M-Auto-2 prerequisite**: M-Auto-1C closure verdict §12.3 explicitly classifies OQ-S62.3 as M-Auto-2 first sub-sprint S-Auto-9 scope (local-Mac in-env diagnostic instrumentation; NO cloud / remote server per human direction).

If your independent walk of §12.3 disagrees with any of the above, surface as a downgrade trigger (not a blocker — the close verdict is Class C in-flight downgrade by design; downgrade triggers refine the M-Auto-2 plan, not block the M-Auto-1C close).

### Axis Mc8 — R-item flips verification

Verify against `docs/action_bank.md` §5.2:

1. **R-S58 CLOSED-AS-THEORETICAL-ONLY**: annotation added 2026-05-30 at M-Auto-1C close with empirical evidence (0/13 historical Cf chars across all `experiments.jsonl` rows since M-Auto-1A close) + reopen condition (any future overnight propose-distribution scan with ≥1 Cf observation re-promotes to ACTIVE). Verify the annotation is internally consistent + the reopen condition is operationally clear for M-Auto-2 close-day re-scan.
2. **R-eval-interactive-judge-score-never-populated UNCHANGED status with annotation update**: S-Auto-8 §3 baseline 6× evidence added (all 6 runs show `mean_judge: 0.0000`); LOW priority M-Auto-2+ observability hygiene confirmed; loader-counted `case_passed` continues as canonical signal. Verify the annotation is consistent with the M-Auto-1B Phase 2 original opening + does NOT priority-bump (per joint AskUserQuestion 2026-05-30).
3. **No NEW R-items opened**: OQ-S62.3 expansion becomes M-Auto-2 scope rather than R-item; OQ-cv-ceiling-calibration-thin-evidence + OQ-prompt-doc-sandbox-gaming-path-typo recorded in handoff §8 as ledger-only. Verify the ledger-only treatment is justified (cv ceiling is a thin-calibration observation that future overnight evidence may surface further bumps; the gaming.py path typo is a minor doc-only inconsistency for next prompt template fold-back).

### Axis Mc9 — Cross-cutting reproducibility check

Spot-check reproducibility:

1. `cd autoloop && uv run --extra dev pytest -q` returns ≥266 PASS, 1 warning (S-Auto-7.2 baseline; S-Auto-8 typically adds 0 new tests).
2. `cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py` returns 31 PASS UNCHANGED.
3. `cd eval_interactive && uv run python -m pytest --tb=no -q` returns `486 passed, 3 failed` UNCHANGED.
4. Java test suite NOT re-run by deliver-agent this sub-sprint per S-Auto-8 handoff §10 gate 4 NOT_REVERIFIED entry. Verify `git diff --stat 586f138..HEAD -- server/ eval/src/main/java/` empty (zero Java touch implies baseline preserved without explicit rerun).
5. `_check_scoring_code_drift(config) == []` silent (scoring SHA `22548e20…` REASSERTED).
6. `git log --oneline 586f138..HEAD` shows the 7+2 commits enumerated in the cumulative range; no additional ambient commits creep in. (If any ambient human commit appears in the range, disposition as Axis I-style observation per S-Auto-7 + M-Auto-1B precedent; should not be a blocker unless it touches §6 fence #1 or #3 or #13.)

---

## Output format

Write your verdict to `docs/codex-findings.md` using the §4.2 sprint-close header format:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph summarizing the cumulative agent-loop scope verdict + the Class C in-flight downgrade context + any downgrade-to-signal triggers for M-Auto-2>
```

PLUS for EACH of the 9 axes Mc1-Mc9:

```
### Axis Mc<N> — <axis name>
verdict: PASS | PASS-with-CONCERN | FAIL
evidence: <code path / file:line / git diff stat / sub-sprint handoff section reference>
concern (if PASS-with-CONCERN or FAIL): <one paragraph>
```

PLUS a final "Downgrade-to-signal triggers" subsection enumerating any non-blocking refinements you recommend for M-Auto-2 (e.g., recommended additions to S-Auto-9 scope; OQ-S62.3 investigation methodology suggestions; cv ceiling calibration recommendations).

---

## Constraints

- **You do NOT edit code.** Any concern surfaced becomes a downgrade trigger for the deliver-agent + human to disposition at the post-Codex close-bundle commit.
- **You do NOT re-judge §5.6 bad-case verdicts** — those are human-judgment gates per `iteration_governance.md` §5.6. Your role is to verify the deliver-agent + human joint review was conducted at close-bundle prep + the verdict is recorded in `_manifest.md`.
- **You do NOT re-litigate the S-Auto-7.2 per-sub-sprint findings** — verify cumulative composition only.
- **You do NOT propose alternate scope** for M-Auto-2 — the deliver-agent + human own M-Auto-2 planning; your downgrade-to-signal triggers are refinements, not scope decisions.
- **Per §4.3 your verdict set** (§4.1): `approve` / `approve with downgrade-to-signal follow-up` / `reject as semantic hardcode` / `needs human architecture decision`. Class C in-flight downgrade at the milestone-close level is EXPECTED and does NOT itself trigger a `fix_required` Codex verdict — the deliver-agent + human classify the milestone close; your Codex verdict is on the code + scope + governance compliance of what shipped (which is substrate-validation + drift envelope + R-S58 baseline + close artefacts, ALL pure-infra + config-governance + docs-only execution attempts).

Expected verdict given the cumulative composition: `approve with downgrade-to-signal follow-up` (acknowledging the OQ-S62.3 expansion as a downgrade trigger naming M-Auto-2 S-Auto-9 as the conversion sprint where the substrate-execution-environment problem gets diagnosed + resolved). `approve` is also acceptable if you find no downgrade-worthy refinement.
