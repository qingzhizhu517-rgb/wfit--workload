# 数据契约与导入实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成需求 1、2、3、8、16，以可预检、可确认、可追溯的导入框架支持教学任务和学期岗位减免，提供教师档案字段预检与清晰前端入口，并准备当前空环境的 fresh DB 整体重建。

**Architecture:** 所有导入统一采用“模板版本识别 → 解析到 `biz_import_batch`/`biz_import_row` 暂存 → 阻断性预检 → 显式确认 → 逐行事务写入”的两阶段模型。教学任务 v2 将计划学时、天数、周数、课程设计学分和人数拆分；模板示例只放说明 Sheet，导入器只读空白数据 Sheet。教师花名册仅用于确定档案字段白名单；岗位侧只导入教务已经核定的“本学期岗位减免工作量”，不建设岗位职责目录、不按职务名称重复计算。

**Tech Stack:** Java 17、Spring Boot 4、MyBatis、EasyExcel 4、MySQL 8、JUnit 5、Mockito、AssertJ、Vue 3、Element Plus、Axios、ESLint、Vite。

---

## 范围与默认决策

1. 需求覆盖映射：需求 1 对应 Task 4；需求 2 对应 Task 3；需求 3 对应 Task 5；需求 8 对应 Task 6-7；需求 16 对应 Task 8。
2. 教学任务 v2 数量契约固定为：G1/G2=`plannedHours`，G3=`days`，G4=`credits + studentCount`，G5=`studentCount`，G6=`weeks + studentCount`；G5 不再要求无业务意义的基数。
3. 模板固定包含“填写说明/示例”和“导入数据”两个 Sheet；示例覆盖 G1/G3/G4/G5/G6，但服务只解析“导入数据”，模板下载后直接回传不得产生任何业务写入。
4. 旧模板只允许走 `templateVersion=V1` 兼容预检；确认时转换为 v2 规范行并保留原始 JSON，不再把旧基数无条件解释为学时。
5. `courseCoefficient` 仅允许 G2/G3/G5 使用；G1/G4/G6 填写该列即预检失败，防止产生无法解释的“总系数”。
6. 岗位导入简化为教师+学期+`positionWorkload`；该值是教务认定的学期岗位减免工作量，职务名称可空且不参与计算，不再设计 `RoleCatalog/rateUnit/standardHours`。
7. 花名册只用于教师字段白名单和异常映射验证，本轮不自动导入其中的真实人员；工号唯一，身份证/住址等敏感列不落库。
8. 当前环境无真实数据，结构变更通过 fresh DB 脚本整体重建；计划只生成重建检查与备份命令，真正执行重建前仍须确认连接的数据库名和环境。
6. 岗位任职确认时固化 `roleNameSnapshot`、`standardHoursSnapshot`、`rateUnitSnapshot`，G11 后续只读取快照，不回查当前目录值。
7. 花名册使用结构化 `Sheet1`；重复工号、未知部门、未知职称和未决人员性质均为阻断错误。已知 `WFIT1882` 两行必须由业务人员明确选择“保留一行”或“合并为一个账号并分别转任职”，系统不得自动合并。
8. 默认人员性质映射仅有“教师→专任”“银铃教师→银龄”；“教师兼行政”“行政”进入待确认队列。“讲师待遇”“无”不自动映射职称。
9. 身份证号、住址及未列入 `TeacherRosterImportDTO` 的敏感列只参与表头风险提示，不写 `raw_json`、日志或数据库。
10. 当前环境整体重建只更新并执行 fresh DB 初始化脚本；不再开发“按测试工号逐条清理”这一过度方案。重建前输出目标库、表数量和备份命令，真正执行属于单独高风险步骤。
11. 前端当前没有 Vitest；本计划不引入新测试框架，前端以 ESLint、生产构建和后端 Controller 契约测试为门禁。
12. 本轮花名册只做字段白名单和异常报告，不执行真实人员导入；后续得到教务正式导出文件时复用同一预检/确认框架。

## 文件职责

- Create: `rear/sql/22_workload_requirements.sql`：导入暂存、学期岗位减免字段、索引和菜单权限的幂等迁移。
- Modify: `rear/sql/01_biz_schema.sql`、`rear/sql/05_biz_menu.sql`、`rear/sql/06_test_accounts.sql`：fresh DB 等价定义和角色授权。
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/BizImportRow.java`：逐行暂存实体。
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/importing/ImportPreviewResponse.java`、`ImportConfirmRequest.java`、`ImportRowView.java`：统一预检/确认契约。
- Modify: `rear/workload-system/src/main/java/com/workload/system/domain/dto/TeachingTaskImportDTO.java`：教学任务 v2 数量字段。
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/TeachingTaskImportV1DTO.java`：旧模板只读兼容模型。
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/PositionWorkloadImportDTO.java`、`TeacherRosterImportDTO.java`：学期岗位减免与教师字段行契约。

- Create: `rear/workload-system/src/main/java/com/workload/system/mapper/BizImportRowMapper.java` 及对应 XML：暂存行持久化。
- Modify: `rear/workload-system/src/main/java/com/workload/system/domain/BizImportBatch.java` 及 Mapper XML：保存模板版本、文件哈希、操作者和确认时间。
- Create: `rear/workload-system/src/main/java/com/workload/system/service/ImportPreviewStore.java`：创建批次、保存规范行、校验确认令牌、原子变更批次状态。
- Modify: `ITeachingTaskImportService`/`TeachingTaskImportServiceImpl`：教学任务预检、确认和 v2 写入。
- Create: `IPositionWorkloadImportService`/`PositionWorkloadImportServiceImpl`：本学期岗位减免预检与确认。
- Modify: `ITeacherProfileImportService`/`TeacherProfileImportServiceImpl`：花名册字段预检和冲突报告；本期不开放确认写入真实人员。
- Modify: `BizTeachingTaskController`、`BizRoleAssignmentController`、`BizTeacherProfileController`：预检、确认、模板、错误行下载端点。
- Create: `front/RuoYi-Vue3/src/components/BizImportWizard/index.vue`：四步导入向导。
- Create: `front/RuoYi-Vue3/src/views/system/baseImport/index.vue`：教师字段预检/岗位减免导入两个入口页。
- Modify: `front/RuoYi-Vue3/src/views/system/{teachingTask,teacherProfile,roleAssignment}/index.vue` 与对应 API：接入统一导入向导。
- Test: 对应 `rear/workload-system/src/test/java/...`：DTO 校验、预检、确认、Controller、模板和服务测试。
- Create: `docs/测试/fixtures/teaching-task-v2-smoke.xlsx`：仅测试回归资源；生产模板示例位于说明 Sheet，二者都不得被当作真实导入数据。

### Task 1: 建立统一导入批次与逐行暂存

**Files:**
- Create: `rear/sql/22_workload_requirements.sql`
- Modify: `rear/sql/01_biz_schema.sql`
- Modify: `rear/workload-system/src/main/java/com/workload/system/domain/BizImportBatch.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/BizImportRow.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizImportBatchMapper.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/mapper/BizImportRowMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizImportBatchMapper.xml`
- Create: `rear/workload-system/src/main/resources/mapper/system/BizImportRowMapper.xml`
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/importing/ImportPreviewResponse.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/importing/ImportConfirmRequest.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/importing/ImportRowView.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/ImportPreviewStore.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/impl/ImportPreviewStoreImpl.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/service/impl/ImportPreviewStoreImplTest.java`

- [ ] **Step 1: 写失败测试，固定待确认批次状态机**

```java
@Test
void confirmRequiresPendingBatchAndMatchingToken()
{
    BizImportBatch batch = batch(17L, 1, "token-hash");
    when(batchMapper.selectBizImportBatchById(17L)).thenReturn(batch);
    when(tokenHasher.matches("confirm-token", "token-hash")).thenReturn(true);
    when(batchMapper.markConfirmed(17L, 1, 2, "jiaowu_test")).thenReturn(1);

    store.assertConfirmable(new ImportConfirmRequest(17L, "confirm-token", false));
    assertThat(store.markConfirmed(17L)).isEqualTo(1);
}

@Test
void confirmedBatchCannotBeConfirmedTwice()
{
    when(batchMapper.selectBizImportBatchById(17L)).thenReturn(batch(17L, 2, "token-hash"));
    assertThatThrownBy(() -> store.assertConfirmable(new ImportConfirmRequest(17L, "confirm-token", false)))
        .isInstanceOf(ServiceException.class)
        .hasMessageContaining("不处于待确认状态");
}
```

- [ ] **Step 2: 运行测试并验证失败**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=ImportPreviewStoreImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL，`ImportPreviewStore`、`ImportConfirmRequest` 和 `markConfirmed` 尚不存在。

- [ ] **Step 3: 实现数据库契约**

`22_workload_requirements.sql` 使用 `information_schema` 前置检测，给 `biz_import_batch` 增加：

```sql
ALTER TABLE biz_import_batch
  ADD COLUMN template_version VARCHAR(20) DEFAULT NULL COMMENT 'V1/V2/ROSTER_V1/ROLE_V1',
  ADD COLUMN source_hash CHAR(64) DEFAULT NULL COMMENT '源文件SHA-256',
  ADD COLUMN operator_id BIGINT DEFAULT NULL,
  ADD COLUMN operator_name VARCHAR(64) DEFAULT NULL,
  ADD COLUMN confirm_token_hash CHAR(64) DEFAULT NULL,
  ADD COLUMN confirmed_at DATETIME DEFAULT NULL;
CREATE UNIQUE INDEX uk_import_source ON biz_import_batch(import_type, source_hash, status);
CREATE TABLE IF NOT EXISTS biz_import_row (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  row_no INT NOT NULL,
  raw_json JSON DEFAULT NULL,
  normalized_json JSON DEFAULT NULL,
  status VARCHAR(16) NOT NULL COMMENT 'READY/BLOCKED/IMPORTED/SKIPPED/FAILED',
  error_code VARCHAR(50) DEFAULT NULL,
  error_message VARCHAR(500) DEFAULT NULL,
  target_id BIGINT DEFAULT NULL,
  create_by VARCHAR(64) DEFAULT '', create_time DATETIME DEFAULT NULL,
  update_by VARCHAR(64) DEFAULT '', update_time DATETIME DEFAULT NULL,
  PRIMARY KEY(id), UNIQUE KEY uk_batch_row(batch_id, row_no), KEY idx_batch_status(batch_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

同样字段必须并入 `01_biz_schema.sql`；`raw_json` 必须经过 DTO 白名单序列化，不保存未映射敏感列。

- [ ] **Step 4: 实现统一 Java 契约和原子状态更新**

```java
public record ImportConfirmRequest(Long batchId, String confirmToken, boolean updateSupport) {}

public record ImportPreviewResponse(Long batchId, String importType, String templateVersion,
        int totalCount, int readyCount, int blockedCount, List<ImportRowView> rows,
        List<String> warnings, String confirmToken) {}

@Update("update biz_import_batch set status=2, confirmed_at=now(), update_by=#{operator} " +
        "where id=#{id} and status=#{expectedStatus}")
int markConfirmed(@Param("id") Long id, @Param("expectedStatus") int expectedStatus,
        @Param("targetStatus") int targetStatus, @Param("operator") String operator);
```

`ImportPreviewStoreImpl` 只允许 `0→1` 完成预检、`1→2` 确认成功、`1→4` 确认失败；`blockedCount>0` 时 `assertConfirmable` 必须拒绝。

- [ ] **Step 5: 运行定向测试**

Run: 与 Step 2 相同。
Expected: PASS，重复确认与错误令牌均被拒绝，条件更新影响行数为 1。

- [ ] **Step 6: 提交**

```bash
git add rear/sql/01_biz_schema.sql rear/sql/22_workload_requirements.sql rear/workload-system/src/main/java/com/workload/system/domain rear/workload-system/src/main/java/com/workload/system/domain/dto/importing rear/workload-system/src/main/java/com/workload/system/mapper/BizImportBatchMapper.java rear/workload-system/src/main/java/com/workload/system/mapper/BizImportRowMapper.java rear/workload-system/src/main/resources/mapper/system/BizImportBatchMapper.xml rear/workload-system/src/main/resources/mapper/system/BizImportRowMapper.xml rear/workload-system/src/main/java/com/workload/system/service/ImportPreviewStore.java rear/workload-system/src/main/java/com/workload/system/service/impl/ImportPreviewStoreImpl.java rear/workload-system/src/test/java/com/workload/system/service/impl/ImportPreviewStoreImplTest.java
git commit -m "feat: 建立导入预检批次契约"
```

### Task 2: 提供错误行下载和批次查询契约

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/controller/BizImportBatchController.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/IBizImportBatchService.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/impl/BizImportBatchServiceImpl.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/controller/BizImportBatchControllerTest.java`

- [ ] **Step 1: 写失败测试，错误文件不得包含敏感原始列**

```java
@Test
void exportsOnlySafeNormalizedColumnsAndError()
{
    when(service.selectErrorRows(17L)).thenReturn(List.of(
        new ImportRowView(61, "WFIT1882", "张某", "BLOCKED", "DUPLICATE_USER_CODE", "工号重复")));
    MockHttpServletResponse response = new MockHttpServletResponse();
    controller.exportErrors(17L, response);
    assertThat(response.getContentAsByteArray()).isNotEmpty();
    assertWorkbookHeaders(response, "行号", "教师工号", "教师姓名", "状态", "错误码", "错误信息");
}
```

- [ ] **Step 2: 运行测试验证端点尚不存在**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=BizImportBatchControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL，`exportErrors`/`selectErrorRows` 尚未定义。

- [ ] **Step 3: 实现查询和下载端点**

```java
@PreAuthorize("@ss.hasPermi('system:importBatch:query')")
@GetMapping("/{batchId}/rows")
public TableDataInfo rows(@PathVariable Long batchId, @RequestParam(required=false) String status)

@PreAuthorize("@ss.hasPermi('system:importBatch:query')")
@GetMapping("/{batchId}/errors")
public void exportErrors(@PathVariable Long batchId, HttpServletResponse response)
```

导出列只能来自 `normalized_json` 的各导入 DTO 白名单字段和错误信息，不导出 `raw_json`；批次不存在返回 404，非本人且无全量数据权限返回 403。

- [ ] **Step 4: 运行测试**

Run: 与 Step 2 相同。
Expected: PASS，工作簿表头精确匹配且无身份证、住址列。

- [ ] **Step 5: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/controller/BizImportBatchController.java rear/workload-system/src/main/java/com/workload/system/service/IBizImportBatchService.java rear/workload-system/src/main/java/com/workload/system/service/impl/BizImportBatchServiceImpl.java rear/workload-system/src/test/java/com/workload/system/controller/BizImportBatchControllerTest.java
git commit -m "feat: 支持下载导入错误行"
```

### Task 3: 拆分教学任务 v2 数量字段

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/domain/dto/TeachingTaskImportDTO.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/TeachingTaskImportV1DTO.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/domain/BizTeachingTask.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizTeachingTaskMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizTeachingTaskMapper.xml`
- Modify: `rear/sql/22_workload_requirements.sql`
- Modify: `rear/sql/01_biz_schema.sql`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/impl/TeachingTaskImportServiceImpl.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/service/impl/TeachingTaskImportServiceImplTest.java`

- [ ] **Step 1: 写参数化失败测试，固定 G1-G6 互斥矩阵**

```java
@ParameterizedTest
@MethodSource("quantityCases")
void validatesQuantityByCategory(TeachingTaskImportDTO row, String expectedMessage)
{
    assertThatThrownBy(() -> invokeValidateRow(row))
        .isInstanceOf(ServiceException.class)
        .hasMessageContaining(expectedMessage);
}

static Stream<Arguments> quantityCases()
{
    return Stream.of(
        arguments(row("G1", null, null, null, null, 80), "G1必须填写计划学时"),
        arguments(row("G3", 48, null, null, null, 35), "G3只能填写天数"),
        arguments(row("G4", null, null, null, null, 15), "G4必须填写课程设计学分"),
        arguments(row("G5", null, null, null, null, null), "G5必须填写指导人数"),
        arguments(row("G6", null, null, 4, null, null), "G6必须填写指导人数"));
}
```

- [ ] **Step 2: 运行测试并确认旧 `baseValue` 契约失败**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=TeachingTaskImportServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL，DTO 没有 `plannedHours/days/weeks/credits`。

- [ ] **Step 3: 替换 v2 DTO 数量列并保留 v1 只读 DTO**

```java
@ExcelProperty("计划学时") private BigDecimal plannedHours;
@ExcelProperty("天数") private BigDecimal days;
@ExcelProperty("周数") private BigDecimal weeks;
@ExcelProperty("课程设计学分") private BigDecimal credits;
@ExcelProperty("选课/指导人数") private Integer studentCount;
```

`TeachingTaskImportV1DTO` 保留原 17 列和 `baseValue`，仅由预检转换器读取，不允许直接进入 `processSingleRow`。

- [ ] **Step 4: 保存源数量快照并按类别写明细**

SQL 给 `biz_teaching_task` 增加 `input_planned_hours/input_days/input_weeks/input_credits DECIMAL(10,2)` 和 `quantity_type VARCHAR(16)`；服务映射使用：

```java
return switch (type) {
    case "G1", "G2" -> dto.getPlannedHours();
    case "G3" -> dto.getDays();
    case "G4" -> dto.getCredits();
    case "G5" -> BigDecimal.valueOf(dto.getStudentCount());
    case "G6" -> dto.getWeeks();
    default -> throw new ServiceException("工作量类别必须为 G1~G6");
};
```

并将 `createG1/G2/G3/G4/G6Detail` 分别改读上述语义字段；G5 只读 `studentCount`。正数小数最多 2 位，人数必须为正整数。

- [ ] **Step 5: 增加系数列适用范围测试与实现**

```java
assertThatThrownBy(() -> invokeValidateRow(validG1().courseCoefficient(new BigDecimal("1.2"))))
    .hasMessageContaining("G1不支持课程系数列");
```

仅 G2/G3/G5 允许非空 `courseCoefficient`，显式值优先于规则默认值。

- [ ] **Step 6: 运行测试**

Run: 与 Step 2 相同。
Expected: PASS；G1-G6 的必填、互斥、精度和系数范围测试全部通过。

- [ ] **Step 7: 提交**

```bash
git add rear/sql/01_biz_schema.sql rear/sql/22_workload_requirements.sql rear/workload-system/src/main/java/com/workload/system/domain/BizTeachingTask.java rear/workload-system/src/main/java/com/workload/system/domain/dto/TeachingTaskImportDTO.java rear/workload-system/src/main/java/com/workload/system/domain/dto/TeachingTaskImportV1DTO.java rear/workload-system/src/main/java/com/workload/system/mapper/BizTeachingTaskMapper.java rear/workload-system/src/main/resources/mapper/system/BizTeachingTaskMapper.xml rear/workload-system/src/main/java/com/workload/system/service/impl/TeachingTaskImportServiceImpl.java rear/workload-system/src/test/java/com/workload/system/service/impl/TeachingTaskImportServiceImplTest.java
git commit -m "feat: 拆分教学任务数量字段"
```

### Task 4: 教学任务样例、模板版本和两阶段导入

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/ITeachingTaskImportService.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/impl/TeachingTaskImportServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/controller/BizTeachingTaskController.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/controller/BizTeachingTaskControllerTest.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/service/impl/TeachingTaskImportServiceImplTest.java`
- Create: `docs/测试/fixtures/teaching-task-v2-smoke.xlsx`
- Create: `rear/workload-system/src/test/java/com/workload/system/fixture/TeachingTaskFixtureWorkbookTest.java`

- [ ] **Step 1: 写失败测试，模板必须有说明页但数据页为空**

```java
@Test
void v2TemplateSeparatesInstructionsFromImportRows() throws Exception
{
    MockHttpServletResponse response = new MockHttpServletResponse();
    controller.importTemplate("V2", response);
    try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
        assertThat(wb.getSheet("导入数据").getLastRowNum()).isZero();
        assertThat(wb.getSheet("示例与说明").getRow(2).getCell(0).getStringCellValue()).isEqualTo("2025-2026-2");
        assertThat(headers(wb.getSheet("导入数据"))).contains("计划学时", "天数", "周数", "课程设计学分");
    }
}
```

- [ ] **Step 2: 运行测试验证当前单 Sheet 空模板不满足契约**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=BizTeachingTaskControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL，`importTemplate(String, response)` 尚不存在。

- [ ] **Step 3: 实现模板与样例**

`POST /system/teachingTask/importTemplate?version=V2` 返回两 Sheet 工作簿；说明页至少包含以下五类字段示例：

```text
G1: plannedHours=48, studentCount=80
G3: days=15, studentCount=35
G4: credits=2, studentCount=60
G5: studentCount=8（其余数量字段为空）
G6: weeks=4, studentCount=25
```

列顺序为：学期、工号、姓名、课程、代码、类别、层次、专业、性质、级别、角色、评价、人数、计划学时、天数、周数、课程设计学分、课程系数、班级、重复次序。说明页使用“示例教师”等虚构标签；独立测试夹具由测试代码构造业务行并断言公式结果，生产模板本身不携带可确认入库的真实数据。

- [ ] **Step 4: 写失败测试，预检不得写教学任务**

```java
@Test
void previewPersistsOnlyDataSheetRowsAndDoesNotCreateBusinessData()
{
    ImportPreviewResponse result = service.preview(workbookWithFiveInstructionRowsAndTwoDataRows(), "smoke.xlsx", "V2");
    assertThat(result.readyCount()).isEqualTo(2);
    verify(teachingTaskMapper, never()).insertBizTeachingTask(any());
    verify(workloadItemMapper, never()).insertBizWorkloadItem(any());
    verify(importRowMapper, times(2)).insertBizImportRow(any());
}
```

- [ ] **Step 5: 实现教学任务预检/确认接口**

```java
ImportPreviewResponse preview(InputStream input, String fileName, String templateVersion);
ImportResult confirm(ImportConfirmRequest request);

@PostMapping("/importPreview")
public AjaxResult importPreview(@RequestParam MultipartFile file,
        @RequestParam(defaultValue="V2") String templateVersion)

@PostMapping("/importConfirm")
public AjaxResult importConfirm(@RequestBody ImportConfirmRequest request)
```

`/importExcel` 保留一个发布周期，但内部执行 `preview`；仅当 0 阻断且请求显式 `legacyAutoConfirm=true` 时确认，并返回弃用警告。确认按 `biz_import_row.row_no` 顺序调用 `processSingleRow`，幂等键为 `(工号,学期,课程代码,班级,类别)`；重复默认 `SKIPPED`，不静默更新。

- [ ] **Step 6: 运行教学任务测试和夹具测试**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=TeachingTaskImportServiceImplTest,BizTeachingTaskControllerTest,TeachingTaskFixtureWorkbookTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS；模板说明页含 G1/G3/G4/G5/G6 示例且数据页为空；预检只统计数据页 2 行、业务表 0 写入；确认后测试夹具 2 行成功并得到固定公式结果。

- [ ] **Step 7: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/service/ITeachingTaskImportService.java rear/workload-system/src/main/java/com/workload/system/service/impl/TeachingTaskImportServiceImpl.java rear/workload-system/src/main/java/com/workload/system/controller/BizTeachingTaskController.java rear/workload-system/src/test/java/com/workload/system/controller/BizTeachingTaskControllerTest.java rear/workload-system/src/test/java/com/workload/system/service/impl/TeachingTaskImportServiceImplTest.java rear/workload-system/src/test/java/com/workload/system/fixture/TeachingTaskFixtureWorkbookTest.java docs/测试/fixtures/teaching-task-v2-smoke.xlsx
git commit -m "feat: 增加教学任务预检与样例"
```

### Task 5: 导入本学期岗位减免工作量（替代原岗位目录/任职复杂模型）

**Files:**
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/PositionWorkloadImportDTO.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/IPositionWorkloadImportService.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/impl/PositionWorkloadImportServiceImpl.java`
- Modify: `BizRoleAssignmentController.java`、`BizRoleAssignment.java` 及 Mapper/XML
- Test: `PositionWorkloadImportServiceImplTest.java`

- [ ] **Step 1: 写失败测试**：教师+学期+`positionWorkload` 可预检；职务为空仍 READY；相同批次重复行为 SKIPPED；状态 1/2 汇总阻断确认。
- [ ] **Step 2: 运行 RED**：`mvn -f rear/pom.xml test -pl workload-system -am -Dtest=PositionWorkloadImportServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`，预期 DTO/服务不存在。
- [ ] **Step 3: 实现 DTO**：表头固定为教师工号、教师姓名、学年学期、岗位减免工作量（本学期）、职务名称（可选）、备注；减免值 `>=0`、两位小数。
- [ ] **Step 4: 实现预检/确认**：沿用 `biz_role_assignment`，`allowance_rate` 存本学期值；不除以 2、不查岗位目录、不按日期折算；保存 `import_batch`，同批次幂等。
- [ ] **Step 5: 运行 GREEN**：预期合法行写入，冻结行零写入，重复确认不新增。
- [ ] **Step 6: 提交**：`git commit -m "feat: 导入学期岗位减免工作量"`。

### Task 6: 教师花名册字段白名单与异常报告（不导入真实人员）

**Files:** `TeacherRosterImportDTO.java`、`TeacherProfileImportServiceImpl.java`、`BizTeacherProfileController.java` 及测试。

- [ ] **Step 1: 写失败测试**：只保留工号、姓名、系、教研室、职称、人员性质候选、联系方式和岗位减免值；身份证/地址不进入 raw JSON；重复工号与异常职称阻断。
- [ ] **Step 2: 运行 RED**：预期白名单 DTO/预检接口不存在。
- [ ] **Step 3: 实现 `POST /system/teacherProfile/importPreview`**：只返回字段映射、异常统计和脱敏行，不提供 confirm 写入接口。
- [ ] **Step 4: 前端展示“字段参考预检”**：明确当前文件不会导入真实教师，允许下载异常清单。
- [ ] **Step 5: 验证并提交**：后端测试、前端 lint/build；`git commit -m "feat: 预检教师档案字段"`。

### Task 7: Fresh DB 整体重建准备

- [ ] **Step 1: 更新 `01_biz_schema.sql` 与初始化文档**，移除岗位目录方案，固化学期岗位减免字段和新导入暂存表。
- [ ] **Step 2: 新增 `rear/scripts/verify-rebuild-target.ps1`**，只输出数据库名、主机、业务表数与待备份表，不执行 DROP。
- [ ] **Step 3: 在临时空库执行全套初始化脚本**，预期所有表/索引/菜单创建成功。
- [ ] **Step 4: 记录重建命令与人工确认短语**；真实环境执行放到独立高风险任务。
- [ ] **Step 5: 提交**：`git commit -m "chore: 准备业务库整体重建"`。

### 以下旧 Task 5-9 已被 2026-09-12 业务确认取代，禁止执行

> 原方案错误地把岗位减免设计为岗位目录+学年标准+任职天数折算，并为无真实数据环境设计逐账号清理。现确认应直接导入本学期岗位减免值，当前库可整体重建。以下内容仅保留审查痕迹。

### [已废弃] 原 Task 5: 建立岗位目录及制度快照

**Files:**
- Modify: `rear/sql/22_workload_requirements.sql`
- Modify: `rear/sql/01_biz_schema.sql`
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/BizRoleCatalog.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/RoleCatalogImportDTO.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/mapper/BizRoleCatalogMapper.java`
- Create: `rear/workload-system/src/main/resources/mapper/system/BizRoleCatalogMapper.xml`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/IRoleCatalogImportService.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/impl/RoleCatalogImportServiceImpl.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/controller/BizRoleCatalogController.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/service/impl/RoleCatalogImportServiceImplTest.java`

- [ ] **Step 1: 写失败测试，目录重叠和标准单位必须阻断**

```java
@Test
void blocksOverlappingEffectiveRoleCode()
{
    when(mapper.countEffectiveOverlap("CLASS_ADVISOR", date("2025-09-01"), date("2027-08-31"))).thenReturn(1);
    ImportPreviewResponse result = service.preview(rows(catalog("CLASS_ADVISOR", "班主任", "180", "YEAR")));
    assertThat(result.blockedCount()).isOne();
    assertThat(result.rows().get(0).errorCode()).isEqualTo("ROLE_EFFECTIVE_OVERLAP");
}
```

- [ ] **Step 2: 运行测试验证岗位目录对象不存在**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=RoleCatalogImportServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL。

- [ ] **Step 3: 创建岗位目录表和实体**

```sql
CREATE TABLE IF NOT EXISTS biz_role_catalog (
 id BIGINT NOT NULL AUTO_INCREMENT, role_code VARCHAR(50) NOT NULL,
 role_name VARCHAR(100) NOT NULL, responsibility VARCHAR(500), qualification VARCHAR(500),
 standard_hours DECIMAL(10,2) NOT NULL, rate_unit VARCHAR(10) NOT NULL,
 effective_from DATE NOT NULL, effective_to DATE, status TINYINT NOT NULL DEFAULT 1,
 create_by VARCHAR(64) DEFAULT '', create_time DATETIME, update_by VARCHAR(64) DEFAULT '',
 update_time DATETIME, remark VARCHAR(500), PRIMARY KEY(id),
 UNIQUE KEY uk_role_version(role_code,effective_from), KEY idx_role_effective(role_code,status,effective_to)
);
```

`rate_unit` 只允许 `YEAR/SEMESTER`，`standard_hours>0`，结束日期不得早于开始日期；生效区间重叠阻断。

- [ ] **Step 4: 实现模板、预检、确认接口**

```java
@GetMapping("/importTemplate")
@PostMapping("/importPreview")
@PostMapping("/importConfirm")
@GetMapping("/list")
```

权限分别使用 `system:roleCatalog:import` 和 `system:roleCatalog:list`。模板示例固定 `CLASS_ADVISOR/班主任/180/YEAR` 与 `DEPT_HEAD/教研室主任/120/YEAR`，岗位系数列不进入 DTO。

- [ ] **Step 5: 运行测试**

Run: 与 Step 2 相同。
Expected: PASS；重复编码重叠被阻断，不重叠新版本可确认。

- [ ] **Step 6: 提交**

```bash
git add rear/sql/01_biz_schema.sql rear/sql/22_workload_requirements.sql rear/workload-system/src/main/java/com/workload/system/domain/BizRoleCatalog.java rear/workload-system/src/main/java/com/workload/system/domain/dto/RoleCatalogImportDTO.java rear/workload-system/src/main/java/com/workload/system/mapper/BizRoleCatalogMapper.java rear/workload-system/src/main/resources/mapper/system/BizRoleCatalogMapper.xml rear/workload-system/src/main/java/com/workload/system/service/IRoleCatalogImportService.java rear/workload-system/src/main/java/com/workload/system/service/impl/RoleCatalogImportServiceImpl.java rear/workload-system/src/main/java/com/workload/system/controller/BizRoleCatalogController.java rear/workload-system/src/test/java/com/workload/system/service/impl/RoleCatalogImportServiceImplTest.java
git commit -m "feat: 增加岗位制度目录"
```

### [已废弃] 原 Task 6: 岗位任职预检、导入与快照

**Files:**
- Modify: `rear/sql/22_workload_requirements.sql`
- Modify: `rear/sql/01_biz_schema.sql`
- Modify: `rear/workload-system/src/main/java/com/workload/system/domain/BizRoleAssignment.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/RoleAssignmentImportDTO.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizRoleAssignmentMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizRoleAssignmentMapper.xml`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/IRoleAssignmentImportService.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/impl/RoleAssignmentImportServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/controller/BizRoleAssignmentController.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/service/impl/RoleAssignmentImportServiceImplTest.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/controller/BizRoleAssignmentControllerTest.java`

- [ ] **Step 1: 写失败测试，锁定教师、目录、日期和重复任职校验**

```java
@Test
void previewResolvesTeacherAndCopiesCatalogSnapshot()
{
    when(userService.selectUserByUserName("test_prof")).thenReturn(user(2001L, "测试讲师"));
    when(catalogMapper.selectEffective("CLASS_ADVISOR", date("2026-02-20"))).thenReturn(
        catalog(7L, "班主任", "180", "YEAR"));
    ImportPreviewResponse result = service.preview(rows(assignment("test_prof", "测试讲师",
        "CLASS_ADVISOR", "2026-02-20", "2026-07-15", "2025-2026-2")));
    assertThat(result.readyCount()).isOne();
    assertThat(result.rows().get(0).normalizedJson()).contains("standardHoursSnapshot\":180", "rateUnitSnapshot\":\"YEAR");
}
```

另测教师姓名不一致、岗位停用、任职区间不在目录生效期、同教师同岗位区间重叠均为 BLOCKED。

- [ ] **Step 2: 运行测试验证失败**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=RoleAssignmentImportServiceImplTest,BizRoleAssignmentControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL。

- [ ] **Step 3: 扩展任职快照字段和唯一性查询**

```sql
ALTER TABLE biz_role_assignment
 ADD COLUMN role_id BIGINT DEFAULT NULL,
 ADD COLUMN role_code VARCHAR(50) DEFAULT NULL,
 ADD COLUMN role_name_snapshot VARCHAR(100) DEFAULT NULL,
 ADD COLUMN standard_hours_snapshot DECIMAL(10,2) DEFAULT NULL,
 ADD COLUMN rate_unit_snapshot VARCHAR(10) DEFAULT NULL,
 ADD COLUMN import_batch VARCHAR(50) DEFAULT NULL,
 ADD KEY idx_role_assignment_lookup(user_id,role_code,start_date,end_date);
```

旧数据迁移：`standard_hours_snapshot=allowance_rate`；`role_type='督导'` 为 `SEMESTER`，其他为 `YEAR`；保留 `role_type/allowance_rate` 兼容，不删除旧列。

- [ ] **Step 4: 实现 DTO 和确认写入**

```java
@ExcelProperty("教师工号") private String userCode;
@ExcelProperty("教师姓名") private String userName;
@ExcelProperty("岗位编码") private String roleCode;
@ExcelProperty("目标范围") private String target;
@ExcelProperty("任职开始") private LocalDate startDate;
@ExcelProperty("任职结束") private LocalDate endDate;
@ExcelProperty("学年学期") private String semester;
@ExcelProperty("标准值覆盖") private BigDecimal standardHoursOverride;
@ExcelProperty("标准单位覆盖") private String rateUnitOverride;
```

覆盖值只有具备 `system:roleAssignment:override` 权限时允许；否则非空即阻断。确认写 `status=1`，保存批次号；相同幂等键默认 SKIPPED。

- [ ] **Step 5: 实现 Controller 契约**

```java
@GetMapping("/importTemplate")
@PostMapping("/importPreview")
@PostMapping("/importConfirm")
```

统一权限 `system:roleAssignment:import`，返回 `batchId/readyCount/blockedCount/conflicts/estimatedG11`；预计 G11 使用目录单位和学期日期，只展示不落工作量表。

- [ ] **Step 6: 运行定向测试**

Run: 与 Step 2 相同。
Expected: PASS；两条标准任职样例预检 READY，确认后快照完整，重复确认不新增记录。

- [ ] **Step 7: 提交**

```bash
git add rear/sql/01_biz_schema.sql rear/sql/22_workload_requirements.sql rear/workload-system/src/main/java/com/workload/system/domain/BizRoleAssignment.java rear/workload-system/src/main/java/com/workload/system/domain/dto/RoleAssignmentImportDTO.java rear/workload-system/src/main/java/com/workload/system/mapper/BizRoleAssignmentMapper.java rear/workload-system/src/main/resources/mapper/system/BizRoleAssignmentMapper.xml rear/workload-system/src/main/java/com/workload/system/service/IRoleAssignmentImportService.java rear/workload-system/src/main/java/com/workload/system/service/impl/RoleAssignmentImportServiceImpl.java rear/workload-system/src/main/java/com/workload/system/controller/BizRoleAssignmentController.java rear/workload-system/src/test/java/com/workload/system/service/impl/RoleAssignmentImportServiceImplTest.java rear/workload-system/src/test/java/com/workload/system/controller/BizRoleAssignmentControllerTest.java
git commit -m "feat: 支持岗位任职预检导入"
```

### [已废弃] 原 Task 7: 真实教师花名册映射预检

**Files:**
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/TeacherRosterImportDTO.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/TeacherRosterMappingConfig.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/ITeacherProfileImportService.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/impl/TeacherProfileImportServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/controller/BizTeacherProfileController.java`
- Modify: `rear/workload-admin/src/main/resources/application.yml`
- Test: `rear/workload-system/src/test/java/com/workload/system/service/impl/TeacherProfileImportServiceImplTest.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/controller/BizTeacherProfileControllerTest.java`

- [ ] **Step 1: 写失败测试，固定已知花名册阻断统计**

```java
@Test
void duplicateEmployeeCodeBlocksWholeConfirmation()
{
    List<TeacherRosterImportDTO> rows = List.of(
        roster(61, "WFIT1882", "甲", "人工智能系", "教师", "讲师"),
        roster(87, "WFIT1882", "甲", "机械工程系", "行政", "无"));
    ImportPreviewResponse preview = service.previewRoster(rows, "新智能制造学院花名册-测试数据.xlsx");
    assertThat(preview.blockedCount()).isEqualTo(2);
    assertThat(preview.warnings()).anyMatch(v -> v.contains("WFIT1882").contains("第61、87行"));
}
```

- [ ] **Step 2: 运行测试验证花名册 DTO/预检不存在**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=TeacherProfileImportServiceImplTest,BizTeacherProfileControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL。

- [ ] **Step 3: 定义结构化 `Sheet1` 白名单 DTO**

```java
@ExcelProperty("系") private String departmentName;
@ExcelProperty("教研室") private String teachingOffice;
@ExcelProperty("员工姓名") private String nickName;
@ExcelProperty("职务") private String positionName;
@ExcelProperty("职务匹配工作量") private BigDecimal positionWorkload;
@ExcelProperty("岗位") private String employmentType;
@ExcelProperty("职称") private String title;
@ExcelProperty("工号") private String userCode;
@ExcelProperty("联系方式") private String phonenumber;
```

不定义身份证、地址字段；解析前只读取表头名用于敏感列提示，绝不读取或序列化敏感列值。

- [ ] **Step 4: 配置映射而非硬编码中文别名**

`application.yml` 增加：

```yaml
wfit:
  roster-import:
    sheet-name: Sheet1
    department-aliases:
      人工智能系: 人工智能系
      机械工程系: 机械工程系
      大数据系: 大数据系
      行政: 行政
    nature-mapping:
      教师: 专任
      银铃教师: 银龄
    unresolved-employment-types: [教师兼行政, 行政]
    unresolved-titles: [无, 讲师待遇]
```

配置启动时校验目标部门是否存在；缺失只产生预检阻断，不自动创建部门。

- [ ] **Step 5: 实现预检接口和响应统计**

```java
@PostMapping("/importPreview")
public AjaxResult importPreview(@RequestParam MultipartFile file,
        @RequestParam(defaultValue="Sheet1") String sheetName)
```

响应必须含 `totalRows/uniqueUserCodes/duplicateCodes/deptCounts/employmentTypeCounts/unresolvedTitles/unresolvedNatures/sensitiveHeaders`。已知源文件验收值：116 行、115 个初始唯一工号、`WFIT1882` 重复；预检必须阻断确认，不承诺导入 115 个账号。

- [ ] **Step 6: 运行定向测试**

Run: 与 Step 2 相同。
Expected: PASS；重复、职称“无/讲师待遇”、行政类人员性质均为 BLOCKED，手机号按字符串保留。

- [ ] **Step 7: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/domain/dto/TeacherRosterImportDTO.java rear/workload-system/src/main/java/com/workload/system/domain/dto/TeacherRosterMappingConfig.java rear/workload-system/src/main/java/com/workload/system/service/ITeacherProfileImportService.java rear/workload-system/src/main/java/com/workload/system/service/impl/TeacherProfileImportServiceImpl.java rear/workload-system/src/main/java/com/workload/system/controller/BizTeacherProfileController.java rear/workload-admin/src/main/resources/application.yml rear/workload-system/src/test/java/com/workload/system/service/impl/TeacherProfileImportServiceImplTest.java rear/workload-system/src/test/java/com/workload/system/controller/BizTeacherProfileControllerTest.java
git commit -m "feat: 增加教师花名册预检"
```

### [已废弃] 原 Task 8: 真实教师确认导入与逐行事务

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/domain/BizTeacherProfile.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizTeacherProfileMapper.java`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizTeacherProfileMapper.xml`
- Modify: `rear/workload-system/src/main/java/com/workload/system/service/impl/TeacherProfileImportServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/controller/BizTeacherProfileController.java`
- Modify: `rear/sql/22_workload_requirements.sql`
- Modify: `rear/sql/01_biz_schema.sql`
- Test: `rear/workload-system/src/test/java/com/workload/system/service/impl/TeacherProfileImportServiceImplTest.java`

- [ ] **Step 1: 写失败测试，未解决冲突不可确认且预检无副作用**

```java
@Test
void confirmRejectsBlockedRosterWithoutWritingUsers()
{
    when(importRowMapper.countByStatus(88L, "BLOCKED")).thenReturn(3);
    assertThatThrownBy(() -> service.confirmRoster(new ImportConfirmRequest(88L, "token", true)))
        .hasMessageContaining("仍有3条阻断记录");
    verify(sysUserService, never()).insertUser(any());
    verify(profileMapper, never()).insertBizTeacherProfile(any());
}
```

- [ ] **Step 2: 运行测试验证确认方法不存在**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=TeacherProfileImportServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL。

- [ ] **Step 3: 保存非敏感花名册扩展快照**

给 `biz_teacher_profile` 增加 `teaching_office VARCHAR(100)`、`position_workload DECIMAL(10,2)`、`position_workload_unit VARCHAR(10)`、`profile_import_batch VARCHAR(50)`；不增加身份证或地址列。`position_workload_unit` 在本批次必须由确认请求明确选择 `YEAR` 或 `SEMESTER`，未选择即阻断。

- [ ] **Step 4: 实现确认接口和逐行事务**

```java
@PostMapping("/importConfirm")
public AjaxResult importConfirm(@RequestBody TeacherRosterConfirmRequest request)

public record TeacherRosterConfirmRequest(Long batchId, String confirmToken,
        boolean updateSupport, String positionWorkloadUnit, Map<Integer, RosterResolution> resolutions) {}
```

`resolutions` 必须覆盖所有重复/枚举冲突行；`processRosterRow` 继续通过 AOP 代理逐行事务，按工号创建/更新 `sys_user` 与 `biz_teacher_profile`，昵称冲突不自动覆盖而按 resolution 执行。确认返回 created/updated/skipped/failed 及逐行目标 ID。

- [ ] **Step 5: 增加首次密码与权限断言**

```java
verify(sysUserService).insertUser(argThat(u -> u.getPwdUpdateDate() == null));
verify(sysUserService).insertUserAuth(createdId, new Long[]{4L});
```

新用户继续触发首次改密；已有非教师用户不得自动追加教师角色，必须阻断并由管理员确认角色变更。

- [ ] **Step 6: 运行测试**

Run: 与 Step 2 相同。
Expected: PASS；无 resolution 时 0 写入，解决后逐行确认且失败行独立回滚。

- [ ] **Step 7: 提交**

```bash
git add rear/sql/01_biz_schema.sql rear/sql/22_workload_requirements.sql rear/workload-system/src/main/java/com/workload/system/domain/BizTeacherProfile.java rear/workload-system/src/main/java/com/workload/system/mapper/BizTeacherProfileMapper.java rear/workload-system/src/main/resources/mapper/system/BizTeacherProfileMapper.xml rear/workload-system/src/main/java/com/workload/system/service/impl/TeacherProfileImportServiceImpl.java rear/workload-system/src/main/java/com/workload/system/controller/BizTeacherProfileController.java rear/workload-system/src/test/java/com/workload/system/service/impl/TeacherProfileImportServiceImplTest.java
git commit -m "feat: 确认导入教师花名册"
```

### [已废弃] 原 Task 9: 测试数据清理预览、备份清单与回滚

**Files:**
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/TestDataCleanupPreview.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/TestDataCleanupService.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/impl/TestDataCleanupServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/controller/BizTeacherProfileController.java`
- Create: `rear/workload-system/src/main/resources/mapper/system/TestDataCleanupMapper.xml`
- Create: `rear/workload-system/src/main/java/com/workload/system/mapper/TestDataCleanupMapper.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/service/impl/TestDataCleanupServiceImplTest.java`

- [ ] **Step 1: 写失败测试，系统账号永不进入候选**

```java
@Test
void previewUsesExplicitWhitelistAndExcludesProtectedAccounts()
{
    TestDataCleanupPreview preview = service.preview(List.of("test_prof", "test_aprof", "admin_test"));
    assertThat(preview.allowedUserCodes()).containsExactly("test_prof", "test_aprof");
    assertThat(preview.protectedUserCodes()).contains("admin_test");
    assertThat(preview.tables()).containsKeys("biz_workload_item", "biz_role_assignment", "biz_teacher_profile", "sys_user");
}
```

- [ ] **Step 2: 运行测试验证清理预览服务不存在**

Run: `mvn -f rear/pom.xml test -pl workload-system -am -Dtest=TestDataCleanupServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: testCompile FAIL。

- [ ] **Step 3: 实现只读影响预览**

```java
@GetMapping("/cleanupPreview")
public AjaxResult cleanupPreview(@RequestParam List<String> userCodes)
```

查询所有外键/逻辑关联表计数、汇总状态、导入批次和用户角色；响应包含 `backupTables`、`rowCounts`、`blockedReasons`、`rollbackOrder` 和 `confirmationPhrase`。存在 status=1/2 汇总、非测试角色、白名单外工号时阻断。

- [ ] **Step 4: 生成备份/回滚 SQL 文本但不执行**

服务生成带时间戳的建议命令：

```bash
mysqldump --single-transaction wflg_workload sys_user sys_user_role biz_teacher_profile biz_teaching_task biz_role_assignment biz_workload_item biz_wl_theory biz_wl_practice biz_wl_internship_training biz_wl_course_design biz_wl_thesis biz_wl_concentrated_internship biz_wl_management biz_workload_summary biz_pay_record biz_allowance_item > backup-test-users-YYYYMMDDHHMMSS.sql
mysql wflg_workload < backup-test-users-YYYYMMDDHHMMSS.sql
```

响应不得包含数据库密码。删除执行端点本任务不实现；实际清理必须在独立授权任务中基于确认短语新增，因此本计划不会删除任何数据。

- [ ] **Step 5: 运行测试**

Run: 与 Step 2 相同。
Expected: PASS；只调用 SELECT mapper，不存在 DELETE/UPDATE mapper 调用。

- [ ] **Step 6: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/domain/dto/TestDataCleanupPreview.java rear/workload-system/src/main/java/com/workload/system/service/TestDataCleanupService.java rear/workload-system/src/main/java/com/workload/system/service/impl/TestDataCleanupServiceImpl.java rear/workload-system/src/main/java/com/workload/system/controller/BizTeacherProfileController.java rear/workload-system/src/main/java/com/workload/system/mapper/TestDataCleanupMapper.java rear/workload-system/src/main/resources/mapper/system/TestDataCleanupMapper.xml rear/workload-system/src/test/java/com/workload/system/service/impl/TestDataCleanupServiceImplTest.java
git commit -m "feat: 预览测试数据清理影响"
```

## 执行边界

执行者只实施 Task 1-7；所有标记“已废弃”的旧任务不得执行。