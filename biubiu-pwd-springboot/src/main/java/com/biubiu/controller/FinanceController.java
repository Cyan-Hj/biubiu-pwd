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

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER_SERVICE')")
    public ApiResponse<FinanceStatsResponse> getStatistics() {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(6).with(LocalTime.MIN);

        // 订单金额统计（已完成）
        BigDecimal todayOrderAmount = orderRepository.sumTotalAmountByStatusAndCreatedAtAfter(Order.Status.COMPLETED, startOfDay);
        BigDecimal monthOrderAmount = orderRepository.sumTotalAmountByStatusAndCreatedAtAfter(Order.Status.COMPLETED, startOfMonth);
        BigDecimal totalOrderAmount = orderRepository.sumTotalAmountByStatus(Order.Status.COMPLETED);

        // 订单数量统计（已完成）
        Long todayOrderCount = orderRepository.countByStatusAndCreatedAtAfter(Order.Status.COMPLETED, startOfDay);
        Long monthOrderCount = orderRepository.countByStatusAndCreatedAtAfter(Order.Status.COMPLETED, startOfMonth);
        Long totalOrderCount = orderRepository.countByStatus(Order.Status.COMPLETED);

        // 取消订单统计
        BigDecimal todayCancelledAmount = orderRepository.sumTotalAmountByStatusAndCreatedAtAfter(Order.Status.CANCELLED, startOfDay);
        BigDecimal monthCancelledAmount = orderRepository.sumTotalAmountByStatusAndCreatedAtAfter(Order.Status.CANCELLED, startOfMonth);
        BigDecimal totalCancelledAmount = orderRepository.sumTotalAmountByStatus(Order.Status.CANCELLED);
        Long todayCancelledCount = orderRepository.countByStatusAndCreatedAtAfter(Order.Status.CANCELLED, startOfDay);
        Long monthCancelledCount = orderRepository.countByStatusAndCreatedAtAfter(Order.Status.CANCELLED, startOfMonth);
        Long totalCancelledCount = orderRepository.countByStatus(Order.Status.CANCELLED);

        // 收入统计
        BigDecimal totalPlayerIncome = financialRecordRepository.sumTotalIncome();
        BigDecimal totalPlatformIncome = totalOrderAmount.subtract(totalPlayerIncome);

        // 收入分布（按类型）- 本月数据
        Map<String, BigDecimal> incomeDistribution = new java.util.HashMap<>();
        // 本月陪玩师收入
        BigDecimal monthPlayerIncome = financialRecordRepository.sumIncomeByCreatedAtAfter(startOfMonth);
        if (monthPlayerIncome == null) monthPlayerIncome = BigDecimal.ZERO;
        // 本月平台抽成 = 本月订单总额 - 本月陪玩师收入
        BigDecimal monthPlatformIncome = monthOrderAmount.subtract(monthPlayerIncome);
        if (monthPlatformIncome.compareTo(BigDecimal.ZERO) < 0) monthPlatformIncome = BigDecimal.ZERO;
        
        incomeDistribution.put("陪玩师收入", monthPlayerIncome);
        incomeDistribution.put("平台抽成", monthPlatformIncome);
        incomeDistribution.put("取消订单", monthCancelledAmount != null ? monthCancelledAmount : BigDecimal.ZERO);

        // 最近7天收入记录
        List<Object[]> dailyStats = orderRepository.findDailyIncomeStats(Order.Status.COMPLETED, startOfWeek);
        List<FinanceStatsResponse.RecentIncomeRecord> recentIncomes = dailyStats.stream()
                .map(stat -> FinanceStatsResponse.RecentIncomeRecord.builder()
                        .date(stat[0].toString())
                        .amount((BigDecimal) stat[1])
                        .orderCount(((Number) stat[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        // 陪玩师收入排行
        List<PlayerIncomeSummary> topPlayers = financialRecordRepository.findTopPlayerIncomes(PageRequest.of(0, 10));
        List<FinanceStatsResponse.PlayerIncomeRank> ranking = topPlayers.stream()
                .map(p -> FinanceStatsResponse.PlayerIncomeRank.builder()
                        .playerId(p.getPlayerId())
                        .nickname(p.getNickname())
                        .totalIncome(p.getTotalIncome())
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

        BigDecimal totalIncome = financialRecordRepository.sumTotalIncomeByPlayerId(currentUser.getId());
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
            // 退回金额
            User player = withdrawal.getPlayer();
            player.setAvailableBalance(player.getAvailableBalance().add(withdrawal.getAmount()));
            userRepository.save(player);
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
                .build();
    }

    private WithdrawalResponse convertToWithdrawalResponse(WithdrawalRequest withdrawal) {
        return WithdrawalResponse.builder()
                .id(withdrawal.getId())
                .playerNickname(withdrawal.getPlayer().getNickname())
                .amount(withdrawal.getAmount())
                .paymentMethod(withdrawal.getPaymentMethod())
                .accountInfo(withdrawal.getAccountInfo())
                .realName(withdrawal.getRealName())
                .status(withdrawal.getStatus())
                .rejectReason(withdrawal.getRejectReason())
                .createdAt(withdrawal.getCreatedAt())
                .build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String phone = authentication.getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
