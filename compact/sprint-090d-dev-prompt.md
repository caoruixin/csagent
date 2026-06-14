# Dev prompt — Sprint 090 (S-Y1.5d session) / S-Auto-37 (M-Auto-7 S-Y1.5d) — REVISED (attempt 2)

> **REVISION (2026-06-12):** S-Y1.5d attempt-1 (commit `40f8c07`, regex
> `\bcs[0-9_][0-9a-z_]+\b`) was HELD by Codex blocker **R-S90.6**: that
> discriminator required a digit/underscore *immediately after* `cs`, so
> real `csmp_*` CaseSpec ids (`cs`+letter, e.g.
> `csmp_g01_uc_a_genuine_faq_miss_obscure`) slipped through → PASS →
> false-negative that weakened the §1.7 Q4 hard-discard. This revised prompt
> uses the **corrected regex** (digit/underscore ANYWHERE in the token),
> exhaustively verified by the deliver-agent against all **452** corpus
> cs-ids (0 misses). Make a NEW commit on top of `40f8c07`.

你是 **dev agent for Sprint 090 / S-Auto-37 (M-Auto-7 S-Y1.5d)** — a
surgical fix-iteration that resolves Codex blockers **R-S90.5** (ordinary
`cs…` words false-FAIL) AND **R-S90.6** (real `csmp_*` ids false-PASS) on
the `Q4.case_id_literal` rule.

**One-line goal:** tighten the `Q4.case_id_literal` regex so it FAILs only
real CaseSpec-id shapes (`cs` immediately followed by a digit or
underscore: `cs011`, `cs_uc_a_no_ad_id`, `cs38s01`) and stops false-FAILing
ordinary `cs…` words (`CSAT`, `csagent`, `css`). Keep the rule's severity
`_FAIL` (the §1.7 raw-eval-phrase hard-discard stays). Pure autoloop-infra;
**no re-bless**.

## Why (the blocker)

Codex R-S90.5 (in `docs/codex-findings.md`): the regex at
`autoloop/autoloop/sandbox/anti_hardcode_check.py:274`,
`\bcs[0-9a-z_]{2,}\b`, matches ANY word starting `cs` + 2 alnum/underscore.
Independent probes returned `FAIL Q4.case_id_literal` on "Use **CSAT**
feedback…" and "The **csagent** should provide grounded answers." — neither
is a CaseSpec id. This breaks S-Y1.5c's load-bearing claim that the four
retained default-FAIL rules are all "surface form == constitutional
intent", and it re-opens a false-positive-discard vector on exactly the
`resolve_faq` surface the S-Y2 pilot runs on. The human chose: **tighten
the regex, keep FAIL** (2026-06-11). Codex confirmed `cs011`,
`cs_uc_a_no_ad_id`, `session_id=abc`, `case_id: 42` must all still FAIL.

## Read order (minimal)

1. `AGENTS.md` (auto-loaded — governance chain).
2. **This prompt** — self-contained.
3. The blocker stanza: `docs/codex-findings.md` (the `## R-S90.5` finding).

## Class

| Field | Value |
|---|---|
| **Layer (§3.2)** | `infra` — autoloop sandbox detector regex precision fix |
| **§7 stanza** | **REQUIRED** (touches the §1.7 enforcement detector). Stanza below; copy into the handoff. |
| **Per-sub-sprint Codex (§4.3)** | **REQUIRED** — targeted re-review of just the Q4 regex diff; resolves R-S90.5 and flips the S-Y1.5c stanza to `pass`. |

## Code anchors

- `autoloop/autoloop/sandbox/anti_hardcode_check.py`:
  * `_RE_Q4_CASE_ID_TOKEN` — lines **274-277** (THE edit):
    ```python
    _RE_Q4_CASE_ID_TOKEN = re.compile(
        r"\bcs[0-9a-z_]{2,}\b",
        re.IGNORECASE,
    )
    ```
  * `_RULES` entry `("Q4.case_id_literal", _FAIL, _q4_case_id_token)` —
    line **370**. **Severity stays `_FAIL` — do NOT touch the registry.**
  * `_q4_case_id_token` helper — line **285** — unchanged (only the regex
    it uses changes).
- Tests `autoloop/tests/test_anti_hardcode_check.py`:
  * `test_q4_case_id_token_rejected` line **144** — uses `cs042` / `cs101`
    (cs+digit) → still FAIL after the fix; should stay green.
  * `test_q4_clean_text_with_cs_in_word_passes` line **174** — mid-word
    "cs" (discuss/customers) → already PASS via `\bcs`; stays green.
  * Other Q4 tests (`…session_id_assignment…`, `…case_id_in_assignment…`)
    are `Q4.id_assignment_literal` — a different regex — untouched.

## Scope (surgical)

1. **Tighten `_RE_Q4_CASE_ID_TOKEN`** to require a digit-or-underscore
   discriminator **ANYWHERE in the cs-token** (not just immediately after
   `cs` — that was the attempt-1 bug that missed `csmp_*`). The structural
   invariant, verified against the corpus: every one of the 452 real
   CaseSpec ids contains a digit or underscore; the false-positive class
   (`csat`/`csagent`/`css`/`csv`/`cstring`) are pure letters. Required
   form:
   ```python
   _RE_Q4_CASE_ID_TOKEN = re.compile(
       r"\bcs[a-z0-9_]*[0-9_][a-z0-9_]*\b",
       re.IGNORECASE,
   )
   ```
   (Reads: `cs` + any run of `[a-z0-9_]` that contains at least one digit
   or underscore.) You own the exact final form, but it MUST satisfy every
   row of the test matrix AND the corpus-completeness check below — both
   are hard requirements, not just the matrix.
2. **Keep `Q4.case_id_literal` severity `_FAIL`** in `_RULES` (no registry
   change). The override-hatch + all other rules are out of scope.

That is the entire change surface: one regex line + new/updated tests.

## Test matrix (the binding contract)

Add/extend tests in `autoloop/tests/test_anti_hardcode_check.py`. Every row
must hold:

| Input (in clean surrounding prose) | Expected | Why |
|---|---|---|
| `cs011`, `cs042`, `cs101` | **FAIL** `Q4.case_id_literal` | real numeric CaseSpec ids |
| `cs_uc_a_no_ad_id` | **FAIL** `Q4.case_id_literal` | underscore CaseSpec id (the CS4 pilot ids) |
| `cs38s01`, `cs59s` | **FAIL** `Q4.case_id_literal` | shadow `cs<n>s<n>` ids |
| `csmp_g01_uc_a_genuine_faq_miss_obscure`, `csmp_s01_uc_a_search_hits_with_ad_id_mismatch`, `csmp_n01_uc_a_visibility_search_hits` | **FAIL** `Q4.case_id_literal` | **the R-S90.6 class** — real `cs`+letter ids (manual_probe family) that attempt-1 missed |
| `CSAT` / `Use CSAT feedback as a quality signal.` | **PASS** | ordinary CS term — the R-S90.5 false-positive |
| `csagent` / `The csagent should provide grounded answers.` | **PASS** | project name — the R-S90.5 false-positive |
| `css`, `cstring`, `csv`, `cscience` (in clean prose) | **PASS** | ordinary `cs`+letter words (pure letters, no digit/underscore) |
| `discuss the customer's case` | **PASS** | mid-word `cs` (existing test — must stay green) |

Concrete new tests (names are suggestions):

- `test_q4_ordinary_cs_words_pass` — `CSAT`, `csagent`, `css`, `csv`,
  `cscience` (in clean prose) → assert verdict == `PASS`.
- `test_q4_underscore_and_shadow_case_ids_still_fail` — `cs_uc_a_no_ad_id`,
  `cs38s01`, `cs59s` → assert verdict == `FAIL`, rule_id ==
  `Q4.case_id_literal`.
- `test_q4_csmp_letter_prefixed_case_ids_still_fail` (**R-S90.6
  regression guard**) — `csmp_g01_uc_a_genuine_faq_miss_obscure`,
  `csmp_s01_uc_a_search_hits_with_ad_id_mismatch`,
  `csmp_n01_uc_a_visibility_search_hits` → assert verdict == `FAIL`,
  rule_id == `Q4.case_id_literal`. These are `cs`+letter ids; attempt-1's
  regex let them PASS.
- Keep `test_q4_case_id_token_rejected` green (cs042/cs101 still FAIL).

### Corpus-completeness check (HARD — the attempt-1 failure mode)

Before close, prove the chosen regex catches **every** `cs`-prefixed
CaseSpec id in the corpus (0 misses). Use a direct Python `os.walk` scan
**run from the repo root** (or with absolute paths) — recursive `grep -r`
/ `find` gave misleading empty results in the deliver-agent session because
the shell `cwd` had drifted into `autoloop/`, where relative
`eval_interactive/…` paths don't exist. Example:

```python
import os, re
roots = ["eval_interactive/case_specs", "eval_interactive/case_specs_shadow"]
rx = re.compile(r"\bcs[a-z0-9_]*[0-9_][a-z0-9_]*\b", re.I)
ids = set()
for r in roots:
    for dp, _, fns in os.walk(r):
        for f in fns:
            if f.endswith((".yaml", ".yml")) and f[:-5].startswith("cs") or f[:-4].startswith("cs"):
                ids.add(f.rsplit(".", 1)[0])
            # also scan `case_id:` / `id:` fields inside each spec
misses = sorted(i for i in ids if i.startswith("cs") and not rx.search(i))
print("cs-id count:", len([i for i in ids if i.startswith("cs")]), "MISSES:", misses)
```

Deliver-agent reference result: **452 cs-ids, 0 misses** with the required
regex. If your scan reports ANY miss, the regex is wrong — STOP and fix
before close. Record the count + `MISSES: []` in the handoff.

## Test / eval requirements

- Full `autoloop` pytest **green** (baseline **344**). Net delta should be
  the new tests only; **no existing test may regress** — in particular grep
  for any test that asserts `Q4.case_id_literal` FAIL and confirm its
  literal is a real `cs<digit|_>` id (if any used a `cs`+letter fake id,
  STOP and surface — that test was asserting on a false positive).
- The 3 real-meta-agent calibration samples + the FP-zero-on-clean test
  stay green (they were already PASS; the tighten only removes false FAILs,
  so it cannot newly fail a clean sample).
- `scoring_code_baseline_sha` (config.yaml:340 = `0d86b08f…`) **unchanged**
  — prove it. **No re-bless.**
- No §3.4 dry-run needed: this change does not affect proposer output — it
  only makes the detector MORE permissive on a false-positive class. The
  test matrix is the evidence.
- Mocked/unit only — per §5.7, the real-LLM evidence remains the downstream
  S-Y2 Part C re-tranche.

## Hard fences / STOP

- Edit **only** `autoloop/autoloop/sandbox/anti_hardcode_check.py` (the one
  regex line) + `autoloop/tests/test_anti_hardcode_check.py`.
- **Do NOT change** `Q4.case_id_literal` severity (`_FAIL` stays), any
  other rule regex, `_normalize`, `_first_new_match`, the `_RULES` order,
  the `severity_overrides` machinery, `propose.txt`, `config.yaml` (except
  nothing — config is untouched here), or any scoring file.
- **Do NOT widen** the regex such that a real `cs<digit>` / `cs_<id>` id
  PASSes — the §1.7 hard-discard for real ids must be preserved (Codex
  hard-fences this).
- **No `eval_interactive/**`, no Skill YAML, no `server/**`, no
  `docs/sprints|milestones|archive` edits.**
- **STOP + escalate** if: any real-id row in the matrix PASSes; any
  existing FAIL test for a genuine id flips; `scoring_code_baseline_sha`
  changes; or you cannot satisfy the full matrix with a single regex.

## §7 Layer-classification + anti-hardcode stanza (copy into handoff)

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (autoloop sandbox detector regex
precision fix; eval-framework-adjacent — removes a false-positive-discard
class that was blocking legitimate `cs…` prose pre-eval).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. It narrows
an over-broad §1.7 enforcement regex; the Q4 raw-eval-phrase hard-discard
is preserved (real `cs<digit|_>` ids still FAIL by default).

**Semantic hardcode:** No semantic hardcode introduced. The change REMOVES
false positives (ordinary `CSAT` / `csagent` language) without weakening
the forbidden-list intent; it does not encode any specific eval phrase or
word whitelist — it uses a structural discriminator (digit/underscore
after `cs`) that distinguishes CaseSpec-id shape from ordinary words.

**Generalization coverage:** target / neighbor / negative / shadow case
counts: n/a — pure-infra detector regex fix. Validation = the §"Test
matrix" unit tests (real ids FAIL; ordinary cs-words PASS) + full autoloop
pytest green + scoring SHA stable.
```

## Codex re-review plan (§4.3, REQUIRED — targeted)

Targeted re-review of ONLY the Q4 regex diff (resolves R-S90.5). Codex
confirms: (1) `CSAT` / `csagent` now PASS; (2) `cs011` / `cs_uc_a_no_ad_id`
/ `cs38s01` still FAIL `Q4.case_id_literal`; (3) no other rule-body,
severity, scoring, or propose.txt change; (4) full pytest green; (5)
`scoring_code_baseline_sha` stable. On pass, the S-Y1.5c
`## Sub-sprint Review Decision — S-Y1.5c` stanza flips
`fix_required → pass`, `blocking_count → 0`, and R-S90.5 is marked
resolved-by-S-Y1.5d.

## Handoff (single combo file — `docs/sprints/sprint-090-handoff.md`)

Append a short `## S-Y1.5d — Q4 regex precision fix (resolves R-S90.5)`
section: the one-line regex before/after; the test matrix results; the §7
stanza above; the scoring-SHA-stable / no-re-bless proof; confirmation
that S-Y1.5c's "four retained FAIL rules are surface==intent" thesis now
holds (the Q4 discriminator makes `cs<digit|_>` a CaseSpec-id surface).
Do NOT rewrite the existing b / c sections.

## Commit discipline

Stage explicitly by file (NO `git add -A`). The dev commit is
`autoloop/**` only (the detector regex + tests). The handoff section is a
deliver/human close artefact bundled at close — NOT part of the dev
commit. Do not run a non-dry-run autoloop.

## Self-check (tick before close)

- [ ] `_RE_Q4_CASE_ID_TOKEN` requires digit/underscore right after `cs`;
      `Q4.case_id_literal` severity still `_FAIL` (registry untouched).
- [ ] Test matrix all green: real ids (`cs011`/`cs042`/`cs101`/
      `cs_uc_a_no_ad_id`/`cs38s01`/`cs59s` **+ `csmp_*` letter-prefixed
      ids**) FAIL; ordinary words (`CSAT`/`csagent`/`css`/`csv`) PASS;
      mid-word `cs` test still PASS.
- [ ] Corpus-completeness check run via Python `os.walk` from the repo
      root (not recursive grep): all cs-prefixed corpus ids FAIL,
      `MISSES: []` (deliver ref: 452 ids, 0 misses); count recorded in
      handoff.
- [ ] No other regex / severity / `_normalize` / scoring / propose.txt /
      config change.
- [ ] Full autoloop pytest green (344 + new tests); no existing FAIL test
      for a genuine id regressed.
- [ ] `scoring_code_baseline_sha` unchanged (proof recorded); no re-bless.
- [ ] Only `anti_hardcode_check.py` + `test_anti_hardcode_check.py`
      touched; commit is `autoloop/**` only.
- [ ] Handoff: `## S-Y1.5d` section appended (b/c sections untouched);
      §7 stanza present.
