---
title: Customer Service Agent — Tool Spec v0.3 (current)
doc_tier: current-runtime
status: current
implementation_status: implemented
source_of_truth: server/src/main/resources/config/tool-policy.yaml + server/src/main/java/com/gumtree/csagent/service/tools/
last_reviewed: 2026-05-10
review_cadence: every_3_to_5_sprints
supersedes:
  - docs/customer_service_tool_spec_v0_2.md
  - docs/customer_service_tool_spec_v0_2.yaml
notes: >
  Code-grounded snapshot of the agent and runtime tool surface. Reflects
  what the running server actually exposes to the LLM and what the
  ToolDispatcher will execute today. Argument and return shapes are
  read from the Java tool implementations and the projected schema in
  ContextProjectionBuilder.
---

> This document is the current tool spec. Where it disagrees with
> `customer_service_tool_spec_v0_2.md` / `.yaml`, the configuration in
> `server/src/main/resources/config/tool-policy.yaml` and the Java
> implementations under `server/src/main/java/com/gumtree/csagent/service/tools/`
> are authoritative. For runtime semantics (phases, plan, allowed-tool
> enforcement) see `runtime_contract.md` in this directory.

# Customer Service Agent — Tool Spec v0.3

## How to read this spec

- **Visibility** is the `type` from `tool-policy.yaml`:
  `AGENT_VISIBLE` tools have a static schema in
  `ContextProjectionBuilder.toolSchemas` and may appear in the
  projected `tool_schemas` array (subject to the per-phase plan
  filter); `RUNTIME_ONLY` tools have no schema and the LLM cannot
  call them — if they execute at all in the live runtime they must
  be invoked by a deterministic Java caller. Whether such a caller
  currently exists varies per tool; see the per-tool notes under
  [Runtime-only tools](#runtime-only-tools) and the dormant
  runtime-only tool entry under [TODO_DECISION](#todo_decision).
- **Allowed UCs** is `tool-policy.yaml` `allowed-ucs`. `[ALL]` means
  the per-UC enforcer never blocks the tool; the per-phase plan
  whitelist may still gate exposure.
- **Schema** describes what the LLM sees in the projection
  (`ContextProjectionBuilder.build*ArgsSchema`); **arguments accepted**
  describes what the tool implementation actually reads in
  `Tool.execute(...)`. Where these differ, both are listed.
- This spec does not invent fields. Anything not present in code or
  config is omitted.

## Tool inventory

`tool-policy.yaml` currently registers 11 tools. The running server has
a corresponding `Tool` bean for each:

| Tool name | Visibility | Allowed UCs |
|---|---|---|
| `search_knowledge` | AGENT_VISIBLE | UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP |
| `resolve_article` | AGENT_VISIBLE | UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP |
| `get_customer_context` | AGENT_VISIBLE | UC-A, UC-C, UC-D, UC-F, UC-FP, UC-K |
| `request_handover` | AGENT_VISIBLE | ALL |
| `record_outcome` | AGENT_VISIBLE | ALL |
| `classify_use_case` | AGENT_VISIBLE | ALL (DISCOVER-only via plan) |
| `lookup_customer_account` | RUNTIME_ONLY | UC-C, UC-D, UC-FP, UC-K |
| `lookup_listing_or_ad` | RUNTIME_ONLY | UC-A, UC-C, UC-FP, UC-K |
| `get_moderation_review_context` | RUNTIME_ONLY | UC-A, UC-FP |
| `get_message_moderation_context` | RUNTIME_ONLY | UC-C |
| `create_case_controlled` | RUNTIME_ONLY | UC-H, UC-J, UC-K |

There is no `HUMAN_ONLY` type in current config; see
[Human-only tools](#human-only-tools) below.

## Agent-visible tools

### `search_knowledge`

- **Purpose**: search the knowledge base for FAQ articles relevant to
  the user's question. Returns a list of hits with `source_id`s.
- **Schema (projected)**: `query: string` (required),
  `uc_tags: string[]` (optional). Source:
  `ContextProjectionBuilder.buildSearchKnowledgeArgsSchema`.
- **Arguments accepted by tool**: `query` (required, non-blank).
  `uc_tags` from the projection is informational; the tool itself
  builds the tag list from session state — `activeUseCase` plus
  `candidateUseCases` (`SearchKnowledgeTool.execute`).
- **Returns**: `faq_miss`, `retrieval_miss`, `answer_miss`, and a
  `hits` array. Each hit carries `source_id`, `title`, `snippet`,
  `canonical_url`, `canonical_url_missing` (Sprint 14 §L0),
  and `score`.

### `resolve_article`

- **Purpose**: fetch the full content of a knowledge article by id,
  with a published-safety refusal for unpublished records.
- **Schema (projected)**: `source_id: string` (required). Source:
  `ContextProjectionBuilder.buildSingleStringFieldSchema("source_id", true)`.
- **Arguments accepted by tool**: `source_id` (canonical) **or**
  `article_id` (legacy alias retained for older callers and tests).
  Source: `ResolveArticleTool.execute` Sprint 8.2 §M0a comment.
- **Returns**: `source_id`, `article_id`, `title`, `summary`,
  `description`, `source_url`, `canonical_url`, `canonical_url_missing`,
  `url_category`, `uc_tags`, `is_published`, `safe_to_show`. Both
  `source_url` and `canonical_url` are emitted to keep older callers
  working while the agent reads the same field name as
  `search_knowledge.hits[*].canonical_url`.
- **Refusal**: `is_published == false` → error
  `article_unpublished_safe_refuse: article '<id>' is not published`.

### `get_customer_context`

- **Purpose**: composite lookup that fetches account, listing, and (when
  the listing status is `removed` / `moderated`) moderation review.
  Returns a sanitized summary with raw PII fields stripped.
- **Schema (projected)**: `email: string` (optional),
  `ad_id: string` (optional). Source:
  `ContextProjectionBuilder.buildGetCustomerContextArgsSchema`.
- **Arguments accepted by tool**: at least one of `email` or `ad_id`
  must be non-blank, otherwise the tool errors.
- **Returns**: `account` (only safe fields:
  `account_status`, `account_type`, `creation_date`, `active_ads_count`,
  `total_ads_count`, `has_verified_email`, `has_verified_phone`),
  `listing` (sanitized), and optionally `moderation_review` for
  removed / moderated listings. Raw email / phone / name / address
  are intentionally omitted (`GetCustomerContextTool.sanitizeAccount`).

### `request_handover`

- **Purpose**: escalate the session to a human agent. Builds the
  handover payload and calls `SalesforceService.requestHandover`.
- **Schema (projected)**: `escalation_reason: string` (required;
  enum below), `summary: string` (optional). Source:
  `ContextProjectionBuilder.buildRequestHandoverArgsSchema`.
- **Canonical `escalation_reason` enum (23 values)**: `user_requested`,
  `user_distress`, `faq_miss_threshold_exceeded`,
  `clarification_budget_exhausted`, `incomplete_intake`,
  `intake_complete_for_uc_g`, `intake_complete_for_uc_h`,
  `intake_complete_for_uc_i`, `intake_complete_for_uc_j`,
  `intake_complete_for_uc_k`, `payment_dispute_detected`,
  `appeal_requires_human`, `imminent_harm`,
  `incorrect_deletion_appeal`, `trust_safety_required`,
  `account_compliance`, `gdpr_intake`,
  `identity_verification_required`, `out_of_scope`,
  `service_degraded`, `turn_budget_exhausted`, `tool_scope_blocked`,
  `runtime_error_threshold`. The same set is mirrored in
  `PhaseEvaluator.CANONICAL_ESCALATION_REASONS` and
  `ToolDispatcher.CANONICAL_ESCALATION_REASONS`. A non-canonical
  value is logged WARN and coerced to `service_degraded` before the
  tool runs.
- **Arguments accepted by tool**: `escalation_reason` (required),
  `summary` (optional — derived by
  `RequestHandoverTool.deriveFallbackSummary` from session state +
  optional `current_user_message` when absent),
  `topic_uc_mismatch` (passed through to the payload),
  `intake_fields` (when present, merged into session state by
  `AgentRunLoopImpl` before the intake-complete guard runs).
  `current_user_message` is **tolerated** as an extra string
  argument by `deriveFallbackSummary` — when present it is mixed
  into the fallback summary alongside session topic/UC and the
  canonical reason — but it is not declared in the projected
  schema, so the LLM is not formally instructed to send it. The
  tool ignores any other unknown argument keys.
- **Returns / payload**: see `RequestHandoverTool.execute`. The
  outbound Salesforce payload includes `version` (`"1.0"`),
  `session_id`, `primary_use_case`, `candidate_use_cases`,
  `current_status`, `summary` (+ `summary_source: "fallback"` when
  derived), `intent_confidence`, `clarification_count`,
  `faq_miss_count`, `articles_shown`, `escalation_reason`,
  `case_id`, `identifiers_collected`, `intake_fields`,
  `total_bot_turns`, `form_topic_subject`, `topic_uc_mismatch`,
  `prompt_version`, `model_version`. The tool result returned to
  the LLM contains `transfer_result`, `session_id`,
  `escalation_reason`, `summary`, and `summary_source` (only when
  fallback was used).
  `primary_use_case` in the outbound payload is **not** an LLM
  argument and is not validated as required by the tool. It is
  derived directly from `session.getActiveUseCase()` (see
  `RequestHandoverTool.execute`) and may therefore be `null` on
  early or forced escalation paths where no UC has been committed
  (e.g. an INIT/DISCOVER forced handover before
  `classify_use_case` fires, a deterministic distress / explicit
  user-requested escalation that bypasses DISCOVER).
- **Known unspecced surface (no behavior change today)**: this
  tool is one of two independently-wired local handover writers; a
  future `HandoverOrchestrator` is intended to be the single
  side-effect owner, idempotent by `session_id`. Tracked by
  `Sprint16HandoverDualPathReproTest`. See `runtime_contract.md`
  → "What is NOT current runtime behavior".

### `record_outcome`

- **Purpose**: persist the final session outcome
  (`resolve | escalate | abandon`).
- **Schema (projected)**: `outcome_class: string` (required) with
  enum `resolve | escalate | abandon`. Source:
  `ContextProjectionBuilder.buildRecordOutcomeArgsSchema`.
- **Arguments accepted by tool**: `outcome_class` is the canonical
  arg name; `outcome` is accepted as a legacy alias. Inbound values
  are matched **case-insensitively**: the canonical lowercase forms
  (`resolve`, `escalate`, `abandon`) and the past-tense aliases
  (`resolved`, `escalated`, `abandoned`) are all accepted in any case
  (`RESOLVED`, `Resolved`, `resolved`, etc.) — see
  `RecordOutcomeTool.normalizeOutcomeClass`. `escalation_reason` is
  required when the normalized value is `escalate`. Source:
  `RecordOutcomeTool.execute` Sprint 9 §O0 comment.
- **Persistence**: the `session_outcomes` row uses the legacy
  uppercase form (`RESOLVED / ESCALATED / ABANDONED`) so downstream
  eval/L1 readers do not need to re-map.
- **Returns**: `session_id`, `outcome_class` (canonical lowercase),
  `outcome` (persisted uppercase), `use_case_id`.
- **In-loop guard**: during a RESOLVE / FAQ plan,
  `record_outcome(outcome_class=resolve)` is rejected if the
  deterministic terminal condition has not been met; reason
  `progressive_resolve_record_outcome_premature` (see
  `AgentRunLoopImpl.PROGRESSIVE_RESOLVE_GUARD_REJECT_REASON`).

### `classify_use_case`

- **Purpose**: lets the LLM commit a use-case classification during
  DISCOVER. Sets `session.activeUseCase` and
  `session.intentConfidence`; emits a `CLASSIFICATION_COMMITTED`
  `BotEvent`. Without this tool, soft-OOS DISCOVER would only emit
  clarifying questions until the budget exhausted.
- **Schema (projected)**: `use_case_id: string` (required, enum
  below), `confidence: number ∈ [0, 1]` (required),
  `reasoning: string` (optional). Source:
  `ContextProjectionBuilder.buildClassifyUseCaseArgsSchema`.
- **`use_case_id` enum**: `UC-A`, `UC-B`, `UC-C`, `UC-D`, `UC-E`,
  `UC-F`, `UC-FP`, `UC-G`, `UC-H`, `UC-I`, `UC-J`, `UC-K`. Mirrors
  `use-case-registry.yaml`.
- **Carry-forward policy**: when the session already has an
  active UC matching the deterministic strong-prior /
  phrase-bias derivation, a different UC is refused with
  `reason: strong_prior_carry_forward` (idempotent
  re-classification with the same UC is allowed and still emits
  the event). Hard-shift drift (UC-G/I/J/H) is pre-empted by
  `DriftDetector` before the loop runs, so the guard releases on
  legitimate hard shifts. Source: `ClassifyUseCaseTool.shouldPreserveStrongPrior`.
- **Allowed only in DISCOVER**: although `tool-policy.yaml` lists
  `allowed-ucs: [ALL]`, exposure is restricted to the DISCOVER plan
  via `PhaseEvaluator.plan(...)` — only the DISCOVER plan includes
  this tool in `allowedTools`, and the projection's `tool_schemas`
  array is filtered to plan-allowed tools.
- **Returns**: `committed: true` plus `use_case_id` and
  `confidence` (clamped to two decimals to fit the
  `intent_confidence` column). On a refused carry-forward:
  `committed: false`, `preserved_use_case_id`,
  `rejected_use_case_id`, `reason`.
- **Loop terminal**: a successful classification inside a DISCOVER
  plan short-circuits the run loop with
  `TerminalOutcome.USE_CASE_IDENTIFIED` so DISCOVER → RESOLVE can
  fire on the next interpret pass.

## Runtime-only tools

These tools are registered as `RUNTIME_ONLY` in `tool-policy.yaml` and
have no schema in `ContextProjectionBuilder`, so the LLM cannot
dispatch them. Whether each one has a live deterministic caller in
the current runtime varies — see notes per tool.

- **`create_case_controlled`** (`CreateCaseControlledTool`) — creates
  a Salesforce case for UC-H, UC-J, UC-K with required fields per
  UC (UC-H: `description`; UC-J: `subject`, `description`; UC-K:
  `email`, `description`) and a fixed queue mapping
  (`Ad_Support_Queue`, `Safety_Queue`, `Account_Support_Queue`).
  **Live deterministic callers**: `ControlKernel.createCaseIfNeeded`
  (escalation-time case creation) and `PhaseEvaluator.createCaseIfAllowed`
  (legacy intake-complete path). Both invoke the bean directly,
  bypassing `ToolDispatcher`; the LLM does not.
- **`lookup_customer_account`** (`LookupCustomerAccountTool`) — looks
  up an account by `email` via `GumtreeApiService`. Returns
  `{found, account?}`. Allowed for UC-C, UC-D, UC-FP, UC-K.
  **No live deterministic caller currently observed** in the
  runtime; registered as a tool bean with no active dispatch path.
  See TODO_DECISION below.
- **`lookup_listing_or_ad`** (`LookupListingTool`) — looks up a
  listing by `ad_id`. Returns `{found, listing?}`. Allowed for
  UC-A, UC-C, UC-FP, UC-K. **No live deterministic caller
  currently observed.** See TODO_DECISION below.
- **`get_moderation_review_context`** (`GetModerationReviewContextTool`)
  — fetches moderation review for an `ad_id` and maps the internal
  reason code to a customer-facing explanation
  (`REASON_CODE_EXPLANATIONS`). Allowed for UC-A, UC-FP. **No live
  deterministic caller currently observed**; on the form-ingestion
  path the equivalent moderation review lookup happens inside
  `GetCustomerContextTool` (composite tool), not via this bean.
  See TODO_DECISION below.
- **`get_message_moderation_context`** (`GetMessageModerationContextTool`)
  — fetches message moderation history for a `conversation_id`.
  Allowed for UC-C. **No live deterministic caller currently
  observed.** See TODO_DECISION below.

## Human-only tools

The current `tool-policy.yaml` defines two `type` values only:
`AGENT_VISIBLE` and `RUNTIME_ONLY`. There is no `HUMAN_ONLY` /
`human_only` type and no tool is currently flagged as such in
config. Sensitive write actions described as `human_only` in
`customer_service_tool_spec_v0_2.*` are simply not registered as
`Tool` beans in this codebase. If/when human-only surfaces are
re-introduced, this section should be revisited.

## `classify_use_case` agent visibility — yes, conditionally

`classify_use_case` is agent-visible (the LLM sees it and can call
it) but only in the DISCOVER phase, because that is the only plan
whose `allowedTools` lists it (see `PhaseEvaluator.plan(...)`
DISCOVER branch and the Sprint 8.1 §M3 schema-filter behavior in
`ContextProjectionBuilder.build`).

## Legacy aliases supported by code

- `resolve_article.article_id` → `source_id` (`ResolveArticleTool`).
- `record_outcome.outcome` → `outcome_class`
  (`RecordOutcomeTool.execute` first attempts `outcome_class`, then
  falls back to `outcome`).
- `record_outcome` uppercase enum (`RESOLVED / ESCALATED /
  ABANDONED`) → canonical lowercase (`resolve / escalate /
  abandon`) (`RecordOutcomeTool.normalizeOutcomeClass`). Persisted
  form remains uppercase.
- `request_handover` non-canonical `escalation_reason` → coerced to
  `service_degraded` (`ToolDispatcher.dispatch`).

## Differences vs v0.2

(High-level deltas; the v0.2 doc is preserved historically and
should not be edited.)

- **`classify_use_case` is new in v0.3**, agent-visible during
  DISCOVER. Not present in v0.2.
- **`record_outcome` canonical arg renamed** to `outcome_class` with
  lowercase enum. v0.2 used `outcome` with uppercase enum. Both are
  still accepted in v0.3 as aliases.
- **`resolve_article` canonical arg renamed** to `source_id`.
  `article_id` is accepted as a legacy alias.
- **`request_handover.summary` is now optional**, with deterministic
  fallback derivation. The 23-value canonical `escalation_reason`
  enum is enforced in code (and coerced on non-canonical input).
- **`create_case_controlled` is `RUNTIME_ONLY`** in v0.3. v0.2 listed
  it as agent-visible / human-only depending on phase; today the
  LLM has no schema for it and the deterministic intake-complete
  hooks own the side effect.
- **`search_knowledge` and `resolve_article` carry `canonical_url` /
  `canonical_url_missing`** so the agent reads the same field name
  on hits and on resolved articles. Unpublished articles are
  refused with the deterministic reason
  `article_unpublished_safe_refuse`.
- **No `HUMAN_ONLY` type** in `tool-policy.yaml`. v0.2's
  `human_only` surfaces are not exposed as tools.
- **Per-phase tool-schema filter** — the projection's
  `tool_schemas` is exactly `PhasePlan.allowedTools`. v0.2 implied
  schema visibility followed the per-UC matrix only.

## TODO_DECISION

- **`fixed_script_library` as a runtime capability vs. tool** —
  v0.2 lists `fixed_script_library` as a runtime capability (not a
  Tool bean). The current code references a
  `ScriptLibraryService` from `PhaseEvaluator`; verify whether
  v0.3 should formally retain `fixed_script_library` as a runtime
  capability section, and what the source of truth for its
  templates is today
  (`docs/fixed_script_library_v1.md` vs. server resources).
- **HandoverOrchestrator cutover** — `request_handover` and
  `SessionManager.recordHandover` are independently-wired
  handover writers today. Once the future
  `HandoverOrchestrator` (release_gate §1.1) lands, this spec
  should drop the dual-path reference and document the
  single-owner contract instead.
- **Dormant runtime-only tools** — `lookup_customer_account`,
  `lookup_listing_or_ad`, `get_moderation_review_context`, and
  `get_message_moderation_context` are registered as `RUNTIME_ONLY`
  tool beans and appear as labels in handover status checks, but
  no live deterministic caller has been identified for any of
  them. Decide whether to (a) wire deterministic callers (e.g.
  surface `get_moderation_review_context` directly on UC-A / UC-FP
  rather than relying on the composite `get_customer_context`),
  (b) retire the beans + policy entries, or (c) document a
  specific owner / planned caller per tool. Until decided, treat
  these as registered tool beans without a current dispatch path.
