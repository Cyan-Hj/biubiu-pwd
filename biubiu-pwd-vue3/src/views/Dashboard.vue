<template>
  <div class="dashboard-page">
    <!-- 欢迎语 -->
    <div class="welcome-section">
      <h1 class="welcome-title">
        <el-icon class="welcome-icon"><Sunny /></el-icon>
        欢迎回来，{{ userStore.userInfo?.nickname || '用户' }}
      </h1>
      <p class="welcome-subtitle">{{ getGreeting() }}，祝您工作愉快！</p>
    </div>

    <!-- 统计卡片 - 仅管理员/客服显示今日订单统计 -->
    <el-row :gutter="20" class="stats-row" v-if="isAdmin || isCustomerService">
      <el-col :xs="24" :sm="12">
        <div class="stat-card primary">
          <div class="stat-bg-icon"><el-icon><Money /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">今日订单金额</div>
            <div class="stat-value">¥{{ formatNumber(todayStats.amount) }}</div>
            <div class="stat-trend">
              <span class="trend-item">
                <el-icon><DocumentChecked /></el-icon> {{ todayStats.count || 0 }} 单
              </span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 最近订单 -->
    <el-card class="recent-orders-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon><Document /></el-icon>
            <span>最近订单</span>
          </div>
          <el-button type="primary" link @click="$router.push('/orders')">
            查看全部 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>
      
      <el-table :data="recentOrders" v-loading="loading" stripe class="recent-table">
        <el-table-column prop="orderNo" label="订单号" width="150">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bossInfo" label="老板信息" min-width="140" show-overflow-tooltip />
        <el-table-column prop="serviceContent" label="服务内容" min-width="180" show-overflow-tooltip />
        <el-table-column prop="totalAmount" label="金额" width="110">
          <template #default="{ row }">
            <span class="order-amount">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getOrders } from '@/api/orders'
import dayjs from 'dayjs'
import { 
  Sunny, Document, Money, DocumentChecked, ArrowRight 
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)
const isCustomerService = computed(() => userStore.isCustomerService)

const loading = ref(false)
const todayStats = ref({ amount: 0, count: 0 })
const recentOrders = ref([])

const getGreeting = () => {
  const hour = dayjs().hour()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
}

const formatNumber = (num) => {
  if (!num) return '0.00'
  return parseFloat(num).toFixed(2)
}

const getStatusType = (status) => {
  const types = {
    0: 'info',
    1: 'warning',
    2: 'primary',
    3: 'success',
    4: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    0: '待分配',
    1: '待接单',
    2: '服务中',
    3: '已完成',
    4: '已取消'
  }
  return texts[status] || '未知'
}

const formatDate = (date) => {
  return date ? dayjs(date).format('MM-DD HH:mm') : '-'
}

const loadData = async () => {
  loading.value = true
  try {
    // 加载最近订单
    const ordersRes = await getOrders({ page: 1, pageSize: 5 })
    recentOrders.value = ordersRes.data?.list || []

    // 加载今日统计数据（管理员/客服）
    if (isAdmin.value || isCustomerService.value) {
      await loadTodayStats()
    }
  } finally {
    loading.value = false
  }
}

// 加载今日统计数据
const loadTodayStats = async () => {
  try {
    const { getFinanceStatistics } = await import('@/api/finance')
    const statsRes = await getFinanceStatistics()
    const stats = statsRes.data || {}

    todayStats.value = {
      amount: stats.todayOrderAmount || 0,
      count: stats.todayOrderCount || 0
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.dashboard-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

// 欢迎区域
.welcome-section {
  margin-bottom: 24px;
  
  .welcome-title {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 24px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 8px;
    
    .welcome-icon {
      color: #e6a23c;
      font-size: 28px;
    }
  }
  
  .welcome-subtitle {
    font-size: 14px;
    color: #909399;
    margin-left: 40px;
  }
}

// 统计卡片
.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  position: relative;
  padding: 24px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }
  
  .stat-bg-icon {
    position: absolute;
    right: 20px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 80px;
    opacity: 0.08;
    transition: opacity 0.3s;
  }
  
  &:hover .stat-bg-icon {
    opacity: 0.12;
  }
  
  .stat-content {
    position: relative;
    z-index: 1;
  }
  
  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-bottom: 8px;
  }
  
  .stat-value {
    font-size: 32px;
    font-weight: 700;
    color: #303133;
    margin-bottom: 12px;
  }
  
  .stat-trend {
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
    
    .trend-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 13px;
      color: #606266;
      
      .el-icon {
        font-size: 14px;
      }
    }
  }
  
  &.primary {
    background: linear-gradient(135deg, #ecf5ff 0%, #fff 100%);
    border-left: 4px solid #409eff;
  }
}

// 最近订单卡片
.recent-orders-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  
  :deep(.el-card__header) {
    padding: 18px 24px;
    border-bottom: 1px solid #ebeef5;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-title {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      
      .el-icon {
        color: #409eff;
        font-size: 20px;
      }
    }
  }
}

.recent-table {
  :deep(th) {
    background: #f5f7fa;
    font-weight: 600;
    color: #606266;
  }
  
  .order-no {
    font-family: monospace;
    font-weight: 600;
    color: #409eff;
  }
  
  .order-amount {
    font-weight: 700;
    color: #f56c6c;
  }
  
  .time-text {
    color: #909399;
    font-size: 13px;
  }
}
</style>
