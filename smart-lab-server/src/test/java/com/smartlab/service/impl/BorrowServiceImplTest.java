package com.smartlab.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartlab.TestSupport;
import com.smartlab.dto.ApproveDTO;
import com.smartlab.dto.BorrowDTO;
import com.smartlab.entity.Borrow;
import com.smartlab.entity.Device;
import com.smartlab.entity.Lab;
import com.smartlab.entity.User;
import com.smartlab.enums.BorrowStatus;
import com.smartlab.enums.DeviceStatus;
import com.smartlab.exception.BusinessException;
import com.smartlab.mapper.BorrowMapper;
import com.smartlab.service.DeviceService;
import com.smartlab.service.LabService;
import com.smartlab.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowServiceImplTest {

    @Mock
    private DeviceService deviceService;
    @Mock
    private UserService userService;
    @Mock
    private LabService labService;
    @Mock
    private BorrowMapper borrowMapper;

    @InjectMocks
    private BorrowServiceImpl borrowService;

    private static final long USER_ID = 1L;
    private static final long DEVICE_ID = 10L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(borrowService, "baseMapper", borrowMapper);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clear();
    }

    private User studentUser() {
        User u = new User();
        u.setId(USER_ID);
        u.setRole("STUDENT");
        return u;
    }

    private User teacherUser() {
        User u = new User();
        u.setId(5L);
        u.setRole("TEACHER");
        return u;
    }

    private Device idleDevice() {
        Device d = new Device();
        d.setId(DEVICE_ID);
        d.setStatus(DeviceStatus.IDLE.code);
        return d;
    }

    private BorrowDTO validDto() {
        BorrowDTO dto = new BorrowDTO();
        dto.setDeviceId(DEVICE_ID);
        dto.setBorrowTime(LocalDateTime.now());
        dto.setExpectReturnTime(LocalDateTime.now().plusDays(3));
        dto.setPurpose("实验");
        return dto;
    }

    @Test
    void apply_success_createsPendingBorrow() {
        TestSupport.loginAsStudent(USER_ID);
        when(userService.getById(USER_ID)).thenReturn(studentUser());
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(borrowMapper.selectCount(any())).thenReturn(0L);

        borrowService.apply(validDto());

        verify(borrowMapper).insert(any(Borrow.class));
    }

    @Test
    void apply_deviceNotExists_throws() {
        TestSupport.loginAsStudent(USER_ID);
        when(deviceService.getById(DEVICE_ID)).thenReturn(null);
        assertThrows(BusinessException.class, () -> borrowService.apply(validDto()));
        verify(borrowMapper, never()).insert(any());
    }

    @Test
    void apply_deviceNotIdle_throws() {
        TestSupport.loginAsStudent(USER_ID);
        Device d = idleDevice();
        d.setStatus(DeviceStatus.BORROWED.code);
        when(deviceService.getById(DEVICE_ID)).thenReturn(d);
        assertThrows(BusinessException.class, () -> borrowService.apply(validDto()));
    }

    @Test
    void apply_missingReturnTime_throws() {
        TestSupport.loginAsStudent(USER_ID);
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        BorrowDTO dto = validDto();
        dto.setExpectReturnTime(null);
        assertThrows(BusinessException.class, () -> borrowService.apply(dto));
    }

    @Test
    void apply_returnBeforeBorrow_throws() {
        TestSupport.loginAsStudent(USER_ID);
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        BorrowDTO dto = validDto();
        dto.setExpectReturnTime(dto.getBorrowTime().minusDays(1));
        assertThrows(BusinessException.class, () -> borrowService.apply(dto));
    }

    @Test
    void apply_borrowLongerThan30Days_throws() {
        TestSupport.loginAsStudent(USER_ID);
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        BorrowDTO dto = validDto();
        dto.setExpectReturnTime(LocalDateTime.now().plusDays(40));
        assertThrows(BusinessException.class, () -> borrowService.apply(dto));
    }

    @Test
    void apply_studentHas3ActiveBorrows_throws() {
        TestSupport.loginAsStudent(USER_ID);
        when(userService.getById(USER_ID)).thenReturn(studentUser());
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(borrowMapper.selectCount(any())).thenReturn(3L);

        assertThrows(BusinessException.class, () -> borrowService.apply(validDto()));
        verify(borrowMapper, never()).insert(any());
    }

    @Test
    void apply_agentTargetNotStudent_throws() {
        TestSupport.loginAsTeacher(5L);
        User target = new User();
        target.setId(8L);
        target.setRole("TEACHER");
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(userService.getById(5L)).thenReturn(teacherUser());
        when(userService.getById(8L)).thenReturn(target);

        BorrowDTO dto = validDto();
        dto.setStudentId(8L);
        assertThrows(BusinessException.class, () -> borrowService.apply(dto));
    }

    @Test
    void apply_agentForStudent_success() {
        TestSupport.loginAsTeacher(5L);
        User target = new User();
        target.setId(8L);
        target.setRole("STUDENT");
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(userService.getById(5L)).thenReturn(teacherUser());
        when(userService.getById(8L)).thenReturn(target);
        when(borrowMapper.selectCount(any())).thenReturn(0L);

        BorrowDTO dto = validDto();
        dto.setStudentId(8L);
        borrowService.apply(dto);

        verify(borrowMapper).insert(argThat(b -> b.getUserId().equals(8L)));
    }

    @Test
    void approve_student_noPermission_throws() {
        TestSupport.loginAsStudent(USER_ID);
        assertThrows(BusinessException.class, () -> borrowService.approve(new ApproveDTO()));
    }

    @Test
    void approve_admin_notPending_throws() {
        TestSupport.loginAsAdmin(9L);
        Borrow borrow = new Borrow();
        borrow.setId(99L);
        borrow.setStatus(BorrowStatus.APPROVED.code);
        when(borrowMapper.selectById(99L)).thenReturn(borrow);

        ApproveDTO dto = new ApproveDTO();
        dto.setBorrowId(99L);
        assertThrows(BusinessException.class, () -> borrowService.approve(dto));
    }

    @Test
    void approve_admin_approved_updatesDevice() {
        TestSupport.loginAsAdmin(9L);
        Borrow borrow = new Borrow();
        borrow.setId(99L);
        borrow.setDeviceId(DEVICE_ID);
        borrow.setStatus(BorrowStatus.PENDING.code);
        when(borrowMapper.selectById(99L)).thenReturn(borrow);
        when(deviceService.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(true);

        ApproveDTO dto = new ApproveDTO();
        dto.setBorrowId(99L);
        dto.setStatus(BorrowStatus.APPROVED.code);
        borrowService.approve(dto);

        verify(deviceService).update(eq(null), any(LambdaUpdateWrapper.class));
        verify(borrowMapper).updateById(any(Borrow.class));
    }

    @Test
    void approve_admin_approved_deviceOccupied_throws() {
        TestSupport.loginAsAdmin(9L);
        Borrow borrow = new Borrow();
        borrow.setId(99L);
        borrow.setDeviceId(DEVICE_ID);
        borrow.setStatus(BorrowStatus.PENDING.code);
        when(borrowMapper.selectById(99L)).thenReturn(borrow);
        when(deviceService.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(false);

        ApproveDTO dto = new ApproveDTO();
        dto.setBorrowId(99L);
        dto.setStatus(BorrowStatus.APPROVED.code);
        assertThrows(BusinessException.class, () -> borrowService.approve(dto));
    }

    @Test
    void approve_admin_invalidStatus_throws() {
        TestSupport.loginAsAdmin(9L);
        Borrow borrow = new Borrow();
        borrow.setId(99L);
        borrow.setStatus(BorrowStatus.PENDING.code);
        when(borrowMapper.selectById(99L)).thenReturn(borrow);

        ApproveDTO dto = new ApproveDTO();
        dto.setBorrowId(99L);
        dto.setStatus(99);
        assertThrows(BusinessException.class, () -> borrowService.approve(dto));
    }

    @Test
    void approve_labAdmin_notManager_throws() {
        TestSupport.loginAsLabAdmin(9L);
        Borrow borrow = new Borrow();
        borrow.setId(99L);
        borrow.setDeviceId(DEVICE_ID);
        borrow.setStatus(BorrowStatus.PENDING.code);
        when(borrowMapper.selectById(99L)).thenReturn(borrow);
        Device d = idleDevice();
        d.setLabId(50L);
        when(deviceService.getById(DEVICE_ID)).thenReturn(d);
        when(labService.count(any())).thenReturn(0L);

        ApproveDTO dto = new ApproveDTO();
        dto.setBorrowId(99L);
        dto.setStatus(BorrowStatus.APPROVED.code);
        assertThrows(BusinessException.class, () -> borrowService.approve(dto));
    }

    @Test
    void approve_labAdmin_managesDevice_success() {
        TestSupport.loginAsLabAdmin(9L);
        Borrow borrow = new Borrow();
        borrow.setId(99L);
        borrow.setDeviceId(DEVICE_ID);
        borrow.setStatus(BorrowStatus.PENDING.code);
        when(borrowMapper.selectById(99L)).thenReturn(borrow);
        Device d = idleDevice();
        d.setLabId(50L);
        when(deviceService.getById(DEVICE_ID)).thenReturn(d);
        when(labService.count(any())).thenReturn(1L);
        when(deviceService.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(true);

        ApproveDTO dto = new ApproveDTO();
        dto.setBorrowId(99L);
        dto.setStatus(BorrowStatus.APPROVED.code);
        borrowService.approve(dto);
        verify(borrowMapper).updateById(any(Borrow.class));
    }

    @Test
    void returnDevice_notOwnerAndNotAdmin_throws() {
        TestSupport.loginAsStudent(2L);
        Borrow borrow = new Borrow();
        borrow.setId(99L);
        borrow.setUserId(5L);
        borrow.setStatus(BorrowStatus.APPROVED.code);
        when(borrowMapper.selectById(99L)).thenReturn(borrow);

        assertThrows(BusinessException.class, () -> borrowService.returnDevice(99L));
    }

    @Test
    void returnDevice_statusNotReturnable_throws() {
        TestSupport.loginAsStudent(USER_ID);
        Borrow borrow = new Borrow();
        borrow.setId(99L);
        borrow.setUserId(USER_ID);
        borrow.setStatus(BorrowStatus.PENDING.code);
        when(borrowMapper.selectById(99L)).thenReturn(borrow);

        assertThrows(BusinessException.class, () -> borrowService.returnDevice(99L));
    }

    @Test
    void returnDevice_success_setsReturnedAndIdle() {
        TestSupport.loginAsStudent(USER_ID);
        Borrow borrow = new Borrow();
        borrow.setId(99L);
        borrow.setUserId(USER_ID);
        borrow.setDeviceId(DEVICE_ID);
        borrow.setStatus(BorrowStatus.APPROVED.code);
        borrow.setExpectReturnTime(LocalDateTime.now().plusDays(1));
        when(borrowMapper.selectById(99L)).thenReturn(borrow);
        when(deviceService.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(true);

        borrowService.returnDevice(99L);

        verify(borrowMapper).updateById(argThat(b ->
                b.getStatus() == BorrowStatus.RETURNED.code && b.getActualReturnTime() != null));
    }

    @Test
    void returnDevice_overdue_addsRemark() {
        TestSupport.loginAsStudent(USER_ID);
        Borrow borrow = new Borrow();
        borrow.setId(99L);
        borrow.setUserId(USER_ID);
        borrow.setDeviceId(DEVICE_ID);
        borrow.setStatus(BorrowStatus.APPROVED.code);
        borrow.setExpectReturnTime(LocalDateTime.now().minusDays(1));
        when(borrowMapper.selectById(99L)).thenReturn(borrow);
        when(deviceService.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(true);

        borrowService.returnDevice(99L);

        verify(borrowMapper).updateById(argThat(b -> b.getRemark() != null && b.getRemark().contains("逾期归还")));
    }

    @Test
    void returnDevice_deviceNotBorrowed_throws() {
        TestSupport.loginAsStudent(USER_ID);
        Borrow borrow = new Borrow();
        borrow.setId(99L);
        borrow.setUserId(USER_ID);
        borrow.setDeviceId(DEVICE_ID);
        borrow.setStatus(BorrowStatus.APPROVED.code);
        when(borrowMapper.selectById(99L)).thenReturn(borrow);
        when(deviceService.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(false);

        assertThrows(BusinessException.class, () -> borrowService.returnDevice(99L));
    }

    @Test
    void cancelBorrow_notOwner_throws() {
        TestSupport.loginAsStudent(2L);
        Borrow borrow = new Borrow();
        borrow.setId(99L);
        borrow.setUserId(5L);
        borrow.setStatus(BorrowStatus.PENDING.code);
        when(borrowMapper.selectById(99L)).thenReturn(borrow);

        assertThrows(BusinessException.class, () -> borrowService.cancelBorrow(99L));
    }

    @Test
    void cancelBorrow_notPending_throws() {
        TestSupport.loginAsStudent(USER_ID);
        Borrow borrow = new Borrow();
        borrow.setId(99L);
        borrow.setUserId(USER_ID);
        borrow.setStatus(BorrowStatus.APPROVED.code);
        when(borrowMapper.selectById(99L)).thenReturn(borrow);

        assertThrows(BusinessException.class, () -> borrowService.cancelBorrow(99L));
    }

    @Test
    void cancelBorrow_success_setsCancelled() {
        TestSupport.loginAsStudent(USER_ID);
        Borrow borrow = new Borrow();
        borrow.setId(99L);
        borrow.setUserId(USER_ID);
        borrow.setStatus(BorrowStatus.PENDING.code);
        when(borrowMapper.selectById(99L)).thenReturn(borrow);

        borrowService.cancelBorrow(99L);

        verify(borrowMapper).updateById(argThat(b -> b.getStatus() == BorrowStatus.CANCELLED.code));
    }
}