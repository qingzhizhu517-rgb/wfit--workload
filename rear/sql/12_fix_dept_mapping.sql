-- ============================================================
-- 12_fix_dept_mapping.sql — sys_dept 部门映射修正（幂等，可重复执行）
-- 执行: mysql -u root -p wflg_workload < 12_fix_dept_mapping.sql
--
-- 背景：biz_teacher_profile 测试数据使用 dept_id 103/105，业务语义要求
--   100=潍理工学院、103/105 为其下属二级学院。而 ry_20260321.sql 默认
--   已有 100（若依科技）、103（研发部门，挂 101 下）、105（测试部门，挂 101 下），
--   原版 INSERT IGNORE 会因主键已存在而全部跳过——既不改名、也不改层级。
--   故改为 INSERT ... ON DUPLICATE KEY UPDATE（upsert），确保无论记录
--   是否已存在，名称/父级/祖链均达到目标状态。
-- ============================================================

-- 1. 校级根部门：100 潍理工学院（覆盖 ry 默认「若依科技」）
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time)
VALUES (100, 0, '0', '潍理工学院', 0, '管理员', '', '', '0', '0', 'admin', sysdate())
ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id), ancestors=VALUES(ancestors), dept_name=VALUES(dept_name), order_num=VALUES(order_num), leader=VALUES(leader), status='0', del_flag='0';

-- 2. 二级学院：103 信息工程学院（覆盖 ry 默认「研发部门」并改挂 100 下）
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time)
VALUES (103, 100, '0,100', '信息工程学院', 1, '', '', '', '0', '0', 'admin', sysdate())
ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id), ancestors=VALUES(ancestors), dept_name=VALUES(dept_name), order_num=VALUES(order_num), status='0', del_flag='0';

-- 3. 二级学院：105 经济管理学院（覆盖 ry 默认「测试部门」并改挂 100 下）
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time)
VALUES (105, 100, '0,100', '经济管理学院', 2, '', '', '', '0', '0', 'admin', sysdate())
ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id), ancestors=VALUES(ancestors), dept_name=VALUES(dept_name), order_num=VALUES(order_num), status='0', del_flag='0';
