# 核算一致性与可解释性实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成需求 4、6、10、11、12、13，使导入、G11 生成、明细/汇总/酬金核算与其他酬金写入遵守同一冻结边界，并让每个核算结果可追溯到源数据、因子来源和规则快照。

**Architecture:** 先建立数据库条件写与汇总行锁两层并发边界：源数据新增类事务锁定同一教师学期的汇总行，已有记录更新类操作使用 `WHERE` 冻结条件并检查影响行数。随后以不可变计算快照承载 G1/G2/G3 的公式、因子、来源和版本；G11 则直接同步教务给出的本学期岗位减免值，不再按岗位名称、学年标准或任职天数推导。系数申请、分类模板、教师友好的详情抽屉及阶段化一键核算均调用同一核算服务。

**Tech Stack:** Java 17、Spring Boot 4.0.7、MyBatis、MySQL 8、JUnit 5、Mockito、AssertJ、Vue 3.5、Element Plus 2.13、Vite 6、EasyExcel 4.0.3。

---

## 2026-09-18 实施记录

- Tasks 1–3 已在前批提交完成；本轮完成 Task 4 和 Task 5，五项写入安全门已落地。
- G11 直接同步教务确认的本学期减免原值，保留来源批次，180 封顶由汇总层执行。岗位源记录增改删接入冻结锁，编辑保留数据库教师/学期归属；表单补齐必填学期，教师详情与导出改用学期口径。
- 并发补强：教师学期锁后对岗位来源执行 `FOR UPDATE` 当前读；`uk_assignment_sem` 防止尚无汇总时重复生成 G11；主明细已核对时跳过，异议/驳回明细仍按既有语义同步。
- Summary/Pay/Allowance 使用专用条件 SQL，更新/删除必须影响一行；唯一键冲突后的更新也受保护。计算保存不覆盖签字/完结状态，预览保持只读；其他酬金批删任一失败则事务回滚。
- 数据库脚本 `22_semester_position_workload.sql` 已提供并在隔离 MySQL 验证：补来源批次列与 G11 唯一键，可重复执行。仅升级结构，不转换历史学年值；本轮未修改业务数据库。
- 验证：后端完整 `clean test` 175/175 通过；前端生产构建、变更文件 ESLint、`git diff --check` 通过；隔离 MySQL 8.0.46 经真实 MyBatis SQL 完成 29 项断言，独立复审无未处理问题。
- 后续从 Task 6 的不可变计算快照继续；尚未实施系数申请、分类导入或阶段化一键核算。

## 范围、顺序与不可变约束

本计划覆盖《教务工作量管理系统 16 项需求实施方案》的需求 4、6、10、11、12、13。执行顺序固定为：冻结与条件写（Tasks 1-5）→ 快照与 G11（Tasks 6-7）→ 系数申请（Tasks 8-9）→ 分类导入和详情（Tasks 10-11）→ 一键核算流程（Task 12）→ 全量验证（Task 13）。前五项是安全门，未全部合并前不得开发会增加写入口的后续功能。

冻结规则只有一套：`biz_workload_summary.status=0` 可写，`1` 待审禁止改数，`2` 已完结永久锁定。已完结记录不得通过 unlock 复活；差额只追加独立线下补差说明，不更新源数据、明细、G11、其他酬金、汇总、酬金或审核快照。`biz_workload_item.status=1` 额外冻结该明细；预览只读，不受冻结阻断。

并发规则分两类：

1. 导入/G11 新建尚无目标行可条件更新，事务开头调用 `WorkloadWriteGuard.lockDraftOrAbsent(userId, semester)`，对已存在的汇总执行 `SELECT ... FOR UPDATE`；审批更新同一汇总行时必须等待该事务完成。
2. 明细、汇总、酬金、其他酬金已有行更新/删除必须使用专用 Mapper 条件 SQL，并检查影响行数恰为 1；禁止以“先查状态、后通用 UPDATE”作为最终保护。

G11 来源固定为教务导入/维护的教师学期岗位减免记录：`userId + semester + positionWorkload + sourceBatchId`。生成器只做幂等同步和 180 封顶前明细落账，不读取岗位目录、不除以 2、不按任职区间折算。历史 `source_type='IMPORT'` 可直接保留为来源标识；当前环境无真实数据，fresh DB 重建后不需要兼容历史 SELF G11。

## 文件结构与职责

- Create: `rear/workload-system/src/main/java/com/workload/system/calc/WorkloadWriteGuard.java`：教师+学期冻结锁与统一错误文案。
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/BizWorkloadCalcSnapshot.java`：不可变公式快照实体。
- Create: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadCalcSnapshotMapper.java`：快照插入与查询。
- Create: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadCalcSnapshotMapper.xml`：快照 SQL。
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/BizCoefficientAdjustment.java`：G1/G2 因子申请实体。
- Create: `rear/workload-system/src/main/java/com/workload/system/service/ICoefficientAdjustmentService.java` and `service/impl/CoefficientAdjustmentServiceImpl.java`：申请状态机和生效编排。
- Create: `rear/workload-system/src/main/java/com/workload/system/controller/BizCoefficientAdjustmentController.java`：申请/审核 API。
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/CalculationRunRequest.java` and `domain/vo/CalculationRunResult.java`：阶段化一键核算契约。
- Create: `rear/sql/22_calculation_consistency.sql`、`23_coefficient_adjustment.sql`、`24_calculation_menu.sql`：幂等迁移。
- Create: `front/RuoYi-Vue3/src/api/system/coefficientAdjustment.js` and `views/system/coefficientAdjustment/index.vue`：系数申请与审核。
- Modify: `TeachingTaskImportServiceImpl`、`ManagementItemGeneratorImpl`、`WorkloadCalcServiceImpl`、`SummaryCalcServiceImpl`、`PayCalcServiceImpl`、`BizAllowanceItemServiceImpl`：统一冻结、条件写和快照。
- Modify: 对应 Mapper/XML、测试、`teachingTask/index.vue`、`workloadItem/index.vue`、`workloadSummary/index.vue`：分类入口、可解释详情、阶段结果。

### Task 1: 建立统一冻结锁（需求 4，安全门）

**Files:**
- Create: `rear/workload-system/src/main/java/com/workload/system/calc/WorkloadWriteGuard.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadSummaryMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadSummaryMapper.xml`
- Test: `rear/workload-system/src/test/java/com/workload/system/calc/WorkloadWriteGuardTest.java`

- [ ] **Step 1: 写失败测试：草稿/待审/完结/无汇总四态**

```java
@ExtendWith(MockitoExtension.class)
class WorkloadWriteGuardTest {
    @Mock BizWorkloadSummaryMapper mapper;
    @InjectMocks WorkloadWriteGuard guard;

    @Test void absentSummaryAllowsSourceCreation() {
        when(mapper.selectByUserSemesterForUpdate(1L, "2025-2026-1")).thenReturn(null);
        assertThatCode(() -> guard.lockDraftOrAbsent(1L, "2025-2026-1")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void submittedSummaryRejectsEveryWrite(int status) {
        BizWorkloadSummary s = new BizWorkloadSummary(); s.setStatus(status);
        when(mapper.selectByUserSemesterForUpdate(1L, "2025-2026-1")).thenReturn(s);
        assertThatThrownBy(() -> guard.lockDraftOrAbsent(1L, "2025-2026-1"))
            .isInstanceOf(ServiceException.class).hasMessageContaining("已进入审批流程");
    }
}
```

- [ ] **Step 2: 运行 RED**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=WorkloadWriteGuardTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL，`WorkloadWriteGuard`/`selectByUserSemesterForUpdate` 不存在。

- [ ] **Step 3: 实现行锁查询与 Guard**

Mapper 签名必须是：

```java
BizWorkloadSummary selectByUserSemesterForUpdate(@Param("userId") Long userId,
        @Param("semester") String semester);
```

XML 必须使用：

```sql
select <所有 BizWorkloadSummaryResult 列>
from biz_workload_summary
where user_id = #{userId} and semester = #{semester}
for update
```

Guard 公共方法保持唯一：

```java
@Transactional(propagation = Propagation.MANDATORY)
public void lockDraftOrAbsent(Long userId, String semester) {
    if (userId == null || !StringUtils.hasText(semester))
        throw new ServiceException("缺少教师或学期，无法校验冻结状态");
    BizWorkloadSummary summary = mapper.selectByUserSemesterForUpdate(userId, semester);
    if (summary != null && WorkloadSummaryStatus.isWriteFrozen(summary.getStatus()))
        throw new ServiceException("学期汇总已进入审批流程，数据已冻结");
}
```

- [ ] **Step 4: 运行 GREEN**

Run: 与 Step 2 相同。
Expected: PASS，4 个状态用例全部通过。

- [ ] **Step 5: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/calc/WorkloadWriteGuard.java rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadSummaryMapper.java rear/workload-system/src/main/resources/mapper/system/BizWorkloadSummaryMapper.xml rear/workload-system/src/test/java/com/workload/system/calc/WorkloadWriteGuardTest.java
git commit -m "fix: 建立核算写入冻结锁"
```

### Task 2: 封堵教学任务导入绕过冻结（需求 4，已确认缺口）

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/impl/TeachingTaskImportServiceImpl.java:128-160`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/impl/BizTeachingTaskServiceImpl.java:73-125`
- Test: `rear/workload-system/src/test/java/com/workload/system/service/impl/TeachingTaskImportServiceImplTest.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/service/impl/BizTeachingTaskServiceImplTest.java`

- [ ] **Step 1: 写失败测试：导入与 CRUD 在写库前锁定教师学期**

```java
@Test void importRowChecksFreezeBeforeRepeatCountAndInsert() {
    assertThatThrownBy(() -> service.processSingleRow(validG1(), "IMP-1"))
        .isInstanceOf(ServiceException.class).hasMessageContaining("冻结");
    InOrder order = inOrder(writeGuard, teachingTaskMapper, workloadItemMapper);
    order.verify(writeGuard).lockDraftOrAbsent(USER, SEMESTER);
    verifyNoInteractions(workloadItemMapper);
    verify(teachingTaskMapper, never()).insertBizTeachingTask(any());
}
```

为 `BizTeachingTaskServiceImpl.insertBizTeachingTask`、`updateBizTeachingTask`、`deleteBizTeachingTaskByIds` 分别断言按数据库中的 `userId+semester` 调 Guard；更新/删除不得信任请求体归属。

- [ ] **Step 2: 运行 RED**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=TeachingTaskImportServiceImplTest,BizTeachingTaskServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL，Guard 未调用，导入仍继续 INSERT。

- [ ] **Step 3: 最小实现**

在 `processSingleRow(TeachingTaskImportDTO dto, String batchNo)` 中 `findUser` 后、`resolveRepeatOrder` 前调用：

```java
workloadWriteGuard.lockDraftOrAbsent(user.getUserId(), dto.getSemester());
```

`BizTeachingTaskServiceImpl` 三类写入口增加 `@Transactional(rollbackFor = Exception.class)`；update/delete 先 `selectBizTeachingTaskById(id)`，不存在抛错，存在则以数据库归属调用同一 Guard。不要在 Controller 复制冻结判断。

- [ ] **Step 4: 运行 GREEN 与导入回归**

Run: 与 Step 2 相同。
Expected: PASS；待审/完结时教学任务、工作量主表和 G 子表均零写入，草稿/无汇总仍成功。

- [ ] **Step 5: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/service/impl/TeachingTaskImportServiceImpl.java rear/workload-system/src/main/java/com/workload/system/service/impl/BizTeachingTaskServiceImpl.java rear/workload-system/src/test/java/com/workload/system/service/impl/TeachingTaskImportServiceImplTest.java rear/workload-system/src/test/java/com/workload/system/service/impl/BizTeachingTaskServiceImplTest.java
git commit -m "fix: 冻结审批中的教学任务写入"
```

### Task 3: 原子保护单条明细重算（需求 4，已确认缺口）

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadItemMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadItemMapper.xml`
- Modify: `rear/workload-system/src/main/java/com/workload/system/calc/WorkloadCalcServiceImpl.java:60-80`
- Test: `rear/workload-system/src/test/java/com/workload/system/calc/WorkloadCalcServiceImplTest.java`

- [ ] **Step 1: 写失败测试：检查后状态变化时 UPDATE=0 必须冲突**

```java
@Test void recalcItemRejectsConcurrentFreeze() {
    givenDraftItem(); when(strategyFactory.get("G1")).thenReturn(strategy);
    when(strategy.calculate(any())).thenReturn(new BigDecimal("52.80"));
    when(itemMapper.updateCalculationIfEditable(any(), eq(0))).thenReturn(0);
    assertThatThrownBy(() -> service.recalcItem(ITEM_ID))
        .isInstanceOf(ServiceException.class).hasMessageContaining("状态已变化");
    verify(itemMapper, never()).updateBizWorkloadItem(any());
}
```

- [ ] **Step 2: 运行 RED**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=WorkloadCalcServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL，专用 Mapper 方法不存在。

- [ ] **Step 3: 实现专用条件 UPDATE**

签名：

```java
int updateCalculationIfEditable(@Param("item") BizWorkloadItem item,
        @Param("draftStatus") Integer draftStatus);
```

SQL 必须同时约束明细与汇总，且只更新计算字段：

```sql
update biz_workload_item i
set i.calculated_workload=#{item.calculatedWorkload},
    i.is_over_limit=#{item.isOverLimit}, i.update_time=#{item.updateTime}
where i.id=#{item.id}
  and ifnull(i.status, 0)=#{draftStatus}
  and not exists (
    select 1 from biz_workload_summary s
    where s.user_id=i.user_id and s.semester=i.semester
      and ifnull(s.status, 0)&lt;&gt;#{draftStatus}
  )
```

`recalcItem(Long itemId)` 保留前置 `assertEditable(item)` 以快速报错，但最终只调用此 SQL；影响行数不是 1 时抛 `ServiceException("明细或汇总状态已变化，请刷新后重试")`。

- [ ] **Step 4: 运行 GREEN**

Run: 与 Step 2 相同。
Expected: PASS；同时断言成功路径仅更新 `calculated_workload/is_over_limit/update_time`。

- [ ] **Step 5: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadItemMapper.java rear/workload-system/src/main/resources/mapper/system/BizWorkloadItemMapper.xml rear/workload-system/src/main/java/com/workload/system/calc/WorkloadCalcServiceImpl.java rear/workload-system/src/test/java/com/workload/system/calc/WorkloadCalcServiceImplTest.java
git commit -m "fix: 原子保护明细重算写入"
```

### Task 4: 原子同步本学期岗位减免值到 G11（需求 10，替代原岗位折算模型）

**Files:**
- Modify: `ManagementItemGeneratorImpl.java`、`BizRoleAssignmentMapper.xml`、`BizWlManagementMapper.xml`
- Test: `ManagementItemGeneratorImplTest.java`

- [x] **Step 1: 写失败测试**：来源 `positionWorkload=90` 时 G11 为 90，不除以 2、不读取任职日期；冻结/已核对时主子表零写入；重复同步保持同一 item。
- [x] **Step 2: 运行 RED**：现有生成器仍按 `allowance_rate ÷ 2 × overlapDays/semesterDays`，断言应失败。
- [x] **Step 3: 实现简化同步**：查询教师+学期有效减免记录，直接使用学期值；职务名只写说明；生成前调用 Guard，子表更新采用草稿条件 SQL 并检查影响行数。
- [x] **Step 4: 更新教师详情文案**：返回“岗位减免工作量（本学期）”“来源批次”“计入 G11”，移除折算日数和 YEAR/SEMESTER 展示。
- [x] **Step 5: 运行 GREEN**：冻结、幂等、直接值和 180 汇总封顶测试全部通过。
- [x] **Step 6: 提交**：`git commit -m "feat: 按学期岗位减免同步G11"`。

### [已废弃] 原 Task 4: 修复 G11 先写后查和已核对子表漂移（需求 10，已确认缺口）

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/calc/ManagementItemGeneratorImpl.java:60-190`
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadItemMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadItemMapper.xml`
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWlManagementMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizWlManagementMapper.xml`
- Test: `rear/workload-system/src/test/java/com/workload/system/calc/ManagementItemGeneratorImplTest.java`

- [ ] **Step 1: 写失败测试：冻结先于任何写，已核对时子表也不改**

```java
@Test void confirmedG11DoesNotUpdateSnapshotDetail() {
    stubExistingItem(1); generator.generate(USER, SEMESTER);
    verify(managementMapper, never()).updateProrationIfEditable(any(), any(), any(), any());
    verify(workloadCalcService, never()).recalcItem(any());
}

@Test void generationLocksSummaryBeforeInsert() {
    stubNoExistingItem(); generator.generate(USER, SEMESTER);
    InOrder order = inOrder(writeGuard, itemMapper);
    order.verify(writeGuard).lockDraftOrAbsent(USER, SEMESTER);
    order.verify(itemMapper).insertBizWorkloadItem(any());
}
```

- [ ] **Step 2: 运行 RED**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=ManagementItemGeneratorImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL；当前先更新 `biz_wl_management`，然后才根据 item.status 跳过主表重算。

- [ ] **Step 3: 重排生成事务并加入条件更新**

`generate(Long userId, String semester)` 开头调用 Guard；`generateForSemester` 不嵌套 `generate` 自调用事务假象，而对每位教师调用代理接口或将 Guard 放入 `generateOne` 之前。已有 item 时先判 `item.status==1`，立即记录 `skippedConfirmed++` 并返回，不得修改 detail。

专用 SQL：

```sql
update biz_wl_management d
join biz_workload_item i on i.id=d.item_id
set d.role_type=#{roleType}, d.prorated_amount=#{amount},
    d.proration_basis=#{basis}, d.update_time=now()
where d.item_id=#{itemId} and ifnull(i.status,0)=0
  and not exists (
    select 1 from biz_workload_summary s
    where s.user_id=i.user_id and s.semester=i.semester and ifnull(s.status,0)&lt;&gt;0
  )
```

方法签名：

```java
int updateProrationIfEditable(@Param("itemId") Long itemId,
 @Param("roleType") String roleType, @Param("amount") BigDecimal amount,
 @Param("basis") String basis);
```

影响行数 0 时抛状态冲突并回滚；新建 item 的 `sourceType` 改为 `AUTO`。

- [ ] **Step 4: 运行 GREEN**

Run: 与 Step 2 相同。
Expected: PASS；冻结、已核对时 G11 主表/子表均不变；草稿重复生成保持同一 item。

- [ ] **Step 5: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/calc/ManagementItemGeneratorImpl.java rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadItemMapper.java rear/workload-system/src/main/java/com/workload/system/mapper/BizWlManagementMapper.java rear/workload-system/src/main/resources/mapper/system/BizWorkloadItemMapper.xml rear/workload-system/src/main/resources/mapper/system/BizWlManagementMapper.xml rear/workload-system/src/test/java/com/workload/system/calc/ManagementItemGeneratorImplTest.java
git commit -m "fix: 冻结并原子更新G11生成快照"
```

### Task 5: 补齐 Summary / Pay / Allowance 条件写（需求 4、13，安全门）

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadSummaryMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadSummaryMapper.xml`
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizPayRecordMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizPayRecordMapper.xml`
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizAllowanceItemMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizAllowanceItemMapper.xml`
- Modify: `rear/workload-system/src/main/java/com/workload/system/calc/SummaryCalcServiceImpl.java:154-181`
- Modify: `rear/workload-system/src/main/java/com/workload/system/calc/PayCalcServiceImpl.java:75-114`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/impl/BizAllowanceItemServiceImpl.java:66-135`
- Test: corresponding three existing test classes

- [x] **Step 1: 写三组失败测试**

```java
when(summaryMapper.updateCalculatedFieldsIfStatus(any(), eq(0))).thenReturn(0);
assertThatThrownBy(() -> summaryService.recalcSummary(USER, SEMESTER, true))
    .hasMessageContaining("状态已变化");

when(payMapper.updateIfSummaryDraft(any())).thenReturn(0);
assertThatThrownBy(() -> payService.recalcPay(USER, SEMESTER))
    .hasMessageContaining("酬金未保存");

when(allowanceMapper.updateIfSummaryDraft(any())).thenReturn(0);
assertThatThrownBy(() -> allowanceService.updateBizAllowanceItem(change))
    .hasMessageContaining("状态已变化");
```

另写删除测试，断言 `deleteByIdIfSummaryDraft(id)==0` 不得返回成功。

- [x] **Step 2: 运行 RED**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=SummaryCalcServiceImplTest,PayCalcServiceImplTest,BizAllowanceItemServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL 或断言 FAIL；三处当前均为通用 `WHERE id=?` 写入。

- [x] **Step 3: 实现 Summary 草稿条件写**

签名保持与现有计划一致：

```java
int updateCalculatedFieldsIfStatus(@Param("summary") BizWorkloadSummary summary,
        @Param("expectedStatus") Integer expectedStatus);
```

SQL 只更新 `academic_year,G7,G8,G9,G10,G11,total_workload,rated_workload,excess_workload,title,pay_rate,performance_pay,is_capped,basic_teaching_standard,basic_teaching_met,remark,update_time`，结尾严格为：

```sql
where id=#{summary.id} and status=#{expectedStatus}
```

不得更新 `status`、签字、`lock_time`、`create_time`。DuplicateKey 分支重新查询后必须校验 `status=0` 再调用此方法，影响行数不是 1 抛冲突。

- [x] **Step 4: 实现 Pay / Allowance 条件写**

`BizPayRecordMapper`：

```java
int updateIfSummaryDraft(@Param("record") BizPayRecord record);
```

SQL 使用 `where id=#{record.id} and exists(select 1 from biz_workload_summary s where s.id=#{record.summaryId} and s.status=0)`。新建 pay record 前调用 Guard；DuplicateKey 降级更新同样走该条件 SQL。

`BizAllowanceItemMapper` 增加 `updateIfSummaryDraft(BizAllowanceItem item)`、`deleteByIdIfSummaryDraft(Long id)`；条件为不存在汇总或汇总 `status=0`。新增 allowance 在事务开头调用 Guard，更新/删除除前置校验外必须检查条件 SQL 影响行数。

- [x] **Step 5: 运行 GREEN 和完整安全门测试**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=WorkloadWriteGuardTest,TeachingTaskImportServiceImplTest,BizTeachingTaskServiceImplTest,WorkloadCalcServiceImplTest,ManagementItemGeneratorImplTest,SummaryCalcServiceImplTest,PayCalcServiceImplTest,BizAllowanceItemServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS；任何并发审批均无法晚写覆盖。

- [x] **Step 6: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/mapper rear/workload-system/src/main/resources/mapper/system rear/workload-system/src/main/java/com/workload/system/calc/SummaryCalcServiceImpl.java rear/workload-system/src/main/java/com/workload/system/calc/PayCalcServiceImpl.java rear/workload-system/src/main/java/com/workload/system/service/impl/BizAllowanceItemServiceImpl.java rear/workload-system/src/test/java/com/workload/system/calc rear/workload-system/src/test/java/com/workload/system/service/impl/BizAllowanceItemServiceImplTest.java
git commit -m "fix: 条件保护汇总酬金与其他酬金写入"
```

### Task 6: 建立不可变计算快照与迁移（需求 6、10、12）

**Files:**
- Create: `rear/sql/22_calculation_consistency.sql`
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/BizWorkloadCalcSnapshot.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadCalcSnapshotMapper.java`
- Create: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadCalcSnapshotMapper.xml`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/WorkloadSnapshotService.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/service/WorkloadSnapshotServiceTest.java`

- [ ] **Step 1: 写失败测试：快照规范化与哈希稳定**

```java
@Test void sameOrderedFactorsProduceStableHash() {
    CalculationSnapshot first = service.capture(item, formula, "RULE-2025-1");
    CalculationSnapshot second = service.capture(item, formula, "RULE-2025-1");
    assertThat(second.getSnapshotHash()).isEqualTo(first.getSnapshotHash());
    assertThat(first.getFormulaExpression()).isEqualTo("J1 × C1 × K1 × Q1 × Q2 × N");
}
```

- [ ] **Step 2: 运行 RED**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=WorkloadSnapshotServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL，新实体/服务不存在。

- [ ] **Step 3: 编写幂等 DDL**

`biz_workload_calc_snapshot` 必须包含：`id,item_id,calculation_version,formula_expression,factor_json,source_json,rule_version,result,snapshot_hash,created_by,created_at`；唯一键 `(item_id,calculation_version)`，索引 `snapshot_hash`。`factor_json/source_json` 使用 JSON，值按固定 key 顺序序列化，金额/系数用字符串避免浮点变化。

同脚本增加 `biz_workload_item.calculation_version BIGINT NOT NULL DEFAULT 0`、`last_calculated_at DATETIME NULL`，并执行：

```sql
update biz_workload_item
set source_type='AUTO'
where item_type='G11' and assignment_id is not null and source_type='IMPORT';
```

- [ ] **Step 4: 实现快照写入**

```java
@Transactional(propagation = Propagation.MANDATORY)
public BizWorkloadCalcSnapshot capture(BizWorkloadItem item,
        FactorFormulaVo formula, String ruleVersion)
```

版本取 `item.calculationVersion + 1`；保存成功后由 Task 7 的条件 UPDATE 同步写 `calculation_version/last_calculated_at`。快照只 INSERT，不 UPDATE/DELETE。

- [ ] **Step 5: 运行 GREEN 与 SQL 静态检查**

Run: 与 Step 2 相同。
Expected: PASS，JSON key 顺序和 SHA-256 哈希稳定。

Run: `git diff --check -- rear/sql/22_calculation_consistency.sql`
Expected: exit 0，脚本不含无条件 DROP/DELETE。

- [ ] **Step 6: 提交**

```bash
git add rear/sql/22_calculation_consistency.sql rear/workload-system/src/main/java/com/workload/system/domain/BizWorkloadCalcSnapshot.java rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadCalcSnapshotMapper.java rear/workload-system/src/main/resources/mapper/system/BizWorkloadCalcSnapshotMapper.xml rear/workload-system/src/main/java/com/workload/system/service/WorkloadSnapshotService.java rear/workload-system/src/test/java/com/workload/system/service/WorkloadSnapshotServiceTest.java
git commit -m "feat: 保存不可变工作量计算快照"
```

### Task 7: 让 G1-G6 与 G11 同步落快照（需求 10、12）

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/calc/WorkloadCalcServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/impl/TeachingTaskImportServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/calc/ManagementItemGeneratorImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/impl/WorkloadFactorFormulaServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/domain/vo/FactorFormulaVo.java`
- Test: `WorkloadCalcServiceImplTest`, `TeachingTaskImportServiceImplTest`, `ManagementItemGeneratorImplTest`, `WorkloadFactorFormulaBuilderTest`

- [ ] **Step 1: 写失败测试：每次成功重算恰好一份快照**

```java
@Test void successfulRecalcPersistsSnapshotBeforeConditionalUpdate() {
    service.recalcItem(ITEM_ID);
    InOrder order = inOrder(snapshotService, itemMapper);
    order.verify(snapshotService).capture(eq(item), any(), anyString());
    order.verify(itemMapper).updateCalculationIfEditable(any(), eq(0));
}
```

另断言条件 UPDATE=0 时整个事务回滚快照；G11 `FactorFormulaVo` 不再伪造岗位计算公式，只包含 `assignmentId,positionWorkload,sourceBatchId,capBefore,capApplied`，并标记来源为 `IMPORTED_APPROVED_VALUE`。

- [ ] **Step 2: 运行 RED**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=WorkloadCalcServiceImplTest,TeachingTaskImportServiceImplTest,ManagementItemGeneratorImplTest,WorkloadFactorFormulaBuilderTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL，当前 G11 builder 返回 null，重算无 snapshot 调用。

- [ ] **Step 3: 扩展公式 VO 和 G11 builder**

`FactorVo` 增加 `ruleCode`、`ruleVersion`、`sourceRef`；来源枚举固定为 `IMPORT_VALUE,RULE_DEFAULT,APPROVED_OVERRIDE,FORMULA_CONSTANT,DERIVED,DISPLAY_ONLY`。禁止详情服务现场用当前规则伪装历史来源；旧数据无快照时返回 `legacy=true,reproducible=false`。

- [ ] **Step 4: 接入三个写入口**

导入、`recalcItem`、G11 生成均按“计算 → build formula → INSERT snapshot → 原子 UPDATE 主表版本/结果”顺序处于同一事务。`updateCalculationIfEditable` 增加：

```sql
calculation_version=#{item.calculationVersion},
last_calculated_at=#{item.lastCalculatedAt}
```

G11 来源为导入的学期岗位减免值，成功同步时固化来源批次和原始值；教师端只展示业务解释。当前环境无真实数据，不保留 SELF 新建兼容分支。

- [ ] **Step 5: 运行 GREEN**

Run: 与 Step 2 相同。
Expected: PASS；失败写不留下孤立快照，成功重算版本单调递增。

- [ ] **Step 6: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/calc rear/workload-system/src/main/java/com/workload/system/service/impl/TeachingTaskImportServiceImpl.java rear/workload-system/src/main/java/com/workload/system/service/impl/WorkloadFactorFormulaServiceImpl.java rear/workload-system/src/main/java/com/workload/system/domain/vo/FactorFormulaVo.java rear/workload-system/src/test/java/com/workload/system
git commit -m "feat: 核算时固化因子与G11快照"
```

### Task 8: 建立 G1/G2 系数申请模型（需求 6）

**Files:**
- Create: `rear/sql/23_coefficient_adjustment.sql`
- Create: domain/mapper/XML/service files listed in File Structure
- Test: `rear/workload-system/src/test/java/com/workload/system/service/impl/CoefficientAdjustmentServiceImplTest.java`

- [ ] **Step 1: 写失败测试：只允许白名单因子且冻结后拒绝**

```java
@ParameterizedTest
@CsvSource({"G1,C1","G1,K1","G1,Q1","G1,Q2","G1,N","G2,K","G2,C2","G2,Q1","G2,Q2"})
void allowedFactorCanBeSubmitted(String category, String factorCode) { /* status DRAFT */ }

@Test void submittedSummaryRejectsApplication() {
    assertThatThrownBy(() -> service.submit(request)).hasMessageContaining("审批流程");
}
```

同时测试 `requestedValue>0`、理由非空、同 item+factor 只能有一个 `PENDING`。

- [ ] **Step 2: 运行 RED**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=CoefficientAdjustmentServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL。

- [ ] **Step 3: 创建表和状态机**

`biz_coefficient_adjustment` 字段固定为：`id,user_id,semester,task_id,item_id,category,factor_code,old_value,requested_value,reason,attachment_url,status(0 PENDING/1 APPROVED/2 REJECTED/3 CANCELLED),reviewer_id,review_reason,reviewed_at,base_calculation_version,created_by,created_at,updated_by,updated_at`。用生成列或普通唯一键保证一条待审：`pending_key = IF(status=0, CONCAT(item_id,':',factor_code), NULL)` + UNIQUE。

- [ ] **Step 4: 实现服务签名**

```java
Long submit(CoefficientAdjustmentRequest request);
void approve(Long id, String reviewReason);
void reject(Long id, String reviewReason);
```

submit 从当前快照读取 oldValue，不接受客户端 oldValue；调用 Guard。approve/reject 使用 `UPDATE ... WHERE id=? AND status=0 AND base_calculation_version=?`，影响行数 0 抛冲突；批准不直接改 G 子表，Task 9 负责原子应用和重算。

- [ ] **Step 5: 运行 GREEN**

Run: 与 Step 2 相同。
Expected: PASS，重复审批、过期版本、非本人申请均被拒绝。

- [ ] **Step 6: 提交**

```bash
git add rear/sql/23_coefficient_adjustment.sql rear/workload-system/src/main/java/com/workload/system/domain/BizCoefficientAdjustment.java rear/workload-system/src/main/java/com/workload/system/mapper/BizCoefficientAdjustmentMapper.java rear/workload-system/src/main/resources/mapper/system/BizCoefficientAdjustmentMapper.xml rear/workload-system/src/main/java/com/workload/system/service/ICoefficientAdjustmentService.java rear/workload-system/src/main/java/com/workload/system/service/impl/CoefficientAdjustmentServiceImpl.java rear/workload-system/src/test/java/com/workload/system/service/impl/CoefficientAdjustmentServiceImplTest.java
git commit -m "feat: 新增G1G2系数调整申请"
```

### Task 9: 审批后原子应用系数并重算（需求 6）

**Files:**
- Modify: `CoefficientAdjustmentServiceImpl.java`
- Modify: G1/G2 Mapper interfaces/XML
- Modify: `WorkloadCalcService.java` and `WorkloadCalcServiceImpl.java`
- Create: `BizCoefficientAdjustmentController.java`
- Test: `CoefficientAdjustmentServiceImplTest.java`

- [ ] **Step 1: 写失败测试：批准操作原子更新指定因子**

```java
@Test void approveAppliesOnlyRequestedFactorThenRecalculates() {
    service.approve(ADJUSTMENT_ID, "材料有效");
    verify(theoryMapper).updateFactorIfVersion(ITEM_ID, "Q2", new BigDecimal("1.50"), 3L);
    verify(workloadCalcService).recalcItem(ITEM_ID);
    verify(adjustmentMapper).markApprovedIfPending(ADJUSTMENT_ID, REVIEWER, "材料有效", 3L);
}
```

- [ ] **Step 2: 运行 RED**

Run: 与 Task 8 Step 2 相同。
Expected: FAIL，批准尚未应用因子。

- [ ] **Step 3: 实现白名单分发和条件 SQL**

禁止 `${factorCode}` 拼列名。Java switch 映射到明确方法，例如：

```java
case "Q2" -> theoryMapper.updateQ2IfVersion(itemId, value, baseVersion);
case "K" -> practiceMapper.updateKIfVersion(itemId, value, baseVersion);
```

每条 SQL JOIN `biz_workload_item i` 并要求 `i.calculation_version=#{baseVersion}`、`ifnull(i.status,0)=0`、不存在非草稿汇总。更新成功后调用 `recalcItem` 产生 `APPROVED_OVERRIDE` 新快照；最后条件更新申请为 APPROVED。任一步失败事务整体回滚。

- [ ] **Step 4: 实现 API**

Controller 路径：`POST /system/coefficientAdjustment`、`GET /myList`、`GET /list`、`POST /{id}/approve`、`POST /{id}/reject`；权限严格使用实施方案定义的 add/list/approve/reject。教师入口使用 `DataScopeUtil.resolveUserId`，审核人从 SecurityContext 取。

- [ ] **Step 5: 运行 GREEN**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=CoefficientAdjustmentServiceImplTest,WorkloadCalcServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS；只改指定因子，结果、版本、快照和申请状态同事务一致。

- [ ] **Step 6: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/controller/BizCoefficientAdjustmentController.java rear/workload-system/src/main/java/com/workload/system/service/impl/CoefficientAdjustmentServiceImpl.java rear/workload-system/src/main/java/com/workload/system/calc/WorkloadCalcService.java rear/workload-system/src/main/java/com/workload/system/calc/WorkloadCalcServiceImpl.java rear/workload-system/src/main/java/com/workload/system/mapper rear/workload-system/src/main/resources/mapper/system rear/workload-system/src/test/java/com/workload/system/service/impl/CoefficientAdjustmentServiceImplTest.java
git commit -m "feat: 审批并应用G1G2系数调整"
```

### Task 10: G1/G2/G3 分类模板与菜单（需求 11）

**Files:**
- Modify: `TeachingTaskImportDTO.java`, `ITeachingTaskImportService.java`, `TeachingTaskImportServiceImpl.java`, `BizTeachingTaskController.java`
- Modify: `front/RuoYi-Vue3/src/api/system/teachingTask.js`
- Modify: `front/RuoYi-Vue3/src/views/system/teachingTask/index.vue`
- Create: `rear/sql/24_calculation_menu.sql`
- Test: `TeachingTaskImportServiceImplTest.java`, `BizTeachingTaskControllerTest.java`

- [ ] **Step 1: 写失败测试：模板类型锁定类别**

```java
@ParameterizedTest
@CsvSource({"G1,G1", "G2,G2", "G3,G3"})
void typedImportAcceptsMatchingCategory(String templateType, String rowType) { /* succeeds */ }

@Test void g1TemplateRejectsG2RowBeforeInsert() {
    assertThatThrownBy(() -> service.processSingleRow(g2Dto(), "IMP", "G1"))
        .hasMessageContaining("模板类别 G1");
}
```

- [ ] **Step 2: 运行 RED**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=TeachingTaskImportServiceImplTest,BizTeachingTaskControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL，新签名/参数不存在。

- [ ] **Step 3: 实现共享分类导入**

接口变为 `importTeachingTasksStreaming(InputStream inputStream, String fileName, String templateType)`；`templateType` 仅允许 `ALL/G1/G2/G3`。Controller 的 `POST /importExcel?templateType=G1` 和 `POST /importTemplate?templateType=G1` 保持原路径兼容，省略为 ALL。服务端在任何写库前比较行类别；三类模板用 EasyExcel includeColumnFieldNames 生成精简列，但 G1/G2/G3 都保留教师、学期、课程、层次、专业、性质、级别、角色、评价、人数、班级、重复次序及各自数量/系数列。

- [ ] **Step 4: 前端增加三个快捷入口**

`teachingTask/index.vue` 导入弹窗增加类型卡片“通用/G1 理论/G2 实践/G3 实习实训”，下载和上传都传同一 `templateType`；表格默认显示中文教师/课程字段，内部 ID 仍不新增列表列。SQL 新增三个 C 型菜单路由到同一组件并携带 query `templateType`，按钮权限仍复用 `system:teachingTask:import`，脚本先按 perms/path 清旧值再幂等 INSERT。

- [ ] **Step 5: 运行 GREEN 与前端构建**

Run: 与 Step 2 相同。Expected: PASS。
Run: `npm --prefix front/RuoYi-Vue3 run build:prod`
Expected: `✓ built`，无 Vue 编译错误。

- [ ] **Step 6: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/domain/dto/TeachingTaskImportDTO.java rear/workload-system/src/main/java/com/workload/system/service rear/workload-system/src/main/java/com/workload/system/controller/BizTeachingTaskController.java rear/workload-system/src/test front/RuoYi-Vue3/src/api/system/teachingTask.js front/RuoYi-Vue3/src/views/system/teachingTask/index.vue rear/sql/24_calculation_menu.sql
git commit -m "feat: 提供G1G2G3分类导入入口"
```

### Task 11: 明细因子详情与系数申请界面（需求 6、12）

**Files:**
- Modify: `WorkloadFactorFormulaServiceImpl.java`, `FactorFormulaVo.java`, `BizWorkloadItemController.java`
- Modify: `front/RuoYi-Vue3/src/components/FactorFormula/index.vue`
- Modify: `front/RuoYi-Vue3/src/views/system/workloadItem/index.vue`
- Create: `front/RuoYi-Vue3/src/api/system/coefficientAdjustment.js`
- Create: `front/RuoYi-Vue3/src/views/system/coefficientAdjustment/index.vue`
- Test: `WorkloadFactorFormulaBuilderTest.java`, `BizWorkloadItemControllerTest.java`

- [ ] **Step 1: 写失败测试：详情读取最新快照而非当前规则**

```java
@Test void detailUsesPersistedSnapshotSources() {
    when(snapshotMapper.selectLatestByItemId(7L)).thenReturn(snapshotWithQ2("1.50", "APPROVED_OVERRIDE"));
    FactorFormulaVo vo = service.build(item);
    assertThat(vo.getFactors()).filteredOn(f -> f.getKey().equals("Q2"))
        .extracting(FactorVo::getSource).containsExactly("APPROVED_OVERRIDE");
}
```

并覆盖 `courseLevel/courseRole/courseNature/teachingEval/className/repeatOrder` 源字段；G11 详情只返回本学期岗位减免原始值、计入值、来源批次和封顶说明。

- [ ] **Step 2: 运行 RED**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=WorkloadFactorFormulaBuilderTest,BizWorkloadItemControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL；现有详情 source 多为 UNKNOWN，课程源字段未返回。

- [ ] **Step 3: 后端返回结构化详情**

保持 `GET /system/workloadItem/{id}`，在 `factorFormula` 中增加 `snapshotVersion/snapshotHash/calculatedAt/legacy`；增加 `sourceTask` 嵌套 VO，字段为上述六项，不把字段复制回主表。若快照存在只从快照解释；无快照才读取子表并明确 `legacy=true,reproducible=false`，不得吞掉快照解析错误后伪装为可复现。

- [ ] **Step 4: 前端呈现因子来源与申请动作**

Factor chip 显示“默认规则/导入值/审批调整/派生值/仅展示”；G1/G2 允许申请的 factor 显示“申请调整”，打开表单仅填写 `requestedValue,reason,attachmentUrl`。内部 `itemId/taskId/assignmentId/snapshotHash` 移入默认折叠的“数据溯源”，课程信息分区显示课程级别/角色及 `Q2` 原因。审核页并排显示旧值、新值、影响前后工作量。

- [ ] **Step 5: 运行 GREEN 与 ESLint**

Run: 与 Step 2 相同。Expected: PASS。
Run: `front/RuoYi-Vue3/node_modules/.bin/eslint.cmd front/RuoYi-Vue3/src/components/FactorFormula/index.vue front/RuoYi-Vue3/src/views/system/workloadItem/index.vue front/RuoYi-Vue3/src/views/system/coefficientAdjustment/index.vue`
Expected: exit 0。

- [ ] **Step 6: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/service/impl/WorkloadFactorFormulaServiceImpl.java rear/workload-system/src/main/java/com/workload/system/domain/vo rear/workload-system/src/main/java/com/workload/system/controller/BizWorkloadItemController.java rear/workload-system/src/test front/RuoYi-Vue3/src/components/FactorFormula/index.vue front/RuoYi-Vue3/src/views/system/workloadItem/index.vue front/RuoYi-Vue3/src/api/system/coefficientAdjustment.js front/RuoYi-Vue3/src/views/system/coefficientAdjustment/index.vue
git commit -m "feat: 展示可追溯因子与系数申请"
```

### Task 12: 阶段化一键核算流程（需求 13）

**Files:**
- Create: `CalculationRunRequest.java`, `CalculationRunResult.java`
- Modify: `WorkloadCalcService.java`, `WorkloadCalcServiceImpl.java`, `BizCalcController.java`
- Modify: `front/RuoYi-Vue3/src/api/system/calc.js`
- Modify: `front/RuoYi-Vue3/src/views/system/workloadSummary/index.vue`
- Test: `WorkloadCalcServiceImplTest.java`, `BizCalcControllerTest.java`

- [ ] **Step 1: 写失败测试：显式阶段、前置条件和结果计数**

```java
@Test void fullRunReportsEveryStage() {
    CalculationRunResult result = service.run(new CalculationRunRequest(USER, SEMESTER, true));
    assertThat(result.getStages()).extracting(StageResult::getCode)
        .containsExactly("VALIDATE", "GENERATE_G11", "RECALC_ITEMS", "RECALC_SUMMARY", "RECALC_PAY");
    assertThat(result.getGeneratedG11Count()).isEqualTo(2);
    assertThat(result.getRecalcItemCount()).isEqualTo(7);
}
```

另测 `includeG11=false` 不调用生成器、存在冻结/缺子表/待审系数申请时 VALIDATE 失败且零写入、批量结果只累计成功教师明细数。

- [ ] **Step 2: 运行 RED**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=WorkloadCalcServiceImplTest,BizCalcControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL，新契约不存在。

- [ ] **Step 3: 实现服务编排**

新增：

```java
CalculationRunResult run(CalculationRunRequest request);
Map<String,Object> runBatch(List<Long> userIds, String semester, boolean includeG11);
```

`run` 单教师单事务，首先 Guard + 一致性校验（教师档案、子表齐全、无 PENDING 系数申请、无 item status=1 要被改写），然后按请求显式执行 G11、明细、汇总、酬金。保留原 `recalcAll` 作为 `includeG11=false` 兼容包装；批量每教师独立代理事务，聚合 `generatedG11Count/recalcItemCount/successCount/failures(stage,reason)`。

- [ ] **Step 4: 实现 API 与前端向导**

新增 `POST /system/calc/run` body `{userId,semester,includeG11}` 与 `/runBatch?semester=` body `{userIds,includeG11}`；现有端点保持兼容。汇总页按钮改为“开始核算”，确认框明确勾选“同步教务岗位减免到 G11”，结果按五阶段展示；独立按钮文案改为“同步岗位减免”。提交按钮只有最近一次 run 成功、`unconfirmedCount=0`、无 PENDING 调整时可用，后端同样重查前置条件。

- [ ] **Step 5: 运行 GREEN 与前端验证**

Run: 与 Step 2 相同。Expected: PASS。
Run: `npm --prefix front/RuoYi-Vue3 run build:prod`
Expected: `✓ built`；核算结果不再固定显示 0 条。

- [ ] **Step 6: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/domain/dto/CalculationRunRequest.java rear/workload-system/src/main/java/com/workload/system/domain/vo/CalculationRunResult.java rear/workload-system/src/main/java/com/workload/system/calc rear/workload-system/src/main/java/com/workload/system/controller/BizCalcController.java rear/workload-system/src/main/java/com/workload/system/service/impl/BizAuditServiceImpl.java rear/workload-system/src/test front/RuoYi-Vue3/src/api/system/calc.js front/RuoYi-Vue3/src/views/system/workloadSummary/index.vue
git commit -m "feat: 编排可观测的一键核算流程"
```

<!-- APPEND -->
