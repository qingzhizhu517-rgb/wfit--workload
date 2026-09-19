-- ============================================================
-- 26_coefficient_teacher_perm.sql —— 授教师(role4)系数调整申请「提交/查看」权限
--
-- 背景：25_calculation_menu.sql 已登记 system:coefficientAdjustment:add/list/approve/reject
--   四个按钮权限（menu_id 20306/20307/20308/20309），但仅授 role1/2/3（管理员/业务管理员/
--   教务助理）。教师自助提交调整申请、查看「我的申请」是本功能的核心，故补授 role4：
--     - system:coefficientAdjustment:add  (menu_id=20306) 教师提交申请
--     - system:coefficientAdjustment:list (menu_id=20307) 教师查看本人申请（/myList 已按 DataScopeUtil 收口到本人）
--   approve/reject 属教务审核动作，教师不授。
--
-- 幂等：先按 (role_id=4, 目标 menu_id) 精确删除，再 INSERT IGNORE；可重复执行。
--   仅动这两条授权，不新增/修改菜单本身，不影响其它角色既有授权。
-- ============================================================

DELETE FROM sys_role_menu
 WHERE role_id = 4
   AND menu_id IN (
     SELECT menu_id FROM sys_menu
      WHERE perms IN ('system:coefficientAdjustment:add', 'system:coefficientAdjustment:list')
   );

INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 4, menu_id FROM sys_menu
 WHERE perms IN ('system:coefficientAdjustment:add', 'system:coefficientAdjustment:list');
