<template>
  <div class="settings-page">
    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon class="title-icon"><Setting /></el-icon>
            <span class="title-text">系统设置</span>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="settings-tabs">
        <el-tab-pane label="平台费率" name="fee">
          <div class="tab-content">
            <el-form :model="configForm" label-width="140px" class="config-form">
              <el-form-item label="平台抽成比例">
                <el-input-number
                  v-model="configForm.platform_fee_rate"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  size="large"
                />
                <span class="unit-label">%</span>
                <div class="form-tip">陪玩师收入 = 订单金额 × (1 - 平台抽成比例)</div>
              </el-form-item>
              <el-divider />
              <h4 class="section-title">订单自动清理</h4>
              <el-form-item label="启用自动清理">
                <el-switch
                  v-model="configForm.order_cleanup_enabled"
                  active-text="启用"
                  inactive-text="禁用"
                />
                <div class="form-tip">启用后，系统将每天凌晨3点自动清理过期的已完成/已取消订单</div>
              </el-form-item>
              <el-form-item label="清理周期">
                <el-input-number
                  v-model="configForm.order_cleanup_days"
                  :min="1"
                  :max="365"
                  :step="1"
                  :disabled="!configForm.order_cleanup_enabled"
                  size="large"
                />
                <span class="unit-label">天前的订单</span>
                <div class="form-tip">将清理指定天数前的已完成和已取消订单</div>
              </el-form-item>
              <el-form-item label="清除陪玩师收入">
                <el-switch
                  v-model="configForm.clear_player_income"
                  :disabled="!configForm.order_cleanup_enabled"
                  active-text="启用"
                  inactive-text="禁用"
                />
                <div class="form-tip danger">启用后，清理订单时将同时清零所有陪玩师的累计收入和可提现余额，此操作不可恢复！</div>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="large" @click="saveConfig" :loading="saving">
                  <el-icon><Check /></el-icon>
                  保存设置
                </el-button>
              </el-form-item>
              <el-divider />
              <h4 class="section-title">手动清理订单</h4>
              <el-form-item label="截止日期">
                <el-date-picker
                  v-model="manualCleanupForm.cutoffDate"
                  type="date"
                  placeholder="选择截止日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
                <div class="form-tip">将清理此日期及之前创建的所有订单（包含当天）</div>
              </el-form-item>
              <el-form-item label="清理选项">
                <el-checkbox v-model="manualCleanupForm.cleanupCompleted">清理已完成订单</el-checkbox>
                <el-checkbox v-model="manualCleanupForm.cleanupCancelled">清理已取消订单</el-checkbox>
              </el-form-item>
              <el-form-item label="清除收入">
                <el-switch
                  v-model="manualCleanupForm.clearPlayerIncome"
                  active-text="是"
                  inactive-text="否"
                />
                <div class="form-tip danger">启用后，清理订单时将同时清零所有陪玩师的累计收入和可提现余额</div>
              </el-form-item>
              <el-form-item label="手动清理">
                <el-button 
                  type="danger" 
                  plain
                  :disabled="!canManualCleanup"
                  :loading="cleanupLoading"
                  @click="handleManualCleanup"
                >
                  <el-icon><Delete /></el-icon>
                  立即清理
                </el-button>
                <div class="form-tip danger">手动清理将立即删除所有符合条件的订单{{ manualCleanupForm.clearPlayerIncome ? '，并清零所有陪玩师的累计收入' : '' }}，此操作不可恢复！</div>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="等级管理" name="level">
          <div class="tab-content">
            <div class="level-header">
              <el-button type="primary" @click="showCreateLevelDialog">
                <el-icon><Plus /></el-icon>
                新增等级
              </el-button>
              <span class="level-tip">配置陪玩师等级类型及默认单价，可通过上下移动调整排序</span>
            </div>
            
            <el-table :data="levelPrices" border stripe class="level-table" v-loading="levelLoading" row-key="id">
              <el-table-column type="index" label="排序" width="70" align="center">
                <template #default="{ $index }">
                  <span class="sort-number">{{ $index + 1 }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="level" label="等级名称" width="150">
                <template #default="{ row }">
                  <el-tag :type="getLevelTagType(row.level)" size="large" effect="light">
                    {{ row.level }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="defaultPrice" label="默认单价" width="150">
                <template #default="{ row }">
                  <el-input-number 
                    v-model="row.defaultPrice" 
                    :min="1" 
                    :max="1000" 
                    :precision="2"
                    size="default"
                  />
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="160">
                <template #default="{ row }">
                  {{ formatDate(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="排序调整" width="140" align="center">
                <template #default="{ $index }">
                  <div class="sort-actions">
                    <el-button 
                      type="primary" 
                      circle 
                      size="small"
                      :disabled="$index === 0"
                      @click="moveLevelUp($index)"
                      title="上移"
                    >
                      <el-icon><ArrowUp /></el-icon>
                    </el-button>
                    <el-button 
                      type="primary" 
                      circle 
                      size="small"
                      :disabled="$index === levelPrices.length - 1"
                      @click="moveLevelDown($index)"
                      title="下移"
                    >
                      <el-icon><ArrowDown /></el-icon>
                    </el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <div class="action-group">
                    <el-button type="primary" size="small" @click="saveLevelPrice(row)">
                      <el-icon><Check /></el-icon>
                      保存
                    </el-button>
                    <el-button type="danger" size="small" @click="handleDeleteLevel(row)">
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <div class="sort-save-row" v-if="levelPrices.length > 0">
              <el-button type="success" @click="saveLevelSortOrder" :loading="levelSortSaving">
                <el-icon><Sort /></el-icon>
                保存当前排序
              </el-button>
              <span class="sort-tip">调整排序后，请点击保存当前排序</span>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="注意事项" name="precaution">
          <div class="tab-content">
            <div class="level-header">
              <el-button type="primary" @click="showCreatePrecautionDialog">
                <el-icon><Plus /></el-icon>
                新增注意事项
              </el-button>
              <span class="level-tip">配置陪玩单的注意事项选项，创建订单时可选择</span>
            </div>
            
            <el-table :data="precautions" border stripe class="level-table" v-loading="precautionLoading" row-key="id">
              <el-table-column type="index" label="排序" width="70" align="center">
                <template #default="{ $index }">
                  <span class="sort-number">{{ $index + 1 }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="value" label="选项内容" min-width="200">
                <template #default="{ row }">
                  <el-tag type="warning" size="large" effect="light">
                    {{ row.value }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="说明" min-width="200">
                <template #default="{ row }">
                  <el-input v-model="row.description" placeholder="可选说明" />
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="160">
                <template #default="{ row }">
                  {{ formatDate(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="排序调整" width="140" align="center">
                <template #default="{ $index }">
                  <div class="sort-actions">
                    <el-button 
                      type="primary" 
                      circle 
                      size="small"
                      :disabled="$index === 0"
                      @click="movePrecautionUp($index)"
                      title="上移"
                    >
                      <el-icon><ArrowUp /></el-icon>
                    </el-button>
                    <el-button 
                      type="primary" 
                      circle 
                      size="small"
                      :disabled="$index === precautions.length - 1"
                      @click="movePrecautionDown($index)"
                      title="下移"
                    >
                      <el-icon><ArrowDown /></el-icon>
                    </el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <div class="action-group">
                    <el-button type="primary" size="small" @click="savePrecaution(row)">
                      <el-icon><Check /></el-icon>
                      保存
                    </el-button>
                    <el-button type="danger" size="small" @click="handleDeletePrecaution(row)">
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <div class="sort-save-row" v-if="precautions.length > 0">
              <el-button type="success" @click="savePrecautionSortOrder" :loading="precautionSortSaving">
                <el-icon><Sort /></el-icon>
                保存当前排序
              </el-button>
              <span class="sort-tip">调整排序后，请点击保存当前排序</span>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="服务项目" name="serviceItem">
          <div class="tab-content">
            <div class="level-header">
              <el-button type="primary" @click="showCreateServiceItemDialog">
                <el-icon><Plus /></el-icon>
                新增服务项目
              </el-button>
              <span class="level-tip">配置护航单的服务项目选项，创建订单时可选择</span>
            </div>
            
            <el-table :data="serviceItems" border stripe class="level-table" v-loading="serviceItemLoading" row-key="id">
              <el-table-column type="index" label="排序" width="70" align="center">
                <template #default="{ $index }">
                  <span class="sort-number">{{ $index + 1 }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="value" label="选项内容" min-width="200">
                <template #default="{ row }">
                  <el-tag type="success" size="large" effect="light">
                    {{ row.value }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="说明" min-width="200">
                <template #default="{ row }">
                  <el-input v-model="row.description" placeholder="可选说明" />
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="160">
                <template #default="{ row }">
                  {{ formatDate(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column label="排序调整" width="140" align="center">
                <template #default="{ $index }">
                  <div class="sort-actions">
                    <el-button 
                      type="primary" 
                      circle 
                      size="small"
                      :disabled="$index === 0"
                      @click="moveServiceItemUp($index)"
                      title="上移"
                    >
                      <el-icon><ArrowUp /></el-icon>
                    </el-button>
                    <el-button 
                      type="primary" 
                      circle 
                      size="small"
                      :disabled="$index === serviceItems.length - 1"
                      @click="moveServiceItemDown($index)"
                      title="下移"
                    >
                      <el-icon><ArrowDown /></el-icon>
                    </el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <div class="action-group">
                    <el-button type="primary" size="small" @click="saveServiceItem(row)">
                      <el-icon><Check /></el-icon>
                      保存
                    </el-button>
                    <el-button type="danger" size="small" @click="handleDeleteServiceItem(row)">
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>

            <div class="sort-save-row" v-if="serviceItems.length > 0">
              <el-button type="success" @click="saveServiceItemSortOrder" :loading="serviceItemSortSaving">
                <el-icon><Sort /></el-icon>
                保存当前排序
              </el-button>
              <span class="sort-tip">调整排序后，请点击保存当前排序</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 创建等级对话框 -->
    <el-dialog v-model="createLevelDialogVisible" title="新增等级" width="450px" class="level-dialog">
      <el-form :model="createLevelForm" :rules="createLevelRules" ref="createLevelFormRef" label-width="100px">
        <el-form-item label="等级名称" prop="level">
          <el-input v-model="createLevelForm.level" placeholder="请输入等级名称，如：机密娱乐" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="默认单价" prop="defaultPrice">
          <el-input-number v-model="createLevelForm.defaultPrice" :min="1" :max="1000" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createLevelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateLevel" :loading="levelCreating">确定</el-button>
      </template>
    </el-dialog>

    <!-- 创建注意事项对话框 -->
    <el-dialog v-model="createPrecautionDialogVisible" title="新增注意事项" width="450px" class="level-dialog">
      <el-form :model="createPrecautionForm" :rules="createPrecautionRules" ref="createPrecautionFormRef" label-width="100px">
        <el-form-item label="选项内容" prop="value">
          <el-input v-model="createPrecautionForm.value" placeholder="请输入注意事项内容，如：要求开麦" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="createPrecautionForm.description" type="textarea" :rows="3" placeholder="可选说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createPrecautionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreatePrecaution" :loading="precautionCreating">确定</el-button>
      </template>
    </el-dialog>

    <!-- 创建服务项目对话框 -->
    <el-dialog v-model="createServiceItemDialogVisible" title="新增服务项目" width="450px" class="level-dialog">
      <el-form :model="createServiceItemForm" :rules="createServiceItemRules" ref="createServiceItemFormRef" label-width="100px">
        <el-form-item label="选项内容" prop="value">
          <el-input v-model="createServiceItemForm.value" placeholder="请输入服务项目内容，如：普通护航" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="createServiceItemForm.description" type="textarea" :rows="3" placeholder="可选说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createServiceItemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreateServiceItem" :loading="serviceItemCreating">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getSystemConfig, 
  updateSystemConfig, 
  getLevelPrices, 
  updateLevelPrice, 
  createLevelPrice, 
  deleteLevelPrice, 
  sortLevelPrices,
  getSystemOptions,
  createSystemOption,
  updateSystemOption,
  deleteSystemOption,
  sortSystemOptions,
  cleanupOrders
} from '@/api/system'
import { Setting, Check, Plus, Delete, ArrowUp, ArrowDown, Sort } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const activeTab = ref('fee')

// ==================== 平台费率 ====================
const saving = ref(false)
const cleanupLoading = ref(false)
const configForm = reactive({
  platform_fee_rate: 0.2,
  order_cleanup_days: 30,
  order_cleanup_enabled: false,
  clear_player_income: false
})

// 手动清理表单
const manualCleanupForm = reactive({
  cutoffDate: null,
  cleanupCompleted: true,
  cleanupCancelled: true,
  clearPlayerIncome: false
})

// 是否可以手动清理
const canManualCleanup = computed(() => {
  return manualCleanupForm.cutoffDate && 
         (manualCleanupForm.cleanupCompleted || manualCleanupForm.cleanupCancelled)
})

// ==================== 等级管理 ====================
const levelLoading = ref(false)
const levelCreating = ref(false)
const levelSortSaving = ref(false)
const createLevelDialogVisible = ref(false)
const createLevelFormRef = ref()
const levelPrices = ref([])

const createLevelForm = reactive({
  level: '',
  defaultPrice: 50
})

const createLevelRules = {
  level: [
    { required: true, message: '请输入等级名称', trigger: 'blur' },
    { min: 1, max: 20, message: '长度在 1 到 20 个字符', trigger: 'blur' }
  ],
  defaultPrice: [
    { required: true, message: '请输入默认单价', trigger: 'blur' }
  ]
}

// ==================== 注意事项管理 ====================
const precautionLoading = ref(false)
const precautionCreating = ref(false)
const precautionSortSaving = ref(false)
const createPrecautionDialogVisible = ref(false)
const createPrecautionFormRef = ref()
const precautions = ref([])

const createPrecautionForm = reactive({
  value: '',
  description: ''
})

const createPrecautionRules = {
  value: [
    { required: true, message: '请输入选项内容', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
  ]
}

// ==================== 服务项目管理 ====================
const serviceItemLoading = ref(false)
const serviceItemCreating = ref(false)
const serviceItemSortSaving = ref(false)
const createServiceItemDialogVisible = ref(false)
const createServiceItemFormRef = ref()
const serviceItems = ref([])

const createServiceItemForm = reactive({
  value: '',
  description: ''
})

const createServiceItemRules = {
  value: [
    { required: true, message: '请输入选项内容', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
  ]
}

// ==================== 通用方法 ====================
const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const getLevelTagType = (level) => {
  const types = {
    '机密娱乐': '',
    '绝密娱乐': 'info',
    '机密技术': 'warning',
    '机密金牌': 'danger',
    '机密巅峰': 'success'
  }
  return types[level] || 'primary'
}

// ==================== 平台费率方法 ====================
const loadConfig = async () => {
  try {
    const res = await getSystemConfig()
    configForm.platform_fee_rate = res.data.platformFeeRate
    configForm.order_cleanup_days = res.data.orderCleanupDays || 30
    configForm.order_cleanup_enabled = res.data.orderCleanupEnabled || false
    configForm.clear_player_income = res.data.clearPlayerIncome || false
  } catch (error) {
    ElMessage.error('加载配置失败')
  }
}

const saveConfig = async () => {
  saving.value = true
  try {
    await updateSystemConfig({
      platformFeeRate: configForm.platform_fee_rate,
      orderCleanupDays: configForm.order_cleanup_days,
      orderCleanupEnabled: configForm.order_cleanup_enabled,
      clearPlayerIncome: configForm.clear_player_income
    })
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleManualCleanup = async () => {
  try {
    const formattedDate = dayjs(manualCleanupForm.cutoffDate).format('YYYY-MM-DD HH:mm')
    const orderTypes = []
    if (manualCleanupForm.cleanupCompleted) orderTypes.push('已完成')
    if (manualCleanupForm.cleanupCancelled) orderTypes.push('已取消')
    
    const confirmMessage = manualCleanupForm.clearPlayerIncome
      ? `确定要清理 ${formattedDate} 之前的${orderTypes.join('、')}订单，并清零所有陪玩师的累计收入吗？此操作不可恢复！`
      : `确定要清理 ${formattedDate} 之前的${orderTypes.join('、')}订单吗？此操作不可恢复！`
    
    await ElMessageBox.confirm(
      confirmMessage,
      '确认清理',
      {
        confirmButtonText: '确定清理',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    cleanupLoading.value = true
    const res = await cleanupOrders({
      cutoffDate: manualCleanupForm.cutoffDate,
      cleanupCompleted: manualCleanupForm.cleanupCompleted,
      cleanupCancelled: manualCleanupForm.cleanupCancelled,
      clearPlayerIncome: manualCleanupForm.clearPlayerIncome
    })
    const data = res.data
    let message = `清理完成：订单 ${data.orderCount} 条，截图 ${data.screenshotCount || 0} 张，会话 ${data.sessionCount} 条，财务记录 ${data.financialCount} 条，操作日志 ${data.operationLogCount || 0} 条`
    if (manualCleanupForm.clearPlayerIncome && data.clearedPlayerCount > 0) {
      message += `，已清零 ${data.clearedPlayerCount} 位陪玩师的累计收入`
    }
    ElMessage.success(message)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '清理失败')
    }
  } finally {
    cleanupLoading.value = false
  }
}

// ==================== 等级管理方法 ====================
const loadLevelPrices = async () => {
  levelLoading.value = true
  try {
    const res = await getLevelPrices()
    levelPrices.value = res.data
  } catch (error) {
    ElMessage.error('加载等级价格失败')
  } finally {
    levelLoading.value = false
  }
}

const saveLevelPrice = async (row) => {
  try {
    await updateLevelPrice(row)
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const showCreateLevelDialog = () => {
  createLevelForm.level = ''
  createLevelForm.defaultPrice = 50
  createLevelDialogVisible.value = true
}

const submitCreateLevel = async () => {
  const valid = await createLevelFormRef.value?.validate().catch(() => false)
  if (!valid) return
  
  levelCreating.value = true
  try {
    await createLevelPrice(createLevelForm)
    ElMessage.success('等级创建成功')
    createLevelDialogVisible.value = false
    loadLevelPrices()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '创建失败')
  } finally {
    levelCreating.value = false
  }
}

const handleDeleteLevel = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除等级 "${row.level}" 吗？删除后该等级的陪玩师将需要重新设置等级。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteLevelPrice(row.id)
    ElMessage.success('删除成功')
    loadLevelPrices()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

const moveLevelUp = (index) => {
  if (index === 0) return
  const temp = levelPrices.value[index]
  levelPrices.value[index] = levelPrices.value[index - 1]
  levelPrices.value[index - 1] = temp
}

const moveLevelDown = (index) => {
  if (index === levelPrices.value.length - 1) return
  const temp = levelPrices.value[index]
  levelPrices.value[index] = levelPrices.value[index + 1]
  levelPrices.value[index + 1] = temp
}

const saveLevelSortOrder = async () => {
  levelSortSaving.value = true
  try {
    await sortLevelPrices(levelPrices.value)
    ElMessage.success('排序保存成功')
    loadLevelPrices()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '排序保存失败')
  } finally {
    levelSortSaving.value = false
  }
}

// ==================== 注意事项管理方法 ====================
const loadPrecautions = async () => {
  precautionLoading.value = true
  try {
    const res = await getSystemOptions('PRECAUTION')
    precautions.value = res.data
  } catch (error) {
    ElMessage.error('加载注意事项失败')
  } finally {
    precautionLoading.value = false
  }
}

const savePrecaution = async (row) => {
  try {
    await updateSystemOption(row.id, row)
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  }
}

const showCreatePrecautionDialog = () => {
  createPrecautionForm.value = ''
  createPrecautionForm.description = ''
  createPrecautionDialogVisible.value = true
}

const submitCreatePrecaution = async () => {
  const valid = await createPrecautionFormRef.value?.validate().catch(() => false)
  if (!valid) return
  
  precautionCreating.value = true
  try {
    await createSystemOption({
      type: 'PRECAUTION',
      value: createPrecautionForm.value,
      description: createPrecautionForm.description
    })
    ElMessage.success('注意事项创建成功')
    createPrecautionDialogVisible.value = false
    loadPrecautions()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '创建失败')
  } finally {
    precautionCreating.value = false
  }
}

const handleDeletePrecaution = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除注意事项 "${row.value}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteSystemOption(row.id)
    ElMessage.success('删除成功')
    loadPrecautions()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

const movePrecautionUp = (index) => {
  if (index === 0) return
  const temp = precautions.value[index]
  precautions.value[index] = precautions.value[index - 1]
  precautions.value[index - 1] = temp
}

const movePrecautionDown = (index) => {
  if (index === precautions.value.length - 1) return
  const temp = precautions.value[index]
  precautions.value[index] = precautions.value[index + 1]
  precautions.value[index + 1] = temp
}

const savePrecautionSortOrder = async () => {
  precautionSortSaving.value = true
  try {
    await sortSystemOptions(precautions.value)
    ElMessage.success('排序保存成功')
    loadPrecautions()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '排序保存失败')
  } finally {
    precautionSortSaving.value = false
  }
}

// ==================== 服务项目管理方法 ====================
const loadServiceItems = async () => {
  serviceItemLoading.value = true
  try {
    const res = await getSystemOptions('SERVICE_ITEM')
    serviceItems.value = res.data
  } catch (error) {
    ElMessage.error('加载服务项目失败')
  } finally {
    serviceItemLoading.value = false
  }
}

const saveServiceItem = async (row) => {
  try {
    await updateSystemOption(row.id, row)
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  }
}

const showCreateServiceItemDialog = () => {
  createServiceItemForm.value = ''
  createServiceItemForm.description = ''
  createServiceItemDialogVisible.value = true
}

const submitCreateServiceItem = async () => {
  const valid = await createServiceItemFormRef.value?.validate().catch(() => false)
  if (!valid) return
  
  serviceItemCreating.value = true
  try {
    await createSystemOption({
      type: 'SERVICE_ITEM',
      value: createServiceItemForm.value,
      description: createServiceItemForm.description
    })
    ElMessage.success('服务项目创建成功')
    createServiceItemDialogVisible.value = false
    loadServiceItems()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '创建失败')
  } finally {
    serviceItemCreating.value = false
  }
}

const handleDeleteServiceItem = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除服务项目 "${row.value}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteSystemOption(row.id)
    ElMessage.success('删除成功')
    loadServiceItems()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

const moveServiceItemUp = (index) => {
  if (index === 0) return
  const temp = serviceItems.value[index]
  serviceItems.value[index] = serviceItems.value[index - 1]
  serviceItems.value[index - 1] = temp
}

const moveServiceItemDown = (index) => {
  if (index === serviceItems.value.length - 1) return
  const temp = serviceItems.value[index]
  serviceItems.value[index] = serviceItems.value[index + 1]
  serviceItems.value[index + 1] = temp
}

const saveServiceItemSortOrder = async () => {
  serviceItemSortSaving.value = true
  try {
    await sortSystemOptions(serviceItems.value)
    ElMessage.success('排序保存成功')
    loadServiceItems()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '排序保存失败')
  } finally {
    serviceItemSortSaving.value = false
  }
}

onMounted(() => {
  loadConfig()
  loadLevelPrices()
  loadPrecautions()
  loadServiceItems()
})
</script>

<style scoped lang="scss">
.settings-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.settings-card {
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

.settings-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 20px;
  }
  
  :deep(.el-tabs__item) {
    font-size: 15px;
    padding: 0 25px;
    height: 45px;
    line-height: 45px;
  }
}

.tab-content {
  padding: 10px;
}

.config-form {
  max-width: 500px;
  
  .section-title {
    margin: 0 0 20px 0;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }
  
  .unit-label {
    margin-left: 10px;
    font-size: 14px;
    color: #606266;
  }
  
  .form-tip {
    color: #909399;
    font-size: 13px;
    margin-top: 8px;
    
    &.danger {
      color: #f56c6c;
    }
  }
}

.level-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  
  .level-tip {
    color: #909399;
    font-size: 13px;
  }
}

.level-table {
  :deep(th) {
    background: #f5f7fa;
    font-weight: 600;
    color: #606266;
  }
  
  :deep(.el-tag) {
    font-size: 14px;
    padding: 6px 15px;
  }
  
  .sort-number {
    font-weight: 600;
    color: #409eff;
    font-size: 14px;
  }
}

.sort-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.action-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.sort-save-row {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
  
  .sort-tip {
    color: #909399;
    font-size: 13px;
  }
}

.level-dialog {
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
</style>
