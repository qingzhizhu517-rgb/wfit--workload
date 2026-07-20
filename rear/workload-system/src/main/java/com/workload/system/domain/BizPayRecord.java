package com.workload.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * 酬金汇总对象 biz_pay_record
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizPayRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long userId;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String semester;

    /** FK biz_workload_summary */
    @Excel(name = "FK biz_workload_summary")
    private Long summaryId;

    /** 课时/绩效酬金 */
    @Excel(name = "课时/绩效酬金")
    private BigDecimal courseHourPay;

    /** 其他酬金合计A+B+C+D+E+F+G */
    @Excel(name = "其他酬金合计A+B+C+D+E+F+G")
    private BigDecimal otherPayTotal;

    /** 总金额(四舍五入取整) */
    @Excel(name = "总金额(四舍五入取整)")
    private Long totalPay;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Integer status;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setSemester(String semester) 
    {
        this.semester = semester;
    }

    public String getSemester() 
    {
        return semester;
    }

    public void setSummaryId(Long summaryId) 
    {
        this.summaryId = summaryId;
    }

    public Long getSummaryId() 
    {
        return summaryId;
    }

    public void setCourseHourPay(BigDecimal courseHourPay) 
    {
        this.courseHourPay = courseHourPay;
    }

    public BigDecimal getCourseHourPay() 
    {
        return courseHourPay;
    }

    public void setOtherPayTotal(BigDecimal otherPayTotal) 
    {
        this.otherPayTotal = otherPayTotal;
    }

    public BigDecimal getOtherPayTotal() 
    {
        return otherPayTotal;
    }

    public void setTotalPay(Long totalPay) 
    {
        this.totalPay = totalPay;
    }

    public Long getTotalPay() 
    {
        return totalPay;
    }

    public void setStatus(Integer status) 
    {
        this.status = status;
    }

    public Integer getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("semester", getSemester())
            .append("summaryId", getSummaryId())
            .append("courseHourPay", getCourseHourPay())
            .append("otherPayTotal", getOtherPayTotal())
            .append("totalPay", getTotalPay())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
