package com.smartlab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartlab.common.PageResult;
import com.smartlab.entity.Announcement;
import com.smartlab.entity.User;
import com.smartlab.exception.BusinessException;
import com.smartlab.mapper.AnnouncementMapper;
import com.smartlab.service.AnnouncementService;
import com.smartlab.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

    private final UserService userService;

    public AnnouncementServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public PageResult<Announcement> listPublished(int page, int size) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .orderByDesc(Announcement::getCreateTime);
        Page<Announcement> result = this.page(new Page<>(page, size), wrapper);
        fillPublisherNames(result.getRecords());
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    @Override
    public PageResult<Announcement> listAll(int page, int size) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .orderByDesc(Announcement::getCreateTime);
        Page<Announcement> result = this.page(new Page<>(page, size), wrapper);
        fillPublisherNames(result.getRecords());
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    private void fillPublisherNames(java.util.List<Announcement> records) {
        for (Announcement a : records) {
            User publisher = userService.getById(a.getPublisherId());
            if (publisher != null) {
                a.setPublisherName(publisher.getUsername());
            }
        }
    }

    @Override
    public void publish(Announcement announcement, Long userId) {
        if (announcement.getTitle() == null || announcement.getTitle().trim().isEmpty()) {
            throw new BusinessException("公告标题不能为空");
        }
        if (announcement.getContent() == null || announcement.getContent().trim().isEmpty()) {
            throw new BusinessException("公告内容不能为空");
        }
        announcement.setPublisherId(userId);
        announcement.setStatus(1);
        User user = userService.getById(userId);
        if (user != null) {
            announcement.setPublisherName(user.getUsername());
        }
        this.save(announcement);
    }

    @Override
    public void withdraw(Long id, Long userId) {
        Announcement announcement = this.getById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        if (!announcement.getPublisherId().equals(userId)) {
            throw new BusinessException("只能撤回自己发布的公告");
        }
        if (announcement.getStatus() == 0) {
            throw new BusinessException("公告已撤回");
        }
        announcement.setStatus(0);
        announcement.setUpdateTime(LocalDateTime.now());
        this.updateById(announcement);
    }
}