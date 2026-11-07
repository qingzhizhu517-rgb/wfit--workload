package com.workload.system.mapper;

import com.workload.system.domain.BizAuditLog;

/**
 * 审批日志Mapper接口
 *
 * @author wflg
 */
public interface BizAuditLogMapper
{
    java.util.List<BizAuditLog> selectPage(@org.apache.ibatis.annotations.Param("summaryId") Long summaryId,@org.apache.ibatis.annotations.Param("offset") long offset,@org.apache.ibatis.annotations.Param("size") int size);
    long countPage(@org.apache.ibatis.annotations.Param("summaryId") Long summaryId);
    /**
     * 新增审批日志
     *
     * @param bizAuditLog 审批日志
     * @return 结果
     */
    public int insertBizAuditLog(BizAuditLog bizAuditLog);
}
