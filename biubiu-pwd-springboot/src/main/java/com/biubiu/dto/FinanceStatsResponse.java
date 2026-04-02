package com.biubiu.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class FinanceStatsResponse {
    private BigDecimal todayOrderAmount;
    private BigDecimal monthOrderAmount;
    private BigDecimal totalOrderAmount;
    private BigDecimal totalPlatformIncome;
    private BigDecimal totalPlayerIncome;
    private BigDecimal todayIncome;
    private BigDecimal monthIncome;
    private Long totalOrderCount;
    private Long todayOrderCount;
    private Long monthOrderCount;
    // 取消订单统计
    private Long cancelledOrderCount;
    private BigDecimal cancelledOrderAmount;
    private Long todayCancelledCount;
    private BigDecimal todayCancelledAmount;
    private Long monthCancelledCount;
    private BigDecimal monthCancelledAmount;
    // 陪玩师统计
    private Long totalPlayerCount;
    private Long pendingPlayerCount;
    private Long activePlayerCount;
    // 待处理事项
    private Long pendingWithdrawals;
    private Long pendingOrders;
    
    private List<PlayerIncomeRank> playerRanking;
    private Map<String, BigDecimal> incomeDistribution;
    private List<RecentIncomeRecord> recentIncomes;

    @Data
    @Builder
    public static class PlayerIncomeRank {
        private Long playerId;
        private String nickname;
        private BigDecimal totalIncome;
    }

    @Data
    @Builder
    public static class RecentIncomeRecord {
        private String date;
        private BigDecimal amount;
        private Long orderCount;
    }
}
