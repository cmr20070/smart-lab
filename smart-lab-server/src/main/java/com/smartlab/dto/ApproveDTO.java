package com.smartlab.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApproveDTO {
    @NotNull(message = "借用记录ID不能为空")
    private Long borrowId;

    @NotNull(message = "审批状态不能为空")
    private Integer status;
    private String approveRemark;
}
