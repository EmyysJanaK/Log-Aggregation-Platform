package com.logaggregator.receiver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaggregator.common.LogEntry;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LogConsumer {

    private final ObjectMapper objectMapper;
    private final LogRepository logRepository; // Spring Data Elasticsearch repository

    public LogConsumer(ObjectMapper objectMapper, LogRepository logRepository) {
        this.objectMapper = objectMapper;
        this.logRepository = logRepository;
    }

    @KafkaListener(topics = "${log.kafka.topic.name:raw-logs}", groupId = "log-receiver-group")
    public void listen(String message) {
        try {
            LogEntry logEntry = objectMapper.readValue(message, LogEntry.class);
            logRepository.save(logEntry); // Save to Elasticsearch
            System.out.println("Received and saved log: " + logEntry.getId());
        } catch (IOException e) {
            System.err.println("Error deserializing log entry or saving to ES: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
