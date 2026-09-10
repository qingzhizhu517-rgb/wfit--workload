-- ============================================================
-- 19_enable_allowance_d.sql —— 启用 D 代阅卷五档阶梯
--
-- 依据《办法》第十五条5（2026-09-10 按原文启用）：
-- 协助外聘教师阅卷按人数五档以酬金形式直接发放：
--   <20 人 → 0 元；[20,60) → 30 元；[60,120) → 80 元；
--   [120,200) → 100 元；≥200 人 → 150 元
--
-- 原「待正式文件、首期不启用」不成立——办法已给完整阶梯；
-- AllowanceDStrategy 已注册（AllowanceStrategyFactory 全量装配）。
--
-- 幂等：可重复执行；同步修正 biz_allowance_item.status 列注释。
-- 改库后如有 Redis 缓存键 wl_rule:PAY_D_* 需删除。
-- ============================================================

-- 1. 五档规则（不存在则插入）
INSERT INTO biz_workload_rule (rule_code, rule_value, rule_desc, effective_from)
SELECT 'PAY_D_MARKING_LT20', 0.00, 'D代阅卷 <20人(元)', '2025-09-01' FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM biz_workload_rule WHERE rule_code = 'PAY_D_MARKING_LT20');
INSERT INTO biz_workload_rule (rule_code, rule_value, rule_desc, effective_from)
SELECT 'PAY_D_MARKING_20_60', 30.00, 'D代阅卷 [20,60)人(元)', '2025-09-01' FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM biz_workload_rule WHERE rule_code = 'PAY_D_MARKING_20_60');
INSERT INTO biz_workload_rule (rule_code, rule_value, rule_desc, effective_from)
SELECT 'PAY_D_MARKING_60_120', 80.00, 'D代阅卷 [60,120)人(元)', '2025-09-01' FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM biz_workload_rule WHERE rule_code = 'PAY_D_MARKING_60_120');
INSERT INTO biz_workload_rule (rule_code, rule_value, rule_desc, effective_from)
SELECT 'PAY_D_MARKING_120_200', 100.00, 'D代阅卷 [120,200)人(元)', '2025-09-01' FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM biz_workload_rule WHERE rule_code = 'PAY_D_MARKING_120_200');
INSERT INTO biz_workload_rule (rule_code, rule_value, rule_desc, effective_from)
SELECT 'PAY_D_MARKING_GE200', 150.00, 'D代阅卷 >=200人(元)', '2025-09-01' FROM DUAL
 WHERE NOT EXISTS (SELECT 1 FROM biz_workload_rule WHERE rule_code = 'PAY_D_MARKING_GE200');

-- 2. 修正列注释（历史遗留「D代阅卷默认0」已失效）
ALTER TABLE biz_allowance_item
  MODIFY COLUMN status TINYINT(1) DEFAULT 1 COMMENT '1正常0停用';
