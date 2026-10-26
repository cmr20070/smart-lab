package com.smartlab.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.entity.OperationLog;
import com.smartlab.service.OperationLogService;
import com.smartlab.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationLogControllerTest {

    @Mock
    private OperationLogService logService;
    @Mock
    private UserService userService;

    @InjectMocks
    private OperationLogController operationLogController;

    @Test
    void list_returnsPageResult() {
        Page<OperationLog> page = new Page<>(1, 20);
        page.setTotal(3);
        OperationLog log = new OperationLog();
        log.setId(1L);
        log.setModule("device");
        page.setRecords(List.of(log));
        when(logService.page(any(), any())).thenReturn(page);

        Result<PageResult<OperationLog>> result = operationLogController.list(1, 20, null, null, null, null, null);

        assertEquals(200, result.getCode());
        assertEquals(3, result.getData().getTotal());
        verify(logService).page(any(), any());
    }

    @Test
    void list_withTimeFilter_returnsPageResult() {
        when(logService.page(any(), any())).thenReturn(new Page<>(1, 20));
        Result<PageResult<OperationLog>> result = operationLogController.list(
                1, 20, "device", "add", null,
                "2026-01-01 00:00:00", "2026-12-31 23:59:59");
        assertEquals(200, result.getCode());
    }

    @Test
    void moduleStats_aggregatesByModule() {
        OperationLog a = new OperationLog();
        a.setModule("device");
        OperationLog b = new OperationLog();
        b.setModule("device");
        OperationLog c = new OperationLog();
        c.setModule("user");
        when(logService.list()).thenReturn(List.of(a, b, c));

        Result<Map<String, Long>> result = operationLogController.moduleStats();

        assertEquals(200, result.getCode());
        assertEquals(2L, result.getData().get("device"));
        assertEquals(1L, result.getData().get("user"));
    }
}