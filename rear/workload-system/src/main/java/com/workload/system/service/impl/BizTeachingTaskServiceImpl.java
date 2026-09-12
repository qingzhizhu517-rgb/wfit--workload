package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.mapper.BizTeachingTaskMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.domain.BizTeachingTask;
import com.workload.system.domain.dto.TeachingTaskExportDTO;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.service.IBizTeachingTaskService;

/**
 * 导入教学任务Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizTeachingTaskServiceImpl implements IBizTeachingTaskService 
{
    @Autowired
    private BizTeachingTaskMapper bizTeachingTaskMapper;

    @Autowired
    private BizWorkloadItemMapper bizWorkloadItemMapper;

    @Autowired
    private WorkloadWriteGuard workloadWriteGuard;

    /**
     * 查询导入教学任务
     * 
     * @param id 导入教学任务主键
     * @return 导入教学任务
     */
    @Override
    public BizTeachingTask selectBizTeachingTaskById(Long id)
    {
        return bizTeachingTaskMapper.selectBizTeachingTaskById(id);
    }

    /**
     * 查询导入教学任务列表
     * 
     * @param bizTeachingTask 导入教学任务
     * @return 导入教学任务
     */
    @Override
    public List<BizTeachingTask> selectBizTeachingTaskList(BizTeachingTask bizTeachingTask)
    {
        return bizTeachingTaskMapper.selectBizTeachingTaskList(bizTeachingTask);
    }

    /**
     * 查询教学任务导出列表
     *
     * @param bizTeachingTask 查询条件
     * @return 教学任务导出数据集合
     */
    @Override
    public List<TeachingTaskExportDTO> selectBizTeachingTaskExportList(BizTeachingTask bizTeachingTask)
    {
        return bizTeachingTaskMapper.selectBizTeachingTaskExportList(bizTeachingTask);
    }

    /**
     * 新增导入教学任务
     * 
     * @param bizTeachingTask 导入教学任务
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertBizTeachingTask(BizTeachingTask bizTeachingTask)
    {
        workloadWriteGuard.lockDraftOrAbsent(bizTeachingTask.getUserId(), bizTeachingTask.getSemester());
        bizTeachingTask.setCreateTime(DateUtils.getNowDate());
        return bizTeachingTaskMapper.insertBizTeachingTask(bizTeachingTask);
    }

    /**
     * 修改导入教学任务
     * 
     * @param bizTeachingTask 导入教学任务
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateBizTeachingTask(BizTeachingTask bizTeachingTask)
    {
        BizTeachingTask existing = requireExistingTask(bizTeachingTask.getId());
        workloadWriteGuard.lockDraftOrAbsent(existing.getUserId(), existing.getSemester());
        // 教师与学期决定冻结边界，手工修改不得借请求体迁移到另一冻结域。
        bizTeachingTask.setUserId(existing.getUserId());
        bizTeachingTask.setSemester(existing.getSemester());
        bizTeachingTask.setUpdateTime(DateUtils.getNowDate());
        return bizTeachingTaskMapper.updateBizTeachingTask(bizTeachingTask);
    }

    /**
     * 批量删除导入教学任务
     * <p>
     * 删除前置拦截：存在关联工作量明细（biz_workload_item.task_id）时拒绝删除（不级联）
     * 
     * @param ids 需要删除的导入教学任务主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBizTeachingTaskByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            BizTeachingTask existing = requireExistingTask(id);
            workloadWriteGuard.lockDraftOrAbsent(existing.getUserId(), existing.getSemester());
            assertNotReferencedByWorkloadItem(id);
        }
        return bizTeachingTaskMapper.deleteBizTeachingTaskByIds(ids);
    }

    /**
     * 删除导入教学任务信息
     * <p>
     * 删除前置拦截：存在关联工作量明细（biz_workload_item.task_id）时拒绝删除（不级联）
     * 
     * @param id 导入教学任务主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBizTeachingTaskById(Long id)
    {
        BizTeachingTask existing = requireExistingTask(id);
        workloadWriteGuard.lockDraftOrAbsent(existing.getUserId(), existing.getSemester());
        assertNotReferencedByWorkloadItem(id);
        return bizTeachingTaskMapper.deleteBizTeachingTaskById(id);
    }

    private BizTeachingTask requireExistingTask(Long id)
    {
        BizTeachingTask existing = bizTeachingTaskMapper.selectBizTeachingTaskById(id);
        if (existing == null)
        {
            throw new ServiceException("教学任务不存在, id=" + id);
        }
        return existing;
    }

    /**
     * 删除前置校验：教学任务被工作量明细引用时抛异常拒绝删除
     *
     * @param taskId 教学任务主键
     */
    private void assertNotReferencedByWorkloadItem(Long taskId)
    {
        BizWorkloadItem query = new BizWorkloadItem();
        query.setTaskId(taskId);
        List<BizWorkloadItem> items = bizWorkloadItemMapper.selectBizWorkloadItemList(query);
        if (items != null && !items.isEmpty())
        {
            throw new ServiceException("教学任务(id=" + taskId + ")已关联 " + items.size()
                    + " 条工作量明细，禁止删除，请先处理相关明细");
        }
    }
}
