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
    <el-row :gutter="20" v-if="isPlayer" class="player-stats-row">
      <el-col :xs="24" :sm="8">
        <div class="stat-card primary">
          <div class="stat-icon large"><el-icon><Wallet /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">累计收入</div>
            <div class="stat-value large">¥{{ formatNumber(incomeData.totalIncome) }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card success">
          <div class="stat-icon large"><el-icon><Money /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">可提现余额</div>
            <div class="stat-value large">¥{{ formatNumber(incomeData.availableBalance) }}</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card info">
          <div class="stat-icon large"><el-icon><TrendCharts /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">今日收入</div>
            <div class="stat-value large">¥{{ formatNumber(incomeData.todayIncome) }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 陪玩师提现申请 -->
    <el-card class="withdraw-card" v-if="isPlayer">
      <template #header>
        <div class="card-header">
          <span><el-icon><CreditCard /></el-icon> 申请提现</span>
        </div>
      </template>
      <el-form :model="withdrawForm" label-width="100px" class="withdraw-form">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="提现金额">
              <el-input-number 
                v-model="withdrawForm.amount" 
                :min="1" 
                :max="incomeData.availableBalance || 0" 
                :precision="2"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="提现方式">
              <el-select v-model="withdrawForm.paymentMethod" placeholder="选择提现方式" style="width: 100%">
                <el-option label="支付宝" value="alipay">
                  <el-icon><Wallet /></el-icon> 支付宝
                </el-option>
                <el-option label="微信" value="wechat">
                  <el-icon><ChatDotRound /></el-icon> 微信
                </el-option>
                <el-option label="银行卡" value="bank">
                  <el-icon><CreditCard /></el-icon> 银行卡
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="收款账号">
              <el-input v-model="withdrawForm.accountInfo" placeholder="请输入收款账号" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="真实姓名">
              <el-input v-model="withdrawForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button 
            type="primary" 
            size="large"
            @click="handleWithdraw" 
            :disabled="!canWithdraw"
            class="withdraw-btn"
          >
            <el-icon><Check /></el-icon> 申请提现
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 收入明细 -->
    <el-card class="records-card">
      <template #header>
        <div class="card-header">
          <span><el-icon><List /></el-icon> 收入明细</span>
          <div class="header-actions">
            <el-radio-group v-model="recordTypeFilter" size="small" @change="loadRecords">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="income">收入</el-radio-button>
              <el-radio-button label="withdrawal">提现</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      <el-table :data="filteredRecords" v-loading="loading" stripe class="records-table">
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
        <el-table-column prop="type" label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.type === 'income' ? 'success' : 'danger'" effect="light" size="small">
              {{ row.type === 'income' ? '收入' : '提现' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="130">
          <template #default="{ row }">
            <span class="amount-cell" :class="{ 'income': row.type === 'income', 'expense': row.type === 'withdrawal' }">
              {{ row.type === 'income' ? '+' : '-' }}¥{{ formatNumber(row.amount) }}
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
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @change="loadRecords"
      />
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
  Check, List, ChatDotRound, CircleClose
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
  realName: ''
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
  return records.value.filter(r => r.type === recordTypeFilter.value)
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
        color: ['#67c23a', '#409eff', '#f56c6c']
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
        itemStyle: { color: '#409eff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ])
        }
      },
      {
        name: '订单数',
        type: 'bar',
        yAxisIndex: 1,
        data: counts.length > 0 ? counts : [0],
        itemStyle: { color: '#67c23a' }
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
      page: page.value,
      pageSize: pageSize.value
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
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

// 统计卡片样式
.stats-row {
  margin-bottom: 20px;
}

.player-stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 20px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
  margin-bottom: 20px;
  
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }
  
  .stat-icon {
    width: 50px;
    height: 50px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    
    &.large {
      width: 60px;
      height: 60px;
      font-size: 32px;
    }
  }
  
  .stat-content {
    flex: 1;
  }
  
  .stat-label {
    font-size: 13px;
    color: #909399;
    margin-bottom: 5px;
  }
  
  .stat-value {
    font-size: 22px;
    font-weight: 700;
    color: #303133;
    
    &.large {
      font-size: 28px;
    }
  }
  
  .stat-sub {
    font-size: 12px;
    color: #c0c4cc;
    margin-top: 3px;
  }
  
  &.primary .stat-icon {
    background: #ecf5ff;
    color: #409eff;
  }
  
  &.info .stat-icon {
    background: #f4f4f5;
    color: #606266;
  }
  
  &.success .stat-icon {
    background: #f0f9eb;
    color: #67c23a;
  }
  
  &.warning .stat-icon {
    background: #fdf6ec;
    color: #e6a23c;
  }
  
  &.danger .stat-icon {
    background: #fef0f0;
    color: #f56c6c;
  }
  
  &.purple .stat-icon {
    background: #f5f0ff;
    color: #9254de;
  }
  
  &.cancelled .stat-icon {
    background: #fef0f0;
    color: #f56c6c;
  }
}

// 图表区域
.charts-row {
  margin-bottom: 20px;
}

.chart-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  
  :deep(.el-card__header) {
    padding: 15px 20px;
    border-bottom: 1px solid #ebeef5;
    
    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
      color: #303133;
    }
  }
}

.chart-container {
  height: 320px;
}

// 排行卡片
.ranking-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
  
  :deep(.el-card__header) {
    padding: 15px 20px;
    border-bottom: 1px solid #ebeef5;
    
    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
      color: #303133;
    }
  }
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 12px 15px;
  margin-bottom: 10px;
  background: #f5f7fa;
  border-radius: 8px;
  transition: background 0.2s;
  
  &:hover {
    background: #ecf5ff;
  }
  
  &.top3 {
    background: linear-gradient(135deg, #fff9e6 0%, #fff5d6 100%);
  }
  
  .rank-number {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: #dcdfe6;
    color: #606266;
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
    color: #303133;
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
    background: linear-gradient(90deg, #409eff 0%, #67c23a 100%);
    border-radius: 3px;
    transition: width 0.5s ease;
  }
  
  .rank-income {
    font-weight: 700;
    color: #67c23a;
    font-size: 15px;
    white-space: nowrap;
  }
}

// 提现卡片
.withdraw-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
  
  :deep(.el-card__header) {
    padding: 15px 20px;
    border-bottom: 1px solid #ebeef5;
    
    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
      color: #303133;
    }
  }
}

.withdraw-form {
  padding: 10px 0;
  
  .withdraw-btn {
    padding: 12px 30px;
    font-size: 15px;
  }
}

// 记录卡片
.records-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  
  :deep(.el-card__header) {
    padding: 15px 20px;
    border-bottom: 1px solid #ebeef5;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      span {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 600;
        color: #303133;
      }
    }
  }
}

.records-table {
  :deep(th) {
    background: #f5f7fa;
    font-weight: 600;
    color: #606266;
  }
  
  .player-name {
    font-weight: 500;
    color: #303133;
  }
  
  .time-cell {
    color: #606266;
    font-size: 13px;
  }
  
  .amount-cell {
    font-weight: 700;
    font-size: 15px;
    
    &.income {
      color: #67c23a;
    }
    
    &.expense {
      color: #f56c6c;
    }
  }
  
  .order-link {
    font-family: monospace;
    color: #409eff;
    font-weight: 500;
  }
  
  .no-order {
    color: #c0c4cc;
  }
  
  .description-cell {
    color: #606266;
    font-size: 13px;
  }
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
