package com.smartlab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BorrowDTO {
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    private LocalDateTime borrowTime;

    @NotNull(message = "预计归还时间不能为空")
    private LocalDateTime expectReturnTime;

    @NotBlank(message = "借用目的不能为空")
    private String purpose;
    private String remark;

    private Long studentId;
    private String studentName;
}