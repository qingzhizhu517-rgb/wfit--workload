-- ----------------------------------------------------------------------------
-- 16_remove_unused_modules.sql
-- 移除随 workload-quartz / workload-generator 模块一并下线的 RuoYi 自带菜单
--
-- 背景：两个后端模块已从 rear/ 删除（pom `modules` 现为 5 个），
--       其 Controller 端点不再注册，对应前端页面也已删除。
--       若菜单仍存在，用户点击会 404，故一并清理。
--
-- 删除项：
--   menu_id 110 定时任务（系统监控下，monitor:job:*）+ 其 6 个按钮
--   menu_id 115 表单构建（系统工具下，tool:build:*）—— 属代码生成工具链
--   menu_id 116 代码生成（系统工具下，tool:gen:*）+ 其 6 个按钮
--
-- 保留项：
--   menu_id   2 系统监控（仍有 在线用户/数据监控/服务监控/缓存监控/缓存列表）
--   menu_id   3 系统工具（仍有 系统接口 swagger —— 本项目用 Swagger 做接口验证）
--   menu_id 117 系统接口
--
-- 幂等：可重复执行。先删 sys_role_menu 关联再删 sys_menu。
-- ----------------------------------------------------------------------------

-- 1. 删除角色-菜单授权关联（必须先于菜单删除；1130 起为 RuoYi 内置按钮权限 ID 段）
DELETE FROM sys_role_menu
 WHERE menu_id IN (
        SELECT menu_id FROM (
          SELECT menu_id FROM sys_menu
           WHERE menu_id IN (110, 115, 116)
              OR parent_id IN (110, 116)
              OR perms LIKE 'monitor:job:%'
              OR perms LIKE 'tool:gen:%'
              OR perms LIKE 'tool:build:%'
        ) t
 );

-- 2. 删除菜单本身
DELETE FROM sys_menu
 WHERE menu_id IN (110, 115, 116)
    OR parent_id IN (110, 116)
    OR perms LIKE 'monitor:job:%'
    OR perms LIKE 'tool:gen:%'
    OR perms LIKE 'tool:build:%';

-- 3. 校验（人工执行时启用）：应返回空集
-- SELECT menu_id, menu_name, perms FROM sys_menu
--  WHERE menu_id IN (110,115,116)
--     OR parent_id IN (110,116)
--     OR perms LIKE 'monitor:job:%' OR perms LIKE 'tool:gen:%' OR perms LIKE 'tool:build:%';
