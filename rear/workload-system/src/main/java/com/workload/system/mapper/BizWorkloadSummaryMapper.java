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
    int updateSettlementPointer(@org.apache.ibatis.annotations.Param("id") Long id, @org.apache.ibatis.annotations.Param("snapshotId") Long snapshotId);
    int lockPeriod(@Param("userId") Long userId, @Param("semester") String semester);
    int markNeedsRecalculation(@Param("userId") Long userId, @Param("semester") String semester);

    /** 仅草稿允许写入计算字段，保留审核状态和签名。 */
    int updateCalculatedFieldsIfStatus(@Param("summary") BizWorkloadSummary summary,
            @Param("expectedStatus") Integer expectedStatus);

    /**
     * 查询学期工作量汇总
     * 
     * @param id 学期工作量汇总主键
     * @return 学期工作量汇总
     */
    public BizWorkloadSummary selectBizWorkloadSummaryById(Long id);

    /**
     * 锁定教师学期汇总行，供源数据写入事务检查冻结状态。
     */
    public BizWorkloadSummary selectByUserSemesterForUpdate(@Param("userId") Long userId,
            @Param("semester") String semester);

    /**
     * 酬金重算写前锁定读：按教师+学期加行锁读取汇总，随后复核冻结状态再写 pay_record，
     * 关闭"初查草稿 → 审批事务置待审 → 酬金仍按旧状态写入"的并发窗口。
     * 与 {@link #selectByUserSemesterForUpdate} 同为 FOR UPDATE，但直接返回汇总数据供绩效读取，
     * recalcPay 因此只需一次锁定查询。
     */
    public BizWorkloadSummary selectForPayUpdate(@Param("userId") Long userId,
            @Param("semester") String semester);

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
