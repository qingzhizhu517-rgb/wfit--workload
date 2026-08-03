-- ============================================================
-- 三端测试账号
-- Admin端: admin / admin123
-- 教务端: jiaowu / jiaowu123
-- 教师端: teacher / teacher123
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

-- 4. 教务助理角色的菜单权限（管理类 + 审批类）
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 3, menu_id FROM sys_menu WHERE menu_id >= 2000 AND menu_id < 3000;

-- 5. 教师角色的菜单权限（查询 + 申报类）
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 4, menu_id FROM sys_menu WHERE menu_id >= 2000 AND menu_id < 3000;

-- 6. 院领导角色的菜单权限（审批 + 报表类）
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 5, menu_id FROM sys_menu WHERE menu_id >= 2000 AND menu_id < 3000;

-- 7. 创建测试用户（密码均为 bcrypt 加密的 123456）
INSERT INTO sys_user (user_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, dept_id, create_time) VALUES
(1001, 'admin_test', '管理员测试', 'admin@wfit.edu.cn', '13800001001', '0', '', '$2a$12$8hRkqDsRxg70wdfgq3MpeOotjQj3Hwu0Gr0qcPkDMbLEyZNsvCje6', '0', '0', 103, NOW()),
(1002, 'jiaowu_test', '教务测试', 'jiaowu@wfit.edu.cn', '13800001002', '0', '', '$2a$12$8hRkqDsRxg70wdfgq3MpeOotjQj3Hwu0Gr0qcPkDMbLEyZNsvCje6', '0', '0', 103, NOW()),
(1003, 'teacher_test', '教师测试', 'teacher@wfit.edu.cn', '13800001003', '0', '', '$2a$12$8hRkqDsRxg70wdfgq3MpeOotjQj3Hwu0Gr0qcPkDMbLEyZNsvCje6', '0', '0', 103, NOW())
ON DUPLICATE KEY UPDATE nick_name=VALUES(nick_name), password=VALUES(password);

-- 8. 分配角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1001, 1),  -- admin_test → 超级管理员
(1002, 3),  -- jiaowu_test → 教务助理
(1003, 4)   -- teacher_test → 教师
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);
