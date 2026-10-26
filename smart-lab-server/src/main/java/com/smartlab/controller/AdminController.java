package com.smartlab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.common.Result;
import com.smartlab.config.SecurityConfig;
import com.smartlab.entity.Borrow;
import com.smartlab.entity.Repair;
import com.smartlab.entity.User;
import com.smartlab.exception.BusinessException;
import com.smartlab.service.BorrowService;
import com.smartlab.service.RepairService;
import com.smartlab.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final BorrowService borrowService;
    private final RepairService repairService;

    public AdminController(UserService userService, PasswordEncoder passwordEncoder,
                           BorrowService borrowService, RepairService repairService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.borrowService = borrowService;
        this.repairService = repairService;
    }

    @GetMapping("/user/list")
    public Result<Page<User>> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getEmail, keyword));
        }
        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userService.page(new Page<>(page, size), wrapper);
        return Result.success(result);
    }

    @PostMapping("/user")
    public Result<Void> add(@RequestBody User user) {
        long count = userService.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername()));
        if (count > 0) {
            return Result.error(400, "用户名已存在");
        }
        String err = validateContact(user);
        if (err != null) {
            return Result.error(400, err);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userService.save(user);
        return Result.success();
    }

    @PutMapping("/user/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody User user) {
        User exist = userService.getById(id);
        if (exist == null) {
            return Result.error(404, "用户不存在");
        }
        user.setId(id);
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        String usernameErr = validateUsername(user, id);
        if (usernameErr != null) {
            return Result.error(400, usernameErr);
        }
        String err = validateContact(user);
        if (err != null) {
            return Result.error(400, err);
        }
        userService.updateById(user);
        return Result.success();
    }

    @DeleteMapping("/user/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var currentUser = (SecurityConfig.UserPrincipal) auth.getPrincipal();
        if (currentUser.userId().equals(id)) {
            throw new BusinessException("不能删除自己的账号");
        }
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        long activeBorrows = borrowService.count(new LambdaQueryWrapper<Borrow>()
                .eq(Borrow::getUserId, id)
                .in(Borrow::getStatus, 0, 1, 5));
        if (activeBorrows > 0) {
            throw new BusinessException("该用户存在未完成的借用记录，无法删除");
        }
        long activeRepairs = repairService.count(new LambdaQueryWrapper<Repair>()
                .eq(Repair::getReporterId, id)
                .eq(Repair::getStatus, 0));
        if (activeRepairs > 0) {
            throw new BusinessException("该用户存在待处理的维修申请，无法删除");
        }
        userService.removeById(id);
        return Result.success();
    }

    @PutMapping("/user/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setStatus(status);
        userService.updateById(user);
        return Result.success();
    }

    private String validateUsername(User user, Long id) {
        String username = user.getUsername();
        if (username == null || username.trim().isEmpty()) {
            user.setUsername(null);
            return null;
        }
        username = username.trim();
        if (username.length() < 3 || username.length() > 20) {
            return "用户名长度需为3-20位";
        }
        if (!username.matches("^[A-Za-z0-9_\\u4e00-\\u9fa5]+$")) {
            return "用户名只能包含字母、数字、下划线和中文";
        }
        user.setUsername(username);
        long count = userService.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .ne(User::getId, id));
        if (count > 0) {
            return "用户名已存在";
        }
        return null;
    }

    private String validateContact(User user) {
        String phone = user.getPhone();
        if (phone != null && !phone.isEmpty() && !phone.matches("^1[3-9]\\d{9}$")) {
            return "手机号格式不正确";
        }
        String email = user.getEmail();
        if (email != null && !email.isEmpty() &&
                !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return "邮箱格式不正确";
        }
        return null;
    }
}