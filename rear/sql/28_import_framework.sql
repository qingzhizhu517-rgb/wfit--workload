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
--      operator_name / confirm_token_hash / confirmed_at 六列（两阶段导入契约），
--      并补 template_category 一列（分类导入锁定 ALL/G1/G2/G3，默认 ALL）
--   2) 建 biz_import_row 逐行暂存表（预检落库 → 确认按 row_no 顺序回放）
--   3) biz_import_batch 加普通索引 idx_import_source(import_type, source_hash)
--      （去重在代码层判定，非唯一约束——见第 3 节说明）
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

-- template_category：分类导入锁定类别（ALL/G1/G2/G3），与 template_version 版式严格分开。
-- 默认 ALL，历史批次加列后即视为通用模板（放行 G1~G6），语义与代码 normalizeTemplateType 一致。
SET @wfit_import_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_import_batch'
     AND column_name = 'template_category') = 0,
  'ALTER TABLE biz_import_batch ADD COLUMN template_category VARCHAR(8) DEFAULT ''ALL'' COMMENT ''模板类别 ALL/G1/G2/G3（分类导入锁定，与版式 template_version 不同）''',
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

-- 3) biz_import_batch 源文件普通索引 idx_import_source(import_type, source_hash)
--    去重在代码层按「已导入(2) 才拒重传」判定（countConfirmedBySource），此处只建普通索引供查询。
--    绝不用唯一约束：否则阻断/放弃的历史预检会在预检插入期就锁死同一文件，修数据后无法重传。
--    先删可能已由旧版本脚本建的唯一索引 uk_import_source，再幂等建普通索引 idx_import_source。
SET @wfit_import_idx = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'biz_import_batch'
     AND index_name = 'uk_import_source') > 0,
  'DROP INDEX uk_import_source ON biz_import_batch',
  'SELECT 1');
PREPARE wfit_import_idx_stmt FROM @wfit_import_idx;
EXECUTE wfit_import_idx_stmt;
DEALLOCATE PREPARE wfit_import_idx_stmt;

SET @wfit_import_idx = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'biz_import_batch'
     AND index_name = 'idx_import_source') = 0,
  'CREATE INDEX idx_import_source ON biz_import_batch(import_type, source_hash)',
  'SELECT 1');
PREPARE wfit_import_idx_stmt FROM @wfit_import_idx;
EXECUTE wfit_import_idx_stmt;
DEALLOCATE PREPARE wfit_import_idx_stmt;

-- ============================================================
-- 4) 教学任务 v2 源数量快照列（需求 2：拆分计划学时/天数/周数/学分）
--    theory_hours/practice_hours 落库口径不变；这里按类别原样保留导入文件填的
--    数量列，供事后审计「导入时到底填了什么」。quantity_type 记录数量语义。
--    幂等：逐列查 information_schema.columns，存在则跳过。
-- ============================================================
SET @wfit_tt_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_teaching_task'
     AND column_name = 'input_planned_hours') = 0,
  'ALTER TABLE biz_teaching_task ADD COLUMN input_planned_hours DECIMAL(10,2) DEFAULT NULL COMMENT ''源数量快照:计划学时(G1/G2)''',
  'SELECT 1');
PREPARE wfit_tt_stmt FROM @wfit_tt_ddl; EXECUTE wfit_tt_stmt; DEALLOCATE PREPARE wfit_tt_stmt;

SET @wfit_tt_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_teaching_task'
     AND column_name = 'input_days') = 0,
  'ALTER TABLE biz_teaching_task ADD COLUMN input_days DECIMAL(10,2) DEFAULT NULL COMMENT ''源数量快照:天数(G3)''',
  'SELECT 1');
PREPARE wfit_tt_stmt FROM @wfit_tt_ddl; EXECUTE wfit_tt_stmt; DEALLOCATE PREPARE wfit_tt_stmt;

SET @wfit_tt_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_teaching_task'
     AND column_name = 'input_weeks') = 0,
  'ALTER TABLE biz_teaching_task ADD COLUMN input_weeks DECIMAL(10,2) DEFAULT NULL COMMENT ''源数量快照:周数(G6)''',
  'SELECT 1');
PREPARE wfit_tt_stmt FROM @wfit_tt_ddl; EXECUTE wfit_tt_stmt; DEALLOCATE PREPARE wfit_tt_stmt;

SET @wfit_tt_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_teaching_task'
     AND column_name = 'input_credits') = 0,
  'ALTER TABLE biz_teaching_task ADD COLUMN input_credits DECIMAL(10,2) DEFAULT NULL COMMENT ''源数量快照:课程设计学分(G4)''',
  'SELECT 1');
PREPARE wfit_tt_stmt FROM @wfit_tt_ddl; EXECUTE wfit_tt_stmt; DEALLOCATE PREPARE wfit_tt_stmt;

SET @wfit_tt_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_teaching_task'
     AND column_name = 'quantity_type') = 0,
  'ALTER TABLE biz_teaching_task ADD COLUMN quantity_type VARCHAR(16) DEFAULT NULL COMMENT ''源数量类型:PLANNED_HOURS/DAYS/WEEKS/CREDITS/STUDENT_COUNT''',
  'SELECT 1');
PREPARE wfit_tt_stmt FROM @wfit_tt_ddl; EXECUTE wfit_tt_stmt; DEALLOCATE PREPARE wfit_tt_stmt;
