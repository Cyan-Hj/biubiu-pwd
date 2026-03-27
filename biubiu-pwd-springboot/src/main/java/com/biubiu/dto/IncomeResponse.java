package com.biubiu.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class IncomeResponse {
    private BigDecimal totalIncome;
    private BigDecimal availableBalance;
    private BigDecimal todayIncome;
}
