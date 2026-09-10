package com.workload.system.calc.strategy;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizWlManagement;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWlManagementMapper;

/**
 * G11 管理服务计算策略：单条 = 明细 prorated_amount（由 G11 生成器按任职区间折算写入），
 * 多岗叠加与学期封顶 180 在汇总层处理。
 * <p>
 * 教师自主申报（source_type=SELF）的 G11 没有明细行，取主表已核定学时，见 calculate 内注释。
 *
 * @author wflg
 * @date 2026-07-21
 */
@Component("managementCalcStrategy")
public class ManagementCalcStrategy extends AbstractWorkloadCalcStrategy
{
    @Autowired
    private BizWlManagementMapper bizWlManagementMapper;

    @Override
    public String getTypeCode()
    {
        return "G11";
    }

    @Override
    public BigDecimal calculate(BizWorkloadItem item)
    {
        BizWlManagement detail = bizWlManagementMapper.selectBizWlManagementByItemId(item.getId());
        if (detail != null)
        {
            return scale(num(detail.getProratedAmount()));
        }
        // 教师自主申报（declare.vue）的 G11 只写主表：biz_wl_management.assignment_id 为 NOT NULL
        // 且外键指向 biz_role_assignment，手工申报没有任职记录，构造不出合法明细行。
        // 这类条目的学时是教师按管理办法自行核定后录在主表上的，重算时沿用该值——
        // 不是静默兜底 0，而是「无明细可算，以已核定值为准」；否则一条申报会让该教师
        // 整个学期的核算全线失败（明细→汇总→酬金），且该条目永远无法通过重算。
        if ("SELF".equals(item.getSourceType()))
        {
            return scale(num(item.getCalculatedWorkload()));
        }
        // 生成器(AUTO)/导入(IMPORT)来源缺明细属数据损坏，保持 fail-loud（见 A2 整改）
        throw new ServiceException("G11管理服务明细缺失, itemId=" + item.getId());
    }
}
