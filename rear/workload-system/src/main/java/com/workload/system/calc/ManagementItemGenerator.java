package com.workload.system.calc;

/**
 * G11 管理服务工作量生成器：将 biz_role_assignment 中教务确认的本学期岗位减免值同步为 G11
 *
 * @author wflg
 * @date 2026-07-21
 */
public interface ManagementItemGenerator
{
    /**
     * 同步某教师某学期 G11 明细（幂等：同一来源记录复用原明细）
     *
     * @param userId 教师
     * @param semester 学期
     * @return 生成/更新条数
     */
    public int generate(Long userId, String semester);

    /**
     * 同步某学期全部有岗位减免来源记录教师的 G11 明细
     *
     * @param semester 学期
     * @return 生成/更新条数
     */
    public int generateForSemester(String semester);
}
