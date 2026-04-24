package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.tools.GetCustomerContextTool;
import com.gumtree.csagent.service.tools.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses pre-chat form data and populates the BotSession
 * with form context and initial UC candidates.
 */
@Slf4j
@Service
public class FormContextIngestionService {

    private final ObjectMapper objectMapper;
    private final UseCaseRegistryService useCaseRegistry;
    private final GetCustomerContextTool customerContextTool;

    public FormContextIngestionService(ObjectMapper objectMapper,
                                        UseCaseRegistryService useCaseRegistry,
                                        GetCustomerContextTool customerContextTool) {
        this.objectMapper = objectMapper;
        this.useCaseRegistry = useCaseRegistry;
        this.customerContextTool = customerContextTool;
    }

    /**
     * Ingest pre-chat form data into the session.
     *
     * @param session      the bot session to populate
     * @param firstName    customer first name
     * @param email        customer email
     * @param topicSubject the selected topic from the form
     * @param adId         optional ad ID
     * @param description  customer's description of their issue
     */
    public void ingest(BotSession session, String firstName, String email,
                       String topicSubject, String adId, String description) {
        try {
            // Sanitize all user-provided form fields to prevent XSS
            firstName = sanitize(firstName);
            email = sanitize(email);
            topicSubject = sanitize(topicSubject);
            adId = sanitize(adId);
            description = sanitize(description);

            // Build form context JSON
            ObjectNode formContext = objectMapper.createObjectNode();
            formContext.put("first_name", firstName);
            formContext.put("email", email);
            formContext.put("topic_subject", topicSubject);
            formContext.put("description", description);
            if (!adId.isBlank()) {
                formContext.put("ad_id", adId);
            }

            session.setFormContext(objectMapper.writeValueAsString(formContext));
            session.setFormTopicSubject(topicSubject);

            // Determine initial UC candidates based on topic_subject
            List<String> candidates = useCaseRegistry.getCandidateUcsForTopic(topicSubject);
            session.setCandidateUseCases(candidates.toArray(new String[0]));

            log.info("Session {}: form context ingested, topicSubject='{}', candidates={}",
                    session.getSessionId(), topicSubject, candidates);

            // Auto-trigger get_customer_context if email is present and any candidate UC allows it
            autoTriggerCustomerContext(session, email, adId, candidates);

        } catch (Exception e) {
            log.error("Failed to ingest form context for session {}: {}",
                    session.getSessionId(), e.getMessage(), e);
        }
    }

    /**
     * Auto-trigger get_customer_context when email is present and at least
     * one candidate UC is in the allowed set (UC-A, UC-C, UC-D, UC-F, UC-FP, UC-K).
     * Results are stored in session.customerContext, listingContext, moderationContext.
     * Non-blocking: failures are logged but do not prevent session from proceeding.
     */
    private void autoTriggerCustomerContext(BotSession session, String email, String adId, List<String> candidateUcs) {
        if (email == null || email.isBlank()) {
            return;
        }

        Set<String> allowedUcs = Set.of("UC-A", "UC-C", "UC-D", "UC-F", "UC-FP", "UC-K");
        boolean hasAllowedUc = candidateUcs.stream().anyMatch(allowedUcs::contains);
        if (!hasAllowedUc) {
            log.debug("Session {}: skipping auto-trigger get_customer_context — no candidate UC in allowed set",
                    session.getSessionId());
            return;
        }

        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("email", email);
            if (adId != null && !adId.isBlank()) {
                params.put("ad_id", adId);
            }

            ToolResult result = customerContextTool.execute(session, params);

            if (result.isSuccess() && result.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.getData();

                if (data.containsKey("account")) {
                    session.setCustomerContext(objectMapper.writeValueAsString(data.get("account")));
                }
                if (data.containsKey("listing")) {
                    session.setListingContext(objectMapper.writeValueAsString(data.get("listing")));
                }
                if (data.containsKey("moderation_review")) {
                    session.setModerationContext(objectMapper.writeValueAsString(data.get("moderation_review")));
                }

                log.info("Session {}: auto-triggered get_customer_context — account={}, listing={}, moderation={}",
                        session.getSessionId(),
                        data.containsKey("account"),
                        data.containsKey("listing"),
                        data.containsKey("moderation_review"));
            } else {
                log.warn("Session {}: get_customer_context returned no data or error: {}",
                        session.getSessionId(), result.isSuccess() ? "empty" : "error");
            }
        } catch (Exception e) {
            log.warn("Session {}: auto-trigger get_customer_context failed (non-blocking): {}",
                    session.getSessionId(), e.getMessage());
        }
    }

    /**
     * Strip all HTML tags from a user-provided form field to prevent XSS.
     * Returns empty string for null input.
     */
    static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return Jsoup.clean(input, Safelist.none()).trim();
    }
}
