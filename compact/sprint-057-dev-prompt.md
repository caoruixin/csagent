# Dev prompt — Sprint 57 / M-Auto-1A S-Auto-4 — Anti-hardcode kernel + content validator + gaming checks

你是 **dev agent for Sprint 57 / M-Auto-1A S-Auto-4**。本次任务：把 S-Auto-3 ships 的 `anti_hardcode_check` placeholder hook 换成 real deterministic regex+heuristic structural detector（NO 第二个 LLM；NO 新 heavy dep），ships `content_validator.py`（placeholder/length/deny-list structural validity），ships `scoring/gaming.py`（6 anti-gaming checks from `docs/proposals/autoloop_design.md` §8 + NEW `tier2_measurement_contract_change_attempt`，all observation-only in v1），wires all three into `loop.py` 的正确 stage 边界（**no signature change** to S-Auto-1/2/3 deliverables；只在 pre-sandbox + post-eval 加两个 insertion points + `loop.py` IterationResult 加 additive optional `gaming_flags` field）+ `program.md` §4 row 3 status flip。**§7 REQUIRED** (`eval_spec`)。**Codex PER-SUB-SPRINT REQUIRED per §4.3 trigger #2** —— the kernel IS §1.7 structural guard, Codex 必须在 M-Auto-1A milestone close 前独立验证 detector 没有 design hole；dev 在 sub-sprint close 时 author handoff + 让 deliver-agent dispatch Codex（dev 不直接 dispatch）。

**关键 D-clauses (human-locked 2026-05-27; 不可违反)**：
- **D1 — Detector mechanism**: regex + heuristic ONLY (stdlib `re` + `unicodedata` + 小 synonym map)。NO 第二个 LLM in propose-stage。`FLAG_FOR_CODEX` 是人工/Codex review 信号，**不在 propose-stage 自动升级到 FAIL，也不在 propose-stage 自动降级到 PASS**。
- **D2 — Detector pattern scope**: 只 encode generic structural patterns（IF/THEN/ELSE shapes、`cs<id>`-like tokens、`session_id`-like tokens、MUST/NEVER/Tier-0-like phrasing、`.contains(...)` / `.matches(...)` 字面）。**禁止** hardcode 具体 eval case wording、具体 user utterances、具体 expected assistant answers、known case success/failure labels。Detector self-discipline regression test 必须 grep rule source 确认 zero literal eval-case-id (`cs011` / `cs015` 等) 出现。
- **D3 — gaming.py 输入契约**:
  - `scoring_code_drift`: 若 `config.fitness.scoring_code_baseline_sha` 缺失/None → emit `WARN rule_id=scoring_code_drift.baseline_missing`；**禁止** 猜测 / silently hash-and-use-as-baseline / auto-pass。
  - `shadow_set_leakage`: detector **禁止** 直接读取 `eval_interactive/case_specs_shadow/` 内容；leak signatures **只能** 来自 `config.gaming.shadow_leak_signatures` (out-of-band human-approved config)。

## Read order (最小化)

只读两个：

1. `AGENTS.md`（auto-loaded via constitution chain）
2. 本 prompt（self-contained sub-sprint contract，下方完整内嵌）

需要查看具体 code anchors 时引用：
- `autoloop/autoloop/sandbox/anti_hardcode_check.py` (S-Auto-3 placeholder; 本 sprint swap body — signature 不动；`AntiHardcodeResult.placeholder` 字段保留但本 sprint 一律 False)
- `autoloop/autoloop/sandbox/yaml_diff_validator.py` (S-Auto-1) — sandbox API；content_validator 插在它之前
- `autoloop/autoloop/loop.py` (S-Auto-3) — 本 sprint 加两个 wiring insertion points (pre-sandbox content_validator + post-eval gaming.detect)；IterationResult dataclass 加 additive optional `gaming_flags: list[dict] = field(default_factory=list)`
- `autoloop/autoloop/cli.py audit` (S-Auto-3) — 本 sprint 扩展 audit render 输出 `gaming_flags` + `anti_hardcode_flag_for_codex` (默认 shadow firewall 仍 RESPECTED; `--include-shadow-detail` flag 行为不变)
- `autoloop/tests/test_loop.py::test_adversarial_fixture_passes_placeholder_anti_hardcode` (S-Auto-3) — 本 sprint **rename + assertion flip** 为 `test_adversarial_fixture_fails_real_detection`，断言 `decision=="discard"` + `discard_reason.startswith("anti_hardcode_rejected:")`
- `autoloop/tests/test_meta_agent.py` 中关于 placeholder=True 的断言 — 本 sprint 更新为 placeholder=False
- `autoloop/config.yaml` — 本 sprint append `content_validator:` + `anti_hardcode:` + `gaming:` 3 个 block + `fitness.scoring_code_baseline_sha: null` (deliver-agent 在 close 时填实际 SHA)
- `autoloop/program.md` §4 row 3 — 本 sprint 单行 status flip (DEFERRED → DELIVERED)
- `autoloop/README.md` audit row — 本 sprint 单行 description 补充 gaming_flags + anti_hardcode_flag_for_codex
- `docs/proposals/autoloop_design.md` §8 — 6 anti-gaming checks 的规范来源（dev 读这一节定 detection rule 形状；本 prompt 已 enumerated 7 个 check 的 id 和 severity，直接按 prompt 实现即可）

## Embedded sub-sprint contract

### Class

`eval_spec` (§3.2 Q6 boundary — kernel 是 propose-stage 结构性 §1.7 defence；不改 runtime / projection / scoring / CaseSpec；它是 §1.7 enforcement chain 的最后一环，S-Auto-1 sandbox white-list + S-Auto-2 lexicographic gate + shadow firewall + S-Auto-3 loop / memory / applier 之后的封口)。**§7 REQUIRED**。

### Goal

S-Auto-4 close 时：

1. **Real anti-hardcode detector replaces S-Auto-3 placeholder**: hook signature `def anti_hardcode_check(hypothesis, *, config) -> AntiHardcodeResult` **verbatim preserved**；S-Auto-4 只换 body。S-Auto-3 的 `test_adversarial_fixture_passes_placeholder_anti_hardcode` rename + flip 为 `test_adversarial_fixture_fails_real_detection` —— 这是 placeholder → real transition 的 load-bearing regression target。
2. **Content validator at propose-stage pre-sandbox**: NEW `autoloop/autoloop/sandbox/content_validator.py`；FAIL 时 iteration discards with `discard_reason=content_validator_rejected:<rule_id>` BEFORE sandbox.validate_skill_yaml_diff。
3. **Anti-gaming checks at post-eval**: NEW `autoloop/autoloop/scoring/gaming.py`；7 checks 全 OBSERVATION-ONLY in v1 (flags persisted + surfaced by `audit`，NOT auto-discard)；ERROR-severity 改为 gating 留给 M-Auto-1B 评估。
4. **`autoloop/program.md` §4 row 3** flip DEFERRED → DELIVERED (deterministic regex+heuristic detector covering Q1/Q2/Q4/Q5 of §4.1 kernel; PASS / FAIL / FLAG_FOR_CODEX verdicts; FLAG_FOR_CODEX 在 v1 observation-only)。
5. **Per-sub-sprint Codex `pass`** at S-Auto-4 close BEFORE M-Auto-1A milestone close。Codex prompt 在 S-Auto-4 close 时 deliver-agent author（**dev 不 dispatch Codex**；dev 在 handoff 中 surface Codex 需要的 calibration table 实际结果 + bypass spot-check fixtures available）。

**Zero touch** to S-Auto-1/2/3 deliverable signatures (`anti_hardcode_check` body swap but signature 不动；`loop.py` 加两个 wiring insertion 但 signature 不动；`IterationResult` 加 additive optional `gaming_flags` field with default factory，向后兼容 S-Auto-3 tests；`cli.py audit` 扩展输出 surface 但 signature 不动；`eval_runner.py` / `tier_evaluator.py` / `baseline_loader.py` / `sandbox/yaml_diff_validator.py` / `meta_agent/*` UNTOUCHED)。**Zero touch** to `eval_interactive/eval_interactive/**`、case_spec、case_specs_shadow、`server/`、`eval/` Java、`data/`、`db/`、`server/src/main/resources/`、`docs/foundational/`、`docs/runtime_freeze_and_risk_policy.md`、`docs/current/`、`docs/sprints/*` archives、`docs/milestones/*` archives、`docs/codex-findings.md`。

### Scope (#1-#9 编号；这是 contract)

#### #1 — `autoloop/autoloop/sandbox/anti_hardcode_check.py` placeholder → real detector

Swap S-Auto-3 placeholder body for deterministic regex + heuristic detector (NO LLM；NO 新 heavy dep；stdlib `re` + `unicodedata` + 小 synonym map only)。覆盖 §4.1 九问 kernel 的 auto-tractable 子集：

**Q1 — Semantic hardcode patterns** (FAIL on high-confidence; FLAG_FOR_CODEX on borderline):
- `IF/WHEN ... THEN/ELSE` decision trees in soft narrative（识别 normalized-case + punctuation + `→` / `->` arrow tokens）
- 枚举 keyword lists ≥3 items in OR-form（如 `"appeal" OR "refund" OR "compensation"`）
- Regex-style pattern enumeration in prose（`.contains(...)`, `.matches(...)`）
- Multi-line decomposition of the above（kernel 必须能识别 IF-THEN 跨 newline / bullet 拆分的情况）

**Q2 — Tier-0 invariant invention attempts** (FAIL):
- Language asserting new MUST-NEVER / Tier-0-equivalent semantics absent current Tier-0（如 "MUST always escalate when X"、"Runtime MUST reject any message containing Y"）
- Detector 结构性识别 Tier-0-like phrasing；**不**读取 `docs/runtime_freeze_and_risk_policy.md`（无 cross-doc dependency in propose-stage）

**Q4 — Eval-phrase / case_id / session_id references** (FAIL):
- 显式 `cs<NNN>` / `cs[a-z0-9]+` substring tokens（正则 `\bcs[0-9a-z_]{2,}\b`）
- 显式 `session_id=...` / `case_id=...` / `iteration_id=...` substring tokens
- **NOT in v1 scope**: 近似引用 known eval CaseSpec phrasings —— 因为这会 hardcode specific eval wording (violates D2)

**Q5 — LLM-ownership-shrinking language** (FAIL on high-confidence; FLAG_FOR_CODEX on borderline):
- "MUST execute X"、"force the assistant to"、"the bot MUST always answer with"、"do not consider Y"（Y 是 §1.3 LLM-owned soft-semantic dimension）
- 单 "MUST" with non-Tier-0 subject → FLAG_FOR_CODEX（不 auto-FAIL —— Codex/human 判断）

##### Detector pattern scope (D2 — 不可违反)

**允许** (detector MAY encode as generic structural patterns):
- IF/WHEN/THEN/ELSE decision-tree shapes (case-insensitive; normalized)
- `cs<id>`-like tokens (regex `\bcs[0-9a-z_]{2,}\b`)
- `session_id`-like / `case_id`-like / `iteration_id`-like tokens
- MUST/NEVER/Tier-0-like phrasing structures
- `.contains(...)` / `.matches(...)` / 显式 regex literal shapes

**禁止** (detector MUST NOT hardcode any of these):
- 具体 eval case wording (e.g., 字面 cs011 user turn phrasing)
- 具体 user utterances (e.g., "my account is locked" as banned phrase)
- 具体 expected assistant answers
- known case success/failure labels (e.g., `closure_criterion` text fragments)

Detector 测量 **generic structural shape**，不测量 specific content。若某 rule 必须依赖具体 eval phrase 才能 fire，该 rule 结构性错误，必须 redesign。

##### API (signature verbatim from S-Auto-3)

```python
from dataclasses import dataclass
from typing import Literal

@dataclass
class AntiHardcodeResult:
    verdict: Literal["PASS", "FAIL", "FLAG_FOR_CODEX"]
    rule_id: str | None              # e.g. "Q1.if_then_decision_tree"
    matched_substring: str | None    # offending excerpt for audit (≤200 chars; redact user-content-like)
    placeholder: bool = False        # S-Auto-4: 一律 False; S-Auto-3 placeholder set True

def anti_hardcode_check(hypothesis, *, config) -> AntiHardcodeResult: ...
```

##### Normalization pipeline (applied to `hypothesis.after_value` before each rule fires)

1. NFKC unicode normalize
2. Lowercase
3. Whitespace collapse (`\s+` → ` `)
4. Optional 小 synonym map for high-frequency obfuscation（如 `equal` → `=`, `equals` → `=`；configurable via `config.anti_hardcode.synonym_map_enabled`，默认 false；deliver-agent 在 calibration evidence 后才打开）

每个 rule 返回自己的 verdict；final verdict 取最高 severity (FAIL > FLAG_FOR_CODEX > PASS)。当 FAIL，`rule_id` 是第一个 fire 的 rule（deterministic；rule 评估顺序按 `rule_id` 升序 fixed）。

##### Detector self-discipline (defender 不得成为 hardcode pump)

Rule set 必须 encode generic structural patterns ONLY（per D2 above）。Detector self-discipline regression test (`test_anti_hardcode_check.py` 最终 fixture) 必须 grep rule source 确认 zero literal eval-case-id / user-utterance / expected-answer literals 出现 —— 若 Codex 找到一条 rule 的 match 依赖 specific eval phrase / user utterance / expected answer / case-status label，该 rule 被 REJECTED。

#### #2 — `autoloop/autoloop/sandbox/content_validator.py` (NEW)

Lightweight 结构性 validity checker，**在 sandbox YAML diff validation 之前** 跑（content-broken hypothesis 比 sandbox-rejected 更早 discard 更省）。

##### Placeholder syntax integrity (D3 — 不可违反)

检测 **`before_value` 里已经存在** 的 placeholder-like tokens 是否在 `after_value` 中保持完整。Tokens 覆盖：
- `{TOKEN}` / `{TOKEN_NAME}` (curly-brace template tokens；prompt 模板常见)
- `${TOKEN}` / `${TOKEN_NAME}` (dollar-prefix template tokens)
- `<TOKEN>` / `<TOKEN_NAME>` (angle-bracket internal markers) **IF already present in `before_value`**（如 Skill YAML 字段用这些作 internal markers）

对每个 in-before placeholder shape，validator 检查同名 token 在 `after_value` 中完整存在（无 missing close-brace、无 rename、无 accidental deletion）。如果 `before_value` 中 没有此类 token，跳过检查（不在 plain prose edit 上产生 false positive）。

**禁止** 引入 Salesforce-specific semantic validation、currency validation、locale validation 等，**除非** 在 `autoloop/config.yaml` `content_validator.custom_token_shapes: [...]` 提供 concrete token shapes。v1 ships with the 3 generic shapes above only。

##### Length sanity

- `len(after_value.strip()) == 0` → FAIL (`rule_id=content_validator.zero_length`)
- `len(after_value) > 5 * len(before_value)` → FAIL (`rule_id=content_validator.length_overflow`) —— 拦 meta-agent rambling expansion
- `len(after_value) < 0.1 * len(before_value)` AND `len(before_value) > 50` → FAIL (`rule_id=content_validator.length_underflow`) —— 拦 substantial 字段的近似 deletion。`0.1` ratio + `50` floor 都 config-tunable（`content_validator.length_underflow_ratio` / `length_underflow_min_before`）；v1 默认 `0.1` + `50`

##### Forbidden-token deny-list

小 seeded list from §1.7 forbidden examples（如 `<<SYSTEM>>` / `<<USER>>` known prompt internal markers）。Configurable via `autoloop/config.yaml` `content_validator.deny_list: [...]`。v1 ships with 故意小 seed (≤10 tokens)；扩展是 separate sprint 决策。

##### API

```python
@dataclass
class ContentValidationResult:
    verdict: Literal["PASS", "FAIL"]
    rule_id: str | None
    detail: str | None

def validate_content(hypothesis) -> ContentValidationResult: ...
```

FAIL → iteration discards with `discard_reason=content_validator_rejected:<rule_id>` BEFORE sandbox YAML diff validate。

#### #3 — `autoloop/autoloop/scoring/gaming.py` (NEW)

Post-eval anti-gaming checks。**全部 OBSERVATION-ONLY in v1** —— 产生 `GamingFlag` records (WARN/ERROR severity)、persist 到 `experiments_log.gaming_flags`、在 `audit` surface，但 **不改变 `tier_evaluator` 的 keep/discard verdict**。ERROR-severity 改为 gating 留给 M-Auto-1B post-overnight 校准。

Port `docs/proposals/autoloop_design.md` §8 的 6 个 check + 加 NEW `tier2_measurement_contract_change_attempt`：

1. **`anomalous_metric_movement`**: `LexicographicVerdict` Layer 3 improvement 与 diff 的 size/scope 不成比例。Heuristic: `improvement_count / max(1, len(diff_chars))` > threshold (default 0.05；configurable via `config.gaming.anomalous_metric_movement_threshold`)。**WARN** severity。

2. **`identical_eval_traces_across_different_hypotheses`**: 两个 iteration 不同 `hypothesis_fingerprint` 产生 byte-identical `results.json`（across all 3 v1 suites）。**ERROR** severity（暗示 eval 没真正 rerun）。Detector 读 `iterations_index.query_recent(n=K)` + per-suite results files 的 hash；当 ≥2 distinct fingerprints 共享 results hash 时 flag。

3. **`suspect_baseline_manipulation`**: 当前 `config.fitness.baseline_dir` value 与上次 `main`-branch commit (或当前 overnight batch iteration 1) 的 value 不同，且变化未记录为 human action in `iterations_index.notes`。**WARN** severity。Detector 读 current config + `git log --follow autoloop/config.yaml` for last `main` SHA + `git show <sha>:autoloop/config.yaml` 提取历史值。若 `git` lookup 因任何原因失败，emit `WARN rule_id=suspect_baseline_manipulation.git_lookup_failed` (don't auto-pass)。

4. **`eval_time_gaming_via_timeout_skip`**: 当前 eval suite 的 skipped / timed-out cases 率 >2× baseline 率。**WARN** severity。Detector 读 `results.json` 每个 case 的 `terminal_outcome` field (skipped / timeout / error counts)。

5. **`scoring_code_drift`** (D3 — 不可违反): SHA of `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` 与 `config.fitness.scoring_code_baseline_sha` (NEW config field；deliver-agent 在 S-Auto-4 close 时填实际 M-Auto-1A close commit SHA) 不同。
   - 若 `config.fitness.scoring_code_baseline_sha` **缺失/None**: emit **WARN** with `rule_id=scoring_code_drift.baseline_missing` and `detail="scoring_code_baseline_sha not set in config; cannot verify drift"`。**禁止** guess、**禁止** auto-pass、**禁止** 计算 hash 后 silently 用作 baseline
   - 若 baseline SHA set 但 current SHA 不同: emit **ERROR** with `rule_id=scoring_code_drift.sha_changed`

6. **`shadow_set_leakage`** (D3 — 不可违反): 在任何 non-audit surface (proposer prompt context、`experiments_log` non-firewall fields、`iterations_index` rows) 检测 shadow per-case info 出现。Implementation:
   - Detector 扫 iteration_record + recent K iterations + the proposer prompt (if logged) 寻找 **redacted leak-signature patterns** seeded from `config.gaming.shadow_leak_signatures: [...]`
   - v1 leak signatures: `["cs59s", "shadow_case_id", "shadow/", "case_specs_shadow"]` (configurable；只从 human-approved config 来源)
   - **Detector 禁止 直接读取 `eval_interactive/case_specs_shadow/` 内容** —— 不 load 任何 shadow case_spec file、不读 shadow 目录列表里的 case id、不依赖 shadow corpus content。Human 要新增 leak signature 时改 config (out-of-band；显式 approval)
   - **ERROR** severity on match

7. **NEW `tier2_measurement_contract_change_attempt`** (S-Auto-1 sandbox 的 belt-and-suspenders): 检测结构性不应该可能的情形 —— diff sandbox-ACCEPTED 且 `results.json` 显示之前 `FAIL` 的 critical_step 现在 score `N/A`（即 measurement contract 在 optimization 下变了，会 invalidate lexicographic gate）。Detector 读 current `results.json` `tier2_result.per_step[]` 与 baseline `tier2_result.per_step[]`，flag 任何 step 的 `outcome` 从 `"FAIL"` 变到 `"N/A"` 或 `"skipped"`。**ERROR** severity。若 fire 暗示 S-Auto-1 sandbox bypass OR check 4 未覆盖的 eval-time gaming。

##### API

```python
from typing import Literal

@dataclass
class GamingFlag:
    rule_id: str
    severity: Literal["WARN", "ERROR"]
    detail: str
    evidence: dict  # rule-specific evidence; never contains raw shadow case content

def detect(iteration_record, recent_records, eval_artefacts, *, config) -> list[GamingFlag]: ...
```

返回的 `list[GamingFlag]` 在 `experiments_log.append(...)` **之前** 追加到 `iteration_record.gaming_flags = [...]`。Empty list = 无 flags raised。

#### #4 — Wire into `autoloop/autoloop/loop.py` (extension only; no signature change)

三个 insertion points in 现有 state machine：

**Insertion A — pre-sandbox content validator** (在现 step 2 与 3 之间):
```
2.   proposer.propose(...) → Hypothesis
2.5  content_validator.validate_content(hypothesis) → ContentValidationResult
     FAIL → discard + log + return (discard_reason=content_validator_rejected:<rule_id>)
3.   sandbox.validate_skill_yaml_diff(...) → ValidationResult
```

**Insertion B — anti-hardcode at propose-stage** (现 step 4，S-Auto-3 hook): 无 wiring 改动；call 已在位。S-Auto-4 只换 `anti_hardcode_check` body。NEW: verdict 为 `FLAG_FOR_CODEX` 时 loop 附加 `iteration_record.anti_hardcode_flag_for_codex = True` (不 discard)；`FAIL` 时 discard with `discard_reason=anti_hardcode_rejected:<rule_id>` (S-Auto-3 hook contract 已有行为)。

**Insertion C — post-eval gaming detect** (在现 step 9 与 10 之间):
```
9.   tier_evaluator.evaluate(...) → LexicographicVerdict
9.5  gaming.detect(iter_record, recent_records, eval_artefacts, config=config) → list[GamingFlag]
     iter_record.gaming_flags = flags
10.  memory.experiments_log.append(iter_record)
```

若 `IterationResult` dataclass 必须加新字段，加为 **additive optional**: `gaming_flags: list[dict] = field(default_factory=list)` —— 向后兼容 S-Auto-3 tests 构造 `IterationResult` 时不传此字段的情况。

#### #5 — `autoloop/autoloop/cli.py audit --experiment exp-N` extension (no signature change)

扩展 audit 输出 surface：
- `anti_hardcode_flag_for_codex: True/False`
- `gaming_flags: [list of {rule_id, severity, detail}]` —— per iteration 渲染为小表

默认 `audit` mode 继续 RESPECT shadow firewall (只读 persisted records 的 aggregate Layer 4 metrics)。`--include-shadow-detail` flag 行为不变 (仍是唯一 path to per-case shadow info)。

#### #6 — `autoloop/config.yaml` extension (append-only; no existing field touched)

Append:

```yaml
content_validator:
  custom_token_shapes: []                 # additional placeholder token shapes; default empty
  deny_list:                              # forbidden-token deny-list
    - "<<SYSTEM>>"                        # example seed; deliver-agent + human curate at S-Auto-4 close
    - "<<USER>>"
  length_overflow_ratio: 5.0
  length_underflow_ratio: 0.1
  length_underflow_min_before: 50

anti_hardcode:
  enabled: true
  synonym_map_enabled: false              # off by default; deliver-agent enables after calibration evidence
  flag_for_codex_rate_warn_threshold: 0.25

gaming:
  enabled: true
  observation_only_in_v1: true            # documents v1 policy; M-Auto-1B may flip
  shadow_leak_signatures:                 # CONFIG-driven only; detector MUST NOT read case_specs_shadow content directly
    - "cs59s"
    - "shadow_case_id"
    - "shadow/"
    - "case_specs_shadow"
  anomalous_metric_movement_threshold: 0.05

fitness:
  scoring_code_baseline_sha: null         # deliver-agent fills at S-Auto-4 close with the M-Auto-1A close commit SHA;
                                          # missing → gaming.scoring_code_drift emits WARN baseline_missing (NOT guess)
```

S-Auto-1/2/3 字段 UNTOUCHED。

#### #7 — `autoloop/program.md` §4 row 3 status flip

单格 edit：

- **Row 3 (Anti-hardcode auto-check)**: `S-Auto-4 — DEFERRED ...` → `S-Auto-4 — DELIVERED (deterministic regex+heuristic detector covering Q1/Q2/Q4/Q5 of the §4.1 nine-question kernel; PASS / FAIL / FLAG_FOR_CODEX verdicts; FLAG_FOR_CODEX is observation-only in v1, never auto-promoted to FAIL by the detector itself)`

其他 prose 不动。

#### #8 — Tests (~50-80 NEW; expected total 147 → 197-227)

`autoloop/tests/test_anti_hardcode_check.py` (NEW):
- ≥8 forbidden positives 跨 Q1/Q2/Q4/Q5 (每 category ≥2) 每个 auto-rejected with 正确 `rule_id`
- ≥4 clean soft-narrative positives (每个 allowed Skill YAML field type 一个) 每个 PASS
- Unicode NFKC normalization (e.g., `'ｉf'` 全角 → matched)
- Multi-line decomposition of an `if-then` (across `\n` 边界) 仍 detected
- `FLAG_FOR_CODEX` verdict on ≥2 borderline cases (单 "MUST" with non-Tier-0 subject；standalone "force the assistant to" without verb specificity)
- **Detector self-discipline regression** (D2 关键): fixture test 断言 kernel rule 定义中 **不** 含任何 specific eval phrase / user utterance / expected answer literal (grep-style scan of rule source for `cs011` / `cs015` / `closure_criterion` 等 —— must be zero)

`autoloop/tests/test_content_validator.py` (NEW):
- Placeholder corruption: `before_value="Hello {USER}"`, `after_value="Hello {USE"` → FAIL `rule_id=content_validator.placeholder_corrupted`
- Placeholder absent in before_value: `before="Hello there"`, `after="Hello {x"` → PASS (no token in before; skip check)
- Length overflow: `len(after) > 5 × len(before)` → FAIL
- Length underflow with floor: `before="..." (60 chars), after="" (0 chars)` → FAIL `zero_length` (zero-length 优先 fire)
- Length underflow ratio: `before="..." (100 chars), after="ok" (2 chars)` → FAIL `length_underflow`
- Underflow ratio 在 before 短时不强制: `before="ok" (2 chars), after="" (0 chars)` → FAIL `zero_length` only (underflow ratio 跳过，因为 before < 50)
- Deny-list hit: `after_value` 含 `<<SYSTEM>>` → FAIL `deny_list`
- Clean edit: 小 soft narrative tweak → PASS
- **D3 regression**: fixture test 断言 validator 在 `custom_token_shapes` empty 时 **不** 依赖 Salesforce-specific token shapes (e.g., Salesforce ID-shaped string in `after_value` 不 trigger)

`autoloop/tests/test_gaming.py` (NEW):
- 7 个 check 每个被 synthetic fixture trigger (positive)
- 7 个 check 每个被 clean fixture **不** trigger (negative)
- `scoring_code_drift.baseline_missing` 在 `config.fitness.scoring_code_baseline_sha` 为 None 时 emit WARN (D3 regression)
- `shadow_set_leakage` 配置驱动: detector with empty `config.gaming.shadow_leak_signatures` 即使 iteration_record 含 `cs59s` 字符串也 **不** 产生 flag (D3 regression —— detector 必须 config-driven，无 implicit defaults 读 shadow content)
- `tier2_measurement_contract_change_attempt` 在之前 FAIL critical_step 现在 score N/A 时 trigger

`autoloop/tests/test_loop_anti_hardcode_wire.py` (NEW; integration):
- **Adversarial-fixture regression target**: S-Auto-3 `test_loop.py::test_adversarial_fixture_passes_placeholder_anti_hardcode` **rename 为 `test_adversarial_fixture_fails_real_detection`** 并断言 `decision=="discard"` + `discard_reason.startswith("anti_hardcode_rejected:")`。**这是 placeholder → real transition 的 load-bearing 测试**
- `test_content_validator_failure_discards_before_sandbox`: mocked content_validator 返回 FAIL → iteration discards；sandbox.validate_skill_yaml_diff **never** called
- `test_gaming_flags_persisted_to_experiments_log_and_visible_in_audit`: 1-iter integration test 跑 happy-path loop with fixture triggering `anomalous_metric_movement`；断言 `experiments_log` row 有 `gaming_flags` list non-empty；断言 `audit --experiment exp-N` 输出 render the flag
- `test_anti_hardcode_flag_for_codex_does_not_discard`: hypothesis 触发 FLAG_FOR_CODEX (not FAIL)；iteration 继续 apply / eval；`experiments_log` row 有 `anti_hardcode_flag_for_codex: true`

**现有 test updates (NOT new tests)**:
- `autoloop/tests/test_loop.py::test_adversarial_fixture_passes_placeholder_anti_hardcode` → rename + flip per above
- `autoloop/tests/test_meta_agent.py` 中关于 placeholder=True 的断言 → 更新为 placeholder=False (detector 不再是 placeholder)

#### #9 — `autoloop/README.md` (single line)

更新 CLI table `audit` row description: S-Auto-4 onward `audit` surfaces `gaming_flags` + `anti_hardcode_flag_for_codex`。其他 README 不改。

### Hard fences / STOP conditions (do NOT do)

- **Signature preservation contract with S-Auto-3** (不可违反):
  - `anti_hardcode_check(hypothesis, *, config) -> AntiHardcodeResult` —— signature verbatim from S-Auto-3；只换 body
  - `AntiHardcodeResult.placeholder` field 保留但 S-Auto-4 一律返回 `False`
  - `loop.py` `IterationResult` dataclass —— 加 `gaming_flags: list[dict] = field(default_factory=list)` 作 **additive optional field** with default value，S-Auto-3 tests 构造 `IterationResult` 时不传此字段仍 PASS
- **No touch** to `eval_interactive/eval_interactive/**`、case_spec、case_specs_shadow、`server/`、`eval/` Java、`data/`、`db/`、`server/src/main/resources/`、`docs/foundational/`、`docs/runtime_freeze_and_risk_policy.md`、`docs/current/`、`docs/sprints/*` archives、`docs/milestones/*` archives、`docs/codex-findings.md`
- **No new heavy deps** in `autoloop/pyproject.toml` —— 只 stdlib `re` + `unicodedata` + `subprocess` (for `git show` in gaming check 3)。若需新 dep, STOP and surface to deliver-agent **之前** add
- **No LLM call** inside `anti_hardcode_check` or `content_validator` (deterministic + cheap；no propose-stage latency hit；no 第二个 LLM dependency)
- **No semantic hardcode IN the detector itself** (D2 不可违反) —— rules 只 encode generic structural shapes。若某 rule 定义含 specific eval phrase / user utterance / expected answer / case-success-label literal，rule 结构性错误。Detector self-discipline test 在 `test_anti_hardcode_check.py` 最终 fixture grep rule source 拦截
- **No shadow case content read by gaming.py** (D3 不可违反) —— `gaming.detect` 不 open 任何 `eval_interactive/case_specs_shadow/` 下的 file、不依赖 shadow corpus content；`shadow_set_leakage` rule 只读 `config.gaming.shadow_leak_signatures` (out-of-band human-approved config)
- **No baseline guessing in gaming.py** (D3 不可违反) —— `config.fitness.scoring_code_baseline_sha` 缺失/None 时 emit WARN `baseline_missing`；**禁止** 计算 hash silently 用作 baseline
- **STOP and surface** 若 detector regex set 超过 ~30 rules total (over-fit / pattern explosion 信号 —— ship 前 re-design)
- **STOP and surface** 若 `FLAG_FOR_CODEX` rate on calibration table > 25% (detector 太 noisy —— ship 前 re-calibrate；`config.anti_hardcode.flag_for_codex_rate_warn_threshold` 文档化此阈值)
- **STOP and surface** 若 Codex bypass spot-check (unicode/synonym/decomposition) 任一成功 —— 该 finding 在 milestone close 前 fix-iteration
- **No live LLM call in tests** —— 所有 anti_hardcode / content_validator / gaming tests 用 synthetic fixtures

### Test / eval requirements

- **Python autoloop suite**: `cd autoloop && uv run pytest -q` —— Sprint 56 baseline `147 passed` 必须 grow by ~50-80 NEW S-Auto-4 tests (total ~197-227 PASS, 0 fail)。Sprint 54 + Sprint 55 tests UNCHANGED。Sprint 56 tests 只在 #8 列出的两个 rename point 更新
- **Existing Python eval_interactive suite UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` 重现 `486 passed, 3 failed`
- **Java baseline UNCHANGED**: skipped per S-Auto-4 Java-zero-touch (verify `git diff --stat HEAD -- server/ eval/src/main/java/` returns empty)
- **NO live LLM call in pytest tests** —— 所有 detector / validator / gaming tests 用 synthetic fixtures
- **Calibration table** (deliver-agent ships in this prompt; dev verifies in pytest): ≥8 forbidden propose examples (含 §1.7 显式 example `IF user.message.contains('appeal') THEN active_use_case := UC-H`) 全部 auto-rejected；≥4 clean soft-narrative examples (一个 per allowed field type) 全部 PASS；≥2 borderline FLAG_FOR_CODEX cases
- **S-Auto-3 adversarial regression**: existing `test_adversarial_fixture_passes_placeholder_anti_hardcode` rename + flip 为 `test_adversarial_fixture_fails_real_detection`；assert `decision=="discard"` + `discard_reason.startswith("anti_hardcode_rejected:")`

### §7 — Layer-classification + anti-hardcode stanza

**Target failure layer:** `eval_spec` (§3.2 Q6 —— kernel 是结构性 §1.7 defence at propose-stage; 不改 runtime / projection / scoring / CaseSpec)。§7 REQUIRED applies。

**Tier-0 invariant:** 不加 Tier-0 invariant。Kernel 是 meta-loop infrastructure，NOT runtime invariant per `docs/runtime_freeze_and_risk_policy.md` §1/§2。Kernel **检测** attempt to invent new Tier-0 semantics (Q2 rule) —— 正好是介绍 Tier-0 的反面。

**Semantic hardcode:** 不引入 semantic hardcode。Justification by surface:
- `anti_hardcode_check.py` rules 是 **generic structural pattern matches** (per D2) —— IF/THEN shapes、`cs<id>`-like tokens、MUST/NEVER phrasing structures、`.contains(...)` literals。Rules **不** enumerate specific eval phrases / user utterances / expected answers / case-status labels。Detector self-discipline test 在 `test_anti_hardcode_check.py` regression-guards 这点
- `content_validator.py` 只是结构性 integrity —— placeholder tokens already in `before_value`、length bounds、configurable deny-list。NO Salesforce-specific 或 domain-specific semantic logic (per D3 —— 只 3 generic placeholder shapes；custom shapes config-driven)
- `gaming.py` checks 是 numeric metric comparisons + structural artefact properties + config-driven leak-signature scans。NO shadow case content 读。NO baseline guessing。NEW `tier2_measurement_contract_change_attempt` 是结构性 impossibility 警报；若 fire 暗示 sandbox 有问题，**不是** semantic decision
- `loop.py` wiring extensions 是 dataclass-additive only；no signature change to orchestrator or S-Auto-1/2/3 deliverables

**Generalization coverage:** target = §4.1 kernel 的 auto-tractable 子集 (Q1+Q2+Q4+Q5) auto-reject 8+ forbidden calibration examples + accept 4+ clean calibration examples + S-Auto-3 adversarial fixture flip PASS → FAIL。Neighbor = unicode NFKC + multi-line decomposition + 小 synonym-map equivalents of forbidden patterns。Negative-control = 每个 allowed field type 一个 clean soft-narrative edit (no false positive)。Adversarial = Codex per-sub-sprint constructs ≥3 hand-authored adversarial proposes (unicode obfuscation、multi-line decomposition、semantic-equivalence via synonym swap)；detector must reject each。Shadow firewall coverage = `shadow_set_leakage` detector 用 fixture (iteration_record 含 `cs59s` token AND config 不含此 signature → no false positive；detector 是 config-driven，**NOT** implicit-default)。

### Codex review plan (§4.3)

**PER-SUB-SPRINT REQUIRED — §4.3 trigger #2**。Anti-hardcode kernel + content validator + gaming checks 是 §1.7 结构性 guard；Codex 必须在 M-Auto-1B 跑 detector against real meta-agent outputs **之前** 独立验证 detector design。

**Codex prompt timing**: deliver-agent 在 S-Auto-4 **close** (NOT at open) 时 author `compact/sprint-057-codex-review-prompt.md`，覆盖实际 delivered commit range。Dev **不** dispatch Codex；dev 在 handoff §11 列出 Codex 需要的 calibration table 实际结果 + bypass spot-check fixtures available。

**Codex 必须验证**:
1. §4.1 九问 kernel walk against detector source (`anti_hardcode_check.py` + `content_validator.py` + `gaming.py`): defender 自身是否 encode §1.7-violating decision logic? (D2 regression —— detector rules 必须 generic structural；specific eval phrases / user utterances / answers / labels 是 forbidden)
2. Detector catches 8+ forbidden calibration examples + 至少 1 个 Codex hand-authored adversarial
3. ≥3 bypass spot-checks: unicode obfuscation (NFKC + visual confusables)、multi-line decomposition (if-then 跨 lines/bullets)、semantic-equivalence via synonym swap (e.g., IF→WHEN, THEN→→)。Detector must reject each
4. D3 regressions: gaming.py **不** 直接读 `case_specs_shadow/` 内容；`scoring_code_drift.baseline_missing` 在 config 字段 unset 时 emit WARN (不 guess)
5. Verdict set 无明显 design hole；FLAG_FOR_CODEX semantics 不被滥用为 silently auto-accept borderline cases

**Verdict expected**: `pass / 0` or `approve with downgrade-to-signal follow-up`。Must return BEFORE M-Auto-1A milestone close。`reject as semantic hardcode` 触发 fix-iteration sub-sprint。

### Handoff requirements

- Author `docs/sprints/sprint-057-handoff.md` at S-Auto-4 close；**§12 留空** (deliver-agent + human at milestone close)
- Record in handoff: `git show --numstat` for S-Auto-4 commit；calibration table actual results (8+ forbidden / 4+ clean / 2+ FLAG_FOR_CODEX)；detector LOC + total rule count (必须 ≤30)；gaming.py LOC + 7 checks × positive + negative fixture status；content_validator LOC + test count；pytest delta (147 → ?)；S-Auto-3 adversarial test rename；`autoloop/config.yaml` content_validator + anti_hardcode + gaming blocks (with deny_list + leak_signatures seeded)；§7 self-walk；OQ-S57.x list 含 FLAG_FOR_CODEX rate measurement on calibration set
- **DO NOT** author `compact/sprint-057-codex-review-prompt.md` —— 这是 deliver-agent 在 S-Auto-4 close 后 author 的 close-bundle artefact，dev 不写

### Commit discipline

Dev 只 stage **S-Auto-4 scope**: 3 NEW source files (`autoloop/autoloop/sandbox/anti_hardcode_check.py` [body swap; signature unchanged]、`autoloop/autoloop/sandbox/content_validator.py`、`autoloop/autoloop/scoring/gaming.py`) + 4 NEW test files + 2 现有 test updates (renames + assertion flips in `test_loop.py` + `test_meta_agent.py`) + extended `autoloop/autoloop/loop.py` (additive wiring + dataclass field) + extended `autoloop/autoloop/cli.py audit` rendering + extended `autoloop/config.yaml` (3 NEW blocks) + 1-row update `autoloop/program.md` §4 row 3 + 1-line `autoloop/README.md` `audit` row note + NEW `docs/sprints/sprint-057-handoff.md`。**No `git add -A`** —— deliver-agent close-bundle 文件 (此 objective archive rename、milestone_objective.md edits at milestone close、10-handoff updates、Codex prompt + findings) 由 human 在 close commit 时 bundle。

One commit at sub-sprint close (commit-at-end pattern)。Commit message: `Sprint 57 / S-Auto-4 — anti-hardcode kernel + content validator + gaming checks`。

### OQ (open questions — 填写在 handoff §11)

- **OQ-S57.x candidates** (expected; dev surfaces as ambiguities encountered):
  - FLAG_FOR_CODEX rate measurement on actual calibration set (target <25%)
  - 是否任一 rule 需要 `synonym_map_enabled = true` after calibration (default false；dev evidence informs)
  - `gaming.scoring_code_drift` baseline SHA 应是 M-Auto-1A close commit SHA 或别的 reference (deliver-agent 在 S-Auto-4 close 时填；不是 dev's call)
  - Content_validator `deny_list` seed list 大小 / 内容 (deliver-agent + human 在 S-Auto-4 close curate；dev ships minimal seed)
  - Any rule that crossed the detector self-discipline regression test in early iteration (dev fixed before commit；handoff records as "fixed during dev")

## Self-check checklist (sub-sprint close 前 dev 勾选)

- [ ] `anti_hardcode_check.py` body 已 swap；signature **不动**；`AntiHardcodeResult.placeholder` 字段保留但本 sprint 一律 False
- [ ] Detector 覆盖 Q1/Q2/Q4/Q5；每类 ≥2 个 calibration positive；每类 ≥1 个 clean negative；≥2 个 borderline FLAG_FOR_CODEX
- [ ] Detector self-discipline regression test PASS (grep rule source for `cs011` / `cs015` / 具体 user utterance / 具体 expected answer literal —— 全部 zero)
- [ ] Detector rule total ≤30 (若超 STOP and surface)
- [ ] `FLAG_FOR_CODEX` rate on calibration table ≤25% (若超 STOP and surface)
- [ ] `content_validator.py` 覆盖 3 generic placeholder shapes + length sanity + deny-list；**不** 引入 Salesforce-specific semantic logic in default config
- [ ] D3 regression tests PASS: `content_validator` 在 `custom_token_shapes` empty 时不依赖 Salesforce shapes；`gaming.shadow_set_leakage` config-driven 不读 `case_specs_shadow/`；`gaming.scoring_code_drift` baseline missing 时 emit WARN (不 guess)
- [ ] `gaming.py` 7 checks 每个 positive + negative fixture covered；all observation-only in v1 (不改 tier_evaluator keep/discard verdict)
- [ ] `loop.py` 3 insertion points wired (pre-sandbox content_validator + post-eval gaming.detect + flag_for_codex flag-attach); `IterationResult` 加 additive optional `gaming_flags` field
- [ ] `cli.py audit` 扩展 surface (gaming_flags + anti_hardcode_flag_for_codex)；default 仍 RESPECT shadow firewall；`--include-shadow-detail` 行为不变
- [ ] S-Auto-3 `test_adversarial_fixture_passes_placeholder_anti_hardcode` renamed + flipped 为 `test_adversarial_fixture_fails_real_detection`；assert `decision=="discard"` + `discard_reason.startswith("anti_hardcode_rejected:")`
- [ ] S-Auto-3 `test_meta_agent.py` placeholder=True assertion 更新为 placeholder=False
- [ ] `autoloop/config.yaml` 加 3 个 NEW block (`content_validator`、`anti_hardcode`、`gaming`) + `fitness.scoring_code_baseline_sha: null` 占位（deliver-agent fills at S-Auto-4 close）
- [ ] `autoloop/program.md` §4 row 3 DEFERRED → DELIVERED；其他 prose 不动
- [ ] `autoloop/README.md` audit row 1-line note 更新
- [ ] `cd autoloop && uv run pytest -q` → ≥197 passed (147 + 50+ NEW)；Sprint 54 + 55 tests UNCHANGED；Sprint 56 tests 只在两个 rename point 更新
- [ ] `cd eval_interactive && uv run python -m pytest --tb=no -q` → `486 passed, 3 failed` UNCHANGED
- [ ] `git diff --stat HEAD -- server/ eval/src/main/java/ eval_interactive/eval_interactive/ eval_interactive/case_specs/ eval_interactive/case_specs_shadow/ data/ db/ server/src/main/resources/ docs/foundational/ docs/runtime_freeze_and_risk_policy.md docs/current/ docs/sprints/sprint-0 docs/milestones/ docs/codex-findings.md` returns empty
- [ ] `docs/sprints/sprint-057-handoff.md` written；§12 留空
- [ ] §7 self-walk in handoff
- [ ] Single commit at sub-sprint close；commit message per contract；no `git add -A`
- [ ] OQ-S57.x list in handoff (含 FLAG_FOR_CODEX rate measurement + 任何 dev 在 development 中 surface 的 ambiguity)
