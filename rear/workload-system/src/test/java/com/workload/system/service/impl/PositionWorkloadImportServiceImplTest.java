package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.workload.common.core.domain.entity.SysUser;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.excel.ImportResult;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.domain.BizImportBatch;
import com.workload.system.domain.BizImportRow;
import com.workload.system.domain.BizRoleAssignment;
import com.workload.system.domain.dto.PositionWorkloadImportDTO;
import com.workload.system.domain.dto.importing.ImportConfirmRequest;
import com.workload.system.domain.dto.importing.ImportPreviewResponse;
import com.workload.system.mapper.BizImportBatchMapper;
import com.workload.system.mapper.BizImportRowMapper;
import com.workload.system.mapper.BizRoleAssignmentMapper;
import com.workload.system.service.ISysUserService;
import com.workload.system.service.ImportPreviewStore;
import com.workload.system.service.ImportTokenHasher;

/**
 * 学期岗位减免工作量导入服务测试。
 *
 * <p>覆盖：预检零业务写入、职务名称可空仍 READY、非法减免值阻断、确认按 row_no 回放、
 * 同批次重复行 SKIPPED、汇总冻结时该行 FAILED 且不写 biz_role_assignment。</p>
 */
class PositionWorkloadImportServiceImplTest
{
    // ==================== 预检：零业务写入 ====================

    /**
     * 预检只落 biz_import_row，教师+学期+减免值齐全的行 READY；职务名称为空仍 READY。
     * 绝不写 biz_role_assignment。
     */
    @Test
    void previewMarksValidRowsReadyAndNeverWritesRoleAssignment() throws Exception
    {
        PositionWorkloadImportServiceImpl service = new PositionWorkloadImportServiceImpl();
        BizImportBatchMapper batchMapper = mock(BizImportBatchMapper.class);
        BizImportRowMapper rowMapper = mock(BizImportRowMapper.class);
        BizRoleAssignmentMapper assignmentMapper = mock(BizRoleAssignmentMapper.class);
        ImportPreviewStore previewStore = mock(ImportPreviewStore.class);
        ImportTokenHasher tokenHasher = new ImportTokenHasher();

        setField(service, "importBatchMapper", batchMapper);
        setField(service, "importRowMapper", rowMapper);
        setField(service, "roleAssignmentMapper", assignmentMapper);
        setField(service, "importPreviewStore", previewStore);
        setField(service, "importTokenHasher", tokenHasher);

        // 第 1 行含职务，第 2 行职务为空
        byte[] workbook = workbook(
                row("T001", "张三", "2025-2026-1", "60", "教研室主任", "备注A"),
                row("T002", "李四", "2025-2026-1", "0", "", ""));

        ImportPreviewResponse result = service.preview(
                new ByteArrayInputStream(workbook), "position.xlsx", "V1");

        assertThat(result.readyCount()).isEqualTo(2);
        assertThat(result.blockedCount()).isZero();
        assertThat(result.importType()).isEqualTo("POSITION_WORKLOAD");
        verify(assignmentMapper, never()).insertBizRoleAssignment(any());
        verify(rowMapper, times(2)).insertBizImportRow(any(BizImportRow.class));
    }

    /** 减免值为负数即阻断，不影响其他合法行。 */
    @Test
    void previewBlocksNegativeWorkload() throws Exception
    {
        PositionWorkloadImportServiceImpl service = new PositionWorkloadImportServiceImpl();
        setField(service, "importBatchMapper", mock(BizImportBatchMapper.class));
        setField(service, "importRowMapper", mock(BizImportRowMapper.class));
        setField(service, "roleAssignmentMapper", mock(BizRoleAssignmentMapper.class));
        setField(service, "importPreviewStore", mock(ImportPreviewStore.class));
        setField(service, "importTokenHasher", new ImportTokenHasher());

        byte[] workbook = workbook(
                row("T001", "张三", "2025-2026-1", "-5", "教研室主任", ""));

        ImportPreviewResponse result = service.preview(
                new ByteArrayInputStream(workbook), "position.xlsx", "V1");

        assertThat(result.readyCount()).isZero();
        assertThat(result.blockedCount()).isEqualTo(1);
        assertThat(result.rows().get(0).errorMessage()).contains("减免");
    }

    // ==================== 确认：幂等 / 冻结 ====================

    /**
     * 确认按 row_no 回放：同批次同（工号,学期）重复行命中幂等键 → SKIPPED，
     * 新行走 processSingleRow 标记 IMPORTED，最后 markConfirmed。
     */
    @Test
    void confirmSkipsDuplicateWithinSameBatch() throws Exception
    {
        PositionWorkloadImportServiceImpl service = org.mockito.Mockito.spy(new PositionWorkloadImportServiceImpl());
        BizImportBatchMapper batchMapper = mock(BizImportBatchMapper.class);
        BizImportRowMapper rowMapper = mock(BizImportRowMapper.class);
        BizRoleAssignmentMapper assignmentMapper = mock(BizRoleAssignmentMapper.class);
        ImportPreviewStore previewStore = mock(ImportPreviewStore.class);
        ISysUserService sysUserService = mock(ISysUserService.class);

        setField(service, "importBatchMapper", batchMapper);
        setField(service, "importRowMapper", rowMapper);
        setField(service, "roleAssignmentMapper", assignmentMapper);
        setField(service, "importPreviewStore", previewStore);
        setField(service, "sysUserService", sysUserService);

        BizImportBatch batch = new BizImportBatch();
        batch.setId(50L);
        batch.setBatchNo("IMP-POS");
        when(batchMapper.selectBizImportBatchById(50L)).thenReturn(batch);

        BizImportRow r1 = importRow(1, normalizedJson("T001", "2025-2026-1", "60"));
        BizImportRow r2 = importRow(2, normalizedJson("T001", "2025-2026-1", "60"));
        when(rowMapper.selectByBatchIdAndStatus(50L, "READY")).thenReturn(java.util.List.of(r1, r2));

        SysUser u1 = new SysUser();
        u1.setUserId(1001L);
        when(sysUserService.selectUserByUserName("T001")).thenReturn(u1);
        // 幂等键按来源批次计数：同批次第 1 行 0（新）→ 写入后第 2 行 1（同批次重复）
        when(assignmentMapper.countBySourceBatch(eq(1001L), eq("2025-2026-1"), eq("IMP-POS")))
                .thenReturn(0, 1);

        org.mockito.Mockito.doReturn(service).when(service).self();
        org.mockito.Mockito.doNothing().when(service)
                .processSingleRow(any(PositionWorkloadImportDTO.class), eq("IMP-POS"));

        ImportResult result = service.confirm(new ImportConfirmRequest(50L, "tok", false));

        verify(service, times(1)).processSingleRow(any(PositionWorkloadImportDTO.class), eq("IMP-POS"));
        verify(previewStore).assertConfirmable(any());
        verify(previewStore).markConfirmed(50L);
        assertThat(result.getSkipCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(1);
    }

    /**
     * 汇总冻结（status 1/2）时该行确认失败：processSingleRow 内经 WorkloadWriteGuard 抛异常，
     * 该行 FAILED，绝不写 biz_role_assignment，批次置失败(4)。
     */
    @Test
    void confirmMarksRowFailedWhenSummaryFrozen() throws Exception
    {
        PositionWorkloadImportServiceImpl service = org.mockito.Mockito.spy(new PositionWorkloadImportServiceImpl());
        BizImportBatchMapper batchMapper = mock(BizImportBatchMapper.class);
        BizImportRowMapper rowMapper = mock(BizImportRowMapper.class);
        BizRoleAssignmentMapper assignmentMapper = mock(BizRoleAssignmentMapper.class);
        ImportPreviewStore previewStore = mock(ImportPreviewStore.class);
        ISysUserService sysUserService = mock(ISysUserService.class);
        WorkloadWriteGuard guard = mock(WorkloadWriteGuard.class);

        setField(service, "importBatchMapper", batchMapper);
        setField(service, "importRowMapper", rowMapper);
        setField(service, "roleAssignmentMapper", assignmentMapper);
        setField(service, "importPreviewStore", previewStore);
        setField(service, "sysUserService", sysUserService);
        setField(service, "workloadWriteGuard", guard);

        BizImportBatch batch = new BizImportBatch();
        batch.setId(60L);
        batch.setBatchNo("IMP-FROZEN");
        when(batchMapper.selectBizImportBatchById(60L)).thenReturn(batch);

        BizImportRow r1 = importRow(1, normalizedJson("T001", "2025-2026-1", "60"));
        when(rowMapper.selectByBatchIdAndStatus(60L, "READY")).thenReturn(java.util.List.of(r1));

        SysUser u1 = new SysUser();
        u1.setUserId(1001L);
        when(sysUserService.selectUserByUserName("T001")).thenReturn(u1);
        when(assignmentMapper.countBySourceBatch(eq(1001L), eq("2025-2026-1"), eq("IMP-FROZEN"))).thenReturn(0);
        // 冻结：guard 抛异常
        org.mockito.Mockito.doThrow(new ServiceException("学期汇总已进入审批流程，数据已冻结"))
                .when(guard).lockDraftOrAbsent(1001L, "2025-2026-1");

        org.mockito.Mockito.doReturn(service).when(service).self();

        ImportResult result = service.confirm(new ImportConfirmRequest(60L, "tok", false));

        assertThat(result.getFailCount()).isEqualTo(1);
        verify(assignmentMapper, never()).insertBizRoleAssignment(any());
        // 入口先认领(1→2)，回放有失败行再转失败(2→4)
        verify(previewStore).markConfirmed(60L);
        verify(previewStore).markFailed(60L);
    }

    /** updateSupport=true 未实现更新语义，confirm 入口即拒绝，不回放、不置终态。 */
    @Test
    void confirmRejectsUpdateSupport() throws Exception
    {
        PositionWorkloadImportServiceImpl service = org.mockito.Mockito.spy(new PositionWorkloadImportServiceImpl());
        BizImportRowMapper rowMapper = mock(BizImportRowMapper.class);
        ImportPreviewStore previewStore = mock(ImportPreviewStore.class);
        setField(service, "importRowMapper", rowMapper);
        setField(service, "importPreviewStore", previewStore);

        assertThatThrownBy(() -> service.confirm(new ImportConfirmRequest(70L, "tok", true)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("updateSupport");
        verify(rowMapper, never()).selectByBatchIdAndStatus(any(), any());
        verify(previewStore, never()).markConfirmed(any());
        verify(previewStore, never()).markFailed(any());
    }

    /**
     * #2 并发认领：confirm 入口原子认领批次(1→2)必须先于回放。认领失败（markConfirmed 抛并发冲突）时，
     * 绝不查询 READY 行、绝不回写批次计数——第二个并发请求因此不会重复 INSERT。
     */
    @Test
    void confirmClaimsBatchAtEntryBeforeReplayingRows() throws Exception
    {
        PositionWorkloadImportServiceImpl service = org.mockito.Mockito.spy(new PositionWorkloadImportServiceImpl());
        BizImportBatchMapper batchMapper = mock(BizImportBatchMapper.class);
        BizImportRowMapper rowMapper = mock(BizImportRowMapper.class);
        ImportPreviewStore previewStore = mock(ImportPreviewStore.class);
        setField(service, "importBatchMapper", batchMapper);
        setField(service, "importRowMapper", rowMapper);
        setField(service, "importPreviewStore", previewStore);

        BizImportBatch batch = new BizImportBatch();
        batch.setId(80L);
        batch.setBatchNo("IMP-CLAIM");
        when(batchMapper.selectBizImportBatchById(80L)).thenReturn(batch);
        when(rowMapper.selectByBatchIdAndStatus(80L, "READY")).thenReturn(java.util.List.of());
        org.mockito.Mockito.doThrow(new ServiceException("批次状态并发冲突，请刷新后重试"))
                .when(previewStore).markConfirmed(80L);

        assertThatThrownBy(() -> service.confirm(new ImportConfirmRequest(80L, "tok", false)))
                .isInstanceOf(ServiceException.class);

        verify(rowMapper, never()).selectByBatchIdAndStatus(any(), any());
        verify(batchMapper, never()).updateBizImportBatch(any());
    }

    // ==================== 辅助 ====================

    private static BizImportRow importRow(int rowNo, String normalizedJson)
    {
        BizImportRow row = new BizImportRow();
        row.setRowNo(rowNo);
        row.setStatus("READY");
        row.setNormalizedJson(normalizedJson);
        return row;
    }

    private static String normalizedJson(String userCode, String semester, String workload)
    {
        return "{\"userCode\":\"" + userCode + "\",\"semester\":\"" + semester
                + "\",\"positionWorkload\":" + workload + "}";
    }

    /** 一行数据：工号/姓名/学期/减免值/职务/备注。 */
    private static String[] row(String userCode, String userName, String semester,
            String workload, String positionName, String remark)
    {
        return new String[]{userCode, userName, semester, workload, positionName, remark};
    }

    /** 构造单 Sheet 岗位减免工作簿（表头 + 若干数据行）。 */
    private static byte[] workbook(String[]... rows) throws Exception
    {
        String[] headers = {"教师工号", "教师姓名", "学年学期", "岗位减免工作量（本学期）", "职务名称", "备注"};
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            Sheet sheet = wb.createSheet("导入数据");
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++)
            {
                header.createCell(i).setCellValue(headers[i]);
            }
            for (int i = 0; i < rows.length; i++)
            {
                Row r = sheet.createRow(i + 1);
                for (int c = 0; c < rows[i].length; c++)
                {
                    r.createCell(c).setCellValue(rows[i][c]);
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private void setField(Object target, String name, Object value) throws Exception
    {
        java.lang.reflect.Field field = PositionWorkloadImportServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
