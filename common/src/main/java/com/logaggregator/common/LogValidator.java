/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 */

package com.logaggregator.common;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Provides validation and sanitization capabilities for log entries and sources.
 * This class ensures data integrity and security in the log aggregation system.
 */
public class LogValidator {

    // Security patterns to detect potentially malicious content
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|</script|javascript:|vbscript:|onload|onerror|onclick)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(?i)(\\||&&|;|`|\\$\\(|\\${)",
        Pattern.CASE_INSENSITIVE
    );

    // Validation constants
    private static final int MAX_MESSAGE_LENGTH = 10000;
    private static final int MAX_SOURCE_NAME_LENGTH = 255;
    private static final int MAX_APPLICATION_NAME_LENGTH = 255;
    private static final int MAX_HOSTNAME_LENGTH = 255;
    private static final int MAX_METADATA_VALUE_LENGTH = 1000;

    /**
     * Represents validation results for a log entry
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final List<String> errors;
        private final List<String> warnings;
        private final LogEntry sanitizedEntry;

        public ValidationResult(boolean isValid, List<String> errors, List<String> warnings, LogEntry sanitizedEntry) {
            this.isValid = isValid;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
            this.sanitizedEntry = sanitizedEntry;
        }

        public boolean isValid() { return isValid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public LogEntry getSanitizedEntry() { return sanitizedEntry; }

        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public boolean hasErrors() { return !errors.isEmpty(); }

        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, errors=%d, warnings=%d}",
                                isValid, errors.size(), warnings.size());
        }
    }

    /**
     * Validate and sanitize a log entry
     */
    public static ValidationResult validateLogEntry(LogEntry entry) {
        if (entry == null) {
            return new ValidationResult(false, List.of("Log entry cannot be null"), new ArrayList<>(), null);
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        LogEntry sanitizedEntry = entry.copy();

        // Validate and sanitize message
        validateMessage(sanitizedEntry, errors, warnings);

        // Validate and sanitize source
        validateSource(sanitizedEntry, errors, warnings);

        // Validate timestamp
        validateTimestamp(sanitizedEntry, errors, warnings);

        // Validate level
        validateLevel(sanitizedEntry, errors, warnings);

        // Validate optional fields
        validateOptionalFields(sanitizedEntry, errors, warnings);

        // Security validation
        performSecurityValidation(sanitizedEntry, errors, warnings);

        // Sanitize metadata
        sanitizeMetadata(sanitizedEntry, warnings);

        boolean isValid = errors.isEmpty();
        return new ValidationResult(isValid, errors, warnings, sanitizedEntry);
    }

    /**
     * Validate and sanitize a log source
     */
    public static ValidationResult validateLogSource(LogSource source) {
        if (source == null) {
            return new ValidationResult(false, List.of("Log source cannot be null"), new ArrayList<>(), null);
        }

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Validate required fields
        if (source.getFileName() == null || source.getFileName().trim().isEmpty()) {
            errors.add("File name is required");
        }

        if (source.getFilePath() == null || source.getFilePath().trim().isEmpty()) {
            errors.add("File path is required");
        }

        if (source.getSourceId() == null || source.getSourceId().trim().isEmpty()) {
            errors.add("Source ID is required");
        }

        // Validate field lengths
        if (source.getSourceName() != null && source.getSourceName().length() > MAX_SOURCE_NAME_LENGTH) {
            errors.add("Source name exceeds maximum length of " + MAX_SOURCE_NAME_LENGTH);
        }

        // Validate file extension
        if (source.getFileName() != null && !source.isLogFile()) {
            warnings.add("File does not have a recognized log file extension");
        }

        // Validate file size
        if (source.getFileSize() < 0) {
            errors.add("File size cannot be negative");
        }

        boolean isValid = errors.isEmpty();
        return new ValidationResult(isValid, errors, warnings, null);
    }

    /**
     * Sanitize a string by removing potentially dangerous content
     */
    public static String sanitizeString(String input) {
        if (input == null) {
            return null;
        }

        String sanitized = input;

        // Remove null characters
        sanitized = sanitized.replace("\0", "");

        // Remove control characters except newline, tab, and carriage return
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

        // Limit length
        if (sanitized.length() > MAX_MESSAGE_LENGTH) {
            sanitized = sanitized.substring(0, MAX_MESSAGE_LENGTH) + "... [truncated]";
        }

        return sanitized.trim();
    }

    /**
     * Check if a string contains potentially malicious content
     */
    public static boolean containsMaliciousContent(String input) {
        if (input == null) {
            return false;
        }

        return SQL_INJECTION_PATTERN.matcher(input).find() ||
               XSS_PATTERN.matcher(input).find() ||
               COMMAND_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Validate that a timestamp is reasonable
     */
    public static boolean isValidTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneYearAgo = now.minusYears(1);
        LocalDateTime oneHourFuture = now.plusHours(1);

        return timestamp.isAfter(oneYearAgo) && timestamp.isBefore(oneHourFuture);
    }

    // Private helper methods

    private static void validateMessage(LogEntry entry, List<String> errors, List<String> warnings) {
        String message = entry.getMessage();

        if (message == null || message.trim().isEmpty()) {
            errors.add("Log message cannot be null or empty");
            return;
        }

        if (message.length() > MAX_MESSAGE_LENGTH) {
            warnings.add("Message length exceeds recommended maximum, will be truncated");
        }

        // Sanitize message
        String sanitized = sanitizeString(message);
        entry.setMessage(sanitized);

        if (!message.equals(sanitized)) {
            warnings.add("Message was sanitized to remove potentially harmful content");
        }
    }

    private static void validateSource(LogEntry entry, List<String> errors, List<String> warnings) {
        String source = entry.getSource();

        if (source == null || source.trim().isEmpty()) {
            errors.add("Log source cannot be null or empty");
            return;
        }

        if (source.length() > MAX_SOURCE_NAME_LENGTH) {
            errors.add("Source name exceeds maximum length of " + MAX_SOURCE_NAME_LENGTH);
        }

        // Sanitize source
        String sanitized = sanitizeString(source);
        entry.setSource(sanitized);
    }

    private static void validateTimestamp(LogEntry entry, List<String> errors, List<String> warnings) {
        LocalDateTime timestamp = entry.getTimestamp();

        if (timestamp == null) {
            warnings.add("Timestamp is null, using current time");
            entry.setTimestamp(LocalDateTime.now());
            return;
        }

        if (!isValidTimestamp(timestamp)) {
            warnings.add("Timestamp appears to be invalid or unreasonable");
        }
    }

    private static void validateLevel(LogEntry entry, List<String> errors, List<String> warnings) {
        LogLevel level = entry.getLevel();

        if (level == null) {
            warnings.add("Log level is null, defaulting to INFO");
            entry.setLevel(LogLevel.INFO);
        }
    }

    private static void validateOptionalFields(LogEntry entry, List<String> errors, List<String> warnings) {
        // Validate hostname
        if (entry.getHostname() != null && entry.getHostname().length() > MAX_HOSTNAME_LENGTH) {
            warnings.add("Hostname exceeds maximum length, will be truncated");
            entry.setHostname(entry.getHostname().substring(0, MAX_HOSTNAME_LENGTH));
        }

        // Validate application name
        if (entry.getApplication() != null && entry.getApplication().length() > MAX_APPLICATION_NAME_LENGTH) {
            warnings.add("Application name exceeds maximum length, will be truncated");
            entry.setApplication(entry.getApplication().substring(0, MAX_APPLICATION_NAME_LENGTH));
        }

        // Sanitize optional string fields
        if (entry.getHostname() != null) {
            entry.setHostname(sanitizeString(entry.getHostname()));
        }

        if (entry.getApplication() != null) {
            entry.setApplication(sanitizeString(entry.getApplication()));
        }

        if (entry.getThread() != null) {
            entry.setThread(sanitizeString(entry.getThread()));
        }

        if (entry.getLoggerName() != null) {
            entry.setLoggerName(sanitizeString(entry.getLoggerName()));
        }
    }

    private static void performSecurityValidation(LogEntry entry, List<String> errors, List<String> warnings) {
        // Check message for malicious content
        if (entry.getMessage() != null && containsMaliciousContent(entry.getMessage())) {
            warnings.add("Message contains potentially malicious content");
        }

        // Check other string fields
        String[] fields = {entry.getSource(), entry.getHostname(), entry.getApplication(),
                          entry.getThread(), entry.getLoggerName()};

        for (String field : fields) {
            if (field != null && containsMaliciousContent(field)) {
                warnings.add("Field contains potentially malicious content: " + field.substring(0, Math.min(50, field.length())));
            }
        }
    }

    private static void sanitizeMetadata(LogEntry entry, List<String> warnings) {
        if (entry.getMetadata() != null) {
            entry.getMetadata().entrySet().removeIf(metadataEntry -> {
                Object value = metadataEntry.getValue();
                if (value instanceof String) {
                    String stringValue = (String) value;
                    if (stringValue.length() > MAX_METADATA_VALUE_LENGTH) {
                        warnings.add("Metadata value truncated for key: " + metadataEntry.getKey());
                        metadataEntry.setValue(stringValue.substring(0, MAX_METADATA_VALUE_LENGTH) + "... [truncated]");
                    }

                    if (containsMaliciousContent(stringValue)) {
                        warnings.add("Malicious content detected in metadata key: " + metadataEntry.getKey());
                        return true; // Remove this entry
                    }
                }
                return false;
            });
        }
    }
}
