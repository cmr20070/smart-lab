package com.smartlab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.TestSupport;
import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.dto.ApproveDTO;
import com.smartlab.dto.BorrowDTO;
import com.smartlab.entity.Borrow;
import com.smartlab.entity.Device;
import com.smartlab.entity.User;
import com.smartlab.enums.BorrowStatus;
import com.smartlab.exception.BusinessException;
import com.smartlab.service.BorrowService;
import com.smartlab.service.DeviceService;
import com.smartlab.service.LabService;
import com.smartlab.service.OperationLogService;
import com.smartlab.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowControllerTest {

    @Mock
    private BorrowService borrowService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private OperationLogService logService;
    @Mock
    private LabService labService;
    @Mock
    private UserService userService;

    @InjectMocks
    private BorrowController borrowController;

    private static final long DEVICE_ID = 100L;

    @BeforeEach
    void setUp() {
        TestSupport.loginAsStudent(1L);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clear();
    }

    @Test
    void apply_callsServiceAndLogs() {
        BorrowDTO dto = new BorrowDTO();
        dto.setDeviceId(DEVICE_ID);
        Device d = new Device();
        d.setId(DEVICE_ID);
        d.setName("投影仪");
        when(deviceService.getById(DEVICE_ID)).thenReturn(d);

        Result<Void> result = borrowController.apply(dto);

        assertEquals(200, result.getCode());
        verify(borrowService).apply(dto);
        verify(logService).log(eq("borrow"), eq("apply"), any(String.class), eq(DEVICE_ID));
    }

    @Test
    void batchApply_loopsApplyAndLogs() {
        BorrowDTO d1 = new BorrowDTO();
        d1.setDeviceId(1L);
        BorrowDTO d2 = new BorrowDTO();
        d2.setDeviceId(2L);
        Result<Void> result = borrowController.batchApply(List.of(d1, d2));
        assertEquals(200, result.getCode());
        verify(borrowService, times(2)).apply(any(BorrowDTO.class));
    }

    @Test
    void approve_callsServiceAndLogs() {
        ApproveDTO dto = new ApproveDTO();
        dto.setBorrowId(5L);
        dto.setStatus(BorrowStatus.APPROVED.code);
        Result<Void> result = borrowController.approve(dto);
        assertEquals(200, result.getCode());
        verify(borrowService).approve(dto);
    }

    @Test
    void returnDevice_callsServiceAndLogs() {
        Borrow b = new Borrow();
        b.setId(5L);
        b.setDeviceId(DEVICE_ID);
        when(borrowService.getById(5L)).thenReturn(b);
        Device d = new Device();
        d.setName("设备x");
        when(deviceService.getById(DEVICE_ID)).thenReturn(d);

        Result<Void> result = borrowController.returnDevice(5L);
        assertEquals(200, result.getCode());
        verify(borrowService).returnDevice(5L);
    }

    @Test
    void cancel_callsService() {
        Borrow b = new Borrow();
        b.setId(5L);
        b.setDeviceId(DEVICE_ID);
        when(borrowService.getById(5L)).thenReturn(b);
        Result<Void> result = borrowController.cancel(5L);
        assertEquals(200, result.getCode());
        verify(borrowService).cancelBorrow(5L);
    }

    @Test
    void delete_notAdmin_throws() {
        Result<Void> result = null;
        try {
            result = borrowController.delete(5L);
            fail("应抛出无权限异常");
        } catch (BusinessException e) {
            assertTrue(e.getMessage().contains("管理员"));
        }
        assertNull(result);
    }

    @Test
    void delete_admin_success() {
        TestSupport.loginAsAdmin(9L);
        Borrow b = new Borrow();
        b.setId(5L);
        b.setDeviceId(DEVICE_ID);
        when(borrowService.getById(5L)).thenReturn(b);
        when(borrowService.removeById(5L)).thenReturn(true);

        Result<Void> result = borrowController.delete(5L);
        assertEquals(200, result.getCode());
        verify(borrowService).removeById(5L);
    }

    @Test
    void remindOverdue_notOverdue_throws() {
        TestSupport.loginAsAdmin(9L);
        Borrow b = new Borrow();
        b.setId(5L);
        b.setDeviceId(DEVICE_ID);
        b.setUserId(1L);
        b.setStatus(BorrowStatus.APPROVED.code);
        b.setExpectReturnTime(LocalDateTime.now().plusDays(1));
        when(borrowService.getById(5L)).thenReturn(b);

        assertThrows(BusinessException.class, () -> borrowController.remindOverdue(5L));
    }

    @Test
    void remindOverdue_success_updatesStatus() {
        TestSupport.loginAsAdmin(9L);
        Borrow b = new Borrow();
        b.setId(5L);
        b.setDeviceId(DEVICE_ID);
        b.setUserId(1L);
        b.setStatus(BorrowStatus.APPROVED.code);
        b.setExpectReturnTime(LocalDateTime.now().minusDays(1));
        when(borrowService.getById(5L)).thenReturn(b);
        Device d = new Device();
        d.setName("投影仪");
        when(deviceService.getById(DEVICE_ID)).thenReturn(d);

        Result<Void> result = borrowController.remindOverdue(5L);
        assertEquals(200, result.getCode());
        assertEquals(BorrowStatus.OVERDUE.code, b.getStatus());
        verify(borrowService).updateById(b);
    }

    @Test
    void list_delegatesToBorrowService() {
        when(borrowService.page(any(), any())).thenReturn(new Page<>(1, 10));
        Result<PageResult<Borrow>> result = borrowController.list(1, 10, null, null, null);
        assertEquals(200, result.getCode());
    }
}