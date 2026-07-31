-- ============================================================
-- 工作量管理系统 业务菜单
-- 执行: mysql -h 172.19.80.1 -uroot -p123456 wflg_workload < 05_biz_menu.sql
-- ============================================================

-- 一级目录：工作量管理
INSERT INTO sys_menu VALUES(2000, '工作量管理', '0', '5', 'workload', NULL, '', 1, 0, 'M', '0', '0', '', 'chart', 'admin', sysdate(), '', NULL, '工作量管理目录');

-- ==================== 基础数据 ====================
INSERT INTO sys_menu VALUES(2001, '教师档案', 2000, '1', 'teacherProfile', 'system/teacherProfile/index', '', 1, 0, 'C', '0', '0', 'system:teacherProfile:list', 'peoples', 'admin', sysdate(), '', NULL, '教师档案菜单');
INSERT INTO sys_menu VALUES(2002, '教学任务', 2000, '2', 'teachingTask', 'system/teachingTask/index', '', 1, 0, 'C', '0', '0', 'system:teachingTask:list', 'education', 'admin', sysdate(), '', NULL, '教学任务菜单');
INSERT INTO sys_menu VALUES(2003, '岗位任职', 2000, '3', 'roleAssignment', 'system/roleAssignment/index', '', 1, 0, 'C', '0', '0', 'system:roleAssignment:list', 'job', 'admin', sysdate(), '', NULL, '岗位任职菜单');
INSERT INTO sys_menu VALUES(2004, '数据导入', 2000, '4', 'importBatch', 'system/importBatch/index', '', 1, 0, 'C', '0', '0', 'system:importBatch:list', 'upload', 'admin', sysdate(), '', NULL, '数据导入菜单');

-- ==================== 工作量明细 ====================
INSERT INTO sys_menu VALUES(2010, '工作量明细', 2000, '5', 'workloadItem', 'system/workloadItem/index', '', 1, 0, 'C', '0', '0', 'system:workloadItem:list', 'list', 'admin', sysdate(), '', NULL, '工作量明细菜单');
INSERT INTO sys_menu VALUES(2011, 'G1 理论课', 2000, '6', 'wlTheory', 'system/wlTheory/index', '', 1, 0, 'C', '0', '0', 'system:wlTheory:list', 'documentation', 'admin', sysdate(), '', NULL, 'G1理论课菜单');
INSERT INTO sys_menu VALUES(2012, 'G2 课内实践', 2000, '7', 'wlPractice', 'system/wlPractice/index', '', 1, 0, 'C', '0', '0', 'system:wlPractice:list', 'documentation', 'admin', sysdate(), '', NULL, 'G2课内实践菜单');
INSERT INTO sys_menu VALUES(2013, 'G3 实习实训', 2000, '8', 'wlInternshipTraining', 'system/wlInternshipTraining/index', '', 1, 0, 'C', '0', '0', 'system:wlInternshipTraining:list', 'documentation', 'admin', sysdate(), '', NULL, 'G3实习实训菜单');
INSERT INTO sys_menu VALUES(2014, 'G4 课程设计', 2000, '9', 'wlCourseDesign', 'system/wlCourseDesign/index', '', 1, 0, 'C', '0', '0', 'system:wlCourseDesign:list', 'documentation', 'admin', sysdate(), '', NULL, 'G4课程设计菜单');
INSERT INTO sys_menu VALUES(2015, 'G5 毕业论文', 2000, '10', 'wlThesis', 'system/wlThesis/index', '', 1, 0, 'C', '0', '0', 'system:wlThesis:list', 'documentation', 'admin', sysdate(), '', NULL, 'G5毕业论文菜单');
INSERT INTO sys_menu VALUES(2016, 'G6 集中实习', 2000, '11', 'wlConcentratedInternship', 'system/wlConcentratedInternship/index', '', 1, 0, 'C', '0', '0', 'system:wlConcentratedInternship:list', 'documentation', 'admin', sysdate(), '', NULL, 'G6集中实习菜单');
INSERT INTO sys_menu VALUES(2017, 'G11 管理服务', 2000, '12', 'wlManagement', 'system/wlManagement/index', '', 1, 0, 'C', '0', '0', 'system:wlManagement:list', 'documentation', 'admin', sysdate(), '', NULL, 'G11管理服务菜单');

-- ==================== 汇总与酬金 ====================
INSERT INTO sys_menu VALUES(2020, '学期汇总', 2000, '13', 'workloadSummary', 'system/workloadSummary/index', '', 1, 0, 'C', '0', '0', 'system:workloadSummary:list', 'chart', 'admin', sysdate(), '', NULL, '学期汇总菜单');
INSERT INTO sys_menu VALUES(2021, '酬金记录', 2000, '14', 'payRecord', 'system/payRecord/index', '', 1, 0, 'C', '0', '0', 'system:payRecord:list', 'money', 'admin', sysdate(), '', NULL, '酬金记录菜单');
INSERT INTO sys_menu VALUES(2022, '其他酬金', 2000, '15', 'allowanceItem', 'system/allowanceItem/index', '', 1, 0, 'C', '0', '0', 'system:allowanceItem:list', 'money', 'admin', sysdate(), '', NULL, '其他酬金菜单');

-- ==================== 教师端 ====================
INSERT INTO sys_menu VALUES(2030, '我的工作量', 2000, '16', 'myWorkload', 'system/myWorkload/declare', '', 1, 0, 'C', '0', '0', 'system:workloadItem:add', 'form', 'admin', sysdate(), '', NULL, '教师自主申报菜单');

-- ==================== 配置 ====================
INSERT INTO sys_menu VALUES(2040, '类别字典', 2000, '17', 'workloadCategoryDict', 'system/workloadCategoryDict/index', '', 1, 0, 'C', '0', '0', 'system:workloadCategoryDict:list', 'dict', 'admin', sysdate(), '', NULL, '类别字典菜单');
INSERT INTO sys_menu VALUES(2041, '计算规则', 2000, '18', 'workloadRule', 'system/workloadRule/index', '', 1, 0, 'C', '0', '0', 'system:workloadRule:list', 'edit', 'admin', sysdate(), '', NULL, '计算规则菜单');
INSERT INTO sys_menu VALUES(2042, '酬金标准', 2000, '19', 'payRate', 'system/payRate/index', '', 1, 0, 'C', '0', '0', 'system:payRate:list', 'skill', 'admin', sysdate(), '', NULL, '酬金标准菜单');

-- ==================== 按钮权限 ====================
-- 教师档案
INSERT INTO sys_menu VALUES(20011, '教师档案查询', 2001, '1', '', '', '', 1, 0, 'F', '0', '0', 'system:teacherProfile:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20012, '教师档案新增', 2001, '2', '', '', '', 1, 0, 'F', '0', '0', 'system:teacherProfile:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20013, '教师档案修改', 2001, '3', '', '', '', 1, 0, 'F', '0', '0', 'system:teacherProfile:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20014, '教师档案删除', 2001, '4', '', '', '', 1, 0, 'F', '0', '0', 'system:teacherProfile:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20015, '教师档案导出', 2001, '5', '', '', '', 1, 0, 'F', '0', '0', 'system:teacherProfile:export', '#', 'admin', sysdate(), '', NULL, '');

-- 教学任务
INSERT INTO sys_menu VALUES(20021, '教学任务查询', 2002, '1', '', '', '', 1, 0, 'F', '0', '0', 'system:teachingTask:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20022, '教学任务新增', 2002, '2', '', '', '', 1, 0, 'F', '0', '0', 'system:teachingTask:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20023, '教学任务修改', 2002, '3', '', '', '', 1, 0, 'F', '0', '0', 'system:teachingTask:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20024, '教学任务删除', 2002, '4', '', '', '', 1, 0, 'F', '0', '0', 'system:teachingTask:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20025, '教学任务导出', 2002, '5', '', '', '', 1, 0, 'F', '0', '0', 'system:teachingTask:export', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20026, '教学任务导入', 2002, '6', '', '', '', 1, 0, 'F', '0', '0', 'system:teachingTask:import', '#', 'admin', sysdate(), '', NULL, '');

-- 工作量明细
INSERT INTO sys_menu VALUES(20101, '工作量明细查询', 2010, '1', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadItem:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20102, '工作量明细新增', 2010, '2', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadItem:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20103, '工作量明细修改', 2010, '3', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadItem:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20104, '工作量明细删除', 2010, '4', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadItem:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20105, '工作量明细导出', 2010, '5', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadItem:export', '#', 'admin', sysdate(), '', NULL, '');

-- 学期汇总
INSERT INTO sys_menu VALUES(20201, '学期汇总查询', 2020, '1', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadSummary:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20202, '学期汇总修改', 2020, '2', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadSummary:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20203, '学期汇总删除', 2020, '3', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadSummary:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20204, '学期汇总导出', 2020, '4', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadSummary:export', '#', 'admin', sysdate(), '', NULL, '');

-- 审批权限
INSERT INTO sys_menu VALUES(20205, '审批提交', 2020, '5', '', '', '', 1, 0, 'F', '0', '0', 'system:audit:submit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20206, '审批通过', 2020, '6', '', '', '', 1, 0, 'F', '0', '0', 'system:audit:approve', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20207, '审批驳回', 2020, '7', '', '', '', 1, 0, 'F', '0', '0', 'system:audit:reject', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20208, '审批签字', 2020, '8', '', '', '', 1, 0, 'F', '0', '0', 'system:audit:sign', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20209, '审批解锁', 2020, '9', '', '', '', 1, 0, 'F', '0', '0', 'system:audit:unlock', '#', 'admin', sysdate(), '', NULL, '');

-- 导出报表权限
INSERT INTO sys_menu VALUES(20210, '导出个人工作量', 2020, '10', '', '', '', 1, 0, 'F', '0', '0', 'system:export:personal', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20211, '导出酬金统计', 2020, '11', '', '', '', 1, 0, 'F', '0', '0', 'system:export:paySummary', '#', 'admin', sysdate(), '', NULL, '');

-- 酬金记录
INSERT INTO sys_menu VALUES(20212, '酬金记录查询', 2021, '1', '', '', '', 1, 0, 'F', '0', '0', 'system:payRecord:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20213, '酬金记录修改', 2021, '2', '', '', '', 1, 0, 'F', '0', '0', 'system:payRecord:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20214, '酬金记录导出', 2021, '3', '', '', '', 1, 0, 'F', '0', '0', 'system:payRecord:export', '#', 'admin', sysdate(), '', NULL, '');

-- 计算引擎权限
INSERT INTO sys_menu VALUES(20301, '计算重算', 2010, '6', '', '', '', 1, 0, 'F', '0', '0', 'system:calc:recalc', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES(20302, '生成G11', 2010, '7', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadItem:add', '#', 'admin', sysdate(), '', NULL, '');

-- ==================== admin 角色授权 ====================
-- role_id=1 是超级管理员，拥有所有权限
INSERT INTO sys_role_menu(role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id >= 2000 AND menu_id < 3000;
