-- T03：稳定教师学期互斥锁 + 汇总新鲜度。部署新应用前执行，可重复执行。
-- 不改写已待审/已完结历史；首次加列时既有草稿需重新核算，不能猜测旧汇总是否新鲜。
CREATE TABLE IF NOT EXISTS biz_workload_period_lock (
    user_id BIGINT NOT NULL,
    semester VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师学期写入互斥锁，保留稳定锁键';
SET @wfit_freshness_missing = (SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='biz_workload_summary' AND column_name='needs_recalculation') = 0;
SET @wfit_consistency_ddl = IF(@wfit_freshness_missing,
 'ALTER TABLE biz_workload_summary ADD COLUMN needs_recalculation TINYINT NOT NULL DEFAULT 1 COMMENT ''源数据已变更，需重新核算''', 'SELECT 1');
PREPARE wfit_consistency_stmt FROM @wfit_consistency_ddl;
EXECUTE wfit_consistency_stmt;
DEALLOCATE PREPARE wfit_consistency_stmt;
-- 默认1使DDL之后进程中断也保持旧草稿不可提交；已进入审批的历史记录保留原流程。
UPDATE biz_workload_summary SET needs_recalculation=0 WHERE status IN (1,2);
