-- ============================================================
-- 10_item_role_type.sql — biz_workload_item 增加 role_type 列（P3-05）
-- 执行: mysql -u root -p wflg_workload < 10_item_role_type.sql
--
-- 执行顺序：在 01_biz_schema.sql ~ 09_dashboard_perm.sql 之后执行；
-- 01_biz_schema.sql 建表语句已同步包含本列（全新建库无需本脚本，
-- 本脚本仅用于既有库的增量升级）。
--
-- 本脚本幂等，可重复执行：通过 information_schema.columns 判断
-- role_type 列不存在时才执行 ALTER TABLE ADD COLUMN。
--
-- 口径说明：
--   枚举与 biz_role_assignment.role_type / biz_wl_management.role_type
--   保持一致：班主任/系主任/教研室主任/专业负责人/俱乐部经理/
--   实验人员/督导/中层副职/心理中心（与前端 bizDict.roleTypeOptions
--   同一枚举源）。列宽 VARCHAR(20) 覆盖全部枚举值（最长 5 字符）。
-- ============================================================

SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_workload_item'
    AND column_name = 'role_type'
);

SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE biz_workload_item ADD COLUMN role_type VARCHAR(20) DEFAULT NULL COMMENT ''岗位类型(G11): 班主任/系主任/教研室主任/专业负责人/俱乐部经理/实验人员/督导/中层副职/心理中心'' AFTER assignment_id',
  'SELECT ''biz_workload_item.role_type already exists, skip'' AS msg');

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
