package com.workload.system.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizImportBatch;
import com.workload.system.domain.dto.importing.ImportConfirmRequest;
import com.workload.system.mapper.BizImportBatchMapper;
import com.workload.system.mapper.BizImportRowMapper;
import com.workload.system.service.ImportPreviewStore;
import com.workload.system.service.ImportTokenHasher;

/**
 * 导入预检批次状态机实现。
 *
 * <p>状态取值与 {@code biz_import_batch.status} 一致：0 解析中 / 1 待确认 / 2 已导入 /
 * 3 已驳回 / 4 失败。仅放行 0→1、1→2、1→4 三个转移；每次转移都走条件 UPDATE，
 * 影响行数≠1 即判定并发冲突。</p>
 *
 * @author wflg
 */
@Service
public class ImportPreviewStoreImpl implements ImportPreviewStore
{
    /** 解析中 */
    private static final int STATUS_PARSING = 0;
    /** 待确认 */
    private static final int STATUS_PENDING = 1;
    /** 已导入 */
    private static final int STATUS_IMPORTED = 2;
    /** 失败 */
    private static final int STATUS_FAILED = 4;

    /** 阻断行状态 */
    private static final String ROW_STATUS_BLOCKED = "BLOCKED";

    @Autowired
    private BizImportBatchMapper batchMapper;

    @Autowired
    private BizImportRowMapper importRowMapper;

    @Autowired
    private ImportTokenHasher tokenHasher;

    @Override
    public void assertConfirmable(ImportConfirmRequest request)
    {
        BizImportBatch batch = requireBatch(request.batchId());
        if (batch.getStatus() == null || batch.getStatus() != STATUS_PENDING)
        {
            throw new ServiceException("批次不处于待确认状态，无法确认");
        }
        int blocked = importRowMapper.countByBatchIdAndStatus(batch.getId(), ROW_STATUS_BLOCKED);
        if (blocked > 0)
        {
            throw new ServiceException("批次仍有 " + blocked + " 条阻断记录，请先修正后重新预检");
        }
        if (!tokenHasher.matches(request.confirmToken(), batch.getConfirmTokenHash()))
        {
            throw new ServiceException("确认令牌无效或已过期，请重新预检");
        }
    }

    @Override
    public int markPrechecked(Long batchId)
    {
        BizImportBatch batch = requireBatch(batchId);
        if (batch.getStatus() == null || batch.getStatus() != STATUS_PARSING)
        {
            throw new ServiceException("批次不处于解析中状态，无法完成预检");
        }
        int affected = batchMapper.markStatus(batchId, STATUS_PARSING, STATUS_PENDING, operatorOf(batch));
        assertSingleRow(affected);
        return affected;
    }

    @Override
    public int markConfirmed(Long batchId)
    {
        BizImportBatch batch = requireBatch(batchId);
        if (batch.getStatus() == null || batch.getStatus() != STATUS_PENDING)
        {
            throw new ServiceException("批次不处于待确认状态，无法确认");
        }
        int affected = batchMapper.markConfirmed(batchId, STATUS_PENDING, STATUS_IMPORTED, operatorOf(batch));
        assertSingleRow(affected);
        return affected;
    }

    @Override
    public int markFailed(Long batchId)
    {
        BizImportBatch batch = requireBatch(batchId);
        if (batch.getStatus() == null || batch.getStatus() != STATUS_PENDING)
        {
            throw new ServiceException("批次不处于待确认状态，无法置为失败");
        }
        int affected = batchMapper.markStatus(batchId, STATUS_PENDING, STATUS_FAILED, operatorOf(batch));
        assertSingleRow(affected);
        return affected;
    }

    private BizImportBatch requireBatch(Long batchId)
    {
        BizImportBatch batch = batchMapper.selectBizImportBatchById(batchId);
        if (batch == null)
        {
            throw new ServiceException("导入批次不存在：" + batchId);
        }
        return batch;
    }

    private String operatorOf(BizImportBatch batch)
    {
        return batch.getOperatorName();
    }

    private void assertSingleRow(int affected)
    {
        if (affected != 1)
        {
            throw new ServiceException("批次状态并发冲突，请刷新后重试");
        }
    }
}
