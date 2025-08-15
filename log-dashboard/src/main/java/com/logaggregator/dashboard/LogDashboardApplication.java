package com.logaggregator.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@EnableElasticsearchRepositories
public class LogDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogDashboardApplication.class, args);
    }
}
