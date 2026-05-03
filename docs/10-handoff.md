# Phase 3 Handoff — Codex Findings Triage and Tier-0 Patch

Date: 2026-05-03
Branch: `design-v1-without-human-review`
Source review: `docs/codex-findings.md`

## 1. Current Status

- Triaged every finding in `docs/codex-findings.md`. Two were tractable in this
  session and were fixed; all other findings were either deferred (server-side
  runtime work) or rejected (informational / process-level).
- Fix 1.3 (source-citation gate too broad) is implemented in `eval_interactive`
  and takes effect immediately on the next eval run.
- Fix 1.6 (routing-prompt UC-A/UC-D mismatch) is committed to
  `routing_prompt.txt`. The change is **not yet active** in the running JVM
  because Spring Boot loads the prompt at `@PostConstruct` and caches it; the
  fix will only apply after the server is rebuilt (`mvn -pl server install`)
  and restarted.
- Smoke pass-rate moved from `0/14 (composite=0.0000)` baseline to
  `1/14 (composite≈0.069)`. The single passing case is `cs_interactive_040`
  (UC-K). The remaining failures are dominated by server-side issues that
  Tier-0 cannot reach: deterministic escalation-reason resolver (1.5),
  handover completeness emission (1.4), runtime case creation (1.8), and
  budget-vs-semantic outcome reasoning (1.9).
- Eval-side runtime no longer false-positives on clarifying / progress /
  greeting turns — the citation gate now fires only on substantive factual
  answers that lack `source_ids`.

## 2. Triage of `docs/codex-findings.md`

Legend: **fixed** = code merged in this session; **deferred** = accepted but
left for a follow-on change because it requires server (Java) work, a
rebuild/restart, or an out-of-scope architectural shift; **rejected** = not
acted on, with reason.

### 2.1 §1 Correctness Bugs

| # | Finding | Decision | Reason |
|---|---|---|---|
| 1.1 | Baseline status misreported (`1/14` vs actual `0/14`) | rejected | Informational. The discrepancy is a documentation drift, not a code defect. The new canonical baseline is recorded below (Section 4) so downstream planning can stop mixing runs. The proposed “summary verifier” is logged as a follow-up, not a Tier-0 fix. |
| 1.2 | `SessionManager.createSession()` does heavy auto-search work | deferred | Requires Java edits in `SessionManager.createSession` to gate the FAQ auto-search behind the first user message, plus a server rebuild/restart. Out-of-scope for the Python-side patch this session. The case that previously errored (`cs_interactive_066` with `session_create_failed`) no longer hits the timeout in the rerun, but the underlying coupling remains. |
| 1.3 | Source-citation gate too broad (rejects clarifying / progress / greeting turns) | **fixed** | `eval_interactive/eval_interactive/scoring/hard_checks.py` now distinguishes substantive factual answers from non-factual turns and only requires citations on the former. See Section 3. |
| 1.4 | Handover completeness missing on escalation paths | deferred | The Python check in `outcome_checks.py:_check_handover_completeness` is correct; the runtime is the bug — `recordHandover` does not run on every escalation path (e.g. clarification-budget exits) and the `handover_payload` for FAQ escalations is sometimes empty. Needs Java change in `SessionManager.recordHandover` and the escalation triggers feeding it. |
| 1.5 | Escalation-reason selection is non-deterministic | deferred | LLM picks the canonical enum today (`request_handover.escalation_reason`). Codex’s recommended server-side resolver is a non-trivial change in `ControlKernel`/`PhaseEvaluator`. Tracked for a Tier-1 patch. |
| 1.6 | Routing prompt mislabels UC-A as account/login | **fixed** | `server/src/main/resources/prompts/routing_prompt.txt` now maps login/password/locked-out/wrong-email signals to UC-D, and adds an explicit UC-A example for ad-visibility/moderation. Effective only after server restart. |
| 1.7 | Hard out-of-scope routing runs before semantic dispute detection (Delivery + dispute → UC-I) | deferred | Java change in `UseCaseRouter` (semantic override before topic-subject hard OOS). Tracked alongside 1.5. |
| 1.8 | Runtime-only case creation is asked of the LLM | deferred | Java change: deterministic `create_case_controlled` from `ControlKernel` after fixed-script intake. Tracked. |
| 1.9 | `turn_budget_exhausted` masks semantic failures | deferred | Java change in the loop’s exit-reason path. Tracked alongside 1.5. |

### 2.2 §2 Missing Eval Cases (2.1 – 2.7)

All seven sub-findings: **deferred**. The smoke set is small (14 cases) and
adding a runtime/contract suite, scoring-hygiene cases, escalation-reason
precedence matrix, routing ambiguity matrix, fixed-script case-creation
cases, tool policy cases, and KB retrieval cases is a sustained authoring
job. Should sequence after Tier-1 server fixes so the new cases exercise
fixed runtime behaviour, not the current bug surface.

### 2.3 §3 Weak Rubric Dimensions (3.1 – 3.7)

All seven: **deferred**. Codex’s recommendations (diagnostic per-bucket
score, citation-quality split, answer-usefulness, handover-quality
sub-dimensions, finer policy compliance, customer-effort, latency
first-class) require both schema and report changes. Out-of-scope for
Tier-0 and best done after Section 1 issues are stabilised so we are
measuring a stable system.

### 2.4 §4 Agent Design Problems (4.1 – 4.6)

All six: **deferred**. These are runtime-architecture statements about
where deterministic logic should live. Each maps to a Java change set:
4.1 (LLM/runtime split) couples to 1.5/1.8/1.9; 4.2 (session lifecycle) to
1.2; 4.3 (UC-registry sync) to 1.6 plus a CI check; 4.4 (Hard OOS as prior
not override) to 1.7; 4.5 (intake-vs-safety precedence) to 1.5; 4.6
(force-answer after retrieval) to 1.9.

### 2.5 §5 Tool-Use Risks (5.1 – 5.5)

All five: **deferred**. Codex recommends mitigations (re-route checks,
cross-UC retrieval probe, runtime-owned side effects, per-phase timeout
budgets, citation-to-claim binding). Each maps to either a server change
or a content-side update (5.5 wants visible article references in user
output, which is a UX/PRD discussion).

### 2.6 §6 Customer Service Policy Gaps (6.1 – 6.7)

All seven: **deferred**. These are policy-authoring tasks for human
escalation availability, payment/refund/dispute boundaries, T&S urgency,
phone/contact privacy, email distinctions, seller payment guidance, and
Delivery exceptions. They depend on PRD owner sign-off and KB updates,
not on the eval harness.

### 2.7 §7 Recommended Minimal Fixes (Fix 0 – Fix 7)

Fix 0 (clean baseline): tracked here in Section 4.
Fix 1 (cheap session creation): deferred (1.2).
Fix 2 (repair scoring gates): partially **fixed** — citation gate now
correctly scoped (1.3); handover-completeness emission remains a runtime
concern (1.4).
Fix 3 (server-side reason resolver): deferred (1.5).
Fix 4 (align routing): partially **fixed** — UC-A/UC-D prompt label
corrected (1.6); semantic Delivery override remains deferred (1.7).
Fix 5 (runtime-owned case creation/handover): deferred (1.8/1.4).
Fix 6 (force answer after retrieval): deferred (1.9/4.6).
Fix 7 (KB content): deferred (post-Tier-1, per Codex).

## 3. Files Changed

1. `eval_interactive/eval_interactive/scoring/hard_checks.py`
   - Added `_NON_FACTUAL_LEAD_PATTERNS` (greeting / acknowledgement /
     progress / clarifying-prompt phrasing) and the helper
     `_is_substantive_factual_answer(turn)`.
   - `_check_source_citation_present` now skips turns that are not
     substantive: a turn is exempt when it neither invoked
     `search_knowledge`/`resolve_article` nor produced a long-form factual
     reply, or when it is a clarifying question / acknowledgement /
     greeting. Substantive factual answers (knowledge tool was called, or
     ≥50 chars and not a single-sentence question) still must carry
     `source_ids`.
   - 70 existing scoring tests still pass
     (`tests/test_hard_checks.py`, `tests/test_composite.py`,
     `tests/test_outcome_checks.py`, `tests/test_composite_gate.py`).

2. `server/src/main/resources/prompts/routing_prompt.txt`
   - “Can’t log in / password reset / locked out / wrong email on account”
     now points to **UC-D (Account & Login)**, matching
     `use-case-registry.yaml`. UC-A is now exemplified by ad-visibility /
     moderation phrasing only.
   - **Not active until the server is rebuilt and restarted.**
   - `server/target/classes/prompts/routing_prompt.txt` carries the same
     update (kept in sync because Maven copied the resource at the same
     mtime).

No other source files were modified. Generated eval results are written
under `eval_interactive/results/20260503-064812/` (first rerun) and
`eval_interactive/results/20260503-065357/` (rerun after the
length-independent clarifier refinement); both are kept for diff.

## 4. Evals Run

Command: `python -m eval_interactive run --set smoke --label smoke-$(date +%Y%m%d-%H%M)`

Reference baseline (per Codex): `results/20260501-215807` —
`0/14`, mean composite `0.0000`, mean outcome `0.6452`,
mean judge `0.7048`, escalation correctness `0.7143`,
mean turns `1.86`. One case errored at session-create
(`cs_interactive_066: session_create_failed`).

Tier-0 rerun #1: `results/20260503-064812` (label `smoke-20260503-1448`) —
`1/14`, mean composite `0.0714`, mean outcome `0.6929`,
mean judge `0.7381`, escalation correctness `0.7857`,
mean turns `1.9`, elapsed 207 s.

Tier-0 rerun #2 (length-independent clarifier refinement):
`results/20260503-065357` (label `smoke-20260503-1453`) —
`1/14`, mean composite `0.0691`, mean outcome `0.5500`,
mean judge `0.6476`, escalation correctness `0.5714`,
mean turns `1.8`, elapsed 191 s. The lower outcome / judge means come
from non-determinism in the bot runtime (different runs route the same
case differently, e.g. `cs_interactive_192` resolved cleanly in run #1
and escalated immediately in run #2). Nothing in the Tier-0 patches
caused those swings.

Pass set across both reruns is `{cs_interactive_040}`. The Tier-0
substrate fix bought one pass that was previously zeroed solely by the
citation-gate false positive.

## 5. Failures Found

Per-case state in rerun #2 (`results/20260503-065357`):

| Case | Expected UC | Result | Dominant remaining blockers |
|---|---|---|---|
| `cs_interactive_001` | UC-C | escalated | escalation_compliance (reason mismatch), handover_completeness missing, UC mismatch — **1.4 / 1.5** |
| `cs_interactive_002` | UC-C | escalated | wrong UC, handover_completeness missing — **1.4 / 1.6** |
| `cs_interactive_004` | UC-D | escalated | wrong outcome (escalated when resolve expected), L3 relevance/tone — **1.6 / 1.7** |
| `cs_interactive_011` | UC-D | escalated | escalation_compliance, handover_completeness, L3 relevance/tone — **1.4 / 1.5** |
| `cs_interactive_014` | UC-C | escalated | escalation_compliance (reason mismatch), handover_completeness missing — **1.4 / 1.5** (citation gate now passes ✓) |
| `cs_interactive_015` | UC-FP | escalated | wrong UC and wrong outcome — **1.6 / 1.7** |
| `cs_interactive_029` | UC-C | escalated | `CONTRACT_VIOLATION:active_use_case` — runtime emitted an invalid UC field; tracker for runtime contract bug |
| `cs_interactive_036` | UC-I | escalated | invalid phase transition, wrong UC, handover gate failed — **1.4 / 1.7** |
| `cs_interactive_038` | UC-J | escalated | escalation_reason mismatch — **1.5** |
| `cs_interactive_040` | UC-K | escalated | **PASS** — composite 0.9667 |
| `cs_interactive_066` | UC-E | escalated | citation gate, escalation_compliance, stall, UC mismatch — **1.2 / 1.5** |
| `cs_interactive_095` | UC-A | escalated | wrong UC and wrong outcome — **1.6** |
| `cs_interactive_192` | UC-B | escalated | wrong outcome (escalated immediately when resolve-from-KB was expected) — **1.4 / 1.9** (runtime variability) |
| `cs_interactive_259` | UC-F | resolved | wrong UC (UC-C), wrong outcome — **1.6** |

Pattern of failures (rerun #2):

- 6 cases blocked on `L1:escalation_compliance` (reason mismatch) — **1.5**
- 6 cases blocked on `L2_GATE_MISSING:handover_completeness` and/or
  `L2_GATE:handover_completeness` — **1.4**
- 7 cases blocked on `L2_GATE:correct_uc` — **1.6 / 1.7**
- 6 cases blocked on `L2_GATE:correct_outcome` — **1.9 / 4.6**
- 1 case showing `CONTRACT_VIOLATION:active_use_case` — runtime data-shape bug
- 1 case showing `STALL:STALL_AFTER_TOOL_INTENT` — runtime never produced
  a follow-up after announcing a tool intent

The citation gate is no longer the dominant L1 blocker — it fires on 0–1
cases per run now, and only on genuinely substantive answers without
sources (e.g. `cs_interactive_066` turn 1 in rerun #2).

## 6. Next Recommended Actions

In priority order, with rationale:

1. **Land Tier-1 server fixes (1.5 → 1.4 → 1.7 → 1.9)** before any KB or
   prompt few-shot work. These are the dominant remaining blockers.
   - 1.5 deterministic escalation-reason resolver in `ControlKernel` (or a
     new resolver service), with precedence: `user_requested` →
     `trust_safety_required` → `payment_dispute_detected` →
     `clarification_budget_exhausted` → `intake_complete_for_uc_*` →
     `turn_budget_exhausted` → `out_of_scope`.
   - 1.4 guarantee `recordHandover` runs on every `ESCALATE` path with a
     populated payload (`session_id`, `primary_use_case`, `summary`,
     `escalation_reason`, `total_bot_turns`).
   - 1.7 add a semantic override step in `UseCaseRouter` so Delivery +
     refund/dispute/scam signals routes to UC-I (or UC-J) before the hard
     OOS branch.
   - 1.9 stop tagging budget-exhaustion when a more specific reason
     applies; reserve `turn_budget_exhausted` for true loop exhaustion.
2. **Restart the server** (`mvn -pl server install -DskipTests` then
   restart Spring Boot) so the routing-prompt fix from §3 actually
   reaches the JVM. Re-run smoke immediately afterward to measure the
   isolated UC-A/UC-D effect.
3. **Investigate the runtime non-determinism** seen between rerun #1 and
   rerun #2 (cases 001 / 015 / 192 swung). Likely sources: temperature
   on routing/chat (eval_interactive.yaml `llm.temperature: 0.0` is good,
   but the server may use a different value internally), or an
   uncontrolled fallback path. Pin both the routing and chat
   temperatures to 0 server-side for reproducible smoke runs.
4. **Add eval cases for the citation-gate refinement** so future
   regressions are caught locally:
   - clarifying long question without sources should pass
   - acknowledgement / progress message without sources should pass
   - factual answer without sources should fail
   - factual answer with sources should pass
5. **Defer KB content updates and prompt few-shots** until the runtime
   stabilises — Codex’s point that they would produce mixed signal is
   confirmed by the variance between reruns #1 and #2.
6. **Track 1.2 (cheap session creation)** explicitly. Even though
   `cs_interactive_066` did not error in this rerun, the coupling is
   load-sensitive and will reappear in larger runs.

When 1.5, 1.4, 1.7, and 1.9 land, expect the smoke pass rate to move into
the `5–6/14` band Codex projected for Tier-0 + Tier-1 combined. Beyond
that requires KB / prompt work (Codex Tier-3 / Tier-4).
