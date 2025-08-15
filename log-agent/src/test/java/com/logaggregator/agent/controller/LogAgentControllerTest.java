/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 */

package com.logaggregator.agent.controller;

import com.logaggregator.agent.service.LogAgentService;
import com.logaggregator.agent.service.HealthMonitorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for LogAgentController
 */
@WebMvcTest(LogAgentController.class)
class LogAgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogAgentService logAgentService;

    @MockBean
    private HealthMonitorService healthMonitorService;

    @Test
    void testGetStatus() throws Exception {
        // Arrange
        Map<String, Object> mockStatus = Map.of(
            "agent_id", "test-agent",
            "hostname", "test-host",
            "status", "running"
        );
        when(logAgentService.getAgentStatus()).thenReturn(mockStatus);

        // Act & Assert
        mockMvc.perform(get("/api/agent/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.agent_id").value("test-agent"))
                .andExpect(jsonPath("$.hostname").value("test-host"))
                .andExpect(jsonPath("$.status").value("running"));
    }

    @Test
    void testGetHealth_Healthy() throws Exception {
        // Arrange
        Map<String, Object> mockHealth = Map.of(
            "overall_status", "HEALTHY",
            "cpu_usage_percent", 25.0,
            "memory_usage_percent", 60.0
        );
        when(healthMonitorService.getHealthStatus()).thenReturn(mockHealth);

        // Act & Assert
        mockMvc.perform(get("/api/agent/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.overall_status").value("HEALTHY"));
    }

    @Test
    void testGetHealth_Critical() throws Exception {
        // Arrange
        Map<String, Object> mockHealth = Map.of(
            "overall_status", "CRITICAL",
            "cpu_usage_percent", 95.0,
            "memory_usage_percent", 98.0
        );
        when(healthMonitorService.getHealthStatus()).thenReturn(mockHealth);

        // Act & Assert
        mockMvc.perform(get("/api/agent/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.overall_status").value("CRITICAL"));
    }

    @Test
    void testGetMetrics() throws Exception {
        // Arrange
        Map<String, Object> mockMetrics = Map.of(
            "jvm_total_memory", 1024 * 1024 * 512L,
            "jvm_free_memory", 1024 * 1024 * 256L,
            "available_processors", 4,
            "agent_uptime_ms", 300000L
        );
        when(healthMonitorService.getPerformanceMetrics()).thenReturn(mockMetrics);

        // Act & Assert
        mockMvc.perform(get("/api/agent/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.available_processors").value(4))
                .andExpect(jsonPath("$.agent_uptime_ms").value(300000));
    }
}
