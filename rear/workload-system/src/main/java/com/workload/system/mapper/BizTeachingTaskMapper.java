package com.workload.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.workload.system.domain.BizTeachingTask;
import com.workload.system.domain.dto.TeachingTaskExportDTO;

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
     * 查询教学任务导出列表
     *
     * @param bizTeachingTask 查询条件
     * @return 教学任务导出数据集合
     */
    public List<TeachingTaskExportDTO> selectBizTeachingTaskExportList(BizTeachingTask bizTeachingTask);

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
     * 统计同一教师、同一学期、同名课程、同一授课层次下已入库的教学任务条数。
     * <p>
     * 用于推导重复系数的「第几次」：本方法返回 n，则当前正在导入的这一条为第 n+1 次。
     * 分组口径来自《办法》第十四条1「课程名称一致即为同一门课，不分年级，
     * 不以课程代码为准。本专科分别算。」故只按 course_name + education_level 分组，
     * 刻意不含 course_code / 班级 / 课程性质 / <b>工作量类别</b>
     * （2026-09-10 移除 item_type：办法条文无类别要求，
     * 原口径同名理论课与实习实训各自从第一次起算，无依据）。
     * <p>
     * 直接统计 biz_teaching_task 行数，不再经 biz_workload_item 关联。
     *
     * @param userId         教师ID
     * @param semester       学年学期
     * @param courseName     课程名称
     * @param educationLevel 授课层次（本科/专科，NULL 与空串视作同组）
     * @return 已入库条数
     */
    public int countSameCourseTask(@Param("userId") Long userId, @Param("semester") String semester,
                                   @Param("courseName") String courseName,
                                   @Param("educationLevel") String educationLevel);

    /**
     * 两阶段导入确认阶段的幂等键计数：判断某教师+学期+课程代码+班级+工作量类别的
     * 教学任务是否已入库。命中(>0)则该行为重复，确认时默认 SKIPPED，不静默覆盖。
     * <p>
     * 工作量类别落在 biz_workload_item.item_type，故与 biz_teaching_task 关联比对；
     * class_name 用 ifnull 归一，避免 NULL 与 '' 判为不同键。
     * 「同课」按课程名称判定（与重复次序分组 countSameCourseTask 一致，CLAUDE.md E1：
     * 教师+学期+课程名称，代码相同但课名不同不算重复），故键用 course_name 而非 course_code。
     *
     * @param userId       教师ID
     * @param semester     学年学期
     * @param courseName   课程名称
     * @param className    班级
     * @param itemType     工作量类别（G1~G6，大写）
     * @return 已入库条数
     */
    public int countImportedTask(@Param("userId") Long userId, @Param("semester") String semester,
                                 @Param("courseName") String courseName, @Param("className") String className,
                                 @Param("itemType") String itemType);
}
