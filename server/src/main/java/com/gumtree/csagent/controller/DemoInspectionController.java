package com.gumtree.csagent.controller;

import com.gumtree.csagent.config.MockProperties;
import com.gumtree.csagent.model.BotEvent;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.MockCase;
import com.gumtree.csagent.model.MockHandoverLog;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.service.mock.MockGumtreeApiService;
import com.gumtree.csagent.service.mock.MockSalesforceService;
import com.gumtree.csagent.service.observability.LocalEventStore;
import com.gumtree.csagent.service.observability.model.FunnelMetrics;
import com.gumtree.csagent.service.observability.model.UcMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/demo")
@Profile("local")
public class DemoInspectionController {

    private final MockSalesforceService salesforceService;
    private final MockGumtreeApiService gumtreeApiService;
    private final BotSessionRepository sessionRepository;
    private final BotEventRepository eventRepository;
    private final MockProperties mockProperties;
    private final LocalEventStore localEventStore;

    public DemoInspectionController(MockSalesforceService salesforceService,
                                    MockGumtreeApiService gumtreeApiService,
                                    BotSessionRepository sessionRepository,
                                    BotEventRepository eventRepository,
                                    MockProperties mockProperties,
                                    LocalEventStore localEventStore) {
        this.salesforceService = salesforceService;
        this.gumtreeApiService = gumtreeApiService;
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.mockProperties = mockProperties;
        this.localEventStore = localEventStore;
    }

    @GetMapping("/cases")
    public ResponseEntity<List<MockCase>> listCases() {
        return ResponseEntity.ok(salesforceService.findAllCases());
    }

    @GetMapping("/handover-logs")
    public ResponseEntity<List<MockHandoverLog>> listHandoverLogs() {
        return ResponseEntity.ok(salesforceService.findAllHandoverLogs());
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<BotSession>> listSessions() {
        return ResponseEntity.ok(sessionRepository.findAll());
    }

    @GetMapping("/sessions/{id}/events")
    public ResponseEntity<List<BotEvent>> listSessionEvents(@PathVariable("id") String sessionId) {
        return ResponseEntity.ok(eventRepository.findBySessionIdOrderByCreatedAt(sessionId));
    }

    @GetMapping("/mock-data/{type}")
    public ResponseEntity<Map<String, Map<String, Object>>> getMockData(@PathVariable("type") String type) {
        Map<String, Map<String, Object>> fixtures = gumtreeApiService.getFixturesByType(type);
        if (fixtures.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(fixtures);
    }

    @PostMapping("/config")
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> configUpdate) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (configUpdate.containsKey("business_hours")) {
            boolean businessHours = Boolean.parseBoolean(configUpdate.get("business_hours").toString());
            mockProperties.setBusinessHours(businessHours);
            result.put("business_hours", businessHours);
            log.info("Mock config updated: business_hours={}", businessHours);
        }

        result.put("status", "updated");
        result.put("current_config", Map.of("business_hours", mockProperties.isBusinessHours()));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sessions/{id}/trace")
    public ResponseEntity<List<BotTurn>> getSessionTrace(@PathVariable("id") String sessionId) {
        return ResponseEntity.ok(localEventStore.getSessionTrace(sessionId));
    }

    @GetMapping("/metrics/funnel")
    public ResponseEntity<FunnelMetrics> getFunnelMetrics() {
        return ResponseEntity.ok(localEventStore.getFunnelMetrics());
    }

    @GetMapping("/metrics/per-uc")
    public ResponseEntity<Map<String, UcMetrics>> getPerUcMetrics() {
        return ResponseEntity.ok(localEventStore.getPerUcMetrics());
    }
}
