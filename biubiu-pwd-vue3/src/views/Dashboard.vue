<template>
  <div class="dashboard-page">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-banner-text">
        <h2>欢迎回来，{{ userStore.userInfo?.nickname || '用户' }}</h2>
        <p>{{ getGreeting() }}，祝您工作愉快！</p>
      </div>
      <div class="welcome-banner-time">
        <div class="time">{{ currentTime }}</div>
        <div class="date">{{ currentDate }}</div>
      </div>
    </div>

    <!-- 公告栏 -->
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

    <!-- 数据卡片区 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card-modern">
          <div class="stat-icon-modern primary">
            <el-icon><Money /></el-icon>
          </div>
          <div class="stat-info-modern">
            <div class="stat-value-modern">¥{{ formatNumber(todayStats.amount) }}</div>
            <div class="stat-label-modern">今日订单金额</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card-modern">
          <div class="stat-icon-modern success">
            <el-icon><DocumentChecked /></el-icon>
          </div>
          <div class="stat-info-modern">
            <div class="stat-value-modern">{{ todayStats.count || 0 }} 单</div>
            <div class="stat-label-modern">今日订单数</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card-modern">
          <div class="stat-icon-modern warning">
            <el-icon><Timer /></el-icon>
          </div>
          <div class="stat-info-modern">
            <div class="stat-value-modern">{{ pendingCount }}</div>
            <div class="stat-label-modern">待处理订单</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="stat-card-modern">
          <div class="stat-icon-modern danger">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-info-modern">
            <div class="stat-value-modern">{{ inServiceCount }}</div>
            <div class="stat-label-modern">服务中订单</div>
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

      <!-- 桌面端表格 -->
      <div class="hidden-xs-only">
        <el-table :data="recentOrders" v-loading="loading" stripe class="data-table">
          <el-table-column prop="orderNo" label="订单号" width="150">
            <template #default="{ row }">
              <span class="order-no-text">{{ row.orderNo }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="bossInfo" label="老板信息" min-width="140" show-overflow-tooltip />
          <el-table-column prop="serviceContent" label="服务内容" min-width="180" show-overflow-tooltip />
          <el-table-column label="金额" width="110">
            <template #default="{ row }">
              <span class="amount-cell">¥{{ formatNumber(row.totalAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <span :class="['status-tag', getStatusClass(row.status)]">
                <span class="status-dot"></span>
                {{ getStatusText(row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="160">
            <template #default="{ row }">
              <span class="time-text">{{ formatDate(row.createdAt) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 移动端卡片列表 -->
      <div class="hidden-sm-and-up order-card-list">
        <div v-for="order in recentOrders" :key="order.id" class="order-card-item">
          <div class="order-card-header">
            <span class="order-no-text">{{ order.orderNo }}</span>
            <span :class="['status-tag', getStatusClass(order.status)]">
              <span class="status-dot"></span>
              {{ getStatusText(order.status) }}
            </span>
          </div>
          <div class="order-card-body">
            <div class="order-card-row">
              <span class="order-card-label">老板</span>
              <span class="order-card-value">{{ order.bossInfo }}</span>
            </div>
            <div class="order-card-row">
              <span class="order-card-label">内容</span>
              <span class="order-card-value">{{ order.serviceContent }}</span>
            </div>
            <div class="order-card-row">
              <span class="order-card-label">金额</span>
              <span class="order-card-value amount-cell">¥{{ formatNumber(order.totalAmount) }}</span>
            </div>
            <div class="order-card-row">
              <span class="order-card-label">时间</span>
              <span class="order-card-value time-text">{{ formatDate(order.createdAt) }}</span>
            </div>
          </div>
        </div>
        <el-empty v-if="recentOrders.length === 0" description="暂无订单" />
      </div>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getOrders } from '@/api/orders'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { 
  Sunny, Document, Money, DocumentChecked, ArrowRight, Bell, Plus, Delete, Timer, User
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)
const isCustomerService = computed(() => userStore.isCustomerService)
const isPlayer = computed(() => userStore.isPlayer)

const loading = ref(false)
const todayStats = ref({ amount: 0, count: 0 })
const recentOrders = ref([])
const pendingCount = ref(0)
const inServiceCount = ref(0)

// 公告相关
const notices = ref([])
const showNoticeDialog = ref(false)
const noticeForm = ref({ content: '' })
const noticeRules = {
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}
const noticeFormRef = ref(null)

// 时间显示
const currentTime = ref('')
const currentDate = ref('')
let timeTimer = null

const updateTime = () => {
  currentTime.value = dayjs().format('HH:mm:ss')
  currentDate.value = dayjs().format('YYYY年MM月DD日 dddd')
}

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
  return parseFloat(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const getStatusClass = (status) => {
  const classes = {
    0: 'status-info',
    1: 'status-warning',
    2: 'status-warning',
    3: 'status-primary',
    4: 'status-success',
    5: 'status-danger'
  }
  return classes[status] || 'status-info'
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
      await loadPlayerTodayStats()
    }

    // 加载待处理和服务中数量
    await loadOrderCounts()
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

// 加载订单数量统计
const loadOrderCounts = async () => {
  try {
    const res = await getOrders({ page: 1, pageSize: 100, status: 0 })
    pendingCount.value = res.data?.total || 0
    const res2 = await getOrders({ page: 1, pageSize: 100, status: 3 })
    inServiceCount.value = res2.data?.total || 0
  } catch (error) {
    console.error('加载订单数量失败', error)
  }
}

onMounted(() => {
  loadData()
  loadNotices()
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeTimer) clearInterval(timeTimer)
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
  padding: 0;
  background: transparent;
  min-height: 100vh;
}

/* 欢迎横幅 */
.welcome-banner {
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
  border-radius: var(--border-radius-lg);
  padding: 24px 28px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  box-shadow: 0 4px 16px rgba(108, 92, 231, 0.25);
}

.welcome-banner-text h2 {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 6px 0;
}

.welcome-banner-text p {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
}

.welcome-banner-time {
  text-align: right;
}

.welcome-banner-time .time {
  font-size: 28px;
  font-weight: 700;
  font-family: var(--font-mono);
  line-height: 1.2;
}

.welcome-banner-time .date {
  font-size: 13px;
  opacity: 0.85;
  margin-top: 4px;
}

/* 公告卡片 */
.notice-card {
  margin-bottom: 20px;
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-card);
  background: var(--bg-card);

  :deep(.el-card__header) {
    padding: 14px 20px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(90deg, var(--primary-bg) 0%, transparent 100%);
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
      color: var(--text-primary);

      .el-icon {
        color: var(--primary-color);
        font-size: 18px;
      }
    }
  }

  .notice-list {
    padding: 4px 0;

    .notice-item {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 10px 14px;
      margin-bottom: 8px;
      background: #f8f9fa;
      border-radius: var(--border-radius);
      border-left: 3px solid var(--primary-color);
      transition: all 0.3s;
      gap: 12px;

      &:hover {
        background: #f0f2f5;
        transform: translateX(2px);
      }

      &:last-child {
        margin-bottom: 0;
      }

      .notice-content {
        display: flex;
        align-items: flex-start;
        gap: 8px;
        flex: 1;
        min-width: 0;

        .notice-tag {
          flex-shrink: 0;
          margin-top: 2px;
        }

        .notice-text {
          color: var(--text-secondary);
          font-size: 14px;
          line-height: 1.6;
          word-break: break-all;
          white-space: pre-wrap;
        }
      }

      .notice-meta {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-shrink: 0;
        padding-top: 2px;

        .notice-time {
          color: var(--text-tertiary);
          font-size: 12px;
          white-space: nowrap;
        }
      }
    }
  }
}

/* 数据卡片 */
.stats-row {
  margin-bottom: 20px;
}

.stat-card-modern {
  background: var(--bg-card);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-card);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s;
  margin-bottom: 16px;
}

.stat-card-modern:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card-hover);
}

.stat-icon-modern {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.stat-icon-modern.primary {
  background: var(--primary-bg);
  color: var(--primary-color);
}

.stat-icon-modern.success {
  background: var(--success-bg);
  color: var(--success-color);
}

.stat-icon-modern.warning {
  background: var(--warning-bg);
  color: var(--warning-dark);
}

.stat-icon-modern.danger {
  background: var(--danger-bg);
  color: var(--danger-dark);
}

.stat-value-modern {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label-modern {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

/* 最近订单卡片 */
.recent-orders-card {
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-card);
  background: var(--bg-card);

  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(90deg, var(--primary-bg) 0%, transparent 100%);
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
      color: var(--text-primary);

      .el-icon {
        color: var(--primary-color);
        font-size: 20px;
      }
    }
  }
}

/* 表格样式 */
.data-table {
  :deep(th.el-table__cell) {
    background: #f8f9fa !important;
    font-weight: 600 !important;
    color: var(--text-primary) !important;
    font-size: 13px;
    padding: 12px 8px;
  }

  :deep(td.el-table__cell) {
    padding: 10px 8px;
    border-bottom: 1px solid #f0f0f0;
  }

  :deep(tr:hover td.el-table__cell) {
    background: rgba(108, 92, 231, 0.04) !important;
  }

  .order-no-text {
    font-family: var(--font-mono);
    font-weight: 600;
    color: var(--primary-color);
    font-size: 13px;
  }

  .amount-cell {
    font-family: var(--font-mono);
    font-weight: 600;
    color: var(--danger-dark);
    text-align: right;
  }

  .time-text {
    color: var(--text-tertiary);
    font-size: 13px;
  }
}

/* 状态标签 */
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}

.status-info {
  background: var(--info-bg);
  color: var(--info-dark);
}
.status-info .status-dot { background: var(--info-dark); }

.status-warning {
  background: var(--warning-bg);
  color: var(--warning-dark);
}
.status-warning .status-dot { background: var(--warning-dark); }

.status-primary {
  background: rgba(108, 92, 231, 0.08);
  color: var(--primary-color);
}
.status-primary .status-dot { background: var(--primary-color); }

.status-success {
  background: var(--success-bg);
  color: var(--success-color);
}
.status-success .status-dot { background: var(--success-color); }

.status-danger {
  background: var(--danger-bg);
  color: var(--danger-dark);
}
.status-danger .status-dot { background: var(--danger-dark); }

/* 移动端订单卡片列表 */
.order-card-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.order-card-item {
  background: var(--bg-card);
  border-radius: var(--border-radius);
  padding: 14px 16px;
  border: 1px solid #f0f0f0;
  transition: all 0.3s;
}

.order-card-item:hover {
  box-shadow: var(--shadow-card);
  border-color: var(--primary-light);
}

.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.order-card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.order-card-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-card-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.order-card-value {
  font-size: 13px;
  color: var(--text-secondary);
  text-align: right;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hidden-xs-only {
  display: block;
}

.hidden-sm-and-up {
  display: none;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .welcome-banner {
    padding: 16px 20px;
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    margin-bottom: 16px;
  }

  .welcome-banner-text h2 {
    font-size: 18px;
  }

  .welcome-banner-time {
    text-align: left;
  }

  .welcome-banner-time .time {
    font-size: 22px;
  }

  .stats-row {
    margin-bottom: 0;
  }

  .stat-card-modern {
    padding: 14px;
    gap: 12px;
    margin-bottom: 12px;
  }

  .stat-icon-modern {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }

  .stat-value-modern {
    font-size: 18px;
  }

  .stat-label-modern {
    font-size: 12px;
  }

  .notice-card {
    margin-bottom: 16px;

    :deep(.el-card__header) {
      padding: 12px 14px;
    }

    .notice-header .notice-title {
      font-size: 14px;

      .el-icon {
        font-size: 16px;
      }
    }

    .notice-list .notice-item {
      padding: 8px 12px;
      flex-direction: column;
      gap: 6px;

      .notice-content .notice-text {
        font-size: 13px;
      }

      .notice-meta {
        width: 100%;
        justify-content: space-between;
        padding-top: 0;
      }
    }
  }

  .recent-orders-card {
    :deep(.el-card__header) {
      padding: 12px 14px;
    }

    .card-header .header-title {
      font-size: 15px;

      .el-icon {
        font-size: 18px;
      }
    }
  }

  .hidden-xs-only {
    display: none;
  }

  .hidden-sm-and-up {
    display: block;
  }
}

@media (max-width: 480px) {
  .welcome-banner {
    padding: 14px 16px;
  }

  .welcome-banner-text h2 {
    font-size: 16px;
  }

  .welcome-banner-text p {
    font-size: 12px;
  }

  .stat-card-modern {
    padding: 12px;
    gap: 10px;
  }

  .stat-icon-modern {
    width: 36px;
    height: 36px;
    font-size: 16px;
  }

  .stat-value-modern {
    font-size: 16px;
  }

  .stat-label-modern {
    font-size: 11px;
  }
}
</style>
