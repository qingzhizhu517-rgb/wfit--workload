# 审核中心与审计字段实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 WFIT 增加无多态歧义的工作量明细审核、可分页的汇总详情与独立审核中心，并让所有 `biz_*` 写入路径统一填充服务端审计字段。

**Architecture:** 明细审核只落 `biz_workload_item_review.summary_id + item_id`，不采用 `target_type/target_id`；每次汇总提交生成不可变审核批次，当前批次通过 `biz_workload_summary.review_batch_no` 定位。查询拆成汇总聚合、`reviews` 分页和 `auditLogs` 分页，前端详情抽屉与审核中心复用同一契约；MyBatis `Executor.update` 插件在 SQL 渲染前覆盖审计字段，显式状态 SQL使用插件注入的保留参数。

**Tech Stack:** Java 17、Spring Boot 4、Spring Security、MyBatis、MySQL 8、JUnit 5/Mockito/AssertJ、Vue 3、Element Plus、Axios、Vitest 3、Vue Test Utils 2、ESLint、Vite 6

---

## 设计边界与最终契约

- 汇总状态仍为 `0 填报中 → 1 教务处待审 → 2 已完结`，驳回为 `1 → 0`；明细状态仍为 `0 草稿/1 已核对/2 有异议/3 已驳回`，两套状态不复用。
- 审核对象固定为 `biz_workload_item`。其他酬金不是工作量明细，本期不把 `ALLOWANCE` 塞入审核表；将来若审核其他酬金，必须新建 `biz_allowance_item_review`，禁止恢复多态目标列。
- `review_type` 取 `TEACHING_TASK`、`MANUAL_DECLARATION`、`G11_ROLE`、`OVER_LIMIT`。优先级为 `OVER_LIMIT` > `G11_ROLE` > `MANUAL_DECLARATION` > `TEACHING_TASK`，保证一条明细只生成一条审核记录。
- `result` 取 `PENDING/PASS/REJECT/WAIVE`；只有汇总状态 `1` 且记录属于当前 `review_batch_no` 时允许审核。`REJECT` 原因必填，`PASS/WAIVE` 原因可空。
- 汇总 `approve` 的门禁为当前批次 `PENDING=0 且 REJECT=0`；驳回汇总清签字并回到状态 `0`，但历史审核批次不删除。
- 教师仅能查看本人汇总、审核记录和时间线；教师不能调用审核中心写接口。教务/业务管理员依权限查看全部，现有 `DataScopeUtil.resolveUserId/assertOwnOrAdmin` 继续作为后端最终防线。
- `reviews` 与 `auditLogs` 都返回标准 `TableDataInfo(rows,total)`，默认 `pageNum=1,pageSize=10`，最大 `pageSize=100`；详情主体不内嵌无界数组。
- `data_version BIGINT` 每次成功重算递增，`last_calculated_at` 记录最近一次汇总重算时间；提交时审核快照保存该版本，防止待审数据与审核对象错位。

## 文件结构

**数据库与权限**
- Create: `rear/sql/22_workload_audit_center.sql` — 存量库幂等迁移、索引、菜单和权限。
- Modify: `rear/sql/01_biz_schema.sql` — fresh DB 新表、汇总版本列、全量审计列。
- Modify: `rear/sql/05_biz_menu.sql` — 审核中心菜单和按钮权限。
- Modify: `rear/sql/06_test_accounts.sql` — role 1/2/3/4 的查询、审核权限基线。

**统一审计字段**
- Create: `rear/workload-common/src/main/java/com/workload/common/core/domain/AuditableEntity.java` — 无 `remark/params` 的轻量审计契约。
- Modify: `rear/workload-common/src/main/java/com/workload/common/core/domain/BaseEntity.java` — 实现 `AuditableEntity`。
- Create: `rear/workload-framework/src/main/java/com/workload/framework/mybatis/BizAuditFieldInterceptor.java` — 覆盖 `biz_*` INSERT/UPDATE 审计字段。
- Modify: `rear/workload-framework/src/main/java/com/workload/framework/config/MyBatisConfig.java` — 注册插件。
- Create: `rear/workload-framework/src/test/java/com/workload/framework/mybatis/BizAuditFieldInterceptorTest.java` — INSERT、UPDATE、批量参数、匿名系统任务、客户端伪造覆盖测试。
- Modify: `rear/workload-system/src/main/java/com/workload/system/domain/BizAuditLog.java` — 继承审计契约字段。
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizAuditLogMapper.xml` — 插件可填充的四字段映射。

**明细审核域**
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/BizWorkloadItemReview.java` — 明细审核实体和筛选参数。
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/ItemReviewCommand.java` — `version/reason` 写命令。
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/vo/ReviewOverviewVo.java` — 审核中心行与统计 DTO。
- Create: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadItemReviewMapper.java`。
- Create: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadItemReviewMapper.xml`。
- Create: `rear/workload-system/src/main/java/com/workload/system/service/BizWorkloadItemReviewService.java`。
- Create: `rear/workload-system/src/main/java/com/workload/system/service/impl/BizWorkloadItemReviewServiceImpl.java`。
- Create: `rear/workload-system/src/main/java/com/workload/system/controller/BizWorkloadItemReviewController.java`。
- Create: `rear/workload-system/src/test/java/com/workload/system/service/impl/BizWorkloadItemReviewServiceImplTest.java`。
- Create: `rear/workload-system/src/test/java/com/workload/system/controller/BizWorkloadItemReviewControllerTest.java`。
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/BizAuditService.java`。
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/impl/BizAuditServiceImpl.java`。
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadSummaryMapper.java`。
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadSummaryMapper.xml`。
- Create: `rear/workload-system/src/test/java/com/workload/system/service/impl/BizAuditServiceImplTest.java`。

**汇总详情与分页日志**
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/vo/WorkloadSummaryDetailVo.java`。
- Create: `rear/workload-system/src/main/java/com/workload/system/service/WorkloadSummaryDetailService.java`。
- Create: `rear/workload-system/src/main/java/com/workload/system/service/impl/WorkloadSummaryDetailServiceImpl.java`。
- Modify: `rear/workload-system/src/main/java/com/workload/system/controller/BizWorkloadSummaryController.java`。
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizAuditLogMapper.java`。
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizAuditLogMapper.xml`。
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadItemMapper.java`。
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadItemMapper.xml`。
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizTeachingTaskMapper.java`。
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizTeachingTaskMapper.xml`。
- Create: `rear/workload-system/src/test/java/com/workload/system/service/impl/WorkloadSummaryDetailServiceImplTest.java`。
- Modify: `rear/workload-system/src/test/java/com/workload/system/controller/BizWorkloadSummaryControllerTest.java`。

**前端**
- Modify: `front/RuoYi-Vue3/package.json` — 固定版本加入 Vitest/jsdom/Vue Test Utils 与 `test:unit`。
- Modify: `front/RuoYi-Vue3/vite.config.js` — Vitest `jsdom` 与 `@` alias 共用配置。
- Create: `front/RuoYi-Vue3/src/api/system/itemReview.js`。
- Modify: `front/RuoYi-Vue3/src/api/system/workloadSummary.js`。
- Create: `front/RuoYi-Vue3/src/views/system/workloadSummary/SummaryAuditDrawer.vue`。
- Modify: `front/RuoYi-Vue3/src/views/system/workloadSummary/index.vue` — 以组件替换现有仅展示汇总字段的抽屉。
- Create: `front/RuoYi-Vue3/src/views/system/auditCenter/index.vue`。
- Create: `front/RuoYi-Vue3/src/views/system/workloadSummary/__tests__/SummaryAuditDrawer.spec.js`。
- Create: `front/RuoYi-Vue3/src/views/system/auditCenter/__tests__/index.spec.js`。

### Task 1: 数据库契约、索引与权限

**Files:**
- Create: `rear/sql/22_workload_audit_center.sql`
- Modify: `rear/sql/01_biz_schema.sql:296-332`
- Modify: `rear/sql/05_biz_menu.sql:43-63`
- Modify: `rear/sql/06_test_accounts.sql`

- [ ] **Step 1: 写迁移契约检查脚本**

先在 `22_workload_audit_center.sql` 末尾加入验收查询；它们在表尚未创建时应失败，证明测试能捕获缺失对象：

```sql
SELECT id, summary_id, item_id, review_batch_no, review_type, result, version,
       create_by, create_time, update_by, update_time
FROM biz_workload_item_review LIMIT 0;
SHOW INDEX FROM biz_workload_item_review;
SELECT review_batch_no, data_version, last_calculated_at
FROM biz_workload_summary LIMIT 0;
```

- [ ] **Step 2: 运行检查并确认失败**

Run（在 `rear/`）：
```bash
mysql -u wfit -p wflg_workload < sql/22_workload_audit_center.sql
```
Expected: `ERROR 1146 ... biz_workload_item_review doesn't exist` 或 `ERROR 1054 ... review_batch_no`。

- [ ] **Step 3: 定义无多态审核表和索引**

在 fresh DDL 与迁移脚本中使用以下准确结构；迁移脚本用 `information_schema.columns/statistics` + prepared statement 包装 `ADD COLUMN/INDEX`，不得依赖 MySQL 不支持的 `ADD COLUMN IF NOT EXISTS`：

```sql
CREATE TABLE IF NOT EXISTS biz_workload_item_review (
  id BIGINT NOT NULL AUTO_INCREMENT,
  summary_id BIGINT NOT NULL COMMENT 'FK biz_workload_summary.id',
  item_id BIGINT NOT NULL COMMENT 'FK biz_workload_item.id',
  review_batch_no VARCHAR(50) NOT NULL,
  review_type VARCHAR(32) NOT NULL COMMENT 'TEACHING_TASK/MANUAL_DECLARATION/G11_ROLE/OVER_LIMIT',
  snapshot_json JSON NOT NULL,
  data_hash CHAR(64) NOT NULL,
  result VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PASS/REJECT/WAIVE',
  reason VARCHAR(500) DEFAULT NULL,
  reviewer_id BIGINT DEFAULT NULL,
  reviewer_name VARCHAR(64) DEFAULT NULL,
  reviewed_at DATETIME DEFAULT NULL,
  version INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_item_review_batch (item_id, review_batch_no),
  KEY idx_review_summary_result (summary_id, review_batch_no, result, id),
  KEY idx_review_center (review_type, result, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作量明细审核记录';
```

汇总表新增：

```sql
ALTER TABLE biz_workload_summary
  ADD COLUMN review_batch_no VARCHAR(50) DEFAULT NULL COMMENT '当前明细审核批次',
  ADD COLUMN data_version BIGINT NOT NULL DEFAULT 0 COMMENT '汇总数据版本',
  ADD COLUMN last_calculated_at DATETIME DEFAULT NULL COMMENT '最近核算时间';
CREATE INDEX idx_summary_review_center
  ON biz_workload_summary(status, semester, review_batch_no, user_id);
```

`biz_audit_log` 补 `create_by/create_time/update_by/update_time`，保留 `operator_id/operator_name` 的业务语义；其他缺四字段的 `biz_*` 表同样补齐。历史空值分别回填 `create_by='MIGRATION'`、`create_time=COALESCE(create_time,NOW())`，再收紧为非空。

- [ ] **Step 4: 增加审核中心菜单和最小权限**

使用未占用的 `menu_id=2023` 创建组件菜单 `auditCenter` → `system/auditCenter/index`，权限 `system:auditRecord:list`；子按钮使用 `20231 query`、`20232 review`。role 1/2/3 拥有 list/query/review；role 4 只拥有 `system:workloadSummary:query`，不授审核中心菜单和 review；同时保留现有 submit/teacherConfirm 权限。

- [ ] **Step 5: 在副本库执行并验证**

Run:
```bash
mysql -u wfit -p wflg_workload < sql/22_workload_audit_center.sql
mysql -u wfit -p -N -e "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='wflg_workload' AND table_name='biz_workload_item_review';"
mysql -u wfit -p -N -e "SELECT index_name FROM information_schema.statistics WHERE table_schema='wflg_workload' AND table_name='biz_workload_item_review' ORDER BY index_name;"
```
Expected: 第一条输出 `17`；索引包含 `PRIMARY`、`uk_item_review_batch`、`idx_review_summary_result`、`idx_review_center`，脚本第二次执行无错误且不重复菜单/索引。

- [ ] **Step 6: 提交数据库契约**

```bash
git add rear/sql/01_biz_schema.sql rear/sql/05_biz_menu.sql rear/sql/06_test_accounts.sql rear/sql/22_workload_audit_center.sql
git commit -m "feat: add item review schema and permissions"
```

### Task 2: MyBatis 统一审计字段填充

**Files:**
- Create: `rear/workload-common/src/main/java/com/workload/common/core/domain/AuditableEntity.java`
- Modify: `rear/workload-common/src/main/java/com/workload/common/core/domain/BaseEntity.java`
- Create: `rear/workload-framework/src/main/java/com/workload/framework/mybatis/BizAuditFieldInterceptor.java`
- Modify: `rear/workload-framework/src/main/java/com/workload/framework/config/MyBatisConfig.java:116-131`
- Create: `rear/workload-framework/src/test/java/com/workload/framework/mybatis/BizAuditFieldInterceptorTest.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/domain/BizAuditLog.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizAuditLogMapper.xml`

- [ ] **Step 1: 写失败测试**

测试构造 `MappedStatement` 的 `INSERT/UPDATE` 命令并直接调用插件，至少包含：客户端传 `createBy="spoof"` 时 INSERT 被覆盖；UPDATE 保留 `createBy/createTime` 且覆盖 `updateBy/updateTime`；`@Param("entity")`、`foreach` 集合逐项填充；无登录上下文回退 `SYSTEM`；`sys_*` statement 不处理。核心断言：

```java
assertThat(entity.getCreateBy()).isEqualTo("jiaowu_test");
assertThat(entity.getCreateTime()).isEqualTo(fixedNow);
assertThat(entity.getUpdateBy()).isEqualTo("jiaowu_test");
assertThat(entity.getUpdateTime()).isEqualTo(fixedNow);
```

- [ ] **Step 2: 运行测试并确认失败**

Run（在 `rear/`）：
```bash
mvn test -pl workload-framework -am -Dtest=BizAuditFieldInterceptorTest -Dsurefire.failIfNoSpecifiedTests=false
```
Expected: FAIL，`BizAuditFieldInterceptor`/`AuditableEntity` 尚不存在。

- [ ] **Step 3: 建立可测试的审计上下文和插件**

`AuditableEntity` 明确定义四个 getter/setter；`BaseEntity implements AuditableEntity`。插件只拦截 `Executor.update(MappedStatement,Object)`，依据 `SqlCommandType.INSERT/UPDATE` 和 SQL `BoundSql.getSql()` 的首个表名是否以 `biz_` 开头决定是否处理。递归参数只支持 `AuditableEntity`、`Map`、`Collection`、数组，使用 identity set 防环；INSERT 强制覆盖四字段，UPDATE 只覆盖 update 字段。

插件公开包级构造器便于测试注入：

```java
BizAuditFieldInterceptor(Supplier<String> usernameSupplier,
                         Supplier<Date> nowSupplier)
```

默认 username supplier 捕获未登录异常并返回 `SYSTEM`。不要改写 SQL 字符串；所有业务 Mapper 的 INSERT/UPDATE 必须显式引用四字段。对于 `@Param` 标量状态更新，插件向 `MapperMethod.ParamMap` 注入 `_auditUpdateBy/_auditUpdateTime`，审批 XML 改为：

```xml
update biz_workload_summary
set status = #{toStatus},
    update_by = #{_auditUpdateBy},
    update_time = #{_auditUpdateTime}
where id = #{id} and status = #{fromStatus}
```

由此删除 Controller/Service 对 `createBy/updateBy/createTime/updateTime` 的客户端信任和手填；业务字段 `operator_id/operator_name/reviewer_*` 不由插件替代。

- [ ] **Step 4: 注册插件并统一日志字段**

在 `MyBatisConfig.sqlSessionFactory` 创建插件 Bean 并执行：

```java
sessionFactory.setPlugins(bizAuditFieldInterceptor);
```

`BizAuditLog` 实现 `AuditableEntity`，Mapper INSERT 写入 `create_by/create_time/update_by/update_time`；动作操作人继续由 `BizAuditServiceImpl` 填 `operatorId/operatorName`。

- [ ] **Step 5: 扫描并修正所有 biz Mapper 字段覆盖**

逐个检查 `rear/workload-system/src/main/resources/mapper/system/Biz*Mapper.xml`。每个 INSERT 必须有四字段绑定；每个 UPDATE 必须有 `update_by/update_time`，禁止更新 `create_by/create_time`。子表实体（G1-G6/G11）均继承 `BaseEntity`，直连 Mapper 也由插件覆盖。

- [ ] **Step 6: 运行插件测试与框架编译**

Run:
```bash
mvn test -pl workload-framework -am -Dtest=BizAuditFieldInterceptorTest -Dsurefire.failIfNoSpecifiedTests=false
mvn clean compile -pl workload-admin -am
```
Expected: 插件测试全部 PASS；四模块编译 `BUILD SUCCESS`。

- [ ] **Step 7: 提交审计基础设施**

```bash
git add rear/workload-common/src/main/java/com/workload/common/core/domain/AuditableEntity.java rear/workload-common/src/main/java/com/workload/common/core/domain/BaseEntity.java rear/workload-framework rear/workload-system/src/main/java/com/workload/system/domain rear/workload-system/src/main/resources/mapper/system
git commit -m "feat: fill business audit fields centrally"
```

### Task 3: 明细审核实体、快照生成与并发写入

**Files:**
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/BizWorkloadItemReview.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/ItemReviewCommand.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/vo/ReviewOverviewVo.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadItemReviewMapper.java`
- Create: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadItemReviewMapper.xml`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/BizWorkloadItemReviewService.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/impl/BizWorkloadItemReviewServiceImpl.java`
- Create: `rear/workload-system/src/test/java/com/workload/system/service/impl/BizWorkloadItemReviewServiceImplTest.java`

- [ ] **Step 1: 写服务失败测试**

覆盖：生成批次只取汇总 `userId+semester` 且排除 `item.status=3`；分类优先级；JSON 快照包含 `itemId/itemType/sourceType/courseName/calculatedWorkload/isOverLimit/itemStatus/dataVersion`；SHA-256 稳定；重复生成同批次由唯一键保持幂等；待审当前批次可 PASS；版本不一致、非状态 1、旧批次、REJECT 空原因均抛 `409/400`。

```java
verify(reviewMapper).updateResultIfVersion(
    91L, "B20260912-1003-1", "PASS", null,
    1002L, "jiaowu_test", 0);
assertThatThrownBy(() -> service.reject(91L, new ItemReviewCommand(0, " ")))
    .isInstanceOf(ServiceException.class).hasMessageContaining("驳回原因");
```

- [ ] **Step 2: 运行测试确认失败**

Run:
```bash
mvn test -pl workload-system -Dtest=BizWorkloadItemReviewServiceImplTest
```
Expected: FAIL，新类型/服务不存在。

- [ ] **Step 3: 实现实体和 Mapper**

Mapper 精确提供：

```java
int insertBatch(@Param("reviews") List<BizWorkloadItemReview> reviews);
BizWorkloadItemReview selectById(Long id);
List<BizWorkloadItemReview> selectList(BizWorkloadItemReview filter);
int updateResultIfVersion(@Param("id") Long id,
    @Param("reviewBatchNo") String reviewBatchNo,
    @Param("result") String result, @Param("reason") String reason,
    @Param("reviewerId") Long reviewerId,
    @Param("reviewerName") String reviewerName,
    @Param("version") Integer version);
Map<String, Long> countCurrentBatch(@Param("summaryId") Long summaryId,
    @Param("reviewBatchNo") String reviewBatchNo);
```

条件更新 SQL 必须包含 `id`、`review_batch_no`、`result='PENDING'`、`version=#{version}`，成功时 `version=version+1` 并写 reviewer/time/audit fields。

- [ ] **Step 4: 实现批次与快照**

批次号格式为 `WR-{summaryId}-{dataVersion}-{yyyyMMddHHmmssSSS}`。采用已排序的 `LinkedHashMap` 序列化 JSON，再对 UTF-8 JSON 做 SHA-256；`snapshot_json` 不读取后续变化。分类函数：

```java
if (item.getIsOverLimit() != null && item.getIsOverLimit() == 1) return "OVER_LIMIT";
if ("G11".equals(item.getItemType())) return "G11_ROLE";
if (Set.of("MANUAL", "SELF").contains(item.getSourceType())) return "MANUAL_DECLARATION";
return "TEACHING_TASK";
```

`PASS/REJECT/WAIVE` 共用一个私有 `review(id, command, result)`；先读取审核记录和汇总，执行归属与状态校验，再原子更新，影响行数非 1 返回 409。

- [ ] **Step 5: 运行测试**

Run:
```bash
mvn test -pl workload-system -Dtest=BizWorkloadItemReviewServiceImplTest
```
Expected: PASS，包含并发版本测试。

- [ ] **Step 6: 提交明细审核核心**

```bash
git add rear/workload-system/src/main/java/com/workload/system/domain rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadItemReviewMapper.java rear/workload-system/src/main/resources/mapper/system/BizWorkloadItemReviewMapper.xml rear/workload-system/src/main/java/com/workload/system/service rear/workload-system/src/test/java/com/workload/system/service/impl/BizWorkloadItemReviewServiceImplTest.java
git commit -m "feat: add versioned workload item reviews"
```

### Task 4: 把明细审核批次接入汇总状态机

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/BizAuditService.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/impl/BizAuditServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadSummaryMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadSummaryMapper.xml`
- Create: `rear/workload-system/src/test/java/com/workload/system/service/impl/BizAuditServiceImplTest.java`

- [ ] **Step 1: 写状态机失败测试**

测试精确顺序：`submit` 在同一事务内生成审核批次，再执行 `0→1` 并写 audit log；无有效明细返回 409；`approve` 在当前批次存在 PENDING/REJECT 时返回 409，全部 PASS/WAIVE 才执行 `1→2`；`reject/unlock` 不删除历史审核；并发 submit 失败不会留下孤立批次。

```java
InOrder inOrder = inOrder(reviewService, summaryMapper, auditLogMapper);
inOrder.verify(reviewService).createBatch(summary);
inOrder.verify(summaryMapper).submitWithReviewBatch(
    summary.getId(), 0, 1, generatedBatchNo);
inOrder.verify(auditLogMapper).insertBizAuditLog(any());
```

- [ ] **Step 2: 运行测试确认失败**

Run:
```bash
mvn test -pl workload-system -Dtest=BizAuditServiceImplTest
```
Expected: FAIL，现有 submit 不生成批次且 approve 无门禁。

- [ ] **Step 3: 接入 submit/approve 门禁**

`submitWithReviewBatch` 原子 SQL 同时设置 `status=1,review_batch_no=#{batchNo}`，条件为 `id/status/data_version`，避免快照后被并发重算。`approve` 调 `countCurrentBatch`，仅在 `pending=0,reject=0,total>0` 时继续；错误文本必须包含三项计数，便于前端展示。

批量提交继续通过 `AopContext.currentProxy().submit(id)` 保持每个汇总独立事务；单人失败写入 `failDetails(id,reason)`，其他记录照常提交。

- [ ] **Step 4: 让核算版本可验证**

修改汇总重算 UPDATE：仅状态 `0` 可写，每次成功 `data_version=data_version+1,last_calculated_at=NOW()`；状态 `1/2` 仍 fail-closed。返回对象重新查询，确保前端拿到数据库递增后的版本。

- [ ] **Step 5: 运行审批与核算回归**

Run:
```bash
mvn test -pl workload-system -Dtest=BizAuditServiceImplTest,WorkloadCalcServiceImplTest,SummaryCalcServiceImplTest
```
Expected: 新状态门禁测试 PASS；若后两类在仓库不存在，Surefire 输出 `No tests matching pattern` 时改运行 `mvn test -pl workload-system`，最终全部 PASS。

- [ ] **Step 6: 提交状态机集成**

```bash
git add rear/workload-system/src/main/java/com/workload/system/service/BizAuditService.java rear/workload-system/src/main/java/com/workload/system/service/impl/BizAuditServiceImpl.java rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadSummaryMapper.java rear/workload-system/src/main/resources/mapper/system/BizWorkloadSummaryMapper.xml rear/workload-system/src/test/java/com/workload/system/service/impl/BizAuditServiceImplTest.java
git commit -m "feat: gate summary approval on item reviews"
```

### Task 5: reviews、auditLogs 分页与汇总详情

**Files:**
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/vo/WorkloadSummaryDetailVo.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/WorkloadSummaryDetailService.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/impl/WorkloadSummaryDetailServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/controller/BizWorkloadSummaryController.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizAuditLogMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizAuditLogMapper.xml`
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadItemMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadItemMapper.xml`
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizTeachingTaskMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizTeachingTaskMapper.xml`
- Create: `rear/workload-system/src/test/java/com/workload/system/service/impl/WorkloadSummaryDetailServiceImplTest.java`
- Modify: `rear/workload-system/src/test/java/com/workload/system/controller/BizWorkloadSummaryControllerTest.java`

- [ ] **Step 1: 写详情聚合失败测试**

固定统计口径并断言：

```text
teachingTaskRowCount = user_id+semester 下全部 biz_teaching_task
validTaskCount       = 同范围 status=1 的教学任务
importRowCount       = 同范围 import_batch 非空的教学任务数（不是 Excel 原始行数）
itemCount            = 同范围 status<>3 的工作量明细数
reviewPending/Pass/Reject/WaiveCount = 当前 review_batch_no 的结果数
```

`detail` 只返回 `summary/counts/lastCalculatedAt/dataVersion/reviewBatchNo`；不携带 reviews/auditLogs 数组。教师访问他人 summary 必须抛异常。

- [ ] **Step 2: 运行测试确认失败**

Run:
```bash
mvn test -pl workload-system -Dtest=WorkloadSummaryDetailServiceImplTest,BizWorkloadSummaryControllerTest
```
Expected: FAIL，详情服务和新 Controller 方法不存在。

- [ ] **Step 3: 实现聚合查询与三个 GET 接口**

```text
GET /system/workloadSummary/{id}/detail
  permission: system:workloadSummary:query
GET /system/workloadSummary/{id}/reviews?pageNum=1&pageSize=10&reviewType=&result=
  permission: system:workloadSummary:query
GET /system/workloadSummary/{id}/auditLogs?pageNum=1&pageSize=10&action=
  permission: system:workloadSummary:query
```

每个接口先查 summary 并执行 `DataScopeUtil.assertOwnOrAdmin(summary.userId)`；列表接口在归属校验后 `startPage()`，且服务端强制 `pageSize≤100`。reviews 默认只查当前批次，显式 `history=true` 时按 `create_time desc,id desc` 查全部批次；auditLogs 固定 `create_time desc,id desc`。

- [ ] **Step 4: 用可命中索引的 SQL 聚合**

审核统计必须限定 `summary_id + review_batch_no`，命中 `idx_review_summary_result`；日志分页限定 `summary_id`，命中 `idx_audit_log_summary`；教学任务统计使用现有 `idx_user_sem`。通过 `EXPLAIN` 验证 key 不为空：

```sql
EXPLAIN SELECT result, COUNT(*) FROM biz_workload_item_review
WHERE summary_id=1 AND review_batch_no='WR-1-1-x' GROUP BY result;
EXPLAIN SELECT * FROM biz_audit_log
WHERE summary_id=1 ORDER BY create_time DESC,id DESC LIMIT 10;
```

- [ ] **Step 5: 运行测试**

Run:
```bash
mvn test -pl workload-system -Dtest=WorkloadSummaryDetailServiceImplTest,BizWorkloadSummaryControllerTest
```
Expected: PASS，包括教师 IDOR、空批次计数为 0、分页参数封顶。

- [ ] **Step 6: 提交查询契约**

```bash
git add rear/workload-system/src/main/java/com/workload/system/domain/vo/WorkloadSummaryDetailVo.java rear/workload-system/src/main/java/com/workload/system/service rear/workload-system/src/main/java/com/workload/system/controller/BizWorkloadSummaryController.java rear/workload-system/src/main/java/com/workload/system/mapper rear/workload-system/src/main/resources/mapper/system rear/workload-system/src/test/java/com/workload/system
git commit -m "feat: add paged summary audit details"
```

### Task 6: 独立审核中心 API

**Files:**
- Create: `rear/workload-system/src/main/java/com/workload/system/controller/BizWorkloadItemReviewController.java`
- Create: `rear/workload-system/src/test/java/com/workload/system/controller/BizWorkloadItemReviewControllerTest.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/BizWorkloadItemReviewService.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/impl/BizWorkloadItemReviewServiceImpl.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadItemReviewMapper.xml`

- [ ] **Step 1: 写 Controller 失败测试**

覆盖分页 overview、单条详情、PASS/REJECT/WAIVE、批量审核。教师角色的查询 userId 被强制为本人，但因为教师没有 `system:auditRecord:list`，集成权限层最终返回 403；服务层仍保留 userId 收口以防权限误配。批量命令每条独立事务并返回 `successCount/failCount/failDetails`。

- [ ] **Step 2: 运行测试确认失败**

Run:
```bash
mvn test -pl workload-system -Dtest=BizWorkloadItemReviewControllerTest
```
Expected: FAIL，Controller 尚不存在。

- [ ] **Step 3: 实现准确接口**

```text
GET  /system/audit/record/list?semester=&userId=&reviewType=&result=&pageNum=&pageSize=
GET  /system/audit/record/{id}
POST /system/audit/record/{id}/pass    body {version,reason}
POST /system/audit/record/{id}/reject  body {version,reason}
POST /system/audit/record/{id}/waive   body {version,reason}
POST /system/audit/record/batch        body {ids,result,reason,versions:{"91":0}}
```

list/query 权限为 `system:auditRecord:list/query`，写权限统一为 `system:auditRecord:review`。overview SQL 只显示 `summary.status=1` 且 `review.review_batch_no=summary.review_batch_no` 的当前待审批次，关联 `sys_user` 返回 `teacherName/teacherCode`，按 `review.create_time asc,review.id asc` 保持队列稳定。

- [ ] **Step 4: 实现批量独立事务**

与现有 `batchSubmit` 一致，通过代理逐条调用单条 review；一个版本冲突不回滚已成功项。禁止用一条 `UPDATE ... WHERE id IN (...)` 绕过逐条版本和状态检查。

- [ ] **Step 5: 运行测试和模块测试**

Run:
```bash
mvn test -pl workload-system -Dtest=BizWorkloadItemReviewControllerTest,BizWorkloadItemReviewServiceImplTest
mvn test -pl workload-system
```
Expected: 全部 PASS。

- [ ] **Step 6: 提交审核中心 API**

```bash
git add rear/workload-system/src/main/java/com/workload/system/controller/BizWorkloadItemReviewController.java rear/workload-system/src/main/java/com/workload/system/service rear/workload-system/src/main/resources/mapper/system/BizWorkloadItemReviewMapper.xml rear/workload-system/src/test/java/com/workload/system
git commit -m "feat: expose workload audit center APIs"
```

### Task 7: 前端测试基建与 API 客户端

**Files:**
- Modify: `front/RuoYi-Vue3/package.json`
- Modify: `front/RuoYi-Vue3/vite.config.js`
- Create: `front/RuoYi-Vue3/src/api/system/itemReview.js`
- Modify: `front/RuoYi-Vue3/src/api/system/workloadSummary.js`

- [ ] **Step 1: 先写 API 契约测试**

创建 `src/api/system/__tests__/auditApi.spec.js`，mock `@/utils/request`，断言 URL、method、params/body，特别是分页参数不塞入路径、审核命令走 JSON body。

- [ ] **Step 2: 安装固定版本测试依赖并确认测试失败**

Run（在 `front/RuoYi-Vue3/`）：
```bash
npm install --save-dev --save-exact vitest@3.2.4 jsdom@26.1.0 @vue/test-utils@2.4.6
npm run test:unit -- src/api/system/__tests__/auditApi.spec.js
```
Expected: 首次在脚本尚未配置时失败；加入 `"test:unit":"vitest run"` 后 FAIL，API 导出不存在。提交时同时纳入 `package-lock.json`。

- [ ] **Step 3: 实现 API 客户端**

`workloadSummary.js` 新增：

```js
export const getSummaryDetail = id => request({ url: `/system/workloadSummary/${id}/detail`, method: 'get' })
export const listSummaryReviews = (id, params) => request({ url: `/system/workloadSummary/${id}/reviews`, method: 'get', params })
export const listSummaryAuditLogs = (id, params) => request({ url: `/system/workloadSummary/${id}/auditLogs`, method: 'get', params })
```

`itemReview.js` 新增 `listItemReviews/getItemReview/passItemReview/rejectItemReview/waiveItemReview/batchReview`，写命令统一传 `data`。

- [ ] **Step 4: 运行 API 测试、lint、构建**

Run:
```bash
npm run test:unit -- src/api/system/__tests__/auditApi.spec.js
npm run lint
npm run build:prod
```
Expected: 测试 PASS、ESLint 0 errors、Vite build 成功。

- [ ] **Step 5: 提交前端基础设施**

```bash
git add front/RuoYi-Vue3/package.json front/RuoYi-Vue3/package-lock.json front/RuoYi-Vue3/vite.config.js front/RuoYi-Vue3/src/api/system front/RuoYi-Vue3/src/api/system/__tests__/auditApi.spec.js
git commit -m "test: add frontend audit API coverage"
```

### Task 8: 汇总详情抽屉

**Files:**
- Create: `front/RuoYi-Vue3/src/views/system/workloadSummary/SummaryAuditDrawer.vue`
- Modify: `front/RuoYi-Vue3/src/views/system/workloadSummary/index.vue:563-747`
- Create: `front/RuoYi-Vue3/src/views/system/workloadSummary/__tests__/SummaryAuditDrawer.spec.js`

- [ ] **Step 1: 写失败组件测试**

测试打开时加载 detail；四个分区为“汇总结果/来源数据/逐条审核/状态时间线”；reviews 与 auditLogs 分别翻页且互不重置；筛 reviewType/result 会回到 review 第 1 页；审核动作后刷新 detail 和当前 review 页；403 不渲染他人数据。

```js
expect(wrapper.text()).toContain('有效教学任务 8')
await wrapper.find('[data-test="review-next"]').trigger('click')
expect(listSummaryReviews).toHaveBeenLastCalledWith(7, expect.objectContaining({ pageNum: 2 }))
```

- [ ] **Step 2: 运行测试确认失败**

Run:
```bash
npm run test:unit -- src/views/system/workloadSummary/__tests__/SummaryAuditDrawer.spec.js
```
Expected: FAIL，组件不存在。

- [ ] **Step 3: 实现抽屉**

抽屉由父页传 `modelValue/summaryId/canReview`。详情卡显示 `teachingTaskRowCount/validTaskCount/importRowCount/itemCount`，审核统计显示四种结果；reviews 表格显示类别、项目、快照工作量、来源、异常、审核人、原因；时间线显示 action/fromStatus/toStatus/operator/reason/createTime。两个 `<pagination>` 各自绑定独立 state，请求切换时用递增 token 丢弃过期响应。

- [ ] **Step 4: 替换父页旧抽屉**

保留 `handleDetail(row)` 入口，但只设置 `detailSummaryId` 并打开 `SummaryAuditDrawer`；删除父页重复的详情展示与请求状态。教师 `canReview=false`，教务由 `v-hasPermi` 控制按钮，API 后端仍作权限校验。

- [ ] **Step 5: 运行组件测试和前端校验**

Run:
```bash
npm run test:unit -- src/views/system/workloadSummary/__tests__/SummaryAuditDrawer.spec.js
npm run lint
npm run build:prod
```
Expected: PASS、0 lint errors、build 成功。

- [ ] **Step 6: 提交详情抽屉**

```bash
git add front/RuoYi-Vue3/src/views/system/workloadSummary
git commit -m "feat: show paged summary audit details"
```

### Task 9: 独立审核中心页面

**Files:**
- Create: `front/RuoYi-Vue3/src/views/system/auditCenter/index.vue`
- Create: `front/RuoYi-Vue3/src/views/system/auditCenter/__tests__/index.spec.js`

- [ ] **Step 1: 写失败组件测试**

覆盖：学期/教师/审核类型/结果筛选；默认查询当前待审汇总；PASS 二次确认；REJECT 必填原因；WAIVE 必填原因；批量操作传每行 version；409 后提示“数据已变化”并刷新；分页后保持筛选。

- [ ] **Step 2: 运行测试确认失败**

Run:
```bash
npm run test:unit -- src/views/system/auditCenter/__tests__/index.spec.js
```
Expected: FAIL，页面不存在。

- [ ] **Step 3: 实现页面结构与交互**

顶部查询栏使用 `semester-select`、`user-select`、reviewType/result select；统计区来自当前分页响应的服务端总计，不用当前页长度冒充全量；表格显示教师、工号、学期、类型、项目、核定工作量、来源、异常、状态、版本。单条驳回/豁免使用不可跳过的 textarea 对话框，最长 500 字；通过可选原因。

批量操作仅作用于已选行，并发送：

```js
{
  ids: selected.map(row => row.id),
  result: 'PASS',
  reason: null,
  versions: Object.fromEntries(selected.map(row => [String(row.id), row.version]))
}
```

完成后展示成功/失败数并刷新；失败行保留可见原因。

- [ ] **Step 4: 运行测试、lint、build**

Run:
```bash
npm run test:unit -- src/views/system/auditCenter/__tests__/index.spec.js
npm run lint
npm run build:prod
```
Expected: PASS、0 lint errors、build 成功；路由由 `sys_menu.component='system/auditCenter/index'` 动态加载。

- [ ] **Step 5: 提交审核中心页面**

```bash
git add front/RuoYi-Vue3/src/views/system/auditCenter
git commit -m "feat: add workload item audit center"
```

### Task 10: 全链路审计回归与最终验证

**Files:**
- Modify: `rear/workload-system/src/test/java/com/workload/system/service/impl/BizAuditServiceImplTest.java`
- Modify: `rear/workload-framework/src/test/java/com/workload/framework/mybatis/BizAuditFieldInterceptorTest.java`
- Modify: `front/RuoYi-Vue3/src/views/system/workloadSummary/__tests__/SummaryAuditDrawer.spec.js`
- Modify: `front/RuoYi-Vue3/src/views/system/auditCenter/__tests__/index.spec.js`

- [ ] **Step 1: 补齐验收矩阵**

后端补：导入逐行、G11 自动生成、单条重算、批量核算、汇总提交、明细审核、驳回、解锁均断言审计字段；异常回滚不留 review；两个审核员用同 version 竞争只有一个成功。前端补：空数据、分页总数、旧响应丢弃、409 刷新、教师只读。

- [ ] **Step 2: 运行后端全部测试**

Run（在 `rear/`）：
```bash
mvn test -pl workload-system
mvn test -pl workload-framework -am -Dsurefire.failIfNoSpecifiedTests=false
mvn clean package -DskipTests
```
Expected: 三条命令均 `BUILD SUCCESS`；无 skipped failure。

- [ ] **Step 3: 运行前端全部检查**

Run（在 `front/RuoYi-Vue3/`）：
```bash
npm run test:unit
npm run lint
npm run build:prod
```
Expected: 全部测试 PASS、ESLint 0 errors、生产构建成功。

- [ ] **Step 4: 执行本地 API 冒烟**

启动后端和前端后，以 `jiaowu_test` 验证：草稿提交生成当前批次；审核中心分页；驳回原因校验；全部 PASS 后汇总 approve 成功；详情 reviews/auditLogs 翻页。以 `teacher_test` 验证本人详情 200、他人详情 403、审核写接口 403。直接查库：

```sql
SELECT id,status,review_batch_no,data_version,last_calculated_at,
       create_by,create_time,update_by,update_time
FROM biz_workload_summary WHERE id=?;
SELECT item_id,review_type,result,version,reviewer_name,reviewed_at,
       create_by,create_time,update_by,update_time
FROM biz_workload_item_review WHERE summary_id=? ORDER BY id;
SELECT action,operator_name,reason,create_by,create_time
FROM biz_audit_log WHERE summary_id=? ORDER BY id;
```

Expected: 每条记录归属正确、无空审计人/时间、当前批次与汇总一致、历史批次仍保留。

- [ ] **Step 5: 提交最终回归测试**

```bash
git add rear/workload-system/src/test rear/workload-framework/src/test front/RuoYi-Vue3/src/views/system
git commit -m "test: cover workload audit center workflow"
```

## 自查结论

- 需求 5：Task 1/3/4/6 覆盖专用明细审核表、批次快照、审核分类、乐观锁、状态门禁与批量回执；明确禁止 `target_type/target_id`。
- 需求 9：Task 5/8/9 覆盖汇总详情抽屉、独立审核中心、来源计数口径、`reviews/auditLogs` 独立分页和教师数据范围。
- 需求 15：Task 1/2/10 覆盖全部 `biz_*` 表四审计字段、客户端覆盖防护、导入/生成器/重算/审批直连 Mapper 与 `SYSTEM` 回退。
- 类型一致性：数据库 `review_batch_no/data_version/last_calculated_at` 对应 Java/JSON `reviewBatchNo/dataVersion/lastCalculatedAt`；审核写命令统一 `version/reason`；状态值只使用本文定义集合。
- 占位符扫描：计划不含 `TBD`、`TODO`、“类似 Task N”或未定义接口；所有实施步骤均有精确文件、命令和预期结果。
