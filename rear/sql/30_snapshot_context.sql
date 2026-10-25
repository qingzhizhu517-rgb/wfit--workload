-- 历史解释上下文：先执行23，再执行本补丁；部署新应用前应用。
-- 仅新增可空列；旧快照保持NULL，不用当前源数据伪造历史。
-- LONGTEXT保留序列化字节用于内容哈希，避免MySQL JSON重排对象键。
SET @wfit_context_ddl = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'biz_workload_calc_snapshot'
     AND column_name = 'context_json') = 0,
  'ALTER TABLE biz_workload_calc_snapshot ADD COLUMN context_json LONGTEXT NULL COMMENT ''版本化历史解释上下文''',
  'SELECT 1');
PREPARE wfit_context_stmt FROM @wfit_context_ddl;
EXECUTE wfit_context_stmt;
DEALLOCATE PREPARE wfit_context_stmt;
