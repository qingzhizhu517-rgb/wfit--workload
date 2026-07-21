package com.workload.system.calc;

/**
 * G11 管理服务工作量生成器：由 biz_role_assignment 按任职区间折算生成/更新明细
 *
 * @author wflg
 * @date 2026-07-21
 */
public interface ManagementItemGenerator
{
    /**
     * 生成某教师某学期 G11 明细（幂等：已存在则按最新任职区间重算）
     *
     * @param userId 教师
     * @param semester 学期
     * @return 生成/更新条数
     */
    public int generate(Long userId, String semester);

    /**
     * 生成某学期全部有任职记录教师的 G11 明细
     *
     * @param semester 学期
     * @return 生成/更新条数
     */
    public int generateForSemester(String semester);
}
