package com.workload.system.domain.dto;

/**
 * 附件1（表一）批量导出失败清单行。
 * <p>
 * 批量导出保留成功结果的同时，把无明细/未汇总/超行数/渲染异常的教师逐条列出，
 * 供导出方对账「谁没被导出、为什么」。原因只暴露可读业务消息，完整堆栈仅进日志。
 * </p>
 *
 * @author wflg
 */
public class Attachment1BatchErrorDTO
{
    /** 失败教师用户 ID。 */
    private final Long userId;

    /** 失败教师标签（姓名(工号)，取不到时回落 userId）。 */
    private final String userLabel;

    /** 失败原因（可读业务消息）。 */
    private final String reason;

    public Attachment1BatchErrorDTO(Long userId, String userLabel, String reason)
    {
        this.userId = userId;
        this.userLabel = userLabel;
        this.reason = reason;
    }

    public Long getUserId()
    {
        return userId;
    }

    public String getUserLabel()
    {
        return userLabel;
    }

    public String getReason()
    {
        return reason;
    }
}
