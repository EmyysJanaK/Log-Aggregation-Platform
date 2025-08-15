/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * purpose of this file is to define the LogLevel enum
* */

package com.logaggregator.common;

/**
 * Represents the severity levels for log entries in the log aggregation system.
 * Each level has a priority value and color coding for display purposes.
 */
public enum LogLevel {
    DEBUG("DEBUG", 10, "CYAN"),
    INFO("INFO", 20, "GREEN"),
    WARN("WARN", 30, "YELLOW"),
    ERROR("ERROR", 40, "RED"),
    FATAL("FATAL", 50, "MAGENTA");

    private final String level;
    private final int priority;
    private final String color;

    LogLevel(String level, int priority, String color) {
        this.level = level;
        this.priority = priority;
        this.color = color;
    }

    public String getLevel() {
        return level;
    }

    public int getPriority() {
        return priority;
    }

    public String getColor() {
        return color;
    }

    /**
     * Check if this level is more severe than another level
     */
    public boolean isMoreSevereThan(LogLevel other) {
        return this.priority > other.priority;
    }

    /**
     * Check if this level is less severe than another level
     */
    public boolean isLessSevereThan(LogLevel other) {
        return this.priority < other.priority;
    }

    /**
     * Check if this level is at least as severe as another level
     */
    public boolean isAtLeastAsSevereAs(LogLevel other) {
        return this.priority >= other.priority;
    }

    /**
     * Check if this is an error level (ERROR or FATAL)
     */
    public boolean isError() {
        return this == ERROR || this == FATAL;
    }

    /**
     * Check if this is a warning level
     */
    public boolean isWarning() {
        return this == WARN;
    }

    /**
     * Check if this is an informational level (INFO or DEBUG)
     */
    public boolean isInformational() {
        return this == INFO || this == DEBUG;
    }

    /**
     * Parse a string to LogLevel, case insensitive
     * Returns INFO if the string doesn't match any level
     */
    public static LogLevel fromString(String levelStr) {
        if (levelStr == null || levelStr.trim().isEmpty()) {
            return INFO;
        }

        try {
            return LogLevel.valueOf(levelStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try common aliases
            String normalized = levelStr.trim().toLowerCase();
            switch (normalized) {
                case "warning": return WARN;
                case "err": return ERROR;
                case "critical": return FATAL;
                case "trace": return DEBUG;
                case "information": return INFO;
                default: return INFO;
            }
        }
    }

    /**
     * Get all log levels that are at least as severe as the given level
     */
    public static LogLevel[] getLevelsAtLeast(LogLevel minLevel) {
        return java.util.Arrays.stream(values())
                .filter(level -> level.isAtLeastAsSevereAs(minLevel))
                .toArray(LogLevel[]::new);
    }

    /**
     * Get the default log level for filtering (INFO)
     */
    public static LogLevel getDefault() {
        return INFO;
    }

    /**
     * Get ANSI color code for console output
     */
    public String getAnsiColor() {
        switch (this) {
            case DEBUG: return "\u001B[36m"; // Cyan
            case INFO: return "\u001B[32m";  // Green
            case WARN: return "\u001B[33m";  // Yellow
            case ERROR: return "\u001B[31m"; // Red
            case FATAL: return "\u001B[35m"; // Magenta
            default: return "\u001B[0m";    // Reset
        }
    }

    /**
     * Get ANSI reset code
     */
    public static String getAnsiReset() {
        return "\u001B[0m";
    }

    /**
     * Format a message with color for console output
     */
    public String formatWithColor(String message) {
        return getAnsiColor() + "[" + this.level + "] " + message + getAnsiReset();
    }

    @Override
    public String toString() {
        return level;
    }
}

