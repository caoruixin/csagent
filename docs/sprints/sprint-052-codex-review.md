# Sprint 52 / M5 S3 — Codex per-sub-sprint review (archive — both passes)

Per-sub-sprint Codex review of M5 S3 (projection audit C1 + Skill-driven
convergence C2), the milestone's only semantic surface (per
`docs/milestone_objective.md` §8 + `iteration_governance.md` §4.3). Two passes:
a first pass that returned `fix_required / 2` on close-package TIMING artifacts
(rerun evidence not yet recorded + dev scope not yet committed — NOT dev-code
issues; the code / anti-hardcode axis was clean), and a re-review that returned
**`pass / 0`** over the immutable range `cf9c120..49d48b1` after the
deliver-agent recorded the rerun evidence + reconciled the P3 doc-drift and the
human committed the S3 scope. Final verdict: **pass / 0** → S3 close **A — Clean
PASS**. Prompts: `compact/sprint-052-codex-review-prompt.md` (first pass) +
`compact/sprint-052-codex-rereview-prompt.md` (re-review).

---

## RE-REVIEW (final — pass / 0)

## Sprint Review Decision
decision: pass
blocking_count: 0
summary: Re-review verdict: P0 rerun-evidence is resolved because the S3 real-LLM bad-case rerun is now recorded in the manifest and handoff, reproduces the M4-close distribution (PASSx5 / IMPROVINGx4 / FAILx3 / OOSRx0), and the fg5q + iwzx session-establishment flakes clear on isolated rerun; P1 committed-range is resolved because this review is against immutable range `cf9c120..49d48b1`; P3 doc drift is reconciled because `runtime_contract.md` now records C2 #3 option (b), with `moderation_context` stripped from the FAQ Skill declaration while `session.moderationContext` remains session-internal / handover-only. I found no new substantive issue in the committed range and re-affirm the first-pass clean code / anti-hardcode verdict.

### Re-Review Scope Result

| First-pass item | Re-review verdict | Evidence |
|---|---|---|
| P0 — missing real-LLM rerun evidence | RESOLVED | `_manifest.md` records the M5 / S3 close rerun; `sprint-052-handoff.md` §12.2 records the same gate. |
| P1 — absent committed range | RESOLVED | `git rev-parse --short HEAD` → `49d48b1`; `git log --oneline cf9c120..HEAD` shows the S3 close commit. |
| P3 — `runtime_contract.md` still named `moderation_context` | RESOLVED | `runtime_contract.md` records C2 #3 option (b); `resolve_faq_grounded_answer.yaml` now lists only `form_context`, `customer_context`, `listing_context`. |

### P0 Evidence Walk (re-review)
- Manifest "M5 / S3 (Sprint 52) close" section scopes the rerun as the C2 #3/#4 semantic-preservation gate (deliver-agent, 2026-05-25, S3 build, human-review authority).
- Run paths: main `results/20260524-172654/`; iwzx isolated clear `20260524-173652`; fg5q isolated attempts `20260524-173645` + `20260524-173810` + clear `20260524-173818`.
- Distribution reproduces the M4-close baseline: PASS×5 (cs001, cs014, cs029, cs066, fg5q) + IMPROVING×4 (alice, cs011, cs012, wmkb) + FAIL×3 (cs015, cs095, iwzx) + OOSR×0.
- Decisive signal stable: every establishing case `containment_outcome=escalated`; the two `CONTRACT_VIOLATION` cases clear on isolated rerun with escalation-shaped outcomes (jq spot-checks confirmed fg5q main `active_use_case missing_after_turns` @0 turns; iwzx isolated `UC-H / escalated / service_degraded / 4 turns`; fg5q 3rd isolated `UC-A / escalated / faq_miss_threshold_exceeded / 3 turns / composite=0.5 / case_passed=true`). Flake is upstream of C2 #4 `tool_schemas` projection; consistent with `R-bad-case-parallel-session-establishment-flakiness`.

### P1 Range / Scope Walk (re-review)
- Immutable range `cf9c120..49d48b1`. `git diff --numstat` matches S3 dev scope (`ContextProjectionBuilder.java` +59/-13, `resolve_faq_grounded_answer.yaml` -1, `PhaseEvaluatorResolveSkillIntegrationTest.java` +1/-2, new `Sprint52ProjectionSkillDrivenTest.java` +336, new C1 matrix +321, new handoff +580) plus deliver close-prep (`_manifest.md`, `runtime_contract.md`, the two review-prompt artefacts).
- Forbidden-surface spot-check: no committed change to `system_prompt.txt`, `eval/`, `data/`, `ui/`, `AgentRunLoopImpl.java`, `TraceWriter.java`, `ControlKernel.java`; only the one-line Skill-YAML strip.

### Code / Anti-Hardcode Re-Affirmation
- C2 #4 registry/Skill-driven: `resolveProjectedToolNames(session, activeUc)` → `skillRegistry.select(phase, activeUseCase)` → `Skill.toolsRequired()`, single defensive fallback to `getVisibleToolsForUc(activeUseCase)` for unmapped tuples (not a per-UC branch / keyword / regex / enum). Run-loop path still overwrites `tool_schemas` from `plan.allowedTools()` = `skill.toolsRequired()` (mapped Skills → byte-identical end state).
- C2 #3 declaration-only; no prompt/procedure/eval-fixture/judge/tool-schema-def/PII/grounding-overlay/scoring change. C2 #2 + #5 remain STOP-surfaced (not landed).

### §4.1 Kernel Re-Affirmation
Q1-Q9 all PASS (Q8 PASS for close: Java wiring tests pass + real-LLM rerun evidence now recorded and reproduces the baseline).

### §6 OQ Dispositions Re-Affirmed
- OQ-S52.1 (#2 context-key gating): defer-to-S4 / R-item if S4 not scheduled (STOP correct).
- OQ-S52.2 (#5 soft-signal gating): defer-to-S4 with real-LLM rerun prerequisite.
- OQ-S52.3 (OQ-S51.2 overlay): closed-with-followup (overlay untouched, load-bearing).
- OQ-S52.4 (`knowledge_hits` vs `accumulated_tool_results`): defer-to-S4 / M5+ canonicalization.

### Verification Commands (re-review)
- `git rev-parse --short HEAD` → `49d48b1`; `git log --oneline cf9c120..HEAD` shows the S3 close commit.
- `jq` over `results/20260524-172654`, `…173652`, `…173818` confirmed flake/clear shape + escalation containment.
- `mvn test -B` → `Tests run: 1172, Failures: 1, Errors: 0, Skipped: 2` (only the inherited `SystemPromptUserRequestedTiebreakerTest`); `Sprint52ProjectionSkillDrivenTest` 7/0/0/0; `PhaseEvaluatorResolveSkillIntegrationTest` 14/0/0/0.

### Residual Risks / Follow-Ups (re-review)
- The session-establishment flake now appears on cs029 (M4) + fg5q + iwzx (S3); upstream of S3 C2, not a code blocker; the manifest's recommendation to prioritize `R-bad-case-parallel-session-establishment-flakiness` is reasonable.
- Future new Skills/phases should keep coverage around the unmapped-tuple fallback so registry-coverage drift does not silently revive UC-palette projection as the common path.

---

## FIRST PASS (superseded — fix_required / 2; both blockers = close-package timing)

## Sprint Review Decision
decision: fix_required
blocking_count: 2
summary: S3's landed code shape is directionally coherent and I found no new semantic hardcode in the C2 #4 `tool_schemas` convergence or the C2 #3 `moderation_context` declaration removal: the helper is registry/Skill-driven with one defensive unmapped-tuple fallback, no prompt/eval/fixture surface moved, C1 correctly justifies replacing-not-deleting the consumed base path, and #2/#5 were correctly STOP-surfaced. S3 cannot close PASS in this checkout because the review was not against the promised immutable `cf9c120..<S3-dev-commit>` range (HEAD is still `cf9c120`, with S3 files uncommitted/untracked) and the required real-LLM bad-case rerun evidence is absent from `docs/sprints/sprint-052-handoff.md` §12 and `bad_cases/_manifest.md`, so the §5.6 semantic-preservation gate for C2 #4 cannot be verified.

### Blocking Findings (first pass)
1. **P0 — Missing S3 real-LLM rerun evidence blocks the semantic-preservation gate.** The S3 contract + milestone §5 make the real-LLM bad-case rerun the C2 evidence gate; at review time the handoff §12 still said reserved/STOP-surfaced and `_manifest.md` had no S3/M5 rerun section. (RESOLVED by the re-review — evidence recorded.)
2. **P1 — Requested commit range absent.** HEAD was `cf9c120`; S3 code/docs were unstaged/untracked → a working-tree review, not the immutable `cf9c120..<S3-dev-commit>` range. (RESOLVED by the re-review — committed as `49d48b1`.)

### §3 Nine-Question Kernel (first pass — code axis clean)
- Q1 PASS (registry/Skill-driven helper; single defensive fallback; no per-UC branch/keyword/regex/enum; C2 #3 = YAML declaration deletion only).
- Q2 PASS/N/A (no new Tier-0 / Java guard). Q3 PASS (Runtime-owned projection source → Skill registry data; no LLM-owned hard branch). Q4 PASS (no eval text/CaseSpec id). Q5 PASS for landed code (semantic-planner ownership unchanged). Q6 PASS (empty `system_prompt.txt` diff). Q7 PASS for code shape; the legacy `buildProjection(...)` path was the only thing needing the rerun evidence. Q8 FIX-REQUIRED solely because the real-LLM rerun was absent (mocked tests cover wiring only). Q9 PASS (durable convergence; #2/#5 STOP-surfaced).

### §4 Hard-Fence Walk (first pass) — all PASS for code
C1-before-C2 (matrix exists + human-reviewed), registry/Skill-driven (no per-UC if-else), no field deleted before C1 confirms (#3 no-consumer, #4 replace-not-delete), OQ-S51.2 overlay untouched, no enum/tool-schema/PII/safety/grounding/scoring change, #2+#5 STOP honored. Commit-timing (P1) was the only fence-side blocker.

### §5 C1-Matrix Consistency (first pass) — claims verified
C1 #3 zero-consumer claim PASS (no `moderation_context` projection reader; the `get_message_moderation_context` tool + `BotSession.moderationContext` field are separate concepts). C1 #4 consumed-base claim PASS (`PhaseEvaluator.evaluate*` + `ControlKernel.recordTurn` confirmed callers). FAQ Skill `allowedTools` spot-check PASS (golden unchanged). Rerun evidence was the only missing item.

### Optional Codex-Surfaced (first pass)
- **P3 — `runtime_contract.md` doc-drift after C2 #3** (note-only): the doc still named `moderation_context` in the FAQ RESOLVE `requiredContextKeys`. (RESOLVED by the deliver-agent before the re-review — reconciled to option (b).)

### Coherence Judgment (first pass)
S3 is one coherent Skill-driven convergence (C1 identified `moderation_context` declaration-only/no-consumer + `tool_schemas` as a consumed base; C2 strips only the stale declaration and replaces — not deletes — the base with the same Skill-registry source `PhasePlan.allowedTools()` already uses). The `getVisibleToolsForUc` fallback is genuinely defensive for unmapped tuples, not a new semantic branch; future Skill-coverage drift should be tested.
