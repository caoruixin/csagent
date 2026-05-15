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

    /**
     * Sprint 14 §L0 — flag set when the article was indexed without a
     * Help_Site_URL / canonical URL. Surfaces missing-URL cases as a
     * data-quality observation rather than silently coalescing them into
     * a null {@link #canonicalUrl}. Soft / observability-only — the
     * agent-visible {@code canonical_url} field still carries
     * {@code null} so prompt logic that checks for a present URL keeps
     * working, while trace evidence and the L0 audit report can
     * distinguish "no URL because data quality gap" from "URL present".
     */
    @JsonProperty("canonical_url_missing")
    @lombok.Builder.Default
    private boolean canonicalUrlMissing = false;

    private double score;
}
