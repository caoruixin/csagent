package com.gumtree.csagent.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gumtree.csagent.model.BotEvent;
import com.gumtree.csagent.model.enums.EventType;
import com.gumtree.csagent.repository.BotEventRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Loads tool-policy.yaml at startup and enforces per-UC tool access rules.
 */
@Slf4j
@Component
public class ToolPolicyEnforcer {

    private final BotEventRepository botEventRepository;
    private final Map<String, ToolPolicy> policies = new LinkedHashMap<>();

    public ToolPolicyEnforcer(BotEventRepository botEventRepository) {
        this.botEventRepository = botEventRepository;
    }

    @PostConstruct
    public void init() {
        try {
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            InputStream is = new ClassPathResource("config/tool-policy.yaml").getInputStream();
            JsonNode root = yamlMapper.readTree(is);

            JsonNode toolsNode = root.get("tools");
            if (toolsNode != null) {
                toolsNode.fields().forEachRemaining(entry -> {
                    String toolName = entry.getKey();
                    JsonNode def = entry.getValue();
                    String type = def.get("type").asText();
                    List<String> allowedUcs = new ArrayList<>();
                    def.get("allowed-ucs").forEach(n -> allowedUcs.add(n.asText()));
                    policies.put(toolName, new ToolPolicy(type, allowedUcs));
                });
            }

            log.info("Loaded tool policies for {} tools", policies.size());
        } catch (Exception e) {
            log.error("Failed to load tool-policy.yaml: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load tool-policy.yaml", e);
        }
    }

    /**
     * Check if a tool is allowed for the given active use case.
     *
     * @param toolName      snake_case tool name
     * @param activeUseCase current UC ID (e.g. "UC-A")
     * @return true if the tool may be executed
     */
    public boolean isToolAllowed(String toolName, String activeUseCase) {
        ToolPolicy policy = policies.get(toolName);
        if (policy == null) {
            log.warn("No policy found for tool '{}', blocking by default", toolName);
            return false;
        }
        if (policy.allowedUcs.contains("ALL")) {
            return true;
        }
        return activeUseCase != null && policy.allowedUcs.contains(activeUseCase);
    }

    /**
     * Get the tool type (AGENT_VISIBLE or RUNTIME_ONLY).
     */
    public String getToolType(String toolName) {
        ToolPolicy policy = policies.get(toolName);
        return policy != null ? policy.type : null;
    }

    /**
     * Emit a TOOL_SCOPE_BLOCKED event when a tool call is denied.
     */
    public void emitBlockedEvent(String sessionId, String toolName, String activeUseCase) {
        BotEvent event = BotEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .eventType(EventType.TOOL_SCOPE_BLOCKED.name())
                .payload(String.format("{\"tool\":\"%s\",\"active_uc\":\"%s\"}", toolName,
                        activeUseCase != null ? activeUseCase : "none"))
                .createdAt(OffsetDateTime.now())
                .build();
        botEventRepository.save(event);
        log.warn("TOOL_SCOPE_BLOCKED: tool='{}', activeUC='{}', sessionId='{}'",
                toolName, activeUseCase, sessionId);
    }

    /**
     * Get all tool names that are AGENT_VISIBLE and allowed for a given UC.
     */
    public List<String> getVisibleToolsForUc(String activeUseCase) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, ToolPolicy> entry : policies.entrySet()) {
            ToolPolicy policy = entry.getValue();
            if ("AGENT_VISIBLE".equals(policy.type)) {
                if (policy.allowedUcs.contains("ALL") ||
                        (activeUseCase != null && policy.allowedUcs.contains(activeUseCase))) {
                    result.add(entry.getKey());
                }
            }
        }
        return result;
    }

    private record ToolPolicy(String type, List<String> allowedUcs) {}
}
