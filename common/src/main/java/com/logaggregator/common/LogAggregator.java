/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 */

package com.logaggregator.common;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides aggregation and statistical analysis capabilities for log entries.
 * This class can generate various metrics and summaries from collections of log entries.
 */
public class LogAggregator {

    /**
     * Represents aggregated statistics for a collection of log entries
     */
    public static class LogStatistics {
        private final long totalCount;
        private final Map<LogLevel, Long> levelCounts;
        private final Map<String, Long> sourceCounts;
        private final Map<String, Long> applicationCounts;
        private final LocalDateTime earliestTimestamp;
        private final LocalDateTime latestTimestamp;
        private final long errorCount;
        private final long warningCount;
        private final double errorRate;
        private final Map<String, Long> topMessages;

        public LogStatistics(long totalCount, Map<LogLevel, Long> levelCounts,
                           Map<String, Long> sourceCounts, Map<String, Long> applicationCounts,
                           LocalDateTime earliestTimestamp, LocalDateTime latestTimestamp,
                           long errorCount, long warningCount, double errorRate,
                           Map<String, Long> topMessages) {
            this.totalCount = totalCount;
            this.levelCounts = levelCounts;
            this.sourceCounts = sourceCounts;
            this.applicationCounts = applicationCounts;
            this.earliestTimestamp = earliestTimestamp;
            this.latestTimestamp = latestTimestamp;
            this.errorCount = errorCount;
            this.warningCount = warningCount;
            this.errorRate = errorRate;
            this.topMessages = topMessages;
        }

        // Getters
        public long getTotalCount() { return totalCount; }
        public Map<LogLevel, Long> getLevelCounts() { return levelCounts; }
        public Map<String, Long> getSourceCounts() { return sourceCounts; }
        public Map<String, Long> getApplicationCounts() { return applicationCounts; }
        public LocalDateTime getEarliestTimestamp() { return earliestTimestamp; }
        public LocalDateTime getLatestTimestamp() { return latestTimestamp; }
        public long getErrorCount() { return errorCount; }
        public long getWarningCount() { return warningCount; }
        public double getErrorRate() { return errorRate; }
        public Map<String, Long> getTopMessages() { return topMessages; }

        public long getTimeSpanMinutes() {
            if (earliestTimestamp != null && latestTimestamp != null) {
                return ChronoUnit.MINUTES.between(earliestTimestamp, latestTimestamp);
            }
            return 0;
        }

        @Override
        public String toString() {
            return String.format(
                "LogStatistics{totalCount=%d, errorRate=%.2f%%, timeSpan=%d minutes}",
                totalCount, errorRate * 100, getTimeSpanMinutes()
            );
        }
    }

    /**
     * Generate comprehensive statistics for a collection of log entries
     */
    public static LogStatistics generateStatistics(Collection<LogEntry> logEntries) {
        if (logEntries == null || logEntries.isEmpty()) {
            return new LogStatistics(0, new HashMap<>(), new HashMap<>(),
                                   new HashMap<>(), null, null, 0, 0, 0.0, new HashMap<>());
        }

        long totalCount = logEntries.size();

        // Count by log level
        Map<LogLevel, Long> levelCounts = logEntries.stream()
            .filter(entry -> entry.getLevel() != null)
            .collect(Collectors.groupingBy(LogEntry::getLevel, Collectors.counting()));

        // Count by source
        Map<String, Long> sourceCounts = logEntries.stream()
            .filter(entry -> entry.getSource() != null)
            .collect(Collectors.groupingBy(LogEntry::getSource, Collectors.counting()));

        // Count by application
        Map<String, Long> applicationCounts = logEntries.stream()
            .filter(entry -> entry.getApplication() != null)
            .collect(Collectors.groupingBy(LogEntry::getApplication, Collectors.counting()));

        // Find time range
        Optional<LocalDateTime> earliest = logEntries.stream()
            .map(LogEntry::getTimestamp)
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo);

        Optional<LocalDateTime> latest = logEntries.stream()
            .map(LogEntry::getTimestamp)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo);

        // Count errors and warnings
        long errorCount = logEntries.stream()
            .mapToLong(entry -> entry.isError() ? 1 : 0)
            .sum();

        long warningCount = logEntries.stream()
            .mapToLong(entry -> entry.isWarning() ? 1 : 0)
            .sum();

        double errorRate = totalCount > 0 ? (double) errorCount / totalCount : 0.0;

        // Top messages
        Map<String, Long> topMessages = logEntries.stream()
            .filter(entry -> entry.getMessage() != null)
            .collect(Collectors.groupingBy(LogEntry::getMessage, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));

        return new LogStatistics(totalCount, levelCounts, sourceCounts, applicationCounts,
                               earliest.orElse(null), latest.orElse(null),
                               errorCount, warningCount, errorRate, topMessages);
    }

    /**
     * Group log entries by time intervals
     */
    public static Map<LocalDateTime, List<LogEntry>> groupByTimeInterval(
            Collection<LogEntry> logEntries, ChronoUnit interval, int amount) {

        return logEntries.stream()
            .filter(entry -> entry.getTimestamp() != null)
            .collect(Collectors.groupingBy(
                entry -> truncateToInterval(entry.getTimestamp(), interval, amount),
                TreeMap::new,
                Collectors.toList()
            ));
    }

    /**
     * Get log entries grouped by hour
     */
    public static Map<LocalDateTime, List<LogEntry>> groupByHour(Collection<LogEntry> logEntries) {
        return groupByTimeInterval(logEntries, ChronoUnit.HOURS, 1);
    }

    /**
     * Get log entries grouped by day
     */
    public static Map<LocalDateTime, List<LogEntry>> groupByDay(Collection<LogEntry> logEntries) {
        return groupByTimeInterval(logEntries, ChronoUnit.DAYS, 1);
    }

    /**
     * Get error rate over time intervals
     */
    public static Map<LocalDateTime, Double> getErrorRateOverTime(
            Collection<LogEntry> logEntries, ChronoUnit interval, int amount) {

        Map<LocalDateTime, List<LogEntry>> grouped = groupByTimeInterval(logEntries, interval, amount);

        return grouped.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<LogEntry> entries = entry.getValue();
                    long errorCount = entries.stream()
                        .mapToLong(logEntry -> logEntry.isError() ? 1 : 0)
                        .sum();
                    return entries.isEmpty() ? 0.0 : (double) errorCount / entries.size();
                }
            ));
    }

    /**
     * Find the most active sources
     */
    public static Map<String, Long> getTopSources(Collection<LogEntry> logEntries, int limit) {
        return logEntries.stream()
            .filter(entry -> entry.getSource() != null)
            .collect(Collectors.groupingBy(LogEntry::getSource, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    /**
     * Find entries with similar patterns
     */
    public static Map<String, List<LogEntry>> groupByMessagePattern(Collection<LogEntry> logEntries) {
        return logEntries.stream()
            .filter(entry -> entry.getMessage() != null)
            .collect(Collectors.groupingBy(
                entry -> normalizeMessage(entry.getMessage()),
                Collectors.toList()
            ));
    }

    /**
     * Get log volume over time
     */
    public static Map<LocalDateTime, Long> getLogVolumeOverTime(
            Collection<LogEntry> logEntries, ChronoUnit interval, int amount) {

        Map<LocalDateTime, List<LogEntry>> grouped = groupByTimeInterval(logEntries, interval, amount);

        return grouped.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (long) entry.getValue().size()
            ));
    }

    /**
     * Detect anomalies in log patterns
     */
    public static List<LogEntry> detectAnomalies(Collection<LogEntry> logEntries) {
        List<LogEntry> anomalies = new ArrayList<>();

        // Find entries with unusual error patterns
        Map<String, Long> messageCounts = logEntries.stream()
            .filter(entry -> entry.getMessage() != null)
            .collect(Collectors.groupingBy(LogEntry::getMessage, Collectors.counting()));

        // Calculate average message frequency
        double avgFrequency = messageCounts.values().stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);

        // Find messages that appear unusually frequently (potential spam/issues)
        double threshold = avgFrequency * 5; // 5x average frequency

        Set<String> anomalousMessages = messageCounts.entrySet().stream()
            .filter(entry -> entry.getValue() > threshold)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        // Collect entries with anomalous messages
        anomalies.addAll(logEntries.stream()
            .filter(entry -> anomalousMessages.contains(entry.getMessage()))
            .collect(Collectors.toList()));

        return anomalies;
    }

    /**
     * Calculate log processing rate (logs per minute)
     */
    public static double calculateLogRate(Collection<LogEntry> logEntries) {
        if (logEntries.isEmpty()) return 0.0;

        Optional<LocalDateTime> earliest = logEntries.stream()
            .map(LogEntry::getTimestamp)
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo);

        Optional<LocalDateTime> latest = logEntries.stream()
            .map(LogEntry::getTimestamp)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo);

        if (earliest.isPresent() && latest.isPresent()) {
            long minutes = ChronoUnit.MINUTES.between(earliest.get(), latest.get());
            return minutes > 0 ? (double) logEntries.size() / minutes : logEntries.size();
        }

        return 0.0;
    }

    // Helper methods

    private static LocalDateTime truncateToInterval(LocalDateTime dateTime, ChronoUnit unit, int amount) {
        switch (unit) {
            case MINUTES:
                return dateTime.truncatedTo(ChronoUnit.MINUTES)
                    .withMinute((dateTime.getMinute() / amount) * amount);
            case HOURS:
                return dateTime.truncatedTo(ChronoUnit.HOURS);
            case DAYS:
                return dateTime.truncatedTo(ChronoUnit.DAYS);
            default:
                return dateTime.truncatedTo(ChronoUnit.HOURS);
        }
    }

    private static String normalizeMessage(String message) {
        // Remove numbers, IDs, and timestamps to group similar messages
        return message.replaceAll("\\d+", "X")
                     .replaceAll("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b", "UUID")
                     .replaceAll("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b", "IP")
                     .replaceAll("\\s+", " ")
                     .trim();
    }
}
