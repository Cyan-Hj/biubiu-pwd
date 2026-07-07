<template>
  <el-container class="layout-container">
    <!-- 桌面端侧边栏 -->
    <el-aside v-if="!isMobile" width="220px" class="aside">
      <div class="logo">
        <div class="logo-icon">
          <span class="logo-text-icon">电竞</span>
        </div>
        <div class="logo-text">
          <h3>电竞陪玩管理</h3>
          <span>专业 · 品质 · 信赖</span>
        </div>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        class="menu"
        background-color="transparent"
        text-color="#b2bec3"
        active-text-color="#fff"
        :collapse-transition="false"
      >
        <el-menu-item index="/dashboard" class="menu-item">
          <el-icon><HomeFilled /></el-icon>
          <span>首页概览</span>
        </el-menu-item>

        <el-menu-item v-if="isPlayer || isAdmin || isCustomerService" index="/grab-hall" class="menu-item">
          <el-icon><Tickets /></el-icon>
          <span>抢单大厅</span>
        </el-menu-item>

        <el-menu-item index="/orders" class="menu-item">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>

        <el-menu-item v-if="isAdmin || isCustomerService" index="/players" class="menu-item">
          <el-icon><UserFilled /></el-icon>
          <span>陪玩师管理</span>
        </el-menu-item>

        <el-menu-item v-if="isAdmin || isCustomerService" index="/boss" class="menu-item">
          <el-icon><User /></el-icon>
          <span>老板管理</span>
        </el-menu-item>

        <el-menu-item index="/finance" class="menu-item">
          <el-icon><Money /></el-icon>
          <span>财务管理</span>
        </el-menu-item>

        <el-menu-item v-if="isAdmin" index="/withdrawals" class="menu-item">
          <el-icon><Check /></el-icon>
          <span>提现审核</span>
        </el-menu-item>

        <el-menu-item v-if="isAdmin" index="/settings" class="menu-item">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>

      <div class="aside-footer">
        <div class="version">v2.0.0</div>
      </div>
    </el-aside>

    <!-- 移动端抽屉 -->
    <el-drawer
      v-if="isMobile"
      v-model="drawerVisible"
      direction="ltr"
      :show-close="false"
      :with-header="false"
      size="260px"
      class="mobile-drawer"
    >
      <div class="aside mobile-aside">
        <div class="logo">
          <div class="logo-icon">
            <span class="logo-text-icon">电竞</span>
          </div>
          <div class="logo-text">
            <h3>电竞陪玩管理</h3>
            <span>专业 · 品质 · 信赖</span>
          </div>
        </div>
        <el-menu
          :default-active="$route.path"
          router
          class="menu"
          background-color="transparent"
          text-color="#b2bec3"
          active-text-color="#fff"
          :collapse-transition="false"
          @select="handleMenuSelect"
        >
          <el-menu-item index="/dashboard" class="menu-item">
            <el-icon><HomeFilled /></el-icon>
            <span>首页概览</span>
          </el-menu-item>

          <el-menu-item v-if="isPlayer || isAdmin || isCustomerService" index="/grab-hall" class="menu-item">
            <el-icon><Tickets /></el-icon>
            <span>抢单大厅</span>
          </el-menu-item>

          <el-menu-item index="/orders" class="menu-item">
            <el-icon><List /></el-icon>
            <span>订单管理</span>
          </el-menu-item>

          <el-menu-item v-if="isAdmin || isCustomerService" index="/players" class="menu-item">
            <el-icon><UserFilled /></el-icon>
            <span>陪玩师管理</span>
          </el-menu-item>

          <el-menu-item v-if="isAdmin || isCustomerService" index="/boss" class="menu-item">
            <el-icon><User /></el-icon>
            <span>老板管理</span>
          </el-menu-item>

          <el-menu-item index="/finance" class="menu-item">
            <el-icon><Money /></el-icon>
            <span>财务管理</span>
          </el-menu-item>

          <el-menu-item v-if="isAdmin" index="/withdrawals" class="menu-item">
            <el-icon><Check /></el-icon>
            <span>提现审核</span>
          </el-menu-item>

          <el-menu-item v-if="isAdmin" index="/settings" class="menu-item">
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-menu>

        <div class="aside-footer">
          <div class="version">v2.0.0</div>
        </div>
      </div>
    </el-drawer>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon v-if="isMobile" class="menu-toggle" @click="drawerVisible = true">
            <Expand />
          </el-icon>
          <breadcrumb />
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-info">
              <el-avatar :size="isMobile ? 30 : 36" class="user-avatar">
                {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div v-if="!isMobile" class="user-detail">
                <span class="user-name">{{ userStore.userInfo?.nickname }}</span>
                <span class="user-role">{{ getRoleText() }}</span>
              </div>
              <el-icon v-if="!isMobile" class="dropdown-icon"><arrow-down /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人信息
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { 
  HomeFilled, UserFilled, List, Money, Check, Setting, 
  ArrowDown, User, SwitchButton, Expand, Tickets
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const isAdmin = computed(() => userStore.isAdmin)
const isCustomerService = computed(() => userStore.isCustomerService)
const isPlayer = computed(() => userStore.isPlayer)

const isMobile = ref(false)
const drawerVisible = ref(false)

const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const handleMenuSelect = () => {
  drawerVisible.value = false
}

const getRoleText = () => {
  if (isAdmin.value) return '管理员'
  if (isCustomerService.value) return '客服'
  return '陪玩师'
}

const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      router.push('/login')
      ElMessage.success('已退出登录')
    })
  }
}

onMounted(async () => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  try {
    await userStore.fetchUserInfo()
  } catch (e) {}
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
  background: var(--bg-body);
}

.aside {
  background: var(--bg-sidebar);
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.15);
}

.logo {
  height: 72px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  
  .logo-icon {
    width: 40px;
    height: 40px;
    background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12px;
    flex-shrink: 0;
    box-shadow: 0 4px 12px rgba(108, 92, 231, 0.3);
  }
  
  .logo-text-icon {
    color: #fff;
    font-size: 15px;
    font-weight: 700;
    letter-spacing: 2px;
  }
  
  .logo-text {
    h3 {
      color: #fff;
      font-size: 15px;
      font-weight: 600;
      margin: 0;
      letter-spacing: 1px;
      white-space: nowrap;
    }
    
    span {
      color: rgba(255, 255, 255, 0.5);
      font-size: 11px;
      letter-spacing: 1px;
    }
  }
}

.menu {
  flex: 1;
  border-right: none;
  padding: 12px 10px;
  
  :deep(.el-menu-item) {
    height: 48px;
    line-height: 48px;
    margin: 4px 0;
    border-radius: var(--border-radius);
    transition: all 0.3s ease;
    color: #b2bec3;
    
    &:hover {
      background: rgba(255, 255, 255, 0.06) !important;
      color: #fff !important;
    }
    
    &.is-active {
      background: var(--bg-sidebar-active) !important;
      color: #fff !important;
      font-weight: 600;
      
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 3px;
        height: 20px;
        background: linear-gradient(180deg, var(--primary-color) 0%, var(--primary-light) 100%);
        border-radius: 0 2px 2px 0;
      }
    }
    
    .el-icon {
      font-size: 18px;
      margin-right: 10px;
      color: inherit;
    }
    
    span {
      font-size: 14px;
      font-weight: 500;
    }
  }
}

.aside-footer {
  padding: 14px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  
  .version {
    color: rgba(255, 255, 255, 0.35);
    font-size: 11px;
    text-align: center;
  }
}

.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.menu-toggle {
  font-size: 22px;
  color: var(--text-primary);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s;

  &:hover {
    background: var(--primary-bg);
    color: var(--primary-color);
  }
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: background 0.25s;
  
  &:hover {
    background: var(--primary-bg);
  }
  
  .user-avatar {
    background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
    color: #fff;
    font-weight: 600;
    font-size: 14px;
  }
  
  .user-detail {
    display: flex;
    flex-direction: column;
    
    .user-name {
      font-size: 14px;
      font-weight: 600;
      color: var(--text-primary);
    }
    
    .user-role {
      font-size: 12px;
      color: var(--text-secondary);
    }
  }
  
  .dropdown-icon {
    color: var(--text-tertiary);
    font-size: 14px;
  }
}

.main {
  background: transparent;
  padding: 20px;
  overflow-y: auto;
}

.mobile-drawer {
  :deep(.el-drawer__body) {
    padding: 0;
  }

  :deep(.el-drawer) {
    background: transparent;
    box-shadow: none;
  }

  .mobile-aside {
    height: 100%;
    box-shadow: 4px 0 20px rgba(0, 0, 0, 0.25);
  }
}

@media (max-width: 768px) {
  .header {
    padding: 0 12px;
    height: 50px;
  }

  .main {
    padding: 12px;
  }
}

@media (max-width: 480px) {
  .main {
    padding: 8px;
  }
}
</style>
