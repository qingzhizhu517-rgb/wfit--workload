# T05 剩余实施切片（2026-10-29）

执行目录固定workload-baseline-integration工作树，不触原工作区/3306业务库，不推送、合并主分支或部署。日期缓存f049a4e已完成；快照完整性v2正在独立收口。以下是继续路线图T05的技术分解，不能提前标记为已实现或业务确认。

## 当前依赖与验收

- [x] 日期选值/缓存/LocalDate契约：570常规、31IT、双时区40HTTP。
- [x] v2完整性：新canonical hash，旧v0/v1严格兼容，Controller明确50017，真实MySQL和HTTP证明。
- [x] A：实际规则解析对象与显式计算上下文、G4/G5/G6来源、明细v3。
- [x] B：新导入/人工修改/系数批准的实际因子来源；其他酬金实际计算证据。
- [x] C：汇总/酬金只追加结算证据，资格与费率完整性、审批门禁、历史展示/导出。

各切片先红后绿，独立规格/质量复核后本地保存；保持完成状态与证据对应，不把完整性等同于来源完整或历史结算可复现。

## A：规则解析与本次实际来源

新增不可变RuleContent/RuleResolution：原行id/code/value/effectiveFrom/To/description/status完整内容；选择日、sourceKind(DB_RULE/CODE_FALLBACK)、默认值用途/缺失原因、内容哈希。id只是定位，内容哈希及嵌入内容才是不可变版本；日期证据序列化为ISO字符串，数值plain string。

显式CalculationPolicyContext(asOfDate)按code memoize实际resolution。同code的默认值冲突拒绝，不使用ThreadLocal或singleton状态。calculate(item,context)、afterCalculated(item,value,context)、buildFresh(item,context)复用同一实例，包含金额规则与告警/审批阈值。保留兼容入口但财务计算统一接入context；不在策略计算后重新查规则拼来源。

rule_version列长度64，只能存裸SHA-256集合摘要，不能加sha256前缀造成超长。原标记RULE-学期不改写历史。

明细来源新增formatVersion3，v0/v1/v2格式/哈希保持原语义；v3包含policySources/factorEvidence/decisionEvidence/evidenceStatus。新因子引用指向实际嵌入内容，校验内容hash与值绑定；外层hash覆盖所有证据。旧无来源显示PARTIAL/MISSING_HISTORICAL_SOURCE，不按值相等指认当前规则或批文。

验收：同id同值但日期/内容变化摘要不同；源行改名/修改/删除后旧v3独立解释；注入第二次resolver不同值但同次只解析一次；G4/G5告警阈值、G6cap/constant被捕获；fallback明确非DB来源；旧v0/v1/v2兼容；篡改内容/引用/值拒绝。

## B：导入/维护/批准/其他酬金来源

候选增量33_factor_provenance.sql（分配前再查库存）：item.factor_provenance_json、allowance.calculation_evidence_json，均可空。没有旧数据回填。

新导入在同一行事务创建context；写每个派生值时同步FactorResolution(value,evidence)。覆盖DB规则和实际硬编码分支：G1的K1/Q1/Q2、G2 C2=0.9、G3首轮K=1、N小班=1、显式导入覆盖值、教学评价等未落源表输入。分支证据保存输入、条件、阈值、操作、结果，不能只写algorithmVersion标签。

新手改因子同时替换为MANUAL_INPUT证据或清对应来源；同值手改也不能继续冒用旧规则/批准。系数批准写APPROVED_OVERRIDE完整批准信息并原事务重算，移除“最新批准且当前值相同”的追认逻辑。每版快照复制当时来源；原因子值或来源缺失明确missing。

其他酬金A—G在实际算amount时保存输入、分支、完整rule resolution、舍入及金额。Pay只复制当时证据，不用当前政策补造旧amount来源。

验收：覆盖值即使等于政策仍标IMPORT_OVERRIDE；旧系数等于现值不补证；同值手改清旧来源；导入原始输入/各类别分支齐全；故障注入证明来源、任务、子表、快照和版本整体回滚。

## C：结算证据和资格

候选增量34_settlement_snapshot.sql：只追加biz_workload_settlement_snapshot(summary_id,user_id,semester,settlement_version,format_version,payload_json,hash,created_by/at)，UNIQUE(summary_id,version)；summary/pay各加可空settlement_snapshot_id指针。仅新增结构，不改历史金额、状态或快照。

payload保存：semester/asOfDate/SEMESTER_END选择模式及真实适用依据；人员性质/职称/特殊状态及资格原因；实际明细id/type/status/calculationVersion/snapshotId/hash/result（手工类保存原值及来源状态）；课程去重/达标依据；完整规则集合、费率内容/内容hash；汇总各结果与警示；Pay使用summaryEvidenceHash、其他酬金输入/amount/实算证据和舍入结果。没有真实业务批准的启用学期不能凭空设一个。

明确专任才按政策算绩效；null/行政/兼岗/未知性质不自动猜资格，不产生可提交完成结算；需要绩效的人员缺费率阻断，不能伪造0。旧教师导入把空性质默认为专任的入口亦需关闭；既有专任历史无法判断是否默认，应记录人工确认边界。

保持period→summary→items/allowance→pay锁序。Summary(persist=true)在原事务读取实际来源构建SummaryEvidence，写汇总并显式交给Pay，写Pay后只追加一次结算快照，再更新两指针；失败全部回滚。独立Pay重算读取已保存SummaryEvidence并校验，不重新查当前profile/rules拼旧汇总。preview不写证据或指针。

WorkloadCalc当前recalcAll/run在Summary已算Pay后又算一次，加入结算版前消除重复编排或同内容复用，保证一次核算只一版。submit/approve锁内检查summary/pay指针一致、快照完整、资格明确及明细版本匹配；不新增完结写入口。老pending/finished缺历史字段不伪补，兼容边界必须显式记录。

列表/Dashboard/附件2封顶与资格解释、导出SQL中的teacher_nature均从结算证据读；旧记录明确历史字段缺失，不借当前配置伪装精确历史。

验收：人员异常/缺费率阻断；profile/rate/rule变更不污染旧证据；独立Pay沿用保存SummaryEvidence；指针/版本错配拒绝提交；任一快照/指针后置失败全回滚；一次run只一版；与提交并发互斥；旧完结迁移/政策变化前后数值/签字/状态/快照hash不变；导出缺史明确。

## 数据与实施约束

所有DDL先审清单和后置契约，副本重复执行演练；schema变更只13306隔离实例，原3306不动。当前MySQL9.0.1，不冒称MySQL8。所有测试Maven串行，JDK17；MyBatis SESSION缓存不改成STATEMENT。运行jar在package前核对PID后停止，保留日志，避免原地覆盖活跃jar。

worker完成后send_message不会触发新回合，修复/续作需followup_task。HANDOFF.md保持当前真实进展与进程配置。

用户补充（2026-10-29）：正式启用学期暂不指定，先完成隔离验收。C正式配置应保持空，QA专用配置与业务生效分开记录。

2026-10-30 C最终：739常规、164IT、48HTTP、管理/教师8浏览器检查/7截图，规格/质量双PASS；33/34副本重复迁移及旧完结保全通过。正式启用学期仍空，隔离验收完成，正式生效不是本次完成条件。
