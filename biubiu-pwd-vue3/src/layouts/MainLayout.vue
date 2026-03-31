<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <div class="logo-icon">
          <el-icon size="28"><Monitor /></el-icon>
        </div>
        <div class="logo-text">
          <h3>Biubiu陪玩</h3>
          <span>专业陪玩平台</span>
        </div>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        class="menu"
        background-color="#1a1a2e"
        text-color="#a0a3bd"
        active-text-color="#fff"
        :collapse-transition="false"
      >
        <el-menu-item index="/dashboard" class="menu-item">
          <el-icon><HomeFilled /></el-icon>
          <span>首页概览</span>
        </el-menu-item>

        <el-menu-item v-if="isAdmin || isCustomerService" index="/players" class="menu-item">
          <el-icon><UserFilled /></el-icon>
          <span>陪玩师管理</span>
        </el-menu-item>

        <el-menu-item index="/orders" class="menu-item">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>

        <el-menu-item index="/finance" class="menu-item">
          <el-icon><Money /></el-icon>
          <span>财务管理</span>
        </el-menu-item>

        <el-menu-item v-if="isAdmin || isCustomerService" index="/boss" class="menu-item">
          <el-icon><User /></el-icon>
          <span>老板预存</span>
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
        <div class="version">v1.0.0</div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <breadcrumb />
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-info">
              <el-avatar :size="36" class="user-avatar">
                {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="user-detail">
                <span class="user-name">{{ userStore.userInfo?.nickname }}</span>
                <span class="user-role">{{ getRoleText() }}</span>
              </div>
              <el-icon class="dropdown-icon"><arrow-down /></el-icon>
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
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { 
  HomeFilled, UserFilled, List, Money, Check, Setting, 
  ArrowDown, User, SwitchButton, Monitor 
} from '@element-plus/icons-vue'

// 导入 User 图标用于老板预存菜单

const router = useRouter()
const userStore = useUserStore()

const isAdmin = computed(() => userStore.isAdmin)
const isCustomerService = computed(() => userStore.isCustomerService)

const getRoleText = () => {
  if (isAdmin.value) return '管理员'
  if (isCustomerService.value) return '客服'
  return '陪玩师'
}

const handleCommand = (command) => {
  if (command === 'logout') {
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
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
  background: #f5f7fa;
}

.aside {
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  display: flex;
  flex-direction: column;
  box-shadow: 4px 0 16px rgba(0, 0, 0, 0.1);
}

.logo {
  height: 80px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  
  .logo-icon {
    width: 44px;
    height: 44px;
    background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    margin-right: 12px;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  }
  
  .logo-text {
    h3 {
      color: #fff;
      font-size: 18px;
      font-weight: 600;
      margin: 0;
      letter-spacing: 1px;
    }
    
    span {
      color: rgba(255, 255, 255, 0.5);
      font-size: 12px;
    }
  }
}

.menu {
  flex: 1;
  border-right: none;
  padding: 12px 0;
  
  :deep(.el-menu-item) {
    height: 50px;
    line-height: 50px;
    margin: 4px 12px;
    border-radius: 8px;
    transition: all 0.3s;
    
    &:hover {
      background: rgba(64, 158, 255, 0.1) !important;
      color: #fff !important;
    }
    
    &.is-active {
      background: linear-gradient(135deg, #409eff 0%, #67c23a 100%) !important;
      color: #fff !important;
      box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
    }
    
    .el-icon {
      font-size: 18px;
      margin-right: 12px;
    }
    
    span {
      font-size: 14px;
      font-weight: 500;
    }
  }
}

.aside-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  
  .version {
    color: rgba(255, 255, 255, 0.4);
    font-size: 12px;
    text-align: center;
  }
}

.header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: background 0.3s;
  
  &:hover {
    background: #f5f7fa;
  }
  
  .user-avatar {
    background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
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
      color: #303133;
    }
    
    .user-role {
      font-size: 12px;
      color: #909399;
    }
  }
  
  .dropdown-icon {
    color: #c0c4cc;
    font-size: 14px;
  }
}

.main {
  background: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
