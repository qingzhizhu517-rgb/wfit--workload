package com.workload.system.domain.dto;

import java.math.BigDecimal;

import com.alibaba.excel.annotation.ExcelProperty;

/**
 * 本学期岗位减免工作量导入行 DTO。
 *
 * <p>业务口径（2026-09-12 确认）：只导入教务已核定的「本学期岗位减免工作量」，
 * 不建岗位职责目录、不按职务名称重复计算、不除以 2、不按任职天数折算。该减免值直接
 * 落 {@code biz_role_assignment.allowance_rate}（列名沿用，语义已为本学期值），
 * 后续由生成器同步为 G11。职务名称仅作展示，可空。</p>
 *
 * <p>表头顺序须与下方 {@code @ExcelProperty} 完全一致：
 * 教师工号 / 教师姓名 / 学年学期 / 岗位减免工作量（本学期）/ 职务名称（可选）/ 备注。</p>
 *
 * @author wflg
 */
public class PositionWorkloadImportDTO
{
    /** 教师工号（对应 sys_user.user_name） */
    @ExcelProperty("教师工号")
    private String userCode;

    /** 教师姓名（仅展示/核对，不参与匹配） */
    @ExcelProperty("教师姓名")
    private String userName;

    /** 学年学期，形如 2025-2026-1 */
    @ExcelProperty("学年学期")
    private String semester;

    /** 本学期岗位减免工作量（>=0，最多 2 位小数） */
    @ExcelProperty("岗位减免工作量（本学期）")
    private BigDecimal positionWorkload;

    /** 职务名称（可空，仅展示，不参与计算） */
    @ExcelProperty("职务名称")
    private String positionName;

    /** 备注（可空） */
    @ExcelProperty("备注")
    private String remark;

    public String getUserCode()
    {
        return userCode;
    }

    public void setUserCode(String userCode)
    {
        this.userCode = userCode;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getSemester()
    {
        return semester;
    }

    public void setSemester(String semester)
    {
        this.semester = semester;
    }

    public BigDecimal getPositionWorkload()
    {
        return positionWorkload;
    }

    public void setPositionWorkload(BigDecimal positionWorkload)
    {
        this.positionWorkload = positionWorkload;
    }

    public String getPositionName()
    {
        return positionName;
    }

    public void setPositionName(String positionName)
    {
        this.positionName = positionName;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
