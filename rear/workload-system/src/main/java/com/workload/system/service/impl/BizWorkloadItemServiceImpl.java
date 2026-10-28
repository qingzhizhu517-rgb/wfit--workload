package com.workload.system.service.impl;

import java.util.List;
import java.util.Objects;
import com.workload.common.utils.DataScopeUtil;
import com.workload.system.calc.WorkloadWriteGuard;
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
    private WorkloadWriteGuard writeGuard;

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
    @Transactional(rollbackFor = Exception.class)
    public int insertBizWorkloadItem(BizWorkloadItem bizWorkloadItem)
    {
        boolean teacher = DataScopeUtil.isTeacherOnly();
        if (teacher && !"G8".equals(bizWorkloadItem.getItemType()) && !"G9".equals(bizWorkloadItem.getItemType()))
        {
            throw new ServiceException("教师自主申报仅支持 G8/G9，其他类别由教务导入或生成");
        }
        Long owner = DataScopeUtil.resolveUserId(bizWorkloadItem.getUserId());
        writeGuard.lockDraftOrAbsent(owner, bizWorkloadItem.getSemester());
        // 新增与编辑一样按白名单构造；确认、审批、计算版本和来源不接受客户端赋值。
        BizWorkloadItem created = new BizWorkloadItem();
        created.setUserId(owner);
        created.setSemester(bizWorkloadItem.getSemester());
        created.setAcademicYear(bizWorkloadItem.getAcademicYear());
        created.setItemType(bizWorkloadItem.getItemType());
        created.setCourseName(bizWorkloadItem.getCourseName());
        created.setDescription(bizWorkloadItem.getDescription());
        created.setRemark(bizWorkloadItem.getRemark());
        created.setEducationLevel(bizWorkloadItem.getEducationLevel());
        created.setMajorCategory(bizWorkloadItem.getMajorCategory());
        // G8/G9当前为人工填报；保留现有表单学时输入，不能允许它伪装成已核对的自动核算记录。
        created.setCalculatedWorkload(bizWorkloadItem.getCalculatedWorkload());
        created.setStatus(0);
        created.setSourceType(teacher ? "SELF" : "MANUAL");
        if (!teacher)
        {
            created.setTaskId(bizWorkloadItem.getTaskId());
            created.setAssignmentId(bizWorkloadItem.getAssignmentId());
            created.setRoleType(bizWorkloadItem.getRoleType());
        }
        created.setCreateTime(DateUtils.getNowDate());
        writeGuard.markChanged(owner, created.getSemester());
        return bizWorkloadItemMapper.insertBizWorkloadItem(created);
    }

    /**
     * 修改工作量明细主表
     *
     * @param bizWorkloadItem 工作量明细主表
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateBizWorkloadItem(BizWorkloadItem bizWorkloadItem)
    {
        BizWorkloadItem existing = requireEditable(bizWorkloadItem.getId());
        if (changed(existing.getUserId(), bizWorkloadItem.getUserId())
                || changed(existing.getSemester(), bizWorkloadItem.getSemester())
                || changed(existing.getItemType(), bizWorkloadItem.getItemType())
                || changed(existing.getTaskId(), bizWorkloadItem.getTaskId())
                || changed(existing.getAssignmentId(), bizWorkloadItem.getAssignmentId()))
        {
            throw new ServiceException("明细归属、类别及源记录关联不可通过通用修改迁移");
        }
        writeGuard.markChanged(existing.getUserId(), existing.getSemester());
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
        if (ids == null || ids.length == 0) return 0;
        for (Long id : ids)
        {
            BizWorkloadItem item = requireEditable(id);
            writeGuard.markChanged(item.getUserId(), item.getSemester());
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
        BizWorkloadItem item = requireEditable(id);
        writeGuard.markChanged(item.getUserId(), item.getSemester());
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
        if (bizWlTheoryMapper.selectByItemIdForUpdate(itemId) != null)
        {
            referencedTable = "biz_wl_theory";
        }
        else if (bizWlPracticeMapper.selectByItemIdForUpdate(itemId) != null)
        {
            referencedTable = "biz_wl_practice";
        }
        else if (bizWlInternshipTrainingMapper.selectByItemIdForUpdate(itemId) != null)
        {
            referencedTable = "biz_wl_internship_training";
        }
        else if (bizWlCourseDesignMapper.selectByItemIdForUpdate(itemId) != null)
        {
            referencedTable = "biz_wl_course_design";
        }
        else if (bizWlThesisMapper.selectByItemIdForUpdate(itemId) != null)
        {
            referencedTable = "biz_wl_thesis";
        }
        else if (bizWlConcentratedInternshipMapper.selectByItemIdForUpdate(itemId) != null)
        {
            referencedTable = "biz_wl_concentrated_internship";
        }
        else if (bizWlManagementMapper.selectByItemIdForUpdate(itemId) != null)
        {
            referencedTable = "biz_wl_management";
        }
        if (referencedTable != null)
        {
            throw new ServiceException("工作量明细(id=" + itemId + ")已被计算子表" + referencedTable
                    + "引用，禁止删除，请先处理相关子表数据");
        }
    }
    private boolean changed(Object existing, Object requested)
    {
        return requested != null && !Objects.equals(existing, requested);
    }

    @Autowired
    private com.workload.system.mapper.BizCoefficientAdjustmentMapper adjustmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirm(Long id, Long expectedVersion, java.math.BigDecimal expectedWorkload)
    {
        if (!com.workload.common.utils.SecurityUtils.hasPermi("system:workloadItem:review"))
            throw new ServiceException("缺少明细核对权限");
        if (expectedVersion == null || expectedVersion < 0 || expectedWorkload == null || expectedWorkload.signum() < 0)
            throw new ServiceException("缺少有效的计算版本或核对学时");
        BizWorkloadItem item = requireEditable(id);
        long version = item.getCalculationVersion() == null ? 0L : item.getCalculationVersion();
        if (version != expectedVersion || item.getCalculatedWorkload() == null
                || item.getCalculatedWorkload().compareTo(expectedWorkload) != 0)
            throw new ServiceException("计算版本或学时已变化，请刷新详情后重新核对");
        if (!Integer.valueOf(0).equals(item.getStatus()))
            throw new ServiceException("只有草稿明细可核对通过");
        if (!"G8".equals(item.getItemType()) && !"G9".equals(item.getItemType()) && (version == 0 || item.getLastCalculatedAt() == null))
            throw new ServiceException("请先完成明细核算再核对");
        if (!adjustmentMapper.selectPendingIdsForUpdate(item.getUserId(), item.getSemester()).isEmpty())
            throw new ServiceException("存在待审系数申请，请先处理");
        int changed = bizWorkloadItemMapper.confirmIfUnchanged(id, expectedVersion, expectedWorkload,
                com.workload.common.utils.SecurityUtils.getUsername());
        if (changed != 1) throw new ServiceException("明细状态或版本已变化，请刷新后重试");
        return changed;
    }

    private BizWorkloadItem requireEditable(Long id)
    {
        BizWorkloadItem item = id == null ? null : bizWorkloadItemMapper.selectBizWorkloadItemById(id);
        if (item == null) throw new ServiceException("工作量明细不存在");
        DataScopeUtil.assertOwnOrAdmin(item.getUserId());
        writeGuard.lockDraftOrAbsent(item.getUserId(), item.getSemester());
        item = bizWorkloadItemMapper.selectByIdForUpdate(id);
        if (item == null) throw new ServiceException("工作量明细已删除，请刷新后重试");
        if (Integer.valueOf(1).equals(item.getStatus()))
        {
            throw new ServiceException("明细已核对，禁止修改或删除");
        }
        return item;
    }
}
