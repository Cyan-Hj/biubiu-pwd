<template>
  <div class="boss-page">
    <el-card class="boss-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon class="title-icon"><User /></el-icon>
            <span class="title-text">老板预存管理</span>
          </div>
          <div class="header-actions">
            <el-button type="primary" @click="showCreateDialog">
              <el-icon><Plus /></el-icon>
              添加老板
            </el-button>
            <el-button type="info" @click="showVipConfigDialog">
              <el-icon><Setting /></el-icon>
              VIP等级配置
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选区域 -->
      <div class="filter-row">
        <el-input
          v-model="filter.keyword"
          placeholder="搜索姓名/联系方式"
          clearable
          class="filter-input"
          @keyup.enter="loadBosses"
        />
        <el-select v-model="filter.vipLevel" placeholder="全部等级" clearable class="filter-select">
          <el-option
            v-for="level in vipLevels"
            :key="level.level"
            :label="level.name"
            :value="level.level"
          />
        </el-select>
        <el-select v-model="filter.customerType" placeholder="全部分类" clearable class="filter-select">
          <el-option label="散客" value="SCATTER" />
          <el-option label="固定客" value="REGULAR" />
        </el-select>
        <el-select v-model="filter.enabled" placeholder="全部状态" clearable class="filter-select">
          <el-option label="启用" :value="true" />
          <el-option label="禁用" :value="false" />
        </el-select>
        <el-button type="primary" @click="loadBosses">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
      </div>

      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card primary">
            <div class="stat-icon"><el-icon><User /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">老板总数</div>
              <div class="stat-value">{{ stats.total }}</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card success">
            <div class="stat-icon"><el-icon><Money /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">总预存余额</div>
              <div class="stat-value">¥{{ formatNumber(stats.totalBalance) }}</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card warning">
            <div class="stat-icon"><el-icon><Wallet /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">累计充值</div>
              <div class="stat-value">¥{{ formatNumber(stats.totalRecharge) }}</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="12" :md="6">
          <div class="stat-card info">
            <div class="stat-icon"><el-icon><ShoppingCart /></el-icon></div>
            <div class="stat-content">
              <div class="stat-label">累计消费</div>
              <div class="stat-value">¥{{ formatNumber(stats.totalConsumption) }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 老板列表 -->
      <el-table :data="bosses" v-loading="loading" stripe class="boss-table">
        <el-table-column prop="name" label="姓名" width="120">
          <template #default="{ row }">
            <span class="boss-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="联系方式" width="180">
          <template #default="{ row }">
            <div class="contact-info">
              <el-tag size="small" :type="row.contactType === 'WECHAT' ? 'success' : 'primary'">
                {{ row.contactType === 'WECHAT' ? '微信' : 'QQ' }}
              </el-tag>
              <span class="contact-value">{{ row.contactValue }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="vipLevel" label="VIP等级" width="120">
          <template #default="{ row }">
            <el-tag :type="getVipTagType(row.vipLevel)" effect="dark" size="small">
              {{ getVipName(row.vipLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="customerType" label="客户分类" width="100">
          <template #default="{ row }">
            <el-tag :type="row.customerType === 'REGULAR' ? 'warning' : 'info'" size="small">
              {{ row.customerType === 'REGULAR' ? '固定客' : '散客' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="balance" label="预存余额" width="130">
          <template #default="{ row }">
            <span class="balance" :class="{ 'zero': row.balance <= 0 }">¥{{ formatNumber(row.balance) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalConsumption" label="累计消费" width="130">
          <template #default="{ row }">
            <span class="consumption">¥{{ formatNumber(row.totalConsumption) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button type="primary" size="small" @click="handleRecharge(row)">
                <el-icon><Money /></el-icon>充值
              </el-button>
              <el-button type="success" size="small" @click="handleEditBalance(row)">
                <el-icon><Edit /></el-icon>改余额
              </el-button>
              <el-button type="info" size="small" @click="handleRecords(row)">
                <el-icon><List /></el-icon>记录
              </el-button>
              <el-button type="warning" size="small" @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>编辑
              </el-button>
              <el-button
                v-if="row.enabled"
                type="danger"
                size="small"
                @click="handleDisable(row)"
              >
                <el-icon><CircleClose /></el-icon>禁用
              </el-button>
              <el-button
                v-else
                type="success"
                size="small"
                @click="handleEnable(row)"
              >
                <el-icon><CircleCheck /></el-icon>启用
              </el-button>
              <el-button type="danger" size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </div>
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
        @current-change="loadBosses"
        @size-change="loadBosses"
      />
    </el-card>

    <!-- 添加/编辑老板对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑老板' : '添加老板'"
      width="500px"
      class="boss-dialog"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入老板姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="联系方式" prop="contactType">
          <el-radio-group v-model="form.contactType">
            <el-radio-button label="WECHAT">微信</el-radio-button>
            <el-radio-button label="QQ">QQ</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="form.contactType === 'WECHAT' ? '微信号' : 'QQ号'" prop="contactValue">
          <el-input v-model="form.contactValue" :placeholder="`请输入${form.contactType === 'WECHAT' ? '微信号' : 'QQ号'}`" />
        </el-form-item>
        <el-form-item label="客户分类" prop="customerType">
          <el-radio-group v-model="form.customerType">
            <el-radio-button label="SCATTER">散客</el-radio-button>
            <el-radio-button label="REGULAR">固定客</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="VIP等级" prop="vipLevel">
          <el-select v-model="form.vipLevel" placeholder="请选择VIP等级" style="width: 100%">
            <el-option
              v-for="level in vipLevels"
              :key="level.level"
              :label="`${level.name} (${level.discountRate * 100}折)`"
              :value="level.level"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 充值对话框 -->
    <el-dialog v-model="rechargeDialogVisible" title="预存充值" width="400px" class="boss-dialog">
      <div class="recharge-info">
        <div class="info-item">
          <span class="label">老板姓名：</span>
          <span class="value">{{ currentBoss?.name }}</span>
        </div>
        <div class="info-item">
          <span class="label">当前余额：</span>
          <span class="value highlight">¥{{ formatNumber(currentBoss?.balance) }}</span>
        </div>
      </div>
      <el-form :model="rechargeForm" :rules="rechargeRules" ref="rechargeFormRef" label-width="100px">
        <el-form-item label="充值金额" prop="amount">
          <el-input-number v-model="rechargeForm.amount" :min="1" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="rechargeForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRecharge" :loading="rechargeLoading">确定充值</el-button>
      </template>
    </el-dialog>

    <!-- 手动修改余额对话框 -->
    <el-dialog v-model="editBalanceDialogVisible" title="手动修改预存金额" width="400px" class="boss-dialog">
      <div class="recharge-info">
        <div class="info-item">
          <span class="label">老板姓名：</span>
          <span class="value">{{ currentBoss?.name }}</span>
        </div>
        <div class="info-item">
          <span class="label">当前余额：</span>
          <span class="value highlight">¥{{ formatNumber(currentBoss?.balance) }}</span>
        </div>
      </div>
      <el-form :model="editBalanceForm" :rules="editBalanceRules" ref="editBalanceFormRef" label-width="100px">
        <el-form-item label="新余额" prop="newBalance">
          <el-input-number v-model="editBalanceForm.newBalance" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="调整说明" prop="remark">
          <el-input v-model="editBalanceForm.remark" type="textarea" :rows="2" placeholder="请输入调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editBalanceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEditBalance" :loading="editBalanceLoading">确定修改</el-button>
      </template>
    </el-dialog>

    <!-- 充值记录对话框 -->
    <el-dialog v-model="recordsDialogVisible" title="充值记录" width="700px" class="boss-dialog">
      <div class="records-header">
        <span class="boss-name">{{ currentBoss?.name }}</span>
        <span class="balance">当前余额：¥{{ formatNumber(currentBoss?.balance) }}</span>
      </div>
      <el-table :data="records" v-loading="recordsLoading" stripe>
        <el-table-column prop="createdAt" label="时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'RECHARGE' ? 'success' : 'danger'" size="small">
              {{ row.type === 'RECHARGE' ? '充值' : '扣减' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            <span :class="row.type === 'RECHARGE' ? 'recharge' : 'deduct'">
              {{ row.type === 'RECHARGE' ? '+' : '-' }}¥{{ formatNumber(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-model:current-page="recordsPage"
        v-model:page-size="recordsPageSize"
        :total="recordsTotal"
        :page-sizes="[10, 20]"
        layout="total, prev, pager, next"
        class="pagination"
        @current-change="loadRecords"
        @size-change="loadRecords"
      />
    </el-dialog>

    <!-- VIP等级配置对话框 -->
    <el-dialog v-model="vipConfigDialogVisible" title="VIP等级配置" width="800px" class="boss-dialog">
      <div class="vip-config-header">
        <el-button type="primary" @click="showAddVipDialog">
          <el-icon><Plus /></el-icon>
          新增等级
        </el-button>
      </div>
      <el-table :data="vipLevels" stripe>
        <el-table-column prop="level" label="等级" width="80" />
        <el-table-column prop="name" label="名称" width="120" />
        <el-table-column prop="discountRate" label="折扣" width="100">
          <template #default="{ row }">
            {{ row.discountRate * 100 }}%
          </template>
        </el-table-column>
        <el-table-column prop="upgradeConsumption" label="升级条件(消费)" width="150">
          <template #default="{ row }">
            {{ row.upgradeConsumption ? '¥' + formatNumber(row.upgradeConsumption) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="upgradeRecharge" label="升级条件(预存)" width="150">
          <template #default="{ row }">
            {{ row.upgradeRecharge ? '¥' + formatNumber(row.upgradeRecharge) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEditVip(row)">编辑</el-button>
            <el-button
              v-if="row.level !== 0"
              type="danger"
              size="small"
              @click="handleDeleteVip(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 新增/编辑VIP等级对话框 -->
    <el-dialog
      v-model="vipFormDialogVisible"
      :title="isEditVip ? '编辑VIP等级' : '新增VIP等级'"
      width="450px"
      class="boss-dialog"
    >
      <el-form :model="vipForm" :rules="vipRules" ref="vipFormRef" label-width="120px">
        <el-form-item label="等级" prop="level">
          <el-input-number v-model="vipForm.level" :min="0" :max="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="vipForm.name" placeholder="如：VIP1、金卡会员" />
        </el-form-item>
        <el-form-item label="折扣率" prop="discountRate">
          <el-input-number
            v-model="vipForm.discountRate"
            :min="0.1"
            :max="1"
            :step="0.05"
            :precision="2"
            style="width: 100%"
          />
          <div class="form-tip">0.85表示85折，1表示无折扣</div>
        </el-form-item>
        <el-form-item label="升级消费金额" prop="upgradeConsumption">
          <el-input-number
            v-model="vipForm.upgradeConsumption"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
          <div class="form-tip">累计消费达到此金额可升级（0表示不限制）</div>
        </el-form-item>
        <el-form-item label="升级预存金额" prop="upgradeRecharge">
          <el-input-number
            v-model="vipForm.upgradeRecharge"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
          <div class="form-tip">预存余额达到此金额可升级（0表示不限制）</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vipFormDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitVipForm" :loading="vipSubmitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import {
  User, Plus, Setting, Search, Money, List, Edit,
  CircleClose, CircleCheck, Wallet, ShoppingCart, Delete
} from '@element-plus/icons-vue'
import {
  getBosses, createBoss, updateBoss, disableBoss, enableBoss, deleteBoss,
  rechargeBoss, updateBossBalance, getBossRecords, getVipLevels, createVipLevel,
  updateVipLevel, deleteVipLevel
} from '@/api/boss'

// 数据
const loading = ref(false)
const bosses = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const vipLevels = ref([])

const filter = reactive({
  keyword: '',
  vipLevel: null,
  customerType: null,
  enabled: null
})

// 统计
const stats = computed(() => {
  const total = bosses.value.length
  const totalBalance = bosses.value.reduce((sum, b) => sum + (b.balance || 0), 0)
  const totalRecharge = bosses.value.reduce((sum, b) => sum + (b.totalRecharge || 0), 0)
  const totalConsumption = bosses.value.reduce((sum, b) => sum + (b.totalConsumption || 0), 0)
  return { total, totalBalance, totalRecharge, totalConsumption }
})

// 老板表单
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const submitting = ref(false)
const currentBossId = ref(null)

const form = reactive({
  name: '',
  contactType: 'WECHAT',
  contactValue: '',
  customerType: 'SCATTER',
  vipLevel: 0,
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  contactType: [{ required: true, message: '请选择联系方式', trigger: 'change' }],
  contactValue: [{ required: true, message: '请输入联系方式', trigger: 'blur' }],
  customerType: [{ required: true, message: '请选择客户分类', trigger: 'change' }],
  vipLevel: [{ required: true, message: '请选择VIP等级', trigger: 'change' }]
}

// 充值
const rechargeDialogVisible = ref(false)
const rechargeFormRef = ref()
const rechargeLoading = ref(false)
const currentBoss = ref(null)

const rechargeForm = reactive({
  amount: 100,
  remark: ''
})

const rechargeRules = {
  amount: [{ required: true, message: '请输入充值金额', trigger: 'blur' }]
}

// 手动修改余额
const editBalanceDialogVisible = ref(false)
const editBalanceFormRef = ref()
const editBalanceLoading = ref(false)

const editBalanceForm = reactive({
  newBalance: 0,
  remark: ''
})

const editBalanceRules = {
  newBalance: [{ required: true, message: '请输入新余额', trigger: 'blur' }],
  remark: [{ required: true, message: '请输入调整说明', trigger: 'blur' }]
}

// 记录
const recordsDialogVisible = ref(false)
const recordsLoading = ref(false)
const records = ref([])
const recordsPage = ref(1)
const recordsPageSize = ref(10)
const recordsTotal = ref(0)

// VIP配置
const vipConfigDialogVisible = ref(false)
const vipFormDialogVisible = ref(false)
const isEditVip = ref(false)
const vipFormRef = ref()
const vipSubmitting = ref(false)
const currentVipId = ref(null)

const vipForm = reactive({
  level: 1,
  name: '',
  discountRate: 1,
  upgradeConsumption: 0,
  upgradeRecharge: 0
})

const vipRules = {
  level: [{ required: true, message: '请输入等级', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  discountRate: [{ required: true, message: '请输入折扣率', trigger: 'blur' }]
}

// 加载数据
const loadBosses = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      keyword: filter.keyword || undefined,
      vipLevel: filter.vipLevel,
      customerType: filter.customerType,
      enabled: filter.enabled
    }
    const res = await getBosses(params)
    bosses.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const loadVipLevels = async () => {
  try {
    const res = await getVipLevels()
    vipLevels.value = res.data || []
  } catch (error) {
    console.error('加载VIP等级失败', error)
  }
}

// 工具函数
const formatNumber = (num) => {
  if (!num) return '0.00'
  return parseFloat(num).toFixed(2)
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const getVipName = (level) => {
  const vip = vipLevels.value.find(v => v.level === level)
  return vip ? vip.name : '普通'
}

const getVipTagType = (level) => {
  const types = ['info', 'success', 'warning', 'danger', 'primary']
  return types[level % types.length] || 'info'
}

// 老板操作
const showCreateDialog = () => {
  isEdit.value = false
  currentBossId.value = null
  form.name = ''
  form.contactType = 'WECHAT'
  form.contactValue = ''
  form.customerType = 'SCATTER'
  form.vipLevel = 0
  form.remark = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  currentBossId.value = row.id
  form.name = row.name
  form.contactType = row.contactType
  form.contactValue = row.contactValue
  form.customerType = row.customerType
  form.vipLevel = row.vipLevel
  form.remark = row.remark
  dialogVisible.value = true
}

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateBoss(currentBossId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createBoss(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadBosses()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleDisable = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要禁用老板 "${row.name}" 吗？`, '确认禁用', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await disableBoss(row.id)
    ElMessage.success('禁用成功')
    loadBosses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '禁用失败')
    }
  }
}

const handleEnable = async (row) => {
  try {
    await enableBoss(row.id)
    ElMessage.success('启用成功')
    loadBosses()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '启用失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除老板 "${row.name}" 吗？此操作不可恢复！`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteBoss(row.id)
    ElMessage.success('删除成功')
    loadBosses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

// 充值操作
const handleRecharge = (row) => {
  currentBoss.value = row
  rechargeForm.amount = 100
  rechargeForm.remark = ''
  rechargeDialogVisible.value = true
}

const submitRecharge = async () => {
  const valid = await rechargeFormRef.value?.validate().catch(() => false)
  if (!valid) return

  rechargeLoading.value = true
  try {
    await rechargeBoss(currentBoss.value.id, rechargeForm)
    ElMessage.success('充值成功')
    rechargeDialogVisible.value = false
    loadBosses()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '充值失败')
  } finally {
    rechargeLoading.value = false
  }
}

// 手动修改余额操作
const handleEditBalance = (row) => {
  currentBoss.value = row
  editBalanceForm.newBalance = row.balance || 0
  editBalanceForm.remark = ''
  editBalanceDialogVisible.value = true
}

const submitEditBalance = async () => {
  const valid = await editBalanceFormRef.value?.validate().catch(() => false)
  if (!valid) return

  editBalanceLoading.value = true
  try {
    await updateBossBalance(currentBoss.value.id, editBalanceForm)
    ElMessage.success('余额修改成功')
    editBalanceDialogVisible.value = false
    loadBosses()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '修改失败')
  } finally {
    editBalanceLoading.value = false
  }
}

// 记录操作
const handleRecords = (row) => {
  currentBoss.value = row
  recordsPage.value = 1
  recordsDialogVisible.value = true
  loadRecords()
}

const loadRecords = async () => {
  if (!currentBoss.value) return
  recordsLoading.value = true
  try {
    const res = await getBossRecords(currentBoss.value.id, {
      page: recordsPage.value,
      pageSize: recordsPageSize.value
    })
    records.value = res.data?.list || []
    recordsTotal.value = res.data?.total || 0
  } finally {
    recordsLoading.value = false
  }
}

// VIP配置操作
const showVipConfigDialog = () => {
  vipConfigDialogVisible.value = true
}

const showAddVipDialog = () => {
  isEditVip.value = false
  currentVipId.value = null
  vipForm.level = vipLevels.value.length
  vipForm.name = ''
  vipForm.discountRate = 1
  vipForm.upgradeConsumption = 0
  vipForm.upgradeRecharge = 0
  vipFormDialogVisible.value = true
}

const handleEditVip = (row) => {
  isEditVip.value = true
  currentVipId.value = row.id
  vipForm.level = row.level
  vipForm.name = row.name
  vipForm.discountRate = row.discountRate
  vipForm.upgradeConsumption = row.upgradeConsumption || 0
  vipForm.upgradeRecharge = row.upgradeRecharge || 0
  vipFormDialogVisible.value = true
}

const submitVipForm = async () => {
  const valid = await vipFormRef.value?.validate().catch(() => false)
  if (!valid) return

  vipSubmitting.value = true
  try {
    if (isEditVip.value) {
      await updateVipLevel(currentVipId.value, vipForm)
      ElMessage.success('更新成功')
    } else {
      await createVipLevel(vipForm)
      ElMessage.success('创建成功')
    }
    vipFormDialogVisible.value = false
    loadVipLevels()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    vipSubmitting.value = false
  }
}

const handleDeleteVip = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除VIP等级 "${row.name}" 吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteVipLevel(row.id)
    ElMessage.success('删除成功')
    loadVipLevels()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadBosses()
  loadVipLevels()
})
</script>

<style scoped lang="scss">
.boss-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.boss-card {
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

.header-actions {
  display: flex;
  gap: 10px;
}

.filter-row {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;

  .filter-input {
    width: 200px;
  }

  .filter-select {
    width: 150px;
  }
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 20px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  .stat-icon {
    width: 50px;
    height: 50px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
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
    font-size: 24px;
    font-weight: 700;
    color: #303133;
  }

  &.primary .stat-icon {
    background: #ecf5ff;
    color: #409eff;
  }

  &.success .stat-icon {
    background: #f0f9eb;
    color: #67c23a;
  }

  &.warning .stat-icon {
    background: #fdf6ec;
    color: #e6a23c;
  }

  &.info .stat-icon {
    background: #f4f4f5;
    color: #606266;
  }
}

.boss-table {
  :deep(th) {
    background: #f5f7fa;
    font-weight: 600;
    color: #606266;
  }

  .boss-name {
    font-weight: 600;
    color: #303133;
  }

  .contact-info {
    display: flex;
    align-items: center;
    gap: 8px;

    .contact-value {
      color: #606266;
      font-size: 13px;
    }
  }

  .balance {
    font-weight: 700;
    color: #67c23a;

    &.zero {
      color: #909399;
    }
  }

  .consumption {
    color: #f56c6c;
    font-weight: 500;
  }

  .action-group {
    display: flex;
    gap: 5px;
    flex-wrap: wrap;
  }
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.boss-dialog {
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

.recharge-info {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;

  .info-item {
    display: flex;
    justify-content: space-between;
    margin-bottom: 10px;

    &:last-child {
      margin-bottom: 0;
    }

    .label {
      color: #909399;
    }

    .value {
      font-weight: 600;
      color: #303133;

      &.highlight {
        color: #67c23a;
        font-size: 18px;
      }
    }
  }
}

.records-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding: 10px 15px;
  background: #f5f7fa;
  border-radius: 8px;

  .boss-name {
    font-weight: 600;
    font-size: 16px;
    color: #303133;
  }

  .balance {
    color: #67c23a;
    font-weight: 600;
  }
}

.recharge {
  color: #67c23a;
  font-weight: 600;
}

.deduct {
  color: #f56c6c;
  font-weight: 600;
}

.vip-config-header {
  margin-bottom: 15px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>
