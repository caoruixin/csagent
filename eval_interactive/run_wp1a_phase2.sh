#!/usr/bin/env bash
# S-Auto-40 WP1-A Phase 2 bounded real-LLM measurement-validation run.
# NOT a pilot / candidate search / PRIMARY-success run. No autoloop.
# Approved bounded set (Sprint 093 precedent): 2 PRIMARY x11 + neighbors/controls x5.
set -u
cd /Users/caoruixin/projects/csagent-latest/eval_interactive || exit 1
EI=.venv/bin/eval-interactive
RUNDIR=results/wp1a-phase2-measurement-20260619
DRAWS=$RUNDIR/draws
mkdir -p "$DRAWS"

# case_path:draws
SET=(
  "case_specs/bad_cases/cs_uc_a_no_ad_id_ad_specific.yaml:11"
  "case_specs/bad_cases/cs_uc_a_loaded_listing.yaml:11"
  "case_specs/bad_cases/cs_uc_a_generic_policy_question.yaml:5"
  "case_specs/bad_cases/cs_uc_a_lookup_failed.yaml:5"
  "case_specs/bad_cases/cs_uc_fp_loaded_moderation.yaml:5"
  "case_specs/case_families/cs011_uc_d_description_ignored/negative/cs11g02_uc_d_explicit_distress.yaml:5"
)

echo "[wp1a-p2] START $(date -u +%FT%TZ) rundir=$RUNDIR"
total=0
for entry in "${SET[@]}"; do
  path="${entry%:*}"; n="${entry##*:}"
  cid=$(basename "$path" .yaml)
  for k in $(seq 1 "$n"); do
    out=$("$EI" run --path "$path" --label wp1a-phase2 2>&1)
    rid=$(echo "$out" | grep 'Run ID:' | awk '{print $NF}')
    stop=$(echo "$out" | grep -oE 'stop=[a-z_]+' | head -1)
    if [ -n "$rid" ] && [ -f "results/$rid/results.json" ]; then
      cp "results/$rid/results.json" "$DRAWS/${cid}__d${k}__${rid}.json"
      total=$((total+1))
      echo "[wp1a-p2] draw $total: $cid d$k rid=$rid $stop"
    else
      echo "[wp1a-p2] draw FAIL: $cid d$k (no rid/results) :: $(echo "$out" | tail -2 | tr '\n' ' ')"
    fi
  done
done
echo "[wp1a-p2] DONE $(date -u +%FT%TZ) total_draws=$total"
