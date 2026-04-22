package com.gumtree.csagent.eval.harness;

import com.gumtree.csagent.eval.config.EvalConfig;
import com.gumtree.csagent.eval.model.EvalSession;
import com.gumtree.csagent.eval.model.EvalTurn;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads eval datasets from CSV files and overlays human review annotations.
 *
 * Dataset CSV common columns:
 *   Id, CaseId, StartTime, EndTime, Platform, Subject, Reason, CSAT,
 *   total_turns, agent_turns, visitor_turns, primary_uc, all_ucs, uc_confidence,
 *   has_escalation_signal, has_frustration_signal, has_identifier_collection,
 *   escalation_evidence, frustration_evidence, identifier_evidence,
 *   quality_score, tags, eval_tier, (+ dataset-specific trailing columns)
 *
 * Turns CSV columns:
 *   conversation_id, case_id, sequence, relative_time_sec, role, speaker, message_redacted
 *
 * HR annotation columns (keyed by session_id):
 *   session_id, reviewer, review_date, source_dataset, case_id, ...
 */
@Slf4j
@Component
public class DatasetLoader {

    private final EvalConfig config;

    public DatasetLoader(EvalConfig config) {
        this.config = config;
    }

    /**
     * Load all sessions for the specified datasets, with HR annotation overlay.
     */
    public List<EvalSession> loadDatasets(List<String> datasetNames) {
        List<EvalSession> allSessions = new ArrayList<>();

        // Load HR annotations first
        Map<String, Map<String, String>> hrAnnotations = loadAnnotations();
        log.info("Loaded {} HR annotations", hrAnnotations.size());

        for (String dsName : datasetNames) {
            try {
                List<EvalSession> sessions = loadSingleDataset(dsName);
                log.info("Loaded {} sessions from dataset: {}", sessions.size(), dsName);
                allSessions.addAll(sessions);
            } catch (Exception e) {
                log.error("Failed to load dataset: {}", dsName, e);
            }
        }

        // Overlay HR annotations
        int annotated = 0;
        for (EvalSession session : allSessions) {
            Map<String, String> annotation = hrAnnotations.get(session.getId());
            if (annotation != null) {
                overlayAnnotation(session, annotation);
                annotated++;
            }
        }
        log.info("Applied HR annotations to {} / {} sessions", annotated, allSessions.size());

        return allSessions;
    }

    private List<EvalSession> loadSingleDataset(String datasetName) throws IOException {
        Path datasetDir = Path.of(config.getDatasetDir());

        // Resolve dataset CSV file — try direct name, then with _dataset suffix
        Path datasetCsv = resolveDatasetFile(datasetDir, datasetName);
        Path turnsCsv = resolveTurnsFile(datasetDir, datasetName);

        if (datasetCsv == null) {
            log.warn("Dataset CSV not found for: {}", datasetName);
            return List.of();
        }

        // Parse dataset CSV
        List<Map<String, String>> datasetRows = readCsv(datasetCsv);
        Map<String, List<EvalTurn>> turnsBySession = new HashMap<>();

        // Parse turns CSV if present
        if (turnsCsv != null) {
            List<Map<String, String>> turnsRows = readCsv(turnsCsv);
            for (Map<String, String> row : turnsRows) {
                String convId = row.get("conversation_id");
                EvalTurn turn = EvalTurn.builder()
                        .conversationId(convId)
                        .caseId(row.getOrDefault("case_id", ""))
                        .sequence(parseIntSafe(row.get("sequence")))
                        .relativeTimeSec(parseIntSafe(row.get("relative_time_sec")))
                        .role(row.getOrDefault("role", ""))
                        .speaker(row.getOrDefault("speaker", ""))
                        .messageRedacted(row.getOrDefault("message_redacted", ""))
                        .build();
                turnsBySession.computeIfAbsent(convId, k -> new ArrayList<>()).add(turn);
            }
            // Sort turns by sequence
            turnsBySession.values().forEach(turns ->
                    turns.sort(Comparator.comparingInt(EvalTurn::getSequence)));
        }

        // Build sessions
        List<EvalSession> sessions = new ArrayList<>();
        for (Map<String, String> row : datasetRows) {
            String id = row.get("Id");
            EvalSession session = EvalSession.builder()
                    .id(id)
                    .caseId(row.getOrDefault("CaseId", ""))
                    .sourceDataset(datasetName)
                    .subject(row.getOrDefault("Subject", ""))
                    .reason(row.getOrDefault("Reason", ""))
                    .platform(row.getOrDefault("Platform", ""))
                    .primaryUc(row.getOrDefault("primary_uc", ""))
                    .allUcs(row.getOrDefault("all_ucs", ""))
                    .ucConfidence(row.getOrDefault("uc_confidence", ""))
                    .hasEscalationSignal(parseBoolSafe(row.get("has_escalation_signal")))
                    .hasFrustrationSignal(parseBoolSafe(row.get("has_frustration_signal")))
                    .hasIdentifierCollection(parseBoolSafe(row.get("has_identifier_collection")))
                    .escalationEvidence(row.getOrDefault("escalation_evidence", ""))
                    .frustrationEvidence(row.getOrDefault("frustration_evidence", ""))
                    .identifierEvidence(row.getOrDefault("identifier_evidence", ""))
                    .qualityScore(parseIntSafe(row.get("quality_score")))
                    .tags(row.getOrDefault("tags", ""))
                    .evalTier(row.getOrDefault("eval_tier", ""))
                    .turns(turnsBySession.getOrDefault(id, List.of()))
                    .build();

            // Capture dataset-specific extra columns
            Set<String> knownColumns = Set.of(
                    "Id", "CaseId", "StartTime", "EndTime", "Platform", "Subject", "Reason", "CSAT",
                    "total_turns", "agent_turns", "visitor_turns", "primary_uc", "all_ucs", "uc_confidence",
                    "has_escalation_signal", "has_frustration_signal", "has_identifier_collection",
                    "escalation_evidence", "frustration_evidence", "identifier_evidence",
                    "quality_score", "tags", "eval_tier");
            Map<String, String> extras = new HashMap<>();
            for (Map.Entry<String, String> entry : row.entrySet()) {
                if (!knownColumns.contains(entry.getKey())) {
                    extras.put(entry.getKey(), entry.getValue());
                }
            }
            session.setExtraFields(extras);

            // Extract description from pre-chat form turn if available
            EvalTurn formTurn = session.getPreChatFormTurn();
            if (formTurn != null) {
                String msg = formTurn.getMessageRedacted();
                // Parse "[Form] Subject: X | Description: Y" format
                if (msg != null && msg.contains("Description:")) {
                    int descIdx = msg.indexOf("Description:");
                    session.setDescription(msg.substring(descIdx + "Description:".length()).trim());
                }
            }

            sessions.add(session);
        }
        return sessions;
    }

    private Path resolveDatasetFile(Path dir, String datasetName) {
        // Try: name.csv, name_dataset.csv, name_bank.csv
        for (String suffix : List.of("", "_dataset", "_bank")) {
            String baseName = datasetName.endsWith(suffix) ? datasetName : datasetName + suffix;
            Path p = dir.resolve(baseName + ".csv");
            if (Files.exists(p)) return p;
        }
        // Try stripping _dataset suffix and checking bare name
        String bare = datasetName.replace("_dataset", "").replace("_bank", "");
        Path p = dir.resolve(bare + ".csv");
        if (Files.exists(p)) return p;
        return null;
    }

    private Path resolveTurnsFile(Path dir, String datasetName) {
        // Derive turns filename: golden_dataset -> golden_turns, badcase_bank -> badcase_turns
        String base = datasetName.replace("_dataset", "").replace("_bank", "");
        Path p = dir.resolve(base + "_turns.csv");
        if (Files.exists(p)) return p;
        return null;
    }

    /**
     * Read CSV into a list of column-name -> value maps.
     */
    private List<Map<String, String>> readCsv(Path csvFile) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(csvFile.toFile())).build()) {
            String[] headers = reader.readNext();
            if (headers == null) return rows;

            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.length && i < line.length; i++) {
                    row.put(headers[i].trim(), line[i] != null ? line[i].trim() : "");
                }
                rows.add(row);
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse CSV: " + csvFile, e);
        }
        return rows;
    }

    /**
     * Load HR annotations keyed by session_id.
     */
    private Map<String, Map<String, String>> loadAnnotations() {
        Path annotationPath = Path.of(config.getAnnotationFile());
        if (!Files.exists(annotationPath)) {
            log.warn("Annotation file not found: {}", annotationPath);
            return Map.of();
        }
        try {
            List<Map<String, String>> rows = readCsv(annotationPath);
            return rows.stream()
                    .filter(r -> r.containsKey("session_id") && !r.get("session_id").isBlank())
                    .collect(Collectors.toMap(
                            r -> r.get("session_id"),
                            r -> r,
                            (a, b) -> b // last one wins on duplicate
                    ));
        } catch (IOException e) {
            log.error("Failed to load annotations", e);
            return Map.of();
        }
    }

    private void overlayAnnotation(EvalSession session, Map<String, String> ann) {
        session.setHrAnnotated(true);
        session.setHrPrimaryUcCorrected(ann.getOrDefault("primary_uc_corrected", ""));
        session.setHrOutcomeClass(ann.getOrDefault("outcome_class", ""));
        session.setHrOutcomeReasoning(ann.getOrDefault("outcome_reasoning", ""));
        session.setHrShouldEscalate(parseBoolSafe(ann.get("should_escalate")));
        session.setHrEscalationTrigger(ann.getOrDefault("escalation_trigger", ""));
        session.setHrEscalationTurn(ann.getOrDefault("escalation_turn", ""));
        session.setHrExpectedToolSequence(ann.getOrDefault("expected_tool_sequence", ""));
        session.setHrForbiddenTools(ann.getOrDefault("forbidden_tools", ""));
        session.setHrAnswerMustNotContain(ann.getOrDefault("answer_must_not_contain", ""));
        session.setHrRiskLevel(ann.getOrDefault("risk_level", ""));
        session.setHrDriftType(ann.getOrDefault("drift_type", ""));
        session.setHrDriftHandling(ann.getOrDefault("drift_handling", ""));
        session.setHrHasFrustration(parseBoolSafe(ann.get("has_frustration")));
        session.setHrFrustrationType(ann.getOrDefault("frustration_type", ""));
        session.setHrGroundingSource(ann.getOrDefault("grounding_source", ""));
        session.setHrExpectedKnowledgeScope(ann.getOrDefault("expected_knowledge_scope", ""));
        session.setHrTranscriptSummary(ann.getOrDefault("transcript_summary", ""));
    }

    private static int parseIntSafe(String val) {
        if (val == null || val.isBlank()) return 0;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean parseBoolSafe(String val) {
        if (val == null || val.isBlank()) return false;
        return "true".equalsIgnoreCase(val.trim()) || "True".equals(val.trim());
    }
}
