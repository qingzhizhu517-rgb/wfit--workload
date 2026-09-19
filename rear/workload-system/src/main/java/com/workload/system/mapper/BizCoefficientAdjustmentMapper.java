package com.workload.system.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.workload.system.domain.BizCoefficientAdjustment;

/**
 * G1/G2 系数调整申请Mapper接口
 *
 * @author wflg
 * @date 2026-09-12
 */
public interface BizCoefficientAdjustmentMapper
{
    /**
     * 查询系数调整申请
     */
    public BizCoefficientAdjustment selectCoefficientAdjustmentById(Long id);

    /**
     * 查询系数调整申请列表
     */
    public List<BizCoefficientAdjustment> selectCoefficientAdjustmentList(BizCoefficientAdjustment adjustment);

    /**
     * 新增系数调整申请
     */
    public int insertCoefficientAdjustment(BizCoefficientAdjustment adjustment);

    /**
     * 统计某明细某系数当前处于待审(PENDING)的申请数（先查再插，配合唯一键双保险）。
     */
    public int countPendingByItemFactor(@Param("itemId") Long itemId, @Param("factorCode") String factorCode);

    /**
     * 服务端读取指定明细某系数的当前值，作为申请的 old_value。
     * <p>只读；用 &lt;choose&gt; 白名单映射到固定列名，绝不用 ${} 拼列名。</p>
     */
    public BigDecimal selectCurrentFactorValue(@Param("category") String category,
            @Param("factorCode") String factorCode, @Param("itemId") Long itemId);

    /**
     * 条件通过：仅当仍为待审且计算版本未变时置为 APPROVED（乐观并发，返回受影响行数）。
     */
    public int markApprovedIfPending(@Param("id") Long id, @Param("reviewerId") Long reviewerId,
            @Param("reviewReason") String reviewReason, @Param("baseVersion") Long baseVersion);

    /**
     * 条件驳回：仅当仍为待审且计算版本未变时置为 REJECTED（乐观并发，返回受影响行数）。
     */
    public int markRejectedIfPending(@Param("id") Long id, @Param("reviewerId") Long reviewerId,
            @Param("reviewReason") String reviewReason, @Param("baseVersion") Long baseVersion);
}
