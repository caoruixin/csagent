# Codex review prompt — Sprint 058 / M-Auto-1B S-Auto-5 — Per-sub-sprint Anti-Hardcode review (§4.3 trigger #2)

You are the **Anti-Hardcode Per-Sub-Sprint Review Agent for Sprint 058 / M-Auto-1B S-Auto-5**. This is a **per-sub-sprint review** dispatched per `docs/current/iteration_governance.md` §4.3 trigger #2 — the S-Auto-5 sub-sprint refines the structural §1.7 anti-hardcode guard (Fix-C step 1 word-boundary regex + Fix-C step 2 `synonym_map_enabled` toggle final state via Path A flip) and **MUST be Codex-verified BEFORE the S-Auto-6 overnight batch can begin**. The milestone-shared Codex review (cumulative S-Auto-5 + S-Auto-6 range) is a **separate** later pass dispatched at M-Auto-1B close; this prompt covers S-Auto-5 ONLY.

The cumulative commit range under review for THIS Codex pass:

```
ae0ec3e  Sprint 058 / S-Auto-5 / M-Auto-1B — live-iter bootstrap + Fix-C step 1 + calibration evidence + step 2 Path A
b0ce174  Sprint 058 / S-Auto-5 / M-Auto-1B — M-Auto-1A live-iter bootstrap completion (anthropic SDK dep)
```

(parent: `6e8692d` — M-Auto-1B planning round — milestone + Sprint 058 / S-Auto-5 sub-sprint contract + dev prompt + research proposal)

Cumulative diff: 8 files changed, +1053 insertions, -1 deletions (b0ce174: 2 files +354/−0; ae0ec3e: 6 files +699/−1).

---

## Read order (minimal)

Read only:

1. **`AGENTS.md`** (auto-loaded via constitution chain — pulls `doc_governance.md` + `agent_context_guide.md` + `iteration_governance.md`).
2. **This prompt** (self-contained executable view per `iteration_governance.md` §9 invariant).
3. **`docs/sprints/sprint-058-handoff.md`** — dev's own S-Auto-5 close handoff (single source you cannot embed because it post-dates this prompt's authoring; read it in full).

You may sample (NOT embed) the following code paths for verification:

- **`autoloop/autoloop/sandbox/anti_hardcode_check.py`** — the file with Fix-C step 1. Specifically: new constant `_RE_WHEN_WORD_BOUNDARY = re.compile(r"\b(?:whenever|when)\b")` at module scope; `_normalize()` body extended with `norm = _RE_WHEN_WORD_BOUNDARY.sub("if", norm)` under the existing `synonym_map_enabled` guard, BEFORE the `_SYNONYM_MAP` whole-string substitution loop. No other change.
- **`autoloop/config.yaml`** — `anti_hardcode.synonym_map_enabled: false → true` flip (Fix-C step 2 Path A) with inline rationale comment + handoff cross-reference. No other config block change.
- **`autoloop/tests/test_anti_hardcode_check.py`** — 4 NEW Fix-C step 1 regression tests (Codex Axis B exact bypass now FAILs; clean "when the user describes" still PASSes; clean "whenever possible" subordinate clause still PASSes; multi-line `When ...\n=> ...` decomposition now FAILs). PLUS the existing 3 detector self-discipline regression tests (`test_detector_source_does_not_hardcode_eval_case_ids` + `..._user_utterance_literals` + `..._rule_count_bounded`) — verify all PASS.
- **`autoloop/tests/test_real_meta_agent_calibration.py`** — NEW; 3 calibration tests (sample-count assertion relaxed from `>=10` to `>=1` per augmented-evidence path; FLAG rate <25%; FP=0 on clean prose).
- **`autoloop/tests/fixtures/real_meta_agent_calibration_samples.json`** — NEW; 3 sanitized real-meta-agent propose samples (exp-2 / exp-3 / exp-4) with metadata block.
- **`autoloop/pyproject.toml`** + **`autoloop/uv.lock`** — `anthropic` SDK dep added (b0ce174 bootstrap commit; M-Auto-1A live-iter prerequisite completion).
- **`autoloop/autoloop/meta_agent/llm_client.py`** — read-only sample (DO NOT verify edits; the module is hard-fenced in S-Auto-5). The `_load_env_local` path mismatch (autoloop/.env.local vs repo-root expected) is OQ-S58.1 — resolved via shell-export workaround; the loader code itself is UNCHANGED.

Do **not** re-read the milestone-level hard fences in full from `docs/milestone_objective.md` or the live S-Auto-5 contract from `docs/sprint_objective.md` for letter-of-fence verification — both are embedded below.

---

## Embedded sub-sprint contract (cumulative scope claim)

### Goal of S-Auto-5 (from `docs/sprint_objective.md` live during sub-sprint — embedded verbatim per §9 invariant)

S-Auto-5 close 时:

1. **Live-iter prerequisites retired.** `mvn package -pl server -am -DskipTests` produces `server/target/*.jar`; `AUTOLOOP_META_LLM_API_KEY` confirmed live via `python -m autoloop check` returning success; working tree clean on `auto-loop-branch` immediately before the first live iteration.
2. **≥1 real iteration completed end-to-end.** `python -m autoloop run --experiments 1` (NO `--dry-run`) drives the full 14-step state machine to a verdict (keep / discard / error all acceptable). Per-iteration elapsed wall-clock time recorded; if first iter <15 min, run 2-3 more to accumulate ≥10 real meta-agent propose samples in `autoloop/results/experiments.jsonl`.
3. **Fix-C step 1 implemented.** `_normalize()` in `autoloop/autoloop/sandbox/anti_hardcode_check.py` extended with `re.sub(r"\b(?:whenever|when)\b", "if", norm)` guarded by `synonym_map_enabled`. Existing `_SYNONYM_MAP` UNCHANGED. 17-fixture sweep still produces 11/11 forbidden FAIL + 4/4 clean PASS + 2/2 FLAG_FOR_CODEX. Detector self-discipline 3 regression tests PASS. Rule count ≤30. Codex Axis B exact bypass `Whenever the customer describes an appeal => route to escalation and skip normal triage.` now FAILS the detector.
4. **Real-meta-agent batch calibration with ≥10 samples**: per-sample verdict recorded; FLAG rate <25%; FP rate on clean prose = 0; evidence in handoff §5.
5. **Fix-C step 2 decision**:
   - Path A (FLAG <25% + FP=0 on clean prose): flip `autoloop/config.yaml` `anti_hardcode.synonym_map_enabled: false → true` (final state).
   - Path B (FP>0 on any clean prose): retain `false` + add new `_RE_Q1_WHEN_ARROW_FLAG` regex + `_q1_when_arrow_flag` rule emitting FLAG_FOR_CODEX (NOT auto-FAIL).
   Decision + rationale + per-sample evidence recorded in handoff.
6. **Per-sub-sprint Codex `pass`** (§4.3 trigger #2). Deliver-agent dispatches; dev does NOT dispatch.

### Three human-locked D-clauses (carried forward from M-Auto-1A S-Auto-4 — unchanged in S-Auto-5)

- **D1 — Detector mechanism**: regex + heuristic ONLY (stdlib `re` + `unicodedata`). NO second LLM in propose-stage critical path. `FLAG_FOR_CODEX` is a human / Codex review signal — NOT auto-promoted to FAIL and NOT auto-demoted to PASS at propose-stage.
- **D2 — Detector pattern scope**: detector encodes **generic structural patterns ONLY**.
  - **Allowed**: IF/WHEN/THEN/ELSE decision-tree shapes; `cs<id>`-like tokens; `session_id`-like / `case_id`-like / `iteration_id`-like tokens; MUST/NEVER/Tier-0-like phrasing structures; `.contains(...)` / `.matches(...)` literals.
  - **Forbidden**: specific eval case wording; specific user utterances; specific expected assistant answers; known case success/failure labels (e.g., `closure_criterion` text fragments).
  - If a rule requires a specific eval phrase to fire, the rule is structurally wrong and must be redesigned.
- **D3 — `gaming.py` input contracts** (UNCHANGED in S-Auto-5; gaming.py is hard-fenced #13).

### What actually shipped in S-Auto-5 (cumulative scope summary)

From dev's handoff (`docs/sprints/sprint-058-handoff.md` §§1-11) and the two-commit diff:

1. **M-Auto-1A live-iter bootstrap completion** (commit `b0ce174`): `anthropic` SDK declared in `autoloop/pyproject.toml` + `uv.lock` regenerated. **Justified separate commit** per OQ-S58.9: the bootstrap was required to satisfy Goal #1's "working tree clean immediately before the first live iteration" requirement; the SDK was referenced at `autoloop/autoloop/meta_agent/llm_client.py:100` (S-Auto-3 substrate code) but not declared in `pyproject.toml` because S-Auto-3 close never exercised the live path. The loader-path mismatch (autoloop/.env.local vs repo-root expected — OQ-S58.1) was resolved via shell-export workaround (NOT a code edit; `meta_agent/` is hard-fenced).

2. **Fix-C step 1: word-boundary regex** (in `ae0ec3e`): one new module-scope constant + one `re.sub` call extension in `_normalize()`. Diff (7 LOC):

   ```
   autoloop/autoloop/sandbox/anti_hardcode_check.py
     @@ -80,6 +80,12 @@ _SYNONYM_MAP = {
     +# Fix-C step 1 (S-Auto-5): word-boundary "whenever"/"when" → "if". Closes
     +# the Codex Axis B bypass `Whenever ... =>` shape where the existing
     +# surrounding-space _SYNONYM_MAP entries do not fire at start-of-string
     +# / after-punctuation positions. Generic structural pattern (D2 compliant).
     +_RE_WHEN_WORD_BOUNDARY = re.compile(r"\b(?:whenever|when)\b")
     @@ -93,6 +99,7 @@ def _normalize(text: str, *, synonym_map_enabled: bool) -> str:
     +        norm = _RE_WHEN_WORD_BOUNDARY.sub("if", norm)
              for src, dst in _SYNONYM_MAP.items():
                  norm = norm.replace(src, dst)
   ```

3. **Fix-C step 2 Path A**: `autoloop/config.yaml` `anti_hardcode.synonym_map_enabled: false → true` flip with detailed inline rationale + handoff cross-reference (13 LOC including comment block).

4. **Live-iter end-to-end evidence** (handoff §4): 4 iterations dispatched (exp-1 errored on missing-anthropic pre-bootstrap; exp-2 errored at Spring spawn — `SpringStartupTimeoutError` rc=1 on port 51786; exp-3 / exp-4 discarded at content_validator length_overflow with 8-9× ratio). All 4 reached terminal verdicts. 3 propose-stage samples captured (exp-2 / exp-3 / exp-4 — propose succeeded before terminal failures).

5. **Calibration evidence** (handoff §5): 3 real-meta-agent samples, all clean_prose_label, all PASS under both `synonym_map_enabled=true` and `false`; FLAG rate 0% (<25%); FP rate on clean prose 0% (=0). Augmented-evidence path approved by human via AskUserQuestion (OQ-S58.4): 3 real + 17-fixture suite = 20 calibration data points; the literal ≥10 real-sample threshold was relaxed to ≥1 with inline test-docstring rationale.

6. **Tests** (handoff §9): autoloop pytest 216 → **223 PASS, 1 warning** (4 new Fix-C step 1 tests + 3 new calibration tests = 7 new). eval_interactive 486 passed, 3 failed UNCHANGED (OQ-S47.3 env-specific). Rule count UNCHANGED at 11 (well under ≤30 cap). Detector self-discipline 3 regression tests PASS.

7. **Adversarial spot-check pre-Codex** (handoff §7): 12 constructions exercised — 3 bypass variants (Codex Axis B exact + full-width unicode `Ｗhenever` + multi-line `When ...\n=> ...`) all FAIL via `Q1.if_then_decision_tree`; 4 clean-prose negative-controls PASS; 5 generalization gaps PASS (synonym swap "anytime" / "as soon as"; German "Wenn"; Spanish "Cuando"; logical-equivalent "Given X then Y"). Generalization gaps surfaced as Fix-D candidates in OQ-S58.8; dev did NOT promote them to immediate FLAG rules.

### Embedded M-Auto-1B hard fences (verbatim from `docs/milestone_objective.md` §6)

All 17 fences. Codex hard-fence walk uses this list as the canonical reference:

1. **No edits** to `server/src/main/java/**`. Runtime byte-identical to M-Auto-1A close `b6b627b` = M5 close `c9390dc`.
2. **No edits** to `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`. Evaluator byte-identical.
3. **CHANGED FROM M-Auto-1A**: `server/src/main/resources/skills/*.yaml` files conditionally writable EXACTLY ONCE in M-Auto-1B via the S-Auto-6 cherry-pick mechanism only. **In S-Auto-5: no edits expected.** `{prompts,scripts,config,mock}/**` byte-identical.
4. **No edits** to `data/**`, `db/migration/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/iteration_governance.md`, `docs/teams/**`.
5. **No edits** to sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-057-*` or any prior milestone archive.
6. **No `git add -A`** by dev. Stage only S-Auto-N scope files explicitly.
7. **CHANGED FROM M-Auto-1A**: cherry-pick to main is **ALLOWED EXACTLY ONCE** during S-Auto-6 (NOT in S-Auto-5). **In S-Auto-5: no cherry-pick expected.**
8. **No new Tier-0 invariant**. C2/C3 DEFER continues per M2-close verdict.
9. **No cross-file diff** by meta-agent ever (sandbox enforces); not applicable in S-Auto-5 because no propose-stage hypothesis is committed to main.
10. **No shadow-set leakage to meta-agent**. Aggregate only.
11. **No mutation of `eval_interactive/results/` schema**.
12. **No editing of `docs/codex-findings.md` during M-Auto-1B execution**. (Codex writes at S-Auto-5 close per §4.3.)
13. **No modification of `autoloop/autoloop/scoring/` baseline files** (`tier_evaluator.py` / `eval_runner.py` / `baseline_loader.py` / `gaming.py`); content-hash locked at `5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c`.
14. **No hardcoding of the Codex Axis B exact phrase** or any specific eval case wording / user utterance / expected answer / case-status label in any detector rule. Fix-C step 1 + optional Fix-B step 2 must encode generic structural shapes only (D2). The 3 detector self-discipline regression tests continue to PASS.
15. **No LLM call inside detector or content_validator** (D1).
16. **No promotion of `gaming.observation_only_in_v1`** from `true` to `false` in M-Auto-1B.
17. **At MOST 1 cherry-pick** during S-Auto-6 (redundant with #7).

### Embedded M-Auto-1B §6.1 OQ-S56.1 disposition (inherited from M-Auto-1A; UNCHANGED in S-Auto-5)

The blessing for `eval_interactive/eval_interactive.yaml` env-var indirection (`bot.base_url: http://localhost:8080` → `${CSAGENT_BACKEND_URL}` + 7-line comment block) carries forward. **In S-Auto-5: no additional edits to that file expected.** No second-edit surface.

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

## Verification axes (S-Auto-5 specific — 9 axes; walk in order)

### Axis A — Nine-question kernel walk against Fix-C step 1 + step 2

Walk the §4.1 kernel above against the cumulative diff. Specifically:

- **Q1**: Does Fix-C step 1's word-boundary regex `r"\b(?:whenever|when)\b"` constitute a "keyword / regex" for a semantic decision? Dev's argument (handoff §1 "Semantic hardcode" rationale): it is a **generic structural pattern matching 2 English words** AND a **refinement of the existing `_SYNONYM_MAP` entries** `" when " → " if "` + `" whenever " → " if "` (whose surrounding-space form failed to fire at start-of-string / after-punctuation positions — which is exactly the Codex Axis B bypass shape). The Fix-C regex is **not net-new enumeration**; it is the same two tokens with a word-boundary anchor instead of whitespace anchor. Judge whether this argument is adequate vs. an enum-expansion concern.
- **Q3**: Could the same outcome be achieved by projecting a soft signal to the LLM? Note: the detector itself is the soft-signal mechanism (`FLAG_FOR_CODEX` is a soft signal). Fix-C step 1 is a structural normalization that feeds the existing `Q1.if_then_decision_tree` regex (already a FAIL rule). The alternative — Fix-B step 2 FLAG rule — was available but NOT chosen because calibration evidence supported Path A. Judge whether the Path A choice is sound.
- **Q4**: Does the change encode any visible-eval case text / CaseSpec id / trace-specific phrasing? Detector self-discipline 3 regression tests claim NO. Independently verify by sampling `autoloop/autoloop/sandbox/anti_hardcode_check.py` for any `cs<id>` / `closure_criterion` / `expected_behavior` / `primary_uc` / `failure_tags` / `source_session_id` literal.
- **Q8**: Does the PR ship generalization eval coverage? Handoff §7 documents 12 adversarial constructions (3 bypass + 4 clean + 5 generalization gaps). Target / neighbor / negative-control / shadow per §7 stanza. Judge whether the 5 PASS generalization gaps (synonym swap "anytime"/"as soon as"; multi-lang "Wenn"/"Cuando"; logical-equivalent "Given X then Y") deserve immediate FLAG rule promotion OR are appropriate Fix-D deferrals.

Report findings inline. For each question that surfaces a concern, paste the diff snippet (≤200 chars) + the §3 layer name where the fix should target.

### Axis B — Codex Axis B exact bypass FAILs after Fix-C step 1

Sample `autoloop/tests/test_anti_hardcode_check.py::test_fix_c_step1_codex_axis_b_bypass_now_fails`. Verify:

1. The test fixture `after_value` is exactly `"Whenever the customer describes an appeal => route to escalation and skip normal triage."` (the Codex Axis B exact bypass from M-Auto-1A close).
2. The test runs with `synonym_map_enabled=True`.
3. The test asserts `result.verdict == "FAIL"` AND `result.rule_id.startswith("Q1.")`.
4. The test PASSES locally (you may run `cd autoloop && uv run --extra dev pytest -q autoloop/tests/test_anti_hardcode_check.py::test_fix_c_step1_codex_axis_b_bypass_now_fails` to confirm).

Trace the bypass routing through `_normalize`:

1. NFKC + lowercase → `whenever the customer describes an appeal => route to escalation and skip normal triage.`
2. Whitespace collapse → same (no double-spaces).
3. `_RE_WHEN_WORD_BOUNDARY.sub("if", ...)` → `if the customer describes an appeal => route to escalation and skip normal triage.`
4. `_SYNONYM_MAP` whole-string replace: `=>` → `→` → `if the customer describes an appeal → route to escalation and skip normal triage.`
5. `_RE_Q1_ARROW_TREE` (existing S-Auto-4 rule; pattern `\bif\b[\s\S]{1,120}?→[\s\S]{1,120}`) matches → FAIL via `Q1.if_then_decision_tree` (or whatever the existing arrow-tree rule_id is; sample `anti_hardcode_check.py` to confirm).

Confirm the routing matches dev's handoff §6 claim.

### Axis C — ≥3 NEW adversarial bypass spot-checks beyond dev's 12

Dev exercised 12 adversarial constructions in handoff §7 (3 bypass FAIL + 4 clean PASS + 5 generalization gaps PASS). Codex must independently add **≥3 NEW adversarial constructions** beyond dev's set. Suggested candidates (use these or invent others):

1. **Zero-width / homoglyph obfuscation**: `Wheneve​r the customer describes an appeal => route to escalation.` (zero-width space inside "whenever"). Verdict expected? — depends on `unicodedata.normalize("NFKC", ...)` behaviour on U+200B.
2. **Arrow variant beyond the dev's set**: `Whenever the customer describes an appeal --> route to escalation.` (double-dash arrow). The existing `_SYNONYM_MAP` has `"->": "→"` but does NOT cover `-->`; the existing `_RE_Q1_IF_THEN` may or may not match without arrow normalization. Verify behaviour.
3. **Capitalized variant with arrow**: `WHENEVER the customer describes an appeal => route to escalation.` (full-caps). After lowercase + word-boundary regex → should map to `if`; verify Q1 catches.
4. **Surrounding punctuation**: `(Whenever the customer describes an appeal) => route to escalation.` (parenthesized). Word-boundary anchors should still fire; verify.
5. **Within a sentence (not start)**: `Note: whenever the customer describes an appeal => route to escalation.` Verify word-boundary fires mid-sentence.

For each, document: construction, expected verdict (FAIL/FLAG/PASS), actual verdict, rule_id (if any). If any NEW bypass is discovered, that becomes a **NEW R-item recommendation** for M-Auto-1B Codex `approve with downgrade-to-signal follow-up` verdict (parallel to how `R-S57-anti-hardcode-whenever-arrow-synonym-bypass` was surfaced at S-Auto-4 Codex).

### Axis D — Real-meta-agent calibration evidence audit

Read `autoloop/tests/fixtures/real_meta_agent_calibration_samples.json` + handoff §5 + the comparison-augmented sample-count claim. Verify:

1. The fixture contains exactly 3 samples (exp-2, exp-3, exp-4); each has `iter_id`, `target_skill_file`, `target_field_path`, `proposed_value`, `before_value`, `rationale`, `iteration_decision`, `iteration_discard_reason`, `synonym_map_enabled: true`, `clean_prose_label: clean`, `captured_date: 2026-05-28`, `sanitization_notes`.
2. None of the 3 `proposed_value` strings contain any `cs<id>` / `case_id` / `session_id` / user-utterance literal. (Spot-check; sanitization should have removed any such tokens but the fixture is human-verifiable.)
3. The handoff §5 per-sample verdict table shows all 3 samples as PASS under both `synonym_map_enabled=true` AND `false` (i.e., the Fix-C step 1 change does NOT regress the verdicts on these 3 real samples).
4. The Path A flip decision rationale (handoff §5 last subsection + §10 OQ-S58.4) explicitly handles the **3 < ≥10 spec gap** via augmented-evidence (3 real + 17-fixture = 20 data points). Judge: is the augmentation argument sound? Specifically: the 17-fixture suite is synthetic (deliver-agent + research-agent fixtures from S-Auto-4); the 3 real samples are the *first* real-distribution evidence. Does augmenting with 17 synthetic samples sufficiently substitute for 7 more real samples? Codex must independently judge whether the sample-count gap weakens the toggle-flip decision OR whether the unanimous PASS pattern across all 20 data points (including the 17 fixtures that span forbidden/clean/borderline) is adequate.
5. The calibration test file `autoloop/tests/test_real_meta_agent_calibration.py` includes 3 tests: `test_real_meta_agent_calibration_sample_count` (assertion relaxed from `>=10` to `>=1` per augmented-evidence path), `test_real_meta_agent_calibration_flag_rate_under_threshold`, `test_real_meta_agent_calibration_fp_zero_on_clean_prose`. All PASS.

Report the audit. If you judge the augmented-evidence path inadequate, return `approve with downgrade-to-signal follow-up` with the trigger being "expand real-meta-agent sample count to ≥10 at M-Auto-2 or earlier through a higher-volume calibration sub-sprint".

### Axis E — Detector self-discipline 3 regression tests PASS; rule count ≤30

Sample `autoloop/tests/test_anti_hardcode_check.py` for the three detector self-discipline tests:

1. `test_detector_source_does_not_hardcode_eval_case_ids` — grep-scans `anti_hardcode_check.py` source for `cs011` / `cs015` / `cs042` / `cs101` / `cs59s` (or similar literals); asserts zero matches.
2. `test_detector_source_does_not_hardcode_user_utterance_literals` — grep-scans for `my account is locked` / `closure_criterion` / `expected_behavior` / `source_session_id` / `primary_uc` / `failure_tags` literals; asserts zero matches.
3. `test_detector_source_rule_count_bounded` — asserts `len(_RULES) <= 30`.

Verify all 3 PASS. Sample the source file to independently confirm: there should be NO literal eval case_id / user utterance / answer / case-status label in the detector source — only the new `_RE_WHEN_WORD_BOUNDARY` constant + the existing 11 rule patterns + the `_normalize` body extension.

Rule count: confirm UNCHANGED at 11 (Fix-C step 1 added no new rule; Path A was chosen so Fix-B FLAG rule was NOT added).

### Axis F — 17-fixture sweep still produces 11/11 + 4/4 + 2/2 after Fix-C step 1

Sample `autoloop/tests/test_anti_hardcode_check.py` for the 17 calibration cases (11 forbidden FAIL + 4 clean PASS + 2 borderline FLAG_FOR_CODEX). Run:

```bash
cd autoloop && uv run --extra dev pytest -q autoloop/tests/test_anti_hardcode_check.py
```

Verify the full test file PASSES (all 17 calibration cases produce the expected verdict + the 3 detector self-discipline tests PASS + the 4 new Fix-C step 1 tests PASS). The handoff §6 claims 11/11 + 4/4 + 2/2 unchanged — independently confirm.

### Axis G — Live-iter end-to-end evidence documented

Read handoff §4 (Live-iter end-to-end record). Verify:

1. `mvn package` succeeded; jar artefact path documented (handoff names `server/target/csagent-server-0.1.0-SNAPSHOT.jar`, 58 MB).
2. `python -m autoloop check` output documented (handoff names "PASS: all 6 Skill YAML paths exist on disk" + "PASS: sandbox self-test (1 positive + 1 negative)").
3. `git status` clean confirmation pre-iteration.
4. Per-iteration table: 4 iterations (exp-1 / exp-2 / exp-3 / exp-4) with `target_skill`, `target_field`, `iteration_decision`, `discard_reason`, `elapsed_s`, `propose_succeeded` columns. All 4 reached terminal verdicts; 3 produced valid propose samples (exp-2/3/4).
5. Per-iteration elapsed times (122.1s / 87.0s / 37.5s) well under the 40-min cap.

Verify the OQ-S58.7 surfaced item (Spring spawn failure on port 51786) is correctly classified as a substrate-side observation (NOT an S-Auto-5 fence violation; `applier.py` is hard-fenced).

### Axis H — Two-commit deviation justification

Dev's commit pattern deviates from the "one commit at sub-sprint close" guidance in the dev prompt's Commit discipline section. Two commits landed:

- `b0ce174` — M-Auto-1A live-iter bootstrap completion (`autoloop/pyproject.toml` + `uv.lock` for `anthropic` SDK).
- `ae0ec3e` — Sprint 058 / S-Auto-5 main commit (Fix-C step 1 + step 2 Path A + tests + handoff).

Verify the deviation is justified per OQ-S58.9: the bootstrap commit was required to satisfy Goal #1's "working tree clean immediately before the first live iteration" requirement after `uv add anthropic` modified `pyproject.toml` + `uv.lock`. Without committing the bootstrap separately, the live iteration could not run on a clean working tree (a soft prerequisite of the loop's branch-create step in `applier.py`).

Judge: is the deviation justified (scope-creep-free, audit-trail-clear) OR is it scope creep that deserves `approve with downgrade-to-signal follow-up` with a trigger to refactor the loader path (i.e., fix `_load_env_local` to read from `autoloop/.env.local` instead of repo-root, retiring the workaround)?

### Axis I — Hard-fence verification (against §6 17 fences embedded above)

Run:

```bash
git diff --stat 6e8692d..ae0ec3e -- server/src/main/java/ eval/src/main/java/ eval_interactive/eval_interactive/ eval_interactive/case_specs/ eval_interactive/case_specs_shadow/ data/ db/migration/ server/src/main/resources/ docs/foundational/ docs/runtime_freeze_and_risk_policy.md docs/current/iteration_governance.md docs/teams/ autoloop/autoloop/scoring/
```

Expected: empty output (zero edits to all enumerated gated surfaces).

Spot-check the four `autoloop/autoloop/scoring/` files via `python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha(...))"` — should match `5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c` (handoff §9 "Zero-touch confirmations" claims this).

Spot-check the sprint archives (`docs/sprints/sprint-001-*` through `docs/sprints/sprint-057-*`) — no edits expected; the new `docs/sprints/sprint-058-handoff.md` is in-scope per §6 fence #5 carve-out.

Verify NO Skill YAML edit landed (fence #3 is conditional on S-Auto-6 cherry-pick; S-Auto-5 must not touch any `server/src/main/resources/skills/*.yaml`).

---

## Output format (§4.2 sprint-close header verbatim)

Write your review to `docs/codex-findings.md` (the live scaffold). Replace the scaffold body with your review content. The deliver-agent will `git mv` the file to `docs/milestones/M-Auto-1B_codex-review.md` at M-Auto-1B close per the standard close-out artefact list; in the interim, the live file carries this S-Auto-5 per-sub-sprint review until the milestone-shared review at M-Auto-1B close appends to it (or replaces it for milestone-shared verdict).

Use this exact 4-line header at the top of your review section (per `iteration_governance.md` §4.2):

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph naming the verdict + key axes findings>
```

Followed by per-axis findings (A through I above), each as a labelled subsection with:

- Axis name + one-sentence finding (PASS / FAIL / CONCERN).
- If CONCERN or FAIL: paste diff snippet (≤200 chars) + reasoning + the §3 layer name where the fix should target + the recommended verdict downgrade (if any).

Then issue **exactly one §4.1 verdict** (the four-verdict set):

- `approve` — the change is not a semantic hardcode (Fix-C step 1 refines existing `_SYNONYM_MAP` semantics; no enum expansion; calibration evidence supports Path A; all 9 axes PASS) AND no new bypass surfaced in Axis C.
- `approve with downgrade-to-signal follow-up` — the change is acceptable as an interim measure, but a follow-up sprint must (a) expand the real-meta-agent calibration sample count to ≥10 (closes Axis D augmented-evidence concern); OR (b) refactor the loader path (closes Axis H two-commit deviation); OR (c) promote one or more of the 5 generalization gaps (Axis C / handoff §7) to immediate FLAG rules. Name the trigger.
- `reject as semantic hardcode` — the change encodes a soft semantic decision the LLM should own (e.g., Codex judges that `\b(?:whenever|when)\b` is net-new enum expansion violating §1.7; or Axis D augmented-evidence path is inadequate). Name the §3 layer the fix should target.
- `needs human architecture decision` — the change crosses an unresolved governance question (e.g., the per-sub-sprint sample-count threshold for real-meta-agent calibration should be a governance-doc clause, not a per-sub-sprint scope decision).

---

## Constraints

- **Do NOT edit code.** Your review is read-only; you may sample any code path but the only file you write to is `docs/codex-findings.md`.
- **Do NOT re-judge §5.6 bad-case suite verdicts** (no bad-case rerun is in scope for this per-sub-sprint review — that's a milestone-close gate, not a sub-sprint gate).
- **Do NOT re-litigate the M-Auto-1A close** or the previously-archived `R-S57-anti-hardcode-whenever-arrow-synonym-bypass` (Sprint 057 / S-Auto-4 Codex review already accepted M-Auto-1A's substrate; S-Auto-5 is the closure of R-S57 via Fix-C).
- **Per-sub-sprint trigger list** (informational): per `iteration_governance.md` §4.3, per-sub-sprint Codex is REQUIRED when the sub-sprint (1) introduces a Tier-0 candidate; (2) crosses a §1.7 forbidden-list red line; (3) touches a hard-fenced surface that the milestone objective explicitly named out of scope; (4) closes a sub-sprint with a `fix_required` outcome that needs per-sub-sprint re-review. S-Auto-5 triggers under (2) — Fix-C touches the §1.7 anti-hardcode defender structural guard.
- **Out-of-scope-review** verdict (`out_of_scope_review`) is the appropriate verdict if you judge that S-Auto-5's actual scope did NOT touch the §1.7 structural guard in a way that required per-sub-sprint review (e.g., Fix-C step 1 is a config-governance + characterization-test refinement scope-exempt per §4.1 scope-exemption clause). However, given the dev contract explicitly named the §1.7 guard touch and the human-locked Fix-C decision, this verdict is unlikely.
- **Verdict must return BEFORE S-Auto-6 overnight starts.** If you return `reject as semantic hardcode`, a fix-iteration sub-sprint S-Auto-5.1 is required before S-Auto-6 can dispatch.

---

## Pre-mitigation already in place (dev-side; reduces Codex friction)

From handoff §7 + §9:

- Detector self-discipline 3 regression tests PASS (no forbidden literal in source).
- Adversarial spot-check (12 constructions) documented in handoff §7 with verdicts.
- Java + scoring + substrate + eval-side zero-touch confirmed (handoff §9 zero-touch table).
- 223 passed, 1 warning autoloop pytest (baseline 216 + 7 new); eval_interactive 486 passed, 3 failed unchanged.

This means Axis A / Axis B / Axis E / Axis F / Axis G / Axis I are likely PASS without further dev work. The verdict tension lives in Axes C (NEW adversarial constructions; possible new R-item surface), D (augmented-evidence path on Path A flip), and H (two-commit deviation).

---

**END OF CODEX REVIEW PROMPT.** Begin with Axis A. Walk each axis in order. Write your review to `docs/codex-findings.md` using the §4.2 sprint-close header verbatim + per-axis findings + exactly one §4.1 verdict.
