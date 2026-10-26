package com.smartlab.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlab.entity.Borrow;
import com.smartlab.entity.Device;
import com.smartlab.entity.Lab;
import com.smartlab.entity.Repair;
import com.smartlab.entity.User;
import com.smartlab.service.BorrowService;
import com.smartlab.service.DeviceService;
import com.smartlab.service.LabService;
import com.smartlab.service.OperationLogService;
import com.smartlab.service.RepairService;
import com.smartlab.service.UserService;
import com.smartlab.config.SecurityConfig;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final DeviceService deviceService;
    private final BorrowService borrowService;
    private final RepairService repairService;
    private final OperationLogService logService;
    private final LabService labService;
    private final UserService userService;

    public ExportController(DeviceService deviceService, BorrowService borrowService,
                            RepairService repairService, OperationLogService logService,
                            LabService labService, UserService userService) {
        this.deviceService = deviceService;
        this.borrowService = borrowService;
        this.repairService = repairService;
        this.logService = logService;
        this.labService = labService;
        this.userService = userService;
    }

    @GetMapping("/device")
    public void exportDevice(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String category,
                              @RequestParam(required = false) Integer status,
                              HttpServletResponse response) throws Exception {
        LambdaQueryWrapper<Device> wrapper = buildDeviceWrapper(keyword, category, status);
        List<Device> list = deviceService.list(wrapper);

        List<Map<String, Object>> rows = list.stream().map(d -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("设备名称", d.getName());
            row.put("设备编号", d.getCode());
            row.put("分类", d.getCategory());
            row.put("型号", d.getModel());
            row.put("规格", d.getSpecification());
            row.put("生产厂家", d.getManufacturer());
            row.put("价格", d.getPrice());
            Lab lab = labService.getById(d.getLabId());
            row.put("所属实验室", lab != null ? lab.getName() : "");
            row.put("状态", statusLabel(d.getStatus()));
            return row;
        }).collect(Collectors.toList());

        writeExcel(response, "设备列表", rows);
        logService.log("device", "export", "导出设备列表", null);
    }

    private String currentRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return ((SecurityConfig.UserPrincipal) auth.getPrincipal()).role();
    }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return ((SecurityConfig.UserPrincipal) auth.getPrincipal()).userId();
    }

    @GetMapping("/borrow")
    public void exportBorrow(@RequestParam(required = false) Integer status,
                              @RequestParam(required = false) String keyword,
                              HttpServletResponse response) throws Exception {
        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();
        String role = currentRole();
        if ("TEACHER".equals(role)) {
            wrapper.eq(Borrow::getUserId, currentUserId());
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
        List<Borrow> list = borrowService.list(wrapper);

        Map<Long, Device> devMap = deviceService.listByIds(list.stream()
                .map(Borrow::getDeviceId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Device::getId, Function.identity()));
        Map<Long, User> usrMap = userService.listByIds(list.stream()
                .map(Borrow::getUserId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<Map<String, Object>> rows = list.stream().map(b -> {
            Map<String, Object> row = new LinkedHashMap<>();
            Device d = devMap.get(b.getDeviceId());
            User u = usrMap.get(b.getUserId());
            row.put("设备名称", d != null ? d.getName() : "");
            row.put("设备编号", d != null ? d.getCode() : "");
            row.put("借用人", u != null ? u.getUsername() : "");
            row.put("借用目的", b.getPurpose());
            row.put("借用时间", b.getBorrowTime());
            row.put("预计归还", b.getExpectReturnTime());
            row.put("实际归还", b.getActualReturnTime());
            row.put("状态", borrowStatusLabel(b.getStatus()));
            return row;
        }).collect(Collectors.toList());

        writeExcel(response, "借用记录", rows);
        logService.log("borrow", "export", "导出借用记录", null);
    }

    @GetMapping("/repair")
    public void exportRepair(@RequestParam(required = false) Integer status,
                              @RequestParam(required = false) String keyword,
                              HttpServletResponse response) throws Exception {
        LambdaQueryWrapper<Repair> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Repair::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            List<Long> devIds = deviceService.list(new LambdaQueryWrapper<Device>()
                    .like(Device::getName, keyword).or().like(Device::getCode, keyword))
                    .stream().map(Device::getId).toList();
            if (!devIds.isEmpty()) {
                wrapper.in(Repair::getDeviceId, devIds);
            }
        }
        wrapper.orderByDesc(Repair::getCreateTime);
        List<Repair> list = repairService.list(wrapper);

        Map<Long, Device> devMap = deviceService.listByIds(list.stream()
                .map(Repair::getDeviceId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Device::getId, Function.identity()));
        Map<Long, User> usrMap = userService.listByIds(list.stream()
                .map(Repair::getReporterId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<Map<String, Object>> rows = list.stream().map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            Device d = devMap.get(r.getDeviceId());
            User u = usrMap.get(r.getReporterId());
            row.put("设备名称", d != null ? d.getName() : "");
            row.put("设备编号", d != null ? d.getCode() : "");
            row.put("报修人", u != null ? u.getUsername() : "");
            row.put("故障描述", r.getFaultDescription());
            row.put("故障级别", levelLabel(r.getFaultLevel()));
            row.put("状态", repairStatusLabel(r.getStatus()));
            row.put("报修时间", r.getReportTime());
            return row;
        }).collect(Collectors.toList());

        writeExcel(response, "维修记录", rows);
        logService.log("repair", "export", "导出维修记录", null);
    }

    private LambdaQueryWrapper<Device> buildDeviceWrapper(String keyword, String category, Integer status) {
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
        wrapper.orderByDesc(Device::getCreateTime);
        return wrapper;
    }

    private void writeExcel(HttpServletResponse response, String fileName, List<Map<String, Object>> rows) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=" + encodedName + ".xlsx");

        ExcelWriter writer = ExcelUtil.getWriter(true);
        if (!rows.isEmpty()) {
            for (String key : rows.get(0).keySet()) {
                writer.addHeaderAlias(key, key);
            }
            writer.setOnlyAlias(true);
            writer.write(rows, true);
        }

        ServletOutputStream out = response.getOutputStream();
        writer.flush(out, true);
        writer.close();
        out.close();
    }

    private String statusLabel(Integer s) {
        Map<Integer, String> map = Map.of(
                1, "空闲",
                2, "借出",
                3, "维修中",
                4, "待实验室管理员审核",
                5, "已报废",
                6, "待管理员终审"
        );
        return map.getOrDefault(s, "未知");
    }

    private String borrowStatusLabel(Integer s) {
        Map<Integer, String> map = Map.of(
                0, "待审批",
                1, "已批准(借出中)",
                2, "已拒绝",
                3, "已归还",
                4, "已取消",
                5, "逾期"
        );
        return map.getOrDefault(s, "未知");
    }

    private String repairStatusLabel(Integer s) {
        Map<Integer, String> map = Map.of(0, "待处理", 1, "维修中", 2, "已完成");
        return map.getOrDefault(s, "未知");
    }

    private String levelLabel(Integer l) {
        Map<Integer, String> map = Map.of(1, "一般", 2, "紧急", 3, "严重");
        return map.getOrDefault(l, "未知");
    }
}