package com.workload.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.workload.common.annotation.Excel;
import com.workload.common.core.domain.BaseEntity;

/**
 * 工作量类别字典对象 biz_workload_category_dict
 * 
 * @author wflg
 * @date 2026-07-20
 */
public class BizWorkloadCategoryDict extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 分类代码(G1..G11) */
    private String typeCode;

    /** 分类名称 */
    @Excel(name = "分类名称")
    private String typeName;

    /** 所属大类(TEACHING/ADMIN/EXTRA) */
    @Excel(name = "所属大类(TEACHING/ADMIN/EXTRA)")
    private String parentGroup;

    /** Java计算策略bean名 */
    @Excel(name = "Java计算策略bean名")
    private String calcStrategy;

    /** 是否计入超额核算(1是0否) */
    @Excel(name = "是否计入超额核算(1是0否)")
    private Integer isCalcExcess;

    /** 排序 */
    @Excel(name = "排序")
    private Long sortOrder;

    /** 状态(1正常0停用) */
    @Excel(name = "状态(1正常0停用)")
    private Integer status;

    public void setTypeCode(String typeCode) 
    {
        this.typeCode = typeCode;
    }

    public String getTypeCode() 
    {
        return typeCode;
    }

    public void setTypeName(String typeName) 
    {
        this.typeName = typeName;
    }

    public String getTypeName() 
    {
        return typeName;
    }

    public void setParentGroup(String parentGroup) 
    {
        this.parentGroup = parentGroup;
    }

    public String getParentGroup() 
    {
        return parentGroup;
    }

    public void setCalcStrategy(String calcStrategy) 
    {
        this.calcStrategy = calcStrategy;
    }

    public String getCalcStrategy() 
    {
        return calcStrategy;
    }

    public void setIsCalcExcess(Integer isCalcExcess) 
    {
        this.isCalcExcess = isCalcExcess;
    }

    public Integer getIsCalcExcess() 
    {
        return isCalcExcess;
    }

    public void setSortOrder(Long sortOrder) 
    {
        this.sortOrder = sortOrder;
    }

    public Long getSortOrder() 
    {
        return sortOrder;
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
            .append("typeCode", getTypeCode())
            .append("typeName", getTypeName())
            .append("parentGroup", getParentGroup())
            .append("calcStrategy", getCalcStrategy())
            .append("isCalcExcess", getIsCalcExcess())
            .append("sortOrder", getSortOrder())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
