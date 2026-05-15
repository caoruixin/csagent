package com.gumtree.csagent.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    public HealthController(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("db", checkDatabase());
        components.put("redis", checkRedis());

        boolean allUp = components.values().stream()
                .allMatch(c -> "UP".equals(((Map<?, ?>) c).get("status")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", allUp ? "UP" : "DOWN");
        result.put("components", components);

        return allUp ? ResponseEntity.ok(result) : ResponseEntity.status(503).body(result);
    }

    @GetMapping("/liveness")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("db", checkDatabase());
        components.put("redis", checkRedis());

        boolean allUp = components.values().stream()
                .allMatch(c -> "UP".equals(((Map<?, ?>) c).get("status")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", allUp ? "UP" : "DOWN");
        result.put("components", components);

        return allUp ? ResponseEntity.ok(result) : ResponseEntity.status(503).body(result);
    }

    private Map<String, String> checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            conn.isValid(2);
            return Map.of("status", "UP");
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    private Map<String, String> checkRedis() {
        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            conn.ping();
            return Map.of("status", "UP");
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }
}
