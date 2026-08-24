<template>
  <div class="login">
    <el-form
      ref="loginRef"
      :model="loginForm"
      :rules="loginRules"
      class="login-form"
    >
      <h3 class="title">
        工作量汇总管理系统
      </h3>

      <div class="input-label">
        账号
      </div>
      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          type="text"
          size="large"
          auto-complete="off"
          placeholder="请输入手机号/工号"
        />
      </el-form-item>

      <div class="input-label">
        密码
      </div>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          size="large"
          auto-complete="off"
          placeholder="请输入密码"
          @keyup.enter="handleLogin"
        />
      </el-form-item>

      <template v-if="captchaEnabled">
        <div class="input-label">
          验证码
        </div>
        <el-form-item prop="code">
          <el-input
            v-model="loginForm.code"
            size="large"
            auto-complete="off"
            placeholder="请输入验证码"
            style="width: 63%"
            @keyup.enter="handleLogin"
          />
          <div class="login-code">
            <img
              :src="codeUrl"
              class="login-code-img"
              @click="getCode"
            >
          </div>
        </el-form-item>
      </template>

      <div class="action-row">
        <el-checkbox v-model="loginForm.rememberMe">
          记住密码
        </el-checkbox>
        <el-link
          type="info"
          :underline="false"
          class="forgot-pwd"
          @click="handleForgotPwd"
        >
          忘记密码?
        </el-link>
      </div>

      <el-form-item style="width:100%; margin-bottom: 10px;">
        <el-button
          :loading="loading"
          size="large"
          type="primary"
          class="login-btn"
          @click.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
      </el-form-item>

      <div
        v-if="register"
        class="register-link"
      >
        <span>还没有账号？ </span>
        <router-link
          class="link-type"
          :to="'/register'"
        >
          立即注册
        </router-link>
      </div>
    </el-form>

    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { getCodeImg } from '@/api/login'
import Cookies from 'js-cookie'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'
import { ref, watch, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus' // 显式引入弹窗组件

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: '',
  password: '',
  rememberMe: false,
  code: '',
  uuid: ''
})

// 在开发环境中使用默认值
if (import.meta.env.DEV) {
  loginForm.value.username = 'admin'
  loginForm.value.password = 'admin123'
}

const loginRules = {
  username: [{ required: true, trigger: 'blur', message: '请输入您的账号' }],
  password: [{ required: true, trigger: 'blur', message: '请输入您的密码' }],
  code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
}

const codeUrl = ref('')
const loading = ref(false)
const captchaEnabled = ref(true)
const register = ref(true)
const redirect = ref(undefined)

watch(route, (newRoute) => {
  redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

/** 忘记密码点击处理 */
function handleForgotPwd() {
  ElMessageBox.alert('请联系教务管理人员进行重置', '提示', {
    confirmButtonText: '确定',
    type: 'info',
  })
}

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (!valid) return
    
    loading.value = true
    saveLoginCredentials()
    
    userStore.login(loginForm.value)
      .then(handleLoginSuccess)
      .catch(handleLoginFailure)
  })
}

function saveLoginCredentials() {
  if (loginForm.value.rememberMe) {
    Cookies.set('username', loginForm.value.username, { expires: 30 })
    Cookies.set('password', encrypt(loginForm.value.password), { expires: 30 })
    Cookies.set('rememberMe', loginForm.value.rememberMe, { expires: 30 })
  } else {
    Cookies.remove('username')
    Cookies.remove('password')
    Cookies.remove('rememberMe')
  }
}

function handleLoginSuccess() {
  const query = route.query
  const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
    if (cur !== 'redirect') {
      acc[cur] = query[cur]
    }
    return acc
  }, {})
  router.push({ path: redirect.value || '/', query: otherQueryParams })
}

function handleLoginFailure() {
  loading.value = false
  if (captchaEnabled.value) {
    getCode()
  }
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = 'data:image/gif;base64,' + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get('username')
  const password = Cookies.get('password')
  const rememberMe = Cookies.get('rememberMe')
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

getCode()
getCookie()
</script>

<style lang='scss' scoped>
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-image: url("../assets/images/material_all.jpg");
  background-size: cover;
  background-position: center;
}

.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: var(--el-text-color-primary);
  font-size: var(--wfit-font-2xl);
  font-weight: 700;
  letter-spacing: 2px;
}

.login-form {
  border-radius: var(--wfit-radius-lg);
  width: 420px;
  padding: 40px 45px 30px 45px;
  z-index: 1;
  background-color: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--wfit-shadow-lg);

  .el-input {
    height: 44px;
    :deep(.el-input__wrapper) {
      background-color: #ffffff !important;
      box-shadow: 0 0 0 1px var(--el-border-color) inset;
      border-radius: var(--wfit-radius-sm);
      transition: box-shadow var(--wfit-transition);
    }
    :deep(.el-input__wrapper:hover) {
      box-shadow: 0 0 0 1px var(--el-border-color-hover) inset;
    }
    :deep(.el-input__wrapper.is-focus) {
      box-shadow: 0 0 0 1px var(--el-color-primary) inset;
    }
    :deep(.el-input__inner) {
      color: var(--el-text-color-primary);
      height: 44px;
    }
  }
}

.input-label {
  font-size: var(--wfit-font-md);
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: var(--wfit-space-sm);
}

.action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--wfit-space-lg);

  :deep(.el-checkbox__label) {
    color: var(--el-text-color-regular);
  }

  .forgot-pwd {
    font-size: var(--wfit-font-md);
    color: var(--el-text-color-secondary) !important;
    
    &:hover {
      color: var(--el-color-primary) !important;
    }
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: var(--wfit-radius-sm);
  font-size: var(--wfit-font-lg);
  letter-spacing: 4px;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.25);
  transition: all var(--wfit-transition);
}

.register-link {
  text-align: center;
  font-size: var(--wfit-font-sm);
  color: var(--el-text-color-secondary);
  margin-top: var(--wfit-space-sm);

  .link-type {
    color: var(--el-color-primary);
    text-decoration: none;
    font-weight: 600;

    &:hover {
      text-decoration: underline;
    }
  }
}

.login-code {
  width: 33%;
  height: 44px;
  float: right;

  img {
    cursor: pointer;
    vertical-align: middle;
    height: 44px;
    border-radius: 6px;
    background-color: #ffffff;
  }
}

.login-code-img {
  height: 44px;
  padding-left: 12px;
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: var(--wfit-font-xs);
  letter-spacing: 1px;
}
</style>