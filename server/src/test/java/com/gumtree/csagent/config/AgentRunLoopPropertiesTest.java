package com.gumtree.csagent.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AgentRunLoopProperties} binding (D16.A scaffolding).
 * Verifies defaults and that values bind from {@code agent.run-loop.*} keys.
 */
class AgentRunLoopPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void defaults_areEmptyEnabledPhasesAndFourMaxSteps() {
        contextRunner.run(context -> {
            AgentRunLoopProperties props = context.getBean(AgentRunLoopProperties.class);
            assertTrue(props.getEnabledPhases().isEmpty(),
                    "default enabled-phases must be empty (legacy path everywhere)");
            assertEquals(4, props.getMaxToolStepsDefault());
        });
    }

    @Test
    void bindsEnabledPhasesFromConfig() {
        contextRunner
                .withPropertyValues(
                        "agent.run-loop.enabled-phases[0]=RESOLVE_FAQ",
                        "agent.run-loop.enabled-phases[1]=RESOLVE_INTAKE")
                .run(context -> {
                    AgentRunLoopProperties props = context.getBean(AgentRunLoopProperties.class);
                    assertEquals(2, props.getEnabledPhases().size());
                    assertEquals("RESOLVE_FAQ", props.getEnabledPhases().get(0));
                    assertEquals("RESOLVE_INTAKE", props.getEnabledPhases().get(1));
                });
    }

    @Test
    void bindsMaxToolStepsFromConfig() {
        contextRunner
                .withPropertyValues("agent.run-loop.max-tool-steps-default=8")
                .run(context -> {
                    AgentRunLoopProperties props = context.getBean(AgentRunLoopProperties.class);
                    assertEquals(8, props.getMaxToolStepsDefault());
                });
    }

    @Configuration
    @EnableConfigurationProperties(AgentRunLoopProperties.class)
    static class TestConfig {}
}
