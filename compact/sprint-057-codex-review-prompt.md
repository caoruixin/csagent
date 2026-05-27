# Codex review prompt — Sprint 57 / M-Auto-1A S-Auto-4 — Per-sub-sprint Anti-Hardcode review (§4.3 trigger #2)

You are the **Anti-Hardcode Per-Sub-Sprint Review Agent for Sprint 57 / M-Auto-1A S-Auto-4**. This is a **per-sub-sprint review** dispatched per `docs/current/iteration_governance.md` §4.3 trigger #2 — the S-Auto-4 sub-sprint ships the structural §1.7 guard (anti-hardcode kernel + content validator + gaming checks) and **must be Codex-verified BEFORE the M-Auto-1A milestone close** can proceed. The milestone-shared Codex review (cumulative S-Auto-1..S-Auto-4 range) is a **separate** later pass dispatched at milestone close; this prompt covers S-Auto-4 ONLY.

The cumulative commit range under review for THIS Codex pass:

```
1feef1f  Sprint 57 / S-Auto-4 — anti-hardcode kernel + content validator + gaming checks
```

(parent: `cf0127f` — Sprint 56 / S-Auto-3 close + Sprint 57 / S-Auto-4 planning round + anti-hardcode kernel contract)

---

## Read order (minimal)

Read only:
1. `AGENTS.md` (auto-loaded via constitution chain — pulls `doc_governance.md` + `agent_context_guide.md` + `iteration_governance.md`).
2. This prompt (self-contained executable view).
3. `docs/sprints/sprint-057-handoff.md` — dev's own S-Auto-4 close handoff (single source you may not embed because it post-dates this prompt's authoring; read it in full).

You may sample (NOT embed) the following code paths for verification:
- `autoloop/autoloop/sandbox/anti_hardcode_check.py` — the body Codex verifies (signature is verbatim from S-Auto-3).
- `autoloop/autoloop/sandbox/content_validator.py` — NEW; structural integrity validator.
- `autoloop/autoloop/scoring/gaming.py` — NEW; 7 anti-gaming checks.
- `autoloop/autoloop/loop.py` — additive wiring extensions at steps 2.5 / 4 / 9.5.
- `autoloop/tests/test_anti_hardcode_check.py` — 348 LOC; the calibration table + detector self-discipline regression.
- `autoloop/tests/test_content_validator.py` — 251 LOC.
- `autoloop/tests/test_gaming.py` — 468 LOC.
- `autoloop/tests/test_loop_anti_hardcode_wire.py` — 340 LOC; integration wire test.
- `autoloop/tests/test_loop.py::test_adversarial_fixture_fails_real_detection` (line 480) — the renamed + flipped S-Auto-3 adversarial fixture; this is the load-bearing regression target.
- `autoloop/config.yaml` — 3 NEW blocks `content_validator:` + `anti_hardcode:` + `gaming:` + `fitness.scoring_code_baseline_sha` filled `"1feef1f"` at S-Auto-4 close.
- `docs/sprints/sprint-056-handoff.md` — S-Auto-3 close; describes the placeholder signature S-Auto-4 must preserve (for backward-compat sanity check).
- `docs/milestone_objective.md` §6 + §6.1 — milestone hard fences + OQ-S56.1 disposition (the only blessed out-of-fence edit is `eval_interactive/eval_interactive.yaml` at S-Auto-3; S-Auto-4 introduces NO additional out-of-fence edits).

**Do not** re-read the milestone-level hard fences in full from `docs/milestone_objective.md` for letter-of-fence verification — they are embedded below for Codex's hard-fence check, and the OQ-S56.1 disposition is the only out-of-fence exception (S-Auto-4 ships zero additional out-of-fence edits per the dev's hard-fence verification at handoff §9).

---

## Embedded sub-sprint contract (cumulative scope claim)

### Goal of S-Auto-4 (from `docs/sprints/sprint-057-objective.md` archive — embedded verbatim)

S-Auto-4 closes the §1.7 enforcement chain whose plumbing S-Auto-1/2/3 built. At close, the auto-loop pipeline must:

1. **Real anti-hardcode detector replaces S-Auto-3 placeholder.** Hook signature `def anti_hardcode_check(hypothesis, *, config) -> AntiHardcodeResult` verbatim preserved; only body changes. The S-Auto-3 adversarial fixture `if user.message contains 'appeal' then route to UC-H else UC-A` flips from PASS (placeholder) to FAIL (real detection). Verdicts ∈ `{PASS, FAIL, FLAG_FOR_CODEX}`.
2. **Content validator at propose-stage pre-sandbox.** NEW `content_validator.py` runs structural validity checks (placeholder integrity for tokens already in `before_value`, zero-length, length overflow >5×, length underflow <0.1×, configurable deny-list). FAIL → discard with `discard_reason=content_validator_rejected:<rule_id>` BEFORE sandbox YAML diff validate.
3. **Anti-gaming checks at post-eval.** NEW `gaming.py` runs the 6 ported checks from `docs/proposals/autoloop_design.md` §8 + the NEW `tier2_measurement_contract_change_attempt`. **All checks OBSERVATION-ONLY in v1** — flags persist to `experiments_log.gaming_flags` + surfaced by `audit`; do NOT auto-discard.
4. **`autoloop/program.md` §4 row 3** flips `S-Auto-4 — DEFERRED ...` → `S-Auto-4 — DELIVERED (deterministic regex+heuristic detector covering Q1/Q2/Q4/Q5 of the §4.1 nine-question kernel; PASS / FAIL / FLAG_FOR_CODEX verdicts; FLAG_FOR_CODEX is observation-only in v1, never auto-promoted to FAIL by the detector itself)`.

**Zero touch** to S-Auto-1/2/3 deliverable signatures (`anti_hardcode_check` body swap, signature unchanged; `loop.py` two wiring insertion points + `IterationResult` gains 3 additive optional fields with defaults; `cli.py audit` extends render surface but signature unchanged; `eval_runner.py` / `tier_evaluator.py` / `baseline_loader.py` / `sandbox/yaml_diff_validator.py` / `meta_agent/*` UNTOUCHED). **Zero touch** to `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`, `server/`, `eval/` Java, `data/`, `db/`, `server/src/main/resources/`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, `docs/sprints/sprint-0**` archives, `docs/milestones/*` archives, `docs/codex-findings.md`. **No new out-of-fence eval_interactive surface edit beyond the OQ-S56.1 disposition recorded at S-Auto-3 close** (which is unchanged in this sub-sprint).

### Three human-locked D-clauses (2026-05-27; carry-forward from the S-Auto-4 contract)

- **D1 — Detector mechanism**: regex + heuristic ONLY (stdlib `re` + `unicodedata` + small synonym map). NO second LLM in the propose-stage critical path. `FLAG_FOR_CODEX` is a human / Codex review signal — **NOT auto-promoted to FAIL** at propose-stage and **NOT auto-demoted to PASS** at propose-stage.
- **D2 — Detector pattern scope**: detector encodes **generic structural patterns ONLY**.
  - Allowed: IF/WHEN/THEN/ELSE decision-tree shapes; `cs<id>`-like tokens; `session_id`-like / `case_id`-like / `iteration_id`-like tokens; MUST/NEVER/Tier-0-like phrasing structures; `.contains(...)` / `.matches(...)` literals.
  - Forbidden: specific eval case wording (e.g., the literal phrasing of a `cs011` user turn); specific user utterances (e.g., "my account is locked" as a banned phrase); specific expected assistant answers; known case success/failure labels (e.g., `closure_criterion` text fragments).
  - If a rule requires a specific eval phrase to fire, the rule is structurally wrong and must be redesigned.
- **D3 — `gaming.py` input contracts**:
  - `scoring_code_drift`: if `config.fitness.scoring_code_baseline_sha` is missing/None → emit `WARN rule_id=scoring_code_drift.baseline_missing`. **MUST NOT** guess, **MUST NOT** silently hash-and-use as baseline, **MUST NOT** auto-pass.
  - `shadow_set_leakage`: detector **MUST NOT** read `eval_interactive/case_specs_shadow/` content directly. Leak signatures **MUST** come ONLY from `config.gaming.shadow_leak_signatures` (out-of-band human-approved config).

### Dev-reported delivery (from `docs/sprints/sprint-057-handoff.md`)

- `anti_hardcode_check.py` body swap — 11 rules across Q1/Q2/Q4/Q5; PASS/FAIL/FLAG_FOR_CODEX; signature preserved; `placeholder=False` in all returns.
- `content_validator.py` (NEW, 187 LOC) — 5 rules; pre-sandbox stage; D3-compliant (no Salesforce-specific defaults).
- `scoring/gaming.py` (NEW, 627 LOC) — 7 checks; ALL observation-only; D3 invariants enforced (config-driven leak signatures; no shadow corpus reads; baseline_missing WARN not guess).
- `loop.py` (+151 LOC) — additive wiring at steps 2.5, 4 (flag-attach), 9.5; `IterationResult` gains 3 additive optional fields with defaults.
- `cli.py audit` (+16 LOC) — surfaces `gaming_flags_summary` + `anti_hardcode_flag_for_codex`; shadow firewall RESPECTED by default.
- `config.yaml` (+70 LOC) — 3 new blocks + `fitness.scoring_code_baseline_sha: null` placeholder (deliver-agent backfilled `"1feef1f"` at S-Auto-4 close).
- `program.md` §4 row 3 — DEFERRED → DELIVERED.
- `README.md` (1 line) — audit row note updated.

Calibration (run via `uv run pytest -q tests/test_anti_hardcode_check.py` + standalone sweep):
- **11/11 forbidden → FAIL** (100% catch across Q1 [4 rules] + Q2 [2 rules] + Q4 [2 rules] + Q5 [3 rules]).
- **4/4 clean → PASS** (0% false positive across the 4 allowed Skill YAML field types).
- **2/2 borderline → FLAG_FOR_CODEX**.
- **FLAG_FOR_CODEX rate = 2/17 = 11.76%** — well under the 25% warn threshold (`config.anti_hardcode.flag_for_codex_rate_warn_threshold: 0.25`).

Pytest: autoloop **147 → 216 passed** (+69 new); `eval_interactive` **486 passed, 3 failed** UNCHANGED.

Hard fences: `git diff --stat cf0127f..1feef1f` against the enumerated hard-fenced paths returns empty (only `docs/sprints/sprint-057-handoff.md` — the new sub-sprint's own archive). Deliver-agent independently reproduced 2026-05-27.

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

## S-Auto-4-specific verification axes (per `iteration_governance.md` §4.3 trigger #2)

Beyond the §4.1 nine-question walk, this per-sub-sprint review must independently verify the following five axes. The kernel walk is the GENERAL question; these axes are the S-Auto-4-SPECIFIC questions that warrant the per-sub-sprint Codex dispatch.

### Axis A — Detector self-discipline (D2 regression)

The detector itself MUST encode generic structural patterns ONLY. A rule whose match depends on a specific eval phrase / user utterance / expected assistant answer / case-success-or-failure label is, by D2, structurally wrong. Codex must:

1. Read `autoloop/autoloop/sandbox/anti_hardcode_check.py` (the body that replaces the S-Auto-3 placeholder).
2. Inspect each of the 11 rule definitions (Q1 ×4, Q2 ×2, Q4 ×2, Q5 ×3 per `sprint-057-handoff.md` §4 calibration table).
3. Verify each rule matches by **structural shape** (IF/THEN form; `cs<id>` regex shape; `session_id`-like token; MUST/NEVER phrasing; `.contains(...)` / `.matches(...)` literal) and **NOT** by a specific eval phrase / known case spec wording / specific user utterance / specific expected answer / known case outcome label.
4. Confirm `autoloop/tests/test_anti_hardcode_check.py` ships a **detector self-discipline regression test** that grep-scans the rule source for forbidden literals (e.g., `cs011`, `cs015`, `closure_criterion`, specific bot dialogue) and asserts they are absent. Report whether this test exists, what it scans for, and whether it actually FAILS under a synthetic injection (e.g., does the test catch a rule that mentions `cs011` verbatim?).

If any rule fails axis A → `reject as semantic hardcode`.

### Axis B — Calibration coverage + adversarial robustness

1. Reproduce the 11 forbidden + 4 clean + 2 borderline fixture results from the dev's calibration table. They must replay PASS at HEAD `1feef1f`.
2. Construct **at least three additional hand-authored adversarial proposes** Codex has not been shown by the dev. Recommended categories (use at least one from each):
   - **Unicode obfuscation**: visually-similar character substitution (Cyrillic `і` for Latin `i`; full-width or zero-width insertions; emoji-as-spacer); the detector must still match after NFKC.
   - **Multi-line decomposition**: split an `IF ... THEN ...` decision tree across several lines, bullets, or section breaks; the detector must still recognise the IF/THEN structural shape.
   - **Semantic-equivalence via synonym swap**: replace `IF` with `WHEN`, `THEN` with `→` or `=>` or `→`, `MUST` with `is required to`, `do not consider` with `disregard`. Note: `config.anti_hardcode.synonym_map_enabled` defaults `false` in v1; flagging that it would catch one of these obfuscations is itself a valuable finding (the right verdict is `approve with downgrade-to-signal follow-up: enable synonym map at M-Auto-1B if any miss observed`).
3. Verify ≥ 2 of the 3 Codex-authored adversarials are detected (FAIL) at HEAD `1feef1f`. A bypass on 1 of 3 is `approve with downgrade-to-signal follow-up` (record the bypass as a M-Auto-1B fix-iteration target). A bypass on ≥ 2 of 3 is `reject as semantic hardcode` (detector design hole).

### Axis C — D3 regressions on `gaming.py`

1. Inspect `autoloop/autoloop/scoring/gaming.py` _and the `test_gaming.py` fixture set_:
   - Confirm `shadow_set_leakage` rule reads ONLY `config.gaming.shadow_leak_signatures` (out-of-band human-approved config) and does NOT open any file under `eval_interactive/case_specs_shadow/`. Verify by grep of `gaming.py` for `case_specs_shadow` / `shadow_path` / equivalent path strings — only references should be in config-comments or string-comparisons against the signatures list. NO `open()` / `Path(...).read()` / `os.listdir()` against the shadow corpus.
   - Confirm `scoring_code_drift` returns `WARN baseline_missing` when `config.fitness.scoring_code_baseline_sha` is `null` / missing — does NOT silently compute and adopt a hash. (Note: deliver-agent backfilled `"1feef1f"` at S-Auto-4 close — Codex confirms by reading the current `autoloop/config.yaml` value AND inspecting `gaming._check_scoring_code_drift`'s missing-baseline branch.)
2. If either D3 invariant is violated → `reject as semantic hardcode` (technically a D3 contract violation, but the verdict set has no separate category; classify as semantic hardcode because both invariants exist to PREVENT the detector from becoming a hardcode pump or a covert eval-corpus reader).

### Axis D — Backward-compat surface preservation

S-Auto-4 must NOT change S-Auto-1 / S-Auto-2 / S-Auto-3 deliverable signatures. Codex must:

1. Verify the `anti_hardcode_check(hypothesis, *, config) -> AntiHardcodeResult` signature in `autoloop/autoloop/sandbox/anti_hardcode_check.py` is byte-identical to the S-Auto-3 placeholder declaration (compare `git show cf0127f^:autoloop/autoloop/sandbox/anti_hardcode_check.py` to extract the placeholder signature and `git show 1feef1f:autoloop/autoloop/sandbox/anti_hardcode_check.py` for the current one). The dataclass `AntiHardcodeResult.placeholder` field MUST still exist and MUST default to `False` (or be set `False` in every S-Auto-4 return).
2. Verify `autoloop/autoloop/loop.py` `IterationResult` dataclass additions are **additive optional** (defaults provided), so S-Auto-3 tests constructing `IterationResult` without the new fields still PASS.
3. Verify the `cli.py audit` subcommand signature is unchanged (argparse subparser definition still uses the same flags); only the rendering output surface grew.
4. Verify `eval_runner.py` / `tier_evaluator.py` / `baseline_loader.py` / `sandbox/yaml_diff_validator.py` / `meta_agent/*` files are byte-identical between `cf0127f` and `1feef1f` (`git diff cf0127f..1feef1f -- <these paths>` returns empty).

If any axis-D check fails → `needs human architecture decision` (backward-compat is a deliver-agent + human policy call, not a pure semantic-hardcode question).

### Axis E — FLAG_FOR_CODEX semantics not abused

`FLAG_FOR_CODEX` exists as a Codex / human review signal — NOT an auto-pass. Codex must:

1. Confirm `loop.py` step 4 wiring attaches `iteration_record.anti_hardcode_flag_for_codex = True` and **continues** the iteration to apply / eval (the iteration is NOT discarded on FLAG; it is auto-logged for later human / Codex review during the M-Auto-1B kept-candidate batch).
2. Confirm there is NO code path that silently flips a FAIL result down to FLAG_FOR_CODEX based on detector confidence drift; all FAIL → FAIL, no auto-relaxation.
3. Confirm the 2 borderline calibration cases ("Operators must always escalate suspicious payment activity." and "Never reject a verified document without supervisor review.") are genuinely ambiguous (a human could legitimately argue either way); they are not failures the dev hid under a FLAG label to inflate the FAIL→PASS calibration scorecard.

If axis E fails → `approve with downgrade-to-signal follow-up` (the dev must clarify the FLAG semantics in a follow-up sub-sprint; M-Auto-1A milestone close may still proceed if axes A–D pass).

---

## Hard-fence verification (embedded)

The milestone-level §6 hard fences from `docs/milestone_objective.md` apply. S-Auto-4 must touch ONLY `autoloop/**` source + tests + 1 row of `autoloop/program.md` + 1 line of `autoloop/README.md` + the new `docs/sprints/sprint-057-handoff.md`. Codex independently runs:

```
git diff --stat cf0127f..1feef1f -- \
  server/ eval/src/main/java/ eval_interactive/eval_interactive/ \
  eval_interactive/case_specs/ eval_interactive/case_specs_shadow/ \
  data/ db/ server/src/main/resources/ \
  docs/foundational/ docs/runtime_freeze_and_risk_policy.md \
  docs/current/ docs/sprints/sprint-0 docs/milestones/ \
  docs/codex-findings.md eval_interactive/eval_interactive.yaml
```

Expected output: ONLY `docs/sprints/sprint-057-handoff.md` appears (the new sub-sprint's own archive — sprints 058+ are the only excluded prefix per §6.5 wording). All other listed paths must return empty.

The **OQ-S56.1 blessed surface** (`eval_interactive/eval_interactive.yaml`) MUST remain byte-identical between `cf0127f` and `1feef1f` (S-Auto-4 introduces no further out-of-fence eval-interactive edit). Verify via `git diff cf0127f..1feef1f -- eval_interactive/eval_interactive.yaml` → empty.

---

## Output format (per `iteration_governance.md` §4.2)

Write findings to `docs/codex-findings.md`. Use the **per-sub-sprint header format** (the milestone-shared header is reserved for the later milestone-close pass):

```
## Sprint 57 / S-Auto-4 Per-Sub-Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph naming verdict + the axes that were dispositive>

### §4.1 nine-question kernel walk
<Q1..Q9 walked; for any "yes" or concern, paste the diff snippet + reasoning>

### Axis A — Detector self-discipline (D2)
<findings>

### Axis B — Calibration + ≥3 hand-authored adversarials
<3 Codex-authored adversarial proposes verbatim + per-proposal detector verdict at HEAD 1feef1f>

### Axis C — D3 regressions on gaming.py
<findings>

### Axis D — Backward-compat surface preservation
<findings>

### Axis E — FLAG_FOR_CODEX semantics not abused
<findings>

### Verdict
<one of: approve | approve with downgrade-to-signal follow-up | reject as semantic hardcode | needs human architecture decision>

### Notes for milestone-shared Codex (handoff to the M-Auto-1A close pass)
<observations the milestone-shared pass should cross-check on the cumulative S-Auto-1..S-Auto-4 range>
```

---

## Constraints

- **Do NOT edit code.** Codex's role is to read + reason + return a verdict. The fix layer naming is allowed (per the kernel's final note); the actual fix is a deliver-agent + dev fix-iteration sub-sprint.
- **Do NOT re-judge §5.6 bad-case suite human-judgment verdicts.** That gate is the deliver-agent + human's at the M-Auto-1A milestone close, not Codex's at S-Auto-4 close.
- **Do NOT broaden scope beyond S-Auto-4** (the `1feef1f` commit only). The milestone-shared cumulative review covers S-Auto-1..S-Auto-4 at milestone close — separate prompt, separate Codex pass.
- **Do NOT propose new Tier-0 invariants.** Q2 of the kernel detects attempts to invent Tier-0; Codex's job is to flag such an attempt, not to bless one.
- **The verdict set is exactly the four options listed in the §4.1 kernel.** No verdict invention.
- **Codex MUST return `pass` (or `approve with downgrade-to-signal follow-up`) BEFORE M-Auto-1A milestone close can proceed.** `reject` triggers a fix-iteration sub-sprint extending M-Auto-1A. `needs human architecture decision` halts and surfaces to deliver-agent + human.

Cumulative scope claim: `1feef1f` covers all four scope items of `docs/sprints/sprint-057-objective.md` (anti-hardcode kernel real body + content_validator + gaming.py 7 checks + loop wiring + cli.py audit extension + config.yaml 3 NEW blocks + program.md §4 row 3 flip + README audit row note); dev-verified 216 pytest PASS at handoff time + deliver-agent reproduced at S-Auto-4 close.
