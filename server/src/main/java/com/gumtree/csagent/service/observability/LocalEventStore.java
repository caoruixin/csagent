package com.gumtree.csagent.service.observability;

import com.gumtree.csagent.model.BotEvent;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.enums.ContainmentOutcome;
import com.gumtree.csagent.model.enums.EventType;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.observability.model.FunnelMetrics;
import com.gumtree.csagent.service.observability.model.UcMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class LocalEventStore {

    private final BotEventRepository botEventRepository;
    private final BotTurnRepository botTurnRepository;
    private final BotSessionRepository botSessionRepository;

    public LocalEventStore(BotEventRepository botEventRepository,
                           BotTurnRepository botTurnRepository,
                           BotSessionRepository botSessionRepository) {
        this.botEventRepository = botEventRepository;
        this.botTurnRepository = botTurnRepository;
        this.botSessionRepository = botSessionRepository;
    }

    public List<BotEvent> getSessionEvents(String sessionId) {
        return botEventRepository.findBySessionIdOrderByCreatedAt(sessionId);
    }

    public List<BotEvent> getEventsByType(EventType eventType) {
        return botEventRepository.findByEventType(eventType.name());
    }

    public List<BotTurn> getSessionTrace(String sessionId) {
        return botTurnRepository.findBySessionIdOrderByTurnIndex(sessionId);
    }

    public Optional<BotSession> getSessionWithOutcome(String sessionId) {
        return botSessionRepository.findById(sessionId);
    }

    public FunnelMetrics getFunnelMetrics() {
        List<BotSession> allSessions = botSessionRepository.findAll();

        long totalSessions = allSessions.size();
        long understoodSessions = allSessions.stream()
                .filter(s -> s.getActiveUseCase() != null && !s.getActiveUseCase().isBlank())
                .count();
        // Trace contract requires lowercase containment_outcome values
        // (resolved/escalated/abandoned/timeout). Compare case-insensitively
        // so historical uppercase rows still match until the migration runs.
        long resolvedSessions = allSessions.stream()
                .filter(s -> ContainmentOutcome.RESOLVED.name().equalsIgnoreCase(s.getContainmentOutcome()))
                .count();
        long escalatedSessions = allSessions.stream()
                .filter(s -> ContainmentOutcome.ESCALATED.name().equalsIgnoreCase(s.getContainmentOutcome()))
                .count();
        long abandonedSessions = allSessions.stream()
                .filter(s -> ContainmentOutcome.ABANDONED.name().equalsIgnoreCase(s.getContainmentOutcome()))
                .count();

        Map<String, Long> outcomeBreakdown = new LinkedHashMap<>();
        for (BotSession session : allSessions) {
            String outcome = session.getContainmentOutcome();
            if (outcome != null && !outcome.isBlank()) {
                outcomeBreakdown.merge(outcome, 1L, Long::sum);
            }
        }

        Map<String, Long> useCaseBreakdown = new LinkedHashMap<>();
        for (BotSession session : allSessions) {
            String uc = session.getActiveUseCase();
            if (uc != null && !uc.isBlank()) {
                useCaseBreakdown.merge(uc, 1L, Long::sum);
            }
        }

        return FunnelMetrics.builder()
                .totalSessions(totalSessions)
                .understoodSessions(understoodSessions)
                .resolvedSessions(resolvedSessions)
                .escalatedSessions(escalatedSessions)
                .abandonedSessions(abandonedSessions)
                .outcomeBreakdown(outcomeBreakdown)
                .useCaseBreakdown(useCaseBreakdown)
                .build();
    }

    public Map<String, UcMetrics> getPerUcMetrics() {
        List<BotSession> allSessions = botSessionRepository.findAll();
        Map<String, UcMetrics> result = new LinkedHashMap<>();

        for (BotSession session : allSessions) {
            String uc = session.getActiveUseCase();
            if (uc == null || uc.isBlank()) {
                continue;
            }

            UcMetrics metrics = result.computeIfAbsent(uc, k ->
                    UcMetrics.builder()
                            .useCaseId(k)
                            .useCaseName(k)
                            .totalSessions(0)
                            .resolved(0)
                            .escalated(0)
                            .resolutionRate(0.0)
                            .build()
            );

            metrics.setTotalSessions(metrics.getTotalSessions() + 1);

            String outcome = session.getContainmentOutcome();
            if (ContainmentOutcome.RESOLVED.name().equalsIgnoreCase(outcome)) {
                metrics.setResolved(metrics.getResolved() + 1);
            } else if (ContainmentOutcome.ESCALATED.name().equalsIgnoreCase(outcome)) {
                metrics.setEscalated(metrics.getEscalated() + 1);
            }
        }

        // Calculate resolution rates
        for (UcMetrics metrics : result.values()) {
            if (metrics.getTotalSessions() > 0) {
                metrics.setResolutionRate(
                        (double) metrics.getResolved() / metrics.getTotalSessions()
                );
            }
        }

        return result;
    }
}
