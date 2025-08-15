package com.logaggregator.dashboard;

import com.logaggregator.common.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final LogSearchService logSearchService;

    public DashboardController(LogSearchService logSearchService) {
        this.logSearchService = logSearchService;
    }

    @GetMapping
    public String dashboard(Model model) {
        // Get recent logs for dashboard overview
        Page<LogEntry> recentLogs = logSearchService.getRecentLogs(PageRequest.of(0, 10));
        model.addAttribute("recentLogs", recentLogs);
        model.addAttribute("totalLogs", recentLogs.getTotalElements());
        return "dashboard";
    }

    @GetMapping("/search")
    public String searchPage() {
        return "search";
    }

    @PostMapping("/search")
    @ResponseBody
    public Page<LogEntry> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return logSearchService.searchLogs(query, level, source, PageRequest.of(page, size));
    }

    @GetMapping("/api/logs/{id}")
    @ResponseBody
    public LogEntry getLog(@PathVariable String id) {
        return logSearchService.getLogById(id);
    }

    @GetMapping("/api/stats")
    @ResponseBody
    public LogStats getStats() {
        return logSearchService.getLogStats();
    }
}
