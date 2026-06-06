-- 创建数据库
CREATE DATABASE IF NOT EXISTS sms_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sms_db;
SET NAMES utf8mb4;

-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录账号',
    password      VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    real_name     VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    role          VARCHAR(30)  NOT NULL COMMENT '角色: ADMIN/DOCTOR/NURSE/CAREGIVER',
    dept          VARCHAR(100) COMMENT '部门',
    phone         VARCHAR(20)  COMMENT '手机号',
    email         VARCHAR(100) COMMENT '邮箱',
    enabled       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    deleted       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    last_login_time DATETIME   COMMENT '最后登录时间',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- 老人档案表
CREATE TABLE IF NOT EXISTS elder (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(50)  NOT NULL COMMENT '姓名',
    gender              VARCHAR(5)   NOT NULL COMMENT '性别',
    age                 INT          NOT NULL COMMENT '年龄',
    id_card             VARCHAR(20)  COMMENT '身份证号',
    room                VARCHAR(50)  COMMENT '床位',
    doctor_id           BIGINT       COMMENT '责任医生ID',
    doctor              VARCHAR(50)  COMMENT '责任医生姓名',
    phone               VARCHAR(20)  COMMENT '联系电话',
    emergency_contact   VARCHAR(100) COMMENT '紧急联系人',
    emergency_phone     VARCHAR(20)  COMMENT '紧急联系电话',
    check_in_date       DATE         COMMENT '入住日期',
    health_status       VARCHAR(20)  NOT NULL DEFAULT 'good' COMMENT '健康状态: good/normal/poor/critical',
    medical_history     TEXT         COMMENT '既往病史',
    remark              TEXT         COMMENT '备注',
    deleted             TINYINT(1)   NOT NULL DEFAULT 0,
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人档案';

-- 健康预警表
CREATE TABLE IF NOT EXISTS health_alert (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_code      VARCHAR(30)  NOT NULL UNIQUE COMMENT '预警编号',
    elder_id        BIGINT       NOT NULL COMMENT '老人ID',
    elder_name      VARCHAR(50)  NOT NULL COMMENT '老人姓名',
    room            VARCHAR(50)  COMMENT '床位',
    alert_type      VARCHAR(50)  NOT NULL COMMENT '预警类型',
    level           VARCHAR(10)  NOT NULL COMMENT '危险等级: high/mid/low',
    description     TEXT         COMMENT '预警描述',
    device_id       VARCHAR(50)  COMMENT '触发设备',
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '状态: pending/processing/handled',
    process_remark  TEXT         COMMENT '处理备注',
    process_user_id BIGINT       COMMENT '处理人ID',
    process_time    DATETIME     COMMENT '处理时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康预警';

-- 评估报告表
CREATE TABLE IF NOT EXISTS assessment_report (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    elder_id        BIGINT       NOT NULL COMMENT '老人ID',
    elder_name      VARCHAR(50)  NOT NULL COMMENT '老人姓名',
    assess_type     VARCHAR(100) NOT NULL COMMENT '评估类型',
    assess_date     DATE         NOT NULL COMMENT '评估日期',
    evaluator_id    BIGINT       COMMENT '评估人ID',
    evaluator       VARCHAR(50)  COMMENT '评估人姓名',
    score           INT          COMMENT '综合得分',
    level           VARCHAR(10)  COMMENT '评估等级',
    conclusion      TEXT         COMMENT '评估结论',
    details         JSON         COMMENT '详细评估项(JSON)',
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估报告';

-- 设备表
CREATE TABLE IF NOT EXISTS device (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_code     VARCHAR(50)  NOT NULL UNIQUE COMMENT '设备编号',
    name            VARCHAR(100) NOT NULL COMMENT '设备名称',
    type            VARCHAR(50)  NOT NULL COMMENT '设备类型',
    location        VARCHAR(100) COMMENT '安装位置',
    bind_elder_id   BIGINT       COMMENT '绑定老人ID',
    bind_elder      VARCHAR(50)  COMMENT '绑定老人姓名',
    status          VARCHAR(20)  NOT NULL DEFAULT 'normal' COMMENT '状态: normal/offline/fault/maintaining',
    battery         INT          DEFAULT 100 COMMENT '电量百分比',
    last_sync_time  DATETIME     COMMENT '最后同步时间',
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备管理';

-- =============================================
-- 初始化数据 (用户账号由 DataInitializer 自动创建，密码统一为 123456)
-- =============================================

-- 老人档案 (20条)
INSERT IGNORE INTO elder (name, gender, age, id_card, room, doctor_id, doctor, phone, emergency_contact, emergency_phone, check_in_date, health_status, medical_history, create_time, update_time) VALUES
('张桂英', '女', 78, '310101194601150024', 'A101-1', 2, '王建国', '13800001001', '张明(子)',   '13800002001', '2026-02-10', 'good',     '高血压',            '2026-02-10 09:12:18', '2026-03-02 10:45:06'),
('李福贵', '男', 82, '310101194205201031', 'A102-2', 3, '李晓梅', '13800001002', '李红(女)',   '13800002002', '2026-02-18', 'normal',   '糖尿病、高血压',    '2026-02-18 10:05:44', '2026-03-12 14:26:18'),
('王淑华', '女', 75, '310101194903080028', 'B201-1', 4, '张宇',   '13800001003', '王刚(子)',   '13800002003', '2026-02-25', 'good',     '冠心病',            '2026-02-25 11:22:31', '2026-03-18 09:40:12'),
('赵国庆', '男', 88, '310101193608120015', 'B202-2', 2, '王建国', '13800001004', '赵丽(女)',   '13800002004', '2026-03-02', 'poor',     '帕金森',            '2026-03-02 08:58:03', '2026-03-25 11:30:45'),
('孙秀兰', '女', 71, '310101195307220044', 'C301-3', 3, '李晓梅', '13800001005', '孙伟(子)',   '13800002005', '2026-03-10', 'good',     NULL,                '2026-03-10 09:47:55', '2026-03-28 15:12:20'),
('陈志远', '男', 79, '310101194512050037', 'C302-1', 4, '张宇',   '13800001006', '陈芳(女)',   '13800002006', '2026-03-15', 'normal',   '高血脂',            '2026-03-15 14:05:10', '2026-04-02 16:33:58'),
('刘美英', '女', 84, '310101194007140022', 'A103-2', 3, '李晓梅', '13800001007', '刘强(子)',   '13800002007', '2026-03-20', 'critical', '脑梗后遗症',        '2026-03-20 08:33:41', '2026-04-05 10:14:09'),
('周德才', '男', 76, '310101194811300019', 'B203-3', 4, '张宇',   '13800001008', '周敏(女)',   '13800002008', '2026-03-28', 'good',     '关节炎',            '2026-03-28 07:55:22', '2026-04-10 09:48:33'),
('吴凤英', '女', 80, '310101194401100046', 'A201-1', 5, '陈明辉', '13800001009', '吴磊(子)',   '13800002009', '2026-04-02', 'normal',   '慢性支气管炎',      '2026-04-02 10:12:09', '2026-04-16 14:02:45'),
('郑国强', '男', 73, '310101195105180011', 'A202-2', 5, '陈明辉', '13800001010', '郑芳(女)',   '13800002010', '2026-04-06', 'good',     NULL,                '2026-04-06 09:25:37', '2026-04-24 11:36:28'),
('黄秀珍', '女', 86, '310101193812250038', 'B301-1', 6, '刘海燕', '13800001011', '黄明(子)',   '13800002011', '2026-04-12', 'poor',     '骨质疏松、白内障',  '2026-04-12 08:44:53', '2026-04-27 13:28:14'),
('杨永清', '男', 77, '310101194709030025', 'B302-2', 6, '刘海燕', '13800001012', '杨丽(女)',   '13800002012', '2026-04-18', 'normal',   '前列腺增生',        '2026-04-18 09:10:15', '2026-05-03 10:59:51'),
('马桂花', '女', 69, '310101195506170042', 'C101-1', 2, '王建国', '13800001013', '马军(子)',   '13800002013', '2026-04-24', 'good',     NULL,                '2026-04-24 15:18:22', '2026-05-08 16:42:33'),
('胡大海', '男', 91, '310101193303090017', 'C102-2', 3, '李晓梅', '13800001014', '胡英(女)',   '13800002014', '2026-04-28', 'critical', '心衰、房颤',        '2026-04-28 10:01:47', '2026-05-12 12:20:05'),
('林玉兰', '女', 74, '310101195009110036', 'A301-1', 4, '张宇',   '13800001015', '林浩(子)',   '13800002015', '2026-05-02', 'good',     '轻度贫血',          '2026-05-02 08:20:30', '2026-05-20 09:55:44'),
('徐建军', '男', 81, '310101194302270023', 'A302-2', 5, '陈明辉', '13800001016', '徐敏(女)',   '13800002016', '2026-05-08', 'normal',   '腰椎间盘突出',      '2026-05-08 09:42:11', '2026-05-26 11:30:20'),
('何秀英', '女', 72, '310101195204080048', 'B101-1', 6, '刘海燕', '13800001017', '何刚(子)',   '13800002017', '2026-05-12', 'good',     NULL,                '2026-05-12 10:18:55', '2026-05-30 12:08:37'),
('唐国民', '男', 85, '310101193907150013', 'B102-2', 2, '王建国', '13800001018', '唐芳(女)',   '13800002018', '2026-05-18', 'poor',     '阿尔茨海默症早期',  '2026-05-18 07:59:24', '2026-06-02 09:14:21'),
('沈桂芳', '女', 70, '310101195410220032', 'C201-1', 3, '李晓梅', '13800001019', '沈伟(子)',   '13800002019', '2026-05-24', 'good',     NULL,                '2026-05-24 09:33:06', '2026-06-05 10:27:18'),
('韩志刚', '男', 83, '310101194108120021', 'C202-2', 4, '张宇',   '13800001020', '韩丽娜(女)', '13800002020', '2026-05-30', 'normal',   '高血压、糖尿病',    '2026-05-30 09:05:12', '2026-06-07 11:40:29');

-- 健康预警 (15条)
INSERT IGNORE INTO health_alert (alert_code, elder_id, elder_name, room, alert_type, level, description, device_id, status, process_time, create_time, update_time) VALUES
('AL001', 1,  '张桂英', 'A101-1', '血压异常',    'high', '收缩压165mmHg，超过警戒值',                 'DEV-0012', 'handled',    '2026-05-31 08:03:22', '2026-05-31 07:42:15', '2026-05-31 08:03:22'),
('AL002', 2,  '李福贵', 'A102-2', '跌倒检测',    'high', '检测到老人发生跌倒，请立即确认',           'DEV-0015', 'pending',    NULL,                    '2026-06-07 09:18:09', '2026-06-07 09:18:09'),
('AL003', 3,  '王淑华', 'B201-1', '长时间卧床',  'mid',  '超过4小时无活动状态',                       'DEV-0022', 'processing', NULL,                    '2026-06-05 06:57:44', '2026-06-05 06:57:44'),
('AL004', 4,  '赵国庆', 'B202-2', '心率异常',    'high', '心率102次/分，持续10分钟',                  'DEV-0018', 'pending',    NULL,                    '2026-06-02 22:31:05', '2026-06-02 22:31:05'),
('AL005', 5,  '孙秀兰', 'C301-3', '体温异常',    'low',  '体温38.2°C，轻度发热',                     'DEV-0031', 'handled',    '2026-06-04 19:28:41', '2026-06-04 19:12:33', '2026-06-04 19:28:41'),
('AL006', 7,  '刘美英', 'A103-2', '血氧偏低',    'high', '血氧饱和度89%，低于正常值',                 'DEV-0033', 'pending',    NULL,                    '2026-06-06 08:46:27', '2026-06-06 08:46:27'),
('AL007', 9,  '吴凤英', 'A201-1', '呼吸频率异常','mid',  '呼吸频率26次/分，偏快',                     'DEV-0035', 'handled',    '2026-06-03 23:17:55', '2026-06-03 23:05:12', '2026-06-03 23:17:55'),
('AL008', 11, '黄秀珍', 'B301-1', '跌倒检测',    'high', '夜间检测到跌倒事件',                        'DEV-0037', 'pending',    NULL,                    '2026-06-08 02:14:08', '2026-06-08 02:14:08'),
('AL009', 14, '胡大海', 'C102-2', '心率异常',    'high', '心率38次/分，心动过缓',                     'DEV-0038', 'processing', NULL,                    '2026-06-06 07:12:49', '2026-06-06 07:12:49'),
('AL010', 4,  '赵国庆', 'B202-2', '离床超时',    'mid',  '凌晨3:00离床超过30分钟未返回',              'DEV-0022', 'handled',    '2026-06-04 03:39:02', '2026-06-04 03:06:18', '2026-06-04 03:39:02'),
('AL011', 16, '徐建军', 'A302-2', '血糖异常',    'mid',  '空腹血糖12.3mmol/L，偏高',                  'DEV-0040', 'pending',    NULL,                    '2026-06-05 06:21:44', '2026-06-05 06:21:44'),
('AL012', 18, '唐国民', 'B102-2', '走失预警',    'high', '老人离开安全区域，定位显示在院外',          'DEV-0012', 'pending',    NULL,                    '2026-06-05 07:04:31', '2026-06-05 07:04:31'),
('AL013', 1,  '张桂英', 'A101-1', '血压异常',    'mid',  '收缩压148mmHg，偏高',                       'DEV-0012', 'handled',    '2026-06-01 14:37:19', '2026-06-01 14:11:05', '2026-06-01 14:37:19'),
('AL014', 20, '韩志刚', 'C202-2', '体温异常',    'low',  '体温37.5°C，低热',                          'DEV-0042', 'handled',    '2026-06-07 06:05:06', '2026-06-07 05:48:52', '2026-06-07 06:05:06'),
('AL015', 12, '杨永清', 'B302-2', '睡眠质量差',  'low',  '翻身次数过多，深睡时间不足2小时',          'DEV-0043', 'pending',    NULL,                    '2026-06-02 04:08:27', '2026-06-02 04:08:27');

-- 评估报告 (12条)
INSERT IGNORE INTO assessment_report (elder_id, elder_name, assess_type, assess_date, evaluator_id, evaluator, score, level, conclusion) VALUES
(1,  '张桂英', 'ADL生活能力评估',    '2026-05-26', 2, '王建国', 82, '良', '生活自理能力良好，日常起居无需协助'),
(2,  '李福贵', '认知功能评估(MMSE)', '2026-05-27', 3, '李晓梅', 68, '中', '认知功能中度减退，需定期复查'),
(3,  '王淑华', '营养状况评估(MNA)',  '2026-05-25', 4, '张宇',   91, '优', '营养状况优良，饮食结构合理'),
(4,  '赵国庆', '跌倒风险评估',       '2026-05-30', 2, '王建国', 45, '差', '跌倒风险高，建议加强看护、使用辅助器具'),
(5,  '孙秀兰', 'ADL生活能力评估',    '2026-05-31', 3, '李晓梅', 88, '良', '生活能力良好'),
(7,  '刘美英', '跌倒风险评估',       '2026-05-24', 5, '陈明辉', 38, '差', '高跌倒风险，需24小时陪护'),
(9,  '吴凤英', '营养状况评估(MNA)',  '2026-06-01', 6, '刘海燕', 75, '中', '营养状况一般，建议增加蛋白质摄入'),
(11, '黄秀珍', '认知功能评估(MMSE)', '2026-05-28', 4, '张宇',   55, '中', '轻度认知障碍，建议开展认知训练'),
(14, '胡大海', 'ADL生活能力评估',    '2026-06-02', 2, '王建国', 35, '差', '日常活动高度依赖，需全面护理支持'),
(16, '徐建军', '疼痛评估(VAS)',      '2026-05-29', 5, '陈明辉', 60, '中', '慢性腰痛中度，建议理疗+药物联合'),
(18, '唐国民', '认知功能评估(MMSE)', '2026-05-23', 3, '李晓梅', 48, '差', '认知功能明显下降，高度疑似阿尔茨海默症'),
(20, '韩志刚', '营养状况评估(MNA)',  '2026-06-03', 6, '刘海燕', 85, '良', '营养状态良好，继续当前饮食方案');

-- 设备 (15条)
INSERT IGNORE INTO device (device_code, name, type, location, bind_elder_id, bind_elder, status, battery, last_sync_time) VALUES
('DEV-0012', '智能手环-12',     '智能手环',   'A101-1',  1,    '张桂英', 'normal',      78,  '2026-06-09 08:14:36'),
('DEV-0015', '智能手环-15',     '智能手环',   'A102-2',  2,    '李福贵', 'normal',      45,  '2026-06-07 07:52:11'),
('DEV-0022', '床垫传感器-22',   '床垫传感器', 'B201-1',  3,    '王淑华', 'normal',      90,  '2026-06-05 23:49:05'),
('DEV-0018', '智能手环-18',     '智能手环',   'B202-2',  4,    '赵国庆', 'offline',     12,  '2026-06-04 22:10:54'),
('CAM-0001', '监控摄像头-A走廊','摄像头',     '走廊A区', NULL,  NULL,     'normal',      100, '2026-06-10 08:31:42'),
('DEV-0031', '血压仪-31',       '血压仪',     'C301-3',  5,    '孙秀兰', 'maintaining', 65,  '2026-06-06 10:07:19'),
('DEV-0028', '血糖仪-28',       '血糖仪',     'C302-1',  6,    '陈志远', 'fault',       30,  '2026-06-03 16:12:33'),
('DEV-0033', '智能手环-33',     '智能手环',   'A103-2',  7,    '刘美英', 'normal',      62,  '2026-06-08 06:58:25'),
('DEV-0035', '床垫传感器-35',   '床垫传感器', 'A201-1',  9,    '吴凤英', 'normal',      85,  '2026-06-01 05:43:02'),
('DEV-0037', '智能手环-37',     '智能手环',   'B301-1',  11,   '黄秀珍', 'normal',      55,  '2026-06-02 21:37:48'),
('DEV-0038', '智能手环-38',     '智能手环',   'C102-2',  14,   '胡大海', 'normal',      70,  '2026-06-11 01:26:59'),
('DEV-0040', '血糖仪-40',       '血糖仪',     'A302-2',  16,   '徐建军', 'normal',      88,  '2026-06-04 06:54:11'),
('DEV-0042', '智能手环-42',     '智能手环',   'C202-2',  20,   '韩志刚', 'normal',      72,  '2026-06-06 12:22:07'),
('DEV-0043', '床垫传感器-43',   '床垫传感器', 'B302-2',  12,   '杨永清', 'normal',      95,  '2026-06-12 07:09:33'),
('CAM-0002', '监控摄像头-B走廊','摄像头',     '走廊B区', NULL,  NULL,     'normal',      100, '2026-06-09 09:02:21');
