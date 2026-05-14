Paste the content below this line into a fresh Codex session after the dev's commits land. No PR will be opened; review the commit range.

---

# Sprint 28 review prompt — Per-case trace dump for smoke harness

You are the review agent (Codex). Single track, single layer (`infra` / eval-harness). The dev's commit lands on branch `design-v1-without-human-review`.

## 1. Loader stanza

Read in order before any verdict:

1. `AGENTS.md`.
2. `docs/current/doc_governance.md`.
3. `docs/current/iteration_governance.md` §1 / §3 / §4.1 / §4.2 / §5 / §7.
4. `docs/sprint_objective.md` — Sprint 28 scope.
5. `docs/sprints/sprint-028-handoff.md` — the dev's handoff.
6. Precedents: `docs/sprints/sprint-025-handoff.md` (the Option B writer-side enrichment precedent — same shape as Sprint 28); `docs/sprints/sprint-027-handoff.md` §6 + §11 Q2 (the consumer use case).
7. `compact/sprint-028-dev-prompt.md` — what was authorized vs what was added.

## 2. §4.1 kernel

Run §4.1 verbatim as loaded. Sprint 28 ships `infra` eval-harness enrichment with no semantic surface touched and no Java code edited under Option B → qualifies for the §4.1 pure-infra exemption IF the dev shipped B. Return `approve` with a one-line exemption note.

If the dev shipped Option A (Java instrumentation), Sprint 28 is NOT exempt — run all 9 questions, focusing Q2 (Tier-0 justification), Q3 (soft signal achievable), Q5 (semantic ownership shift), Q7 (tool / capability / PII / grounding preserved).

Q8 coverage: accept Sprint 28 §8 — target=14-case rerun, neighbor=full suites, negative=`elapsed_ms` no regression, shadow=0 (deferred to G2 per Sprint 25 precedent).

## 3. Sprint-28-specific deviations

### 3.1 Bundled enrichment expectations

The bundle is one writer-side enrichment commit:

1. `eval_interactive/eval_interactive/batch/executor.py` — `_build_case_result` extended with one additive list field carrying per-turn trace from `trace_data.turns[]`.
2. New `eval_interactive/tests/test_<name>.py` — regression file mirroring Sprint 25's `test_executor_llm_calls_enrichment.py` shape; ≥ 6 tests covering populated / empty / backwards-compat / serializability paths.

A bundled writer-side enrichment in this shape is **not scope drift** (per Sprint 25 precedent). Do not flag.

### 3.2 Hard-fence violations (BLOCKING)

`docs/sprint_objective.md` §5.3 lists 13 hard fences (read it). Any violation is BLOCKING. Specifically check:

1. Semantic surface untouched (no prompt edits, no Java semantic decision moves).
2. No Sprint 23/24/25/26/27-landed code edits — `BotSession.consecutiveDeadlineCount`, Sprint 25's `executor.py llm_calls`, `agent_client.py get_llm_calls`, `LlmSyntheticBaselineTest.java`, Sprint 23 `system_prompt.txt`, `AlreadyCalledPromptConsumptionTest.java`.
3. No eval-spec / Sprint 20 case-family / foundational / governance / sprint archive / Tier-0 / deadline-or-model-config edits.
4. Opportunistic field expansion — only the four R-item-named fields (`tool_calls`, `phase_plan`, `projection`, `LlmCallEvents`) ship. Any 5th / 6th additive field is scope drift unless handoff defers it to a follow-on R-item AND it is not actually shipped.
5. No mocked-LLM as primary evidence (Sprint 23 lesson).

### 3.3 Schema-backwards-compat (BLOCKING)

A regression on existing `results.json` is BLOCKING. Verify: Sprint 25's `llm_calls[]` field present on every `case_results[]` in same shape (`jq '.case_results[0].llm_calls[0] | keys'` returns the 13 Sprint-25 keys); `transcript` + composite/judge/L1/L2/L3 fields + `case_id`/`primary_uc`/`expected_outcome`/`status` all present same shape; new fields additive only.

### 3.4 Reproducibility (BLOCKING)

Every quantitative claim in the handoff cites source path + extraction recipe + literal output. Cross-check ≥1 number per cited table: re-run the recipe against the named `results.json` and confirm output matches. Per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

### 3.5 Premise re-verification (NON-BLOCKING)

Handoff §1.6 + §3 should re-state each of `docs/sprint_objective.md` §2's six premise items with the session-start read. If skipped but code is correct, flag non-blocking.

## 4. Packaging rollforward

The commit may bundle deliver-agent files (`docs/sprint_objective.md`, `compact/sprint-028-*-prompt.md`, `compact/sprint-deliver-orchestrator.md`) + pre-existing unrelated mods (`csagent_system_design_review.md`, `system_prompt.txt`, `csagent-solution-_20260514.md`).

If the only out-of-scope content is path-based packaging artefacts AND substantive findings close per your own evidence, this is **A-with-packaging-note** territory — `out_of_scope_review` with `blocking_count: 1` on path grounds is acceptable. Classify and report; do not block substance. Per `feedback_out_of_scope_review_packaging_rollforward.md`. Content-based blockers → `fix_required` instead.

## 5. §4.2 sprint-close header

At the top of `docs/codex-findings.md`:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

Per-finding write-ups in the body referenced by ID. Mirror `docs/sprints/sprint-025-fix-codex-review.md` shape.

If the dev shipped a docs-only outcome (premise check failed; handoff is investigation-only, no code change), Sprint 28 qualifies for the §4.1 exemption per Sprint 26 / 27 precedent (`feedback_close_with_codex_skipped_docs_only_outcome.md`). Return one line: "Docs-only outcome — §4.1 exemption applies; verdict `approve (exemption: docs-only investigation, no semantic surface)`. Recommend human skip the formal review per Sprint 26/27 precedent."

## 6. Deferral rule

Valid-but-out-of-scope concerns (richer `/trace` response, candidate 5th field, broader test coverage) go to `docs/action_bank.md` as deferred items, NOT blocker findings. Do not edit `docs/action_bank.md` yourself — deliver-agent close-turn action. If handoff §7 already names the concern, acknowledge; do not list as a finding.

## 7. Final reminders

- Read the commit-range diff start-to-end; verify the bundle.
- One verdict, one header. Substance over ceremony.
- More than 5 findings on `infra` writer-side enrichment is over-review.
- §1.7 forbidden list is canonical for what Sprint 28 must NOT contain. Confirm before `pass`.
