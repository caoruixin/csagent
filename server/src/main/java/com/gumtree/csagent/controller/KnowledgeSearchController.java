package com.gumtree.csagent.controller;

import com.gumtree.csagent.model.KnowledgeSearchRequest;
import com.gumtree.csagent.model.KnowledgeSearchResult;
import com.gumtree.csagent.service.knowledge.KnowledgeSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/faq")
public class KnowledgeSearchController {

    private final KnowledgeSearchService knowledgeSearchService;

    public KnowledgeSearchController(KnowledgeSearchService knowledgeSearchService) {
        this.knowledgeSearchService = knowledgeSearchService;
    }

    @PostMapping("/search")
    public ResponseEntity<KnowledgeSearchResult> search(@RequestBody KnowledgeSearchRequest request) {
        log.info("FAQ search request: query='{}', uc_tags={}", request.getQuery(), request.getUcTags());

        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity.badRequest().body(
                    KnowledgeSearchResult.builder()
                            .hits(java.util.List.of())
                            .faqMiss(true)
                            .retrievalMiss(true)
                            .answerMiss(false)
                            .build()
            );
        }

        try {
            KnowledgeSearchResult result = knowledgeSearchService.search(
                    request.getQuery(),
                    request.getUcTags(),
                    null, 0
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("FAQ search failed for query='{}': {}", request.getQuery(), e.getMessage(), e);
            return ResponseEntity.ok(
                    KnowledgeSearchResult.builder()
                            .hits(java.util.List.of())
                            .faqMiss(true)
                            .retrievalMiss(true)
                            .answerMiss(false)
                            .build()
            );
        }
    }
}
