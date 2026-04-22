package com.gumtree.csagent.eval;

import com.gumtree.csagent.eval.harness.EvalRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CS Agent Eval Harness -- Spring Boot CLI application.
 *
 * Usage:
 *   mvn spring-boot:run                           # Run with smoke suite (default)
 *   mvn spring-boot:run -Peval-smoke              # Explicit smoke profile
 *   mvn spring-boot:run -Peval-full               # Full regression suite
 *   mvn spring-boot:run -Deval.suite=smoke        # Direct property override
 *
 * The application:
 *   1. Loads the specified suite configuration
 *   2. Loads eval datasets from CSV files
 *   3. For each session: replays through the bot via HTTP, then grades the result
 *   4. Aggregates metrics across all sessions
 *   5. Checks quality gates (hard gates must all pass for a release)
 *   6. Generates HTML + JSON reports in target/eval-reports/
 *
 * Exit code: 0 if all hard gates pass, 1 if any hard gate fails.
 */
@Slf4j
@SpringBootApplication
public class EvalApplication implements CommandLineRunner {

    private final EvalRunner evalRunner;

    public EvalApplication(EvalRunner evalRunner) {
        this.evalRunner = evalRunner;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(
                SpringApplication.run(EvalApplication.class, args)));
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("CS Agent Eval Harness starting...");

        boolean allGatesPassed = evalRunner.run();

        if (allGatesPassed) {
            log.info("Eval completed successfully. All hard gates passed.");
        } else {
            log.error("Eval completed with failures. One or more hard gates failed.");
            throw new RuntimeException("Eval hard gate failure");
        }
    }
}
