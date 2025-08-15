package com.logaggregator.receiver.temp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A temporary utility to simulate log generation to a file
 * This can be used for testing file-based log ingestion
 */
public class LogFileSimulator {

    private static final String[] LOG_LEVELS = {"INFO", "WARN", "ERROR", "DEBUG"};
    private static final String[] SERVICE_NAMES = {"user-service", "order-service", "payment-service"};
    private static final String[] ACTIONS = {
        "started", "completed", "failed", "processed", "received", "sent", "validated"
    };
    private static final String[] OBJECTS = {
        "request", "response", "transaction", "data", "message", "event", "task"
    };

    private static final Random random = new Random();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final String outputDir;
    private final String filename;
    private final int intervalSeconds;
    private final ScheduledExecutorService scheduler;

    public LogFileSimulator(String outputDir, String filename, int intervalSeconds) {
        this.outputDir = outputDir;
        this.filename = filename;
        this.intervalSeconds = intervalSeconds;
        this.scheduler = Executors.newScheduledThreadPool(1);

        // Create output directory if it doesn't exist
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void start() {
        System.out.println("Starting log file simulator...");
        System.out.println("Writing logs to: " + outputDir + File.separator + filename);
        System.out.println("Log interval: " + intervalSeconds + " seconds");

        scheduler.scheduleAtFixedRate(this::generateLogEntry, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        System.out.println("Stopping log file simulator...");
        scheduler.shutdown();
    }

    private void generateLogEntry() {
        String level = LOG_LEVELS[random.nextInt(LOG_LEVELS.length)];
        String service = SERVICE_NAMES[random.nextInt(SERVICE_NAMES.length)];
        String action = ACTIONS[random.nextInt(ACTIONS.length)];
        String object = OBJECTS[random.nextInt(OBJECTS.length)];

        String timestamp = LocalDateTime.now().format(formatter);
        String logId = String.format("LOG-%08d", random.nextInt(100000000));
        String message = String.format("%s %s %s", action, object, generateRandomId());

        String logEntry = String.format("%s [%s] %s - %s: %s%n",
                timestamp, level, logId, service, message);

        writeToFile(logEntry);
    }

    private String generateRandomId() {
        return String.format("ID-%06d", random.nextInt(1000000));
    }

    private void writeToFile(String logEntry) {
        String filePath = outputDir + File.separator + filename;

        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.write(logEntry);
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Example usage: simulate logs every 2 seconds to temp/logs/simulated.log
        String outputDir = "temp/logs";
        String filename = "simulated.log";
        int intervalSeconds = 2;

        LogFileSimulator simulator = new LogFileSimulator(outputDir, filename, intervalSeconds);
        simulator.start();

        // Run for 1 minute then stop
        try {
            Thread.sleep(60 * 1000);
            simulator.stop();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
