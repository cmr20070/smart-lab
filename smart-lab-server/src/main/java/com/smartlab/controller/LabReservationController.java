package com.smartlab.controller;

import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.entity.LabReservation;
import com.smartlab.service.LabReservationService;
import com.smartlab.service.OperationLogService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lab-reservation")
public class LabReservationController {

    private final LabReservationService reservationService;
    private final OperationLogService logService;

    public LabReservationController(LabReservationService reservationService, OperationLogService logService) {
        this.reservationService = reservationService;
        this.logService = logService;
    }

    @PostMapping("/apply")
    public Result<Long> apply(@RequestBody java.util.Map<String, Object> body) {
        Long labId = Long.valueOf(body.get("labId").toString());
        String purpose = (String) body.getOrDefault("purpose", "");
        String reserveDate = (String) body.get("reserveDate");
        String startDate = (String) body.getOrDefault("startDate", reserveDate);
        String endDate = (String) body.getOrDefault("endDate", startDate);
        String startTime = (String) body.getOrDefault("startTime", "08:00");
        String endTime = (String) body.getOrDefault("endTime", "20:00");
        long days = reservationService.reserveMulti(labId, purpose, startDate, endDate, startTime, endTime);
        logService.log("lab-reservation", "apply", "预约实验室(ID:" + labId + "), 共" + days + "天", labId);
        return Result.success(days);
    }

    @GetMapping("/list")
    public Result<PageResult<LabReservation>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long labId) {
        return Result.success(reservationService.pageReservations(page, size, status, labId));
    }

    @PutMapping("/approve")
    public Result<Void> approve(@RequestBody java.util.Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        Integer status = Integer.valueOf(body.get("status").toString());
        String remark = (String) body.getOrDefault("remark", "");
        reservationService.approve(id, status, remark);
        return Result.success();
    }

    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        var r = reservationService.getById(id);
        if (r != null && r.getStatus() == 0) {
            r.setStatus(3);
            reservationService.updateById(r);
        }
        return Result.success();
    }

    @PutMapping("/admin-cancel/{id}")
    public Result<Void> adminCancel(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, Object> body) {
        String remark = body != null ? (String) body.getOrDefault("remark", "") : "";
        reservationService.adminCancel(id, remark);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reservationService.adminDelete(id);
        return Result.success();
    }
}