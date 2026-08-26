-- ============================================================
-- 四端测试账号（密码均为 123456）
-- Admin端:  admin_test  / 123456
-- 教务端:   jiaowu_test / 123456
-- 教师端:   teacher_test / 123456
-- 院领导端: leader_test / 123456
-- ============================================================

-- 1. 创建业务管理员角色（role_id=2）— admin_test 使用，避免 role_id=1 被 RuoYi 框架硬编码跳过
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES (2, '业务管理员', 'biz_admin', 2, '1', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '业务管理员角色（测试用，全部数据权限）')
ON DUPLICATE KEY UPDATE role_name='业务管理员', role_key='biz_admin', data_scope='1', status='0', del_flag='0';

-- 2. 创建教务角色（role_id=3）— data_scope='1' 全部数据权限，避免 sys_role_dept 为空导致查询返回零行
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES (3, '教务助理', 'assistant', 3, '1', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '教务助理角色')
ON DUPLICATE KEY UPDATE role_name='教务助理', role_key='assistant', data_scope='1', status='0', del_flag='0';

-- 3. 创建教师角色（role_id=4）— data_scope='1'（DataScopeUtil 会强制按 userId 过滤）
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES (4, '教师', 'teacher', 4, '1', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '教师角色')
ON DUPLICATE KEY UPDATE role_name='教师', role_key='teacher', data_scope='1', status='0', del_flag='0';

-- 4. 创建院领导角色（role_id=5）— data_scope='1' 全部数据权限
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES (5, '院领导', 'leader', 5, '1', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '院领导角色')
ON DUPLICATE KEY UPDATE role_name='院领导', role_key='leader', data_scope='1', status='0', del_flag='0';

-- 4. 清除旧的角色菜单分配（可重复执行）
DELETE FROM sys_role_menu WHERE role_id IN (1, 2, 3, 4, 5) AND menu_id >= 2000;

-- 4.1 管理员角色（role_id=1）— 拥有全部业务菜单（仅供原始 admin 用户使用）
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 1, menu_id FROM sys_menu WHERE menu_id >= 2000;

-- 4.2 业务管理员角色（role_id=2）— 拥有全部业务菜单（admin_test 使用）
INSERT INTO sys_role_menu(role_id, menu_id) SELECT 2, menu_id FROM sys_menu WHERE menu_id >= 2000;

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
-- 按钮权限：审批（教务提交/教务审核/驳回/解锁，见 08_review_fixes.sql 回归补丁）
(3, 20205), (3, 20206), (3, 20207), (3, 20209),
-- 按钮权限：导出报表
(3, 20210), (3, 20211),
-- 按钮权限：酬金记录
(3, 20212), (3, 20213), (3, 20214),
-- 按钮权限：明细/配置页增删改查导出（原 15_fix_menu_buttons.sql 并入，见 Bug排查报告-2026-08-26 Bug #1）
-- G1~G6/G11 明细页
(3, 20111), (3, 20112), (3, 20113), (3, 20114), (3, 20115),
(3, 20121), (3, 20122), (3, 20123), (3, 20124), (3, 20125),
(3, 20131), (3, 20132), (3, 20133), (3, 20134), (3, 20135),
(3, 20141), (3, 20142), (3, 20143), (3, 20144), (3, 20145),
(3, 20151), (3, 20152), (3, 20153), (3, 20154), (3, 20155),
(3, 20161), (3, 20162), (3, 20163), (3, 20164), (3, 20165),
(3, 20171), (3, 20172), (3, 20173), (3, 20174), (3, 20175),
-- 数据导入批次
(3, 20041), (3, 20042), (3, 20043), (3, 20044), (3, 20045),
-- 其他酬金 / 酬金记录增删 / 学期汇总新增
(3, 20221), (3, 20222), (3, 20223), (3, 20224), (3, 20225),
(3, 20218), (3, 20219), (3, 20220),
-- 配置：类别字典 / 计算规则 / 酬金标准
(3, 20401), (3, 20402), (3, 20403), (3, 20404), (3, 20405),
(3, 20411), (3, 20412), (3, 20413), (3, 20414), (3, 20415),
(3, 20421), (3, 20422), (3, 20423), (3, 20424), (3, 20425);

-- 6. 教师角色的菜单权限（role_id=4）— 查看自己的工作量、自主申报
INSERT INTO sys_role_menu(role_id, menu_id) VALUES
-- 目录
(4, 2000),
-- 工作量明细页面（TeacherDashboard 近期明细需要 menu_id 2010）
(4, 2010),
-- 学期汇总（查看自己的）
(4, 2020),
-- 酬金记录（查看自己的）
(4, 2021),
-- 我的工作量（自主申报）
(4, 2030),
-- 按钮权限：工作量明细查询（兜底）
(4, 20101),
-- 按钮权限：学期汇总 查询
(4, 20201),
-- 按钮权限：审批提交（修复教师提交按钮永不渲染，见 08_review_fixes.sql / P3-03）
(4, 20205),
-- 按钮权限：导出个人工作量
(4, 20210),
-- 按钮权限：酬金记录 查询
(4, 20212),
-- 按钮权限：教师确认（教师本人确认汇总签字，见 08_review_fixes.sql / P3-02）
(4, 20215);

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
-- 按钮权限：审批（签字；08_review_fixes.sql 已撤销错位的 20206 approve 授权，见 P2-03）
(5, 20208),
-- 按钮权限：导出报表
(5, 20210), (5, 20211),
-- 按钮权限：酬金记录 查询/导出
(5, 20212), (5, 20214),
-- 按钮权限：管理员仪表盘统计（LeaderDashboard 需要 menu_id 20216）
(5, 20216);

-- 8. 创建测试用户（密码均为 bcrypt 加密的 123456）
INSERT INTO sys_user (user_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, dept_id, create_time, pwd_update_date) VALUES
(1001, 'admin_test', '管理员测试', 'admin@wfit.edu.cn', '13800001001', '0', '', '$2a$12$8hRkqDsRxg70wdfgq3MpeOotjQj3Hwu0Gr0qcPkDMbLEyZNsvCje6', '0', '0', 103, NOW(), NOW()),
(1002, 'jiaowu_test', '教务测试', 'jiaowu@wfit.edu.cn', '13800001002', '0', '', '$2a$12$8hRkqDsRxg70wdfgq3MpeOotjQj3Hwu0Gr0qcPkDMbLEyZNsvCje6', '0', '0', 103, NOW(), NOW()),
(1003, 'teacher_test', '教师测试', 'teacher@wfit.edu.cn', '13800001003', '0', '', '$2a$12$8hRkqDsRxg70wdfgq3MpeOotjQj3Hwu0Gr0qcPkDMbLEyZNsvCje6', '0', '0', 103, NOW(), NOW()),
(1004, 'leader_test', '院领导测试', 'leader@wfit.edu.cn', '13800001004', '0', '', '$2a$12$8hRkqDsRxg70wdfgq3MpeOotjQj3Hwu0Gr0qcPkDMbLEyZNsvCje6', '0', '0', 103, NOW(), NOW())
ON DUPLICATE KEY UPDATE nick_name=VALUES(nick_name), password=VALUES(password), pwd_update_date=NOW();

-- 9. 分配角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1001, 2),  -- admin_test → 业务管理员（role_id=2，避免 role_id=1 被框架跳过）
(1002, 3),  -- jiaowu_test → 教务助理
(1003, 4),  -- teacher_test → 教师
(1004, 5)   -- leader_test → 院领导
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);
