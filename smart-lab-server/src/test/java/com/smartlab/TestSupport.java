package com.smartlab;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.smartlab.config.SecurityConfig.UserPrincipal;
import com.smartlab.entity.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * 测试支持工具：
 * 1. 模拟登录用户（写入 SecurityContextHolder）；
 * 2. 初始化 MyBatis-Plus 表元数据，使 LambdaQueryWrapper/LambdaUpdateWrapper
 *    在纯单元测试环境中也能解析实体列（否则会抛 “can not find lambda cache”）。
 */
public final class TestSupport {

    static {
        initMybatisPlus();
    }

    private TestSupport() {
    }

    /**
     * 预先注册各实体类的表元数据，供 Lambda 解析使用。
     * 在纯 JUnit（未启动 Spring 容器）环境下，MyBatis-Plus 不会自动收集实体，
     * 因此这里通过 TableInfoHelper 手动注册。
     */
    public static void initMybatisPlus() {
        try {
            MybatisConfiguration configuration = new MybatisConfiguration();
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
            for (Class<?> clazz : new Class<?>[]{
                    User.class, Device.class, Borrow.class, Lab.class,
                    LabReservation.class, Repair.class,
                    OperationLog.class}) {
                try {
                    TableInfoHelper.initTableInfo(assistant, clazz);
                } catch (Exception ignored) {
                    // 个别为 @TableField(exist=false) 字段的重复注册可忽略
                }
            }
        } catch (Exception ignored) {
            // 若环境不支持，抛出的异常由具体用例体现
        }
    }

    public static void loginAsAdmin(Long userId) {
        login(new UserPrincipal(userId, "admin", "ADMIN"));
    }

    public static void loginAsLabAdmin(Long userId) {
        login(new UserPrincipal(userId, "labadmin", "LAB_ADMIN"));
    }

    public static void loginAsTeacher(Long userId) {
        login(new UserPrincipal(userId, "teacher", "TEACHER"));
    }

    public static void loginAsStudent(Long userId) {
        login(new UserPrincipal(userId, "student", "STUDENT"));
    }

    public static void login(UserPrincipal principal) {
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority(principal.role())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}