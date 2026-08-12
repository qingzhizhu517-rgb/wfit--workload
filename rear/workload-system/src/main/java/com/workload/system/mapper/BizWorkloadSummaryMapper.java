package com.workload.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.workload.system.domain.BizWorkloadSummary;

/**
 * 学期工作量汇总Mapper接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface BizWorkloadSummaryMapper 
{
    /**
     * 查询学期工作量汇总
     * 
     * @param id 学期工作量汇总主键
     * @return 学期工作量汇总
     */
    public BizWorkloadSummary selectBizWorkloadSummaryById(Long id);

    /**
     * 查询学期工作量汇总列表
     * 
     * @param bizWorkloadSummary 学期工作量汇总
     * @return 学期工作量汇总集合
     */
    public List<BizWorkloadSummary> selectBizWorkloadSummaryList(BizWorkloadSummary bizWorkloadSummary);

    /**
     * 新增学期工作量汇总
     * 
     * @param bizWorkloadSummary 学期工作量汇总
     * @return 结果
     */
    public int insertBizWorkloadSummary(BizWorkloadSummary bizWorkloadSummary);

    /**
     * 修改学期工作量汇总
     * 
     * @param bizWorkloadSummary 学期工作量汇总
     * @return 结果
     */
    public int updateBizWorkloadSummary(BizWorkloadSummary bizWorkloadSummary);

    /**
     * 删除学期工作量汇总
     * 
     * @param id 学期工作量汇总主键
     * @return 结果
     */
    public int deleteBizWorkloadSummaryById(Long id);

    /**
     * 批量删除学期工作量汇总
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBizWorkloadSummaryByIds(Long[] ids);

    /**
     * 审批域专用：带前置状态的原子状态更新（update ... where id=? and status=?）
     * 用于提交审核（填报中 → 待审），返回影响行数供并发判定
     *
     * @param id 汇总主键
     * @param fromStatus 前置状态
     * @param toStatus 目标状态
     * @param updateBy 更新人
     * @return 影响行数
     */
    public int updateStatusIf(@Param("id") Long id, @Param("fromStatus") Integer fromStatus,
            @Param("toStatus") Integer toStatus, @Param("updateBy") String updateBy);

    /**
     * 审批域专用：教务助理审核通过（同时写 academic_assistant_sign 签字与时间），带前置状态条件
     *
     * @param id 汇总主键
     * @param fromStatus 前置状态
     * @param toStatus 目标状态
     * @param signName 教务助理签字（登录账户）
     * @param updateBy 更新人
     * @return 影响行数
     */
    public int approveSummary(@Param("id") Long id, @Param("fromStatus") Integer fromStatus,
            @Param("toStatus") Integer toStatus, @Param("signName") String signName, @Param("updateBy") String updateBy);

    /**
     * 审批域专用：驳回（同时写 remark 驳回原因，兼容现有行为），带前置状态条件
     *
     * @param id 汇总主键
     * @param fromStatus 前置状态
     * @param toStatus 目标状态
     * @param remark 驳回原因（reason 为空时保持原 remark）
     * @param updateBy 更新人
     * @return 影响行数
     */
    public int rejectSummary(@Param("id") Long id, @Param("fromStatus") Integer fromStatus,
            @Param("toStatus") Integer toStatus, @Param("remark") String remark, @Param("updateBy") String updateBy);

    /**
     * 审批域专用：院领导签字确认（同时写 dept_leader_sign 与 lock_time），带前置状态条件
     *
     * @param id 汇总主键
     * @param fromStatus 前置状态
     * @param toStatus 目标状态
     * @param signName 院领导签字（登录账户）
     * @param updateBy 更新人
     * @return 影响行数
     */
    public int signSummary(@Param("id") Long id, @Param("fromStatus") Integer fromStatus,
            @Param("toStatus") Integer toStatus, @Param("signName") String signName, @Param("updateBy") String updateBy);

    /**
     * 审批域专用：解锁（显式 SET lock_time = NULL，规避动态 SQL 对 null 值跳过的问题）
     *
     * @param id 汇总主键
     * @param updateBy 更新人
     * @return 影响行数
     */
    public int unlockById(@Param("id") Long id, @Param("updateBy") String updateBy);

    /**
     * 审批域专用：教师本人确认（写 teacher_sign/teacher_sign_time），限定本人且状态为待审/待签
     *
     * @param id 汇总主键
     * @param userId 教师用户ID（限定本人）
     * @param teacherSign 教师签字
     * @return 影响行数
     */
    public int teacherConfirmById(@Param("id") Long id, @Param("userId") Long userId, @Param("teacherSign") String teacherSign);
}
