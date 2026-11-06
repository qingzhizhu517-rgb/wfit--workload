# T06细分执行计划

**范围：** 审核/补差数据契约、35幂等迁移与fresh结构、统一审计安全边界。不启用T07 API/T08页面，不变更T05正式空范围。

**现场：** 固定集成worktree；原`/Users/a1/Desktop/wfit--workload`和3306原库禁止改动。仅13306的独立克隆演练，不能回放历史有状态QA脚本；Maven串行由协调代理执行。

## 切片A：数据契约与迁移

- [x] 读AGENTS、HANDOFF、T05证据边界、路线图及旧审核计划/当前Mapper。
- [x] 向协调代理确认独立batch、item_review、offline_adjustment_note、版本0/NULL、历史保真和权限矩阵。
- [x] 写只读catalog回归11项：缺校验、历史可空性、唯一键/版本默认、结构化补差、快照非空及CHECK完整表达式；质量反例先失败后修正。
- [x] 新增`docs/验收/审核契约.md`，修订旧计划，清除冲突编号/原库命令/伪造历史回填。
- [x] 新增35，fresh01、05/06权限与只读checker同步。
- [x] 协调代理在MySQL9.0.1两份13306克隆各两轮35；全部原biz列/行hash不变，三个新表0回填，14项/克隆共28项事务回滚约束/唯一键例通过；最终每库216只读检查、两项实际漂移反例被拒且精确恢复；完整fresh链和MySQL8仍未验证。证据`docs/验收/35迁移演练证据.json`。
- [x] 修订MIGRATIONS清单与当前文件hash；实际演练由协调代理记录，未执行不能写通过。

## 切片B：统一审计Java实现

- [ ] 明确20个BaseEntity业务Mapper＋AuditLog的statement/参数覆盖清单，不对所有biz无差别填充。
- [ ] 创建AuditableEntity、BaseEntity实现、受控MyBatis插件与配置；约定标量ParamMap保留_audit*，防环遍历。
- [ ] INSERT防伪创建/更新字段；UPDATE仅更新服务端update字段，普通XML移除create字段SET；业务operator/reviewer不信任HTTP输入。
- [ ] 固定历史/业务时间、calc/settlement快照created_*及period_lock排除；新batch/补差由T07专用服务。
- [ ] 先失败后通过：单实体/集合/数组/Mapper直调/标量、伪造操作者、匿名SYSTEM、异常与线程复用清理、固定历史不变；全量后端回归按当前日志汇总。

## 收口

- [ ] 规格复核：本契约能解释提交前核对与待审证据，WAIVE独立授权、补差只追加，T07约束明确未启用。
- [ ] 质量复核：迁移双轮无历史改写、checker及审计测试、diff --check，保全原43文件与原库边界。
- [ ] 仅本地统一提交；不宣称业务验收或正式启用，不勾选T07/T08。

切片A独立规格及质量复核最终PASS；Java审计仍待切片B，不据此关闭整个T06。
