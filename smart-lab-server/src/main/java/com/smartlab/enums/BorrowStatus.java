package com.smartlab.enums;

public enum BorrowStatus {
    PENDING(0, "待审批"),
    APPROVED(1, "已批准(借出中)"),
    REJECTED(2, "已拒绝"),
    RETURNED(3, "已归还"),
    CANCELLED(4, "已取消"),
    OVERDUE(5, "逾期");

    public final int code;
    public final String label;

    BorrowStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static BorrowStatus of(int code) {
        for (BorrowStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知借用状态: " + code);
    }
}