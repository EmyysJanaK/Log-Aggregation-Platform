/*
 * this file is part of Log Aggregator.
 * Log Aggregator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *
 */

package com.logaggregator.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Log Agent.
 * This agent is responsible for collecting logs from various sources
 * and sending them to the log aggregation platform via Kafka.
 */
@SpringBootApplication
@EnableScheduling
public class LogAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogAgentApplication.class, args);
    }
}
