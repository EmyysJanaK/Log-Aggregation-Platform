package com.logaggregator.receiver;

import com.logaggregator.common.LogEntry;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogRepository extends ElasticsearchRepository<LogEntry, String> {
    // Spring Data Elasticsearch provides basic CRUD operations.
    // You can add custom query methods here if needed later.
}