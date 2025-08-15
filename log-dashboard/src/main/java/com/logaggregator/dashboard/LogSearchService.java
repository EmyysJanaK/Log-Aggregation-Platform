package com.logaggregator.dashboard;

import com.logaggregator.common.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class LogSearchService {

    private final LogSearchRepository logSearchRepository;

    public LogSearchService(LogSearchRepository logSearchRepository) {
        this.logSearchRepository = logSearchRepository;
    }

    public Page<LogEntry> getRecentLogs(PageRequest pageRequest) {
        PageRequest sortedPageRequest = PageRequest.of(
            pageRequest.getPageNumber(),
            pageRequest.getPageSize(),
            Sort.by(Sort.Direction.DESC, "timestamp")
        );
        return logSearchRepository.findAll(sortedPageRequest);
    }

    public Page<LogEntry> searchLogs(String query, String level, String source, PageRequest pageRequest) {
        if (query != null && !query.trim().isEmpty()) {
            return logSearchRepository.findByMessageContaining(query.trim(), pageRequest);
        }

        if (level != null && !level.trim().isEmpty()) {
            if (source != null && !source.trim().isEmpty()) {
                return logSearchRepository.findByLevelAndSource(level.trim(), source.trim(), pageRequest);
            }
            return logSearchRepository.findByLevel(level.trim(), pageRequest);
        }

        if (source != null && !source.trim().isEmpty()) {
            return logSearchRepository.findBySource(source.trim(), pageRequest);
        }

        return getRecentLogs(pageRequest);
    }

    public LogEntry getLogById(String id) {
        return logSearchRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Log not found with id: " + id));
    }

    public LogStats getLogStats() {
        long totalLogs = logSearchRepository.count();
        long errorLogs = logSearchRepository.countByLevel("ERROR");
        long warnLogs = logSearchRepository.countByLevel("WARN");
        long infoLogs = logSearchRepository.countByLevel("INFO");

        return new LogStats(totalLogs, errorLogs, warnLogs, infoLogs);
    }
}
