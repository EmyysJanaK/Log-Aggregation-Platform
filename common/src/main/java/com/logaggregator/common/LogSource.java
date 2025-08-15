/*
 *
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 * Log Aggregator is distributed in the hope that it will be useful,
 *
* */

package com.logaggregator.common;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a log source in the log aggregation system.
 * This class encapsulates information about where logs originate from,
 * including file details, source identification, and metadata.
 */
public class LogSource {
    private String fileName;
    private String filePath;
    private String sourceType;
    private String sourceId;
    private String sourceName;
    private LocalDateTime lastAccessTime;
    private LocalDateTime createdTime;
    private boolean isActive;
    private long fileSize;

    /**
     * Constructor with basic source information
     */
    public LogSource(String fileName, String filePath, String sourceType, String sourceId, String sourceName) {
        this.fileName = validateString(fileName, "fileName");
        this.filePath = validateString(filePath, "filePath");
        this.sourceType = validateString(sourceType, "sourceType");
        this.sourceId = validateString(sourceId, "sourceId");
        this.sourceName = validateString(sourceName, "sourceName");
        this.createdTime = LocalDateTime.now();
        this.lastAccessTime = LocalDateTime.now();
        this.isActive = true;
        this.fileSize = 0L;
    }

    /**
     * Constructor with all fields
     */
    public LogSource(String fileName, String filePath, String sourceType, String sourceId,
                    String sourceName, long fileSize, boolean isActive) {
        this(fileName, filePath, sourceType, sourceId, sourceName);
        this.fileSize = fileSize;
        this.isActive = isActive;
    }

    // Getter methods
    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public LocalDateTime getLastAccessTime() {
        return lastAccessTime;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public boolean isActive() {
        return isActive;
    }

    public long getFileSize() {
        return fileSize;
    }

    // Setter methods
    public void setFileName(String fileName) {
        this.fileName = validateString(fileName, "fileName");
    }

    public void setFilePath(String filePath) {
        this.filePath = validateString(filePath, "filePath");
    }

    public void setSourceType(String sourceType) {
        this.sourceType = validateString(sourceType, "sourceType");
    }

    public void setSourceId(String sourceId) {
        this.sourceId = validateString(sourceId, "sourceId");
    }

    public void setSourceName(String sourceName) {
        this.sourceName = validateString(sourceName, "sourceName");
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = Math.max(0, fileSize);
    }

    // Utility methods

    /**
     * Updates the last access time to current time
     */
    public void updateLastAccessTime() {
        this.lastAccessTime = LocalDateTime.now();
    }

    /**
     * Gets the full path including file name
     */
    public String getFullPath() {
        if (filePath.endsWith("/") || filePath.endsWith("\\")) {
            return filePath + fileName;
        }
        return filePath + "/" + fileName;
    }

    /**
     * Gets the file extension from the filename
     */
    public String getFileExtension() {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * Checks if this is a log file based on common log file extensions
     */
    public boolean isLogFile() {
        String extension = getFileExtension().toLowerCase();
        return extension.equals("log") || extension.equals("txt") ||
               extension.equals("out") || extension.equals("err");
    }

    /**
     * Creates a unique identifier for this log source
     */
    public String getUniqueIdentifier() {
        return sourceType + ":" + sourceId + ":" + fileName;
    }

    /**
     * Validates that a string parameter is not null or empty
     */
    private String validateString(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LogSource logSource = (LogSource) obj;
        return Objects.equals(sourceId, logSource.sourceId) &&
               Objects.equals(filePath, logSource.filePath) &&
               Objects.equals(fileName, logSource.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId, filePath, fileName);
    }

    @Override
    public String toString() {
        return "LogSource{" +
                "fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", sourceType='" + sourceType + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", sourceName='" + sourceName + '\'' +
                ", isActive=" + isActive +
                ", fileSize=" + fileSize +
                ", createdTime=" + createdTime +
                '}';
    }
}

