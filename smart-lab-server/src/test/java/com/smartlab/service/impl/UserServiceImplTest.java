package com.smartlab.service.impl;

import com.smartlab.TestSupport;
import com.smartlab.common.JwtUtils;
import com.smartlab.dto.ChangePasswordDTO;
import com.smartlab.dto.LoginDTO;
import com.smartlab.dto.RegisterDTO;
import com.smartlab.entity.User;
import com.smartlab.exception.BusinessException;
import com.smartlab.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clear();
    }

    private User user() {
        User u = new User();
        u.setId(1L);
        u.setUsername("zhangsan");
        u.setPassword("encoded");
        u.setRole("STUDENT");
        u.setStatus(1);
        return u;
    }

    @Test
    void login_success_returnsTokenAndUserInfo() {
        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(user());
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);
        when(jwtUtils.generateToken(1L, "zhangsan", "STUDENT")).thenReturn("jwt-token");

        LoginDTO dto = new LoginDTO();
        dto.setUsername("zhangsan");
        dto.setPassword("123456");

        Map<String, Object> result = userService.login(dto);

        assertEquals("jwt-token", result.get("token"));
        Map<String, Object> info = (Map<String, Object>) result.get("userInfo");
        assertEquals(1L, info.get("id"));
        assertEquals("STUDENT", info.get("role"));
        verify(jwtUtils).generateToken(1L, "zhangsan", "STUDENT");
    }

    @Test
    void login_userNotFound_throws() {
        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        LoginDTO dto = new LoginDTO();
        dto.setUsername("x");
        dto.setPassword("y");
        assertThrows(BusinessException.class, () -> userService.login(dto));
    }

    @Test
    void login_disabledAccount_throws() {
        User u = user();
        u.setStatus(0);
        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(u);
        LoginDTO dto = new LoginDTO();
        dto.setUsername("zhangsan");
        dto.setPassword("123456");
        assertThrows(BusinessException.class, () -> userService.login(dto));
    }

    @Test
    void login_wrongPassword_throws() {
        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(user());
        when(passwordEncoder.matches("bad", "encoded")).thenReturn(false);
        LoginDTO dto = new LoginDTO();
        dto.setUsername("zhangsan");
        dto.setPassword("bad");
        assertThrows(BusinessException.class, () -> userService.login(dto));
    }

    @Test
    void register_usernameExists_throws() {
        when(userMapper.selectCount(any())).thenReturn(1L);
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("zhangsan");
        dto.setPassword("123456");
        assertThrows(BusinessException.class, () -> userService.register(dto));
    }

    @Test
    void register_defaultRoleStudent() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("hashed");
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("newuser");
        dto.setPassword("123456");

        userService.register(dto);

        verify(userMapper).insert(argThat(u ->
                u.getRole().equals("STUDENT") && u.getPassword().equals("hashed") && u.getStatus() == 1));
    }

    @Test
    void register_teacherRoleAllowed() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("teacher1");
        dto.setPassword("123456");
        dto.setRole("TEACHER");
        userService.register(dto);
        verify(userMapper).insert(argThat(u -> u.getRole().equals("TEACHER")));
    }

    @Test
    void register_adminRoleNotAllowed_throws() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("boss");
        dto.setPassword("123456");
        dto.setRole("ADMIN");
        assertThrows(BusinessException.class, () -> userService.register(dto));
    }

    @Test
    void changePassword_wrongOldPassword_throws() {
        TestSupport.loginAsStudent(1L);
        when(userMapper.selectById(1L)).thenReturn(user());
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("wrong");
        dto.setNewPassword("newpass");
        assertThrows(BusinessException.class, () -> userService.changePassword(dto));
    }

    @Test
    void changePassword_success() {
        TestSupport.loginAsStudent(1L);
        when(userMapper.selectById(1L)).thenReturn(user());
        when(passwordEncoder.matches("oldpass", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("newencoded");
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("oldpass");
        dto.setNewPassword("newpass");
        userService.changePassword(dto);
        verify(userMapper).updateById(argThat(u -> u.getPassword().equals("newencoded")));
    }

    @Test
    void updateProfile_withPassword_throws() {
        TestSupport.loginAsStudent(1L);
        when(userMapper.selectById(1L)).thenReturn(user());
        User patch = new User();
        patch.setPassword("hacked");
        assertThrows(BusinessException.class, () -> userService.updateProfile(patch));
    }

    @Test
    void updateProfile_invalidPhone_throws() {
        TestSupport.loginAsStudent(1L);
        when(userMapper.selectById(1L)).thenReturn(user());
        User patch = new User();
        patch.setPhone("123");
        assertThrows(BusinessException.class, () -> userService.updateProfile(patch));
    }

    @Test
    void updateProfile_invalidEmail_throws() {
        TestSupport.loginAsStudent(1L);
        when(userMapper.selectById(1L)).thenReturn(user());
        User patch = new User();
        patch.setEmail("not-an-email");
        assertThrows(BusinessException.class, () -> userService.updateProfile(patch));
    }

    @Test
    void updateProfile_usernameTooShort_throws() {
        TestSupport.loginAsStudent(1L);
        when(userMapper.selectById(1L)).thenReturn(user());
        User patch = new User();
        patch.setUsername("ab");
        assertThrows(BusinessException.class, () -> userService.updateProfile(patch));
    }

    @Test
    void updateProfile_usernameTaken_throws() {
        TestSupport.loginAsStudent(1L);
        when(userMapper.selectById(1L)).thenReturn(user());
        when(userMapper.selectCount(any())).thenReturn(1L);
        User patch = new User();
        patch.setUsername("lisi");
        assertThrows(BusinessException.class, () -> userService.updateProfile(patch));
    }

    @Test
    void updateProfile_success_updatesOwnId() {
        TestSupport.loginAsStudent(1L);
        when(userMapper.selectById(1L)).thenReturn(user());
        when(userMapper.selectCount(any())).thenReturn(0L);
        User patch = new User();
        patch.setUsername("newname");
        patch.setEmail("a@b.com");
        userService.updateProfile(patch);
        verify(userMapper).updateById(argThat(u -> u.getId().equals(1L) && u.getUsername().equals("newname")));
    }
}