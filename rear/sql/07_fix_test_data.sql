-- ============================================================
-- 修复测试数据问题
-- 问题1: G11管理服务明细缺失导致全量重算失败
-- 问题2: 岗位任职数据缺失
-- 执行: mysql -u root -p wflg_workload < rear/sql/07_fix_test_data.sql
-- ============================================================

-- 1. 补充岗位任职数据
INSERT INTO biz_role_assignment (id, user_id, role_type, target, start_date, end_date, allowance_rate, semester, status) VALUES
(1, 4, '班主任', '英语2301班', '2025-09-01', '2026-01-15', 180.00, '2025-2026-1', 0),
(2, 3, '教研室主任', '数学教研室', '2025-09-01', '2026-01-15', 120.00, '2025-2026-1', 0)
ON DUPLICATE KEY UPDATE target=VALUES(target);

-- 2. 补充G11管理服务明细（修复itemId=511缺失问题）
INSERT INTO biz_wl_management (item_id, assignment_id, role_type, prorated_amount, proration_basis) VALUES
(511, 1, '班主任', 180.00, '标准180学时 × 全学期任职')
ON DUPLICATE KEY UPDATE prorated_amount=VALUES(prorated_amount);

-- 3. 验证数据完整性
SELECT '岗位任职数据' AS check_item, COUNT(*) AS cnt FROM biz_role_assignment
UNION ALL
SELECT 'G11管理服务明细', COUNT(*) FROM biz_wl_management
UNION ALL
SELECT '工作量明细(G11)', COUNT(*) FROM biz_workload_item WHERE item_type='G11';
