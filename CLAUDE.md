# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

潍理工教学工作量智能化管理系统（WFIT Workload），基于 RuoYi-Vue3 前后端分离框架定制开发，用于高校教师教学工作量核算、汇总、绩效酬金计算。系统替代教师手动汇总、核对、填写工作量的传统模式，实现教务处→学院→教师的全流程数字化管理。

## 项目进度

| 里程碑 | 内容 | 状态 | 核心产出 |
|--------|------|------|----------|
| M1 | 环境搭建与基建 | ✅ 已完成 | RuoYi-Vue3 部署、18 表 DDL、种子数据、代码生成器 CRUD |
| M2 | 权限与前端骨架改造 | ✅ 已完成 | 19 个菜单、按钮权限、Admin/Teacher 仪表盘 |
| M3 | 核心策略引擎（G1-G11） | ✅ 已完成 | 7 个策略 bean、汇总服务、酬金服务、G11 生成器 |
| M4 | Excel 导入与教师申报 | ✅ 已完成 | EasyExcel 导入、教师自主申报（declare.vue）、数据闭环 |
| M5 | 审批流与报表导出 | ✅ 已完成 | 三级审批状态机、附件1/2 报表导出、批量提交审核 |

**整体完成度：100%**（核心功能全部落地，剩余为优化和完善）

## 技术栈

- **后端**: Spring Boot 4.0.7 / Java 17 / Maven 多模块 / MyBatis / Druid / Spring Security + JWT
- **前端**: Vue 3.5 + Vite 6 + Element Plus 2.13 + Pinia 3 + Vue Router 4
- **数据库**: MySQL 8 (库名 `wflg_workload`)，Redis (缓存 + 会话)
- **Excel**: Apache POI 5.5.1（导入导出）
- **API 文档**: Springdoc OpenAPI (Swagger UI at `/swagger-ui.html`)

## 目录结构

```
wfit/
├── WFIT_workload/
│   ├── rear/                          # 后端 Maven 多模块
│   │   ├── workload-admin/            # 启动入口 + 系统管理 Controller (端口 8084)
│   │   ├── workload-system/           # 业务核心模块
│   │   │   ├── src/**/calc/           # 核算引擎
│   │   │   │   ├── strategy/          # G1-G11 策略 bean（7 个 StrategyImpl）
│   │   │   │   ├── rule/              # 规则参数服务（RuleParamService, Redis 缓存）
│   │   │   │   ├── allowance/         # 酬金计算（PayCalcService, A~G 费率）
│   │   │   │   ├── WorkloadCalcService    # 单条/批量重算
│   │   │   │   ├── SummaryCalcService     # 学期汇总计算
│   │   │   │   ├── PayCalcService         # 酬金计算
│   │   │   │   ├── ManagementItemGenerator# G11 自动生成
│   │   │   │   └── SemesterCalendar       # 学期校历工具
│   │   │   ├── src/**/controller/     # 22 个业务 Controller（Biz*Controller）
│   │   │   ├── src/**/service/        # 18+ 个业务 Service
│   │   │   ├── src/**/mapper/         # 18 个 MyBatis Mapper 接口
│   │   │   └── src/**/domain/         # 18 个实体类
│   │   ├── workload-framework/        # 框架层：Security、数据源、AOP、配置
│   │   ├── workload-common/           # 通用工具、注解、异常处理
│   │   ├── workload-quartz/           # 定时任务模块
│   │   ├── workload-generator/        # 代码生成器
│   │   └── sql/                       # 建表 + 种子数据 + 计算规则 + 菜单
│   │       ├── 01_biz_schema.sql      # 18 张业务表 DDL
│   │       ├── 02_biz_seed.sql        # 种子数据（字典11条、规则39条、费率4条）
│   │       ├── 03_calc_rules.sql      # 补充规则（G11 封顶、酬金 A-G 费率）
│   │       ├── 04_biz_test_data.sql   # 测试数据（6教师+9教学任务+12明细+4汇总）
│   │       └── 05_biz_menu.sql        # 业务菜单 SQL（19 子菜单 + 按钮权限）
│   └── front/RuoYi-Vue3/             # 前端 Vue 3 项目
│       └── src/
│           ├── api/system/            # 31 个业务 API 文件
│           ├── views/system/          # 19 个业务页面
│           │   ├── teacherProfile/    # 教师档案
│           │   ├── teachingTask/      # 教学任务
│           │   ├── roleAssignment/    # 岗位任职
│           │   ├── importBatch/       # 数据导入批次
│           │   ├── workloadItem/      # 工作量明细主表
│           │   ├── wlTheory/          # G1 理论课
│           │   ├── wlPractice/        # G2 课内实践
│           │   ├── wlInternshipTraining/ # G3 实习实训
│           │   ├── wlCourseDesign/    # G4 课程设计
│           │   ├── wlThesis/          # G5 毕业论文
│           │   ├── wlConcentratedInternship/ # G6 集中实习
│           │   ├── wlManagement/      # G11 管理服务
│           │   ├── workloadSummary/   # 学期汇总（含审批按钮）
│           │   ├── payRecord/         # 酬金记录
│           │   ├── allowanceItem/     # 其他酬金明细
│           │   ├── myWorkload/        # 教师自主申报（declare.vue）
│           │   ├── workloadCategoryDict/ # 类别字典
│           │   ├── workloadRule/      # 计算规则
│           │   └── payRate/           # 酬金标准
│           └── views/dashboard/       # 仪表盘
│               ├── AdminDashboard.vue # 管理员大屏（4统计+ECharts+待办）
│               └── TeacherDashboard.vue # 教师工作台（数据卡+明细+达标面板）
├── else/                              # 原始需求文档、管理办法
│   ├── 工作量.md                      # 业务需求权威来源（G1-G11 公式）
│   ├── 潍理工工作量管理系统设计new).md  # 系统设计文档（E-R图+SOP+路线图）
│   ├── 实施计划-M4M5-2026-07-24.md    # M4/M5 实施计划
│   └── 进度报告-2026-07-24.md         # 进度报告
├── docs/                              # 开发计划与设计规范
│   └── superpowers/
│       ├── plans/                     # 实施计划（M1/M3）
│       └── specs/                     # 数据库设计规范（权威 v2）
└── CLAUDE.md                          # 本文件
```

## 常用命令

### 后端 (在 `WFIT_workload/rear/` 目录下)

```bash
# 编译打包 (跳过测试)
mvn clean package -DskipTests

# 仅编译指定模块
mvn clean compile -pl workload-system -am

# 运行 (主入口 workload-admin)
java -jar workload-admin/target/workload-admin.jar

# 或直接 Maven 启动
mvn spring-boot:run -pl workload-admin
```

### 前端 (在 `WFIT_workload/front/RuoYi-Vue3/` 目录下)

```bash
# 安装依赖
npm install --registry=https://registry.npmmirror.com

# 开发服务器 (默认 http://localhost:80)
npm run dev

# 生产构建
npm run build:prod
```

### 数据库初始化

```bash
# 按顺序执行 SQL
mysql -u root -p wflg_workload < rear/sql/01_biz_schema.sql
mysql -u root -p wflg_workload < rear/sql/02_biz_seed.sql
mysql -u root -p wflg_workload < rear/sql/03_calc_rules.sql
mysql -u root -p wflg_workload < rear/sql/04_biz_test_data.sql
mysql -u root -p wflg_workload < rear/sql/05_biz_menu.sql
```

## 配置要点

- 后端端口: `8084` (application.yml)
- 数据库: `127.0.0.1:3306/wflg_workload`，用户 `root`，密码 `123456` (application-druid.yml)
- Redis: `localhost:6379`，无密码
- 文件上传路径: `rear/uploadPath/`
- 前端 API 代理: 开发环境默认代理到 `http://localhost:8084`
- 学期校历: `application.yml` 的 `wl.semester` 节点

## 业务核心：工作量核算公式

系统核心是按课程类型计算教学工作量，公式定义在 `else/工作量.md` 和 `03_calc_rules.sql` 中：

| 类型 | 公式 | 说明 |
|------|------|------|
| G1 理论课 | `J1 * C1 * K1 * Q1 * Q2 * Q3 * N` | J1=计划学时, C1=重复系数, K1=必修1.1/选修1.0, Q1/Q2=质量, N=合堂 |
| G2 实践课 | `J2 * K * C2 * Q1 * Q2 * Q3` | J2=实践学时, K=理工1.0/其他0.9, C2=0.9 |
| G3 实习实训 | `T * D * K * Q1 * Q2 * Q3` | T=实际天数(×8学时), D=理工4/艺术3/文史2 |
| G4 课程设计 | `J4 * min(R4,20) * 0.4` | J4=学分, R4=人数(上限20) |
| G5 毕业论文 | `R5 * K5` | K5=理工本9/专5, 文史本6/专4 |
| G6 集中实习 | `W * min(R6,20) * 0.4` | W=周数, R6=人数(上限20) |
| G11 管理服务 | 按岗位标准学时 * 任职天数/学期天数 | 学期封顶 180 |
| 绩效酬金 | `(min(总工作量,540) - 180) * 职称单位酬金` | 教授70/副60/讲50/助40 |

汇总层级：G7=G1~G6合计, G10=G7+G8+G9, 总工作量=G10+G11

## 计算引擎架构

核心是**策略模式 + Spring IOC 动态分发**，彻底消灭 if-else：

1. `WorkloadCalcStrategy` 接口 — 统一计算入口
2. 7 个策略实现 — Theory/Practice/InternshipTraining/CourseDesign/Thesis/ConcentratedInternship/Management
3. `DispatcherService` — 根据 `category_dict.calc_strategy` 字段动态调用对应策略
4. `RuleParamService` — 规则参数读取（Redis 缓存），政策变动改数据库即可
5. `SummaryCalcService` — 学期汇总 → JSON 动态分类
6. `PayCalcService` — 酬金 A~G 计算
7. `ManagementItemGenerator` — 自动从 roleAssignment 生成 G11 条目

**API 端点**（`BizCalcController`，路径 `/system/calc/*`）：

| 端点 | 方法 | 功能 |
|------|------|------|
| `/recalcItem/{id}` | POST | 单条工作量重算 |
| `/recalcItems` | POST | 批量重算 |
| `/recalcSummary` | POST | 学期汇总重算 |
| `/recalcPay` | POST | 酬金计算 |
| `/preview` | GET | 预览汇总数据 |
| `/genG11` | POST | 自动生成管理服务条目 |
| `/recalcAll` | POST | 全量重算（需 userId） |

## 审批流状态机

```
0: 填报中 (教师/教务员可编辑)
  ↓ 教务员提交
1: 教务助理待审 (教务助理可审核)
  ↓ 教务助理通过/驳回
2: 院领导待签 (院领导可签字)
  ↓ 院领导签字
3: 已完结 (锁定，不可修改)
  ↓
-1: 驳回 (回到填报中，可修改后重新提交)
```

## 数据库分层架构（18 张 biz 表）

| 层级 | 表名 | 说明 |
|------|------|------|
| 支撑层 | biz_teacher_profile | 教师档案（职称、类别、额定学时） |
| 支撑层 | biz_workload_category_dict | 类别字典（G1-G11，绑定策略 Bean） |
| 支撑层 | biz_workload_rule | 核算规则参数（39 条，Redis 缓存） |
| 支撑层 | biz_pay_rate | 酬金费率（教授70/副60/讲50/助40） |
| 支撑层 | biz_import_batch | 导入批次记录 |
| 源数据层 | biz_teaching_task | 教学任务（Excel 导入的原始数据） |
| 源数据层 | biz_role_assignment | 岗位任职（生成 G11 的依据） |
| 计算明细层 | biz_workload_item | 工作量明细主表 |
| 计算明细层 | biz_wl_theory | G1 理论课明细 |
| 计算明细层 | biz_wl_practice | G2 实践课明细 |
| 计算明细层 | biz_wl_internship_training | G3 实习实训明细 |
| 计算明细层 | biz_wl_course_design | G4 课程设计明细 |
| 计算明细层 | biz_wl_thesis | G5 毕业论文明细 |
| 计算明细层 | biz_wl_concentrated_internship | G6 集中实习明细 |
| 计算明细层 | biz_wl_management | G11 管理服务明细 |
| 汇总层 | biz_workload_summary | 学期汇总（含审批状态、JSON 动态分类） |
| 酬金层 | biz_pay_record | 酬金汇总记录 |
| 酬金层 | biz_allowance_item | 其他酬金明细（A-G） |

## RuoYi 框架约定

本项目继承 RuoYi-Vue3 框架的代码规范：

- **Controller**: 返回 `AjaxResult` 或 `TableDataInfo`，分页用 `startPage()` + `getDataTable()`
- **Service**: 接口 `IXxxService` + 实现 `XxxServiceImpl`，事务注解在实现类
- **Mapper**: 接口 + XML 映射文件，XML 在 `resources/mapper/` 下
- **实体**: 继承 `BaseEntity`（含 createBy/createTime/updateBy/updateTime/remark）
- **权限**: 注解 `@PreAuthorize("@ss.hasPermi('system:xxx:list')")`，权限标识 `模块:实体:操作`
- **日志**: `@Log(title = "xxx", businessType = BusinessType.INSERT)` 操作日志注解
- **导入导出**: 实体字段加 `@Excel` 注解，配合 POI

## 已知问题与待办

| # | 问题 | 优先级 | 说明 |
|---|------|--------|------|
| 1 | collegeStats 返回空 | 中 | dept 数据未正确关联 teacher_profile |
| 2 | G8/G9 策略为空 | 低 | 第二课堂/其他工作量暂无自动计算逻辑，直接录金额 |
| 3 | 代阅卷酬金 D 档位 | 低 | 待正式文件确认，首期不启用 |
| 4 | Q3 全外文课程系数 | 低 | 取值待外部门文件确认 |
| 5 | G5 艺术类 K5 映射 | 低 | 暂按文史类处理 |

## 注意事项

- 当前存在两套前端（Vue2 在 `rear/workload-ui` 已弃用，Vue3 在 `front/RuoYi-Vue3` 为活跃版本）
- 学期校历配置在 `application.yml` 的 `wl.semester` 节点，用于任职折算和特殊状态判定
- 业务表前缀 `biz_`，系统表前缀 `sys_`（RuoYi 内置）
- `.env.*` 文件包含敏感配置，已被 git 跟踪需注意
- G11 管理服务条目由 `ManagementItemGenerator` 从 `biz_role_assignment` 自动生成，也可手动录入
- 汇总表 `biz_workload_summary` 使用 JSON 字段 `category_details` 存储动态分类汇总，扩展新类别无需改表结构
