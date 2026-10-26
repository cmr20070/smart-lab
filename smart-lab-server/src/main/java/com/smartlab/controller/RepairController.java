package com.smartlab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.config.SecurityConfig.UserPrincipal;
import com.smartlab.dto.RepairDTO;
import com.smartlab.entity.Repair;
import com.smartlab.entity.Device;
import com.smartlab.entity.User;
import com.smartlab.entity.Lab;
import com.smartlab.enums.RepairStatus;
import com.smartlab.service.DeviceService;
import com.smartlab.service.LabService;
import com.smartlab.service.OperationLogService;
import com.smartlab.service.UserService;
import com.smartlab.exception.BusinessException;
import com.smartlab.service.RepairService;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/repair")
public class RepairController {

    private final RepairService repairService;
    private final OperationLogService logService;
    private final LabService labService;
    private final DeviceService deviceService;
    private final UserService userService;

    public RepairController(RepairService repairService, OperationLogService logService,
                            LabService labService, DeviceService deviceService, UserService userService) {
        this.repairService = repairService;
        this.logService = logService;
        this.labService = labService;
        this.deviceService = deviceService;
        this.userService = userService;
    }

    @GetMapping("/list")
    public Result<PageResult<Repair>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) Integer faultLevel,
                                            @RequestParam(required = false) String keyword) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = (UserPrincipal) authentication.getPrincipal();

        LambdaQueryWrapper<Repair> wrapper = new LambdaQueryWrapper<>();
        if ("STUDENT".equals(principal.role()) || "TEACHER".equals(principal.role())) {
            wrapper.eq(Repair::getReporterId, principal.userId());
        }
        if (status != null) {
            wrapper.eq(Repair::getStatus, status);
        }
        if (faultLevel != null) {
            wrapper.eq(Repair::getFaultLevel, faultLevel);
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
                    if (!devIds.isEmpty()) { w.in(Repair::getDeviceId, devIds); first = false; }
                    if (!usrIds.isEmpty()) { if (!first) w.or(); w.in(Repair::getReporterId, usrIds); }
                });
            }
        }
        wrapper.orderByDesc(Repair::getCreateTime);
        Page<Repair> pageResult = repairService.page(new Page<>(page, size), wrapper);
        fillRepairInfo(pageResult.getRecords());
        return Result.success(PageResult.of(pageResult.getTotal(), page, size, pageResult.getRecords()));
    }

    private void fillRepairInfo(List<Repair> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, Device> devMap = deviceService.listByIds(list.stream()
                .map(Repair::getDeviceId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Device::getId, Function.identity()));
        Map<Long, User> userMap = userService.listByIds(list.stream()
                .map(r -> java.util.Arrays.asList(r.getReporterId(), r.getRepairerId()))
                .flatMap(List::stream).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        for (Repair r : list) {
            Device d = devMap.get(r.getDeviceId());
            if (d != null) {
                r.setDeviceName(d.getName());
                r.setDeviceCode(d.getCode());
                r.setLabId(d.getLabId());
                r.setDeviceImageUrl(d.getImageUrl());
            }
            User rep = userMap.get(r.getReporterId());
            if (rep != null) {
                r.setReporterName(rep.getUsername());
            }
            User repa = userMap.get(r.getRepairerId());
            if (repa != null) {
                r.setRepairerName(repa.getUsername());
            }
        }
    }

    @PostMapping("/report")
    public Result<Void> report(@Valid @RequestBody RepairDTO dto) {
        repairService.reportFault(dto);
        logService.log("repair", "report", "报修设备(ID: " + dto.getDeviceId() + ")", dto.getDeviceId());
        return Result.success();
    }

    @PutMapping("/assign/{id}")
    public Result<Void> assign(@PathVariable Long id) {
        Repair repair = repairService.getById(id);
        repairService.assignRepair(id);
        logService.log("repair", "assign", "接修任务: " + (repair != null ? deviceNameOf(repair.getDeviceId()) : id), id);
        return Result.success();
    }

    @PutMapping("/complete/{id}")
    public Result<Void> complete(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String description = (String) body.getOrDefault("repairDescription", "");
        BigDecimal cost = body.get("repairCost") != null
                ? new BigDecimal(body.get("repairCost").toString()) : BigDecimal.ZERO;
        Repair repair = repairService.getById(id);
        repairService.completeRepair(id, description, cost);
        logService.log("repair", "complete", "完成维修: " + (repair != null ? deviceNameOf(repair.getDeviceId()) : id), id);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Repair repair) {
        Repair exist = repairService.getById(id);
        if (exist == null) {
            throw new BusinessException("维修记录不存在");
        }
        repair.setId(id);
        repairService.updateById(repair);
        logService.log("repair", "update", "编辑维修记录(ID: " + id + ")", id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        var principal = SecurityContextHolder.getContext().getAuthentication();
        var user = (com.smartlab.config.SecurityConfig.UserPrincipal) principal.getPrincipal();
        Repair repair = repairService.getById(id);
        if (repair == null) {
            throw new BusinessException("维修记录不存在");
        }
        if ("LAB_ADMIN".equals(user.role())) {
            Device dev = deviceService.getById(repair.getDeviceId());
            Long labId = dev != null ? dev.getLabId() : null;
            long managedCount = labService.count(new LambdaQueryWrapper<Lab>()
                    .eq(Lab::getManagerId, user.userId())
                    .eq(Lab::getId, labId));
            if (managedCount == 0) {
                throw new BusinessException("您不是该设备所属实验室的负责人，无权删除");
            }
        } else if (!"ADMIN".equals(user.role())) {
            throw new BusinessException("无权删除维修记录");
        }
        repairService.removeById(id);
        logService.log("repair", "delete", "删除维修记录: " + deviceNameOf(repair.getDeviceId()), id);
        return Result.success();
    }

    private String deviceNameOf(Long deviceId) {
        if (deviceId == null) {
            return "";
        }
        Device d = deviceService.getById(deviceId);
        return d != null ? d.getName() : "";
    }
}