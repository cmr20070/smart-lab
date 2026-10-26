package com.smartlab.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartlab.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lab")
public class Lab extends BaseEntity {
    private String name;
    private String code;
    private String location;
    private Integer capacity;
    private String category;
    private String description;
    private String imageUrl;
    private Long managerId;
    private Integer status;
}