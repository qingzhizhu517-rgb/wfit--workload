-- ============================================================
-- 四端测试账号（密码均为 123456）
-- Admin端:  admin_test  / 123456
-- 教务端:   jiaowu_test / 123456
-- 教师端:   teacher_test / 123456
-- 院领导端: leader_test / 123456
-- ============================================================

-- 1. 创建教务角色（role_id=3）
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES (3, '教务助理', 'assistant', 3, '2', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '教务助理角色')
ON DUPLICATE KEY UPDATE role_name='教务助理', role_key='assistant', status='0', del_flag='0';

-- 2. 创建教师角色（role_id=4）
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES (4, '教师', 'teacher', 4, '2', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '教师角色')
ON DUPLICATE KEY UPDATE role_name='教师', role_key='teacher', status='0', del_flag='0';

-- 3. 创建院领导角色（role_id=5）
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES (5, '院领导', 'leader', 5, '2', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '院领导角色')
ON DUPLICATE KEY UPDATE role_name='院领导', role_key='leader', status='0', del_flag='0';

-- 4. 清除旧的角色菜单分配（可重复执行）
DELETE FROM sys_role_menu WHERE role_id IN (1, 3, 4, 5) AND menu_id >= 2000;

-- 4.1 管理员角色（role_id=1）— 拥有全部业务菜单
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 1, menu_id FROM sys_menu WHERE menu_id >= 2000;

-- 5. 教务助理角色的菜单权限（role_id=3）— 数据管理、核算、导入、审批
INSERT INTO sys_role_menu(role_id, menu_id) VALUES
-- 目录
(3, 2000),
-- 基础数据：教师档案、教学任务、岗位任职、数据导入
(3, 2001), (3, 2002), (3, 2003), (3, 2004),
-- 工作量明细：主表 + G1~G6 + G11
(3, 2010), (3, 2011), (3, 2012), (3, 2013), (3, 2014), (3, 2015), (3, 2016), (3, 2017),
-- 汇总与酬金
(3, 2020), (3, 2021), (3, 2022),
-- 配置
(3, 2040), (3, 2041), (3, 2042),
-- 按钮权限：教师档案 CRUD+导出
(3, 20011), (3, 20012), (3, 20013), (3, 20014), (3, 20015),
-- 按钮权限：教学任务 CRUD+导入导出
(3, 20021), (3, 20022), (3, 20023), (3, 20024), (3, 20025), (3, 20026),
-- 按钮权限：工作量明细 增删改查导出+重算+G11
(3, 20101), (3, 20102), (3, 20103), (3, 20104), (3, 20105), (3, 20301), (3, 20302),
-- 按钮权限：学期汇总 增删改查导出
(3, 20201), (3, 20202), (3, 20203), (3, 20204),
-- 按钮权限：审批（教务提交）
(3, 20205),
-- 按钮权限：导出报表
(3, 20210), (3, 20211),
-- 按钮权限：酬金记录
(3, 20212), (3, 20213), (3, 20214);

-- 6. 教师角色的菜单权限（role_id=4）— 查看自己的工作量、自主申报
INSERT INTO sys_role_menu(role_id, menu_id) VALUES
-- 目录
(4, 2000),
-- 学期汇总（查看自己的）
(4, 2020),
-- 酬金记录（查看自己的）
(4, 2021),
-- 我的工作量（自主申报）
(4, 2030),
-- 按钮权限：学期汇总 查询
(4, 20201),
-- 按钮权限：导出个人工作量
(4, 20210),
-- 按钮权限：酬金记录 查询
(4, 20212);

-- 7. 院领导角色的菜单权限（role_id=5）— 审批签字、查看报表
INSERT INTO sys_role_menu(role_id, menu_id) VALUES
-- 目录
(5, 2000),
-- 学期汇总
(5, 2020),
-- 酬金记录
(5, 2021),
-- 按钮权限：学期汇总 查询/导出
(5, 20201), (5, 20204),
-- 按钮权限：审批（通过、签字）
(5, 20206), (5, 20208),
-- 按钮权限：导出报表
(5, 20210), (5, 20211),
-- 按钮权限：酬金记录 查询/导出
(5, 20212), (5, 20214);

-- 8. 创建测试用户（密码均为 bcrypt 加密的 123456）
INSERT INTO sys_user (user_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, dept_id, create_time) VALUES
(1001, 'admin_test', '管理员测试', 'admin@wfit.edu.cn', '13800001001', '0', '', '$2a$12$8hRkqDsRxg70wdfgq3MpeOotjQj3Hwu0Gr0qcPkDMbLEyZNsvCje6', '0', '0', 103, NOW()),
(1002, 'jiaowu_test', '教务测试', 'jiaowu@wfit.edu.cn', '13800001002', '0', '', '$2a$12$8hRkqDsRxg70wdfgq3MpeOotjQj3Hwu0Gr0qcPkDMbLEyZNsvCje6', '0', '0', 103, NOW()),
(1003, 'teacher_test', '教师测试', 'teacher@wfit.edu.cn', '13800001003', '0', '', '$2a$12$8hRkqDsRxg70wdfgq3MpeOotjQj3Hwu0Gr0qcPkDMbLEyZNsvCje6', '0', '0', 103, NOW()),
(1004, 'leader_test', '院领导测试', 'leader@wfit.edu.cn', '13800001004', '0', '', '$2a$12$8hRkqDsRxg70wdfgq3MpeOotjQj3Hwu0Gr0qcPkDMbLEyZNsvCje6', '0', '0', 103, NOW())
ON DUPLICATE KEY UPDATE nick_name=VALUES(nick_name), password=VALUES(password);

-- 9. 分配角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1001, 1),  -- admin_test → 超级管理员
(1002, 3),  -- jiaowu_test → 教务助理
(1003, 4),  -- teacher_test → 教师
(1004, 5)   -- leader_test → 院领导
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);
