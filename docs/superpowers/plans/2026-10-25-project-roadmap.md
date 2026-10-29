# WFIT 后续工作 Implementation Plan

> **For agentic workers:** 使用 `executing-plans` 技能按任务执行并保留验证检查点；用户已于2026-10-25授权推进，当前在隔离工作区实施；逐项进度见task_plan.md。不自动合并原main、清库或发布。任务步骤使用复选框跟踪。

**Goal:** 把本地补丁、远程功能和数据库整合为一致基线，补齐审核与导入闭环，以可复现的对账、并发和发布证据完成一期验收。

**Architecture:** 保持现有四模块单体与Vue3前端；继续使用策略引擎、MyBatis和教师+学期事务边界。先建立唯一代码/结构基线，再完善历史版本、明细审核批次与结果追溯，最后开展完整业务验收。

**Tech Stack:** Java17、Spring Boot4.0.7、MyBatis、MySQL、Redis、Vue3、Vite6、Element Plus、JUnit5/Mockito；复用现有POI/EasyExcel及远程Node单元测试。

---

## 0. 执行规则、分工及产出目录

项目根目录：`/Users/a1/Desktop/wfit--workload`。以下文件均相对该目录，执行者需先核对其存在性及整合后的变化。新建迁移文件名必须在T02确认后分配；本计划用明确职责描述候选文件，不抢占可能冲突的数字。

- 技术负责人：T01版本整合、T04质量门禁、T13发布与文档；后端/数据负责人：T02、T03、T05—T07；前端负责人：T08、T09、T14；验证负责人：T10、T11；环境管理员：T12。单人执行时这些是责任角色，不要求增设团队。
- 现状报告：`docs/项目现状核查-2026-10-25.md`。
- 长期证据：在`docs/验收/`按任务建立Markdown记录，记录commit、迁移清单、工具版本、命令、结果、日期和证据文件，不记录明文凭据或真实人员隐私。
- 每个任务以一个可复核提交或PR收口；仅提交任务相关文件。提交与推送须遵循届时授权，不在本次规划中执行。
- 先用现有测试复现风险；证据支持的缺陷才进入修改。文档、基线清单等低风险工作不增加镜像实现的测试。
- 未确认业务问题沿用`docs/教务工作量16项需求-待老师确认.md`允许的默认值，正式结算前记录生效学期和异常人员归类；已确认原则不重问。

## 1. 优先级、依赖与粗估

P0：阻塞安全整合或共享环境使用；P1：一期功能/验收必要项；P2：收敛工程债务、按测量优化。估算是单人有效工作日，不包含外部业务确认等待；需在T03结束后重估。

| ID | 任务 | 优先级 | 依赖 | 粗估/日 | 完成产出 |
|---|---|---|---|---:|---|
| T01 | 保全并整合代码基线 | P0 | 无 | 0.5—1 | 可恢复快照、整合分支、差异处置记录 |
| T02 | 数据库备份、迁移清单与副本升级 | P0 | T01 | 1—2 | 可恢复备份、唯一迁移清单、升级/恢复记录 |
| T03 | 写入冻结、核对与并发收尾 | P0 | T01、T02 | 1.5—3 | 受保护的统一基线、真实多连接回归 |
| T04 | 固定工具链及CI基线 | P0 | T01 | 1—2 | JDK17/Node固定版本、自动门禁和可见lint基线 |
| T05 | 规则费率版本与历史追溯 | P1；历史重算前关闭 | T02、T03 | 2—4 | 按期选值、真实版本绑定、快照校验与历史测试 |
| T06 | 审核/补差数据契约与统一审计 | P1 | T03、T05 | 1.5—2.5 | 定稿契约、迁移、审计填充及安全测试 |
| T07 | 明细审核批次、汇总门禁及补差API | P1 | T06 | 2—3 | 审核服务、分页详情、独立补差说明 |
| T08 | 审核中心与汇总详情前端 | P1 | T07 | 1.5—3 | 分类审核页面、来源统计、时间线 |
| T09 | 岗位减免导入和花名册预检页面 | P1 | T02、T03 | 1—2 | 可操作预检/确认及异常回执 |
| T10 | 导入→核算→审核→酬金→导出整链验收 | P1 | T05、T07、T08、T09 | 2—3 | 一组固定预期的数据库/API/浏览器/Excel对账 |
| T11 | 规模、权限和故障恢复验收 | P1 | T10 | 1—2 | 整学期规模报告与故障恢复证据 |
| T12 | 凭据、账号、监控与部署配置收口 | P0（对外前） | T01 | 0.5—1 | 轮换验证记录、安全环境清单 |
| T13 | 发布演练、操作手册与最终签收 | P1 | T04、T10、T11、T12、T14 | 1—2 | 发布包、回退演练、业务签收清单 |
| T14 | lint债务清理与针对性性能优化 | P2；关键功能错误前移T04 | T04；性能优化依赖T11 | 2—4 | 全量lint零错误、必要的性能改进证据 |

合计约18.5—34.5个有效工作日，仅供排期。T04、T12可在代码基线完成后独立推进；T09与审核后端实现相互独立。关键功能链为T01→T02→T03→T05→T06→T07→T08→T10→T11→T13；T14是正式交付的工程质量门禁。

## 2. 任务步骤与验收

### T01：保全并整合代码基线

**涉及文件：** 既有27个修改文件及11个新增文件；重点`rear/workload-system/src/main/java/com/workload/system/service/impl/TeachingTaskImportServiceImpl.java`、相关输入契约测试、`rear/workload-system/pom.xml`；新增`docs/验收/版本整合.md`。

- [x] 记录`git status --porcelain=v1 -uall`、分支SHA和未跟踪文件清单；保存包含未跟踪文件的可恢复工作快照，敏感且被忽略文件单独保存在本机受限位置。
- [x] 检查已有工作区后创建或复用`codex/`整合分支/隔离工作区，基于最新origin/main；保留原工作树用于比对。
- [x] 按“快照上下文→核对/审批门禁→通用写入保护→导入校验→页面/测试”逐组移植，不直接覆盖远程新版导入实现。
- [x] 对照远程数量字段、预检规范化和确认回放，把本地默认值与正数校验收敛到共享输入契约；验证两阶段路径也包含这些校验。
- [x] 运行后端常规测试、远程已有前端单元测试和生产构建，记录整合SHA及各组变更去向。

**验收：** 25个远程提交已纳入整合基线，原38个本地开发文件均有保留/替代说明；没有仅因文本合并成功就跳过语义验证；原快照可恢复。产出可运行性还依赖T02，不能先启动缺表的新版应用。

### T02：数据库备份、迁移清单与副本升级

**涉及文件：** `rear/sql/01_biz_schema.sql`、远程`rear/sql/28_import_framework.sql`、本地`rear/sql/28_snapshot_context.sql`及`29_item_review_permission.sql`、`docs/计算快照上下文格式与升级.md`；新增`rear/sql/MIGRATIONS.md`、`docs/验收/数据库升级与恢复.md`。

- [ ] 只读核对目标host/port/schema/version、表数、关键行数和schema指纹。当前MySQL9.0.1与文档MySQL8分别列出，确定实际部署支持版本并在其上验证。
- [x] 生成结构+数据备份，记录时间和SHA-256，备份存放仓库之外；在独立数据库恢复并检查关键表数和行数。恢复成功才进入升级演练。
- [x] 以文件名、用途、内容哈希、前置条件和后置查询为键整理迁移清单；区分fresh脚本与增量脚本，明确03非幂等。检查22/28号历史冲突，不根据文件前缀猜执行状态。
- [x] 统一28导入/快照及29权限的后续命名与引用；为可能已执行的旧名字记录等价映射与结构探测，保留原迁移内容追溯。
- [x] 在恢复副本按清单升级；对声明幂等的脚本重复执行，验证表/列/索引/角色授权及业务行数无意外变化；保存实际应用清单，不在本期强制引入全新迁移框架。
- [ ] 演练旧应用回退时保留新增可空列/历史快照；如需数据恢复，证明从备份恢复到独立目标可用。

**验收：** 副本同时满足导入、快照和权限契约；`biz_import_row`、7个导入批次新列、5个任务数量新列、`context_json`及核对权限可查询；每条迁移有可复验状态。不得直接以含DROP的fresh脚本升级现有库。


### T03：写入冻结、核对与并发收尾

**涉及文件：** `calc/WorkloadWriteGuard.java`、`calc/WorkloadCalcServiceImpl.java`、`calc/PayCalcServiceImpl.java`，`service/impl/{BizWorkloadItemServiceImpl,BizWorkloadSummaryServiceImpl,BizPayRecordServiceImpl,BizAuditServiceImpl,CoefficientAdjustmentServiceImpl,TeachingTaskImportServiceImpl}.java`及对应Mapper XML；以上位于`rear/workload-system/src/main/java/com/workload/system/`。测试复用`src/test/java/com/workload/system/integration/CoefficientApprovalTransactionIT.java`，新增`WorkloadConcurrencyIT.java`与`SourceChangeConsistencyIT.java`于同integration目录。

- [x] 建立“源任务/子表/G8G9/G11/系数/汇总/酬金 × 草稿/待审/完结”的写入口矩阵，覆盖CRUD、导入确认、生成器、旧兼容端点与批处理。
- [x] 在隔离数据库复现源变更/子表删除后汇总是否陈旧；如存在缺口，采用统一数据版本/需重算标识或原子重算，阻止陈旧结果提交，不改变已完结记录。
- [x] 用两个物理连接和显式同步屏障测试“系数批准vs汇总提交”“核算vs审核完结”“两个确认同一版本”“首次生成汇总vs另一写入”。断言只允许一个合法结果或明确冲突，不能出现混合因子、重复快照或部分提交。
- [x] 真实接入完整核算服务重跑失败回滚；保留现有14项临时表IT作为窄回归，不能用替身覆盖宣称整链通过。
- [x] 验证所有角色不能通过通用编辑篡改计算列、归属、签字；冻结记录批删全批回滚。清理前端无效解锁和手改结果入口。

**验收：** 待审/完结不静默改数，核对时版本一致；源变更后的汇总新鲜度可见且可执行约束；多连接测试重复运行稳定。只修复已复现缺口，避免借此重构整个计算引擎。

### T04：固定工具链及CI基线

**涉及文件：** `rear/pom.xml`、`front/RuoYi-Vue3/package.json`及锁文件、`.eslintrc.cjs`、`README.md`、`AGENTS.md`、`CLAUDE.md`；新增`.github/workflows/verify.yml`、`.java-version`、`.nvmrc`、`docs/验收/质量门禁.md`。

- [ ] 记录并固定已验证的JDK17和Node版本，使用`npm ci`复现依赖；确保CI实际使用目标JDK而不是本机默认Java26。
- [ ] 后端执行常规测试；前端执行整合后已有`npm run test:unit`和生产构建，上传测试结果，全部要求退出0。
- [ ] 运行完整lint并按规则/文件分类；当前1477错误作为审计起点，新增代码不得增加错误。对登录、权限、审核、导入、导出关键路径的未定义变量/副作用等先写复现并关闭；格式清理移交T14。
- [ ] 将lint债务作为可见失败项或明确独立债务检查，不把忽略规则、删除规则或只检查改动文件冒充全量lint通过。
- [ ] 修正生产`VITE_APP_TITLE`配置；更新指导文档的测试命令、公式、G11申报边界、状态和迁移引用。

**验收：** 干净环境能复现测试/构建，新增代码受门禁约束；真实MySQL IT独立配置专用测试库，连接缺失不能记作通过。全量lint最终清零由T14负责，正式发布前必须关闭。

### T05：规则费率版本与历史追溯

**2026-10-29状态：** 日期选值/缓存/有效期LocalDate契约第一切片通过，570常规＋31MySQL＋双时区40条HTTP。下列来源ID/不可变版本、结算上下文和快照哈希要求仍未完成；日期部分完成不勾选包含来源要求的整项。详见`docs/验收/T05日期选值与缓存.md`。

**涉及文件：** `rear/workload-system/src/main/java/com/workload/system/calc/rule/{RuleParamService,RuleParamServiceImpl}.java`、`calc/SummaryCalcServiceImpl.java`、`calc/WorkloadCalcServiceImpl.java`、`service/WorkloadSnapshotService.java`、`service/SnapshotContextCodec.java`、`service/impl/{WorkloadFactorFormulaServiceImpl,BizWorkloadCategoryDictServiceImpl,BizWorkloadRuleServiceImpl}.java`及规则/费率Mapper；测试新增`calc/rule/HistoricalRuleResolutionTest.java`、`service/SnapshotIntegrityTest.java`。

- [ ] 固定业务口径：沿用未回复默认“学期结束日生效版本”，在结算配置中记录生效学期；对行政/兼岗和人员性质为空等异常不自动猜测结算资格。
- [ ] 让规则和费率读取显式接收asOfDate/版本，返回值同时给出不可变来源ID/版本；缓存包含日期/版本维度，覆盖未来生效、过期和改名失效。
- [x] 复用已有规则CRUD失效能力并补事务/时间切换测试；字典改绑策略时驱逐对应StrategyCache，避免把已修复的规则evict重复开发。
- [ ] 将真实版本来源写入明细快照，替换仅`RULE-学期`的伪版本含义；固化汇总/酬金使用的规则、费率、人员性质和明细版本集合。
- [ ] 实现按formatVersion读取校验哈希，兼容旧哈希格式；损坏/未知版本明确报错，缺少的历史字段明确标记，不用当前源数据回填冒充历史。
- [ ] 测试旧/新政策跨生效日、同学期重新计算、来源记录后续修改、缺费率、旧快照读取及篡改检测；已完结结果不因配置变化被重写。

**验收：** 同一历史版本在当前日期改变后仍可解释并重现；版本不是标签而有真实不可变依据；历史结算风险R4—R6有关闭证据。

### T06：审核/补差数据契约与统一审计

**涉及文件：** 修订`docs/superpowers/plans/2026-09-12-workload-audit-center.md`，新增`docs/验收/审核契约.md`；`rear/sql/MIGRATIONS.md`、`rear/sql/01_biz_schema.sql`、`rear/sql/05_biz_menu.sql`、`rear/sql/06_test_accounts.sql`；新增`rear/workload-common/src/main/java/com/workload/common/core/domain/AuditableEntity.java`、`rear/workload-framework/src/main/java/com/workload/framework/mybatis/BizAuditFieldInterceptor.java`，修改同framework/config下`MyBatisConfig.java`及common/BaseEntity。审计测试放`rear/workload-framework/src/test/java/com/workload/framework/mybatis/BizAuditFieldInterceptorTest.java`。

- [ ] 先解决现状与旧计划的阶段差异：草稿态教务核对是提交前检查；待审态审核批次是审批证据。两者分别命名和授权，不把已有item.status复用为审核批次result。
- [ ] 固定审核对象为workload_item，分类仅教学任务/人工申报/G11/超限；其他酬金不塞入多态审核表；result采用PENDING/PASS/REJECT/WAIVE并定义豁免权限。
- [ ] 定义summary数据版本、审核批次快照、唯一键、并发版本和分页上限100；驳回原因必填。另建只追加的线下补差说明，关联完结汇总但不修改原工作量/酬金。
- [ ] 在T02迁移清单中分配空闲编号并编写增量和fresh结构，验证重复执行、历史记录保留和权限最小化。
- [ ] 自动审计填充仅面向约定的biz实体/参数，服务端覆盖客户端伪造操作者；测试单对象、集合、Mapper直调、匿名系统来源和线程上下文清理。

**验收：** 数据契约能同时解释“提交前核对”与“待审审核”；迁移无22/28编号碰撞；系统任务与用户动作可追溯；不因统一填充破坏手工固定历史字段。

### T07：明细审核批次、汇总门禁与补差API

**涉及文件：** 在system业务包新增`domain/BizWorkloadItemReview.java`、`mapper/BizWorkloadItemReviewMapper.java`、`service/BizWorkloadItemReviewService.java`、`service/impl/BizWorkloadItemReviewServiceImpl.java`、`controller/BizWorkloadItemReviewController.java`、`service/WorkloadSummaryDetailService.java`及impl；新增对应`resources/mapper/system/BizWorkloadItemReviewMapper.xml`。补差说明使用独立`BizOfflineAdjustmentNote`实体/Mapper/Service/Controller及XML。修改`BizAuditServiceImpl`、`BizWorkloadSummaryController`、`BizAuditLogMapper`；测试新增同名Service/Controller测试。

- [ ] 提交汇总时按数据版本生成审核批次并冻结引用快照；再次提交生成新批次，保留此前结果。
- [ ] 实现逐条通过/驳回/授权豁免及有界批量接口，校验状态1、当前批次、用户权限和版本。重复或过期命令返回明确冲突。
- [ ] 汇总终审要求当前批次无待审、无驳回并匹配数据版本；驳回清签回草稿，保留历史审核，不开放完结解锁。
- [ ] 提供summary detail、reviews、auditLogs分页查询和固定口径的来源计数；教师仅本人可读、无审核写权限。
- [ ] 实现完结后追加线下补差说明，验证业务数值、签字、原快照均不变；禁止编辑/删除历史说明。

**验收：** 完整通过/驳回/重提/豁免/完结/线下说明链有数据库、Service及安全接口测试；跨教师ID不能越权；列表无无界数组。

### T08：审核中心与汇总详情前端

**涉及文件：** 新增`front/RuoYi-Vue3/src/api/system/itemReview.js`、`src/views/system/auditCenter/index.vue`、`src/views/system/workloadSummary/SummaryAuditDrawer.vue`；修改`src/api/system/workloadSummary.js`、`src/views/system/workloadSummary/index.vue`；测试置于各组件`__tests__`目录。优先复用已有测试工具，确需组件测试时在独立提交补最小工具配置。

- [ ] 按分类/状态/批次筛选展示，固定教师、学期、项目、来源、核定值、异常、审核结果，来源统计区分导入原始行/有效任务/明细。
- [ ] 详情显示历史快照、版本、规则/费率来源、分页审核与时间线；缺历史字段明确提示。
- [ ] 驳回必填原因；过期版本保留输入并刷新，不错误提示成功；汇总通过显示未处理数量和原因。
- [ ] 移除解锁按钮与旧签字阶段文案；完结页只提供具权限的线下说明追加入口。
- [ ] 覆盖教师只读、教务审核、双击提交、分页、空数据、网络失败和状态冲突；用真实浏览器走一次完整操作并保留记录。

**验收：** 管理者能从来源定位到一条审核证据，教师能看到本人结果与原因；UI不承诺后端禁止的操作；关键交互测试通过。

### T09：补齐岗位减免导入和花名册预检页面

**涉及文件：** `front/RuoYi-Vue3/src/views/system/{roleAssignment,teacherProfile}/index.vue`、`src/api/system/{roleAssignment,teacherProfile}.js`；复用远程后端`BizRoleAssignmentController`、`BizTeacherProfileController`及其ImportService；更新`docs/教务与教师操作手册.md`、`docs/测试/导入模板说明.md`。

- [ ] 岗位页接模板下载、预检、确认及错误清单，接口方法以远程真实契约为准（岗位模板为POST），不照抄旧建议GET接口。
- [ ] 预检展示学期、教师、减免值、重复来源处理及180封顶预期；确认按钮按批次状态和确认令牌使用，不允许重复确认产生新行。
- [ ] 教师页接新花名册预检，展示工号/职称/部门/人员性质异常；新预检不偷偷调用旧importData落真实人员。
- [ ] 对旧导入入口明确用途和权限；异常行政/兼岗或重复工号只出确认清单；正式人员落库需另有已授权的数据任务。
- [ ] 测试重传、类别/学期错配、他人批次、无权限、过期令牌、网络重试和部分失败；验证页面回执与实际表数一致。

**验收：** 两个新后端能力都有可用入口，岗位值仅导入教务学期值、不按职务再计算；花名册预检不会隐式创建账号。

### T10：整链业务与导出对账

**涉及文件：** 复用远程`rear/workload-system/src/test/resources/fixtures/pay_acceptance_fixture.sql`及cleanup、`src/test/java/com/workload/system/fixture/TeachingTaskFixtureWorkbookTest.java`；新增`docs/验收/整链验收矩阵.md`、隔离测试库专用运行脚本与产出目录；更新`docs/测试/测试用例清单.md`。

- [ ] 建立脱敏固定样本，覆盖G1—G6/G8/G9/G11、G4>60不截断、G6>20封顶、G11>180、540边界、专任/外聘/缺性质、无费率和A—G。
- [ ] 在导入前写出每行因子/工作量、每教师G7—G11、绩效、其他酬金、舍入总额和每份报表的明确预期，避免用被测实现生成自己的期望。
- [ ] 实际执行模板→预检→确认→G11→核算→系数审批→核对→汇总提交→逐条审核→终审→导出；包括失败重试、驳回重提和完结后拒写。
- [ ] 比较数据库/API/页面/附件1/附件2，断言数值、来源、封顶说明、审核版本一致；修改当前源数据后再次读取历史报表，不得污染历史证据。
- [ ] 批量导出验证1教师、多Sheet、ZIP、跨页、跨学期、超阈值、部分失败与权限越界；校验失败清单和文件名，不只检查HTTP200。
- [ ] 由教务角色与教师角色按手册执行并登记差异；缺少业务签收时明确标记“技术验收完成、业务签收待完成”。

**验收：** 每个样本差异为0或有已确认的政策解释；失败不能被“完成”提示掩盖；所有证据绑定同一commit与迁移清单。

### T11：规模、权限与故障恢复

**涉及文件：** `rear/workload-system/src/main/java/com/workload/system/controller/BizDashboardController.java`、报表服务和对应Mapper为测量对象；新增`docs/验收/容量与恢复.md`及隔离环境负载脚本。

- [ ] 按实际拟用学期人数/任务规模生成虚构数据，记录机器、堆、MySQL/Redis版本、并发和限制。没有真实规模输入时先测100教师/1万明细，再测1000教师/10万明细，明确只是合成负载。
- [ ] 测列表、仪表盘、按教师批量核算、多Sheet/ZIP导出的p50/p95、SQL次数、峰值内存、耗时及失败率；启动前约定试用门槛，建议普通列表p95≤2秒、仪表盘≤3秒（在记录的环境和负载下）。
- [ ] 在1/20/100位教师导出及每教师5000行边界测资源与超限拒绝；核算/大导出以有界完成、明确失败回执、无OOM为最低要求，基于基线再制定耗时预算。
- [ ] 独立验证教师跨人读取/提交/导出、旧权限、无权批次、过期版本和批量混入无权对象。
- [ ] 模拟导入确认中断、数据库回滚、Redis不可用和重复请求；验证恢复后无部分结果，并在副本重演备份恢复。

**验收：** 可复现报告包含原始测量和失败样本；有实际问题才进入T14优化；高风险权限/恢复问题修复并重验后才能T13。

### T12：共享环境前的凭据与配置收口

**涉及文件：** `.env.example`、`rear/workload-admin/src/main/resources/application-druid.yml`、`application.yml`、`docs/口令轮换清单-2026-08-30.md`；新增`docs/验收/共享环境配置检查.md`。真实密钥仅在环境或本机受限配置中。

- [ ] 环境管理员核实历史DB/JWT/Druid凭据是否已轮换；只登记完成时间、责任人和验证结果，不收集真实值。旧口令不能再认证，旧token失效才算关闭。
- [ ] 用业务专用账号运行应用，运行权限与迁移权限分开；共享环境限制Druid/Swagger访问并关闭无用途监控入口。
- [ ] 停用或改密公开测试账号，验证强制改密与管理员重置链；生产配置不自动加载测试夹具和真实花名册。
- [ ] 核对应用、数据库和Redis监听/访问范围、备份存放权限、日志脱敏及恢复权限。

**验收：** 有验证记录而非仅“.env已忽略”；不要求改写Git历史。轮换本身涉及环境账号变更，执行时由环境管理员按已有清单操作，不在本轮规划中代跑。

### T13：发布演练、操作手册与签收

**涉及文件：** `README.md`、`AGENTS.md`、`CLAUDE.md`、`docs/教务与教师操作手册.md`、`rear/sql/MIGRATIONS.md`；新增`docs/验收/发布与回退.md`、`docs/验收/一期签收清单.md`。

- [ ] 用同一候选SHA从干净环境完成安装、建库/升级、测试、启动、浏览器冒烟；记录配置变量和迁移校验，不复制真实密钥。
- [ ] 形成应用包/前端包校验值、数据库迁移清单、备份位置、应用回退和数据恢复步骤；在副本实际演练一次。
- [ ] 按教师、教务、管理员角色复核操作手册；状态/公式/岗位值/补差及报表说明与实际页面一致。
- [ ] 核对16项需求逐项证据、P0风险关闭、全量lint零错误和业务签收；未关闭事项列责任人、影响与截止条件，不能仅改“100%”字样。

**验收：** 可复现的版本、结构、环境和业务验收四份证据一致；发布动作在单独授权下执行，完成本任务的准备并不自动发布。

### T14：lint债务和必要优化

**涉及文件：** `front/RuoYi-Vue3/.eslintrc.cjs`与报告定位的具体JS/Vue文件，必要时新增`.gitattributes`；性能修改只限T11证明的热点Controller/Service/Mapper。

- [ ] 先清理未定义变量、重复键、错误emit/prop契约和computed副作用，按功能区域提交并运行对应交互回归。
- [ ] 再单独提交换行格式与安全等值替换，关闭本次`git diff --check`发现的既有空白/CRLF问题；不把全库格式化和业务修改混为一个提交，不禁用规则来“清零”。
- [ ] 运行完整lint，要求0错误；86个历史警告按类别处理或在有依据的规则配置中明确，报告真实剩余数。
- [ ] 只对T11测出的瓶颈做SQL聚合/分页、索引、按需加载等最小优化，保留改前/改后同负载测量和结果一致性对账。

**验收：** lint退出0，测试/构建通过；优化有可复现收益且业务结果不变。没有证据的通用架构重写不进入本期。

## 3. 验证命令与结果要求

```sh
# 后端常规测试；从项目根运行，明确使用已安装JDK17
JAVA_HOME=/Users/a1/Library/Java/JavaVirtualMachines/ms-17.0.19/Contents/Home mvn -f rear/pom.xml -B -ntp test

# MySQL窄IT；先在环境注入WFIT_TEST_DB_URL/USER/PASSWORD，目标必须是本地隔离库
JAVA_HOME=/Users/a1/Library/Java/JavaVirtualMachines/ms-17.0.19/Contents/Home mvn -f rear/pom.xml -pl workload-system -am test -Dtest=CoefficientApprovalTransactionIT -Dsurefire.failIfNoSpecifiedTests=false

# 整合后的前端（在front/RuoYi-Vue3运行）
npm ci
npm run test:unit
npm run build:prod
npm run lint

# 提交前格式/空白检查（项目根）
git diff --check
```

上面的`test:unit`依赖T01纳入远程package.json，当前旧本地package.json尚无此脚本。新IT分别显式指定`WorkloadConcurrencyIT`、`SourceChangeConsistencyIT`；常规测试默认不含`*IT`。期望为测试无失败/错误、构建退出0、全量lint零错误；测试数量整合后以实际结果为准，不强行固定为292或337。

## 4. 阶段出口

| 阶段 | 包含任务 | 可以对外声称的状态 |
|---|---|---|
| A：一致基线 | T01—T04 | 代码/结构配套、可复现测试构建；尚非业务交付 |
| B：业务闭环 | T05—T09 | 历史口径、审核、补差和导入入口已实现且有专项验证 |
| C：技术验收 | T10—T12、T14 | 整链、容量、安全、恢复和质量门禁通过 |
| D：正式签收准备 | T13 | 候选版本及业务签收证据齐全，发布按单独授权执行 |

最先执行T01，随后T02与T04；T12可同步准备。不要先新增页面或重构计算器，以免在尚未统一的基线上继续累积变更。
