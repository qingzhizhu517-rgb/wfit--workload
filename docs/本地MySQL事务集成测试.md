# 本地 MySQL 事务集成测试

`CoefficientApprovalTransactionIT` 使用实际 Spring 事务代理、MyBatis XML 和 MySQL。重算调用替身在真实快照写入、版本更新后注入异常，验证外层审批事务的原子性；不替代完整核算、HTTP鉴权和并发验收。

## 运行

需要 Java 17、本地 MySQL、已通过 `rear/scripts/check-migrations.py` 检查的隔离库（具体契约见 `rear/sql/MIGRATIONS.md`，不能仅以最大编号判断），以及既有库用户的查询和创建临时表权限。测试只接受 `jdbc:mysql://127.0.0.1:` URL。

通过环境设置 `WFIT_TEST_DB_URL`、`WFIT_TEST_DB_USER`、`WFIT_TEST_DB_PASSWORD`（不要提交凭据），然后在项目根目录运行：

```sh
mvn -f rear/pom.xml -pl workload-system -am test \
  -Dtest=CoefficientApprovalTransactionIT \
  -Dsurefire.failIfNoSpecifiedTests=false
```

IT按Maven约定不在默认单元测试中运行，显式调用时环境/连接缺失直接失败，不静默跳过。MySQL JDBC驱动复用项目受管理版本，仅加test作用域。

## 数据隔离

每例新建独立物理连接，用 `SHOW CREATE TABLE` 读取实际结构，然后创建所需同名TEMPORARY影子表。全部创建成功后才写入夹具。临时表在该连接中遮蔽真实表，连接销毁后自动移除；不执行真实表清理、不占用真实表业务主键。

测试使用 SingleConnectionDataSource 与 SpringManagedTransactionFactory，让 Mapper 和 JdbcTemplate 参与同一实际事务。不能改成普通连接池：临时表只属于创建它的连接。

## 覆盖

- 批准后重算失败：因子、审批状态、版本和快照全部回滚。
- 成功：上述状态提交；重复批准拒绝，快照不重复。
- 汇总待审/完结：真实条件SQL阻断，未调用重算。
- 明细版本已变化：真实条件SQL阻断，审批与因子不变。
- 存在待审系数申请或未核对明细：真实当前读阻断汇总提交和终审，汇总状态保持不变。
- 明细状态0/2阻断、1/3放行，检查限定教师与学期；核对操作的版本、学时和重复更新条件也有真实SQL验证。

上述窄IT不覆盖跨连接并发和完整核算；新增专项如下。Controller真实安全链和整链业务验收仍未完成。

## T03 来源与真实多连接专项

新增 `SourceChangeConsistencyIT`（11项）与 `WorkloadConcurrencyIT`（6项）。三类合计31项，2026-10-28通过；并发6项另重复运行通过。

```sh
mvn -f rear/pom.xml -pl workload-system -am test \
  -Dtest=CoefficientApprovalTransactionIT,SourceChangeConsistencyIT,WorkloadConcurrencyIT \
  -Dsurefire.failIfNoSpecifiedTests=false
```

使用专用本机实例，URL必须为127.0.0.1且拒绝3306。新fixture读取已通过32迁移的源库DDL，每例新建随机 `wfit_it_` schema，仅写随机库，结束删除自己创建的schema；需要该隔离实例的CREATE/DROP DATABASE权限，不应为正式账号扩大权限。并发用例另需读取performance_schema.data_locks/data_lock_waits。不能使用连接池单连接或TEMPORARY表替代真实多连接。

MyBatis使用与应用一致的默认SESSION缓存；锁读显式清缓存，避免测试设置掩盖生产旧读。随机schema和线程同步屏障配合数据库实际等待观测；超时失败则仍终止工作线程后再清理schema。

核算使用真实G1策略、Spring事务、MyBatis、快照、汇总和酬金服务，仅规则提供默认值和身份等非本次目标依赖使用替身。后置SQL触发器主动失败验证回滚；不代表全策略或真实鉴权端到端通过。详细入口、红绿证据及限制见 `docs/验收/T03写入口矩阵.md`。
