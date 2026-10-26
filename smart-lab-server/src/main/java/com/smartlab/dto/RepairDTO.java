package com.smartlab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RepairDTO {
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    @NotBlank(message = "故障描述不能为空")
    private String faultDescription;

    @NotNull(message = "故障级别不能为空")
    private Integer faultLevel;
    private String remark;
}
