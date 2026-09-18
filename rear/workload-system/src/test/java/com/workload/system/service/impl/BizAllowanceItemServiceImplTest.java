package com.workload.system.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.exception.ServiceException;
import com.workload.system.calc.PayCalcService;
import com.workload.system.calc.WorkloadWriteGuard;
import com.workload.system.calc.allowance.AllowanceCalcStrategy;
import com.workload.system.calc.allowance.AllowanceStrategyFactory;
import com.workload.system.domain.BizAllowanceItem;
import com.workload.system.mapper.BizAllowanceItemMapper;

/**
 * 其他酬金明细服务单元测试——A 项 ≥20 人归零的 remark 告警
 * （2026-09-10 统一原则：截断与降级必须告警）。
 *
 * @author wflg
 * @date 2026-09-10
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("其他酬金明细服务（A 项归零告警，第十五条1(2)）")
class BizAllowanceItemServiceImplTest
{
    @InjectMocks
    private BizAllowanceItemServiceImpl service;

    @Mock
    private BizAllowanceItemMapper bizAllowanceItemMapper;

    @Mock
    private AllowanceStrategyFactory allowanceStrategyFactory;

    @Mock
    private PayCalcService payCalcService;

    @Mock private WorkloadWriteGuard writeGuard;

    @Test
    void insertAcquiresFreezeLockBeforeCalculation()
    {
        doThrow(new ServiceException("数据已冻结")).when(writeGuard).lockDraftOrAbsent(1L, "2025-2026-1");
        assertThatThrownBy(() -> service.insertBizAllowanceItem(selfItem(15L))).hasMessageContaining("冻结");
        verifyNoInteractions(allowanceStrategyFactory, bizAllowanceItemMapper);
    }

    @Test
    void concurrentFreezeRejectsUpdateMiss()
    {
        BizAllowanceItem old = selfItem(15L);old.setId(7L);
        when(bizAllowanceItemMapper.selectBizAllowanceItemById(7L)).thenReturn(old);
        AllowanceCalcStrategy strategy = mock(AllowanceCalcStrategy.class);
        when(allowanceStrategyFactory.get("A")).thenReturn(strategy);
        when(strategy.calculate(any())).thenReturn(new BigDecimal("260"));
        assertThatThrownBy(() -> service.updateBizAllowanceItem(old)).hasMessageContaining("状态已变化");
        verify(bizAllowanceItemMapper, never()).updateBizAllowanceItem(any());
    }

    @Test
    void concurrentFreezeRejectsDeleteMiss()
    {
        when(bizAllowanceItemMapper.selectBizAllowanceItemById(7L)).thenReturn(selfItem(15L));
        assertThatThrownBy(() -> service.deleteBizAllowanceItemById(7L)).hasMessageContaining("状态已变化");
        verify(bizAllowanceItemMapper, never()).deleteBizAllowanceItemById(any());
    }

    @Test
    void batchDeletePropagatesLaterConditionalConflict()
    {
        when(bizAllowanceItemMapper.selectBizAllowanceItemById(7L)).thenReturn(selfItem(15L));
        when(bizAllowanceItemMapper.selectBizAllowanceItemById(8L)).thenReturn(selfItem(15L));
        when(bizAllowanceItemMapper.deleteByIdIfSummaryDraft(7L)).thenReturn(1);
        when(bizAllowanceItemMapper.deleteByIdIfSummaryDraft(8L)).thenReturn(0);

        assertThatThrownBy(() -> service.deleteBizAllowanceItemByIds(new Long[] {7L, 8L}))
                .hasMessageContaining("状态已变化");
        verify(bizAllowanceItemMapper, never()).deleteBizAllowanceItemByIds(any());
    }

    @Test
    void batchDeleteMissingRecordCannotReturnSuccess()
    {
        assertThatThrownBy(() -> service.deleteBizAllowanceItemByIds(new Long[] {99L}))
                .hasMessageContaining("不存在");
        verify(bizAllowanceItemMapper, never()).deleteBizAllowanceItemByIds(any());
    }

    @Test
    @DisplayName("A 自学辅导 25 人 → 金额归零且 remark 写入单独开班告警")
    void geTwentyWritesZeroAndWarning()
    {
        AllowanceCalcStrategy strategy = mock(AllowanceCalcStrategy.class);
        when(strategy.calculate(any())).thenReturn(BigDecimal.ZERO);
        when(allowanceStrategyFactory.get("A")).thenReturn(strategy);

        BizAllowanceItem item = selfItem(25L);

        service.insertBizAllowanceItem(item);

        assertThat(item.getAmount()).isEqualByComparingTo("0.00");
        assertThat(item.getRemark())
                .contains("第十五条1(2)")
                .contains("本项不计酬金");
    }

    @Test
    @DisplayName("A 自学辅导 15 人（6~19 档）→ 不写归零告警")
    void belowTwentyNoZeroWarning()
    {
        AllowanceCalcStrategy strategy = mock(AllowanceCalcStrategy.class);
        when(strategy.calculate(any())).thenReturn(new BigDecimal("260"));
        when(allowanceStrategyFactory.get("A")).thenReturn(strategy);

        BizAllowanceItem item = selfItem(15L);

        service.insertBizAllowanceItem(item);

        assertThat(item.getAmount()).isEqualByComparingTo("260.00");
        assertThat(item.getRemark()).isNull();
    }

    @Test
    @DisplayName("A 自学辅导从 25 人改为 15 人 → 清除此前系统生成的归零告警")
    void belowTwentyClearsPreviousSystemWarning()
    {
        AllowanceCalcStrategy strategy = mock(AllowanceCalcStrategy.class);
        when(strategy.calculate(any())).thenReturn(new BigDecimal("260"));
        when(allowanceStrategyFactory.get("A")).thenReturn(strategy);

        BizAllowanceItem item = selfItem(15L);
        item.setId(7L);
        item.setRemark("人数≥20 已达单独开班标准（第十五条1(2)），本项不计酬金，工作量按理论课路线核算");
        when(bizAllowanceItemMapper.selectBizAllowanceItemById(7L)).thenReturn(item);
        when(bizAllowanceItemMapper.updateIfSummaryDraft(any())).thenReturn(1);

        service.updateBizAllowanceItem(item);

        assertThat(item.getRemark()).isNull();
    }

    @Test
    @DisplayName("修改时按存量归属校验，伪造草稿归属不能绕过待审冻结")
    void updateChecksPersistedOwnershipBeforeRequestOwnership()
    {
        BizAllowanceItem old = selfItem(15L);
        old.setId(7L);
        old.setUserId(9L);
        old.setSemester("2025-2026-2");
        when(bizAllowanceItemMapper.selectBizAllowanceItemById(7L)).thenReturn(old);
        doThrow(new ServiceException("学期汇总已进入审批流程，其他酬金禁止修改"))
                .when(payCalcService).assertAllowanceEditable(9L, "2025-2026-2");

        BizAllowanceItem forged = selfItem(15L);
        forged.setId(7L);
        forged.setUserId(1L);
        forged.setSemester("2025-2026-1");

        assertThatThrownBy(() -> service.updateBizAllowanceItem(forged))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("禁止修改");
        verify(payCalcService).assertAllowanceEditable(9L, "2025-2026-2");
    }

    @Test
    @DisplayName("修改不存在的其他酬金记录时报业务异常")
    void updateMissingAllowanceThrowsServiceException()
    {
        BizAllowanceItem item = selfItem(15L);
        item.setId(99L);
        when(bizAllowanceItemMapper.selectBizAllowanceItemById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateBizAllowanceItem(item))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("修改时保留存量 userId 和 semester，禁止迁移归属")
    void updatePreservesPersistedOwnership()
    {
        when(bizAllowanceItemMapper.updateIfSummaryDraft(any())).thenReturn(1);
        AllowanceCalcStrategy strategy = mock(AllowanceCalcStrategy.class);
        when(strategy.calculate(any())).thenReturn(new BigDecimal("260"));
        when(allowanceStrategyFactory.get("A")).thenReturn(strategy);
        BizAllowanceItem old = selfItem(15L);
        old.setId(7L);
        old.setUserId(9L);
        old.setSemester("2025-2026-2");
        when(bizAllowanceItemMapper.selectBizAllowanceItemById(7L)).thenReturn(old);

        BizAllowanceItem forged = selfItem(15L);
        forged.setId(7L);
        forged.setUserId(1L);
        forged.setSemester("2025-2026-1");
        service.updateBizAllowanceItem(forged);

        assertThat(forged.getUserId()).isEqualTo(9L);
        assertThat(forged.getSemester()).isEqualTo("2025-2026-2");
    }

    @Test
    @DisplayName("删除路径继续按存量归属校验冻结")
    void deleteChecksPersistedOwnership()
    {
        when(bizAllowanceItemMapper.deleteByIdIfSummaryDraft(7L)).thenReturn(1);
        BizAllowanceItem old = selfItem(15L);
        old.setId(7L);
        old.setUserId(9L);
        old.setSemester("2025-2026-2");
        when(bizAllowanceItemMapper.selectBizAllowanceItemById(7L)).thenReturn(old);

        service.deleteBizAllowanceItemById(7L);

        verify(payCalcService).assertAllowanceEditable(9L, "2025-2026-2");
    }

    private BizAllowanceItem selfItem(long count)
    {
        BizAllowanceItem item = new BizAllowanceItem();
        item.setUserId(1L);
        item.setSemester("2025-2026-1");
        item.setFeeType("A");
        item.setFeeSubtype("自学加辅导");
        item.setStudentCount(count);
        return item;
    }
}
