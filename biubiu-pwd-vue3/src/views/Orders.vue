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
              <el-radio-button :label="3">
                <el-icon><Loading /></el-icon>服务中
              </el-radio-button>
              <el-radio-button :label="6">
                <el-icon><VideoPause /></el-icon>暂存
              </el-radio-button>
              <el-radio-button label="pending_audit">
                <el-icon><CircleCheck /></el-icon>待审核
              </el-radio-button>
              <el-radio-button label="audited">
                <el-icon><CircleCheck /></el-icon>已审核
              </el-radio-button>
              <el-radio-button :label="5">
                <el-icon><CircleClose /></el-icon>已取消
              </el-radio-button>
            </el-radio-group>
            <el-button
              v-if="isAdmin"
              type="warning"
              class="create-btn"
              @click="replenishDialogVisible = true"
            >
              <el-icon><RefreshLeft /></el-icon>
              补单
            </el-button>
            <el-button
              v-if="isAdmin"
              type="info"
              plain
              class="create-btn"
              @click="handleViewDeletedBackups"
            >
              <el-icon><Delete /></el-icon>
              已删除记录
            </el-button>
            <el-button
              v-if="isAdmin || isCustomerService"
              type="primary"
              class="create-btn"
              @click="handleOpenCreate"
            >
              <el-icon><Plus /></el-icon>
              创建订单
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索和统计区域 -->
      <div v-if="!isPlayer">
        <!-- 搜索框 -->
        <div class="search-row">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索订单编号/老板名字/服务内容"
            clearable
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button v-if="searchKeyword" @click="handleClearSearch">
            <el-icon><Close /></el-icon>
            清除
          </el-button>
        </div>
        
        <!-- 统计卡片 -->
        <div class="stats-row">
          <div class="stat-card pending">
            <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
            <div class="stat-info">
              <div class="stat-label">待分配</div>
              <div class="stat-value">{{ orderStats.pending }}</div>
            </div>
          </div>
          <div class="stat-card in-service">
            <div class="stat-icon"><el-icon><Loading /></el-icon></div>
            <div class="stat-info">
              <div class="stat-label">服务中</div>
              <div class="stat-value">{{ orderStats.inService }}</div>
            </div>
          </div>
          <div class="stat-card paused">
            <div class="stat-icon"><el-icon><VideoPause /></el-icon></div>
            <div class="stat-info">
              <div class="stat-label">暂存</div>
              <div class="stat-value">{{ orderStats.paused }}</div>
            </div>
          </div>
          <div class="stat-card completed">
            <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
            <div class="stat-info">
              <div class="stat-label">待审核</div>
              <div class="stat-value">{{ orderStats.pendingAudit }}</div>
            </div>
          </div>
          <div class="stat-card audited">
            <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
            <div class="stat-info">
              <div class="stat-label">已审核</div>
              <div class="stat-value">{{ orderStats.audited }}</div>
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
      </div>

      <div v-if="isPlayer">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8" v-for="order in orders" :key="order.id" style="margin-bottom: 20px;">
            <el-card class="order-item-card" :body-style="{ padding: '20px' }" shadow="hover">
              <template #header>
                <div class="card-header-inner">
                  <span class="order-no">
                    {{ order.orderNo }}
                    <el-tag v-if="order.orderType === 'huhang'" size="small" type="warning" style="margin-left: 6px">护航</el-tag>
                  </span>
                  <el-tag :type="order.status === 4 ? getAuditStatusType(order.auditStatus) : getStatusType(order.status)" effect="dark" size="small">
                    <el-icon v-if="order.status === 3" class="is-loading"><Loading /></el-icon>
                    {{ order.status === 4 ? getAuditStatusText(order.auditStatus) : getStatusText(order.status) }}
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
                  <span class="info-value">{{ formatHoursToHM(order.serviceHours) }} (¥{{ order.pricePerHour }}/h)</span>
                </div>
                <div class="info-row price-row">
                  <span class="info-label">总价:</span>
                  <span class="price-value">¥{{ order.totalAmount }}</span>
                </div>
                <div v-if="order.expectedIncomeAmount" class="info-row income-row">
                  <span class="info-label">预计收入:</span>
                  <span class="income-value">¥{{ order.expectedIncomeAmount }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">预约:</span>
                  <span class="info-value">{{ formatDate(order.scheduledTime) || '未设置' }}</span>
                </div>
                <div v-if="order.startedAt" class="info-row">
                  <span class="info-label">开始:</span>
                  <span class="info-value">{{ formatDate(order.startedAt) }}</span>
                </div>
                <div v-if="order.completedAt" class="info-row">
                  <span class="info-label">完成:</span>
                  <span class="info-value">{{ formatDate(order.completedAt) }}</span>
                </div>
                <div v-if="order.actualHours && order.status === 4" class="info-row">
                  <span class="info-label">实际时长:</span>
                  <span class="info-value actual-hours">{{ formatHoursToHM(order.actualHours) }}</span>
                </div>
                <div v-if="order.status === 4 && order.actualIncomeAmount" class="info-row actual-income-row">
                  <span class="info-label">实际收入:</span>
                  <span class="actual-income-value">¥{{ order.actualIncomeAmount }}</span>
                </div>
                <div v-if="order.status === 4 && order.depositDeductAmount" class="info-row">
                  <span class="info-label">单抵金额:</span>
                  <span class="info-value" style="color: #e6a23c;">-¥{{ order.depositDeductAmount }}</span>
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
                  <el-button v-if="order.status === 3 && !order.currentUserCompleted && !order.otherPlayerCompleted" type="success" size="default" @click="handleComplete(order)">
                    <el-icon><CircleCheck /></el-icon>完成订单
                  </el-button>
                  <!-- 服务中状态 - 当前用户已完成，另一人未完成：等待他人 -->
                  <el-button v-if="order.status === 3 && order.currentUserCompleted && !order.otherPlayerCompleted" type="info" size="default" disabled>
                    <el-icon><Timer /></el-icon>等待他人完成
                  </el-button>
                  <!-- 服务中状态 - 当前用户未完成，另一人已完成：可以完成（最后一人） -->
                  <el-button v-if="order.status === 3 && !order.currentUserCompleted && order.otherPlayerCompleted" type="success" size="default" @click="handleComplete(order)">
                    <el-icon><CircleCheck /></el-icon>完成订单
                  </el-button>
                  <!-- 暂存按钮 - 已接单(1或2)或服务中(3)时可暂存 -->
                  <el-button v-if="(order.status === 1 || order.status === 2 || order.status === 3) && order.currentUserAccepted" type="warning" size="default" @click="handlePause(order)">
                    <el-icon><VideoPause /></el-icon>暂存
                  </el-button>
                  <!-- 恢复按钮 - 暂存状态时可恢复 -->
                  <el-button v-if="order.status === 6" type="success" size="default" @click="handleResume(order)">
                    <el-icon><RefreshRight /></el-icon>恢复订单
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
          <el-button 
            type="success" 
            plain
            :disabled="selectedCompletedOrders.length === 0"
            @click="handleBatchAuditPass"
          >
            <el-icon><CircleCheck /></el-icon>
            批量审核 {{ selectedCompletedOrders.length > 0 ? `(${selectedCompletedOrders.length})` : '' }}
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
          <el-table-column prop="orderNo" label="订单号" min-width="130">
            <template #default="{ row }">
              <span :class="['order-no-text', { 'replenish-order': row.remark?.includes('【补单】') }]">{{ row.orderNo }}</span>
              <el-tag v-if="row.remark?.includes('【补单】')" size="small" type="warning" style="margin-left: 4px">补单</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="bossInfo" label="老板信息" min-width="140" show-overflow-tooltip />
          <el-table-column prop="serviceContent" label="服务内容" min-width="150" show-overflow-tooltip />
          <el-table-column label="时长/价格" min-width="120">
            <template #default="{ row }">
              <div class="time-price">
                <div class="time">{{ formatHoursToHM(row.serviceHours) }}</div>
                <div class="price">¥{{ row.pricePerHour }}/h</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="预计金额" min-width="130" align="center">
            <template #default="{ row }">
              <div class="amount-cell">
                <span class="total-amount">¥{{ row.totalAmount }}</span>
                <span v-if="row.status === 4 && row.actualHours && row.actualHours > row.serviceHours && row.actualTotalAmount" class="actual-amount-tag">
                  (实: ¥{{ row.actualTotalAmount }})
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" min-width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 4 ? getAuditStatusType(row.auditStatus) : getStatusType(row.status)" effect="light" size="small">
                <el-icon v-if="row.status === 3" class="is-loading"><Loading /></el-icon>
                {{ row.status === 4 ? getAuditStatusText(row.auditStatus) : getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="当前陪玩师" min-width="130">
            <template #default="{ row }">
              <span :class="{ 'no-player': !row.currentPlayerNickname }">
                 {{ row.currentPlayerNickname || '待分配' }}
                 <template v-if="row.currentPlayer2Nickname">
                   / {{ row.currentPlayer2Nickname }}
                 </template>
              </span>
            </template>
          </el-table-column>
          <el-table-column label="时间" min-width="130">
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
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <div class="action-group">
                <el-dropdown size="small" trigger="click" @command="(cmd) => handleAction(cmd, row)">
                  <el-button type="primary" size="small">
                    <el-icon><Operation /></el-icon>操作<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="detail"><el-icon><View /></el-icon>详情</el-dropdown-item>
                      <el-dropdown-item v-if="(isAdmin || isCustomerService) && row.status !== 5" command="edit"><el-icon><Edit /></el-icon>编辑</el-dropdown-item>
                      <el-dropdown-item v-if="(isAdmin || isCustomerService) && row.status === 0" command="assign"><el-icon><Position /></el-icon>派送</el-dropdown-item>
                      <el-dropdown-item v-if="(isAdmin || isCustomerService) && row.status === 0 && !row.inGrabHall" command="publish"><el-icon><Tickets /></el-icon>发布到大厅</el-dropdown-item>
                      <el-dropdown-item v-if="(isAdmin || isCustomerService) && row.inGrabHall && row.grabStatus !== 'ASSIGNED'" command="withdraw"><el-icon><RefreshLeft /></el-icon>从大厅撤回</el-dropdown-item>
                      <el-dropdown-item v-if="(isAdmin || isCustomerService) && (row.status === 1 || row.status === 2 || row.status === 3)" command="reassign"><el-icon><RefreshRight /></el-icon>改派</el-dropdown-item>
                      <el-dropdown-item v-if="(isAdmin || isCustomerService) && (row.status === 0 || row.status === 3)" command="pause"><el-icon><VideoPause /></el-icon>暂存</el-dropdown-item>
                      <el-dropdown-item v-if="(isAdmin || isCustomerService) && row.status === 6" command="resume"><el-icon><RefreshRight /></el-icon>恢复</el-dropdown-item>
                      <el-dropdown-item v-if="(isAdmin || isCustomerService) && row.status !== 4 && row.status !== 5" command="cancel" divided><el-icon><CircleClose /></el-icon>取消</el-dropdown-item>
                      <el-dropdown-item v-if="(isAdmin || isCustomerService) && row.status === 4 && row.auditStatus !== 1" command="audit"><el-icon><CircleCheck /></el-icon>审核通过</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
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
    <el-dialog v-model="createDialogVisible" title="创建订单" width="600px" class="order-dialog">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px" class="order-form">
        <el-form-item label="客户类型" prop="customer_type">
          <el-radio-group v-model="createForm.customer_type" @change="handleCustomerTypeChange">
            <el-radio-button label="SCATTER">散客</el-radio-button>
            <el-radio-button label="REGULAR">固定客</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <template v-if="createForm.customer_type === 'REGULAR'">
          <el-form-item label="老板名字" prop="boss_info">
            <el-autocomplete
              v-model="createForm.boss_info"
              :fetch-suggestions="queryBossSuggestions"
              placeholder=""
              style="width: 100%"
              @select="handleBossSelect"
              @clear="handleBossClear"
              @input="handleBossInput"
              clearable
            >
              <template #default="{ item }">
                <div class="boss-suggestion-item">
                  <span class="boss-suggestion-name">
                    <span v-if="item.bossNo" class="boss-suggestion-no">{{ item.bossNo }}</span>
                    {{ item.name }}
                  </span>
                  <span class="boss-suggestion-info">
                    <el-tag size="small" type="warning">VIP{{ item.vipLevel }}</el-tag>
                    <span class="boss-suggestion-balance">余额: ¥{{ item.balance }}</span>
                  </span>
                </div>
              </template>
            </el-autocomplete>
            <div v-if="!selectedBoss && createForm.boss_info && createForm.boss_info.trim()" class="boss-match-info">
              <el-tag type="info" size="small">未匹配到已有老板，将自动创建新老板</el-tag>
            </div>
          </el-form-item>
          <el-form-item v-if="selectedBoss" label="VIP折扣">
            <div class="vip-info">
              <el-tag type="warning">VIP{{ selectedBoss.vipLevel }}</el-tag>
              <span class="discount-text">{{ getDiscountText(selectedBoss.vipLevel) }}</span>
              <span class="discount-rate">({{ getDiscountRateText(selectedBoss.vipLevel) }})</span>
              <span class="balance-text">预存余额: ¥{{ selectedBoss.balance }}</span>
            </div>
          </el-form-item>
        </template>
        <el-form-item v-if="createForm.customer_type === 'SCATTER'" label="老板名字" prop="boss_info">
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
          <el-form-item label="陪玩人数" prop="player_count">
            <el-radio-group v-model="createForm.player_count">
              <el-radio-button label="single">单人</el-radio-button>
              <el-radio-button label="double">双人</el-radio-button>
            </el-radio-group>
            <div class="player-count-tip">
              <el-icon><InfoFilled /></el-icon>
              <span>选择双人将自动创建两份相同的订单，分别派送两位同等级陪玩师</span>
            </div>
          </el-form-item>
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
            <el-select v-model="createForm.precautions" placeholder="请选择注意事项" style="width: 100%" clearable multiple>
              <el-option
                v-for="item in precautions"
                :key="item.id"
                :label="item.value"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="服务时长" prop="service_hours">
            <div class="duration-input-group">
              <el-input-number v-model="createForm.service_hours" :min="0" :max="23" :step="1" style="width: 120px" @change="calculateTotalPrice" />
              <span class="duration-unit">小时</span>
              <el-input-number v-model="createForm.service_minutes" :min="0" :max="59" :step="15" style="width: 120px" @change="calculateTotalPrice" />
              <span class="duration-unit">分钟</span>
              <span class="duration-total">= {{ formatDuration(createForm.service_hours, createForm.service_minutes) }}</span>
            </div>
          </el-form-item>
          <el-form-item label="单价">
            <el-input-number v-model="createForm.price_per_hour" :min="1" :max="1000" :precision="2" style="width: 100%" @change="calculateTotalPrice" />
          </el-form-item>
          <el-form-item v-if="createForm.customer_type === 'REGULAR' && selectedBoss && getVipDiscount(selectedBoss.vipLevel) < 1" label="折扣后">
            <div class="discounted-price">¥{{ calculatedDiscountedPrice.toFixed(2) }}</div>
            <div class="discount-info">{{ getDiscountText(selectedBoss.vipLevel) }} 优惠 ¥{{ (calculatedOriginalPrice - calculatedDiscountedPrice).toFixed(2) }}</div>
          </el-form-item>
          <el-form-item label="总价" prop="manual_total_amount">
            <el-input-number v-model="createForm.manual_total_amount" :min="1" :max="10000" :precision="2" style="width: 100%" />
            <div class="auto-price-tip">
              自动计算: ¥{{ calculatedServicePrice.toFixed(2) }}
              <template v-if="createForm.customer_type === 'REGULAR' && selectedBoss && getVipDiscount(selectedBoss.vipLevel) < 1">
                × {{ getDiscountText(selectedBoss.vipLevel) }} = ¥{{ calculatedDiscountedPrice.toFixed(2) }}
              </template>
            </div>
          </el-form-item>
          <el-form-item v-if="createForm.customer_type === 'REGULAR' && selectedBoss && selectedBoss.balance > 0" label="使用余额">
            <el-switch v-model="createForm.use_balance" active-text="是" inactive-text="否" />
            <div v-if="createForm.use_balance && createForm.manual_total_amount > 0" class="balance-deduct-info">
              将从预存余额中扣除 ¥{{ Math.min(selectedBoss.balance, createForm.manual_total_amount).toFixed(2) }}
            </div>
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
        <el-form-item label="预约时间">
          <div class="scheduled-time-selectors">
            <el-select v-model="createForm.scheduled_month" placeholder="月" style="width: 80px" @change="updateScheduledTime">
              <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m" />
            </el-select>
            <el-select v-model="createForm.scheduled_day" placeholder="日" style="width: 80px" @change="updateScheduledTime">
              <el-option v-for="d in getDaysInMonth" :key="d" :label="d + '日'" :value="d" />
            </el-select>
            <el-select v-model="createForm.scheduled_hour" placeholder="时" style="width: 80px" @change="updateScheduledTime">
              <el-option v-for="h in 24" :key="h" :label="h - 1 + '时'" :value="h - 1" />
            </el-select>
            <el-select v-model="createForm.scheduled_minute" placeholder="分" style="width: 80px" @change="updateScheduledTime">
              <el-option v-for="min in 12" :key="min" :label="(min - 1) * 5 + '分'" :value="(min - 1) * 5" />
            </el-select>
          </div>
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
    <el-dialog v-model="assignDialogVisible" title="派送订单" width="600px" class="order-dialog">
      <el-form label-width="100px">
        <el-form-item label="订单类型">
          <el-tag v-if="currentOrder?.playerCount === 'DOUBLE' || currentOrder?.playerCount === 'double'" type="danger" size="large">双人订单</el-tag>
          <el-tag v-else type="info" size="large">单人订单</el-tag>
        </el-form-item>
        <el-form-item label="筛选条件">
          <div class="assign-filters">
            <el-select v-model="assignFilter.level" placeholder="全部等级" clearable style="width: 140px" @change="filterAvailablePlayers">
              <el-option
                v-for="lvl in assignFilterLevels"
                :key="lvl"
                :label="lvl"
                :value="lvl"
              />
            </el-select>
            <el-select v-model="assignFilter.status" placeholder="全部状态" clearable style="width: 140px" @change="filterAvailablePlayers">
              <el-option label="空闲" value="idle" />
              <el-option label="服务中" value="busy" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="陪玩师1">
          <el-select v-model="assignForm.player_id" placeholder="请选择陪玩师1" style="width: 100%" filterable>
            <el-option
              v-for="player in filteredPlayers"
              :key="player.id"
              :label="(player.playerNo ? player.playerNo + ' ' : '') + player.nickname + ' (等级: ' + player.level + ' | ¥' + player.pricePerHour + '/h | 进行中: ' + player.activeOrdersCount + ')'"
              :value="player.id"
              :disabled="player.id === assignForm.player_id2"
            >
              <div class="player-option">
                <span class="player-option-name">
                  <span v-if="player.playerNo" class="player-option-no">{{ player.playerNo }}</span>
                  {{ player.nickname }}
                </span>
                <span class="player-option-tags">
                  <el-tag size="small" :type="getLevelTagType(player.level)">{{ player.level }}</el-tag>
                  <el-tag size="small" type="info">¥{{ player.pricePerHour }}/h</el-tag>
                  <el-tag size="small" :type="player.activeOrdersCount === 0 ? 'success' : 'warning'">
                    {{ player.activeOrdersCount === 0 ? '空闲' : '进行中 ' + player.activeOrdersCount }}
                  </el-tag>
                </span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="陪玩师2" v-if="currentOrder?.playerCount === 'DOUBLE' || currentOrder?.playerCount === 'double'">
          <el-select v-model="assignForm.player_id2" placeholder="请选择陪玩师2" style="width: 100%" filterable>
            <el-option
              v-for="player in filteredPlayers"
              :key="player.id"
              :label="(player.playerNo ? player.playerNo + ' ' : '') + player.nickname + ' (等级: ' + player.level + ' | ¥' + player.pricePerHour + '/h | 进行中: ' + player.activeOrdersCount + ')'"
              :value="player.id"
              :disabled="player.id === assignForm.player_id"
            >
              <div class="player-option">
                <span class="player-option-name">
                  <span v-if="player.playerNo" class="player-option-no">{{ player.playerNo }}</span>
                  {{ player.nickname }}
                </span>
                <span class="player-option-tags">
                  <el-tag size="small" :type="getLevelTagType(player.level)">{{ player.level }}</el-tag>
                  <el-tag size="small" type="info">¥{{ player.pricePerHour }}/h</el-tag>
                  <el-tag size="small" :type="player.activeOrdersCount === 0 ? 'success' : 'warning'">
                    {{ player.activeOrdersCount === 0 ? '空闲' : '进行中 ' + player.activeOrdersCount }}
                  </el-tag>
                </span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign" :loading="submittingAction === 'assign'">确定派送</el-button>
      </template>
    </el-dialog>

    <!-- 完成订单对话框 -->
    <el-dialog v-model="completeDialogVisible" title="完成订单" width="680px" class="order-dialog complete-order-dialog" @opened="handleCompleteDialogOpen" @close="handleCompleteDialogClose">
      <el-form :model="completeForm" :rules="completeRules" ref="completeFormRef" label-width="100px">
        <el-form-item v-if="currentOrder?.orderType !== 'huhang'" label="实际时长" prop="actual_hours">
          <div class="duration-input-group">
            <el-input-number v-model="completeForm.actual_hours" :min="0" :max="23" :step="1" style="width: 120px" />
            <span class="duration-unit">小时</span>
            <el-input-number v-model="completeForm.actual_minutes" :min="0" :max="59" :step="1" style="width: 120px" />
            <span class="duration-unit">分钟</span>
            <span class="duration-total">= {{ formatDuration(completeForm.actual_hours, completeForm.actual_minutes) }}</span>
          </div>
        </el-form-item>
        
        <!-- 截图上传区域 -->
        <div class="screenshot-section">
          <div class="section-header">
            <div class="section-title">
              <el-icon><Picture /></el-icon>
              <span>结单截图</span>
            </div>
            <el-tag type="info" size="small" effect="light">支持复制粘贴上传，可上传多张</el-tag>
          </div>

          <div class="simple-screenshot-container">
            <!-- 已上传的截图预览列表 -->
            <div v-if="completeForm.screenshots.length > 0" class="screenshot-preview-list">
              <div v-for="(item, index) in completeForm.screenshots" :key="item.id" class="screenshot-preview-item">
                <img :src="item.preview || item.url" class="screenshot-img" />
                <button class="screenshot-delete-btn" @click.stop="deleteScreenshot(index)" title="删除">
                  <el-icon><Close /></el-icon>
                </button>
              </div>
            </div>

            <!-- 上传区域 -->
            <div v-if="completeForm.screenshots.length === 0" class="simple-upload-area"
                 tabindex="0"
                 @dragover.prevent
                 @drop.prevent="handleDrop($event)"
                 @click="triggerUpload"
                 @paste="handlePaste($event)">
              <div class="upload-content">
                <el-icon class="upload-main-icon"><Plus /></el-icon>
                <span class="upload-main-text">点击、拖拽或粘贴截图</span>
                <span class="upload-sub-text">支持 Ctrl+V 粘贴</span>
              </div>
              <input type="file" ref="screenshotInput" style="display: none" accept="image/*" multiple @change="handleFileSelect" />
            </div>
          </div>

          <div v-if="completeForm.screenshots.length === 0" class="screenshot-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>请上传至少一张结单截图，可上传多张，确认完成后统一上传</span>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComplete" :loading="completing">确认完成</el-button>
      </template>
    </el-dialog>

    <!-- 编辑订单对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑订单" width="700px" class="order-dialog edit-order-dialog">
      <div v-if="currentOrder" class="edit-order-content">
        <div class="edit-section">
          <div class="edit-section-title">基本信息</div>
          <el-descriptions :column="2" border :colon="true">
            <el-descriptions-item label="订单号">
              <span class="order-no-display">{{ currentOrder.orderNo }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="订单类型">
              <el-tag :type="currentOrder.playerCount === 'DOUBLE' ? 'warning' : 'primary'" effect="light" size="small">
                {{ currentOrder.playerCount === 'DOUBLE' ? '双人订单' : '单人订单' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="老板名字">
              <el-input v-model="editForm.boss_info" placeholder="请输入老板名字" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="陪玩师">
              <span :class="{ 'no-player': !currentOrder.currentPlayerNickname }">
                {{ currentOrder.currentPlayerNickname || '待分配' }}
                <template v-if="currentOrder.currentPlayer2Nickname">
                  / {{ currentOrder.currentPlayer2Nickname }}
                </template>
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="服务内容" :span="2">
              <el-input v-model="editForm.service_content" placeholder="请输入服务内容" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="服务时长">
              <el-input-number v-model="editForm.service_hours" :min="0.5" :max="24" :step="0.5" size="small" style="width: 100%" :disabled="currentOrder.orderType === 'huhang'" @change="calculateEditTotalPrice" />
            </el-descriptions-item>
            <el-descriptions-item label="单价">
              <el-input-number v-model="editForm.price_per_hour" :min="1" :max="1000" :precision="2" size="small" style="width: 100%" :disabled="currentOrder.orderType === 'huhang'" @change="calculateEditTotalPrice" />
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="edit-section">
          <div class="edit-section-title">金额信息</div>
          <el-descriptions :column="2" border :colon="true">
            <el-descriptions-item label="预计总价">
              <el-input-number v-model="editForm.total_amount" :min="0" :max="100000" :precision="2" size="small" style="width: 100%" @change="calculateExpectedIncome" />
            </el-descriptions-item>
            <el-descriptions-item label="实际时长">
              <template v-if="currentOrder.orderType === 'huhang'">
                <span>1小时</span>
              </template>
              <div v-else class="edit-duration-group">
                <el-input-number v-model="editForm.actual_hours" :min="0" :max="23" :step="1" size="small" style="width: 90px" @change="calculateActualAmounts" />
                <span class="edit-duration-unit">时</span>
                <el-input-number v-model="editForm.actual_minutes" :min="0" :max="59" :step="1" size="small" style="width: 90px" @change="calculateActualAmounts" />
                <span class="edit-duration-unit">分</span>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="预计收入">
              <el-input-number v-model="editForm.expected_income_amount" :min="0" :max="100000" :precision="2" size="small" style="width: 100%" />
              <div class="edit-field-hint">{{ currentOrder.playerCount === 'DOUBLE' ? '单人预计收入' : '陪玩师预计收入' }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="实际总价">
              <el-input-number v-model="editForm.actual_total_amount" :min="0" :max="100000" :precision="2" size="small" style="width: 100%" />
              <div class="edit-field-hint">不填则系统自动计算</div>
            </el-descriptions-item>
            <el-descriptions-item label="实际收入">
              <el-input-number v-model="editForm.actual_income_amount" :min="0" :max="100000" :precision="2" size="small" style="width: 100%" />
              <div class="edit-field-hint">{{ currentOrder.playerCount === 'DOUBLE' ? '单人实际收入，不填则自动计算' : '陪玩师实际收入，不填则自动计算' }}</div>
            </el-descriptions-item>
            <el-descriptions-item v-if="editForm.deposit_deduct_amount" label="单抵金额">
              <span style="color: #e6a23c; font-weight: 500;">-¥{{ editForm.deposit_deduct_amount }}</span>
              <div class="edit-field-hint">{{ currentOrder.playerCount === 'DOUBLE' ? '双人单抵合计' : '本次订单扣押金' }}</div>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit" :loading="submittingAction === 'edit'">保存修改</el-button>
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

    <!-- 暂存订单对话框 -->
    <el-dialog v-model="pauseDialogVisible" title="暂存订单" width="400px" class="order-dialog pause-dialog">
      <div class="pause-confirm-content">
        <el-icon class="pause-icon"><VideoPause /></el-icon>
        <p class="pause-tip">确认暂存订单 <strong>{{ currentOrder?.orderNo }}</strong> ？</p>
        <p class="pause-sub-tip">暂存后可随时恢复订单继续服务</p>
      </div>
      <template #footer>
        <el-button @click="pauseDialogVisible = false">取 消</el-button>
        <el-button type="warning" @click="submitPause">确认暂存</el-button>
      </template>
    </el-dialog>

    <!-- 图片预览对话框 -->
    <el-dialog 
      v-model="previewVisible" 
      width="auto"
      class="image-preview-dialog" 
      :show-close="true"
      destroy-on-close
      align-center
    >
      <img :src="previewImageUrl" class="preview-image" />
    </el-dialog>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="700px" class="order-dialog">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="currentOrder.status === 4 ? getAuditStatusType(currentOrder.auditStatus) : getStatusType(currentOrder.status)" effect="light" size="small">
            {{ currentOrder.status === 4 ? getAuditStatusText(currentOrder.auditStatus) : getStatusText(currentOrder.status) }}
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
          <el-descriptions-item label="服务时长">{{ formatHoursToHM(currentOrder.serviceHours) }}</el-descriptions-item>
          <el-descriptions-item label="单价">¥{{ currentOrder.pricePerHour }}/小时</el-descriptions-item>
          <el-descriptions-item label="订单预计总价">
            <span class="detail-price">¥{{ currentOrder.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="实际时长">{{ currentOrder.actualHours ? formatHoursToHM(currentOrder.actualHours) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前陪玩师">{{ currentOrder.currentPlayerNickname || '待分配' }}</el-descriptions-item>
          <el-descriptions-item v-if="currentOrder.status === 4 && currentOrder.actualTotalAmount" label="订单实际总价">
            <span class="detail-actual-price">¥{{ currentOrder.actualTotalAmount }}</span>
          </el-descriptions-item>
        </template>
        
        <!-- 双人订单显示 -->
        <template v-if="currentOrder.playerCount === 'DOUBLE'">
          <el-descriptions-item label="服务项目" :span="2">{{ currentOrder.serviceContent }}</el-descriptions-item>
          <el-descriptions-item label="预估时长">{{ formatHoursToHM(currentOrder.serviceHours) }}</el-descriptions-item>
          <el-descriptions-item label="订单预计总价">
            <span class="detail-price">¥{{ currentOrder.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="陪玩师1">{{ currentOrder.currentPlayerNickname || '待分配' }}</el-descriptions-item>
          <el-descriptions-item label="陪玩师2">{{ currentOrder.currentPlayer2Nickname || '待分配' }}</el-descriptions-item>
          <el-descriptions-item v-if="currentOrder.status === 4 && currentOrder.actualTotalAmount" label="订单实际总价">
            <span class="detail-actual-price">¥{{ currentOrder.actualTotalAmount }}</span>
          </el-descriptions-item>
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
                  <span>{{ session.actualHours ? formatHoursToHM(session.actualHours) : '-' }}</span>
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
        
        <!-- 完成订单截图展示 -->
        <template v-if="currentOrder.status === 4 && detailScreenshotUrls.length > 0">
          <el-descriptions-item label="完成截图" :span="2">
            <div class="detail-screenshot-list">
              <img 
                v-for="url in detailScreenshotUrls" 
                :key="url"
                :src="url"
                class="detail-screenshot-img"
                @click="openImagePreview(url)"
              />
            </div>
          </el-descriptions-item>
        </template>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 补单对话框 -->
    <el-dialog v-model="replenishDialogVisible" title="补单（仅记录，不影响财务）" width="600px" class="order-dialog">
      <el-alert type="warning" :closable="false" style="margin-bottom: 16px">
        补单仅创建订单记录，不会触发任何财务计算（陪玩师收入、老板余额、财务记录等均不受影响）。
      </el-alert>
      <el-form :model="replenishForm" ref="replenishFormRef" label-width="110px" class="order-form">
        <el-form-item label="原始订单号">
          <el-input v-model="replenishForm.originalOrderNo" placeholder="可选，填写原始订单号便于追溯" />
        </el-form-item>
        <el-form-item label="老板信息" required>
          <el-input v-model="replenishForm.bossInfo" placeholder="请输入老板信息" />
        </el-form-item>
        <el-form-item label="服务内容" required>
          <el-input v-model="replenishForm.serviceContent" placeholder="请输入服务内容" />
        </el-form-item>
        <el-form-item label="订单类型">
          <el-radio-group v-model="replenishForm.orderType">
            <el-radio-button label="peiwand">陪玩单</el-radio-button>
            <el-radio-button label="huhang">护航单</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="单人/双人">
          <el-radio-group v-model="replenishForm.playerCount">
            <el-radio-button label="single">单人</el-radio-button>
            <el-radio-button label="double">双人</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="服务时长(小时)" required>
          <el-input-number v-model="replenishForm.serviceHours" :min="0.5" :step="0.5" :precision="1" />
        </el-form-item>
        <el-form-item label="单价(元/小时)" required>
          <el-input-number v-model="replenishForm.pricePerHour" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="总金额" required>
          <el-input-number v-model="replenishForm.totalAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="实际时长(小时)">
          <el-input-number v-model="replenishForm.actualHours" :min="0" :step="0.5" :precision="1" placeholder="不填则使用服务时长" />
        </el-form-item>
        <el-form-item label="陪玩师">
          <el-select v-model="replenishForm.currentPlayerId" placeholder="选择陪玩师（可选）" clearable filterable style="width: 100%">
            <el-option v-for="p in availablePlayers" :key="p.id" :label="p.nickname + (p.level ? ' (' + p.level + ')' : '')" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="replenishForm.playerCount === 'double'" label="陪玩师2">
          <el-select v-model="replenishForm.currentPlayer2Id" placeholder="选择第二位陪玩师（可选）" clearable filterable style="width: 100%">
            <el-option v-for="p in availablePlayers" :key="p.id" :label="p.nickname + (p.level ? ' (' + p.level + ')' : '')" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="原始创建时间">
          <el-date-picker v-model="replenishForm.originalCreatedAt" type="datetime" placeholder="可选，填写原始订单的创建时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原始完成时间">
          <el-date-picker v-model="replenishForm.originalCompletedAt" type="datetime" placeholder="可选，填写原始订单的完成时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="replenishForm.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replenishDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReplenish" :loading="replenishLoading">确认补单</el-button>
      </template>
    </el-dialog>

    <!-- 已删除订单记录对话框 -->
    <el-dialog v-model="deletedBackupsDialogVisible" title="已删除订单记录" width="900px" class="order-dialog">
      <el-table :data="deletedBackups" v-loading="deletedBackupsLoading" stripe max-height="500">
        <el-table-column prop="orderNo" label="订单号" min-width="130" />
        <el-table-column prop="bossInfo" label="老板信息" min-width="100" show-overflow-tooltip />
        <el-table-column prop="serviceContent" label="服务内容" min-width="120" show-overflow-tooltip />
        <el-table-column label="金额" min-width="80">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="删除时状态" min-width="90" />
        <el-table-column label="陪玩师" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.currentPlayerNickname || '-' }}
            <template v-if="row.currentPlayer2Nickname"> / {{ row.currentPlayer2Nickname }}</template>
          </template>
        </el-table-column>
        <el-table-column label="删除时间" min-width="130">
          <template #default="{ row }">{{ formatDate(row.deletedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleReplenishFromBackup(row)">补单</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!deletedBackupsLoading && deletedBackups.length === 0" description="暂无已删除订单记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getOrders, getOrderById, getMyInServiceOrders, createOrder, updateOrder, assignOrder, acceptOrder, completeOrder, cancelOrder, pauseOrder, resumeOrder, batchDeleteOrders, replenishOrder, getDeletedBackups, getDeletedBackup, auditPassOrder, batchAuditPassOrders } from '@/api/orders'
import { publishToHall, withdrawFromHall } from '@/api/grabHall'
import { getPlayers } from '@/api/users'
import { getLevelPrices, getSystemOptions, getSystemConfig } from '@/api/system'
import { getBosses, getVipLevels, createBoss } from '@/api/boss'
import dayjs from 'dayjs'
import { Document, Plus, CircleCheck, CircleClose, Timer, Loading, Check, Position, RefreshRight, Calendar, View, User, Delete, Warning, Picture, InfoFilled, Upload, Clock, Edit, Search, Close, VideoPause, Tickets, RefreshLeft, Operation, ArrowDown } from '@element-plus/icons-vue'

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
const searchKeyword = ref('')
const availablePlayers = ref([])
const levelPrices = ref([])
const precautions = ref([])
const serviceItems = ref([])
const bosses = ref([])
const vipLevels = ref([])
const platformFeeRate = ref(0.2)

// 启用的老板列表
const enabledBosses = computed(() => {
  return bosses.value.filter(boss => boss.enabled !== false)
})

// 选中的老板
const selectedBoss = computed(() => {
  if (!createForm.boss_id) return null
  return bosses.value.find(boss => boss.id === createForm.boss_id)
})

// 计算服务时长（小时）
const calculatedServiceHours = computed(() => {
  const hours = createForm.service_hours || 0
  const minutes = createForm.service_minutes || 0
  return hours + minutes / 60
})

// 计算服务价格（基于时长）
const calculatedServicePrice = computed(() => {
  return calculatedServiceHours.value * createForm.price_per_hour
})

// 计算原价
const calculatedOriginalPrice = computed(() => {
  return calculatedServicePrice.value
})

// 计算折扣后价格
const calculatedDiscountedPrice = computed(() => {
  const original = calculatedOriginalPrice.value
  if (createForm.customer_type !== 'REGULAR' || !selectedBoss.value) {
    return original
  }
  const discount = getVipDiscount(selectedBoss.value.vipLevel)
  return original * discount
})

// 获取当前月份的天数
const getDaysInMonth = computed(() => {
  const month = createForm.scheduled_month
  if (!month) return 31
  const year = dayjs().year()
  const daysInMonth = dayjs(`${year}-${month}`).daysInMonth()
  return daysInMonth
})

// 更新预约时间
const updateScheduledTime = () => {
  if (createForm.scheduled_month && createForm.scheduled_day && createForm.scheduled_hour !== null) {
    const year = dayjs().year()
    const month = String(createForm.scheduled_month).padStart(2, '0')
    const day = String(createForm.scheduled_day).padStart(2, '0')
    const hour = String(createForm.scheduled_hour).padStart(2, '0')
    const minute = String(createForm.scheduled_minute || 0).padStart(2, '0')
    createForm.scheduled_time = `${year}-${month}-${day} ${hour}:${minute}:00`
  } else {
    createForm.scheduled_time = ''
  }
}

const orderStats = ref({
  pending: 0,
  waiting: 0,
  inService: 0,
  paused: 0,
  pendingAudit: 0,
  audited: 0,
  cancelled: 0
})

const createDialogVisible = ref(false)
const submittingAction = ref('')
const assignDialogVisible = ref(false)
const completeDialogVisible = ref(false)
const cancelDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const currentOrder = ref(null)
const createFormRef = ref()
const cancelFormRef = ref()
const selectedOrders = ref([])

const selectedCompletedOrders = computed(() => {
  return selectedOrders.value.filter(o => o.status === 4 && o.auditStatus !== 1)
})

const replenishDialogVisible = ref(false)
const replenishLoading = ref(false)
const replenishFormRef = ref()
const replenishForm = reactive({
  originalOrderNo: '',
  bossInfo: '',
  serviceContent: '',
  orderType: 'peiwand',
  playerCount: 'single',
  serviceHours: 1,
  pricePerHour: 50,
  totalAmount: 50,
  actualHours: null,
  currentPlayerId: null,
  currentPlayer2Id: null,
  originalCreatedAt: null,
  originalCompletedAt: null,
  remark: ''
})

const deletedBackupsDialogVisible = ref(false)
const deletedBackupsLoading = ref(false)
const deletedBackups = ref([])

const createForm = reactive({
  boss_info: '',
  service_type: 'peiwand',
  player_level: '',
  precautions: [],
  service_content: '',
  player_count: 'single',
  service_hours: 1,
  service_minutes: 0,
  price_per_hour: 50,
  total_amount: 50,
  manual_total_amount: 50,
  scheduled_time: '',
  scheduled_month: null,
  scheduled_day: null,
  scheduled_hour: null,
  scheduled_minute: null,
  remark: '',
  customer_type: 'SCATTER',
  boss_id: null,
  use_balance: false
})

const createRules = {
  boss_info: [{ required: true, message: '请输入老板名字', trigger: 'change' }],
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

const assignFilter = reactive({
  level: '',
  status: ''
})

const assignFilterLevels = computed(() => {
  const levels = [...new Set(availablePlayers.value.map(p => p.level).filter(Boolean))]
  // 按照系统设置的等级排序（sortOrder）进行排序
  return levels.sort((a, b) => {
    const levelA = levelPrices.value.find(l => l.level === a)
    const levelB = levelPrices.value.find(l => l.level === b)
    const sortOrderA = levelA ? levelA.sortOrder : 999999
    const sortOrderB = levelB ? levelB.sortOrder : 999999
    return sortOrderA - sortOrderB
  })
})

const filteredPlayers = computed(() => {
  let list = availablePlayers.value
  if (assignFilter.level) {
    list = list.filter(p => p.level === assignFilter.level)
  }
  if (assignFilter.status === 'idle') {
    list = list.filter(p => !p.activeOrdersCount || p.activeOrdersCount === 0)
  } else if (assignFilter.status === 'busy') {
    list = list.filter(p => p.activeOrdersCount && p.activeOrdersCount > 0)
  }
  return list
})

const filterAvailablePlayers = () => {
}

const getLevelTagType = (level) => {
  const types = {
    '机密娱乐': '',
    '绝密娱乐': 'success',
    '机密技术': 'warning',
    '机密金牌': 'danger',
    '机密巅峰': ''
  }
  return types[level] || 'info'
}

const completeForm = reactive({
  actual_hours: 1,
  actual_minutes: 0,
  // 结单截图列表（支持多张）
  screenshots: [], // 每项: { preview: '', file: null, url: '' }
})

const completeRules = {
  actual_hours: [{ required: true, message: '请输入实际时长', trigger: 'blur' }],
  screenshots: [{ required: true, message: '请上传至少一张结单截图', trigger: 'change' }]
}

const completeFormRef = ref()
const screenshotInput = ref()
const completing = ref(false)
const activeUploadCard = ref(null)

// 上传请求头，添加认证token
const uploadHeaders = computed(() => {
  const token = sessionStorage.getItem('token')
  return {
    Authorization: token ? `Bearer ${token}` : ''
  }
})

const cancelForm = reactive({
  reason: ''
})

const cancelRules = {
  reason: [{ required: true, message: '请输入取消原因', trigger: 'blur' }]
}

const pauseDialogVisible = ref(false)
const pauseFormRef = ref()
const pauseForm = reactive({
  reason: ''
})

const pauseRules = {
  reason: [{ required: true, message: '请输入暂存原因', trigger: 'blur' }]
}

const editDialogVisible = ref(false)
const editForm = reactive({
  boss_info: '',
  service_content: '',
  service_hours: 1,
  price_per_hour: 50,
  total_amount: 50,
  expected_income_amount: null,
  actual_hours: null,
  actual_minutes: null,
  actual_total_amount: null,
  actual_income_amount: null,
  deposit_deduct_amount: null
})

const editRules = {}

const getStatusType = (status) => {
  const types = {
    0: 'info',    // PENDING_ASSIGN: 待分配
    1: 'warning', // PENDING_ACCEPT: 待接单
    2: 'warning', // PENDING_ACCEPT_2: 待接单2
    3: 'primary', // IN_SERVICE: 服务中
    4: 'success', // COMPLETED: 已完成
    5: 'danger',  // CANCELLED: 已取消
    6: ''         // PAUSED: 暂存
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
    5: '已取消',      // CANCELLED
    6: '暂存'         // PAUSED
  }
  return texts[status] || '未知'
}

const getAuditStatusType = (auditStatus) => {
  return auditStatus === 1 ? 'success' : 'warning'
}

const getAuditStatusText = (auditStatus) => {
  return auditStatus === 1 ? '已审核' : '待审核'
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
    paused: orders.value.filter(o => o.status === 6).length,
    pendingAudit: orders.value.filter(o => o.status === 4 && o.auditStatus !== 1).length,
    audited: orders.value.filter(o => o.status === 4 && o.auditStatus === 1).length,
    cancelled: orders.value.filter(o => o.status === 5).length
  }
}

const loadOrders = async () => {
  loading.value = true
  try {
    let apiStatus = statusFilter.value
    if (apiStatus === 'pending_audit' || apiStatus === 'audited') {
      apiStatus = 4
    }
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      status: apiStatus !== '' ? apiStatus : undefined,
      sortBy: 'createdAt',
      sortOrder: 'desc'
    }
    if (dateFilterType.value === 'today') {
      params.today = true
    } else if (dateFilterType.value === 'dateRange' && dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    if (searchKeyword.value && searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }
    const res = await getOrders(params)
    let list = Array.isArray(res.data) ? res.data : (res.data?.list || [])
    if (statusFilter.value === 'pending_audit') {
      list = list.filter(o => o.auditStatus !== 1)
    } else if (statusFilter.value === 'audited') {
      list = list.filter(o => o.auditStatus === 1)
    }
    orders.value = list
    if (isPlayer.value) {
      const statusPriority = { 3: 0, 1: 1, 2: 2, 6: 3, 0: 4, 4: 5, 5: 6 }
      orders.value.sort((a, b) => {
        const pa = statusPriority[a.status] ?? 99
        const pb = statusPriority[b.status] ?? 99
        if (pa !== pb) return pa - pb
        return new Date(b.createdAt) - new Date(a.createdAt)
      })
    }
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

const handleSearch = () => {
  page.value = 1
  loadOrders()
}

const handleClearSearch = () => {
  searchKeyword.value = ''
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

// 加载老板列表
const loadBosses = async () => {
  try {
    const res = await getBosses({ page: 1, pageSize: 1000 })
    bosses.value = res.data?.list || []
  } catch (error) {
    console.error('加载老板列表失败', error)
  }
}

// 加载VIP等级配置
const loadVipLevels = async () => {
  try {
    const res = await getVipLevels()
    vipLevels.value = res.data || []
  } catch (error) {
    console.error('加载VIP等级失败', error)
  }
}

// 获取VIP折扣 (返回折扣率，如 0.9 表示9折)
const getVipDiscount = (vipLevel) => {
  // 处理 null, undefined, 空字符串
  if (vipLevel === null || vipLevel === undefined || vipLevel === '') return 1.0
  // 支持数字或字符串类型的level
  const levelNum = Number(vipLevel)
  // VIP0 默认无折扣
  if (levelNum === 0) return 1.0
  const vip = vipLevels.value.find(v => v.level === levelNum || v.level === vipLevel)
  // 注意：后端字段名是 discountRate，不是 discount
  return vip ? Number(vip.discountRate || vip.discount) : 1.0
}

// 获取折扣显示文本 (如 "9折" 或 "无折扣")
const getDiscountText = (vipLevel) => {
  const discount = getVipDiscount(vipLevel)
  if (discount >= 1.0) return '无折扣'
  // 将 0.85 转换为 "8.5折"
  const discountNumber = (discount * 10).toFixed(1)
  // 如果末尾是.0，去掉
  const formatted = discountNumber.endsWith('.0') ? discountNumber.slice(0, -2) : discountNumber
  return `${formatted}折`
}

// 获取折扣率显示 (如 "90%" 或 "原价")
const getDiscountRateText = (vipLevel) => {
  const discount = getVipDiscount(vipLevel)
  if (discount >= 1.0) return '原价'
  const percentage = Math.round(discount * 100)
  return `${percentage}%`
}

// 客户类型改变处理
const handleCustomerTypeChange = () => {
  createForm.boss_id = null
  createForm.boss_info = ''
  createForm.use_balance = false
  if (createForm.customer_type === 'SCATTER') {
    calculateTotalPrice()
  }
}

const queryBossSuggestions = (queryString, cb) => {
  const results = queryString
    ? enabledBosses.value.filter(boss =>
        boss.name.toLowerCase().includes(queryString.toLowerCase())
      )
    : enabledBosses.value
  cb(results.map(boss => ({ ...boss, value: boss.name })))
}

const handleBossSelect = (item) => {
  createForm.boss_id = item.id
  createForm.boss_info = item.name
  if (item.balance > 0) {
    createForm.use_balance = true
  }
  const discountedPrice = calculatedDiscountedPrice.value
  createForm.manual_total_amount = Number(discountedPrice.toFixed(2))
}

const handleBossClear = () => {
  createForm.boss_id = null
  createForm.use_balance = false
}

const handleBossInput = (val) => {
  if (createForm.boss_id) {
    const boss = bosses.value.find(b => b.id === createForm.boss_id)
    if (!boss || boss.name !== val) {
      createForm.boss_id = null
      createForm.use_balance = false
    }
  }
  if (!createForm.boss_id && val && val.trim()) {
    const exactMatch = enabledBosses.value.find(b => b.name === val.trim())
    if (exactMatch) {
      createForm.boss_id = exactMatch.id
      if (exactMatch.balance > 0) {
        createForm.use_balance = true
      }
    }
  }
}

// 服务类型改变时重置相关字段
const handleServiceTypeChange = () => {
  createForm.player_level = ''
  createForm.precautions = []
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
  const hours = createForm.service_hours || 0
  const minutes = createForm.service_minutes || 0
  const totalHours = hours + minutes / 60
  createForm.manual_total_amount = Number((totalHours * createForm.price_per_hour).toFixed(2))
}

const submitCreate = async () => {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    let bossId = null
    if (createForm.customer_type === 'REGULAR') {
      if (createForm.boss_id) {
        bossId = createForm.boss_id
      } else if (createForm.boss_info && createForm.boss_info.trim()) {
        const res = await createBoss({
          name: createForm.boss_info.trim(),
          contactType: 'WECHAT',
          contactValue: '',
          customerType: 'REGULAR',
          vipLevel: 0
        })
        bossId = res.data?.id
        await loadBosses()
      }
    }

    let orderData = {
      bossInfo: createForm.boss_info,
      scheduledTime: createForm.scheduled_time ? dayjs(createForm.scheduled_time).format('YYYY-MM-DDTHH:mm:ss') : null,
      remark: createForm.remark,
      orderType: createForm.service_type,
      customerType: createForm.customer_type,
      bossId: bossId,
      useBalance: createForm.customer_type === 'REGULAR' ? createForm.use_balance : false
    }

    if (createForm.service_type === 'peiwand') {
      // 陪玩单
      let precautionsText = createForm.precautions && createForm.precautions.length > 0 ? `【${createForm.precautions.join('、')}】` : ''
      orderData.serviceContent = createForm.player_level + '陪玩' + precautionsText
      // 计算服务时长（小时+分钟转换为小时）
      const totalHours = (createForm.service_hours || 0) + (createForm.service_minutes || 0) / 60
      orderData.serviceHours = totalHours
      orderData.pricePerHour = createForm.price_per_hour
      // 计算原价和折扣
      const originalAmount = calculatedOriginalPrice.value
      orderData.originalAmount = originalAmount.toFixed(2)
      if (createForm.customer_type === 'REGULAR' && selectedBoss.value) {
        const discount = getVipDiscount(selectedBoss.value.vipLevel)
        orderData.discountRate = discount
      }
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

    // 双人陪玩单：创建两份相同订单
    const isDoublePeiwan = createForm.service_type === 'peiwand' && createForm.player_count === 'double'

    if (isDoublePeiwan) {
      // 创建第一份订单
      await createOrder(orderData)
      // 创建第二份相同订单
      await createOrder({ ...orderData })
      ElMessage.success('双人订单创建成功（已创建2份相同订单）')
    } else {
      await createOrder(orderData)
      ElMessage.success('订单创建成功')
    }

    createDialogVisible.value = false
    createFormRef.value?.resetFields()
    // 重置默认值
    createForm.service_type = 'peiwand'
    createForm.player_count = 'single'
    createForm.service_hours = 1
    createForm.service_minutes = 0
    createForm.price_per_hour = 50
    createForm.total_amount = 50
    createForm.manual_total_amount = 50
    createForm.customer_type = 'SCATTER'
    createForm.boss_id = null
    createForm.use_balance = false
    createForm.scheduled_month = null
    createForm.scheduled_day = null
    createForm.scheduled_hour = null
    createForm.scheduled_minute = null
    loadOrders()
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const handleAction = (cmd, row) => {
  const actions = {
    detail: handleDetail,
    edit: handleEdit,
    assign: handleAssign,
    publish: handlePublishToHall,
    withdraw: handleWithdrawFromHall,
    reassign: handleAssign,
    pause: handlePause,
    resume: handleResume,
    cancel: handleCancel,
    audit: handleAuditPass
  }
  if (actions[cmd]) actions[cmd](row)
}

const handleAssign = async (row) => {
  currentOrder.value = row
  const res = await getPlayers({ status: 'active', pageSize: 1000 })
  availablePlayers.value = res.data?.list || res.data || []
  assignForm.player_id = ''
  assignForm.player_id2 = ''
  assignFilter.level = ''
  assignFilter.status = ''
  assignDialogVisible.value = true
}

const handlePublishToHall = async (row) => {
  try {
    let priorityLevel = null
    if (row.orderType === 'huhang' && row.playerCount === 'SINGLE') {
      const { value } = await ElMessageBox.prompt(
        '可为单人护航单设置优先等级（高等级优先抢占），留空则不设置',
        '发布到抢单大厅',
        {
          confirmButtonText: '发布',
          cancelButtonText: '取消',
          inputPlaceholder: '输入等级名称（如：黄金），留空不设置',
          inputPattern: /^.{0,20}$/,
          inputErrorMessage: '等级名称最长20个字符'
        }
      ).catch(() => ({ value: null }))
      if (value === null) return
      priorityLevel = value.trim() || null
    } else {
      await ElMessageBox.confirm('确定将此订单发布到抢单大厅？', '发布到抢单大厅', {
        confirmButtonText: '发布',
        cancelButtonText: '取消',
        type: 'info'
      })
    }
    await publishToHall(row.id, { priorityLevel })
    ElMessage.success('发布成功')
    loadOrders()
  } catch (e) {
    if (e !== 'cancel') {
      // error handled by interceptor
    }
  }
}

const handleWithdrawFromHall = async (row) => {
  try {
    await ElMessageBox.confirm('确定将此订单从抢单大厅撤回？所有等待者将被清空。', '从大厅撤回', {
      confirmButtonText: '撤回',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await withdrawFromHall(row.id)
    ElMessage.success('撤回成功')
    loadOrders()
  } catch (e) {
    if (e !== 'cancel') {
      // error handled by interceptor
    }
  }
}

const handleDetail = async (row) => {
  try {
    const res = await getOrderById(row.id)
    currentOrder.value = res.data || row
  } catch (error) {
    currentOrder.value = row
  }
  detailDialogVisible.value = true
}

// 详情页截图URL列表（统一使用 screenshotUrls 字段，避免重复）
const detailScreenshotUrls = computed(() => {
  if (!currentOrder.value) return []
  if (currentOrder.value.screenshotUrls && currentOrder.value.screenshotUrls.length > 0) {
    return [...new Set(currentOrder.value.screenshotUrls)]
  }
  return []
})

// 图片预览
const previewVisible = ref(false)
const previewImageUrl = ref('')

const openImagePreview = (url) => {
  previewImageUrl.value = url
  previewVisible.value = true
}

const submitAssign = async () => {
  if (!assignForm.player_id) {
    ElMessage.warning('请选择陪玩师1')
    return
  }
  
  const isDoubleOrder = currentOrder.value?.playerCount === 'DOUBLE' || currentOrder.value?.playerCount === 'double'
  if (isDoubleOrder && !assignForm.player_id2) {
    ElMessage.warning('双人订单需要选择两个陪玩师')
    return
  }
  
  if (submittingAction.value) return
  submittingAction.value = 'assign'
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
  } finally {
    submittingAction.value = ''
  }
}

const handleAccept = async (row) => {
  if (submittingAction.value) return
  submittingAction.value = 'accept'
  try {
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
  } finally {
    submittingAction.value = ''
  }
}

const handleComplete = (row) => {
  currentOrder.value = row
  completeForm.actual_hours = Math.floor(row.serviceHours)
  completeForm.actual_minutes = Math.round((row.serviceHours - Math.floor(row.serviceHours)) * 60)
  // 重置所有截图相关字段
  completeForm.start_screenshot_preview = ''
  completeForm.end_screenshot_preview = ''
  completeForm.start_screenshot_file = null
  completeForm.end_screenshot_file = null
  completeForm.start_screenshot_url = ''
  completeForm.end_screenshot_url = ''
  completeDialogVisible.value = true
}

const formatDuration = (hours, minutes) => {
  const totalMinutes = (hours || 0) * 60 + (minutes || 0)
  const h = Math.floor(totalMinutes / 60)
  const m = totalMinutes % 60
  if (h === 0) {
    return `${m}分钟`
  } else if (m === 0) {
    return `${h}小时`
  } else {
    return `${h}小时${m}分钟`
  }
}

const formatHoursToHM = (hours) => {
  if (!hours && hours !== 0) return '-'
  const h = Math.floor(hours)
  const m = Math.round((hours - h) * 60)
  if (h === 0 && m === 0) return '0分钟'
  if (h === 0) return `${m}分钟`
  if (m === 0) return `${h}小时`
  return `${h}小时${m}分钟`
}

const handleCompleteDialogOpen = () => {
  // 添加全局粘贴事件监听
  document.addEventListener('paste', handleGlobalPaste)
  // 默认聚焦到开始截图
  activeUploadCard.value = 'start'
}

const handleCompleteDialogClose = () => {
  // 移除全局粘贴事件监听
  document.removeEventListener('paste', handleGlobalPaste)
  activeUploadCard.value = null
}

const handleGlobalPaste = (event) => {
  if (!completeDialogVisible.value) return

  const items = event.clipboardData?.items
  if (!items) return

  for (let item of items) {
    if (item.type.indexOf('image') !== -1) {
      const file = item.getAsFile()
      if (file) {
        addScreenshot(file)
      }
      break
    }
  }
}

const addScreenshot = (file) => {
  const localUrl = URL.createObjectURL(file)
  completeForm.screenshots.push({
    id: Date.now() + Math.random(), // 唯一ID
    preview: localUrl,
    file: file,
    url: ''
  })
}

const handleDrop = (event) => {
  const files = event.dataTransfer?.files
  if (!files || files.length === 0) return

  for (let i = 0; i < files.length; i++) {
    const file = files[i]
    if (file.type.indexOf('image') !== -1) {
      addScreenshot(file)
    }
  }
}

const triggerUpload = () => {
  screenshotInput.value?.click()
}

const deleteScreenshot = (index) => {
  const item = completeForm.screenshots[index]
  if (item.preview) {
    URL.revokeObjectURL(item.preview)
  }
  completeForm.screenshots.splice(index, 1)
}

const handleFileSelect = (event) => {
  const files = event.target.files
  if (!files || files.length === 0) return

  for (let i = 0; i < files.length; i++) {
    const file = files[i]
    if (file.type.indexOf('image') !== -1) {
      addScreenshot(file)
    }
  }
  // 清空input，允许重复选择同一文件
  event.target.value = ''
}

const uploadImage = async (file) => {
  const formData = new FormData()
  formData.append('file', file)

  try {
    const response = await fetch('/api/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${sessionStorage.getItem('token')}`
      },
      body: formData
    })

    const result = await response.json()

    if (result.success !== false && result.data) {
      return result.data
    } else {
      ElMessage.error(result.message || '上传失败')
      return null
    }
  } catch (error) {
    ElMessage.error('上传失败')
    return null
  }
}

const submitComplete = async () => {
  const valid = await completeFormRef.value?.validate().catch(() => false)
  if (!valid) return

  if (completeForm.screenshots.length === 0) {
    ElMessage.error('请上传至少一张结单截图')
    return
  }

  if (submittingAction.value) return
  submittingAction.value = 'complete'

  completing.value = true
  try {
    // 上传所有未上传的截图
    const screenshotUrls = []
    for (let i = 0; i < completeForm.screenshots.length; i++) {
      const item = completeForm.screenshots[i]
      if (item.url) {
        // 已上传过，直接使用
        screenshotUrls.push(item.url)
      } else if (item.file) {
        // 需要上传
        const url = await uploadImage(item.file)
        if (url) {
          item.url = url
          screenshotUrls.push(url)
        } else {
          ElMessage.error(`第${i + 1}张截图上传失败，请重试`)
          return
        }
      }
    }

    const payload = {
      screenshotUrls: screenshotUrls
    }
    if (currentOrder.value?.orderType !== 'huhang') {
      const totalMinutes = (completeForm.actual_hours || 0) * 60 + (completeForm.actual_minutes || 0)
      payload.actualHours = totalMinutes / 60
    }
    await completeOrder(currentOrder.value.id, payload)
    ElMessage.success('订单完成')
    completeDialogVisible.value = false
    // 移除全局粘贴事件监听
    document.removeEventListener('paste', handleGlobalPaste)
    // 释放所有本地预览URL
    completeForm.screenshots.forEach(item => {
      if (item.preview) {
        URL.revokeObjectURL(item.preview)
      }
    })
    // 重置表单
    completeForm.screenshots = []
    loadOrders()
  } catch (error) {
    // 错误已在request.js中处理
  } finally {
    completing.value = false
    submittingAction.value = ''
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

const handleAuditPass = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定审核通过订单 ${row.orderNo} 吗？审核通过后将发放陪玩师收入。`,
      '审核确认',
      { confirmButtonText: '审核通过', cancelButtonText: '取消', type: 'info' }
    )
    await auditPassOrder(row.id)
    ElMessage.success('审核通过')
    loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      // handled by interceptor
    }
  }
}

const handleBatchAuditPass = async () => {
  const orders = selectedCompletedOrders.value
  if (orders.length === 0) {
    ElMessage.warning('请先选择待审核的订单')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定批量审核通过 ${orders.length} 个订单吗？审核通过后将发放陪玩师收入。`,
      '批量审核确认',
      { confirmButtonText: '全部通过', cancelButtonText: '取消', type: 'info' }
    )
    const ids = orders.map(o => o.id)
    await batchAuditPassOrders(ids)
    ElMessage.success('批量审核成功')
    selectedOrders.value = []
    loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      // handled by interceptor
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

const handlePause = (row) => {
  currentOrder.value = row
  pauseDialogVisible.value = true
}

const submitPause = async () => {
  try {
    await pauseOrder(currentOrder.value.id, { reason: '' })
    ElMessage.success('订单已暂存')
    pauseDialogVisible.value = false
    loadOrders()
  } catch (error) {
    ElMessage.error('暂存失败')
  }
}

const handleViewDeletedBackups = async () => {
  deletedBackupsDialogVisible.value = true
  deletedBackupsLoading.value = true
  try {
    const res = await getDeletedBackups()
    deletedBackups.value = res.data || []
  } catch (error) {
    ElMessage.error('获取已删除记录失败')
  } finally {
    deletedBackupsLoading.value = false
  }
}

const handleReplenishFromBackup = (backup) => {
  replenishForm.originalOrderNo = backup.orderNo || ''
  replenishForm.bossInfo = backup.bossInfo || ''
  replenishForm.serviceContent = backup.serviceContent || ''
  replenishForm.orderType = backup.orderType || 'peiwand'
  replenishForm.playerCount = backup.playerCount || 'single'
  replenishForm.serviceHours = backup.serviceHours || 1
  replenishForm.pricePerHour = backup.pricePerHour || 50
  replenishForm.totalAmount = backup.totalAmount || 50
  replenishForm.actualHours = backup.actualHours || null
  replenishForm.currentPlayerId = backup.currentPlayerId || null
  replenishForm.currentPlayer2Id = backup.currentPlayer2Id || null
  replenishForm.originalCreatedAt = backup.createdAt || null
  replenishForm.originalCompletedAt = backup.completedAt || null
  replenishForm.remark = ''
  deletedBackupsDialogVisible.value = false
  replenishDialogVisible.value = true
}

const submitReplenish = async () => {
  if (!replenishForm.bossInfo || !replenishForm.serviceContent) {
    ElMessage.warning('请填写老板信息和服务内容')
    return
  }

  replenishLoading.value = true
  try {
    await replenishOrder({
      bossInfo: replenishForm.bossInfo,
      serviceContent: replenishForm.serviceContent,
      serviceHours: replenishForm.serviceHours,
      pricePerHour: replenishForm.pricePerHour,
      totalAmount: replenishForm.totalAmount,
      actualHours: replenishForm.actualHours,
      orderType: replenishForm.orderType,
      playerCount: replenishForm.playerCount,
      currentPlayerId: replenishForm.currentPlayerId,
      currentPlayer2Id: replenishForm.currentPlayer2Id,
      originalOrderNo: replenishForm.originalOrderNo,
      originalCreatedAt: replenishForm.originalCreatedAt ? dayjs(replenishForm.originalCreatedAt).format('YYYY-MM-DDTHH:mm:ss') : null,
      originalCompletedAt: replenishForm.originalCompletedAt ? dayjs(replenishForm.originalCompletedAt).format('YYYY-MM-DDTHH:mm:ss') : null,
      remark: replenishForm.remark
    })
    ElMessage.success('补单成功')
    replenishDialogVisible.value = false
    Object.assign(replenishForm, {
      originalOrderNo: '',
      bossInfo: '',
      serviceContent: '',
      orderType: 'peiwand',
      playerCount: 'single',
      serviceHours: 1,
      pricePerHour: 50,
      totalAmount: 50,
      actualHours: null,
      currentPlayerId: null,
      currentPlayer2Id: null,
      originalCreatedAt: null,
      originalCompletedAt: null,
      remark: ''
    })
    loadOrders()
  } catch (error) {
    ElMessage.error('补单失败')
  } finally {
    replenishLoading.value = false
  }
}

const handleResume = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确定要恢复此订单吗？',
      '恢复订单',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    await resumeOrder(row.id)
    ElMessage.success('订单已恢复')
    loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('恢复失败')
    }
  }
}

const handleEdit = async (row) => {
  try {
    const res = await getOrderById(row.id)
    currentOrder.value = res.data || row
  } catch (error) {
    currentOrder.value = row
  }
  const order = currentOrder.value
  const isHuhang = order.orderType === 'huhang'
  editForm.boss_info = order.bossInfo || ''
  editForm.service_content = order.serviceContent || ''
  editForm.service_hours = isHuhang ? 1 : (order.serviceHours || 1)
  editForm.price_per_hour = isHuhang ? (order.totalAmount || 50) : (order.pricePerHour || 50)
  editForm.total_amount = order.totalAmount || 0
  if (isHuhang) {
    editForm.actual_hours = 1
    editForm.actual_minutes = 0
  } else if (order.actualHours) {
    editForm.actual_hours = Math.floor(order.actualHours)
    editForm.actual_minutes = Math.round((order.actualHours - Math.floor(order.actualHours)) * 60)
  } else {
    editForm.actual_hours = null
    editForm.actual_minutes = null
  }
  const isCompleted = order.status === 4
  editForm.actual_total_amount = isCompleted ? (order.actualTotalAmount || null) : null
  editForm.actual_income_amount = isCompleted ? (order.actualIncomeAmount || null) : null
  editForm.expected_income_amount = order.expectedIncomeAmount || null
  editForm.deposit_deduct_amount = isCompleted ? (order.depositDeductAmount || null) : null
  if (!platformFeeRate.value || platformFeeRate.value === 0.2) {
    getSystemConfig().then(res => {
      if (res.data?.platformFeeRate != null) {
        platformFeeRate.value = Number(res.data.platformFeeRate)
      }
    }).catch(() => {})
  }
  editDialogVisible.value = true
}

const calculateEditTotalPrice = () => {
  editForm.total_amount = Number((editForm.service_hours * editForm.price_per_hour).toFixed(2))
  calculateExpectedIncome()
}

const calculateExpectedIncome = () => {
  const total = editForm.total_amount || 0
  const expected = Number((total * (1 - platformFeeRate.value)).toFixed(2))
  editForm.expected_income_amount = currentOrder.value?.playerCount === 'DOUBLE'
    ? Number((expected / 2).toFixed(2))
    : expected
}

const calculateActualAmounts = () => {
  const actualHoursTotal = (editForm.actual_hours || 0) + (editForm.actual_minutes || 0) / 60
  const serviceHours = editForm.service_hours || 0
  const pricePerHour = editForm.price_per_hour || 0
  const totalAmount = editForm.total_amount || 0

  if (actualHoursTotal <= 0) {
    editForm.actual_total_amount = null
    editForm.actual_income_amount = null
    return
  }

  let actualTotalAmount = totalAmount

  if (currentOrder.value?.orderType !== 'huhang' && actualHoursTotal > serviceHours) {
    const extraMinutes = (actualHoursTotal - serviceHours) * 60
    const totalExtraMinutes = Math.round(extraMinutes)
    const fullHours = Math.floor(totalExtraMinutes / 60)
    const remainingMinutes = totalExtraMinutes % 60

    let extraFee = fullHours * pricePerHour
    if (remainingMinutes > 15 && remainingMinutes <= 45) {
      extraFee += pricePerHour * 0.5
    } else if (remainingMinutes > 45) {
      extraFee += pricePerHour
    }

    actualTotalAmount = totalAmount + extraFee
  }

  editForm.actual_total_amount = Number(actualTotalAmount.toFixed(2))
  const totalIncome = Number((actualTotalAmount * (1 - platformFeeRate.value)).toFixed(2))
  editForm.actual_income_amount = currentOrder.value?.playerCount === 'DOUBLE'
    ? Number((totalIncome / 2).toFixed(2))
    : totalIncome
}

const submitEdit = async () => {
  if (submittingAction.value) return
  submittingAction.value = 'edit'
  try {
    const updateData = {}

    if (editForm.boss_info && editForm.boss_info.trim()) {
      updateData.bossInfo = editForm.boss_info
    }
    if (editForm.service_content && editForm.service_content.trim()) {
      updateData.serviceContent = editForm.service_content
    }
    if (editForm.service_hours != null) {
      updateData.serviceHours = editForm.service_hours
    }
    if (editForm.price_per_hour != null) {
      updateData.pricePerHour = editForm.price_per_hour
    }
    if (editForm.total_amount != null) {
      updateData.totalAmount = editForm.total_amount
    }
    if (editForm.actual_hours != null || editForm.actual_minutes != null) {
      const hours = (editForm.actual_hours || 0) + (editForm.actual_minutes || 0) / 60
      updateData.actualHours = hours
    }
    if (editForm.actual_total_amount != null) {
      updateData.actualTotalAmount = editForm.actual_total_amount
    }
    if (editForm.actual_income_amount != null) {
      const isDouble = currentOrder.value?.playerCount === 'DOUBLE'
      updateData.actualIncomeAmount = isDouble
        ? Number((editForm.actual_income_amount * 2).toFixed(2))
        : editForm.actual_income_amount
    }

    await updateOrder(currentOrder.value.id, updateData)
    ElMessage.success('订单修改成功')
    editDialogVisible.value = false
    loadOrders()
  } catch (error) {
    // 错误由 request.js 全局拦截器统一处理
  } finally {
    submittingAction.value = ''
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

watch(replenishDialogVisible, async (val) => {
  if (val) {
    try {
      const res = await getPlayers({ status: 'active', pageSize: 1000 })
      availablePlayers.value = res.data?.list || res.data || []
    } catch (e) {
      // ignore
    }
  }
})

const scheduleIdle = window.requestIdleCallback || ((cb) => setTimeout(cb, 200))

const preloadConfigData = () => {
  loadLevelPrices()
  loadPrecautions()
  loadServiceItems()
  if (!isPlayer.value) {
    loadBosses()
    loadVipLevels()
    getSystemConfig().then(res => {
      if (res.data?.platformFeeRate != null) {
        platformFeeRate.value = Number(res.data.platformFeeRate)
      }
    }).catch(() => {})
  }
}

const handleOpenCreate = () => {
  if (!levelPrices.value.length) preloadConfigData()
  createDialogVisible.value = true
}

onMounted(() => {
  loadOrders()
  scheduleIdle(preloadConfigData)
  timerInterval = setInterval(() => {
    now.value = dayjs()
  }, 1000)
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
  padding: 0;
  background: transparent;
  min-height: 100vh;
}

.order-card {
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-card);
  background: var(--bg-card);
  
  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(90deg, var(--primary-bg) 0%, transparent 100%);
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
    color: var(--primary-color);
  }
  
  .title-text {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary);
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

// 统计卡片容器
.stats-row {
  display: flex;
  gap: 15px;
  margin-bottom: 25px;
  flex-wrap: wrap;
}

.search-row {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 20px;
  
  .search-input {
    width: 300px;
  }
}

.stat-card {
  flex: 1;
  min-width: 150px;
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 16px 18px;
  border-radius: var(--border-radius);
  background: var(--bg-card);
  box-shadow: var(--shadow-card);
  transition: all 0.3s;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-card-hover);
  }
  
  .stat-icon {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    flex-shrink: 0;
  }
  
  .stat-info {
    flex: 1;
  }
  
  .stat-label {
    font-size: 12px;
    color: var(--text-secondary);
    margin-bottom: 4px;
  }
  
  .stat-value {
    font-size: 20px;
    font-weight: 700;
    color: var(--text-primary);
  }
  
  &.pending .stat-icon {
    background: var(--info-bg);
    color: var(--info-dark);
  }
  
  &.waiting .stat-icon {
    background: var(--warning-bg);
    color: var(--warning-dark);
  }
  
  &.in-service .stat-icon {
    background: var(--success-bg);
    color: var(--success-color);
  }
  
  &.paused .stat-icon {
    background: #f4f4f5;
    color: #909399;
  }
  
  &.completed .stat-icon {
    background: var(--warning-bg);
    color: var(--warning-dark);
  }
  
  &.audited .stat-icon {
    background: var(--success-bg);
    color: var(--success-color);
  }
  
  &.cancelled .stat-icon {
    background: var(--danger-bg);
    color: var(--danger-dark);
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
      color: var(--warning-dark);
      font-size: 14px;
      
      .el-icon {
        font-size: 16px;
      }
    }
  }
}

// VIP信息和价格样式
.vip-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  
  .discount-text {
    color: var(--warning-dark);
    font-weight: 600;
    font-size: 14px;
  }
  
  .discount-rate {
    color: var(--text-tertiary);
    font-size: 13px;
  }
  
  .balance-text {
    color: var(--success-color);
    font-weight: 600;
    font-size: 14px;
    margin-left: 10px;
  }
}

.original-price {
  font-size: 14px;
  color: var(--text-tertiary);
  text-decoration: line-through;
}

.discounted-price {
  font-size: 18px;
  color: var(--danger-dark);
  font-weight: 700;
}

.discount-info {
  font-size: 12px;
  color: var(--warning-dark);
  margin-top: 5px;
}

.balance-deduct-info {
  font-size: 12px;
  color: var(--success-color);
  margin-top: 5px;
}

.order-table {
  :deep(th.el-table__cell) {
    background: #f8f9fa !important;
    font-weight: 600 !important;
    color: var(--text-primary) !important;
    font-size: 13px;
    padding: 12px 8px;
  }
  
  :deep(td.el-table__cell) {
    padding: 10px 8px;
    border-bottom: 1px solid #f0f0f0;
  }
  
  :deep(tr:hover td.el-table__cell) {
    background: rgba(108, 92, 231, 0.04) !important;
  }
  
  .order-no-text {
    font-family: var(--font-mono);
    font-weight: 600;
    color: var(--primary-color);
  }

  .replenish-order {
    color: var(--warning-dark) !important;
  }
  
  .time-price {
    .time {
      font-weight: 600;
      color: var(--text-primary);
    }
    .price {
      font-size: 12px;
      color: var(--text-secondary);
    }
  }
  
  .total-amount {
    font-family: var(--font-mono);
    font-weight: 700;
    color: var(--danger-dark);
    font-size: 15px;
  }
  
  .amount-cell {
    display: flex;
    flex-direction: column;
    gap: 2px;
    align-items: center;
  }
  
  .actual-amount-tag {
    font-size: 12px;
    color: var(--warning-dark);
    font-weight: 600;
  }
  
  .no-player {
    color: var(--text-tertiary);
    font-style: italic;
  }
  
  .time-info {
    .create-time {
      color: var(--text-secondary);
    }
    .schedule-time {
      font-size: 12px;
      color: var(--primary-color);
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
  border-radius: var(--border-radius);
  transition: all 0.3s;
  
  &:hover {
    transform: translateY(-3px);
    box-shadow: var(--shadow-card-hover);
  }
  
  :deep(.el-card__header) {
    padding: 14px 18px;
    background: linear-gradient(90deg, var(--primary-bg) 0%, transparent 100%);
  }
}

.card-header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-no {
  font-family: var(--font-mono);
  font-weight: 700;
  color: var(--primary-color);
  font-size: 15px;
}

.order-content {
  .info-row {
    display: flex;
    margin-bottom: 10px;
    
    .info-label {
      width: 80px;
      color: var(--text-tertiary);
      font-size: 13px;
    }
    
    .info-value {
      flex: 1;
      color: var(--text-secondary);
      font-size: 14px;
    }
  }
  
  .price-row {
    .price-value {
      font-size: 18px;
      font-weight: 700;
      color: var(--danger-dark);
    }
  }
  
  .income-row {
    .income-value {
      font-weight: 600;
      color: var(--success-color);
    }
  }
  
  .actual-income-row {
    .actual-income-value {
      font-weight: 700;
      color: var(--danger-dark);
      font-size: 16px;
    }
  }
  
  .actual-hours {
    color: var(--primary-color);
    font-weight: 600;
  }
  
  .service-timer {
    margin: 15px 0;
    padding: 15px;
    background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
    border-radius: var(--border-radius);
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
      font-family: var(--font-mono);
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
  .duration-input-group {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    
    .duration-unit {
      color: var(--text-secondary);
      font-size: 14px;
      white-space: nowrap;
    }
    
    .duration-total {
      color: var(--primary-color);
      font-size: 14px;
      font-weight: 500;
      margin-left: 10px;
    }
  }
  
  // 截图上传样式 - 商务简约风格
  .screenshot-section {
    background: #fafbfc;
    border-radius: 8px;
    padding: 20px;
    margin-top: 20px;
    border: 1px solid #e4e7ed;

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;

      .section-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 15px;
        font-weight: 600;
        color: #303133;

        .el-icon {
          font-size: 18px;
          color: #606266;
        }
      }
    }

    // 简洁截图上传区域
    .simple-screenshot-container {
      display: flex;
      flex-direction: column;
      gap: 16px;

      // 已上传截图预览列表
      .screenshot-preview-list {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;

        .screenshot-preview-item {
          position: relative;
          width: 120px;
          height: 80px;
          border-radius: 6px;
          overflow: hidden;
          border: 1px solid #e4e7ed;
          cursor: pointer;

          &:hover {
            border-color: #409eff;

            .screenshot-delete-btn {
              display: flex;
            }
          }

          .screenshot-img {
            width: 100%;
            height: 100%;
            object-fit: cover;
          }

          .screenshot-delete-btn {
            position: absolute;
            top: 4px;
            right: 4px;
            width: 20px;
            height: 20px;
            border-radius: 50%;
            background: rgba(0, 0, 0, 0.5);
            border: none;
            cursor: pointer;
            display: none;
            align-items: center;
            justify-content: center;
            padding: 0;

            .el-icon {
              color: #fff;
              font-size: 12px;
            }

            &:hover {
              background: #f56c6c;
            }
          }
        }
      }

      // 上传区域 - 长方形
      .simple-upload-area {
        width: 100%;
        height: 100px;
        border: 2px dashed #dcdfe6;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.2s ease;
        background: #fafafa;
        outline: none;

        &:hover,
        &:focus {
          border-color: var(--primary-color);
          background: var(--primary-bg);
        }

        &:active {
          border-color: var(--primary-color);
          background: rgba(108, 92, 231, 0.12);
        }

        .upload-content {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 4px;

          .upload-main-icon {
            font-size: 24px;
            color: var(--primary-color);
          }

          .upload-main-text {
            font-size: 14px;
            color: var(--text-secondary);
          }

          .upload-sub-text {
            font-size: 12px;
            color: var(--text-tertiary);
          }
        }
      }
    }

    .screenshot-hint {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      margin-top: 12px;
      padding: 8px 12px;
      background: #f4f4f5;
      border-radius: 4px;
      color: #606266;
      font-size: 12px;

      .el-icon {
        color: #909399;
        font-size: 14px;
      }
    }
  }
}

.order-form {
  .total-price {
    font-size: 24px;
    font-weight: 700;
    color: var(--danger-dark);
  }
  .price-display {
    font-size: 16px;
    color: var(--text-secondary);
    font-weight: 500;
  }
  .auto-price-tip {
    font-size: 12px;
    color: var(--text-tertiary);
    margin-top: 5px;
  }
}

.order-no-display {
  font-family: var(--font-mono);
  font-weight: 600;
  color: var(--primary-color);
  font-size: 14px;
}

.edit-order-dialog {
  .edit-order-content {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .edit-section {
    .edit-section-title {
      font-size: 14px;
      font-weight: 600;
      color: var(--text-primary);
      margin-bottom: 8px;
      padding-left: 8px;
      border-left: 3px solid var(--primary-color);
    }
  }

  .edit-duration-group {
    display: flex;
    align-items: center;
    gap: 6px;

    .edit-duration-unit {
      color: var(--text-secondary);
      font-size: 13px;
    }
  }

  .edit-field-hint {
    margin-top: 4px;
    font-size: 12px;
    color: var(--text-tertiary);
  }

  .no-player {
    color: var(--text-tertiary);
    font-style: italic;
  }

  :deep(.el-descriptions__body) {
    .el-descriptions__table {
      .el-descriptions__cell {
        padding: 10px 14px;
      }

      .el-descriptions__label {
        width: 100px;
        font-weight: 600;
        color: #606266;
        background: #f5f7fa;
      }
    }
  }
}

.cancel-reason {
  color: var(--danger-dark);
  font-size: 13px;
}

.detail-price {
  font-size: 18px;
  font-weight: 700;
  color: var(--danger-dark);
}

.detail-actual-price {
  font-size: 18px;
  font-weight: 700;
  color: var(--warning-dark);
}

.detail-cancel-reason {
  color: var(--danger-dark);
  font-weight: 500;
}

.player-sessions {
  display: flex;
  flex-direction: column;
  gap: 15px;
  
  .player-session-item {
    padding: 12px 15px;
    background: #f8f9fa;
    border-radius: var(--border-radius);
    border-left: 3px solid var(--primary-color);
    
    .player-name {
      font-weight: 600;
      font-size: 15px;
      color: var(--text-primary);
      margin-bottom: 8px;
      display: flex;
      align-items: center;
      gap: 5px;
      
      .el-icon {
        color: var(--primary-color);
      }
    }
    
    .session-detail {
      font-size: 13px;
      color: var(--text-secondary);
      margin: 4px 0;
      padding-left: 20px;
      
      .session-label {
        color: var(--text-tertiary);
        margin-right: 5px;
      }
    }
  }
}

.pause-confirm-content {
  text-align: center;
  padding: 20px 0 10px;

  .pause-icon {
    font-size: 48px;
    color: var(--warning-dark);
    margin-bottom: 16px;
  }

  .pause-tip {
    font-size: 16px;
    color: var(--text-primary);
    margin: 0 0 8px;

    strong {
      color: var(--warning-dark);
    }
  }

  .pause-sub-tip {
    font-size: 13px;
    color: var(--text-tertiary);
    margin: 0;
  }
}

.detail-screenshot-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  
  .detail-screenshot-img {
    width: 160px;
    height: 110px;
    border-radius: var(--border-radius);
    object-fit: cover;
    border: 1px solid #e4e7ed;
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:hover {
      border-color: var(--primary-color);
      box-shadow: 0 4px 12px rgba(108, 92, 231, 0.2);
      transform: translateY(-2px);
    }
  }
}

.image-preview-dialog {
  :deep(.el-dialog) {
    max-width: 90vw;
    max-height: 90vh;
    background: transparent;
    box-shadow: none;
  }
  
  :deep(.el-dialog__header) {
    display: none;
  }
  
  :deep(.el-dialog__body) {
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .preview-image {
    max-width: 90vw;
    max-height: 90vh;
    object-fit: contain;
    border-radius: 8px;
  }
}

.assign-filters {
  display: flex;
  gap: 10px;
}

.player-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .player-option-name {
    display: flex;
    align-items: center;
    gap: 6px;

    .player-option-no {
      font-family: var(--font-mono);
      font-weight: 600;
      color: var(--primary-color);
      font-size: 12px;
    }
  }

  .player-option-tags {
    display: flex;
    gap: 4px;
  }
}

.boss-suggestion-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 4px 0;

  .boss-suggestion-name {
    display: flex;
    align-items: center;
    gap: 6px;

    .boss-suggestion-no {
      font-family: var(--font-mono);
      font-weight: 600;
      color: var(--primary-color);
      font-size: 12px;
    }
  }

  .boss-suggestion-info {
    display: flex;
    align-items: center;
    gap: 8px;

    .boss-suggestion-balance {
      font-size: 12px;
      color: var(--text-tertiary);
    }
  }
}

.boss-match-info {
  margin-top: 2px;
  line-height: 1;
}

.player-count-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 12px;
  background: var(--success-bg);
  border-radius: var(--border-radius-sm);
  color: var(--success-color);
  font-size: 13px;

  .el-icon {
    font-size: 14px;
  }
}

@media screen and (max-width: 768px) {
  .orders-page {
    padding: 0;
  }

  .order-card {
    border-radius: var(--border-radius);

    :deep(.el-card__header) {
      padding: 12px 14px;
    }

    :deep(.el-card__body) {
      padding: 12px;
    }
  }

  .card-header {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .header-title {
    .title-icon {
      font-size: 20px;
    }

    .title-text {
      font-size: 17px;
    }
  }

  .header-actions {
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }

  .date-filter-type {
    width: 100%;

    :deep(.el-radio-button__inner) {
      padding: 7px 10px;
      font-size: 12px;
    }
  }

  .date-range-picker {
    width: 100%;

    :deep(.el-date-editor) {
      width: 100% !important;
    }
  }

  .status-filter {
    width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;

    :deep(.el-radio-group) {
      display: flex;
      flex-wrap: nowrap;
      width: max-content;
    }

    :deep(.el-radio-button__inner) {
      padding: 7px 10px;
      font-size: 12px;
      white-space: nowrap;
    }
  }

  .create-btn {
    width: 100%;
    justify-content: center;
  }

  .search-row {
    flex-direction: column;
    gap: 8px;
    margin-bottom: 12px;

    .search-input {
      width: 100%;
    }

    .el-button {
      width: 100%;
    }
  }

  .stats-row {
    gap: 8px;
    margin-bottom: 15px;
  }

  .stat-card {
    min-width: calc(50% - 8px);
    flex: 0 0 calc(50% - 8px);
    padding: 12px;
    gap: 10px;

    .stat-icon {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      font-size: 16px;
    }

    .stat-value {
      font-size: 18px;
    }

    .stat-label {
      font-size: 11px;
    }
  }

  .table-container {
    margin-top: 12px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;

    .el-table {
      min-width: 700px;
    }

    .action-group {
      .el-button {
        padding: 5px 8px;
        font-size: 12px;

        .el-icon {
          margin-right: 2px;
        }
      }
    }
  }

  .order-item-card {
    :deep(.el-card__header) {
      padding: 10px 14px;
    }
  }

  .order-content {
    .info-row {
      margin-bottom: 6px;

      .info-label {
        width: 65px;
        font-size: 12px;
      }

      .info-value {
        font-size: 13px;
      }
    }

    .service-timer {
      margin: 10px 0;
      padding: 10px;

      .timer-value {
        font-size: 22px;
      }
    }

    .action-buttons {
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 12px;

      .el-button {
        flex: 1 1 calc(50% - 4px);
        min-width: 0;
        font-size: 13px;
      }
    }
  }

  .pagination {
    margin-top: 15px;

    :deep(.el-pagination) {
      flex-wrap: wrap;
      justify-content: center;
      gap: 8px;

      .el-pagination__sizes {
        display: none;
      }
    }
  }

  .order-dialog {
    .duration-input-group {
      flex-wrap: wrap;
      gap: 8px;

      .el-input-number {
        width: 100px !important;
      }
    }

    .screenshot-section {
      padding: 12px;

      .simple-screenshot-container {
        .screenshot-preview-list {
          .screenshot-preview-item {
            width: 100px;
            height: 70px;
          }
        }

        .simple-upload-area {
          height: 80px;
        }
      }
    }
  }

  .player-option {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .boss-suggestion-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .assign-filters {
    flex-direction: column;
    gap: 8px;

    .el-select {
      width: 100% !important;
    }
  }

  .detail-screenshot-list {
    justify-content: center;
    
    .detail-screenshot-img {
      width: 140px;
      height: 95px;
    }
  }

  .el-descriptions {
    :deep(.el-descriptions__body) {
      .el-descriptions__table {
        .el-descriptions__cell {
          padding: 8px 10px;
          font-size: 13px;
        }
      }
    }
  }

  .edit-order-dialog {
    .edit-section {
      .edit-section-title {
        font-size: 13px;
      }
    }

    :deep(.el-input-number) {
      width: 100% !important;
    }

    :deep(.el-input__inner) {
      font-size: 13px;
    }

    .edit-duration-group {
      gap: 4px;

      :deep(.el-input-number) {
        width: 75px !important;
      }

      .edit-duration-unit {
        font-size: 12px;
      }
    }

    .edit-field-hint {
      font-size: 11px;
    }
  }

  .scheduled-time-selectors {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;

    .el-select {
      width: 70px !important;
      flex: 1;
      min-width: 60px;
    }
  }
}
</style>
