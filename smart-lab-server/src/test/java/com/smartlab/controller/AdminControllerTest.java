package com.smartlab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.TestSupport;
import com.smartlab.common.Result;
import com.smartlab.entity.Borrow;
import com.smartlab.entity.Repair;
import com.smartlab.entity.User;
import com.smartlab.exception.BusinessException;
import com.smartlab.service.BorrowService;
import com.smartlab.service.RepairService;
import com.smartlab.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private BorrowService borrowService;
    @Mock
    private RepairService repairService;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        TestSupport.loginAsAdmin(9L);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clear();
    }

    @Test
    void list_returnsPage() {
        when(userService.page(any(), any())).thenReturn(new Page<>(1, 10));
        Result<Page<User>> result = adminController.list(1, 10, null, "TEACHER");
        assertEquals(200, result.getCode());
        verify(userService).page(any(), any());
    }

    @Test
    void add_duplicateUsername_returnsError() {
        when(userService.count(any(LambdaQueryWrapper.class))).thenReturn(1L);
        User u = new User();
        u.setUsername("zhangsan");
        Result<Void> result = adminController.add(u);
        assertEquals(400, result.getCode());
        verify(userService, never()).save(any());
    }

    @Test
    void add_invalidPhone_returnsError() {
        when(userService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        User u = new User();
        u.setUsername("zhangsan");
        u.setPhone("123");
        Result<Void> result = adminController.add(u);
        assertEquals(400, result.getCode());
        verify(userService, never()).save(any());
    }

    @Test
    void add_success_encodesPasswordAndDefaultsStatus() {
        when(userService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("raw123")).thenReturn("encoded");
        User u = new User();
        u.setUsername("zhangsan");
        u.setPassword("raw123");
        Result<Void> result = adminController.add(u);
        assertEquals(200, result.getCode());
        assertEquals("encoded", u.getPassword());
        assertEquals(1, u.getStatus());
        verify(userService).save(u);
    }

    @Test
    void update_notExists_returns404() {
        when(userService.getById(5L)).thenReturn(null);
        Result<Void> result = adminController.update(5L, new User());
        assertEquals(404, result.getCode());
    }

    @Test
    void update_success_nullEmptyPasswordKeepsNull() {
        User exist = new User();
        exist.setId(5L);
        when(userService.getById(5L)).thenReturn(exist);
        User u = new User();
        u.setUsername("lisi");
        Result<Void> result = adminController.update(5L, u);
        assertEquals(200, result.getCode());
        assertNull(u.getPassword());
        verify(userService).updateById(u);
    }

    @Test
    void update_duplicateUsername_returnsError() {
        User exist = new User();
        exist.setId(5L);
        when(userService.getById(5L)).thenReturn(exist);
        when(userService.count(any(LambdaQueryWrapper.class))).thenReturn(1L);
        User u = new User();
        u.setUsername("dup");
        Result<Void> result = adminController.update(5L, u);
        assertEquals(400, result.getCode());
    }

    @Test
    void delete_self_throws() {
        TestSupport.loginAsAdmin(9L);
        assertThrows(BusinessException.class, () -> adminController.delete(9L));
    }

    @Test
    void delete_notExists_throws() {
        when(userService.getById(5L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> adminController.delete(5L));
    }

    @Test
    void delete_userWithActiveBorrows_throws() {
        when(userService.getById(5L)).thenReturn(new User());
        when(borrowService.count(any(LambdaQueryWrapper.class))).thenReturn(2L);
        assertThrows(BusinessException.class, () -> adminController.delete(5L));
    }

    @Test
    void delete_userWithPendingRepair_throws() {
        when(userService.getById(5L)).thenReturn(new User());
        when(borrowService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(repairService.count(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThrows(BusinessException.class, () -> adminController.delete(5L));
    }

    @Test
    void delete_success_removes() {
        when(userService.getById(5L)).thenReturn(new User());
        when(borrowService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(repairService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userService.removeById(5L)).thenReturn(true);
        Result<Void> result = adminController.delete(5L);
        assertEquals(200, result.getCode());
        verify(userService).removeById(5L);
    }

    @Test
    void toggleStatus_notExists_returns404() {
        when(userService.getById(5L)).thenReturn(null);
        Result<Void> result = adminController.toggleStatus(5L, 0);
        assertEquals(404, result.getCode());
    }

    @Test
    void toggleStatus_success_updates() {
        User u = new User();
        u.setId(5L);
        u.setStatus(1);
        when(userService.getById(5L)).thenReturn(u);
        Result<Void> result = adminController.toggleStatus(5L, 0);
        assertEquals(200, result.getCode());
        assertEquals(0, u.getStatus());
        verify(userService).updateById(u);
    }
}