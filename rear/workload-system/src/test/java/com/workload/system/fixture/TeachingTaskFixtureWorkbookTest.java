package com.workload.system.fixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.workload.system.domain.BizImportRow;
import com.workload.system.domain.dto.importing.ImportPreviewResponse;
import com.workload.system.mapper.BizImportBatchMapper;
import com.workload.system.mapper.BizImportRowMapper;
import com.workload.system.mapper.BizTeachingTaskMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.service.ImportPreviewStore;
import com.workload.system.service.ImportTokenHasher;
import com.workload.system.service.impl.TeachingTaskImportServiceImpl;

/**
 * 教学任务 v2 两阶段导入的夹具工作簿测试：由测试代码构造「5 行说明 + 2 行数据」双 Sheet 工作簿，
 * 断言预检只统计数据页 2 行且业务表零写入（说明页示例绝不入库、不计入 readyCount）。
 *
 * <p>夹具由测试代码构造而非携带真实 xlsx 数据文件——生产模板示例位于说明 Sheet，
 * 二者都不得被当作真实导入数据。</p>
 */
class TeachingTaskFixtureWorkbookTest
{
    private static final String[] V2_HEADERS = {
            "学年学期", "教师工号", "教师姓名", "课程名称", "课程代码", "工作量类别",
            "授课层次", "专业大类", "课程性质", "课程级别", "课程角色", "教学评价", "选课/指导人数",
            "计划学时", "天数", "周数", "课程设计学分", "课程系数", "班级", "重复次序"};

    @Test
    void previewCountsOnlyDataSheetRowsWithoutBusinessWrites() throws Exception
    {
        TeachingTaskImportServiceImpl service = new TeachingTaskImportServiceImpl();
        BizImportBatchMapper batchMapper = mock(BizImportBatchMapper.class);
        BizImportRowMapper rowMapper = mock(BizImportRowMapper.class);
        BizTeachingTaskMapper taskMapper = mock(BizTeachingTaskMapper.class);
        BizWorkloadItemMapper itemMapper = mock(BizWorkloadItemMapper.class);
        ImportPreviewStore previewStore = mock(ImportPreviewStore.class);

        setField(service, "importBatchMapper", batchMapper);
        setField(service, "importRowMapper", rowMapper);
        setField(service, "teachingTaskMapper", taskMapper);
        setField(service, "workloadItemMapper", itemMapper);
        setField(service, "importPreviewStore", previewStore);
        setField(service, "importTokenHasher", new ImportTokenHasher());

        byte[] wb = fixtureWorkbook();
        ImportPreviewResponse result = service.preview(new ByteArrayInputStream(wb), "teaching-task-v2-smoke.xlsx", "V2");

        assertThat(result.readyCount()).isEqualTo(2);
        assertThat(result.totalCount()).isEqualTo(2);
        verify(rowMapper, times(2)).insertBizImportRow(any(BizImportRow.class));
        verify(taskMapper, never()).insertBizTeachingTask(any());
        verify(itemMapper, never()).insertBizWorkloadItem(any());
    }

    /** 说明页 5 行示例（G1/G3/G4/G5/G6）+ 数据页 2 行有效 G1 数据。 */
    private static byte[] fixtureWorkbook() throws Exception
    {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream())
        {
            Sheet instruction = wb.createSheet("示例与说明");
            writeHeader(instruction);
            String[] types = {"G1", "G3", "G4", "G5", "G6"};
            for (int i = 0; i < types.length; i++)
            {
                Row r = instruction.createRow(i + 1);
                r.createCell(0).setCellValue("2025-2026-2");
                r.createCell(2).setCellValue("示例教师");
                r.createCell(5).setCellValue(types[i]);
            }
            Sheet data = wb.createSheet("导入数据");
            writeHeader(data);
            for (int i = 1; i <= 2; i++)
            {
                Row r = data.createRow(i);
                r.createCell(0).setCellValue("2025-2026-1");
                r.createCell(1).setCellValue("T00" + i);
                r.createCell(2).setCellValue("教师" + i);
                r.createCell(3).setCellValue("高等数学");
                r.createCell(4).setCellValue("MATH101");
                r.createCell(5).setCellValue("G1");
                r.createCell(6).setCellValue("本科");
                r.createCell(7).setCellValue("理工类");
                r.createCell(8).setCellValue("必修");
                r.createCell(12).setCellValue(50);
                r.createCell(13).setCellValue(48);
                r.createCell(18).setCellValue("计算机240" + i);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void writeHeader(Sheet sheet)
    {
        Row header = sheet.createRow(0);
        for (int i = 0; i < V2_HEADERS.length; i++)
        {
            header.createCell(i).setCellValue(V2_HEADERS[i]);
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception
    {
        java.lang.reflect.Field field = TeachingTaskImportServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
