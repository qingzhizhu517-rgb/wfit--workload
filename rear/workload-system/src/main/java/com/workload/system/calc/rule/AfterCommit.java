package com.workload.system.calc.rule;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 写入成功提交后失效；回滚保留原缓存，无事务时立即失效。 */
public final class AfterCommit
{
    private AfterCommit() { }
    public static void run(Runnable action)
    {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
            {
                @Override public void afterCommit() { action.run(); }
            });
        }
        else action.run();
    }
}
