<template>
  <div class="app-container">
    <admin-dashboard v-hasRole="['admin', 'jiaowu']" />

    <teacher-dashboard v-hasRole="['teacher']" />
  </div>
</template>

<script setup name="Index">
// 引入刚刚封装好的两个组件
import AdminDashboard from './dashboard/AdminDashboard.vue'
import TeacherDashboard from './dashboard/TeacherDashboard.vue'

// 如果需要页面一加载就去后台请求一些公用数据，可以在这里写逻辑
</script>

<style scoped lang="scss">
/* 入口文件通常只需要控制最外层的容器样式即可 */
.app-container {
  padding: 0; /* 根据需要调整，因为子组件里可能已经有 padding 了 */
  background-color: #f0f2f5;
  min-height: calc(100vh - 84px);
}
</style>