package com.gumtree.csagent.service.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.MockCase;
import com.gumtree.csagent.repository.MockCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Component
@Profile("local")
public class MockDataInitializer implements CommandLineRunner {

    private final MockCaseRepository caseRepository;
    private final ObjectMapper objectMapper;

    public MockDataInitializer(MockCaseRepository caseRepository, ObjectMapper objectMapper) {
        this.caseRepository = caseRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        try {
            if (caseRepository.count() > 0) {
                log.info("Mock cases table already has data, skipping seed");
                return;
            }

            log.info("Seeding mock_cases from fixture files...");
            int seeded = 0;

            try {
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource[] resources = resolver.getResources("classpath:mock/cases/*.json");

                for (Resource resource : resources) {
                    try (InputStream is = resource.getInputStream()) {
                        Map<String, Object> data = objectMapper.readValue(is, new TypeReference<>() {});
                        MockCase mockCase = mapToMockCase(data);
                        caseRepository.save(mockCase);
                        seeded++;
                        log.info("Seeded mock case: caseId={}, useCaseId={}", mockCase.getCaseId(), mockCase.getUseCaseId());
                    } catch (IOException e) {
                        log.warn("Failed to load case fixture {}: {}", resource.getFilename(), e.getMessage());
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to scan mock/cases directory: {}", e.getMessage());
            }

            log.info("Mock data initialization complete: {} cases seeded", seeded);
        } catch (Exception e) {
            log.warn("Mock data initialization skipped due to error (non-fatal): {}", e.getMessage());
        }
    }

    private MockCase mapToMockCase(Map<String, Object> data) {
        OffsetDateTime now = OffsetDateTime.now();
        String intakeFieldsJson = null;
        Object intakeFields = data.get("intake_fields");
        if (intakeFields != null) {
            try {
                intakeFieldsJson = objectMapper.writeValueAsString(intakeFields);
            } catch (IOException e) {
                log.warn("Failed to serialize intake_fields: {}", e.getMessage());
            }
        }

        return MockCase.builder()
                .caseId((String) data.get("case_id"))
                .sessionId((String) data.get("session_id"))
                .useCaseId((String) data.get("use_case_id"))
                .subject((String) data.get("subject"))
                .description((String) data.get("description"))
                .contactEmail((String) data.get("contact_email"))
                .adId((String) data.get("ad_id"))
                .status((String) data.getOrDefault("status", "New"))
                .queueName((String) data.get("queue_name"))
                .intakeFields(intakeFieldsJson)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
