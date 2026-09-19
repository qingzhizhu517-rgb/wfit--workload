package com.workload.system.service;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * G1/G2 系数调整申请Service接口
 *
 * <p>只负责申请与审批状态流转；通过后原子应用系数并重算由 Task 9 负责。</p>
 *
 * @author wflg
 * @date 2026-09-12
 */
public interface ICoefficientAdjustmentService
{
    /**
     * 提交系数调整申请。
     * <p>old_value 由服务端从当前值读取，不接受客户端传入；同一 item+factor 同时只能有一条待审。</p>
     *
     * @param request 申请入参
     * @return 新建申请主键
     */
    Long submit(CoefficientAdjustmentRequest request);

    /**
     * 通过申请（乐观并发；仅置 APPROVED，不改 G 子表、不重算）。
     */
    void approve(Long id, String reviewReason);

    /**
     * 驳回申请（reviewReason 必填）。
     */
    void reject(Long id, String reviewReason);

    /**
     * 系数调整申请入参 DTO。
     * <p>作为接口内部类，保持本任务 diff 收敛在既定文件集内；oldValue 字段仅用于证明其被服务端忽略。</p>
     */
    class CoefficientAdjustmentRequest implements Serializable
    {
        private static final long serialVersionUID = 1L;

        /** 关联工作量明细 FK biz_workload_item */
        private Long itemId;

        /** 工作量类别：仅 G1/G2 */
        private String category;

        /** 系数编码 */
        private String factorCode;

        /** 申请调整为的系数值 */
        private BigDecimal requestedValue;

        /** 申请理由 */
        private String reason;

        /** 佐证材料地址 */
        private String attachmentUrl;

        /** 客户端可能传入的旧值——服务端一律忽略并以当前值覆盖 */
        private BigDecimal oldValue;

        public Long getItemId()
        {
            return itemId;
        }

        public void setItemId(Long itemId)
        {
            this.itemId = itemId;
        }

        public String getCategory()
        {
            return category;
        }

        public void setCategory(String category)
        {
            this.category = category;
        }

        public String getFactorCode()
        {
            return factorCode;
        }

        public void setFactorCode(String factorCode)
        {
            this.factorCode = factorCode;
        }

        public BigDecimal getRequestedValue()
        {
            return requestedValue;
        }

        public void setRequestedValue(BigDecimal requestedValue)
        {
            this.requestedValue = requestedValue;
        }

        public String getReason()
        {
            return reason;
        }

        public void setReason(String reason)
        {
            this.reason = reason;
        }

        public String getAttachmentUrl()
        {
            return attachmentUrl;
        }

        public void setAttachmentUrl(String attachmentUrl)
        {
            this.attachmentUrl = attachmentUrl;
        }

        public BigDecimal getOldValue()
        {
            return oldValue;
        }

        public void setOldValue(BigDecimal oldValue)
        {
            this.oldValue = oldValue;
        }
    }
}
