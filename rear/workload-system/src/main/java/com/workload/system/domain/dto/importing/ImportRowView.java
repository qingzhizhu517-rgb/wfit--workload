package com.workload.system.domain.dto.importing;

/**
 * 预检/错误行的对外视图。
 *
 * <p>仅暴露规范化后的白名单字段与错误信息，绝不携带 {@code raw_json} 中的原始敏感列
 * （身份证、住址等永不进入此视图）。</p>
 *
 * @param rowNo        源文件物理行号
 * @param userCode     教师工号（白名单字段）
 * @param userName     教师姓名（白名单字段）
 * @param status       行状态 READY/BLOCKED/IMPORTED/SKIPPED/FAILED
 * @param errorCode    错误码（阻断/失败行）
 * @param errorMessage 错误信息（阻断/失败行）
 *
 * @author wflg
 */
public record ImportRowView(int rowNo, String userCode, String userName, String status,
        String errorCode, String errorMessage)
{
}
