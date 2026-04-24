package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.KnowledgeHit;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds the projected context JSON for LLM invocation.
 * Combines session state, form context, conversation history, knowledge results,
 * and current phase into a single context document.
 */
@Slf4j
@Service
public class ContextProjectionBuilder {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
    );

    private final ObjectMapper objectMapper;
    private final UseCaseRegistryService useCaseRegistry;
    private final ControlPolicyService controlPolicy;
    private final ToolPolicyEnforcer toolPolicyEnforcer;

    public ContextProjectionBuilder(ObjectMapper objectMapper,
                                     UseCaseRegistryService useCaseRegistry,
                                     ControlPolicyService controlPolicy,
                                     ToolPolicyEnforcer toolPolicyEnforcer) {
        this.objectMapper = objectMapper;
        this.useCaseRegistry = useCaseRegistry;
        this.controlPolicy = controlPolicy;
        this.toolPolicyEnforcer = toolPolicyEnforcer;
    }

    /**
     * Build a projected context JSON string for the LLM.
     *
     * @param session          current bot session
     * @param conversationHistory previous turns in the session
     * @param knowledgeHits    retrieved knowledge articles (may be null)
     * @param userMessage      the current user message
     * @return JSON string representing the full projected context
     */
    public String buildProjection(BotSession session,
                                   List<BotTurn> conversationHistory,
                                   List<KnowledgeHit> knowledgeHits,
                                   String userMessage) {
        try {
            ObjectNode projection = objectMapper.createObjectNode();

            // Session state
            ObjectNode sessionNode = objectMapper.createObjectNode();
            sessionNode.put("session_id", session.getSessionId());
            sessionNode.put("current_phase", session.getCurrentPhase());
            sessionNode.put("active_use_case", session.getActiveUseCase());
            sessionNode.put("total_bot_turns", session.getTotalBotTurns());
            sessionNode.put("clarification_count", session.getClarificationCount());
            sessionNode.put("faq_miss_count", session.getFaqMissCount());
            sessionNode.put("handling_state", session.getHandlingState());
            projection.set("session", sessionNode);

            // Task summary
            String activeUc = session.getActiveUseCase();
            String taskSummary = buildTaskSummary(session);
            projection.put("task_summary", taskSummary);

            // Allowed actions based on phase + UC type
            ArrayNode allowedActionsNode = objectMapper.createArrayNode();
            for (String action : getAllowedActions(session)) {
                allowedActionsNode.add(action);
            }
            projection.set("allowed_actions", allowedActionsNode);

            // Risk flags from UC registry
            ArrayNode riskFlagsNode = objectMapper.createArrayNode();
            if (activeUc != null) {
                UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(activeUc);
                if (ucDef != null && ucDef.riskLevel() != null) {
                    riskFlagsNode.add(ucDef.riskLevel());
                }
            }
            projection.set("risk_flags", riskFlagsNode);

            // Budget state
            ObjectNode budgetNode = objectMapper.createObjectNode();
            budgetNode.put("total_bot_turns", session.getTotalBotTurns());
            budgetNode.put("max_bot_turns", getMaxBotTurns(session));
            budgetNode.put("clarification_count", session.getClarificationCount());
            budgetNode.put("max_clarification", controlPolicy.getMaxClarificationRounds());
            budgetNode.put("faq_miss_count", session.getFaqMissCount());
            budgetNode.put("max_faq_miss", controlPolicy.getMaxFaqMiss());
            projection.set("budget_state", budgetNode);

            // Tool schemas (visible tools for current UC)
            ArrayNode toolSchemasNode = objectMapper.createArrayNode();
            if (activeUc != null) {
                for (String toolName : toolPolicyEnforcer.getVisibleToolsForUc(activeUc)) {
                    toolSchemasNode.add(toolName);
                }
            }
            projection.set("tool_schemas", toolSchemasNode);

            // Form context (pre-chat form data)
            if (session.getFormContext() != null && !session.getFormContext().isBlank()) {
                projection.set("form_context", objectMapper.readTree(session.getFormContext()));
            }

            // Customer context
            if (session.getCustomerContext() != null && !session.getCustomerContext().isBlank()) {
                projection.set("customer_context", objectMapper.readTree(session.getCustomerContext()));
            }

            // Listing context
            if (session.getListingContext() != null && !session.getListingContext().isBlank()) {
                projection.set("listing_context", objectMapper.readTree(session.getListingContext()));
            }

            // Conversation history (last 10 turns max to limit context window)
            ArrayNode historyNode = objectMapper.createArrayNode();
            int startIdx = Math.max(0, conversationHistory.size() - 10);
            for (int i = startIdx; i < conversationHistory.size(); i++) {
                BotTurn turn = conversationHistory.get(i);
                ObjectNode turnNode = objectMapper.createObjectNode();
                turnNode.put("turn_index", turn.getTurnIndex());
                turnNode.put("user_message", redactPii(turn.getUserMessage()));
                turnNode.put("bot_response", turn.getBotResponse());
                turnNode.put("action", turn.getActionSelected());
                historyNode.add(turnNode);
            }
            projection.set("conversation_history", historyNode);

            // Knowledge hits (if any)
            if (knowledgeHits != null && !knowledgeHits.isEmpty()) {
                ArrayNode hitsNode = objectMapper.createArrayNode();
                for (KnowledgeHit hit : knowledgeHits) {
                    ObjectNode hitNode = objectMapper.createObjectNode();
                    hitNode.put("source_id", hit.getSourceId());
                    hitNode.put("title", hit.getTitle());
                    hitNode.put("snippet", hit.getSnippet());
                    hitNode.put("score", hit.getScore());
                    hitsNode.add(hitNode);
                }
                projection.set("knowledge_hits", hitsNode);

                // Phase-aware instruction: tell the LLM knowledge is pre-searched
                projection.put("knowledge_instruction",
                        "Knowledge results have already been retrieved and are provided above in 'knowledge_hits'. "
                        + "Do NOT return action 'retrieve_knowledge'. "
                        + "Use action 'answer_grounded' to respond based on the provided knowledge, "
                        + "or 'ask_user' if the knowledge does not address the user's question.");
            }

            // Current user message
            projection.put("current_user_message", redactPii(userMessage));

            return objectMapper.writeValueAsString(projection);

        } catch (Exception e) {
            log.error("Failed to build context projection: {}", e.getMessage(), e);
            return "{}";
        }
    }

    /**
     * Basic PII redaction: replace email addresses with placeholder.
     * Full PII handling is deferred to DM6.
     */
    private String redactPii(String text) {
        if (text == null) {
            return null;
        }
        return EMAIL_PATTERN.matcher(text).replaceAll("[REDACTED_EMAIL]");
    }

    /**
     * Build a human-readable task summary from session state.
     */
    private String buildTaskSummary(BotSession session) {
        StringBuilder sb = new StringBuilder();
        String topicSubject = session.getFormTopicSubject();
        String activeUc = session.getActiveUseCase();
        String phase = session.getCurrentPhase();

        if (topicSubject != null) {
            sb.append("User inquiry about ").append(topicSubject).append(". ");
        }
        if (activeUc != null) {
            UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(activeUc);
            String ucName = ucDef != null ? ucDef.name() : activeUc;
            sb.append(ucName).append(" detected");
            if (session.getIntentConfidence() != null) {
                sb.append(" with ").append(session.getIntentConfidence()).append(" confidence");
            }
            sb.append(". ");
        }
        if (phase != null) {
            sb.append("Current phase: ").append(phase).append(".");
        }
        return sb.toString().trim();
    }

    /**
     * Get allowed actions based on current phase and UC type.
     */
    private List<String> getAllowedActions(BotSession session) {
        String phase = session.getCurrentPhase();
        String activeUc = session.getActiveUseCase();
        boolean isIntakeUc = false;

        if (activeUc != null) {
            UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(activeUc);
            isIntakeUc = ucDef != null && "INTAKE".equals(ucDef.path());
        }

        List<String> actions = new ArrayList<>();
        switch (phase != null ? phase : "") {
            case "DISCOVER":
                actions.add("ask_user");
                actions.add("escalate_human");
                break;
            case "RESOLVE":
                if (isIntakeUc) {
                    actions.add("ask_user");
                    actions.add("escalate_human");
                } else {
                    actions.add("retrieve_knowledge");
                    actions.add("answer_grounded");
                    actions.add("ask_user");
                    actions.add("escalate_human");
                    actions.add("finish");
                }
                break;
            case "CONFIRM":
                actions.add("answer_grounded");
                actions.add("escalate_human");
                actions.add("finish");
                break;
            default:
                actions.add("escalate_human");
                break;
        }
        return actions;
    }

    /**
     * Get the appropriate max bot turns based on UC type.
     */
    private int getMaxBotTurns(BotSession session) {
        String activeUc = session.getActiveUseCase();
        if (activeUc != null) {
            UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(activeUc);
            if (ucDef != null && "INTAKE".equals(ucDef.path())) {
                return controlPolicy.getMaxBotTurnsIntake();
            }
        }
        return controlPolicy.getMaxBotTurnsFaq();
    }
}
