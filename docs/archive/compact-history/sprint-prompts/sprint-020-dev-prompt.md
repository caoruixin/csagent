Paste the content below this line into a fresh Claude Code session in a worktree of this repo on the current branch `design-v1-without-human-review` (or a feature branch if you prefer). Dev agent has zero conversation context.

---

You are the dev agent for **Sprint 20 — G2 Interactive Case Family + Shadow Split (Track A) + Already-Called Soft Signal (Track B)**. Two-track sprint mirroring Sprint 19's A+B precedent. Track A is the meta-sprint for generalization (case-family authoring + v0 shadow-split mechanism); Track B is a read-side soft-signal slot in projection. Track A is content-authoring (not subject to Bundle-or-defer); Track B is bundled per the sprint objective's policy.

## 1. Load the constitution chain (do this first)

Read in order. Do not start coding until all are read.

1. `AGENTS.md` — repo constitution chain.
2. `docs/current/doc_governance.md` — tier model, source-of-truth rules.
3. `docs/current/agent_context_guide.md` — per-task reading lists + Context Pack Prompt. Track A: "Eval, governance" list. Track B: "Runtime, phase machine, drift" list.
4. `docs/current/iteration_governance.md` — §1 (Constitution), §3 (Fix Layer Classification — Track B), §5 (Eval Acceptance Rules — Track A IS the §5.1 deliverable), §7 (Sprint-objective stanza).
5. `docs/sprint_objective.md` — Sprint 20 scope. End-to-end. §"Bundle-or-defer policy", §"Do not implement", §"Layer-classification + anti-hardcode stanza" are normative.
6. `docs/diagnostics/failure-briefs/*.md` — all 10 G1 briefs. Track A authors a case family per brief.
7. `docs/sprints/sprint-019-handoff.md` — §3 annotates which 2026-05-10 fail tags are "infra-shape noise" vs "real semantic failure"; §4.2 is the Track B foundation.
8. `docs/sprints/sprint-018-handoff.md` — Method note on L3 override check (`eval_interactive/case_spec_overrides.yaml` by `source_session_id`) + manual-probe ground-truth derivation. Both apply to Track A.

**Produce a Context Pack** per `agent_context_guide.md` before writing code or CaseSpecs. Include:

- Source-of-truth decisions per track (especially: where do new case families live? Where does the shadow-split mechanism live? Path proposals required).
- Implementation status of `ContextProjectionBuilder.java` (or whatever owns projection assembly today), existing slot shapes, whether `arguments_hash` has a precedent.
- Implementation status of existing shadow mechanism. Repo grep shows Wave A6.1 shadow-audit refs in `eval_interactive/eval_interactive/case_spec/llm_persona_reviewer.py`; the §5.1 sprint-acceptance shadow split is NOT delivered. Track A delivers v0.
- Top 3-5 risks.

Paste the Context Pack into the chat and wait for human confirmation before authoring CaseSpecs or wiring the slot.

## 2. Track A scope (G2 case-family authoring + shadow split)

For each of the 10 G1 briefs, produce a case family:

- **Target case** (≥1): the brief's case. For the 9 smoke briefs, the target CaseSpec exists in `eval_interactive/case_specs/smoke/` — promote / reference. For the manual-probe brief, author fresh.
- **Neighbor cases** (≥2): same failure shape / UC, should pass under the brief's expected behaviour.
- **Negative-control cases** (≥2): should NOT trigger the brief's failure shape. Used to confirm a future remediation is provably narrow.
- **Shadow cases** (≥2): held out from you; visible to the human and to the review agent only.

Total: ≥70 CaseSpec-shaped entries (10 × ≥7 per family). Confirm exact counts in Context Pack and handoff §9.

**Authoring discipline (anti-hardcode):**

- CaseSpec describes user shapes + expected behaviours, NOT runtime decisions. Do NOT encode keyword / regex / per-UC matrix into YAML.
- Before treating any CaseSpec as ground truth, check `eval_interactive/case_spec_overrides.yaml` by `source_session_id` (Sprint 18 Method note). For manual-probe, derive from phase 2 UC policy + bot `phase_plan` `system_instruction`.
- **cs_192 family:** if the target authoring surfaces a `CONTRACT_VIOLATION:active_use_case` that would re-open Sprint 8 §K0 remediation, STOP, flag `human_review_required` in handoff §10, do not invent a Tier-0, proceed with the other 9 families. cs_192 family is authored; remediation is deferred.

**Shadow-split mechanism (v0).** Must enforce: dev agent cannot read shadow files during development; human + review agent CAN; harness recognises shadow class and treats it differently. You propose mechanism shape in the Context Pack (directory layout, naming convention, runner flag, access boundary doc); human reviews before you build. Plausible shapes: separate gitignored sub-directory committed via a separate flow; `shadow: true` flag in front matter + runner skip; manifest of shadow case ids + runner gate. Pick one, justify, wait for confirmation. v0 may be lightweight.

Track A explicitly does **NOT** remediate any failure it catalogues. No prompt / runtime / judge / corpus edit. Shadow-split is the only non-CaseSpec deliverable Track A ships.

## 3. Track B scope (`R-prompt-projection-already-called-soft-signal`)

Add `already_called` to per-step projection (confirm exact file path in Context Pack; likely `ContextProjectionBuilder.java`):

```
already_called: [
  { "tool": "<name>", "arguments_hash": "<stable hash>", "at_step": <int> },
  ...
]
```

Lists prior identical-args tool calls within the same `AgentRunLoop.run(...)`. Observability only; LLM owns whether to re-emit. No Java check enforces consumption.

**Regression test** (`server/test/**`) demonstrates: (a) slot populated when prior identical-args call exists; (b) slot empty / absent otherwise; (c) runtime does NOT short-circuit on slot population (write-tool dispatch proceeds; read-tool dispatch proceeds — `R-idempotent-read-tool-short-circuit` is out of Sprint 20 scope).

**Bundle hard rules (Track B):**

- No keyword / regex / if-else / enum / per-UC matrix.
- No prompt edit beyond pure projection. If `system_prompt.txt` already iterates the projection, no change there; if not, do NOT edit the prompt to consume the slot — that is a separate prompt sprint.
- No short-circuit on the slot.
- Soft signal only.

## 4. Files in scope

**Track A:** `eval_interactive/case_specs/<family-dir>/*.yaml` (new CaseSpecs; exact dir proposed in Context Pack); `eval_interactive/scripts/` or wherever harness lives (shadow-split file(s); location proposed in Context Pack); `eval_interactive/eval_interactive/` Python package if runner needs shadow-aware flag; `docs/sprints/sprint-020-handoff.md` (NEW); `docs/10-handoff.md` (refresh lead to Sprint 20).

**Track B:** `server/**` projection-assembly path (likely `ContextProjectionBuilder.java`); `server/test/**` regression test.

## 5. Files NOT in scope (hard fence)

Do NOT edit: `docs/sprint_objective.md` (deliver agent owns); any `docs/sprints/sprint-0XX-*.md` archive; `docs/codex-findings.md` (Codex writes at close); `docs/current/iteration_governance.md`, `doc_governance.md`, `agent_context_guide.md` (fold-back cadence); `docs/foundational/**`; `docs/runtime_freeze_and_risk_policy.md` (no new Tier-0); `eval_interactive/case_specs/smoke/*.yaml` (read-only refs; promoting into a family manifest OK, behaviour-altering edits NOT); `eval_interactive/case_spec_overrides.yaml` (Wave A5/A6); `eval_interactive/personas*.yaml`; `data/faq/**`; FAQ corpus; judge rubric files; `system_prompt.txt` / any prompt template beyond the projection slot; any short-circuit / dispatch dedup on `already_called` (= `R-idempotent-read-tool-short-circuit`, out of scope).

## 6. Sprint 20 handoff structure (12 sections; required)

Write `docs/sprints/sprint-020-handoff.md`:

1. **Context Pack** — your §1 pack verbatim with human confirmations annotated.
2. **Sprint-objective recap** — quote goal + two-track split.
3. **Track A — 10 case families.** One subsection per brief: brief id, target case id/file, neighbor / negative / shadow case ids, source CaseSpec (for 9 smoke families), L3 override status, authoring rationale.
4. **Track A — Shadow-split mechanism v0.** File layout, naming convention, runner flag, dev-agent access boundary documentation, harness files added.
5. **Track B — `already_called` slot.** Final slot shape, arguments-hash function (cite impl), at-step semantics, regression test location, three behaviour bars confirmed.
6. **Files changed** — one-line rationale per file. Split Track A / Track B.
7. **Layer-classification self-walk** — restate §7 multi-layer stanza: Track A = `eval_spec` + `infra`; Track B = `prompt_projection`.
8. **Anti-hardcode self-walk (§4.1, 9 questions)** — answer each per the diff.
9. **Generalization-coverage table** — Track A IS the deliverable: 10 families × counts + shadow-split enforcement story.
10. **Sprint-objective-met check** — walk every "Do not implement" + every "Success metrics" bar.
11. **Open questions** — cs_192 `human_review_required`; shadow enforcement gaps; L3 override surprises.
12. **Action-bank deltas + next recommended action** — new R-items if any surface; next sprint recommendation.

## 7. Eval requirements

- Run smoke after Track B's slot lands: `pytest eval_interactive/...` per existing harness. Capture `results.json`; reference in handoff §5.
- Confirm safety / grounding floors per `iteration_governance.md` §5.1.
- Do NOT run Track A's case families as a smoke-style eval — they are authored for downstream sprints; running mixes Sprint 20's authoring with Sprint 19's slow-LLM noise. Authoring success is verified by inspection.
- Do NOT consume any shadow-class case during Sprint 20. You author shadow; you do not read it.

## 8. Stop conditions

Stop and surface to the human if:

- Context Pack path proposals are unclear or unconfirmed.
- cs_192 family target authoring surfaces a `human_review_required` shape (§3.2 Q2). Author the rest; flag the remediation gap in §10.
- `ContextProjectionBuilder.java` doesn't exist at the expected path, or projection assembly is owned elsewhere. Confirm via Context Pack.
- Shadow-split v0 design surfaces an enforcement question v0 cannot cleanly answer. Surface; do not invent.
- Smoke rerun after slot wiring shows a regression on the 8 non-regressed Sprint 19 smoke cases. Slot is observability-only; any regression is a wiring bug, not semantic. Investigate; if not resolvable in one round, surface.

Surface via handoff §11; stop pushing commits until human responds.

## 9. Commit shape

Branch is `design-v1-without-human-review` (human runs locally; no PR). Commit all your update at the end would be fine. Deliver agent archives at sprint close in a separate commit. The handoff is the artefact Codex reviews — make it precise.
