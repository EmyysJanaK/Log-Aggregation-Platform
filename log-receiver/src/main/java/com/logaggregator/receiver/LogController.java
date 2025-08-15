package com.logaggregator.receiver;

import com.logaggregator.common.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogRepository logRepository;

    public LogController(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping
    public Page<LogEntry> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return logRepository.findAll(PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public LogEntry getLogById(@PathVariable String id) {
        return logRepository.findById(id)
                .orElseThrow(() -> new LogNotFoundException("Log not found with id: " + id));
    }
}
