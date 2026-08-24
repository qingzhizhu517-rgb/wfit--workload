-- ============================================================
-- 修复 collegeStats 返回空的问题
-- 原因：biz_teacher_profile.dept_id 使用了 103/105，但 sys_dept 中不存在
-- 解决：添加缺失的部门记录
-- ============================================================

-- 确保父部门存在（若依默认 100=若依科技）
INSERT IGNORE INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time) 
VALUES (100, 0, '0', '潍理工学院', 0, '管理员', '', '', '0', '0', 'admin', sysdate());

-- 添加二级学院（与 biz_teacher_profile.test_data 中的 dept_id 对应）
INSERT IGNORE INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time) 
VALUES (103, 100, '0,100', '信息工程学院', 1, '', '', '', '0', '0', 'admin', sysdate());

INSERT IGNORE INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time) 
VALUES (105, 100, '0,100', '经济管理学院', 2, '', '', '', '0', '0', 'admin', sysdate());

-- 如果有其他缺失的 dept_id，在此补充
-- INSERT IGNORE INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time) 
-- VALUES (104, 100, '0,100', '其他学院', 3, '', '', '', '0', '0', 'admin', sysdate());
