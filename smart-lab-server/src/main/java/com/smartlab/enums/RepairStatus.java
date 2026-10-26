package com.smartlab.enums;

public enum RepairStatus {
    PENDING(0, "待处理"),
    REPAIRING(1, "维修中"),
    COMPLETED(2, "已完成");

    public final int code;
    public final String label;

    RepairStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static RepairStatus of(int code) {
        for (RepairStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知维修状态: " + code);
    }
}