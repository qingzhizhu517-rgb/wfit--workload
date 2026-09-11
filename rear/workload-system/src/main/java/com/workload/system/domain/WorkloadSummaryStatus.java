package com.workload.system.domain;

/** 学期工作量汇总审批状态。 */
public final class WorkloadSummaryStatus
{
    public static final int DRAFT = 0;
    public static final int PENDING_AUDIT = 1;
    public static final int FINISHED = 2;

    private WorkloadSummaryStatus()
    {
    }
}
