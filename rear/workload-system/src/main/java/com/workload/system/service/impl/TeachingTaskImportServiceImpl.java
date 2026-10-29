package com.workload.system.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import com.workload.system.calc.rule.CalculationPolicyContext;
import com.workload.system.calc.SemesterCalendar;
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
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.SecurityUtils;
import com.workload.common.utils.excel.ExcelReadUtil;
import com.workload.common.utils.excel.ImportResult;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.calc.rule.RuleParamService;
import com.workload.system.calc.strategy.CalcStrategyFactory;
import com.workload.system.domain.BizImportBatch;
import com.workload.system.domain.BizTeachingTask;
import com.workload.system.domain.BizWlConcentratedInternship;
import com.workload.system.domain.BizWlCourseDesign;
import com.workload.system.domain.BizWlInternshipTraining;
import com.workload.system.domain.BizWlPractice;
import com.workload.system.domain.BizWlTheory;
import com.workload.system.domain.BizWlThesis;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.BizImportRow;
import com.workload.system.domain.dto.TeachingTaskImportDTO;
import com.workload.system.domain.dto.TeachingTaskImportV1DTO;
import com.workload.system.domain.dto.importing.ImportConfirmRequest;
import com.workload.system.domain.dto.importing.ImportPreviewResponse;
import com.workload.system.domain.dto.importing.ImportRowView;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.mapper.BizImportBatchMapper;
import com.workload.system.mapper.BizImportRowMapper;
import com.workload.system.mapper.BizTeachingTaskMapper;
import com.workload.system.mapper.BizWlConcentratedInternshipMapper;
import com.workload.system.mapper.BizWlCourseDesignMapper;
import com.workload.system.mapper.BizWlInternshipTrainingMapper;
import com.workload.system.mapper.BizWlPracticeMapper;
import com.workload.system.mapper.BizWlTheoryMapper;
import com.workload.system.mapper.BizWlThesisMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.service.ITeachingTaskImportService;
import com.workload.system.service.ImportPreviewStore;
import com.workload.system.service.ImportTokenHasher;
import com.workload.system.service.ISysUserService;
import com.workload.system.service.IWorkloadFactorFormulaService;
import com.workload.system.service.WorkloadSnapshotService;
import com.workload.common.core.domain.entity.SysUser;

/**
 * 教学任务 Excel 导入服务实现
 *
 * @author wflg
 */
@Service
public class TeachingTaskImportServiceImpl implements ITeachingTaskImportService
{
    private static final Logger log = LoggerFactory.getLogger(TeachingTaskImportServiceImpl.class);

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private BizTeachingTaskMapper teachingTaskMapper;

    @Autowired
    private BizWorkloadItemMapper workloadItemMapper;

    @Autowired
    private BizWlTheoryMapper wlTheoryMapper;

    @Autowired
    private BizWlPracticeMapper wlPracticeMapper;

    @Autowired
    private BizWlInternshipTrainingMapper wlInternshipTrainingMapper;

    @Autowired
    private BizWlCourseDesignMapper wlCourseDesignMapper;

    @Autowired
    private BizWlThesisMapper wlThesisMapper;

    @Autowired
    private BizWlConcentratedInternshipMapper wlConcentratedInternshipMapper;

    @Autowired
    private BizImportBatchMapper importBatchMapper;

    @Autowired
    private BizImportRowMapper importRowMapper;

    @Autowired
    private ImportPreviewStore importPreviewStore;

    @Autowired
    private ImportTokenHasher importTokenHasher;

    /** normalized_json / raw_json 白名单序列化：DTO 只含业务字段，无敏感列 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CalcStrategyFactory calcStrategyFactory;

    @Autowired
    private RuleParamService ruleParamService;

    @Autowired
    private SemesterCalendar semesterCalendar = new SemesterCalendar();

    @Autowired
    private WorkloadWriteGuard workloadWriteGuard;

    @Autowired
    private IWorkloadFactorFormulaService factorFormulaService;

    @Autowired
    private WorkloadSnapshotService snapshotService;

    @Override
    public ImportResult importTeachingTasksStreaming(InputStream inputStream, String fileName, String templateType)
    {
        // 归一化并校验模板类别（ALL/G1/G2/G3）；null/空按 ALL 兼容
        String template = normalizeTemplateType(templateType);

        String batchNo = "IMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 创建导入批次记录
        BizImportBatch batch = new BizImportBatch();
        batch.setBatchNo(batchNo);
        batch.setImportType("TEACHING_TASK");
        batch.setFileName(fileName);
        batch.setStatus(0); // 0=解析中
        importBatchMapper.insertBizImportBatch(batch);

        // 结果累加器由监听器维护：逐行独立事务处理，边读边入库（不全量驻留内存）；
        // 单元格解析异常(onException)与业务异常共用同一 ImportResult 和物理行号，避免错误丢失/行号错位
        TeachingTaskImportServiceImpl proxy = (TeachingTaskImportServiceImpl) AopContext.currentProxy();

        // EasyExcel 逐行回调：physicalRow 为 0 基含表头的物理行号，展示时 +1 转 1 基
        ImportResult result = ExcelReadUtil.readEachRow(inputStream, TeachingTaskImportDTO.class,
                (dto, physicalRow) -> proxy.processSingleRow(dto, batchNo, template));
        result.setBatchId(batch.getId());

        // 更新批次记录（对齐表定义 status 语义：2=已导入 4=失败/部分失败）
        batch.setTotalCount((long) result.getTotalCount());
        batch.setSuccessCount((long) result.getSuccessCount());
        batch.setFailCount((long) result.getFailCount());
        batch.setStatus(result.hasErrors() ? 4 : 2);
        if (result.hasErrors())
        {
            StringBuilder sb = new StringBuilder();
            result.getErrors().forEach(err -> sb.append(err.toString()).append("; "));
            batch.setErrorSummary(sb.toString().substring(0, Math.min(sb.length(), 500)));
        }
        importBatchMapper.updateBizImportBatch(batch);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal processSingleRow(TeachingTaskImportDTO dto, String batchNo)
    {
        // 通用模板：等价 templateType=ALL，放行 G1~G6
        return processSingleRow(dto, batchNo, "ALL");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal processSingleRow(TeachingTaskImportDTO dto, String batchNo, String templateType)
    {
        // 同一个规范化输入用于校验、任务落库和系数派生。
        normalizeRow(dto);
        // 1. 校验必填字段
        validateRow(dto);

        // 2. 分类模板闸门：必须在任何触库（查教师/锁汇总/插入）之前比较行类别与模板类别，
        //    不匹配立即拒绝，保证 G1 模板不会误收 G2/G3 行并落库
        assertTemplateType(dto, templateType);

        // 3. 查找教师
        SysUser user = findUser(dto.getUserCode());

        // 3. 锁定汇总并校验冻结状态，必须先于重复计数及任何业务写入
        workloadWriteGuard.lockDraftOrAbsent(user.getUserId(), dto.getSemester());

        // 4. 定重复次序：必须在落库前算，否则 countSameCourseTask 会把本行也数进去
        long repeatOrder = resolveRepeatOrder(dto, user.getUserId());

        // 5. 创建教学任务
        BizTeachingTask task = createTeachingTask(dto, user.getUserId(), batchNo, repeatOrder);
        teachingTaskMapper.insertBizTeachingTask(task);

        // 5. 创建工作量明细主表
        BizWorkloadItem item = createWorkloadItem(dto, user.getUserId(), task.getId());
        workloadItemMapper.insertBizWorkloadItem(item);

        // 6. 创建类别明细并计算（重复系数按 repeatOrder 取值）
        CalculationPolicyContext context = new CalculationPolicyContext(semesterCalendar.endOf(dto.getSemester()), ruleParamService);
        BigDecimal calculated = createDetailAndCalc(dto, item, repeatOrder, context);

        // 7. 触发策略后置回调：G5/G6 据此在 item 上置 is_over_limit 超标标记。
        // 漏调的后果是「是否超限」要等到下一次重算才落库，刚导完看列表和附件1 全显示未超限。
        calcStrategyFactory.get(dto.getWorkloadType().toUpperCase()).afterCalculated(item, calculated, context);
        item.setCalculatedWorkload(calculated);

        // 8. 固化不可变计算快照，与回写落在同一行导入事务：导入即产生可追溯的因子/来源/规则版本快照，
        // 而不必等下一次重算。快照 INSERT 与主表 UPDATE 同事务，任一失败整行回滚。
        // 用 buildFresh：导入首次固化快照必须反映当前子表真值，不做快照 overlay。
        FactorFormulaVo formula = factorFormulaService.buildFresh(item, context);
        if (formula == null)
        {
            throw new ServiceException("无法构建计算公式，明细子表可能缺失, itemId=" + item.getId());
        }
        item.setCalculationVersion(snapshotService.capture(item, formula,
                context.policySetHash()).getCalculationVersion());
        item.setLastCalculatedAt(new Date());

        // 9. 回写计算结果（连同回调置上的 is_over_limit、快照版本/时间一并落库，共用同一条 UPDATE）
        workloadItemMapper.updateBizWorkloadItem(item);
        workloadWriteGuard.markChanged(user.getUserId(), dto.getSemester());

        return calculated;
    }

    // ==================== 两阶段导入：预检 / 确认 ====================

    /** 目标 Sheet 名：v2 模板的数据页，预检只解析此页，说明页示例绝不入库 */
    private static final String DATA_SHEET_NAME = "导入数据";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportPreviewResponse preview(InputStream input, String fileName, String templateVersion)
    {
        // 通用模板：等价 templateType=ALL，放行 G1~G6
        return preview(input, fileName, templateVersion, "ALL");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportPreviewResponse preview(InputStream input, String fileName, String templateVersion,
                                         String templateType)
    {
        String version = normalizeTemplateVersion(templateVersion);
        // 归一化并校验模板类别（ALL/G1/G2/G3）；null/空按 ALL 兼容
        String template = normalizeTemplateType(templateType);

        // 读全字节：一份算 SHA-256 源哈希，一份喂 EasyExcel（流不可重复消费）
        byte[] bytes = readAllBytes(input);
        String sourceHash = sha256Hex(bytes);

        // 去重按「是否已成功导入」判定，而非「是否预检过」：同一文件已导入(2) 则拒绝重复导入；
        // 阻断/放弃的历史预检不阻止修数据后重传。整方法 @Transactional，解析中途失败连批次一起回滚，
        // 不留 status=0 孤儿占用源哈希。
        if (importBatchMapper.countConfirmedBySource("TEACHING_TASK", sourceHash) > 0)
        {
            throw new ServiceException("该文件已成功导入过（源文件哈希一致），请勿重复导入");
        }

        // 1) 建批次（解析中 0），落模板版本/模板类别/源哈希/操作者/一次性确认令牌哈希
        String batchNo = "IMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String confirmToken = UUID.randomUUID().toString().replace("-", "");
        BizImportBatch batch = new BizImportBatch();
        batch.setBatchNo(batchNo);
        batch.setImportType("TEACHING_TASK");
        batch.setFileName(fileName);
        batch.setStatus(0);
        batch.setTemplateVersion(version);
        // 模板类别落库：确认阶段二次闸门据此拦错类行（与 template_version 版式严格分开）
        batch.setTemplateCategory(template);
        batch.setSourceHash(sourceHash);
        batch.setConfirmTokenHash(importTokenHasher.hash(confirmToken));
        fillOperator(batch);
        importBatchMapper.insertBizImportBatch(batch);

        // 2) 逐行解析→校验（含分类闸门）→落 biz_import_row（预检零业务写入）
        List<ImportRowView> views = new ArrayList<>();
        int[] counts = new int[2]; // [0]=ready [1]=blocked

        if ("V1".equals(version))
        {
            // V1 旧文件：读旧表头后转 v2 规范行，避免旧「基数」列被当作缺列全行拒绝
            ExcelReadUtil.readEachRowOfSheet(new ByteArrayInputStream(bytes), TeachingTaskImportV1DTO.class,
                    null, (v1, physicalRow) ->
                            handlePreviewRow(batch.getId(), physicalRow, convertV1(v1), v1, template, views, counts));
        }
        else
        {
            // V2：优先读「导入数据」页（说明页示例不入库）；旧单 Sheet 文件无该页时回退第一个 Sheet，
            // 避免 /importExcel 默认 V2 却收到旧单 Sheet 文件时整批读空/报错。
            ExcelReadUtil.readEachRowOfSheetOrFirst(bytes, TeachingTaskImportDTO.class,
                    DATA_SHEET_NAME, (dto, physicalRow) ->
                            handlePreviewRow(batch.getId(), physicalRow, dto, dto, template, views, counts));
        }

        int total = counts[0] + counts[1];

        // 3) 回写批次计数并推进到待确认(1)
        batch.setTotalCount((long) total);
        batch.setSuccessCount((long) counts[0]);
        batch.setFailCount((long) counts[1]);
        importBatchMapper.updateBizImportBatch(batch);
        importPreviewStore.markPrechecked(batch.getId());

        List<String> warnings = new ArrayList<>();
        if ("V1".equals(version))
        {
            warnings.add("V1 旧模板导入已弃用：请尽快改用 V2 双 Sheet 模板（本次已按类别转换旧基数列）");
        }
        if (counts[1] > 0)
        {
            warnings.add("有 " + counts[1] + " 行被阻断，修正后方可确认导入");
        }

        return new ImportPreviewResponse(batch.getId(), "TEACHING_TASK", version,
                total, counts[0], counts[1], views, warnings, confirmToken);
    }

    /**
     * 预检单行：先校验必填字段，再按模板类别闸门校验行类别（templateType!=ALL 且行类别不符即阻断）；
     * 通过落 READY + normalized/raw JSON，失败落 BLOCKED + 错误信息。
     * 绝不触碰教学任务/工作量主表，只写 biz_import_row。
     */
    private void handlePreviewRow(Long batchId, int physicalRow, TeachingTaskImportDTO normalized,
                                  Object rawDto, String templateType, List<ImportRowView> views, int[] counts)
    {
        // V2 的 rawDto 与 normalized 指向同一 DTO，先保存原始值，避免默认值改写审计原文。
        String rawJson = toJson(rawDto);
        String status;
        String errorMessage = null;
        String errorCode = null;
        try
        {
            normalizeRow(normalized);
            validateRow(normalized);
            // 分类模板闸门：与写库前 processSingleRow 复用同一判定，错误信息含「模板类别 Gx」
            assertTemplateType(normalized, templateType);
            status = "READY";
            counts[0]++;
        }
        catch (Exception e)
        {
            status = "BLOCKED";
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            errorCode = "VALIDATION";
            counts[1]++;
        }

        BizImportRow row = new BizImportRow();
        row.setBatchId(batchId);
        row.setRowNo(physicalRow);
        row.setRawJson(rawJson);
        row.setNormalizedJson(toJson(normalized));
        row.setStatus(status);
        if (errorMessage != null)
        {
            row.setErrorCode(errorCode);
            row.setErrorMessage(truncate(errorMessage, 500));
        }
        row.setCreateTime(new Date());
        importRowMapper.insertBizImportRow(row);

        views.add(new ImportRowView(physicalRow, normalized.getUserCode(), normalized.getUserName(),
                status, row.getErrorCode(), row.getErrorMessage()));
    }

    @Override
    public ImportResult confirm(ImportConfirmRequest request)
    {
        // 1) 校验可确认：待确认(1) + 令牌匹配 + 0 阻断
        importPreviewStore.assertConfirmable(request);

        // updateSupport（更新既有目标）尚未实现：processSingleRow 只 INSERT，
        // 若放行会静默造重复。显式拒绝，避免数据翻倍；重复行统一走 SKIPPED。
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
        // 确认阶段的分类闸门：读批次落库的 template_category（而非硬编码 ALL）。
        // 即便预检漏判，确认回放时 processSingleRow 仍按此类别拦错类行；null/旧批次归一化为 ALL。
        String category = normalizeTemplateType(batch.getTemplateCategory());

        // 入口原子认领批次(1→2)：并发双击时只有一个请求 markConfirmed 影响 1 行、得以继续；
        // 其余请求影响 0 行抛冲突并退出，绝不进入回放，从根本上杜绝重复 INSERT。
        importPreviewStore.markConfirmed(request.batchId());

        // 2) 按 row_no 顺序回放 READY 行；每行独立事务经 AOP 代理，失败只记该行
        List<BizImportRow> rows = importRowMapper.selectByBatchIdAndStatus(request.batchId(), "READY");
        ImportResult result = new ImportResult();
        result.setBatchId(request.batchId());
        ITeachingTaskImportService proxy = self();

        for (BizImportRow row : rows)
        {
            TeachingTaskImportDTO dto = fromJson(row.getNormalizedJson());
            try
            {
                if (dto == null)
                {
                    throw new ServiceException("暂存行解析为空，无法确认导入");
                }
                // 暂存数据仍需在查重之前重新规范化和校验；旧批次也不能绕过输入契约。
                normalizeRow(dto);
                validateRow(dto);
                assertTemplateType(dto, category);
                if (isDuplicate(dto))
                {
                    row.setStatus("SKIPPED");
                    row.setErrorCode("DUPLICATE");
                    row.setErrorMessage("幂等键命中（工号+学期+课程名称+班级+类别），已跳过");
                    row.setUpdateTime(new Date());
                    importRowMapper.updateBizImportRow(row);
                    result.addSkip();
                    continue;
                }
                // 分类闸门读批次 template_category：确认阶段仍按批次类别校验行类别，
                // 保留 guard/快照/afterCalculated
                proxy.processSingleRow(dto, batchNo, category);
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

        // 3) 回写批次真实计数（confirm 阶段的 SKIPPED/FAILED 覆盖 preview 的 ready/blocked 值，
        //    否则 biz_import_batch 计数停留在预检口径，审计时与实际结果不符）。
        //    同步内存 status=2 已导入：batch 是认领前(status=1)加载的，若不同步，RuoYi 动态 update
        //    会以旧值 status=1 覆盖回去，撤销 markConfirmed 的 1→2，破坏文件去重(需 status=2)与 markFailed。
        batch.setStatus(2);
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

        // 4) 批次已在入口认领为已导入(2)；若回放有失败行再转失败(4)，
        //    避免部分失败仍标「已导入」误导审计。
        if (result.hasErrors())
        {
            importPreviewStore.markFailed(request.batchId());
        }
        return result;
    }

    /**
     * 幂等判定：解析教师后按（工号→userId,学期,课程名称,班级,类别）查已入库条数，命中即视为重复（SKIPPED）。
     * 更新既有目标（updateSupport）尚未实现，confirm 已在入口拒绝该选项，故此处一律按跳过口径。
     * 教师查不到不在此拦截——留给 processSingleRow fail-loud。
     */
    private boolean isDuplicate(TeachingTaskImportDTO dto)
    {
        SysUser user = sysUserService.selectUserByUserName(dto.getUserCode());
        if (user == null)
        {
            return false;
        }
        String type = dto.getWorkloadType() == null ? "" : dto.getWorkloadType().trim().toUpperCase();
        return teachingTaskMapper.countImportedTask(user.getUserId(), dto.getSemester(),
                dto.getCourseName(), dto.getClassName(), type) > 0;
    }

    /**
     * 解析当前 bean 的 AOP 代理（以接口类型），供确认阶段逐行独立事务调用 processSingleRow。
     * 抽成方法以便单测 spy 覆盖，绕过真实代理。
     */
    protected ITeachingTaskImportService self()
    {
        return (ITeachingTaskImportService) AopContext.currentProxy();
    }

    /**
     * V1 旧行转 v2 规范行：单一「基数」列 baseValue 按类别落到语义字段。
     * G1/G2→计划学时，G3→天数，G6→周数，G4→课程设计学分，G5 用选课人数（baseValue 忽略）。
     */
    private TeachingTaskImportDTO convertV1(TeachingTaskImportV1DTO v1)
    {
        TeachingTaskImportDTO dto = new TeachingTaskImportDTO();
        dto.setSemester(v1.getSemester());
        dto.setUserCode(v1.getUserCode());
        dto.setUserName(v1.getUserName());
        dto.setCourseName(v1.getCourseName());
        dto.setCourseCode(v1.getCourseCode());
        dto.setWorkloadType(v1.getWorkloadType());
        dto.setEducationLevel(v1.getEducationLevel());
        dto.setMajorCategory(v1.getMajorCategory());
        dto.setCourseNature(v1.getCourseNature());
        dto.setCourseLevel(v1.getCourseLevel());
        dto.setCourseRole(v1.getCourseRole());
        dto.setTeachingEval(v1.getTeachingEval());
        dto.setStudentCount(v1.getStudentCount());
        dto.setCourseCoefficient(v1.getCourseCoefficient());
        dto.setClassName(v1.getClassName());
        dto.setRepeatOrder(v1.getRepeatOrder());

        String type = v1.getWorkloadType() == null ? "" : v1.getWorkloadType().trim().toUpperCase();
        BigDecimal base = v1.getBaseValue();
        switch (type)
        {
            case "G1":
            case "G2":
                dto.setPlannedHours(base);
                break;
            case "G3":
                dto.setDays(base);
                break;
            case "G6":
                dto.setWeeks(base);
                break;
            case "G4":
                dto.setCredits(base);
                break;
            default:
                // G5 只用选课人数，基数列无语义；其余非法类别留待 validateRow 拒绝
                break;
        }
        return dto;
    }

    /** 归一化模板版本：null/空 → V2；其余转大写后必须为 V1/V2。 */
    private String normalizeTemplateVersion(String templateVersion)
    {
        if (!StringUtils.hasText(templateVersion))
        {
            return "V2";
        }
        String v = templateVersion.trim().toUpperCase();
        if (!"V1".equals(v) && !"V2".equals(v))
        {
            throw new ServiceException("不支持的模板版本: " + templateVersion + "（仅允许 V1/V2）");
        }
        return v;
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

    private TeachingTaskImportDTO fromJson(String json)
    {
        if (!StringUtils.hasText(json))
        {
            return null;
        }
        try
        {
            return objectMapper.readValue(json, TeachingTaskImportDTO.class);
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

    /**
     * 归一化模板类别：null/空 → ALL；其余转大写后必须为 ALL/G1/G2/G3，否则拒绝。
     */
    private String normalizeTemplateType(String templateType)
    {
        if (!StringUtils.hasText(templateType))
        {
            return "ALL";
        }
        String normalized = templateType.trim().toUpperCase();
        if (!normalized.equals("ALL") && !normalized.equals("G1")
                && !normalized.equals("G2") && !normalized.equals("G3"))
        {
            throw new ServiceException("不支持的导入模板类别: " + templateType + "（仅允许 ALL/G1/G2/G3）");
        }
        return normalized;
    }

    /**
     * 分类模板闸门：ALL 放行任意合法类别（G1~G6）；G1/G2/G3 模板仅接受同类行，
     * 行类别不符即抛错（消息含「模板类别 Gx」），调用点保证其先于任何写库执行。
     */
    private void assertTemplateType(TeachingTaskImportDTO dto, String templateType)
    {
        String template = normalizeTemplateType(templateType);
        if ("ALL".equals(template))
        {
            return;
        }
        String rowType = dto.getWorkloadType() == null ? "" : dto.getWorkloadType().trim().toUpperCase();
        if (!template.equals(rowType))
        {
            throw new ServiceException("模板类别 " + template + " 只接受 " + template
                    + " 类工作量，当前行类别为 " + dto.getWorkloadType());
        }
    }

    /** 规范化可选默认值，预检、确认和直接导入共用同一输入。 */
    private void normalizeRow(TeachingTaskImportDTO dto)
    {
        if (dto == null)
        {
            throw new ServiceException("导入行不能为空");
        }
        dto.setCourseNature(defaultStr(dto.getCourseNature(), "必修").trim());
        dto.setMajorCategory(defaultStr(dto.getMajorCategory(), "理工类").trim());
    }

    /**
     * 校验必填字段
     */
    private void validateRow(TeachingTaskImportDTO dto)
    {
        if (!StringUtils.hasText(dto.getSemester()))
        {
            throw new ServiceException("学年学期不能为空");
        }
        // 学期格式必须为 学年-学年-学期号，如 2025-2026-1（学期号 1 秋 / 2 春）
        semesterCalendar.endOf(dto.getSemester());
        if (!dto.getSemester().matches("^\\d{4}-\\d{4}-[12]$"))
        {
            throw new ServiceException("学期格式非法，应形如 2025-2026-1，当前: " + dto.getSemester());
        }
        if (!StringUtils.hasText(dto.getUserCode()))
        {
            throw new ServiceException("教师工号不能为空");
        }
        if (!StringUtils.hasText(dto.getCourseName()))
        {
            throw new ServiceException("课程名称不能为空");
        }
        if (!StringUtils.hasText(dto.getWorkloadType()))
        {
            throw new ServiceException("工作量类别不能为空");
        }
        // 校验类别代码（数量互斥矩阵前置：后续校验依赖已归一的类别）
        String type = dto.getWorkloadType().toUpperCase();
        if (!type.matches("G[1-6]"))
        {
            throw new ServiceException("工作量类别必须为 G1~G6，当前: " + dto.getWorkloadType());
        }
        // 课程系数列适用范围：仅 G2/G3/G5 允许非空（教务按个案覆盖 K/D/K5）；
        // G1/G4/G6 填该列即拒绝，防止产生无法解释的「总系数」
        validateCourseCoefficientScope(type, dto.getCourseCoefficient());
        // v2 数量字段按类别校验：先拒不适用字段（互斥），再校必填 + 精度/人数
        validateQuantityByCategory(type, dto);
        // G1/G2/G3 的人数虽可省略，显式填写时也必须为正。
        if (dto.getStudentCount() != null && dto.getStudentCount() <= 0)
        {
            throw new ServiceException("选课/指导人数必须为正整数");
        }
        if (dto.getCourseCoefficient() != null && dto.getCourseCoefficient().signum() <= 0)
        {
            throw new ServiceException("课程系数必须大于 0");
        }
        // 重复次序为可选列；填了就必须是正整数，填 0 或负数属笔误，直接拒绝而非静默当 1
        if (dto.getRepeatOrder() != null && dto.getRepeatOrder() < 1)
        {
            throw new ServiceException("重复次序必须为不小于 1 的整数，当前: " + dto.getRepeatOrder());
        }
    }

    /**
     * 课程系数列适用范围校验：仅 G2/G3/G5 允许非空。
     * G1/G4/G6 的公式没有可由单列覆盖的自由系数，填了即拒绝。
     */
    private void validateCourseCoefficientScope(String type, BigDecimal courseCoefficient)
    {
        if (courseCoefficient == null)
        {
            return;
        }
        if (!"G2".equals(type) && !"G3".equals(type) && !"G5".equals(type))
        {
            throw new ServiceException(type + "不支持课程系数列（仅 G2/G3/G5 允许按个案覆盖系数）");
        }
    }

    /**
     * v2 数量字段互斥矩阵：每类只接受语义匹配的数量列。
     * <ul>
     *   <li>G1/G2 → 计划学时 plannedHours</li>
     *   <li>G3 → 天数 days</li>
     *   <li>G4 → 课程设计学分 credits + 指导人数 studentCount</li>
     *   <li>G5 → 指导人数 studentCount（不再要求无意义的占位基数）</li>
     *   <li>G6 → 周数 weeks + 指导人数 studentCount</li>
     * </ul>
     * 先拒绝填了不适用数量列的行（报「Gx只能填写…」），再校验本类必填项（报「Gx必须填写…」）。
     * plannedHours/days/weeks/credits 为正数且最多 2 位小数；人数为正整数。
     * 选课/指导人数列跨类共用（G1/G2 作合堂人数、G3 可留空），故互斥只约束四个数量列，不约束人数。
     */
    private void validateQuantityByCategory(String type, TeachingTaskImportDTO dto)
    {
        switch (type)
        {
            case "G1":
            case "G2":
                assertOnlyQuantity(type, "计划学时", dto, QuantityField.PLANNED_HOURS);
                requirePositiveDecimal(dto.getPlannedHours(), type + "必须填写计划学时");
                break;
            case "G3":
                assertOnlyQuantity(type, "天数", dto, QuantityField.DAYS);
                requirePositiveDecimal(dto.getDays(), type + "必须填写天数");
                break;
            case "G4":
                assertOnlyQuantity(type, "课程设计学分和指导人数", dto, QuantityField.CREDITS);
                requirePositiveDecimal(dto.getCredits(), type + "必须填写课程设计学分");
                requirePositiveStudent(dto.getStudentCount(), type + "必须填写指导人数");
                break;
            case "G5":
                assertOnlyQuantity(type, "指导人数", dto, null);
                requirePositiveStudent(dto.getStudentCount(), type + "必须填写指导人数");
                break;
            case "G6":
                assertOnlyQuantity(type, "周数和指导人数", dto, QuantityField.WEEKS);
                requirePositiveDecimal(dto.getWeeks(), type + "必须填写周数");
                requirePositiveStudent(dto.getStudentCount(), type + "必须填写指导人数");
                break;
            default:
                throw new ServiceException("工作量类别必须为 G1~G6，当前: " + type);
        }
    }

    /** 本类允许的唯一数量列枚举（null 表示该类不接受任何数量列，如 G5 只认人数）。 */
    private enum QuantityField
    {
        PLANNED_HOURS, DAYS, WEEKS, CREDITS
    }

    /**
     * 校验四个数量列中除 allowed 外均未填；填了不适用列则报「Gx只能填写{allowedLabel}」。
     * 人数列不在此约束范围内（跨类共用）。
     */
    private void assertOnlyQuantity(String type, String allowedLabel, TeachingTaskImportDTO dto,
                                    QuantityField allowed)
    {
        if (allowed != QuantityField.PLANNED_HOURS && dto.getPlannedHours() != null)
        {
            throw new ServiceException(type + "只能填写" + allowedLabel);
        }
        if (allowed != QuantityField.DAYS && dto.getDays() != null)
        {
            throw new ServiceException(type + "只能填写" + allowedLabel);
        }
        if (allowed != QuantityField.WEEKS && dto.getWeeks() != null)
        {
            throw new ServiceException(type + "只能填写" + allowedLabel);
        }
        if (allowed != QuantityField.CREDITS && dto.getCredits() != null)
        {
            throw new ServiceException(type + "只能填写" + allowedLabel);
        }
    }

    /** 数量列必填校验：非空、大于 0、最多 2 位小数。 */
    private void requirePositiveDecimal(BigDecimal value, String requiredMessage)
    {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException(requiredMessage + "（正数，最多2位小数）");
        }
        if (value.stripTrailingZeros().scale() > 2)
        {
            throw new ServiceException(requiredMessage.replace("必须填写", "") + "最多保留2位小数，当前: " + value.toPlainString());
        }
    }

    /** 指导人数必填校验：非空、正整数。 */
    private void requirePositiveStudent(Integer studentCount, String requiredMessage)
    {
        if (studentCount == null || studentCount <= 0)
        {
            throw new ServiceException(requiredMessage + "（正整数）");
        }
    }

    /**
     * 定重复系数的「第几次」。
     * <p>
     * 优先取 Excel「重复次序」列的显式值（教务可人工指定哪个班算第一次）；
     * 留空则按同组已入库条数 +1 自动补位，见 {@code countSameCourseTask} 的分组口径
     * （教师 + 学期 + 课程名称 + 授课层次，不含课程代码、班级与工作量类别）。
     * <p>
     * 自动补位依赖流式导入的「逐行独立事务、顺序提交」：处理第 N 行时前 N-1 行已提交可见。
     * 分两次导入同一门课的不同班级也能正确续算，因为计数走库而非批次内存。
     *
     * @return 次序（≥1）
     */
    private long resolveRepeatOrder(TeachingTaskImportDTO dto, Long userId)
    {
        if (dto.getRepeatOrder() != null)
        {
            return dto.getRepeatOrder().longValue();
        }
        // 与 createTeachingTask 写库值保持一致，否则默认「本科」的行会分到不同组
        String educationLevel = normalizeEducationLevel(dto.getEducationLevel());
        int existing = teachingTaskMapper.countSameCourseTask(userId, dto.getSemester(),
                dto.getCourseName(), educationLevel);
        return existing + 1L;
    }

    /**
     * 通过工号精确查找教师
     */
    private SysUser findUser(String userCode)
    {
        SysUser user = sysUserService.selectUserByUserName(userCode);
        if (user == null)
        {
            throw new ServiceException("工号 '" + userCode + "' 不存在");
        }
        return user;
    }

    /**
     * 创建教学任务记录
     */
    private BizTeachingTask createTeachingTask(TeachingTaskImportDTO dto, Long userId, String batchNo, long repeatOrder)
    {
        BizTeachingTask task = new BizTeachingTask();
        task.setUserId(userId);
        task.setSemester(dto.getSemester());
        task.setAcademicYear(extractAcademicYear(dto.getSemester()));
        task.setCourseName(dto.getCourseName());
        task.setCourseCode(dto.getCourseCode());
        task.setEducationLevel(normalizeEducationLevel(dto.getEducationLevel()));
        task.setMajorCategory(defaultStr(dto.getMajorCategory(), "理工类"));
        task.setCourseNature(defaultStr(dto.getCourseNature(), "必修"));
        task.setCourseLevel(defaultStr(dto.getCourseLevel(), "其他"));
        task.setCourseRole(defaultStr(dto.getCourseRole(), "独立"));
        // 班级：区分同一门课的不同班次，附件1「系数说明」列据此指名道姓到具体班级
        task.setClassName(dto.getClassName());
        task.setStudentCount(dto.getStudentCount() != null ? dto.getStudentCount().longValue() : 0L);
        task.setTheoryHours(isG1(dto) ? dto.getPlannedHours() : BigDecimal.ZERO);
        task.setPracticeHours(isG2(dto) ? dto.getPlannedHours() : BigDecimal.ZERO);
        // 源数量快照：按类别原样保留导入文件填的数量列（学时/天数/周数/学分），
        // 与 theory_hours/practice_hours 的口径落库分离，供事后审计「导入时到底填了什么」。
        task.setInputPlannedHours(dto.getPlannedHours());
        task.setInputDays(dto.getDays());
        task.setInputWeeks(dto.getWeeks());
        task.setInputCredits(dto.getCredits());
        task.setQuantityType(resolveQuantityType(dto.getWorkloadType()));
        // 重复次序落库：既是 C1/K 的取值依据，也是事后审计「为什么这条只算 0.8」的唯一凭据
        task.setRepeatOrder(repeatOrder);
        // 重修标志不占层次列（用户拍板）：课程名含「重修」即置 1
        task.setIsRetake(dto.getCourseName() != null && dto.getCourseName().contains("重修") ? 1 : 0);
        task.setImportSource("EXCEL");
        task.setImportBatch(batchNo);
        task.setImportTime(new Date());
        // 教学任务为源数据记录，状态语义 1=正常/有效 0=停用（与手工新增 reset() 及 schema DEFAULT 1 对齐）；
        // 前端 normalStatusMap 也按 1正常/0停用 渲染，导入置 1 才与手工录入一致，避免全部显示“停用”
        task.setStatus(1);
        return task;
    }

    /**
     * 源数量类型标识：记录该行导入时填的是哪种数量列，便于事后审计与前端按类别回显。
     * G1/G2=PLANNED_HOURS、G3=DAYS、G4=CREDITS、G5=STUDENT_COUNT、G6=WEEKS。
     */
    private String resolveQuantityType(String workloadType)
    {
        String type = workloadType == null ? "" : workloadType.trim().toUpperCase();
        return switch (type)
        {
            case "G1", "G2" -> "PLANNED_HOURS";
            case "G3" -> "DAYS";
            case "G4" -> "CREDITS";
            case "G5" -> "STUDENT_COUNT";
            case "G6" -> "WEEKS";
            default -> null;
        };
    }

    /**
     * 创建工作量明细主表
     */
    private BizWorkloadItem createWorkloadItem(TeachingTaskImportDTO dto, Long userId, Long taskId)
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setUserId(userId);
        item.setSemester(dto.getSemester());
        item.setAcademicYear(extractAcademicYear(dto.getSemester()));
        item.setItemType(dto.getWorkloadType().toUpperCase());
        item.setSourceType("IMPORT");
        item.setTaskId(taskId);
        item.setCourseName(dto.getCourseName());
        item.setEducationLevel(normalizeEducationLevel(dto.getEducationLevel()));
        item.setMajorCategory(defaultStr(dto.getMajorCategory(), "理工类"));
        item.setCalculatedWorkload(BigDecimal.ZERO);
        item.setStatus(0);
        return item;
    }

    /**
     * 创建类别明细并调用策略计算
     */
    private BigDecimal createDetailAndCalc(TeachingTaskImportDTO dto, BizWorkloadItem item, long repeatOrder, CalculationPolicyContext context)
    {
        String type = dto.getWorkloadType().toUpperCase();

        switch (type)
        {
            case "G1":
                return createG1Detail(dto, item, repeatOrder, context);
            case "G2":
                return createG2Detail(dto, item, context);
            case "G3":
                return createG3Detail(dto, item, repeatOrder, context);
            case "G4":
                return createG4Detail(dto, item, context);
            case "G5":
                return createG5Detail(dto, item, context);
            case "G6":
                return createG6Detail(dto, item, context);
            default:
                throw new ServiceException("不支持的工作量类别: " + type);
        }
    }

    /**
     * G1 理论课：J1 × C1 × K1 × Q1 × Q2 × Q3 × N
     */
    private BigDecimal createG1Detail(TeachingTaskImportDTO dto, BizWorkloadItem item, long repeatOrder, CalculationPolicyContext context)
    {
        BizWlTheory detail = new BizWlTheory();
        detail.setItemId(item.getId());
        detail.setJ1(dto.getPlannedHours());
        detail.setC1(calcC1(repeatOrder, dto)); // 重复系数：按同名课第几次，1.0/0.9/0.8
        detail.setK1(calcK1(dto)); // 课程类型系数
        detail.setQ1(calcQ1(dto)); // 教学质量系数
        detail.setQ2(calcQ2(dto)); // 课程质量系数
        detail.setQ3(BigDecimal.ONE); // 全外文系数默认 1.0
        detail.setN(calcN(dto));   // 合堂系数
        wlTheoryMapper.insertBizWlTheory(detail);

        // 策略计算（G1 策略必配；未配/配错时 CalcStrategyFactory 抛异常，由外层记为该行错误）
        return calcStrategyFactory.get("G1").calculate(item, context);
    }

    /**
     * G2 实践课：J2 × K × C2 × Q1 × Q2
     */
    private BigDecimal createG2Detail(TeachingTaskImportDTO dto, BizWorkloadItem item, CalculationPolicyContext context)
    {
        BizWlPractice detail = new BizWlPractice();
        detail.setItemId(item.getId());
        detail.setJ2(dto.getPlannedHours());
        detail.setK(calcG2K(dto)); // 专业大类系数：理工 1.0 / 其他 0.9
        detail.setC2(new BigDecimal("0.9")); // 实践课重复系数固定 0.9
        detail.setQ1(calcQ1(dto));
        detail.setQ2(calcQ2(dto));
        detail.setQ3(BigDecimal.ONE);
        wlPracticeMapper.insertBizWlPractice(detail);

        return calcStrategyFactory.get("G2").calculate(item, context);
    }

    /**
     * G3 实习实训：T × D × K × Q1 × Q2
     */
    private BigDecimal createG3Detail(TeachingTaskImportDTO dto, BizWorkloadItem item, long repeatOrder, CalculationPolicyContext context)
    {
        BizWlInternshipTraining detail = new BizWlInternshipTraining();
        detail.setItemId(item.getId());
        detail.setT(dto.getDays()); // 天数
        detail.setD(calcG3D(dto)); // 指导系数：理工 4.0 / 艺术 3.0 / 文史 2.0
        detail.setK(calcG3RepeatK(repeatOrder, dto)); // 重复系数：第一轮 1.0，第二轮起 0.9
        detail.setQ1(calcQ1(dto));
        detail.setQ2(calcQ2(dto));
        detail.setQ3(BigDecimal.ONE);
        wlInternshipTrainingMapper.insertBizWlInternshipTraining(detail);

        return calcStrategyFactory.get("G3").calculate(item, context);
    }

    /**
     * G4 课程设计：J4 × R4 × 0.4
     */
    private BigDecimal createG4Detail(TeachingTaskImportDTO dto, BizWorkloadItem item, CalculationPolicyContext context)
    {
        BizWlCourseDesign detail = new BizWlCourseDesign();
        detail.setItemId(item.getId());
        detail.setJ4(dto.getCredits()); // 学分
        detail.setR4(dto.getStudentCount() != null ? dto.getStudentCount().longValue() : 0L);
        wlCourseDesignMapper.insertBizWlCourseDesign(detail);

        return calcStrategyFactory.get("G4").calculate(item, context);
    }

    /**
     * G5 毕业论文：R5 × K5
     */
    private BigDecimal createG5Detail(TeachingTaskImportDTO dto, BizWorkloadItem item, CalculationPolicyContext context)
    {
        BizWlThesis detail = new BizWlThesis();
        detail.setItemId(item.getId());
        detail.setR5(dto.getStudentCount() != null ? dto.getStudentCount().longValue() : 0L);
        detail.setK5(calcG5K5(dto)); // K5：理工本 9 / 理工专 5 / 文史本 6 / 文史专 4
        // 学科门类随 K5 同口径落库（附件1 X/Y 双列分列用；艺术/其他按文史，同 calcG5K5 归档）
        detail.setDisciplineCategory(isLiGong(dto.getMajorCategory()) ? "SCITECH" : "LIBERAL_ARTS");
        wlThesisMapper.insertBizWlThesis(detail);

        return calcStrategyFactory.get("G5").calculate(item, context);
    }

    /**
     * G6 集中实习：W × R6 × 0.4
     */
    private BigDecimal createG6Detail(TeachingTaskImportDTO dto, BizWorkloadItem item, CalculationPolicyContext context)
    {
        BizWlConcentratedInternship detail = new BizWlConcentratedInternship();
        detail.setItemId(item.getId());
        detail.setW(dto.getWeeks()); // 周数
        detail.setR6(dto.getStudentCount() != null ? dto.getStudentCount().longValue() : 0L);
        wlConcentratedInternshipMapper.insertBizWlConcentratedInternship(detail);

        return calcStrategyFactory.get("G6").calculate(item, context);
    }

    // --- 系数计算辅助方法 ---

    /**
     * G1 理论课重复系数 C1：第一次 1.0，第二次 0.9，第三次及以后 0.8。
     * <p>
     * 取值走 {@code biz_workload_rule} 的 COEF_REPEAT_1ST / 2ND / 3RD_UP，
     * 政策调整改库即可，不必改代码。默认值与 02_biz_seed.sql 种子一致，
     * 仅在规则被误删时兜底，避免整批导入因缺一条参数而全数失败。
     *
     * @param repeatOrder 同名课第几次（≥1）
     */
    private BigDecimal calcC1(long repeatOrder, TeachingTaskImportDTO dto)
    {
        if (repeatOrder <= 1L)
        {
            return ruleParamService.get("COEF_REPEAT_1ST", new BigDecimal("1.0"), semesterCalendar.endOf(dto.getSemester()));
        }
        if (repeatOrder == 2L)
        {
            return ruleParamService.get("COEF_REPEAT_2ND", new BigDecimal("0.9"), semesterCalendar.endOf(dto.getSemester()));
        }
        return ruleParamService.get("COEF_REPEAT_3RD_UP", new BigDecimal("0.8"), semesterCalendar.endOf(dto.getSemester()));
    }

    /**
     * G3 实习实训重复系数 K：第一轮 1.0，第二轮及以后 0.9。
     * <p>
     * 与 G1 的 C1 不同，{@code else/工作量.md:67-68} 对 G3 只规定两档
     * （"从第二轮次重复系数K＝0.9"），没有第三轮 0.8 的说法，
     * 故第三轮及以后继续沿用 0.9，不套用 COEF_REPEAT_3RD_UP。
     *
     * @param repeatOrder 同一门实习实训第几轮（≥1）
     */
    private BigDecimal calcG3RepeatK(long repeatOrder, TeachingTaskImportDTO dto)
    {
        if (repeatOrder <= 1L)
        {
            return BigDecimal.ONE;
        }
        return ruleParamService.get("COEF_REPEAT_2ND", new BigDecimal("0.9"), semesterCalendar.endOf(dto.getSemester()));
    }

    /**
     * G2 实践课专业大类系数 K：理工类 1.0，其他专业 0.9（{@code else/工作量.md:57}）。
     * <p>
     * Excel「课程系数」列填了就以它为准（教务按个案覆盖），留空才按专业大类自动取值。
     * 此前留空一律落 1.0，等于把文史/艺术/其他专业的实践课都按理工计，多算 11%。
     */
    private BigDecimal calcG2K(TeachingTaskImportDTO dto)
    {
        if (dto.getCourseCoefficient() != null)
        {
            return dto.getCourseCoefficient();
        }
        return isLiGong(dto.getMajorCategory())
                ? ruleParamService.get("COEF_PRACTICE_LG", new BigDecimal("1.0"), semesterCalendar.endOf(dto.getSemester()))
                : ruleParamService.get("COEF_PRACTICE_OTHER", new BigDecimal("0.9"), semesterCalendar.endOf(dto.getSemester()));
    }

    /**
     * G3 实习实训指导系数 D：理工类 4.0，艺术类 3.0，文史类 2.0（{@code else/工作量.md:66}）。
     * <p>
     * 「单位指导 D=2.0」无法从导入列区分（模板没有「指导方式」列），只能由教务在
     * 「课程系数」列显式填 2.0 覆盖；本方法只负责按专业大类自动取值。
     * 专业大类为「其他」时按文史类取 2.0——文档未单列该档，取最低档避免多算。
     */
    private BigDecimal calcG3D(TeachingTaskImportDTO dto)
    {
        if (dto.getCourseCoefficient() != null)
        {
            return dto.getCourseCoefficient();
        }
        String category = dto.getMajorCategory();
        if (isLiGong(category))
        {
            return ruleParamService.get("COEF_TRAIN_D_LG", new BigDecimal("4.0"), semesterCalendar.endOf(dto.getSemester()));
        }
        if (category != null && category.contains("艺术"))
        {
            return ruleParamService.get("COEF_TRAIN_D_ART", new BigDecimal("3.0"), semesterCalendar.endOf(dto.getSemester()));
        }
        return ruleParamService.get("COEF_TRAIN_D_HUM", new BigDecimal("2.0"), semesterCalendar.endOf(dto.getSemester()));
    }

    /**
     * G5 毕业论文指导系数 K5：理工本 9 / 理工专 5 / 文史本 6 / 文史专 4
     * （{@code else/工作量.md:79-83}，取值见 {@code biz_workload_rule.COEF_THESIS_K5_*}）。
     * <p>
     * 艺术类按文史类取值——文档的 K5 只分理工/文史两支，艺术类归属待专业目录确认
     * （见 CLAUDE.md 待办 #5）；「其他」同理归文史，取低档避免多算。
     * <p>
     * 此前留空一律落 9（理工本科档），文史专科因此按 9 而非 4 计，多算 125%。
     */
    private BigDecimal calcG5K5(TeachingTaskImportDTO dto)
    {
        if (dto.getCourseCoefficient() != null)
        {
            return dto.getCourseCoefficient();
        }
        String discipline = isLiGong(dto.getMajorCategory()) ? "LG" : "HU";
        // 授课层次只有本科/专科两档，非「专科」一律按本科（与 createTeachingTask 默认「本科」一致）
        String level = "专科".equals(dto.getEducationLevel()) ? "C" : "B";
        BigDecimal fallback = defaultK5(discipline, level);
        return ruleParamService.get("COEF_THESIS_K5_" + discipline + "_" + level, fallback, semesterCalendar.endOf(dto.getSemester()));
    }

    /** K5 兜底默认值，与 02_biz_seed.sql 的 COEF_THESIS_K5_* 一致，仅在规则被误删时生效 */
    private BigDecimal defaultK5(String discipline, String level)
    {
        if ("LG".equals(discipline))
        {
            return "C".equals(level) ? new BigDecimal("5") : new BigDecimal("9");
        }
        return "C".equals(level) ? new BigDecimal("4") : new BigDecimal("6");
    }

    /**
     * 专业大类是否理工类。库中实际取值为 理工类/文史类/艺术类/其他，
     * 用 contains 而非 equals，容忍「理工」「理工科」等写法差异。
     */
    private boolean isLiGong(String majorCategory)
    {
        return majorCategory != null && majorCategory.contains("理工");
    }

    /**
     * K1 课程类型系数：必修 1.1，选修 1.0
     */
    private BigDecimal calcK1(TeachingTaskImportDTO dto)
    {
        if ("必修".equals(dto.getCourseNature()))
        {
            return new BigDecimal("1.1");
        }
        return new BigDecimal("1.0");
    }

    /**
     * Q1 教学质量系数：优秀/良好 1.0，不合格 0.8
     */
    private BigDecimal calcQ1(TeachingTaskImportDTO dto)
    {
        if ("不合格".equals(dto.getTeachingEval()))
        {
            return new BigDecimal("0.8");
        }
        return new BigDecimal("1.0");
    }

    /**
     * Q2 课程质量系数
     * 省级一流主持人 1.5，团队前3 1.2
     * 校级精品主持人 1.2，团队前3 1.1
     * 其他 1.0
     */
    private BigDecimal calcQ2(TeachingTaskImportDTO dto)
    {
        String level = dto.getCourseLevel();
        String role = dto.getCourseRole();

        if ("省级一流".equals(level))
        {
            if ("主持人".equals(role))
            {
                return new BigDecimal("1.5");
            }
            if ("团队前3".equals(role))
            {
                return new BigDecimal("1.2");
            }
        }
        if ("校级精品".equals(level))
        {
            if ("主持人".equals(role))
            {
                return new BigDecimal("1.2");
            }
            if ("团队前3".equals(role))
            {
                return new BigDecimal("1.1");
            }
        }
        return new BigDecimal("1.0");
    }

    /**
     * N 合堂系数：120-150 人 1.1，151 人及以上 1.2，否则 1.0。
     * <p>
     * 取值走 {@code biz_workload_rule} 的 COEF_CLASS_120_150 / COEF_CLASS_151_UP，
     * 与 C1 一致改库即可生效；人数档位阈值仍在代码里（规则表只存系数值，不存区间）。
     */
    private BigDecimal calcN(TeachingTaskImportDTO dto)
    {
        int count = dto.getStudentCount() != null ? dto.getStudentCount() : 0;
        if (count >= 151)
        {
            return ruleParamService.get("COEF_CLASS_151_UP", new BigDecimal("1.2"), semesterCalendar.endOf(dto.getSemester()));
        }
        if (count >= 120)
        {
            return ruleParamService.get("COEF_CLASS_120_150", new BigDecimal("1.1"), semesterCalendar.endOf(dto.getSemester()));
        }
        return BigDecimal.ONE;
    }

    // --- 工具方法 ---

    private boolean isG1(TeachingTaskImportDTO dto)
    {
        return "G1".equalsIgnoreCase(dto.getWorkloadType());
    }

    private boolean isG2(TeachingTaskImportDTO dto)
    {
        return "G2".equalsIgnoreCase(dto.getWorkloadType());
    }

    private String extractAcademicYear(String semester)
    {
        // "2025-2026-1" -> "2025-2026"
        if (semester != null && semester.length() >= 9)
        {
            return semester.substring(0, 9);
        }
        return semester;
    }

    /**
     * 层次归一化（用户拍板，2026-09-10）：专升本归「本科」；空值落「本科」。
     * 重修不进层次列（另有 is_retake 标志）。
     */
    private String normalizeEducationLevel(String level)
    {
        if (!StringUtils.hasText(level) || "专升本".equals(level))
        {
            return "本科";
        }
        return level;
    }

    private String defaultStr(String value, String defaultVal)
    {
        return StringUtils.hasText(value) ? value : defaultVal;
    }
}
