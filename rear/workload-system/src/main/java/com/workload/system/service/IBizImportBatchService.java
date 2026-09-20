package com.workload.system.service;

import java.util.List;
import com.workload.system.domain.BizImportBatch;
import com.workload.system.domain.dto.importing.ImportRowView;

/**
 * 导入批次记录Service接口
 *
 * @author wflg
 * @date 2026-07-20
 */
public interface IBizImportBatchService
{
    /**
     * 查询导入批次记录
     *
     * @param id 导入批次记录主键
     * @return 导入批次记录
     */
    public BizImportBatch selectBizImportBatchById(Long id);

    /**
     * 查询批次暂存行视图（按 row_no 升序）。
     * <p>
     * 仅返回白名单字段（行号/工号/姓名/状态/错误码/错误信息），
     * 从 {@code normalized_json} 解析而来，绝不携带 {@code raw_json} 中的敏感原始列。
     * </p>
     *
     * @param batchId 批次ID
     * @param status  行状态过滤（可空，为空返回全部行）
     * @return 行视图集合
     */
    public List<ImportRowView> selectRowViews(Long batchId, String status);

    /**
     * 查询批次的错误行（BLOCKED/FAILED）视图，供错误清单导出。
     *
     * @param batchId 批次ID
     * @return 错误行视图集合
     */
    public List<ImportRowView> selectErrorRows(Long batchId);

    /**
     * 查询导入批次记录列表
     * 
     * @param bizImportBatch 导入批次记录
     * @return 导入批次记录集合
     */
    public List<BizImportBatch> selectBizImportBatchList(BizImportBatch bizImportBatch);

    /**
     * 新增导入批次记录
     * 
     * @param bizImportBatch 导入批次记录
     * @return 结果
     */
    public int insertBizImportBatch(BizImportBatch bizImportBatch);

    /**
     * 修改导入批次记录
     * 
     * @param bizImportBatch 导入批次记录
     * @return 结果
     */
    public int updateBizImportBatch(BizImportBatch bizImportBatch);

    /**
     * 批量删除导入批次记录
     * 
     * @param ids 需要删除的导入批次记录主键集合
     * @return 结果
     */
    public int deleteBizImportBatchByIds(Long[] ids);

    /**
     * 删除导入批次记录信息
     * 
     * @param id 导入批次记录主键
     * @return 结果
     */
    public int deleteBizImportBatchById(Long id);
}
