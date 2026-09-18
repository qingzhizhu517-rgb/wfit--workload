package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import com.workload.common.exception.ServiceException;
import com.workload.system.calc.WorkloadWriteGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.workload.system.mapper.BizRoleAssignmentMapper;
import com.workload.system.domain.BizRoleAssignment;
import com.workload.system.service.IBizRoleAssignmentService;

/**
 * 岗位任职Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizRoleAssignmentServiceImpl implements IBizRoleAssignmentService 
{
    @Autowired
    private BizRoleAssignmentMapper bizRoleAssignmentMapper;

    @Autowired
    private WorkloadWriteGuard workloadWriteGuard;

    /**
     * 查询岗位任职
     * 
     * @param id 岗位任职主键
     * @return 岗位任职
     */
    @Override
    public BizRoleAssignment selectBizRoleAssignmentById(Long id)
    {
        return bizRoleAssignmentMapper.selectBizRoleAssignmentById(id);
    }

    /**
     * 查询岗位任职列表
     * 
     * @param bizRoleAssignment 岗位任职
     * @return 岗位任职
     */
    @Override
    public List<BizRoleAssignment> selectBizRoleAssignmentList(BizRoleAssignment bizRoleAssignment)
    {
        return bizRoleAssignmentMapper.selectBizRoleAssignmentList(bizRoleAssignment);
    }

    /**
     * 新增岗位任职
     * 
     * @param bizRoleAssignment 岗位任职
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertBizRoleAssignment(BizRoleAssignment bizRoleAssignment)
    {
        workloadWriteGuard.lockDraftOrAbsent(bizRoleAssignment.getUserId(), bizRoleAssignment.getSemester());
        bizRoleAssignment.setCreateTime(DateUtils.getNowDate());
        return bizRoleAssignmentMapper.insertBizRoleAssignment(bizRoleAssignment);
    }

    /**
     * 修改岗位任职
     * 
     * @param bizRoleAssignment 岗位任职
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateBizRoleAssignment(BizRoleAssignment bizRoleAssignment)
    {
        BizRoleAssignment existing = lockExisting(bizRoleAssignment.getId());
        bizRoleAssignment.setUserId(existing.getUserId());
        bizRoleAssignment.setSemester(existing.getSemester());
        bizRoleAssignment.setUpdateTime(DateUtils.getNowDate());
        return bizRoleAssignmentMapper.updateBizRoleAssignment(bizRoleAssignment);
    }

    /**
     * 批量删除岗位任职
     * 
     * @param ids 需要删除的岗位任职主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBizRoleAssignmentByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            lockExisting(id);
        }
        return bizRoleAssignmentMapper.deleteBizRoleAssignmentByIds(ids);
    }

    /**
     * 删除岗位任职信息
     * 
     * @param id 岗位任职主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBizRoleAssignmentById(Long id)
    {
        lockExisting(id);
        return bizRoleAssignmentMapper.deleteBizRoleAssignmentById(id);
    }

    private BizRoleAssignment lockExisting(Long id)
    {
        BizRoleAssignment existing = bizRoleAssignmentMapper.selectBizRoleAssignmentById(id);
        if (existing == null)
        {
            throw new ServiceException("岗位减免记录不存在, id=" + id);
        }
        workloadWriteGuard.lockDraftOrAbsent(existing.getUserId(), existing.getSemester());
        return existing;
    }
}
