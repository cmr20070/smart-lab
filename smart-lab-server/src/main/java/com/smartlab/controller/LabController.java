package com.smartlab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.entity.Device;
import com.smartlab.entity.Lab;
import com.smartlab.exception.BusinessException;
import com.smartlab.service.DeviceService;
import com.smartlab.service.LabService;
import com.smartlab.service.OperationLogService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lab")
public class LabController {

    private final LabService labService;
    private final OperationLogService logService;
    private final DeviceService deviceService;

    public LabController(LabService labService, OperationLogService logService, DeviceService deviceService) {
        this.labService = labService;
        this.logService = logService;
        this.deviceService = deviceService;
    }

    @GetMapping("/list")
    public Result<PageResult<Lab>> list(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String category) {
        LambdaQueryWrapper<Lab> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Lab::getName, keyword).or().like(Lab::getCode, keyword));
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Lab::getCategory, category);
        }
        wrapper.orderByDesc(Lab::getCreateTime);
        Page<Lab> pageResult = labService.page(new Page<>(page, size), wrapper);
        return Result.success(PageResult.of(pageResult.getTotal(), page, size, pageResult.getRecords()));
    }

    @GetMapping("/all")
    public Result<java.util.List<Lab>> all() {
        return Result.success(labService.list());
    }

    @PostMapping
    public Result<Void> add(@RequestBody Lab lab) {
        labService.save(lab);
        logService.log("lab", "add", "添加实验室: " + lab.getName(), lab.getId());
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Lab lab) {
        lab.setId(id);
        labService.updateById(lab);
        logService.log("lab", "edit", "编辑实验室: " + lab.getName(), id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Lab lab = labService.getById(id);
        if (lab == null) {
            throw new BusinessException("实验室不存在");
        }
        long deviceCount = deviceService.count(new LambdaQueryWrapper<Device>()
                .eq(Device::getLabId, id));
        if (deviceCount > 0) {
            throw new BusinessException("该实验室下还有 " + deviceCount + " 台设备，无法删除");
        }
        labService.removeById(id);
        logService.log("lab", "delete", "删除实验室: " + lab.getName(), id);
        return Result.success();
    }
}