package com.gumtree.csagent.eval.graders.model;

import com.gumtree.csagent.eval.config.EvalConfig;
import com.gumtree.csagent.eval.model.EvalSession;
import com.gumtree.csagent.eval.model.GradeResult;
import com.gumtree.csagent.eval.model.SessionResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * LLM-based grader: assesses whether clarification questions asked by the bot were necessary.
 * Only applicable for sessions from the clarification dataset.
 */
@Slf4j
@Component
public class ClarificationGrader {

    private static final String NAME = "clarification";

    private static final String SYSTEM_PROMPT = """
            You are an evaluation judge for a customer service bot's clarification behavior.
            The bot should only ask clarification questions when the customer's issue is genuinely ambiguous
            and the clarification will change the bot's response path. Over-clarification wastes the customer's time.

            Given the customer's issue context and the bot's clarification questions, determine:
            NECESSARY - the clarification questions were needed to properly diagnose/resolve the issue
            UNNECESSARY - the bot had enough information and should not have asked for clarification
            OVER_CLARIFIED - the bot asked too many clarification questions when fewer would suffice

            Respond with ONLY one of: NECESSARY, UNNECESSARY, or OVER_CLARIFIED, followed by a brief explanation.
            """;

    private final EvalConfig config;
    private final RestTemplate restTemplate;

    public ClarificationGrader(EvalConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    public GradeResult grade(EvalSession session, SessionResult result) {
        if (!result.isReplaySuccess()) {
            return GradeResult.skip(NAME, "Replay failed");
        }

        // Only applicable for clarification-relevant sessions
        boolean isClarificationDataset = "clarification_dataset".equals(session.getSourceDataset());
        String clarificationCount = session.getExtraFields().get("clarification_count");
        boolean hasClarificationData = isClarificationDataset
                || (clarificationCount != null && !"0".equals(clarificationCount));

        if (!hasClarificationData && result.getBotResponses().stream().noneMatch(this::looksLikeClarification)) {
            return GradeResult.skip(NAME, "No clarification behavior detected");
        }

        // Build context
        String customerContext = buildCustomerContext(session);
        String botOutput = String.join("\n---\n", result.getBotResponses());

        try {
            String verdict = callLlmJudge(customerContext, botOutput);
            boolean passed = verdict.toUpperCase().startsWith("NECESSARY");
            boolean overClarified = verdict.toUpperCase().startsWith("OVER_CLARIFIED");

            return GradeResult.builder()
                    .graderName(NAME)
                    .passed(passed)
                    .score(passed ? 1.0 : (overClarified ? 0.5 : 0.0))
                    .detail(verdict)
                    .metadata(Map.of(
                            "verdict", passed ? "NECESSARY" : (overClarified ? "OVER_CLARIFIED" : "UNNECESSARY"),
                            "source_dataset", session.getSourceDataset()
                    ))
                    .build();
        } catch (Exception e) {
            log.warn("LLM clarification check failed for session {}: {}",
                    session.getId(), e.getMessage());
            return GradeResult.error(NAME, "LLM call failed: " + e.getMessage());
        }
    }

    private boolean looksLikeClarification(String response) {
        String lower = response.toLowerCase();
        return lower.contains("could you") || lower.contains("can you clarify")
                || lower.contains("could you confirm") || lower.contains("can you provide")
                || lower.contains("could you tell me") || lower.contains("what is your");
    }

    private String buildCustomerContext(EvalSession session) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("Topic: ").append(session.getSubject()).append("\n");
        if (session.getDescription() != null) {
            ctx.append("Description: ").append(session.getDescription()).append("\n");
        }
        ctx.append("Primary UC: ").append(session.getEffectivePrimaryUc()).append("\n");
        ctx.append("Has identifier collection: ").append(session.isHasIdentifierCollection()).append("\n");
        if (session.isHrAnnotated()) {
            if (StringUtils.isNotBlank(session.getHrTranscriptSummary())) {
                ctx.append("Summary: ").append(session.getHrTranscriptSummary()).append("\n");
            }
        }
        return ctx.toString();
    }

    @SuppressWarnings("unchecked")
    private String callLlmJudge(String customerContext, String botOutput) {
        String userPrompt = String.format("""
                Customer context:
                %s

                Bot responses (may include clarification questions):
                %s

                Were the bot's clarification questions NECESSARY, UNNECESSARY, or OVER_CLARIFIED?
                """, customerContext, botOutput);

        Map<String, Object> request = Map.of(
                "model", config.getLlmModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 200,
                "temperature", 0.0
        );

        String url = config.getLlmBaseUrl() + "/v1/chat/completions";
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        if (response != null && response.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }
        return "NECESSARY (no LLM response, defaulting to pass)";
    }
}
