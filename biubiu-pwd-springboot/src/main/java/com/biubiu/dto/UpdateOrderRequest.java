package com.biubiu.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateOrderRequest {
    private String bossInfo;
    private String serviceContent;
    private BigDecimal serviceHours;
    private BigDecimal pricePerHour;
    private BigDecimal totalAmount;
    private BigDecimal actualHours;
    private BigDecimal actualTotalAmount;
    private BigDecimal actualIncomeAmount;

    private LocalDateTime scheduledTime;
    private String remark;

    private Long bossId;
    private BigDecimal originalAmount;
    private BigDecimal discountRate;
    private Boolean useBalance;
}
