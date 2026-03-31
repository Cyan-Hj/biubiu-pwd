package com.biubiu.controller;

import com.biubiu.dto.*;
import com.biubiu.dto.CancelOrderRequest;
import com.biubiu.entity.Order;
import com.biubiu.entity.User;
import com.biubiu.repository.OrderRepository;
import com.biubiu.repository.UserRepository;
import com.biubiu.service.OrderService;
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
    private final OrderService orderService;

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean today,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
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
        if (currentUser.getRole() == User.Role.player) {
            playerId = currentUser.getId();
            excludeCancelled = true; // 陪玩师看不到取消的订单
            cancelledStatus = Order.Status.CANCELLED;
        }

        Sort sort = Sort.by("desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);

        Page<Order> orderPage = orderRepository.findOrders(
                statusEnum,
                playerId,
                today,
                startDate,
                endDate,
                excludeCancelled,
                cancelledStatus,
                PageRequest.of(page - 1, pageSize, sort)
        );

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
        List<Order> orders = orderRepository.findByCurrentPlayerIdAndStatus(currentUser.getId(), Order.Status.IN_SERVICE);
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

    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> batchDeleteOrders(@RequestBody List<Long> ids) {
        orderService.batchDeleteOrders(ids);
        return ApiResponse.success("批量删除成功", null);
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
                .playerCount(order.getPlayerCount() != null ? order.getPlayerCount().name() : null)
                .startScreenshotUrl(order.getStartScreenshotUrl())
                .endScreenshotUrl(order.getEndScreenshotUrl())
                .build();
        
        // 获取当前登录用户
        User currentUser = getCurrentUser();
        
        // 判断当前用户是否是该订单的陪玩师之一
        boolean isPlayer1 = order.getCurrentPlayer() != null && order.getCurrentPlayer().getId().equals(currentUser.getId());
        boolean isPlayer2 = order.getCurrentPlayer2() != null && order.getCurrentPlayer2().getId().equals(currentUser.getId());
        
        if (currentUser.getRole() == User.Role.player && (isPlayer1 || isPlayer2)) {
            // 检查当前用户是否已经接单（存在未结束的会话）
            List<com.biubiu.entity.OrderSession> sessions = orderSessionRepository.findByOrderIdAndPlayerId(order.getId(), currentUser.getId());
            boolean hasActiveSession = sessions.stream().anyMatch(s -> s.getEndedAt() == null);
            response.setCurrentUserAccepted(hasActiveSession);
            
            // 检查当前用户是否已完成（存在已结束的会话）
            boolean hasCompletedSession = sessions.stream().anyMatch(s -> s.getEndedAt() != null);
            response.setCurrentUserCompleted(hasCompletedSession);
            
            // 对于双人订单，检查另一个陪玩师是否已完成
            if (order.getPlayerCount() == Order.PlayerCount.DOUBLE) {
                Long otherPlayerId = isPlayer1 ? 
                    (order.getCurrentPlayer2() != null ? order.getCurrentPlayer2().getId() : null) : 
                    (order.getCurrentPlayer() != null ? order.getCurrentPlayer().getId() : null);
                
                if (otherPlayerId != null) {
                    // 检查另一个陪玩师是否有已结束的会话
                    List<com.biubiu.entity.OrderSession> otherSessions = orderSessionRepository.findByOrderIdAndPlayerId(order.getId(), otherPlayerId);
                    boolean otherCompleted = otherSessions.stream().anyMatch(s -> s.getEndedAt() != null);
                    response.setOtherPlayerCompleted(otherCompleted);
                } else {
                    response.setOtherPlayerCompleted(false);
                }
            }
            
            // 如果是已完成订单，附加上收入信息
            if (order.getStatus() == Order.Status.COMPLETED) {
                com.biubiu.entity.FinancialRecord record = financialRecordRepository.findFirstByOrderIdAndPlayerIdAndTypeOrderByIdDesc(
                    order.getId(), 
                    currentUser.getId(), 
                    com.biubiu.entity.FinancialRecord.Type.income
                ).orElse(null);
                
                if (record != null) {
                    response.setIncomeAmount(record.getAmount());
                }
            }
        } else {
            response.setCurrentUserAccepted(false);
            response.setCurrentUserCompleted(false);
            response.setOtherPlayerCompleted(false);
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
