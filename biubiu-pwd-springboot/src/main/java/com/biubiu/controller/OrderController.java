package com.biubiu.controller;

import com.biubiu.dto.*;
import com.biubiu.dto.CancelOrderRequest;
import com.biubiu.entity.Order;
import com.biubiu.entity.User;
import com.biubiu.repository.OrderRepository;
import com.biubiu.repository.UserRepository;
import com.biubiu.service.OrderService;
import com.biubiu.service.DeletedOrderBackupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final com.biubiu.repository.FinancialRecordRepository financialRecordRepository;
    private final com.biubiu.repository.OrderSessionRepository orderSessionRepository;
    private final com.biubiu.repository.SystemConfigRepository systemConfigRepository;
    private final OrderService orderService;
    private final com.biubiu.service.GrabOrderService grabOrderService;
    private final DeletedOrderBackupService deletedOrderBackupService;

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean today,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        User currentUser = getCurrentUser();
        Order.Status statusEnum = null;
        if (status != null) {
            statusEnum = Order.Status.values()[status];
        }

        Long playerId = null;
        Boolean excludeCancelled = null;
        Order.Status cancelledStatus = null;
        if (currentUser.getRole() == User.Role.PLAYER) {
            playerId = currentUser.getId();
            excludeCancelled = true; // 陪玩师看不到取消的订单
            cancelledStatus = Order.Status.CANCELLED;
        }

        Sort sort = Sort.by("desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);

        Page<Order> orderPage;
        
        // 如果有搜索关键词，使用搜索查询
        if (keyword != null && !keyword.trim().isEmpty()) {
            orderPage = orderRepository.searchOrders(
                    keyword.trim(),
                    statusEnum,
                    playerId,
                    today,
                    startDate,
                    endDate,
                    excludeCancelled,
                    cancelledStatus,
                    PageRequest.of(page - 1, pageSize, sort)
            );
        } else {
            orderPage = orderRepository.findOrders(
                    statusEnum,
                    playerId,
                    today,
                    startDate,
                    endDate,
                    excludeCancelled,
                    cancelledStatus,
                    PageRequest.of(page - 1, pageSize, sort)
            );
        }

        List<OrderResponse> list = orderPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        PageResponse<OrderResponse> response = PageResponse.<OrderResponse>builder()
                .list(list)
                .total(orderPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();

        return ApiResponse.success(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        User currentUser = getCurrentUser();
        Order saved = orderService.createOrder(request, currentUser);
        return ApiResponse.success("订单创建成功", convertToResponse(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<OrderResponse> updateOrder(@PathVariable Long id, @Valid @RequestBody com.biubiu.dto.UpdateOrderRequest request) {
        User currentUser = getCurrentUser();
        Order saved = orderService.updateOrder(id, request, currentUser);
        return ApiResponse.success("订单修改成功", convertToResponse(saved));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<Void> assignOrder(@PathVariable Long id, @Valid @RequestBody AssignOrderRequest request) {
        User currentUser = getCurrentUser();
        orderService.assignOrder(id, request, currentUser);
        return ApiResponse.success("派送成功", null);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<Void> cancelOrder(@PathVariable Long id, @Valid @RequestBody CancelOrderRequest request) {
        User currentUser = getCurrentUser();
        orderService.cancelOrder(id, request.getReason(), currentUser);
        return ApiResponse.success("订单取消成功", null);
    }

    @PostMapping("/{id}/pause")
    public ApiResponse<Void> pauseOrder(@PathVariable Long id, @Valid @RequestBody PauseOrderRequest request) {
        User currentUser = getCurrentUser();
        orderService.pauseOrder(id, request.getReason(), currentUser);
        return ApiResponse.success("订单已暂存", null);
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<Void> resumeOrder(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        orderService.resumeOrder(id, currentUser);
        return ApiResponse.success("订单已恢复", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        return ApiResponse.success(convertToResponse(order));
    }

    @GetMapping("/by-order-no/{orderNo}")
    public ApiResponse<OrderResponse> getOrderByOrderNo(@PathVariable String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        return ApiResponse.success(convertToResponse(order));
    }

    @GetMapping("/my-in-service")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<List<OrderResponse>> getMyInServiceOrders() {
        User currentUser = getCurrentUser();
        List<Order> inServiceOrders = orderRepository.findByCurrentPlayerIdAndStatus(currentUser.getId(), Order.Status.IN_SERVICE);
        List<Order> pausedOrders = orderRepository.findByCurrentPlayerIdAndStatus(currentUser.getId(), Order.Status.PAUSED);
        List<Order> orders = new java.util.ArrayList<>();
        orders.addAll(inServiceOrders);
        orders.addAll(pausedOrders);
        List<OrderResponse> responses = orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/admin/in-service")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<List<OrderResponse>> getAllInServiceOrders() {
        List<Order> orders = orderRepository.findByStatus(Order.Status.IN_SERVICE);
        List<OrderResponse> responses = orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<Void> acceptOrder(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        orderService.acceptOrder(id, currentUser);
        return ApiResponse.success("接单成功", null);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<Void> completeOrder(@PathVariable Long id, @Valid @RequestBody CompleteOrderRequest request) {
        User currentUser = getCurrentUser();
        orderService.completeOrder(id, request, currentUser);
        return ApiResponse.success("订单完成", null);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> batchDeleteOrders(@RequestBody List<Long> ids) {
        orderService.batchDeleteOrders(ids);
        return ApiResponse.success("批量删除成功", null);
    }

    @PostMapping("/{id}/publish-to-hall")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<Void> publishToHall(@PathVariable Long id, @RequestBody(required = false) com.biubiu.dto.PublishToHallRequest request) {
        User currentUser = getCurrentUser();
        grabOrderService.publishToHall(id, request, currentUser);
        return ApiResponse.success("发布成功", null);
    }

    @PostMapping("/{id}/withdraw-from-hall")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<Void> withdrawFromHall(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        grabOrderService.withdrawFromHall(id, currentUser);
        return ApiResponse.success("撤回成功", null);
    }

    @PostMapping("/replenish")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrderResponse> replenishOrder(@Valid @RequestBody ReplenishOrderRequest request) {
        User currentUser = getCurrentUser();
        Order saved = orderService.replenishOrder(request, currentUser);
        return ApiResponse.success("补单创建成功", convertToResponse(saved));
    }

    @GetMapping("/deleted-backups")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<DeletedOrderBackupService.DeletedOrderBackup>> listDeletedBackups() {
        return ApiResponse.success(deletedOrderBackupService.listBackups());
    }

    @GetMapping("/deleted-backups/{orderNo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DeletedOrderBackupService.DeletedOrderBackup> getDeletedBackup(@PathVariable String orderNo) {
        DeletedOrderBackupService.DeletedOrderBackup backup = deletedOrderBackupService.getBackup(orderNo);
        if (backup == null) {
            return ApiResponse.error("未找到该订单的备份记录");
        }
        return ApiResponse.success(backup);
    }

    @GetMapping("/pending-audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<List<AuditOrderResponse>> getPendingAuditOrders() {
        List<Order> orders = orderRepository.findByStatusAndAuditStatus(Order.Status.COMPLETED, 0);
        List<AuditOrderResponse> list = orders.stream()
                .map(this::convertToAuditResponse)
                .collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    @PostMapping("/{id}/audit-pass")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<Void> auditPassOrder(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        orderService.auditOrder(id, currentUser);
        return ApiResponse.success("审核通过", null);
    }

    @PostMapping("/batch-audit-pass")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<Integer> batchAuditPassOrders(@RequestBody List<Long> ids) {
        User currentUser = getCurrentUser();
        int count = 0;
        for (Long id : ids) {
            try {
                orderService.auditOrder(id, currentUser);
                count++;
            } catch (Exception ignored) {
            }
        }
        return ApiResponse.success("批量审核通过 " + count + " 条", count);
    }

    private AuditOrderResponse convertToAuditResponse(Order order) {
        BigDecimal platformFeeRate = systemConfigRepository.findFirstByOrderByIdAsc()
                .map(com.biubiu.entity.SystemConfig::getPlatformFeeRate)
                .orElse(BigDecimal.valueOf(0.2));

        BigDecimal orderTotalAmount = order.getTotalAmount();
        if (orderTotalAmount == null) {
            orderTotalAmount = order.getPricePerHour().multiply(order.getServiceHours());
        }
        BigDecimal expectedPlayerIncome = orderTotalAmount.multiply(BigDecimal.ONE.subtract(platformFeeRate));
        if (order.getPlayerCount() == Order.PlayerCount.DOUBLE) {
            expectedPlayerIncome = expectedPlayerIncome.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        } else {
            expectedPlayerIncome = expectedPlayerIncome.setScale(2, java.math.RoundingMode.HALF_UP);
        }

        BigDecimal actualTotalAmount;
        if (order.getActualTotalAmount() != null) {
            actualTotalAmount = order.getActualTotalAmount();
        } else {
            actualTotalAmount = order.getTotalAmount();
        }
        actualTotalAmount = actualTotalAmount.setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal actualPlayerIncome;
        if (order.getActualIncomeAmount() != null) {
            actualPlayerIncome = order.getActualIncomeAmount();
            if (order.getPlayerCount() == Order.PlayerCount.DOUBLE) {
                actualPlayerIncome = actualPlayerIncome.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
            }
        } else {
            actualPlayerIncome = actualTotalAmount.multiply(BigDecimal.ONE.subtract(platformFeeRate));
            if (order.getPlayerCount() == Order.PlayerCount.DOUBLE) {
                actualPlayerIncome = actualPlayerIncome.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
            } else {
                actualPlayerIncome = actualPlayerIncome.setScale(2, java.math.RoundingMode.HALF_UP);
            }
        }

        return AuditOrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .playerId(order.getCurrentPlayer() != null ? order.getCurrentPlayer().getId() : null)
                .playerNickname(order.getCurrentPlayer() != null ? order.getCurrentPlayer().getNickname() : null)
                .totalAmount(order.getTotalAmount())
                .serviceHours(order.getServiceHours())
                .expectedIncomeAmount(expectedPlayerIncome)
                .actualTotalAmount(actualTotalAmount)
                .actualHours(order.getActualHours())
                .actualIncomeAmount(actualPlayerIncome)
                .completedAt(order.getCompletedAt())
                .auditStatus(order.getAuditStatus())
                .build();
    }

    private OrderResponse convertToResponse(Order order) {
        OrderResponse response = OrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .bossInfo(order.getBossInfo())
                .serviceContent(order.getServiceContent())
                .serviceHours(order.getServiceHours())
                .pricePerHour(order.getPricePerHour())
                .totalAmount(order.getTotalAmount())
                .scheduledTime(order.getScheduledTime())
                .remark(order.getRemark())
                .status(order.getStatus() != null ? order.getStatus().ordinal() : null)
                .currentPlayerId(order.getCurrentPlayer() != null ? order.getCurrentPlayer().getId() : null)
                .currentPlayerNickname(order.getCurrentPlayer() != null ? order.getCurrentPlayer().getNickname() : null)
                .currentPlayer2Id(order.getCurrentPlayer2() != null ? order.getCurrentPlayer2().getId() : null)
                .currentPlayer2Nickname(order.getCurrentPlayer2() != null ? order.getCurrentPlayer2().getNickname() : null)
                .createdAt(order.getCreatedAt())
                .startedAt(order.getStartedAt())
                .completedAt(order.getCompletedAt())
                .actualHours(order.getActualHours())
                .createdByNickname(order.getCreatedBy() != null ? order.getCreatedBy().getNickname() : null)
                .assignedByNickname(order.getAssignedBy() != null ? order.getAssignedBy().getNickname() : null)
                .cancelReason(order.getCancelReason())
                .cancelledAt(order.getCancelledAt())
                .pauseReason(order.getPauseReason())
                .pausedAt(order.getPausedAt())
                .resumedAt(order.getResumedAt())
                .statusBeforePause(order.getStatusBeforePause() != null ? order.getStatusBeforePause().ordinal() : null)
                .playerCount(order.getPlayerCount() != null ? order.getPlayerCount().name() : null)
                .orderType(order.getOrderType())
                .startScreenshotUrl(order.getStartScreenshotUrl())
                .endScreenshotUrl(order.getEndScreenshotUrl())
                .screenshotUrls(order.getScreenshotUrls() != null && !order.getScreenshotUrls().isEmpty()
                    ? Arrays.asList(order.getScreenshotUrls().split(","))
                    : Collections.emptyList())
                .inGrabHall(order.getInGrabHall())
                .grabStatus(order.getGrabStatus() != null ? order.getGrabStatus().name() : null)
                .priorityLevel(order.getPriorityLevel())
                .auditStatus(order.getAuditStatus())
                .build();
        
        // 获取当前登录用户
        User currentUser = getCurrentUser();
        
        // 判断当前用户是否是该订单的陪玩师之一
        boolean isPlayer1 = order.getCurrentPlayer() != null && order.getCurrentPlayer().getId().equals(currentUser.getId());
        boolean isPlayer2 = order.getCurrentPlayer2() != null && order.getCurrentPlayer2().getId().equals(currentUser.getId());
        
        if (currentUser.getRole() == User.Role.PLAYER && (isPlayer1 || isPlayer2)) {
            // 检查当前用户是否已经接单（存在未结束的会话）
            List<com.biubiu.entity.OrderSession> sessions = orderSessionRepository.findByOrderIdAndPlayerId(order.getId(), currentUser.getId());
            boolean hasActiveSession = sessions.stream().anyMatch(s -> s.getEndedAt() == null);
            response.setCurrentUserAccepted(hasActiveSession);
            
            // 检查当前用户是否已完成：仅在订单状态为IN_SERVICE时，判断是否有已结束的会话且没有活跃会话
            if (order.getStatus() == Order.Status.IN_SERVICE) {
                boolean hasCompletedSession = sessions.stream().anyMatch(s -> s.getEndedAt() != null);
                boolean hasActive = sessions.stream().anyMatch(s -> s.getEndedAt() == null);
                response.setCurrentUserCompleted(hasCompletedSession && !hasActive);
            } else {
                response.setCurrentUserCompleted(false);
            }
            
            // 对于双人订单，检查另一个陪玩师是否已完成
            if (order.getPlayerCount() == Order.PlayerCount.DOUBLE) {
                Long otherPlayerId = isPlayer1 ? 
                    (order.getCurrentPlayer2() != null ? order.getCurrentPlayer2().getId() : null) : 
                    (order.getCurrentPlayer() != null ? order.getCurrentPlayer().getId() : null);
                
                if (otherPlayerId != null) {
                    List<com.biubiu.entity.OrderSession> otherSessions = orderSessionRepository.findByOrderIdAndPlayerId(order.getId(), otherPlayerId);
                    if (order.getStatus() == Order.Status.IN_SERVICE) {
                        boolean otherHasCompleted = otherSessions.stream().anyMatch(s -> s.getEndedAt() != null);
                        boolean otherHasActive = otherSessions.stream().anyMatch(s -> s.getEndedAt() == null);
                        response.setOtherPlayerCompleted(otherHasCompleted && !otherHasActive);
                    } else {
                        response.setOtherPlayerCompleted(false);
                    }
                } else {
                    response.setOtherPlayerCompleted(false);
                }
            }
            
            // 获取平台抽成比例
            BigDecimal platformFeeRate = systemConfigRepository.findFirstByOrderByIdAsc()
                    .map(com.biubiu.entity.SystemConfig::getPlatformFeeRate)
                    .orElse(BigDecimal.valueOf(0.2));
            
            // 计算预计收入（始终基于订单总价）
            BigDecimal orderTotalAmount = order.getTotalAmount();
            if (orderTotalAmount == null) {
                orderTotalAmount = order.getPricePerHour().multiply(order.getServiceHours());
            }
            BigDecimal expectedPlayerIncome = orderTotalAmount.multiply(BigDecimal.ONE.subtract(platformFeeRate));
            if (order.getPlayerCount() == Order.PlayerCount.DOUBLE) {
                response.setExpectedIncomeAmount(expectedPlayerIncome.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP));
            } else {
                response.setExpectedIncomeAmount(expectedPlayerIncome.setScale(2, java.math.RoundingMode.HALF_UP));
            }
            
            // 计算实际收入和实际订单金额
            // 优先使用管理员手动设置的实际总价
            BigDecimal actualTotalAmount;
            if (order.getActualTotalAmount() != null) {
                actualTotalAmount = order.getActualTotalAmount();
            } else if ("huhang".equals(order.getOrderType())) {
                actualTotalAmount = order.getTotalAmount();
            } else {
                actualTotalAmount = order.getTotalAmount();
                BigDecimal createdHours = order.getServiceHours();
                BigDecimal actualHours = order.getActualHours() != null ? order.getActualHours() : createdHours;

                if (actualHours.compareTo(createdHours) > 0) {
                    BigDecimal actualPricePerHour = order.getTotalAmount().divide(createdHours, 2, java.math.RoundingMode.HALF_UP);
                    BigDecimal extraMinutes = actualHours.subtract(createdHours).multiply(BigDecimal.valueOf(60));
                    int totalExtraMinutes = extraMinutes.intValue();
                    int fullHours = totalExtraMinutes / 60;
                    int remainingMinutes = totalExtraMinutes % 60;

                    BigDecimal extraFee = BigDecimal.valueOf(fullHours).multiply(actualPricePerHour);
                    if (remainingMinutes > 15 && remainingMinutes <= 45) {
                        extraFee = extraFee.add(actualPricePerHour.multiply(BigDecimal.valueOf(0.5)));
                    } else if (remainingMinutes > 45) {
                        extraFee = extraFee.add(actualPricePerHour);
                    }

                    actualTotalAmount = order.getTotalAmount().add(extraFee);
                }
            }
            
            response.setActualTotalAmount(actualTotalAmount.setScale(2, java.math.RoundingMode.HALF_UP));
            
            if (order.getActualIncomeAmount() != null) {
                BigDecimal storedIncome = order.getActualIncomeAmount();
                if (order.getPlayerCount() == Order.PlayerCount.DOUBLE) {
                    response.setActualIncomeAmount(storedIncome.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP));
                } else {
                    response.setActualIncomeAmount(storedIncome);
                }
            } else {
                BigDecimal actualPlayerIncome = actualTotalAmount.multiply(BigDecimal.ONE.subtract(platformFeeRate));
                if (order.getPlayerCount() == Order.PlayerCount.DOUBLE) {
                    response.setActualIncomeAmount(actualPlayerIncome.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP));
                } else {
                    response.setActualIncomeAmount(actualPlayerIncome.setScale(2, java.math.RoundingMode.HALF_UP));
                }
            }
            
            // 设置incomeAmount（兼容旧逻辑）
            if (order.getStatus() == Order.Status.COMPLETED) {
                response.setIncomeAmount(response.getActualIncomeAmount());
            } else {
                response.setIncomeAmount(response.getExpectedIncomeAmount());
            }
        } else {
            response.setCurrentUserAccepted(false);
            response.setCurrentUserCompleted(false);
            response.setOtherPlayerCompleted(false);
            
            // 对于非陪玩师用户（管理员/客服），也需要计算实际金额用于显示
            BigDecimal platformFeeRate = systemConfigRepository.findFirstByOrderByIdAsc()
                    .map(com.biubiu.entity.SystemConfig::getPlatformFeeRate)
                    .orElse(BigDecimal.valueOf(0.2));

            // 优先使用管理员手动设置的实际总价
            BigDecimal actualTotalAmount;
            if (order.getActualTotalAmount() != null) {
                actualTotalAmount = order.getActualTotalAmount();
            } else if ("huhang".equals(order.getOrderType())) {
                actualTotalAmount = order.getTotalAmount();
            } else {
                BigDecimal pricePerHour = order.getPricePerHour();
                BigDecimal createdHours = order.getServiceHours();
                BigDecimal actualHours = order.getActualHours() != null ? order.getActualHours() : createdHours;

                if (actualHours.compareTo(createdHours) > 0) {
                    BigDecimal extraMinutes = actualHours.subtract(createdHours).multiply(BigDecimal.valueOf(60));
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
            
            response.setActualTotalAmount(actualTotalAmount.setScale(2, java.math.RoundingMode.HALF_UP));
            
            // 计算预计收入
            BigDecimal orderTotalAmount = order.getTotalAmount();
            if (orderTotalAmount == null) {
                orderTotalAmount = order.getPricePerHour().multiply(order.getServiceHours());
            }
            BigDecimal expectedPlayerIncome = orderTotalAmount.multiply(BigDecimal.ONE.subtract(platformFeeRate));
            if (order.getPlayerCount() == Order.PlayerCount.DOUBLE) {
                response.setExpectedIncomeAmount(expectedPlayerIncome.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP));
            } else {
                response.setExpectedIncomeAmount(expectedPlayerIncome.setScale(2, java.math.RoundingMode.HALF_UP));
            }
            
            // 计算实际收入
            if (order.getActualIncomeAmount() != null) {
                BigDecimal storedIncome = order.getActualIncomeAmount();
                if (order.getPlayerCount() == Order.PlayerCount.DOUBLE) {
                    response.setActualIncomeAmount(storedIncome.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP));
                } else {
                    response.setActualIncomeAmount(storedIncome);
                }
            } else {
                BigDecimal actualPlayerIncome = actualTotalAmount.multiply(BigDecimal.ONE.subtract(platformFeeRate));
                if (order.getPlayerCount() == Order.PlayerCount.DOUBLE) {
                    response.setActualIncomeAmount(actualPlayerIncome.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP));
                } else {
                    response.setActualIncomeAmount(actualPlayerIncome.setScale(2, java.math.RoundingMode.HALF_UP));
                }
            }
            
            // 设置incomeAmount
            if (order.getStatus() == Order.Status.COMPLETED) {
                response.setIncomeAmount(response.getActualIncomeAmount());
            } else {
                response.setIncomeAmount(response.getExpectedIncomeAmount());
            }
        }
        
        // 构建陪玩师会话信息列表（用于详情页面）
        List<com.biubiu.entity.OrderSession> allSessions = orderSessionRepository.findByOrderId(order.getId());
        List<com.biubiu.dto.PlayerSessionInfo> playerSessions = new java.util.ArrayList<>();
        
        for (com.biubiu.entity.OrderSession session : allSessions) {
            com.biubiu.dto.PlayerSessionInfo sessionInfo = com.biubiu.dto.PlayerSessionInfo.builder()
                    .playerId(session.getPlayer().getId())
                    .playerNickname(session.getPlayer().getNickname())
                    .startedAt(session.getStartedAt())
                    .endedAt(session.getEndedAt())
                    .actualHours(session.getActualHours())
                    .build();
            playerSessions.add(sessionInfo);
        }
        response.setPlayerSessions(playerSessions);
        
        return response;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String phone = authentication.getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
