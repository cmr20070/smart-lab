package com.smartlab.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartlab.TestSupport;
import com.smartlab.dto.RepairDTO;
import com.smartlab.entity.Device;
import com.smartlab.entity.Repair;
import com.smartlab.enums.DeviceStatus;
import com.smartlab.enums.RepairStatus;
import com.smartlab.exception.BusinessException;
import com.smartlab.mapper.RepairMapper;
import com.smartlab.service.DeviceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairServiceImplTest {

    @Mock
    private DeviceService deviceService;
    @Mock
    private RepairMapper repairMapper;

    @InjectMocks
    private RepairServiceImpl repairService;

    private static final long DEVICE_ID = 100L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(repairService, "baseMapper", repairMapper);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clear();
    }

    private Device idleDevice() {
        Device d = new Device();
        d.setId(DEVICE_ID);
        d.setStatus(DeviceStatus.IDLE.code);
        return d;
    }

    private RepairDTO validDto() {
        RepairDTO dto = new RepairDTO();
        dto.setDeviceId(DEVICE_ID);
        dto.setFaultDescription("屏幕花屏");
        dto.setFaultLevel(2);
        return dto;
    }

    @Test
    void reportFault_success_createsRepairAndSetsRepairing() {
        TestSupport.loginAsStudent(1L);
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(deviceService.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(true);

        repairService.reportFault(validDto());

        verify(repairMapper).insert(argThat(r ->
                r.getStatus() == RepairStatus.PENDING.code && r.getReporterId().equals(1L)));
        verify(deviceService).update(eq(null), any(LambdaUpdateWrapper.class));
    }

    @Test
    void reportFault_deviceNotExists_throws() {
        TestSupport.loginAsStudent(1L);
        when(deviceService.getById(DEVICE_ID)).thenReturn(null);
        assertThrows(BusinessException.class, () -> repairService.reportFault(validDto()));
    }

    @Test
    void reportFault_scrappedDevice_throws() {
        TestSupport.loginAsStudent(1L);
        Device d = idleDevice();
        d.setStatus(DeviceStatus.SCRAPPED.code);
        when(deviceService.getById(DEVICE_ID)).thenReturn(d);
        assertThrows(BusinessException.class, () -> repairService.reportFault(validDto()));
    }

    @Test
    void reportFault_alreadyRepairing_throws() {
        TestSupport.loginAsStudent(1L);
        Device d = idleDevice();
        d.setStatus(DeviceStatus.REPAIRING.code);
        when(deviceService.getById(DEVICE_ID)).thenReturn(d);
        assertThrows(BusinessException.class, () -> repairService.reportFault(validDto()));
    }

    @Test
    void reportFault_stateChanged_fails() {
        TestSupport.loginAsStudent(1L);
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(deviceService.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(false);
        assertThrows(BusinessException.class, () -> repairService.reportFault(validDto()));
    }

    @Test
    void assignRepair_noPermission_throws() {
        TestSupport.loginAsStudent(1L);
        assertThrows(BusinessException.class, () -> repairService.assignRepair(200L));
    }

    @Test
    void assignRepair_notPending_throws() {
        TestSupport.loginAsAdmin(2L);
        Repair repair = new Repair();
        repair.setId(200L);
        repair.setStatus(RepairStatus.REPAIRING.code);
        when(repairMapper.selectById(200L)).thenReturn(repair);
        assertThrows(BusinessException.class, () -> repairService.assignRepair(200L));
    }

    @Test
    void assignRepair_success_setsRepairing() {
        TestSupport.loginAsLabAdmin(2L);
        Repair repair = new Repair();
        repair.setId(200L);
        repair.setStatus(RepairStatus.PENDING.code);
        when(repairMapper.selectById(200L)).thenReturn(repair);

        repairService.assignRepair(200L);

        verify(repairMapper).updateById(argThat(r ->
                r.getStatus() == RepairStatus.REPAIRING.code && r.getRepairerId().equals(2L)));
    }

    @Test
    void completeRepair_noPermission_throws() {
        TestSupport.loginAsStudent(1L);
        assertThrows(BusinessException.class, () -> repairService.completeRepair(200L, "done", BigDecimal.TEN));
    }

    @Test
    void completeRepair_notRepairing_throws() {
        TestSupport.loginAsAdmin(2L);
        Repair repair = new Repair();
        repair.setId(200L);
        repair.setStatus(RepairStatus.PENDING.code);
        when(repairMapper.selectById(200L)).thenReturn(repair);
        assertThrows(BusinessException.class,
                () -> repairService.completeRepair(200L, "done", BigDecimal.TEN));
    }

    @Test
    void completeRepair_success_setsCompletedAndIdle() {
        TestSupport.loginAsAdmin(2L);
        Repair repair = new Repair();
        repair.setId(200L);
        repair.setDeviceId(DEVICE_ID);
        repair.setStatus(RepairStatus.REPAIRING.code);
        when(repairMapper.selectById(200L)).thenReturn(repair);
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(deviceService.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(true);

        repairService.completeRepair(200L, "修复完成", new BigDecimal("99.50"));

        verify(repairMapper).updateById(argThat(r ->
                r.getStatus() == RepairStatus.COMPLETED.code
                        && r.getRepairCost().compareTo(new BigDecimal("99.50")) == 0));
        verify(deviceService).update(eq(null), any(LambdaUpdateWrapper.class));
    }

    @Test
    void completeRepair_deviceUpdateFails_throws() {
        TestSupport.loginAsAdmin(2L);
        Repair repair = new Repair();
        repair.setId(200L);
        repair.setDeviceId(DEVICE_ID);
        repair.setStatus(RepairStatus.REPAIRING.code);
        when(repairMapper.selectById(200L)).thenReturn(repair);
        when(deviceService.getById(DEVICE_ID)).thenReturn(idleDevice());
        when(deviceService.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> repairService.completeRepair(200L, "done", BigDecimal.ZERO));
    }
}