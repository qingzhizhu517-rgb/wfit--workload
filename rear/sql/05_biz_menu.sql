-- ============================================================
-- 工作量管理系统 业务菜单
-- 执行: mysql -u root -p wflg_workload < 05_biz_menu.sql
-- 注意: sys_menu 有 20 列，INSERT 必须显式指定列名
-- ============================================================

-- 先删除可能存在的旧数据（可重复执行）
DELETE FROM sys_role_menu WHERE menu_id >= 2000 AND menu_id < 30000;
DELETE FROM sys_menu WHERE menu_id >= 2000 AND menu_id < 30000;

-- 一级目录：工作量管理
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2000, '工作量管理', 0, 5, 'workload', NULL, '', '', 1, 0, 'M', '0', '0', '', 'chart', 'admin', sysdate(), '', NULL, '工作量管理目录');

-- ==================== 基础数据 ====================
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2001, '教师档案', 2000, 1, 'teacherProfile', 'system/teacherProfile/index', '', '', 1, 0, 'C', '0', '0', 'system:teacherProfile:list', 'peoples', 'admin', sysdate(), '', NULL, '教师档案菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2002, '教学任务', 2000, 2, 'teachingTask', 'system/teachingTask/index', '', '', 1, 0, 'C', '0', '0', 'system:teachingTask:list', 'education', 'admin', sysdate(), '', NULL, '教学任务菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2003, '岗位任职', 2000, 3, 'roleAssignment', 'system/roleAssignment/index', '', '', 1, 0, 'C', '0', '0', 'system:roleAssignment:list', 'job', 'admin', sysdate(), '', NULL, '岗位任职菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2004, '数据导入', 2000, 4, 'importBatch', 'system/importBatch/index', '', '', 1, 0, 'C', '0', '0', 'system:importBatch:list', 'upload', 'admin', sysdate(), '', NULL, '数据导入菜单');

-- ==================== 工作量明细 ====================
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2010, '工作量明细总表', 2000, 5, 'workloadItem', 'system/workloadItem/index', '', '', 1, 0, 'C', '0', '0', 'system:workloadItem:list', 'list', 'admin', sysdate(), '', NULL, '工作量明细菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2011, '理论课明细(G1)', 2000, 6, 'wlTheory', 'system/wlTheory/index', '', '', 1, 0, 'C', '0', '0', 'system:wlTheory:list', 'documentation', 'admin', sysdate(), '', NULL, 'G1理论课菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2012, '课内实践明细(G2)', 2000, 7, 'wlPractice', 'system/wlPractice/index', '', '', 1, 0, 'C', '0', '0', 'system:wlPractice:list', 'documentation', 'admin', sysdate(), '', NULL, 'G2课内实践菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2013, '实习实训明细(G3)', 2000, 8, 'wlInternshipTraining', 'system/wlInternshipTraining/index', '', '', 1, 0, 'C', '0', '0', 'system:wlInternshipTraining:list', 'documentation', 'admin', sysdate(), '', NULL, 'G3实习实训菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2014, '课程设计明细(G4)', 2000, 9, 'wlCourseDesign', 'system/wlCourseDesign/index', '', '', 1, 0, 'C', '0', '0', 'system:wlCourseDesign:list', 'documentation', 'admin', sysdate(), '', NULL, 'G4课程设计菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2015, '毕业论文明细(G5)', 2000, 10, 'wlThesis', 'system/wlThesis/index', '', '', 1, 0, 'C', '0', '0', 'system:wlThesis:list', 'documentation', 'admin', sysdate(), '', NULL, 'G5毕业论文菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2016, '集中实习明细(G6)', 2000, 11, 'wlConcentratedInternship', 'system/wlConcentratedInternship/index', '', '', 1, 0, 'C', '0', '0', 'system:wlConcentratedInternship:list', 'documentation', 'admin', sysdate(), '', NULL, 'G6集中实习菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2017, '管理服务明细(G11)', 2000, 12, 'wlManagement', 'system/wlManagement/index', '', '', 1, 0, 'C', '0', '0', 'system:wlManagement:list', 'documentation', 'admin', sysdate(), '', NULL, 'G11管理服务菜单');

-- ==================== 汇总与酬金 ====================
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2020, '学期汇总', 2000, 13, 'workloadSummary', 'system/workloadSummary/index', '', '', 1, 0, 'C', '0', '0', 'system:workloadSummary:list', 'chart', 'admin', sysdate(), '', NULL, '学期汇总菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2021, '酬金记录', 2000, 14, 'payRecord', 'system/payRecord/index', '', '', 1, 0, 'C', '0', '0', 'system:payRecord:list', 'money', 'admin', sysdate(), '', NULL, '酬金记录菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2022, '其他酬金', 2000, 15, 'allowanceItem', 'system/allowanceItem/index', '', '', 1, 0, 'C', '0', '0', 'system:allowanceItem:list', 'money', 'admin', sysdate(), '', NULL, '其他酬金菜单');

-- ==================== 教师端 ====================
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2030, '我的工作量', 2000, 16, 'myWorkload', 'system/myWorkload/declare', '', '', 1, 0, 'C', '0', '0', 'system:workloadItem:add', 'form', 'admin', sysdate(), '', NULL, '教师自主申报菜单');

-- ==================== 配置 ====================
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2040, '类别字典', 2000, 17, 'workloadCategoryDict', 'system/workloadCategoryDict/index', '', '', 1, 0, 'C', '0', '0', 'system:workloadCategoryDict:list', 'dict', 'admin', sysdate(), '', NULL, '类别字典菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2041, '计算规则', 2000, 18, 'workloadRule', 'system/workloadRule/index', '', '', 1, 0, 'C', '0', '0', 'system:workloadRule:list', 'edit', 'admin', sysdate(), '', NULL, '计算规则菜单');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(2042, '酬金标准', 2000, 19, 'payRate', 'system/payRate/index', '', '', 1, 0, 'C', '0', '0', 'system:payRate:list', 'skill', 'admin', sysdate(), '', NULL, '酬金标准菜单');

-- ==================== 按钮权限 ====================
-- 教师档案
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20011, '教师档案查询', 2001, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:teacherProfile:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20012, '教师档案新增', 2001, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:teacherProfile:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20013, '教师档案修改', 2001, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:teacherProfile:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20014, '教师档案删除', 2001, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:teacherProfile:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20015, '教师档案导出', 2001, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:teacherProfile:export', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20016, '教师档案导入', 2001, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:teacherProfile:import', '#', 'admin', sysdate(), '', NULL, '');

-- 教学任务
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20021, '教学任务查询', 2002, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:teachingTask:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20022, '教学任务新增', 2002, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:teachingTask:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20023, '教学任务修改', 2002, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:teachingTask:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20024, '教学任务删除', 2002, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:teachingTask:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20025, '教学任务导出', 2002, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:teachingTask:export', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20026, '教学任务导入', 2002, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:teachingTask:import', '#', 'admin', sysdate(), '', NULL, '');

-- 岗位任职
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20031, '岗位任职查询', 2003, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:roleAssignment:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20032, '岗位任职新增', 2003, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:roleAssignment:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20033, '岗位任职修改', 2003, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:roleAssignment:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20034, '岗位任职删除', 2003, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:roleAssignment:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20035, '岗位任职导出', 2003, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:roleAssignment:export', '#', 'admin', sysdate(), '', NULL, '');

-- 工作量明细
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20101, '工作量明细查询', 2010, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadItem:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20102, '工作量明细新增', 2010, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadItem:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20103, '工作量明细修改', 2010, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadItem:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20104, '工作量明细删除', 2010, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadItem:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20105, '工作量明细导出', 2010, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadItem:export', '#', 'admin', sysdate(), '', NULL, '');

-- 学期汇总
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20201, '学期汇总查询', 2020, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadSummary:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20202, '学期汇总修改', 2020, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadSummary:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20203, '学期汇总删除', 2020, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadSummary:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20204, '学期汇总导出', 2020, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadSummary:export', '#', 'admin', sysdate(), '', NULL, '');

-- 审批权限
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20205, '审批提交', 2020, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:audit:submit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20206, '审批通过', 2020, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:audit:approve', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20207, '审批驳回', 2020, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'system:audit:reject', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20208, '审批签字', 2020, 8, '', '', '', '', 1, 0, 'F', '0', '0', 'system:audit:sign', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20209, '审批解锁', 2020, 9, '', '', '', '', 1, 0, 'F', '0', '0', 'system:audit:unlock', '#', 'admin', sysdate(), '', NULL, '');

-- 导出报表权限
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20210, '导出个人工作量', 2020, 10, '', '', '', '', 1, 0, 'F', '0', '0', 'system:export:personal', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20211, '导出酬金统计', 2020, 11, '', '', '', '', 1, 0, 'F', '0', '0', 'system:export:paySummary', '#', 'admin', sysdate(), '', NULL, '');

-- 教师确认（08_review_fixes.sql 同步登记，字段完全一致）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20215, '教师确认', 2020, 12, '', '', '', '', 1, 0, 'F', '0', '0', 'system:audit:teacherConfirm', '#', 'admin', sysdate(), '', NULL, '');

-- 仪表盘权限（09_dashboard_perm.sql 同步登记，字段完全一致）
-- 对应 BizDashboardController @PreAuthorize 注解权限串（审查项 P2-05 回归修复）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20216, '管理员仪表盘统计', 2020, 13, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dashboard:adminStats', '#', 'admin', sysdate(), '', NULL, '仪表盘-管理员全局统计');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20217, '学院概况统计', 2020, 14, '', '', '', '', 1, 0, 'F', '0', '0', 'system:dashboard:collegeStats', '#', 'admin', sysdate(), '', NULL, '仪表盘-各学院教学任务概况');

-- 酬金记录
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20212, '酬金记录查询', 2021, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRecord:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20213, '酬金记录修改', 2021, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRecord:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20214, '酬金记录导出', 2021, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRecord:export', '#', 'admin', sysdate(), '', NULL, '');

-- 计算引擎权限
-- 20301 perms 为 BizCalcController 重算端点实际权限串（原 system:calc:recalc 为死权限，见 15_fix_menu_buttons.sql / Bug #3）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20301, '计算重算', 2010, 6, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadItem:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20302, '生成G11', 2010, 7, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadItem:add', '#', 'admin', sysdate(), '', NULL, '');

-- ==================== 明细/配置页按钮权限（原 15_fix_menu_buttons.sql 并入） ====================
-- Bug排查报告-2026-08-26 Bug #1：G 明细页与配置页的 add/edit/query/remove/export
-- 此前未登记，导致除超管外所有角色按钮不渲染、接口 403

-- G1 理论课（2011）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20111, 'G1理论课查询', 2011, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlTheory:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20112, 'G1理论课新增', 2011, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlTheory:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20113, 'G1理论课修改', 2011, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlTheory:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20114, 'G1理论课删除', 2011, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlTheory:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20115, 'G1理论课导出', 2011, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlTheory:export', '#', 'admin', sysdate(), '', NULL, '');

-- G2 课内实践（2012）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20121, 'G2课内实践查询', 2012, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlPractice:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20122, 'G2课内实践新增', 2012, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlPractice:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20123, 'G2课内实践修改', 2012, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlPractice:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20124, 'G2课内实践删除', 2012, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlPractice:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20125, 'G2课内实践导出', 2012, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlPractice:export', '#', 'admin', sysdate(), '', NULL, '');

-- G3 实习实训（2013）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20131, 'G3实习实训查询', 2013, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlInternshipTraining:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20132, 'G3实习实训新增', 2013, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlInternshipTraining:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20133, 'G3实习实训修改', 2013, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlInternshipTraining:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20134, 'G3实习实训删除', 2013, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlInternshipTraining:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20135, 'G3实习实训导出', 2013, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlInternshipTraining:export', '#', 'admin', sysdate(), '', NULL, '');

-- G4 课程设计（2014）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20141, 'G4课程设计查询', 2014, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlCourseDesign:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20142, 'G4课程设计新增', 2014, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlCourseDesign:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20143, 'G4课程设计修改', 2014, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlCourseDesign:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20144, 'G4课程设计删除', 2014, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlCourseDesign:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20145, 'G4课程设计导出', 2014, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlCourseDesign:export', '#', 'admin', sysdate(), '', NULL, '');

-- G5 毕业论文（2015）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20151, 'G5毕业论文查询', 2015, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlThesis:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20152, 'G5毕业论文新增', 2015, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlThesis:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20153, 'G5毕业论文修改', 2015, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlThesis:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20154, 'G5毕业论文删除', 2015, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlThesis:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20155, 'G5毕业论文导出', 2015, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlThesis:export', '#', 'admin', sysdate(), '', NULL, '');

-- G6 集中实习（2016）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20161, 'G6集中实习查询', 2016, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlConcentratedInternship:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20162, 'G6集中实习新增', 2016, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlConcentratedInternship:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20163, 'G6集中实习修改', 2016, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlConcentratedInternship:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20164, 'G6集中实习删除', 2016, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlConcentratedInternship:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20165, 'G6集中实习导出', 2016, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlConcentratedInternship:export', '#', 'admin', sysdate(), '', NULL, '');

-- G11 管理服务（2017）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20171, 'G11管理服务查询', 2017, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlManagement:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20172, 'G11管理服务新增', 2017, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlManagement:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20173, 'G11管理服务修改', 2017, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlManagement:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20174, 'G11管理服务删除', 2017, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlManagement:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20175, 'G11管理服务导出', 2017, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlManagement:export', '#', 'admin', sysdate(), '', NULL, '');

-- 数据导入（2004）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20041, '数据导入批次查询', 2004, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:importBatch:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20042, '数据导入批次新增', 2004, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:importBatch:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20043, '数据导入批次修改', 2004, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:importBatch:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20044, '数据导入批次删除', 2004, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:importBatch:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20045, '数据导入批次导出', 2004, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:importBatch:export', '#', 'admin', sysdate(), '', NULL, '');

-- 其他酬金（2022）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20221, '其他酬金查询', 2022, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:allowanceItem:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20222, '其他酬金新增', 2022, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:allowanceItem:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20223, '其他酬金修改', 2022, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:allowanceItem:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20224, '其他酬金删除', 2022, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:allowanceItem:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20225, '其他酬金导出', 2022, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:allowanceItem:export', '#', 'admin', sysdate(), '', NULL, '');

-- 酬金记录补充（2021；query/edit/export 已有 20212-20214）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20218, '酬金记录新增', 2021, 8, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRecord:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20219, '酬金记录删除', 2021, 9, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRecord:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 学期汇总补充（2020；query/edit/remove/export 已有 20201-20204）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20220, '学期汇总新增', 2020, 15, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadSummary:add', '#', 'admin', sysdate(), '', NULL, '');

-- 类别字典（2040）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20401, '类别字典查询', 2040, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadCategoryDict:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20402, '类别字典新增', 2040, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadCategoryDict:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20403, '类别字典修改', 2040, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadCategoryDict:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20404, '类别字典删除', 2040, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadCategoryDict:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20405, '类别字典导出', 2040, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadCategoryDict:export', '#', 'admin', sysdate(), '', NULL, '');

-- 计算规则（2041）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20411, '计算规则查询', 2041, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadRule:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20412, '计算规则新增', 2041, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadRule:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20413, '计算规则修改', 2041, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadRule:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20414, '计算规则删除', 2041, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadRule:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20415, '计算规则导出', 2041, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadRule:export', '#', 'admin', sysdate(), '', NULL, '');

-- 酬金标准（2042）
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20421, '酬金标准查询', 2042, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRate:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20422, '酬金标准新增', 2042, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRate:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20423, '酬金标准修改', 2042, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRate:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20424, '酬金标准删除', 2042, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRate:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20425, '酬金标准导出', 2042, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRate:export', '#', 'admin', sysdate(), '', NULL, '');

-- ==================== admin 角色授权 ====================
-- role_id=1 是超级管理员，拥有所有权限（上界 30000 与顶部 DELETE 范围一致，
-- 覆盖 20011+ 五位按钮菜单，修复原 < 3000 漏授按钮权限的问题）
INSERT INTO sys_role_menu(role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id >= 2000 AND menu_id < 30000;

-- role_id=2 业务管理员（admin_test 使用），拥有全部业务菜单权限
INSERT INTO sys_role_menu(role_id, menu_id)
SELECT 2, menu_id FROM sys_menu WHERE menu_id >= 2000 AND menu_id < 30000;

-- ==================== 仪表盘权限授权（与 09_dashboard_perm.sql 一致） ====================
-- 授权矩阵：
--   20216 adminStats   → role1(admin) + role2(业务管理员) + role3(教务)
--   20217 collegeStats → role1(admin) + role2(业务管理员) + role3(教务) + role5(院领导)
--   teacherStats 复用 20201(system:workloadSummary:query)，role4(教师) 兜底补授
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES (1, 20216), (2, 20216), (3, 20216);
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES (1, 20217), (2, 20217), (3, 20217), (5, 20217);
INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES (4, 20201);
