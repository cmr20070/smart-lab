package com.smartlab.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartlab.common.PageResult;
import com.smartlab.entity.LabReservation;

import java.util.Map;

public interface LabReservationService extends IService<LabReservation> {
    long reserveMulti(Long labId, String purpose, String startDate, String endDate, String startTime, String endTime);
    void approve(Long reservationId, Integer status, String approveRemark);
    PageResult<LabReservation> pageReservations(int page, int size, Integer status, Long labId);
    void adminCancel(Long id, String remark);
    void adminDelete(Long id);
}