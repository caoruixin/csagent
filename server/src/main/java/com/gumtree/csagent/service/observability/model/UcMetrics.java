package com.gumtree.csagent.service.observability.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UcMetrics {

    private String useCaseId;
    private String useCaseName;
    private long totalSessions;
    private long resolved;
    private long escalated;
    private double resolutionRate;
}
