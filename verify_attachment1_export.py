# -*- coding: utf-8 -*-
"""
附件1（标准表一）导出对齐自动验证。

用法：
    python verify_attachment1_export.py [userId] [semester]
    默认 userId=2003 semester=2026-2027-1（钱伟重复系数专项数据）

做三层比对，任何一层不一致都记入差异报告：
  1. 结构层：导出件 vs classpath 模板——39 列、三层表头文字、表头合并单元格、
     固定行（标题占位替换/总计/签字/备注）位置与内容。
  2. 导出忠实性：导出件 vs 数据库——按开课任务聚合落库值，不把导出 SQL 当期望；
     教师级列取 summary 落库值。
  3. 规则一致性：从原始因子和有效规则独立计算 G4/G6，避免数据库与导出同时错误时假阳性；
     数值按 2 位小数比对，中文字符串全等比对。

产出：docs/附件1导出对齐验证报告.md，退出码 0=零差异 / 1=有差异。
"""
import io
import json
import os
import re
import subprocess
import sys
import urllib.request
from decimal import Decimal, ROUND_HALF_UP

import openpyxl

BASE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(BASE, "docs")
TMP = os.path.join(BASE, ".workbuddy", "_att1_export.xlsx")
TEMPLATE = os.path.join(BASE, "rear", "workload-system", "src", "main",
                        "resources", "templates", "attachment1.xlsx")
API = "http://localhost:8084"
MYSQL = r"D:\app\mysql\install\bin\mysql.exe"

DIFFS = []
CHECKS = [0]


def diff(section, where, expected, actual):
    DIFFS.append({"section": section, "where": where,
                  "expected": str(expected), "actual": str(actual)})


def check(cond, section, where, expected, actual):
    CHECKS[0] += 1
    if not cond:
        diff(section, where, expected, actual)


# ---------------------------------------------------------------- 1. 拉导出件
def login_and_export(user_id, semester):
    body = json.dumps({"username": "admin_test", "password": "123456"}).encode()
    req = urllib.request.Request(API + "/login", data=body,
                                 headers={"Content-Type": "application/json"})
    token = json.loads(urllib.request.urlopen(req, timeout=30).read())["token"]
    req = urllib.request.Request(
        API + f"/system/export/attachment1?userId={user_id}&semester={semester}",
        headers={"Authorization": "Bearer " + token})
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = resp.read()
    with open(TMP, "wb") as f:
        f.write(data)
    return len(data)


# ---------------------------------------------------------------- 2. 读数据库
def db_env():
    env = dict(os.environ)
    env.setdefault("WFIT_DB_HOST", "127.0.0.1")
    env.setdefault("WFIT_DB_PORT", "3306")
    env.setdefault("WFIT_DB_NAME", "wflg_workload")
    env.setdefault("WFIT_DB_USER", "wfit")
    # 显式环境变量优先；.env 只补充缺失值，且文件可选。
    env_path = os.path.join(BASE, ".env")
    if os.path.exists(env_path):
        with io.open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    env.setdefault(k.strip(), v.strip())
    if not env.get("WFIT_DB_PASSWORD"):
        raise RuntimeError("缺少 WFIT_DB_PASSWORD：请通过环境变量或仓库根 .env 注入")
    return env


def db_query(sql):
    env = db_env()
    cmd = [MYSQL, "-h" + env["WFIT_DB_HOST"], "-P" + env["WFIT_DB_PORT"],
           "-u" + env["WFIT_DB_USER"], env["WFIT_DB_NAME"],
           "--default-character-set=utf8mb4", "--batch", "-e", sql]
    process_env = dict(os.environ)
    process_env["MYSQL_PWD"] = env["WFIT_DB_PASSWORD"]
    out = subprocess.run(cmd, capture_output=True, check=True, env=process_env).stdout.decode("utf-8")
    lines = [l for l in out.splitlines() if l]
    if not lines:
        return []  # 无 summary 等空结果：mysql --batch 不输出表头
    header = lines[0].split("\t")
    rows = [dict(zip(header, l.split("\t"))) for l in lines[1:]]
    return rows


def dec(s):
    if s is None or s == "NULL" or s == "":
        return None
    return Decimal(str(s))


def num(s):
    value = dec(s)
    return None if value is None else float(value)


def required_dec(value, label):
    result = dec(value)
    if result is None:
        raise ValueError(f"规则复算缺少必填原始因子：{label}")
    return result


def validate_inputs(user_id, semester):
    if not str(user_id).isdigit() or int(user_id) <= 0:
        raise ValueError("userId 必须是正整数")
    if not re.fullmatch(r"\d{4}-\d{4}-[12]", str(semester)):
        raise ValueError("semester 格式必须为 YYYY-YYYY-1/2")
    return str(int(user_id)), str(semester)


def scaled(value):
    if value is None:
        raise ValueError("规则复算缺少必填原始因子")
    return value.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def fetch_rules():
    codes = "'CONST_COURSE_DESIGN','CAP_R6_MAX','CAP_G11_SEMESTER','RATED_WORKLOAD'"
    rows = db_query(f"""
        select rule_code, rule_value from biz_workload_rule
        where status = 1 and rule_code in ({codes})
          and (effective_from is null or effective_from <= current_date)
          and (effective_to is null or effective_to >= current_date)
        order by rule_code, effective_from desc""")
    rules = {}
    for row in rows:
        rules.setdefault(row["rule_code"], dec(row["rule_value"]))
    defaults = {"CONST_COURSE_DESIGN": Decimal("0.4"),
                "CAP_R6_MAX": Decimal("20"),
                "CAP_G11_SEMESTER": Decimal("180"),
                "RATED_WORKLOAD": Decimal("180")}
    return {code: rules.get(code, value) for code, value in defaults.items()}


def fetch_expected_rows(user_id, semester, rules):
    """按任务聚合落库值，并从原始因子独立计算 G4/G6 规则期望。"""
    rows = db_query(f"""
        select i.id, ifnull(i.task_id, -i.id) as grp, i.item_type,
               coalesce(t.course_name, i.course_name) as course_name,
               i.education_level, i.calculated_workload,
               th.J1, th.C1, th.K1, th.Q1, th.Q2, th.Q3, th.N,
               pr.J2, pr.K as prK, pr.C2,
               itr.T, itr.D, itr.K as itrK,
               cd.J4, cd.R4,
               ts.discipline_category, ci.W, ci.R6
        from biz_workload_item i
            left join biz_teaching_task t on t.id = i.task_id
            left join biz_wl_theory th on th.item_id = i.id and i.item_type = 'G1'
            left join biz_wl_practice pr on pr.item_id = i.id and i.item_type = 'G2'
            left join biz_wl_internship_training itr on itr.item_id = i.id and i.item_type = 'G3'
            left join biz_wl_course_design cd on cd.item_id = i.id and i.item_type = 'G4'
            left join biz_wl_thesis ts on ts.item_id = i.id and i.item_type = 'G5'
            left join biz_wl_concentrated_internship ci on ci.item_id = i.id and i.item_type = 'G6'
        where i.user_id = {user_id} and i.semester = '{semester}'
          and i.item_type in ('G1','G2','G3','G4','G5','G6')
          and (i.status is null or i.status != 3)
        order by i.id""")
    groups = {}
    order = []
    for r in rows:
        g = int(r["grp"])
        if g not in groups:
            groups[g] = []
            order.append(g)
        groups[g].append(r)

    expected = []
    rule_checks = []
    for g in order:
        items = groups[g]
        agg = {}
        for key, col in [("j1", "J1"), ("c1", "C1"), ("k1", "K1"), ("q1", "Q1"),
                         ("q2", "Q2"), ("q3", "Q3"), ("n", "N"),
                         ("practiceK", "prK"), ("c2", "C2"),
                         ("d", "D"), ("internK", "itrK")]:
            vals = [num(r[col]) for r in items if r[col] not in ("NULL", None, "")]
            agg[key] = max(vals) if vals else None
        for key, col in [("j2", "J2"), ("t", "T"), ("j4", "J4"), ("r4", "R4"),
                         ("w", "W"), ("r6", "R6")]:
            vals = [num(r[col]) for r in items if r[col] not in ("NULL", None, "")]
            agg[key] = sum(vals) if vals else None
        for key, typ in [("g1", "G1"), ("g2", "G2"), ("g3", "G3"),
                         ("g4", "G4"), ("g6", "G6")]:
            vals = [dec(r["calculated_workload"]) for r in items if r["item_type"] == typ]
            agg[key] = float(sum(vals, Decimal("0"))) if vals else None
        lib = sci = None
        for r in items:
            if r["item_type"] == "G5":
                v = num(r["calculated_workload"])
                if r["discipline_category"] == "SCITECH":
                    sci = (sci or 0) + v
                else:
                    lib = (lib or 0) + v
        agg["g5Liberal"], agg["g5Scitech"] = lib, sci
        courses = [r["course_name"] for r in items if r["course_name"] not in ("NULL", None, "")]
        levels = [r["education_level"] for r in items if r["education_level"] not in ("NULL", None, "")]
        agg["courseName"] = max(courses) if courses else None
        agg["educationLevel"] = max(levels) if levels else None
        expected.append(agg)

        for r in items:
            actual = dec(r["calculated_workload"])
            if r["item_type"] == "G4":
                expected_value = scaled(required_dec(r["J4"], f"item {r['id']} J4")
                                        * required_dec(r["R4"], f"item {r['id']} R4")
                                        * rules["CONST_COURSE_DESIGN"])
                rule_checks.append((r["id"], "G4 不截断", expected_value, actual))
            elif r["item_type"] == "G6":
                people = min(required_dec(r["R6"], f"item {r['id']} R6"), rules["CAP_R6_MAX"])
                expected_value = scaled(required_dec(r["W"], f"item {r['id']} W")
                                        * people * rules["CONST_COURSE_DESIGN"])
                rule_checks.append((r["id"], "G6 人数封顶", expected_value, actual))
    return expected, rule_checks


def fetch_teacher(user_id, semester):
    rows = db_query(f"""
        select d.dept_name, u.nick_name, s.academic_year, s.G7, s.G8, s.G9, s.G10, s.G11,
               s.g8_remark, s.g11_remark, s.total_workload, s.rated_workload, s.excess_workload,
               s.teacher_sign
        from biz_workload_summary s
            left join sys_user u on u.user_id = s.user_id
            left join sys_dept d on d.dept_id = u.dept_id
        where s.user_id = {user_id} and s.semester = '{semester}' limit 1""")
    return rows[0] if rows else None


# ---------------------------------------------------------------- 3. 比对
COLS = "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z AA AB AC AD AE AF AG AH AI AJ AK AL AM".split()
ROW_FIELDS = ["courseName", "educationLevel", "j1", "c1", "k1", "q1", "q2", "q3", "n", "g1",
              "j2", "practiceK", "c2", "g2", "t", "d", "internK", "g3", "j4", "r4", "g4",
              "g5Liberal", "g5Scitech", "w", "r6", "g6"]  # 对应 C..AB（列索引 2..27）


def close(a, b):
    if a is None and b is None:
        return True
    if a is None or b is None:
        return False
    return abs(float(a) - float(b)) < 0.005


def cellv(ws, r, c):
    return ws.cell(row=r, column=c).value


def verify_structure(wb_out, wb_tpl, teacher, semester, n_rows):
    out, tpl = wb_out.active, wb_tpl.active
    check(out.max_column == 39, "结构", "总列数", 39, out.max_column)
    # 三层表头逐格全等（rows 2-4）
    for r in (2, 3, 4):
        for c in range(1, 40):
            t, o = cellv(tpl, r, c), cellv(out, r, c)
            check(t == o, "结构", f"表头 {COLS[c-1]}{r}", repr(t), repr(o))
    # 表头区合并单元格集合一致（rows 1-4）
    m_tpl = {str(m) for m in tpl.merged_cells.ranges if m.min_row <= 4}
    m_out = {str(m) for m in out.merged_cells.ranges if m.min_row <= 4}
    check(m_tpl == m_out, "结构", "表头合并单元格", sorted(m_tpl), sorted(m_out))
    # 固定块合并单元格（总计/签字区）应随数据行数整体平移 n-3 行
    shift = n_rows - 3
    from openpyxl.utils.cell import range_boundaries, get_column_letter
    def shifted(rng, k):
        c1, r1, c2, r2 = range_boundaries(rng)
        return f"{get_column_letter(c1)}{r1+k}:{get_column_letter(c2)}{r2+k}"
    m_tpl_fixed = {shifted(str(m), shift) for m in tpl.merged_cells.ranges if m.min_row >= 8}
    m_out_fixed = {str(m) for m in out.merged_cells.ranges if m.min_row >= 5 + n_rows - 1}
    check(m_tpl_fixed == m_out_fixed, "结构", "固定块合并单元格(平移后)",
          sorted(m_tpl_fixed), sorted(m_out_fixed))
    # 标题行占位替换
    title = str(cellv(out, 1, 1))
    year = semester.rsplit("-", 1)[0]
    sem_no = semester.rsplit("-", 1)[1]
    check(year in title, "结构", "标题含学年", year, title)
    check(f"第{sem_no}学期" in title, "结构", "标题含学期号", f"第{sem_no}学期", title)
    if teacher and teacher["dept_name"] not in ("NULL", None):
        check(teacher["dept_name"] in title, "结构", "标题含学院", teacher["dept_name"], title)
    check("表一）" in title, "结构", "标题含「表一」", "表一）", title)
    # 固定行：总计在 4+n+1 行、签字/备注随行平移
    total_r = 5 + n_rows
    b = cellv(out, total_r, 2)
    check(b is not None and "总计" in str(b), "结构", f"总计行位置(B{total_r})", "总计：", repr(b))
    sign = [str(cellv(out, total_r + 1, 2)), str(cellv(out, total_r + 1, 24))]
    check(any("院（部）领导签字" in s for s in sign), "结构", "签字行", "院（部）领导签字", sign)
    note = str(cellv(out, total_r + 3, 3))
    check("班主任变动" in note, "结构", "备注行1", "班主任变动...", note[:30])
    check("未说明的" in str(cellv(out, total_r + 4, 3)), "结构", "备注行2", "未说明的...", "")


def verify_data(wb_out, expected, teacher):
    out = wb_out.active
    n = len(expected)
    check(out.max_row >= 5 + n, "导出忠实性", "行数下限", f">= {5 + n}", out.max_row)
    # 表头 4 行 + n 数据 + 固定块 5 行（总计/签字×2/备注×2），一行不多不少
    check(out.max_row == n + 9, "导出忠实性", "总行数(无尾部空行残留)", n + 9, out.max_row)

    for i, exp in enumerate(expected):
        r = 5 + i
        # 文本列（编码验证：中文全等）
        for j, key in enumerate(ROW_FIELDS):
            c = 3 + j  # C 列起
            actual = cellv(out, r, c)
            if key in ("courseName", "educationLevel"):
                check(exp[key] == actual, "导出忠实性", f"{COLS[c-1]}{r}({key})", exp[key], actual)
            else:
                ok = close(exp[key], actual)
                # 精度：DB DECIMAL(10,2) vs 导出浮点，容差半分位
                check(ok, "导出忠实性", f"{COLS[c-1]}{r}({key})",
                      None if exp[key] is None else round(exp[key], 2),
                      None if actual is None else round(float(actual), 2))
        # 教师级列只在首行出现，其余行必须为空（对齐参考件填法）
        if i > 0:
            for c in (1, 2, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39):
                check(cellv(out, r, c) in (None, ""), "导出忠实性",
                      f"{COLS[c-1]}{r} 应留空(教师级列只填首行)", "空", repr(cellv(out, r, c)))

    if teacher:
        r = 5
        pairs = [(1, "dept_name"), (2, "nick_name"), (29, "G7"), (30, "G8"), (31, "G9"),
                 (32, "g8_remark"), (33, "G10"), (34, "G11"), (35, "g11_remark"),
                 (36, "total_workload"), (37, "rated_workload"), (38, "excess_workload"),
                 (39, "teacher_sign")]
        for c, key in pairs:
            actual = cellv(out, r, c)
            if key in ("dept_name", "nick_name", "g8_remark", "g11_remark", "teacher_sign"):
                exp = teacher[key]
                exp = None if exp == "NULL" else exp
                check(exp == actual, "导出忠实性", f"{COLS[c-1]}5({key})", exp, actual)
            else:
                check(close(num(teacher[key]), actual), "导出忠实性", f"{COLS[c-1]}5({key})",
                      round(num(teacher[key]), 2), None if actual is None else round(float(actual), 2))
        # 总计行 AJ
        aj = cellv(out, 5 + n, 36)
        check(close(num(teacher["total_workload"]), aj), "导出忠实性", "总计行AJ",
              round(num(teacher["total_workload"]), 2),
              None if aj is None else round(float(aj), 2))


def verify_rules(rule_checks):
    for item_id, label, expected, actual in rule_checks:
        check(expected == actual, "规则一致性", f"item {item_id} {label}", expected, actual)


def verify_rule_summary(user_id, semester, rules, teacher):
    if not teacher:
        return
    rows = db_query(f"""
        select i.item_type, i.calculated_workload, cd.J4, cd.R4, ci.W, ci.R6
        from biz_workload_item i
        left join biz_wl_course_design cd on cd.item_id = i.id and i.item_type = 'G4'
        left join biz_wl_concentrated_internship ci on ci.item_id = i.id and i.item_type = 'G6'
        where i.user_id = {user_id} and i.semester = '{semester}'
          and (i.status is null or i.status != 3)""")
    sums = {}
    for row in rows:
        typ = row["item_type"]
        value = dec(row["calculated_workload"]) or Decimal("0")
        if typ == "G4":
            value = scaled(required_dec(row["J4"], "summary G4 J4")
                           * required_dec(row["R4"], "summary G4 R4")
                           * rules["CONST_COURSE_DESIGN"])
        elif typ == "G6":
            people = min(required_dec(row["R6"], "summary G6 R6"), rules["CAP_R6_MAX"])
            value = scaled(required_dec(row["W"], "summary G6 W")
                           * people * rules["CONST_COURSE_DESIGN"])
        sums[typ] = sums.get(typ, Decimal("0")) + value
    g7 = scaled(sum((sums.get(f"G{i}", Decimal("0")) for i in range(1, 7)), Decimal("0")))
    g8 = scaled(sums.get("G8", Decimal("0")))
    g9 = scaled(sums.get("G9", Decimal("0")))
    g10 = scaled(g7 + g8 + g9)
    g11 = scaled(min(sums.get("G11", Decimal("0")), rules["CAP_G11_SEMESTER"]))
    total = scaled(g10 + g11)
    rated = dec(teacher["rated_workload"])
    excess = scaled(max(total - rated, Decimal("0")))
    for field, expected in [("G7", g7), ("G8", g8), ("G9", g9), ("G10", g10),
                            ("G11", g11), ("total_workload", total),
                            ("excess_workload", excess)]:
        check(expected == dec(teacher[field]), "规则一致性",
              f"summary.{field}", expected, dec(teacher[field]))


# ---------------------------------------------------------------- 4. 报告
def write_report(scenarios):
    total_checks = sum(s["checks"] for s in scenarios)
    total_diffs = sum(len(s["diffs"]) for s in scenarios)
    lines = [
        "# 附件1导出对齐验证报告", "",
        f"- 生成方式：`verify_attachment1_export.py`（登录后端 → 调导出端点 → 与模板/数据库逐项比对）",
        f"- 验证时间：{__import__('datetime').datetime.now().strftime('%Y-%m-%d %H:%M')}",
        f"- 检查点总数：{total_checks}，不一致：{total_diffs}",
        f"- 结论：{'✅ 全部场景零差异：导出忠实性与核算规则一致性均通过' if not total_diffs else '❌ 存在差异，见下表'}",
        "",
        "## 场景", "",
        "| 场景 | 教师 | 学期 | 数据行 | 检查点 | 差异 | 覆盖点 |",
        "|---|---|---|---|---|---|---|",
    ]
    for s in scenarios:
        lines.append(f"| {s['label']} | {s['user']} | {s['semester']} | {s['n']} | {s['checks']} | {len(s['diffs'])} | {s['scope']} |")
    lines.append("")
    if total_diffs:
        lines += ["## 差异明细", "", "| # | 场景 | 层 | 位置 | 期望 | 实际 |", "|---|---|---|---|---|---|"]
        i = 0
        for s in scenarios:
            for d in s["diffs"]:
                i += 1
                lines.append(f"| {i} | {s['label']} | {d['section']} | {d['where']} | {d['expected']} | {d['actual']} |")
        lines.append("")
    else:
        lines += [
            "## 验证覆盖范围", "",
            "- **结构**：39 列总数；第 2~4 行三层表头逐格全等；表头区与固定块合并单元格集合一致（固定块随行数平移）；"
            "标题占位替换（学年/学期号/学院）；总计行/签字区/备注行位置与内容。",
            "- **导出忠实性**：任务行 C~AB 列（26 字段/行）与数据库按任务聚合的落库值逐项比对；"
            "教师级列（A/B/AC~AM）只填首行、其余行为空；总计行 AJ。",
            "- **规则一致性**：G4 按 J4×R4×常量、不截断；G6 按 W×min(R6,有效规则上限)×常量；"
            "直接核对每条明细落库值，防止数据库与导出同时错误形成假阳性。",
            "- **精度/编码**：数值按 0.005 容差比对（DECIMAL(10,2) 无漂移）；中文字符串全等（utf8mb4 无乱码）；"
            "总行数精确等于 4+n+5（无 shiftRows 残留空行）。",
            "- **边界**：多行扩张（n>3，固定块下移）与单行收缩（n<3，固定块上移）均已覆盖。",
            "",
        ]
    path = os.path.join(OUT_DIR, "附件1导出对齐验证报告.md")
    with io.open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    return path


def run_scenario(label, user_id, semester, scope):
    user_id, semester = validate_inputs(user_id, semester)
    size = login_and_export(user_id, semester)
    rules = fetch_rules()
    expected, rule_checks = fetch_expected_rows(user_id, semester, rules)
    teacher = fetch_teacher(user_id, semester)
    if expected and not teacher:
        raise RuntimeError("存在附件1明细但尚无学期汇总，请先核算汇总")
    wb_out = openpyxl.load_workbook(TMP)
    wb_tpl = openpyxl.load_workbook(TEMPLATE)

    CHECKS[0] = 0
    DIFFS.clear()
    verify_structure(wb_out, wb_tpl, teacher, semester, len(expected))
    verify_data(wb_out, expected, teacher)
    verify_rules(rule_checks)
    verify_rule_summary(user_id, semester, rules, teacher)
    return {"label": label, "user": user_id, "semester": semester, "n": len(expected),
            "checks": CHECKS[0], "diffs": list(DIFFS), "scope": scope, "size": size}


def main():
    if len(sys.argv) > 2:
        scenarios_spec = [(sys.argv[1], sys.argv[2], "指定场景")]
    else:
        scenarios_spec = [
            ("2003", "2026-2027-1", "多行扩张：18 任务行、G1~G6 全类别、教师级列+总计"),
            ("2010", "2025-2026-1", "单行收缩：1 任务行、无汇总（教师级列留空）"),
        ]
    scenarios = []
    for uid, sem, scope in scenarios_spec:
        scenarios.append(run_scenario(f"{uid}@{sem}", uid, sem, scope))
        print(f"  场景 {uid}@{sem}: 检查点 {scenarios[-1]['checks']}，差异 {len(scenarios[-1]['diffs'])}")

    path = write_report(scenarios)
    total_diffs = sum(len(s["diffs"]) for s in scenarios)
    for s in scenarios:
        for d in s["diffs"][:20]:
            print(f"  [{d['section']}] {d['where']}: 期望 {d['expected']} / 实际 {d['actual']}")
    print(f"报告 → {path}")
    sys.exit(1 if total_diffs else 0)


if __name__ == "__main__":
    main()
