package com.gumtree.csagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeHit {

    @JsonProperty("source_id")
    private String sourceId;

    private String title;

    private String snippet;

    @JsonProperty("canonical_url")
    private String canonicalUrl;

    private double score;
}
