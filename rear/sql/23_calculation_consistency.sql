-- ============================================================
-- 23_calculation_consistency.sql —— 不可变工作量计算快照 + 明细版本列
--
-- 计划编号修正：原计划写 22_calculation_consistency.sql，与既有
-- 22_semester_position_workload.sql 冲突，本任务顺延为 23。
--
-- 内容：
--   1) 新建 biz_workload_calc_snapshot（append-only 计算快照）
--   2) biz_workload_item 增加 calculation_version / last_calculated_at
--   3) 将 G11 且 assignment_id 非空且 source_type='IMPORT' 的行归一为 'AUTO'
--
-- 幂等：可重复执行。
--   - 建表用 CREATE TABLE IF NOT EXISTS
--   - 加列前查 information_schema，存在则跳过（无条件 SELECT 1）
--   - 数据 UPDATE 天然幂等（二次执行无匹配行）
-- 不含任何无条件 DROP / DELETE / TRUNCATE。
-- ============================================================

-- 1) 计算快照表（append-only：只 INSERT，不 UPDATE/DELETE）
CREATE TABLE IF NOT EXISTS biz_workload_calc_snapshot (
  id                   BIGINT(20)     NOT NULL AUTO_INCREMENT,
  item_id              BIGINT(20)     NOT NULL                 COMMENT 'FK biz_workload_item.id',
  calculation_version  BIGINT(20)     NOT NULL                 COMMENT '明细计算版本(同一itemId单调递增)',
  formula_expression   VARCHAR(255)   DEFAULT NULL             COMMENT '规范化公式表达式(全角×)',
  factor_json          JSON           DEFAULT NULL             COMMENT '因子取值(固定key顺序,数值为字符串)',
  source_json          JSON           DEFAULT NULL             COMMENT '因子来源(与factor_json同key顺序)',
  rule_version         VARCHAR(64)    DEFAULT NULL             COMMENT '规则版本标识',
  result               DECIMAL(10,2)  DEFAULT NULL             COMMENT '核算结果工作量',
  snapshot_hash        CHAR(64)       NOT NULL                 COMMENT '内容哈希(SHA-256)',
  created_by           VARCHAR(64)    DEFAULT ''               COMMENT '创建人',
  created_at           DATETIME       DEFAULT NULL             COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_item_version (item_id, calculation_version),
  KEY idx_snapshot_hash (snapshot_hash),
  KEY idx_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变工作量计算快照';

-- 2) biz_workload_item 增加计算版本与最近核算时间（幂等加列）
SET @wfit_consistency_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_workload_item'
     AND column_name = 'calculation_version') = 0,
  'ALTER TABLE biz_workload_item ADD COLUMN calculation_version BIGINT(20) NOT NULL DEFAULT 0 COMMENT ''明细计算版本''',
  'SELECT 1');
PREPARE wfit_consistency_stmt FROM @wfit_consistency_ddl;
EXECUTE wfit_consistency_stmt;
DEALLOCATE PREPARE wfit_consistency_stmt;

SET @wfit_consistency_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_workload_item'
     AND column_name = 'last_calculated_at') = 0,
  'ALTER TABLE biz_workload_item ADD COLUMN last_calculated_at DATETIME DEFAULT NULL COMMENT ''最近核算时间''',
  'SELECT 1');
PREPARE wfit_consistency_stmt FROM @wfit_consistency_ddl;
EXECUTE wfit_consistency_stmt;
DEALLOCATE PREPARE wfit_consistency_stmt;

-- 3) G11 岗位来源统一来源类型：IMPORT -> AUTO（幂等，二次执行无匹配行）
UPDATE biz_workload_item
   SET source_type = 'AUTO'
 WHERE item_type = 'G11'
   AND assignment_id IS NOT NULL
   AND source_type = 'IMPORT';
