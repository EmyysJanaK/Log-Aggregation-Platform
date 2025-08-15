package com.logaggregator.dashboard;

import com.logaggregator.common.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogSearchRepository extends ElasticsearchRepository<LogEntry, String> {

    Page<LogEntry> findByMessageContaining(String message, Pageable pageable);

    Page<LogEntry> findByLevel(String level, Pageable pageable);

    Page<LogEntry> findBySource(String source, Pageable pageable);

    Page<LogEntry> findByLevelAndSource(String level, String source, Pageable pageable);

    long countByLevel(String level);
}
