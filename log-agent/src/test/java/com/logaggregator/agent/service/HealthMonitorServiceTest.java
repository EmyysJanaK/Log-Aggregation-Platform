/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 */

package com.logaggregator.agent.service;

import com.logaggregator.agent.config.LogAgentConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthMonitorService
 */
@ExtendWith(MockitoExtension.class)
class HealthMonitorServiceTest {

    @Mock
    private LogAgentConfig config;

    @Mock
    private LogProducerService logProducerService;

    private HealthMonitorService healthMonitorService;

    @BeforeEach
    void setUp() {
        when(config.getAgentId()).thenReturn("test-agent");
        when(config.getHostname()).thenReturn("test-host");

        healthMonitorService = new HealthMonitorService(config, logProducerService);
    }

    @Test
    void testStartMonitoring() {
        // Act
        healthMonitorService.startMonitoring();

        // Assert
        Map<String, Object> healthStatus = healthMonitorService.getHealthStatus();
        assertTrue((Boolean) healthStatus.get("is_running"));
        assertEquals("HEALTHY", healthStatus.get("overall_status"));

        // Cleanup
        healthMonitorService.stopMonitoring();
    }

    @Test
    void testStopMonitoring() {
        // Arrange
        healthMonitorService.startMonitoring();

        // Act
        healthMonitorService.stopMonitoring();

        // Assert
        Map<String, Object> healthStatus = healthMonitorService.getHealthStatus();
        assertFalse((Boolean) healthStatus.get("is_running"));
        assertEquals("STOPPED", healthStatus.get("overall_status"));
    }

    @Test
    void testGetHealthStatus() {
        // Act
        Map<String, Object> healthStatus = healthMonitorService.getHealthStatus();

        // Assert
        assertNotNull(healthStatus);
        assertTrue(healthStatus.containsKey("is_running"));
        assertTrue(healthStatus.containsKey("health_checks_count"));
        assertTrue(healthStatus.containsKey("uptime_ms"));
        assertTrue(healthStatus.containsKey("cpu_usage_percent"));
        assertTrue(healthStatus.containsKey("memory_usage_percent"));
        assertTrue(healthStatus.containsKey("overall_status"));
    }

    @Test
    void testGetPerformanceMetrics() {
        // Act
        Map<String, Object> metrics = healthMonitorService.getPerformanceMetrics();

        // Assert
        assertNotNull(metrics);
        assertTrue(metrics.containsKey("jvm_total_memory"));
        assertTrue(metrics.containsKey("jvm_free_memory"));
        assertTrue(metrics.containsKey("jvm_used_memory"));
        assertTrue(metrics.containsKey("jvm_max_memory"));
        assertTrue(metrics.containsKey("available_processors"));
        assertTrue(metrics.containsKey("agent_uptime_ms"));
        assertTrue(metrics.containsKey("health_checks_performed"));

        // Verify numeric values
        assertTrue((Long) metrics.get("jvm_total_memory") > 0);
        assertTrue((Integer) metrics.get("available_processors") > 0);
    }

    @Test
    void testHealthStatusWhenNotRunning() {
        // Act
        Map<String, Object> healthStatus = healthMonitorService.getHealthStatus();

        // Assert
        assertFalse((Boolean) healthStatus.get("is_running"));
        assertEquals("STOPPED", healthStatus.get("overall_status"));
    }
}
