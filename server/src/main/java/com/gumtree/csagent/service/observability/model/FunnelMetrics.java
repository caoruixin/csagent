package com.gumtree.csagent.service.observability.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunnelMetrics {

    private long totalSessions;
    private long understoodSessions;
    private long resolvedSessions;
    private long escalatedSessions;
    private long abandonedSessions;
    private Map<String, Long> outcomeBreakdown;
    private Map<String, Long> useCaseBreakdown;
}
