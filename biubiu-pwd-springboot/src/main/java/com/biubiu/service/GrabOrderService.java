package com.biubiu.service;

import com.biubiu.dto.*;
import com.biubiu.entity.*;
import com.biubiu.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GrabOrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final GrabWaitingPlayerRepository waitingPlayerRepository;
    private final GrabCooldownRepository cooldownRepository;
    private final GrabPriorityWaitRepository priorityWaitRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final LevelPriceRepository levelPriceRepository;
    private final OrderSessionRepository orderSessionRepository;
    private final OperationLogRepository operationLogRepository;

    @Transactional
    public GrabResult grabOrder(Long orderId, GrabRequest request, User currentUser) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        validateGrabEligibility(order, currentUser);

        return switch (request.getGrabType()) {
            case "SINGLE" -> handleSingleGrab(order, currentUser);
            case "DOUBLE_SOLO" -> handleDoubleSoloGrab(order, currentUser, request);
            case "DOUBLE_TEAM" -> handleDoubleTeamGrab(order, currentUser, request);
            default -> throw new RuntimeException("无效的抢单类型");
        };
    }

    private void validateGrabEligibility(Order order, User player) {
        if (!Boolean.TRUE.equals(order.getInGrabHall())) {
            throw new RuntimeException("订单不在抢单大厅");
        }
        if (order.getGrabStatus() == Order.GrabStatus.ASSIGNED) {
            throw new RuntimeException("该订单已被抢走");
        }
        if (player.getStatus() != User.Status.active) {
            throw new RuntimeException("您的账号状态异常，无法抢单");
        }
        List<Order.Status> activeStatuses = Arrays.asList(
                Order.Status.PENDING_ACCEPT, Order.Status.PENDING_ACCEPT_2, Order.Status.IN_SERVICE);
        long activeCount = orderRepository.countActiveOrdersByPlayerId(player.getId(), activeStatuses);
        if (activeCount > 0) {
            throw new RuntimeException("您有正在进行的订单，请先完成后再抢单");
        }
        if (!canGrabByLevel(order, player)) {
            throw new RuntimeException("您的等级不满足此订单要求");
        }
        boolean inCooldown = cooldownRepository.existsByOrderIdAndPlayerIdAndCooldownUntilAfter(
                order.getId(), player.getId(), LocalDateTime.now());
        if (inCooldown) {
            throw new RuntimeException("您在此订单的冷却期内，请稍后再试");
        }
    }

    private boolean canGrabByLevel(Order order, User player) {
        if ("huhang".equals(order.getOrderType())) {
            return true;
        }
        LevelPrice orderLevel = getMaxLevelByPrice(order.getPricePerHour());
        if (orderLevel == null) return true;
        LevelPrice playerLevel = levelPriceRepository.findByLevel(player.getLevel()).orElse(null);
        if (playerLevel == null) return false;
        return playerLevel.getSortOrder() >= orderLevel.getSortOrder();
    }

    private LevelPrice getMaxLevelByPrice(BigDecimal pricePerHour) {
        List<LevelPrice> allLevels = levelPriceRepository.findAllByOrderBySortOrderAsc();
        LevelPrice result = null;
        for (LevelPrice lp : allLevels) {
            if (lp.getDefaultPrice().compareTo(pricePerHour) <= 0) {
                result = lp;
            }
        }
        return result;
    }

    private boolean isHighPriorityPlayer(Order order, User player) {
        if (!"huhang".equals(order.getOrderType())) return true;
        if (order.getPriorityLevel() == null) return true;
        LevelPrice priorityLevel = levelPriceRepository.findByLevel(order.getPriorityLevel()).orElse(null);
        if (priorityLevel == null) return true;
        LevelPrice playerLevel = levelPriceRepository.findByLevel(player.getLevel()).orElse(null);
        if (playerLevel == null) return false;
        return playerLevel.getSortOrder() >= priorityLevel.getSortOrder();
    }

    private GrabResult handleSingleGrab(Order order, User player) {
        if (order.getGrabStatus() != Order.GrabStatus.OPEN) {
            throw new RuntimeException("该订单已被抢走");
        }
        if (order.getPlayerCount() != Order.PlayerCount.SINGLE) {
            throw new RuntimeException("该订单不是单人单");
        }

        if (order.getPriorityLevel() != null) {
            if (isHighPriorityPlayer(order, player)) {
                priorityWaitRepository.deleteByOrderId(order.getId());
                assignOrderToPlayer(order, player);
                logOperation(order, player, "GRAB", "抢单成功（高等级抢占）");
                return GrabResult.builder()
                        .orderId(order.getId())
                        .orderNo(order.getOrderNo())
                        .status("IN_SERVICE")
                        .build();
            } else {
                if (priorityWaitRepository.existsByOrderIdAndPlayerId(order.getId(), player.getId())) {
                    throw new RuntimeException("您已在优先等待中");
                }
                SystemConfig config = getSystemConfig();
                GrabPriorityWait wait = new GrabPriorityWait();
                wait.setOrder(order);
                wait.setPlayer(player);
                wait.setWaitUntil(LocalDateTime.now().plusSeconds(config.getGrabPriorityWaitSeconds()));
                priorityWaitRepository.save(wait);

                int waitSeconds = config.getGrabPriorityWaitSeconds();
                return GrabResult.builder()
                        .orderId(order.getId())
                        .orderNo(order.getOrderNo())
                        .status("PRIORITY_WAITING")
                        .priorityWaitUntil(wait.getWaitUntil())
                        .priorityWaitSeconds(waitSeconds)
                        .build();
            }
        } else {
            assignOrderToPlayer(order, player);
            logOperation(order, player, "GRAB", "抢单成功");
            return GrabResult.builder()
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .status("IN_SERVICE")
                    .build();
        }
    }

    private GrabResult handleDoubleSoloGrab(Order order, User player, GrabRequest request) {
        if (order.getGrabStatus() == Order.GrabStatus.LOCKED) {
            throw new RuntimeException("订单已被固排锁定");
        }
        if (order.getPlayerCount() != Order.PlayerCount.DOUBLE) {
            throw new RuntimeException("该订单不是双人单");
        }

        if ("WAIT".equals(request.getAction())) {
            if (waitingPlayerRepository.existsByOrderIdAndPlayerId(order.getId(), player.getId())) {
                throw new RuntimeException("您已在等待列表中");
            }
            GrabWaitingPlayer wp = new GrabWaitingPlayer();
            wp.setOrder(order);
            wp.setPlayer(player);
            waitingPlayerRepository.save(wp);

            if (order.getGrabStatus() == Order.GrabStatus.OPEN) {
                order.setGrabStatus(Order.GrabStatus.WAITING);
                orderRepository.save(order);
            }

            return GrabResult.builder()
                    .orderId(order.getId())
                    .status("WAITING")
                    .build();
        }

        if ("TEAM".equals(request.getAction())) {
            if (request.getTargetPlayerId() == null) {
                throw new RuntimeException("请选择组队对象");
            }
            List<GrabWaitingPlayer> waiters = waitingPlayerRepository.findByOrderId(order.getId());
            GrabWaitingPlayer target = waiters.stream()
                    .filter(w -> w.getPlayer().getId().equals(request.getTargetPlayerId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("目标陪玩师不在等待列表中"));

            if (target.getPlayer().getId().equals(player.getId())) {
                throw new RuntimeException("不能与自己组队");
            }

            if (!canGrabByLevel(order, target.getPlayer())) {
                throw new RuntimeException("目标陪玩师等级不满足要求");
            }

            assignOrderToDoublePlayers(order, player, target.getPlayer());
            waitingPlayerRepository.deleteByOrderId(order.getId());
            logOperation(order, player, "GRAB_TEAM", "与" + target.getPlayer().getNickname() + "组队抢单成功");
            return GrabResult.builder()
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .status("IN_SERVICE")
                    .build();
        }

        throw new RuntimeException("无效的操作类型");
    }

    private GrabResult handleDoubleTeamGrab(Order order, User player, GrabRequest request) {
        if (order.getGrabStatus() != Order.GrabStatus.OPEN) {
            throw new RuntimeException("当前有散排等待中，只能排队或组队");
        }
        if (order.getPlayerCount() != Order.PlayerCount.DOUBLE) {
            throw new RuntimeException("该订单不是双人单");
        }

        User partner;
        if (request.getPartnerId() != null) {
            partner = userRepository.findById(request.getPartnerId())
                    .orElseThrow(() -> new RuntimeException("固排陪玩师不存在"));
        } else if (request.getPartnerPhone() != null && !request.getPartnerPhone().trim().isEmpty()) {
            partner = userRepository.findByPhone(request.getPartnerPhone())
                    .orElseThrow(() -> new RuntimeException("该手机号对应的陪玩师不存在"));
        } else {
            throw new RuntimeException("请选择固排队友");
        }
        if (partner.getRole() != User.Role.PLAYER) {
            throw new RuntimeException("该用户不是陪玩师");
        }
        if (partner.getId().equals(player.getId())) {
            throw new RuntimeException("不能指定自己为固排");
        }
        List<Order.Status> activeStatuses = Arrays.asList(
                Order.Status.PENDING_ACCEPT, Order.Status.PENDING_ACCEPT_2, Order.Status.IN_SERVICE);
        long partnerActiveCount = orderRepository.countActiveOrdersByPlayerId(partner.getId(), activeStatuses);
        if (partnerActiveCount > 0) {
            throw new RuntimeException("该陪玩师有进行中的订单");
        }
        if (!canGrabByLevel(order, partner)) {
            throw new RuntimeException("固排等级不满足要求");
        }
        boolean partnerInCooldown = cooldownRepository.existsByOrderIdAndPlayerIdAndCooldownUntilAfter(
                order.getId(), partner.getId(), LocalDateTime.now());
        if (partnerInCooldown) {
            throw new RuntimeException("固排在此订单冷却期内");
        }

        SystemConfig config = getSystemConfig();
        order.setGrabLeader(player);
        order.setGrabPartner(partner);
        order.setGrabLockUntil(LocalDateTime.now().plusSeconds(config.getGrabTeamLockSeconds()));
        order.setGrabStatus(Order.GrabStatus.LOCKED);
        orderRepository.save(order);

        long remainingSeconds = config.getGrabTeamLockSeconds();
        return GrabResult.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .status("LOCKED")
                .partnerNickname(partner.getNickname())
                .lockUntil(order.getGrabLockUntil())
                .lockRemainingSeconds((int) remainingSeconds)
                .build();
    }

    @Transactional
    public GrabResult joinTeam(Long orderId, User currentUser) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (order.getGrabPartner() == null || !order.getGrabPartner().getId().equals(currentUser.getId())) {
            throw new RuntimeException("您不是此订单指定的固排队友");
        }
        if (order.getGrabStatus() != Order.GrabStatus.LOCKED) {
            throw new RuntimeException("订单状态不正确");
        }
        if (order.getGrabLockUntil() != null && order.getGrabLockUntil().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("锁定已过期，订单已回到抢单池");
        }
        List<Order.Status> activeStatuses = Arrays.asList(
                Order.Status.PENDING_ACCEPT, Order.Status.PENDING_ACCEPT_2, Order.Status.IN_SERVICE);
        long activeCount = orderRepository.countActiveOrdersByPlayerId(currentUser.getId(), activeStatuses);
        if (activeCount > 0) {
            throw new RuntimeException("您有正在进行的订单，无法加入");
        }

        User leader = order.getGrabLeader();
        assignOrderToDoublePlayers(order, leader, currentUser);
        order.setGrabLeader(null);
        order.setGrabPartner(null);
        order.setGrabLockUntil(null);
        orderRepository.save(order);
        logOperation(order, currentUser, "JOIN_TEAM", "固排加入成功");

        return GrabResult.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .status("IN_SERVICE")
                .build();
    }

    @Transactional
    public void cancelGrab(Long orderId, User currentUser) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        boolean isWaitingPlayer = waitingPlayerRepository.existsByOrderIdAndPlayerId(orderId, currentUser.getId());
        boolean isGrabLeader = order.getGrabLeader() != null && order.getGrabLeader().getId().equals(currentUser.getId());
        boolean isPriorityWaiting = priorityWaitRepository.existsByOrderIdAndPlayerId(orderId, currentUser.getId());

        if (!isWaitingPlayer && !isGrabLeader && !isPriorityWaiting) {
            throw new RuntimeException("您不在此订单的等待列表中");
        }

        if (isWaitingPlayer) {
            waitingPlayerRepository.deleteByOrderIdAndPlayerId(orderId, currentUser.getId());
            List<GrabWaitingPlayer> remaining = waitingPlayerRepository.findByOrderId(orderId);
            if (remaining.isEmpty() && order.getGrabStatus() == Order.GrabStatus.WAITING) {
                order.setGrabStatus(Order.GrabStatus.OPEN);
                orderRepository.save(order);
            }
        }

        if (isGrabLeader) {
            SystemConfig config = getSystemConfig();
            GrabCooldown cooldown = new GrabCooldown();
            cooldown.setOrder(order);
            cooldown.setPlayer(currentUser);
            cooldown.setCooldownUntil(LocalDateTime.now().plusSeconds(config.getGrabCooldownSeconds()));
            cooldownRepository.save(cooldown);

            order.setGrabStatus(Order.GrabStatus.OPEN);
            order.setGrabLeader(null);
            order.setGrabPartner(null);
            order.setGrabLockUntil(null);
            orderRepository.save(order);
        }

        if (isPriorityWaiting) {
            priorityWaitRepository.deleteByOrderIdAndPlayerId(orderId, currentUser.getId());
        }
    }

    @Transactional
    public void publishToHall(Long orderId, PublishToHallRequest request, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (order.getStatus() != Order.Status.PENDING_ASSIGN) {
            throw new RuntimeException("只有待分配的订单才能发布到抢单大厅");
        }
        if (Boolean.TRUE.equals(order.getInGrabHall())) {
            throw new RuntimeException("订单已在抢单大厅中");
        }

        if (request.getPriorityLevel() != null && !request.getPriorityLevel().trim().isEmpty()) {
            if (!"huhang".equals(order.getOrderType())) {
                throw new RuntimeException("陪玩单不支持优先等级");
            }
            if (order.getPlayerCount() != Order.PlayerCount.SINGLE) {
                throw new RuntimeException("仅单人护航单支持优先等级");
            }
            if (!levelPriceRepository.existsByLevel(request.getPriorityLevel())) {
                throw new RuntimeException("无效的等级名称");
            }
            order.setPriorityLevel(request.getPriorityLevel());
        }

        order.setInGrabHall(true);
        order.setHallPublishTime(LocalDateTime.now());
        order.setGrabStatus(Order.GrabStatus.OPEN);
        orderRepository.save(order);

        logOperation(order, currentUser, "PUBLISH_HALL", "发布到抢单大厅");
    }

    @Transactional
    public void withdrawFromHall(Long orderId, User currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!Boolean.TRUE.equals(order.getInGrabHall())) {
            throw new RuntimeException("订单不在抢单大厅");
        }
        if (order.getGrabStatus() == Order.GrabStatus.ASSIGNED) {
            throw new RuntimeException("订单已被抢走，无法撤回");
        }

        order.setInGrabHall(false);
        order.setGrabStatus(null);
        order.setGrabLeader(null);
        order.setGrabPartner(null);
        order.setGrabLockUntil(null);
        order.setPriorityLevel(null);
        orderRepository.save(order);

        waitingPlayerRepository.deleteByOrderId(orderId);
        cooldownRepository.deleteByOrderId(orderId);
        priorityWaitRepository.deleteByOrderId(orderId);

        logOperation(order, currentUser, "WITHDRAW_HALL", "从抢单大厅撤回");
    }

    public Page<GrabHallOrderResponse> getGrabHallOrders(String orderType, String playerCount,
                                                         int page, int size, User currentUser) {
        List<Order.GrabStatus> grabStatuses = Arrays.asList(Order.GrabStatus.OPEN, Order.GrabStatus.WAITING, Order.GrabStatus.LOCKED);
        Order.PlayerCount pc = playerCount != null ? Order.PlayerCount.valueOf(playerCount) : null;
        Page<Order> orderPage = orderRepository.findGrabHallOrders(grabStatuses, orderType, pc,
                PageRequest.of(page, size));

        SystemConfig config = getSystemConfig();
        BigDecimal platformFeeRate = config.getPlatformFeeRate();

        return orderPage.map(order -> convertToGrabHallResponse(order, currentUser, platformFeeRate));
    }

    public MyGrabStatusResponse getMyGrabStatus(User currentUser) {
        MyGrabStatusResponse.MyGrabStatusResponseBuilder builder = MyGrabStatusResponse.builder();

        List<GrabWaitingPlayer> waitingList = waitingPlayerRepository.findByPlayerId(currentUser.getId());
        if (!waitingList.isEmpty()) {
            GrabWaitingPlayer wp = waitingList.get(0);
            Order order = wp.getOrder();
            List<GrabWaitingPlayer> allWaiters = waitingPlayerRepository.findByOrderId(order.getId());
            List<MyGrabStatusResponse.OtherWaiterInfo> others = allWaiters.stream()
                    .filter(w -> !w.getPlayer().getId().equals(currentUser.getId()))
                    .map(w -> MyGrabStatusResponse.OtherWaiterInfo.builder()
                            .id(w.getPlayer().getId())
                            .nickname(w.getPlayer().getNickname())
                            .level(w.getPlayer().getLevel())
                            .build())
                    .collect(Collectors.toList());
            builder.waitingOrder(MyGrabStatusResponse.WaitingOrderInfo.builder()
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .grabType("DOUBLE_SOLO")
                    .waitingSince(wp.getJoinedAt())
                    .otherWaiters(others)
                    .build());
        } else {
            builder.waitingOrder(null);
        }

        List<GrabPriorityWait> priorityWaits = priorityWaitRepository.findByPlayerId(currentUser.getId());
        if (!priorityWaits.isEmpty()) {
            GrabPriorityWait pw = priorityWaits.get(0);
            Order order = pw.getOrder();
            int remainingSeconds = (int) LocalDateTime.now().until(pw.getWaitUntil(), ChronoUnit.SECONDS);
            builder.priorityWaitingOrder(MyGrabStatusResponse.PriorityWaitingOrderInfo.builder()
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .priorityWaitUntil(pw.getWaitUntil())
                    .priorityWaitRemainingSeconds(Math.max(0, remainingSeconds))
                    .build());
        } else {
            builder.priorityWaitingOrder(null);
        }

        List<Order> lockedOrders = orderRepository.findByGrabStatusAndGrabLockUntilBefore(
                Order.GrabStatus.LOCKED, LocalDateTime.now().plusYears(100));
        Order lockedOrder = lockedOrders.stream()
                .filter(o -> o.getGrabPartner() != null && o.getGrabPartner().getId().equals(currentUser.getId()))
                .findFirst().orElse(null);
        if (lockedOrder != null) {
            int remainingSeconds = (int) LocalDateTime.now().until(lockedOrder.getGrabLockUntil(), ChronoUnit.SECONDS);
            builder.lockedOrder(MyGrabStatusResponse.LockedOrderInfo.builder()
                    .orderId(lockedOrder.getId())
                    .orderNo(lockedOrder.getOrderNo())
                    .partnerNickname(lockedOrder.getGrabLeader().getNickname())
                    .lockUntil(lockedOrder.getGrabLockUntil())
                    .lockRemainingSeconds(Math.max(0, remainingSeconds))
                    .build());
        } else {
            Order leaderLockedOrder = lockedOrders.stream()
                    .filter(o -> o.getGrabLeader() != null && o.getGrabLeader().getId().equals(currentUser.getId()))
                    .findFirst().orElse(null);
            if (leaderLockedOrder != null) {
                int remainingSeconds = (int) LocalDateTime.now().until(leaderLockedOrder.getGrabLockUntil(), ChronoUnit.SECONDS);
                builder.lockedOrder(MyGrabStatusResponse.LockedOrderInfo.builder()
                        .orderId(leaderLockedOrder.getId())
                        .orderNo(leaderLockedOrder.getOrderNo())
                        .partnerNickname(leaderLockedOrder.getGrabPartner().getNickname())
                        .lockUntil(leaderLockedOrder.getGrabLockUntil())
                        .lockRemainingSeconds(Math.max(0, remainingSeconds))
                        .build());
            } else {
                builder.lockedOrder(null);
            }
        }

        List<Order.Status> activeStatuses = Arrays.asList(
                Order.Status.PENDING_ACCEPT, Order.Status.PENDING_ACCEPT_2, Order.Status.IN_SERVICE);
        long activeCount = orderRepository.countActiveOrdersByPlayerId(currentUser.getId(), activeStatuses);
        builder.hasActiveOrder(activeCount > 0);

        return builder.build();
    }

    private void assignOrderToPlayer(Order order, User player) {
        order.setCurrentPlayer(player);
        order.setStatus(Order.Status.IN_SERVICE);
        order.setGrabStatus(Order.GrabStatus.ASSIGNED);
        order.setInGrabHall(false);
        order.setAssignedAt(LocalDateTime.now());
        order.setStartedAt(LocalDateTime.now());
        clearAllGrabRecords(order);
        orderRepository.save(order);

        createOrderSession(order, player);
    }

    private void assignOrderToDoublePlayers(Order order, User player1, User player2) {
        order.setCurrentPlayer(player1);
        order.setCurrentPlayer2(player2);
        order.setStatus(Order.Status.IN_SERVICE);
        order.setGrabStatus(Order.GrabStatus.ASSIGNED);
        order.setInGrabHall(false);
        order.setAssignedAt(LocalDateTime.now());
        order.setStartedAt(LocalDateTime.now());
        clearAllGrabRecords(order);
        orderRepository.save(order);

        createOrderSession(order, player1);
        createOrderSession(order, player2);
    }

    private void createOrderSession(Order order, User player) {
        OrderSession session = new OrderSession();
        session.setOrder(order);
        session.setPlayer(player);
        session.setStartedAt(LocalDateTime.now());
        orderSessionRepository.save(session);
    }

    private void clearAllGrabRecords(Order order) {
        waitingPlayerRepository.deleteByOrderId(order.getId());
        priorityWaitRepository.deleteByOrderId(order.getId());
        cooldownRepository.deleteByOrderId(order.getId());
        order.setGrabLeader(null);
        order.setGrabPartner(null);
        order.setGrabLockUntil(null);
        order.setPriorityLevel(null);
    }

    private SystemConfig getSystemConfig() {
        return systemConfigRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("系统配置不存在"));
    }

    private GrabHallOrderResponse convertToGrabHallResponse(Order order, User currentUser, BigDecimal platformFeeRate) {
        BigDecimal orderTotal = order.getTotalAmount() != null ? order.getTotalAmount() :
                order.getPricePerHour().multiply(order.getServiceHours());
        BigDecimal netIncome = orderTotal.multiply(BigDecimal.ONE.subtract(platformFeeRate));
        int divisor = order.getPlayerCount() == Order.PlayerCount.SINGLE ? 1 : 2;
        BigDecimal estimatedIncome = netIncome.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);

        boolean isLocked = order.getGrabStatus() == Order.GrabStatus.LOCKED;
        Integer lockRemainingSeconds = null;
        if (isLocked && order.getGrabLockUntil() != null) {
            long secs = LocalDateTime.now().until(order.getGrabLockUntil(), ChronoUnit.SECONDS);
            lockRemainingSeconds = Math.max(0, (int) secs);
        }

        List<GrabWaitingPlayer> waiters = waitingPlayerRepository.findByOrderId(order.getId());
        List<GrabHallOrderResponse.WaitingPlayerInfo> waiterInfos = waiters.stream()
                .map(w -> GrabHallOrderResponse.WaitingPlayerInfo.builder()
                        .id(w.getPlayer().getId())
                        .playerNo(w.getPlayer().getPlayerNo())
                        .nickname(w.getPlayer().getNickname())
                        .level(w.getPlayer().getLevel())
                        .pricePerHour(w.getPlayer().getPricePerHour())
                        .build())
                .collect(Collectors.toList());

        List<GrabPriorityWait> priorityWaits = priorityWaitRepository.findByOrderId(order.getId());
        boolean isPriorityWaiting = !priorityWaits.isEmpty();
        Integer priorityWaitRemainingSeconds = null;
        if (isPriorityWaiting) {
            LocalDateTime earliest = priorityWaits.stream()
                    .map(GrabPriorityWait::getWaitUntil)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
            if (earliest != null) {
                long secs = LocalDateTime.now().until(earliest, ChronoUnit.SECONDS);
                priorityWaitRemainingSeconds = Math.max(0, (int) secs);
            }
        }
        List<GrabHallOrderResponse.PriorityWaitPlayerInfo> priorityWaitPlayerInfos = priorityWaits.stream()
                .map(pw -> GrabHallOrderResponse.PriorityWaitPlayerInfo.builder()
                        .id(pw.getPlayer().getId())
                        .nickname(pw.getPlayer().getNickname())
                        .level(pw.getPlayer().getLevel())
                        .build())
                .collect(Collectors.toList());

        return GrabHallOrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .orderType(order.getOrderType())
                .playerCount(order.getPlayerCount() != null ? order.getPlayerCount().name() : null)
                .grabStatus(order.getGrabStatus() != null ? order.getGrabStatus().name() : null)
                .pricePerHour(order.getPricePerHour())
                .totalAmount(orderTotal)
                .serviceHours(order.getServiceHours())
                .estimatedIncome(estimatedIncome)
                .bossInfo(order.getBossInfo())
                .customerType(order.getCustomerType() != null ? order.getCustomerType().name() : null)
                .serviceContent(order.getServiceContent())
                .scheduledTime(order.getScheduledTime())
                .publishedAt(order.getHallPublishTime())
                .waitingPlayers(waiterInfos)
                .isLocked(isLocked)
                .lockRemainingSeconds(lockRemainingSeconds)
                .grabPartnerId(order.getGrabPartner() != null ? order.getGrabPartner().getId() : null)
                .grabPartnerNickname(order.getGrabPartner() != null ? order.getGrabPartner().getNickname() : null)
                .grabLeaderId(order.getGrabLeader() != null ? order.getGrabLeader().getId() : null)
                .priorityLevel(order.getPriorityLevel())
                .isPriorityWaiting(isPriorityWaiting)
                .priorityWaitRemainingSeconds(priorityWaitRemainingSeconds)
                .priorityWaitPlayers(priorityWaitPlayerInfos)
                .build();
    }

    private void logOperation(Order order, User user, String type, String detail) {
        OperationLog log = new OperationLog();
        log.setOrder(order);
        log.setOperator(user);
        log.setOperationType(type);
        log.setNewStatus(order.getStatus());
        log.setDetails(detail);
        operationLogRepository.save(log);
    }
}
