package com.smartlab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.TestSupport;
import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.dto.DeviceStatusDTO;
import com.smartlab.entity.Device;
import com.smartlab.entity.Lab;
import com.smartlab.enums.DeviceStatus;
import com.smartlab.exception.BusinessException;
import com.smartlab.service.BorrowService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {

    @Mock
    private DeviceService deviceService;
    @Mock
    private OperationLogService logService;
    @Mock
    private BorrowService borrowService;
    @Mock
    private RepairService repairService;
    @Mock
    private UserService userService;
    @Mock
    private LabService labService;

    @InjectMocks
    private DeviceController deviceController;

    private static final long DEVICE_ID = 100L;

    @BeforeEach
    void setUp() {
        TestSupport.loginAsAdmin(9L);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clear();
    }

    private Device idleDevice() {
        Device d = new Device();
        d.setId(DEVICE_ID);
        d.setName("示波器");
        d.setStatus(DeviceStatus.IDLE.code);
        return d;
    }

    @Test
    void list_returnsPage() {
        Page<Device> p = new Page<>(1, 10);
        when(deviceService.page(any(), any())).thenReturn(p);
        Result<PageResult<Device>> result = deviceController.list(1, 10, null, null, null, null);
        assertNotNull(result.getData());
    }

    @Test
    void add_student_noPermission_throws() {
        TestSupport.loginAsStudent(1L);
        Device d = new Device();
        assertThrows(BusinessException.class, () -> deviceController.add(d));
    }

    @Test
    void add_admin_setsDefaultStatusAndSaves() {
        when(deviceService.save(any(Device.class))).thenReturn(true);
        Device d = new Device();
        d.setName("设备1");
        Result<Void> result = deviceController.add(d);
        assertEquals(200, result.getCode());
        assertEquals(DeviceStatus.IDLE.code, d.getStatus());
        verify(deviceService).save(d);
    }

    @Test
    void update_deviceNotExists_throws() {
        when(deviceService.getById(DEVICE_ID)).thenReturn(null);
        Device d = new Device();
        assertThrows(BusinessException.class, () -> deviceController.update(DEVICE_ID, d));
    }

    @Test
    void update_targetLabNotExists_throws() {
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(labService.getById(999L)).thenReturn(null);
        Device d = new Device();
        d.setLabId(999L);
        assertThrows(BusinessException.class, () -> deviceController.update(DEVICE_ID, d));
    }

    @Test
    void delete_deviceInUse_throws() {
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(borrowService.count(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> deviceController.delete(DEVICE_ID));
    }

    @Test
    void delete_success_removes() {
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(borrowService.count(any())).thenReturn(0L);
        when(repairService.count(any())).thenReturn(0L);
        when(deviceService.removeById(DEVICE_ID)).thenReturn(true);
        Result<Void> result = deviceController.delete(DEVICE_ID);
        assertEquals(200, result.getCode());
    }

    @Test
    void updateStatus_directScrapNotAllowedForNonAdminFlow_throwsWhenForbiddenStatus() {
        // 直接设置 DISCARD_PENDING_ADMIN 应被拦截
        DeviceStatusDTO dto = new DeviceStatusDTO();
        dto.setStatus(DeviceStatus.DISCARD_PENDING_ADMIN.code);
        // 报废状态只能走专门报废流程，直接设置应抛异常
        assertThrows(BusinessException.class, () -> deviceController.updateStatus(DEVICE_ID, dto));
    }

    @Test
    void submitDiscard_adminSetsScrapped() {
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(deviceService.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(true);
        Result<Void> result = deviceController.submitDiscard(DEVICE_ID, java.util.Map.of("reason", "损坏"));
        assertEquals(200, result.getCode());
        verify(deviceService).update(eq(null), any(LambdaUpdateWrapper.class));
    }

    @Test
    void submitDiscard_nonAdmin_setsPendingAdmin() {
        TestSupport.loginAsLabAdmin(9L);
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(deviceService.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(true);
        Result<Void> result = deviceController.submitDiscard(DEVICE_ID, java.util.Map.of());
        assertEquals(200, result.getCode());
        verify(deviceService).update(eq(null), any(LambdaUpdateWrapper.class));
    }

    @Test
    void submitDiscard_notIdle_throws() {
        Device d = idleDevice();
        d.setStatus(DeviceStatus.BORROWED.code);
        when(deviceService.getById(DEVICE_ID)).thenReturn(d);
        assertThrows(BusinessException.class, () -> deviceController.submitDiscard(DEVICE_ID, java.util.Map.of()));
    }

    @Test
    void approveDiscard_nonAdmin_throws() {
        TestSupport.loginAsLabAdmin(9L);
        assertThrows(BusinessException.class, () -> deviceController.approveDiscard(DEVICE_ID));
    }

    @Test
    void approveDiscard_admin_success() {
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(deviceService.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(true);
        Result<Void> result = deviceController.approveDiscard(DEVICE_ID);
        assertEquals(200, result.getCode());
    }

    @Test
    void finalApproveDiscard_admin_cancelsPendingBorrows() {
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(deviceService.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(true);
        when(borrowService.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(true);
        when(repairService.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(true);

        Result<Void> result = deviceController.finalApproveDiscard(DEVICE_ID);

        assertEquals(200, result.getCode());
        verify(borrowService).update(any(), any(LambdaUpdateWrapper.class));
        verify(repairService).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void rejectDiscard_student_throws() {
        TestSupport.loginAsStudent(1L);
        assertThrows(BusinessException.class, () -> deviceController.rejectDiscard(DEVICE_ID));
    }
}