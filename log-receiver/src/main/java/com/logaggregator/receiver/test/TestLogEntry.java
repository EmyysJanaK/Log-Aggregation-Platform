package com.logaggregator.receiver.test;

import java.time.LocalDateTime;

/**
 * A test implementation of a log entry with common log fields
 */
public class TestLogEntry {
    private final String id;
    private final String service;
    private final String level;
    private final String message;
    private final LocalDateTime timestamp;

    public TestLogEntry(String id, String service, String level, String message, LocalDateTime timestamp) {
        this.id = id;
        this.service = service;
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getService() {
        return service;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "TestLogEntry{" +
                "id='" + id + '\'' +
                ", service='" + service + '\'' +
                ", level='" + level + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
