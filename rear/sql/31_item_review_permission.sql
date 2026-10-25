-- 独立的教务核对权限；不授教师角色，不依赖环境中的角色ID。
-- 按权限去重，由数据库分配menu_id；先执行既有业务菜单脚本。
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '教务核对明细',2010,20,'','F','0','0','system:workloadItem:review','#','admin',now()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms='system:workloadItem:review');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.perms='system:workloadItem:review'
WHERE r.role_key IN ('admin','biz_admin','jiaowu','assistant') AND r.status='0';
