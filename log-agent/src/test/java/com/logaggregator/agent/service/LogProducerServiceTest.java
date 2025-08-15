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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LogProducerService
 */
@ExtendWith(MockitoExtension.class)
class LogProducerServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private LogAgentConfig config;

    @Mock
    private ListenableFuture<SendResult<String, String>> kafkaFuture;

    private LogProducerService logProducerService;

    @BeforeEach
    void setUp() {
        when(config.getKafkaTopicName()).thenReturn("test-topic");
        logProducerService = new LogProducerService(kafkaTemplate, config);
    }

    @Test
    void testSendLogEntry_Success() throws Exception {
        // Arrange
        LogEntry logEntry = new LogEntry("test-source", LogLevel.INFO, "test message");
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(kafkaFuture);

        // Act
        CompletableFuture<Void> result = logProducerService.sendLogEntry(logEntry);

        // Simulate successful callback
        verify(kafkaFuture).addCallback(any());

        // Verify
        verify(kafkaTemplate).send(eq("test-topic"), eq("test-source"), anyString());
        assertNotNull(result);
    }

    @Test
    void testSendLogEntries_Batch() throws Exception {
        // Arrange
        List<LogEntry> logEntries = Arrays.asList(
            new LogEntry("source1", LogLevel.INFO, "message1"),
            new LogEntry("source2", LogLevel.ERROR, "message2"),
            new LogEntry("source3", LogLevel.WARN, "message3")
        );

        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(kafkaFuture);

        // Act
        CompletableFuture<Void> result = logProducerService.sendLogEntries(logEntries);

        // Verify
        verify(kafkaTemplate, times(3)).send(anyString(), anyString(), anyString());
        assertNotNull(result);
    }

    @Test
    void testSendLogEntry_WithNullSource() throws Exception {
        // Arrange
        LogEntry logEntry = new LogEntry(null, LogLevel.INFO, "test message");
        logEntry.setSource(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(kafkaFuture);

        // Act
        CompletableFuture<Void> result = logProducerService.sendLogEntry(logEntry);

        // Verify - should use "unknown" as key when source is null
        verify(kafkaTemplate).send(eq("test-topic"), eq("unknown"), anyString());
        assertNotNull(result);
    }
}
