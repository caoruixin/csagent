# Dev Prompt — Sprint 072 / S-Auto-16 (M-Auto-4 sub-sprint 1 of 3)

> Self-contained executable view of `docs/sprint_objective.md` (prompt-artifact-rules §9).
> Paste this into a fresh dev session. You need NO other doc except `AGENTS.md`
> (auto-loaded) + this prompt. Code-anchor paths below are read on demand.

## 1. Role identity

You are the **dev agent for Sprint 072 / S-Auto-16**, the FIRST sub-sprint of
Milestone **M-Auto-4 — Autoloop Fitness Measurement Reliability**.

**One-line goal:** Build the autoloop k-of-n fitness-measurement core (per-case
majority vote over provider-comparable repeated samples) + run the time-boxed §0
fallback diagnostic, and prove (real-LLM) it reduces suite-level variance — but
commit the machinery **INERT (`samples_per_case: 1`)** because the live majority
gate is only sound after the baseline is re-blessed (a LATER sub-sprint). This is
a MEASUREMENT sprint: do not touch the bot's capability (no skill/prompt/routing
edit).

## 2. Read order (minimal)

1. `AGENTS.md` — auto-loaded governance chain (Constitution §1, iteration_governance
   §1.5/§1.7 anti-hardcode, §5 eval acceptance, §7 stanza).
2. **This prompt** — the full contract.
3. On demand only, the code anchors in §10 (read/modify as you implement).

Do NOT read `docs/sprints/*` or `docs/archive/*`.

## 3. THE LOAD-BEARING SCOPE BOUNDARY (read before coding)

`tier_evaluator`'s L0/L1/L3 gates compare **candidate vs baseline**. The baseline
re-bless is **DEFERRED to S-Auto-17**, so this sub-sprint's baseline is still a
single noisy draw. Flipping `samples_per_case>1` in the LIVE loop against a
single-draw baseline is **asymmetric** and could MANUFACTURE spurious regressions
(baseline lucky-pass vs candidate honest-majority-fail).

**Therefore: ship the full k-of-n + aggregation + majority-input machinery, but
commit it INERT — `samples_per_case: 1`, which is byte-identical to today
(proposal §8.7).** The `n>1` path is exercised ONLY in unit tests + the §6.1
variance-measurement run (scratch dir, NO baseline-pointer move). The live flip to
n=3 + the symmetric majority gate are S-Auto-17. **Do NOT commit
`samples_per_case: 3`.**

## 4. Class

- **Layer (primary):** `infra` — autoloop fitness measurement reliability in
  `autoloop/autoloop/scoring/`. No bot-runtime semantic change; no `eval_spec`
  change (that is S-Auto-18).
- **§7 stanza:** included (self-walked in §8). Pure `infra`/measurement → technically
  §7-EXEMPT, but included for rigor because you edit SHA-locked fitness-gate scoring
  code + trigger a per-sub-sprint Codex review.
- **Codex review plan (§4.3):** PER-SUB-SPRINT REQUIRED — you edit 3 of the now-5
  SHA-locked scoring files (`eval_runner.py`, `tier_evaluator.py`, and `gaming.py` to
  extend `_compute_scoring_code_sha` coverage to the NEW `aggregate.py`) → fence-#13
  hard-fenced-surface trigger. **You do NOT dispatch Codex.** It folds into the
  deliver-authored M-Auto-4 milestone-shared close prompt and is a **validation /
  sign-off** pass, NOT an authorship-provenance source. Record the deferral + SHA
  recompute in handoff §11.

## 5. Goal

Make the per-case pass/fail signal CAPABLE of being a majority vote over repeated,
provider-comparable samples, prove variance reduction (real-LLM), and enforce the
primary-only fitness provider policy — WITHOUT flipping the live loop or
re-blessing the baseline or changing the escalation spec.

## 6. Scope (execute in order; proposal §8.6 steps 1–4)

### #1 — §0 fallback diagnostic + primary-only fitness enforcement (time-boxed)

Policy (locked v1): autoloop fitness eval is **primary-provider-only**. Product
runtime keeps its fallback chain; the fitness harness must keep provider/model
COMPARABLE so the vote is over one model population. §0 *enforces + instruments*
the policy — time-boxed instrumentation, NOT a research sprint.

1. **Capture per attempt, per case:** `actual_provider`, `actual_model`,
   `fallback_count`, `request_id` (if returned). Surface as NEW OPTIONAL per-case
   eval-result fields (consumers tolerate absence).
   - **Route (pick the cheapest faithful, ADDITIVE option):** `FallbackLlmClient.java`
     logs `[chat:fallback-engaged]` / `[chat:fallback-skipped-*]` with
     `primary=`/`fallback=` labels (~`:59/:80/:108/:127`). Cheapest first cut =
     parse those server logs keyed to the eval run window. Durable route = thread
     the provider/model label through the response object. **If you thread it, the
     server change is ADDITIVE metadata only — do NOT alter `FallbackLlmClient`'s
     fallback DECISION logic (hard fence).** Record which route + why in handoff.
2. **Primary-only enforcement:** prefer **retrying the primary** at the eval_runner
   attempt level on a transient primary failure. If an attempt is served by the
   fallback (`fallback_count>0` / `actual_model != primary`) → mark
   `provider_mixed`/`non_comparable`, **drop from the vote**. If the primary can't
   serve within the retry cap → mark `infra_error`, also excluded, never pass/fail.
3. **Connect to existing infra-error detection:** `loop.py`
   `_INFRA_ESCALATION_REASONS:776 = {service_degraded, runtime_error_threshold}` +
   `_assess_infra_error:879` already demote infra-degraded runs to
   `decision="error"`. Confirm whether the `faq_miss` escalation jitter is (a)
   genuine backend non-determinism or (b) fallback/infra degradation already on the
   infra-error path. Do NOT double-count: `service_degraded`/`runtime_error_threshold`
   → infra-error drop, not a scored escalation outcome.
4. **Exit:** one-paragraph handoff finding (fallback frequency + co-occurrence with
   jittery escalation cases + whether primary-only retry holds `min_valid_attempts=3`
   within cost). **STOP-and-surface** if fallback is frequent enough that
   primary-only retry can't reach `min_valid_attempts=3` within a sane retry cap.

### #2 — `aggregate.py` (NEW): pure majority + pass_rate helper

- Create `autoloop/autoloop/scoring/aggregate.py`: a **pure** function (no I/O / no
  network / no file reads) — per-attempt records for one case →
  `{majority_passed, pass_rate, valid_attempts, attempts[]}`.
- Majority over **valid attempts only** (exclude `provider_mixed`/`infra_error`).
  `pass_rate = passes / valid_attempts`.
- **Insufficient-valid rule** (no tie heuristic): `min_valid_attempts=3`. If
  `valid_attempts < 3` → `non_comparable`/`infra_error`, does NOT gate. `1/2`,
  `1/1`, any sub-threshold = neither improvement nor stable regression.
- Unit-test (`autoloop/tests/test_aggregate.py`): majority up/down, provider_mixed
  exclusion, infra_error exclusion, sub-threshold → non_comparable, pass_rate. Pure
  (no LLM).
- **Drift-guard coverage (RESOLVED — not an OQ):** `aggregate.py` produces the
  `majority_passed` used for the fitness verdict, so it MUST be inside the
  `scoring_code_baseline_sha` coverage. Edit `gaming.py` `_compute_scoring_code_sha()`
  to hash a **5-file** set (`tier_evaluator.py` + `eval_runner.py` +
  `baseline_loader.py` + `gaming.py` + `aggregate.py`) and update the `config.yaml`
  comment (`~:129-132`) "four files" → five. Leaving it out is a scoring-code drift
  hole. (`gaming.py` is in the set → captured by the close-time recompute in #5.)

### #3 — `eval_runner` n-loop + per-attempt persistence + majority aggregation

- Wrap `run_v1_fitness_suite` (`eval_runner.py:228`) to run `n =
  fitness.samples_per_case` times. `run_suite` (`:87`) stays the single-run
  primitive. `SuiteRunResult` (`:72`) gains a per-attempt list.
- **Per-attempt record:** `case_passed`, `failure_reason`/`failure_tags`,
  `escalation_reason`, the §1 provider fields, and a `valid` flag (`false` if
  `provider_mixed`/`infra_error`).
- **Aggregate per case** via `aggregate.py`: persist per-attempt rows + `pass_rate`
  + `majority_passed`. Invalid attempts excluded from numerator AND denominator.
- **Retry:** if `valid_attempts < min_valid_attempts`, retry missing attempts up to
  `aggregation.attempt_retry_cap` (=2); still short → `infra_error`/`non_comparable`.
- **Cost guard:** honor `eval_suite_timeout_seconds` (`config.yaml:122`) **per
  attempt**, not per triple. Keep `bad_cases parallel:1` (`config.yaml:83`).
- **INERT default:** committed `fitness.samples_per_case = 1`. At n=1 the loop runs
  once, `majority_passed` = the single attempt, byte-identical to today (§8.7).
- Tests (`autoloop/tests/test_eval_runner.py`, mockable): loop count, provider_mixed
  drop, infra_error drop, retry-to-cap, insufficient → non_comparable, persistence
  shape.

### #4 — `tier_evaluator` reads the aggregated majority signal

- `_evaluate_layer0` (~`:235`), `_evaluate_layer1` (`:385`; bad no-drop `:400-407`;
  anchor max-drop `:417-431`), `_evaluate_layer3` (`:521`), `_evaluate_layer2`
  (`:445`) read the **aggregated** `majority_passed` per-case signal, not a single
  draw.
- **L0 Tier-0 delta + stable-reproduction:** a Tier-0 violation counts as a candidate
  regression only if it reproduces in the **majority** of candidate samples AND was
  absent from the baseline majority. A single noisy Tier-0 flip no longer discards.
  **Safety-floor strictness UNCHANGED** — no family removed, no threshold lowered.
- L1 bad_cases strict no-drop (`:400-407`) + anchor max-drop (`:417-431`) compare
  candidate-majority vs the baseline snapshot.
- **n=1 invariance:** at committed `samples_per_case=1`, `majority_passed` = the
  single attempt's pass and the baseline is the existing single-draw snapshot →
  L0/L1/L2/L3 behaviour byte-identical to today. Majority logic only diverges at n>1.
- Tests (`autoloop/tests/test_tier_evaluator.py`, mockable): majority-based L0/L1/L3;
  stable (majority) gates / flaky (minority) does NOT; n=1 reproduces the pre-sprint
  verdict on a fixture.

### #5 — `config.yaml` knobs (committed inert) + SHA recompute

Add to `autoloop/config.yaml` `fitness`:
```yaml
samples_per_case: 1                 # COMMITTED INERT (=today). S-Auto-17 flips to 3 WITH the re-bless.
aggregation:
  method: majority
  min_valid_attempts: 3
  attempt_retry_cap: 2
provider_policy:
  fitness_provider: primary_only
  prefer_retry_primary: true
  on_fallback: drop_attempt_non_comparable
  record_per_attempt: [actual_provider, actual_model, fallback_count, request_id]
```
- Do NOT add `baseline_aggregation` / do NOT move `baseline_dir`. Leave
  `anchor_outcome_max_drop_cases:0`, `improvement_min_cases:1`,
  `shadow_max_drop_pct:3.0` unchanged.
- **Recompute `scoring_code_baseline_sha` (`config.yaml:171`)** at close — editing
  `eval_runner.py` + `tier_evaluator.py` + `gaming.py` (the latter to widen the set
  per #2) trips the hash, now over the **5-file** set (`tier_evaluator.py` +
  `eval_runner.py` + `baseline_loader.py` + `gaming.py` + `aggregate.py`). Reproduce
  via the one-liner in the `config.yaml` comment (~`:149`):
  `uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"`
  at this HEAD; record old→new in handoff. Expect `scoring_code_drift` /
  `suspect_baseline_manipulation` to observe the change (intended fence-#13 behaviour).

### #6 — Acceptance evidence (real-LLM; §5.7)

- **§6.1 variance drop (PRIMARY evidence):** clean tree at this commit, run each
  suite n≥5 via the new n-loop into a SCRATCH results dir (NOT the baseline
  pointer). Report before/after suite-level score std-dev: single-draw vs
  majority. Real-LLM (mocks may NOT be primary evidence — §5.7). Needs backend up +
  creds in `autoloop/.env.local` + a clean committed tree.
- **Measurement-only run isolation (acceptance invariant):** the n≥5 / n=3
  variance run is a MEASUREMENT artifact only — it MUST NOT (a) participate in any
  `keep`/`discard` decision, (b) update/overwrite the baseline (scratch dir only;
  `baseline_dir` untouched), or (c) trigger or feed an overnight loop gate. The
  committed loop stays at `samples_per_case=1`.
- **§6.3 flaky-vs-stable:** show the harness labels minority-fail `flaky` (no gate)
  vs majority-fail `stable` (gates).
- **§6.7 measurement integrity:** primary-only; fallback attempts dropped
  (`provider_mixed`); `<min_valid_attempts=3` → `infra_error`/`non_comparable`,
  never gates.
- Unit suites green: `test_aggregate.py`, `test_eval_runner.py`,
  `test_tier_evaluator.py`.

## 7. Hard fences / STOP conditions

- **Do NOT commit `samples_per_case: 3`** — committed value is `1` (inert).
- **Do NOT re-bless the baseline / move `baseline_dir` / overwrite the retained
  single-run baseline.** §6.1 variance run = scratch dir only.
- **Do NOT edit any skill YAML soft field, prompt, or routing semantic.** No
  CaseSpec / `eval_spec` edit (§5 escalation tiers = S-Auto-18).
- **Do NOT alter `FallbackLlmClient`'s fallback DECISION logic.** §0 capture is
  ADDITIVE metadata only.
- **Tier-0 families: no family removed, no threshold lowered.** Only noise-driven
  single-flip filtering (majority / stable-reproduction).
- **`bad_cases` strict no-drop preserved**; no drop-budget.
- **`aggregate.py` MUST join the `scoring_code_baseline_sha` coverage** (5-file set,
  via the `gaming.py` edit in #2) — it feeds the fitness verdict, so leaving it
  outside the drift guard is a hole. RESOLVED decision, not an OQ.
- **Scope hold (deferred — do NOT pull forward):** §5
  `acceptable_escalation_families` → S-Auto-18; baseline re-bless + live
  `samples_per_case=3` flip → S-Auto-17.
- **STOP-and-surface** (handoff §7, do not improvise) if: §0 shows primary-only
  retry can't reach `min_valid_attempts=3` within a sane retry cap; OR
  `tier_evaluator` majority-awareness can't keep n=1 byte-identical to today; OR
  provider metadata can't be captured additively without changing fallback decision
  logic.
- **§5.7 eval evidence gate:** mocked-LLM = wiring only; the §6.1 variance metric
  MUST be real-LLM.

## 8. §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (autoloop fitness measurement reliability —
sampling / aggregation / per-attempt persistence in `autoloop/autoloop/scoring/` +
additive provider-metadata capture). No `eval_spec` change this sub-sprint.

**Tier-0 invariant:** Adds no Tier-0 invariant. *Preserves* existing Tier-0 safety
families (PII, escalation_compliance, critical_policy, phase_transition) at current
strictness; the L0 change is a measurement decision (stable-reproduction across
k-of-n), not a policy relaxation.

**Semantic hardcode:** None. No skill/prompt soft field edited; no keyword/regex/enum
routing added. Majority aggregation is a pure cardinality computation over existing
pass/fail signals; the primary-only policy keys on `actual_model`/`fallback_count`,
not content.

**Generalization coverage:** Measurement sub-sprint — evidence is the
repeated-baseline variance metric (§6.1) + flaky-vs-stable distinguishability (§6.3),
not case-family counts. The shadow gate (L4) stays active + firewalled; only the
aggregate majority crosses to the loop; its role is unchanged.

## 9. Test / eval requirements

- **Java:** no new regression vs `1213 / 1 / 0 / 2` (sole failure =
  `SystemPromptUserRequestedTiebreakerTest`, OQ-S41.5). If §0 threads metadata
  through the response object → add/extend Java tests for the additive field +
  rebuild (`mvn`; restart backend before any eyeball/eval — no hot reload).
- **autoloop pytest:** `test_aggregate.py` (new) + `test_eval_runner.py` +
  `test_tier_evaluator.py` green; no regression vs `276`.
- **eval_interactive pytest:** no regression vs `503 / 0` under `uv run`.
- **Real-LLM §6.1 variance run:** required primary acceptance evidence (§5.7).
- **scoring SHA:** recomputed + recorded; `scoring_code_drift` explained.

## 10. Code anchors (verified 2026-06-03; read/modify on demand)

| Anchor | Path | Use |
|---|---|---|
| `run_v1_fitness_suite:228`, `run_suite:87`, `SuiteRunResult:72` | `autoloop/autoloop/scoring/eval_runner.py` | n-loop + per-attempt persistence (#3) |
| `_evaluate_layer0:235`, `_layer1:385` (bad `:400-407`, anchor `:417-431`), `_layer2:445`, `_layer3:521` | `autoloop/autoloop/scoring/tier_evaluator.py` | majority input + L0 stable-reproduction (#4) |
| `BaselineSnapshot:66`, `load:80` | `autoloop/autoloop/scoring/baseline_loader.py` | read-only this sub-sprint (re-bless is S-Auto-17) |
| `_INFRA_ESCALATION_REASONS:776`, `_assess_infra_error:879` | `autoloop/autoloop/loop.py` | infra-error connect (#1.3) |
| fallback logs ~`:59/:80/:108/:127` | `server/.../service/llm/FallbackLlmClient.java` | provider-metadata capture (#1.1); DECISION logic is a hard fence |
| `buildRequestBody:363` | `server/.../service/llm/OpenAiCompatibleLlmClient.java` | context only (no seed/top_p — confirms no source headroom) |
| `temperature:19` (OQ-S65.8 comment) | `server/.../model/LlmRequest.java` | context only (temp already 0.0) |
| `:83/:96/:100/:104/:118/:122/:171` | `autoloop/config.yaml` | knobs (#5); SHA at `:171`, reproduce one-liner ~`:149` |
| `aggregate.py` (NEW) | `autoloop/autoloop/scoring/aggregate.py` | create (#2); MUST join the 5-file SHA set |
| `_compute_scoring_code_sha()` | `autoloop/autoloop/scoring/gaming.py` | widen hash set to include `aggregate.py` (#2); SHA-locked |
| `bad_cases/cs011,cs014,cs029` | `eval_interactive/case_specs/bad_cases/` | jittery-case context (do NOT edit) |

## 11. Handoff requirements (author `docs/sprints/sprint-072-handoff.md`)

Standard sub-sprint handoff. MUST include: §0 fallback-frequency finding
(one paragraph); the §6.1 variance before/after std-dev table (real-LLM);
confirmation the measurement-only n>1 run did not gate / re-bless / feed an
overnight; old→new `scoring_code_baseline_sha` over the 5-file set (now including
`aggregate.py`); confirmation `samples_per_case` committed =1 (inert) + n=1
byte-identical evidence; §11 Codex deferral note (per-sub-sprint folds into
M-Auto-4 milestone-shared close) + fence-#13 authorization; §12 self-classification.

## 12. Commit discipline

Stage only authorized-scope files (NOT `git add -A`). One commit per §8.6 step where
practical (diagnostic / aggregate.py / eval_runner / tier_evaluator / config+SHA) so
revert is per-step (§8.7). Run the loop / any eval ONLY on a clean committed tree
(`project_autoloop_dirty_index_hazard`). Deliver-agent-owned files are bundled by the
human at close.

## 13. Self-check checklist (complete before claiming done)

- [ ] §0 diagnostic ran; fallback finding written; primary-only enforced;
      STOP-and-surface evaluated.
- [ ] `aggregate.py` pure (no I/O) + unit-tested AND added to the 5-file
      `scoring_code_baseline_sha` coverage via `gaming.py` (`config.yaml` "four
      files" comment updated to five).
- [ ] `eval_runner` n-loop + per-attempt persistence + retry + drop rules;
      `run_suite:87` primitive unchanged; tests green.
- [ ] `tier_evaluator` reads aggregated majority; L0 stable-reproduction; n=1
      byte-identical (fixture-proven).
- [ ] `config.yaml`: `samples_per_case: 1` (NOT 3); aggregation + provider_policy
      added; `baseline_dir` UNCHANGED.
- [ ] `scoring_code_baseline_sha` recomputed + recorded over the 5-file set; drift
      explained.
- [ ] §6.1 variance drop real-LLM (scratch dir); §6.3 flaky-vs-stable; §6.7
      measurement integrity; the n>1 measurement run did NOT gate / re-bless / feed
      an overnight.
- [ ] Java / autoloop / eval_interactive baselines no regression.
- [ ] No skill/prompt/routing/CaseSpec edit; no fallback-decision change; no
      baseline re-bless.
- [ ] Handoff §0 / §6.1 / SHA / §11 / §12 written.
