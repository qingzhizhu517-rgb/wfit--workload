-- G11 本学期岗位减免来源字段；幂等结构升级，新库已并入 01_biz_schema.sql。
-- 仅升级结构，不换算、不覆盖已有工作量，不重算已审核数据。
-- allowance_rate 从本版本起表示教务确认的本学期值。旧学年值须先核实；
-- 当前开发环境按既定计划 fresh DB 重建，不能把本脚本当作金额转换脚本。

SET @wfit_position_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_role_assignment'
     AND column_name = 'source_batch_id') = 0,
  'ALTER TABLE biz_role_assignment ADD COLUMN source_batch_id VARCHAR(64) DEFAULT NULL COMMENT ''来源批次''',
  'SELECT 1');
PREPARE wfit_position_stmt FROM @wfit_position_ddl;
EXECUTE wfit_position_stmt;
DEALLOCATE PREPARE wfit_position_stmt;

SET @wfit_position_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_wl_management'
     AND column_name = 'source_batch_id') = 0,
  'ALTER TABLE biz_wl_management ADD COLUMN source_batch_id VARCHAR(64) DEFAULT NULL COMMENT ''来源批次''',
  'SELECT 1');
PREPARE wfit_position_stmt FROM @wfit_position_ddl;
EXECUTE wfit_position_stmt;
DEALLOCATE PREPARE wfit_position_stmt;

-- 无汇总行时，行锁无法独占尚不存在的 G11；唯一键保证并发同步不产生重复记录。
-- 若升级库已有重复记录，此操作会报错并停止，必须人工核对，禁止自动删改历史记录。
SET @wfit_position_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'biz_workload_item'
     AND index_name = 'uk_assignment_sem') = 0,
  'ALTER TABLE biz_workload_item ADD UNIQUE KEY uk_assignment_sem (assignment_id, semester)',
  'SELECT 1');
PREPARE wfit_position_stmt FROM @wfit_position_ddl;
EXECUTE wfit_position_stmt;
DEALLOCATE PREPARE wfit_position_stmt;

ALTER TABLE biz_role_assignment
  MODIFY COLUMN allowance_rate DECIMAL(10,2) NOT NULL COMMENT '岗位减免工作量（本学期）';
ALTER TABLE biz_wl_management
  MODIFY COLUMN prorated_amount DECIMAL(10,2) NOT NULL COMMENT '岗位减免工作量（本学期）',
  MODIFY COLUMN proration_basis VARCHAR(200) DEFAULT NULL COMMENT '计入G11说明';
