/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 */

package com.logaggregator.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing log entries from various formats.
 * Supports common log formats including Apache, Nginx, syslog, and custom formats.
 */
public class LogParser {

    // Common log format patterns
    private static final Pattern APACHE_COMMON_LOG = Pattern.compile(
        "^(\\S+) \\S+ \\S+ \\[([^\\]]+)\\] \"([^\"]+)\" (\\d+) (\\d+|-)"
    );

    private static final Pattern APACHE_COMBINED_LOG = Pattern.compile(
        "^(\\S+) \\S+ \\S+ \\[([^\\]]+)\\] \"([^\"]+)\" (\\d+) (\\d+|-) \"([^\"]*)\" \"([^\"]*)\""
    );

    private static final Pattern SYSLOG_PATTERN = Pattern.compile(
        "^(\\w{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+(\\S+)\\s+(\\S+)(?:\\[(\\d+)\\])?:\\s*(.+)"
    );

    private static final Pattern JAVA_LOG_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+\\[(\\w+)\\]\\s+(\\w+)\\s+(.+?)\\s+-\\s+(.+)"
    );

    private static final Pattern NGINX_ACCESS_LOG = Pattern.compile(
        "^(\\S+) - (\\S+) \\[([^\\]]+)\\] \"([^\"]+)\" (\\d+) (\\d+) \"([^\"]*)\" \"([^\"]*)\""
    );

    // Date format patterns
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("MMM dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z"),
        DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss")
    };

    /**
     * Parse a log line and create a LogEntry
     */
    public static LogEntry parseLine(String logLine, String sourceId) {
        if (logLine == null || logLine.trim().isEmpty()) {
            return null;
        }

        LogEntry entry = null;

        // Try different parsing strategies
        entry = parseJavaLog(logLine, sourceId);
        if (entry != null) return entry;

        entry = parseSyslog(logLine, sourceId);
        if (entry != null) return entry;

        entry = parseApacheLog(logLine, sourceId);
        if (entry != null) return entry;

        entry = parseNginxLog(logLine, sourceId);
        if (entry != null) return entry;

        // Fallback: create a generic log entry
        return createGenericLogEntry(logLine, sourceId);
    }

    /**
     * Parse Java application log format
     */
    private static LogEntry parseJavaLog(String logLine, String sourceId) {
        Matcher matcher = JAVA_LOG_PATTERN.matcher(logLine);
        if (matcher.matches()) {
            String timestamp = matcher.group(1);
            String level = matcher.group(2);
            String thread = matcher.group(3);
            String logger = matcher.group(4);
            String message = matcher.group(5);

            LogEntry entry = new LogEntry(sourceId, LogLevel.fromString(level), message);
            entry.setTimestamp(parseDateTime(timestamp));
            entry.setThread(thread);
            entry.setLoggerName(logger);
            entry.setRawMessage(logLine);

            return entry;
        }
        return null;
    }

    /**
     * Parse syslog format
     */
    private static LogEntry parseSyslog(String logLine, String sourceId) {
        Matcher matcher = SYSLOG_PATTERN.matcher(logLine);
        if (matcher.matches()) {
            String timestamp = matcher.group(1);
            String hostname = matcher.group(2);
            String application = matcher.group(3);
            String pid = matcher.group(4);
            String message = matcher.group(5);

            LogEntry entry = new LogEntry(sourceId, LogLevel.INFO, message);
            entry.setTimestamp(parseDateTime(timestamp));
            entry.setHostname(hostname);
            entry.setApplication(application);
            entry.setRawMessage(logLine);

            if (pid != null) {
                entry.addMetadata("pid", pid);
            }

            // Detect log level from message content
            LogLevel detectedLevel = detectLogLevelFromMessage(message);
            entry.setLevel(detectedLevel);

            return entry;
        }
        return null;
    }

    /**
     * Parse Apache log format
     */
    private static LogEntry parseApacheLog(String logLine, String sourceId) {
        Matcher matcher = APACHE_COMBINED_LOG.matcher(logLine);
        if (!matcher.matches()) {
            matcher = APACHE_COMMON_LOG.matcher(logLine);
        }

        if (matcher.matches()) {
            String clientIp = matcher.group(1);
            String timestamp = matcher.group(2);
            String request = matcher.group(3);
            String status = matcher.group(4);
            String size = matcher.group(5);

            LogEntry entry = new LogEntry(sourceId, LogLevel.INFO, request);
            entry.setTimestamp(parseDateTime(timestamp));
            entry.setRawMessage(logLine);
            entry.setApplication("apache");

            entry.addMetadata("client_ip", clientIp);
            entry.addMetadata("status_code", status);
            entry.addMetadata("response_size", size);

            // Set log level based on HTTP status code
            int statusCode = Integer.parseInt(status);
            if (statusCode >= 500) {
                entry.setLevel(LogLevel.ERROR);
            } else if (statusCode >= 400) {
                entry.setLevel(LogLevel.WARN);
            }

            return entry;
        }
        return null;
    }

    /**
     * Parse Nginx log format
     */
    private static LogEntry parseNginxLog(String logLine, String sourceId) {
        Matcher matcher = NGINX_ACCESS_LOG.matcher(logLine);
        if (matcher.matches()) {
            String clientIp = matcher.group(1);
            String user = matcher.group(2);
            String timestamp = matcher.group(3);
            String request = matcher.group(4);
            String status = matcher.group(5);
            String size = matcher.group(6);
            String referer = matcher.group(7);
            String userAgent = matcher.group(8);

            LogEntry entry = new LogEntry(sourceId, LogLevel.INFO, request);
            entry.setTimestamp(parseDateTime(timestamp));
            entry.setRawMessage(logLine);
            entry.setApplication("nginx");

            entry.addMetadata("client_ip", clientIp);
            entry.addMetadata("user", user);
            entry.addMetadata("status_code", status);
            entry.addMetadata("response_size", size);
            entry.addMetadata("referer", referer);
            entry.addMetadata("user_agent", userAgent);

            return entry;
        }
        return null;
    }

    /**
     * Create a generic log entry when no specific format is detected
     */
    private static LogEntry createGenericLogEntry(String logLine, String sourceId) {
        LogLevel level = detectLogLevelFromMessage(logLine);
        LogEntry entry = new LogEntry(sourceId, level, logLine);
        entry.setRawMessage(logLine);
        return entry;
    }

    /**
     * Detect log level from message content using keywords
     */
    private static LogLevel detectLogLevelFromMessage(String message) {
        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("fatal") || lowerMessage.contains("critical")) {
            return LogLevel.FATAL;
        } else if (lowerMessage.contains("error") || lowerMessage.contains("exception") ||
                   lowerMessage.contains("failed") || lowerMessage.contains("failure")) {
            return LogLevel.ERROR;
        } else if (lowerMessage.contains("warn") || lowerMessage.contains("warning") ||
                   lowerMessage.contains("deprecated")) {
            return LogLevel.WARN;
        } else if (lowerMessage.contains("debug") || lowerMessage.contains("trace")) {
            return LogLevel.DEBUG;
        } else {
            return LogLevel.INFO;
        }
    }

    /**
     * Parse date/time string using multiple format attempts
     */
    private static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateTimeStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        // If all formatters fail, return current time
        return LocalDateTime.now();
    }

    /**
     * Parse structured log data (JSON-like format)
     */
    public static LogEntry parseStructuredLog(String jsonLog, String sourceId) {
        // This is a simplified version - in a real implementation,
        // you would use a JSON parser like Jackson
        try {
            // Basic JSON-like parsing for demonstration
            Map<String, String> fields = parseSimpleJson(jsonLog);

            String level = fields.getOrDefault("level", "INFO");
            String message = fields.getOrDefault("message", jsonLog);
            String timestamp = fields.get("timestamp");

            LogEntry entry = new LogEntry(sourceId, LogLevel.fromString(level), message);

            if (timestamp != null) {
                entry.setTimestamp(parseDateTime(timestamp));
            }

            // Add all fields as metadata
            for (Map.Entry<String, String> field : fields.entrySet()) {
                if (!field.getKey().equals("level") && !field.getKey().equals("message") &&
                    !field.getKey().equals("timestamp")) {
                    entry.addMetadata(field.getKey(), field.getValue());
                }
            }

            entry.setRawMessage(jsonLog);
            return entry;

        } catch (Exception e) {
            return createGenericLogEntry(jsonLog, sourceId);
        }
    }

    /**
     * Simple JSON parser for basic key-value extraction
     */
    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> fields = new HashMap<>();

        // Remove braces and split by commas
        String content = json.replaceAll("[{}]", "").trim();
        String[] pairs = content.split(",");

        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("\"", "");
                String value = keyValue[1].trim().replaceAll("\"", "");
                fields.put(key, value);
            }
        }

        return fields;
    }

    /**
     * Check if a log line matches a specific format
     */
    public static boolean isFormatSupported(String logLine) {
        return parseJavaLog(logLine, "test") != null ||
               parseSyslog(logLine, "test") != null ||
               parseApacheLog(logLine, "test") != null ||
               parseNginxLog(logLine, "test") != null;
    }
}
