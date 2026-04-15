package com.biubiu.controller;

import com.biubiu.dto.*;
import com.biubiu.entity.FinancialRecord;
import com.biubiu.entity.User;
import com.biubiu.entity.WithdrawalRequest;
import com.biubiu.entity.Order;
import com.biubiu.repository.FinancialRecordRepository;
import com.biubiu.repository.UserRepository;
import com.biubiu.repository.WithdrawalRequestRepository;
import com.biubiu.repository.OrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinancialRecordRepository financialRecordRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final com.biubiu.repository.SystemConfigRepository systemConfigRepository;
    
    /**
     * 计算订单的实际金额（基于实际时长）
     */
    private BigDecimal calculateActualTotalAmount(Order order) {
        if (order.getStatus() != Order.Status.COMPLETED) {
            return order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        }
        
        BigDecimal pricePerHour = order.getPricePerHour();
        BigDecimal createdHours = order.getServiceHours();
        BigDecimal actualHours = order.getActualHours() != null ? order.getActualHours() : createdHours;
        
        if (actualHours.compareTo(createdHours) <= 0) {
            return createdHours.multiply(pricePerHour);
        }
        
        // 计算超出时间的费用
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
        
        return createdHours.multiply(pricePerHour).add(extraFee);
    }
    
    /**
     * 计算订单的陪玩师实际收入
     */
    private BigDecimal calculateActualPlayerIncome(Order order) {
        BigDecimal actualTotalAmount = calculateActualTotalAmount(order);
        BigDecimal platformFeeRate = systemConfigRepository.findFirstByOrderByIdAsc()
                .map(com.biubiu.entity.SystemConfig::getPlatformFeeRate)
                .orElse(BigDecimal.valueOf(0.2));
        
        BigDecimal totalPlayerIncome = actualTotalAmount.multiply(BigDecimal.ONE.subtract(platformFeeRate));
        
        if (order.getPlayerCount() == Order.PlayerCount.DOUBLE) {
            return totalPlayerIncome.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        } else {
            return totalPlayerIncome.setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<FinanceStatsResponse> getStatistics() {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(6).with(LocalTime.MIN);

        // 获取所有已完成订单并计算实际金额
        List<Order> todayCompletedOrders = orderRepository.findByStatusAndCreatedAtAfter(Order.Status.COMPLETED, startOfDay);
        List<Order> monthCompletedOrders = orderRepository.findByStatusAndCreatedAtAfter(Order.Status.COMPLETED, startOfMonth);
        List<Order> allCompletedOrders = orderRepository.findByStatus(Order.Status.COMPLETED);
        
        // 计算实际订单金额（基于实际时长）
        BigDecimal todayOrderAmount = todayCompletedOrders.stream()
                .map(this::calculateActualTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthOrderAmount = monthCompletedOrders.stream()
                .map(this::calculateActualTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOrderAmount = allCompletedOrders.stream()
                .map(this::calculateActualTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 订单数量统计（已完成）
        Long todayOrderCount = (long) todayCompletedOrders.size();
        Long monthOrderCount = (long) monthCompletedOrders.size();
        Long totalOrderCount = (long) allCompletedOrders.size();

        // 取消订单统计（取消订单使用原价）
        BigDecimal todayCancelledAmount = orderRepository.sumTotalAmountByStatusAndCreatedAtAfter(Order.Status.CANCELLED, startOfDay);
        BigDecimal monthCancelledAmount = orderRepository.sumTotalAmountByStatusAndCreatedAtAfter(Order.Status.CANCELLED, startOfMonth);
        BigDecimal totalCancelledAmount = orderRepository.sumTotalAmountByStatus(Order.Status.CANCELLED);
        Long todayCancelledCount = orderRepository.countByStatusAndCreatedAtAfter(Order.Status.CANCELLED, startOfDay);
        Long monthCancelledCount = orderRepository.countByStatusAndCreatedAtAfter(Order.Status.CANCELLED, startOfMonth);
        Long totalCancelledCount = orderRepository.countByStatus(Order.Status.CANCELLED);

        // 计算实际陪玩师总收入（基于实际金额）
        BigDecimal totalPlayerIncome = allCompletedOrders.stream()
                .flatMap(order -> {
                    BigDecimal actualIncome = calculateActualPlayerIncome(order);
                    // 双人订单需要计算两个陪玩师的收入
                    if (order.getPlayerCount() == Order.PlayerCount.DOUBLE && order.getCurrentPlayer() != null && order.getCurrentPlayer2() != null) {
                        return java.util.stream.Stream.of(
                            new AbstractMap.SimpleEntry<>(order.getCurrentPlayer().getId(), actualIncome),
                            new AbstractMap.SimpleEntry<>(order.getCurrentPlayer2().getId(), actualIncome)
                        );
                    } else if (order.getCurrentPlayer() != null) {
                        return java.util.stream.Stream.of(
                            new AbstractMap.SimpleEntry<>(order.getCurrentPlayer().getId(), actualIncome)
                        );
                    }
                    return java.util.stream.Stream.empty();
                })
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalPlatformIncome = totalOrderAmount.subtract(totalPlayerIncome);

        // 收入分布（按类型）- 本月数据
        Map<String, BigDecimal> incomeDistribution = new java.util.HashMap<>();
        // 本月陪玩师收入（基于实际金额）
        BigDecimal monthPlayerIncome = monthCompletedOrders.stream()
                .flatMap(order -> {
                    BigDecimal actualIncome = calculateActualPlayerIncome(order);
                    if (order.getPlayerCount() == Order.PlayerCount.DOUBLE && order.getCurrentPlayer() != null && order.getCurrentPlayer2() != null) {
                        return java.util.stream.Stream.of(
                            new AbstractMap.SimpleEntry<>(order.getCurrentPlayer().getId(), actualIncome),
                            new AbstractMap.SimpleEntry<>(order.getCurrentPlayer2().getId(), actualIncome)
                        );
                    } else if (order.getCurrentPlayer() != null) {
                        return java.util.stream.Stream.of(
                            new AbstractMap.SimpleEntry<>(order.getCurrentPlayer().getId(), actualIncome)
                        );
                    }
                    return java.util.stream.Stream.empty();
                })
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (monthPlayerIncome == null) monthPlayerIncome = BigDecimal.ZERO;
        // 本月平台抽成 = 本月实际订单总额 - 本月陪玩师实际收入
        BigDecimal monthPlatformIncome = monthOrderAmount.subtract(monthPlayerIncome);
        if (monthPlatformIncome.compareTo(BigDecimal.ZERO) < 0) monthPlatformIncome = BigDecimal.ZERO;
        
        incomeDistribution.put("陪玩师收入", monthPlayerIncome);
        incomeDistribution.put("平台抽成", monthPlatformIncome);
        incomeDistribution.put("取消订单", monthCancelledAmount != null ? monthCancelledAmount : BigDecimal.ZERO);

        // 最近7天收入记录（使用实际金额）
        List<Order> weekCompletedOrders = orderRepository.findByStatusAndCreatedAtAfter(Order.Status.COMPLETED, startOfWeek);
        Map<String, java.util.List<Order>> dailyOrders = weekCompletedOrders.stream()
                .collect(Collectors.groupingBy(order -> order.getCreatedAt().toLocalDate().toString()));
        
        List<FinanceStatsResponse.RecentIncomeRecord> recentIncomes = dailyOrders.entrySet().stream()
                .map(entry -> FinanceStatsResponse.RecentIncomeRecord.builder()
                        .date(entry.getKey())
                        .amount(entry.getValue().stream()
                                .map(this::calculateActualTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .orderCount((long) entry.getValue().size())
                        .build())
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());

        // 陪玩师收入排行（基于实际收入）
        Map<Long, String> playerNicknames = new java.util.HashMap<>();
        Map<Long, BigDecimal> playerIncomes = new java.util.HashMap<>();
        
        for (Order order : allCompletedOrders) {
            BigDecimal actualIncome = calculateActualPlayerIncome(order);
            
            if (order.getCurrentPlayer() != null) {
                Long playerId = order.getCurrentPlayer().getId();
                playerNicknames.put(playerId, order.getCurrentPlayer().getNickname());
                playerIncomes.merge(playerId, actualIncome, BigDecimal::add);
            }
            
            if (order.getPlayerCount() == Order.PlayerCount.DOUBLE && order.getCurrentPlayer2() != null) {
                Long playerId = order.getCurrentPlayer2().getId();
                playerNicknames.put(playerId, order.getCurrentPlayer2().getNickname());
                playerIncomes.merge(playerId, actualIncome, BigDecimal::add);
            }
        }
        
        List<FinanceStatsResponse.PlayerIncomeRank> ranking = playerIncomes.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .map(entry -> FinanceStatsResponse.PlayerIncomeRank.builder()
                        .playerId(entry.getKey())
                        .nickname(playerNicknames.getOrDefault(entry.getKey(), "未知"))
                        .totalIncome(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        // 陪玩师统计
        Long totalPlayerCount = userRepository.countByRole(User.Role.PLAYER);
        Long pendingPlayerCount = userRepository.countByRoleAndStatus(User.Role.PLAYER, User.Status.pending);
        Long activePlayerCount = userRepository.countByRoleAndStatus(User.Role.PLAYER, User.Status.active);

        // 待处理事项统计
        Long pendingWithdrawals = withdrawalRequestRepository.countByStatus(WithdrawalRequest.Status.pending);
        Long pendingOrders = orderRepository.countByStatus(Order.Status.PENDING_ASSIGN);

        return ApiResponse.success(FinanceStatsResponse.builder()
                .todayOrderAmount(todayOrderAmount != null ? todayOrderAmount : BigDecimal.ZERO)
                .monthOrderAmount(monthOrderAmount != null ? monthOrderAmount : BigDecimal.ZERO)
                .totalOrderAmount(totalOrderAmount != null ? totalOrderAmount : BigDecimal.ZERO)
                .totalPlatformIncome(totalPlatformIncome != null ? totalPlatformIncome : BigDecimal.ZERO)
                .totalPlayerIncome(totalPlayerIncome != null ? totalPlayerIncome : BigDecimal.ZERO)
                .todayOrderCount(todayOrderCount != null ? todayOrderCount : 0L)
                .monthOrderCount(monthOrderCount != null ? monthOrderCount : 0L)
                .totalOrderCount(totalOrderCount != null ? totalOrderCount : 0L)
                .cancelledOrderCount(totalCancelledCount != null ? totalCancelledCount : 0L)
                .cancelledOrderAmount(totalCancelledAmount != null ? totalCancelledAmount : BigDecimal.ZERO)
                .todayCancelledCount(todayCancelledCount != null ? todayCancelledCount : 0L)
                .todayCancelledAmount(todayCancelledAmount != null ? todayCancelledAmount : BigDecimal.ZERO)
                .monthCancelledCount(monthCancelledCount != null ? monthCancelledCount : 0L)
                .monthCancelledAmount(monthCancelledAmount != null ? monthCancelledAmount : BigDecimal.ZERO)
                .playerRanking(ranking)
                .incomeDistribution(incomeDistribution)
                .recentIncomes(recentIncomes)
                .totalPlayerCount(totalPlayerCount != null ? totalPlayerCount : 0L)
                .pendingPlayerCount(pendingPlayerCount != null ? pendingPlayerCount : 0L)
                .activePlayerCount(activePlayerCount != null ? activePlayerCount : 0L)
                .pendingWithdrawals(pendingWithdrawals != null ? pendingWithdrawals : 0L)
                .pendingOrders(pendingOrders != null ? pendingOrders : 0L)
                .build());
    }

    @GetMapping("/income")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<IncomeResponse> getIncome() {
        User currentUser = getCurrentUser();

        BigDecimal totalIncome = currentUser.getTotalIncome();
        BigDecimal availableBalance = currentUser.getAvailableBalance();

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        BigDecimal todayIncome = financialRecordRepository.sumTodayIncomeByPlayerId(currentUser.getId(), startOfDay);

        IncomeResponse response = IncomeResponse.builder()
                .totalIncome(totalIncome != null ? totalIncome : BigDecimal.ZERO)
                .availableBalance(availableBalance != null ? availableBalance : BigDecimal.ZERO)
                .todayIncome(todayIncome != null ? todayIncome : BigDecimal.ZERO)
                .build();

        return ApiResponse.success(response);
    }

    @GetMapping("/income/records")
    public ApiResponse<PageResponse<FinancialRecordResponse>> getIncomeRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        User currentUser = getCurrentUser();

        Page<FinancialRecord> recordPage;
        if (currentUser.getRole() == User.Role.ADMIN) {
            recordPage = financialRecordRepository.findAll(
                PageRequest.of(page - 1, pageSize, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
            );
        } else {
            recordPage = financialRecordRepository.findByPlayerId(
                currentUser.getId(),
                PageRequest.of(page - 1, pageSize)
            );
        }

        List<FinancialRecordResponse> list = recordPage.getContent().stream()
                .map(this::convertToRecordResponse)
                .collect(Collectors.toList());

        PageResponse<FinancialRecordResponse> response = PageResponse.<FinancialRecordResponse>builder()
                .list(list)
                .total(recordPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();

        return ApiResponse.success(response);
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('PLAYER')")
    public ApiResponse<Void> withdraw(@Valid @RequestBody WithdrawRequest request) {
        User currentUser = getCurrentUser();

        if (request.getAmount().compareTo(currentUser.getAvailableBalance()) > 0) {
            throw new RuntimeException("提现金额超过可提现余额");
        }

        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setPlayer(currentUser);
        withdrawal.setAmount(request.getAmount());
        withdrawal.setPaymentMethod(request.getPaymentMethod());
        withdrawal.setAccountInfo(request.getAccountInfo());
        withdrawal.setRealName(request.getRealName());
        withdrawal.setStatus(WithdrawalRequest.Status.pending);

        withdrawalRequestRepository.save(withdrawal);

        // 冻结提现金额
        currentUser.setAvailableBalance(currentUser.getAvailableBalance().subtract(request.getAmount()));
        userRepository.save(currentUser);

        return ApiResponse.success("提现申请已提交", null);
    }

    @GetMapping("/withdraw/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<WithdrawalResponse>> getPendingWithdrawals() {
        List<WithdrawalRequest> withdrawals = withdrawalRequestRepository.findPendingRequests();

        List<WithdrawalResponse> list = withdrawals.stream()
                .map(this::convertToWithdrawalResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(list);
    }

    @GetMapping("/withdraw/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<WithdrawalResponse>> getAllWithdrawals() {
        List<WithdrawalRequest> withdrawals = withdrawalRequestRepository.findAllByOrderByCreatedAtDesc();

        List<WithdrawalResponse> list = withdrawals.stream()
                .map(this::convertToWithdrawalResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(list);
    }

    @PostMapping("/withdraw/approve-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> approveAllPendingWithdrawals() {
        User currentUser = getCurrentUser();
        List<WithdrawalRequest> pendingList = withdrawalRequestRepository.findPendingRequests();
        int count = 0;
        for (WithdrawalRequest withdrawal : pendingList) {
            withdrawal.setStatus(WithdrawalRequest.Status.approved);
            withdrawal.setReviewedBy(currentUser);
            withdrawal.setReviewedAt(LocalDateTime.now());

            FinancialRecord record = new FinancialRecord();
            record.setPlayer(withdrawal.getPlayer());
            record.setType(FinancialRecord.Type.withdrawal);
            record.setAmount(withdrawal.getAmount());
            record.setDescription("提现到" + (withdrawal.getPaymentMethod() != null ? withdrawal.getPaymentMethod() : "") + "（审核通过）");
            financialRecordRepository.save(record);

            withdrawalRequestRepository.save(withdrawal);
            count++;
        }
        return ApiResponse.success("批量审核通过 " + count + " 条", count);
    }

    @PostMapping("/withdraw/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> reviewWithdrawal(
            @PathVariable Long id,
            @Valid @RequestBody ReviewWithdrawalRequest request) {

        User currentUser = getCurrentUser();

        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("提现申请不存在"));

        if (withdrawal.getStatus() != WithdrawalRequest.Status.pending) {
            throw new RuntimeException("该申请已处理");
        }

        withdrawal.setStatus(request.getStatus());
        withdrawal.setReviewedBy(currentUser);
        withdrawal.setReviewedAt(LocalDateTime.now());

        if (request.getStatus() == WithdrawalRequest.Status.rejected) {
            withdrawal.setRejectReason(request.getRejectReason());
            User player = withdrawal.getPlayer();
            player.setAvailableBalance(player.getAvailableBalance().add(withdrawal.getAmount()));
            userRepository.save(player);
        }

        if (request.getStatus() == WithdrawalRequest.Status.approved) {
            FinancialRecord record = new FinancialRecord();
            record.setPlayer(withdrawal.getPlayer());
            record.setType(FinancialRecord.Type.withdrawal);
            record.setAmount(withdrawal.getAmount());
            record.setDescription("提现到" + (withdrawal.getPaymentMethod() != null ? withdrawal.getPaymentMethod() : "") + "（审核通过）");
            financialRecordRepository.save(record);
        }

        withdrawalRequestRepository.save(withdrawal);

        return ApiResponse.success("处理成功", null);
    }

    private FinancialRecordResponse convertToRecordResponse(FinancialRecord record) {
        return FinancialRecordResponse.builder()
                .id(record.getId())
                .type(record.getType())
                .amount(record.getAmount())
                .orderNo(record.getOrder() != null ? record.getOrder().getOrderNo() : null)
                .serviceContent(record.getOrder() != null ? record.getOrder().getServiceContent() : null)
                .description(record.getDescription())
                .createdAt(record.getCreatedAt())
                .playerNickname(record.getPlayer().getNickname())
                .orderType(record.getOrder() != null ? record.getOrder().getOrderType() : null)
                .remark(record.getOrder() != null ? record.getOrder().getRemark() : null)
                .totalAmount(record.getOrder() != null ? record.getOrder().getTotalAmount() : null)
                .serviceHours(record.getOrder() != null ? record.getOrder().getServiceHours() : null)
                .actualHours(record.getOrder() != null ? record.getOrder().getActualHours() : null)
                .pricePerHour(record.getOrder() != null ? record.getOrder().getPricePerHour() : null)
                .bossInfo(record.getOrder() != null ? record.getOrder().getBossInfo() : null)
                .build();
    }

    private WithdrawalResponse convertToWithdrawalResponse(WithdrawalRequest withdrawal) {
        return WithdrawalResponse.builder()
                .id(withdrawal.getId())
                .playerId(withdrawal.getPlayer().getId())
                .playerNo(withdrawal.getPlayer().getPlayerNo())
                .playerNickname(withdrawal.getPlayer().getNickname())
                .playerPhone(withdrawal.getPlayer().getPhone())
                .amount(withdrawal.getAmount())
                .paymentMethod(withdrawal.getPaymentMethod())
                .accountInfo(withdrawal.getAccountInfo())
                .realName(withdrawal.getRealName())
                .status(withdrawal.getStatus())
                .rejectReason(withdrawal.getRejectReason())
                .createdAt(withdrawal.getCreatedAt())
                .reviewedAt(withdrawal.getReviewedAt())
                .build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String phone = authentication.getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
