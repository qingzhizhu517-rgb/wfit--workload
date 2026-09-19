package com.workload.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workload.system.domain.BizWorkloadCalcSnapshot;
import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.vo.FactorFormulaVo;
import com.workload.system.domain.vo.FactorFormulaVo.FactorVo;
import com.workload.system.mapper.BizWorkloadCalcSnapshotMapper;

@ExtendWith(MockitoExtension.class)
class WorkloadSnapshotServiceTest
{
    private static final Long ITEM_ID = 7L;

    @Mock private BizWorkloadCalcSnapshotMapper snapshotMapper;
    @InjectMocks private WorkloadSnapshotService service;

    private BizWorkloadItem item()
    {
        BizWorkloadItem item = new BizWorkloadItem();
        item.setId(ITEM_ID);
        item.setUserId(1001L);
        item.setSemester("2025-2026-1");
        item.setItemType("G1");
        item.setCalculatedWorkload(new BigDecimal("52.80"));
        return item;
    }

    private FactorFormulaVo g1Formula()
    {
        List<FactorVo> factors = Arrays.asList(
                new FactorVo("J1", new BigDecimal("32"), "IMPORT_VALUE", "计划学时", "1"),
                new FactorVo("C1", new BigDecimal("1.0"), "RULE_DEFAULT", "重复系数", "1"),
                new FactorVo("K1", new BigDecimal("1.1"), "RULE_DEFAULT", "必修/选修", "1"),
                new FactorVo("Q1", new BigDecimal("1.0"), "IMPORT_VALUE", "质量系数1", "1"),
                new FactorVo("Q2", new BigDecimal("1.0"), "IMPORT_VALUE", "质量系数2", "1"),
                new FactorVo("N", new BigDecimal("1.5"), "IMPORT_VALUE", "合堂系数", "1"));
        return new FactorFormulaVo("G1", "J1 * C1 * K1 * Q1 * Q2 * N",
                new BigDecimal("52.80"), "理论课工作量", factors);
    }

    @Test
    void sameOrderedFactorsProduceStableHash()
    {
        when(snapshotMapper.selectMaxVersionByItemId(ITEM_ID)).thenReturn(null);

        BizWorkloadCalcSnapshot first = service.capture(item(), g1Formula(), "RULE-2025-1");
        BizWorkloadCalcSnapshot second = service.capture(item(), g1Formula(), "RULE-2025-1");

        assertThat(second.getSnapshotHash()).isEqualTo(first.getSnapshotHash());
        assertThat(first.getSnapshotHash()).matches("[0-9a-f]{64}");
        assertThat(first.getFormulaExpression()).isEqualTo("J1 × C1 × K1 × Q1 × Q2 × N");
    }

    @Test
    void factorJsonKeepsOrderAndSerialisesBigDecimalAsString()
    {
        when(snapshotMapper.selectMaxVersionByItemId(ITEM_ID)).thenReturn(null);

        BizWorkloadCalcSnapshot snapshot = service.capture(item(), g1Formula(), "RULE-2025-1");

        assertThat(snapshot.getFactorJson())
                .isEqualTo("{\"J1\":\"32\",\"C1\":\"1.0\",\"K1\":\"1.1\",\"Q1\":\"1.0\",\"Q2\":\"1.0\",\"N\":\"1.5\"}");
        assertThat(snapshot.getSourceJson())
                .isEqualTo("{\"J1\":\"IMPORT_VALUE\",\"C1\":\"RULE_DEFAULT\",\"K1\":\"RULE_DEFAULT\","
                        + "\"Q1\":\"IMPORT_VALUE\",\"Q2\":\"IMPORT_VALUE\",\"N\":\"IMPORT_VALUE\"}");
        assertThat(snapshot.getRuleVersion()).isEqualTo("RULE-2025-1");
        assertThat(snapshot.getResult()).isEqualByComparingTo(new BigDecimal("52.80"));
    }

    @Test
    void versionIncrementsFromExistingMaxVersion()
    {
        when(snapshotMapper.selectMaxVersionByItemId(ITEM_ID)).thenReturn(3L);

        BizWorkloadCalcSnapshot snapshot = service.capture(item(), g1Formula(), "RULE-2025-1");

        assertThat(snapshot.getCalculationVersion()).isEqualTo(4L);
        assertThat(snapshot.getItemId()).isEqualTo(ITEM_ID);
    }

    @Test
    void differentResultProducesDifferentHash()
    {
        when(snapshotMapper.selectMaxVersionByItemId(ITEM_ID)).thenReturn(null);

        BizWorkloadCalcSnapshot base = service.capture(item(), g1Formula(), "RULE-2025-1");

        FactorFormulaVo changed = new FactorFormulaVo("G1", "J1 * C1 * K1 * Q1 * Q2 * N",
                new BigDecimal("48.00"), "理论课工作量", g1Formula().getFactors());
        BizWorkloadCalcSnapshot other = service.capture(item(), changed, "RULE-2025-1");

        assertThat(other.getSnapshotHash()).isNotEqualTo(base.getSnapshotHash());
    }
}
