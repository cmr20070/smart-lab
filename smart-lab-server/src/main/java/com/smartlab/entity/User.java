package com.smartlab.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartlab.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {
    private String username;
    private String password;
    private String phone;
    private String email;
    private String role;
    private String avatar;
    private Integer status;
}