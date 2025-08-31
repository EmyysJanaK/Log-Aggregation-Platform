//

package com.logaggregator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaggregator.common.LogEntry;
import com.logaggregator.common.LogLevel;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Component
public class LogProcessor {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Pattern for extracting error codes
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("ERROR_\\d+");

    public LogProcessor(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${log.kafka.topic.raw:raw-logs}", groupId = "log-processor-group")
    public void processLog(String rawLogMessage) {
        try {
            LogEntry logEntry = objectMapper.readValue(rawLogMessage, LogEntry.class);

            // Process the log entry
            LogEntry processedLog = enhanceLogEntry(logEntry);

            // Send to processed topic
            String processedMessage = objectMapper.writeValueAsString(processedLog);
            kafkaTemplate.send("processed-logs", processedMessage);

            System.out.println("Processed log: " + processedLog.getId());

        } catch (Exception e) {
            System.err.println("Error processing log: " + e.getMessage());
        }
    }

    private LogEntry enhanceLogEntry(LogEntry original) {
        // Create enhanced log entry with additional processing
        LogEntry enhanced = new LogEntry();
        enhanced.setId(original.getId());
        enhanced.setSource(original.getSource());
        enhanced.setLevel(original.getLevel());
        enhanced.setMessage(original.getMessage());
        enhanced.setTimestamp(original.getTimestamp());

        // Add processing metadata
        enhanced.setProcessedTimestamp(LocalDateTime.now());

        // Extract error codes if present
        if (original.getMessage() != null) {
            java.util.regex.Matcher matcher = ERROR_CODE_PATTERN.matcher(original.getMessage());
            if (matcher.find()) {
                enhanced.addMetadata("errorCode", matcher.group());
            }
        }

        // Categorize log level
        enhanced.addMetadata("severity", categorizeSeverity(original.getLevel()));

        return enhanced;
    }

    private String categorizeSeverity(LogLevel level) {
        if (level == null) return "UNKNOWN";

        switch (level) {
            case ERROR:
            case FATAL:
                return "HIGH";
            case WARN:
                return "MEDIUM";
            case INFO:
            case DEBUG:
                return "LOW";
            default:
                return "UNKNOWN";
        }
    }
}
