package org.example.controller;

import org.example.analytics.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getTrendAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        Map<String, Object> analysis = analyticsService.getExecutionTrends(fromDate, toDate);
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/regression/{environment}")
    public ResponseEntity<Map<String, Object>> getRegressionMetrics(
            @PathVariable String environment,
            @RequestParam(defaultValue = "30") int days) {

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        Map<String, Object> metrics = analyticsService.getPerformanceMetrics(startDate, endDate);
        return ResponseEntity.ok(metrics);
    }
}
