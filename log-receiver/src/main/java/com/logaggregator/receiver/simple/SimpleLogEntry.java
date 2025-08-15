package com.logaggregator.receiver.simple;

import java.time.Instant;
import java.util.UUID;

/**
 * A simple implementation of a log entry for testing purposes
 */
public class SimpleLogEntry {
    private final String id;
    private final String source;
    private final String level;
    private final String message;
    private final Instant timestamp;

    public SimpleLogEntry(String source, String level, String message) {
        if (source == null || level == null || message == null) {
            throw new IllegalArgumentException("Source, level, and message cannot be null");
        }

        this.id = UUID.randomUUID().toString();
        this.source = source;
        this.level = level;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "SimpleLogEntry{" +
                "id='" + id + '\'' +
                ", source='" + source + '\'' +
                ", level='" + level + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
