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
| M5 | 审批流与报表导出 | ✅ 已完成 | 审批状态机（2026-09-10 简化为两级）、附件1/2 报表导出、批量提交审核 |

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
│       ├── 16_remove_unused_modules.sql # 随 quartz/generator 模块下线清理菜单（幂等）
│       ├── ry_20260321.sql            # RuoYi 基础系统表
│       └── quartz.sql                 # Quartz 调度器表
├── front/RuoYi-Vue3/                  # 前端 Vue 3 项目
│   └── src/
│       ├── api/system/                # 31 个业务 API 文件
│       ├── views/system/              # 19 个业务页面（含 myWorkload/declare.vue 教师自主申报）
│       └── views/dashboard/           # 仪表盘
│           ├── AdminDashboard.vue     # 管理员大屏（4统计+ECharts+待办）
│           ├── TeacherDashboard.vue   # 教师工作台（数据卡+明细+达标面板）
│           └── JiaoWuDashboard.vue    # 教务助理工作台
│           # LeaderDashboard.vue 已于 2026-09-10 删除（院领导签字环节移除后无对应功能）
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

> **凭据一律走环境变量，不再硬编码入库。** 复制仓库根 `.env.example` 为 `.env` 填值，
> 或在 IDEA 的 Run/Debug Configurations → Environment variables 中注入。
> `WFIT_DB_PASSWORD` / `WFIT_DRUID_PASSWORD` / `WFIT_TOKEN_SECRET` **无默认值**，
> 未注入则后端启动直接失败（刻意设计，避免沿用弱口令而不自知）。

- 后端端口: `8084` (application.yml)
- 数据库: `application-druid.yml`，连接串由 `WFIT_DB_HOST`(默认 `127.0.0.1`) / `WFIT_DB_PORT` / `WFIT_DB_NAME` / `WFIT_DB_USER`(默认 `wfit`) / `WFIT_DB_PASSWORD` 组装。建议用仅授权 `wflg_workload` 库的专用账号，勿用 root
- JWT 密钥: `application.yml` 的 `token.secret` 由 `WFIT_TOKEN_SECRET` 注入（HS512 需 ≥64 字节，`openssl rand -base64 48` 生成）。**该值泄露即可伪造任意用户令牌绕过登录**；更换会使所有已发放令牌失效，需全员重新登录
- Druid 监控台: `/druid/*`，账密由 `WFIT_DRUID_USER`(默认 `admin`) / `WFIT_DRUID_PASSWORD` 注入（原框架默认 `ruoyi/123456` 已移除）。生产环境建议 `statViewServlet.enabled: false`
- Redis: `WFIT_REDIS_HOST`(默认 `localhost`) / `WFIT_REDIS_PORT`(默认 6379) / `WFIT_REDIS_PASSWORD`(本机留空)
- 文件上传路径: `rear/uploadPath/`
- 前端开发端口: `3000` (vite.config.js)
- 前端 API 代理: 开发环境 `/dev-api` 前缀请求代理到 `http://localhost:8084`（去掉前缀）
- 学期校历: `application.yml` 的 `wl.semester` 节点（秋季 09-01~01-31，春季 02-20~07-15）
- MySQL MCP: 仓库根 `.mcp.json` 注册了 `mysql` MCP server（`127.0.0.1:3306/wflg_workload`），可直接用 MCP 工具查库/看表结构，无需手写 `mysql` CLI。**该文件含本机明文口令，已在 `.gitignore` 中排除，不入库**

### 测试账号（密码均为 `123456`，来自 `06_test_accounts.sql`）

> 仅供本地开发/演示。部署到任何可被他人访问的环境前，必须改密或删除这批账号。

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
| G1 理论课 | `J1 * C1 * K1 * Q1 * Q2 * N` | J1=计划学时, C1=重复系数, K1=必修1.1/选修1.0, Q1/Q2=质量, N=合堂。**Q3 不参与**（第十四条1，Q3 为保留列） |
| G2 实践课 | `J2 * K * C2 * Q1 * Q2` | J2=实践学时, K=理工1.0/其他0.9, C2=0.9。**Q3 不参与**（第十四条2） |
| G3 实习实训 | `T * D * K * Q1 * Q2` | T=实际天数(×8学时), D=理工4/艺术3/文史2。**Q3 不参与**（第十四条3） |
| G4 课程设计 | `J4 * R4 * 0.4` | J4=学分, R4=实际人数**不截断**（第十四条4），超 CAP_R4_MAX=60 置 is_over_limit 告警 |
| G5 毕业论文 | `R5 * K5` | K5=理工本9/专5, 文史本6/专4 |
| G6 集中实习 | `W * min(R6,20) * 0.4` | W=周数, R6=人数(上限20) |
| G11 管理服务 | 岗位标准学时(**学年**)/2 × 任职天数/学期天数 | 学期封顶 180；督导例外（第十七条 15/学期不折半，2026-09-10 统一） |
| 绩效酬金 | `(min(总工作量,540) - 180) * 职称单位酬金` | 教授70/副60/讲50/助40 |

汇总层级：G7=G1~G6合计, G10=G7+G8+G9, 总工作量=G10+G11

制度性校验（`ComplianceChecker`，2026-09-10，只告警不改数值，落 `biz_workload_summary.remark`）：
三门理论课（G1 去重课程名≥3）视同完成基本教学量→`basic_teaching_met=1`（第六条）；
课程数>3（非实践类去重）告警（第六条）；周学时 (ΣJ1+ΣJ2)÷16 超上限告警
（行政岗 6/其余 16/仅专科 18，第八/九/十条，超限≤2 提示须审批）。
「本科(含专升本)」判本科档（不含「专科」子串）；系统无「院长」类岗位枚举，第八条 12 学时档不校验。
同 remark 还承载封顶告警（2026-09-10 统一「截断与降级必须告警」）：
G11 累计超 180/学期（第十六条注）、总工作量超 CAP_200PCT 绩效封顶（第二十条）、
A 项 ≥20 人归零（第十五条1(2)，落 `biz_allowance_item.remark`）；
附件1「系数说明」列按 G4（未封顶告警）/G5（须报批）/G6（已封顶）三分支措辞。

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
5. `SummaryCalcService` — 学期汇总：按类别累加后落 `biz_workload_summary` 的定长列 `G7`/`G8`/`G9`/`G10`/`G11`/`total_workload`/`excess_workload`/`performance_pay`/`is_capped`。**G1~G6 的分项合计不落汇总表**（只有 G7 一个合计列），要看分项须回 `biz_workload_item` 明细或导出附件1
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
| `/recalcAll` | POST | 单教师全量重算：明细→汇总→酬金，单事务，失败整体回滚（需 userId） |
| `/recalcAllBatch` | POST | **批量一键核算**：`?semester=` + body `[userId...]`；body 空/省略 = 该学期全部有明细的教师。每位教师独立事务，单人失败只记入 `failures`，其余照算。教师角色被强制收敛为只能算自己 |

## 审批流状态机

> **2026-09-10 简化为两级**：院领导签字环节已移除。旧状态 3（已完结）迁移为 2，
> 见 `sql/17_simplify_approval.sql`。`dept_leader_sign` / `dept_leader_sign_time`
> 两列保留但不再写入（便于将来恢复）。

```
0: 填报中 (教师/教务员可编辑)
  ↓ 教务员提交
1: 教务处待审 (教务处可审核/驳回)
  ↓ 教务处审核通过 —— 即终态，同时写 academic_assistant_sign 与 lock_time
2: 已完结 (锁定，不可修改)
  ↓ 管理员可解锁
0: 回到填报中

驳回：1 → 0（仅待审阶段可驳回）
注意：驳回**不退化为 -1**，直接回到 0（-1 从未实现，文档旧值）
```

**注意明细表 `biz_workload_item.status` 是另一套状态机**，与汇总状态无关：
`0草稿/1已核对(冻结)/2有异议/3已驳回`。`SummaryCalcServiceImpl` 跳过 `item.status==3`（已驳回）
不参与汇总——改审批状态时**不要连带改这里**。

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
| A3 | G11 折算多除了一个 2，与公式/种子数据/封顶矛盾 | ✅（**2026-09-10 依办法第十六条反向修正**）`allowance_rate` 约定改为存**学年值**，`ManagementItemGeneratorImpl` 恢复 ÷2；督导（第十七条 15/学期）不折半。A3 当年按学期值种子自洽，与现约定不是同一数据前提 |
| A4 | 教学任务导入自调用致 `@Transactional` 失效，部分失败提交半截数据 | ✅ 改用 `AopContext.currentProxy()` 每行独立事务 |
| A5 | 院领导待签(2)环节无驳回路径 | ✅ `BizAuditServiceImpl.reject` 放开 `from∈{1,2}`；院领导授 `reject` 权限 |
| A6 | G4 人数上限 20 与权威文档 R4≤60 冲突 | ✅ `CourseDesignCalcStrategy` 默认值改 60；`14_fix_calc_rules.sql` 已随 02 并入。**注意：本机库直到 2026-08-31 才真正执行到 60**（原记「已部署库」不实），且 `RuleParamServiceImpl` 缓存无 TTL，改库后必须删 Redis 键 `wl_rule:CAP_R4_MAX`，否则重启也读旧值 —— 见待办 #10。**2026-09-10 起语义变更：CAP_R4_MAX 由截断上限改为告警阈值，R4 按实际人数计算不再 min()**（办法第十四条4 未写「超出不计」） |
| A7 | 自学辅导 ≥20 人错算 260 元 | ⚠️ **2026-09-10 已按办法第十五条1(2) 改为归零**：≥20 人应单独开班按基本工作量（G1 路线）计，本项不再计绩效酬金；原「走手工金额分支」无条文依据且可绕过 260 上限。存量 0 行受影响 |
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

### 凭据治理与强制改密（2026-08-30）

| # | 问题 | 修复 |
|---|------|------|
| D1 | DB 口令 / Druid 台账密 / JWT secret 明文硬编码入库（公开仓库） | ✅ 全改环境变量注入（`WFIT_*`），`*_PASSWORD` 与 `WFIT_TOKEN_SECRET` **无默认值**，未注入则启动失败；新增 `.env.example`，`.env` 与 `.mcp.json` 入 `.gitignore`。轮换清单见 `docs/口令轮换清单-2026-08-30.md`（明文仍留在 git 历史，唯一补救是换口令） |
| D2 | 初始弱口令仅前端弹窗提醒，可取消、路由不设卡，等于无强制力 | ✅ 新增 `ForcePasswordChangeInterceptor`：`pwd_update_date IS NULL` 时除白名单外一切请求返回 **602**；前端 `request.js` 拦 602 弹不可取消对话框，`permission.js` 守卫只放行 `/user/profile*`，改密成功后清标记回首页 |
| D3 | 管理员重置密码后 `pwd_update_date = sysdate()`，用户拿着管理员知晓的口令即可畅通使用 | ✅ 拆分两个 mapper：用户自改走 `resetUserPwd`(置当前时间)，管理员重置走 `resetUserPwdRequireChange`(置 NULL)，强制用户再次自行改密 |

### 可追溯性与批量核算（2026-08-31）

用户诉求：一键核算只能单人？总金额怎么算？报表在哪导出？汇总完的记录「重复系数是哪个班」查不出来。
四项口径由用户拍板，下表按项落地。

| # | 问题 | 修复 |
|---|------|------|
| E1 | 重复系数 C1 在导入时硬编码 1.0，「第几次」这个事实从未落库，事后无从追溯 | ✅ 导入模板新增「重复次序」列（第 17 列）；留空则按 `countSameCourseTask` 自动补位。分组口径 = 教师 + 学期 + 课程名称 + 授课层次（**不含课程代码、班级与工作量类别**，依《办法》第十四条1「课程名称一致即同一门课，本专科分别算」；2026-09-10 移除 item_type——条文无类别要求，同名理论课与实习实训原各自从第一次起算无依据）。次序落 `biz_teaching_task.repeat_order`，C1 取 `COEF_REPEAT_1ST/2ND/3RD_UP` |
| E2 | G3 的 K 同为重复系数，也一直是常量 | ✅ `calcG3RepeatK`：第一轮 1.0，第二轮起 0.9。`else/工作量.md:67-68` 对 G3 只规定两档，故第三轮**不**套用 0.8 |
| E3 | `biz_teaching_task.class_name` 字段存在但导入模板无「班级」列，报表无法指名到具体班次 | ✅ 模板新增「班级」列（第 16 列），附件1 增「班级」列与「系数说明」列，把「这条为什么只算 0.8」写成人话 |
| E4 | 一键核算只能单教师（前后端两层都是） | ✅ 后端 `WorkloadCalcService.recalcAllBatch` + `POST /system/calc/recalcAllBatch`（每教师独立事务，失败收集进 `failures`）；前端学期汇总页新增「核算所选(n)」与「全学期核算」，原单人按钮保留 |
| E5 | 报表入口只在仪表盘，且靠 `$prompt` 手输学期；附件2 自己重算酬金而非读账 | ✅ 报表入口挪到学期汇总页（工具栏 + 行内「更多」），学期/教师取搜索栏；附件2 改读 `biz_pay_record`，并列「超额工作量」（未封顶）与「计酬超额工作量」（`min(总量,CAP_200PCT)−额定`）+「是否触顶」+「备注」，回答「这个人为什么没钱」（非专任/未核算/触顶） |
| E6 | 「G1~G6 分项落汇总表」 | ⛔ 用户明确不做。分项仍只在 `biz_workload_item` 明细层，靠附件1 导出追溯 |

**导入模板 17 列**（顺序须与 `TeachingTaskImportDTO` 的 `@ExcelProperty` 完全一致）：
学年学期 / 教师工号 / 教师姓名 / 课程名称 / 课程代码 / 工作量类别 / 授课层次 / 专业大类 / 课程性质 /
课程级别 / 课程角色 / 教学评价 / 选课人数 / 计划学时·天数·周数 / 课程系数 / **班级** / **重复次序**

**报表列集**：附件1 = 19 列（含 班级 / 重复次序 / 重复系数 / K1 / Q1 / Q2 / Q3 / N / 其他系数(D·K5·K) / 系数说明 / 数据来源）；
附件2 = 17 列（含 人员性质 / 是否触顶 / 计酬超额工作量 / 备注）。

**总金额口径**（`SummaryCalcServiceImpl`，勿凭直觉推）：
`G7=ΣG1..G6`（跳过 `status==3` 的明细）→ `G10=G7+G8+G9` → `G11=min(ΣG11,180)` → `总工作量=G10+G11`；
`绩效酬金=(min(总工作量,540)−180)×职称费率`，且仅当 `总工作量>180` **且 `teacher_nature ∈ {专任, null}`**（外聘/校企/银龄不计发，是设计如此，不是 bug）；
`其他酬金=Σ biz_allowance_item.amount(status=1)`；`total_pay=round(绩效+其他)`。
`excess_workload` 刻意**不封顶**，故触顶教师「超额×单位酬金 ≠ 绩效酬金」，附件2 用两列并排 + 备注解释。

**测试用例**：仓库根 `gen_qianwei_repeat_coef_import.py` → `教学任务导入_钱伟_重复系数专项_18条.xlsx`，
A~K 共 11 组覆盖 本专科分别计数 / 代码相同但课名不同不算重复 / N 与 C1 叠加 / G2 的 C2 恒 0.9 / G3 两档 K /
G4 触 60 上限 / G5·G6 超限标记 / **K 组显式「重复次序」优先于文件行序**。Sheet2 逐行列出期望系数、期望工作量与落库校验点。

### 端到端验证发现并修复（2026-08-31，18 条样例实测导入 → 核算 → 导出）

| # | 问题 | 修复 |
|---|------|------|
| F1 | 教师自主申报（`declare.vue`）的 G11 只写 `biz_workload_item` 主表，而 `biz_wl_management.assignment_id` 为 NOT NULL 且外键指向 `biz_role_assignment` —— 手工申报没有任职记录，明细行**构造不出来**。`ManagementCalcStrategy` 遇 null 明细无条件抛异常，这条申报因此永远无法重算；又因批量核算的事务粒度是「每教师一个事务」，一条申报会让该教师整学期 明细→汇总→酬金 全线失败（实测 `recalcAllBatch` failCount 1，reason=`G11管理服务明细缺失, itemId=9056`） | ✅ `ManagementCalcStrategy.calculate` 增加 SELF 分支：`source_type=SELF` 且无明细时取主表 `calculated_workload`（教师按管理办法自行核定的学时）；AUTO/IMPORT 来源缺明细仍 fail-loud，保持 A2 口径 |
| F2 | `afterCalculated` 的唯一调用点是 `WorkloadCalcServiceImpl:79`，导入路径从不触发 → G5/G6 的 `is_over_limit` 导入后恒为 0，要等下一次重算才置位（而重算又被 F1 挡住） | ✅ `TeachingTaskImportServiceImpl.processSingleRow` 在回写前调 `calcStrategyFactory.get(type).afterCalculated(item, calculated)`，超限标记与工作量共用同一条 UPDATE 落库。实测超限行置 1、未超限行仍 0 |

**实测结论**（钱伟 `T20270001` / `2026-2027-1`，18 条导入 + 3 条自主申报）：

- 系数 **18/18** 与样例工作簿「预期结果」表逐行一致：本专科分别计数、课程代码相同但课名不同不算重复、显式「重复次序」优先于文件行序、G2 的 C2 恒 0.9、G3 两档 K、G4 触 60 上限
- 汇总链：`G7 947.33`（18 条之和）+ G8 10 + G9 5 → `G10 962.33`；`G11 min(20,180)=20` → `总工作量 982.33`；额定 180；`excess_workload 802.33`（刻意不封顶）；`is_capped=1`
- `performance_pay=0` **不是 bug**：该教师 `teacher_nature=外聘`，按设计不计发绩效
- 附件1 = 19 列 / 21 条明细，系数说明渲染为「第 1 次（计算机2401），重复系数 1」，G5/G6 超限行追加「人数超上限，已按封顶值核算」；附件2 = 17 列，超额 802.33 与计酬超额 360.0（=`min(982.33,540)−180`）并列，备注同时点出「外聘不计发」与「已触 200% 上限 540」
- `recalcAllBatch` 三种入参（指定 `userIds` / 空数组 / 省略 body）均 `successCount 1` `failCount 0`
- 导出端点是 **GET** `/system/export/personalWorkload` 与 **GET** `/system/export/paySummary`（不是 POST，也不是 `/export/personal`）；导入端点是 **POST** `/system/teachingTask/importExcel`（不是 `/importData`）

### 逻辑审计与修复（2026-09-01，代码 × `else/工作量.md` × `biz_workload_rule` 三方对读）

| # | 问题 | 修复 |
|---|------|------|
| G1 | **导入时「按专业大类/授课层次取系数」的逻辑根本不存在**：`createG2Detail`/`createG3Detail`/`createG5Detail` 只取 Excel「课程系数」列，留空就硬编码兜底 `1.0`/`4.0`/`9`——全是理工本科档。规则表 `COEF_PRACTICE_LG/OTHER`、`COEF_TRAIN_D_LG/ART/HUM/UNIT`、`COEF_THESIS_K5_{LG,HU}_{B,C}` 全部 status=1 却**零处引用**；DTO 与库里都有 `major_category`/`education_level`，只是从未参与选系数。实测偏差：G5 文史专科 K5 落 9 而非 4（+125%）、G3 文史 D 落 4.0 而非 2.0（+100%）、G2 文史 K 落 1.0 而非 0.9（+11%） | ✅ 新增 `calcG2K`/`calcG3D`/`calcG5K5`：**Excel「课程系数」列填了仍优先**（教务按个案覆盖，G3「单位指导 D=2.0」只能这样表达），留空才按 专业大类(×授课层次) 查规则表。艺术类与「其他」在 G5 归文史档（文档 K5 只分理工/文史两支，见待办 #5），在 G3 分别取 `D_ART 3.0` / `D_HUM 2.0`。`calcN` 同步改读 `COEF_CLASS_120_150/151_UP` |
| G2 | **解锁不清签字**：`unlockById` 只置 `lock_time=NULL, status=0`，三个签字字段及时间全部保留。记录退回草稿态后数字可被重算，旧签字等于让教务/院领导/教师为改动后的新数字背书（B2 修 reject 时是同一理由，unlock 漏改） | ✅ `unlockById` 与 `rejectSummary` 同口径清空 `academic_assistant_sign`/`teacher_sign`/`dept_leader_sign` 及三个时间 |
| G3 | 附件1 对 G5 行写「人数超上限，已按封顶值核算」是**假的**：G5 从不封顶（`ThesisCalcStrategy` 无 `min`），其 `is_over_limit` 只表示「须报院长批准、教务处备案」；G6 才是真按 `min(R6,20)` 封顶。该缺陷是 F2 让标记真正落库后才显形 | ✅ `buildCoefRemark` 按 `item_type` 分流文案，G5 写「人数超申报上限，须报院长批准、教务处备案（学时按实际人数计，未封顶）」 |
| G4 | `ManagementItemGeneratorImpl` 类注释仍写「标准学时/学年 ÷ 2 ×…」，A3 删掉该除法时漏改注释，照注释维护会把 ÷2 补回去 | ✅ 注释更正为「rate 已定性为学期标准」，并写明勿再补 ÷2 |
| G5 | `AllowanceBStrategy` 取 `PAY_B_CONCENTRATED` 时兜底默认值写的是 `10`（分散价），规则行若被删/停用，集中实习按 10 元/人少发 1/3 | ✅ 兜底按档位分流：集中 15 / 分散 10 |

**实测验证**（8 条专项样本导入 → 读库 → 删除，钱伟基线 947.33/982.33/21 条已恢复）：

| 样本 | 期望 | 实落 | 期望学时 | 实算 |
|------|------|------|----------|------|
| G5 文史·专科 R5=6 | K5=4 | 4.00 | 24.00 | 24.00 |
| G5 文史·本科 R5=5 | K5=6 | 6.00 | 30.00 | 30.00 |
| G5 理工·专科 R5=4 | K5=5 | 5.00 | 20.00 | 20.00 |
| G3 文史 T=5 | D=2.0 | 2.00 | 10.00 | 10.00 |
| G3 艺术 T=5 | D=3.0 | 3.00 | 15.00 | 15.00 |
| G3 理工·显式填 2.0 | 覆盖 4.0 | 2.00 | 10.00 | 10.00 |
| G2 文史 J2=16 | K=0.9 | 0.90 | 12.96 | 12.96 |
| G1 160 人 J1=32 | N=1.2 | 1.20 | 42.24 | 42.24 |

unlock 走 submit→approve→sign→unlock 往返：签字三栏与 `lock_time` 在 status 3 时齐全，解锁后全部为 NULL、status 回 0。
附件1 导出 19 列，G5 行文案为「…须报院长批准、教务处备案（学时按实际人数计，未封顶）」，G6 行仍为「已按封顶值核算」。

> ⚠️ **G1 只对新导入生效**。D/K5/K 是导入时算好写进 `biz_wl_*` 的，重算只读回不重算系数，
> 因此**存量明细必须改数据才能纠正**（重跑导入或直接 UPDATE 明细表后重算）。
> 另：`04_biz_test_data.sql` 手写的种子里 G5 文史类本科 K5=9（应 6）、专业大类「其他」的 G5 K5=9（按上述口径应 6）本身就是错的。

### 待办（未处理，需排期/决策）

| # | 问题 | 优先级 | 说明 |
|---|------|--------|------|
| 1 | ~~collegeStats 返回空~~ | ✅ 已修复 | 执行 `12_fix_dept_mapping.sql` 补充 sys_dept 数据 |
| 2 | G8/G9 策略为空 | 低 | 设计如此：第二课堂/其他工作量为手动录入金额 |
| 3 | ~~代阅卷酬金 D 档位~~ | ✅ 已启用 | 办法第十五条5 给出完整五档（<20→0、[20,60)→30、[60,120)→80、[120,200)→100、≥200→150），「待正式文件」不成立。`AllowanceDStrategy` 已注册，规则见 `03_calc_rules.sql` / `19_enable_allowance_d.sql`（2026-09-10） |
| 4 | ~~Q3 全外文课程系数~~ | ✅ 已裁定 | 办法第十四条1~3 公式均不含 Q3（附件1 模板 L/P/T 列同）；字段保留作展示列，**不参与 G1/G2/G3 计算**（2026-09-10 修正，此前三策略均多乘 Q3） |
| 5 | G5 艺术类 K5 映射 | 低 | 暂按文史类处理，待艺术类专业目录确认 |
| 6 | `assertOwnOrAdmin` 豁免范围偏宽（`!isTeacherOnly`） | 低 | 已裁决保留：现有角色仅 admin/biz_admin/assistant/leader/teacher，除教师外均应豁免且先过 `@PreAuthorize`；彻底收严需引入显式管理角色白名单，属权限模型改造 |
| 7 | 策略缓存 `StrategyCache.evict/clear` 无调用方 | 低 | 字典改绑策略后缓存永久陈旧到重启，建议在字典增改删 Service 调 evict |
| 8 | ~~教师账号默认弱口令 123456 无强制改密~~ | ✅ 已修复 | `ForcePasswordChangeInterceptor` 服务端拦截（`pwd_update_date IS NULL` → 602）+ 前端路由守卫只放行改密页；管理员重置密码走 `resetUserPwdRequireChange` 置 NULL，要求用户再次自行改密 |
| 9 | 200%/540 封顶边界 `>` vs `>=`、`teacherNature` 为 null 当专任发绩效 | 低 | 需业务确认口径，非明确 bug |
| 10 | `RuleParamServiceImpl` 写 Redis 不设 TTL（`setCacheObject(key, value)`），改 `biz_workload_rule` 后即使重启后端也仍读旧值 | 中 | 已有 `evict(code)` 但无调用方，规则维护 Service 保存后应调 evict；临时手段是删 `wl_rule:<CODE>` 键。本轮 `CAP_R4_MAX` 即因此卡在 20 |
| 11 | 自主申报的 G11 与生成器的 G11 是两套模型：前者只有主表已核定学时，后者才有 `biz_wl_management` 折算明细 | 低 | F1 已让重算不再崩（SELF 取主表值），但「教师能不能自主申报 G11」这个产品口径未定。彻底方案二选一：① `declare.vue` + 后端禁掉 G11 自主申报，只走 `biz_role_assignment` → 生成器；② 申报时一并建任职记录与明细行。定了再改 |
| 12 | ~~本科毕业论文 R5 要不要封顶 10~~ | ✅ 已裁定 | 办法第十四条5（PDF 原文）只说「R5≤10 时**按实际人数计算**、＞8 须报院长批准」，**未写「超出不计算」**→ 不封顶，仅报批标记。死配置 `CAP_R5_BACHELOR/CAP_R5_JUNIOR` 已停用（`18_fix_r5_approval.sql`），专科阈值改挂 `APPROVAL_R5_JUNIOR=15`（2026-09-10） |
| 13 | ~~G6 的 `CAP_R6_MAX=20` 缺文档依据~~ | ✅ 已裁定 | 办法第十四条6**注4** 明写「指导实习的学生人数不超过20人，**超出部分不计算工作量及酬金**」→ `min(R6,20)` 截断**有依据**，此前「缺文档依据」系仅查 `else/工作量.md` 漏了 PDF 原文的误判（2026-09-10，已固化 `ConcentratedInternshipCalcStrategyTest`） |
| 14 | **审批中允许重算**：汇总/酬金重算只拒已完结，`assertEditable` 只看 item.status=1（已核对）与 summary.status=2（已完结）。**待审(status=1)期间重算会改数字**，而教务处签字仍挂在旧数字上 | 中 | 2026-09-10 两级简化后已完结为 2。与 A12（教师 status≠0 不能改明细）、B2（只有驳回才清签）不同口径。修法可选：重算前拒 status=1，或改数后自动清签退回 0 |
| 15 | **停用的教学任务仍计入重复次序**：`countSameCourseTask` 不过滤 `t.status`（1正常/0停用），停用行照数，后续班次被多降一档系数 | 中 | 方向是少算。逻辑上停用即不成立，倾向加 `and ifnull(t.status,1)=1`，待确认「停用是否等于该次开课不成立」 |
| 16 | `CONST_COURSE_DESIGN`(0.40) 被 G4 与 G6 共用一个规则键 | 低 | 两条独立政策捆在一起，改一个静默动另一个。拆键需同步改 02 种子与两个策略 |
| 17 | `onDetailDeleted` 只清零明细，不刷汇总、不校验可编辑性 | 低 | 汇总滞后于已删明细，直到下次重算；已完结记录的明细被删也照清零 |
| 18 | 生成器写 `source_type='IMPORT'` 而非 AUTO | 低 | 与 `ManagementCalcStrategy` 注释、本文档的「生成器(AUTO)/导入(IMPORT)」口径不符，三态实际只有两态可辨。改需同步迁移存量数据 |
| 19 | 已核对(status=1)的 G11 再跑生成器：`biz_wl_management.prorated_amount` 被改写，主表 `calculated_workload` 冻结不动 | 低 | 明细与主表数字打架，审计时以哪个为准不明确 |
| 20 | `SemesterCalendar` 硬假设秋季学期结束落在次年（`atYear(yearEnd)`） | 低 | 若有人把 `wl.semester.autumn-end` 配成 `12-31`，区间会变成 16 个月，G11 折算分母随之失真。建议加区间跨度校验 |

## 注意事项

- 活跃前端只有 `front/RuoYi-Vue3`（Vue 3）；历史上的 Vue2 `workload-ui` 已移除，不在仓库内
- **2026-09-10 减量**：`workload-quartz` / `workload-generator` / `manage` 三模块已删除
  （`rear/pom.xml` 的 `modules` 现为 4 个）。前端 `views/tool/gen`、`views/tool/build`、
  `views/monitor/job` 与对应菜单/按钮权限（`monitor:job:*`、`tool:gen:*`、`tool:build:*`）
  已一并清理，脚本 `sql/16_remove_unused_modules.sql`（幂等）。后台任务若将来需要异步重算，
  再按需引入 `spring-boot-starter-quartz` 或 `@Scheduled`（后者无需独立模块）
- 学期格式为 `2025-2026-1`（学年+学期号），校历配置在 `application.yml` 的 `wl.semester` 节点
- 业务表前缀 `biz_`，系统表前缀 `sys_`（RuoYi 内置）
- `front/RuoYi-Vue3/.env.development` 已被 git 跟踪，但仅含页面标题与 `/dev-api` 前缀，**无敏感信息**；后端凭据已全部改为环境变量注入（见「配置要点」）
- ⚠️ **历史遗留**：`application-druid.yml`(DB root/123456、Druid ruoyi/123456) 与 `application.yml`(JWT secret) 的明文值曾提交入库，仍留在 git 历史中。当前工作树已清除，但旧 commit 可追溯 —— 唯一有效的补救是**轮换这些口令**（DB 改专用账号、JWT secret 重新生成），而非只改文件
- G11 管理服务条目由 `ManagementItemGenerator` 从 `biz_role_assignment` 自动生成，也可手动录入
- ⚠️ `biz_workload_summary` **没有** `category_details` JSON 列 —— 该字段只存在于 `else/潍理工工作量管理系统设计new).md` 的设计稿中，DDL(`01_biz_schema.sql`)、实际库与 Java 代码里均无此列，早期文档把它写成了既成事实。现状是 G7~G11 定长列；如需 G1~G6 分项汇总，属未实施的增强项（本轮已由用户明确不做）
- `DataScopeUtil.resolveUserId()` 强制教师角色只能看自己的数据，防止 IDOR，已在 calc/export/dashboard 控制器中使用
- 策略 bean 名称必须与 `biz_workload_category_dict.calc_strategy` 列精确匹配（如 `theoryCalcStrategy`），`CalcStrategyFactory` 按 bean 名解析

## 仓库内其他 AI 指引文件（以本文件为准）

| 文件 | 定位 | 注意 |
|------|------|------|
| `AGENTS.md` | 精简版速查（面向通用 AI agent） | 部分内容过时，冲突时以本文件为准 |
| `PROJECT_CONTEXT.md` | 一页式速查手册（含账号/配置速查） | 环境描述停留在 WSL，DB IP 注意事项同上 |
| `README.md` | 面向人的项目介绍 | 业务公式与代码一致，可作交叉参考 |
| 上级目录 `../CLAUDE.md` | 历史版本的项目指引 | **已过时**（前端端口写 80、DB 写 localhost），勿据此操作 |
