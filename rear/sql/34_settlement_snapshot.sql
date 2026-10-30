-- Structure only: no historical amounts, workflow state or evidence is rewritten.
CREATE TABLE IF NOT EXISTS biz_workload_settlement_snapshot (
 id BIGINT NOT NULL AUTO_INCREMENT,
 summary_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 semester VARCHAR(20) NOT NULL,
 settlement_version BIGINT NOT NULL,
 format_version INT NOT NULL,
 payload_json LONGTEXT NOT NULL,
 hash CHAR(64) NOT NULL,
 created_by VARCHAR(64) NOT NULL,
 created_at DATETIME NOT NULL,
 PRIMARY KEY(id), UNIQUE KEY uk_summary_settlement_version(summary_id,settlement_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='只追加学期结算证据';
SET @settlement_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_workload_summary' AND column_name='settlement_snapshot_id')=0,
 'ALTER TABLE biz_workload_summary ADD COLUMN settlement_snapshot_id BIGINT NULL COMMENT ''服务器结算证据指针''','SELECT 1');
PREPARE settlement_stmt FROM @settlement_ddl;
EXECUTE settlement_stmt;
DEALLOCATE PREPARE settlement_stmt;
SET @settlement_ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_pay_record' AND column_name='settlement_snapshot_id')=0,
 'ALTER TABLE biz_pay_record ADD COLUMN settlement_snapshot_id BIGINT NULL COMMENT ''服务器结算证据指针''','SELECT 1');
PREPARE settlement_stmt FROM @settlement_ddl;
EXECUTE settlement_stmt;
DEALLOCATE PREPARE settlement_stmt;
