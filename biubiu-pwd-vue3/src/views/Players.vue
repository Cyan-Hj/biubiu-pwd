<template>
  <div class="players-page">
    <el-card class="player-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon class="title-icon"><UserFilled /></el-icon>
            <span class="title-text">陪玩师管理</span>
          </div>
          <div class="header-actions">
            <el-input
              v-model="searchQuery"
              placeholder="搜索昵称/手机号"
              class="search-input"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" class="search-btn" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
          </div>
        </div>
      </template>

      <!-- 等级筛选标签 -->
      <div class="filter-row">
        <div class="filter-label">等级筛选：</div>
        <el-radio-group v-model="levelFilter" @change="handleLevelChange" class="level-filter">
          <el-radio-button label="">全部等级</el-radio-button>
          <el-radio-button 
            v-for="level in availableLevels" 
            :key="level" 
            :label="level"
          >
            <el-tag :type="getLevelType(level)" size="small" effect="light" class="level-tag">
              {{ level }}
            </el-tag>
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 状态筛选标签 -->
      <div class="filter-row">
        <div class="filter-label">状态筛选：</div>
        <el-radio-group v-model="statusFilter" @change="handleStatusChange" class="status-filter">
          <el-radio-button label="">
            <el-icon><Grid /></el-icon>全部
          </el-radio-button>
          <el-radio-button label="pending">
            <el-icon><Timer /></el-icon>待审核
          </el-radio-button>
          <el-radio-button label="active">
            <el-icon><CircleCheck /></el-icon>已通过
          </el-radio-button>
          <el-radio-button label="disabled">
            <el-icon><CircleClose /></el-icon>已禁用
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-row">
        <div class="stat-card pending">
          <div class="stat-icon"><el-icon><Timer /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">待审核</div>
            <div class="stat-value">{{ playerStats.pending }}</div>
          </div>
        </div>
        <div class="stat-card active">
          <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">已通过</div>
            <div class="stat-value">{{ playerStats.active }}</div>
          </div>
        </div>
        <div class="stat-card disabled">
          <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">已禁用</div>
            <div class="stat-value">{{ playerStats.disabled }}</div>
          </div>
        </div>
        <div class="stat-card total">
          <div class="stat-icon"><el-icon><UserFilled /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">总人数</div>
            <div class="stat-value">{{ playerStats.total }}</div>
          </div>
        </div>
        <div v-if="isAdmin" class="stat-card upgrade" @click="showUpgradePanel = !showUpgradePanel" style="cursor: pointer;">
          <div class="stat-icon"><el-icon><TopRight /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">升级申请</div>
            <div class="stat-value">{{ pendingUpgradeCount }}</div>
          </div>
        </div>
      </div>

      <!-- 等级升级审批面板 -->
      <div v-if="showUpgradePanel" class="upgrade-panel">
        <div class="upgrade-panel-header">
          <span class="upgrade-panel-title">等级升级申请</span>
          <div class="upgrade-panel-actions">
            <el-button v-if="selectedUpgradeIds.length > 0" type="success" size="small" @click="handleBatchApprove">
              <el-icon><Check /></el-icon>一键批准 ({{ selectedUpgradeIds.length }})
            </el-button>
            <el-button size="small" @click="loadUpgradeApplications">
              <el-icon><Refresh /></el-icon>刷新
            </el-button>
          </div>
        </div>
        <el-table :data="upgradeApplications" v-loading="upgradeLoading" stripe size="small" @selection-change="val => selectedUpgradeIds = val.map(v => v.id)">
          <el-table-column type="selection" width="45" :selectable="row => row.status === 'PENDING'" />
          <el-table-column label="陪玩师" min-width="120">
            <template #default="{ row }">
              <span style="font-weight: 500; color: #303133;">{{ row.playerNo }} {{ row.playerNickname }}</span>
            </template>
          </el-table-column>
          <el-table-column label="当前等级" width="110">
            <template #default="{ row }">
              <el-tag :type="getLevelType(row.currentLevel)" size="small" effect="light">{{ row.currentLevel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请等级" width="110">
            <template #default="{ row }">
              <el-tag :type="getLevelType(row.requestedLevel)" size="small" effect="plain">{{ row.requestedLevel }}</el-tag>
              <div v-if="row.requestedLevelPrice" style="font-size: 11px; color: #ff6b6b; margin-top: 2px;">¥{{ row.requestedLevelPrice }}/h</div>
            </template>
          </el-table-column>
          <el-table-column label="理由" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">
              <span style="color: #606266; font-size: 12px;">{{ row.reason || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="getUpgradeStatusType(row.status)" size="small" effect="light">
                {{ getUpgradeStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="140">
            <template #default="{ row }">
              <span style="font-size: 12px; color: #909399;">{{ formatUpgradeDate(row.createdAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <div class="action-group">
                <el-button v-if="row.status === 'PENDING'" type="success" size="small" @click="handleApproveUpgrade(row)">
                  <el-icon><Check /></el-icon>批准
                </el-button>
                <el-button v-if="row.status === 'PENDING'" type="danger" size="small" @click="handleRejectUpgrade(row)">
                  <el-icon><CircleClose /></el-icon>拒绝
                </el-button>
                <span v-if="row.status !== 'PENDING'" style="font-size: 12px; color: #909399;">
                  {{ row.reviewerNickname || '-' }}
                </span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-container">
        <el-table :data="players" v-loading="loading" stripe class="player-table">
          <el-table-column prop="playerNo" label="编号" width="100" align="center">
            <template #default="{ row }">
              <span class="player-no">{{ row.playerNo || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="nickname" label="昵称" min-width="100" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="nickname-cell">
                <el-avatar :size="28" :src="row.avatar" class="avatar">
                  {{ row.nickname?.charAt(0) || '?' }}
                </el-avatar>
                <span class="nickname-text">{{ row.nickname }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" width="120">
            <template #default="{ row }">
              <span class="phone-text">{{ row.phone }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="level" label="等级" width="90">
            <template #default="{ row }">
              <el-tag :type="getLevelType(row.level)" effect="light" size="small">
                {{ row.level }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="pricePerHour" label="单价" width="90">
            <template #default="{ row }">
              <span class="price-text">¥{{ row.pricePerHour }}/h</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalIncome" label="累计收入" width="100">
            <template #default="{ row }">
              <span class="income-text">¥{{ row.totalIncome || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="availableBalance" label="可提现" width="100">
            <template #default="{ row }">
              <span class="balance-text">¥{{ row.availableBalance || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="押金" width="130">
            <template #default="{ row }">
              <div v-if="row.depositMode && row.depositMode !== 'NONE'">
                <span>¥{{ row.deposit || 0 }} / {{ row.depositLimit || 200 }}</span>
                <el-tag size="small" :type="row.depositMode === 'SELF_PAY' ? 'success' : 'warning'" style="margin-left: 4px">
                  {{ row.depositMode === 'SELF_PAY' ? '自缴' : '单抵' }}
                </el-tag>
              </div>
              <span v-else class="no-deposit">未设置</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" effect="light" size="small">
                <el-icon v-if="row.status === 'pending'"><Timer /></el-icon>
                <el-icon v-else-if="row.status === 'active'"><CircleCheck /></el-icon>
                <el-icon v-else><CircleClose /></el-icon>
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="activeOrdersCount" label="进行中" width="80" align="center">
            <template #default="{ row }">
              <el-badge :value="row.activeOrdersCount" :hidden="row.activeOrdersCount === 0 || row.activeOrdersCount === 1" type="primary">
                <span :class="['order-count', { 'idle': row.activeOrdersCount === 0, 'in-service': row.activeOrdersCount === 1 }]">
                  {{ row.activeOrdersCount === 0 ? '空闲' : (row.activeOrdersCount === 1 ? '服务中' : row.activeOrdersCount) }}
                </span>
              </el-badge>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <div class="action-group" v-if="isAdmin">
                <el-button
                  v-if="row.status === 'pending'"
                  type="success"
                  size="small"
                  @click="handleApprove(row)"
                >
                  <el-icon><Check /></el-icon>审核
                </el-button>
                <el-button type="primary" size="small" @click="handleEdit(row)">
                  <el-icon><Edit /></el-icon>编辑
                </el-button>
                <el-button type="info" size="small" @click="handleDeposit(row)">
                  <el-icon><Wallet /></el-icon>押金
                </el-button>
                <el-button type="warning" size="small" @click="handleResetPassword(row)">
                  <el-icon><Key /></el-icon>重置密码
                </el-button>
                <el-button type="danger" size="small" @click="handleDelete(row)">
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </div>
              <span v-else-if="isCustomerService" class="no-action">-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @current-change="loadPlayers"
        @size-change="loadPlayers"
      />
    </el-card>

    <!-- 审核对话框 -->
    <el-dialog v-model="approveDialogVisible" title="审核陪玩师" width="450px" class="player-dialog">
      <el-form :model="approveForm" label-width="100px" class="dialog-form">
        <el-form-item label="陪玩师">
          <div class="player-info">
            <el-avatar :size="40" :src="currentPlayer?.avatar">
              {{ currentPlayer?.nickname?.charAt(0) || '?' }}
            </el-avatar>
            <span class="player-name">{{ currentPlayer?.nickname }}</span>
          </div>
        </el-form-item>
        <el-form-item label="等级" prop="level">
          <el-select v-model="approveForm.level" placeholder="选择等级" style="width: 100%" @change="handleApproveLevelChange">
            <el-option
              v-for="level in availableLevels"
              :key="level"
              :label="level"
              :value="level"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="单价" prop="pricePerHour">
          <el-input-number v-model="approveForm.pricePerHour" :min="1" :max="1000" :precision="2" style="width: 100%" />
          <div class="form-tip" v-if="approveForm.level">默认价格: ¥{{ levelPriceMap[approveForm.level] }}/小时</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="success" @click="submitApprove">通过审核</el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑陪玩师" width="450px" class="player-dialog">
      <el-form :model="editForm" label-width="100px" class="dialog-form">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="editForm.level" placeholder="选择等级" style="width: 100%" @change="handleEditLevelChange">
            <el-option
              v-for="level in availableLevels"
              :key="level"
              :label="level"
              :value="level"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="单价">
          <el-input-number v-model="editForm.pricePerHour" :min="1" :max="1000" :precision="2" style="width: 100%" />
          <div class="form-tip" v-if="editForm.level">默认价格: ¥{{ levelPriceMap[editForm.level] }}/小时</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option label="已通过" value="active" />
            <el-option label="已禁用" value="disabled" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetPwdDialogVisible" title="重置密码" width="450px" class="player-dialog">
      <el-form :model="resetPwdForm" label-width="100px" class="dialog-form">
        <el-form-item label="陪玩师">
          <div class="player-info">
            <el-avatar :size="40" :src="currentPlayer?.avatar">
              {{ currentPlayer?.nickname?.charAt(0) || '?' }}
            </el-avatar>
            <span class="player-name">{{ currentPlayer?.nickname }}</span>
          </div>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="resetPwdForm.password" readonly class="password-input">
            <template #append>
              <el-tag type="info">默认密码</el-tag>
            </template>
          </el-input>
          <div class="form-tip">密码已固定为 123456，请提醒用户及时修改</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="submitResetPassword">确认重置</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="depositDialogVisible" title="押金管理" width="500px" class="player-dialog">
      <div v-if="currentPlayer" style="margin-bottom: 16px">
        <strong>{{ currentPlayer.nickname }}</strong> 当前押金：¥{{ currentPlayer.deposit || 0 }}
      </div>
      <el-form label-width="100px">
        <el-form-item label="押金模式">
          <el-select v-model="depositForm.depositMode" style="width: 100%">
            <el-option label="未设置" value="NONE" />
            <el-option label="自缴押金" value="SELF_PAY" />
            <el-option label="单抵押金" value="ORDER_DEDUCT" />
          </el-select>
        </el-form-item>
        <el-form-item label="押金上限">
          <el-input-number v-model="depositForm.depositLimit" :min="0" :step="50" style="width: 100%" />
        </el-form-item>
        <el-form-item label="当前押金">
          <el-input-number v-model="depositForm.deposit" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="depositDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDepositUpdate">保存</el-button>
        <el-button type="success" @click="submitDepositPay">自缴押金</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPlayers, approvePlayer, updatePlayer, resetPassword, deletePlayer, updateDeposit, payDeposit } from '@/api/users'
import { getLevelPrices } from '@/api/system'
import { getPendingLevelApplications, approveLevelApplication, rejectLevelApplication, batchApproveLevelApplications } from '@/api/levelUpgrade'
import { useUserStore } from '@/stores/user'
import { UserFilled, Search, Timer, CircleCheck, CircleClose, Check, Edit, Key, Grid, Delete, TopRight, Refresh, Wallet } from '@element-plus/icons-vue'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)
const isCustomerService = computed(() => userStore.isCustomerService)

const loading = ref(false)
const players = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchQuery = ref('')
const statusFilter = ref('')
const levelFilter = ref('')
const availableLevels = ref([])
const levelPriceMap = ref({})

const playerStats = ref({
  pending: 0,
  active: 0,
  disabled: 0,
  total: 0
})

const approveDialogVisible = ref(false)
const editDialogVisible = ref(false)
const resetPwdDialogVisible = ref(false)
const depositDialogVisible = ref(false)
const currentPlayer = ref(null)

const approveForm = reactive({
  level: '',
  pricePerHour: 50
})

const editForm = reactive({
  nickname: '',
  level: '',
  pricePerHour: 50,
  status: ''
})

const resetPwdForm = reactive({
  password: ''
})

const showUpgradePanel = ref(false)
const upgradeLoading = ref(false)
const upgradeApplications = ref([])
const selectedUpgradeIds = ref([])

const pendingUpgradeCount = computed(() => {
  return upgradeApplications.value.filter(a => a.status === 'PENDING').length
})

const getStatusType = (status) => {
  const types = { pending: 'warning', active: 'success', disabled: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { pending: '待审核', active: '已通过', disabled: '已禁用' }
  return texts[status] || status
}

const getLevelType = (level) => {
  const types = {
    '机密娱乐': '',
    '绝密娱乐': 'info',
    '机密技术': 'warning',
    '机密金牌': 'danger',
    '机密巅峰': 'success'
  }
  return types[level] || 'primary'
}

const getUpgradeStatusType = (status) => {
  const types = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return types[status] || 'info'
}

const getUpgradeStatusText = (status) => {
  const texts = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }
  return texts[status] || status
}

const formatUpgradeDate = (date) => {
  if (!date) return '-'
  const d = new Date(date)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

const loadLevels = async () => {
  try {
    const res = await getLevelPrices()
    availableLevels.value = res.data.map(item => item.level)
    levelPriceMap.value = {}
    res.data.forEach(item => {
      levelPriceMap.value[item.level] = item.defaultPrice
    })
  } catch (error) {
    console.error('加载等级失败', error)
  }
}

const calculateStats = () => {
  playerStats.value = {
    pending: players.value.filter(p => p.status === 'pending').length,
    active: players.value.filter(p => p.status === 'active').length,
    disabled: players.value.filter(p => p.status === 'disabled').length,
    total: total.value
  }
}

const loadPlayers = async () => {
  loading.value = true
  try {
    const res = await getPlayers({
      page: page.value,
      pageSize: pageSize.value,
      search: searchQuery.value,
      status: statusFilter.value,
      level: levelFilter.value
    })
    players.value = Array.isArray(res.data) ? res.data : (res.data?.list || [])
    total.value = res.data?.total || 0
    calculateStats()
  } finally {
    loading.value = false
  }
}

const loadUpgradeApplications = async () => {
  upgradeLoading.value = true
  try {
    const res = await getPendingLevelApplications()
    upgradeApplications.value = res.data || []
  } catch (error) {
    console.error('加载升级申请失败', error)
  } finally {
    upgradeLoading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadPlayers()
}

const handleStatusChange = () => {
  page.value = 1
  loadPlayers()
}

const handleLevelChange = () => {
  page.value = 1
  loadPlayers()
}

const handleApprove = (row) => {
  currentPlayer.value = row
  const defaultLevel = availableLevels.value[0] || '机密娱乐'
  approveForm.level = defaultLevel
  approveForm.pricePerHour = levelPriceMap.value[defaultLevel] || 50
  approveDialogVisible.value = true
}

const handleApproveLevelChange = (level) => {
  if (level && levelPriceMap.value[level]) {
    approveForm.pricePerHour = levelPriceMap.value[level]
  }
}

const submitApprove = async () => {
  try {
    await approvePlayer(currentPlayer.value.id, approveForm)
    ElMessage.success('审核通过')
    approveDialogVisible.value = false
    loadPlayers()
  } catch (error) {
    ElMessage.error('审核失败')
  }
}

const handleEdit = (row) => {
  currentPlayer.value = row
  Object.assign(editForm, {
    nickname: row.nickname,
    level: row.level,
    pricePerHour: row.pricePerHour,
    status: row.status
  })
  editDialogVisible.value = true
}

const handleEditLevelChange = (level) => {
  if (level && levelPriceMap.value[level]) {
    editForm.pricePerHour = levelPriceMap.value[level]
  }
}

const submitEdit = async () => {
  try {
    await updatePlayer(currentPlayer.value.id, editForm)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadPlayers()
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

const handleResetPassword = (row) => {
  currentPlayer.value = row
  resetPwdForm.password = '123456'
  resetPwdDialogVisible.value = true
}

const submitResetPassword = async () => {
  try {
    await resetPassword(currentPlayer.value.id, { password: '123456' })
    ElMessage.success('密码重置成功，新密码为：123456')
    resetPwdDialogVisible.value = false
  } catch (error) {
    ElMessage.error('密码重置失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除陪玩师 "${row.nickname}" 吗？此操作不可恢复！`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deletePlayer(row.id)
    ElMessage.success('删除成功')
    loadPlayers()
  } catch (error) {
    if (error !== 'cancel') {
    }
  }
}

const depositForm = reactive({
  depositMode: 'NONE',
  deposit: 0,
  depositLimit: 200,
  payAmount: 200
})

const handleDeposit = (row) => {
  currentPlayer.value = row
  depositForm.depositMode = row.depositMode || 'NONE'
  depositForm.deposit = row.deposit || 0
  depositForm.depositLimit = row.depositLimit || 200
  depositForm.payAmount = row.depositLimit || 200
  depositDialogVisible.value = true
}

const submitDepositUpdate = async () => {
  try {
    await updateDeposit(currentPlayer.value.id, {
      depositMode: depositForm.depositMode,
      deposit: depositForm.deposit,
      depositLimit: depositForm.depositLimit
    })
    ElMessage.success('押金信息更新成功')
    depositDialogVisible.value = false
    loadPlayers()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '更新失败')
  }
}

const submitDepositPay = async () => {
  try {
    await payDeposit(currentPlayer.value.id, {
      amount: depositForm.payAmount
    })
    ElMessage.success('押金缴纳成功')
    depositDialogVisible.value = false
    loadPlayers()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '缴纳失败')
  }
}

const handleApproveUpgrade = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定批准 "${row.playerNickname}" 升级到 "${row.requestedLevel}" 吗？`,
      '批准升级',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'success' }
    )
    await approveLevelApplication(row.id)
    ElMessage.success('已批准')
    loadUpgradeApplications()
    loadPlayers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleRejectUpgrade = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定拒绝 "${row.playerNickname}" 的升级申请吗？`,
      '拒绝升级',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await rejectLevelApplication(row.id)
    ElMessage.success('已拒绝')
    loadUpgradeApplications()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleBatchApprove = async () => {
  try {
    await ElMessageBox.confirm(
      `确定一键批准选中的 ${selectedUpgradeIds.value.length} 个申请吗？`,
      '一键批准',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'success' }
    )
    await batchApproveLevelApplications(selectedUpgradeIds.value)
    ElMessage.success('批量审批完成')
    selectedUpgradeIds.value = []
    loadUpgradeApplications()
    loadPlayers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量审批失败')
    }
  }
}

onMounted(() => {
  loadLevels()
  loadPlayers()
  // 只有管理员才加载升级申请
  if (isAdmin.value) {
    loadUpgradeApplications()
  }
})
</script>

<style scoped lang="scss">
.players-page {
  padding: 20px;
  background: #f8f8fc;
  min-height: 100vh;
}

.player-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

  :deep(.el-card__header) {
    padding: 20px;
    border-bottom: 1px solid #ebeef5;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;

  .title-icon {
    font-size: 24px;
    color: #ff6b6b;
  }

  .title-text {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input {
  width: 220px;
}

.search-btn {
  display: flex;
  align-items: center;
  gap: 5px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
  flex-wrap: wrap;

  .filter-label {
    font-size: 14px;
    color: #606266;
    font-weight: 500;
    white-space: nowrap;
  }
}

.level-filter {
  :deep(.el-radio-button__inner) {
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 8px 16px;
    font-size: 13px;

    .level-tag {
      margin: 0;
      border: none;
      background: transparent;
    }
  }

  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    .level-tag {
      background: #fff;
    }
  }
}

.status-filter {
  :deep(.el-radio-button__inner) {
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 8px 16px;
    font-size: 13px;
  }
}

.stats-row {
  display: flex;
  gap: 15px;
  margin: 25px 0;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 150px;
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 20px;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  .stat-icon {
    width: 50px;
    height: 50px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
  }

  .stat-info {
    flex: 1;
  }

  .stat-label {
    font-size: 13px;
    color: #909399;
    margin-bottom: 5px;
  }

  .stat-value {
    font-size: 24px;
    font-weight: 700;
    color: #303133;
  }

  &.pending .stat-icon {
    background: #fdf6ec;
    color: #e6a23c;
  }

  &.active .stat-icon {
    background: #fff8f0;
    color: #f0c27f;
  }

  &.disabled .stat-icon {
    background: #fef0f0;
    color: #f56c6c;
  }

  &.total .stat-icon {
    background: #fff5f5;
    color: #ff6b6b;
  }

  &.upgrade {
    border: 2px solid #e8f4e8;
    background: linear-gradient(135deg, #f0f9eb 0%, #fff 100%);

    .stat-icon {
      background: #f0f9eb;
      color: #67c23a;
    }

    .stat-value {
      color: #67c23a;
    }
  }
}

.upgrade-panel {
  margin-bottom: 24px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e8f4e8;
  overflow: hidden;

  .upgrade-panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 20px;
    background: linear-gradient(135deg, #f0f9eb 0%, #e8f5e9 100%);

    .upgrade-panel-title {
      font-size: 15px;
      font-weight: 600;
      color: #2d6a2e;
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .upgrade-panel-actions {
      display: flex;
      gap: 8px;
    }
  }

  :deep(.el-table) {
    border-radius: 0;

    th.el-table__cell {
      background: #fafbfc;
      font-weight: 600;
      color: #606266;
    }
  }
}

.table-container {
  margin-top: 20px;
  overflow-x: auto;
  min-width: 100%;
}

.player-table {
  min-width: 950px;

  :deep(th) {
    background: #f8f8fc;
    font-weight: 600;
    color: #606266;
  }

  .player-no {
    font-family: monospace;
    font-weight: 600;
    color: #ff6b6b;
    font-size: 13px;
  }

  .nickname-cell {
    display: flex;
    align-items: center;
    gap: 10px;

    .avatar {
      flex-shrink: 0;
    }

    .nickname-text {
      font-weight: 500;
      color: #303133;
    }
  }

  .phone-text {
    font-family: monospace;
    color: #606266;
  }

  .price-text {
    font-weight: 600;
    color: #ff6b6b;
  }

  .income-text {
    font-weight: 600;
    color: #f0c27f;
  }

  .balance-text {
    font-weight: 600;
    color: #e6a23c;
  }

  .order-count {
    font-weight: 600;
    color: #606266;

    &.idle {
      color: #f0c27f;
      font-size: 12px;
    }

    &.in-service {
      color: #ff6b6b;
      font-size: 12px;
    }
  }

  .action-group {
    display: flex;
    gap: 5px;
    flex-wrap: wrap;
  }

  .no-action {
    color: #c0c4cc;
    text-align: center;
    display: block;
  }
}

.pagination {
  margin-top: 25px;
  justify-content: flex-end;
}

.player-dialog {
  :deep(.el-dialog__header) {
    padding: 20px;
    border-bottom: 1px solid #ebeef5;

    .el-dialog__title {
      font-weight: 600;
    }
  }

  :deep(.el-dialog__body) {
    padding: 25px 20px;
  }
}

.dialog-form {
  .player-info {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px;
    background: #f8f8fc;
    border-radius: 8px;

    .player-name {
      font-weight: 600;
      font-size: 15px;
      color: #303133;
    }
  }

  .form-tip {
    color: #909399;
    font-size: 12px;
    margin-top: 5px;
  }
}

@media (max-width: 768px) {
  .players-page {
    padding: 12px;
  }

  .player-card {
    :deep(.el-card__header) {
      padding: 16px;
    }
  }

  .card-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .header-title {
    .title-icon {
      font-size: 20px;
    }

    .title-text {
      font-size: 18px;
    }
  }

  .header-actions {
    .search-input {
      flex: 1;
      width: auto;
    }
  }

  .filter-row {
    margin-bottom: 12px;

    .filter-label {
      font-size: 13px;
    }
  }

  .level-filter,
  .status-filter {
    :deep(.el-radio-button__inner) {
      padding: 6px 10px;
      font-size: 12px;
    }
  }

  .stats-row {
    gap: 10px;
    margin: 16px 0;
  }

  .stat-card {
    flex: 0 0 calc(33.333% - 7px);
    min-width: auto;
    padding: 12px;
    gap: 8px;

    .stat-icon {
      width: 36px;
      height: 36px;
      border-radius: 8px;
      font-size: 18px;
    }

    .stat-info {
      .stat-label {
        font-size: 11px;
        margin-bottom: 2px;
      }

      .stat-value {
        font-size: 18px;
      }
    }
  }

  .upgrade-panel {
    margin-bottom: 16px;

    .upgrade-panel-header {
      padding: 12px 14px;
      flex-direction: column;
      align-items: flex-start;
      gap: 10px;

      .upgrade-panel-title {
        font-size: 14px;
      }

      .upgrade-panel-actions {
        width: 100%;
        justify-content: flex-end;
      }
    }

    :deep(.el-table) {
      font-size: 12px;

      th.el-table__cell,
      td.el-table__cell {
        padding: 8px 6px;
      }
    }
  }

  .table-container {
    margin-top: 16px;
    border-radius: 8px;
    overflow-x: auto;
  }

  .player-table {
    min-width: 800px;
    font-size: 12px;

    :deep(th) {
      padding: 10px 6px;
    }

    :deep(td) {
      padding: 8px 6px;
    }

    .nickname-cell {
      gap: 6px;

      .avatar {
        width: 24px;
        height: 24px;
        font-size: 12px;
      }

      .nickname-text {
        font-size: 12px;
      }
    }

    .action-group {
      gap: 3px;

      .el-button {
        padding: 4px 8px;
        font-size: 11px;
      }
    }
  }

  .pagination {
    margin-top: 16px;
    justify-content: center;
  }

  .player-dialog {
    :deep(.el-dialog) {
      width: 90% !important;
      max-width: 450px;
    }

    :deep(.el-dialog__header) {
      padding: 16px;
    }

    :deep(.el-dialog__body) {
      padding: 16px;
    }
  }
}

@media (max-width: 480px) {
  .players-page {
    padding: 8px;
  }

  .player-card {
    border-radius: 10px;

    :deep(.el-card__header) {
      padding: 12px;
    }
  }

  .header-title {
    .title-icon {
      font-size: 18px;
    }

    .title-text {
      font-size: 16px;
    }
  }

  .header-actions {
    flex-direction: column;
    gap: 8px;

    .search-input {
      width: 100%;
    }

    .search-btn {
      width: 100%;
      justify-content: center;
    }
  }

  .filter-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 10px;

    .filter-label {
      font-size: 12px;
    }
  }

  .level-filter,
  .status-filter {
    width: 100%;

    :deep(.el-radio-group) {
      display: flex;
      flex-wrap: wrap;
    }

    :deep(.el-radio-button) {
      flex: 1;
    }

    :deep(.el-radio-button__inner) {
      padding: 5px 8px;
      font-size: 11px;
      width: 100%;
    }
  }

  .stats-row {
    gap: 8px;
    margin: 12px 0;
  }

  .stat-card {
    flex: 0 0 calc(50% - 4px);
    padding: 10px;
    gap: 6px;

    .stat-icon {
      width: 32px;
      height: 32px;
      border-radius: 6px;
      font-size: 16px;
    }

    .stat-info {
      .stat-label {
        font-size: 10px;
        margin-bottom: 1px;
      }

      .stat-value {
        font-size: 16px;
      }
    }
  }

  .upgrade-panel {
    margin-bottom: 12px;
    border-radius: 8px;

    .upgrade-panel-header {
      padding: 10px 12px;

      .upgrade-panel-title {
        font-size: 13px;
      }

      .upgrade-panel-actions {
        gap: 6px;

        .el-button {
          padding: 5px 10px;
          font-size: 11px;
        }
      }
    }

    :deep(.el-table) {
      font-size: 11px;

      th.el-table__cell,
      td.el-table__cell {
        padding: 6px 4px;
      }

      .cell {
        padding: 0 2px;
      }
    }
  }

  .table-container {
    margin-top: 12px;
    border-radius: 6px;
  }

  .player-table {
    min-width: 700px;
    font-size: 11px;

    :deep(th) {
      padding: 8px 4px;
    }

    :deep(td) {
      padding: 6px 4px;
    }

    .player-no {
      font-size: 11px;
    }

    .nickname-cell {
      gap: 4px;

      .avatar {
        width: 20px;
        height: 20px;
        font-size: 10px;
      }

      .nickname-text {
        font-size: 11px;
      }
    }

    .phone-text,
    .price-text,
    .income-text,
    .balance-text {
      font-size: 11px;
    }

    .action-group {
      gap: 2px;

      .el-button {
        padding: 3px 6px;
        font-size: 10px;
      }
    }
  }

  .pagination {
    margin-top: 12px;

    :deep(.el-pagination) {
      justify-content: center;
      flex-wrap: wrap;
      gap: 8px;
    }

    :deep(.el-pagination__sizes) {
      margin-right: 0;
    }

    :deep(.el-pagination__total) {
      display: block;
      width: 100%;
      text-align: center;
      margin-bottom: 8px;
    }
  }

  .player-dialog {
    :deep(.el-dialog) {
      width: 95% !important;
      max-width: none;
      margin: 10px auto;
    }

    :deep(.el-dialog__header) {
      padding: 12px 16px;
    }

    :deep(.el-dialog__body) {
      padding: 12px;
    }

    :deep(.el-dialog__footer) {
      padding: 12px 16px;
    }
  }

  .dialog-form {
    .player-info {
      padding: 8px;
      gap: 8px;

      .player-name {
        font-size: 14px;
      }
    }

    .form-tip {
      font-size: 11px;
    }
  }
}
</style>
