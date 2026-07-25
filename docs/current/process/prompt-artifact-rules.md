---
title: Agent prompt artifact rules (self-containment invariant)
doc_tier: current-runtime
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-07-26
review_cadence: every 3-5 sprints
supersedes: []
superseded_by: null
notes: >
  Extracted from docs/current/iteration_governance.md §9 on 2026-06-02
  as part of the Layer A/B always-loaded split. Original section numbering
  preserved. Cite as "prompt-artifact-rules §9.N".

  Amended 2026-07-26: the two-artefact requirement (a per-sub-sprint
  docs/sprint_objective.md plus a dev prompt embedding it, kept in step by
  a four-rule synchronization cadence) is withdrawn in favour of a single
  contract artefact — see §9.3.1. §9.3 additionally gains three rules that
  were previously unwritten practice: verification artefacts are authored
  after delivery (§9.3.2), a verifier's findings are adjudicated before
  they become work (§9.3.3), and fixes are routed back to the implementing
  session rather than a fresh one (§9.3.4). §9.1, the §9.2 table row for
  the review prompt, and the §9.6 footnote are updated to match.
---

# Agent prompt artifact rules

This process doc receives the agent prompt artifact rules (§9), moved
out of the always-loaded `iteration_governance.md` on 2026-06-02.
Section numbers are preserved against the original file so existing
citations ("prompt-artifact-rules §9.3", "§9.1") continue to resolve.

References to sections that stayed in the always-loaded Layer A read
"iteration_governance §X" (e.g., iteration_governance §4.1) for
unambiguity. References to the milestone schema read
"milestone-framework §8.3".

## 9. Agent prompt artifact rules (2026-05-26 update)

This section codifies the **self-containment invariant** for the
prompt files that the deliver-agent produces for dev and review
agents. The invariant exists so that a fresh dev or review session
can be started by pasting a single prompt file into a new session,
without that session having to read any further repo doc (other
than `AGENTS.md` governance chain, which is auto-loaded).

### 9.1 Invariant

A **prompt artifact** (`compact/sprint-NNN-dev-prompt.md` for dev,
`compact/<review-scope>-review-prompt.md` for review) **is the
contract** — not a view of one held elsewhere. The human approves the
prompt; there is no second document that the prompt must be kept in
step with.

The rationale is in §9.3. In short: an invariant that requires two
artefacts to carry identical content also requires someone to keep
them identical, and that cost is paid unevenly. One artefact cannot
drift from itself.

**Self-contained** means: a fresh dev / review session, given ONLY
this prompt file (plus `AGENTS.md` governance chain, auto-loaded),
has every piece of information it needs to:

- understand its role and the bounded scope of the session;
- execute the contract end-to-end (write code / run tests / author
  handoff for dev; walk iteration_governance §4.1 kernel + verify
  scope discipline + produce `docs/codex-findings.md` for review);
- self-check that the work is complete before claiming so.

The prompt MUST embed (not reference) all contract content. The
single exception is artefacts that the prompt's consumer is
expected to produce (e.g., per-sub-sprint dev handoff is consumed
by review but produced by dev; review prompt references handoff
paths but cannot embed handoff content because it does not yet
exist at prompt-authoring time).

### 9.2 Embed vs reference rules

| Content | Embed in prompt | Reference only |
|---|---|---|
| Role identity, goal, scope, hard fences, test/eval requirements, §7 stanza, handoff requirements, commit discipline | ✓ | |
| §4.1 nine-question kernel (in review prompt) | ✓ | |
| Sub-sprint cumulative scope claim (in review prompt) | ✓ | |
| Governance chain (Constitution, doc_governance, agent_context_guide, iteration_governance) | — | Via AGENTS.md (auto-loaded) |
| Dev handoff + the actual diff (in review prompt) | ✓ | Review prompts are authored AFTER delivery (§9.3), so the delivered artefacts exist and MUST be named concretely — what to verify, and against what claim |
| Code anchors (specific file:line references the agent needs to read or modify) | — | Path reference (the agent reads them on demand during work) |
| Research-agent solutions in `docs/solutions/` | — | Reference if needed; do NOT embed (proposal-tier, may be out of date relative to milestone scope decisions) |

### 9.3 One contract, verified after delivery

**Amended 2026-07-26.** This section previously required a separate
`docs/sprint_objective.md` per sub-sprint, with the dev prompt as its
embedded view and a four-rule two-way synchronization cadence. That
requirement is withdrawn. The rules below replace it.

#### 9.3.1 One artefact carries the contract

The prompt is the contract. The deliver-agent authors it, the human
approves it, the dev session executes it. Scope changes mid-sprint are
made in the prompt, in place.

A live `docs/sprint_objective.md` may still be used when the human
wants a short scoping document to react to before a full prompt is
written — but it is a **drafting convenience, not a second contract**.
Once the prompt exists, the prompt governs, and nothing is required to
be kept in step with it.

At close, the prompt stays at its `compact/` path as the historical
record of what was actually commissioned. It is not re-archived.

#### 9.3.2 Verification artefacts are authored after delivery

A review prompt is written **against what was delivered** — the diff,
the handoff, the test numbers, the specific claims the dev session
made — and never before. Pre-authoring a review prompt forces it to
reference artefacts that do not exist yet, which is exactly the
content a reviewer most needs stated concretely.

This is not a licence to skip review. It relocates it: review is
commissioned at the moment there is something to review, and it is
scoped by what that thing turned out to be.

#### 9.3.3 Findings require adjudication before they become work

**A verifier's findings are claims, not instructions.** They are
routed back to implementation only after someone with authority over
the contract has judged which ones are real.

This step is load-bearing rather than ceremonial. A verifier can be
wrong in the same direction as the specs it is checking — asserting a
defect where the delivered behaviour was correct — and a finding
accepted unexamined will then commission a change that makes correct
behaviour wrong. Adjudication requires looking at the underlying
evidence (traces, transcripts, the running system), not only at the
verifier's report.

Whoever adjudicates must be able to return the verdict *and its
reasoning*, so that a rejected finding is closed with a reason rather
than silently dropped.

#### 9.3.4 Prefer resuming the implementing session over spawning a new one

When adjudicated findings go back for a fix, send them to the session
that did the work, with its context intact, rather than starting a
fresh one. That session already holds why it made each decision, which
is what lets it recognise — and withdraw — its own wrong turn. A fresh
session must re-derive that context and is more likely to defend the
existing implementation than to overturn it.

The self-containment invariant (§9.1) is what makes a *cold* start
possible; it is not an argument for preferring one.

### 9.4 Exemptions

The self-containment invariant is **not required** for:

- The research-agent's `docs/solutions/<name>.md` proposal artefact
  — it is a human-facing proposal, not an agent-execution prompt.
- The deliver-agent's own activation template
  `docs/teams/deliver-activation.md` — it is intentionally minimal
  and points to `docs/teams/deliver-agent.md` for full role definition.
- Cross-session continuity scaffolding in `docs/10-handoff.md` — it
  is structurally a session-handoff log, not an executable prompt.

### 9.5 Backwards compatibility

Pre-2026-05-26 prompts (Sprint 1 through Sprint 53; M1 through M5)
were authored under the older reference-based convention and remain
in their archived form. The self-containment invariant applies
prospectively from the next sub-sprint and the next milestone
review onward.

If a future fold-back pass discovers a historical prompt that
violates this invariant in a way that would meaningfully impair
re-running that session, the deliver-agent SHALL note the issue in
the sprint archive but SHALL NOT retroactively edit the archived
prompt (per `doc_governance.md` "Sprint archives never edited"
rule).

The 2026-07-26 single-contract amendment (§9.3.1) likewise applies
prospectively. Sub-sprints delivered under the two-artefact convention
keep both artefacts exactly as archived — including the paired
`docs/sprints/sprint-NNN-objective.md` files and the "self-contained
executable view of this contract; sync invariant per …" wording they
carry. Those are the accurate record of how that sprint was actually
commissioned. Do not tidy them to match the current rule.

### 9.6 Auto-loop readiness footnote

This section is a prerequisite for the auto-evolution / auto-loop
direction proposed in `docs/solutions/auto_evolution_skill_driven_v1.md`:
a meta-agent driving sub-sprint iterations needs self-contained
prompt artefacts so that each spawned dev / review session is a
deterministic executable unit. The §9.3.1 single-contract rule serves
that requirement more directly than the two-artefact synchronization
cadence it replaced: the prompt is authoritative by construction, so
there is nothing to cross-check at session-spawn time and no window in
which the two copies disagree.

Note that §9.3.3 (adjudication) and §9.3.4 (resume over respawn) are
the parts of this section a meta-agent cannot absorb by itself. An
autonomous loop that routes a verifier's findings straight back into
implementation has no adjudicating party, and that is precisely the
configuration in which a wrong finding commissions a wrong fix.

A separate milestone may later evolve §9 into a richer "session
pack" concept (per the deliver-agent proposal options under
discussion 2026-05-26) — bundling prompt + context snapshots + bad
case fixtures into a single archivable directory. §9 as authored
here is the minimum invariant; the session-pack evolution is
additive on top.
