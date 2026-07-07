<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <div class="brand">
          <div class="brand-logo">电竞</div>
          <div class="brand-info">
            <h1 class="brand-title">电竞陪玩管理</h1>
            <p class="brand-subtitle">专业 · 品质 · 信赖</p>
          </div>
        </div>
      </div>

      <div class="login-body">
        <h2 class="form-title">欢迎登录</h2>
        <p class="form-desc">请输入您的账号信息</p>

        <el-tabs v-model="activeTab" stretch class="login-tabs">
          <el-tab-pane label="账号登录" name="login">
            <el-form :model="loginForm" :rules="rules" ref="loginFormRef" class="login-form">
              <el-form-item prop="phone">
                <el-input
                  v-model="loginForm.phone"
                  placeholder="请输入手机号"
                  size="large"
                  clearable
                >
                  <template #prefix>
                    <el-icon><Iphone /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入密码"
                  size="large"
                  show-password
                  @keyup.enter="handleLogin"
                >
                  <template #prefix>
                    <el-icon><Lock /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  :loading="loading"
                  @click="handleLogin"
                  class="login-btn"
                >
                  立即登录
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册陪玩师" name="register">
            <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" class="login-form">
              <el-form-item prop="phone">
                <el-input
                  v-model="registerForm.phone"
                  placeholder="请输入手机号"
                  size="large"
                  clearable
                >
                  <template #prefix>
                    <el-icon><Iphone /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item prop="nickname">
                <el-input
                  v-model="registerForm.nickname"
                  placeholder="请输入昵称"
                  size="large"
                  clearable
                >
                  <template #prefix>
                    <el-icon><User /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  placeholder="设置密码"
                  size="large"
                  show-password
                >
                  <template #prefix>
                    <el-icon><Lock /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item prop="confirmPassword">
                <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="确认密码"
                  size="large"
                  show-password
                  @keyup.enter="handleRegister"
                >
                  <template #prefix>
                    <el-icon><Lock /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  :loading="loading"
                  @click="handleRegister"
                  class="login-btn"
                >
                  立即注册
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div class="login-footer">
        <p>注册即代表同意 <a href="#">服务协议</a> 和 <a href="#">隐私政策</a></p>
      </div>
    </div>

    <div class="login-bg">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { register } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()

const loginForm = reactive({
  phone: '',
  password: ''
})

const registerForm = reactive({
  phone: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const validatePhone = (rule, value, callback) => {
  const phoneRegex = /^1[3-9]\d{9}$/
  if (!value) {
    callback(new Error('请输入手机号'))
  } else if (!phoneRegex.test(value)) {
    callback(new Error('请输入正确的手机号'))
  } else {
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({
      phone: registerForm.phone,
      nickname: registerForm.nickname,
      password: registerForm.password
    })
    ElMessage.success('注册成功，请等待管理员审核')
    activeTab.value = 'login'
  } catch (error) {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
  position: relative;
  overflow: hidden;
  padding: 40px 20px;
}

.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  pointer-events: none;

  .bg-circle {
    position: absolute;
    border-radius: 50%;
    opacity: 0.4;
  }

  .bg-circle-1 {
    width: 600px;
    height: 600px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.15) 0%, rgba(118, 75, 162, 0.1) 100%);
    top: -200px;
    right: -200px;
  }

  .bg-circle-2 {
    width: 400px;
    height: 400px;
    background: linear-gradient(135deg, rgba(118, 75, 162, 0.1) 0%, rgba(102, 126, 234, 0.08) 100%);
    bottom: -100px;
    left: -100px;
  }
}

.login-box {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 440px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.login-header {
  padding: 40px 40px 0;
  text-align: center;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 16px;

  .brand-logo {
    width: 56px;
    height: 56px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 20px;
    font-weight: 700;
    letter-spacing: 2px;
    flex-shrink: 0;
  }

  .brand-info {
    text-align: left;
  }

  .brand-title {
    font-size: 24px;
    font-weight: 700;
    color: #1a1a2e;
    margin: 0 0 4px 0;
    letter-spacing: 2px;
  }

  .brand-subtitle {
    font-size: 13px;
    color: #909399;
    margin: 0;
    letter-spacing: 4px;
  }
}

.login-body {
  padding: 32px 40px 40px;
}

.form-title {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px 0;
  text-align: center;
}

.form-desc {
  font-size: 14px;
  color: #909399;
  margin: 0 0 28px 0;
  text-align: center;
}

.login-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 28px;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
    background: #e4e7ed;
  }

  :deep(.el-tabs__item) {
    font-size: 15px;
    font-weight: 500;
    color: #606266;
    padding: 0 24px;

    &.is-active {
      font-weight: 600;
      color: #667eea;
    }
  }

  :deep(.el-tabs__active-bar) {
    background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
    height: 3px;
    border-radius: 3px 3px 0 0;
  }
}

.login-form {
  .el-input {
    :deep(.el-input__wrapper) {
      border-radius: 10px;
      padding: 4px 16px;
      box-shadow: 0 0 0 1px #e4e7ed inset;
      transition: all 0.3s;

      &:hover {
        box-shadow: 0 0 0 1px #c0c4cc inset;
      }

      &.is-focus {
        box-shadow: 0 0 0 1px #667eea inset, 0 0 0 4px rgba(102, 126, 234, 0.08);
      }
    }

    :deep(.el-input__inner) {
      height: 46px;
      font-size: 15px;
    }

    :deep(.el-input__prefix) {
      color: #909399;
    }
  }

  .el-form-item {
    margin-bottom: 20px;

    &:last-child {
      margin-bottom: 0;
      margin-top: 28px;
    }
  }
}

.login-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
  letter-spacing: 2px;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
  }

  &:active {
    transform: translateY(0);
  }
}

.login-footer {
  padding: 20px 40px;
  background: #fafbfc;
  border-top: 1px solid #f0f0f0;
  text-align: center;

  p {
    font-size: 13px;
    color: #909399;
    margin: 0;

    a {
      color: #667eea;
      text-decoration: none;
      font-weight: 500;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

@media (max-width: 480px) {
  .login-container {
    padding: 20px;
    background: #fff;
  }

  .login-box {
    box-shadow: none;
    border-radius: 0;
  }

  .login-header {
    padding: 24px 24px 0;
  }

  .brand {
    flex-direction: column;
    gap: 12px;

    .brand-info {
      text-align: center;
    }
  }

  .login-body {
    padding: 24px;
  }

  .login-footer {
    padding: 16px 24px;
  }
}
</style>
