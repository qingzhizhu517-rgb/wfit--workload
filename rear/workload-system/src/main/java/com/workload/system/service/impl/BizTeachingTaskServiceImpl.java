package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.workload.system.mapper.BizTeachingTaskMapper;
import com.workload.system.domain.BizTeachingTask;
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
     * 新增导入教学任务
     * 
     * @param bizTeachingTask 导入教学任务
     * @return 结果
     */
    @Override
    public int insertBizTeachingTask(BizTeachingTask bizTeachingTask)
    {
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
    public int updateBizTeachingTask(BizTeachingTask bizTeachingTask)
    {
        bizTeachingTask.setUpdateTime(DateUtils.getNowDate());
        return bizTeachingTaskMapper.updateBizTeachingTask(bizTeachingTask);
    }

    /**
     * 批量删除导入教学任务
     * 
     * @param ids 需要删除的导入教学任务主键
     * @return 结果
     */
    @Override
    public int deleteBizTeachingTaskByIds(Long[] ids)
    {
        return bizTeachingTaskMapper.deleteBizTeachingTaskByIds(ids);
    }

    /**
     * 删除导入教学任务信息
     * 
     * @param id 导入教学任务主键
     * @return 结果
     */
    @Override
    public int deleteBizTeachingTaskById(Long id)
    {
        return bizTeachingTaskMapper.deleteBizTeachingTaskById(id);
    }
}
