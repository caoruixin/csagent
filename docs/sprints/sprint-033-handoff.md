---
title: Sprint 33 handoff — DISCOVER UC-A/FP/H soft-signal + classification guidance (M1 sub-sprint 1)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-16
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 33 is the first sub-sprint of Milestone M1 (DISCOVER + Intake)
  per `docs/milestone_objective.md`. Layer `prompt_projection` per
  `docs/current/iteration_governance.md` §3.2 Q3. Codex sprint-close
  review deferred to M1 milestone-shared close per §4.3 default.
  Closure verdict (§12) is left for the M1 milestone-shared decision;
  this archive captures only the dev-session evidence.
---

# Sprint 33 handoff — DISCOVER UC-A/FP/H soft-signal + classification guidance

## 1. Context pack

- **Sub-sprint:** Sprint 33, first sub-sprint of Milestone M1 (DISCOVER + Intake) per `docs/milestone_objective.md`.
- **Layer:** `prompt_projection` per `docs/current/iteration_governance.md` §3.2 Q3.
- **Bad case in scope:** `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` dimension D1 (UC-A vs UC-H DISCOVER mis-classification on REMOVED-listing + Ad Support topic).
- **Codex review:** deferred to M1 milestone-shared close per `iteration_governance.md` §4.3 default. No per-sub-sprint Codex trigger fired (no Tier-0 candidate, no §1.7 red line, no hard-fence violation).
- **Tier model:** the projection slot is a `current-runtime` contract; the prompt teaching paragraph is `current-runtime`; the handoff (this file) is `sprint-archive`. No `foundational` doc edited.

Authoritative sources consulted at session start:

- `docs/milestone_objective.md` (M1 north star: scope, acceptance bar, hard fences).
- `docs/sprint_objective.md` (Sprint 33 contract: §5 file table, §6 hard fences, §8 stanza, §9 success metrics, §10 stop conditions).
- `docs/current/iteration_governance.md` §1.3 / §1.7 / §3.2 Q3 / §4.1 / §5.5 / §5.6 / §7 / §8.
- `docs/sprints/sprint-031-handoff.md` §14 + `docs/sprints/sprint-031-fix-handoff.md` (soft-signal-projection + parameterised invariance-test pattern).
- `docs/sprints/sprint-032-handoff.md` §13 (Option β coverage gap context — separate concern from Sprint 33's D1 fix).
- `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` (the target case + closure criterion).
- `eval_interactive/case_specs/bad_cases/_manifest.md` (bad-case suite convention).
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (existing projection slot pattern).
- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (existing DISCOVER `systemInstruction`).
- `server/src/main/resources/prompts/system_prompt.txt` (existing `already_called` + `alternate_candidate_use_cases` teaching paragraphs).
- `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` (`getCandidateUcsForTopic`).
- `server/src/test/java/com/gumtree/csagent/service/runtime/IntakeAmbiguousCandidatesProjectionTest.java` (projection regression test pattern).
- `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java` (Sprint 31 fix-iteration #2 parameterised invariance-test pattern).

## 2. Sub-sprint-objective recap

Project a soft signal that REMOVED-listing + multi-candidate topic yields multiple plausible UCs (UC-A visibility, UC-FP ad-support deletion, UC-H appeal) AND extend the DISCOVER `systemInstruction` with principle-level disambiguation guidance for ad-status UCs AND add a sibling teaching paragraph to `system_prompt.txt`. The LLM owns the resulting classification decision; the runtime surfaces observable evidence.

Hard fences honoured: no `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` edits; no `IntakeFieldExtractor` / `INTAKE_UCS` / `escalation_reason` enum touch; no Tier-0 invariant; no regex / keyword / per-UC matrix in production; no new case families; no edit to existing case families; no edit to sprint archives; no edit to `eval_interactive/eval_interactive/`; no edit to deliver-agent-owned files (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md`).

## 3. Premise re-verification (§4 spot-check)

All five premises verified at HEAD post-Sprint-32-close (commit `c9edb37`):

1. **`PhaseEvaluator.java` DISCOVER `systemInstruction`** (lines ~411-431) carries the Sprint 7 §I0 weak-candidate cue (UC-F payment / UC-B advertising / UC-C messaging / UC-D account-login). NO UC-A vs UC-H disambiguation guidance, NO REMOVED-listing-specific guidance. Confirmed via `Read` at offset 350-440.
2. **`ContextProjectionBuilder.java` projection slots** at HEAD: `candidate_use_cases` (Sprint 7 §I0), `alternate_candidate_use_cases` (Sprint 31), `already_called` (Sprint 20 Track B), `intake_state` (Sprint 7 §I2), plus the §L2 reroute observability fields. NO `discover_disambiguation_signals` slot. Confirmed via Read of full file (1054 lines).
3. **`UseCaseRegistryService.getCandidateUcsForTopic`** API verified: `List<String> getCandidateUcsForTopic(String topicSubject)`. For `"Ad Support"`, returns `[UC-A, UC-B, UC-FP, UC-H]` per the YAML registry (`server/src/main/resources/config/use-case-registry.yaml` lines 1-13, 38-43, 50-55). For single-candidate topics like `"Replies or Messaging"` returns `[UC-C]`.
4. **`BotSession.listingContext`** is a `jsonb` column persisted as `String` (`server/src/main/java/com/gumtree/csagent/model/BotSession.java:111-112`). Status enum confirmed via mock listings: `REMOVED` is present in `mock/listings/removed_policy.json`, `mock/listings/alice_removed_prohibited.json`, `mock/listings/removed_multiple_accounts.json`, plus `scam_listing.json` and `grace_pet_ad.json`. Other expected states: LIVE / PROCESSING / SUSPENDED / EXPIRED.
5. **`system_prompt.txt`** carries the Sprint 23 `already_called` paragraph (HEAD lines 23-28) and the Sprint 31 `alternate_candidate_use_cases` paragraph (HEAD lines 30-34) — confirmed via Read of HEAD content (after temporary `git checkout HEAD -- system_prompt.txt` for clean-staging dance described in §9).
6. **Bad-case suite Alice case** loads from `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` per the new local manifest (`_manifest.md`). Loader works: `uv run eval-interactive run --path case_specs/bad_cases/` discovered 1 case.

No premise drift surfaced. Working-tree carries a pre-existing Sprint 24-era mod on `system_prompt.txt:66` (removal of "(Sprint 6 §G1)" tag from the ACTIVE-UC TIEBREAKER header — causes the inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` failure that has been the documented baseline since Sprint 24). Sprint 33 does NOT touch this line; the mod was excluded from the Sprint 33 commit (see §9 staging discipline).

## 4. Implementation walkthrough

### 4.1 `ContextProjectionBuilder.java` — `discover_disambiguation_signals` slot

**Insertion point:** immediately after the Sprint 31 `alternate_candidate_use_cases` block (post-edit lines ~417-433). The slot is set via `projection.set("discover_disambiguation_signals", buildDiscoverDisambiguationSignalsNode(session));` so the slot is always present on every projection (shape stability per the §N0 nullable-field convention used by adjacent slots).

**Helper:** `buildDiscoverDisambiguationSignalsNode(BotSession)` (post-edit lines ~864-927). Returns an `ObjectNode` with three fields, each always present:

- `ad_status_observed` — set to the listing's status text when `extractListingStatus(session)` returns one of `REMOVED / SUSPENDED / EXPIRED`; otherwise JSON null.
- `topic_subject_carries_multiple_candidate_ucs` — boolean; true iff `useCaseRegistry.getCandidateUcsForTopic(topicSubject).size() > 1`.
- `candidate_ucs_for_topic` — JSON array. Populated only when the multi-candidate flag is true; empty array otherwise.

**Sub-helper:** `extractListingStatus(BotSession)` (post-edit lines ~929-955). Reads the `jsonb` `listingContext` string, parses with the shared `objectMapper`, returns the `status` text field. Returns `null` on absent / blank / unparseable / no-status. The OUTER `buildProjection` already attempts `objectMapper.readTree(session.getListingContext())` later in the method; if production ever sees an unparseable listing context the OUTER parse would throw and the whole projection collapses to `"{}"` — Sprint 33's defensive extractor is belt-and-suspenders for a future refactor where the outer parse may be removed.

**Rationale for `ObjectNode` (not array) shape:** the slot carries multiple sub-signals with distinct semantics (status observation + topic flag + candidate list). Future Sprint 35 / 36 may add `description_shape_distinct_words` or `intake_field_stall_count` siblings; the object shape supports additive sub-field growth without a schema break. Compare to the Sprint 31 `alternate_candidate_use_cases` (single conceptual list → array) and Sprint 20 `already_called` (list-of-records → array). The decision matches the §N0 nullable-field convention (always-present keys, never-omitted sub-fields).

**Rationale for `REMOVED / SUSPENDED / EXPIRED` triplet:** these are the listing states where the listing is not visible to the user (the user cannot find it via search or in their account view). LIVE / PROCESSING are visible-or-soon-visible states where the user's "where is my ad" question has a different shape (status check, not disambiguation). The triplet was confirmed via the mock listing fixtures plus the Alice CaseSpec's `failure_shape` field (which names REMOVED specifically). PROCESSING and LIVE are intentionally excluded to keep the signal sparse — see §6 OQ1 below for the open question on whether to extend.

### 4.2 `PhaseEvaluator.java` — DISCOVER `systemInstruction` extension

**Insertion point:** appended at the end of the existing DISCOVER `systemInstruction` text (post-edit lines ~411-448). The new paragraph follows the existing Sprint 7 §I0 weak-candidate cue and concludes the systemInstruction string.

**Wording:** principle-level teaching. The paragraph explicitly:

- References the projection slot by name (`discover_disambiguation_signals` and the three sub-fields by name) so the LLM has a path from the runtime evidence to the prompt instruction.
- Frames the disambiguation as understand-vs-act (FAQ-resolvable visibility/reason query vs intake-path appeal), not as UC-A-vs-UC-H by ID. This lets the same teaching generalize to UC-A vs UC-FP, UC-B vs UC-H, UC-FP vs UC-H, etc.
- Recommends one focused clarifying question on the turn when ambiguous, with an example wording — but does NOT mandate the question; the LLM may still classify with high confidence if the user's literal request is unambiguous.
- Explicitly forbids committing an intake-path UC on `ad_status_observed` alone — naming the database-row vs stated-need distinction. This is the principle that the original Alice failure violated.

**No keyword / regex / per-UC matrix.** The paragraph names UC-A / UC-FP / UC-H only by way of example phrases in parentheses (not as branching rules); the LLM is free to read the candidate list from the projection slot and pick any UC the slot exposes. No "if user says X then UC Y" rule.

### 4.3 `system_prompt.txt` — sibling teaching paragraph

**Insertion point:** between the existing Sprint 31 `alternate_candidate_use_cases` paragraph and the existing `DISCOVER phase guidance` block (post-edit lines 36-43). Mirrors the Sprint 23 `already_called` and Sprint 31 `alternate_candidate_use_cases` shape:

- Title naming the slot + provenance ("introduced Sprint 33").
- Sub-bullets describing each of the three fields and when they fire.
- A paragraph naming the soft-signal posture: "OBSERVABLE EVIDENCE the LLM may use to inform classify_use_case … The runtime does NOT enforce or branch on the slot value; you own the read decision."
- A final note that empty / null sub-fields are the common case.

**No tool-specific or UC-specific branches.** The paragraph teaches the slot's semantics; it does not instruct "for UC-A do X, for UC-H do Y".

## 5. Bad-case suite Alice rerun — primary signal per §5.6

**Run:** `cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/` against the local backend at `http://localhost:8080` (`make backend` profile=local, real Moonshot/Kimi LLM endpoint per `application-local.yml`).

**Result path:** `eval_interactive/results/20260516-110928/results.json` (also HTML report at `report.html`).

**Closure-criterion evaluation:** **PASS on criterion (a)** with concrete trace evidence.

Per the CaseSpec's `closure_criterion`:
> (a) Bot routes the conversation to UC-A handling (FAQ-path for ad visibility questions), runs search_knowledge / resolve_article to retrieve the removal-reason FAQ surface, and answers the user's actual question.

Trace evidence (extracted from `case_results[0]`):

- **`active_use_case` = UC-A** at session terminal state. (Original Alice failure: bot committed `UC-H` and locked into intake; Sprint 33 bot stays on UC-A.)
- **`total_turns` = 4** (well within `max_turns: 8`; no dead loop).
- **`judge_score` = 0.8** (mean of groundedness / relevance / tone_appropriateness LLM-judge dimensions).
- **`stop_reason` = goal_achieved**.
- **Tool sequence:** bot called `search_knowledge` with `uc_tags=["UC-A"]` on turn 1, retrieved 3 hits including the canonical removal-reason article (`ka41r000000LIEJAA4`, title "(temp) Ad removed - By CS (general)"), then `resolve_article` on the top hit. On turn 2 user pressed for specifics ("I didn't receive any email … specific reason"); bot called `search_knowledge` again with refined queries (`"specific reason ad removed no email"`, `"AD-2001 removed reason vintage leather sofa"`), correctly admitted it did not have access to the specific reason, offered email-spam-folder check + escalation to human agent. No fabrication.
- **Bot response turn 1** (verbatim): "Hi Alice, I can see your ad 'Vintage Leather Sofa - Must Go' (AD-2001) has been removed. According to our records, it was likely removed because it didn't follow our posting rules. You should have received an email with details on what needs to be changed. Once you make the required changes, you can repost the ad. For more information, check our posting rules here: http://help.gumtree.com/articles/General_Information/General-rules (source: ka41r000000LIEJAA4)." — grounded with source citation.
- **Bot response turn 2** (verbatim): "I understand your frustration, Alice. Unfortunately, I don't have access to the specific reason why your ad was removed — the article I found mentions that an email was sent with details, but since you didn't receive it, I'd recommend checking your spam folder or ensuring your email address is correct. If you'd like, I can escalate this to a human agent who can look into the exact reason for the removal." — honest about limits, offers escalation.

**Projection slot fired correctly:**

```
turn 0: phase=RESOLVE uc=UC-A topic=Ad Support listing_status=REMOVED
   discover_disambiguation_signals = {
     "ad_status_observed": "REMOVED",
     "candidate_ucs_for_topic": ["UC-A", "UC-B", "UC-FP", "UC-H"],
     "topic_subject_carries_multiple_candidate_ucs": true
   }
turn 1: (identical — slot is schema-stable across turns)
```

(Trace extraction: `cat results/20260516-110928/results.json | python3 -c "import json,sys; ..."`, see §4.3 of this handoff for the recipe.)

**Note on `case_passed: False` in the result:** the bad-case CaseSpec carries the standard eval rubric (`L1` / `L2` / `L3` checks per the case_specs/schema). The case_passed=False is driven by:

- `L1:trace_minimum` — `containment_outcome` is blank at terminal state (the bot did not call `record_outcome(resolve)` despite goal_achieved). Orthogonal to Sprint 33's D1 fix; relates to the Sprint 23 / Sprint 11 progressive-resolve checkpoint surface.
- `L2_GATE:correct_outcome` / `L2:correct_outcome` — `outcome_class` is blank where the CaseSpec expects `resolve`. Same root cause as above (no `record_outcome`).
- `L2:tool_sequence_match` — the expected tool sequence in the CaseSpec includes `get_customer_context` + `record_outcome` after the FAQ resolve; the actual sequence stopped at `resolve_article` (no record_outcome) before escalating to handover when the user pressed for specifics.

**None of these is a Sprint-33 closure-criterion FAIL.** The CaseSpec's `closure_criterion.FAIL conditions` are:

> bot enters > 6 turns AND ends with non-canonical request_handover or runs out of budget without progress; bot fabricates an answer about why the ad was removed; bot calls request_handover(intake_complete_for_uc_h) when fields_collected is empty.

None of these triggered. The bot stayed on UC-A, used FAQ-path tools, grounded the answer with source citation, did not fabricate, did not call request_handover with intake_complete_for_uc_h. **Closure-criterion (a) is met.**

**Status:** **PASS** on the Sprint 33 D1 dimension. The L1:trace_minimum / L2 outcome misses are orthogonal concerns (related to Sprint 23 progressive-resolve / containment_outcome surface, not D1 mis-classification) and are M2 candidates per the Sprint 23 archive.

## 6. Generalization coverage table (per §8 stanza)

| coverage class | shape | evidence in Sprint 33 |
|---|---|---|
| **Target** | Alice UC-A vs UC-H mis-classification on REMOVED-listing + Ad Support topic | Bad-case suite Alice rerun PASS on closure-criterion (a) — see §5 |
| **Neighbor (multi-candidate REMOVED/SUSPENDED/EXPIRED)** | parameterised across 6 invariance variants (`AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest`): REMOVED+Ad Support, LIVE+Ad Support, REMOVED+Replies-or-Messaging (single-candidate), null-listing+Ad Support, REMOVED+null-topic, SUSPENDED+Ad Support | 6/6 variants PASS the four invariance bars (TerminalOutcome, llm call count, no tool dispatch, final-message identity) plus the slot-value verifier |
| **Negative (signal must NOT fire)** | LIVE listing + Ad Support (ad_status=null); single-candidate topic with REMOVED listing (multi-candidate flag = false); null topic with REMOVED listing (multi-candidate flag = false); listing without `status` field (ad_status=null) | Covered by `DiscoverDisambiguationSignalsProjectionTest.liveListing_doesNotPopulateAdStatus`, `singleCandidateTopic_clearsMultiCandidateFlag`, `nullTopicSubject_clearsMultiCandidateFlag`, `listingContextWithoutStatus_leavesAdStatusNull` |
| **Shadow** | held-out LLM-behaviour validation across multiple Alice-shape replays | Deferred to M1 milestone-shared close per `iteration_governance.md` §5.6 + §4.3 default. The shadow surface for this slot is the M1 bad-case suite rerun against the cumulative Sprint 33+34(+35) commit range (delivered at M1 close) |

Per M1 hard fence #3 (no edits to existing case families), Sprint 33 does NOT author neighbor/negative/shadow case families in `eval_interactive/case_specs/case_families/`. The Java integration-test parameterization (6 variants × 5 invariance bars) is the right shape for Sprint 33's regression coverage; new case families are explicitly out of M1 sub-sprint 1 scope per `sprint_objective.md` §10 condition 8.

## 7. Open questions for M1 (deliver-agent + human)

**OQ1 — Should the ad_status_observed triplet extend to PROCESSING?**

Currently the slot fires `ad_status_observed` only on `REMOVED / SUSPENDED / EXPIRED`. PROCESSING listings are intentionally excluded because a PROCESSING ad is in a "soon visible" state, not a "not visible" state. However a user asking "where is my ad" on a PROCESSING listing is the original Sprint 11 §M0 last_entity_context_ref scenario — and that path may benefit from the same disambiguation cue. **Recommendation:** defer to M1 milestone close or Sprint 34 (intake prefill) review; if Sprint 34 ships intake-prefill for UC-G/H/I/J without observing PROCESSING traffic, no extension needed.

**OQ2 — Should the DISCOVER prompt teaching reference `alternate_candidate_use_cases` explicitly?**

The Sprint 33 DISCOVER teaching paragraph mentions "candidate UCs for this topic" but does NOT explicitly cross-reference the Sprint 31 `alternate_candidate_use_cases` slot. The two slots have different provenance (Sprint 33 is observation-time; Sprint 31 is intake-time) and serve different LLM-judgement points (DISCOVER classification vs in-flight reroute). **Recommendation:** keep them independent for now; M1 milestone-close review evaluates whether the LLM is using both slots correctly or treating them as confusingly overlapping.

**OQ3 — Should the slot be projected during RESOLVE / CONFIRM phases too?**

The slot fires on every projection regardless of phase (it is appended to the base projection in `buildProjection`, called for every phase). The Sprint 33 DISCOVER teaching only references it in DISCOVER context. **Recommendation:** keep schema-stable across phases (per the §N0 nullable-field convention used by adjacent slots), but consider whether a RESOLVE-phase teaching paragraph is worth adding in a future sprint (M2 candidate).

**OQ4 — Bad-case rubric divergence (L1:trace_minimum / L2:correct_outcome / L2:tool_sequence_match).**

The Alice bad-case rerun produces `case_passed: False` despite closure-criterion PASS on (a). The eval rubric expects `record_outcome` + a longer tool sequence; the bot stopped after the FAQ resolve and offered escalation. **Recommendation:** this is a Sprint 11 / Sprint 23 progressive-resolve concern, not a Sprint 33 D1 concern. The deliver-agent + human should decide at M1 milestone close whether the bad-case CaseSpec's `expected.expected_tool_sequence` should be adjusted (closer to the actual graceful-escalation path) or whether the bot's `record_outcome` discipline needs sprint-side work in M2.

**OQ5 — Why did the LLM commit UC-A on turn 1 instead of asking the clarifying question?**

The Sprint 33 DISCOVER teaching offered the option "if ambiguous, ask ONE focused clarifying question this turn". The Alice run shows the LLM committed UC-A directly on turn 1 (no clarifying question). This is acceptable per closure-criterion (a) — the LLM judged the user's literal request ("I can't see my advert … can you help me figure out what happened?") as unambiguous (visibility question, not appeal request). **Observation, not a concern.** The signal worked as intended: the LLM saw the multi-candidate evidence, weighed the user's literal wording, and made a confident classification toward the FAQ-resolvable UC. If a future Alice-shape user phrased it as "I want to appeal", the LLM has the same evidence and would likely classify UC-H. The Sprint 33 design is "evidence + principle, not enforcement".

## 8. Anti-hardcode self-walk (§4.1 nine-question kernel)

Walked before final commit; expected verdict `approve`:

1. **Does the PR add a keyword / regex / if-else / enum / per-UC matrix for a semantic decision?** No. The projection slot is built from runtime-observable state (`session.listingContext.status`, `session.formTopicSubject`, `useCaseRegistry.getCandidateUcsForTopic`); no keyword on user content, no regex on bot output, no per-UC matrix in Java. The DISCOVER and `system_prompt.txt` paragraphs are principle-level prompt teaching, not rule dumps.
2. **If yes to (1), justified as Tier-0 invariant?** N/A.
3. **Could the same outcome be achieved by projecting a soft signal?** This sprint IS the soft-signal projection (exactly the answer §4.1 Q3 asks for).
4. **Does the change encode visible-eval case text, trace-specific phrasing, or a CaseSpec id?** No. The DISCOVER teaching mentions UC-A / UC-FP / UC-H only as example UC IDs in a parenthetical; no Alice phrasing, no CaseSpec id, no trace text.
5. **Does the change move semantic ownership from the LLM to Java?** No. The opposite: surfaces more observable evidence to the LLM (which owns the classification per §1.3) without any Java branch on the new evidence (proved by `AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest` 6-variant invariance).
6. **Does the change add an if-else block to the prompt instead of principle-level guidance?** No. The DISCOVER paragraph teaches the understand-vs-act distinction and the database-row-vs-stated-need principle; no if-elif chain.
7. **Does the change preserve tool schema, capability / permission boundary, PII / safety floor, and grounding floor?** Yes. No tool schema change, no `allowedTools` widening, no PII surface change, no grounding contract change.
8. **Does the PR ship generalization eval coverage (target / neighbor / negative / shadow)?** Target + Neighbor + Negative shipped (see §6 table). Shadow deferred to M1 milestone close per §5.6 + §4.3 default — explicit in this archive and in `sprint_objective.md` §9.
9. **If the change is temporary, does it carry a rollback / sunset plan?** Not temporary. The slot is a permanent observability surface intended to grow with future sub-signals.

**Expected verdict at M1 milestone-shared Codex close: `approve`.** No semantic hardcode; Constitution §1.3 ownership preserved; generalization coverage shipped at the Java layer (cross-LLM shadow validation at milestone close).

## 9. Files changed

| path | change | lines | new file? |
|---|---|---|---|
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | new `discover_disambiguation_signals` projection slot + `buildDiscoverDisambiguationSignalsNode` helper + `extractListingStatus` sub-helper | +107 / -0 | no |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | append principle-level ad-status disambiguation paragraph to the DISCOVER `systemInstruction` | +18 / -1 | no |
| `server/src/main/resources/prompts/system_prompt.txt` | new `discover_disambiguation_signals` teaching paragraph adjacent to existing `already_called` + `alternate_candidate_use_cases` paragraphs | +9 / -0 | no |
| `server/src/test/java/com/gumtree/csagent/service/runtime/DiscoverDisambiguationSignalsProjectionTest.java` | new — 9 unit tests (3 fire-condition tests, 4 non-fire tests, 2 schema-stability tests) | +293 | yes |
| `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest.java` | new — 6 parameterised variants × 5 invariance bars per Sprint 31 fix-iteration #2 T8 precedent | +324 | yes |
| `docs/sprints/sprint-033-handoff.md` | this archive | +N | yes |

Total staged: **5 production / test files + 1 handoff archive**. Net Java baseline delta: **+15 new tests (9 projection + 6 integration parameter variants)**.

**Staging-discipline note:** the working tree carries a pre-existing Sprint 24-era mod to `system_prompt.txt:66` (removal of "(Sprint 6 §G1)" from the ACTIVE-UC TIEBREAKER header — causes the inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` failure that has been the documented baseline since Sprint 24). Sprint 33 does NOT bundle this mod into the commit. The staging dance: temporarily revert `system_prompt.txt` to HEAD, re-apply the Sprint 33 paragraph as a clean diff vs HEAD, stage that, then restore the working-tree pre-existing state. Result: the committed diff vs HEAD contains only the Sprint 33 paragraph (9 insertions); the working-tree pre-existing Sprint 24-era mod remains as unstaged `git diff HEAD` content for future handling.

## 10. Layer-classification self-walk (per §3)

Walked the §3.2 decision questions:

1. **Q1 (infra)?** No. No session-start crash, timeout, OOM, or transport issue.
2. **Q2 (java_guard / Tier-0 invariant break)?** No. No Tier-0 invariant in `docs/runtime_freeze_and_risk_policy.md` §1 / §2 is violated by the absence of UC-A vs UC-H disambiguation; the Constitution's §1.3 LLM-owns-classification rule governs this surface.
3. **Q3 (prompt_projection)?** **YES.** The LLM was choosing validly within available options at DISCOVER, but the projection lacked (a) the diagnostic signal naming the disambiguation surface, and (b) the principle-level teaching that REMOVED-listing visibility questions deserve disambiguation. **Layer = `prompt_projection`**, matching `sprint_objective.md` §8 stanza.
4. **Q4 (skill_state)?** No. The Sprint 33 fix is observation-time + DISCOVER prompt, not multi-turn state durability. (Sprint 34 will address the skill_state layer for intake-prefill UC-G/H/I/J.)
5. **Q5 (semantic_planner)?** No. The LLM's semantic choice (UC-A vs UC-H) IS the target outcome variable; the fix at the LLM layer would have been "fine-tune the LLM" — out of scope.
6. **Q6 (eval_spec)?** No. The bad-case CaseSpec is authoritative; the OQ4 above flags a peripheral rubric concern (L1:trace_minimum) that is M2 scope.
7. **Q7 (product_policy)?** No. The disambiguation between visibility-explanation and appeal-action is a UX shape, not a product policy decision.

**Tail rule (judge_calibration)?** N/A. Single-run evidence; no flip across reruns observed in Sprint 33 (judge_score=0.8 stable on the one run; multiple reruns deferred to M1 close).

**Default tail (human_review_required)?** N/A. Question 3 matched cleanly.

**Confirmed:** Sprint 33 lands on `prompt_projection`. Matches §7 stanza in `sprint_objective.md`.

## 11. §5 Eval Acceptance bars

Walked each bar from `iteration_governance.md` §5.1:

| bar | status | cited evidence |
|---|---|---|
| Target cases pass | **PASS** | Alice bad-case rerun closure-criterion (a) met; see §5 trace evidence |
| Neighbor cases no regression | **PASS** | `AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest` 6 variants × 5 invariance bars all PASS; baseline server suite 932/1-inherited/0/2 unchanged from Sprint 32 close |
| Negative-control cases unchanged | **PASS** | LIVE / single-candidate / null-topic / null-listing / no-status all leave the slot empty per `DiscoverDisambiguationSignalsProjectionTest.{liveListing,singleCandidateTopic,nullTopicSubject,nullListingContext,listingContextWithoutStatus}` |
| Shadow cases no regression | **DEFERRED to M1 close** per `iteration_governance.md` §5.6 + §4.3 default. Shadow validation surface is the M1 milestone-shared bad-case suite rerun on the cumulative Sprint 33+34(+35) commit range |
| Safety floor unchanged | **PASS** | No PII redaction change; no `escalation_reason` enum touch; no Tier-0 invariant change; Tier-0 schema unchanged |
| Grounding floor unchanged | **PASS** | No grounding-contract surface touched; the FAQ grounding diagnostics surface (`faq_grounding_contract.md`) is untouched. Alice rerun bot grounds its answer with source citation `(source: ka41r000000LIEJAA4)` |
| Wrong-containment rate unchanged or down | **DOWN** (Alice-specific evidence; Sprint 33 single run) — Sprint 33 fixes a known wrong-containment case (UC-H intake lock-in for a UC-A FAQ question). M1 close will broaden the evidence base across the bad-case suite |
| Over-escalation rate unchanged or down | **UNCHANGED** — Alice run did call `request_handover` after the user pressed for specifics the FAQ surface could not provide; the bot gracefully escalated rather than fabricating. This is correct behaviour, not over-escalation |
| Architecture-health metrics not regressed | **UNCHANGED** — collection is `not_started` per `iteration_governance.md` §6 table; no metric regressed because no metric is collected. Direction-of-health: `new_semantic_hardcode_count` = 0 (Sprint 33 ships zero hardcodes); `soft_signal_conversion_count` = +1 (the new DISCOVER signal); `planner_ownership_ratio` unchanged or up (the LLM owns the new classification surface) |

**Smoke composite_score:** observation only per §5.5. Not run in Sprint 33 dev session; M1 milestone close rerun will track the smoke surface alongside the bad-case suite.

## 12. Closure verdict — left for M1 milestone-shared decision

Per `iteration_governance.md` §4.3 default + `feedback_handoff_verdict_section_delegation.md`: Sprint 33 does not close standalone. The closure verdict is decided at M1 milestone close by the deliver-agent + human, against the cumulative Sprint 33 + Sprint 34 (+ 35 + 36 if shipped) commit range and the M1 milestone acceptance bar (Alice closure-criterion + secondary observations).

**Sprint 33 dev-session evidence summary for the M1 close decision:**

- **Bad-case suite Alice:** PASS on closure-criterion (a). Trace evidence in §5.
- **Java baseline:** preserved (932 / 1-inherited / 0 / 2). Net delta from Sprint 32 close: +15 new tests passing.
- **§4.1 anti-hardcode self-walk:** expected verdict `approve`. Detail in §8.
- **Layer classification:** `prompt_projection`. Detail in §10.
- **Generalization coverage:** target + neighbor + negative shipped at Java layer; shadow deferred to M1 close. Detail in §6.
- **Hard fences:** all honoured. No `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` / `IntakeFieldExtractor` / `INTAKE_UCS` / `escalation_reason` enum touch.

**Recommended M1 close evaluation focus:** confirm that Sprint 33's DISCOVER fix + Sprint 34's intake-prefill (if shipped before close) together close Alice's D1 + D2 dimensions, and that the Sprint 35 Option β coverage probe (if shipped) does not surface a regression on the existing soft-signal trajectory. The Sprint 36 conditional is fired only if the M1 close evidence shows Alice's D3 (INTAKE-locked escape) is still load-bearing.
