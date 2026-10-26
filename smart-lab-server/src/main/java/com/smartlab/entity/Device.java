package com.smartlab.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartlab.common.BaseEntity;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device")
public class Device extends BaseEntity {
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过100")
    private String name;

    @NotBlank(message = "设备编号不能为空")
    @Size(max = 50, message = "设备编号长度不能超过50")
    private String code;

    @Size(max = 50, message = "设备分类长度不能超过50")
    private String category;

    @Size(max = 100, message = "型号长度不能超过100")
    private String model;

    @Size(max = 500, message = "规格长度不能超过500")
    private String specification;

    @Size(max = 100, message = "制造商长度不能超过100")
    private String manufacturer;

    private LocalDate purchaseDate;

    @DecimalMin(value = "0.0", message = "价格不能为负数")
    private BigDecimal price;

    private Long labId;

    private Integer status;

    @Size(max = 500, message = "图片URL长度不能超过500")
    private String imageUrl;

    @Size(max = 1000, message = "描述长度不能超过1000")
    private String description;
}