package com.logaggregator.dashboard;

/**
 * Represents statistics about log entries in the system.
 * Used by the dashboard to display summary information.
 */
public class LogStats {
    private long totalLogs;
    private long errorLogs;
    private long warnLogs;
    private long infoLogs;

    // Default constructor
    public LogStats() {}

    // Constructor with all fields
    public LogStats(long totalLogs, long errorLogs, long warnLogs, long infoLogs) {
        this.totalLogs = totalLogs;
        this.errorLogs = errorLogs;
        this.warnLogs = warnLogs;
        this.infoLogs = infoLogs;
    }

    // Getters and Setters
    public long getTotalLogs() {
        return totalLogs;
    }

    public void setTotalLogs(long totalLogs) {
        this.totalLogs = totalLogs;
    }

    public long getErrorLogs() {
        return errorLogs;
    }

    public void setErrorLogs(long errorLogs) {
        this.errorLogs = errorLogs;
    }

    public long getWarnLogs() {
        return warnLogs;
    }

    public void setWarnLogs(long warnLogs) {
        this.warnLogs = warnLogs;
    }

    public long getInfoLogs() {
        return infoLogs;
    }

    public void setInfoLogs(long infoLogs) {
        this.infoLogs = infoLogs;
    }

    // Utility methods
    public double getErrorPercentage() {
        return totalLogs > 0 ? (double) errorLogs / totalLogs * 100 : 0;
    }

    public double getWarnPercentage() {
        return totalLogs > 0 ? (double) warnLogs / totalLogs * 100 : 0;
    }

    public double getInfoPercentage() {
        return totalLogs > 0 ? (double) infoLogs / totalLogs * 100 : 0;
    }

    @Override
    public String toString() {
        return String.format("LogStats{totalLogs=%d, errorLogs=%d, warnLogs=%d, infoLogs=%d}", 
            totalLogs, errorLogs, warnLogs, infoLogs);
    }
}