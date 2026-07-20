package com.workload.system.mapper;

import java.util.List;
import com.workload.system.domain.BizTeachingTask;

/**
 * 导入教学任务Mapper接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface BizTeachingTaskMapper 
{
    /**
     * 查询导入教学任务
     * 
     * @param id 导入教学任务主键
     * @return 导入教学任务
     */
    public BizTeachingTask selectBizTeachingTaskById(Long id);

    /**
     * 查询导入教学任务列表
     * 
     * @param bizTeachingTask 导入教学任务
     * @return 导入教学任务集合
     */
    public List<BizTeachingTask> selectBizTeachingTaskList(BizTeachingTask bizTeachingTask);

    /**
     * 新增导入教学任务
     * 
     * @param bizTeachingTask 导入教学任务
     * @return 结果
     */
    public int insertBizTeachingTask(BizTeachingTask bizTeachingTask);

    /**
     * 修改导入教学任务
     * 
     * @param bizTeachingTask 导入教学任务
     * @return 结果
     */
    public int updateBizTeachingTask(BizTeachingTask bizTeachingTask);

    /**
     * 删除导入教学任务
     * 
     * @param id 导入教学任务主键
     * @return 结果
     */
    public int deleteBizTeachingTaskById(Long id);

    /**
     * 批量删除导入教学任务
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBizTeachingTaskByIds(Long[] ids);
}
