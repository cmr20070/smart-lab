package com.smartlab.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceStatusDTO {

    @NotNull(message = "目标状态不能为空")
    private Integer status;

    private Long operatorId;
    private String operatorName;

    private String purpose;
    private String remark;
    private LocalDateTime expectReturnTime;

    private String faultDescription;
    private Integer faultLevel;
    private String repairDescription;
}