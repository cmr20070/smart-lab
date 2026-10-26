package com.smartlab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartlab.dto.ChangePasswordDTO;
import com.smartlab.dto.LoginDTO;
import com.smartlab.dto.RegisterDTO;
import com.smartlab.entity.User;
import java.util.Map;

public interface UserService extends IService<User> {
    Map<String, Object> login(LoginDTO loginDTO);
    void register(RegisterDTO registerDTO);
    User getCurrentUser();
    void changePassword(ChangePasswordDTO dto);
    void updateProfile(User user);
}