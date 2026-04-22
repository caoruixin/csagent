package com.gumtree.csagent.service.guardrails;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides "thinking" placeholder messages while the LLM processes a response.
 * Used by the controller layer to send immediate acknowledgement to the user.
 *
 * Messages are cycled in round-robin order to provide variety across
 * consecutive placeholder displays within the same session.
 */
@Slf4j
@Service
public class ProgressPlaceholderService {

    private static final List<String> PLACEHOLDERS = List.of(
            "Just a moment while I look into this...",
            "Let me check that for you...",
            "Working on this..."
    );

    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * Get the next placeholder message in round-robin order.
     *
     * @return a placeholder message string
     */
    public String getPlaceholder() {
        int index = Math.abs(counter.getAndIncrement() % PLACEHOLDERS.size());
        return PLACEHOLDERS.get(index);
    }
}
