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
- **文档**: `docs/API接口文档.md`（22 个 Controller、121 个端点）、`docs/代码审查报告.md`（33 个问题修复记录）
- **Excel**: Apache POI 5.5.1（导出）+ EasyExcel 4.0.3（流式导入，见 A14/B1 整改）
- **API 文档**: Springdoc OpenAPI (Swagger UI at `/swagger-ui.html`)

## 目录结构

> 注意：`rear/` 与 `front/` 位于仓库根目录（即本文件所在目录），**无**额外嵌套。

```
wfit--workload/                         # 仓库根（GitHub: qingzhizhu517-rgb/wfit--workload）
                                       # 本机检出路径: /Users/a1/Desktop/wfit/WFIT_workload（macOS）
                                       # 历史开发环境: WSL /home/aohs/vibecoding/wfit--workload
├── rear/                              # 后端 Maven 多模块（group=com.workload）
│   ├── workload-admin/                # 启动入口 + 系统管理 Controller (端口 8084)
│   ├── workload-system/               # 业务核心模块
│   │   ├── src/main/**/calc/          # 核算引擎
│   │   │   ├── strategy/              # G1-G11 策略 bean（7 个 Strategy + CalcStrategyFactory + StrategyCache）
│   │   │   ├── rule/                  # 规则参数服务（RuleParamService, Redis 缓存）
│   │   │   ├── allowance/             # 其他酬金策略（AllowanceA/B/C/E/F/G + StrategyFactory，D 未注册）
│   │   │   ├── WorkloadCalcService    # 单条/批量/全量重算
│   │   │   ├── SummaryCalcService     # 学期汇总计算
│   │   │   ├── PayCalcService         # 酬金计算
│   │   │   ├── ManagementItemGenerator# G11 自动生成
│   │   │   └── SemesterCalendar       # 学期校历工具
│   │   ├── src/main/**/domain/dto/    # 导入导出 DTO
│   │   ├── src/main/**/controller/    # 22 个业务 Controller（Biz*Controller）
│   │   ├── src/main/**/service/       # 业务 Service
│   │   ├── src/main/**/mapper/        # MyBatis Mapper 接口
│   │   ├── src/main/**/domain/        # 实体类
│   │   └── src/test/**/calc/strategy/ # JUnit 5 单元测试（CalcStrategyFactoryTest, StrategyCacheTest）
│   ├── workload-framework/            # 框架层：Security、数据源、AOP、配置
│   ├── workload-common/               # 通用工具、注解、异常处理
│   ├── workload-quartz/               # 定时任务模块
│   ├── workload-generator/            # 代码生成器
│   ├── manage/                        # 空壳占位（仅 hello-world Main.java，无业务）
│   └── sql/                           # 建表 + 种子数据 + 计算规则 + 菜单
│       ├── 01_biz_schema.sql          # 18 张业务表 DDL
│       ├── 02_biz_seed.sql            # 种子数据（字典11条、规则39条、费率4条）
│       ├── 03_calc_rules.sql          # 补充规则（G11 封顶、酬金 A-G 费率）
│       ├── 04_biz_test_data.sql       # 测试数据（6教师+9教学任务+12明细+4汇总）
│       ├── 05_biz_menu.sql            # 业务菜单 SQL（19 子菜单 + 按钮权限）
│       ├── 06_test_accounts.sql       # 测试账号
│       ├── 07_fix_test_data.sql       # 测试数据修复脚本
│       ├── 08_review_fixes.sql        # 代码审查修复补丁（含第 19 张表 biz_audit_log）
│       ├── 09_dashboard_perm.sql      # 仪表盘权限登记
│       ├── 10_item_role_type.sql      # 岗位类型字段补充
│       ├── 11_fix_duplicate_rules.sql # 规则去重修复（幂等，可重复执行）
│       ├── 12_fix_dept_mapping.sql    # sys_dept 数据补充（修复 collegeStats 空返回）
│       ├── 13_fix_audit_perm.sql      # 撤销教务助理 unlock 越权 + 院领导授 reject（幂等）
│       ├── 14_fix_calc_rules.sql      # G4 人数上限 CAP_R4_MAX 20→60（幂等 UPDATE）
│       ├── 15_fix_menu_buttons.sql    # 补 63 个明细/配置页按钮权限 + 清理死权限（幂等，已并入 05/06）
│       ├── ry_20260321.sql            # RuoYi 基础系统表
│       └── quartz.sql                 # Quartz 调度器表
├── front/RuoYi-Vue3/                  # 前端 Vue 3 项目
│   └── src/
│       ├── api/system/                # 31 个业务 API 文件
│       ├── views/system/              # 19 个业务页面（含 myWorkload/declare.vue 教师自主申报）
│       └── views/dashboard/           # 仪表盘
│           ├── AdminDashboard.vue     # 管理员大屏（4统计+ECharts+待办）
│           ├── TeacherDashboard.vue   # 教师工作台（数据卡+明细+达标面板）
│           ├── JiaoWuDashboard.vue    # 教务助理工作台
│           └── LeaderDashboard.vue    # 院领导工作台
├── else/                              # 原始需求文档、管理办法
│   ├── 工作量.md                      # 业务需求权威来源（G1-G11 公式）
│   ├── 潍理工工作量管理系统设计new).md  # 系统设计文档（E-R图+SOP+路线图）
│   ├── 实施计划-M4M5-2026-07-24.md    # M4/M5 实施计划
│   └── 进度报告-2026-07-24.md         # 进度报告
├── docs/                              # 开发文档
│   ├── API接口文档.md                  # 完整 API 参考（22 Controller、121 端点）
│   ├── 代码审查报告.md                  # 代码审查报告（33 个问题修复记录）
│   ├── 测试.md                        # 测试文档
│   ├── api-test-report.md             # API 测试报告
│   └── superpowers/
│       ├── plans/                     # 实施计划（M1/M3）
│       └── specs/                     # 数据库设计规范（权威 v2）
└── CLAUDE.md                          # 本文件
```

## 常用命令

### 后端 (在 `rear/` 目录下)

```bash
# 编译打包 (跳过测试)
mvn clean package -DskipTests

# 仅编译指定模块
mvn clean compile -pl workload-system -am

# 运行单元测试（仅 workload-system 模块有测试）
mvn test -pl workload-system
# 运行单个测试类
mvn test -pl workload-system -Dtest=CalcStrategyFactoryTest

# 运行 (主入口 workload-admin)
java -jar workload-admin/target/workload-admin.jar

# 或直接 Maven 启动
mvn spring-boot:run -pl workload-admin
```

### 前端 (在 `front/RuoYi-Vue3/` 目录下)

```bash
# 安装依赖
npm install --registry=https://registry.npmmirror.com

# 开发服务器 (端口 3000，见 vite.config.js)
npm run dev

# 生产构建
npm run build:prod

# Lint（ESLint，检查 .js/.vue）
npm run lint          # 仅检查
npm run lint:fix      # 检查并自动修复
```

- API 代理：开发环境所有 `/dev-api` 前缀请求会代理到 `http://localhost:8084`（去掉前缀）
- Swagger UI 代理：`/v3/api-docs/*` 直接透传到后端

### 数据库初始化

```bash
# 基础表（先执行 RuoYi 系统表）
mysql -u root -p wflg_workload < rear/sql/ry_20260321.sql
mysql -u root -p wflg_workload < rear/sql/quartz.sql

# 按顺序执行业务 SQL
mysql -u root -p wflg_workload < rear/sql/01_biz_schema.sql
mysql -u root -p wflg_workload < rear/sql/02_biz_seed.sql
mysql -u root -p wflg_workload < rear/sql/03_calc_rules.sql    # 非幂等，仅执行一次
mysql -u root -p wflg_workload < rear/sql/04_biz_test_data.sql
mysql -u root -p wflg_workload < rear/sql/05_biz_menu.sql

# 角色 + 四端测试账号
mysql -u root -p wflg_workload < rear/sql/06_test_accounts.sql
# 必须：biz_audit_log 审计表仅在此创建（01 未包含）
mysql -u root -p wflg_workload < rear/sql/08_review_fixes.sql
# 必须：撤销教务助理 unlock 越权 + 院领导补授 reject
mysql -u root -p wflg_workload < rear/sql/13_fix_audit_perm.sql

# 可选：部门名称学院化（100→潍理工学院、103→信息工程学院、105→经济管理学院）
mysql -u root -p wflg_workload < rear/sql/12_fix_dept_mapping.sql

# 存量库可选：补 63 个明细/配置页按钮权限 + 清理死权限（新库无需，已并入 05/06）
mysql -u root -p wflg_workload < rear/sql/15_fix_menu_buttons.sql
```

> 07/09/10/11/14/15 的修复内容已分别并入 04/05/01/02/05+06 基础脚本，新库无需执行（脚本均幂等，执行亦无害）；08 与 13 为新库必需。详见 README「数据库初始化」。

## 测试

- **后端**：`workload-system` 模块引入了 JUnit 5 + Mockito + AssertJ，现有单元测试位于 `src/test/**/calc/strategy/`（`CalcStrategyFactoryTest`、`StrategyCacheTest`），运行 `mvn test -pl workload-system`。其余模块**无**测试；无 `spring-boot-starter-test`，故没有集成测试。
- **手动验证**：启动后端 + 前端，通过 Swagger UI (`/swagger-ui.html`) 或前端页面测试。
- **API 冒烟**：根目录 `test_api.sh` 提供 12 项 curl 冒烟测试，需先启动后端并关闭验证码（`UPDATE sys_config SET config_value='false' WHERE config_key='sys.account.captchaEnabled'`）。

## 配置要点

- 后端端口: `8084` (application.yml)
- 数据库: `application-druid.yml` 中默认写 `172.19.80.1:3306/wflg_workload`（历史 WSL 桥接 IP），用户 `root`，密码 `123456`。**当前活跃开发环境为 Windows 本机，需将 host 改为 `127.0.0.1`**（`.mcp.json` 的 MySQL MCP 已指向 `127.0.0.1:3306`）
- Redis: `localhost:6379`，无密码
- 文件上传路径: `rear/uploadPath/`
- 前端开发端口: `3000` (vite.config.js)
- 前端 API 代理: 开发环境 `/dev-api` 前缀请求代理到 `http://localhost:8084`（去掉前缀）
- 学期校历: `application.yml` 的 `wl.semester` 节点（秋季 09-01~01-31，春季 02-20~07-15）
- MySQL MCP: 仓库根 `.mcp.json` 注册了 `mysql` MCP server（`127.0.0.1:3306/wflg_workload`），可直接用 MCP 工具查库/看表结构，无需手写 `mysql` CLI

### 测试账号（密码均为 `123456`，来自 `06_test_accounts.sql`）

| 账号 | user_id | 角色 | 说明 |
|------|---------|------|------|
| `admin_test` | 1001 | admin (role_id=1) | 管理员，全部权限 |
| `jiaowu_test` | 1002 | assistant (role_id=3) | 教务助理，审批权限 |
| `teacher_test` | 1003 | teacher (role_id=4) | 教师，仅看自己的数据 |
| `leader_test` | 1004 | leader (role_id=5) | 院领导，签字权限 |

## 业务核心：工作量核算公式

系统核心是按课程类型计算教学工作量，公式定义在 `else/工作量.md` 和 `03_calc_rules.sql` 中：

| 类型 | 公式 | 说明 |
|------|------|------|
| G1 理论课 | `J1 * C1 * K1 * Q1 * Q2 * Q3 * N` | J1=计划学时, C1=重复系数, K1=必修1.1/选修1.0, Q1/Q2=质量, N=合堂 |
| G2 实践课 | `J2 * K * C2 * Q1 * Q2 * Q3` | J2=实践学时, K=理工1.0/其他0.9, C2=0.9 |
| G3 实习实训 | `T * D * K * Q1 * Q2 * Q3` | T=实际天数(×8学时), D=理工4/艺术3/文史2 |
| G4 课程设计 | `J4 * min(R4,60) * 0.4` | J4=学分, R4=人数(上限60，见 A6/14_fix_calc_rules.sql) |
| G5 毕业论文 | `R5 * K5` | K5=理工本9/专5, 文史本6/专4 |
| G6 集中实习 | `W * min(R6,20) * 0.4` | W=周数, R6=人数(上限20) |
| G11 管理服务 | 按岗位标准学时 * 任职天数/学期天数 | 学期封顶 180 |
| 绩效酬金 | `(min(总工作量,540) - 180) * 职称单位酬金` | 教授70/副60/讲50/助40 |

汇总层级：G7=G1~G6合计, G10=G7+G8+G9, 总工作量=G10+G11

## 计算引擎架构

核心是**策略模式 + Spring IOC 动态分发**，彻底消灭 if-else（包路径 `com.workload.system.calc`）：

1. `WorkloadCalcStrategy` 接口 — 统一计算入口（`getTypeCode()`、`calculate()`、`afterCalculated()` default 回调，G5/G6 用它置 `is_over_limit` 超标标记）
2. 7 个策略实现（`@Component("bean名")`，bean 名必须与 `calc_strategy` 列精确一致）：

   | typeCode | bean 名 | 类 |
   |----------|---------|-----|
   | G1 | `theoryCalcStrategy` | TheoryCalcStrategy |
   | G2 | `practiceCalcStrategy` | PracticeCalcStrategy |
   | G3 | `internshipTrainingCalcStrategy` | InternshipTrainingCalcStrategy |
   | G4 | `courseDesignCalcStrategy` | CourseDesignCalcStrategy |
   | G5 | `thesisCalcStrategy` | ThesisCalcStrategy |
   | G6 | `concentratedInternshipCalcStrategy` | ConcentratedInternshipCalcStrategy |
   | G11 | `managementCalcStrategy` | ManagementCalcStrategy |

3. `CalcStrategyFactory` — `@Autowired Map<String,WorkloadCalcStrategy>` 按 bean 名注入；查 `biz_workload_category_dict.calc_strategy` 列得 bean 名后 `strategyMap.get(name)` 解析，结果缓存在 `ConcurrentHashMap`（`StrategyCache`）。**聚合类别 G7/G8/G9/G10 无策略，返回 null**
4. `RuleParamService` — 规则参数读取（Redis 缓存），政策变动改数据库即可
5. `SummaryCalcService` — 学期汇总 → `biz_workload_summary.category_details` JSON 动态分类
6. `PayCalcService` + `allowance/AllowanceStrategyFactory`（`@Autowired List<AllowanceCalcStrategy>` 按 `getFeeType()` 建 map）— 其他酬金 A/B/C/E/F/G 六个策略；**D（代阅卷）未注册，首期不启用**
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

## 数据库分层架构（19 张 biz 表）

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
| 审计层 | biz_audit_log | 审批流审计日志（**在 `08_review_fixes.sql` 创建，不在 01 schema**） |

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

### 本轮审计已修复（2026-08-21）

| # | 问题 | 修复 |
|---|------|------|
| A1 | 教务助理(role3)被越权授予 `unlock`，可复活院领导已完结记录 | ✅ `13_fix_audit_perm.sql` 撤销授权，unlock 仅授管理员 |
| A2 | 策略解析失败（bean 名配错）静默返回 null，工作量被无声置 0 | ✅ `CalcStrategyFactory.resolve()` 改为抛 `ServiceException` |
| A3 | G11 折算多除了一个 2，与公式/种子数据/封顶矛盾 | ✅ `ManagementItemGeneratorImpl` 删除 `divide(2)`（rate 定性为学期标准） |
| A4 | 教学任务导入自调用致 `@Transactional` 失效，部分失败提交半截数据 | ✅ 改用 `AopContext.currentProxy()` 每行独立事务 |
| A5 | 院领导待签(2)环节无驳回路径 | ✅ `BizAuditServiceImpl.reject` 放开 `from∈{1,2}`；院领导授 `reject` 权限 |
| A6 | G4 人数上限 20 与权威文档 R4≤60 冲突 | ✅ `CourseDesignCalcStrategy` 默认值改 60；`14_fix_calc_rules.sql` UPDATE 已部署库 |
| A7 | 自学辅导 ≥20 人错算 260 元 | ✅ `AllowanceAStrategy` 增加 `count≥20` 走手工金额分支 |
| A8 | 导入批次 status 语义与表定义错位 | ✅ 对齐为 2=已导入 / 4=失败 |
| A9 | 学期格式无校验，脏数据入库 | ✅ `validateRow` 增加 `^\d{4}-\d{4}-[12]$` 正则 |
| A10 | AdminDashboard 图表切换传字符串致 `TypeError` | ✅ 缓存 `lastCollegeData`，切换时无参重绘 |
| A11 | 通用 `PUT /system/workloadSummary` 可直写 status（状态机后门） | ✅ edit 对所有角色剔除 status/签字/lock_time 字段；已完结(3)记录整体拒改 |
| A12 | 申报明细提交后不冻结，教师可改在审数据 | ✅ `BizWorkloadItemController.assertItemEditable`：教师 add/edit/remove 前校验 summary.status==0 |
| A13 | `assertOwnOrAdmin` 硬编码 `userId==1` 误拦业务管理员 | ✅ `DataScopeUtil` 改为 `!isTeacherOnly()` 按角色判定，与 `resolveUserId` 对称 |
| A14 | 教学任务导入全量入内存，大文件 OOM | ✅ 改流式 `importTeachingTasksStreaming` + `ExcelReadUtil.readEachRow` 逐行入库 |

### code-review 二轮修复（2026-08-22，修 A11/A14 引入的回归）

| # | 问题 | 修复 |
|---|------|------|
| B1 | 流式导入丢弃 `ExcelReadUtil` 返回值，单元格解析错误被静默吞掉（数据丢失） | ✅ `ExcelImportListener` 新增逐行模式，`onException` 与业务异常共用同一 `ImportResult` |
| B2 | 从待签(2)驳回残留 `academic_assistant_sign`/`teacher_sign`，草稿态显示已签 | ✅ `rejectSummary` mapper 回填时清空全部签字字段 + `lock_time` |
| B3 | `assertItemEditable` 省略 semester 可绕过冻结（fail-open） | ✅ 改 fail-closed：教师缺 userId/semester 直接抛异常 |
| B4 | 工厂改抛异常后，导入 `createGxDetail` 手动兜底成死代码且逻辑矛盾 | ✅ 删除 6 个手动兜底分支，统一走策略；配错即该行报错（fail-loud 一致） |
| B5 | 自学辅导 ≥20 人且金额空时静默发 0 | ✅ 金额为空抛异常，不再按 0 发放 |
| B6 | 流式错误行号跳过行后错位 | ✅ 随 B1 改用 EasyExcel 物理行号（`readRowHolder().getRowIndex()`） |

### 权限专项修复（2026-08-26，见 docs/Bug排查报告-2026-08-26.md）

| # | 问题 | 修复 |
|---|------|------|
| C1 | 7 张 G 明细页 + 6 个配置页的 add/edit/query/remove/export 按钮权限串未在 sys_menu 登记（后端注解/前端指令齐全，唯 SQL 缺失），非超管角色按钮不渲染且接口 403 | ✅ `15_fix_menu_buttons.sql` 登记 63 个 F 型按钮并授 role1/2/3；同步并入 `05_biz_menu.sql`/`06_test_accounts.sql` |
| C2 | 审批 `submit`/`batchSubmit` 无归属校验，教师可提交他人汇总（横向越权） | ✅ `BizAuditServiceImpl.submit()` 增加 `DataScopeUtil.assertOwnOrAdmin(summary.getUserId())`，与 teacherConfirm 对齐 |
| C3 | 菜单 20301 登记 `system:calc:recalc` 为死权限（无代码引用） | ✅ perms 改为 BizCalcController 实际使用的 `system:workloadItem:edit` |

### 待办（未处理，需排期/决策）

| # | 问题 | 优先级 | 说明 |
|---|------|--------|------|
| 1 | ~~collegeStats 返回空~~ | ✅ 已修复 | 执行 `12_fix_dept_mapping.sql` 补充 sys_dept 数据 |
| 2 | G8/G9 策略为空 | 低 | 设计如此：第二课堂/其他工作量为手动录入金额 |
| 3 | 代阅卷酬金 D 档位 | 低 | 待正式文件确认，首期不启用 |
| 4 | Q3 全外文课程系数 | 低 | 取值待外部门文件确认 |
| 5 | G5 艺术类 K5 映射 | 低 | 暂按文史类处理，待艺术类专业目录确认 |
| 6 | `assertOwnOrAdmin` 豁免范围偏宽（`!isTeacherOnly`） | 低 | 已裁决保留：现有角色仅 admin/biz_admin/assistant/leader/teacher，除教师外均应豁免且先过 `@PreAuthorize`；彻底收严需引入显式管理角色白名单，属权限模型改造 |
| 7 | 策略缓存 `StrategyCache.evict/clear` 无调用方 | 低 | 字典改绑策略后缓存永久陈旧到重启，建议在字典增改删 Service 调 evict |
| 8 | 教师账号默认弱口令 123456 无强制改密 | 低 | 密码已加密入库，但初始弱口令建议强制首次改密 |
| 9 | 200%/540 封顶边界 `>` vs `>=`、`teacherNature` 为 null 当专任发绩效 | 低 | 需业务确认口径，非明确 bug |

## 注意事项

- 活跃前端只有 `front/RuoYi-Vue3`（Vue 3）；历史上的 Vue2 `workload-ui` 已移除，不在仓库内
- `rear/manage/` 模块是空壳占位（仅有 hello-world 的 `Main.java`），无业务逻辑
- 学期格式为 `2025-2026-1`（学年+学期号），校历配置在 `application.yml` 的 `wl.semester` 节点
- 业务表前缀 `biz_`，系统表前缀 `sys_`（RuoYi 内置）
- `.env.development` 和 `application-druid.yml` 包含敏感配置且已被 git 跟踪
- G11 管理服务条目由 `ManagementItemGenerator` 从 `biz_role_assignment` 自动生成，也可手动录入
- 汇总表 `biz_workload_summary` 使用 JSON 字段 `category_details` 存储动态分类汇总，扩展新类别无需改表结构
- `DataScopeUtil.resolveUserId()` 强制教师角色只能看自己的数据，防止 IDOR，已在 calc/export/dashboard 控制器中使用
- 策略 bean 名称必须与 `biz_workload_category_dict.calc_strategy` 列精确匹配（如 `theoryCalcStrategy`），`CalcStrategyFactory` 按 bean 名解析

## 仓库内其他 AI 指引文件（以本文件为准）

| 文件 | 定位 | 注意 |
|------|------|------|
| `AGENTS.md` | 精简版速查（面向通用 AI agent） | 部分内容过时，冲突时以本文件为准 |
| `PROJECT_CONTEXT.md` | 一页式速查手册（含账号/配置速查） | 环境描述停留在 WSL，DB IP 注意事项同上 |
| `README.md` | 面向人的项目介绍 | 业务公式与代码一致，可作交叉参考 |
| 上级目录 `../CLAUDE.md` | 历史版本的项目指引 | **已过时**（前端端口写 80、DB 写 localhost），勿据此操作 |
