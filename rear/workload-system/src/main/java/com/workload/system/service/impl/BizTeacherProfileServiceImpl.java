package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import com.workload.common.utils.SecurityUtils;
import com.workload.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizTeacherProfileMapper;
import com.workload.system.mapper.BizTeachingTaskMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.mapper.BizWorkloadSummaryMapper;
import com.workload.system.mapper.BizRoleAssignmentMapper;
import com.workload.system.domain.BizTeacherProfile;
import com.workload.system.domain.BizTeachingTask;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizWorkloadSummary;
import com.workload.system.domain.BizRoleAssignment;
import com.workload.system.service.IBizTeacherProfileService;

/**
 * 教师业务档案Service业务层处理
 *
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizTeacherProfileServiceImpl implements IBizTeacherProfileService
{
    @Autowired
    private BizTeacherProfileMapper bizTeacherProfileMapper;

    /** 关联业务表：删除教师档案前需校验无关联数据，防止产生孤儿记录 */
    @Autowired
    private BizTeachingTaskMapper bizTeachingTaskMapper;

    @Autowired
    private BizWorkloadItemMapper bizWorkloadItemMapper;

    @Autowired
    private BizWorkloadSummaryMapper bizWorkloadSummaryMapper;

    @Autowired
    private BizRoleAssignmentMapper bizRoleAssignmentMapper;

    /**
     * 查询教师业务档案
     * 
     * @param userId 教师业务档案主键
     * @return 教师业务档案
     */
    @Override
    public BizTeacherProfile selectBizTeacherProfileByUserId(Long userId)
    {
        return bizTeacherProfileMapper.selectBizTeacherProfileByUserId(userId);
    }

    /**
     * 查询教师业务档案列表
     * 
     * @param bizTeacherProfile 教师业务档案
     * @return 教师业务档案
     */
    @Override
    public List<BizTeacherProfile> selectBizTeacherProfileList(BizTeacherProfile bizTeacherProfile)
    {
        return bizTeacherProfileMapper.selectBizTeacherProfileList(bizTeacherProfile);
    }

    /**
     * 新增教师业务档案
     * 
     * @param bizTeacherProfile 教师业务档案
     * @return 结果
     */
    @Override
    public int insertBizTeacherProfile(BizTeacherProfile bizTeacherProfile)
    {
        // 检查是否已存在该教师的业务档案
        BizTeacherProfile existing = bizTeacherProfileMapper.selectBizTeacherProfileByUserId(bizTeacherProfile.getUserId());
        if (existing != null)
        {
            throw new ServiceException("该教师已存在业务档案，请勿重复添加");
        }
        bizTeacherProfile.setCreateTime(DateUtils.getNowDate());
        bizTeacherProfile.setCreateBy(SecurityUtils.getUsername());
        return bizTeacherProfileMapper.insertBizTeacherProfile(bizTeacherProfile);
    }

    /**
     * 修改教师业务档案
     * 
     * @param bizTeacherProfile 教师业务档案
     * @return 结果
     */
    @Override
    public int updateBizTeacherProfile(BizTeacherProfile bizTeacherProfile)
    {
        bizTeacherProfile.setUpdateTime(DateUtils.getNowDate());
        return bizTeacherProfileMapper.updateBizTeacherProfile(bizTeacherProfile);
    }

    /**
     * 批量删除教师业务档案
     *
     * @param userIds 需要删除的教师业务档案主键
     * @return 结果
     */
    @Override
    public int deleteBizTeacherProfileByUserIds(Long[] userIds)
    {
        if (userIds != null)
        {
            for (Long userId : userIds)
            {
                assertNoRelatedData(userId);
            }
        }
        return bizTeacherProfileMapper.deleteBizTeacherProfileByUserIds(userIds);
    }

    /**
     * 删除教师业务档案信息
     *
     * @param userId 教师业务档案主键
     * @return 结果
     */
    @Override
    public int deleteBizTeacherProfileByUserId(Long userId)
    {
        assertNoRelatedData(userId);
        return bizTeacherProfileMapper.deleteBizTeacherProfileByUserId(userId);
    }

    /**
     * 删除前关联校验：教师存在教学任务/工作量明细/学期汇总/岗位任职时禁止删除，
     * 避免删档后遗留无主的业务数据（孤儿记录）。命中任一关联即抛出 ServiceException 阻断删除。
     *
     * @param userId 教师用户ID
     */
    private void assertNoRelatedData(Long userId)
    {
        if (userId == null)
        {
            return;
        }

        BizTeachingTask taskQuery = new BizTeachingTask();
        taskQuery.setUserId(userId);
        if (!bizTeachingTaskMapper.selectBizTeachingTaskList(taskQuery).isEmpty())
        {
            throw new ServiceException("该教师存在教学任务记录，请先删除其教学任务后再删除档案");
        }

        BizWorkloadItem itemQuery = new BizWorkloadItem();
        itemQuery.setUserId(userId);
        if (!bizWorkloadItemMapper.selectBizWorkloadItemList(itemQuery).isEmpty())
        {
            throw new ServiceException("该教师存在工作量明细记录，请先删除其工作量明细后再删除档案");
        }

        BizWorkloadSummary summaryQuery = new BizWorkloadSummary();
        summaryQuery.setUserId(userId);
        if (!bizWorkloadSummaryMapper.selectBizWorkloadSummaryList(summaryQuery).isEmpty())
        {
            throw new ServiceException("该教师存在学期汇总记录，请先删除其学期汇总后再删除档案");
        }

        BizRoleAssignment roleQuery = new BizRoleAssignment();
        roleQuery.setUserId(userId);
        if (!bizRoleAssignmentMapper.selectBizRoleAssignmentList(roleQuery).isEmpty())
        {
            throw new ServiceException("该教师存在岗位任职记录，请先删除其岗位任职后再删除档案");
        }
    }
}
