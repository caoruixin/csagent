package com.gumtree.csagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeSearchResult {

    private List<KnowledgeHit> hits;

    @JsonProperty("faq_miss")
    private boolean faqMiss;

    @JsonProperty("retrieval_miss")
    private boolean retrievalMiss;

    @JsonProperty("answer_miss")
    private boolean answerMiss;
}
