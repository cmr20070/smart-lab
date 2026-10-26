package com.smartlab.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.common.PageResult;
import com.smartlab.entity.Borrow;
import com.smartlab.mapper.BorrowMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

    @Mock
    private BorrowMapper borrowMapper;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(deviceService, "baseMapper", borrowMapper);
    }

    @Test
    void getOverdueBorrows_returnsPageResult() {
        Page<Borrow> page = new Page<>(1, 20);
        page.setTotal(5);
        when(borrowMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<Borrow> result = deviceService.getOverdueBorrows(1, 20);

        assertEquals(5, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(20, result.getSize());
    }
}