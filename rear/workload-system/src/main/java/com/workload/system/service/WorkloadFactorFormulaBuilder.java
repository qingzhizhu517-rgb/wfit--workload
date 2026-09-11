package com.workload.system.service;

import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.vo.FactorFormulaVo;

/**
 * @deprecated 请注入 {@link IWorkloadFactorFormulaService}。
 */
@Deprecated
public class WorkloadFactorFormulaBuilder
{
    private final IWorkloadFactorFormulaService delegate;

    public WorkloadFactorFormulaBuilder(IWorkloadFactorFormulaService delegate)
    {
        this.delegate = delegate;
    }

    public FactorFormulaVo build(BizWorkloadItem item)
    {
        return delegate.build(item);
    }
}
