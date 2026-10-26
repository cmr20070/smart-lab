package com.smartlab.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartlab.config.SecurityConfig.UserPrincipal;
import com.smartlab.dto.RepairDTO;
import com.smartlab.entity.Device;
import com.smartlab.entity.Repair;
import com.smartlab.enums.DeviceStatus;
import com.smartlab.enums.RepairStatus;
import com.smartlab.exception.BusinessException;
import com.smartlab.mapper.RepairMapper;
import com.smartlab.service.DeviceService;
import com.smartlab.service.RepairService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class RepairServiceImpl extends ServiceImpl<RepairMapper, Repair> implements RepairService {

    private final DeviceService deviceService;

    public RepairServiceImpl(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    @Transactional
    public void reportFault(RepairDTO dto) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = (UserPrincipal) authentication.getPrincipal();

        Device device = deviceService.getById(dto.getDeviceId());
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if (device.getStatus() == DeviceStatus.SCRAPPED.code
                || device.getStatus() == DeviceStatus.DISCARD_PENDING_LAB.code
                || device.getStatus() == DeviceStatus.DISCARD_PENDING_ADMIN.code) {
            throw new BusinessException("设备已报废或在报废流程中，无法报修");
        }
        if (device.getStatus() == DeviceStatus.REPAIRING.code) {
            throw new BusinessException("设备已在维修中，请勿重复报修");
        }

        Repair repair = new Repair();
        repair.setDeviceId(device.getId());
        repair.setReporterId(principal.userId());
        repair.setReportTime(LocalDateTime.now());
        repair.setFaultDescription(dto.getFaultDescription());
        repair.setFaultLevel(dto.getFaultLevel());
        repair.setStatus(RepairStatus.PENDING.code);
        repair.setRemark(dto.getRemark());
        this.save(repair);

        boolean ok = deviceService.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getId, device.getId())
                .in(Device::getStatus, DeviceStatus.IDLE.code, DeviceStatus.BORROWED.code)
                .set(Device::getStatus, DeviceStatus.REPAIRING.code));
        if (!ok) {
            throw new BusinessException("设备状态已变更，报修失败，请刷新页面");
        }
    }

    @Override
    @Transactional
    public void assignRepair(Long repairId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = (UserPrincipal) authentication.getPrincipal();
        if (!"ADMIN".equals(principal.role()) && !"LAB_ADMIN".equals(principal.role())) {
            throw new BusinessException("无接修权限");
        }

        Repair repair = this.getById(repairId);
        if (repair == null) {
            throw new BusinessException("维修记录不存在");
        }
        if (repair.getStatus() != RepairStatus.PENDING.code) {
            throw new BusinessException("该维修记录当前状态不允许分配");
        }

        repair.setRepairerId(principal.userId());
        repair.setRepairTime(LocalDateTime.now());
        repair.setStatus(RepairStatus.REPAIRING.code);
        this.updateById(repair);
    }

    @Override
    @Transactional
    public void completeRepair(Long repairId, String repairDescription, BigDecimal cost) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = (UserPrincipal) authentication.getPrincipal();
        if (!"ADMIN".equals(principal.role()) && !"LAB_ADMIN".equals(principal.role())) {
            throw new BusinessException("无维修完成权限");
        }

        Repair repair = this.getById(repairId);
        if (repair == null) {
            throw new BusinessException("维修记录不存在");
        }
        if (repair.getStatus() != RepairStatus.REPAIRING.code) {
            throw new BusinessException("该维修记录当前状态不允许完成");
        }

        repair.setFinishTime(LocalDateTime.now());
        repair.setRepairDescription(repairDescription);
        repair.setRepairCost(cost);
        repair.setStatus(RepairStatus.COMPLETED.code);
        this.updateById(repair);

        Device device = deviceService.getById(repair.getDeviceId());
        if (device != null) {
            boolean ok = deviceService.update(null, new LambdaUpdateWrapper<Device>()
                    .eq(Device::getId, device.getId())
                    .eq(Device::getStatus, DeviceStatus.REPAIRING.code)
                    .set(Device::getStatus, DeviceStatus.IDLE.code));
            if (!ok) {
                throw new BusinessException("设备状态已变更，无法自动恢复空闲");
            }
        }
    }
}