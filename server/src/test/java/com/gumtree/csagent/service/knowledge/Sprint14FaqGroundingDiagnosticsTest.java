package com.gumtree.csagent.service.knowledge;

import com.gumtree.csagent.model.ToolEvent;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 14 §L2 — FAQ grounding contract + soft diagnostics regression.
 *
 * <p>Pins the {@link FaqOutputClass} taxonomy, the
 * {@link FaqOutputClassifier} heuristic decisions, and the
 * {@link FaqGroundingDiagnostics#compute} result table. Diagnostics are
 * observability-only — none of these tests gate the response or assert
 * any escalation/reroute decision.
 */
class Sprint14FaqGroundingDiagnosticsTest {

    private static final String SEARCH_TOOL = "search_knowledge";
    private static final String RESOLVE_TOOL = "resolve_article";

    // -------------------------------------------------------------------
    //  L2 — output class taxonomy
    // -------------------------------------------------------------------

    @Test
    void outputClass_factualAnswer_isTheOnlyClassThatRequiresGrounding() {
        assertTrue(FaqOutputClass.FACTUAL_ANSWER.requiresGrounding());
        assertFalse(FaqOutputClass.CLARIFICATION.requiresGrounding());
        assertFalse(FaqOutputClass.EMPATHY_ACK.requiresGrounding());
        assertFalse(FaqOutputClass.HANDOVER.requiresGrounding());
        assertFalse(FaqOutputClass.TOOL_STATUS.requiresGrounding());
        assertFalse(FaqOutputClass.INTAKE_COLLECTION.requiresGrounding());
    }

    @Test
    void classifier_clarification_questionShape() {
        FaqOutputClass cls = FaqOutputClassifier.classify(
                "Could you share the ad ID so I can take a closer look?",
                FaqOutputClassifier.ClassifierContext.empty());
        assertEquals(FaqOutputClass.CLARIFICATION, cls);
    }

    @Test
    void classifier_intakeCollection_intakeUcContext() {
        FaqOutputClass cls = FaqOutputClassifier.classify(
                "To proceed, please provide your email and the ad ID.",
                new FaqOutputClassifier.ClassifierContext(true, false));
        assertEquals(FaqOutputClass.INTAKE_COLLECTION, cls);
    }

    @Test
    void classifier_empathyAck_apologyShape() {
        FaqOutputClass cls = FaqOutputClassifier.classify(
                "I'm so sorry that happened.",
                FaqOutputClassifier.ClassifierContext.empty());
        assertEquals(FaqOutputClass.EMPATHY_ACK, cls);
    }

    @Test
    void classifier_handover_handoverDispatchedFlag() {
        FaqOutputClass cls = FaqOutputClassifier.classify(
                "Here is some general advice.",
                new FaqOutputClassifier.ClassifierContext(false, true));
        assertEquals(FaqOutputClass.HANDOVER, cls,
                "handover_dispatched runtime flag must override heuristic word matching");
    }

    @Test
    void classifier_toolStatus_lookingIntoIt() {
        FaqOutputClass cls = FaqOutputClassifier.classify(
                "I'm looking into this for you.",
                FaqOutputClassifier.ClassifierContext.empty());
        assertEquals(FaqOutputClass.TOOL_STATUS, cls);
    }

    @Test
    void classifier_factualAnswer_defaultWhenNoOtherShapeMatches() {
        FaqOutputClass cls = FaqOutputClassifier.classify(
                "Ads can be hidden during moderation review for up to 24 hours.",
                FaqOutputClassifier.ClassifierContext.empty());
        assertEquals(FaqOutputClass.FACTUAL_ANSWER, cls);
    }

    // -------------------------------------------------------------------
    //  L2 — diagnostic state table
    // -------------------------------------------------------------------

    @Test
    void diagnostics_factualGrounded_whenCitationMatchesResolvedSet() {
        SourceEvidenceLineage lineage = SourceEvidenceLineage.fromToolEvents(
                List.of(searchEvent(0, hit("ka4P200000001AbIAI",
                                "Where is my advert",
                                "https://help.gumtree.com/where-is-my-ad")),
                        resolveSuccessEvent(1, "ka4P200000001AbIAI",
                                "Where is my advert",
                                "https://help.gumtree.com/where-is-my-ad")),
                "Per article ka4P200000001AbIAI, ads can be hidden for up to 24 hours.");

        FaqGroundingDiagnostics dx = FaqGroundingDiagnostics.compute(
                FaqOutputClass.FACTUAL_ANSWER, lineage);

        assertEquals(FaqGroundingDiagnostics.STATE_FACTUAL_GROUNDED, dx.faqGroundingState());
        assertTrue(dx.citationPresent());
        assertTrue(dx.citationMatch());
        assertFalse(dx.citationDrift());
        assertFalse(dx.resolvedButUncited());
        assertFalse(dx.retrievedButUnresolved());
    }

    @Test
    void diagnostics_factualUncited_whenResolvedButNoCitation() {
        SourceEvidenceLineage lineage = SourceEvidenceLineage.fromToolEvents(
                List.of(searchEvent(0, hit("ka4P200000001AbIAI",
                                "Where is my advert",
                                "https://help.gumtree.com/where-is-my-ad")),
                        resolveSuccessEvent(1, "ka4P200000001AbIAI",
                                "Where is my advert",
                                "https://help.gumtree.com/where-is-my-ad")),
                "Ads can be hidden during moderation review.");

        FaqGroundingDiagnostics dx = FaqGroundingDiagnostics.compute(
                FaqOutputClass.FACTUAL_ANSWER, lineage);

        assertEquals(FaqGroundingDiagnostics.STATE_FACTUAL_UNCITED, dx.faqGroundingState());
        assertFalse(dx.citationPresent());
        assertTrue(dx.resolvedButUncited(),
                "resolved_but_uncited must surface as a soft diagnostic");
    }

    @Test
    void diagnostics_factualUnresolved_whenSearchRanButNoResolve() {
        SourceEvidenceLineage lineage = SourceEvidenceLineage.fromToolEvents(
                List.of(searchEvent(0, hit("ka4P200000001AbIAI",
                        "Where is my advert",
                        "https://help.gumtree.com/where-is-my-ad"))),
                "Ads can be hidden during moderation review.");

        FaqGroundingDiagnostics dx = FaqGroundingDiagnostics.compute(
                FaqOutputClass.FACTUAL_ANSWER, lineage);

        assertEquals(FaqGroundingDiagnostics.STATE_FACTUAL_UNRESOLVED, dx.faqGroundingState());
        assertTrue(dx.retrievedButUnresolved(),
                "retrieved_but_unresolved must surface so §G2 gaps are observable");
        assertFalse(dx.citationPresent());
    }

    @Test
    void diagnostics_factualUnretrieved_whenNoSearchAndFactualClass() {
        SourceEvidenceLineage lineage = SourceEvidenceLineage.fromToolEvents(
                List.of(),
                "Ads can be hidden during moderation review.");

        FaqGroundingDiagnostics dx = FaqGroundingDiagnostics.compute(
                FaqOutputClass.FACTUAL_ANSWER, lineage);

        assertEquals(FaqGroundingDiagnostics.STATE_FACTUAL_UNRETRIEVED, dx.faqGroundingState());
        assertFalse(dx.citationPresent());
        assertFalse(dx.citationMatch());
    }

    @Test
    void diagnostics_citationDrift_whenBotCitesOffCandidateUrl() {
        SourceEvidenceLineage lineage = SourceEvidenceLineage.fromToolEvents(
                List.of(searchEvent(0, hit("ka4P200000001AbIAI",
                                "Property Scams",
                                "https://help.gumtree.com/property-scams")),
                        resolveSuccessEvent(1, "ka4P200000001AbIAI",
                                "Property Scams",
                                "https://help.gumtree.com/property-scams")),
                "You can read more at https://www.gov.uk/consumer-rights.");

        FaqGroundingDiagnostics dx = FaqGroundingDiagnostics.compute(
                FaqOutputClass.FACTUAL_ANSWER, lineage);

        assertTrue(dx.citationPresent(),
                "free-form URL citation still counts as a citation event");
        assertTrue(dx.citationDrift(),
                "URL pointing at a non-candidate domain must surface as citation_drift");
        assertFalse(dx.citationMatch(),
                "no source_id citation, so citation_match stays false");
        assertTrue(dx.resolvedButUncited(),
                "resolved article was not credited by the cited URL");
    }

    @Test
    void diagnostics_nonFactualClasses_skipGrounding() {
        // For clarification / empathy / handover / intake / tool_status,
        // grounding state must be `non_factual` and the per-source flags
        // must NOT be set. Sprint 14 §L2 explicitly carves these out.
        for (FaqOutputClass cls : new FaqOutputClass[]{
                FaqOutputClass.CLARIFICATION,
                FaqOutputClass.EMPATHY_ACK,
                FaqOutputClass.HANDOVER,
                FaqOutputClass.TOOL_STATUS,
                FaqOutputClass.INTAKE_COLLECTION
        }) {
            SourceEvidenceLineage lineage = SourceEvidenceLineage.fromToolEvents(
                    List.of(searchEvent(0, hit("ka4P200000001AbIAI",
                                    "Where is my advert",
                                    "https://help.gumtree.com/where-is-my-ad")),
                            resolveSuccessEvent(1, "ka4P200000001AbIAI",
                                    "Where is my advert",
                                    "https://help.gumtree.com/where-is-my-ad")),
                    "Could you share the ad ID?");

            FaqGroundingDiagnostics dx = FaqGroundingDiagnostics.compute(cls, lineage);
            assertEquals(FaqGroundingDiagnostics.STATE_NON_FACTUAL,
                    dx.faqGroundingState(),
                    "class " + cls + " must NOT trigger factual grounding state");
        }
    }

    @Test
    void diagnostics_missingCitationIsObservable_neverBlockingByContract() {
        // Sprint 14 §L2 — the diagnostics record must compute and return
        // an observability surface even for a missing-citation factual
        // answer. The presence of `STATE_FACTUAL_UNCITED` AND the
        // observable `resolved_but_uncited=true` flag IS the contract; no
        // exception, no rejection, no rewrite.
        SourceEvidenceLineage lineage = SourceEvidenceLineage.fromToolEvents(
                List.of(searchEvent(0, hit("ka4P200000001AbIAI",
                                "Where is my advert",
                                "https://help.gumtree.com/where-is-my-ad")),
                        resolveSuccessEvent(1, "ka4P200000001AbIAI",
                                "Where is my advert",
                                "https://help.gumtree.com/where-is-my-ad")),
                "Ads can be hidden during moderation review.");

        FaqGroundingDiagnostics dx = FaqGroundingDiagnostics.compute(
                FaqOutputClass.FACTUAL_ANSWER, lineage);

        assertEquals(FaqGroundingDiagnostics.STATE_FACTUAL_UNCITED, dx.faqGroundingState());
        assertTrue(dx.resolvedButUncited());
        // Sprint 14 §L2 contract: no further side-effects exposed beyond
        // the observability fields. Compute returned cleanly.
    }

    // -------------------------------------------------------------------
    //  Test helpers
    // -------------------------------------------------------------------

    private static ToolEvent searchEvent(int seq, Map<String, Object> hit) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hits", List.of(hit));
        data.put("faq_miss", false);
        data.put("retrieval_miss", false);
        data.put("answer_miss", false);
        return new ToolEvent(seq, seq, SEARCH_TOOL,
                Map.of("query", "test"), true, data, null, 12L);
    }

    private static Map<String, Object> hit(String sourceId, String title,
                                             String canonicalUrl) {
        Map<String, Object> hit = new LinkedHashMap<>();
        hit.put("source_id", sourceId);
        hit.put("title", title);
        hit.put("snippet", "snippet");
        hit.put("canonical_url", canonicalUrl);
        hit.put("canonical_url_missing", canonicalUrl == null || canonicalUrl.isBlank());
        hit.put("score", 4.5);
        return hit;
    }

    private static ToolEvent resolveSuccessEvent(int seq, String sourceId, String title,
                                                   String canonicalUrl) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("source_id", sourceId);
        data.put("article_id", sourceId);
        data.put("title", title);
        data.put("description", "Body of article " + sourceId);
        data.put("source_url", canonicalUrl);
        data.put("canonical_url", canonicalUrl);
        data.put("canonical_url_missing", canonicalUrl == null || canonicalUrl.isBlank());
        data.put("is_published", true);
        data.put("safe_to_show", true);
        return new ToolEvent(seq, seq, RESOLVE_TOOL,
                Map.of("source_id", sourceId), true, data, null, 8L);
    }
}
