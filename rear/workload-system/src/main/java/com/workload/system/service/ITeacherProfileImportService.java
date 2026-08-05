package com.workload.system.service;

import java.util.List;

import com.workload.system.domain.dto.TeacherProfileImportDTO;

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
     */
    void importTeacherProfiles(List<TeacherProfileImportDTO> rows, String fileName, boolean updateSupport);
}
