package com.smartlab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlab.entity.Borrow;
import com.smartlab.entity.Device;
import com.smartlab.entity.Lab;
import com.smartlab.entity.Repair;
import com.smartlab.entity.User;
import com.smartlab.service.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final DeviceService deviceService;
    private final LabService labService;
    private final BorrowService borrowService;
    private final RepairService repairService;
    private final UserService userService;

    public StatisticsServiceImpl(DeviceService deviceService, LabService labService,
                                  BorrowService borrowService, RepairService repairService,
                                  UserService userService) {
        this.deviceService = deviceService;
        this.labService = labService;
        this.borrowService = borrowService;
        this.repairService = repairService;
        this.userService = userService;
    }

    @Override
    public Map<String, Object> getDeviceStatusStats() {
        List<Device> devices = deviceService.list(new LambdaQueryWrapper<>());
        Map<String, Object> result = new HashMap<>();
        result.put("total", devices.size());

        Map<Integer, Long> statusCount = devices.stream()
                .collect(Collectors.groupingBy(Device::getStatus, Collectors.counting()));
        result.put("idle", statusCount.getOrDefault(1, 0L));
        result.put("borrowed", statusCount.getOrDefault(2, 0L));
        result.put("repairing", statusCount.getOrDefault(3, 0L));
        result.put("pendingDiscard", statusCount.getOrDefault(4, 0L));
        result.put("pendingFinal", statusCount.getOrDefault(6, 0L));
        result.put("scrapped", statusCount.getOrDefault(5, 0L));

        Map<String, Long> categoryCount = devices.stream()
                .collect(Collectors.groupingBy(d -> d.getCategory() != null ? d.getCategory() : "其他",
                        Collectors.counting()));
        result.put("categoryDistribution", categoryCount);

        return result;
    }

    @Override
    public Map<String, Object> getBorrowStats() {
        List<Borrow> borrows = borrowService.list();
        Map<String, Object> result = new HashMap<>();
        result.put("total", borrows.size());

        Map<Integer, Long> statusCount = borrows.stream()
                .collect(Collectors.groupingBy(Borrow::getStatus, Collectors.counting()));
        result.put("pending", statusCount.getOrDefault(0, 0L));
        result.put("approved", statusCount.getOrDefault(1, 0L));
        result.put("rejected", statusCount.getOrDefault(2, 0L));
        result.put("returned", statusCount.getOrDefault(3, 0L));
        result.put("cancelled", statusCount.getOrDefault(4, 0L));
        result.put("overdue", statusCount.getOrDefault(5, 0L));

        Map<Long, List<Borrow>> byDevice = borrows.stream()
                .filter(b -> b.getDeviceId() != null)
                .collect(Collectors.groupingBy(Borrow::getDeviceId));
        Map<Long, String> deviceNameById = deviceService.listByIds(byDevice.keySet()).stream()
                .collect(Collectors.toMap(Device::getId, Device::getName));
        List<Map<String, Object>> topDevices = byDevice.entrySet().stream()
                .collect(Collectors.toMap(e -> deviceNameById.getOrDefault(e.getKey(), "未知"),
                        e -> (long) e.getValue().size(), Long::sum))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        result.put("topDevices", topDevices);

        return result;
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("userCount", userService.count());
        result.put("labCount", labService.count());
        long deviceTotal = deviceService.count();
        result.put("deviceCount", deviceTotal);

        long pendingBorrow = borrowService.count(new LambdaQueryWrapper<Borrow>().eq(Borrow::getStatus, 0));
        long pendingRepair = repairService.count(new LambdaQueryWrapper<Repair>().eq(Repair::getStatus, 0));
        result.put("pendingBorrow", pendingBorrow);
        result.put("pendingRepair", pendingRepair);
        result.put("pendingTotal", pendingBorrow + pendingRepair);

        long overdueCount = borrowService.count(new LambdaQueryWrapper<Borrow>()
                .in(Borrow::getStatus, 1, 5)
                .lt(Borrow::getExpectReturnTime, java.time.LocalDateTime.now()));
        result.put("overdueCount", overdueCount);

        long borrowed = deviceService.count(new LambdaQueryWrapper<Device>().eq(Device::getStatus, 2));
        long repairing = deviceService.count(new LambdaQueryWrapper<Device>().eq(Device::getStatus, 3));
        long utilization = deviceTotal > 0 ? Math.round((borrowed + repairing) * 100.0 / deviceTotal) : 0;
        result.put("utilization", utilization);

        return result;
    }
}