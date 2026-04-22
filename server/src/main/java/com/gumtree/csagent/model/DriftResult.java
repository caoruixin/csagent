package com.gumtree.csagent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriftResult {

    public enum DriftType {
        NONE,
        MINOR,
        SOFT_SHIFT,
        HARD_SHIFT,
        USER_ESCALATION_REQUEST
    }

    private DriftType type;
    private String newUseCase;
    @Builder.Default
    private boolean escalationRequested = false;
}
