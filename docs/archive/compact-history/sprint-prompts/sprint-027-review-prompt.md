Paste the content below this line into a fresh Codex session after the dev's commit lands. OR the human MAY skip Codex review per the docs-only §4.1 exemption + Sprint 26 precedent (`feedback_close_with_codex_skipped_docs_only_outcome.md`). The human chooses at close.

---

You are the **Sprint 27 review agent (Codex)**. Sprint 27 is a
docs-only investigation / probe sprint on
`R-prompt-phase-plan-directive-followship` (`docs/action_bank.md:450`).
The dev agent produced an evidence table + a recommendation drawn from
(R1) / (R2) / (R3). The recommendation IS the primary deliverable; no
slot was shipped.

## 1. Load order

1. `AGENTS.md` (loads `iteration_governance.md` §1 §3 §4 §5 §7 +
   `doc_governance.md` + `agent_context_guide.md`).
2. `docs/sprint_objective.md` (Sprint 27 contract — note §7 EXEMPT
   stanza, fourteen hard fences, §1.7 hard gate, reproducibility
   rule).
3. `docs/sprints/sprint-027-handoff.md` (deliverable).
4. Commit range: `git log --oneline 1f4a1db..HEAD` (Sprint 26 close =
   `1f4a1db`); `git diff 1f4a1db..HEAD`.

## 2. §4.1 kernel

Run the §4.1 kernel verbatim as loaded from
`docs/current/iteration_governance.md` §4.1. Sprint 27 is **EXEMPT**
under the §4.1 first-paragraph exemption clause IF AND ONLY IF the
diff is genuinely docs-only.

**If genuinely docs-only** (only `docs/sprints/sprint-027-handoff.md`
+ `docs/action_bank.md` line 450 + possibly deliver-agent files per
§4), verdict = `approve (exemption: docs-only — Sprint 15 / 16 / 17 /
18 / 22 / 26 precedent)`. Sprint 22 is the direct precedent. Run §3
substantive checks regardless.

**If the diff touches any source-tree file** (`.java` / `.py` /
`.ts` / `.tsx` / `.yml` / `.yaml` / `.properties` under `server/` /
`ui/` / `eval_interactive/eval_interactive/`), exemption does NOT
apply — scope violation per §3.

## 3. Sprint-27-specific substantive checks (run regardless)

- **No source-tree edits.** `git diff 1f4a1db..HEAD --name-only` lists
  only docs. Any non-docs file under `server/` / `ui/` /
  `eval_interactive/eval_interactive/` = BLOCKING.
- **Reproducibility (HARD).** Every `observed_triggers` /
  `observed_non_fulfillments` cell cites BOTH source path AND
  extraction method (`jq` with literal output, `python3 -c` with
  literal result, or "manual eyeball over `<path>`, n=K cases"). A
  cell without both = BLOCKING per
  `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` +
  Sprint 25 fix precedent `c8b8c85`.
- **Rubric uniform.** Every directive across `PhaseEvaluator.java`
  lines 411 / 458 / 485 / 517 / 564 / 616 has Q1 / Q2 / Q3 / Q4
  recorded. Missing any = BLOCKING.
- **§1.7 respected.** Evidence-gathering does NOT match user / bot
  text against keyword / regex / content patterns. Detection by
  event-shape signals only (`llm_calls`, `failure_tags`, phase
  transitions, transcript metadata). Content matching = BLOCKING.
- **Recommendation grounded.** Handoff lands on exactly one of
  (R1) / (R2) / (R3); justification matches the table's count +
  classification distribution. Contradicting the table = BLOCKING.
  Hedged between outcomes = BLOCKING.
- **No slot / `PhasePlan` restructure / `DirectiveSpec` class.**
  Code-level slot design beyond a sentence-level sketch = BLOCKING.
- **R-item updated, NOT closed.** `docs/action_bank.md:450` has probe
  finding + recommendation appended; R-item not marked closed.
- **(R2) / (R3) are valid outcomes.** Do NOT flag absence of code
  change as scope-incomplete; both are named in the contract.

## 4. Packaging-rollforward rule

If your only blocking finding is path-based (deliver-agent-owned files
like `docs/sprint_objective.md`, `compact/sprint-027-*.md`,
`compact/sprint-deliver-orchestrator.md` bundled into the dev's
commit) AND substantive findings closed, classify substantively +
note the packaging artefact. Human classification = A-with-packaging-
note (Sprint 20 precedent). Do NOT split into a separate re-review.

## 5. Sprint-close header (write to `docs/codex-findings.md`)

Per `iteration_governance.md` §4.2:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

List findings (blocking + non-blocking) below with file paths + line
cites; name the §3 fix-layer for each. Do not propose code fixes
beyond naming the layer.

Pathways: §3 checks pass + docs-only → `pass`, `blocking_count: 0`.
Substantive findings close but deliver-agent files bundled →
`out_of_scope_review`, `blocking_count: 1` (packaging-only).
Substantive failure (irreproducible count, §1.7 violation, rubric
incomplete, source-tree edit) → `fix_required`.

## 6. Deferral-to-action_bank rule

Out-of-scope concerns the diff surfaces (issues outside the fourteen
Sprint 27 fences but not in the dev's handoff) → **non-blocking**
findings with a recommendation that the deliver agent / human append
them to `docs/action_bank.md` §5.2 as deferred R-items. Do NOT
escalate to BLOCKING.

## 7. Codex MAY be skipped at human discretion

Per Sprint 26 precedent
(`feedback_close_with_codex_skipped_docs_only_outcome.md`), the human
MAY apply the §4.1 exemption directly at close without dispatching
Codex, IF AND ONLY IF the close commit is purely docs. This prompt is
provided so Codex CAN run; the decision belongs to the human. If
you're reading this, you're running — apply §3 checks and write the
sprint-close header.

## 8. Verdict set (§4.1)

- `approve` — Sprint 27 is genuinely docs-only and all §3 checks
  pass; note the docs-only exemption.
- `approve with downgrade-to-signal follow-up` — unlikely on a
  docs-only sprint; double-check whether the dev shipped a slot.
- `reject as semantic hardcode` — the classification method itself
  encoded a semantic hardcode (e.g. content matching). §1.7 violation.
- `needs human architecture decision` — directive-shape fragmentation
  is itself an architecture boundary issue the human must adjudicate
  before a structural sprint runs.
