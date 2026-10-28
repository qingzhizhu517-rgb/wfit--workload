package com.workload.system.service.impl;

import java.util.List;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizWorkloadItemMapper;
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

    @Autowired
    private BizWorkloadItemMapper itemMapper;

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
        int rows = bizRoleAssignmentMapper.insertBizRoleAssignment(bizRoleAssignment);
        workloadWriteGuard.markChanged(bizRoleAssignment.getUserId(), bizRoleAssignment.getSemester());
        return rows;
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
        int rows = bizRoleAssignmentMapper.updateBizRoleAssignment(bizRoleAssignment);
        workloadWriteGuard.markChanged(existing.getUserId(), existing.getSemester());
        return rows;
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
            BizRoleAssignment existing = lockExisting(id);
            workloadWriteGuard.markChanged(existing.getUserId(), existing.getSemester());
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
        BizRoleAssignment existing = lockExisting(id);
        workloadWriteGuard.markChanged(existing.getUserId(), existing.getSemester());
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
        BizWorkloadItem query = new BizWorkloadItem();
        query.setAssignmentId(id);
        query.getParams().put("currentRead", true);
        if (!itemMapper.selectBizWorkloadItemList(query).isEmpty())
        {
            throw new ServiceException("岗位记录已关联工作量明细，请先处理关联明细后再修改或删除");
        }
        return existing;
    }
}
