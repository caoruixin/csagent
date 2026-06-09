# Sprint 22 — dev-agent prompt (phase 2 line 358 reconciliation + R-item closure)

Paste the content below this line into a fresh Claude Code session on
branch `design-v1-without-human-review`. Working tree at session-start
carries deliver-agent-owned files (`docs/sprint_objective.md`,
`compact/sprint-022-*-prompt.md`, possibly
`compact/sprint-deliver-orchestrator.md`); see §"Working tree at
start" below.

---

You are the dev agent on Sprint 22. This is a **narrow docs-only
scope-correction sprint**. No runtime change. No semantic surface
touched. Seven directives, listed below. Execute in order.

## Loader chain

Load in this order before any edit:

1. `AGENTS.md` (repo constitution chain).
2. `docs/current/doc_governance.md` (tier model + decision rules,
   especially "Code ahead of docs" routing and the "Stale references"
   notes-style amendment guidance).
3. `docs/current/agent_context_guide.md` (per-task reading lists;
   this sprint sits at the boundary of "Eval, governance" and
   "Tool schema, tool policy" reading lists).
4. `docs/current/iteration_governance.md` §3 (Fix Layer Classification
   — for placing the follow-on R-item correctly) and §7 (stanza
   criteria — to confirm this sprint is exempt; do NOT write a
   stanza).
5. `docs/sprint_objective.md` (Sprint 22 objective — your contract).
6. `docs/foundational/phase2_domain_realization_spec.md` §2.10
   (read lines ~1085–1125 to confirm §2.10.1 is the cross-UC matrix
   and that line 1098 is the `get_customer_context` row; also read
   the UC-H-01 block around lines ~310–362 to see line 358 in
   context).
7. The three failure briefs you will annotate (see directive 5).

You do NOT need to read Sprint 21 archive in full; the objective
cites the relevant Sprint 21 facts directly.

## Working tree at start

Expect deliver-agent-owned files to be uncommitted in your working
tree at session start. Specifically:

- `docs/sprint_objective.md` carries the Sprint 22 objective (deliver
  agent authored).
- `compact/sprint-022-dev-prompt.md` (this file) and
  `compact/sprint-022-review-prompt.md` carry the planning prompts.
- `compact/sprint-deliver-orchestrator.md` may have been updated
  with Sprint 22 context.
- Possibly `docs/diagnostics/codex-findings.md` deletion from
  Sprint 17 close.

**Do not stage these yourself, but do not be surprised if the human
bundles them at commit time.** They are not your scope. When you
run `git add` for your close commit, stage **only** the files you
authored under "Implement only" in the objective; avoid `git add -A`
/ `git add .`.

## The seven directives

### Directive 1 — Reconcile phase 2 line 358 prose annotation

File: `docs/foundational/phase2_domain_realization_spec.md`.

Locate line 358 inside the UC-H-01 YAML block:

Before (current line 358):
```
  - get_customer_context (限 UC-A/UC-FP/UC-K，对 UC-H 不可，因此仅靠 user_message 提问收集)
```

After (intent-preserving rewrite; α-broader shape — stops enumerating
allowed UCs, cross-references the matrix):
```
  - get_customer_context (UC-H 不可用 — 见 §2.10.1 cross-UC allowlist；UC-H 仅靠 user_message 提问收集标识符)
```

You may refine the wording, but you must preserve all three of: the
UC-H-cannot-call claim, the cross-reference to §2.10.1, and the
"collect via user_message" intent. Do not enumerate any UCs in the
new prose.

Then bump the doc's front-matter `last_reviewed` to `2026-05-14`.
Append a one-line entry to the front-matter `notes` field recording
the reconciliation: `Sprint 22 — reconciled UC-H-01 line 358 prose
against §2.10.1 cross-UC matrix.`

Do NOT edit any other line in phase 2. The UC-H-01 YAML block's
structure must remain byte-identical (only the prose annotation on
line 358 changes).

### Directive 2 — Close `R-generator-get-customer-context-policy-mismatch` as not-an-actual-policy-mismatch

File: `docs/action_bank.md` line 396. The row already carries Sprint
21 `status: done` text. Append a "**Sprint 22 correction:**" addendum
to the existing description cell that states (paraphrased — preserve
intent, refine wording):

> **Sprint 22 correction (2026-05-14):** the policy-mismatch premise
> was based on a misread of phase 2 line 358 (a UC-H-local prose
> annotation inside the UC-H-01 YAML block) as the cross-UC rule.
> The actual cross-UC allowlist at `phase2_domain_realization_spec.md`
> §2.10.1 line 1098 explicitly permits `get_customer_context` for
> UC-C, UC-D, and UC-F (corroborated by
> `customer_service_tool_spec_v0_2.yaml` line 260 and
> `customer_service_tool_spec_v0_3.md` line 60). The CaseSpec
> generator was correct; no policy widening is needed. The
> behavioural question — does the bot actually use the tool when
> account-state matters on FAQ-miss shapes in UC-C / UC-D / UC-F? —
> is moved to `R-uc-cdf-get-customer-context-bot-actual-usage`.

Do not delete the Sprint 21 `status: done` reference; layer the
Sprint 22 correction on top of it.

### Directive 3 — Close `R-phase2-uc-cdf-customer-context-policy-widen` as premise-invalidated

File: `docs/action_bank.md` line 559. This R-item was opened by
Sprint 21 §3.7 as the routed-to product-policy R-item for the
"widening" path. Sprint 22's premise-verification invalidates the
widening premise. Mark the row's description with a `**status: done
— closed as premise-invalidated by Sprint 22**` prefix followed by
a one-paragraph explanation (paraphrased — preserve intent):

> The premise that phase 2 §2.10 line 358 forbade `get_customer_context`
> for UC-C / UC-D / UC-F was based on a misread of UC-H-local prose.
> The normative cross-UC matrix at §2.10.1 line 1098 already permits
> the tool for UC-C, UC-D, and UC-F (corroborated by
> `customer_service_tool_spec_v0_2.yaml` line 260 and
> `customer_service_tool_spec_v0_3.md` line 60). No widening is
> needed. Closed without further action. Any residual behavioural
> question is captured in `R-uc-cdf-get-customer-context-bot-actual-usage`
> (Sprint 22 §5.2 open).

Preserve the existing description (do not delete the Sprint 21
context) — add the closure note as a prefix.

### Directive 4 — Update §6 Closed action index

File: `docs/action_bank.md` §6 (line 576+). Convention check first:
look at how Sprint 21 routed-to R-items are marked. Line 396's row
keeps `status: done` in-table in §5.2 (the §5.2 "Systematic" table)
rather than moving to §6. Apply the same convention to line 559's
row (in §5.2 "Sprint 21 routed-to R-items" — keep it in §5.2 with
the closed-marker prefix; do not move it).

For the §6 index, add a single Sprint 22 row:

```
| Sprint 22 | phase 2 line 358 reconciliation + R-item closure (`get_customer_context` policy not-mismatch + new `R-uc-cdf-get-customer-context-bot-actual-usage`) | closed (docs-only governance) | `docs/sprints/sprint-022-*` |
```

### Directive 5 — Append correction notes to the three briefs

Use the notes-style appended block per `doc_governance.md` "Stale
references" guidance. **Do not rewrite existing brief content.**
Append a `> **Correction (Sprint 22, 2026-05-14):**` block
immediately under the affected anchor.

#### 5a — `docs/diagnostics/failure-briefs/cs001-uc-c-template-escalate-on-faq-miss.md`

Anchor: line 67, bullet 2 ("CaseSpec ↔ phase 2 tool-policy conflict
on `get_customer_context`"). After that bullet text (before bullet
2 ends or as a continuation paragraph immediately under bullet 2,
within the same `2.` block), append:

```markdown
   > **Correction (Sprint 22, 2026-05-14):** the claim that phase 2
   > §2.10 line 358 restricts `get_customer_context` to UC-A / UC-FP
   > / UC-K is based on a misread of UC-H-local prose inside the
   > UC-H-01 YAML block. The normative cross-UC allowlist at
   > §2.10.1 line 1098 explicitly permits the tool for UC-C
   > (corroborated by `customer_service_tool_spec_v0_2.yaml` line
   > 260 and `customer_service_tool_spec_v0_3.md` line 60). The
   > CaseSpec generator was correct; no policy widening is needed.
   > The residual question — does the bot actually use the tool when
   > account-state matters? — is captured in
   > `R-uc-cdf-get-customer-context-bot-actual-usage` (action_bank
   > §5.2). The originally-proposed
   > `R-generator-get-customer-context-policy-mismatch` and its
   > routed-to `R-phase2-uc-cdf-customer-context-policy-widen` are
   > both closed.
```

The indentation must match the list nesting (this bullet is item 2
in an ordered list, so the `>` block continues at the indentation of
bullet 2's body text).

#### 5b — `docs/diagnostics/failure-briefs/cs011-uc-d-detailed-description-ignored-on-faq-miss.md`

Anchor: line 70 paragraph (the "systematic generator-vs-policy
mismatch" paragraph). Append immediately under that paragraph
(before the next paragraph at line 76):

```markdown
> **Correction (Sprint 22, 2026-05-14):** the "systematic
> generator-vs-policy mismatch" hypothesis is invalidated. The
> normative cross-UC allowlist at
> `docs/foundational/phase2_domain_realization_spec.md` §2.10.1
> line 1098 explicitly permits `get_customer_context` for UC-D
> (corroborated by `customer_service_tool_spec_v0_2.yaml` line
> 260 and `customer_service_tool_spec_v0_3.md` line 60). Line 358
> is UC-H-local prose, not the cross-UC rule. The CaseSpec
> generator was correct. The behavioural question — does the bot
> use the tool when account-state matters on FAQ-miss? — is moved
> to `R-uc-cdf-get-customer-context-bot-actual-usage` (action_bank
> §5.2). `R-generator-get-customer-context-policy-mismatch` and
> `R-phase2-uc-cdf-customer-context-policy-widen` are both closed.
```

#### 5c — `docs/diagnostics/failure-briefs/cs259-uc-f-sprint7-i0-violation-on-payment-question.md`

Anchor: line 100, bullet 1 ("`R-generator-get-customer-context-policy-mismatch`
(cs_001 UC-C + cs_011 UC-D + now cs_259 UC-F). Three confirmed
instances across three UCs. The 'systematic generator-vs-policy
mismatch' hypothesis from cs_011 is solid now."). Append immediately
under that bullet (within the same `1.` block):

```markdown
   > **Correction (Sprint 22, 2026-05-14):** the "systematic
   > generator-vs-policy mismatch" hypothesis is invalidated. Phase
   > 2 §2.10.1 line 1098 (the normative cross-UC matrix) explicitly
   > permits `get_customer_context` for UC-F (corroborated by
   > `customer_service_tool_spec_v0_2.yaml` line 260 and
   > `customer_service_tool_spec_v0_3.md` line 60). Line 358 was
   > UC-H-local prose. The CaseSpec generator was correct. The
   > behavioural question is now `R-uc-cdf-get-customer-context-bot-actual-usage`
   > (action_bank §5.2).
   > `R-generator-get-customer-context-policy-mismatch` is closed;
   > `R-phase2-uc-cdf-customer-context-policy-widen` is also closed.
```

Do not edit bullet 1's original text. Do not edit bullets 2 / 3 / 4
/ 5 of the same Related-observation section; they reference other
R-items unaffected by this correction.

### Directive 6 — Open the new follow-on R-item

File: `docs/action_bank.md` §5.2. Insert a new row in the
"Per-case L3 / governance" sub-section (after the existing rows
around line 407–417) OR in a new "Sprint 22 surfaced behavioural
R-items" sub-section — your call; either placement is acceptable
provided the row is in §5.2 and is clearly attributable to Sprint 22.

Row content (refine wording, preserve substance):

```
| R-uc-cdf-get-customer-context-bot-actual-usage | cs_001 (UC-C) + cs_011 (UC-D) + cs_259 (UC-F); discovered via Sprint 22 premise-verification | Layer hint: `prompt_projection` or `semantic_planner` (NOT `product_policy` — the policy already permits per phase 2 §2.10.1 line 1098). The three source briefs documented that on FAQ-miss shapes where account-state would plausibly matter, the bot did not call `get_customer_context` despite the tool being allowed for UC-C / UC-D / UC-F. Open question: does the per-turn projection surface the right signal to consider the tool, or does the semantic planner systematically under-call it? **Disposition: proposed (Sprint 22 close 2026-05-14); deferred for future investigation sprint.** No remediation in Sprint 22. |
```

If you place the row outside the "Per-case L3" sub-section, ensure
the surrounding header structure makes it discoverable.

### Directive 7 — Handoff + 10-handoff refresh

Create `docs/sprints/sprint-022-handoff.md` (NEW). Apply the
12-section handoff doc contract (the same shape Sprints 17 / 18 /
19 / 20 / 21 used), with these adaptations:

- §1 Outcome — one paragraph summarising the close.
- §2 Scope delivered — list the seven directives executed.
- §3 Files touched — one row per file with line-ranges.
- §4 Tests / eval — n/a for docs-only (state explicitly).
- §5 Tier-0 invariants — none introduced; none touched.
- §6 Anti-hardcode self-walk — trivially clean (no semantic
  surface touched); state explicitly with one-line evidence per §4.1
  question 1 / 5 / 7.
- §7 Architecture-health metrics — no change (none collected; not
  applicable).
- §8 R-items closed / opened — list the two closed
  (`R-generator-get-customer-context-policy-mismatch`,
  `R-phase2-uc-cdf-customer-context-policy-widen`) and the one
  opened (`R-uc-cdf-get-customer-context-bot-actual-usage`).
- §9 Open questions surfaced — at minimum the behavioural question
  the new R-item captures; any other surprises encountered.
- §10 Recommended next sprint — your call; reference Sprint 21
  §12.4 / Sprint 19 backlog for context. Likely candidates: the
  new follow-on R-item investigation, OR resumption of the
  Sprint 19 Track B Handover Orchestrator work, OR
  `R-case-spec-overrides-schema-scoring-extension`.
- §11 Risks / known gaps.
- §12 Sprint review verdict — set after Codex returns (leave
  placeholder; human / deliver agent fills on close).

Then refresh `docs/10-handoff.md` §1 to lead with Sprint 22's close
narrative (one paragraph). Preserve all other sections; do not
delete Sprint 21 narrative.

## Commit at end

Single close commit suggested message:

```
docs: close sprint 22 — phase 2 line 358 reconciliation + R-item closure
```

Stage only the files in your "Implement only" list:
- `docs/foundational/phase2_domain_realization_spec.md`
- `docs/diagnostics/failure-briefs/cs001-uc-c-template-escalate-on-faq-miss.md`
- `docs/diagnostics/failure-briefs/cs011-uc-d-detailed-description-ignored-on-faq-miss.md`
- `docs/diagnostics/failure-briefs/cs259-uc-f-sprint7-i0-violation-on-payment-question.md`
- `docs/action_bank.md`
- `docs/sprints/sprint-022-handoff.md`
- `docs/10-handoff.md`

Do NOT stage `compact/`, `docs/sprint_objective.md`, or
`docs/codex-findings.md` from your own commit. The human will
handle the deliver-agent file bundling separately if needed.

## Stop conditions

Stop and surface to the human if any of these fire:

1. You find yourself tempted to touch any file NOT in the
   "Implement only" list.
2. You discover that §2.10.1 line 1098 actually does **not**
   permit UC-C / UC-D / UC-F (the whole sprint premise collapses).
3. You consider widening the policy (i.e. propose adding any UC to
   the §2.10.1 matrix or to either v0.2.yaml / v0.3.md allowlist).
4. You consider rewriting any of the three briefs' Related
   observation content beyond the appended notes block.
5. You consider investigating the follow-on R-item inline.
6. You find yourself wanting to start a sub-agent / Task tool to
   parallelize.

Do not attempt to spawn another agent.
