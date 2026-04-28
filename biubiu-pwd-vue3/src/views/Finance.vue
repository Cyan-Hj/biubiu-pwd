<template>
  <div class="finance-page">
    <!-- 管理员视图 -->
    <div v-if="isAdmin">
      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <div class="stat-card primary">
            <div class="stat-icon"><el-icon><Calendar /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">今日订单总额</div>
              <div class="stat-value">¥{{ formatNumber(adminStats.todayOrderAmount) }}</div>
              <div class="stat-sub">{{ adminStats.todayOrderCount || 0 }} 单</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <div class="stat-card info">
            <div class="stat-icon"><el-icon><DataLine /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">本月订单总额</div>
              <div class="stat-value">¥{{ formatNumber(adminStats.monthOrderAmount) }}</div>
              <div class="stat-sub">{{ adminStats.monthOrderCount || 0 }} 单</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <div class="stat-card success">
            <div class="stat-icon"><el-icon><Money /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">平台总流水</div>
              <div class="stat-value">¥{{ formatNumber(adminStats.totalOrderAmount) }}</div>
              <div class="stat-sub">{{ adminStats.totalOrderCount || 0 }} 单</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <div class="stat-card warning">
            <div class="stat-icon"><el-icon><Coin /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">平台抽成总额</div>
              <div class="stat-value">¥{{ formatNumber(adminStats.totalPlatformIncome) }}</div>
              <div class="stat-sub">占比 {{ calculateRatio(adminStats.totalPlatformIncome, adminStats.totalOrderAmount) }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <div class="stat-card danger">
            <div class="stat-icon"><el-icon><User /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">陪玩师总收入</div>
              <div class="stat-value">¥{{ formatNumber(adminStats.totalPlayerIncome) }}</div>
              <div class="stat-sub">占比 {{ calculateRatio(adminStats.totalPlayerIncome, adminStats.totalOrderAmount) }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <div class="stat-card purple">
            <div class="stat-icon"><el-icon><TrendCharts /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">平均客单价</div>
              <div class="stat-value">¥{{ formatNumber(averageOrderValue) }}</div>
              <div class="stat-sub">每单平均</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 取消订单统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="24" :sm="12" :md="8">
          <div class="stat-card cancelled">
            <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">今日取消订单</div>
              <div class="stat-value">¥{{ formatNumber(adminStats.todayCancelledAmount) }}</div>
              <div class="stat-sub">{{ adminStats.todayCancelledCount || 0 }} 单</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <div class="stat-card cancelled">
            <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">本月取消订单</div>
              <div class="stat-value">¥{{ formatNumber(adminStats.monthCancelledAmount) }}</div>
              <div class="stat-sub">{{ adminStats.monthCancelledCount || 0 }} 单</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8">
          <div class="stat-card cancelled">
            <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">累计取消订单</div>
              <div class="stat-value">¥{{ formatNumber(adminStats.cancelledOrderAmount) }}</div>
              <div class="stat-sub">{{ adminStats.cancelledOrderCount || 0 }} 单</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 图表区域 -->
      <el-row :gutter="20" class="charts-row">
        <el-col :xs="24" :lg="12">
          <el-card class="chart-card">
            <template #header>
              <div class="card-header">
                <span><el-icon><PieChart /></el-icon> 本月收入分布</span>
              </div>
            </template>
            <div ref="pieChartRef" class="chart-container"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-card class="chart-card">
            <template #header>
              <div class="card-header">
                <span><el-icon><Histogram /></el-icon> 近7天收入趋势</span>
              </div>
            </template>
            <div ref="lineChartRef" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 陪玩师收入排行 -->
      <el-card class="ranking-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><Trophy /></el-icon> 陪玩师收入排行 TOP10</span>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :xs="24" :md="12" v-for="(player, index) in adminStats.playerRanking" :key="player.playerId">
            <div class="rank-item" :class="{ 'top3': index < 3 }">
              <div class="rank-number" :class="{ 'top': index < 3 }">{{ index + 1 }}</div>
              <div class="rank-info">
                <div class="rank-name">{{ player.nickname }}</div>
                <div class="rank-bar-container">
                  <div class="rank-bar" :style="{ width: getRankBarWidth(player.totalIncome) + '%' }"></div>
                </div>
              </div>
              <div class="rank-income">¥{{ formatNumber(player.totalIncome) }}</div>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </div>

    <!-- 陪玩师视图 -->
    <div v-if="isPlayer">
      <el-row :gutter="20" class="player-stats-row">
        <el-col :xs="24" :sm="8">
          <div class="stat-card primary">
            <div class="stat-icon"><el-icon><Wallet /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">累计总收入</div>
              <div class="stat-value">¥{{ formatNumber(incomeData.totalIncome) }}</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="stat-card success">
            <div class="stat-icon"><el-icon><CreditCard /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">可提现余额</div>
              <div class="stat-value">¥{{ formatNumber(incomeData.availableBalance) }}</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="stat-card info">
            <div class="stat-icon"><el-icon><Calendar /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">今日收入</div>
              <div class="stat-value">¥{{ formatNumber(incomeData.todayIncome) }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 提现 -->
      <el-card class="withdraw-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><CreditCard /></el-icon> 申请提现</span>
          </div>
        </template>
        <el-form :model="withdrawForm" label-width="100px" class="withdraw-form">
          <el-form-item label="提现金额">
            <el-input-number v-model="withdrawForm.amount" :min="1" :max="incomeData.availableBalance || 1" :precision="2" :step="100" />
            <div class="balance-tip">可提现余额: ¥{{ formatNumber(incomeData.availableBalance) }}</div>
          </el-form-item>
          <el-form-item label="收款方式">
            <el-radio-group v-model="withdrawForm.paymentMethod">
              <el-radio-button label="alipay">支付宝</el-radio-button>
              <el-radio-button label="wechat">微信</el-radio-button>
              <el-radio-button label="bank">银行卡</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="收款账号">
            <el-input v-model="withdrawForm.accountInfo" placeholder="请输入收款账号" />
          </el-form-item>
          <el-form-item label="真实姓名">
            <el-input v-model="withdrawForm.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item v-if="withdrawForm.paymentMethod === 'bank'" label="所在银行">
            <el-input v-model="withdrawForm.bankName" placeholder="请输入所在银行，如：中国工商银行" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleWithdraw" :disabled="!canWithdraw" class="withdraw-btn">
              <el-icon><Check /></el-icon>
              提交提现申请
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 收入明细 -->
    <el-card class="records-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><List /></el-icon> 收入明细</span>
          <div class="header-actions">
            <el-radio-group v-model="recordTypeFilter" size="small">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="income">收入</el-radio-button>
              <el-radio-button label="withdrawal">提现</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      <el-table :data="filteredRecords" v-loading="loading" stripe class="records-table" row-key="id" max-height="600">
        <el-table-column v-if="isAdmin" prop="playerNickname" label="陪玩师" width="120">
          <template #default="{ row }">
            <div class="player-name">{{ row.playerNickname }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="160">
          <template #default="{ row }">
            <div class="time-cell">{{ formatDate(row.createdAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="recordType" label="类型" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.recordType === 'income'" type="success" effect="light" size="small">收入</el-tag>
            <el-tag v-else-if="row.recordType === 'withdrawal'" type="danger" effect="light" size="small">提现</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="130">
          <template #default="{ row }">
            <span class="amount-cell" :class="{ 'income': row.recordType === 'income', 'expense': row.recordType === 'withdrawal' }">
              {{ row.recordType === 'income' ? '+' : row.recordType === 'withdrawal' ? '-' : '' }}¥{{ formatNumber(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="关联订单" width="160">
          <template #default="{ row }">
            <span v-if="row.orderNo" class="order-link">{{ row.orderNo }}</span>
            <span v-else class="no-order">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="200">
          <template #default="{ row }">
            <div class="description-cell">{{ row.description || '-' }}</div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getIncome, getIncomeRecords, withdraw, getFinanceStatistics } from '@/api/finance'
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import { 
  Calendar, DataLine, Money, Coin, User, TrendCharts, 
  PieChart, Histogram, Trophy, Wallet, CreditCard, 
  Check, List, ChatDotRound, CircleClose, Lock
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const isPlayer = computed(() => userStore.isPlayer)
const isAdmin = computed(() => userStore.isAdmin)

const loading = ref(false)
const incomeData = ref({
  totalIncome: 0,
  availableBalance: 0,
  todayIncome: 0
})
const adminStats = ref({
  todayOrderAmount: 0,
  monthOrderAmount: 0,
  totalOrderAmount: 0,
  totalPlatformIncome: 0,
  totalPlayerIncome: 0,
  todayOrderCount: 0,
  monthOrderCount: 0,
  totalOrderCount: 0,
  // 取消订单统计
  cancelledOrderCount: 0,
  cancelledOrderAmount: 0,
  todayCancelledCount: 0,
  todayCancelledAmount: 0,
  monthCancelledCount: 0,
  monthCancelledAmount: 0,
  playerRanking: [],
  incomeDistribution: {},
  recentIncomes: []
})
const records = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const recordTypeFilter = ref('')

const pieChartRef = ref(null)
const lineChartRef = ref(null)
let pieChart = null
let lineChart = null

const withdrawForm = reactive({
  amount: 100,
  paymentMethod: 'alipay',
  accountInfo: '',
  realName: '',
  bankName: ''
})

const canWithdraw = computed(() => {
  return incomeData.value.availableBalance > 0 && 
         withdrawForm.amount > 0 && 
         withdrawForm.amount <= incomeData.value.availableBalance &&
         withdrawForm.accountInfo && 
         withdrawForm.realName
})

const averageOrderValue = computed(() => {
  const total = adminStats.value.totalOrderAmount || 0
  const count = adminStats.value.totalOrderCount || 1
  return total / count
})

const filteredRecords = computed(() => {
  if (!recordTypeFilter.value) return records.value
  return records.value.filter(r => r.recordType === recordTypeFilter.value)
})

const formatNumber = (num) => {
  if (!num) return '0.00'
  return parseFloat(num).toFixed(2)
}

const calculateRatio = (part, total) => {
  if (!part || !total || total === 0) return '0.0'
  return ((part / total) * 100).toFixed(1)
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const getRankBarWidth = (income) => {
  if (!adminStats.value.playerRanking || adminStats.value.playerRanking.length === 0) return 0
  const max = Math.max(...adminStats.value.playerRanking.map(p => p.totalIncome || 0))
  if (max === 0) return 0
  return (income / max) * 100
}

const initPieChart = () => {
  if (!pieChartRef.value || !adminStats.value.incomeDistribution) return

  if (pieChart) {
    pieChart.dispose()
  }

  pieChart = echarts.init(pieChartRef.value)

  const data = Object.entries(adminStats.value.incomeDistribution || {}).map(([name, value]) => ({
    name,
    value: parseFloat(value || 0)
  }))

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: ¥{c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      textStyle: { fontSize: 14 }
    },
    series: [
      {
        name: '本月收入分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n¥{c}'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        data: data.length > 0 ? data : [
          { name: '陪玩师收入', value: 0 },
          { name: '平台抽成', value: 0 },
          { name: '取消订单', value: 0 }
        ],
        color: ['#667eea', '#f0c27f', '#f56c6c']
      }
    ]
  }

  pieChart.setOption(option)
}

const initLineChart = () => {
  if (!lineChartRef.value || !adminStats.value.recentIncomes) return
  
  if (lineChart) {
    lineChart.dispose()
  }
  
  lineChart = echarts.init(lineChartRef.value)
  
  const recentData = adminStats.value.recentIncomes || []
  const dates = recentData.map(d => d.date).reverse()
  const amounts = recentData.map(d => parseFloat(d.amount || 0)).reverse()
  const counts = recentData.map(d => d.orderCount || 0).reverse()
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['收入金额', '订单数'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates.length > 0 ? dates : ['暂无数据'],
      axisLabel: { rotate: 30 }
    },
    yAxis: [
      {
        type: 'value',
        name: '金额(¥)',
        position: 'left',
        axisLabel: { formatter: '¥{value}' }
      },
      {
        type: 'value',
        name: '订单数',
        position: 'right',
        minInterval: 1
      }
    ],
    series: [
      {
        name: '收入金额',
        type: 'line',
        smooth: true,
        data: amounts.length > 0 ? amounts : [0],
        itemStyle: { color: '#667eea' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
            { offset: 1, color: 'rgba(102, 126, 234, 0.05)' }
          ])
        }
      },
      {
        name: '订单数',
        type: 'bar',
        yAxisIndex: 1,
        data: counts.length > 0 ? counts : [0],
        itemStyle: { color: '#f0c27f' }
      }
    ]
  }
  
  lineChart.setOption(option)
}

const loadIncome = async () => {
  if (!isPlayer.value) return
  try {
    const res = await getIncome()
    incomeData.value = res.data || {}
  } catch (error) {
    console.error('加载收入数据失败', error)
  }
}

const loadAdminStats = async () => {
  if (!isAdmin.value) return
  try {
    const res = await getFinanceStatistics()
    adminStats.value = res.data || {}
    
    await nextTick()
    initPieChart()
    initLineChart()
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

const loadRecords = async () => {
  loading.value = true
  try {
    const res = await getIncomeRecords({
      page: 1,
      pageSize: 9999
    })
    records.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const handleWithdraw = async () => {
  if (!withdrawForm.amount || !withdrawForm.accountInfo || !withdrawForm.realName) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (withdrawForm.paymentMethod === 'bank' && !withdrawForm.bankName) {
    ElMessage.warning('请填写所在银行')
    return
  }
  if (withdrawForm.amount > incomeData.value.availableBalance) {
    ElMessage.warning('提现金额不能超过可提现余额')
    return
  }
  try {
    await withdraw(withdrawForm)
    ElMessage.success('提现申请已提交，等待审核')
    loadIncome()
    loadRecords()
    withdrawForm.amount = 100
    withdrawForm.accountInfo = ''
    withdrawForm.realName = ''
    withdrawForm.bankName = ''
  } catch (error) {
    ElMessage.error('提现申请失败')
  }
}

const handleResize = () => {
  pieChart?.resize()
  lineChart?.resize()
}

onMounted(() => {
  loadIncome()
  loadRecords()
  loadAdminStats()
  window.addEventListener('resize', handleResize)
})

watch(() => adminStats.value, () => {
  nextTick(() => {
    initPieChart()
    initLineChart()
  })
}, { deep: true })
</script>

<style scoped lang="scss">
.finance-page {
  padding: 0;
  background: transparent;
  min-height: 100vh;
}

.stats-row {
  margin-bottom: 20px;
}

.player-stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: var(--border-radius-lg);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
  transition: all 0.3s;
  margin-bottom: 16px;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-card-hover);
  }
  
  .stat-icon {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
    flex-shrink: 0;
    
    &.large {
      width: 52px;
      height: 52px;
      font-size: 26px;
    }
  }
  
  .stat-content {
    flex: 1;
  }
  
  .stat-label {
    font-size: 12px;
    color: var(--text-secondary);
    margin-bottom: 4px;
  }
  
  .stat-value {
    font-size: 20px;
    font-weight: 700;
    color: var(--text-primary);
    
    &.large {
      font-size: 24px;
    }
  }
  
  .stat-sub {
    font-size: 12px;
    color: var(--text-tertiary);
    margin-top: 2px;
  }
  
  &.primary .stat-icon {
    background: var(--danger-bg);
    color: var(--danger-dark);
  }
  
  &.info .stat-icon {
    background: #f4f4f5;
    color: var(--text-secondary);
  }
  
  &.success .stat-icon {
    background: var(--warning-bg);
    color: var(--warning-dark);
  }
  
  &.warning .stat-icon {
    background: var(--warning-bg);
    color: var(--warning-dark);
  }
  
  &.danger .stat-icon {
    background: var(--danger-bg);
    color: var(--danger-dark);
  }
  
  &.purple .stat-icon {
    background: var(--primary-bg);
    color: var(--primary-color);
  }
  
  &.cancelled .stat-icon {
    background: var(--danger-bg);
    color: var(--danger-dark);
  }
}

.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-card);
  background: var(--bg-card);
  
  :deep(.el-card__header) {
    padding: 14px 20px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(90deg, var(--primary-bg) 0%, transparent 100%);
    
    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
      color: var(--text-primary);
    }
  }
}

.chart-container {
  height: 320px;
}

.ranking-card {
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-card);
  background: var(--bg-card);
  margin-bottom: 20px;
  
  :deep(.el-card__header) {
    padding: 14px 20px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(90deg, var(--primary-bg) 0%, transparent 100%);
    
    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
      color: var(--text-primary);
    }
  }
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px 15px;
  margin-bottom: 10px;
  background: #f8f9fa;
  border-radius: var(--border-radius);
  transition: all 0.2s;
  
  &:hover {
    background: var(--primary-bg);
  }
  
  &.top3 {
    background: linear-gradient(135deg, #fff9e6 0%, #fff5d6 100%);
  }
  
  .rank-number {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: #dcdfe6;
    color: var(--text-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: 14px;
    flex-shrink: 0;
    
    &.top {
      background: #ffd700;
      color: #8b6914;
    }
  }
  
  .rank-info {
    flex: 1;
    min-width: 0;
  }
  
  .rank-name {
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 6px;
  }
  
  .rank-bar-container {
    height: 6px;
    background: #e4e7ed;
    border-radius: 3px;
    overflow: hidden;
  }
  
  .rank-bar {
    height: 100%;
    background: linear-gradient(90deg, var(--primary-color) 0%, var(--primary-light) 100%);
    border-radius: 3px;
    transition: width 0.5s ease;
  }
  
  .rank-income {
    font-weight: 700;
    color: var(--warning-dark);
    font-size: 15px;
    white-space: nowrap;
  }
}

.withdraw-card {
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-card);
  background: var(--bg-card);
  margin-bottom: 20px;
  
  :deep(.el-card__header) {
    padding: 14px 20px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(90deg, var(--primary-bg) 0%, transparent 100%);
    
    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
      color: var(--text-primary);
    }
  }
}

.withdraw-form {
  padding: 10px 0;
  
  .balance-tip {
    font-size: 12px;
    color: var(--danger-dark);
    margin-top: 5px;
  }
  
  .withdraw-btn {
    padding: 12px 30px;
    font-size: 15px;
  }
}

.records-card {
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-card);
  background: var(--bg-card);
  
  :deep(.el-card__header) {
    padding: 14px 20px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(90deg, var(--primary-bg) 0%, transparent 100%);
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      span {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 600;
        color: var(--text-primary);
      }
    }
  }
}

.records-table {
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
  
  .player-name {
    font-weight: 500;
    color: var(--text-primary);
  }
  
  .time-cell {
    color: var(--text-secondary);
    font-size: 13px;
  }
  
  .amount-cell {
    font-family: var(--font-mono);
    font-weight: 700;
    font-size: 15px;
    
    &.income {
      color: var(--success-color);
    }
    
    &.expense {
      color: var(--danger-dark);
    }
  }
  
  .order-link {
    font-family: var(--font-mono);
    color: var(--primary-color);
    font-weight: 500;
  }
  
  .no-order {
    color: var(--text-tertiary);
  }
  
  .description-cell {
    color: var(--text-secondary);
    font-size: 13px;
  }
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.expand-content {
  padding: 16px 20px;
  background: #f8f9fa;
  border-radius: var(--border-radius);
}

.not-available-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  background: var(--bg-card);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-card);
  margin-bottom: 20px;
  
  .not-available-icon {
    font-size: 80px;
    color: var(--text-tertiary);
    margin-bottom: 20px;
  }
  
  .not-available-text {
    color: var(--text-secondary);
    font-size: 14px;
    margin-top: 10px;
  }
}

@media (max-width: 768px) {
  .stats-row,
  .player-stats-row {
    gap: 8px;
    margin-bottom: 12px;
  }

  .stat-card {
    padding: 12px;
    gap: 10px;

    .stat-icon {
      width: 38px;
      height: 38px;
      border-radius: 50%;
      font-size: 18px;
    }

    .stat-value {
      font-size: 18px;
    }

    .stat-label {
      font-size: 11px;
    }
  }

  .charts-row {
    margin-bottom: 12px;
  }

  .chart-card {
    margin-bottom: 12px;

    :deep(.el-card__header) {
      padding: 12px 14px;
    }
  }

  .chart-container {
    height: 240px;
  }

  .ranking-card {
    margin-bottom: 12px;

    :deep(.el-card__header) {
      padding: 12px 14px;
    }
  }

  .rank-item {
    padding: 10px 12px;
    gap: 10px;

    .rank-number {
      width: 28px;
      height: 28px;
      font-size: 12px;
    }

    .rank-name {
      font-size: 13px;
      margin-bottom: 4px;
    }

    .rank-income {
      font-size: 14px;
    }
  }

  .withdraw-card {
    margin-bottom: 12px;

    :deep(.el-card__header) {
      padding: 12px 14px;
    }
  }

  .withdraw-form {
    :deep(.el-form-item__label) {
      float: none;
      display: block;
      text-align: left;
      padding-bottom: 4px;
      width: auto !important;
    }

    :deep(.el-form-item__content) {
      margin-left: 0 !important;
    }
  }

  .records-card {
    :deep(.el-card__header) {
      padding: 12px 14px;
    }
  }

  .records-table {
    min-width: 700px;
  }

  .pagination {
    margin-top: 12px;

    :deep(.el-pagination) {
      flex-wrap: wrap;
      justify-content: center;
      gap: 8px;

      .el-pagination__sizes {
        display: none;
      }
    }
  }
}

@media (max-width: 480px) {
  .stat-card {
    padding: 10px;
    gap: 8px;

    .stat-icon {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      font-size: 16px;
    }

    .stat-value {
      font-size: 16px;
    }

    .stat-label {
      font-size: 11px;
    }
  }

  .chart-container {
    height: 200px;
  }

  .rank-item {
    padding: 8px 10px;
    gap: 8px;

    .rank-number {
      width: 24px;
      height: 24px;
      font-size: 11px;
    }

    .rank-name {
      font-size: 12px;
    }

    .rank-income {
      font-size: 13px;
    }
  }

  .withdraw-btn {
    width: 100%;
    padding: 10px;
  }
}
</style>
