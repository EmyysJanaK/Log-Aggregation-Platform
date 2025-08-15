/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 */

package com.logaggregator.agent.service;

import com.logaggregator.agent.config.LogAgentConfig;
import com.logaggregator.common.LogEntry;
import com.logaggregator.common.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for monitoring the health and performance of the Log Agent.
 * Tracks system metrics, agent performance, and generates health reports.
 */
@Service
public class HealthMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(HealthMonitorService.class);

    private final LogAgentConfig config;
    private final LogProducerService logProducerService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicLong healthChecksCount = new AtomicLong(0);
    private boolean isRunning = false;

    // Health metrics
    private double lastCpuUsage = 0.0;
    private long lastMemoryUsed = 0;
    private long lastMemoryMax = 0;
    private long startTime = System.currentTimeMillis();

    @Autowired
    public HealthMonitorService(LogAgentConfig config, LogProducerService logProducerService) {
        this.config = config;
        this.logProducerService = logProducerService;
    }

    /**
     * Start health monitoring
     */
    public void startMonitoring() {
        if (isRunning) {
            logger.warn("Health monitoring is already running");
            return;
        }

        isRunning = true;
        startTime = System.currentTimeMillis();

        // Schedule health checks every minute
        scheduler.scheduleAtFixedRate(
            this::performHealthCheck,
            1,
            60,
            TimeUnit.SECONDS
        );

        logger.info("Health monitoring started");
    }

    /**
     * Stop health monitoring
     */
    public void stopMonitoring() {
        isRunning = false;

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("Health monitoring stopped");
    }

    /**
     * Perform comprehensive health check
     */
    private void performHealthCheck() {
        try {
            healthChecksCount.incrementAndGet();

            // Collect system metrics
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            // CPU usage
            lastCpuUsage = osBean.getProcessCpuLoad() * 100;

            // Memory usage
            lastMemoryUsed = memoryBean.getHeapMemoryUsage().getUsed();
            lastMemoryMax = memoryBean.getHeapMemoryUsage().getMax();
            double memoryUsagePercent = (double) lastMemoryUsed / lastMemoryMax * 100;

            // Check for alerts
            checkForAlerts(lastCpuUsage, memoryUsagePercent);

            // Generate health log entry
            generateHealthLogEntry(lastCpuUsage, memoryUsagePercent);

            logger.debug("Health check completed - CPU: {:.2f}%, Memory: {:.2f}%",
                        lastCpuUsage, memoryUsagePercent);

        } catch (Exception e) {
            logger.error("Error during health check: {}", e.getMessage());
        }
    }

    /**
     * Check for alert conditions
     */
    private void checkForAlerts(double cpuUsage, double memoryUsage) {
        // High CPU usage alert
        if (cpuUsage > 80.0) {
            generateAlert("HIGH_CPU_USAGE",
                         String.format("CPU usage is high: %.2f%%", cpuUsage),
                         LogLevel.WARN);
        }

        // High memory usage alert
        if (memoryUsage > 85.0) {
            generateAlert("HIGH_MEMORY_USAGE",
                         String.format("Memory usage is high: %.2f%%", memoryUsage),
                         LogLevel.WARN);
        }

        // Critical memory usage alert
        if (memoryUsage > 95.0) {
            generateAlert("CRITICAL_MEMORY_USAGE",
                         String.format("Memory usage is critical: %.2f%%", memoryUsage),
                         LogLevel.ERROR);
        }
    }

    /**
     * Generate an alert log entry
     */
    private void generateAlert(String alertType, String message, LogLevel level) {
        String sourceId = config.getAgentId() + ":health-monitor";

        LogEntry alertEntry = new LogEntry(sourceId, level, message);
        alertEntry.setHostname(config.getHostname());
        alertEntry.setApplication("log-agent-health");
        alertEntry.addMetadata("agent_id", config.getAgentId());
        alertEntry.addMetadata("alert_type", alertType);
        alertEntry.addTag("log_type", "health");
        alertEntry.addTag("alert", "true");

        logProducerService.sendLogEntry(alertEntry);
        logger.warn("Health alert generated: {} - {}", alertType, message);
    }

    /**
     * Generate periodic health log entry
     */
    private void generateHealthLogEntry(double cpuUsage, double memoryUsage) {
        // Only send health metrics every 5 minutes to avoid spam
        if (healthChecksCount.get() % 5 == 0) {
            String sourceId = config.getAgentId() + ":health-monitor";
            String message = String.format("Agent health metrics - CPU: %.2f%%, Memory: %.2f%%",
                                          cpuUsage, memoryUsage);

            LogEntry healthEntry = new LogEntry(sourceId, LogLevel.INFO, message);
            healthEntry.setHostname(config.getHostname());
            healthEntry.setApplication("log-agent-health");
            healthEntry.addMetadata("agent_id", config.getAgentId());
            healthEntry.addMetadata("cpu_usage_percent", cpuUsage);
            healthEntry.addMetadata("memory_usage_percent", memoryUsage);
            healthEntry.addMetadata("uptime_ms", System.currentTimeMillis() - startTime);
            healthEntry.addTag("log_type", "metrics");

            logProducerService.sendLogEntry(healthEntry);
        }
    }

    /**
     * Get current health status
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();

        health.put("is_running", isRunning);
        health.put("health_checks_count", healthChecksCount.get());
        health.put("uptime_ms", System.currentTimeMillis() - startTime);
        health.put("cpu_usage_percent", lastCpuUsage);
        health.put("memory_used_bytes", lastMemoryUsed);
        health.put("memory_max_bytes", lastMemoryMax);
        health.put("memory_usage_percent", lastMemoryMax > 0 ? (double) lastMemoryUsed / lastMemoryMax * 100 : 0);

        // Overall health status
        String status = determineOverallHealth();
        health.put("overall_status", status);

        return health;
    }

    /**
     * Determine overall health status
     */
    private String determineOverallHealth() {
        double memoryUsagePercent = lastMemoryMax > 0 ? (double) lastMemoryUsed / lastMemoryMax * 100 : 0;

        if (lastCpuUsage > 90 || memoryUsagePercent > 95) {
            return "CRITICAL";
        } else if (lastCpuUsage > 80 || memoryUsagePercent > 85) {
            return "WARNING";
        } else if (isRunning) {
            return "HEALTHY";
        } else {
            return "STOPPED";
        }
    }

    /**
     * Get detailed performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Runtime runtime = Runtime.getRuntime();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        Map<String, Object> metrics = new HashMap<>();

        // JVM metrics
        metrics.put("jvm_total_memory", runtime.totalMemory());
        metrics.put("jvm_free_memory", runtime.freeMemory());
        metrics.put("jvm_used_memory", runtime.totalMemory() - runtime.freeMemory());
        metrics.put("jvm_max_memory", runtime.maxMemory());

        // System metrics
        metrics.put("available_processors", osBean.getAvailableProcessors());
        metrics.put("system_load_average", osBean.getSystemLoadAverage());
        metrics.put("process_cpu_load", osBean.getProcessCpuLoad());

        // Agent metrics
        metrics.put("agent_uptime_ms", System.currentTimeMillis() - startTime);
        metrics.put("health_checks_performed", healthChecksCount.get());

        return metrics;
    }
}
