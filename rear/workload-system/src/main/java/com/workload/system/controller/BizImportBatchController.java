package com.workload.system.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.workload.common.annotation.Log;
import com.workload.common.core.controller.BaseController;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.enums.BusinessType;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizImportBatch;
import com.workload.system.domain.dto.importing.ImportRowView;
import com.workload.system.service.IBizImportBatchService;
import com.workload.common.utils.poi.ExcelUtil;
import com.workload.common.core.page.TableDataInfo;

/**
 * 导入批次记录Controller
 * 
 * @author wflg
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/system/importBatch")
public class BizImportBatchController extends BaseController
{
    @Autowired
    private IBizImportBatchService bizImportBatchService;

    /**
     * 查询导入批次记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:importBatch:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizImportBatch bizImportBatch)
    {
        startPage();
        List<BizImportBatch> list = bizImportBatchService.selectBizImportBatchList(bizImportBatch);
        return getDataTable(list);
    }

    /**
     * 导出导入批次记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:importBatch:export')")
    @Log(title = "导入批次记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizImportBatch bizImportBatch)
    {
        List<BizImportBatch> list = bizImportBatchService.selectBizImportBatchList(bizImportBatch);
        ExcelUtil<BizImportBatch> util = new ExcelUtil<BizImportBatch>(BizImportBatch.class);
        util.exportExcel(response, list, "导入批次记录数据");
    }

    /**
     * 获取导入批次记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:importBatch:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bizImportBatchService.selectBizImportBatchById(id));
    }

    /**
     * 新增导入批次记录
     */
    @PreAuthorize("@ss.hasPermi('system:importBatch:add')")
    @Log(title = "导入批次记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizImportBatch bizImportBatch)
    {
        return toAjax(bizImportBatchService.insertBizImportBatch(bizImportBatch));
    }

    /**
     * 修改导入批次记录
     */
    @PreAuthorize("@ss.hasPermi('system:importBatch:edit')")
    @Log(title = "导入批次记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizImportBatch bizImportBatch)
    {
        return toAjax(bizImportBatchService.updateBizImportBatch(bizImportBatch));
    }

    /**
     * 删除导入批次记录
     */
    @PreAuthorize("@ss.hasPermi('system:importBatch:remove')")
    @Log(title = "导入批次记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bizImportBatchService.deleteBizImportBatchByIds(ids));
    }

    /** 错误清单表头（白名单）：与 {@link ImportRowView} 一一对应，绝不含身份证/住址等敏感原始列 */
    private static final String[] ERROR_HEADERS = {"行号", "教师工号", "教师姓名", "状态", "错误码", "错误信息"};

    /**
     * 分页查询批次暂存行（可按行状态过滤）。
     * <p>
     * 仅返回 {@link ImportRowView} 白名单字段（行号/工号/姓名/状态/错误码/错误信息），
     * 由 {@code normalized_json} 解析而来，不暴露原始敏感列。批次不存在时抛业务异常。
     * </p>
     *
     * @param batchId 批次ID
     * @param status  行状态过滤（READY/BLOCKED/IMPORTED/SKIPPED/FAILED，可空）
     */
    @PreAuthorize("@ss.hasPermi('system:importBatch:query')")
    @GetMapping("/{batchId}/rows")
    public TableDataInfo rows(@PathVariable("batchId") Long batchId,
            @RequestParam(value = "status", required = false) String status)
    {
        requireBatch(batchId);
        startPage();
        List<ImportRowView> rows = bizImportBatchService.selectRowViews(batchId, status);
        return getDataTable(rows);
    }

    /**
     * 下载批次错误行清单（BLOCKED/FAILED）。
     * <p>
     * 只导出 6 列白名单字段，绝不包含 {@code raw_json} 中的身份证、住址等敏感原始列。
     * 批次不存在时返回 404。
     * </p>
     *
     * @param batchId  批次ID
     * @param response HTTP 响应，直接写出 xlsx 流
     */
    @PreAuthorize("@ss.hasPermi('system:importBatch:query')")
    @Log(title = "导入错误行", businessType = BusinessType.EXPORT)
    @GetMapping("/{batchId}/errors")
    public void exportErrors(@PathVariable("batchId") Long batchId, HttpServletResponse response) throws IOException
    {
        BizImportBatch batch = bizImportBatchService.selectBizImportBatchById(batchId);
        if (batch == null)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("导入批次不存在：" + batchId);
            return;
        }

        List<ImportRowView> errorRows = bizImportBatchService.selectErrorRows(batchId);
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            Sheet sheet = wb.createSheet("导入错误行");
            Row header = sheet.createRow(0);
            for (int i = 0; i < ERROR_HEADERS.length; i++)
            {
                header.createCell(i).setCellValue(ERROR_HEADERS[i]);
            }
            int rowIdx = 1;
            for (ImportRowView view : errorRows)
            {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(String.valueOf(view.rowNo()));
                setCell(row, 1, view.userCode());
                setCell(row, 2, view.userName());
                setCell(row, 3, view.status());
                setCell(row, 4, view.errorCode());
                setCell(row, 5, view.errorMessage());
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(
                            "导入错误行_批次" + batchId + ".xlsx", StandardCharsets.UTF_8));
            wb.write(response.getOutputStream());
        }
    }

    /** 空值写空串，避免单元格显示 "null" */
    private void setCell(Row row, int col, String value)
    {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
    }

    /** 批次不存在即抛业务异常（由全局异常处理转为错误响应） */
    private void requireBatch(Long batchId)
    {
        if (bizImportBatchService.selectBizImportBatchById(batchId) == null)
        {
            throw new ServiceException("导入批次不存在：" + batchId);
        }
    }
}
