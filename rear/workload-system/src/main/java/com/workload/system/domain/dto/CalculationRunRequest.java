package com.workload.system.domain.dto;

/**
 * 阶段化一键核算请求：单教师单学期。
 * <p>
 * {@code includeG11=true} 时先同步教务确认的本学期岗位减免到 G11，再依次重算
 * 明细→汇总→酬金；{@code false} 时跳过 G11 同步（等价于旧 recalcAll 语义）。
 */
public class CalculationRunRequest
{
    /** 教师用户ID */
    private Long userId;

    /** 学年学期，如 2025-2026-1 */
    private String semester;

    /** 是否同步教务岗位减免到 G11 */
    private boolean includeG11;

    public CalculationRunRequest()
    {
    }

    public CalculationRunRequest(Long userId, String semester, boolean includeG11)
    {
        this.userId = userId;
        this.semester = semester;
        this.includeG11 = includeG11;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getSemester()
    {
        return semester;
    }

    public void setSemester(String semester)
    {
        this.semester = semester;
    }

    public boolean isIncludeG11()
    {
        return includeG11;
    }

    public void setIncludeG11(boolean includeG11)
    {
        this.includeG11 = includeG11;
    }
}
