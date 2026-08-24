package com.workload.system.mapper;

import com.workload.system.domain.BizAuditLog;

/**
 * 审批日志Mapper接口
 *
 * @author wflg
 */
public interface BizAuditLogMapper
{
    /**
     * 新增审批日志
     *
     * @param bizAuditLog 审批日志
     * @return 结果
     */
    public int insertBizAuditLog(BizAuditLog bizAuditLog);
}
