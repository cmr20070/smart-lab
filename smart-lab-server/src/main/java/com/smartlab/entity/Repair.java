package com.smartlab.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartlab.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("repair")
public class Repair extends BaseEntity {
    private Long deviceId;
    @TableField(exist = false)
    private String deviceName;
    @TableField(exist = false)
    private String deviceCode;
    @TableField(exist = false)
    private Long labId;
    private Long reporterId;
    @TableField(exist = false)
    private String reporterName;
    private LocalDateTime reportTime;
    private String faultDescription;
    private Integer faultLevel;
    private Integer status;
    private Long repairerId;
    @TableField(exist = false)
    private String deviceImageUrl;
    @TableField(exist = false)
    private String repairerName;
    private LocalDateTime repairTime;
    private LocalDateTime finishTime;
    private BigDecimal repairCost;
    private String repairDescription;
    private String remark;
}