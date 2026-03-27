<template>
  <div class="orders-page">
    <el-card class="order-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon class="title-icon"><Document /></el-icon>
            <span class="title-text">订单管理</span>
          </div>
          <div class="header-actions">
            <el-radio-group v-model="dateFilterType" @change="handleDateFilterTypeChange" class="date-filter-type">
              <el-radio-button label="all">全部订单</el-radio-button>
              <el-radio-button label="today">
                <el-icon><Calendar /></el-icon>今日订单
              </el-radio-button>
              <el-radio-button label="dateRange">
                <el-icon><Calendar /></el-icon>日期筛选
              </el-radio-button>
            </el-radio-group>
            <el-date-picker
              v-if="dateFilterType === 'dateRange'"
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              @change="handleDateRangeChange"
              class="date-range-picker"
            />
            <el-radio-group v-model="statusFilter" @change="handleStatusChange" class="status-filter">
              <el-radio-button label="">全部状态</el-radio-button>
              <el-radio-button :label="0">
                <el-icon><CircleCheck /></el-icon>待分配
              </el-radio-button>
              <el-radio-button :label="1">
                <el-icon><Timer /></el-icon>待接单
              </el-radio-button>
              <el-radio-button :label="2">
                <el-icon><Timer /></el-icon>待接单2
              </el-radio-button>
              <el-radio-button :label="3">
                <el-icon><Loading /></el-icon>服务中
              </el-radio-button>
              <el-radio-button :label="4">
                <el-icon><CircleCheck /></el-icon>已完成
              </el-radio-button>
              <el-radio-button :label="5">
                <el-icon><CircleClose /></el-icon>已取消
              </el-radio-button>
            </el-radio-group>
            <el-button
              v-if="isAdmin || isCustomerService"
              type="primary"
              class="create-btn"
              @click="createDialogVisible = true"
            >
              <el-icon><Plus /></el-icon>
              创建订单
            </el-button>
          </div>
        </div>
      </template>

      <!-- 统计卡片 -->
      <div class="stats-row" v-if="!isPlayer">
        <div class="stat-card pending">
          <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">待分配</div>
            <div class="stat-value">{{ orderStats.pending }}</div>
          </div>
        </div>
        <div class="stat-card waiting">
          <div class="stat-icon"><el-icon><Timer /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">待接单</div>
            <div class="stat-value">{{ orderStats.waiting }}</div>
          </div>
        </div>
        <div class="stat-card in-service">
          <div class="stat-icon"><el-icon><Loading /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">服务中</div>
            <div class="stat-value">{{ orderStats.inService }}</div>
          </div>
        </div>
        <div class="stat-card completed">
          <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">已完成</div>
            <div class="stat-value">{{ orderStats.completed }}</div>
          </div>
        </div>
        <div class="stat-card cancelled">
          <div class="stat-icon"><el-icon><CircleClose /></el-icon></div>
          <div class="stat-info">
            <div class="stat-label">已取消</div>
            <div class="stat-value">{{ orderStats.cancelled }}</div>
          </div>
        </div>
      </div>

      <div v-if="isPlayer">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8" v-for="order in orders" :key="order.id" style="margin-bottom: 20px;">
            <el-card class="order-item-card" :body-style="{ padding: '20px' }" shadow="hover">
              <template #header>
                <div class="card-header-inner">
                  <span class="order-no">{{ order.orderNo }}</span>
                  <el-tag :type="getStatusType(order.status)" effect="dark" size="small">
                    <el-icon v-if="order.status === 3" class="is-loading"><Loading /></el-icon>
                    {{ getStatusText(order.status) }}
                  </el-tag>
                </div>
              </template>
              <div class="order-content">
                <div class="info-row">
                  <span class="info-label">老板:</span>
                  <span class="info-value">{{ order.bossInfo }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">内容:</span>
                  <span class="info-value">{{ order.serviceContent }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">时长:</span>
                  <span class="info-value">{{ order.serviceHours }}h (¥{{ order.pricePerHour }}/h)</span>
                </div>
                <div class="info-row price-row">
                  <span class="info-label">总价:</span>
                  <span class="price-value">¥{{ order.totalAmount }}</span>
                </div>
                <div v-if="order.incomeAmount" class="info-row income-row">
                  <span class="info-label">预计收入:</span>
                  <span class="income-value">¥{{ order.incomeAmount }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">预约:</span>
                  <span class="info-value">{{ formatDate(order.scheduledTime) }}</span>
                </div>
                
                <div v-if="order.status === 3" class="service-timer">
                  <div class="timer-label">正在服务中</div>
                  <div class="timer-value">{{ getServiceDuration(order.startedAt) }}</div>
                </div>

                <div class="action-buttons">
                  <!-- 待接单状态（status=1）- 未接单 -->
                  <el-button v-if="order.status === 1 && !order.currentUserAccepted" type="success" size="default" @click="handleAccept(order)">
                    <el-icon><Check /></el-icon>接单
                  </el-button>
                  <!-- 待接单状态（status=1）- 已接单，等待另一人 -->
                  <el-button v-if="order.status === 1 && order.currentUserAccepted" type="info" size="default" disabled>
                    <el-icon><Timer /></el-icon>等待他人接单
                  </el-button>
                  <!-- 待接单2状态（status=2）- 未接单 -->
                  <el-button v-if="order.status === 2 && !order.currentUserAccepted" type="success" size="default" @click="handleAccept(order)">
                    <el-icon><Check /></el-icon>接单
                  </el-button>
                  <!-- 待接单2状态（status=2）- 已接单，等待另一人 -->
                  <el-button v-if="order.status === 2 && order.currentUserAccepted" type="info" size="default" disabled>
                    <el-icon><Timer /></el-icon>等待他人接单
                  </el-button>
                  <!-- 服务中状态 - 当前用户未完成，另一人未完成：可以完成 -->
                  <el-button v-if="order.status === 3 && !order.currentUserCompleted && !order.otherPlayerCompleted" type="warning" size="default" @click="handleComplete(order)">
                    <el-icon><CircleCheck /></el-icon>完成订单
                  </el-button>
                  <!-- 服务中状态 - 当前用户已完成，另一人未完成：等待他人 -->
                  <el-button v-if="order.status === 3 && order.currentUserCompleted && !order.otherPlayerCompleted" type="info" size="default" disabled>
                    <el-icon><Timer /></el-icon>等待他人完成
                  </el-button>
                  <!-- 服务中状态 - 当前用户未完成，另一人已完成：可以完成（最后一人） -->
                  <el-button v-if="order.status === 3 && !order.currentUserCompleted && order.otherPlayerCompleted" type="warning" size="default" @click="handleComplete(order)">
                    <el-icon><CircleCheck /></el-icon>完成订单
                  </el-button>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-if="orders.length === 0" description="暂无订单" />
      </div>

      <div v-else class="table-container">
        <div v-if="isAdmin" class="batch-actions">
          <el-button 
            type="danger" 
            plain
            :disabled="selectedOrders.length === 0"
            @click="handleBatchDelete"
          >
            <el-icon><Delete /></el-icon>
            批量删除 {{ selectedOrders.length > 0 ? `(${selectedOrders.length})` : '' }}
          </el-button>
          <span v-if="selectedOrders.length > 0" class="batch-tip">
            <el-icon><Warning /></el-icon>
            已选择 {{ selectedOrders.length }} 个订单
          </span>
        </div>
        <el-table 
          :data="orders" 
          v-loading="loading" 
          stripe 
          class="order-table"
          @selection-change="handleSelectionChange"
        >
          <el-table-column v-if="isAdmin" type="selection" width="55" />
          <el-table-column prop="orderNo" label="订单号" width="150">
            <template #default="{ row }">
              <span class="order-no-text">{{ row.orderNo }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="bossInfo" label="老板信息" min-width="140" show-overflow-tooltip />
          <el-table-column prop="serviceContent" label="服务内容" min-width="150" show-overflow-tooltip />
          <el-table-column label="时长/价格" width="140">
            <template #default="{ row }">
              <div class="time-price">
                <div class="time">{{ row.serviceHours }}h</div>
                <div class="price">¥{{ row.pricePerHour }}/h</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="120">
            <template #default="{ row }">
              <span class="total-amount">¥{{ row.totalAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" effect="light" size="small">
                <el-icon v-if="row.status === 3" class="is-loading"><Loading /></el-icon>
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="当前陪玩师" width="160">
            <template #default="{ row }">
              <span :class="{ 'no-player': !row.currentPlayerNickname }">
                 {{ row.currentPlayerNickname || '待分配' }}
                 <template v-if="row.currentPlayer2Nickname">
                   / {{ row.currentPlayer2Nickname }}
                 </template>
              </span>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="160">
            <template #default="{ row }">
              <div class="time-info">
                <div class="create-time">{{ formatDate(row.createdAt) }}</div>
                <div v-if="row.scheduledTime" class="schedule-time">
                  预约: {{ formatDate(row.scheduledTime) }}
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="取消原因" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.status === 5" class="cancel-reason">{{ row.cancelReason || '-' }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <div class="action-group">
                <el-button
                  type="info"
                  size="small"
                  @click="handleDetail(row)"
                >
                  <el-icon><View /></el-icon>详情
                </el-button>
                <el-button
                  v-if="(isAdmin || isCustomerService) && row.status === 0"
                  type="primary"
                  size="small"
                  @click="handleAssign(row)"
                >
                  <el-icon><Position /></el-icon>派送
                </el-button>
                <el-button
                  v-if="(isAdmin || isCustomerService) && (row.status === 1 || row.status === 2 || row.status === 3)"
                  type="warning"
                  size="small"
                  @click="handleAssign(row)"
                >
                  <el-icon><RefreshRight /></el-icon>改派
                </el-button>
                <el-button
                  v-if="(isAdmin || isCustomerService) && row.status !== 4 && row.status !== 5"
                  type="danger"
                  size="small"
                  @click="handleCancel(row)"
                >
                  <el-icon><CircleClose /></el-icon>取消
                </el-button>
              </div>
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
        @current-change="loadOrders"
        @size-change="loadOrders"
      />
    </el-card>

    <!-- 创建订单对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建订单" width="550px" class="order-dialog">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px" class="order-form">
        <el-form-item label="老板名字" prop="boss_info">
          <el-input v-model="createForm.boss_info" placeholder="请输入老板名字" />
        </el-form-item>
        <el-form-item label="服务内容" prop="service_type">
          <el-radio-group v-model="createForm.service_type" @change="handleServiceTypeChange">
            <el-radio-button label="peiwand">陪玩单</el-radio-button>
            <el-radio-button label="huhang">护航单</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <!-- 陪玩单选项 -->
        <template v-if="createForm.service_type === 'peiwand'">
          <el-form-item label="陪玩等级" prop="player_level">
            <el-select v-model="createForm.player_level" placeholder="请选择陪玩等级" style="width: 100%" @change="handleLevelChange">
              <el-option
                v-for="level in levelPrices"
                :key="level.id"
                :label="level.level + ' (¥' + level.defaultPrice + '/h)'"
                :value="level.level"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="注意事项" prop="precautions">
            <el-select v-model="createForm.precautions" placeholder="请选择注意事项" style="width: 100%" clearable>
              <el-option
                v-for="item in precautions"
                :key="item.id"
                :label="item.value"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="服务时长" prop="service_hours">
            <el-input-number v-model="createForm.service_hours" :min="0.5" :max="24" :step="0.5" style="width: 100%" @change="calculateTotalPrice" />
          </el-form-item>
          <el-form-item label="单价">
            <div class="price-display">¥{{ createForm.price_per_hour }}/h</div>
          </el-form-item>
          <el-form-item label="总价" prop="manual_total_amount">
            <el-input-number v-model="createForm.manual_total_amount" :min="1" :max="10000" :precision="2" style="width: 100%" />
            <div class="auto-price-tip">自动计算价格: ¥{{ (createForm.service_hours * createForm.price_per_hour).toFixed(2) }}</div>
          </el-form-item>
        </template>
        <!-- 护航单选项 -->
        <template v-if="createForm.service_type === 'huhang'">
          <el-form-item label="服务人数" prop="player_count">
            <el-radio-group v-model="createForm.player_count">
              <el-radio-button label="single">单人</el-radio-button>
              <el-radio-button label="double">双人</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="服务项目" prop="service_content">
            <el-select v-model="createForm.service_content" placeholder="请选择服务项目" style="width: 100%" filterable allow-create>
              <el-option
                v-for="item in serviceItems"
                :key="item.id"
                :label="item.value"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="订单价格" prop="total_amount">
            <el-input-number v-model="createForm.total_amount" :min="1" :max="10000" :precision="2" style="width: 100%" />
          </el-form-item>
        </template>
        <el-form-item label="预约时间" prop="scheduled_time">
          <el-date-picker
            v-model="createForm.scheduled_time"
            type="datetime"
            placeholder="选择预约时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定创建</el-button>
      </template>
    </el-dialog>

    <!-- 派送订单对话框 -->
    <el-dialog v-model="assignDialogVisible" title="派送订单" width="550px" class="order-dialog">
      <el-form label-width="100px">
        <el-form-item label="订单类型">
          <el-tag v-if="currentOrder?.playerCount === 'DOUBLE' || currentOrder?.playerCount === 'double'" type="danger" size="large">双人订单</el-tag>
          <el-tag v-else type="info" size="large">单人订单</el-tag>
        </el-form-item>
        <el-form-item label="陪玩师1">
          <el-select v-model="assignForm.player_id" placeholder="请选择陪玩师1" style="width: 100%" filterable>
            <el-option
              v-for="player in availablePlayers"
              :key="player.id"
              :label="player.nickname + ' (等级: ' + player.level + ' | ¥' + player.pricePerHour + '/h | 进行中: ' + player.activeOrdersCount + ')'"
              :value="player.id"
              :disabled="player.id === assignForm.player_id2"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="陪玩师2" v-if="currentOrder?.playerCount === 'DOUBLE' || currentOrder?.playerCount === 'double'">
          <el-select v-model="assignForm.player_id2" placeholder="请选择陪玩师2" style="width: 100%" filterable>
            <el-option
              v-for="player in availablePlayers"
              :key="player.id"
              :label="player.nickname + ' (等级: ' + player.level + ' | ¥' + player.pricePerHour + '/h | 进行中: ' + player.activeOrdersCount + ')'"
              :value="player.id"
              :disabled="player.id === assignForm.player_id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">确定派送</el-button>
      </template>
    </el-dialog>

    <!-- 完成订单对话框 -->
    <el-dialog v-model="completeDialogVisible" title="完成订单" width="400px" class="order-dialog">
      <el-form :model="completeForm" label-width="100px">
        <el-form-item label="实际时长">
          <el-input-number v-model="completeForm.actual_hours" :min="0.5" :max="24" :step="0.5" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComplete">确认完成</el-button>
      </template>
    </el-dialog>

    <!-- 取消订单对话框 -->
    <el-dialog v-model="cancelDialogVisible" title="取消订单" width="450px" class="order-dialog">
      <el-form :model="cancelForm" :rules="cancelRules" ref="cancelFormRef" label-width="100px">
        <el-form-item label="订单号">
          <span class="order-no-display">{{ currentOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="取消原因" prop="reason">
          <el-input
            v-model="cancelForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入取消原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitCancel">确认取消</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="700px" class="order-dialog">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getStatusType(currentOrder.status)" effect="light" size="small">
            {{ getStatusText(currentOrder.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="订单类型">
          <el-tag :type="currentOrder.playerCount === 'DOUBLE' ? 'warning' : 'primary'" effect="light" size="small">
            {{ currentOrder.playerCount === 'DOUBLE' ? '双人订单' : '单人订单' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="老板信息" :span="2">{{ currentOrder.bossInfo }}</el-descriptions-item>
        
        <!-- 单人订单显示 -->
        <template v-if="currentOrder.playerCount !== 'DOUBLE'">
          <el-descriptions-item label="服务内容" :span="2">{{ currentOrder.serviceContent }}</el-descriptions-item>
          <el-descriptions-item label="服务时长">{{ currentOrder.serviceHours }} 小时</el-descriptions-item>
          <el-descriptions-item label="单价">¥{{ currentOrder.pricePerHour }}/小时</el-descriptions-item>
          <el-descriptions-item label="订单总价">
            <span class="detail-price">¥{{ currentOrder.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="实际时长">{{ currentOrder.actualHours || '-' }} 小时</el-descriptions-item>
          <el-descriptions-item label="当前陪玩师">{{ currentOrder.currentPlayerNickname || '待分配' }}</el-descriptions-item>
        </template>
        
        <!-- 双人订单显示 -->
        <template v-if="currentOrder.playerCount === 'DOUBLE'">
          <el-descriptions-item label="服务项目" :span="2">{{ currentOrder.serviceContent }}</el-descriptions-item>
          <el-descriptions-item label="预估时长">{{ currentOrder.serviceHours }} 小时</el-descriptions-item>
          <el-descriptions-item label="订单总价">
            <span class="detail-price">¥{{ currentOrder.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="陪玩师1">{{ currentOrder.currentPlayerNickname || '待分配' }}</el-descriptions-item>
          <el-descriptions-item label="陪玩师2">{{ currentOrder.currentPlayer2Nickname || '待分配' }}</el-descriptions-item>
        </template>
        
        <el-descriptions-item label="创建人">{{ currentOrder.createdByNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预约时间">{{ formatDate(currentOrder.scheduledTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(currentOrder.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatDate(currentOrder.startedAt) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDate(currentOrder.completedAt) || '-' }}</el-descriptions-item>
        
        <!-- 双人订单陪玩师完成详情 -->
        <template v-if="currentOrder.playerCount === 'DOUBLE' && currentOrder.playerSessions && currentOrder.playerSessions.length > 0">
          <el-descriptions-item label="陪玩师服务详情" :span="2">
            <div class="player-sessions">
              <div v-for="session in currentOrder.playerSessions" :key="session.playerId" class="player-session-item">
                <div class="player-name">
                  <el-icon><User /></el-icon>
                  {{ session.playerNickname }}
                </div>
                <div class="session-detail">
                  <span class="session-label">接单时间:</span>
                  <span>{{ formatDate(session.startedAt) || '-' }}</span>
                </div>
                <div class="session-detail">
                  <span class="session-label">完成时间:</span>
                  <span>{{ formatDate(session.endedAt) || '服务中...' }}</span>
                </div>
                <div class="session-detail">
                  <span class="session-label">服务时长:</span>
                  <span>{{ session.actualHours ? session.actualHours + ' 小时' : '-' }}</span>
                </div>
              </div>
            </div>
          </el-descriptions-item>
        </template>
        
        <el-descriptions-item v-if="currentOrder.status === 5" label="取消时间">{{ formatDate(currentOrder.cancelledAt) || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="currentOrder.status === 5" label="取消原因" :span="2">
          <span class="detail-cancel-reason">{{ currentOrder.cancelReason || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getOrders, getOrderById, getMyInServiceOrders, createOrder, assignOrder, acceptOrder, completeOrder, cancelOrder, batchDeleteOrders } from '@/api/orders'
import { getPlayers } from '@/api/users'
import { getLevelPrices, getSystemOptions } from '@/api/system'
import dayjs from 'dayjs'
import { Document, Plus, CircleCheck, CircleClose, Timer, Loading, Check, Position, RefreshRight, Calendar, View, User, Delete, Warning } from '@element-plus/icons-vue'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)
const isCustomerService = computed(() => userStore.isCustomerService)
const isPlayer = computed(() => userStore.isPlayer)
const userId = computed(() => userStore.userInfo?.id)

const loading = ref(false)
const orders = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusFilter = ref('')
const dateFilterType = ref('all')
const dateRange = ref([])
const availablePlayers = ref([])
const levelPrices = ref([])
const precautions = ref([])
const serviceItems = ref([])

const orderStats = ref({
  pending: 0,
  waiting: 0,
  inService: 0,
  completed: 0,
  cancelled: 0
})

const createDialogVisible = ref(false)
const assignDialogVisible = ref(false)
const completeDialogVisible = ref(false)
const cancelDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const currentOrder = ref(null)
const createFormRef = ref()
const cancelFormRef = ref()
const selectedOrders = ref([])

const createForm = reactive({
  boss_info: '',
  service_type: 'peiwand',
  player_level: '',
  precautions: '',
  service_content: '',
  player_count: 'single',
  service_hours: 1,
  price_per_hour: 50,
  total_amount: 50,
  manual_total_amount: 50,
  scheduled_time: '',
  remark: ''
})

const createRules = {
  boss_info: [{ required: true, message: '请输入老板名字', trigger: 'blur' }],
  service_type: [{ required: true, message: '请选择服务类型', trigger: 'change' }],
  player_level: [{ required: true, message: '请选择陪玩等级', trigger: 'change' }],
  service_content: [{ required: true, message: '请输入服务项目', trigger: 'blur' }],
  service_hours: [{ required: true, message: '请输入服务时长', trigger: 'blur' }],
  total_amount: [{ required: true, message: '请输入订单价格', trigger: 'blur' }]
}

const assignForm = reactive({
  player_id: '',
  player_id2: ''
})

const completeForm = reactive({
  actual_hours: 1
})

const cancelForm = reactive({
  reason: ''
})

const cancelRules = {
  reason: [{ required: true, message: '请输入取消原因', trigger: 'blur' }]
}

const getStatusType = (status) => {
  const types = {
    0: 'info',    // PENDING_ASSIGN: 待分配
    1: 'warning', // PENDING_ACCEPT: 待接单
    2: 'warning', // PENDING_ACCEPT_2: 待接单2
    3: 'primary', // IN_SERVICE: 服务中
    4: 'success', // COMPLETED: 已完成
    5: 'danger'   // CANCELLED: 已取消
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    0: '待分配',      // PENDING_ASSIGN
    1: '待接单',      // PENDING_ACCEPT
    2: '待接单2',     // PENDING_ACCEPT_2
    3: '服务中',      // IN_SERVICE
    4: '已完成',      // COMPLETED
    5: '已取消'       // CANCELLED
  }
  return texts[status] || '未知'
}

const formatDate = (date) => {
  return date ? dayjs(date).format('MM-DD HH:mm') : '-'
}

const now = ref(dayjs())
let timerInterval

const getServiceDuration = (startedAt) => {
  if (!startedAt) return '00:00:00'
  const start = dayjs(startedAt)
  const diff = now.value.diff(start)
  if (diff < 0) return '00:00:00'

  const seconds = Math.floor((diff / 1000) % 60)
  const minutes = Math.floor((diff / (1000 * 60)) % 60)
  const hours = Math.floor((diff / (1000 * 60 * 60)))

  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
}

const calculateStats = () => {
  orderStats.value = {
    pending: orders.value.filter(o => o.status === 0).length,
    waiting: orders.value.filter(o => o.status === 1 || o.status === 2).length,
    inService: orders.value.filter(o => o.status === 3).length,
    completed: orders.value.filter(o => o.status === 4).length,
    cancelled: orders.value.filter(o => o.status === 5).length
  }
}

const loadOrders = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      status: statusFilter.value !== '' ? statusFilter.value : undefined,
      sortBy: 'createdAt',
      sortOrder: 'desc'
    }
    if (dateFilterType.value === 'today') {
      params.today = true
    } else if (dateFilterType.value === 'dateRange' && dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getOrders(params)
    orders.value = Array.isArray(res.data) ? res.data : (res.data?.list || [])
    total.value = res.data?.total || 0
    if (!isPlayer.value) {
      calculateStats()
    }
  } finally {
    loading.value = false
  }
}

const handleStatusChange = () => {
  page.value = 1
  loadOrders()
}

const handleDateFilterTypeChange = () => {
  page.value = 1
  if (dateFilterType.value !== 'dateRange') {
    dateRange.value = []
  }
  loadOrders()
}

const handleDateRangeChange = () => {
  page.value = 1
  loadOrders()
}

// 加载等级价格
const loadLevelPrices = async () => {
  try {
    const res = await getLevelPrices()
    levelPrices.value = res.data || []
  } catch (error) {
    console.error('加载等级价格失败', error)
  }
}

// 加载注意事项
const loadPrecautions = async () => {
  try {
    const res = await getSystemOptions('PRECAUTION')
    precautions.value = res.data || []
  } catch (error) {
    console.error('加载注意事项失败', error)
  }
}

// 加载服务项目
const loadServiceItems = async () => {
  try {
    const res = await getSystemOptions('SERVICE_ITEM')
    serviceItems.value = res.data || []
  } catch (error) {
    console.error('加载服务项目失败', error)
  }
}

// 服务类型改变时重置相关字段
const handleServiceTypeChange = () => {
  createForm.player_level = ''
  createForm.precautions = ''
  createForm.service_content = ''
  createForm.player_count = 'single'
  createForm.service_hours = 1
  createForm.price_per_hour = 50
  createForm.total_amount = 50
  createForm.manual_total_amount = 50
}

// 等级改变时更新单价和手动总价
const handleLevelChange = () => {
  const selectedLevel = levelPrices.value.find(l => l.level === createForm.player_level)
  if (selectedLevel) {
    createForm.price_per_hour = selectedLevel.defaultPrice
    // 更新手动总价为自动计算的价格
    createForm.manual_total_amount = Number((createForm.service_hours * createForm.price_per_hour).toFixed(2))
  }
}

// 计算总价
const calculateTotalPrice = () => {
  // 价格自动根据等级和时长计算，无需额外操作
}

const submitCreate = async () => {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    let orderData = {
      bossInfo: createForm.boss_info,
      scheduledTime: createForm.scheduled_time ? dayjs(createForm.scheduled_time).format('YYYY-MM-DDTHH:mm:ss') : null,
      remark: createForm.remark,
      orderType: createForm.service_type
    }

    if (createForm.service_type === 'peiwand') {
      // 陪玩单
      let precautionsText = createForm.precautions ? `【${createForm.precautions}】` : ''
      orderData.serviceContent = createForm.player_level + '陪玩' + precautionsText
      orderData.serviceHours = createForm.service_hours
      orderData.pricePerHour = createForm.price_per_hour
      // 使用手动输入的总价
      orderData.totalAmount = createForm.manual_total_amount.toFixed(2)
      orderData.playerCount = 'single'
    } else {
      // 护航单
      let playerCountText = createForm.player_count === 'double' ? '【双人】' : '【单人】'
      orderData.serviceContent = createForm.service_content + playerCountText
      orderData.serviceHours = 1
      orderData.pricePerHour = createForm.total_amount
      orderData.totalAmount = createForm.total_amount.toFixed(2)
      orderData.playerCount = createForm.player_count
    }

    await createOrder(orderData)
    ElMessage.success('订单创建成功')
    createDialogVisible.value = false
    createFormRef.value?.resetFields()
    // 重置默认值
    createForm.service_type = 'peiwand'
    createForm.player_count = 'single'
    createForm.service_hours = 1
    createForm.price_per_hour = 50
    createForm.total_amount = 50
    createForm.manual_total_amount = 50
    loadOrders()
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const handleAssign = async (row) => {
  currentOrder.value = row
  const res = await getPlayers({ status: 'active', pageSize: 1000 })
  availablePlayers.value = res.data?.list || res.data || []
  assignForm.player_id = ''
  assignForm.player_id2 = ''
  assignDialogVisible.value = true
}

const handleDetail = (row) => {
  currentOrder.value = row
  detailDialogVisible.value = true
}

const submitAssign = async () => {
  if (!assignForm.player_id) {
    ElMessage.warning('请选择陪玩师1')
    return
  }
  
  // 双人订单需要选择第二个陪玩师
  const isDoubleOrder = currentOrder.value?.playerCount === 'DOUBLE' || currentOrder.value?.playerCount === 'double'
  if (isDoubleOrder && !assignForm.player_id2) {
    ElMessage.warning('双人订单需要选择两个陪玩师')
    return
  }
  
  try {
    const assignData = { 
      playerId: assignForm.player_id,
      playerId2: isDoubleOrder ? assignForm.player_id2 : null
    }
    await assignOrder(currentOrder.value.id, assignData)
    ElMessage.success('派送成功')
    assignDialogVisible.value = false
    loadOrders()
  } catch (error) {
    ElMessage.error('派送失败')
  }
}

const handleAccept = async (row) => {
  try {
    // 先获取订单最新状态
    const orderRes = await getOrderById(row.id)
    console.log('订单详情:', orderRes.data)
    console.log('当前用户ID:', userId.value)
    console.log('订单状态:', orderRes.data.status)
    console.log('陪玩师1:', orderRes.data.currentPlayerId, orderRes.data.currentPlayerNickname)
    console.log('陪玩师2:', orderRes.data.currentPlayer2Id, orderRes.data.currentPlayer2Nickname)
    console.log('订单类型:', orderRes.data.playerCount)
    
    await acceptOrder(row.id)
    ElMessage.success('接单成功')
    loadOrders()
  } catch (error) {
    ElMessage.error(error.message || '接单失败')
    console.error('接单失败:', error)
    
    // 如果是因为有服务中订单，显示详细信息
    if (error.message && error.message.includes('服务中的订单')) {
      try {
        const inServiceRes = await getMyInServiceOrders()
        console.log('我的服务中订单:', inServiceRes.data)
        console.log('服务中订单数量:', inServiceRes.data?.length || 0)
        if (inServiceRes.data && inServiceRes.data.length > 0) {
          console.table(inServiceRes.data.map(o => ({
            id: o.id,
            orderNo: o.orderNo,
            status: o.status,
            currentPlayerId: o.currentPlayerId,
            currentPlayer2Id: o.currentPlayer2Id
          })))
        }
      } catch (e) {
        console.error('获取服务中订单失败:', e)
      }
    }
  }
}

const handleComplete = (row) => {
  currentOrder.value = row
  completeForm.actual_hours = row.service_hours
  completeDialogVisible.value = true
}

const submitComplete = async () => {
  try {
    const payload = { actualHours: completeForm.actual_hours || 0.5 }
    await completeOrder(currentOrder.value.id, payload)
    ElMessage.success('订单完成')
    completeDialogVisible.value = false
    loadOrders()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleCancel = (row) => {
  currentOrder.value = row
  cancelForm.reason = ''
  cancelDialogVisible.value = true
}

const handleSelectionChange = (selection) => {
  selectedOrders.value = selection
}

const handleBatchDelete = async () => {
  if (selectedOrders.value.length === 0) {
    ElMessage.warning('请先选择要删除的订单')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedOrders.value.length} 个订单吗？此操作不可恢复！`,
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        distinguishCancelAndClose: true
      }
    )
    
    // 二次确认：需要输入"DELETE"才能执行删除
    const { value } = await ElMessageBox.prompt(
      '请输入 "DELETE" 确认删除操作',
      '危险操作确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        inputPattern: /^DELETE$/,
        inputErrorMessage: '请输入正确的确认文字'
      }
    )
    
    if (value === 'DELETE') {
      const ids = selectedOrders.value.map(order => order.id)
      await batchDeleteOrders(ids)
      ElMessage.success('批量删除成功')
      selectedOrders.value = []
      loadOrders()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '批量删除失败')
    }
  }
}

const submitCancel = async () => {
  const valid = await cancelFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await cancelOrder(currentOrder.value.id, { reason: cancelForm.reason })
    ElMessage.success('订单已取消')
    cancelDialogVisible.value = false
    loadOrders()
  } catch (error) {
    ElMessage.error('取消失败')
  }
}

// 自动刷新订单列表的定时器
let autoRefreshInterval = null

// 启动自动刷新
const startAutoRefresh = () => {
  if (autoRefreshInterval) return
  autoRefreshInterval = setInterval(() => {
    // 如果有待接单的订单，自动刷新
    const hasPendingOrder = orders.value.some(o => o.status === 1 || o.status === 2)
    if (hasPendingOrder) {
      loadOrders()
    }
  }, 3000) // 每3秒刷新一次
}

// 停止自动刷新
const stopAutoRefresh = () => {
  if (autoRefreshInterval) {
    clearInterval(autoRefreshInterval)
    autoRefreshInterval = null
  }
}

onMounted(() => {
  loadOrders()
  loadLevelPrices()
  loadPrecautions()
  loadServiceItems()
  timerInterval = setInterval(() => {
    now.value = dayjs()
  }, 1000)
  // 陪玩师启动自动刷新
  if (isPlayer.value) {
    startAutoRefresh()
  }
})

onUnmounted(() => {
  if (timerInterval) clearInterval(timerInterval)
  stopAutoRefresh()
})
</script>

<style scoped lang="scss">
.orders-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.order-card {
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
  gap: 15px;
  flex-wrap: wrap;
}

.date-filter-type {
  :deep(.el-radio-button__inner) {
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 8px 16px;
    font-size: 13px;
  }
}

.date-range-picker {
  width: 240px;
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

.create-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px 20px;
  font-weight: 500;
}

// 统计卡片
.stats-row {
  display: flex;
  gap: 15px;
  margin-bottom: 25px;
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
    background: #ecf5ff;
    color: #409eff;
  }
  
  &.waiting .stat-icon {
    background: #fdf6ec;
    color: #e6a23c;
  }
  
  &.in-service .stat-icon {
    background: #f0f9eb;
    color: #67c23a;
  }
  
  &.completed .stat-icon {
    background: #f4f4f5;
    color: #909399;
  }
  
  &.cancelled .stat-icon {
    background: #fef0f0;
    color: #f56c6c;
  }
}

// 表格样式
.table-container {
  margin-top: 20px;
  
  .batch-actions {
    margin-bottom: 15px;
    display: flex;
    align-items: center;
    gap: 15px;
    
    .batch-tip {
      display: flex;
      align-items: center;
      gap: 5px;
      color: #e6a23c;
      font-size: 14px;
      
      .el-icon {
        font-size: 16px;
      }
    }
  }
}

.order-table {
  :deep(th) {
    background: #f5f7fa;
    font-weight: 600;
    color: #606266;
  }
  
  .order-no-text {
    font-family: monospace;
    font-weight: 600;
    color: #409eff;
  }
  
  .time-price {
    .time {
      font-weight: 600;
      color: #303133;
    }
    .price {
      font-size: 12px;
      color: #909399;
    }
  }
  
  .total-amount {
    font-weight: 700;
    color: #f56c6c;
    font-size: 15px;
  }
  
  .no-player {
    color: #c0c4cc;
    font-style: italic;
  }
  
  .time-info {
    .create-time {
      color: #606266;
    }
    .schedule-time {
      font-size: 12px;
      color: #409eff;
    }
  }
  
  .action-group {
    display: flex;
    gap: 5px;
    flex-wrap: wrap;
  }
}

// 陪玩师卡片样式
.order-item-card {
  border-radius: 10px;
  transition: transform 0.2s, box-shadow 0.2s;
  
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }
  
  :deep(.el-card__header) {
    padding: 15px 20px;
    background: #f5f7fa;
  }
}

.card-header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-no {
  font-family: monospace;
  font-weight: 700;
  color: #409eff;
  font-size: 15px;
}

.order-content {
  .info-row {
    display: flex;
    margin-bottom: 10px;
    
    .info-label {
      width: 80px;
      color: #909399;
      font-size: 13px;
    }
    
    .info-value {
      flex: 1;
      color: #303133;
      font-size: 14px;
    }
  }
  
  .price-row {
    .price-value {
      font-size: 18px;
      font-weight: 700;
      color: #f56c6c;
    }
  }
  
  .income-row {
    .income-value {
      font-weight: 600;
      color: #67c23a;
    }
  }
  
  .service-timer {
    margin: 15px 0;
    padding: 15px;
    background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
    border-radius: 8px;
    text-align: center;
    color: #fff;
    
    .timer-label {
      font-size: 12px;
      opacity: 0.9;
      margin-bottom: 5px;
    }
    
    .timer-value {
      font-size: 28px;
      font-weight: 700;
      font-family: monospace;
    }
  }
  
  .action-buttons {
    display: flex;
    gap: 10px;
    margin-top: 15px;
    
    .el-button {
      flex: 1;
    }
  }
}

// 分页
.pagination {
  margin-top: 25px;
  justify-content: flex-end;
}

// 对话框样式
.order-dialog {
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

.order-form {
  .total-price {
    font-size: 24px;
    font-weight: 700;
    color: #f56c6c;
  }
  .price-display {
    font-size: 16px;
    color: #606266;
    font-weight: 500;
  }
  .auto-price-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 5px;
  }
}

.order-no-display {
  font-family: monospace;
  font-weight: 600;
  color: #409eff;
  font-size: 14px;
}

.cancel-reason {
  color: #f56c6c;
  font-size: 13px;
}

.detail-price {
  font-size: 18px;
  font-weight: 700;
  color: #f56c6c;
}

.detail-cancel-reason {
  color: #f56c6c;
  font-weight: 500;
}

.player-sessions {
  display: flex;
  flex-direction: column;
  gap: 15px;
  
  .player-session-item {
    padding: 12px 15px;
    background: #f5f7fa;
    border-radius: 8px;
    border-left: 3px solid #409eff;
    
    .player-name {
      font-weight: 600;
      font-size: 15px;
      color: #303133;
      margin-bottom: 8px;
      display: flex;
      align-items: center;
      gap: 5px;
      
      .el-icon {
        color: #409eff;
      }
    }
    
    .session-detail {
      font-size: 13px;
      color: #606266;
      margin: 4px 0;
      padding-left: 20px;
      
      .session-label {
        color: #909399;
        margin-right: 5px;
      }
    }
  }
}
</style>
