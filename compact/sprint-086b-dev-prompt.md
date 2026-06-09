# Dev prompt — Sprint 086b / S-Auto-31 (M-Auto-7 S-Y1 Part B) — CS4 readiness: executable CaseSpecs

## 1. Role identity

你是 dev agent for **Sprint 086b / S-Auto-31 (M-Auto-7 S-Y1, Part B)** — the
eval-spec half of the last autoloop launch blocker. Goal: **author the CS4
CaseSpecs the autoloop pilot (S-Y2) will optimize against**, reconciled to real
fixtures, so the pilot can run on an honest baseline. Layer: `eval_spec`.
§7-REQUIRED. **No code / no procedure-text change** (Part A landed the
projection slots; the pilot authors the procedure in S-Y2). **No real-LLM run
in 086b** — schema-compile + tiering only.

## 2. Read order (minimal)

- `AGENTS.md` (auto-loaded).
- This prompt.
- Source material (verified at HEAD `auto-loop-branch`, 2026-06-08):
  - `docs/solutions/2026-06-08-cs4-casespec-drafts-appendix.md` — §2-§6 are the
    5 NEW CaseSpec YAML bodies (copy verbatim except the B2 substitutions);
    §7-§8 are the 2 EXTEND diffs. **The appendix ad_ids are placeholders — apply
    §3.2 below.**
  - `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`
    (EXTEND target — currently `outcome_checks: []` at the end; already uses
    real `AD-2001`) + `eval_interactive/case_specs/bad_cases/wmkb_uc_a_trader_flag_secondary_uc_h.yaml`
    (EXTEND target — `outcome_class: either`, 3 hard_checks, `outcome_checks: []`,
    no `bot_handling_pattern` / `expected_tool_sequence`).
  - `eval_interactive/case_specs/bad_cases/_manifest.md` — the lifecycle ledger
    + tier definitions; you append rows for the 5 NEW cases.
  - `eval_interactive/eval_interactive/specs/schema.py` — CaseSpec schema for the
    compile check.
  - Fixture spot-check (confirm these resolve before authoring):
    `server/src/main/resources/mock/listings/live_ad.json` (`AD-1001`, LIVE),
    `…/mock/listings/carol_listing.json` (`AD-2002`, LIVE, Furniture,
    `carol.blocked@example.com`), `…/mock/listings/alice_removed_prohibited.json`
    (`AD-2001`, REMOVED) + `…/mock/moderation_reviews/alice_prohibited_item.json`
    (`AD-2001`, `PROHIBITED_ITEM`). `AD-9999` intentionally has no fixture.

## 3. Embedded contract

### 3.1 Background

Part A (086a, landed) added two OBSERVABLE projection slots:
`moderation_reason_available` (boolean, in `discover_disambiguation_signals`)
and `candidate_use_cases_named` (`{id,name}` array). The already-live
`customer_context_status` slot emits `missing_ad_id` / `lookup_failed` /
`lookup_skipped` / `loaded`. Part B authors the CaseSpecs that exercise these
slot states so the S-Y2 pilot has a concrete target/anti-误杀 gate. Each NEW
case is designed so its fixture choice makes a specific slot state fire — that
is the "reconciliation against the merged slots" (see §3.2 table).

### 3.2 What to author (Scope) — fixture reconciliation RESOLVED

**Reuse existing `server/src/main/resources/mock/` fixtures — author NO new
server fixture files.** The appendix placeholders `AD-3001` / `AD-3050` /
`AD-3060` / `AD-3070` and `IMAGE_QUALITY` do NOT exist. The eval harness
resolves ad_ids through the same Java `MockGumtreeApiService`, so a
non-resolving ad_id mis-scores. Substitute WHILE copying:

| CaseSpec | placeholder → real fixture | form_context.email | slot state it must produce |
|---|---|---|---|
| `cs_uc_a_no_ad_id_ad_specific` | turn-2 `AD-3001` → **`AD-1001`** (LIVE) | keep `sam.no.ad.id@example.com` | `customer_context_status` `missing_ad_id` (T1) → `loaded` (T2) |
| `cs_uc_a_generic_policy_question` | none (`ad_id: ''`) | keep `jordan.generic@example.com` | `missing_ad_id` (correct — generic) |
| `cs_uc_a_loaded_listing` | `AD-3050` → **`AD-2002`** (LIVE Furniture) | → `carol.blocked@example.com` | `customer_context_status=loaded`; `moderation_reason_available=false` |
| `cs_uc_fp_loaded_moderation` | `AD-3060` → **`AD-2001`** (REMOVED + moderation); `IMAGE_QUALITY` → real `PROHIBITED_ITEM` | → `alice.removed@example.com` | **`moderation_reason_available=TRUE`** + `loaded` |
| `cs_uc_a_lookup_failed` | form `AD-9999` (**KEEP** — non-resolving) + turn-2 `AD-3070` → **`AD-2002`** (LIVE) | keep `casey.lookup.fails@example.com` | `lookup_failed` (T1) → `loaded` (T2) |
| `alice_uc_a_uc_h_misclass` (EXTEND) | `AD-2001` already real — diff is outcome_checks only | unchanged | n/a |
| `wmkb_…` (EXTEND) | `ad_id: ''` already — diff only | unchanged | n/a |

B1. Copy the **5 NEW** CaseSpecs from appendix §2-§6 into
   `eval_interactive/case_specs/bad_cases/` (one file each), applying the table
   substitutions. Apply the **2 EXTEND** diffs from appendix §7-§8 to the two
   existing files.

B2. Substitution rules:
   - Set each pre-loaded case's `form_context.email` to the fixture's
     `seller_email` so the pre-chat auto-trigger loads account + listing
     consistently (auto-trigger fires for `Ad Support` candidate UCs; proven by
     the `alice` case auto-loading moderation on `AD-2001`).
   - `cs_uc_fp_loaded_moderation`: rewrite the `hidden_fact` + `closure_criterion`
     illustrative reason from `IMAGE_QUALITY` to `PROHIBITED_ITEM` (the real
     `AD-2001` moderation reason). The `closure_criterion`'s "whatever the
     moderation_review actually says" wording keeps the rubric valid.
   - `cs_uc_a_loaded_listing`: update the `bot_handling_pattern` /
     `closure_criterion` examples from "status=ACTIVE, category=Furniture" to
     `AD-2002`'s real values (status=LIVE, category=Home & Garden > Furniture).
   - Do NOT alter `alice`'s real `AD-2001` fixture.

B3. Schema compile — **dry schema-load / lint only, NO real-LLM run**. Verify
   each authored CaseSpec parses against
   `eval_interactive/eval_interactive/specs/schema.py` (Path γ outcome_checks:
   `correct_uc` / `correct_outcome` / `tool_sequence_match` [+ `turn_efficiency`
   on `cs_uc_a_generic_policy_question`]; expected-level `forbidden_tools` +
   `answer_must_not_contain`; `closure_criterion` / `bot_handling_pattern`).
   Confirm every `expected_tool_sequence` tool name exists in `tool-policy.yaml`.

B4. `_manifest.md` ledger rows for the 5 NEW cases with tiers: Tier-1 target
   (`cs_uc_a_no_ad_id_ad_specific`, `cs_uc_a_loaded_listing`,
   `cs_uc_a_lookup_failed`), Tier-1 anti-误杀 negative-control
   (`cs_uc_a_generic_policy_question`), Tier-2 neighbor
   (`cs_uc_fp_loaded_moderation`). The 2 EXTEND cases keep their existing rows
   (note the strengthened outcome_checks). The human gates the final tiering.

### 3.3 Hard fences / STOP

- Author NO new server fixture files; reuse the table's existing fixtures only.
- Do NOT widen any CaseSpec to accept current bot behaviour (§5.4). CaseSpec
  `outcome_checks` pin trace shape (`correct_uc` / `correct_outcome` /
  tool-sequence), NOT `user_message` keywords.
- Every target case has its paired anti-误杀 negative-control
  (`cs_uc_a_generic_policy_question`); do not weaken it.
- No code change, no skill-procedure-text change, no schema/framework extension
  (Path γ within the current schema).
- **STOP + surface an OQ** if any reconciled ad_id does not resolve as the table
  claims (spot-check the fixture files in §2). Do not author a CaseSpec on a
  non-resolving ad_id.
- **No real-LLM run in 086b.** The targets-fail / control-passes evidence is the
  pre-pilot re-bless (pilot Part C.1) after S-Y1 closes.

### 3.4 §7 stanza (embedded)

**Target failure layer:** `eval_spec` (Part B CaseSpec authoring).
**Tier-0 invariant:** none added. Part B is eval-spec authoring per §5.6.
**Semantic hardcode:** none. CaseSpec `outcome_checks` pin trace shape, not
message keywords; no keyword/regex/enum; no procedure-text change.
**Generalization coverage:** target/neighbor/negative/shadow = 3 / 1 / 1 / ≥2 —
Tier-1 targets (no_ad_id / loaded_listing / lookup_failed); Tier-2 neighbor
(uc_fp_loaded_moderation); anti-误杀 negative-control (generic_policy_question);
shadow = held-out UC-A entity-context + generic-UC-A variants.

### 3.5 §4.3 Codex

Per-sub-sprint Codex DEFERRED to the M-Auto-7 milestone-shared close. No §4.3
trigger (eval authoring, no procedure text, no Tier-0, no hard-fence, no
fix-iteration). The §5.6 tiering review is the human gate. Do NOT dispatch Codex.

### 3.6 Handoff + commit

Author `docs/sprints/sprint-086b-handoff.md`: §0 evidence (5 NEW authored + 2
EXTEND applied; per-case placeholder→fixture reconciliation with the slot state
each produces; schema-compile result; tool-name check; tiering proposal); §11
any OQs (e.g. a placeholder that could not be reconciled); §12 re-bless deferred
to the pre-pilot Part C.1 after S-Y1 close.

**Commit discipline:** stage ONLY
`eval_interactive/case_specs/bad_cases/*.yaml` (5 NEW + 2 EXTEND) +
`eval_interactive/case_specs/bad_cases/_manifest.md` + the handoff. **No new
server fixture files** (reuse path). **No `git add -A`** — unrelated working-tree
files (`compact/aidazi-*`, `compact/framework-plan-v4-*`) must stay out. New
commit, separate from the 086a Part A commit.

## 4. Self-check checklist

- [ ] 5 NEW CaseSpecs authored in `case_specs/bad_cases/`; 2 EXTEND diffs applied.
- [ ] Every placeholder ad_id substituted per the §3.2 table; no `AD-30xx` / `IMAGE_QUALITY` left in any file (except `AD-9999`, kept intentionally).
- [ ] Each pre-loaded case's `form_context.email` set to the fixture seller_email; fixtures spot-checked to resolve.
- [ ] `cs_uc_fp_loaded_moderation` narrative rewritten to `PROHIBITED_ITEM`; `cs_uc_a_loaded_listing` narrative rewritten to LIVE / Home & Garden > Furniture.
- [ ] All CaseSpecs schema-compile (dry load/lint); every `expected_tool_sequence` tool exists in `tool-policy.yaml`.
- [ ] `_manifest.md` rows added with proposed tiers; EXTEND rows note strengthened outcome_checks.
- [ ] No code / procedure-text / fixture-file / schema change; no real-LLM run.
- [ ] Handoff authored; only authorized eval files staged (no `git add -A`).
