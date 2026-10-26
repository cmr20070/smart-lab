package com.smartlab.controller;

import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.config.SecurityConfig.UserPrincipal;
import com.smartlab.entity.Announcement;
import com.smartlab.service.AnnouncementService;
import com.smartlab.service.OperationLogService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final OperationLogService logService;

    public AnnouncementController(AnnouncementService announcementService, OperationLogService logService) {
        this.announcementService = announcementService;
        this.logService = logService;
    }

    private UserPrincipal currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserPrincipal) authentication.getPrincipal();
    }

    @GetMapping("/list")
    public Result<PageResult<Announcement>> list(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.success(announcementService.listPublished(page, size));
    }

    @GetMapping("/all")
    public Result<PageResult<Announcement>> listAll(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return Result.success(announcementService.listAll(page, size));
    }

    @PostMapping
    public Result<Void> publish(@RequestBody Announcement announcement) {
        var principal = currentUser();
        String role = principal.role();
        if (!"ADMIN".equals(role) && !"LAB_ADMIN".equals(role)) {
            return Result.error(403, "无权限发布公告");
        }
        announcementService.publish(announcement, principal.userId());
        logService.log("announcement", "publish", "发布公告: " + announcement.getTitle(), null);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Announcement announcement) {
        var principal = currentUser();
        Announcement existing = announcementService.getById(id);
        if (existing == null) {
            return Result.error(404, "公告不存在");
        }
        String role = principal.role();
        if (!"ADMIN".equals(role) && !"LAB_ADMIN".equals(role) && !existing.getPublisherId().equals(principal.userId())) {
            return Result.error(403, "只能编辑自己发布的公告");
        }
        existing.setTitle(announcement.getTitle());
        existing.setContent(announcement.getContent());
        existing.setType(announcement.getType());
        announcementService.updateById(existing);
        logService.log("announcement", "update", "编辑公告: " + existing.getTitle(), id);
        return Result.success();
    }

    @PutMapping("/withdraw/{id}")
    public Result<Void> withdraw(@PathVariable Long id) {
        var principal = currentUser();
        announcementService.withdraw(id, principal.userId());
        logService.log("announcement", "withdraw", "撤回公告", id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        var principal = currentUser();
        if (!"ADMIN".equals(principal.role())) {
            return Result.error(403, "仅系统管理员可删除公告");
        }
        Announcement announcement = announcementService.getById(id);
        if (announcement != null) {
            announcementService.removeById(id);
            logService.log("announcement", "delete", "删除公告: " + announcement.getTitle(), id);
        }
        return Result.success();
    }
}