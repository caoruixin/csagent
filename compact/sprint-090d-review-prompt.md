# Codex re-review prompt — M-Auto-7 S-Y1.5d (targeted, resolves R-S90.5)

你是 **Anti-Hardcode Review Agent — targeted re-review for M-Auto-7
S-Y1.5d (Sprint 090, S-Auto-37)**. This is a NARROW §4.3 re-review of one
fix commit that resolves the single blocking finding **R-S90.5** from the
S-Y1.5c review. Its sole purpose: confirm the Q4 regex precision fix
resolves R-S90.5 without weakening the §1.7 Q4 hard-discard or introducing
any new issue — and, on pass, flip the held S-Y1.5c verdict.

**Scope (the ONLY change to judge):** the **corrected S-Y1.5d commit** (a
new commit on top of `9718dfa6`). Judge the net `Q4.case_id_literal` change
vs the pre-d state — `git diff d6b92228..708f8cb3 -- autoloop/` —
which is one regex line + Q4 tests:

- `autoloop/autoloop/sandbox/anti_hardcode_check.py` (the
  `_RE_Q4_CASE_ID_TOKEN` regex + its comment).
- `autoloop/tests/test_anti_hardcode_check.py` (new/updated Q4 tests
  incl. the `csmp_*` regression guard).

(Attempt-1 commit `9718dfa6` was HELD by R-S90.6 — see below.)

Do **not** re-litigate S-Y1.5b or the rest of S-Y1.5c — both already passed
the combined review (`docs/codex-findings.md`). This re-review covers ONLY
the Q4 regex delta.

## Context — the blocker being fixed

R-S90.5 (from the S-Y1.5c review): `Q4.case_id_literal`'s regex
`\bcs[0-9a-z_]{2,}\b` matched ANY word starting `cs` + 2 alnum/underscore,
so ordinary terms (`CSAT`, `csagent`, `css`) FALSE-FAILed as if they were
CaseSpec-id leaks. That broke S-Y1.5c's claim that the four retained
default-FAIL rules are all "surface == constitutional intent" and re-opened
a false-positive-discard vector on the `resolve_faq` S-Y2 pilot surface.
**Human architecture decision (2026-06-11): tighten the regex, keep FAIL.**

**R-S90.6 (attempt-1, now fixed):** the first S-Y1.5d regex
`\bcs[0-9_][0-9a-z_]+\b` required the discriminator *immediately after*
`cs`, so real `cs`+letter ids (`csmp_g01_uc_a_genuine_faq_miss_obscure`,
`csmp_s01_…`, the whole `manual_probe_uc_a_resolve_must` family) returned
PASS — a false-negative weakening the Q4 hard-discard. The deliver-agent
verified (direct Python `os.walk` from the repo root — see the
verification caveat below) that the corpus has **452 distinct cs-ids, every
one containing a digit or underscore, and ZERO pure-letter cs-ids**, so the
corrected rule is "contains a
digit/underscore ANYWHERE in the cs-token."

## What S-Y1.5d shipped (corrected)

`_RE_Q4_CASE_ID_TOKEN`: `\bcs[0-9a-z_]{2,}\b` →
**`\bcs[a-z0-9_]*[0-9_][a-z0-9_]*\b`** (`cs` + a run of `[a-z0-9_]`
containing at least one digit or underscore). `Q4.case_id_literal`
severity stays `_FAIL`. Nothing else changed.

**Verification caveat:** verify corpus claims with a direct Python
`os.walk` scan **run from the repo root** (or with absolute paths), NOT
recursive `grep -r`/`find` — the latter gave misleading empty results in
the deliver session due to a shell `cwd` drift into `autoloop/`.

## Loader (minimal)

1. `AGENTS.md` (auto-loaded).
2. **This prompt** — self-contained.
3. The corrected-d diff: `git diff d6b92228..708f8cb3 -- autoloop/`.
4. Dev handoff `## S-Y1.5d` section: `docs/sprints/sprint-090-handoff.md`.
5. The held verdict to update: `docs/codex-findings.md`
   (`## Sub-sprint Review Decision — S-Y1.5c` + the `R-S90.5` finding).

## Verification checklist (all must hold)

1. **R-S90.5 resolved.** Probe the tightened regex: `CSAT`, `csagent`,
   `css` (in clean prose) now return **PASS** (no `Q4.case_id_literal`
   match).
2. **Q4 hard-discard preserved — incl. the R-S90.6 `cs`+letter class.**
   `cs011`, `cs042`, `cs101`, `cs_uc_a_no_ad_id`, `cs38s01`, `cs59s`
   **AND** `csmp_g01_uc_a_genuine_faq_miss_obscure`,
   `csmp_s01_uc_a_search_hits_with_ad_id_mismatch`,
   `csmp_n01_uc_a_visibility_search_hits` still return **FAIL**
   `Q4.case_id_literal`. (Codex's earlier S-Y1.5c probes for `session_id=abc`
   / `case_id: 42` are `Q4.id_assignment_literal`, untouched.) **Then run
   the corpus-completeness check** (direct Python `os.walk` over
   `eval_interactive/case_specs` + `case_specs_shadow`, NOT recursive grep):
   every cs-prefixed CaseSpec id must FAIL — deliver reference **452 ids, 0
   misses**. Any miss ⇒ `fix_required`.
3. **No new hardcode.** The discriminator is a STRUCTURAL shape (the
   cs-token contains a digit or underscore), not a word whitelist and not
   any specific eval phrase / CaseSpec literal. Confirm the source contains
   no real `cs<id>` literal (D2 self-discipline test stays green). Kernel Q4
   (no eval-text encoding) + Q5/Q6 (no new soft-decision hardcode) hold: the
   change REMOVES false positives, it does not encode a decision the LLM
   owns.
4. **Scope minimal.** Only the two named files changed. `_RULES` order +
   `Q4.case_id_literal` severity (`_FAIL`) unchanged; `_normalize`,
   `_first_new_match`, all other rule regexes, `severity_overrides`,
   `propose.txt`, `config.yaml`, scoring/ all untouched.
5. **Scoring SHA stable / no re-bless.** `autoloop/autoloop/scoring/` has
   no diff; `scoring_code_baseline_sha = 0d86b08f…` (config.yaml:340)
   reproduces; `gaming.scoring_code_drift.sha_changed` must NOT fire.
6. **Full suite green.** Full `autoloop` pytest passes (dev: 346 passed,
   1 pre-existing warning; baseline 344 + 2 new Q4 tests; no existing
   FAIL-for-genuine-id test regressed).

## Residual to note (non-blocking)

The corrected rule catches every cs-token containing a digit or underscore.
Verified-complete for the current corpus (452 ids, 0 pure-letter cs-ids).
Residual: ordinary tokens that happen to contain an embedded digit
(`css3`, `cs50`, `cs2go`) still FALSE-FAIL — a far narrower class than the
original `csat`/`csagent` one, and unlikely in durable Skill prose. Note as
a non-blocking OQ; it does not block. (A future id of shape pure-`cs<letter>`
with no digit/underscore would be missed, but none exists in the corpus and
the naming convention always carries a numeric/underscore index.)

## Output — update `docs/codex-findings.md`

On pass:

- Flip `## Sub-sprint Review Decision — S-Y1.5c` from
  `decision: fix_required` / `blocking_count: 1` → **`decision: pass` /
  `blocking_count: 0`**, with a one-line summary noting R-S90.5 is resolved
  by S-Y1.5d (`9718dfa6`).
- Mark the `R-S90.5` blocking finding **RESOLVED-by-S-Y1.5d** (keep the
  finding text; add the resolution line + the verification evidence:
  CSAT/csagent → PASS, real ids → FAIL, scoring SHA stable, suite green).
- Add a short `## Sub-sprint Review Decision — S-Y1.5d` stanza
  (`decision` / `blocking_count` / one-paragraph summary) for the targeted
  fix-iteration.

If a checklist item FAILs, return `fix_required` with the specific item +
the failing probe; do not flip S-Y1.5c.

## Constraints

- Do NOT edit code or tests. Review only.
- Targeted re-review: do not re-open b or the already-approved parts of c.
- Naming: this resolves the S-Y1.5c blocker; after it passes, the combo
  close (b + c + d) is review-complete and S-Y2 Part C (`run -n 3` under
  the existing pilot block) is unblocked on a clean tree.
