-- ============================================================
-- 18_fix_r5_approval.sql —— G5 死配置清理 + 专科报批阈值接入
--
-- 依据《办法》第十四条5（2026-09-10 按原文修正）：
--   本科：R5≤10 时按实际人数计算，R5＞8 须报院长批准、教务处备案
--   专科：R5≤15 时按实际人数计算，R5＞15 须报院长批准
-- 条文未写「超出部分不计算」（对比第十四条6注4），故：
--   1) 停用死配置 CAP_R5_BACHELOR=10 / CAP_R5_JUNIOR=15
--      （历史上零代码引用为「截断」，仅 ThesisCalcStrategy 曾误用
--        CAP_R5_JUNIOR 作专科报批阈值，已改读 APPROVAL_R5_JUNIOR）
--   2) 新增专科报批阈值 APPROVAL_R5_JUNIOR=15
--
-- 幂等：可重复执行。全新库执行 01→06+08+13 后本文件仍可安全执行。
-- 改库后需删 Redis 缓存键：wl_rule:APPROVAL_R5_JUNIOR（如已产生）；
-- wl_rule:CAP_R5_* 若存在也一并删除。
-- ============================================================

-- 1. 停用 G5 死配置（status 置 0，保留行作历史追溯）
UPDATE biz_workload_rule
   SET status = 0,
       update_by = 'system',
       update_time = NOW(),
       remark = CONCAT(IFNULL(remark, ''), '；2026-09-10 停用：办法第十四条5 无硬上限，属死配置')
 WHERE rule_code IN ('CAP_R5_BACHELOR', 'CAP_R5_JUNIOR')
   AND status = 1;

-- 2. 新增专科报批阈值（不存在则插入，存在则保持）
INSERT INTO biz_workload_rule (rule_code, rule_value, rule_desc, effective_from)
SELECT 'APPROVAL_R5_JUNIOR', 15.00, 'G5毕业论文专科院长审批阈值(>15须批)', '2025-09-01'
  FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM biz_workload_rule WHERE rule_code = 'APPROVAL_R5_JUNIOR');
