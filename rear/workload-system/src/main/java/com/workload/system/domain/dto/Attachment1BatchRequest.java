package com.workload.system.domain.dto;

import java.util.List;

import com.workload.common.exception.ServiceException;

/**
 * 附件1（表一）批量导出请求契约。
 * <p>
 * 请求体固定为 {@code {"semester":"2026-2027-1","userIds":[2001,2002],"format":"xlsx"}}：
 * {@code format} 决定输出容器——{@code xlsx} 为多 Sheet 单工作簿，{@code zip} 为逐教师打包。
 * 安全上限（每种容器最多几位教师、单教师最多几行）由后端配置固定，绝不由前端携带。
 * </p>
 *
 * @author wflg
 */
public class Attachment1BatchRequest
{
    /** 输出容器格式。 */
    public enum Format
    {
        /** 多 Sheet 单工作簿。 */
        XLSX,
        /** 逐教师打包 ZIP。 */
        ZIP
    }

    /** 学年学期，如 {@code 2026-2027-1}。 */
    private String semester;

    /** 待导出教师用户 ID 列表（由服务端去重、保序后使用）。 */
    private List<Long> userIds;

    /** 输出格式串（{@code xlsx}/{@code zip}，大小写不敏感）。 */
    private String format;

    /**
     * 归一化输出格式：留空默认 {@code XLSX}；非法值抛业务异常，不静默回落。
     *
     * @return 解析后的 {@link Format}
     */
    public Format normalizedFormat()
    {
        try
        {
            return Format.valueOf(format == null ? "XLSX" : format.trim().toUpperCase());
        }
        catch (IllegalArgumentException ex)
        {
            throw new ServiceException("format 仅支持 xlsx 或 zip");
        }
    }

    public String getSemester()
    {
        return semester;
    }

    public void setSemester(String semester)
    {
        this.semester = semester;
    }

    public List<Long> getUserIds()
    {
        return userIds;
    }

    public void setUserIds(List<Long> userIds)
    {
        this.userIds = userIds;
    }

    public String getFormat()
    {
        return format;
    }

    public void setFormat(String format)
    {
        this.format = format;
    }
}
