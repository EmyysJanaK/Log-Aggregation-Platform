/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 */

package com.logaggregator.agent.service;

import com.logaggregator.agent.config.LogAgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.net.InetAddress;
import java.util.Map;

/**
 * Main orchestration service for the Log Agent.
 * Coordinates file monitoring, system log collection, and health monitoring.
 */
@Service
public class LogAgentService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(LogAgentService.class);

    private final LogAgentConfig config;
    private final FileMonitorService fileMonitorService;
    private final SystemLogService systemLogService;
    private final HealthMonitorService healthMonitorService;
    private final LogProducerService logProducerService;

    @Autowired
    public LogAgentService(LogAgentConfig config,
                          FileMonitorService fileMonitorService,
                          SystemLogService systemLogService,
                          HealthMonitorService healthMonitorService,
                          LogProducerService logProducerService) {
        this.config = config;
        this.fileMonitorService = fileMonitorService;
        this.systemLogService = systemLogService;
        this.healthMonitorService = healthMonitorService;
        this.logProducerService = logProducerService;
    }

    @Override
    public void run(String... args) {
        logger.info("Starting Log Agent with ID: {}", config.getAgentId());

        // Set hostname if not configured
        if ("localhost".equals(config.getHostname())) {
            try {
                String detectedHostname = InetAddress.getLocalHost().getHostName();
                config.setHostname(detectedHostname);
                logger.info("Detected hostname: {}", detectedHostname);
            } catch (Exception e) {
                logger.warn("Could not detect hostname, using: {}", config.getHostname());
            }
        }

        // Start services
        startServices();

        // Log startup information
        logStartupInfo();

        logger.info("Log Agent started successfully");
    }

    private void startServices() {
        // Start health monitoring
        healthMonitorService.startMonitoring();

        // Start file monitoring if enabled
        if (config.isEnableFileWatcher() && !config.getWatchDirectories().isEmpty()) {
            fileMonitorService.startMonitoring();
        } else {
            logger.info("File monitoring disabled or no directories configured");
        }

        // Start system log collection if enabled
        if (config.isEnableSystemLogs()) {
            systemLogService.startCollection();
        } else {
            logger.info("System log collection disabled");
        }

        // Log producer stats
        logProducerService.logProducerStats();
    }

    private void logStartupInfo() {
        logger.info("=== Log Agent Configuration ===");
        logger.info("Agent ID: {}", config.getAgentId());
        logger.info("Hostname: {}", config.getHostname());
        logger.info("Watch Directories: {}", config.getWatchDirectories());
        logger.info("File Patterns: {}", config.getFilePatterns());
        logger.info("Scan Interval: {} seconds", config.getScanIntervalSeconds());
        logger.info("Batch Size: {}", config.getBatchSize());
        logger.info("Kafka Topic: {}", config.getKafkaTopicName());
        logger.info("File Watcher Enabled: {}", config.isEnableFileWatcher());
        logger.info("System Logs Enabled: {}", config.isEnableSystemLogs());
        logger.info("================================");
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Log Agent...");

        try {
            fileMonitorService.stopMonitoring();
            systemLogService.stopCollection();
            healthMonitorService.stopMonitoring();

            logger.info("Log Agent shutdown completed");
        } catch (Exception e) {
            logger.error("Error during shutdown: {}", e.getMessage());
        }
    }

    /**
     * Get comprehensive agent status
     */
    public Map<String, Object> getAgentStatus() {
        return Map.of(
            "agent_id", config.getAgentId(),
            "hostname", config.getHostname(),
            "file_monitoring", fileMonitorService.getMonitoringStats(),
            "system_logs", systemLogService.getCollectionStats(),
            "health", healthMonitorService.getHealthStatus()
        );
    }
}
