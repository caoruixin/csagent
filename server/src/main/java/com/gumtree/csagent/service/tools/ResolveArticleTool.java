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
        String articleId = (String) parameters.get("article_id");
        if (articleId == null || articleId.isBlank()) {
            return ToolResult.error("Parameter 'article_id' is required");
        }

        Optional<KbArticle> articleOpt = kbArticleRepository.findById(articleId);
        if (articleOpt.isEmpty()) {
            log.warn("Article not found: {}", articleId);
            return ToolResult.error("Article not found: " + articleId);
        }

        KbArticle article = articleOpt.get();
        Map<String, Object> data = new LinkedHashMap<>();
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
}
