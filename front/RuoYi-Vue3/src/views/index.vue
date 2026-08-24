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

  if (roles.includes('admin')) {
    target = '/admin/dashboard'
  } else if (roles.includes('leader')) {
    target = '/leader/dashboard'
  } else if (roles.includes('jiaowu') || roles.includes('assistant')) {
    target = '/jiaowu/dashboard'
  }

  router.replace(target)
})
</script>
