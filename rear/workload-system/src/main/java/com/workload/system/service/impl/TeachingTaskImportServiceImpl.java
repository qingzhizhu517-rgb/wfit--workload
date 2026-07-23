package com.workload.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.workload.common.exception.ServiceException;
import com.workload.common.utils.excel.ImportResult;
import com.workload.system.calc.strategy.CalcStrategyFactory;
import com.workload.system.calc.strategy.WorkloadCalcStrategy;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importTeachingTasks(List<TeachingTaskImportDTO> rows, String fileName)
    {
        ImportResult result = new ImportResult();
        String batchNo = "IMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 创建导入批次记录
        BizImportBatch batch = new BizImportBatch();
        batch.setBatchNo(batchNo);
        batch.setImportType("TEACHING_TASK");
        batch.setFileName(fileName);
        batch.setTotalCount((long) rows.size());
        batch.setStatus(0); // 处理中
        importBatchMapper.insertBizImportBatch(batch);
        result.setBatchId(batch.getId());

        for (int i = 0; i < rows.size(); i++)
        {
            try
            {
                processSingleRow(rows.get(i), batchNo);
                result.addSuccess();
            }
            catch (Exception e)
            {
                log.warn("导入第 {} 行失败: {}", i + 2, e.getMessage());
                result.addError(i + 2, e.getMessage());
            }
        }

        // 更新批次记录
        batch.setSuccessCount((long) result.getSuccessCount());
        batch.setFailCount((long) result.getFailCount());
        batch.setStatus(result.hasErrors() ? 2 : 1); // 1=完成 2=部分失败
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
        // 1. 校验必填字段
        validateRow(dto);

        // 2. 查找教师
        SysUser user = findUser(dto.getUserCode());

        // 3. 创建教学任务
        BizTeachingTask task = createTeachingTask(dto, user.getUserId(), batchNo);
        teachingTaskMapper.insertBizTeachingTask(task);

        // 4. 创建工作量明细主表
        BizWorkloadItem item = createWorkloadItem(dto, user.getUserId(), task.getId());
        workloadItemMapper.insertBizWorkloadItem(item);

        // 5. 创建类别明细并计算
        BigDecimal calculated = createDetailAndCalc(dto, item);

        // 6. 回写计算结果
        item.setCalculatedWorkload(calculated);
        workloadItemMapper.updateBizWorkloadItem(item);

        return calculated;
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
    private BizTeachingTask createTeachingTask(TeachingTaskImportDTO dto, Long userId, String batchNo)
    {
        BizTeachingTask task = new BizTeachingTask();
        task.setUserId(userId);
        task.setSemester(dto.getSemester());
        task.setAcademicYear(extractAcademicYear(dto.getSemester()));
        task.setCourseName(dto.getCourseName());
        task.setCourseCode(dto.getCourseCode());
        task.setEducationLevel(defaultStr(dto.getEducationLevel(), "本科"));
        task.setMajorCategory(defaultStr(dto.getMajorCategory(), "理工类"));
        task.setCourseNature(defaultStr(dto.getCourseNature(), "必修"));
        task.setCourseLevel(defaultStr(dto.getCourseLevel(), "其他"));
        task.setCourseRole(defaultStr(dto.getCourseRole(), "独立"));
        task.setStudentCount(dto.getStudentCount() != null ? dto.getStudentCount().longValue() : 0L);
        task.setTheoryHours(isG1(dto) ? dto.getBaseValue() : BigDecimal.ZERO);
        task.setPracticeHours(isG2(dto) ? dto.getBaseValue() : BigDecimal.ZERO);
        task.setRepeatOrder(1L); // 默认第一次，后续可由用户手动调整
        task.setImportSource("EXCEL");
        task.setImportBatch(batchNo);
        task.setImportTime(new Date());
        task.setStatus(0);
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
        item.setEducationLevel(defaultStr(dto.getEducationLevel(), "本科"));
        item.setMajorCategory(defaultStr(dto.getMajorCategory(), "理工类"));
        item.setCalculatedWorkload(BigDecimal.ZERO);
        item.setStatus(0);
        return item;
    }

    /**
     * 创建类别明细并调用策略计算
     */
    private BigDecimal createDetailAndCalc(TeachingTaskImportDTO dto, BizWorkloadItem item)
    {
        String type = dto.getWorkloadType().toUpperCase();

        switch (type)
        {
            case "G1":
                return createG1Detail(dto, item);
            case "G2":
                return createG2Detail(dto, item);
            case "G3":
                return createG3Detail(dto, item);
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
    private BigDecimal createG1Detail(TeachingTaskImportDTO dto, BizWorkloadItem item)
    {
        BizWlTheory detail = new BizWlTheory();
        detail.setItemId(item.getId());
        detail.setJ1(dto.getBaseValue());
        detail.setC1(calcC1(dto)); // 重复系数：根据同名课次数
        detail.setK1(calcK1(dto)); // 课程类型系数
        detail.setQ1(calcQ1(dto)); // 教学质量系数
        detail.setQ2(calcQ2(dto)); // 课程质量系数
        detail.setQ3(BigDecimal.ONE); // 全外文系数默认 1.0
        detail.setN(calcN(dto));   // 合堂系数
        wlTheoryMapper.insertBizWlTheory(detail);

        // 调用策略计算
        WorkloadCalcStrategy strategy = calcStrategyFactory.get("G1");
        if (strategy != null)
        {
            return strategy.calculate(item);
        }
        // 手动计算兜底
        return detail.getJ1().multiply(detail.getC1()).multiply(detail.getK1())
                .multiply(detail.getQ1()).multiply(detail.getQ2()).multiply(detail.getQ3())
                .multiply(detail.getN()).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * G2 实践课：J2 × K × C2 × Q1 × Q2
     */
    private BigDecimal createG2Detail(TeachingTaskImportDTO dto, BizWorkloadItem item)
    {
        BizWlPractice detail = new BizWlPractice();
        detail.setItemId(item.getId());
        detail.setJ2(dto.getBaseValue());
        detail.setK(dto.getCourseCoefficient() != null ? dto.getCourseCoefficient() : BigDecimal.ONE);
        detail.setC2(new BigDecimal("0.9")); // 实践课重复系数固定 0.9
        detail.setQ1(calcQ1(dto));
        detail.setQ2(calcQ2(dto));
        detail.setQ3(BigDecimal.ONE);
        wlPracticeMapper.insertBizWlPractice(detail);

        WorkloadCalcStrategy strategy = calcStrategyFactory.get("G2");
        if (strategy != null)
        {
            return strategy.calculate(item);
        }
        return detail.getJ2().multiply(detail.getK()).multiply(detail.getC2())
                .multiply(detail.getQ1()).multiply(detail.getQ2())
                .multiply(detail.getQ3()).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * G3 实习实训：T × D × K × Q1 × Q2
     */
    private BigDecimal createG3Detail(TeachingTaskImportDTO dto, BizWorkloadItem item)
    {
        BizWlInternshipTraining detail = new BizWlInternshipTraining();
        detail.setItemId(item.getId());
        detail.setT(dto.getBaseValue()); // 天数
        detail.setD(dto.getCourseCoefficient() != null ? dto.getCourseCoefficient() : new BigDecimal("4.0"));
        detail.setK(BigDecimal.ONE);
        detail.setQ1(calcQ1(dto));
        detail.setQ2(calcQ2(dto));
        detail.setQ3(BigDecimal.ONE);
        wlInternshipTrainingMapper.insertBizWlInternshipTraining(detail);

        WorkloadCalcStrategy strategy = calcStrategyFactory.get("G3");
        if (strategy != null)
        {
            return strategy.calculate(item);
        }
        return detail.getT().multiply(detail.getD()).multiply(detail.getK())
                .multiply(detail.getQ1()).multiply(detail.getQ2())
                .multiply(detail.getQ3()).setScale(2, RoundingMode.HALF_UP);
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

        WorkloadCalcStrategy strategy = calcStrategyFactory.get("G4");
        if (strategy != null)
        {
            return strategy.calculate(item);
        }
        return detail.getJ4().multiply(new BigDecimal(detail.getR4()))
                .multiply(new BigDecimal("0.4")).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * G5 毕业论文：R5 × K5
     */
    private BigDecimal createG5Detail(TeachingTaskImportDTO dto, BizWorkloadItem item)
    {
        BizWlThesis detail = new BizWlThesis();
        detail.setItemId(item.getId());
        detail.setR5(dto.getStudentCount() != null ? dto.getStudentCount().longValue() : 0L);
        detail.setK5(dto.getCourseCoefficient() != null ? dto.getCourseCoefficient() : new BigDecimal("9"));
        wlThesisMapper.insertBizWlThesis(detail);

        WorkloadCalcStrategy strategy = calcStrategyFactory.get("G5");
        if (strategy != null)
        {
            return strategy.calculate(item);
        }
        return new BigDecimal(detail.getR5()).multiply(detail.getK5())
                .setScale(2, RoundingMode.HALF_UP);
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

        WorkloadCalcStrategy strategy = calcStrategyFactory.get("G6");
        if (strategy != null)
        {
            return strategy.calculate(item);
        }
        return detail.getW().multiply(new BigDecimal(detail.getR6()))
                .multiply(new BigDecimal("0.4")).setScale(2, RoundingMode.HALF_UP);
    }

    // --- 系数计算辅助方法 ---

    /**
     * C1 重复系数：根据同名课第几次
     * 第一次 1.0，第二次 0.9，第三次及以后 0.8
     */
    private BigDecimal calcC1(TeachingTaskImportDTO dto)
    {
        // 默认第一次，后续可通过 repeatOrder 字段或查重逻辑确定
        return new BigDecimal("1.0");
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
     * N 合堂系数：120-150人 1.1，151+人 1.2，否则 1.0
     */
    private BigDecimal calcN(TeachingTaskImportDTO dto)
    {
        int count = dto.getStudentCount() != null ? dto.getStudentCount() : 0;
        if (count >= 151)
        {
            return new BigDecimal("1.2");
        }
        if (count >= 120)
        {
            return new BigDecimal("1.1");
        }
        return new BigDecimal("1.0");
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

    private String defaultStr(String value, String defaultVal)
    {
        return StringUtils.hasText(value) ? value : defaultVal;
    }
}
