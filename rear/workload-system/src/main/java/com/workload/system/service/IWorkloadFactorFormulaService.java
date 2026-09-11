package com.workload.system.service;

import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.vo.FactorFormulaVo;

/** 构造工作量明细的公式与因子展示信息。 */
public interface IWorkloadFactorFormulaService
{
    FactorFormulaVo build(BizWorkloadItem item);
}
