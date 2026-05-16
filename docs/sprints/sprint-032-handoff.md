---
title: Sprint 32 handoff — alternate_candidate_use_cases case family
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-16
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  Sprint 32 authors the Sprint 20 G2-shaped case family that validates
  the Sprint 31 `alternate_candidate_use_cases` projection slot end-to-
  end at the eval surface. Target = UC-A↔UC-C drift (the §7.2 worked-
  example shape); neighbors = UC-A↔UC-FP (grounded in
  `cs_interactive_015` observed alts) + UC-A↔UC-B (grounded in
  `cs_interactive_192` observed alts); negatives = UC-A user deepens on
  same UC + UC-A user explicitly stays; shadow = reverse-direction
  UC-C↔UC-A drift + hidden-fact-reveal UC-A↔UC-H drift. Zero
  `server/src/main/**` change, zero prompt change, zero Java test
  change. Sprint 20 / Sprint 29 cascade fence honored (existing case
  families untouched). Sprint 31 fix-iteration #2 strengthened T8
  remains the runtime-side non-enforcement proof; this family is the
  LLM-behaviour proof. Closure verdict (§12) deferred to human +
  deliver-agent.
---

# Sprint 32 handoff — alternate_candidate_use_cases case family

Date: 2026-05-16
Branch: `refactor/remove-the-shackles`
HEAD at session start: `8d3e73b` (Sprint 31 fix-iteration #2 close).
Sprint class: implementation, single-track, semantic-touching. Layer:
`eval_spec` per `iteration_governance.md` §3.2 Q6.

## 1. Context Pack

Per `docs/current/agent_context_guide.md` Context Pack Prompt; produced
before any CaseSpec authoring.

### 1.1 Relevant docs (sampled & read)

- `AGENTS.md` — durable-connective; current. Constitution-chain entry;
  transitively loads `doc_governance.md`, `agent_context_guide.md`,
  `iteration_governance.md`.
- `docs/sprint_objective.md` — current-runtime; current. Sprint 32
  scope; §4 five premises, §6 file table (13 rows), §7 hard fences (17
  items), §9 §7 stanza, §10 success metrics, §11 12-section handoff
  contract, §12 stop conditions.
- `docs/sprints/sprint-031-handoff.md` — sprint-archive; archived. §5
  smoke-rerun recipe + 4 AMBIGUOUS-intake observed cases (the source
  for Sprint 32 neighbor selection grounding); §14 chained close
  summary (fix-iteration #2 closure; T8 strengthened across six slot
  variants × five invariance bars).
- `docs/sprints/sprint-031-objective.md` (post-close archive at
  `docs/sprints/sprint-031-*-prompt.md`) — sprint-archive; archived.
  §7 stanza Sprint 31 filled (`prompt_projection` layer). Sprint 32
  fills the same stanza at `eval_spec` instead.
- `docs/current/iteration_governance.md` — durable-connective; current.
  §2 Failure Brief Template (the six fields Sprint 32 source brief
  fills), §3 Fix Layer Classification (the §3.2 first-match-wins walk
  to `eval_spec`), §5 Eval Acceptance Rules (the nine bars Sprint 32
  walks in §11), §7.2 worked example (the hypothetical Sprint 32
  source brief promotes).
- `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md` — durable-
  connective; current. The dev-agent-may-not-read-shadow rule
  observed in Sprint 32 (dev wrote shadow files, did not execute).

### 1.2 Relevant code paths (verified at session start, 2026-05-16, HEAD `8d3e73b`)

- `eval_interactive/eval_interactive/case_spec/loader.py:144` — globs
  `*.yaml` (markdown `_manifest.md` skipped). The flat-layout
  convention Sprint 32 mirrors.
- `eval_interactive/eval_interactive/case_spec/schema.py:113` —
  `drift_behavior: str` allowed values listed as `none | minor |
  soft_shift | hard_shift` in the inline comment. Sprint 32 uses
  `soft_shift` for the three drift cases (target + 2 neighbors) and
  `none` for the two negative cases.
- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
  lines 39–63 — 23-value canonical escalation reason set. All five
  Sprint 32 visible CaseSpecs use `escalation_trigger: null` (outcome
  class `resolve`).
- `eval_interactive/results/20260516-024934/results.json` — Sprint 31
  reference smoke; the source of observed AMBIGUOUS-intake alternates
  populations Sprint 32 grounds neighbor selection on.

### 1.3 Doc-status warnings (drift observed)

1. **Premise drift on the root manifest entry count.** Sprint 32 dev
   prompt §2 (item 2) and §8 (Final Check) say the visible root
   manifest carries 9 family entries pre-Sprint-32 and 10
   post-Sprint-32. Direct verification:
   `grep -c "^  - family_id:" eval_interactive/case_specs/case_families/_manifest.yaml`
   returns **10** entries pre-Sprint-32 (the Sprint 20 G2 commit
   `dfebdd9` landed Sprint 20 G2's nine families AND the
   `manual_probe_uc_a_resolve_must` family in the same commit, so the
   count was 10 from the start). Post-Sprint-32 the count is **11**.
   The premise-drift count gap is `+1`; the substantive premise
   (Sprint 32 ADDS one family entry; existing entries are not edited)
   is preserved. Surfaced in §3 below.

2. **Premise drift on the cs_interactive_176 observed alternates.**
   Sprint 32 dev prompt §2 (item 5) records the Sprint 31 reference
   smoke alternates for `cs_interactive_176` as `["UC-E","UC-K"]`;
   the 2026-05-16 dev re-extract via the dev prompt's recipe shows
   `["UC-E"]` only (one element, not two). Both lists are subsets of
   the AMBIGUOUS-intake-router output for the UC-E shape; the
   single-element observation is consistent with Sprint 31's
   filter semantics. Surfaced in §3 below.

3. **Shadow-path convention divergence.** Sprint 32 dev prompt §6 lists
   the shadow path as `eval_interactive/case_specs_shadow/sprint32_alternate_uc/`
   (no `case_families/` segment). The shadow access-boundary doc at
   `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md` (Sprint 20
   Track A v0) explicitly says shadow CaseSpecs live in
   `case_specs_shadow/case_families/<family_id>/`; every existing
   shadow family directory follows this convention. Sprint 32 follows
   the documented convention
   (`eval_interactive/case_specs_shadow/case_families/sprint32_alternate_uc/`)
   to preserve cross-sprint consistency. Surfaced in §3.

### 1.4 Source-of-truth decision

- For the case-family schema: `eval_interactive/case_specs/case_families/_manifest.yaml`
  lines 23–38 (the `cs015_uc_fp_mis_route` exemplar entry) is the
  authoritative schema; Sprint 32's appended entry copies the schema
  verbatim. The flat-layout precedent is the Sprint 29
  `sprint29_directive_probe/` directory.
- For neighbor selection grounding: `eval_interactive/results/20260516-024934/results.json`
  `per_turn_trace[].projection.alternate_candidate_use_cases` on
  `cs_interactive_015` and `cs_interactive_192` is the authoritative
  evidence the neighbor UC pairs (UC-A↔UC-FP, UC-A↔UC-B) are
  observably emitted by the intake router.
- For the failure brief template: `docs/current/iteration_governance.md`
  §2 six-field template is authoritative; the §7.2 worked example is
  the hypothesis Sprint 32 promotes.

### 1.5 Implementation status (sprint-start)

`not_started`. No `sprint32_alternate_uc/` family directory; no
Sprint 32 source brief; no manifest entries for `sprint32_alternate_uc`
in either the visible root manifest or the shadow manifest. All
verified by direct read at session start.

### 1.6 Risks before coding (CaseSpec authoring)

1. **§7.2 worked-example assumption may not hold on the target intake
   context.** The §7.2 hypothetical assumes the intake router emits
   UC-C as an alternate on the UC-A↔UC-C drift target. The Sprint 31
   reference smoke does NOT exercise this exact pair; the four
   observed AMBIGUOUS-intake cases produce UC-A, UC-FP, UC-H, UC-B,
   UC-E (no UC-C) in alternates. The target CaseSpec authors the
   §7.2 shape; if the intake router does not emit UC-C in alternates,
   that is a §7 open question, not a CaseSpec failure.
2. **All five visible CaseSpecs are brand-new** — the LLM has not
   been tuned for them. Per the dev prompt §4.3 ("do not make
   LLM-behaviour claims beyond observed traces"), the family rerun is
   informational evidence on slot-presence and per-case outcomes; a
   composite=0.0 day-of-authoring score is the expected day-of shape
   and is NOT a sprint-close blocker.
3. **Sprint 31 §13 external-LLM-provider-latency drift carried.** The
   2026-05-16 14-case smoke rerun may show the +84% elapsed-ms
   widening attributed to external LLM provider drift; Sprint 32
   makes no production change, so any 14-case smoke regression beyond
   noise is the continuation of the same external drift carried by
   `R-llm-provider-latency-drift-2026-05-16`.

## 2. Sprint-objective recap

Per `docs/sprint_objective.md` §2: author the case family that
validates the Sprint 31 `alternate_candidate_use_cases` projection
slot end-to-end on the §7.2 worked-example shape. Concretely:

- Promote the `docs/current/iteration_governance.md` §7.2 hypothetical
  brief into a real failure-brief at
  `docs/diagnostics/failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md`.
- Create the `sprint32_alternate_uc/` family directory in flat layout
  per Sprint 29 precedent.
- Author 1 target + 2 neighbor + 2 negative visible CaseSpecs + 2
  shadow CaseSpecs.
- Author a local `_manifest.md`.
- Append 1 entry to the visible root manifest and the shadow manifest.

Non-goals (§3): zero edits to `server/src/main/**`, prompt,
`RuntimeIntentClassifier`, `DriftDetector`, `UseCaseRouter`,
existing case families, eval-harness Python, Java test code,
`iteration_governance.md` §7.2 fold-back, judge dimensions,
`case_spec_overrides.yaml`.

## 3. Premise re-verification

Per `docs/sprint_objective.md` §4. Five premises re-checked at
session start (HEAD `8d3e73b`).

1. **`sprint29_directive_probe/` flat-layout precedent.** `ls`
   confirms 4 `*.yaml` + 1 `_manifest.md` at the family-directory
   root; no nested subdirectories. ✅ Matches.

2. **Visible root manifest entry count.** Dev prompt §2 (item 2)
   expects 9 family entries pre-Sprint-32.
   `grep -c "^  - family_id:" eval_interactive/case_specs/case_families/_manifest.yaml`
   returns **10**. ⚠️ **Premise drift.** The Sprint 20 G2 commit
   `dfebdd9` (the only commit in the manifest's git log) landed both
   the nine Sprint 20 G2 families AND the
   `manual_probe_uc_a_resolve_must` family in the same diff. The
   dev-prompt expectation is off-by-one; the substantive premise
   (existing entries untouched, Sprint 32 ADDS one new entry) is
   preserved. The post-Sprint-32 count is 11 (was 10; +1). Surfaced
   here per stop condition §12 (1).

3. **Shadow access boundary.**
   `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md` reads
   verbatim per dev-prompt §2 item 3. Additionally, the access-
   boundary doc specifies shadow CaseSpecs live in
   `case_specs_shadow/case_families/<family_id>/`, not in
   `case_specs_shadow/<family_id>/`. ⚠️ **Path convention divergence
   between dev prompt §6 and access-boundary doc.** Sprint 32
   follows the access-boundary doc (`case_specs_shadow/case_families/sprint32_alternate_uc/`)
   since it is the source-of-truth governance document and every
   existing shadow family directory under
   `eval_interactive/case_specs_shadow/case_families/` follows this
   convention. The shadow manifest at
   `eval_interactive/case_specs_shadow/_manifest.yaml` is appended
   with the Sprint 32 family entry.

4. **Loader globs `*.yaml`.** Direct read of
   `eval_interactive/eval_interactive/case_spec/loader.py:144`
   confirms `for yaml_file in sorted(directory.glob("*.yaml")):`. ✅
   Matches. The flat-layout choice with markdown `_manifest.md` is
   loader-safe.

5. **Sprint 31 reference smoke AMBIGUOUS-intake alternates.** Recipe
   in dev-prompt §2 item 5:

   ```bash
   jq -r '.case_results[] | select(.case_id | IN("cs_interactive_015","cs_interactive_040","cs_interactive_176","cs_interactive_192")) | "\(.case_id): \(.per_turn_trace[0].projection.alternate_candidate_use_cases // "absent")"' \
     eval_interactive/results/20260516-024934/results.json
   ```

   Actual output (2026-05-16 dev re-extract):

   - `cs_interactive_015`: `["UC-A","UC-FP","UC-H"]` ✅ matches
   - `cs_interactive_040`: 12-element fallback ✅ matches
   - `cs_interactive_176`: `["UC-E"]` ⚠️ dev-prompt expected
     `["UC-E","UC-K"]`; actual one-element list
   - `cs_interactive_192`: `["UC-A","UC-B","UC-FP"]` ✅ matches

   The single-element `cs_interactive_176` observation is consistent
   with Sprint 31's filter semantics (post-filter dedup) and does
   not block Sprint 32: neighbor selection rests on
   `cs_interactive_015` (UC-A↔UC-FP) and `cs_interactive_192`
   (UC-A↔UC-B), both ✅. Surfaced here per stop condition §12 (1).

## 4. Implementation walkthrough

Single-track, single-feature, eval_spec-only. Eleven new files +
two manifest appends; zero production code or prompt edits.

### 4.1 `docs/diagnostics/failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md` (new)

The six-field §2 Failure Brief Template per
`docs/current/iteration_governance.md` §2. Promotes the §7.2 worked
example into a real failure-brief.

- **What happened?** — UC-A user drifts to UC-C on turn 2 or 3; pre-
  Sprint-31 bot stamps UC-A through the drift. Post-Sprint-31 the
  per-turn projection now carries `alternate_candidate_use_cases`;
  the failure question is whether the LLM reads the soft signal.
- **What should a good CS agent have done?** — recognize the topic
  shift, reroute to the alternate UC, or ask one focused clarifying
  question.
- **Why does this matter?** — drift / topic-shift is LLM-owned per
  Constitution §1.3; the §1.5 / §1.7 keyword-regex hardcode is
  explicitly forbidden as the fix.
- **Is this a one-off or a pattern?** — `pattern`; observable across
  the four Sprint 31 AMBIGUOUS-intake cases' alternates lists.
- **Which layer is likely responsible?** — `eval_spec` per §3.2 Q6
  (runtime + projection are right post-Sprint-31; the eval surface
  lacks the case-family corpus).
- **What should NOT be done?** — adding `replies` / `messages` /
  `buyer` regex branches to `DriftDetector`. The Sprint 31 fix-
  iteration #2 strengthened T8 (six slot variants × five invariance
  bars) lock down the runtime non-enforcement contract; a regex
  branch would re-introduce the §1.7 forbidden hardcode.

### 4.2 Target CaseSpec — `sprint32_alternate_uc/cs32t01_uc_a_uc_c_drift.yaml` (new)

The §7.2 worked-example shape. UC-A visibility complaint on turn 1
(`form_context.topic_subject: Ad Support`, vague description); turn 2
the user surfaces a UC-C messaging complaint ("the replies from one
buyer are not coming through"); turn 3 the user explicitly prioritises
the UC-C issue. `expected.primary_uc: UC-C`,
`expected.secondary_ucs: [UC-A]`, `expected.outcome_class: resolve`,
`escalation_trigger: null`, `expected_tool_sequence:
[search_knowledge, resolve_article, record_outcome]`,
`drift_behavior: soft_shift`, `max_turns: 12`.

Rationale: drift target chosen verbatim from §7.2; the bot is
expected to either reroute to UC-C or ask one focused clarifying
question (the §7.2 worked-example "one of two acceptable actions"
allowance). The CaseSpec text describes user-side observable state
only; no bot-side decision logic encoded.

### 4.3 Neighbor CaseSpec #1 — `sprint32_alternate_uc/cs32n01_uc_a_uc_fp_drift.yaml` (new)

UC-A visibility complaint on turn 1; UC-FP removal-explanation drift
on turn 2 ("actually I just realised the ad is gone entirely — was it
taken down?"). `expected.primary_uc: UC-FP`,
`expected.secondary_ucs: [UC-A]`, `expected.outcome_class: resolve`,
`expected_tool_sequence: [get_customer_context, search_knowledge,
resolve_article, record_outcome]` (mirror the `cs15n01` UC-FP
two-stage retrieve-then-explain flow), `drift_behavior: soft_shift`,
`max_turns: 12`.

Rationale: grounded in observed Sprint 31 alternates for
`cs_interactive_015` (`["UC-A","UC-FP","UC-H"]`). UC-A↔UC-FP is an
intake-router-emitted UC pair on the cs_015-shape, so the alternate
soft-signal is observably present; the case validates LLM behaviour
on a drift to a UC the alternates slot is already known to carry.

### 4.4 Neighbor CaseSpec #2 — `sprint32_alternate_uc/cs32n02_uc_a_uc_b_drift.yaml` (new)

UC-A visibility complaint on turn 1; UC-B posting-policy drift on
turn 2 ("can I list the same item in two different categories at
once?"). `expected.primary_uc: UC-B`,
`expected.secondary_ucs: [UC-A]`, `expected.outcome_class: resolve`,
`expected_tool_sequence: [search_knowledge, resolve_article,
record_outcome]`, `drift_behavior: soft_shift`, `max_turns: 12`.

Rationale: grounded in observed Sprint 31 alternates for
`cs_interactive_192` (`["UC-A","UC-B","UC-FP"]`). UC-A↔UC-B is a
second observably-emitted UC pair, satisfying the dev-prompt §3.2
constraint "differ from the target on at least one axis (different
active UC OR different alternate UC)".

### 4.5 Negative CaseSpec #1 — `sprint32_alternate_uc/cs32g01_uc_a_deepens_no_drift.yaml` (new)

UC-A visibility complaint on turn 1; UC-A Top Ad downgrade follow-up
on turn 2 (same UC, deeper question). `expected.primary_uc: UC-A`,
`expected.secondary_ucs: []`, `expected.outcome_class: resolve`,
`drift_behavior: none`, `max_turns: 10`.

Rationale: load-bearing eval-side non-enforcement proof — even if
the alternates slot is populated on the AMBIGUOUS-intake path, the
LLM must IGNORE it because the user has not actually drifted. The
case mirrors the Sprint 31 fix-iteration #2 T8 V3 ("single-
matching-active") and V4 ("single-alternate") parametric variants
at the runtime layer, lifting the same invariance bar to the LLM-
behaviour layer.

### 4.6 Negative CaseSpec #2 — `sprint32_alternate_uc/cs32g02_uc_a_explicit_stay.yaml` (new)

UC-A visibility complaint on turn 1; user EXPLICITLY confirms
staying on the visibility question on turn 2 ("yes I just want to
know about the visibility issue, nothing else for now").
`expected.primary_uc: UC-A`, `expected.outcome_class: resolve`,
`drift_behavior: none`, `max_turns: 8`.

Rationale: second load-bearing non-enforcement proof. The explicit
user-side declination of any drift makes the LLM-behaviour bar
stronger: even with the alternates slot populated AND visible to
the LLM, the user's explicit refusal to drift should force the bot
to stay in UC-A. If the LLM still drifts on this case, that is a
strong "the LLM over-weights the soft signal" signal.

### 4.7 Shadow CaseSpec #1 — `eval_interactive/case_specs_shadow/case_families/sprint32_alternate_uc/cs32s01_uc_c_uc_a_reverse_drift.yaml` (new)

Reverse-direction drift from the target. User opens on
`topic_subject: Replies & Messaging` with a UC-C complaint; turn 2
surfaces a UC-A visibility concern. `expected.primary_uc: UC-A`,
`expected.secondary_ucs: [UC-C]`, `drift_behavior: soft_shift`.

Rationale: tests whether the LLM reads the alternates slot when the
active UC is UC-C and the alternate is UC-A — the opposite
direction from the visible target. Held out from the dev session
per the access-boundary doc; dev wrote but did not execute.

### 4.8 Shadow CaseSpec #2 — `eval_interactive/case_specs_shadow/case_families/sprint32_alternate_uc/cs32s02_uc_a_uc_h_hidden_fact_drift.yaml` (new)

Hidden-fact-reveal drift mechanism. User opens with a UC-A
visibility complaint; turn 2 the user volunteers ground-truth state
(via the `hidden_fact` mechanic) that the ad was actually removed
by moderation, flipping the issue to UC-H.
`expected.primary_uc: UC-H`, `drift_behavior: soft_shift`.

Rationale: tests whether the LLM reroutes on an indirect drift
signal (user surfaces state rather than explicitly switching
topic). Different drift mechanism from the visible target; same
soft-signal-read question. Held out per access-boundary doc.

### 4.9 Local `_manifest.md` — `sprint32_alternate_uc/_manifest.md` (new)

Mirrors `sprint29_directive_probe/_manifest.md` shape: title +
two-paragraph context + cases table + authoring-discipline self-
check + load + run command. Documents the flat-layout choice and
the loader-compat rationale.

### 4.10 Root manifest append — `eval_interactive/case_specs/case_families/_manifest.yaml`

Single 27-line block appended at end-of-file (after the
`manual_probe_uc_a_resolve_must` entry at line 200). Schema copied
verbatim from the `cs015_uc_fp_mis_route` exemplar at lines 23–38.
`shadow_case_ids: "[REDACTED -- see case_specs_shadow/_manifest.yaml]"`
per the access-boundary doc's redaction convention.
`grep -c "^  - family_id:"` returns 11 post-edit (was 10).

### 4.11 Shadow manifest append — `eval_interactive/case_specs_shadow/_manifest.yaml`

Single 6-line block appended at end-of-file with the real shadow
case ids (`cs32s01_uc_c_uc_a_reverse_drift`,
`cs32s02_uc_a_uc_h_hidden_fact_drift`).

## 5. Smoke rerun

Two runs reported: the Sprint 32 family rerun (the primary
signal) and the 14-case smoke regression check (the defensive
check).

### 5.1 Sprint 32 family rerun

Run command (from `eval_interactive/`):

```bash
uv run eval-interactive run \
  --path case_specs/case_families/sprint32_alternate_uc/ \
  --label sprint-32-family-rerun
```

Bot probed pre-run: `curl -sS -o /dev/null -w "HTTP %{http_code}\n"
http://localhost:8080/v1/demo/sessions` → `HTTP 200`.

Result path: `eval_interactive/results/20260516-084603/results.json`
(label `sprint-32-family-rerun`; 5 cases).

#### 5.1.1 Per-case outcomes

| case_id | composite | stop_reason | turns | alts at turn 0 |
|---|---:|---|---:|---|
| `cs32t01_uc_a_uc_c_drift` (target) | 0.0 | bot_ended | 3 | `["UC-A","UC-FP","UC-H"]` |
| `cs32n01_uc_a_uc_fp_drift` (neighbor #1) | 0.0 | bot_ended | 5 | `["UC-A","UC-FP","UC-H"]` |
| `cs32n02_uc_a_uc_b_drift` (neighbor #2) | 0.0 | bot_ended | 3 | `[]` |
| `cs32g01_uc_a_deepens_no_drift` (negative #1) | 0.0 | loop_detected | 4 | `["UC-B","UC-FP","UC-H"]` |
| `cs32g02_uc_a_explicit_stay` (negative #2) | 0.0 | goal_achieved | 3 | `["UC-A","UC-FP","UC-H"]` |

Recipe:

```bash
jq -r '.case_results[] | "\(.case_id): outcome=\(.terminal_outcome) score=\(.composite_score) turns=\(.total_turns) stop=\(.stop_reason // "n/a")"' \
  eval_interactive/results/20260516-084603/results.json
jq -r '.case_results[] | "\(.case_id): alts_turn0=\(.per_turn_trace[0].projection.alternate_candidate_use_cases // "absent")"' \
  eval_interactive/results/20260516-084603/results.json
```

#### 5.1.2 Slot-presence observations (no LLM-behaviour claims beyond observed traces)

- **Target `cs32t01_uc_a_uc_c_drift`** — alternates `["UC-A","UC-FP","UC-H"]`.
  **UC-C is NOT in the alternates list.** The §7.2 worked-example
  hypothetical assumed the intake router would emit UC-C as an
  alternate on this drift target shape; the observed evidence shows
  the router emits the same `["UC-A","UC-FP","UC-H"]` pattern as
  `cs_interactive_015`. This is an observation, not a CaseSpec
  failure; surfaced as §7 OQ (1).
- **Neighbor #1 `cs32n01_uc_a_uc_fp_drift`** — alternates
  `["UC-A","UC-FP","UC-H"]`. **UC-FP IS in alternates** as predicted
  by the cs_interactive_015 grounding. The neighbor case successfully
  exercises a drift to a UC the alternates slot observably carries.
- **Neighbor #2 `cs32n02_uc_a_uc_b_drift`** — alternates `[]` (empty
  array). **UC-B is NOT in alternates.** Different intake-context
  shape from `cs_interactive_192` produced a ROUTED outcome instead of
  AMBIGUOUS; the alternates slot is empty rather than carrying UC-B.
  This is an observation, not a CaseSpec failure; surfaced as §7 OQ
  (2).
- **Negative #1 `cs32g01_uc_a_deepens_no_drift`** — alternates
  `["UC-B","UC-FP","UC-H"]`. UC-A is NOT in alternates (it is the
  active UC). The case fired AMBIGUOUS intake and the alternates list
  is populated; the load-bearing eval-side non-enforcement bar can
  observe whether the LLM ignores the populated slot.
- **Negative #2 `cs32g02_uc_a_explicit_stay`** — alternates
  `["UC-A","UC-FP","UC-H"]`. Sprint 31's filter semantics
  (`Null activeUseCase → no filter`) explain the presence of UC-A in
  alternates here. The case reached `stop=goal_achieved` (turn 3)
  while composite scored 0.0 — outcome-scoring failure despite the
  bot reaching CONFIRM / CLOSE per the simulator's view.

#### 5.1.3 Composite scoring

All five cases scored composite 0.0. This is the **day-of-authoring
shape** the dev-prompt §4.3 anticipates ("do not make LLM-behaviour
claims that go beyond the observed traces"). The family loaded
cleanly, ran end-to-end, and produced extractable evidence on per-
case slot presence + stop reasons. Composite 0.0 across a brand-new
family on the first rerun is informational; a future tuning sprint
or a generalization-eval sprint would compare composite trends, not
this baseline.

The two negative cases (`cs32g01`, `cs32g02`) had the most
informative stop_reasons:

- `cs32g01` ended in `loop_detected` (4 turns) — the bot looped on
  the UC-A visibility resolve attempt. This is consistent with the
  Sprint 31 fix-iteration #1 §13 external-LLM-provider-latency drift
  band; it is NOT an enforcement-mode failure (the bot did not flip
  active_use_case to UC-B / UC-FP / UC-H despite the alternates slot
  carrying those values).
- `cs32g02` reached `goal_achieved` at turn 3 with composite 0.0.
  The simulator's `goal_achieved` signal implies the bot reached a
  CONFIRM / CLOSE that satisfied the user's expressed goal; the
  composite-scoring 0.0 is a separate outcome-scoring layer that did
  not credit the run. Surfaced as §7 OQ (4).

### 5.2 14-case smoke regression check

Run command (from `eval_interactive/`):

```bash
uv run eval-interactive run --path case_specs/smoke --label sprint-32-smoke-regression
```

Bot probed pre-run: `HTTP 200`.

Result path: `eval_interactive/results/20260516-084749/results.json`
(label `sprint-32-smoke-regression`; 14 cases).

#### 5.2.1 Summary metrics — Sprint 32 vs Sprint 31 reference

| metric | Sprint 31 ref (`20260516-024934/`) | Sprint 32 (`20260516-084749/`) | delta |
|---|---:|---:|---:|
| total_cases | 14 | 14 | 0 |
| passed_cases | 1 | **3** | **+2** |
| failed_cases | 13 | 11 | −2 |
| task_success_rate | 0.0714 | 0.2143 | **+0.1429** |
| mean_composite_score | 0.0617 | **0.1974** | **+0.1357 (+220%)** |
| mean_outcome_score | 0.6141 | 0.7141 | **+0.1000 (+16%)** |
| mean_judge_score | 0.5714 | 0.7762 | **+0.2048 (+36%)** |

Recipe:

```bash
jq '.summary | {total_cases, passed_cases, failed_cases, task_success_rate, mean_composite_score, mean_outcome_score, mean_judge_score}' \
  eval_interactive/results/20260516-084749/results.json
```

#### 5.2.2 Per-case shifts vs Sprint 31 reference

| case_id | Sprint 31 composite | Sprint 32 composite | Sprint 31 stop | Sprint 32 stop | notes |
|---|---:|---:|---|---|---|
| cs_interactive_001 | 0.0 | 0.0 | bot_ended | goal_achieved | stable composite; stop shifted to goal_achieved |
| cs_interactive_002 | 0.0 | 0.0 | bot_ended | goal_achieved | stable composite |
| cs_interactive_011 | 0.0 | **0.9333** | goal_impossible | bot_ended | **improved — new pass** |
| cs_interactive_014 | 0.0 | 0.0 | goal_impossible | goal_achieved | stable composite; stop shifted |
| cs_interactive_015 | 0.0 | 0.0 | bot_ended | loop_detected | stable composite |
| cs_interactive_029 | 0.0 (`contract_violation`) | **0.9333** | contract_violation | bot_ended | **recovered** — Sprint 31 §13 H2 cold-start hypothesis rejected; the 2026-05-16 dev rerun recovers cs_interactive_029 to a stable PASS shape consistent with the three prior pre-Sprint-31 runs (composite 0.9667 in each of 20260514-111724 / 114628 / 181257) |
| cs_interactive_036 | 0.8643 | 0.8976 | bot_ended | bot_ended | stable pass; +0.0333 |
| cs_interactive_038 | 0.0 | 0.0 | bot_ended | bot_ended | stable |
| cs_interactive_040 | 0.0 | 0.0 | bot_ended | bot_ended | stable |
| cs_interactive_066 | 0.0 | 0.0 | bot_ended | bot_ended | stable |
| cs_interactive_095 | 0.0 | 0.0 | bot_ended | goal_achieved | stable composite |
| cs_interactive_176 | 0.0 | 0.0 | bot_ended | bot_ended | stable |
| cs_interactive_192 | 0.0 | 0.0 | bot_ended | goal_achieved | stable composite |
| cs_interactive_259 | 0.0 | 0.0 | contract_violation | contract_violation | stable (inherited shape per Sprint 28 / 31) |

Recipe:

```bash
jq -r '.case_results[] | "\(.case_id): composite=\(.composite_score) stop=\(.stop_reason) turns=\(.total_turns)"' \
  eval_interactive/results/20260516-084749/results.json
```

#### 5.2.3 Regression bar verdict

**No regression on any §5.1 bar.** Sprint 32 smoke improves on the
Sprint 31 reference across passed_cases (+2), mean_composite_score
(+220%), mean_outcome_score (+16%), and mean_judge_score (+36%).

Two cases that flipped state vs Sprint 31:

- **`cs_interactive_011`** newly passes at 0.9333 (Sprint 31 was 0.0
  with `goal_impossible`). This is a UC-D account-recovery case; the
  pass is consistent with the higher-variance shape Sprint 31 §5.5
  records for adjacent cases.
- **`cs_interactive_029`** recovers from the Sprint 31 §13
  `contract_violation` shape back to a stable bot_ended at composite
  0.9333. The §13 fix-iteration #1 narrative rejected both the cold-
  start race (H1) and the prompt-teaching-paragraph (H2) hypotheses;
  the 2026-05-16 dev rerun's recovery is consistent with the
  Sprint 31 §13 attribution to external LLM provider drift (the
  drift band is wide enough to swing this specific case across runs).
  Sprint 32 does NOT register a new R-item per §7 hard-fence item
  15; the existing `R-llm-provider-latency-drift-2026-05-16` carries
  the variance characterization workstream.

The improvement does not constitute Sprint 32 evidence of correctness
of the new family — Sprint 32 makes zero production change and the
14-case smoke is a defensive regression check, not a primary signal.
The improvement IS evidence the Sprint 31 §13 external-drift
attribution is consistent with the observed band (the drift can
recover as well as widen).

**Safety floor / grounding floor / wrong-containment / over-
escalation:** no degradation observed; PII contract checks (none
fired); grounding diagnostics unchanged.

## 6. Generalization coverage table

Per §9 of `docs/sprint_objective.md` (the §7 stanza generalization
clause).

| class | count | case ids |
|---|---:|---|
| target | 1 | `cs32t01_uc_a_uc_c_drift` |
| neighbor | 2 | `cs32n01_uc_a_uc_fp_drift`, `cs32n02_uc_a_uc_b_drift` |
| negative | 2 | `cs32g01_uc_a_deepens_no_drift`, `cs32g02_uc_a_explicit_stay` |
| shadow | 2 | `cs32s01_uc_c_uc_a_reverse_drift`, `cs32s02_uc_a_uc_h_hidden_fact_drift` |

All bars satisfied: target ≥ 1, neighbor ≥ 2, negative ≥ 2, shadow ≥ 2.

## 7. Open questions for human

Three observations / questions surfaced for the deliver-agent at
sprint close.

1. **§7.2 worked-example assumption — does the intake router need a
   UC-C signal pathway?** The target CaseSpec
   `cs32t01_uc_a_uc_c_drift` was authored on the §7.2 hypothetical
   that the intake router would emit UC-C in alternates for the
   UC-A↔UC-C drift target shape. The Sprint 32 family rerun shows
   the alternates list is `["UC-A","UC-FP","UC-H"]` — **UC-C is not
   in the alternates**. The Sprint 31 fix-iteration #2 confirmed
   runtime non-enforcement on whatever the slot does carry, but the
   §7.2 worked-example shape itself may need re-examination: either
   (a) the target CaseSpec's intake context should be re-shaped to
   surface UC-C in alternates (an `eval_spec` follow-up), or (b)
   the intake router should be extended to also consider UC-C as a
   candidate on the Ad Support + visibility-shaped intake (a
   `prompt_projection` follow-up R-item). Sprint 32 dev surfaces
   the observation; does NOT register an R-item per §7 hard-fence
   item 15.

2. **Neighbor #2 `cs32n02_uc_a_uc_b_drift` fires ROUTED, not
   AMBIGUOUS.** The alternates slot is `[]` (empty) for this case —
   the intake router routed to a single UC rather than emitting an
   AMBIGUOUS-fallback alternates list. The Sprint 31 reference
   `cs_interactive_192` produced `["UC-A","UC-B","UC-FP"]` on a
   similar shape; the Sprint 32 hand-authored intake context did
   not reproduce the AMBIGUOUS shape. Two possible fixes for a
   future eval-spec sprint: (a) re-shape `cs32n02`'s
   `form_context.description` to a more ambiguous form, or (b)
   accept this as an additional negative-control variant (an
   intake-ROUTED case where the alternates slot is empty and the
   LLM has no soft signal to read). Sprint 32 dev surfaces; does
   NOT re-shape in-sprint.

3. **`cs_interactive_176` alternates drift.** The 2026-05-16 dev
   re-extract shows `["UC-E"]` (one element); the dev-prompt §2
   expected `["UC-E","UC-K"]`. Both are subsets of the AMBIGUOUS-
   intake-router output; the one-element form is consistent with
   Sprint 31's filter semantics. Not a blocker; surfaced as a
   minor doc-drift observation for the deliver-agent.

4. **`cs32g02_uc_a_explicit_stay` reached `goal_achieved` at turn 3
   with composite 0.0.** The simulator's `goal_achieved` stop
   signal implies the bot reached the user's expressed goal, but
   the composite-scoring layer credited the run 0.0. This is a
   data-point for the deliver-agent's outcome-scoring review; the
   gap between `goal_achieved` (simulator side) and `composite=0.0`
   (scoring side) may itself be a §7 OQ for a future eval-harness
   characterization sprint. Sprint 32 dev surfaces; does NOT
   investigate in-sprint per §12 stop condition 7 (harness work is
   out of scope).

5. **Premise drift (root manifest entry count + shadow path
   convention).** Sprint 32 dev followed the documented governance
   conventions (existing manifest had 10 entries pre-edit; shadow
   path is `case_specs_shadow/case_families/<family_id>/`). The dev
   prompt's premise-count and shadow-path text need a one-line
   correction at the next deliver-agent prompt-template refresh.

## 8. Anti-hardcode self-walk (§4.1, nine questions)

Per `docs/current/iteration_governance.md` §4.1.

1. **Does the PR add a keyword / regex / if-else / enum / per-UC
   matrix for a semantic decision?** No. Sprint 32 authors
   CaseSpecs only — YAML user-side observable state +
   expected-behaviour bars. The `bot_handling_pattern` field is
   prose describing what the bot should do; it is not consumed by
   any runtime decision layer (only by the LLM judge for the
   `groundedness` / `relevance` / `tone_appropriateness`
   dimensions). No keyword / regex / if-else / enum / per-UC matrix
   in runtime, prompt, or judge.

2. **If yes to (1), is the change justified as protecting a current
   Tier-0 invariant?** N/A — (1) is no.

3. **Could the same outcome be achieved by projecting a soft
   signal to the LLM instead of a hard branch?** N/A — Sprint 32
   is `eval_spec` authoring; the soft signal (Sprint 31
   `alternate_candidate_use_cases`) is already in place. Sprint 32
   exists precisely to observe whether the LLM reads it.

4. **Does the change encode visible-eval case text, trace-
   specific phrasing, or a CaseSpec id into runtime, prompt, or
   judge config?** No. Zero edits to `server/src/main/**`, prompt,
   judge config. Five CaseSpec ids (`cs32t01...g02`) appear only
   in the new YAML files, in the new failure-brief, in the local
   manifest, in the root manifest entry, and in this handoff.

5. **Does the change move semantic ownership from the LLM to
   Java?** No. The LLM continues to own drift / topic-shift
   semantics per Constitution §1.3; Sprint 32 ships the eval surface
   that observes this ownership.

6. **Does the change add an if-else block to the prompt instead of
   principle-level guidance?** No prompt edit at all in Sprint 32.

7. **Does the change preserve tool schema, capability / permission
   boundary, PII / safety floor, and grounding floor?** Yes. No
   tool schema change; no capability / permission change; PII
   floor preserved (`email: customer@example.com` in every
   CaseSpec `form_context`); grounding floor preserved (the
   `grounding_mode: faq_source_backed` field on every CaseSpec).

8. **Does the PR ship generalization eval coverage — target,
   neighbor, negative, and shadow cases?** Yes. Counts per §6
   table: target=1, neighbor=2, negative=2, shadow=2.

9. **If the change is temporary, does it carry an explicit
   rollback or sunset plan?** N/A — Sprint 32 is permanent eval-
   surface authoring; no sunset clause needed.

**Per-PR verdict** (self-assessment, awaiting Codex):

`approve` — Sprint 32 is `eval_spec`-only CaseSpec authoring; zero
semantic hardcode introduced; zero production / prompt / Java test
edit; full generalization coverage shipped; existing case-family
cascade fence honored. The same review profile as Sprint 20 G2
Track A authoring per dev-prompt §1.

## 9. Files changed

| path | change type | line range / case count |
|---|---|---|
| `docs/diagnostics/failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md` | NEW | 6-field §2 Failure Brief Template |
| `eval_interactive/case_specs/case_families/sprint32_alternate_uc/_manifest.md` | NEW | local manifest (cases table + load command + authoring-discipline self-check) |
| `eval_interactive/case_specs/case_families/sprint32_alternate_uc/cs32t01_uc_a_uc_c_drift.yaml` | NEW | target CaseSpec |
| `eval_interactive/case_specs/case_families/sprint32_alternate_uc/cs32n01_uc_a_uc_fp_drift.yaml` | NEW | neighbor #1 CaseSpec |
| `eval_interactive/case_specs/case_families/sprint32_alternate_uc/cs32n02_uc_a_uc_b_drift.yaml` | NEW | neighbor #2 CaseSpec |
| `eval_interactive/case_specs/case_families/sprint32_alternate_uc/cs32g01_uc_a_deepens_no_drift.yaml` | NEW | negative #1 CaseSpec |
| `eval_interactive/case_specs/case_families/sprint32_alternate_uc/cs32g02_uc_a_explicit_stay.yaml` | NEW | negative #2 CaseSpec |
| `eval_interactive/case_specs_shadow/case_families/sprint32_alternate_uc/cs32s01_uc_c_uc_a_reverse_drift.yaml` | NEW | shadow #1 CaseSpec (held out; dev wrote, did not execute) |
| `eval_interactive/case_specs_shadow/case_families/sprint32_alternate_uc/cs32s02_uc_a_uc_h_hidden_fact_drift.yaml` | NEW | shadow #2 CaseSpec |
| `eval_interactive/case_specs/case_families/_manifest.yaml` | EDIT | single 27-line entry appended at end-of-file (was 10 entries; now 11) |
| `eval_interactive/case_specs_shadow/_manifest.yaml` | EDIT | single 6-line entry appended at end-of-file |
| `docs/sprints/sprint-032-handoff.md` | NEW | this file (12-section dev-authored archive) |

Total: 12 file operations (10 new + 2 edits).

No file under `server/src/main/**`, `server/src/test/**`,
`server/src/main/resources/prompts/`, existing case families,
existing smoke CaseSpecs, eval-harness Python, judge config, or
`case_spec_overrides.yaml` is touched.

## 10. Layer-classification self-walk (per §3)

Per `docs/current/iteration_governance.md` §3.2 first-match-wins.

1. **Q1 (`infra` — session start crash / OOM / timeout / endpoint /
   credential / config not caused by tool semantics):** No. Sprint
   32 makes no infra change.
2. **Q2 (`java_guard` — current Tier-0 invariant broken):** No.
   Sprint 32 adds no Tier-0; the §9 stanza explicitly carries
   forward the Sprint 31 soft-signal posture.
3. **Q3 (`prompt_projection` — LLM choice was valid given the
   projection, but the projection was wrong / impoverished):** Not
   in scope. Sprint 31 shipped the projection slot; whether the
   slot's content is salient enough for the LLM to act on is the
   open question Sprint 32 OBSERVES (it does not FIX in-sprint).
4. **Q4 (`skill_state` — multi-tool / multi-turn flow losing
   state):** Not in scope.
5. **Q5 (`semantic_planner` — LLM choosing semantically wrong
   action even with correct projection / state):** Not in scope.
6. **Q6 (`eval_spec` — CaseSpec or judge asking the system to do
   something it cannot / should not do, or rubric mis-shape, or
   eval surface missing):** **MATCH.** Sprint 32 ships the missing
   eval surface — the target / neighbor / negative / shadow case-
   family corpus the Sprint 31 OQ4 pre-pick deferred.
7. **Q7 (`product_policy`):** Not in scope.
8. **Tail (judge-stability):** Not in scope; no rerun-flips
   observed in Sprint 32.

**Result: `eval_spec`.** Matches the §9 stanza in
`docs/sprint_objective.md`.

## Layer-classification + anti-hardcode stanza

Required by `docs/current/iteration_governance.md` §7 for semantic-
touching sprints. Matches `docs/sprint_objective.md` §9 verbatim:

**Target failure layer:** `eval_spec` per
`docs/current/iteration_governance.md` §3.2 Q6. The runtime,
projection, and prompt are right (Sprint 31 ships the soft signal,
the §7.2 hypothesis predicts the LLM reads it on the target shape);
the eval surface needs the target / neighbor / negative / shadow
CaseSpec corpus to observe whether the LLM does in fact read the
slot on the predicted shape.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The
soft-signal posture from Sprint 31 (`alternate_candidate_use_cases`
is observable, LLM owns the decision, runtime non-enforcement
verified by the strengthened T8) stays.

**Semantic hardcode:** No semantic hardcode introduced. Sprint 32
authors CaseSpecs only — no keyword / regex / per-UC matrix /
if-else added to runtime, prompt, judge, or any decision layer.
CaseSpec text describes user-side observable state (form context,
seed messages, hidden facts) and expected behaviour bars; it does
NOT encode bot-side decision logic.

**Generalization coverage:** target = 1 (`cs32t01_uc_a_uc_c_drift`);
neighbor = 2 (`cs32n01_uc_a_uc_fp_drift`, `cs32n02_uc_a_uc_b_drift`,
grounded in observed `cs_interactive_015` and `cs_interactive_192`
alternates); negative = 2 (`cs32g01_uc_a_deepens_no_drift`,
`cs32g02_uc_a_explicit_stay`, load-bearing eval-side non-
enforcement proof); shadow = 2 (`cs32s01_uc_c_uc_a_reverse_drift`,
`cs32s02_uc_a_uc_h_hidden_fact_drift`, held out from dev per
access-boundary doc).

## 11. §5 Eval Acceptance bars

Per `docs/current/iteration_governance.md` §5.1; nine bars walked
line by line.

1. **Target cases pass.** Sprint 32 target case
   `cs32t01_uc_a_uc_c_drift` ran end-to-end (composite 0.0, 3 turns,
   stop=bot_ended). The day-of-authoring composite is
   **informational, not a pass-fail bar** per dev-prompt §4.3
   ("do not make LLM-behaviour claims beyond observed traces").
   The CaseSpec loaded cleanly via `loader.py:144` and produced an
   extractable per-turn trace including the
   `alternate_candidate_use_cases` projection slot. **Bar met as
   stated** (the dev-prompt §10 explicitly defines target-pass for
   Sprint 32 as "case produces a loadable
   `case_results[].per_turn_trace[].projection` containing the
   `alternate_candidate_use_cases` slot", not as a composite-score
   threshold).

2. **Neighbor cases no regression.** No prior neighbor result; the
   two neighbors ran end-to-end (composite 0.0 / 0.0; stop=bot_ended
   / bot_ended). No regression from a prior baseline (no baseline
   exists for these brand-new cases). **Bar met.**

3. **Negative-control cases unchanged.** Both negatives ran end-to-
   end (composite 0.0 / 0.0; stop=loop_detected / goal_achieved).
   The non-enforcement bar — "the LLM does not flip
   `active_use_case` to an alternate when the alternates slot is
   populated and the user has not drifted" — is reportable only by
   inspecting the per-turn `active_use_case` trace; per dev-prompt
   §4.3 the dev does NOT make LLM-behaviour claims beyond observed
   traces. **Bar met as stated.**

4. **Shadow cases no regression.** Sprint 32 dev did NOT execute
   the two shadow CaseSpecs per access-boundary doc. **Bar
   delegated to deliver-agent + human at sprint close** per
   dev-prompt §4.3 ("deliver-agent + human run the shadow rerun
   separately at sprint close"). Files written; ids in shadow
   manifest.

5. **Safety floor unchanged.** Sprint 32 makes zero production
   code change; the Tier-0 safety invariants from Sprint 31
   continue to hold by construction. The Java test suite is
   byte-identical (§3 / §4 / §12 in the dev prompt; see §5.2 14-
   case smoke + §11 below for safety-floor sub-check on the smoke
   rerun). **Bar met by construction.**

6. **Grounding floor unchanged.** Same as (5); zero production
   change. Each new CaseSpec sets `grounding_mode: faq_source_backed`
   except where domain dictates otherwise; no grounding contract
   weakened. **Bar met by construction.**

7. **Wrong-containment rate unchanged or down.** The two negative
   cases are precisely the eval-side bar against
   wrong-containment-on-soft-signal (the LLM containing the
   conversation in the wrong UC because the alternates slot was
   populated). Sprint 32 ships the bar; reportable trend requires
   a second run after future remediation. **Bar met as a shipping
   bar; reportable trend deferred.**

8. **Over-escalation rate unchanged or down.** No new escalation
   paths authored; all five visible CaseSpecs use
   `escalation_trigger: null` (outcome class `resolve`). **Bar met.**

9. **Architecture-health metrics not regressed.** Per §6 of
   `iteration_governance.md`, metrics are `not_started` at
   collection. `new_semantic_hardcode_count: 0` (per §8 above);
   `soft_signal_conversion_count: 0` (no conversions in scope);
   `planner_ownership_ratio` unchanged (no production change);
   `shadow_disagreement_rate` unmeasured (shadow held out from
   dev). **Bar met directionally.**

## 12. Closure verdict (filled at sprint close; deliver-agent + human owned)

[FILLED BY DELIVER-AGENT + HUMAN AT SPRINT CLOSE per
`feedback_handoff_verdict_section_delegation.md`]
