package com.workload.system.service.impl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.workload.common.exception.ServiceException;
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
import com.workload.system.domain.dto.TeachingTaskImportDTO;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.mapper.BizImportBatchMapper;
import com.workload.system.mapper.BizTeachingTaskMapper;
import com.workload.system.mapper.BizWlConcentratedInternshipMapper;
import com.workload.system.mapper.BizWlCourseDesignMapper;
import com.workload.system.mapper.BizWlInternshipTrainingMapper;
import com.workload.system.mapper.BizWlPracticeMapper;
import com.workload.system.mapper.BizWlTheoryMapper;
import com.workload.system.mapper.BizWlThesisMapper;
import com.workload.system.mapper.BizWorkloadItemMapper;
import com.workload.system.service.ITeachingTaskImportService;
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
    private CalcStrategyFactory calcStrategyFactory;

    @Autowired
    private RuleParamService ruleParamService;

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
        BigDecimal calculated = createDetailAndCalc(dto, item, repeatOrder);

        // 7. 触发策略后置回调：G5/G6 据此在 item 上置 is_over_limit 超标标记。
        // 漏调的后果是「是否超限」要等到下一次重算才落库，刚导完看列表和附件1 全显示未超限。
        calcStrategyFactory.get(dto.getWorkloadType().toUpperCase()).afterCalculated(item, calculated);
        item.setCalculatedWorkload(calculated);

        // 8. 固化不可变计算快照，与回写落在同一行导入事务：导入即产生可追溯的因子/来源/规则版本快照，
        // 而不必等下一次重算。快照 INSERT 与主表 UPDATE 同事务，任一失败整行回滚。
        FactorFormulaVo formula = factorFormulaService.build(item);
        if (formula == null)
        {
            throw new ServiceException("无法构建计算公式，明细子表可能缺失, itemId=" + item.getId());
        }
        item.setCalculationVersion(snapshotService.capture(item, formula,
                "RULE-" + dto.getSemester()).getCalculationVersion());
        item.setLastCalculatedAt(new Date());

        // 9. 回写计算结果（连同回调置上的 is_over_limit、快照版本/时间一并落库，共用同一条 UPDATE）
        workloadItemMapper.updateBizWorkloadItem(item);

        return calculated;
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
        if (dto.getBaseValue() == null || dto.getBaseValue().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("计划学时必须大于 0");
        }
        // 校验类别代码
        String type = dto.getWorkloadType().toUpperCase();
        if (!type.matches("G[1-6]"))
        {
            throw new ServiceException("工作量类别必须为 G1~G6，当前: " + dto.getWorkloadType());
        }
        // 重复次序为可选列；填了就必须是正整数，填 0 或负数属笔误，直接拒绝而非静默当 1
        if (dto.getRepeatOrder() != null && dto.getRepeatOrder() < 1)
        {
            throw new ServiceException("重复次序必须为不小于 1 的整数，当前: " + dto.getRepeatOrder());
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
        task.setTheoryHours(isG1(dto) ? dto.getBaseValue() : BigDecimal.ZERO);
        task.setPracticeHours(isG2(dto) ? dto.getBaseValue() : BigDecimal.ZERO);
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
    private BigDecimal createDetailAndCalc(TeachingTaskImportDTO dto, BizWorkloadItem item, long repeatOrder)
    {
        String type = dto.getWorkloadType().toUpperCase();

        switch (type)
        {
            case "G1":
                return createG1Detail(dto, item, repeatOrder);
            case "G2":
                return createG2Detail(dto, item);
            case "G3":
                return createG3Detail(dto, item, repeatOrder);
            case "G4":
                return createG4Detail(dto, item);
            case "G5":
                return createG5Detail(dto, item);
            case "G6":
                return createG6Detail(dto, item);
            default:
                throw new ServiceException("不支持的工作量类别: " + type);
        }
    }

    /**
     * G1 理论课：J1 × C1 × K1 × Q1 × Q2 × Q3 × N
     */
    private BigDecimal createG1Detail(TeachingTaskImportDTO dto, BizWorkloadItem item, long repeatOrder)
    {
        BizWlTheory detail = new BizWlTheory();
        detail.setItemId(item.getId());
        detail.setJ1(dto.getBaseValue());
        detail.setC1(calcC1(repeatOrder)); // 重复系数：按同名课第几次，1.0/0.9/0.8
        detail.setK1(calcK1(dto)); // 课程类型系数
        detail.setQ1(calcQ1(dto)); // 教学质量系数
        detail.setQ2(calcQ2(dto)); // 课程质量系数
        detail.setQ3(BigDecimal.ONE); // 全外文系数默认 1.0
        detail.setN(calcN(dto));   // 合堂系数
        wlTheoryMapper.insertBizWlTheory(detail);

        // 策略计算（G1 策略必配；未配/配错时 CalcStrategyFactory 抛异常，由外层记为该行错误）
        return calcStrategyFactory.get("G1").calculate(item);
    }

    /**
     * G2 实践课：J2 × K × C2 × Q1 × Q2
     */
    private BigDecimal createG2Detail(TeachingTaskImportDTO dto, BizWorkloadItem item)
    {
        BizWlPractice detail = new BizWlPractice();
        detail.setItemId(item.getId());
        detail.setJ2(dto.getBaseValue());
        detail.setK(calcG2K(dto)); // 专业大类系数：理工 1.0 / 其他 0.9
        detail.setC2(new BigDecimal("0.9")); // 实践课重复系数固定 0.9
        detail.setQ1(calcQ1(dto));
        detail.setQ2(calcQ2(dto));
        detail.setQ3(BigDecimal.ONE);
        wlPracticeMapper.insertBizWlPractice(detail);

        return calcStrategyFactory.get("G2").calculate(item);
    }

    /**
     * G3 实习实训：T × D × K × Q1 × Q2
     */
    private BigDecimal createG3Detail(TeachingTaskImportDTO dto, BizWorkloadItem item, long repeatOrder)
    {
        BizWlInternshipTraining detail = new BizWlInternshipTraining();
        detail.setItemId(item.getId());
        detail.setT(dto.getBaseValue()); // 天数
        detail.setD(calcG3D(dto)); // 指导系数：理工 4.0 / 艺术 3.0 / 文史 2.0
        detail.setK(calcG3RepeatK(repeatOrder)); // 重复系数：第一轮 1.0，第二轮起 0.9
        detail.setQ1(calcQ1(dto));
        detail.setQ2(calcQ2(dto));
        detail.setQ3(BigDecimal.ONE);
        wlInternshipTrainingMapper.insertBizWlInternshipTraining(detail);

        return calcStrategyFactory.get("G3").calculate(item);
    }

    /**
     * G4 课程设计：J4 × R4 × 0.4
     */
    private BigDecimal createG4Detail(TeachingTaskImportDTO dto, BizWorkloadItem item)
    {
        BizWlCourseDesign detail = new BizWlCourseDesign();
        detail.setItemId(item.getId());
        detail.setJ4(dto.getBaseValue()); // 学分
        detail.setR4(dto.getStudentCount() != null ? dto.getStudentCount().longValue() : 0L);
        wlCourseDesignMapper.insertBizWlCourseDesign(detail);

        return calcStrategyFactory.get("G4").calculate(item);
    }

    /**
     * G5 毕业论文：R5 × K5
     */
    private BigDecimal createG5Detail(TeachingTaskImportDTO dto, BizWorkloadItem item)
    {
        BizWlThesis detail = new BizWlThesis();
        detail.setItemId(item.getId());
        detail.setR5(dto.getStudentCount() != null ? dto.getStudentCount().longValue() : 0L);
        detail.setK5(calcG5K5(dto)); // K5：理工本 9 / 理工专 5 / 文史本 6 / 文史专 4
        // 学科门类随 K5 同口径落库（附件1 X/Y 双列分列用；艺术/其他按文史，同 calcG5K5 归档）
        detail.setDisciplineCategory(isLiGong(dto.getMajorCategory()) ? "SCITECH" : "LIBERAL_ARTS");
        wlThesisMapper.insertBizWlThesis(detail);

        return calcStrategyFactory.get("G5").calculate(item);
    }

    /**
     * G6 集中实习：W × R6 × 0.4
     */
    private BigDecimal createG6Detail(TeachingTaskImportDTO dto, BizWorkloadItem item)
    {
        BizWlConcentratedInternship detail = new BizWlConcentratedInternship();
        detail.setItemId(item.getId());
        detail.setW(dto.getBaseValue()); // 周数
        detail.setR6(dto.getStudentCount() != null ? dto.getStudentCount().longValue() : 0L);
        wlConcentratedInternshipMapper.insertBizWlConcentratedInternship(detail);

        return calcStrategyFactory.get("G6").calculate(item);
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
    private BigDecimal calcC1(long repeatOrder)
    {
        if (repeatOrder <= 1L)
        {
            return ruleParamService.get("COEF_REPEAT_1ST", new BigDecimal("1.0"));
        }
        if (repeatOrder == 2L)
        {
            return ruleParamService.get("COEF_REPEAT_2ND", new BigDecimal("0.9"));
        }
        return ruleParamService.get("COEF_REPEAT_3RD_UP", new BigDecimal("0.8"));
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
    private BigDecimal calcG3RepeatK(long repeatOrder)
    {
        if (repeatOrder <= 1L)
        {
            return BigDecimal.ONE;
        }
        return ruleParamService.get("COEF_REPEAT_2ND", new BigDecimal("0.9"));
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
                ? ruleParamService.get("COEF_PRACTICE_LG", new BigDecimal("1.0"))
                : ruleParamService.get("COEF_PRACTICE_OTHER", new BigDecimal("0.9"));
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
            return ruleParamService.get("COEF_TRAIN_D_LG", new BigDecimal("4.0"));
        }
        if (category != null && category.contains("艺术"))
        {
            return ruleParamService.get("COEF_TRAIN_D_ART", new BigDecimal("3.0"));
        }
        return ruleParamService.get("COEF_TRAIN_D_HUM", new BigDecimal("2.0"));
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
        return ruleParamService.get("COEF_THESIS_K5_" + discipline + "_" + level, fallback);
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
            return ruleParamService.get("COEF_CLASS_151_UP", new BigDecimal("1.2"));
        }
        if (count >= 120)
        {
            return ruleParamService.get("COEF_CLASS_120_150", new BigDecimal("1.1"));
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
