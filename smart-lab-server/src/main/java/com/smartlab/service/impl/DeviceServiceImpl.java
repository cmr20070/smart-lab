package com.smartlab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartlab.common.PageResult;
import com.smartlab.entity.Borrow;
import com.smartlab.entity.Device;
import com.smartlab.enums.BorrowStatus;
import com.smartlab.mapper.BorrowMapper;
import com.smartlab.mapper.DeviceMapper;
import com.smartlab.service.DeviceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    private final BorrowMapper borrowMapper;

    public DeviceServiceImpl(BorrowMapper borrowMapper) {
        this.borrowMapper = borrowMapper;
    }

    @Override
    public PageResult<Borrow> getOverdueBorrows(int page, int size) {
        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Borrow::getStatus, BorrowStatus.APPROVED.code, BorrowStatus.OVERDUE.code)
                .lt(Borrow::getExpectReturnTime, LocalDateTime.now())
                .orderByAsc(Borrow::getExpectReturnTime);
        Page<Borrow> pageResult = borrowMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(pageResult.getTotal(), page, size, pageResult.getRecords());
    }
}