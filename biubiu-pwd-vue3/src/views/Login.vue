<template>
  <div class="login-container">
    <div class="login-wrapper">
      <div class="login-brand">
        <div class="brand-logo">
          <el-icon size="48"><Monitor /></el-icon>
        </div>
        <h1 class="brand-title">Biubiu陪玩</h1>
        <p class="brand-subtitle">专业游戏陪玩服务平台</p>
      </div>
      
      <el-card class="login-box" shadow="hover">
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
        
        <div class="login-footer">
          <p>注册即代表同意 <a href="#">服务协议</a> 和 <a href="#">隐私政策</a></p>
        </div>
      </el-card>
    </div>
    
    <div class="login-bg">
      <div class="bg-pattern"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Lock, User, Monitor } from '@element-plus/icons-vue'
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
    ElMessage.error(error.message || '登录失败')
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
    ElMessage.error(error.message || '注册失败')
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
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  
  .bg-pattern {
    position: absolute;
    top: -50%;
    left: -50%;
    right: -50%;
    bottom: -50%;
    background-image: 
      radial-gradient(circle at 20% 80%, rgba(64, 158, 255, 0.15) 0%, transparent 50%),
      radial-gradient(circle at 80% 20%, rgba(103, 194, 58, 0.15) 0%, transparent 50%),
      radial-gradient(circle at 40% 40%, rgba(230, 162, 60, 0.1) 0%, transparent 40%);
    animation: bgFloat 20s ease-in-out infinite;
  }
}

@keyframes bgFloat {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  33% { transform: translate(30px, -30px) rotate(1deg); }
  66% { transform: translate(-20px, 20px) rotate(-1deg); }
}

.login-wrapper {
  display: flex;
  align-items: center;
  gap: 80px;
  z-index: 1;
  padding: 40px;
}

.login-brand {
  text-align: center;
  color: #fff;
  
  .brand-logo {
    width: 100px;
    height: 100px;
    margin: 0 auto 24px;
    background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
    border-radius: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8px 32px rgba(64, 158, 255, 0.3);
    animation: logoPulse 2s ease-in-out infinite;
  }
  
  @keyframes logoPulse {
    0%, 100% { transform: scale(1); box-shadow: 0 8px 32px rgba(64, 158, 255, 0.3); }
    50% { transform: scale(1.05); box-shadow: 0 12px 40px rgba(64, 158, 255, 0.4); }
  }
  
  .brand-title {
    font-size: 42px;
    font-weight: 700;
    margin-bottom: 12px;
    background: linear-gradient(135deg, #fff 0%, #a0cfff 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
  
  .brand-subtitle {
    font-size: 16px;
    color: rgba(255, 255, 255, 0.7);
    letter-spacing: 2px;
  }
}

.login-box {
  width: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  
  :deep(.el-card__body) {
    padding: 32px;
  }
}

.login-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 28px;
  }
  
  :deep(.el-tabs__item) {
    font-size: 16px;
    font-weight: 500;
    
    &.is-active {
      font-weight: 600;
    }
  }
}

.login-form {
  .el-input {
    :deep(.el-input__wrapper) {
      border-radius: 10px;
      padding: 4px 15px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
      
      &.is-focus {
        box-shadow: 0 0 0 1px #409eff inset, 0 2px 8px rgba(64, 158, 255, 0.15);
      }
    }
    
    :deep(.el-input__inner) {
      height: 44px;
      font-size: 15px;
    }
  }
  
  .el-form-item {
    margin-bottom: 20px;
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  border: none;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.3);
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4);
  }
  
  &:active {
    transform: translateY(0);
  }
}

.login-footer {
  margin-top: 24px;
  text-align: center;
  
  p {
    font-size: 13px;
    color: #909399;
    
    a {
      color: #409eff;
      text-decoration: none;
      
      &:hover {
        text-decoration: underline;
      }
    }
  }
}

@media (max-width: 900px) {
  .login-wrapper {
    flex-direction: column;
    gap: 40px;
  }
  
  .login-brand {
    .brand-title {
      font-size: 32px;
    }
  }
  
  .login-box {
    width: 100%;
    max-width: 400px;
  }
}
</style>
