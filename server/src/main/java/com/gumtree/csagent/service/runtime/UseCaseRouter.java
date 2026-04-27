package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Two-stage use case routing:
 * Stage 1: Check topic_subject against strong priors, handover-only topics
 * Stage 2: Use LLM to classify description against UC candidates
 */
@Slf4j
@Service
public class UseCaseRouter {

    private final UseCaseRegistryService useCaseRegistry;
    private final LlmInvocationService llmInvocation;
    private final ObjectMapper objectMapper;

    public UseCaseRouter(UseCaseRegistryService useCaseRegistry,
                         LlmInvocationService llmInvocation,
                         ObjectMapper objectMapper) {
        this.useCaseRegistry = useCaseRegistry;
        this.llmInvocation = llmInvocation;
        this.objectMapper = objectMapper;
    }

    /**
     * Route the session to the appropriate use case.
     * Modifies session in place: activeUseCase, candidateUseCases, intentConfidence.
     *
     * @return the routing result
     */
    public RoutingResult route(BotSession session, String topicSubject, String description) {
        log.info("Session {}: routing, topicSubject='{}', description length={}",
                session.getSessionId(), topicSubject,
                description != null ? description.length() : 0);

        // Stage 1: Check topic_subject for direct routing

        // Check handover-only topics first (out-of-scope)
        if (useCaseRegistry.isHandoverOnlyTopic(topicSubject)) {
            Optional<String> oosCategory = useCaseRegistry.getOutOfScopeCategory(topicSubject);
            String reason = oosCategory.orElse("OUT_OF_SCOPE");
            log.info("Session {}: handover-only topic '{}' -> OOS ({})",
                    session.getSessionId(), topicSubject, reason);
            return RoutingResult.outOfScope(reason);
        }

        // Check strong prior topics
        Optional<String> strongPrior = useCaseRegistry.getStrongPriorTopic(topicSubject);
        if (strongPrior.isPresent()) {
            String ucId = strongPrior.get();
            log.info("Session {}: strong prior topic '{}' -> {}",
                    session.getSessionId(), topicSubject, ucId);
            session.setActiveUseCase(ucId);
            session.setIntentConfidence(new BigDecimal("0.90"));
            return RoutingResult.routed(ucId, new BigDecimal("0.90"));
        }

        // Stage 2: LLM classification for weak priors and "Account Support" (full candidate set)
        List<String> candidates = useCaseRegistry.getCandidateUcsForTopic(topicSubject);
        if (candidates.isEmpty()) {
            // No matching UCs for this topic — check if it's an unknown topic
            log.warn("Session {}: no candidate UCs for topic '{}', escalating",
                    session.getSessionId(), topicSubject);
            return RoutingResult.outOfScope("UNKNOWN_TOPIC");
        }

        return routeViaLlm(session, candidates, topicSubject, description);
    }

    private RoutingResult routeViaLlm(BotSession session, List<String> candidates,
                                       String topicSubject, String description) {
        // Build candidate descriptions for the LLM
        String ucCandidatesText = candidates.stream()
                .map(ucId -> {
                    UseCaseRegistryService.UseCaseDefinition def = useCaseRegistry.getUseCase(ucId);
                    return String.format("- %s: %s (path: %s)", ucId, def.name(), def.path());
                })
                .collect(Collectors.joining("\n"));

        try {
            LlmResponse response = llmInvocation.invokeRouting(ucCandidatesText, topicSubject, description,
                    session.getSessionId());

            if ("error_fallback".equals(response.getFinishReason())) {
                // LLM failed — if we have a single candidate, use it; otherwise escalate
                if (candidates.size() == 1) {
                    String ucId = candidates.get(0);
                    session.setActiveUseCase(ucId);
                    session.setIntentConfidence(new BigDecimal("0.50"));
                    return RoutingResult.routed(ucId, new BigDecimal("0.50"));
                }
                return RoutingResult.ambiguous(candidates);
            }

            // Parse the routing response
            String content = response.getContent();
            JsonNode root = objectMapper.readTree(cleanJsonResponse(content));

            String useCaseId = root.has("use_case") ? root.get("use_case").asText("") : "";
            double confidence = root.has("confidence") ? root.get("confidence").asDouble(0.0) : 0.0;

            // Validate the returned UC is in our candidate set
            if (!candidates.contains(useCaseId)) {
                log.warn("Session {}: LLM returned UC '{}' not in candidates {}, picking first candidate",
                        session.getSessionId(), useCaseId, candidates);
                useCaseId = candidates.get(0);
                confidence = 0.4;
            }

            BigDecimal conf = BigDecimal.valueOf(confidence);
            session.setActiveUseCase(useCaseId);
            session.setIntentConfidence(conf);
            session.setCandidateUseCases(candidates.toArray(new String[0]));

            log.info("Session {}: LLM routed to {} with confidence {}",
                    session.getSessionId(), useCaseId, conf);

            return RoutingResult.routed(useCaseId, conf);

        } catch (Exception e) {
            log.error("Session {}: routing LLM parse failed: {}",
                    session.getSessionId(), e.getMessage());
            // Fallback: use first candidate with low confidence
            if (!candidates.isEmpty()) {
                String ucId = candidates.get(0);
                session.setActiveUseCase(ucId);
                session.setIntentConfidence(new BigDecimal("0.30"));
                return RoutingResult.routed(ucId, new BigDecimal("0.30"));
            }
            return RoutingResult.ambiguous(candidates);
        }
    }

    private String cleanJsonResponse(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int nl = trimmed.indexOf('\n');
            if (nl > 0) trimmed = trimmed.substring(nl + 1);
            if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.length() - 3);
            trimmed = trimmed.trim();
        }
        return trimmed;
    }

    /**
     * Routing result.
     */
    public record RoutingResult(
            RoutingOutcome outcome,
            String activeUseCase,
            BigDecimal confidence,
            String outOfScopeReason,
            List<String> ambiguousCandidates
    ) {
        public enum RoutingOutcome { ROUTED, OUT_OF_SCOPE, AMBIGUOUS }

        static RoutingResult routed(String ucId, BigDecimal confidence) {
            return new RoutingResult(RoutingOutcome.ROUTED, ucId, confidence, null, null);
        }

        static RoutingResult outOfScope(String reason) {
            return new RoutingResult(RoutingOutcome.OUT_OF_SCOPE, null, null, reason, null);
        }

        static RoutingResult ambiguous(List<String> candidates) {
            return new RoutingResult(RoutingOutcome.AMBIGUOUS, null, null, null, candidates);
        }
    }
}
