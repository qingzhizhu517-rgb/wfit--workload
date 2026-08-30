import { ref } from 'vue'
import { listUserSimple } from '@/api/system/user'

/**
 * 用户简要信息全局缓存：所有业务页面共享一次请求
 * userMap: { [userId]: { userId, userName, nickName, deptId, deptName } }
 */
const userMap = ref({})
const userList = ref([])
let loadingPromise = null

export function useUserMap() {
  if (!loadingPromise) {
    loadingPromise = load()
  }

  /** 表格展示：张三（2025001） */
  const userLabel = (userId) => {
    if (userId === null || userId === undefined || userId === '') return '-'
    const u = userMap.value[userId]
    return u ? `${u.nickName}（${u.userName}）` : String(userId)
  }

  /** 仅姓名 */
  const userName = (userId) => userMap.value[userId]?.nickName || (userId ?? '-')

  /** 仅工号 */
  const userCode = (userId) => userMap.value[userId]?.userName || (userId ?? '-')

  /** 院部名 */
  const deptName = (userId) => userMap.value[userId]?.deptName || '-'

  return { userMap, userList, userLabel, userName, userCode, deptName, refresh }
}

/** 拉取并填充缓存 */
function load() {
  return listUserSimple().then(res => {
    const list = res.data || []
    userList.value = list
    const map = {}
    list.forEach(u => { map[u.userId] = u })
    userMap.value = map
    return map
  }).catch(() => {
    loadingPromise = null
  })
}

/**
 * 强制刷新缓存：用于导入等会新增/变更用户的场景，
 * 使新用户能立即在姓名/院部列正确回显（否则命中旧的模块级缓存显示原始 userId）。
 */
export function refresh() {
  loadingPromise = load()
  return loadingPromise
}
