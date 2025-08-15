package com.logaggregator.receiver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@EnableElasticsearchRepositories // Enable Spring Data Elasticsearch repositories
public class LogReceiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogReceiverApplication.class, args);
    }
}
