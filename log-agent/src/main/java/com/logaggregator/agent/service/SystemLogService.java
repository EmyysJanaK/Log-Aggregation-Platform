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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for collecting system logs from the operating system.
 * Supports Windows Event Log and Unix syslog collection.
 */
@Service
public class SystemLogService {

    private static final Logger logger = LoggerFactory.getLogger(SystemLogService.class);

    private final LogAgentConfig config;
    private final LogProducerService logProducerService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final AtomicLong collectedLogsCount = new AtomicLong(0);
    private boolean isRunning = false;

    @Autowired
    public SystemLogService(LogAgentConfig config, LogProducerService logProducerService) {
        this.config = config;
        this.logProducerService = logProducerService;
    }

    /**
     * Start system log collection
     */
    public void startCollection() {
        if (isRunning) {
            logger.warn("System log collection is already running");
            return;
        }

        isRunning = true;

        // Start periodic system log collection
        scheduler.scheduleAtFixedRate(
            this::collectSystemLogs,
            0,
            config.getScanIntervalSeconds() * 2L, // Less frequent than file monitoring
            TimeUnit.SECONDS
        );

        logger.info("System log collection started");
    }

    /**
     * Stop system log collection
     */
    public void stopCollection() {
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

        logger.info("System log collection stopped");
    }

    /**
     * Collect system logs based on the operating system
     */
    private void collectSystemLogs() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.contains("win")) {
                collectWindowsEventLogs();
            } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("mac")) {
                collectUnixSystemLogs();
            } else {
                logger.debug("Unsupported OS for system log collection: {}", osName);
            }

        } catch (Exception e) {
            logger.error("Error collecting system logs: {}", e.getMessage());
        }
    }

    /**
     * Collect Windows Event Logs using PowerShell
     */
    private void collectWindowsEventLogs() {
        try {
            // Use PowerShell to get recent system events
            String command = "powershell.exe Get-EventLog -LogName System -Newest 10 | Select-Object TimeGenerated,EntryType,Source,Message | ConvertTo-Json";

            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode == 0 && output.length() > 0) {
                parseWindowsEventLogs(output.toString());
            }

        } catch (IOException | InterruptedException e) {
            logger.debug("Could not collect Windows event logs: {}", e.getMessage());
        }
    }

    /**
     * Parse Windows Event Log JSON output
     */
    private void parseWindowsEventLogs(String jsonOutput) {
        try {
            // Simple JSON parsing for demonstration - in production use Jackson
            String[] events = jsonOutput.split("\\},\\s*\\{");

            for (String event : events) {
                LogEntry logEntry = parseWindowsEvent(event);
                if (logEntry != null) {
                    logProducerService.sendLogEntry(logEntry);
                    collectedLogsCount.incrementAndGet();
                }
            }

        } catch (Exception e) {
            logger.debug("Error parsing Windows event logs: {}", e.getMessage());
        }
    }

    /**
     * Parse individual Windows event
     */
    private LogEntry parseWindowsEvent(String eventJson) {
        try {
            // Extract basic information from JSON-like string
            String message = extractJsonValue(eventJson, "Message");
            String source = extractJsonValue(eventJson, "Source");
            String entryType = extractJsonValue(eventJson, "EntryType");

            if (message != null && !message.trim().isEmpty()) {
                LogLevel level = mapWindowsEventType(entryType);
                String sourceId = config.getAgentId() + ":windows-event:" + source;

                LogEntry entry = new LogEntry(sourceId, level, message);
                entry.setHostname(config.getHostname());
                entry.setApplication("windows-event-log");
                entry.addMetadata("agent_id", config.getAgentId());
                entry.addMetadata("event_source", source);
                entry.addMetadata("event_type", entryType);
                entry.addTag("log_type", "system");
                entry.addTag("os", "windows");

                return entry;
            }

        } catch (Exception e) {
            logger.debug("Error parsing Windows event: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Extract value from simple JSON string
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);

        if (m.find()) {
            return m.group(1);
        }

        return null;
    }

    /**
     * Map Windows event type to LogLevel
     */
    private LogLevel mapWindowsEventType(String entryType) {
        if (entryType == null) return LogLevel.INFO;

        switch (entryType.toLowerCase()) {
            case "error":
                return LogLevel.ERROR;
            case "warning":
                return LogLevel.WARN;
            case "information":
                return LogLevel.INFO;
            default:
                return LogLevel.INFO;
        }
    }

    /**
     * Collect Unix/Linux system logs
     */
    private void collectUnixSystemLogs() {
        try {
            // Try to read recent syslog entries
            String[] commands = {
                "tail -n 10 /var/log/syslog",
                "tail -n 10 /var/log/messages",
                "journalctl -n 10 --no-pager"
            };

            for (String command : commands) {
                if (executeUnixCommand(command)) {
                    break; // Successfully collected logs
                }
            }

        } catch (Exception e) {
            logger.debug("Error collecting Unix system logs: {}", e.getMessage());
        }
    }

    /**
     * Execute Unix command and process output
     */
    private boolean executeUnixCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int logCount = 0;

            while ((line = reader.readLine()) != null && logCount < 10) {
                LogEntry logEntry = parseUnixSystemLog(line);
                if (logEntry != null) {
                    logProducerService.sendLogEntry(logEntry);
                    collectedLogsCount.incrementAndGet();
                    logCount++;
                }
            }

            int exitCode = process.waitFor();
            return exitCode == 0 && logCount > 0;

        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Parse Unix system log line
     */
    private LogEntry parseUnixSystemLog(String logLine) {
        if (logLine == null || logLine.trim().isEmpty()) {
            return null;
        }

        try {
            String sourceId = config.getAgentId() + ":system-log";
            LogLevel level = detectLogLevel(logLine);

            LogEntry entry = new LogEntry(sourceId, level, logLine);
            entry.setHostname(config.getHostname());
            entry.setApplication("system-log");
            entry.addMetadata("agent_id", config.getAgentId());
            entry.addTag("log_type", "system");
            entry.addTag("os", "unix");

            return entry;

        } catch (Exception e) {
            logger.debug("Error parsing Unix system log: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Detect log level from log message content
     */
    private LogLevel detectLogLevel(String message) {
        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("error") || lowerMessage.contains("fail") ||
            lowerMessage.contains("critical") || lowerMessage.contains("fatal")) {
            return LogLevel.ERROR;
        } else if (lowerMessage.contains("warn") || lowerMessage.contains("warning")) {
            return LogLevel.WARN;
        } else if (lowerMessage.contains("debug")) {
            return LogLevel.DEBUG;
        } else {
            return LogLevel.INFO;
        }
    }

    /**
     * Get collection statistics
     */
    public Map<String, Object> getCollectionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("is_running", isRunning);
        stats.put("collected_logs_count", collectedLogsCount.get());
        stats.put("os_name", System.getProperty("os.name"));
        stats.put("collection_enabled", config.isEnableSystemLogs());
        return stats;
    }
}
