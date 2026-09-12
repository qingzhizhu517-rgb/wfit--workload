package com.workload.system.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WorkloadSummaryStatusTest
{
    @Test
    void shouldDefineSummaryApprovalStatusValues()
    {
        assertThat(WorkloadSummaryStatus.DRAFT).isZero();
        assertThat(WorkloadSummaryStatus.PENDING_AUDIT).isEqualTo(1);
        assertThat(WorkloadSummaryStatus.FINISHED).isEqualTo(2);
    }
}
