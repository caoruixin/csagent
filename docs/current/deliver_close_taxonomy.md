---
doc_tier: current-runtime
status: active
owner: deliver-agent + human
review_cadence: every milestone close
description: Sprint / milestone close decision classifications for deliver-agent.
---

# Deliver Close Taxonomy

Codex review 返回后，deliver agent + human 对每个 blocker **逐条分类**，然后给出 NET recommendation。
不要对整个 sprint 强制选单一分类（参见"混合分类"）。

## Primary Classifications (A / B / C / D)

### A — Clean close

Codex `decision: pass, blocking_count: 0`。归档 codex-findings → `docs/sprints/sprint-NNN-codex-review.md`。

### A-with-packaging-note

Codex `decision: out_of_scope_review`，blocker 是 **path-based**（deliver-agent 文件被 bundled 进 dev commit），substantive findings 已 close。
- **Resolution**: Option X — roll forward with packaging note in close handoff。不需要 re-review。
- **Close handoff 必须包含**: substantive verdict + packaging artefact 说明 + roll forward statement。
- **Origin**: Sprint 20 fix re-review (2026-05-13)。

### A-with-Codex-skipped

Sprint scoped 为 potentially-action，结果 dev outcome 是 **docs-only**（zero semantic-touching edits）。Human 引用 §4.1 exemption 跳过 Codex。
- **Resolution**: 不生成 `sprint-NNN-codex-review.md`。Close handoff §12 必须记录 skip 决定 + §4.1 exemption citation + date。
- **区别于 Sprint 18 G1**: G1 是 open-time planned-skip；本分类是 close-time human-discretion。
- **Origin**: Sprint 26 close (2026-05-15)。

### A-with-evidence-gap-acknowledgment

Codex `decision: fix_required`，blocker 是 **typographical-only**（markdown/punctuation delta），Codex 自己的 non-blocking checks 确认 substance sound。
- **Resolution**: human 接受 typographical gap，close over fix_required。Close handoff 引用 Codex non-blocking-check evidence。
- **NOT B**: B 是 substantive 内容错误，需要 fix iteration。本分类的 blocker 不改变 evidence package 的 prove 内容。
- **Origin**: Sprint 21 fix re-review (2026-05-14)。

### B — Substantive blocker, targeted fix iteration

Codex blocker 是 **content-substantive**（wrong claim, missing evidence, behaviour drift）。需要 dev fix iteration。

### B-resolved-without-re-review

B 的 fix scope 是 **mechanical + verifiable**（如"添加 jq 命令"），且 Codex substantive verdict 已是 approve。
- **条件**: (1) kernel + verification-notes 均 approve；(2) fix verifiable by command；(3) fix 只碰 Codex flagged 的 artefact；(4) 不产生新 evidence。
- **Resolution**: human self-verify，不需要 Codex re-review。Close handoff §12 记录 self-verification。
- **Origin**: Sprint 29 close (2026-05-16)。

### C — Codex broadens scope

Codex 的 finding 超出当前 sprint/milestone scope。
- **Resolution**: 不让 dev fix；要求 Codex rewrite targeted review 或将 items 移入 `action_bank.md` deferred。

### D — Non-convergence

Multiple fix rounds 未 converge。停止自动化，human review required。

## Special Patterns

### Mixed per-blocker classification

Multi-blocker review 中，每个 blocker 独立分类。NET recommendation = union。
- A+B mix → B fix runs; A items 记 packaging note。
- A+C mix → C rebuttal; A items 记 packaging note。
- B+C mix → B fix first; fix 后判断 C 是否仍 apply。

### Conditional finding self-resolving

Codex blocker 包含 "if X then exclude" 条件，handoff §9.3 确认条件成立。
- **Resolution**: A-with-packaging-note。human 在 `git add` 时不 stage 该文件即可。不需要 dev fix 或 re-review。
- **Origin**: Sprint 29 close (2026-05-16)。

## Quick Decision Flowchart

```
Codex returns → for each blocker:
  ├─ path-based (extra files in commit)?
  │   └─ substantive findings closed? → A-with-packaging-note
  ├─ conditional ("if X, exclude")?
  │   └─ handoff confirms condition? → A-with-packaging-note
  ├─ typographical-only?
  │   └─ non-blocking checks confirm substance? → A-with-evidence-gap-acknowledgment
  ├─ mechanical fix + kernel approved?
  │   └─ B-resolved-without-re-review
  ├─ content-substantive, in-scope? → B
  ├─ out-of-scope? → C
  └─ NET: union of per-blocker → dominant action
```
