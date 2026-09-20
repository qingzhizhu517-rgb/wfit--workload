package com.workload.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import com.workload.common.utils.DateUtils;
import com.workload.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workload.system.mapper.BizImportBatchMapper;
import com.workload.system.mapper.BizImportRowMapper;
import com.workload.system.domain.BizImportBatch;
import com.workload.system.domain.BizImportRow;
import com.workload.system.domain.dto.importing.ImportRowView;
import com.workload.system.service.IBizImportBatchService;

/**
 * 导入批次记录Service业务层处理
 *
 * @author wflg
 * @date 2026-07-20
 */
@Service
public class BizImportBatchServiceImpl implements IBizImportBatchService
{
    private static final Logger log = LoggerFactory.getLogger(BizImportBatchServiceImpl.class);

    /** 错误行状态：阻断 */
    private static final String ROW_STATUS_BLOCKED = "BLOCKED";
    /** 错误行状态：确认阶段写入失败 */
    private static final String ROW_STATUS_FAILED = "FAILED";

    /** normalized_json 中承载工号/姓名的白名单键（各导入 DTO 统一约定） */
    private static final String[] USER_CODE_KEYS = {"userCode", "employeeCode"};
    private static final String[] USER_NAME_KEYS = {"userName", "nickName", "teacherName"};

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private BizImportBatchMapper bizImportBatchMapper;

    @Autowired
    private BizImportRowMapper bizImportRowMapper;

    /**
     * 查询导入批次记录
     *
     * @param id 导入批次记录主键
     * @return 导入批次记录
     */
    @Override
    public BizImportBatch selectBizImportBatchById(Long id)
    {
        return bizImportBatchMapper.selectBizImportBatchById(id);
    }

    /**
     * 查询批次暂存行视图（按 row_no 升序）。
     * <p>
     * status 为空返回全部行，否则按行状态过滤；仅输出 {@code normalized_json} 中的
     * 白名单字段，绝不解析或返回 {@code raw_json} 里的敏感原始列。
     * </p>
     */
    @Override
    public List<ImportRowView> selectRowViews(Long batchId, String status)
    {
        List<BizImportRow> rows = StringUtils.isNotEmpty(status)
                ? bizImportRowMapper.selectByBatchIdAndStatus(batchId, status)
                : bizImportRowMapper.selectBizImportRowByBatchId(batchId);
        return toViews(rows);
    }

    /**
     * 查询批次的错误行（BLOCKED + FAILED）视图，供错误清单导出。
     */
    @Override
    public List<ImportRowView> selectErrorRows(Long batchId)
    {
        List<BizImportRow> rows = new ArrayList<>();
        rows.addAll(bizImportRowMapper.selectByBatchIdAndStatus(batchId, ROW_STATUS_BLOCKED));
        rows.addAll(bizImportRowMapper.selectByBatchIdAndStatus(batchId, ROW_STATUS_FAILED));
        rows.sort((a, b) -> Integer.compare(
                a.getRowNo() == null ? 0 : a.getRowNo(),
                b.getRowNo() == null ? 0 : b.getRowNo()));
        return toViews(rows);
    }

    /** 把暂存行映射为白名单视图；工号/姓名从 normalized_json 解析，解析失败留空不阻断导出 */
    private List<ImportRowView> toViews(List<BizImportRow> rows)
    {
        List<ImportRowView> views = new ArrayList<>(rows.size());
        for (BizImportRow row : rows)
        {
            JsonNode normalized = parseNormalized(row.getNormalizedJson());
            views.add(new ImportRowView(
                    row.getRowNo() == null ? 0 : row.getRowNo(),
                    pick(normalized, USER_CODE_KEYS),
                    pick(normalized, USER_NAME_KEYS),
                    row.getStatus(),
                    row.getErrorCode(),
                    row.getErrorMessage()));
        }
        return views;
    }

    /** 宽松解析 normalized_json，非法/空 JSON 返回 null，绝不因坏数据让导出失败 */
    private JsonNode parseNormalized(String normalizedJson)
    {
        if (StringUtils.isEmpty(normalizedJson))
        {
            return null;
        }
        try
        {
            return objectMapper.readTree(normalizedJson);
        }
        catch (Exception e)
        {
            log.warn("解析 normalized_json 失败，工号/姓名列留空: {}", e.getMessage());
            return null;
        }
    }

    /** 按白名单键顺序取第一个非空文本值 */
    private String pick(JsonNode node, String[] keys)
    {
        if (node == null)
        {
            return null;
        }
        for (String key : keys)
        {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull() && StringUtils.isNotEmpty(value.asText()))
            {
                return value.asText();
            }
        }
        return null;
    }

    /**
     * 查询导入批次记录列表
     * 
     * @param bizImportBatch 导入批次记录
     * @return 导入批次记录
     */
    @Override
    public List<BizImportBatch> selectBizImportBatchList(BizImportBatch bizImportBatch)
    {
        return bizImportBatchMapper.selectBizImportBatchList(bizImportBatch);
    }

    /**
     * 新增导入批次记录
     * 
     * @param bizImportBatch 导入批次记录
     * @return 结果
     */
    @Override
    public int insertBizImportBatch(BizImportBatch bizImportBatch)
    {
        bizImportBatch.setCreateTime(DateUtils.getNowDate());
        return bizImportBatchMapper.insertBizImportBatch(bizImportBatch);
    }

    /**
     * 修改导入批次记录
     * 
     * @param bizImportBatch 导入批次记录
     * @return 结果
     */
    @Override
    public int updateBizImportBatch(BizImportBatch bizImportBatch)
    {
        bizImportBatch.setUpdateTime(DateUtils.getNowDate());
        return bizImportBatchMapper.updateBizImportBatch(bizImportBatch);
    }

    /**
     * 批量删除导入批次记录
     * 
     * @param ids 需要删除的导入批次记录主键
     * @return 结果
     */
    @Override
    public int deleteBizImportBatchByIds(Long[] ids)
    {
        return bizImportBatchMapper.deleteBizImportBatchByIds(ids);
    }

    /**
     * 删除导入批次记录信息
     * 
     * @param id 导入批次记录主键
     * @return 结果
     */
    @Override
    public int deleteBizImportBatchById(Long id)
    {
        return bizImportBatchMapper.deleteBizImportBatchById(id);
    }
}
