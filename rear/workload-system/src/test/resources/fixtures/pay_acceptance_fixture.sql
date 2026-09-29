-- =============================================================================
-- 酬金验收夹具（Task 9 Step 1）：数据库 / API / 附件2 三处金额口径的可重复验收数据。
--
-- 固定学期 2099-2100-1、固定用户 99001~99005，绝不与 04_biz_test_data.sql 的业务数据重叠。
-- 幂等：先按固定 ID + 固定学期删子表再插（可重复执行）。只动固定测试 ID，
--       绝不 DROP / TRUNCATE / 全表 DELETE，不碰 biz_workload_rule / biz_pay_rate。
--
-- 用法（本地测试库，非 CI）：
--   mysql -u wfit -p wflg_workload < rear/workload-system/src/test/resources/fixtures/pay_acceptance_fixture.sql
-- 随后以具备 system:payRecord:edit / system:export:paySummary 的账号依次：
--   POST /system/calc/recalcPay?userId=9900X&semester=2099-2100-1  (X=1..5)
--   GET  /system/payRecord/list?semester=2099-2100-1
--   GET  /system/export/paySummary?semester=2099-2100-1
-- 期望值见文件末尾对账 SELECT。
--
-- 关键口径：PayCalcServiceImpl.recalcPay 直接读取 biz_workload_summary.performance_pay
--          作为 course_hour_pay（不按人员性质/费率重算），再累加 status=1 的其他酬金，
--          total_pay = round(course + other, HALF_UP)。故本夹具的 performance_pay 即
--          「汇总已算好」的绩效快照，代表 SummaryCalcService 计算后的状态。
--          汇总一律 status=0，以便 recalcPay 可写入；biz_pay_record 不预置，由 recalcPay 产出。
-- =============================================================================

-- ---- 1. 幂等清理（与 pay_acceptance_cleanup.sql 一致，仅固定 ID + 固定学期）----
delete from biz_allowance_item   where user_id between 99001 and 99005 and semester = '2099-2100-1';
delete from biz_pay_record       where user_id between 99001 and 99005 and semester = '2099-2100-1';
delete from biz_workload_summary where user_id between 99001 and 99005 and semester = '2099-2100-1';
delete from biz_teacher_profile  where user_id between 99001 and 99005;
delete from sys_user_role        where user_id between 99001 and 99005;
delete from sys_user             where user_id between 99001 and 99005;

-- ---- 2. sys_user（附件2 取 user_name/nick_name/dept_id）----
insert into sys_user
    (user_id, dept_id, user_name, nick_name, user_type, status, del_flag, create_by, create_time, remark)
values
    (99001, 103, 'PAY_T99001', '酬金专任教授', '00', '0', '0', 'fixture', sysdate(), '酬金验收夹具-勿动'),
    (99002, 103, 'PAY_T99002', '酬金专任讲师', '00', '0', '0', 'fixture', sysdate(), '酬金验收夹具-勿动'),
    (99003, 103, 'PAY_T99003', '酬金外聘讲师', '00', '0', '0', 'fixture', sysdate(), '酬金验收夹具-勿动'),
    (99004, 103, 'PAY_T99004', '酬金封顶教授', '00', '0', '0', 'fixture', sysdate(), '酬金验收夹具-勿动'),
    (99005, 103, 'PAY_T99005', '酬金无费率师', '00', '0', '0', 'fixture', sysdate(), '酬金验收夹具-勿动');

-- ---- 3. sys_user_role（授教师角色 role_id=4，供 recalcPay 的档案/权限链完整）----
insert into sys_user_role (user_id, role_id) values
    (99001, 4), (99002, 4), (99003, 4), (99004, 4), (99005, 4);

-- ---- 4. biz_teacher_profile（人员性质 + 职称，决定绩效是否计发 / 有无费率）----
insert into biz_teacher_profile
    (user_id, title, teacher_nature, special_status, dept_id, create_by, create_time, remark)
values
    (99001, '教授',   '专任', '正常', 103, 'fixture', sysdate(), '专任正常：绩效应计发'),
    (99002, '讲师',   '专任', '正常', 103, 'fixture', sysdate(), '低于额定：绩效 0，仅其他酬金'),
    (99003, '讲师',   '外聘', '正常', 103, 'fixture', sysdate(), '外聘：绩效不计发'),
    (99004, '教授',   '专任', '正常', 103, 'fixture', sysdate(), '触 540 封顶'),
    (99005, '未定级', '专任', '正常', 103, 'fixture', sysdate(), '无费率职称：pay_rate NULL，绩效 0');

-- ---- 5. biz_workload_summary（status=0；performance_pay 为汇总已算好的绩效快照）----
-- 显式指定 id 便于确定性；uk_user_sem 保证幂等（前面已按 user_id+semester 删除）。
insert into biz_workload_summary
    (id, user_id, semester, academic_year, G7, G8, G9, G10, G11,
     total_workload, rated_workload, excess_workload, title, pay_rate,
     performance_pay, is_capped, status, create_by, create_time, remark)
values
    -- 99001 专任教授：总 282.72，(min(282.72,540)-180)*70 = 102.72*70 = 7190.40
    (990001, 99001, '2099-2100-1', '2099-2100', 282.72, 0, 0, 282.72, 0,
     282.72, 180.00, 102.72, '教授', 70.00, 7190.40, 0, 0, 'fixture', sysdate(), '专任正常'),
    -- 99002 专任讲师：总 150 < 180，绩效 0；其他酬金 30
    (990002, 99002, '2099-2100-1', '2099-2100', 150.00, 0, 0, 150.00, 0,
     150.00, 180.00, 0.00, '讲师', 50.00, 0.00, 0, 0, 'fixture', sysdate(), '低于额定'),
    -- 99003 外聘讲师：总 982.33，外聘不计发绩效（汇总快照即为 0），触顶
    (990003, 99003, '2099-2100-1', '2099-2100', 982.33, 0, 0, 982.33, 0,
     982.33, 180.00, 802.33, '讲师', 50.00, 0.00, 1, 0, 'fixture', sysdate(), '外聘不计发'),
    -- 99004 专任教授：总 600，封顶 (540-180)*70 = 360*70 = 25200，触顶
    (990004, 99004, '2099-2100-1', '2099-2100', 600.00, 0, 0, 600.00, 0,
     600.00, 180.00, 420.00, '教授', 70.00, 25200.00, 1, 0, 'fixture', sysdate(), '540 封顶'),
    -- 99005 专任无费率：总 300，pay_rate NULL，绩效 0
    (990005, 99005, '2099-2100-1', '2099-2100', 300.00, 0, 0, 300.00, 0,
     300.00, 180.00, 120.00, '未定级', NULL, 0.00, 0, 0, 'fixture', sysdate(), '无费率职称');

-- ---- 6. biz_allowance_item ----
-- 99001：A~G 七条独立、status=1，合计 A120+B100+C240+D80+E90+F450+G120 = 1200.00；
--        另加一条 status=0 amount=9999 验证「停用不汇总」（不得计入 other_pay_total）。
insert into biz_allowance_item
    (id, user_id, semester, fee_type, fee_subtype, student_count, duration_hours, days,
     class_count, workload_units, lecture_name, amount, status, create_by, create_time, remark)
values
    (990101, 99001, '2099-2100-1', 'A', '自学辅导',  5,  NULL, NULL, NULL, NULL, NULL, 120.00, 1, 'fixture', sysdate(), 'A 自学辅导 5 人'),
    (990102, 99001, '2099-2100-1', 'B', '分散',     10,  NULL, NULL, NULL, NULL, NULL, 100.00, 1, 'fixture', sysdate(), 'B 分散实习 10 人'),
    (990103, 99001, '2099-2100-1', 'C', NULL,        2,  NULL, NULL, NULL, NULL, NULL, 240.00, 1, 'fixture', sysdate(), 'C 论文/设计 2 组'),
    (990104, 99001, '2099-2100-1', 'D', NULL,       60,  NULL, NULL, NULL, NULL, NULL,  80.00, 1, 'fixture', sysdate(), 'D 代阅卷 60 份'),
    (990105, 99001, '2099-2100-1', 'E', NULL,        0, 1.50, NULL, NULL, NULL, '学术讲座', 90.00, 1, 'fixture', sysdate(), 'E 讲座 1.5 小时'),
    (990106, 99001, '2099-2100-1', 'F', NULL,        0,  NULL, 2.0,    3, NULL, NULL, 450.00, 1, 'fixture', sysdate(), 'F 运动会 2 天 3 班'),
    (990107, 99001, '2099-2100-1', 'G', NULL,        0,  NULL, NULL, NULL, 4.0,  NULL, 120.00, 1, 'fixture', sysdate(), 'G 夜间值班 4 单位'),
    (990108, 99001, '2099-2100-1', 'E', NULL,        0, 9.90, NULL, NULL, NULL, '停用项-不应汇总', 9999.00, 0, 'fixture', sysdate(), 'status=0 停用，验证不计入合计');
-- 99002：其他酬金合计 30
insert into biz_allowance_item
    (id, user_id, semester, fee_type, fee_subtype, student_count, amount, status, create_by, create_time, remark)
values
    (990201, 99002, '2099-2100-1', 'A', '自学辅导', 5, 30.00, 1, 'fixture', sysdate(), '其他酬金 30');

-- ---- 7. 对账 SELECT：五位教师预期列（供人工比对 recalcPay 输出与附件2）----
select 用户ID, 场景, 期望总工作量, 期望单位酬金, 期望绩效酬金, 期望其他酬金, 期望总金额, 备注要点 from (
    select 99001 as 用户ID, '专任教授(正常)'   as 场景, '282.72' as 期望总工作量, '70'   as 期望单位酬金,
           '7190.40' as 期望绩效酬金, '1200.00' as 期望其他酬金, '8390' as 期望总金额,
           '备注应为 -；停用的 9999 项不计入其他酬金' as 备注要点, 1 as 序
    union all select 99002, '专任讲师(低于额定)', '150.00', '50',  '0.00',    '30.00', '30',    '总工作量<180，绩效 0', 2
    union all select 99003, '外聘讲师',          '982.33', '50',  '0.00',    '0.00',  '0',     '备注含「不计发」，是否触顶=是', 3
    union all select 99004, '专任教授(540封顶)', '600.00', '70',  '25200.00','0.00',  '25200', '备注含「540」，是否触顶=是', 4
    union all select 99005, '专任无费率职称',    '300.00', 'NULL','0.00',    '0.00',  '0',     'pay_rate NULL，绩效 0', 5
) t order by 序;
