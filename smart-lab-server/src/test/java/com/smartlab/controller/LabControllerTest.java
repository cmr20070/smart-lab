package com.smartlab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.TestSupport;
import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.entity.Device;
import com.smartlab.entity.Lab;
import com.smartlab.exception.BusinessException;
import com.smartlab.service.DeviceService;
import com.smartlab.service.LabService;
import com.smartlab.service.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabControllerTest {

    @Mock
    private LabService labService;
    @Mock
    private OperationLogService logService;
    @Mock
    private DeviceService deviceService;

    @InjectMocks
    private LabController labController;

    @BeforeEach
    void setUp() {
        TestSupport.loginAsAdmin(9L);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clear();
    }

    @Test
    void list_delegates() {
        when(labService.page(any(), any())).thenReturn(new Page<>(1, 10));
        Result<PageResult<Lab>> result = labController.list(1, 10, null, null);
        assertEquals(200, result.getCode());
        verify(labService).page(any(), any());
    }

    @Test
    void all_delegates() {
        when(labService.list()).thenReturn(java.util.List.of());
        Result<java.util.List<Lab>> result = labController.all();
        assertEquals(200, result.getCode());
        verify(labService).list();
    }

    @Test
    void add_savesAndLogs() {
        Lab lab = new Lab();
        lab.setName("物理实验室");
        Result<Void> result = labController.add(lab);
        assertEquals(200, result.getCode());
        verify(labService).save(lab);
        verify(logService).log(eq("lab"), eq("add"), contains("物理实验室"), any());
    }

    @Test
    void update_savesAndLogs() {
        Lab lab = new Lab();
        lab.setName("化学实验室");
        Result<Void> result = labController.update(5L, lab);
        assertEquals(200, result.getCode());
        verify(labService).updateById(lab);
        assertEquals(5L, lab.getId());
    }

    @Test
    void delete_notExists_throws() {
        when(labService.getById(5L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> labController.delete(5L));
    }

    @Test
    void delete_hasDevices_throws() {
        when(labService.getById(5L)).thenReturn(new Lab());
        when(deviceService.count(any(LambdaQueryWrapper.class))).thenReturn(2L);
        assertThrows(BusinessException.class, () -> labController.delete(5L));
    }

    @Test
    void delete_success_removes() {
        when(labService.getById(5L)).thenReturn(new Lab());
        when(deviceService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(labService.removeById(5L)).thenReturn(true);
        Result<Void> result = labController.delete(5L);
        assertEquals(200, result.getCode());
        verify(labService).removeById(5L);
    }
}