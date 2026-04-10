<template>
  <div class="profile-page">
    <!-- 顶部：基本信息 + 修改密码 -->
    <el-row :gutter="24" class="top-section">
      <el-col :xs="24" :sm="12">
        <el-card class="info-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>基本信息</span>
            </div>
          </template>
          <div class="info-body">
            <div class="avatar-section">
              <el-avatar :size="72" class="user-avatar">
                {{ profile.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="avatar-info">
                <h3>{{ profile.nickname }}</h3>
                <el-tag :type="getRoleTagType()" effect="light" size="small">{{ getRoleText() }}</el-tag>
              </div>
            </div>
            <div class="info-list">
              <div class="info-item">
                <span class="info-label">手机号</span>
                <span class="info-value">{{ maskPhone(profile.phone) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">昵称</span>
                <div class="info-value editable">
                  <span>{{ profile.nickname }}</span>
                  <el-button type="primary" link size="small" @click="showNicknameDialog = true">
                    <el-icon><Edit /></el-icon> 修改
                  </el-button>
                </div>
              </div>
              <div class="info-item">
                <span class="info-label">角色</span>
                <span class="info-value">{{ getRoleText() }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">注册时间</span>
                <span class="info-value">{{ formatDate(profile.createdAt) }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12">
        <el-card class="password-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>修改密码</span>
            </div>
          </template>
          <div class="password-body">
            <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="0" class="password-form">
              <el-form-item prop="oldPassword">
                <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" show-password prefix-icon="Lock" />
              </el-form-item>
              <el-form-item prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password prefix-icon="Key" />
              </el-form-item>
              <el-form-item prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password prefix-icon="CircleCheck" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading" class="submit-btn">确认修改</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 陪玩师信息 -->
    <el-card v-if="isPlayer" class="player-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>陪玩师信息</span>
        </div>
      </template>
      <div class="player-stats">
        <div class="stat-item">
          <div class="stat-icon">
            <el-icon><Ticket /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">编号</div>
            <div class="stat-value mono">{{ profile.playerNo || '-' }}</div>
          </div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-icon">
            <el-icon><Medal /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">等级</div>
            <div class="stat-value">
              <el-tag :type="getLevelType()" effect="light" size="small">{{ profile.level || '-' }}</el-tag>
            </div>
          </div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item upgrade-action" v-if="isPlayer">
          <div class="stat-icon upgrade-icon">
            <el-icon><TopRight /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">等级提升</div>
            <div class="stat-value">
              <el-button type="primary" link size="small" @click="showUpgradeDialog = true">
                申请升级
              </el-button>
            </div>
          </div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-icon">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">状态</div>
            <div class="stat-value">
              <el-tag :type="getPlayerStatusType()" effect="light" size="small">{{ getPlayerStatusText() }}</el-tag>
            </div>
          </div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-icon">
            <el-icon><Money /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">单价</div>
            <div class="stat-value price">¥{{ profile.pricePerHour || 0 }}/小时</div>
          </div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-icon">
            <el-icon><Wallet /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">总收入</div>
            <div class="stat-value income">¥{{ profile.totalIncome || 0 }}</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 修改昵称对话框 -->
    <el-dialog v-model="showNicknameDialog" title="修改昵称" width="400px" :close-on-click-modal="false">
      <el-form :model="nicknameForm" :rules="nicknameRules" ref="nicknameFormRef" label-width="0">
        <el-form-item prop="nickname">
          <el-input v-model="nicknameForm.nickname" placeholder="请输入新昵称" maxlength="20" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNicknameDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateNickname" :loading="nicknameLoading">确认</el-button>
      </template>
    </el-dialog>

    <!-- 申请升级对话框 -->
    <el-dialog v-model="showUpgradeDialog" title="申请等级升级" width="450px" :close-on-click-modal="false">
      <el-form :model="upgradeForm" :rules="upgradeRules" ref="upgradeFormRef" label-width="90px">
        <el-form-item label="当前等级">
          <el-tag :type="getLevelType()" effect="light">{{ profile.level || '-' }}</el-tag>
        </el-form-item>
        <el-form-item label="申请等级" prop="requestedLevel">
          <el-select v-model="upgradeForm.requestedLevel" placeholder="请选择目标等级" style="width: 100%">
            <el-option
              v-for="level in upgradeLevels"
              :key="level.name"
              :label="`${level.name} (¥${level.price}/小时)`"
              :value="level.name"
              :disabled="level.name === profile.level"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申请理由">
          <el-input v-model="upgradeForm.reason" type="textarea" :rows="3" placeholder="请简要说明升级理由（选填）" maxlength="200" show-word-limit />
        </el-form-item>
        <div v-if="hasPendingApplication" class="pending-tip">
          <el-icon><Warning /></el-icon> 您已有待审核的申请，请耐心等待审批
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showUpgradeDialog = false">取消</el-button>
        <el-button type="primary" @click="handleApplyUpgrade" :loading="upgradeLoading" :disabled="hasPendingApplication">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 升级申请记录 -->
    <el-card v-if="isPlayer && upgradeApplications.length > 0" class="upgrade-history-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>升级申请记录</span>
          <el-tag v-if="hasPendingApplication" type="warning" effect="light" size="small">审批中</el-tag>
        </div>
      </template>
      <div class="upgrade-timeline">
        <div v-for="app in upgradeApplications" :key="app.id" class="timeline-item">
          <div class="timeline-dot" :class="app.status.toLowerCase()"></div>
          <div class="timeline-content">
            <div class="timeline-header">
              <div class="level-change">
                <el-tag size="small" :type="getLevelTypeByName(app.currentLevel)" effect="light">{{ app.currentLevel }}</el-tag>
                <span class="level-arrow">→</span>
                <el-tag size="small" :type="getLevelTypeByName(app.requestedLevel)" effect="plain">{{ app.requestedLevel }}</el-tag>
              </div>
              <el-tag :type="getUpgradeStatusType(app.status)" size="small" effect="light" round>
                {{ getUpgradeStatusText(app.status) }}
              </el-tag>
            </div>
            <div class="timeline-meta">
              <span class="meta-time">{{ formatDate(app.createdAt) }}</span>
              <span v-if="app.reviewerNickname" class="meta-reviewer">审批人: {{ app.reviewerNickname }}</span>
            </div>
            <div v-if="app.reason" class="timeline-reason">
              <el-icon><ChatDotRound /></el-icon> {{ app.reason }}
            </div>
            <div v-if="app.reviewComment" class="timeline-comment">
              审批意见: {{ app.reviewComment }}
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 客服账号管理 - 仅管理员可见 -->
    <el-card v-if="isAdmin" class="cs-management-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-with-action">
            <span>客服账号管理</span>
            <el-button type="primary" size="small" @click="showCreateCsDialog = true">
              <el-icon><Plus /></el-icon> 创建客服
            </el-button>
          </div>
        </div>
      </template>
      <el-table :data="csAccounts" v-loading="csLoading" stripe>
        <el-table-column prop="nickname" label="昵称" min-width="140">
          <template #default="{ row }">
            <div class="cs-nickname">
              <el-avatar :size="32" class="cs-avatar">
                {{ row.nickname?.charAt(0) || 'C' }}
              </el-avatar>
              <span>{{ row.nickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">
            <span class="cs-phone">{{ row.phone }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            <span class="cs-time">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag 
              :type="row.enabled !== false ? 'success' : 'danger'" 
              size="small" 
              effect="light"
              round
            >
              {{ row.enabled !== false ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="cs-actions">
              <el-tooltip content="编辑" placement="top">
                <el-button type="primary" link @click="handleEditCs(row)">
                  <el-icon><Edit /></el-icon>
                </el-button>
              </el-tooltip>
              <el-divider direction="vertical" />
              <el-tooltip content="重置密码" placement="top">
                <el-button type="warning" link @click="handleResetCsPassword(row)">
                  <el-icon><Key /></el-icon>
                </el-button>
              </el-tooltip>
              <el-divider direction="vertical" />
              <el-tooltip :content="row.enabled !== false ? '禁用' : '启用'" placement="top">
                <el-button 
                  :type="row.enabled !== false ? 'danger' : 'success'" 
                  link 
                  @click="handleToggleCsStatus(row)"
                >
                  <el-icon><component :is="row.enabled !== false ? 'CircleClose' : 'CircleCheck'" /></el-icon>
                </el-button>
              </el-tooltip>
              <el-divider direction="vertical" />
              <el-tooltip content="删除" placement="top">
                <el-button type="danger" link @click="handleDeleteCs(row)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="csAccounts.length === 0 && !csLoading" description="暂无客服账号" :image-size="60" />
    </el-card>

    <!-- 创建客服对话框 -->
    <el-dialog v-model="showCreateCsDialog" title="创建客服账号" width="450px" :close-on-click-modal="false">
      <el-form :model="csForm" :rules="csRules" ref="csFormRef" label-width="80px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="csForm.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="csForm.nickname" placeholder="请输入昵称" maxlength="20" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="csForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateCsDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateCs" :loading="csCreating">创建</el-button>
      </template>
    </el-dialog>

    <!-- 编辑客服对话框 -->
    <el-dialog v-model="showEditCsDialog" title="编辑客服账号" width="450px" :close-on-click-modal="false">
      <el-form :model="editCsForm" :rules="editCsRules" ref="editCsFormRef" label-width="80px">
        <el-form-item label="手机号">
          <el-input v-model="editCsForm.phone" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editCsForm.nickname" placeholder="请输入昵称" maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditCsDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEditCs" :loading="csEditing">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置客服密码对话框 -->
    <el-dialog v-model="showResetCsPwdDialog" title="重置客服密码" width="400px">
      <el-form :model="resetCsPwdForm" :rules="resetCsPwdRules" ref="resetCsPwdFormRef" label-width="80px">
        <el-form-item label="客服">
          <div class="reset-cs-info">
            <el-avatar :size="32" class="cs-avatar">{{ currentCsAccount?.nickname?.charAt(0) || 'C' }}</el-avatar>
            <span>{{ currentCsAccount?.nickname }}</span>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetCsPwdForm.password" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showResetCsPwdDialog = false">取消</el-button>
        <el-button type="primary" @click="submitResetCsPassword" :loading="csResetting">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Ticket, Medal, CircleCheck, Money, Wallet, TopRight, Right, Warning, ChatDotRound, Plus, Key, CircleClose, Delete } from '@element-plus/icons-vue'
import { getProfile, updateNickname, changePassword } from '@/api/auth'
import { applyLevelUpgrade, getMyLevelApplications } from '@/api/levelUpgrade'
import { getLevelPrices } from '@/api/system'
import { getCustomerServiceList, createCustomerService, updateCustomerService, resetCustomerServicePassword, deleteCustomerService } from '@/api/users'
import dayjs from 'dayjs'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)
const isPlayer = computed(() => userStore.isPlayer)

const profile = ref({})
const showNicknameDialog = ref(false)
const showUpgradeDialog = ref(false)
const nicknameLoading = ref(false)
const passwordLoading = ref(false)
const upgradeLoading = ref(false)
const nicknameFormRef = ref(null)
const passwordFormRef = ref(null)
const upgradeFormRef = ref(null)
const upgradeApplications = ref([])
const upgradeLevels = ref([])

// 客服管理相关
const csLoading = ref(false)
const csCreating = ref(false)
const csEditing = ref(false)
const csResetting = ref(false)
const csDeleting = ref(false)
const csAccounts = ref([])
const showCreateCsDialog = ref(false)
const showEditCsDialog = ref(false)
const showResetCsPwdDialog = ref(false)
const currentCsAccount = ref(null)
const csFormRef = ref(null)
const editCsFormRef = ref(null)
const resetCsPwdFormRef = ref(null)

const csForm = reactive({
  phone: '',
  nickname: '',
  password: ''
})

const csRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

const editCsForm = reactive({
  id: null,
  phone: '',
  nickname: ''
})

const editCsRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const resetCsPwdForm = reactive({
  password: ''
})

const resetCsPwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

const nicknameForm = reactive({ nickname: '' })
const nicknameRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const upgradeForm = reactive({
  requestedLevel: '',
  reason: ''
})

const upgradeRules = {
  requestedLevel: [{ required: true, message: '请选择目标等级', trigger: 'change' }]
}

const hasPendingApplication = computed(() => {
  return upgradeApplications.value.some(app => app.status === 'PENDING')
})

const getRoleText = () => {
  if (isAdmin.value) return '管理员'
  if (userStore.isCustomerService) return '客服'
  return '陪玩师'
}

const getRoleTagType = () => {
  if (isAdmin.value) return 'danger'
  if (userStore.isCustomerService) return 'warning'
  return 'primary'
}

const maskPhone = (phone) => {
  if (!phone) return '-'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const getPlayerStatusType = () => {
  const types = { idle: 'success', in_service: 'primary', offline: 'info', pending: 'warning', active: 'success' }
  return types[profile.value.status] || 'info'
}

const getPlayerStatusText = () => {
  const texts = { idle: '空闲', in_service: '服务中', offline: '离线', pending: '待审核', active: '空闲' }
  return texts[profile.value.status] || profile.value.status || '-'
}

const getLevelType = () => {
  const types = {
    '机密娱乐': '',
    '绝密娱乐': 'info',
    '机密技术': 'warning',
    '机密金牌': 'danger',
    '机密巅峰': 'success'
  }
  return types[profile.value.level] || 'primary'
}

const getLevelTypeByName = (level) => {
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

const loadProfile = async () => {
  try {
    const res = await getProfile()
    profile.value = res.data || {}
    nicknameForm.nickname = profile.value.nickname || ''
  } catch (error) {
    console.error('加载个人信息失败', error)
  }
}

const loadUpgradeData = async () => {
  if (!isPlayer.value) return
  try {
    const [levelsRes, appsRes] = await Promise.all([
      getLevelPrices(),
      getMyLevelApplications()
    ])
    upgradeLevels.value = (levelsRes.data || []).map(item => ({
      name: item.level,
      price: item.defaultPrice
    }))
    upgradeApplications.value = appsRes.data || []
  } catch (error) {
    console.error('加载升级数据失败', error)
  }
}

const handleApplyUpgrade = async () => {
  const valid = await upgradeFormRef.value?.validate().catch(() => false)
  if (!valid) return

  upgradeLoading.value = true
  try {
    await applyLevelUpgrade({
      requestedLevel: upgradeForm.requestedLevel,
      reason: upgradeForm.reason
    })
    ElMessage.success('升级申请已提交')
    showUpgradeDialog.value = false
    upgradeForm.requestedLevel = ''
    upgradeForm.reason = ''
    await loadUpgradeData()
  } catch (error) {
  } finally {
    upgradeLoading.value = false
  }
}

const handleUpdateNickname = async () => {
  const valid = await nicknameFormRef.value?.validate().catch(() => false)
  if (!valid) return

  nicknameLoading.value = true
  try {
    await updateNickname({ nickname: nicknameForm.nickname })
    ElMessage.success('昵称修改成功')
    showNicknameDialog.value = false
    await loadProfile()
    await userStore.fetchUserInfo()
  } catch (error) {
    console.error('修改昵称失败:', error)
  } finally {
    nicknameLoading.value = false
  }
}

const handleChangePassword = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (error) {
  } finally {
    passwordLoading.value = false
  }
}

const loadCsAccounts = async () => {
  if (!isAdmin.value) return
  csLoading.value = true
  try {
    const res = await getCustomerServiceList()
    csAccounts.value = res.data || []
  } catch (error) {
    console.error('加载客服列表失败', error)
  } finally {
    csLoading.value = false
  }
}

const handleCreateCs = async () => {
  const valid = await csFormRef.value?.validate().catch(() => false)
  if (!valid) return

  csCreating.value = true
  try {
    await createCustomerService({
      phone: csForm.phone,
      nickname: csForm.nickname,
      password: csForm.password
    })
    ElMessage.success('客服账号创建成功')
    showCreateCsDialog.value = false
    csForm.phone = ''
    csForm.nickname = ''
    csForm.password = ''
    await loadCsAccounts()
  } catch (error) {
    console.error('创建客服失败', error)
  } finally {
    csCreating.value = false
  }
}

const handleToggleCsStatus = async (row) => {
  const newStatus = row.enabled !== false ? false : true
  const actionText = newStatus ? '启用' : '禁用'
  try {
    await updateCustomerService(row.id, { enabled: newStatus })
    ElMessage.success(`${actionText}成功`)
    await loadCsAccounts()
  } catch (error) {
    console.error(`${actionText}客服失败`, error)
  }
}

const handleEditCs = (row) => {
  currentCsAccount.value = row
  editCsForm.id = row.id
  editCsForm.phone = row.phone
  editCsForm.nickname = row.nickname
  showEditCsDialog.value = true
}

const submitEditCs = async () => {
  const valid = await editCsFormRef.value?.validate().catch(() => false)
  if (!valid) return

  csEditing.value = true
  try {
    await updateCustomerService(editCsForm.id, {
      nickname: editCsForm.nickname
    })
    ElMessage.success('客服信息更新成功')
    showEditCsDialog.value = false
    await loadCsAccounts()
  } catch (error) {
    console.error('更新客服失败', error)
  } finally {
    csEditing.value = false
  }
}

const handleDeleteCs = (row) => {
  ElMessageBox.confirm(
    `确定要删除客服 "${row.nickname}" 吗？此操作不可恢复！`,
    '确认删除',
    {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    csDeleting.value = true
    try {
      await deleteCustomerService(row.id)
      ElMessage.success('客服账号已删除')
      await loadCsAccounts()
    } catch (error) {
      console.error('删除客服失败', error)
    } finally {
      csDeleting.value = false
    }
  }).catch(() => {})
}

const handleResetCsPassword = (row) => {
  currentCsAccount.value = row
  resetCsPwdForm.password = ''
  showResetCsPwdDialog.value = true
}

const submitResetCsPassword = async () => {
  const valid = await resetCsPwdFormRef.value?.validate().catch(() => false)
  if (!valid) return

  csResetting.value = true
  try {
    await resetCustomerServicePassword(currentCsAccount.value.id, {
      password: resetCsPwdForm.password
    })
    ElMessage.success('密码重置成功')
    showResetCsPwdDialog.value = false
    resetCsPwdForm.password = ''
  } catch (error) {
    console.error('重置密码失败', error)
  } finally {
    csResetting.value = false
  }
}

onMounted(() => {
  loadProfile()
  loadUpgradeData()
  loadCsAccounts()
})
</script>

<style scoped lang="scss">
.profile-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 8px;
}

.top-section {
  margin-bottom: 8px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
  color: #2d2d4a;
}

.info-card,
.password-card,
.player-card {
  border-radius: 16px;
  border: none;
  background: #fff;
  transition: all 0.3s ease;

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid #f0f0f5;
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.info-card {
  height: 100%;
}

.info-body {
  .avatar-section {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 24px;
    padding-bottom: 20px;
    border-bottom: 1px solid #f5f5f8;

    .user-avatar {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      font-size: 28px;
      font-weight: 600;
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
    }

    .avatar-info {
      h3 {
        margin: 0 0 8px 0;
        font-size: 20px;
        color: #2d2d4a;
        font-weight: 600;
      }
    }
  }

  .info-list {
    .info-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 0;
      border-bottom: 1px solid #f8f8fb;

      &:last-child {
        border-bottom: none;
      }

      .info-label {
        color: #8a8ab0;
        font-size: 14px;
        flex-shrink: 0;
        width: 70px;
      }

      .info-value {
        color: #2d2d4a;
        font-size: 14px;
        font-weight: 500;

        &.editable {
          display: flex;
          align-items: center;
          gap: 8px;
        }
      }
    }
  }
}

.password-body {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 280px;
}

.password-form {
  :deep(.el-input__wrapper) {
    border-radius: 10px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    padding: 4px 12px;
  }

  :deep(.el-input__inner) {
    height: 44px;
  }

  .submit-btn {
    width: 100%;
    height: 44px;
    border-radius: 10px;
    font-size: 15px;
    font-weight: 500;
    margin-top: 8px;
  }
}

.player-card {
  margin-top: 16px;

  :deep(.el-card__body) {
    padding: 32px 24px;
  }
}

.player-stats {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0;
  overflow-x: auto;
  padding: 4px 0;

  .stat-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 0 16px;
    flex: 1;
    min-width: 140px;
    justify-content: flex-start;

    .stat-icon {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 18px;
      flex-shrink: 0;
    }

    .stat-content {
      min-width: 0;

      .stat-label {
        font-size: 12px;
        color: #8a8ab0;
        margin-bottom: 4px;
        white-space: nowrap;
      }

      .stat-value {
        font-size: 14px;
        font-weight: 600;
        color: #2d2d4a;
        white-space: nowrap;

        &.mono {
          font-family: 'SF Mono', monospace;
          color: #667eea;
        }

        &.price {
          color: #667eea;
        }

        &.income {
          color: #f0c27f;
        }

        .el-tag {
          white-space: nowrap;
        }

        .el-button {
          white-space: nowrap;
        }
      }
    }
  }

  .stat-divider {
    width: 1px;
    height: 50px;
    background: linear-gradient(180deg, transparent 0%, #e8e8f0 50%, transparent 100%);
    flex-shrink: 0;
  }

  .stat-item.upgrade-action {
    transition: all 0.2s ease;
    cursor: pointer;

    &:hover {
      .stat-icon.upgrade-icon {
        box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
      }
    }

    .stat-icon.upgrade-icon {
      background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
    }

    .stat-label {
      color: #67c23a;
    }

    .stat-value {
      .el-button {
        font-weight: 600;
        color: #67c23a;
        padding: 0;
        height: auto;

        &:hover {
          color: #529b2e;
        }
      }
    }
  }
}

.upgrade-history-card {
  margin-top: 16px;

  :deep(.el-card__header) {
    padding: 16px 24px;
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}

.upgrade-timeline {
  position: relative;
  padding-left: 24px;

  &::before {
    content: '';
    position: absolute;
    left: 8px;
    top: 0;
    bottom: 0;
    width: 2px;
    background: linear-gradient(180deg, #e8e8f0, #f0f0f5);
  }

  .timeline-item {
    position: relative;
    padding: 0 0 24px 0;

    &:last-child {
      padding-bottom: 0;
    }

    .timeline-dot {
      position: absolute;
      left: -20px;
      top: 4px;
      width: 14px;
      height: 14px;
      border-radius: 50%;
      border: 2px solid #e8e8f0;
      background: #fff;
      z-index: 1;

      &.pending {
        border-color: #e6a23c;
        background: #fdf6ec;
        box-shadow: 0 0 0 3px rgba(230, 162, 60, 0.15);
      }

      &.approved {
        border-color: #67c23a;
        background: #f0f9eb;
        box-shadow: 0 0 0 3px rgba(103, 194, 58, 0.15);
      }

      &.rejected {
        border-color: #f56c6c;
        background: #fef0f0;
        box-shadow: 0 0 0 3px rgba(245, 108, 108, 0.15);
      }
    }

    .timeline-content {
      background: #fafbfc;
      border-radius: 10px;
      padding: 14px 16px;
      border: 1px solid #f0f0f5;
      transition: all 0.2s;

      &:hover {
        border-color: #e0e0e8;
        background: #fff;
      }

      .timeline-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 8px;

        .level-change {
          display: flex;
          align-items: center;
          gap: 8px;

          .level-arrow {
            color: #c0c4cc;
            font-size: 16px;
            font-weight: 600;
          }
        }
      }

      .timeline-meta {
        display: flex;
        align-items: center;
        gap: 16px;
        margin-bottom: 6px;

        .meta-time {
          font-size: 12px;
          color: #8a8ab0;
        }

        .meta-reviewer {
          font-size: 12px;
          color: #a0a0b8;
        }
      }

      .timeline-reason {
        font-size: 13px;
        color: #606266;
        margin-top: 6px;
        padding: 6px 10px;
        background: #f5f5f8;
        border-radius: 6px;
        display: flex;
        align-items: flex-start;
        gap: 6px;

        .el-icon {
          margin-top: 2px;
          color: #8a8ab0;
        }
      }

      .timeline-comment {
        font-size: 13px;
        color: #e6a23c;
        margin-top: 6px;
        padding: 6px 10px;
        background: #fdf6ec;
        border-radius: 6px;
      }
    }
  }
}

.pending-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #e6a23c;
  font-size: 13px;
  padding: 10px;
  background: #fdf6ec;
  border-radius: 8px;
  margin-top: 8px;
}

// 客服管理样式
.cs-management-card {
  margin-top: 16px;
  border-radius: 16px;

  :deep(.el-card__header) {
    padding: 18px 24px;
    background: linear-gradient(135deg, #f8f9fc 0%, #f0f2f8 100%);
    border-bottom: 1px solid rgba(102, 126, 234, 0.1);
  }

  :deep(.el-card__body) {
    padding: 0;
  }

  .header-with-action {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;

    span {
      font-size: 16px;
      font-weight: 600;
      color: #2d2d4a;
    }

    .el-button {
      border-radius: 8px;
      padding: 8px 16px;
      font-weight: 500;

      .el-icon {
        margin-right: 4px;
      }
    }
  }

  :deep(.el-table) {
    font-size: 14px;

    .el-table__header {
      th {
        background: #fafbfc;
        color: #606266;
        font-weight: 600;
        padding: 14px 16px;
      }
    }

    .el-table__row {
      td {
        padding: 12px 16px;
      }

      &:hover {
        background: #f8f9fc;
      }
    }
  }

  .cs-nickname {
    display: flex;
    align-items: center;
    gap: 10px;

    .cs-avatar {
      background: linear-gradient(135deg, #e6a23c 0%, #f0c27f 100%);
      color: #fff;
      font-size: 12px;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba(230, 162, 60, 0.3);
    }

    span {
      font-weight: 500;
      color: #303133;
    }
  }

  .cs-phone {
    font-family: 'SF Mono', monospace;
    color: #606266;
    font-size: 13px;
  }

  .cs-time {
    color: #909399;
    font-size: 13px;
  }

  .cs-actions {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0;

    .el-button {
      padding: 6px;
      font-size: 16px;

      .el-icon {
        font-size: 16px;
      }

      &:hover {
        transform: scale(1.1);
      }
    }

    .el-divider {
      margin: 0 4px;
      height: 16px;
    }
  }

  :deep(.el-empty) {
    padding: 40px 0;

    .el-empty__description {
      color: #909399;
      font-size: 14px;
    }
  }
}

.reset-cs-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 8px;

  .cs-avatar {
    background: linear-gradient(135deg, #e6a23c 0%, #f0c27f 100%);
    color: #fff;
    font-size: 14px;
    font-weight: 600;
  }

  span {
    font-weight: 500;
    color: #303133;
  }
}

@media (max-width: 768px) {
  .profile-page {
    padding: 12px;
  }

  .top-section {
    .el-col {
      margin-bottom: 16px;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }

  .info-card,
  .password-card,
  .player-card {
    border-radius: 12px;

    :deep(.el-card__header) {
      padding: 16px 20px;
    }

    :deep(.el-card__body) {
      padding: 20px;
    }
  }

  .info-body {
    .avatar-section {
      margin-bottom: 20px;
      padding-bottom: 16px;

      .user-avatar {
        width: 60px;
        height: 60px;
        font-size: 24px;
      }

      .avatar-info {
        h3 {
          font-size: 18px;
        }
      }
    }

    .info-list {
      .info-item {
        padding: 12px 0;

        .info-label {
          width: 60px;
          font-size: 13px;
        }

        .info-value {
          font-size: 13px;
        }
      }
    }
  }

  .player-stats {
    flex-wrap: wrap;
    gap: 12px;

    .stat-item {
      flex: 0 0 calc(33.333% - 8px);
      min-width: auto;
      padding: 0;
      justify-content: flex-start;
      gap: 8px;

      .stat-icon {
        width: 32px;
        height: 32px;
        font-size: 14px;
      }

      .stat-content {
        .stat-label {
          font-size: 10px;
          margin-bottom: 2px;
        }

        .stat-value {
          font-size: 12px;

          .el-tag {
            font-size: 11px;
            padding: 0 6px;
            height: 20px;
          }

          .el-button {
            font-size: 11px;
          }
        }
      }
    }

    .stat-divider {
      display: none;
    }
  }

  .password-body {
    min-height: auto;
  }

  .password-form {
    :deep(.el-input__inner) {
      height: 40px;
    }

    .submit-btn {
      height: 40px;
    }
  }

  .upgrade-history-card {
    :deep(.el-card__header) {
      padding: 16px 20px;
    }
  }

  .upgrade-timeline {
    padding-left: 20px;

    &::before {
      left: 6px;
    }

    .timeline-item {
      padding-bottom: 20px;

      .timeline-dot {
        left: -16px;
        width: 12px;
        height: 12px;
      }

      .timeline-content {
        padding: 12px 14px;

        .timeline-header {
          flex-direction: column;
          align-items: flex-start;
          gap: 8px;
          margin-bottom: 6px;

          .level-change {
            .el-tag {
              font-size: 11px;
              height: 20px;
            }

            .level-arrow {
              font-size: 14px;
            }
          }
        }

        .timeline-meta {
          flex-wrap: wrap;
          gap: 8px;

          .meta-time,
          .meta-reviewer {
            font-size: 11px;
          }
        }

        .timeline-reason,
        .timeline-comment {
          font-size: 12px;
          padding: 5px 8px;
        }
      }
    }
  }
}

@media (max-width: 480px) {
  .profile-page {
    padding: 8px;
  }

  .top-section {
    .el-col {
      margin-bottom: 12px;
    }
  }

  .info-card,
  .password-card,
  .player-card {
    border-radius: 10px;

    :deep(.el-card__header) {
      padding: 14px 16px;
    }

    :deep(.el-card__body) {
      padding: 16px;
    }
  }

  .info-body {
    .avatar-section {
      gap: 12px;
      margin-bottom: 16px;
      padding-bottom: 14px;

      .user-avatar {
        width: 48px;
        height: 48px;
        font-size: 20px;
      }

      .avatar-info {
        h3 {
          font-size: 16px;
          margin-bottom: 4px;
        }
      }
    }

    .info-list {
      .info-item {
        padding: 10px 0;

        .info-label {
          width: 56px;
          font-size: 12px;
        }

        .info-value {
          font-size: 12px;
        }
      }
    }
  }

  .player-stats {
    gap: 10px;

    .stat-item {
      flex: 0 0 calc(50% - 5px);
      gap: 6px;

      .stat-icon {
        width: 28px;
        height: 28px;
        border-radius: 8px;
        font-size: 13px;
      }

      .stat-content {
        .stat-label {
          font-size: 10px;
          margin-bottom: 1px;
        }

        .stat-value {
          font-size: 11px;

          .el-tag {
            font-size: 10px;
            padding: 0 4px;
            height: 18px;
          }

          .el-button {
            font-size: 10px;
          }
        }
      }
    }
  }

  .password-body {
    min-height: auto;
  }

  .password-form {
    :deep(.el-input__wrapper) {
      padding: 2px 8px;
    }

    :deep(.el-input__inner) {
      height: 36px;
      font-size: 13px;
    }

    .submit-btn {
      height: 36px;
      font-size: 13px;
    }
  }

  .upgrade-timeline {
    padding-left: 16px;

    &::before {
      left: 4px;
    }

    .timeline-item {
      padding-bottom: 16px;

      .timeline-dot {
        left: -14px;
        width: 10px;
        height: 10px;
      }

      .timeline-content {
        padding: 10px 12px;
        border-radius: 8px;

        .timeline-header {
          .level-change {
            gap: 4px;

            .el-tag {
              font-size: 10px;
              height: 18px;
              padding: 0 4px;
            }
          }
        }

        .timeline-meta {
          .meta-time,
          .meta-reviewer {
            font-size: 10px;
          }
        }

        .timeline-reason,
        .timeline-comment {
          font-size: 11px;
          padding: 4px 6px;
        }
      }
    }
  }

  .card-header {
    font-size: 14px;
  }

  .pending-tip {
    font-size: 12px;
    padding: 8px;
  }
}
</style>
