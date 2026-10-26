package com.smartlab.controller;

import com.smartlab.common.Result;
import com.smartlab.service.StatisticsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/device-status")
    public Result<Map<String, Object>> deviceStatus() {
        return Result.success(statisticsService.getDeviceStatusStats());
    }

    @GetMapping("/borrow")
    public Result<Map<String, Object>> borrowStats() {
        return Result.success(statisticsService.getBorrowStats());
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(statisticsService.getDashboardStats());
    }
}
