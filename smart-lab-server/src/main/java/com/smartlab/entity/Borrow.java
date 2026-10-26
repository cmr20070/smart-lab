package com.smartlab.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartlab.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("borrow")
public class Borrow extends BaseEntity {
    private Long userId;
    @TableField(exist = false)
    private String username;
    private Long deviceId;
    @TableField(exist = false)
    private String deviceName;
    @TableField(exist = false)
    private String deviceCode;
    @TableField(exist = false)
    private String deviceImageUrl;
    @TableField(exist = false)
    private Long labId;
    private LocalDateTime borrowTime;
    private LocalDateTime expectReturnTime;
    private LocalDateTime actualReturnTime;
    private Integer status;
    private String purpose;
    private String remark;
    private Long approverId;
    @TableField(exist = false)
    private String approverName;
    private LocalDateTime approveTime;
    private String approveRemark;
}