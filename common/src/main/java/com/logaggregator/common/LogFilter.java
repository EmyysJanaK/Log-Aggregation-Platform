/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 */

package com.logaggregator.common;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Provides filtering capabilities for log entries based on various criteria.
 * This class implements the Strategy pattern for different filtering approaches.
 */
public class LogFilter {

    private LogLevel minLevel;
    private LogLevel maxLevel;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> sources;
    private List<String> applications;
    private Pattern messagePattern;
    private String hostname;
    private boolean includeErrors;
    private boolean includeWarnings;

    public LogFilter() {
        this.minLevel = LogLevel.DEBUG;
        this.maxLevel = LogLevel.FATAL;
        this.includeErrors = true;
        this.includeWarnings = true;
    }

    /**
     * Creates a predicate that can be used to filter log entries
     */
    public Predicate<LogEntry> toPredicate() {
        return logEntry -> {
            // Level filtering
            if (logEntry.getLevel() != null) {
                if (logEntry.getLevel().isLessSevereThan(minLevel) ||
                    logEntry.getLevel().isMoreSevereThan(maxLevel)) {
                    return false;
                }
            }

            // Time range filtering
            if (startTime != null && logEntry.getTimestamp() != null &&
                logEntry.getTimestamp().isBefore(startTime)) {
                return false;
            }

            if (endTime != null && logEntry.getTimestamp() != null &&
                logEntry.getTimestamp().isAfter(endTime)) {
                return false;
            }

            // Source filtering
            if (sources != null && !sources.isEmpty() &&
                !sources.contains(logEntry.getSource())) {
                return false;
            }

            // Application filtering
            if (applications != null && !applications.isEmpty() &&
                !applications.contains(logEntry.getApplication())) {
                return false;
            }

            // Hostname filtering
            if (hostname != null && !hostname.equals(logEntry.getHostname())) {
                return false;
            }

            // Message pattern filtering
            if (messagePattern != null && logEntry.getMessage() != null &&
                !messagePattern.matcher(logEntry.getMessage()).find()) {
                return false;
            }

            // Error/Warning inclusion
            if (!includeErrors && logEntry.isError()) {
                return false;
            }

            if (!includeWarnings && logEntry.isWarning()) {
                return false;
            }

            return true;
        };
    }

    // Builder pattern methods
    public LogFilter withMinLevel(LogLevel minLevel) {
        this.minLevel = minLevel;
        return this;
    }

    public LogFilter withMaxLevel(LogLevel maxLevel) {
        this.maxLevel = maxLevel;
        return this;
    }

    public LogFilter withTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        return this;
    }

    public LogFilter withSources(List<String> sources) {
        this.sources = sources;
        return this;
    }

    public LogFilter withApplications(List<String> applications) {
        this.applications = applications;
        return this;
    }

    public LogFilter withMessagePattern(String pattern) {
        this.messagePattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        return this;
    }

    public LogFilter withHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public LogFilter includeErrors(boolean includeErrors) {
        this.includeErrors = includeErrors;
        return this;
    }

    public LogFilter includeWarnings(boolean includeWarnings) {
        this.includeWarnings = includeWarnings;
        return this;
    }

    // Static factory methods for common filters
    public static LogFilter errorsOnly() {
        return new LogFilter()
                .withMinLevel(LogLevel.ERROR)
                .includeWarnings(false);
    }

    public static LogFilter warningsAndErrors() {
        return new LogFilter()
                .withMinLevel(LogLevel.WARN);
    }

    public static LogFilter infoAndAbove() {
        return new LogFilter()
                .withMinLevel(LogLevel.INFO);
    }

    public static LogFilter forSource(String source) {
        return new LogFilter()
                .withSources(List.of(source));
    }

    public static LogFilter forTimeRange(LocalDateTime start, LocalDateTime end) {
        return new LogFilter()
                .withTimeRange(start, end);
    }

    public static LogFilter containingMessage(String messagePattern) {
        return new LogFilter()
                .withMessagePattern(messagePattern);
    }

    // Getters
    public LogLevel getMinLevel() { return minLevel; }
    public LogLevel getMaxLevel() { return maxLevel; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public List<String> getSources() { return sources; }
    public List<String> getApplications() { return applications; }
    public Pattern getMessagePattern() { return messagePattern; }
    public String getHostname() { return hostname; }
    public boolean isIncludeErrors() { return includeErrors; }
    public boolean isIncludeWarnings() { return includeWarnings; }

    @Override
    public String toString() {
        return "LogFilter{" +
                "minLevel=" + minLevel +
                ", maxLevel=" + maxLevel +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", sources=" + sources +
                ", applications=" + applications +
                ", hostname='" + hostname + '\'' +
                ", includeErrors=" + includeErrors +
                ", includeWarnings=" + includeWarnings +
                '}';
    }
}
