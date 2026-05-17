package com.gumtree.csagent.service.runtime.skill;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;

import java.util.Map;
import java.util.Optional;

/**
 * Per-dispatch data surface read by {@link SkillGuardrailDispatcher} when
 * walking the active Skill's {@code guardrails[]} per Sprint 37 freeze §9.1.
 *
 * <p>Built once at each dispatch site in {@code AgentRunLoopImpl} and passed
 * to {@link SkillGuardrailDispatcher#checkBeforeDispatch} /
 * {@link SkillGuardrailDispatcher#checkBeforeOutcomePersist}. Each per-predicate
 * handler reads the subset of fields it needs:
 *
 * <ul>
 *   <li>{@code plan} — the active {@link PhasePlan} (phase + useCase + tools).</li>
 *   <li>{@code session} — the {@link BotSession} (intake fields, current
 *       phase for the {@code premature_resolve_outcome_guard} delegation).</li>
 *   <li>{@code accumulatedToolResults} — read by
 *       {@code faq_miss_handover_requires_resolve_attempt} for the
 *       search_knowledge / resolve_article cross-check.</li>
 *   <li>{@code lastLlmRawResponse} — read by {@code must_cite_source} for
 *       source_id token presence check on the bot's user-facing message.</li>
 *   <li>{@code parsedUserMessage} — same purpose as {@code lastLlmRawResponse}
 *       but already parsed via {@code ActionParser} when available;
 *       empty {@link Optional} when the dispatch site cannot supply it.</li>
 * </ul>
 */
public record DispatchContext(
        PhasePlan plan,
        BotSession session,
        Map<String, Object> accumulatedToolResults,
        String lastLlmRawResponse,
        Optional<String> parsedUserMessage
) {
    public DispatchContext {
        accumulatedToolResults = accumulatedToolResults == null
                ? Map.of()
                : Map.copyOf(accumulatedToolResults);
        parsedUserMessage = parsedUserMessage == null ? Optional.empty() : parsedUserMessage;
    }
}
