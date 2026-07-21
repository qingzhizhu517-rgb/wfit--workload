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
 * 多岗叠加与学期封顶 180 在汇总层处理
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
        if (detail == null)
        {
            throw new ServiceException("G11管理服务明细缺失, itemId=" + item.getId());
        }
        return scale(num(detail.getProratedAmount()));
    }
}
