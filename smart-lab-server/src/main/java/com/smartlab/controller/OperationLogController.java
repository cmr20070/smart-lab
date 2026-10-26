package com.smartlab.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.entity.OperationLog;
import com.smartlab.entity.User;
import com.smartlab.service.OperationLogService;
import com.smartlab.service.UserService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/log")
public class OperationLogController {

    private final OperationLogService logService;
    private final UserService userService;

    public OperationLogController(OperationLogService logService, UserService userService) {
        this.logService = logService;
        this.userService = userService;
    }

    @GetMapping("/list")
    public Result<PageResult<OperationLog>> list(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(required = false) String module,
                                                  @RequestParam(required = false) String action,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String startTime,
                                                  @RequestParam(required = false) String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (module != null && !module.isEmpty()) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (action != null && !action.isEmpty()) {
            wrapper.eq(OperationLog::getAction, action);
        }
        if (keyword != null && !keyword.isEmpty()) {
            applyKeyword(wrapper, keyword);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(OperationLog::getCreateTime, LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(OperationLog::getCreateTime, LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);

        Page<OperationLog> pageResult = logService.page(new Page<>(page, size), wrapper);
        fillOperator(pageResult.getRecords());
        return Result.success(PageResult.of(pageResult.getTotal(), page, size, pageResult.getRecords()));
    }

    private void applyKeyword(LambdaQueryWrapper<OperationLog> wrapper, String keyword) {
        List<Long> usrIds = userService.list(new LambdaQueryWrapper<User>()
                .like(User::getUsername, keyword)).stream().map(User::getId).toList();
        if (!usrIds.isEmpty()) {
            wrapper.and(w -> w.in(OperationLog::getUserId, usrIds)
                    .or().like(OperationLog::getDescription, keyword));
        } else {
            wrapper.like(OperationLog::getDescription, keyword);
        }
    }

    private void fillOperator(List<OperationLog> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = userService.listByIds(list.stream()
                .map(OperationLog::getUserId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, User::getUsername));
        for (OperationLog log : list) {
            if (log.getUserId() != null) {
                log.setUsername(nameMap.get(log.getUserId()));
            }
        }
    }

    @GetMapping("/module-stats")
    public Result<Map<String, Long>> moduleStats() {
        List<OperationLog> list = logService.list();
        Map<String, Long> stats = new HashMap<>();
        for (OperationLog log : list) {
            stats.merge(log.getModule(), 1L, Long::sum);
        }
        return Result.success(stats);
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false) String module,
                       @RequestParam(required = false) String action,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       HttpServletResponse response) throws Exception {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (module != null && !module.isEmpty()) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (action != null && !action.isEmpty()) {
            wrapper.eq(OperationLog::getAction, action);
        }
        if (keyword != null && !keyword.isEmpty()) {
            applyKeyword(wrapper, keyword);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(OperationLog::getCreateTime, LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(OperationLog::getCreateTime, LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);

        List<OperationLog> list = logService.list(wrapper);
        fillOperator(list);

        Map<String, String> moduleMap = Map.of("user", "用户管理", "lab", "实验室管理", "device", "设备管理", "borrow", "借用预约", "repair", "维修维护");
        Map<String, String> actionMap = Map.ofEntries(
                Map.entry("add", "新增"), Map.entry("edit", "编辑"), Map.entry("delete", "删除"),
                Map.entry("apply", "申请"), Map.entry("approve", "审批通过"), Map.entry("reject", "审批拒绝"),
                Map.entry("return", "归还"), Map.entry("cancel", "取消"), Map.entry("report", "报修"),
                Map.entry("assign", "分配"), Map.entry("complete", "完成"), Map.entry("status", "改状态"),
                Map.entry("enable", "启用"), Map.entry("disable", "禁用"), Map.entry("discard", "报废申请"),
                Map.entry("discardApprove", "报废通过"), Map.entry("discardReject", "报废驳回"), Map.entry("export", "导出")
        );
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            OperationLog log = list.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("序号", i + 1);
            row.put("操作人", log.getUsername() != null ? log.getUsername() : "系统");
            row.put("操作模块", moduleMap.getOrDefault(log.getModule(), log.getModule()));
            row.put("操作类型", actionMap.getOrDefault(log.getAction(), log.getAction()));
            row.put("操作描述", log.getDescription());
            row.put("IP地址", log.getIp() != null ? log.getIp() : "-");
            row.put("操作时间", log.getCreateTime() != null ? log.getCreateTime().format(dtf) : "");
            rows.add(row);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String encodedName = URLEncoder.encode("操作日志", StandardCharsets.UTF_8).replace("+", "%20");
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
}