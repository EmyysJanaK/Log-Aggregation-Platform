package com.logaggregator.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.Map;

public class LogEntry {
    private String id;
    private String source;
    private String level;
    private String message;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private Map<String, Object> metadata;
    
    public LogEntry() {}
    
    public LogEntry(String source, String level, String message) {
        this.source = source;
        this.level = level;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.id = generateId();
    }
    
    private String generateId() {
        return System.currentTimeMillis() + "-" + hashCode();
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - %s: %s", 
            timestamp, level, source, message);
    }
}