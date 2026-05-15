package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.KnowledgeHit;
import com.gumtree.csagent.model.KnowledgeSearchResult;
import com.gumtree.csagent.service.knowledge.KnowledgeSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Searches the knowledge base for relevant articles.
 * AGENT_VISIBLE — the LLM may call this tool directly.
 */
@Slf4j
@Component
public class SearchKnowledgeTool implements Tool {

    private final KnowledgeSearchService knowledgeSearchService;

    public SearchKnowledgeTool(KnowledgeSearchService knowledgeSearchService) {
        this.knowledgeSearchService = knowledgeSearchService;
    }

    @Override
    public String getName() {
        return "search_knowledge";
    }

    @Override
    public ToolResult execute(BotSession session, Map<String, Object> parameters) {
        String query = (String) parameters.get("query");
        if (query == null || query.isBlank()) {
            return ToolResult.error("Parameter 'query' is required");
        }

        // Build UC tag list from session context
        List<String> ucTags = new ArrayList<>();
        if (session.getActiveUseCase() != null) {
            ucTags.add(session.getActiveUseCase());
        }
        if (session.getCandidateUseCases() != null) {
            for (String uc : session.getCandidateUseCases()) {
                if (!ucTags.contains(uc)) {
                    ucTags.add(uc);
                }
            }
        }

        KnowledgeSearchResult result = knowledgeSearchService.search(query, ucTags,
                session.getSessionId(),
                session.getTotalBotTurns() != null ? session.getTotalBotTurns() : 0);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("faq_miss", result.isFaqMiss());
        data.put("retrieval_miss", result.isRetrievalMiss());
        data.put("answer_miss", result.isAnswerMiss());
        data.put("hits", result.getHits().stream().map(hit -> {
            Map<String, Object> hitMap = new LinkedHashMap<>();
            hitMap.put("source_id", hit.getSourceId());
            hitMap.put("title", hit.getTitle());
            hitMap.put("snippet", hit.getSnippet());
            hitMap.put("canonical_url", hit.getCanonicalUrl());
            // Sprint 14 §L0 — surface missing-URL cases so downstream
            // observability can classify a missing canonical_url as a
            // data-quality gap rather than ambiguity.
            hitMap.put("canonical_url_missing", hit.isCanonicalUrlMissing());
            hitMap.put("score", hit.getScore());
            return hitMap;
        }).collect(Collectors.toList()));

        return ToolResult.ok(data);
    }
}
