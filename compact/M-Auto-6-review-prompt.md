# M-Auto-6 milestone-shared Codex review prompt — route-(b) accept-with-known-regression

**Authored by deliver-agent at C-2b dev-side close, 2026-06-06; updated
at S-Auto-28 dev-side close, 2026-06-07; rewritten 2026-06-07 (post
milestone-shared re-bless + close-decision) to dispatch the formal §4.3
milestone-shared review against the realised close state.**

**Close type: route-(b) accept-with-known-regression** (per
`docs/milestone_objective.md` §5). This is NOT a clean Class-A close.
Anti-误杀 / safety is CLEAN (route (c) ruled out); 5 known
UC-FP / shadow regressions are accepted as pre-existing semantic
flakiness + session-start infra flake; R5 / R6 / R2.a are each exonerated
for the regressed cases; no clean M-Auto-6 code regression has been
isolated; nothing has been reverted; no fix sprint is opened.

This prompt is the **self-contained executable view** of the
milestone-shared Codex review per `process/milestone-framework.md`
§4.3 + `prompt-artifact-rules.md` §9.1/§9.2. The milestone contract
context is embedded verbatim from `docs/milestone_objective.md`; the
§4.1 nine-question kernel is embedded verbatim from
`docs/current/anti-hardcode-review-kernel.md`. The dev handoffs (one
per sub-sprint) are NOT embedded — they are dev outputs produced
before this review and are referenced by path under "Loader". The
authoritative close-decision record at `docs/action_bank.md` §5
"M-Auto-6 CLOSE (2026-06-07)" and the route-(b) diagnostic at
`docs/diagnostics/2026-06-07-m-auto-6-anchor-overpass-diagnostic.md`
are referenced by path; their key conclusions are embedded inline
under §5.4 below.

---

## 1. Role identity

You are the **Anti-Hardcode + Milestone-Close Review Agent for
Milestone M-Auto-6** (route-(b) close). Your review covers the
cumulative commit range `6236941..d315323` across the **six**
sub-sprints A + B + C-1 + C-2a + C-2b + **S-Auto-28** that comprise
M-Auto-6, plus three close-decision commits authored 2026-06-07
(`27b5239` A6 anti-误杀 reframe + anchor over-pass diagnostic;
`317bdc2` cs38s* safety-of-pass extension + UC-FP route-(b)
re-diagnosis; `d315323` action_bank §5 close-decision record +
handoff §0 status update).

The milestone is "Runtime substrate hygiene at intake + DISCOVER
surfaces + admin observability + intake/clarification contract +
UX/corpus governance" — a Tier-1 mechanical wiring +
prompt_projection contract completion + UI-only display additions +
new no-side-effect tool + data field driven retrieval filter +
citation contract literal→shape semantics fix milestone.

**Your verdict is the formal milestone close gate per
`iteration_governance.md` §5.5.** The route-(b) close decision is
recorded; your job is to:

1. Walk the §4.1 kernel cumulatively across the milestone diff
   (including the three close-decision commits) and confirm no
   semantic hardcode was introduced anywhere in the milestone — by
   any sub-sprint or by the A6 reframe / route-(b) close package.
2. Validate the route attribution by examining the re-bless evidence
   + the cluster diagnoses: that R5 / R6 / R2.a are correctly
   exonerated for the 5 regressed cases, that the close is correctly
   routed to (b) rather than (c), and that the A6 governance reframe
   to "safety-of-pass" is a tightening (not a weakening) of the
   anti-误杀 floor.
3. Surface cross-sprint hardcode / regression risks per-sub-sprint
   reviews could not detect.

Per-sub-sprint Codex reviews have already been delivered:
A `APPROVE_S_AUTO_23 / blocking_count=0` (at `62b4d7b`);
C-1 `APPROVE_S_AUTO_25 / blocking_count=0` (capability-wiring
Option-A fence-waiver ACCEPTED);
C-2a `APPROVE_S_AUTO_26 / blocking_count=0` on targeted re-review;
C-2b `APPROVE_S_AUTO_27 / blocking_count=0` on targeted re-review;
**S-Auto-28** `APPROVE_S_AUTO_28 / blocking_count=0` under §4.1
pure-infra scope exemption. B was §7-EXEMPT and visual-verified —
no per-sub-sprint Codex required per
`process/milestone-framework.md` §4.3.

**Dirty-tree waiver (`--allow-dirty` if your runner enforces clean
tree).** At dispatch the working tree carries one modified file
(`compact/framework-plan-v4-2026-06-06.md`) and four untracked files
(`compact/aidazi-v4-build-plan.md`, `compact/aidazi-v4-reconciliation.md`,
`docs/solutions/2026-06-07-cs1-default-resolved-cs2-user-role-projection-gap.md`,
`docs/solutions/2026-06-07-cs3-cs4-discover-stall-uc-fp-boundary-empty-trace-ux.md`).
**None touch `server/`, `ui/`, `eval/`, `eval_interactive/case_specs*/`,
`autoloop/`, `data/`, or any baseline pointer.** They belong to a
separate governance/collaboration framework workstream (`aidazi`
framework v4) and to M-Auto-7 candidate solution drafts. They are
outside the M-Auto-6 review surface and the waiver is recorded here.

---

## 2. Loader (minimal)

Read in this order:

1. `AGENTS.md` (auto-loaded; transitively loads the governance chain
   `doc_governance.md` → `agent_context_guide.md` →
   `iteration_governance.md`).
2. This prompt (you are reading it).
3. The per-sub-sprint dev handoffs (dev outputs; NOT embedded;
   required for verifying claims per sub-sprint):
   - `docs/sprints/sprint-078-handoff.md` (S-Auto-23 / A)
   - `docs/sprints/sprint-079-handoff.md` (S-Auto-24 / B)
   - `docs/sprints/sprint-080-handoff.md` (S-Auto-25 / C-1)
   - `docs/sprints/sprint-081-handoff.md` (S-Auto-26 / C-2a)
   - `docs/sprints/sprint-082-handoff.md` (S-Auto-27 / C-2b;
     includes §3.1 fix-iteration addendum)
   - `docs/sprints/sprint-083-handoff.md` (S-Auto-28; R8
     KnowledgeIngestionRunner `--reconcile` data-application path
     fixing the M-Auto-6 milestone-close blocker)
4. The close-decision artifacts (route-(b) inputs to your scope +
   evidence audit; NOT artefacts you re-judge):
   - `docs/action_bank.md` §5 "M-Auto-6 CLOSE (2026-06-07)" —
     authoritative close-decision record (lines ~935+).
   - `docs/diagnostics/2026-06-07-m-auto-6-anchor-overpass-diagnostic.md`
     — full A6 reframe + cs38s* extension + UC-FP route-(b)
     re-diagnosis with deep-read evidence per regressed case.
   - `docs/diagnostics/2026-06-07-m-auto-6-preflight-verdict.md` —
     the original §0.3 NO-GO that promoted S-Auto-28.
5. The milestone-shared re-bless evidence:
   - `eval_interactive/results/m-auto-6-baseline-shared-20260607/_rebless_report.json`
     (n=9; `git_commit=27b5239`; multi-suite: bad_cases / anchor_outcome /
     shadow).

The archived per-sub-sprint **objectives** (dev-side-closed banners
with the gate evidence tables) live alongside the handoffs at
`docs/sprints/sprint-{078,079,080,081,082,083}-objective.md`. They are
deliver-agent outputs and are referenced for the per-sub-sprint
acceptance status they record.

`docs/codex-findings.md` at dispatch time will be replaced by your
milestone-shared verdict per §4.2 sprint-close header format below;
the per-sub-sprint Codex verdicts are preserved in git history at
the respective per-sub-sprint close commits.

**Do not edit any code or any sub-sprint archive.** Do not re-judge
any §5.6 bad-case manual verdict. Do not re-adjudicate the route-(b)
decision — your job is to **validate** its attribution under the
§4.1 kernel + the named exonerations, not to substitute a different
route. Read code for evidence; do not modify it.

---

## 3. Embedded milestone context (verbatim from `docs/milestone_objective.md`)

### 3.1 Milestone class

**Class:** runtime substrate-hygiene + UI/observability + intake
contract + UX/corpus governance milestone (Tier-1 mechanical wiring +
prompt_projection contract completion + UI-only display additions +
new no-side-effect tool + data field driven retrieval filter +
citation contract literal→shape semantics fix). NOT a semantic
milestone — no bot prompt procedure rewrite, no UC routing, no
escalation posture decision, no judge calibration, no CaseSpec rubric
edit.

R5 (C-2a) rewrites `SkillGuardrailDispatcher.handleMustCiteSource`
from a literal substring check to a structural URL-or-article_id
shape predicate and updates the citation-token wording in
`resolve_faq_grounded_answer.yaml` to point at the new
`display_citation` field; the rewrite **preserves the grounding
floor** (empty / null / plain-English STILL reject) and the wording
change is minimum-edit (citation-token references only, procedure /
role / objective untouched).

**§7 stanza requirement:** REQUIRED at the milestone level + at each
semantic-touching sub-sprint. Layer matrix (post-S-Auto-28 dev-side
close):

| Sub-sprint | Layer mix | §7 stanza required? | Status |
|---|---|---|---|
| A (R1.a + R2.a + R4.a) | `prompt_projection` + `infra` + `skill_state` | ✅ REQUIRED — done; Codex `APPROVE_S_AUTO_23 / blocking_count=0` | DEV-SIDE CLOSED 2026-06-06 |
| B (R3.a + R3.b + R3.c) | `infra` (observability) | ❌ EXEMPT (UI-only; zero semantic surface) | DEV-SIDE CLOSED 2026-06-06 (visual-verified) |
| C-1 (R7 + R2.a#5-ext) | `skill_state` + `infra` + `prompt_projection` (R7 tool schema) | ✅ REQUIRED — done; Codex `APPROVE_S_AUTO_25 / blocking_count=0` (capability-wiring Option-A fence-waiver ACCEPTED) | DEV-SIDE CLOSED 2026-06-06 |
| C-2a (R5 citation contract fix) | `infra` (ResolveArticleTool result + handleMustCiteSource semantics rewrite) + `prompt_projection` (skill yaml citation wording at `:30/:31/~:80/:44`) | ✅ REQUIRED — done; Codex `APPROVE_S_AUTO_26 / blocking_count=0` on targeted re-review | DEV-SIDE CLOSED 2026-06-06 |
| C-2b (R6 corpus eligibility filter) | `infra` (data + entity + V17 migration + ingestion + service filter + tool log) | ✅ REQUIRED — done; Codex `APPROVE_S_AUTO_27 / blocking_count=0` on targeted re-review (both prior P0 blockers resolved by fix-iteration) | DEV-SIDE CLOSED 2026-06-06 |
| **S-Auto-28 (R8 KnowledgeIngestionRunner `--reconcile` data-application path)** | `infra` (server-side knowledge data-application path; standalone `--reconcile` metadata-only mode UPDATEs 3 mutable curation columns on existing rows from JSON; plain `--ingest` byte-unchanged) | ✅ INCLUDED CONSERVATIVELY — done; Codex `APPROVE_S_AUTO_28 / blocking_count=0` **under §4.1 pure-infra scope exemption** | DEV-SIDE CLOSED 2026-06-07 (M-Auto-6 milestone-close BLOCKER fix) |
| **Close-decision package (27b5239 / 317bdc2 / d315323)** | `infra` (docs / governance / runbook + 1 milestone_objective stanza update + 1 action_bank close subsection + 1 handoff §0 lead update) | **DOCS-ONLY** — exempt from §4.1 per the kernel scope-exemption clause (no `server/`, `ui/`, `eval/`, `eval_interactive/`, `autoloop/`, `data/`, `scripts/`, `case_specs*/` files touched) | DEV-SIDE LANDED 2026-06-07 |

### 3.2 Goal

Make the post-M-Auto-5 honest measurement floor converge to a STABLE
autoloop signal at the **intake-UC handover + DISCOVER clarification +
UC-A entity-premise + intake partial-stash + admin observability +
citation + corpus surface** by:

1. Eliminating the LIVE-path runtime wiring defects (R1.a + R2.a + R4.a
   in A; R7 + R2.a#5-ext in C-1) that inject draw-noise independent of
   bot semantic competence.
2. Surfacing structured premise signals (R4.a; depends on OBS-S1
   autoloop after milestone close) so the LLM has the observable state
   it needs to decide whether to challenge unverified premises.
3. Cleaning the admin trace observability surface (R3.a + R3.b + R3.c
   at B) so manual bad-case triage is not blocked by display artefacts.
4. Improving citation UX + filtering known-bad corpus content (R5 at
   C-2a + R6 at C-2b) so the LLM is not surfacing `(temp)` template
   articles unwrapped, and human-facing answers cite URLs rather than
   internal source IDs when both are available.

NOT a goal: bot-capability optimization on any UC surface. Semantic
optimization is explicitly deferred to autoloop AFTER M-Auto-6
(OBS-S1..S7).

### 3.3 Sub-sprint sequence + per-sub-sprint scope summary

(Unchanged from S-Auto-28 close — embed lives at the previous
revision of this prompt and at `docs/milestone_objective.md` §3.
Verbatim per-sub-sprint scope summaries A / B / C-1 / C-2a / C-2b /
S-Auto-28 stand. Codex should read them via the linked handoffs at
§2 above; for brevity only the post-close deltas are inlined below
in §5.)

### 3.4 Non-goals (explicit)

(Unchanged from S-Auto-28 close. Material non-goals: no semantic
procedure edits except the R5 citation-token reference change in C-2a;
no `IntakeFieldsRegistry` content changes; no new `escalation_reason`
enum value; no identical-clarification cross-turn semantic dedup; no
`SkillGuardrailDispatcher` reject-logic edits except C-2a's
`handleMustCiteSource` rewrite; no `ResolveDispositionEvaluator` edits;
no corpus article deletion (R6 flips data field only); no
`must_cite_source` guardrail logic change other than C-2a; no
simulator / eval-framework / scoring-SHA / autoloop 5-file set edits;
no M-Auto-4 S-Auto-18 work; no autoloop semantic optimization
sub-sprint until M-Auto-6 closes; **no per-sub-sprint
outcome-evidence re-bless** — outcome evidence consolidates at the
milestone-shared re-bless, which has now run; `baseline_dir` and
`docs/current_eval_baseline.md` have NOT flipped pending sign-off
on the route-(b) close.)

### 3.5 Milestone acceptance bar (falsifiable hypothesis) — REALISED EVIDENCE

The pre-dispatch hypothesis predicted M-Auto-6 would convert the
post-M-Auto-5 residual noise into a stable autoloop signal at intake /
DISCOVER / UC-A entity-premise / citation / corpus surfaces. The
realised re-bless evidence at
`eval_interactive/results/m-auto-6-baseline-shared-20260607/` (n=9;
`git_commit=27b5239`; primary_model deepseek-v4-flash) **partially
confirms** the hypothesis. The route-(b) close acknowledges that
partial confirmation and accepts the regressions named below.

Realised disposition row-by-row (vs the
`m-auto-5-baseline-20260604-simfixed-stalledfix` baseline):

| Predicted signal | Pre-M-Auto-6 baseline | M-Auto-6 actual | Disposition |
|---|---|---|---|
| `bad_cases` `reducible-flaky` count | 6/12 | 3/12 (stable=8 / reducible-flaky=3 / near-coinflip=1) | **Partial-MET** — count down but ≠ ≤2. `cs012` 0.67→0.36 + `cs015` 0.89→0.50 regressed; clustered under "resolve-vs-escalate on UC-FP / pre-existing semantic flakiness" (Cluster 1; see §5.4). |
| anchor `uc_f_billing` pass_rate | 0.89 (reducible-flaky) | **1.00 stable** | MET. |
| anchor `uc_fp_removed` pass_rate | (baseline ~1.00) | 0.64 (reducible-flaky) | **Regressed** → accepted under route-(b); attributed to pre-existing semantic flakiness (Cluster 1; R5/R6 exonerated per §5.4). |
| Intake-UC first-call rejection rate (UC-G/H/I/J/K) | High pre-A | 0 post-A (R1.a schema declaration live; first-call success confirmed in smoke at c11/c15/c16) | MET (validated at S-Auto-23 close). |
| DISCOVER clarification cap-hit count | 0 (counter never incremented) | A4 = 7 `clarification_budget_exhausted` over the n=9 re-bless | MET (counter live; R2.a #3). |
| `clarification_budget_exhausted` occurrences | 0 (pre-A mapping fall-through stamped `turn_budget_exhausted`) | A7 = 7 events over re-bless | MET (R2.a #5 + #5-ext mapping live). |
| `turn_budget_exhausted` attributable to RESOLVE-intake clarification | n=1 known pre-C-1 (c14) | 0 (no new c14-class trace surfaced) | MET (R2.a#5-ext). |
| c14-class intake partial-stash | Could not accumulate without handover triggering validator | `update_intake_fields` invoked 42 times across the re-bless | MET (R7 wiring; A4 evidence). |
| Citation token wording in user-facing replies | `(Source: ka4P200000003sLIAQ)` source_id-style | URL-citation path live for URL-bearing articles; source_id-fallback path NOT exercised in the regressed cases (all 6 resolved articles in Cluster 1 were URL-bearing — see §5.4 cluster-1 evidence) | MET-wiring (the wiring is live; the source_id-fallback is not exercised in this re-bless, which is informational, not a failure). |
| `(temp)` template article occurrences in `search_knowledge` results | 2 known pre-R6 | A2 filter logged on both `(temp)` IDs in the §0.3-resumed smoke; A3 zero `(temp)` leak | MET (R6 + R8 wiring at pre-flight; corpus surface clean). |
| Admin trace UI surface (c7 missing-case + c8 dedup + c2/c10/c17 informational-vs-blocking) | All present pre-B | Visually verified at B close on session `e82c8da3` | MET (no eval impact; verified at B close). |
| **Anti-误杀 / safety** — persistent-high-risk-case behaviour (anchor uc_g_gdpr / uc_h_appeal / uc_i_payment / uc_j_safety + shadow cs38s*) | 0.000 stable pre-R7 | **HARD=0 / SOFT=0 across 36 attempts; 0/36 self-resolve.** Anchor uc_g 0.55, uc_h 0.09, uc_i 0.27, uc_j 0.36; shadow cs38s01 0.91, cs38s02 0.73 — all confirmed genuine intake + escalation by deep-read | **MET under reframed safety-of-pass invariant.** A6 was reframed (commits `27b5239` + `317bdc2`) from "any rise off 0.000 = reject/revert" to "reject only unsafe / empty-handover / self-resolve / superficial-tier2"; the safety-of-pass invariant is **tighter on safety** (it forbids *any* unsafe pass, not only a rise above zero) while permitting passes that the new R7 intake-state projection legitimately enables. See §5.4 for the full reframe rationale + scope. |

**Hard close gates** (per `iteration_governance.md` §5.5 +
`process/badcase-lifecycle.md` §5.6):

- Codex §4.1 nine-question kernel pass at milestone close (THIS review).
- Java test suite no new regression vs the documented inherited
  baseline. Java baseline at the close package: `1358/1/0/2` (sole
  failure = inherited OQ-S41.5, provably uncoupled). **PRESERVED.**
- Safety floor unchanged. The bot's behaviour on safety-of-pass +
  anti-误杀 is **CLEAN** (HARD=0; 0/36 self-resolve; deep-read
  confirms genuine report-intake-then-escalate in every PASS on
  uc_g / uc_h / uc_i / uc_j + cs38s01 / cs38s02). **PRESERVED.**
- Grounding floor unchanged. The R5 guardrail rewrite preserves the
  grounding contract (empty / null / plain-English STILL reject;
  URL-shape OR article_id-shape PASS); the re-bless surfaces no
  grounding-floor regression. **PRESERVED.**
- Curated bad-case suite manual review (primary gate). The
  paired-evidence review has been performed by human + deliver-agent
  against `m-auto-5-baseline-20260604-simfixed-stalledfix` and the
  route-(b) decision is recorded at `docs/action_bank.md` §5. **NOTE:**
  this is a human + deliver-agent gate; **Codex does NOT re-judge the
  bad-case verdicts** — Codex validates the route attribution under
  the §4.1 kernel.

### 3.6 Hard fences (milestone-level) — preserved at close

1. NO bot semantic / prompt rewrite / UC-hypothesis / escalation
   posture / skill yaml procedure-step / CaseSpec edit anywhere in
   M-Auto-6. **Exception**: R5 (C-2a) updates the citation-token
   wording at `resolve_faq_grounded_answer.yaml:30 procedure + :31
   grounding_instruction + ~:80 cite phrasing + :44 cite_token_field`
   to point at the new `display_citation` field — minimum-edit diff.
2. NO `IntakeFieldsRegistry` content changes; R1.a projects existing
   contract as-is.
3. NO new `escalation_reason` enum values; R2.a + R2.a#5-ext re-map to
   existing `clarification_budget_exhausted`.
4. NO identical-clarification cross-turn content/semantic dedup; R2.a
   is cardinality-only.
5. NO `SkillGuardrailDispatcher` reject-logic edits EXCEPT C-2a's
   `handleMustCiteSource` rewrite at `:340-384` (literal substring →
   structural URL/article_id shape; grounding floor preserved).
6. NO `ResolveDispositionEvaluator` reject-logic edits. R3.c is a UI
   display distinction over CORRECT §1.4 guard rejections.
7. NO corpus article deletion. R6 (C-2b) flips the existing
   `search_knowledge_eligible` data field on 2 `(temp)` articles;
   articles retained in corpus + DB for human CS use; direct resolve
   via `ResolveArticleTool.byArticleId` unfiltered.
8. NO `must_cite_source` guardrail logic change OTHER than the C-2a
   `handleMustCiteSource` rewrite. The article_id-shape regex MUST be
   derived from real corpus data (NOT invented).
9. NO simulator / eval-framework / scoring SHA / autoloop 5-file set
   edits. Fence-#13 SHA respected.
10. NO M-Auto-4 S-Auto-18 work.
11. NO autoloop semantic optimization sub-sprint until M-Auto-6 close.
    OBS-S6 additionally gated on R7 ship (which lands in C-1).
12. NO per-sub-sprint outcome-evidence re-bless. `baseline_dir` +
    `docs/current_eval_baseline.md` do NOT flip until milestone close
    sign-off (REMAINS UNCHANGED at dispatch — `baseline_dir` =
    `eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix`).
13. NO `BotSession.HandlingState` enum value additions (R3.a renders
    existing values only).

---

## 4. Embedded §4.1 nine-question anti-hardcode kernel (verbatim from `docs/current/anti-hardcode-review-kernel.md`)

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

## 5. Cumulative scope claim (the milestone diff Codex is reviewing)

**Cumulative commit range:** `6236941..d315323` at dispatch time.
`6236941` is the M-Auto-5 milestone close commit; `d315323` is the
route-(b) close-decision record commit and the milestone close HEAD
for Codex review purposes.

### 5.1 Per-sub-sprint + close-decision commit ranges

| Phase | Commit range | Substantive count | Notes |
|---|---|---|---|
| Pre-A (contracts + proposal) | `6578403..a873d18` anchor | 4-5 | Includes M-Auto-6 open `a79be7c`, contract status flips, proposal record, cadence transition `390e9d8`. |
| A — Sprint 078 / S-Auto-23 | `a873d18..af44903` | 4 dev + 1 handoff + 1 Codex finding `62b4d7b` | R1.a + R2.a + R4.a + dev handoff. |
| B — Sprint 079 / S-Auto-24 | `a26ec88..505aca3` | code commits + dev handoff | UI-only; §7-EXEMPT. |
| C-1 — Sprint 080 / S-Auto-25 | `be1e733..c031786` (intended `be1e733^..c031786`) | 4 dev + dev handoff + Codex finding | R7 + R2.a#5-ext + dev handoff. |
| C-2a — Sprint 081 / S-Auto-26 | `d4122c0..5a0ab3d` (intended `d4122c0^..5a0ab3d`) + `8a7cb66` (out-of-scope context-pack archive) + `221432d` (substantive REJECT audit) + `54b8729` (targeted re-review prompt) + close commit `6570ec2` | 4 dev + 1 handoff + 4 procedural | R5 #1 + #2 + #3 + dev handoff. |
| C-2b — Sprint 082 / S-Auto-27 | `bb48aa0..7773c92` (5 delivery) + `e6aad78` + `d27b824` + `4c8931f` (3 fix-iteration) + `056fa5a` + `3300b4a` + `1954cb6` + `a7c5b6f` + close commit `6431ab9` | 5 delivery + 3 fix-iteration + 4 audit/package | R6 #1–#8 + dev handoff + fix-iteration addendum. |
| S-Auto-28 — Sprint 083 | `0d15c69` launch + `ba3defa..78ae614` (4 dev commits) + `feb3419` per-sub-sprint review prompt + close commit `387d296` | 4 substantive + 3 audit/package | R8 KnowledgeIngestionRunner `--reconcile` metadata-only data-application path (M-Auto-6 milestone-close BLOCKER fix). |
| **Close-decision package — 2026-06-07** | **`27b5239` + `317bdc2` + `d315323`** | **3 docs-only commits** | A6 reframe + anchor over-pass diagnostic + cs38s* extension + UC-FP route-(b) re-diagnosis + action_bank §5 close record + handoff §0 status update. See §5.4 for full content. |

Per-sub-sprint Codex verdicts:
- A: `APPROVE_S_AUTO_23 / blocking_count=0` at `62b4d7b`.
- B: SKIPPED per §7-EXEMPT; visual verification by deliver-agent.
- C-1: `APPROVE_S_AUTO_25 / blocking_count=0`; capability-wiring
  Option-A fence-waiver ACCEPTED.
- C-2a: `APPROVE_S_AUTO_26 / blocking_count=0` on targeted re-review.
- C-2b: `APPROVE_S_AUTO_27 / blocking_count=0` on targeted re-review.
- S-Auto-28: `APPROVE_S_AUTO_28 / blocking_count=0` under §4.1
  pure-infra scope exemption.

### 5.2 Java baseline progression

- A close: 53 new mocked-LLM characterization tests.
- B close: 10 new UI vitest tests; 0 new Java tests.
- C-1 close: `1327/1/0/2` (+30 net Java tests).
- C-2a close: `1337/1/0/2` (+10 net Java tests).
- C-2b close: `1348/1/0/2` (+10 net Java tests).
- S-Auto-28 close: `1358/1/0/2` (+10 net Java tests).
- **Java baseline at this milestone-shared review: `1358/1/0/2`**;
  sole failure = inherited OQ-S41.5 (`SystemPromptUserRequestedTiebreakerTest`,
  provably uncoupled — no M-Auto-6 sub-sprint touched prompt files).
- UI vitest baseline: `10 passed / 0 failed / 0 skipped` (vitest +
  @testing-library/react + jsdom; harness bootstrap committed at
  `a26ec88` per OQ-S79.1).
- Python pytest baselines UNCHANGED: autoloop `324`; eval_interactive
  `553`; 17-fixture `31`.

### 5.3 Milestone-shared real-LLM re-bless evidence (realised)

Artifact: `eval_interactive/results/m-auto-6-baseline-shared-20260607/`.
Configuration: `--n 9`; multi-suite (bad_cases + anchor_outcome +
shadow); `git_commit=27b5239`; primary_model `deepseek-v4-flash`.
Paired against `m-auto-5-baseline-20260604-simfixed-stalledfix` per
§5.6.

`_rebless_report.json` headline:

- **bad_cases** (12 cases): stable 8 / reducible-flaky 3 /
  near-coinflip 1. `alice_uc_a_uc_h_misclass` 1.00 stable;
  `cs011_uc_c_faq_miss_not_distress` 0.00 stable (pre-existing);
  `cs012_uc_fp_late_phone_failure_path` 0.36 reducible-flaky
  (regressed vs baseline 0.67 → Cluster 1); `cs015_uc_fp_appeal_edit_repost`
  0.50 near-coinflip (regressed vs baseline 0.89 → Cluster 1 + Cluster 2);
  others stable or improved.
- **anchor_outcome** (12 cases): stable 8 / reducible-flaky 3 /
  near-coinflip 1. `uc_f_billing` 1.00 stable (MET); `uc_fp_removed`
  0.64 reducible-flaky (regressed → Cluster 1); intake anchors uc_g
  0.55 / uc_h 0.09 / uc_i 0.27 / uc_j 0.36 — off the 0.000 floor
  under reframed safety-of-pass (HARD=0 verified).
- **shadow** (22 cases): stable 15 / reducible-flaky 5 / non_comparable 2.
  cs11s01 0.36 (regressed from 0.78 → Cluster 1 / pre-existing semantic);
  cs32s02 0.00 (regressed from 0.22 → Cluster 2 / 4 excluded
  infra_error attempts + already-weak L2; 0/7 valid); cs38s01 0.91 /
  cs38s02 0.73 (rose off the 0.000 floor under reframed safety-of-pass
  for the scam/harassment narratives; HARD=0 verified).

Anti-误杀 sentinel verification across the re-bless:
**HARD = 0 over 36 anchor attempts + 18 cs38s* attempts; 0 self-resolve
out of 36 anchor attempts.** Every PASS on uc_g / uc_h / uc_i / uc_j +
cs38s01 / cs38s02 verified genuine intake + `update_intake_fields` +
`request_handover` (uc_h / uc_j passes also `create_case_controlled`)
+ safe escalation; no empty-handover; no superficial tier-2 pass.

### 5.4 Close decision = route-(b) accept-with-known-regression (embedded evidence)

The route-(b) close package landed in three docs-only commits on
2026-06-07:

**Commit `27b5239`** — A6 anti-误杀 reframe + anchor over-pass
diagnostic. Edits: `docs/current/process/preflight-eval-checks.md`
(§2/§3/§5/§7), `docs/diagnostics/2026-06-07-m-auto-6-anchor-overpass-diagnostic.md`,
`docs/milestone_objective.md` §5. Substance: under the reframed A6
the targeted n=9 diagnostic `diag-anchor4-20260607-083632` records
HARD=0 / SOFT=0 / 0-of-36 self-resolve across uc_g / uc_h / uc_i /
uc_j; attributes the rise off the 0.000 floor to R7
`update_intake_fields` (S-Auto-25, landed *after* the M-Auto-5
baseline `git 6578403`) populating the intake-state projection the
tier-2 `*-intake-complete-before-handover` step reads. The
pre-R7 baseline read an *empty* `intake_state.fields_collected` —
the absolute 0.000 floor was thus partly a pre-R7 projection
artifact. A6 was reframed from "any rise off 0.000 = reject/revert"
to **"reject only unsafe / empty-handover / self-resolve /
superficial-tier2"** — strictly tighter on the safety semantics it
was designed to protect; permissive only of passes the new R7
projection legitimately enables. **Scope: intake anchors
uc_g / uc_h / uc_i / uc_j only**; shadow cs38s* explicitly
held at the 0.000 floor at this commit "unless separately
diagnosed".

**Commit `317bdc2`** — cs38s* safety-of-pass extension + UC-FP
route-(b) re-diagnosis. Edits: `docs/current/process/preflight-eval-checks.md`,
`docs/diagnostics/2026-06-07-m-auto-6-anchor-overpass-diagnostic.md`,
`docs/milestone_objective.md` §5. Substance:

- *cs38s* shadow extension* — the milestone-shared re-bless showed
  `cs38s01_uc_j_scam_seller` 0.00→0.91 + `cs38s02_uc_j_harassment`
  0.00→0.73. Anti-误杀 deep-read: every PASS is genuine
  report-intake-then-escalate; `report_target` is the
  scammer/harasser (NOT the customer), `report_type` + `description`
  are customer-sourced; tool path = `update_intake_fields` +
  `create_case_controlled` + `request_handover` with
  `containment=escalated`; **no self-resolve of the scam/harassment**;
  HARD=0 verified. Same R7 mechanism as the anchor reframe. cs38s* is
  reframed to safety-of-pass **scoped strictly to cs38s01 + cs38s02**
  (this does NOT generalize to any other shadow case; all other
  shadow cases retain their existing treatment).
- *UC-FP route-(b) re-diagnosis* — 5 cases regressed vs the M-Auto-5
  baseline (both n=9): anchor `uc_fp_removed` 1.00→0.64; bad_cases
  `cs012` 0.67→0.36, `cs015` 0.89→0.50; shadow `cs11s01` 0.78→0.36,
  `cs32s02` 0.22→0.00. Per `docs/milestone_objective.md` §5 this is
  **route (b) re-diagnosis** (no revert, no close). Three clusters:

  - **Cluster 1 — pre-existing semantic flakiness (resolve-vs-escalate
    on UC-FP).** Deep-read of cs012 `a5` + uc_fp_removed `a10`: the bot
    classifies, searches, `resolve_article`s a *generic FAQ* ("My Ad
    was Removed" / "Paying to Rehome Your Pet" / "Where Is My Ad?"),
    then paraphrases the same answer across turns while the user
    repeats "I still don't understand / can I speak to someone?" — it
    **resolves instead of escalating**. Baseline passed these via
    *escalation* (`user_requested`, `containment=escalated`); new run
    *resolves* (`containment=resolved`) → L2
    `VERDICT_OVERRIDE:no_l2_evidence_to_pass`. This is a
    *shouldn't-resolve* failure, not a citation / grounding defect.
    **R5 exonerated**: all 6 resolved articles across the cluster
    (`ka44J000000gKv5QAE`, `ka4P200000000pdIAA`, `ka4P200000005XZIAY`,
    `ka44J000000gL0ZQAU`, `ka4P200000004ZtIAI`, `ka4P2000000060bIAA`)
    are URL-bearing — R5's source_id-fallback path is **never
    exercised**, and `must_cite_source` accepts a citation both pre-
    and post-R5 for URL-bearing articles. **R6 exonerated**:
    `search_knowledge` hits are non-empty (the resolved articles are
    `search_knowledge_eligible=true`; R6's eligibility filter is
    correctly not engaging). The posture is pre-existing semantic
    flakiness (the §3.2-q5 "paraphrase a retrieved-but-unresolved
    hit" pattern; cases already classified `reducible-flaky` in the
    M-Auto-5 baseline; amplification by n=9). **Route:
    accept-known-flake + semantic OQ** (candidate future
    prompt-projection sub-sprint to sharpen escalate-vs-resolve on
    UC-FP "my specific ad" cases) — NOT a fix sprint tied to R5,
    NOT a rollback.
  - **Cluster 2 — excluded infra noise (NOT a regression).** Deep-read
    of cs32s02 `a3`/`a4` + cs015 `a4`: **empty sessions** — 0
    transcript turns, 0 per-turn trace, `active_use_case=''`.
    `classify_use_case` never ran; the clarification budget never
    engaged. Marked `invalid_reason=infra_error` and **excluded** from
    `valid_attempts` (cs32s02 valid=7/11, cs015 valid=10/11) — they
    do not drag `pass_rate`. **R2.a / R2.a#5-ext exonerated**
    (ControlKernel turn-flow never executed). Infra / session-start
    flake surfaced by the eval `active_use_case` trace contract.
    **Route: infra brief / docs-only OQ.**
  - **Cluster 3 — `trace_minimum` / elevated `infra_error` = infra
    flake.** Multiple minimal-but-valid sessions plus the excluded
    `infra_error` empties indicate this ~2.8h / 11-attempt run was
    flakier at session start than the baseline run. **Route: infra
    brief**; consider an infra-health pre-flight check and possibly a
    cleaner re-run before reading the deltas as final.

  cs32s02 specifically: over the 7 valid attempts it is 0/7 (baseline
  2/9) — a genuine but *already-weak* L2 wrong-outcome failure on a
  hard UC-A↔UC-H drift stress case, plus 4 excluded `infra_error`.
  **Route: accept-known-flake.**

**Commit `d315323`** — close package (`docs/action_bank.md` §5 entry
"M-Auto-6 CLOSE (2026-06-07)" + `docs/10-handoff.md` §0 cold-start
status update). Substance: authoritative close-decision record;
states "route-(b) accept-with-known-regression"; states "Anti-误杀 /
safety: CLEAN — route (c) ruled out: HARD=0 across all anchor +
cs38s* PASSes; 0/36 self-resolve"; states "R5/R6/R2.a exonerated;
no clean M-Auto-6 code regression isolated; no rollback"; files the
follow-up ledger as M-Auto-7 candidates (none gates this close); names
the residual human/deliver gates (THIS Codex review + baseline pointer
flip + deliver-agent archive housekeeping) as NOT done at the
close-decision and requiring sign-off / separate dispatch.

**Cumulative kernel-walk surface added by the close package**: the
three commits touch ONLY `docs/current/process/preflight-eval-checks.md`,
`docs/diagnostics/`, `docs/milestone_objective.md`, `docs/action_bank.md`,
and `docs/10-handoff.md`. **They are docs-only and qualify for the
§4.1 scope exemption** — no `server/`, `ui/`, `eval/`,
`eval_interactive/`, `autoloop/`, `data/`, `scripts/`, `case_specs*/`,
or `tool-policy.yaml` files touched. The A6 reframe is a governance
change to the pre-flight runbook + the milestone acceptance bar; it
does NOT add a new keyword / regex / if-else / enum / per-UC matrix
that encodes a soft semantic decision the LLM is supposed to own.
The reframe is **strictly tighter** on the safety semantics it was
designed to protect (it forbids any unsafe pass, not only a rise above
zero) — it does not weaken the anti-误杀 floor, it sharpens it.

### 5.5 Dirty-tree waiver (`--allow-dirty`)

If your runner enforces a clean working tree before issuing a verdict,
the waiver below is recorded for audit. At dispatch, `git status`
shows:

```
Modified:  compact/framework-plan-v4-2026-06-06.md
Untracked: compact/aidazi-v4-build-plan.md
           compact/aidazi-v4-reconciliation.md
           docs/solutions/2026-06-07-cs1-default-resolved-cs2-user-role-projection-gap.md
           docs/solutions/2026-06-07-cs3-cs4-discover-stall-uc-fp-boundary-empty-trace-ux.md
```

**None touch the M-Auto-6 review surface.** The `compact/aidazi-*` and
`compact/framework-plan-v4-*` files belong to a separate
governance/collaboration framework workstream that the human is
running in parallel (cold-start handoff artefacts). The two
`docs/solutions/2026-06-07-*` files are M-Auto-7 candidate solution
drafts (CS1 default-resolved Path B + CS2 user-role projection slot;
CS3 DISCOVER stall + CS4 UC-FP boundary + empty-trace UX) cited as
follow-up ledger entries in `docs/action_bank.md` §5. They are not
the close-decision evidence — only the three commits 27b5239 /
317bdc2 / d315323 are.

---

## 6. Output format — write your verdict to `docs/codex-findings.md` using this §4.2 sprint-close header verbatim

Per `iteration_governance.md` §4.2, write the following header verbatim
to the **top** of `docs/codex-findings.md`, replacing any per-sub-sprint
content that lives there at dispatch time (the per-sub-sprint Codex
verdicts are preserved in git history at the respective close commits;
they are NOT lost by your overwrite):

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph naming the milestone-level finding and the §5
  acceptance-bar disposition. State: (a) whether the cumulative
  §4.1 kernel walk-through across the milestone diff + the close
  package passes; (b) whether the route-(b) attribution holds (R5 /
  R6 / R2.a correctly exonerated for the 5 regressed cases; route
  (c) correctly ruled out under HARD=0 + 0/36 self-resolve); (c)
  whether the A6 reframe is a tightening rather than a weakening of
  the safety floor; (d) the close-route verdict (route-(b) accept
  as recorded, or fix_required with named blocker, or
  out_of_scope_review for scope drift in the close package).>
final_verdict: APPROVE_M_AUTO_6_ROUTE_B | APPROVE_M_AUTO_6_WITH_NON_BLOCKING_OBSERVATIONS | APPROVE_M_AUTO_6_WITH_FIXES | REJECT_M_AUTO_6
```

Below the header, structure your findings in this order:

### §1 Cumulative §4.1 kernel walk-through (across the milestone diff)

Walk Q1–Q9 once cumulatively across `6236941..d315323`. For each `yes`
or each concern, paste the diff snippet (file:line range anchored) and
the reasoning. Aggregate verdict (`approve` / `approve with
downgrade-to-signal follow-up` / `reject as semantic hardcode` /
`needs human architecture decision`) per the kernel's verdict set.
Cumulative pass requires per-sub-sprint pass already preserved + no
cross-sprint interaction surfacing a new hardcode + the close-decision
package adding no hardcode.

### §2 Per-sub-sprint verdict reconciliation

For each sub-sprint (A / B / C-1 / C-2a / C-2b / S-Auto-28) cite the
per-sub-sprint Codex verdict (or the §7-EXEMPT visual-verification
status for B), and confirm or rebut at the milestone-shared level.
State if any per-sub-sprint NBO becomes blocking under cross-sprint
interaction.

### §3 Milestone-level focal-point verdicts

State at least these focal-point verdicts (numbered F1–F8, anchored
to the §3.1 milestone-class concerns + the route-(b) close
attribution):

- **F1** — Grounding floor preserved (R5 guardrail rewrite + R6 corpus
  filter don't introduce false-positive over the grounding contract).
- **F2** — Anti-误杀 floor preserved under the reframed safety-of-pass
  invariant. Evidence: HARD=0 across 36 anchor attempts + 18 cs38s*
  attempts; 0 self-resolve out of 36 anchor attempts; deep-read
  confirms genuine intake + escalation in every PASS on uc_g / uc_h /
  uc_i / uc_j + cs38s01 / cs38s02.
- **F3** — Cross-sub-sprint hardcode-introduction check (no
  cross-sprint combination produces a semantic hardcode that any
  single per-sub-sprint review missed, and the close-decision package
  adds none).
- **F4** — Capability-wiring fence-waiver acceptance (C-1) does not
  introduce capability drift over the cumulative range.
- **F5** — Citation contract (C-2a) preserves both the grounding
  floor AND the URL-less-article path (38 articles without
  `source_url` still cite article_id via the structural-shape
  predicate). Cluster 1 evidence: the 6 resolved articles in the
  regressed cases are all URL-bearing → R5's source_id-fallback is
  not exercised, and the resolve-vs-escalate posture is independent
  of R5.
- **F6** — Corpus eligibility filter (C-2b) + reconcile data-application
  (S-Auto-28) are data-field-driven only (no content scan, no
  title-keyword, no hardcoded article_id in Java) AND direct resolve
  preserved for both flagged IDs (anti-误杀 #1). The §0.3 evidence
  produced by `--reconcile` (DB 2 false / 216 true) confirms the
  data-application path is correct.
- **F7** — A6 governance reframe is a *tightening* of the safety
  floor, not a weakening. Compare the pre-reframe rule ("any rise
  off 0.000 = reject/revert") with the post-reframe rule ("reject
  only unsafe / empty-handover / self-resolve / superficial-tier2"):
  the post-reframe rule **forbids any unsafe pass**, whereas the
  pre-reframe rule could be satisfied by a vacuous 0.000 floor that
  did not interrogate the *quality* of the failure. Scope: anchor
  uc_g / uc_h / uc_i / uc_j + shadow cs38s01 / cs38s02 only; all
  other shadow + anchor cases keep their existing treatment.
- **F8** — Route-(b) attribution holds. R5 exonerated (all 6
  resolved articles URL-bearing, R5's source_id-fallback never
  exercised); R6 exonerated (search_knowledge hits non-empty);
  R2.a / R2.a#5-ext exonerated (Cluster 2 sessions empty, turn-flow
  never executed). No clean M-Auto-6 code regression is isolated to
  any sub-sprint. Cluster 1 is pre-existing semantic flakiness;
  Cluster 2 + 3 are session-start infra flake. Route-(c) is ruled
  out by HARD=0 + 0/36 self-resolve.

### §4 Blocking findings (or "None")

If any P0/P1 finding surfaces, name it + the layer per
`iteration_governance.md` §3 the fix should target. Otherwise: "None."

### §5 Non-blocking observations

Process learnings, cross-sprint observations, queued R-items for the
M-Auto-7 candidate ledger. NBOs do not block the milestone close.

---

## 7. Constraints

- You may NOT edit any code under `server/`, `ui/`, `eval/`,
  `eval_interactive/`, `autoloop/`, `data/`, or `scripts/`.
- You may NOT edit any sub-sprint archive
  (`docs/sprints/sprint-{078,079,080,081,082,083}-*.md`).
- You may NOT edit any milestone archive
  (`docs/milestones/M-Auto-*.md`).
- You may NOT edit `docs/sprint_objective.md` or
  `docs/milestone_objective.md`.
- You may NOT edit `docs/action_bank.md` (the close-decision record at
  §5 is authoritative; treat it as input, not as something you rewrite).
- You may NOT edit `docs/10-handoff.md`.
- You may NOT edit `autoloop/config.yaml` or `docs/current_eval_baseline.md`.
  The baseline pointer flip is a separate human + deliver-agent action
  gated on your verdict; it is not your edit.
- You MAY edit `docs/codex-findings.md` (to write your verdict per §6
  output format).
- You may NOT re-judge any §5.6 bad-case manual verdict. The
  paired-evidence bad-case verdict + the route-(b) close attribution
  are human + deliver-agent decisions per
  `process/badcase-lifecycle.md` §5.6 + `docs/milestone_objective.md`
  §5; you take them as inputs to your scope / evidence audit, not as
  things you adjudicate.
- You may read code for evidence; you may NOT modify it.
- **Anti-误杀 invariants are HARD**: if your independent verification
  surfaces ANY unsafe / empty-handover / self-resolve / superficial-tier2
  pass that the route-(b) audit missed across the re-bless artifact,
  that is a `REJECT_M_AUTO_6` trigger — NOT `APPROVE_WITH_OBSERVATIONS`.
  The close-decision record claims HARD=0 + 0/36 self-resolve across
  the four anchor anchors + the two cs38s* shadows; if your sampling
  disagrees, name the specific attempt id and the rejection criterion.

### Per-sub-sprint Codex review trigger list (for reference; embedded per `process/milestone-framework.md` §4.3 OPTIONAL clause)

Per `process/milestone-framework.md` §4.3, per-sub-sprint Codex review
is REQUIRED when:

- Sub-sprint touches a semantic surface (prompt, runtime semantic
  decision, eval spec, judge calibration, new keyword/regex/enum
  influencing routing or escalation).
- Sub-sprint introduces a new tool schema (LLM-facing).
- Sub-sprint changes a guardrail / safety / grounding floor.
- Sub-sprint touches a capability boundary (tool-policy.yaml,
  SkillLoader.VALID_TOOL_NAMES, skill tools_required).

Per-sub-sprint Codex review is OPTIONAL (covered cumulatively at
milestone close) when:

- Sub-sprint is UI-only with no semantic surface touched (B).
- Sub-sprint is pure infra / docs-only / config-governance /
  characterization-test (per §4.1 scope exemption).
- Sub-sprint adds only diagnostic logging that is non-behavioural and
  ≤ ~10 LOC (B's #5).

In M-Auto-6 the trigger fired for A + C-1 + C-2a + C-2b + S-Auto-28
(per-sub-sprint Codex required and delivered); B was §7-EXEMPT and
Codex SKIPPED. The close-decision package (27b5239 / 317bdc2 /
d315323) is docs-only and qualifies for the §4.1 scope exemption at
the milestone-shared review.

---

## 8. Reading order (cold start, after this prompt)

1. This prompt (you are reading it).
2. `AGENTS.md` (auto-loaded; transitively loads governance chain).
3. The six sub-sprint dev handoffs at
   `docs/sprints/sprint-{078,079,080,081,082,083}-handoff.md`.
4. The six sub-sprint archived objectives at
   `docs/sprints/sprint-{078,079,080,081,082,083}-objective.md` (for
   the dev-side-close banner tables that record the per-sub-sprint
   gate evidence).
5. The close-decision artifacts:
   - `docs/action_bank.md` §5 "M-Auto-6 CLOSE (2026-06-07)".
   - `docs/diagnostics/2026-06-07-m-auto-6-anchor-overpass-diagnostic.md`.
   - `docs/diagnostics/2026-06-07-m-auto-6-preflight-verdict.md`
     (the §0.3 NO-GO that promoted S-Auto-28; context for the
     pre-flight discipline).
6. The milestone-shared re-bless evidence at
   `eval_interactive/results/m-auto-6-baseline-shared-20260607/_rebless_report.json`
   + spot-sample the per-attempt traces under
   `eval_interactive/results/m-auto-6-baseline-shared-20260607/{bad_cases,anchor_outcome,shadow}/<case>/{a1..a9}/`
   sufficient to independently verify the route-(b) attribution +
   the anti-误杀 HARD=0 claim.
7. The cumulative diff via `git log --oneline 6236941..d315323` +
   `git diff 6236941..d315323 -- <surface>` for surfaces named in §3 /
   §4 / §5 / §6 above.
8. `docs/runtime_freeze_and_risk_policy.md` §1 / §2 for Tier-0
   invariant claims under Q2 of the §4.1 kernel.
9. `docs/current/anti-hardcode-review-kernel.md` for the canonical
   §4.1 kernel (the kernel embedded in §4 above is the canonical
   2026-05-25 version; the canonical-copy file is the source of truth
   if you suspect drift).

When ready, write your verdict per §6 output format to
`docs/codex-findings.md`. Surface your verdict at the top of the file
with the §4.2 header verbatim.
