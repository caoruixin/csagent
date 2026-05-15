// Supporting coverage for Sprint 23 prompt teaching; not primary evidence for
// behaviour reversal. The primary causal evidence is the cs_040 target rerun
// at eval_interactive/results/20260514-080835/results.json (see Sprint 23
// handoff §13.1 + "Fix iteration" section). This test asserts text anchors
// and the absence of tool/UC branching only.

package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 23 Track A regression test — system-prompt teaching for the
 * Sprint 20 {@code already_called} projection slot
 * ({@code R-already-called-prompt-consumption}).
 *
 * <p>Sprint 20 wired the slot into
 * {@link ContextProjectionBuilder#build} (covered by
 * {@link AlreadyCalledProjectionTest}) but did not land the system-prompt
 * teaching that lets the LLM understand and act on the slot. Sprint 23
 * Track A determined — from the 2026-05-10 smoke run results.json and
 * the 2026-05-13 manual-probe brief — that the LLM was re-emitting
 * identical-args tool calls precisely because the prompt had zero
 * teaching about the slot. The fix is principled teaching text that
 * (a) names the slot, (b) describes its content in observable terms,
 * (c) tells the LLM to reuse the prior payload from
 * {@code accumulated_tool_results} when its planned call matches a slot
 * entry, and (d) leaves the decision to the LLM (soft signal, no
 * runtime short-circuit).
 *
 * <p>This regression test asserts the principled teaching is present in
 * {@code prompts/system_prompt.txt}. The teaching must remain
 * tool-name and use-case agnostic — no tool-name branching, no UC
 * branching, no eval case ids (§1.7 forbidden list). The test asserts
 * the absence of those anti-patterns alongside the positive presence
 * of the four anchors.
 */
class AlreadyCalledPromptConsumptionTest {

    private static final String SYSTEM_PROMPT_PATH = "prompts/system_prompt.txt";

    @Test
    void systemPrompt_teachesAlreadyCalledSlot_principled() throws IOException {
        String prompt = loadSystemPrompt();

        // (a) names the slot
        assertTrue(prompt.contains("already_called"),
                "system_prompt.txt must name the `already_called` projection slot so the "
                        + "LLM knows the field exists. Sprint 23 Track A "
                        + "R-already-called-prompt-consumption.");

        // (b) describes its content in observable terms — the three fields the
        //     Sprint 20 slot carries
        assertTrue(prompt.contains("arguments_hash"),
                "system_prompt.txt must name the `arguments_hash` field so the LLM "
                        + "can reason about identity of a planned call vs prior calls.");

        // (c) cross-references accumulated_tool_results so the LLM knows where
        //     to read the prior payload instead of re-emitting
        assertTrue(prompt.contains("accumulated_tool_results"),
                "system_prompt.txt must point the LLM at `accumulated_tool_results` as "
                        + "the place to read the prior payload when the slot matches a "
                        + "planned call, so the LLM can avoid re-emitting.");

        // (d) leaves the decision to the LLM (soft signal). The teaching must
        //     not be phrased as a hard runtime prohibition — the LLM owns the
        //     re-emit judgement (§1.3) and the runtime does NOT short-circuit
        //     dispatch (Sprint 19 §4.2 / Sprint 20 §5 contract).
        assertTrue(prompt.toLowerCase().contains("not block")
                        || prompt.toLowerCase().contains("you own")
                        || prompt.toLowerCase().contains("you may"),
                "system_prompt.txt must leave the re-emit decision to the LLM (soft "
                        + "signal). Look for explicit ownership wording such as "
                        + "'you own', 'you may emit', or 'does not block dispatch'.");
    }

    @Test
    void systemPrompt_teaching_doesNotBranchOnToolNameOrUseCase() throws IOException {
        String prompt = loadSystemPrompt();

        // The teaching text must be principled — no tool-name or UC if-else
        // (§1.7 forbidden list + Sprint 23 dev-prompt §7 hard gate).
        // We anchor the assertion on the teaching block specifically by
        // checking a window around the `already_called` mention, so we do
        // not false-positive on tool-names or UCs that legitimately appear
        // elsewhere in the prompt (e.g. the escalation decision tree).
        int slotIdx = prompt.indexOf("already_called");
        assertTrue(slotIdx >= 0, "precondition: prompt must mention `already_called`");
        int blockStart = Math.max(0, slotIdx - 200);
        int blockEnd = Math.min(prompt.length(), slotIdx + 1200);
        String teachingBlock = prompt.substring(blockStart, blockEnd);

        // No tool-name branching in the teaching block. The teaching is
        // about a slot pattern; specific tools must not appear as
        // conditions (no "if search_knowledge", no "if resolve_article").
        assertFalse(teachingBlock.contains("search_knowledge"),
                "Sprint 23 §7 hard gate: principled teaching must not branch on "
                        + "tool name (no `search_knowledge` mention in the "
                        + "already_called teaching block).");
        assertFalse(teachingBlock.contains("resolve_article"),
                "Sprint 23 §7 hard gate: principled teaching must not branch on "
                        + "tool name (no `resolve_article` mention in the teaching "
                        + "block).");

        // No UC-id branching in the teaching block. UC ids (UC-A, UC-B, …
        // UC-K, UC-FP) are routing decisions, not tool-emission decisions;
        // their presence here would conflate the surfaces.
        for (String uc : new String[] {
                "UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP",
                "UC-G", "UC-H", "UC-I", "UC-J", "UC-K"}) {
            assertFalse(teachingBlock.contains(uc),
                    "Sprint 23 §7 hard gate: principled teaching must not branch on "
                            + "use-case id (found `" + uc + "` inside the "
                            + "already_called teaching block).");
        }
    }

    private static String loadSystemPrompt() throws IOException {
        try (var is = new ClassPathResource(SYSTEM_PROMPT_PATH).getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
