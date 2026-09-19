-- ============================================================
-- 25_calculation_menu.sql —— G1/G2/G3 分类导入入口菜单 + 系数调整申请权限登记
--
-- 计划编号修正：计划正文写 24_calculation_menu.sql，但 24 已被
-- 24_coefficient_adjustment.sql 占用，本任务顺延为 25。
--
-- 内容：
--   1) 新增三个 C 型菜单（G1/G2/G3 分类导入快捷入口），路由到 teachingTask 组件
--      并携带 query templateType；菜单可见性复用 system:teachingTask:import 权限串。
--   2) 补登记 Task9 遗留的 system:coefficientAdjustment:add/list/approve/reject 四个
--      按钮权限（BizCoefficientAdjustmentController 已声明注解，唯 sys_menu 未登记），
--      挂在「工作量明细总表」(menu_id=2010) 下，授予 role1/2/3（管理员/业务管理员/教务助理）。
--
-- menu_id 规划（均为既有 05/15 脚本未占用区段）：
--   分类导入 C 菜单：2005(G1) / 2006(G2) / 2007(G3)
--   系数调整按钮（父 2010）：20306(add) / 20307(list) / 20308(approve) / 20309(reject)
--
-- 幂等：先按本次新增的精确 path / perms 清理，再 INSERT；可重复执行。
--   注意：分类导入 C 菜单复用 system:teachingTask:import 权限串，故只能按 path 清理，
--   绝不按该 perms 删除（否则会误删既有导入按钮 20026）。
--   不含任何无条件 DROP / TRUNCATE，也不动既有菜单。
-- ============================================================

-- ---------- 幂等清理：仅删除本脚本新增的菜单/权限 ----------
-- 先清 sys_role_menu（引用 menu_id），再清 sys_menu
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM sys_menu
  WHERE (menu_type = 'C' AND path IN ('teachingTaskG1', 'teachingTaskG2', 'teachingTaskG3'))
     OR perms IN ('system:coefficientAdjustment:add', 'system:coefficientAdjustment:list',
                  'system:coefficientAdjustment:approve', 'system:coefficientAdjustment:reject')
);
DELETE FROM sys_menu
  WHERE (menu_type = 'C' AND path IN ('teachingTaskG1', 'teachingTaskG2', 'teachingTaskG3'))
     OR perms IN ('system:coefficientAdjustment:add', 'system:coefficientAdjustment:list',
                  'system:coefficientAdjustment:approve', 'system:coefficientAdjustment:reject');

-- ---------- 1) G1/G2/G3 分类导入快捷入口（C 型，父 2000 工作量管理）----------
-- 路由到 teachingTask 组件，query 预置 templateType；前端据此打开对应分类的导入弹窗。
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2005, 'G1理论课导入', 2000, 20, 'teachingTaskG1', 'system/teachingTask/index', '{"templateType":"G1"}', '', 1, 0, 'C', '0', '0', 'system:teachingTask:import', 'documentation', 'admin', sysdate(), '', NULL, 'G1 理论课分类导入入口');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2006, 'G2实践课导入', 2000, 21, 'teachingTaskG2', 'system/teachingTask/index', '{"templateType":"G2"}', '', 1, 0, 'C', '0', '0', 'system:teachingTask:import', 'documentation', 'admin', sysdate(), '', NULL, 'G2 实践课分类导入入口');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2007, 'G3实习实训导入', 2000, 22, 'teachingTaskG3', 'system/teachingTask/index', '{"templateType":"G3"}', '', 1, 0, 'C', '0', '0', 'system:teachingTask:import', 'documentation', 'admin', sysdate(), '', NULL, 'G3 实习实训分类导入入口');

-- ---------- 2) 系数调整申请按钮权限（F 型，父 2010 工作量明细总表）----------
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20306, '系数调整申请', 2010, 8, '', '', '', '', 1, 0, 'F', '0', '0', 'system:coefficientAdjustment:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20307, '系数调整查询', 2010, 9, '', '', '', '', 1, 0, 'F', '0', '0', 'system:coefficientAdjustment:list', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20308, '系数调整审批通过', 2010, 10, '', '', '', '', 1, 0, 'F', '0', '0', 'system:coefficientAdjustment:approve', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20309, '系数调整审批驳回', 2010, 11, '', '', '', '', 1, 0, 'F', '0', '0', 'system:coefficientAdjustment:reject', '#', 'admin', sysdate(), '', NULL, '');

-- ---------- 3) 角色授权：role1(管理员)/role2(业务管理员)/role3(教务助理) 全量 ----------
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2005, 2006, 2007, 20306, 20307, 20308, 20309);
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 2, menu_id FROM sys_menu WHERE menu_id IN (2005, 2006, 2007, 20306, 20307, 20308, 20309);
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 3, menu_id FROM sys_menu WHERE menu_id IN (2005, 2006, 2007, 20306, 20307, 20308, 20309);
