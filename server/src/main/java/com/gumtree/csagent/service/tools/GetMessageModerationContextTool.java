package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.GumtreeApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime-only tool for UC-C (message moderation disputes).
 * Fetches message moderation history for a conversation.
 * Delegates to GumtreeApiService.getMessageModerationHistory().
 */
@Slf4j
@Component
public class GetMessageModerationContextTool implements Tool {

    private final GumtreeApiService gumtreeApiService;

    public GetMessageModerationContextTool(GumtreeApiService gumtreeApiService) {
        this.gumtreeApiService = gumtreeApiService;
    }

    @Override
    public String getName() {
        return "get_message_moderation_context";
    }

    @Override
    public ToolResult execute(BotSession session, Map<String, Object> parameters) {
        String conversationId = (String) parameters.get("conversation_id");
        if (conversationId == null || conversationId.isBlank()) {
            return ToolResult.error("Parameter 'conversation_id' is required");
        }

        Map<String, Object> moderationHistory = gumtreeApiService.getMessageModerationHistory(conversationId);

        if (moderationHistory == null || moderationHistory.isEmpty()) {
            log.info("No message moderation history for conversation_id='{}' in session '{}'",
                    conversationId, session.getSessionId());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("found", false);
            return ToolResult.ok(data);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("found", true);
        data.put("moderation_history", moderationHistory);

        return ToolResult.ok(data);
    }
}
