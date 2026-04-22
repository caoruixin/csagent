package com.gumtree.csagent.eval.harness;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.eval.config.EvalConfig;
import com.gumtree.csagent.eval.model.EvalSession;
import com.gumtree.csagent.eval.model.EvalTurn;
import com.gumtree.csagent.eval.model.SessionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP replay simulator that runs eval sessions against the live bot.
 *
 * Flow:
 *   1. POST /v1/chat/sessions with form data -> get session_id
 *   2. For each visitor turn: POST /v1/chat/sessions/{id}/messages
 *   3. Collect all bot responses
 *   4. GET /v1/chat/sessions/{id} to retrieve final session state
 */
@Slf4j
@Component
public class SessionSimulator {

    private final EvalConfig config;
    private final RestTemplate restTemplate;
    private final FormNormalizer formNormalizer;
    private final ObjectMapper objectMapper;

    public SessionSimulator(EvalConfig config, RestTemplate restTemplate,
                            FormNormalizer formNormalizer) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.formNormalizer = formNormalizer;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Replay an eval session through the bot and collect results.
     */
    @SuppressWarnings("unchecked")
    public SessionResult simulate(EvalSession evalSession) {
        long startTime = System.currentTimeMillis();
        SessionResult result = SessionResult.builder()
                .evalSessionId(evalSession.getId())
                .sourceDataset(evalSession.getSourceDataset())
                .expectedUc(evalSession.getEffectivePrimaryUc())
                .expectedOutcome(evalSession.getEffectiveOutcomeClass())
                .build();

        try {
            // Step 1: Create session
            Map<String, String> createPayload = formNormalizer.buildCreateSessionPayload(evalSession);
            String createUrl = config.getBotBaseUrl() + "/v1/chat/sessions";

            log.debug("Creating session for eval {}: {}", evalSession.getId(), createPayload);
            ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                    createUrl, createPayload, Map.class);

            if (!createResponse.getStatusCode().is2xxSuccessful() || createResponse.getBody() == null) {
                result.setReplaySuccess(false);
                result.setReplayError("Session creation failed: HTTP " + createResponse.getStatusCode());
                return finishResult(result, startTime);
            }

            Map<String, Object> createBody = createResponse.getBody();
            String botSessionId = (String) createBody.get("session_id");
            result.setBotSessionId(botSessionId);

            // Collect the initial bot response
            String initialReply = (String) createBody.get("reply_text");
            if (initialReply != null) {
                result.getBotResponses().add(initialReply);
            }

            // Check if chat should end immediately
            Boolean shouldEnd = (Boolean) createBody.get("should_end_chat");
            if (Boolean.TRUE.equals(shouldEnd)) {
                log.debug("Bot ended session immediately for eval {}", evalSession.getId());
                result.setReplaySuccess(true);
                return finishResult(result, startTime, botSessionId);
            }

            // Step 2: Send visitor turns
            List<EvalTurn> visitorTurns = evalSession.getVisitorTurns();
            String messageUrl = config.getBotBaseUrl() + "/v1/chat/sessions/" + botSessionId + "/messages";

            for (EvalTurn turn : visitorTurns) {
                try {
                    Map<String, String> msgPayload = Map.of("message", turn.getMessageRedacted());
                    ResponseEntity<Map> msgResponse = restTemplate.postForEntity(
                            messageUrl, msgPayload, Map.class);

                    if (msgResponse.getStatusCode().is2xxSuccessful() && msgResponse.getBody() != null) {
                        Map<String, Object> msgBody = msgResponse.getBody();
                        String reply = (String) msgBody.get("reply_text");
                        if (reply != null) {
                            result.getBotResponses().add(reply);
                        }
                        result.setBotTurnCount(result.getBotTurnCount() + 1);

                        // Check if chat should end
                        Boolean endChat = (Boolean) msgBody.get("should_end_chat");
                        if (Boolean.TRUE.equals(endChat)) {
                            log.debug("Bot ended chat at turn {} for eval {}", turn.getSequence(), evalSession.getId());
                            break;
                        }
                    } else {
                        log.warn("Message failed for session {}, turn {}: HTTP {}",
                                botSessionId, turn.getSequence(), msgResponse.getStatusCode());
                    }
                } catch (RestClientException e) {
                    log.warn("Message error for session {}, turn {}: {}",
                            botSessionId, turn.getSequence(), e.getMessage());
                }
            }

            result.setReplaySuccess(true);
            return finishResult(result, startTime, botSessionId);

        } catch (RestClientException e) {
            log.error("Replay failed for eval session {}: {}", evalSession.getId(), e.getMessage());
            result.setReplaySuccess(false);
            result.setReplayError(e.getMessage());
            return finishResult(result, startTime);
        }
    }

    @SuppressWarnings("unchecked")
    private SessionResult finishResult(SessionResult result, long startTime, String botSessionId) {
        // Step 3: Fetch final session state
        try {
            String stateUrl = config.getBotBaseUrl() + "/v1/chat/sessions/" + botSessionId;
            ResponseEntity<Map> stateResponse = restTemplate.getForEntity(stateUrl, Map.class);

            if (stateResponse.getStatusCode().is2xxSuccessful() && stateResponse.getBody() != null) {
                Map<String, Object> state = stateResponse.getBody();
                result.setBotSessionState(state);
                result.setBotActiveUseCase((String) state.get("activeUseCase"));
                result.setBotContainmentOutcome((String) state.get("containmentOutcome"));
                result.setBotHandlingState((String) state.get("handlingState"));
                result.setBotCurrentPhase((String) state.get("currentPhase"));
                result.setBotEscalationReason((String) state.get("escalationReason"));

                // Extract tools called if present
                Object articlesShown = state.get("articlesShown");
                if (articlesShown instanceof List) {
                    result.setToolsCalled(new ArrayList<>((List<String>) articlesShown));
                }
            }
        } catch (RestClientException e) {
            log.warn("Failed to fetch final session state for {}: {}", botSessionId, e.getMessage());
        }

        return finishResult(result, startTime);
    }

    private SessionResult finishResult(SessionResult result, long startTime) {
        result.setReplayDurationMs(System.currentTimeMillis() - startTime);
        return result;
    }
}
