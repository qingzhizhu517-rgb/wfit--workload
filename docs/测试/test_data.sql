-- ============================================================
-- WFIT 工作量管理系统 端到端测试数据
-- 幂等脚本：先清理再插入，使用 semester='2025-2026-2' 隔离
-- 测试账号（密码均为 123456）：
--   admin_test(1001)  jiaowu_test(1002)  teacher_test(1003)  leader_test(1004)
-- 新增教师：test_prof(2001) 讲师  test_aprof(2002) 教授
-- 执行：mysql -u root -p wflg_workload < docs/测试/test_data.sql
-- ============================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------
-- 0. 清理本次测试专用数据（ID 范围 9000+，semester=2025-2026-2）
-- -----------------------------------------------------------
DELETE FROM biz_wl_theory WHERE item_id >= 9000;
DELETE FROM biz_wl_practice WHERE item_id >= 9000;
DELETE FROM biz_wl_internship_training WHERE item_id >= 9000;
DELETE FROM biz_wl_course_design WHERE item_id >= 9000;
DELETE FROM biz_wl_thesis WHERE item_id >= 9000;
DELETE FROM biz_wl_concentrated_internship WHERE item_id >= 9000;
DELETE FROM biz_wl_management WHERE item_id >= 9000;
DELETE FROM biz_audit_log WHERE summary_id >= 9000;
DELETE FROM biz_allowance_item WHERE user_id IN (2001, 2002) AND semester = '2025-2026-2';
DELETE FROM biz_pay_record WHERE user_id IN (2001, 2002) AND semester = '2025-2026-2';
DELETE FROM biz_workload_summary WHERE user_id IN (2001, 2002) AND semester = '2025-2026-2';
DELETE FROM biz_workload_item WHERE id >= 9000;
DELETE FROM biz_role_assignment WHERE user_id IN (2001, 2002) AND semester = '2025-2026-2';
DELETE FROM biz_teaching_task WHERE id >= 9000;
DELETE FROM biz_teacher_profile WHERE user_id IN (2001, 2002);
DELETE FROM sys_user_role WHERE user_id IN (2001, 2002);
DELETE FROM sys_user WHERE user_id IN (2001, 2002);

-- -----------------------------------------------------------
-- 1. 新增测试教师用户（sys_user）
-- -----------------------------------------------------------
INSERT INTO sys_user (user_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, dept_id, create_time) VALUES
(2001, 'test_prof',  '测试讲师', 'test_prof@wfit.edu.cn',  '13900002001', '0', '', '$2a$12$8hRkqDsRxg70wdfgq3MpeOotjQj3Hwu0Gr0qcPkDMbLEyZNsvCje6', '0', '0', 103, NOW()),
(2002, 'test_aprof', '测试教授', 'test_aprof@wfit.edu.cn', '13900002002', '0', '', '$2a$12$8hRkqDsRxg70wdfgq3MpeOotjQj3Hwu0Gr0qcPkDMbLEyZNsvCje6', '0', '0', 103, NOW())
ON DUPLICATE KEY UPDATE nick_name=VALUES(nick_name), password=VALUES(password);

-- 分配教师角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
(2001, 4), (2002, 4)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

-- -----------------------------------------------------------
-- 2. 教师档案（biz_teacher_profile）
-- -----------------------------------------------------------
INSERT INTO biz_teacher_profile (user_id, title, teacher_nature, special_status, dept_id) VALUES
(2001, '讲师', '专任', '正常', 103),
(2002, '教授', '专任', '正常', 103)
ON DUPLICATE KEY UPDATE title=VALUES(title), teacher_nature=VALUES(teacher_nature);

-- -----------------------------------------------------------
-- 3. 教学任务（biz_teaching_task）
--    覆盖 G1-G6 各类型，用于测试核算引擎
-- -----------------------------------------------------------
INSERT INTO biz_teaching_task (id, user_id, semester, academic_year, course_name, course_code, education_level, major_category, course_nature, course_level, course_role, student_count, theory_hours, practice_hours, repeat_order, import_source, status) VALUES
-- 测试讲师(2001): G1 理论课 × 2 + G2 实践课 × 1 + G4 课程设计 × 1
(9001, 2001, '2025-2026-2', '2025-2026', '离散数学',     'MATH4001', '本科', '理工类', '必修', '其他', '独立', 80,  48.0, 0.0,  1, 'E2E', 1),
(9002, 2001, '2025-2026-2', '2025-2026', '离散数学',     'MATH4001', '本科', '理工类', '必修', '其他', '独立', 75,  48.0, 0.0,  2, 'E2E', 1),
(9003, 2001, '2025-2026-2', '2025-2026', 'C语言程序设计实验','CS1002', '本科', '理工类', '必修', '其他', '独立', 40,  0.0,  32.0, 1, 'E2E', 1),
-- 测试教授(2002): G1 理论课(大班合堂) + G3 实习实训 + G5 毕业论文 + G6 集中实习
(9004, 2002, '2025-2026-2', '2025-2026', '高等数学B',    'MATH1003', '本科', '理工类', '必修', '省级一流', '主持人', 160, 64.0, 0.0, 1, 'E2E', 1),
(9005, 2002, '2025-2026-2', '2025-2026', '生产实习',     'ME3001',   '本科', '理工类', '必修', '其他', '独立', 35,  0.0,  0.0,  1, 'E2E', 1),
(9006, 2002, '2025-2026-2', '2025-2026', '毕业设计指导',  'ME4001',   '本科', '理工类', '必修', '其他', '独立', 0,   0.0,  0.0,  1, 'E2E', 1),
(9007, 2002, '2025-2026-2', '2025-2026', '企业实训周',    'ME3002',   '本科', '理工类', '必修', '其他', '独立', 30,  0.0,  0.0,  1, 'E2E', 1)
ON DUPLICATE KEY UPDATE course_name=VALUES(course_name);

-- -----------------------------------------------------------
-- 4. 工作量明细主表（biz_workload_item）
-- -----------------------------------------------------------
INSERT INTO biz_workload_item (id, user_id, semester, academic_year, item_type, source_type, task_id, course_name, education_level, major_category, calculated_workload, status) VALUES
-- 测试讲师 G1 × 2（含重复授课）
(9001, 2001, '2025-2026-2', '2025-2026', 'G1', 'IMPORT', 9001, '离散数学',         '本科', '理工类', 0.00, 0),
(9002, 2001, '2025-2026-2', '2025-2026', 'G1', 'IMPORT', 9002, '离散数学(重复)',    '本科', '理工类', 0.00, 0),
-- 测试讲师 G2 实践课
(9003, 2001, '2025-2026-2', '2025-2026', 'G2', 'IMPORT', 9003, 'C语言程序设计实验', '本科', '理工类', 0.00, 0),
-- 测试教授 G1 大班合堂（160人 → N=1.2）
(9004, 2002, '2025-2026-2', '2025-2026', 'G1', 'IMPORT', 9004, '高等数学B',        '本科', '理工类', 0.00, 0),
-- 测试教授 G3 实习实训
(9005, 2002, '2025-2026-2', '2025-2026', 'G3', 'IMPORT', 9005, '生产实习',          '本科', '理工类', 0.00, 0),
-- 测试教授 G5 毕业论文
(9006, 2002, '2025-2026-2', '2025-2026', 'G5', 'IMPORT', 9006, '毕业设计指导',      '本科', '理工类', 0.00, 0),
-- 测试教授 G6 集中实习
(9007, 2002, '2025-2026-2', '2025-2026', 'G6', 'IMPORT', 9007, '企业实训周',        '本科', '理工类', 0.00, 0)
ON DUPLICATE KEY UPDATE course_name=VALUES(course_name);

-- -----------------------------------------------------------
-- 5. G1 理论课明细（biz_wl_theory）
-- -----------------------------------------------------------
INSERT INTO biz_wl_theory (item_id, J1, C1, K1, Q1, Q2, Q3, N) VALUES
-- 测试讲师: 离散数学 第1次 (48学时, 必修K1=1.1, 80人无合堂)
(9001, 48.00, 1.00, 1.10, 1.00, 1.00, 1.00, 1.00),
-- 测试讲师: 离散数学 第2次重复 (C1=0.9)
(9002, 48.00, 0.90, 1.10, 1.00, 1.00, 1.00, 1.00),
-- 测试教授: 高等数学B (64学时, 省级一流Q2=1.5, 160人合堂N=1.2)
(9004, 64.00, 1.00, 1.10, 1.00, 1.50, 1.00, 1.20);

-- -----------------------------------------------------------
-- 6. G2 实践课明细（biz_wl_practice）
-- -----------------------------------------------------------
INSERT INTO biz_wl_practice (item_id, J2, K, C2, Q1, Q2, Q3) VALUES
-- 测试讲师: C语言实验 (32学时, 理工K=1.0, C2=0.9)
(9003, 32.00, 1.00, 0.90, 1.00, 1.00, 1.00);

-- -----------------------------------------------------------
-- 7. G3 实习实训明细（biz_wl_internship_training）
-- -----------------------------------------------------------
INSERT INTO biz_wl_internship_training (item_id, T, D, K, Q1, Q2, Q3) VALUES
-- 测试教授: 生产实习 (15天, 理工D=4.0)
(9005, 15.00, 4.00, 1.00, 1.00, 1.00, 1.00);

-- -----------------------------------------------------------
-- 8. G5 毕业论文明细（biz_wl_thesis）
-- -----------------------------------------------------------
INSERT INTO biz_wl_thesis (item_id, R5, K5, education_level, major) VALUES
-- 测试教授: 指导8人理工类本科论文 (R5=8, K5=9, 公式=72)
(9006, 8, 9.00, '本科', '理工类');

-- -----------------------------------------------------------
-- 9. G6 集中实习明细（biz_wl_concentrated_internship）
-- -----------------------------------------------------------
INSERT INTO biz_wl_concentrated_internship (item_id, W, R6) VALUES
-- 测试教授: 集中实习3周, 25人(超上限20, 应截断) 公式=3×20×0.4=24
(9007, 3.00, 25);

-- -----------------------------------------------------------
-- 10. 岗位任职（biz_role_assignment）— 用于 G11 生成
-- -----------------------------------------------------------
INSERT INTO biz_role_assignment (id, user_id, role_type, target, start_date, end_date, allowance_rate, semester, status) VALUES
(9001, 2001, '班主任',   '计科2401班', '2026-02-20', '2026-07-15', 180.00, '2025-2026-2', 0),
(9002, 2002, '教研室主任', '数学教研室', '2026-02-20', '2026-07-15', 120.00, '2025-2026-2', 0)
ON DUPLICATE KEY UPDATE target=VALUES(target);

-- -----------------------------------------------------------
-- 11. 验证查询（执行后可手动检查）
-- -----------------------------------------------------------
-- SELECT '=== 测试用户 ===' AS info;
-- SELECT user_id, user_name, nick_name, dept_id FROM sys_user WHERE user_id IN (2001, 2002);
-- SELECT '=== 教师档案 ===' AS info;
-- SELECT * FROM biz_teacher_profile WHERE user_id IN (2001, 2002);
-- SELECT '=== 教学任务 ===' AS info;
-- SELECT id, user_id, course_name, student_count, theory_hours, practice_hours FROM biz_teaching_task WHERE id >= 9000;
-- SELECT '=== 工作量明细 ===' AS info;
-- SELECT id, user_id, item_type, course_name, calculated_workload, status FROM biz_workload_item WHERE id >= 9000;
-- SELECT '=== 岗位任职 ===' AS info;
-- SELECT * FROM biz_role_assignment WHERE user_id IN (2001, 2002) AND semester = '2025-2026-2';
