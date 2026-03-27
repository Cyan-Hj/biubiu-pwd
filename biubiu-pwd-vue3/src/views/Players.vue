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
      </div>

      <div class="table-container">
        <el-table :data="players" v-loading="loading" stripe class="player-table">
          <el-table-column prop="id" label="ID" width="60" align="center">
            <template #default="{ row }">
              <span class="id-text">{{ row.id }}</span>
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
          <!-- 操作列 -->
          <el-table-column label="操作" width="220" fixed="right">
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
                <el-button type="warning" size="small" @click="handleResetPassword(row)">
                  <el-icon><Key /></el-icon>重置密码
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

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPlayers, approvePlayer, updatePlayer, resetPassword } from '@/api/users'
import { getLevelPrices } from '@/api/system'
import { useUserStore } from '@/stores/user'
import { UserFilled, Search, Timer, CircleCheck, CircleClose, Check, Edit, Key, Grid } from '@element-plus/icons-vue'

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
const levelPriceMap = ref({}) // 等级价格映射

const playerStats = ref({
  pending: 0,
  active: 0,
  disabled: 0,
  total: 0
})

const approveDialogVisible = ref(false)
const editDialogVisible = ref(false)
const resetPwdDialogVisible = ref(false)
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

const getStatusType = (status) => {
  const types = {
    pending: 'warning',
    active: 'success',
    disabled: 'danger'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    pending: '待审核',
    active: '已通过',
    disabled: '已禁用'
  }
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

const loadLevels = async () => {
  try {
    const res = await getLevelPrices()
    availableLevels.value = res.data.map(item => item.level)
    // 构建等级价格映射
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
  // 自动填充默认价格
  approveForm.pricePerHour = levelPriceMap.value[defaultLevel] || 50
  approveDialogVisible.value = true
}

// 审核对话框等级变化时自动填充价格
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

// 编辑对话框等级变化时自动填充价格
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

onMounted(() => {
  loadLevels()
  loadPlayers()
})
</script>

<style scoped lang="scss">
.players-page {
  padding: 20px;
  background: #f5f7fa;
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
    color: #409eff;
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

// 筛选行
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

// 等级筛选
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

// 状态筛选
.status-filter {
  :deep(.el-radio-button__inner) {
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 8px 16px;
    font-size: 13px;
  }
}

// 统计卡片
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
    background: #f0f9eb;
    color: #67c23a;
  }
  
  &.disabled .stat-icon {
    background: #fef0f0;
    color: #f56c6c;
  }
  
  &.total .stat-icon {
    background: #ecf5ff;
    color: #409eff;
  }
}

// 表格样式
.table-container {
  margin-top: 20px;
  overflow-x: auto;
  min-width: 100%;
}

.player-table {
  min-width: 950px;
  
  :deep(th) {
    background: #f5f7fa;
    font-weight: 600;
    color: #606266;
  }
  
  .id-text {
    font-family: monospace;
    font-weight: 600;
    color: #909399;
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
    color: #409eff;
  }
  
  .income-text {
    font-weight: 600;
    color: #67c23a;
  }
  
  .balance-text {
    font-weight: 600;
    color: #e6a23c;
  }
  
  .order-count {
    font-weight: 600;
    color: #606266;

    &.idle {
      color: #67c23a;
      font-size: 12px;
    }

    &.in-service {
      color: #409eff;
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

// 分页
.pagination {
  margin-top: 25px;
  justify-content: flex-end;
}

// 对话框样式
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
    background: #f5f7fa;
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

</style>
