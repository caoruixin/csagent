package com.gumtree.csagent.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Auto-loads {@code .env.local} from the project root into Spring's
 * environment at startup. This removes the dependency on Makefile
 * {@code include .env.local / export} — both {@code make backend}
 * and direct {@code mvn spring-boot:run} now pick up the same config.
 *
 * <p>Registered via {@code META-INF/spring.factories}.
 *
 * <p>Precedence: OS environment variables override .env.local values
 * (consistent with 12-factor app convention). Values from .env.local
 * only fill in vars that are NOT already set in the process environment.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "dotenvLocal";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                        SpringApplication application) {
        Path dotenv = findDotenvLocal();
        if (dotenv == null) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        try {
            for (String line : Files.readAllLines(dotenv)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                // Strip optional surrounding quotes
                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                // Only set if not already in OS environment (12-factor precedence)
                if (System.getenv(key) == null) {
                    props.put(key, value);
                }
            }
        } catch (IOException e) {
            // Silently skip — .env.local is optional for production deployments
            return;
        }

        if (!props.isEmpty()) {
            environment.getPropertySources()
                    .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
        }
    }

    private Path findDotenvLocal() {
        // Try working directory first (typical for `mvn spring-boot:run` from server/)
        Path cwd = Path.of(System.getProperty("user.dir", "."));
        Path candidate = cwd.resolve(".env.local");
        if (Files.isRegularFile(candidate)) {
            return candidate;
        }
        // Try parent directory (when cwd is server/)
        Path parent = cwd.getParent();
        if (parent != null) {
            candidate = parent.resolve(".env.local");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
