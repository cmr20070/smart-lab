package com.smartlab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.config.SecurityConfig.UserPrincipal;
import com.smartlab.dto.ApproveDTO;
import com.smartlab.dto.BorrowDTO;
import com.smartlab.entity.Borrow;
import com.smartlab.entity.Device;
import com.smartlab.entity.User;
import com.smartlab.enums.BorrowStatus;
import com.smartlab.exception.BusinessException;
import com.smartlab.entity.Lab;
import com.smartlab.service.BorrowService;
import com.smartlab.service.LabService;
import com.smartlab.service.DeviceService;
import com.smartlab.service.OperationLogService;
import com.smartlab.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/borrow")
public class BorrowController {

    private final BorrowService borrowService;
    private final DeviceService deviceService;
    private final OperationLogService logService;
    private final LabService labService;
    private final UserService userService;

    public BorrowController(BorrowService borrowService, DeviceService deviceService,
                            OperationLogService logService, LabService labService,
                            UserService userService) {
        this.borrowService = borrowService;
        this.deviceService = deviceService;
        this.logService = logService;
        this.labService = labService;
        this.userService = userService;
    }

    private UserPrincipal currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserPrincipal) authentication.getPrincipal();
    }

    @GetMapping("/list")
    public Result<PageResult<Borrow>> list(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) Long userId,
                                           @RequestParam(required = false) String keyword) {
        var principal = currentUser();
        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();

        String role = principal.role();
        if ("STUDENT".equals(role) || "TEACHER".equals(role)) {
            wrapper.eq(Borrow::getUserId, principal.userId());
        } else if ("LAB_ADMIN".equals(role)) {
            List<Long> managedDeviceIds = managedDeviceIds(principal.userId());
            if (managedDeviceIds.isEmpty()) {
                return Result.success(PageResult.of(0, page, size, new ArrayList<>()));
            }
            wrapper.in(Borrow::getDeviceId, managedDeviceIds);
        } else if (userId != null) {
            wrapper.eq(Borrow::getUserId, userId);
        }

        if (status != null) {
            wrapper.eq(Borrow::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            List<Long> devIds = deviceService.list(new LambdaQueryWrapper<Device>()
                    .like(Device::getName, keyword).or().like(Device::getCode, keyword))
                    .stream().map(Device::getId).toList();
            List<Long> usrIds = userService.list(new LambdaQueryWrapper<User>()
                    .like(User::getUsername, keyword))
                    .stream().map(User::getId).toList();
            if (!devIds.isEmpty() || !usrIds.isEmpty()) {
                wrapper.and(w -> {
                    boolean first = true;
                    if (!devIds.isEmpty()) { w.in(Borrow::getDeviceId, devIds); first = false; }
                    if (!usrIds.isEmpty()) { if (!first) w.or(); w.in(Borrow::getUserId, usrIds); }
                });
            }
        }
        wrapper.orderByDesc(Borrow::getCreateTime);
        Page<Borrow> pageResult = borrowService.page(new Page<>(page, size), wrapper);
        fillBorrowInfo(pageResult.getRecords());
        return Result.success(PageResult.of(pageResult.getTotal(), page, size, pageResult.getRecords()));
    }

    @GetMapping("/all")
    public Result<List<Borrow>> all() {
        var principal = currentUser();
        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();
        String role = principal.role();
        if ("STUDENT".equals(role) || "TEACHER".equals(role)) {
            wrapper.eq(Borrow::getUserId, principal.userId());
        } else if ("LAB_ADMIN".equals(role)) {
            List<Long> managedDeviceIds = managedDeviceIds(principal.userId());
            if (managedDeviceIds.isEmpty()) {
                return Result.success(new ArrayList<>());
            }
            wrapper.in(Borrow::getDeviceId, managedDeviceIds);
        }
        List<Borrow> list = borrowService.list(wrapper);
        fillBorrowInfo(list);
        return Result.success(list);
    }

    private List<Long> managedDeviceIds(Long managerId) {
        List<Long> labIds = labService.list(new LambdaQueryWrapper<Lab>()
                .eq(Lab::getManagerId, managerId)).stream().map(Lab::getId).toList();
        if (labIds.isEmpty()) {
            return List.of();
        }
        return deviceService.list(new LambdaQueryWrapper<Device>().in(Device::getLabId, labIds))
                .stream().map(Device::getId).toList();
    }

    private void fillBorrowInfo(List<Borrow> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, Device> devMap = deviceService.listByIds(list.stream()
                .map(Borrow::getDeviceId).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Device::getId, Function.identity()));
        Map<Long, User> userMap = userService.listByIds(list.stream()
                .map(b -> java.util.Arrays.asList(b.getUserId(), b.getApproverId()))
                .flatMap(List::stream).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        for (Borrow b : list) {
            Device d = devMap.get(b.getDeviceId());
            if (d != null) {
                b.setDeviceName(d.getName());
                b.setDeviceCode(d.getCode());
                b.setDeviceImageUrl(d.getImageUrl());
                b.setLabId(d.getLabId());
            }
            User u = userMap.get(b.getUserId());
            if (u != null) {
                b.setUsername(u.getUsername());
            }
            User a = userMap.get(b.getApproverId());
            if (a != null) {
                b.setApproverName(a.getUsername());
            }
        }
    }

    @PostMapping("/apply")
    public Result<Void> apply(@RequestBody BorrowDTO dto) {
        borrowService.apply(dto);

        Device device = deviceService.getById(dto.getDeviceId());
        String deviceName = device != null ? device.getName() : "未知设备";
        logService.log("borrow", "apply", "申请借用: " + deviceName, dto.getDeviceId());
        return Result.success();
    }

    @PostMapping("/batch")
    public Result<Void> batchApply(@RequestBody List<BorrowDTO> list) {
        if (list == null || list.isEmpty()) {
            throw new BusinessException("请选择要借用的设备");
        }
        var principal = currentUser();
        String role = principal.role();
        if ("STUDENT".equals(role)) {
            if (list.size() > 3) {
                throw new BusinessException("学生一次性最多借用3台设备，当前选择了" + list.size() + "台");
            }
        } else if ("TEACHER".equals(role)) {
            if (list.size() > 100) {
                throw new BusinessException("教师一次性最多借用100台设备，当前选择了" + list.size() + "台");
            }
        }
        for (BorrowDTO dto : list) {
            borrowService.apply(dto);
        }
        logService.log("borrow", "batch", "批量申请 " + list.size() + " 台设备", null);
        return Result.success();
    }

    @PutMapping("/approve")
    public Result<Void> approve(@RequestBody ApproveDTO dto) {
        borrowService.approve(dto);
        logService.log("borrow", "approve",
                dto.getStatus() == BorrowStatus.APPROVED.code ? "审批通过" : "审批拒绝",
                dto.getBorrowId());
        return Result.success();
    }

    @PutMapping("/return/{id}")
    public Result<Void> returnDevice(@PathVariable Long id) {
        borrowService.returnDevice(id);
        Borrow borrow = borrowService.getById(id);
        logService.log("borrow", "return", "归还设备: " + (borrow != null ? deviceNameOf(borrow.getDeviceId()) : id), id);
        return Result.success();
    }

    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        borrowService.cancelBorrow(id);
        Borrow borrow = borrowService.getById(id);
        logService.log("borrow", "cancel", "取消借用: " + (borrow != null ? deviceNameOf(borrow.getDeviceId()) : id), id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        var principal = currentUser();
        if (!"ADMIN".equals(principal.role())) {
            throw new BusinessException("仅系统管理员可删除借用记录");
        }
        Borrow borrow = borrowService.getById(id);
        borrowService.removeById(id);
        logService.log("borrow", "delete", "删除借用记录: " + (borrow != null ? deviceNameOf(borrow.getDeviceId()) : id), id);
        return Result.success();
    }

    private String deviceNameOf(Long deviceId) {
        if (deviceId == null) {
            return "";
        }
        Device d = deviceService.getById(deviceId);
        return d != null ? d.getName() : "";
    }

    @PutMapping("/remind/{id}")
    public Result<Void> remindOverdue(@PathVariable Long id) {
        var principal = currentUser();
        String role = principal.role();
        if (!"ADMIN".equals(role) && !"LAB_ADMIN".equals(role)) {
            throw new BusinessException("无催还权限");
        }

        Borrow borrow = borrowService.getById(id);
        if (borrow == null) {
            throw new BusinessException("借用记录不存在");
        }
        if (!isOverdue(borrow)) {
            throw new BusinessException("该借用还未逾期，无需催还");
        }

        if ("LAB_ADMIN".equals(role) && !managedDeviceIds(principal.userId()).contains(borrow.getDeviceId())) {
            throw new BusinessException("您只能催还本实验室设备的借用");
        }

        borrow.setStatus(BorrowStatus.OVERDUE.code);
        borrow.setUpdateTime(java.time.LocalDateTime.now());
        borrowService.updateById(borrow);

        Device device = deviceService.getById(borrow.getDeviceId());
        String deviceName = device != null ? device.getName() : "未知设备";

        logService.log("borrow", "remind", "标记逾期设备: " + deviceName, id);
        return Result.success();
    }

    private boolean isOverdue(Borrow borrow) {
        if (borrow.getStatus() == null || borrow.getStatus() == BorrowStatus.RETURNED.code) {
            return false;
        }
        if (borrow.getExpectReturnTime() == null) {
            return false;
        }
        return borrow.getExpectReturnTime().isBefore(java.time.LocalDateTime.now());
    }
}