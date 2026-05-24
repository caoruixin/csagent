# Instructions — Milestone framework adoption + Sprint 32 close + M1 setup

Session: deliver-agent cold-start through M1 + Sprint 33 prep.
Source jsonl: `~/.claude/projects/-Users-caoruixin-projects-csagent-latest/904d1e33-875c-4fa8-bbc7-0468c5ee1c56.jsonl`

## 1. Activate deliver-agent + bootstrap context

After `/clear`, pasted the deliver-agent role definition (责任 / 多 agent 协作模式 / 协作目标 / 关键文档与用途 / 已确认协作流程) plus:

- **Placeholder 1 (the proposed whole solution)**: research-agent's summary of the model-first runtime upgrade proposal — "Semantic Agent Core + Boundary Kernel" target architecture with shadow → low-risk live → skill/issue rollout strategy. Covers original 7-sprint roadmap (Sprint 17 Semantic Ownership & Shadow Baseline → Sprint 23 Narrow Factual / URL Policy + Projection Cleanup) plus the Triggered Single Handover Orchestrator.
- **Placeholder 2 (next deliver scope)**: investigate which of the original Sprint 17-23 + Triggered roadmap items are delivered vs not, then lock down the next sprint scope.
- **Context handoff reference**: `@compact/context-handoff-sprint31-fix2.md` — the in-flight Sprint 31 fix-iteration #2 state from the prior deliver-agent session.

## 2. Investigate the Alice bad case coverage

Provided a full bad case description for session `3772e56b-caa7-4e0a-84fc-75a26ffbe2b2`:

- Alice mock account, REMOVED listing AD-2001, asked "I can't see my advert anymore"
- Bot mis-classified UC-H (Ad Removal Appeal) with confidence 0.80 instead of UC-A (visibility / "why was my ad removed")
- 7-turn dead loop: bot kept asking `stated_reason_or_context` because IntakeFieldExtractor only implements UC-K (UC-H form-context prefill missing); user kept saying "I don't know, that's what I want you to tell me"
- Turn 4 `request_handover(intake_complete_for_uc_h)` correctly rejected by guard because fields_collected was empty
- All 7 LLM calls hit deepseek-v4-flash, no Kimi fallback; latency observable in `llm_call_log` table but UI doesn't show it
- Five distinct dimensions identified: D1 (DISCOVER mis-classification), D2 (IntakeFieldExtractor UC-K only), D3 (INTAKE_UCS hard lock-in at `PhaseEvaluator.java:535`), D4 (`stated_reason_or_context` circularity), D5 (Trace UI observability gap)

**Task:** dispatch an agent to investigate whether the sprints in the whole solution scope OR the backlog in `docs/action_bank.md` cover and solve these problems. **Don't code** — investigation only.

## 3. Status check on original Sprint 17-23 roadmap

> what's the progress of the initial whole solution scope " -Sprint 17 — Semantic Ownership & Shadow Baseline / -Sprint 18 — Semantic Planner Shadow Mode + Soft-signal Projection / -Sprint 19 — Low-risk Live Reroute Pilot / -Sprint 20 — Skill Plan-state: FAQ-resolve + Intake / -Sprint 21 — Escalation Reason + Human UX / -Sprint 22 — Issue Ledger MVP / -Sprint 23 — Narrow Factual / URL Policy + Projection Cleanup" and , are they still in the backlog?

## 4. Strategic planning question — how to adjust the iteration cadence

The most substantial input. Key concerns laid out:

- 一方面想完成原本的 Sprint 18-23 交付（model-first 演进方向）
- 另一方面在使用中又发现了新的问题（如 Alice bad case），希望也能解决
- 已确定的 Deliver / Dev / Review Agent 协作模式 + governance + constitution 需要保持稳定
- 希望进展能扎实往前走，不要在特别小的范围里不停优化
- Interactive evaluation 现在受制于本身的维度和打分方式问题，导致结果波动

**问题：** 既要加速升级节奏（这是 branch 可以快速调整），又不要受限于当前 eval 结果（eval 维度和打分本身可能就有问题）。**稳定的应是大目标方向 + 治理手段**；**可调的是 sprint 切分粒度（不要太小，希望有大块的 sprint，特别精小拖慢演进速度）**。

How should the plan be adjusted?

## 5. Approve milestone framework + acceptance gate downgrade + Codex frequency shift + bad-case suite

Responded to 4-question AskUserQuestion with:

- **Milestone framework**: adopt it. Open question on whether to record / update sprint governance file (since new sessions get context via key docs; /clear / compact / new session means context needs to be re-passed).
- **Acceptance gate**: formalize smoke composite_score demotion to observation + build curated bad-case suite as new primary gate.
- **Codex frequency**: milestone-shared review (sub-sprints share one Codex review at milestone close).
- **Roadmap → R-items**: unsure on the relationship between action_bank (R-items) / sprint_objective.md / new milestone_objective.md. Stated effect wanted: (a) ONE place to record desired improvements regardless of source; (b) for each sprint, ONE place explicitly naming what's being done for dev/review coordination. Sticking with existing files is OK if they meet this; if not, create new ones.

## 6. Approve Sprint 32 closure path + governance upgrade timing + bad-case suite location

Responded to second 4-question AskUserQuestion with:

- **Sprint 32 Finding 1** (shadow path mismatch): OOSR — deliver-agent fix `sprint_objective.md` path post-facto. Dev followed `_ACCESS_BOUNDARY.md:91` correctly.
- **Sprint 32 Finding 2** (Sprint 30 Option β prediction empirically falsified — target case alternates lack UC-C): in-flight downgrade Sprint 32 to investigation, new R-item, A-with-investigation-finding classification.
- **Governance upgrade timing**: do it now — parallel with Sprint 32 close.
- **Bad-case suite location**: new `eval_interactive/case_specs/bad_cases/` directory; Alice as the first entry.

## 7. Export this session's instructions to .chat folder

> export instructions to .chat folder, and give it a customised name

Filename chosen: `milestone-framework-adoption-and-m1-setup.md`.
