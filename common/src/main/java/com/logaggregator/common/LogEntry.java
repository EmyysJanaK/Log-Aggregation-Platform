/*
* this file is part of Log Aggregator.
* * Log Aggregator is free software: you can redistribute it and/or modify
* * it under the terms of the GNU General Public License as published by
*
* */
package com.logaggregator.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single log entry in the log aggregation system.
 * This class encapsulates all information about a log message including
 * its content, metadata, and processing information.
 */
public class LogEntry {
    private String id;
    private String source;
    private LogLevel level;
    private String message;
    private String rawMessage;
    private String hostname;
    private String application;
    private String thread;
    private String loggerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime processedTimestamp;

    private Map<String, Object> metadata;
    private Map<String, String> tags;

    // Default constructor for Jackson
    public LogEntry() {
        this.metadata = new HashMap<>();
        this.tags = new HashMap<>();
        this.processedTimestamp = LocalDateTime.now();
    }

    // Basic constructor
    public LogEntry(String source, LogLevel level, String message) {
        this();
        this.source = validateString(source, "source");
        this.level = level != null ? level : LogLevel.INFO;
        this.message = validateString(message, "message");
        this.rawMessage = message;
        this.timestamp = LocalDateTime.now();
        this.id = generateId();
    }
    
    // Constructor with string level for backward compatibility
    public LogEntry(String source, String level, String message) {
        this(source, parseLogLevel(level), message);
    }

    // Full constructor
    public LogEntry(String source, LogLevel level, String message, String hostname,
                   String application, String thread, String loggerName) {
        this(source, level, message);
        this.hostname = hostname;
        this.application = application;
        this.thread = thread;
        this.loggerName = loggerName;
    }

    // Utility method to parse string to LogLevel
    private static LogLevel parseLogLevel(String level) {
        if (level == null) return LogLevel.INFO;
        try {
            return LogLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LogLevel.INFO;
        }
    }

    // Generate unique ID using UUID
    private String generateId() {
        return UUID.randomUUID().toString();
    }
    
    // Validation helper
    private String validateString(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return value.trim();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public LogLevel getLevel() { return level; }
    public void setLevel(LogLevel level) { this.level = level; }

    // String level setter for backward compatibility
    public void setLevel(String level) { this.level = parseLogLevel(level); }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getRawMessage() { return rawMessage; }
    public void setRawMessage(String rawMessage) { this.rawMessage = rawMessage; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getApplication() { return application; }
    public void setApplication(String application) { this.application = application; }

    public String getThread() { return thread; }
    public void setThread(String thread) { this.thread = thread; }

    public String getLoggerName() { return loggerName; }
    public void setLoggerName(String loggerName) { this.loggerName = loggerName; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public LocalDateTime getProcessedTimestamp() { return processedTimestamp; }
    public void setProcessedTimestamp(LocalDateTime processedTimestamp) {
        this.processedTimestamp = processedTimestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata != null ? metadata : new HashMap<>();
    }
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public Map<String, String> getTags() {
        return tags != null ? tags : new HashMap<>();
    }
    public void setTags(Map<String, String> tags) {
        this.tags = tags != null ? tags : new HashMap<>();
    }

    // Utility methods

    /**
     * Add a metadata entry
     */
    public void addMetadata(String key, Object value) {
        if (metadata == null) metadata = new HashMap<>();
        metadata.put(key, value);
    }

    /**
     * Add a tag
     */
    public void addTag(String key, String value) {
        if (tags == null) tags = new HashMap<>();
        tags.put(key, value);
    }

    /**
     * Check if this log entry has a specific tag
     */
    public boolean hasTag(String key) {
        return tags != null && tags.containsKey(key);
    }

    /**
     * Get tag value
     */
    public String getTag(String key) {
        return tags != null ? tags.get(key) : null;
    }

    /**
     * Check if this is an error level log
     */
    public boolean isError() {
        return level == LogLevel.ERROR || level == LogLevel.FATAL;
    }

    /**
     * Check if this is a warning level log
     */
    public boolean isWarning() {
        return level == LogLevel.WARN;
    }

    /**
     * Check if this is a debug level log
     */
    public boolean isDebug() {
        return level == LogLevel.DEBUG;
    }

    /**
     * Get formatted timestamp as string
     */
    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
    }

    /**
     * Get the log level as string (for backward compatibility)
     */
    public String getLevelAsString() {
        return level != null ? level.toString() : "INFO";
    }

    /**
     * Create a copy of this log entry
     */
    public LogEntry copy() {
        LogEntry copy = new LogEntry();
        copy.id = this.id;
        copy.source = this.source;
        copy.level = this.level;
        copy.message = this.message;
        copy.rawMessage = this.rawMessage;
        copy.hostname = this.hostname;
        copy.application = this.application;
        copy.thread = this.thread;
        copy.loggerName = this.loggerName;
        copy.timestamp = this.timestamp;
        copy.processedTimestamp = this.processedTimestamp;
        copy.metadata = this.metadata != null ? new HashMap<>(this.metadata) : new HashMap<>();
        copy.tags = this.tags != null ? new HashMap<>(this.tags) : new HashMap<>();
        return copy;
    }

    /**
     * Update the processed timestamp to current time
     */
    public void markAsProcessed() {
        this.processedTimestamp = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LogEntry logEntry = (LogEntry) obj;
        return Objects.equals(id, logEntry.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s: %s", 
            getFormattedTimestamp(),
            level != null ? level : "INFO",
            source != null ? source : "UNKNOWN",
            message != null ? message : "");
    }

    /**
     * Get detailed string representation including all fields
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LogEntry{");
        sb.append("id='").append(id).append('\'');
        sb.append(", source='").append(source).append('\'');
        sb.append(", level=").append(level);
        sb.append(", message='").append(message).append('\'');
        sb.append(", hostname='").append(hostname).append('\'');
        sb.append(", application='").append(application).append('\'');
        sb.append(", thread='").append(thread).append('\'');
        sb.append(", timestamp=").append(timestamp);
        sb.append(", tagsCount=").append(tags != null ? tags.size() : 0);
        sb.append(", metadataCount=").append(metadata != null ? metadata.size() : 0);
        sb.append('}');
        return sb.toString();
    }
}

