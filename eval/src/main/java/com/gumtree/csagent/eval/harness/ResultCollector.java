package com.gumtree.csagent.eval.harness;

import com.gumtree.csagent.eval.model.SessionResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe collector for per-session evaluation results.
 */
@Slf4j
@Component
public class ResultCollector {

    @Getter
    private final List<SessionResult> results = new CopyOnWriteArrayList<>();

    public void addResult(SessionResult result) {
        results.add(result);
        if (!result.isReplaySuccess()) {
            log.warn("Session {} replay failed: {}", result.getEvalSessionId(), result.getReplayError());
        } else if (!result.allPassed()) {
            log.info("Session {} has {} failed grades",
                    result.getEvalSessionId(), result.getFailedGrades().size());
        }
    }

    public int totalSessions() {
        return results.size();
    }

    public int successfulReplays() {
        return (int) results.stream().filter(SessionResult::isReplaySuccess).count();
    }

    public int failedReplays() {
        return (int) results.stream().filter(r -> !r.isReplaySuccess()).count();
    }

    public List<SessionResult> getFailedSessions() {
        return results.stream()
                .filter(r -> !r.allPassed() || !r.isReplaySuccess())
                .toList();
    }

    public List<SessionResult> getResultsByDataset(String dataset) {
        return results.stream()
                .filter(r -> dataset.equals(r.getSourceDataset()))
                .toList();
    }

    public void clear() {
        results.clear();
    }
}
