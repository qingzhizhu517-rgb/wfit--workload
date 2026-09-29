package com.workload.system.service;

import java.util.List;

import com.workload.system.domain.dto.Attachment1BatchRequest;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 附件1（表一）导出输出边界服务。
 * <p>
 * 承接模板渲染、单人/批量输出与阈值/失败清单处理，把 POI 细节从 Controller 抽离。
 * 单人输出（{@link #writeSingle}）在 Task 3 由实现类接管现有 GET；批量输出
 * （{@link #writeBatch}）小批量写多 Sheet 工作簿、大批量逐教师流式写 ZIP，
 * 输出直接落 {@link HttpServletResponse}，不在内存聚合整批容器。
 * </p>
 *
 * @author wflg
 */
public interface Attachment1ExportService
{
    /**
     * 单教师附件1导出：无明细返回文本、有明细无汇总抛「先核算汇总」、否则写 xlsx。
     *
     * @param userId   目标教师用户 ID（调用方已完成数据范围收口）
     * @param semester 学年学期
     * @param response HTTP 响应
     */
    void writeSingle(Long userId, String semester, HttpServletResponse response) throws Exception;

    /**
     * 多教师批量附件1导出：{@code XLSX} 写多 Sheet 单工作簿，{@code ZIP} 逐教师打包，
     * 两者均保留成功结果并附结构化失败清单。
     *
     * @param semester 学年学期
     * @param userIds  已去重、保序的教师用户 ID 列表
     * @param format   输出容器格式
     * @param response HTTP 响应
     */
    void writeBatch(String semester, List<Long> userIds,
                    Attachment1BatchRequest.Format format,
                    HttpServletResponse response) throws Exception;
}
