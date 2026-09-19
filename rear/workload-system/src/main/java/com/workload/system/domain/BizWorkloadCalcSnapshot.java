package com.workload.system.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 不可变工作量计算快照对象 biz_workload_calc_snapshot
 *
 * <p>每次成功核算固化一份 append-only 快照：公式、因子取值、因子来源、规则版本、结果与内容哈希。
 * 快照只 INSERT，不 UPDATE/DELETE；同一明细以 {@code calculation_version} 单调递增区分。</p>
 */
public class BizWorkloadCalcSnapshot implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** FK biz_workload_item.id */
    private Long itemId;

    /** 明细计算版本（同一 itemId 单调递增） */
    private Long calculationVersion;

    /** 规范化后的公式表达式，如「J1 × C1 × K1 × Q1 × Q2 × N」 */
    private String formulaExpression;

    /** 因子取值 JSON（固定 key 顺序，数值以字符串序列化避免浮点漂移） */
    private String factorJson;

    /** 因子来源 JSON（与 factorJson 同 key 顺序） */
    private String sourceJson;

    /** 规则版本标识 */
    private String ruleVersion;

    /** 核算结果工作量 */
    private BigDecimal result;

    /** 内容哈希（SHA-256，覆盖规范化后的 factorJson+sourceJson+formula+ruleVersion+result） */
    private String snapshotHash;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getItemId()
    {
        return itemId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public Long getCalculationVersion()
    {
        return calculationVersion;
    }

    public void setCalculationVersion(Long calculationVersion)
    {
        this.calculationVersion = calculationVersion;
    }

    public String getFormulaExpression()
    {
        return formulaExpression;
    }

    public void setFormulaExpression(String formulaExpression)
    {
        this.formulaExpression = formulaExpression;
    }

    public String getFactorJson()
    {
        return factorJson;
    }

    public void setFactorJson(String factorJson)
    {
        this.factorJson = factorJson;
    }

    public String getSourceJson()
    {
        return sourceJson;
    }

    public void setSourceJson(String sourceJson)
    {
        this.sourceJson = sourceJson;
    }

    public String getRuleVersion()
    {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion)
    {
        this.ruleVersion = ruleVersion;
    }

    public BigDecimal getResult()
    {
        return result;
    }

    public void setResult(BigDecimal result)
    {
        this.result = result;
    }

    public String getSnapshotHash()
    {
        return snapshotHash;
    }

    public void setSnapshotHash(String snapshotHash)
    {
        this.snapshotHash = snapshotHash;
    }

    public String getCreatedBy()
    {
        return createdBy;
    }

    public void setCreatedBy(String createdBy)
    {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("itemId", getItemId())
                .append("calculationVersion", getCalculationVersion())
                .append("formulaExpression", getFormulaExpression())
                .append("factorJson", getFactorJson())
                .append("sourceJson", getSourceJson())
                .append("ruleVersion", getRuleVersion())
                .append("result", getResult())
                .append("snapshotHash", getSnapshotHash())
                .append("createdBy", getCreatedBy())
                .append("createdAt", getCreatedAt())
                .toString();
    }
}
