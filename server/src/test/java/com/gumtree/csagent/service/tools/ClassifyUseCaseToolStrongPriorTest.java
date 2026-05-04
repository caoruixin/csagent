package com.gumtree.csagent.service.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.service.runtime.UseCaseRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Sprint §C2: Replies/Messaging strong-prior carry-forward into the
 * bot-turn AgentRunLoop.
 *
 * <p>Pinned behavior:
 * <ol>
 *   <li>cs_interactive_014 form context (topic="Replies & Messaging" with
 *       UC-C from the strong prior) — LLM proposing UC-B / UC-F / UC-H is
 *       REJECTED; session.activeUseCase stays UC-C.</li>
 *   <li>Idempotent re-classification (LLM proposes UC-C while UC-C already
 *       active) is allowed and persists.</li>
 *   <li>Hard-shift exit path: when DriftDetector has already flipped active
 *       UC to UC-J / UC-G / UC-I (by the time the bot loop runs), the
 *       carry-forward releases and the LLM can commit further changes.</li>
 *   <li>Sessions without a strong-prior basis keep the original behavior —
 *       LLM-driven classify_use_case writes the new UC.</li>
 *   <li>cs_interactive_001 / 002 verbatim form context preserves UC-C.</li>
 *   <li>Sessions without form context keep working (no NPE).</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class ClassifyUseCaseToolStrongPriorTest {

    @Mock private BotSessionRepository botSessionRepository;
    @Mock private BotEventRepository botEventRepository;

    private UseCaseRegistryService registry;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ClassifyUseCaseTool tool;

    @BeforeEach
    void setUp() {
        registry = new UseCaseRegistryService();
        registry.init(); // load real registry — strong-prior table includes "Replies or Messaging" -> UC-C
        tool = new ClassifyUseCaseTool(registry, botSessionRepository, botEventRepository, objectMapper);
    }

    private BotSession sessionWithFormContext(String topic, String description, String activeUc) {
        BotSession session = BotSession.builder()
                .sessionId("sess-test")
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(2)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .formTopicSubject(topic)
                .activeUseCase(activeUc)
                .intentConfidence(new BigDecimal("0.90"))
                .build();
        try {
            session.setFormContext(objectMapper.writeValueAsString(Map.of(
                    "topic_subject", topic,
                    "description", description == null ? "" : description)));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return session;
    }

    @Test
    void cs014_strongPriorRefusesLlmDriftToUcB() {
        BotSession session = sessionWithFormContext(
                "Replies & Messaging",
                "I am getting no response from sellers when I contact them.",
                "UC-C");
        ToolResult result = tool.execute(session,
                Map.of("use_case_id", "UC-B", "confidence", 0.7));

        assertTrue(result.isSuccess(),
                "Tool returns success(committed=false) so the loop sees a benign result.");
        assertEquals("UC-C", session.getActiveUseCase(),
                "session.activeUseCase MUST remain UC-C — strong prior carries forward.");
        assertEquals(Boolean.FALSE, result.getData().get("committed"));
        assertEquals("strong_prior_carry_forward", result.getData().get("reason"));
        assertEquals("UC-C", result.getData().get("preserved_use_case_id"));
        assertEquals("UC-B", result.getData().get("rejected_use_case_id"));
        verify(botSessionRepository, never()).save(any());
        verify(botEventRepository, never()).save(any());
    }

    @Test
    void cs014_strongPriorRefusesLlmDriftToUcF() {
        BotSession session = sessionWithFormContext(
                "Replies & Messaging",
                "I am getting no response from sellers when I contact them.",
                "UC-C");
        ToolResult result = tool.execute(session,
                Map.of("use_case_id", "UC-F", "confidence", 0.5));

        assertTrue(result.isSuccess());
        assertEquals("UC-C", session.getActiveUseCase());
    }

    @Test
    void cs014_strongPriorRefusesLlmDriftToUcH() {
        BotSession session = sessionWithFormContext(
                "Replies & Messaging",
                "I am getting no response from sellers when I contact them.",
                "UC-C");
        ToolResult result = tool.execute(session,
                Map.of("use_case_id", "UC-H", "confidence", 0.4));

        assertTrue(result.isSuccess());
        assertEquals("UC-C", session.getActiveUseCase());
    }

    @Test
    void cs014_idempotentReclassification_committsCleanly() {
        BotSession session = sessionWithFormContext(
                "Replies or Messaging",
                "I am getting no response from sellers when I contact them.",
                "UC-C");
        ToolResult result = tool.execute(session,
                Map.of("use_case_id", "UC-C", "confidence", 0.9));

        assertTrue(result.isSuccess());
        assertEquals(Boolean.TRUE, result.getData().get("committed"));
        assertEquals("UC-C", session.getActiveUseCase());
        verify(botSessionRepository, times(1)).save(session);
        verify(botEventRepository, times(1)).save(any());
    }

    @Test
    void hardShiftAlreadyApplied_releasesCarryForward() {
        // DriftDetector mutates active UC BEFORE the bot loop runs — once the
        // active UC is no longer the strong-prior pick, the carry-forward
        // policy releases and the LLM can commit further changes.
        BotSession session = sessionWithFormContext(
                "Replies & Messaging",
                "Actually I want my data deleted under GDPR",
                "UC-G"); // hard-shift already moved UC away from UC-C
        ToolResult result = tool.execute(session,
                Map.of("use_case_id", "UC-G", "confidence", 0.95));

        assertTrue(result.isSuccess());
        assertEquals("UC-G", session.getActiveUseCase(),
                "Once active UC has hard-shifted, the LLM remains free to (re)classify.");
        verify(botSessionRepository, times(1)).save(session);
    }

    @Test
    void hardShiftAlreadyApplied_lLmCanThenChooseDifferentUc() {
        // session activeUC is UC-J (Trust & Safety hard shift), and now the
        // LLM proposes UC-I (refund). Because activeUseCase != strong-prior
        // for the form context (UC-C), the policy releases.
        BotSession session = sessionWithFormContext(
                "Replies & Messaging",
                "There is a scammer asking me for a refund",
                "UC-J");
        ToolResult result = tool.execute(session,
                Map.of("use_case_id", "UC-I", "confidence", 0.7));

        assertTrue(result.isSuccess());
        assertEquals("UC-I", session.getActiveUseCase());
    }

    @Test
    void noStrongPrior_lLmCanFreelyClassify() {
        // Topic is a weak prior with no B2 phrase bias for "I want help".
        BotSession session = sessionWithFormContext(
                "Account Support",
                "general question",
                null);
        ToolResult result = tool.execute(session,
                Map.of("use_case_id", "UC-D", "confidence", 0.7));

        assertTrue(result.isSuccess());
        assertEquals("UC-D", session.getActiveUseCase());
        verify(botSessionRepository, times(1)).save(session);
    }

    @Test
    void noFormContext_doesNotNpe() {
        BotSession session = BotSession.builder()
                .sessionId("sess-no-form")
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(1)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .activeUseCase(null)
                .build();
        ToolResult result = tool.execute(session,
                Map.of("use_case_id", "UC-A", "confidence", 0.6));

        assertTrue(result.isSuccess());
        assertEquals("UC-A", session.getActiveUseCase());
    }

    @Test
    void cs014_htmlEncodedAmpersandTopic_stillCarriesForward() {
        // Sprint §C2: live FormContextIngestionService.sanitize produces
        // "Replies &amp; Messaging" — the carry-forward must still recognise
        // it as the UC-C strong prior.
        BotSession session = sessionWithFormContext(
                "Replies &amp; Messaging",
                "I am getting no response from sellers when I contact them.",
                "UC-C");
        ToolResult result = tool.execute(session,
                Map.of("use_case_id", "UC-B", "confidence", 0.7));

        assertTrue(result.isSuccess());
        assertEquals("UC-C", session.getActiveUseCase(),
                "Even with HTML-encoded topic, strong prior must still carry forward.");
    }

    @Test
    void deriveStrongPriorUc_recognisesAliasAndRegistry() {
        // Alias normalisation for "Replies & Messaging" → UC-C
        Optional<String> derived = com.gumtree.csagent.service.runtime.UseCaseRouter
                .deriveStrongPriorUc("Replies & Messaging", "anything", registry);
        assertEquals(Optional.of("UC-C"), derived);

        // Direct registry hit
        derived = com.gumtree.csagent.service.runtime.UseCaseRouter
                .deriveStrongPriorUc("Replies or Messaging", "anything", registry);
        assertEquals(Optional.of("UC-C"), derived);

        // Other strong priors
        derived = com.gumtree.csagent.service.runtime.UseCaseRouter
                .deriveStrongPriorUc("Delete My Account or Data", "anything", registry);
        assertEquals(Optional.of("UC-G"), derived);

        // B2 messaging phrase bias on weak-prior topic
        derived = com.gumtree.csagent.service.runtime.UseCaseRouter
                .deriveStrongPriorUc("Account Support",
                        "no response from sellers", registry);
        assertEquals(Optional.of("UC-C"), derived);

        // No match
        derived = com.gumtree.csagent.service.runtime.UseCaseRouter
                .deriveStrongPriorUc("Account Support", "general question", registry);
        assertTrue(derived.isEmpty());
    }

    @Test
    void cs001_styleVerbatim_preservesUcC() {
        // cs_interactive_001 verbatim seed: "Replies & Messaging" + messaging description
        BotSession session = sessionWithFormContext(
                "Replies & Messaging",
                "I'm not receiving any messages from buyers",
                "UC-C");
        ToolResult result = tool.execute(session,
                Map.of("use_case_id", "UC-D", "confidence", 0.5));

        assertTrue(result.isSuccess());
        assertEquals("UC-C", session.getActiveUseCase());
    }

    @Test
    void cs002_styleVerbatim_preservesUcC() {
        // cs_interactive_002 verbatim seed: messaging-related description on
        // alias topic; B2 bias would also produce UC-C.
        BotSession session = sessionWithFormContext(
                "Replies & Messaging",
                "Notifications are not coming through, How long do I have to wait",
                "UC-C");
        ToolResult result = tool.execute(session,
                Map.of("use_case_id", "UC-F", "confidence", 0.5));

        assertTrue(result.isSuccess());
        assertEquals("UC-C", session.getActiveUseCase());
    }

    @Test
    void cs066Style_ucKPath_isUnaffected() {
        // cs_interactive_066: topic="Account Support" + UC-K technical-regression
        // override committed UC-K. The strong-prior derivation produces empty
        // (no strong prior, no B2 messaging phrase). Carry-forward must
        // therefore NOT fire; the LLM can still commit UC-K when it does.
        BotSession session = sessionWithFormContext(
                "Account Support",
                "the phone-number contact option disappeared",
                "UC-K");
        ToolResult result = tool.execute(session,
                Map.of("use_case_id", "UC-K", "confidence", 0.95));

        assertTrue(result.isSuccess());
        assertEquals("UC-K", session.getActiveUseCase());
        verify(botSessionRepository, times(1)).save(session);
    }

    @Test
    void cs095Style_ucAPath_isUnaffected() {
        // cs_interactive_095: topic="Account Support" + B2 ads-visibility
        // bias committed UC-A. The B2 derivation matches UC-A; idempotent
        // commits stay through.
        BotSession session = sessionWithFormContext(
                "Account Support",
                "telling me I have no adverts",
                "UC-A");
        ToolResult result = tool.execute(session,
                Map.of("use_case_id", "UC-A", "confidence", 0.85));

        assertTrue(result.isSuccess());
        assertEquals("UC-A", session.getActiveUseCase());
        verify(botSessionRepository, times(1)).save(session);
    }
}
