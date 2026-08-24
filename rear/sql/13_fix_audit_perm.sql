-- ============================================================================
-- 13_fix_audit_perm.sql  审批权限越权修复（幂等，可重复执行）
--
-- 背景：审计发现 08_review_fixes.sql 的回归补丁 3.1 将 20209(system:audit:unlock)
--       授给了教务助理(role_id=3)。unlock 是「已完结(3) → 填报中(0)」的终态解锁，
--       教务助理据此可把院领导已签字锁定的记录单方面复活，绕过状态机锁定约束。
--       同时院领导(role_id=5)待签环节缺少驳回权限（对应后端 reject 已放开 from∈{1,2}）。
--
-- 菜单 ID 约定（见 05_biz_menu.sql）：
--   20206 approve 审核通过 / 20207 reject 驳回 / 20208 sign 签字 / 20209 unlock 解锁
-- ============================================================================

-- 1. 撤销教务助理(role_id=3)的 unlock 越权授权（解锁仅限管理员）
DELETE FROM sys_role_menu WHERE role_id = 3 AND menu_id = 20209;

-- 2. unlock 仅授管理员：admin(role_id=1) 与业务管理员(role_id=2)
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES (1, 20209), (2, 20209);

-- 3. 院领导(role_id=5)增授 reject（待签环节可驳回，与后端 reject from∈{1,2} 对齐）
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES (5, 20207);
