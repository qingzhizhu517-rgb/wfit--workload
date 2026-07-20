-- ============================================================
-- 潍理工教师工作量管理系统 业务表 DDL（18 张 biz_* 表）
-- 库：wflg_workload  字符集：utf8mb4
-- 分层：支撑层 5 / 源数据层 2 / 计算明细层 8 / 汇总层 1 / 酬金层 2
-- ============================================================

-- ========== 支撑层 ==========
-- 教师业务档案
DROP TABLE IF EXISTS biz_teacher_profile;
CREATE TABLE biz_teacher_profile (
  user_id              BIGINT(20)    NOT NULL                   COMMENT '教师ID(关联sys_user.user_id)',
  title                VARCHAR(50)   DEFAULT NULL                COMMENT '职称(教授/副教授/讲师/助教/未定级)',
  teacher_nature       VARCHAR(50)   DEFAULT '专任'              COMMENT '人员性质(专任/外聘/校企/银龄/青州外聘)',
  special_status       VARCHAR(50)   DEFAULT '正常'              COMMENT '特殊状态(正常/产假/在职读博/访学)',
  special_status_start DATE          DEFAULT NULL                COMMENT '特殊状态起',
  special_status_end   DATE          DEFAULT NULL                COMMENT '特殊状态止',
  enterprise_eval_result VARCHAR(20) DEFAULT NULL                COMMENT '校企考核结果(优秀/合格/不合格)',
  dept_id              BIGINT(20)    DEFAULT NULL                COMMENT '院部(sys_dept.dept_id)',
  create_by            VARCHAR(64)   DEFAULT ''                  COMMENT '创建者',
  create_time          DATETIME      DEFAULT NULL                COMMENT '创建时间',
  update_by            VARCHAR(64)   DEFAULT ''                  COMMENT '更新者',
  update_time          DATETIME      DEFAULT NULL                COMMENT '更新时间',
  remark               VARCHAR(500)  DEFAULT NULL                COMMENT '备注',
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师业务档案';

-- G 类别字典
DROP TABLE IF EXISTS biz_workload_category_dict;
CREATE TABLE biz_workload_category_dict (
  type_code            VARCHAR(10)   NOT NULL                    COMMENT '分类代码(G1..G11)',
  type_name            VARCHAR(100)  NOT NULL                    COMMENT '分类名称',
  parent_group         VARCHAR(20)   NOT NULL                    COMMENT '所属大类(TEACHING/ADMIN/EXTRA)',
  calc_strategy        VARCHAR(100)  DEFAULT NULL                COMMENT 'Java计算策略bean名',
  is_calc_excess       TINYINT(1)    DEFAULT 1                    COMMENT '是否计入超额核算(1是0否)',
  sort_order           INT(11)       DEFAULT 0                    COMMENT '排序',
  status               TINYINT(1)    DEFAULT 1                    COMMENT '状态(1正常0停用)',
  create_by            VARCHAR(64)   DEFAULT '',
  create_time          DATETIME      DEFAULT NULL,
  update_by            VARCHAR(64)   DEFAULT '',
  update_time          DATETIME      DEFAULT NULL,
  remark               VARCHAR(500)  DEFAULT NULL,
  PRIMARY KEY (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作量类别动态字典';

-- 全局规则参数
DROP TABLE IF EXISTS biz_workload_rule;
CREATE TABLE biz_workload_rule (
  id                   BIGINT(20)    NOT NULL AUTO_INCREMENT,
  rule_code            VARCHAR(50)   NOT NULL                    COMMENT '参数键名',
  rule_value           DECIMAL(10,2) NOT NULL                    COMMENT '参数数值',
  rule_desc            VARCHAR(255)  DEFAULT NULL                COMMENT '参数说明',
  effective_from       DATE          DEFAULT NULL,
  effective_to         DATE          DEFAULT NULL,
  status               TINYINT(1)    DEFAULT 1,
  create_by            VARCHAR(64)   DEFAULT '',
  create_time          DATETIME      DEFAULT NULL,
  update_by            VARCHAR(64)   DEFAULT '',
  update_time          DATETIME      DEFAULT NULL,
  remark               VARCHAR(500)  DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rule_code_eff (rule_code, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局核算规则参数';

-- 职称单位酬金费率
DROP TABLE IF EXISTS biz_pay_rate;
CREATE TABLE biz_pay_rate (
  id                   BIGINT(20)    NOT NULL AUTO_INCREMENT,
  title                VARCHAR(50)   NOT NULL                    COMMENT '职称',
  rate                 DECIMAL(10,2) NOT NULL                    COMMENT '单位工作量酬金(元)',
  effective_from       DATE          NOT NULL,
  effective_to         DATE          DEFAULT NULL,
  status               TINYINT(1)    DEFAULT 1,
  create_by            VARCHAR(64)   DEFAULT '',
  create_time          DATETIME      DEFAULT NULL,
  update_by            VARCHAR(64)   DEFAULT '',
  update_time          DATETIME      DEFAULT NULL,
  remark               VARCHAR(500)  DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职称单位酬金费率';

-- 导入批次记录
DROP TABLE IF EXISTS biz_import_batch;
CREATE TABLE biz_import_batch (
  id                   BIGINT(20)    NOT NULL AUTO_INCREMENT,
  batch_no             VARCHAR(50)   NOT NULL                    COMMENT '批次号',
  import_type          VARCHAR(20)   NOT NULL                    COMMENT '教学任务/教师信息/岗位任职',
  file_name            VARCHAR(200)  DEFAULT NULL,
  file_url             VARCHAR(500)  DEFAULT NULL,
  total_count          INT(11)       DEFAULT 0,
  success_count        INT(11)       DEFAULT 0,
  fail_count           INT(11)       DEFAULT 0,
  status               TINYINT(1)    DEFAULT 0                    COMMENT '0解析中/1待确认/2已导入/3已驳回/4失败',
  error_summary        VARCHAR(1000) DEFAULT NULL,
  create_by            VARCHAR(64)   DEFAULT '',
  create_time          DATETIME      DEFAULT NULL,
  update_by            VARCHAR(64)   DEFAULT '',
  update_time          DATETIME      DEFAULT NULL,
  remark               VARCHAR(500)  DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导入批次记录';

-- ========== 源数据层 ==========
-- 导入教学任务
DROP TABLE IF EXISTS biz_teaching_task;
CREATE TABLE biz_teaching_task (
  id                   BIGINT(20)    NOT NULL AUTO_INCREMENT,
  user_id              BIGINT(20)    NOT NULL                    COMMENT '教师ID',
  semester             VARCHAR(20)   NOT NULL                    COMMENT '学年学期(如2025-2026-1)',
  academic_year        VARCHAR(20)   DEFAULT NULL,
  course_name          VARCHAR(100)  NOT NULL                    COMMENT '课程名称',
  course_code          VARCHAR(50)   DEFAULT NULL                COMMENT '课程代码',
  education_level      VARCHAR(20)   NOT NULL DEFAULT '本科'      COMMENT '本科(含专升本)/专科',
  major_category       VARCHAR(50)   DEFAULT '理工类'             COMMENT '理工类/文史类/艺术类/其他',
  course_nature        VARCHAR(20)   DEFAULT '必修'               COMMENT '必修/选修',
  course_level         VARCHAR(50)   DEFAULT '其他'              COMMENT '省级一流/校级精品/其他',
  course_role          VARCHAR(20)   DEFAULT '独立'              COMMENT '主持人/团队前3/独立',
  class_name           VARCHAR(100)  DEFAULT NULL,
  student_count        INT(11)       DEFAULT 0                    COMMENT '合堂人数',
  theory_hours         DECIMAL(10,2) DEFAULT NULL                 COMMENT '理论学时J1',
  practice_hours       DECIMAL(10,2) DEFAULT NULL                 COMMENT '实践学时J2',
  repeat_order         INT(11)       DEFAULT 1                    COMMENT '同名课第几次(1/2/3+ -> C1 1.0/0.9/0.8)',
  import_source        VARCHAR(50)   DEFAULT NULL,
  import_batch         VARCHAR(50)   DEFAULT NULL,
  import_time          DATETIME      DEFAULT NULL,
  status               TINYINT(1)    DEFAULT 1,
  create_by            VARCHAR(64)   DEFAULT '',
  create_time          DATETIME      DEFAULT NULL,
  update_by            VARCHAR(64)   DEFAULT '',
  update_time          DATETIME      DEFAULT NULL,
  remark               VARCHAR(500)  DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_user_sem (user_id, semester),
  KEY idx_course_sem (course_name, semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导入教学任务';

-- 岗位任职
DROP TABLE IF EXISTS biz_role_assignment;
CREATE TABLE biz_role_assignment (
  id                   BIGINT(20)    NOT NULL AUTO_INCREMENT,
  user_id              BIGINT(20)    NOT NULL,
  role_type            VARCHAR(50)   NOT NULL                    COMMENT '班主任/系主任/教研室主任/专业负责人/俱乐部经理/实验人员/督导/中层副职/心理中心',
  target               VARCHAR(100)  DEFAULT NULL                COMMENT '目标班级或范围',
  start_date           DATE          NOT NULL                    COMMENT '任职起',
  end_date             DATE          DEFAULT NULL                COMMENT '任职止(NULL=至今)',
  semester             VARCHAR(20)   DEFAULT NULL,
  academic_year        VARCHAR(20)   DEFAULT NULL,
  allowance_rate       DECIMAL(10,2) NOT NULL                    COMMENT '该岗位标准学时/学年',
  status               TINYINT(1)    DEFAULT 1,
  create_by            VARCHAR(64)   DEFAULT '',
  create_time          DATETIME      DEFAULT NULL,
  update_by            VARCHAR(64)   DEFAULT '',
  update_time          DATETIME      DEFAULT NULL,
  remark               VARCHAR(500)  DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_user_sem (user_id, semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位任职';

-- ========== 计算明细层 ==========
-- 工作量明细主表
DROP TABLE IF EXISTS biz_workload_item;
CREATE TABLE biz_workload_item (
  id                   BIGINT(20)    NOT NULL AUTO_INCREMENT,
  user_id              BIGINT(20)    NOT NULL,
  semester             VARCHAR(20)   NOT NULL,
  academic_year        VARCHAR(20)   DEFAULT NULL,
  item_type            VARCHAR(10)   NOT NULL                    COMMENT 'G1..G9,G11',
  source_type          VARCHAR(10)   NOT NULL                    COMMENT 'IMPORT/MANUAL',
  task_id              BIGINT(20)    DEFAULT NULL                COMMENT 'FK biz_teaching_task',
  assignment_id        BIGINT(20)    DEFAULT NULL                COMMENT 'FK biz_role_assignment(G11)',
  course_name          VARCHAR(100)  DEFAULT NULL,
  education_level      VARCHAR(20)   DEFAULT NULL,
  major_category       VARCHAR(50)   DEFAULT NULL,
  calculated_workload  DECIMAL(10,2) NOT NULL                    COMMENT '最终核算工作量',
  description          VARCHAR(500)  DEFAULT NULL                COMMENT 'G8/G9说明',
  is_over_limit        TINYINT(1)    DEFAULT 0                    COMMENT '指导人数超标(G5/G6)',
  dean_approval_status TINYINT(1)    DEFAULT 0                    COMMENT '0未审批/1通过/2驳回',
  dean_approval_by     VARCHAR(64)   DEFAULT NULL,
  dean_approval_time   DATETIME      DEFAULT NULL,
  appeal_status        TINYINT(1)    DEFAULT 0                    COMMENT '0无/1申诉中/2已处理/3已驳回',
  appeal_reason        VARCHAR(500)  DEFAULT NULL,
  appeal_reply         VARCHAR(500)  DEFAULT NULL,
  status               TINYINT(1)    DEFAULT 0                    COMMENT '0草稿/1已核对/2有异议/3已驳回',
  create_by            VARCHAR(64)   DEFAULT '',
  create_time          DATETIME      DEFAULT NULL,
  update_by            VARCHAR(64)   DEFAULT '',
  update_time          DATETIME      DEFAULT NULL,
  remark               VARCHAR(500)  DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_user_sem_type (user_id, semester, item_type),
  KEY idx_task (task_id),
  KEY idx_assignment (assignment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作量明细主表';

-- G1 理论课
DROP TABLE IF EXISTS biz_wl_theory;
CREATE TABLE biz_wl_theory (
  item_id   BIGINT(20)     NOT NULL                COMMENT 'FK biz_workload_item.id',
  J1        DECIMAL(10,2)  NOT NULL                COMMENT '理论学时',
  C1        DECIMAL(10,2)  DEFAULT 1.00            COMMENT '重复系数1.0/0.9/0.8',
  K1        DECIMAL(10,2)  DEFAULT 1.10            COMMENT '课程类型必修1.1/选修1.0',
  Q1        DECIMAL(10,2)  DEFAULT 1.00            COMMENT '教学质量1.0/不合格0.8',
  Q2        DECIMAL(10,2)  DEFAULT 1.00            COMMENT '课程质量',
  Q3        DECIMAL(10,2)  DEFAULT 1.00            COMMENT '全外文系数',
  N         DECIMAL(10,2)  DEFAULT 1.00            COMMENT '合堂1.1/1.2',
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME DEFAULT NULL,
  remark    VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='G1理论课明细';

-- G2 课内实践/实验/实训
DROP TABLE IF EXISTS biz_wl_practice;
CREATE TABLE biz_wl_practice (
  item_id BIGINT(20) NOT NULL,
  J2      DECIMAL(10,2) NOT NULL                   COMMENT '实践学时',
  K       DECIMAL(10,2) DEFAULT 1.00               COMMENT '理工1.0/其他0.9',
  C2      DECIMAL(10,2) DEFAULT 1.00               COMMENT '实践重复第一次1.0/第二次起0.9',
  Q1      DECIMAL(10,2) DEFAULT 1.00, Q2 DECIMAL(10,2) DEFAULT 1.00, Q3 DECIMAL(10,2) DEFAULT 1.00,
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='G2课内实践明细';

-- G3 教学实习/实训
DROP TABLE IF EXISTS biz_wl_internship_training;
CREATE TABLE biz_wl_internship_training (
  item_id BIGINT(20) NOT NULL,
  T       DECIMAL(10,2) NOT NULL                   COMMENT '实际天数(1天=8学时)',
  D       DECIMAL(10,2) NOT NULL                   COMMENT '指导系数理工4/艺术3/文史2/单位2',
  K       DECIMAL(10,2) DEFAULT 1.00               COMMENT '重复系数第一轮1/第二轮0.9',
  Q1      DECIMAL(10,2) DEFAULT 1.00, Q2 DECIMAL(10,2) DEFAULT 1.00, Q3 DECIMAL(10,2) DEFAULT 1.00,
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='G3教学实习实训明细';

-- G4 课程设计
DROP TABLE IF EXISTS biz_wl_course_design;
CREATE TABLE biz_wl_course_design (
  item_id BIGINT(20) NOT NULL,
  J4      DECIMAL(10,2) NOT NULL                   COMMENT '课程设计学分',
  R4      INT(11) NOT NULL                         COMMENT '指导人数(<=20)',
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='G4课程设计明细';

-- G5 毕业论文
DROP TABLE IF EXISTS biz_wl_thesis;
CREATE TABLE biz_wl_thesis (
  item_id BIGINT(20) NOT NULL,
  R5      INT(11) NOT NULL                         COMMENT '指导人数(本<=10,专<=15)',
  K5      DECIMAL(10,2) NOT NULL                   COMMENT '系数理工本9/专5,文史本6/专4',
  education_level VARCHAR(20) DEFAULT NULL        COMMENT '本科/专科',
  major          VARCHAR(50) DEFAULT NULL         COMMENT '理工类/文史类',
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='G5毕业论文明细';

-- G6 集中实习(现场跟班)
DROP TABLE IF EXISTS biz_wl_concentrated_internship;
CREATE TABLE biz_wl_concentrated_internship (
  item_id BIGINT(20) NOT NULL,
  W       DECIMAL(10,2) NOT NULL                   COMMENT '实习周数',
  R6      INT(11) NOT NULL                         COMMENT '指导人数(<=20)',
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='G6集中实习明细';

-- G11 管理服务
DROP TABLE IF EXISTS biz_wl_management;
CREATE TABLE biz_wl_management (
  item_id         BIGINT(20) NOT NULL,
  assignment_id   BIGINT(20) NOT NULL               COMMENT 'FK biz_role_assignment',
  role_type       VARCHAR(50) DEFAULT NULL,
  prorated_amount DECIMAL(10,2) NOT NULL            COMMENT '按任职区间折算学时',
  proration_basis VARCHAR(200) DEFAULT NULL         COMMENT '折算说明',
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='G11管理服务明细';

-- ========== 汇总层 ==========
DROP TABLE IF EXISTS biz_workload_summary;
CREATE TABLE biz_workload_summary (
  id                       BIGINT(20)    NOT NULL AUTO_INCREMENT,
  user_id                  BIGINT(20)    NOT NULL,
  semester                 VARCHAR(20)   NOT NULL,
  academic_year            VARCHAR(20)   DEFAULT NULL,
  G7                       DECIMAL(10,2) DEFAULT 0              COMMENT '第一课堂=G1+G2+G3+G4+G5+G6',
  G8                       DECIMAL(10,2) DEFAULT 0              COMMENT '第二课堂',
  G9                       DECIMAL(10,2) DEFAULT 0              COMMENT '其他',
  G10                      DECIMAL(10,2) DEFAULT 0              COMMENT '教学合计=G7+G8+G9',
  G11                      DECIMAL(10,2) DEFAULT 0              COMMENT '管理服务',
  total_workload           DECIMAL(10,2) DEFAULT 0              COMMENT '总工作量=G10+G11',
  rated_workload           DECIMAL(10,2) DEFAULT 180.00        COMMENT '额定(统一180)',
  excess_workload          DECIMAL(10,2) DEFAULT 0              COMMENT '超额定=max(0,total-rated)',
  title                    VARCHAR(50)   DEFAULT NULL           COMMENT '职称快照',
  pay_rate                 DECIMAL(10,2) DEFAULT NULL           COMMENT '单位酬金快照',
  performance_pay          DECIMAL(10,2) DEFAULT 0              COMMENT '绩效酬金=(min(total,540)-180)*rate',
  is_capped                TINYINT(1)    DEFAULT 0              COMMENT '触200%封顶',
  basic_teaching_standard  DECIMAL(10,2) DEFAULT NULL           COMMENT '第五条达标标准/学期',
  basic_teaching_met       TINYINT(1)    DEFAULT 0              COMMENT '达标G10>=standard',
  status                   TINYINT(1)    DEFAULT 0              COMMENT '0草稿/1已公示/2已审核/3已锁定',
  teacher_sign             VARCHAR(64)   DEFAULT NULL,
  teacher_sign_time        DATETIME      DEFAULT NULL,
  dept_leader_sign         VARCHAR(64)   DEFAULT NULL,
  dept_leader_sign_time    DATETIME      DEFAULT NULL,
  academic_assistant_sign  VARCHAR(64)   DEFAULT NULL,
  academic_assistant_sign_time DATETIME   DEFAULT NULL,
  lock_time                DATETIME      DEFAULT NULL,
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_sem (user_id, semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期工作量汇总';

-- ========== 酬金层 ==========
DROP TABLE IF EXISTS biz_pay_record;
CREATE TABLE biz_pay_record (
  id              BIGINT(20)    NOT NULL AUTO_INCREMENT,
  user_id         BIGINT(20)    NOT NULL,
  semester        VARCHAR(20)   NOT NULL,
  summary_id      BIGINT(20)    DEFAULT NULL            COMMENT 'FK biz_workload_summary',
  course_hour_pay DECIMAL(10,2) DEFAULT 0              COMMENT '课时/绩效酬金',
  other_pay_total DECIMAL(10,2) DEFAULT 0              COMMENT '其他酬金合计A+B+C+D+E+F+G',
  total_pay       INT(11)       DEFAULT 0              COMMENT '总金额(四舍五入取整)',
  status          TINYINT(1)    DEFAULT 0,
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_sem (user_id, semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酬金汇总';

DROP TABLE IF EXISTS biz_allowance_item;
CREATE TABLE biz_allowance_item (
  id              BIGINT(20)    NOT NULL AUTO_INCREMENT,
  pay_record_id   BIGINT(20)    DEFAULT NULL            COMMENT 'FK biz_pay_record',
  user_id         BIGINT(20)    NOT NULL,
  semester        VARCHAR(20)   NOT NULL,
  fee_type        VARCHAR(10)   NOT NULL                COMMENT 'A/B/C/D/E/F/G',
  fee_subtype     VARCHAR(20)   DEFAULT NULL            COMMENT 'A重修:跟班/单独开班/自学辅导;B实习:分散/集中不跟班',
  student_count   INT(11)       DEFAULT 0,
  duration_hours  DECIMAL(10,2) DEFAULT NULL            COMMENT 'E讲座时长',
  days            DECIMAL(10,2) DEFAULT NULL            COMMENT 'F运动会天数',
  class_count     INT(11)       DEFAULT NULL            COMMENT 'F体测班数',
  workload_units  DECIMAL(10,2) DEFAULT NULL            COMMENT 'G夜间值班工作量',
  lecture_name    VARCHAR(200)  DEFAULT NULL            COMMENT 'E讲座名称',
  ext             JSON          DEFAULT NULL            COMMENT '扩展字段',
  amount          DECIMAL(10,2) NOT NULL                COMMENT '计算金额',
  status          TINYINT(1)    DEFAULT 1               COMMENT '1正常0停用(D代阅卷默认0)',
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_pay_record (pay_record_id),
  KEY idx_user_sem (user_id, semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='其他酬金明细';
