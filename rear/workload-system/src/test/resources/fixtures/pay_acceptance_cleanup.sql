-- =============================================================================
-- 酬金验收夹具清理（Task 9 Step 2）：仅删除固定测试 ID + 固定学期的夹具数据。
--
-- 与 pay_acceptance_fixture.sql 的清理段一致。绝不全表 DELETE / DROP / TRUNCATE，
-- 绝不删除 biz_workload_rule / biz_pay_rate 等规则费率数据。
--
-- 用法：
--   mysql -u wfit -p wflg_workload < rear/workload-system/src/test/resources/fixtures/pay_acceptance_cleanup.sql
-- 执行后查询固定学期 + 固定 ID 均应返回 0 行。
-- =============================================================================

delete from biz_allowance_item   where user_id between 99001 and 99005 and semester = '2099-2100-1';
delete from biz_pay_record       where user_id between 99001 and 99005 and semester = '2099-2100-1';
delete from biz_workload_summary where user_id between 99001 and 99005 and semester = '2099-2100-1';
delete from biz_teacher_profile  where user_id between 99001 and 99005;
delete from sys_user_role        where user_id between 99001 and 99005;
delete from sys_user             where user_id between 99001 and 99005;
