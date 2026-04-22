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
 * LLM-based grader: checks whether bot responses are grounded in provided knowledge.
 * Uses the configured LLM endpoint to judge: GROUNDED or NOT_GROUNDED.
 */
@Slf4j
@Component
public class GroundednessGrader {

    private static final String NAME = "groundedness";
    private static final String SYSTEM_PROMPT = """
            You are an evaluation judge. Given a customer service bot response and the knowledge context it should be grounded in,
            determine if the response is GROUNDED (all claims are supported by the knowledge context) or NOT_GROUNDED
            (contains claims not supported by the knowledge context).

            Respond with ONLY "GROUNDED" or "NOT_GROUNDED" followed by a brief explanation.
            """;

    private final EvalConfig config;
    private final RestTemplate restTemplate;

    public GroundednessGrader(EvalConfig config, RestTemplate restTemplate) {
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

        // Need grounding context from HR annotations
        String groundingSource = session.isHrAnnotated() ? session.getHrGroundingSource() : null;
        String knowledgeScope = session.isHrAnnotated() ? session.getHrExpectedKnowledgeScope() : null;

        if (StringUtils.isBlank(groundingSource) && StringUtils.isBlank(knowledgeScope)) {
            return GradeResult.skip(NAME, "No grounding context available for this session");
        }

        String knowledgeContext = StringUtils.isNotBlank(groundingSource)
                ? groundingSource : knowledgeScope;

        // Concatenate bot responses for evaluation
        String botOutput = String.join("\n---\n", result.getBotResponses());

        try {
            String verdict = callLlmJudge(botOutput, knowledgeContext);
            boolean grounded = verdict.toUpperCase().startsWith("GROUNDED");

            return GradeResult.builder()
                    .graderName(NAME)
                    .passed(grounded)
                    .score(grounded ? 1.0 : 0.0)
                    .detail(verdict)
                    .metadata(Map.of(
                            "knowledge_context", knowledgeContext,
                            "verdict", grounded ? "GROUNDED" : "NOT_GROUNDED"
                    ))
                    .build();
        } catch (Exception e) {
            log.warn("LLM groundedness check failed for session {}: {}",
                    session.getId(), e.getMessage());
            return GradeResult.error(NAME, "LLM call failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String callLlmJudge(String botOutput, String knowledgeContext) {
        String userPrompt = String.format("""
                Knowledge context: %s

                Bot response to evaluate:
                %s

                Is this response GROUNDED or NOT_GROUNDED in the knowledge context?
                """, knowledgeContext, botOutput);

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
        return "NOT_GROUNDED (no LLM response)";
    }
}
