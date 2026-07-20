package com.workload.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * 其他酬金明细对象 biz_allowance_item
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizAllowanceItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** FK biz_pay_record */
    @Excel(name = "FK biz_pay_record")
    private Long payRecordId;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long userId;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private String semester;

    /** A/B/C/D/E/F/G */
    @Excel(name = "A/B/C/D/E/F/G")
    private String feeType;

    /** A重修:跟班/单独开班/自学辅导;B实习:分散/集中不跟班 */
    @Excel(name = "A重修:跟班/单独开班/自学辅导;B实习:分散/集中不跟班")
    private String feeSubtype;

    /** $column.columnComment */
    @Excel(name = "${comment}", readConverterExp = "$column.readConverterExp()")
    private Long studentCount;

    /** E讲座时长 */
    @Excel(name = "E讲座时长")
    private BigDecimal durationHours;

    /** F运动会天数 */
    @Excel(name = "F运动会天数")
    private BigDecimal days;

    /** F体测班数 */
    @Excel(name = "F体测班数")
    private Long classCount;

    /** G夜间值班工作量 */
    @Excel(name = "G夜间值班工作量")
    private BigDecimal workloadUnits;

    /** E讲座名称 */
    @Excel(name = "E讲座名称")
    private String lectureName;

    /** 扩展字段 */
    @Excel(name = "扩展字段")
    private String ext;

    /** 计算金额 */
    @Excel(name = "计算金额")
    private BigDecimal amount;

    /** 1正常0停用(D代阅卷默认0) */
    @Excel(name = "1正常0停用(D代阅卷默认0)")
    private Integer status;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setPayRecordId(Long payRecordId) 
    {
        this.payRecordId = payRecordId;
    }

    public Long getPayRecordId() 
    {
        return payRecordId;
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

    public void setFeeType(String feeType) 
    {
        this.feeType = feeType;
    }

    public String getFeeType() 
    {
        return feeType;
    }

    public void setFeeSubtype(String feeSubtype) 
    {
        this.feeSubtype = feeSubtype;
    }

    public String getFeeSubtype() 
    {
        return feeSubtype;
    }

    public void setStudentCount(Long studentCount) 
    {
        this.studentCount = studentCount;
    }

    public Long getStudentCount() 
    {
        return studentCount;
    }

    public void setDurationHours(BigDecimal durationHours) 
    {
        this.durationHours = durationHours;
    }

    public BigDecimal getDurationHours() 
    {
        return durationHours;
    }

    public void setDays(BigDecimal days) 
    {
        this.days = days;
    }

    public BigDecimal getDays() 
    {
        return days;
    }

    public void setClassCount(Long classCount) 
    {
        this.classCount = classCount;
    }

    public Long getClassCount() 
    {
        return classCount;
    }

    public void setWorkloadUnits(BigDecimal workloadUnits) 
    {
        this.workloadUnits = workloadUnits;
    }

    public BigDecimal getWorkloadUnits() 
    {
        return workloadUnits;
    }

    public void setLectureName(String lectureName) 
    {
        this.lectureName = lectureName;
    }

    public String getLectureName() 
    {
        return lectureName;
    }

    public void setExt(String ext) 
    {
        this.ext = ext;
    }

    public String getExt() 
    {
        return ext;
    }

    public void setAmount(BigDecimal amount) 
    {
        this.amount = amount;
    }

    public BigDecimal getAmount() 
    {
        return amount;
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
            .append("payRecordId", getPayRecordId())
            .append("userId", getUserId())
            .append("semester", getSemester())
            .append("feeType", getFeeType())
            .append("feeSubtype", getFeeSubtype())
            .append("studentCount", getStudentCount())
            .append("durationHours", getDurationHours())
            .append("days", getDays())
            .append("classCount", getClassCount())
            .append("workloadUnits", getWorkloadUnits())
            .append("lectureName", getLectureName())
            .append("ext", getExt())
            .append("amount", getAmount())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
