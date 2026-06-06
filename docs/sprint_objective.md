---
title: Sprint objective — TBD post M-Auto-6 close (no active sub-sprint; unblock sequence is next)
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: docs/milestone_objective.md (M-Auto-6 acceptance bar) + docs/10-handoff.md §0 (cold-start table) + docs/current/process/preflight-eval-checks.md (the §5.9 pre-flight runbook that resumes post-§0.3-PASS) + this placeholder
last_reviewed: 2026-06-07
review_cadence: replaced when next sub-sprint or milestone is promoted
supersedes: docs/sprints/sprint-083-objective.md
superseded_by: null
notes: >
  PLACEHOLDER — no active sub-sprint contract.

  Milestone M-Auto-6 (Runtime substrate hygiene + admin observability +
  intake/clarification contract + UX/corpus governance) is **ACTIVE
  (dev-side complete; milestone close pending)**. All 6 sub-sprints are
  DEV-SIDE CLOSED as of 2026-06-07:
  - A (S-Auto-23 / Sprint 078; R1.a + R2.a + R4.a) — Codex
    `APPROVE_S_AUTO_23 / blocking_count=0`.
  - B (S-Auto-24 / Sprint 079; R3.a + R3.b + R3.c admin trace
    observability) — visual-verified; §7-EXEMPT.
  - C-1 (S-Auto-25 / Sprint 080; R7 + R2.a#5-ext) — Codex
    `APPROVE_S_AUTO_25 / blocking_count=0`; capability-wiring
    Option-A fence-waiver ACCEPTED.
  - C-2a (S-Auto-26 / Sprint 081; R5 citation contract fix) — Codex
    `APPROVE_S_AUTO_26 / blocking_count=0` on targeted re-review.
  - C-2b (S-Auto-27 / Sprint 082; R6 corpus eligibility filter) —
    Codex `APPROVE_S_AUTO_27 / blocking_count=0` on targeted
    re-review (both prior P0 blockers — F2 second direct-resolve
    test + F3 V17 SQL comment forbidden-grep — resolved by fix
    iteration commits `e6aad78` + `d27b824` + `4c8931f`).
  - **S-Auto-28 (Sprint 083; R8 KnowledgeIngestionRunner `--reconcile`
    data-application path; M-Auto-6 milestone-close BLOCKER fix)** —
    Codex `APPROVE_S_AUTO_28 / blocking_count=0` under §4.1 pure-infra
    scope exemption; F1-F6 all PASS; 3 non-blocking observations
    (NBO #1 standalone entry-gate test gap queued as
    `R-standalone-reconcile-entry-gate-test` for S-Auto-29+ pickup;
    NBO #2 post-APPROVE evidence assertions reminder; NBO #3
    independent review verification 31/0/0/0 + diff-check clean).
    Closes the M-Auto-6 pre-flight blocker brief at
    `docs/diagnostics/failure-briefs/preflight-2026-06-07-kb-ingest-skip-existing-blocks-r6-flag.md`.

  **NEXT ACTION (deliver-agent + human, NOW)**: run the S-Auto-28
  Definition-of-done **unblock sequence** before any further sub-sprint
  is opened.

  ```bash
  # 1. Rebuild + restart backend (detect prior by PORT per the §0.1 drift fix):
  lsof -ti:8080 | xargs -r kill -9
  mvn -o -pl server spring-boot:run -Dspring-boot.run.arguments=--reconcile \
      > /tmp/csagent-reconcile.log 2>&1 &
  # Expect the reconcile INFO summary:
  #   articlesReconciled=2 articlesInsertedNew=0 articlesUnchanged=216

  # 2. Re-run §5.9 §0.3 from docs/current/process/preflight-eval-checks.md:
  psql -d csagent -c "SELECT article_id, search_knowledge_eligible FROM kb_articles WHERE article_id IN ('ka41r000000LIEJAA4','ka41r000000LIEEAA4');"
  psql -d csagent -c "SELECT search_knowledge_eligible, count(*) FROM kb_articles GROUP BY 1;"
  # Expect: both flagged IDs false; count → 2 false / 216 true.
  ```

  **Anti-误杀 #7 forbids using a manual SQL UPDATE as the §0.3 evidence**
  — the DB state MUST be produced by the `--reconcile` run. Per
  `docs/sprints/sprint-083-objective.md` archive + handoff.

  On §0.3 PASS: resume the M-Auto-6 pre-flight from
  `docs/current/process/preflight-eval-checks.md` Step 1 (bad_cases
  smoke at `--n 1 --parallel 1`) → Step 2 (anchor_outcome anti-误杀
  sentinel) → §4 verdict. On GO, the milestone-shared §9 real-LLM
  re-bless launches (deliver-agent + human action).

  **Do NOT promote a next sub-sprint until** either (a) M-Auto-6
  closes via the milestone-shared re-bless, OR (b) a new blocker is
  filed (e.g. smoke surfaces a NO-GO at A1-A11 anomaly per the
  runbook §3, requiring fix-iteration).

  S-Auto-29+ candidates (per docs/milestone_objective.md §3 +
  action_bank.md, deferred to M-Auto-6 close decision):
  - `R-standalone-reconcile-entry-gate-test` (NEW from S-Auto-28 NBO
    #1; infra-test pickup; pin standalone `--reconcile` through
    `ApplicationArguments` to `run()` so a gate regression fails
    before the shared DB step).
  - Cluster B.2 (`primary_uc` vs `active_use_case` authority decision
    — needs research-agent sub-sprint).
  - Cluster C.1 (cs59s session_create 400 — research-first short).
  - `R-aggregate-retains-per-attempt-composite-l2` (from M-Auto-5
    Codex non-blocking observation #3).
  - Autoloop semantic sub-sprint targeting OBS-S1 (UC-A / UC-H /
    UC-J verify-entity-context) / OBS-S2 (DISCOVER disambiguation) /
    OBS-S6 (intake too literal — gated on R7 ship, now landed) /
    OBS-S7 (escalate-without-summary).
  - Post-M-Auto-6 OBS-only backlog from S-Auto-24 close
    (OQ-S79.2 single-session demo endpoint; OQ-S79.3 per-call
    timestamps).
  - S-Auto-25 / S-Auto-26 non-blocking observations queued for
    S-Auto-29+.
  - `R-docs-reconciliation-faq-grounding-contract-vs-must-cite-source`
    (NBO #2 from S-Auto-26 Codex; post-M-Auto-6 docs-only sprint).
  - M-Auto-4 S-Auto-18 (escalation-family tier reshape; deferred
    to M-Auto-7+).

  Decision deferred to the M-Auto-6 close. Do NOT promote a next
  sub-sprint until:
  1. The S-Auto-28 Definition-of-done unblock sequence above
     completes (rebuild + standalone `--reconcile` + §0.3 PASS).
  2. The M-Auto-6 pre-flight Step 1 + Step 2 + §4 verdict return GO.
  3. The milestone-shared §9 real-LLM re-bless launches and
     paired-evidence is reviewed.
  4. The milestone-shared Codex review at
     `compact/M-Auto-6-review-prompt.md` returns a verdict.
  5. The human + deliver-agent classify M-Auto-6 close route per
     `docs/milestone_objective.md` §5 routes (a) prediction holds /
     (b) prediction fails / (c) anti-误杀 violation.

  `baseline_dir` UNCHANGED (`m-auto-5-baseline-20260604-simfixed-stalledfix`)
  and `docs/current_eval_baseline.md` UNCHANGED until the
  milestone-shared re-bless lands at M-Auto-6 close.
---

# Sprint objective — placeholder (no active sub-sprint; S-Auto-28 unblock sequence is next)

See front matter notes for the M-Auto-6 milestone status, the
S-Auto-28 Definition-of-done unblock sequence (rebuild backend +
standalone `--reconcile` + §0.3 re-check; anti-误杀 #7 forbids manual
SQL UPDATE), and the S-Auto-29+ candidate list deferred to M-Auto-6
close.

Active milestone contract: `docs/milestone_objective.md`.
Cold-start state: `docs/10-handoff.md` §0.
Pre-flight runbook (resumes on §0.3 PASS): `docs/current/process/preflight-eval-checks.md`.
