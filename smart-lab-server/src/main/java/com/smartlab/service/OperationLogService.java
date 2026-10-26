package com.smartlab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartlab.entity.OperationLog;

public interface OperationLogService extends IService<OperationLog> {
    void log(String module, String action, String description, Long targetId);
}