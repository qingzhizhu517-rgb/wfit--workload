package com.workload.system.service.impl;

import static org.mockito.ArgumentMatchers.same;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import com.workload.system.calc.rule.CalculationPolicyContext;
import com.workload.system.calc.rule.PolicyHash;
import java.util.List;
import java.util.stream.Stream;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workload.common.core.domain.entity.SysUser;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.calc.strategy.CalcStrategyFactory;
import com.workload.system.calc.strategy.WorkloadCalcStrategy;
import com.workload.system.domain.*;
import com.workload.system.domain.dto.TeachingTaskImportDTO;
import com.workload.system.domain.dto.TeachingTaskImportV1DTO;
import com.workload.system.domain.dto.importing.ImportConfirmRequest;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.mapper.*;
import com.workload.system.service.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TeachingTaskTwoStageInputContractTest
{
    private final ObjectMapper json = new ObjectMapper();
    private final TeachingTaskImportServiceImpl service = spy(new TeachingTaskImportServiceImpl());
    private final BizImportBatchMapper batches = mock(BizImportBatchMapper.class);
    private final BizImportRowMapper rows = mock(BizImportRowMapper.class);
    private final BizTeachingTaskMapper tasks = mock(BizTeachingTaskMapper.class);
    private final BizWorkloadItemMapper items = mock(BizWorkloadItemMapper.class);
    private final ISysUserService users = mock(ISysUserService.class);
    private final WorkloadWriteGuard writeGuard = mock(WorkloadWriteGuard.class);

    TeachingTaskTwoStageInputContractTest()
    {
        ReflectionTestUtils.setField(service, "importBatchMapper", batches);
        ReflectionTestUtils.setField(service, "importRowMapper", rows);
        ReflectionTestUtils.setField(service, "teachingTaskMapper", tasks);
        ReflectionTestUtils.setField(service, "workloadItemMapper", items);
        ReflectionTestUtils.setField(service, "sysUserService", users);
        ReflectionTestUtils.setField(service, "workloadWriteGuard", writeGuard);
        ReflectionTestUtils.setField(service, "importPreviewStore", mock(ImportPreviewStore.class));
        ReflectionTestUtils.setField(service, "importTokenHasher", new ImportTokenHasher());
        doAnswer(i -> {
            BizImportBatch batch = i.getArgument(0);
            batch.setId(50L);
            when(batches.selectBizImportBatchById(50L)).thenReturn(batch);
            return 1;
        }).when(batches).insertBizImportBatch(any());
        doReturn(service).when(service).self();
    }

    private static TeachingTaskImportDTO validRow(String type)
    {
        TeachingTaskImportDTO row = new TeachingTaskImportDTO();
        row.setSemester("2025-2026-1");
        row.setUserCode("T008");
        row.setCourseName("测试课程");
        row.setWorkloadType(type);
        row.setStudentCount(20);
        switch (type)
        {
            case "G1", "G2" -> row.setPlannedHours(new BigDecimal("48"));
            case "G3" -> row.setDays(new BigDecimal("5"));
            case "G4" -> row.setCredits(new BigDecimal("2"));
            case "G6" -> row.setWeeks(new BigDecimal("4"));
            default -> { }
        }
        return row;
    }

    static Stream<Arguments> invalidRows()
    {
        Stream.Builder<Arguments> cases = Stream.builder();
        for (String type : new String[]{"G1", "G2", "G3", "G4", "G5", "G6"})
        {
            for (int count : new int[]{0, -1})
            {
                TeachingTaskImportDTO row = validRow(type);
                row.setStudentCount(count);
                cases.add(Arguments.of(row, "人数"));
            }
        }
        for (String type : new String[]{"G4", "G5", "G6"})
        {
            TeachingTaskImportDTO row = validRow(type);
            row.setStudentCount(null);
            cases.add(Arguments.of(row, "人数"));
        }
        for (String type : new String[]{"G2", "G3", "G5"})
        {
            for (String value : new String[]{"0", "-1"})
            {
                TeachingTaskImportDTO row = validRow(type);
                row.setCourseCoefficient(new BigDecimal(value));
                cases.add(Arguments.of(row, "系数"));
            }
        }
        return cases.build();
    }

    @ParameterizedTest
    @MethodSource("invalidRows")
    void previewBlocksInvalidInputsWithoutBusinessWrites(TeachingTaskImportDTO input, String message)
    {
        var preview = service.preview(new ByteArrayInputStream(workbook(input)), "input.xlsx", "V2");
        assertThat(preview.blockedCount()).isEqualTo(1);
        assertThat(preview.readyCount()).isZero();
        ArgumentCaptor<BizImportRow> stored = ArgumentCaptor.forClass(BizImportRow.class);
        verify(rows).insertBizImportRow(stored.capture());
        assertThat(stored.getValue().getStatus()).isEqualTo("BLOCKED");
        assertThat(stored.getValue().getErrorMessage()).contains(message);
        verifyNoInteractions(users, tasks, items, writeGuard);
    }

    @ParameterizedTest
    @MethodSource("invalidRows")
    void confirmRevalidatesStagedInputsBeforeBusinessLookup(TeachingTaskImportDTO input, String message)
            throws Exception
    {
        BizImportBatch batch = new BizImportBatch();
        batch.setId(50L);
        batch.setBatchNo("IMP");
        when(batches.selectBizImportBatchById(50L)).thenReturn(batch);
        BizImportRow stored = new BizImportRow();
        stored.setRowNo(2);
        stored.setNormalizedJson(json.writeValueAsString(input));
        when(rows.selectByBatchIdAndStatus(50L, "READY")).thenReturn(List.of(stored));
        var result = service.confirm(new ImportConfirmRequest(50L, "token", false));
        assertThat(result.getFailCount()).isEqualTo(1);
        assertThat(stored.getStatus()).isEqualTo("FAILED");
        assertThat(stored.getErrorMessage()).contains(message);
        verifyNoInteractions(users, tasks, items, writeGuard);
    }

    @ParameterizedTest
    @CsvSource({"V1,G1", "V1,G2", "V2,G1", "V2,G2"})
    void previewAndConfirmUseSameDefaultsAndPreserveRawInput(String version, String type) throws Exception
    {
        TeachingTaskImportDTO input = validRow(type);
        input.setCourseNature("   ");
        input.setMajorCategory("   ");
        byte[] bytes;
        if ("V1".equals(version))
        {
            TeachingTaskImportV1DTO legacy = new TeachingTaskImportV1DTO();
            legacy.setSemester(input.getSemester());
            legacy.setUserCode(input.getUserCode());
            legacy.setCourseName(input.getCourseName());
            legacy.setWorkloadType(type);
            legacy.setBaseValue(input.getPlannedHours());
            legacy.setStudentCount(input.getStudentCount());
            legacy.setCourseNature("   ");
            legacy.setMajorCategory("   ");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            EasyExcel.write(out, TeachingTaskImportV1DTO.class).sheet("导入数据").doWrite(List.of(legacy));
            bytes = out.toByteArray();
        }
        else
        {
            bytes = workbook(input);
        }
        var preview = service.preview(new ByteArrayInputStream(bytes), "defaults.xlsx", version);
        assertThat(preview.readyCount()).isEqualTo(1);
        ArgumentCaptor<BizImportRow> stored = ArgumentCaptor.forClass(BizImportRow.class);
        verify(rows).insertBizImportRow(stored.capture());
        var normalized = json.readTree(stored.getValue().getNormalizedJson());
        var raw = json.readTree(stored.getValue().getRawJson());
        assertThat(normalized.path("courseNature").asText()).isEqualTo("必修");
        assertThat(normalized.path("majorCategory").asText()).isEqualTo("理工类");
        assertThat(raw.path("courseNature").asText()).doesNotContain("必修");
        assertThat(raw.path("majorCategory").asText()).doesNotContain("理工类");
        verifyNoInteractions(users, tasks, items, writeGuard);

        when(users.selectUserByUserName("T008")).thenReturn(new SysUser(8L));
        when(rows.selectByBatchIdAndStatus(50L, "READY")).thenReturn(List.of(stored.getValue()));
        RuleParamService rules = mock(RuleParamService.class);
        when(rules.get(anyString(), any(BigDecimal.class), org.mockito.ArgumentMatchers.eq(java.time.LocalDate.of(2026, 1, 31)))).thenAnswer(i -> i.getArgument(1));
        CalcStrategyFactory factory = mock(CalcStrategyFactory.class);
        WorkloadCalcStrategy strategy = mock(WorkloadCalcStrategy.class);
        when(factory.get(type)).thenReturn(strategy);
        when(strategy.calculate(any(), any(CalculationPolicyContext.class))).thenReturn(BigDecimal.ONE);
        IWorkloadFactorFormulaService formulas = mock(IWorkloadFactorFormulaService.class);
        when(formulas.buildFresh(any(), any(CalculationPolicyContext.class))).thenReturn(mock(FactorFormulaVo.class));
        WorkloadSnapshotService snapshots = mock(WorkloadSnapshotService.class);
        BizWorkloadCalcSnapshot snapshot = new BizWorkloadCalcSnapshot();
        snapshot.setCalculationVersion(1L);
        when(snapshots.capture(any(), any(), anyString())).thenReturn(snapshot);
        BizWlTheoryMapper theory = mock(BizWlTheoryMapper.class);
        BizWlPracticeMapper practice = mock(BizWlPracticeMapper.class);
        ReflectionTestUtils.setField(service, "wlTheoryMapper", theory);
        ReflectionTestUtils.setField(service, "wlPracticeMapper", practice);
        ReflectionTestUtils.setField(service, "ruleParamService", rules);
        ReflectionTestUtils.setField(service, "calcStrategyFactory", factory);
        ReflectionTestUtils.setField(service, "factorFormulaService", formulas);
        ReflectionTestUtils.setField(service, "snapshotService", snapshots);
        var result = service.confirm(new ImportConfirmRequest(50L, preview.confirmToken(), false));
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(stored.getValue().getStatus()).isEqualTo("IMPORTED");
        ArgumentCaptor<BizTeachingTask> task = ArgumentCaptor.forClass(BizTeachingTask.class);
        verify(tasks).insertBizTeachingTask(task.capture());
        assertThat(task.getValue().getCourseNature()).isEqualTo(normalized.path("courseNature").asText());
        assertThat(task.getValue().getMajorCategory()).isEqualTo(normalized.path("majorCategory").asText());
        assertThat(task.getValue().getInputPlannedHours()).isEqualByComparingTo("48");
        if ("G1".equals(type))
        {
            ArgumentCaptor<BizWlTheory> detail = ArgumentCaptor.forClass(BizWlTheory.class);
            verify(theory).insertBizWlTheory(detail.capture());
            assertThat(detail.getValue().getK1()).isEqualByComparingTo("1.1");
        }
        else
        {
            ArgumentCaptor<BizWlPractice> detail = ArgumentCaptor.forClass(BizWlPractice.class);
            verify(practice).insertBizWlPractice(detail.capture());
            assertThat(detail.getValue().getK()).isEqualByComparingTo("1.0");
        }
        ArgumentCaptor<CalculationPolicyContext> context=ArgumentCaptor.forClass(CalculationPolicyContext.class);
        verify(strategy).calculate(any(), context.capture());
        verify(strategy).afterCalculated(any(), eq(BigDecimal.ONE), same(context.getValue()));
        verify(formulas).buildFresh(any(), same(context.getValue()));
        assertThat(context.getValue().asOfDate()).isEqualTo(java.time.LocalDate.of(2026,1,31));
        verify(snapshots).capture(any(), any(), eq(PolicyHash.hash(List.of())));
        var order = inOrder(snapshots, items, writeGuard);
        order.verify(snapshots).capture(any(), any(), eq(PolicyHash.hash(List.of())));
        order.verify(items).updateBizWorkloadItem(any());
        order.verify(writeGuard).markChanged(8L, "2025-2026-1");
    }

    private byte[] workbook(TeachingTaskImportDTO input)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out, TeachingTaskImportDTO.class).sheet("导入数据").doWrite(List.of(input));
        return out.toByteArray();
    }
}
