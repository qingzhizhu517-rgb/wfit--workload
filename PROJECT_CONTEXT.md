# PROJECT_CONTEXT — 潍理工教学工作量系统 速查手册

> 面向 AI / 新开发者的一页式项目上下文速查。所有事实均核对过最新代码（2026-07 之后），
> 优先看本文件；更完整的背景见 `CLAUDE.md`、`README.md`、`docs/`。
> 当前运行环境：**WSL（Ubuntu）**，工作目录 `/home/aohs/vibecoding/wfit--workload`。

---

## 0. 一句话定位

高校教师**教学工作量核算 → 学期汇总 → 绩效酬金**系统，替代手工汇总，实现教务处→学院→教师全流程数字化。基于 RuoYi-Vue3 前后端分离框架定制，M1–M5 全部完成。

---

## 1. 技术栈与环境（速查）

| 项 | 值 |
|---|---|
| 后端 | Spring Boot **4.0.7** / Java **17** / Maven 多模块 / MyBatis / Druid |
| 安全 | Spring Security + JWT（HS512，30 分钟过期） |
| 前端 | Vue 3.5 + Vite 6 + Element Plus 2.13 + Pinia 3 |
| 数据库 | MySQL 8，**`172.19.80.1:3306/wflg_workload`**（WSL 桥接 IP，指向 Windows 宿主，非 localhost） |
| 缓存 | Redis `localhost:6379`（无密码） |
| 后端端口 | `8084`，Swagger 在 `/swagger-ui.html` |
| 前端端口 | `3000`（dev），`/dev-api` 代理到 `http://localhost:8084`（去掉前缀） |

**关键配置位置**
- 数据源/DB 密码：`rear/workload-admin/src/main/resources/application-druid.yml`（root/123456）
- 端口/Redis/学期校历/导入密码：`rear/workload-admin/src/main/resources/application.yml`
  - `wl.semester`：秋 `09-01~01-31`，春 `02-20~07-15`
  - `wfit.default-password: 123456`（导入新建教师账号初始密码）
  - `wfit.import.max-size: 10`（Excel 上传上限 MB）

**账号（密码均 `123456`）**：`admin_test`(管理员) / `jiaowu_test`(教务助理) / `teacher_test`(教师) / `leader_test`(院领导)；RuoYi 内置 `admin/admin123`。

---

## 2. 目录结构与模块依赖

```
wfit--workload/
├── rear/                          # 后端（Maven 多模块，group=com.workload, ver=3.9.2）
│   ├── workload-admin/            # 启动入口 + 系统管理 Controller（端口 8084）
│   ├── workload-system/           # ★ 全部业务逻辑（calc 引擎 + 22 个 Biz Controller）
│   ├── workload-framework/        # Security、数据源、AOP、配置
│   ├── workload-common/           # 通用工具、注解、异常
│   ├── workload-quartz/           # 定时任务
│   ├── workload-generator/        # 代码生成器
│   ├── manage/                    # 空壳（仅 hello-world Main.java，无业务）
│   └── sql/                       # 建表/种子/规则/菜单（新库：01→06 + 08 + 13）
├── front/RuoYi-Vue3/              # 前端（Vue 3）
│   └── src/{api,views}/system/    # 31 个 API 文件 + 19 个业务页面
├── else/                          # 需求/设计原始文档（工作量.md 是 G1-G11 权威公式）
└── docs/                          # API 文档、审查报告、测试、superpowers specs/plans
```

**Maven 模块依赖方向**：`workload-admin → workload-system → workload-framework → workload-common`（quartz/generator/manage 为旁支）。

---

## 3. 业务核心：G1–G11 核算公式

| 类型 | 名称 | 公式 | 说明 |
|---|---|---|---|
| G1 | 理论课 | `J1×C1×K1×Q1×Q2×Q3×N` | J1=计划学时, C1=重复系数, K1=必修1.1/选修1.0, Q=质量, N=合堂 |
| G2 | 课内实践 | `J2×K×C2×Q1×Q2×Q3` | J2=实践学时, K=理工1.0/其他0.9, C2=0.9 |
| G3 | 实习实训 | `T×D×K×Q1×Q2×Q3` | T=实际天数(×8学时), D=理工4/艺术3/文史2 |
| G4 | 课程设计 | `J4×min(R4,20)×0.4` | J4=学分, R4=人数(上限20) |
| G5 | 毕业论文 | `R5×K5` | K5=理工本9/专5, 文史本6/专4 |
| G6 | 集中实习 | `W×min(R6,20)×0.4` | W=周数, R6=人数(上限20) |
| G7 | 第一课堂合计 | G1+G2+G3+G4+G5+G6 | 无独立策略，聚合项 |
| G8 | 第二课堂 | 手动录入 | **无自动计算** |
| G9 | 其他工作量 | 手动录入 | **无自动计算** |
| G10 | 教学工作量合计 | G7+G8+G9 | 聚合项 |
| G11 | 管理服务 | 岗位标准学时 × 任职天数/学期天数 | 学期封顶 180，由 `ManagementItemGenerator` 从 roleAssignment 生成 |

**绩效酬金**：`课时酬金 = (min(总工作量,540) − 180) × 职称单位酬金`；教授70/副60/讲50/助40。
其他酬金 `A(重修辅导)+B(毕业实习)+C(论文重修)+D(代阅卷)+E(讲座)+F(运动会裁判)+G(夜间值班)`。

---

## 4. 计算引擎架构（策略模式 + Spring IOC）

**核心：按 bean 名动态分发，无 if-else。** 包路径 `com.workload.system.calc`。

数据流：
```
类型代码 G1/G2/...
   ↓ BizWorkloadCategoryDictMapper 查 category_dict.calc_strategy 列（存 bean 名）
CalcStrategyFactory.get(typeCode)
   ↓ strategyMap.get(beanName)  （@Autowired Map<String,WorkloadCalcStrategy>，按名注入）
WorkloadCalcStrategy.calculate(item)   → 返回 BigDecimal（scale=2 HALF_UP）
   ↓ afterCalculated(item, value) 回调（G5/G6 置 is_over_limit 超标标记）
落库 biz_workload_item（主表）+ biz_wl_*（明细）
```

**接口** `strategy/WorkloadCalcStrategy`：`getTypeCode()` / `calculate(item)` / `afterCalculated(item, value)`(default)。
**工厂** `strategy/CalcStrategyFactory`：结果缓存 `ConcurrentHashMap`，无策略类别（G7/G8/G9/G10）返回 null。

**7 个工作量策略 bean（`@Component("xxx")` 名必须与 `calc_strategy` 列精确一致）**：

| typeCode | bean 名 | 类 |
|---|---|---|
| G1 | `theoryCalcStrategy` | TheoryCalcStrategy |
| G2 | `practiceCalcStrategy` | PracticeCalcStrategy |
| G3 | `internshipTrainingCalcStrategy` | InternshipTrainingCalcStrategy |
| G4 | `courseDesignCalcStrategy` | CourseDesignCalcStrategy |
| G5 | `thesisCalcStrategy` | ThesisCalcStrategy |
| G6 | `concentratedInternshipCalcStrategy` | ConcentratedInternshipCalcStrategy |
| G11 | `managementCalcStrategy` | ManagementCalcStrategy |

**规则参数** `rule/RuleParamService`（Redis 缓存，政策变动只改库）。
**酬金** `allowance/AllowanceStrategyFactory`：`@Autowired List<AllowanceCalcStrategy>` → 按 `getFeeType()` 建 map，`get(feeType)` 返回策略。
**酬金策略（默认 bean 名 = 类名首字母小写）**：`AllowanceA/B/C/E/F/GStrategy` 共 6 个；**D（代阅卷）未注册**（`AllowanceStrategyFactory` 注释明确"首期未注册，取不到即未启用"）。
**服务编排**：`WorkloadCalcService`(单条/批量/全量) → `SummaryCalcService`(学期汇总) → `PayCalcService`(酬金)；`ManagementItemGenerator`(G11 生成)；`SemesterCalendar`(校历)。

---

## 5. 数据流（端到端）

```
Excel 导入（教务员）→ biz_import_batch + biz_teaching_task / biz_role_assignment（源数据层）
   ↓ calc_strategy 列 → CalcStrategyFactory 解析 bean
策略 calculate() → biz_workload_item（主表）+ biz_wl_*（G1~G6/G11 明细层）
   ↓
SummaryCalcService → biz_workload_summary（汇总层；JSON 字段 category_details 动态分类，扩展类别不改表）
   ↓
PayCalcService → biz_pay_record + biz_allowance_item（酬金层）
```

---

## 6. 数据库表分层（19 张业务表）

| 层 | 表 | 备注 |
|---|---|---|
| 支撑层 | biz_teacher_profile / biz_workload_category_dict / biz_workload_rule / biz_pay_rate / biz_import_batch | 类别字典的 calc_strategy 列绑定策略 bean |
| 源数据层 | biz_teaching_task / biz_role_assignment | Excel 导入原始数据 / 岗位任职 |
| 明细层 | biz_workload_item + biz_wl_theory / practice / internship_training / course_design / thesis / concentrated_internship / management | 主表 + 7 张 G 明细 |
| 汇总层 | biz_workload_summary | 含审批状态 + JSON category_details |
| 酬金层 | biz_pay_record / biz_allowance_item | 酬金汇总 / 其他酬金明细(A-G) |
| 审计 | biz_audit_log | **在 `08_review_fixes.sql` 创建**（不在 01 schema 里），审批流审计 |

> 18 张在 `01_biz_schema.sql`，第 19 张 `biz_audit_log` 在 `08_review_fixes.sql`。

**SQL 执行顺序**（DB `wflg_workload`，新库最小集）：`ry_20260321.sql → quartz.sql → 01_biz_schema → 02_biz_seed → 03_calc_rules → 04_biz_test_data → 05_biz_menu → 06_test_accounts → 08_review_fixes → 13_fix_audit_perm`。
- **08 必须**：`biz_audit_log`（第 19 张表）仅在此创建，01 未包含。
- **13 必须**：撤销 06 授予教务助理的 unlock 越权，并给院领导补授 reject。
- **07/09/10/11/14 已并入** 04/05/01/02，新库可跳过（全幂等，跑了无害）。
- **12 可选**：将 ry 默认部门名改为学院名（upsert，幂等）。
- **03 非幂等**（`INSERT INTO`），只执行一次。

---

## 7. 后端 API 索引（22 个 Biz Controller，`/system/*`）

| Controller | 职责 |
|---|---|
| BizCalcController | `/system/calc/*` 计算引擎（见下） |
| BizDashboardController | 仪表盘统计 |
| BizAuditController | 审批流 |
| BizExportController | 附件1/2 报表导出 |
| BizTeacherProfileController / BizTeachingTaskController / BizRoleAssignmentController / BizImportBatchController | 源数据/支撑层 CRUD + 导入 |
| BizWorkloadItemController | 工作量明细主表 |
| BizWlTheory/Practice/InternshipTraining/CourseDesign/Thesis/ConcentratedInternship/ManagementController | 7 张 G 明细 |
| BizWorkloadSummaryController | 学期汇总（含审批按钮/状态统计） |
| BizPayRecordController / BizAllowanceItemController | 酬金 |
| BizWorkloadCategoryDictController / BizWorkloadRuleController / BizPayRateController | 字典/规则/费率 |

**计算引擎端点**（`BizCalcController`，权限 `@PreAuthorize("@ss.hasPermi(...)")`）：

| 端点 | 方法 | 功能 |
|---|---|---|
| `/recalcItem/{itemId}` | POST | 单条重算 |
| `/recalcItems` | POST | 批量重算（userId, semester） |
| `/recalcSummary` | POST | 学期汇总落库 |
| `/preview` | GET | 汇总预览（不落库） |
| `/recalcPay` | POST | 酬金计算（需先汇总） |
| `/genG11` | POST | 生成 G11 管理条目 |
| `/recalcAll` | POST | 全量：明细→汇总→酬金（单事务，失败整体回滚） |

> `controller/ImportFileValidator.java` 是导入校验辅助类，非 Controller。

---

## 8. 前端页面索引（`front/RuoYi-Vue3/src/views/`）

- **业务页（19，`system/`）**：teacherProfile, teachingTask, roleAssignment, importBatch, workloadItem, wlTheory, wlPractice, wlInternshipTraining, wlCourseDesign, wlThesis, wlConcentratedInternship, wlManagement, workloadSummary(含审批), payRecord, allowanceItem, workloadCategoryDict, workloadRule, payRate, myWorkload/declare.vue（教师自主申报）
- **仪表盘（4，`dashboard/`）**：AdminDashboard, TeacherDashboard, JiaoWuDashboard, LeaderDashboard
- **API（31，`api/system/*.js`）**：与页面一一对应 + `calc.js`/`audit.js`/`export.js`/`dashboard.js`

---

## 9. RuoYi 框架约定（改代码必守）

- **Controller**：返回 `AjaxResult` 或 `TableDataInfo`；分页 `startPage()` + `getDataTable()`；继承 `BaseController`
- **Service**：接口 `IXxxService` + 实现 `XxxServiceImpl`；`@Transactional` 只加在 impl
- **Mapper**：接口 + XML（`resources/mapper/system/*.xml`）
- **实体**：继承 `BaseEntity`（自动 createBy/updateBy/createTime/updateTime/remark）
- **权限**：`@PreAuthorize("@ss.hasPermi('system:xxx:list')")`，格式 `模块:实体:操作`
- **日志**：`@Log(title="...", businessType=BusinessType.INSERT)`
- **导入导出**：实体字段 `@Excel` + POI/EasyExcel

---

## 10. 审批状态机

```
0 填报中 ──教务员提交──▶ 1 教务助理待审 ──通过──▶ 2 院领导待签 ──签字──▶ 3 已完结(锁定)
   ▲                            │驳回                              │驳回
   └────────────────────────────┴──────────────────────────────────┘
-1 驳回 → 回到 0，可修改后重新提交
```

审计落在 `biz_audit_log`（from_status/to_status 为 TINYINT(1)）。

---

## 11. 非显然事实 / 坑点（AI 必读）

1. **DB 主机是 `172.19.80.1`**（WSL 桥接 IP，指向 Windows 宿主 MySQL），不是 localhost —— 连不上数据库先查这个。
2. **无自动化测试**：`spring-boot-starter-test` 未引入。验证靠 Swagger（`/swagger-ui.html`）或前端页面；`test_api.sh` 提供 12 项 curl 冒烟测试（需先关验证码）。
3. **策略按 bean 名解析**：`biz_workload_category_dict.calc_strategy` 列必须与 `@Component("xxx")` 精确一致，改错就取不到策略（返回 null → 该类别无计算）。
4. **G8/G9 无策略**（手动录入）；**酬金 D 档（代阅卷）未注册**。
5. **`manage` 模块是空壳**；`rear/workload-ui`（Vue2）已废弃，活跃前端只有 `front/RuoYi-Vue3`。
6. **学期格式** `2025-2026-1`（学年+学期号），校历在 `application.yml` 的 `wl.semester`。
7. **数据隔离**：`DataScopeUtil.resolveUserId()` 强制教师角色只能看本人数据（防 IDOR），已在 calc/export/dashboard 控制器收口。
8. **敏感配置已 git 跟踪**：`.env.development`、`application-druid.yml`（DB 密码明文 123456）。
9. **汇总表动态分类**：`biz_workload_summary.category_details` 是 JSON，新增类别无需改表结构。
10. **验证码**：默认 `math` 类型，自动化脚本需先 `UPDATE sys_config SET config_value='false' WHERE config_key='sys.account.captchaEnabled'`。

---

## 12. 常用命令（WSL 内执行）

```bash
# 后端（在 rear/ 下）
mvn clean package -DskipTests                 # 全量打包
mvn clean compile -pl workload-system -am     # 只编业务模块
mvn spring-boot:run -pl workload-admin        # 启动（端口 8084）

# 前端（在 front/RuoYi-Vue3/ 下）
npm install --registry=https://registry.npmmirror.com
npm run dev          # 3000 端口
npm run build:prod

# 数据库（在 rear/sql/ 下，按顺序，新库最小集）
mysql -u root -p wflg_workload < ry_20260321.sql
mysql -u root -p wflg_workload < quartz.sql
mysql -u root -p wflg_workload < 01_biz_schema.sql
mysql -u root -p wflg_workload < 02_biz_seed.sql
mysql -u root -p wflg_workload < 03_calc_rules.sql
mysql -u root -p wflg_workload < 04_biz_test_data.sql
mysql -u root -p wflg_workload < 05_biz_menu.sql
mysql -u root -p wflg_workload < 06_test_accounts.sql
mysql -u root -p wflg_workload < 08_review_fixes.sql   # 必须：biz_audit_log
mysql -u root -p wflg_workload < 13_fix_audit_perm.sql # 必须：撤越权+补驳回
# 可选 12_fix_dept_mapping.sql（部门学院化）；07/09/10/11/14 已并入基础脚本

# API 冒烟测试（需先启动后端并关验证码）
bash test_api.sh
```

---

## 13. 关键入口文件速查

| 组件 | 文件 |
|---|---|
| 启动类 | `rear/workload-admin/src/main/java/com/workload/RuoYiApplication.java` |
| 计算 API | `rear/workload-system/**/controller/BizCalcController.java` |
| 策略接口 | `rear/workload-system/**/calc/strategy/WorkloadCalcStrategy.java` |
| 策略工厂 | `rear/workload-system/**/calc/strategy/CalcStrategyFactory.java` |
| 规则参数 | `rear/workload-system/**/calc/rule/RuleParamService.java` |
| 酬金计算 | `rear/workload-system/**/calc/PayCalcService.java`（+ `allowance/`） |
| 学期汇总 | `rear/workload-system/**/calc/SummaryCalcService.java` |
| G11 生成 | `rear/workload-system/**/calc/ManagementItemGenerator.java` |
| 校历 | `rear/workload-system/**/calc/SemesterCalendar.java` |
| 数据隔离 | `rear/workload-common/**/utils/DataScopeUtil.java` |
| 业务 DDL | `rear/sql/01_biz_schema.sql`（+ `08_review_fixes.sql` 的审计表） |
| 前端入口 | `front/RuoYi-Vue3/src/main.js`（代理见 `vite.config.js`） |
| 权威公式 | `else/工作量.md` |
