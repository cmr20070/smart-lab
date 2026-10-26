package com.smartlab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartlab.common.PageResult;
import com.smartlab.entity.Announcement;

public interface AnnouncementService extends IService<Announcement> {
    PageResult<Announcement> listPublished(int page, int size);
    PageResult<Announcement> listAll(int page, int size);
    void publish(Announcement announcement, Long userId);
    void withdraw(Long id, Long userId);
}