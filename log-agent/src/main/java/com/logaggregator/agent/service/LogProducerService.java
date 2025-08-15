/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 */

package com.logaggregator.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaggregator.agent.config.LogAgentConfig;
import com.logaggregator.common.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for sending log entries to Kafka.
 * Handles batching, serialization, and error handling.
 */
@Service
public class LogProducerService {

    private static final Logger logger = LoggerFactory.getLogger(LogProducerService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final LogAgentConfig config;
    private final ObjectMapper objectMapper;

    @Autowired
    public LogProducerService(KafkaTemplate<String, String> kafkaTemplate,
                             LogAgentConfig config) {
        this.kafkaTemplate = kafkaTemplate;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send a single log entry to Kafka
     */
    public CompletableFuture<Void> sendLogEntry(LogEntry logEntry) {
        try {
            String jsonLog = objectMapper.writeValueAsString(logEntry);
            String key = generateKey(logEntry);

            // Send to Kafka and handle the result
            kafkaTemplate.send(config.getKafkaTopicName(), key, jsonLog)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Failed to send log entry: {}", throwable.getMessage());
                    } else {
                        logger.debug("Log entry sent successfully: partition={}, offset={}",
                                   result.getRecordMetadata().partition(),
                                   result.getRecordMetadata().offset());
                    }
                });

            return CompletableFuture.completedFuture(null);

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize log entry to JSON: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Send multiple log entries as a batch
     */
    public CompletableFuture<Void> sendLogEntries(List<LogEntry> logEntries) {
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            logEntries.stream()
                .map(this::sendLogEntry)
                .toArray(CompletableFuture[]::new)
        );

        return allFutures.whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error("Failed to send batch of {} log entries", logEntries.size());
            } else {
                logger.info("Successfully sent batch of {} log entries", logEntries.size());
            }
        });
    }

    /**
     * Generate a partition key for the log entry
     */
    private String generateKey(LogEntry logEntry) {
        // Use source as key for better distribution
        return logEntry.getSource() != null ? logEntry.getSource() : "unknown";
    }

    /**
     * Get producer statistics
     */
    public void logProducerStats() {
        // In a real implementation, you might want to track metrics
        logger.info("Kafka producer is active for topic: {}", config.getKafkaTopicName());
    }
}
