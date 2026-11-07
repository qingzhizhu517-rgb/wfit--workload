# T07 审核批次、逐项审核与线下补差实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. 当前后端代理负责本计划全部后端文件；父代理负责全局证据、真实HTTP验收、提交和推送。不得启动新代理或提交代码。

**Goal:** 在既有教师学期锁、T05证据和35结构之上实施真实审核批次、不可变审核历史、严格分页详情及只追加线下补差。

**Architecture:** 保留四模块和既有Summary审批状态机；新增独立review领域服务与严格format1 codec，不修改T05旧codec。所有变更按period→summary→items/allowance→pay→batch/reviews锁序；逐项CAS在独立事务服务执行，批量协调器没有外层事务。查询只还原保存证据，不以当前规则重建历史。

**Tech Stack:** Java17、Spring事务、MyBatis XML、Jackson树严格输入、JUnit5/Mockito、13306 MySQL9隔离随机schema。

## 固定设计决策

- data_version服务器持有，Summary→Pay使用明确内部入口，一次逻辑结算只在Pay末尾推进一次；独立Pay也推进。仅真正Summary计算更新时间。失败连同金额/版本/时间整体回滚，teacherConfirm不推进。
- 无batch历史待审的approve/逐条审核返回409并明确先驳回、重新核算再提交；reject可清签回0留历史。dataVersion0必须先重算。finished只追加说明，无解锁。
- BizAuditService强制调用真实ReviewBatchService，无可空分支。submit的批次、成员、状态CAS和日志同事务；approve要求非空完整成员、双hash、版本/数值/原证据引用一致；teacherConfirm保留本人/待审合法性，不新增必须教师签字才能终审的规则。
- 来源计数为纳入审核明细中的教学任务/岗位去重引用数、手工申报条目数、审核成员数和驳回排除数的固定口径；导入raw无法归属时返回null及缺史原因，不使用整批原始行数。
- 线下补差请求必填Idempotency-Key header（1—64受控ASCII字符）；客户端body仅adjustmentAmount/reason/handledAt/referenceNo/attachmentRef/noteText。同key规范化内容相同返回原行，不同409。附件仅受控/profile/upload/相对引用，拒绝任意URL、穿越、反斜杠和编码路径。
- ReviewSnapshotCodec独立format1，明确所有键及NULL，完整类型/enum/唯一有序成员校验，保存规范canonical摘要；batch与item双层hash和原calc/settlement引用校验。历史read不查询现源行。
- 已有review历史的草稿禁止删除，批量删除先全部锁后当前读预检。批次/说明无UPDATE/DELETE Mapper。

## 文件边界

新增system/review包：ReviewCommands（严格命令/分页/幂等校验）、ReviewSnapshotCodec（规范证据）、ReviewBatchService（提交/完整性/删除门禁）、ItemReviewService（逐项独立事务）、ReviewQueryService（详情与分页）、OfflineAdjustmentNoteService（说明追加）。新增对应三实体/三Mapper/XML及审核中心和summary详情Controller。必要修改BizAuditServiceImpl、SummaryCalcServiceImpl、PayCalcService/Impl、summary实体/Mapper/XML、summary删除服务与AuditLog分页Mapper。framework仅增加精确item_review/summary statement注册和精确CoverageTest排除。

## 执行小步与验收

### 1. 严格命令与codec

- [x] 先写缺功能红测：未知/伪造字段、字符串/小数version、原因边界、批量重复/101项、分页非法、decimal精度、日期与附件穿越、幂等key。
- [x] 串行运行针对测试并保存完整日志，确认因缺实现失败。
- [x] 最小实现命令解析；codec测试覆盖完整key/NULL/enum/类型、乱序/重复成员、hash篡改与重新签名但结构非法。
- [x] 运行新测试到绿，不改T05codec。

### 2. 版本与批次提交

- [x] 写Summary/Pay版本一次推进、失败回滚、teacherConfirm不推进测试。
- [x] 加server-owned summary映射、版本推进SQL与明确Summary→Pay调用入口。
- [x] 写新批次非空/来源分类优先级、snapshot引用、历史缺失、全部同事务失败回滚测试。
- [x] 实现实体/Mapper/批次服务，submit强制调用；保留原CAS断言。

### 3. 逐项审核与终审

- [x] 写PENDING→PASS/REJECT/WAIVE、独立权限、过期/重复/跨批409、混合批量部分成功测试。
- [x] 实现每项REQUIRES_NEW事务和无事务批量协调，≤100且无重复。
- [x] 写approve空集合/遗漏/版本/hash/成员篡改拒绝、教师签字独立、无batch历史409；reject清签保留历史、永不unlock。
- [x] 实现mandatory审核门禁与历史拒绝策略，旧fixture真实ready版本/批次/审核，不删除CAS/freeze断言。

### 4. 分页详情与补差

- [x] 写本人DataScope、各分页默认1/10上限100、稳定id排序、非法400、历史读取不依赖当前源测试。
- [x] 实现有界detail/中心list及reviews/auditLogs/notes独立分页，不暴露整份无界batch。
- [x] 写finished追加、正负金额、可信身份、同key同内容幂等/异内容409、只追加且原hash不变测试。
- [x] 实现说明服务/端点与同事务业务审计，历史结算引用保留NULL。

### 5. 真实隔离与回归

- [x] framework显式注册review insert/result CAS和新增summary写语句；CoverageTest准确排除batch/note专用created_*，不宽扫。
- [x] 更新system独立真实fixture的35表复制和真实review服务；不依赖framework、不镜像审计插件。
- [x] run-t06-it.py运行新ReviewLifecycleIT及原170IT相关fixture；仅13306随机schema，不触碰旧验证源/3306。
- [x] 检查未改行CRLF/LF保持，串行常规测试；将日志/覆盖/剩余限制回报父代理，由父完成package、4角色HTTP、独立规格/质量复核与提交。

## 2026-11-06 后端交付状态

- 源码与测试已冻结，父代理执行后续独立规格/质量审查、package及真实角色HTTP；本计划不代表T07业务签收、T08页面或正式范围启用。
- 命令/codec/业务先红后绿日志：`/tmp/wfit-t07-01-red.log`至`/tmp/wfit-t07-29-upload-red.log`；真实错误包含无batch历史错误放行、JsonNode跨Jackson3空序列化、高精度Double吞尾数、500字原因拼日志超列及本系统中文上传名误拒，均以窄修回归。
- 当前全量常规：766（system754 + framework12）通过，`/tmp/wfit-t07-30-final-regular.log`。
- 最终全部真实IT：193（system185 + framework8）通过，`/tmp/wfit-t07-31-final-all-it.log`；覆盖原170及新增23，包含并发/CAS、部分批量、独立WAIVE权限、500字原因、审计失败回滚、真实教师范围、同key异actor及两项真实插件专项。
- 生产插件现精确覆盖22实体74写statement；batch/note专用created字段明确排除，不宽扫。原T05两codec/SQL35/checker均未修改。
- 与HEAD逐行比较：所有rear已跟踪改动的未改行CRLF/LF差异为0，`/tmp/wfit-t07-line-endings.json`；不覆盖父代理文档、原Desktop树或原3306。
- detail冻结子树转普通Map/List/标量供Jackson3，持久codec仍Jackson2独立format1；状态为NOT_SUBMITTED/HISTORICAL_MISSING/HISTORY_ONLY/STALE/VERIFIED，并带current/versionMatches。
- 线下说明必填`Idempotency-Key`且绑定可信actor；日志仅有界引用摘要，完整reason原样保存在review/note。上传引用允许本系统中文/括号/空格命名，仍拒URL、编码、穿越及反斜杠。

## 规格复核两项修正（2026-11-06）

- 原因与备注：reason/noteText原文先限长，trim只判必填原因非空；不裁剪、不截断业务文本。日期、凭证及附件引用沿用原规范化范围。原长501/10001且边缘有空白也拒绝；review/reject/note实际落库保留原文。
- 新审核format1语义：needsRecalculation必须显式0；isCapped/basicTeachingMet/pay.status按真实0/1枚举，原DDL可空旗标保留显式NULL；lastCalculatedAt为必填epoch毫秒long字符串，签字/历史item时间允许显式NULL或合法epoch字符串。pay对象与金额输出必须存在，pay/summary和pay/batch结算引用一致，历史NULL结算引用仍保留NULL，不补当前政策。
- 真实反例证实独立Pay原可为旧version0或无计算时间的summary推进版本；现所有Pay写路径要求已持久化真实Summary计算时间，独立Pay还要求已有正dataVersion。首轮Summary→Pay的version0仅在真正Summary已经写计算时间后可推进。submit额外拒缺计算时间，不制造历史时间。
- 红测：`/tmp/wfit-t07-32-spec-red.log`（6失败、1错误，含两种独立Pay反例）；focused绿：`/tmp/wfit-t07-33-spec-green.log`，54项。
- 修正后全量常规769（system757 + framework12）通过：`/tmp/wfit-t07-34-spec-final-regular.log`；全量真实MySQL197（system189 + framework8）通过：`/tmp/wfit-t07-35-spec-final-all-it.log`。旧T05测试及原CAS/freeze/隔离断言保留。
- 源码/测试再次冻结，交父复核并执行package与双版本真实HTTP；本次未提交、推送或部署。

## 2026-11-07最终收口

真实HTTP关闭归属拒绝500→403、旧batchSubmit完整预检及冻结500→409；先红后窄修，锁序与金额逻辑不变。最终788常规、199真实IT、MySQL9/8.4各93角色HTTP通过，规格/质量复核PASS。完整证据见docs/验收/T07审核生命周期.md。自建后端及owned MySQL8容器已停，保留隔离QA样本与数据卷，不部署；T08尚未完成。
