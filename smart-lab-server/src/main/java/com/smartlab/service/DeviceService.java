package com.smartlab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartlab.common.PageResult;
import com.smartlab.entity.Borrow;
import com.smartlab.entity.Device;

public interface DeviceService extends IService<Device> {
    PageResult<Borrow> getOverdueBorrows(int page, int size);
}