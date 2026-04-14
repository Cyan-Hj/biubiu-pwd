<template>
  <div class="grab-hall-page">
    <el-card v-if="grabDisabled" class="hall-card hall-disabled">
      <div class="disabled-content">
        <el-icon class="disabled-icon"><Warning /></el-icon>
        <h2 class="disabled-title">抢单大厅暂时关闭</h2>
        <p class="disabled-desc">管理员已关闭抢单大厅，请稍后再来</p>
      </div>
    </el-card>
    <el-card v-else class="hall-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon class="title-icon"><Tickets /></el-icon>
            <span class="title-text">抢单大厅</span>
          </div>
          <div class="header-actions">
            <el-button @click="loadOrders" :loading="loading">
              <el-icon><Refresh /></el-icon>刷新
            </el-button>
          </div>
        </div>
      </template>

      <div class="filter-row">
        <el-radio-group v-model="filterType" @change="handleFilterChange" class="filter-group">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="SINGLE">单人单</el-radio-button>
          <el-radio-button label="DOUBLE">双人单</el-radio-button>
        </el-radio-group>
        <el-radio-group v-model="filterOrderType" @change="handleFilterChange" class="filter-group">
          <el-radio-button label="">全部类型</el-radio-button>
          <el-radio-button label="peiwand">陪玩单</el-radio-button>
          <el-radio-button label="huhang">护航单</el-radio-button>
        </el-radio-group>
      </div>

      <div v-loading="loading" class="order-list">
        <el-empty v-if="orders.length === 0 && !loading" description="暂无可抢订单" />

        <div v-for="order in orders" :key="order.id" class="order-card-item">
          <div class="order-header">
            <div class="order-info">
              <span class="order-no">{{ order.orderNo }}</span>
              <el-tag :type="order.orderType === 'huhang' ? 'danger' : 'primary'" size="small">
                {{ order.orderType === 'huhang' ? '护航单' : '陪玩单' }}
              </el-tag>
              <el-tag :type="order.playerCount === 'DOUBLE' ? 'warning' : 'success'" size="small">
                {{ order.playerCount === 'DOUBLE' ? '双人' : '单人' }}
              </el-tag>
              <el-tag v-if="order.priorityLevel" type="danger" size="small" effect="dark">
                优先等级: {{ order.priorityLevel }}
              </el-tag>
            </div>
            <div class="order-price">
              <span class="price">¥{{ order.pricePerHour }}/h</span>
              <span class="total">合计 ¥{{ order.totalAmount }}</span>
            </div>
          </div>

          <div class="order-body">
            <div class="order-detail">
              <div class="detail-item">
                <span class="label">老板：</span>
                <span class="value">{{ order.bossInfo || '散客' }}</span>
              </div>
              <div class="detail-item">
                <span class="label">时长：</span>
                <span class="value">{{ order.serviceHours }}小时</span>
              </div>
              <div class="detail-item">
                <span class="label">预估收入：</span>
                <span class="value income">¥{{ order.estimatedIncome }}</span>
              </div>
              <div v-if="order.serviceContent" class="detail-item">
                <span class="label">内容：</span>
                <span class="value">{{ order.serviceContent }}</span>
              </div>
              <div v-if="order.scheduledTime" class="detail-item">
                <span class="label">预约：</span>
                <span class="value">{{ formatTime(order.scheduledTime) }}</span>
              </div>
            </div>

            <div v-if="order.priorityLevel && order.isPriorityWaiting" class="priority-waiting-info">
              <el-icon><Warning /></el-icon>
              <span>{{ order.priorityWaitPlayers.map(p => p.nickname + '(' + p.level + ')').join('、') }} 等待优先确认中，剩余 {{ formatSeconds(order.priorityWaitRemainingSeconds) }}</span>
            </div>

            <div v-if="order.isLocked" class="locked-info">
              <el-icon><Lock /></el-icon>
              <span>已被锁定，等待队友加入 剩余 {{ formatSeconds(order.lockRemainingSeconds) }}</span>
            </div>

            <div v-if="order.waitingPlayers && order.waitingPlayers.length > 0" class="waiting-players">
              <div class="waiting-title">当前等待队友：</div>
              <div v-for="wp in order.waitingPlayers" :key="wp.id" class="waiting-player">
                <span class="wp-name">{{ wp.nickname }}</span>
                <span class="wp-level">({{ wp.level }})</span>
                <span class="wp-price">¥{{ wp.pricePerHour }}/h</span>
              </div>
            </div>
          </div>

          <div v-if="isPlayer" class="order-actions">
            <template v-if="order.grabStatus === 'OPEN' || order.grabStatus === 'WAITING'">
              <template v-if="order.playerCount === 'SINGLE'">
                <el-button type="primary" @click="handleGrabSingle(order)" :loading="grabbingOrderId === order.id">
                  立即抢单
                </el-button>
              </template>
              <template v-else>
                <el-button type="primary" @click="handleGrabDoubleWait(order)" :loading="grabbingOrderId === order.id">
                  进入等待列表
                </el-button>
                <template v-if="order.waitingPlayers && order.waitingPlayers.length > 0">
                  <el-button
                    v-for="wp in order.waitingPlayers.filter(p => p.id !== currentUserId)"
                    :key="wp.id"
                    type="success"
                    @click="handleGrabDoubleTeam(order, wp)"
                    :loading="grabbingOrderId === order.id"
                  >
                    与{{ wp.nickname }}组队
                  </el-button>
                </template>
                <el-button
                  v-if="order.grabStatus === 'OPEN'"
                  type="warning"
                  @click="handleGrabDoubleTeamGrab(order)"
                  :loading="grabbingOrderId === order.id"
                >
                  有固排
                </el-button>
                <div v-if="order.grabStatus === 'WAITING'" class="no-team-tip">
                  ⚠️ 有散排等待中，无法选择固排
                </div>
              </template>
            </template>
            <template v-else-if="order.grabStatus === 'LOCKED'">
              <template v-if="order.grabPartnerId === currentUserId">
                <el-button type="success" @click="handleJoinTeam(order)" :loading="grabbingOrderId === order.id">
                  加入队伍
                </el-button>
              </template>
              <template v-else>
                <el-button disabled type="info">已被锁定</el-button>
              </template>
            </template>
          </div>
        </div>

        <div v-if="totalPages > 1" class="pagination-row">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="totalElements"
            layout="prev, pager, next"
            @current-change="loadOrders"
          />
        </div>
      </div>
    </el-card>

    <el-dialog v-model="grabDialogVisible" :title="grabDialogTitle" width="450px" class="grab-dialog">
      <div class="grab-confirm-content">
        <div class="confirm-row"><span class="label">订单号：</span>{{ grabDialogOrder?.orderNo }}</div>
        <div class="confirm-row"><span class="label">类型：</span>{{ grabDialogOrder?.orderType === 'huhang' ? '护航单' : '陪玩单' }}</div>
        <div class="confirm-row"><span class="label">价格：</span>¥{{ grabDialogOrder?.pricePerHour }}/h</div>
        <div class="confirm-row"><span class="label">时长：</span>{{ grabDialogOrder?.serviceHours }}小时</div>
        <div class="confirm-row"><span class="label">预估收入：</span><span class="income">¥{{ grabDialogOrder?.estimatedIncome }}</span></div>

        <div v-if="grabDialogMode === 'DOUBLE_TEAM_GRAB'" class="team-grab-input">
          <el-form :model="teamGrabForm" label-width="100px">
            <el-form-item label="固排队友">
              <el-autocomplete
                v-model="teamGrabForm.partnerKeyword"
                :fetch-suggestions="queryPartnerSuggestions"
                placeholder="搜索昵称或手机号"
                @select="handlePartnerSelect"
                @clear="handlePartnerClear"
                clearable
                style="width: 100%"
              >
                <template #default="{ item }">
                  <div class="partner-suggestion">
                    <span class="partner-name">{{ item.nickname }}</span>
                    <span class="partner-info">{{ item.level }} · {{ item.phone }}</span>
                  </div>
                </template>
              </el-autocomplete>
            </el-form-item>
          </el-form>
          <div class="team-grab-tip">锁定时间内，固排队友需加入，超时将释放订单</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="grabDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmGrab" :loading="grabLoading">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="myStatusDialogVisible" title="我的抢单状态" width="500px">
      <div v-if="myStatus">
        <div v-if="myStatus.hasActiveOrder" class="status-warning">
          <el-icon><Warning /></el-icon> 您有进行中的订单，无法抢单
        </div>

        <div v-if="myStatus.waitingOrder" class="status-section">
          <h4>等待中的订单</h4>
          <p>订单号：{{ myStatus.waitingOrder.orderNo }}</p>
          <p>等待时间：{{ formatTime(myStatus.waitingOrder.waitingSince) }}</p>
          <div v-if="myStatus.waitingOrder.otherWaiters.length > 0">
            <p>其他等待者：</p>
            <div v-for="w in myStatus.waitingOrder.otherWaiters" :key="w.id" class="other-waiter">
              {{ w.nickname }} ({{ w.level }})
            </div>
          </div>
          <el-button type="danger" size="small" @click="handleCancelGrab(myStatus.waitingOrder.orderId)">取消等待</el-button>
        </div>

        <div v-if="myStatus.priorityWaitingOrder" class="status-section">
          <h4>优先等待中</h4>
          <p>订单号：{{ myStatus.priorityWaitingOrder.orderNo }}</p>
          <p>剩余时间：{{ formatSeconds(myStatus.priorityWaitingOrder.priorityWaitRemainingSeconds) }}</p>
          <el-button type="danger" size="small" @click="handleCancelGrab(myStatus.priorityWaitingOrder.orderId)">取消等待</el-button>
        </div>

        <div v-if="myStatus.lockedOrder" class="status-section">
          <h4>锁定中的订单</h4>
          <p>订单号：{{ myStatus.lockedOrder.orderNo }}</p>
          <p>队友：{{ myStatus.lockedOrder.partnerNickname }}</p>
          <p>剩余时间：{{ formatSeconds(myStatus.lockedOrder.lockRemainingSeconds) }}</p>
          <el-button type="danger" size="small" @click="handleCancelGrab(myStatus.lockedOrder.orderId)">取消锁定</el-button>
        </div>

        <div v-if="myStatus.cooldowns && myStatus.cooldowns.length > 0" class="status-section">
          <h4>冷却中的订单</h4>
          <div v-for="c in myStatus.cooldowns" :key="c.orderId" class="cooldown-item">
            订单 {{ c.orderNo }} 冷却剩余 {{ formatSeconds(c.remainingSeconds) }}
          </div>
        </div>

        <el-empty v-if="!myStatus.hasActiveOrder && !myStatus.waitingOrder && !myStatus.priorityWaitingOrder && !myStatus.lockedOrder && (!myStatus.cooldowns || myStatus.cooldowns.length === 0)" description="暂无抢单状态" />
      </div>
    </el-dialog>

    <div v-if="isPlayer" class="status-bar">
      <el-button type="info" @click="showMyStatus">我的状态</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGrabHallOrders, grabOrder, joinTeam, cancelGrab, getMyGrabStatus, searchPlayers } from '@/api/grabHall'
import { Tickets, Refresh, Warning, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id)
const isPlayer = computed(() => userStore.isPlayer)

const grabDisabled = ref(false)
const loading = ref(false)
const orders = ref([])
const currentPage = ref(1)
const pageSize = 20
const totalElements = ref(0)
const totalPages = ref(0)
const filterType = ref('')
const filterOrderType = ref('')
const grabbingOrderId = ref(null)

const grabDialogVisible = ref(false)
const grabDialogMode = ref('')
const grabDialogOrder = ref(null)
const grabLoading = ref(false)
const teamGrabForm = ref({ partnerKeyword: '', selectedPartnerId: null })

const myStatusDialogVisible = ref(false)
const myStatus = ref(null)

let pollingTimer = null

const grabDialogTitle = ref('')

const formatTime = (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-'

const formatSeconds = (secs) => {
  if (!secs || secs <= 0) return '00:00'
  const m = Math.floor(secs / 60)
  const s = secs % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const loadOrders = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize
    }
    if (filterType.value) params.playerCount = filterType.value
    if (filterOrderType.value) params.orderType = filterOrderType.value
    const res = await getGrabHallOrders(params)
    orders.value = res.data?.content || []
    totalElements.value = res.data?.totalElements || 0
    totalPages.value = res.data?.totalPages || 0
  } catch (e) {
    const msg = e.response?.data?.message || ''
    if (msg.includes('已关闭')) {
      grabDisabled.value = true
      if (pollingTimer) { clearInterval(pollingTimer); pollingTimer = null }
      return
    }
    if (!grabDisabled.value) {
      ElMessage.error('加载抢单大厅失败')
    }
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  currentPage.value = 1
  loadOrders()
}

const handleGrabSingle = (order) => {
  grabDialogMode.value = 'SINGLE'
  grabDialogOrder.value = order
  grabDialogTitle.value = '确认抢单'
  grabDialogVisible.value = true
}

const handleGrabDoubleWait = (order) => {
  grabDialogMode.value = 'DOUBLE_WAIT'
  grabDialogOrder.value = order
  grabDialogTitle.value = '抢单确认 - 进入等待列表'
  grabDialogVisible.value = true
}

const handleGrabDoubleTeam = (order, wp) => {
  grabDialogMode.value = 'DOUBLE_TEAM'
  grabDialogOrder.value = order
  teamGrabForm.value.targetPlayerId = wp.id
  grabDialogTitle.value = `确认与${wp.nickname}组队`
  grabDialogVisible.value = true
}

const handleGrabDoubleTeamGrab = (order) => {
  grabDialogMode.value = 'DOUBLE_TEAM_GRAB'
  grabDialogOrder.value = order
  teamGrabForm.value = { partnerKeyword: '', selectedPartnerId: null }
  grabDialogTitle.value = '抢单确认 - 有固排'
  grabDialogVisible.value = true
}

const queryPartnerSuggestions = async (queryString, cb) => {
  if (!queryString || queryString.trim().length < 1) {
    cb([])
    return
  }
  try {
    const res = await searchPlayers(queryString.trim())
    const suggestions = (res.data || []).map(p => ({
      ...p,
      value: p.nickname
    }))
    cb(suggestions)
  } catch (e) {
    cb([])
  }
}

const handlePartnerSelect = (item) => {
  teamGrabForm.value.selectedPartnerId = item.id
}

const handlePartnerClear = () => {
  teamGrabForm.value.selectedPartnerId = null
}

const confirmGrab = async () => {
  const order = grabDialogOrder.value
  grabbingOrderId.value = order.id
  grabLoading.value = true
  try {
    let data = {}
    if (grabDialogMode.value === 'SINGLE') {
      data = { grabType: 'SINGLE' }
    } else if (grabDialogMode.value === 'DOUBLE_WAIT') {
      data = { grabType: 'DOUBLE_SOLO', action: 'WAIT' }
    } else if (grabDialogMode.value === 'DOUBLE_TEAM') {
      data = { grabType: 'DOUBLE_SOLO', action: 'TEAM', targetPlayerId: teamGrabForm.value.targetPlayerId }
    } else if (grabDialogMode.value === 'DOUBLE_TEAM_GRAB') {
      if (!teamGrabForm.value.selectedPartnerId) {
        ElMessage.warning('请选择固排队友')
        return
      }
      data = { grabType: 'DOUBLE_TEAM', partnerId: teamGrabForm.value.selectedPartnerId }
    }

    const res = await grabOrder(order.id, data)
    const result = res.data

    if (result.status === 'PENDING_ACCEPT') {
      ElMessage.success('抢单成功！')
    } else if (result.status === 'IN_SERVICE') {
      ElMessage.success('组队成功，已进入服务中！')
    } else if (result.status === 'PRIORITY_WAITING') {
      ElMessage.info(`已进入优先等待期，等待${result.priorityWaitSeconds}秒内无高等级抢占则自动获得`)
    } else if (result.status === 'WAITING') {
      ElMessage.success('已进入等待列表')
    } else if (result.status === 'LOCKED') {
      ElMessage.success('已锁定订单，等待固排加入')
    }

    grabDialogVisible.value = false
    loadOrders()
  } catch (e) {
    // error handled by interceptor
  } finally {
    grabbingOrderId.value = null
    grabLoading.value = false
  }
}

const handleJoinTeam = async (order) => {
  grabbingOrderId.value = order.id
  grabLoading.value = true
  try {
    const res = await joinTeam(order.id)
    const result = res.data
    if (result.status === 'IN_SERVICE') {
      ElMessage.success('加入成功，订单开始服务！')
    } else {
      ElMessage.success('加入成功！')
    }
    grabDialogVisible.value = false
    loadOrders()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加入失败')
  } finally {
    grabbingOrderId.value = null
    grabLoading.value = false
  }
}

const handleCancelGrab = async (orderId) => {
  try {
    await ElMessageBox.confirm('确定要取消吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelGrab(orderId)
    ElMessage.success('已取消')
    loadMyStatus()
    loadOrders()
  } catch (e) {
    if (e !== 'cancel') {
      // error handled by interceptor
    }
  }
}

const showMyStatus = async () => {
  try {
    const res = await getMyGrabStatus()
    myStatus.value = res.data
    myStatusDialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取状态失败')
  }
}

const loadMyStatus = async () => {
  try {
    const res = await getMyGrabStatus()
    myStatus.value = res.data
  } catch (e) {}
}

onMounted(() => {
  loadOrders()
  pollingTimer = setInterval(() => {
    if (!grabDisabled.value) loadOrders()
  }, 5000)
})

onUnmounted(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
})
</script>

<style scoped lang="scss">
.grab-hall-page {
  position: relative;
  min-height: calc(100vh - 100px);
}

.hall-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.hall-disabled {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.disabled-content {
  text-align: center;
  padding: 60px 20px;
}

.disabled-icon {
  font-size: 64px;
  color: #e6a23c;
  margin-bottom: 20px;
}

.disabled-title {
  font-size: 22px;
  color: #303133;
  margin: 0 0 12px 0;
  font-weight: 600;
}

.disabled-desc {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;

  .title-icon {
    font-size: 24px;
    color: #667eea;
  }

  .title-text {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }
}

.filter-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;

  .filter-group {
    flex-wrap: wrap;
  }
}

.order-list {
  min-height: 200px;
}

.order-card-item {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 12px;
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.12);
    border-color: rgba(102, 126, 234, 0.3);
  }
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  .order-info {
    display: flex;
    align-items: center;
    gap: 8px;

    .order-no {
      font-weight: 600;
      color: #303133;
      font-size: 15px;
    }
  }

  .order-price {
    text-align: right;

    .price {
      font-size: 18px;
      font-weight: 700;
      color: #667eea;
    }

    .total {
      font-size: 13px;
      color: #909399;
      margin-left: 8px;
    }
  }
}

.order-body {
  .order-detail {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 10px;

    .detail-item {
      font-size: 14px;

      .label {
        color: #909399;
      }

      .value {
        color: #606266;

        &.income {
          color: #e6a23c;
          font-weight: 600;
        }
      }
    }
  }
}

.priority-waiting-info {
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 10px;
  color: #e6a23c;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.locked-info {
  background: #f4f4f5;
  border: 1px solid #e9e9eb;
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 10px;
  color: #909399;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.waiting-players {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 10px;

  .waiting-title {
    font-size: 13px;
    color: #67c23a;
    font-weight: 600;
    margin-bottom: 6px;
  }

  .waiting-player {
    font-size: 13px;
    color: #606266;
    margin-bottom: 4px;

    .wp-name {
      font-weight: 600;
    }

    .wp-level {
      color: #909399;
      margin: 0 4px;
    }

    .wp-price {
      color: #e6a23c;
    }
  }
}

.order-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;

  .no-team-tip {
    font-size: 12px;
    color: #e6a23c;
  }
}

.pagination-row {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.grab-dialog {
  .grab-confirm-content {
    .confirm-row {
      margin-bottom: 10px;
      font-size: 14px;

      .label {
        color: #909399;
      }

      .income {
        color: #e6a23c;
        font-weight: 600;
      }
    }
  }

  .team-grab-input {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid #ebeef5;
  }

  .team-grab-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 8px;
  }
}

.partner-suggestion {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 2px 0;

  .partner-name {
    font-weight: 500;
  }

  .partner-info {
    font-size: 12px;
    color: #909399;
  }
}

.status-bar {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 100;
}

.status-warning {
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 6px;
  padding: 10px;
  color: #e6a23c;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-section {
  margin-bottom: 20px;
  padding: 12px;
  background: #f8f8fc;
  border-radius: 8px;

  h4 {
    margin: 0 0 8px 0;
    color: #303133;
    font-size: 15px;
  }

  p {
    margin: 4px 0;
    font-size: 14px;
    color: #606266;
  }
}

.other-waiter {
  font-size: 13px;
  color: #606266;
  margin: 2px 0;
}

.cooldown-item {
  font-size: 13px;
  color: #f56c6c;
  margin: 4px 0;
}

@media (max-width: 768px) {
  .order-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .order-body .order-detail {
    flex-direction: column;
    gap: 6px;
  }

  .filter-row {
    flex-direction: column;
  }
}
</style>
