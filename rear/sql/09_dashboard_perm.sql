-- ============================================================
-- 09_dashboard_perm.sql — 仪表盘权限回归修复（审查项 P2-05）
-- 执行: mysql -u root -p wflg_workload < 09_dashboard_perm.sql
--
-- 执行顺序: 01 → 02 → 03 → 04 → 05 → 06 → 08 → 09
--   （本脚本依赖 05 的业务菜单、06 的角色/账号，须最后执行）
--
-- 背景：BizDashboardController 三个端点已加 @PreAuthorize，但对应权限串
--   未在 sys_menu 登记，除 admin(userId=1 在 PermissionService 豁免)外，
--   教务/院领导访问仪表盘 403。本脚本补登记权限并授权相应角色。
--
-- 注解权限串（与 BizDashboardController.java 逐字核对）：
--   GET /system/dashboard/adminStats   → system:dashboard:adminStats
--   GET /system/dashboard/teacherStats → system:workloadSummary:query
--                                        （复用既有按钮 20201，无需新增）
--   GET /system/dashboard/collegeStats → system:dashboard:collegeStats
--
-- 本脚本幂等，可重复执行（全部 INSERT IGNORE）。
-- ============================================================

-- ==================== 1. sys_menu 登记仪表盘按钮权限 ====================
-- menu_id 选择：20216/20217（05_biz_menu.sql 已核对：20201~20215 已占用，
--   20216/20217 未冲突），挂在学期汇总菜单 2020 下，编号风格同 20215。

-- 1.1 管理员仪表盘统计（system:dashboard:adminStats）
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20216, '管理员仪表盘统计', 2020, 13, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dashboard:adminStats', '#', 'admin', sysdate(), '', NULL, '仪表盘-管理员全局统计');

-- 1.2 各学院概况统计（system:dashboard:collegeStats）
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20217, '学院概况统计', 2020, 14, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dashboard:collegeStats', '#', 'admin', sysdate(), '', NULL, '仪表盘-各学院教学任务概况');

-- ==================== 2. sys_role_menu 角色授权 ====================
-- 角色 id 以 06_test_accounts.sql 为准：
--   role_id=1 管理员 / role_id=3 教务助理 / role_id=4 教师 / role_id=5 院领导
--
-- 授权矩阵：
--   20216 adminStats   → role1(admin) + role3(教务)
--   20217 collegeStats → role1(admin) + role3(教务) + role5(院领导)
--   teacherStats 复用 20201(system:workloadSummary:query)
--                      → role4(教师) 兜底补授，保证教师可访问本人仪表盘

-- 2.1 adminStats：管理员 + 教务
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES (1, 20216), (3, 20216);

-- 2.2 collegeStats：管理员 + 教务 + 院领导
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES (1, 20217), (3, 20217), (5, 20217);

-- 2.3 teacherStats：教师查看本人仪表盘（权限串 system:workloadSummary:query，
--     对应既有按钮 20201；06_test_accounts.sql 已授 role4，此处兜底防脱漏）
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES (4, 20201);
