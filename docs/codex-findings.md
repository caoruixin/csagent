## Sprint Review Decision
decision: <pass | fix_required | out_of_scope_review>
blocking_count: <number>
summary: <one paragraph — Codex's verdict on the active sprint or milestone>

## Review Evidence
<bullet list — review scope, commit range, what was loaded, what was re-run, what was independently verified>

## Blocking Findings (if any)
<numbered list; each entry quotes the diff snippet OR file:line + cited reference>

## Anti-Hardcode Kernel (per `iteration_governance.md` §4.1)
<per-Q verdict (Q1-Q9): pass | concern | fail, with one-line justification + file:line citation>

## §1.7 Boundary Check
<per-item verdict (a-e): pass | concern | fail, with one-line justification>

## Hard-Fence Verification
<per-fence verdict: pass | concern | fail, with file:line citation or empty-diff confirmation>

## Schema And Reproducibility Checks
<spot-check verdicts for line counts, allowlists, unchanged-claims, and reproducibility per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`>

## Validation Runs
<Java baseline + targeted spot-check runs + any unexpected results>

## Tier-0 Candidate Independent Verification (if applicable)
<C1-C5 verdicts: REJECTED preserved / QUALIFIED-DEFER continued / ELEVATE NOW / NOT A CANDIDATE>

## OQ Independent Verification
<per-OQ verdict, citing dev handoff §7 OQ ids and the contract-drift items if any>

## Deferred / Non-Blocking Notes
<observations that do not change the verdict but are worth recording>
