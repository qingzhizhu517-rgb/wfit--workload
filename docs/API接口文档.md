# 潍理工教学工作量管理系统 — API 接口文档

> **版本**：v1.0  
> **更新日期**：2026-07-31  
> **基础路径**：`http://localhost:8084`  
> **认证方式**：JWT Bearer Token（Header: `Authorization: Bearer <token>`）  
> **Swagger UI**：http://localhost:8084/swagger-ui.html

---

## 一、项目开发现状总览

### 1.1 后端接口统计

| 统计项 | 数量 |
|-------|------|
| 业务 Controller | 22 个 |
| API 接口总数 | **121 个** |
| 核心业务接口（计算/审批/导出） | 15 个 |
| CRUD 标准接口 | 106 个（19 个实体 × 6 个方法） |

### 1.2 前端页面统计

| 类型 | 数量 | 说明 |
|------|------|------|
| 业务页面 | **19 个** | views/system/ 下的工作量管理页面 |
| Dashboard | **2 个** | AdminDashboard + TeacherDashboard |
| API 接口文件 | **31 个** | api/system/ 下的接口定义 |

### 1.3 里程碑完成状态

| 里程碑 | 内容 | 状态 | 核心接口 |
|--------|------|------|----------|
| M1 | 环境搭建与基建 | ✅ 100% | 18 表 CRUD |
| M2 | 权限与前端骨架 | ✅ 100% | Dashboard、菜单、角色权限 |
| M3 | 核心策略引擎 | ✅ 100% | `/system/calc/*` 7 个端点 |
| M4 | Excel 导入与教师申报 | ✅ 100% | `/system/teachingTask/importExcel`、`declare.vue` |
| M5 | 审批流与报表导出 | ✅ 100% | `/system/audit/*` 6 个端点、`/system/export/*` 2 个端点 |

---

## 二、接口通用约定

### 2.1 统一返回格式

**AjaxResult**（操作类接口）：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": { ... }
}
```

**TableDataInfo**（分页列表接口）：
```json
{
  "code": 200,
  "msg": "查询成功",
  "total": 100,
  "rows": [ ... ]
}
```

### 2.2 分页参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| pageNum | int | 1 | 当前页码 |
| pageSize | int | 10 | 每页条数 |
| orderByColumn | String | - | 排序字段 |
| isAsc | String | asc | 排序方式（asc/desc） |

### 2.3 通用 CRUD 模板

以下 19 个实体均遵循统一的 6 方法模板：

| 方法 | HTTP | 路径 | 说明 |
|------|------|------|------|
| list | GET | `/{entity}/list` | 分页查询 |
| export | POST | `/{entity}/export` | 导出 Excel |
| getInfo | GET | `/{entity}/{id}` | 查询详情 |
| add | POST | `/{entity}` | 新增 |
| edit | PUT | `/{entity}` | 修改 |
| remove | DELETE | `/{entity}/{ids}` | 批量删除 |

---

## 三、核心业务接口

### 3.1 计算引擎 `/system/calc/*`

> **Controller**: `BizCalcController`  
> **职责**: 工作量核算的核心调度，支持单条重算、批量重算、汇总、酬金、一键核算

| # | 方法 | 路径 | 功能 | 参数 | 说明 |
|---|------|------|------|------|------|
| 1 | POST | `/system/calc/recalcItem/{itemId}` | 重算单条明细 | `itemId` (路径) | 根据类别字典的 calc_strategy 动态调用对应策略 bean |
| 2 | POST | `/system/calc/recalcItems` | 批量重算明细 | `userId`, `semester` (Query) | 重算某教师某学期全部未冻结明细 |
| 3 | POST | `/system/calc/recalcSummary` | 重算学期汇总 | `userId`, `semester` (Query) | 落库汇总，计算 G7/G10/总工作量/超额/酬金 |
| 4 | GET | `/system/calc/preview` | 汇总预览 | `userId`, `semester` (Query) | 不落库仿真预览，用于导出前确认 |
| 5 | POST | `/system/calc/recalcPay` | 重算酬金 | `userId`, `semester` (Query) | 需先重算汇总，计算课时酬金+其他酬金 |
| 6 | POST | `/system/calc/genG11` | 生成 G11 | `semester` (必填), `userId` (可选) | 从岗位任职自动生成管理服务明细，支持全量或单人 |
| 7 | POST | `/system/calc/recalcAll` | **一键核算（单教师）** | `userId`, `semester` (Query) | 重算明细 → 汇总 → 酬金，Service 层单事务，失败整体回滚 |
| 8 | POST | `/system/calc/recalcAllBatch` | **批量一键核算** | `semester` (Query) + Body `[userId,...]`（可省略/传空 = 该学期全部有明细的教师） | 逐教师执行「明细→汇总→酬金」，**每人独立事务**：单人失败只记入 `failures`，其余照算。返回 `{successCount, failCount, recalcItemCount, failures:[{userId,userName,reason}]}`。教师角色被 `DataScopeUtil` 强制收敛为只能算自己 |

**核算公式**：

| 类型 | 公式 |
|------|------|
| G1 理论课 | `J1 × C1 × K1 × Q1 × Q2 × Q3 × N` |
| G2 实践课 | `J2 × K × C2 × Q1 × Q2 × Q3` |
| G3 实习实训 | `T × D × K × Q1 × Q2 × Q3` |
| G4 课程设计 | `J4 × min(R4, 20) × 0.4` |
| G5 毕业论文 | `R5 × K5` |
| G6 集中实习 | `W × min(R6, 20) × 0.4` |
| G11 管理服务 | `岗位标准学时 × 任职天数 / 学期天数`（封顶 180） |
| 绩效酬金 | `(min(总工作量, 540) - 180) × 职称单位酬金` |

---

### 3.2 审批流 `/system/audit/*`

> **Controller**: `BizAuditController`  
> **职责**: 学期工作量汇总的三级审批流程管理

**状态机**：
```
0: 填报中 ──submit──→ 1: 教务助理待审 ──approve──→ 2: 院领导待签 ──sign──→ 3: 已完结
       ↑                    │ reject                        │
       └────────────────────┘                               │ unlock
       ↑                                                    │
       └────────────────────────────────────────────────────┘
```

| # | 方法 | 路径 | 功能 | 参数 | 状态流转 |
|---|------|------|------|------|----------|
| 1 | POST | `/system/audit/submit` | 提交审核 | `id` (Query) | 0 → 1 |
| 2 | POST | `/system/audit/approve` | 教务助理通过 | `id` (Query) | 1 → 2 |
| 3 | POST | `/system/audit/reject` | 驳回 | `id`, `reason` (Query) | 1 → 0 |
| 4 | POST | `/system/audit/sign` | 院领导签字 | `id` (Query) | 2 → 3 |
| 5 | POST | `/system/audit/unlock` | 解锁（管理员） | `id` (Query) | 3 → 0 |
| 6 | POST | `/system/audit/batchSubmit` | 批量提交 | `ids` (Query, 逗号分隔) | 0 → 1（批量） |

---

### 3.3 报表导出 `/system/export/*`

> **Controller**: `BizExportController`  
> **职责**: 导出学校规定格式的 Excel 报表

| # | 方法 | 路径 | 功能 | 参数 | 说明 |
|---|------|------|------|------|------|
| 1 | GET | `/system/export/personalWorkload` | 附件1：个人工作量明细表 | `userId`, `semester` | 权限 `system:export:personal`（菜单 20210）。教师角色的 `userId` 被 `DataScopeUtil.resolveUserId` 强制改写为本人，传别人的 ID 无效 |
| 2 | GET | `/system/export/paySummary` | 附件2：绩效酬金统计表 | `semester` | 权限 `system:export:paySummary`（菜单 20211，**教师不持有**）。全院一张表，教师角色调用会被收敛成只有自己一行 |

**前端入口**：原先只挂在各角色仪表盘上；现已同时落到「学期汇总」页 —— 工具栏导出附件2（取搜索栏的学期），行内「附件1」按钮导出该行教师本学期明细。两处都走 `utils/blobDownload.js`，失败时能读出后端返回的 `text/plain` 文本而不是抛一个空 blob。

**附件1 实际列集**（EasyExcel 平铺一张 sheet「工作量明细」，列序 = `PersonalWorkloadDetailExportDTO` 字段声明序，共 19 列）：

```
类别 | 项目名称 | 班级 | 授课层次 | 专业大类 | 基数(学时/天/周/学分) | 人数 |
重复次序 | 重复系数 | 课程类型K1 | 教学质量Q1 | 课程质量Q2 | 全外文Q3 | 合堂系数N |
其他系数(D/K5/K) | 核算工作量 | 系数说明 | 数据来源 | 状态
```

- 「重复次序 / 重复系数 / 班级」是本轮新增的可追溯性三件套：直接回答「这条为什么只算 0.8、是哪个班」。
- 「系数说明」由 `buildCoefRemark` 拼成人话（第几次开课、合堂人数档、是否触封顶），无需再回查 `biz_wl_*`。
- 「数据来源」区分 `IMPORT`(Excel 导入) / `SELF`(教师自主申报) / `AUTO`(G11 自动生成)。
- 无明细时**不写空 Excel**，返回 `text/plain` 提示「未找到该教师该学期的工作量明细」，避免空文件被误读成「本学期没工作量」。

**附件2 实际列集**（sheet「绩效酬金统计」，列序 = `PaySummaryExportDTO` 字段声明序，共 17 列）：

```
工号 | 姓名 | 院部 | 学年学期 | 职称 | 人员性质 | 总工作量 | 额定工作量 | 超额工作量 |
是否触顶 | 计酬超额工作量 | 单位酬金(元) | 绩效酬金(元) | 其他酬金(元) | 总金额(元) | 审批状态 | 备注
```

- 绩效/其他/总金额三列**读 `biz_pay_record` 落库值**（与「酬金记录」页同源），不在导出时重算。
  `biz_pay_record` 是 LEFT JOIN，未核算酬金的教师照样在册，金额列为空即「待核算」。
- 「超额工作量」= 总工作量 − 额定，**不封顶**；触顶(>`CAP_200PCT`)时它不是计酬基数，故并排给出
  「计酬超额工作量」= `min(总工作量, CAP_200PCT) − 额定`。两列并排是为了让「超额 × 单位酬金 ≠ 绩效酬金」
  不被当成算错。封顶线取 `biz_workload_rule` 的 `CAP_200PCT` 当前值，未在 SQL 里写死 540。
- 「备注」写明金额为 0 的原因：人员性质非「专任」不计发 / 已触 200% 上限 / 尚未核算。

---

### 3.4 仪表盘 `/system/dashboard/*`

> **Controller**: `BizDashboardController`  
> **职责**: 首页统计数据

| # | 方法 | 路径 | 功能 | 参数 | 返回数据 |
|---|------|------|------|------|----------|
| 1 | GET | `/system/dashboard/adminStats` | 管理员统计 | `semester` (可选) | taskCount, itemCount, teacherCount, summaryCount, totalWorkload, totalPay, totalExcess, pendingAppeal |
| 2 | GET | `/system/dashboard/teacherStats` | 教师统计 | `semester` (可选) | courseCount, itemCount, totalWorkload, excessWorkload, performancePay, quotaWorkload, auditStatus, appealCount |
| 3 | GET | `/system/dashboard/collegeStats` | 院系统计 | `semester` (可选) | 各学院任务数、明细数（柱图/折线图数据） |

---

### 3.5 教学任务导入 `/system/teachingTask/*`

> **Controller**: `BizTeachingTaskController`  
> **职责**: 教学任务管理 + Excel 导入

| # | 方法 | 路径 | 功能 | 参数 | 说明 |
|---|------|------|------|------|------|
| 1 | GET | `/system/teachingTask/list` | 查询列表 | Query 参数绑定 | 分页 |
| 2 | POST | `/system/teachingTask/importExcel` | **Excel 导入** | `file` (MultipartFile) | 自动解析并创建工作量明细 |
| 3 | POST | `/system/teachingTask/importTemplate` | 下载导入模板 | - | 返回 Excel 模板文件（17 列，列序见下） |
| 4 | POST | `/system/teachingTask/export` | 导出列表 | Query 参数绑定 | 返回 Excel |
| 5 | GET | `/system/teachingTask/{id}` | 查询详情 | `id` (路径) | |
| 6 | POST | `/system/teachingTask` | 新增 | JSON Body | |
| 7 | PUT | `/system/teachingTask` | 修改 | JSON Body | |
| 8 | DELETE | `/system/teachingTask/{ids}` | 批量删除 | `ids` (路径) | |

**导入模板 17 列**（列序 = `TeachingTaskImportDTO` 的 `@ExcelProperty` 声明序；EasyExcel 按表头名匹配，列可换位但名不能改）：

```
学年学期 | 教师工号 | 教师姓名 | 课程名称 | 课程代码 | 工作量类别 | 授课层次 | 专业大类 |
课程性质 | 课程级别 | 课程角色 | 教学评价 | 选课人数 | 计划学时/天数/周数 | 课程系数 |
班级 | 重复次序
```

- 必填：`学年学期`(须形如 `2025-2026-1`)、`教师工号`(须已存在于 `sys_user`)、`课程名称`、`工作量类别`(G1~G6)、`计划学时/天数/周数`(>0)。其余留空走默认值（授课层次=本科、专业大类=理工类、课程性质=必修…）。
- **`班级`**：落 `biz_teaching_task.class_name`，是附件1「系数说明」能指名到具体班次的唯一来源。同一门课的不同班务必填写，否则导出只能看到课程名。
- **`重复次序`**：可选，决定重复系数 C1(G1) / K(G3) 取第几档。**填了以填的为准**（教务可人工指定哪个班算第一次）；留空则按 `countSameCourseTask` 的同组已入库条数 +1 自动补位。分组口径 = 教师 + 学年学期 + 课程名称 + 授课层次 + 工作量类别（**不含课程代码与班级**，与 `else/工作量.md:15-18` 一致）。填 0 或负数视为笔误直接报错，不静默当 1。
- 自动补位依赖流式导入的「逐行独立事务、顺序提交」，因此分两次导入同一门课的不同班级也能正确续算（计数走库，不走批次内存）。

---

## 四、基础数据接口（CRUD 模板）

### 4.1 教师档案 `/system/teacherProfile/*`

**主键**: `userId`

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户ID（关联 sys_user） |
| title | String | 职称（教授/副教授/讲师/助教） |
| category | String | 人员类别（专任/外聘/校企/行政） |
| teacherNature | String | 教师性质（专任/外聘/青州外聘/校企/银龄） |
| minWorkload | BigDecimal | 学年额定工作量（学时） |
| specialStatus | String | 特殊状态（正常/产假/在职读博/访学） |

---

### 4.2 岗位任职 `/system/roleAssignment/*`

**主键**: `id`

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 教师ID |
| roleName | String | 岗位名称（班主任/教研室主任/系主任等） |
| standardHours | BigDecimal | 岗位标准学时 |
| startDate | Date | 任职开始日期 |
| endDate | Date | 任职结束日期 |
| academicYear | String | 学年 |

---

### 4.3 工作量明细主表 `/system/workloadItem/*`

**主键**: `id`

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 教师ID |
| semester | String | 学年学期（如 2025-2026-1） |
| categoryCode | String | 工作量类别（G1-G11） |
| courseName | String | 课程/项目名称 |
| baseValue | BigDecimal | 基数学时/天数/学分 |
| finalWorkload | BigDecimal | 最终核算工作量 |
| frozen | Integer | 是否冻结（0否 1是） |
| sourceType | String | 来源（IMPORT/SELF/GENERATOR） |

---

### 4.4 工作量类别子表（7 个）

每个子表均遵循 CRUD 模板，主键为 `itemId`（关联 workload_item.id）。

#### G1 理论课 `/system/wlTheory/*`

| 字段 | 说明 |
|------|------|
| theoryHours | 理论学时 J1 |
| coefRepeat | 重复系数 C1（1.0/0.9/0.8） |
| coefType | 课程类型 K1（必修1.1/选修1.0） |
| coefQualityTeach | 教学质量 Q1（优秀/良好1.0/不合格0.8） |
| coefQualityCourse | 课程质量 Q2（省级一流1.5/校级精品1.2/其他1.0） |
| coefForeignLang | 全外文课程 Q3 |
| coefClassSize | 合堂系数 N（120-150人→1.1/151+→1.2） |
| educationLevel | 授课层次（本科/专科） |
| majorCategory | 专业大类（理工/文史/艺术） |
| courseNature | 课程性质（必修/选修） |
| courseLevel | 课程级别（省级一流/校级精品/其他） |
| courseRole | 课程角色（主持人/团队前3/独立） |
| teachingEval | 教学评价（优秀/良好/合格/不合格） |

#### G2 课内实践 `/system/wlPractice/*`

| 字段 | 说明 |
|------|------|
| practiceHours | 实践学时 J2 |
| coefMajor | 专业类别 K（理工1.0/其他0.9） |
| coefRepeat | 重复系数 C2（固定0.9） |

#### G3 实习实训 `/system/wlInternshipTraining/*`

| 字段 | 说明 |
|------|------|
| actualDays | 实际天数 T（每天8学时） |
| coefGuide | 指导系数 D（理工4/艺术3/文史2） |
| coefRepeat | 重复系数 K（首轮1/后续0.9） |

#### G4 课程设计 `/system/wlCourseDesign/*`

| 字段 | 说明 |
|------|------|
| credits | 学分 J4 |
| studentCount | 指导人数 R4（上限20） |
| perCreditConst | 每学分常量（默认0.4） |

#### G5 毕业论文 `/system/wlThesis/*`

| 字段 | 说明 |
|------|------|
| studentCount | 指导人数 R5（本科≤10/专科≤15） |
| coefType | 系数 K5（理工本9/专5，文史本6/专4） |
| educationLevel | 层次（本科/专科） |
| majorCategory | 科类（理工/文史） |

#### G6 集中实习 `/system/wlConcentratedInternship/*`

| 字段 | 说明 |
|------|------|
| weeks | 实习周数 W |
| studentCount | 指导人数 R6（上限20） |
| perWeekConst | 每周常量（默认0.4） |

#### G11 管理服务 `/system/wlManagement/*`

| 字段 | 说明 |
|------|------|
| roleAssignmentId | 关联岗位任职ID |
| roleName | 岗位名称 |
| standardHours | 岗位标准学时 |
| proratedAmount | 折算学时（按任职天数/学期天数） |

---

### 4.5 学期汇总 `/system/workloadSummary/*`

**主键**: `id`，**唯一约束**: `userId + semester`

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 教师ID |
| semester | String | 学年学期 |
| quotaWorkload | BigDecimal | 额定工作量 |
| totalTeachingWork | BigDecimal | 教学工作量（G7） |
| totalAdminWork | BigDecimal | 管理服务工作量（G11） |
| totalWorkload | BigDecimal | 总工作量（G10+G11） |
| excessWorkload | BigDecimal | 超额工作量 |
| performancePay | BigDecimal | 绩效酬金 |
| auditStatus | Integer | 审批状态（0填报中/1待审/2待签/3已完结/-1驳回） |
| assistantSign | String | 教务助理签字账号 |
| leaderSign | String | 院领导签字账号 |
| categoryDetails | JSON | 各类别动态汇总 `{"G1":150, "G8":20}` |
| groupDetails | JSON | 各大类动态汇总 `{"TEACHING":150, "ADMIN":20}` |

---

### 4.6 酬金管理

#### 酬金汇总 `/system/payRecord/*`

| 字段 | 说明 |
|------|------|
| userId | 教师ID |
| semester | 学年学期 |
| teachingPay | 课时酬金 |
| performancePay | 绩效酬金 |
| totalPay | 总酬金 |
| payStatus | 发放状态 |

#### 其他酬金明细 `/system/allowanceItem/*`

| 字段 | 说明 |
|------|------|
| userId | 教师ID |
| semester | 学年学期 |
| feeType | 费用类型（A重修辅导/B毕业实习/C论文重修/D代阅卷/E讲座/F运动会裁判/G夜间值班） |
| subType | 子类型 |
| baseCount | 核算基数 |
| unitPrice | 单价 |
| totalAmount | 总金额 |

#### 酬金费率 `/system/payRate/*`

| 字段 | 说明 |
|------|------|
| title | 职称 |
| unitPrice | 单位工作量酬金（教授70/副60/讲50/助40） |
| effectiveStart | 生效开始日期 |
| effectiveEnd | 生效结束日期 |

---

### 4.7 配置管理

#### 类别字典 `/system/workloadCategoryDict/*`

**主键**: `typeCode`（G1-G11）

| 字段 | 说明 |
|------|------|
| typeCode | 分类代码（G1/G2/.../G11） |
| typeName | 分类名称 |
| parentGroup | 所属大类（TEACHING/ADMIN/EXTRA） |
| calcStrategy | 关联的 Java 策略 Bean 名称 |
| isCalcExcess | 是否计入超额核算 |

#### 核算规则 `/system/workloadRule/*`

| 字段 | 说明 |
|------|------|
| ruleCode | 参数键名（如 COEF_CLASS_120_150） |
| ruleValue | 参数数值（如 1.1） |
| ruleDesc | 参数说明 |

#### 导入批次 `/system/importBatch/*`

| 字段 | 说明 |
|------|------|
| batchNo | 批次号 |
| importType | 导入类型 |
| fileName | 文件名 |
| totalCount | 总记录数 |
| successCount | 成功数 |
| failCount | 失败数 |
| status | 状态 |

---

## 五、前端页面清单

### 5.1 Dashboard 首页

| 组件 | 路径 | 功能 | 角色 |
|------|------|------|------|
| `AdminDashboard.vue` | `views/dashboard/` | 4 统计卡片 + ECharts 柱/折图 + 待办列表 | admin, jiaowu |
| `TeacherDashboard.vue` | `views/dashboard/` | 欢迎卡 + 数据卡 + 近期明细 + 达标面板 + 申诉入口 | teacher |

首页 `views/index.vue` 通过 `v-hasRole` 指令按角色分发渲染。

### 5.2 业务管理页面（19 个）

| # | 页面 | 路径 | 功能 | 关键操作 |
|---|------|------|------|----------|
| 1 | 教师档案 | `views/system/teacherProfile/` | 教师业务档案管理 | 增删改查导出 |
| 2 | 教学任务 | `views/system/teachingTask/` | 教学任务管理 | 增删改查、**Excel导入**、导出模板 |
| 3 | 岗位任职 | `views/system/roleAssignment/` | 岗位任职管理 | 增删改查导出 |
| 4 | 数据导入 | `views/system/importBatch/` | 导入批次记录 | 查看、删除批次 |
| 5 | 工作量明细 | `views/system/workloadItem/` | 工作量明细主表 | 增删改查导出、**重算学期明细** |
| 6 | G1 理论课 | `views/system/wlTheory/` | G1 理论课明细 | 增删改查导出 |
| 7 | G2 课内实践 | `views/system/wlPractice/` | G2 课内实践明细 | 增删改查导出 |
| 8 | G3 实习实训 | `views/system/wlInternshipTraining/` | G3 实习实训明细 | 增删改查导出 |
| 9 | G4 课程设计 | `views/system/wlCourseDesign/` | G4 课程设计明细 | 增删改查导出 |
| 10 | G5 毕业论文 | `views/system/wlThesis/` | G5 毕业论文明细 | 增删改查导出 |
| 11 | G6 集中实习 | `views/system/wlConcentratedInternship/` | G6 集中实习明细 | 增删改查导出 |
| 12 | G11 管理服务 | `views/system/wlManagement/` | G11 管理服务明细 | 增删改查导出 |
| 13 | 学期汇总 | `views/system/workloadSummary/` | 学期工作量汇总 | **一键核算**、汇总预览、生成G11、**批量提交审核**、导出 |
| 14 | 酬金记录 | `views/system/payRecord/` | 酬金汇总 | 增删改查导出、**重算酬金** |
| 15 | 其他酬金 | `views/system/allowanceItem/` | 其他酬金明细（A-G） | 增删改查导出 |
| 16 | **自主申报** | `views/system/myWorkload/declare.vue` | 教师自主申报 | G8/G9/G11 申报提交 |
| 17 | 类别字典 | `views/system/workloadCategoryDict/` | 类别字典管理 | 增删改查 |
| 18 | 计算规则 | `views/system/workloadRule/` | 核算规则参数 | 增删改查导出 |
| 19 | 酬金标准 | `views/system/payRate/` | 酬金费率管理 | 增删改查导出 |

### 5.3 RuoYi 内置页面

user、role、menu、dept、dict、config、notice、post 等系统管理页面（框架自带）。

---

## 六、前端 API 文件清单（31 个）

### 6.1 核心业务 API

| 文件 | 方法 | HTTP | URL | 说明 |
|------|------|------|-----|------|
| **calc.js** | `recalcItem(itemId)` | POST | `/system/calc/recalcItem/{itemId}` | 重算单条 |
| | `recalcItems(userId, semester)` | POST | `/system/calc/recalcItems` | 批量重算 |
| | `recalcSummary(userId, semester)` | POST | `/system/calc/recalcSummary` | 重算汇总 |
| | `previewSummary(userId, semester)` | GET | `/system/calc/preview` | 预览汇总 |
| | `recalcPay(userId, semester)` | POST | `/system/calc/recalcPay` | 重算酬金 |
| | `genG11(semester, userId)` | POST | `/system/calc/genG11` | 生成G11 |
| | `recalcAll(userId, semester)` | POST | `/system/calc/recalcAll` | 一键核算（单教师） |
| | `recalcAllBatch(semester, userIds)` | POST | `/system/calc/recalcAllBatch` | 批量核算（`userIds` 传空数组 = 全学期） |
| **audit.js** | `auditSubmit(id)` | POST | `/system/audit/submit` | 提交审核 |
| | `auditApprove(id)` | POST | `/system/audit/approve` | 审核通过 |
| | `auditReject(id, reason)` | POST | `/system/audit/reject` | 驳回 |
| | `auditSign(id)` | POST | `/system/audit/sign` | 院领导签字 |
| | `auditUnlock(id)` | POST | `/system/audit/unlock` | 解锁 |
| | `auditBatchSubmit(ids)` | POST | `/system/audit/batchSubmit` | 批量提交 |
| **export.js** | `exportPersonalWorkload(params)` | GET | `/system/export/personalWorkload` | 附件1导出 |
| | `exportPaySummary(params)` | GET | `/system/export/paySummary` | 附件2导出 |
| **dashboard.js** | `getAdminStats(semester)` | GET | `/system/dashboard/adminStats` | 管理员统计 |
| | `getTeacherStats(semester)` | GET | `/system/dashboard/teacherStats` | 教师统计 |
| | `getCollegeStats(semester)` | GET | `/system/dashboard/collegeStats` | 院系统计 |

### 6.2 CRUD 标准 API（每个均提供 list/get/add/update/del 五个方法）

| 文件 | URL 前缀 | 业务 |
|------|----------|------|
| teacherProfile.js | `/system/teacherProfile` | 教师档案 |
| teachingTask.js | `/system/teachingTask` | 教学任务（+importExcel/importTemplate） |
| roleAssignment.js | `/system/roleAssignment` | 岗位任职 |
| workloadItem.js | `/system/workloadItem` | 工作量明细主表 |
| workloadSummary.js | `/system/workloadSummary` | 学期汇总 |
| workloadCategoryDict.js | `/system/workloadCategoryDict` | 类别字典 |
| workloadRule.js | `/system/workloadRule` | 核算规则 |
| payRate.js | `/system/payRate` | 酬金费率 |
| payRecord.js | `/system/payRecord` | 酬金汇总 |
| allowanceItem.js | `/system/allowanceItem` | 其他酬金 |
| importBatch.js | `/system/importBatch` | 导入批次 |
| wlTheory.js | `/system/wlTheory` | G1 理论课 |
| wlPractice.js | `/system/wlPractice` | G2 课内实践 |
| wlInternshipTraining.js | `/system/wlInternshipTraining` | G3 实习实训 |
| wlCourseDesign.js | `/system/wlCourseDesign` | G4 课程设计 |
| wlThesis.js | `/system/wlThesis` | G5 毕业论文 |
| wlConcentratedInternship.js | `/system/wlConcentratedInternship` | G6 集中实习 |
| wlManagement.js | `/system/wlManagement` | G11 管理服务 |

### 6.3 RuoYi 框架原生 API

| 文件 | 说明 |
|------|------|
| user.js | 用户管理 |
| role.js | 角色管理 |
| menu.js | 菜单管理 |
| dept.js | 部门管理 |
| post.js | 岗位管理 |
| config.js | 系统配置 |
| notice.js | 通知公告 |
| dict/data.js | 字典数据 |
| dict/type.js | 字典类型 |

---

## 七、业务流程与接口调用链

### 7.1 完整核算流程（一键核算）

```
前端: recalcAll(userId, semester)
        ↓
后端: WorkloadCalcService.recalcItems(userId, semester)
        ↓ 遍历该教师该学期所有未冻结 workload_item
        ↓ 根据 category_dict.calc_strategy 调用对应 Strategy
        ↓ 读取 RuleParamService 的规则参数（Redis 缓存）
        ↓ 计算 final_workload，更新 biz_workload_item + biz_wl_* 明细
      SummaryCalcService.recalcSummary(userId, semester)
        ↓ 按类别 GROUP BY 汇总（结果不落 JSON，biz_workload_summary 无 category_details 列）
        ↓ 计算 G7/G10/G11(封顶180)/总工作量/超额(不封顶)/绩效(按 min(总量,540)−180，仅专任) → 更新 biz_workload_summary
      PayCalcService.recalcPay(userId, semester)
        ↓ 读取 pay_rate 获取职称单价
        ↓ 计算课时酬金 + 其他酬金(A-G) → 写入 biz_pay_record
        ↓ 返回完整结果
```

### 7.2 Excel 导入流程

```
前端: importExcel(file) — FormData 上传
        ↓
后端: TeachingTaskImportListener 逐行解析
        ↓ 根据 workloadType 匹配 category_dict.calc_strategy
        ↓ 调用 DispatcherService 计算
        ↓ 生成 biz_workload_item + biz_wl_* 明细
        ↓ 生成 biz_import_batch 记录
        ↓ 返回导入结果（成功/失败/跳过）
```

### 7.3 审批流程

```
教师/教务员: auditSubmit(id) → 0→1
        ↓
教务助理: auditApprove(id) → 1→2  或  auditReject(id, reason) → 1→0
        ↓
院领导: auditSign(id) → 2→3（完结锁定）
        ↓
管理员（异常处理）: auditUnlock(id) → 3→0（重新开放）
```

### 7.4 报表导出流程

```
附件1  前端: exportPersonalWorkload({userId, semester}) — responseType: blob
        ↓ DataScopeUtil.resolveUserId(userId)（教师收敛为本人）
        ↓ BizExportMapper.selectPersonalWorkloadExport：workload_item LEFT JOIN 6 张 wl_* 明细 + teaching_task（取班级/重复次序）
        ↓ 空结果 → 直接写 text/plain「未找到该教师该学期的工作量明细」并 return（不产出空 Excel）
        ↓ 逐行 buildCoefRemark(dto) 生成「系数说明」人话
        ↓ EasyExcel 写 19 列平铺 sheet → 工作量明细_<姓名>_<学期>.xlsx

附件2  前端: exportPaySummary({semester})
        ↓ BizExportMapper.selectPaySummaryExport(semester, resolveUserId(null))
          summary LEFT JOIN pay_record（金额读落库值，不重算）LEFT JOIN teacher_profile（取人员性质）
        ↓ cap200 = RuleParamService.get("CAP_200PCT", 540)
        ↓ 逐行 fillPayExplain(dto, cap200)：算「计酬超额工作量」+ 写「备注」（非专任/已触顶/未核算）
        ↓ EasyExcel 写 17 列平铺 sheet → 绩效酬金统计_<学期>.xlsx
```

---

## 八、已知问题与待优化

| # | 问题 | 影响范围 | 优先级 |
|---|------|----------|--------|
| 1 | ~~collegeStats 返回空数组~~ | ✅ 已修复：执行 `12_fix_dept_mapping.sql` 补 `sys_dept` 数据 | - |
| 2 | G8/G9 无自动计算策略 | 第二课堂/其他工作量需手动录金额 | 低 |
| 3 | 代阅卷酬金 D 档位待确认 | 其他酬金明细 | 低 |
| 4 | Q3 全外文课程系数待确认 | G1 理论课计算 | 低 |
| 5 | G5 艺术类 K5 映射待确认 | G5 毕业论文计算 | 低 |

---

**文档完毕。**
