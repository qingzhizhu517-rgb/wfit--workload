package com.workload.system.service.impl;

import java.util.List;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.workload.system.mapper.BizWlConcentratedInternshipMapper;
import com.workload.system.mapper.BizWlCourseDesignMapper;
import com.workload.system.mapper.BizWlInternshipTrainingMapper;
import com.workload.system.mapper.BizWlManagementMapper;
import com.workload.system.mapper.BizWlPracticeMapper;
import com.workload.system.mapper.BizWlTheoryMapper;
import com.workload.system.mapper.BizWlThesisMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.service.IBizWorkloadItemService;

/**
 * 工作量明细主表Service业务层处理
 * 
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizWorkloadItemServiceImpl implements IBizWorkloadItemService 
{
    @Autowired
    private BizWorkloadItemMapper bizWorkloadItemMapper;

    @Autowired
    private BizWlTheoryMapper bizWlTheoryMapper;

    @Autowired
    private BizWlPracticeMapper bizWlPracticeMapper;

    @Autowired
    private BizWlInternshipTrainingMapper bizWlInternshipTrainingMapper;

    @Autowired
    private BizWlCourseDesignMapper bizWlCourseDesignMapper;

    @Autowired
    private BizWlThesisMapper bizWlThesisMapper;

    @Autowired
    private BizWlConcentratedInternshipMapper bizWlConcentratedInternshipMapper;

    @Autowired
    private BizWlManagementMapper bizWlManagementMapper;

    /**
     * 查询工作量明细主表
     * 
     * @param id 工作量明细主表主键
     * @return 工作量明细主表
     */
    @Override
    public BizWorkloadItem selectBizWorkloadItemById(Long id)
    {
        return bizWorkloadItemMapper.selectBizWorkloadItemById(id);
    }

    /**
     * 查询工作量明细主表列表
     * 
     * @param bizWorkloadItem 工作量明细主表
     * @return 工作量明细主表
     */
    @Override
    public List<BizWorkloadItem> selectBizWorkloadItemList(BizWorkloadItem bizWorkloadItem)
    {
        return bizWorkloadItemMapper.selectBizWorkloadItemList(bizWorkloadItem);
    }

    /**
     * 新增工作量明细主表
     * 
     * @param bizWorkloadItem 工作量明细主表
     * @return 结果
     */
    @Override
    public int insertBizWorkloadItem(BizWorkloadItem bizWorkloadItem)
    {
        bizWorkloadItem.setCreateTime(DateUtils.getNowDate());
        return bizWorkloadItemMapper.insertBizWorkloadItem(bizWorkloadItem);
    }

    /**
     * 修改工作量明细主表
     * 
     * @param bizWorkloadItem 工作量明细主表
     * @return 结果
     */
    @Override
    public int updateBizWorkloadItem(BizWorkloadItem bizWorkloadItem)
    {
        bizWorkloadItem.setUpdateTime(DateUtils.getNowDate());
        return bizWorkloadItemMapper.updateBizWorkloadItem(bizWorkloadItem);
    }

    /**
     * 批量删除工作量明细主表
     * <p>
     * 删除前置拦截：存在关联计算子表（biz_wl_*.item_id）时拒绝删除（不级联）
     * 
     * @param ids 需要删除的工作量明细主表主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBizWorkloadItemByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            assertNotReferencedByWlDetail(id);
        }
        return bizWorkloadItemMapper.deleteBizWorkloadItemByIds(ids);
    }

    /**
     * 删除工作量明细主表信息
     * <p>
     * 删除前置拦截：存在关联计算子表（biz_wl_*.item_id）时拒绝删除（不级联）
     * 
     * @param id 工作量明细主表主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBizWorkloadItemById(Long id)
    {
        assertNotReferencedByWlDetail(id);
        return bizWorkloadItemMapper.deleteBizWorkloadItemById(id);
    }

    /**
     * 删除前置校验：工作量明细被计算子表（biz_wl_theory/biz_wl_practice/biz_wl_internship_training/
     * biz_wl_course_design/biz_wl_thesis/biz_wl_concentrated_internship/biz_wl_management，
     * 均以 item_id 外键关联 biz_workload_item.id）引用时抛异常拒绝删除
     *
     * @param itemId 工作量明细主键
     */
    private void assertNotReferencedByWlDetail(Long itemId)
    {
        String referencedTable = null;
        if (bizWlTheoryMapper.selectBizWlTheoryByItemId(itemId) != null)
        {
            referencedTable = "biz_wl_theory";
        }
        else if (bizWlPracticeMapper.selectBizWlPracticeByItemId(itemId) != null)
        {
            referencedTable = "biz_wl_practice";
        }
        else if (bizWlInternshipTrainingMapper.selectBizWlInternshipTrainingByItemId(itemId) != null)
        {
            referencedTable = "biz_wl_internship_training";
        }
        else if (bizWlCourseDesignMapper.selectBizWlCourseDesignByItemId(itemId) != null)
        {
            referencedTable = "biz_wl_course_design";
        }
        else if (bizWlThesisMapper.selectBizWlThesisByItemId(itemId) != null)
        {
            referencedTable = "biz_wl_thesis";
        }
        else if (bizWlConcentratedInternshipMapper.selectBizWlConcentratedInternshipByItemId(itemId) != null)
        {
            referencedTable = "biz_wl_concentrated_internship";
        }
        else if (bizWlManagementMapper.selectBizWlManagementByItemId(itemId) != null)
        {
            referencedTable = "biz_wl_management";
        }
        if (referencedTable != null)
        {
            throw new ServiceException("工作量明细(id=" + itemId + ")已被计算子表" + referencedTable
                    + "引用，禁止删除，请先处理相关子表数据");
        }
    }
}
