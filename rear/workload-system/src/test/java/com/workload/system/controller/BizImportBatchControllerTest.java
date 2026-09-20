package com.workload.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import com.workload.common.exception.ServiceException;
import com.workload.common.utils.PageUtils;
import com.workload.system.domain.BizImportBatch;
import com.workload.system.domain.dto.importing.ImportRowView;
import com.workload.system.service.IBizImportBatchService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 导入批次查询/错误行下载 Controller 契约测试。
 *
 * 覆盖：错误行导出只含 6 列白名单（绝不含身份证/住址等敏感原始列）、行查询分页契约、
 * 批次不存在时导出返回 404、行查询抛业务异常。
 */
@ExtendWith(MockitoExtension.class)
class BizImportBatchControllerTest
{
    @Mock
    private IBizImportBatchService bizImportBatchService;

    @InjectMocks
    private BizImportBatchController controller;

    @Test
    void exportsOnlySafeNormalizedColumnsAndError() throws Exception
    {
        when(bizImportBatchService.selectBizImportBatchById(17L)).thenReturn(batch(17L));
        when(bizImportBatchService.selectErrorRows(17L)).thenReturn(List.of(
                new ImportRowView(61, "WFIT1882", "张某", "BLOCKED", "DUPLICATE_USER_CODE", "工号重复")));

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportErrors(17L, response);

        assertThat(response.getContentAsByteArray()).isNotEmpty();
        assertWorkbookHeaders(response, "行号", "教师工号", "教师姓名", "状态", "错误码", "错误信息");
        assertFirstDataRow(response, "61", "WFIT1882", "张某", "BLOCKED", "DUPLICATE_USER_CODE", "工号重复");
    }

    @Test
    void rowsReturnsPagedTableDataFilteredByStatus()
    {
        when(bizImportBatchService.selectBizImportBatchById(17L)).thenReturn(batch(17L));
        List<ImportRowView> views = List.of(
                new ImportRowView(61, "WFIT1882", "张某", "BLOCKED", "DUPLICATE_USER_CODE", "工号重复"),
                new ImportRowView(87, "WFIT1883", "李某", "BLOCKED", "UNKNOWN_DEPT", "部门未知"));
        when(bizImportBatchService.selectRowViews(17L, "BLOCKED")).thenReturn(views);

        com.workload.common.core.page.TableDataInfo table;
        try (MockedStatic<PageUtils> pageUtils = mockStatic(PageUtils.class))
        {
            table = controller.rows(17L, "BLOCKED");
        }

        assertThat(table.getCode()).isEqualTo(200);
        assertThat(table.getRows()).isEqualTo(views);
        assertThat(table.getTotal()).isEqualTo(2L);
    }

    @Test
    void exportErrorsReturns404WhenBatchMissing() throws Exception
    {
        when(bizImportBatchService.selectBizImportBatchById(99L)).thenReturn(null);

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportErrors(99L, response);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void rowsRejectsMissingBatch()
    {
        when(bizImportBatchService.selectBizImportBatchById(99L)).thenReturn(null);

        assertThatThrownBy(() -> controller.rows(99L, null))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不存在");
    }

    private BizImportBatch batch(Long id)
    {
        BizImportBatch batch = new BizImportBatch();
        batch.setId(id);
        batch.setStatus(1);
        return batch;
    }

    private void assertWorkbookHeaders(MockHttpServletResponse response, String... expected) throws Exception
    {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray())))
        {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertThat(header.getLastCellNum()).isEqualTo((short) expected.length);
            for (int i = 0; i < expected.length; i++)
            {
                assertThat(header.getCell(i).getStringCellValue()).isEqualTo(expected[i]);
            }
        }
    }

    private void assertFirstDataRow(MockHttpServletResponse response, String... expected) throws Exception
    {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray())))
        {
            Row row = wb.getSheetAt(0).getRow(1);
            for (int i = 0; i < expected.length; i++)
            {
                assertThat(row.getCell(i).toString()).isEqualTo(expected[i]);
            }
        }
    }
}
