package com.smartlab.controller;

import com.smartlab.common.PageResult;
import com.smartlab.common.Result;
import com.smartlab.exception.BusinessException;
import com.smartlab.service.OperationLogService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/recyclebin")
public class RecycleBinController {

    private final JdbcTemplate jdbcTemplate;
    private final OperationLogService logService;

    public RecycleBinController(JdbcTemplate jdbcTemplate, OperationLogService logService) {
        this.jdbcTemplate = jdbcTemplate;
        this.logService = logService;
    }

    private static final Map<String, String> TABLE_MAP = Map.of(
            "device", "device",
            "lab", "lab",
            "borrow", "borrow",
            "repair", "repair",
            "user", "user",
            "announcement", "announcement",
            "lab_reservation", "lab_reservation"
    );

    private static final Map<String, String> NAME_SQL_MAP = Map.of(
            "device", "SELECT name FROM device WHERE id = ?",
            "lab", "SELECT name FROM lab WHERE id = ?",
            "borrow", "SELECT u.username FROM borrow b JOIN `user` u ON b.user_id = u.id WHERE b.id = ?",
            "repair", "SELECT u.username FROM repair r JOIN `user` u ON r.reporter_id = u.id WHERE r.id = ?",
            "user", "SELECT username FROM `user` WHERE id = ?",
            "announcement", "SELECT title FROM announcement WHERE id = ?",
            "lab_reservation", "SELECT u.username FROM lab_reservation r JOIN `user` u ON r.user_id = u.id WHERE r.id = ?"
    );

    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        String table = TABLE_MAP.get(type);
        if (table == null) {
            throw new BusinessException("未知的数据类型: " + type);
        }

        String countSql = "SELECT COUNT(*) FROM " + table + " WHERE deleted = 1";
        long total = jdbcTemplate.queryForObject(countSql, Long.class);

        String sql = "SELECT * FROM " + table + " WHERE deleted = 1 ORDER BY update_time DESC LIMIT ? OFFSET ?";
        int offset = (page - 1) * size;
        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, size, offset);

        return Result.success(PageResult.of(total, page, size, records));
    }

    @GetMapping("/counts")
    public Result<Map<String, Long>> counts() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : TABLE_MAP.entrySet()) {
            String sql = "SELECT COUNT(*) FROM " + e.getValue() + " WHERE deleted = 1";
            long count = jdbcTemplate.queryForObject(sql, Long.class);
            result.put(e.getKey(), count);
        }
        return Result.success(result);
    }

    @PutMapping("/restore/{type}/{id}")
    public Result<Void> restore(@PathVariable String type, @PathVariable Long id) {
        String table = TABLE_MAP.get(type);
        if (table == null) {
            throw new BusinessException("未知的数据类型: " + type);
        }

        String checkSql = "SELECT deleted FROM " + table + " WHERE id = ?";
        List<Integer> deletedList = jdbcTemplate.queryForList(checkSql, Integer.class, id);
        if (deletedList.isEmpty()) {
            throw new BusinessException("数据不存在");
        }
        if (deletedList.get(0) == 0) {
            throw new BusinessException("该数据未被删除");
        }

        String name = jdbcTemplate.queryForObject(NAME_SQL_MAP.get(type), String.class, id);

        String sql = "UPDATE " + table + " SET deleted = 0 WHERE id = ?";
        jdbcTemplate.update(sql, id);

        logService.log("recyclebin", "restore", "恢复" + type + ": " + (name != null ? name : id), id);
        return Result.success();
    }

    @DeleteMapping("/purge/{type}/{id}")
    public Result<Void> purge(@PathVariable String type, @PathVariable Long id) {
        String table = TABLE_MAP.get(type);
        if (table == null) {
            throw new BusinessException("未知的数据类型: " + type);
        }

        String checkSql = "SELECT deleted FROM " + table + " WHERE id = ?";
        List<Integer> deletedList = jdbcTemplate.queryForList(checkSql, Integer.class, id);
        if (deletedList.isEmpty()) {
            throw new BusinessException("数据不存在");
        }
        if (deletedList.get(0) == 0) {
            throw new BusinessException("该数据未被删除");
        }

        String name = jdbcTemplate.queryForObject(NAME_SQL_MAP.get(type), String.class, id);

        cascadeDeleteReferences(type, id);

        String sql = "DELETE FROM " + table + " WHERE id = ? AND deleted = 1";
        jdbcTemplate.update(sql, id);

        logService.log("recyclebin", "purge", "彻底删除" + type + ": " + (name != null ? name : id), id);
        return Result.success();
    }

    @DeleteMapping("/purge/{type}")
    public Result<Void> purgeAll(@PathVariable String type) {
        String table = TABLE_MAP.get(type);
        if (table == null) {
            throw new BusinessException("未知的数据类型: " + type);
        }

        String countSql = "SELECT COUNT(*) FROM " + table + " WHERE deleted = 1";
        long count = jdbcTemplate.queryForObject(countSql, Long.class);
        if (count == 0) {
            throw new BusinessException("该类型没有已删除数据");
        }

        cascadeDeleteAllReferences(type);

        String sql = "DELETE FROM " + table + " WHERE deleted = 1";
        jdbcTemplate.update(sql);

        logService.log("recyclebin", "purgeAll", "清空" + type + "回收站 " + count + " 条", null);
        return Result.success();
    }

    /**
     * 彻底删除某条记录前，先清掉引用了它的子记录（外键约束必须物理消除）。
     * 对 NOT NULL 外键列（如 borrow.user_id / device_id、repair.reporter_id 等）直接删除子行；
     * 对可空外键列（如 approver_id / repairer_id / operation_log.user_id 等）置为 NULL 以保留子记录。
     */
    private void cascadeDeleteReferences(String type, Long id) {
        switch (type) {
            case "user" -> {
                jdbcTemplate.update("DELETE FROM borrow WHERE user_id = ?", id);
                jdbcTemplate.update("UPDATE borrow SET approver_id = NULL WHERE approver_id = ?", id);
                jdbcTemplate.update("DELETE FROM repair WHERE reporter_id = ?", id);
                jdbcTemplate.update("UPDATE repair SET repairer_id = NULL WHERE repairer_id = ?", id);
                jdbcTemplate.update("DELETE FROM lab_reservation WHERE user_id = ?", id);
                jdbcTemplate.update("UPDATE lab_reservation SET approver_id = NULL WHERE approver_id = ?", id);
                jdbcTemplate.update("UPDATE operation_log SET user_id = NULL WHERE user_id = ?", id);
                jdbcTemplate.update("UPDATE lab SET manager_id = NULL WHERE manager_id = ?", id);
            }
            case "device" -> {
                jdbcTemplate.update("DELETE FROM borrow WHERE device_id = ?", id);
                jdbcTemplate.update("DELETE FROM repair WHERE device_id = ?", id);
            }
            case "lab" -> {
                jdbcTemplate.update("UPDATE device SET lab_id = NULL WHERE lab_id = ?", id);
                jdbcTemplate.update("DELETE FROM lab_reservation WHERE lab_id = ?", id);
            }
            default -> { }
        }
    }

    /**
     * 清空某类型回收站时，先清掉所有引用这些待删记录的子记录。
     */
    private void cascadeDeleteAllReferences(String type) {
        switch (type) {
            case "user" -> {
                jdbcTemplate.update("DELETE FROM borrow WHERE user_id IN (SELECT id FROM `user` WHERE deleted = 1)");
                jdbcTemplate.update("UPDATE borrow SET approver_id = NULL WHERE approver_id IN (SELECT id FROM `user` WHERE deleted = 1)");
                jdbcTemplate.update("DELETE FROM repair WHERE reporter_id IN (SELECT id FROM `user` WHERE deleted = 1)");
                jdbcTemplate.update("UPDATE repair SET repairer_id = NULL WHERE repairer_id IN (SELECT id FROM `user` WHERE deleted = 1)");
                jdbcTemplate.update("DELETE FROM lab_reservation WHERE user_id IN (SELECT id FROM `user` WHERE deleted = 1)");
                jdbcTemplate.update("UPDATE lab_reservation SET approver_id = NULL WHERE approver_id IN (SELECT id FROM `user` WHERE deleted = 1)");
                jdbcTemplate.update("UPDATE operation_log SET user_id = NULL WHERE user_id IN (SELECT id FROM `user` WHERE deleted = 1)");
                jdbcTemplate.update("UPDATE lab SET manager_id = NULL WHERE manager_id IN (SELECT id FROM `user` WHERE deleted = 1)");
            }
            case "device" -> {
                jdbcTemplate.update("DELETE FROM borrow WHERE device_id IN (SELECT id FROM device WHERE deleted = 1)");
                jdbcTemplate.update("DELETE FROM repair WHERE device_id IN (SELECT id FROM device WHERE deleted = 1)");
            }
            case "lab" -> {
                jdbcTemplate.update("UPDATE device SET lab_id = NULL WHERE lab_id IN (SELECT id FROM lab WHERE deleted = 1)");
                jdbcTemplate.update("DELETE FROM lab_reservation WHERE lab_id IN (SELECT id FROM lab WHERE deleted = 1)");
            }
            default -> { }
        }
    }
}