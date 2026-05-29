# Codex review prompt — Milestone M-Auto-1B — Auto-Evolution Calibration — Milestone-shared Anti-Hardcode review at SPLIT close (§4.3 default + §8.5 split decision verification)

You are the **Milestone-Shared Anti-Hardcode Review Agent for Milestone M-Auto-1B — Auto-Evolution Calibration**. This is the **milestone-shared Codex pass** dispatched per `docs/current/iteration_governance.md` §4.3 default at M-Auto-1B close. M-Auto-1B is being **CLOSED WITH AN INCOMPLETE ACCEPTANCE BAR** per the human-locked §8.5 split decision 2026-05-29 (close at S-Auto-7.1; open new milestone M-Auto-1C for the deferred S-Auto-7.2 applier fix + S-Auto-8 overnight + cherry-pick scope). Your verdict feeds the deliver-agent + human's milestone-close classification (likely B — Surfaced findings need fix-iteration or A-with-acceptance-bar-revision at the milestone level; sub-sprint S-Auto-7.1 itself is A — Clean PASS per the deliver-agent + human joint decision).

**Two per-sub-sprint Codex reviews have ALREADY been completed** and are inputs to this milestone-shared pass (do NOT re-litigate the per-sub-sprint findings):

1. **S-Auto-5 / Sprint 058** per-sub-sprint review (§4.3 trigger #2 — Fix-C step 1 touched the §1.7 anti-hardcode structural guard) returned `decision: pass / blocking_count: 0` 2026-05-28 with §4.1 verdict `approve with downgrade-to-signal follow-up`. Trigger = `R-S58-anti-hardcode-zero-width-when-arrow-bypass` opened as Codex-Axis-C-surfaced M-Auto-1B / Fix-D candidate; the Codex Axis B exact bypass `Whenever ... =>` now FAILs the detector via Fix-C; calibrated via 3 real meta-agent samples + 17 fixture sweep (augmented-evidence path per OQ-S58.4); `synonym_map_enabled: false → true` Path A flip. Closed `R-S57-anti-hardcode-whenever-arrow-synonym-bypass`. Archived at `compact/sprint-058-codex-review-prompt.md` (the prompt; the verdict content was prepended to the live `docs/codex-findings.md` at S-Auto-5 close 2026-05-28).

2. **S-Auto-7 / Sprint 060** per-sub-sprint review (§4.3 trigger #3 — fence #13 controlled override on `eval_runner.py` planning-blessed + fence #2 in-session override on `loader.py` human-authorized) returned `decision: pass / blocking_count: 0` 2026-05-29 with §4.1 verdict `approve with downgrade-to-signal follow-up`. Trigger = annotate `docs/milestone_objective.md` §6 fence #2 with controlled-override note for `loader.py` parallel to fence #13 annotation; **ADDRESSED in the S-Auto-7 close-bundle commit `307f69a`** (fence #2 now self-documents the in-session controlled override; Codex Axis B reproducer pointer in place). Archived at `compact/sprint-060-codex-review-prompt.md` (the prompt; the verdict content is in the live `docs/codex-findings.md` at the time you start this review — DO NOT re-litigate the S-Auto-7 axes A-I; your milestone-shared pass extends them cumulatively).

**S-Auto-6 / Sprint 059** closed EMPTY Class C — In-flight downgrade 2026-05-29; no Codex (no semantic-touching delta shipped). **S-Auto-7.1 / Sprint 061** Codex review plan was DEFAULT milestone-shared (no §4.3 per-sub-sprint trigger fired — pure infra + scope-respected + scoring SHA reasserted); this milestone-shared review is its primary Codex gate.

The cumulative commit range under review for THIS milestone-shared pass:

```
b6b627b (exclusive)..<S-Auto-7.1-close-sha> (inclusive)

b0ce174  Sprint 058 / S-Auto-5 / M-Auto-1B — M-Auto-1A live-iter bootstrap completion (anthropic SDK dep)
ae0ec3e  Sprint 058 / S-Auto-5 / M-Auto-1B — live-iter bootstrap + Fix-C step 1 + calibration evidence + step 2 Path A
1f51ae8  Sprint 058 / S-Auto-5 close-bundle — per-sub-sprint Codex prompt + 10-handoff + action_bank refresh
6e8692d  M-Auto-1B planning round — milestone + Sprint 058 / S-Auto-5 sub-sprint contract + dev prompt + research proposal
         (NB: 6e8692d is the M-Auto-1B planning commit BEFORE b0ce174 + ae0ec3e per git log — confirm via `git log --oneline b6b627b..HEAD`)
1943ed5  Sprint 059 / S-Auto-6 — close empty (Class C in-flight downgrade; substrate pre-flight blockers A + B surfaced)
b6084f9  Sprint 059 / S-Auto-6 close-bundle — deliver-agent close-empty + S-Auto-7 contract + dev prompt + milestone in-place revision 2 → 4 sub-sprints
60c5b67  Architecture README (standalone documentation commit; NOT part of any sub-sprint scope; out-of-scope-but-clean per S-Auto-7 Codex Axis I)
559927a  Sprint 060 / S-Auto-7 — Blocker B fix path (b) + scoring SHA rebaseline 5177b674→22548e20 + 5 new tests
b0a3704  Sprint 060 / S-Auto-7 — Blocker A baseline_dir blessing + Blocker C in-session loader.py fix + handoff
307f69a  Sprint 060 / S-Auto-7 close-bundle — deliver-agent close + S-Auto-7.1 contract + dev prompt + milestone in-place revision 4 → 5 sub-sprints + §6 fence #2 annotation
19213b1  Sprint 061 / S-Auto-7.1 — Pre-flight env check + OQ-S60.10 dotenv + smoke iter (Goal #3 BLOCKED on OQ-S61.1 applier mvn module-selection)
<S-Auto-7.1-close-bundle-sha>  Sprint 061 / S-Auto-7.1 close-bundle — deliver-agent close + M-Auto-1B milestone-shared Codex prompt + §0 + §1 + action_bank Sprint 61 row
                              (this is the commit that BUNDLES THIS REVIEW PROMPT itself; final SHA will be the close-bundle commit human creates BEFORE dispatching this prompt to you)
```

12 commits in cumulative range (verify via `git log --oneline b6b627b..HEAD`); 5 sub-sprints (S-Auto-5 + S-Auto-6 [empty] + S-Auto-7 + S-Auto-7.1 + the S-Auto-7.1 close-bundle); 3 per-sub-sprint Codex reviews (S-Auto-5 + S-Auto-7 + this milestone-shared pass which doubles as S-Auto-7.1 Codex by default).

The previous milestone (M-Auto-1A — Auto-Evolution Build) closed at commit `b6b627b` (A — Clean PASS sub-classified `approve with downgrade-to-signal follow-up` 2026-05-28). All Java + eval-interactive Python baselines from M-Auto-1A close are the inherited baselines this milestone-shared review compares against.

---

## Read order (minimal)

Read only:

1. `AGENTS.md` (auto-loaded via constitution chain — pulls `doc_governance.md` + `agent_context_guide.md` + `iteration_governance.md`).
2. This prompt (self-contained executable view per `iteration_governance.md` §9 invariant).
3. **The live `docs/codex-findings.md`** — contains the S-Auto-5 + S-Auto-7 per-sub-sprint Codex verdicts; your milestone-shared pass starts from those findings and extends them cumulatively. DO NOT re-litigate the per-sub-sprint axes; DO confirm the verdicts still hold when S-Auto-7.1 is added to the cumulative composition.
4. **The four sub-sprint handoffs** (the dev's own close artefacts):
   - `docs/sprints/sprint-058-handoff.md` — S-Auto-5: live-iter bootstrap + Fix-C step 1 + calibration evidence + step 2 Path A (closed R-S57; opened R-S58)
   - `docs/sprints/sprint-059-handoff.md` — S-Auto-6: close-empty Class C (substrate pre-flight blockers A + B surfaced)
   - `docs/sprints/sprint-060-handoff.md` — S-Auto-7: substrate fix (Blocker A baseline blessing + Blocker B eval_runner fix path (b) + Blocker C in-session loader.py + scoring SHA rebaseline 5177b674→22548e20); Goal #4 BLOCKED at Step 6 alt-port Spring spawn → S-Auto-7.1 fix-iteration
   - `docs/sprints/sprint-061-handoff.md` — S-Auto-7.1: pre-flight env check + OQ-S60.10 dotenv + smoke iter (Goal #3 BLOCKED on NEW OQ-S61.1 applier.py:370 mvn module-selection bug; csagent-parent packaging=pom + `-pl server -am` selects parent into reactor)
5. **The per-sub-sprint Codex prompts** (input-only; for cross-reference if you need to understand what was already verified):
   - `compact/sprint-058-codex-review-prompt.md` — S-Auto-5 per-sub-sprint review prompt (Axis A-I; verdict `pass / 0` `approve with downgrade-to-signal follow-up`)
   - `compact/sprint-060-codex-review-prompt.md` — S-Auto-7 per-sub-sprint review prompt (Axis A-I; verdict `pass / 0` `approve with downgrade-to-signal follow-up`; governance trigger satisfied at S-Auto-7 close-bundle)

You may sample (NOT embed) the following code paths for verification:

- `autoloop/autoloop/sandbox/anti_hardcode_check.py` — Fix-C step 1 word-boundary regex `_RE_WHEN_WORD_BOUNDARY` in `_normalize`; rule count 11 (≤30); detector self-discipline 3 regression tests PASS (S-Auto-5 + S-Auto-7 + S-Auto-7.1 zero-touch).
- `autoloop/autoloop/scoring/eval_runner.py` — S-Auto-7 Blocker B fix path (b) removes `--output-dir` + adopts auto-timestamp via set-diff + symlink staging; verify Codex Axis B (S-Auto-7) findings still hold.
- `autoloop/autoloop/scoring/{tier_evaluator,baseline_loader,gaming}.py` — fence #13 other-three siblings byte-identical to M-Auto-1A close `5177b674…→22548e20…` controlled rebaseline at S-Auto-7 only.
- `autoloop/config.yaml` — fitness.baseline_dir (S-Auto-7 blessed `eval_interactive/results/m-auto-1b-baseline-20260529`); fitness.scoring_code_baseline_sha (S-Auto-7 rebaselined `22548e20…`); anti_hardcode.synonym_map_enabled (S-Auto-5 Path A flip `false → true`).
- `eval_interactive/eval_interactive/case_spec/loader.py` — S-Auto-7 in-session controlled override (rglob + skip `_*.yaml`); fence #2 annotation in milestone_objective.md §6 covers this.
- `autoloop/autoloop/preflight.py` — NEW at S-Auto-7.1; 6 pre-flight checks + auto_reboot helper; pure-infra.
- `autoloop/autoloop/cli.py` — S-Auto-7.1 extension: `preflight` subcommand + `--auto-reboot` / `--skip-preflight` flags + OQ-S60.10 dotenv auto-load helper.
- `autoloop/autoloop/sandbox/applier.py:370` — the NEW OQ-S61.1 bug surface (out of M-Auto-1B scope; will be S-Auto-7.2 fix in M-Auto-1C; verify it's NOT touched in S-Auto-7.1 range).
- `autoloop/tests/test_preflight.py` — 27 new tests (S-Auto-7.1 +27 = 228 → 255 PASS).
- `pom.xml` — confirm `csagent-parent` packaging=pom + `<modules>server + eval</modules>` (root cause of OQ-S61.1).
- `docs/milestone_objective.md` — milestone live (will be archived AFTER your verdict to `docs/milestones/M-Auto-1B_objective.md`); read §1 milestone class + §2 goal + §3 sub-sprint sequence + §5 acceptance bar (15 hard gates) + §6 hard fences (17 items including fence #2 + #13 annotations) + §7 R-items + §10 stop conditions + §12 closure verdict draft (Codex outcome filled AFTER your verdict).
- `docs/action_bank.md` §5 — R-S57 closed at S-Auto-5; R-S58 OPEN (M-Auto-1C / M-Auto-2 candidate); other carry-overs from M-Auto-1A.

Do **not** re-read the entire `docs/codex-findings.md` if you've already seen it on the prior per-sub-sprint passes — sample only the cross-reference sections.

---

## Embedded milestone class + Codex review plan (from `docs/milestone_objective.md` §1 + §8)

| Sub-sprint | Layer (primary) | §7 stanza | Codex review |
|---|---|---|---|
| S-Auto-5 / Sprint 058 — Live-iter bootstrap + Fix-C step 1 + calibration evidence + step 2 Path A | `infra` + `eval_spec` | REQUIRED | **Per-sub-sprint COMPLETE (verdict `pass / 0` `approve with downgrade-to-signal follow-up`); this milestone-shared pass cross-checks cumulative composition** |
| S-Auto-6 / Sprint 059 — Close empty Class C (substrate pre-flight blocked) | N/A (no semantic delta) | EXEMPT | None (no Codex per close-empty — no scope shipped) |
| S-Auto-7 / Sprint 060 — Substrate fix (Blocker A + Blocker B fix path (b) + Blocker C in-session + scoring SHA rebaseline) | `infra` | EXEMPT (self-walked; pure-infra carve-out) | **Per-sub-sprint COMPLETE (verdict `pass / 0` `approve with downgrade-to-signal follow-up`; governance trigger satisfied at S-Auto-7 close-bundle); this milestone-shared pass cross-checks cumulative composition** |
| S-Auto-7.1 / Sprint 061 — Pre-flight env check + OQ-S60.10 dotenv + smoke iter (Goal #3 BLOCKED on OQ-S61.1 → M-Auto-1C) | `infra` | EXEMPT (self-walked; pure-infra carve-out) | **Milestone-shared (this review) — no §4.3 per-sub-sprint trigger fired in S-Auto-7.1** |

The §4.3 trigger #4 (prior sub-sprint fix_required outcome) did NOT apply at S-Auto-7.1 open because S-Auto-7 closed `approve with downgrade-to-signal follow-up`, not `fix_required`. The S-Auto-7 governance trigger was a docs-only milestone_objective annotation, addressed in the S-Auto-7 close-bundle and not requiring per-sub-sprint re-review of S-Auto-7.1.

---

## Embedded milestone goal (from `docs/milestone_objective.md` §2)

Stand up the **first real run** of the auto-evolution loop end-to-end and bring the anti-hardcode detector to **calibration-validated state** against real meta-agent propose outputs. At M-Auto-1B close, M-Auto-1A's substrate was supposed to be demonstrated to:

1. ✓ Complete live-iter prerequisites: built `server/` jar; `AUTOLOOP_META_LLM_API_KEY` live; clean working tree on `auto-loop-branch` (S-Auto-5 LANDED).
2. ✓ Run ≥1 real iteration to a verdict (S-Auto-5 dispatched 4 live iterations; 3 propose samples captured).
3. ✓ Produce ≥10 real meta-agent propose samples for detector calibration (S-Auto-5 captured 3 real + augmented-evidence path via OQ-S58.4 = 3 real + 17 fixture = 20 calibration data points; FLAG rate 0%, FP rate 0% on clean prose).
4. ✓ Close `R-S57-anti-hardcode-whenever-arrow-synonym-bypass` via Fix-C hybrid (S-Auto-5: Fix-C step 1 word-boundary regex + Fix-C step 2 Path A `synonym_map_enabled: false → true`).
5. ✗ Run first overnight batch (10-20 iterations, 6-8h budget): **BLOCKED** by OQ-S58.7 / OQ-S60.7 / OQ-S61.1 chain (Spring spawn brittleness root-cause now characterized as `applier.py:370` mvn module-selection bug on `csagent-parent` packaging=pom).
6. ✗ First human review of overnight kept candidates: NOT REACHED.
7. ✗ First cherry-pick to main: NOT REACHED.

**Acceptance bar reality**: 8/15 hard gates met (`docs/milestone_objective.md` §5); 7/15 unmet because all depend on a working smoke iter through Step 9 (eval_runner + tier_evaluator). **The §8.5 split decision at S-Auto-7.1 close opens M-Auto-1C to address the OQ-S61.1 applier fix (S-Auto-7.2) and the deferred overnight + cherry-pick scope (S-Auto-8).**

**Dataset scope (v1)**: per-iteration fitness = `bad_cases` (12) + `anchor_outcome` (12) + `shadow` (22; NOT 23 because the 23rd entry was `_manifest.yaml` correctly filtered by the S-Auto-7 Blocker C loader fix) = **46 cases** (corrected from 47 per the dev prompt expectation). The `anchor` 159-case suite is intentionally NOT used as a per-iteration fitness signal in v1.

**Layer 0 scope (v1)**: unchanged from M-Auto-1A — Python `hard_checks` Tier-0 family; legacy Java replay (`R-eval-java-module-retirement`) deferred to M-Auto-2+.

**Crucially M-Auto-1B ships NO Skill YAML edits and NO cherry-picks to main.** Cherry-pick was planned for S-Auto-6 (allowed EXACTLY ONCE per §6 fence #7) but never executed because S-Auto-6 closed empty. Subsequent sub-sprints (S-Auto-7, S-Auto-7.1) were pure-infra substrate-fix + ergonomics; no `server/src/main/resources/skills/*.yaml` touched.

---

## Embedded milestone §6 hard fences (verbatim from `docs/milestone_objective.md` §6 with the S-Auto-7 close-bundle controlled-override annotations)

All 17 fences. Your hard-fence walk uses this list as the canonical reference:

1. **No edits** to `server/src/main/java/**`. Runtime byte-identical to M-Auto-1A close `b6b627b` (= M5 close `c9390dc`).
2. **No edits** to `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`. Evaluator byte-identical. **S-Auto-7 CONTROLLED OVERRIDE on `eval_interactive/eval_interactive/case_spec/loader.py` ONLY** (in-session human-authorized 2026-05-29; substrate path-handling repair; zero-impact negative-control on 6 flat case_set dirs; per-sub-sprint Codex verified at S-Auto-7 close; annotation added to milestone_objective.md §6 at S-Auto-7 close-bundle 307f69a). All OTHER files in `eval_interactive/eval_interactive/**` byte-identical for the remainder of M-Auto-1B.
3. **`server/src/main/resources/skills/*.yaml`** conditionally writable EXACTLY ONCE during S-Auto-6 cherry-pick — but **NEVER EXERCISED** (S-Auto-6 closed empty; S-Auto-7/7.1 substrate-only). `{prompts,scripts,config,mock}/**` byte-identical.
4. **No edits** to `data/**`, `db/migration/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/iteration_governance.md`, `docs/teams/**`.
5. **No edits** to sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-057-*` or any prior milestone archive.
6. **No `git add -A`** by dev. Stage only sub-sprint scope files explicitly.
7. Cherry-pick to main **ALLOWED EXACTLY ONCE** during S-Auto-6 — **NOT EXERCISED** (S-Auto-6 closed empty; S-Auto-7/7.1 substrate-only).
8. **No new Tier-0 invariant**. C2/C3 DEFER continues.
9. **No cross-file diff** by meta-agent. NOT EXERCISED (no propose-stage hypothesis was committed to main in M-Auto-1B).
10. **No shadow-set leakage to meta-agent**. Aggregate only. NOT EXERCISED in propose-stage (no overnight ran); the S-Auto-2 tier_evaluator firewall is structurally enforced.
11. **No mutation of `eval_interactive/results/` schema**. Verified at S-Auto-7 Blocker C loader fix; the loader change does NOT alter on-disk schema, only HOW the loader walks directory tree.
12. **No editing of `docs/codex-findings.md` during M-Auto-1B execution**. Per-sub-sprint Codex content lives in the live file across the milestone; this milestone-shared pass extends it; AFTER your verdict, deliver-agent archives to `docs/milestones/M-Auto-1B_codex-review.md`.
13. **No modification of `autoloop/autoloop/scoring/` baseline files** (`tier_evaluator.py` / `eval_runner.py` / `baseline_loader.py` / `gaming.py`); content-hash was locked at `5177b674…` from M-Auto-1A close. **S-Auto-7 CONTROLLED OVERRIDE on `eval_runner.py` ONLY** (pre-blessed at S-Auto-7 planning round; content-hash rebaselined `5177b674…→22548e20…`; per-sub-sprint Codex verified at S-Auto-7 close). The other three files (`tier_evaluator.py`, `baseline_loader.py`, `gaming.py`) stay byte-identical to M-Auto-1A close.
14. **No hardcoding of any specific eval case wording / user utterance / expected answer / case-status label** in any detector rule. Fix-C step 1 word-boundary regex `\b(?:whenever|when)\b` is generic structural per D2; detector self-discipline 3 regression tests PASS. Verified at S-Auto-5 per-sub-sprint Codex.
15. **No LLM call inside detector or content_validator** (D1). Verified at S-Auto-5 per-sub-sprint Codex; S-Auto-7/7.1 zero-touch.
16. **No promotion of `gaming.observation_only_in_v1`** from `true` to `false` in M-Auto-1B. Verified UNCHANGED.
17. **At MOST 1 cherry-pick** during S-Auto-6 — **NOT EXERCISED** (S-Auto-6 closed empty).

### 6.1 OQ-S56.1 disposition (inherited from M-Auto-1A; UNCHANGED across M-Auto-1B)

The blessing for `eval_interactive/eval_interactive.yaml` env-var indirection (`bot.base_url: http://localhost:8080` → `${CSAGENT_BACKEND_URL}` + 7-line comment block) UNCHANGED across all 4 sub-sprints. No second-edit surface.

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

## Verification axes (M-Auto-1B-shared — 10 axes; walk in order)

### Axis M1 — §4.1 nine-question kernel walk (cumulative; cross-check the per-sub-sprint Codex verdicts hold under cumulative composition)

Walk Q1-Q9 against the cumulative S-Auto-5..S-Auto-7.1 range. Special attention to:

- **Q1**: Does the cumulative range encode any keyword / regex / if-else / enum / per-UC matrix for a semantic decision that the per-sub-sprint reviews missed? The S-Auto-5 Fix-C step 1 `\b(?:whenever|when)\b` is a generic structural pattern (refinement of existing `_SYNONYM_MAP` semantics, not enum expansion — per-sub-sprint Codex confirmed); S-Auto-7 substrate fixes are pure-infra plumbing; S-Auto-7.1 pre-flight is pure-infra environment discovery + reboot ergonomics. Cumulative composition should produce NO new semantic hardcode. Spot-check `autoloop/autoloop/sandbox/anti_hardcode_check.py` for the post-Fix-C state + `autoloop/autoloop/preflight.py` for the new module.
- **Q2**: Cumulative Tier-0 invariant addition? S-Auto-5 + S-Auto-7 + S-Auto-7.1 add NONE; C2/C3 DEFER unchanged.
- **Q4**: Cumulative case_id / closure_criterion / user-utterance literal in code? Detector self-discipline 3 regression tests should PASS unchanged. Sample `autoloop/autoloop/sandbox/anti_hardcode_check.py` source for any literal eval-case-id / specific user utterance / answer / case-status label.
- **Q7**: Cumulative preservation of tool schema / capability boundary / safety floor / grounding floor? Verify via the hard-fence walk in Axis M5.
- **Q8**: Cumulative generalization coverage? S-Auto-5 calibration (3 real + 17 fixture = 20 data points); S-Auto-7 substrate verification (228 PASS pytest + real-CLI baseline + clean baseline_loader.load); S-Auto-7.1 pre-flight 27 tests + operator-verified end-to-end. Adequate for the infra/eval_spec scope.
- **Q9**: Any temporary measures introduced? S-Auto-5 Fix-C step 1 (sunset trigger = M-Auto-2 zero-width / format-character normalization in Fix-D for R-S58); S-Auto-7 controlled fence overrides (both finalized; not "temporary measures" in the §1.7 sense); S-Auto-7.1 pre-flight (permanent ergonomic; not temporary).

Report findings inline. For each question that surfaces a NEW concern beyond what per-sub-sprint Codex already flagged, paste the diff snippet (≤200 chars) + reasoning + the §3 layer name where the fix should target.

### Axis M2 — Cumulative §1.7 boundary check on the Fix-C step 1 word-boundary regex + the optional Fix-B FLAG rule (NOT taken) + the loader.py controlled override + the eval_runner.py controlled override

The S-Auto-5 per-sub-sprint Codex verified Fix-C step 1 in isolation. The S-Auto-7 per-sub-sprint Codex verified the two fence overrides in isolation. Your cumulative check: do these four substrate changes COMPOSE to introduce a §1.7 forbidden-list violation that none of them does alone?

- Fix-C step 1 is generic structural normalization (`whenever` + `when` → `if` via word-boundary regex).
- `eval_runner.py` fix path (b) is subprocess invocation + filesystem auto-timestamp consumption (plumbing).
- `loader.py` is `glob → rglob` + skip `_*.yaml` (path-handling).
- Cumulative: all four are substrate plumbing; no composition introduces a semantic surface.

Verify by spot-check: `git diff b6b627b..HEAD -- autoloop/autoloop/sandbox/anti_hardcode_check.py autoloop/autoloop/scoring/eval_runner.py eval_interactive/eval_interactive/case_spec/loader.py` should show only the documented changes (the Fix-C step 1 regex + the eval_runner adaptations + the loader rglob).

### Axis M3 — Sub-sprint dispositions table verification

Read `docs/milestone_objective.md` §12 closure verdict draft (filled by deliver-agent at S-Auto-7.1 close-bundle; Codex outcome stub left blank for your verdict). Verify the deliver-agent's classifications:

| Sub-sprint | Deliver-agent classification | Your verification |
|---|---|---|
| S-Auto-5 / Sprint 058 | A — Clean PASS sub-classified `approve with downgrade-to-signal follow-up` (R-S57 closed via Fix-C; R-S58 opened as Codex Axis C surface) | Verify per-sub-sprint Codex 2026-05-28 verdict matches; verify R-S57 close + R-S58 open both annotated in `docs/action_bank.md` §5 |
| S-Auto-6 / Sprint 059 | EMPTY Class C — In-flight downgrade (substrate pre-flight blockers A + B surfaced via dev AskUserQuestion gates before any LLM budget consumed; scope deferred to S-Auto-8) | Verify dev commit `1943ed5` is handoff-only (+325 lines, no code changes); verify zero baseline reruns / overnight / cherry-pick / hard-fence overrides / LLM budget consumed |
| S-Auto-7 / Sprint 060 | B — Surfaced findings need fix-iteration sub-classified `approve with downgrade-to-signal follow-up` (substrate fixes ACCEPTED by Codex; Goal #4 BLOCKED → S-Auto-7.1) | Verify per-sub-sprint Codex 2026-05-29 verdict matches; verify governance trigger (§6 fence #2 annotation for loader.py) ADDRESSED in S-Auto-7 close-bundle commit `307f69a` |
| S-Auto-7.1 / Sprint 061 | A — Clean PASS at sub-sprint level (its 5 own goals; #1+#2+#4+#5 met; #3 Goal #3 BLOCKED is structurally OQ-S61.1 belonging to M-Auto-1C scope) | Verify pre-flight infra delivered + operator-verified end-to-end (handoff §4 transcript); verify 27 new tests landed; verify zero fence violation cumulative; verify scoring SHA `22548e20…` reasserted |

### Axis M4 — Acceptance bar audit (15 hard gates from §5; the 7 unmet items justify the §8.5 split decision)

The deliver-agent classified M-Auto-1B's acceptance bar as 8/15 met:

| Met (8) | Unmet (7) |
|---|---|
| Tier-0 safety floor unchanged | Live iter end-to-end (Goal #4 of S-Auto-7 + Goal #3 of S-Auto-7.1 — both BLOCKED) |
| Java baseline preserved (1183 / 1 / 0 / 2) | Pre-batch baseline drift envelope at S-Auto-6 open |
| Python baseline preserved (eval_interactive 486+3; autoloop grew 216→255 +27 from S-Auto-7.1) | First overnight batch executed |
| S-Auto-5 per-sub-sprint Codex `pass / 0` | First human review of overnight kept candidates |
| Detector calibration evidence at S-Auto-5 close (17/4/2 sweep + 3 real samples + 20 calibration data points all PASS) | First cherry-pick decision via AskUserQuestion |
| Real-meta-agent batch calibration at S-Auto-5 (augmented-evidence path per OQ-S58.4) | Curated bad-case suite manual review pass (M-Auto-1B close; defer to Phase 2 evidence collection) |
| R-S57 closed (S-Auto-5 Fix-C hybrid) | Shadow regression-safety gate (M-Auto-1B close; defer to Phase 2 evidence collection) |
| Milestone-shared Codex review at M-Auto-1B close | (Milestone-shared Codex is THIS review — verifying the 7 unmet acceptance bar items + the §8.5 split decision is the cumulative gate this pass evaluates) |

Verify the 7 unmet items all trace to the SAME upstream cause (smoke iter through Step 9 blocked, ultimately by OQ-S61.1 applier.py:370 mvn module-selection on csagent-parent packaging=pom). Confirm that NOT addressing these items in M-Auto-1B is the deliver-agent + human joint decision (not a Codex-blockable scope-cut).

### Axis M5 — Cumulative hard-fence verification (against §6 17 fences + 6.1 OQ-S56.1)

Run:

```bash
git diff --stat b6b627b..HEAD -- \
  server/src/main/java/ eval/src/main/java/ \
  eval_interactive/case_specs/ eval_interactive/case_specs_shadow/ \
  data/ db/migration/ \
  server/src/main/resources/ \
  docs/foundational/ docs/runtime_freeze_and_risk_policy.md docs/current/ docs/teams/
```

Expected: empty (zero edits to all enumerated gated surfaces; cumulative across all sub-sprints).

Spot-check the two CONTROLLED OVERRIDES are correctly scoped:

```bash
# Fence #13 override: only eval_runner.py touched; other three scoring files byte-identical to M-Auto-1A close
git diff --stat b6b627b..HEAD -- autoloop/autoloop/scoring/tier_evaluator.py autoloop/autoloop/scoring/baseline_loader.py autoloop/autoloop/scoring/gaming.py
# Expected: empty

# Fence #2 override: only loader.py touched; eval-interactive CLI + other eval_interactive/eval_interactive/ files byte-identical
git diff --stat b6b627b..HEAD -- eval_interactive/eval_interactive/cli.py
# Expected: empty

# Reproduce S-Auto-7 close scoring SHA + verify S-Auto-7.1 didn't re-touch
cd autoloop && uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"
# Expected: 22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9
```

Verify NO Skill YAML edit landed (fence #3 cherry-pick was reserved for S-Auto-6 which closed empty):

```bash
git diff --stat b6b627b..HEAD -- server/src/main/resources/skills/
# Expected: empty
```

Spot-check sprint archives + milestone archives untouched (fence #5):

```bash
git diff --stat b6b627b..HEAD -- docs/sprints/sprint-001-*.md docs/sprints/sprint-002-*.md ... docs/sprints/sprint-057-*.md docs/milestones/
# Expected: empty for sprint-001..057 + docs/milestones/
# The new docs/sprints/sprint-058-*.md, sprint-059-*.md, sprint-060-*.md, sprint-061-*.md are in-scope NEW archives per §6 fence #5 carve-out
```

### Axis M6 — §5.6 bad-case manual review + shadow regression-safety gate (deliver-agent + human-led; you read the evidence)

Per `iteration_governance.md` §5.6, the milestone close MUST include a manual review of the bad-case suite + shadow regression-safety gate. For M-Auto-1B, given the bot runtime is byte-identical to M-Auto-1A close on the runtime path (zero `server/` / `eval/src/main/java/` / `eval_interactive/eval_interactive/` Java + zero `server/src/main/resources/skills/*.yaml` edits cumulative), the §5.6 + shadow rerun is expected to produce only LLM-provider drift signature similar to M-Auto-1A close (which observed 5/34 = 14.7% one-direction `True/0.5 → False/0.0` drift recorded as provider non-determinism, NOT regression).

The deliver-agent + human ran the rerun in Phase 2 (between this Codex prompt dispatch and your reading). Read `docs/milestone_objective.md` §12 closure verdict section "§5.6 bad-case + shadow regression-safety evidence" for the run-id + per-suite case_passed counts + per-case PASS/FAIL/IMPROVING joint judgment.

**Your verification**:
- Does the §5.6 rerun show ANY regression beyond the M-Auto-1A close-day drift signature? If yes, escalate.
- Does the shadow rerun show ANY new regression beyond `R-shadow-fixture-empty-form-session-create-400` (M5 known) and the M-Auto-1A close-day drift? If yes, escalate.
- Bot byte-identical claim: verify `git diff --stat b6b627b..HEAD -- server/src/main/java/ eval/src/main/java/ eval_interactive/eval_interactive/ server/src/main/resources/ data/ db/migration/` returns empty (this is fence #1 + #2 + #4 cumulative).

**If the §5.6 + shadow rerun has NOT yet been run at the time of your review** (Phase 2 incomplete): note the missing evidence in your verdict + recommend the deliver-agent + human complete Phase 2 before final milestone close. Per `feedback_milestone_close_bad_case_before_codex` memory: missing-evidence is a fix_required P0 timing concern (not code); resolve via Phase 2 evidence collection + targeted re-review.

### Axis M7 — §8.5 split decision verification (NEW; this is the critical cumulative-level decision Codex verifies)

The deliver-agent + human jointly decided at S-Auto-7.1 close 2026-05-29 to **split M-Auto-1B per §8.5** rather than (a) extend to 6 sub-sprints with overflow exception OR (b) defer to M-Auto-2. The split rationale:

- §8.5 directive is explicit: "A milestone that exceeds 5 sub-sprints is a signal that the milestone scope is too large; the deliver-agent SHALL split it at the next milestone planning round."
- M-Auto-1B at S-Auto-7.1 close = 5 sub-sprints already (S-Auto-5 + S-Auto-6 [empty] + S-Auto-7 + S-Auto-7.1 + S-Auto-8 planned originally for overnight). Adding the new S-Auto-7.2 (OQ-S61.1 applier fix) would bring it to 6 → SHALL split.
- M-Auto-1C "Auto-Evolution Calibration Continuation" opens with 2 sub-sprints: S-Auto-7.2 / Sprint 062 (applier.py:370 mvn module-selection fix; 1-line + 1-2 tests; ~half-day) + S-Auto-8 / Sprint 063 (first overnight + first human review + first cherry-pick; ~3-5 days).
- M-Auto-1B closes with 8/15 acceptance bar met + 7/15 deferred to M-Auto-1C.

**Your verification**:
- Is the split decision §8.5-compliant? (SHALL split + clean scope boundary between substrate-fix+pre-flight vs overnight+cherry-pick.)
- Is M-Auto-1C scope (2 sub-sprints) clean? (Well within §8.5 ceiling; coherent architectural theme = "finish what M-Auto-1B started".)
- Does M-Auto-1B's incomplete acceptance bar (7/15 unmet) get cleanly transferred to M-Auto-1C without scope leakage? Read `docs/milestone_objective.md` (next-milestone draft if deliver-agent included it in this close-bundle; else the post-Codex Phase 3 will create it).
- Does the split avoid smuggling scope across milestones (§8.5 "A sub-sprint that crosses an unrelated architectural surface is a signal that the sub-sprint belongs to a different milestone")? S-Auto-7.2 (applier mvn fix) + S-Auto-8 (overnight + cherry-pick) are coherent with M-Auto-1A/M-Auto-1B's auto-evolution substrate theme; they're NOT smuggled cross-milestone scope.

Report your judgment. If the split is sound, return `approve` for the split decision at the milestone-shared level. If you see scope leakage OR a cleaner alternative, name it.

### Axis M8 — Cross-cutting reproducibility check

Per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`, every numeric claim in the deliver-agent's M-Auto-1B close package must be reproducible:

| Claim | Reproduction |
|---|---|
| autoloop pytest 216 (M-Auto-1A) → 228 (S-Auto-7) → 255 (S-Auto-7.1) | `cd autoloop && uv run --extra dev pytest -q` at each sub-sprint close; deltas +7 (S-Auto-5) +5 (S-Auto-7) +27 (S-Auto-7.1) |
| eval_interactive pytest 486 / 3 UNCHANGED | `cd eval_interactive && uv run python -m pytest --tb=no -q` |
| Java baseline 1183 / 1 / 0 / 2 UNCHANGED | `cd server && mvn test -B` (skip if Java zero-touch verified via git diff) |
| scoring_code_baseline_sha M-Auto-1A close `5177b674…` → S-Auto-7 close `22548e20…` | `cd autoloop && uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"` at each commit |
| Blessed baseline_dir `eval_interactive/results/m-auto-1b-baseline-20260529/` loads cleanly (3 SuiteSnapshot, 0 warnings, 46 cases, 16 case_passed, 18 tier-2 mandatory FAILs) | `from autoloop.scoring import baseline_loader; baseline_loader.load(Path('eval_interactive/results/m-auto-1b-baseline-20260529').resolve(), config=config)` |
| Detector self-discipline 3 regression tests PASS | `cd autoloop && uv run --extra dev pytest -q autoloop/tests/test_anti_hardcode_check.py` |
| Pre-flight 27 new tests PASS + auto-reboot path operator-verified against live pid=8613 | `cd autoloop && uv run --extra dev pytest -q tests/test_preflight.py`; `docs/sprints/sprint-061-handoff.md` §4 transcript |

Spot-check at least 4 claims. Report any divergence.

### Axis M9 — R-item flips audit

Per `docs/action_bank.md` §5 + §6 close-action index:

- **Closed at M-Auto-1B (one)**: `R-S57-anti-hardcode-whenever-arrow-synonym-bypass` (closed at S-Auto-5 close 2026-05-28 via Fix-C hybrid; cumulative M-Auto-1B close confirms the Codex Axis B exact bypass `Whenever ... =>` FAILs the detector + end-to-end calibration evidence held across all subsequent sub-sprints since the detector was untouched after S-Auto-5).
- **Opened at M-Auto-1B (one)**: `R-S58-anti-hardcode-zero-width-when-arrow-bypass` (S-Auto-5 Codex Axis C surfaced 2026-05-28; M-Auto-1C / Fix-D candidate; final disposition deliver-agent + human at M-Auto-1C planning round based on overnight evidence after S-Auto-8 lands).
- **Carry-over from M-Auto-1A**: 7 R-items unchanged (`R-eval-java-module-retirement`, `R-bad-case-parallel-session-establishment-flakiness`, `R-shadow-fixture-empty-form-session-create-400`, `R-case-families-manifest-cs095-smoke-vs-anchor-orphan`, `R-bad-case-metadata-field-name-canonicalize`, `R-bad-case-suite-uc-ghij-seed-from-real-sessions`, `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress`).
- **NEW from M-Auto-1B → M-Auto-1C transition**: `OQ-S61.1-applier-mvn-module-selection-csagent-parent` (S-Auto-7.1 handoff §6 dispositioned; S-Auto-7.2 / Sprint 062 in M-Auto-1C resolves; 1-line applier.py:370 fix). May or may not become a tracked R-item per deliver-agent + human discretion at M-Auto-1C open.

Verify these flips are correctly annotated in `docs/action_bank.md` §5 + §6.

### Axis M10 — Architecture-health metric direction

Per `iteration_governance.md` §6:

- **`new_semantic_hardcode_count`** (target: down): cumulative across S-Auto-5..S-Auto-7.1 = **0** (Fix-C step 1 is generic structural per D2; detector self-discipline 3 regression tests PASS; substrate-fix + pre-flight + dotenv all pure-infra). Direction: **NO change (good — held flat at 0)**.
- **`soft_signal_conversion_count`** (target: up): no cherry-pick landed in M-Auto-1B (S-Auto-6 was the cherry-pick slot but closed empty); no Skill YAML edit → 0 conversions. Direction: **0 movement (expected for incomplete-acceptance-bar close)**.
- **`planner_ownership_ratio`** (target: up): no runtime behaviour change → unchanged. Direction: **0 movement (expected)**.
- **`shadow_disagreement_rate`** (target: down): not yet measured (no overnight ran). Direction: **first measurement deferred to M-Auto-1C S-Auto-8 overnight**.

Confirm these directions in your verdict. They are observation-only per §5.5 (collection_status: not_started for §6 metrics).

---

## Output format (§4.2 sprint-close header verbatim)

Write your review to `docs/codex-findings.md` (APPENDING to the existing per-sub-sprint Codex content there for S-Auto-5 + S-Auto-7; DO NOT overwrite). The deliver-agent will `git mv` the entire `docs/codex-findings.md` to `docs/milestones/M-Auto-1B_codex-review.md` at M-Auto-1B close-bundle Phase 3 per §8.4; in the interim, your milestone-shared review extends the live file.

Use this exact 4-line header at the top of your appended section (per `iteration_governance.md` §4.2):

```
## M-Auto-1B Milestone-Shared Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph naming the cumulative verdict + key axes findings + the §8.5 split decision verdict>
```

Followed by per-axis findings (M1 through M10 above), each as a labelled subsection with:

- Axis name + one-sentence finding (PASS / FAIL / CONCERN).
- If CONCERN or FAIL: paste diff snippet (≤200 chars) + reasoning + the §3 layer name where the fix should target + recommended verdict downgrade (if any).

Then issue **exactly one §4.1 verdict** (the four-verdict set):

- `approve` — cumulative composition introduces no semantic hardcode (all axes PASS); per-sub-sprint Codex verdicts hold under cumulative; §8.5 split decision sound; M-Auto-1C open is clean continuation; §5.6 + shadow rerun (Phase 2) within expected LLM-provider drift signature; reproducibility checks PASS.
- `approve with downgrade-to-signal follow-up` — cumulative composition introduces no new hardcode but a residual concern surfaces (e.g., the §5.6 + shadow rerun shows a NEW unexpected drift pattern beyond LLM-provider non-determinism; OR the M-Auto-1C scope as drafted has an ambiguity worth flagging; OR R-S58 / OQ-S61.1 / OQ-S60.10 deeper loader.py path-mismatch deserve immediate M-Auto-1C inclusion vs M-Auto-2 deferral). Name the trigger.
- `reject as semantic hardcode` — cumulative composition introduces a soft semantic decision the LLM should own that the per-sub-sprint Codex passes missed; OR the §8.5 split decision smuggles scope across milestones; OR the §5.6 + shadow rerun surfaces a real regression (not LLM-provider drift). Name the §3 layer the fix should target.
- `needs human architecture decision` — the close crosses an unresolved governance question (e.g., whether §8.5 split with incomplete acceptance bar requires governance-doc clarification; whether the cumulative shadow firewall holds across multiple controlled fence overrides).

---

## Constraints

- **Do NOT edit code.** Your review is read-only; you may sample any code path but the only file you write to is `docs/codex-findings.md` (APPENDING to existing per-sub-sprint content).
- **Do NOT re-judge §5.6 bad-case suite verdicts** independently — the §5.6 rule is human-judgment primary. Your role is to verify internal consistency of deliver-agent + human's recorded notes + classify ANY suspicious patterns.
- **Do NOT re-litigate the S-Auto-5 + S-Auto-7 per-sub-sprint Codex findings** — those are CLOSED. Your cumulative pass extends them at the milestone-shared level.
- **Do NOT re-litigate the §8.5 split decision** OUTCOME — the deliver-agent + human decided 2026-05-29 to split. Your verification (Axis M7) is whether the split is §8.5-compliant + scope-clean, NOT whether to split.
- **DO escalate to `needs human architecture decision`** if you find the split decision violates §8.5 (e.g., if you judge that splitting at 5 sub-sprints when the rule says SHALL-split-AT-6 is premature).
- **DO escalate to `reject as semantic hardcode`** if you find a cumulative composition that introduces a hardcode that none of the per-sub-sprint passes individually saw.
- **DO mark fix_required** if the §5.6 + shadow rerun evidence is MISSING from `docs/milestone_objective.md` §12 (per `feedback_milestone_close_bad_case_before_codex` memory) — recommend Phase 2 evidence collection + targeted re-review.

---

## Pre-mitigation already in place (deliver-agent side; reduces Codex friction)

From `docs/codex-findings.md` (live; carries S-Auto-5 + S-Auto-7 per-sub-sprint content):

- Both per-sub-sprint Codex passes returned `decision: pass / blocking_count: 0` first pass single round with §4.1 verdict `approve with downgrade-to-signal follow-up`.
- All M-Auto-1B §6 17 hard fences honored with TWO documented controlled overrides (fence #13 `eval_runner.py` planning-blessed; fence #2 `loader.py` in-session human-authorized + annotation added at S-Auto-7 close-bundle satisfying the governance trigger).
- R-S57 closed cleanly via Fix-C hybrid (S-Auto-5); R-S58 opened cleanly as Codex-Axis-C-surfaced M-Auto-2 / Fix-D candidate.
- §5.6 bad-case suite + shadow rerun evidence is captured in `docs/milestone_objective.md` §12 (Phase 2 evidence collection by deliver-agent + human; reproducibility commands documented).
- §8.5 split decision rationale recorded with deliver-agent + human joint AskUserQuestion record + memory `feedback_preflight_env_check_outer_loop` capturing the deeper human-locked principle behind the S-Auto-7.1 design.

This means Axes M1 / M2 / M3 / M5 / M9 / M10 are likely PASS without further work. The verdict tension lives in Axes M4 (acceptance bar honest assessment), M6 (§5.6 + shadow evidence completeness), M7 (§8.5 split soundness), and M8 (reproducibility spot-checks).

---

**END OF M-AUTO-1B MILESTONE-SHARED CODEX REVIEW PROMPT.** Begin with Axis M1. Walk each axis in order. APPEND your review to `docs/codex-findings.md` using the §4.2 sprint-close header verbatim + per-axis findings + exactly one §4.1 verdict.
