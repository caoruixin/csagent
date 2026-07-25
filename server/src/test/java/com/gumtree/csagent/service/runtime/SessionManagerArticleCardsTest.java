package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.ChatSessionResponse;
import com.gumtree.csagent.model.KbArticle;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.repository.KbArticleRepository;
import com.gumtree.csagent.repository.MockHandoverLogRepository;
import com.gumtree.csagent.repository.SessionOutcomeRepository;
import com.gumtree.csagent.service.knowledge.ArticleCardAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * P1-C (2026-07-25) — end of the wire: the FAQ sources a turn used must reach
 * {@code ChatSessionResponse.additional_data.articles}, which
 * {@code MessageBubble.tsx:15} reads to render {@code ArticleCard}. Before this
 * change every {@code additionalData} call site emitted at most
 * {@code {"latency_ms": …}}, so the card component could never fire.
 *
 * <p>{@link ArticleCardAssemblerTest} covers the selection rule and the
 * no-internal-id guarantee in isolation; this test covers the wiring and the
 * key-presence contract at the {@code SessionManager} boundary.
 */
@ExtendWith(MockitoExtension.class)
class SessionManagerArticleCardsTest {

    private static final Pattern INTERNAL_ID =
            Pattern.compile("\\bka[A-Za-z0-9]{13}([A-Za-z0-9]{3})?\\b");
    private static final String ARTICLE_ID = "ka4P2000000021pIAA";
    private static final String ARTICLE_URL = "https://help.gumtree.com/repost-expired-advert";

    @Mock private BotSessionRepository sessionRepository;
    @Mock private BotEventRepository eventRepository;
    @Mock private SessionOutcomeRepository outcomeRepository;
    @Mock private MockHandoverLogRepository handoverLogRepository;
    @Mock private BotTurnRepository botTurnRepository;
    @Mock private FormContextIngestionService formIngestion;
    @Mock private UseCaseRouter useCaseRouter;
    @Mock private ControlKernel controlKernel;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private KbArticleRepository kbArticleRepository;

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        EscalationReasonResolver resolver = new EscalationReasonResolver();
        HandoverPayloadAssembler handoverAssembler = new HandoverPayloadAssembler(
                botTurnRepository, resolver, objectMapper);
        sessionManager = new SessionManager(
                sessionRepository, eventRepository, outcomeRepository,
                handoverLogRepository, botTurnRepository, formIngestion,
                useCaseRouter, controlKernel, controlPolicy,
                useCaseRegistry, objectMapper, handoverAssembler, resolver,
                new ArticleCardAssembler(kbArticleRepository));
    }

    @Test
    @DisplayName("a grounded reply carries the cited source into additional_data.articles")
    void processMessage_groundedReply_populatesArticles() {
        BotSession session = liveSession();
        stubKernelReply(session, "You can repost it from My Ads: " + ARTICLE_URL);
        session.setRetrievedSourceIds(new String[]{ARTICLE_ID});
        session.setResolvedSourceIds(new String[]{ARTICLE_ID});
        session.setCitedSourceIds(new String[]{ARTICLE_ID});
        lenient().when(kbArticleRepository.findAllById(anyCollection()))
                .thenReturn(List.of(KbArticle.builder()
                        .articleId(ARTICLE_ID)
                        .title("Reposting an expired advert")
                        .summary("Expired adverts can be reposted from My Ads within 90 days.")
                        .sourceUrl(ARTICLE_URL)
                        .isPublished(true)
                        .build()));

        ChatSessionResponse response = sessionManager.processMessage("sess-1", "How do I repost?");

        Object raw = response.getAdditionalData().get("articles");
        assertTrue(raw instanceof List, "articles must be a JSON array. Got: " + raw);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> articles = (List<Map<String, Object>>) raw;
        assertEquals(1, articles.size());
        assertEquals("Reposting an expired advert", articles.get(0).get("title"));
        assertEquals(ARTICLE_URL, articles.get(0).get("url"));
        // latency_ms must survive alongside the new key.
        assertTrue(response.getAdditionalData().containsKey("latency_ms"));
    }

    @Test
    @DisplayName("SAFETY: the internal ka… id does not reach additional_data")
    void processMessage_doesNotLeakInternalArticleId() {
        BotSession session = liveSession();
        stubKernelReply(session, "You can repost it from My Ads: " + ARTICLE_URL);
        session.setResolvedSourceIds(new String[]{ARTICLE_ID});
        session.setCitedSourceIds(new String[]{ARTICLE_ID});
        lenient().when(kbArticleRepository.findAllById(anyCollection()))
                .thenReturn(List.of(KbArticle.builder()
                        .articleId(ARTICLE_ID)
                        .title("Reposting an expired advert")
                        .summary("Expired adverts can be reposted from My Ads.")
                        .sourceUrl(ARTICLE_URL)
                        .isPublished(true)
                        .build()));

        ChatSessionResponse response = sessionManager.processMessage("sess-1", "How do I repost?");

        String rendered = String.valueOf(response.getAdditionalData());
        assertFalse(INTERNAL_ID.matcher(rendered).find(),
                "internal Salesforce Knowledge id leaked to the client: " + rendered);
        assertFalse(rendered.contains(ARTICLE_ID), rendered);
    }

    @Test
    @DisplayName("a turn with no source omits the articles key entirely (no empty array)")
    void processMessage_ungroundedReply_omitsArticlesKey() {
        BotSession session = liveSession();
        stubKernelReply(session, "Could you tell me which advert you mean?");

        ChatSessionResponse response = sessionManager.processMessage("sess-1", "help");

        assertFalse(response.getAdditionalData().containsKey("articles"),
                "articles must be omitted, not emitted as []. Got: "
                        + response.getAdditionalData());
        assertTrue(response.getAdditionalData().containsKey("latency_ms"));
    }

    @Test
    @DisplayName("a KB failure during assembly does not break the reply")
    void processMessage_assemblyFailure_stillReturnsReply() {
        BotSession session = liveSession();
        stubKernelReply(session, "Here is what I found: " + ARTICLE_URL);
        session.setResolvedSourceIds(new String[]{ARTICLE_ID});
        session.setCitedSourceIds(new String[]{ARTICLE_ID});
        lenient().when(kbArticleRepository.findAllById(anyCollection()))
                .thenThrow(new RuntimeException("connection reset"));

        ChatSessionResponse response = sessionManager.processMessage("sess-1", "How do I repost?");

        assertEquals("Here is what I found: " + ARTICLE_URL, response.getReplyText());
        assertFalse(response.getAdditionalData().containsKey("articles"));
    }

    // ------------------------------------------------------------------

    private BotSession liveSession() {
        BotSession session = BotSession.builder()
                .sessionId("sess-1")
                .handlingState("BOT_HANDLING")
                .currentPhase("RESOLVE")
                .activeUseCase("UC-C")
                .totalBotTurns(1)
                .build();
        when(sessionRepository.findById("sess-1")).thenReturn(Optional.of(session));
        return session;
    }

    private void stubKernelReply(BotSession session, String replyText) {
        when(controlKernel.processMessage(any(BotSession.class), anyString()))
                .thenReturn(new ControlKernel.KernelResult(replyText, false, 42L));
    }
}
