package com.smartlab.controller;

import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.exception.BusinessException;
import com.smartlab.service.OperationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecycleBinControllerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private OperationLogService logService;

    @InjectMocks
    private RecycleBinController recycleBinController;

    @Test
    void list_unknownType_throws() {
        assertThrows(BusinessException.class, () -> recycleBinController.list("xxx", 1, 10));
    }

    @Test
    void list_returnsPageResult() {
        doReturn(2L).when(jdbcTemplate).queryForObject(any(String.class), eq(Long.class));
        doReturn(java.util.Collections.emptyList()).when(jdbcTemplate)
                .queryForList(any(String.class), any(Object[].class));

        Result<PageResult<Map<String, Object>>> result = recycleBinController.list("device", 1, 10);

        assertEquals(200, result.getCode());
        assertEquals(2L, result.getData().getTotal());
    }

    @Test
    void counts_returnsAllTables() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class))).thenReturn(1L);
        Result<Map<String, Long>> result = recycleBinController.counts();
        assertEquals(200, result.getCode());
        assertEquals(7, result.getData().size());
    }

    @Test
    void restore_notDeleted_throws() {
        when(jdbcTemplate.queryForList(any(String.class), eq(Integer.class), eq(1L)))
                .thenReturn(List.of(0));
        assertThrows(BusinessException.class, () -> recycleBinController.restore("device", 1L));
    }

    @Test
    void restore_notExists_throws() {
        when(jdbcTemplate.queryForList(any(String.class), eq(Integer.class), eq(1L)))
                .thenReturn(List.of());
        assertThrows(BusinessException.class, () -> recycleBinController.restore("device", 1L));
    }

    @Test
    void restore_success_updatesAndLogs() {
        when(jdbcTemplate.queryForList(any(String.class), eq(Integer.class), eq(1L)))
                .thenReturn(List.of(1));
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(1L)))
                .thenReturn("示波器");
        when(jdbcTemplate.update(any(String.class), eq(1L))).thenReturn(1);

        Result<Void> result = recycleBinController.restore("device", 1L);

        assertEquals(200, result.getCode());
        verify(logService).log(eq("recyclebin"), eq("restore"), any(String.class), eq(1L));
    }

    @Test
    void purge_success_deletesAndLogs() {
        when(jdbcTemplate.queryForList(any(String.class), eq(Integer.class), eq(1L)))
                .thenReturn(List.of(1));
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(1L)))
                .thenReturn("示波器");
        when(jdbcTemplate.update(any(String.class), eq(1L))).thenReturn(1);

        Result<Void> result = recycleBinController.purge("device", 1L);

        assertEquals(200, result.getCode());
        verify(jdbcTemplate, atLeastOnce()).update(any(String.class), eq(1L));
        verify(logService).log(eq("recyclebin"), eq("purge"), any(String.class), eq(1L));
    }

    @Test
    void purgeAll_empty_throws() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class))).thenReturn(0L);
        assertThrows(BusinessException.class, () -> recycleBinController.purgeAll("device"));
    }

    @Test
    void purgeAll_success_deletesAndLogs() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class))).thenReturn(3L);
        when(jdbcTemplate.update(any(String.class))).thenReturn(3);

        Result<Void> result = recycleBinController.purgeAll("device");

        assertEquals(200, result.getCode());
        verify(jdbcTemplate, atLeastOnce()).update(any(String.class));
        verify(logService).log(eq("recyclebin"), eq("purgeAll"), any(String.class), isNull());
    }
}