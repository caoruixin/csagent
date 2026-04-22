package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.GumtreeApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime-only tool that looks up a customer account by email.
 * Delegates to GumtreeApiService.getAccountByEmail().
 */
@Slf4j
@Component
public class LookupCustomerAccountTool implements Tool {

    private final GumtreeApiService gumtreeApiService;

    public LookupCustomerAccountTool(GumtreeApiService gumtreeApiService) {
        this.gumtreeApiService = gumtreeApiService;
    }

    @Override
    public String getName() {
        return "lookup_customer_account";
    }

    @Override
    public ToolResult execute(BotSession session, Map<String, Object> parameters) {
        String email = (String) parameters.get("email");
        if (email == null || email.isBlank()) {
            return ToolResult.error("Parameter 'email' is required");
        }

        Map<String, Object> account = gumtreeApiService.getAccountByEmail(email);

        if (account == null || account.isEmpty()) {
            log.info("No account found for email in session '{}'", session.getSessionId());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("found", false);
            return ToolResult.ok(data);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("found", true);
        data.put("account", account);

        return ToolResult.ok(data);
    }
}
