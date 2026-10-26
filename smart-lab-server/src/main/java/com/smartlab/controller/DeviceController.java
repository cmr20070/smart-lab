package com.smartlab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.dto.DeviceStatusDTO;
import com.smartlab.entity.Borrow;
import com.smartlab.entity.Device;
import com.smartlab.entity.Lab;
import com.smartlab.entity.Repair;
import com.smartlab.entity.User;
import com.smartlab.config.SecurityConfig.UserPrincipal;
import com.smartlab.enums.BorrowStatus;
import com.smartlab.enums.DeviceStatus;
import com.smartlab.enums.RepairStatus;
import com.smartlab.exception.BusinessException;
import com.smartlab.service.BorrowService;
import com.smartlab.service.DeviceService;
import com.smartlab.service.LabService;
import com.smartlab.service.OperationLogService;
import com.smartlab.service.RepairService;
import com.smartlab.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final DeviceService deviceService;
    private final OperationLogService logService;
    private final BorrowService borrowService;
    private final RepairService repairService;
    private final UserService userService;
    private final LabService labService;

    public DeviceController(DeviceService deviceService, OperationLogService logService,
                            BorrowService borrowService, RepairService repairService, UserService userService,
                            LabService labService) {
        this.deviceService = deviceService;
        this.logService = logService;
        this.borrowService = borrowService;
        this.repairService = repairService;
        this.userService = userService;
        this.labService = labService;
    }

    @GetMapping("/list")
    public Result<PageResult<Device>> list(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) Long labId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Device::getName, keyword)
                    .or().like(Device::getCode, keyword)
                    .or().like(Device::getModel, keyword));
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Device::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(Device::getStatus, status);
        }
        if (labId != null) {
            wrapper.eq(Device::getLabId, labId);
        }
        wrapper.orderByDesc(Device::getCreateTime);
        Page<Device> pageResult = deviceService.page(new Page<>(page, size), wrapper);
        return Result.success(PageResult.of(pageResult.getTotal(), page, size, pageResult.getRecords()));
    }

    @GetMapping("/all")
    public Result<java.util.List<Device>> all() {
        return Result.success(deviceService.list());
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody Device device) {
        checkManagePermission();
        if (device.getStatus() == null) {
            device.setStatus(DeviceStatus.IDLE.code);
        }
        deviceService.save(device);
        logService.log("device", "add", "添加设备: " + device.getName(), device.getId());
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Device device) {
        checkManagePermission();
        Device exist = deviceService.getById(id);
        if (exist == null) {
            throw new BusinessException("设备不存在");
        }
        if (device.getLabId() != null && !device.getLabId().equals(exist.getLabId())) {
            Lab lab = labService.getById(device.getLabId());
            if (lab == null) {
                throw new BusinessException("目标实验室不存在");
            }
        }
        device.setId(id);
        deviceService.updateById(device);
        logService.log("device", "edit", "编辑设备: " + device.getName(), id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkManagePermission();
        Device device = deviceService.getById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if (device.getStatus() != DeviceStatus.IDLE.code && device.getStatus() != DeviceStatus.SCRAPPED.code) {
            throw new BusinessException("仅空闲或已报废设备可删除");
        }
        long activeBorrows = borrowService.count(new LambdaQueryWrapper<Borrow>()
                .eq(Borrow::getDeviceId, id)
                .in(Borrow::getStatus, BorrowStatus.PENDING.code, BorrowStatus.APPROVED.code, BorrowStatus.OVERDUE.code));
        if (activeBorrows > 0) {
            throw new BusinessException("该设备存在未完成的借用记录，无法删除");
        }
        long activeRepairs = repairService.count(new LambdaQueryWrapper<Repair>()
                .eq(Repair::getDeviceId, id)
                .eq(Repair::getStatus, RepairStatus.PENDING.code));
        if (activeRepairs > 0) {
            throw new BusinessException("该设备存在待处理的维修申请，无法删除");
        }
        deviceService.removeById(id);
        logService.log("device", "delete", "删除设备: " + device.getName(), id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @Transactional
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody DeviceStatusDTO dto) {
        checkManagePermission();
        String role = currentRole();
        boolean isAdmin = "ADMIN".equals(role);
        int status = dto.getStatus();

        if (status == DeviceStatus.DISCARD_PENDING_ADMIN.code || status == DeviceStatus.DISCARD_PENDING_LAB.code) {
            throw new BusinessException("报废请使用专门的报废流程操作");
        }
        if (status == DeviceStatus.SCRAPPED.code && !isAdmin) {
            throw new BusinessException("只有管理员可以直接报废设备");
        }

        Device device = deviceService.getById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if (device.getStatus().equals(status)) {
            return Result.success();
        }

        if (status == DeviceStatus.BORROWED.code) {
            if (dto.getOperatorId() == null) {
                throw new BusinessException("请选择借用人");
            }
            if (dto.getExpectReturnTime() == null) {
                throw new BusinessException("请选择预计归还时间");
            }
            User borrower = userService.getById(dto.getOperatorId());
            if (borrower == null) {
                throw new BusinessException("借用人不存在");
            }
        }

        if (status == DeviceStatus.REPAIRING.code) {
            if (dto.getOperatorId() == null) {
                throw new BusinessException("请选择维修人");
            }
            User repairer = userService.getById(dto.getOperatorId());
            if (repairer == null) {
                throw new BusinessException("维修人不存在");
            }
            if (!"LAB_ADMIN".equals(repairer.getRole()) && !"ADMIN".equals(repairer.getRole())) {
                throw new BusinessException("维修人必须是实验室管理员或管理员");
            }
        }

        boolean ok;
        if (isAdmin) {
            ok = deviceService.update(null, new LambdaUpdateWrapper<Device>()
                    .eq(Device::getId, id)
                    .ne(Device::getStatus, status)
                    .set(Device::getStatus, status));
        } else {
            ok = deviceService.update(null, new LambdaUpdateWrapper<Device>()
                    .eq(Device::getId, id)
                    .in(Device::getStatus, DeviceStatus.IDLE.code, DeviceStatus.BORROWED.code, DeviceStatus.REPAIRING.code)
                    .set(Device::getStatus, status));
        }
        if (!ok) {
            throw new BusinessException("设备状态已变更，请刷新页面");
        }

        if (status == DeviceStatus.BORROWED.code) {
            createBorrowRecord(device, dto);
        } else if (status == DeviceStatus.REPAIRING.code) {
            createRepairRecord(device, dto);
        }

        String actionDetail = buildStatusLog(status, device, dto.getOperatorId(), dto.getOperatorName());
        logService.log("device", "status", actionDetail + " (by " + role + ")", id);
        return Result.success();
    }

    private void createBorrowRecord(Device device, DeviceStatusDTO dto) {
        User borrower = userService.getById(dto.getOperatorId());
        Borrow borrow = new Borrow();
        borrow.setUserId(borrower.getId());
        borrow.setDeviceId(device.getId());
        borrow.setBorrowTime(LocalDateTime.now());
        borrow.setExpectReturnTime(dto.getExpectReturnTime());
        borrow.setPurpose(dto.getPurpose());
        borrow.setRemark(dto.getRemark());
        borrow.setStatus(BorrowStatus.APPROVED.code);
        borrow.setApproverId(currentUserId());
        borrow.setApproveTime(LocalDateTime.now());
        borrow.setApproveRemark("管理员直接设置借出");
        borrowService.save(borrow);
    }

    private void createRepairRecord(Device device, DeviceStatusDTO dto) {
        User repairer = userService.getById(dto.getOperatorId());
        Repair repair = new Repair();
        repair.setDeviceId(device.getId());
        repair.setReporterId(currentUserId());
        repair.setReportTime(LocalDateTime.now());
        repair.setFaultDescription(dto.getFaultDescription());
        repair.setFaultLevel(dto.getFaultLevel() != null ? dto.getFaultLevel() : 1);
        repair.setStatus(RepairStatus.REPAIRING.code);
        repair.setRepairerId(repairer.getId());
        repair.setRepairTime(LocalDateTime.now());
        repair.setRepairDescription(dto.getRepairDescription());
        repair.setRemark(dto.getRemark());
        repairService.save(repair);
    }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return ((UserPrincipal) auth.getPrincipal()).userId();
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return ((UserPrincipal) auth.getPrincipal()).username();
    }

    private String buildStatusLog(int status, Device device, Long operatorId, String operatorName) {
        if (status == DeviceStatus.BORROWED.code && operatorName != null) {
            return "[" + device.getName() + "] 设为借出 → 借用人: " + operatorName + "(id:" + operatorId + ") [自动生成借用记录]";
        }
        if (status == DeviceStatus.REPAIRING.code && operatorName != null) {
            return "[" + device.getName() + "] 设为维修中 → 维修人: " + operatorName + "(id:" + operatorId + ") [自动生成维修记录]";
        }
        return "[" + device.getName() + "] 状态变更 → " + DeviceStatus.of(status).label;
    }

    @PutMapping("/{id}/discard")
    public Result<Void> submitDiscard(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        Device device = deviceService.getById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        if (device.getStatus() != DeviceStatus.IDLE.code) {
            throw new BusinessException("仅空闲状态的设备可报废，请先归还/完成维修");
        }
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        boolean isAdmin = "ADMIN".equals(currentRole());

        if (isAdmin) {
            boolean ok = deviceService.update(null, new LambdaUpdateWrapper<Device>()
                    .eq(Device::getId, id)
                    .eq(Device::getStatus, DeviceStatus.IDLE.code)
                    .set(Device::getStatus, DeviceStatus.SCRAPPED.code));
            if (!ok) {
                throw new BusinessException("设备状态不允许报废，请刷新页面");
            }
            logService.log("device", "discard", "管理员直接报废: " + device.getName() + "，原因: " + reason, id);
        } else {
            boolean ok = deviceService.update(null, new LambdaUpdateWrapper<Device>()
                    .eq(Device::getId, id)
                    .eq(Device::getStatus, DeviceStatus.IDLE.code)
                    .set(Device::getStatus, DeviceStatus.DISCARD_PENDING_ADMIN.code));
            if (!ok) {
                throw new BusinessException("设备状态不允许提交报废，请刷新页面");
            }
            logService.log("device", "discard", "提交报废申请: " + device.getName() + "，原因: " + reason + "（待管理员审核）", id);
        }
        return Result.success();
    }

    @PutMapping("/{id}/discard/approve")
    public Result<Void> approveDiscard(@PathVariable Long id) {
        checkAdminPermission();
        Device device = deviceService.getById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        boolean ok = deviceService.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getId, id)
                .eq(Device::getStatus, DeviceStatus.DISCARD_PENDING_ADMIN.code)
                .set(Device::getStatus, DeviceStatus.SCRAPPED.code));
        if (!ok) {
            throw new BusinessException("设备状态已变更，请刷新页面");
        }
        logService.log("device", "discardApprove", "管理员审核通过报废: " + device.getName() + "（已报废）", id);
        return Result.success();
    }

    @PutMapping("/{id}/discard/approve-final")
    @Transactional
    public Result<Void> finalApproveDiscard(@PathVariable Long id) {
        checkAdminPermission();
        Device device = deviceService.getById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        boolean ok = deviceService.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getId, id)
                .eq(Device::getStatus, DeviceStatus.DISCARD_PENDING_ADMIN.code)
                .set(Device::getStatus, DeviceStatus.SCRAPPED.code));
        if (!ok) {
            throw new BusinessException("设备状态已变更，请刷新页面");
        }

        borrowService.update(null, new LambdaUpdateWrapper<Borrow>()
                .eq(Borrow::getDeviceId, id)
                .in(Borrow::getStatus, BorrowStatus.PENDING.code)
                .set(Borrow::getStatus, BorrowStatus.CANCELLED.code)
                .set(Borrow::getRemark, "设备已报废，申请自动取消"));

        repairService.update(null, new LambdaUpdateWrapper<Repair>()
                .eq(Repair::getDeviceId, id)
                .in(Repair::getStatus, RepairStatus.PENDING.code, RepairStatus.REPAIRING.code)
                .set(Repair::getStatus, RepairStatus.COMPLETED.code)
                .set(Repair::getRepairDescription, "设备已报废，维修自动终止"));

        logService.log("device", "discardApproveFinal", "管理员终审通过: " + device.getName() + "（已报废）", id);
        return Result.success();
    }

    @PutMapping("/{id}/discard/reject")
    public Result<Void> rejectDiscard(@PathVariable Long id) {
        checkLabAdminPermission();
        Device device = deviceService.getById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        boolean ok = deviceService.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getId, id)
                .in(Device::getStatus, DeviceStatus.DISCARD_PENDING_LAB.code, DeviceStatus.DISCARD_PENDING_ADMIN.code)
                .set(Device::getStatus, DeviceStatus.IDLE.code));
        if (!ok) {
            throw new BusinessException("设备状态已变更，请刷新页面");
        }
        logService.log("device", "discardReject", "报废审核驳回: " + device.getName() + "（恢复空闲）", id);
        return Result.success();
    }

    @GetMapping("/discard/list")
    public Result<PageResult<Device>> discardList(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Device::getStatus, DeviceStatus.DISCARD_PENDING_LAB.code, DeviceStatus.DISCARD_PENDING_ADMIN.code)
                .orderByDesc(Device::getUpdateTime);
        Page<Device> pageResult = deviceService.page(new Page<>(page, size), wrapper);
        return Result.success(PageResult.of(pageResult.getTotal(), page, size, pageResult.getRecords()));
    }

    @GetMapping("/overdue/borrows")
    public Result<PageResult<com.smartlab.entity.Borrow>> overdueBorrows(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(deviceService.getOverdueBorrows(page, size));
    }

    private void checkManagePermission() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = (UserPrincipal) authentication.getPrincipal();
        if ("STUDENT".equals(principal.role()) || "TEACHER".equals(principal.role())) {
            throw new BusinessException("无操作权限");
        }
    }

    private void checkAdminPermission() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = (UserPrincipal) authentication.getPrincipal();
        if (!"ADMIN".equals(principal.role())) {
            throw new BusinessException("只有管理员可以执行此操作");
        }
    }

    private void checkLabAdminPermission() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = (UserPrincipal) authentication.getPrincipal();
        var role = principal.role();
        if (!"ADMIN".equals(role) && !"LAB_ADMIN".equals(role)) {
            throw new BusinessException("只有实验室管理员及以上可以执行此操作");
        }
    }

    private String currentRole() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = (UserPrincipal) authentication.getPrincipal();
        return principal.role();
    }
}