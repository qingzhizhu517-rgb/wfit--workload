# 审核中心与审计字段实施计划（2026-11-06修订）

原2026-09-12草案被路线图T06/T07/T08拆分。本文件为后续实施导航，数据契约以`docs/验收/审核契约.md`为准；不得按旧草案直接执行SQL或把计划当成已上线能力。

## 已裁定的修正

- 草稿“教务核对”与待审“逐条审核”分开；item.status不存储审核result。
- 目标固定workload_item；分类TEACHING_TASK/MANUAL_DECLARATION/G11_ROLE/OVER_LIMIT；PENDING/PASS/REJECT/WAIVE，豁免独立授权且原因必填。
- 批次独立表保存summary版本、汇总快照和原T05结算引用；逐项快照不可改，审核结果CAS。
- 完结永久锁定；仅追加独立线下补差说明，不更改原数值/签字/证据。
- 22已被G11学期来源脚本占用，审核迁移重新分配35；28固定导入框架，29历史保留，不复用。
- 删除旧草案中针对原业务库的直接执行命令。演练只在13306独立克隆，由受控连接脚本加载私有环境，不能使用省略host/port的命令。
- 删除旧草案历史审计回填方案。禁止以MIGRATION或当前时间伪造历史创建者/时间；仅对biz_audit_log新增三项nullable审计列。
- 不发布尚不存在的auditCenter页面；T06只预置隐藏F按钮权限，不新增C路由。

## T06：契约、迁移与统一审计

详见`docs/superpowers/plans/2026-11-06-t06-review-contract.md`。

- [x] 定稿`docs/验收/审核契约.md`及三表/summary版本/权限边界。
- [x] 35增量与01/05/06 fresh一致，双克隆双轮且原业务行hash不变。
- [x] 扩展只读`rear/scripts/check-migrations.py`，先失败后通过；验证新权限最小化及旧教务核对保留。
- [x] AuditableEntity、BaseEntity、受控MyBatis插件及明确Mapper覆盖名单；业务操作者服务端绑定，UPDATE禁止改创建字段。
- [x] 单对象/集合/标量/Mapper直调/匿名来源/异常上下文清理/固定历史测试；不动T05固定快照字段。

T06结束仅表示技术切片通过，不代表T07 API、T08页面或业务签收。

## T07：批次、审核、门禁与补差API（未启用）

**新增域/Mapper/Service/Controller：** BizWorkloadReviewBatch（专用只追加写入）；BizWorkloadItemReview、ItemReviewCommand、ReviewOverviewVo；WorkloadSummaryDetailService/WorkloadSummaryDetailVo；BizOfflineAdjustmentNote（只INSERT/SELECT）。修改BizAuditServiceImpl、BizWorkloadSummaryController、BizAuditLogMapper及summary持久化入口；新增相应单测、MySQL事务测试及真实角色HTTP验收。

- [ ] 明确历史无批次待审的处理策略，禁止补造历史批次或伪PASS。
- [ ] 汇总/结算成功持久化推进data_version，标脏保持needs_recalculation；覆盖单条/批量/源变更/酬金单独重算，不越过T05结算与资格门禁。
- [ ] 实现独立review format1 canonical codec和完整性校验；batch保存summary数值/签字、item全集/计算快照与T05结算引用；缺历史明确标识。
- [ ] submit锁内同事务新建batch/items、改summary 0→1、写日志；空集、脏数据、错误版本或证据拒绝，全失败不留孤行。
- [ ] PASS/REJECT普通review权限；WAIVE独立权限。仅当前待审批次、PENDING、version匹配可CAS；REJECT/WAIVE原因必填，服务端审核人/时间，终态不覆写。
- [ ] approve锁内验证batch非空/完整、data_version/hash/新鲜度/结算引用一致、全部PASS/WAIVE。reject必填理由、清签回草稿且保留历史；禁止unlock。
- [ ] summary detail聚合、reviews/auditLogs/补差说明独立分页，默认1/10且最大100；教师强制本人，审核中心无教师写权。
- [ ] 批量最大100、逐项真实独立事务，返回successCount/failCount/failDetails；两个审核员同version竞争仅一成功。
- [ ] append-only补差说明要求summary=2、原引用保真、request_key幂等；不存在edit/delete，无重新核算，无原值/签字/证据变更。
- [ ] 源修改及所有写入口锁序period→summary→items/allowance→pay一致，等待锁后读取当前数据；HTTP权限和IDOR用真实四角色校验。

## T08：审核中心与汇总详情页面（未启用）

**新增：** `src/api/system/itemReview.js`、`src/api/system/offlineAdjustmentNote.js`、`src/views/system/workloadSummary/SummaryAuditDrawer.vue`、`src/views/system/auditCenter/index.vue`及组件测试。复用现有Vitest工具链，不重新安排已完成的测试基础设施任务。

- [ ] 页面对草稿核对/待审审核明确命名，按分类/结果/批次筛选；固定显示教师/学期/项目/来源/核定值/异常/审核结果。
- [ ] 详情分汇总、来源、审核、时间线及线下说明；显示保存的版本/规则费率证据，缺史明确提示，不能补当前政策。
- [ ] reviews与auditLogs独立页码，过滤重置对应页，过期响应丢弃；409提示数据变化并刷新。
- [ ] PASS确认；REJECT/WAIVE原因必填；豁免及补差依独立权限显示；已完结无解锁或编辑原值。
- [ ] 待API与页面均实装后，再由独立迁移建立C菜单/页面路由，不把本阶段隐藏按钮改成不存在页面的入口。
- [ ] 教师只读、双击提交、空数据、网络失败、分页及冲突组件验证；真实浏览器走通过/驳回重提/豁免/完结/追加说明链。

## 验收纪律

无原工作树或3306操作，不推送/部署；不输出私有连接/token/dump。所有测试/build/MySQL/browser声明必须有当前日志；MySQL9演练不声称MySQL8。T10再做完整导入→核算→审核→酬金→导出链，T06不提前关闭T07/T08/T10。
