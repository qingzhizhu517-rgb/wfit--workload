#!/bin/bash
# ============================================================
# 计算引擎子系统端到端场景验证（对应计划 2026-07-21-计算引擎子系统 Task 6）
# 覆盖验证标准：
#   1. G1~G6 公式已知答案用例（含 G4/G6 截断、G5 超标标记）+ G8 直录透传
#   2. 冻结/锁定护栏（明细已核对、汇总已锁定均拒绝修改）
#   3. 汇总 4 场景：正常超额(300) / <=180 绩效 0 / >540 封顶 / 产假达标减半
#   4. 酬金 A~G 金额 + total_pay 取整
#   5. G11 任职折算 + 两岗叠加 180 封顶 + 幂等重跑
#   6. 规则改值不重启生效（PAY_E_HOURLY 60->70->60）
#
# 用法：后端启动后执行  bash rear/scripts/verify_calc_engine.sh
# 依赖：curl / jq / redis-cli / mysql（仅用于测试数据清理与验证码读取）
# ============================================================
set -u
BASE="${BASE_URL:-http://localhost:8084}"
MYSQL=/usr/local/mysql/bin/mysql
DB=wflg_workload
S1="2025-2026-1"   # 秋季：2025-09-01 ~ 2026-01-31
S2="2025-2026-2"   # 春季：2026-02-20 ~ 2026-07-15
S3="2026-2027-1"   # 秋季：2026-09-01 ~ 2027-01-31（153 天，G11 测试用）

PASS=0; FAIL=0
say()  { printf '%s\n' "$*"; }
ok()   { PASS=$((PASS+1)); say "  [PASS] $1"; }
bad()  { FAIL=$((FAIL+1)); say "  [FAIL] $1  (期望: $2, 实际: $3)"; }

# assert_jq <描述> <json> <jq过滤式(结果须为true)>
assert_jq() {
  local desc="$1" json="$2" filter="$3"
  local r
  r=$(printf '%s' "$json" | jq -r "$filter" 2>/dev/null)
  if [ "$r" = "true" ]; then ok "$desc"; else bad "$desc" "$filter => true" "$(printf '%s' "$json" | jq -c '.' 2>/dev/null | head -c 400)"; fi
}
# assert_val <描述> <期望值> <实际值>
assert_val() {
  if [ "$2" = "$3" ]; then ok "$1"; else bad "$1" "$2" "$3"; fi
}

# ---------- 登录（captcha + redis） ----------
say "== 登录 =="
UUID=$(curl -s "$BASE/captchaImage" | jq -r '.uuid')
CODE=$(redis-cli get "captcha_codes:$UUID" | tr -d '"')
TOKEN=$(curl -s -X POST "$BASE/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"admin\",\"password\":\"admin123\",\"code\":\"$CODE\",\"uuid\":\"$UUID\"}" | jq -r '.token')
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then say "登录失败，终止"; exit 1; fi
AUTH="Authorization: Bearer $TOKEN"
CT="Content-Type: application/json"

api() { # api <METHOD> <PATH> [BODY]
  local m="$1" p="$2" b="${3:-}"
  if [ -n "$b" ]; then
    curl -s -X "$m" "$BASE$p" -H "$AUTH" -H "$CT" -d "$b"
  else
    curl -s -X "$m" "$BASE$p" -H "$AUTH"
  fi
}

# ---------- 清理历史测试数据（保证脚本可重跑） ----------
say "== 清理测试数据 =="
$MYSQL -h 127.0.0.1 -uroot -p123456 $DB 2>/dev/null <<'SQL'
DELETE d FROM biz_wl_theory d JOIN biz_workload_item i ON d.item_id=i.id WHERE i.user_id IN (1,2) AND i.semester IN ('2025-2026-1','2025-2026-2','2026-2027-1');
DELETE d FROM biz_wl_practice d JOIN biz_workload_item i ON d.item_id=i.id WHERE i.user_id IN (1,2) AND i.semester IN ('2025-2026-1','2025-2026-2','2026-2027-1');
DELETE d FROM biz_wl_internship_training d JOIN biz_workload_item i ON d.item_id=i.id WHERE i.user_id IN (1,2) AND i.semester IN ('2025-2026-1','2025-2026-2','2026-2027-1');
DELETE d FROM biz_wl_course_design d JOIN biz_workload_item i ON d.item_id=i.id WHERE i.user_id IN (1,2) AND i.semester IN ('2025-2026-1','2025-2026-2','2026-2027-1');
DELETE d FROM biz_wl_thesis d JOIN biz_workload_item i ON d.item_id=i.id WHERE i.user_id IN (1,2) AND i.semester IN ('2025-2026-1','2025-2026-2','2026-2027-1');
DELETE d FROM biz_wl_concentrated_internship d JOIN biz_workload_item i ON d.item_id=i.id WHERE i.user_id IN (1,2) AND i.semester IN ('2025-2026-1','2025-2026-2','2026-2027-1');
DELETE d FROM biz_wl_management d JOIN biz_workload_item i ON d.item_id=i.id WHERE i.user_id IN (1,2) AND i.semester IN ('2025-2026-1','2025-2026-2','2026-2027-1');
DELETE FROM biz_workload_item WHERE user_id IN (1,2) AND semester IN ('2025-2026-1','2025-2026-2','2026-2027-1');
DELETE FROM biz_workload_summary WHERE user_id IN (1,2) AND semester IN ('2025-2026-1','2025-2026-2','2026-2027-1');
DELETE FROM biz_allowance_item WHERE user_id IN (1,2) AND semester IN ('2025-2026-1','2025-2026-2','2026-2027-1');
DELETE FROM biz_pay_record WHERE user_id IN (1,2) AND semester IN ('2025-2026-1','2025-2026-2','2026-2027-1');
DELETE FROM biz_role_assignment WHERE user_id IN (1,2) AND semester IN ('2025-2026-1','2025-2026-2','2026-2027-1');
DELETE FROM biz_teacher_profile WHERE user_id IN (1,2);
SQL
say "  完成"

# ---------- 教师档案：user1 讲师/专任；user2 讲师/专任/产假 ----------
say "== 教师档案 =="
R=$(api POST /system/teacherProfile '{"userId":1,"title":"讲师","teacherNature":"专任","specialStatus":"正常"}')
assert_jq "user1 档案(讲师/专任)" "$R" '.code==200'
R=$(api POST /system/teacherProfile '{"userId":2,"title":"讲师","teacherNature":"专任","specialStatus":"产假"}')
assert_jq "user2 档案(讲师/专任/产假)" "$R" '.code==200'

# ---------- 工具：建明细 item + detail，回读 calculated_workload ----------
new_item() { # new_item <userId> <semester> <itemType> [calcWorkload]
  local uid="$1" sem="$2" type="$3" cw="${4:-0}"
  api POST /system/workloadItem "{\"userId\":$uid,\"semester\":\"$sem\",\"itemType\":\"$type\",\"sourceType\":\"MANUAL\",\"calculatedWorkload\":$cw}" >/dev/null
  api GET "/system/workloadItem/list?userId=$uid&semester=$sem&itemType=$type" | jq -r '[.rows[].id] | max'
}
item_val() { api GET "/system/workloadItem/$1" | jq -r '.data.calculatedWorkload'; }

say "== 标准1：G 公式用例 =="
# --- G1 = J1*C1*K1*Q1*Q2*Q3*N = 32*0.9*1.1*1*1*1*1.1 = 34.85 ---
ID_G1=$(new_item 1 "$S1" G1)
R=$(api POST /system/wlTheory "{\"itemId\":$ID_G1,\"J1\":32,\"j1\":32,\"C1\":0.9,\"c1\":0.9,\"K1\":1.1,\"k1\":1.1,\"Q1\":1,\"q1\":1,\"Q2\":1,\"q2\":1,\"Q3\":1,\"q3\":1,\"N\":1.1,\"n\":1.1}")
assert_jq "G1 明细创建(自动重算)" "$R" '.code==200'
assert_val "G1=34.85 (32x0.9x1.1x1.1)" "34.85" "$(item_val $ID_G1)"

# --- G2 = J2*K*C2*Q1*Q2*Q3 = 76.15 ---
ID_G2=$(new_item 1 "$S1" G2)
api POST /system/wlPractice "{\"itemId\":$ID_G2,\"J2\":76.15,\"j2\":76.15,\"K\":1,\"k\":1,\"C2\":1,\"c2\":1,\"Q1\":1,\"q1\":1,\"Q2\":1,\"q2\":1,\"Q3\":1,\"q3\":1}" >/dev/null
assert_val "G2=76.15 (J2=76.15)" "76.15" "$(item_val $ID_G2)"

# --- G3 = T*D*K*Q1*Q2*Q3 = 5*4 = 20 ---
ID_G3=$(new_item 1 "$S1" G3)
api POST /system/wlInternshipTraining "{\"itemId\":$ID_G3,\"T\":5,\"t\":5,\"D\":4,\"d\":4,\"K\":1,\"k\":1,\"Q1\":1,\"q1\":1,\"Q2\":1,\"q2\":1,\"Q3\":1,\"q3\":1}" >/dev/null
assert_val "G3=20.00 (T=5,D=4)" "20.00" "$(item_val $ID_G3)"

# --- G4 = J4*min(R4,20)*0.4：R4=25 截断 -> 1*20*0.4 = 8 ---
ID_G4=$(new_item 1 "$S1" G4)
api POST /system/wlCourseDesign "{\"itemId\":$ID_G4,\"J4\":1,\"j4\":1,\"R4\":25,\"r4\":25}" >/dev/null
assert_val "G4 截断=8.00 (R4=25按20算)" "8.00" "$(item_val $ID_G4)"

# --- G5 = R5*K5 不截断：本科 R5=9 -> 81 且 is_over_limit=1 ---
ID_G5=$(new_item 1 "$S1" G5)
api POST /system/wlThesis "{\"itemId\":$ID_G5,\"R5\":9,\"r5\":9,\"K5\":9,\"k5\":9,\"educationLevel\":\"本科\"}" >/dev/null
assert_val "G5 不截断=81.00 (R5=9,K5=9)" "81.00" "$(item_val $ID_G5)"
R=$(api GET "/system/workloadItem/$ID_G5")
assert_jq "G5 本科 R5>8 置 is_over_limit=1" "$R" '(.data.isOverLimit//.data.is_over_limit)==1'

# --- G6 = W*min(R6,20)*0.4：R6=25 截断 -> 10*20*0.4 = 80 且 is_over_limit=1 ---
ID_G6=$(new_item 1 "$S1" G6)
api POST /system/wlConcentratedInternship "{\"itemId\":$ID_G6,\"W\":10,\"w\":10,\"R6\":25,\"r6\":25}" >/dev/null
assert_val "G6 截断=80.00 (W=10,R6=25按20算)" "80.00" "$(item_val $ID_G6)"
R=$(api GET "/system/workloadItem/$ID_G6")
assert_jq "G6 R6>20 置 is_over_limit=1" "$R" '(.data.isOverLimit//.data.is_over_limit)==1'

# --- G8 直录透传：recalcItem 后仍为录入值 30 ---
ID_G8=$(new_item 2 "$S1" G8 30)
R=$(api POST "/system/calc/recalcItem/$ID_G8")
assert_jq "G8 无策略直录透传=30" "$R" '.code==200 and .data==30'

# --- user2/S1 的正常 G1（产假场景用，100） ---
ID_U2G1=$(new_item 2 "$S1" G1)
api POST /system/wlTheory "{\"itemId\":$ID_U2G1,\"J1\":100,\"j1\":100,\"C1\":1,\"c1\":1,\"K1\":1,\"k1\":1,\"Q1\":1,\"q1\":1,\"Q2\":1,\"q2\":1,\"Q3\":1,\"q3\":1,\"N\":1,\"n\":1}" >/dev/null
assert_val "user2 G1=100.00" "100.00" "$(item_val $ID_U2G1)"

say "== 标准2：冻结护栏 =="
api PUT /system/workloadItem "{\"id\":$ID_G1,\"status\":1}" >/dev/null
R=$(api PUT /system/wlTheory "{\"itemId\":$ID_G1,\"J1\":40,\"j1\":40}")
assert_jq "已核对明细改系数被拒" "$R" '.code!=200 and (.msg|test("冻结"))'
R=$(api DELETE "/system/wlTheory/$ID_G1")
assert_jq "已核对明细删除被拒" "$R" '.code!=200 and (.msg|test("冻结"))'
api PUT /system/workloadItem "{\"id\":$ID_G1,\"status\":0}" >/dev/null
R=$(api POST "/system/calc/recalcItem/$ID_G1")
assert_jq "解冻后 recalcItem 恢复" "$R" '.code==200 and .data==34.85'

say "== 标准3：汇总 4 场景 =="
# 场景A：user1/S1 total=300 -> 超额120，绩效(300-180)*50=6000
R=$(api POST "/system/calc/recalcSummary?userId=1&semester=$S1")
assert_jq "A: G7=300 (G1..G6)" "$R" '(.data.summary.g7//.data.summary.G7)==300'
assert_jq "A: G10=300" "$R" '(.data.summary.g10//.data.summary.G10)==300'
assert_jq "A: total=300 / excess=120" "$R" '.data.summary.totalWorkload==300 and .data.summary.excessWorkload==120'
assert_jq "A: 绩效=(300-180)*50=6000" "$R" '.data.summary.performancePay==6000'
assert_jq "A: 职称快照=讲师 rate=50, is_capped=0" "$R" '.data.summary.title=="讲师" and .data.summary.payRate==50 and .data.summary.isCapped==0'
assert_jq "A: 达标标准=120(240/2) 且 met=1" "$R" '.data.summary.basicTeachingStandard==120 and .data.summary.basicTeachingMet==1'
assert_jq "A: 未核对条数=6" "$R" '.data.unconfirmedCount==6'

# 场景B：user2/S1 total=130(<=180) 产假 -> 绩效0，标准=120*0.5=60，met=1
R=$(api POST "/system/calc/recalcSummary?userId=2&semester=$S1")
assert_jq "B: total=130 (G1=100+G8=30)" "$R" '.data.summary.totalWorkload==130'
assert_jq "B: 绩效=0 (<=180)" "$R" '.data.summary.performancePay==0 and .data.summary.excessWorkload==0'
assert_jq "B: 产假标准=60 (240/2*0.5) 且 met=1" "$R" '.data.summary.basicTeachingStandard==60 and .data.summary.basicTeachingMet==1'

# 场景C：user1/S2 total=600(>540) -> 绩效(540-180)*50=18000，is_capped=1
ID_G1C=$(new_item 1 "$S2" G1)
api POST /system/wlTheory "{\"itemId\":$ID_G1C,\"J1\":600,\"j1\":600,\"C1\":1,\"c1\":1,\"K1\":1,\"k1\":1,\"Q1\":1,\"q1\":1,\"Q2\":1,\"q2\":1,\"Q3\":1,\"q3\":1,\"N\":1,\"n\":1}" >/dev/null
R=$(api POST "/system/calc/recalcSummary?userId=1&semester=$S2")
assert_jq "C: total=600, excess=420" "$R" '.data.summary.totalWorkload==600 and .data.summary.excessWorkload==420'
assert_jq "C: 绩效=(540-180)*50=18000 且 is_capped=1" "$R" '.data.summary.performancePay==18000 and .data.summary.isCapped==1'

say "== 标准4：酬金 A~G + 取整 =="
post_allowance() { api POST /system/allowanceItem "$1" >/dev/null; }
last_amount() { api GET "/system/allowanceItem/list?userId=$1&semester=$2&feeType=$3" | jq -r '[.rows[].amount] | max'; }
post_allowance "{\"userId\":1,\"semester\":\"$S1\",\"feeType\":\"A\",\"feeSubtype\":\"自学辅导\",\"studentCount\":4}"
assert_val "A 自学<6人=120" "120.00" "$(last_amount 1 "$S1" A)"
post_allowance "{\"userId\":1,\"semester\":\"$S1\",\"feeType\":\"B\",\"feeSubtype\":\"分散\",\"studentCount\":5}"
assert_val "B 分散5人=50" "50.00" "$(last_amount 1 "$S1" B)"
post_allowance "{\"userId\":1,\"semester\":\"$S1\",\"feeType\":\"C\",\"studentCount\":2}"
assert_val "C 论文重修2人=240" "240.00" "$(last_amount 1 "$S1" C)"
post_allowance "{\"userId\":1,\"semester\":\"$S1\",\"feeType\":\"E\",\"durationHours\":2,\"lectureName\":\"测试讲座\"}"
assert_val "E 讲座2h=120" "120.00" "$(last_amount 1 "$S1" E)"
post_allowance "{\"userId\":1,\"semester\":\"$S1\",\"feeType\":\"F\",\"days\":2,\"classCount\":3}"
assert_val "F (2天x6+3班x1)x30=450" "450.00" "$(last_amount 1 "$S1" F)"
post_allowance "{\"userId\":1,\"semester\":\"$S1\",\"feeType\":\"G\",\"workloadUnits\":10.55}"
assert_val "G 10.55x30=316.50" "316.50" "$(last_amount 1 "$S1" G)"

R=$(api POST "/system/calc/recalcPay?userId=1&semester=$S1")
assert_jq "pay: course_hour_pay=6000" "$R" '.data.courseHourPay==6000'
assert_jq "pay: other=1296.50" "$R" '.data.otherPayTotal==1296.50'
assert_jq "pay: total_pay 取整=7297 (7296.50 HALF_UP)" "$R" '.data.totalPay==7297'

R=$(api GET "/system/calc/preview?userId=1&semester=$S1")
assert_jq "预览(persist=false) total=300" "$R" '.data.summary.totalWorkload==300'

say "== 标准6：规则改值不重启生效 =="
RULE_ID=$(api GET "/system/workloadRule/list?ruleCode=PAY_E_HOURLY" | jq -r '.rows[0].id')
api PUT /system/workloadRule "{\"id\":$RULE_ID,\"ruleValue\":70}" >/dev/null
post_allowance "{\"userId\":2,\"semester\":\"$S1\",\"feeType\":\"E\",\"durationHours\":2}"
assert_val "改值后 E 2h=140 (70/h)" "140.00" "$(last_amount 2 "$S1" E)"
api PUT /system/workloadRule "{\"id\":$RULE_ID,\"ruleValue\":60}" >/dev/null
AID=$(api GET "/system/allowanceItem/list?userId=2&semester=$S1&feeType=E" | jq -r '.rows[0].id')
R=$(api PUT /system/allowanceItem "{\"id\":$AID,\"userId\":2,\"semester\":\"$S1\",\"feeType\":\"E\",\"durationHours\":2}")
assert_jq "改回后 allowance 更新成功" "$R" '.code==200'
assert_val "改回60后 E 2h=120" "120.00" "$(last_amount 2 "$S1" E)"
api DELETE "/system/allowanceItem/$AID" >/dev/null

say "== 标准5：G11 折算 + 180 封顶 =="
# user1/S3：系主任 180/年 全学期 -> 90；教研室主任 200/年 全学期 -> 100；叠加 190 -> 封顶 180
api POST /system/roleAssignment "{\"userId\":1,\"roleType\":\"系主任\",\"target\":\"测试学院\",\"startDate\":\"2026-09-01\",\"allowanceRate\":180,\"semester\":\"$S3\",\"academicYear\":\"2026-2027\",\"status\":1}" >/dev/null
api POST /system/roleAssignment "{\"userId\":1,\"roleType\":\"教研室主任\",\"target\":\"测试系\",\"startDate\":\"2026-09-01\",\"allowanceRate\":200,\"semester\":\"$S3\",\"academicYear\":\"2026-2027\",\"status\":1}" >/dev/null
R=$(api POST "/system/calc/genG11?semester=$S3&userId=1")
assert_jq "genG11 生成2条" "$R" '.code==200 and .data==2'
VALS=$(api GET "/system/workloadItem/list?userId=1&semester=$S3&itemType=G11" | jq -r '[.rows[].calculatedWorkload] | sort | join(",")')
assert_val "G11 满学期折算 90+100" "90.00,100.00" "$VALS"
# 幂等重跑：仍 2 条且值不变
R=$(api POST "/system/calc/genG11?semester=$S3&userId=1")
CNT=$(api GET "/system/workloadItem/list?userId=1&semester=$S3&itemType=G11" | jq -r '.total')
assert_val "genG11 幂等(重跑仍2条)" "2" "$CNT"
# user2/S3：2026-11-01 起任职(92/153天) 180/年 -> 90*92/153 = 54.12
api POST /system/roleAssignment "{\"userId\":2,\"roleType\":\"系主任\",\"target\":\"测试学院\",\"startDate\":\"2026-11-01\",\"allowanceRate\":180,\"semester\":\"$S3\",\"academicYear\":\"2026-2027\",\"status\":1}" >/dev/null
api POST "/system/calc/genG11?semester=$S3&userId=2" >/dev/null
VAL=$(api GET "/system/workloadItem/list?userId=2&semester=$S3&itemType=G11" | jq -r '.rows[0].calculatedWorkload')
assert_val "G11 按天折算=54.12 (90x92/153)" "54.12" "$VAL"
# 汇总层 180 封顶
R=$(api POST "/system/calc/recalcSummary?userId=1&semester=$S3")
assert_jq "G11 两岗叠加封顶=180 (90+100=190)" "$R" '(.data.summary.g11//.data.summary.G11)==180 and .data.summary.totalWorkload==180'

say "== 标准2b：汇总锁定护栏 =="
SUM_ID=$(api GET "/system/workloadSummary/list?userId=2&semester=$S1" | jq -r '.rows[0].id')
api PUT /system/workloadSummary "{\"id\":$SUM_ID,\"status\":3}" >/dev/null
R=$(api POST "/system/calc/recalcSummary?userId=2&semester=$S1")
assert_jq "锁定后 recalcSummary 被拒" "$R" '.code!=200 and (.msg|test("锁定"))'
R=$(api POST "/system/calc/recalcPay?userId=2&semester=$S1")
assert_jq "锁定后 recalcPay 被拒" "$R" '.code!=200 and (.msg|test("锁定"))'
ID_TMP=$(new_item 2 "$S1" G1)
R=$(api POST /system/wlTheory "{\"itemId\":$ID_TMP,\"J1\":10,\"j1\":10,\"C1\":1,\"c1\":1,\"K1\":1,\"k1\":1,\"Q1\":1,\"q1\":1,\"Q2\":1,\"q2\":1,\"Q3\":1,\"q3\":1,\"N\":1,\"n\":1}")
assert_jq "锁定学期新增明细被拒" "$R" '.code!=200 and (.msg|test("锁定"))'
api PUT /system/workloadSummary "{\"id\":$SUM_ID,\"status\":0}" >/dev/null
R=$(api POST "/system/calc/recalcSummary?userId=2&semester=$S1")
assert_jq "解锁后 recalcSummary 恢复" "$R" '.code==200 and .data.summary.totalWorkload==130'

say ""
say "==================================================="
say "结果：PASS=$PASS  FAIL=$FAIL"
say "==================================================="
[ "$FAIL" -eq 0 ]
