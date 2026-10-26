package com.smartlab.controller;

import com.smartlab.TestSupport;
import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.entity.LabReservation;
import com.smartlab.entity.User;
import com.smartlab.service.LabReservationService;
import com.smartlab.service.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabReservationControllerTest {

    @Mock
    private LabReservationService reservationService;
    @Mock
    private OperationLogService logService;

    @InjectMocks
    private LabReservationController reservationController;

    @BeforeEach
    void setUp() {
        TestSupport.loginAsStudent(1L);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clear();
    }

    @Test
    void apply_callsReserveMultiAndLogs() {
        when(reservationService.reserveMulti(anyLong(), anyString(), anyString(), anyString(),
                anyString(), anyString())).thenReturn(3L);

        Map<String, Object> body = Map.of(
                "labId", 1,
                "purpose", "上课",
                "reserveDate", "2026-10-01",
                "startDate", "2026-10-01",
                "endDate", "2026-10-03",
                "startTime", "08:00",
                "endTime", "20:00"
        );

        Result<Long> result = reservationController.apply(body);

        assertEquals(200, result.getCode());
        assertEquals(3L, result.getData());
        verify(reservationService).reserveMulti(1L, "上课", "2026-10-01", "2026-10-03", "08:00", "20:00");
        verify(logService).log(eq("lab-reservation"), eq("apply"), anyString(), eq(1L));
    }

    @Test
    void list_delegates() {
        when(reservationService.pageReservations(anyInt(), anyInt(), any(), any())).thenReturn(PageResult.of(0, 1, 10, java.util.List.of()));
        Result<PageResult<LabReservation>> result = reservationController.list(1, 10, null, null);
        assertEquals(200, result.getCode());
        verify(reservationService).pageReservations(1, 10, null, null);
    }

    @Test
    void approve_callsApprove() {
        Map<String, Object> body = Map.of("id", 5, "status", 1, "remark", "ok");
        Result<Void> result = reservationController.approve(body);
        assertEquals(200, result.getCode());
        verify(reservationService).approve(5L, 1, "ok");
    }

    @Test
    void cancel_whenStatusPending_setsCancelledLocally() {
        LabReservation r = new LabReservation();
        r.setId(5L);
        r.setStatus(0);
        when(reservationService.getById(5L)).thenReturn(r);

        Result<Void> result = reservationController.cancel(5L);

        assertEquals(200, result.getCode());
        assertEquals(3, r.getStatus());
        verify(reservationService).updateById(r);
    }

    @Test
    void adminCancel_callsAdminCancel() {
        Map<String, Object> body = Map.of("remark", "场地冲突");
        Result<Void> result = reservationController.adminCancel(5L, body);
        assertEquals(200, result.getCode());
        verify(reservationService).adminCancel(5L, "场地冲突");
    }

    @Test
    void delete_callsAdminDelete() {
        Result<Void> result = reservationController.delete(5L);
        assertEquals(200, result.getCode());
        verify(reservationService).adminDelete(5L);
    }
}