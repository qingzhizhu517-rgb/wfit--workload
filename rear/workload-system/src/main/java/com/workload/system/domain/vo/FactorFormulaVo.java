package com.workload.system.domain.vo;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 工作量计算公式及因子说明。
 */
public class FactorFormulaVo
{
    private String formulaType;
    private String expression;
    private BigDecimal result;
    private String description;
    private boolean reproducible;
    /** 旧数据无快照、只能读回子表时为 true：此时展示可信但无法逐因子复现历史核算 */
    private boolean legacy;
    /** 命中的最新计算快照版本；无快照为 null */
    private Long snapshotVersion;
    /** 命中的快照内容哈希；无快照为 null */
    private String snapshotHash;
    /** 快照固化时间；无快照为 null */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date calculatedAt;
    /** 源数据（教学任务）快照，仅只读展示，字段不回写主表 */
    private SourceTaskVo sourceTask;
    private List<FactorVo> factors;

    public FactorFormulaVo(String formulaType, String expression, BigDecimal result,
            String description, List<FactorVo> factors)
    {
        this(formulaType, expression, result, description, true, factors);
    }

    public FactorFormulaVo(String formulaType, String expression, BigDecimal result,
            String description, boolean reproducible, List<FactorVo> factors)
    {
        this.formulaType = formulaType;
        this.expression = expression;
        this.result = result;
        this.description = description;
        this.reproducible = reproducible;
        this.legacy = false;
        this.factors = factors;
    }

    public String getFormulaType()
    {
        return formulaType;
    }

    public String getExpression()
    {
        return expression;
    }

    public BigDecimal getResult()
    {
        return result;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isReproducible()
    {
        return reproducible;
    }

    public void setReproducible(boolean reproducible)
    {
        this.reproducible = reproducible;
    }

    public boolean isLegacy()
    {
        return legacy;
    }

    public void setLegacy(boolean legacy)
    {
        this.legacy = legacy;
    }

    public Long getSnapshotVersion()
    {
        return snapshotVersion;
    }

    public void setSnapshotVersion(Long snapshotVersion)
    {
        this.snapshotVersion = snapshotVersion;
    }

    public String getSnapshotHash()
    {
        return snapshotHash;
    }

    public void setSnapshotHash(String snapshotHash)
    {
        this.snapshotHash = snapshotHash;
    }

    public Date getCalculatedAt()
    {
        return calculatedAt;
    }

    public void setCalculatedAt(Date calculatedAt)
    {
        this.calculatedAt = calculatedAt;
    }

    public SourceTaskVo getSourceTask()
    {
        return sourceTask;
    }

    public void setSourceTask(SourceTaskVo sourceTask)
    {
        this.sourceTask = sourceTask;
    }

    public List<FactorVo> getFactors()
    {
        return factors;
    }

    /**
     * 源数据（教学任务）只读快照：解释一条明细的因子取值依据的原始课程信息。
     * 字段来自 biz_teaching_task，仅用于详情展示，不复制回 biz_workload_item 主表。
     */
    public static class SourceTaskVo
    {
        /** 课程级别（省级一流/校级精品/其他）—— 影响 Q2 */
        private String courseLevel;
        /** 课程角色（主持人/团队前3/独立） */
        private String courseRole;
        /** 课程性质（必修/选修）—— 影响 K1 */
        private String courseNature;
        /** 教学评价（导入时用于派生 Q1，未持久化到任务表时为 null） */
        private String teachingEval;
        /** 班级：区分同名课不同班次 */
        private String className;
        /** 同名课第几次开课（1/2/3+）—— 影响 C1 */
        private Long repeatOrder;

        public String getCourseLevel()
        {
            return courseLevel;
        }

        public void setCourseLevel(String courseLevel)
        {
            this.courseLevel = courseLevel;
        }

        public String getCourseRole()
        {
            return courseRole;
        }

        public void setCourseRole(String courseRole)
        {
            this.courseRole = courseRole;
        }

        public String getCourseNature()
        {
            return courseNature;
        }

        public void setCourseNature(String courseNature)
        {
            this.courseNature = courseNature;
        }

        public String getTeachingEval()
        {
            return teachingEval;
        }

        public void setTeachingEval(String teachingEval)
        {
            this.teachingEval = teachingEval;
        }

        public String getClassName()
        {
            return className;
        }

        public void setClassName(String className)
        {
            this.className = className;
        }

        public Long getRepeatOrder()
        {
            return repeatOrder;
        }

        public void setRepeatOrder(Long repeatOrder)
        {
            this.repeatOrder = repeatOrder;
        }
    }

    /** 单个公式因子。 */
    public static class FactorVo
    {
        private String key;
        private Object value;
        private String source;
        private String description;
        private String status;
        /** 因子取值所依据的规则键（如 COEF_REPEAT_1ST），无则为空 */
        private String ruleCode;
        /** 取值时的规则版本，历史快照据此复现 */
        private String ruleVersion;
        /** 来源引用：导入值/审批单/岗位来源批次等的可追溯标识 */
        private String sourceRef;

        public FactorVo(String key, Object value, String source, String description, String status)
        {
            this.key = key;
            this.value = value;
            this.source = source;
            this.description = description;
            this.status = status;
        }

        public String getKey()
        {
            return key;
        }

        public Object getValue()
        {
            return value;
        }

        public void setValue(Object value)
        {
            this.value = value;
        }

        public String getSource()
        {
            return source;
        }

        public void setSource(String source)
        {
            this.source = source;
        }

        public String getDescription()
        {
            return description;
        }

        public String getStatus()
        {
            return status;
        }

        public String getRuleCode()
        {
            return ruleCode;
        }

        public void setRuleCode(String ruleCode)
        {
            this.ruleCode = ruleCode;
        }

        public String getRuleVersion()
        {
            return ruleVersion;
        }

        public void setRuleVersion(String ruleVersion)
        {
            this.ruleVersion = ruleVersion;
        }

        public String getSourceRef()
        {
            return sourceRef;
        }

        public void setSourceRef(String sourceRef)
        {
            this.sourceRef = sourceRef;
        }
    }
}
