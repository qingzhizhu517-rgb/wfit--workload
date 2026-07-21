-- ============================================================
-- 潍理工教师工作量管理系统 计算引擎补充规则参数
-- G11 学期封顶 + 其他酬金 A~G 费率（fee_type 策略 bean 用）
-- 执行: /usr/local/mysql/bin/mysql -h 127.0.0.1 -uroot -p123456 wflg_workload < 03_calc_rules.sql
-- ============================================================

INSERT INTO biz_workload_rule (rule_code, rule_value, rule_desc, effective_from) VALUES
('CAP_G11_SEMESTER',   180.00, '管理服务G11学期累计封顶', '2025-09-01'),
('PAY_A_SELF_LT6',     120.00, 'A重修自学辅导 <6人金额(元)', '2025-09-01'),
('PAY_A_SELF_6_20',    260.00, 'A重修自学辅导 6-20人金额(元)', '2025-09-01'),
('PAY_B_DISPERSED',     10.00, 'B毕业实习分散 元/人', '2025-09-01'),
('PAY_B_CONCENTRATED',  15.00, 'B毕业实习集中(不现场跟班) 元/人', '2025-09-01'),
('PAY_C_THESIS',       120.00, 'C论文重修 元/人', '2025-09-01'),
('PAY_E_HOURLY',        60.00, 'E讲座 元/小时', '2025-09-01'),
('PAY_F_DAY_UNITS',      6.00, 'F运动会裁判 每天工作量', '2025-09-01'),
('PAY_F_CLASS_UNITS',    1.00, 'F运动会 每体测班工作量', '2025-09-01'),
('PAY_UNIT_FEE',        30.00, 'F/G 每单位工作量酬金(元)', '2025-09-01');

-- G5 本科毕业论文院长审批阈值（>8 须审批，硬上限 CAP_R5_BACHELOR=10）
INSERT INTO biz_workload_rule (rule_code, rule_value, rule_desc, effective_from) VALUES
('APPROVAL_R5_BACHELOR', 8.00, 'G5毕业论文本科院长审批阈值(>8须批)', '2025-09-01');
