-- ----------------------------------------------------------------------------
-- 17_simplify_approval.sql
-- 审批流简化为两级（教师 + 教务处），移除院领导签字环节
--
-- 状态机变化：
--   旧：0 填报中 → 1 教务助理待审 → 2 院领导待签 → 3 已完结(锁定)；驳回 1/2 → 0
--   新：0 填报中 → 1 教务处待审  → 2 已完结(锁定)          ；驳回 1   → 0
--
-- 配套代码改动（同批提交）：
--   BizAuditServiceImpl   approve 改为 1→2 终态；删除 sign；reject/teacherConfirm 收窄为仅 1
--   BizAuditService       删除 sign 接口方法
--   BizAuditController    删除 POST /system/audit/sign 端点
--   BizWorkloadSummaryMapper.xml  approveSummary 增写 lock_time；删 signSummary；
--                                unlockById where status=2；teacherConfirmById status in (1)
--   WorkloadCalcServiceImpl / BizWorkloadSummaryController  SUMMARY_STATUS_FINISHED 3→2
--
-- 保留但不再写入的列：dept_leader_sign / dept_leader_sign_time
--   （保留以便将来恢复院领导签字；rejectSummary / unlockById 仍会清空它们，
--     防止存量数据残留旧签字导致草稿态显示"已签"）
--
-- 幂等：可重复执行。
-- ----------------------------------------------------------------------------

-- 1. 存量数据迁移：已完结 3 → 2
--    执行前确认：SELECT status, COUNT(*) FROM biz_workload_summary GROUP BY status;
UPDATE biz_workload_summary SET status = 2 WHERE status = 3;

-- 1b. 同步列注释（原注释含已废弃的「2待院领导签字/3已完结」口径）
ALTER TABLE biz_workload_summary
  MODIFY COLUMN status TINYINT(1) DEFAULT 0 COMMENT '0填报中/1教务处待审/2已完结(锁定)';

-- 2. 移除院领导签字权限（对应端点已删除，留着即死权限）
DELETE FROM sys_role_menu
 WHERE menu_id IN (
        SELECT menu_id FROM (
          SELECT menu_id FROM sys_menu WHERE perms = 'system:audit:sign'
        ) t
 );

DELETE FROM sys_menu WHERE perms = 'system:audit:sign';

-- 3. 院领导(role_id=5)不再有审批环节，撤销其驳回权限
--    （13_fix_audit_perm.sql 第 3 步曾为「待签环节可驳回」授予它；该环节已不存在）
--    注：role_id=5 角色本身是否保留，属角色模型收敛，另行处理
DELETE FROM sys_role_menu WHERE role_id = 5 AND menu_id = 20207;

-- 4. 校验（人工执行时启用）
-- 4.1 应无 status=3 的行
-- SELECT COUNT(*) AS still_finished3 FROM biz_workload_summary WHERE status = 3;
-- 4.2 应无 system:audit:sign
-- SELECT * FROM sys_menu WHERE perms = 'system:audit:sign';
-- 4.3 院领导不应再有 20207
-- SELECT * FROM sys_role_menu WHERE role_id = 5 AND menu_id = 20207;
-- 4.4 保留项检查（应仍在）
-- SELECT menu_id, menu_name, perms FROM sys_menu
--  WHERE perms IN ('system:audit:submit','system:audit:approve','system:audit:reject',
--                  'system:audit:unlock','system:audit:teacherConfirm');
