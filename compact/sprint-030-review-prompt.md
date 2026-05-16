Paste the content below this line into a fresh Codex session after the dev's commit lands. No PR will be opened; review the commit range.

---

# Sprint 30 review prompt — Alternate-UC signal data-source design (investigation-only, docs-only)

You are the review agent (Codex). Sprint 30 is **docs-only**. Single deliverable: one new design proposal doc + one R-item registration. Sprint 30 is **exempt** from the §4.1 Anti-Hardcode kernel per `iteration_governance.md` §4.1 ("Pure infra, docs-only, config-governance, and characterization-test PRs are not subject to this review"). Codex review is **OPTIONAL** at sprint close per `feedback_close_with_codex_skipped_docs_only_outcome.md` (Sprint 16 / 22 / 26 / 27 precedent).

If the human declined to invoke Codex review, no work is needed — return one line: "Sprint 30 docs-only — Codex review declined per §4.1 docs-only exemption (Sprint 16/22/26/27 precedent)."

If Codex review IS invoked, run the targeted review below.

## 1. Loader stanza

Read in order before any verdict:

1. `AGENTS.md`.
2. `docs/current/doc_governance.md` (esp. "PR checklist for docs-only reconciliation").
3. `docs/current/iteration_governance.md` §1 / §3 / §4.1 / §4.2 / §5 / §7.
4. `docs/sprint_objective.md` — Sprint 30 scope.
5. `docs/sprints/sprint-030-handoff.md` — the dev's handoff.
6. `docs/proposals/alternate_uc_signal_data_source_design.md` — the design doc the dev produced.
7. Precedents: `docs/proposals/handover_orchestrator_design.md` (Sprint 16 docs-only design freeze) + `docs/sprints/sprint-027-handoff.md` (most recent investigation-only sprint).
8. `compact/sprint-030-dev-prompt.md` — what the dev was authorized to do vs what landed.

## 2. §4.1 kernel

**Sprint 30 qualifies for the §4.1 docs-only exemption.** The kernel does NOT need to be walked verbatim. Return the kernel verdict as `approve (exemption: docs-only design freeze; no semantic surface touched)` per Sprint 16 / 22 precedent.

If the dev shipped any code (any file under `server/`, `eval_interactive/`, or `server/src/main/resources/prompts/`), the exemption is **VOID** — Sprint 30 was authored as docs-only; any code change is scope drift. Walk the kernel verbatim AND flag scope-drift as a `fix_required` blocker.

## 3. Sprint-30-specific deviations to verify

### 3.1 Bundle expectations (NOT scope drift)

The bundle is one design doc + one R-item registration:

1. `docs/proposals/alternate_uc_signal_data_source_design.md` — NEW design doc per `docs/sprint_objective.md` §5.2 (10 sections).
2. `docs/action_bank.md` — APPEND `R-alternate-uc-signal-data-source` per §5.1.
3. `docs/sprints/sprint-030-handoff.md` — NEW 12-section handoff per §11.

A bundle in this exact shape is **not scope drift**. Do not flag.

### 3.2 Hard-fence violations (BLOCKING)

`docs/sprint_objective.md` §7 lists 14 hard fences. Any violation is BLOCKING. Specifically check:

1. **No code under `server/` or `eval_interactive/`** (zero Java, zero Python). `git diff --name-only` against the merge base must return nothing under those paths.
2. **No prompt edit** under `server/src/main/resources/prompts/`.
3. **No new CaseSpecs** under `eval_interactive/case_specs/`.
4. **No edits to existing case families** (Sprint 20 / Sprint 29 cascade fence).
5. **No edits to foundational docs** under `docs/foundational/`.
6. **No edits to governance docs** under `docs/current/`. Specifically: `iteration_governance.md` §7.2 worked example MUST NOT be edited in place; the fold-back is a separate cadence per `doc_governance.md`.
7. **No edits to sprint archives** under `docs/sprints/sprint-001-*` through `sprint-029-*`.
8. **No Tier-0 changes** in `docs/runtime_freeze_and_risk_policy.md`.
9. **No deadline / model / retry / budget config edits**.
10. **No editing of `R-prompt-phase-plan-directive-followship`** at `docs/action_bank.md:450`.

### 3.3 Design-doc content checks (BLOCKING substance gates)

The design doc MUST contain:

1. **Premise gap restated** with reproducible code citations. Cross-check ≥ 1 cited file:line — re-grep / re-head, confirm output matches the design doc claim.
2. **At least 3 candidate data sources** evaluated. Each carries: description, code-paths-to-touch, layer classification per §3, anti-hardcode posture (soft vs hard), Sprint 31 size estimate, risks.
3. **One recommendation** with explicit Constitution citation (§1.3 / §1.5 / §1.7). If the recommendation is `human_review_required` per `docs/sprint_objective.md` §5.4, that is acceptable — verify the dev surfaced honestly (no soft-signal claim where a hard branch is actually proposed).
4. **Code-paths-to-touch table** for the chosen option with file:line ranges. Cross-check ≥ 1 row — does the cited file:line exist? does the proposed change type (additive field / new method / new projection branch) actually fit the cited surface?
5. **§7 stanza pre-fill for Sprint 31** — target layer, Tier-0 posture, semantic-hardcode posture, generalization coverage. The pre-fill SHALL be honest about the chosen option's behaviour.
6. **Out-of-scope for Sprint 31** — at minimum names that shadow planner mode is OUT.
7. **Open questions for the human** — every item the design pass could not decide.

### 3.4 Anti-hardcode posture check (BLOCKING)

The design's recommendation MUST satisfy the §1.7 forbidden list:

- The chosen design is a **soft signal** — LLM reads, decides. No Java branch acts on the value. Verify the design doc explicitly states this.
- No keyword / regex / if-else / per-UC matrix in the data-source pipeline. If the chosen design surfaces a "list" by promoting `RuntimeIntentClassifier`'s existing pattern set, the design SHALL discuss this semantic shift explicitly.
- No new Tier-0 invariant proposed.

If the design proposes a hard branch dressed as a soft signal (e.g. "Java reads the alternates and triggers a reroute on cardinality > 0"), that is a `fix_required` blocker. The slot must be observable state the LLM owns, per Constitution §1.3.

### 3.5 Reproducibility (BLOCKING)

Every claim about code behaviour in the design doc cites a source path + extraction recipe. Cross-check ≥ 1 cited code claim per candidate option (so ≥ 3 total): re-run the cited grep / head against the named file path; confirm output matches the design doc claim. Per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

### 3.6 Premise re-verification check (NON-BLOCKING)

Handoff §3 SHOULD re-state each of `docs/sprint_objective.md` §4's five premise items with the session-start re-verification. If skipped but design doc is internally consistent, flag non-blocking.

## 4. Packaging rollforward

The commit may bundle deliver-agent files (`docs/sprint_objective.md`, `compact/sprint-030-*-prompt.md`, `docs/10-handoff.md`) + Sprint 29 archive (`docs/sprints/sprint-029-objective.md`, the existing `docs/sprints/sprint-029-handoff.md`) + pre-existing unrelated working-tree mods.

If the only out-of-scope content is path-based packaging artefacts AND substantive findings close per your own evidence, this is **A-with-packaging-note** territory — `out_of_scope_review` with `blocking_count: 1` on path grounds is acceptable. Classify and report; do not block substance. Per `feedback_out_of_scope_review_packaging_rollforward.md`. Content-based blockers (anything failing §3.2 / §3.3 / §3.4 / §3.5) → `fix_required` instead.

## 5. §4.2 sprint-close header

At the top of the dev's `docs/codex-findings.md` (or wherever Codex publishes — defer to recent precedent at `docs/sprints/sprint-028-codex-review.md` shape):

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

Per-finding write-ups in the body. Mirror `docs/sprints/sprint-028-codex-review.md` shape.

If the dev shipped a `human_review_required` recommendation per §5.4 (no candidate satisfies the soft-signal bar), that is **not** a fail — Sprint 30 explicitly authorizes this exit. Verify the dev surfaced honestly (no hard branch dressed as soft) and approve.

## 6. Deferral rule

Valid-but-out-of-scope concerns (e.g. "the chosen option's Sprint 31 implementation needs a new bot-side endpoint not yet scoped") go to `docs/action_bank.md` as **deferred items**, NOT blocker findings. Do not edit `docs/action_bank.md` yourself — that's the deliver-agent's close-turn action. If the design doc §8 already names the concern, acknowledge; do not list as a finding.

## 7. Final reminders

- Read the commit-range diff start to end; verify the bundle is docs-only.
- One verdict, one header. Substance over ceremony.
- More than 5 findings on a docs-only design doc is over-review; trim to the most load-bearing.
- The §1.7 forbidden list is canonical for what Sprint 30's recommendation must NOT propose. Confirm before `pass`.
- A `human_review_required` recommendation is a **valid** sprint outcome, not a failure. Sprint 30 §5.4 explicitly authorizes it.
