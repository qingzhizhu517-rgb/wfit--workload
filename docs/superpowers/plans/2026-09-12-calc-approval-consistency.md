# 核算与审批一致性优化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让工作量明细、汇总和酬金在待审/已完结阶段保持冻结，并通过条件更新消除审批与重算并发时覆盖终态的风险。

**Architecture:** Java 生产代码统一使用 `WorkloadSummaryStatus`；领域 service 执行冻结校验；汇总持久化改用带预期状态的专用 Mapper 更新且不回写审批字段；批量核算保持单教师事务并聚合明细计数。

**Tech Stack:** Java 17、Spring Boot、MyBatis、JUnit 5、Mockito、AssertJ、Vue 3、Axios、ESLint、Vite。

---

## 文件职责

- `rear/workload-system/src/main/java/com/workload/system/domain/WorkloadSummaryStatus.java`：Java 侧审批状态唯一来源。
- `rear/workload-system/src/main/java/com/workload/system/calc/WorkloadCalcServiceImpl.java`：明细冻结与单人/批量核算编排。
- `rear/workload-system/src/main/java/com/workload/system/calc/SummaryCalcServiceImpl.java`：汇总状态校验、计算和条件持久化。
- `rear/workload-system/src/main/java/com/workload/system/calc/PayCalcServiceImpl.java`：酬金写入前状态复核。
- `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadSummaryMapper.java`：声明草稿态条件更新。
- `rear/workload-system/src/main/resources/mapper/system/BizWorkloadSummaryMapper.xml`：实现不覆盖审批字段的条件 UPDATE。
- `front/RuoYi-Vue3/src/views/system/workloadSummary/index.vue`：展示批量实际重算明细数。
- 对应 `src/test`：按三态矩阵与并发冲突编写回归测试。

### Task 1: 收口状态定义

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/calc/WorkloadCalcServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/impl/BizAuditServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/controller/BizWorkloadSummaryController.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/domain/WorkloadSummaryStatusTest.java`

- [ ] **Step 1: 写状态契约测试**

```java
@Test
void exposesOnlyCurrentApprovalStates()
{
    assertThat(WorkloadSummaryStatus.DRAFT).isZero();
    assertThat(WorkloadSummaryStatus.PENDING_AUDIT).isEqualTo(1);
    assertThat(WorkloadSummaryStatus.FINISHED).isEqualTo(2);
}
```

- [ ] **Step 2: 运行测试并确认当前状态契约**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=WorkloadSummaryStatusTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS；若测试不存在则先 testCompile 失败，证明测试尚未落地。

- [ ] **Step 3: 替换生产代码中的汇总状态硬编码**

将 `STATUS_DRAFT`、`STATUS_PENDING_AUDIT`、`STATUS_FINISHED` 和 `SUMMARY_STATUS_LOCKED` 替换为 `WorkloadSummaryStatus.*`；明细自身的 0/1/2/3 状态不替换。

- [ ] **Step 4: 运行状态及审批测试**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=WorkloadSummaryStatusTest,BizAuditServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS，0 failures。

- [ ] **Step 5: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/domain/WorkloadSummaryStatus.java rear/workload-system/src/main/java/com/workload/system/calc/WorkloadCalcServiceImpl.java rear/workload-system/src/main/java/com/workload/system/service/impl/BizAuditServiceImpl.java rear/workload-system/src/main/java/com/workload/system/controller/BizWorkloadSummaryController.java rear/workload-system/src/test/java/com/workload/system/domain/WorkloadSummaryStatusTest.java
git commit -m "refactor: 收口工作量汇总状态定义"
```

### Task 2: 冻结待审阶段的明细与持久化核算

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/calc/WorkloadCalcServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/calc/SummaryCalcServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/calc/PayCalcServiceImpl.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/calc/WorkloadCalcServiceImplTest.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/calc/SummaryCalcServiceImplTest.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/calc/PayCalcServiceImplTest.java`

- [ ] **Step 1: 写失败测试：待审禁止修改与持久化重算**

```java
assertThatThrownBy(() -> service.recalcSummary(USER, SEMESTER, true))
    .isInstanceOf(ServiceException.class)
    .hasMessageContaining("待审");
```

为 `WorkloadCalcServiceImpl.assertEditable` 和 `PayCalcServiceImpl.recalcPay` 各增加 status=1 同类断言；另断言 `recalcSummary(..., false)` 允许生成预览。

- [ ] **Step 2: 运行定向测试验证失败**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=WorkloadCalcServiceImplTest,SummaryCalcServiceImplTest,PayCalcServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: status=1 的写操作测试 FAIL，预览测试 PASS。

- [ ] **Step 3: 实现统一冻结判断**

```java
private boolean isWriteFrozen(Integer status)
{
    return status != null && status != WorkloadSummaryStatus.DRAFT;
}
```

`persist=true`、明细写入和酬金重算使用该判断；`persist=false` 不因审批状态拒绝。

- [ ] **Step 4: 运行定向测试**

Run: 与 Step 2 相同。
Expected: PASS，待审和终态写操作都被拒绝，预览仍可用。

- [ ] **Step 5: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/calc rear/workload-system/src/test/java/com/workload/system/calc
git commit -m "fix: 冻结审批中的工作量核算数据"
```

### Task 3: 汇总计算使用草稿态条件更新

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadSummaryMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadSummaryMapper.xml`
- Modify: `rear/workload-system/src/main/java/com/workload/system/calc/SummaryCalcServiceImpl.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/calc/SummaryCalcServiceImplTest.java`

- [ ] **Step 1: 写失败测试：并发状态变化不得静默成功**

```java
when(summaryMapper.updateCalculatedFieldsIfStatus(any(), eq(WorkloadSummaryStatus.DRAFT)))
    .thenReturn(0);
assertThatThrownBy(() -> service.recalcSummary(USER, SEMESTER, true))
    .isInstanceOf(ServiceException.class)
    .hasMessageContaining("状态已变化");
```

- [ ] **Step 2: 运行测试验证 Mapper 方法尚不存在或行为失败**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=SummaryCalcServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL（方法未声明）或断言 FAIL。

- [ ] **Step 3: 声明并实现条件更新**

Mapper 方法：

```java
int updateCalculatedFieldsIfStatus(@Param("summary") BizWorkloadSummary summary,
        @Param("expectedStatus") Integer expectedStatus);
```

SQL 只更新 `academic_year`、G7-G11、总量、额定、超额、职称、费率、绩效、封顶、达标、remark、update_time；结尾使用：

```sql
where id = #{summary.id} and status = #{expectedStatus}
```

不得更新 `status`、任何签字字段或 `lock_time`。

- [ ] **Step 4: 服务检查影响行数**

```java
int rows = mapper.updateCalculatedFieldsIfStatus(summary, WorkloadSummaryStatus.DRAFT);
if (rows != 1) {
    throw new ServiceException("汇总状态已变化，请刷新后重试", HttpStatus.CONFLICT);
}
```

- [ ] **Step 5: 运行汇总测试与完整后端测试**

Run: `mvn -f rear/pom.xml test -pl workload-system -am`
Expected: BUILD SUCCESS，0 failures。

- [ ] **Step 6: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadSummaryMapper.java rear/workload-system/src/main/resources/mapper/system/BizWorkloadSummaryMapper.xml rear/workload-system/src/main/java/com/workload/system/calc/SummaryCalcServiceImpl.java rear/workload-system/src/test/java/com/workload/system/calc/SummaryCalcServiceImplTest.java
git commit -m "fix: 原子保护汇总重算写入"
```

### Task 4: 加固酬金写入状态复核

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/calc/PayCalcServiceImpl.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/calc/PayCalcServiceImplTest.java`

- [ ] **Step 1: 写失败测试：保存前状态变化时拒绝写 pay record**

模拟首次查询草稿、保存前查询已完结，并断言 `BizPayRecordMapper.insert/update` 均未调用。

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=PayCalcServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL，当前实现仍调用 pay record mapper。

- [ ] **Step 3: 在写入前重新读取并校验草稿态**

```java
BizWorkloadSummary latest = findSummary(userId, semester);
if (latest == null || latest.getStatus() == null
        || latest.getStatus() != WorkloadSummaryStatus.DRAFT) {
    throw new ServiceException("汇总状态已变化，酬金未保存，请刷新后重试");
}
```

并以 `latest.getId()` 作为 `summaryId`，避免使用过期对象。

- [ ] **Step 4: 运行测试**

Run: 与 Step 2 相同。
Expected: PASS，0 failures。

- [ ] **Step 5: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/calc/PayCalcServiceImpl.java rear/workload-system/src/test/java/com/workload/system/calc/PayCalcServiceImplTest.java
git commit -m "fix: 防止审批并发改写定稿酬金"
```

### Task 5: 返回并展示批量明细计数

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/calc/WorkloadCalcServiceImpl.java`
- Modify: `front/RuoYi-Vue3/src/views/system/workloadSummary/index.vue`
- Test: `rear/workload-system/src/test/java/com/workload/system/calc/WorkloadCalcServiceImplTest.java`

- [ ] **Step 1: 写失败测试：批量结果聚合成功教师的明细计数**

```java
assertThat(result.get("recalcItemCount")).isEqualTo(7);
```

模拟两个教师 `recalcAll` 分别返回 3 和 4，第三位失败；失败教师不计入。

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=WorkloadCalcServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL，字段为 null。

- [ ] **Step 3: 聚合 `recalcItemCount`**

在成功分支读取 `proxy.recalcAll` 返回值：

```java
Map<String, Object> teacherResult = proxy.recalcAll(uid, semester);
recalcItemCount += ((Number) teacherResult.getOrDefault("recalcItemCount", 0)).intValue();
```

最终 `data.put("recalcItemCount", recalcItemCount)`。

- [ ] **Step 4: 运行后端测试和前端验证**

Run: `mvn -f rear/pom.xml test -pl workload-system -am`
Expected: BUILD SUCCESS。

Run: `npm --prefix front/RuoYi-Vue3 run build:prod`
Expected: `✓ built`。

- [ ] **Step 5: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/calc/WorkloadCalcServiceImpl.java rear/workload-system/src/test/java/com/workload/system/calc/WorkloadCalcServiceImplTest.java front/RuoYi-Vue3/src/views/system/workloadSummary/index.vue
git commit -m "fix: 返回批量核算实际明细数"
```

### Task 6: 最终验证与复审

**Files:**
- Review: all files changed by Tasks 1-5

- [ ] **Step 1: 完整后端测试**

Run: `mvn -f rear/pom.xml test -pl workload-system -am`
Expected: BUILD SUCCESS，0 failures/errors。

- [ ] **Step 2: 变更前端定向 ESLint**

Run: `front/RuoYi-Vue3/node_modules/.bin/eslint.cmd front/RuoYi-Vue3/src/views/system/workloadSummary/index.vue`
Expected: exit 0。

- [ ] **Step 3: 前端生产构建**

Run: `npm --prefix front/RuoYi-Vue3 run build:prod`
Expected: `✓ built`；允许现存 `%VITE_APP_TITLE%` 环境提示，但不允许编译错误。

- [ ] **Step 4: Diff 与状态检查**

Run: `git diff --check && git status --short`
Expected: 无空白错误；仅包含计划内改动。

- [ ] **Step 5: 代码复审**

运行 `/code-review high`，仅保留可复现的 correctness findings；如有发现，回到对应 Task 先写失败测试再修复。

- [ ] **Step 6: 阶段提交（仅有未提交修正时）**

```bash
git add <复审修正文件>
git commit -m "fix: 完善核算审批一致性"
```
