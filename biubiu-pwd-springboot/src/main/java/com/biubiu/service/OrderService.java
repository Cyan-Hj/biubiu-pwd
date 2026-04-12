package com.biubiu.service;

import com.biubiu.dto.AssignOrderRequest;
import com.biubiu.dto.CompleteOrderRequest;
import com.biubiu.dto.CreateOrderRequest;
import com.biubiu.entity.*;
import com.biubiu.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderService {

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final FinancialRecordRepository financialRecordRepository;
    private final OrderSessionRepository orderSessionRepository;
    private final OperationLogRepository operationLogRepository;
    private final BossRepository bossRepository;
    private final OrderBalanceService orderBalanceService;
    private final BossRechargeRecordRepository rechargeRecordRepository;

    @Transactional
    public Order createOrder(CreateOrderRequest request, User currentUser) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBossInfo(request.getBossInfo());
        order.setServiceContent(request.getServiceContent());
        order.setServiceHours(request.getServiceHours());
        order.setPricePerHour(request.getPricePerHour());
        order.setTotalAmount(request.getTotalAmount());
        order.setScheduledTime(request.getScheduledTime());
        order.setRemark(request.getRemark());
        order.setStatus(Order.Status.PENDING_ASSIGN);
        order.setCreatedBy(currentUser);

        // 设置订单类型和人数
        if ("double".equalsIgnoreCase(request.getPlayerCount())) {
            order.setPlayerCount(Order.PlayerCount.DOUBLE);
        } else {
            order.setPlayerCount(Order.PlayerCount.SINGLE);
        }

        order.setOrderType(request.getOrderType());

        // 设置客户类型和老板信息
        if (request.getCustomerType() != null) {
            order.setCustomerType(Order.CustomerType.valueOf(request.getCustomerType()));
        } else {
            order.setCustomerType(Order.CustomerType.SCATTER);
        }

        if (request.getBossId() != null && order.getCustomerType() == Order.CustomerType.REGULAR) {
            Boss boss = bossRepository.findById(request.getBossId())
                    .orElseThrow(() -> new RuntimeException("老板不存在"));
            order.setBoss(boss);
            order.setOriginalAmount(request.getOriginalAmount());
            order.setDiscountRate(request.getDiscountRate());
            order.setUseBalance(request.getUseBalance());

            // 如果使用余额支付，扣减余额
            if (Boolean.TRUE.equals(request.getUseBalance())) {
                orderBalanceService.deductBalance(order, boss, request.getTotalAmount());
            }
        }

        Order saved = orderRepository.save(order);
        
        logOperation(saved, currentUser, "CREATE", null, Order.Status.PENDING_ASSIGN, "创建订单");
        return saved;
    }

    @Transactional
    public void assignOrder(Long id, AssignOrderRequest request, User currentUser) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        Order.Status oldStatus = order.getStatus();

        // 允许待分配或待接单状态（支持改派）
        if (order.getStatus() != Order.Status.PENDING_ASSIGN &&
            order.getStatus() != Order.Status.PENDING_ACCEPT &&
            order.getStatus() != Order.Status.PENDING_ACCEPT_2 &&
            order.getStatus() != Order.Status.IN_SERVICE) {
            throw new RuntimeException("订单状态不正确，无法派单或改派");
        }

        // 双人订单
        if (order.getPlayerCount() == Order.PlayerCount.DOUBLE) {
            assignDoubleOrder(order, request, currentUser, oldStatus);
        } else {
            // 单人订单
            assignSingleOrder(order, request, currentUser, oldStatus);
        }
    }

    private void assignSingleOrder(Order order, AssignOrderRequest request, User currentUser, Order.Status oldStatus) {
        User player = userRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new RuntimeException("陪玩师不存在"));

        if (player.getStatus() != User.Status.active) {
            throw new RuntimeException("陪玩师状态不正常");
        }

        // 改派时清理所有旧的会话记录
        clearAllSessions(order);

        order.setStatus(Order.Status.PENDING_ACCEPT);
        order.setCurrentPlayer(player);
        order.setAssignedBy(currentUser);
        order.setAssignedAt(LocalDateTime.now());

        orderRepository.save(order);
        
        String type = oldStatus == Order.Status.PENDING_ASSIGN ? "ASSIGN" : "REASSIGN";
        logOperation(order, currentUser, type, oldStatus, Order.Status.PENDING_ACCEPT, "派单给: " + player.getNickname());
    }

    private void assignDoubleOrder(Order order, AssignOrderRequest request, User currentUser, Order.Status oldStatus) {
        // 双人订单必须选择两个陪玩师
        if (request.getPlayerId() == null || request.getPlayerId2() == null) {
            throw new RuntimeException("双人订单必须选择两个陪玩师");
        }

        User player1 = userRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new RuntimeException("陪玩师1不存在"));
        User player2 = userRepository.findById(request.getPlayerId2())
                .orElseThrow(() -> new RuntimeException("陪玩师2不存在"));

        if (player1.getStatus() != User.Status.active || player2.getStatus() != User.Status.active) {
            throw new RuntimeException("陪玩师状态不正常");
        }

        if (player1.getId().equals(player2.getId())) {
            throw new RuntimeException("两个陪玩师不能是同一个人");
        }

        // 改派时清理所有旧的会话记录，所有人需要重新接单
        clearAllSessions(order);

        order.setStatus(Order.Status.PENDING_ACCEPT);
        order.setCurrentPlayer(player1);
        order.setCurrentPlayer2(player2);
        order.setAssignedBy(currentUser);
        order.setAssignedAt(LocalDateTime.now());

        orderRepository.save(order);
        
        String type = oldStatus == Order.Status.PENDING_ASSIGN ? "ASSIGN" : "REASSIGN";
        logOperation(order, currentUser, type, oldStatus, Order.Status.PENDING_ACCEPT, 
                "派单给: " + player1.getNickname() + " 和 " + player2.getNickname());
    }

    private void endCurrentSession(Order order) {
        User oldPlayer = order.getCurrentPlayer();
        if (oldPlayer != null) {
            OrderSession session = orderSessionRepository.findFirstByOrderIdAndPlayerIdAndEndedAtIsNullOrderByIdDesc(order.getId(), oldPlayer.getId())
                    .orElse(null);
            if (session != null) {
                session.setEndedAt(LocalDateTime.now());
                long minutes = java.time.Duration.between(session.getStartedAt(), session.getEndedAt()).toMinutes();
                session.setActualHours(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
                orderSessionRepository.save(session);
            }
        }
        // 结束第二个陪玩师的会话（如果有）
        User oldPlayer2 = order.getCurrentPlayer2();
        if (oldPlayer2 != null) {
            OrderSession session2 = orderSessionRepository.findFirstByOrderIdAndPlayerIdAndEndedAtIsNullOrderByIdDesc(order.getId(), oldPlayer2.getId())
                    .orElse(null);
            if (session2 != null) {
                session2.setEndedAt(LocalDateTime.now());
                long minutes = java.time.Duration.between(session2.getStartedAt(), session2.getEndedAt()).toMinutes();
                session2.setActualHours(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
                orderSessionRepository.save(session2);
            }
        }
    }

    private void clearAllSessions(Order order) {
        // 获取该订单的所有会话记录
        List<OrderSession> allSessions = orderSessionRepository.findByOrderId(order.getId());
        
        // 删除所有会话记录，让所有人重新接单
        for (OrderSession session : allSessions) {
            orderSessionRepository.delete(session);
        }
    }

    private void endSessionForPlayer(Long orderId, Long playerId) {
        // 结束指定陪玩师的未结束会话
        OrderSession session = orderSessionRepository.findFirstByOrderIdAndPlayerIdAndEndedAtIsNullOrderByIdDesc(orderId, playerId)
                .orElse(null);
        if (session != null) {
            session.setEndedAt(LocalDateTime.now());
            long minutes = java.time.Duration.between(session.getStartedAt(), session.getEndedAt()).toMinutes();
            session.setActualHours(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
            orderSessionRepository.save(session);
        }
    }

    @Transactional
    public void cancelOrder(Long id, String reason, User currentUser) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        Order.Status oldStatus = order.getStatus();

        // 如果订单已经是取消或完成状态，不允许再次取消
        if (order.getStatus() == Order.Status.CANCELLED) {
            throw new RuntimeException("订单已经是取消状态");
        }
        if (order.getStatus() == Order.Status.COMPLETED) {
            throw new RuntimeException("订单已完成，无法取消");
        }

        // 如果订单是服务中状态或待接单2状态（双人订单第一个已接单），需要先结算当前服务会话
        if (order.getStatus() == Order.Status.IN_SERVICE || order.getStatus() == Order.Status.PENDING_ACCEPT_2) {
            // 结束第一个陪玩师的会话
            User currentPlayer = order.getCurrentPlayer();
            if (currentPlayer != null) {
                OrderSession session = orderSessionRepository.findByOrderIdAndPlayerIdAndEndedAtIsNull(order.getId(), currentPlayer.getId())
                        .orElse(null);
                if (session != null) {
                    session.setEndedAt(LocalDateTime.now());
                    long minutes = java.time.Duration.between(session.getStartedAt(), session.getEndedAt()).toMinutes();
                    session.setActualHours(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
                    orderSessionRepository.save(session);
                }
            }
            // 结束第二个陪玩师的会话（如果有）
            User currentPlayer2 = order.getCurrentPlayer2();
            if (currentPlayer2 != null) {
                OrderSession session2 = orderSessionRepository.findByOrderIdAndPlayerIdAndEndedAtIsNull(order.getId(), currentPlayer2.getId())
                        .orElse(null);
                if (session2 != null) {
                    session2.setEndedAt(LocalDateTime.now());
                    long minutes = java.time.Duration.between(session2.getStartedAt(), session2.getEndedAt()).toMinutes();
                    session2.setActualHours(BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
                    orderSessionRepository.save(session2);
                }
            }
        }

        order.setStatus(Order.Status.CANCELLED);
        order.setCancelReason(reason);
        order.setCancelledAt(LocalDateTime.now());

        // 如果订单使用了余额支付，退还扣除的余额
        if (order.getBoss() != null && order.getBalanceDeducted() != null && order.getBalanceDeducted().compareTo(BigDecimal.ZERO) > 0) {
            Boss boss = order.getBoss();
            BigDecimal refundAmount = order.getBalanceDeducted();
            boss.setBalance(boss.getBalance().add(refundAmount));
            bossRepository.save(boss);

            com.biubiu.entity.BossRechargeRecord refundRecord = new com.biubiu.entity.BossRechargeRecord();
            refundRecord.setBossId(boss.getId());
            refundRecord.setAmount(refundAmount);
            refundRecord.setType(com.biubiu.entity.BossRechargeRecord.Type.REFUND);
            refundRecord.setOrderNo(order.getOrderNo());
            refundRecord.setRemark("取消订单，退还余额¥" + refundAmount);
            refundRecord.setOperatorId(currentUser.getId());
            rechargeRecordRepository.save(refundRecord);

            order.setBalanceDeducted(BigDecimal.ZERO);
        }

        orderRepository.save(order);
        logOperation(order, currentUser, "CANCEL", oldStatus, Order.Status.CANCELLED, "取消订单，原因: " + reason);
    }

    @Transactional
    public void acceptOrder(Long id, User currentUser) {
        System.err.println("[DEBUG] acceptOrder called, orderId=" + id + ", userId=" + currentUser.getId() + ", userPhone=" + currentUser.getPhone());
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        Order.Status oldStatus = order.getStatus();
        System.err.println("[DEBUG] order found, playerCount=" + order.getPlayerCount() + ", status=" + oldStatus);
        System.err.println("[DEBUG] currentPlayer=" + (order.getCurrentPlayer() != null ? order.getCurrentPlayer().getId() : "null") + 
                          ", currentPlayer2=" + (order.getCurrentPlayer2() != null ? order.getCurrentPlayer2().getId() : "null"));

        // 单人订单
        if (order.getPlayerCount() == Order.PlayerCount.SINGLE) {
            System.err.println("[DEBUG] 进入单人订单处理分支");
            acceptSingleOrder(order, currentUser, oldStatus);
        } else {
            // 双人订单
            System.err.println("[DEBUG] 进入双人订单处理分支");
            acceptDoubleOrder(order, currentUser, oldStatus);
        }
    }

    private void acceptSingleOrder(Order order, User currentUser, Order.Status oldStatus) {
        System.err.println("[DEBUG] acceptSingleOrder called, orderId=" + order.getId() + ", userId=" + currentUser.getId());
        
        if (order.getStatus() != Order.Status.PENDING_ACCEPT) {
            System.err.println("[DEBUG] 单人订单状态不正确: " + order.getStatus());
            throw new RuntimeException("订单状态不正确，当前状态: " + order.getStatus());
        }

        if (!order.getCurrentPlayer().getId().equals(currentUser.getId())) {
            System.err.println("[DEBUG] 单人订单无权操作");
            throw new RuntimeException("无权操作此订单");
        }

        // 检查该陪玩师是否已经有服务中的订单
        System.err.println("[DEBUG] 单人订单查询服务中订单数...");
        long inServiceCount = orderRepository.countByCurrentPlayerIdAndStatus(currentUser.getId(), Order.Status.IN_SERVICE);
        System.err.println("[DEBUG] 单人订单服务中订单数: " + inServiceCount);
        if (inServiceCount > 0) {
            System.err.println("[DEBUG] 单人订单已有服务中订单");
            throw new RuntimeException("您当前有 " + inServiceCount + " 个服务中的订单，请先完成后再接新订单");
        }

        order.setStatus(Order.Status.IN_SERVICE);
        order.setStartedAt(LocalDateTime.now());

        orderRepository.save(order);

        // 创建新的服务会话
        OrderSession session = new OrderSession();
        session.setOrder(order);
        session.setPlayer(currentUser);
        session.setStartedAt(LocalDateTime.now());
        orderSessionRepository.save(session);

        logOperation(order, currentUser, "ACCEPT", oldStatus, Order.Status.IN_SERVICE, "接单");
    }

    private void acceptDoubleOrder(Order order, User currentUser, Order.Status oldStatus) {
        System.err.println("[DEBUG] acceptDoubleOrder called, orderId=" + order.getId() + ", userId=" + currentUser.getId() + ", orderStatus=" + order.getStatus());
        
        // 检查订单状态是否正确
        if (order.getStatus() != Order.Status.PENDING_ACCEPT && order.getStatus() != Order.Status.PENDING_ACCEPT_2) {
            System.err.println("[DEBUG] 订单状态不正确: " + order.getStatus());
            throw new RuntimeException("订单状态不正确，当前状态: " + order.getStatus());
        }

        // 检查是否是两个陪玩师之一
        boolean isPlayer1 = order.getCurrentPlayer() != null && order.getCurrentPlayer().getId().equals(currentUser.getId());
        boolean isPlayer2 = order.getCurrentPlayer2() != null && order.getCurrentPlayer2().getId().equals(currentUser.getId());
        
        System.err.println("[DEBUG] isPlayer1=" + isPlayer1 + ", isPlayer2=" + isPlayer2);
        System.err.println("[DEBUG] currentPlayer=" + (order.getCurrentPlayer() != null ? order.getCurrentPlayer().getId() : "null") + 
                          ", currentPlayer2=" + (order.getCurrentPlayer2() != null ? order.getCurrentPlayer2().getId() : "null"));

        if (!isPlayer1 && !isPlayer2) {
            String player1Name = order.getCurrentPlayer() != null ? order.getCurrentPlayer().getNickname() : "null";
            String player2Name = order.getCurrentPlayer2() != null ? order.getCurrentPlayer2().getNickname() : "null";
            System.err.println("[DEBUG] 无权操作此订单");
            throw new RuntimeException("无权操作此订单，您不是该订单的陪玩师。订单陪玩师1: " + player1Name + ", 陪玩师2: " + player2Name);
        }

        // 检查该陪玩师是否已经有服务中的订单
        System.err.println("[DEBUG] 查询服务中订单数...");
        long inServiceCount = orderRepository.countByCurrentPlayerIdAndStatus(currentUser.getId(), Order.Status.IN_SERVICE);
        System.err.println("[DEBUG] 服务中订单数: " + inServiceCount);
        if (inServiceCount > 0) {
            System.err.println("[DEBUG] 已有服务中订单");
            throw new RuntimeException("您当前有 " + inServiceCount + " 个服务中的订单，请先完成后再接新订单");
        }

        // 检查是否已经接过单了
        System.err.println("[DEBUG] 检查是否已经接过单...");
        List<OrderSession> existingSessions = orderSessionRepository.findByOrderIdAndPlayerId(order.getId(), currentUser.getId());
        System.err.println("[DEBUG] 找到 " + existingSessions.size() + " 个会话");
        OrderSession existingSession = existingSessions.stream().filter(s -> s.getEndedAt() == null).findFirst().orElse(null);
        if (existingSession != null) {
            System.err.println("[DEBUG] 已经接过此订单，会话ID=" + existingSession.getId());
            throw new RuntimeException("您已经接过此订单");
        }
        System.err.println("[DEBUG] 未接过此订单");

        // 创建新的服务会话
        System.err.println("[DEBUG] 创建新的服务会话...");
        OrderSession session = new OrderSession();
        session.setOrder(order);
        session.setPlayer(currentUser);
        session.setStartedAt(LocalDateTime.now());
        orderSessionRepository.save(session);
        System.err.println("[DEBUG] 会话创建成功，ID=" + session.getId());

        // 检查是否两个陪玩师都已接单
        System.err.println("[DEBUG] 检查是否两个陪玩师都已接单...");
        List<OrderSession> sessions = orderSessionRepository.findByOrderId(order.getId());
        long activeSessions = sessions.stream().filter(s -> s.getEndedAt() == null).count();
        System.err.println("[DEBUG] 活跃会话数: " + activeSessions);

        if (activeSessions >= 2) {
            // 两人都接单了，进入服务中状态
            System.err.println("[DEBUG] 两人都接单了，进入服务中状态");
            order.setStatus(Order.Status.IN_SERVICE);
            order.setStartedAt(LocalDateTime.now());
            orderRepository.save(order);
            logOperation(order, currentUser, "ACCEPT", oldStatus, Order.Status.IN_SERVICE, "接单（双人订单，两人都已接单）");
        } else {
            // 只有一人接单，等待另一个人
            System.err.println("[DEBUG] 只有一人接单，等待另一个人");
            if (order.getStatus() == Order.Status.PENDING_ACCEPT) {
                order.setStatus(Order.Status.PENDING_ACCEPT_2);
                orderRepository.save(order);
            }
            logOperation(order, currentUser, "ACCEPT", oldStatus, order.getStatus(), "接单（双人订单，等待另一人）");
        }
        System.err.println("[DEBUG] acceptDoubleOrder 完成");
    }

    @Transactional
    public void completeOrder(Long id, CompleteOrderRequest request, User currentUser) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        
        Order.Status oldStatus = order.getStatus();

        // 检查订单状态是否为服务中
        if (order.getStatus() != Order.Status.IN_SERVICE) {
            throw new RuntimeException("订单状态不正确");
        }

        // 检查是否是两个陪玩师之一
        boolean isPlayer1 = order.getCurrentPlayer() != null && order.getCurrentPlayer().getId().equals(currentUser.getId());
        boolean isPlayer2 = order.getCurrentPlayer2() != null && order.getCurrentPlayer2().getId().equals(currentUser.getId());
        if (!isPlayer1 && !isPlayer2) {
            throw new RuntimeException("无权操作此订单");
        }

        // 检查当前用户是否已经完成过此订单
        OrderSession currentSession = orderSessionRepository.findFirstByOrderIdAndPlayerIdAndEndedAtIsNullOrderByIdDesc(order.getId(), currentUser.getId())
                .orElse(null);

        if (currentSession == null) {
            throw new RuntimeException("您没有进行中的服务会话，或已经完成此订单");
        }

        // 结束当前会话
        currentSession.setEndedAt(LocalDateTime.now());
        // 计算当前会话时长
        long currentMinutes = java.time.Duration.between(currentSession.getStartedAt(), currentSession.getEndedAt()).toMinutes();
        BigDecimal currentHours = BigDecimal.valueOf(currentMinutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        
        // 如果计算出的时长小于0.05小时（3分钟），且请求中有手动输入的时长，则使用手动输入的时长
        if (currentHours.compareTo(new BigDecimal("0.05")) < 0 && request.getActualHours() != null && request.getActualHours().compareTo(BigDecimal.ZERO) > 0) {
            currentHours = request.getActualHours();
        }
        
        currentSession.setActualHours(currentHours);
        orderSessionRepository.save(currentSession);

        // 检查是否还有其他陪玩师没有完成
        List<OrderSession> allSessions = orderSessionRepository.findByOrderId(order.getId());
        long activeSessions = allSessions.stream().filter(s -> s.getEndedAt() == null).count();

        if (activeSessions == 0) {
            // 所有人都完成了，订单完成
            order.setStatus(Order.Status.COMPLETED);
            order.setCompletedAt(LocalDateTime.now());

            // 重新计算总实际时长（所有会话时长之和）
            BigDecimal totalActualHours = allSessions.stream()
                    .map(s -> s.getActualHours() != null ? s.getActualHours() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 如果没有会话记录（兼容旧数据），使用请求中的时长
            if (totalActualHours.compareTo(BigDecimal.ZERO) == 0 && request.getActualHours() != null) {
                totalActualHours = request.getActualHours();
            }
            order.setActualHours(totalActualHours);

            // 保存截图URL
            if (request.getScreenshotUrls() != null && !request.getScreenshotUrls().isEmpty()) {
                order.setScreenshotUrls(String.join(",", request.getScreenshotUrls()));
                order.setStartScreenshotUrl(request.getScreenshotUrls().get(0));
                if (request.getScreenshotUrls().size() > 1) {
                    order.setEndScreenshotUrl(request.getScreenshotUrls().get(1));
                }
            } else {
                // 兼容旧版本
                if (request.getStartScreenshotUrl() != null) {
                    order.setStartScreenshotUrl(request.getStartScreenshotUrl());
                }
                if (request.getEndScreenshotUrl() != null) {
                    order.setEndScreenshotUrl(request.getEndScreenshotUrl());
                }
                if (order.getStartScreenshotUrl() != null) {
                    String urls = order.getEndScreenshotUrl() != null
                        ? order.getStartScreenshotUrl() + "," + order.getEndScreenshotUrl()
                        : order.getStartScreenshotUrl();
                    order.setScreenshotUrls(urls);
                }
            }

            // 计算订单实际金额
            BigDecimal actualTotalAmount;
            if ("huhang".equals(order.getOrderType())) {
                actualTotalAmount = order.getTotalAmount();
            } else {
                BigDecimal pricePerHour = order.getPricePerHour();
                BigDecimal createdHours = order.getServiceHours();
                
                if (totalActualHours.compareTo(createdHours) > 0) {
                    BigDecimal extraMinutes = totalActualHours.subtract(createdHours).multiply(BigDecimal.valueOf(60));
                    int totalExtraMinutes = extraMinutes.intValue();
                    int fullHours = totalExtraMinutes / 60;
                    int remainingMinutes = totalExtraMinutes % 60;
                    
                    BigDecimal extraFee = BigDecimal.valueOf(fullHours).multiply(pricePerHour);
                    if (remainingMinutes > 15 && remainingMinutes <= 45) {
                        extraFee = extraFee.add(pricePerHour.multiply(BigDecimal.valueOf(0.5)));
                    } else if (remainingMinutes > 45) {
                        extraFee = extraFee.add(pricePerHour);
                    }
                    
                    actualTotalAmount = createdHours.multiply(pricePerHour).add(extraFee);
                } else {
                    actualTotalAmount = createdHours.multiply(pricePerHour);
                }
            }

            // 获取平台抽成比例
            BigDecimal platformFeeRate = systemConfigRepository.findFirstByOrderByIdAsc()
                    .map(SystemConfig::getPlatformFeeRate)
                    .orElse(BigDecimal.valueOf(0.2));

            // 计算所有陪玩师的总可分配收入 = 实际金额 * (1 - 抽成比例)
            BigDecimal totalPlayerIncome = actualTotalAmount.multiply(BigDecimal.ONE.subtract(platformFeeRate));

            // 双人订单：对半分
            if (order.getPlayerCount() == Order.PlayerCount.DOUBLE) {
                BigDecimal incomePerPerson = totalPlayerIncome.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
                
                for (OrderSession session : allSessions) {
                    if (session.getActualHours() == null || session.getActualHours().compareTo(BigDecimal.ZERO) <= 0) continue;

                    User player = session.getPlayer();
                    player.setTotalIncome(player.getTotalIncome().add(incomePerPerson));
                    player.setAvailableBalance(player.getAvailableBalance().add(incomePerPerson));
                    userRepository.save(player);

                    // 创建财务记录
                    FinancialRecord record = new FinancialRecord();
                    record.setOrder(order);
                    record.setPlayer(player);
                    record.setType(FinancialRecord.Type.income);
                    record.setAmount(incomePerPerson);
                    record.setDescription("订单收入 (单号: " + order.getOrderNo() + ", 双人订单对半分)");
                    financialRecordRepository.save(record);
                }
            } else {
                // 单人订单：按服务时长比例分配
                for (OrderSession session : allSessions) {
                    if (session.getActualHours() == null || session.getActualHours().compareTo(BigDecimal.ZERO) <= 0) continue;

                    // 该陪玩师服务时长占比
                    BigDecimal ratio = totalActualHours.compareTo(BigDecimal.ZERO) > 0 ?
                            session.getActualHours().divide(totalActualHours, 4, java.math.RoundingMode.HALF_UP) : BigDecimal.ONE;

                    // 该陪玩师实际所得
                    BigDecimal income = totalPlayerIncome.multiply(ratio).setScale(2, java.math.RoundingMode.HALF_UP);

                    User player = session.getPlayer();
                    player.setTotalIncome(player.getTotalIncome().add(income));
                    player.setAvailableBalance(player.getAvailableBalance().add(income));
                    userRepository.save(player);

                    // 创建财务记录
                    FinancialRecord record = new FinancialRecord();
                    record.setOrder(order);
                    record.setPlayer(player);
                    record.setType(FinancialRecord.Type.income);
                    record.setAmount(income);
                    record.setDescription("订单收入 (单号: " + order.getOrderNo() + ", 占比: " + ratio.multiply(BigDecimal.valueOf(100)).setScale(2) + "%)");
                    financialRecordRepository.save(record);
                }
            }

            // 更新老板的累计消费（按实际金额计算）
            if (order.getBoss() != null) {
                Boss boss = order.getBoss();
                boss.setTotalConsumption(boss.getTotalConsumption().add(actualTotalAmount));
                bossRepository.save(boss);
            }

            orderRepository.save(order);
            logOperation(order, currentUser, "COMPLETE", oldStatus, Order.Status.COMPLETED, "完成订单（所有陪玩师已完成）");
        } else {
            // 还有其他人没有完成，只记录当前陪玩师完成
            orderRepository.save(order);
            logOperation(order, currentUser, "COMPLETE", oldStatus, Order.Status.IN_SERVICE, "完成服务，等待其他陪玩师完成");
        }
    }

    @Transactional
    public void batchDeleteOrders(List<Long> ids) {
        for (Long id : ids) {
            Order order = orderRepository.findById(id).orElse(null);
            if (order != null) {
                List<OperationLog> logs = operationLogRepository.findByOrderIdOrderByCreatedAtDesc(id);
                operationLogRepository.deleteAll(logs);
                
                List<OrderSession> sessions = orderSessionRepository.findByOrderId(id);
                orderSessionRepository.deleteAll(sessions);
                
                List<FinancialRecord> records = financialRecordRepository.findByOrderId(id);
                financialRecordRepository.deleteAll(records);
                
                deleteScreenshotFile(order.getStartScreenshotUrl());
                deleteScreenshotFile(order.getEndScreenshotUrl());
                if (order.getScreenshotUrls() != null && !order.getScreenshotUrls().isEmpty()) {
                    for (String url : order.getScreenshotUrls().split(",")) {
                        deleteScreenshotFile(url);
                    }
                }
                
                orderRepository.delete(order);
            }
        }
    }

    private void deleteScreenshotFile(String screenshotUrl) {
        if (screenshotUrl != null && !screenshotUrl.isEmpty()) {
            try {
                String filename = screenshotUrl.replace("/uploads/", "");
                Path filePath = Paths.get(uploadPath).resolve(filename);
                Files.deleteIfExists(filePath);
            } catch (Exception ignored) {
            }
        }
    }

    private void logOperation(Order order, User operator, String type, Order.Status oldStatus, Order.Status newStatus, String details) {
        OperationLog log = new OperationLog();
        log.setOrder(order);
        log.setOperator(operator);
        log.setOperationType(type);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setDetails(details);
        operationLogRepository.save(log);
    }

    @Transactional
    public void pauseOrder(Long id, String reason, User currentUser) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        Order.Status oldStatus = order.getStatus();

        boolean isAdminOrCS = currentUser.getRole() == User.Role.ADMIN || currentUser.getRole() == User.Role.CUSTOMER_SERVICE;
        boolean isPlayer = currentUser.getRole() == User.Role.PLAYER;

        if (isAdminOrCS) {
            if (order.getStatus() != Order.Status.PENDING_ASSIGN && order.getStatus() != Order.Status.IN_SERVICE) {
                throw new RuntimeException("管理员/客服只能在待分配或服务中状态下暂存订单");
            }
        } else if (isPlayer) {
            if (order.getStatus() != Order.Status.PENDING_ACCEPT
                    && order.getStatus() != Order.Status.PENDING_ACCEPT_2
                    && order.getStatus() != Order.Status.IN_SERVICE) {
                throw new RuntimeException("陪玩师只能在已接单或服务中状态下暂存订单");
            }
            boolean isPlayer1 = order.getCurrentPlayer() != null && order.getCurrentPlayer().getId().equals(currentUser.getId());
            boolean isPlayer2 = order.getCurrentPlayer2() != null && order.getCurrentPlayer2().getId().equals(currentUser.getId());
            if (!isPlayer1 && !isPlayer2) {
                throw new RuntimeException("无权操作此订单");
            }
            if (order.getStatus() == Order.Status.PENDING_ACCEPT || order.getStatus() == Order.Status.PENDING_ACCEPT_2) {
                List<OrderSession> sessions = orderSessionRepository.findByOrderIdAndPlayerId(order.getId(), currentUser.getId());
                boolean hasActiveSession = sessions.stream().anyMatch(s -> s.getEndedAt() == null);
                if (!hasActiveSession) {
                    throw new RuntimeException("您尚未接单，无法暂存");
                }
            }
        } else {
            throw new RuntimeException("无权操作此订单");
        }

        if (order.getStatus() == Order.Status.IN_SERVICE) {
            endCurrentSession(order);
        }

        order.setStatusBeforePause(oldStatus);
        order.setStatus(Order.Status.PAUSED);
        order.setPauseReason(reason);
        order.setPausedAt(LocalDateTime.now());

        orderRepository.save(order);
        logOperation(order, currentUser, "PAUSE", oldStatus, Order.Status.PAUSED, "暂存订单，原因: " + reason);
    }

    @Transactional
    public void resumeOrder(Long id, User currentUser) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (order.getStatus() != Order.Status.PAUSED) {
            throw new RuntimeException("订单不是暂存状态，无法恢复");
        }

        Order.Status oldStatus = order.getStatus();
        Order.Status targetStatus = order.getStatusBeforePause();
        if (targetStatus == null) {
            targetStatus = Order.Status.PENDING_ASSIGN;
        }

        order.setStatus(targetStatus);
        order.setResumedAt(LocalDateTime.now());

        if (targetStatus == Order.Status.IN_SERVICE) {
            if (order.getCurrentPlayer() != null) {
                OrderSession session = new OrderSession();
                session.setOrder(order);
                session.setPlayer(order.getCurrentPlayer());
                session.setStartedAt(LocalDateTime.now());
                orderSessionRepository.save(session);
            }
            if (order.getCurrentPlayer2() != null) {
                OrderSession session2 = new OrderSession();
                session2.setOrder(order);
                session2.setPlayer(order.getCurrentPlayer2());
                session2.setStartedAt(LocalDateTime.now());
                orderSessionRepository.save(session2);
            }
        }

        orderRepository.save(order);
        logOperation(order, currentUser, "RESUME", oldStatus, targetStatus, "恢复订单到: " + targetStatus.name());
    }

    @Transactional
    public Order updateOrder(Long id, com.biubiu.dto.UpdateOrderRequest request, User currentUser) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (order.getStatus() == Order.Status.CANCELLED) {
            throw new RuntimeException("已取消的订单无法修改");
        }

        BigDecimal oldTotalAmount = order.getTotalAmount();
        BigDecimal oldBalanceDeducted = order.getBalanceDeducted();
        Boss oldBoss = order.getBoss();

        order.setBossInfo(request.getBossInfo());
        order.setServiceContent(request.getServiceContent());
        order.setServiceHours(request.getServiceHours());
        order.setPricePerHour(request.getPricePerHour());
        order.setTotalAmount(request.getTotalAmount());
        order.setScheduledTime(request.getScheduledTime());
        order.setRemark(request.getRemark());
        order.setOriginalAmount(request.getOriginalAmount());
        order.setDiscountRate(request.getDiscountRate());

        if (request.getBossId() != null) {
            Boss newBoss = bossRepository.findById(request.getBossId())
                    .orElseThrow(() -> new RuntimeException("老板不存在"));
            order.setBoss(newBoss);
            order.setCustomerType(Order.CustomerType.REGULAR);
        } else {
            order.setBoss(null);
            order.setCustomerType(Order.CustomerType.SCATTER);
        }

        boolean useBalance = Boolean.TRUE.equals(request.getUseBalance());
        order.setUseBalance(useBalance);

        if (oldBoss != null && oldBalanceDeducted != null && oldBalanceDeducted.compareTo(BigDecimal.ZERO) > 0) {
            oldBoss.setBalance(oldBoss.getBalance().add(oldBalanceDeducted));
            bossRepository.save(oldBoss);

            com.biubiu.entity.BossRechargeRecord refundRecord = new com.biubiu.entity.BossRechargeRecord();
            refundRecord.setBossId(oldBoss.getId());
            refundRecord.setAmount(oldBalanceDeducted);
            refundRecord.setType(com.biubiu.entity.BossRechargeRecord.Type.REFUND);
            refundRecord.setOrderNo(order.getOrderNo());
            refundRecord.setRemark("修改订单，退还原扣款¥" + oldBalanceDeducted);
            refundRecord.setOperatorId(currentUser.getId());
            rechargeRecordRepository.save(refundRecord);
        }

        order.setBalanceDeducted(null);

        if (useBalance && order.getBoss() != null) {
            orderBalanceService.deductBalance(order, order.getBoss(), request.getTotalAmount());
        }

        Order saved = orderRepository.save(order);

        logOperation(saved, currentUser, "UPDATE", order.getStatus(), order.getStatus(), "修改订单");
        return saved;
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%06d", new Random().nextInt(1000000));
        return "DD" + date + random;
    }
}
