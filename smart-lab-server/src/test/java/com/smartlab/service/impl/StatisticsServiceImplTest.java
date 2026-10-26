package com.smartlab.service.impl;

import com.smartlab.entity.Borrow;
import com.smartlab.entity.Device;
import com.smartlab.entity.Lab;
import com.smartlab.entity.Repair;
import com.smartlab.entity.User;
import com.smartlab.enums.BorrowStatus;
import com.smartlab.enums.DeviceStatus;
import com.smartlab.enums.RepairStatus;
import com.smartlab.service.BorrowService;
import com.smartlab.service.DeviceService;
import com.smartlab.service.LabService;
import com.smartlab.service.RepairService;
import com.smartlab.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock
    private DeviceService deviceService;
    @Mock
    private LabService labService;
    @Mock
    private BorrowService borrowService;
    @Mock
    private RepairService repairService;
    @Mock
    private UserService userService;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @Test
    void getDeviceStatusStats_groupsByStatusAndCategory() {
        Device idle = new Device();
        idle.setId(1L);
        idle.setStatus(DeviceStatus.IDLE.code);
        idle.setCategory("电脑");
        Device borrowed = new Device();
        borrowed.setId(2L);
        borrowed.setStatus(DeviceStatus.BORROWED.code);
        borrowed.setCategory("电脑");
        Device scrapped = new Device();
        scrapped.setId(3L);
        scrapped.setStatus(DeviceStatus.SCRAPPED.code);
        scrapped.setCategory(null);

        when(deviceService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(idle, borrowed, scrapped));

        Map<String, Object> result = statisticsService.getDeviceStatusStats();

        assertEquals(3, result.get("total"));
        assertEquals(1L, result.get("idle"));
        assertEquals(1L, result.get("borrowed"));
        assertEquals(1L, result.get("scrapped"));
        Map<String, Long> cat = (Map<String, Long>) result.get("categoryDistribution");
        assertEquals(2L, cat.get("电脑"));
        assertEquals(1L, cat.get("其他"));
    }

    @Test
    void getBorrowStats_computesStatusCounts() {
        Borrow b = new Borrow();
        b.setDeviceId(1L);
        b.setStatus(BorrowStatus.PENDING.code);
        when(borrowService.list()).thenReturn(List.of(b, b));
        Device d = new Device();
        d.setId(1L);
        d.setName("投影仪");
        when(deviceService.listByIds(any())).thenReturn(List.of(d));

        Map<String, Object> result = statisticsService.getBorrowStats();

        assertEquals(2, result.get("total"));
        assertEquals(2L, result.get("pending"));
        List<Map<String, Object>> top = (List<Map<String, Object>>) result.get("topDevices");
        assertEquals("投影仪", top.get(0).get("name"));
        assertEquals(2L, top.get(0).get("count"));
    }

    @Test
    void getDashboardStats_computesCounts() {
        when(userService.count()).thenReturn(10L);
        when(labService.count()).thenReturn(3L);
        when(deviceService.count()).thenReturn(5L);
        when(borrowService.count(any())).thenReturn(2L);
        when(repairService.count(any())).thenReturn(1L);

        Map<String, Object> result = statisticsService.getDashboardStats();

        assertEquals(10L, result.get("userCount"));
        assertEquals(3L, result.get("labCount"));
        assertEquals(5L, result.get("deviceCount"));
        assertEquals(2L, result.get("pendingBorrow"));
        assertEquals(1L, result.get("pendingRepair"));
        assertEquals(3L, result.get("pendingTotal"));
    }

    @Test
    void getDashboardStats_zeroDevice_utilizationIsZero() {
        when(userService.count()).thenReturn(0L);
        when(labService.count()).thenReturn(0L);
        when(deviceService.count()).thenReturn(0L);
        when(borrowService.count(any())).thenReturn(0L);
        when(repairService.count(any())).thenReturn(0L);

        Map<String, Object> result = statisticsService.getDashboardStats();

        assertEquals(0L, result.get("overdueCount"));
        assertEquals(0L, result.get("utilization"));
    }
}