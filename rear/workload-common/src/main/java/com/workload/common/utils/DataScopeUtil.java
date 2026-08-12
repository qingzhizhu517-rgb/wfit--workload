package com.workload.common.utils;

import java.util.List;
import com.workload.common.core.domain.entity.SysRole;
import com.workload.common.exception.ServiceException;

/**
 * 教师数据范围安全工具类
 * <p>
 * 统一收口「教师角色只能访问本人数据」的判断逻辑，根除 IDOR 越权
 * （承接代码审查报告 P2-01/P2-02/P2-04/P2-06/P2-10）。
 * </p>
 *
 * @author wflg
 */
public class DataScopeUtil
{
    /** 教师角色标识（见 rear/sql/06_test_accounts.sql：role_key='teacher'） */
    public static final String TEACHER_ROLE_KEY = "teacher";

    private DataScopeUtil()
    {
    }

    /**
     * 当前用户是否为「纯教师」：非超管（userId != 1）且拥有 teacher 角色。
     * <p>
     * 注意：此处不复用 {@link SecurityUtils#hasRole(String)}，因为该方法内置
     * 「角色 key 为 admin 即放行任意角色」的超管角色绕过逻辑，会把持有
     * admin 角色的非超管账号误判为教师；这里改为对角色列表做精确匹配，
     * 与原有各 Controller 中 "teacher".equals(roleKey) 的判断语义保持一致。
     * </p>
     *
     * @return 是否为仅教师身份
     */
    public static boolean isTeacherOnly()
    {
        if (SecurityUtils.isAdmin())
        {
            return false;
        }
        List<SysRole> roles = SecurityUtils.getLoginUser().getUser().getRoles();
        if (roles == null)
        {
            return false;
        }
        return roles.stream().anyMatch(r -> TEACHER_ROLE_KEY.equals(r.getRoleKey()));
    }

    /**
     * 断言当前用户是超管或目标数据归属本人，否则抛越权异常。
     *
     * @param targetUserId 目标记录归属的用户ID
     */
    public static void assertOwnOrAdmin(Long targetUserId)
    {
        if (SecurityUtils.isAdmin())
        {
            return;
        }
        if (targetUserId == null || !targetUserId.equals(SecurityUtils.getUserId()))
        {
            throw new ServiceException("无权访问他人数据");
        }
    }

    /**
     * 数据范围收口：解析查询/导出/写入参数中的 userId。
     * <p>
     * 教师角色强制返回当前登录用户ID（忽略入参中传入的他人 userId）；
     * 管理角色原样返回入参。适用于 list/export/getInfo/add 等场景：
     * {@code entity.setUserId(DataScopeUtil.resolveUserId(entity.getUserId()))}
     * </p>
     *
     * @param requestedUserId 请求中携带的 userId（可为 null）
     * @return 收口后的 userId
     */
    public static Long resolveUserId(Long requestedUserId)
    {
        if (isTeacherOnly())
        {
            return SecurityUtils.getUserId();
        }
        return requestedUserId;
    }
}
