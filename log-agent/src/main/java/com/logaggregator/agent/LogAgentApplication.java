package com.logaggregator.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // scheduling for periodic log generation
public class LogAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogAgentApplication.class, args);
    }
}