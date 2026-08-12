/**
 * 业务枚举选项与状态映射（取值与 rear/sql/01_biz_schema.sql 字段注释保持一致）
 * 用法：
 *   import { majorCategoryOptions, workloadItemStatusMap } from '@/utils/bizDict'
 *   <biz-tag :value="row.status" :map="workloadItemStatusMap" />
 */

/** 下拉选项（label 展示 / value 存库） */
const opt = (label, value) => ({ label, value: value ?? label })

export const educationLevelOptions = [opt('本科(含专升本)', '本科'), opt('专科')]
export const majorCategoryOptions = [opt('理工类'), opt('文史类'), opt('艺术类'), opt('其他')]
export const courseNatureOptions = [opt('必修'), opt('选修')]
export const courseLevelOptions = [opt('省级一流'), opt('校级精品'), opt('其他')]
export const courseRoleOptions = [opt('主持人'), opt('团队前3'), opt('独立')]

export const teacherTitleOptions = [opt('教授'), opt('副教授'), opt('讲师'), opt('助教'), opt('未定级')]
export const teacherNatureOptions = [opt('专任'), opt('外聘'), opt('校企'), opt('银龄'), opt('青州外聘')]
export const specialStatusOptions = [opt('正常'), opt('产假'), opt('在职读博'), opt('访学')]
export const enterpriseEvalOptions = [opt('优秀'), opt('合格'), opt('不合格')]

export const roleTypeOptions = [
  opt('班主任'), opt('系主任'), opt('教研室主任'), opt('专业负责人'),
  opt('俱乐部经理'), opt('实验人员'), opt('督导'), opt('中层副职'), opt('心理中心')
]

export const importTypeOptions = [opt('教学任务'), opt('教师信息'), opt('岗位任职')]

export const itemTypeOptions = [
  opt('G1 理论课', 'G1'), opt('G2 课内实践', 'G2'), opt('G3 教学实习实训', 'G3'),
  opt('G4 课程设计', 'G4'), opt('G5 毕业论文', 'G5'), opt('G6 集中实习', 'G6'),
  opt('G7 第一课堂', 'G7'), opt('G8 第二课堂', 'G8'), opt('G9 其他', 'G9'),
  opt('G11 管理服务', 'G11')
]

export const sourceTypeOptions = [opt('导入', 'IMPORT'), opt('手工录入', 'MANUAL')]

export const feeTypeOptions = [
  opt('A 重修辅导金', 'A'), opt('B 实习指导费', 'B'), opt('C 论文重修指导', 'C'),
  opt('D 代阅卷', 'D'), opt('E 讲座', 'E'), opt('F 运动会/体测', 'F'), opt('G 夜间值班', 'G')
]

export const feeSubtypeMap = {
  A: [opt('跟班重修', '跟班'), opt('单独开班', '单独开班'), opt('自学辅导', '自学辅导')],
  B: [opt('分散实习', '分散'), opt('集中实习(不跟班)', '集中不跟班')]
}

/** 状态映射：value -> { label, type(el-tag) } */
const st = (label, type) => ({ label, type })

export const workloadItemStatusMap = {
  0: st('草稿', 'info'), 1: st('已核对', 'success'), 2: st('有异议', 'warning'), 3: st('已驳回', 'danger')
}
export const approvalStatusMap = {
  0: st('未审批', 'info'), 1: st('已通过', 'success'), 2: st('已驳回', 'danger')
}
export const appealStatusMap = {
  0: st('无', 'info'), 1: st('申诉中', 'warning'), 2: st('已处理', 'success'), 3: st('已驳回', 'danger')
}
export const summaryStatusMap = {
  0: st('填报中', 'info'), 1: st('待教务审核', 'primary'), 2: st('待院领导签字', 'warning'), 3: st('已完结', 'success')
}
export const importBatchStatusMap = {
  0: st('解析中', 'warning'), 1: st('待确认', 'primary'), 2: st('已导入', 'success'), 3: st('已驳回', 'danger'), 4: st('失败', 'danger')
}
export const normalStatusMap = {
  1: st('正常', 'success'), 0: st('停用', 'danger')
}
export const yesNoMap = {
  1: st('是', 'warning'), 0: st('否', 'info')
}

/** 酬金发放状态（原 payRecord/index.vue 内联定义收敛，键值保持不变） */
export const payStatusMap = {
  0: st('未发放', 'info'), 1: st('已发放', 'success')
}

/** 教师特殊状态（原 teacherProfile/index.vue 内联定义收敛，键值保持不变） */
export const specialStatusMap = {
  '正常': st('正常', 'success'),
  '产假': st('产假', 'warning'),
  '在职读博': st('在职读博', 'warning'),
  '访学': st('访学', 'primary')
}

/** 将 Options 数组转为 biz-tag 可用的 Map（value -> { label, type }） */
export function optionsToMap(options, type = 'primary') {
  return Object.fromEntries(options.map(o => [o.value, { label: o.label, type }]))
}

/** 工作量类别 biz-tag Map（原 workloadItem/index.vue 内联收敛，label 同 itemTypeOptions） */
export const itemTypeMap = optionsToMap(itemTypeOptions)

/** 数据来源 biz-tag Map（原 workloadItem/index.vue 内联收敛，键值保持不变） */
export const sourceTypeMap = { IMPORT: st('导入', 'info'), MANUAL: st('手工', 'success') }

/** 管理岗位 biz-tag Map（原 roleAssignment/index.vue 内联收敛，键值同 roleTypeOptions） */
export const roleTypeMap = optionsToMap(roleTypeOptions)

/** 金额展示（千分位，空值显示 -） */
export function formatAmount(v, digits = 2) {
  if (v === null || v === undefined || v === '') return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

/** 空值兜底 */
export function dash(v) {
  return v === null || v === undefined || v === '' ? '-' : v
}

/** 数值展示（千分位，最多 maxDigits 位小数且去尾零，空值显示 -），用于学时/人数/系数/工作量等 */
export function formatNumber(v, maxDigits = 2) {
  if (v === null || v === undefined || v === '') return dash()
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: maxDigits })
}

/** 业务规则常量（规则来源：rear/sql/03_calc_rules.sql 核算规则参数，与 SummaryCalcServiceImpl 口径一致） */
/** 学期工作量封顶（学时）：绩效酬金 = 专任 (min(total, CAP_200PCT) − rated) × rate，规则参数 CAP_200PCT 默认 540 */
export const SEMESTER_WORKLOAD_CAP = 540
/** G11 管理服务学期累计封顶（学时）：规则参数 CAP_G11_SEMESTER 默认 180，多岗叠加封顶在汇总层处理 */
export const G11_SEMESTER_CAP = 180

/**
 * 根据当前日期推算学年学期
 * 规则（与后端 SemesterCalendar 数据口径核对：秋季学期 09-01~01-31 为第 1 学期，春季学期 02-20~07-15 为第 2 学期）：
 *   - 8 月及以后（新学年筹备/开学）→ (年)-(年+1)-1
 *   - 2 月~7 月（春季学期及暑假前）→ (年-1)-(年)-2
 *   - 1 月（秋季学期末，跨学年归属上一学年第 1 学期）→ (年-1)-(年)-1
 * @returns {string} 如 '2025-2026-1'
 */
export function getCurrentSemester() {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1 // 1-12
  if (month >= 8) {
    return `${year}-${year + 1}-1`
  } else if (month >= 2) {
    return `${year - 1}-${year}-2`
  } else {
    return `${year - 1}-${year}-1`
  }
}
