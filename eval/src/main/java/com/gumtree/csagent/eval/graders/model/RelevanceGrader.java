package com.gumtree.csagent.eval.graders.model;

import com.gumtree.csagent.eval.config.EvalConfig;
import com.gumtree.csagent.eval.model.EvalSession;
import com.gumtree.csagent.eval.model.GradeResult;
import com.gumtree.csagent.eval.model.SessionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM-based grader: rates the relevance of bot responses on a 1-5 scale.
 */
@Slf4j
@Component
public class RelevanceGrader {

    private static final String NAME = "relevance";
    private static final double PASS_THRESHOLD = 3.0;
    private static final Pattern SCORE_PATTERN = Pattern.compile("\\b([1-5])\\b");

    private static final String SYSTEM_PROMPT = """
            You are an evaluation judge for a customer service bot. Rate the relevance of the bot's response
            to the customer's issue on a scale of 1-5:

            1 = Completely irrelevant, does not address the customer's issue at all
            2 = Mostly irrelevant, tangentially related but unhelpful
            3 = Somewhat relevant, addresses the general topic but misses key points
            4 = Mostly relevant, addresses the issue with minor gaps
            5 = Highly relevant, directly and completely addresses the customer's issue

            Respond with ONLY a number (1-5) followed by a brief explanation.
            """;

    private final EvalConfig config;
    private final RestTemplate restTemplate;

    public RelevanceGrader(EvalConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    public GradeResult grade(EvalSession session, SessionResult result) {
        if (!result.isReplaySuccess()) {
            return GradeResult.skip(NAME, "Replay failed");
        }

        if (result.getBotResponses().isEmpty()) {
            return GradeResult.skip(NAME, "No bot responses to evaluate");
        }

        String customerIssue = buildCustomerContext(session);
        String botOutput = String.join("\n---\n", result.getBotResponses());

        try {
            String verdict = callLlmJudge(customerIssue, botOutput);
            double score = parseScore(verdict);

            return GradeResult.builder()
                    .graderName(NAME)
                    .passed(score >= PASS_THRESHOLD)
                    .score(score / 5.0) // Normalize to 0-1
                    .detail(String.format("Relevance score: %.0f/5. %s", score, verdict))
                    .metadata(Map.of(
                            "raw_score", score,
                            "threshold", PASS_THRESHOLD
                    ))
                    .build();
        } catch (Exception e) {
            log.warn("LLM relevance check failed for session {}: {}",
                    session.getId(), e.getMessage());
            return GradeResult.error(NAME, "LLM call failed: " + e.getMessage());
        }
    }

    private String buildCustomerContext(EvalSession session) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("Topic: ").append(session.getSubject()).append("\n");
        if (session.getDescription() != null) {
            ctx.append("Description: ").append(session.getDescription()).append("\n");
        }
        ctx.append("Primary UC: ").append(session.getEffectivePrimaryUc()).append("\n");
        if (session.isHrAnnotated() && session.getHrTranscriptSummary() != null) {
            ctx.append("Summary: ").append(session.getHrTranscriptSummary()).append("\n");
        }
        return ctx.toString();
    }

    @SuppressWarnings("unchecked")
    private String callLlmJudge(String customerIssue, String botOutput) {
        String userPrompt = String.format("""
                Customer issue:
                %s

                Bot response:
                %s

                Rate the relevance (1-5):
                """, customerIssue, botOutput);

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
        return "3 (no LLM response, defaulting to neutral)";
    }

    private double parseScore(String verdict) {
        Matcher m = SCORE_PATTERN.matcher(verdict);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return 3.0; // default middle score if parsing fails
    }
}
