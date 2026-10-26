package com.smartlab.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumsTest {

    @Test
    void borrowStatus_of_returnsCorrectEnum() {
        assertEquals(BorrowStatus.PENDING, BorrowStatus.of(0));
        assertEquals(BorrowStatus.APPROVED, BorrowStatus.of(1));
        assertEquals(BorrowStatus.REJECTED, BorrowStatus.of(2));
        assertEquals(BorrowStatus.RETURNED, BorrowStatus.of(3));
        assertEquals(BorrowStatus.CANCELLED, BorrowStatus.of(4));
        assertEquals(BorrowStatus.OVERDUE, BorrowStatus.of(5));
    }

    @Test
    void borrowStatus_of_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> BorrowStatus.of(99));
    }

    @Test
    void deviceStatus_of_returnsCorrectEnum() {
        assertEquals(DeviceStatus.IDLE, DeviceStatus.of(1));
        assertEquals(DeviceStatus.BORROWED, DeviceStatus.of(2));
        assertEquals(DeviceStatus.REPAIRING, DeviceStatus.of(3));
        assertEquals(DeviceStatus.DISCARD_PENDING_LAB, DeviceStatus.of(4));
        assertEquals(DeviceStatus.SCRAPPED, DeviceStatus.of(5));
        assertEquals(DeviceStatus.DISCARD_PENDING_ADMIN, DeviceStatus.of(6));
    }

    @Test
    void deviceStatus_of_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> DeviceStatus.of(100));
    }

    @Test
    void repairStatus_of_returnsCorrectEnum() {
        assertEquals(RepairStatus.PENDING, RepairStatus.of(0));
        assertEquals(RepairStatus.REPAIRING, RepairStatus.of(1));
        assertEquals(RepairStatus.COMPLETED, RepairStatus.of(2));
    }

    @Test
    void repairStatus_of_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> RepairStatus.of(-1));
    }

    @Test
    void labels_areNotEmpty() {
        for (BorrowStatus s : BorrowStatus.values()) {
            assertFalse(s.label.isBlank());
        }
        for (DeviceStatus s : DeviceStatus.values()) {
            assertFalse(s.label.isBlank());
        }
        for (RepairStatus s : RepairStatus.values()) {
            assertFalse(s.label.isBlank());
        }
    }
}