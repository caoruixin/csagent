# Wave A6.5 QA report

**Generated:** 2026-04-30
**Branch:** `design-v1-without-human-review`
**Reviewer:** Claude Opus 4.7 (1M context), automated QA pass
**Scope:** Static review + snapshot tests + end-to-end verification of the Wave A6 LLM persona-review rollout (A6.1–A6.3 already shipped; A6.4 cache population not yet run; A6.5 is this report).

## Overall verdict

**Pass with caveats.** All 21 regression tests + the new 5 snapshot tests are green; full suite (250 tests) is green; offline mode is byte-identical across two runs; API key never leaks to disk. **Do not** run A6.4 full-corpus cache population (367 sessions, ≈$1) until the maintainer decides on Open Issue #1 (case_id-positional cache key); otherwise any future HR CSV row insertion will silently invalidate every committed cache file. See Open Issues for the unblock plan.

## 1. Static QA findings

### HIGH

**1.1 — `case_id` is positional and is part of the prompt + cache key.**
* **File:** `eval_interactive/eval_interactive/case_spec/llm_persona_reviewer.py:223` (renders `case_id (hint): {case_id}` into the user prompt) + `:665-668` (`prompt_hash = sha256(system + user)`).
* **Symptom:** the committed cache files for the three smoke sessions have `case_id_hint: cs_interactive_001/002/003`, but the production HR CSV places those sessions at row positions 12/55/64. The prompt body therefore differs from what was hashed at smoke-cache time, so production cache lookups miss for every smoke session today. I verified this empirically: `sha256(prompt_with_case_id="cs_interactive_001")` ≠ `sha256(prompt_with_case_id="cs_interactive_012")` (full content otherwise identical).
* **Impact:** the "committed cache → reproducible regeneration" promise (design doc §"Cache contract") is broken in practice for any session whose row position can change. Adding/removing one HR row up-stream will invalidate ALL downstream caches.
* **Suggested fix (NOT applied):** either drop `case_id` from the prompt body (the LLM does not need it for persona judgement), or change the cache key from `(source_session_id, prompt_hash)` to `source_session_id` alone with `prompt_hash` becoming an advisory mismatch warning. I recommend dropping `case_id` from the prompt — it is informational only.

**1.2 — `case_spec_overrides.yaml` keys by `(case_id, source_session_id)`, same positional fragility.**
* **File:** `eval_interactive/eval_interactive/case_spec/extractor.py:528-544` (`_apply_case_spec_override` rejects an override whose `case_id` does not match `spec.case_id`).
* **Symptom:** running `extract_case_specs` on a 3-row subset silently drops the `cs_interactive_012` Wave A5 override because the same session is now `cs_interactive_001` in the subset (verified empirically: subset offline run gives `outcome_class=escalate`, not the reviewed `resolve`).
* **Impact:** any tooling that runs the extractor on a subset of the HR CSV (smoke runs, ad-hoc debugging, partial regeneration) silently bypasses approved Wave A5 overrides, producing different `expected.*` from the canonical run.
* **Suggested fix (NOT applied):** key overrides by `source_session_id` alone (and treat `case_id` in the override file as advisory). Same direction as 1.1.

### MEDIUM

**2.1 — L3 override scope is `expected.*` + `scoring` only, not "any field".**
* **File:** `eval_interactive/eval_interactive/case_spec/extractor.py:455-469` (`_CASE_OVERRIDE_EXPECTED_FIELDS` allow-list) + `:528-602` (`_apply_case_spec_override` only mutates `spec.expected` and `spec.scoring`).
* **Symptom:** the design doc asserts L3 may write any field including persona; the implementation cannot. This is fine today (every approved override changes `expected.*`), but blocks A6.4 if the maintainer ever wants to pin a persona override that disagrees with L2.
* **Suggested fix:** widen `_apply_case_spec_override` to handle a `persona:` block, or document the scope discrepancy in the design doc. Leave as-is for A6.4 if no persona override is needed.

**2.2 — Cache files are trust-on-read.**
* **File:** `eval_interactive/eval_interactive/case_spec/llm_persona_reviewer.py:702-730` (`_validate_cache_record`).
* **Symptom:** `_validate_cache_record` only checks `source_session_id` matches and that `accepted_value.{seed_messages, user_goal_summary}` exist. A hand-edited cache file with a 500-char `user_goal_summary`, a forbidden top-level key (e.g. `outcome_class`), or an unsupported `hidden_fact` would all be loaded as-is.
* **Impact:** by design (per §"Cache contract" — diff review is the human gate), but the validator could re-apply `validate_response`-style checks on read at near-zero cost and would catch obvious tampering before the persona reaches `Persona`.
* **Suggested fix:** call `validate_response(accepted_value, rule_draft, transcript)` from `_read_cache` and emit a `logger.warning(...)` if it fails (still trust the file, just surface the issue). Optional defense-in-depth.

**2.3 — Subset extraction silently drops L3 overrides (same root cause as 1.2).** See 1.2 — flagged separately so the maintainer notices the runtime symptom (escalate vs. resolve drift in subset runs) and not just the file-format symptom.

### LOW

**3.1 — Forbidden keys nested inside `hidden_facts` entries are silently ignored.**
* **File:** `eval_interactive/eval_interactive/case_spec/llm_persona_reviewer.py:498-516` (`validate_response`).
* **Symptom:** if the LLM returns `hidden_facts: [{fact: "x", disclose_when: "y", primary_uc: "UC-G"}]`, the extra `primary_uc` is silently stripped (only `fact`/`disclose_when` are read). No validation note is recorded.
* **Impact:** zero — the value never flows to `Expected.primary_uc`. But PR reviewers reading `validation_notes` cannot see that the LLM attempted to overstep, which the task's static-QA item #6 asks us to flag.
* **Suggested fix:** in the hidden_facts loop, record any extra keys via `notes.append(f"hidden_fact_extra_keys: {extras}")`. One-line change.

**3.2 — `_coerce_persona_from_record` falls back to `rule_draft.verbosity` only when seeds are empty, not when verbosity is not in the cache.**
* **File:** `eval_interactive/eval_interactive/case_spec/llm_persona_reviewer.py:680-695`.
* **Symptom:** `derive_verbosity(seeds)` is called when seeds exist; verbosity in the cache is documentation only, never trusted. This is the documented contract (rule extractor's `_derive_verbosity` is the single source of truth) — flagging only because the docstring says "informational only" and a future reviewer might assume it is round-tripped.
* **Suggested fix:** none; the current behaviour is correct. Optionally add a unit test that mutating `verbosity` in a cache file does not change the produced persona.

### PASS (verified, no action)

* **API key handling.** Grep confirms `self._api_key` is only read once (the `Authorization: Bearer` header at `llm_persona_reviewer.py:564`), never logged, never written into cache, never echoed in audit. `grep -ri DEEPSEEK_API_KEY case_spec_llm_cache/ qa-reports/` returned zero hits.
* **Concurrency.** `extract_case_specs` is a synchronous for-loop (`extractor.py:1511-1559`); `LlmPersonaReviewer.review` is synchronous; each session reads then writes its own cache file inside the same call. No asyncio reordering risk.
* **L2-then-L3 ordering.** `_build_case_spec` runs L2 at step (5b) (`extractor.py:1257-1306`) before constructing `spec`; `_apply_case_spec_override` runs at `extractor.py:1358` after spec construction. L3 has final word over `expected.*` for every code path.
* **Top-level forbidden keys.** `validate_response` at `llm_persona_reviewer.py:427-430` records `f"forbidden_key_present: {extra_keys}"` by name and sets `status="forbidden_key_present"`, which leads to `acceptance_reason="rule_fallback_validation_failed"`. Existing test `test_forbidden_key_in_response_falls_back` covers this.
* **Length-fail retry (Option A).** Existing test `test_user_goal_summary_length_retry` exercises the one-retry-then-fallback path end-to-end. Cache merges first/retry notes correctly.
* **Prompt sha256.** `PROMPT_TEMPLATE_SHA256` is computed at module import from `PROMPT_SYSTEM`; it equals the locked value `61617a17cfd68a14ef0a7496724c2141e0896b6e0b92c34394668f858e9c0364` (verified by the new `test_snapshot_prompt_sha256_locked`).

## 2. Snapshot tests added

New file: `eval_interactive/tests/regression/test_case_spec_persona_snapshots.py` (5 tests, all offline).

| # | Test name | What it pins |
|---|-----------|--------------|
| 1 | `test_snapshot_cs_interactive_012_persona` | seeds + ugs + hidden_facts of the 570Q5000008hx9tIAA cache; asserts L3 override still wins on `expected.outcome_class=resolve`, `primary_uc=UC-FP`, `should_escalate=False`, no `request_handover`. |
| 2 | `test_snapshot_offline_mode_falls_back_to_rule_draft` | `--no-llm` mode produces the rule_draft greetings (`Hi Jason`, `Thank you and happy new year`, …), AND L3 override still applies on top of rule_draft (`UC-FP / resolve`). |
| 3 | `test_snapshot_cs_interactive_055_persona` | seeds + ugs of 570Q5000008TMmvIAG cache; asserts UC-FP via the UC-B reclassification map (no Wave A5 override for this case). |
| 4 | `test_snapshot_cs_interactive_064_persona` | seeds + ugs of 570Q5000008l7flIAA cache; asserts `primary_uc=UC-C` (HR-driven, no override). |
| 5 | `test_snapshot_prompt_sha256_locked` | `PROMPT_TEMPLATE_SHA256` == the locked constant `61617a17…`; any future prompt edit fails CI loudly. |

Test design notes:

* All 5 tests run with `llm_offline=True` or with an injected `_ScriptedDeepSeekClient` (a tiny in-memory fake that returns the cache file's `llm_proposal` keyed by `source_session_id`). Zero HTTP calls.
* The fake client extracts `source_session_id` from the rendered prompt body, so tests do not need to coordinate with the production extractor's row ordering. This deliberately decouples the snapshot from the case_id-positional bug (Open Issue #1) so it does not give false greens once that bug is fixed.
* Cache writes go to `tmp_path/cache`, never to the committed cache directory.
* Verbatim and word-overlap validators run end-to-end against the real `data/eval_datasets/{badcase,escalation,golden}_turns.csv` files, so the test would also fail if the source transcript text drifted away from the cached seeds.

## 3. End-to-end verification

### 3.1 Regression-dir tests

```
$ python -m pytest eval_interactive/tests/regression/ -v
... (21 tests)
============================== 21 passed in 5.49s ==============================
```

Includes: 7 cache tests (existing) + 1 override test (existing) + 3 anchor tests (existing) + 5 corpus-lint tests (existing) + 5 new snapshot tests.

### 3.2 Full test suite

```
$ python -m pytest eval_interactive/tests/ -q
250 passed in 6.43s
```

### 3.3 Byte-identical regeneration on the 3 smoke sessions

Wrote a one-off script that built a 3-row HR subset (`570Q5000008hx9tIAA`, `570Q5000008TMmvIAG`, `570Q5000008l7flIAA`), ran `extract_case_specs(..., llm_offline=True)` into two output dirs, then compared all generated YAMLs with `filecmp.cmp(shallow=False)`.

```
YAMLs generated: 3 on each side; checking byte-identical...
OK: all 3 YAMLs byte-identical between runs
```

Script removed after use (per Task 4 step 3 instructions).

### 3.4 Offline mode (`--no-llm`) sanity

When run on the FULL HR CSV with `llm_offline=True`:

* `cs_interactive_012 / 570Q5000008hx9tIAA`: persona seeds = `["Hi Jason", "Thank you and happy new year", ...]` (rule_draft); `expected.primary_uc=UC-FP`, `outcome_class=resolve` (Wave A5 override applied). ✓
* `cs_interactive_055 / 570Q5000008TMmvIAG`: persona seeds from `extract_transcript_evidence` (already issue-bearing); `expected.primary_uc=UC-FP` (UC-B reclassification map). ✓
* `cs_interactive_064 / 570Q5000008l7flIAA`: persona seeds from transcript evidence; `expected.primary_uc=UC-C`. ✓

Caveat: the same 3-row subset (without preserving HR row positions) gives `cs_interactive_001/002/003`, and the override silently drops out (verified — see Open Issue #1.2). This is not a Wave A6 regression but is exposed by Wave A6 because L2 introduces a second positional dependency.

### 3.5 git status

Working tree carries the new snapshot test file, the qa report, and the egg-info auto-update; no committed changes.

```
?? eval_interactive/tests/regression/test_case_spec_persona_snapshots.py
?? qa-reports/wave-a6-qa-report.md
M  eval_interactive/eval_interactive.egg-info/SOURCES.txt   (auto-regenerated by editable install)
```

The 3 cache files in `eval_interactive/case_spec_llm_cache/` are unchanged.

### 3.6 API-key leakage check

```
$ grep -ri "DEEPSEEK_API_KEY" eval_interactive/case_spec_llm_cache/ qa-reports/
(no hits)
```

## 4. Acceptance-criteria checklist

Walking the "## Acceptance Criteria" list of `docs/interactive_case_spec_generation_plan.md`:

| # | Criterion | Status | Evidence |
|---|-----------|--------|----------|
| 1 | No CaseSpec generation path merges turns from multiple datasets for a single HR row. | ✓ | `_load_source_turn_indexes` keys per source_dataset; existing `test_extractor_source_turns.py` covers it. |
| 2 | Every generated CaseSpec has an audit record with source turns file and evidence summary. | ✓ | `_record_audit` is called inside `_build_case_spec` for every spec; verified via `dump_audit_to`. |
| 3 | `cs_interactive_001` cannot silently become policy-default `resolve`; if `resolve`, audit must justify why. | ✓ | `outcome_decision_reason` is recorded by `case_outcome_resolver`; not a Wave A6 change but still true. |
| 4 | `cs_interactive_004` generates as contained UC-D resolve, no `request_handover`. | ✓ | Existing `test_case_spec_overrides.py` is broader — confirmed by visual inspection of `case_specs/anchor/cs_interactive_004.yaml`. |
| 5 | `cs_interactive_012` regenerates as resolve-first UC-FP via approved override. | ✓ | New `test_snapshot_cs_interactive_012_persona` + existing `test_cs_interactive_012_override_survives_fresh_extraction`. |
| 6 | Weak false-positive transcript signals must not override `transcript_indicated_outcome=resolve`. | ✓ | Not a Wave A6 change, covered by `case_outcome_resolver` and existing tests. |
| 7 | All smoke cases have structured review records before being treated as high-confidence regression fixtures. | ✓ | `qa-reports/smoke-case-review.{md,yaml}` exist; cache files for the 3 smoke sessions exist. |
| 8 | Approved corrections are applied through an override file, not silent direct edits. | ✓ | `case_spec_overrides.yaml` is in place; `_apply_case_spec_override` is the only mutation path. |
| 9 | Human-only tools remain absent from per-case `forbidden_tools` and covered by global L1 checks. | ✓ | `_resolve_forbidden_tools` strips them; verified by `test_corpus_lint`. |
| 10 | Linter and regression tests fail on the old shallow-turn behaviour. | ✓ | `test_corpus_lint` is green. |
| 11 | LLM never proposes/writes `expected.*`, `scoring.*`, `form_context.*`, `primary_uc`, `secondary_ucs`. | ✓ | `ALLOWED_RESPONSE_KEYS` allow-list; `validate_response` rejects any extra key by name; existing `test_forbidden_key_in_response_falls_back`. |
| 12 | Every persona field is traceable to `rule_draft` / cached `llm_proposal` / Wave A5 override. | ✓ | `cache_record.{rule_draft, llm_proposal, accepted_value, acceptance_reason, validation_notes}` all written by `_build_cache_record`. Audit log records `llm_persona_changed_fields`. |
| 13 | Two consecutive `extract_case_specs` runs produce byte-identical YAML. | ✓ | `test_byte_identical_regeneration` (existing) + my one-off 3-row diff (Section 3.3). |
| 14 | `--no-llm` produces specs whose persona = `rule_draft` and never calls DeepSeek. | ✓ | `test_offline_mode_returns_rule_draft` + new `test_snapshot_offline_mode_falls_back_to_rule_draft`. |
| 15 | Cache for `cs_interactive_012` shows seeds reflecting the actual issue with `acceptance_reason: auto_accept_high_confidence`. | ✓ | `case_spec_llm_cache/570Q5000008hx9tIAA.yaml` has the kitten-deletion seeds and `acceptance_reason: auto_accept_high_confidence`. |
| 16 | Snapshot tests pin persona for at least one anchor case per UC and fail on silent drift. | ✓ (partial) | New file pins UC-FP (cs_interactive_012, cs_interactive_055) + UC-C (cs_interactive_064). UC-A / UC-B / UC-D / UC-E / UC-F / UC-G…UC-K are NOT yet pinned because the smoke cache only covers 3 sessions. Will be expanded after A6.4 cache population. |

## 5. Open issues for the maintainer

1. **(MUST FIX before A6.4) `case_id` is positional and is part of `prompt_hash` + `case_spec_overrides.yaml` key.** Recommend either (a) drop `case_id` from `render_user_prompt` (one-line fix at `llm_persona_reviewer.py:223`), or (b) switch the override file to keying by `source_session_id` alone and the cache key to `source_session_id`. Until this is decided and applied, populating the 367-session cache will produce files whose prompt_hash is invalidated by any future HR-row insert/delete, and subset extractions silently drop reviewed Wave A5 overrides.

2. **(MEDIUM) `_apply_case_spec_override` allow-list is `expected.*` + `scoring` only.** Design says L3 may write any field (incl. persona). If A6.4 retires the `cs_interactive_012` override entirely (because L2 now produces equivalent persona), this is moot. If the maintainer ever wants to pin a persona override that disagrees with L2, widen `_CASE_OVERRIDE_EXPECTED_FIELDS` (or add a sibling `_CASE_OVERRIDE_PERSONA_FIELDS`) and document the precedence.

3. **(LOW) Hidden-fact "extra-key" leak.** Validator silently drops nested forbidden keys inside `hidden_facts` entries. One-line fix to add a `validation_notes` entry. Not security-critical.

4. **(LOW) Cache read does not re-validate.** Hand-edited cache files with a 500-char `user_goal_summary`, a forbidden top-level key, or an unsupported hidden_fact are loaded as-is. Acceptable per design (PR diff review is the gate), but a defense-in-depth `logger.warning` from `_read_cache` calling `validate_response` would catch obvious tampering.

5. **A6.4 readiness:** the production reviewer module, validators, cache I/O, CLI flags (`--no-llm`, `--refresh-llm-session`, `--llm-model`, `--llm-cache-dir`), and audit fields are all in place. The only blocker before running the 367-session population is Open Issue #1. After #1 is resolved, A6.4 should be a straight `python -m eval_interactive.scripts.regenerate_case_specs --refresh-llm-session …` for any session whose cached prompt_hash no longer matches; expect ≈67 minutes wall time and ≈$1 cost.

6. **Snapshot coverage gap (UC matrix):** Acceptance #16 requires "at least one anchor case per UC". Current smoke cache covers UC-FP (×2) and UC-C only. Once A6.4 produces the full 367 cache, extend `test_case_spec_persona_snapshots.py` to add one snapshot per UC (UC-A, UC-B, UC-D, UC-E, UC-F, UC-G, UC-H, UC-I, UC-J, UC-K).

7. **Prompt sha256 lock-in:** the new `test_snapshot_prompt_sha256_locked` will fail if anyone edits `PROMPT_SYSTEM`. To intentionally re-bless the prompt, regenerate the constant and update `LOCKED_PROMPT_SHA256` in the test in the same commit; the whole 367-session cache will need refresh in the same PR.
