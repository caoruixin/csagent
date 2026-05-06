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
 */
@Slf4j
@Component
public class ResolveArticleTool implements Tool {

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
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("source_id", article.getArticleId());
        data.put("article_id", article.getArticleId());
        data.put("title", article.getTitle());
        data.put("summary", article.getSummary());
        data.put("description", article.getDescription());
        data.put("source_url", article.getSourceUrl());
        data.put("url_category", article.getUrlCategory());
        data.put("uc_tags", article.getUcTags());
        data.put("is_published", article.getIsPublished());

        return ToolResult.ok(data);
    }

    private static String readStringParam(Map<String, Object> parameters, String name) {
        if (parameters == null) return null;
        Object raw = parameters.get(name);
        if (!(raw instanceof String s)) return null;
        return s.isBlank() ? null : s;
    }
}
