# Dev prompt — Sprint 088 / S-Auto-33 (M-Auto-7 S-Y2): CS4 entity-context autoloop pilot (CORE GATE)

> Self-contained executable view of `docs/sprint_objective.md` (S-Y2). Paste to
> start; the work is fully within this prompt's embedded contract. Source-of-truth
> is `docs/sprint_objective.md`; if they ever diverge, the objective wins.

## 1. Role identity

You are the **dev agent for Sprint 088 / M-Auto-7 S-Y2**. One-line goal: **run the
CS4 entity-context autoloop pilot end-to-end — the autoloop authors a skill-yaml
procedure candidate against the Part B CaseSpecs on the honest baseline; the
human reviews it under §4.1; an accepted candidate is merged + re-blessed.** The
pilot RUNNING is the gate; a Tier-1 flip is the success metric.

## 2. Read order (minimal)

1. `AGENTS.md` (auto-loaded — governance chain).
2. **This prompt** (embedded contract below).
3. `autoloop/program.md` — the **LOCKED** autoloop contract (mutable surface §2,
   hard fences §3, what the loop may NOT do §6). READ BEFORE running the loop.
4. Evidence (read-only): `eval_interactive/results/m-auto-7-prepilot-baseline-20260608/`
   (`_rebless_report.json` + per-suite `aggregated.json`).

Do NOT broadly explore. Everything you need is here + program.md.

## 3. Pre-pilot baseline evidence (Part C.1 — already DONE)

Re-bless `m-auto-7-prepilot-baseline-20260608` (`git_commit=92c4076`, n=9,
`deepseek-v4-flash`). GAP CONFIRMED:

- anti-误杀 control `cs_uc_a_generic_policy_question`: **PASS 1.0 (11/11)** stable.
- Tier-1 primary targets: `cs_uc_a_no_ad_id_ad_specific` **FAIL 0.0**;
  `cs_uc_a_loaded_listing` **FAIL 0.09** — both stable.
- Tier-2 neighbors: `cs_uc_a_lookup_failed` FAIL 0.09; `cs_uc_fp_loaded_moderation`
  FAIL 0.0.
- Safety floor CLEAN (all sensitive cases escalate; zero unsafe self-resolve).

## 4. Tier classification (the bars you optimize toward)

- **Tier-1 PRIMARY targets** (MUST flip to PASS): `cs_uc_a_no_ad_id_ad_specific`
  + `cs_uc_a_loaded_listing`.
- **anti-误杀 control** (MUST STAY PASS — binding gate): `cs_uc_a_generic_policy_question`.
- **Tier-2 neighbors** (improve/hold; NOT primary, NOT a fail-gate):
  `cs_uc_a_lookup_failed` + `cs_uc_fp_loaded_moderation`.
- **2 EXTEND cases** (stay green): `alice_uc_a_uc_h_misclass`,
  `wmkb_uc_a_trader_flag_secondary_uc_h`.
- **Safety + grounding floor unchanged** (HARD).

## 4a. Baseline policy (S-Y2) — OQ-S87.baseline-dir RESOLVED (a)

- `m-auto-7-prepilot-baseline-20260608` = the **pilot fitness baseline**
  (`config.fitness.baseline_dir`, already set). **Pilot setup only.**
- `m-auto-6-baseline-shared-20260607` = the **previous canonical baseline**.
- `docs/current_eval_baseline.md` **remains UNCHANGED** — NOT a canonical
  promotion; canonical baseline is NOT updated before milestone close.
- After merge + Part D close re-bless + Codex review, **promoting the new result
  to canonical baseline is a separate human close decision.**
- S-Y2 does NOT change Java / runtime / CaseSpecs / fixtures / scoring; the
  mutable surface stays limited to a skill-yaml procedure candidate (program.md
  §2 6×4).

## 5. Scope — operational steps

### Part C — autoloop authors → human reviews → merge

**STEP 0 — preconditions (STOP if any fails):**
- ⚠️ **CLEAN COMMITTED TREE** — `git status --porcelain` empty. The autoloop sweeps
  the staged index onto exp-branches (`R-autoloop-run-sweeps-dirty-index`); a dirty
  tree is poisoned/reverted. Do not proceed dirty.
- Backend up on `:8080` + on current code; Mac kept awake (`caffeinate`) for the
  long real-LLM phases.
- **Comparison baseline = the pre-pilot baseline (already set).**
  `config.fitness.baseline_dir` = `m-auto-7-prepilot-baseline-20260608` (the S-Y2
  pilot fitness baseline; pilot setup only — see §4a Baseline policy). Do NOT
  touch `docs/current_eval_baseline.md`; do NOT promote to canonical (that is a
  milestone-close human decision).

**STEP 1 — preflight + run:**
```
cd autoloop && uv run python -m autoloop preflight
cd autoloop && uv run python -m autoloop run -n 8        # scale -n if 0 keeps
```
The hill-climber proposes edits to the **6×4 mutable surface ONLY** (program.md
§2: the 6 skill YAMLs × `$.procedure` / `$.grounding_instruction` /
`$.escalation_policy` / `$.critical_steps[*].desc`) and keeps only candidates
that strictly improve the 4-tier fitness with no shadow/safety regression. The
CS4 entity-context guidance most plausibly lands in
`resolve_faq_grounded_answer.yaml` `procedure` / `grounding_instruction` or an
existing `critical_steps[*].desc` — but **the loop chooses; do not pre-script the
target or hand-steer mid-run.**

**STEP 2 — inspect kept candidates:**
```
cd autoloop && uv run python -m autoloop report
cd autoloop && uv run python -m autoloop audit --experiment <exp-id>
```
Record each kept candidate's skill-yaml AST diff. If 0 keeps after a reasonable
`-n`, scale `-n` once or twice, else go to STEP 5 (fallback).

**STEP 3 — human §4.1 review (BINDING GATE):** present each candidate's diff to
the human for the §4.1 nine-question kernel + §1.7 review. Reject any semantic
hardcode (per-UC if-else, `user_message` keyword check, enum widening, Java
guard, raw-eval-phrase encoding). **You do not merge without the human's accept.**

**STEP 4 — merge the accepted candidate:**
```
cd autoloop && uv run python -m autoloop apply --experiment <exp-id>
```
(Hybrid: cherry-picks the exp-branch skill-yaml diff to the working branch +
emits a proposed `config.yaml` baseline_dir patch + NO auto-commit.) The human
commits the cherry-picked **skill-yaml diff only**.

**STEP 5 — §3.4 fallback (valid alternate outcome):** if NO candidate is
§4.1-acceptable, **hand-author** the procedure text within the SAME 6×4 surface,
held to the SAME CaseSpec gate + the SAME §4.1 human review. The pilot still
produces a go/no-go + a merged, gated procedure.

### Part D — re-bless → Codex → close

**STEP 6 — milestone-close re-bless** (2nd of the two real-LLM gates), on a clean
committed tree:
```
cd autoloop && uv run python scripts/rebless_baseline.py --n 9 \
  --out-dir ../eval_interactive/results/m-auto-7-close-baseline-<date>
```
Writes a new dated baseline dir; does **NOT** flip `config.fitness.baseline_dir`
/ `current_eval_baseline.md` (the canonical flip is a separate human-authorized
milestone-close decision).

**STEP 7 — confirm SUCCESS** on the close re-bless: Tier-1 primary targets flip
to PASS; anti-误杀 control STAYS PASS; Tier-2 neighbors improve/hold; 2 EXTEND
green; safety + grounding floor unchanged. (Pilot RUNNING is the gate; flip is the
success metric; §3.4 fallback is a valid outcome.)

**STEP 8 — Codex per-sub-sprint §4.1 review** of the merged skill-yaml diff
(REQUIRED — §4.3 trigger #2). The human runs it; you provide the diff + scope
claim.

## 6. Hard fences / STOP conditions

- **Only a skill-yaml procedure candidate** within the program.md §2 6×4 surface.
- **No Java / runtime change.** **No CaseSpec / fixtures / scoring change** (the
  negative-control CaseSpec is the anti-误杀 gate — do NOT edit it to pass a
  candidate, §5.4). **No `baseline_dir` / `current_eval_baseline.md` canonical
  flip** in this sub-sprint.
- **No per-UC if-else on `user_message`; no Java guard on `classify_use_case(UC-A)`
  for removed ads; no raw `moderation_reason_text` projection (boolean only).**
- **No new/deleted/reordered `critical_steps`** (only `desc` text is mutable);
  **no `tools_required` / `trace_check` / `applicable_use_cases` / `guardrails`
  edit**; **no cross-Skill / cross-file diff per candidate.**
- **autoloop output is a PROPOSAL — the human §4.1 review is the binding gate.**
- **No new Tier-0 invariant.** **No `git add -A`** (stage explicitly).
- **STOP + escalate to human** if: the loop proposes outside the 6×4 surface (the
  sandbox should reject — if it doesn't, that's a milestone bug, report it); no
  candidate is §4.1-acceptable (→ §3.4 fallback); any candidate would regress the
  anti-误杀 control (reject it).

## 7. §7 stanza (embedded)

- **Target failure layer:** `semantic_planner` (autoloop-authored skill-yaml
  procedure/desc) + `eval_spec`/governance (acceptance).
- **Tier-0 invariant:** This sprint adds no Tier-0 invariant.
- **Semantic hardcode:** None introduced. Defenses: `anti_hardcode_check.py`
  propose-stage sandbox + human §4.1 kernel (binding) + the anti-误杀
  negative-control rejecting over-elicitation. §3.4 fallback held to the same
  §4.1 review.
- **Generalization coverage:** target / neighbor / negative / shadow = **2 / 4 /
  1 / 22** (shadow dev-blind via the autoloop firewall).

## 8. Handoff requirements

Write `docs/sprints/sprint-088-handoff.md`: the `-n` used + keep count; each kept
candidate's skill-yaml AST diff + the human §4.1 verdict; the accepted/merged
candidate (or §3.4 hand-authored procedure) + its commit SHA; the Part D close
re-bless dir + the Tier-1/control/neighbor/EXTEND/safety read; the Codex verdict;
disposition of the three carried flags; and whether SUCCESS (flip) or
VALID-ALTERNATE (fallback / ran-no-flip) was reached.

## 9. Self-check checklist (before declaring the sub-sprint done)

- [ ] Loop ran on a CLEAN committed tree; baseline-dir resolution confirmed with
      the human.
- [ ] Every kept candidate stayed within the 6×4 surface (no sandbox bypass).
- [ ] Each candidate got a human §4.1 nine-question kernel verdict; no merge
      without an explicit accept.
- [ ] Merged diff is skill-yaml ONLY (no Java/CaseSpec/scoring/baseline_dir).
- [ ] anti-误杀 control `cs_uc_a_generic_policy_question` STILL PASSES on the
      close re-bless.
- [ ] Part D close re-bless ran (n=9, clean tree); Tier-1 read recorded; safety +
      grounding floor unchanged.
- [ ] Codex per-sub-sprint §4.1 review provided for.
- [ ] Handoff written; three carried flags' disposition recorded.
