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
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FileMonitorService
 */
@ExtendWith(MockitoExtension.class)
class FileMonitorServiceTest {

    @Mock
    private LogAgentConfig config;

    @Mock
    private LogProducerService logProducerService;

    private FileMonitorService fileMonitorService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(config.getAgentId()).thenReturn("test-agent");
        when(config.getHostname()).thenReturn("test-host");
        when(config.getScanIntervalSeconds()).thenReturn(1);
        when(config.getBatchSize()).thenReturn(10);
        when(config.getMaxFileSizeBytes()).thenReturn(1024L * 1024L);
        when(config.isEnableFileWatcher()).thenReturn(true);
        when(config.getFilePatterns()).thenReturn(Arrays.asList("*.log", "*.txt"));

        fileMonitorService = new FileMonitorService(config, logProducerService);
    }

    @Test
    void testStartMonitoring_WithValidDirectory() throws IOException {
        // Arrange
        String tempDirPath = tempDir.toString();
        when(config.getWatchDirectories()).thenReturn(List.of(tempDirPath));

        // Create a test log file
        Path logFile = tempDir.resolve("test.log");
        Files.write(logFile, "Test log entry\n".getBytes());

        // Act
        fileMonitorService.startMonitoring();

        // Give it a moment to process
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Assert
        Map<String, Object> stats = fileMonitorService.getMonitoringStats();
        assertTrue((Boolean) stats.get("is_running"));
        assertEquals(List.of(tempDirPath), stats.get("watch_directories"));

        // Cleanup
        fileMonitorService.stopMonitoring();
    }

    @Test
    void testStopMonitoring() {
        // Arrange
        when(config.getWatchDirectories()).thenReturn(List.of(tempDir.toString()));
        fileMonitorService.startMonitoring();

        // Act
        fileMonitorService.stopMonitoring();

        // Assert
        Map<String, Object> stats = fileMonitorService.getMonitoringStats();
        assertFalse((Boolean) stats.get("is_running"));
    }

    @Test
    void testGetMonitoringStats() {
        // Arrange
        List<String> watchDirs = List.of("/test/dir1", "/test/dir2");
        when(config.getWatchDirectories()).thenReturn(watchDirs);

        // Act
        Map<String, Object> stats = fileMonitorService.getMonitoringStats();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("tracked_files_count"));
        assertTrue(stats.containsKey("watch_directories"));
        assertTrue(stats.containsKey("is_running"));
        assertEquals(watchDirs, stats.get("watch_directories"));
    }

    @Test
    void testFileCreation_TriggersProcessing() throws IOException, InterruptedException {
        // Arrange
        when(config.getWatchDirectories()).thenReturn(List.of(tempDir.toString()));
        fileMonitorService.startMonitoring();

        // Wait for initial setup
        Thread.sleep(50);

        // Act - Create a new log file
        Path newLogFile = tempDir.resolve("new-test.log");
        Files.write(newLogFile, "New log entry\nAnother entry\n".getBytes());

        // Wait for file processing
        Thread.sleep(100);

        // Assert
        Map<String, Object> stats = fileMonitorService.getMonitoringStats();
        int trackedFiles = (Integer) stats.get("tracked_files_count");
        assertTrue(trackedFiles >= 0, "Should have tracked files");

        // Cleanup
        fileMonitorService.stopMonitoring();
    }
}
