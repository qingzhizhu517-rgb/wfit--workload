-- ============================================================
-- 08_review_fixes.sql — 代码审查修复（docs/代码审查报告.md）
-- 执行: mysql -u root -p wflg_workload < 08_review_fixes.sql
--
-- 本脚本幂等，可重复执行：
--   1. biz_audit_log 审计日志表     （承接 P1-03/P1-05，CREATE TABLE IF NOT EXISTS）
--   2. 权限修复                     （承接 P2-03/P3-03）
--      - 教师 role4 增授 20205 审批提交（修复教师提交按钮永不渲染）
--      - 院领导 role5 撤销 20206 审批通过（错位授权；保留 20208 签字）
--      - 新增按钮菜单 20215 教师确认（system:audit:teacherConfirm）
--        并授权 role4；role1(admin) 同步显式授权，保证不重放
--        06_test_accounts.sql L28 的全量 SELECT 时 admin 亦持有该权限
--
-- 口径说明：
--   biz_audit_log.from_status / to_status 采用 TINYINT(1)，与
--   01_biz_schema.sql 中 biz_workload_summary.status（TINYINT(1)，
--   0填报中/1待教务审核/2待院领导签字/3已完结）保持一致（已核对）。
-- ============================================================

-- ==================== 1. 审批流转日志表（P1-03/P1-05） ====================
CREATE TABLE IF NOT EXISTS biz_audit_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  summary_id    BIGINT       NOT NULL COMMENT '学期汇总ID',
  action        VARCHAR(32)  NOT NULL COMMENT '动作: submit/approve/reject/sign/unlock/teacherConfirm',
  from_status   TINYINT(1)   DEFAULT NULL COMMENT '流转前状态（与 biz_workload_summary.status 同口径: 0填报中/1待教务审核/2待院领导签字/3已完结）',
  to_status     TINYINT(1)   DEFAULT NULL COMMENT '流转后状态',
  operator_id   BIGINT       DEFAULT NULL COMMENT '操作人ID',
  operator_name VARCHAR(64)  DEFAULT NULL COMMENT '操作人',
  reason        VARCHAR(500) DEFAULT NULL COMMENT '原因（驳回理由等）',
  create_time   DATETIME     DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_audit_log_summary (summary_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作量审批流转日志';

-- ==================== 2. 权限修复（P2-03/P3-03） ====================

-- 2.1 新增按钮菜单 20215「教师确认」（05_biz_menu.sql 已核对：20215 未被占用）
--     写法与 05_biz_menu.sql 中其他 F 型按钮菜单一致；INSERT IGNORE 保证重跑不报错
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20215, '教师确认', 2020, 12, '', '', '', '', 1, 0, 'F', '0', '0', 'system:audit:teacherConfirm', '#', 'admin', sysdate(), '', NULL, '');

-- 2.2 教师角色（role_id=4）增授审批提交按钮，修复教师提交按钮永不渲染（P3-03）
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES (4, 20205);

-- 2.3 院领导角色（role_id=5）撤销错位的 approve 授权；(5, 20208) sign 保留
DELETE FROM sys_role_menu WHERE role_id = 5 AND menu_id = 20206;

-- 2.4 教师确认按钮授权教师角色（role_id=4）
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES (4, 20215);

-- 2.5 admin 角色（role_id=1）和业务管理员（role_id=2）显式授权 20215
--     （06_test_accounts.sql 的 SELECT 全量授权会自动覆盖，此处保证不重放 06 时也有该权限）
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) SELECT 1, menu_id FROM sys_menu WHERE menu_id = 20215;
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) SELECT 2, menu_id FROM sys_menu WHERE menu_id = 20215;

-- ==================== 3. 回归补丁（审批链 approve 403 修复） ====================
-- 回归验证发现：2.3 撤销 (5,20206) 后，20206(approve)/20207(reject)/20209(unlock)
-- 无任何业务角色持有，审批链在 approve 处 403；另线上库 sys_user_role 缺 (1001,1)
-- 致 admin_test 无权限。以下补丁均幂等（INSERT IGNORE），可重复执行。

-- 3.1 教务助理（role_id=3）承接审批审核环节：approve/reject/unlock（审查报告 P2-03 修复口径）
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES (3, 20206), (3, 20207), (3, 20209);

-- 3.2 测试账号角色绑定补齐（防账号与角色脱绑；与 06_test_accounts.sql 声明一致）
--     admin_test 使用 role_id=2（业务管理员），避免 role_id=1 被框架跳过
INSERT IGNORE INTO sys_user_role(user_id, role_id) VALUES (1001, 2), (1002, 3), (1003, 4), (1004, 5);

-- 3.3 admin 角色（role_id=1）和业务管理员（role_id=2）业务菜单全量授权补齐
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) SELECT 1, menu_id FROM sys_menu WHERE menu_id >= 2000;
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) SELECT 2, menu_id FROM sys_menu WHERE menu_id >= 2000;
