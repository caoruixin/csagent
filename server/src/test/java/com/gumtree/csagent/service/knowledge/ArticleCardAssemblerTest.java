package com.gumtree.csagent.service.knowledge;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.KbArticle;
import com.gumtree.csagent.repository.KbArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-C (2026-07-25) — {@code additional_data.articles} was never populated, so
 * {@code ui/src/components/chat/ArticleCard.tsx} was dead code and FAQ source
 * cards never rendered.
 *
 * <p>These tests pin the three properties that matter for the fix:
 *
 * <ol>
 *   <li>cards come from the evidence the turn <em>used</em> (cited), not from
 *       the raw retrieval candidate list;</li>
 *   <li>the payload shape matches {@code MessageBubble.tsx:15}'s
 *       {@code {title, snippet, url}};</li>
 *   <li><b>no internal Salesforce Knowledge id ({@code ka…}) ever reaches the
 *       customer-facing payload</b> — the ids are lookup keys only.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class ArticleCardAssemblerTest {

    /** The same {@code ka…} shape the runtime treats as an internal id. */
    private static final Pattern INTERNAL_ID =
            Pattern.compile("\\bka[A-Za-z0-9]{13}([A-Za-z0-9]{3})?\\b");

    private static final String ID_RESOLVED = "ka4P2000000021pIAA";
    private static final String ID_RETRIEVED_ONLY = "ka4P2000000099zXYZ";
    private static final String ID_UNUSED = "ka4P2000000077aBCD";

    @Mock private KbArticleRepository kbArticleRepository;

    private ArticleCardAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new ArticleCardAssembler(kbArticleRepository);
    }

    // ------------------------------------------------------------------
    //  Tier-0: no internal id leakage
    // ------------------------------------------------------------------

    @Test
    @DisplayName("SAFETY: the internal ka… article id never appears in the payload")
    void assemble_neverLeaksInternalArticleId() {
        BotSession session = sessionWith(
                new String[]{ID_RESOLVED}, new String[]{ID_RESOLVED},
                new String[]{ID_RESOLVED}, null);
        stubFindAllById(List.of(article(ID_RESOLVED,
                "Reposting an expired advert",
                "Expired adverts can be reposted from My Ads within 90 days.",
                "https://help.gumtree.com/repost-expired-advert")));

        List<Map<String, Object>> cards = assembler.assemble(session);

        assertEquals(1, cards.size());
        String rendered = cards.toString();
        assertFalse(INTERNAL_ID.matcher(rendered).find(),
                "internal Salesforce Knowledge id leaked into the customer payload: " + rendered);
        assertFalse(rendered.contains(ID_RESOLVED), rendered);
    }

    @Test
    @DisplayName("SAFETY: a ka… id embedded in KB text is scrubbed out of the card")
    void assemble_scrubsInternalIdEmbeddedInKbText() {
        BotSession session = sessionWith(
                new String[]{ID_RESOLVED}, new String[]{ID_RESOLVED},
                new String[]{ID_RESOLVED}, null);
        stubFindAllById(List.of(article(ID_RESOLVED,
                "Reposting an expired advert",
                "See internal note " + ID_UNUSED + " for the agent-side steps.",
                "https://help.gumtree.com/repost-expired-advert")));

        List<Map<String, Object>> cards = assembler.assemble(session);

        String rendered = cards.toString();
        assertFalse(INTERNAL_ID.matcher(rendered).find(),
                "a ka… token carried inside a KB field must be scrubbed: " + rendered);
    }

    // ------------------------------------------------------------------
    //  Payload shape
    // ------------------------------------------------------------------

    @Test
    @DisplayName("card carries exactly {title, snippet, url}")
    void assemble_cardShapeMatchesUiContract() {
        BotSession session = sessionWith(
                new String[]{ID_RESOLVED}, new String[]{ID_RESOLVED},
                new String[]{ID_RESOLVED}, null);
        stubFindAllById(List.of(article(ID_RESOLVED, "Reposting an expired advert",
                "Expired adverts can be reposted from My Ads.",
                "https://help.gumtree.com/repost-expired-advert")));

        Map<String, Object> card = assembler.assemble(session).get(0);

        assertEquals(Set.of("title", "snippet", "url"), card.keySet());
        assertEquals("Reposting an expired advert", card.get("title"));
        assertEquals("Expired adverts can be reposted from My Ads.", card.get("snippet"));
        assertEquals("https://help.gumtree.com/repost-expired-advert", card.get("url"));
    }

    // ------------------------------------------------------------------
    //  Selection rule
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a retrieved-but-never-cited candidate is not pushed at the customer")
    void assemble_excludesUncitedRetrievalCandidates() {
        BotSession session = sessionWith(
                new String[]{ID_RESOLVED, ID_UNUSED}, new String[]{ID_RESOLVED},
                new String[]{ID_RESOLVED}, null);
        stubFindAllById(List.of(article(ID_RESOLVED, "Reposting an expired advert",
                "Expired adverts can be reposted from My Ads.",
                "https://help.gumtree.com/repost-expired-advert")));

        List<Map<String, Object>> cards = assembler.assemble(session);

        assertEquals(1, cards.size());
        verify(kbArticleRepository).findAllById(argThatContainsOnly(ID_RESOLVED));
    }

    @Test
    @DisplayName("cited-from-search-hits without a resolve step still produces a card")
    void assemble_citedRetrievedWithoutResolve_stillRenders() {
        BotSession session = sessionWith(
                new String[]{ID_RETRIEVED_ONLY}, new String[0],
                new String[]{ID_RETRIEVED_ONLY}, null);
        stubFindAllById(List.of(article(ID_RETRIEVED_ONLY, "Finding your live advert",
                "Live adverts appear under My Ads.",
                "https://help.gumtree.com/find-live-advert")));

        List<Map<String, Object>> cards = assembler.assemble(session);

        assertEquals(1, cards.size());
        assertEquals("Finding your live advert", cards.get(0).get("title"));
    }

    @Test
    @DisplayName("a cited URL with no per-turn candidate is mapped back through kb_articles")
    void assemble_citedCanonicalUrl_mapsBackToArticle() {
        // The documented "search on one turn, answer on a later turn" shape:
        // the answer turn ran no tools, so the extractor had no candidate to
        // map the URL onto and recorded it as a bare canonical URL.
        BotSession session = sessionWith(new String[0], new String[0], new String[0],
                new String[]{"https://help.gumtree.com/repost-expired-advert"});
        when(kbArticleRepository.findBySourceUrlInAndIsPublishedTrue(anyCollection()))
                .thenReturn(List.of(article(ID_RESOLVED, "Reposting an expired advert",
                        "Expired adverts can be reposted from My Ads.",
                        "https://help.gumtree.com/repost-expired-advert")));

        List<Map<String, Object>> cards = assembler.assemble(session);

        assertEquals(1, cards.size());
        assertEquals("https://help.gumtree.com/repost-expired-advert", cards.get(0).get("url"));
        assertFalse(INTERNAL_ID.matcher(cards.toString()).find(), cards.toString());
    }

    @Test
    @DisplayName("a turn that cited nothing produces no cards and no KB read")
    void assemble_noCitations_returnsEmptyWithoutQuerying() {
        BotSession session = sessionWith(
                new String[]{ID_UNUSED}, new String[]{ID_UNUSED}, new String[0], new String[0]);

        assertTrue(assembler.assemble(session).isEmpty());
        verify(kbArticleRepository, never()).findAllById(anyCollection());
        verify(kbArticleRepository, never()).findBySourceUrlInAndIsPublishedTrue(anyCollection());
    }

    @Test
    @DisplayName("a null session is tolerated")
    void assemble_nullSession_returnsEmpty() {
        assertTrue(assembler.assemble(null).isEmpty());
    }

    // ------------------------------------------------------------------
    //  Render guards
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an unpublished article is never rendered as a card (§L0 parity)")
    void assemble_dropsUnpublishedArticle() {
        BotSession session = sessionWith(
                new String[]{ID_RESOLVED}, new String[]{ID_RESOLVED},
                new String[]{ID_RESOLVED}, null);
        KbArticle unpublished = article(ID_RESOLVED, "Draft policy note", "Not for customers.",
                "https://help.gumtree.com/draft");
        unpublished.setIsPublished(false);
        stubFindAllById(List.of(unpublished));

        assertTrue(assembler.assemble(session).isEmpty());
    }

    @Test
    @DisplayName("an article with no canonical URL is dropped rather than rendered link-less")
    void assemble_dropsArticleWithoutUrl() {
        BotSession session = sessionWith(
                new String[]{ID_RESOLVED}, new String[]{ID_RESOLVED},
                new String[]{ID_RESOLVED}, null);
        stubFindAllById(List.of(article(ID_RESOLVED, "No URL on file", "Body text.", null)));

        assertTrue(assembler.assemble(session).isEmpty());
    }

    @Test
    @DisplayName("snippet falls back to the description and is length-capped")
    void assemble_snippetFallsBackToDescriptionAndIsCapped() {
        BotSession session = sessionWith(
                new String[]{ID_RESOLVED}, new String[]{ID_RESOLVED},
                new String[]{ID_RESOLVED}, null);
        KbArticle a = article(ID_RESOLVED, "Long article", null,
                "https://help.gumtree.com/long");
        a.setDescription("word ".repeat(200));
        stubFindAllById(List.of(a));

        String snippet = (String) assembler.assemble(session).get(0).get("snippet");

        assertTrue(snippet.startsWith("word word"), snippet);
        assertTrue(snippet.endsWith("..."), snippet);
        assertTrue(snippet.length() <= ArticleCardAssembler.SNIPPET_MAX_CHARS + 3,
                "snippet length " + snippet.length());
    }

    @Test
    @DisplayName("no more than MAX_CARDS cards are emitted")
    void assemble_capsCardCount() {
        String[] cited = {"ka4P200000000001AAA", "ka4P200000000002AAA",
                "ka4P200000000003AAA", "ka4P200000000004AAA"};
        BotSession session = sessionWith(cited, cited, cited, null);
        stubFindAllById(List.of(
                article(cited[0], "One", "a", "https://help.gumtree.com/1"),
                article(cited[1], "Two", "b", "https://help.gumtree.com/2"),
                article(cited[2], "Three", "c", "https://help.gumtree.com/3"),
                article(cited[3], "Four", "d", "https://help.gumtree.com/4")));

        assertEquals(ArticleCardAssembler.MAX_CARDS, assembler.assemble(session).size());
    }

    @Test
    @DisplayName("a KB lookup failure degrades to no cards rather than throwing")
    void assemble_kbFailure_degradesGracefully() {
        BotSession session = sessionWith(
                new String[]{ID_RESOLVED}, new String[]{ID_RESOLVED},
                new String[]{ID_RESOLVED}, null);
        when(kbArticleRepository.findAllById(anyCollection()))
                .thenThrow(new RuntimeException("connection reset"));

        assertTrue(assembler.assemble(session).isEmpty());
    }

    // ------------------------------------------------------------------

    private void stubFindAllById(List<KbArticle> articles) {
        lenient().when(kbArticleRepository.findAllById(anyCollection())).thenReturn(articles);
    }

    private static Collection<String> argThatContainsOnly(String id) {
        return org.mockito.ArgumentMatchers.argThat(
                (Collection<String> c) -> c.size() == 1 && c.contains(id));
    }

    private static BotSession sessionWith(String[] retrieved, String[] resolved,
                                          String[] cited, String[] citedUrls) {
        BotSession session = BotSession.builder().sessionId("sess-1").build();
        session.setRetrievedSourceIds(retrieved);
        session.setResolvedSourceIds(resolved);
        session.setCitedSourceIds(cited);
        session.setCitedCanonicalUrls(citedUrls);
        return session;
    }

    private static KbArticle article(String id, String title, String summary, String url) {
        return KbArticle.builder()
                .articleId(id)
                .title(title)
                .summary(summary)
                .sourceUrl(url)
                .isPublished(true)
                .build();
    }
}
