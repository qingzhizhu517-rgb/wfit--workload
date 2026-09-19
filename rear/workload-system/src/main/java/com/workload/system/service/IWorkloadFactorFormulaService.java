package com.workload.system.service;

import com.workload.system.domain.BizWorkloadItem;
import com.workload.system.domain.vo.FactorFormulaVo;

/** 构造工作量明细的公式与因子展示信息。 */
public interface IWorkloadFactorFormulaService
{
    /**
     * 详情读取用：先按子表构建因子，再用最新已固化快照覆盖取值/来源（历史保真），
     * 无快照则标记 legacy/不可复现。<strong>不可用于 capture 写入路径</strong>，
     * 否则会把旧快照因子回写进新快照。
     */
    FactorFormulaVo build(BizWorkloadItem item);

    /**
     * 核算/固化快照用：仅按当前子表构建新鲜因子，<strong>不做任何快照 overlay</strong>。
     * capture 路径（recalcItem/导入）与一致性子表探针都用它，保证新快照反映当前子表真值，
     * 不被上一版快照污染。
     */
    FactorFormulaVo buildFresh(BizWorkloadItem item);
}
