package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.PhasePlan;

import java.util.List;

/**
 * Mechanical model↔tool execution worker. Per Phase 3 §3.3.3 (v9 — D16),
 * the loop is bounded by {@link PhasePlan#maxToolSteps()} and produces an
 * {@link AgentRunResult} that {@code ControlKernel} flattens into the trace
 * tables.
 *
 * <p>The interface intentionally takes no responsibility for phase
 * transitions, persistence, or policy decisions — those belong to
 * {@code PhaseEvaluator} and {@code ControlKernel}. The loop only:
 *
 * <ol>
 *   <li>Builds a context projection (via {@code ContextProjectionBuilder})</li>
 *   <li>Invokes the LLM (via {@code LlmInvocationService})</li>
 *   <li>Parses the response (via {@code ActionParser})</li>
 *   <li>Validates each requested tool against {@link PhasePlan#allowedTools()}
 *       and dispatches it (via {@code ToolDispatcher})</li>
 *   <li>Repeats until a terminal outcome is reached or
 *       {@link PhasePlan#maxToolSteps()} is exhausted</li>
 * </ol>
 *
 * <p>D16.A scaffolds the interface only; the real loop lands in D16.B.
 */
public interface AgentRunLoop {

    AgentRunResult run(PhasePlan plan,
                       BotSession session,
                       String userMessage,
                       List<BotTurn> history);
}
