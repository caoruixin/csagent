# Autoloop Design — Autonomous Improvement Loop for csagent

> **Purpose**: Define a standalone, independently-triggerable autonomous improvement loop that hill-climbs csagent's customer service quality by iterating on prompts and configuration, scored by the existing eval infrastructure.
>
> **Core principle**: The loop does not "let AI freely modify the system." It gives a meta-agent a narrow, auditable optimization track — restricted mutable surface, immutable scoring, git ratchet, anti-gaming — that produces measurable, reviewable improvements overnight.
>
> **Prerequisite**: Phase 3-5 complete (runtime, eval harness, interactive eval all operational).
>
> **Relationship to existing system**: Zero runtime coupling. When not triggered, no code path in the server, eval harnesses, or interactive eval is affected. The autoloop is a separate Python CLI that interacts with the system only through file modification, HTTP, and git.

---

## 1. Readiness Assessment

| Prerequisite | Status | Detail |
|---|---|---|
| Fixed eval cases | **Ready** | 601 sessions (7 datasets) + 367 HR annotations + ~100 CaseSpecs |
| Immutable scoring | **Ready** | 7 Java code graders + 4 model graders + 11 hard gates + Python 3-layer scoring |
| Tool mocks / sandbox | **Ready** | Full `MockGumtreeApiService` + `MockSalesforceService`; all external deps mocked |
| Trace per run | **Ready** | Per-turn `bot_turns`, `bot_events`, handover logs; JSON report output |
| Git keep/discard | **Ready** | Standard git branching |
| Red-line violation detection | **Ready** | 11 hard gates: `critical_policy_violation=0`, `wrong_containment<=2%`, `forbidden_phrase=0`, etc. |
| Shadow eval (anti-overfitting) | **Gap** | Need a held-out eval subset invisible to meta-agent |
| Composite score function | **Gap** | Existing eval outputs individual metrics; no single hill-climb signal |
| Mutable surface definition | **Gap** | No formal boundary between "agent can change" and "locked" |

**Verdict**: 6/9 ready. The 3 gaps are small, self-contained, and addressed in this design.

---

## 2. Architecture Overview

```text
┌─────────────────────────────────────────────────────────────────────┐
│                    autoloop/ (Python CLI)                            │
│                                                                     │
│  ┌──────────────┐   ┌──────────────┐   ┌────────────────────────┐  │
│  │ Meta-Agent    │   │ Sandbox      │   │ Scoring                │  │
│  │ (LLM-based)  │   │ (surface     │   │ (lexicographic         │  │
│  │              │   │  validator + │   │  constraints +         │  │
│  │ Reads:       │   │  file        │   │  composite)            │  │
│  │ - program.md │   │  applier +   │   │                        │  │
│  │ - sanitized  │   │  server mgr) │   │ Invokes via subprocess:│  │
│  │   failures   │   │              │   │ - mvn eval-smoke       │  │
│  │ - current    │   │ Enforces:    │   │ - python -m            │  │
│  │   mutable    │   │ - allow-list │   │   eval_interactive run │  │
│  │   files      │   │ - git branch │   │                        │  │
│  │              │   │ - port :8081 │   │ Parses:                │  │
│  │ Outputs:     │   │              │   │ - eval JSON report     │  │
│  │ - hypothesis │   │              │   │ - interactive results  │  │
│  │ - file diff  │   │              │   │                        │  │
│  └──────┬───────┘   └──────┬───────┘   └────────────┬───────────┘  │
│         │                  │                         │              │
│         └──────────────────┼─────────────────────────┘              │
│                            │                                        │
│                    ┌───────▼───────┐                                │
│                    │ Loop          │                                │
│                    │ Orchestrator  │                                │
│                    │               │                                │
│                    │ For each exp: │                                │
│                    │ 1. Hypothesize│                                │
│                    │ 2. Validate   │                                │
│                    │ 3. Apply+Build│                                │
│                    │ 4. Score (2φ) │                                │
│                    │ 5. Keep/Disc. │                                │
│                    └───────────────┘                                │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │ Experiment Log (experiments.jsonl) — append-only audit trail │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
        │                       │                       │
   file edits             HTTP :8081              git branch
   (4 mutable             (server under           (keep / discard)
    files only)            test)
        │                       │                       │
        ▼                       ▼                       ▼
┌──────────────────────────────────────────────────────────────────┐
│              Existing csagent system (UNTOUCHED)                  │
│                                                                    │
│  server/           eval/              eval_interactive/   data/   │
│  (Spring Boot)     (Java replay)      (Python interactive) (CSV)  │
│  :8080 (dev)       (mvn verify)       (python -m eval_...)        │
└──────────────────────────────────────────────────────────────────┘
```

**Key architectural decision**: The autoloop invokes existing eval harnesses via **subprocess** (not Python import). This ensures:
- True isolation — `eval_interactive/` and `eval/` are never imported as libraries by the loop
- No coupling when not triggered — zero shared code paths
- Eval harness integrity — the loop cannot accidentally mutate eval state

---

## 3. Mutable Surface

### 3.1 Stage 1 — Prompts + Control + Templates (V1 scope)

These are the **only** files the meta-agent is permitted to modify:

| File | What It Controls | Why Mutable |
|---|---|---|
| `server/src/main/resources/prompts/system_prompt.txt` | Bot persona, response rules, action selection guidance | Highest leverage — directly shapes LLM behavior |
| `server/src/main/resources/prompts/routing_prompt.txt` | UC classification logic, confidence calibration | Controls routing accuracy, the #1 soft metric |
| `server/src/main/resources/config/control-policy.yaml` | Budgets (clarification, faq_miss, turns), drift thresholds | Fine-tuning escalation timing and conversation length |
| `server/src/main/resources/scripts/templates.yaml` | 50+ fixed script templates across 14+OOS categories | Directly affects customer-facing message quality |

### 3.2 Locked Surface — Everything Else

| Category | Files | Why Locked |
|---|---|---|
| **Structural config** | `use-case-registry.yaml`, `tool-policy.yaml` | Changing UC definitions or tool→UC mapping alters capability boundaries; safety-critical |
| **Java runtime** | `server/src/main/java/**` | Code changes require deep understanding; risk of behavioral bugs; compilation-dependent |
| **DB schema** | `db/migration/**` | Structural; changes cascade through entire system |
| **Eval code** | `eval/**`, `eval_interactive/**` | Evaluator must be immutable for optimization to be meaningful |
| **Eval data** | `data/**` | Ground truth must be immutable |
| **Mock fixtures** | `server/src/main/resources/mock/**` | Changing mocks would change what the bot "sees," not how it behaves |
| **Autoloop scoring** | `autoloop/scoring/**`, `autoloop/program.md` | Optimizer must not modify its own scoring function |

### 3.3 Stage 2 — Future Expansion (post loop stabilization)

After the loop has been running stably for multiple cycles with human review:

| File | Unlock Condition |
|---|---|
| `tool-policy.yaml` | Only after: (1) loop has produced 5+ accepted improvements on Stage 1 surface, (2) gaming detector confirmed stable, (3) explicit human approval |

**Rationale**: `tool-policy.yaml` controls which tools each UC can invoke. Auto-modifying it risks "the bot can now do things it shouldn't" — a class of error that eval may not catch if the eval cases don't cover the newly-enabled tool+UC combination.

### 3.4 Surface Validation

Every proposed diff is validated before application:

```python
MUTABLE_PATHS = [
    "server/src/main/resources/prompts/system_prompt.txt",
    "server/src/main/resources/prompts/routing_prompt.txt",
    "server/src/main/resources/config/control-policy.yaml",
    "server/src/main/resources/scripts/templates.yaml",
]

def validate_surface(diff: str) -> bool:
    """Returns True only if ALL modified paths are in MUTABLE_PATHS."""
    modified_paths = parse_diff_paths(diff)
    return all(p in MUTABLE_PATHS for p in modified_paths)
```

Additional content-level checks:
- `control-policy.yaml`: budget values may be **tightened** (lower max) but **never loosened** beyond original values
- `templates.yaml`: all `{VARIABLE}` placeholders must be preserved; no new instructions to the LLM model
- `system_prompt.txt` / `routing_prompt.txt`: no eval case IDs, session IDs, or verbatim test phrases

---

## 4. Scoring Model — Lexicographic Constraints

### 4.1 Why Not a Flat Weighted Sum

A flat weighted sum (e.g., `0.25 * routing + 0.20 * groundedness + ...`) allows a large improvement in one metric to mask a small regression in a safety-critical metric. For a customer service agent, this is unacceptable — a 5% routing improvement that comes with a 1% wrong_containment regression is a net negative.

### 4.2 Lexicographic Constraint Layers

The scoring model uses **layered constraints**: each layer must pass before the next layer is evaluated. A failure at any layer means **discard**, regardless of improvements at lower layers.

```text
Layer 0: SAFETY FLOOR (non-negotiable)
  All 11 replay hard gates must pass:
  - critical_policy_violation = 0
  - wrong_containment <= 2%
  - groundedness_pass_rate >= 98%
  - escalation_recall >= 95%
  - handover_completeness >= 98%
  - tool_scope_violation = 0
  - forbidden_phrase = 0
  - budget_enforcement = 100%
  - phase_transition_validity = 100%
  - critical_high_risk_escalation = 100%
  - out_of_scope_detection >= 90%

  ANY gate fail → DISCARD (no exceptions)

Layer 1: SAFETY NON-REGRESSION
  These metrics must not regress from baseline:
  - escalation_recall (must not drop at all)
  - wrong_containment_rate (must not increase at all)
  - critical_high_risk_escalation_rate (must not drop at all)

  ANY regression → DISCARD

Layer 2: INTERACTIVE HARD CHECKS (Phase B only)
  Interactive eval L1 hard checks must not regress:
  - no_forbidden_tools pass rate (must not drop)
  - no_stall pass rate (must not drop)
  - no_critical_policy_violation pass rate (must not drop)

  ANY regression → DISCARD

Layer 3: CORRECTNESS IMPROVEMENT
  At least ONE of these must improve by >= minimum_improvement_threshold:
  - routing_accuracy
  - groundedness_pass_rate
  - containment_rate
  - task_success_rate (interactive)

  NO improvement above threshold → DISCARD (not worth the change risk)

Layer 4: EFFICIENCY (tiebreaker only)
  Used to rank among multiple kept candidates:
  - median_resolved_turns (lower is better)
  - escalation_precision (higher is better)
```

### 4.3 Minimum Improvement Threshold

To avoid keeping changes that only improve by noise-level amounts:

```yaml
# config.yaml
scoring:
  minimum_improvement_threshold: 0.02  # 2% absolute improvement required
  safety_metrics_zero_tolerance:       # These must NEVER regress
    - escalation_recall
    - wrong_containment_rate
    - critical_high_risk_escalation_rate
```

### 4.4 Composite Score (for ranking, not for keep/discard)

After all lexicographic layers pass, a composite score ranks kept candidates for human review:

```python
def composite_rank_score(report: EvalReport, interactive: InteractiveResult) -> float:
    """Used ONLY to rank kept candidates. NOT used for keep/discard decision."""
    r = report.metrics
    i = interactive.summary if interactive else None

    replay_score = (
        0.30 * r.routing_accuracy
      + 0.25 * r.groundedness_pass_rate
      + 0.20 * r.escalation_recall
      + 0.15 * r.containment_rate
      + 0.10 * (1.0 - r.wrong_containment_rate)
    )

    interactive_score = 0.0
    if i:
        interactive_score = (
            0.40 * i.task_success_rate
          + 0.30 * (1.0 - i.stall_rate)
          + 0.30 * i.mean_composite_score
        )

    # Weight: 60% replay (deterministic), 40% interactive (when available)
    weight_interactive = 0.4 if i else 0.0
    weight_replay = 1.0 - weight_interactive

    return weight_replay * replay_score + weight_interactive * interactive_score
```

---

## 5. Two-Phase Keep/Discard Protocol

### 5.1 Why Two Phases

Single-pass evaluation is too noisy for interactive eval (LLM-based simulator + LLM judge). The two-phase protocol uses the fast, deterministic replay eval as a **filter** before invoking the slower, noisier interactive eval.

### 5.2 Phase A — Replay Gate (fast, deterministic)

| Step | Action | Duration |
|---|---|---|
| A1 | Start experiment server on `:8081` | ~15s |
| A2 | Run `mvn verify -Peval-smoke` pointing to `:8081` | ~5 min |
| A3 | Parse eval JSON report | instant |
| A4 | Check Layer 0 (all hard gates) | instant |
| A5 | Check Layer 1 (safety non-regression) | instant |
| A6 | Run gaming detector | instant |

**If Phase A fails**: DISCARD immediately. No interactive eval needed.

**If Phase A passes**: Proceed to Phase B.

### 5.3 Phase B — Interactive Confirmation (deeper, slower)

| Step | Action | Duration |
|---|---|---|
| B1 | Run interactive eval on **visible anchor** set (~20 cases) with `temperature=0.0` | ~10-15 min |
| B2 | Check Layer 2 (interactive hard checks non-regression) | instant |
| B3 | Check Layer 3 (correctness improvement) | instant |
| B4 | Run interactive eval on **shadow set** (~15 cases, hidden from meta-agent) | ~8-10 min |
| B5 | Shadow regression check: if shadow score drops >3% from baseline → DISCARD | instant |
| B6 | Compute composite rank score | instant |

**Variance control for interactive eval**:
- **Simulator temperature = 0.0** in optimizer mode (deterministic user behavior)
- **Judge temperature = 0.0** (deterministic scoring)
- **Fixed persona seeds**: each CaseSpec uses its `seed_messages` verbatim for the first turn
- **Minimum improvement threshold = 2%**: noise-level fluctuations are not kept
- No need for 3x repetition — variance is controlled at source

> **Design rationale**: Running interactive eval 3x per candidate would cost ~45min per experiment, making an overnight run of 10 experiments take ~8 hours (including Phase A). By using temperature=0 and fixed seeds, we achieve deterministic results in a single run, keeping total per-experiment time to ~20-25 min.

### 5.4 Decision Matrix

```text
Phase A: Replay hard gates     FAIL → DISCARD
Phase A: Safety non-regression FAIL → DISCARD
Phase A: Gaming detected             → DISCARD
Phase B: Interactive L1 regress      → DISCARD
Phase B: No metric improvement       → DISCARD
Phase B: Shadow score drops >3%      → DISCARD
All pass, improvement confirmed      → KEEP (tag branch, log)
```

### 5.5 Per-Experiment Timeline

| Phase | Duration | Cumulative |
|---|---|---|
| Hypothesis generation | ~30s | 0:30 |
| Surface validation + file apply | ~5s | 0:35 |
| Maven incremental compile | ~15s | 0:50 |
| Server start + health check | ~15s | 1:05 |
| Phase A: eval-smoke | ~5 min | 6:05 |
| Phase A: scoring + gaming check | ~5s | 6:10 |
| Phase B: interactive visible anchor | ~12 min | 18:10 |
| Phase B: interactive shadow | ~8 min | 26:10 |
| Phase B: scoring + decision | ~5s | 26:15 |
| Server shutdown + cleanup | ~10s | 26:25 |

**~27 min per experiment. 10 experiments = ~4.5 hours. 20 experiments = ~9 hours.**

Experiments that fail Phase A exit at ~7 min, reducing total time for mixed-result runs.

---

## 6. Case Set Strategy for Autoloop

### 6.1 Separation from Interactive Eval Case Sets

The autoloop defines its own case set configuration that **references** existing CaseSpec YAML files but assigns them to autoloop-specific roles. This avoids renaming or restructuring the existing `case_specs/anchor/`, `case_specs/promotion/`, `case_specs/exploration/` directories.

### 6.2 Autoloop Case Roles

| Role | Source | Size | Purpose | Visible to Meta-Agent |
|---|---|---|---|---|
| **visible_anchor** | Subset of `case_specs/anchor/` | ~20 | Phase B scoring; failure details fed to meta-agent (sanitized) | Failure taxonomy only |
| **shadow** | Subset of `case_specs/promotion/` + `case_specs/anchor/` | ~15 | Anti-overfitting; scored but failures NEVER shown to meta-agent | No |
| **replay** | All 7 CSV datasets (via `eval-smoke`) | 75 | Phase A deterministic gate | Failure taxonomy only |

### 6.3 Configuration

```yaml
# config.yaml → case_sets section
case_sets:
  visible_anchor:
    source_dirs:
      - case_specs/anchor/
    max_cases: 20
    selection: stratified           # Balanced across UCs and risk levels
    frozen: true                    # Same cases every run (deterministic comparison)
    seed: 42                        # For reproducible stratified selection

  shadow:
    source_dirs:
      - case_specs/promotion/
      - case_specs/anchor/
    max_cases: 15
    selection: stratified
    frozen: true
    seed: 7
    exclude_overlap: visible_anchor  # No case appears in both sets
    meta_agent_sees_failures: false   # Critical: failures hidden from hypothesis generation
```

### 6.4 Shadow Set Integrity

The shadow set serves as the autoloop's primary defense against overfitting. Its integrity depends on:

1. **Meta-agent never sees shadow failure details** — only the aggregate "shadow regression detected: yes/no" signal
2. **Shadow cases are frozen** — same cases every run, reproducible comparison
3. **Shadow cases overlap in UC coverage** with visible anchor — so improvements on visible cases should transfer to shadow cases if they are genuine
4. **Periodic rotation** (manual, human-initiated): every ~50 experiments, swap 20% of visible/shadow cases to prevent slow overfitting to the visible partition

---

## 7. Meta-Agent Design

### 7.1 Role

The meta-agent is an LLM (same provider as csagent: DashScope/Kimi) that reads eval failures and proposes a single, small, testable change to one mutable file.

### 7.2 Sanitized Input — What the Meta-Agent Sees

The meta-agent receives **failure taxonomy summaries**, not raw CaseSpec content. This prevents semantic overfitting to specific test cases.

**What the meta-agent sees**:

```json
{
  "baseline_score": {
    "routing_accuracy": 0.82,
    "groundedness_pass_rate": 0.97,
    "escalation_recall": 0.93,
    "containment_rate": 0.45
  },
  "failure_taxonomy": [
    {
      "root_cause_cluster": "ROUTING_SIGNAL",
      "affected_uc": "UC-H",
      "count": 4,
      "failure_tags": ["uc_mismatch", "low_confidence_routing"],
      "representative_symptom": "Bot classified ad removal appeal as generic ad support"
    },
    {
      "root_cause_cluster": "PROMPT_CLARITY",
      "affected_uc": "UC-FP",
      "count": 2,
      "failure_tags": ["stall_after_tool_intent"],
      "representative_symptom": "Bot promised to check moderation reason but did not cite specific reason in response"
    }
  ],
  "recent_experiments": [
    {"id": "exp-003", "hypothesis": "Added explicit instruction for UC-H routing", "result": "keep", "delta": "+3% routing"},
    {"id": "exp-004", "hypothesis": "Lowered drift threshold", "result": "discard", "reason": "escalation_recall regressed"}
  ],
  "current_mutable_files": {
    "system_prompt.txt": "<full content>",
    "routing_prompt.txt": "<full content>",
    "control-policy.yaml": "<full content>",
    "templates.yaml": "<full content>"
  }
}
```

**What the meta-agent does NOT see**:

- Individual case IDs or session IDs
- Raw user messages or seed_messages from CaseSpecs
- Expected tool sequences for specific cases
- Hidden facts from personas
- Shadow set results (only "shadow regression: yes/no")
- Full eval traces or turn-level data

### 7.3 Failure Taxonomy Builder

The `analyzer.py` module converts raw eval reports into sanitized failure taxonomy:

```python
ROOT_CAUSE_CLUSTERS = [
    "PROMPT_CLARITY",       # System prompt lacks specific instruction for this scenario
    "PROMPT_CONFLICT",      # System prompt has contradictory instructions
    "ROUTING_SIGNAL",       # Routing prompt misses or over-weights a classification signal
    "BUDGET_TUNING",        # Control-policy threshold too tight or too loose
    "TEMPLATE_GAP",         # Script template missing for this scenario
    "TEMPLATE_QUALITY",     # Existing template is awkward, incomplete, or misleading
    "EVAL_NOISE",           # Failure appears to be eval noise, not real regression
]

def build_failure_taxonomy(eval_report, interactive_result=None) -> list[FailureCluster]:
    """Aggregate individual failures into root-cause clusters.
    
    Groups by: affected UC, failure type, failure tags.
    Strips: case IDs, session IDs, raw messages, expected sequences.
    Produces: cluster summary with count, tags, and ONE representative symptom sentence.
    """
```

### 7.4 Meta-Agent Prompts

#### `analyze.txt` — Failure Taxonomy Analysis

```text
You are analyzing failures from a customer service bot evaluation run.

The bot handles 12 use cases (UC-A through UC-K plus OUT_OF_SCOPE variants) with a
state machine (INIT→DISCOVER→RESOLVE→CONFIRM→CLOSE/ESCALATE), knowledge retrieval
via pgvector, and fixed script templates for intake UCs.

Current evaluation metrics:
{baseline_metrics_json}

Failure clusters:
{failure_taxonomy_json}

Previous experiments (last 5):
{recent_experiments_json}

For each failure cluster, classify the most likely root cause as ONE of:
- PROMPT_CLARITY: system prompt lacks specific instruction
- PROMPT_CONFLICT: system prompt has contradictory instructions  
- ROUTING_SIGNAL: routing prompt misses or over-weights a signal
- BUDGET_TUNING: control-policy threshold too tight or too loose
- TEMPLATE_GAP: script template missing for scenario
- TEMPLATE_QUALITY: existing template is awkward or incomplete
- EVAL_NOISE: failure appears to be eval noise

Output JSON:
{
  "analysis": [
    {"cluster_id": 0, "root_cause": "ROUTING_SIGNAL", "detail": "...", "confidence": 0.8}
  ],
  "top_root_cause": "ROUTING_SIGNAL",
  "recommended_file": "routing_prompt.txt",
  "previous_experiments_relevant": ["exp-003 tried similar but only partial fix"]
}
```

#### `propose.txt` — Hypothesis Generation

```text
You are proposing ONE small, testable change to improve a customer service bot.

CONSTRAINT — you may ONLY modify these files:
- server/src/main/resources/prompts/system_prompt.txt
- server/src/main/resources/prompts/routing_prompt.txt
- server/src/main/resources/config/control-policy.yaml
- server/src/main/resources/scripts/templates.yaml

Current file contents:
{current_mutable_files}

Failure analysis (from previous step):
{analysis_json}

RULES:
1. Propose exactly ONE change to exactly ONE file.
2. The change must target the top root cause from the analysis.
3. Do NOT reference specific test case IDs, session IDs, or user messages.
4. Do NOT add instructions mentioning "AI", "language model", or similar.
5. Keep changes minimal — prefer adding one sentence over rewriting a paragraph.
6. For control-policy.yaml: you may tighten budgets (lower max) but NEVER loosen
   them beyond: max-clarification-rounds:2, max-faq-miss:2, max-bot-turns-faq:15,
   max-bot-turns-intake:10, max-repeated-same-action:2, max-total-bot-turns:25.
7. For templates.yaml: preserve all {VARIABLE} placeholders exactly as they are.
8. Explain your hypothesis in one sentence.

Output JSON:
{
  "hypothesis": "Adding explicit instruction to distinguish UC-H (appeal) from UC-FP (policy explanation) based on user intent to reinstate",
  "target_file": "routing_prompt.txt",
  "change_type": "add_instruction",
  "diff": "--- a/server/src/main/resources/prompts/routing_prompt.txt\n+++ b/server/src/main/resources/prompts/routing_prompt.txt\n@@ ... @@\n ...",
  "expected_impact": {"metric": "routing_accuracy", "direction": "increase"},
  "risk_assessment": "low — only adds classification signal, does not change action logic"
}
```

---

## 8. Anti-Gaming Detection

### 8.1 Gaming Checks

Each experiment runs through 6 gaming checks before a keep decision:

| # | Check | What It Detects | How |
|---|---|---|---|
| 1 | **Hardcoded content** | Eval case content verbatim in prompt | Grep mutable files for session_ids, case_ids, known eval phrases (sampled from datasets) |
| 2 | **Over-escalation gaming** | Escalate everything → 100% escalation recall | If `escalation_recall` improved but `escalation_precision` dropped >10% |
| 3 | **Containment gaming** | Resolve everything → high containment | If `containment_rate` increased but `wrong_containment_rate` also increased |
| 4 | **Prompt injection in templates** | Templates contain LLM instructions instead of customer-facing text | Regex check for instruction-like patterns in `templates.yaml` values |
| 5 | **Metric whack-a-mole** | One metric improves while another drops | Any safety metric drops >1% while target metric improves |
| 6 | **Risk correlation** | Correlated metrics move in suspicious directions | Composite check (see §8.2) |

### 8.2 Risk Correlation Check

Customer service-specific anti-gaming that checks whether apparently positive metric movements are actually safe:

```python
def check_risk_correlation(baseline: Metrics, candidate: Metrics) -> list[str]:
    """Returns list of gaming flags. Empty = clean."""
    flags = []
    
    # If containment went up, these must not get worse
    if candidate.containment_rate > baseline.containment_rate:
        if candidate.handover_completeness < baseline.handover_completeness:
            flags.append("containment_up_but_handover_quality_down")
        if candidate.escalation_precision < baseline.escalation_precision - 0.05:
            flags.append("containment_up_but_escalation_precision_down")
        if candidate.wrong_containment_rate > baseline.wrong_containment_rate:
            flags.append("containment_up_but_wrong_containment_up")
    
    # If routing accuracy went up, stall rate must not increase
    if candidate.routing_accuracy > baseline.routing_accuracy:
        if candidate.stall_rate > baseline.stall_rate + 0.02:
            flags.append("routing_up_but_stall_rate_up")
    
    # If turn count went down, task_success must not drop
    if candidate.median_resolved_turns < baseline.median_resolved_turns:
        if candidate.task_success_rate < baseline.task_success_rate - 0.02:
            flags.append("faster_but_less_successful")
    
    return flags
```

### 8.3 Gaming Response

Any gaming flag → **DISCARD** the experiment. The flag is logged in `experiments.jsonl` for human review. If the same gaming pattern appears in 3+ consecutive experiments, the loop pauses and alerts.

---

## 9. program.md — Human Optimization Contract

This file is the human-authored "constitution" for the auto-loop. It is **LOCKED** — the meta-agent cannot modify it.

```markdown
# CS Agent Optimization — Program Contract

## Objective
Improve csagent's customer service quality as measured by the existing
eval harness, without introducing safety regressions.

## What "better" means (priority order — lexicographic, not weighted)
1. Zero critical policy violations (hard floor, never trade)
2. No regression in escalation recall or wrong containment (safety)
3. Higher routing accuracy (correct UC identification)
4. Higher groundedness pass rate (answers backed by knowledge)
5. Higher task success rate in interactive eval
6. Lower stall rate in interactive eval
7. Lower median turns for resolved cases (efficiency — last priority)

## Mutable Surface (what you may change)
- server/src/main/resources/prompts/system_prompt.txt
- server/src/main/resources/prompts/routing_prompt.txt
- server/src/main/resources/config/control-policy.yaml
- server/src/main/resources/scripts/templates.yaml

## Constraints
- ONE change per experiment, to ONE file
- Changes must be explainable in one sentence
- Never reference eval case IDs, session IDs, or test data in prompts
- Never add model self-reference ("As a large language model...")
- Never weaken escalation or safety thresholds
- Budget thresholds: may tighten, NEVER loosen beyond original values
- Template changes: must preserve all {VARIABLE} placeholders
- Never remove existing safety-related instructions (additive changes preferred)

## Forbidden
- Hardcoding answers to known test cases
- Removing or weakening safety instructions
- Changing files outside the mutable surface
- Any change that reduces escalation_recall, even if other metrics improve
- Gaming: artificially boosting one metric at the expense of correlated safety metrics
```

---

## 10. Experiment Lifecycle

### 10.1 Single Experiment Flow

```text
┌─ Experiment exp-{N} ──────────────────────────────────────────────┐
│                                                                    │
│  1. CREATE BRANCH                                                  │
│     git checkout -b autoloop/exp-{N}                               │
│                                                                    │
│  2. HYPOTHESIZE                                                    │
│     meta_agent.analyze(eval_report, experiment_log) → taxonomy     │
│     meta_agent.propose(taxonomy, current_files) → hypothesis+diff  │
│                                                                    │
│  3. VALIDATE                                                       │
│     surface_validator.check(diff) → pass/fail                      │
│     content_validator.check(diff) → pass/fail                      │
│     If fail → DISCARD (log: "surface_violation")                   │
│                                                                    │
│  4. APPLY + BUILD                                                  │
│     Apply diff to mutable file                                     │
│     git add + commit on autoloop/exp-{N}                           │
│     mvn compile -pl server (incremental, ~15s)                     │
│                                                                    │
│  5. PHASE A — Replay Gate                                          │
│     Start server on :8081                                          │
│     Wait for /internal/health → UP                                 │
│     Run: mvn -f eval/pom.xml spring-boot:run                      │
│          -Deval.bot-base-url=http://localhost:8081                  │
│          -Deval.suite=smoke                                        │
│     Parse eval JSON report                                         │
│     Check: Layer 0 (hard gates) → pass/fail                       │
│     Check: Layer 1 (safety non-regression) → pass/fail            │
│     Check: gaming detector → pass/fail                             │
│     If fail → DISCARD (log reason) → skip Phase B                 │
│                                                                    │
│  6. PHASE B — Interactive Confirmation                             │
│     Run: python -m eval_interactive run                            │
│          --cases autoloop/visible_anchor.txt                       │
│          --bot-url http://localhost:8081                            │
│          --label autoloop-exp-{N}-visible                          │
│          --simulator-temperature 0.0                               │
│     Check: Layer 2 (interactive L1 non-regression) → pass/fail    │
│     Check: Layer 3 (correctness improvement >= 2%) → pass/fail    │
│                                                                    │
│     Run: python -m eval_interactive run                            │
│          --cases autoloop/shadow.txt                               │
│          --bot-url http://localhost:8081                            │
│          --label autoloop-exp-{N}-shadow                           │
│          --simulator-temperature 0.0                               │
│     Check: shadow regression (>3% drop) → pass/fail               │
│                                                                    │
│  7. DECISION                                                       │
│     All checks pass → KEEP                                         │
│       Tag branch: autoloop/keep-{N}                                │
│       Compute composite rank score                                 │
│       Log: keep + score + hypothesis + diff                        │
│     Any check fail → DISCARD                                       │
│       Delete branch                                                │
│       Log: discard + reason + hypothesis + diff                    │
│                                                                    │
│  8. CLEANUP                                                        │
│     Kill server on :8081                                           │
│     Checkout original branch                                       │
│                                                                    │
│  9. APPEND to experiments.jsonl                                    │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### 10.2 Experiment Log Schema

```json
{
  "experiment_id": "exp-007",
  "timestamp": "2026-04-26T02:14:30Z",
  "branch": "autoloop/exp-007",
  "hypothesis": "Added instruction to distinguish UC-H from UC-FP based on reinstatement intent",
  "target_file": "routing_prompt.txt",
  "change_type": "add_instruction",
  "diff_summary": "+2 lines in routing_prompt.txt",
  "phase_a": {
    "hard_gates_passed": true,
    "safety_non_regression": true,
    "gaming_flags": [],
    "metrics": {
      "routing_accuracy": 0.85,
      "groundedness_pass_rate": 0.98,
      "escalation_recall": 0.96,
      "containment_rate": 0.48
    }
  },
  "phase_b": {
    "visible_anchor": {
      "task_success_rate": 0.75,
      "stall_rate": 0.05,
      "l1_hard_checks_pass_rate": 1.0
    },
    "shadow": {
      "composite_score": 0.72,
      "regression_detected": false
    },
    "improvement_detected": true,
    "improvement_metric": "routing_accuracy",
    "improvement_delta": 0.03
  },
  "decision": "keep",
  "composite_rank_score": 0.78,
  "duration_seconds": 1585
}
```

---

## 11. Execution Modes

| Mode | Command | Experiments | Eval Suite | Duration | Use Case |
|---|---|---|---|---|---|
| **Check** | `python -m autoloop check` | 0 | — | ~5s | Verify prerequisites (DB, server buildable, eval runnable) |
| **Dry-run** | `python -m autoloop run --experiments 1 --dry-run` | 1 | — | ~1 min | Test hypothesis generation without building/running |
| **Single** | `python -m autoloop run --experiments 1` | 1 | smoke + interactive | ~27 min | Test the full loop end-to-end |
| **Night** | `python -m autoloop run --experiments 10` | 10 | smoke + interactive | ~4.5 hours | Overnight improvement run |
| **Deep night** | `python -m autoloop run --experiments 20` | 20 | smoke + interactive | ~9 hours | Extended overnight run |
| **Replay-only** | `python -m autoloop run --experiments 20 --phase-a-only` | 20 | smoke only | ~2.5 hours | Fast screening without interactive eval |
| **Report** | `python -m autoloop report` | — | — | instant | View experiment log summary |
| **Apply** | `python -m autoloop apply --experiment exp-007` | — | — | instant | Cherry-pick a kept experiment to current branch |
| **Audit** | `python -m autoloop audit --experiment exp-007` | — | — | instant | View full diff + scores + gaming checks for one experiment |

---

## 12. File Structure

```
autoloop/                                    # NEW: Standalone autonomous improvement loop
├── program.md                               # Human-authored optimization contract (LOCKED)
├── pyproject.toml                           # Python 3.11+: click, httpx, pyyaml, openai
├── config.yaml                              # Loop configuration
├── autoloop/
│   ├── __init__.py
│   ├── cli.py                               # click CLI: check, run, report, apply, audit
│   ├── loop.py                              # Main loop orchestrator
│   ├── experiment.py                        # Experiment data model + JSONL logger
│   ├── meta_agent/
│   │   ├── __init__.py
│   │   ├── analyzer.py                      # Eval failures → sanitized failure taxonomy
│   │   ├── proposer.py                      # Failure taxonomy → ONE change hypothesis
│   │   └── prompts/
│   │       ├── analyze.txt                  # Failure analysis prompt template
│   │       └── propose.txt                  # Change proposal prompt template
│   ├── sandbox/
│   │   ├── __init__.py
│   │   ├── surface.py                       # Mutable surface allow-list + validation
│   │   ├── content_validator.py             # Content-level checks (budgets, placeholders, etc.)
│   │   ├── applier.py                       # Apply proposed diff to files
│   │   └── server.py                        # Start/stop/health-check server on custom port
│   ├── scoring/
│   │   ├── __init__.py
│   │   ├── eval_runner.py                   # Invoke Java eval via mvn subprocess
│   │   ├── interactive_runner.py            # Invoke Python interactive eval via subprocess
│   │   ├── report_parser.py                 # Parse eval JSON report → metrics dict
│   │   ├── lexicographic.py                 # Layered constraint evaluation (L0-L4)
│   │   ├── composite.py                     # Rank score for kept candidates
│   │   └── gaming.py                        # 6 anti-gaming checks + risk correlation
│   └── report.py                            # Summary HTML report generator
├── visible_anchor.txt                       # List of CaseSpec paths for visible anchor set
├── shadow.txt                               # List of CaseSpec paths for shadow set (LOCKED)
├── results/
│   ├── experiments.jsonl                    # Append-only experiment log
│   └── runs/                                # Per-run artifacts (eval reports, diffs)
│       └── exp-001/
│           ├── hypothesis.json
│           ├── diff.patch
│           ├── eval_report.json
│           ├── interactive_visible.json
│           ├── interactive_shadow.json
│           └── decision.json
└── tests/
    ├── test_surface.py                      # Mutable surface validation tests
    ├── test_content_validator.py             # Content-level check tests
    ├── test_lexicographic.py                # Scoring layer tests
    ├── test_gaming.py                       # Anti-gaming check tests
    └── test_loop_dry_run.py                 # End-to-end dry run test
```

---

## 13. Configuration

```yaml
# autoloop/config.yaml

# ── Server ──
server:
  project_root: ..                           # Relative to autoloop/
  port: 8081                                 # Experiment server port (not 8080)
  health_endpoint: /internal/health
  startup_timeout_seconds: 60
  build_command: "mvn compile -pl server -q"
  start_command: "mvn spring-boot:run -pl server -Dspring-boot.run.profiles=local -Dserver.port={port}"

# ── LLM (for meta-agent) ──
llm:
  base_url: ${DASHSCOPE_BASE_URL}
  api_key: ${DASHSCOPE_API_KEY}
  model: ${DASHSCOPE_CHAT_MODEL}
  temperature: 0.3                           # Slightly creative for hypothesis generation
  max_tokens: 2000

# ── Eval ──
eval:
  replay:
    command: "mvn spring-boot:run -f eval/pom.xml -Deval.bot-base-url=http://localhost:{port} -Deval.suite=smoke"
    report_path: "eval/target/eval-reports/eval-report.json"
    timeout_seconds: 600
  interactive:
    command: "python -m eval_interactive run --bot-url http://localhost:{port} --simulator-temperature 0.0 --label {label}"
    timeout_seconds: 1200

# ── Case Sets ──
case_sets:
  visible_anchor:
    list_file: visible_anchor.txt            # One CaseSpec path per line
    max_cases: 20
  shadow:
    list_file: shadow.txt
    max_cases: 15
    meta_agent_sees_failures: false

# ── Scoring ──
scoring:
  minimum_improvement_threshold: 0.02        # 2% absolute improvement required on at least one metric
  shadow_regression_threshold: 0.03          # >3% drop on shadow → discard
  safety_zero_tolerance:                     # These must NEVER regress
    - escalation_recall
    - wrong_containment_rate
    - critical_high_risk_escalation_rate

# ── Gaming ──
gaming:
  escalation_precision_drop_threshold: 0.10  # >10% precision drop with recall improvement → gaming
  metric_regression_threshold: 0.01          # Any safety metric drops >1% → gaming flag
  consecutive_gaming_pause: 3                # Pause loop after 3 consecutive gaming flags

# ── Loop ──
loop:
  max_experiments: 20
  pause_on_consecutive_discards: 5           # Pause if 5 in a row fail (likely stuck)
  experiment_history_window: 10              # Show last N experiments to meta-agent
  git_branch_prefix: "autoloop/"
  cleanup_discarded_branches: true           # Delete branches for discarded experiments
```

---

## 14. Makefile Integration

```makefile
# Add to existing Makefile:

# ── Autoloop ──
autoloop-check:
	cd autoloop && python -m autoloop check

autoloop-dry:
	cd autoloop && python -m autoloop run --experiments 1 --dry-run

autoloop-single:
	cd autoloop && python -m autoloop run --experiments 1

autoloop-night:
	cd autoloop && python -m autoloop run --experiments 10

autoloop-deep:
	cd autoloop && python -m autoloop run --experiments 20

autoloop-replay-only:
	cd autoloop && python -m autoloop run --experiments 20 --phase-a-only

autoloop-report:
	cd autoloop && python -m autoloop report

autoloop-apply:
	cd autoloop && python -m autoloop apply --experiment $(EXP)

autoloop-audit:
	cd autoloop && python -m autoloop audit --experiment $(EXP)
```

---

## 15. Implementation Plan

| Step | What | Effort | Output |
|---|---|---|---|
| **1** | `program.md` + `config.yaml` + `surface.py` + `content_validator.py` | 1 day | Mutable surface definition, validation, content checks |
| **2** | `report_parser.py` + `lexicographic.py` + `composite.py` | 1 day | Parse eval reports → layered scoring |
| **3** | `server.py` (start/stop on custom port, health check) | 0.5 day | Server lifecycle management |
| **4** | `eval_runner.py` + `interactive_runner.py` (subprocess invocation) | 0.5 day | Eval harness invocation |
| **5** | `analyzer.py` + `proposer.py` + prompt templates | 1.5 days | Meta-agent hypothesis generation with sanitized inputs |
| **6** | `applier.py` (apply diff, validate, git commit) | 0.5 day | File modification + git management |
| **7** | `gaming.py` (6 checks + risk correlation) | 0.5 day | Anti-gaming detection |
| **8** | `loop.py` + `cli.py` (orchestration + two-phase protocol) | 1.5 days | Main loop with Phase A/B |
| **9** | `experiment.py` + `report.py` (JSONL log + HTML summary) | 0.5 day | Experiment logging and reporting |
| **10** | Case set configuration (visible_anchor.txt, shadow.txt) | 0.5 day | Case set selection from existing CaseSpecs |
| **11** | Tests + Makefile integration + end-to-end validation | 1 day | Verification |

**Total: ~9 days. Critical path: ~6 days (steps 1→2→4→5→8→11).**

---

## 16. Done Criteria

### Loop Operational

- [ ] `python -m autoloop check` passes all prerequisites (DB accessible, server buildable, eval runnable)
- [ ] `python -m autoloop run --experiments 1 --dry-run` generates valid hypothesis without building/running
- [ ] `python -m autoloop run --experiments 1` completes full two-phase protocol end-to-end
- [ ] Phase A correctly discards experiments that fail hard gates
- [ ] Phase B correctly discards experiments with interactive L1 regression
- [ ] Shadow eval correctly flags when shadow score drops >3%
- [ ] Gaming detector correctly flags synthetic over-escalation experiment
- [ ] Surface validator correctly rejects diffs that modify locked files
- [ ] Content validator correctly rejects loosened budget thresholds
- [ ] `experiments.jsonl` contains complete audit trail for all experiments
- [ ] `python -m autoloop report` produces readable HTML summary
- [ ] `python -m autoloop apply --experiment <id>` cherry-picks to current branch

### Safety Verified

- [ ] No experiment that passes all checks has a safety metric regression
- [ ] Meta-agent never sees shadow set failure details (verify via logged inputs)
- [ ] Meta-agent never sees raw case IDs or session IDs (verify via logged inputs)
- [ ] All mutable file modifications are within the 4-file allow-list
- [ ] Budget values in control-policy.yaml are never loosened beyond originals
- [ ] Existing system unaffected: `mvn spring-boot:run` on :8080 works independently
- [ ] Existing eval unaffected: `mvn verify -Peval-smoke` continues to pass without autoloop

### Night Run Validated

- [ ] 10-experiment overnight run completes within 5 hours
- [ ] At least 1 experiment in the run is kept (demonstrates the loop can find improvements)
- [ ] At least 1 experiment is correctly discarded (demonstrates the loop can reject bad changes)
- [ ] No consecutive-gaming-pause triggered on clean run
- [ ] Results are human-reviewable the next morning via `autoloop report`

---

## 17. Design Decisions Log

| Decision | Chosen | Rejected | Rationale |
|---|---|---|---|
| Architecture | Standalone `autoloop/` CLI | Embedded in `eval_interactive/` | Optimizer conceptually independent from evaluator; true isolation when not triggered |
| Eval invocation | Subprocess (mvn/python -m) | Python import of eval_interactive modules | Avoids coupling; eval harness integrity preserved; no shared Python state |
| Scoring model | Lexicographic constraints | Flat weighted sum | Safety metrics are non-negotiable floors, not tradeable weights |
| Variance control | temperature=0 + fixed seeds + min threshold | 3x repeated runs | 3x repetition costs ~45min/experiment; temp=0 achieves determinism in 1 run |
| Keep/discard | Two-phase (replay gate → interactive confirm) | Single-phase | Replay is fast+deterministic; filters bad candidates before expensive interactive eval |
| Mutable surface V1 | 4 files (prompts + control-policy + templates) | Include tool-policy.yaml | tool-policy controls capability boundaries; too risky for automated changes in V1 |
| Meta-agent input | Sanitized failure taxonomy | Raw CaseSpec + eval traces | Prevents semantic overfitting to specific test cases |
| Anti-gaming | 6 checks + risk correlation | Simple metric comparison | Customer service requires correlated-metric safety (containment↑ + wrong_containment↑ = gaming) |
| Server isolation | Separate port (:8081) | Git worktree with separate build | Worktree adds complexity; shared DB is fine since knowledge index is read-only |
| Case set management | Config files referencing existing CaseSpec dirs | Renamed optimizer_* directories | Avoids restructuring existing case_specs/ layout |

---

## Appendix A: Relationship to Existing Components

```text
autoloop/          eval/              eval_interactive/    server/
(NEW)              (EXISTING)         (EXISTING)           (EXISTING)
                                                           
Invokes via ───►   mvn eval-smoke     python -m eval_      mvn spring-boot:run
subprocess         (parses JSON       interactive run       on :8081
                    report)           (parses results)      (under test)
                                                           
Never imports      Never modified     Never modified        Files modified:
Never modifies     by autoloop        by autoloop           only 4 MUTABLE
                                                            files
```

## Appendix B: Karpathy Loop Mapping

| Karpathy autoresearch | csagent autoloop | Notes |
|---|---|---|
| `prepare.py` (locked) | `eval/` + `eval_interactive/` + `data/` (locked) | Evaluation is the immutable foundation |
| `train.py` (mutable) | 4 mutable files (prompts, config, templates) | Narrower surface than Karpathy's single-file approach |
| `program.md` (human contract) | `autoloop/program.md` (human contract) | Same concept — human defines "better" and constraints |
| `val_bpb` (single metric) | Lexicographic layers → composite rank | Customer service needs layered safety, not single metric |
| 5-min training run | ~27 min experiment (build + Phase A + Phase B) | Longer but still feasible for overnight runs |
| git keep/discard | git branch keep/delete | Same mechanism |
| — | Shadow eval + anti-gaming | Added: Karpathy's original didn't need these for training code |
