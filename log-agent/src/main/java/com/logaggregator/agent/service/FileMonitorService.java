/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 */

package com.logaggregator.agent.service;

import com.logaggregator.agent.config.LogAgentConfig;
import com.logaggregator.common.LogEntry;
import com.logaggregator.common.LogParser;
import com.logaggregator.common.LogSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for monitoring log files and detecting changes.
 * Uses Java NIO WatchService for efficient file monitoring.
 */
@Service
public class FileMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(FileMonitorService.class);

    private final LogAgentConfig config;
    private final LogProducerService logProducerService;
    private final Map<Path, LogFileTracker> trackedFiles = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private WatchService watchService;
    private boolean isRunning = false;

    @Autowired
    public FileMonitorService(LogAgentConfig config, LogProducerService logProducerService) {
        this.config = config;
        this.logProducerService = logProducerService;
    }

    /**
     * Tracks file position and metadata for tail-like functionality
     */
    private static class LogFileTracker {
        private final Path filePath;
        private final LogSource logSource;
        private long lastPosition;
        private long lastModified;
        private RandomAccessFile randomAccessFile;

        public LogFileTracker(Path filePath, LogSource logSource) {
            this.filePath = filePath;
            this.logSource = logSource;
            this.lastPosition = 0;
            this.lastModified = 0;
        }

        public Path getFilePath() { return filePath; }
        public LogSource getLogSource() { return logSource; }
        public long getLastPosition() { return lastPosition; }
        public void setLastPosition(long lastPosition) { this.lastPosition = lastPosition; }
        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }
        public RandomAccessFile getRandomAccessFile() { return randomAccessFile; }
        public void setRandomAccessFile(RandomAccessFile randomAccessFile) { this.randomAccessFile = randomAccessFile; }

        public void close() {
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    logger.warn("Error closing file: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Start monitoring configured directories
     */
    public void startMonitoring() {
        if (isRunning) {
            logger.warn("File monitoring is already running");
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
            isRunning = true;

            // Initial scan of watch directories
            performInitialScan();

            // Start file watcher thread
            if (config.isEnableFileWatcher()) {
                scheduler.execute(this::watchForFileChanges);
            }

            // Start periodic scanner
            scheduler.scheduleAtFixedRate(
                this::scanForNewFiles,
                config.getScanIntervalSeconds(),
                config.getScanIntervalSeconds(),
                TimeUnit.SECONDS
            );

            logger.info("File monitoring started for {} directories", config.getWatchDirectories().size());

        } catch (IOException e) {
            logger.error("Failed to start file monitoring: {}", e.getMessage());
            isRunning = false;
        }
    }

    /**
     * Stop monitoring and cleanup resources
     */
    public void stopMonitoring() {
        isRunning = false;

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Close all tracked files
        trackedFiles.values().forEach(LogFileTracker::close);
        trackedFiles.clear();

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.warn("Error closing watch service: {}", e.getMessage());
            }
        }

        logger.info("File monitoring stopped");
    }

    /**
     * Perform initial scan of configured directories
     */
    private void performInitialScan() {
        for (String directory : config.getWatchDirectories()) {
            Path dirPath = Paths.get(directory);
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                scanDirectory(dirPath, true);
                registerDirectoryForWatching(dirPath);
            } else {
                logger.warn("Watch directory does not exist or is not a directory: {}", directory);
            }
        }
    }

    /**
     * Register directory with WatchService
     */
    private void registerDirectoryForWatching(Path directory) {
        try {
            directory.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
            logger.debug("Registered directory for watching: {}", directory);
        } catch (IOException e) {
            logger.error("Failed to register directory for watching: {}", e.getMessage());
        }
    }

    /**
     * Watch for file system events
     */
    private void watchForFileChanges() {
        while (isRunning) {
            try {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path eventPath = (Path) event.context();
                    Path fullPath = ((Path) key.watchable()).resolve(eventPath);

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        handleFileCreated(fullPath);
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        handleFileModified(fullPath);
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        handleFileDeleted(fullPath);
                    }
                }

                if (!key.reset()) {
                    break;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in file watcher: {}", e.getMessage());
            }
        }
    }

    /**
     * Scan directory for log files
     */
    private void scanDirectory(Path directory, boolean isInitialScan) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isLogFile(file) && attrs.size() <= config.getMaxFileSizeBytes()) {
                        if (isInitialScan) {
                            trackFile(file, true); // Start from end for initial scan
                        } else {
                            trackFile(file, false); // Start from beginning for new files
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.error("Error scanning directory {}: {}", directory, e.getMessage());
        }
    }

    /**
     * Check if file matches log file patterns
     */
    private boolean isLogFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return config.getFilePatterns().stream()
            .anyMatch(pattern -> {
                String regexPattern = pattern.replace("*", ".*").replace("?", ".");
                return fileName.matches(regexPattern);
            });
    }

    /**
     * Start tracking a log file
     */
    private void trackFile(Path filePath, boolean startFromEnd) {
        if (trackedFiles.containsKey(filePath)) {
            return; // Already tracking
        }

        try {
            LogSource logSource = createLogSource(filePath);
            LogFileTracker tracker = new LogFileTracker(filePath, logSource);

            RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r");
            tracker.setRandomAccessFile(raf);

            if (startFromEnd) {
                // For existing files, start from the end
                tracker.setLastPosition(raf.length());
            } else {
                // For new files, start from the beginning
                tracker.setLastPosition(0);
            }

            tracker.setLastModified(Files.getLastModifiedTime(filePath).toMillis());
            trackedFiles.put(filePath, tracker);

            logger.info("Started tracking file: {} (position: {})", filePath, tracker.getLastPosition());

        } catch (IOException e) {
            logger.error("Failed to track file {}: {}", filePath, e.getMessage());
        }
    }

    /**
     * Create LogSource from file path
     */
    private LogSource createLogSource(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String filePathStr = filePath.getParent().toString();
        String sourceId = config.getAgentId() + ":" + filePath.toString();

        return new LogSource(fileName, filePathStr, "file", sourceId, config.getHostname());
    }

    /**
     * Handle file creation event
     */
    private void handleFileCreated(Path filePath) {
        if (isLogFile(filePath)) {
            logger.debug("New log file detected: {}", filePath);
            trackFile(filePath, false);
        }
    }

    /**
     * Handle file modification event
     */
    private void handleFileModified(Path filePath) {
        LogFileTracker tracker = trackedFiles.get(filePath);
        if (tracker != null) {
            processFileChanges(tracker);
        }
    }

    /**
     * Handle file deletion event
     */
    private void handleFileDeleted(Path filePath) {
        LogFileTracker tracker = trackedFiles.remove(filePath);
        if (tracker != null) {
            tracker.close();
            logger.debug("Stopped tracking deleted file: {}", filePath);
        }
    }

    /**
     * Process new content in a file
     */
    private void processFileChanges(LogFileTracker tracker) {
        try {
            RandomAccessFile raf = tracker.getRandomAccessFile();
            long currentLength = raf.length();

            if (currentLength < tracker.getLastPosition()) {
                // File was truncated, start from beginning
                tracker.setLastPosition(0);
            }

            if (currentLength > tracker.getLastPosition()) {
                raf.seek(tracker.getLastPosition());

                List<LogEntry> logEntries = new ArrayList<>();
                String line;

                while ((line = raf.readLine()) != null && logEntries.size() < config.getBatchSize()) {
                    LogEntry entry = LogParser.parseLine(line, tracker.getLogSource().getSourceId());
                    if (entry != null) {
                        entry.setHostname(config.getHostname());
                        entry.addMetadata("agent_id", config.getAgentId());
                        entry.addMetadata("file_path", tracker.getFilePath().toString());
                        logEntries.add(entry);
                    }
                }

                tracker.setLastPosition(raf.getFilePointer());

                if (!logEntries.isEmpty()) {
                    logProducerService.sendLogEntries(logEntries);
                    logger.debug("Processed {} log entries from {}", logEntries.size(), tracker.getFilePath());
                }
            }

        } catch (IOException e) {
            logger.error("Error processing file changes for {}: {}", tracker.getFilePath(), e.getMessage());
        }
    }

    /**
     * Periodic scan for new files
     */
    private void scanForNewFiles() {
        for (String directory : config.getWatchDirectories()) {
            Path dirPath = Paths.get(directory);
            if (Files.exists(dirPath)) {
                scanDirectory(dirPath, false);
            }
        }
    }

    /**
     * Get monitoring statistics
     */
    public Map<String, Object> getMonitoringStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("tracked_files_count", trackedFiles.size());
        stats.put("watch_directories", config.getWatchDirectories());
        stats.put("is_running", isRunning);
        stats.put("tracked_files", trackedFiles.keySet().stream().map(Path::toString).toArray());
        return stats;
    }
}
