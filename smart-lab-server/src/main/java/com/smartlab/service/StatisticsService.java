package com.smartlab.service;

import java.util.Map;

public interface StatisticsService {
    Map<String, Object> getDeviceStatusStats();
    Map<String, Object> getBorrowStats();
    Map<String, Object> getDashboardStats();
}
