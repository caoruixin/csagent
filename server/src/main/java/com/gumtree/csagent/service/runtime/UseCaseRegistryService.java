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
import java.util.stream.Collectors;

/**
 * Loads use-case-registry.yaml at startup and provides lookup methods
 * for use case definitions, topic priors, and out-of-scope categories.
 */
@Slf4j
@Service
public class UseCaseRegistryService {

    private final Map<String, UseCaseDefinition> useCases = new LinkedHashMap<>();
    private final Map<String, String> strongPriorTopics = new LinkedHashMap<>();
    private final Set<String> weakPriorTopics = new LinkedHashSet<>();
    private final Set<String> handoverOnlyTopics = new LinkedHashSet<>();
    private final Map<String, String> outOfScopeCategories = new LinkedHashMap<>(); // topic-subject -> category key

    @PostConstruct
    public void init() {
        try {
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            InputStream is = new ClassPathResource("config/use-case-registry.yaml").getInputStream();
            JsonNode root = yamlMapper.readTree(is);

            // Parse use cases
            JsonNode ucNode = root.get("use-cases");
            if (ucNode != null) {
                ucNode.fields().forEachRemaining(entry -> {
                    String ucId = entry.getKey();
                    JsonNode def = entry.getValue();
                    List<String> topics = new ArrayList<>();
                    if (def.has("topic-subjects")) {
                        def.get("topic-subjects").forEach(t -> topics.add(t.asText()));
                    }
                    UseCaseDefinition ucDef = new UseCaseDefinition(
                            ucId,
                            def.get("name").asText(),
                            topics,
                            def.get("risk-level").asText(),
                            def.get("allow-bot-resolution").asBoolean(),
                            def.get("path").asText()
                    );
                    useCases.put(ucId, ucDef);
                });
            }

            // Parse out-of-scope
            JsonNode oosNode = root.get("out-of-scope");
            if (oosNode != null) {
                oosNode.fields().forEachRemaining(entry -> {
                    String categoryKey = entry.getKey();
                    String topicSubject = entry.getValue().get("topic-subject").asText();
                    outOfScopeCategories.put(topicSubject, categoryKey);
                });
            }

            // Parse strong-prior-topics
            JsonNode strongNode = root.get("strong-prior-topics");
            if (strongNode != null) {
                strongNode.fields().forEachRemaining(entry ->
                        strongPriorTopics.put(entry.getKey(), entry.getValue().asText()));
            }

            // Parse weak-prior-topics
            JsonNode weakNode = root.get("weak-prior-topics");
            if (weakNode != null) {
                weakNode.forEach(t -> weakPriorTopics.add(t.asText()));
            }

            // Parse handover-only-topics
            JsonNode handoverNode = root.get("handover-only-topics");
            if (handoverNode != null) {
                handoverNode.forEach(t -> handoverOnlyTopics.add(t.asText()));
            }

            log.info("Loaded {} use cases, {} strong priors, {} weak priors, {} handover-only, {} out-of-scope",
                    useCases.size(), strongPriorTopics.size(), weakPriorTopics.size(),
                    handoverOnlyTopics.size(), outOfScopeCategories.size());

        } catch (Exception e) {
            log.error("Failed to load use-case-registry.yaml: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load use-case-registry.yaml", e);
        }
    }

    /**
     * Get a use case definition by ID.
     */
    public UseCaseDefinition getUseCase(String ucId) {
        return useCases.get(ucId);
    }

    /**
     * Cheap lookup helper: returns true iff {@code ucId} is a known UC in the
     * loaded registry. Added 2026-05-02 for the {@code classify_use_case} tool
     * (Phase 2 Fix 3c) so the LLM cannot commit an out-of-registry UC string.
     */
    public boolean isKnownUseCase(String ucId) {
        return ucId != null && useCases.containsKey(ucId);
    }

    /** Registry {@code path} value: FAQ-only — may retrieve knowledge, never collects intake. */
    public static final String PATH_FAQ = "FAQ";
    /** Registry {@code path} value: intake-only — collects intake fields, never retrieves knowledge. */
    public static final String PATH_INTAKE = "INTAKE";
    /**
     * Registry {@code path} value: both — may retrieve knowledge AND collect intake
     * fields. The LLM owns which of the two a given turn needs (Constitution §1.3);
     * the Runtime only publishes the capability (§1.4).
     */
    public static final String PATH_PARTIAL = "PARTIAL";

    /**
     * WS-3 / A3 (2026-07-25) — the three routing predicates the runtime derives
     * from {@code use-case-registry.yaml}'s {@code path} field.
     *
     * <p>Before this, four Java classes each carried their own literal
     * {@code INTAKE_UCS = {UC-G, UC-H, UC-I, UC-J, UC-K}} constant
     * ({@code PhaseEvaluator}, {@code ControlKernel},
     * {@code ResolveDispositionEvaluator}, plus two eval-side Python sets that
     * already disagreed with each other over UC-K). Duplicated sets drift; the
     * registry is now the single declaration and these predicates are the only
     * supported way to ask the question.
     *
     * <p>An unknown / unregistered UC is treated as intake-only and
     * not-knowledge-capable — the conservative direction (no retrieval on an
     * unrecognised UC).
     */
    public boolean isIntakeOnlyPath(String ucId) {
        return PATH_INTAKE.equals(pathOf(ucId));
    }

    /**
     * True when the UC's registry {@code path} permits knowledge retrieval and a
     * grounded, bot-authored answer ({@code FAQ} or {@code PARTIAL}).
     */
    public boolean isKnowledgeCapablePath(String ucId) {
        String path = pathOf(ucId);
        return PATH_FAQ.equals(path) || PATH_PARTIAL.equals(path);
    }

    /**
     * True when the UC's registry {@code path} requires intake-field collection
     * before a handover can be considered complete ({@code INTAKE} or
     * {@code PARTIAL}).
     */
    public boolean collectsIntakeFields(String ucId) {
        String path = pathOf(ucId);
        return PATH_INTAKE.equals(path) || PATH_PARTIAL.equals(path);
    }

    /**
     * The raw registry {@code path} for a UC, or {@code null} when the UC is
     * unknown or carries no path. Prefer the three predicates above; this
     * accessor exists for logging / diagnostics.
     */
    public String pathOf(String ucId) {
        UseCaseDefinition def = getUseCase(ucId);
        return def == null ? null : def.path();
    }

    /**
     * Get all use case definitions.
     */
    public Map<String, UseCaseDefinition> getAllUseCases() {
        return Collections.unmodifiableMap(useCases);
    }

    /**
     * Check if a topic subject maps to a strong prior UC.
     */
    public Optional<String> getStrongPriorTopic(String topicSubject) {
        return Optional.ofNullable(strongPriorTopics.get(topicSubject));
    }

    /**
     * Check if a topic subject is a handover-only (out of scope) topic.
     */
    public boolean isHandoverOnlyTopic(String topicSubject) {
        return handoverOnlyTopics.contains(topicSubject);
    }

    /**
     * Check if a topic subject is a weak prior topic.
     */
    public boolean isWeakPriorTopic(String topicSubject) {
        return weakPriorTopics.contains(topicSubject);
    }

    /**
     * Get the out-of-scope category key for a topic subject.
     */
    public Optional<String> getOutOfScopeCategory(String topicSubject) {
        return Optional.ofNullable(outOfScopeCategories.get(topicSubject));
    }

    /**
     * Get candidate UC IDs that match a given topic subject.
     * For "Account Support", returns the full candidate set (all UCs).
     */
    public List<String> getCandidateUcsForTopic(String topicSubject) {
        if ("Account Support".equals(topicSubject)) {
            // Account Support gets full candidate set
            return new ArrayList<>(useCases.keySet());
        }
        return useCases.entrySet().stream()
                .filter(e -> e.getValue().topicSubjects().contains(topicSubject))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Use case definition record.
     */
    public record UseCaseDefinition(
            String ucId,
            String name,
            List<String> topicSubjects,
            String riskLevel,
            boolean allowBotResolution,
            String path
    ) {}
}
