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
  0: st('草稿', 'info'), 1: st('已公示', 'primary'), 2: st('已审核', 'success'), 3: st('已锁定', 'warning')
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

/** 金额展示（千分位，空值显示 -） */
export function formatAmount(v, digits = 2) {
  if (v === null || v === undefined || v === '') return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

/** 空值兜底 */
export function dash(v) {
  return v === null || v === undefined || v === '' ? '-' : v
}

/**
 * 根据当前日期推算学年学期
 * 规则：9月~次年1月 = 第一学期，2月~8月 = 第二学期
 * @returns {string} 如 '2025-2026-1'
 */
export function getCurrentSemester() {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1 // 1-12
  if (month >= 9) {
    return `${year}-${year + 1}-1`
  } else if (month >= 2) {
    return `${year - 1}-${year}-2`
  } else {
    return `${year - 1}-${year}-1`
  }
}
