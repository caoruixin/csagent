package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

/**
 * Loads control-policy.yaml at startup. Provides budget values
 * and phase transition validation.
 */
@Slf4j
@Service
public class ControlPolicyService {

    private int maxClarificationRounds;
    private int maxFaqMiss;
    private int maxBotTurnsFaq;
    private int maxBotTurnsIntake;
    private int maxRepeatedSameAction;
    private int maxTotalBotTurns;
    private double minorDriftSimilarity;
    private double softShiftConfidence;

    private final Map<String, List<String>> phaseTransitions = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        try {
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            InputStream is = new ClassPathResource("config/control-policy.yaml").getInputStream();
            JsonNode root = yamlMapper.readTree(is);

            // Parse budgets
            JsonNode budgets = root.get("budgets");
            maxClarificationRounds = budgets.get("max-clarification-rounds").asInt();
            maxFaqMiss = budgets.get("max-faq-miss").asInt();
            maxBotTurnsFaq = budgets.get("max-bot-turns-faq").asInt();
            maxBotTurnsIntake = budgets.get("max-bot-turns-intake").asInt();
            maxRepeatedSameAction = budgets.get("max-repeated-same-action").asInt();
            maxTotalBotTurns = budgets.get("max-total-bot-turns").asInt();

            // Parse phase transitions
            JsonNode transitions = root.get("phase-transitions");
            transitions.fields().forEachRemaining(entry -> {
                List<String> targets = new ArrayList<>();
                entry.getValue().forEach(t -> targets.add(t.asText()));
                phaseTransitions.put(entry.getKey(), targets);
            });

            // Parse drift thresholds
            JsonNode drift = root.get("drift-thresholds");
            minorDriftSimilarity = drift.get("minor-drift-similarity").asDouble();
            softShiftConfidence = drift.get("soft-shift-confidence").asDouble();

            log.info("Control policy loaded: maxClarification={}, maxFaqMiss={}, maxBotTurnsFaq={}, " +
                     "maxBotTurnsIntake={}, maxRepeatedAction={}, maxTotalTurns={}",
                    maxClarificationRounds, maxFaqMiss, maxBotTurnsFaq,
                    maxBotTurnsIntake, maxRepeatedSameAction, maxTotalBotTurns);

        } catch (Exception e) {
            log.error("Failed to load control-policy.yaml: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load control-policy.yaml", e);
        }
    }

    /**
     * Check if a phase transition is valid.
     */
    public boolean isValidTransition(String from, String to) {
        List<String> allowed = phaseTransitions.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * Get allowed target phases from a given phase.
     */
    public List<String> getAllowedTransitions(String from) {
        return phaseTransitions.getOrDefault(from, List.of());
    }

    public int getMaxClarificationRounds() {
        return maxClarificationRounds;
    }

    public int getMaxFaqMiss() {
        return maxFaqMiss;
    }

    public int getMaxBotTurnsFaq(){
        return maxBotTurnsFaq;
    }

    public int getMaxBotTurnsIntake() {
        return maxBotTurnsIntake;
    }

    public int getMaxRepeatedSameAction() {
        return maxRepeatedSameAction;
    }

    public int getMaxTotalBotTurns() {
        return maxTotalBotTurns;
    }

    public double getMinorDriftSimilarity() {
        return minorDriftSimilarity;
    }

    public double getSoftShiftConfidence() {
        return softShiftConfidence;
    }
}
