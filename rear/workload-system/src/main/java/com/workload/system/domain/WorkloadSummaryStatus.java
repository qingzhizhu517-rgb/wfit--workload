package com.workload.system.domain;

/** 学期工作量汇总审批状态。 */
public final class WorkloadSummaryStatus
{
    public static final int DRAFT = 0;
    public static final int PENDING_AUDIT = 1;
    public static final int FINISHED = 2;

    /**
     * 汇总记录是否允许改写业务数据。不存在汇总时由调用方决定是否可新建；
     * 一旦有状态，只有草稿态可写，未知/遗留状态按冻结处理（fail-closed）。
     */
    public static boolean isWriteFrozen(Integer status)
    {
        return status != null && status != DRAFT;
    }

    private WorkloadSummaryStatus()
    {
    }
}
