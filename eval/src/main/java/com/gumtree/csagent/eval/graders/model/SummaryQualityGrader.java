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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM-based grader: rates the quality of handover summaries on a 1-5 scale.
 * Only applicable when a session is escalated.
 */
@Slf4j
@Component
public class SummaryQualityGrader {

    private static final String NAME = "summary_quality";
    private static final double PASS_THRESHOLD = 3.0;
    private static final Pattern SCORE_PATTERN = Pattern.compile("\\b([1-5])\\b");

    private static final String SYSTEM_PROMPT = """
            You are an evaluation judge for a customer service bot's handover summary quality.
            When the bot escalates to a human agent, it should provide a summary that includes:
            - The customer's issue (what they contacted about)
            - What the bot tried (diagnostics, tools used)
            - Why it's escalating (escalation trigger)
            - Relevant context (UC, identifiers, knowledge found)

            Rate the handover summary quality on a scale of 1-5:
            1 = No useful summary provided
            2 = Minimal summary, missing most key context
            3 = Partial summary, covers the issue but missing diagnostics or escalation reason
            4 = Good summary, covers issue + what was tried + why escalating
            5 = Excellent summary, complete context for the human agent

            Respond with ONLY a number (1-5) followed by a brief explanation.
            """;

    private final EvalConfig config;
    private final RestTemplate restTemplate;

    public SummaryQualityGrader(EvalConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    public GradeResult grade(EvalSession session, SessionResult result) {
        if (!result.isReplaySuccess()) {
            return GradeResult.skip(NAME, "Replay failed");
        }

        // Only applicable when escalation occurred
        boolean escalated = "ESCALATED".equalsIgnoreCase(result.getBotHandlingState())
                || StringUtils.isNotBlank(result.getBotEscalationReason());
        if (!escalated) {
            return GradeResult.skip(NAME, "Session was not escalated, summary quality N/A");
        }

        // Build the summary context from session state
        Map<String, Object> state = result.getBotSessionState();
        String summaryContext = buildSummaryContext(state, result);

        if (StringUtils.isBlank(summaryContext)) {
            return GradeResult.fail(NAME, "Escalated but no summary context available in session state");
        }

        try {
            String verdict = callLlmJudge(session, summaryContext);
            double score = parseScore(verdict);

            return GradeResult.builder()
                    .graderName(NAME)
                    .passed(score >= PASS_THRESHOLD)
                    .score(score / 5.0)
                    .detail(String.format("Summary quality: %.0f/5. %s", score, verdict))
                    .metadata(Map.of("raw_score", score, "threshold", PASS_THRESHOLD))
                    .build();
        } catch (Exception e) {
            log.warn("LLM summary quality check failed for session {}: {}",
                    session.getId(), e.getMessage());
            return GradeResult.error(NAME, "LLM call failed: " + e.getMessage());
        }
    }

    private String buildSummaryContext(Map<String, Object> state, SessionResult result) {
        if (state == null || state.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Active UC: ").append(state.getOrDefault("activeUseCase", "unknown")).append("\n");
        sb.append("Escalation reason: ").append(state.getOrDefault("escalationReason", "unknown")).append("\n");
        sb.append("Form topic: ").append(state.getOrDefault("formTopicSubject", "")).append("\n");

        Object formCtx = state.get("formContext");
        if (formCtx != null) {
            sb.append("Form context: ").append(formCtx).append("\n");
        }
        Object custCtx = state.get("customerContext");
        if (custCtx != null) {
            sb.append("Customer context: ").append(custCtx).append("\n");
        }

        sb.append("\nBot responses during session:\n");
        for (String response : result.getBotResponses()) {
            sb.append("- ").append(response.length() > 200 ? response.substring(0, 200) + "..." : response).append("\n");
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String callLlmJudge(EvalSession session, String summaryContext) {
        String userPrompt = String.format("""
                Customer's original issue: %s - %s
                Expected UC: %s

                Handover summary context:
                %s

                Rate the handover summary quality (1-5):
                """, session.getSubject(),
                session.getDescription() != null ? session.getDescription() : "",
                session.getEffectivePrimaryUc(),
                summaryContext);

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
        return "3 (no LLM response)";
    }

    private double parseScore(String verdict) {
        Matcher m = SCORE_PATTERN.matcher(verdict);
        if (m.find()) return Double.parseDouble(m.group(1));
        return 3.0;
    }
}
