package com.smartlab.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartlab.common.JwtUtils;
import com.smartlab.config.SecurityConfig.UserPrincipal;
import com.smartlab.dto.ChangePasswordDTO;
import com.smartlab.dto.LoginDTO;
import com.smartlab.dto.RegisterDTO;
import com.smartlab.entity.User;
import com.smartlab.exception.BusinessException;
import com.smartlab.mapper.UserMapper;
import com.smartlab.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public UserServiceImpl(PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("role", user.getRole());
        userInfo.put("phone", user.getPhone());
        userInfo.put("email", user.getEmail());
        userInfo.put("avatar", user.getAvatar());
        result.put("userInfo", userInfo);
        return result;
    }

    @Override
    public void register(RegisterDTO registerDTO) {
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        BeanUtil.copyProperties(registerDTO, user);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("STUDENT");
        } else {
            String role = user.getRole();
            if (!"STUDENT".equals(role) && !"TEACHER".equals(role)) {
                throw new BusinessException("仅允许注册学生或教师角色");
            }
        }
        user.setStatus(1);
        this.save(user);
    }

    @Override
    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new BusinessException(401, "未登录");
        }
        return this.getById(principal.userId());
    }

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        this.updateById(user);
    }

    @Override
    public void updateProfile(User user) {
        User current = getCurrentUser();
        if (user.getPassword() != null) {
            throw new BusinessException("不能通过资料接口修改密码");
        }
        // 角色不可修改
        user.setRole(null);
        // 手机号/邮箱格式校验（可选字段，非空时才校验）
        if (user.getPhone() != null && !user.getPhone().isEmpty()
                && !user.getPhone().matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("手机号格式不正确");
        }
        if (user.getEmail() != null && !user.getEmail().isEmpty()
                && !user.getEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessException("邮箱格式不正确");
        }
        // 用户名更新（可选），需校验唯一性与格式
        String newUsername = user.getUsername();
        if (newUsername == null || newUsername.isEmpty()) {
            user.setUsername(null);
        } else {
            newUsername = newUsername.trim();
            if (newUsername.length() < 3 || newUsername.length() > 20) {
                throw new BusinessException("用户名长度需为3-20位");
            }
            if (!newUsername.matches("^[A-Za-z0-9_\\u4e00-\\u9fa5]+$")) {
                throw new BusinessException("用户名只能包含字母、数字、下划线和中文");
            }
            long count = this.count(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, newUsername)
                    .ne(User::getId, current.getId()));
            if (count > 0) {
                throw new BusinessException("用户名已存在，请更换");
            }
            user.setUsername(newUsername);
        }
        user.setId(current.getId());
        this.updateById(user);
    }
}