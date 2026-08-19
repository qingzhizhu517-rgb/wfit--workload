<template>
  <div class="register">
    <el-form
      ref="registerRef"
      :model="registerForm"
      :rules="registerRules"
      class="register-form"
    >
      <h3 class="title">
        欢迎注册
      </h3>

      <div class="input-label">
        账号
      </div>
      <el-form-item prop="username">
        <el-input 
          v-model="registerForm.username" 
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
          v-model="registerForm.password"
          type="password"
          size="large" 
          auto-complete="off"
          placeholder="请输入密码"
          @keyup.enter="handleRegister"
        />
      </el-form-item>

      <div class="input-label">
        确认密码
      </div>
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          size="large" 
          auto-complete="off"
          placeholder="请再次输入您的密码"
          @keyup.enter="handleRegister"
        />
      </el-form-item>

      <template v-if="captchaEnabled">
        <div class="input-label">
          验证码
        </div>
        <el-form-item prop="code">
          <el-input
            v-model="registerForm.code" 
            size="large"
            auto-complete="off"
            placeholder="请输入验证码"
            style="width: 63%"
            @keyup.enter="handleRegister"
          />
          <div class="register-code">
            <img
              :src="codeUrl"
              class="register-code-img"
              @click="getCode"
            >
          </div>
        </el-form-item>
      </template>

      <el-form-item style="width:100%; margin-top: 15px; margin-bottom: 10px;">
        <el-button
          :loading="loading"
          size="large" 
          type="primary"
          class="register-btn"
          @click.prevent="handleRegister"
        >
          <span v-if="!loading">注 册</span>
          <span v-else>注 册 中...</span>
        </el-button>
      </el-form-item>
      
      <div class="login-link">
        <span>已有账号？ </span>
        <router-link
          class="link-type"
          :to="'/login'"
        >
          立即登录
        </router-link>
      </div>
    </el-form>
    
    <div class="el-register-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox } from 'element-plus'
import { getCodeImg, register } from '@/api/login'
import defaultSettings from '@/settings'
import { ref, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const router = useRouter()
const { proxy } = getCurrentInstance()

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  code: '',
  uuid: ''
})

const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, trigger: 'blur', message: '请输入您的账号' },
    { min: 2, max: 20, message: '用户账号长度必须介于 2 和 20 之间', trigger: 'blur' }
  ],
  password: [
    { required: true, trigger: 'blur', message: '请输入您的密码' },
    { min: 5, max: 20, message: '用户密码长度必须介于 5 和 20 之间', trigger: 'blur' },
    { pattern: /^[^<>"'|\\]+$/, message: '不能包含非法字符：< > " \' \\\ |', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, trigger: 'blur', message: '请再次输入您的密码' },
    { required: true, validator: equalToPassword, trigger: 'blur' }
  ],
  code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
}

const codeUrl = ref('')
const loading = ref(false)
const captchaEnabled = ref(true)

function handleRegister() {
  proxy.$refs.registerRef.validate(valid => {
    if (valid) {
      loading.value = true
      register(registerForm.value).then(res => {
        const username = registerForm.value.username
        ElMessageBox.alert('<font color=\'red\'>恭喜你，您的账号 ' + username + ' 注册成功！</font>', '系统提示', {
          dangerouslyUseHTMLString: true,
          type: 'success',
        }).then(() => {
          router.push('/login')
        }).catch(() => {})
      }).catch(() => {
        loading.value = false
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = 'data:image/gif;base64,' + res.img
      registerForm.value.uuid = res.uuid
    }
  })
}

getCode()
</script>

<style lang='scss' scoped>
.register {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  /* 确保背景图片路径与您的项目一致，保持与登录页相同 */
  background-image: url("../assets/images/material_all.jpg");
  background-size: cover;
  background-position: center;
}

.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #333333;
  font-size: 28px;
  font-weight: bold;
  letter-spacing: 2px;
}

.register-form {
  border-radius: 20px;
  width: 420px;
  padding: 40px 45px 30px 45px;
  z-index: 1;

  /* --- 核心毛玻璃效果 --- */
  background-color: rgba(255, 255, 255, 0.65); /* 半透明白色背景 */
  backdrop-filter: blur(12px); /* 背景模糊化 */
  border: 1px solid rgba(255, 255, 255, 0.5); /* 增加一个微弱的白边，增强玻璃质感 */
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1); /* 柔和的阴影 */
  /* ---------------------- */

  .el-input {
    height: 44px;

    /* 确保 Element Plus 输入框本体不受毛玻璃效果影响 */
    :deep(.el-input__wrapper) {
      background-color: #ffffff !important; /* 强制纯白底色 */
      box-shadow: 0 0 0 1px #dcdfe6 inset; /* 恢复默认的边框阴影 */
      border-radius: 6px;
    }
    
    :deep(.el-input__inner) {
      color: #333333; /* 确保文字颜色深色清晰 */
      height: 44px;
    }
  }
}

.input-label {
  font-size: 14px;
  font-weight: bold;
  color: #333333;
  margin-bottom: 10px;
}

.register-btn {
  width: 100%;
  height: 48px;
  border-radius: 6px;
  font-size: 16px;
  letter-spacing: 4px;
  background: #4a8df8;
  border: none;
  box-shadow: 0 4px 10px rgba(74, 141, 248, 0.3);

  &:hover {
    background: #3b7dec;
  }
}

.login-link {
  text-align: center;
  font-size: 13px;
  color: #666666;
  margin-top: 10px;

  .link-type {
    color: #4a8df8;
    text-decoration: none;
    font-weight: bold;

    &:hover {
      text-decoration: underline;
    }
  }
}

.register-code {
  width: 33%;
  height: 44px;
  float: right;

  img {
    cursor: pointer;
    vertical-align: middle;
    height: 44px;
    border-radius: 6px;
    background-color: #ffffff; /* 防止透明背景的验证码受到干扰 */
  }
}

.register-code-img {
  height: 44px;
  padding-left: 12px;
}

.el-register-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #666;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}
</style>