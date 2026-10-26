package com.smartlab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.TestSupport;
import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.dto.RepairDTO;
import com.smartlab.entity.Device;
import com.smartlab.entity.Repair;
import com.smartlab.enums.RepairStatus;
import com.smartlab.exception.BusinessException;
import com.smartlab.service.DeviceService;
import com.smartlab.service.LabService;
import com.smartlab.service.OperationLogService;
import com.smartlab.service.RepairService;
import com.smartlab.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairControllerTest {

    @Mock
    private RepairService repairService;
    @Mock
    private OperationLogService logService;
    @Mock
    private LabService labService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private UserService userService;

    @InjectMocks
    private RepairController repairController;

    private static final long DEVICE_ID = 1L;
    private static final long REPAIR_ID = 10L;

    @BeforeEach
    void setUp() {
        TestSupport.loginAsAdmin(9L);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clear();
    }

    @Test
    void list_returnsPageResult() {
        when(repairService.page(any(), any())).thenReturn(new Page<>(1, 10));
        Result<PageResult<Repair>> result = repairController.list(1, 10, null, null, null);
        assertEquals(200, result.getCode());
        verify(repairService).page(any(), any());
    }

    @Test
    void list_studentFiltersByReporter() {
        TestSupport.loginAsStudent(1L);
        when(repairService.page(any(), any())).thenReturn(new Page<>(1, 10));
        Result<PageResult<Repair>> result = repairController.list(1, 10, 0, null, "keyword");
        assertEquals(200, result.getCode());
    }

    @Test
    void report_callsServiceAndLogs() {
        RepairDTO dto = new RepairDTO();
        dto.setDeviceId(DEVICE_ID);
        dto.setFaultDescription("黑屏");
        dto.setFaultLevel(2);

        Result<Void> result = repairController.report(dto);

        assertEquals(200, result.getCode());
        verify(repairService).reportFault(dto);
        verify(logService).log(eq("repair"), eq("report"), any(String.class), eq(DEVICE_ID));
    }

    @Test
    void assign_callsServiceAndLogs() {
        Repair r = new Repair();
        r.setId(REPAIR_ID);
        r.setDeviceId(DEVICE_ID);
        when(repairService.getById(REPAIR_ID)).thenReturn(r);
        when(deviceService.getById(DEVICE_ID)).thenReturn(null);

        Result<Void> result = repairController.assign(REPAIR_ID);

        assertEquals(200, result.getCode());
        verify(repairService).assignRepair(REPAIR_ID);
    }

    @Test
    void complete_callsServiceAndLogs() {
        Repair r = new Repair();
        r.setId(REPAIR_ID);
        r.setDeviceId(DEVICE_ID);
        when(repairService.getById(REPAIR_ID)).thenReturn(r);
        when(deviceService.getById(DEVICE_ID)).thenReturn(null);

        Map<String, Object> body = Map.of("repairDescription", "更换屏幕", "repairCost", "150.5");
        Result<Void> result = repairController.complete(REPAIR_ID, body);

        assertEquals(200, result.getCode());
        verify(repairService).completeRepair(eq(REPAIR_ID), eq("更换屏幕"), eq(new BigDecimal("150.5")));
    }

    @Test
    void complete_noCost_usesZero() {
        Repair r = new Repair();
        r.setId(REPAIR_ID);
        when(repairService.getById(REPAIR_ID)).thenReturn(r);

        Result<Void> result = repairController.complete(REPAIR_ID, Map.of());

        assertEquals(200, result.getCode());
        verify(repairService).completeRepair(eq(REPAIR_ID), eq(""), eq(BigDecimal.ZERO));
    }

    @Test
    void delete_notExists_throws() {
        when(repairService.getById(REPAIR_ID)).thenReturn(null);
        assertThrows(BusinessException.class, () -> repairController.delete(REPAIR_ID));
    }

    @Test
    void delete_student_noPermission_throws() {
        TestSupport.loginAsStudent(1L);
        Repair r = new Repair();
        r.setId(REPAIR_ID);
        when(repairService.getById(REPAIR_ID)).thenReturn(r);
        assertThrows(BusinessException.class, () -> repairController.delete(REPAIR_ID));
    }

    @Test
    void delete_labAdminNotManager_throws() {
        TestSupport.loginAsLabAdmin(9L);
        Repair r = new Repair();
        r.setId(REPAIR_ID);
        r.setDeviceId(DEVICE_ID);
        when(repairService.getById(REPAIR_ID)).thenReturn(r);
        Device d = new Device();
        d.setLabId(5L);
        when(deviceService.getById(DEVICE_ID)).thenReturn(d);
        when(labService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        assertThrows(BusinessException.class, () -> repairController.delete(REPAIR_ID));
    }

    @Test
    void delete_admin_success() {
        Repair r = new Repair();
        r.setId(REPAIR_ID);
        r.setDeviceId(DEVICE_ID);
        when(repairService.getById(REPAIR_ID)).thenReturn(r);
        when(deviceService.getById(DEVICE_ID)).thenReturn(null);
        when(repairService.removeById(REPAIR_ID)).thenReturn(true);

        Result<Void> result = repairController.delete(REPAIR_ID);

        assertEquals(200, result.getCode());
        verify(repairService).removeById(REPAIR_ID);
        verify(logService).log(eq("repair"), eq("delete"), any(String.class), eq(REPAIR_ID));
    }
}