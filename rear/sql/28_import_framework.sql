-- ============================================================
-- 28_import_framework.sql —— 统一导入批次契约 + 逐行暂存表
--
-- 计划编号修正：原计划写 22_workload_requirements.sql，但 22-27 已全部占用
-- （22_semester_position_workload / 23_calculation_consistency /
--  24_coefficient_adjustment / 25_calculation_menu /
--  26_coefficient_teacher_perm / 27_calc_flow_menu），本框架顺延为 28。
--
-- 内容：
--   1) biz_import_batch 增加 template_version / source_hash / operator_id /
--      operator_name / confirm_token_hash / confirmed_at 六列（两阶段导入契约）
--   2) 建 biz_import_row 逐行暂存表（预检落库 → 确认按 row_no 顺序回放）
--   3) biz_import_batch 加唯一索引 uk_import_source(import_type, source_hash, status)
--
-- 幂等：可重复执行。
--   - 加列前查 information_schema.columns，存在则跳过（SELECT 1）
--   - 建表用 CREATE TABLE IF NOT EXISTS
--   - 加索引前查 information_schema.statistics，存在则跳过（SELECT 1）
-- 不含任何无条件 DROP / DELETE / TRUNCATE。
--
-- ⚠️ 关于 uk_import_source 对既有导入的兼容性：
--   现有流式导入 importTeachingTasksStreaming 会插 biz_import_batch，且 source_hash
--   为 NULL。InnoDB 唯一索引对 NULL 值不做唯一约束（多行 NULL 允许并存，符合 SQL 标准），
--   因此该唯一索引不会破坏现有导入——只有当同一 (import_type, source_hash, status) 且
--   source_hash 非 NULL 的组合重复时才会阻断，正是两阶段导入去重所需的语义。
-- ============================================================

-- 1) biz_import_batch 增加两阶段导入契约列（逐列幂等加列）
SET @wfit_import_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_import_batch'
     AND column_name = 'template_version') = 0,
  'ALTER TABLE biz_import_batch ADD COLUMN template_version VARCHAR(20) DEFAULT NULL COMMENT ''模板版本 V1/V2/ROSTER_V1/ROLE_V1''',
  'SELECT 1');
PREPARE wfit_import_stmt FROM @wfit_import_ddl;
EXECUTE wfit_import_stmt;
DEALLOCATE PREPARE wfit_import_stmt;

SET @wfit_import_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_import_batch'
     AND column_name = 'source_hash') = 0,
  'ALTER TABLE biz_import_batch ADD COLUMN source_hash CHAR(64) DEFAULT NULL COMMENT ''源文件SHA-256''',
  'SELECT 1');
PREPARE wfit_import_stmt FROM @wfit_import_ddl;
EXECUTE wfit_import_stmt;
DEALLOCATE PREPARE wfit_import_stmt;

SET @wfit_import_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_import_batch'
     AND column_name = 'operator_id') = 0,
  'ALTER TABLE biz_import_batch ADD COLUMN operator_id BIGINT(20) DEFAULT NULL COMMENT ''操作者用户ID''',
  'SELECT 1');
PREPARE wfit_import_stmt FROM @wfit_import_ddl;
EXECUTE wfit_import_stmt;
DEALLOCATE PREPARE wfit_import_stmt;

SET @wfit_import_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_import_batch'
     AND column_name = 'operator_name') = 0,
  'ALTER TABLE biz_import_batch ADD COLUMN operator_name VARCHAR(64) DEFAULT NULL COMMENT ''操作者用户名''',
  'SELECT 1');
PREPARE wfit_import_stmt FROM @wfit_import_ddl;
EXECUTE wfit_import_stmt;
DEALLOCATE PREPARE wfit_import_stmt;

SET @wfit_import_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_import_batch'
     AND column_name = 'confirm_token_hash') = 0,
  'ALTER TABLE biz_import_batch ADD COLUMN confirm_token_hash CHAR(64) DEFAULT NULL COMMENT ''一次性确认令牌哈希(SHA-256)''',
  'SELECT 1');
PREPARE wfit_import_stmt FROM @wfit_import_ddl;
EXECUTE wfit_import_stmt;
DEALLOCATE PREPARE wfit_import_stmt;

SET @wfit_import_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_import_batch'
     AND column_name = 'confirmed_at') = 0,
  'ALTER TABLE biz_import_batch ADD COLUMN confirmed_at DATETIME DEFAULT NULL COMMENT ''确认时间''',
  'SELECT 1');
PREPARE wfit_import_stmt FROM @wfit_import_ddl;
EXECUTE wfit_import_stmt;
DEALLOCATE PREPARE wfit_import_stmt;

-- 2) biz_import_row 逐行暂存表
CREATE TABLE IF NOT EXISTS biz_import_row (
  id                BIGINT(20)    NOT NULL AUTO_INCREMENT,
  batch_id          BIGINT(20)    NOT NULL                 COMMENT 'FK biz_import_batch.id',
  row_no            INT(11)       NOT NULL                 COMMENT '源文件物理行号',
  raw_json          JSON          DEFAULT NULL             COMMENT '原始行JSON(DTO白名单序列化,无敏感列)',
  normalized_json   JSON          DEFAULT NULL             COMMENT '规范化后行JSON',
  status            VARCHAR(16)   NOT NULL                 COMMENT 'READY/BLOCKED/IMPORTED/SKIPPED/FAILED',
  error_code        VARCHAR(50)   DEFAULT NULL             COMMENT '错误码',
  error_message     VARCHAR(500)  DEFAULT NULL             COMMENT '错误信息',
  target_id         BIGINT(20)    DEFAULT NULL             COMMENT '写入后的业务目标记录ID',
  create_by         VARCHAR(64)   DEFAULT '',
  create_time       DATETIME      DEFAULT NULL,
  update_by         VARCHAR(64)   DEFAULT '',
  update_time       DATETIME      DEFAULT NULL,
  remark            VARCHAR(500)  DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_batch_row (batch_id, row_no),
  KEY idx_batch_status (batch_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导入逐行暂存';

-- 3) biz_import_batch 源文件去重唯一索引（幂等加索引：先查 statistics）
--    见文件头说明：source_hash 为 NULL 的既有流式导入不受约束（InnoDB 允许多 NULL 行）。
SET @wfit_import_idx = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'biz_import_batch'
     AND index_name = 'uk_import_source') = 0,
  'CREATE UNIQUE INDEX uk_import_source ON biz_import_batch(import_type, source_hash, status)',
  'SELECT 1');
PREPARE wfit_import_idx_stmt FROM @wfit_import_idx;
EXECUTE wfit_import_idx_stmt;
DEALLOCATE PREPARE wfit_import_idx_stmt;
