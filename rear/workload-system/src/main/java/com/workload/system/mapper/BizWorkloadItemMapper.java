package com.workload.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.workload.system.domain.BizWorkloadItem;

/**
 * 工作量明细主表Mapper接口
 * 
 * @author wflg
 * @date 2026-07-20
 */
public interface BizWorkloadItemMapper 
{
    /**
     * 查询工作量明细主表
     * 
     * @param id 工作量明细主表主键
     * @return 工作量明细主表
     */
    public BizWorkloadItem selectBizWorkloadItemById(Long id);

    /**
     * 查询工作量明细主表列表
     * 
     * @param bizWorkloadItem 工作量明细主表
     * @return 工作量明细主表集合
     */
    public List<BizWorkloadItem> selectBizWorkloadItemList(BizWorkloadItem bizWorkloadItem);

    /**
     * 新增工作量明细主表
     * 
     * @param bizWorkloadItem 工作量明细主表
     * @return 结果
     */
    public int insertBizWorkloadItem(BizWorkloadItem bizWorkloadItem);

    /**
     * 修改工作量明细主表
     * 
     * @param bizWorkloadItem 工作量明细主表
     * @return 结果
     */
    public int updateBizWorkloadItem(BizWorkloadItem bizWorkloadItem);

    /**
     * 仅当明细和关联汇总仍可编辑时原子更新计算结果。
     */
    public int updateCalculationIfEditable(@Param("item") BizWorkloadItem item,
            @Param("confirmedStatus") Integer confirmedStatus,
            @Param("draftSummaryStatus") Integer draftSummaryStatus);

    /**
     * 删除工作量明细主表
     * 
     * @param id 工作量明细主表主键
     * @return 结果
     */
    public int deleteBizWorkloadItemById(Long id);

    /**
     * 批量删除工作量明细主表
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBizWorkloadItemByIds(Long[] ids);

    /**
     * 查该学期存在工作量明细的教师ID（去重、升序）。
     * <p>
     * 全学期批量核算的教师清单来源。刻意不与 biz_teacher_profile 内联：
     * 缺档案的教师应当作为「失败明细」暴露给教务，而不是被静默跳过
     * —— 有明细却没档案本身就是需要修的数据问题。
     *
     * @param semester 学年学期
     * @return 教师ID列表
     */
    public List<Long> selectUserIdsBySemester(@Param("semester") String semester);
}
