<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col
        :span="6"
        :xs="24"
      >
        <el-card class="box-card">
          <template #header>
            <div class="clearfix">
              <span>个人信息</span>
            </div>
          </template>
          <div>
            <div class="text-center">
              <userAvatar />
            </div>
            <ul class="list-group list-group-striped">
              <li class="list-group-item">
                <svg-icon icon-class="user" />用户名称
                <div class="pull-right">
                  {{ state.user.userName }}
                </div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="phone" />手机号码
                <div class="pull-right">
                  {{ state.user.phonenumber }}
                </div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="email" />用户邮箱
                <div class="pull-right">
                  {{ state.user.email }}
                </div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="tree" />所属部门
                <div
                  v-if="state.user.dept"
                  class="pull-right"
                >
                  {{ state.user.dept.deptName }} / {{ state.postGroup }}
                </div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="peoples" />所属角色
                <div class="pull-right">
                  {{ state.roleGroup }}
                </div>
              </li>
              <li class="list-group-item">
                <svg-icon icon-class="date" />创建日期
                <div class="pull-right">
                  {{ state.user.createTime }}
                </div>
              </li>
            </ul>
          </div>
        </el-card>
      </el-col>
      <el-col
        :span="18"
        :xs="24"
      >
        <el-card>
          <template #header>
            <div class="clearfix">
              <span>基本资料</span>
            </div>
          </template>
          <el-alert
            v-if="userStore.mustChangePwd"
            type="warning"
            :closable="false"
            show-icon
            title="您正在使用初始密码，请先在「修改密码」页设置新密码，之后才能使用系统其他功能。"
            style="margin-bottom: 12px"
          />
          <el-tabs v-model="selectedTab">
            <el-tab-pane
              label="基本资料"
              name="userinfo"
            >
              <userInfo :user="state.user" />
            </el-tab-pane>
            <el-tab-pane
              label="修改密码"
              name="resetPwd"
            >
              <resetPwd />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Profile">
import userAvatar from './userAvatar'
import userInfo from './userInfo'
import resetPwd from './resetPwd'
import { getUserProfile } from '@/api/system/user'
import useUserStore from '@/store/modules/user'

const route = useRoute()
const userStore = useUserStore()
// 强制改密时直接落在「修改密码」页，省去用户自己切页签
const selectedTab = ref(userStore.mustChangePwd ? 'resetPwd' : 'userinfo')
const state = reactive({
  user: {},
  roleGroup: {},
  postGroup: {}
})

function getUser() {
  getUserProfile().then(response => {
    state.user = response.data
    state.roleGroup = response.roleGroup
    state.postGroup = response.postGroup
  })
}

onMounted(() => {
  const activeTab = route.params && route.params.activeTab
  if (activeTab) {
    selectedTab.value = activeTab
  }
  getUser()
})
</script>
