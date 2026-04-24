package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.*;
import com.gumtree.csagent.service.guardrails.ScriptLibraryService;
import com.gumtree.csagent.service.knowledge.KnowledgeSearchService;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import com.gumtree.csagent.service.tools.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Per-phase logic evaluator. Determines what action to take based on
 * the current session phase and state.
 */
@Slf4j
@Service
public class PhaseEvaluator {

    /** Intake-only UCs that should never invoke knowledge search. */
    private static final Set<String> INTAKE_UCS = Set.of("UC-G", "UC-H", "UC-I", "UC-J", "UC-K");

    /** Map from intake UC ID to its script template prefix. */
    private static final Map<String, String> INTAKE_TEMPLATE_PREFIX = Map.of(
            "UC-G", "g",
            "UC-H", "h",
            "UC-I", "i",
            "UC-J", "j",
            "UC-K", "k"
    );

    /** Opening template keys per intake UC (first available is used). */
    private static final Map<String, List<String>> INTAKE_OPENING_TEMPLATES = Map.of(
            "UC-G", List.of("g_process_explanation"),
            "UC-H", List.of("h_empathy"),
            "UC-I", List.of("i_disclaimer"),
            "UC-J", List.of("j_acknowledgment"),
            "UC-K", List.of("k_basic_troubleshoot")
    );

    /** Map from intake UC ID to the human-readable team name for template variable substitution. */
    private static final Map<String, String> UC_TEAM_NAME = Map.of(
            "UC-G", "Data Protection",
            "UC-H", "Ad Support",
            "UC-I", "Payments",
            "UC-J", "Trust & Safety",
            "UC-K", "Technical Support"
    );

    /** Default SLA hours used in template variable substitution. */
    private static final String DEFAULT_SLA_HOURS = "24-48";

    private final UseCaseRegistryService useCaseRegistry;
    private final KnowledgeSearchService knowledgeSearchService;
    private final ScriptLibraryService scriptLibrary;
    private final LlmInvocationService llmInvocation;
    private final ContextProjectionBuilder contextProjection;
    private final ActionParser actionParser;
    private final ObjectMapper objectMapper;
    private final CreateCaseControlledTool createCaseTool;
    private final EventEmitter eventEmitter;
    private final ToolDispatcher toolDispatcher;

    public PhaseEvaluator(UseCaseRegistryService useCaseRegistry,
                          KnowledgeSearchService knowledgeSearchService,
                          ScriptLibraryService scriptLibrary,
                          LlmInvocationService llmInvocation,
                          ContextProjectionBuilder contextProjection,
                          ActionParser actionParser,
                          ObjectMapper objectMapper,
                          CreateCaseControlledTool createCaseTool,
                          EventEmitter eventEmitter,
                          ToolDispatcher toolDispatcher) {
        this.useCaseRegistry = useCaseRegistry;
        this.knowledgeSearchService = knowledgeSearchService;
        this.scriptLibrary = scriptLibrary;
        this.llmInvocation = llmInvocation;
        this.contextProjection = contextProjection;
        this.actionParser = actionParser;
        this.objectMapper = objectMapper;
        this.createCaseTool = createCaseTool;
        this.eventEmitter = eventEmitter;
        this.toolDispatcher = toolDispatcher;
    }

    /**
     * Evaluate the current phase and produce a PhaseResult with the action to take
     * and the next phase to transition to.
     */
    public PhaseResult evaluate(BotSession session, String userMessage,
                                 List<BotTurn> conversationHistory) {
        String phase = session.getCurrentPhase();
        log.debug("Session {}: evaluating phase {}", session.getSessionId(), phase);

        return switch (phase) {
            case "DISCOVER" -> evaluateDiscover(session, userMessage, conversationHistory);
            case "RESOLVE" -> evaluateResolve(session, userMessage, conversationHistory);
            case "CONFIRM" -> evaluateConfirm(session, userMessage, conversationHistory);
            case "CLOSE" -> evaluateClose(session);
            case "ESCALATE" -> evaluateEscalate(session, conversationHistory);
            default -> {
                log.warn("Session {}: unexpected phase '{}'", session.getSessionId(), phase);
                yield PhaseResult.escalate(session,
                        "I'm having trouble processing your request. Let me connect you with a human agent.",
                        "unexpected_phase");
            }
        };
    }

    private PhaseResult evaluateDiscover(BotSession session, String userMessage,
                                          List<BotTurn> conversationHistory) {
        String activeUc = session.getActiveUseCase();

        // If UC already identified, move to RESOLVE
        if (activeUc != null && !activeUc.isBlank()) {
            UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(activeUc);
            if (ucDef != null) {
                log.info("Session {}: UC {} identified, transitioning to RESOLVE", session.getSessionId(), activeUc);
                return PhaseResult.transition(session, "RESOLVE", null, "use_case_identified");
            }
        }

        // UC not yet identified — use LLM to ask clarifying question
        String projection = contextProjection.buildProjection(session, conversationHistory, null, userMessage);
        LlmResponse llmResponse = llmInvocation.invokeChat(projection, userMessage);
        ParsedAction action = actionParser.parse(llmResponse.getContent());

        // Track clarification
        if ("ask_user".equals(action.getAction())) {
            session.setClarificationCount(session.getClarificationCount() + 1);
            eventEmitter.emitClarificationAsked(session.getSessionId(), session.getTotalBotTurns(),
                    session.getClarificationCount());
        }

        // If LLM determined escalation (e.g. safe fallback during API failure), escalate immediately
        if ("escalate_human".equals(action.getAction())) {
            return PhaseResult.escalate(session, action.getUserMessage(),
                "llm_determined_escalation");
        }

        return PhaseResult.respond(session, action, llmResponse, null);
    }

    private PhaseResult evaluateResolve(BotSession session, String userMessage,
                                         List<BotTurn> conversationHistory) {
        String activeUc = session.getActiveUseCase();
        UseCaseRegistryService.UseCaseDefinition ucDef = useCaseRegistry.getUseCase(activeUc);

        if (ucDef == null) {
            return PhaseResult.escalate(session,
                    "I'm having trouble identifying how to help you. Let me connect you with a human agent.",
                    "unknown_use_case");
        }

        // Intake UCs (UC-G/H/I/J/K) must NEVER invoke knowledge search —
        // they use fixed script templates + ask_user for intake field collection.
        if ("INTAKE".equals(ucDef.path()) || INTAKE_UCS.contains(activeUc)) {
            return resolveIntake(session, userMessage, conversationHistory, ucDef);
        }

        // FAQ UCs proceed with knowledge search
        return resolveFaq(session, userMessage, conversationHistory, ucDef);
    }

    private PhaseResult resolveFaq(BotSession session, String userMessage,
                                    List<BotTurn> conversationHistory,
                                    UseCaseRegistryService.UseCaseDefinition ucDef) {
        // Step 1: Search knowledge base via ToolDispatcher for policy enforcement
        List<String> ucTags = List.of(ucDef.ucId());
        KnowledgeSearchResult searchResult;

        ToolResult toolResult = toolDispatcher.dispatch("search_knowledge", session,
                Map.of("query", userMessage, "uc_tags", ucTags));

        if (!toolResult.isSuccess()) {
            log.warn("Session {}: search_knowledge blocked or failed: {}",
                    session.getSessionId(), toolResult.getErrorMessage());
            return PhaseResult.escalate(session,
                    "I need to connect you with a specialist who can help with this.",
                    "tool_scope_blocked");
        }

        searchResult = reconstructSearchResult(toolResult.getData());

        // Emit RETRIEVAL_EXECUTED event
        eventEmitter.emitRetrievalExecuted(session.getSessionId(), session.getTotalBotTurns(),
                userMessage, searchResult.isFaqMiss(),
                searchResult.getHits() != null ? searchResult.getHits().size() : 0);

        // Step 2: Check for FAQ miss
        if (searchResult.isFaqMiss()) {
            session.setFaqMissCount(session.getFaqMissCount() + 1);
            log.info("Session {}: FAQ miss #{}", session.getSessionId(), session.getFaqMissCount());

            // If we've hit the FAQ miss limit, escalate
            // (BudgetChecker will catch this on next turn, but be proactive)
            if (session.getFaqMissCount() >= 2) {
                return PhaseResult.escalate(session,
                        "I wasn't able to find a clear answer to your question. Let me connect you with a specialist who can help.",
                        "faq_miss_exceeded");
            }

            // Ask user to rephrase
            return PhaseResult.respond(session,
                    ParsedAction.builder()
                            .action("ask_user")
                            .parameters(Map.of())
                            .userMessage("I couldn't find a specific answer to that. Could you describe your issue in a bit more detail?")
                            .reasoning("FAQ miss - asking for clarification")
                            .build(),
                    null, null);
        }

        // Step 3: Knowledge found — use LLM to generate grounded answer
        String projection = contextProjection.buildProjection(
                session, conversationHistory, searchResult.getHits(), userMessage);
        LlmResponse llmResponse = llmInvocation.invokeChat(projection, userMessage);
        ParsedAction action = actionParser.parse(llmResponse.getContent());

        // Track articles shown
        if (searchResult.getHits() != null && !searchResult.getHits().isEmpty()) {
            String[] sourceIds = searchResult.getHits().stream()
                    .map(KnowledgeHit::getSourceId)
                    .toArray(String[]::new);
            // Merge with existing articles shown
            String[] existing = session.getArticlesShown();
            if (existing != null) {
                String[] merged = new String[existing.length + sourceIds.length];
                System.arraycopy(existing, 0, merged, 0, existing.length);
                System.arraycopy(sourceIds, 0, merged, existing.length, sourceIds.length);
                session.setArticlesShown(merged);
            } else {
                session.setArticlesShown(sourceIds);
            }

            // Emit ARTICLE_SHOWN event for each hit
            for (KnowledgeHit hit : searchResult.getHits()) {
                eventEmitter.emitArticleShown(session.getSessionId(), session.getTotalBotTurns(),
                        hit.getSourceId(), hit.getTitle());
            }
        }

        // If the LLM responded with answer_grounded, transition to CONFIRM
        if ("answer_grounded".equals(action.getAction()) || "finish".equals(action.getAction())) {
            return PhaseResult.transitionWithResponse(session, "CONFIRM", action, llmResponse, "answer_provided", searchResult.getHits());
        }

        // Defense-in-depth: if LLM returned retrieve_knowledge despite knowledge being
        // pre-loaded in context, re-invoke once with an explicit grounding instruction.
        // This adds latency (double LLM call) but prevents the user from seeing
        // an intermediate "Let me check..." message with no follow-up.
        if ("retrieve_knowledge".equals(action.getAction())) {
            log.warn("Session {}: LLM returned retrieve_knowledge despite pre-loaded knowledge. "
                    + "Re-invoking with explicit grounding instruction.", session.getSessionId());

            String retryProjection = contextProjection.buildProjection(
                    session, conversationHistory, searchResult.getHits(), userMessage);
            String groundingOverride = "IMPORTANT: Knowledge articles have already been retrieved "
                    + "and are included in this context under 'knowledge_hits'. "
                    + "You MUST use action 'answer_grounded' now and compose a helpful response "
                    + "based on the provided knowledge snippets. Do NOT return 'retrieve_knowledge'.";
            LlmResponse retryResponse = llmInvocation.invokeChat(
                    retryProjection, groundingOverride + "\n\nUser question: " + userMessage);
            ParsedAction retryAction = actionParser.parse(retryResponse.getContent());

            if ("answer_grounded".equals(retryAction.getAction()) || "finish".equals(retryAction.getAction())) {
                return PhaseResult.transitionWithResponse(session, "CONFIRM", retryAction, retryResponse, "answer_provided_retry", searchResult.getHits());
            }
            // If still not answer_grounded, use whatever message the LLM produced
            return PhaseResult.respond(session, retryAction, retryResponse, searchResult.getHits());
        }

        // If LLM determined escalation (e.g. safe fallback during API failure), escalate immediately
        if ("escalate_human".equals(action.getAction())) {
            return PhaseResult.escalate(session, action.getUserMessage(),
                "llm_determined_escalation");
        }

        return PhaseResult.respond(session, action, llmResponse, searchResult.getHits());
    }

    private PhaseResult resolveIntake(BotSession session, String userMessage,
                                       List<BotTurn> conversationHistory,
                                       UseCaseRegistryService.UseCaseDefinition ucDef) {
        // INTAKE path: use fixed script templates for intake field collection,
        // then escalate. Intake UCs never self-resolve and never call search_knowledge.
        String activeUc = ucDef.ucId();
        String prefix = INTAKE_TEMPLATE_PREFIX.getOrDefault(activeUc, activeUc.toLowerCase().replace("uc-", ""));
        Map<String, String> vars = buildIntakeVariables(session, activeUc);

        // On the first intake turn, send the opening + intake prompt template.
        // Guard: use lastAction — if it's already set, the opening template was already sent.
        // (conversationHistory may be empty because BotTurns are persisted after response)
        if (session.getLastAction() == null) {
            String empathy = null;
            List<String> openingKeys = INTAKE_OPENING_TEMPLATES.getOrDefault(activeUc, List.of());
            for (String key : openingKeys) {
                empathy = scriptLibrary.renderTemplate(key, vars);
                if (empathy != null) break;
            }
            String intakePrompt = scriptLibrary.renderTemplate(prefix + "_intake_prompt", vars);

            String message;
            if (empathy != null && intakePrompt != null) {
                message = empathy + "\n\n" + intakePrompt;
            } else if (intakePrompt != null) {
                message = intakePrompt;
            } else {
                // Fallback: use LLM to generate an intake prompt
                message = "I'd like to help with this. Could you provide some more details so I can pass this to the right team?";
            }

            return PhaseResult.respond(session,
                    ParsedAction.builder()
                            .action("ask_user")
                            .parameters(Map.of("template", prefix + "_intake_prompt"))
                            .userMessage(message)
                            .reasoning("Intake UC " + activeUc + ": presenting intake template")
                            .build(),
                    null, null);
        }

        // Subsequent turns: use LLM to decide if intake is complete or more info needed.
        // Prepend an intake-specific instruction so the LLM knows NOT to search knowledge
        // and instead focuses on collecting remaining fields or escalating.
        String intakeInstruction = "You are collecting information for a " + UC_TEAM_NAME.getOrDefault(activeUc, "support")
                + " case (use case " + activeUc + "). "
                + "You must ONLY use action 'ask_user' to collect remaining required details, "
                + "or 'escalate_human' when you have enough information to hand over to the team. "
                + "Do NOT use 'retrieve_knowledge' — this is an intake flow, not a FAQ flow. "
                + "Acknowledge what the user provided, then ask for anything still missing or escalate.";

        String projection = contextProjection.buildProjection(session, conversationHistory, null, userMessage);
        LlmResponse llmResponse = llmInvocation.invokeChat(
                projection, intakeInstruction + "\n\nUser message: " + userMessage);
        ParsedAction action = actionParser.parse(llmResponse.getContent());

        // If the LLM says escalate or finish, we escalate with intake-complete template
        if ("escalate_human".equals(action.getAction()) || "finish".equals(action.getAction())) {
            // Create case for UC-H/J/K before escalation
            createCaseIfAllowed(session, activeUc);

            // Rebuild vars after case creation (CASE_NUMBER may have changed)
            vars = buildIntakeVariables(session, activeUc);

            String completeTemplate = scriptLibrary.renderTemplate(prefix + "_escalation", vars);
            if (completeTemplate == null) {
                completeTemplate = scriptLibrary.renderTemplate(prefix + "_intake_complete_case_created", vars);
            }
            String msg = completeTemplate != null ? completeTemplate : action.getUserMessage();
            return PhaseResult.escalate(session, msg, "intake_complete");
        }

        // Defense-in-depth: if LLM returned retrieve_knowledge in intake mode,
        // map it to ask_user — the bot should acknowledge what the user gave
        // and ask for remaining intake fields, not try to search.
        if ("retrieve_knowledge".equals(action.getAction()) || "answer_grounded".equals(action.getAction())) {
            log.warn("Session {}: LLM returned '{}' in intake mode for {}. "
                    + "Re-invoking to collect intake fields.", session.getSessionId(), action.getAction(), activeUc);

            String retryInstruction = "IMPORTANT: This is an INTAKE case for " + UC_TEAM_NAME.getOrDefault(activeUc, "support")
                    + ". You CANNOT search knowledge or provide answers. "
                    + "The user just said: \"" + userMessage + "\". "
                    + "Acknowledge what they provided, then either: "
                    + "(1) use 'ask_user' to collect any remaining details needed, or "
                    + "(2) use 'escalate_human' if you have enough info to pass to the team.";
            LlmResponse retryResponse = llmInvocation.invokeChat(projection, retryInstruction);
            ParsedAction retryAction = actionParser.parse(retryResponse.getContent());

            if ("escalate_human".equals(retryAction.getAction()) || "finish".equals(retryAction.getAction())) {
                // Create case for UC-H/J/K before escalation
                createCaseIfAllowed(session, activeUc);

                // Rebuild vars after case creation
                vars = buildIntakeVariables(session, activeUc);

                String tmpl = scriptLibrary.renderTemplate(prefix + "_escalation", vars);
                String msg = tmpl != null ? tmpl : retryAction.getUserMessage();
                return PhaseResult.escalate(session, msg, "intake_complete");
            }
            return PhaseResult.respond(session, retryAction, retryResponse, null);
        }

        // If LLM determined escalation (e.g. safe fallback during API failure), escalate immediately
        if ("escalate_human".equals(action.getAction())) {
            return PhaseResult.escalate(session, action.getUserMessage(),
                "llm_determined_escalation");
        }

        return PhaseResult.respond(session, action, llmResponse, null);
    }

    /**
     * Reconstruct a {@link KnowledgeSearchResult} from the map returned by
     * {@link com.gumtree.csagent.service.tools.SearchKnowledgeTool}.
     * The tool serialises the result as a flat map with keys: faq_miss, retrieval_miss, answer_miss, hits.
     */
    @SuppressWarnings("unchecked")
    private KnowledgeSearchResult reconstructSearchResult(Map<String, Object> data) {
        boolean faqMiss = Boolean.TRUE.equals(data.get("faq_miss"));
        boolean retrievalMiss = Boolean.TRUE.equals(data.get("retrieval_miss"));
        boolean answerMiss = Boolean.TRUE.equals(data.get("answer_miss"));

        List<KnowledgeHit> hits = new ArrayList<>();
        Object rawHits = data.get("hits");
        if (rawHits instanceof List<?> hitList) {
            for (Object item : hitList) {
                if (item instanceof Map<?, ?> hitMap) {
                    Map<String, Object> m = (Map<String, Object>) hitMap;
                    hits.add(KnowledgeHit.builder()
                            .sourceId((String) m.get("source_id"))
                            .title((String) m.get("title"))
                            .snippet((String) m.get("snippet"))
                            .canonicalUrl((String) m.get("canonical_url"))
                            .score(m.get("score") instanceof Number n ? n.doubleValue() : 0.0)
                            .build());
                }
            }
        }

        return KnowledgeSearchResult.builder()
                .faqMiss(faqMiss)
                .retrievalMiss(retrievalMiss)
                .answerMiss(answerMiss)
                .hits(hits)
                .build();
    }

    /**
     * Build a variable map for template substitution from the session context.
     * Resolves {SLA_HOURS}, {first_name}, {CASE_NUMBER}, and {TEAM_NAME}.
     */
    private Map<String, String> buildIntakeVariables(BotSession session, String activeUc) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("SLA_HOURS", DEFAULT_SLA_HOURS);
        vars.put("TEAM_NAME", UC_TEAM_NAME.getOrDefault(activeUc, "Support"));

        // Extract first_name from formContext JSON
        String firstName = extractFormField(session, "first_name");
        vars.put("first_name", firstName != null && !firstName.isBlank() ? firstName : "there");

        // Use case ID as the case number, or generate a placeholder
        String caseNumber = session.getCaseId();
        if (caseNumber == null || caseNumber.isBlank()) {
            caseNumber = "CS-" + session.getSessionId().substring(0, 8).toUpperCase();
        }
        vars.put("CASE_NUMBER", caseNumber);

        return vars;
    }

    /**
     * Extract a field value from the session's formContext JSON.
     */
    private String extractFormField(BotSession session, String fieldName) {
        if (session.getFormContext() == null || session.getFormContext().isBlank()) {
            return null;
        }
        try {
            JsonNode formNode = objectMapper.readTree(session.getFormContext());
            JsonNode field = formNode.get(fieldName);
            return field != null ? field.asText(null) : null;
        } catch (Exception e) {
            log.warn("Session {}: failed to extract '{}' from formContext: {}",
                    session.getSessionId(), fieldName, e.getMessage());
            return null;
        }
    }

    /**
     * Create a case via CreateCaseControlledTool for intake UCs that require it (UC-H, UC-J, UC-K).
     * Stores the case_id in the session. Non-blocking: failures are logged but do not prevent escalation.
     */
    private void createCaseIfAllowed(BotSession session, String activeUc) {
        Set<String> caseCreationUcs = Set.of("UC-H", "UC-J", "UC-K");
        if (!caseCreationUcs.contains(activeUc)) {
            return;
        }

        try {
            Map<String, Object> params = new LinkedHashMap<>();

            // Build case fields from session context
            String email = extractFormField(session, "email");
            String description = extractFormField(session, "description");
            String adId = extractFormField(session, "ad_id");

            // Subject per UC
            String subject = switch (activeUc) {
                case "UC-H" -> "Ad Support - Appeal";
                case "UC-J" -> "Report a Safety Issue";
                case "UC-K" -> "Technical Support Request";
                default -> activeUc + " case";
            };

            params.put("subject", subject);
            if (description != null) params.put("description", description);
            if (email != null) params.put("email", email);
            if (adId != null) params.put("ad_id", adId);

            ToolResult result = createCaseTool.execute(session, params);

            if (result.isSuccess() && result.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) result.getData();
                String caseId = (String) data.get("case_id");
                if (caseId != null) {
                    session.setCaseId(caseId);
                    log.info("Session {}: case created for {}: caseId={}",
                            session.getSessionId(), activeUc, caseId);

                    // Emit CASE_CREATED event
                    eventEmitter.emitCaseCreated(session.getSessionId(),
                            session.getTotalBotTurns(), caseId, activeUc);
                }
            } else {
                log.warn("Session {}: create_case_controlled failed for {}",
                        session.getSessionId(), activeUc);
            }
        } catch (Exception e) {
            log.warn("Session {}: case creation failed (non-blocking) for {}: {}",
                    session.getSessionId(), activeUc, e.getMessage());
        }
    }

    private PhaseResult evaluateConfirm(BotSession session, String userMessage,
                                         List<BotTurn> conversationHistory) {
        String lower = userMessage.toLowerCase(Locale.ENGLISH).trim();

        // Check for positive confirmation
        if (isPositiveConfirmation(lower)) {
            log.info("Session {}: user confirmed resolution", session.getSessionId());
            return PhaseResult.transition(session, "CLOSE", null, "user_confirmed");
        }

        // Check for negative / "no, that didn't help"
        if (isNegativeConfirmation(lower)) {
            log.info("Session {}: user rejected resolution, retrying RESOLVE", session.getSessionId());
            return PhaseResult.transition(session, "RESOLVE", null, "user_rejected");
        }

        // Check for escalation request
        if (isEscalationRequest(lower)) {
            return PhaseResult.escalate(session,
                    "No problem, let me connect you with a human agent.",
                    "user_requested_escalation");
        }

        // Otherwise, use LLM to interpret the response
        String projection = contextProjection.buildProjection(session, conversationHistory, null, userMessage);
        LlmResponse llmResponse = llmInvocation.invokeChat(projection, userMessage);
        ParsedAction action = actionParser.parse(llmResponse.getContent());

        if ("finish".equals(action.getAction())) {
            return PhaseResult.transition(session, "CLOSE", null, "llm_determined_close");
        }
        if ("escalate_human".equals(action.getAction())) {
            return PhaseResult.escalate(session, action.getUserMessage(), "llm_determined_escalation");
        }

        return PhaseResult.respond(session, action, llmResponse, null);
    }

    private PhaseResult evaluateClose(BotSession session) {
        session.setHandlingState("CLOSED");
        session.setContainmentOutcome("RESOLVED");

        // Emit OUTCOME_RECORDED and SESSION_CLOSED events
        eventEmitter.emitOutcomeRecorded(session.getSessionId(), "RESOLVED", session.getActiveUseCase());
        eventEmitter.emitSessionClosed(session.getSessionId(), "RESOLVED");

        return PhaseResult.close(session,
                "I'm glad I could help! If you have any other questions, feel free to start a new chat. Have a great day!");
    }

    private PhaseResult evaluateEscalate(BotSession session, List<BotTurn> conversationHistory) {
        session.setHandlingState("QUEUE_TO_HUMAN");
        session.setContainmentOutcome("ESCALATED");

        String message = "I'm connecting you with a human agent who can assist you further. " +
                          "A summary of our conversation will be provided to them. Please hold on.";

        return PhaseResult.escalate(session, message, session.getEscalationReason());
    }

    private boolean isPositiveConfirmation(String msg) {
        return msg.matches("(?i)(yes|yeah|yep|yup|sure|that helps|that worked|thanks|thank you|" +
                "great|perfect|awesome|got it|understood|makes sense|helpful|resolved|all good).*");
    }

    private boolean isNegativeConfirmation(String msg) {
        return msg.matches("(?i)(no|nope|nah|didn't help|not helpful|doesn't answer|wrong|incorrect|" +
                "that's not right|not what i asked|try again|different question).*");
    }

    private boolean isEscalationRequest(String msg) {
        return msg.matches("(?i).*(talk to (an?\\s+)?agent|human agent|real person|transfer me|" +
                "speak (to|with) (a\\s+)?(human|person|agent)|connect me|live agent).*");
    }

    /**
     * Result of phase evaluation.
     */
    public record PhaseResult(
            String nextPhase,
            ParsedAction action,
            LlmResponse llmResponse,
            List<KnowledgeHit> knowledgeHits,
            String transitionReason,
            boolean shouldClose,
            boolean shouldEscalate,
            String escalationReason,
            String responseText
    ) {
        static PhaseResult respond(BotSession session, ParsedAction action,
                                    LlmResponse llmResponse, List<KnowledgeHit> knowledgeHits) {
            return new PhaseResult(null, action, llmResponse, knowledgeHits,
                    null, false, false, null, action.getUserMessage());
        }

        static PhaseResult transition(BotSession session, String nextPhase,
                                       LlmResponse llmResponse, String reason) {
            return new PhaseResult(nextPhase, null, llmResponse, null,
                    reason, false, false, null, null);
        }

        static PhaseResult transitionWithResponse(BotSession session, String nextPhase,
                                                    ParsedAction action, LlmResponse llmResponse,
                                                    String reason) {
            return new PhaseResult(nextPhase, action, llmResponse, null,
                    reason, false, false, null, action.getUserMessage());
        }

        static PhaseResult transitionWithResponse(BotSession session, String nextPhase,
                                                    ParsedAction action, LlmResponse llmResponse,
                                                    String reason, List<KnowledgeHit> knowledgeHits) {
            return new PhaseResult(nextPhase, action, llmResponse, knowledgeHits,
                    reason, false, false, null, action.getUserMessage());
        }

        static PhaseResult escalate(BotSession session, String message, String reason) {
            session.setEscalationReason(reason);
            session.setHandlingState("QUEUE_TO_HUMAN");
            session.setContainmentOutcome("ESCALATED");
            return new PhaseResult("ESCALATE", null, null, null,
                    reason, false, true, reason, message);
        }

        static PhaseResult close(BotSession session, String message) {
            return new PhaseResult("CLOSE", null, null, null,
                    "session_closed", true, false, null, message);
        }
    }
}
