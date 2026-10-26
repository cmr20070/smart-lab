package com.smartlab.service.impl;

import com.smartlab.entity.OperationLog;
import com.smartlab.entity.User;
import com.smartlab.mapper.OperationLogMapper;
import com.smartlab.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationLogServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private OperationLogMapper operationLogMapper;

    @InjectMocks
    private OperationLogServiceImpl operationLogService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(operationLogService, "baseMapper", operationLogMapper);
    }

    @Test
    void log_setsIpAndCurrentUser() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        User u = new User();
        u.setId(7L);
        when(userService.getCurrentUser()).thenReturn(u);

        operationLogService.log("borrow", "apply", "申请借用", 100L);

        verify(operationLogMapper).insert(argThat(log ->
                log.getUserId().equals(7L)
                        && "192.168.1.1".equals(log.getIp())
                        && log.getTargetId().equals(100L)));
    }

    @Test
    void log_requestForwardedBlank_fallsBackToRemoteAddr() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        User u = new User();
        u.setId(7L);
        when(userService.getCurrentUser()).thenReturn(u);

        operationLogService.log("device", "edit", "编辑设备", 1L);

        verify(operationLogMapper).insert(argThat(log ->
                "127.0.0.1".equals(log.getIp())));
    }

    @Test
    void log_whenNoCurrentUser_keepsUserIdNull() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("0.0.0.0");
        when(userService.getCurrentUser()).thenThrow(new RuntimeException("未登录"));

        operationLogService.log("x", "y", "desc", null);

        verify(operationLogMapper).insert(argThat(log ->
                log.getUserId() == null));
    }
}