-- ============================================================
-- 潍理工教师工作量管理系统 测试数据
-- 教师档案 + 教学任务 + 工作量明细 + 汇总
-- 执行: /usr/local/mysql/bin/mysql -h 127.0.0.1 -uroot -p123456 wflg_workload < 04_biz_test_data.sql
-- ============================================================

-- -----------------------------------------------------------
-- 1. 补充测试用户（若依 sys_user）
-- -----------------------------------------------------------
INSERT INTO sys_user (user_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, dept_id, create_time) VALUES
(3, 'zhangsan', '张三', 'zhangsan@wfit.edu.cn', '13800000001', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 103, NOW()),
(4, 'lisi',     '李四', 'lisi@wfit.edu.cn',     '13800000002', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 103, NOW()),
(5, 'wangwu',   '王五', 'wangwu@wfit.edu.cn',   '13800000003', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 105, NOW()),
(6, 'zhaoliu',  '赵六', 'zhaoliu@wfit.edu.cn',  '13800000004', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 105, NOW())
ON DUPLICATE KEY UPDATE nick_name=VALUES(nick_name);

-- 给新用户分配角色（2=普通角色）
INSERT INTO sys_user_role (user_id, role_id) VALUES
(3, 2), (4, 2), (5, 2), (6, 2)
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id);

-- -----------------------------------------------------------
-- 2. 教师档案（biz_teacher_profile）
-- -----------------------------------------------------------
INSERT INTO biz_teacher_profile (user_id, title, teacher_nature, special_status, dept_id) VALUES
(1, '讲师',   '专任', '正常', 103),
(2, '讲师',   '专任', '正常', 105),
(3, '教授',   '专任', '正常', 103),
(4, '副教授', '专任', '正常', 103),
(5, '讲师',   '外聘', '正常', 105),
(6, '助教',   '专任', '正常', 105)
ON DUPLICATE KEY UPDATE title=VALUES(title), teacher_nature=VALUES(teacher_nature);

-- -----------------------------------------------------------
-- 3. 教学任务（biz_teaching_task）— 模拟教务排课
-- -----------------------------------------------------------
INSERT INTO biz_teaching_task (id, user_id, semester, academic_year, course_name, course_code, education_level, major_category, course_nature, course_level, course_role, student_count, theory_hours, practice_hours, repeat_order, import_source, status) VALUES
-- 张三 (userId=3): 3门理论课 + 1门实践课
(101, 3, '2025-2026-1', '2025-2026', '高等数学A',   'MATH1001', '本科', '理工类', '必修', '其他', '独立', 120, 64.0, 0.0,  1, 'SEED', 1),
(102, 3, '2025-2026-1', '2025-2026', '线性代数',   'MATH2001', '本科', '理工类', '必修', '其他', '独立', 90,  48.0, 0.0,  1, 'SEED', 1),
(103, 3, '2025-2026-1', '2025-2026', '概率论',     'MATH3001', '本科', '理工类', '必修', '其他', '独立', 85,  48.0, 0.0,  2, 'SEED', 1),
(104, 3, '2025-2026-1', '2025-2026', '数学实验',   'MATH1002', '本科', '理工类', '必修', '其他', '独立', 40,  0.0,  32.0, 1, 'SEED', 1),

-- 李四 (userId=4): 2门理论课 + 1门毕业论文
(201, 4, '2025-2026-1', '2025-2026', '大学英语I',  'ENG1001',  '本科', '文史类', '必修', '其他', '独立', 150, 48.0, 0.0,  1, 'SEED', 1),
(202, 4, '2025-2026-1', '2025-2026', '大学英语II', 'ENG1002',  '本科', '文史类', '必修', '其他', '独立', 140, 48.0, 0.0,  1, 'SEED', 1),

-- 王五 (userId=5): 1门省级一流课 + 1门实习
(301, 5, '2025-2026-1', '2025-2026', '数据结构',   'CS2001',   '本科', '理工类', '必修', '省级一流', '主持人', 100, 56.0, 0.0, 1, 'SEED', 1),
(302, 5, '2025-2026-1', '2025-2026', '企业实训',   'CS3001',   '本科', '理工类', '必修', '其他', '独立', 30,  0.0,  0.0,  1, 'SEED', 1),

-- 赵六 (userId=6): 1门理论课（助教）
(401, 6, '2025-2026-1', '2025-2026', '计算机基础', 'CS1001',   '本科', '理工类', '选修', '其他', '独立', 60,  32.0, 0.0,  1, 'SEED', 1)
ON DUPLICATE KEY UPDATE course_name=VALUES(course_name);

-- -----------------------------------------------------------
-- 4. 工作量明细主表（biz_workload_item）
-- -----------------------------------------------------------
INSERT INTO biz_workload_item (id, user_id, semester, academic_year, item_type, source_type, task_id, course_name, education_level, major_category, calculated_workload, status) VALUES
-- 张三 G1×3 + G2×1
(501, 3, '2025-2026-1', '2025-2026', 'G1', 'IMPORT', 101, '高等数学A', '本科', '理工类', 0.00, 0),
(502, 3, '2025-2026-1', '2025-2026', 'G1', 'IMPORT', 102, '线性代数',   '本科', '理工类', 0.00, 0),
(503, 3, '2025-2026-1', '2025-2026', 'G1', 'IMPORT', 103, '概率论',     '本科', '理工类', 0.00, 0),
(504, 3, '2025-2026-1', '2025-2026', 'G2', 'IMPORT', 104, '数学实验',   '本科', '理工类', 0.00, 0),

-- 李四 G1×2（合堂课）
(505, 4, '2025-2026-1', '2025-2026', 'G1', 'IMPORT', 201, '大学英语I',  '本科', '文史类', 0.00, 0),
(506, 4, '2025-2026-1', '2025-2026', 'G1', 'IMPORT', 202, '大学英语II', '本科', '文史类', 0.00, 0),

-- 王五 G1×1（省级一流主持人）+ G3×1
(507, 5, '2025-2026-1', '2025-2026', 'G1', 'IMPORT', 301, '数据结构',   '本科', '理工类', 0.00, 0),
(508, 5, '2025-2026-1', '2025-2026', 'G3', 'IMPORT', 302, '企业实训',   '本科', '理工类', 0.00, 0),

-- 赵六 G1×1（选修课）
(509, 6, '2025-2026-1', '2025-2026', 'G1', 'IMPORT', 401, '计算机基础', '本科', '理工类', 0.00, 0),

-- 张三 G8 自主申报
(510, 3, '2025-2026-1', '2025-2026', 'G8', 'MANUAL', NULL, '指导ACM竞赛', NULL, NULL, 20.00, 0),

-- 李四 G11 自主申报（班主任）
(511, 4, '2025-2026-1', '2025-2026', 'G11', 'MANUAL', NULL, '英语2301班班主任', NULL, NULL, 180.00, 0),

-- 张三 G4 课程设计
(601, 3, '2025-2026-1', '2025-2026', 'G4', 'IMPORT', NULL, '数据结构课程设计', NULL, NULL, 0.00, 0),

-- 李四 G5 毕业论文
(602, 4, '2025-2026-1', '2025-2026', 'G5', 'IMPORT', NULL, '英语专业毕业论文指导', NULL, NULL, 0.00, 0),

-- 王五 G6 集中实习
(603, 5, '2025-2026-1', '2025-2026', 'G6', 'IMPORT', NULL, '计算机专业集中实习', NULL, NULL, 0.00, 0)
ON DUPLICATE KEY UPDATE course_name=VALUES(course_name);

-- -----------------------------------------------------------
-- 5. G1 理论课明细（biz_wl_theory）
-- -----------------------------------------------------------
INSERT INTO biz_wl_theory (item_id, J1, C1, K1, Q1, Q2, Q3, N) VALUES
-- 张三: 高等数学A (64学时, 必修K1=1.1, 合堂120人N=1.1)
(501, 64.00, 1.00, 1.10, 1.00, 1.00, 1.00, 1.10),
-- 张三: 线性代数 (48学时, 必修, 90人无合堂)
(502, 48.00, 1.00, 1.10, 1.00, 1.00, 1.00, 1.00),
-- 张三: 概率论 (48学时, 第二次重复C1=0.9)
(503, 48.00, 0.90, 1.10, 1.00, 1.00, 1.00, 1.00),
-- 李四: 大学英语I (48学时, 合堂150人N=1.1)
(505, 48.00, 1.00, 1.10, 1.00, 1.00, 1.00, 1.10),
-- 李四: 大学英语II (48学时, 合堂140人N=1.1)
(506, 48.00, 1.00, 1.10, 1.00, 1.00, 1.00, 1.10),
-- 王五: 数据结构 (56学时, 省级一流主持人Q2=1.5, 100人无合堂)
(507, 56.00, 1.00, 1.10, 1.00, 1.50, 1.00, 1.00),
-- 赵六: 计算机基础 (32学时, 选修K1=1.0)
(509, 32.00, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00);

-- -----------------------------------------------------------
-- 6. G2 实践课明细（biz_wl_practice）
-- -----------------------------------------------------------
INSERT INTO biz_wl_practice (item_id, J2, K, C2, Q1, Q2, Q3) VALUES
-- 张三: 数学实验 (32学时, 理工类K=1.0, C2=0.9)
(504, 32.00, 1.00, 0.90, 1.00, 1.00, 1.00);

-- -----------------------------------------------------------
-- 7. G3 实习实训明细（biz_wl_internship_training）
-- -----------------------------------------------------------
INSERT INTO biz_wl_internship_training (item_id, T, D, K, Q1, Q2, Q3) VALUES
-- 王五: 企业实训 (20天, 理工类D=4.0)
(508, 20.00, 4.00, 1.00, 1.00, 1.00, 1.00);

-- -----------------------------------------------------------
-- 7.1 G4 课程设计明细（biz_wl_course_design）
-- -----------------------------------------------------------
INSERT INTO biz_wl_course_design (item_id, J4, R4) VALUES
-- 张三: 数据结构课程设计 (2学分, 15人, 公式: J4 × min(R4,20) × 0.4 = 2×15×0.4 = 12.0)
(601, 2.00, 15);

-- -----------------------------------------------------------
-- 7.2 G5 毕业论文明细（biz_wl_thesis）
-- -----------------------------------------------------------
INSERT INTO biz_wl_thesis (item_id, R5, K5, education_level, major) VALUES
-- 李四: 指导10人文史类本科论文 (公式: R5 × K5 = 10 × 9 = 90.0)
(602, 10, 9.00, '本科', '文史类');

-- -----------------------------------------------------------
-- 7.3 G6 集中实习明细（biz_wl_concentrated_internship）
-- -----------------------------------------------------------
INSERT INTO biz_wl_concentrated_internship (item_id, W, R6) VALUES
-- 王五: 集中实习4周, 指导15人 (公式: W × min(R6,20) × 0.4 = 4×15×0.4 = 24.0)
(603, 4.00, 15);

-- -----------------------------------------------------------
-- 8. 触发计算引擎重算（通过直接更新汇总表模拟）
--    实际生产中应调用 /system/calc/recalcSummary 接口
-- -----------------------------------------------------------

-- 张三汇总: G1=176.0, G2=28.8, G4=12.0, G8=20 → G7=216.8, total=236.8, 教授额定128, excess=108.8
INSERT INTO biz_workload_summary (id, user_id, semester, academic_year, title, rated_workload, basic_teaching_standard, basic_teaching_met, total_workload, G7, G8, G9, G10, G11, excess_workload, pay_rate, performance_pay, is_capped, status) VALUES
(101, 3, '2025-2026-1', '2025-2026', '教授', 128.00, 128.00, 1, 236.80, 216.80, 20.00, 0.00, 236.80, 0.00, 108.80, 70.00, 7616.00, 0, 0)
ON DUPLICATE KEY UPDATE total_workload=VALUES(total_workload);

-- 李四汇总: G1=105.6, G5=90.0, G11=180 → G7=195.6, total=375.6, excess=135.6
INSERT INTO biz_workload_summary (id, user_id, semester, academic_year, title, rated_workload, basic_teaching_standard, basic_teaching_met, total_workload, G7, G8, G9, G10, G11, excess_workload, pay_rate, performance_pay, is_capped, status) VALUES
(102, 4, '2025-2026-1', '2025-2026', '副教授', 240.00, 240.00, 1, 375.60, 195.60, 0.00, 0.00, 195.60, 180.00, 135.60, 60.00, 8136.00, 0, 0)
ON DUPLICATE KEY UPDATE total_workload=VALUES(total_workload);

-- 王五汇总: G1=92.4, G3=160, G6=24.0 → G7=276.4, total=276.4, excess=36.4
INSERT INTO biz_workload_summary (id, user_id, semester, academic_year, title, rated_workload, basic_teaching_standard, basic_teaching_met, total_workload, G7, G8, G9, G10, G11, excess_workload, pay_rate, performance_pay, is_capped, status) VALUES
(103, 5, '2025-2026-1', '2025-2026', '讲师', 240.00, 240.00, 1, 276.40, 276.40, 0.00, 0.00, 276.40, 0.00, 36.40, 50.00, 1820.00, 0, 0)
ON DUPLICATE KEY UPDATE total_workload=VALUES(total_workload);

-- 赵六汇总: G1=32, total=32, 未达192额定 → 无超额
INSERT INTO biz_workload_summary (id, user_id, semester, academic_year, title, rated_workload, basic_teaching_standard, basic_teaching_met, total_workload, G7, G8, G9, G10, G11, excess_workload, pay_rate, performance_pay, is_capped, status) VALUES
(104, 6, '2025-2026-1', '2025-2026', '助教', 192.00, 192.00, 0, 32.00, 32.00, 0.00, 0.00, 32.00, 0.00, 0.00, 40.00, 0.00, 0, 0)
ON DUPLICATE KEY UPDATE total_workload=VALUES(total_workload);
