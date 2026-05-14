# Current Handoff

Date: 2026-05-14
Branch: `design-v1-without-human-review`

## 1. Current phase

Current phase:
Sprint 24 (Slow-LLM Placeholder Coalesce + Coarse Latency Proxy —
two-track, semantic-touching on Track A; investigation-only on
Track B) closed clean on 2026-05-14 with Codex `decision: pass,
blocking_count: 0` on the first review pass (no fix iteration
required). Classification: **A — Clean close**. UX-over-pass-rate
framing per the human's 2026-05-14 reframe. Sprint 24 paired off
Sprint 23 §11: Track A is the deterministic UX repair on the
cross-turn slow-LLM placeholder loop surface; Track B is the
honest coarse-proxy latency baseline with explicit no-per-call-claim
and a follow-on instrumentation R-item proposed. The single dev
commit (`e21b1b6`, "sprint 24 track A: cross-turn slow-LLM
placeholder coalesce + honest next-step") ships the Track A bundle
verbatim per the objective: new `consecutive_deadline_count`
`@Column` + `@Builder.Default = 0` `Integer` field on `BotSession`
(lines 79–81, mirroring V12 `runtime_error_count` shape);
`SessionManager` builder init `.consecutiveDeadlineCount(0)` at
line 108 (parallel to the `.runtimeErrorCount(0)` precedent);
`PhaseEvaluator` reset hook in the outcome-dispatch prologue
(lines 684–693, mirroring the existing ERROR reset with
`DEADLINE_EXCEEDED` substituted for `ERROR`; resets only when
session is non-null, outcome is non-deadline, and the counter is
non-null and > 0); split `DEADLINE_EXCEEDED` / `LLM_UNAVAILABLE`
case-block (`DEADLINE_EXCEEDED` increments the counter and
threshold-gates between the existing placeholder *"Sorry, I'm a
bit slow right now. Please try sending that again in a moment."*
on the first consecutive deadline and a distinct honest next-step
message *"I'm still having trouble responding in time. If you'd
like, I can connect you with a specialist, or you can try again
in a few minutes."* on the second; `LLM_UNAVAILABLE` branch
preserved with its existing message and `agent_llm_unavailable`
transition tag); single Flyway V13 migration
(`V13__add_consecutive_deadline_count.sql`) adding the column to
`bot_sessions`; behaviour-level regression suite
`Sprint24DeadlinePlaceholderCoalesceTest` (3 methods asserting
placeholder on the 1st consecutive deadline, distinct-next-step
intent token + no auto-handover on the 2nd, and reset-to-placeholder
after a non-deadline outcome between two deadlines). Both branches
keep the transition tag `agent_deadline_exceeded`; neither
auto-calls `request_handover` (the 2nd-deadline message offers a
handover but the user owns the choice). The trigger is the
*event-shape* count of consecutive `DEADLINE_EXCEEDED` outcomes,
NOT a keyword / regex / if-else / enum on user content; the
runtime owns timeout fallback emission deterministically (§1.4),
no soft signal is projected to the LLM, no semantic hardcode is
introduced. Track A's regression test passed (`mvn -pl server
-Dtest=Sprint24DeadlinePlaceholderCoalesceTest test`: 3 tests, 0
failures); full server suite ran 901 tests with 1 failure
(`SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`),
which is **pre-existing** and tied to the working-tree uncommitted
edit on `server/src/main/resources/prompts/system_prompt.txt`
(documented in Sprint 23 §9 with the same failure shape) — not
authored by the Sprint 24 dev agent and not part of commit
`e21b1b6`.

Track B is investigation-only and lives entirely in the handoff.
The dev session's coarse-proxy table extracted case-level
`elapsed_ms` and `total_turns` from `case_results[]` in the two
`results.json` files Sprint 19 §3.7 / Sprint 23 §4.2 named: pre
`20260505-235231` n_cases=14 sum_turns=32 p50≈19.9s p95≈30.5s;
post `20260510-134558` n_cases=14 sum_turns=42 p50≈41.1s
p95≈92.3s — direction matches the planning-turn citation
(post-`f2d4cb2` p95 widened), magnitude diverges substantially
from the planning-turn numbers carried into the Sprint 24
objective §2 (pre p50≈10.4s / p95≈24.6s; post p50≈10.5s /
p95≈27.6s; n="105 + 27 case-turns"). The "105 + 27 case-turns" n
value could not be reconstructed by the dev session from any
reasonable extraction over `case_results[]` (case count 14+14,
transcript entries 66+82, sum_turns 32+42, bot turns 34+40). The
dev session reports both views honestly per the §6.2 fence #6
("do not represent coarse proxy data as per-call latency
evidence"; the fence cuts in both directions — overstating AND
understating the divergence both violate the spirit of the
fence). The §4.3 no-per-call-claim paragraph holds: per-LLM-call
latency is NOT in `eval_interactive/results/*/results.json` at
the per-turn granularity needed to verify the Sprint 19 §3.7
hypothesis directly; case-level `elapsed_ms` conflates LLM
round-trip + tool dispatch + persistence + retry overhead; the
coarse proxy is a *signal*, not *evidence*. The §10 open
question 1 surfaces the magnitude discrepancy as an **open
methodology question for the follow-on
`R-per-llm-call-latency-instrumentation` sprint**, which is the
prerequisite to any future deadline-budget widening or
model-revert decision. The Track B *conclusion* (per-call
instrumentation is the prerequisite) is robust to the magnitude
question — only the pre-instrumentation magnitude estimate
depends on it.

The Codex sprint-close review at
`docs/sprints/sprint-024-codex-review.md` returned `decision:
pass, blocking_count: 0` on the first pass with the §4.1 per-PR
Anti-Hardcode verdict `approve` (all 9 questions answered: no
keyword/regex/if-else/per-UC matrix, no Tier-0 invariant added,
no LLM ownership shift, no soft-signal substitution path, no
visible-eval encoding, no prompt edit, tool schema /
capability / PII / grounding preserved, generalization coverage
accepted under the Sprint 24 deviation with shadow deferred to
G2, not temporary so no sunset plan needed). Codex's 8 Sprint
24 checks all pass: Track A bundle present at cited line
numbers; 2nd-deadline message names the issue and offers next
steps; regression test asserts first placeholder + second
distinct/non-empty/next-step intent + no auto-handover + reset
behaviour; Track B shipped no runtime/eval/config code; the
coarse-proxy table cites the objective's `24.6s -> 27.6s` p95
signal; case-level `elapsed_ms` is signal not per-call
evidence; the proposed `R-per-llm-call-latency-instrumentation`
carries the prerequisite-flag; action-bank disposition is
appropriate for Sprint 24 close; hard fences hold in the
reviewed commit range (no deadline-budget widening, no model
config change, no prompt projection work, no eval-spec work,
no Tier-0 change, no coarse proxy represented as per-call
evidence); out-of-scope surfaces (`ChatController.java:125`,
`cs_040` UC-K routing) are untouched. Codex's two informational
observations (non-blocking): (1) Track B magnitude discrepancy
between planning-turn citation and dev-session independent
extraction is correctly carried as a human open question for
the follow-on instrumentation R-item, not a blocking Track B
claim; (2) working-tree uncommitted files
(`docs/sprint_objective.md`, `compact/sprint-024-*.md`,
`csagent_system_design_review.md`,
`server/src/main/resources/prompts/system_prompt.txt`) are
deliver-agent-owned or pre-existing and are not scope drift
under the packaging-rollforward rule
(`feedback_out_of_scope_review_packaging_rollforward.md`).

Files committed in the dev's single commit (`e21b1b6`):

- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
  — new `consecutive_deadline_count` `@Column` +
  `@Builder.Default = 0` `Integer` field at lines 79–81.
- `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java`
  — `.consecutiveDeadlineCount(0)` builder init at line 108.
- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
  — reset hook lines 684–693; split DEADLINE_EXCEEDED branch
  lines 765–803 (counter increment + threshold-gated message);
  LLM_UNAVAILABLE branch lines 804–817 preserved unchanged.
- `server/src/main/resources/db/migration/V13__add_consecutive_deadline_count.sql`
  — new Flyway migration mirroring V12 `runtime_error_count`
  shape.
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint24DeadlinePlaceholderCoalesceTest.java`
  — new behaviour-level regression suite (3 test methods).
- `docs/sprints/sprint-024-handoff.md` — 12-section sprint
  handoff (§12 verdict block filled at close per
  `feedback_handoff_verdict_section_delegation.md`).

Files added at close commit (deliver-agent close-out, not the
dev's commit):

- `docs/sprints/sprint-024-objective.md` — archive copy of the
  running `docs/sprint_objective.md` (deliver-agent-owned;
  untracked at session start; `mv` + `git add` per
  `feedback_git_mv_uses_head_content.md`; running file removed).
- `docs/sprints/sprint-024-codex-review.md` — archive copy of
  the running `docs/codex-findings.md` (untracked at session
  start; `cp` + `git add` archive + `rm` original per
  `feedback_packaging_codex_findings_supersession.md`).
- `docs/10-handoff.md` (this file) — updated lead to Sprint 24
  close; Sprint 23 demoted to "Preceding sprint".
- `docs/action_bank.md` — Sprint 24 closed row added to §6;
  `R-slow-llm-placeholder-coalesce-honest-next-step` disposition
  updated to "implemented (Sprint 24 Track A)" with the §11
  disposition note; `R-llm-latency-budget-investigation`
  disposition updated to "reframed-as-coarse-proxy +
  per-call-instrumentation deferred to follow-on R-item";
  NEW row `R-per-llm-call-latency-instrumentation` added to
  §5.2 with the prerequisite-flag and disposition `proposed
  (Sprint 24 §4.5; investigation-only this sprint;
  instrumentation is a future sprint)`;
  `R-cs040-uc-k-topic-subject-routing` unchanged.

This sprint is `docs/current/iteration_governance.md` §7
stanza-**REQUIRED** (semantic-touching, two-track) and the
multi-layer prospective per-track stanza is in the archived
`docs/sprints/sprint-024-objective.md` §7 (Track A target failure
layer = `infra`; Track B target failure layer = `infra`
diagnostic). Generalization-coverage table per §5.1 in handoff
§8 (target / neighbor / negative / shadow; shadow deferred to
G2 per the objective). The §4.1 Anti-Hardcode review verdict is
`approve` per both the dev's §7 self-walk in handoff §7.1 and
Codex's per-PR verdict in
`docs/sprints/sprint-024-codex-review.md` §"4.1 Anti-Hardcode
Kernel". Five Sprint 24 §10 open questions are recorded in the
archived handoff: (1) Track B coarse-proxy magnitude divergence
methodology — the planning-turn `≈3-second p95 widening` vs the
dev-session `≈62-second p95 widening` should be reconciled by
the follow-on `R-per-llm-call-latency-instrumentation` sprint
which will name the comparison ground-truth method; (2)
`LLM_UNAVAILABLE` coalesce deferral as a paired R-item (n=0
evidence today, conditional opening per the
controlled-multi-shape-testing bar); (3)
`ChatController.java:125` legacy emission audit (n-ladder
evidence — defensive R-item or fully covered by Track A?).

The natural follow-on sprint per Sprint 24 handoff §12 is
`R-per-llm-call-latency-instrumentation` (instrumentation
sprint that produces real per-LLM-call latency data and the
methodology to compare pre/post `f2d4cb2`). Per Sprint 24 §10
question 1, the dev agent on that sprint should reconcile the
§4.4 magnitude divergence with the human (which methodology
anchors the comparison ground-truth). Alternative candidates
on the action_bank §5.2 backlog include:
`R-cs040-uc-k-topic-subject-routing` (Sprint 22-surfaced;
Sprint 23 carried forward; Sprint 24 fence held);
`R-handover-orchestrator-write-side` (Sprint 16 design freeze;
`docs/release_gate.md` §1.1 P1-pre-cutover blocker);
`R-prompt-phase-plan-directive-followship` (3-instance promoted;
addresses bot off-policy answers);
`R-uc-cdf-get-customer-context-bot-actual-usage` (Sprint 22
surfaced behavioural question);
`R-uc-k-intake-complete-case-id-binding` (Sprint 19 §3.6); and
Sprint 24 handoff §10 open questions 2 (`LLM_UNAVAILABLE`
coalesce paired R-item) and 3 (`ChatController.java:125`
defensive R-item).

Preceding sprint:
Sprint 23 (repeated FAQ calls + LLM stall root-cause investigation —
two-track investigation+bundle, semantic-touching) closed on
2026-05-14 after one fix iteration on the **PASS branch**. Sprint 23
opened on 2026-05-14 with a two-track scope: Track A bundles a narrow
`prompt_projection` fix (`R-already-called-prompt-consumption`),
Track B remains investigation-only on the strict bundle gate ("ONLY
if root cause is purely user-facing repeated fallback messaging").
UX-over-pass-rate framing per the human's reframe of 2026-05-14.
**Final outcome: Track A bundle landed with target-reversal evidence;
Track B investigation-only with the narrow UX-repair shape proposed
as a follow-on R-item.** The parent dev commit (`39cb1b9`,
"sprint 23: teach LLM about already_called slot so it stops
re-emitting identical FAQ searches") landed the principled teaching
paragraph in `server/src/main/resources/prompts/system_prompt.txt`
between the Rules section and the DISCOVER phase guidance; the
paragraph names the Sprint 20 `already_called` projection slot,
describes its content in observable terms (`arguments_hash`,
`at_step`, cross-reference to `accumulated_tool_results`), tells
the LLM to read the prior payload from `accumulated_tool_results`
when its planned call matches a slot entry, and leaves the
re-emit decision to the LLM as a soft signal ("you own the
judgement"; "the slot does not block dispatch"). The teaching is
principled — no `search_knowledge` / `resolve_article` / UC-A
through UC-K / UC-FP branching, asserted by the new regression
test
`AlreadyCalledPromptConsumptionTest.systemPrompt_teaching_doesNotBranchOnToolNameOrUseCase`.

The parent dev's first-pass Codex review returned `decision:
fix_required, blocking_count: 3` against three Findings: (1)
missing root-cause matrix columns (six observable fields per
target case were demanded — turn, raw LLM tool calls, dispatched
calls, projection `already_called` contents,
`accumulated_tool_results` contents, argument-hash / same-args
status), (2) inferred-vs-conclusive evidence on the Track A
bundle, and (3) regression evidence not reversing the target
shape. The human authored a strict-evidence-gate fix-iteration
objective: PASS branch required real-LLM target rerun showing
shape reversal on ≥1 Track A target with explicit forbid of
mocked-LLM-as-primary-evidence; DOWNGRADE branch required prompt
revert + test delete + R-item re-open. The fix dev (commit
`19ce2ae`, "sprint 23 fix: PASS — augment matrices, target rerun,
landed with target-reversal evidence") took the PASS branch: it
augmented both root-cause matrices with the six observable columns
(`unavailable: <cause>` cells named the source-of-truth gaps in
the 2026-05-10 `results.json` snapshot), ran a real-LLM target
rerun of `cs_interactive_040` against the post-`39cb1b9` teaching
prompt under the harness's normal configuration (DashScope
`qwen-plus`), and produced
`eval_interactive/results/20260514-080835/results.json`. The
rerun shows the duplicate-`search_knowledge` shape reversed: the
session-level tool sequence is now
`['classify_use_case', 'search_knowledge', 'resolve_article', 'record_outcome']`
(1 `search_knowledge`, no back-to-back duplicates) vs the
original 2026-05-10 sequence
`['search_knowledge', 'classify_use_case', 'search_knowledge', 'resolve_article', 'record_outcome', 'search_knowledge']`
(3 `search_knowledge`, with turn 1's `[sk, sk]` back-to-back as
the parent target). A second rerun of `cs_interactive_014`
(`eval_interactive/results/20260514-081022/results.json`) is
honestly reported in the fix iteration's §4 as **partial**: the
case passed at composite 0.871 via handover (`stop=bot_ended`,
`request_handover`), but the underlying 4-consecutive
`search_knowledge` shape persists — the teaching helps the LLM
exit the duplicate-call dead-end rather than fully suppress the
duplicate emission on this case. The fix-iteration evidence gate
required reversal on ≥1 target; cs_040 satisfies it. cs_014's
partial result motivates the conditional follow-on
`R-accumulated-tool-results-prompt-consumption` (§11). The fix
re-review at `docs/sprints/sprint-023-fix-codex-review.md`
returned `decision: pass, blocking_count: 0`: Finding 1 closes
on the augmented matrices, Findings 2 and 3 close on the real-LLM
target rerun + the test's supporting-coverage relabelling. The
§4.1 anti-hardcode verdict is `approve`; the PASS action_bank
disposition phrase `landed with target-reversal evidence` is
verified.

Track B (placeholder / stall) remained investigation-only. The
proximate cause is conclusive (PhaseEvaluator lines 754–770 emit
identical text on every `DEADLINE_EXCEEDED` outcome without
session-scope state-tracking; consecutive deadlines therefore
produce back-to-back identical bot messages, which the runtime
loop-detector flags). The deeper cause (model latency / timeout
config — Sprint 19 §3.7 hypothesis) is **unverified** because
per-turn LLM latency data is not in the results.json snapshot.
The dev read the strict bundle gate and deferred. The narrow
Track B fix shape (session-scope `consecutiveDeadlineCount` +
honest next-step on second consecutive deadline; mirrors V12
`runtime_error_count`) is proposed as the new R-item
`R-slow-llm-placeholder-coalesce-honest-next-step` with a
paired `R-llm-latency-budget-investigation` diagnostic R-item.
**No deadline-budget widening; no model config change; no
Tier-0 invariant; no eval-spec edit; no semantic hardcode.**
Server suite green: 898 / 0 / 0 / 1 (Sprint 20 baseline was
894 / 0 / 0 / 1; Sprints 21 / 22 added tests bringing the
pre-Sprint-23 baseline to 896; this sprint's +2 are both in the
new `AlreadyCalledPromptConsumptionTest` class). The single
`SystemPromptUserRequestedTiebreakerTest` failure observed
locally is entirely attributable to a pre-existing uncommitted
working-tree mod (the cosmetic `ACTIVE-UC TIEBREAKER (Sprint 6
§G1)` header rename) that is NOT part of either commit `39cb1b9`
or `19ce2ae`; the deliver agent surfaces it here for visibility
but it is unrelated to Sprint 23. Files committed across the two
Sprint 23 commits:

- `server/src/main/resources/prompts/system_prompt.txt` (parent
  commit `39cb1b9`) — new principled teaching paragraph for the
  `already_called` slot. Inserted between the Rules section and
  the DISCOVER phase guidance; 8 lines total. Names the slot,
  describes its three fields (`arguments_hash`, `at_step`,
  implicit `tool`), cross-references `accumulated_tool_results`,
  specifies the reuse-the-prior-payload action when the planned
  call matches a slot entry, leaves the re-emit decision to the
  LLM (soft signal), and specifies the empty-array semantics.
- `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledPromptConsumptionTest.java`
  (parent commit `39cb1b9`) — 2 unit tests asserting teaching
  presence + principled-shape anchors and the anti-hardcode
  1400-character window. Both pass. **Relabelled as supporting
  coverage** in the fix commit `19ce2ae` via a 6-line top-of-file
  comment pointing at the cs_040 target rerun as primary
  evidence. No logic change.
- `docs/sprints/sprint-023-handoff.md` (parent commit `39cb1b9`
  +1118 lines; fix commit `19ce2ae` +328 lines for the augmented
  matrices in §3.2 / §4.2 + the new `## Fix iteration` section
  with rerun command / results path / per-target reversal verdict
  / §11 / §13.1 updates from PARTIAL → PASS / action_bank
  disposition phrasing / mocked-LLM supporting-coverage note /
  anti-hardcode self-walk + the §12 verdict block filled at
  close).
- `docs/sprints/sprint-023-objective.md` (close commit; copied
  from the running `docs/sprint_objective.md` which carried the
  parent objective + the strict-evidence-gate fix-iteration
  append; running file removed).
- `docs/sprints/sprint-023-fix-codex-review.md` (close commit;
  copied from the running `docs/codex-findings.md` which carried
  the fix re-review `decision: pass, blocking_count: 0`; running
  file removed). The parent first-pass Codex review (`decision:
  fix_required, blocking_count: 3`) was not separately archived
  because it lived only as untracked content at the running
  `docs/codex-findings.md` between the two commits; its Findings
  text is quoted in the fix-iteration handoff section and in this
  file's archive at `docs/sprints/sprint-023-fix-codex-review.md`
  closure_verdict rows.
- `docs/10-handoff.md` (this file) — updated lead to Sprint 23
  close + PASS branch resolution; Sprint 22 demoted to "Preceding
  sprint".
- `docs/action_bank.md` (close commit) — Sprint 23 closed row
  added to §6; `R-already-called-prompt-consumption`
  disposition updated to "landed with target-reversal evidence"
  per the parent and fix-iteration objective's hard phrasing
  constraint; four new R-items added to §5.2
  (`R-slow-llm-placeholder-coalesce-honest-next-step`,
  `R-llm-latency-budget-investigation`,
  `R-cs040-uc-k-topic-subject-routing`,
  `R-accumulated-tool-results-prompt-consumption`).

Six deferred-by-design items from `docs/action_bank.md` §5.2 were
explicitly named as out-of-Sprint-23-scope per the §9 hard fence.
Two open questions for the human are recorded in the archived
handoff §10: (1) Track B's narrow UX-repair ship cadence (next
sprint, in parallel with latency-data collection, or wait for
latency data first); (2) `R-prompt-phase-plan-directive-followship`
n-ladder threshold readiness (n=2 ladder strengthened by Sprint 23
manual-probe walk, still below the user's controlled-multi-shape-
testing bar).

This sprint is `docs/current/iteration_governance.md` §7
stanza-REQUIRED (semantic-touching, two-track) and the multi-layer
prospective stanza is in the archived
`docs/sprints/sprint-023-objective.md` §11. Generalization-coverage
table per §5.1 in handoff §8 (target / neighbor / negative /
shadow). The §4.1 Anti-Hardcode review verdict is `approve` per
both the dev's §7 self-walk and Codex's fix re-review non-blocking
note (Track A is the canonical Sprint 19 §4.2 Layer 1 soft-signal-
plus-teaching shape; Track B is investigation-only → no per-PR
verdict surface). The mocked-LLM hard fence (the deliver agent's
strict-evidence-gate directive: "a mocked LLM cannot reliably
prove the prompt change causes the LLM to behave differently
unless it simply bakes in the desired behavior") was honored: the
primary causal evidence for Findings 2 + 3 closure is the cs_040
real-LLM target rerun, not a mock; no new mocked-LLM integration
test was written in the fix iteration.

Preceding sprint:
Sprint 22 (phase 2 line 358 reconciliation + R-item closure) closed
on 2026-05-14 as a narrow docs-only scope-correction sprint. A
Sprint 22 planning-turn premise-verification check discovered that
the policy-mismatch premise Sprint 21 carried forward — that phase
2 forbids `get_customer_context` for UC-C / UC-D / UC-F — was based
on a misread of UC-H-local prose inside the UC-H-01 YAML block
(phase 2 line 358) as the cross-UC rule. The normative cross-UC
matrix at phase 2 §2.10.1 line 1098 already permits the tool for
UC-C, UC-D, and UC-F (corroborated by
`docs/customer_service_tool_spec_v0_2.yaml` line 260 and
`docs/current/customer_service_tool_spec_v0_3.md` line 60). Sprint
22 reconciled the misleading prose annotation in phase 2 (now
defers to §2.10.1 for the cross-UC question; added minimal YAML
front matter per `docs/current/doc_governance.md`), closed
`R-generator-get-customer-context-policy-mismatch` (with a Sprint
22 correction addendum on the Sprint 21 `status: done` text) and
the routed-to `R-phase2-uc-cdf-customer-context-policy-widen` (as
premise-invalidated), appended notes-style correction blocks to the
three affected Failure Briefs (cs_001 / cs_011 / cs_259 — no
rewrite, original brief content preserved), and opened a single
follow-on R-item `R-uc-cdf-get-customer-context-bot-actual-usage`
(layer hint `prompt_projection` or `semantic_planner`, NOT
`product_policy`; disposition `proposed / deferred`) capturing the
residual behavioural question: does the bot actually call the tool
when account-state matters in UC-C / UC-D / UC-F? No runtime
change, no policy widening, no CaseSpec / override / judge / prompt
/ FAQ corpus / case-family edits, no Tier-0 candidacy. The Sprint
22 handoff at `docs/sprints/sprint-022-handoff.md` records the
seven-directive scope, the closed / opened R-items, and a rescoping
of Sprint 21 §12.4's recommended-next-sprint suggestion (the
widening sprint is no longer needed; Sprint 22's §10 names
`R-case-spec-overrides-schema-scoring-extension` as the narrow
high-leverage candidate, with Sprint 19 Track B Handover
Orchestrator and the new behavioural R-item as alternatives).

This sprint is `docs/current/iteration_governance.md` §7
stanza-**EXEMPT** under the docs-only / config-governance exemption
(precedent: Sprint 15, Sprint 16, Sprint 17, Sprint 18). No stanza
is required and none was written. No semantic surface was touched,
so the §4.1 Anti-Hardcode review verdict is `approve` with the
exemption named.

Preceding sprint:
Sprint 21 (Wave A5/A6 L3 Review Batch — per-case + 1 systematic)
closed on 2026-05-14 as a single-track semantic-touching sprint on
the eval_spec surface. Sprint 21 processed the seven L3 R-items
named in Sprint 20 §12 / `docs/action_bank.md` §5.2 Wave A5/A6
backlog and delivered an L3 disposition for each — one of three
classes: approved override (`eval_spec` adjustment landed in
`eval_interactive/case_spec_overrides.yaml`), rejected as
non-eval_spec (layer reclassified + new R-item proposed), or
deferred with reason (named dependency that must lift first). The
sprint is paper-only on the eval_spec surface: no runtime change,
no prompt change, no FAQ corpus change, no judge code change, no
edit to any Sprint 20-authored case-family content (the cascade
rule was non-negotiable per the sprint objective). Three deliverable
surfaces changed:

- `eval_interactive/case_spec_overrides.yaml` — three new approved
  override entries added (cs_001 escalation_trigger flip from
  `clarification_budget_exhausted` to `faq_miss_threshold_exceeded`
  with matching `bot_handling_pattern` rewrite; cs_192
  `secondary_ucs` dedupe from `[UC-K, UC-D, UC-B]` to `[UC-K, UC-D]`;
  cs_095 classification flip from UC-A primary `[UC-D, UC-K]`
  secondary to UC-D primary `[UC-A, UC-K]` secondary — supersedes
  the Wave A2.1 legacy migration entry, which is removed from the
  legacy block + header comment updated from "9 remaining" to
  "8 remaining (plus cs_interactive_095 superseded by Sprint 21
  below)"). Verified by the actual loader at
  `eval_interactive/eval_interactive/case_spec/extractor.py:_load_case_spec_overrides`:
  17 applied entries (was 15), 0 pending, 0 duplicate session ids.
  Disposition summary: 3 approved (cs_001, cs_095, cs_192), 2
  rejected (cs_176 → `semantic_planner`, systematic
  `R-generator-get-customer-context-policy-mismatch` →
  `product_policy`), 2 deferred (cs_038 + cs_040 blocked on the
  override schema not supporting `scoring.*` overrides — both
  routed to the new `R-case-spec-overrides-schema-scoring-extension`
  R-item).
- `docs/sprints/sprint-021-handoff.md` — NEW. The 12-section sprint
  handoff with the Context Pack, sprint-objective recap, per-R-item
  disposition write-ups with §1.7 self-check evidence for every
  approved override, YAML deltas table, cascade-rule observance
  evidence, files-changed list, 9-question anti-hardcode self-walk
  (verdict `approve`), §3 layer-classification walk for the rejected
  R-items, generalization-coverage statement per approved override,
  sprint-objective-met check, five open questions for the human, and
  the action-bank deltas + next recommended action recommendation.
  Four new R-items proposed: `R-phase2-uc-cdf-customer-context-policy-widen`
  (`product_policy`; widen phase 2 §2.10 line 358 to allow
  `get_customer_context` for UC-C / UC-D / UC-F),
  `R-cs176-semantic-planner-escalation-family-discrimination`
  (`semantic_planner`; user-intent vs bot-limit escalation reason
  selection), `R-case-spec-overrides-schema-scoring-extension`
  (`infra` / eval harness; extend the override schema to permit
  `scoring.*` overrides or add new `Expected` fields the scorer
  consumes), and `R-cs095-family-refresh-post-l3-reversal`
  (contingent / DORMANT — only fires if a future disposition
  reverses cs_095 UC primary back to UC-A).
- `docs/10-handoff.md` (this file) updated lead to Sprint 21; Sprint
  20 demoted to "Preceding sprint" with the existing detail
  paragraphs preserved below for history.

This sprint declares the `docs/current/iteration_governance.md` §7
Layer-classification + anti-hardcode stanza fulfilled in
**multi-layer prospective** form per the Sprint 19 / Sprint 20
precedent: the seven dispositions span four §3.1 layers (3 ×
`eval_spec` approved overrides, 1 × `product_policy` rejection, 1 ×
`semantic_planner` rejection, 2 × deferred pending the schema
extension at `infra` / eval harness). No Tier-0 invariant is added;
no semantic hardcode is introduced (each approved override is per-
`source_session_id` ground truth, not a runtime branch or prompt
rule); no `human_review_required` flag fires beyond cs_040's
explicit §3.2 Q2 routing back to the pre-existing Tier-0 candidate
`R-escalation-reason-runtime-evidence-contract-review` (out of Sprint
21 scope per the objective). The §1.7 forbidden-line check ("widening
eval spec to accept a genuine bot mistake") was applied to every
approved override with brief-quoted evidence (see Sprint 21 handoff
§3.1, §3.4, §3.6) and to every rejected / deferred disposition (see
Sprint 21 handoff §3.2, §3.3, §3.5, §3.7); cs_176 is the explicit
example of declining to widen an L1-correctly-hard-failed case.

Sprint 21 required a narrow fix iteration after Codex's first review
returned `decision: fix_required, blocking_count: 3`. All three
blockers were approved-override evidence-gap findings under the
parent objective's §1.7 evidence-package gate: each of cs_001 /
cs_095 / cs_192 had quoted Ground-truth chain only and not the
brief's `What happened?` + `What should a good CS agent have done?`
field bodies. The fix iteration was paste-in evidence remediation
only (six verbatim brief-quote blocks added at
`docs/sprints/sprint-021-handoff.md` lines ~280 / ~452 / ~608, two
per approved override); no override re-litigation, no case-family
edits, no runtime / prompt / judge / YAML changes. Cs_095's
dimension-distinction language (separating the approved UC-D
classification override from the brief's orthogonal `prompt_projection`
/ `semantic_planner` failures the override does NOT widen eval to
accept) was preserved byte-identical per the fix-iteration objective
and Codex's own verification at
`docs/sprints/sprint-021-codex-review.md:4`.

Codex's Sprint 21 fix re-review returned `decision: fix_required,
blocking_count: 3`. All three blockers are typographical-fidelity
findings: dropped markdown emphasis (`*way*` on cs_001's
"`What should`" quote, `**account-state-aware investigation**` on
cs_095's, `**...**` on cs_192's) and one missing trailing colon in
cs_001's quote. The substantive dispositions are correct per Codex's
own non-blocking checks at `docs/sprints/sprint-021-codex-review.md`
lines 71–74: override schema sane (17 applied / 0 pending / three
Sprint 21 source session ids present), no case-family edit, no
judge-rubric edit, no governance edit, no semantic hardcode, and the
cs_095 dimension-distinction paragraph remains byte-identical to the
pre-fix state. The human accepted the typographical gap as
close-eligible on 2026-05-14: the gap is in evidence-package
documentation polish (5-character markdown / punctuation deltas on
quote blocks that were the entire point of the fix iteration), not
in any system behaviour, override content, case-family edit, or
governance surface. This sprint is classified
**A-with-evidence-gap-acknowledgment** (distinct from Sprint 20's
A-with-packaging-note, which was commit-boundary, and from B, which
would be an in-scope substantive blocker); no further re-review
round is required, and no remediation against the runtime, prompt,
CaseSpec, judge, or eval harness is open. Lesson captured in deliver
agent memory at
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_review_prompt_verbatim_disambiguation.md`
and `feedback_close_over_fix_required_typographical.md`: future
review prompts demanding source-quote fidelity must explicitly
disambiguate byte-verbatim (markdown + punctuation preserved) vs.
content-verbatim, and the close-over-fix_required-on-typographical-
fidelity pattern is itself a documented classification.

Preceding sprint (most recently closed, Codex pass):
Sprint 20 (G2 Interactive Case Family + Shadow Split + Already-Called
Soft Signal — parallel A + B) closed on 2026-05-13 after a narrow fix
iteration on the same branch. Sprint 20 was a two-track
semantic-touching sprint. Track A is content-authoring on the
eval-spec surface; it converts the 10 G1 Failure Briefs into the
four-class case-family structure required by
`docs/current/iteration_governance.md` §5.1 (target / neighbor /
negative / shadow) and builds the v0 shadow-split mechanism that
hides shadow-class cases from the dev agent. Track B is the
canonical `prompt_projection` bundle per Sprint 19 §"Bundle-or-defer
policy": it surfaces an `already_called: [{tool, arguments_hash,
at_step}]` diagnostic slot in the per-step projection so the LLM
can see prior identical-args calls before re-emitting. **Outcome:
both tracks land.** Track A explicitly does NOT remediate any G1
brief; remediation is a G3+ sprint that consumes the families.
Four deliverable surfaces changed:

- `eval_interactive/case_specs/case_families/` (new) and
  `eval_interactive/case_specs_shadow/` (new) — 61 new CaseSpec
  files across 10 families plus visible + shadow manifests + an
  access-boundary doc. 9 smoke targets referenced via the visible
  manifest (no file duplication per Context Pack §7.5 Option A);
  1 manual-probe target hand-authored from the brief's quoted
  system_instruction MUST clause + phase 2 UC-A policy. Totals:
  10 targets + 20 neighbors + 20 negatives + 20 shadows = 70
  CaseSpec-shaped entries.
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
  and `AgentRunLoopImpl.java` — new 6-argument `build(...)` overload
  emits `already_called` from successful prior `ToolEvent`s; legacy
  5-argument overload preserved (delegates with an empty list, still
  emits the slot for projection shape stability). Argument hash via
  stable Jackson canonicalisation + first-16-hex-chars of SHA-256.
  Slot is observability-only; the runtime does NOT short-circuit
  dispatch on slot population.
- `docs/sprints/sprint-020-handoff.md` (parent sprint handoff) +
  `docs/sprints/sprint-020-fix-handoff.md` (fix iteration addendum).
  The parent handoff is the 12-section handoff with the Context
  Pack, per-family detail, shadow-split mechanism description, slot
  wiring detail, 9-question anti-hardcode self-walk (verdict
  `approve`), generalization-coverage table, and §12 action bank
  delta recommendations. The fix-iteration addendum carries the
  diff summary + test results for the two findings the parent
  Codex pass surfaced.
- `docs/10-handoff.md` (this file) updated lead to Sprint 20; Sprint
  19 demoted to "Preceding sprint".

Sprint 20 required a narrow fix iteration after Codex's first review
returned `decision: fix_required, blocking_count: 2`. The fix
iteration added an integration-level test
(`AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest`) that
drives `AgentRunLoopImpl.run(...)` end-to-end with two identical
LLM-emitted `search_knowledge` calls and verifies both reach
`ToolDispatcher.dispatch(...)` plus the second-step projection
populates the `already_called` slot — closing Finding 1's runtime
non-enforcement bar at the run-loop granularity Codex required. The
fix also added `AlreadyCalledCs011T2ShapeTest` to cover the cs_011
T2 slot-population target named in the parent objective lines
231–233, and reconciled `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md`
to describe only the v0 mechanism actually shipped (directory
boundary + custom-path-only loading via `CaseSetManager.load_custom(path)`
+ documented self-restraint); the previously-claimed
`--include-shadow` runner gate was unimplemented and is now moved
to a "Known v0 gaps / v1 hardening" section as the deferred
`R-shadow-include-flag-runner-gate` R-item. Full server suite after
the fix: **896 / 0 / 0 / 1 (1 pre-existing skip)**.

Codex's Sprint 20 fix re-review returned `decision:
out_of_scope_review, blocking_count: 1`. Both substantive findings
closed cleanly per Codex's own evidence (see
`docs/sprints/sprint-020-fix-codex-review.md` §"Resolution evidence
observed for the two original findings"); the single blocker was a
mechanical commit-boundary / packaging observation that the dev's
fix commit (`cec7da5`) bundled deliver-agent-owned files — the
`docs/sprint_objective.md` fix-iteration append, the `compact/`
planning prompts, and `compact/sprint-deliver-orchestrator.md` — into
the same commit as the authorized fix scope, rather than substantive
dev-agent scope drift on a behaviour surface. The packaging artefact
is acknowledged here and rolled forward into this close commit; no
further re-review round is required, and no remediation against the
runtime, prompt, CaseSpec, judge, or eval harness is open.

This sprint declares the `docs/current/iteration_governance.md` §7
Layer-classification + anti-hardcode stanza fulfilled in
**multi-layer** form per the sprint objective: Track A = `eval_spec`
(case-family authoring) + `infra` (shadow-split mechanism); Track B
= `prompt_projection`. The fix iteration's stanza extension lands
`prompt_projection` (Finding 1 regression-test extension at
`AgentRunLoopImpl.run(...)` runtime boundary) + `infra` (Finding 2
eval-harness documentation reconciliation, code-path-free). No
Tier-0 invariant is added; no semantic hardcode is introduced; no
`human_review_required` flag fired. The generalization-coverage
table is the deliverable itself for Track A.

Earlier sprint (Codex pass):
Sprint 19 (Smoke Regression Investigation + Orchestrator Tool-Call
De-Dup — parallel A + B) walked `docs/current/iteration_governance.md`
§3.2 per case for the two R-items the Sprint 18 G1 backlog named
as blocking G2: `R-smoke-regression-investigation` (Track A) and
`R-runtime-orchestrator-tool-call-deduplication` (Track B). Outcome
was proposal-only on both tracks: Track A found five of six regressed
cases share a slow-LLM placeholder shape (layer `infra` per §3.2
Q1; deferred remediation as `R-slow-llm-placeholder-coalesce`),
cs_011 lands at `semantic_planner` (failure to escalate on explicit
handover request), cs_066 at `skill_state` (UC-K case_id-binding
loss). Track B re-classified the orchestrator-de-dup R-item from
`infra` to `semantic_planner` and proposed two follow-on items:
`R-prompt-projection-already-called-soft-signal` (read-side soft
signal — delivered by Sprint 20 Track B) and
`R-idempotent-read-tool-short-circuit` (conditional). Full handoff:
`docs/sprints/sprint-019-handoff.md`.

Earlier sprint (no Codex run):
Sprint 18 (Human-led Failure Portfolio — G1) filed 10 representative
real failures as Failure Briefs under
`docs/diagnostics/failure-briefs/` per the Sprint 17 §2 template and
recorded 18 G1-surfaced R-items + 2 open observations under
`docs/action_bank.md` §5.2. No runtime, prompt, FAQ corpus, CaseSpec,
judge, eval harness, test, or script change. G1 declared the §7
stanza **exempt** under the docs-only governance exemption. Full
handoff: `docs/sprints/sprint-018-handoff.md`. Archive:
`docs/sprints/sprint-018-g1-failure-portfolio-objective.md`.

Earlier sprint (Codex pass):
Sprint 17 (Iteration Governance Lite — G0) landed the docs-only
governance bundle (`docs/current/iteration_governance.md` 7 sections
+ `AGENTS.md` constitution chain + `docs/action_bank.md` §5.1
governance-track backlog); Codex `decision: pass, blocking_count: 0`;
archive under `docs/sprints/sprint-017-*`.

Earlier accepted sprint:
Sprint 16 (Handover Exactly-Once Contract and Repro) landed as a
docs + characterization-test sprint; no runtime behaviour change.
Defined the handover exactly-once contract in
`docs/proposals/handover_orchestrator_design.md`, added
`Sprint16HandoverDualPathReproTest`, and opened the
`docs/release_gate.md` §1.1 blocking rule + the **Single Handover
Orchestrator** action item.


## 2. Sprint 14 goal

Sprint 14 upgrades FAQ / KB grounding trustworthiness by making the
knowledge source chain, published safety, canonical URL availability,
and citation observability explicit — without introducing a broad hard
runtime citation gate, a new skill runtime framework, broad routing
rewrites, or additional mechanical escalation.

Scope is exactly the three Sprint 14 actions L0 / L1 / L2.

## 3. Sprint 14 implementation

### L0 — KB canonical URL / Help URL / published safety audit + fix

Code changes:

- `server/src/main/java/com/gumtree/csagent/repository/KbChunkRepository.java`
  — added `findNearestByEmbeddingPublishedOnly` and
  `findNearestByEmbeddingWithUcTagsPublishedOnly` queries that join
  `kb_articles` with `is_published = true`. Existing legacy methods
  preserved for backward compatibility.
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
  — routes through the new published-only ANN methods so unpublished
  candidates never enter the rerank pipeline. Added defense-in-depth
  hit-projection filter that drops any post-fetch unpublished article
  even if the ANN returned it via a stale cache. Stamps
  `canonical_url_missing` per hit.
- `server/src/main/java/com/gumtree/csagent/model/KnowledgeHit.java`
  — added `canonicalUrlMissing` boolean (Jackson `canonical_url_missing`).
- `server/src/main/java/com/gumtree/csagent/service/tools/SearchKnowledgeTool.java`
  — projects `hits[*].canonical_url_missing` into the agent-visible
  response so missing-URL data-quality gaps are observable.
- `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`
  — refuses unpublished articles with deterministic
  `article_unpublished_safe_refuse` reject reason
  (`UNPUBLISHED_REJECT_REASON` constant). Exposes `canonical_url`
  alongside legacy `source_url`. Stamps `canonical_url_missing` and
  `safe_to_show` (= published AND non-blank body).
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
  — extracted `buildKbArticleFromJson(...)` package-private helper so
  the FAQ → KbArticle field-preservation contract (including
  `source_url` and `is_published`) is unit-testable.
- `scripts/build_knowledge_base.py` — additive: `mapping_summary_report.md`
  now carries a "URL & Published-Safety Coverage" block enumerating
  total / published / unpublished / with-canonical-URL / missing-URL /
  unsafe-to-show counts. JSON content unchanged.

QA report: `qa-reports/faq-kb-lineage-and-url-audit.md`.

Tests added (5 focused tests of the form Sprint 14 §L0 prescribed):

- `Sprint14KnowledgeIngestionCanonicalUrlTest`
  (4 cases — preservation of `Help_Site_URL__c` → `source_url`, respects
  explicit `published_status=false`, missing URL surfaces null,
  CSV-mapping fallback for `uc_tags`).
- `Sprint14KnowledgeSearchPublishedFilterTest`
  (4 cases — UC-scoped path routes through published-only ANN, no-tag
  path same, post-rerank projection drops still-unpublished article,
  missing-URL hit stamps `canonical_url_missing=true`).
- `Sprint14ResolveArticleSafetyTest`
  (4 cases — refuses unpublished with canonical reject reason,
  exposes `canonical_url` mirroring `search_knowledge`, missing URL
  classified as observable DQ gap, blank body marks `safe_to_show=false`).

### L1 — Separate retrieved / resolved / cited source evidence

Code:

- `server/src/main/java/com/gumtree/csagent/service/knowledge/CitationExtractor.java`
  (new) — passive citation extractor. Detects Salesforce KA `source_id`
  pattern, canonical URL pattern, and (conservatively) title fuzzy
  match against retrieved/resolved candidates. Stateless;
  side-effect-free.
- `server/src/main/java/com/gumtree/csagent/service/knowledge/SourceEvidenceLineage.java`
  (new) — record `(retrievedSourceIds, resolvedSourceIds,
  citedSourceIds, citedCanonicalUrls, retrievedCanonicalUrls)` with
  `fromToolEvents(toolEvents, botResponse)` constructor walking
  `search_knowledge` (retrieved) and `resolve_article` (resolved) tool
  events. §L0 contract: a refused unpublished resolve is NOT counted
  as resolved — the article remains in the
  `retrievedButUnresolvedSourceIds` set.
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
  — added 4 new `@Transient` slots: `retrievedSourceIds`,
  `resolvedSourceIds`, `citedSourceIds`, `citedCanonicalUrls`. Schema
  unchanged; `bot_turns.source_ids` column preserved verbatim.
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  — `recordRunResult(...)` now stamps the new transient slots via the
  new `stampFaqGroundingObservability(...)` helper. The existing
  `bot_turns.source_ids` write path is untouched. Stamping is wrapped
  in try/catch so any unexpected pattern in tool events / bot text
  cannot break record-turn persistence.

Tests:

- `Sprint14SourceEvidenceLineageTest` (7 cases):
  retrieved-only does not imply cited, resolved evidence tracked
  separately, source_id mention detected, URL mention detected and
  mapped back to candidate, third-party URL recorded as free-form
  citation, missing citation observable but non-blocking, refused
  unpublished resolve excluded from resolved set.

### L2 — FAQ grounding contract + soft diagnostics

Docs:

- `docs/current/faq_grounding_contract.md` (new, normative) — defines the
  output class taxonomy, the §L1 evidence lineage construction, the
  §L2 diagnostic state transition table, and the non-blocking
  guarantee. Records future narrow Sprint 16 §S1 hardening candidates
  surfaced by the §L0 audit (`R-faq-grounded-resolve-bypass`,
  `R-cited-but-unresolved`, `R-resolved-but-uncited-rate`,
  `R-canonical-url-missing-rate`).

Code:

- `FaqOutputClass` enum — six classes: `factual_answer`,
  `clarification`, `empathy_ack`, `handover`, `tool_status`,
  `intake_collection`. Only `factual_answer` requires grounding.
- `FaqOutputClassifier` — heuristic classifier with
  `ClassifierContext(intakeUseCase, handoverDispatched)` so the
  runtime's `handoverDispatched` flag overrides any wording-based
  guess and intake UCs prefer `intake_collection` over
  `clarification` on a question shape.
- `FaqGroundingDiagnostics` — record `(outputClass, faqGroundingState,
  citationPresent, citationMatch, citationDrift, resolvedButUncited,
  retrievedButUnresolved)`. State table: `factual_grounded`,
  `factual_uncited`, `factual_unresolved`, `factual_unretrieved`,
  `non_factual`, `unknown`.
- `BotSession` — added 7 transient slots: `faqOutputClass`,
  `faqGroundingState`, `citationPresent`, `citationMatch`,
  `citationDrift`, `resolvedButUncited`, `retrievedButUnresolved`.
- `ControlKernel.stampFaqGroundingObservability(...)` — single call
  from `recordRunResult` populates §L1 + §L2 slots; observability-only
  with no rejection / rewrite / loop.

Tests:

- `Sprint14FaqGroundingDiagnosticsTest` (14 cases):
  taxonomy exemption check (only factual requires grounding),
  classifier coverage for each shape (clarification / empathy /
  handover-flag override / tool_status / intake / factual default), and
  the full diagnostic state table (`factual_grounded`,
  `factual_uncited`, `factual_unresolved`, `factual_unretrieved`,
  `citation_drift`, all five non-factual classes skip grounding,
  missing citation is observable not blocking).

### Audit findings (factual-answer bypass — Sprint 16 candidate)

Per Sprint 14 §L2, if the audit found a current factual-answer bypass
of search/resolve, document it as a future narrow Sprint 16 candidate.
The audit recorded **R-faq-grounded-resolve-bypass** in
`docs/current/faq_grounding_contract.md` §6: the existing Sprint 6 §G2 guard
refuses the `request_handover(faq_miss_threshold_exceeded)` shape, but
does NOT refuse a FINAL_ANSWER shape that paraphrases an unresolved
hit. This is observable today as `retrieved_but_unresolved=true` on a
factual-answer turn. Sprint 14 explicitly does NOT close it — the
guidance is for a future narrow Sprint 16 §S1 hardening if real-traffic
evidence motivates it.

## 4. Files changed (Sprint 14)

Production code:

- `server/src/main/java/com/gumtree/csagent/repository/KbChunkRepository.java`
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
- `server/src/main/java/com/gumtree/csagent/service/knowledge/CitationExtractor.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/SourceEvidenceLineage.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqOutputClass.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqOutputClassifier.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqGroundingDiagnostics.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  (added §L1/§L2 stamping helper; preserved all existing logic)
- `server/src/main/java/com/gumtree/csagent/service/tools/SearchKnowledgeTool.java`
- `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`
- `server/src/main/java/com/gumtree/csagent/model/KnowledgeHit.java`
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
  (added 11 `@Transient` slots; no schema change)

Tests (new):

- `Sprint14KnowledgeIngestionCanonicalUrlTest` (4 cases)
- `Sprint14KnowledgeSearchPublishedFilterTest` (4 cases)
- `Sprint14ResolveArticleSafetyTest` (4 cases)
- `Sprint14SourceEvidenceLineageTest` (7 cases)
- `Sprint14FaqGroundingDiagnosticsTest` (14 cases)

Docs:

- `docs/current/faq_grounding_contract.md` (new, normative)
- `qa-reports/faq-kb-lineage-and-url-audit.md` (new — §L0 audit + repair)
- `docs/10-handoff.md` (this file)
- `docs/action_bank.md` (Sprint 14 row added)
- `docs/sprint_objective.md` retained — already contains the Sprint 14
  objective.

Scripts:

- `scripts/build_knowledge_base.py` — additive URL & Published-Safety
  Coverage block in the auto-generated mapping report.

No FAQ corpus content, CaseSpec, judge, broad routing taxonomy,
handover payload contract, Salesforce backend contract, full Issue
Ledger, all-UC task taxonomy, escalation enum, or system prompt was
touched.

No DB schema migration. The `bot_turns.source_ids` column is
preserved verbatim; new lineage / diagnostics fields are
session-transient observability material consumed via projection /
trace evidence.

## 5. Tests run

- `mvn -pl server test -Dtest='Sprint14*'`
  → **33 / 0 / 0 / 0** (Sprint 14 §L0/§L1/§L2 focused tests).
- `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
  Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
  Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
  Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test'`
  → **269 / 0 / 0 / 0** (named Sprint 14 regression guards).
- `mvn -pl server test`
  → **854 / 0 / 0 / 0** at the Sprint 14 milestone (was 821
  pre-Sprint-14; +33 new Sprint 14 §L0/§L1/§L2 tests). Post Sprint
  14.1 closure the suite is **859 / 0 / 0 / 0** (+5 trace-persistence
  tests; see §15).
- `pytest eval_interactive/tests/`
  → **294 / 0** (full Python eval test suite).
- Smoke runs were NOT executed for Sprint 14. Sprint 14 introduces no
  FAQ corpus content change, no judge change, no CaseSpec change, no
  prompt change, no escalation enum change, no routing taxonomy
  change, and no eval-output schema change. The current canonical
  baseline (`docs/current_eval_baseline.md`) is preserved
  (post-Sprint-8 r1 / r2 runs).

## 6. KB URL / published audit result

| Metric | Value |
|--------|-------|
| Total articles | 218 |
| Articles published (`published_status=true`) | 218 |
| Articles unpublished (`published_status=false`) | 0 |
| Articles with `source_url` (canonical URL) | 180 |
| Articles missing `source_url` (canonical-URL DQ gap) | 38 |
| Articles unsafe to show (unpublished OR empty body) | 0 |

The 38 articles missing a canonical URL come from rows the Salesforce
export shipped without `Help_Site_URL__c`. They remain searchable but
the §L0 fix surfaces the gap as `canonical_url_missing=true` on every
search-hit and resolve-article response so a reviewer can classify the
case as a corpus-side curation task rather than a runtime fix. The
broader curation work is out of Sprint 14 scope and queued for the Eval
Governance / corpus-audit owner alongside `G-FAQ-corpus-answerability`.

Per `qa-reports/faq-kb-lineage-and-url-audit.md` §3, nine concrete §L0
fixes landed this sprint (published-safety filter at SQL layer + at hit
projection, refusal of unpublished `resolve_article`, exposure of
`canonical_url` alongside `source_url`, observable
`canonical_url_missing` flag on both tools, and the `safe_to_show`
conjunctive predicate on `resolve_article`).

## 7. Retrieved / resolved / cited evidence contract

Sprint 14 §L1 splits the historically-overloaded `sourceIds` concept
into four observably-distinct dimensions, all populated each turn and
durably persisted on `bot_turns.projected_context.faq_grounding`
(Sprint 14.1 closure) and mirrored onto in-memory `BotSession`
transient slots:

- `retrievedSourceIds` — IDs from successful `search_knowledge` events
  (post §L0 published-safety filter). De-duplicated; insertion order
  preserved.
- `resolvedSourceIds` — IDs that successfully passed through
  `resolve_article`. A refused unpublished resolve is NOT counted.
- `citedSourceIds` — IDs detected in the customer-visible reply by
  the passive `CitationExtractor` (source_id mention, URL mention
  mapped back to candidate, conservative title match).
- `citedCanonicalUrls` — URLs in the reply that did NOT correspond to
  any candidate `canonical_url`. Typically third-party (e.g. gov.uk).

The `bot_turns.source_ids` column is preserved verbatim as the
backward-compatible aggregate. Sprint 14 §L1 explicitly does NOT use
the diff between the four dimensions to gate / rewrite / loop the bot
response. The output is observability material — see
`docs/current/faq_grounding_contract.md` §3 for the canonical contract.

## 8. Soft diagnostic fields

Sprint 14 §L2 surfaces seven additional fields per turn under
`bot_turns.projected_context.faq_grounding` (Sprint 14.1 closure;
`docs/current/faq_grounding_contract.md` §4) and mirrors the same values onto
`BotSession` transient slots for in-process readers:

- `faqOutputClass` — taxonomy token (`factual_answer`, `clarification`,
  `empathy_ack`, `handover`, `tool_status`, `intake_collection`).
- `faqGroundingState` — coarse state (`factual_grounded`,
  `factual_uncited`, `factual_unresolved`, `factual_unretrieved`,
  `non_factual`, `unknown`).
- `citationPresent`, `citationMatch`, `citationDrift` — citation
  observability triplet.
- `resolvedButUncited`, `retrievedButUnresolved` — evidence-diff
  observability pair.

All seven are observability-only. Sprint 14 §L2 does NOT block, rewrite,
re-loop, or escalate based on any of them. The §G2 FAQ-grounded-resolve
guard from Sprint 6 remains the only enforced runtime check on this
surface.

## 9. Regression guard outcomes

All Sprint 14 active guards pass:

- `L1:escalation_reason_consistency` = **0** (never re-introduced).
- `CONTRACT_VIOLATION:active_use_case` = **0**.
- Existing FAQ S1 guard tests remain green
  (`AgentRunLoopS1FaqGroundedResolveGuardTest`).
- Sprint 6 §G0 ReadTimeout closure intact
  (`test_agent_client_session_create_timeout.py`).
- cs014 remains UC-C
  (`Cs014RouteAndLoopHandoverIntegrationTest`,
  `Cs014RouteAndDistressRegressionTest`).
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 distress reconciliation remains green.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested` regression remains
  green (`Cs176ExplicitHumanHelpHandoverIntegrationTest`).
- All Sprint 7 / 7.1 / 8 / 8.1 / 8.2 / 9 / 9.1 / 10 / 11 / 11.1 / 12 /
  13 hard invariants remain green
  (named regression suite: 269 / 0).
- `RuntimeIntentClassifier` remains runtime-internal.
- `bot_turns.source_ids` write path unchanged.
- `bot_turns` / `bot_sessions` / `kb_articles` / `kb_chunks` schemas
  unchanged.

New Sprint 14 §L0 / §L1 / §L2 guards (33 deterministic JUnit tests):

- §L0 — `Sprint14KnowledgeIngestionCanonicalUrlTest`,
  `Sprint14KnowledgeSearchPublishedFilterTest`,
  `Sprint14ResolveArticleSafetyTest`.
- §L1 — `Sprint14SourceEvidenceLineageTest`.
- §L2 — `Sprint14FaqGroundingDiagnosticsTest`.

## 10. Remaining risks

**No new P0 / P1 blockers opened by Sprint 14.** The change is additive
observability + a narrow §L0 published-safety / canonical-URL fix; no
runtime main-flow architecture file was modified.

Sprint 14 §L0 audit surfaced four narrow follow-ups (recorded in
`docs/current/faq_grounding_contract.md` §6 — all deferred):

- **R-faq-grounded-resolve-bypass** — factual answers paraphrasing
  retrieved-but-unresolved hits. Observable today as
  `retrieved_but_unresolved=true` on a factual-answer turn. Future
  narrow Sprint 16 §S1 hardening candidate IF real-traffic shows
  reproducible cases.
- **R-cited-but-unresolved** — `citation_drift=true` with cited
  source_id never retrieved/resolved. Hallucination signal candidate;
  Eval Governance owns until reproduced.
- **R-resolved-but-uncited-rate** — corpus-level rate of uncited
  factual answers. Eval Governance.
- **R-canonical-url-missing-rate** — 38 articles missing
  `Help_Site_URL__c`. Corpus curation, not runtime.

Residuals carried forward from Sprint 13 §8 (unchanged):

- `R-cs015-description-keyword`, `R-cs176-UC-I-drift`,
  `R-S3-no-prior-search-guard`, `R-S5-Tier2-runtime-guard`,
  `R-stall-detector-calibration`, `R-L3-relevance-tone`,
  `R-FAQ-corpus-answerability`, `R-advert-link-product-decision`,
  `R-rerank-fallback-diagnostics`, `R-task-type-token-naming`,
  `R-record-outcome-loop`, `R-clean-baseline-promote`,
  `R-full-issue-ledger`, `R-skill-runtime-framework`,
  `R-prompt-risk-signal-handling`.

The current canonical eval baseline remains the post-Sprint-8 r1 / r2
runs documented in `docs/current_eval_baseline.md`. Sprint 14
explicitly does NOT promote a new canonical eval baseline.

## 11. Recommendation for Sprint 15 or Sprint 16

Recommended next phase:

**Eval Governance docs sprint** (or equivalent governance-only work)
remains the primary recommendation. Sprint 14 + 14.1 closed the FAQ /
KB evidence lineage and safety workstream (including the persistence
gap Codex flagged) that Sprint 13 §8 / §9 had open; the largest
residual category is still `judge_volatility` / `faq_corpus_gap` /
`product_policy_gap`, all governance-owned.

(The Sprint 14.1 Codex review surfaced **Script / Policy Config
Governance Sprint** as an alternative recommended next phase. Either
phase is reasonable; choose by prioritisation, not by Sprint 14 / 14.1
state.)

Alternative phases ranked:

1. **Eval Governance** — primary recommendation (above).
2. **Narrow Sprint 16 §S1 hardening** — only if real-traffic evidence
   demonstrates `R-faq-grounded-resolve-bypass` reproducibly affects
   answer correctness on a high-traffic UC. The Sprint 14 §L1 / §L2
   diagnostics surface (`retrieved_but_unresolved=true` on a
   `factual_answer` turn) is the trigger to look for. Until then, the
   §G2 prompt-side nudge already handles the dominant case.
3. **Narrow Corpus Curation** — fill the 38 missing
   `Help_Site_URL__c` rows. Owner: Eval Governance / corpus audit.
4. **Re-run validation after infra cleanup** — viable if Kimi /
   smoke credentials are available; runs alongside Eval Governance.
5. **Release Candidate Hardening** — premature; depends on a
   clean canonical baseline that has not yet been promoted.

Do not start another runtime sprint unless triage finds a new
P0 / P1 runtime blocker. Sprint 14 explicitly does NOT promote a new
canonical eval baseline.

## 12. Was Sprint 14 objective met?

Sprint 14 alone was **not** met under its initial Codex review: the
review correctly identified that the §L1 / §L2 diagnostics were stamped
only onto `@Transient` `BotSession` fields after `BotTurn.save(...)`,
so a normal save / reload trace could not observe them. Sprint 14.1
(§15 below) closed that single blocking gap. The Sprint 14.1 closure
review returned `decision: pass, blocking_count: 0`. Sprint 14 + 14.1
together are accepted:

- L0 KB canonical URL / Help URL / published safety audit + fix landed.
  Source chain traced (CSV → build script → JSON → DB → service →
  tools); `Help_Site_URL__c` confirmed preserved; published-safety
  filter added at SQL + projection layer; `resolve_article` refuses
  unpublished with deterministic reject reason; missing canonical URL
  classified as observable DQ gap; QA report
  `qa-reports/faq-kb-lineage-and-url-audit.md` shipped; 12 focused
  tests.
- L1 separate retrieved / resolved / cited source evidence landed.
  `SourceEvidenceLineage` value object splits the historically-
  overloaded `sourceIds` concept; `CitationExtractor` provides passive
  citation extraction (source_id pattern, URL pattern, conservative
  title match); existing `bot_turns.source_ids` write path preserved
  for back-compat; 7 focused tests confirm retrieved-only does not
  imply cited, resolved tracked separately, citation extractor
  patterns work, missing citation observable not blocking.
- L2 FAQ grounding contract + soft diagnostics landed.
  `docs/current/faq_grounding_contract.md` defines the output class taxonomy,
  the §L1 evidence lineage construction, the §L2 diagnostic state
  table, and the non-blocking guarantee. `FaqOutputClass`,
  `FaqOutputClassifier`, `FaqGroundingDiagnostics` services compute
  the seven soft diagnostic fields per turn. 14 focused tests.
- Diagnostics are durably observable on
  `bot_turns.projected_context.faq_grounding` (Sprint 14.1 closure);
  the in-memory `BotSession` transient slots are mirrored from the
  same computed values for in-process readers / tests. No DB schema
  migration; no broad runtime / prompt / routing scope; no hard
  citation gate; existing §G2 guard preserved verbatim.
- Full server suite 859 / 0 (post Sprint 14.1; was 854 pre-closure);
  named Sprint 14 regression suite 269 / 0; full Python eval 294 / 0;
  `L1:escalation_reason_consistency = 0`;
  `CONTRACT_VIOLATION:active_use_case = 0`; cs014 / cs066 / cs095 /
  cs002 / cs029 / cs176 regressions all green.

Out-of-scope items (broad hard citation gate, new skill runtime
framework, broad S1 rewrite, broad routing taxonomy rewrite,
escalation enum changes, CaseSpec churn, FAQ corpus content rewrite,
judge calibration, eval expansion, broad prompt rewrite, mechanical
risk-keyword escalation) were NOT touched.

## 13. Current-doc maintenance rule

`docs/10-handoff.md`, `docs/diagnostics/codex-findings.md`,
`docs/sprint_objective.md`, and `docs/action_bank.md` are
overwrite-current-state files. Before replacing one of them:

1. archive the previous version under `docs/sprints/` if it
   belongs to a sprint closure, or
   `docs/archive/current-docs/` if it is an ad hoc transition;
2. then overwrite the working file;
3. keep only actionable current state in the working file.

Historical detail belongs in `docs/sprints/`,
`docs/archive/current-docs/`, `eval_interactive/results/`, and
`qa-reports/`.

Sprint 13 archives are at `docs/sprints/sprint-013-*`;
Sprint 14 will archive to `docs/sprints/sprint-014-*` on closure.

## 14. Do not reopen

- broad full review
- broad routing rewrite
- judge calibration implementation
- CaseSpec churn
- anchor / exploration / promotion hard-gate expansion
- smoke 14/14 optimization
- S3 no-prior-search guard unless explicitly selected
- S5 Tier-2 runtime guard unless explicitly selected
- broad TraceViewer redesign
- llm_call_log dashboard / per-tool latency dashboard
- search threshold tuning / answer_miss / faq_miss semantic redesign
- tool-deadline guard / bypass-DISCOVER redesign
- advert-link generator / direct listing URL tool
- full Issue Ledger / `issues[]` / per-issue budgets / all-UC task
  taxonomy / full skill runtime framework / handover payload rewrite
- new escalation reason enum value
- runtime sprint unless a new P0 / P1 runtime blocker is found
- broad system prompt rewrite — narrow risk-signal-handling proposal
  in `docs/runtime_freeze_and_risk_policy.md` §6.2 only on Eval
  Governance trigger
- broad FAQ corpus rewrite — narrow corpus curation for the 38
  `Help_Site_URL__c` gap rows is a queued Eval Governance / corpus
  audit task
- hard runtime citation gate — Sprint 14 §L2 explicitly carved this
  out; only consider after real-traffic evidence escalates the
  `citation_drift` / `retrieved_but_unresolved` signals

## 15. Sprint 14.1 closure (FAQ grounding observability persistence)

Status: **closed (Codex pass)**. The Sprint 14.1 closure review
returned `decision: pass, blocking_count: 0`, accepting Sprint 14 +
14.1 as a unit.

The initial Sprint 14 Codex review had returned `decision:
fix_required, blocking_count: 1` against the Sprint 14 diff. The
single blocker was:

> Sprint 14 L1/L2 lineage + grounding diagnostics are computed but not
> durably observable. They are stamped onto `@Transient` `BotSession`
> fields after `BotTurn` is saved, so a normal save/reload trace cannot
> see `retrieved_source_ids`, `resolved_source_ids`, `cited_source_ids`,
> `faq_grounding_state`, `citation_present`, `citation_match`,
> `citation_drift`, `resolved_but_uncited`, `retrieved_but_unresolved`.

### Closure summary

- **Blocker fixed:** the §L1 `SourceEvidenceLineage`, §L2
  `FaqOutputClass`, and §L2 `FaqGroundingDiagnostics` are now computed
  in `ControlKernel.recordRunResult(...)` BEFORE
  `turnRepository.save(turn)`, and the snake_case payload is merged
  into the existing `bot_turns.projected_context` JSONB column under a
  new top-level `faq_grounding` key.
- **Trace surface used:**
  `bot_turns.projected_context.faq_grounding` (JSONB; no migration —
  the column already exists and is opaque JSON).
- **Fields persisted under `faq_grounding`:**
  - `retrieved_source_ids` (array of source_id strings),
  - `resolved_source_ids` (array of source_id strings),
  - `cited_source_ids` (array of source_id strings),
  - `cited_canonical_urls` (array of free-form URLs that did NOT
    correspond to any candidate `canonical_url`),
  - `output_class` (one of `factual_answer`, `clarification`,
    `empathy_ack`, `handover`, `tool_status`, `intake_collection`, or
    `null` when classification was skipped),
  - `faq_grounding_state` (`factual_grounded` / `factual_uncited` /
    `factual_unresolved` / `factual_unretrieved` / `non_factual` /
    `unknown`),
  - `citation_present`, `citation_match`, `citation_drift`,
  - `resolved_but_uncited`, `retrieved_but_unresolved`.
- **Backwards compatibility:** the existing
  `bot_turns.source_ids` `text[]` column is preserved verbatim; its
  write path in `recordRunResult` is unchanged. The `BotSession`
  `@Transient` slots are still populated (mirroring the durable
  values) so any in-memory reader / projection / test that already
  consumes them keeps working.
- **Out of scope (carved out, per Sprint 14.1 task):**
  - no DB migration (uses the existing JSONB column);
  - no hard citation gate (no rejection / rewrite / re-loop on
    missing citation);
  - no new skill runtime framework;
  - no broad S1 hardening;
  - no FAQ corpus / CaseSpec / judge / prompt / routing /
    escalation-enum / risk-policy edit.

### Files changed (Sprint 14.1)

Production code:

- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  — `recordRunResult(...)` reordered to compute lineage / output class
  / diagnostics BEFORE saving the turn; new
  `mergeFaqGroundingIntoProjection(...)`,
  `buildFaqGroundingPayload(...)`, and
  `stampFaqGroundingObservabilityFromComputed(...)` helpers replace
  the old post-save `stampFaqGroundingObservability(...)` call site.

Tests added:

- `server/src/test/java/com/gumtree/csagent/integration/Sprint141FaqGroundingTracePersistenceTest.java`
  (5 cases — retrieved-only, resolved-only, cited-by-source-id,
  cited-by-canonical-url, missing-citation-non-blocking).

Docs:

- `docs/10-handoff.md` (this section + corrections to §1, §7, §8,
  §12 — Sprint 14 was NOT met before Codex review).
- `docs/current/faq_grounding_contract.md` §5 — wiring narrative updated to
  name the durable trace surface
  (`bot_turns.projected_context.faq_grounding`).
- `docs/action_bank.md` — Sprint 14 status reflects the §14.1 closure.

### Tests run

- `mvn -pl server test -Dtest='Sprint14*,Sprint141*'`
  → **38 / 0 / 0 / 0** (33 Sprint 14 + 5 Sprint 14.1 closure tests).
- `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
  Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
  Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
  Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test'`
  → **269 / 0 / 0 / 0** (named Sprint regression suite — including
  Sprint 6 §G2 FAQ S1 guard, cs014 / cs066 / cs095 / cs002 / cs029 /
  cs176 regressions).
- `mvn -pl server test`
  → **859 / 0 / 0 / 0** (was 854; +5 new Sprint 14.1 closure tests).
- `pytest eval_interactive/tests/` — not re-run; Sprint 14.1 changes
  no eval-output schema and no Python parsing path.
- Smoke runs not required — Sprint 14.1 changes no FAQ corpus, no
  prompt, no judge, no CaseSpec, no escalation enum, no routing
  taxonomy, no eval-output schema.

### What Sprint 14.1 explicitly does NOT do

- No DB migration. The trace surface is the existing `jsonb`
  `bot_turns.projected_context` column — `faq_grounding` rides as an
  additive top-level key. Old rows simply won't have the key.
- No hard citation gate. Missing citation remains observable
  (`citation_present=false`, optionally `resolved_but_uncited=true` /
  `retrieved_but_unresolved=true`) but is never used to reject,
  rewrite, or re-loop the bot reply.
- No broad S1 hardening. The Sprint 16 §S1 candidates documented in
  `docs/current/faq_grounding_contract.md` §6 (`R-faq-grounded-resolve-bypass`,
  `R-cited-but-unresolved`, `R-resolved-but-uncited-rate`,
  `R-canonical-url-missing-rate`) remain deferred.
- No new skill runtime framework, no new escalation reason value, no
  CaseSpec edits, no FAQ corpus content edit, no judge / prompt /
  routing / risk-policy change.

## 16. Sprint 15 — Script / Policy Config Governance

Status: **accepted / ready to archive**. The Sprint 15 Codex review
returned `decision: pass, blocking_count: 0`, accepting Sprint 15
as inside the config-governance scope (no prompt, routing, judge,
corpus, CaseSpec, hard citation gate, hot reload, dashboard, or
broad runtime work). Sprint 15 is the latest accepted sprint and
introduced no runtime semantic change. Diff stays narrow: three
actions only.

### 16.1 Implemented actions

#### M0 — Externalize DriftDetector / risk keywords to YAML

What landed:

- New config: `server/src/main/resources/config/risk-keywords.yaml`
  (`version: 1`). Carries the exact same five hard-shift groups
  (UC-J / UC-G / UC-I / UC-J / UC-H, in the original declaration
  order) with the exact same keyword strings as the previous
  hardcoded `DriftDetector.HARD_SHIFT_KEYWORDS` list, plus the same
  escalation regex under `escalation-pattern`.
- New loader: `RiskKeywordsConfig` (Spring `@Service`) loads the YAML
  at startup, compiles the escalation pattern, and validates:
  required fields, non-empty groups, non-blank keywords, no
  cross-group keyword duplicates (which would be unreachable under
  first-match-wins), and a parseable regex. Fail-fast on any
  violation.
- `DriftDetector` constructor-injects `RiskKeywordsConfig`. The
  matching contract is unchanged — same lower-case substring check,
  same first-match-wins precedence within and across groups, same
  same-UC suppression, same `DriftResult` outputs.
- A no-arg test convenience constructor on `DriftDetector` loads the
  bundled default classpath resource so legacy unit tests
  (`DriftDetectorTest`) keep working without behavioural change.

Sprint 15 §M0 explicitly does NOT add new risk semantics, new risk
levels, new escalation reasons, or auto-handover for Level 1 / Level
2 risk signals. Future risk-policy reviews are expected to land as
config diff rather than Java source edits.

Tests added:

- `Sprint15RiskKeywordsConfigTest` (11 cases) — config validation
  parity (default loads, group / keyword set parity vs the hardcoded
  reference, version pin), six structural-validation negative tests
  (missing escalation pattern, empty group list, empty keywords,
  blank target UC, duplicate keyword, invalid regex), the full
  parity matrix across escalation phrases / hard-shift groups /
  same-UC suppression / escalation-precedence / no-drift baseline,
  the cross-group first-match-wins precedence assertion, and a
  keyword-uniqueness sanity guard.

#### M1 — Script library version pin and docs ↔ YAML consistency check

What landed:

- `server/src/main/resources/scripts/templates.yaml` now declares
  `library_version: "v1.1"` and `library_version_date: "2026-04-21"`,
  matching the latest entry of the `## 11. Version` table in
  `docs/fixed_script_library_v1.md`.
- `ScriptLibraryService` reads and exposes the version pin
  (`getLibraryVersion()`, `getLibraryVersionDate()`) and fails fast
  at startup if `library_version` is missing.
- New build-time check:
  `Sprint15ScriptLibraryConsistencyTest` parses both surfaces and
  fails the build when:
  - `library_version` / `library_version_date` is absent or blank,
  - the YAML version pin diverges from the latest doc version row,
  - any required template ID listed in the test's reference manifest
    is missing from the YAML,
  - a required template's `variables:` declaration drifts from the
    doc's variable contract,
  - a top-level template id is duplicated.

Sprint 15 §M1 explicitly does NOT rewrite script copy, change tone /
escalation wording, alter forbidden-phrase rules, or introduce a new
template engine. ScriptLibraryService substitution semantics are
unchanged except for additive metadata validation.

Tests added:

- `Sprint15ScriptLibraryConsistencyTest` (5 cases) — version pin
  presence, version + date parity vs the doc's §11 Version table,
  required-template-IDs presence, required-variables contract
  parity for every template that takes parameters, and duplicate
  top-level id detection.

#### M2 — Retrieval / answer gate / rerank fallback thresholds + diagnostics

What landed:

- New `@ConfigurationProperties("knowledge.retrieval")` bean
  `KnowledgeRetrievalProperties` carrying the previously hardcoded
  thresholds with **defaults unchanged**:
  - `ann-limit: 20`
  - `retrieval-gate-threshold: 0.3`
  - `answer-gate-threshold: 3.5`
  - `rerank-candidates: 8`
  - `top-results: 3`
  - `rerank-fallback-score: 2.5`
- Range validation runs in a `@PostConstruct` `validate()` step:
  `ann-limit ≥ rerank-candidates ≥ top-results ≥ 1`, retrieval gate
  ∈ [0.0, 1.0], answer gate ∈ [1.0, 5.0], fallback score ∈ [1.0,
  5.0], and `rerank-fallback-score < answer-gate-threshold` so a
  fallback-score result can never satisfy the answer gate.
- `application.yml` adds an explicit `knowledge.retrieval` block
  whose values match the defaults verbatim — future tuning becomes
  an auditable config diff rather than a hidden Java edit.
- `KnowledgeSearchService` no longer holds `static final`
  thresholds; it reads them per-call from the injected properties
  bean. Pipeline behaviour (ANN limit, retrieval gate test, dedup,
  rerank window, answer gate test, top-results truncation) is
  unchanged.
- `RerankService` now sources the neutral fallback score from
  `KnowledgeRetrievalProperties.rerankFallbackScore()` (default 2.5,
  unchanged) for both the parse-fallback path and the LLM-failure /
  future-failure paths.
- Diagnostics added (observability-only, no behaviour change):
  - `KnowledgeSearchService` logs `retrieval_miss` /
    `answer_miss` with `reason=retrieval_gate` or `reason=answer_gate`
    and the threshold value; on `answer_miss` it also logs
    `top_is_fallback_score=true|false` so a parse-failure /
    call-failure attribution is visible.
  - `RerankService` logs `Rerank parse fallback` with
    `reason=empty_response | unparseable` plus the fallback score
    used, and `Rerank LLM call failed` /
    `Rerank future failed` with the fallback score on the failure
    paths.

Sprint 15 §M2 explicitly does NOT tune thresholds, change FAQ
answerability semantics, change corpus content, or change eval
expected outcomes.

Tests added:

- `Sprint15KnowledgeRetrievalConfigTest` (9 cases) — defaults parity
  (each default pinned by literal value), defaults pass validation,
  and seven structural-validation negative tests (ann-limit < 1,
  ann-limit < rerank-candidates, rerank-candidates < top-results,
  retrieval gate out of range, answer gate out of range,
  fallback ≥ answer gate, fallback out of range).

Updates to existing tests for constructor signature changes:

- `RerankServiceTest` — passes the default
  `KnowledgeRetrievalProperties` instance into the constructor; the
  pre-existing parse-failure / call-failure cases remain green and
  continue to assert the 2.5 fallback score (now sourced from the
  default config).
- `Sprint14KnowledgeSearchPublishedFilterTest` — passes the default
  `KnowledgeRetrievalProperties` instance into the constructor;
  Sprint 14 §L0 published-only filter / projection contract is
  preserved and remains green.

### 16.2 Files changed (Sprint 15)

Production code:

- `server/src/main/java/com/gumtree/csagent/service/runtime/RiskKeywordsConfig.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/runtime/DriftDetector.java`
  (constructor-injects `RiskKeywordsConfig`; matching contract
  unchanged)
- `server/src/main/java/com/gumtree/csagent/service/guardrails/ScriptLibraryService.java`
  (reads `library_version` / `library_version_date`; fail-fast on
  missing pin; substitution semantics unchanged)
- `server/src/main/java/com/gumtree/csagent/config/KnowledgeRetrievalProperties.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
  (constructor-injects `KnowledgeRetrievalProperties`; reads
  thresholds from config; adds threshold-gate diagnostics; pipeline
  behaviour unchanged)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/RerankService.java`
  (constructor-injects `KnowledgeRetrievalProperties`; reads
  fallback score from config; adds parse / call / future failure
  diagnostics; scoring contract unchanged)

Config / resources:

- `server/src/main/resources/config/risk-keywords.yaml` (new)
- `server/src/main/resources/scripts/templates.yaml`
  (additive `library_version` + `library_version_date` keys; no
  template copy change)
- `server/src/main/resources/application.yml`
  (additive `knowledge.retrieval.*` block matching the previous
  hardcoded defaults verbatim)

Tests (new):

- `Sprint15RiskKeywordsConfigTest` (11 cases — config validation,
  group / keyword parity, full DriftDetector behavioural parity
  matrix, first-match-wins precedence)
- `Sprint15ScriptLibraryConsistencyTest` (5 cases — version pin
  presence, doc-vs-YAML version + date parity, required template
  IDs, required variables contract, duplicate-id guard)
- `Sprint15KnowledgeRetrievalConfigTest` (9 cases — defaults parity,
  validation positive + 7 negatives)

Tests (updated for constructor signature):

- `RerankServiceTest`
- `Sprint14KnowledgeSearchPublishedFilterTest`

Docs:

- `docs/10-handoff.md` (this section + §1 update).
- `docs/action_bank.md` (Sprint 15 deliverables added).
- `docs/sprint_objective.md` retained — already contains the
  Sprint 15 objective.
- No edit to `docs/current_eval_baseline.md` — Sprint 15 changes no
  eval expected outcome; baseline remains the post-Sprint-8 r1 / r2
  runs.

No FAQ corpus content, CaseSpec, judge, broad routing taxonomy,
handover payload contract, Salesforce backend contract, full Issue
Ledger, all-UC task taxonomy, escalation enum, system prompt, DB
schema migration, runtime main-flow architecture file, or hard
citation gate was touched.

### 16.3 Tests run

- `mvn -pl server test -Dtest='Sprint15*,DriftDetectorTest,RerankServiceTest'`
  → **55 / 0 / 0 / 0** (25 Sprint 15 §M0 / §M1 / §M2 focused tests +
  18 legacy `DriftDetectorTest` cases + 12 `RerankServiceTest`
  cases, confirming parity under the new wiring).
- `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
  Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
  Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
  Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test,Sprint14*,
  Sprint141*,Sprint15*'`
  → **332 / 0 / 0 / 0** (extended named regression suite — including
  Sprint 6 §G2 FAQ S1 guard, cs014 / cs066 / cs095 / cs002 / cs029 /
  cs176 regressions, all Sprint 14 §L0 / §L1 / §L2 + §14.1 trace
  persistence, plus the new Sprint 15 §M0 / §M1 / §M2 focused
  suite).
- `mvn -pl server test`
  → **884 / 0 / 0 / 0** (was 859 pre-Sprint-15; +25 new Sprint 15
  §M0 / §M1 / §M2 tests).
- `pytest eval_interactive/tests/` — not re-run; Sprint 15 changes
  no Python parsing path and no eval-output schema.
- Smoke runs not required — Sprint 15 changes no FAQ corpus, no
  prompt, no judge, no CaseSpec, no escalation enum, no routing
  taxonomy, no eval-output schema. The current canonical baseline
  (`docs/current_eval_baseline.md`) is preserved (post-Sprint-8 r1 /
  r2 runs).

### 16.4 Behaviour-preservation evidence

- §M0 — `Sprint15RiskKeywordsConfigTest.detect_parityMatrix_matchesHardcodedReference`
  exercises every escalation phrase, every hard-shift keyword group,
  the same-UC suppression rule, the escalation-precedence rule, and
  the no-drift baseline against the previously hardcoded
  expectations. The legacy `DriftDetectorTest` (18 cases) keeps
  passing under the new YAML-loaded path. The
  cross-group-first-match-wins assertion locks the precedence
  ordering. No DriftResult shape, type, or field has changed.
- §M1 — `ScriptLibraryService.getTemplate(...)` /
  `renderTemplate(...)` substitution semantics are unchanged. The
  approved template copy (50+ templates / 14 categories) is
  bit-for-bit identical to v1.1 of the doc; the consistency test
  enforces no silent copy drift in the variables contract.
- §M2 — `Sprint15KnowledgeRetrievalConfigTest.defaults_matchPreviouslyHardcodedConstants`
  pins each default to the literal value of the prior `static
  final` constant. `Sprint14KnowledgeSearchPublishedFilterTest`
  (Sprint 14 §L0 contract) and `RerankServiceTest` (parse / call
  failure → 2.5 fallback) keep passing under the new wiring. The
  full retrieval pipeline (ANN limit, retrieval gate test, dedup,
  rerank window, answer gate test, top-results truncation) is
  observable-equivalent under default config; the validator
  forbids any threshold combination that would change semantic
  ordering.

### 16.5 Config files added / changed

- `server/src/main/resources/config/risk-keywords.yaml` (added) —
  source of truth for `DriftDetector` escalation pattern + hard-shift
  groups.
- `server/src/main/resources/scripts/templates.yaml` (changed —
  additive `library_version`, `library_version_date` only).
- `server/src/main/resources/application.yml` (changed — additive
  `knowledge.retrieval.*` block; defaults match prior hardcoded
  constants verbatim).

### 16.6 Regression guard outcomes

All Sprint 15 active guards pass:

- `L1:escalation_reason_consistency` = **0** (never re-introduced).
- `CONTRACT_VIOLATION:active_use_case` = **0**.
- Sprint 14 `bot_turns.projected_context.faq_grounding` persistence
  trace remains intact (`Sprint141FaqGroundingTracePersistenceTest`,
  5 cases).
- Sprint 14 published-only KB search remains intact
  (`Sprint14KnowledgeSearchPublishedFilterTest`, 4 cases).
- Existing FAQ S1 guard remains green
  (`AgentRunLoopS1FaqGroundedResolveGuardTest`).
- Sprint 6 §G0 ReadTimeout no-retry closure intact.
- cs014 remains UC-C
  (`Cs014RouteAndLoopHandoverIntegrationTest`,
  `Cs014RouteAndDistressRegressionTest`).
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 distress reconciliation remains green.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested` regression remains
  green.
- All Sprint 7 / 7.1 / 8 / 8.1 / 8.2 / 9 / 9.1 / 10 / 11 / 11.1 /
  12 / 13 hard invariants remain green
  (named regression suite: 332 / 0).
- `RuntimeIntentClassifier` remains runtime-internal.
- `bot_turns.source_ids` write path unchanged.
- `bot_turns` / `bot_sessions` / `kb_articles` / `kb_chunks`
  schemas unchanged.

### 16.7 Remaining risks

**No new P0 / P1 blockers opened by Sprint 15.** The change is
config-governance + diagnostics; no runtime main-flow architecture
file gained new semantics, no DriftResult / KnowledgeSearchResult /
ScoredCandidate shape changed.

Carry-over notes:

- The `library_version` pin must be updated whenever
  `docs/fixed_script_library_v1.md` ships a new approved version
  row — `Sprint15ScriptLibraryConsistencyTest` will fail the build
  if the two surfaces diverge. This is the intended invariant.
- Future threshold tuning must land as a `knowledge.retrieval.*`
  config diff. The validator constraints
  (`fallback < answer-gate`, `ann ≥ rerank ≥ top`) are the floor,
  not a tuned recommendation.
- `RiskKeywordsConfig` is loaded once at startup. Hot-reload remains
  out of scope; Sprint 15 explicitly does NOT introduce ops-owned
  runtime config. A risk-keyword change still requires a redeploy.

Residuals carried forward from Sprint 14 §10 remain unchanged
(`R-faq-grounded-resolve-bypass`, `R-cited-but-unresolved`,
`R-resolved-but-uncited-rate`, `R-canonical-url-missing-rate`, plus
the older Sprint 13 §8 list).

### 16.8 Recommended next phase

The Sprint 15 closure surfaces no runtime regression. The previously
listed alternatives remain viable:

1. **Eval Governance docs sprint** — primary recommendation. The
   largest residual category is still `judge_volatility` /
   `faq_corpus_gap` / `product_policy_gap`, all governance-owned.
2. **Narrow Sprint 16 §S1 hardening** — only if real-traffic
   evidence demonstrates `R-faq-grounded-resolve-bypass`
   reproducibly affects answer correctness on a high-traffic UC.
3. **Narrow Corpus Curation** — fill the 38 missing
   `Help_Site_URL__c` rows. Owner: Eval Governance / corpus audit.
4. **Re-run validation after infra cleanup** — viable if Kimi /
   smoke credentials are available; runs alongside Eval Governance.

Do not start another runtime sprint unless triage finds a new
P0 / P1 runtime blocker. Sprint 15 explicitly does NOT promote a
new canonical eval baseline.

### 16.9 What Sprint 15 explicitly does NOT do

- No new risk semantics, no new escalation reasons, no new risk
  levels, no auto-handover for Level 1 / Level 2 risk signals.
- No new hard Java guard.
- No prompt rewrite, no system prompt edit, no broad routing
  rewrite.
- No hard runtime citation gate.
- No FAQ corpus rewrite.
- No judge calibration, no CaseSpec changes, no eval expansion.
- No anchor / exploration / promotion hard-gate expansion.
- No hot reload / ops-owned runtime config.
- No dashboard work, no full TraceViewer redesign.
- No new skill runtime framework.
- No broad S1 rewrite.
- No DB migration. No schema change. No bot_turns / bot_sessions /
  kb_articles / kb_chunks edit.
- No threshold tuning. Defaults are pinned to the previous
  hardcoded values verbatim.

## 17. Sprint 16 — Handover Exactly-Once Contract and Repro

Status: **landed (docs + characterization-test sprint, no runtime
change)**. Sprint 16 freezes the handover exactly-once contract and
adds the characterization tests that demonstrate the current
LLM-driven dual local-persistence shape, without modifying any
runtime behaviour. It does not yet implement
`HandoverOrchestrator`; that is the explicitly deferred future
runtime sprint trigger.

### 17.1 Exact docs / tests changed

Docs (new + edited):

- `docs/proposals/handover_orchestrator_design.md` (new) — defines the
  exactly-once contract. §1 names the current dual-path shape
  (writer #1 = `RequestHandoverTool` →
  `SalesforceService.requestHandover` →
  `MockSalesforceService` → `mock_handover_log` row #1; writer #2
  = `SessionManager.recordHandover` → direct
  `handoverLogRepository.save(...)` → `mock_handover_log` row
  #2). §2 distinguishes confirmed local persistence duplication
  from unproven real Salesforce double transfer. §3 freezes three
  contracts: trace evidence (`request_handover`), outcome
  persistence (`record_outcome`), and the future
  `HandoverOrchestrator` handover side-effect (idempotent by
  `session_id`). §4 sketches the future orchestrator shape. §5
  records why Sprint 16 is docs + characterization only. §6
  pre-specifies acceptance criteria for the future runtime sprint.
- `docs/runtime_freeze_and_risk_policy.md` (edited) — added §10
  "Known unspecced surfaces" with §10.1 entry covering the
  handover dual-path. Renumbered References to §11; added
  cross-reference links to the Sprint 16 design / release_gate /
  source files.
- `docs/release_gate.md` (new) — opens the release-gate ledger.
  §1.1 blocking rule: before real Salesforce cutover, the
  handover side-effect must be idempotent by `session_id`. The
  rule is **not satisfied** at Sprint 16 close.
- `docs/action_bank.md` (edited) — Sprint 16 row added to §3
  active deliverables (H0 / H1 / H2); new
  D-single-handover-orchestrator entry in §4 deferred runtime
  candidates ("Single Handover Orchestrator: exactly-once
  side-effect owner before Salesforce production cutover");
  Sprint 16 row added to §6 closed-action index.
- `docs/sprint_objective.md` retained — already contains the
  Sprint 16 objective.
- `docs/10-handoff.md` (this file) — §1 and new §17 (this
  section).

Tests (new):

- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint16HandoverDualPathReproTest.java`
  (3 cases): two passing characterization cases plus one
  `@Disabled` future-invariant case.
  - `dualLocalPersistence_llmDrivenRequestHandoverPath_producesTwoMockHandoverLogWrites_currentBehaviour`
    (passing) — exercises `RequestHandoverTool.execute(...)` with
    a real `MockSalesforceService` backed by a mocked
    `MockHandoverLogRepository`, then replays the
    `SessionManager.recordHandover` surface (real
    `HandoverPayloadAssembler` → direct `repo.save(...)`).
    Asserts two saves on the same repository for the same
    `session_id`; asserts the two payloads carry distinct
    `version` (`"1.0"` vs `"1.1"`) and distinct `transfer_result`
    (`MockSalesforceService` set vs `mock_transfer` literal).
  - `distinguishesLocalPersistenceDuplication_fromUnprovenRealSalesforceDoubleTransfer`
    (passing) — pins that today the only `SalesforceService`
    implementation is `MockSalesforceService`. Designed to break
    on first wire-up of a real Salesforce client so the reviewer
    is forced to re-read the §10.1 contract before passing the
    release gate.
  - `futureInvariant_atMostOneTransmittedHandoverDecisionPerSessionId_disabledUntilOrchestratorLands`
    (`@Disabled`, TODO) — encodes the future invariant: at most
    one transmitted / `offline_logged` handover decision per
    `session_id`. Would fail today by design (the LLM-driven
    path writes twice). Removing the `@Disabled` annotation is
    listed as one of the acceptance criteria for the future
    "Single Handover Orchestrator" runtime sprint
    (`docs/release_gate.md` §1.1 #4-#5).

Source comments (new, no behaviour change):

- `server/src/main/java/com/gumtree/csagent/service/tools/RequestHandoverTool.java`
  — class-level Javadoc carries a Sprint 16 §H0
  known-unspecced-surface marker pointing to
  `docs/proposals/handover_orchestrator_design.md` and the future-invariant
  test.
- `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java`
  — `recordHandover(...)` carries the matching marker on its
  Javadoc.

### 17.2 Current confirmed risk

- **Duplicated local persistence (P2 today, mock / local).** On
  the LLM-driven `request_handover` path two
  `mock_handover_log` rows are written for the same `session_id`
  on the same ESCALATE turn. The two writers are unaware of each
  other and persist payloads with different `version` and
  `transfer_result` shapes.
- **P1 launch-readiness severity.** The dual-path shape is one
  wire-up away from a real double transfer at the moment a
  production `SalesforceService` implementation lands.
- **P0 / P1 production-incident severity** if a real double
  transfer occurs after cutover (case re-routed twice in
  Salesforce: re-assignment, mis-prioritisation, queue
  duplication; user-visible).
- The hard-OOS path (`SessionManager.createSession` → synthetic
  `request_handover` evidence + single `recordHandover`) and the
  kernel force-escalate / Step 2.5 path remain single-write today
  (pinned by Sprint 16 §H0 contract; not opened or weakened by
  Sprint 16).

### 17.3 What is not confirmed

- A real **double Salesforce transfer** is not currently proven.
  The production Salesforce client is not wired; only
  `MockSalesforceService` (`@Profile("local")`) implements
  `SalesforceService`, and `SessionManager.recordHandover` does
  not flow through `SalesforceService` at all (it writes directly
  to `mock_handover_log`).
- `HandoverPayloadAssembler` is the candidate single payload
  builder for the future orchestrator. Sprint 16 does not commit
  to it as the final shape; the orchestrator design doc names it
  as the candidate but leaves payload-schema decisions to the
  runtime sprint.
- No production case routing is currently affected. The
  characterization test runs against an in-memory mocked
  repository.

### 17.4 Future runtime sprint trigger

A future "Single Handover Orchestrator" runtime sprint must start
when either:

1. a real Salesforce client is staged for cutover (the launch-time
   wire-up forces the orchestrator to land first to satisfy
   `docs/release_gate.md` §1.1), or
2. a real-traffic case shows duplicated handover routing in
   Salesforce (would be the first P0 / P1 confirmation that
   moves the issue out of P2 territory).

Until then, `Sprint16HandoverDualPathReproTest` is the standing
guard that the dual-path shape has not been silently fixed (the
two passing cases would break) and that the future invariant has
not been silently re-introduced as a hard gate (the `@Disabled`
case would need to be re-enabled deliberately).

The runtime sprint's acceptance criteria are pre-specified in
`docs/proposals/handover_orchestrator_design.md` §6 and
`docs/release_gate.md` §1.1.

### 17.5 Tests run

- `mvn -pl server test
  -Dtest='Sprint16*,RequestHandoverToolTest,HandoverPayloadAssemblerTest,Cs014RouteAndLoopHandoverIntegrationTest,Cs176ExplicitHumanHelpHandoverIntegrationTest,AgentRunLoopHandoverReasonNormalizationIntegrationTest'`
  → **22 / 0 / 0 / 1** (Sprint 16 §H1 focused suite + handover
  regression suite; the 1 skipped is the `@Disabled`
  future-invariant case).
- `mvn -pl server test`
  → **887 / 0 / 0 / 1** (was 884 pre-Sprint-16; +2 new passing
  Sprint 16 tests + 1 new disabled future-invariant test). No
  regression in any Sprint 6 / 7 / 7.1 / 8 / 8.2 / 9 / 9.1 / 10
  / 11 / 11.1 / 12 / 13 / 14 / 14.1 / 15 invariant. The cs014 /
  cs066 / cs095 / cs002 / cs029 / cs176 / cs001 regression suite
  remains green. `L1:escalation_reason_consistency = 0` and
  `CONTRACT_VIOLATION:active_use_case = 0` are preserved.
- `pytest eval_interactive/tests/` — not re-run; Sprint 16
  changes no Python file, no eval-output schema, no judge, no
  CaseSpec, no FAQ corpus. The Sprint 16 diff is two Markdown
  files (new), three Markdown files (edited), one new Java test,
  and two Javadoc-only edits on existing Java source.
- Smoke runs not required — Sprint 16 changes no FAQ corpus, no
  prompt, no judge, no CaseSpec, no escalation enum, no routing
  taxonomy, no eval-output schema, no DB schema. The current
  canonical baseline (`docs/current_eval_baseline.md`) is
  preserved (post-Sprint-8 r1 / r2 runs).

### 17.6 Next recommended action

Recommended next phase: **defer**. Sprint 16 is intentionally a
docs + characterization sprint; the runtime fix
("Single Handover Orchestrator") is queued as a deferred runtime
candidate (`docs/action_bank.md` §4 row
`D-single-handover-orchestrator`) and gated by the real-Salesforce
cutover plan (`docs/release_gate.md` §1.1).

In priority order:

1. **Eval Governance Follow-up** — primary recommendation
   carried over from Sprint 15. Largest residual category is
   still `judge_volatility` / `faq_corpus_gap` /
   `product_policy_gap`.
2. **Single Handover Orchestrator runtime sprint** — start ONLY
   when (a) a real Salesforce client is staged for cutover, or
   (b) a real-traffic case shows duplicated handover routing.
   Acceptance criteria pre-specified in
   `docs/proposals/handover_orchestrator_design.md` §6 and
   `docs/release_gate.md` §1.1; the `@Disabled` future-invariant
   test is the closing artifact.
3. **Narrow Sprint 16 §S1 hardening** (FAQ-grounded resolve
   bypass) — only on real-traffic evidence per
   `docs/current/faq_grounding_contract.md` §6.
4. **Narrow Corpus Curation** — fill the 38 missing
   `Help_Site_URL__c` rows.

Do not start the Single Handover Orchestrator runtime sprint
opportunistically. Sprint 13's runtime freeze remains in force; a
new runtime sprint requires a P0 / P1 trigger or the real
Salesforce cutover plan.

### 17.7 What Sprint 16 explicitly does NOT do

- No `HandoverOrchestrator` implementation. Sprint 16 is design
  + characterization only; the runtime fix is the future
  "Single Handover Orchestrator" sprint.
- No production Salesforce client. The release-gate rule
  (`docs/release_gate.md` §1.1) blocks that wire-up until the
  orchestrator lands.
- No change to `RequestHandoverTool` behaviour. Class-level
  Javadoc adds a §H0 marker only.
- No change to `SessionManager.recordHandover` behaviour. Method
  Javadoc adds a §H0 marker only.
- No change to `MockSalesforceService` behaviour or the
  `SalesforceService` interface.
- No change to the Phase 3 §3.6.2 handover payload schema.
- No change to `HandoverPayloadAssembler` behaviour.
- No prompt edit. No `system_prompt.txt` change.
- No routing change. No new escalation reason. No CaseSpec
  change. No FAQ corpus change. No judge calibration change. No
  eval expected-outcome change.
- No DB migration. No schema change. `mock_handover_log` /
  `bot_turns` / `bot_sessions` / `kb_articles` / `kb_chunks`
  schemas unchanged.
- No new live smoke baseline. No anchor / exploration /
  promotion hard-gate change.
