package com.workload.system.domain.vo;

import java.util.ArrayList;
import java.util.List;

import com.workload.system.domain.BizPayRecord;
import com.workload.system.domain.BizWorkloadSummary;

/**
 * 阶段化一键核算结果：把「校验→同步 G11→重算明细→重算汇总→重算酬金」五个阶段
 * 逐一暴露，前端据此分阶段展示，不再固定显示「0 条」。
 */
public class CalculationRunResult
{
    /** 校验阶段 */
    public static final String STAGE_VALIDATE = "VALIDATE";
    /** 同步教务岗位减免到 G11 阶段 */
    public static final String STAGE_GENERATE_G11 = "GENERATE_G11";
    /** 重算明细阶段 */
    public static final String STAGE_RECALC_ITEMS = "RECALC_ITEMS";
    /** 重算汇总阶段 */
    public static final String STAGE_RECALC_SUMMARY = "RECALC_SUMMARY";
    /** 重算酬金阶段 */
    public static final String STAGE_RECALC_PAY = "RECALC_PAY";

    private Long userId;
    private String semester;
    private boolean includeG11;

    /** 各阶段结果，按执行顺序排列 */
    private final List<StageResult> stages = new ArrayList<>();

    /** 同步生成/更新的 G11 条数 */
    private int generatedG11Count;
    /** 重算的明细条数 */
    private int recalcItemCount;
    /** 尚未核对的明细条数（提示用） */
    private int unconfirmedCount;

    private BizWorkloadSummary summary;
    private BizPayRecord payRecord;

    public CalculationRunResult()
    {
    }

    public CalculationRunResult(Long userId, String semester, boolean includeG11)
    {
        this.userId = userId;
        this.semester = semester;
        this.includeG11 = includeG11;
    }

    /** 追加一个阶段结果并返回它本身，便于链式回填。 */
    public StageResult addStage(String code, boolean ok, int count, String message)
    {
        StageResult stage = new StageResult(code, ok, count, message);
        stages.add(stage);
        return stage;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getSemester()
    {
        return semester;
    }

    public void setSemester(String semester)
    {
        this.semester = semester;
    }

    public boolean isIncludeG11()
    {
        return includeG11;
    }

    public void setIncludeG11(boolean includeG11)
    {
        this.includeG11 = includeG11;
    }

    public List<StageResult> getStages()
    {
        return stages;
    }

    public int getGeneratedG11Count()
    {
        return generatedG11Count;
    }

    public void setGeneratedG11Count(int generatedG11Count)
    {
        this.generatedG11Count = generatedG11Count;
    }

    public int getRecalcItemCount()
    {
        return recalcItemCount;
    }

    public void setRecalcItemCount(int recalcItemCount)
    {
        this.recalcItemCount = recalcItemCount;
    }

    public int getUnconfirmedCount()
    {
        return unconfirmedCount;
    }

    public void setUnconfirmedCount(int unconfirmedCount)
    {
        this.unconfirmedCount = unconfirmedCount;
    }

    public BizWorkloadSummary getSummary()
    {
        return summary;
    }

    public void setSummary(BizWorkloadSummary summary)
    {
        this.summary = summary;
    }

    public BizPayRecord getPayRecord()
    {
        return payRecord;
    }

    public void setPayRecord(BizPayRecord payRecord)
    {
        this.payRecord = payRecord;
    }

    /** 单个阶段结果。 */
    public static class StageResult
    {
        private String code;
        private boolean ok;
        private int count;
        private String message;

        public StageResult()
        {
        }

        public StageResult(String code, boolean ok, int count, String message)
        {
            this.code = code;
            this.ok = ok;
            this.count = count;
            this.message = message;
        }

        public String getCode()
        {
            return code;
        }

        public void setCode(String code)
        {
            this.code = code;
        }

        public boolean isOk()
        {
            return ok;
        }

        public void setOk(boolean ok)
        {
            this.ok = ok;
        }

        public int getCount()
        {
            return count;
        }

        public void setCount(int count)
        {
            this.count = count;
        }

        public String getMessage()
        {
            return message;
        }

        public void setMessage(String message)
        {
            this.message = message;
        }
    }
}
