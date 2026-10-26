package com.smartlab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlab.common.Result;
import com.smartlab.dto.ChangePasswordDTO;
import com.smartlab.entity.User;
import com.smartlab.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/info")
    public Result<User> getInfo() {
        return Result.success(userService.getCurrentUser());
    }

    @PutMapping("/info")
    public Result<Void> updateInfo(@RequestBody User user) {
        userService.updateProfile(user);
        return Result.success();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return Result.success();
    }

    @GetMapping("/all")
    public Result<List<User>> all(@RequestParam(required = false) String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.select(User::getId, User::getUsername, User::getPhone, User::getEmail, User::getRole, User::getAvatar);
        wrapper.orderByAsc(User::getId);
        return Result.success(userService.list(wrapper));
    }
}