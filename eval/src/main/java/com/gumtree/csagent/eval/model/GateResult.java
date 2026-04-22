package com.gumtree.csagent.eval.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Gate evaluation result - whether a quality gate passed or failed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GateResult {

    private String gateName;
    private String description;
    private boolean hard;
    private boolean passed;
    private double actualValue;
    private double threshold;
    private String operator;
    private String detail;

    /**
     * Collection of all gate results for a run.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GateSummary {
        @Builder.Default
        private List<GateResult> gates = new ArrayList<>();
        private boolean allHardGatesPassed;
        private boolean allGatesPassed;
        private int hardGatesFailed;
        private int softGatesFailed;

        public void addGate(GateResult gate) {
            gates.add(gate);
        }

        public void computeSummary() {
            hardGatesFailed = (int) gates.stream()
                    .filter(g -> g.isHard() && !g.isPassed())
                    .count();
            softGatesFailed = (int) gates.stream()
                    .filter(g -> !g.isHard() && !g.isPassed())
                    .count();
            allHardGatesPassed = hardGatesFailed == 0;
            allGatesPassed = hardGatesFailed == 0 && softGatesFailed == 0;
        }
    }
}
