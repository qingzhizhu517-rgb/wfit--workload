package com.workload.system.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workload.common.core.domain.entity.SysUser;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.SecurityUtils;
import com.workload.common.utils.excel.ExcelReadUtil;
import com.workload.common.utils.excel.ImportResult;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.domain.BizImportBatch;
import com.workload.system.domain.BizImportRow;
import com.workload.system.domain.BizRoleAssignment;
import com.workload.system.domain.dto.PositionWorkloadImportDTO;
import com.workload.system.domain.dto.importing.ImportConfirmRequest;
import com.workload.system.domain.dto.importing.ImportPreviewResponse;
import com.workload.system.domain.dto.importing.ImportRowView;
import com.workload.system.mapper.BizImportBatchMapper;
import com.workload.system.mapper.BizImportRowMapper;
import com.workload.system.mapper.BizRoleAssignmentMapper;
import com.workload.system.service.IPositionWorkloadImportService;
import com.workload.system.service.ISysUserService;
import com.workload.system.service.ImportPreviewStore;
import com.workload.system.service.ImportTokenHasher;

/**
 * 本学期岗位减免工作量导入服务实现。
 *
 * <p>照搬教学任务两阶段范式：preview 只读数据页、零业务写入、逐行落 {@code biz_import_row}；
 * confirm 按 {@code row_no} 回放写 {@code biz_role_assignment}，每行独立事务经 AOP 代理，
 * 写前经 {@link WorkloadWriteGuard} 校验冻结状态。幂等键（工号,学期）命中即 SKIPPED，
 * 不除以 2、不查岗位目录、不按日期折算。</p>
 *
 * @author wflg
 */
@Service
public class PositionWorkloadImportServiceImpl implements IPositionWorkloadImportService
{
    private static final Logger log = LoggerFactory.getLogger(PositionWorkloadImportServiceImpl.class);

    /** 导入类型常量：与 biz_import_batch.import_type 一致，用于去重与审计。 */
    public static final String IMPORT_TYPE = "POSITION_WORKLOAD";

    /** 目标 Sheet 名：优先解析此页，兼容旧单 Sheet 文件回退第一个 Sheet。 */
    private static final String DATA_SHEET_NAME = "导入数据";

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private BizRoleAssignmentMapper roleAssignmentMapper;

    @Autowired
    private BizImportBatchMapper importBatchMapper;

    @Autowired
    private BizImportRowMapper importRowMapper;

    @Autowired
    private ImportPreviewStore importPreviewStore;

    @Autowired
    private ImportTokenHasher importTokenHasher;

    @Autowired
    private WorkloadWriteGuard workloadWriteGuard;

    /** normalized_json / raw_json 白名单序列化：DTO 只含业务字段，无敏感列。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 预检 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportPreviewResponse preview(InputStream input, String fileName, String templateVersion)
    {
        String version = StringUtils.hasText(templateVersion) ? templateVersion.trim().toUpperCase() : "V1";

        byte[] bytes = readAllBytes(input);
        String sourceHash = sha256Hex(bytes);

        // 去重按「是否已成功导入」判定：同一文件已导入(2) 则拒绝重复导入；
        // 阻断/放弃的历史预检不阻止修数据后重传。整方法 @Transactional，解析中途失败连批次一起回滚。
        if (importBatchMapper.countConfirmedBySource(IMPORT_TYPE, sourceHash) > 0)
        {
            throw new ServiceException("该文件已成功导入过（源文件哈希一致），请勿重复导入");
        }

        // 1) 建批次（解析中 0），落模板版本/源哈希/操作者/一次性确认令牌哈希
        String batchNo = "IMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String confirmToken = UUID.randomUUID().toString().replace("-", "");
        BizImportBatch batch = new BizImportBatch();
        batch.setBatchNo(batchNo);
        batch.setImportType(IMPORT_TYPE);
        batch.setFileName(fileName);
        batch.setStatus(0);
        batch.setTemplateVersion(version);
        batch.setSourceHash(sourceHash);
        batch.setConfirmTokenHash(importTokenHasher.hash(confirmToken));
        fillOperator(batch);
        importBatchMapper.insertBizImportBatch(batch);

        // 2) 逐行解析 → 校验 → 落 biz_import_row（预检零业务写入）
        List<ImportRowView> views = new ArrayList<>();
        int[] counts = new int[2]; // [0]=ready [1]=blocked
        ExcelReadUtil.readEachRowOfSheetOrFirst(bytes, PositionWorkloadImportDTO.class,
                DATA_SHEET_NAME, (dto, physicalRow) -> handlePreviewRow(batch.getId(), physicalRow, dto, views, counts));

        int total = counts[0] + counts[1];

        // 3) 回写批次计数并推进到待确认(1)
        batch.setTotalCount((long) total);
        batch.setSuccessCount((long) counts[0]);
        batch.setFailCount((long) counts[1]);
        importBatchMapper.updateBizImportBatch(batch);
        importPreviewStore.markPrechecked(batch.getId());

        List<String> warnings = new ArrayList<>();
        if (counts[1] > 0)
        {
            warnings.add("有 " + counts[1] + " 行被阻断，修正后方可确认导入");
        }

        return new ImportPreviewResponse(batch.getId(), IMPORT_TYPE, version,
                total, counts[0], counts[1], views, warnings, confirmToken);
    }

    /**
     * 预检单行：校验通过落 READY + normalized/raw JSON；失败落 BLOCKED + 错误信息。
     * 绝不触碰 biz_role_assignment，只写 biz_import_row。
     */
    private void handlePreviewRow(Long batchId, int physicalRow, PositionWorkloadImportDTO dto,
            List<ImportRowView> views, int[] counts)
    {
        String status;
        String errorMessage = null;
        try
        {
            validateRow(dto);
            status = "READY";
            counts[0]++;
        }
        catch (Exception e)
        {
            status = "BLOCKED";
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            counts[1]++;
        }

        BizImportRow row = new BizImportRow();
        row.setBatchId(batchId);
        row.setRowNo(physicalRow);
        row.setRawJson(toJson(dto));
        row.setNormalizedJson(toJson(dto));
        row.setStatus(status);
        if (errorMessage != null)
        {
            row.setErrorCode("VALIDATION");
            row.setErrorMessage(truncate(errorMessage, 500));
        }
        row.setCreateTime(new Date());
        importRowMapper.insertBizImportRow(row);

        views.add(new ImportRowView(physicalRow, dto.getUserCode(), dto.getUserName(),
                status, row.getErrorCode(), row.getErrorMessage()));
    }

    // ==================== 确认 ====================

    @Override
    public ImportResult confirm(ImportConfirmRequest request)
    {
        // 1) 校验可确认：待确认(1) + 令牌匹配 + 0 阻断
        importPreviewStore.assertConfirmable(request);

        // updateSupport（更新既有目标）尚未实现：processSingleRow 只 INSERT，
        // 放行会静默造重复。显式拒绝，重复行统一走 SKIPPED。
        if (request.updateSupport())
        {
            throw new ServiceException("暂不支持更新既有导入记录（updateSupport）；重复行将按跳过处理，请去掉该选项");
        }

        BizImportBatch batch = importBatchMapper.selectBizImportBatchById(request.batchId());
        if (batch == null)
        {
            throw new ServiceException("导入批次不存在：" + request.batchId());
        }
        String batchNo = batch.getBatchNo();

        // 入口原子认领批次(1→2)：并发双击时只有一个请求 markConfirmed 影响 1 行、得以继续；
        // 其余请求影响 0 行抛冲突并退出，绝不进入回放，杜绝重复 INSERT。
        importPreviewStore.markConfirmed(request.batchId());

        // 2) 按 row_no 顺序回放 READY 行；每行独立事务经 AOP 代理，失败只记该行
        List<BizImportRow> rows = importRowMapper.selectByBatchIdAndStatus(request.batchId(), "READY");
        ImportResult result = new ImportResult();
        result.setBatchId(request.batchId());
        IPositionWorkloadImportService proxy = self();

        for (BizImportRow row : rows)
        {
            PositionWorkloadImportDTO dto = fromJson(row.getNormalizedJson());
            try
            {
                if (dto != null && isDuplicate(dto, batchNo))
                {
                    row.setStatus("SKIPPED");
                    row.setErrorCode("DUPLICATE");
                    row.setErrorMessage("同一来源批次已导入该教师本学期记录，已跳过");
                    row.setUpdateTime(new Date());
                    importRowMapper.updateBizImportRow(row);
                    result.addSkip();
                    continue;
                }
                proxy.processSingleRow(dto, batchNo);
                row.setStatus("IMPORTED");
                row.setUpdateTime(new Date());
                importRowMapper.updateBizImportRow(row);
                result.addSuccess();
            }
            catch (Exception e)
            {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                row.setStatus("FAILED");
                row.setErrorCode("IMPORT");
                row.setErrorMessage(truncate(msg, 500));
                row.setUpdateTime(new Date());
                importRowMapper.updateBizImportRow(row);
                result.addError(row.getRowNo() == null ? 0 : row.getRowNo(), msg);
            }
        }

        // 3) 回写批次真实计数（confirm 阶段的 SKIPPED/FAILED 覆盖 preview 的 ready/blocked 值）
        batch.setSuccessCount((long) result.getSuccessCount());
        batch.setFailCount((long) result.getFailCount());
        batch.setTotalCount((long) result.getTotalCount());
        if (result.hasErrors())
        {
            StringBuilder sb = new StringBuilder();
            result.getErrors().forEach(err -> sb.append(err.toString()).append("; "));
            batch.setErrorSummary(sb.substring(0, Math.min(sb.length(), 500)));
        }
        importBatchMapper.updateBizImportBatch(batch);

        // 4) 批次已在入口认领为已导入(2)；若回放有失败行再转失败(4)
        if (result.hasErrors())
        {
            importPreviewStore.markFailed(request.batchId());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processSingleRow(PositionWorkloadImportDTO dto, String batchNo)
    {
        // 1) 查教师（fail-loud：查不到即该行失败）
        SysUser user = findUser(dto.getUserCode());

        // 2) 校验冻结：审批中/已完结(1/2)则该行失败，绝不写入
        workloadWriteGuard.lockDraftOrAbsent(user.getUserId(), dto.getSemester());

        // 3) 写 biz_role_assignment（status=1 有效，allowance_rate=本学期减免值，不除2/不折算）
        BizRoleAssignment assignment = new BizRoleAssignment();
        assignment.setUserId(user.getUserId());
        assignment.setSemester(dto.getSemester());
        assignment.setAcademicYear(extractAcademicYear(dto.getSemester()));
        assignment.setAllowanceRate(dto.getPositionWorkload());
        // role_type 列 NOT NULL 且无默认值；职务名称仅展示可空，空时落占位值「岗位减免」以满足约束
        assignment.setRoleType(StringUtils.hasText(dto.getPositionName()) ? dto.getPositionName() : "岗位减免");
        assignment.setSourceBatchId(batchNo);
        assignment.setStatus(1);
        assignment.setRemark(dto.getRemark());
        assignment.setCreateTime(new Date());
        roleAssignmentMapper.insertBizRoleAssignment(assignment);
    }

    /**
     * 解析当前 bean 的 AOP 代理（以接口类型），供确认阶段逐行独立事务调用 processSingleRow。
     * 抽成方法以便单测 spy 覆盖，绕过真实代理。
     */
    protected IPositionWorkloadImportService self()
    {
        return (IPositionWorkloadImportService) AopContext.currentProxy();
    }

    /**
     * 幂等判定：仅当同一来源批次已写过该教师+学期记录时才视为重复（SKIPPED，防同批次重复行）。
     * 不同来源批次的减免记录允许并存并在汇总层相加（需求3），故不以「教师+学期存在即跳过」误丢第二笔。
     * 教师查不到不在此拦截——留给 processSingleRow fail-loud。
     */
    private boolean isDuplicate(PositionWorkloadImportDTO dto, String batchNo)
    {
        SysUser user = sysUserService.selectUserByUserName(dto.getUserCode());
        if (user == null)
        {
            return false;
        }
        return roleAssignmentMapper.countBySourceBatch(user.getUserId(), dto.getSemester(), batchNo) > 0;
    }

    // ==================== 校验 ====================

    /**
     * 行校验：工号非空、学期格式合法、减免值 &gt;=0 且最多 2 位小数。
     * 职务名称与备注可空（空仍 READY）。
     */
    private void validateRow(PositionWorkloadImportDTO dto)
    {
        if (!StringUtils.hasText(dto.getUserCode()))
        {
            throw new ServiceException("教师工号不能为空");
        }
        if (!StringUtils.hasText(dto.getSemester()))
        {
            throw new ServiceException("学年学期不能为空");
        }
        if (!dto.getSemester().matches("^\\d{4}-\\d{4}-[12]$"))
        {
            throw new ServiceException("学期格式非法，应形如 2025-2026-1，当前: " + dto.getSemester());
        }
        BigDecimal workload = dto.getPositionWorkload();
        if (workload == null)
        {
            throw new ServiceException("岗位减免工作量不能为空");
        }
        if (workload.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new ServiceException("岗位减免工作量不能为负数，当前: " + workload.toPlainString());
        }
        if (workload.stripTrailingZeros().scale() > 2)
        {
            throw new ServiceException("岗位减免工作量最多保留 2 位小数，当前: " + workload.toPlainString());
        }
    }

    // ==================== 工具 ====================

    private SysUser findUser(String userCode)
    {
        SysUser user = sysUserService.selectUserByUserName(userCode);
        if (user == null)
        {
            throw new ServiceException("工号 '" + userCode + "' 不存在");
        }
        return user;
    }

    private String extractAcademicYear(String semester)
    {
        if (semester != null && semester.length() >= 9)
        {
            return semester.substring(0, 9);
        }
        return semester;
    }

    /** 落操作者：优先当前登录用户（无安全上下文时留空，如系统任务/单测）。 */
    private void fillOperator(BizImportBatch batch)
    {
        try
        {
            batch.setOperatorId(SecurityUtils.getUserId());
            batch.setOperatorName(SecurityUtils.getUsername());
        }
        catch (Exception ignored)
        {
            // 无安全上下文：操作者留空，不阻断预检
        }
    }

    private byte[] readAllBytes(InputStream input)
    {
        try
        {
            return input.readAllBytes();
        }
        catch (IOException e)
        {
            throw new ServiceException("读取导入文件失败：" + e.getMessage());
        }
    }

    private String sha256Hex(byte[] bytes)
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest)
            {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        }
        catch (Exception e)
        {
            throw new ServiceException("计算源文件哈希失败：" + e.getMessage());
        }
    }

    private String toJson(Object dto)
    {
        try
        {
            return objectMapper.writeValueAsString(dto);
        }
        catch (Exception e)
        {
            log.warn("序列化导入行失败: {}", e.getMessage());
            return null;
        }
    }

    private PositionWorkloadImportDTO fromJson(String json)
    {
        if (!StringUtils.hasText(json))
        {
            return null;
        }
        try
        {
            return objectMapper.readValue(json, PositionWorkloadImportDTO.class);
        }
        catch (Exception e)
        {
            throw new ServiceException("解析暂存行失败：" + e.getMessage());
        }
    }

    private String truncate(String s, int max)
    {
        if (s == null)
        {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
