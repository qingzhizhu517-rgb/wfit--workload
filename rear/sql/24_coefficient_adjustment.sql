-- ============================================================
-- 24_coefficient_adjustment.sql —— G1/G2 系数调整申请模型 + 状态机
--
-- 计划编号修正：原计划写 23_coefficient_adjustment.sql，与既有
-- 23_calculation_consistency.sql 冲突，本任务顺延为 24。
--
-- 内容：
--   1) 新建 biz_coefficient_adjustment（系数调整申请，含审批状态机）
--   2) 用生成列 pending_key + UNIQUE 保证同一 item+factor 同时只允许一条待审
--
-- 状态机：0 待审(PENDING) → 1 通过(APPROVED) / 2 驳回(REJECTED)；3 撤销(CANCELLED)。
-- 通过后原子应用系数并重算由 Task 9 负责，本表只承载申请与审批状态。
--
-- 幂等：可重复执行（CREATE TABLE IF NOT EXISTS）。
-- 不含任何无条件 DROP / DELETE / TRUNCATE。
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_coefficient_adjustment (
  id                       BIGINT(20)     NOT NULL AUTO_INCREMENT,
  user_id                  BIGINT(20)     NOT NULL                 COMMENT '申请所属教师（服务端由明细带出）',
  semester                 VARCHAR(20)    NOT NULL                 COMMENT '学年学期',
  task_id                  BIGINT(20)     DEFAULT NULL             COMMENT 'FK biz_teaching_task（可空）',
  item_id                  BIGINT(20)     NOT NULL                 COMMENT 'FK biz_workload_item.id',
  category                 VARCHAR(10)    NOT NULL                 COMMENT '工作量类别：仅 G1/G2',
  factor_code              VARCHAR(16)    NOT NULL                 COMMENT '系数编码：G1 的 C1/K1/Q1/Q2/N，G2 的 K/C2/Q1/Q2',
  old_value                DECIMAL(10,2)  DEFAULT NULL             COMMENT '申请前系数值（服务端读取，客户端传入被忽略）',
  requested_value          DECIMAL(10,2)  NOT NULL                 COMMENT '申请调整为的系数值（>0）',
  reason                   VARCHAR(500)   NOT NULL                 COMMENT '申请理由',
  attachment_url           VARCHAR(255)   DEFAULT NULL             COMMENT '佐证材料地址',
  status                   TINYINT(4)     NOT NULL DEFAULT 0       COMMENT '0待审/1通过/2驳回/3撤销',
  reviewer_id              BIGINT(20)     DEFAULT NULL             COMMENT '审核人',
  review_reason            VARCHAR(500)   DEFAULT NULL             COMMENT '审核意见',
  reviewed_at              DATETIME       DEFAULT NULL             COMMENT '审核时间',
  base_calculation_version BIGINT(20)     NOT NULL DEFAULT 0       COMMENT '提交时明细计算版本（乐观并发）',
  create_by                VARCHAR(64)    DEFAULT ''               COMMENT '创建人',
  create_time              DATETIME       DEFAULT NULL             COMMENT '创建时间',
  update_by                VARCHAR(64)    DEFAULT ''               COMMENT '更新人',
  update_time              DATETIME       DEFAULT NULL             COMMENT '更新时间',
  remark                   VARCHAR(500)   DEFAULT NULL             COMMENT '备注',
  -- 生成列：仅待审(status=0)时取 item:factor，其余为 NULL；配合唯一键保证同一 item+factor 至多一条待审。
  -- MySQL 唯一索引允许多个 NULL，因此已终态的历史申请不互相冲突。
  pending_key              VARCHAR(64)    GENERATED ALWAYS AS (IF(status = 0, CONCAT(item_id, ':', factor_code), NULL)) VIRTUAL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_pending_item_factor (pending_key),
  KEY idx_item (item_id),
  KEY idx_user_semester (user_id, semester),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='G1/G2系数调整申请';
