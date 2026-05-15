package com.gumtree.csagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mock")
public class MockProperties {

    private boolean businessHours = true;
    private int toolLatencyMs = 0;
}
