package com.biubiu.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AuditOrderResponse {
    private Long id;
    private String orderNo;
    private String playerNickname;
    private Long playerId;

    private BigDecimal totalAmount;
    private BigDecimal serviceHours;
    private BigDecimal expectedIncomeAmount;

    private BigDecimal actualTotalAmount;
    private BigDecimal actualHours;
    private BigDecimal actualIncomeAmount;

    private LocalDateTime completedAt;
    private Integer auditStatus;
}