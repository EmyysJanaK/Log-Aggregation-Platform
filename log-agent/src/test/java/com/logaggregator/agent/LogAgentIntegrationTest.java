/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 */

package com.logaggregator.agent;

import com.logaggregator.agent.config.LogAgentConfig;
import com.logaggregator.agent.service.HealthMonitorService;
import com.logaggregator.agent.service.LogAgentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Log Agent application
 */
@SpringBootTest(classes = LogAgentApplication.class)
@TestPropertySource(properties = {
    "log.agent.agent-id=test-agent-integration",
    "log.agent.hostname=test-host",
    "log.agent.scan-interval-seconds=1",
    "log.agent.enable-file-watcher=false",
    "log.agent.enable-system-logs=false",
    "spring.kafka.producer.bootstrap-servers=localhost:9092"
})
class LogAgentIntegrationTest {

    @Autowired
    private LogAgentConfig config;

    @Autowired
    private LogAgentService logAgentService;

    @Autowired
    private HealthMonitorService healthMonitorService;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @TempDir
    Path tempDir;

    @Test
    void testApplicationStartup() {
        // Assert that the application context loads successfully
        assertNotNull(config);
        assertNotNull(logAgentService);
        assertNotNull(healthMonitorService);

        // Verify configuration
        assertEquals("test-agent-integration", config.getAgentId());
        assertEquals("test-host", config.getHostname());
        assertEquals(1, config.getScanIntervalSeconds());
        assertFalse(config.isEnableFileWatcher());
        assertFalse(config.isEnableSystemLogs());
    }

    @Test
    void testHealthMonitoring() throws InterruptedException {
        // Start health monitoring
        healthMonitorService.startMonitoring();

        // Wait a moment for health check to run
        Thread.sleep(100);

        // Get health status
        Map<String, Object> healthStatus = healthMonitorService.getHealthStatus();

        // Verify health status
        assertNotNull(healthStatus);
        assertTrue((Boolean) healthStatus.get("is_running"));
        assertNotNull(healthStatus.get("overall_status"));
        assertTrue(healthStatus.containsKey("cpu_usage_percent"));
        assertTrue(healthStatus.containsKey("memory_usage_percent"));

        // Stop monitoring
        healthMonitorService.stopMonitoring();

        // Verify stopped status
        healthStatus = healthMonitorService.getHealthStatus();
        assertFalse((Boolean) healthStatus.get("is_running"));
        assertEquals("STOPPED", healthStatus.get("overall_status"));
    }

    @Test
    void testAgentStatus() {
        // Get agent status
        Map<String, Object> status = logAgentService.getAgentStatus();

        // Verify status structure
        assertNotNull(status);
        assertEquals("test-agent-integration", status.get("agent_id"));
        assertEquals("test-host", status.get("hostname"));
        assertTrue(status.containsKey("file_monitoring"));
        assertTrue(status.containsKey("system_logs"));
        assertTrue(status.containsKey("health"));

        // Verify nested objects
        Map<String, Object> fileMonitoring = (Map<String, Object>) status.get("file_monitoring");
        assertNotNull(fileMonitoring);
        assertTrue(fileMonitoring.containsKey("tracked_files_count"));

        Map<String, Object> health = (Map<String, Object>) status.get("health");
        assertNotNull(health);
        assertTrue(health.containsKey("overall_status"));
    }

    @Test
    void testPerformanceMetrics() {
        // Get performance metrics
        Map<String, Object> metrics = healthMonitorService.getPerformanceMetrics();

        // Verify metrics structure
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("jvm_total_memory"));
        assertTrue(metrics.containsKey("jvm_free_memory"));
        assertTrue(metrics.containsKey("jvm_used_memory"));
        assertTrue(metrics.containsKey("jvm_max_memory"));
        assertTrue(metrics.containsKey("available_processors"));
        assertTrue(metrics.containsKey("agent_uptime_ms"));

        // Verify values are reasonable
        assertTrue((Long) metrics.get("jvm_total_memory") > 0);
        assertTrue((Integer) metrics.get("available_processors") > 0);
        assertTrue((Long) metrics.get("agent_uptime_ms") >= 0);
    }
}
