package com.gumtree.csagent.service.knowledge;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.KbArticle;
import com.gumtree.csagent.repository.KbArticleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Builds the customer-facing {@code additional_data.articles} payload for a
 * turn from the evidence lineage the turn already produced.
 *
 * <p><b>Why this exists.</b> The chat UI has always been able to render FAQ
 * source cards ({@code ui/src/components/chat/ArticleCard.tsx}, driven by
 * {@code MessageBubble.tsx}'s {@code data.articles}), but no server code ever
 * populated the key — every {@code additionalData} call site emitted
 * {@code Map.of()} or {@code Map.of("latency_ms", ...)}. The cards were
 * therefore unreachable and the knowledge sources behind an answer reached the
 * customer only as an inline URL in the reply prose, if at all.
 *
 * <p><b>Where the data comes from.</b> No new retrieval is performed. The
 * selection is read from the §L1 evidence lineage
 * ({@link SourceEvidenceLineage}) that {@code ControlKernel.recordRunResult}
 * has already stamped onto the transient {@link BotSession} slots for this
 * turn, and the display fields are read back from {@code kb_articles} by id.
 * A turn that retrieved nothing produces no cards.
 *
 * <p><b>Selection rule</b> (see {@link #assemble}): only articles the bot
 * actually <em>used</em> in the customer-visible reply are shown — never the
 * full retrieval candidate list. Preference order:
 *
 * <ol>
 *   <li>cited ∩ resolved — quoted in the reply <em>and</em> deepened through
 *       {@code resolve_article};</li>
 *   <li>cited ∩ retrieved — quoted in the reply from the
 *       {@code search_knowledge} hits without a separate resolve step (the
 *       common shape when the LLM answers straight from the hit list);</li>
 *   <li>cited canonical URLs that the extractor could not map back to a
 *       per-turn candidate, matched to {@code kb_articles.source_url}. This
 *       covers the documented "search on one turn, answer on a later turn"
 *       pattern, where the answer turn ran no tools and so has no per-turn
 *       candidate set to map against.</li>
 * </ol>
 *
 * <p><b>Safety floor.</b> Only customer-visible fields leave this class:
 * {@code title}, {@code snippet}, {@code url}. The internal Salesforce
 * Knowledge article id ({@code ka…}) is used as a lookup key and is never
 * emitted; {@link #redactInternalIds} additionally scrubs any {@code ka…}
 * token that a KB text field might itself contain, so the no-leak property
 * holds independently of corpus content. Unpublished articles are dropped,
 * mirroring the §L0 published-safety posture of
 * {@code ResolveArticleTool}.
 */
@Slf4j
@Service
public class ArticleCardAssembler {

    /** {@code additional_data} key read by {@code MessageBubble.tsx:15}. */
    public static final String ARTICLES_KEY = "articles";

    /**
     * Upper bound on cards per turn. {@code search_knowledge} returns the top
     * 3 hits, so this is a payload guard rather than a real truncation.
     */
    static final int MAX_CARDS = 3;

    /** Upper bound on the rendered snippet, in characters. */
    static final int SNIPPET_MAX_CHARS = 240;

    /**
     * Salesforce Knowledge id shape. Same pattern as
     * {@code CitationExtractor.SOURCE_ID_PATTERN}: {@code ka} plus 13 or 16
     * alphanumerics. Used here only to redact, never to match customer intent.
     */
    private static final Pattern INTERNAL_ARTICLE_ID_PATTERN =
            Pattern.compile("\\bka[A-Za-z0-9]{13}([A-Za-z0-9]{3})?\\b");

    private final KbArticleRepository kbArticleRepository;

    public ArticleCardAssembler(KbArticleRepository kbArticleRepository) {
        this.kbArticleRepository = kbArticleRepository;
    }

    /**
     * Assemble the source cards for the turn the given session just completed.
     *
     * @return an immutable list of {@code {title, snippet, url}} maps, in
     *         preference order; empty when this turn used no knowledge source.
     *         Callers should omit the {@code articles} key entirely when the
     *         list is empty rather than emitting {@code []}.
     */
    public List<Map<String, Object>> assemble(BotSession session) {
        if (session == null) {
            return List.of();
        }
        List<String> cited = toList(session.getCitedSourceIds());
        List<String> citedUrls = toList(session.getCitedCanonicalUrls());
        if (cited.isEmpty() && citedUrls.isEmpty()) {
            return List.of();
        }

        Set<String> resolved = toSet(session.getResolvedSourceIds());
        Set<String> retrieved = toSet(session.getRetrievedSourceIds());

        // Tier 1: cited AND resolved. Tier 2 (cited AND retrieved) only when
        // tier 1 is empty, so a resolved-and-cited article is never diluted by
        // a merely-retrieved one.
        Set<String> selectedIds = new LinkedHashSet<>();
        for (String id : cited) {
            if (resolved.contains(id)) {
                selectedIds.add(id);
            }
        }
        if (selectedIds.isEmpty()) {
            for (String id : cited) {
                if (retrieved.contains(id)) {
                    selectedIds.add(id);
                }
            }
        }

        List<Map<String, Object>> cards = new ArrayList<>();
        Set<String> emittedArticleIds = new LinkedHashSet<>();
        if (!selectedIds.isEmpty()) {
            appendCards(cards, emittedArticleIds, lookupByIds(selectedIds));
        }
        // Tier 3: URLs the bot put in the reply that carried no per-turn
        // candidate to map against. Always merged (a reply can cite one
        // mapped and one unmapped source), de-duplicated by article id.
        if (!citedUrls.isEmpty() && cards.size() < MAX_CARDS) {
            appendCards(cards, emittedArticleIds, lookupByUrls(citedUrls));
        }
        return List.copyOf(cards);
    }

    private List<KbArticle> lookupByIds(Collection<String> ids) {
        try {
            return kbArticleRepository.findAllById(ids);
        } catch (Exception e) {
            log.warn("article-card lookup by id failed ({} ids): {}", ids.size(), e.getMessage());
            return List.of();
        }
    }

    private List<KbArticle> lookupByUrls(Collection<String> urls) {
        try {
            return kbArticleRepository.findBySourceUrlInAndIsPublishedTrue(urls);
        } catch (Exception e) {
            log.warn("article-card lookup by url failed ({} urls): {}", urls.size(), e.getMessage());
            return List.of();
        }
    }

    private void appendCards(List<Map<String, Object>> cards, Set<String> emittedArticleIds,
                             List<KbArticle> articles) {
        if (articles == null) {
            return;
        }
        for (KbArticle article : articles) {
            if (cards.size() >= MAX_CARDS) {
                return;
            }
            if (article == null || article.getArticleId() == null
                    || !emittedArticleIds.add(article.getArticleId())) {
                continue;
            }
            Map<String, Object> card = toCard(article);
            if (card != null) {
                cards.add(card);
            }
        }
    }

    /**
     * Project one KB article onto the customer-visible card contract. Returns
     * {@code null} when the article cannot be rendered as a card (unpublished,
     * no title, or no URL to link to).
     */
    private static Map<String, Object> toCard(KbArticle article) {
        if (Boolean.FALSE.equals(article.getIsPublished())) {
            // §L0 published-safety: draft / suspended content is never shown.
            return null;
        }
        String title = blankToNull(article.getTitle());
        String url = blankToNull(article.getSourceUrl());
        if (title == null || url == null) {
            // ArticleCard.tsx renders the title as the link text of an
            // <a href={url}>; without both it is a broken card.
            return null;
        }
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("title", redactInternalIds(title));
        card.put("snippet", redactInternalIds(buildSnippet(article)));
        card.put("url", url);
        return card;
    }

    private static String buildSnippet(KbArticle article) {
        String source = blankToNull(article.getSummary());
        if (source == null) {
            source = blankToNull(article.getDescription());
        }
        if (source == null) {
            return "";
        }
        String collapsed = source.replaceAll("\\s+", " ").trim();
        if (collapsed.length() <= SNIPPET_MAX_CHARS) {
            return collapsed;
        }
        String head = collapsed.substring(0, SNIPPET_MAX_CHARS);
        int lastSpace = head.lastIndexOf(' ');
        if (lastSpace > SNIPPET_MAX_CHARS / 2) {
            head = head.substring(0, lastSpace);
        }
        return head + "...";
    }

    /**
     * Strip any internal Salesforce Knowledge id embedded in a KB text field.
     * The ids are runtime lookup keys, not customer-facing identifiers; this
     * keeps the no-leak guarantee independent of what the corpus happens to
     * contain.
     */
    private static String redactInternalIds(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String cleaned = INTERNAL_ARTICLE_ID_PATTERN.matcher(text).replaceAll("");
        if (cleaned.equals(text)) {
            return text;
        }
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static List<String> toList(String[] values) {
        if (values == null || values.length == 0) {
            return List.of();
        }
        List<String> out = new ArrayList<>(values.length);
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                out.add(v.trim());
            }
        }
        return out;
    }

    private static Set<String> toSet(String[] values) {
        if (values == null || values.length == 0) {
            return Set.of();
        }
        Set<String> out = new LinkedHashSet<>(Arrays.asList(values));
        out.remove(null);
        return out;
    }

}
