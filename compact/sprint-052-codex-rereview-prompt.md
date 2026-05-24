Paste the content below this line into a fresh Codex session for the **Sprint 52 / M5 S3 RE-REVIEW**, after the human has committed the S3 dev scope + the deliver-agent close-prep (so the `cf9c120..<S3-dev-commit>` range exists and the rerun evidence is present). This supersedes the first-pass review.

**Why a re-review (deliver-agent → human + Codex):** the first-pass review (`docs/codex-findings.md`, `decision: fix_required, blocking_count: 2`) returned a CLEAN code/anti-hardcode axis — Q1-Q7 + Q9 PASS, all §4 hard fences PASS, all §5 C1-matrix claims independently verified, Java `1172/1/0/2` reproduced, golden 4→3 confirmed — and its **two blockers were both close-package TIMING artifacts, not dev-code issues**:

- **P0 (missing real-LLM rerun evidence)** — the review ran BEFORE the deliver-agent recorded the rerun. The evidence is now present (see "What changed" below).
- **P1 (commit range absent; HEAD was `cf9c120`, S3 uncommitted)** — the S3 dev scope is now committed, so the immutable `cf9c120..<S3-dev-commit>` range exists.

This is the same pattern as the M4-Eval-Cleanup close (first pass `fix_required/1` close-package timing → re-review `pass/0`). The re-review's job is to confirm the two timing blockers + the one P3 doc-drift are resolved and to re-affirm the (already clean) code-axis verdict — NOT to re-litigate the nine-question kernel from scratch (it passed).

## What changed since the first pass (verify each)

1. **P0 resolved — real-LLM rerun evidence recorded.** `eval_interactive/case_specs/bad_cases/_manifest.md` now has a **"M5 / S3 (Sprint 52) close real-LLM rerun — sub-sprint regression-safety gate (2026-05-25)"** section, and `docs/sprints/sprint-052-handoff.md` **§12.2** records the rerun. Verify:
   - The distribution reproduces the M4-close baseline: **PASS×5** (cs001, cs014, cs029, cs066, fg5q) + **IMPROVING×4** (alice, cs011, cs012, wmkb) + **FAIL×3** (cs015, cs095, iwzx) + **OOSR×0**.
   - The decisive signal — outcome class — is stable (every establishing case escalates; zero resolve↔escalate flip).
   - fg5q + iwzx hit the intermittent `CONTRACT_VIOLATION` session-establishment flake on the main run (`results/20260524-172654/`) and **cleared on isolated rerun** (iwzx `results/20260524-173652/`, 1×; fg5q `results/20260524-173818/`, on the 3rd try) — the documented `R-bad-case-parallel-session-establishment-flakiness` shape, categorically upstream of `tool_schemas` projection.
   - The semantic-preservation claim for C2 #4's cold-edge legacy `buildProjection(...)` paths is therefore supported: no C2-attributable degradation. Confirm the manifest verdicts are consistent with the cited result paths (spot-check 2-3 cases via `jq` on the run JSON if desired).
2. **P1 resolved — committed range.** `git rev-parse --short HEAD` now returns the S3 dev commit; `git log --oneline cf9c120..HEAD` shows it. Verify the committed S3 scope matches handoff §5 (`ContextProjectionBuilder.java` +59/-13, `resolve_faq_grounded_answer.yaml` -1, `PhaseEvaluatorResolveSkillIntegrationTest.java` golden +1/-2, NEW `Sprint52ProjectionSkillDrivenTest.java`, NEW `docs/diagnostics/m5-s3-projection-consumption-map.md`, the handoff) plus the deliver-agent close-prep docs (the manifest S3 section, the §12 fill, the `runtime_contract.md` P3 reconciliation). The `compact/sprint-052-codex-*review-prompt.md` files are deliver-agent artefacts, not S3 dev-scope code.
3. **P3 resolved — `runtime_contract.md` doc-drift reconciled.** The first pass surfaced a P3 (note-only): `docs/current/runtime_contract.md` still named `moderation_context` in the FAQ RESOLVE `requiredContextKeys`. The deliver-agent reconciled it (the section now records that M5-S3 C2 #3 took **option (b)** — dropped the dangling declaration; `session.moderationContext` stays session-internal / handover-only). Verify the doc now matches the shipped behaviour.

## Re-review scope (confirm + re-affirm; do not re-walk all 9 from scratch)

1. Confirm **P0** is resolved: the rerun evidence is present, the distribution reproduces the M4-close baseline, and the flake cases cleared on isolated rerun. This closes the §3-Q8 / §5 gap that was the substantive root of the first-pass `fix_required`.
2. Confirm **P1** is resolved: the review is now against the immutable `cf9c120..<S3-dev-commit>` range, and the committed scope matches the handoff.
3. Confirm **P3** is resolved: `runtime_contract.md` reflects the C2 #3 strip.
4. Re-affirm the code-axis verdict from the first pass (registry/Skill-driven C2 #4 with a single defensive fallback; C2 #3 declaration-only strip; #2/#5 correctly STOP-surfaced; all §4 fences hold; the C1-matrix claims hold). If you find any NEW substantive issue in the committed range that was not visible in the working-tree review, surface it; otherwise the timing blockers are cleared.
5. Re-affirm the §6 OQ dispositions (OQ-S52.1 #2 → S4; OQ-S52.2 #5 → S4; OQ-S52.3 overlay → closed-with-followup; OQ-S52.4 knowledge_hits → S4/M5+) unless your assessment changed.

## Output

Overwrite `docs/codex-findings.md` (delete-and-add supersession; the deliver-agent archives BOTH passes to `docs/sprints/sprint-052-codex-review.md` at close). Use the §4.2 header:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph — re-review verdict: P0 rerun-evidence resolved (distribution reproduces M4-close; flake cases cleared on isolation), P1 committed-range resolved, P3 doc-drift reconciled; code/anti-hardcode axis re-affirmed clean>
```

Expected verdict: `pass / 0` (the first pass already cleared the code axis; the timing blockers + P3 are resolved). If a genuinely new substantive issue appears in the committed range, return `fix_required` with that finding only. Do NOT edit code; do NOT re-open the STOP-surfaced #2/#5 as PR-level concerns (they were not landed).
