-- 设置字符集
SET NAMES utf8mb4;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS smart_lab DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_lab;

-- ============================================================
-- 用户表
-- ============================================================
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
) COMMENT '用户表';

-- ============================================================
-- 实验室表
-- ============================================================
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
) COMMENT '实验室表';

-- ============================================================
-- 设备表
-- ============================================================
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
) COMMENT '设备表';

-- ============================================================
-- 借用记录表
-- ============================================================
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
    status INT DEFAULT 0 COMMENT '状态: 0待审批 1已通过 2已拒绝 3已归还 4已取消',
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
    CONSTRAINT chk_borrow_status CHECK (status IN (0,1,2,3,4))
) COMMENT '借用记录表';

-- ============================================================
-- 维修记录表
-- ============================================================
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
) COMMENT '维修记录表';

-- ============================================================
-- 操作日志表
-- ============================================================
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
) COMMENT '操作日志表';

-- ============================================================
-- 公告通知表（管理员/实验室管理员发布，全员可见）
-- ============================================================
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
) COMMENT '公告通知表';

ALTER TABLE announcement ADD COLUMN IF NOT EXISTS type VARCHAR(50) DEFAULT 'GENERAL' COMMENT '公告类型' AFTER content;

-- ============================================================
-- 初始化数据
-- ============================================================

-- 默认管理员 (密码: admin123)
INSERT INTO user (username, password, role, status) VALUES
('admin', '$2a$10$GjsvCyT1fDz9vu2nmJjgZOc87vOFUMUSQjlzwwbOAQ.e5DDAQP1ze', 'ADMIN', 1);

-- 测试实验室 (manager_id 后设，先 NULL)
INSERT INTO lab (name, code, location, capacity, category, status) VALUES
('计算机实验室A', 'LAB-001', '教学楼A栋301', 60, '计算机', 1),
('物理实验室', 'LAB-002', '实验楼B栋101', 40, '物理', 1),
('化学实验室', 'LAB-003', '实验楼B栋201', 35, '化学', 1),
('电子实验室', 'LAB-004', '实验楼C栋102', 50, '电子', 1);

-- 测试设备 (lab_name 已移除，通过 lab_id 关联)
INSERT INTO device (name, code, category, model, specification, manufacturer, purchase_date, price, lab_id, status) VALUES
('台式计算机', 'DEV-001', '计算机设备', 'Dell OptiPlex 7080', 'i7-10700/16GB/512GB', '戴尔', '2024-01-15', 5500.00, 1, 1),
('笔记本电脑', 'DEV-002', '计算机设备', 'ThinkPad X1 Carbon', 'i5-1340P/16GB/1TB', '联想', '2024-05-10', 12000.00, 1, 1),
('服务器', 'DEV-003', '计算机设备', 'Dell PowerEdge R740', 'Xeon Gold/128GB/4TB', '戴尔', '2024-03-22', 45000.00, 1, 1),
('激光打印机', 'DEV-004', '计算机外设', 'HP LaserJet Pro M404dn', '黑白激光', '惠普', '2024-02-08', 2800.00, 1, 1),
('网络交换机', 'DEV-005', '网络设备', 'Huawei S5720-28X', '24口千兆', '华为', '2024-01-30', 6800.00, 1, 1),
('示波器', 'DEV-006', '电子测量', 'Tektronix TDS2024C', '200MHz/4通道', '泰克', '2024-02-20', 12000.00, 4, 1),
('万用表', 'DEV-007', '电子测量', 'Fluke 17B+', '数字万用表', '福禄克', '2024-01-20', 800.00, 4, 1),
('函数信号发生器', 'DEV-008', '电子测量', 'Rigol DG1022Z', '25MHz双通道', '普源', '2024-04-15', 3500.00, 4, 1),
('直流稳压电源', 'DEV-009', '电子测量', 'Keysight E36312A', '三路输出', '是德科技', '2024-05-01', 9800.00, 4, 1),
('频谱分析仪', 'DEV-010', '电子测量', 'R&S FPC1000', '5kHz~3GHz', '罗德与施瓦茨', '2024-06-12', 28000.00, 4, 1),
('分光光度计', 'DEV-011', '化学分析', 'UV-2600i', '紫外可见', '岛津', '2024-03-10', 35000.00, 3, 1),
('电子天平', 'DEV-012', '化学仪器', 'Sartorius BSA224S', '0.1mg精度', '赛多利斯', '2024-02-28', 12000.00, 3, 1),
('pH计', 'DEV-013', '化学仪器', 'Mettler FE28', '台式pH计', '梅特勒', '2024-04-20', 4200.00, 3, 1),
('离心机', 'DEV-014', '化学仪器', 'Eppendorf 5810R', '冷冻离心机', '艾本德', '2024-03-15', 18000.00, 3, 1),
('电热恒温水浴锅', 'DEV-015', '化学仪器', 'HH-6', '六孔数显', '国华', '2024-05-05', 1200.00, 3, 1),
('光学显微镜', 'DEV-016', '物理实验', 'Olympus CX23', '双目', '奥林巴斯', '2024-04-05', 8500.00, 2, 1),
('牛顿第二定律实验仪', 'DEV-017', '物理实验', 'DWP-1', '气垫导轨', '大华', '2024-01-18', 2600.00, 2, 1),
('霍尔效应实验仪', 'DEV-018', '物理实验', 'HL-1', '磁场测量', '科明', '2024-03-08', 3200.00, 2, 1),
('声速测定仪', 'DEV-019', '物理实验', 'SV-2', '超声测量', '大华', '2024-02-12', 1500.00, 2, 1),
('转动惯量实验仪', 'DEV-020', '物理实验', 'ZGL-1', '三线摆法', '科明', '2024-04-22', 2200.00, 2, 1);

-- 将 admin 设为所有实验室负责人
UPDATE lab SET manager_id = 1;