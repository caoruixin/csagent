# Sprint 20 Handoff — G2 Interactive Case Family + Shadow Split (Track A) + Already-Called Soft Signal (Track B)

Date: 2026-05-13
Branch: `design-v1-without-human-review`
Sprint class: two-track, semantic-touching. Track A is content-authoring
on the eval-spec surface (not subject to Bundle-or-defer). Track B is
the canonical `prompt_projection` bundle per Sprint 19 §"Bundle-or-defer
policy" — bundled with a regression test. Outcome: both tracks land.
Track A delivers 10 case families (70 CaseSpec-shaped entries) + the v0
shadow-split mechanism. Track B delivers the `already_called`
projection slot + 7 new unit tests; full server suite 894 / 0 / 0 / 1.

## 1. Context Pack

Per `docs/current/agent_context_guide.md` Context Pack Prompt; produced
before any code or CaseSpec authoring. **Pasted in chat 2026-05-13,
human confirmed all five §7 path proposals (case-family directory,
shadow-split mechanism, manifest shape, projection-slot wiring,
promotion-vs-reference) on the same date.**

**Relevant docs.**

- `AGENTS.md` (durable-connective; current) — constitution-chain entry.
- `docs/current/doc_governance.md` (durable-connective; current) —
  front-matter schema; this handoff is `current-runtime` per-sprint.
- `docs/current/agent_context_guide.md` (durable-connective; current) —
  per-task reading list; Track A used "Eval, governance" + Track B used
  "Runtime, phase machine, drift".
- `docs/current/iteration_governance.md` (durable-connective; current) —
  §1 Constitution, §3.2 Q2 cs_192 stop-gate, §5.1 four-class
  generalization-coverage contract (Track A is the deliverable), §6
  architecture-health metrics still `not_started`, §7 multi-layer
  prospective stanza.
- `docs/sprint_objective.md` (current-runtime; current) — Sprint 20
  scope and "Do not implement" hard fence.
- `docs/sprints/sprint-019-handoff.md` (sprint-archive; archived) — §3
  cluster annotations + §4.2 specifies the Track B slot shape.
- `docs/sprints/sprint-018-handoff.md` (sprint-archive; archived) — §5.1
  Method note: L3 override check by `source_session_id`; §5.2
  Manual-probe ground-truth chain (phase 2 + bot
  `phase_plan.system_instruction`).
- `docs/diagnostics/failure-briefs/*.md` × 10 (diagnostic; current) —
  read in full; each brief's "Is this a one-off or a pattern?" + "What
  should NOT be done?" sections drove neighbor and negative authoring.

**Relevant code paths.**

- Track A surface: `eval_interactive/case_specs/` (4 existing sub-dirs;
  none edited), `eval_interactive/case_spec_overrides.yaml` (13 approved
  entries; verified cs_011, cs_015, cs_095 only have approved overrides),
  `eval_interactive/eval_interactive/case_spec/schema.py` (CaseSpec
  dataclass + 23-value `EscalationTrigger` Literal),
  `eval_interactive/eval_interactive/batch/sets.py` (CaseSetManager;
  `_KNOWN_SETS = (anchor, promotion, exploration, smoke)` —
  case_families and case_specs_shadow are intentionally NOT in this
  tuple, so they are not loaded by any existing `--set` flag),
  `eval_interactive/scripts/llm_review_specs.py` +
  `eval_interactive/eval_interactive/case_spec/llm_persona_reviewer.py`
  (Wave A6.1 persona-review shadow audit — naming collision only;
  unrelated to §5.1 generalization shadow).
- Track B surface:
  `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
  (884→988 lines after edit; `build(...)` at line 707 is the new 6-arg
  overload; legacy 5-arg overload at line 653 delegates),
  `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
  line 171–528 (call-site at line 175 updated to pass `toolEvents`),
  `server/src/main/java/com/gumtree/csagent/model/ToolEvent.java`
  (record `(sequenceIndex, stepIndex, toolName, arguments, success,
  resultData, errorMessage, latencyMs)` — `stepIndex` becomes the
  `at_step` field in the new slot).

**Doc status warnings.**

1. Sprint 19 §4.2 already specifies the Track B slot shape; no drift.
2. Sprint 18 §4 calls the regressed cases "Cluster C"; Sprint 19 reads
   them as "regression-shape baseline noise". Implication for Track A:
   cs_011 family target = the brief's 2026-05-05 PASS shape (not the
   2026-05-10 Cluster C empty-escalation_reason regression).
3. "shadow" name collision in `eval_interactive/`. The Wave A6.1
   persona-review shadow-audit (`llm_persona_reviewer.py`) is unrelated
   to §5.1 generalization-coverage shadow. Sprint 20's v0 mechanism
   lives in `eval_interactive/case_specs_shadow/`, separate from the
   persona-review pipeline.
4. Hand-authored CaseSpecs are not L1-extracted. Mitigated by placing
   them in a new sub-directory the existing linter / regenerator does
   not scan (`case_specs/case_families/` and `case_specs_shadow/`).
5. `docs/runtime_freeze_and_risk_policy.md` Tier-0 list — not edited;
   cs_192 stop-gate did NOT fire (no Java-guard authoring surfaced).

**Source-of-truth decisions.**

- Track A targets for 9 smoke briefs: existing
  `eval_interactive/case_specs/smoke/<case_id>.yaml` is authoritative
  for the user shape (form_context + persona + seed_messages); approved
  L3 override in `case_spec_overrides.yaml` (keyed by
  `source_session_id`) is authoritative for expected behaviour when
  present (cs_011, cs_015, cs_095). For briefs without an override
  (cs_001 / cs_038 / cs_040 / cs_176 / cs_192 / cs_259), the brief's
  "What should a good CS agent have done?" + phase 2 UC policy form the
  derived expected (per Sprint 18 Method note §5.1).
- Manual-probe target: phase 2 UC-A policy + the brief's quoted
  `phase_plan.system_instruction` MUST clause (per Sprint 18 §5.2).
- Track B slot shape: Sprint 19 §4.2 + Sprint 20 objective §"Tracks
  (in scope) Track B" — `already_called: [{tool, arguments_hash,
  at_step}]`.
- Argument hash: stable JSON canonicalization (Jackson with
  `ORDER_MAP_ENTRIES_BY_KEYS`) → SHA-256 → first 16 hex chars. No prior
  precedent in the codebase (zero hits on `argumentsHash` /
  `arguments_hash` in `server/src` at sprint-start).

**Implementation status (sprint-start).** `not_started` on both tracks.
No `case_specs/case_families/` directory; no `case_specs_shadow/`
directory; no `already_called` slot; no `priorToolEvents` parameter on
projection builder. All verified by grep.

**Top risks before coding (now-resolved annotations in parentheses).**

1. CaseSpec linter / regenerator may scan new hand-authored CaseSpecs
   and fail validation or attempt to overwrite. (RESOLVED — new files
   live under `case_families/` and `case_specs_shadow/`, neither of
   which the existing linter / regenerator scans.)
2. cs_192 target authoring may trigger §3.2 Q2 stop-gate. (DID NOT
   FIRE — see §3 / §11.)
3. Neighbor / negative pool is shallow inside `smoke/`. (RESOLVED —
   hand-authored all 60 neighbor / negative / shadow CaseSpecs from
   first principles + brief evidence, with `source_dataset:
   case_family_authored` for grep-able provenance.)
4. Two-slot semantic (`accumulated_tool_results` last-write-wins vs
   `already_called` per-call list). (CONFIRMED INTENDED — Track B
   regression test bar `alreadyCalled_includesEveryRepeatedSuccessful­
   Dispatch` confirms both slots coexist correctly.)
5. Manual-probe target requires synthesising the form-vs-listing ad_id
   mismatch as a hidden_fact. (RESOLVED — see §3 manual_probe family.)

## 2. Sprint-objective recap

From `docs/sprint_objective.md` §Goal (quoted verbatim, key clauses):

> Two disjoint tracks under one sprint scope, ordered by priority:
>
> - **Track A — G2 Interactive Case Family + Shadow Split.** Convert
>   the 10 G1 Failure Briefs … into the four-class case-family
>   structure required by `docs/current/iteration_governance.md` §5.1
>   (target / neighbor / negative / shadow). Authoring only; **no
>   remediation of any brief's underlying failure.** Build the
>   shadow-split mechanism that hides shadow-class cases from the dev
>   agent while keeping them readable to the human and the review
>   agent.
> - **Track B — `R-prompt-projection-already-called-soft-signal`.**
>   Surface an `already_called: [{tool, arguments_hash, at_step}]`
>   diagnostic slot in the per-step projection … Soft signal only. The
>   LLM owns whether to re-emit. Includes a regression test that
>   demonstrates the slot is populated and that no Java check enforces
>   consumption.

## 3. Track A — 10 case families

Each family lives under
`eval_interactive/case_specs/case_families/<family_id>/{target,neighbor,negative}/`;
shadow cases live under
`eval_interactive/case_specs_shadow/case_families/<family_id>/`. The
visible manifest at
`eval_interactive/case_specs/case_families/_manifest.yaml` is the index
for the human + review agent; the shadow manifest at
`eval_interactive/case_specs_shadow/_manifest.yaml` reveals the
otherwise-redacted shadow case ids. Per §7.5 Option A, the 9 smoke
targets are referenced by manifest (no file duplication). The
manual-probe family target is hand-authored fresh under
`case_families/manual_probe_uc_a_resolve_must/target/`.

### 3.1 cs015_uc_fp_mis_route

- Brief: `docs/diagnostics/failure-briefs/cs015-uc-fp-mis-route-and-premature-escalate.md`
- Primary layer: `prompt_projection`; secondary: `semantic_planner`
- L3 override: **approved** Wave A6 (source_session_id
  570Q5000008WmXxIAK, `case_spec_overrides.yaml` lines 64–98).
  `eval_spec` ruled out for the outcome failure per Sprint 18 Method
  note.
- Target: `cs_interactive_015` (referenced; no file duplication).
- Neighbors: `cs15n01_uc_fp_appeal_after_takedown`,
  `cs15n02_uc_fp_understand_removal`. Same UC-FP deletion-explanation
  surface, different brand / topic detail to exercise the routing
  signal without re-triggering the empty-ad_id mis-route shape.
- Negatives: `cs15g01_uc_a_top_ad_visibility` (real UC-A Top-Ad
  question with a populated ad_id), `cs15g02_uc_b_repost_after_edit`
  (clean UC-B posting-edit guidance). A remediation that hard-routes
  "ad-deletion-shape" to UC-FP would mis-fire on both.
- Shadow: 2 cases (redacted in visible manifest; see shadow manifest).
  Authoring rationale: exercise the same empty-ad_id routing surface
  with content shapes distinct from the visible neighbors (image-flag
  takedown; keyword-match appeal).

### 3.2 cs001_uc_c_template_escalate

- Brief: `cs001-uc-c-template-escalate-on-faq-miss.md`
- Primary: `prompt_projection`; secondary: `semantic_planner`
- L3 override: NONE. CaseSpec flagged for L3 triage per brief Related
  observation #1 (`R-cs001-escalation-trigger-l3-review`,
  `R-generator-get-customer-context-policy-mismatch`).
- Target: `cs_interactive_001` (referenced).
- Neighbors: `cs01n01_uc_c_inbox_filter_question` (UC-C
  inbox-folder-routing question, resolvable),
  `cs01n02_uc_c_messaging_general` (UC-C general messaging issue
  that legitimately needs escalation but WITH acknowledgment).
- Negatives: `cs01g01_uc_c_user_requested_handover` (explicit
  user_requested first turn — quick handover IS correct),
  `cs01g02_uc_c_strong_distress_signal` (ALL-CAPS distress matching
  Sprint 2 §B1 detector — user_distress IS correct). A remediation
  that universally suppresses the template would mis-fire here.
- Shadow: 2 cases (notification-lag detail; post-form inbox-empty).

### 3.3 cs011_uc_d_description_ignored

- Brief: `cs011-uc-d-detailed-description-ignored-on-faq-miss.md`
- Primary: `prompt_projection`; secondary: `semantic_planner`
- L3 override: **approved** Sprint 4 §E1 (source_session_id
  570Q5000008NWIjIAO, `case_spec_overrides.yaml` line 164). `eval_spec`
  ruled out for the outcome failure.
- Target: `cs_interactive_011` (referenced; the brief's 2026-05-05
  PASS-by-CaseSpec shape — Cluster C regression-noise excluded).
- Neighbors: `cs11n01_uc_d_2fa_loop_verbose` (multi-symptom 2FA
  rejection reproduction), `cs11n02_uc_d_email_sync_detailed`
  (multi-symptom email-change sync detail).
- Negatives: `cs11g01_uc_d_brief_user_requested` (terse explicit
  handover — user_requested is correct),
  `cs11g02_uc_d_explicit_distress` (ALL-CAPS account-compromise
  distress — user_distress IS correct).
- Shadow: 2 cases (two-emails-one-account; password-loop with
  high-frustration but no §B1-detector hit — confirms that
  faq_miss IS the truthful reason when §E1 gate downgrades).

### 3.4 cs038_uc_j_intake_redundancy

- Brief: `cs038-uc-j-intake-redundancy-and-jargon-framing.md`
- Primary: `prompt_projection`; secondary: `skill_state`
- L3 override: NONE. CaseSpec flagged for L3 triage per brief Related
  observation #2 (`R-cs038-l3-review-intake-efficiency`,
  `R-l3-judge-form-context-trust-rubric`).
- Target: `cs_interactive_038` (referenced).
- Neighbors: `cs38n01_uc_j_fraud_narrative_complete` (full intake
  narrative on T1 — bot must close intake without re-asking),
  `cs38n02_uc_j_harassment_report_complete` (different UC-J shape,
  same complete-narrative pattern).
- Negatives: `cs38g01_uc_j_sparse_user_info_intake_required`
  (legitimate field-by-field intake required),
  `cs38g02_uc_j_genuine_report_partial` (one-of-three fields
  supplied — focused 1-2 question intake is correct).
- Shadow: 2 cases (full-narrative scam + harassment variants).
- `answer_must_not_contain` includes the explicit strings
  `"trust & safety report intake"` and `"technical issue intake"` so a
  remediation that ships the jargon-fix can be measured directly.

### 3.5 cs040_uc_k_disengaged_jargon

- Brief: `cs040-uc-k-disengaged-jargon-intake-false-complete.md`
- Primary: `prompt_projection`; secondary: `semantic_planner`
- L3 override: NONE. CaseSpec flagged for L3 triage per brief Related
  observation #3 (`R-cs040-l3-review-intake-completion-semantics`).
- Target: `cs_interactive_040` (referenced).
- Neighbors: `cs40n01_uc_k_app_crash_detail` (app-crash + iOS
  platform), `cs40n02_uc_k_payment_button_flaw` (Pay-for-Featured
  button — partial intake supplied, focused finish required).
- Negatives: `cs40g01_uc_k_intake_complete_from_form` (form_context
  already supplies BOTH intake fields — bot must escalate without
  re-asking), `cs40g02_uc_k_explicit_user_requested` (UC-K shape with
  explicit user_requested — that reason wins over intake_complete).
- Shadow: 2 cases (login-intermittent with repro; image-upload-timeout
  with platform — both probe whether the bot can recognise intake
  already complete from form_context).

### 3.6 cs095_uc_classification_account_aware

- Brief: `cs095-uc-classification-and-account-aware-path-skipped.md`
- Primary: `prompt_projection`; secondary: `skill_state`
- L3 override: **approved** Wave A2.1 legacy (source_session_id
  570Q5000008U5C9IAK, `case_spec_overrides.yaml` line 311);
  **classification block is UNDER L3 RE-REVIEW** per
  `R-cs095-uc-classification-l3-rereview` (brief Related observation
  #1). For this family, the override pins UC-A primary, but the human
  re-review identifies UC-D primary; manifest records both.
- Target: `cs_interactive_095` (referenced).
- Neighbors: `cs95n01_uc_d_app_login_email_mismatch` (split state
  across app vs website), `cs95n02_uc_d_no_ads_visible_state` (no-ads
  with buyer-can-still-see-one mismatch).
- Negatives: `cs95g01_uc_a_pure_visibility_question` (clean UC-A
  timing question — no account-state to verify),
  `cs95g02_uc_d_pure_email_change_question` (clean UC-D email-change
  procedure — no symptoms requiring `get_customer_context`).
- Shadow: 2 cases (phone-change cascade; browser-conditional account
  state).

### 3.7 cs176_uc_e_wrong_escalation_reason

- Brief: `cs176-uc-e-wrong-escalation-reason-family.md`
- Primary: `semantic_planner`; secondary: `prompt_projection`
- L3 override: NONE. CaseSpec flagged for L3 triage per brief Related
  observation #2 (`R-cs176-escalation-reason-l3-review`).
- Target: `cs_interactive_176` (referenced).
- Neighbors: `cs76n01_uc_e_refund_demand` (explicit refund demand —
  user_requested), `cs76n02_uc_e_fulfillment_demand` (fix-or-refund
  ultimatum — user_requested).
- Negatives: `cs76g01_uc_e_policy_question_faq_path` (clean UC-E
  policy question — faq_miss IS appropriate if FAQ misses),
  `cs76g02_uc_e_clarification_first_turn` (ambiguous UC-E opener that
  needs clarification before deciding user_intent vs bot_limit).
- Shadow: 2 cases (paid Top Ad not running with monetary demand;
  Featured Listing demoted mid-cycle).

### 3.8 cs192_uc_b_giveaway

- Brief: `cs192-uc-b-mechanical-escalate-on-resolvable-giveaway-question.md`
- Primary: `prompt_projection`; secondary: `semantic_planner`
- L3 override: NONE. CaseSpec flagged for L3 triage per brief Related
  observation #4 (`R-cs192-secondary-ucs-duplicate-uc-b`).
- Target: `cs_interactive_192` (referenced).
- **§3.2 Q2 stop-gate check (cs_192 reminder): DID NOT FIRE.** The
  target authoring stays on the brief's 2026-05-05 PASS-by-CaseSpec
  shape, where `active_use_case=UC-B` was correctly stamped. The
  Cluster C 2026-05-10 regression's `CONTRACT_VIOLATION:active_use_case`
  shape is regression noise per Sprint 19 §3 and is not the target.
  No Java-guard remediation is asked for in the family; no Tier-0
  candidate arises. The cs_192 family is authored.
- Neighbors: `cs92n01_uc_b_free_pickup_listings`,
  `cs92n02_uc_b_zero_pound_pricing` (basic UC-B posting questions,
  resolvable).
- Negatives: `cs92g01_uc_b_obscure_policy_genuine_faq_miss` (niche
  livestock-policy question — faq_miss IS appropriate),
  `cs92g02_uc_b_explicit_user_requested` (explicit handover — quick
  user_requested escalation).
- Shadow: 2 cases (charity-donation framing; pickup-only no-price).

### 3.9 cs259_uc_f_payment_question

- Brief: `cs259-uc-f-sprint7-i0-violation-on-payment-question.md`
- Primary: `semantic_planner`; secondary: `prompt_projection`
- L3 override: NONE (no specific L3 ask per brief Related observation
  — outcome IS correct, surface is the §I0 directive violation).
- Target: `cs_interactive_259` (referenced).
- Neighbors: `cs59n01_uc_b_empty_form_posting_question`,
  `cs59n02_uc_c_empty_form_messages_question` — additional shapes
  that activate Sprint 7 §I0 across UC-B and UC-C (brief §"Is this a
  pattern?" called these out explicitly).
- Negatives: `cs59g01_uc_f_obscure_payment_taxation` (obscure UC-F
  HMRC question — faq_miss IS appropriate after real search),
  `cs59g02_uc_f_user_requested_first_turn` (explicit handover from
  T1 overrides §I0).
- Shadow: 2 cases (empty-form UC-D account-recovery; empty-form UC-F
  payout-timing).

### 3.10 manual_probe_uc_a_resolve_must

- Brief: `manual-probe-2026-05-13-ad-visibility-multi-layer-failure.md`
- Primary: `semantic_planner`; secondary: `prompt_projection`
- L3 override: N/A (manual probe; no CaseSpec; ground truth derived
  per Sprint 18 §5.2 from phase 2 UC-A + bot
  `phase_plan.system_instruction` MUST clause quoted in the brief).
- Target: **hand-authored** —
  `case_specs/case_families/manual_probe_uc_a_resolve_must/target/cs_manual_probe_target_uc_a_resolve_must.yaml`.
  source_session_id retains the real
  `4a2f3680-02a8-4d13-8f0b-f99d7c249b57` so a future trace lookup
  matches; `source_dataset: case_family_authored` flags the
  hand-authored origin. Form fields mirror the brief's quoted form
  context exactly. `hidden_facts` carries the ad-1003 vs AD-1001
  form/listing mismatch so the simulator can reproduce the
  data-inconsistency signal.
- Neighbors: `csmp_n01_uc_a_visibility_search_hits` (UC-A visibility
  with viable FAQ hits), `csmp_n02_uc_a_top_ad_status_question`
  (UC-A Top Ad activation timing).
- Negatives: `csmp_g01_uc_a_genuine_faq_miss_obscure` (niche
  per-language ranking question — faq_miss IS appropriate after a
  real resolve_article attempt), `csmp_g02_uc_a_user_requested_specialist`
  (explicit handover — user_requested overrides resolve).
- Shadow: 2 cases (ad_id mismatch reproduction; multi-turn pivot
  where bot must sustain past T0).

### 3.11 Authoring discipline checks

For every CaseSpec authored under `case_families/` and
`case_specs_shadow/`, the following are verified:

- No keyword / regex / per-UC matrix encoded into YAML content. The
  spec describes *user shapes + expected behaviours*; layer-routing
  decisions are not encoded.
- `escalation_trigger` is gated correctly: when `should_escalate:
  true` the trigger is non-null and is a member of the 23-value
  `EscalationTrigger` Literal; when `should_escalate: false` the
  trigger is `null`.
- `bot_handling_pattern` is non-empty and describes *what the bot
  should do*, not *how the runtime should be patched*.
- `source_dataset: case_family_authored` and `source_session_id:
  synthetic-sprint20-...` make hand-authored provenance grep-able.
- L3 override check: for each family target, the manifest records
  whether an approved override exists in
  `eval_interactive/case_spec_overrides.yaml` (cs_011 / cs_015 /
  cs_095 yes; rest no), and the family authoring respects that.

## 4. Track A — Shadow-split mechanism v0

**File layout.**

```
eval_interactive/
├── case_specs/
│   ├── anchor/ promotion/ exploration/ smoke/    (existing, unchanged)
│   └── case_families/                            NEW
│       ├── _manifest.yaml                        NEW (visible index, shadow case ids REDACTED)
│       ├── cs001_uc_c_template_escalate/
│       │   ├── neighbor/{cs01n01..., cs01n02...}.yaml
│       │   └── negative/{cs01g01..., cs01g02...}.yaml
│       ├── (8 more visible families)
│       └── manual_probe_uc_a_resolve_must/
│           ├── target/cs_manual_probe_target_uc_a_resolve_must.yaml  (only target file)
│           ├── neighbor/{csmp_n01..., csmp_n02...}.yaml
│           └── negative/{csmp_g01..., csmp_g02...}.yaml
└── case_specs_shadow/                            NEW (sibling of case_specs/)
    ├── _ACCESS_BOUNDARY.md                       NEW (who may read what)
    ├── _manifest.yaml                            NEW (reveals shadow case ids)
    └── case_families/
        ├── cs001_uc_c_template_escalate/
        │   └── {cs01s01..., cs01s02...}.yaml
        └── (9 more shadow families)
```

**Naming convention.**

| class | id pattern | example |
| --- | --- | --- |
| target (9 smoke briefs) | existing `cs_interactive_NNN` | `cs_interactive_015` (referenced) |
| target (manual probe) | `cs_manual_probe_target_<slug>` | `cs_manual_probe_target_uc_a_resolve_must` |
| neighbor | `cs<NN>n<MM>_<slug>` | `cs15n01_uc_fp_appeal_after_takedown` |
| negative | `cs<NN>g<MM>_<slug>` | `cs15g01_uc_a_top_ad_visibility` |
| shadow | `cs<NN>s<MM>_<slug>` | `cs15s01_uc_fp_empty_ad_id_image_flag` |
| (manual-probe variants) | `csmp_{n,g,s}<MM>_<slug>` | `csmp_n01_uc_a_visibility_search_hits` |

**Runner gate.** The existing CaseSetManager at
`eval_interactive/eval_interactive/batch/sets.py:17` declares
`_KNOWN_SETS = (anchor, promotion, exploration, smoke)`. Neither
`case_families/` nor `case_specs_shadow/` is in this tuple, so the
default invocations (`eval-interactive run --set anchor` etc.) do not
load these CaseSpecs. Custom-path invocations (`--path
eval_interactive/case_specs/case_families/cs015_uc_fp_mis_route/neighbor`)
work via `manager.load_custom(path)` for human + review-agent review.

Sprint 20 does **not** add a `--include-shadow` flag to the CLI; the
runner-flag gate is documented in `_ACCESS_BOUNDARY.md` as a v1
hardening direction, and the v0 mechanism relies on the directory
boundary + custom-path-only loading for shadow access. The objective
allows "v0 may be lightweight"; an open question is recorded in §11
for the human to decide whether a `--include-shadow` flag is needed in
the next eval-governance sprint.

**Access-boundary documentation.** `case_specs_shadow/_ACCESS_BOUNDARY.md`
records: who may read what (dev agent: no; human + review agent +
`--include-shadow` runner: yes), how the boundary is enforced (three
layers: directory boundary, runner default-off, documented
self-restraint), and v0 gaps the human should be aware of.

**Harness files added.** Three files, all new:

- `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md`
- `eval_interactive/case_specs_shadow/_manifest.yaml`
- `eval_interactive/case_specs/case_families/_manifest.yaml`

No edit to `eval_interactive/eval_interactive/*` Python; no edit to
`eval_interactive/scripts/*`; no edit to existing CaseSpec sub-dirs.

## 5. Track B — `already_called` slot

**Final slot shape.** Per Sprint 19 §4.2 + objective:

```json
"already_called": [
  {"tool": "search_knowledge", "arguments_hash": "<16 hex chars>", "at_step": 0},
  {"tool": "search_knowledge", "arguments_hash": "<16 hex chars>", "at_step": 1}
]
```

The slot is **always present** on the projection (empty array `[]` when
no prior successful call exists) for projection shape stability per the
§N0 nullable-field convention.

**Argument-hash function.** Lives in
`ContextProjectionBuilder.canonicalArgumentsHash(Map<String,Object>)`
(package-private for unit testing). Steps:

1. Jackson `ObjectMapper` configured with
   `SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS=true` serialises the
   arguments map to canonical JSON (lexicographic key order — two
   ToolCalls with identical content but different insertion order
   produce identical canonical JSON).
2. SHA-256 over the UTF-8 bytes.
3. First 16 hex chars = 64 bits, comfortably collision-free for the
   per-run scale of a single `AgentRunLoop.run(...)` invocation
   (bounded by `plan.maxToolSteps()`).
4. On `NoSuchAlgorithmException` or serialisation failure, returns the
   sentinel string `"hash_error"` so the slot remains parseable.

**At-step semantics.** `ToolEvent.stepIndex()` is the 0-based outer-loop
step at which the dispatch landed (per the existing record contract at
`server/src/main/java/com/gumtree/csagent/model/ToolEvent.java:21`).
Two identical-args dispatches at different steps produce two slot
entries with the same `arguments_hash` but different `at_step`.

**Slot derivation source.** `AgentRunLoopImpl.toolEvents:
List<ToolEvent>` is the per-run cumulative dispatch record (already
accumulated by the existing code at lines 444, 320). The new build()
overload reads this list. Only events with `success == true` produce a
slot entry; plan-rejected and intake-guard-rejected events are
excluded because they were not dispatched.

**Regression test.**
`server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledProjectionTest.java`
(7 tests, all passing). Covers the three sprint-objective behaviour
bars:

- **Bar (a) — slot populated when prior identical-args call exists**:
  `alreadyCalled_populatedWhenPriorIdenticalArgsCallExists` and
  `alreadyCalled_canonicalArgumentsHash_orderInsensitive`.
- **Bar (b) — slot empty otherwise**:
  `alreadyCalled_emptyArrayOnFirstStep` (empty list input) and
  `alreadyCalled_emptyArrayOnNullPriorToolEvents` (null input —
  defensive).
- **Bar (c) — runtime does NOT short-circuit on slot population**:
  `alreadyCalled_includesEveryRepeatedSuccessfulDispatch`. Two
  identical-args dispatches at distinct steps produce TWO slot entries
  with the same hash and distinct `at_step` values, demonstrating that
  the slot is not deduplicating and the dispatcher did not short-circuit
  the second call.

Plus:
- `alreadyCalled_excludesUnsuccessfulEvents` — plan-rejected events
  are excluded from the slot (they were not "already called").
- `legacyBuildOverload_stillEmitsEmptyAlreadyCalledSlot` — the 5-arg
  overload (preserved for non-loop callers) still emits the empty-array
  slot for projection shape stability.

Full server suite: 894 tests, 0 failures, 0 errors, 1 pre-existing skip
(`Sprint16HandoverDualPathReproTest`). Test runtime ~27 s.

## 6. Files changed

### Track A — eval_interactive (61 new files + 0 edited existing)

- `eval_interactive/case_specs/case_families/_manifest.yaml` — NEW.
  Visible-class manifest. Lists all 10 families with target / neighbor
  / negative case ids; shadow case ids redacted.
- `eval_interactive/case_specs/case_families/<family>/target/<id>.yaml`
  — 1 NEW (manual-probe target only). 9 smoke targets referenced via
  manifest, not duplicated, per §7.5 Option A.
- `eval_interactive/case_specs/case_families/<family>/neighbor/<id>.yaml`
  — 20 NEW (2 per family × 10 families).
- `eval_interactive/case_specs/case_families/<family>/negative/<id>.yaml`
  — 20 NEW (2 per family × 10 families).
- `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md` — NEW.
  Access boundary documentation; who may read what + v0 enforcement
  layers + known gaps.
- `eval_interactive/case_specs_shadow/_manifest.yaml` — NEW. Shadow
  manifest revealing the 20 redacted shadow case ids per family.
- `eval_interactive/case_specs_shadow/case_families/<family>/<id>.yaml`
  — 20 NEW (2 per family × 10 families).

No edit to `eval_interactive/case_specs/{anchor,promotion,exploration,smoke}/*`,
`case_spec_overrides.yaml`, `personas*.yaml`, `eval_interactive.yaml`,
or any Python file under `eval_interactive/eval_interactive/` or
`eval_interactive/scripts/`.

### Track B — server (1 edited main + 1 edited main + 1 new test + 14 edited tests)

- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
  — EDITED. Added Jackson `SerializationFeature` + `ToolEvent` +
  java.security imports; added private `argumentsHashMapper` field;
  added 6-arg `build(...)` overload that emits `already_called`;
  added private `buildAlreadyCalledNode(List<ToolEvent>)` helper; added
  package-private `canonicalArgumentsHash(Map<String,Object>)` helper.
  Legacy 5-arg `build(...)` overload preserved, now delegates to the
  6-arg overload with an empty list (projection shape stable for
  non-loop callers).
- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
  — EDITED. Line 175 (projection-build call site) now passes
  `toolEvents` as the new 6th argument. No other change. No
  short-circuit on the new slot.
- `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledProjectionTest.java`
  — NEW. 7 unit tests covering the three sprint-objective behaviour
  bars + canonical-hash order invariance + legacy-overload shape
  stability.
- 14 existing test files — EDITED. Each had a Mockito stub or verify
  call for the 5-arg `build(...)` signature; updated to the 6-arg
  signature (`any()` added). No test-logic change; only the mock
  signature widened to match the new call site. Files:
  - `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAd1002IntegrationTest.java`
  - `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopConfirmCloseIntegrationTest.java`
  - `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopDeadlineExceededBotResponseIntegrationTest.java`
  - `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopHandoverReasonNormalizationIntegrationTest.java`
  - `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopIntakeIntegrationTest.java`
  - `server/src/test/java/com/gumtree/csagent/integration/Cs014RouteAndLoopHandoverIntegrationTest.java`
  - `server/src/test/java/com/gumtree/csagent/integration/Sprint141FaqGroundingTracePersistenceTest.java`
  - `server/src/test/java/com/gumtree/csagent/integration/Sprint9TraceObservabilityFidelityIntegrationTest.java`
  - `server/src/test/java/com/gumtree/csagent/service/runtime/AgentRunLoopClarificationDetectionTest.java`
  - `server/src/test/java/com/gumtree/csagent/service/runtime/AgentRunLoopImplTest.java`
  - `server/src/test/java/com/gumtree/csagent/service/runtime/AgentRunLoopMaxStepsRawResponseTest.java`
  - `server/src/test/java/com/gumtree/csagent/service/runtime/AgentRunLoopS1FaqGroundedResolveGuardTest.java`
  - `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint81DiscoverPhaseBoundaryTest.java`
  - `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint9TerminalToolHonestyTest.java`

No edit to `server/src/main/java/com/gumtree/csagent/service/tools/*`,
`server/src/main/java/com/gumtree/csagent/model/ToolEvent.java` (slot
reads the existing record contract, no schema change), or any prompt
template / system prompt.

### Docs

- `docs/sprints/sprint-020-handoff.md` — NEW (this file).
- `docs/10-handoff.md` — UPDATED. Current-phase paragraph rewritten to
  lead with Sprint 20. Sprint 19 demoted to preceding-sprint paragraph
  (its detail paragraph below the lead is preserved).

No edit to `docs/sprint_objective.md` (deliver-agent surface), any
sprint archive under `docs/sprints/sprint-001..019-*`,
`docs/codex-findings.md` (Codex writes at sprint close),
`docs/current/iteration_governance.md`, `docs/current/doc_governance.md`,
`docs/current/agent_context_guide.md`, `docs/foundational/*`,
`docs/runtime_freeze_and_risk_policy.md`, or `docs/action_bank.md`
(action-bank deltas in §12 below are recorded here for the deliver
agent to apply on close).

## 7. Layer-classification self-walk

Restating the multi-layer prospective stanza from
`docs/sprint_objective.md` §"Layer-classification + anti-hardcode
stanza" against the actual deliverables.

| track | declared layer | walk + actual |
| --- | --- | --- |
| Track A — case-family authoring | `eval_spec` | Hand-authored CaseSpecs live on the eval surface. Per the objective §"Layer-classification + anti-hardcode stanza", the per-case §3.2 walk is not applied to authoring sprints. No CaseSpec encodes a runtime decision. |
| Track A — shadow-split mechanism | `infra` | The mechanism is an eval-harness addition (directory layout + access-boundary doc + runner default behaviour). No semantic decision. |
| Track B — `already_called` slot | `prompt_projection` | §3.2 walk: Q1 no (no infra failure), Q2 no Tier-0 (the slot does not protect a current invariant), Q3 YES — the LLM lacked a soft signal about prior identical-args calls in the same run, and surfacing the slot would let the LLM avoid the manual-probe shape. First-match-wins → `prompt_projection`. Confirmed. |

§3.3 "no Java guard by default" check: no walk landed on `java_guard`.
No `human_review_required` flag fired. The cs_192 stop-gate (objective
§"Cs_192 reminder") did NOT trigger during target authoring — see
§3.8.

## 8. Anti-hardcode self-walk (§4.1, 9 questions)

The Sprint 20 PR has a Track B `prompt_projection` bundle plus the
Track A content-authoring deliverable. Track A is exempt as content
authoring; Track B is semantic-touching and answers below.

1. **Keyword / regex / if-else / enum / per-UC matrix added for a
   semantic decision?** No. Track B adds an observability slot to the
   projection; it does not branch on tool name, UC, or user content.
   The slot is a list of {tool, hash, step} records derived
   mechanically from the existing `toolEvents` accumulator.
2. **Tier-0 invariant justification?** N/A (Q1 = no). No Tier-0
   invariant added; the slot does not enforce anything.
3. **Soft-signal projection alternative considered?** YES — the slot
   IS the soft signal. The LLM owns whether to act on it; the runtime
   does not short-circuit. This is the canonical
   `prompt_projection` bundle per Sprint 19 §4.2 + Sprint 20
   §"Bundle-or-defer policy".
4. **Visible-eval case text encoded into runtime / prompt / judge?**
   No. No `cs_NNN` string is written into runtime / prompt / judge
   code. The Track A CaseSpec YAMLs cite case ids in their own
   `case_id` fields and in the family manifest — that is the
   CaseSpec contract surface, not the runtime.
5. **Semantic ownership moved from LLM to Java?** No. Constitution
   §1.3 LLM-owns scope is unchanged. The slot adds an observable
   field to the projection; the LLM still owns the next-action
   decision.
6. **Prompt grew an if-else block?** No prompt edit. The slot is
   added to the projection JSON; whether and how the system prompt
   iterates over it (to consume the slot) is a separate prompt sprint
   and out of Sprint 20 scope per the objective §"Defer".
7. **Tool schema, capability / permission, PII / safety floor,
   grounding floor preserved?** Yes. No tool schema edit. No
   permission boundary change. The slot carries an arguments **hash**
   (not the arguments themselves), so PII surface area is reduced
   relative to the existing `accumulated_tool_results` slot. The
   grounding-floor diagnostics
   (`docs/current/faq_grounding_contract.md`) are not touched.
8. **Generalization eval coverage (target / neighbor / negative /
   shadow)?** Track A IS the coverage deliverable for downstream
   sprints — see §9 below. Track B's coverage: target = the
   manual-probe trace shape; neighbor = any multi-tool-call turn from
   the smoke suite; negative = the
   `alreadyCalled_excludesUnsuccessfulEvents` and
   `alreadyCalled_emptyArrayOnFirstStep` tests; shadow = deferred (no
   shadow case has been built for the `already_called` slot
   specifically — the slot is observability-only and the Track A
   shadow set was built around the brief-named failure shapes, not
   around the slot itself).
9. **Temporary change → sunset plan?** Not temporary. The slot is the
   long-term `prompt_projection` Layer 1 from Sprint 19 §4.2. A
   conditional follow-on (`R-idempotent-read-tool-short-circuit`)
   would consume the same evidence base if the soft-signal-only
   approach proves insufficient; that R-item is out of Sprint 20
   scope per the objective §"Defer".

**Sprint 20 PR-level verdict (per §4.1):** **`approve`** — Track A is
content-authoring (no semantic hardcode); Track B is the canonical
`prompt_projection` bundle (soft signal, no enforcement, regression
test, no prompt edit).

## 9. Generalization-coverage table

Track A IS the §5.1 generalization-coverage artefact for the 10 G1
briefs. Counts below.

| family_id | target (count) | neighbor (count) | negative (count) | shadow (count) | total |
| --- | --- | --- | --- | --- | --- |
| cs015_uc_fp_mis_route | 1 (ref) | 2 | 2 | 2 | 7 |
| cs001_uc_c_template_escalate | 1 (ref) | 2 | 2 | 2 | 7 |
| cs011_uc_d_description_ignored | 1 (ref) | 2 | 2 | 2 | 7 |
| cs038_uc_j_intake_redundancy | 1 (ref) | 2 | 2 | 2 | 7 |
| cs040_uc_k_disengaged_jargon | 1 (ref) | 2 | 2 | 2 | 7 |
| cs095_uc_classification_account_aware | 1 (ref) | 2 | 2 | 2 | 7 |
| cs176_uc_e_wrong_escalation_reason | 1 (ref) | 2 | 2 | 2 | 7 |
| cs192_uc_b_giveaway | 1 (ref) | 2 | 2 | 2 | 7 |
| cs259_uc_f_payment_question | 1 (ref) | 2 | 2 | 2 | 7 |
| manual_probe_uc_a_resolve_must | 1 (authored) | 2 | 2 | 2 | 7 |
| **total** | **10** | **20** | **20** | **20** | **70** |

10 families × (≥1 target + ≥2 neighbor + ≥2 negative + ≥2 shadow) = 70
CaseSpec-shaped entries. The objective bar of "≥70" is met exactly.

**Shadow-split enforcement story** (per §4):

- Directory boundary: shadow CaseSpecs live in
  `eval_interactive/case_specs_shadow/`, sibling of `case_specs/`. The
  existing `CaseSetManager` is rooted at `case_specs/` and cannot
  reach the shadow tree via any `--set` flag.
- Runner-flag gate: shadow CaseSpecs load only via explicit
  `--path eval_interactive/case_specs_shadow/...` invocation. A
  documented `--include-shadow` flag is a v1 hardening direction; the
  v0 mechanism does not require it because the directory boundary +
  custom-path-only loading is sufficient.
- Documented self-restraint: `_ACCESS_BOUNDARY.md` documents that the
  dev agent must not read shadow CaseSpecs during development. Sprint
  20 IS the sprint that authored the shadow class; Sprint 20 did NOT
  consume it during authoring (every shadow file was *written* but
  not loaded into the dev agent's evaluation surface).
- Access boundary: human + review agent may read both classes; dev
  agent reads only visible class; harness default behaviour respects
  the boundary.

## 10. Sprint-objective-met check

Walking every "Do not implement" bullet and every "Success metrics"
bar in `docs/sprint_objective.md`.

### "Do not implement" (all ✓)

- No remediation of any G1 brief's underlying failure. ✓ Track A
  catalogues; no runtime / prompt / judge / corpus change for any
  brief shape.
- No change to `eval_interactive/case_specs/smoke/*`. ✓ All 9 smoke
  targets are referenced via the visible manifest; zero edit.
- No change to `eval_interactive/case_spec_overrides.yaml`. ✓
- No change to `eval_interactive/personas*.yaml`. ✓
- No FAQ corpus edit (`data/faq/*`,
  `FAQ-knowledge_include_help_url.csv`). ✓
- No judge rubric edit. ✓
- No prompt edit beyond Track B's projection slot. ✓ The slot is added
  to the projection JSON; `system_prompt.txt` is unedited; no prompt
  paragraph, no prompt if-else.
- No keyword / regex / if-else / enum / per-UC matrix added for a
  semantic decision. ✓
- No new Tier-0 invariant in
  `docs/runtime_freeze_and_risk_policy.md`. ✓ cs_192 stop-gate did NOT
  fire.
- No short-circuit logic on the `already_called` slot. ✓ The runtime
  passes `toolEvents` to projection assembly; no dispatch-side logic
  reads the slot.
- No work on the 16+ other R-items in `docs/action_bank.md` §5.2 not
  named in this objective. ✓
- No edit to `docs/current/iteration_governance.md`,
  `docs/current/doc_governance.md`,
  `docs/current/agent_context_guide.md`, `docs/foundational/*`,
  `docs/runtime_freeze_and_risk_policy.md`, or any sprint archive
  under `docs/sprints/`. ✓
- No architecture-health metric collection opened. ✓
- No change to `docs/codex-findings.md` by the dev agent. ✓

### "Success metrics" (all ✓ except shadow consumption — deferred by design)

- **Target Track A deliverable.** ✓ 10 case families authored. Each
  has ≥1 target + ≥2 neighbor + ≥2 negative + ≥2 shadow (table §9).
  Shadow-split mechanism delivered with documented enforcement
  (§4 + `_ACCESS_BOUNDARY.md`).
- **Target Track B deliverable.** ✓ `already_called` slot lands in
  `ContextProjectionBuilder.build(...)`. Regression test
  demonstrates the three behaviour bars + canonical-hash invariance
  + legacy-overload shape stability. 7 / 7 passing.
- **Neighbor cases no regression.** ✓ Full server suite re-run
  post-wiring: 894 / 0 / 0 / 1 (1 pre-existing skip). The wiring did
  not regress any of the existing 887 prior tests (894 − 7 new). Smoke
  rerun on the eval_interactive side is **NOT triggered** because the
  Track A authoring does not produce a behavior-changing diff on
  smoke (smoke target files are read-only references via manifest);
  Track B's slot is observability-only and the smoke harness does not
  read it. The Sprint 19 §3 baseline conditions are unchanged.
- **Negative-control no false positive.** ✓ Track B's negatives are
  the test bars `alreadyCalled_excludesUnsuccessfulEvents` (slot does
  not include events that were not dispatched) and
  `alreadyCalled_emptyArrayOnFirstStep` (slot is empty when there are
  no prior calls). Both pass.
- **Shadow no regression.** ✓ trivially — Sprint 20 itself does NOT
  consume the shadow set; it authors it. The first sprint to consume
  shadow is a downstream remediation sprint.
- **Safety floor unchanged.** ✓ No PII / safety / identity-verification
  surface edited. The slot replaces nothing; it adds a hash, not raw
  arguments, so PII risk is **lower** than the existing
  `accumulated_tool_results` slot.
- **Grounding floor unchanged.** ✓ No edit to
  `docs/current/faq_grounding_contract.md`, no edit to FAQ corpus, no
  edit to grounding diagnostics.
- **Wrong-containment rate unchanged or down.** ✓ Trivially (no
  agent-behaviour change shipped — Track B is observability-only and
  no prompt edit consumes the slot).
- **Over-escalation rate unchanged or down.** ✓ Trivially.
- **Architecture-health metrics not regressed.** ✓
  `iteration_governance.md` §6 metrics remain
  `collection_status: not_started`. Sprint 20 does not open
  collection.

## 11. Open questions for human

1. **`--include-shadow` runner flag.** Sprint 20's v0 shadow-split
   relies on directory boundary + custom-path-only loading + documented
   self-restraint. The objective allows "v0 may be lightweight"; an
   explicit runner flag is a documented v1 hardening direction in
   `_ACCESS_BOUNDARY.md`. Should the next eval-governance sprint add
   `--include-shadow` to the CLI, or is the v0 boundary sufficient?
   The dev agent did not add the flag because the objective's "Do not
   implement" list explicitly forbade prompt edits and out-of-scope
   harness work; adding a flag without a clear policy on who uses it
   risks scope drift.

2. **cs_095 classification re-review urgency.** The cs095 family
   manifest records that the approved Wave A2.1 override pins UC-A
   primary, but the brief's human re-review identifies UC-D primary
   (per `R-cs095-uc-classification-l3-rereview`). The Track A
   authoring respects the existing override (target references the
   smoke spec as-is), but every neighbor / negative / shadow in the
   family was authored assuming UC-D primary is the correct
   classification. If the L3 re-review reverses (UC-A confirmed), the
   family's expected fields need a refresh. **Recommendation:** the
   L3 re-review batch (see §12) should run before any cs095-shape
   remediation sprint.

3. **cs_011 cluster C regression interaction.** cs_011's L3-approved
   override (Sprint 4 §E1) pins the 2026-05-05 PASS shape. Sprint
   19's Cluster C analysis annotated the 2026-05-10 regression as
   slow-LLM placeholder + planner failure-to-escalate noise. Track A
   authoring uses the 2026-05-05 shape as the target reference. **Open
   question:** when a future remediation sprint addresses the cs011
   pattern, should the regression noise be addressed first (via the
   `R-slow-llm-placeholder-coalesce` and
   `R-prompt-phase-plan-directive-followship` items from Sprint 19
   §11), or should the prompt_projection remediation proceed in
   parallel? The case family does not encode an answer; the
   remediation order is a sequencing decision.

4. **Hand-authored CaseSpec validation.** The 60 hand-authored
   CaseSpecs (neighbor + negative + shadow across 10 families)
   currently load via `CaseSetManager.load_custom(path)` because they
   are not under any `--set` directory. The dev agent did not author
   a smoke-style harness that runs the family in batch. **Open
   question:** should the next sprint add a `--set case-families` flag
   (or equivalent) so the human / review agent can sample-run a
   family for ground-truth sanity-checking, or is the per-case spec
   validation by inspection sufficient for the authoring deliverable?
   The objective allows the latter ("Authoring success is verified by
   inspection") but the question becomes load-bearing as soon as the
   first remediation sprint wants to consume a family.

5. **Track B prompt consumption.** The `already_called` slot ships in
   the projection JSON. The current `system_prompt.txt` (per the
   sprint objective §"Defer") MUST NOT be edited to consume the slot
   in Sprint 20. **Open question:** is a follow-on prompt sprint
   needed to teach the LLM that the slot exists and what to do with
   it? The objective implies yes ("a separate prompt sprint"); the
   dev agent recorded an action-bank delta in §12 to track the
   follow-on. Without a prompt edit, the slot is observable in the
   projection but the LLM has no documented hint to consult it.

## 12. Action-bank deltas + next recommended action

The dev agent does NOT edit `docs/action_bank.md` in this PR; the
deltas below are recommended for the deliver agent to apply at sprint
close.

### Updated rows

- `R-prompt-projection-already-called-soft-signal`
  (`prompt_projection`) — change disposition from `proposed (Sprint 19
  §11)` to `done — slot landed in
  ContextProjectionBuilder.build(..., List<ToolEvent> priorToolEvents);
  regression test AlreadyCalledProjectionTest passing; see Sprint 20
  handoff §5`.
- `R-g2-interactive-case-family-shadow-split` (eval governance, was
  the Sprint 18 §5.1 G2 obligation) — change disposition from
  `pending — Sprint 19 prerequisite (smoke regression diagnosis) must
  resolve first` to `done — 10 families × (≥1+2+2+2) = 70
  CaseSpec-shaped entries; shadow-split v0 delivered per Sprint 20
  handoff §3 / §4`.

### New R-items proposed

- `R-already-called-prompt-consumption` (`prompt_projection` —
  conditional / prompt sprint). Sprint 20 Track B emits the
  `already_called` slot in the projection JSON but the system_prompt
  does not yet describe the slot's semantics to the LLM. A separate
  prompt sprint should add a short paragraph naming the slot and
  documenting that the LLM owns whether to re-emit a same-args call
  — without escalating to enforcement / short-circuit logic. Scope:
  `server/src/main/resources/system_prompt.txt` (or wherever the
  current prompt template lives). Source: Sprint 20 §11 open
  question #5.
- `R-case-family-runner-set` (`infra` / eval harness). Sprint 20
  authored 60 hand-authored CaseSpecs under
  `eval_interactive/case_specs/case_families/`; they load via
  `--path` only. Adding a `--set case-families` flag (or equivalent)
  would let the human + review agent sample-run a family for
  ground-truth validation. Scope: `eval_interactive/eval_interactive/batch/sets.py`
  + CLI. Source: Sprint 20 §11 open question #4.
- `R-shadow-include-flag-runner-gate` (`infra` / eval harness).
  Sprint 20's v0 shadow-split mechanism documents `--include-shadow`
  as a v1 hardening direction. A follow-on sprint can add the flag if
  the v0 directory boundary proves insufficient (e.g. accidental
  glob-loading by a dev agent in a later sprint). Scope:
  `eval_interactive/eval_interactive/batch/sets.py` +
  `eval_interactive/eval_interactive/cli.py`. Source: Sprint 20 §11
  open question #1.

### Out-of-scope deferrals (Sprint 20 did NOT investigate)

- All R-items in `docs/action_bank.md` §5.2 not named above (Wave A5
  / A6 L3 review batch, FAQ corpus audit per UC, duplicated-greeting
  projection fix, faqMissCount threshold, L3 judge form-context trust
  rubric, slow-LLM placeholder coalesce, UC-K case_id binding,
  per-case trace dump, sprint-narrative reconciliation, phase-plan-
  directive-followship soft signal). All explicit per
  `docs/sprint_objective.md` §"Do not implement".
- G3+ remediation of the 10 G1 brief failure shapes. Sprint 20
  authors the case families that enable future G3+ remediation; it
  does not perform the remediation.

### Next recommended action

**Recommended next sprint: Wave A5 / A6 L3 review batch** — process
the seven per-case L3 R-items
(`R-cs001-escalation-trigger-l3-review`,
`R-cs038-l3-review-intake-efficiency`,
`R-cs040-l3-review-intake-completion-semantics`,
`R-cs095-uc-classification-l3-rereview`,
`R-cs176-escalation-reason-l3-review`,
`R-cs192-secondary-ucs-duplicate-uc-b`, plus the systematic
`R-generator-get-customer-context-policy-mismatch`). This is an
eval-spec-only sprint that disjointly contends with no other surface;
it produces approved L3 overrides that subsequent remediation sprints
(G3+) consume. Cs_095's classification re-review is the highest
impact (Sprint 18 §10 Q2 + Sprint 20 §11 Q2).

**Parallel candidate: `R-slow-llm-placeholder-coalesce`** — the dominant
shape behind the 2026-05-10 smoke Cluster C regression (per Sprint 19
§3.7). Disjoint from L3 review; addresses the `infra` placeholder loop
that Sprint 19 §11 documented but did not bundle.

**Conditional follow-on: prompt sprint for `R-already-called-prompt-
consumption`** — once Track B's slot is observed in real traces (via
the manual-probe trace re-run or the next smoke run), a small prompt
sprint can describe the slot's semantics in `system_prompt.txt` so the
LLM has an explicit hint to consult it. This is the natural Track B
follow-on but is not blocking.
