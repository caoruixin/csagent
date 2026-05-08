# Sprint Objective

Date: 2026-05-09

## Sprint name

Script / Policy Config Governance Sprint 15

## Goal

Reduce hardcoded policy / script / threshold drift by moving selected governance data into explicit config surfaces and adding consistency checks, while preserving current runtime semantics.

Sprint 15 must be behaviour-preserving. It should not introduce new routing semantics, new escalation semantics, new risk levels, new prompt policy, FAQ corpus changes, judge calibration, CaseSpec churn, or broad runtime rewrites.

Sprint 14 closed with:
- KB published-safety and canonical URL observability improved
- retrieved / resolved / cited evidence separated
- FAQ grounding diagnostics durably persisted under `bot_turns.projected_context.faq_grounding`
- no hard citation gate
- no DB migration
- Codex decision: pass
- blocking_count: 0

## Implement exactly these 3 actions

### M0. Externalize DriftDetector / risk keywords to YAML, behaviour-preserving

Move the existing DriftDetector risk keyword list from hardcoded Java into a runtime-loaded YAML config.

Required behaviour:

- Create a config file, for example:
  `server/src/main/resources/config/risk-keywords.yaml`
- Preserve current behaviour exactly:
  - same keyword groups
  - same matching semantics
  - same risk labels / drift outputs
  - same precedence
- Keep deterministic guard boundaries intact.
- Do not add new risk keywords unless required to preserve an already-hardcoded Java keyword.
- Do not change escalation behaviour.
- Do not turn Level 1 / Level 2 risk signals into automatic handover.
- Add a focused parity test proving YAML-loaded config produces the same results as the previous hardcoded list on representative examples.
- Add a config validation test:
  - file exists
  - no duplicate keys
  - required fields present
  - empty keyword groups fail fast

Acceptance condition:

- DriftDetector behaviour remains unchanged.
- Policy changes can later be reviewed through config diff instead of Java source changes.

### M1. Script library version pin and docs ↔ YAML consistency check

Add a lightweight consistency check between approved script docs and runtime templates.

Required behaviour:

- Add explicit script library version metadata to `server/src/main/resources/scripts/templates.yaml` if absent.
- Align it with `docs/fixed_script_library_v1.md` version metadata.
- Add a test or build-time check that fails if:
  - docs version and YAML version diverge
  - required template IDs are missing
  - required template variables are missing or renamed without docs update
- Do not rewrite script copy.
- Do not alter tone or escalation wording.
- Do not change forbidden phrase rules.
- Do not introduce a new template engine.

Acceptance condition:

- Runtime templates and approved docs cannot silently drift.
- Existing ScriptLibraryService behaviour remains unchanged except for metadata validation.

### M2. Retrieval / answer gate / rerank fallback thresholds config exposure + diagnostics

Expose existing knowledge retrieval thresholds as config with defaults unchanged.

Required behaviour:

- Externalize current constants such as:
  - ANN limit
  - retrieval threshold
  - answer gate
  - max returned results
  - rerank fallback score
- Defaults must remain exactly equivalent to current Java constants.
- Add startup/config validation:
  - thresholds are numeric
  - thresholds are in sane ranges
  - answer gate remains >= retrieval gate where relevant
- Add diagnostics / logging for rerank fallback usage:
  - when rerank parsing fails
  - when rerank service returns fallback score
  - when answer_miss / retrieval_miss is caused by threshold gate
- Do not tune thresholds in this sprint.
- Do not change FAQ answerability semantics.
- Do not change corpus content.
- Do not change eval expected outcomes.

Acceptance condition:

- Current retrieval behaviour is preserved under default config.
- Future threshold changes become auditable config changes rather than hidden Java edits.

## Regression guards

- `L1:escalation_reason_consistency` remains 0.
- `CONTRACT_VIOLATION:active_use_case` remains 0.
- Sprint 14 FAQ grounding observability remains intact:
  - `bot_turns.projected_context.faq_grounding` persists retrieved / resolved / cited / diagnostic fields.
- Sprint 14 KB published-safety remains intact.
- Existing FAQ S1 guard remains green.
- Sprint 6 ReadTimeout no-retry closure remains intact.
- cs014 remains UC-C.
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 distress reconciliation remains green.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested` focused regression remains green.

## Do not implement

- New risk semantics
- New escalation reasons
- New hard Java guard
- Broad routing rewrite
- Prompt rewrite
- Hard citation gate
- FAQ corpus rewrite
- Judge calibration
- CaseSpec changes
- Anchor / exploration / promotion hard-gate expansion
- Hot reload / ops-owned runtime config
- Dashboard work
- Full TraceViewer redesign
- New skill runtime framework
- Broad S1 rewrite

## Success metrics

Primary:

- Risk keyword config is externalized with behaviour parity.
- Script template version pin prevents docs/runtime drift.
- Retrieval / rerank thresholds are config-visible with defaults unchanged.
- No runtime semantics change is introduced.
- Regression guards remain green.

Secondary:

- Smoke pass rate is not a primary metric for this sprint.
- Focused config parity tests matter more than pass-rate movement.

## Review rule

Codex must review only whether M0 / M1 / M2 were implemented and whether Sprint 15 stayed behaviour-preserving.

Codex should not request semantic risk policy changes, prompt rewrites, routing changes, judge calibration, CaseSpec churn, corpus updates, hard citation gate, or broad runtime work unless Sprint 15 directly introduces a P0/P1 regression.