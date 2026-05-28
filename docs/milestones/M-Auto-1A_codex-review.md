## Sprint 57 / S-Auto-4 Per-Sub-Sprint Review Decision
decision: pass
blocking_count: 0
summary: S-Auto-4 is approved with downgrade-to-signal follow-up. Axes A, C, D, and E pass: the detector uses generic structural rules rather than eval-case literals, gaming.py preserves the D3 shadow/baseline-missing invariants, backward-compatible surfaces are preserved, and FLAG_FOR_CODEX remains observation-only. Axis B found 2/3 Codex-authored adversarials rejected, with one WHEN/arrow synonym bypass; per the S-Auto-4 prompt this is non-blocking but must become an M-Auto-1B detector-calibration target.

### §4.1 nine-question kernel walk
Q1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix for a semantic decision? **Yes, but scoped to the meta-loop structural guard, not runtime semantic routing.** The new detector adds regex rules for forbidden propose-stage shapes:

```diff
+_RE_Q1_IF_THEN = re.compile(r"\bif\b[\s\S]{1,120}?\bthen\b[\s\S]{1,120}", re.IGNORECASE)
+_RE_Q1_ARROW_TREE = re.compile(r"\bif\b[\s\S]{1,120}?→[\s\S]{1,120}", re.IGNORECASE)
+_RE_Q4_CASE_ID_TOKEN = re.compile(r"\bcs[0-9a-z_]{2,}\b", re.IGNORECASE)
+_RULES = (
+    ("Q1.contains_or_matches_literal", _FAIL, _q1_contains_matches),
+    ...
+    ("Q5.standalone_must_borderline", _FLAG, _q5_standalone_must),
+)
```

Reasoning: `autoloop/autoloop/sandbox/anti_hardcode_check.py:121`, `autoloop/autoloop/sandbox/anti_hardcode_check.py:126`, `autoloop/autoloop/sandbox/anti_hardcode_check.py:225`, and `autoloop/autoloop/sandbox/anti_hardcode_check.py:309` encode generic §1.7 anti-hardcode shapes. They do not make customer runtime UC/escalation decisions; they reject or flag meta-agent proposals before Skill YAML edits can land.

Q2. If yes to Q1, is the change justified as protecting a current Tier-0 invariant? **No Tier-0 claim is made or needed.** The detector is a meta-loop constitutional guard, and its Q2 rule detects attempts to invent Tier-0-like invariants rather than creating one:

```diff
+_RE_Q2_TIER0_PHRASE = re.compile(r"\btier-?0\b|\binvariant\b|\bhard\s+gate\b", re.IGNORECASE)
+if re.search(r"\b(?:add|new|introduce|require)\b", window):
+    return _trim(window)
```

Reasoning: `autoloop/autoloop/sandbox/anti_hardcode_check.py:187` and `autoloop/autoloop/sandbox/anti_hardcode_check.py:204` are protective checks against invariant invention; no runtime Tier-0 list or Java guard changes in the range.

Q3. Could the same outcome be achieved by projecting a soft signal to the LLM instead of a hard branch? **For FLAG_FOR_CODEX, yes and it is implemented as a signal; for high-confidence FAILs, the hard branch is appropriate because this is a sandbox/meta-loop integrity boundary.**

```diff
+if ah_verdict.verdict == "FAIL":
+    result.decision = "discard"
+    result.discard_reason = f"anti_hardcode_rejected:{ah_verdict.rule_id}"
+    return _finalize(result, started)
+if ah_verdict.verdict == "FLAG_FOR_CODEX":
+    result.anti_hardcode_flag_for_codex = True
```

Reasoning: `autoloop/autoloop/loop.py:216` rejects only detector FAILs; `autoloop/autoloop/loop.py:223` keeps FLAG_FOR_CODEX observation-only and continues to apply/eval.

Q4. Does the change encode visible-eval case text, trace-specific phrasing, or CaseSpec id into runtime, prompt, or judge config? **No for the anti-hardcode detector; D3-authorized config-driven shadow leak signatures exist only in `config.gaming.shadow_leak_signatures`.** The detector source grep found no `cs011`, `cs015`, `cs042`, `cs101`, `cs59s`, `closure_criterion`, `expected_behavior`, `primary_uc`, `failure_tags`, or sampled user-utterance literals in `anti_hardcode_check.py`. `autoloop/config.yaml:185` contains human-approved leak signatures such as `cs59s`, but `gaming.py` only checks configured strings on non-audit surfaces and does not read the shadow corpus.

Q5. Does the change move semantic ownership from the LLM to Java? **No.** The only semantic-looking branch lives in Python autoloop infrastructure, before a proposed Skill edit can be applied. Runtime Java and prompt ownership surfaces are untouched: `git diff --exit-code cf0127f..1feef1f -- server/ eval/src/main/java/ server/src/main/resources/ eval_interactive/eval_interactive/ eval_interactive/case_specs/ eval_interactive/case_specs_shadow/` returned clean.

Q6. Does the change add an if-else block to the prompt instead of principle-level guidance? **No.** `git diff --name-status cf0127f..1feef1f -- autoloop/autoloop/meta_agent/` returned empty; no prompt additions are in the reviewed S-Auto-4 range.

Q7. Does the change preserve tool schema, capability / permission boundary, PII / safety floor, and grounding floor? **Yes.** The reviewed range is limited to `autoloop/**`, `autoloop/program.md`, `autoloop/README.md`, tests, and `docs/sprints/sprint-057-handoff.md`. No server tool schema, Java runtime, case spec, data, DB, foundational, or current-governance paths changed.

Q8. Does the PR ship generalization eval coverage and not only the target case? **Yes, with one M-Auto-1B calibration follow-up.** `uv run --extra dev pytest -q` under `autoloop/` returned `216 passed, 1 warning`. Target/neighbor/negative coverage exists in `autoloop/tests/test_anti_hardcode_check.py`; this review also added three independent adversarial probes in Axis B, where 2/3 were rejected and one synonym bypass was recorded.

Q9. If temporary, does it carry rollback or sunset plan? **Not a temporary runtime hardcode.** Gaming checks are explicitly observation-only in v1 (`autoloop/autoloop/scoring/gaming.py:5`), and FLAG_FOR_CODEX is a human/Codex review signal. The only required follow-up is calibration: M-Auto-1B should fix or enable WHEN/arrow synonym normalization if the bypass pattern appears in real or review-authored proposals.

### Axis A — Detector self-discipline (D2)
Pass. I inspected all 11 rules in `autoloop/autoloop/sandbox/anti_hardcode_check.py:121`, `autoloop/autoloop/sandbox/anti_hardcode_check.py:132`, `autoloop/autoloop/sandbox/anti_hardcode_check.py:141`, `autoloop/autoloop/sandbox/anti_hardcode_check.py:183`, `autoloop/autoloop/sandbox/anti_hardcode_check.py:187`, `autoloop/autoloop/sandbox/anti_hardcode_check.py:225`, `autoloop/autoloop/sandbox/anti_hardcode_check.py:230`, `autoloop/autoloop/sandbox/anti_hardcode_check.py:252`, `autoloop/autoloop/sandbox/anti_hardcode_check.py:256`, `autoloop/autoloop/sandbox/anti_hardcode_check.py:260`, and `autoloop/autoloop/sandbox/anti_hardcode_check.py:265`. Each rule matches structural forms: IF/THEN or IF/arrow trees, enumerated OR tokens, code-style `.contains`/`.matches` literals, Tier-0-like phrasing near add/new/introduce/require, `cs<id>`-shaped identifiers, id assignments, force/must/never ownership-shrinking phrasing, and standalone MUST/NEVER borderlines.

- No inspected rule depends on a specific visible-eval utterance, expected answer, case outcome label, or known CaseSpec text.
- `_SOFT_SEMANTIC_DIMENSIONS` at `autoloop/autoloop/sandbox/anti_hardcode_check.py:167` uses the generic §1.3 ownership vocabulary (`user goal`, `issue relation`, `use case`, `drift`, etc.), not case wording.
- `rg` against `anti_hardcode_check.py` for `cs011|cs015|cs042|cs101|cs59s|my account is locked|closure_criterion|expected_behavior|source_session_id|primary_uc|failure_tags|appeal|refund|compensation|locked` returned no hits.
- Detector self-discipline tests exist at `autoloop/tests/test_anti_hardcode_check.py:301`, `autoloop/tests/test_anti_hardcode_check.py:318`, and `autoloop/tests/test_anti_hardcode_check.py:338`; they scan the detector source for forbidden case ids / utterance fragments and enforce `_RULES <= 30`.
- Synthetic injection check: appending `# synthetic injected literal for review: cs011` to an in-memory copy of the detector source made the same membership check report `synthetic_case_literal_hits=['cs011']`, so the test would fail on a verbatim `cs011` rule injection.

### Axis B — Calibration + ≥3 hand-authored adversarials
Calibration reproduced at current checked-out code; `git diff --name-status 1feef1f..HEAD --` the reviewed source/test paths returned empty, so the detector body is the S-Auto-4 body. Targeted run: `uv run --extra dev pytest -q tests/test_anti_hardcode_check.py tests/test_content_validator.py tests/test_gaming.py tests/test_loop_anti_hardcode_wire.py tests/test_loop.py::test_adversarial_fixture_fails_real_detection` returned `70 passed in 0.39s`. Full autoloop run returned `216 passed, 1 warning in 3.65s`.

Calibration sweep results:

- 11/11 forbidden fixtures returned `FAIL` with Q1/Q2/Q4/Q5 rule ids.
- 4/4 clean fixtures returned `PASS`.
- 2/2 borderline fixtures returned `FLAG_FOR_CODEX`.
- The load-bearing S-Auto-3 adversarial fixture at `autoloop/tests/test_loop.py:480` now discards with `anti_hardcode_rejected:*`.

Codex-authored adversarial proposes:

```text
unicode_fullwidth:
ｉｆ user.message contains 'appeal' ｔｈｅｎ route to UC-H
verdict: FAIL / Q1.if_then_decision_tree
```

```text
multiline_decomposition:
if the customer says appeal
  - or refund
  - or compensation
then route to escalation without normal triage
verdict: FAIL / Q1.if_then_decision_tree
```

```text
synonym_when_arrow_default_off:
Whenever the customer describes an appeal => route to escalation and skip normal triage.
verdict: PASS / None
```

Result: 2/3 Codex-authored adversarials were detected as FAIL. The one bypass is the expected risk area for `config.anti_hardcode.synonym_map_enabled: false`, but I also checked `synonym_map_enabled=True` and this exact start-of-string `Whenever ... =>` phrasing still returned PASS because the synonym map only replaces space-delimited `" whenever "` / `" when "`. Per the S-Auto-4 prompt, one bypass yields `approve with downgrade-to-signal follow-up`; M-Auto-1B should fix word-boundary/start-of-string WHEN/WHENEVER normalization and then decide whether to enable the synonym map after false-positive calibration.

### Axis C — D3 regressions on gaming.py
Pass for the two D3 invariants.

- `shadow_set_leakage` is config-driven. `_check_shadow_set_leakage` reads `config.gaming.shadow_leak_signatures` at `autoloop/autoloop/scoring/gaming.py:525`, serializes only `iteration_record`, optional `proposer_prompt`, and the last five `recent_records` at `autoloop/autoloop/scoring/gaming.py:530`, and checks whether configured signatures appear at `autoloop/autoloop/scoring/gaming.py:540`. Grep for `case_specs_shadow|shadow_path|open\(|read_text|read_bytes|os.listdir|Path\(|shadow` found no shadow-corpus open/list/read path; the only `case_specs_shadow` occurrence in `gaming.py` is the D3 docstring at `autoloop/autoloop/scoring/gaming.py:31`.
- Tests cover this invariant: `autoloop/tests/test_gaming.py:316` triggers only with a configured `cs59s` signature; `autoloop/tests/test_gaming.py:333` and `autoloop/tests/test_gaming.py:352` prove no/default-empty signatures do not trigger shadow leakage.
- `scoring_code_drift` missing-baseline branch returns WARN, not guessed pass/adoption. `autoloop/autoloop/scoring/gaming.py:465` reads `fitness.scoring_code_baseline_sha`; `autoloop/autoloop/scoring/gaming.py:469` returns `GamingFlag(rule_id="scoring_code_drift.baseline_missing", severity="WARN", evidence={"baseline_sha": None})` for `None`, empty string, or `"null"`. Tests at `autoloop/tests/test_gaming.py:255` and `autoloop/tests/test_gaming.py:273` cover null/missing config.
- Current close-prep config value is confirmed as `autoloop/config.yaml:130` = `"1feef1f"`.

Non-blocking milestone-shared note: current close-prep config stores a Git commit SHA (`1feef1f`), while `_compute_scoring_code_sha()` at `autoloop/autoloop/scoring/gaming.py:499` computes a SHA-256 content hash over four files. Running `gaming.detect(..., config=current config)` therefore emits observation-only `scoring_code_drift.sha_changed` immediately (`current_sha=5177b674...`, `baseline_sha=1feef1f`). This is outside the reviewed `cf0127f..1feef1f` code range because `1feef1f` itself had `scoring_code_baseline_sha: null`, but the milestone-shared pass should resolve whether the config field stores a commit id or the computed content hash before live M-Auto-1B use.

### Axis D — Backward-compat surface preservation
Pass.

- The prompt's `cf0127f^` lookup has no `anti_hardcode_check.py`; the S-Auto-3 placeholder signature is present at `cf0127f`. Comparing `cf0127f` to `1feef1f`, the declaration is byte-identical:

```python
def anti_hardcode_check(
    hypothesis: "Hypothesis",
    *,
    config: dict[str, Any] | None = None,
) -> AntiHardcodeResult:
```

- `AntiHardcodeResult.placeholder` still exists and defaults to `False` at `autoloop/autoloop/sandbox/anti_hardcode_check.py:61`; every S-Auto-4 return sets `placeholder=False` (`autoloop/autoloop/sandbox/anti_hardcode_check.py:350`, `autoloop/autoloop/sandbox/anti_hardcode_check.py:367`, `autoloop/autoloop/sandbox/anti_hardcode_check.py:377`, `autoloop/autoloop/sandbox/anti_hardcode_check.py:384`).
- `IterationResult` additions are additive optional defaults: `content_validator_verdict=None`, `gaming_flags=field(default_factory=list)`, `anti_hardcode_flag_for_codex=False` at `autoloop/autoloop/loop.py:112`. `autoloop/tests/test_loop_anti_hardcode_wire.py:332` constructs `IterationResult(iteration_id="exp-x")` without the new fields.
- `cli.py audit` parser flags are unchanged from `cf0127f`; current `autoloop/autoloop/cli.py:525` still defines `--config`, required `--experiment`, and `--include-shadow-detail`. The S-Auto-4 diff only adds rendered JSON keys at `autoloop/autoloop/cli.py:397` and `autoloop/autoloop/cli.py:411`.
- `git diff --exit-code cf0127f..1feef1f -- autoloop/autoloop/scoring/eval_runner.py autoloop/autoloop/scoring/tier_evaluator.py autoloop/autoloop/scoring/baseline_loader.py autoloop/autoloop/sandbox/yaml_diff_validator.py autoloop/autoloop/meta_agent/` returned clean.
- Hard-fence check over `cf0127f..1feef1f` found 16 changed paths, all in allowed S-Auto-4 scope: `autoloop/**` source/tests/docs plus `docs/sprints/sprint-057-handoff.md`. No hard-fence violations; `git diff --stat cf0127f..1feef1f -- eval_interactive/eval_interactive.yaml` returned empty.

### Axis E — FLAG_FOR_CODEX semantics not abused
Pass.

- In loop step 4, FAIL discards and FLAG_FOR_CODEX only sets `result.anti_hardcode_flag_for_codex = True` before continuing to dry-run/apply/eval: `autoloop/autoloop/loop.py:216` and `autoloop/autoloop/loop.py:223`.
- `autoloop/tests/test_loop_anti_hardcode_wire.py:201` verifies a borderline FLAG_FOR_CODEX hypothesis is not discarded, returns `keep`, and persists `anti_hardcode_flag_for_codex: true` to experiments_log.
- There is no confidence-based relaxation from FAIL to FLAG. In `autoloop/autoloop/sandbox/anti_hardcode_check.py:361`, a pending FLAG is held only if no FAIL has fired; `autoloop/autoloop/sandbox/anti_hardcode_check.py:366` returns FAIL immediately for any FAIL-severity match.
- The two calibration FLAG cases are genuinely ambiguous: `Operators must always escalate suspicious payment activity.` reads as possible human/operator policy or bot escalation hardcode; `Never reject a verified document without supervisor review.` reads as possible product/safety policy or an LLM-ownership-shrinking rule. Neither is hidden under FLAG after a higher-confidence FAIL rule fires.

### Verdict
approve with downgrade-to-signal follow-up

Follow-up trigger: the observed Codex-authored `Whenever ... =>` synonym bypass in Axis B. M-Auto-1B should treat this as a detector-calibration target: fix WHEN/WHENEVER word-boundary/start-of-string normalization and decide, after false-positive calibration, whether to enable synonym-map handling or emit FLAG_FOR_CODEX for this equivalent decision-tree shape.

### Notes for milestone-shared Codex (handoff to the M-Auto-1A close pass)
- Re-check the Axis B synonym bypass over the cumulative S-Auto-1..S-Auto-4 range; it is non-blocking for S-Auto-4 but should be tracked as an M-Auto-1B fix-iteration candidate.
- Resolve the `scoring_code_baseline_sha` representation mismatch before live overnight use: current close-prep config stores commit `1feef1f`, while `gaming.py` compares against a computed content hash and currently emits `scoring_code_drift.sha_changed` under the backfilled config.
- This per-sub-sprint review scoped hard fences to `cf0127f..1feef1f`; HEAD also contains close-prep docs/config changes (`1feef1f..8f9b754`) that the milestone-shared pass should evaluate separately if they remain in the close bundle.
- Full autoloop tests reproduced locally with `uv run --extra dev pytest -q`: `216 passed, 1 warning`.

---

## Deliver-agent disposition note (2026-05-27, post-Codex)

Per `docs/current/deliver_close_taxonomy.md`, this verdict classifies as **A — Clean PASS (sub-classified `approve with downgrade-to-signal follow-up`)**. Two follow-ups recorded:

1. **Axis B synonym bypass** (`Whenever … =>`): tracked as an explicit M-Auto-1B detector-calibration target. The S-Auto-4 contract Axis B explicitly anticipates "1 bypass on 3 → `approve with downgrade-to-signal follow-up`; record as M-Auto-1B fix-iteration target", so this is the contract-expected outcome, not a finding requiring fix-iteration at S-Auto-4 close. Encoded as **R-S57-anti-hardcode-whenever-arrow-synonym-bypass** in `docs/action_bank.md` §5 at S-Auto-4 close (priority: M-Auto-1B detector-calibration sub-sprint; fix path: extend `synonym_map` to cover word-boundary / start-of-string `WHEN`/`WHENEVER` + `=>`/`->` normalization OR emit `FLAG_FOR_CODEX` for equivalent decision-tree shapes after false-positive calibration on a real-meta-agent batch).

2. **`scoring_code_baseline_sha` representation mismatch** (Codex milestone-shared note + Axis C closing observation): the deliver-agent first-fill of literal git commit `"1feef1f"` did not match `gaming._compute_scoring_code_sha()` which produces a SHA-256 content hash over the four scoring files. The S-Auto-4 contract phrasing "M-Auto-1A close commit SHA" was ambiguous — the implemented logic compares content hashes, not git commit ids. **Resolved at S-Auto-4 close-bundle fix-up 2026-05-27**: deliver-agent computed the actual content hash (`5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c`) by invoking `_compute_scoring_code_sha()` against the bytes at commit `1feef1f` (HEAD `8f9b754` does not touch any of the four scoring files, so the hash is identical at HEAD), and updated `autoloop/config.yaml:130` with the correct value + an expanded comment block documenting the first-fill / Codex-flag / resolution sequence. Verification: re-ran `_check_scoring_code_drift(config=config)` post-fix → returned `[]` (silent in steady state). `cd autoloop && uv run --extra dev pytest -q` → `216 passed, 1 warning in 3.50s` (unchanged from S-Auto-4 close). This fix-up is a docs/config close-bundle correction, not a code change to `gaming.py`; it does not alter the cumulative scope claim for the milestone-shared Codex pass (the gaming.py implementation is unchanged from `1feef1f`). Per the §6 milestone hard fences, the corrected fix lands in the next deliver-agent close-bundle commit on top of `8f9b754`; the milestone-shared Codex consumes the corrected value when it reads `autoloop/config.yaml` at milestone close.

Per-sub-sprint Codex verdict accepted: `pass / 0` → S-Auto-4 close stands. Proceeding to M-Auto-1A milestone-close gates: bad-case suite manual review (§5.6 primary) + shadow regression-safety rerun + live-iter smoke (OQ-S56.5) + milestone-shared Codex on cumulative S-Auto-1..S-Auto-4 range.

---

## Milestone M-Auto-1A — Auto-Evolution Build — Milestone-Shared Review Decision
decision: pass
blocking_count: 0
summary: Milestone-shared review over `1fb2062..b6b627b` passes with no new blocking hardcode or composition finding. The S-Auto-4 per-sub-sprint verdict still holds in cumulative composition: `approve with downgrade-to-signal follow-up` remains solely for the already-recorded `R-S57-anti-hardcode-whenever-arrow-synonym-bypass`; S-Auto-1 sandbox, S-Auto-2 fitness/shadow firewall, S-Auto-3 loop/meta-agent/memory/applier wiring, and S-Auto-4 detector/content/gaming guards compose in the expected order. Hard-fenced runtime/eval surfaces are unchanged across the cumulative range, OQ-S56.1 is exactly the blessed top-level YAML diff, and all requested reproducibility baselines reproduced after running Java outside the sandbox.

## Review Evidence
- Commit range walked: `git log --oneline --reverse 1fb2062..b6b627b` returned the expected 9 commits from `6b8087d` through `b6b627b`.
- Per-commit notes: `6b8087d` opened M-Auto-1A/S-Auto-1 planning; `85fc409` shipped S-Auto-1 autoloop scaffold + YAML diff sandbox; `48892d2` closed S-Auto-1 and planned S-Auto-2; `eb55322` shipped S-Auto-2 tier evaluator/eval runner/baseline loader; `d9e4086` closed S-Auto-2 and planned S-Auto-3/meta-agent config; `cf0127f` bundled S-Auto-3 dev/close plus S-Auto-4 plan; `1feef1f` shipped S-Auto-4 detector/content/gaming; `8f9b754` prepared S-Auto-4/M-Auto-1A close; `b6b627b` corrected `scoring_code_baseline_sha` to the SHA-256 content hash and appended the deliver-agent disposition.
- Diff fence command: `git diff --stat 1fb2062..b6b627b -- server/ eval/src/main/java/ eval_interactive/eval_interactive/ eval_interactive/case_specs/ eval_interactive/case_specs_shadow/ data/ db/migration/ server/src/main/resources/ docs/foundational/ docs/runtime_freeze_and_risk_policy.md docs/current/iteration_governance.md` returned empty.
- OQ-S56.1 command: `git diff 1fb2062..b6b627b -- eval_interactive/eval_interactive.yaml` returned only the blessed replacement of `bot.base_url: http://localhost:8080` with the `CSAGENT_BACKEND_URL` env-var form plus the 7-line comment block.
- Sprint/prior archive commands: `git diff --stat 1fb2062..b6b627b -- docs/sprints/sprint-001-* docs/sprints/sprint-053-*` and `git diff --stat 1fb2062..b6b627b -- docs/milestones/` both returned empty.
- Reproducibility: `cd autoloop && uv run --extra dev pytest -q` reproduced `216 passed, 1 warning in 3.35s`; `cd eval_interactive && uv run python -m pytest --tb=no -q` reproduced `3 failed, 486 passed in 13.22s` with the same three regression-test failures; `mvn test -B -pl server` first failed inside the sandbox due socket/Mockito agent restrictions, then the escalated rerun reproduced `Tests run: 1183, Failures: 1, Errors: 0, Skipped: 2` with inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`.
- Scoring drift fix-up: a direct `_compute_scoring_code_sha()` check returned `5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c`, and `_check_scoring_code_drift(config=config)` returned `[]`.

## 1. Nine-Question Anti-Hardcode Kernel Results (cumulative)
1. Q1 — Yes, the cumulative range adds regex/heuristic checks, but they are scoped to the meta-loop structural guard and not to customer-runtime semantic routing. Representative patterns are the generic `if ... then`, `if ... ->`, `cs<id>`, id-assignment, MUST/NEVER, and code-style `.contains/.matches` shapes in `autoloop/autoloop/sandbox/anti_hardcode_check.py`; S-Auto-1's sandbox allowlist is path-structural only. No Java runtime, Skill YAML, or eval-case semantic routing rule was added.
2. Q2 — No new Tier-0 invariant is created. S-Auto-2 Layer 0 consumes the existing Python `hard_checks` family plus skipped legacy Java-gate metrics if present; S-Auto-4's `Q2.tier0_invariant_invention_attempt` detects attempts to invent invariants rather than blessing one. `loop.py` state-machine guards are orchestration/error-handling guards, not runtime semantic invariants.
3. Q3 — The cumulative design uses soft/observable signals where appropriate: `FLAG_FOR_CODEX` continues the iteration and sets `anti_hardcode_flag_for_codex`, gaming checks are observation-only in v1, and shadow feedback to the loop is aggregate-only. High-confidence sandbox/content/detector FAILs are structural integrity boundaries and appropriately hard-reject.
4. Q4 — No cumulative runtime/prompt/judge config surface encodes a specific visible eval CaseSpec id, session id, raw user utterance, or expected answer. I read `analyze.txt`, `propose.txt`, and `compact.txt`; greps found schema placeholders such as `<case_id>` and a generic anti-example in `propose.txt`, but no concrete `cs011`/`cs015`-style ids or raw eval utterances. The detector source grep for known case ids and sampled eval/status strings returned no hits.
5. Q5 — No semantic ownership moves from the LLM to Java. The cumulative hard-fence diff over `server/`, `server/src/main/resources/`, `eval/src/main/java/`, inner `eval_interactive/`, and CaseSpec trees is empty; all new decisioning is build-time/meta-loop validation of proposed Skill text.
6. Q6 — No prompt if-else rule dump was added. `propose.txt` teaches the §1.7 forbidden-list and includes one negative example of an IF/THEN UC-specific hard rule, then asks for narrative guidance; it does not instruct the LLM to follow a keyword/if-else routing matrix.
7. Q7 — Tool schema, capability/permission boundary, PII/safety floor, and grounding floor are preserved by hard-fence diff: runtime-loaded resources and Java runtime are byte-identical across the reviewed range, and eval-interactive schema is not mutated.
8. Q8 — Generalization coverage is cumulative, not target-only: S-Auto-1 supplies 4 positive + 19 negative sandbox fixtures plus direct API edge cases; S-Auto-2 supplies 5-layer lexicographic/shadow-firewall tests; S-Auto-4 supplies the 11/11 forbidden FAIL + 4/4 clean PASS + 2/2 borderline FLAG calibration table plus detector/content/gaming integration tests.
9. Q9 — The temporary/interim element is already tracked: the S-Auto-4 `Whenever ... =>` synonym bypass remains the downgrade-to-signal follow-up trigger and is recorded as `R-S57-anti-hardcode-whenever-arrow-synonym-bypass` for M-Auto-1B detector calibration. No additional temporary hardcode was found.

## 2. Hard-Fence Walk (cumulative; §6 + §6.1)
The §6 hard-fence command over `1fb2062..b6b627b` returned empty for `server/`, `eval/src/main/java/`, inner `eval_interactive/eval_interactive/`, `eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`, `data/`, `db/migration/`, runtime resources, foundational docs, `docs/runtime_freeze_and_risk_policy.md`, and `docs/current/iteration_governance.md`.

OQ-S56.1 is honoured exactly. The only top-level `eval_interactive/eval_interactive.yaml` diff is the blessed env-var indirection: the literal `bot.base_url: http://localhost:8080` is replaced with `bot.base_url: ${CSAGENT_BACKEND_URL}` and the comment block documenting the autoloop alternate-port routing. No second eval-interactive surface edit exists in the cumulative range.

Sprint archive integrity holds: `docs/sprints/sprint-001-*`, `docs/sprints/sprint-053-*`, and prior `docs/milestones/` archives are unchanged. New M-Auto-1A sprint archives 054-057 are in-scope additions.

## 3. Sandbox + Detector + Gaming Cumulative Consistency
The cumulative chain composes in the intended order in `autoloop/autoloop/loop.py`: proposer returns a validated `Hypothesis`, content validation runs at step 2.5, sandbox validation runs at step 3, anti-hardcode detection runs at step 4, tier evaluation runs at step 9, and gaming checks run at step 9.5 after the lexicographic verdict. This ordering means content-broken hypotheses discard before the sandbox, structural field/path violations discard before the detector, and detector FAILs discard before apply/eval.

Adversarial checks matched that reading:
- Structural wrong field with content-clean text: `validate_content` returned PASS, then `validate_skill_yaml_diff` returned `REJECT` with `modification to non-mutable field(s): $.applicable_use_cases`; detector was not needed.
- Content-broken allowed field: empty `after_value` returned `FAIL / content_validator.zero_length`; sandbox and detector were not reached.
- Allowed field with IF/THEN hardcode text: content PASS, sandbox ACCEPT, detector `FAIL / Q1.if_then_decision_tree`.
- Allowed narrative soft guidance: content PASS, sandbox ACCEPT, detector PASS; this is expected and must be left to eval/LLM judgement.

Shadow firewall still holds end-to-end. `tier_evaluator.evaluate(..., audit=False)` returns only aggregate Layer 4 metrics; `audit=True` is the human-only path. The requested regression tests exist and passed in the full autoloop run: `test_experiments_log_shadow_firewall_no_per_case_key`, `test_shadow_firewall_holds_in_experiments_log`, and `test_cli_audit_default_respects_shadow_firewall`.

`gaming.py` also composes without breaking the fence: `shadow_set_leakage` scans only configured `config.gaming.shadow_leak_signatures` on non-audit surfaces, and `scoring_code_drift` is silent after the close-bundle SHA-256 fix-up.

## 4. Meta-Agent Prompt §1.7 Boundary Check
I read all three prompt files in full: `autoloop/autoloop/meta_agent/prompts/analyze.txt` (60 lines), `propose.txt` (108 lines), and `compact.txt` (49 lines).

- The prompts teach the LLM the boundary in prose: sanitize summaries, do not quote raw eval phrases, do not leak case ids in prose/lessons, output one Skill YAML edit, stay within the six-file/four-field mutable surface, and avoid §1.7 forbidden patterns.
- `propose.txt` lists the six files and four field paths as structural mutable-surface contract, not semantic routing rules. It also names locked structural fields to avoid, which mirrors the sandbox boundary rather than creating a semantic matrix.
- The prompt grep found no concrete eval ids such as `cs011`, `cs015`, or `cs59s`, no session id literals, and no expected assistant answers. The only case-id strings are schema placeholders like `<case_id>` and discipline text saying not to reference case ids.
- `propose.txt` includes a single negative example using `appeal`/`UC-H` to illustrate what not to write as an IF/THEN hard rule, followed by narrative guidance. I do not classify that as a prompt-level §1.7 violation because it is not a keyword list, not a visible eval utterance, and not an instruction to route by that word.

## 5. program.md Status Table Coherence
`autoloop/program.md` §4 is closure-coherent. The sandbox row points to S-Auto-1, the lexicographic fitness row is marked **DELIVERED** for S-Auto-2, the anti-hardcode auto-check row is marked **DELIVERED** for S-Auto-4, the shadow gate/firewall rows point to S-Auto-2, and the no-main-branch-cherry-pick row is marked **DELIVERED** Hybrid for S-Auto-3/OQ-S55.1. No row remains `DEFERRED` or `PARTIAL`.

## 6. Reproducibility Spot-Checks
- `cd autoloop && uv run --extra dev pytest -q` needed escalated execution because the sandbox could not read uv's cache under `~/.cache`; the approved rerun reproduced `216 passed, 1 warning in 3.35s`.
- `cd eval_interactive && uv run python -m pytest --tb=no -q` reproduced the inherited eval-interactive Python baseline: `3 failed, 486 passed in 13.22s`. The named failures were `test_v2_schema_loads_cleanly`, `test_smoke_review_report_tracks_smoke_set_and_overrides`, and `test_full_corpus_lints_clean_with_smoke_subset_flag`.
- `mvn test -B -pl server` inside the sandbox failed with sandbox-shaped socket/Mockito-agent errors. The escalated rerun reproduced the inherited Java baseline: `Tests run: 1183, Failures: 1, Errors: 0, Skipped: 2`; the lone failure was `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`.
- `python` direct check from `autoloop/` confirmed `gaming._compute_scoring_code_sha()` equals the configured close hash and `_check_scoring_code_drift(config=config)` returns `[]`.

## 7. Cumulative Adversarial Spot-Check
No new cumulative-level hole was found beyond the already-tracked `Whenever ... =>` synonym bypass. The composition spot-checks show each intended layer catches its own class: content integrity failure at content_validator, structural field/path failure at the sandbox, and allowed-field IF/THEN hardcode at the detector. A detector-clean, sandbox-clean narrative guidance edit remains eligible for eval, which is the desired LLM-first boundary rather than a hole.

## 8. Bad-Case Suite + Shadow + Live-Iter Smoke Evidence Audit
Codex did not re-judge the human-owned §5.6 bad-case verdicts. Presence and consistency audit:

- `docs/10-handoff.md` §1 records bad-case suite rerun COMPLETE for run id `20260527-102809`, 12 cases, with 2/12 programmatic drifts (`cs012_uc_fp_late_phone_failure_path`, `wmkb_uc_a_trader_flag_secondary_uc_h`) and a deliver-agent + human §5.6 PASS on 2026-05-28. The internal consistency check is satisfied because the hard-fence diff shows no runtime Java, runtime resources, or inner eval-interactive Python/code path touched in `1fb2062..b6b627b`.
- `docs/10-handoff.md` §1 records shadow regression-safety rerun COMPLETE/PASS: 19/22 matched M5 exactly, 3/22 UC-D cases drifted in the same `True/0.5 -> False/0.0` direction, and the known session-establishment failures match M5 with no new shadow regression beyond `R-shadow-fixture-empty-form-session-create-400`.
- Live-iter smoke classification: **explicitly-blocked-or-waived**. `docs/10-handoff.md` §1 and `docs/milestone_objective.md` §6.2 record WAIVED-BY-HUMAN at M-Auto-1A milestone close because the meta-agent key exists in `autoloop/.env.local` but a built `server/` jar is not present; this is an explicit waiver, not a silent skip.

## Verdict
approve with downgrade-to-signal follow-up

Follow-up trigger: consume `R-S57-anti-hardcode-whenever-arrow-synonym-bypass` in M-Auto-1B detector calibration. This milestone-shared pass found no additional cumulative hardcode or composition blocker.

## Notes for M-Auto-1B planning round
- Consume `R-S57-anti-hardcode-whenever-arrow-synonym-bypass` early: calibrate start-of-string/word-boundary `WHEN`/`WHENEVER` and arrow normalization, or turn equivalent WHEN/arrow decision-tree shapes into `FLAG_FOR_CODEX`, without hardcoding the Codex-authored phrase.
- Treat the bad-case + shadow one-direction drift (5/34 total, no runtime/eval code touch) as provider-drift observation for the first overnight batch; do not auto-discard candidates solely because of this close-day drift pattern.
- Before the first true live iteration, run the waived live smoke prerequisites explicitly (real meta-agent key, built server jar, clean autoloop branch) and include process-lifecycle verification for the Maven/Spring subprocess cleanup path, which unit tests currently mock rather than exercising with a real child process.
