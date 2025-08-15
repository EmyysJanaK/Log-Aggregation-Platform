package com.logaggregator.receiver.test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * A utility class for generating test log entries
 * This can be used for testing the log receiver functionality
 * without relying on actual log agents
 */
public class TestLogGenerator {

    private static final String[] SERVICES = {
        "user-service", "order-service", "payment-service",
        "inventory-service", "notification-service"
    };

    private static final String[] LOG_LEVELS = {
        "INFO", "WARN", "ERROR", "DEBUG", "TRACE"
    };

    private static final String[] INFO_MESSAGES = {
        "User logged in successfully",
        "Order processed",
        "Payment received",
        "Data synchronized",
        "Cache refreshed"
    };

    private static final String[] WARN_MESSAGES = {
        "High memory usage detected",
        "Slow database query",
        "API rate limit approaching",
        "Connection pool nearing capacity",
        "Cache hit ratio below threshold"
    };

    private static final String[] ERROR_MESSAGES = {
        "Database connection failed",
        "API timeout",
        "Payment processing error",
        "Authentication failed",
        "Unexpected exception"
    };

    private static final Random random = new Random();

    /**
     * Generate a random test log entry
     * @return Map containing log entry data
     */
    public static TestLogEntry generateRandomLog() {
        String service = SERVICES[random.nextInt(SERVICES.length)];
        String level = LOG_LEVELS[random.nextInt(LOG_LEVELS.length)];
        String message;

        // Select message based on log level
        switch (level) {
            case "INFO":
                message = INFO_MESSAGES[random.nextInt(INFO_MESSAGES.length)];
                break;
            case "WARN":
                message = WARN_MESSAGES[random.nextInt(WARN_MESSAGES.length)];
                break;
            case "ERROR":
                message = ERROR_MESSAGES[random.nextInt(ERROR_MESSAGES.length)];
                break;
            default:
                message = "Log message for " + level;
        }

        return new TestLogEntry(
            UUID.randomUUID().toString(),
            service,
            level,
            message,
            LocalDateTime.now()
        );
    }

    /**
     * Generate multiple random test log entries
     * @param count Number of log entries to generate
     * @return List of generated log entries
     */
    public static List<TestLogEntry> generateRandomLogs(int count) {
        List<TestLogEntry> logs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            logs.add(generateRandomLog());
        }
        return logs;
    }

    public static void main(String[] args) {
        // Generate and print 5 random log entries
        List<TestLogEntry> logs = generateRandomLogs(5);
        logs.forEach(System.out::println);
    }
}
