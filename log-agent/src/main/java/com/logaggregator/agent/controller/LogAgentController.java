/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 */

package com.logaggregator.agent.controller;

import com.logaggregator.agent.service.LogAgentService;
import com.logaggregator.agent.service.HealthMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for monitoring and managing the Log Agent.
 * Provides endpoints for health checks, status monitoring, and metrics.
 */
@RestController
@RequestMapping("/api/agent")
public class LogAgentController {

    private final LogAgentService logAgentService;
    private final HealthMonitorService healthMonitorService;

    @Autowired
    public LogAgentController(LogAgentService logAgentService,
                             HealthMonitorService healthMonitorService) {
        this.logAgentService = logAgentService;
        this.healthMonitorService = healthMonitorService;
    }

    /**
     * Get comprehensive agent status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = logAgentService.getAgentStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Get health status for load balancer health checks
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = healthMonitorService.getHealthStatus();
        String overallStatus = (String) health.get("overall_status");

        if ("CRITICAL".equals(overallStatus)) {
            return ResponseEntity.status(503).body(health);
        } else if ("WARNING".equals(overallStatus)) {
            return ResponseEntity.status(200).body(health);
        } else {
            return ResponseEntity.ok(health);
        }
    }

    /**
     * Get detailed performance metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = healthMonitorService.getPerformanceMetrics();
        return ResponseEntity.ok(metrics);
    }
}
