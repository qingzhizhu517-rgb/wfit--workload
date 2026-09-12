# 批量导出与酬金验收 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在保留单教师附件1 GET 契约的前提下，交付可控内存、可追踪失败、权限收口的多教师批量导出，并建立覆盖绩效与 A–G 其他酬金全部关键边界的可重复验收夹具。

**Architecture:** 导出侧把模板渲染从 Controller 抽到专用服务，批量接口用一次批量查询后按 `userId` 分组；小批量输出一个多 Sheet 工作簿，大批量逐教师写入 ZIP，所有输出直接写 `HttpServletResponse`，不在内存聚合整个压缩包。酬金侧以测试夹具固化策略输入、汇总快照和预期金额，并在 `pay_record` 写入前用带行锁的汇总状态复核消除审批并发窗口。

**Tech Stack:** Java 17、Spring Boot 4、Spring MVC、MyBatis、Apache POI 5.5.1、EasyExcel 4.0.3、JUnit 5、Mockito、AssertJ、Vue 3、Element Plus、Axios、Node.js `node:test`。

---

## 需求映射与文件结构

- 需求 7：任务 1–5。保留 `GET /system/export/attachment1`；新增 `POST /system/export/attachment1/batch`；批量 SQL、按教师分组、多 Sheet/ZIP、阈值、失败清单、权限和跨页选择都有独立验收。
- 需求 14：任务 6–9。覆盖专任绩效、外聘、540 封顶、无费率、A–G、停用酬金、幂等、金额舍入、并发状态复核及附件2交叉验收。
- 不引入异步导出任务和 Quartz；同步阈值足以约束本期资源使用，超过绝对上限直接拒绝。
- 不改现有单人接口路径、参数、权限串和文件版式；批量接口复用相同的 39 列模板渲染函数。

**新增文件：**
- `rear/workload-system/src/main/java/com/workload/system/domain/dto/Attachment1BatchRequest.java`：批量请求契约与格式枚举。
- `rear/workload-system/src/main/java/com/workload/system/domain/dto/Attachment1BatchErrorDTO.java`：失败清单行。
- `rear/workload-system/src/main/java/com/workload/system/service/Attachment1ExportService.java`：单人/批量附件1输出边界。
- `rear/workload-system/src/main/java/com/workload/system/service/impl/Attachment1ExportServiceImpl.java`：模板渲染、多 Sheet、ZIP、阈值与文件名处理。
- `rear/workload-system/src/test/java/com/workload/system/service/impl/Attachment1ExportServiceImplTest.java`：输出格式、失败隔离与阈值测试。
- `front/RuoYi-Vue3/src/utils/crossPageSelection.js`：与页面无关的跨页选择集合逻辑。
- `front/RuoYi-Vue3/tests/crossPageSelection.test.mjs`：使用 Node 内建测试运行器验证跨页选择。
- `rear/workload-system/src/test/java/com/workload/system/calc/allowance/AllowanceStrategyAcceptanceTest.java`：A–G 参数化验收。
- `rear/workload-system/src/test/java/com/workload/system/calc/PayAcceptanceFixture.java`：教师、汇总、酬金输入构造器与固定期望值。
- `rear/workload-system/src/test/resources/fixtures/pay_acceptance_fixture.sql`：本地数据库/API/附件2验收数据。
- `rear/workload-system/src/test/resources/fixtures/pay_acceptance_cleanup.sql`：仅按夹具固定 ID 清理。

**修改文件：**
- `rear/workload-system/src/main/java/com/workload/system/controller/BizExportController.java`：委托导出服务并暴露批量 POST。
- `rear/workload-system/src/main/java/com/workload/system/mapper/BizExportMapper.java`：批量查询签名。
- `rear/workload-system/src/main/resources/mapper/system/BizExportMapper.xml`：`IN userIds` 的两条批量查询。
- `rear/workload-system/src/test/java/com/workload/system/controller/BizExportControllerTest.java`：接口、权限范围和兼容测试。
- `rear/workload-admin/src/main/resources/application.yml`：批量导出三项后端阈值。
- `front/RuoYi-Vue3/src/api/system/export.js`：批量 POST blob API。
- `front/RuoYi-Vue3/src/views/system/workloadSummary/index.vue`：跨页选择、格式确认、失败说明与下载。
- `front/RuoYi-Vue3/package.json`：增加无第三方依赖的 `test:unit` 命令。
- `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadSummaryMapper.java`：写前锁定复核签名。
- `rear/workload-system/src/main/resources/mapper/system/BizWorkloadSummaryMapper.xml`：`FOR UPDATE` 查询。
- `rear/workload-system/src/main/java/com/workload/system/calc/PayCalcServiceImpl.java`：写前状态复核。
- `rear/workload-system/src/test/java/com/workload/system/calc/PayCalcServiceImplTest.java`：幂等、舍入、过滤及状态竞态。
- `rear/workload-system/src/test/java/com/workload/system/calc/SummaryCalcServiceImplTest.java`：绩效、人员性质、封顶和无费率。

### Task 1: 锁定批量附件1契约、阈值和数据范围

**Files:**
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/Attachment1BatchRequest.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/domain/dto/Attachment1BatchErrorDTO.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/controller/BizExportController.java:103-153`
- Modify: `rear/workload-admin/src/main/resources/application.yml:157-169`
- Test: `rear/workload-system/src/test/java/com/workload/system/controller/BizExportControllerTest.java`

- [ ] **Step 1: 写批量端点失败测试**

在 `BizExportControllerTest` 增加服务 Mock，验证空学期、空 ID、教师角色批量请求和超量请求。教师角色调用批量端点不得“替换为本人后继续”，而应明确拒绝；批量导出属于管理操作，避免持有 `system:export:personal` 的教师借 body 探测他人是否存在。

```java
@Test
void batchAttachment1RejectsTeacherRole() {
    Attachment1BatchRequest request = new Attachment1BatchRequest();
    request.setSemester("2026-2027-1");
    request.setUserIds(List.of(2001L, 2002L));
    request.setFormat("xlsx");
    try (MockedStatic<DataScopeUtil> scope = mockStatic(DataScopeUtil.class)) {
        scope.when(DataScopeUtil::isTeacherOnly).thenReturn(true);
        assertThatThrownBy(() -> controller.exportAttachment1Batch(request,
                new MockHttpServletResponse()))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("教师角色不允许批量导出");
    }
}

@Test
void batchAttachment1DelegatesDistinctUsers() throws Exception {
    Attachment1BatchRequest request = new Attachment1BatchRequest();
    request.setSemester("2026-2027-1");
    request.setUserIds(List.of(2002L, 2001L, 2002L));
    request.setFormat("xlsx");
    controller.exportAttachment1Batch(request, new MockHttpServletResponse());
    verify(attachment1ExportService).writeBatch("2026-2027-1",
            List.of(2002L, 2001L), Attachment1BatchRequest.Format.XLSX, any());
}
```

- [ ] **Step 2: 运行 Controller 测试并确认红灯**

Run（PowerShell）：

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" -f rear\pom.xml -pl workload-system -Dtest=BizExportControllerTest test
```

Expected: `COMPILATION ERROR`，提示 `Attachment1BatchRequest` 或 `exportAttachment1Batch` 不存在。

- [ ] **Step 3: 新增请求和错误 DTO**

```java
public class Attachment1BatchRequest {
    public enum Format { XLSX, ZIP }
    private String semester;
    private List<Long> userIds;
    private String format;
    public Format normalizedFormat() {
        try { return Format.valueOf(format == null ? "XLSX" : format.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new ServiceException("format 仅支持 xlsx 或 zip"); }
    }
    // 标准 getter/setter
}

public class Attachment1BatchErrorDTO {
    private Long userId;
    private String userLabel;
    private String reason;
    public Attachment1BatchErrorDTO(Long userId, String userLabel, String reason) { /* 逐字段赋值 */ }
    // 标准 getter
}
```

请求体固定为：

```json
{"semester":"2026-2027-1","userIds":[2001,2002],"format":"xlsx"}
```

响应固定为文件流。`xlsx` 包含成功教师 Sheet 和 `_失败清单` Sheet；`zip` 包含每位成功教师的 xlsx 与根目录 `失败清单.xlsx`。全部失败仍返回对应容器及失败清单，不返回看似成功的空文件。

- [ ] **Step 4: 增加 POST 端点与后端阈值配置**

```java
@PreAuthorize("@ss.hasPermi('system:export:personal')")
@Log(title = "批量导出表一", businessType = BusinessType.EXPORT)
@PostMapping("/attachment1/batch")
public void exportAttachment1Batch(@RequestBody Attachment1BatchRequest request,
        HttpServletResponse response) throws Exception {
    if (DataScopeUtil.isTeacherOnly()) {
        throw new ServiceException("教师角色不允许批量导出，请使用单教师导出");
    }
    String semester = request == null ? null : StringUtils.trim(request.getSemester());
    if (StringUtils.isEmpty(semester)) throw new ServiceException("semester 不能为空");
    List<Long> ids = request.getUserIds() == null ? List.of()
            : request.getUserIds().stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) throw new ServiceException("至少选择 1 位教师");
    attachment1ExportService.writeBatch(semester, ids, request.normalizedFormat(), response);
}
```

在 `application.yml` 的 `wfit` 下加入固定默认值：

```yaml
  export:
    attachment1:
      workbook-max-teachers: 20
      zip-max-teachers: 100
      max-rows-per-teacher: 5000
```

语义：`xlsx` 最多 20 人；`zip` 最多 100 人；任一教师超过 5000 条任务行时只把该教师写入失败清单。绝不由前端自行决定安全上限。

- [ ] **Step 5: 测试通过并提交**

Run: 同 Step 2。

Expected: `Tests run: 4` 或更多，`Failures: 0, Errors: 0`，`BUILD SUCCESS`。

```bash
git add rear/workload-system/src/main/java/com/workload/system/domain/dto/Attachment1BatchRequest.java rear/workload-system/src/main/java/com/workload/system/domain/dto/Attachment1BatchErrorDTO.java rear/workload-system/src/main/java/com/workload/system/controller/BizExportController.java rear/workload-system/src/test/java/com/workload/system/controller/BizExportControllerTest.java rear/workload-admin/src/main/resources/application.yml
git commit -m "feat: define batch attachment export contract"
```

### Task 2: 一次批量查询并按教师分组

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizExportMapper.java:21-68`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizExportMapper.xml:87-170`
- Test: `rear/workload-system/src/test/java/com/workload/system/service/impl/Attachment1ExportServiceImplTest.java`

- [ ] **Step 1: 写查询调用次数测试**

```java
@Test
void batchLoadsRowsAndTeachersInTwoQueries() throws Exception {
    when(mapper.selectAttachment1RowsBatch(List.of(2L, 1L), SEMESTER))
        .thenReturn(List.of(row(1L, "课程A"), row(2L, "课程B")));
    when(mapper.selectAttachment1TeachersBatch(List.of(2L, 1L), SEMESTER))
        .thenReturn(List.of(teacher(1L, "甲"), teacher(2L, "乙")));
    service.prepareBatch(SEMESTER, List.of(2L, 1L));
    verify(mapper, times(1)).selectAttachment1RowsBatch(anyList(), eq(SEMESTER));
    verify(mapper, times(1)).selectAttachment1TeachersBatch(anyList(), eq(SEMESTER));
    verify(mapper, never()).selectAttachment1Rows(anyLong(), anyString());
}
```

为支持分组，在现有两个 DTO 增加内部定位字段 `userId`；该字段不对应模板列，只用于批量结果 `groupingBy`。

- [ ] **Step 2: 运行测试并确认缺少批量 Mapper 方法**

Run:

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" -f rear\pom.xml -pl workload-system -Dtest=Attachment1ExportServiceImplTest test
```

Expected: 编译失败，提示 `selectAttachment1RowsBatch`/`selectAttachment1TeachersBatch` 未定义。

- [ ] **Step 3: 增加批量 Mapper 签名**

```java
List<Attachment1RowDTO> selectAttachment1RowsBatch(
    @Param("userIds") List<Long> userIds, @Param("semester") String semester);
List<Attachment1TeacherDTO> selectAttachment1TeachersBatch(
    @Param("userIds") List<Long> userIds, @Param("semester") String semester);
```

- [ ] **Step 4: 增加两条批量 SQL**

复制现有两条单人查询的列集，行查询额外选择 `i.user_id AS userId`，教师查询额外选择 `s.user_id AS userId`。条件严格写为：

```xml
where i.semester = #{semester}
  and i.user_id in
  <foreach collection="userIds" item="userId" open="(" separator="," close=")">
      #{userId}
  </foreach>
  and i.item_type in ('G1','G2','G3','G4','G5','G6')
  and (i.status is null or i.status != 3)
group by i.user_id, ifnull(i.task_id, -i.id)
order by i.user_id, min(i.id)
```

教师查询使用：

```xml
where s.semester = #{semester}
  and s.user_id in
  <foreach collection="userIds" item="userId" open="(" separator="," close=")">
      #{userId}
  </foreach>
order by s.user_id
```

服务端以请求 ID 顺序构造 `LinkedHashMap<Long, TeacherExportData>`，不要依赖 SQL 排序；每位教师恰好归入自己的数据组。SQL 参数只使用 MyBatis 绑定，禁止拼接 ID 字符串。

- [ ] **Step 5: 测试通过并提交**

Run: 同 Step 2。

Expected: `Failures: 0, Errors: 0`，`BUILD SUCCESS`。

```bash
git add rear/workload-system/src/main/java/com/workload/system/mapper/BizExportMapper.java rear/workload-system/src/main/resources/mapper/system/BizExportMapper.xml rear/workload-system/src/main/java/com/workload/system/domain/dto/Attachment1RowDTO.java rear/workload-system/src/main/java/com/workload/system/domain/dto/Attachment1TeacherDTO.java rear/workload-system/src/test/java/com/workload/system/service/impl/Attachment1ExportServiceImplTest.java
git commit -m "perf: batch attachment export queries"
```

### Task 3: 抽取模板渲染并实现流式多 Sheet/ZIP

**Files:**
- Create: `rear/workload-system/src/main/java/com/workload/system/service/Attachment1ExportService.java`
- Create: `rear/workload-system/src/main/java/com/workload/system/service/impl/Attachment1ExportServiceImpl.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/controller/BizExportController.java:97-325`
- Test: `rear/workload-system/src/test/java/com/workload/system/service/impl/Attachment1ExportServiceImplTest.java`
- Test: `rear/workload-system/src/test/java/com/workload/system/controller/BizExportControllerTest.java`

- [ ] **Step 1: 写 XLSX/ZIP 和失败隔离测试**

```java
@Test
void xlsxContainsOneSheetPerSuccessAndErrorSheet() throws Exception {
    stubTeacher(1L, "同名教师", true, true);
    stubTeacher(2L, "同名教师", false, true); // 无明细
    MockHttpServletResponse response = new MockHttpServletResponse();
    service.writeBatch(SEMESTER, List.of(1L, 2L), Format.XLSX, response);
    try (Workbook wb = WorkbookFactory.create(
            new ByteArrayInputStream(response.getContentAsByteArray()))) {
        assertThat(wb.getSheet("同名教师(1)")).isNotNull();
        assertThat(wb.getSheet("_失败清单").getRow(1).getCell(2).getStringCellValue())
            .contains("无工作量明细");
    }
}

@Test
void zipStreamsEachWorkbookAndErrorsEntry() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    service.writeBatch(SEMESTER, List.of(1L, 2L), Format.ZIP, response);
    try (ZipInputStream zip = new ZipInputStream(
            new ByteArrayInputStream(response.getContentAsByteArray()))) {
        assertThat(zipEntryNames(zip)).contains("表一_同名教师(1)_2026-2027-1.xlsx",
                "失败清单.xlsx");
    }
}

@Test
void oversizeTeacherBecomesFailureWithoutDroppingOtherTeachers() {
    when(maxRowsPerTeacher).thenReturn(2);
    // user 1 返回 3 行，user 2 返回 1 行
    // 断言 user 2 文件存在、user 1 进入失败清单
}
```

- [ ] **Step 2: 运行测试并确认服务不存在**

Run:

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" -f rear\pom.xml -pl workload-system -Dtest=Attachment1ExportServiceImplTest,BizExportControllerTest test
```

Expected: 编译失败，提示 `Attachment1ExportServiceImpl` 不存在。

- [ ] **Step 3: 抽取单人渲染，不改变 GET**

服务接口固定为：

```java
public interface Attachment1ExportService {
    void writeSingle(Long userId, String semester, HttpServletResponse response) throws Exception;
    void writeBatch(String semester, List<Long> userIds,
                    Attachment1BatchRequest.Format format,
                    HttpServletResponse response) throws Exception;
}
```

把 Controller 当前 `ATT1_*` 常量、`fillAttachment1`、`fillAttachment1Row`、`setText`、`setNum`、`isRowBlank` 移入实现类。`writeSingle` 继续：无明细返回 UTF-8 文本；有明细但无汇总抛“先核算汇总”；文件名仍为 `表一_<姓名(工号)>_<学期>.xlsx`。Controller 的 GET 只做参数校验、`DataScopeUtil.resolveUserId` 与服务委托，现有 `BizExportControllerTest` 的 39 列、G4/G6 与固定块断言必须原样通过。

- [ ] **Step 4: 实现多 Sheet 工作簿**

```java
try (XSSFWorkbook out = new XSSFWorkbook()) {
    for (TeacherExportData data : prepared.successes()) {
        try (InputStream is = template().getInputStream(); XSSFWorkbook one = new XSSFWorkbook(is)) {
            fillAttachment1(one.getSheetAt(0), data.teacher(), data.rows(), semester);
            copySheet(one.getSheetAt(0), out, uniqueSheetName(data, usedNames));
        } catch (Exception ex) {
            errors.add(error(data.userId(), safeMessage(ex)));
        }
    }
    appendErrorSheet(out, errors);
    out.write(response.getOutputStream());
}
```

`uniqueSheetName` 必须替换 `[]:*?/\\`、截断到 31 字符，并在重名时追加 `(userId)`；`copySheet` 要复制行高、列宽、单元格值/公式/样式、合并单元格和打印设置。失败原因只暴露 `ServiceException` 消息；其他异常写“生成失败，请联系管理员”，完整堆栈只进日志。

- [ ] **Step 5: 实现逐 entry ZIP**

```java
response.setContentType("application/zip");
try (ZipOutputStream zip = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
    for (TeacherExportData data : prepared.successes()) {
        try (XSSFWorkbook wb = renderOne(data, semester);
             ByteArrayOutputStream one = new ByteArrayOutputStream(256 * 1024)) {
            wb.write(one);
            zip.putNextEntry(new ZipEntry(uniqueFileName(data, usedNames)));
            one.writeTo(zip);
            zip.closeEntry();
        } catch (Exception ex) {
            errors.add(error(data.userId(), safeMessage(ex)));
        }
    }
    if (!errors.isEmpty()) writeErrorsEntry(zip, errors);
    zip.finish();
}
```

内存边界是一位教师的工作簿，而不是整批 ZIP；每个循环关闭 POI 工作簿和临时字节流。若单教师接近 5000 行仍超出可接受内存，后续单独切 `SXSSFWorkbook`，本期不同时改模板复制机制。

- [ ] **Step 6: 阈值和响应头验收**

测试并实现：`xlsx` 21 人时报“xlsx 最多 20 位教师，请改用 zip”；`zip` 101 人时报“单次最多 100 位教师”；响应文件名分别为 `表一批量_<semester>_<count>人.xlsx|zip`。校验必须发生在写响应头/字节之前。

- [ ] **Step 7: 运行测试并提交**

Run: 同 Step 2。

Expected: 所有 Controller/Service 测试 `Failures: 0, Errors: 0`。

```bash
git add rear/workload-system/src/main/java/com/workload/system/service/Attachment1ExportService.java rear/workload-system/src/main/java/com/workload/system/service/impl/Attachment1ExportServiceImpl.java rear/workload-system/src/main/java/com/workload/system/controller/BizExportController.java rear/workload-system/src/test/java/com/workload/system/service/impl/Attachment1ExportServiceImplTest.java rear/workload-system/src/test/java/com/workload/system/controller/BizExportControllerTest.java
git commit -m "feat: stream batch attachment workbooks"
```

### Task 4: 前端跨页选择与批量下载

**Files:**
- Create: `front/RuoYi-Vue3/src/utils/crossPageSelection.js`
- Create: `front/RuoYi-Vue3/tests/crossPageSelection.test.mjs`
- Modify: `front/RuoYi-Vue3/src/api/system/export.js:1-31`
- Modify: `front/RuoYi-Vue3/src/views/system/workloadSummary/index.vue:64-264,746-1012`
- Modify: `front/RuoYi-Vue3/package.json:8-15`

- [ ] **Step 1: 写跨页选择失败测试**

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import { mergePageSelection, selectedIds } from '../src/utils/crossPageSelection.js'

test('换页只替换当前页选择并保留其他页', () => {
  let selected = new Map([[1, { userId: 1 }], [3, { userId: 3 }]])
  selected = mergePageSelection(selected, [{ userId: 1 }, { userId: 2 }], [{ userId: 2 }])
  assert.deepEqual(selectedIds(selected), [3, 2])
})

test('筛选范围变化时由调用方清空', () => {
  const selected = mergePageSelection(new Map(), [{ userId: 1 }], [{ userId: 1 }])
  selected.clear()
  assert.deepEqual(selectedIds(selected), [])
})
```

- [ ] **Step 2: 运行并确认模块不存在**

Run:

```powershell
node --test front\RuoYi-Vue3\tests\crossPageSelection.test.mjs
```

Expected: `ERR_MODULE_NOT_FOUND`。

- [ ] **Step 3: 实现纯函数并加入脚本**

```js
export function mergePageSelection(existing, pageRows, selectedRows) {
  const next = new Map(existing)
  pageRows.forEach(row => next.delete(row.userId))
  selectedRows.forEach(row => row.userId && next.set(row.userId, row))
  return next
}
export const selectedIds = selected => [...selected.keys()]
```

`package.json` 增加：

```json
"test:unit": "node --test tests/*.test.mjs"
```

- [ ] **Step 4: 添加 API 与 blob 下载类型**

```js
export function exportAttachment1Batch(data) {
  return request({
    url: '/system/export/attachment1/batch',
    method: 'post',
    data,
    responseType: 'blob'
  })
}
```

调用 `saveBlobAsFile` 时：XLSX 使用现有默认 MIME；ZIP 显式传 `application/zip`。

- [ ] **Step 5: 页面实现跨页选择**

表格添加 `row-key="userId"`，selection 列添加 `:reserve-selection="true"`，并保存 `selectedByUserId = ref(new Map())`。`handleSelectionChange` 用 `workloadSummaryList.value` 作为当前页、用工具函数合并；按钮文案显示全局数量。学期、教师、状态任一筛选变化及 `resetQuery` 时调用 `clearCrossPageSelection()`，禁止把不同学期/筛选范围混入一个批次。

- [ ] **Step 6: 增加格式确认和失败说明**

管理角色新增“批量导出表一”按钮，仅在已选至少一人时启用。点击后：1–20 人默认 XLSX，可切 ZIP；21–100 人只允许 ZIP；超过 100 人前端立即提示拆批，但后端仍做相同限制。确认文案包含学期、已选教师数、输出格式及“无明细/未汇总教师会写入失败清单”。下载名为 `表一批量_<semester>_<count>人.<ext>`；finally 关闭 loading，错误 blob 继续由 `saveBlobAsFile` 统一处理。

- [ ] **Step 7: 运行前端验证并提交**

Run:

```powershell
npm --prefix front\RuoYi-Vue3 run test:unit
npm --prefix front\RuoYi-Vue3 run lint
npm --prefix front\RuoYi-Vue3 run build:prod
```

Expected: Node 测试 `pass 2, fail 0`；ESLint 无 error；Vite 输出 `built in` 且退出码 0。

```bash
git add front/RuoYi-Vue3/src/utils/crossPageSelection.js front/RuoYi-Vue3/tests/crossPageSelection.test.mjs front/RuoYi-Vue3/src/api/system/export.js front/RuoYi-Vue3/src/views/system/workloadSummary/index.vue front/RuoYi-Vue3/package.json
git commit -m "feat: add cross-page batch export selection"
```

### Task 5: 批量导出安全、失败清单和审计回归

**Files:**
- Modify: `rear/workload-system/src/test/java/com/workload/system/controller/BizExportControllerTest.java`
- Modify: `rear/workload-system/src/test/java/com/workload/system/service/impl/Attachment1ExportServiceImplTest.java`
- Modify: `rear/workload-system/src/main/java/com/workload/system/controller/BizExportController.java`

- [ ] **Step 1: 增加安全与失败矩阵测试**

```java
@ParameterizedTest
@ValueSource(strings = {"", "pdf", "../../zip"})
void rejectsUnsupportedFormat(String format) { /* 断言 ServiceException */ }

@Test
void sanitizesZipEntryAndSheetNames() { /* 姓名含 /:*?[]\\ 时没有路径穿越且 Sheet 合法 */ }

@Test
void noSummaryAndNoRowsProduceDifferentReasons() {
    // rows 空 -> “无工作量明细”；teacher 空 -> “尚未核算汇总”
}

@Test
void singleGetStillScopesTeacherToSelf() {
    // mock resolveUserId(999L)=1003L，verify writeSingle(1003L,...)
}
```

- [ ] **Step 2: 运行测试确认至少一个红灯**

Run:

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" -f rear\pom.xml -pl workload-system -Dtest=Attachment1ExportServiceImplTest,BizExportControllerTest test
```

Expected: 新增非法文件名或失败原因断言失败。

- [ ] **Step 3: 完成输入和输出安全处理**

统一文件名清洗：移除控制字符，替换路径分隔符，不允许 `..` 成为 ZIP 路径段；Sheet 使用 `WorkbookUtil.createSafeSheetName` 后再做重名消歧。批量接口保留 `@Log(... EXPORT)`，但请求体日志不得包含工作簿字节；系统现有操作日志应记录操作人、端点、学期和教师数。

- [ ] **Step 4: 运行导出测试与模块测试**

Run:

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" -f rear\pom.xml -pl workload-system test
```

Expected: `Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

- [ ] **Step 5: 提交**

```bash
git add rear/workload-system/src/main/java/com/workload/system/controller/BizExportController.java rear/workload-system/src/test/java/com/workload/system/controller/BizExportControllerTest.java rear/workload-system/src/test/java/com/workload/system/service/impl/Attachment1ExportServiceImplTest.java
git commit -m "test: harden batch attachment export"
```

### Task 6: 固化 A–G 策略验收

**Files:**
- Create: `rear/workload-system/src/test/java/com/workload/system/calc/allowance/AllowanceStrategyAcceptanceTest.java`
- Modify: `rear/workload-system/src/test/java/com/workload/system/calc/allowance/AllowanceAStrategyTest.java`
- Modify: `rear/workload-system/src/test/java/com/workload/system/calc/allowance/AllowanceDStrategyTest.java`

- [ ] **Step 1: 写 A–G 参数化验收测试**

测试用真实策略类，不 Mock 计算结果；只 Mock `RuleParamService` 并令其返回传入 fallback，确保当前默认规则矩阵可复现。

```java
@ParameterizedTest(name = "{0} => {2}")
@MethodSource("cases")
void allowanceStrategiesMatchAcceptanceFixture(String type, BizAllowanceItem item,
                                                String expected) {
    AllowanceCalcStrategy strategy = strategies.get(type);
    assertThat(strategy.calculate(item)).isEqualByComparingTo(expected);
}

static Stream<Arguments> cases() {
    return Stream.of(
      args("A", item("A").subtype("自学").students(5), "120.00"),
      args("A", item("A").subtype("自学").students(20), "0.00"),
      args("B", item("B").subtype("分散").students(10), "100.00"),
      args("B", item("B").subtype("集中不跟班").students(10), "150.00"),
      args("C", item("C").students(2), "240.00"),
      args("D", item("D").students(60), "80.00"),
      args("E", item("E").hours("1.5"), "90.00"),
      args("F", item("F").days("2").classes(3), "450.00"),
      args("G", item("G").units("4"), "120.00")
    );
}
```

另对 D 添加边界 `19→0、20→30、59→30、60→80、119→80、120→100、199→100、200→150`；A 添加 `5→120、6→260、19→260、20→0`。

- [ ] **Step 2: 运行测试，确认夹具期望与实现一致**

Run:

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" -f rear\pom.xml -pl workload-system -Dtest=AllowanceStrategyAcceptanceTest,AllowanceAStrategyTest,AllowanceDStrategyTest test
```

Expected: 新测试首次因测试辅助构造器缺失而编译失败；完成辅助构造器后 `Failures: 0, Errors: 0`。若金额不符，先核对 `03_calc_rules.sql`，不要修改期望去迁就异常实现。

- [ ] **Step 3: 验证 Service 才负责落库金额**

扩展 `BizAllowanceItemServiceImplTest`，对 A–G 逐类 Mock 工厂返回对应真实策略，调用 `insertBizAllowanceItem` 后断言 mapper 收到已计算的 `amount` 和 `status` 输入；明确 `PayCalcServiceImpl` 只汇总已落库金额，不重复运行策略。

- [ ] **Step 4: 运行并提交**

Run:

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" -f rear\pom.xml -pl workload-system -Dtest=AllowanceStrategyAcceptanceTest,BizAllowanceItemServiceImplTest test
```

Expected: `Failures: 0, Errors: 0`。

```bash
git add rear/workload-system/src/test/java/com/workload/system/calc/allowance/AllowanceStrategyAcceptanceTest.java rear/workload-system/src/test/java/com/workload/system/calc/allowance/AllowanceAStrategyTest.java rear/workload-system/src/test/java/com/workload/system/calc/allowance/AllowanceDStrategyTest.java rear/workload-system/src/test/java/com/workload/system/service/impl/BizAllowanceItemServiceImplTest.java
git commit -m "test: cover allowance strategies A through G"
```

### Task 7: 固化绩效、外聘、540 封顶和无费率

**Files:**
- Create: `rear/workload-system/src/test/java/com/workload/system/calc/PayAcceptanceFixture.java`
- Modify: `rear/workload-system/src/test/java/com/workload/system/calc/SummaryCalcServiceImplTest.java`

- [ ] **Step 1: 建立固定教师场景**

```java
record SummaryCase(String name, String nature, String title, String total,
                   String rate, String expectedPay, int capped) {}
static Stream<SummaryCase> performanceCases() {
    return Stream.of(
      new SummaryCase("专任正常", "专任", "教授", "282.72", "70", "7190.40", 0),
      new SummaryCase("低于额定", "专任", "讲师", "150", "50", "0.00", 0),
      new SummaryCase("外聘", "外聘", "讲师", "982.33", "50", "0.00", 1),
      new SummaryCase("超过540", "专任", "教授", "600", "70", "25200.00", 1),
      new SummaryCase("恰好540", "专任", "教授", "540", "70", "25200.00", 0),
      new SummaryCase("无费率", "专任", "未定职称", "300", null, "0.00", 0)
    );
}
```

- [ ] **Step 2: 写 Summary 参数化测试**

每例让明细总和等于 `total`，档案返回 `nature/title`，费率 Mapper 按 `rate` 返回当前有效费率或空列表。断言 `performancePay`、`payRate`、`isCapped` 和封顶备注。无费率按当前明确口径验收：不报错、`payRate=null`、绩效 `0.00`；附件2后续显示 0 而不是伪造费率。

- [ ] **Step 3: 运行测试并确认红灯/绿灯证据**

Run:

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" -f rear\pom.xml -pl workload-system -Dtest=SummaryCalcServiceImplTest test
```

Expected: 首次至少因夹具方法缺失编译失败；实现夹具后所有场景通过。特别确认 `total=540` 的 `isCapped=0` 与当前 `>` 边界一致，`540.01` 可另加用例并期望 `isCapped=1`。

- [ ] **Step 4: 提交**

```bash
git add rear/workload-system/src/test/java/com/workload/system/calc/PayAcceptanceFixture.java rear/workload-system/src/test/java/com/workload/system/calc/SummaryCalcServiceImplTest.java
git commit -m "test: cover performance pay boundaries"
```

### Task 8: 验证酬金汇总、幂等、舍入并关闭写前状态竞态

**Files:**
- Modify: `rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadSummaryMapper.java:13-119`
- Modify: `rear/workload-system/src/main/resources/mapper/system/BizWorkloadSummaryMapper.xml:79-83`
- Modify: `rear/workload-system/src/main/java/com/workload/system/calc/PayCalcServiceImpl.java:44-115`
- Modify: `rear/workload-system/src/test/java/com/workload/system/calc/PayCalcServiceImplTest.java`

- [ ] **Step 1: 写汇总金额、停用过滤和舍入测试**

```java
@Test
void sumsOnlyEnabledAllowancesAndRoundsTotalHalfUp() {
    BizWorkloadSummary summary = summaryWithStatus(DRAFT);
    summary.setPerformancePay(new BigDecimal("7190.40"));
    when(summaryMapper.selectForPayUpdate(USER, SEMESTER)).thenReturn(summary);
    when(allowanceMapper.selectBizAllowanceItemList(argThat(q -> q.getStatus() == 1)))
        .thenReturn(List.of(amount("120.00"), amount("100.00"), amount("240.00"),
            amount("80.00"), amount("90.00"), amount("450.00"), amount("120.49")));
    BizPayRecord result = service.recalcPay(USER, SEMESTER);
    assertThat(result.getCourseHourPay()).isEqualByComparingTo("7190.40");
    assertThat(result.getOtherPayTotal()).isEqualByComparingTo("1200.49");
    assertThat(result.getTotalPay()).isEqualTo(8391L); // 8390.89 HALF_UP
}
```

另加 `.49` 与 `.50` 两个总额边界；Mock 返回列表只能包含 `status=1` 行，并验证查询对象确实带 `status=1`。

- [ ] **Step 2: 写重复重算幂等和唯一键竞态测试**

```java
@Test
void existingRecordIsUpdatedWithoutSecondInsert() {
    BizPayRecord existing = record(99L);
    when(payRecordMapper.selectBizPayRecordList(any())).thenReturn(List.of(existing));
    service.recalcPay(USER, SEMESTER);
    service.recalcPay(USER, SEMESTER);
    verify(payRecordMapper, never()).insertBizPayRecord(any());
    verify(payRecordMapper, times(2)).updateBizPayRecord(argThat(r -> r.getId().equals(99L)));
}

@Test
void duplicateInsertFallsBackToUpdateSameUniqueRecord() {
    when(payRecordMapper.selectBizPayRecordList(any()))
        .thenReturn(List.of(), List.of(record(99L)));
    doThrow(new DuplicateKeyException("uk_user_sem"))
        .when(payRecordMapper).insertBizPayRecord(any());
    assertThat(service.recalcPay(USER, SEMESTER).getId()).isEqualTo(99L);
    verify(payRecordMapper).updateBizPayRecord(any());
}
```

- [ ] **Step 3: 写“初查草稿、写前已待审”竞态测试**

```java
@Test
void rechecksSummaryStatusUnderLockBeforeWritingPayRecord() {
    BizWorkloadSummary pending = summaryWithStatus(PENDING_AUDIT);
    when(summaryMapper.selectForPayUpdate(USER, SEMESTER)).thenReturn(pending);
    assertThatThrownBy(() -> service.recalcPay(USER, SEMESTER))
        .isInstanceOf(ServiceException.class).hasMessageContaining("酬金已冻结");
    verify(payRecordMapper, never()).insertBizPayRecord(any());
    verify(payRecordMapper, never()).updateBizPayRecord(any());
}
```

此测试代表真实竞态：审批事务先获得/提交汇总状态，酬金事务必须在写 `pay_record` 前重新读到状态 1/2，不能依赖更早的非锁定查询结果。

- [ ] **Step 4: 运行并确认缺少锁定查询**

Run:

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" -f rear\pom.xml -pl workload-system -Dtest=PayCalcServiceImplTest test
```

Expected: 编译失败，提示 `selectForPayUpdate` 不存在。

- [ ] **Step 5: 增加写前锁定状态查询**

Mapper：

```java
BizWorkloadSummary selectForPayUpdate(@Param("userId") Long userId,
                                      @Param("semester") String semester);
```

XML：

```xml
<select id="selectForPayUpdate" resultMap="BizWorkloadSummaryResult">
  <include refid="selectBizWorkloadSummaryVo"/>
  where user_id = #{userId} and semester = #{semester}
  limit 1 for update
</select>
```

`recalcPay` 已处于 `@Transactional`；将取得教师档案后的 summary 查询替换成 `selectForPayUpdate`，随后立即复核 `WorkloadSummaryStatus.isWriteFrozen`，之后才查询其他酬金和 insert/update `pay_record`。锁定顺序固定为 summary → allowance → pay_record；审批只更新 summary，可避免逆序死锁。`assertAllowanceEditable` 保留普通查询，不让只读校验持有行锁。

- [ ] **Step 6: 运行并提交**

Run: 同 Step 4。

Expected: `PayCalcServiceImplTest` 全部通过，`insert/update` 在冻结测试中调用次数为 0。

```bash
git add rear/workload-system/src/main/java/com/workload/system/mapper/BizWorkloadSummaryMapper.java rear/workload-system/src/main/resources/mapper/system/BizWorkloadSummaryMapper.xml rear/workload-system/src/main/java/com/workload/system/calc/PayCalcServiceImpl.java rear/workload-system/src/test/java/com/workload/system/calc/PayCalcServiceImplTest.java
git commit -m "fix: recheck summary state before pay write"
```

### Task 9: 数据库/API/附件2酬金验收夹具

**Files:**
- Create: `rear/workload-system/src/test/resources/fixtures/pay_acceptance_fixture.sql`
- Create: `rear/workload-system/src/test/resources/fixtures/pay_acceptance_cleanup.sql`
- Modify: `rear/workload-system/src/test/java/com/workload/system/controller/BizExportControllerTest.java`

- [ ] **Step 1: 编写可重复 SQL 夹具**

固定学期 `2099-2100-1`，固定用户 ID `99001–99005`，避免覆盖 `04_biz_test_data.sql`。脚本第一段先按这些 ID 删除子表，再插入：

```sql
-- 99001 专任教授：282.72，绩效 7190.40，A-G 合计 1200
-- 99002 专任讲师：150，绩效 0，其他 30，总额 30
-- 99003 外聘讲师：982.33，绩效 0
-- 99004 专任教授：600，绩效按 540 封顶为 25200
-- 99005 专任“无费率职称”：300，pay_rate NULL，绩效 0
```

A–G 必须是 7 条独立 `biz_allowance_item`：A 120、B 100、C 240、D 80、E 90、F 450、G 120，总计 1200；另插入一条 `status=0, amount=9999` 验证不汇总。汇总全部为 `status=0`，以便调用 `recalcPay`。SQL 直接写策略算出的金额是 API 汇总验收输入；Task 6 已单独证明“输入字段→金额”。脚本末尾 SELECT 返回五位教师的预期列，便于人工对账。

- [ ] **Step 2: 编写精确清理脚本**

```sql
delete from biz_allowance_item where user_id between 99001 and 99005 and semester='2099-2100-1';
delete from biz_pay_record where user_id between 99001 and 99005 and semester='2099-2100-1';
delete from biz_workload_summary where user_id between 99001 and 99005 and semester='2099-2100-1';
delete from biz_teacher_profile where user_id between 99001 and 99005;
delete from sys_user_role where user_id between 99001 and 99005;
delete from sys_user where user_id between 99001 and 99005;
```

只清固定 ID 与固定学期，不使用全表删除，不清规则和费率。

- [ ] **Step 3: 补附件2同源测试**

在 `BizExportControllerTest` Mock `selectPaySummaryExport` 返回：已核算专任、外聘、封顶、无费率和 `totalPay=null` 五行；调用 `exportPaySummary` 后解析 xlsx，断言：外聘备注含“不计发”；封顶备注含 `540`；未核算行含“尚未核算”；金额列取 DTO/pay_record，不在 Controller 重算。

- [ ] **Step 4: 运行自动化测试**

Run:

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" -f rear\pom.xml -pl workload-system -Dtest=PayCalcServiceImplTest,SummaryCalcServiceImplTest,AllowanceStrategyAcceptanceTest,BizExportControllerTest test
```

Expected: `Failures: 0, Errors: 0`，`BUILD SUCCESS`。

- [ ] **Step 5: 在本地测试库执行夹具并走 API**

启动后端前设置仓库要求的 `.env`。使用 MySQL 客户端执行：

```powershell
mysql -u wfit -p wflg_workload < rear\workload-system\src\test\resources\fixtures\pay_acceptance_fixture.sql
```

以具备 `system:payRecord:edit` 和 `system:export:paySummary` 的测试管理员登录，依次调用：

```text
POST /system/calc/recalcPay?userId=99001&semester=2099-2100-1
POST /system/calc/recalcPay?userId=99002&semester=2099-2100-1
POST /system/calc/recalcPay?userId=99003&semester=2099-2100-1
POST /system/calc/recalcPay?userId=99004&semester=2099-2100-1
POST /system/calc/recalcPay?userId=99005&semester=2099-2100-1
GET  /system/payRecord/list?semester=2099-2100-1
GET  /system/export/paySummary?semester=2099-2100-1
```

Expected records：`99001=(7190.40,1200.00,8390)`；`99002=(0.00,30.00,30)`；`99003=(0.00,0.00,0)`；`99004=(25200.00,0.00,25200)`；`99005=(0.00,0.00,0)`。重复执行 99001 两次后，`biz_pay_record` 中 `(99001,'2099-2100-1')` 仍只有 1 行且金额相同。附件2含 5 行，并与列表金额逐行一致。

- [ ] **Step 6: 验证写前状态复核**

把 99001 汇总状态更新为 1 后再次调用 `recalcPay`。

Expected: HTTP 业务错误消息含“学期汇总已进入审批流程，酬金已冻结”；原 `pay_record` 的 `update_time` 和金额均不变。恢复/清理用固定清理脚本，不手工删其他数据。

- [ ] **Step 7: 清理并提交**

```powershell
mysql -u wfit -p wflg_workload < rear\workload-system\src\test\resources\fixtures\pay_acceptance_cleanup.sql
```

Expected: 查询固定学期和固定 ID 均返回 0 行。

```bash
git add rear/workload-system/src/test/resources/fixtures/pay_acceptance_fixture.sql rear/workload-system/src/test/resources/fixtures/pay_acceptance_cleanup.sql rear/workload-system/src/test/java/com/workload/system/controller/BizExportControllerTest.java
git commit -m "test: add pay acceptance fixture"
```

### Task 10: 全量回归和交付证据

**Files:**
- Modify only if validation exposes a defect in files listed above.

- [ ] **Step 1: 后端全模块测试**

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" -f rear\pom.xml -pl workload-system test
```

Expected: `Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

- [ ] **Step 2: 后端编译联动模块**

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" -f rear\pom.xml -pl workload-admin -am -DskipTests package
```

Expected: reactor 中 `workload-common/framework/system/admin` 均 `SUCCESS`。

- [ ] **Step 3: 前端测试、Lint 和生产构建**

```powershell
npm --prefix front\RuoYi-Vue3 run test:unit
npm --prefix front\RuoYi-Vue3 run lint
npm --prefix front\RuoYi-Vue3 run build:prod
```

Expected: 单测无失败；ESLint 无 error；`dist` 构建成功。

- [ ] **Step 4: 手工验收导出矩阵**

依次验证：单人 GET 仍下载 39 列表一；2 人 XLSX 有 2 个教师 Sheet；2 人 ZIP 有 2 个 xlsx；一人无明细时成功文件仍存在且失败清单准确；同名教师无 Sheet/文件覆盖；21 人 XLSX 被拒并提示 ZIP；101 人 ZIP 被拒；纯教师账号看不到批量按钮且直接 POST 被拒；管理账号可按跨页勾选导出，翻页后数量保持，改变学期后选择清空。

- [ ] **Step 5: 检查差异只包含预期文件**

```powershell
git status --short
git diff --check
git diff --stat main...HEAD
```

Expected: 无尾随空格；无 `.env`、导出文件、测试数据库转储、`node_modules` 或 `target` 被跟踪；仅本计划列出的生产代码、测试、配置和夹具变化。

- [ ] **Step 6: 最终提交**

仅当 Step 1–5 发现并修复了遗漏时创建此收尾提交；否则不制造空提交。

```bash
git add <仅本轮修复的精确文件路径>
git commit -m "test: verify batch export and pay acceptance"
```

## 最终验收清单

- 现有 `GET /system/export/attachment1` 的路径、参数、权限和 39 列模板不变。
- `POST /system/export/attachment1/batch` 只接受管理角色、非空学期、去重后的 1–100 个教师 ID 与 `xlsx|zip`。
- 后端限制为 XLSX 20 人、ZIP 100 人、单教师 5000 任务行；前端提示不能替代后端拒绝。
- 两条批量 SQL 一次取行、一次取教师，服务按 `userId` 分组；不执行 N×2 查询。
- XLSX/ZIP 均保留成功结果并提供结构化失败清单；无明细、未汇总、超行数、渲染异常原因可区分。
- ZIP 只在内存保留当前教师工作簿；文件/Sheet 名无路径穿越、非法字符或同名覆盖。
- 跨页选择保留其他页、当前页取消即时生效，筛选范围变化必清空。
- A–G 真实策略输入金额全部断言；`status=0` 其他酬金不汇总。
- 专任正常、低于额定、外聘、540/540.01/600、无费率均有确定断言。
- `pay_record` 的课程酬金和其他酬金保留 2 位，总额按 HALF_UP 取整数；重复重算只有一行。
- `recalcPay` 在写前使用 `FOR UPDATE` 复核汇总状态，状态 1/2 时绝不 insert/update。
- 数据库、酬金列表与附件2对五位固定夹具金额一致，清理脚本只删除固定测试 ID。

## 实施顺序与提交序列

1. `feat: define batch attachment export contract`
2. `perf: batch attachment export queries`
3. `feat: stream batch attachment workbooks`
4. `feat: add cross-page batch export selection`
5. `test: harden batch attachment export`
6. `test: cover allowance strategies A through G`
7. `test: cover performance pay boundaries`
8. `fix: recheck summary state before pay write`
9. `test: add pay acceptance fixture`
10. 可选 `test: verify batch export and pay acceptance`（仅用于修复最终验证暴露的问题）
