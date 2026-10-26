package com.smartlab.controller;

import com.smartlab.common.Result;
import com.smartlab.service.StatisticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsControllerTest {

    @Mock
    private StatisticsService statisticsService;

    @InjectMocks
    private StatisticsController statisticsController;

    @Test
    void deviceStatus_delegates() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", 5);
        when(statisticsService.getDeviceStatusStats()).thenReturn(data);
        Result<Map<String, Object>> result = statisticsController.deviceStatus();
        assertEquals(5, result.getData().get("total"));
    }

    @Test
    void borrowStats_delegates() {
        when(statisticsService.getBorrowStats()).thenReturn(new HashMap<>());
        Result<Map<String, Object>> result = statisticsController.borrowStats();
        assertEquals(200, result.getCode());
        verify(statisticsService).getBorrowStats();
    }

    @Test
    void dashboard_delegates() {
        when(statisticsService.getDashboardStats()).thenReturn(new HashMap<>());
        Result<Map<String, Object>> result = statisticsController.dashboard();
        assertEquals(200, result.getCode());
        verify(statisticsService).getDashboardStats();
    }
}