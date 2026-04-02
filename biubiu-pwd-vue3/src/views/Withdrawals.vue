<template>
  <div class="withdrawals-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>提现审核</span>
        </div>
      </template>

      <el-table :data="withdrawals" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="player_nickname" label="申请人" />
        <el-table-column prop="amount" label="提现金额" width="120">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: bold;">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="payment_method" label="提现方式" width="100">
          <template #default="{ row }">
            {{ getPaymentMethodText(row.payment_method) }}
          </template>
        </el-table-column>
        <el-table-column prop="account_info" label="收款账号" />
        <el-table-column prop="real_name" label="真实姓名" width="100" />
        <el-table-column prop="created_at" label="申请时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.created_at) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleApprove(row)">
              通过
            </el-button>
            <el-button type="danger" size="small" @click="handleReject(row)">
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 拒绝对话框 -->
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
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPendingWithdrawals, reviewWithdrawal } from '@/api/finance'
import dayjs from 'dayjs'

const loading = ref(false)
const withdrawals = ref([])
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentWithdrawal = ref(null)

const getPaymentMethodText = (method) => {
  const texts = {
    alipay: '支付宝',
    wechat: '微信',
    bank: '银行卡'
  }
  return texts[method] || method
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const loadWithdrawals = async () => {
  loading.value = true
  try {
    const res = await getPendingWithdrawals()
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
</style>
