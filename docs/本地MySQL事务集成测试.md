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

每例新建独立物理连接，用 `SHOW CREATE TABLE` 读取实际结构，然后创建五张同名TEMPORARY影子表。全部创建成功后才写入夹具。临时表在该连接中遮蔽真实表，连接销毁后自动移除；不执行真实表清理、不占用真实表业务主键。

测试使用 SingleConnectionDataSource 与 SpringManagedTransactionFactory，让 Mapper 和 JdbcTemplate 参与同一实际事务。不能改成普通连接池：临时表只属于创建它的连接。

## 覆盖

- 批准后重算失败：因子、审批状态、版本和快照全部回滚。
- 成功：上述状态提交；重复批准拒绝，快照不重复。
- 汇总待审/完结：真实条件SQL阻断，未调用重算。
- 明细版本已变化：真实条件SQL阻断，审批与因子不变。
- 存在待审系数申请或未核对明细：真实当前读阻断汇总提交和终审，汇总状态保持不变。
- 明细状态0/2阻断、1/3放行，检查限定教师与学期；核对操作的版本、学时和重复更新条件也有真实SQL验证。

尚未覆盖跨连接并发、完整重算服务、Controller安全链和汇总提交审核闭环。
