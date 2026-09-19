-- ============================================================
-- 27_calc_flow_menu.sql —— 系数调整申请列表页侧边栏入口菜单登记
--
-- 背景：Task 11 建立了 front/RuoYi-Vue3/src/views/system/coefficientAdjustment/index.vue，
--   Task 9 登记了 system:coefficientAdjustment:add/list/approve/reject 四个「按钮」权限
--   （25_calculation_menu.sql，F 型，挂在 2010 下），但始终没有一个「菜单」(C 型) 把该页面
--   挂进侧边栏 —— 教师/教务打不开这个列表页。本脚本补一个 C 型菜单入口。
--
-- 内容：
--   新增 1 个 C 型菜单「系数调整申请」，component 指向 system/coefficientAdjustment/index，
--   perms 复用 system:coefficientAdjustment:list（不新增权限串），授 role1/2/3/4
--   （管理员/业务管理员/教务助理看全部审批，教师看「我的」——后端 /myList 已按 DataScopeUtil 收口本人）。
--
-- menu_id 规划：2008（05/15/25 脚本未占用；2005-2007 已被分类导入占用，2010 为明细总表）。
--
-- 幂等：先按本次新增的精确 path 清理（不按 perms 删，避免误删 25 脚本的 F 型 list 按钮 20307），
--   再 INSERT；可重复执行。不含任何无条件 DROP / TRUNCATE，也不动既有菜单。
-- ============================================================

-- ---------- 幂等清理：仅删除本脚本新增的菜单 ----------
DELETE FROM sys_role_menu WHERE menu_id IN (
  SELECT menu_id FROM sys_menu
  WHERE menu_type = 'C' AND path = 'coefficientAdjustment'
);
DELETE FROM sys_menu
  WHERE menu_type = 'C' AND path = 'coefficientAdjustment';

-- ---------- 系数调整申请列表页入口（C 型，父 2000 工作量管理）----------
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2008, '系数调整申请', 2000, 23, 'coefficientAdjustment', 'system/coefficientAdjustment/index', '', '', 1, 0, 'C', '0', '0', 'system:coefficientAdjustment:list', 'edit', 'admin', sysdate(), '', NULL, 'G1/G2 系数调整申请与审核列表页');

-- ---------- 角色授权：role1/2/3（全部审批视图）+ role4（教师看「我的」）----------
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id = 2008;
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 2, menu_id FROM sys_menu WHERE menu_id = 2008;
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 3, menu_id FROM sys_menu WHERE menu_id = 2008;
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 4, menu_id FROM sys_menu WHERE menu_id = 2008;
