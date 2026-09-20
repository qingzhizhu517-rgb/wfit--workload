package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.exception.ServiceException;
import com.workload.system.domain.BizImportBatch;
import com.workload.system.domain.dto.importing.ImportConfirmRequest;
import com.workload.system.mapper.BizImportBatchMapper;
import com.workload.system.mapper.BizImportRowMapper;
import com.workload.system.service.ImportTokenHasher;

/**
 * 导入预检批次状态机单元测试。
 *
 * 覆盖：待确认(1)+令牌匹配才可确认、已确认(2)不可二次确认、阻断行存在时拒绝确认、
 * 预检完成(0→1)与确认失败(1→4)转移、条件更新影响行数≠1 视为并发冲突。
 */
@ExtendWith(MockitoExtension.class)
class ImportPreviewStoreImplTest
{
    @Mock private BizImportBatchMapper batchMapper;
    @Mock private BizImportRowMapper importRowMapper;
    @Mock private ImportTokenHasher tokenHasher;
    @InjectMocks private ImportPreviewStoreImpl store;

    @Test
    void confirmRequiresPendingBatchAndMatchingToken()
    {
        BizImportBatch batch = batch(17L, 1, "token-hash");
        when(batchMapper.selectBizImportBatchById(17L)).thenReturn(batch);
        when(importRowMapper.countByBatchIdAndStatus(17L, "BLOCKED")).thenReturn(0);
        when(tokenHasher.matches("confirm-token", "token-hash")).thenReturn(true);
        when(batchMapper.markConfirmed(17L, 1, 2, "jiaowu_test")).thenReturn(1);

        store.assertConfirmable(new ImportConfirmRequest(17L, "confirm-token", false));
        assertThat(store.markConfirmed(17L)).isEqualTo(1);
    }

    @Test
    void confirmedBatchCannotBeConfirmedTwice()
    {
        when(batchMapper.selectBizImportBatchById(17L)).thenReturn(batch(17L, 2, "token-hash"));
        assertThatThrownBy(() -> store.assertConfirmable(new ImportConfirmRequest(17L, "confirm-token", false)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不处于待确认状态");
    }

    @Test
    void blockedRowsRejectConfirmation()
    {
        when(batchMapper.selectBizImportBatchById(17L)).thenReturn(batch(17L, 1, "token-hash"));
        when(importRowMapper.countByBatchIdAndStatus(17L, "BLOCKED")).thenReturn(3);

        assertThatThrownBy(() -> store.assertConfirmable(new ImportConfirmRequest(17L, "confirm-token", false)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("阻断");
        verify(tokenHasher, never()).matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void wrongTokenRejectsConfirmation()
    {
        when(batchMapper.selectBizImportBatchById(17L)).thenReturn(batch(17L, 1, "token-hash"));
        when(importRowMapper.countByBatchIdAndStatus(17L, "BLOCKED")).thenReturn(0);
        when(tokenHasher.matches("wrong-token", "token-hash")).thenReturn(false);

        assertThatThrownBy(() -> store.assertConfirmable(new ImportConfirmRequest(17L, "wrong-token", false)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("令牌");
    }

    @Test
    void missingBatchRejectsConfirmation()
    {
        when(batchMapper.selectBizImportBatchById(99L)).thenReturn(null);
        assertThatThrownBy(() -> store.assertConfirmable(new ImportConfirmRequest(99L, "confirm-token", false)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void concurrentConfirmConflictThrows()
    {
        when(batchMapper.selectBizImportBatchById(17L)).thenReturn(batch(17L, 1, "token-hash"));
        when(batchMapper.markConfirmed(17L, 1, 2, "jiaowu_test")).thenReturn(0);

        assertThatThrownBy(() -> store.markConfirmed(17L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("并发");
    }

    @Test
    void markPrecheckedPromotesParsingBatch()
    {
        when(batchMapper.selectBizImportBatchById(17L)).thenReturn(batch(17L, 0, "token-hash"));
        when(batchMapper.markStatus(17L, 0, 1, "jiaowu_test")).thenReturn(1);

        assertThat(store.markPrechecked(17L)).isEqualTo(1);
    }

    @Test
    void markPrecheckedRejectsNonParsingBatch()
    {
        when(batchMapper.selectBizImportBatchById(17L)).thenReturn(batch(17L, 1, "token-hash"));
        assertThatThrownBy(() -> store.markPrechecked(17L))
                .isInstanceOf(ServiceException.class);
        verify(batchMapper, never()).markStatus(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void markFailedFromPendingBatch()
    {
        when(batchMapper.selectBizImportBatchById(17L)).thenReturn(batch(17L, 1, "token-hash"));
        when(batchMapper.markStatus(17L, 1, 4, "jiaowu_test")).thenReturn(1);

        assertThat(store.markFailed(17L)).isEqualTo(1);
    }

    private BizImportBatch batch(Long id, int status, String tokenHash)
    {
        BizImportBatch batch = new BizImportBatch();
        batch.setId(id);
        batch.setStatus(status);
        batch.setConfirmTokenHash(tokenHash);
        batch.setOperatorName("jiaowu_test");
        return batch;
    }
}
