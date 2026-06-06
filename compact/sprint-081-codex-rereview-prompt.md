# Sprint 081 / S-Auto-26 / M-Auto-6 Sub-sprint C-2a — Codex per-sub-sprint **targeted re-review** prompt (clean-tree gate flip; substantive review already PASS)

## Role identity

You are the **Anti-Hardcode + Per-Sub-Sprint Review Agent** for **Sprint
081 / S-Auto-26 / M-Auto-6 Sub-sprint C-2a** (R5 citation contract fix),
performing a **targeted re-review** after the prior pass returned
`REJECT_S_AUTO_26 / out_of_scope_review / blocking_count=1` solely on
the procedural clean-tree gate.

The **substantive review is already complete** and is preserved verbatim
in `docs/codex-findings.md` (the §1 per-change verdicts + §2 nine-question
kernel walkthrough + §3 five focal-point verdicts + §5 non-blocking
observations were all `pass`; 67/0/0/0 focused tests; §4.1 aggregate
verdict `approve`). Your previous §4 blocking finding was:

> **`out_of_scope_review` — working tree was not clean before review.**
> At HEAD `6e55d79`, pre-review and pre-write `git status --short`
> reported `?? compact/context-pack-governance-framework-extraction-2026-06-06.md`.
> ... Corrective direction: place that unrelated file into its
> intended committed or removed state, then rerun the clean-tree check
> and reissue the sprint-close decision; do not change the audited C-2a
> range.

The deliver-agent has resolved the dirty-tree condition by committing
the unrelated context-pack at commit `8a7cb66` (per user direction
2026-06-06; the file is a separate-workstream cold-start handoff for
the governance/collaboration framework extraction project — out of
M-Auto-6 scope; preserved as a committed planning artifact alongside
other `compact/sprint-NNN-*.md` planning context).

Your task: verify the clean-tree gate now passes, confirm the audited
C-2a commit range is unchanged, and reissue the sprint-close verdict
header at the top of `docs/codex-findings.md`.

## What you DO NOT need to redo

- The §1 per-change verdicts on R5 #1 / R5 #2 / R5 #3 / fence-expansion
  #1 / fence-expansion #2 / golden-set churn. (Already PASS; preserved
  in `docs/codex-findings.md`.)
- The §2 nine-question kernel Q1–Q9 walkthrough. (Already PASS with
  anchored evidence; aggregate verdict `approve`.)
- The §3 five focal-point verdicts F1–F5. (Already PASS with anchored
  evidence; F5 confirmed byte-mechanical.)
- The §5 non-blocking observations (NBO #1 audit-gap process learning;
  NBO #2 docs/current/faq_grounding_contract.md historical drift; NBO #3
  67/0/0/0 focused tests). (Already recorded.)
- The focused test re-run. (Already 67/0/0/0; not re-required unless the
  audited range changed.)

## What you DO need to do

### Step 1 — Verify the clean-tree gate now passes

Run `git status --short` at HEAD. Confirm there are zero `M ` /
`A ` / `??` lines (or only files that are intentionally
deliver-managed and not in any committed scope — e.g. `docs/codex-findings.md`
itself if you are mid-edit on the verdict header is acceptable; nothing
else should appear).

Specifically, the previously-blocking untracked file
`compact/context-pack-governance-framework-extraction-2026-06-06.md`
should NO LONGER appear in `git status --short` (it is now tracked at
commit `8a7cb66`).

If the working tree is dirty for any other reason: raise as a NEW
`out_of_scope_review` finding (do not auto-approve). Otherwise:
proceed to Step 2.

### Step 2 — Verify the audited C-2a commit range is unchanged

Confirm the audited cumulative range is still `d4122c0^..5a0ab3d` (4
commits = R5 #1 ResolveArticleTool `display_citation` additive + R5 #2
`SkillGuardrailDispatcher.handleMustCiteSource` literal→shape rewrite +
R5 #3 skill yaml citation-wording consistency + dev handoff).

Specifically, run:
```
git log --oneline d4122c0^..5a0ab3d
```

Expected: exactly 4 commits in this range, with hashes `d4122c0`,
`7b0665e`, `1624a7a`, `5a0ab3d`. If the range has different commit
content (any commit was amended, rebased, or reordered since the
substantive review): raise as a NEW `scope_drift` blocking finding +
require substantive re-review.

The commit `8a7cb66` that flipped the dirty-tree gate is OUT of the
audited range (it touches only the previously-untracked context-pack
under `compact/`; zero C-2a / R5 file touched). Confirm via:
```
git show 8a7cb66 --stat
```
Expected: exactly 1 file touched —
`compact/context-pack-governance-framework-extraction-2026-06-06.md` —
with `+92 insertions(-0 deletions)`. No `server/` / `docs/` / yaml /
test file touched.

### Step 3 — Reissue the sprint-close verdict header

Replace ONLY the top-of-file verdict header in `docs/codex-findings.md`
with the new APPROVE shape:

```
## Sprint Review Decision
decision: pass
blocking_count: 0
final_verdict: APPROVE_S_AUTO_26
summary: <one paragraph; see template below>
```

Suggested summary template (adapt freely; cite the resolution
commit):

> Sprint 081 / S-Auto-26 / M-Auto-6 Sub-sprint C-2a is approved on
> re-review. The audited four-commit range `d4122c0^..5a0ab3d`
> previously passed every anti-hardcode and focal-point check
> (substantive verdict preserved at §1–§5); the prior `REJECT_S_AUTO_26
> / out_of_scope_review` was issued solely because of an untracked
> unrelated planning artifact in the working tree. Commit `8a7cb66`
> placed that file into its intended committed state (out of C-2a
> scope; touches only `compact/context-pack-governance-framework-extraction-2026-06-06.md`).
> Clean-tree gate now passes; the audited C-2a range is unchanged; the
> previously-recorded substantive verdict stands. Verdict flipped to
> `APPROVE_S_AUTO_26 / blocking_count=0`.

### Step 4 — Update §4 Blocking Findings

Replace the prior §4 content (which named the one blocking finding) with:

```
## §4 Blocking Findings

None on re-review. The prior `out_of_scope_review` finding was
resolved by commit `8a7cb66` (preserved the unrelated context-pack at
its intended committed location; out of C-2a scope). Clean-tree gate
now passes at HEAD; the audited cumulative range `d4122c0^..5a0ab3d`
is unchanged from the substantive review.
```

### Step 5 — Preserve §1 / §2 / §3 / §5 verbatim

DO NOT redo or rewrite the §1 per-change verdicts, §2 nine-question
kernel walkthrough, §3 five focal-point verdicts, or §5 non-blocking
observations. They were correct at the substantive review and remain
correct (the audited range did not change). They stay in
`docs/codex-findings.md` as-is.

Optional (RECOMMENDED): append a one-line addendum to §5 NBO #3
noting that re-review verified the audited range is unchanged at
HEAD `8a7cb66` (the dirty-tree-fix commit) without re-running tests
(since the source files in the audited range were not touched).

## Constraints

1. **Do NOT edit any code.** Only `docs/codex-findings.md` may be
   written (and only the verdict header + §4 + optional §5 addendum).
2. **Do NOT re-judge the substantive verdict.** The §1 / §2 / §3 / §5
   content was correct and remains correct.
3. **Do NOT widen scope** to other M-Auto-6 sub-sprints or to the
   non-C-2a parts of the working tree. The audited range is
   `d4122c0^..5a0ab3d`; the dirty-tree-fix commit is `8a7cb66`. That
   is the entire universe of this re-review.
4. **Cite anchors** for the clean-tree verification + range
   verification + commit `8a7cb66` shape.
5. **If you find a NEW blocking issue** (e.g. the working tree is
   still dirty for a reason I haven't accounted for; the audited
   range has been amended; a new file appeared in
   `compact/sprint-081-*` that wasn't there before): raise it as a
   blocking finding and do NOT auto-approve.

## Acceptance verdict shapes (re-review)

- **APPROVE_S_AUTO_26 / blocking_count=0**: clean-tree gate passes;
  audited range unchanged; substantive verdict stands. This is the
  expected outcome given the deliver-agent's report. Issue the new
  verdict header + §4 update + optional §5 addendum.
- **REJECT_S_AUTO_26 / out_of_scope_review (NEW)**: working tree
  still dirty for a different reason, or the audited range was
  amended / rebased since substantive review. Enumerate the new
  finding and require deliver-agent resolution.
- **REJECT_S_AUTO_26 / blocking_count > 0 (substantive)**: a code
  change appeared in the audited range since substantive review (e.g.
  a `git commit --amend` retroactively edited one of the 4 commits)
  that violates the file fence or §1.7. This should NOT happen given
  the deliver-agent's report — if you observe it, raise it as a
  substantive blocker and require human review.

When done: ensure `docs/codex-findings.md` carries the new verdict
header + updated §4 + preserved §1 / §2 / §3 / §5 + (optional) §5
addendum; commit nothing else; hand back to deliver-agent for close
+ Sub-sprint C-2b launch.
