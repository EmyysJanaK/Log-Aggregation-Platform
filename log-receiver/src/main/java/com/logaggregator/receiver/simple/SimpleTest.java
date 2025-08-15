package com.logaggregator.receiver.simple;

/**
 * Simple test class to verify basic Java compilation works
 */
public class SimpleTest {
    public static void main(String[] args) {
        System.out.println("Testing SimpleLogEntry creation...");
        
        try {
            SimpleLogEntry logEntry = new SimpleLogEntry("test-service", "INFO", "Test message");
            System.out.println("Created log entry: " + logEntry.toString());
            System.out.println("ID: " + logEntry.getId());
            System.out.println("Source: " + logEntry.getSource());
            System.out.println("Level: " + logEntry.getLevel());
            System.out.println("Message: " + logEntry.getMessage());
            System.out.println("Timestamp: " + logEntry.getTimestamp());
            
            System.out.println("✅ SimpleLogEntry is working correctly!");
            
            // Test null validation
            try {
                new SimpleLogEntry(null, "INFO", "message");
                System.out.println("❌ Null validation failed");
            } catch (IllegalArgumentException e) {
                System.out.println("✅ Null validation working: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error creating SimpleLogEntry: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
