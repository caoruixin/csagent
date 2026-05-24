## Sprint Review Decision
decision: fix_required
blocking_count: 2
summary: S3's landed code shape is directionally coherent and I found no new semantic hardcode in the C2 #4 `tool_schemas` convergence or the C2 #3 `moderation_context` declaration removal: the helper is registry/Skill-driven with one defensive unmapped-tuple fallback, no prompt/eval/fixture surface moved, C1 correctly justifies replacing-not-deleting the consumed base path, and #2/#5 were correctly STOP-surfaced. S3 cannot close PASS in this checkout because the review was not against the promised immutable `cf9c120..<S3-dev-commit>` range (HEAD is still `cf9c120`, with S3 files uncommitted/untracked) and the required real-LLM bad-case rerun evidence is absent from `docs/sprints/sprint-052-handoff.md` §12 and `bad_cases/_manifest.md`, so the §5.6 semantic-preservation gate for C2 #4 cannot be verified.

## Blocking Findings

1. **P0 - Missing S3 real-LLM rerun evidence blocks the semantic-preservation gate.** `docs/sprint_objective.md:162` makes the real-LLM bad-case suite rerun the S3 evidence gate and requires the M4-close distribution to hold; `docs/milestone_objective.md:163` makes the same distribution a hard regression-safety gate for M5. The only landed S3 handoff text still says the rerun is STOP-surfaced/reserved (`docs/sprints/sprint-052-handoff.md:475`, `docs/sprints/sprint-052-handoff.md:505`), and `eval_interactive/case_specs/bad_cases/_manifest.md:198` contains the M4 baseline but no S3/M5 rerun section. Because C2 #4 intentionally changes the cold-edge legacy `buildProjection(...)` tool palette to the Skill set, Codex cannot verify the requested PASSx5 / IMPROVINGx4 / FAILx3 / OOSRx0 preservation claim.

2. **P1 - Requested commit range is absent, so this is a working-tree review rather than the promised S3 commit-range review.** The review prompt requires `cf9c120..<S3-dev-commit>`, but `git rev-parse --short HEAD` returns `cf9c120`; `git status --short` shows S3 code/docs as unstaged/untracked, including `docs/sprints/sprint-052-handoff.md` and `docs/diagnostics/m5-s3-projection-consumption-map.md`. The untracked `compact/sprint-052-codex-review-prompt.md` is also outside the expected S3 dev-scope file list. I reviewed the working-tree delta against `cf9c120`; the result must not be treated as an immutable commit-range approval until the intended dev commit exists and the rerun evidence is recorded.

## §3 Nine-Question Anti-Hardcode Kernel Results

1. **Q1 - Semantic hardcode added? PASS for code shape.** The load-bearing helper calls `skillRegistry.select(phase, activeUseCase)` and returns `Skill.toolsRequired()` when present, with only null guards and the single fallback to `toolPolicyEnforcer.getVisibleToolsForUc(activeUseCase)` when no Skill maps the tuple (`server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:1385`, `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:1391`, `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:1400`). The projection loop consumes that helper generically (`server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:630`); I found no new `if (uc.equals(...))`, regex, keyword, tool-name routing branch, or CaseSpec-specific runtime rule in the tracked diff. C2 #3 is a YAML declaration deletion only (`server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:20`).

   Diff snippet reviewed:

   ```java
   private List<String> resolveProjectedToolNames(BotSession session, String activeUseCase) {
       if (session == null) {
           return Collections.emptyList();
       }
       String phase = session.getCurrentPhase();
       if (skillRegistry != null) {
           Optional<Skill> selected = skillRegistry.select(phase, activeUseCase);
           if (selected.isPresent()) {
               List<String> toolsRequired = selected.get().toolsRequired();
               return toolsRequired == null ? Collections.emptyList() : toolsRequired;
           }
       }
       if (activeUseCase == null) {
           return Collections.emptyList();
       }
       return toolPolicyEnforcer.getVisibleToolsForUc(activeUseCase);
   }
   ```

2. **Q2 - Tier-0 invariant protection? PASS / N/A.** No new Tier-0 invariant or Java guard is added. The runtime-freeze Tier-0 surfaces in `docs/runtime_freeze_and_risk_policy.md` §1/§2 are not widened, and the change stays inside the Runtime-owned tool whitelist/projection source-of-truth boundary.

3. **Q3 - Soft signal vs hard branch? PASS.** C2 #4 moves a Runtime-owned tool-schema projection source from a UC palette to Skill registry declarations; it does not hard-branch over a semantic decision the LLM owns. `SkillRegistry.select` itself is data/registry selection with exact and wildcard lookup, returning empty on miss (`server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java:65`).

4. **Q4 - Eval phrase / CaseSpec id encoded? PASS.** The tracked runtime/YAML diff contains no visible-eval case IDs or trace phrases; the only suspicious scan hits are existing imports/comments and the deleted `moderation_context` key. No `eval_interactive/**`, `eval/**`, `data/**`, `composite.py`, or eval-fixture path changed.

5. **Q5 - LLM ownership shrunk? PASS for the landed code; evidence gate still missing.** The bot's semantic decisions (UC hypothesis, drift/topic shift, next action, escalation posture, response strategy) are untouched. C2 #4 consolidates the Runtime-owned tool whitelist at the Skill registry (`docs/current/iteration_governance.md` §1.4), and C2 #3 removes a declaration the projection never emitted.

6. **Q6 - Prompt if-else added? PASS.** `server/src/main/resources/prompts/system_prompt.txt` has an empty diff. No Skill `procedure`, `critical_steps[].desc`, or other Skill YAML body changed beyond the single `required_context_keys` deletion in `resolve_faq_grounded_answer.yaml`.

7. **Q7 - Tool schema / capability / PII / grounding floor preserved? BLOCKED on real-LLM evidence, code shape otherwise PASS.** For mapped Skills, the helper returns `Skill.toolsRequired()`, and `PhaseEvaluator.composeSkillPhasePlan(...)` puts the same `skill.toolsRequired()` into `PhasePlan.allowedTools()` (`server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:450`, `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:454`). The plan-aware run-loop overwrite is unchanged and still rewrites `tool_schemas` from `plan.allowedTools()` (`server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:851`, `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:868`). PII redaction and the FAQ-grounding overlay are untouched (`server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java:2029`, `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java:2363`). The missing rerun evidence is the remaining blocker for the legacy direct-`buildProjection(...)` paths.

8. **Q8 - Generalization coverage? FIX REQUIRED.** The seven new wiring/rendering tests exist and pass (`Sprint52ProjectionSkillDrivenTest`: Skill-match/no fallback, unmatched fallback, null-UC no-wildcard empty, null-UC wildcard Skill, plan overwrite no-op, no `moderation_context` emit, YAML declaration absent). `mvn test -B` confirms the suite-level count `1172 / 1 / 0 / 2` with only the inherited `SystemPromptUserRequestedTiebreakerTest` failure, and the new S52 test class is `7 / 0 / 0 / 0`. However, per `docs/sprint_objective.md:123` and `docs/sprint_objective.md:162`, mocked/unit tests cover wiring only; the real-LLM rerun is the semantic-preservation gate and is absent.

9. **Q9 - Rollback / sunset? PASS.** C2 #3 and #4 are intended durable M2-correct convergence, not temporary. A normal revert of the S3 dev commit would reverse both landed changes once that commit exists; #2 and #5 remain STOP-surfaced continuation items.

## §4 Hard-Fence Walk

| Fence | Verdict | Evidence |
|---|---|---|
| C1 BEFORE C2 | PASS for artifacts, process evidence not independently timestamped in git because no S3 commit exists | The matrix exists and is the gate artifact (`docs/diagnostics/m5-s3-projection-consumption-map.md:25`); the handoff records C1 human review before C2 (`docs/sprints/sprint-052-handoff.md:454`). Commit timing remains a blocker because the expected S3 dev commit is absent. |
| Registry/Skill-driven, no per-UC if-else | PASS | `resolveProjectedToolNames` selects via `SkillRegistry` then uses one unmapped fallback (`ContextProjectionBuilder.java:1391`, `ContextProjectionBuilder.java:1400`); no new UC-specific branch landed. |
| No field deleted/changed before C1 confirms safety | PASS for #3/#4; #2/#5 deferred | `moderation_context` is safe-to-resolve as a never-emitted declaration with no consumer (`docs/diagnostics/m5-s3-projection-consumption-map.md:223`); `tool_schemas` base is consumed, so C2 #4 replaces rather than deletes (`docs/diagnostics/m5-s3-projection-consumption-map.md:175`). |
| No change to LLM-visible semantic information for run-loop primary path | PASS for code, BLOCKED for legacy-path evidence | The run-loop overwrite remains `plan.allowedTools()`-based (`ContextProjectionBuilder.java:851`); the direct legacy callers are real and still consume `buildProjection(...)` (`PhaseEvaluator.java:858`, `PhaseEvaluator.java:982`, `ControlKernel.java:1670`), so the missing real-LLM rerun blocks close. |
| OQ-S51.2 FAQ-grounding overlay untouched | PASS | No `ControlKernel` diff; overlay merge remains at `ControlKernel.java:2029` and `ControlKernel.java:2363`. |
| No enum/tool-schema definition/PII/safety/grounding/eval-scoring change | PASS | Empty diffs for `eval_interactive/`, `eval/`, `data/`, `ui/`, `system_prompt.txt`, `AgentRunLoopImpl`, `TraceWriter`, and `ControlKernel`; only one Skill YAML changed. |
| #2 + #5 STOP-and-surface honored | PASS | No `requiredContextKeys`-driven projection gating or `softSignalViaProjection` gating appears in the `ContextProjectionBuilder` diff; the matrix flags #2/#5 STOP (`docs/diagnostics/m5-s3-projection-consumption-map.md:231`, `docs/diagnostics/m5-s3-projection-consumption-map.md:232`). |

## §5 Real-LLM Rerun + C1 Matrix Consistency

- **Rerun evidence: FAIL / missing.** The M4 baseline is documented at `eval_interactive/case_specs/bad_cases/_manifest.md:226` through `eval_interactive/case_specs/bad_cases/_manifest.md:233`, but there is no S3/M5 rerun section and handoff §12 is still reserved (`docs/sprints/sprint-052-handoff.md:505`). This is the primary blocker.
- **C1 #3 zero-consumer claim: PASS for runtime/eval surfaces checked.** `system_prompt.txt` has no `moderation_context` mention; `ContextProjectionBuilder.buildDriftAndTaskHistory` copies only drift/task fields (`ContextProjectionBuilder.java:1287`, `ContextProjectionBuilder.java:1304`); no production Java projection reader uses `"moderation_context"` beyond the session column on `BotSession` (`server/src/main/java/com/gumtree/csagent/model/BotSession.java:114`); and no other Skill YAML declares `moderation_context` after the strip. The `get_message_moderation_context` tool and `BotSession.moderationContext` remain separate session/tool concepts, not consumers of a projection slot.
- **C1 #4 consumed-base claim: PASS.** The matrix says `buildProjection(...)` is consumed by legacy `PhaseEvaluator.evaluate*` paths and `ControlKernel.recordTurn` (`docs/diagnostics/m5-s3-projection-consumption-map.md:69`); grep confirms direct callers at `PhaseEvaluator.java:858`, `PhaseEvaluator.java:982`, `PhaseEvaluator.java:1025`, `PhaseEvaluator.java:1105`, `PhaseEvaluator.java:1373`, and `ControlKernel.java:1670`.
- **FAQ Skill allowed-tools spot-check: PASS.** The production FAQ Skill still declares `get_customer_context`, `search_knowledge`, `resolve_article`, `record_outcome`, `request_handover` (`server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:13`), and the integration golden still expects exactly that `plan.allowedTools()` list (`server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorResolveSkillIntegrationTest.java:146`).

## §6 OQ Disposition

| OQ | Assessment | Recommended disposition |
|---|---|---|
| OQ-S52.1 - C2 #2 Skill-declared context-key gating | STOP was correct, not over-cautious: the matrix shows `discover_triage` does not declare `customer_context` and only FAQ declares `listing_context`, so immediate gating would remove LLM-visible context from plausible DISCOVER/intake/confirm paths (`docs/diagnostics/m5-s3-projection-consumption-map.md:183`, `docs/diagnostics/m5-s3-projection-consumption-map.md:184`). | `defer-to-S4` with a prerequisite Skill-declaration audit + real-LLM rerun; if S4 is not scheduled, `surface-as-r-item`. |
| OQ-S52.2 - C2 #5 soft-signal gating | Correct convergence shape (Skill `state_inheritance.soft_signal_via_projection`) but still behaviour-risky because it changes explicit-null/empty soft-signal visibility across phase boundaries (`docs/diagnostics/m5-s3-projection-consumption-map.md:125`, `docs/diagnostics/m5-s3-projection-consumption-map.md:127`). | `defer-to-S4`; require the DISCOVER->RESOLVE bad-case rerun evidence before landing. |
| OQ-S52.3 - OQ-S51.2 FAQ-grounding overlay | Human option (a) leave-as-is is consistent with the matrix: the overlay is pre-existing, not LLM-seen in raw per-step projections, not read by scorers, but load-bearing for persistence tests (`docs/diagnostics/m5-s3-projection-consumption-map.md:276`, `docs/diagnostics/m5-s3-projection-consumption-map.md:281`). | `closed-with-followup`; open an R-item only if the human wants to canonicalize overlay storage later. |
| OQ-S52.4 - `knowledge_hits` / `accumulated_tool_results` dual path | `knowledge_hits` is not dead because legacy FAQ paths pass non-null hits (`docs/diagnostics/m5-s3-projection-consumption-map.md:196`), while run-loop knowledge lands in `accumulated_tool_results` (`docs/diagnostics/m5-s3-projection-consumption-map.md:211`). This is a canonical-path question, not an S3 blocker. | `defer-to-S4` if S4 runs; otherwise `surface-as-r-item` for M5+ projection canonicalization. |

## Reproducibility Spot-Check

- **Range / status:** `git rev-parse --short HEAD` -> `cf9c120`; S3 files are uncommitted/untracked. This does not match the requested `cf9c120..<S3-dev-commit>` immutable range.
- **Footprint numstat:** tracked diff matches handoff for the three tracked files: `ContextProjectionBuilder.java` `59/13`, `resolve_faq_grounded_answer.yaml` `0/1`, `PhaseEvaluatorResolveSkillIntegrationTest.java` `1/2`. Untracked S3 files have the cited sizes: matrix `321` lines, handoff `508` lines, new test `336` lines. Extra untracked `compact/sprint-052-codex-review-prompt.md` is outside the expected S3 dev-scope list.
- **Forbidden scope paths:** empty diffs for `eval_interactive/`, `eval/`, `data/`, `ui/`, `system_prompt.txt`, `AgentRunLoopImpl`, `TraceWriter`, and `ControlKernel`; only `resolve_faq_grounded_answer.yaml` changed under `server/src/main/resources/skills/`.
- **Java baseline:** `mvn test -B` reproduced `Tests run: 1172, Failures: 1, Errors: 0, Skipped: 2`; the failure is `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`, matching the inherited baseline claim. Command exits non-zero because that inherited failure remains.
- **New S3 tests:** Surefire report confirms `Sprint52ProjectionSkillDrivenTest` ran `7 / 0 / 0 / 0`; `PhaseEvaluatorResolveSkillIntegrationTest` ran `14 / 0 / 0 / 0`.
- **Golden 4 -> 3 keys:** the golden now expects `Set.of("form_context", "customer_context", "listing_context")` at `PhaseEvaluatorResolveSkillIntegrationTest.java:149`, matching the YAML deletion of `moderation_context` from `resolve_faq_grounded_answer.yaml:20`.

## Optional Codex-Surfaced New Findings

- **P3 - Current-runtime doc drift after C2 #3.** `docs/current/runtime_contract.md:524` still says the FAQ RESOLVE `PhasePlan.requiredContextKeys` names `moderation_context`, while S3 removed that declaration. The same TODO already names option (b) drop the key (`docs/current/runtime_contract.md:535`), so this is a docs fold-back/update follow-up rather than a code blocker.

## Coherence Judgment

The S3 code delta itself is one coherent Skill-driven convergence: C1 identified `moderation_context` as declaration-only/no-consumer and `tool_schemas` as a consumed base path; C2 therefore strips only the stale declaration and replaces (does not delete) the base with the same Skill registry source already used by `PhasePlan.allowedTools()`. The #2 and #5 decisions were correctly STOP-surfaced because their matrix rows show potential removal of LLM-visible context or soft signals.

The `getVisibleToolsForUc` fallback appears genuinely defensive for unmapped tuples rather than a new semantic branch: it is reached only after `SkillRegistry.select(...)` misses and after `activeUseCase` is non-null. I would not call it a registry-coverage gap for today's six production Skills, but the silent fallback means future Skill coverage drift should be tested when new phases/UCs are added. S3 should not close from this checkout until the dev commit exists and the real-LLM rerun distribution is recorded and verified.
