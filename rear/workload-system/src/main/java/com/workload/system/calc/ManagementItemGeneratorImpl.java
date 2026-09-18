package com.workload.system.calc;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DateUtils;
import com.workload.system.domain.BizRoleAssignment;
import com.workload.system.domain.BizWlManagement;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.mapper.BizRoleAssignmentMapper;
import com.workload.system.mapper.BizWlManagementMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;

/**
 * 将教务确认的教师本学期岗位减免值幂等同步为 G11 明细。
 * 职务名称只用于说明；不按学年折半、不读取任职日期、不按岗位名称推导。
 * 多条 G11 的 180 学时封顶仍由汇总层处理。
 */
@Service
public class ManagementItemGeneratorImpl implements ManagementItemGenerator
{
    private final BizRoleAssignmentMapper assignmentMapper;
    private final BizWorkloadItemMapper itemMapper;
    private final BizWlManagementMapper managementMapper;
    private final WorkloadCalcService workloadCalcService;
    private final WorkloadWriteGuard writeGuard;

    public ManagementItemGeneratorImpl(BizRoleAssignmentMapper assignmentMapper,
            BizWorkloadItemMapper itemMapper, BizWlManagementMapper managementMapper,
            WorkloadCalcService workloadCalcService, WorkloadWriteGuard writeGuard)
    {
        this.assignmentMapper = assignmentMapper;
        this.itemMapper = itemMapper;
        this.managementMapper = managementMapper;
        this.workloadCalcService = workloadCalcService;
        this.writeGuard = writeGuard;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generate(Long userId, String semester)
    {
        writeGuard.lockDraftOrAbsent(userId, semester);
        return syncAssignments(assignmentMapper.selectActiveByUserSemesterForUpdate(userId, semester), semester);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateForSemester(String semester)
    {
        if (!StringUtils.hasText(semester))
        {
            throw new ServiceException("请选择学期后同步岗位减免");
        }
        Set<Long> userIds = new LinkedHashSet<>();
        for (BizRoleAssignment assignment : selectAssignments(null, semester))
        {
            userIds.add(assignment.getUserId());
        }
        int count = 0;
        for (Long userId : userIds)
        {
            writeGuard.lockDraftOrAbsent(userId, semester);
            count += syncAssignments(assignmentMapper.selectActiveByUserSemesterForUpdate(userId, semester), semester);
        }
        return count;
    }

    private List<BizRoleAssignment> selectAssignments(Long userId, String semester)
    {
        BizRoleAssignment query = new BizRoleAssignment();
        query.setUserId(userId);
        query.setSemester(semester);
        query.setStatus(1);
        return assignmentMapper.selectBizRoleAssignmentList(query);
    }

    private int syncAssignments(List<BizRoleAssignment> assignments, String semester)
    {
        int count = 0;
        for (BizRoleAssignment assignment : assignments)
        {
            if (syncOne(assignment, semester))
            {
                count++;
            }
        }
        return count;
    }

    private boolean syncOne(BizRoleAssignment assignment, String semester)
    {
        BigDecimal amount = assignment.getAllowanceRate();
        if (amount == null || amount.signum() < 0)
        {
            throw new ServiceException("岗位减免工作量（本学期）必须为非负数, id=" + assignment.getId());
        }
        String basis = buildBasis(assignment);
        BizWorkloadItem item = findG11Item(assignment.getUserId(), semester, assignment.getId());
        if (item != null && Integer.valueOf(1).equals(item.getStatus()))
        {
            return false;
        }
        if (item == null)
        {
            item = newItem(assignment, semester);
            itemMapper.insertBizWorkloadItem(item);
            insertDetail(item, assignment, amount, basis);
        }
        else
        {
            BizWlManagement detail = managementMapper.selectBizWlManagementByItemId(item.getId());
            if (detail == null)
            {
                throw new ServiceException("G11管理服务明细缺失, itemId=" + item.getId());
            }
            int rows = managementMapper.updateProrationIfEditable(item.getId(), assignment.getRoleType(),
                    amount, basis, assignment.getSourceBatchId());
            if (rows != 1)
            {
                throw new ServiceException("G11明细或汇总状态已变化，请刷新后重试");
            }
        }
        workloadCalcService.recalcItem(item.getId());
        return true;
    }

    private BizWorkloadItem newItem(BizRoleAssignment assignment, String semester)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setUserId(assignment.getUserId());
        item.setSemester(semester);
        item.setAcademicYear(assignment.getAcademicYear());
        item.setItemType("G11");
        item.setSourceType("IMPORT");
        item.setAssignmentId(assignment.getId());
        item.setRoleType(assignment.getRoleType());
        item.setCalculatedWorkload(BigDecimal.ZERO);
        item.setStatus(0);
        item.setCreateTime(DateUtils.getNowDate());
        return item;
    }

    private void insertDetail(BizWorkloadItem item, BizRoleAssignment assignment,
            BigDecimal amount, String basis)
    {
        BizWlManagement detail = new BizWlManagement();
        detail.setItemId(item.getId());
        detail.setAssignmentId(assignment.getId());
        detail.setRoleType(assignment.getRoleType());
        detail.setProratedAmount(amount);
        detail.setProrationBasis(basis);
        detail.setSourceBatchId(assignment.getSourceBatchId());
        detail.setCreateTime(DateUtils.getNowDate());
        managementMapper.insertBizWlManagement(detail);
    }

    private String buildBasis(BizRoleAssignment assignment)
    {
        String batch = assignment.getSourceBatchId();
        return batch == null || batch.isBlank()
                ? "岗位减免工作量（本学期），计入 G11"
                : "岗位减免工作量（本学期），来源批次 " + batch + "，计入 G11";
    }

    private BizWorkloadItem findG11Item(Long userId, String semester, Long assignmentId)
    {
        BizWorkloadItem query = new BizWorkloadItem();
        query.setUserId(userId);
        query.setSemester(semester);
        query.setItemType("G11");
        query.setAssignmentId(assignmentId);
        List<BizWorkloadItem> items = itemMapper.selectBizWorkloadItemList(query);
        return items.isEmpty() ? null : items.get(0);
    }
}
