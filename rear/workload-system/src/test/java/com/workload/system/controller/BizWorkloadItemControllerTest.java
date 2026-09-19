package com.workload.system.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.common.core.domain.AjaxResult;
import com.workload.common.exception.ServiceException;
import com.workload.common.utils.DataScopeUtil;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.service.IBizWorkloadItemService;
import com.workload.system.service.IBizWorkloadSummaryService;
import com.workload.system.service.IWorkloadFactorFormulaService;

@ExtendWith(MockitoExtension.class)
class BizWorkloadItemControllerTest
{
    @InjectMocks private BizWorkloadItemController controller;
    @Mock private IBizWorkloadItemService workloadItemService;
    @Mock private IBizWorkloadSummaryService workloadSummaryService;
    @Mock private IWorkloadFactorFormulaService factorFormulaService;

    @Test
    void shouldAppendFormulaToTheOriginalFlatItemResponse()
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(9L);
        item.setUserId(2002L);
        item.setItemType("G1");
        when(workloadItemService.selectBizWorkloadItemById(9L)).thenReturn(item);
        when(factorFormulaService.build(item)).thenReturn(null);

        try (MockedStatic<DataScopeUtil> dataScope = mockStatic(DataScopeUtil.class))
        {
            AjaxResult response = controller.getInfo(9L);
            assertThat(response.get(AjaxResult.DATA_TAG)).isSameAs(item);
            dataScope.verify(() -> DataScopeUtil.assertOwnOrAdmin(2002L));
        }
    }

    @Test
    void shouldExposeSnapshotMetadataAndSourceTaskOnDetail()
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(9L);
        item.setUserId(2002L);
        item.setItemType("G1");
        FactorFormulaVo vo = new FactorFormulaVo("G1", "J1 × C1 × K1 × Q1 × Q2 × N",
                new BigDecimal("42.24"), "口径说明", true, java.util.Collections.emptyList());
        vo.setSnapshotVersion(3L);
        vo.setSnapshotHash("hash-abc");
        vo.setCalculatedAt(new java.util.Date());
        vo.setLegacy(false);
        FactorFormulaVo.SourceTaskVo sourceTask = new FactorFormulaVo.SourceTaskVo();
        sourceTask.setCourseLevel("省级一流");
        sourceTask.setClassName("计算机2401");
        vo.setSourceTask(sourceTask);
        when(workloadItemService.selectBizWorkloadItemById(9L)).thenReturn(item);
        when(factorFormulaService.build(item)).thenReturn(vo);

        try (MockedStatic<DataScopeUtil> dataScope = mockStatic(DataScopeUtil.class))
        {
            AjaxResult response = controller.getInfo(9L);
            BizWorkloadItem data = (BizWorkloadItem) response.get(AjaxResult.DATA_TAG);
            FactorFormulaVo formula = data.getFactorFormula();
            assertThat(formula.getSnapshotVersion()).isEqualTo(3L);
            assertThat(formula.getSnapshotHash()).isEqualTo("hash-abc");
            assertThat(formula.getCalculatedAt()).isNotNull();
            assertThat(formula.isLegacy()).isFalse();
            assertThat(formula.getSourceTask()).isNotNull();
            assertThat(formula.getSourceTask().getCourseLevel()).isEqualTo("省级一流");
            assertThat(formula.getSourceTask().getClassName()).isEqualTo("计算机2401");
        }
    }

    @Test
    void shouldReturnFlatItemWhenFormulaEnhancementFails()
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(9L);
        item.setUserId(2002L);
        item.setItemType("G6");
        item.setCalculatedWorkload(new BigDecimal("32"));
        when(workloadItemService.selectBizWorkloadItemById(9L)).thenReturn(item);
        when(factorFormulaService.build(item)).thenThrow(new IllegalStateException("rule cache unavailable"));

        try (MockedStatic<DataScopeUtil> dataScope = mockStatic(DataScopeUtil.class))
        {
            AjaxResult response = controller.getInfo(9L);
            assertThat(response.get(AjaxResult.DATA_TAG)).isSameAs(item);
            assertThat(item.getFactorFormula()).isNull();
            dataScope.verify(() -> DataScopeUtil.assertOwnOrAdmin(2002L));
        }
    }

    @Test
    void mustAuthorizeParentBeforeQueryingAnyDetailTable()
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(9L);
        item.setUserId(2002L);
        item.setItemType("G1");
        when(workloadItemService.selectBizWorkloadItemById(9L)).thenReturn(item);

        try (MockedStatic<DataScopeUtil> dataScope = mockStatic(DataScopeUtil.class))
        {
            dataScope.when(() -> DataScopeUtil.assertOwnOrAdmin(2002L))
                    .thenThrow(new ServiceException("无权访问他人数据"));

            assertThatThrownBy(() -> controller.getInfo(9L))
                    .isInstanceOf(ServiceException.class)
                    .hasMessage("工作量明细不存在");
            verifyNoInteractions(factorFormulaService);
        }
    }

    @Test
    void shouldHideWhetherAnInaccessibleRecordExists()
    {
        when(workloadItemService.selectBizWorkloadItemById(10L)).thenReturn(null);

        assertThatThrownBy(() -> controller.getInfo(10L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("工作量明细不存在");
        verifyNoInteractions(factorFormulaService);
    }
}
