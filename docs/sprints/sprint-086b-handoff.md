---
title: Sprint 086b / S-Auto-31 handoff — CS4 readiness Part B (CaseSpec authoring)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: eval_interactive/case_specs/bad_cases/ (the 5 NEW + 2 EXTEND CaseSpecs) + eval_interactive/case_specs/bad_cases/_manifest.md
last_reviewed: 2026-06-08
supersedes: []
superseded_by: null
notes: >
  M-Auto-7 S-Y1 Part B — the eval-spec half of the last autoloop launch
  blocker. Authors the CS4 CaseSpecs the S-Y2 autoloop pilot will optimize
  against, reconciled to the Part A (086a) projection slots so the pilot runs
  on an honest baseline. Layer `eval_spec`; §7-REQUIRED. NO code / NO
  skill-procedure-text / NO schema change; NO real-LLM run in 086b
  (schema-compile + tiering only). 5 NEW CaseSpecs copied from
  docs/solutions/2026-06-08-cs4-casespec-drafts-appendix.md §2-§6 with the
  appendix placeholder ad_ids (AD-30xx) + IMAGE_QUALITY reconciled to real
  server/src/main/resources/mock/ fixtures (NO new server fixture authored);
  2 EXTEND diffs (§7-§8) applied to alice + wmkb. One reconciliation beyond
  the table: cs_uc_a_lookup_failed's appendix escalation_trigger was
  schema-invalid with should_escalate=false → set to null (OQ-086b.1). All 7
  schema-compile; outcome_checks + tool names valid. Per-sub-sprint Codex
  EXEMPT (folds into the M-Auto-7 milestone-shared review). Targets-fail /
  control-passes evidence is the pre-pilot re-bless (pilot Part C.1) after
  S-Y1 close (§12).
---

# Sprint 086b / S-Auto-31 — CS4 readiness Part B (CaseSpec authoring)

## §0 — Cold-start evidence table

| Item | Result |
|------|--------|
| **Goal** | Author the CS4 CaseSpecs the S-Y2 autoloop pilot optimizes against, reconciled to the merged Part A projection slots, so the pilot runs on an honest target / anti-误杀 baseline. Eval-spec authoring only; the pilot authors the procedure in S-Y2. |
| **Layer (§3.2)** | `eval_spec` (CaseSpec authoring; `outcome_checks` pin trace shape, not message keywords). |
| **5 NEW authored** | `case_specs/bad_cases/{cs_uc_a_no_ad_id_ad_specific, cs_uc_a_generic_policy_question, cs_uc_a_loaded_listing, cs_uc_fp_loaded_moderation, cs_uc_a_lookup_failed}.yaml` — copied from appendix §2-§6 with the §3.2 substitutions applied. |
| **2 EXTEND applied** | `alice_uc_a_uc_h_misclass.yaml` (appendix §7: `outcome_checks: []` → `[correct_uc, correct_outcome, tool_sequence_match]`, nothing else touched) · `wmkb_uc_a_trader_flag_secondary_uc_h.yaml` (appendix §8: `outcome_class either→resolve`; +`bot_handling_pattern`/`expected_tool_sequence`/`forbidden_tools`/`max_turns:6`; `hard_checks` 3→8 alice-parity; `outcome_checks: []` → 3 checks). |
| **Reconciliation — `cs_uc_a_no_ad_id_ad_specific`** | turn-2 `AD-3001` → `AD-1001` (LIVE, `mock/listings/live_ad.json`). email `sam.no.ad.id@example.com` kept (`ad_id:''`). Slot: `customer_context_status` `missing_ad_id` (T1) → `loaded` (T2). |
| **Reconciliation — `cs_uc_a_generic_policy_question`** | no ad_id (`ad_id:''`); email `jordan.generic@example.com` kept. Slot: `missing_ad_id` (correct — generic). Anti-误杀 negative-control. |
| **Reconciliation — `cs_uc_a_loaded_listing`** | `AD-3050` → `AD-2002` (LIVE, Home & Garden > Furniture, `mock/listings/carol_listing.json`). email → `carol.blocked@example.com` (seller_email). Narrative rewritten status=ACTIVE/Furniture → status=LIVE / Home & Garden > Furniture. Slot: `customer_context_status=loaded`; `moderation_reason_available=false`. |
| **Reconciliation — `cs_uc_fp_loaded_moderation`** | `AD-3060` → `AD-2001` (REMOVED, `mock/listings/alice_removed_prohibited.json`); `IMAGE_QUALITY` → `PROHIBITED_ITEM` (`mock/moderation_reviews/alice_prohibited_item.json`, `reason_display`). email → `alice.removed@example.com`. hidden_fact + bot_handling_pattern rewritten to PROHIBITED_ITEM. Slot: `moderation_reason_available=TRUE` + `loaded`. Alice's `AD-2001` fixture NOT altered (shared read-only). |
| **Reconciliation — `cs_uc_a_lookup_failed`** | form `AD-9999` KEPT (non-resolving — verified absent from `mock/`); turn-2 `AD-3070` → `AD-2002` (LIVE). email `casey.lookup.fails@example.com` kept. Slot: `lookup_failed` (T1) → `loaded` (T2). |
| **Fixture spot-check** | `AD-1001`/`AD-2001`/`AD-2002` each resolve to exactly one listing fixture; `AD-9999` has NO fixture (intended); only `AD-2001` has a moderation review (`PROHIBITED_ITEM`), `AD-2002` has none → the four targeted slot states are physically producible. |
| **Schema-compile (dry; no real-LLM)** | All 7 affected YAMLs load through `eval_interactive.case_spec.loader.load_case_spec` (runs `Expected.__post_init__` validation); full-directory load = 17 specs, unique `case_id`s. PASS. |
| **outcome_checks valid** | Every `outcome_check` ∈ `OutcomeChecker.ALL_CHECKS` — `correct_uc` / `correct_outcome` / `tool_sequence_match` on all 5+2; `turn_efficiency` additionally on `cs_uc_a_generic_policy_question`. PASS. |
| **tool-name check** | Every `expected_tool_sequence` + `forbidden_tools` entry exists in `server/src/main/resources/config/tool-policy.yaml` (12 tools). Sequences use `classify_use_case`/`get_customer_context`/`search_knowledge`/`resolve_article`/`record_outcome`; forbidden use `create_case_controlled`/`get_message_moderation_context`/`request_handover`/`get_moderation_review_context`. PASS. |
| **Tiering proposal (human gates)** | Tier-1 target ×3: `cs_uc_a_no_ad_id_ad_specific`, `cs_uc_a_loaded_listing`, `cs_uc_a_lookup_failed`. Tier-1 anti-误杀 negative-control ×1: `cs_uc_a_generic_policy_question`. Tier-2 neighbor ×1: `cs_uc_fp_loaded_moderation`. Lifecycle tier recorded as `scope-relevant` (CS4-surface-specific; named by the S-Y2 pilot scope) with the proposed Tier-1/Tier-2 role annotated in the `_manifest.md` ledger. |
| **`_manifest.md`** | 5 NEW ledger rows appended (status `opened (086b)`, M1–M4 `—`); dated "Sprint 086b" section added with the placeholder→fixture table, compile/lint result, the OQ-086b.1 reconciliation, and the EXTEND strengthening note. EXTEND rows keep their existing verdicts. |
| **Hard fences honored** | NO new server fixture (reuse path only). NO code / skill-procedure-text / schema / framework change. NO CaseSpec widened to accept current bot behaviour (§5.4 — `outcome_checks` pin trace shape, not `user_message` keywords). Anti-误杀 negative-control authored + not weakened. NO real-LLM run. |
| **§7 stanza** | §7-REQUIRED — embedded in the 086b dev prompt §3.4 (layer `eval_spec`; no Tier-0; no semantic hardcode; coverage target/neighbor/negative/shadow = 3/1/1/≥2). |
| **Codex (§4.3)** | Per-sub-sprint EXEMPT; DEFERRED to the M-Auto-7 milestone-shared close review. Not dispatched. The §5.6 tiering review is the human gate. |
| **Commit** | One new commit, separate from the 086a Part A commit; staged ONLY the 5 NEW + 2 EXTEND yamls + `_manifest.md` + this handoff. No `git add -A`. |

## §11 — Open questions

**OQ-086b.1 — `cs_uc_a_lookup_failed` escalation_trigger reconciled beyond the
§3.2 table (no intent lost).** The research-agent appendix §6 draft set
`escalation_trigger: lookup_failed_user_cannot_correct` together with
`should_escalate: false`. The CaseSpec schema rejects this on two counts: (1)
`Expected.__post_init__` raises `ValueError` ("escalation_trigger must be
empty/null when should_escalate=false"), and (2) the string is not a member of
the canonical `EscalationTrigger` enum (`schema.py:47-71`). Reconciled to
`escalation_trigger: null` — the only schema-valid form given
`should_escalate: false`. **No intent lost:** the degradation/escalate path the
draft trigger described stays fully expressed by `outcome_class: either` +
`acceptable_outcomes: [resolve, escalate]` + `closure_criterion` PASS condition
(c) ("Bot escalates with reason=user_requested or reason=lookup_failed …"). This
is NOT an eval-side widening to accept current bot behaviour (§5.4); the case
still pins `correct_uc` / `correct_outcome` / `tool_sequence_match`. **Decision
for deliver / human at §5.6 tiering:** confirm `null` is the intended structured
value (recommended), or, if the suite later wants a structured escalate pin on
this case, that requires `should_escalate: true` + a canonical enum value
(e.g. `service_degraded`) — a deliberate change to the case's required outcome,
not a copy fix.

**OQ-086b.2 — appendix schema path drift (non-blocking, doc-hygiene).** The
086b dev prompt §2 + the appendix cite the CaseSpec schema at
`eval_interactive/eval_interactive/specs/schema.py`; the real path is
`eval_interactive/eval_interactive/case_spec/schema.py` (and the loader is
`…/case_spec/loader.py`, the outcome-check registry
`…/scoring/outcome_checks.py`). Compile was run against the real path. Flagged
so a future prompt / appendix citation is corrected; no effect on the authored
cases.

**No placeholder failed to reconcile.** Every appendix placeholder resolved to
a real fixture per the §3.2 table; the lone STOP-condition (a reconciled ad_id
that does not resolve) did not fire. `AD-9999` is intentionally kept
non-resolving (it is the `lookup_failed` trigger), confirmed absent from
`mock/`.

## §12 — Re-bless deferred to pre-pilot Part C.1 (no real-LLM evidence in 086b)

Per the sprint contract, **no real-LLM run executes in 086b** — this is
eval-spec authoring. The deliverable is verified by dry schema-compile +
fixture spot-check + tool-name lint + diff inspection only. The
targets-fail / control-passes evidence — i.e. that the 3 Tier-1 targets +
the Tier-2 neighbor currently FAIL their `closure_criterion` against the
pre-pilot bot, and the anti-误杀 negative-control PASSES (the bot does NOT
already force ad_id elicitation on a generic UC-A question) — is the
**pre-pilot baseline re-bless = pilot Part C.1**, run by deliver-agent +
human after S-Y1 closes. Per §5.7, a mocked-LLM run cannot be primary
evidence that these cases discriminate the CS4 behaviour, because the mock
controls the measured variable; the real-LLM re-bless is the eval-evidence
gate.

Expected re-bless signal (forensic, for the Part C.1 reviewer):

- **Tier-1 targets** (`cs_uc_a_no_ad_id_ad_specific`, `cs_uc_a_loaded_listing`,
  `cs_uc_a_lookup_failed`): the pre-pilot bot should currently FAIL the entity-
  context closure_criterion (answers generically / ignores the loaded listing /
  ignores `lookup_failed`) — these are the failure modes the S-Y2 procedure is
  meant to close.
- **Tier-2 neighbor** (`cs_uc_fp_loaded_moderation`): should currently mis-route
  or cite a non-matching generic policy article instead of grounding in the
  `PROHIBITED_ITEM` moderation_review (the CS4 trace c891efb0 shape).
- **Anti-误杀 negative-control** (`cs_uc_a_generic_policy_question`): should
  currently PASS (direct FAQ-grounded answer, no ad_id elicitation). The S-Y2
  pilot procedure MUST NOT flip this to FAIL — it is the binding gate against
  over-correcting the targets into forced ad_id elicitation on generic turns.

Tiering is finalized by deliver-agent + human at the §5.6 review (the proposed
Tier-1/Tier-2 roles are recorded in `_manifest.md`; the human gates the final
tiering).
