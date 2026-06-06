package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.KbArticle;
import com.gumtree.csagent.repository.KbArticleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves a full article by ID from the knowledge base.
 * AGENT_VISIBLE — the LLM calls this after search_knowledge to get full article details.
 *
 * <p>Sprint 14 §L0 — KB published-safety / canonical URL audit:
 * unpublished articles are refused with a deterministic
 * {@link #UNPUBLISHED_REJECT_REASON} error so the agent cannot surface
 * draft / safety-suspended content. Successful resolves stamp
 * {@code canonical_url} (mirrors the {@code search_knowledge.hits[*]}
 * field name so the agent does not have to translate between
 * {@code source_url} and {@code canonical_url}), {@code canonical_url_missing}
 * (data-quality observation when no Help_Site_URL is on file), and
 * {@code safe_to_show} (true iff published with a non-blank body).
 */
@Slf4j
@Component
public class ResolveArticleTool implements Tool {

    /**
     * Sprint 14 §L0 — error code surfaced when {@code resolve_article} is
     * called for an article whose {@code is_published=false}. The reject
     * reason is deterministic so trace UIs and L1 evaluators can detect
     * the published-safety refusal without parsing free text.
     */
    public static final String UNPUBLISHED_REJECT_REASON =
            "article_unpublished_safe_refuse";

    private final KbArticleRepository kbArticleRepository;

    public ResolveArticleTool(KbArticleRepository kbArticleRepository) {
        this.kbArticleRepository = kbArticleRepository;
    }

    @Override
    public String getName() {
        return "resolve_article";
    }

    @Override
    public ToolResult execute(BotSession session, Map<String, Object> parameters) {
        // Sprint 8.2 §M0a — canonical argument is `source_id` (matches the
        // projected tool schema and `search_knowledge.hits[*].source_id`).
        // `article_id` is accepted as a legacy alias so older callers/tests
        // keep working, but the projected schema and missing-parameter error
        // both name `source_id`.
        String sourceId = readStringParam(parameters, "source_id");
        if (sourceId == null) {
            sourceId = readStringParam(parameters, "article_id");
        }
        if (sourceId == null) {
            return ToolResult.error("Parameter 'source_id' is required");
        }

        Optional<KbArticle> articleOpt = kbArticleRepository.findById(sourceId);
        if (articleOpt.isEmpty()) {
            log.warn("Article not found: {}", sourceId);
            return ToolResult.error("Article not found: " + sourceId);
        }

        KbArticle article = articleOpt.get();

        // Sprint 14 §L0 — refuse unpublished articles deterministically.
        // The error string carries the canonical reject reason so the
        // trace surface / S1 evaluators can distinguish a published-safety
        // refusal from "article not found" or a generic transport error.
        if (Boolean.FALSE.equals(article.getIsPublished())) {
            log.warn("resolve_article refusing unpublished article: {}", sourceId);
            return ToolResult.error(UNPUBLISHED_REJECT_REASON
                    + ": article '" + sourceId + "' is not published");
        }

        String canonicalUrl = article.getSourceUrl();
        boolean canonicalUrlMissing = canonicalUrl == null || canonicalUrl.isBlank();
        String description = article.getDescription();
        boolean bodyMissing = description == null || description.isBlank();
        // safe_to_show is the conjunctive published-safety predicate. The
        // refuse-on-unpublished branch above keeps unpublished candidates
        // out entirely; this flag still surfaces "published but
        // body-missing" as a soft signal so downstream prompts can
        // decline to quote an empty article.
        boolean safeToShow = Boolean.TRUE.equals(article.getIsPublished()) && !bodyMissing;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("source_id", article.getArticleId());
        data.put("article_id", article.getArticleId());
        data.put("title", article.getTitle());
        data.put("summary", article.getSummary());
        data.put("description", description);
        // Keep the legacy `source_url` field for callers that already
        // read it, AND expose `canonical_url` so the agent sees the same
        // field name used by `search_knowledge.hits[*].canonical_url`.
        data.put("source_url", canonicalUrl);
        data.put("canonical_url", canonicalUrl);
        data.put("canonical_url_missing", canonicalUrlMissing);
        // R5 — single preferred citation token: the source_url when present
        // and non-blank, else the structural article_id fallback. The
        // must_cite_source guardrail accepts either shape.
        data.put("display_citation",
                !canonicalUrlMissing ? canonicalUrl : article.getArticleId());
        data.put("url_category", article.getUrlCategory());
        data.put("uc_tags", article.getUcTags());
        data.put("is_published", article.getIsPublished());
        data.put("safe_to_show", safeToShow);

        return ToolResult.ok(data);
    }

    private static String readStringParam(Map<String, Object> parameters, String name) {
        if (parameters == null) return null;
        Object raw = parameters.get(name);
        if (!(raw instanceof String s)) return null;
        return s.isBlank() ? null : s;
    }
}
