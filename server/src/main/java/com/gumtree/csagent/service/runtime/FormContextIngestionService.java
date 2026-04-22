package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gumtree.csagent.model.BotSession;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Parses pre-chat form data and populates the BotSession
 * with form context and initial UC candidates.
 */
@Slf4j
@Service
public class FormContextIngestionService {

    private final ObjectMapper objectMapper;
    private final UseCaseRegistryService useCaseRegistry;

    public FormContextIngestionService(ObjectMapper objectMapper,
                                        UseCaseRegistryService useCaseRegistry) {
        this.objectMapper = objectMapper;
        this.useCaseRegistry = useCaseRegistry;
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

        } catch (Exception e) {
            log.error("Failed to ingest form context for session {}: {}",
                    session.getSessionId(), e.getMessage(), e);
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
