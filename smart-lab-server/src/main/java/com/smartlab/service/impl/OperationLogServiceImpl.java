package com.smartlab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartlab.entity.OperationLog;
import com.smartlab.entity.User;
import com.smartlab.mapper.OperationLogMapper;
import com.smartlab.service.OperationLogService;
import com.smartlab.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    private final UserService userService;
    private final HttpServletRequest request;

    public OperationLogServiceImpl(UserService userService, HttpServletRequest request) {
        this.userService = userService;
        this.request = request;
    }

    @Override
    public void log(String module, String action, String description, Long targetId) {
        OperationLog log = new OperationLog();
        log.setModule(module);
        log.setAction(action);
        log.setDescription(description);
        log.setTargetId(targetId);
        log.setIp(getClientIp());

        try {
            User user = userService.getCurrentUser();
            log.setUserId(user.getId());
        } catch (Exception e) {
            log.setUserId(null);
        }

        this.save(log);
    }

    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}