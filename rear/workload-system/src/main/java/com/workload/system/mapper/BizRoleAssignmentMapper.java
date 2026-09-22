package com.workload.system.mapper;

import java.util.List;
import com.workload.system.domain.BizRoleAssignment;

/**
 * 岗位任职Mapper接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface BizRoleAssignmentMapper
{
    /** 冻结锁后使用当前读，避免批量同步沿用锁前的一致性快照。 */
    List<BizRoleAssignment> selectActiveByUserSemesterForUpdate(
            @org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("semester") String semester);

    /**
     * 统计某教师某学期、且来自指定来源批次的有效(status=1)岗位减免记录数。
     * 用于导入确认的幂等判定：仅当同一批次已写过该教师+学期记录时才 SKIPPED（重复确认同一批次），
     * 不同来源批次的记录允许并存（需求3：多条来源记录相加，汇总层按 180 封顶），
     * 故键含 source_batch_id，绝不以「教师+学期存在即跳过」误丢第二笔来源。
     *
     * @param userId        教师用户ID
     * @param semester      学年学期
     * @param sourceBatchId 来源批次号
     * @return 该批次下有效记录数
     */
    int countBySourceBatch(@org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("semester") String semester,
            @org.apache.ibatis.annotations.Param("sourceBatchId") String sourceBatchId);

    /**
     * 查询岗位任职
     * 
     * @param id 岗位任职主键
     * @return 岗位任职
     */
    public BizRoleAssignment selectBizRoleAssignmentById(Long id);

    /**
     * 查询岗位任职列表
     * 
     * @param bizRoleAssignment 岗位任职
     * @return 岗位任职集合
     */
    public List<BizRoleAssignment> selectBizRoleAssignmentList(BizRoleAssignment bizRoleAssignment);

    /**
     * 新增岗位任职
     * 
     * @param bizRoleAssignment 岗位任职
     * @return 结果
     */
    public int insertBizRoleAssignment(BizRoleAssignment bizRoleAssignment);

    /**
     * 修改岗位任职
     * 
     * @param bizRoleAssignment 岗位任职
     * @return 结果
     */
    public int updateBizRoleAssignment(BizRoleAssignment bizRoleAssignment);

    /**
     * 删除岗位任职
     * 
     * @param id 岗位任职主键
     * @return 结果
     */
    public int deleteBizRoleAssignmentById(Long id);

    /**
     * 批量删除岗位任职
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBizRoleAssignmentByIds(Long[] ids);
}
