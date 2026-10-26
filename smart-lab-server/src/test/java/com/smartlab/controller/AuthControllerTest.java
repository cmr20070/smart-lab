package com.smartlab.controller;

import com.smartlab.common.Result;
import com.smartlab.dto.LoginDTO;
import com.smartlab.dto.RegisterDTO;
import com.smartlab.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_returnsResultWithToken() {
        java.util.Map<String, Object> data = java.util.Map.of("token", "abc");
        when(userService.login(any(LoginDTO.class))).thenReturn(data);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("zhangsan");
        dto.setPassword("123456");

        Result<java.util.Map<String, Object>> result = authController.login(dto);
        assertEquals(200, result.getCode());
        assertEquals("abc", result.getData().get("token"));
    }

    @Test
    void register_callsService() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("newuser");
        dto.setPassword("123456");

        Result<Void> result = authController.register(dto);

        assertEquals(200, result.getCode());
        verify(userService).register(dto);
    }
}