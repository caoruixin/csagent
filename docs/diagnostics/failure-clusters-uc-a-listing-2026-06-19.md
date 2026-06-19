---
title: "UC-A listing-grounding failure clusters (S-Auto-40 WP1-A Phase 2)"
doc_tier: diagnostic
status: diagnostic
implementation_status: historical
source_of_truth: eval_interactive/results/wp1a-phase2-measurement-20260619 (bounded run draws)
last_reviewed: 2026-06-19
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Three bot-behaviour failure clusters surfaced (not introduced) by the S-Auto-40
  WP1-A Phase-2 bounded measurement run over cs_uc_a_loaded_listing +
  cs_uc_a_no_ad_id_ad_specific. These are MEASUREMENT findings — WP1-A changed no
  bot/runtime/prompt. They are evidence for the held M-Auto-7 entity-context work
  (R-uc-a-entity-context-verify-procedure-via-autoloop) and the auto-adjudication
  follow-up (R-conditional-adjudication-auto-triage), NOT a fix sprint. Per §1.5/§1.7
  these are semantic-planner / prompt_projection failures — do NOT fix by keyword/
  regex/per-UC hardcode.
---

# UC-A listing-grounding failure clusters (S-Auto-40 WP1-A Phase 2)

Source run: `eval_interactive/results/wp1a-phase2-measurement-20260619` (42 draws;
2 PRIMARY ×11 + neighbors/controls ×5; bot=deepseek-v4-flash, sim=moonshot,
temp 0.0; HEAD `b6329041`). These clusters were **observed by the new
`user_state` instrumentation**, not caused by it. All cited draws are already
`case_passed=False` via independent gates (`correct_uc` / `correct_outcome`).

The four `CONDITIONAL_ELIGIBLE` escalate traces (all `cs_uc_a_loaded_listing`)
were human-REJECTED on closure-quality review (registry
`eval_interactive/case_specs/conditional_outcome_adjudications.yaml`); these
clusters record *why*.

## Cluster 1 — UC-A misclassified as UC-B

**Pattern:** an ad-performance / visibility question on a known listing ("my ad
isn't performing, not many views") is classified as UC-B instead of UC-A, so the
listing-grounded resolve path is never entered.

**Evidence (Phase-2 traces):**
- `30878877-52a5-4e54-a351-218b670ebe6e` — active_uc=UC-B, correct_uc=0.0
- `8841784c-3136-4e24-96ca-b0a163b01871` — active_uc=UC-B, correct_uc=0.0
- `a5bcf38e-6e0f-4ece-b178-9d69bba5c4c4` — active_uc=UC-B, correct_uc=0.0
(3 of the 4 CONDITIONAL_ELIGIBLE escalate traces; recurring across the
`loaded_listing` draw set.)

**§3 layer hypothesis:** `semantic_planner` (UC hypothesis) — the LLM picks the
wrong UC even with the listing in context. Possibly `prompt_projection` if the
listing/entity signal is under-surfaced at classify time.

**What NOT to do (§1.5/§1.7):** do not add a keyword/regex map ("views" → UC-A)
or a per-UC hardcode. This is a soft semantic decision the LLM owns.

## Cluster 2 — listing context ignored or used only superficially

**Pattern:** the bot answers with generic visibility FAQ advice (clear photos /
accurate description / fair price / Featured-Top-Ad) without substantively using
the specific listing's fields. Includes the *superficial* sub-case: referencing
the listing title/status once, then reverting to generic advice.

**Evidence (Phase-2 traces):**
- `30878877…`, `8841784c…` — generic visibility tips, no listing-specific field.
- `a5bcf38e…` — answers from a generic "Creating Effective Ads" article;
  "category" mentioned generically, not the listing's actual category.
- `f3a4fff1-937f-4c76-bc12-bc9589fd1995` — **superficial-use exemplar**: correct
  UC-A, references the listing (title "Handmade Wooden Desk" + LIVE status,
  consistent with pre-loaded customer context) but then reverts to generic FAQ
  advice rather than a listing-grounded resolution.

**Provenance note (binding, per the closure criterion):** both **tool-returned**
(`get_customer_context`) **and pre-loaded** `customer_context` are valid entity
provenance — absence of a `get_customer_context` tool call is NOT itself the
failure. The failure is **non-substantive use** of the listing fields.

**§3 layer hypothesis:** `semantic_planner` (chooses generic FAQ over
listing-grounded resolution) + `prompt_projection` (whether the pre-loaded
listing fields are surfaced prominently enough to be used substantively).

**What NOT to do:** do not score on mere title/status string-presence; require
substantive listing-field use. Do not hardcode required phrases.

## Cluster 3 — budget-driven escalation before grounded resolution

**Pattern:** the bot escalates on a budget/miss reason
(`turn_budget_exhausted`, `faq_miss_threshold_exceeded`) before ever delivering a
listing-grounded resolution — i.e. it gives up on budget rather than escalating a
genuinely-unresolved user *after* real help.

**Evidence (Phase-2 traces):**
- `f3a4fff1…` — escalation reason `turn_budget_exhausted` after generic advice.
- `30878877…` — escalation reason `faq_miss_threshold_exceeded` after generic advice.
(Distinct from a *genuine* user-accepted escalation; cf.
`project_faq_overescalate_maxsteps_misstamp` for the reason-mislabel vector.)

**§3 layer hypothesis:** overlaps `R-oq-s93.1-confirm-record-vs-handover`
(CONFIRM record-vs-handover) and the budget-family runtime vector — a
budget-driven escalation pre-empts the grounded resolve path. Layer:
`semantic_planner` / `prompt_projection` for the give-up choice; `infra`/
reason-resolver for the budget-family stamps.

**What NOT to do:** do not relax the turn budget or auto-accept budget
escalations; the fix is to reach grounded resolution earlier, not to widen
acceptance.

## Disposition

These clusters are **held evidence** for the M-Auto-7 entity-context work
(`R-uc-a-entity-context-verify-procedure-via-autoloop`) and inform the
auto-adjudication pre-filter (`R-conditional-adjudication-auto-triage`,
`docs/action_bank.md` §5). They are **not** a WP1-A fix item — WP1-A is the
measurement contract + conditional acceptance only. WP1-B, WP2, objective
annotation, and the M-Auto-7 pilot remain HELD.
