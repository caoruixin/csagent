# instructions 
## q1
@docs/customer_service_agent_tech_spec.md @docs/customer_service_agent_eval_spec.md  @docs/BRD.md @docs/PRD_biz_part.md
@docs/case-data-stat.md @docs/case-data-stat.md @docs/case-samples.md  @docs/07-engineering-constraints.md 这几份文档,包括Business Requirement Document,去除了技术规范、技术方案相关内容之后的Product Requirement Document,还有一份是我们对Case做的统计分析 etc.
请基于这些文档，推导出或更新（对于已经输出的部分，要更新迭代 ） @docs/customer_service_agent_delivery_workbook.md 中提到的几个部分
"Phase 0 — Freeze Normative  Layer","Phase 1 — Build Solution Input Pack" ,"Phase 2 — Produce Domain Realization Spec",并output到对应的markdown file ，先不要创建"phase 3 Detailed Technical Design" 以及之后的内容，直到我补充完必要的信息和数据 

### a
⏺ All four updates done. Here's the summary of v2 iteration:

  Files updated

  File: phase0_normative_freeze.md
  Key changes: Replaced single-spec reference with split tech_spec + eval_spec. Added §0.1 (purpose), §0.4 (领域化范围), §0.5
    (drift防护)。 0.3 inheritance table expanded from 8 rows to 24 rows with explicit spec section pointers (§5, §8, §10.5, §11, §13,
    §15, §16, §17, §18, §19, §20 from tech_spec; §4, §6, §11, §15 from eval_spec).
  ────────────────────────────────────────
  File: phase1_solution_input_pack.md
  Key changes: Restructured into 5 sub-sections per 1.1 (objectives / scope / use cases / metrics / brand voice). Added: BRD
    tone-of-voice (§1.1.5 — "Gumtree Support Assistant" naming, opening / escalation / out-of-hours phrases, forbidden phrases, WCAG);
    PRD §2 system roles + §3.2 Handling state machine + §9 data model skeleton (§1.3.1–1.3.3). Added Salesforce integration table
    (§1.4.12).
  ────────────────────────────────────────
  File: phase2_domain_realization_spec.md
  Key changes: Added case_volume to each use case (5,506 / 3,160 / 2,627 / 4,906 / 3,811 / 488 / 20,875). Added §2.4 escalation message
    templates from BRD. Added §2.7 Handover Payload JSON schema + Handling State coupling. Added §2.9 Guardrails (10 rows) mapping
    tech_spec §16 + BRD §6.3 brand voice. Added §2.10 Phase 2 → Phase 3 移交清单 (frozen contracts that Phase 3 must not modify).
  Updated
     forbidden automation table with source citations.
  ────────────────────────────────────────
  File: customer_service_agent_delivery_workbook.md
  Key changes: Marked Phase 0/1/2 as v2; Phase 3/4/5 changed from "待启动" to "暂缓". Added 规范层文件 (外部依赖) table and 业务输入文件

    table. Phase 6.4 expanded from 9 bullets to 12-row table with affected Phase 3 sections + suggested owners. Phase 6.5 explicitly
    states "暂不启动 Phase 3 / 4 / 5".

  What's frozen, what's open

  Frozen contracts (Phase 3 must respect): use case schemas, allowed actions, control budgets, drift semantics, escalation triggers +
  reason codes, handover payload JSON, outcome → ContainmentOutcome__c mapping, guardrail rules, brand voice templates.

  Open / blocked on user input (12 items in §6.4 of workbook): Help Centre KB IDs, UC-FP-01 final wording (legal sign-off), Salesforce
  org config, vector DB choice, embedding model, golden dataset, traffic % + CSAT thresholds, off-hours policy, Salesforce custom object
   naming, CSAT mechanism, PII redaction rules, repo creation.

  Phase 3 Detailed Technical Design is paused as requested — will start once you've supplied those.

✻ Churned for 10m 10s


## q2
how do you think about the role of docs @docs/customer_service_agent_tech_spec.md @docs/customer_service_agent_eval_spec.md  for
  this customer service agent E2E delivery? what's the relationship between them and @docs/customer_service_agent_delivery_workbook.md
  . How to use them moving forward?

### a
⏺ Good question — and worth answering carefully because the three docs play very different roles, and confusing them is a common source
  of churn.

  The mental model: three layers

  ┌─────────────────────────────────────────────────────────────┐
  │ Layer 1 — Normative (universal, slow-changing)              │
  │   tech_spec.md  +  eval_spec.md                             │
  │   "The rules of the game for ANY CS agent built this way"   │
  └─────────────────────────────────────────────────────────────┘
                            ↑ inherited from
  ┌─────────────────────────────────────────────────────────────┐
  │ Layer 2 — Methodology (process, slow-changing)              │
  │   delivery_workbook.md                                      │
  │   "How to take Layer 1 and deliver a specific project"      │
  └─────────────────────────────────────────────────────────────┘
                            ↑ produces
  ┌─────────────────────────────────────────────────────────────┐
  │ Layer 3 — Project (specific, fast-changing)                 │
  │   phase0..phase5 outputs + BRD + PRD + data + eng constraints│
  │   "This Gumtree CS Bot's actual decisions and design"       │
  └─────────────────────────────────────────────────────────────┘

  The three docs you asked about live in Layer 1 and Layer 2. The phase outputs we've been producing live in Layer 3.

  ---
  What each doc is FOR

  customer_service_agent_tech_spec.md — the technical constitution

  - Subject: Any CS agent built on this methodology (not Gumtree-specific).
  - Scope: Architecture, control kernel, state model, allowed actions, tools, handover, guardrails, observability, NFRs, release
  criteria.
  - Authority: Defines invariants. "single agent + bounded loop", "INIT→DISCOVER→RESOLVE→CONFIRM→CLOSE/ESCALATE", "5 allowed actions",
  "context projected not concatenated", etc.
  - Evolution: Slow. V1 → V1.1 → V2 versioning is intentional — most decisions stay locked for long stretches.
  - Anti-pattern: Treating it as a Gumtree project doc. It isn't. It's the spec another team could use to build a totally different CS
  agent and still produce a recognizably-similar system.

  customer_service_agent_eval_spec.md — the quality constitution

  - Subject: Same as tech_spec (universal).
  - Scope: What "good" means — eval scope, dataset structure, grader design, 8 metric categories, launch gates, CI/CD integration,
  failure taxonomy, ownership model.
  - Authority: Defines what must be measured before release and how regressions are caught.
  - Why separate from tech_spec: Quality concerns evolve on a different cadence than runtime concerns. New eval suites can be added
  without touching the runtime spec, and vice versa. Separating them lets each be maintained by the right owners (eng vs. QA/product).
  - Critical pairing: Tech spec says what the system does; eval spec says how we know it does it correctly. Neither is sufficient alone.

  customer_service_agent_delivery_workbook.md — the methodology / scaffold

  - Subject: The process of turning Layer 1 into a Layer 3 delivery.
  - Scope: A 6-phase template (Freeze Normative → Build Inputs → Domain Realization → Tech Design → Implementation Packet →
  Eval/Release).
  - Authority: Defines what gets produced when and in what structure — not the content of the production.
  - Key property: Output-shaped. Each phase has a fixed set of slots that must be filled. This makes projects auditable ("show me your
  Phase 2 use case registry") and comparable across projects.
  - What it is NOT: Not a project tracker, not a status doc, not a knowledge base. It's a template + an index.

  ---
  The relationships in one sentence each

  ┌─────────────────────┬───────────────────────────────────────────────────────────────────────────────────────────────────────────┐
  │            Relationship            │                                         One-liner                                          │
  ├────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────┤
  │ tech_spec ↔ eval_spec              │ Peers. One says what; the other says how-we-know. Both Layer 1.                            │
  ├────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────┤
  │ tech_spec → workbook               │ The workbook's Phase 0 is literally "freeze which parts of tech_spec this project          │
  │                                    │ inherits." Phases 3, 4 reference tech_spec sections.                                       │
  ├────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────┤
  │ eval_spec → workbook               │ The workbook's Phase 5 (Eval/Release/Feedback) is the application of eval_spec to this     │
  │                                    │ project. Release gates trace back to eval_spec §11.                                        │
  ├────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────┤
  │                                    │ The workbook is a template; the phase outputs are filled instances. We split them into     │
  │ workbook → phase outputs           │ separate files for review-ability — but the workbook still holds the index and Phase 6     │
  │                                    │ status.                                                                                    │
  ├────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────┤
  │ Phase 0 → all later phases         │ Phase 0 is the drift gate. It locks which Layer 1 rules apply to this project. Later       │
  │                                    │ phases that violate Phase 0 must explicitly document the drift.                            │
  ├────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────┤
  │ BRD/PRD/data/eng-constraints →     │ These are business inputs (also Layer 3, but unstructured-ish). Phase 1 absorbs them into  │
  │ phase outputs                      │ the workbook's structure.                                                                  │
  └────────────────────────────────────┴────────────────────────────────────────────────────────────────────────────────────────────┘

  ---
  How to use them moving forward (operational)

  Day-to-day: which doc do I open?

  ┌───────────────────────────────────┬─────────────────────────────────────────────────────────────────────────────────────────────┐
  │             Question              │                                          Open this                                          │
  ├───────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────┤
  │ "Is this design choice allowed?"  │ tech_spec or eval_spec — find the rule, then check phase0 to confirm it's inherited.        │
  ├───────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────┤
  │ "What does Gumtree-specifically   │ The relevant phase output (phase1/2/3).                                                     │
  │ do here?"                         │                                                                                             │
  ├───────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────┤
  │ "Where does this project stand?"  │ workbook → Phase 6 (status, missing inputs, next actions).                                  │
  ├───────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────┤
  │ "What's the schema for X?"        │ tech_spec for invariant schemas (e.g., handover payload contract); phase output for         │
  │                                   │ project-specific (e.g., Bot_Session__c fields).                                             │
  ├───────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────┤
  │ "Why was this decision made?"     │ Phase output (project-specific decisions) or git history of tech_spec (invariant            │
  │                                   │ evolution).                                                                                 │
  ├───────────────────────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────┤
  │ "What's our launch gate?"         │ eval_spec §11, mirrored into phase0 §0.3, applied in phase5.                                │
  └───────────────────────────────────┴─────────────────────────────────────────────────────────────────────────────────────────────┘

  When making any new design decision

  Apply this 3-step trace:

  1. Does this conflict with anything in tech_spec or eval_spec?
    - If yes → either change the design, or explicitly document drift in phase0.5 and get spec-owner sign-off.
    - If no → proceed.
  2. Does this fit one of the workbook phase slots?
    - If yes → put it in the right phase output file.
    - If no → it might be premature, or the workbook needs an extension.
  3. Is the decision project-specific or universal?
    - Project-specific → phase output.
    - Universal → propose an update to tech_spec/eval_spec (new revision), don't bury it in a phase output.

  When the specs themselves change (V1.1, V2)

  This will happen. When it does:

  - Re-run Phase 0 to re-freeze inheritance against the new version.
  - Diff against the previous phase0 to see what's now newly inherited / newly possible / newly forbidden.
  - Cascade impact through phase 1 → 5 only where the diff actually matters. Don't blindly rewrite everything.

  Anti-patterns to avoid

  ┌─────────────────────────────────────────────┬──────────────────────────────────────────┬────────────────────────────────────────┐
  │                   Mistake                   │               Why it's bad               │                  Fix                   │
  ├─────────────────────────────────────────────┼──────────────────────────────────────────┼────────────────────────────────────────┤
  │ Putting Gumtree-specific content into       │ Pollutes the universal layer; next       │ Keep it in phase outputs.              │
  │ tech_spec/eval_spec                         │ project inherits noise.                  │                                        │
  ├─────────────────────────────────────────────┼──────────────────────────────────────────┼────────────────────────────────────────┤
  │ Treating phase0 as a one-time setup         │ It's the drift gate. If specs evolve,    │ Treat phase0 as living but             │
  │                                             │ phase0 must be refreshed.                │ slow-changing.                         │
  ├─────────────────────────────────────────────┼──────────────────────────────────────────┼────────────────────────────────────────┤
  │ Letting phase outputs contradict tech_spec  │ Silent drift. Eventually breaks at       │ Explicit drift section + owner         │
  │ without acknowledgment                      │ release-gate or in production.           │ approval.                              │
  ├─────────────────────────────────────────────┼──────────────────────────────────────────┼────────────────────────────────────────┤
  │ Treating the workbook as the doc people     │ It's a template + index. The actual      │ Read phase outputs; use workbook for   │
  │ read                                        │ content lives in phase outputs.          │ navigation.                            │
  ├─────────────────────────────────────────────┼──────────────────────────────────────────┼────────────────────────────────────────┤
  │ Skipping eval_spec because "it's just       │ Then you have no defensible launch       │ Eval spec drives the gates that decide │
  │ metrics"                                    │ criteria.                                │  whether you can ship.                 │
  ├─────────────────────────────────────────────┼──────────────────────────────────────────┼────────────────────────────────────────┤
  │ Adding new use cases without going back     │ Phase 2 is the contract that phases 3-5  │ Update phase 2 first, then propagate.  │
  │ through phase 2                             │ depend on.                               │                                        │
  └─────────────────────────────────────────────┴──────────────────────────────────────────┴────────────────────────────────────────┘

  The role moving forward, summarized

  - tech_spec + eval_spec = your source of truth for what doesn't change. Reference frequently, modify rarely, version explicitly.
  - delivery_workbook = your checklist + index. Use it to navigate, to audit completeness, and to onboard. Don't store knowledge in it.
  - phase outputs = your living project artifacts. This is where the real work accumulates. The split into separate files (which we just
   did) is what makes them reviewable and maintainable.

  The healthy steady state: 95% of your reading and editing happens in the phase outputs; you touch tech_spec/eval_spec only when (a)
  onboarding, (b) deciding a contested design point, or (c) genuinely evolving the universal spec. The workbook is the scaffold that
  keeps phase outputs comparable across projects and across iterations.

✻ Sautéed for 1m 20s

## q3
@docs/customer_service_conversation_samples_organized.xlsx  @docs/inferred_tool_candidates_from_human_conversations.xlsx  @docs/customer_service_tool_spec_v0_1.yaml 这几份新增的文档,包括真实case对应的conversation samples、推导出的tools、以及tool spec.
请基于这些文档，推导或更新（对于已经输出的部分，进行更新 ） @docs/customer_service_agent_delivery_workbook.md 中提到的几个部分
"Phase 0 — Freeze Normative  Layer","Phase 1 — Build Solution Input Pack" ,"Phase 2 — Produce Domain Realization Spec",并output到对应的markdown file ，先不要创建"phase 3 Detailed Technical Design" 以及之后的内容，直到我补充完必要的信息和数据 