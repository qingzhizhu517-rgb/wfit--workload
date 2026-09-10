<template>
  <div />
</template>

<script setup name="Index">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

onMounted(() => {
  const roles = userStore.roles || []
  let target = '/teacher/dashboard' // 默认教师端

  // 2026-09-10 审批简化为两级后，院领导不再有任何审批环节，
  // /leader/dashboard（院领导工作台）已随之下线（LeaderDashboard.vue 与路由均已删除）。
  // 此处刻意不再保留 leader 分支——若仍写着 /leader/dashboard，院领导登录会落到不存在的路由。
  // 注：leader 角色与 leader_test 账号本身是否保留，属角色模型收敛，另行处理。
  if (roles.includes('admin')) {
    target = '/admin/dashboard'
  } else if (roles.includes('jiaowu') || roles.includes('assistant')) {
    target = '/jiaowu/dashboard'
  }

  router.replace(target)
})
</script>
