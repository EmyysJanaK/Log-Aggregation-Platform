package com.logaggregator.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaggregator.common.LogEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class LogProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String serviceName = "payment-service"; // Example service name
    private final String hostName;
    private final String ipAddress;

    @Value("${log.kafka.topic.name:raw-logs}")
    private String topicName;

    private final String[] logLevels = {"INFO", "WARN", "ERROR", "FATAL", "DEBUG"};
    private final String[] messages = {
            "User logged in successfully.",
            "Failed to connect to database.",
            "Processing payment for order #12345.",
            "Invalid input detected.",
            "Configuration loaded."
    };

    public LogProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        try {
            this.hostName = InetAddress.getLocalHost().getHostName();
            this.ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            throw new RuntimeException("Could not get host info", e);
        }
    }

    @Scheduled(fixedRate = 1000) // Send a log every 1 second
    public void generateAndSendLog() {
        String logLevel = logLevels[ThreadLocalRandom.current().nextInt(logLevels.length)];
        String message = messages[ThreadLocalRandom.current().nextInt(messages.length)];

        LogEntry logEntry = new LogEntry(
                UUID.randomUUID().toString(),
                Instant.now(),
                serviceName,
                logLevel,
                "main-thread",
                message,
                hostName,
                ipAddress
        );

        try {
            String logJson = objectMapper.writeValueAsString(logEntry);
            kafkaTemplate.send(topicName, logEntry.getId(), logJson); // Use ID as key for ordering if needed
            System.out.println("Sent log: " + logJson);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing log entry: " + e.getMessage());
        }
    }
}