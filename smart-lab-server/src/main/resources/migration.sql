USE smart_lab;
ALTER TABLE lab MODIFY COLUMN image_url VARCHAR(500) COMMENT '实验室图片URL';
ALTER TABLE device MODIFY COLUMN image_url VARCHAR(500) COMMENT '设备图片URL';
CREATE INDEX idx_user_status ON user(status);
CREATE INDEX idx_user_role ON user(role);
CREATE INDEX idx_lab_status ON lab(status);
CREATE INDEX idx_lab_category ON lab(category);
CREATE INDEX idx_device_lab_id ON device(lab_id);
CREATE INDEX idx_device_status ON device(status);
CREATE INDEX idx_device_category ON device(category);
CREATE INDEX idx_borrow_user_id ON borrow(user_id);
CREATE INDEX idx_borrow_device_id ON borrow(device_id);
CREATE INDEX idx_borrow_status ON borrow(status);
CREATE INDEX idx_borrow_time ON borrow(borrow_time);
CREATE INDEX idx_borrow_approver ON borrow(approver_id);
CREATE INDEX idx_repair_device_id ON repair(device_id);
CREATE INDEX idx_repair_reporter_id ON repair(reporter_id);
CREATE INDEX idx_repair_repairer_id ON repair(repairer_id);
CREATE INDEX idx_repair_status ON repair(status);
CREATE INDEX idx_repair_report_time ON repair(report_time);
CREATE INDEX idx_log_user_id ON operation_log(user_id);
CREATE INDEX idx_log_create_time ON operation_log(create_time);
CREATE INDEX idx_log_module ON operation_log(module);
CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '公告ID',
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT COMMENT '公告内容',
    type VARCHAR(50) DEFAULT 'GENERAL' COMMENT '公告类型: GENERAL通知 NOTICE提醒 POLICY制度 MAINTENANCE维护 HOLIDAY假期',
    publisher_id BIGINT NOT NULL COMMENT '发布人ID',
    publisher_name VARCHAR(50) COMMENT '发布人姓名',
    status TINYINT DEFAULT 1 COMMENT '状态: 1已发布 0已撤回',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
    INDEX idx_announce_status (status),
    INDEX idx_announce_create_time (create_time)
) COMMENT='公告通知表';

ALTER TABLE announcement ADD COLUMN IF NOT EXISTS type VARCHAR(50) DEFAULT 'GENERAL' COMMENT '公告类型' AFTER content;
SELECT 'OK: 2 ALTER + 18 indexes' AS result;