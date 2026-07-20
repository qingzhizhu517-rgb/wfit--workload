# 潍理工工作量管理系统设计

## 数据库设计

### 数据库设计E-R图（初代）

``` Mermaid
erDiagram
    SYS_USER ||--o| BIZ_TEACHER_PROFILE : "1:1 扩展 (用户与教师信息)"
    SYS_USER {
        bigint user_id PK
        varchar user_name "用户名/工号"
        varchar phonenumber "手机号"
    }

    BIZ_TEACHER_PROFILE {
        bigint user_id PK
        varchar title "职称(教授/讲师...)"
        varchar category "类别(专任/外聘...)"
        int min_workload "额定学时"
    }

    BIZ_TEACHER_PROFILE ||--o{ BIZ_TEACHING_TASK : "1:N 承担 (教师与教学任务)"
    BIZ_TEACHING_TASK {
        bigint task_id PK
        varchar semester "学年学期"
        varchar course_code "课程代码"
        int plan_hours "计划学时"
        int student_count "选课人数"
    }

    BIZ_TEACHING_TASK ||--o| BIZ_WORKLOAD_RECORD : "1:1 核算 (任务与明细记录)"
    BIZ_TEACHER_PROFILE ||--o{ BIZ_WORKLOAD_RECORD : "1:N 产生 (教师与非教学类明细)"
    BIZ_WORKLOAD_RECORD {
        bigint record_id PK
        bigint task_id FK
        bigint teacher_id FK
        varchar workload_type "类型(理论/实验...)"
        decimal final_workload "最终核算学时"
        decimal coef_c "重复系数快照"
        decimal coef_n "合堂系数快照"
    }

    BIZ_TEACHER_PROFILE ||--o{ BIZ_WORKLOAD_SUMMARY : "1:N 归集 (教师与学期汇总)"
    BIZ_WORKLOAD_RECORD }o--|| BIZ_WORKLOAD_SUMMARY : "N:1 统计依据 (明细与汇总)"
    BIZ_WORKLOAD_SUMMARY {
        bigint summary_id PK
        bigint teacher_id FK
        varchar semester "学年学期"
        decimal total_workload "总工作量"
        decimal excess_workload "超额工作量"
        decimal performance_pay "绩效酬金"
    }

    BIZ_RULE_CONFIG {
        bigint config_id PK
        varchar rule_key "参数键名"
        decimal rule_value "参数数值"
        varchar description "规则描述"
    }
```

### 数据库建表sql（初代）

``` mysql
-- 1. 教师扩展信息表（与若依 sys_user 一对一关联）
CREATE TABLE `biz_teacher_profile` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID(关联sys_user)',
  `title` varchar(50) DEFAULT NULL COMMENT '职称(教授/副教授/讲师/助教等)',
  `category` varchar(50) DEFAULT NULL COMMENT '人员类别(专任/外聘/校领导/行政人员等)',
  `min_workload` int(11) DEFAULT 0 COMMENT '学年额定工作量(学时)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师扩展信息表';

-- 2. 原始教学任务表 (教务处导入的输入数据)
CREATE TABLE `biz_teaching_task` (
  `task_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `semester` varchar(20) NOT NULL COMMENT '学年学期(如:2025-2026-1)',
  `course_code` varchar(50) NOT NULL COMMENT '课程代码',
  `course_name` varchar(100) NOT NULL COMMENT '课程名称',
  `course_type` varchar(50) DEFAULT NULL COMMENT '课程类型(理论/课内实践/独立实验等)',
  `teacher_id` bigint(20) NOT NULL COMMENT '任课教师ID',
  `student_count` int(11) DEFAULT 0 COMMENT '选课人数/合堂人数',
  `plan_hours` int(11) DEFAULT 0 COMMENT '计划学时(J1/J2等)',
  `calc_status` tinyint(1) DEFAULT 0 COMMENT '核算状态(0未核算 1已核算)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者(导入人)',
  `create_time` datetime DEFAULT NULL COMMENT '导入时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`task_id`),
  KEY `idx_teacher_semester` (`teacher_id`,`semester`)
) ENGINE=InnoDB AUTO_INCREMENT=10000 DEFAULT CHARSET=utf8mb4 COMMENT='原始教学任务表';

-- 3. 工作量核算明细表 (对应附件1)
CREATE TABLE `biz_workload_record` (
  `record_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `task_id` bigint(20) DEFAULT NULL COMMENT '关联教学任务ID(管理类无此ID)',
  `teacher_id` bigint(20) NOT NULL COMMENT '教师ID',
  `semester` varchar(20) NOT NULL COMMENT '学年学期',
  `workload_type` varchar(50) NOT NULL COMMENT '工作量类型(理论教学/集中实习/管理服务等)',
  `base_hours` decimal(10,2) DEFAULT 0.00 COMMENT '基数学时/天数/周数',
  `coef_c` decimal(10,2) DEFAULT 1.00 COMMENT '重复系数(C1/C2)',
  `coef_k` decimal(10,2) DEFAULT 1.00 COMMENT '课程系数(K/K1/K5)',
  `coef_q1` decimal(10,2) DEFAULT 1.00 COMMENT '教学质量系数(Q1)',
  `coef_q2` decimal(10,2) DEFAULT 1.00 COMMENT '课程质量系数(Q2)',
  `coef_n` decimal(10,2) DEFAULT 1.00 COMMENT '合堂系数(N)',
  `coef_other` decimal(10,2) DEFAULT 1.00 COMMENT '其他综合系数',
  `final_workload` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '最终核算工作量(学时)',
  `audit_status` tinyint(1) DEFAULT 0 COMMENT '审核状态(0正常 1异议待审 2已驳回)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '核算时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_teacher_semester` (`teacher_id`,`semester`)
) ENGINE=InnoDB AUTO_INCREMENT=10000 DEFAULT CHARSET=utf8mb4 COMMENT='工作量核算明细表';

-- 4. 学期工作量与绩效汇总表 (对应附件2)
CREATE TABLE `biz_workload_summary` (
  `summary_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '汇总ID',
  `teacher_id` bigint(20) NOT NULL COMMENT '教师ID',
  `semester` varchar(20) NOT NULL COMMENT '学年学期',
  `teacher_title_snap` varchar(50) DEFAULT NULL COMMENT '结算时职称快照',
  `total_teaching_work` decimal(10,2) DEFAULT 0.00 COMMENT '教学总工作量',
  `total_admin_work` decimal(10,2) DEFAULT 0.00 COMMENT '管理服务总工作量',
  `total_workload` decimal(10,2) DEFAULT 0.00 COMMENT '核定总工作量',
  `base_deduction` decimal(10,2) DEFAULT 0.00 COMMENT '应扣除额定工作量',
  `excess_workload` decimal(10,2) DEFAULT 0.00 COMMENT '超额工作量',
  `unit_price` decimal(10,2) DEFAULT 0.00 COMMENT '单位工作量酬金标准(元)',
  `performance_pay` decimal(10,2) DEFAULT 0.00 COMMENT '绩效酬金总计(税前)',
  `create_time` datetime DEFAULT NULL COMMENT '汇总时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '说明',
  PRIMARY KEY (`summary_id`),
  UNIQUE KEY `uk_teacher_semester` (`teacher_id`,`semester`)
) ENGINE=InnoDB AUTO_INCREMENT=10000 DEFAULT CHARSET=utf8mb4 COMMENT='学期工作量与绩效汇总表';

-- 5. 动态核算系数配置表
CREATE TABLE `biz_rule_config` (
  `config_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `rule_type` varchar(50) NOT NULL COMMENT '规则分类(如:合堂系数/课时费标准)',
  `rule_key` varchar(100) NOT NULL COMMENT '规则键名(如:class_size_120_150)',
  `rule_value` decimal(10,2) NOT NULL COMMENT '规则数值(如:1.1)',
  `description` varchar(255) DEFAULT NULL COMMENT '规则描述(如:合堂人数120-150人系数)',
  `update_by` varchar(64) DEFAULT '' COMMENT '修改人',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_rule_key` (`rule_key`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='动态核算系数配置表';
```



### 新针对拓充代——企业级 SaaS 系统中常用的**“元数据驱动（Metadata-Driven）+ 快照留存”**架构



``` mysql



-- ==========================================================
-- 1. 基础扩展层
-- ==========================================================

-- 1.1 教师扩展信息表（与若依 sys_user 表通过 user_id 一对一关联）
CREATE TABLE `biz_teacher_profile` (
  `user_id` bigint(20) NOT NULL COMMENT '教师ID(关联sys_user的user_id)',
  `title` varchar(50) DEFAULT NULL COMMENT '职称(教授/副教授/讲师/助教等)',
  `category` varchar(50) DEFAULT NULL COMMENT '人员类别(专任/外聘/校企/行政等)',
  `min_workload` decimal(10,2) DEFAULT 0.00 COMMENT '学年额定基础工作量(学时)',
  `is_outsource` tinyint(1) DEFAULT 0 COMMENT '是否外聘(1是 0否)',
  `special_status` varchar(50) DEFAULT '正常' COMMENT '特殊状态(正常/产假/在职读博/访学)',
  `teacher_nature` varchar(50) DEFAULT '专任' COMMENT '教师性质(专任/外聘/青州外聘/校企/银龄)',
  `enterprise_eval_result` varchar(20) DEFAULT NULL COMMENT '校企教师考核结果(优秀/合格/不合格)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师基础信息扩展表';

-- ==========================================================
-- 2. 元数据配置层 (系统的大脑)
-- ==========================================================

-- 2.1 工作量类别字典表 (管理 G1, G2... G12 等动态分类)
CREATE TABLE `biz_workload_category_dict` (
  `type_code` varchar(20) NOT NULL COMMENT '分类代码 (主键, 如: G1, G8, G12)',
  `type_name` varchar(100) NOT NULL COMMENT '分类名称 (如: 理论课, 第二课堂, 产学研，汇总)',
  `parent_group` varchar(50) NOT NULL COMMENT '所属大类 (TEACHING:教学, ADMIN:管理, EXTRA:其他)',
  `calc_strategy` varchar(100) DEFAULT NULL COMMENT '关联的Java计算策略Bean名称(如: theoryCalcStrategy)',
  `is_calc_excess` tinyint(1) DEFAULT 1 COMMENT '是否计入超额核算 (1:是 0:否)',
  `sort_order` int(11) DEFAULT 0 COMMENT '前端展示排序',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态 (1:正常 0:停用)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作量类别动态字典表';

-- 2.2 动态规则参数表 (管理 1.0, 0.9, 1.2 等具体数值)
CREATE TABLE `biz_workload_rule` (
  `rule_id` int(11) NOT NULL AUTO_INCREMENT,
  `rule_code` varchar(50) NOT NULL COMMENT '参数键名 (如: COEF_CLASS_120_150)',
  `rule_value` decimal(10,2) NOT NULL COMMENT '参数数值 (如: 1.1)',
  `rule_desc` varchar(255) DEFAULT NULL COMMENT '参数说明 (如: 合堂120-150人系数)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局核算规则参数表';

-- ==========================================================
-- 3. 业务流水层 (事件的快照账本)
-- ==========================================================

-- 3.0 教学任务表 (导入数据)
CREATE TABLE `biz_teaching_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '教师ID',
  `semester` varchar(20) NOT NULL COMMENT '学年学期',
  `course_name` varchar(100) NOT NULL COMMENT '课程/项目名称',
  `education_level` varchar(20) NOT NULL DEFAULT '本科' COMMENT '授课层次(本科/专科)',
  `major_category` varchar(50) DEFAULT '理工类' COMMENT '专业大类(理工类/文史类/艺术类/其他)',
  `course_nature` varchar(20) DEFAULT '必修' COMMENT '课程性质(必修/选修)',
  `course_level` varchar(50) DEFAULT '其他' COMMENT '课程级别(省级一流/校级精品/其他)',
  `course_role` varchar(20) DEFAULT '独立' COMMENT '课程角色(主持人/团队前3/普通成员/独立)',
  `teaching_eval` varchar(20) DEFAULT '合格' COMMENT '期末教学评价(优秀/良好/合格/不合格)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_user_semester` (`user_id`,`semester`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学任务基础信息表';

-- 3.1 教学类工作量明细表 (处理 G1~G6 等需要基数乘系数的类型)
CREATE TABLE `biz_teaching_workload` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '教师ID',
  `semester` varchar(20) NOT NULL COMMENT '学年学期 (如: 2025-2026-1)',
  `item_type` varchar(20) NOT NULL COMMENT '工作量类型(关联category_dict的type_code)',
  `course_name` varchar(100) NOT NULL COMMENT '课程/项目名称',
  `education_level` varchar(20) DEFAULT '本科' COMMENT '授课层次(本科/专科)',
  `course_code` varchar(50) DEFAULT NULL COMMENT '课程代码(便于和教务系统对账)',
  
  -- 基础指标
  `base_value` decimal(10,2) NOT NULL COMMENT '核心基数 (学时/天数/学分/周数)',
  `student_count` int(11) DEFAULT 0 COMMENT '选课/负责人数',
  
  -- 动态系数快照 (核算时的历史切片)
  `coef_repeat` decimal(10,2) DEFAULT 1.00 COMMENT '重复系数快照 (C1/C2)',
  `coef_type` decimal(10,2) DEFAULT 1.00 COMMENT '类型系数快照 (K1/D等)',
  `coef_quality` decimal(10,2) DEFAULT 1.00 COMMENT '质量综合系数快照 (Q1*Q2)',
  `coef_class_size` decimal(10,2) DEFAULT 1.00 COMMENT '合堂系数快照 (N)',
  `calculated_workload` decimal(10,2) NOT NULL COMMENT '最终核算工作量',

  -- 代课与超标审批
  `is_substitute` tinyint(1) DEFAULT 0 COMMENT '是否为代课(1是 0否)',
  `substitute_for` varchar(100) DEFAULT NULL COMMENT '原任课教师(代谁的课)',
  `is_over_limit` tinyint(1) DEFAULT 0 COMMENT '指导人数是否超标(毕设/实习用)',
  `dean_approval_status` tinyint(1) DEFAULT 1 COMMENT '院长超标审批状态(1通过 0驳回)',
  
  -- 异议处理
  `appeal_status` tinyint(2) DEFAULT 0 COMMENT '异议状态(0无 1申诉中 2已处理 3已驳回)',
  `appeal_reason` varchar(500) DEFAULT NULL COMMENT '教师申诉理由',
  `appeal_reply` varchar(500) DEFAULT NULL COMMENT '教务处理回复',

  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_user_semester` (`user_id`,`semester`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学类工作量流水明细表';

-- 3.2 附加与管理工作量表 (处理 G8, G9, G11 等直接核定数值的类型)
CREATE TABLE `biz_extra_workload` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '教师ID',
  `semester` varchar(20) NOT NULL COMMENT '学年学期',
  `item_type` varchar(20) NOT NULL COMMENT '工作量类型(关联category_dict的type_code)',
  `item_name` varchar(100) NOT NULL COMMENT '项目名称(如: 班主任/学科竞赛指导)',
  `workload_value` decimal(10,2) NOT NULL COMMENT '核定工作量数值',
  `start_date` date DEFAULT NULL COMMENT '任职开始日期(变动标明用)',
  `end_date` date DEFAULT NULL COMMENT '任职结束日期(变动标明用)',

  -- 异议处理
  `appeal_status` tinyint(2) DEFAULT 0 COMMENT '异议状态(0无 1申诉中 2已处理 3已驳回)',
  `appeal_reason` varchar(500) DEFAULT NULL COMMENT '教师申诉理由',
  `appeal_reply` varchar(500) DEFAULT NULL COMMENT '教务处理回复',

  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_user_semester` (`user_id`,`semester`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附加与管理类工作量明细表';

-- 3.3 专项酬金明细表 (处理附件2中的 A~G 纯金钱项目)
CREATE TABLE `biz_allowance_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '教师ID',
  `semester` varchar(20) NOT NULL COMMENT '学年学期',
  `allowance_type` varchar(20) NOT NULL COMMENT '酬金类别 (A/B/C/D/E/F/G)',
  `item_name` varchar(100) NOT NULL COMMENT '项目名称',
  `base_count` decimal(10,2) NOT NULL COMMENT '核算基数 (人数/时长/天数)',
  `unit_price` decimal(10,2) DEFAULT 0.00 COMMENT '执行单价',
  `total_amount` decimal(10,2) NOT NULL COMMENT '最终绩效酬金(元)',

  -- 异议处理
  `appeal_status` tinyint(2) DEFAULT 0 COMMENT '异议状态(0无 1申诉中 2已处理 3已驳回)',
  `appeal_reason` varchar(500) DEFAULT NULL COMMENT '教师申诉理由',
  `appeal_reply` varchar(500) DEFAULT NULL COMMENT '教务处理回复',

  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专项绩效酬金明细表';

-- ==========================================================
-- 4. 结果汇总层 (动态 JSON + 审批流)
-- ==========================================================

-- 4.1 学期工作量与审批汇总表 (对应附件1和附件2的最终输出)
CREATE TABLE `biz_workload_summary` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '教师ID',
  `semester` varchar(20) NOT NULL COMMENT '学年学期',
  
  -- 宏观固定指标
  `quota_workload` decimal(10,2) NOT NULL COMMENT '本学期/学年额定基础工作量',
  `grand_total_workload` decimal(10,2) DEFAULT 0.00 COMMENT '核定总工作量 (包含所有类别)',
  `excess_workload` decimal(10,2) DEFAULT 0.00 COMMENT '超额工作量 (总额 - 额定)',
  `grand_total_allowance` decimal(10,2) DEFAULT 0.00 COMMENT '纯金钱专项总计(元)',
  
  -- 动态分类 JSON (应对分类扩展的核心)
  `category_details` json DEFAULT NULL COMMENT '各小类动态汇总JSON，如 {"G1": 150, "G8": 20, "G12": 15}',
  `group_details` json DEFAULT NULL COMMENT '各大类动态汇总JSON，如 {"TEACHING": 150, "ADMIN": 20, "EXTRA": 15}',
  
  -- 审批流转状态
  `audit_status` tinyint(2) DEFAULT 0 COMMENT '审核状态 (0:填报中 1:教务助理待审 2:院部领导待签 3:已完结 -1:驳回)',
  `assistant_sign` varchar(64) DEFAULT NULL COMMENT '教务助理签字(记录账号)',
  `leader_sign` varchar(64) DEFAULT NULL COMMENT '院部领导签字(记录账号)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_semester` (`user_id`,`semester`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期工作量审核与动态汇总表';
```



#### 表讲解

#### 1. 基础扩展层：`biz_teacher_profile` (定基调)

这表用来补足若依 `sys_user` 缺失的业务人事信息，它的核心功能是**“定门槛”**和**“定价”**。

- `title` / `education`: 决定了算绩效时乘的“单价”是多少。
- `min_workload`: 决定了该教师这学期要扣除多少免费学时，剩下的才能算超额发钱。
- `is_outsource` / `teacher_nature`: 决定计算逻辑的走向（外聘老师和专任老师的算钱通道往往不同）。

#### 2. 元数据配置层：`dict` 与 `rule` (系统的大脑)

这两张表**彻底告别在 Java 代码里写死 if-else**。

- **`biz_workload_category_dict` (字典表)**:
  - `calc_strategy`: 灵魂字段。它存的是 Spring Boot 里的 `@Service("xxx")` 名称。前端传个 G1 过来，Java 去这表里一查，就知道该去调哪段代码来算。
  - `is_calc_excess`: 如果教务处规定“某些杂活不算超额绩效”，改这个字段为 0，底层算钱时自动跳过。
- **`biz_workload_rule` (规则表)**:
  - `rule_value`: 存 1.1、0.9 这种极易变动的政策系数。明年改政策，后台改个数字就行，不用重新发版。

#### 3. 业务流水层：事件快照账本

这是数据量最大的地方。分为原始输入 (`task`) 和核算结果 (`workload`)。

- **`biz_teaching_task` (原始任务表)**:
  - 功能：承载教务处导出的原始排课 Excel。
  - 核心字段：`education_level`, `major_category`, `teaching_eval` 等。这些是 Java 策略类进行计算时必须要用到的前置判断条件（比如判断是本科还是专科，以此决定 C1 系数）。
- **`biz_teaching_workload` (教学明细流水表)**:
  - 功能：保存计算结果。这是一张**只能新增、不能随意修改的“小票表”**。
  - `base_value`: 核心基数（比如上了 32 节课）。
  - `coef_*` 系列: **历史快照**。算账那一刻用到的各种系数。如果未来规则变了，这行数据的学时依然不会错乱，因为算它的“证据”都被固化在这里了。
  - `appeal_*` 系列: 异议闭环。老师觉得这笔账不对，直接针对这条记录发起申诉，教务处在此行填入回复。
- **`biz_extra_workload` & `biz_allowance_record`**:
  - 功能：这俩是对教学明细的补充。`extra` 存直接给学时的工作（如当班主任给 180 学时）；`allowance` 存直接给钱的工作（如裁判费一天 30 元）。

#### 4. 结果汇总层：`biz_workload_summary` (动态容器与审批流)

这是系统的“输出大盘”，也是元数据驱动的终极体现。

- `category_details` (JSON): **全表最亮眼的设计**。它代替了传统的 `sum_g1`, `sum_g2` 这种死字段。后端只需将明细表按类别求和，塞入 `{ "G1": 100, "G8": 20 }` 即可。未来如果多出个 G13，表结构完全不用动，JSON 里多个键值对而已。
- `audit_status`, `*_sign`: 控制审批流。一条记录在这个学期最终“定稿”前，要在教师、教务助理、院领导之间流转，这个状态机就是锁死数据的关键。





## 注意事项总结（规则）

### 附件一

**教师总工作量=教学工作量+管理服务工作量**

#### 1.理论课

``` 理论计算公式
	理论课 G1(J1*C1*K1*Q1*Q2*N)
	 	理论课工作量 J1
	 	是否重复课 C1
	 	课程类型 K1
	 	教学质量系数 Q1
	 	课程质量系数 Q2
	 	合堂系数 N
?	 	非语言类全外文课程系数 Q3
```

```理论课相关解释
--C1 为本学期上课重复系数
第一次课C1＝1.0
第一次重复（即第二次课）C1＝0.9
以后C1＝0.8。（课程名称一致即为同一门课，不分年级，不以课程代码为准。本专科分别算。）

--K1 为课程类型系数：必修课K1＝1.1，选修课K1＝1.0。

--Q1 为教学质量系数：学期末对教师的教学质量评价等级为优秀和良好的Q1＝1.0，不合格的Q1＝0.8。

--Q2 为课程质量系数：
1.省级一流课程（含省级课程思政示范课）
主持人Q2=1.5、团队成员前3位Q2=1.2；
2.校级精品课程
主持人Q2=1.2、团队成员前3位Q2=1.1；
3.其他课程 Q2=1.0。

--N 为合堂系数：
合堂人数为120-150人，N=1.1；
合堂人数为151人及以上，N=1.2。
```

#### 2.课内实践学时、独立实验课、校内排课实训课

```
 课内实践学时、独立实验课、校内排课实训课G2(J2*K*C2*Q1*Q2)
 	实践课学时数J2
 	实践课课程系数K
 	实践课重复系数C2
```

```解释
--J2 为教学计划规定的实践课学时数。

--K 为实践课课程系数：理工类专业课程K＝1.0，其他专业的课程K=0.9。

--C2 为实践课重复系数：C2＝0.9。实验学时的上课轮次根据实验条件确定。
```

#### 3、教学实习、实训课（实践周、外出教学活动）

```
教学实习、实训课（实践周、外出教学活动）G3(T*D*K*Q1*Q2)
```

```
--T 为指导教学实习、实训的实际天数，一天按8学时计算，四舍五入保留一位小数；

--D 为教师指导一个自然班学生实习实训的系数；
（1）教师准备、指导学生实习、实训的，理工类专业D ＝4.0，艺术类专业D＝3.0，文史类专业D＝2.0；
（2）学生到实习单位实习、实训，由实习单位教师指导、讲解，我校教师参与实习、实训的，D＝2.0；
（3）同专业到同一实习单位实习、实训，原则上由一位教师承担，若必须由多位教师承担，须经教务处批准；

--K 为重复系数，第一轮K＝1，若同一位教师带不同批次的学生到同样的实习单位实习、实训
从第二轮次重复系数K＝0.9。
```

#### 4、课程设计

```
课程设计 G4(J4*R4*0.4)
	课程设计学分J4
	课程设计人数R4
```

```
--J4 为该课程设计的计划学分数；

--R4 为指导课程设计学生的人数，R4≤60。
一位教师同一时间只能辅导一个班的课程设计。
```

#### 5、毕业论文（设计）

```
指导毕业论文（设计）工作量  G5(R5*K5)
```

```
--R5 为指导毕业论文（设计）的学生人数
对于指导本科学生毕业论文（设计）
R5≤10时，按实际人数计算
当R5＞8时，须报院长批准，教务处备案。
对于指导专科学生毕业论文（设计）
R5≤15时，按实际人数计算
当R5＞15时，须报院长批准，教务处备案。

--K5 为系数
指导理工类本科毕业生毕业论文（设计）K5 ＝9，专科K5＝5；
指导文史类本科专业毕业论文（设计）K5 ＝6，专科K5＝4。
```

#### 6、集中实习（现场跟班指导）  

#### **毕业实习**1）分散实习 2）集中实习（不现场跟班指导）3）集中实习（现场跟班指导） )

```
集中实习G6（W*R6*0.4）
	实习周数W
	学生人数R6
```

```
--分散实习  指导教师酬金=指导学生人数×10元
--集中实习（不现场跟班指导）指导教师酬金=指导学生人数×15元
--集中实习（现场跟班指导） 工作量计算公式：G7=W×R7×0.4
			W为教师按计划指导学生实习的周数；
?			R7为指导实习的学生人数。
 注意事项很多，后续需要继续阅读指导
```

#### 7、第一课堂教学工作量

第一课堂工作量总计  :   G7(G1+G2+G3+G4+G5+G6)

#### 8、第二课堂工作量

第二课堂工作量G8

#### 9、其他方面

其他工作量G9

其他工作量说明

#### 10、教学工作量合计

G10(G7+G8+G9)

#### 11、管理服务工作量

管理服务工作量G11

管理服务工作量说明

#### 12、合计

总工作量(G10+G11)

#### 13、额定工作量

额定工作量

#### 超额定工作量（保留1位小数）

```

过程中需要 院（部）领导签字、教务助理审核签字

如学期进行中 有班主任变动或者职务变动，请务必标明任职时间:几月几日-几月几日
A、请各位老师仔细阅读《教师教育教学工作量绩效计算办法》
B、若文件里未说明的，请教务助理汇总后再咨询教务。
```

### 附件2

#### 重修辅导金A

```
（1）跟班重修课程工作量计算，教学班人数按照实际计
算系数；
（2）学生人数大于等于 20 人单独开班，按照教师基本
工作量标准计算；
（3）自学加辅导学生重修课程，按照人数单独计绩效酬金，
如下：
	①学生人数小于6人，不计工作量，绩效酬金120元；
	②学生人数大于等于6人小于20人，不计工作量，绩效酬金260元。
```

#### 指导毕业实习酬金B

```
分散实习：
	酬金=指导学生人数×10元
集中实习（不现场跟班指导）：
	酬金=指导学生人数×15元
```

#### 指导论文重修酬金C

```
酬金C = 毕业论文（设计）重修指导人数 x 120元
```

#### 代阅卷酬金D ???

```
（1）学生人数小于20，不计酬金；
（2）学生人数大于等于20小于60，酬金30元；
（3）学生人数大于等于60小于120，酬金80元；
（4）学生人数大于等于120小于200，酬金100元；
（5）学生人数大于等于200，酬金150元。
```

#### 讲座酬金E

```
酬金E = 时长(小时) x 60
```

#### 运动会裁判员酬金F

```
每天按6个工作量计算，工作量绩
效酬金依据单位工作量30元标准计算，直接计入酬金。体质健
康测试每班测试时间约2小时，按照工作量的50%计算，即一个
体测班计1个工作量。
```

#### 夜间值班酬金G

```
单位工作量按30元标准计算，工作量核算以潍理工酒店签字为依据。
```

#### 其他酬金

```
其他酬金=A+B+C+D+E+F+G
```









# 开发思路

### 系统的标准使用流程 (SOP)

结合这套数据库和若依的 RBAC 权限，系统在真实学校环境中的运转流程如下：

#### 阶段 1：期初建档与配置 (系统管理员 / 教务处)

1. **同步基础数据**：系统对接学校人事系统，或 Excel 批量导入，生成 `sys_user` 账号和 `biz_teacher_profile` 教师档案（录入张老师是讲师，额定 240 学时）。
2. **设置规则参数**：教务管理员进入【核算规则配置】菜单，将 `biz_workload_rule` 表中的合堂系数、重修单价等政策数值录入。
3. **配置字典**：确保 `biz_workload_category_dict` 中存在 G1-G11 的分类代码，并绑定好 Java 策略 Bean 名称。

#### 阶段 2：期中数据采集 (教务员 / 专任教师)

1. **教务批量导入 (G1~G6)**：教务员拿到教务排课系统的 Excel（包含几百门课），使用系统提供的【工作量导入】功能。
   - *后端动作*：逐行读取 Excel，匹配 `calc_strategy`，自动计算各种系数（如查重算出 C1），最终生成几百条记录落库到 `biz_teaching_workload`，并保存快照。
2. **教师自主申报 (G8, G9, G11)**：张老师登录自己的账号，进入【附加工作量申报】菜单，选择“班主任(G11)”，填入 180 学时。
   - *后端动作*：记录落库到 `biz_extra_workload`。

#### 阶段 3：期末结账与审批 (教务处 -> 教务助理 -> 院领导)

1. **触发核算**：期末，教务管理员点击【生成本学期汇总】。
   - *后端动作*：系统根据 `user_id` 和 `semester`，将三张流水表的数据按类别 `GROUP BY`，生成 JSON 塞入 `biz_workload_summary`。计算超额工作量和总绩效。状态变为 `0`（待审）。
2. **教务助理初审**：登录系统，查看本学院所有人的 `summary` 数据，确认无误后点击“通过”，状态变 `1`。
3. **院领导终审**：登录系统，进行电子签名确认，状态变 `2`（完结）。

#### 阶段 4：发钱与存档 (财务处 / 教师)

- 财务处一键导出“附件2：绩效酬金统计表”。
- 教师个人端查阅最终版“附件1：个人明细表”。

------

###  清晰的开发路线图 (Roadmap)

为了不让你在庞大的项目中迷失，我为你制定了从 0 到 1 的开发顺序（敏捷开发模式）：

#### 🎯 里程碑 1：环境搭建与基建 (1周)

- **目标**：跑通框架，生成基础 CRUD 代码。
- **任务**：
  1. 部署若依 Vue3 + Spring Boot 前后端分离版。
  2. 在 MySQL 中执行补齐了审计字段的最终版 SQL。
  3. 打开若依后台的【代码生成】模块，导入这 5 张 biz 表，一键生成所有的 Entity、Mapper、Service、Controller 以及前端 Vue 页面。
  4. 将生成的代码复制进你的项目中，确保后台能看到这些菜单并能进行简单的增删改查。

#### 🎯 里程碑 2：权限与前端骨架改造 (1周)

- **目标**：实现千人千面。
- **任务**：
  1. 在【角色管理】创建：教务管理员、教务助理、院领导、专任教师。
  2. 利用 `v-hasRole` 改造首页 `Index.vue`，实现教务大屏和教师个人工作台的分离（参考我们之前的代码）。
  3. 配置数据权限：让“专任教师”角色只能看到自己 `user_id` 的流水数据。

#### 🎯 里程碑 3：攻坚核心策略引擎 (后端核心) (2周)

- **目标**：彻底消灭 if-else，实现工作量动态计算。
- **任务**：
  1. 定义 `WorkloadCalcStrategy` 接口。
  2. 实现 `TheoryCalcStrategyImpl` (理论课算法，重点攻克历史同名课程查重，得出 C1 系数的逻辑)。
  3. 实现 `PracticeCalcStrategyImpl` (实践课算法) 等等。
  4. 编写一个统一的 `DispatcherService`，根据前端传来的类型，利用 Spring IOC 动态调用对应算法。

#### 🎯 里程碑 4：Excel 导入与聚合核算 (最复杂) (2周)

- **目标**：打通数据的输入和输出。
- **任务**：
  1. 引入 Alibaba `EasyExcel`，编写教务处排课表导入的 Listener。
  2. 在 Listener 中调用里程碑 3 写好的策略引擎，将 Excel 数据转化为 `biz_teaching_workload` 的明细落库。
  3. **编写期末汇总定时任务/接口**：使用 Mybatis 的聚合查询，或者 Java 的 Stream API，将明细按类型分组，转化为 JSON 字符串，保存到 `biz_workload_summary` 的 `category_details` 中。

#### 🎯 里程碑 5：审批流与复杂报表导出 (1周)

- **目标**：业务闭环。
- **任务**：
  1. 在汇总表管理页面，添加“审核通过/驳回”按钮，修改 `audit_status`。
  2. 利用 EasyExcel 的模板填充功能（Fill 功能），将 `summary` 里的 JSON 数据和明细数据，原汁原味地导出为学校规定的“附件1”和“附件2”格式。























新增大类 例如G1，G2，G3

新增小类 R，C1，C2，K

大类与小类所属关系 多对多

Excel表中显示大类

新增记录，内容包括大类id，小类id，数值记录
