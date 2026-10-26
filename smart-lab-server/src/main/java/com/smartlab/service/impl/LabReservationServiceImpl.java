package com.smartlab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartlab.common.PageResult;
import com.smartlab.config.SecurityConfig.UserPrincipal;
import com.smartlab.entity.Lab;
import com.smartlab.entity.LabReservation;
import com.smartlab.entity.User;
import com.smartlab.exception.BusinessException;
import com.smartlab.mapper.LabReservationMapper;
import com.smartlab.service.LabReservationService;
import com.smartlab.service.LabService;
import com.smartlab.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LabReservationServiceImpl extends ServiceImpl<LabReservationMapper, LabReservation>
        implements LabReservationService {

    private final LabService labService;
    private final UserService userService;

    public LabReservationServiceImpl(LabService labService, UserService userService) {
        this.labService = labService;
        this.userService = userService;
    }

    private UserPrincipal currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (UserPrincipal) auth.getPrincipal();
    }

    @Override
    @Transactional
    public long reserveMulti(Long labId, String purpose, String startDateStr, String endDateStr, String startTime, String endTime) {
        var principal = currentUser();

        Lab lab = labService.getById(labId);
        if (lab == null) throw new BusinessException("实验室不存在");
        if (lab.getStatus() != 1) throw new BusinessException("该实验室已停用");

        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        if (startDate.isBefore(LocalDate.now())) throw new BusinessException("开始日期不能早于今天");
        if (endDate.isBefore(startDate)) throw new BusinessException("结束日期不能早于开始日期");

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (daysBetween > 30) throw new BusinessException("单次预约最长为30天");

        List<LabReservation> existing = this.list(new LambdaQueryWrapper<LabReservation>()
                .eq(LabReservation::getLabId, labId)
                .in(LabReservation::getStatus, 0, 1));

        for (LabReservation r : existing) {
            LocalDate ed = r.getEndDate() != null ? r.getEndDate() : r.getReserveDate();
            LocalDate sd = r.getReserveDate();
            if (!endDate.isBefore(sd) && !startDate.isAfter(ed)) {
                throw new BusinessException("所选日期范围内存在已预约时段（" + sd + " 至 " + ed + "）");
            }
        }

        LabReservation r = new LabReservation();
        r.setLabId(labId);
        r.setUserId(principal.userId());
        r.setPurpose(purpose);
        r.setReserveDate(startDate);
        r.setEndDate(endDate);
        r.setStartTime(startTime);
        r.setEndTime(endTime);
        r.setStatus(0);
        this.save(r);

        return daysBetween;
    }

    @Override
    @Transactional
    public void approve(Long reservationId, Integer status, String approveRemark) {
        var principal = currentUser();
        String role = principal.role();
        if (!"ADMIN".equals(role) && !"LAB_ADMIN".equals(role)) {
            throw new BusinessException("无审批权限");
        }

        LabReservation r = this.getById(reservationId);
        if (r == null) throw new BusinessException("预约记录不存在");
        if (r.getStatus() != 0) throw new BusinessException("该预约已处理");

        if ("LAB_ADMIN".equals(role)) {
            long canManage = labService.count(new LambdaQueryWrapper<Lab>()
                    .eq(Lab::getManagerId, principal.userId())
                    .eq(Lab::getId, r.getLabId()));
            if (canManage == 0) throw new BusinessException("您不是该实验室的负责人，无权审批");
        }

        r.setStatus(status);
        r.setApproverId(principal.userId());
        r.setApproveTime(LocalDateTime.now());
        r.setApproveRemark(approveRemark);
        this.updateById(r);
    }

    @Override
    public PageResult<LabReservation> pageReservations(int page, int size, Integer status, Long labId) {
        var principal = currentUser();
        String role = principal.role();

        LambdaQueryWrapper<LabReservation> wrapper = new LambdaQueryWrapper<>();
        if ("STUDENT".equals(role) || "TEACHER".equals(role)) {
            wrapper.eq(LabReservation::getUserId, principal.userId());
        } else if ("LAB_ADMIN".equals(role)) {
            List<Long> labIds = labService.list(new LambdaQueryWrapper<Lab>()
                    .eq(Lab::getManagerId, principal.userId()))
                    .stream().map(Lab::getId).toList();
            if (!labIds.isEmpty()) wrapper.in(LabReservation::getLabId, labIds);
            else wrapper.eq(LabReservation::getId, -1L);
        }
        if (status != null) wrapper.eq(LabReservation::getStatus, status);
        if (labId != null) wrapper.eq(LabReservation::getLabId, labId);
        wrapper.orderByDesc(LabReservation::getReserveDate).orderByDesc(LabReservation::getCreateTime);

        Page<LabReservation> pageResult = this.page(new Page<>(page, size), wrapper);
        fillReservationInfo(pageResult.getRecords());
        return PageResult.of(pageResult.getTotal(), page, size, pageResult.getRecords());
    }

    private void fillReservationInfo(List<LabReservation> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, Lab> labMap = labService.listByIds(list.stream()
                .map(LabReservation::getLabId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Lab::getId, Function.identity()));
        Map<Long, User> userMap = userService.listByIds(list.stream()
                .map(r -> java.util.Arrays.asList(r.getUserId(), r.getApproverId()))
                .flatMap(List::stream).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        for (LabReservation r : list) {
            Lab l = labMap.get(r.getLabId());
            if (l != null) {
                r.setLabName(l.getName());
            }
            User u = userMap.get(r.getUserId());
            if (u != null) {
                r.setUsername(u.getUsername());
                r.setUserRole(u.getRole());
            }
            User a = userMap.get(r.getApproverId());
            if (a != null) {
                r.setApproverName(a.getUsername());
            }
        }
    }

    @Override
    public void adminCancel(Long id, String remark) {
        var principal = currentUser();
        String role = principal.role();
        if (!"ADMIN".equals(role) && !"LAB_ADMIN".equals(role)) {
            throw new BusinessException("无管理权限");
        }
        LabReservation r = this.getById(id);
        if (r == null) throw new BusinessException("预约记录不存在");

        if ("LAB_ADMIN".equals(role)) {
            long canManage = labService.count(new LambdaQueryWrapper<Lab>()
                    .eq(Lab::getManagerId, principal.userId())
                    .eq(Lab::getId, r.getLabId()));
            if (canManage == 0) throw new BusinessException("您不是该实验室的负责人");
        }

        r.setStatus(3);
        r.setApproverId(principal.userId());
        r.setApproveTime(LocalDateTime.now());
        r.setApproveRemark("管理员取消: " + remark);
        this.updateById(r);
    }

    @Override
    public void adminDelete(Long id) {
        var principal = currentUser();
        String role = principal.role();
        if (!"ADMIN".equals(role) && !"LAB_ADMIN".equals(role)) {
            throw new BusinessException("无管理权限");
        }
        LabReservation r = this.getById(id);
        if (r == null) throw new BusinessException("预约记录不存在");

        if ("LAB_ADMIN".equals(role)) {
            long canManage = labService.count(new LambdaQueryWrapper<Lab>()
                    .eq(Lab::getManagerId, principal.userId())
                    .eq(Lab::getId, r.getLabId()));
            if (canManage == 0) throw new BusinessException("您不是该实验室的负责人");
        }

        this.removeById(id);
    }
}