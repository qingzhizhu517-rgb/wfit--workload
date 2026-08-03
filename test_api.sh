#!/bin/bash
# 系统API自动化测试脚本
# 使用前请先关闭验证码:
# UPDATE sys_config SET config_value='false' WHERE config_key='sys.account.captchaEnabled';

set -e
BASE="http://localhost:8084"
PASS=0
FAIL=0

check() {
  local name=$1
  local result=$2
  if [ "$result" = "0" ]; then
    echo "✓ $name"
    ((PASS++))
  else
    echo "✗ $name"
    ((FAIL++))
  fi
}

# 1. 登录测试
echo "=== 1. 登录测试 ==="
RESP=$(curl -s -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

if echo "$RESP" | grep -q '"code":200'; then
  TOKEN=$(echo "$RESP" | sed 's/.*"token":"\([^"]*\)".*/\1/')
  check "Admin登录" 0
else
  echo "登录失败: $RESP"
  check "Admin登录" 1
  exit 1
fi

AUTH="Authorization: Bearer $TOKEN"

# 2. 用户信息
echo -e "\n=== 2. 用户信息 ==="
RESP=$(curl -s -H "$AUTH" "$BASE/system/user/info")
if echo "$RESP" | grep -q '"code":200'; then
  check "获取用户信息" 0
else
  check "获取用户信息" 1
fi

# 3. 教师档案
echo -e "\n=== 3. 教师档案 ==="
RESP=$(curl -s -H "$AUTH" "$BASE/system/teacherProfile/list")
if echo "$RESP" | grep -q '"code":200'; then
  check "教师档案列表" 0
else
  check "教师档案列表" 1
fi

# 4. 教学任务
echo -e "\n=== 4. 教学任务 ==="
RESP=$(curl -s -H "$AUTH" "$BASE/system/teachingTask/list")
if echo "$RESP" | grep -q '"code":200'; then
  check "教学任务列表" 0
else
  check "教学任务列表" 1
fi

# 5. 工作量明细
echo -e "\n=== 5. 工作量明细 ==="
RESP=$(curl -s -H "$AUTH" "$BASE/system/workloadItem/list")
if echo "$RESP" | grep -q '"code":200'; then
  check "工作量明细列表" 0
else
  check "工作量明细列表" 1
fi

# 6. 学期汇总
echo -e "\n=== 6. 学期汇总 ==="
RESP=$(curl -s -H "$AUTH" "$BASE/system/workloadSummary/list")
if echo "$RESP" | grep -q '"code":200'; then
  check "学期汇总列表" 0
else
  check "学期汇总列表" 1
fi

# 7. 酬金记录
echo -e "\n=== 7. 酬金记录 ==="
RESP=$(curl -s -H "$AUTH" "$BASE/system/payRecord/list")
if echo "$RESP" | grep -q '"code":200'; then
  check "酬金记录列表" 0
else
  check "酬金记录列表" 1
fi

# 8. 计算预览
echo -e "\n=== 8. 计算预览 ==="
RESP=$(curl -s -H "$AUTH" "$BASE/system/calc/preview?semester=2025-2026-2")
if echo "$RESP" | grep -q '"code":200'; then
  check "计算预览" 0
else
  check "计算预览" 1
fi

# 9. 类别字典
echo -e "\n=== 9. 类别字典 ==="
RESP=$(curl -s -H "$AUTH" "$BASE/system/workloadCategoryDict/list")
if echo "$RESP" | grep -q '"code":200'; then
  check "类别字典" 0
else
  check "类别字典" 1
fi

# 10. 计算规则
echo -e "\n=== 10. 计算规则 ==="
RESP=$(curl -s -H "$AUTH" "$BASE/system/workloadRule/list")
if echo "$RESP" | grep -q '"code":200'; then
  check "计算规则" 0
else
  check "计算规则" 1
fi

# 11. 酬金费率
echo -e "\n=== 11. 酬金费率 ==="
RESP=$(curl -s -H "$AUTH" "$BASE/system/payRate/list")
if echo "$RESP" | grep -q '"code":200'; then
  check "酬金费率" 0
else
  check "酬金费率" 1
fi

# 12. 仪表盘统计
echo -e "\n=== 12. 仪表盘 ==="
RESP=$(curl -s -H "$AUTH" "$BASE/system/workloadSummary/countByStatus")
if echo "$RESP" | grep -q '"code":200'; then
  check "仪表盘统计" 0
else
  check "仪表盘统计" 1
fi

# 汇总
echo -e "\n=== 测试结果 ==="
echo "通过: $PASS"
echo "失败: $FAIL"
echo "总计: $((PASS + FAIL))"

if [ $FAIL -eq 0 ]; then
  echo "✓ 全部测试通过!"
  exit 0
else
  echo "✗ 有 $FAIL 个测试失败"
  exit 1
fi
