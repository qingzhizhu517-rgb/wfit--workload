package com.workload.system.calc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import com.workload.system.domain.*;
import com.workload.system.mapper.*;
class SettlementPresentationTest {
 @Test void historicalPayPointerCannotPretendToBeVerified() {
  var summaries=mock(BizWorkloadSummaryMapper.class);var s=new BizWorkloadSummary();s.setId(1L);s.setSemester("2097-2098-1");
  when(summaries.selectBizWorkloadSummaryById(1L)).thenReturn(s);
  var svc=new SettlementSnapshotService(mock(BizWorkloadSettlementSnapshotMapper.class),mock(BizWorkloadCalcSnapshotMapper.class),summaries,mock(BizPayRecordMapper.class),mock(BizWorkloadItemMapper.class),mock(BizAllowanceItemMapper.class),new SettlementScope());
  var p=new BizPayRecord();p.setSummaryId(1L);p.setSettlementSnapshotId(7L);
  assertThatThrownBy(()->svc.decorate(p)).hasMessageContaining("指针");
 }
 @Test void independentPayMustValidateBothExistingPointersBeforeAppending() {
  // savedSummary is the entry used by independent pay recalculation. A missing paired pay is corrupt evidence.
  var f=new SettlementTestData();var sm=mock(BizWorkloadSummaryMapper.class);var pm=mock(BizPayRecordMapper.class);var im=mock(BizWorkloadItemMapper.class);
  var snap=mock(BizWorkloadSettlementSnapshotMapper.class);when(snap.selectByIdCurrent(7L)).thenReturn(f.row());when(im.selectBizWorkloadItemList(any())).thenReturn(java.util.List.of());
  var svc=new SettlementSnapshotService(snap,mock(BizWorkloadCalcSnapshotMapper.class),sm,pm,im,mock(BizAllowanceItemMapper.class),new SettlementScope());
  assertThatThrownBy(()->svc.assertPreviousPay(f.summary(),null)).hasMessageContaining("酬金");
 }

 @Test void clearingKnownSummaryPointerCannotMasqueradeAsHistoricalEvenAfterDeactivation() {
  var snapshots=mock(BizWorkloadSettlementSnapshotMapper.class);when(snapshots.selectMaxVersion(1L)).thenReturn(1L);
  var s=new SettlementTestData().summary();s.setSettlementSnapshotId(null);
  var svc=new SettlementSnapshotService(snapshots,mock(BizWorkloadCalcSnapshotMapper.class),mock(BizWorkloadSummaryMapper.class),mock(BizPayRecordMapper.class),mock(BizWorkloadItemMapper.class),mock(BizAllowanceItemMapper.class),new SettlementScope());
  assertThatThrownBy(()->svc.decorate(s)).hasMessageContaining("结算证据");
 }
 @Test void payDetailWithClearedPointerCannotInheritVerifiedFromItsSummary() {
  var f=new SettlementTestData();var snapshots=mock(BizWorkloadSettlementSnapshotMapper.class);when(snapshots.selectById(7L)).thenReturn(f.row());
  var summaries=mock(BizWorkloadSummaryMapper.class);when(summaries.selectBizWorkloadSummaryById(1L)).thenReturn(f.summary());
  var pays=mock(BizPayRecordMapper.class);var stored=pay();when(pays.selectBizPayRecordList(any())).thenReturn(java.util.List.of(stored));
  var items=mock(BizWorkloadItemMapper.class);when(items.selectBizWorkloadItemList(any())).thenReturn(java.util.List.of());
  var allowances=mock(BizAllowanceItemMapper.class);when(allowances.selectBizAllowanceItemList(any())).thenReturn(java.util.List.of());
  var svc=new SettlementSnapshotService(snapshots,mock(BizWorkloadCalcSnapshotMapper.class),summaries,pays,items,allowances,new SettlementScope());
  var detail=pay();detail.setSettlementSnapshotId(null);
  assertThatThrownBy(()->svc.decorate(detail)).hasMessageContaining("指针");
 }
 private BizPayRecord pay(){var p=new BizPayRecord();p.setId(2L);p.setUserId(1003L);p.setSemester("2097-2098-1");p.setSummaryId(1L);p.setSettlementSnapshotId(7L);p.setCourseHourPay(new java.math.BigDecimal("0.00"));p.setOtherPayTotal(new java.math.BigDecimal("0.00"));p.setTotalPay(0L);return p;}

 @Test void recalculationBoundaryLocksInputsAndAllowancesBeforeInspectingPairedPay() {
  var data=new SettlementTestData();var snapshots=mock(BizWorkloadSettlementSnapshotMapper.class);when(snapshots.selectMaxVersionCurrent(1L)).thenReturn(1L);when(snapshots.selectByIdCurrent(7L)).thenReturn(data.row());
  var pays=mock(BizPayRecordMapper.class);when(pays.selectBizPayRecordList(any())).thenReturn(java.util.List.of(pay()));
  var items=mock(BizWorkloadItemMapper.class);when(items.selectBizWorkloadItemList(any())).thenReturn(java.util.List.of());
  var allowances=mock(BizAllowanceItemMapper.class);when(allowances.selectBizAllowanceItemList(any())).thenReturn(java.util.List.of());
  var scope=new SettlementScope();scope.setVerificationSemesters(java.util.List.of("2097-2098-1"));
  var svc=new SettlementSnapshotService(snapshots,mock(BizWorkloadCalcSnapshotMapper.class),mock(BizWorkloadSummaryMapper.class),pays,items,allowances,scope);svc.assertRecalculationBoundary(data.summary());
  var order=inOrder(items,allowances,pays);order.verify(items).selectBizWorkloadItemList(argThat(q->Boolean.TRUE.equals(q.getParams().get("currentRead"))));order.verify(allowances).selectBizAllowanceItemList(argThat(q->Integer.valueOf(1).equals(q.getStatus())&&Boolean.TRUE.equals(q.getParams().get("currentRead"))));order.verify(pays).selectBizPayRecordList(any());
 }
}
