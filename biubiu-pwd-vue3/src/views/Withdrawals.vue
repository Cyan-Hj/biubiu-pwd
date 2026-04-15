<template>
  <div class="withdrawals-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>提现审核</span>
          <div class="header-actions">
            <el-radio-group v-model="statusFilter" size="small" @change="handleFilterChange">
              <el-radio-button label="">全部</el-radio-button>
              <el-radio-button label="pending">待审核</el-radio-button>
              <el-radio-button label="approved">已通过</el-radio-button>
              <el-radio-button label="rejected">已拒绝</el-radio-button>
            </el-radio-group>
            <el-button type="success" @click="handleApproveAll" :disabled="pendingCount === 0">
              <el-icon><Check /></el-icon>
              一键全部通过 ({{ pendingCount }})
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredWithdrawals" v-loading="loading" border max-height="650">
        <el-table-column prop="playerNo" label="编号" width="90" />
        <el-table-column prop="playerNickname" label="昵称" width="120" />
        <el-table-column prop="playerPhone" label="手机号" width="130" />
        <el-table-column prop="amount" label="提现金额" width="110">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: bold;">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="提现方式" width="100">
          <template #default="{ row }">
            {{ getPaymentMethodText(row.paymentMethod) }}
          </template>
        </el-table-column>
        <el-table-column prop="accountInfo" label="收款账户" min-width="180" show-overflow-tooltip />
        <el-table-column prop="realName" label="真实姓名" width="100" />
        <el-table-column prop="createdAt" label="申请时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'pending'" type="warning" size="small">待审核</el-tag>
            <el-tag v-else-if="row.status === 'approved'" type="success" size="small">已通过</el-tag>
            <el-tag v-else-if="row.status === 'rejected'" type="danger" size="small">已拒绝</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button type="success" size="small" @click="handleApprove(row)">通过</el-button>
              <el-button type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
            </template>
            <template v-else>
              <span class="reviewed-info">
                {{ formatDate(row.reviewedAt) }}
              </span>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="rejectDialogVisible" title="拒绝提现" width="400px">
      <el-form label-width="100px">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectReason" type="textarea" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReject">确定拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllWithdrawals, approveAllWithdrawals, reviewWithdrawal } from '@/api/finance'
import dayjs from 'dayjs'

const loading = ref(false)
const withdrawals = ref([])
const statusFilter = ref('')
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentWithdrawal = ref(null)

const getPaymentMethodText = (method) => {
  const texts = { alipay: '支付宝', wechat: '微信', bank: '银行卡' }
  return texts[method] || method
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const filteredWithdrawals = computed(() => {
  if (!statusFilter.value) return withdrawals.value
  return withdrawals.value.filter(w => w.status === statusFilter.value)
})

const pendingCount = computed(() => {
  return withdrawals.value.filter(w => w.status === 'pending').length
})

const handleFilterChange = () => {}

const loadWithdrawals = async () => {
  loading.value = true
  try {
    const res = await getAllWithdrawals()
    withdrawals.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleApprove = async (row) => {
  try {
    await reviewWithdrawal(row.id, { status: 'approved' })
    ElMessage.success('审核通过')
    loadWithdrawals()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleApproveAll = async () => {
  try {
    await ElMessageBox.confirm(
      `确认一键通过所有 ${pendingCount.value} 条待审核的提现申请？`,
      '一键审核确认',
      { confirmButtonText: '确认全部通过', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await approveAllWithdrawals()
    ElMessage.success(res.message || '批量审核完成')
    loadWithdrawals()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleReject = (row) => {
  currentWithdrawal.value = row
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

const submitReject = async () => {
  if (!rejectReason.value) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  try {
    await reviewWithdrawal(currentWithdrawal.value.id, {
      status: 'rejected',
      rejectReason: rejectReason.value
    })
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    loadWithdrawals()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadWithdrawals()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.reviewed-info {
  font-size: 12px;
  color: #909399;
}
</style>
