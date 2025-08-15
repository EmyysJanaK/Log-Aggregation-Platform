/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 */

package com.logaggregator.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.ArrayList;

/**
 * Configuration properties for the Log Agent.
 * Defines settings for log collection, monitoring, and Kafka publishing.
 */
@Configuration
@ConfigurationProperties(prefix = "log.agent")
public class LogAgentConfig {

    private String agentId = "default-agent";
    private String hostname = "localhost";
    private List<String> watchDirectories = new ArrayList<>();
    private List<String> filePatterns = List.of("*.log", "*.txt");
    private int scanIntervalSeconds = 30;
    private int batchSize = 100;
    private boolean enableFileWatcher = true;
    private boolean enableSystemLogs = true;
    private String kafkaTopicName = "raw-logs";
    private long maxFileSizeBytes = 100 * 1024 * 1024; // 100MB
    private int maxRetries = 3;
    private boolean compressLogs = true;

    // Getters and Setters
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public List<String> getWatchDirectories() { return watchDirectories; }
    public void setWatchDirectories(List<String> watchDirectories) { this.watchDirectories = watchDirectories; }

    public List<String> getFilePatterns() { return filePatterns; }
    public void setFilePatterns(List<String> filePatterns) { this.filePatterns = filePatterns; }

    public int getScanIntervalSeconds() { return scanIntervalSeconds; }
    public void setScanIntervalSeconds(int scanIntervalSeconds) { this.scanIntervalSeconds = scanIntervalSeconds; }

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public boolean isEnableFileWatcher() { return enableFileWatcher; }
    public void setEnableFileWatcher(boolean enableFileWatcher) { this.enableFileWatcher = enableFileWatcher; }

    public boolean isEnableSystemLogs() { return enableSystemLogs; }
    public void setEnableSystemLogs(boolean enableSystemLogs) { this.enableSystemLogs = enableSystemLogs; }

    public String getKafkaTopicName() { return kafkaTopicName; }
    public void setKafkaTopicName(String kafkaTopicName) { this.kafkaTopicName = kafkaTopicName; }

    public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
    public void setMaxFileSizeBytes(long maxFileSizeBytes) { this.maxFileSizeBytes = maxFileSizeBytes; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public boolean isCompressLogs() { return compressLogs; }
    public void setCompressLogs(boolean compressLogs) { this.compressLogs = compressLogs; }
}
