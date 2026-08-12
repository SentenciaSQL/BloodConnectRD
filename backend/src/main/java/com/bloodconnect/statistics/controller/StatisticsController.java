package com.bloodconnect.statistics.controller;

import com.bloodconnect.statistics.dto.DashboardStatisticsResponse;
import com.bloodconnect.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatisticsResponse> dashboard() {
        return ResponseEntity.ok(statisticsService.dashboard());
    }
}
