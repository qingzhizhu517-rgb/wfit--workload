package com.workload.system.service;

import java.io.InputStream;
import java.util.List;

import com.workload.system.domain.dto.TeacherProfileImportDTO;
import com.workload.system.domain.dto.importing.TeacherRosterPreviewResponse;

/**
 * 教师档案 Excel 导入服务
 *
 * @author wflg
 */
public interface ITeacherProfileImportService
{
    /**
     * 批量导入教师档案
     * <p>
     * 流程：解析行 → 查找/创建用户 → 创建教师档案
     *
     * @param rows           Excel 行数据
     * @param fileName       原始文件名
     * @param updateSupport  是否更新已存在的档案
     * @param startRowNumber 本批首行的数据行号（从 1 开始，用于行级校验错误定位）
     */
    void importTeacherProfiles(List<TeacherProfileImportDTO> rows, String fileName, boolean updateSupport, int startRowNumber);

    /**
     * 教师花名册字段白名单预检（Task 6，preview-only）。
     * <p>
     * 只解析、只统计、只脱敏报告，<b>不写任何业务表</b>、不落导入暂存、<b>不导入真实教师</b>。
     * 敏感列（身份证/住址等）只读表头名做风险提示，其值绝不读入或落地。
     *
     * @param input    Excel 文件输入流（花名册 Sheet1）
     * @param fileName 原始文件名（仅用于提示，不参与解析）
     * @return 字段映射统计、异常队列与脱敏逐行视图
     */
    TeacherRosterPreviewResponse previewRoster(InputStream input, String fileName);
}
