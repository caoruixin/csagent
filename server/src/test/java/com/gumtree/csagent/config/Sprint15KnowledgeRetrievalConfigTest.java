package com.gumtree.csagent.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 15 §M2 — defaults parity + validation tests for
 * {@link KnowledgeRetrievalProperties}.
 *
 * <p>Defaults must remain exactly equivalent to the previously
 * hardcoded constants in {@code KnowledgeSearchService} and
 * {@code RerankService}. Sprint 15 explicitly does NOT tune
 * thresholds; the test pins each default by literal value.
 */
class Sprint15KnowledgeRetrievalConfigTest {

    @Test
    void defaults_matchPreviouslyHardcodedConstants() {
        KnowledgeRetrievalProperties props = new KnowledgeRetrievalProperties();
        // Pinned defaults (parity with the old constants):
        //   ANN_LIMIT                = 20
        //   RETRIEVAL_GATE_THRESHOLD = 0.3
        //   ANSWER_GATE_THRESHOLD    = 3.5
        //   RERANK_CANDIDATES        = 8
        //   TOP_RESULTS              = 3
        //   rerank fallback score    = 2.5
        assertEquals(20, props.getAnnLimit());
        assertEquals(0.3, props.getRetrievalGateThreshold(), 0.0);
        assertEquals(3.5, props.getAnswerGateThreshold(), 0.0);
        assertEquals(8, props.getRerankCandidates());
        assertEquals(3, props.getTopResults());
        assertEquals(2.5, props.getRerankFallbackScore(), 0.0);
    }

    @Test
    void defaults_passValidation() {
        // Should not throw; defaults are in valid ranges.
        new KnowledgeRetrievalProperties().validate();
    }

    @Test
    void validation_annLimitBelowOne_failsFast() {
        KnowledgeRetrievalProperties p = new KnowledgeRetrievalProperties();
        p.setAnnLimit(0);
        IllegalStateException ex = assertThrows(IllegalStateException.class, p::validate);
        assertTrue(ex.getMessage().contains("ann-limit"));
    }

    @Test
    void validation_annLimitLessThanRerankCandidates_failsFast() {
        KnowledgeRetrievalProperties p = new KnowledgeRetrievalProperties();
        p.setAnnLimit(4);
        p.setRerankCandidates(8);
        IllegalStateException ex = assertThrows(IllegalStateException.class, p::validate);
        assertTrue(ex.getMessage().contains("ann-limit"));
        assertTrue(ex.getMessage().contains("rerank-candidates"));
    }

    @Test
    void validation_rerankCandidatesLessThanTopResults_failsFast() {
        KnowledgeRetrievalProperties p = new KnowledgeRetrievalProperties();
        p.setRerankCandidates(2);
        p.setTopResults(3);
        IllegalStateException ex = assertThrows(IllegalStateException.class, p::validate);
        assertTrue(ex.getMessage().contains("rerank-candidates"));
        assertTrue(ex.getMessage().contains("top-results"));
    }

    @Test
    void validation_retrievalGateOutOfRange_failsFast() {
        KnowledgeRetrievalProperties p = new KnowledgeRetrievalProperties();
        p.setRetrievalGateThreshold(1.5);
        IllegalStateException ex = assertThrows(IllegalStateException.class, p::validate);
        assertTrue(ex.getMessage().contains("retrieval-gate-threshold"));
    }

    @Test
    void validation_answerGateOutOfRange_failsFast() {
        KnowledgeRetrievalProperties p = new KnowledgeRetrievalProperties();
        p.setAnswerGateThreshold(0.5);
        IllegalStateException ex = assertThrows(IllegalStateException.class, p::validate);
        assertTrue(ex.getMessage().contains("answer-gate-threshold"));
    }

    @Test
    void validation_fallbackAtOrAboveAnswerGate_failsFast() {
        KnowledgeRetrievalProperties p = new KnowledgeRetrievalProperties();
        p.setAnswerGateThreshold(3.5);
        p.setRerankFallbackScore(3.5);
        IllegalStateException ex = assertThrows(IllegalStateException.class, p::validate);
        assertTrue(ex.getMessage().contains("rerank-fallback-score"));
        assertTrue(ex.getMessage().contains("answer-gate-threshold"));
    }

    @Test
    void validation_fallbackOutOfRange_failsFast() {
        KnowledgeRetrievalProperties p = new KnowledgeRetrievalProperties();
        p.setRerankFallbackScore(0.5);
        IllegalStateException ex = assertThrows(IllegalStateException.class, p::validate);
        assertTrue(ex.getMessage().contains("rerank-fallback-score"));
    }
}
