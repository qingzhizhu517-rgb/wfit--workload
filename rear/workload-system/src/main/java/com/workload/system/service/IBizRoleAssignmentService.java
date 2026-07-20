package com.workload.system.service;

import java.util.List;
import com.workload.system.domain.BizRoleAssignment;

/**
 * 岗位任职Service接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface IBizRoleAssignmentService 
{
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
     * 批量删除岗位任职
     * 
     * @param ids 需要删除的岗位任职主键集合
     * @return 结果
     */
    public int deleteBizRoleAssignmentByIds(Long[] ids);

    /**
     * 删除岗位任职信息
     * 
     * @param id 岗位任职主键
     * @return 结果
     */
    public int deleteBizRoleAssignmentById(Long id);
}
