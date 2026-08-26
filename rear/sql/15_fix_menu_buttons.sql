-- ============================================================
-- 15_fix_menu_buttons.sql - 补齐明细/配置页按钮权限登记（幂等，可重复执行）
-- 执行: mysql -u root -p wflg_workload < 15_fix_menu_buttons.sql
--
-- 背景（docs/Bug排查报告-2026-08-26.md Bug #1）：
--   05_biz_menu.sql 仅为 5 张基础页面登记了 F 型按钮，7 张 G 明细页
--   （wlTheory/wlPractice/wlInternshipTraining/wlCourseDesign/wlThesis/
--   wlConcentratedInternship/wlManagement）与 importBatch/payRate/payRecord/
--   allowanceItem/workloadCategoryDict/workloadRule 的 add/edit/query/remove/
--   export 按钮全部未登记。后端 @PreAuthorize 与前端 v-hasPermi 均齐全，
--   唯独 sys_menu.perms 缺失 -> 除超管外任何角色按钮不渲染、接口 403。
--
-- 同时处理 Bug #3：菜单 20301 登记的 system:calc:recalc 为死权限
--   （后端无任何代码引用），改为 BizCalcController 重算端点实际使用的
--   system:workloadItem:edit。
--
-- menu_id 规划（沿用「页面 menu_id * 10 + 序号」约定）：
--   wlTheory 20111-20115 / wlPractice 20121-20125 / wlInternshipTraining 20131-20135
--   wlCourseDesign 20141-20145 / wlThesis 20151-20155 / wlConcentratedInternship 20161-20165
--   wlManagement 20171-20175 / importBatch 20041-20045 / allowanceItem 20221-20225
--   payRecord 20218-20219（add/remove，20212-20214 已占用）
--   workloadCategoryDict 20401-20405 / workloadRule 20411-20415 / payRate 20421-20425
--   workloadSummary:add 20220（20201-20217 已占用）
--
-- 授权：role1(admin)/role2(业务管理员) 全量；role3(教务助理) 全量
--   （教务负责明细维护、导入、配置管理）；role4(教师)/role5(院领导) 不涉及。
--
-- 注：新库执行 01->06 后本脚本内容已并入 05/06，无需再执行（幂等，执行亦无害）。
-- ============================================================

-- ==================== Bug #3：清理死权限 ====================
UPDATE sys_menu SET perms = 'system:workloadItem:edit', remark = '工作量重算（BizCalcController 实际权限串，原 system:calc:recalc 为死权限）'
WHERE menu_id = 20301 AND perms = 'system:calc:recalc';

-- ==================== Bug #1：补登记按钮权限 ====================

-- ---- G1 理论课（页面 2011）----
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20111, 'G1理论课查询', 2011, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlTheory:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20112, 'G1理论课新增', 2011, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlTheory:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20113, 'G1理论课修改', 2011, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlTheory:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20114, 'G1理论课删除', 2011, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlTheory:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20115, 'G1理论课导出', 2011, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlTheory:export', '#', 'admin', sysdate(), '', NULL, '');

-- ---- G2 课内实践（页面 2012）----
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20121, 'G2课内实践查询', 2012, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlPractice:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20122, 'G2课内实践新增', 2012, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlPractice:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20123, 'G2课内实践修改', 2012, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlPractice:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20124, 'G2课内实践删除', 2012, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlPractice:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20125, 'G2课内实践导出', 2012, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlPractice:export', '#', 'admin', sysdate(), '', NULL, '');

-- ---- G3 实习实训（页面 2013）----
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20131, 'G3实习实训查询', 2013, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlInternshipTraining:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20132, 'G3实习实训新增', 2013, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlInternshipTraining:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20133, 'G3实习实训修改', 2013, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlInternshipTraining:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20134, 'G3实习实训删除', 2013, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlInternshipTraining:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20135, 'G3实习实训导出', 2013, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlInternshipTraining:export', '#', 'admin', sysdate(), '', NULL, '');

-- ---- G4 课程设计（页面 2014）----
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20141, 'G4课程设计查询', 2014, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlCourseDesign:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20142, 'G4课程设计新增', 2014, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlCourseDesign:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20143, 'G4课程设计修改', 2014, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlCourseDesign:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20144, 'G4课程设计删除', 2014, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlCourseDesign:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20145, 'G4课程设计导出', 2014, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlCourseDesign:export', '#', 'admin', sysdate(), '', NULL, '');

-- ---- G5 毕业论文（页面 2015）----
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20151, 'G5毕业论文查询', 2015, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlThesis:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20152, 'G5毕业论文新增', 2015, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlThesis:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20153, 'G5毕业论文修改', 2015, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlThesis:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20154, 'G5毕业论文删除', 2015, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlThesis:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20155, 'G5毕业论文导出', 2015, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlThesis:export', '#', 'admin', sysdate(), '', NULL, '');

-- ---- G6 集中实习（页面 2016）----
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20161, 'G6集中实习查询', 2016, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlConcentratedInternship:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20162, 'G6集中实习新增', 2016, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlConcentratedInternship:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20163, 'G6集中实习修改', 2016, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlConcentratedInternship:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20164, 'G6集中实习删除', 2016, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlConcentratedInternship:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20165, 'G6集中实习导出', 2016, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlConcentratedInternship:export', '#', 'admin', sysdate(), '', NULL, '');

-- ---- G11 管理服务（页面 2017）----
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20171, 'G11管理服务查询', 2017, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlManagement:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20172, 'G11管理服务新增', 2017, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlManagement:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20173, 'G11管理服务修改', 2017, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlManagement:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20174, 'G11管理服务删除', 2017, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlManagement:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20175, 'G11管理服务导出', 2017, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:wlManagement:export', '#', 'admin', sysdate(), '', NULL, '');

-- ---- 数据导入（页面 2004）----
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20041, '数据导入批次查询', 2004, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:importBatch:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20042, '数据导入批次新增', 2004, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:importBatch:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20043, '数据导入批次修改', 2004, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:importBatch:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20044, '数据导入批次删除', 2004, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:importBatch:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20045, '数据导入批次导出', 2004, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:importBatch:export', '#', 'admin', sysdate(), '', NULL, '');

-- ---- 其他酬金（页面 2022）----
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20221, '其他酬金查询', 2022, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:allowanceItem:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20222, '其他酬金新增', 2022, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:allowanceItem:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20223, '其他酬金修改', 2022, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:allowanceItem:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20224, '其他酬金删除', 2022, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:allowanceItem:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20225, '其他酬金导出', 2022, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:allowanceItem:export', '#', 'admin', sysdate(), '', NULL, '');

-- ---- 酬金记录补充（页面 2021，query/edit/export 已有 20212-20214）----
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20218, '酬金记录新增', 2021, 8, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRecord:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20219, '酬金记录删除', 2021, 9, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRecord:remove', '#', 'admin', sysdate(), '', NULL, '');

-- ---- 学期汇总补充（页面 2020，query/edit/remove/export 已有 20201-20204）----
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20220, '学期汇总新增', 2020, 15, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadSummary:add', '#', 'admin', sysdate(), '', NULL, '');

-- ---- 类别字典（页面 2040）----
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20401, '类别字典查询', 2040, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadCategoryDict:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20402, '类别字典新增', 2040, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadCategoryDict:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20403, '类别字典修改', 2040, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadCategoryDict:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20404, '类别字典删除', 2040, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadCategoryDict:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20405, '类别字典导出', 2040, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadCategoryDict:export', '#', 'admin', sysdate(), '', NULL, '');

-- ---- 计算规则（页面 2041）----
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20411, '计算规则查询', 2041, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadRule:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20412, '计算规则新增', 2041, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadRule:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20413, '计算规则修改', 2041, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadRule:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20414, '计算规则删除', 2041, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadRule:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20415, '计算规则导出', 2041, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:workloadRule:export', '#', 'admin', sysdate(), '', NULL, '');

-- ---- 酬金标准（页面 2042）----
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20421, '酬金标准查询', 2042, 1, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRate:query', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20422, '酬金标准新增', 2042, 2, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRate:add', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20423, '酬金标准修改', 2042, 3, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRate:edit', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20424, '酬金标准删除', 2042, 4, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRate:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT IGNORE INTO sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES(20425, '酬金标准导出', 2042, 5, '', '', '', '', 1, 0, 'F', '0', '0', 'system:payRate:export', '#', 'admin', sysdate(), '', NULL, '');

-- ==================== 角色授权 ====================
-- 新增按钮 menu_id 段（见顶部规划），role1/role2 全量、role3 教务全量
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (
  20041,20042,20043,20044,20045,
  20111,20112,20113,20114,20115,
  20121,20122,20123,20124,20125,
  20131,20132,20133,20134,20135,
  20141,20142,20143,20144,20145,
  20151,20152,20153,20154,20155,
  20161,20162,20163,20164,20165,
  20171,20172,20173,20174,20175,
  20218,20219,20220,
  20221,20222,20223,20224,20225,
  20401,20402,20403,20404,20405,
  20411,20412,20413,20414,20415,
  20421,20422,20423,20424,20425
);
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 2, menu_id FROM sys_menu WHERE menu_id IN (
  20041,20042,20043,20044,20045,
  20111,20112,20113,20114,20115,
  20121,20122,20123,20124,20125,
  20131,20132,20133,20134,20135,
  20141,20142,20143,20144,20145,
  20151,20152,20153,20154,20155,
  20161,20162,20163,20164,20165,
  20171,20172,20173,20174,20175,
  20218,20219,20220,
  20221,20222,20223,20224,20225,
  20401,20402,20403,20404,20405,
  20411,20412,20413,20414,20415,
  20421,20422,20423,20424,20425
);
INSERT IGNORE INTO sys_role_menu(role_id, menu_id)
SELECT 3, menu_id FROM sys_menu WHERE menu_id IN (
  20041,20042,20043,20044,20045,
  20111,20112,20113,20114,20115,
  20121,20122,20123,20124,20125,
  20131,20132,20133,20134,20135,
  20141,20142,20143,20144,20145,
  20151,20152,20153,20154,20155,
  20161,20162,20163,20164,20165,
  20171,20172,20173,20174,20175,
  20218,20219,20220,
  20221,20222,20223,20224,20225,
  20401,20402,20403,20404,20405,
  20411,20412,20413,20414,20415,
  20421,20422,20423,20424,20425
);

-- 教师(role4)/院领导(role5)不涉及明细写操作，不授权
