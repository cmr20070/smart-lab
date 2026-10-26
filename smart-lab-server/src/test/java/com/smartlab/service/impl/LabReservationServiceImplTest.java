package com.smartlab.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlab.TestSupport;
import com.smartlab.common.PageResult;
import com.smartlab.entity.Lab;
import com.smartlab.entity.LabReservation;
import com.smartlab.exception.BusinessException;
import com.smartlab.mapper.LabReservationMapper;
import com.smartlab.service.LabService;
import com.smartlab.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabReservationServiceImplTest {

    @Mock
    private LabService labService;
    @Mock
    private UserService userService;
    @Mock
    private LabReservationMapper reservationMapper;

    @InjectMocks
    private LabReservationServiceImpl reservationService;

    private static final long LAB_ID = 1L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reservationService, "baseMapper", reservationMapper);
    }

    @AfterEach
    void tearDown() {
        TestSupport.clear();
    }

    private Lab lab() {
        Lab l = new Lab();
        l.setId(LAB_ID);
        l.setStatus(1);
        return l;
    }

    @Test
    void reserveMulti_success_savesAndReturnsDays() {
        TestSupport.loginAsStudent(1L);
        when(labService.getById(LAB_ID)).thenReturn(lab());
        when(reservationMapper.selectList(any())).thenReturn(List.of());
        when(reservationMapper.insert(any(LabReservation.class))).thenReturn(1);

        String today = LocalDate.now().toString();
        long days = reservationService.reserveMulti(LAB_ID, "上课", today, today, "08:00", "10:00");

        assertEquals(1L, days);
        verify(reservationMapper).insert(argThat(r ->
                r.getLabId().equals(LAB_ID) && r.getUserId().equals(1L) && r.getStatus() == 0));
    }

    @Test
    void reserveMulti_multipleDays_returnsDayCount() {
        TestSupport.loginAsStudent(1L);
        when(labService.getById(LAB_ID)).thenReturn(lab());
        when(reservationMapper.selectList(any())).thenReturn(List.of());

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);
        long days = reservationService.reserveMulti(LAB_ID, "实验",
                start.toString(), end.toString(), "08:00", "20:00");
        assertEquals(3L, days);
    }

    @Test
    void reserveMulti_labNotExists_throws() {
        TestSupport.loginAsStudent(1L);
        when(labService.getById(LAB_ID)).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> reservationService.reserveMulti(LAB_ID, "x", "2026-10-01", "2026-10-01", "08:00", "20:00"));
    }

    @Test
    void reserveMulti_labDisabled_throws() {
        TestSupport.loginAsStudent(1L);
        Lab l = lab();
        l.setStatus(0);
        when(labService.getById(LAB_ID)).thenReturn(l);
        assertThrows(BusinessException.class,
                () -> reservationService.reserveMulti(LAB_ID, "x", "2026-10-01", "2026-10-01", "08:00", "20:00"));
    }

    @Test
    void reserveMulti_startBeforeToday_throws() {
        TestSupport.loginAsStudent(1L);
        when(labService.getById(LAB_ID)).thenReturn(lab());
        assertThrows(BusinessException.class,
                () -> reservationService.reserveMulti(LAB_ID, "x", "2020-01-01", "2020-01-02", "08:00", "20:00"));
    }

    @Test
    void reserveMulti_endBeforeStart_throws() {
        TestSupport.loginAsStudent(1L);
        when(labService.getById(LAB_ID)).thenReturn(lab());
        assertThrows(BusinessException.class,
                () -> reservationService.reserveMulti(LAB_ID, "x", "2026-10-03", "2026-10-01", "08:00", "20:00"));
    }

    @Test
    void reserveMulti_over30Days_throws() {
        TestSupport.loginAsStudent(1L);
        when(labService.getById(LAB_ID)).thenReturn(lab());
        assertThrows(BusinessException.class,
                () -> reservationService.reserveMulti(LAB_ID, "x", "2026-10-01", "2026-12-31", "08:00", "20:00"));
    }

    @Test
    void reserveMulti_timeConflict_throws() {
        TestSupport.loginAsStudent(1L);
        when(labService.getById(LAB_ID)).thenReturn(lab());
        LocalDate start = LocalDate.now().plusDays(1);
        LabReservation existing = new LabReservation();
        existing.setLabId(LAB_ID);
        existing.setReserveDate(start);
        existing.setEndDate(start);
        when(reservationMapper.selectList(any())).thenReturn(List.of(existing));

        assertThrows(BusinessException.class,
                () -> reservationService.reserveMulti(LAB_ID, "x",
                        start.toString(), start.plusDays(2).toString(), "08:00", "20:00"));
    }

    @Test
    void approve_student_noPermission_throws() {
        TestSupport.loginAsStudent(1L);
        assertThrows(BusinessException.class, () -> reservationService.approve(10L, 1, null));
    }

    @Test
    void approve_admin_success() {
        TestSupport.loginAsAdmin(9L);
        LabReservation r = new LabReservation();
        r.setId(10L);
        r.setStatus(0);
        when(reservationMapper.selectById(10L)).thenReturn(r);

        reservationService.approve(10L, 1, "ok");

        verify(reservationMapper).updateById(r);
        assertEquals(1, r.getStatus());
        assertEquals(9L, r.getApproverId());
    }

    @Test
    void approve_alreadyHandled_throws() {
        TestSupport.loginAsAdmin(9L);
        LabReservation r = new LabReservation();
        r.setId(10L);
        r.setStatus(1);
        when(reservationMapper.selectById(10L)).thenReturn(r);
        assertThrows(BusinessException.class, () -> reservationService.approve(10L, 1, null));
    }

    @Test
    void approve_labAdmin_notManager_throws() {
        TestSupport.loginAsLabAdmin(9L);
        LabReservation r = new LabReservation();
        r.setId(10L);
        r.setStatus(0);
        r.setLabId(LAB_ID);
        when(reservationMapper.selectById(10L)).thenReturn(r);
        when(labService.count(any())).thenReturn(0L);
        assertThrows(BusinessException.class, () -> reservationService.approve(10L, 1, null));
    }

    @Test
    void approve_labAdmin_manager_success() {
        TestSupport.loginAsLabAdmin(9L);
        LabReservation r = new LabReservation();
        r.setId(10L);
        r.setStatus(0);
        r.setLabId(LAB_ID);
        when(reservationMapper.selectById(10L)).thenReturn(r);
        when(labService.count(any())).thenReturn(1L);

        reservationService.approve(10L, 1, null);
        verify(reservationMapper).updateById(r);
    }

    @Test
    void pageReservations_student_limitedToOwn() {
        TestSupport.loginAsStudent(1L);
        when(reservationMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 10));

        PageResult<LabReservation> result = reservationService.pageReservations(1, 10, null, null);

        assertNotNull(result);
        verify(reservationMapper).selectPage(any(), any());
    }

    @Test
    void adminCancel_labAdmin_notManager_throws() {
        TestSupport.loginAsLabAdmin(9L);
        LabReservation r = new LabReservation();
        r.setId(10L);
        r.setLabId(LAB_ID);
        when(reservationMapper.selectById(10L)).thenReturn(r);
        when(labService.count(any())).thenReturn(0L);
        assertThrows(BusinessException.class, () -> reservationService.adminCancel(10L, "x"));
    }

    @Test
    void adminCancel_admin_success() {
        TestSupport.loginAsAdmin(9L);
        LabReservation r = new LabReservation();
        r.setId(10L);
        r.setLabId(LAB_ID);
        when(reservationMapper.selectById(10L)).thenReturn(r);

        reservationService.adminCancel(10L, "场地冲突");

        verify(reservationMapper).updateById(argThat(x -> x.getStatus() == 3));
    }

    @Test
    void adminDelete_student_noPermission_throws() {
        TestSupport.loginAsStudent(1L);
        assertThrows(BusinessException.class, () -> reservationService.adminDelete(10L));
    }

    @Test
    void adminDelete_admin_success() {
        TestSupport.loginAsAdmin(9L);
        LabReservation r = new LabReservation();
        r.setId(10L);
        when(reservationMapper.selectById(10L)).thenReturn(r);

        reservationService.adminDelete(10L);

        verify(reservationMapper).deleteById(any(LabReservation.class));
    }
}