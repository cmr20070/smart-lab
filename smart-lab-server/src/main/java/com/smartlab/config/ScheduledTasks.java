package com.smartlab.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlab.entity.Borrow;
import com.smartlab.service.BorrowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final BorrowService borrowService;

    public ScheduledTasks(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void markOverdueBorrows() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Borrow> query = new LambdaQueryWrapper<Borrow>()
                .in(Borrow::getStatus, 0, 1)
                .isNotNull(Borrow::getExpectReturnTime)
                .lt(Borrow::getExpectReturnTime, now);
        List<Borrow> overdueList = borrowService.list(query);

        if (overdueList.isEmpty()) {
            return;
        }

        int updated = 0;
        for (Borrow b : overdueList) {
            b.setStatus(5);
            b.setUpdateTime(now);
            borrowService.updateById(b);
            updated++;
        }
        log.info("定时任务: 已标记 {} 条借用为逾期状态", updated);
    }
}