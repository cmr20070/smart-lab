package com.smartlab.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartlab.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lab_reservation")
public class LabReservation extends BaseEntity {
    private Long labId;
    @TableField(exist = false)
    private String labName;
    private Long userId;
    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String userRole;
    private String purpose;
    private LocalDate reserveDate;
    private LocalDate endDate;
    private String startTime;
    private String endTime;
    private Integer status;
    private Long approverId;
    @TableField(exist = false)
    private String approverName;
    private LocalDateTime approveTime;
    private String approveRemark;
}