package com.smartlab.enums;

public enum DeviceStatus {
    IDLE(1, "空闲"),
    BORROWED(2, "借出"),
    REPAIRING(3, "维修中"),
    DISCARD_PENDING_LAB(4, "待实验室管理员审核"),
    SCRAPPED(5, "已报废"),
    DISCARD_PENDING_ADMIN(6, "待管理员终审");

    public final int code;
    public final String label;

    DeviceStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static DeviceStatus of(int code) {
        for (DeviceStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知设备状态: " + code);
    }
}