package com.smartlab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartlab.config.SecurityConfig.UserPrincipal;
import com.smartlab.dto.ApproveDTO;
import com.smartlab.dto.BorrowDTO;
import com.smartlab.entity.Borrow;
import com.smartlab.entity.Device;
import com.smartlab.entity.User;
import com.smartlab.enums.BorrowStatus;
import com.smartlab.enums.DeviceStatus;
import com.smartlab.exception.BusinessException;
import com.smartlab.mapper.BorrowMapper;
import com.smartlab.entity.Lab;
import com.smartlab.service.BorrowService;
import com.smartlab.service.LabService;
import com.smartlab.service.DeviceService;
import com.smartlab.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class BorrowServiceImpl extends ServiceImpl<BorrowMapper, Borrow> implements BorrowService {

    private final DeviceService deviceService;
    private final UserService userService;
    private final LabService labService;

    public BorrowServiceImpl(DeviceService deviceService, UserService userService, LabService labService) {
        this.deviceService = deviceService;
        this.userService = userService;
        this.labService = labService;
    }

    private UserPrincipal currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserPrincipal) authentication.getPrincipal();
    }

    @Override
    @Transactional
    public void apply(BorrowDTO dto) {
        var principal = currentUser();
        var user = userService.getById(principal.userId());

        Device device = deviceService.getById(dto.getDeviceId());
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if (device.getStatus() != DeviceStatus.IDLE.code) {
            throw new BusinessException("设备当前不可借用(状态: " + DeviceStatus.of(device.getStatus()).label + ")");
        }

        LocalDateTime borrowTime = dto.getBorrowTime() != null ? dto.getBorrowTime() : LocalDateTime.now();
        LocalDateTime returnTime = dto.getExpectReturnTime();
        if (returnTime == null) {
            throw new BusinessException("请选择预计归还时间");
        }
        if (borrowTime.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BusinessException("借用时间不能早于当前时间");
        }
        if (returnTime.isBefore(borrowTime)) {
            throw new BusinessException("归还时间不能早于借用时间");
        }
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(borrowTime, returnTime);
        if (daysBetween > 30) {
            throw new BusinessException("单次借用最长为30天");
        }

        Long targetUserId = (dto.getStudentId() != null) ? dto.getStudentId() : principal.userId();
        User targetUser = (dto.getStudentId() != null) ? userService.getById(dto.getStudentId()) : user;

        if (dto.getStudentId() != null) {
            if (targetUser == null) {
                throw new BusinessException("目标学生不存在");
            }
            if (!"STUDENT".equals(targetUser.getRole())) {
                throw new BusinessException("代申请人必须是学生");
            }
        }

        String targetRole = targetUser.getRole();

        long todayCount = this.count(new LambdaQueryWrapper<Borrow>()
                .eq(Borrow::getUserId, targetUserId)
                .ge(Borrow::getCreateTime, java.time.LocalDate.now().atStartOfDay()));

        if ("STUDENT".equals(targetRole)) {
            if (todayCount >= 3) {
                throw new BusinessException("学生每天最多借用3台设备，今日已达上限");
            }
        } else if ("TEACHER".equals(targetRole)) {
            if (todayCount >= 100) {
                throw new BusinessException("教师每天最多借用100台设备，今日已达上限");
            }
        }

        long activeBorrows = this.count(new LambdaQueryWrapper<Borrow>()
                .eq(Borrow::getUserId, targetUserId)
                .in(Borrow::getStatus, BorrowStatus.PENDING.code, BorrowStatus.APPROVED.code, BorrowStatus.OVERDUE.code));
        if (activeBorrows >= 3) {
            if (dto.getStudentId() != null) {
                throw new BusinessException("该学生已有3条未归还的借用记录，请先归还");
            } else {
                throw new BusinessException("您已有3条未归还的借用记录，请先归还");
            }
        }

        Borrow borrow = new Borrow();
        borrow.setUserId(targetUserId);
        borrow.setDeviceId(device.getId());
        borrow.setBorrowTime(borrowTime);
        borrow.setExpectReturnTime(dto.getExpectReturnTime());
        borrow.setPurpose(dto.getPurpose());
        borrow.setRemark(dto.getRemark());
        borrow.setStatus(BorrowStatus.PENDING.code);

        this.save(borrow);
    }

    @Override
    @Transactional
    public void approve(ApproveDTO dto) {
        var principal = currentUser();
        String role = principal.role();
        if (!"ADMIN".equals(role) && !"LAB_ADMIN".equals(role)) {
            throw new BusinessException("无审批权限");
        }

        Borrow borrow = this.getById(dto.getBorrowId());
        if (borrow == null) {
            throw new BusinessException("借用记录不存在");
        }
        if (borrow.getStatus() != BorrowStatus.PENDING.code) {
            throw new BusinessException("该记录已审批");
        }

        if ("LAB_ADMIN".equals(role)) {
            Device dev = deviceService.getById(borrow.getDeviceId());
            Long labId = dev != null ? dev.getLabId() : null;
            long managedCount = labService.count(new LambdaQueryWrapper<Lab>()
                    .eq(Lab::getManagerId, principal.userId())
                    .eq(Lab::getId, labId));
            if (managedCount == 0) {
                throw new BusinessException("您不是该设备所属实验室的负责人，无权审批");
            }
        }

        if (dto.getStatus() != BorrowStatus.APPROVED.code && dto.getStatus() != BorrowStatus.REJECTED.code) {
            throw new BusinessException("审批状态不合法");
        }

        borrow.setStatus(dto.getStatus());
        borrow.setApproverId(principal.userId());
        borrow.setApproveTime(LocalDateTime.now());
        borrow.setApproveRemark(dto.getApproveRemark());

        if (dto.getStatus() == BorrowStatus.APPROVED.code) {
            boolean deviceOk = deviceService.update(null, new LambdaUpdateWrapper<Device>()
                    .eq(Device::getId, borrow.getDeviceId())
                    .eq(Device::getStatus, DeviceStatus.IDLE.code)
                    .set(Device::getStatus, DeviceStatus.BORROWED.code));
            if (!deviceOk) {
                throw new BusinessException("设备已被占用或状态变更，审批失败，请刷新页面");
            }
            this.updateById(borrow);
        } else {
            this.updateById(borrow);
        }
    }

    @Override
    @Transactional
    public void returnDevice(Long borrowId) {
        var principal = currentUser();

        Borrow borrow = this.getById(borrowId);
        if (borrow == null) {
            throw new BusinessException("借用记录不存在");
        }
        if (!borrow.getUserId().equals(principal.userId())
                && !"ADMIN".equals(principal.role())
                && !"LAB_ADMIN".equals(principal.role())) {
            throw new BusinessException("只能归还自己的借用记录");
        }
        if (borrow.getStatus() != BorrowStatus.APPROVED.code
                && borrow.getStatus() != BorrowStatus.OVERDUE.code) {
            throw new BusinessException("该记录状态不允许归还");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isOverdue = borrow.getExpectReturnTime() != null && now.isAfter(borrow.getExpectReturnTime());

        borrow.setActualReturnTime(now);
        borrow.setStatus(BorrowStatus.RETURNED.code);
        if (isOverdue) {
            borrow.setRemark((borrow.getRemark() == null ? "" : borrow.getRemark() + " | ") + "⚠️逾期归还");
        }
        this.updateById(borrow);

        boolean ok = deviceService.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getId, borrow.getDeviceId())
                .eq(Device::getStatus, DeviceStatus.BORROWED.code)
                .set(Device::getStatus, DeviceStatus.IDLE.code));
        if (!ok) {
            throw new BusinessException("设备当前不在借出状态，无法归还");
        }
    }

    @Override
    @Transactional
    public void cancelBorrow(Long borrowId) {
        var principal = currentUser();

        Borrow borrow = this.getById(borrowId);
        if (borrow == null) {
            throw new BusinessException("借用记录不存在");
        }
        if (!borrow.getUserId().equals(principal.userId())) {
            throw new BusinessException("只能取消自己的借用申请");
        }
        if (borrow.getStatus() != BorrowStatus.PENDING.code) {
            throw new BusinessException("只能取消待审批的申请");
        }

        borrow.setStatus(BorrowStatus.CANCELLED.code);
        this.updateById(borrow);
    }
}