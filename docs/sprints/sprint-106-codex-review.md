---
title: Sprint 106 Codex §4.1 anti-hardcode review — full transcript
doc_tier: sprint-archive
status: current
implementation_status: historical
source_of_truth: this file
last_reviewed: 2026-07-26
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Read-only `codex exec --sandbox read-only` with `model_reasoning_effort=high`
  over `82f8a137..HEAD` on branch sprint-106-casespec-promotion, using the
  nine-question kernel from docs/current/anti-hardcode-review-kernel.md plus six
  adversarial checks the sprint named (§5.4-in-reverse, the two deletions, the
  seven trigger rewrites, hardcode-by-data, the residual R1 count, the negative
  controls). Verdict `approve` / `pass` / blocking_count 0. Reviewer's final
  message reproduced verbatim; the three non-blocking findings were fixed in
  `ea282420` + `73cc0e83` and are tabulated in sprint-106-handoff.md §12.1.
---

# Sprint 106 — Codex §4.1 review, verbatim

1. **Q1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix for a semantic decision?**  
No. The branch changes only eval data and docs: 26 `outcome_class` flips, 7 `escalation_trigger` rewrites, 2 deletions, and 12 declarative `conditional_outcome_acceptance` blocks in CaseSpecs. It does **not** add executable branching in runtime, prompt, or judge config.

2. **Q2. If yes to (1), is the change justified as protecting a current Tier-0 invariant?**  
No trigger. There is no new hard branch to justify.

3. **Q3. Could the same outcome be achieved by projecting a soft signal to the LLM instead of a hard branch in Java or the prompt?**  
Not applicable to the shipped change. The sprint edits eval expectations only; it does not move behavior into Java or the prompt.

4. **Q4. Does the change encode visible-eval case text, trace-specific phrasing, or a CaseSpec id into runtime, prompt, or judge config?**  
No in executable surfaces. The notes quote transcript text as provenance, but the scored fields stay generic: standard enum values and the same `conditional_outcome_acceptance` schema. I found no `acceptable_outcomes: [resolve, escalate]` and no new permissive acceptance set at all.  
On your specific adversarial checks: the seven trigger rewrites are all canonical enum members, and none changed `outcome_class`:
`clarification_budget_exhausted -> payment_dispute_detected|out_of_scope|user_requested|appeal_requires_human|agent_unable_to_resolve` in [cs_interactive_007.yaml](/Users/caoruixin/projects/csagent-wt-106/eval_interactive/case_specs/promotion/cs_interactive_007.yaml:100), [cs_interactive_022.yaml](/Users/caoruixin/projects/csagent-wt-106/eval_interactive/case_specs/promotion/cs_interactive_022.yaml:97), [cs_interactive_077.yaml](/Users/caoruixin/projects/csagent-wt-106/eval_interactive/case_specs/promotion/cs_interactive_077.yaml:96), [cs_interactive_125.yaml](/Users/caoruixin/projects/csagent-wt-106/eval_interactive/case_specs/promotion/cs_interactive_125.yaml:100), [cs_interactive_131.yaml](/Users/caoruixin/projects/csagent-wt-106/eval_interactive/case_specs/promotion/cs_interactive_131.yaml:98), [cs_interactive_227.yaml](/Users/caoruixin/projects/csagent-wt-106/eval_interactive/case_specs/promotion/cs_interactive_227.yaml:101), [cs_interactive_229.yaml](/Users/caoruixin/projects/csagent-wt-106/eval_interactive/case_specs/promotion/cs_interactive_229.yaml:99).  
Transcript spot-checks support six of the seven cleanly. `227` is the weak one: the new trigger is better than the old budget artefact, but the transcript also supports the handoff’s own follow-up that this spec may actually want a future `resolve` flip [docs/sprints/sprint-106-handoff.md](/Users/caoruixin/projects/csagent-wt-106/docs/sprints/sprint-106-handoff.md:774).

5. **Q5. Does the change move semantic ownership from the LLM to Java?**  
No. The branch narrows eval expectations; it does not shift ownership into runtime code.

6. **Q6. Does the change add an if-else block to the prompt instead of principle-level or observable-state guidance?**  
No. No prompt files changed.

7. **Q7. Does the change preserve tool schema, capability / permission boundary, PII / safety floor, and grounding floor?**  
Yes. No runtime/tool/test/override paths changed, and I found no widening that newly accepts ungrounded bot behavior. The UC-A review-removal population was largely kept escalating, which is the right side of §5.4-in-reverse.

8. **Q8. Does the PR ship generalization eval coverage — target, neighbor, negative, and shadow cases — and not only the target case?**  
Mostly yes. The changed set includes promotion plus case-family neighbor/negative members, and the negative controls held: [anchor/cs_interactive_030.yaml](/Users/caoruixin/projects/csagent-wt-106/eval_interactive/case_specs/anchor/cs_interactive_030.yaml:1) and [anchor/cs_interactive_155.yaml](/Users/caoruixin/projects/csagent-wt-106/eval_interactive/case_specs/anchor/cs_interactive_155.yaml:1) are byte-unchanged, and `anchor/`, `smoke/`, `bad_cases/`, `exploration/` are untouched.  
Concern, non-blocking: 12 two-stage specs now live in programmatic buckets, so escalate-after-grounded-help is signal-only until adjudicated [docs/sprints/sprint-106-handoff.md](/Users/caoruixin/projects/csagent-wt-106/docs/sprints/sprint-106-handoff.md:466). That is a measurement caveat, not semantic hardcode.  
Accuracy notes: the handoff header is stale at [docs/sprints/sprint-106-handoff.md](/Users/caoruixin/projects/csagent-wt-106/docs/sprints/sprint-106-handoff.md:21): the current `82f8a137..HEAD` range is **12** commits, not 6, and the overall diff is not “all under `case_specs/`” because the handoff doc itself changed. Also, [docs/sprints/sprint-106-handoff.md](/Users/caoruixin/projects/csagent-wt-106/docs/sprints/sprint-106-handoff.md:676) cites `data/filtered/*`, but that path is absent in this worktree; the transcript evidence I verified came from `data/eval_datasets/*_turns.csv`.

9. **Q9. If the change is temporary, does it carry an explicit rollback or sunset plan?**  
Mostly yes, in the right sense for eval data. The branch treats the corpus edits as durable curation, and it records the needed follow-ups: R1 waiver support, anchor trigger cleanup, and a generator guard/home for the new “curated artefact” rule [docs/sprints/sprint-106-handoff.md](/Users/caoruixin/projects/csagent-wt-106/docs/sprints/sprint-106-handoff.md:741).  
On the two deletions: `070` is legitimate removal; its transcript never reaches the substance and the underlying non-taxpayer exception is a compliance-line question [badcase_turns.csv](/Users/caoruixin/projects/csagent-wt-106/data/eval_datasets/badcase_turns.csv:875), [docs/sprints/sprint-106-handoff.md](/Users/caoruixin/projects/csagent-wt-106/docs/sprints/sprint-106-handoff.md:696). `086` is borderline but still acceptable: the transcript supports “explain current trial + log feature request,” not a settled accessible-product route, so removing it is not obviously §5.4-by-deletion [handover_turns.csv](/Users/caoruixin/projects/csagent-wt-106/data/eval_datasets/handover_turns.csv:1532), [docs/sprints/sprint-106-handoff.md](/Users/caoruixin/projects/csagent-wt-106/docs/sprints/sprint-106-handoff.md:697).  
One doc mismatch remains: the handoff says the old `cs_interactive_185` “2 of 3 draws” argument was withdrawn [docs/sprints/sprint-106-handoff.md](/Users/caoruixin/projects/csagent-wt-106/docs/sprints/sprint-106-handoff.md:350), but the committed note in [cs_interactive_185.yaml](/Users/caoruixin/projects/csagent-wt-106/eval_interactive/case_specs/promotion/cs_interactive_185.yaml:50) still contains that withdrawn argument.

approve

## Sprint Review Decision
decision: pass
blocking_count: 0
summary: The branch does not introduce semantic hardcode: there is no runtime/prompt/judge branching, no permissive `[resolve, escalate]` widening, and the kept escalate cases mostly stay on the safe side of §5.4 by refusing to teach unsupported write actions or unguided review-removal behavior. The seven trigger rewrites are enum-valid and transcript-supported, the two deletions are defensible corpus-scope removals rather than evidence laundering, and the untouched-bucket negative controls held. Non-blocking issues remain: the handoff has stale range/count metadata, the `cs_interactive_185` file note still carries an argument the handoff says was withdrawn, and the two-stage specs in programmatic buckets are measurement-signal only until adjudication process catches up.
