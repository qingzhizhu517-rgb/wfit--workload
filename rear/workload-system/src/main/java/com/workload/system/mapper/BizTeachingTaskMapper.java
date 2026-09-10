package com.workload.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
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

    /**
     * 统计同一教师、同一学期、同名课程、同一授课层次、同一工作量类别下已入库的教学任务条数。
     * <p>
     * 用于推导重复系数的「第几次」：本方法返回 n，则当前正在导入的这一条为第 n+1 次。
     * 分组口径来自 else/工作量.md:15-18「课程名称一致即为同一门课，不分年级，
     * 不以课程代码为准。本专科分别算。」故只按 course_name + education_level 分组，
     * 刻意不含 course_code / 班级 / 课程性质。
     * <p>
     * 类别经 biz_workload_item.item_type 限定：G1 的 C1 与 G3 的 K 各自独立计数，
     * 同名的理论课与实习实训不互相干扰。
     *
     * @param userId         教师ID
     * @param semester       学年学期
     * @param courseName     课程名称
     * @param educationLevel 授课层次（本科/专科，NULL 与空串视作同组）
     * @param itemType       工作量类别（G1/G3...）
     * @return 已入库条数
     */
    public int countSameCourseTask(@Param("userId") Long userId, @Param("semester") String semester,
                                   @Param("courseName") String courseName,
                                   @Param("educationLevel") String educationLevel,
                                   @Param("itemType") String itemType);
}
