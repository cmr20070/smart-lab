package com.smartlab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.TestSupport;
import com.smartlab.common.Result;
import com.smartlab.dto.ChangePasswordDTO;
import com.smartlab.entity.User;
import com.smartlab.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @AfterEach
    void tearDown() {
        TestSupport.clear();
    }

    @Test
    void getInfo_returnsCurrentUser() {
        TestSupport.loginAsStudent(1L);
        User u = new User();
        u.setId(1L);
        when(userService.getCurrentUser()).thenReturn(u);

        Result<User> result = userController.getInfo();
        assertEquals(1L, result.getData().getId());
    }

    @Test
    void updateInfo_callsService() {
        TestSupport.loginAsStudent(1L);
        User patch = new User();
        Result<Void> result = userController.updateInfo(patch);
        assertEquals(200, result.getCode());
        verify(userService).updateProfile(patch);
    }

    @Test
    void changePassword_callsService() {
        TestSupport.loginAsStudent(1L);
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("old");
        dto.setNewPassword("new");
        Result<Void> result = userController.changePassword(dto);
        assertEquals(200, result.getCode());
        verify(userService).changePassword(dto);
    }

    @Test
    void all_filtersByRole_whenProvided() {
        when(userService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        Result<List<User>> result = userController.all("TEACHER");
        assertEquals(200, result.getCode());
        verify(userService).list(any(LambdaQueryWrapper.class));
    }

    @Test
    void all_noRole_returnsAll() {
        when(userService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        Result<List<User>> result = userController.all(null);
        assertEquals(200, result.getCode());
    }
}