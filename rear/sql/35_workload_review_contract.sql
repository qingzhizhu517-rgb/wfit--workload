-- 35 additive T06 contract. Prerequisite: through34 and biz_audit_log.
-- No historical versions, audit dates, amounts or evidence are backfilled.
-- T06 contract only. T07 owns immutable evidence and append-only enforcement.
CREATE TABLE IF NOT EXISTS biz_workload_review_batch (
 id BIGINT NOT NULL AUTO_INCREMENT,
 summary_id BIGINT NOT NULL,
 review_batch_no VARCHAR(50) NOT NULL,
 summary_data_version BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 semester VARCHAR(20) NOT NULL,
 settlement_snapshot_id BIGINT NULL,
 settlement_snapshot_hash CHAR(64) NULL,
 format_version INT NOT NULL,
 snapshot_json JSON NOT NULL,
 data_hash CHAR(64) NOT NULL,
 created_by VARCHAR(64) NOT NULL,
 created_at DATETIME NOT NULL,
 PRIMARY KEY(id),
 UNIQUE KEY uk_summary_review_batch(summary_id,review_batch_no),
 KEY idx_review_batch_teacher(user_id,semester,id),
 CONSTRAINT ck_review_batch_version CHECK(summary_data_version>0 AND format_version>0),
 CONSTRAINT ck_review_batch_settlement CHECK(
 (settlement_snapshot_id IS NULL AND settlement_snapshot_hash IS NULL) OR
 (settlement_snapshot_id IS NOT NULL AND settlement_snapshot_hash IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='只追加汇总审核批次证据';

CREATE TABLE IF NOT EXISTS biz_workload_item_review (
 id BIGINT NOT NULL AUTO_INCREMENT,
 summary_id BIGINT NOT NULL,
 item_id BIGINT NOT NULL COMMENT '仅指向biz_workload_item',
 review_batch_no VARCHAR(50) NOT NULL,
 review_type VARCHAR(32) NOT NULL,
 format_version INT NOT NULL,
 snapshot_json JSON NOT NULL,
 data_hash CHAR(64) NOT NULL,
 result VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 reason VARCHAR(500) NULL,
 reviewer_id BIGINT NULL,
 reviewer_name VARCHAR(64) NULL,
 reviewed_at DATETIME NULL,
 version BIGINT NOT NULL DEFAULT 0,
 create_by VARCHAR(64) NOT NULL,
 create_time DATETIME NOT NULL,
 update_by VARCHAR(64) NULL,
 update_time DATETIME NULL,
 PRIMARY KEY(id),
 UNIQUE KEY uk_summary_batch_item(summary_id,review_batch_no,item_id),
 KEY idx_review_summary_result(summary_id,review_batch_no,result,id),
 KEY idx_review_center(review_type,result,create_time,id),
 CONSTRAINT ck_item_review_type CHECK(review_type IN ('TEACHING_TASK','MANUAL_DECLARATION','G11_ROLE','OVER_LIMIT')),
 CONSTRAINT ck_item_review_result CHECK(result IN ('PENDING','PASS','REJECT','WAIVE')),
 CONSTRAINT ck_item_review_reason CHECK(result NOT IN ('REJECT','WAIVE') OR (reason IS NOT NULL AND CHAR_LENGTH(TRIM(reason))>0)),
 CONSTRAINT ck_item_review_version CHECK(version>=0 AND format_version>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='明细审核证据及独立审核结果';

CREATE TABLE IF NOT EXISTS biz_offline_adjustment_note (
 id BIGINT NOT NULL AUTO_INCREMENT,
 summary_id BIGINT NOT NULL COMMENT '仅关联状态2已完结汇总',
 user_id BIGINT NOT NULL COMMENT '服务端从原汇总派生',
 semester VARCHAR(20) NOT NULL COMMENT '服务端从原汇总派生',
 request_key VARCHAR(64) NOT NULL COMMENT '服务端幂等请求标识',
 adjustment_amount DECIMAL(12,2) NOT NULL COMMENT '线下补差金额，可正可负，不回写原酬金',
 reason VARCHAR(500) NOT NULL,
 handled_at DATE NOT NULL COMMENT '实际线下处理日期，不是创建时间',
 handled_by_id BIGINT NOT NULL COMMENT '服务端可信经办用户ID',
 handled_by VARCHAR(64) NOT NULL COMMENT '服务端可信经办账号快照',
 reference_no VARCHAR(100) NULL,
 note_text TEXT NULL COMMENT '可选线下处理备注',
 attachment_ref VARCHAR(500) NULL,
 settlement_snapshot_id BIGINT NULL COMMENT '原完结证据引用，历史缺失保留NULL',
 settlement_snapshot_hash CHAR(64) NULL,
 created_by VARCHAR(64) NOT NULL,
 created_at DATETIME NOT NULL,
 PRIMARY KEY(id),
 UNIQUE KEY uk_adjustment_request(summary_id,request_key),
 KEY idx_adjustment_summary(summary_id,id),
 CONSTRAINT ck_adjustment_reason CHECK(CHAR_LENGTH(TRIM(reason))>0),
 CONSTRAINT ck_adjustment_settlement CHECK(
 (settlement_snapshot_id IS NULL AND settlement_snapshot_hash IS NULL) OR
 (settlement_snapshot_id IS NOT NULL AND settlement_snapshot_hash IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='只追加完结汇总线下补差说明';

SET @review_ddl=IF((SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='biz_workload_summary' AND column_name='review_batch_no')=0,
 'ALTER TABLE biz_workload_summary ADD COLUMN review_batch_no VARCHAR(50) NULL COMMENT ''当前审核批次，旧记录缺失保持NULL''','SELECT 1');
PREPARE review_stmt FROM @review_ddl;
EXECUTE review_stmt;
DEALLOCATE PREPARE review_stmt;
SET @review_ddl=IF((SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='biz_workload_summary' AND column_name='data_version')=0,
 'ALTER TABLE biz_workload_summary ADD COLUMN data_version BIGINT NOT NULL DEFAULT 0 COMMENT ''新版审核数据版本，0表示尚未纳入''','SELECT 1');
PREPARE review_stmt FROM @review_ddl;
EXECUTE review_stmt;
DEALLOCATE PREPARE review_stmt;
SET @review_ddl=IF((SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='biz_workload_summary' AND column_name='last_calculated_at')=0,
 'ALTER TABLE biz_workload_summary ADD COLUMN last_calculated_at DATETIME NULL COMMENT ''新版审核最近核算时间，历史缺失不回填''','SELECT 1');
PREPARE review_stmt FROM @review_ddl;
EXECUTE review_stmt;
DEALLOCATE PREPARE review_stmt;
SET @review_ddl=IF((SELECT COUNT(*) FROM information_schema.statistics
 WHERE table_schema=DATABASE() AND table_name='biz_workload_summary' AND index_name='idx_summary_review_center')=0,
 'CREATE INDEX idx_summary_review_center ON biz_workload_summary(status,semester,review_batch_no,user_id)','SELECT 1');
PREPARE review_stmt FROM @review_ddl;
EXECUTE review_stmt;
DEALLOCATE PREPARE review_stmt;
-- Exact audit allowlist: only missing nullable fields on biz_audit_log.
SET @review_ddl=IF((SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='biz_audit_log' AND column_name='create_by')=0,
 'ALTER TABLE biz_audit_log ADD COLUMN create_by VARCHAR(64) NULL','SELECT 1');
PREPARE review_stmt FROM @review_ddl;
EXECUTE review_stmt;
DEALLOCATE PREPARE review_stmt;
SET @review_ddl=IF((SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='biz_audit_log' AND column_name='update_by')=0,
 'ALTER TABLE biz_audit_log ADD COLUMN update_by VARCHAR(64) NULL','SELECT 1');
PREPARE review_stmt FROM @review_ddl;
EXECUTE review_stmt;
DEALLOCATE PREPARE review_stmt;
SET @review_ddl=IF((SELECT COUNT(*) FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='biz_audit_log' AND column_name='update_time')=0,
 'ALTER TABLE biz_audit_log ADD COLUMN update_time DATETIME NULL','SELECT 1');
PREPARE review_stmt FROM @review_ddl;
EXECUTE review_stmt;
DEALLOCATE PREPARE review_stmt;

-- Pure hidden buttons; no route/component is published before T08.
-- Resolve by perms and allocate menu_id automatically: custom menus cannot collide.
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT p.menu_name,2020,p.order_num,'',NULL,'F','1','0',p.perms,'#','admin',CURRENT_TIMESTAMP
FROM (
 SELECT '待审审核查询' menu_name,30 order_num,'system:auditRecord:list' perms
 UNION ALL SELECT '审核证据详情',31,'system:auditRecord:query'
 UNION ALL SELECT '待审逐条审核',32,'system:auditRecord:review'
 UNION ALL SELECT '审核授权豁免',33,'system:auditRecord:waive'
 UNION ALL SELECT '线下补差说明查询',34,'system:offlineAdjustmentNote:query'
 UNION ALL SELECT '追加线下补差说明',35,'system:offlineAdjustmentNote:add'
) p WHERE NOT EXISTS(SELECT 1 FROM sys_menu m WHERE m.perms=p.perms);
-- Revoke only these NEW permissions outside the explicit role-key matrix.
DELETE grants FROM sys_role_menu grants JOIN sys_menu m ON m.menu_id=grants.menu_id
JOIN sys_role r ON r.role_id=grants.role_id
WHERE (m.perms IN ('system:auditRecord:list','system:auditRecord:query','system:auditRecord:review','system:offlineAdjustmentNote:query')
 AND (r.role_key NOT IN ('admin','biz_admin','jiaowu','assistant') OR r.status<>'0' OR r.del_flag<>'0'))
 OR (m.perms IN ('system:auditRecord:waive','system:offlineAdjustmentNote:add')
 AND (r.role_key NOT IN ('admin','biz_admin') OR r.status<>'0' OR r.del_flag<>'0'));
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m
 ON m.perms IN ('system:auditRecord:list','system:auditRecord:query','system:auditRecord:review','system:offlineAdjustmentNote:query')
WHERE r.role_key IN ('admin','biz_admin','jiaowu','assistant') AND r.status='0' AND r.del_flag='0';
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m
 ON m.perms IN ('system:auditRecord:waive','system:offlineAdjustmentNote:add')
WHERE r.role_key IN ('admin','biz_admin') AND r.status='0' AND r.del_flag='0';
