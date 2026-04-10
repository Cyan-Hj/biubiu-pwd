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

    <!-- 公告栏 - 仅管理员可见编辑按钮 -->
    <el-card class="notice-card" v-if="isAdmin || notices.length > 0">
      <template #header>
        <div class="notice-header">
          <div class="notice-title">
            <el-icon><Bell /></el-icon>
            <span>系统公告</span>
          </div>
          <el-button v-if="isAdmin" type="primary" link @click="showNoticeDialog = true">
            <el-icon><Plus /></el-icon> 发布公告
          </el-button>
        </div>
      </template>
      <div class="notice-list">
        <div v-for="(notice, index) in notices" :key="index" class="notice-item">
          <div class="notice-content">
            <el-tag type="warning" size="small" effect="light" class="notice-tag">公告</el-tag>
            <span class="notice-text">{{ notice.content }}</span>
          </div>
          <div class="notice-meta">
            <span class="notice-time">{{ formatDate(notice.createdAt) }}</span>
            <el-button v-if="isAdmin" type="danger" link size="small" @click="deleteNotice(index)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <el-empty v-if="notices.length === 0" description="暂无公告" :image-size="60" />
      </div>
    </el-card>

    <!-- 统计卡片 - 所有用户都显示今日订单统计 -->
    <el-row :gutter="20" class="stats-row">
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

    <!-- 发布公告对话框 -->
    <el-dialog
      v-model="showNoticeDialog"
      title="发布公告"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="noticeForm" :rules="noticeRules" ref="noticeFormRef" label-width="0">
        <el-form-item prop="content">
          <el-input
            v-model="noticeForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入公告内容..."
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNoticeDialog = false">取消</el-button>
        <el-button type="primary" @click="publishNotice">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getOrders } from '@/api/orders'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { 
  Sunny, Document, Money, DocumentChecked, ArrowRight, Bell, Plus, Delete
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)
const isCustomerService = computed(() => userStore.isCustomerService)
const isPlayer = computed(() => userStore.isPlayer)

const loading = ref(false)
const todayStats = ref({ amount: 0, count: 0 })
const recentOrders = ref([])

// 公告相关
const notices = ref([])
const showNoticeDialog = ref(false)
const noticeForm = ref({ content: '' })
const noticeRules = {
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}
const noticeFormRef = ref(null)

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
    2: 'warning',
    3: 'primary',
    4: 'success',
    5: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    0: '待分配',
    1: '待接单',
    2: '待接单2',
    3: '服务中',
    4: '已完成',
    5: '已取消'
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

    // 加载今日统计数据
    if (isAdmin.value || isCustomerService.value) {
      await loadTodayStats()
    } else if (isPlayer.value) {
      // 陪玩师加载个人今日订单统计
      await loadPlayerTodayStats()
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

// 加载陪玩师个人今日订单统计
const loadPlayerTodayStats = async () => {
  try {
    const today = dayjs().format('YYYY-MM-DD')
    const tomorrow = dayjs().add(1, 'day').format('YYYY-MM-DD')
    const ordersRes = await getOrders({
      page: 1,
      pageSize: 100,
      startDate: today,
      endDate: tomorrow
    })
    const orders = ordersRes.data?.list || []

    // 只统计今日已完成订单
    const completedOrders = orders.filter(order => order.status === 4)

    todayStats.value = {
      amount: completedOrders.reduce((sum, order) => sum + (order.playerIncome || 0), 0),
      count: completedOrders.length
    }
  } catch (error) {
    console.error('加载陪玩师统计数据失败', error)
  }
}

onMounted(() => {
  loadData()
  loadNotices()
})

// 加载公告
const loadNotices = () => {
  const saved = localStorage.getItem('dashboard_notices')
  if (saved) {
    notices.value = JSON.parse(saved)
  }
}

// 保存公告
const saveNotices = () => {
  localStorage.setItem('dashboard_notices', JSON.stringify(notices.value))
}

// 发布公告
const publishNotice = async () => {
  const valid = await noticeFormRef.value?.validate().catch(() => false)
  if (!valid) return

  notices.value.unshift({
    content: noticeForm.value.content,
    createdAt: new Date().toISOString()
  })
  saveNotices()
  noticeForm.value.content = ''
  showNoticeDialog.value = false
  ElMessage.success('公告发布成功')
}

// 删除公告
const deleteNotice = (index) => {
  notices.value.splice(index, 1)
  saveNotices()
  ElMessage.success('公告已删除')
}
</script>

<style scoped lang="scss">
.dashboard-page {
  padding: 20px;
  background: #f8f8fc;
  min-height: 100vh;
}

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
      color: #ff6b6b;
      font-size: 28px;
    }
  }
  
  .welcome-subtitle {
    font-size: 14px;
    color: #909399;
    margin-left: 40px;
  }
}

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
    background: linear-gradient(135deg, #fff5f5 0%, #fff 100%);
    border-left: 4px solid #ff6b6b;
  }
}

.notice-card {
  margin-bottom: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  background: linear-gradient(135deg, #fff 0%, #fafbfc 100%);
  
  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(90deg, rgba(102, 126, 234, 0.05) 0%, transparent 100%);
  }
  
  .notice-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .notice-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      font-weight: 600;
      color: #2d2d4a;
      
      .el-icon {
        color: #667eea;
        font-size: 18px;
      }
    }
  }
  
  .notice-list {
    padding: 8px 0;
    
    .notice-item {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 12px 16px;
      margin-bottom: 8px;
      background: #f8f9fc;
      border-radius: 8px;
      border-left: 3px solid #667eea;
      transition: all 0.3s;
      gap: 16px;
      
      &:hover {
        background: #f0f2f8;
        transform: translateX(4px);
      }
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .notice-content {
        display: flex;
        align-items: flex-start;
        gap: 10px;
        flex: 1;
        min-width: 0;
        
        .notice-tag {
          flex-shrink: 0;
          margin-top: 2px;
        }
        
        .notice-text {
          color: #4a4a6a;
          font-size: 14px;
          line-height: 1.6;
          word-break: break-all;
          white-space: pre-wrap;
        }
      }
      
      .notice-meta {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-shrink: 0;
        padding-top: 2px;
        
        .notice-time {
          color: #a0a0c0;
          font-size: 12px;
          white-space: nowrap;
        }
      }
    }
  }
}

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
        color: #ff6b6b;
        font-size: 20px;
      }
    }
  }
}

.recent-table {
  :deep(th) {
    background: #f8f8fc;
    font-weight: 600;
    color: #606266;
  }

  .order-no {
    font-family: monospace;
    font-weight: 600;
    color: #ff6b6b;
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

@media (max-width: 768px) {
  .dashboard-page {
    padding: 12px;
  }

  .welcome-section {
    margin-bottom: 16px;

    .welcome-title {
      font-size: 20px;
      gap: 8px;

      .welcome-icon {
        font-size: 24px;
      }
    }

    .welcome-subtitle {
      font-size: 13px;
      margin-left: 32px;
    }
  }

  .stats-row {
    margin-bottom: 16px;
  }

  .stat-card {
    padding: 20px;

    .stat-bg-icon {
      font-size: 60px;
      right: 16px;
    }

    .stat-label {
      font-size: 13px;
    }

    .stat-value {
      font-size: 26px;
    }

    .stat-trend {
      .trend-item {
        font-size: 12px;
      }
    }
  }

  .notice-card {
    margin-bottom: 16px;
    border-radius: 10px;

    :deep(.el-card__header) {
      padding: 14px 16px;
    }

    .notice-header {
      .notice-title {
        font-size: 14px;

        .el-icon {
          font-size: 16px;
        }
      }
    }

    .notice-list {
      .notice-item {
        padding: 10px 12px;
        flex-direction: column;
        gap: 8px;

        .notice-content {
          .notice-text {
            font-size: 13px;
          }
        }

        .notice-meta {
          padding-top: 0;
          width: 100%;
          justify-content: space-between;
        }
      }
    }
  }

  .recent-orders-card {
    border-radius: 10px;

    :deep(.el-card__header) {
      padding: 14px 16px;
    }

    .card-header {
      .header-title {
        font-size: 15px;

        .el-icon {
          font-size: 18px;
        }
      }
    }
  }

  .recent-table {
    font-size: 12px;

    :deep(th) {
      padding: 10px 8px;
    }

    :deep(td) {
      padding: 8px;
    }

    .order-no {
      font-size: 12px;
    }

    .order-amount {
      font-size: 13px;
    }

    .time-text {
      font-size: 12px;
    }
  }
}

@media (max-width: 480px) {
  .dashboard-page {
    padding: 8px;
  }

  .welcome-section {
    margin-bottom: 12px;

    .welcome-title {
      font-size: 18px;
      gap: 6px;

      .welcome-icon {
        font-size: 20px;
      }
    }

    .welcome-subtitle {
      font-size: 12px;
      margin-left: 26px;
    }
  }

  .stats-row {
    margin-bottom: 12px;
  }

  .stat-card {
    padding: 16px;
    border-radius: 10px;

    .stat-bg-icon {
      font-size: 48px;
      right: 12px;
    }

    .stat-label {
      font-size: 12px;
      margin-bottom: 6px;
    }

    .stat-value {
      font-size: 22px;
      margin-bottom: 8px;
    }

    .stat-trend {
      .trend-item {
        font-size: 11px;
      }
    }
  }

  .notice-card {
    margin-bottom: 12px;
    border-radius: 8px;

    :deep(.el-card__header) {
      padding: 12px 14px;
    }

    .notice-header {
      .notice-title {
        font-size: 13px;

        .el-icon {
          font-size: 14px;
        }
      }
    }

    .notice-list {
      .notice-item {
        padding: 8px 10px;
        border-radius: 6px;

        .notice-content {
          gap: 6px;

          .notice-tag {
            font-size: 10px;
            padding: 0 4px;
            height: 18px;
          }

          .notice-text {
            font-size: 12px;
            line-height: 1.5;
          }
        }

        .notice-meta {
          .notice-time {
            font-size: 11px;
          }
        }
      }
    }
  }

  .recent-orders-card {
    border-radius: 8px;

    :deep(.el-card__header) {
      padding: 12px 14px;
    }

    .card-header {
      .header-title {
        font-size: 14px;

        .el-icon {
          font-size: 16px;
        }
      }
    }
  }

  .recent-table {
    font-size: 11px;

    :deep(th) {
      padding: 8px 6px;
    }

    :deep(td) {
      padding: 6px;
    }

    .order-no {
      font-size: 11px;
    }

    .order-amount {
      font-size: 12px;
    }

    .time-text {
      font-size: 11px;
    }
  }
}
</style>
