package com.workload.system.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.workload.system.domain.BizWlTheory;

/**
 * G1理论课明细Mapper接口
 *
 * @author wflg
 * @date 2026-07-20
 */
public interface BizWlTheoryMapper
{
    /**
     * 查询G1理论课明细
     * 
     * @param itemId G1理论课明细主键
     * @return G1理论课明细
     */
    public BizWlTheory selectBizWlTheoryByItemId(Long itemId);

    /**
     * 查询G1理论课明细列表
     * 
     * @param bizWlTheory G1理论课明细
     * @return G1理论课明细集合
     */
    public List<BizWlTheory> selectBizWlTheoryList(BizWlTheory bizWlTheory);

    /**
     * 新增G1理论课明细
     * 
     * @param bizWlTheory G1理论课明细
     * @return 结果
     */
    public int insertBizWlTheory(BizWlTheory bizWlTheory);

    /**
     * 修改G1理论课明细
     * 
     * @param bizWlTheory G1理论课明细
     * @return 结果
     */
    public int updateBizWlTheory(BizWlTheory bizWlTheory);

    /**
     * 删除G1理论课明细
     * 
     * @param itemId G1理论课明细主键
     * @return 结果
     */
    public int deleteBizWlTheoryByItemId(Long itemId);

    /**
     * 批量删除G1理论课明细
     *
     * @param itemIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBizWlTheoryByItemIds(Long[] itemIds);

    // ---- Task 9：审批通过后原子应用单个系数（JOIN 主表校验版本/状态/汇总冻结门，影响 1 行才成功） ----
    // 白名单列名由 Java switch 明确分发到独立方法，绝不用 ${} 拼列名。

    /** 条件更新 C1（第 n 次重复系数）。 */
    int updateC1IfVersion(@Param("itemId") Long itemId, @Param("value") BigDecimal value,
            @Param("baseVersion") Long baseVersion);

    /** 条件更新 K1（必修/选修系数）。 */
    int updateK1IfVersion(@Param("itemId") Long itemId, @Param("value") BigDecimal value,
            @Param("baseVersion") Long baseVersion);

    /** 条件更新 Q1（质量系数一）。 */
    int updateQ1IfVersion(@Param("itemId") Long itemId, @Param("value") BigDecimal value,
            @Param("baseVersion") Long baseVersion);

    /** 条件更新 Q2（质量系数二）。 */
    int updateQ2IfVersion(@Param("itemId") Long itemId, @Param("value") BigDecimal value,
            @Param("baseVersion") Long baseVersion);

    /** 条件更新 N（合堂系数）。 */
    int updateNIfVersion(@Param("itemId") Long itemId, @Param("value") BigDecimal value,
            @Param("baseVersion") Long baseVersion);
}
