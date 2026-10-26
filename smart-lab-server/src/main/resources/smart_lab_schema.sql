-- ============================================================================
-- 智慧校园实验室设备管理系统 —— 数据库完整脚本（用于 E-R 实体练习/画图参考）
-- 数据库: smart_lab   字符集: utf8mb4
-- 共 7 张表，全部字段带注释，含外键与逻辑删除，可直接执行。
-- E-R 联系汇总：
--   1) user     1 ───< N  borrow          (一个用户可发起多条借用)
--   2) device   1 ───< N  borrow          (一台设备对应多条借用)
--   3) lab      1 ───< N  device          (一个实验室拥有多台设备)
--   4) lab      1 ───< N  lab_reservation (一个实验室可被多次预约)
--   5) user     1 ───< N  lab_reservation (一个用户可发起多条预约)
--   6) user     1 ───< N  repair          (reporter_id 报修人 / repairer_id 维修人)
--   7) device   1 ───< N  repair          (一台设备对应多条报修)
--   8) user     1 ───< N  operation_log   (一个用户产生多条操作日志)
--   9) user     1 ───< N  lab.manager_id  (一个负责人管理多个实验室)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS smart_lab DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_lab;

-- ----------------------------------------------------------------------------
-- 实体1: 用户表 user    （参与者：系统管理员/实验室管理员/教师/学生）
--   E-R: user ──< borrow, lab_reservation, repair, operation_log
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色: ADMIN系统管理员/LAB_ADMIN实验室管理员/TEACHER教师/STUDENT学生',
    avatar VARCHAR(500) COMMENT '头像URL',
    status INT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',

    UNIQUE KEY uk_username (username),
    INDEX idx_user_status (status),
    INDEX idx_user_role (role),
    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN','LAB_ADMIN','TEACHER','STUDENT')),
    CONSTRAINT chk_user_status CHECK (status IN (0,1))
) COMMENT='用户表';

-- ----------------------------------------------------------------------------
-- 实体2: 实验室表 lab
--   E-R: lab ──< device, lab_reservation ;  lab.manager_id ──> user
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lab (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '实验室ID',
    name VARCHAR(100) NOT NULL COMMENT '实验室名称',
    code VARCHAR(50) NOT NULL COMMENT '实验室编号',
    location VARCHAR(200) COMMENT '位置',
    capacity INT COMMENT '容纳人数',
    category VARCHAR(50) COMMENT '分类: 计算机/物理/化学/电子等',
    description VARCHAR(2000) COMMENT '描述',
    image_url VARCHAR(500) COMMENT '实验室图片URL',
    manager_id BIGINT COMMENT '负责人ID(关联user表)',
    status INT DEFAULT 1 COMMENT '状态: 1启用 0停用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',

    UNIQUE KEY uk_lab_code (code),
    INDEX idx_lab_status (status),
    INDEX idx_lab_category (category),
    INDEX idx_lab_manager (manager_id),
    CONSTRAINT fk_lab_manager FOREIGN KEY (manager_id) REFERENCES user(id) ON DELETE SET NULL,
    CONSTRAINT chk_lab_status CHECK (status IN (0,1))
) COMMENT='实验室表';

-- ----------------------------------------------------------------------------
-- 实体3: 设备表 device
--   E-R: device ──< borrow, repair ;  device.lab_id ──> lab
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '设备ID',
    name VARCHAR(200) NOT NULL COMMENT '设备名称',
    code VARCHAR(50) NOT NULL COMMENT '设备编号',
    category VARCHAR(50) COMMENT '设备分类',
    model VARCHAR(100) COMMENT '型号',
    specification VARCHAR(300) COMMENT '规格参数',
    manufacturer VARCHAR(100) COMMENT '生产厂家',
    purchase_date DATE COMMENT '采购日期',
    price DECIMAL(12,2) COMMENT '采购价格',
    lab_id BIGINT COMMENT '所属实验室ID',
    status INT DEFAULT 1 COMMENT '状态: 0故障 1在用 2借出 3维修中 4待报废 5已报废',
    image_url VARCHAR(500) COMMENT '设备图片URL',
    description VARCHAR(500) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',

    UNIQUE KEY uk_device_code (code),
    INDEX idx_device_lab_id (lab_id),
    INDEX idx_device_status (status),
    INDEX idx_device_category (category),
    CONSTRAINT fk_device_lab FOREIGN KEY (lab_id) REFERENCES lab(id) ON DELETE SET NULL,
    CONSTRAINT chk_device_status CHECK (status IN (0,1,2,3,4,5))
) COMMENT='设备表';

-- ----------------------------------------------------------------------------
-- 实体4: 借用记录表 borrow（联系实体：user × device）
--   E-R: borrow.user_id ──> user ; borrow.device_id ──> device
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS borrow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '借用记录ID',
    user_id BIGINT NOT NULL COMMENT '借用人ID',
    username VARCHAR(50) COMMENT '借用人用户名(冗余快照)',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    device_name VARCHAR(200) COMMENT '设备名称(冗余快照)',
    device_code VARCHAR(50) COMMENT '设备编号(冗余快照)',
    lab_id BIGINT COMMENT '实验室ID(冗余快照)',
    borrow_time DATETIME COMMENT '借用时间',
    expect_return_time DATETIME COMMENT '预计归还时间',
    actual_return_time DATETIME COMMENT '实际归还时间',
    status INT DEFAULT 0 COMMENT '状态: 0待审批 1已通过 2已拒绝 3已归还 4已取消 5逾期',
    purpose VARCHAR(500) COMMENT '借用目的',
    remark VARCHAR(500) COMMENT '备注',
    approver_id BIGINT COMMENT '审批人ID',
    approve_time DATETIME COMMENT '审批时间',
    approve_remark VARCHAR(500) COMMENT '审批意见',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',

    INDEX idx_borrow_user_id (user_id),
    INDEX idx_borrow_device_id (device_id),
    INDEX idx_borrow_status (status),
    INDEX idx_borrow_time (borrow_time),
    INDEX idx_borrow_approver (approver_id),
    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES user(id),
    CONSTRAINT fk_borrow_device FOREIGN KEY (device_id) REFERENCES device(id),
    CONSTRAINT fk_borrow_approver FOREIGN KEY (approver_id) REFERENCES user(id),
    CONSTRAINT chk_borrow_status CHECK (status IN (0,1,2,3,4,5))
) COMMENT='借用记录表';

-- ----------------------------------------------------------------------------
-- 实体5: 维修记录表 repair（联系实体：device × user）
--   E-R: repair.device_id ──> device ; repair.reporter_id ──> user ; repair.repairer_id ──> user
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS repair (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '维修记录ID',
    device_id BIGINT NOT NULL COMMENT '设备ID',
    reporter_id BIGINT NOT NULL COMMENT '报修人ID',
    report_time DATETIME COMMENT '报修时间',
    fault_description TEXT COMMENT '故障描述',
    fault_level INT DEFAULT 1 COMMENT '故障级别: 1一般 2紧急 3严重',
    status INT DEFAULT 0 COMMENT '状态: 0待处理 1维修中 2已完成',
    repairer_id BIGINT COMMENT '维修人ID',
    repair_time DATETIME COMMENT '维修开始时间',
    finish_time DATETIME COMMENT '维修完成时间',
    repair_cost DECIMAL(12,2) COMMENT '维修费用',
    repair_description TEXT COMMENT '维修描述',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',

    INDEX idx_repair_device_id (device_id),
    INDEX idx_repair_reporter_id (reporter_id),
    INDEX idx_repair_repairer_id (repairer_id),
    INDEX idx_repair_status (status),
    INDEX idx_repair_report_time (report_time),
    CONSTRAINT fk_repair_device FOREIGN KEY (device_id) REFERENCES device(id),
    CONSTRAINT fk_repair_reporter FOREIGN KEY (reporter_id) REFERENCES user(id),
    CONSTRAINT fk_repair_repairer FOREIGN KEY (repairer_id) REFERENCES user(id),
    CONSTRAINT chk_repair_status CHECK (status IN (0,1,2)),
    CONSTRAINT chk_repair_level CHECK (fault_level IN (1,2,3))
) COMMENT='维修记录表';

-- ----------------------------------------------------------------------------
-- 实体6: 操作日志表 operation_log
--   E-R: operation_log.user_id ──> user
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT COMMENT '操作用户ID',
    module VARCHAR(50) COMMENT '操作模块: user/lab/device/borrow/repair',
    action VARCHAR(50) COMMENT '操作类型',
    description VARCHAR(500) COMMENT '操作描述',
    target_id BIGINT COMMENT '操作目标ID',
    ip VARCHAR(50) COMMENT '操作IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    INDEX idx_log_user_id (user_id),
    INDEX idx_log_create_time (create_time),
    INDEX idx_log_module (module),
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES user(id)
) COMMENT='操作日志表';

-- ----------------------------------------------------------------------------
-- 实体7: 实验室预约表 lab_reservation（联系实体：user × lab）
--   E-R: lab_reservation.lab_id ──> lab ; lab_reservation.user_id ──> user
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lab_reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '预约记录ID',
    lab_id BIGINT NOT NULL COMMENT '实验室ID',
    user_id BIGINT NOT NULL COMMENT '预约人ID',
    purpose VARCHAR(500) COMMENT '预约用途',
    reserve_date DATE NOT NULL COMMENT '预约日期',
    end_date DATE COMMENT '结束日期(多日预约)',
    start_time VARCHAR(10) COMMENT '开始时间 HH:mm',
    end_time VARCHAR(10) COMMENT '结束时间 HH:mm',
    status TINYINT DEFAULT 0 COMMENT '状态: 0待审核 1已通过 2已拒绝 3已取消',
    approver_id BIGINT COMMENT '审核人ID',
    approve_time DATETIME COMMENT '审核时间',
    approve_remark VARCHAR(500) COMMENT '审核意见',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
    INDEX idx_lab_id (lab_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_reserve_date (reserve_date),
    CONSTRAINT fk_res_lab FOREIGN KEY (lab_id) REFERENCES lab(id),
    CONSTRAINT fk_res_user FOREIGN KEY (user_id) REFERENCES user(id)
) COMMENT='实验室预约';

-- ----------------------------------------------------------------------------
-- 公告通知表（管理员/实验室管理员发布，全员可见）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '公告ID',
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT COMMENT '公告内容',
    publisher_id BIGINT NOT NULL COMMENT '发布人ID',
    publisher_name VARCHAR(50) COMMENT '发布人姓名',
    status TINYINT DEFAULT 1 COMMENT '状态: 1已发布 0已撤回',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
    INDEX idx_announce_status (status),
    INDEX idx_announce_create_time (create_time)
) COMMENT='公告通知表';

-- ----------------------------------------------------------------------------
-- 初始化数据（默认管理员 admin，密码 admin123）
-- ----------------------------------------------------------------------------
INSERT INTO user (username, password, role, status) VALUES
('admin', '$2a$10$GjsvCyT1fDz9vu2nmJjgZOc87vOFUMUSQjlzwwbOAQ.e5DDAQP1ze', 'ADMIN', 1);

INSERT INTO lab (name, code, location, capacity, category, status) VALUES
('计算机实验室A', 'LAB-001', '教学楼A栋301', 60, '计算机', 1),
('物理实验室', 'LAB-002', '实验楼B栋101', 40, '物理', 1),
('化学实验室', 'LAB-003', '实验楼B栋201', 35, '化学', 1),
('电子实验室', 'LAB-004', '实验楼C栋102', 50, '电子', 1);

UPDATE lab SET manager_id = 1;